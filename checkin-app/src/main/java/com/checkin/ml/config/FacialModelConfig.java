package com.checkin.ml.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration for the Facial Emotion Recognition ONNX model.
 * All parameters (paths, tensor input/output names, shapes, and label order)
 * are fully configurable and not hardcoded to a specific model.
 */
public class FacialModelConfig {

    public static final String DEFAULT_MODEL_PATH = "models/facial/face_emotion_model.onnx";
    public static final String FALLBACK_MODEL_PATH = "models/facial/fer2013_model.onnx";
    public static final String DEFAULT_INPUT_NAME = "pixel_values";
    public static final String DEFAULT_OUTPUT_NAME = "logits";
    public static final long[] DEFAULT_INPUT_DIMS = new long[]{1, 3, 224, 224};
    public static final long[] DEFAULT_OUTPUT_DIMS = new long[]{1, 7};
    public static final List<String> DEFAULT_LABELS = List.of(
            "Angry",
            "Disgust",
            "Fear",
            "Happy",
            "Sad",
            "Surprise",
            "Neutral"
    );

    private String modelPath = DEFAULT_MODEL_PATH;
    private String inputName = DEFAULT_INPUT_NAME;
    private String outputName = DEFAULT_OUTPUT_NAME;
    private long[] inputDimensions = DEFAULT_INPUT_DIMS.clone();
    private long[] outputDimensions = DEFAULT_OUTPUT_DIMS.clone();
    private List<String> labelMapping = new ArrayList<>(DEFAULT_LABELS);
    private PreprocessingConfig preprocessingConfig = PreprocessingConfig.defaultViTFacial();

    public FacialModelConfig() {
    }

    public FacialModelConfig(String modelPath, String inputName, String outputName,
                             long[] inputDimensions, long[] outputDimensions,
                             List<String> labelMapping, PreprocessingConfig preprocessingConfig) {
        this.modelPath = modelPath != null ? modelPath : DEFAULT_MODEL_PATH;
        this.inputName = inputName != null ? inputName : DEFAULT_INPUT_NAME;
        this.outputName = outputName != null ? outputName : DEFAULT_OUTPUT_NAME;
        this.inputDimensions = inputDimensions != null ? inputDimensions.clone() : DEFAULT_INPUT_DIMS.clone();
        this.outputDimensions = outputDimensions != null ? outputDimensions.clone() : DEFAULT_OUTPUT_DIMS.clone();
        this.labelMapping = labelMapping != null ? new ArrayList<>(labelMapping) : new ArrayList<>(DEFAULT_LABELS);
        this.preprocessingConfig = preprocessingConfig != null ? preprocessingConfig : PreprocessingConfig.defaultFacial();
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public String getInputName() {
        return inputName;
    }

    public void setInputName(String inputName) {
        this.inputName = inputName;
    }

    public String getOutputName() {
        return outputName;
    }

    public void setOutputName(String outputName) {
        this.outputName = outputName;
    }

    public long[] getInputDimensions() {
        return inputDimensions != null ? inputDimensions.clone() : new long[0];
    }

    public void setInputDimensions(long[] inputDimensions) {
        this.inputDimensions = inputDimensions != null ? inputDimensions.clone() : new long[0];
    }

    public long[] getOutputDimensions() {
        return outputDimensions != null ? outputDimensions.clone() : new long[0];
    }

    public void setOutputDimensions(long[] outputDimensions) {
        this.outputDimensions = outputDimensions != null ? outputDimensions.clone() : new long[0];
    }

    public List<String> getLabelMapping() {
        return Collections.unmodifiableList(labelMapping);
    }

    public void setLabelMapping(List<String> labelMapping) {
        this.labelMapping = labelMapping != null ? new ArrayList<>(labelMapping) : new ArrayList<>(DEFAULT_LABELS);
    }

    public PreprocessingConfig getPreprocessingConfig() {
        return preprocessingConfig;
    }

    public void setPreprocessingConfig(PreprocessingConfig preprocessingConfig) {
        this.preprocessingConfig = preprocessingConfig;
    }
}
