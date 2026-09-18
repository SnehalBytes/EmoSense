package com.checkin.model;

import java.util.List;

/**
 * Represents a single text sample from the GoEmotions dataset.
 */
public record TextDatasetSample(
        String id,
        String text,
        List<String> rawEmotionLabels,
        List<String> mappedEmotionLabels,
        String split // "train", "dev", or "test"
) {
    public boolean isMultiLabel() {
        return rawEmotionLabels != null && rawEmotionLabels.size() > 1;
    }
}
