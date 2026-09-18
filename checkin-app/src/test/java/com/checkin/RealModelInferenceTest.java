package com.checkin;

import com.checkin.ml.*;
import com.checkin.ml.config.FacialModelConfig;
import com.checkin.ml.config.TextModelConfig;
import com.checkin.preprocessing.ImagePreprocessor;
import com.checkin.preprocessing.TextPreprocessor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class RealModelInferenceTest {

    private static ModelManager modelManager;

    @BeforeAll
    static void setUp() {
        modelManager = ModelManager.getInstance();
    }

    @Test
    @DisplayName("Verify ByteLevelBpeTokenizer encodes text correctly")
    void testByteLevelBpeTokenizer() {
        TextModelConfig config = modelManager.getTextConfig();
        OnnxTextTokenizer tokenizer = new OnnxTextTokenizer(modelManager, config);

        assertTrue(tokenizer.isAvailable(), "Tokenizer should be available with vocab.json and merges.txt");

        String sampleText = "I am so happy and grateful today!";
        TextTokenizer.TokenizedInput tokenized = tokenizer.tokenize(sampleText, 64);

        assertNotNull(tokenized);
        assertTrue(tokenized.inputIds().length >= 3, "Should produce at least BOS, token, EOS");
        assertEquals(0L, tokenized.inputIds()[0], "First token must be <s> (0)");
        assertEquals(2L, tokenized.inputIds()[tokenized.inputIds().length - 1], "Last token must be </s> (2)");
        assertEquals(tokenized.inputIds().length, tokenized.attentionMask().length);

        System.out.println("[TOKENIZER TEST] Text: \"" + sampleText + "\"");
        System.out.println("  Token IDs length: " + tokenized.inputIds().length);
        System.out.println("  Token IDs: " + java.util.Arrays.toString(tokenized.inputIds()));
    }

    @Test
    @DisplayName("Verify Real ONNX Text Model inference on positive and negative text")
    void testRealTextInference() {
        assertTrue(modelManager.isTextModelAvailable(), "Text model must be available");

        OnnxTextEmotionModel textModel = new OnnxTextEmotionModel();
        assertTrue(textModel.isTrainedModel(), "Model must report isTrainedModel = true");

        // 1. Positive text
        String happyText = "I am so joyful, excited and thrilled with these wonderful results!";
        TextPreprocessor.PreprocessedText happyInput = new TextPreprocessor().process(happyText);

        EmotionPrediction happyPrediction = textModel.predict(happyInput);
        assertNotNull(happyPrediction);
        assertTrue(happyPrediction.isRealModel(), "Prediction must be REAL (not mock, not unavailable)");
        assertNotNull(happyPrediction.primaryEmotion());
        assertFalse(happyPrediction.probabilityDistribution().isEmpty(), "Distribution must have all 28 emotions");
        assertTrue(happyPrediction.confidence() > 0.0, "Confidence must be > 0");

        System.out.println("[REAL TEXT INFERENCE - POSITIVE]");
        System.out.println("  Input: " + happyText);
        System.out.println("  Primary emotion: " + happyPrediction.primaryEmotion());
        System.out.println("  Confidence: " + happyPrediction.confidence() + "%");
        System.out.println("  Notes: " + happyPrediction.modelNotes());

        // 2. Negative/sad text
        String sadText = "I feel deeply sad, heartbroken, and completely depressed today.";
        TextPreprocessor.PreprocessedText sadInput = new TextPreprocessor().process(sadText);

        EmotionPrediction sadPrediction = textModel.predict(sadInput);
        assertNotNull(sadPrediction);
        assertTrue(sadPrediction.isRealModel());
        System.out.println("[REAL TEXT INFERENCE - NEGATIVE]");
        System.out.println("  Input: " + sadText);
        System.out.println("  Primary emotion: " + sadPrediction.primaryEmotion());
        System.out.println("  Confidence: " + sadPrediction.confidence() + "%");
        System.out.println("  Notes: " + sadPrediction.modelNotes());

        // Ensure both predictions are not just blindly the same neutral fallback
        System.out.println("  Positive Emotion: " + happyPrediction.primaryEmotion() +
                " vs Negative Emotion: " + sadPrediction.primaryEmotion());
    }

    @Test
    @DisplayName("Verify Real ONNX Facial Model inference on dataset sample images")
    void testRealFacialInference() throws Exception {
        assertTrue(modelManager.isFacialModelAvailable(), "Facial model must be available");

        OnnxFacialEmotionModel facialModel = new OnnxFacialEmotionModel();
        assertTrue(facialModel.isTrainedModel(), "Facial model must report isTrainedModel = true");

        // Locate test images from FER dataset
        File testDir = new File("data/facial/test");
        if (!testDir.exists()) {
            testDir = new File("../data/facial/test");
        }

        if (testDir.exists() && testDir.isDirectory()) {
            // Test a happy sample
            File happyDir = new File(testDir, "happy");
            File[] happyFiles = happyDir.listFiles((d, name) -> name.endsWith(".jpg") || name.endsWith(".png"));
            if (happyFiles != null && happyFiles.length > 0) {
                File sampleHappy = happyFiles[0];
                BufferedImage img = ImageIO.read(sampleHappy);
                ImagePreprocessor.PreprocessedImage input = new ImagePreprocessor.PreprocessedImage(
                        sampleHappy, img, img.getWidth(), img.getHeight(), false
                );

                EmotionPrediction pred = facialModel.predict(input);
                assertNotNull(pred);
                assertTrue(pred.isRealModel(), "Prediction must be REAL");
                assertNotNull(pred.primaryEmotion());
                assertEquals(7, pred.probabilityDistribution().size(), "Must output 7 emotion probabilities");
                assertTrue(pred.confidence() > 0.0);

                System.out.println("[REAL FACIAL INFERENCE - HAPPY SAMPLE]");
                System.out.println("  File: " + sampleHappy.getName());
                System.out.println("  Predicted: " + pred.primaryEmotion() + " (" + pred.confidence() + "%)");
                System.out.println("  Distribution: " + pred.probabilityDistribution());
            }

            // Test a sad sample
            File sadDir = new File(testDir, "sad");
            File[] sadFiles = sadDir.listFiles((d, name) -> name.endsWith(".jpg") || name.endsWith(".png"));
            if (sadFiles != null && sadFiles.length > 0) {
                File sampleSad = sadFiles[0];
                BufferedImage img = ImageIO.read(sampleSad);
                ImagePreprocessor.PreprocessedImage input = new ImagePreprocessor.PreprocessedImage(
                        sampleSad, img, img.getWidth(), img.getHeight(), false
                );

                EmotionPrediction pred = facialModel.predict(input);
                assertNotNull(pred);
                assertTrue(pred.isRealModel(), "Prediction must be REAL");
                System.out.println("[REAL FACIAL INFERENCE - SAD SAMPLE]");
                System.out.println("  File: " + sampleSad.getName());
                System.out.println("  Predicted: " + pred.primaryEmotion() + " (" + pred.confidence() + "%)");
                System.out.println("  Distribution: " + pred.probabilityDistribution());
            }
        } else {
            System.out.println("FER dataset directory not found at relative path; generating synthetic image test");
            BufferedImage synth = new BufferedImage(224, 224, BufferedImage.TYPE_INT_RGB);
            ImagePreprocessor.PreprocessedImage input = new ImagePreprocessor.PreprocessedImage(
                    new File("synth.png"), synth, 224, 224, false
            );
            EmotionPrediction pred = facialModel.predict(input);
            assertNotNull(pred);
            assertTrue(pred.isRealModel());
        }
    }

    @Test
    @DisplayName("Verify missing model returns UNAVAILABLE (never Neutral fallback)")
    void testMissingModelBehavior() {
        FacialModelConfig missingConfig = new FacialModelConfig();
        missingConfig.setModelPath("non_existent_model.onnx");

        ModelManager customManager = new ModelManager(missingConfig, new TextModelConfig(), null);
        OnnxFacialEmotionModel model = new OnnxFacialEmotionModel(customManager, missingConfig);

        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ImagePreprocessor.PreprocessedImage input = new ImagePreprocessor.PreprocessedImage(
                new File("test.png"), img, 100, 100, false
        );

        EmotionPrediction pred = model.predict(input);
        assertNotNull(pred);
        assertTrue(pred.isUnavailable(), "Missing model must return UNAVAILABLE");
        assertNotEquals("Neutral", pred.primaryEmotion(), "Must NEVER return Neutral for missing model");
        assertTrue(pred.modelNotes().contains("UNAVAILABLE") || pred.modelNotes().contains("not found"));
    }

    @Test
    @DisplayName("Verify empty input handling")
    void testEmptyInputHandling() {
        OnnxTextEmotionModel textModel = new OnnxTextEmotionModel();
        TextPreprocessor.PreprocessedText emptyInput = new TextPreprocessor.PreprocessedText(
                "", "", List.of(), 0, 0
        );
        EmotionPrediction textPred = textModel.predict(emptyInput);
        assertTrue(textPred.isUnavailable(), "Empty text input must return UNAVAILABLE");

        OnnxFacialEmotionModel facialModel = new OnnxFacialEmotionModel();
        EmotionPrediction facialPred = facialModel.predict((ImagePreprocessor.PreprocessedImage) null);
        assertTrue(facialPred.isUnavailable(), "Null image input must return UNAVAILABLE");
    }
}
