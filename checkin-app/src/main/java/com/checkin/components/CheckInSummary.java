package com.checkin.components;

import com.checkin.Theme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Reusable card: "YOUR CHECK-IN" — displays real-time checklist status
 * for photo and thoughts inputs.
 */
public class CheckInSummary extends VBox {

    private final Label photoStatus = new Label();
    private final Label thoughtsStatus = new Label();

    public CheckInSummary() {
        super(8);
        setPadding(new Insets(14, 20, 14, 20));
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("summary-box");

        Label title = new Label("YOUR CHECK-IN");
        title.getStyleClass().add("summary-title");

        photoStatus.getStyleClass().add("summary-line");
        thoughtsStatus.getStyleClass().add("summary-line");

        HBox statusRow = new HBox(30, photoStatus, thoughtsStatus);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(title, statusRow);
        update(false, false);
    }

    /** Refresh the checklist based on input presence. */
    public void update(boolean hasPhoto, boolean hasThoughts) {
        photoStatus.setText((hasPhoto ? "✓" : "○") + "  " + (hasPhoto ? "Photo added" : "No photo added"));
        thoughtsStatus.setText((hasThoughts ? "✓" : "○") + "  " + (hasThoughts ? "Thoughts added" : "No thoughts added"));

        photoStatus.getStyleClass().removeAll("status-complete", "status-incomplete");
        photoStatus.getStyleClass().add(hasPhoto ? "status-complete" : "status-incomplete");

        thoughtsStatus.getStyleClass().removeAll("status-complete", "status-incomplete");
        thoughtsStatus.getStyleClass().add(hasThoughts ? "status-complete" : "status-incomplete");
    }
}
