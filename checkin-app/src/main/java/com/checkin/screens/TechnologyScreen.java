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
 * Technology and About screen for EmoSense.
 * Introduces the project's academic mission, explains core multimodal capabilities,
 * details the JavaFX and ONNX machine learning architecture, and outlines the B.Tech scope.
 */
public class TechnologyScreen extends BorderPane {

    private final Navigation navigation;
    private final Runnable onBack;
    private final VBox contentArea = new VBox(18);

    public TechnologyScreen(Navigation navigation) {
        this(navigation, null);
    }

    public TechnologyScreen(Navigation navigation, Runnable onBack) {
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
        Label title = new Label("EmoSense");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #f1f3f9;");

        Label subtitle = new Label("Understanding the emotions we don't always express.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label techBadge = new Label("Technology & Architecture");
        techBadge.setStyle(
                "-fx-background-color: rgba(99, 102, 241, 0.15);" +
                "-fx-text-fill: #a5b4fc;" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 5 12;" +
                "-fx-background-radius: 6;" +
                "-fx-border-color: rgba(99, 102, 241, 0.35);" +
                "-fx-border-radius: 6;"
        );

        header.getChildren().addAll(backBtn, titleBox, spacer, techBadge);
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

        // 1. Hero Introduction Card
        contentArea.getChildren().add(buildHeroCard());

        // 2. Main Capabilities Cards
        contentArea.getChildren().add(buildCapabilityCard(
                "FACIAL SIGNAL ANALYSIS",
                "Computer Vision Pipeline",
                "When an uploaded facial photo is provided, the system utilizes an ultra-lightweight face detector " +
                "to isolate facial regions. The detected face is normalized and evaluated by a Convolutional Neural Network " +
                "running locally via ONNX Runtime Java, estimating probability distributions across 7 facial expression classes " +
                "(Angry, Disgust, Fear, Happy, Sad, Surprise, Neutral).\n\n" +
                "Analysis operates strictly on the uploaded static image; no continuous webcam surveillance or live video feed is captured."
        ));

        contentArea.getChildren().add(buildCapabilityCard(
                "TEXTUAL SIGNAL ANALYSIS",
                "Natural Language Processing",
                "Written thoughts submitted during a check-in are preprocessed and tokenized using a Byte-Level Byte-Pair Encoding (BPE) tokenizer. " +
                "A RoBERTa model fine-tuned on GoEmotions processes the token sequence to identify 28 granular emotional cues, " +
                "which are systematically mapped to primary emotion categories."
        ));

        contentArea.getChildren().add(buildCapabilityCard(
                "MULTIMODAL INSIGHT",
                "Signal Fusion Engine",
                "When both a photo and written thoughts are submitted, EmoSense executes a multimodal fusion heuristic. " +
                "The engine compares the facial expression signal with the textual emotional cue, assessing agreement and relative " +
                "confidence levels to present a unified multimodal perspective."
        ));

        contentArea.getChildren().add(buildCapabilityCard(
                "CHECK-IN HISTORY & INSIGHTS",
                "Longitudinal Personal Trends",
                "Each completed check-in is saved and associated exclusively with the authenticated user ID. " +
                "Users can review past check-ins, view saved model distributions without re-running inference, and explore " +
                "distribution analytics across their historical submissions."
        ));

        contentArea.getChildren().add(buildCapabilityCard(
                "AI COMPANION",
                "Supportive Reflection Space",
                "The companion feature provides a calm conversational interface for users to talk through what is on their mind, " +
                "reflect upon check-in insights, and engage with non-clinical, empathetic supportive dialogues."
        ));

        // 3. Technical Stack Specifications Card
        contentArea.getChildren().add(buildTechStackCard());

        // 4. Academic Project Overview Card
        contentArea.getChildren().add(buildProjectInfoCard());
    }

    private VBox buildHeroCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(24, 28, 24, 28));
        Theme.applyCardStyle(card);
        card.setStyle(
                card.getStyle() +
                "-fx-background-color: linear-gradient(to right, #1a1e36, #161a29);" +
                "-fx-border-color: rgba(99, 102, 241, 0.35);" +
                "-fx-border-width: 1.5;"
        );

        Label badge = new Label("ABOUT EMOSENSE");
        badge.setStyle(
                "-fx-background-color: rgba(99, 102, 241, 0.2);" +
                "-fx-text-fill: #a5b4fc;" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 3 8;" +
                "-fx-background-radius: 4;"
        );

        Label headline = new Label("Multimodal Affective Computing on the Desktop");
        headline.setStyle("-fx-font-size: 19px; -fx-font-weight: 800; -fx-text-fill: #f1f3f9;");

        Label body = new Label(
                "EmoSense is an academic desktop prototype designed to investigate how multimodal signals—visual expressions " +
                "and textual thoughts—can be computationally interpreted to support self-reflection and emotional awareness. " +
                "By combining local neural network inference with an accessible desktop UI, EmoSense offers users a secure, " +
                "calm space for personal introspection."
        );
        body.setStyle("-fx-font-size: 13px; -fx-text-fill: #cbd5e1; -fx-line-spacing: 2px;");
        body.setWrapText(true);

        card.getChildren().addAll(badge, headline, body);
        return card;
    }

    private VBox buildCapabilityCard(String heading, String tag, String description) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20, 24, 20, 24));
        Theme.applyCardStyle(card);

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(heading);
        title.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #818cf8; -fx-letter-spacing: 0.5px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label tagLabel = new Label(tag);
        tagLabel.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.05);" +
                "-fx-text-fill: #a5b4fc;" +
                "-fx-font-size: 10px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 2 7;" +
                "-fx-background-radius: 4;"
        );

        topRow.getChildren().addAll(title, spacer, tagLabel);

        Label desc = new Label(description);
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #cbd5e1; -fx-line-spacing: 2px;");
        desc.setWrapText(true);

        card.getChildren().addAll(topRow, desc);
        return card;
    }

    private VBox buildTechStackCard() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(20, 24, 20, 24));
        Theme.applyCardStyle(card);

        Label title = new Label("TECHNOLOGY STACK & SPECIFICATIONS");
        title.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #818cf8; -fx-letter-spacing: 0.5px;");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);

        int r = 0;
        addTechRow(grid, r++, "Platform & Runtime", "Java 17 (OpenJDK)");
        addTechRow(grid, r++, "Desktop GUI", "JavaFX 21 (Declarative Theme & CSS styling)");
        addTechRow(grid, r++, "ML Inference Engine", "ONNX Runtime Java (com.microsoft.onnxruntime)");
        addTechRow(grid, r++, "Face Detection", "Ultra-Lightweight Face Detector (320x240 ONNX)");
        addTechRow(grid, r++, "Facial Emotion Model", "ResNet / CNN (FER2013 7 classes, 48x48 ONNX)");
        addTechRow(grid, r++, "Text Emotion Model", "RoBERTa (GoEmotions 28 classes ONNX)");
        addTechRow(grid, r++, "Text Tokenization", "Byte-Level BPE (vocabulary.json & merges.txt)");
        addTechRow(grid, r++, "AI Companion Engine", "Google Gemini API (with Local Java Fallback)");
        addTechRow(grid, r++, "Persistence Layer", "MySQL 8.0 with JDBC & In-Memory Prototype DAO");
        addTechRow(grid, r++, "Credential Security", "Cryptographic BCrypt Password Hashing");

        card.getChildren().addAll(title, grid);
        return card;
    }

    private void addTechRow(GridPane grid, int row, String label, String value) {
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        l.setPrefWidth(180);

        Label v = new Label(value);
        v.setStyle("-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

        grid.add(l, 0, row);
        grid.add(v, 1, row);
    }

    private VBox buildProjectInfoCard() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20, 24, 20, 24));
        Theme.applyCardStyle(card);

        Label title = new Label("ACADEMIC PROJECT INFORMATION");
        title.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #818cf8; -fx-letter-spacing: 0.5px;");

        Label desc = new Label(
                "EmoSense is an academic B.Tech Computer Science & Engineering capstone prototype. " +
                "It explores the intersection of affective computing, human-computer interaction (HCI), " +
                "and embedded machine learning models on consumer desktop hardware.\n\n" +
                "Notice: This prototype does not claim real-time continuous facial tracking, temporal micro-expression " +
                "detection, clinical diagnosis, or commercial medical effectiveness. All evaluations are conducted within " +
                "the parameters of the academic study."
        );
        desc.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #cbd5e1; -fx-line-spacing: 2px;");
        desc.setWrapText(true);

        card.getChildren().addAll(title, desc);
        return card;
    }

    public VBox getContentArea() {
        return contentArea;
    }
}
