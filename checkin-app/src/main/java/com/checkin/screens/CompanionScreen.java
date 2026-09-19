package com.checkin.screens;

import com.checkin.Theme;
import com.checkin.model.AnalysisResult;
import com.checkin.model.ChatMessage;
import com.checkin.services.AICompanionService;
import com.checkin.utils.Navigation;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI Companion screen for EmoSense.
 * Provides a calm, supportive, non-clinical chat interface for emotional reflection
 * with an asynchronous thinking animation state that never blocks the JavaFX Application Thread.
 */
public class CompanionScreen extends BorderPane {

    private final Navigation navigation;
    private final AICompanionService companionService;
    private final VBox messagesContainer = new VBox(16);
    private final ScrollPane scrollPane = new ScrollPane();
    private final TextField inputField = new TextField();
    private final Button sendButton = new Button("Send");
    private final Label modeBadge = new Label();

    // Asynchronous thinking state tracking
    private boolean isResponding = false;
    private Duration thinkingDelay = Duration.millis(1500);
    private PauseTransition activePause = null;
    private Timeline thinkingTimeline = null;
    private Node currentThinkingBubble = null;

    private CompletableFuture<String> pendingFuture = null;
    private final AtomicBoolean pauseCompleted = new AtomicBoolean(false);
    private final AtomicReference<String> pendingReplyText = new AtomicReference<>(null);

    public CompanionScreen(Navigation navigation) {
        this(navigation, new AICompanionService(), null);
    }

    public CompanionScreen(Navigation navigation, AnalysisResult contextResult) {
        this(navigation, new AICompanionService(), contextResult);
    }

    public CompanionScreen(Navigation navigation, AICompanionService companionService) {
        this(navigation, companionService, null);
    }

    public CompanionScreen(Navigation navigation, AICompanionService companionService, AnalysisResult contextResult) {
        this.navigation = navigation;
        this.companionService = companionService != null ? companionService : new AICompanionService();

        if (contextResult != null) {
            this.companionService.setCheckInContext(contextResult);
        }

        getStyleClass().add("screen-root");

        setTop(buildHeader());
        setCenter(buildChatArea());
        setBottom(buildInputArea());

        // Safety: cancel any pending response when detached from scene/disposed
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                cancelPendingResponse();
            }
        });

        renderConversation();
    }

    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 32, 16, 32));
        header.setStyle(
                "-fx-background-color: " + Theme.COLOR_CARD_BG + ";" +
                "-fx-border-color: " + Theme.COLOR_CARD_BORDER + ";" +
                "-fx-border-width: 0 0 1 0;"
        );

        Button backBtn = new Button("← Dashboard");
        Theme.applySecondaryButton(backBtn);
        backBtn.setOnAction(e -> {
            cancelPendingResponse();
            if (navigation != null) navigation.showDashboard();
        });

        VBox titleBox = new VBox(2);
        Label title = new Label("AI Companion");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #f1f3f9;");

        Label subtitle = new Label("A supportive space to reflect on what you're feeling.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        updateModeBadge();
        actions.getChildren().add(modeBadge);

        if (companionService.hasCheckInContext()) {
            Label contextBadge = new Label("Check-In Context Active");
            contextBadge.setStyle(
                    "-fx-background-color: rgba(99, 102, 241, 0.15);" +
                    "-fx-text-fill: #a5b4fc;" +
                    "-fx-border-color: rgba(99, 91, 255, 0.35);" +
                    "-fx-border-radius: 6;" +
                    "-fx-background-radius: 6;" +
                    "-fx-padding: 5 10;" +
                    "-fx-font-size: 11px;" +
                    "-fx-font-weight: bold;"
            );
            actions.getChildren().add(contextBadge);
        }

        Button clearBtn = new Button("Clear Conversation");
        Theme.applySecondaryButton(clearBtn);
        clearBtn.setStyle(
                clearBtn.getStyle() +
                "-fx-font-size: 12px;" +
                "-fx-text-fill: #94a3b8;" +
                "-fx-padding: 6 14;" +
                "-fx-background-radius: 6;"
        );
        clearBtn.setOnAction(e -> handleClearConversation());

        actions.getChildren().add(clearBtn);

        header.getChildren().addAll(backBtn, titleBox, spacer, actions);
        return header;
    }

    private void updateModeBadge() {
        if (companionService.isOnlineEngineConfigured() && !companionService.isLastResponseUsedFallback()) {
            modeBadge.setText("Online Mode");
            modeBadge.setStyle(
                    "-fx-background-color: rgba(16, 185, 129, 0.12);" +
                    "-fx-text-fill: #34d399;" +
                    "-fx-border-color: rgba(16, 185, 129, 0.3);" +
                    "-fx-border-radius: 6;" +
                    "-fx-background-radius: 6;" +
                    "-fx-padding: 5 10;" +
                    "-fx-font-size: 11px;" +
                    "-fx-font-weight: bold;"
            );
        } else {
            modeBadge.setText("Offline Mode");
            modeBadge.setStyle(
                    "-fx-background-color: rgba(148, 163, 184, 0.12);" +
                    "-fx-text-fill: #94a3b8;" +
                    "-fx-border-color: rgba(148, 163, 184, 0.28);" +
                    "-fx-border-radius: 6;" +
                    "-fx-background-radius: 6;" +
                    "-fx-padding: 5 10;" +
                    "-fx-font-size: 11px;" +
                    "-fx-font-weight: bold;"
            );
        }
    }

    private Node buildChatArea() {
        messagesContainer.setPadding(new Insets(24, 32, 24, 32));
        messagesContainer.setMaxWidth(820);

        VBox outer = new VBox(messagesContainer);
        outer.setAlignment(Pos.TOP_CENTER);
        outer.setStyle("-fx-background-color: transparent;");

        scrollPane.setContent(outer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("edge-to-edge");
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        return scrollPane;
    }

    private VBox buildInputArea() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(14, 32, 16, 32));
        container.setAlignment(Pos.CENTER);
        container.setStyle(
                "-fx-background-color: " + Theme.COLOR_CARD_BG + ";" +
                "-fx-border-color: " + Theme.COLOR_CARD_BORDER + ";" +
                "-fx-border-width: 1 0 0 0;"
        );

        HBox inputRow = new HBox(12);
        inputRow.setAlignment(Pos.CENTER);
        inputRow.setMaxWidth(820);

        inputField.setPromptText("Type your thoughts or reflections here...");
        inputField.setPrefHeight(44);
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputField.setStyle(
                "-fx-background-color: #111422;" +
                "-fx-text-fill: #f1f3f9;" +
                "-fx-prompt-text-fill: #64748b;" +
                "-fx-border-color: #2e364e;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 0 14;" +
                "-fx-font-size: 13.5px;"
        );
        inputField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !isResponding) {
                handleSendMessage();
            }
        });

        sendButton.setPrefHeight(44);
        sendButton.setPrefWidth(90);
        sendButton.getStyleClass().add("analyze-button");
        sendButton.setStyle(
                "-fx-background-color: " + Theme.COLOR_ACCENT + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;" +
                "-fx-font-size: 13.5px;"
        );
        sendButton.setOnAction(e -> handleSendMessage());

        inputRow.getChildren().addAll(inputField, sendButton);

        Label disclaimer = new Label("EmoSense is an academic emotional-signal prototype. It does not provide medical diagnosis or treatment.");
        disclaimer.setStyle("-fx-font-size: 11px; -fx-font-style: italic; -fx-text-fill: #64748b;");
        disclaimer.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        container.getChildren().addAll(inputRow, disclaimer);
        return container;
    }

    /**
     * Handles the user submission workflow:
     * 1. Displays user message immediately in chat
     * 2. Temporarily disables Send button and input to prevent duplicate submissions
     * 3. Displays an animated "Thinking..." companion bubble
     * 4. Asynchronously generates the companion response on a background thread without blocking JavaFX
     * 5. Displays the response after both the response is computed and the minimum thinking animation delay passes
     */
    public void handleSendMessage() {
        String text = inputField.getText();
        if (text == null || text.trim().isEmpty() || isResponding) {
            return;
        }

        isResponding = true;
        sendButton.setDisable(true);
        inputField.setDisable(true);
        inputField.clear();

        // If currently showing empty state, remove it
        if (companionService.getConversationHistory().isEmpty()) {
            messagesContainer.getChildren().clear();
        }

        // STEP 1: Immediately add the user's message to the conversation and screen
        ChatMessage userMsg = companionService.addUserMessage(text);
        messagesContainer.getChildren().add(buildMessageBubble(userMsg));

        // STEP 2: Immediately create and display a temporary companion bubble
        currentThinkingBubble = buildThinkingBubble();
        messagesContainer.getChildren().add(currentThinkingBubble);
        scrollToBottom();

        pauseCompleted.set(false);
        pendingReplyText.set(null);

        // STEP 3: Asynchronously compute response on a background thread
        // Never blocks the JavaFX Application Thread and does not mutate conversation history until response is displayed
        pendingFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return companionService.computeCompanionReplyText();
            } catch (Exception ex) {
                return "I hear you. Take a moment for yourself, and feel free to share whatever is on your mind whenever you are ready.";
            }
        });

        // When the background future completes, finish response if minimum delay has passed
        pendingFuture.thenAccept(replyText -> {
            pendingReplyText.set(replyText);
            if (pauseCompleted.get()) {
                Platform.runLater(this::finishCompanionResponse);
            }
        });

        // STEP 4: Ensure thinking animation is visible for at least the specified delay
        activePause = new PauseTransition(thinkingDelay);
        activePause.setOnFinished(e -> {
            pauseCompleted.set(true);
            if (pendingReplyText.get() != null || (pendingFuture != null && pendingFuture.isDone())) {
                finishCompanionResponse();
            }
        });
        activePause.play();
    }

    /**
     * Completes the companion response after the thinking period:
     * Removes the thinking bubble, appends the real companion message, updates status, and re-enables input.
     */
    public void finishCompanionResponse() {
        if (!isResponding) {
            return;
        }

        // Stop animations
        if (thinkingTimeline != null) {
            thinkingTimeline.stop();
            thinkingTimeline = null;
        }
        if (activePause != null) {
            activePause.stop();
            activePause = null;
        }

        // STEP 6: Remove the temporary Thinking bubble
        if (currentThinkingBubble != null) {
            messagesContainer.getChildren().remove(currentThinkingBubble);
            currentThinkingBubble = null;
        }

        String replyText = pendingReplyText.get();
        if (replyText == null && pendingFuture != null && pendingFuture.isDone()) {
            try {
                replyText = pendingFuture.getNow(null);
            } catch (Exception ignored) {
            }
        }

        // If finishCompanionResponse was invoked synchronously (e.g. from a test), wait briefly or compute
        if (replyText == null) {
            if (pendingFuture != null) {
                try {
                    replyText = pendingFuture.get(200, TimeUnit.MILLISECONDS);
                } catch (Exception ignored) {
                }
            }
            if (replyText == null) {
                try {
                    replyText = companionService.computeCompanionReplyText();
                } catch (Exception ex) {
                    replyText = null;
                }
            }
        }

        if (replyText == null || replyText.isBlank()) {
            replyText = "I wasn't able to respond just now. Please try again.";
        }

        // Update mode badge
        updateModeBadge();

        // STEP 7: Add to conversation history and display the actual companion response
        ChatMessage reply = companionService.addCompanionMessage(replyText);
        messagesContainer.getChildren().add(buildMessageBubble(reply));

        // STEP 8: Re-enable Send and input
        isResponding = false;
        sendButton.setDisable(false);
        inputField.setDisable(false);
        inputField.requestFocus();

        scrollToBottom();
    }

    /**
     * Safely cancels any active thinking delay and animation without causing JavaFX exceptions.
     */
    public void cancelPendingResponse() {
        if (pendingFuture != null) {
            pendingFuture.cancel(true);
            pendingFuture = null;
        }
        if (activePause != null) {
            activePause.stop();
            activePause = null;
        }
        if (thinkingTimeline != null) {
            thinkingTimeline.stop();
            thinkingTimeline = null;
        }
        if (currentThinkingBubble != null) {
            messagesContainer.getChildren().remove(currentThinkingBubble);
            currentThinkingBubble = null;
        }
        isResponding = false;
        sendButton.setDisable(false);
        inputField.setDisable(false);
    }

    public void handleClearConversation() {
        cancelPendingResponse();
        companionService.clearConversation();
        updateModeBadge();
        renderConversation();
    }

    private void renderConversation() {
        messagesContainer.getChildren().clear();

        List<ChatMessage> history = companionService.getConversationHistory();
        if (history.isEmpty()) {
            messagesContainer.getChildren().add(buildEmptyState());
        } else {
            for (ChatMessage msg : history) {
                messagesContainer.getChildren().add(buildMessageBubble(msg));
            }
        }
    }

    private VBox buildEmptyState() {
        VBox emptyCard = new VBox(16);
        emptyCard.setPadding(new Insets(40, 32, 40, 32));
        emptyCard.setAlignment(Pos.CENTER);
        Theme.applyCardStyle(emptyCard);

        Label title = new Label("How are you feeling today?");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f1f3f9;");

        Label subtitle = new Label("Share what's on your mind. I'm here to help you reflect.");
        subtitle.setStyle("-fx-font-size: 13.5px; -fx-text-fill: " + Theme.COLOR_TEXT_SECONDARY + ";");
        subtitle.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        emptyCard.getChildren().addAll(title, subtitle);

        if (companionService.hasCheckInContext()) {
            VBox contextBox = new VBox(6);
            contextBox.setAlignment(Pos.CENTER_LEFT);
            contextBox.setPadding(new Insets(12, 16, 12, 16));
            contextBox.setStyle(
                    "-fx-background-color: rgba(99, 91, 255, 0.08);" +
                    "-fx-border-color: rgba(99, 91, 255, 0.25);" +
                    "-fx-border-radius: 8;" +
                    "-fx-background-radius: 8;"
            );

            Label ctxLabel = new Label("Active Check-In Context:");
            ctxLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #a5b4fc;");

            Label ctxDetails = new Label(companionService.getFormattedContextSummary());
            ctxDetails.setStyle("-fx-font-size: 12px; -fx-text-fill: #cbd5e1;");
            ctxDetails.setWrapText(true);

            contextBox.getChildren().addAll(ctxLabel, ctxDetails);
            emptyCard.getChildren().add(contextBox);
        }

        Label starterHeader = new Label("Suggested Starters:");
        starterHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #64748b;");

        HBox chips = new HBox(8);
        chips.setAlignment(Pos.CENTER);

        chips.getChildren().add(createStarterChip("I had a difficult day."));
        chips.getChildren().add(createStarterChip("Today was quite productive."));
        if (companionService.hasCheckInContext()) {
            chips.getChildren().add(createStarterChip("Tell me about my check-in."));
        } else {
            chips.getChildren().add(createStarterChip("Feeling a bit overwhelmed."));
        }

        emptyCard.getChildren().addAll(starterHeader, chips);
        return emptyCard;
    }

    private Button createStarterChip(String promptText) {
        Button chip = new Button(promptText);
        chip.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.05);" +
                "-fx-text-fill: #c7d2fe;" +
                "-fx-border-color: #2e364e;" +
                "-fx-border-radius: 20;" +
                "-fx-background-radius: 20;" +
                "-fx-padding: 6 14;" +
                "-fx-font-size: 12px;" +
                "-fx-cursor: hand;"
        );
        chip.setOnAction(e -> {
            if (isResponding) return;
            inputField.setText(promptText);
            handleSendMessage();
        });
        return chip;
    }

    /**
     * Builds the temporary thinking bubble matching the companion visual style:
     * dark card, subtle border, rounded corners, EMOSENSE COMPANION tag, and animated dots.
     */
    private HBox buildThinkingBubble() {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);

        VBox bubble = new VBox(6);
        bubble.setMaxWidth(560);
        bubble.setPadding(new Insets(12, 16, 12, 16));
        bubble.setStyle(
                "-fx-background-color: #161a29;" +
                "-fx-background-radius: 14 14 14 3;" +
                "-fx-border-color: #242b45;" +
                "-fx-border-radius: 14 14 14 3;" +
                "-fx-border-width: 1;"
        );

        Label nameTag = new Label("EMOSENSE COMPANION");
        nameTag.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #818cf8; -fx-letter-spacing: 0.5px;");

        Label dotsLabel = new Label("● ○ ○");
        dotsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #818cf8; -fx-font-weight: bold; -fx-letter-spacing: 2px;");

        Label thinkingLabel = new Label("Thinking...");
        thinkingLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-style: italic;");

        VBox content = new VBox(4);
        content.getChildren().addAll(dotsLabel, thinkingLabel);

        bubble.getChildren().addAll(nameTag, content);
        row.getChildren().add(bubble);

        // Continuous JavaFX Timeline animation:
        // ● ○ ○ -> ○ ● ○ -> ○ ○ ● -> ○ ○ ● -> ○ ● ○ -> ● ○ ○
        thinkingTimeline = new Timeline(
                new KeyFrame(Duration.millis(0), e -> dotsLabel.setText("● ○ ○")),
                new KeyFrame(Duration.millis(250), e -> dotsLabel.setText("○ ● ○")),
                new KeyFrame(Duration.millis(500), e -> dotsLabel.setText("○ ○ ●")),
                new KeyFrame(Duration.millis(750), e -> dotsLabel.setText("○ ○ ●")),
                new KeyFrame(Duration.millis(1000), e -> dotsLabel.setText("○ ● ○")),
                new KeyFrame(Duration.millis(1250), e -> dotsLabel.setText("● ○ ○")),
                new KeyFrame(Duration.millis(1500))
        );
        thinkingTimeline.setCycleCount(Animation.INDEFINITE);
        thinkingTimeline.play();

        return row;
    }

    private HBox buildMessageBubble(ChatMessage msg) {
        HBox row = new HBox();
        VBox bubble = new VBox(6);
        bubble.setMaxWidth(560);
        bubble.setPadding(new Insets(12, 16, 12, 16));

        Label textLabel = new Label(msg.text());
        textLabel.setWrapText(true);

        Label timeLabel = new Label(msg.getFormattedTime());
        timeLabel.setStyle("-fx-font-size: 10px;");

        if (msg.isUser()) {
            row.setAlignment(Pos.CENTER_RIGHT);
            bubble.setStyle(
                    "-fx-background-color: #242c47;" +
                    "-fx-background-radius: 14 14 3 14;" +
                    "-fx-border-color: #3b466e;" +
                    "-fx-border-radius: 14 14 3 14;" +
                    "-fx-border-width: 1;"
            );
            textLabel.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #f1f3f9;");
            timeLabel.setStyle(timeLabel.getStyle() + "-fx-text-fill: #94a3b8;");

            HBox meta = new HBox(timeLabel);
            meta.setAlignment(Pos.CENTER_RIGHT);
            bubble.getChildren().addAll(textLabel, meta);

        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            bubble.setStyle(
                    "-fx-background-color: #161a29;" +
                    "-fx-background-radius: 14 14 14 3;" +
                    "-fx-border-color: #242b45;" +
                    "-fx-border-radius: 14 14 14 3;" +
                    "-fx-border-width: 1;"
            );
            Label nameTag = new Label("EMOSENSE COMPANION");
            nameTag.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #818cf8; -fx-letter-spacing: 0.5px;");

            textLabel.setStyle("-fx-font-size: 13.5px; -fx-text-fill: #e2e8f0; -fx-line-spacing: 2px;");
            timeLabel.setStyle(timeLabel.getStyle() + "-fx-text-fill: #64748b;");

            HBox meta = new HBox(timeLabel);
            meta.setAlignment(Pos.CENTER_LEFT);
            bubble.getChildren().addAll(nameTag, textLabel, meta);
        }

        row.getChildren().add(bubble);
        return row;
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    public boolean isResponding() {
        return isResponding;
    }

    public boolean isSendButtonDisabled() {
        return sendButton.isDisabled();
    }

    public Node getCurrentThinkingBubble() {
        return currentThinkingBubble;
    }

    public Timeline getThinkingTimeline() {
        return thinkingTimeline;
    }

    public void setThinkingDelay(Duration delay) {
        this.thinkingDelay = delay != null ? delay : Duration.millis(1500);
    }

    public Duration getThinkingDelay() {
        return thinkingDelay;
    }

    public AICompanionService getCompanionService() {
        return companionService;
    }

    public TextField getInputField() {
        return inputField;
    }

    public Button getSendButton() {
        return sendButton;
    }

    public VBox getMessagesContainer() {
        return messagesContainer;
    }
}
