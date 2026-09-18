package com.checkin.ml;

/**
 * Encapsulates the output of an emotion recognition model inference.
 * Explicitly tracks whether the result is from a prototype/mock engine or a verified ML model.
 */
public record ModelPrediction(
        String label,
        double confidence,
        boolean isPrototypeMock,
        String modelName,
        String notes
) {
    public static ModelPrediction prototype(String label, double confidence, String modelName, String notes) {
        return new ModelPrediction(label, confidence, true, modelName, notes);
    }

    public static ModelPrediction production(String label, double confidence, String modelName) {
        return new ModelPrediction(label, confidence, false, modelName, "Trained model inference");
    }
}
