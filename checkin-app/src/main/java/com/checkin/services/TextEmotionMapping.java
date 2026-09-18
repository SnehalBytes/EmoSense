package com.checkin.services;

import com.checkin.model.EmotionLabel;

import java.util.*;

/**
 * Explicit mapping layer between GoEmotions' 28 fine-grained emotion categories
 * and EmoSense's 8 broader textual emotional signal categories.
 *
 * NOTE ON TAXONOMY & RESPONSIBLE DESIGN:
 * The GoEmotions dataset (Demszky et al., 2020) defines 28 categories derived from Reddit comments.
 * EmoSense aggregates these into high-level textual signals (Happy, Sad, Angry, Fearful, Neutral,
 * Stressed, Overwhelmed, Calm) for check-in reflection.
 * This grouping is an approximate prototype mapping for user self-reflection and academic prototyping;
 * it is NOT a medical, psychiatric, or clinically validated diagnostic system.
 */
public final class TextEmotionMapping {

    // All 28 standard GoEmotions categories (ordered by dataset index 0..27)
    public static final List<String> GO_EMOTIONS_28 = List.of(
            "admiration",     // 0
            "amusement",      // 1
            "anger",          // 2
            "annoyance",      // 3
            "approval",       // 4
            "caring",         // 5
            "confusion",      // 6
            "curiosity",      // 7
            "desire",         // 8
            "disappointment", // 9
            "disapproval",    // 10
            "disgust",        // 11
            "embarrassment",  // 12
            "excitement",     // 13
            "fear",           // 14
            "gratitude",      // 15
            "grief",          // 16
            "joy",            // 17
            "love",           // 18
            "nervousness",    // 19
            "optimism",       // 20
            "pride",          // 21
            "realization",    // 22
            "relief",         // 23
            "remorse",        // 24
            "sadness",        // 25
            "surprise",       // 26
            "neutral"         // 27
    );

    private static final Map<String, EmotionLabel> GO_TO_EMOSENSE = new HashMap<>();

    static {
        // Happy / Positive signals
        GO_TO_EMOSENSE.put("joy", EmotionLabel.HAPPY);
        GO_TO_EMOSENSE.put("amusement", EmotionLabel.HAPPY);
        GO_TO_EMOSENSE.put("approval", EmotionLabel.HAPPY);
        GO_TO_EMOSENSE.put("excitement", EmotionLabel.HAPPY);
        GO_TO_EMOSENSE.put("gratitude", EmotionLabel.HAPPY);
        GO_TO_EMOSENSE.put("love", EmotionLabel.HAPPY);
        GO_TO_EMOSENSE.put("optimism", EmotionLabel.HAPPY);
        GO_TO_EMOSENSE.put("pride", EmotionLabel.HAPPY);
        GO_TO_EMOSENSE.put("admiration", EmotionLabel.HAPPY);
        GO_TO_EMOSENSE.put("desire", EmotionLabel.HAPPY);

        // Sad / Low valence signals
        GO_TO_EMOSENSE.put("sadness", EmotionLabel.SAD);
        GO_TO_EMOSENSE.put("disappointment", EmotionLabel.SAD);
        GO_TO_EMOSENSE.put("embarrassment", EmotionLabel.SAD);
        GO_TO_EMOSENSE.put("grief", EmotionLabel.SAD);
        GO_TO_EMOSENSE.put("remorse", EmotionLabel.SAD);

        // Angry / Irritated signals
        GO_TO_EMOSENSE.put("anger", EmotionLabel.ANGRY);
        GO_TO_EMOSENSE.put("annoyance", EmotionLabel.ANGRY);
        GO_TO_EMOSENSE.put("disapproval", EmotionLabel.ANGRY);
        GO_TO_EMOSENSE.put("disgust", EmotionLabel.DISGUST);

        // Fearful / Anxious signals
        GO_TO_EMOSENSE.put("fear", EmotionLabel.FEARFUL);
        GO_TO_EMOSENSE.put("nervousness", EmotionLabel.STRESSED);

        // Cognitive / Overwhelm signals
        GO_TO_EMOSENSE.put("confusion", EmotionLabel.OVERWHELMED);
        GO_TO_EMOSENSE.put("curiosity", EmotionLabel.SURPRISE);
        GO_TO_EMOSENSE.put("realization", EmotionLabel.SURPRISE);
        GO_TO_EMOSENSE.put("surprise", EmotionLabel.SURPRISE);

        // Calm / Grounded signals
        GO_TO_EMOSENSE.put("relief", EmotionLabel.CALM);
        GO_TO_EMOSENSE.put("caring", EmotionLabel.CALM);

        // Neutral
        GO_TO_EMOSENSE.put("neutral", EmotionLabel.NEUTRAL);
    }

    private TextEmotionMapping() {}

    /**
     * Maps a single GoEmotions label name (e.g. "joy" or "annoyance")
     * to an EmoSense high-level EmotionLabel.
     */
    public static EmotionLabel map(String goEmotion) {
        if (goEmotion == null) return EmotionLabel.NEUTRAL;
        String clean = goEmotion.trim().toLowerCase();
        return GO_TO_EMOSENSE.getOrDefault(clean, EmotionLabel.NEUTRAL);
    }

    public static String mapGoEmotionToSignal(String goEmotion) {
        EmotionLabel label = map(goEmotion);
        return label != null ? label.getDisplayName() : "Neutral";
    }

    /**
     * Maps a GoEmotions numeric index (0..27) to an EmotionLabel.
     */
    public static EmotionLabel mapIndex(int index) {
        if (index >= 0 && index < GO_EMOTIONS_28.size()) {
            return map(GO_EMOTIONS_28.get(index));
        }
        return EmotionLabel.NEUTRAL;
    }

    /**
     * Returns the name of the GoEmotions label by index.
     */
    public static String getGoEmotionName(int index) {
        if (index >= 0 && index < GO_EMOTIONS_28.size()) {
            return GO_EMOTIONS_28.get(index);
        }
        return "unknown";
    }

    /**
     * Returns an unmodifiable view of all 28 GoEmotions categories.
     */
    public static List<String> getAllGoEmotions() {
        return GO_EMOTIONS_28;
    }
}
