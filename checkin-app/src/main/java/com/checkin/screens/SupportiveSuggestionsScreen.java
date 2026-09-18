package com.checkin.screens;

import com.checkin.Theme;
import com.checkin.model.AnalysisResult;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Supportive Suggestions screen: provides suggestions tailored to
 * the check-in outcome and a handoff to the AI Companion or Dashboard.
 */
public class SupportiveSuggestionsScreen extends VBox {

    public SupportiveSuggestionsScreen(AnalysisResult result, Runnable onOpenCompanion, Runnable onStartOver, Runnable onDashboard) {
        super(16);
        getStyleClass().add("screen-root");
        setPadding(new Insets(36));
        setAlignment(Pos.CENTER);

        Label title = new Label("Supportive Suggestions");
        title.getStyleClass().add("screen-title");

        Label basedOn = new Label("Based on: " + result.getCombinedLabel());
        basedOn.getStyleClass().add("screen-subtitle");

        VBox suggestionsBox = new VBox(10);
        suggestionsBox.setMaxWidth(520);
        suggestionsBox.setAlignment(Pos.CENTER_LEFT);
        Theme.applyCardStyle(suggestionsBox);
        suggestionsBox.setPadding(new Insets(20));

        for (String suggestion : buildSuggestions(result)) {
            Label line = new Label("•  " + suggestion);
            line.setWrapText(true);
            line.getStyleClass().add("suggestion-line");
            suggestionsBox.getChildren().add(line);
        }

        Button companionButton = new Button("Talk to Your AI Companion");
        companionButton.getStyleClass().add("analyze-button");
        companionButton.setPrefWidth(260);
        companionButton.setPrefHeight(44);
        companionButton.setOnAction(e -> {
            if (onOpenCompanion != null) onOpenCompanion.run();
        });

        Button startOverButton = new Button("New Check-In");
        Theme.applySecondaryButton(startOverButton);
        startOverButton.setOnAction(e -> {
            if (onStartOver != null) onStartOver.run();
        });

        Button dashboardButton = new Button("Dashboard");
        Theme.applySecondaryButton(dashboardButton);
        dashboardButton.setOnAction(e -> {
            if (onDashboard != null) onDashboard.run();
        });

        HBox actions = new HBox(12, companionButton, startOverButton, dashboardButton);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(10, 0, 0, 0));

        getChildren().addAll(title, basedOn, suggestionsBox, actions);
    }

    private List<String> buildSuggestions(AnalysisResult result) {
        String label = result.getCombinedLabel().toLowerCase();
        List<String> suggestions = new ArrayList<>();

        if (label.contains("negative") || label.contains("stress") || label.contains("overwhelm")
                || label.contains("sad") || label.contains("anx")) {
            suggestions.add("Take a short pause and try a few slow, deep breaths.");
            suggestions.add("Write down one simple action within your control today.");
            suggestions.add("Consider reaching out to a friend, colleague, or loved one.");
        } else if (label.contains("positive") || label.contains("content") || label.contains("happy")
                || label.contains("hope")) {
            suggestions.add("Take a moment to savor what went well today.");
            suggestions.add("Reflect on the positive events that contributed to this feeling.");
            suggestions.add("Share your positive energy with someone nearby.");
        } else {
            suggestions.add("Check in with yourself again later today to observe how your feelings evolve.");
            suggestions.add("Notice the subtle factors that influenced your state of mind.");
        }
        suggestions.add("Remember: EmoSense is a wellness reflection tool, not a medical service.");
        return suggestions;
    }
}
