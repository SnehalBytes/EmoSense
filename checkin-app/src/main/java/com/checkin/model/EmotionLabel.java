package com.checkin.model;

/**
 * Standardized emotion signal labels used across EmoSense.
 * Contains both standard facial expression classes and broader textual signal categories.
 */
public enum EmotionLabel {

    // 7 Standard Facial Expression Categories
    ANGRY("Angry", SignalType.FACIAL),
    DISGUST("Disgust", SignalType.FACIAL),
    FEAR("Fear", SignalType.FACIAL),
    HAPPY("Happy", SignalType.FACIAL),
    NEUTRAL("Neutral", SignalType.FACIAL),
    SAD("Sad", SignalType.FACIAL),
    SURPRISE("Surprise", SignalType.FACIAL),

    // Additional / Broader Textual Expression Categories
    FEARFUL("Fearful", SignalType.TEXTUAL),
    STRESSED("Stressed", SignalType.TEXTUAL),
    OVERWHELMED("Overwhelmed", SignalType.TEXTUAL),
    CALM("Calm", SignalType.TEXTUAL);

    public enum SignalType {
        FACIAL,
        TEXTUAL,
        BOTH
    }

    private final String displayName;
    private final SignalType primarySignal;

    EmotionLabel(String displayName, SignalType primarySignal) {
        this.displayName = displayName;
        this.primarySignal = primarySignal;
    }

    public String getDisplayName() {
        return displayName;
    }

    public SignalType getPrimarySignal() {
        return primarySignal;
    }

    public static EmotionLabel fromString(String name) {
        if (name == null || name.isBlank()) return NEUTRAL;
        String clean = name.trim().toUpperCase();
        for (EmotionLabel label : values()) {
            if (label.name().equalsIgnoreCase(clean) || label.displayName.equalsIgnoreCase(clean)) {
                return label;
            }
        }
        return NEUTRAL;
    }
}
