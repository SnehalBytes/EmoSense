package com.checkin.services;

import com.checkin.model.CheckInRecord;

import java.util.*;

/**
 * Service calculating aggregate statistics and insights for EmoSense.
 * Strictly limited to actual stored user check-ins.
 * Enforces user isolation and operates with both MySQL JDBC and in-memory stores.
 * Explicitly non-clinical and non-diagnostic.
 */
public class InsightsService {

    public static final List<String> FER2013_CLASSES = List.of(
            "Angry", "Disgust", "Fear", "Happy", "Sad", "Surprise", "Neutral"
    );

    private final HistoryService historyService;

    public InsightsService() {
        this(new HistoryService());
    }

    public InsightsService(HistoryService historyService) {
        this.historyService = historyService != null ? historyService : new HistoryService();
    }

    public record UserInsights(
            int totalCheckIns,
            int photoCount,
            int textCount,
            int multimodalCount,
            double averageConfidence,
            Map<String, Integer> facialDistribution,
            Map<String, Integer> textDistribution,
            int multimodalAlignedCount,
            int multimodalDifferCount,
            String mostFrequentFacial,
            String mostFrequentText,
            List<CheckInRecord> recentRecords
    ) {
        public static UserInsights empty() {
            Map<String, Integer> emptyFacial = new LinkedHashMap<>();
            for (String f : FER2013_CLASSES) emptyFacial.put(f, 0);
            return new UserInsights(0, 0, 0, 0, 0.0, emptyFacial, Collections.emptyMap(), 0, 0, null, null, Collections.emptyList());
        }

        public boolean hasSufficientData() {
            return totalCheckIns >= 3;
        }

        public double alignmentRate() {
            int totalMulti = multimodalAlignedCount + multimodalDifferCount;
            if (totalMulti == 0) return 0.0;
            return ((double) multimodalAlignedCount / totalMulti) * 100.0;
        }

        public Map<String, Integer> signalFrequency() {
            Map<String, Integer> combined = new LinkedHashMap<>();
            facialDistribution.forEach((k, v) -> {
                if (v > 0) combined.put("Facial: " + k, v);
            });
            textDistribution.forEach((k, v) -> {
                if (v > 0) combined.put("Text: " + k, v);
            });
            return combined;
        }
    }

    /**
     * Aggregates emotional insights for the authenticated user from actual stored records.
     * Enforces strict user isolation.
     *
     * @param userId ID of the user
     * @return UserInsights populated strictly from real data
     * @throws InsightsAccessException if database failure occurs
     */
    public UserInsights computeUserInsights(String userId) {
        if (userId == null || userId.isBlank()) {
            return UserInsights.empty();
        }

        List<CheckInRecord> records;
        try {
            records = historyService.getUserHistory(userId);
        } catch (Exception e) {
            throw new InsightsAccessException("Unable to load user insights.", e);
        }

        if (records == null || records.isEmpty()) {
            return UserInsights.empty();
        }

        int total = records.size();
        int photoCount = 0;
        int textCount = 0;
        int multimodalCount = 0;

        double confidenceSum = 0.0;
        int confidenceCount = 0;

        // Initialize all 7 FER2013 classes with 0
        Map<String, Integer> facialDist = new LinkedHashMap<>();
        for (String c : FER2013_CLASSES) {
            facialDist.put(c, 0);
        }

        Map<String, Integer> textDist = new LinkedHashMap<>();

        int alignedCount = 0;
        int differCount = 0;

        for (CheckInRecord r : records) {
            boolean hasPhoto = r.hasPhoto();
            boolean hasText = r.hasThoughts();

            if (hasPhoto && hasText) {
                multimodalCount++;
                if (r.combinedConfidence() > 0) {
                    confidenceSum += r.combinedConfidence();
                    confidenceCount++;
                }
            } else if (hasPhoto) {
                photoCount++;
                if (r.facialConfidence() > 0) {
                    confidenceSum += r.facialConfidence();
                    confidenceCount++;
                }
            } else if (hasText) {
                textCount++;
                if (r.textConfidence() > 0) {
                    confidenceSum += r.textConfidence();
                    confidenceCount++;
                }
            }

            // Facial distribution
            if (hasPhoto && r.facialLabel() != null && !r.facialLabel().isBlank()) {
                String fLabel = canonicalFacialLabel(r.facialLabel());
                facialDist.put(fLabel, facialDist.getOrDefault(fLabel, 0) + 1);
            }

            // Textual distribution
            if (hasText && r.textLabel() != null && !r.textLabel().isBlank()) {
                String tLabel = canonicalTextLabel(r.textLabel());
                textDist.put(tLabel, textDist.getOrDefault(tLabel, 0) + 1);
            }

            // Multimodal alignment
            if (hasPhoto && hasText && r.facialLabel() != null && r.textLabel() != null) {
                if (isAligned(r.facialLabel(), r.textLabel())) {
                    alignedCount++;
                } else {
                    differCount++;
                }
            }
        }

        double avgConfidence = confidenceCount > 0 ? (confidenceSum / confidenceCount) : 0.0;

        // Most frequent facial signal
        String topFacial = null;
        int maxFacial = 0;
        for (Map.Entry<String, Integer> entry : facialDist.entrySet()) {
            if (entry.getValue() > maxFacial) {
                maxFacial = entry.getValue();
                topFacial = entry.getKey();
            }
        }

        // Most frequent textual signal
        String topText = null;
        int maxText = 0;
        for (Map.Entry<String, Integer> entry : textDist.entrySet()) {
            if (entry.getValue() > maxText) {
                maxText = entry.getValue();
                topText = entry.getKey();
            }
        }

        return new UserInsights(
                total,
                photoCount,
                textCount,
                multimodalCount,
                avgConfidence,
                Collections.unmodifiableMap(facialDist),
                Collections.unmodifiableMap(textDist),
                alignedCount,
                differCount,
                topFacial,
                topText,
                records
        );
    }

    private static String canonicalFacialLabel(String label) {
        for (String c : FER2013_CLASSES) {
            if (c.equalsIgnoreCase(label.trim())) {
                return c;
            }
        }
        return label.trim();
    }

    private static String canonicalTextLabel(String label) {
        String trimmed = label.trim();
        if (trimmed.isEmpty()) return "Unknown";
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }

    private static boolean isAligned(String facial, String text) {
        if (facial == null || text == null) return false;
        String f = facial.trim().toLowerCase();
        String t = text.trim().toLowerCase();
        if (f.equals(t)) return true;

        // Sentiment group alignment
        boolean fPositive = f.contains("happy") || f.contains("surprise");
        boolean tPositive = t.contains("joy") || t.contains("happy") || t.contains("love") || t.contains("optimism")
                || t.contains("excitement") || t.contains("amusement") || t.contains("admiration") || t.contains("gratitude")
                || t.contains("pride") || t.contains("relief");

        boolean fNegative = f.contains("sad") || f.contains("angry") || f.contains("fear") || f.contains("disgust");
        boolean tNegative = t.contains("sad") || t.contains("grief") || t.contains("anger") || t.contains("fear")
                || t.contains("disgust") || t.contains("disappointment") || t.contains("annoyance") || t.contains("nervousness");

        return (fPositive && tPositive) || (fNegative && tNegative);
    }

    public HistoryService getHistoryService() {
        return historyService;
    }

    /**
     * Unchecked exception indicating an error occurred while querying insights.
     */
    public static class InsightsAccessException extends RuntimeException {
        public InsightsAccessException(String message) {
            super(message);
        }
        public InsightsAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
