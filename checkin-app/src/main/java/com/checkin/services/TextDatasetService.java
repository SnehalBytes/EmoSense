package com.checkin.services;

import com.checkin.config.DatasetConfig;
import com.checkin.model.EmotionLabel;
import com.checkin.model.TextDatasetSample;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Service for locating, inspecting, parsing, and lazily reading
 * the GoEmotions dataset.
 *
 * Designed for memory efficiency: uses streaming and pagination instead
 * of holding all 54,000+ comments in memory.
 */
public class TextDatasetService {

    public TextDatasetService() {}

    public boolean isAvailable() {
        return DatasetConfig.isTextDatasetAvailable();
    }

    public Path getSplitFile(String split) {
        if ("dev".equalsIgnoreCase(split)) {
            return DatasetConfig.getTextDevFile();
        } else if ("test".equalsIgnoreCase(split)) {
            return DatasetConfig.getTextTestFile();
        } else {
            return DatasetConfig.getTextTrainFile();
        }
    }

    /**
     * Counts rows in a split file.
     */
    public int countRows(String split) {
        Path path = getSplitFile(split);
        if (!Files.isRegularFile(path)) return 0;

        try (var lines = Files.lines(path)) {
            return (int) lines.count();
        } catch (IOException e) {
            return 0;
        }
    }

    public record SplitMetrics(int total, int singleLabel, int multiLabel, int malformed, int emptyText) {}

    /**
     * Inspects a TSV split and computes label breakdown and integrity stats.
     */
    public SplitMetrics inspectSplit(String split) {
        Path path = getSplitFile(split);
        if (!Files.isRegularFile(path)) return new SplitMetrics(0, 0, 0, 0, 0);

        int total = 0;
        int singleLabel = 0;
        int multiLabel = 0;
        int malformed = 0;
        int emptyText = 0;

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                total++;
                String[] parts = line.split("\t");
                if (parts.length < 3) {
                    malformed++;
                    continue;
                }
                String text = parts[0];
                String labels = parts[1];
                if (text.trim().isEmpty()) {
                    emptyText++;
                }
                if (labels.contains(",")) {
                    multiLabel++;
                } else {
                    singleLabel++;
                }
            }
        } catch (IOException e) {
            return new SplitMetrics(0, 0, 0, 0, 0);
        }

        return new SplitMetrics(total, singleLabel, multiLabel, malformed, emptyText);
    }

    /**
     * Lazily reads up to 'limit' samples from a dataset split.
     */
    public List<TextDatasetSample> sampleComments(String split, int limit) {
        Path path = getSplitFile(split);
        if (!Files.isRegularFile(path)) return Collections.emptyList();

        List<TextDatasetSample> samples = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < limit) {
                String[] parts = line.split("\t");
                if (parts.length < 3) continue;

                String text = parts[0];
                String labelIndicesStr = parts[1];
                String id = parts[2];

                List<String> rawLabels = new ArrayList<>();
                List<String> mappedLabels = new ArrayList<>();

                for (String idxStr : labelIndicesStr.split(",")) {
                    try {
                        int idx = Integer.parseInt(idxStr.trim());
                        String rawName = TextEmotionMapping.getGoEmotionName(idx);
                        rawLabels.add(rawName);
                        EmotionLabel mapped = TextEmotionMapping.mapIndex(idx);
                        mappedLabels.add(mapped.getDisplayName());
                    } catch (NumberFormatException ignored) {}
                }

                samples.add(new TextDatasetSample(id, text, rawLabels, mappedLabels, split));
                count++;
            }
        } catch (IOException e) {
            return Collections.emptyList();
        }

        return samples;
    }
}
