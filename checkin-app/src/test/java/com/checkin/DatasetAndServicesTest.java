package com.checkin;

import com.checkin.config.DatasetConfig;
import com.checkin.dao.CheckInDAO;
import com.checkin.dao.InMemoryCheckInDAO;
import com.checkin.model.CheckInData;
import com.checkin.model.CheckInRecord;
import com.checkin.model.EmotionLabel;
import com.checkin.preprocessing.ImagePreprocessor;
import com.checkin.preprocessing.TextPreprocessor;
import com.checkin.services.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DatasetAndServicesTest {

    @Test
    void testDatasetConfigPaths() {
        assertNotNull(DatasetConfig.getDataRoot());
        assertTrue(DatasetConfig.isFacialDatasetAvailable(), "Facial dataset train directory should be available");
        assertTrue(DatasetConfig.isTextDatasetAvailable(), "Text dataset train.tsv should be available");
    }

    @Test
    void testDatasetInspectionReport() {
        DatasetInspectionService service = new DatasetInspectionService();
        service.printConsoleReport();
        var stats = service.gatherStatistics();
        assertTrue(stats.facialAvailable());
        assertTrue(stats.textAvailable());
    }

    @Test
    void testFacialDatasetService() {
        FacialDatasetService service = new FacialDatasetService();
        assertTrue(service.isAvailable());

        assertEquals(7, service.getClasses().size());
        assertTrue(service.getClasses().contains("happy"));
        assertTrue(service.getClasses().contains("sad"));
        assertTrue(service.getClasses().contains("neutral"));

        int trainCount = service.countImagesInSplit("train");
        assertTrue(trainCount > 25000, "Train images should be ~28,709");

        int testCount = service.countImagesInSplit("test");
        assertTrue(testCount > 5000, "Test images should be ~7,178");

        Map<String, Integer> trainClasses = service.getClassCounts("train");
        assertEquals(7, trainClasses.size());
        for (String cls : service.getClasses()) {
            assertTrue(trainClasses.get(cls) > 100, "Each facial class should have > 100 samples");
        }

        var samples = service.sampleImages("train", "happy", 5);
        assertFalse(samples.isEmpty());
        assertEquals(5, samples.size());
        assertTrue(samples.get(0).isValid());
    }

    @Test
    void testTextDatasetService() {
        TextDatasetService service = new TextDatasetService();
        assertTrue(service.isAvailable());

        int trainRows = service.countRows("train");
        assertEquals(43410, trainRows, "train.tsv row count should match exactly 43,410");

        var trainMetrics = service.inspectSplit("train");
        assertEquals(43410, trainMetrics.total());
        assertEquals(0, trainMetrics.malformed());
        assertEquals(0, trainMetrics.emptyText());
        assertTrue(trainMetrics.multiLabel() > 5000);

        var samples = service.sampleComments("train", 10);
        assertEquals(10, samples.size());
        assertNotNull(samples.get(0).text());
        assertFalse(samples.get(0).text().isBlank());
    }

    @Test
    void testTextEmotionMapping() {
        assertEquals(28, TextEmotionMapping.getAllGoEmotions().size());

        assertEquals(EmotionLabel.HAPPY, TextEmotionMapping.map("joy"));
        assertEquals(EmotionLabel.HAPPY, TextEmotionMapping.map("admiration"));
        assertEquals(EmotionLabel.SAD, TextEmotionMapping.map("sadness"));
        assertEquals(EmotionLabel.ANGRY, TextEmotionMapping.map("anger"));
        assertEquals(EmotionLabel.DISGUST, TextEmotionMapping.map("disgust"));
        assertEquals(EmotionLabel.FEARFUL, TextEmotionMapping.map("fear"));
        assertEquals(EmotionLabel.STRESSED, TextEmotionMapping.map("nervousness"));
        assertEquals(EmotionLabel.OVERWHELMED, TextEmotionMapping.map("confusion"));
        assertEquals(EmotionLabel.CALM, TextEmotionMapping.map("relief"));
        assertEquals(EmotionLabel.NEUTRAL, TextEmotionMapping.map("neutral"));

        // Fallback for null/unknown
        assertEquals(EmotionLabel.NEUTRAL, TextEmotionMapping.map(null));
        assertEquals(EmotionLabel.NEUTRAL, TextEmotionMapping.map("unknown_emotion"));
    }

    @Test
    void testPreprocessors() {
        TextPreprocessor textPreprocessor = new TextPreprocessor();
        var preprocessedText = textPreprocessor.process("  I am feeling   calm and relaxed today!  ");
        assertEquals("I am feeling calm and relaxed today!", preprocessedText.cleanedText());
        assertTrue(preprocessedText.tokens().contains("calm"));
        assertTrue(preprocessedText.tokens().contains("relaxed"));

        assertThrows(IllegalArgumentException.class, () -> textPreprocessor.process("   "));
        assertThrows(IllegalArgumentException.class, () -> textPreprocessor.process(null));

        ImagePreprocessor imgPreprocessor = new ImagePreprocessor();
        assertThrows(IllegalArgumentException.class, () -> imgPreprocessor.validateFile(null));
        assertThrows(IllegalArgumentException.class, () -> imgPreprocessor.validateFile(new File("nonexistent.jpg")));
    }

    @Test
    void testAnalysisCoordinator() {
        AnalysisCoordinator coordinator = new AnalysisCoordinator();

        // 1. Text only check-in
        CheckInData textOnly = new CheckInData();
        textOnly.setThoughtsText("Today was a really joyful and wonderful day!");
        var textResult = coordinator.analyze(textOnly);

        assertFalse(textResult.hasFacial());
        assertTrue(textResult.hasText());
        assertNotNull(textResult.getTextLabel());
        assertTrue(textResult.getTextLabel().contains("Happy") || textResult.getTextLabel().contains("Positive"));
        assertNotNull(textResult.getCombinedLabel());

        // 2. Empty check-in
        CheckInData emptyData = new CheckInData();
        var emptyResult = coordinator.analyze(emptyData);
        assertFalse(emptyResult.hasFacial());
        assertFalse(emptyResult.hasText());
        assertEquals("No Signal", emptyResult.getCombinedLabel());
    }

    @Test
    void testMultimodalWeightedFusionAndAgreement() {
        MultimodalAnalysisService service = new MultimodalAnalysisService(0.7, 0.3);
        assertEquals(0.7, service.getPhotoWeight(), 0.001);
        assertEquals(0.3, service.getTextWeight(), 0.001);

        // 1. Agreeing strain signals
        var facialStrain = com.checkin.ml.EmotionPrediction.mock("Sad Expression", 80.0, Map.of("Sad", 80.0), "MockFacial", "Note");
        var textStrain = com.checkin.ml.EmotionPrediction.mock("Stressed / Overwhelmed Cue", 70.0, Map.of("Stressed", 70.0), "MockText", "Note");
        var strainResult = service.fuse(facialStrain, textStrain);
        assertEquals("Aligned Signals", strainResult.agreementStatus());
        assertTrue(strainResult.combinedLabel().contains("Strain"));
        assertEquals((80.0 * 0.7) + (70.0 * 0.3), strainResult.combinedConfidence(), 0.001);

        // 2. Disagreeing signals (e.g. Happy face vs Stressed thoughts)
        var facialHappy = com.checkin.ml.EmotionPrediction.mock("Happy Expression", 85.0, Map.of("Happy", 85.0), "MockFacial", "Note");
        var diffResult = service.fuse(facialHappy, textStrain);
        assertEquals("Signals Differ", diffResult.agreementStatus());
        assertTrue(diffResult.fusionSummary().contains("Signals Differ"));
    }

    @Test
    void testKnownFacialAndTextTestSamples() throws Exception {
        // 1. Known sample from facial test dataset
        FacialDatasetService facialService = new FacialDatasetService();
        var samples = facialService.sampleImages("test", "happy", 1);
        assertFalse(samples.isEmpty(), "Should have at least 1 test sample from happy class");
        File sampleFile = samples.get(0).filePath().toFile();
        assertTrue(sampleFile.exists());

        MockPhotoAnalysisService photoAnalysis = new MockPhotoAnalysisService();
        var facialPred = photoAnalysis.analyze(sampleFile);
        assertNotNull(facialPred);
        assertNotNull(facialPred.primaryEmotion());
        assertTrue(facialPred.isPrototypeMock());
        assertFalse(facialPred.probabilities().isEmpty());

        // 2. Known sample from text test dataset
        TextDatasetService textService = new TextDatasetService();
        var textSamples = textService.sampleComments("test", 1);
        assertFalse(textSamples.isEmpty(), "Should have at least 1 test comment");
        String knownComment = textSamples.get(0).text();
        assertNotNull(knownComment);

        MockTextAnalysisService textAnalysis = new MockTextAnalysisService();
        var textPred = textAnalysis.analyze(knownComment);
        assertNotNull(textPred);
        assertNotNull(textPred.primaryEmotion());
        assertTrue(textPred.isPrototypeMock());
    }

    @Test
    void testCheckInDAO() {
        CheckInDAO dao = new InMemoryCheckInDAO();
        String userId = "user-123";

        CheckInRecord record = new CheckInRecord(
                UUID.randomUUID().toString(),
                userId,
                "test.jpg",
                "Feeling good",
                "Neutral Expression (Signal Detected)",
                70.0,
                "Positive / Happy Cue",
                75.0,
                "Harmonious Positive Signal",
                72.5,
                LocalDateTime.now()
        );

        assertTrue(dao.save(record));
        assertEquals(1, dao.countByUserId(userId));
        var list = dao.findByUserId(userId);
        assertEquals(1, list.size());
        assertEquals(record.id(), list.get(0).id());
    }
}
