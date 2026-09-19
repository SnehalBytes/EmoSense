package com.checkin.screens;

import com.checkin.Theme;
import com.checkin.components.AnalyzeButton;
import com.checkin.components.CheckInSummary;
import com.checkin.components.PhotoUploadCard;
import com.checkin.components.ThoughtsInputCard;
import com.checkin.model.CheckInData;
import com.checkin.utils.Navigation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

/**
 * Redesigned "Express Yourself" Check-In Screen.
 * Places FACIAL SIGNAL and YOUR THOUGHTS side-by-side in a responsive layout,
 * displays real-time input status, and offers the adaptive Analyze action.
 * Optimized so the complete interface fits naturally inside the window without
 * clipping or unnecessary vertical scrolling.
 */
public class CheckInScreen extends BorderPane {

    private final Navigation navigation;
    private final CheckInData checkInData = new CheckInData();

    private final PhotoUploadCard photoCard = new PhotoUploadCard();
    private final ThoughtsInputCard thoughtsCard = new ThoughtsInputCard();
    private final CheckInSummary summary = new CheckInSummary();
    private final AnalyzeButton analyzeButton = new AnalyzeButton();
    private final Label multimodalBadge = new Label("Multimodal Check-In Active");

    public CheckInScreen(Navigation navigation) {
        this.navigation = navigation;
        getStyleClass().add("screen-root");

        setTop(buildHeader());
        setCenter(buildMainLayout());

        setupEventBindings();
        refreshState();
    }

    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 36, 12, 36));

        Button backBtn = new Button("← Dashboard");
        Theme.applySecondaryButton(backBtn);
        backBtn.setOnAction(e -> navigation.showDashboard());

        VBox titleBox = new VBox(2);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("How are you feeling today?");
        title.setStyle(
                "-fx-font-size: 22px;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: #f1f3f9;"
        );

        Label subtitle = new Label("Share a photo, your thoughts, or both. EmoSense can analyze photo-only, text-only, or combined multimodal signals.");
        subtitle.setStyle(
                "-fx-font-size: 13px;" +
                "-fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";"
        );

        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        multimodalBadge.getStyleClass().add("multimodal-chip");
        multimodalBadge.setVisible(false);
        multimodalBadge.setManaged(false);

        header.getChildren().addAll(backBtn, titleBox, spacer, multimodalBadge);
        return header;
    }

    private ScrollPane buildMainLayout() {
        VBox layout = new VBox(14);
        layout.setPadding(new Insets(6, 36, 18, 36));
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setMaxWidth(1100);

        // Two Cards Side-by-Side
        HBox cardsRow = new HBox(18);
        cardsRow.setAlignment(Pos.CENTER);

        HBox.setHgrow(photoCard, Priority.ALWAYS);
        HBox.setHgrow(thoughtsCard, Priority.ALWAYS);
        photoCard.setMaxWidth(Double.MAX_VALUE);
        thoughtsCard.setMaxWidth(Double.MAX_VALUE);
        photoCard.setPrefWidth(500);
        thoughtsCard.setPrefWidth(500);

        // Fixed clean heights to prevent clipping and scrolling on standard resolutions
        photoCard.setMinHeight(360);
        photoCard.setPrefHeight(380);
        thoughtsCard.setMinHeight(360);
        thoughtsCard.setPrefHeight(380);

        cardsRow.getChildren().addAll(photoCard, thoughtsCard);

        // Check-In Summary (Full width)
        summary.setMaxWidth(Double.MAX_VALUE);

        // Analyze Button Footer
        HBox buttonContainer = new HBox();
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(4, 0, 8, 0));

        analyzeButton.setPrefWidth(300);
        analyzeButton.setPrefHeight(46);
        buttonContainer.getChildren().add(analyzeButton);

        layout.getChildren().addAll(cardsRow, summary, buttonContainer);

        VBox outerWrapper = new VBox(layout);
        outerWrapper.setAlignment(Pos.TOP_CENTER);

        ScrollPane scrollPane = new ScrollPane(outerWrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("edge-to-edge");
        return scrollPane;
    }

    private void setupEventBindings() {
        photoCard.setOnPhotoChanged(file -> {
            if (file != null) {
                checkInData.setPhotoFile(file);
            } else {
                checkInData.clearPhoto();
            }
            refreshState();
        });

        thoughtsCard.setOnTextChanged(text -> {
            checkInData.setThoughtsText(text);
            refreshState();
        });

        analyzeButton.setOnAction(e -> {
            if (checkInData.hasPhoto() || checkInData.hasThoughts()) {
                navigation.showLoading(checkInData);
            }
        });
    }

    private void refreshState() {
        boolean hasPhoto = checkInData.hasPhoto();
        boolean hasThoughts = checkInData.hasThoughts();

        summary.update(hasPhoto, hasThoughts);
        analyzeButton.updateState(hasPhoto, hasThoughts);

        boolean multimodal = hasPhoto && hasThoughts;
        multimodalBadge.setVisible(multimodal);
        multimodalBadge.setManaged(multimodal);
    }
}
