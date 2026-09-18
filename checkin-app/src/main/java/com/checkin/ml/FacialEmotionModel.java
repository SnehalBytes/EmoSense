package com.checkin.ml;

import com.checkin.preprocessing.ImagePreprocessor;

import java.io.File;
import java.util.List;

/**
 * Interface for Facial Emotion Recognition models.
 * Defines the standard ML inference pipeline:
 *   Image
 *    ↓
 *   Preprocessing
 *    ↓
 *   ONNX Inference
 *    ↓
 *   7 Outputs (Angry, Disgust, Fear, Happy, Neutral, Sad, Surprise)
 *    ↓
 *   EmotionPrediction
 */
public interface FacialEmotionModel extends EmotionModel {

    List<String> EXPECTED_LABELS = List.of(
            "ANGRY",
            "DISGUST",
            "FEAR",
            "HAPPY",
            "NEUTRAL",
            "SAD",
            "SURPRISE"
    );

    /**
     * Executes prediction on preprocessed facial image representation.
     */
    EmotionPrediction predict(ImagePreprocessor.PreprocessedImage input) throws Exception;

    /**
     * Convenience method to preprocess a raw image file and execute prediction.
     */
    default EmotionPrediction predict(File imageFile) throws Exception {
        ImagePreprocessor preprocessor = new ImagePreprocessor();
        return predict(preprocessor.process(imageFile));
    }
}
