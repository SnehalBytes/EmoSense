package com.checkin.ml;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.checkin.ml.EmotionPrediction.Modality;
import com.checkin.ml.config.TextModelConfig;
import com.checkin.preprocessing.TextPreprocessor;
import com.checkin.services.TextEmotionMapping;

import java.nio.LongBuffer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Real ONNX Text Emotion Recognition Model implementation.
 * Follows the pipeline:
 *   Text -> Tokenizer -> input_ids, attention_mask -> ONNX inference -> 28 GoEmotions outputs
 *   -> GoEmotions label mapping -> EmoSense textual signal
 *
 * If the model file or tokenizer is missing, returns UNAVAILABLE.
 * Never returns Neutral as a fallback.
 */
public class OnnxTextEmotionModel implements TextEmotionModel {

    private final ModelManager modelManager;
    private final TextModelConfig config;
    private final TextTokenizer tokenizer;

    public OnnxTextEmotionModel() {
        this(ModelManager.getInstance(), ModelManager.getInstance().getTextConfig(),
                new OnnxTextTokenizer(ModelManager.getInstance(), ModelManager.getInstance().getTextConfig()));
    }

    public OnnxTextEmotionModel(ModelManager modelManager, TextModelConfig config, TextTokenizer tokenizer) {
        this.modelManager = modelManager;
        this.config = config != null ? config : new TextModelConfig();
        this.tokenizer = tokenizer != null ? tokenizer : new OnnxTextTokenizer(modelManager, this.config);
    }

    @Override
    public EmotionPrediction predict(TextPreprocessor.PreprocessedText input) {
        // 1. Verify model availability
        if (!modelManager.isTextModelAvailable()) {
            return EmotionPrediction.unavailable(
                    Modality.TEXTUAL,
                    "Text model file not found in " + config.getModelPath() + ". Model status: UNAVAILABLE."
            );
        }

        // 2. Verify genuine tokenizer availability
        if (!tokenizer.isAvailable()) {
            return EmotionPrediction.unavailable(
                    Modality.TEXTUAL,
                    "Tokenizer file not found in " + config.getTokenizerPath() + ". Tokenizer status: UNAVAILABLE."
            );
        }

        OrtSession session = modelManager.getTextSession();
        if (session == null) {
            return EmotionPrediction.unavailable(
                    Modality.TEXTUAL,
                    "ONNX text inference session not initialized. Model status: UNAVAILABLE."
            );
        }

        if (input == null || input.rawText() == null || input.rawText().isBlank()) {
            return EmotionPrediction.unavailable(
                    Modality.TEXTUAL,
                    "Empty thoughts text provided."
            );
        }

        try {
            // 3. Tokenize input with genuine tokenizer
            TextTokenizer.TokenizedInput tokenized = tokenizer.tokenize(input.rawText(), config.getMaxSequenceLength());
            long[] dims = new long[]{1, tokenized.inputIds().length};

            try (OnnxTensor inputIdsTensor = OnnxTensor.createTensor(
                    modelManager.getEnvironment(),
                    LongBuffer.wrap(tokenized.inputIds()),
                    dims
            );
            OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(
                    modelManager.getEnvironment(),
                    LongBuffer.wrap(tokenized.attentionMask()),
                    dims
            )) {
                Map<String, OnnxTensor> inputs = new LinkedHashMap<>();
                inputs.put(config.getInputIdsName(), inputIdsTensor);
                inputs.put(config.getAttentionMaskName(), attentionMaskTensor);

                // 4. Run ONNX inference
                try (OrtSession.Result result = session.run(inputs)) {
                float[][] logits = (float[][]) result.get(0).getValue();
                float[] scores = logits[0]; // 28 classes

                // Sigmoid / Softmax
                float[] probabilities = sigmoid(scores);

                List<String> labels = config.getLabelMapping();
                Map<String, Double> distribution = new LinkedHashMap<>();
                int bestIndex = 0;
                float maxProb = -1.0f;

                for (int i = 0; i < labels.size() && i < probabilities.length; i++) {
                    float p = probabilities[i];
                    distribution.put(labels.get(i), (double) Math.round(p * 10000.0f) / 100.0);
                    if (p > maxProb) {
                        maxProb = p;
                        bestIndex = i;
                    }
                }

                String rawEmotion = bestIndex < labels.size() ? labels.get(bestIndex) : "neutral";
                double topConfidence = Math.round(maxProb * 10000.0f) / 100.0;

                // 5. Map 28 GoEmotions to EmoSense textual signal
                String mappedSignal = TextEmotionMapping.mapGoEmotionToSignal(rawEmotion);

                String notes = "Real ONNX GoEmotions Inference — Raw emotion: " + rawEmotion
                        + " (" + topConfidence + "%). Output classes: " + probabilities.length;

                return EmotionPrediction.real(
                        mappedSignal,
                        topConfidence,
                        distribution,
                        Modality.TEXTUAL,
                        getModelName(),
                        notes
                );
            }
            }
        } catch (OrtException e) {
            System.err.println("[OnnxTextEmotionModel] ONNX inference error: " + e.getMessage());
            return EmotionPrediction.unavailable(
                    Modality.TEXTUAL,
                    "ONNX text inference failure: " + e.getMessage()
            );
        } catch (Exception e) {
            System.err.println("[OnnxTextEmotionModel] Unexpected error: " + e.getMessage());
            return EmotionPrediction.unavailable(
                    Modality.TEXTUAL,
                    "Text emotion analysis error: " + e.getMessage()
            );
        }
    }

    private float[] sigmoid(float[] input) {
        float[] output = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = (float) (1.0 / (1.0 + Math.exp(-input[i])));
        }
        return output;
    }

    @Override
    public String getModelName() {
        return "Real ONNX GoEmotions Text Model (" + config.getModelPath() + ")";
    }

    @Override
    public boolean isTrainedModel() {
        return modelManager.isTextModelAvailable() && tokenizer.isAvailable();
    }
}
