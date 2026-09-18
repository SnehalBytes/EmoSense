package com.checkin.model;

import java.util.Collections;
import java.util.Map;

/**
 * Encapsulates verified counts, formats, splits, and class distributions
 * for the EmoSense facial and textual datasets.
 */
public record DatasetStatistics(
        // Facial Dataset
        boolean facialAvailable,
        int facialTrainTotal,
        int facialTestTotal,
        int facialGrandTotal,
        Map<String, Integer> facialTrainClassCounts,
        Map<String, Integer> facialTestClassCounts,
        String facialImageFormat,
        String facialResolution,
        int facialCorruptedCount,

        // Text Dataset (GoEmotions)
        boolean textAvailable,
        int textTrainTotal,
        int textDevTotal,
        int textTestTotal,
        int textGrandTotal,
        int textSingleLabelCount,
        int textMultiLabelCount,
        int textMalformedRowCount,
        int textEmptyTextCount,
        int textEmotionCategoryCount
) {
    public static DatasetStatistics empty() {
        return new DatasetStatistics(
                false, 0, 0, 0, Collections.emptyMap(), Collections.emptyMap(), "Unknown", "Unknown", 0,
                false, 0, 0, 0, 0, 0, 0, 0, 0, 0
        );
    }

    public String formatSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("         EMOSENSE DATASET VERIFIED SUMMARY          \n");
        sb.append("====================================================\n");
        sb.append("1. FACIAL DATASET (FER2013 formatted):\n");
        sb.append("   - Available: ").append(facialAvailable).append("\n");
        sb.append("   - Format: ").append(facialImageFormat).append("\n");
        sb.append("   - Native Resolution: ").append(facialResolution).append("\n");
        sb.append("   - Train Images: ").append(facialTrainTotal).append("\n");
        sb.append("   - Test Images: ").append(facialTestTotal).append("\n");
        sb.append("   - Grand Total Images: ").append(facialGrandTotal).append("\n");
        sb.append("   - Corrupted/Unreadable: ").append(facialCorruptedCount).append("\n");
        sb.append("   - Train Class Distribution:\n");
        facialTrainClassCounts.forEach((cls, count) ->
                sb.append(String.format("       • %-10s : %d\n", cls, count)));
        sb.append("   - Test Class Distribution:\n");
        facialTestClassCounts.forEach((cls, count) ->
                sb.append(String.format("       • %-10s : %d\n", cls, count)));

        sb.append("\n2. TEXT DATASET (GoEmotions):\n");
        sb.append("   - Available: ").append(textAvailable).append("\n");
        sb.append("   - Emotion Classes: ").append(textEmotionCategoryCount).append(" categories\n");
        sb.append("   - Train Split (train.tsv): ").append(textTrainTotal).append(" rows\n");
        sb.append("   - Dev Split (dev.tsv):     ").append(textDevTotal).append(" rows\n");
        sb.append("   - Test Split (test.tsv):   ").append(textTestTotal).append(" rows\n");
        sb.append("   - Grand Total Samples:     ").append(textGrandTotal).append(" comments\n");
        sb.append("   - Single-label Samples:    ").append(textSingleLabelCount).append("\n");
        sb.append("   - Multi-label Samples:     ").append(textMultiLabelCount).append("\n");
        sb.append("   - Malformed Rows:          ").append(textMalformedRowCount).append("\n");
        sb.append("   - Empty Texts:             ").append(textEmptyTextCount).append("\n");
        sb.append("====================================================\n");
        return sb.toString();
    }
}
