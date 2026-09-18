package com.checkin.model;

/**
 * Data model for an individual emotional signal (facial, textual, or multimodal).
 * Structured for persistence in database storage (e.g. MySQL via JDBC).
 */
public record EmotionSignal(
        String signalType, // "FACIAL", "TEXTUAL", "MULTIMODAL"
        String label,
        double confidence,
        boolean isPrototype,
        String notes
) {}
