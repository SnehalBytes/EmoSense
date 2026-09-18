package com.checkin;

import com.checkin.ml.EmotionPrediction;
import com.checkin.ml.EmotionPrediction.Modality;
import com.checkin.model.AnalysisResult;
import com.checkin.screens.ResultsScreen;
import com.checkin.services.MultimodalAnalysisService.CombinedInsight;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ResultsScreenTest {

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already started
        }
    }

    @Test
    @DisplayName("Verify facial-only result rendering and missing text indicator")
    void testFacialOnlyResultRendering() {
        Map<String, Double> dist = new LinkedHashMap<>();
        dist.put("Angry", 0.01);
        dist.put("Disgust", 0.0);
        dist.put("Fear", 0.01);
        dist.put("Happy", 99.53);
        dist.put("Sad", 0.01);
        dist.put("Surprise", 0.1);
        dist.put("Neutral", 0.34);

        EmotionPrediction facialPred = EmotionPrediction.real(
                "Happy", 99.53, dist, Modality.FACIAL, "Real ViT Model", "Inference OK"
        );

        CombinedInsight singleInsight = new CombinedInsight(
                "Happy", 99.53, "Facial Signal Only", "Analysis based solely on photo."
        );

        AnalysisResult result = new AnalysisResult(
                "Happy", 99.53,
                null, 0.0,
                "Happy", 99.53,
                facialPred, null, singleInsight
        );

        assertTrue(result.hasFacial());
        assertFalse(result.hasText());
        assertTrue(result.isFacialRealModel());
        assertEquals(7, result.getFacialProbabilities().size());

        ResultsScreen screen = new ResultsScreen(result, () -> {}, () -> {}, () -> {});
        assertNotNull(screen);

        List<String> allLabels = collectAllLabelTexts(screen);
        assertTrue(allLabels.contains("FACIAL SIGNAL"), "Must render FACIAL SIGNAL heading");
        assertTrue(allLabels.contains("REAL MODEL"), "Must render REAL MODEL badge");
        assertTrue(allLabels.contains("Happy"), "Must render predicted emotion Happy");
        assertTrue(allLabels.contains("Text signal not provided."), "Must render missing text notice");
        assertTrue(allLabels.contains("Single Modality: Facial Signal Evaluated"), "Must render single modality note");
    }

    @Test
    @DisplayName("Verify text-only result rendering and missing facial indicator")
    void testTextOnlyResultRendering() {
        Map<String, Double> dist = new LinkedHashMap<>();
        dist.put("sadness", 98.61);

        EmotionPrediction textPred = EmotionPrediction.real(
                "Sad", 98.61, dist, Modality.TEXTUAL, "Real RoBERTa Model",
                "Real ONNX GoEmotions Inference — Raw emotion: sadness (98.61%). Output classes: 28"
        );

        CombinedInsight singleInsight = new CombinedInsight(
                "Sad", 98.61, "Textual Cue Only", "Analysis based solely on text."
        );

        AnalysisResult result = new AnalysisResult(
                null, 0.0,
                "Sad", 98.61,
                "Sad", 98.61,
                null, textPred, singleInsight
        );

        assertFalse(result.hasFacial());
        assertTrue(result.hasText());
        assertTrue(result.isTextRealModel());
        assertEquals("sadness", result.getRawTextEmotion());

        ResultsScreen screen = new ResultsScreen(result, () -> {}, () -> {}, () -> {});
        assertNotNull(screen);

        List<String> allLabels = collectAllLabelTexts(screen);
        assertTrue(allLabels.contains("TEXTUAL SIGNAL"), "Must render TEXTUAL SIGNAL heading");
        assertTrue(allLabels.contains("REAL MODEL"), "Must render REAL MODEL badge");
        assertTrue(allLabels.contains("Sad"), "Must render predicted emotion Sad");
        assertTrue(allLabels.contains("Facial signal not provided."), "Must render missing photo notice");
        assertTrue(allLabels.contains("Single Modality: Textual Cue Evaluated"), "Must render single modality note");
        assertTrue(allLabels.stream().anyMatch(l -> l.contains("Detected Emotional Nuance: Sadness")),
                "Must display detected GoEmotions nuance");
    }

    @Test
    @DisplayName("Verify multimodal result rendering with both modalities active")
    void testMultimodalResultRendering() {
        Map<String, Double> fDist = Map.of(
                "Angry", 0.1, "Disgust", 0.0, "Fear", 0.0, "Happy", 99.5, "Sad", 0.1, "Surprise", 0.1, "Neutral", 0.2
        );
        EmotionPrediction facialPred = EmotionPrediction.real(
                "Happy", 99.5, fDist, Modality.FACIAL, "ViT", "OK"
        );

        Map<String, Double> tDist = Map.of("excitement", 96.65);
        EmotionPrediction textPred = EmotionPrediction.real(
                "Happy", 96.65, tDist, Modality.TEXTUAL, "RoBERTa",
                "Real ONNX GoEmotions Inference — Raw emotion: excitement (96.65%). Output classes: 28"
        );

        CombinedInsight combinedInsight = new CombinedInsight(
                "Positive / Uplifted Signal", 98.08, "Aligned Signals",
                "Signals indicate positive emotional balance across both inputs."
        );

        AnalysisResult result = new AnalysisResult(
                "Happy", 99.5,
                "Happy", 96.65,
                "Positive / Uplifted Signal", 98.08,
                facialPred, textPred, combinedInsight
        );

        assertTrue(result.hasFacial());
        assertTrue(result.hasText());
        assertTrue(result.isFacialRealModel());
        assertTrue(result.isTextRealModel());

        ResultsScreen screen = new ResultsScreen(result, () -> {}, () -> {}, () -> {});
        assertNotNull(screen);

        List<String> allLabels = collectAllLabelTexts(screen);
        assertTrue(allLabels.contains("MULTIMODAL INSIGHT"));
        assertTrue(allLabels.contains("Prototype Multimodal Fusion"));
        assertTrue(allLabels.contains("Positive / Uplifted Signal"));
        assertTrue(allLabels.stream().anyMatch(l -> l.toLowerCase().contains("differences between physical expressions and written thoughts are normal")));
    }

    @Test
    @DisplayName("Verify unavailable model state shows 'Analysis unavailable' and zero Neutral fallback")
    void testUnavailableModelState() {
        EmotionPrediction unavailFacial = EmotionPrediction.unavailable(Modality.FACIAL, "Model file not found");
        EmotionPrediction unavailText = EmotionPrediction.unavailable(Modality.TEXTUAL, "Tokenizer missing");

        CombinedInsight unavailCombined = new CombinedInsight(
                "Analysis unavailable", 0.0, "Analysis Error", "Analysis unavailable."
        );

        AnalysisResult result = new AnalysisResult(
                "Analysis unavailable", 0.0,
                "Analysis unavailable", 0.0,
                "Analysis unavailable", 0.0,
                unavailFacial, unavailText, unavailCombined
        );

        assertTrue(result.isFacialUnavailable());
        assertTrue(result.isTextUnavailable());

        ResultsScreen screen = new ResultsScreen(result, () -> {}, () -> {}, () -> {});
        assertNotNull(screen);

        List<String> allLabels = collectAllLabelTexts(screen);
        assertTrue(allLabels.contains("Analysis unavailable"));
        assertTrue(allLabels.contains("UNAVAILABLE"));
        assertFalse(allLabels.contains("Neutral Expression"), "Must NEVER fall back to Neutral Expression");
    }

    @Test
    @DisplayName("Verify 7-class facial probability distribution progress bars are rendered")
    void testProbabilityDistributionRendering() {
        Map<String, Double> dist = new LinkedHashMap<>();
        dist.put("Angry", 1.2);
        dist.put("Disgust", 0.3);
        dist.put("Fear", 2.5);
        dist.put("Happy", 85.4);
        dist.put("Sad", 4.1);
        dist.put("Surprise", 3.0);
        dist.put("Neutral", 3.5);

        EmotionPrediction facialPred = EmotionPrediction.real(
                "Happy", 85.4, dist, Modality.FACIAL, "ViT", "OK"
        );

        AnalysisResult result = new AnalysisResult(
                "Happy", 85.4,
                null, 0.0,
                "Happy", 85.4,
                facialPred, null, null
        );

        ResultsScreen screen = new ResultsScreen(result, () -> {}, () -> {}, () -> {});
        List<String> allLabels = collectAllLabelTexts(screen);

        // Check each class label exists in the distribution
        List<String> expectedClasses = List.of("Angry", "Disgust", "Fear", "Happy", "Sad", "Surprise", "Neutral");
        for (String c : expectedClasses) {
            assertTrue(allLabels.contains(c), "Distribution must display class: " + c);
        }

        assertTrue(allLabels.contains("85.4%"));
        assertTrue(allLabels.contains("1.2%"));
    }

    @Test
    @DisplayName("Verify supportive suggestions section is rendered with non-medical framing")
    void testSupportiveSuggestionsRendering() {
        AnalysisResult result = new AnalysisResult(
                "Happy", 95.0,
                "Happy", 92.0,
                "Positive / Uplifted Signal", 93.5
        );

        ResultsScreen screen = new ResultsScreen(result, () -> {}, () -> {}, () -> {});
        List<String> allLabels = collectAllLabelTexts(screen);

        assertTrue(allLabels.contains("SUPPORTIVE REFLECTIONS"));
        assertTrue(allLabels.contains("Optional · Non-Medical"));
        assertTrue(allLabels.stream().anyMatch(l -> l.contains("general wellness considerations, not counseling")));
    }

    @Test
    @DisplayName("Verify Action Buttons wiring and triggers")
    void testActionButtonsWiring() {
        AtomicBoolean checkInTriggered = new AtomicBoolean(false);
        AtomicBoolean historyTriggered = new AtomicBoolean(false);
        AtomicBoolean companionTriggered = new AtomicBoolean(false);

        AnalysisResult result = new AnalysisResult("Happy", 90.0, null, 0.0, "Happy", 90.0);
        ResultsScreen screen = new ResultsScreen(
                result,
                () -> checkInTriggered.set(true),
                () -> historyTriggered.set(true),
                () -> companionTriggered.set(true)
        );

        List<Button> buttons = collectAllButtons(screen);
        assertEquals(3, buttons.size(), "Must contain exactly 3 main action buttons");

        Button newCheckInBtn = buttons.stream().filter(b -> b.getText().equals("New Check-In")).findFirst().orElseThrow();
        Button viewHistoryBtn = buttons.stream().filter(b -> b.getText().equals("View History")).findFirst().orElseThrow();
        Button aiCompanionBtn = buttons.stream().filter(b -> b.getText().equals("AI Companion")).findFirst().orElseThrow();

        newCheckInBtn.fire();
        assertTrue(checkInTriggered.get(), "New Check-In button must fire callback");

        viewHistoryBtn.fire();
        assertTrue(historyTriggered.get(), "View History button must fire callback");

        aiCompanionBtn.fire();
        assertTrue(companionTriggered.get(), "AI Companion button must fire callback");
    }

    private List<String> collectAllLabelTexts(Node root) {
        List<String> list = new ArrayList<>();
        collectLabelsRecursive(root, list);
        return list;
    }

    private void collectLabelsRecursive(Node node, List<String> list) {
        if (node instanceof Label l && l.getText() != null) {
            list.add(l.getText());
        }
        if (node instanceof ScrollPane sp && sp.getContent() != null) {
            collectLabelsRecursive(sp.getContent(), list);
        }
        if (node instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectLabelsRecursive(child, list);
            }
        }
    }

    private List<Button> collectAllButtons(Node root) {
        List<Button> list = new ArrayList<>();
        collectButtonsRecursive(root, list);
        return list;
    }

    private void collectButtonsRecursive(Node node, List<Button> list) {
        if (node instanceof Button b) {
            list.add(b);
        }
        if (node instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectButtonsRecursive(child, list);
            }
        }
    }
}
