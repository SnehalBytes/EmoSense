package com.checkin.ml;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.checkin.ml.EmotionPrediction.Modality;
import com.checkin.ml.config.FacialModelConfig;
import com.checkin.preprocessing.ImagePreprocessor;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Real ONNX Facial Emotion Recognition Model implementation.
 * Follows the pipeline:
 *   Image -> Preprocessing -> ONNX Inference -> 7 outputs -> EmotionPrediction
 *
 * If the model file is absent or cannot be loaded, returns UNAVAILABLE.
 * NEVER returns Neutral as a fallback.
 */
public class OnnxFacialEmotionModel implements FacialEmotionModel {

    private final ModelManager modelManager;
    private final FacialModelConfig config;

    public OnnxFacialEmotionModel() {
        this(ModelManager.getInstance(), ModelManager.getInstance().getFacialConfig());
    }

    public OnnxFacialEmotionModel(ModelManager modelManager, FacialModelConfig config) {
        this.modelManager = modelManager;
        this.config = config != null ? config : new FacialModelConfig();
    }

    @Override
    public EmotionPrediction predict(ImagePreprocessor.PreprocessedImage input) {
        // 1. Verify model availability
        if (!modelManager.isFacialModelAvailable()) {
            return EmotionPrediction.unavailable(
                    Modality.FACIAL,
                    "Facial model file not found in " + config.getModelPath() + ". Model status: UNAVAILABLE."
            );
        }

        OrtSession session = modelManager.getFacialSession();
        if (session == null) {
            return EmotionPrediction.unavailable(
                    Modality.FACIAL,
                    "ONNX inference session not initialized. Model status: UNAVAILABLE."
            );
        }

        if (input == null || input.bufferedImage() == null) {
            return EmotionPrediction.unavailable(
                    Modality.FACIAL,
                    "Invalid preprocessed image data provided."
            );
        }

        try {
            // 2. Prepare ONNX input tensor
            long[] dims = config.getInputDimensions(); // e.g. [1, 3, 224, 224]
            int channels = dims.length >= 2 ? (int) dims[1] : 3;
            int targetH = dims.length >= 4 ? (int) dims[2] : 224;
            int targetW = dims.length >= 4 ? (int) dims[3] : 224;

            float[] floatBuffer = extractPixelFloats(input.bufferedImage(), channels, targetW, targetH);

            try (OnnxTensor inputTensor = OnnxTensor.createTensor(
                    modelManager.getEnvironment(),
                    FloatBuffer.wrap(floatBuffer),
                    dims
            )) {
                // 3. Execute ONNX inference
                Map<String, OnnxTensor> inputs = Collections.singletonMap(config.getInputName(), inputTensor);
                try (OrtSession.Result result = session.run(inputs)) {
                    // 4. Extract raw outputs
                    float[][] rawScores = (float[][]) result.get(0).getValue();
                    float[] scores = rawScores[0];

                    // 5. Softmax normalization over 7 classes
                    float[] probabilities = softmax(scores);

                    // 6. Map to configured labels
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

                    String topLabel = bestIndex < labels.size() ? labels.get(bestIndex) : "Unknown";
                    double topConfidence = Math.round(maxProb * 10000.0f) / 100.0;

                    String notes = "Real ONNX Inference — Input shape: " + java.util.Arrays.toString(dims)
                            + ", Output classes: " + probabilities.length;

                    return EmotionPrediction.real(
                            topLabel,
                            topConfidence,
                            distribution,
                            Modality.FACIAL,
                            getModelName(),
                            notes
                    );
                }
            }
        } catch (OrtException e) {
            System.err.println("[OnnxFacialEmotionModel] Inference error: " + e.getMessage());
            return EmotionPrediction.unavailable(
                    Modality.FACIAL,
                    "ONNX inference failure: " + e.getMessage()
            );
        } catch (Exception e) {
            System.err.println("[OnnxFacialEmotionModel] Unexpected error: " + e.getMessage());
            return EmotionPrediction.unavailable(
                    Modality.FACIAL,
                    "Facial analysis error: " + e.getMessage()
            );
        }
    }

    private float[] extractPixelFloats(BufferedImage src, int channels, int targetW, int targetH) {
        BufferedImage resized = new BufferedImage(targetW, targetH,
                channels == 1 ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_INT_RGB);
        Graphics2D gfx = resized.createGraphics();
        gfx.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        gfx.drawImage(src, 0, 0, targetW, targetH, null);
        gfx.dispose();

        if (channels == 1) {
            byte[] pixels = ((DataBufferByte) resized.getRaster().getDataBuffer()).getData();
            float[] floats = new float[pixels.length];
            for (int i = 0; i < pixels.length; i++) {
                floats[i] = (pixels[i] & 0xFF) / 255.0f;
            }
            return floats;
        } else {
            float[] floats = new float[3 * targetW * targetH];
            int channelSize = targetW * targetH;
            for (int y = 0; y < targetH; y++) {
                for (int x = 0; x < targetW; x++) {
                    int rgb = resized.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;

                    int pixelIndex = y * targetW + x;
                    // Standard ViT normalization: (val / 255.0 - 0.5) / 0.5
                    floats[pixelIndex] = ((r / 255.0f) - 0.5f) / 0.5f;                    // R channel
                    floats[channelSize + pixelIndex] = ((g / 255.0f) - 0.5f) / 0.5f;      // G channel
                    floats[2 * channelSize + pixelIndex] = ((b / 255.0f) - 0.5f) / 0.5f;  // B channel
                }
            }
            return floats;
        }
    }

    private float[] softmax(float[] input) {
        float[] output = new float[input.length];
        float max = Float.NEGATIVE_INFINITY;
        for (float v : input) {
            if (v > max) max = v;
        }
        float sum = 0.0f;
        for (int i = 0; i < input.length; i++) {
            output[i] = (float) Math.exp(input[i] - max);
            sum += output[i];
        }
        if (sum > 0.0f) {
            for (int i = 0; i < output.length; i++) {
                output[i] /= sum;
            }
        }
        return output;
    }

    @Override
    public String getModelName() {
        return "Real ONNX Facial Emotion Model (" + config.getModelPath() + ")";
    }

    @Override
    public boolean isTrainedModel() {
        return modelManager.isFacialModelAvailable();
    }
}
