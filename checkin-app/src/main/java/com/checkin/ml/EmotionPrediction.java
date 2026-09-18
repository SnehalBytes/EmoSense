package com.checkin.ml;

import java.util.Collections;
import java.util.Map;

/**
 * Standard prediction entity representing an emotion classification output.
 * Fully transparent regarding modality, probability distribution, and REAL vs MOCK model status.
 */
public record EmotionPrediction(
        String primarySignal,
        double confidence,
        Map<String, Double> probabilityDistribution,
        Modality modality,
        ModelStatus modelStatus,
        String modelName,
        String notes
) {
    public enum Modality {
        FACIAL,
        TEXTUAL,
        MULTIMODAL
    }

    public enum ModelStatus {
        REAL,
        MOCK,
        UNAVAILABLE
    }

    // Factory methods
    public static EmotionPrediction mock(String primarySignal, double confidence, Map<String, Double> probabilities, String modelName, String notes) {
        return new EmotionPrediction(primarySignal, confidence, probabilities != null ? probabilities : Collections.emptyMap(),
                Modality.FACIAL, ModelStatus.MOCK, modelName, notes);
    }

    public static EmotionPrediction mock(String primarySignal, double confidence, Map<String, Double> probabilities, Modality modality, String modelName, String notes) {
        return new EmotionPrediction(primarySignal, confidence, probabilities != null ? probabilities : Collections.emptyMap(),
                modality, ModelStatus.MOCK, modelName, notes);
    }

    public static EmotionPrediction real(String primarySignal, double confidence, Map<String, Double> probabilities, Modality modality, String modelName, String notes) {
        return new EmotionPrediction(primarySignal, confidence, probabilities != null ? probabilities : Collections.emptyMap(),
                modality, ModelStatus.REAL, modelName, notes);
    }

    public static EmotionPrediction unavailable(Modality modality, String errorMessage) {
        return new EmotionPrediction("Analysis unavailable", 0.0, Collections.emptyMap(),
                modality, ModelStatus.UNAVAILABLE, "Inference Engine", errorMessage);
    }

    // Backward-compatibility accessors
    public String primaryEmotion() {
        return primarySignal;
    }

    public Map<String, Double> probabilities() {
        return probabilityDistribution;
    }

    public boolean isPrototypeMock() {
        return modelStatus == ModelStatus.MOCK;
    }

    public boolean isRealModel() {
        return modelStatus == ModelStatus.REAL;
    }

    public boolean isUnavailable() {
        return modelStatus == ModelStatus.UNAVAILABLE;
    }

    public String modelNotes() {
        return notes;
    }
}
