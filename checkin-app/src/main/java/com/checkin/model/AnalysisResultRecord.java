package com.checkin.model;

import java.time.LocalDateTime;

/**
 * Entity representing a stored analysis result record in the relational database.
 * Explicitly tracks model_source (REAL, MOCK, UNAVAILABLE), modality (FACIAL, TEXTUAL, MULTIMODAL),
 * primary_signal, confidence, probability_distribution, and timestamp.
 */
public record AnalysisResultRecord(
        String id,
        String checkInId,
        String primarySignal,
        double confidence,
        String photoSignal,
        String textSignal,
        String combinedSignal,
        String signalAgreement,
        String modelSource,
        String modality,
        String probabilityDistribution,
        LocalDateTime createdAt
) {
    /**
     * Canonical 12-argument constructor.
     */
    public AnalysisResultRecord {
        if (modelSource == null || modelSource.isBlank()) {
            modelSource = "UNAVAILABLE";
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Backward-compatible 10-argument constructor.
     */
    public AnalysisResultRecord(String id, String checkInId, String primarySignal, double confidence,
                                String photoSignal, String textSignal, String combinedSignal,
                                String signalAgreement, String modelSource, LocalDateTime createdAt) {
        this(id, checkInId, primarySignal, confidence, photoSignal, textSignal, combinedSignal,
                signalAgreement, modelSource,
                deriveModality(photoSignal, textSignal), null, createdAt);
    }

    /**
     * Backward-compatible 9-argument constructor.
     */
    public AnalysisResultRecord(String id, String checkInId, String primarySignal, double confidence,
                                String photoSignal, String textSignal, String combinedSignal,
                                String signalAgreement, String modelSource) {
        this(id, checkInId, primarySignal, confidence, photoSignal, textSignal, combinedSignal,
                signalAgreement, modelSource,
                deriveModality(photoSignal, textSignal), null, LocalDateTime.now());
    }

    /**
     * 6-argument convenience constructor matching the required schema fields:
     * model_source, modality, primary_signal, confidence, probability_distribution, created_at.
     */
    public AnalysisResultRecord(String modelSource, String modality, String primarySignal,
                                double confidence, String probabilityDistribution, LocalDateTime createdAt) {
        this(null, null, primarySignal, confidence, null, null, primarySignal,
                "N/A", modelSource, modality, probabilityDistribution, createdAt);
    }

    private static String deriveModality(String photoSignal, String textSignal) {
        if (photoSignal != null && textSignal != null) return "MULTIMODAL";
        if (photoSignal != null) return "FACIAL";
        if (textSignal != null) return "TEXTUAL";
        return "UNKNOWN";
    }

    public boolean isReal() {
        return "REAL".equalsIgnoreCase(modelSource);
    }

    public boolean isMock() {
        return "MOCK".equalsIgnoreCase(modelSource);
    }

    public boolean isUnavailable() {
        return "UNAVAILABLE".equalsIgnoreCase(modelSource);
    }

    /**
     * Converts this database record to a UI-ready AnalysisResult object,
     * restoring genuine model status, probability distributions, and fusion insights.
     */
    public AnalysisResult toAnalysisResult() {
        java.util.Map<String, Double> facialProbs = new java.util.HashMap<>();
        java.util.Map<String, Double> textProbs = new java.util.HashMap<>();

        if (probabilityDistribution != null && !probabilityDistribution.isBlank()) {
            parseDistributions(probabilityDistribution, facialProbs, textProbs);
        }

        com.checkin.ml.EmotionPrediction facialPred = null;
        if (photoSignal != null && !photoSignal.isBlank()) {
            if (isUnavailable() || "Analysis unavailable".equalsIgnoreCase(photoSignal)) {
                facialPred = com.checkin.ml.EmotionPrediction.unavailable(
                        com.checkin.ml.EmotionPrediction.Modality.FACIAL, "Analysis unavailable");
            } else if (isReal()) {
                facialPred = com.checkin.ml.EmotionPrediction.real(
                        photoSignal, confidence, facialProbs,
                        com.checkin.ml.EmotionPrediction.Modality.FACIAL, "FER2013 ONNX Model", null);
            } else {
                facialPred = com.checkin.ml.EmotionPrediction.mock(
                        photoSignal, confidence, facialProbs,
                        com.checkin.ml.EmotionPrediction.Modality.FACIAL, "FER2013 Prototype Model", null);
            }
        }

        com.checkin.ml.EmotionPrediction textPred = null;
        if (textSignal != null && !textSignal.isBlank()) {
            if (isUnavailable() || "Analysis unavailable".equalsIgnoreCase(textSignal)) {
                textPred = com.checkin.ml.EmotionPrediction.unavailable(
                        com.checkin.ml.EmotionPrediction.Modality.TEXTUAL, "Analysis unavailable");
            } else if (isReal()) {
                textPred = com.checkin.ml.EmotionPrediction.real(
                        textSignal, confidence, textProbs,
                        com.checkin.ml.EmotionPrediction.Modality.TEXTUAL, "GoEmotions ONNX Model",
                        "Raw emotion: " + textSignal);
            } else {
                textPred = com.checkin.ml.EmotionPrediction.mock(
                        textSignal, confidence, textProbs,
                        com.checkin.ml.EmotionPrediction.Modality.TEXTUAL, "GoEmotions Prototype Model", null);
            }
        }

        com.checkin.services.MultimodalAnalysisService.CombinedInsight insight = null;
        if (photoSignal != null && textSignal != null) {
            String combined = combinedSignal != null ? combinedSignal : primarySignal;
            String note = signalAgreement != null ? signalAgreement : "Multimodal Fusion Analysis";
            insight = new com.checkin.services.MultimodalAnalysisService.CombinedInsight(
                    combined, confidence, note, "Synthesized from facial expression and textual emotional cues."
            );
        }

        return new AnalysisResult(
                photoSignal,
                confidence,
                textSignal,
                confidence,
                combinedSignal != null ? combinedSignal : primarySignal,
                confidence,
                facialPred,
                textPred,
                insight
        );
    }

    private static void parseDistributions(String raw,
                                           java.util.Map<String, Double> facial,
                                           java.util.Map<String, Double> text) {
        try {
            String[] sections = raw.split("\\|");
            for (String section : sections) {
                boolean isTextSection = section.startsWith("TEXT=");
                boolean isFacialSection = section.startsWith("FACIAL=");
                String content = section;
                if (isTextSection) content = section.substring(5);
                if (isFacialSection) content = section.substring(7);

                String[] pairs = content.split("[,;]");
                for (String pair : pairs) {
                    String[] kv = pair.split("[:=]");
                    if (kv.length == 2) {
                        String key = kv[0].trim();
                        double val = Double.parseDouble(kv[1].trim());
                        if (isTextSection) {
                            text.put(key, val);
                        } else {
                            facial.put(key, val);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Gracefully ignore parsing errors on legacy formats
        }
    }
}
