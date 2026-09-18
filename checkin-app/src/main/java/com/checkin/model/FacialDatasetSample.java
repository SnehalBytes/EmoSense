package com.checkin.model;

import java.nio.file.Path;

/**
 * Represents a single sample metadata item from the facial dataset.
 * Does NOT hold full image pixel buffer in memory to ensure lazy loading.
 */
public record FacialDatasetSample(
        Path filePath,
        String fileName,
        String emotionClass,
        String split, // "train" or "test"
        long fileSizeBytes,
        int width,
        int height
) {
    public boolean isValid() {
        return filePath != null && fileSizeBytes > 0 && width > 0 && height > 0;
    }
}
