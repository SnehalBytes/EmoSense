package com.checkin.ml.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration for the Text Emotion Recognition ONNX model (e.g. GoEmotions transformer).
 * All model paths, tokenizer paths, input/output tensor names, sequence lengths,
 * and GoEmotions 28-class label mappings are fully configurable.
 */
public class TextModelConfig {

    public static final String DEFAULT_MODEL_PATH = "models/text/goemotions_model.onnx";
    public static final String DEFAULT_TOKENIZER_PATH = "models/text/tokenizer.json";
    public static final String DEFAULT_VOCAB_PATH = "models/text/vocab.json";
    public static final String DEFAULT_MERGES_PATH = "models/text/merges.txt";

    public static final String DEFAULT_INPUT_IDS_NAME = "input_ids";
    public static final String DEFAULT_ATTENTION_MASK_NAME = "attention_mask";
    public static final String DEFAULT_TOKEN_TYPE_IDS_NAME = "token_type_ids";
    public static final String DEFAULT_OUTPUT_NAME = "logits";

    public static final int DEFAULT_MAX_SEQ_LENGTH = 128;
    public static final long[] DEFAULT_OUTPUT_DIMS = new long[]{1, 28};

    public static final List<String> DEFAULT_GOEMOTIONS_LABELS = List.of(
            "admiration", "amusement", "anger", "annoyance", "approval", "caring",
            "confusion", "curiosity", "desire", "disappointment", "disapproval",
            "disgust", "embarrassment", "excitement", "fear", "gratitude", "grief",
            "joy", "love", "nervousness", "optimism", "pride", "realization",
            "relief", "remorse", "sadness", "surprise", "neutral"
    );

    private String modelPath = DEFAULT_MODEL_PATH;
    private String tokenizerPath = DEFAULT_TOKENIZER_PATH;
    private String vocabPath = DEFAULT_VOCAB_PATH;
    private String mergesPath = DEFAULT_MERGES_PATH;

    private String inputIdsName = DEFAULT_INPUT_IDS_NAME;
    private String attentionMaskName = DEFAULT_ATTENTION_MASK_NAME;
    private String tokenTypeIdsName = DEFAULT_TOKEN_TYPE_IDS_NAME;
    private String outputName = DEFAULT_OUTPUT_NAME;

    private int maxSequenceLength = DEFAULT_MAX_SEQ_LENGTH;
    private long[] outputDimensions = DEFAULT_OUTPUT_DIMS.clone();
    private List<String> labelMapping = new ArrayList<>(DEFAULT_GOEMOTIONS_LABELS);
    private PreprocessingConfig preprocessingConfig = PreprocessingConfig.defaultText();

    public TextModelConfig() {
    }

    public TextModelConfig(String modelPath, String tokenizerPath, String vocabPath,
                           String inputIdsName, String attentionMaskName, String tokenTypeIdsName,
                           String outputName, int maxSequenceLength, long[] outputDimensions,
                           List<String> labelMapping, PreprocessingConfig preprocessingConfig) {
        this.modelPath = modelPath != null ? modelPath : DEFAULT_MODEL_PATH;
        this.tokenizerPath = tokenizerPath != null ? tokenizerPath : DEFAULT_TOKENIZER_PATH;
        this.vocabPath = vocabPath != null ? vocabPath : DEFAULT_VOCAB_PATH;
        this.inputIdsName = inputIdsName != null ? inputIdsName : DEFAULT_INPUT_IDS_NAME;
        this.attentionMaskName = attentionMaskName != null ? attentionMaskName : DEFAULT_ATTENTION_MASK_NAME;
        this.tokenTypeIdsName = tokenTypeIdsName != null ? tokenTypeIdsName : DEFAULT_TOKEN_TYPE_IDS_NAME;
        this.outputName = outputName != null ? outputName : DEFAULT_OUTPUT_NAME;
        this.maxSequenceLength = maxSequenceLength > 0 ? maxSequenceLength : DEFAULT_MAX_SEQ_LENGTH;
        this.outputDimensions = outputDimensions != null ? outputDimensions.clone() : DEFAULT_OUTPUT_DIMS.clone();
        this.labelMapping = labelMapping != null ? new ArrayList<>(labelMapping) : new ArrayList<>(DEFAULT_GOEMOTIONS_LABELS);
        this.preprocessingConfig = preprocessingConfig != null ? preprocessingConfig : PreprocessingConfig.defaultText();
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public String getTokenizerPath() {
        return tokenizerPath;
    }

    public void setTokenizerPath(String tokenizerPath) {
        this.tokenizerPath = tokenizerPath;
    }

    public String getVocabPath() {
        return vocabPath;
    }

    public void setVocabPath(String vocabPath) {
        this.vocabPath = vocabPath;
    }

    public String getMergesPath() {
        return mergesPath;
    }

    public void setMergesPath(String mergesPath) {
        this.mergesPath = mergesPath;
    }

    public String getInputIdsName() {
        return inputIdsName;
    }

    public void setInputIdsName(String inputIdsName) {
        this.inputIdsName = inputIdsName;
    }

    public String getAttentionMaskName() {
        return attentionMaskName;
    }

    public void setAttentionMaskName(String attentionMaskName) {
        this.attentionMaskName = attentionMaskName;
    }

    public String getTokenTypeIdsName() {
        return tokenTypeIdsName;
    }

    public void setTokenTypeIdsName(String tokenTypeIdsName) {
        this.tokenTypeIdsName = tokenTypeIdsName;
    }

    public String getOutputName() {
        return outputName;
    }

    public void setOutputName(String outputName) {
        this.outputName = outputName;
    }

    public int getMaxSequenceLength() {
        return maxSequenceLength;
    }

    public void setMaxSequenceLength(int maxSequenceLength) {
        this.maxSequenceLength = maxSequenceLength;
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
        this.labelMapping = labelMapping != null ? new ArrayList<>(labelMapping) : new ArrayList<>(DEFAULT_GOEMOTIONS_LABELS);
    }

    public PreprocessingConfig getPreprocessingConfig() {
        return preprocessingConfig;
    }

    public void setPreprocessingConfig(PreprocessingConfig preprocessingConfig) {
        this.preprocessingConfig = preprocessingConfig;
    }
}
