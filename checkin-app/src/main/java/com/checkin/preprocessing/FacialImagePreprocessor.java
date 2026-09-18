package com.checkin.preprocessing;

import java.io.File;
import java.io.IOException;

/**
 * Dedicated image preprocessor for facial expression photos.
 * Implements preliminary format and bounds validation.
 *
 * NOTE: Exact tensor transformations (scaling to 48x48 vs 224x224, mean/std normalization,
 * channel order) will be applied once the specific model architecture (e.g. ResNet/Mini_Xception)
 * is confirmed.
 */
public class FacialImagePreprocessor extends ImagePreprocessor {

    public FacialImagePreprocessor() {
        super();
    }

    public PreprocessedImage prepareFacialImage(File file) throws IllegalArgumentException, IOException {
        return process(file);
    }
}
