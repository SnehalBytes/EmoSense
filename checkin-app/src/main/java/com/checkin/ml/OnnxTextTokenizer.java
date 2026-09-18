package com.checkin.ml;

import com.checkin.ml.config.TextModelConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Tokenizer implementation corresponding to the real text ONNX model.
 * Loads and executes genuine Byte-Level BPE tokenization using vocab.json and merges.txt.
 *
 * Strictly enforces: DO NOT implement fake tokenization for the real model.
 * If tokenizer files are absent or fail to load, reports unavailable.
 */
public class OnnxTextTokenizer implements TextTokenizer {

    private final TextModelConfig config;
    private final ModelManager modelManager;

    private ByteLevelBpeTokenizer bpeTokenizer;
    private boolean initialized = false;

    public OnnxTextTokenizer(ModelManager modelManager, TextModelConfig config) {
        this.modelManager = modelManager;
        this.config = config;
    }

    private synchronized void ensureInitialized() {
        if (initialized) return;
        initialized = true;
        try {
            Optional<File> vocabFile = modelManager.locateModelFile(config.getVocabPath());
            Optional<File> mergesFile = modelManager.locateModelFile(config.getMergesPath());
            if (vocabFile.isPresent() && mergesFile.isPresent()) {
                this.bpeTokenizer = new ByteLevelBpeTokenizer(vocabFile.get(), mergesFile.get());
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize ByteLevelBpeTokenizer: " + e.getMessage());
            this.bpeTokenizer = null;
        }
    }

    @Override
    public boolean isAvailable() {
        Optional<File> vocabFile = modelManager.locateModelFile(config.getVocabPath());
        Optional<File> mergesFile = modelManager.locateModelFile(config.getMergesPath());
        return vocabFile.isPresent() && vocabFile.get().exists() && vocabFile.get().length() > 0
                && mergesFile.isPresent() && mergesFile.get().exists() && mergesFile.get().length() > 0;
    }

    @Override
    public String getTokenizerSource() {
        Optional<File> vocabFile = modelManager.locateModelFile(config.getVocabPath());
        return vocabFile.map(File::getAbsolutePath).orElse("UNAVAILABLE");
    }

    @Override
    public TokenizedInput tokenize(String text, int maxSequenceLength) {
        if (!isAvailable()) {
            throw new IllegalStateException("Real tokenizer is unavailable. Required files missing: "
                    + config.getVocabPath() + " / " + config.getMergesPath()
                    + ". Real model tokenization requires genuine tokenizer assets.");
        }

        ensureInitialized();
        if (bpeTokenizer == null) {
            throw new IllegalStateException("Failed to load tokenizer from "
                    + config.getVocabPath() + " and " + config.getMergesPath());
        }

        int maxLen = maxSequenceLength > 0 ? maxSequenceLength : config.getMaxSequenceLength();
        List<Long> tokenIdsList = bpeTokenizer.encode(text, maxLen);

        long[] inputIds = new long[tokenIdsList.size()];
        long[] attentionMask = new long[tokenIdsList.size()];
        long[] tokenTypeIds = new long[tokenIdsList.size()];
        List<String> tokens = new ArrayList<>(tokenIdsList.size());

        for (int i = 0; i < tokenIdsList.size(); i++) {
            inputIds[i] = tokenIdsList.get(i);
            attentionMask[i] = 1L;
            tokenTypeIds[i] = 0L;
            tokens.add(String.valueOf(inputIds[i]));
        }

        return new TokenizedInput(inputIds, attentionMask, tokenTypeIds, Collections.unmodifiableList(tokens));
    }
}
