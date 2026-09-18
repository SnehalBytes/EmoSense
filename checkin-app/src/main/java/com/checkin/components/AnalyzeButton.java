package com.checkin.components;

import javafx.scene.control.Button;

/**
 * Primary action button: "Analyze My Check-In".
 * Dynamically enables/disables and adapts its text based on input presence.
 */
public class AnalyzeButton extends Button {

    private static final String DEFAULT_LABEL = "Analyze My Check-In";

    public AnalyzeButton() {
        super(DEFAULT_LABEL);
        getStyleClass().add("analyze-button");
        setDisable(true);
    }

    /**
     * Update button state and adaptive label.
     */
    public void updateState(boolean hasPhoto, boolean hasThoughts) {
        boolean canAnalyze = hasPhoto || hasThoughts;
        setDisable(!canAnalyze);

        if (hasPhoto && hasThoughts) {
            setText("Analyze My Check-In");
        } else if (hasPhoto) {
            setText("Analyze My Photo");
        } else if (hasThoughts) {
            setText("Analyze My Thoughts");
        } else {
            setText(DEFAULT_LABEL);
        }
    }
}
