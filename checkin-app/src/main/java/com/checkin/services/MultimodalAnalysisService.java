package com.checkin.services;

import com.checkin.ml.EmotionPrediction;

/**
 * Service synthesizing multimodal signals (facial expression + textual cues).
 * Employs configurable weighted fusion and signal agreement comparison.
 *
 * Explicitly labeled as "Prototype Multimodal Fusion".
 * Not presented as a clinical or medically validated diagnosis.
 */
public class MultimodalAnalysisService {

    private double photoWeight;
    private double textWeight;

    public MultimodalAnalysisService() {
        this(0.5, 0.5); // Default balanced weighting
    }

    public MultimodalAnalysisService(double photoWeight, double textWeight) {
        setWeights(photoWeight, textWeight);
    }

    public void setWeights(double photoWeight, double textWeight) {
        if (photoWeight < 0 || textWeight < 0 || (photoWeight + textWeight) <= 0) {
            this.photoWeight = 0.5;
            this.textWeight = 0.5;
        } else {
            double total = photoWeight + textWeight;
            this.photoWeight = photoWeight / total;
            this.textWeight = textWeight / total;
        }
    }

    public double getPhotoWeight() {
        return photoWeight;
    }

    public double getTextWeight() {
        return textWeight;
    }

    public record CombinedInsight(
            String combinedLabel,
            double combinedConfidence,
            String agreementStatus,
            String fusionSummary
    ) {}

    public CombinedInsight fuse(EmotionPrediction facial, EmotionPrediction textual) {
        if (facial == null && textual == null) {
            return new CombinedInsight("No Signal", 0.0, "None", "No inputs were provided for analysis.");
        }
        if (facial == null) {
            return new CombinedInsight(textual.primaryEmotion(), textual.confidence(), "Textual Cue Only",
                    "Analysis based solely on written thoughts. Facial signal was not provided.");
        }
        if (textual == null) {
            return new CombinedInsight(facial.primaryEmotion(), facial.confidence(), "Facial Signal Only",
                    "Analysis based solely on static facial photo. Textual cues were not provided.");
        }

        boolean fError = facial.primaryEmotion() != null && (facial.primaryEmotion().contains("Error") || facial.primaryEmotion().contains("Unable to complete") || facial.primaryEmotion().toLowerCase().contains("unavailable"));
        boolean tError = textual.primaryEmotion() != null && (textual.primaryEmotion().contains("Error") || textual.primaryEmotion().contains("Unable to complete") || textual.primaryEmotion().toLowerCase().contains("unavailable"));

        if (fError && tError) {
            return new CombinedInsight("Analysis unavailable", 0.0, "Analysis Error",
                    "Emotion analysis could not be completed on provided inputs.");
        }
        if (fError) {
            return new CombinedInsight(textual.primaryEmotion(), textual.confidence(), "Textual Cue Only",
                    "Facial analysis could not be completed. Showing textual cues.");
        }
        if (tError) {
            return new CombinedInsight(facial.primaryEmotion(), facial.confidence(), "Facial Signal Only",
                    "Textual analysis could not be completed. Showing facial cues.");
        }

        // Both modalities present: perform weighted fusion and agreement comparison
        double weightedConfidence = (facial.confidence() * photoWeight) + (textual.confidence() * textWeight);

        String fLabel = facial.primaryEmotion().toLowerCase();
        String tLabel = textual.primaryEmotion().toLowerCase();

        boolean fPositive = fLabel.contains("happy") || fLabel.contains("joy") || fLabel.contains("positive") || fLabel.contains("content");
        boolean tPositive = tLabel.contains("happy") || tLabel.contains("joy") || tLabel.contains("positive") || tLabel.contains("content") || tLabel.contains("calm");

        boolean fNegative = fLabel.contains("sad") || fLabel.contains("angry") || fLabel.contains("fear") || fLabel.contains("disgust") || fLabel.contains("frustrat");
        boolean tNegative = tLabel.contains("sad") || tLabel.contains("angry") || tLabel.contains("frustrat") || tLabel.contains("stress") || tLabel.contains("overwhelm");

        boolean fNeutral = fLabel.contains("neutral");
        boolean tNeutral = tLabel.contains("neutral");

        String combinedLabel;
        String agreementStatus;
        String summary;

        if (fNegative && tNegative) {
            combinedLabel = "Emotional Strain / Challenging Signal";
            agreementStatus = "Aligned Signals";
            summary = "Signals indicate emotional strain across both inputs. Both your physical expression and words reflect challenging emotional tones.";
        } else if (fPositive && tPositive) {
            combinedLabel = "Positive / Uplifted Signal";
            agreementStatus = "Aligned Signals";
            summary = "Signals indicate positive emotional balance across both inputs.";
        } else if ((fPositive && tNegative) || (fNegative && tPositive) || (fPositive != tPositive && (fPositive || tPositive))) {
            combinedLabel = "Mixed Emotional Signals";
            agreementStatus = "Signals Differ";
            summary = "Signals Differ: Your facial expression and written thoughts reflect different nuances. "
                    + "Different modalities naturally capture distinct aspects of our emotional experience. Disagreement is normal and expected.";
        } else if (fNeutral && tNeutral) {
            combinedLabel = "Neutral / Balanced Signal";
            agreementStatus = "Aligned Neutral Baseline";
            summary = "Signals reflect a neutral baseline across both modalities.";
        } else if (fNeutral || tNeutral) {
            String activeLabel = !fNeutral ? facial.primaryEmotion() : textual.primaryEmotion();
            combinedLabel = "Subtle / Partial Signal (" + activeLabel + ")";
            agreementStatus = "Signals Differ";
            summary = "One modality indicated a neutral baseline while the other indicated " + activeLabel + ".";
        } else {
            combinedLabel = "Complex Multimodal Signals";
            agreementStatus = "Signals Differ";
            summary = "Signals capture distinct emotional dimensions across facial and textual expressions.";
        }

        return new CombinedInsight(combinedLabel, weightedConfidence, agreementStatus, summary);
    }
}
