package com.checkin;

import com.checkin.ml.EmotionPrediction;
import com.checkin.ml.EmotionPrediction.Modality;
import com.checkin.ml.EmotionPrediction.ModelStatus;
import com.checkin.ml.ModelManager;
import com.checkin.ml.OnnxFacialEmotionModel;
import com.checkin.ml.OnnxTextEmotionModel;
import com.checkin.ml.OnnxTextTokenizer;
import com.checkin.ml.config.FaceDetectorConfig;
import com.checkin.ml.config.FacialModelConfig;
import com.checkin.ml.config.PreprocessingConfig;
import com.checkin.ml.config.TextModelConfig;
import com.checkin.model.AnalysisResultRecord;
import com.checkin.preprocessing.FacialImagePreprocessor;
import com.checkin.preprocessing.ImagePreprocessor;
import com.checkin.preprocessing.TextPreprocessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests verifying model lifecycle, availability checks, UNAVAILABLE status reporting,
 * and zero fallback to Neutral when models are absent.
 */
public class ModelManagerTest {

    private ModelManager modelManager;

    @BeforeEach
    void setUp() {
        ModelManager.resetInstance();
        modelManager = new ModelManager();
    }

    @AfterEach
    void tearDown() {
        if (modelManager != null) {
            modelManager.close();
        }
        ModelManager.resetInstance();
    }

    @Test
    void testRealModelsDetectedWhenPresent() {
        // When real models are placed in the models directory, ModelManager reports REAL
        assertEquals(ModelStatus.REAL, modelManager.getFacialModelStatus(),
                "Facial model status must be REAL when face_emotion_model.onnx is present");
        assertTrue(modelManager.isFacialModelAvailable(),
                "isFacialModelAvailable() must return true when model file is present");

        assertEquals(ModelStatus.REAL, modelManager.getTextModelStatus(),
                "Text model status must be REAL when goemotions_model.onnx is present");
        assertTrue(modelManager.isTextModelAvailable(),
                "isTextModelAvailable() must return true when model file is present");

        assertEquals(ModelStatus.REAL, modelManager.getFaceDetectorStatus(),
                "Face detector status must be REAL when face_detector.onnx is present");
        assertTrue(modelManager.isFaceDetectorAvailable(),
                "isFaceDetectorAvailable() must return true when detector model file is present");
    }

    @Test
    void testAbsentModelsReturnUnavailable() {
        FacialModelConfig absentFacial = new FacialModelConfig();
        absentFacial.setModelPath("models/facial/missing_model.onnx");
        TextModelConfig absentText = new TextModelConfig();
        absentText.setModelPath("models/text/missing_model.onnx");
        FaceDetectorConfig absentDetector = new FaceDetectorConfig();
        absentDetector.setModelPath("models/face_detection/missing_detector.onnx");

        ModelManager absentMm = new ModelManager(absentFacial, absentText, absentDetector);

        // When model files are absent from disk, ModelManager MUST report UNAVAILABLE
        assertEquals(ModelStatus.UNAVAILABLE, absentMm.getFacialModelStatus(),
                "Facial model status must be UNAVAILABLE when model file is absent");
        assertFalse(absentMm.isFacialModelAvailable(),
                "isFacialModelAvailable() must return false when model file is absent");

        assertEquals(ModelStatus.UNAVAILABLE, absentMm.getTextModelStatus(),
                "Text model status must be UNAVAILABLE when model file is absent");
        assertFalse(absentMm.isTextModelAvailable(),
                "isTextModelAvailable() must return false when model file is absent");

        assertEquals(ModelStatus.UNAVAILABLE, absentMm.getFaceDetectorStatus(),
                "Face detector status must be UNAVAILABLE when detector model file is absent");
        assertFalse(absentMm.isFaceDetectorAvailable(),
                "isFaceDetectorAvailable() must return false when detector model file is absent");
        absentMm.close();
    }

    @Test
    void testMockModeDistinction() {
        FacialModelConfig absentFacial = new FacialModelConfig();
        absentFacial.setModelPath("models/facial/missing_model.onnx");
        ModelManager absentMm = new ModelManager(absentFacial, new TextModelConfig(), new FaceDetectorConfig());

        // Toggle mock mode
        absentMm.setMockMode(true);
        assertTrue(absentMm.isMockMode());

        assertEquals(ModelStatus.MOCK, absentMm.getFacialModelStatus(),
                "When mock mode is enabled, facial status must report MOCK");
        assertEquals(ModelStatus.MOCK, absentMm.getTextModelStatus(),
                "When mock mode is enabled, text status must report MOCK");
        assertEquals(ModelStatus.MOCK, absentMm.getFaceDetectorStatus(),
                "When mock mode is enabled, detector status must report MOCK");

        // Toggle back to real mode
        absentMm.setMockMode(false);
        assertFalse(absentMm.isMockMode());
        assertEquals(ModelStatus.UNAVAILABLE, absentMm.getFacialModelStatus(),
                "When mock mode is disabled and model is absent, status must return UNAVAILABLE");
        absentMm.close();
    }

    @Test
    void testFacialModelNeverReturnsNeutralWhenUnavailable() throws Exception {
        FacialModelConfig absentConfig = new FacialModelConfig();
        absentConfig.setModelPath("models/facial/missing_model.onnx");
        ModelManager absentMm = new ModelManager(absentConfig, new TextModelConfig(), null);
        OnnxFacialEmotionModel facialModel = new OnnxFacialEmotionModel(absentMm, absentConfig);

        // Dummy preprocessed data
        java.awt.image.BufferedImage dummyImg = new java.awt.image.BufferedImage(48, 48, java.awt.image.BufferedImage.TYPE_BYTE_GRAY);
        ImagePreprocessor.PreprocessedImage input = new ImagePreprocessor.PreprocessedImage(
                new File("dummy.jpg"), dummyImg, 48, 48, true
        );

        EmotionPrediction prediction = facialModel.predict(input);

        assertNotNull(prediction);
        assertEquals(ModelStatus.UNAVAILABLE, prediction.modelStatus(),
                "Absent facial model must produce UNAVAILABLE prediction");
        assertEquals("Analysis unavailable", prediction.primarySignal(),
                "Primary signal must be 'Analysis unavailable', NEVER 'Neutral'");
        assertNotEquals("Neutral", prediction.primarySignal(),
                "CRITICAL: Must NEVER return Neutral as a fallback for missing models");
        assertEquals(0.0, prediction.confidence(),
                "Confidence must be 0.0 when analysis is unavailable");
        absentMm.close();
    }

    @Test
    void testTextModelNeverReturnsNeutralWhenUnavailable() throws Exception {
        TextModelConfig absentConfig = new TextModelConfig();
        absentConfig.setModelPath("models/text/missing_model.onnx");
        absentConfig.setVocabPath("models/text/missing_vocab.json");
        absentConfig.setMergesPath("models/text/missing_merges.txt");
        ModelManager absentMm = new ModelManager(new FacialModelConfig(), absentConfig, null);
        OnnxTextEmotionModel textModel = new OnnxTextEmotionModel(
                absentMm,
                absentConfig,
                new OnnxTextTokenizer(absentMm, absentConfig)
        );

        TextPreprocessor.PreprocessedText input = new TextPreprocessor().process("I am feeling great today!");

        EmotionPrediction prediction = textModel.predict(input);

        assertNotNull(prediction);
        assertEquals(ModelStatus.UNAVAILABLE, prediction.modelStatus(),
                "Absent text model or tokenizer must produce UNAVAILABLE prediction");
        assertEquals("Analysis unavailable", prediction.primarySignal(),
                "Primary signal must be 'Analysis unavailable', NEVER 'Neutral'");
        assertNotEquals("Neutral", prediction.primarySignal(),
                "CRITICAL: Must NEVER return Neutral as a fallback for missing models");
        assertEquals(0.0, prediction.confidence(),
                "Confidence must be 0.0 when analysis is unavailable");
        absentMm.close();
    }

    @Test
    void testTokenizerReportsUnavailableWithoutFakeTokenization() {
        TextModelConfig absentConfig = new TextModelConfig();
        absentConfig.setVocabPath("models/text/missing_vocab.json");
        absentConfig.setMergesPath("models/text/missing_merges.txt");
        OnnxTextTokenizer tokenizer = new OnnxTextTokenizer(modelManager, absentConfig);

        assertFalse(tokenizer.isAvailable(),
                "Tokenizer must report isAvailable() == false when tokenizer files are absent");

        assertThrows(IllegalStateException.class, () -> tokenizer.tokenize("Sample text", 128),
                "Tokenizer must throw IllegalStateException rather than producing fabricated tokens");
    }

    @Test
    void testModelAutoDetectionWhenFileAdded(@TempDir Path tempDir) throws Exception {
        // Create a real file in temp directory
        File fakeWeightFile = tempDir.resolve("fer2013_model.onnx").toFile();
        try (FileOutputStream fos = new FileOutputStream(fakeWeightFile)) {
            fos.write(new byte[]{0x08, 0x01, 0x12, 0x00}); // non-empty file
        }

        FacialModelConfig customConfig = new FacialModelConfig();
        customConfig.setModelPath(fakeWeightFile.getAbsolutePath());

        ModelManager customManager = new ModelManager(customConfig, new TextModelConfig(), new FaceDetectorConfig());

        Optional<File> located = customManager.locateModelFile(customConfig.getModelPath());
        assertTrue(located.isPresent(), "ModelManager must locate existing model file");
        assertEquals(fakeWeightFile.getAbsolutePath(), located.get().getAbsolutePath());

        customManager.close();
    }

    @Test
    void testModelConfigurationsAreConfigurable() {
        // Facial Model Config
        FacialModelConfig facialConfig = new FacialModelConfig();
        facialConfig.setModelPath("custom/path/model.onnx");
        facialConfig.setInputName("custom_in");
        facialConfig.setOutputName("custom_out");
        facialConfig.setInputDimensions(new long[]{1, 3, 224, 224});
        facialConfig.setOutputDimensions(new long[]{1, 7});
        facialConfig.setLabelMapping(List.of("A", "B", "C", "D", "E", "F", "G"));

        assertEquals("custom/path/model.onnx", facialConfig.getModelPath());
        assertEquals("custom_in", facialConfig.getInputName());
        assertEquals("custom_out", facialConfig.getOutputName());
        assertArrayEquals(new long[]{1, 3, 224, 224}, facialConfig.getInputDimensions());
        assertEquals(7, facialConfig.getLabelMapping().size());

        // Text Model Config
        TextModelConfig textConfig = new TextModelConfig();
        textConfig.setModelPath("custom/text.onnx");
        textConfig.setTokenizerPath("custom/tok.json");
        textConfig.setMaxSequenceLength(64);
        textConfig.setOutputDimensions(new long[]{1, 28});

        assertEquals("custom/text.onnx", textConfig.getModelPath());
        assertEquals("custom/tok.json", textConfig.getTokenizerPath());
        assertEquals(64, textConfig.getMaxSequenceLength());
        assertEquals(28, textConfig.getLabelMapping().size());

        // Face Detector Config
        FaceDetectorConfig detectorConfig = new FaceDetectorConfig();
        detectorConfig.setModelPath("custom/face.onnx");
        detectorConfig.setConfidenceThreshold(0.75);

        assertEquals("custom/face.onnx", detectorConfig.getModelPath());
        assertEquals(0.75, detectorConfig.getConfidenceThreshold());
    }

    @Test
    void testDatabaseCompatibilityRecord() {
        LocalDateTime now = LocalDateTime.now();

        // 1. UNAVAILABLE result record
        AnalysisResultRecord unavailRecord = new AnalysisResultRecord(
                "res-1", "chk-1", "Analysis unavailable", 0.0,
                null, null, "Analysis unavailable",
                "N/A", "UNAVAILABLE", "MULTIMODAL", null, now
        );

        assertTrue(unavailRecord.isUnavailable());
        assertFalse(unavailRecord.isReal());
        assertFalse(unavailRecord.isMock());
        assertEquals("UNAVAILABLE", unavailRecord.modelSource());
        assertEquals("MULTIMODAL", unavailRecord.modality());
        assertEquals("Analysis unavailable", unavailRecord.primarySignal());
        assertEquals(0.0, unavailRecord.confidence());

        // 2. REAL result record
        AnalysisResultRecord realRecord = new AnalysisResultRecord(
                "REAL", "FACIAL", "HAPPY", 94.5, "{\"HAPPY\":94.5,\"NEUTRAL\":5.5}", now
        );

        assertTrue(realRecord.isReal());
        assertFalse(realRecord.isMock());
        assertEquals("REAL", realRecord.modelSource());
        assertEquals("FACIAL", realRecord.modality());
        assertEquals("HAPPY", realRecord.primarySignal());
        assertEquals(94.5, realRecord.confidence());
        assertEquals("{\"HAPPY\":94.5,\"NEUTRAL\":5.5}", realRecord.probabilityDistribution());

        // 3. MOCK result record
        AnalysisResultRecord mockRecord = new AnalysisResultRecord(
                "res-3", "chk-3", "Neutral Expression", 70.0,
                "Neutral Expression", null, "Neutral Expression",
                "None", "MOCK", now
        );

        assertTrue(mockRecord.isMock());
        assertFalse(mockRecord.isReal());
        assertEquals("MOCK", mockRecord.modelSource());
    }
}
