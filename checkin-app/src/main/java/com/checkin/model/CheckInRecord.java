package com.checkin.model;

import java.time.LocalDateTime;

/**
 * Entity representing an individual check-in entry saved by a user.
 * Prepared for relational database persistence (MySQL JDBC).
 */
public record CheckInRecord(
        String id,
        String userId,
        String photoFileName,
        String thoughtsText,
        String facialLabel,
        double facialConfidence,
        String textLabel,
        double textConfidence,
        String combinedLabel,
        double combinedConfidence,
        LocalDateTime createdAt
) {
    public boolean hasPhoto() {
        return photoFileName != null && !photoFileName.isBlank();
    }

    public boolean hasThoughts() {
        return thoughtsText != null && !thoughtsText.isBlank();
    }
}
