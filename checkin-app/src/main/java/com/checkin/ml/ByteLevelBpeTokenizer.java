package com.checkin.ml;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure Java implementation of the Byte-Level BPE Tokenizer (RoBERTa / GPT-2).
 * Reads vocab.json and merges.txt directly and produces token_ids and attention_mask.
 */
public class ByteLevelBpeTokenizer {

    private static final Pattern GPT2_SPLIT_PATTERN = Pattern.compile(
            "'s|'t|'re|'ve|'m|'ll|'d| ?\\p{L}+| ?\\p{N}+| ?[^\\s\\p{L}\\p{N}]+|\\s+(?!\\S)|\\s+"
    );

    private final Map<Integer, Character> byteToChar;
    private final Map<String, Long> vocab;
    private final Map<String, Integer> bpeRanks;
    private final Map<String, List<String>> bpeCache = new HashMap<>();

    private final long bosId;
    private final long eosId;
    private final long padId;
    private final long unkId;

    public ByteLevelBpeTokenizer(File vocabFile, File mergesFile) throws IOException {
        this.byteToChar = buildByteToCharMap();

        // 1. Load vocab.json
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Integer> rawVocab = mapper.readValue(vocabFile, new TypeReference<Map<String, Integer>>() {});
        this.vocab = new HashMap<>(rawVocab.size());
        for (Map.Entry<String, Integer> entry : rawVocab.entrySet()) {
            this.vocab.put(entry.getKey(), entry.getValue().longValue());
        }

        // 2. Load merges.txt
        List<String> lines = Files.readAllLines(mergesFile.toPath(), StandardCharsets.UTF_8);
        this.bpeRanks = new HashMap<>(lines.size());
        int rank = 0;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            bpeRanks.put(line, rank++);
        }

        this.bosId = vocab.getOrDefault("<s>", 0L);
        this.padId = vocab.getOrDefault("<pad>", 1L);
        this.eosId = vocab.getOrDefault("</s>", 2L);
        this.unkId = vocab.getOrDefault("<unk>", 3L);
    }

    public List<Long> encode(String text, int maxSeqLength) {
        if (text == null || text.trim().isEmpty()) {
            return List.of(bosId, eosId);
        }

        List<Long> tokenIds = new ArrayList<>();
        tokenIds.add(bosId);

        Matcher matcher = GPT2_SPLIT_PATTERN.matcher(text);
        while (matcher.find()) {
            String token = matcher.group();
            String byteEncoded = tokenToByteUnicode(token);
            List<String> bpeTokens = bpe(byteEncoded);
            for (String bpeToken : bpeTokens) {
                Long id = vocab.get(bpeToken);
                if (id != null) {
                    tokenIds.add(id);
                } else {
                    tokenIds.add(unkId);
                }
            }
        }

        // Truncate if necessary (leaving room for EOS)
        if (tokenIds.size() >= maxSeqLength) {
            tokenIds = new ArrayList<>(tokenIds.subList(0, maxSeqLength - 1));
        }
        tokenIds.add(eosId);

        return tokenIds;
    }

    private String tokenToByteUnicode(String token) {
        byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder(bytes.length);
        for (byte b : bytes) {
            sb.append(byteToChar.get(b & 0xFF));
        }
        return sb.toString();
    }

    private List<String> bpe(String token) {
        if (bpeCache.containsKey(token)) {
            return bpeCache.get(token);
        }

        List<String> word = new ArrayList<>();
        for (int i = 0; i < token.length(); i++) {
            word.add(String.valueOf(token.charAt(i)));
        }

        while (word.size() > 1) {
            // Find pair with lowest rank
            String minPairKey = null;
            int minRank = Integer.MAX_VALUE;
            int minIndex = -1;

            for (int i = 0; i < word.size() - 1; i++) {
                String pairKey = word.get(i) + " " + word.get(i + 1);
                Integer r = bpeRanks.get(pairKey);
                if (r != null && r < minRank) {
                    minRank = r;
                    minPairKey = pairKey;
                    minIndex = i;
                }
            }

            if (minPairKey == null) {
                break; // No more merges possible
            }

            String[] parts = minPairKey.split(" ");
            String first = parts[0];
            String second = parts[1];

            List<String> newWord = new ArrayList<>();
            int i = 0;
            while (i < word.size()) {
                if (i < word.size() - 1 && word.get(i).equals(first) && word.get(i + 1).equals(second)) {
                    newWord.add(first + second);
                    i += 2;
                } else {
                    newWord.add(word.get(i));
                    i += 1;
                }
            }
            word = newWord;
        }

        bpeCache.put(token, word);
        return word;
    }

    private static Map<Integer, Character> buildByteToCharMap() {
        Map<Integer, Character> map = new HashMap<>();
        List<Integer> bs = new ArrayList<>();
        for (int b = '!'; b <= '~'; b++) bs.add(b);
        for (int b = '¡'; b <= '¬'; b++) bs.add(b);
        for (int b = '®'; b <= 'ÿ'; b++) bs.add(b);

        List<Integer> cs = new ArrayList<>(bs);
        int n = 0;
        for (int b = 0; b < 256; b++) {
            if (!bs.contains(b)) {
                bs.add(b);
                cs.add(256 + n);
                n++;
            }
        }
        for (int i = 0; i < bs.size(); i++) {
            map.put(bs.get(i), (char) (int) cs.get(i));
        }
        return map;
    }

    public long getBosId() {
        return bosId;
    }

    public long getEosId() {
        return eosId;
    }

    public long getPadId() {
        return padId;
    }

    public long getUnkId() {
        return unkId;
    }

    public int getVocabSize() {
        return vocab.size();
    }
}
