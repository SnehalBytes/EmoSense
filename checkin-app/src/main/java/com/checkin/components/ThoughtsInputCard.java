package com.checkin.components;

import com.checkin.Theme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Reusable card: "YOUR THOUGHTS" — text area for free-form reflection
 * with prompt chips, character counter (0/1000), and a Clear action.
 */
public class ThoughtsInputCard extends VBox {

    private static final int MAX_CHARS = 1000;

    private final TextArea textArea = new TextArea();
    private final Label charCounter = new Label("0 / " + MAX_CHARS);
    private final Button clearButton = new Button("Clear");

    private Consumer<String> onTextChanged;

    public ThoughtsInputCard() {
        super(12);
        setPadding(new Insets(20));
        Theme.applyCardStyle(this);
        setAlignment(Pos.TOP_LEFT);

        VBox headerBox = new VBox(3);
        headerBox.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("YOUR THOUGHTS");
        title.getStyleClass().add("card-title");

        Label subtitle = new Label("Put your feelings into words.");
        subtitle.getStyleClass().add("card-subtitle");

        headerBox.getChildren().addAll(title, subtitle);

        FlowPane prompts = buildPromptButtons();

        textArea.setPromptText("What's on your mind right now?");
        textArea.setWrapText(true);
        textArea.setPrefRowCount(7);
        VBox.setVgrow(textArea, Priority.ALWAYS);

        textArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > MAX_CHARS) {
                textArea.setText(newVal.substring(0, MAX_CHARS));
                return;
            }
            int len = newVal != null ? newVal.length() : 0;
            charCounter.setText(len + " / " + MAX_CHARS);
            if (onTextChanged != null) {
                onTextChanged.accept(newVal);
            }
        });

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        charCounter.getStyleClass().add("hint-label");
        clearButton.getStyleClass().add("secondary-button");
        clearButton.setOnAction(e -> {
            textArea.clear();
            textArea.requestFocus();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        footer.getChildren().addAll(charCounter, spacer, clearButton);

        getChildren().addAll(headerBox, prompts, textArea, footer);
    }

    private FlowPane buildPromptButtons() {
        FlowPane prompts = new FlowPane(8, 8);
        prompts.setAlignment(Pos.CENTER_LEFT);

        String[] suggestions = {
                "How am I feeling?",
                "What is bothering me?",
                "What made me happy today?",
                "What am I worried about?"
        };

        for (String suggestion : suggestions) {
            Button b = new Button(suggestion);
            b.getStyleClass().add("prompt-chip");
            b.setOnAction(e -> insertPrompt(suggestion));
            prompts.getChildren().add(b);
        }
        return prompts;
    }

    private void insertPrompt(String prompt) {
        String current = textArea.getText();
        if (current == null || current.isBlank()) {
            textArea.setText(prompt + " ");
        } else if (!current.endsWith(" ")) {
            textArea.setText(current + " " + prompt + " ");
        } else {
            textArea.setText(current + prompt + " ");
        }
        textArea.positionCaret(textArea.getText().length());
        textArea.requestFocus();
    }

    public String getThoughtsText() {
        return textArea.getText();
    }

    public boolean hasThoughts() {
        return textArea.getText() != null && !textArea.getText().trim().isEmpty();
    }

    public void setOnTextChanged(Consumer<String> callback) {
        this.onTextChanged = callback;
    }
}
