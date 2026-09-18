package com.checkin.ml;

import com.checkin.preprocessing.TextPreprocessor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Prototype / Mock implementation for textual emotion analysis using keyword heuristics.
 * Explicitly marked as a prototype stand-in until real ML inference (e.g., GoEmotions fine-tuned transformer) is integrated.
 */
public class MockTextEmotionModel implements TextEmotionModel {

    @Override
    public EmotionPrediction predict(TextPreprocessor.PreprocessedText input) {
        List<String> tokens = input.tokens();

        String detectedLabel = "Neutral Cue";
        double confidence = 70.0;

        if (containsAny(tokens, "happy", "joy", "great", "glad", "excited", "good", "love", "wonderful", "peaceful", "content")) {
            detectedLabel = "Positive / Happy Cue";
            confidence = 75.0;
        } else if (containsAny(tokens, "sad", "unhappy", "depressed", "down", "crying", "hurt", "grief", "lonely", "disappointed")) {
            detectedLabel = "Sad / Somber Cue";
            confidence = 75.0;
        } else if (containsAny(tokens, "angry", "furious", "mad", "annoyed", "irritated", "hate", "frustrated", "rage")) {
            detectedLabel = "Angry / Frustrated Cue";
            confidence = 75.0;
        } else if (containsAny(tokens, "stress", "stressed", "overwhelmed", "anxious", "anxiety", "worried", "panic", "nervous")) {
            detectedLabel = "Stressed / Overwhelmed Cue";
            confidence = 75.0;
        } else if (containsAny(tokens, "calm", "relax", "relaxed", "fine", "okay", "chill", "serene")) {
            detectedLabel = "Calm / Grounded Cue";
            confidence = 75.0;
        }

        Map<String, Double> probs = new LinkedHashMap<>();
        probs.put(detectedLabel, confidence);
        probs.put("Other Cues", 100.0 - confidence);

        String notes = "Prototype / Mock Textual Analysis — Analyzed " + tokens.size() + " words. Real NLP model pending.";
        return EmotionPrediction.mock(detectedLabel, confidence, probs, EmotionPrediction.Modality.TEXTUAL, getModelName(), notes);
    }

    private boolean containsAny(List<String> tokens, String... targets) {
        for (String token : tokens) {
            for (String target : targets) {
                if (token.equalsIgnoreCase(target)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getModelName() {
        return "Prototype / Mock Textual Heuristics Engine";
    }

    @Override
    public boolean isTrainedModel() {
        return false;
    }
}
