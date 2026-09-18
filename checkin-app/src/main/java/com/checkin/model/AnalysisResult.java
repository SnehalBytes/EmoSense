package com.checkin.model;

import com.checkin.ml.EmotionPrediction;
import com.checkin.services.MultimodalAnalysisService.CombinedInsight;

import java.util.Collections;
import java.util.Map;

/**
 * Result of running analysis on a check-in. Any of the three signals may be
 * absent depending on which inputs were provided (facialLabel is null when
 * there was no photo; textLabel is null when there were no thoughts).
 *
 * Fully encapsulates genuine model predictions, confidence percentages,
 * class probability distributions, and multimodal fusion insights.
 */
public class AnalysisResult {

    private final String facialLabel;
    private final double facialConfidence;

    private final String textLabel;
    private final double textConfidence;

    private final String combinedLabel;
    private final double combinedConfidence;

    private final EmotionPrediction facialPrediction;
    private final EmotionPrediction textPrediction;
    private final CombinedInsight combinedInsight;

    public AnalysisResult(String facialLabel, double facialConfidence,
                          String textLabel, double textConfidence,
                          String combinedLabel, double combinedConfidence) {
        this(facialLabel, facialConfidence, textLabel, textConfidence, combinedLabel, combinedConfidence,
             null, null, null);
    }

    public AnalysisResult(String facialLabel, double facialConfidence,
                          String textLabel, double textConfidence,
                          String combinedLabel, double combinedConfidence,
                          EmotionPrediction facialPrediction,
                          EmotionPrediction textPrediction,
                          CombinedInsight combinedInsight) {
        this.facialLabel = facialLabel;
        this.facialConfidence = facialConfidence;
        this.textLabel = textLabel;
        this.textConfidence = textConfidence;
        this.combinedLabel = combinedLabel;
        this.combinedConfidence = combinedConfidence;
        this.facialPrediction = facialPrediction;
        this.textPrediction = textPrediction;
        this.combinedInsight = combinedInsight;
    }

    public boolean hasFacial() {
        return facialLabel != null;
    }

    public boolean hasText() {
        return textLabel != null;
    }

    public String getFacialLabel() {
        return facialLabel;
    }

    public double getFacialConfidence() {
        return facialConfidence;
    }

    public String getTextLabel() {
        return textLabel;
    }

    public double getTextConfidence() {
        return textConfidence;
    }

    public String getCombinedLabel() {
        return combinedLabel;
    }

    public double getCombinedConfidence() {
        return combinedConfidence;
    }

    public EmotionPrediction getFacialPrediction() {
        return facialPrediction;
    }

    public EmotionPrediction getTextPrediction() {
        return textPrediction;
    }

    public CombinedInsight getCombinedInsight() {
        return combinedInsight;
    }

    public Map<String, Double> getFacialProbabilities() {
        if (facialPrediction != null && facialPrediction.probabilityDistribution() != null) {
            return facialPrediction.probabilityDistribution();
        }
        return Collections.emptyMap();
    }

    public Map<String, Double> getTextProbabilities() {
        if (textPrediction != null && textPrediction.probabilityDistribution() != null) {
            return textPrediction.probabilityDistribution();
        }
        return Collections.emptyMap();
    }

    public String getRawTextEmotion() {
        if (textPrediction != null && textPrediction.notes() != null) {
            String notes = textPrediction.notes();
            int idx = notes.indexOf("Raw emotion: ");
            if (idx != -1) {
                int end = notes.indexOf("(", idx);
                if (end != -1) {
                    return notes.substring(idx + 13, end).trim();
                }
            }
        }
        return null;
    }

    public boolean isFacialRealModel() {
        return facialPrediction != null && facialPrediction.isRealModel();
    }

    public boolean isTextRealModel() {
        return textPrediction != null && textPrediction.isRealModel();
    }

    public boolean isFacialUnavailable() {
        if (facialLabel != null && facialLabel.equalsIgnoreCase("Analysis unavailable")) {
            return true;
        }
        return facialPrediction != null && facialPrediction.isUnavailable();
    }

    public boolean isTextUnavailable() {
        if (textLabel != null && textLabel.equalsIgnoreCase("Analysis unavailable")) {
            return true;
        }
        return textPrediction != null && textPrediction.isUnavailable();
    }
}
