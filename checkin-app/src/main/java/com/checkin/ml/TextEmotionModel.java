package com.checkin.ml;

import com.checkin.preprocessing.TextPreprocessor;

/**
 * Interface for Text Emotion Recognition models.
 * Defines the real transformer ML inference pipeline:
 *   Text
 *    ↓
 *   Tokenizer
 *    ↓
 *   input_ids, attention_mask
 *    ↓
 *   ONNX Inference
 *    ↓
 *   28 GoEmotions outputs
 *    ↓
 *   GoEmotions label mapping
 *    ↓
 *   EmoSense textual signal
 */
public interface TextEmotionModel extends EmotionModel {

    /**
     * Executes prediction on preprocessed text input.
     */
    EmotionPrediction predict(TextPreprocessor.PreprocessedText input) throws Exception;

    /**
     * Convenience method to process raw text string and execute prediction.
     */
    default EmotionPrediction predict(String text) throws Exception {
        TextPreprocessor preprocessor = new TextPreprocessor();
        return predict(preprocessor.process(text));
    }
}
