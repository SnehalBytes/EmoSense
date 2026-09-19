package com.checkin.screens;

import com.checkin.Theme;
import com.checkin.model.AnalysisResult;
import com.checkin.utils.Navigation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.*;

/**
 * Enhanced Results Screen for EmoSense.
 * Displays genuine model predictions, confidence percentages, 7-class facial
 * probability distributions, GoEmotions textual cues, multimodal fusion insights,
 * and neutral non-medical supportive suggestions.
 */
public class ResultsScreen extends VBox {

    private static final List<String> FACIAL_CLASSES = List.of(
            "Angry", "Disgust", "Fear", "Happy", "Sad", "Surprise", "Neutral"
    );

    // Primary constructor with the 3 requested action buttons
    public ResultsScreen(AnalysisResult result, Runnable onNewCheckIn, Runnable onViewHistory, Runnable onAICompanion) {
        this(result, onNewCheckIn, onViewHistory, onAICompanion, null);
    }

    // Convenience constructor accepting Navigation
    public ResultsScreen(AnalysisResult result, Navigation navigation) {
        this(
                result,
                navigation != null ? navigation::showCheckIn : null,
                navigation != null ? navigation::showHistory : null,
                navigation != null ? () -> navigation.showCompanion(result) : null,
                navigation != null ? navigation::showDashboard : null
        );
    }


    public ResultsScreen(AnalysisResult result, Runnable onNewCheckIn, Runnable onViewHistory, Runnable onAICompanion, Runnable onDashboard) {
        super(20);
        getStyleClass().add("screen-root");
        setPadding(new Insets(24, 30, 24, 30));
        setAlignment(Pos.TOP_CENTER);

        // 1. Header
        Label title = new Label("Your Emotional Check-In");
        title.getStyleClass().add("screen-title");

        Label subtitle = new Label("EmoSense combines available signals from your submitted photo and/or thoughts.");
        subtitle.getStyleClass().add("screen-subtitle");
        subtitle.setWrapText(true);
        subtitle.setAlignment(Pos.CENTER);

        VBox headerBox = new VBox(6, title, subtitle);
        headerBox.setAlignment(Pos.CENTER);

        // 2. Main Scrollable Container
        VBox resultsBox = new VBox(16);
        resultsBox.setMaxWidth(680);
        resultsBox.setAlignment(Pos.TOP_CENTER);

        // Card 1: Facial Signal
        resultsBox.getChildren().add(buildFacialCard(result));

        // Card 2: Textual Signal
        resultsBox.getChildren().add(buildTextualCard(result));

        // Card 3: Multimodal Insight
        resultsBox.getChildren().add(buildMultimodalCard(result));

        // Card 4: Supportive Suggestions
        resultsBox.getChildren().add(buildSupportiveCard(result));

        VBox outerScrollBox = new VBox(resultsBox);
        outerScrollBox.setAlignment(Pos.TOP_CENTER);
        outerScrollBox.setPadding(new Insets(4, 12, 16, 12));

        ScrollPane scrollPane = new ScrollPane(outerScrollBox);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("edge-to-edge");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // 3. Action Buttons Row
        Button newCheckInBtn = new Button("New Check-In");
        newCheckInBtn.getStyleClass().add("analyze-button");
        newCheckInBtn.setPrefWidth(160);
        newCheckInBtn.setPrefHeight(42);
        newCheckInBtn.setOnAction(e -> {
            if (onNewCheckIn != null) onNewCheckIn.run();
        });

        Button viewHistoryBtn = new Button("View History");
        Theme.applySecondaryButton(viewHistoryBtn);
        viewHistoryBtn.setPrefWidth(150);
        viewHistoryBtn.setPrefHeight(42);
        viewHistoryBtn.setOnAction(e -> {
            if (onViewHistory != null) onViewHistory.run();
            else if (onDashboard != null) onDashboard.run();
        });

        Button aiCompanionBtn = new Button("AI Companion");
        Theme.applySecondaryButton(aiCompanionBtn);
        aiCompanionBtn.setStyle(aiCompanionBtn.getStyle() + "; -fx-border-color: #635bff; -fx-border-radius: 8; -fx-border-width: 1;");
        aiCompanionBtn.setPrefWidth(150);
        aiCompanionBtn.setPrefHeight(42);
        aiCompanionBtn.setOnAction(e -> {
            if (onAICompanion != null) onAICompanion.run();
        });

        HBox actions = new HBox(12, newCheckInBtn, viewHistoryBtn, aiCompanionBtn);
        if (onDashboard != null) {
            Button dashBtn = new Button("Dashboard");
            Theme.applySecondaryButton(dashBtn);
            dashBtn.setPrefWidth(130);
            dashBtn.setPrefHeight(42);
            dashBtn.setOnAction(e -> onDashboard.run());
            actions.getChildren().add(dashBtn);
        }
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(8, 0, 8, 0));

        getChildren().addAll(headerBox, scrollPane, actions);
    }

    // =========================================================================
    // Card 1: Facial Signal Card
    // =========================================================================
    private VBox buildFacialCard(AnalysisResult result) {
        VBox card = createCardBase();

        // Title row
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label heading = new Label("FACIAL SIGNAL");
        heading.getStyleClass().add("result-heading");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label();
        if (!result.hasFacial()) {
            statusBadge.setText("NOT PROVIDED");
            statusBadge.getStyleClass().add("badge-single");
        } else if (result.isFacialUnavailable()) {
            statusBadge.setText("UNAVAILABLE");
            statusBadge.getStyleClass().add("badge-unavailable");
        } else {
            statusBadge.setText("REAL MODEL");
            statusBadge.getStyleClass().add("badge-real");
        }

        titleRow.getChildren().addAll(heading, spacer, statusBadge);
        card.getChildren().add(titleRow);

        // Content
        if (!result.hasFacial()) {
            Label missingLabel = new Label("Facial signal not provided.");
            missingLabel.getStyleClass().add("result-value");
            missingLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px;");

            Label note = new Label("Upload a facial photo during check-in to evaluate observed facial expressions.");
            note.getStyleClass().add("fusion-note");

            card.getChildren().addAll(missingLabel, note);
            return card;
        }

        if (result.isFacialUnavailable()) {
            Label unavailLabel = new Label("Analysis unavailable");
            unavailLabel.getStyleClass().add("result-value");
            unavailLabel.setStyle("-fx-text-fill: #f87171;");

            Label note = new Label("Facial emotion model is currently unavailable on this device.");
            note.getStyleClass().add("fusion-note");

            card.getChildren().addAll(unavailLabel, note);
            return card;
        }

        // Predicted Emotion & Confidence
        Label emotionValue = new Label(result.getFacialLabel());
        emotionValue.getStyleClass().add("result-value");

        ProgressBar mainBar = new ProgressBar(result.getFacialConfidence() / 100.0);
        mainBar.setPrefWidth(420);
        mainBar.setPrefHeight(10);

        Label pctLabel = new Label(String.format(Locale.US, "%.1f%% confidence", result.getFacialConfidence()));
        pctLabel.getStyleClass().add("result-percent");

        HBox barRow = new HBox(12, mainBar, pctLabel);
        barRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(emotionValue, barRow);

        // 7-Class Probability Distribution
        Map<String, Double> probs = result.getFacialProbabilities();
        if (!probs.isEmpty()) {
            VBox distBox = new VBox(6);
            distBox.setPadding(new Insets(8, 0, 0, 0));

            Label distTitle = new Label("FACIAL PROBABILITY DISTRIBUTION (7 CLASSES)");
            distTitle.getStyleClass().add("distribution-header");
            distBox.getChildren().add(distTitle);

            String predictedEmotion = result.getFacialLabel();

            for (String className : FACIAL_CLASSES) {
                double p = probs.getOrDefault(className, 0.0);
                boolean isTop = className.equalsIgnoreCase(predictedEmotion);

                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);

                Label nameLbl = new Label(className);
                nameLbl.setPrefWidth(75);
                nameLbl.getStyleClass().add(isTop ? "prob-label-top" : "prob-label");

                ProgressBar pBar = new ProgressBar(Math.min(1.0, p / 100.0));
                pBar.setPrefWidth(380);
                pBar.getStyleClass().add(isTop ? "prob-bar-top" : "prob-bar");
                HBox.setHgrow(pBar, Priority.ALWAYS);

                Label valLbl = new Label(String.format(Locale.US, "%.1f%%", p));
                valLbl.setPrefWidth(55);
                valLbl.setAlignment(Pos.CENTER_RIGHT);
                valLbl.getStyleClass().add(isTop ? "prob-pct-top" : "prob-pct");

                row.getChildren().addAll(nameLbl, pBar, valLbl);
                distBox.getChildren().add(row);
            }

            card.getChildren().add(distBox);
        }

        return card;
    }

    // =========================================================================
    // Card 2: Textual Signal Card
    // =========================================================================
    private VBox buildTextualCard(AnalysisResult result) {
        VBox card = createCardBase();

        // Title row
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label heading = new Label("TEXTUAL SIGNAL");
        heading.getStyleClass().add("result-heading");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label();
        if (!result.hasText()) {
            statusBadge.setText("NOT PROVIDED");
            statusBadge.getStyleClass().add("badge-single");
        } else if (result.isTextUnavailable()) {
            statusBadge.setText("UNAVAILABLE");
            statusBadge.getStyleClass().add("badge-unavailable");
        } else {
            statusBadge.setText("REAL MODEL");
            statusBadge.getStyleClass().add("badge-real");
        }

        titleRow.getChildren().addAll(heading, spacer, statusBadge);
        card.getChildren().add(titleRow);

        // Content
        if (!result.hasText()) {
            Label missingLabel = new Label("Text signal not provided.");
            missingLabel.getStyleClass().add("result-value");
            missingLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px;");

            Label note = new Label("Enter your written thoughts during check-in to evaluate textual emotional cues.");
            note.getStyleClass().add("fusion-note");

            card.getChildren().addAll(missingLabel, note);
            return card;
        }

        if (result.isTextUnavailable()) {
            Label unavailLabel = new Label("Analysis unavailable");
            unavailLabel.getStyleClass().add("result-value");
            unavailLabel.setStyle("-fx-text-fill: #f87171;");

            Label note = new Label("Text emotion model is currently unavailable on this device.");
            note.getStyleClass().add("fusion-note");

            card.getChildren().addAll(unavailLabel, note);
            return card;
        }

        // Predicted Emotion & Confidence
        Label emotionValue = new Label(result.getTextLabel());
        emotionValue.getStyleClass().add("result-value");

        ProgressBar mainBar = new ProgressBar(result.getTextConfidence() / 100.0);
        mainBar.setPrefWidth(420);
        mainBar.setPrefHeight(10);

        Label pctLabel = new Label(String.format(Locale.US, "%.1f%% confidence", result.getTextConfidence()));
        pctLabel.getStyleClass().add("result-percent");

        HBox barRow = new HBox(12, mainBar, pctLabel);
        barRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(emotionValue, barRow);

        // Detected GoEmotions Signal Tag
        String rawEmotion = result.getRawTextEmotion();
        if (rawEmotion != null && !rawEmotion.isBlank()) {
            HBox tagRow = new HBox(10);
            tagRow.setAlignment(Pos.CENTER_LEFT);
            tagRow.setPadding(new Insets(4, 0, 0, 0));

            Label tag = new Label("Detected Emotional Nuance: " + capitalize(rawEmotion));
            tag.getStyleClass().add("detected-tag");

            Label modelRef = new Label("MiniLMv2 Transformer (GoEmotions 28 classes)");
            modelRef.getStyleClass().add("fusion-disclaimer");

            tagRow.getChildren().addAll(tag, modelRef);
            card.getChildren().add(tagRow);
        }

        return card;
    }

    // =========================================================================
    // Card 3: Multimodal Insight Card
    // =========================================================================
    private VBox buildMultimodalCard(AnalysisResult result) {
        VBox card = createCardBase();
        card.getStyleClass().add("combined-card");

        // Title row
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label heading = new Label("MULTIMODAL INSIGHT");
        heading.getStyleClass().add("result-heading");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label fusionBadge = new Label("Prototype Multimodal Fusion");
        fusionBadge.getStyleClass().add("badge-fusion");

        titleRow.getChildren().addAll(heading, spacer, fusionBadge);
        card.getChildren().add(titleRow);

        // Both available
        if (result.hasFacial() && result.hasText() && !result.isFacialUnavailable() && !result.isTextUnavailable()) {
            Label combinedLabel = new Label(result.getCombinedLabel());
            combinedLabel.getStyleClass().add("result-value");

            ProgressBar bar = new ProgressBar(result.getCombinedConfidence() / 100.0);
            bar.setPrefWidth(420);
            bar.setPrefHeight(10);

            Label pctLabel = new Label(String.format(Locale.US, "%.1f%% fusion confidence", result.getCombinedConfidence()));
            pctLabel.getStyleClass().add("result-percent");

            HBox barRow = new HBox(12, bar, pctLabel);
            barRow.setAlignment(Pos.CENTER_LEFT);

            // Signal Nuance & Agreement
            String agreement = result.getCombinedInsight() != null ? result.getCombinedInsight().agreementStatus() : "Signals Evaluated";
            Label agreementLbl = new Label("Signal Alignment: " + agreement);
            agreementLbl.getStyleClass().add("fusion-label");

            Label signalNote = new Label(
                    "Your facial expression is one signal. Your words are another. "
                    + "Differences between physical expressions and written thoughts are normal and expected."
            );
            signalNote.getStyleClass().add("fusion-note");
            signalNote.setWrapText(true);

            Label disclaimer = new Label(
                    "Prototype Multimodal Fusion is an exploratory wellness signal summary, not a medical or clinical diagnosis."
            );
            disclaimer.getStyleClass().add("fusion-disclaimer");
            disclaimer.setWrapText(true);

            card.getChildren().addAll(combinedLabel, barRow, agreementLbl, signalNote, disclaimer);
        } else if (result.hasFacial() && !result.isFacialUnavailable()) {
            Label singleLabel = new Label("Single Modality: Facial Signal Evaluated");
            singleLabel.getStyleClass().add("result-value");
            singleLabel.setStyle("-fx-font-size: 18px;");

            Label note = new Label("Analysis based solely on static facial photo. Textual cues were not provided.");
            note.getStyleClass().add("fusion-note");

            Label disclaimer = new Label("Prototype signal evaluation for self-reflection. Not a medical assessment.");
            disclaimer.getStyleClass().add("fusion-disclaimer");

            card.getChildren().addAll(singleLabel, note, disclaimer);
        } else if (result.hasText() && !result.isTextUnavailable()) {
            Label singleLabel = new Label("Single Modality: Textual Cue Evaluated");
            singleLabel.getStyleClass().add("result-value");
            singleLabel.setStyle("-fx-font-size: 18px;");

            Label note = new Label("Analysis based solely on written thoughts. Facial signal was not provided.");
            note.getStyleClass().add("fusion-note");

            Label disclaimer = new Label("Prototype signal evaluation for self-reflection. Not a medical assessment.");
            disclaimer.getStyleClass().add("fusion-disclaimer");

            card.getChildren().addAll(singleLabel, note, disclaimer);
        } else {
            Label unavail = new Label("Analysis unavailable");
            unavail.getStyleClass().add("result-value");
            unavail.setStyle("-fx-text-fill: #f87171;");

            Label note = new Label("Emotion analysis could not be completed on provided inputs.");
            note.getStyleClass().add("fusion-note");

            card.getChildren().addAll(unavail, note);
        }

        return card;
    }

    // =========================================================================
    // Card 4: Supportive Reflections Card
    // =========================================================================
    private VBox buildSupportiveCard(AnalysisResult result) {
        VBox card = createCardBase();
        card.getStyleClass().add("suggestions-card");

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label heading = new Label("SUPPORTIVE REFLECTIONS");
        heading.getStyleClass().add("result-heading");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label optBadge = new Label("Optional · Non-Medical");
        optBadge.getStyleClass().add("badge-single");

        titleRow.getChildren().addAll(heading, spacer, optBadge);

        Label intro = new Label("A few neutral reflections to consider based on your check-in:");
        intro.getStyleClass().add("card-subtitle");

        VBox suggestionsBox = new VBox(8);
        suggestionsBox.setPadding(new Insets(4, 0, 4, 0));

        List<String> items = generateSupportiveSuggestions(result);
        for (String item : items) {
            Label line = new Label("•  " + item);
            line.setWrapText(true);
            line.getStyleClass().add("suggestion-bullet");
            suggestionsBox.getChildren().add(line);
        }

        Label disclaimer = new Label("These reflections are general wellness considerations, not counseling, treatment, or clinical diagnosis.");
        disclaimer.getStyleClass().add("suggestion-disclaimer");
        disclaimer.setWrapText(true);

        card.getChildren().addAll(titleRow, intro, suggestionsBox, disclaimer);
        return card;
    }

    private List<String> generateSupportiveSuggestions(AnalysisResult result) {
        List<String> list = new ArrayList<>();
        String combined = (result.getCombinedLabel() != null ? result.getCombinedLabel() : "").toLowerCase();
        String facial = (result.getFacialLabel() != null ? result.getFacialLabel() : "").toLowerCase();
        String text = (result.getTextLabel() != null ? result.getTextLabel() : "").toLowerCase();

        boolean isChallenging = combined.contains("strain") || combined.contains("challeng")
                || facial.contains("sad") || facial.contains("angry") || facial.contains("fear")
                || text.contains("sad") || text.contains("angry") || text.contains("frustrat");

        boolean isPositive = combined.contains("positive") || combined.contains("uplift")
                || facial.contains("happy") || text.contains("happy") || text.contains("joy");

        if (isChallenging) {
            list.add("Take a short pause and try a few minutes of slow, steady breathing.");
            list.add("Write down what is on your mind or step outside for a brief walk.");
            list.add("Connect with someone you trust if you feel like talking.");
        } else if (isPositive) {
            list.add("Take a moment to acknowledge and savor what went well today.");
            list.add("Continue an activity that helps you feel engaged and positive.");
            list.add("Consider sharing your positive momentum with someone around you.");
        } else {
            list.add("Take a short break from screens to refresh your mental focus.");
            list.add("Write down one or two priorities for the rest of your day.");
            list.add("Check in with yourself again later to observe how your feelings evolve.");
        }

        return list;
    }

    private VBox createCardBase() {
        VBox card = new VBox(10);
        card.getStyleClass().add("result-card");
        Theme.applyCardStyle(card);
        card.setPadding(new Insets(18, 22, 18, 22));
        card.setAlignment(Pos.CENTER_LEFT);
        return card;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
