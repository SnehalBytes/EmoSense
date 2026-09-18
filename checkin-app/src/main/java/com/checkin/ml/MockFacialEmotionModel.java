package com.checkin.ml;

import com.checkin.preprocessing.ImagePreprocessor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Prototype / Mock implementation for facial emotion analysis.
 * Explicitly marked as a prototype stand-in until real ML inference (e.g., ONNX model) is integrated.
 * Does NOT generate fake random percentages presented as real machine learning.
 */
public class MockFacialEmotionModel implements FacialEmotionModel {

    @Override
    public EmotionPrediction predict(ImagePreprocessor.PreprocessedImage input) {
        String label = "Neutral Expression";
        double baselineConfidence = 70.0;

        Map<String, Double> probs = new LinkedHashMap<>();
        probs.put("Angry", 5.0);
        probs.put("Disgust", 2.0);
        probs.put("Fear", 3.0);
        probs.put("Happy", 10.0);
        probs.put("Neutral", 70.0);
        probs.put("Sad", 5.0);
        probs.put("Surprise", 5.0);

        String notes = "Prototype / Mock Facial Analysis — Image decoded ("
                + input.originalWidth() + "x" + input.originalHeight() + " px). Real neural model pending.";

        return EmotionPrediction.mock(label, baselineConfidence, probs, EmotionPrediction.Modality.FACIAL, getModelName(), notes);
    }

    @Override
    public String getModelName() {
        return "Prototype / Mock Facial Signal Engine";
    }

    @Override
    public boolean isTrainedModel() {
        return false;
    }
}
