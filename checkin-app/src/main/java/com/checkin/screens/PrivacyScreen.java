package com.checkin.screens;

import com.checkin.Theme;
import com.checkin.utils.Navigation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

/**
 * Privacy and Data Practices screen for EmoSense.
 * Explains the academic prototype scope, voluntary multimodal data inputs,
 * probabilistic model inferences, local database association, and the strict
 * non-clinical, non-medical disclaimer.
 */
public class PrivacyScreen extends BorderPane {

    private final Navigation navigation;
    private final Runnable onBack;
    private final VBox contentArea = new VBox(18);

    public PrivacyScreen(Navigation navigation) {
        this(navigation, null);
    }

    public PrivacyScreen(Navigation navigation, Runnable onBack) {
        this.navigation = navigation;
        this.onBack = onBack != null ? onBack : (navigation != null ? navigation::showDashboard : () -> {});

        getStyleClass().add("screen-root");

        setTop(buildHeader());
        setCenter(buildScrollContainer());

        renderContent();
    }

    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 36, 18, 36));
        header.setStyle(
                "-fx-background-color: " + Theme.COLOR_CARD_BG + ";" +
                "-fx-border-color: " + Theme.COLOR_CARD_BORDER + ";" +
                "-fx-border-width: 0 0 1 0;"
        );

        Button backBtn = new Button("← Back");
        Theme.applySecondaryButton(backBtn);
        backBtn.setOnAction(e -> onBack.run());

        VBox titleBox = new VBox(2);
        Label title = new Label("Privacy & Data Practices");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #f1f3f9;");

        Label subtitle = new Label("Information on data usage, model boundaries, and academic prototype guidelines.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label academicBadge = new Label("Academic Prototype");
        academicBadge.setStyle(
                "-fx-background-color: rgba(99, 102, 241, 0.15);" +
                "-fx-text-fill: #a5b4fc;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 5 12;" +
                "-fx-background-radius: 6;" +
                "-fx-border-color: rgba(99, 102, 241, 0.35);" +
                "-fx-border-radius: 6;"
        );

        header.getChildren().addAll(backBtn, titleBox, spacer, academicBadge);
        return header;
    }

    private ScrollPane buildScrollContainer() {
        contentArea.setPadding(new Insets(26, 36, 36, 36));
        contentArea.setAlignment(Pos.TOP_CENTER);
        contentArea.setMaxWidth(760);

        VBox outer = new VBox(contentArea);
        outer.setAlignment(Pos.TOP_CENTER);

        ScrollPane scrollPane = new ScrollPane(outer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("edge-to-edge");
        return scrollPane;
    }

    private void renderContent() {
        contentArea.getChildren().clear();

        // Section 1: Non-Clinical Medical Disclaimer (Primary Alert)
        contentArea.getChildren().add(buildDisclaimerCard());

        // Section 2: Academic Prototype Purpose
        contentArea.getChildren().add(buildSectionCard(
                "ACADEMIC PROTOTYPE PURPOSE",
                "EmoSense is an academic research prototype developed as a B.Tech capstone project. " +
                "Its objective is to examine multimodal affective computing methods on desktop systems—specifically combining " +
                "computer vision models for facial expression classification and natural language processing models for textual emotion cues."
        ));

        // Section 3: Data Inputs During Check-Ins
        contentArea.getChildren().add(buildSectionCard(
                "DATA COLLECTION DURING CHECK-INS",
                "During a check-in, users may voluntarily provide:\n\n" +
                "• An uploaded facial image (photograph)\n" +
                "• Written thoughts or reflections (text)\n" +
                "• Or both inputs simultaneously.\n\n" +
                "Submitting inputs is completely voluntary. The application does not activate your webcam continuously or stream live video; " +
                "it strictly analyzes the static image or text you explicitly submit for that check-in."
        ));

        // Section 4: Model Analysis & Probabilistic Outputs
        contentArea.getChildren().add(buildSectionCard(
                "MODEL-BASED SIGNAL ANALYSIS & UNCERTAINTY",
                "The system processes submitted inputs through embedded machine learning models:\n\n" +
                "• Facial expressions are classified across 7 FER2013 categories (Angry, Disgust, Fear, Happy, Sad, Surprise, Neutral).\n" +
                "• Written thoughts are analyzed using a fine-tuned RoBERTa language model to identify underlying emotional cues.\n" +
                "• Multimodal insight produces an experimental combined signal based on input confidence and alignment.\n\n" +
                "IMPORTANT: All model outputs are probabilistic approximations. They do not constitute an exact or objective measure of how " +
                "a person truly feels. Prediction confidence can be influenced by camera angle, lighting conditions, cultural nuances, " +
                "sarcasm, and expressive ambiguity."
        ));

        // Section 5: User Account & Data Storage
        contentArea.getChildren().add(buildSectionCard(
                "ACCOUNT & DATA STORAGE",
                "Check-in records and analysis results are associated with the authenticated user account in the configured local MySQL database " +
                "(or temporary in-memory store if offline). User passwords are cryptographically hashed using standard BCrypt before storage. " +
                "Data is accessible only through your authenticated session and is not shared with external third-party advertisers."
        ));

        // Section 6: Capabilities & Realistic Boundaries
        contentArea.getChildren().add(buildSectionCard(
                "SYSTEM CAPABILITIES & REALISTIC BOUNDARIES",
                "As an educational prototype:\n\n" +
                "• We do not claim military-grade security, zero-knowledge encryption, or real-time micro-expression surveillance.\n" +
                "• Stored check-in records remain in the application database according to user persistence policies.\n" +
                "• Users are encouraged to maintain awareness of their local desktop security environment."
        ));
    }

    private VBox buildDisclaimerCard() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20, 24, 20, 24));
        Theme.applyCardStyle(card);
        card.setStyle(
                card.getStyle() +
                "-fx-border-color: rgba(248, 113, 113, 0.45);" +
                "-fx-border-width: 1.5;" +
                "-fx-background-color: linear-gradient(to right, #241922, #181422);"
        );

        Label title = new Label("NON-MEDICAL & NON-CLINICAL NOTICE");
        title.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 800; -fx-text-fill: #fca5a5; -fx-letter-spacing: 0.5px;");

        Label notice = new Label(
                "EmoSense is NOT a medical, psychological, psychiatric, or mental-health diagnostic tool. " +
                "The emotional signals, confidence percentages, and supportive thoughts generated by the application " +
                "are probabilistic outputs intended solely for personal reflection and academic demonstration.\n\n" +
                "Users must not rely on this application for medical decisions, diagnosis, or clinical treatment. " +
                "If you or someone you know is experiencing emotional distress or a mental health crisis, please consult a " +
                "licensed healthcare professional or contact a professional crisis helpline."
        );
        notice.setStyle("-fx-font-size: 13px; -fx-text-fill: #f1f3f9; -fx-line-spacing: 2px;");
        notice.setWrapText(true);

        card.getChildren().addAll(title, notice);
        return card;
    }

    private VBox buildSectionCard(String heading, String body) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20, 24, 20, 24));
        Theme.applyCardStyle(card);

        Label title = new Label(heading);
        title.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #818cf8; -fx-letter-spacing: 0.5px;");

        Label text = new Label(body);
        text.setStyle("-fx-font-size: 13px; -fx-text-fill: #cbd5e1; -fx-line-spacing: 2px;");
        text.setWrapText(true);

        card.getChildren().addAll(title, text);
        return card;
    }

    public VBox getContentArea() {
        return contentArea;
    }
}
