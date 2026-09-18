package com.checkin.ml.config;

/**
 * Configuration for face detection module.
 * Accommodates both ONNX-based deep learning detectors (e.g. UltraFace, YuNet, SSD)
 * and OpenCV Haar Cascade classifier files.
 */
public class FaceDetectorConfig {

    public static final String DEFAULT_ONNX_PATH = "models/face_detection/face_detector.onnx";
    public static final String DEFAULT_CASCADE_PATH = "models/face_detection/haarcascade_frontalface_default.xml";
    public static final String DEFAULT_INPUT_NAME = "input";
    public static final String DEFAULT_CONFIDENCES_OUTPUT = "scores";
    public static final String DEFAULT_BOXES_OUTPUT = "boxes";
    public static final long[] DEFAULT_INPUT_DIMS = new long[]{1, 3, 300, 300};
    public static final double DEFAULT_CONFIDENCE_THRESHOLD = 0.65;

    private String modelPath = DEFAULT_ONNX_PATH;
    private String cascadePath = DEFAULT_CASCADE_PATH;
    private String inputName = DEFAULT_INPUT_NAME;
    private String outputName = DEFAULT_CONFIDENCES_OUTPUT;
    private String boxesOutputName = DEFAULT_BOXES_OUTPUT;
    private long[] inputDimensions = DEFAULT_INPUT_DIMS.clone();
    private double confidenceThreshold = DEFAULT_CONFIDENCE_THRESHOLD;
    private PreprocessingConfig preprocessingConfig = PreprocessingConfig.defaultFaceDetection();

    public FaceDetectorConfig() {
    }

    public FaceDetectorConfig(String modelPath, String inputName, String outputName,
                              long[] inputDimensions, double confidenceThreshold,
                              PreprocessingConfig preprocessingConfig) {
        this.modelPath = modelPath != null ? modelPath : DEFAULT_ONNX_PATH;
        this.inputName = inputName != null ? inputName : DEFAULT_INPUT_NAME;
        this.outputName = outputName != null ? outputName : DEFAULT_CONFIDENCES_OUTPUT;
        this.inputDimensions = inputDimensions != null ? inputDimensions.clone() : DEFAULT_INPUT_DIMS.clone();
        this.confidenceThreshold = confidenceThreshold > 0.0 ? confidenceThreshold : DEFAULT_CONFIDENCE_THRESHOLD;
        this.preprocessingConfig = preprocessingConfig != null ? preprocessingConfig : PreprocessingConfig.defaultFaceDetection();
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public String getCascadePath() {
        return cascadePath;
    }

    public void setCascadePath(String cascadePath) {
        this.cascadePath = cascadePath;
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

    public String getBoxesOutputName() {
        return boxesOutputName;
    }

    public void setBoxesOutputName(String boxesOutputName) {
        this.boxesOutputName = boxesOutputName;
    }

    public long[] getInputDimensions() {
        return inputDimensions != null ? inputDimensions.clone() : new long[0];
    }

    public void setInputDimensions(long[] inputDimensions) {
        this.inputDimensions = inputDimensions != null ? inputDimensions.clone() : new long[0];
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public PreprocessingConfig getPreprocessingConfig() {
        return preprocessingConfig;
    }

    public void setPreprocessingConfig(PreprocessingConfig preprocessingConfig) {
        this.preprocessingConfig = preprocessingConfig;
    }
}
