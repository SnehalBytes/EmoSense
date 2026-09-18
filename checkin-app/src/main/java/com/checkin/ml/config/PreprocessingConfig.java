package com.checkin.ml.config;

import java.util.Arrays;

/**
 * Preprocessing configuration parameters for neural network input preparation.
 * Stores target dimensions, color space options, and normalization values.
 */
public record PreprocessingConfig(
        int targetWidth,
        int targetHeight,
        boolean grayscale,
        boolean normalizeToZeroOne,
        float[] mean,
        float[] stdDev,
        boolean toRgb
) {
    public static PreprocessingConfig defaultFacial() {
        // Standard FER2013: 48x48 single-channel grayscale, normalized to [0, 1]
        return new PreprocessingConfig(
                48, 48,
                true,
                true,
                new float[]{0.5f},
                new float[]{0.5f},
                false
        );
    }

    public static PreprocessingConfig defaultViTFacial() {
        // ViT FER2013: 224x224 RGB, normalized with mean 0.5, std 0.5
        return new PreprocessingConfig(
                224, 224,
                false,
                true,
                new float[]{0.5f, 0.5f, 0.5f},
                new float[]{0.5f, 0.5f, 0.5f},
                true
        );
    }

    public static PreprocessingConfig defaultFaceDetection() {
        // Standard Face Detector (e.g., UltraFace / SSD): 320x240 or 300x300 RGB
        return new PreprocessingConfig(
                300, 300,
                false,
                true,
                new float[]{127.0f, 127.0f, 127.0f},
                new float[]{128.0f, 128.0f, 128.0f},
                true
        );
    }

    public static PreprocessingConfig defaultText() {
        return new PreprocessingConfig(
                0, 0,
                false,
                false,
                new float[0],
                new float[0],
                false
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PreprocessingConfig other)) return false;
        return targetWidth == other.targetWidth
                && targetHeight == other.targetHeight
                && grayscale == other.grayscale
                && normalizeToZeroOne == other.normalizeToZeroOne
                && toRgb == other.toRgb
                && Arrays.equals(mean, other.mean)
                && Arrays.equals(stdDev, other.stdDev);
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(targetWidth);
        result = 31 * result + Integer.hashCode(targetHeight);
        result = 31 * result + Boolean.hashCode(grayscale);
        result = 31 * result + Boolean.hashCode(normalizeToZeroOne);
        result = 31 * result + Arrays.hashCode(mean);
        result = 31 * result + Arrays.hashCode(stdDev);
        result = 31 * result + Boolean.hashCode(toRgb);
        return result;
    }
}
