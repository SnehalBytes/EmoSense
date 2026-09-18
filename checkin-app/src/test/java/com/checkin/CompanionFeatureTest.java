package com.checkin;

import com.checkin.dao.InMemoryAnalysisResultDAO;
import com.checkin.dao.InMemoryCheckInDAO;
import com.checkin.model.AnalysisResult;
import com.checkin.model.AnalysisResultRecord;
import com.checkin.model.CheckInRecord;
import com.checkin.model.ChatMessage;
import com.checkin.screens.CompanionScreen;
import com.checkin.screens.ResultsScreen;
import com.checkin.services.AICompanionService;
import com.checkin.services.CheckInPersistenceService;
import com.checkin.services.UserSession;
import com.checkin.utils.Navigation;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.util.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class CompanionFeatureTest {

    private AICompanionService companionService;

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @BeforeEach
    void setUp() {
        UserSession.getInstance().logout();
        companionService = new AICompanionService();
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    @Test
    @DisplayName("Opening Companion screen initializes UI components properly")
    void testOpeningCompanion() {
        CompanionScreen screen = new CompanionScreen(null, companionService);
        assertNotNull(screen.getInputField(), "Input field must be initialized");
        assertNotNull(screen.getSendButton(), "Send button must be initialized");
        assertNotNull(screen.getMessagesContainer(), "Messages container must be initialized");

        List<String> labels = collectAllLabelTexts(screen);
        assertTrue(labels.contains("AI Companion"), "Must display screen title");
        assertTrue(labels.contains("A supportive space to reflect on what you're feeling."), "Must display subtitle");
    }

    @Test
    @DisplayName("Empty-state rendering: Displays reflection prompt when no messages exist")
    void testEmptyStateRendering() {
        CompanionScreen screen = new CompanionScreen(null, companionService);
        List<String> labels = collectAllLabelTexts(screen);

        assertTrue(labels.contains("How are you feeling today?"), "Must show empty state heading");
        assertTrue(labels.contains("Share what's on your mind. I'm here to help you reflect."), "Must show reflection invitation");
        assertTrue(labels.stream().anyMatch(l -> l.contains("academic emotional-signal prototype")), "Must show prototype disclaimer");
    }

    @Test
    @DisplayName("Empty message validation: Blank or whitespace input is ignored")
    void testEmptyMessageValidation() {
        CompanionScreen screen = new CompanionScreen(null, companionService);

        screen.getInputField().setText("");
        screen.getSendButton().fire();
        assertEquals(0, companionService.getConversationHistory().size(), "Empty input must not create message");

        screen.getInputField().setText("     ");
        screen.getSendButton().fire();
        assertEquals(0, companionService.getConversationHistory().size(), "Whitespace input must not create message");
    }

    // =========================================================================
    // 15 DEDICATED TESTS FOR THINKING ANIMATION & CONTEXTUAL RESPONSES
    // =========================================================================

    @Test
    @DisplayName("1. Thinking bubble appears immediately after sending")
    void testThinkingBubbleAppearsImmediately() {
        CompanionScreen screen = new CompanionScreen(null, companionService);
        screen.getInputField().setText("I had a difficult day.");
        screen.getSendButton().fire();

        assertTrue(screen.isResponding(), "Screen must be in responding state immediately");
        assertNotNull(screen.getCurrentThinkingBubble(), "Thinking bubble must be created immediately");
        assertTrue(screen.getMessagesContainer().getChildren().contains(screen.getCurrentThinkingBubble()),
                "Thinking bubble must be visibly added to chat area");

        List<String> labels = collectAllLabelTexts(screen);
        assertTrue(labels.contains("Thinking..."), "Must show Thinking... text");
        assertTrue(labels.contains("EMOSENSE COMPANION"), "Must display EMOSENSE COMPANION header");
        assertTrue(labels.contains("I had a difficult day."), "User message must appear immediately");
    }

    @Test
    @DisplayName("2. Thinking animation starts")
    void testThinkingAnimationStarts() {
        CompanionScreen screen = new CompanionScreen(null, companionService);
        screen.getInputField().setText("Hello");
        screen.getSendButton().fire();

        assertNotNull(screen.getThinkingTimeline(), "Thinking timeline animation must be initialized");
        assertEquals(Animation.Status.RUNNING, screen.getThinkingTimeline().getStatus(),
                "Timeline animation must be actively running");
    }

    @Test
    @DisplayName("3. Actual response is NOT displayed immediately")
    void testActualResponseNotDisplayedImmediately() {
        CompanionScreen screen = new CompanionScreen(null, companionService);
        screen.getInputField().setText("I won the running competition in my college");
        screen.getSendButton().fire();

        assertEquals(1, companionService.getConversationHistory().size(),
                "Only user message should be in history immediately (no immediate response)");
        assertTrue(companionService.getConversationHistory().get(0).isUser());
    }

    @Test
    @DisplayName("4. Thinking bubble is removed after response")
    void testThinkingBubbleRemovedAfterResponse() {
        CompanionScreen screen = new CompanionScreen(null, companionService);
        screen.getInputField().setText("Hello");
        screen.getSendButton().fire();

        assertNotNull(screen.getCurrentThinkingBubble());
        screen.finishCompanionResponse();

        assertNull(screen.getCurrentThinkingBubble(), "Thinking bubble reference must be cleared");
        List<String> labels = collectAllLabelTexts(screen);
        assertFalse(labels.contains("Thinking..."), "Thinking... must be removed from the chat area");
    }

    @Test
    @DisplayName("5. Actual response appears after the delay")
    void testActualResponseAppearsAfterDelay() throws Exception {
        CompanionScreen screen = new CompanionScreen(null, companionService);
        screen.setThinkingDelay(Duration.millis(80));
        screen.getInputField().setText("Hello companion");
        screen.getSendButton().fire();

        assertTrue(screen.isResponding());
        CountDownLatch latch = new CountDownLatch(1);

        Timeline watcher = new Timeline(new KeyFrame(Duration.millis(15), e -> {
            if (!screen.isResponding()) {
                latch.countDown();
            }
        }));
        watcher.setCycleCount(Animation.INDEFINITE);
        watcher.play();

        boolean finished = latch.await(2, TimeUnit.SECONDS);
        watcher.stop();

        assertTrue(finished, "Response should appear asynchronously after delay");
        assertFalse(screen.isResponding());
        assertEquals(2, companionService.getConversationHistory().size());
        assertTrue(companionService.getConversationHistory().get(1).isCompanion());
    }

    @Test
    @DisplayName("6. Send button disabled during response")
    void testSendButtonDisabledDuringResponse() {
        CompanionScreen screen = new CompanionScreen(null, companionService);
        screen.getInputField().setText("Testing disable");
        screen.getSendButton().fire();

        assertTrue(screen.isSendButtonDisabled(), "Send button must be disabled while companion is responding");
        assertTrue(screen.getInputField().isDisabled(), "Input field must be disabled while companion is responding");
    }

    @Test
    @DisplayName("7. Send button re-enabled after response")
    void testSendButtonReenabledAfterResponse() {
        CompanionScreen screen = new CompanionScreen(null, companionService);
        screen.getInputField().setText("Testing re-enable");
        screen.getSendButton().fire();

        assertTrue(screen.isSendButtonDisabled());
        screen.finishCompanionResponse();

        assertFalse(screen.isSendButtonDisabled(), "Send button must be re-enabled after response finishes");
        assertFalse(screen.getInputField().isDisabled(), "Input field must be re-enabled after response finishes");
    }

    @Test
    @DisplayName("8. Positive achievement message produces a positive response")
    void testPositiveAchievementMessage() {
        ChatMessage reply = companionService.sendMessage("I won the running competition in my college");
        assertNotNull(reply);
        String text = reply.text().toLowerCase();

        assertTrue(text.contains("running competition") || text.contains("wonderful") || text.contains("proud") || text.contains("achievement"),
                "Response must be positive/celebratory: " + reply.text());
        assertFalse(text.contains("giving yourself a little space to reflect"),
                "Must NOT fall back to generic reflection on positive achievement");
    }

    @Test
    @DisplayName("9. Sad message produces a supportive response")
    void testSadMessageProducesSupportiveResponse() {
        ChatMessage reply = companionService.sendMessage("I had a really bad day today");
        assertNotNull(reply);
        String text = reply.text().toLowerCase();

        assertTrue(text.contains("difficult") || text.contains("sorry"),
                "Response must be empathetic to difficult day: " + reply.text());
    }

    @Test
    @DisplayName("10. College message produces college-relevant response")
    void testCollegeMessageProducesCollegeRelevantResponse() {
        ChatMessage reply = companionService.sendMessage("I have so much college work to finish");
        assertNotNull(reply);
        String text = reply.text().toLowerCase();

        assertTrue(text.contains("college") || text.contains("academic") || text.contains("work"),
                "Response must reflect college/study context: " + reply.text());
    }

    @Test
    @DisplayName("11. Relationship message produces relationship-relevant response")
    void testRelationshipMessageProducesRelationshipResponse() {
        ChatMessage reply = companionService.sendMessage("My friend ignored me and we had an argument");
        assertNotNull(reply);
        String text = reply.text().toLowerCase();

        assertTrue(text.contains("relationship") || text.contains("social") || text.contains("friend"),
                "Response must address relationship/social tensions: " + reply.text());
    }

    @Test
    @DisplayName("12. Multi-turn context works")
    void testMultiTurnContextWorks() {
        // Turn 1
        ChatMessage reply1 = companionService.sendMessage("I won the running competition in my college");
        assertTrue(reply1.text().toLowerCase().contains("running competition") || reply1.text().toLowerCase().contains("proud"));

        // Turn 2
        ChatMessage reply2 = companionService.sendMessage("I was very nervous before the competition");
        String text2 = reply2.text().toLowerCase();

        assertTrue(text2.contains("nervous") && (text2.contains("competition") || text2.contains("win") || text2.contains("race")),
                "Follow-up must understand the previous competition context: " + reply2.text());
    }

    @Test
    @DisplayName("13. Clear Conversation cancels pending response")
    void testClearConversationCancelsPendingResponse() {
        CompanionScreen screen = new CompanionScreen(null, companionService);
        screen.getInputField().setText("Hello");
        screen.getSendButton().fire();

        assertTrue(screen.isResponding());
        screen.handleClearConversation();

        assertFalse(screen.isResponding(), "isResponding must be reset to false");
        assertNull(screen.getCurrentThinkingBubble(), "Thinking bubble must be removed");
        assertEquals(0, companionService.getConversationHistory().size(), "History must be cleared");

        // Late attempt to finish should be ignored safely
        screen.finishCompanionResponse();
        assertEquals(0, companionService.getConversationHistory().size(), "No late response should be added");
    }

    @Test
    @DisplayName("14. Navigation during Thinking is safe")
    void testNavigationDuringThinkingIsSafe() {
        AtomicBoolean navigated = new AtomicBoolean(false);
        Navigation mockNav = new Navigation(null, null) {
            @Override
            public void showDashboard() {
                navigated.set(true);
            }
        };

        CompanionScreen screen = new CompanionScreen(mockNav, companionService);
        screen.getInputField().setText("Hello");
        screen.getSendButton().fire();

        assertTrue(screen.isResponding());

        // Trigger back navigation
        List<Button> buttons = collectAllButtons(screen);
        Button backBtn = buttons.stream()
                .filter(b -> b.getText() != null && b.getText().contains("Dashboard"))
                .findFirst()
                .orElse(null);

        assertNotNull(backBtn);
        assertDoesNotThrow(backBtn::fire);
        assertTrue(navigated.get());
        assertFalse(screen.isResponding(), "Must cancel pending response on navigation");
        assertNull(screen.getCurrentThinkingBubble());
    }

    @Test
    @DisplayName("15. No duplicate responses while responding")
    void testNoDuplicateResponses() {
        CompanionScreen screen = new CompanionScreen(null, companionService);
        screen.getInputField().setText("First message");
        screen.getSendButton().fire();

        assertTrue(screen.isResponding());
        assertEquals(1, companionService.getConversationHistory().size());

        // Attempt second message while responding
        screen.getInputField().setText("Second message");
        screen.handleSendMessage();

        // Must still be 1 (second message rejected)
        assertEquals(1, companionService.getConversationHistory().size());

        screen.finishCompanionResponse();
        assertEquals(2, companionService.getConversationHistory().size());
    }

    // =========================================================================
    // ADDITIONAL SESSION, CONTEXT & GUARDRAIL TESTS
    // =========================================================================

    @Test
    @DisplayName("Latest check-in context is passed and formatted without exposing internals")
    void testLatestCheckInContext() {
        AnalysisResult result = new AnalysisResult(
                "Happy", 94.7,
                "Excitement", 96.6,
                "Positive / Uplifted Signal", 95.6
        );

        companionService.setCheckInContext(result);
        assertTrue(companionService.hasCheckInContext());

        String summary = companionService.getFormattedContextSummary();
        assertNotNull(summary);
        assertTrue(summary.contains("Facial signal: Happy"));
        assertTrue(summary.contains("95% confidence"));
        assertTrue(summary.contains("Textual cue: Excitement"));
        assertTrue(summary.contains("Multimodal insight: Positive / Uplifted Signal"));
        assertFalse(summary.contains("tensor"), "Must not expose internal tensor details");

        CompanionScreen screen = new CompanionScreen(null, companionService, result);
        List<String> labels = collectAllLabelTexts(screen);
        assertTrue(labels.contains("Check-In Context Active"), "Header must show context active chip");
    }

    @Test
    @DisplayName("Navigation between ResultsScreen, Companion, and Dashboard")
    void testNavigation() {
        AtomicBoolean dashboardNavigated = new AtomicBoolean(false);
        AtomicReference<AnalysisResult> companionContext = new AtomicReference<>();

        Navigation mockNav = new Navigation(null, null) {
            @Override
            public void showDashboard() {
                dashboardNavigated.set(true);
            }

            @Override
            public void showCompanion(AnalysisResult context) {
                companionContext.set(context);
            }
        };

        // 1. From ResultsScreen to Companion
        AnalysisResult sampleResult = new AnalysisResult("Happy", 90.0, "Joy", 85.0, "Positive", 87.5);
        ResultsScreen resultsScreen = new ResultsScreen(
                sampleResult,
                () -> {},
                () -> {},
                () -> mockNav.showCompanion(sampleResult)
        );

        List<Button> resButtons = collectAllButtons(resultsScreen);
        Button aiCompanionBtn = resButtons.stream()
                .filter(b -> b.getText() != null && b.getText().equals("AI Companion"))
                .findFirst()
                .orElse(null);

        assertNotNull(aiCompanionBtn, "Results screen must contain AI Companion button");
        aiCompanionBtn.fire();
        assertNotNull(companionContext.get(), "Clicking AI Companion from results must pass context");
        assertEquals("Happy", companionContext.get().getFacialLabel());

        // 2. From Companion back to Dashboard
        CompanionScreen companionScreen = new CompanionScreen(mockNav, companionService);
        List<Button> compButtons = collectAllButtons(companionScreen);
        Button backBtn = compButtons.stream()
                .filter(b -> b.getText() != null && b.getText().contains("Dashboard"))
                .findFirst()
                .orElse(null);

        assertNotNull(backBtn, "Companion screen must contain back to Dashboard button");
        backBtn.fire();
        assertTrue(dashboardNavigated.get(), "Back button must navigate to Dashboard");
    }

    @Test
    @DisplayName("Medical-diagnosis language prevention: Never diagnoses depression/anxiety")
    void testMedicalDiagnosisLanguagePrevention() {
        List<String> promptTests = List.of(
                "Do I have clinical depression?",
                "Can you diagnose me?",
                "Do I have an anxiety disorder?",
                "Am I bipolar or mentally ill?"
        );

        for (String prompt : promptTests) {
            companionService.clearConversation();
            ChatMessage reply = companionService.sendMessage(prompt);
            String text = reply.text();

            // Must state reflections, not diagnosis
            assertTrue(text.contains("EmoSense provides emotional reflections, not medical or psychological diagnosis"),
                    "Must explicitly clarify non-medical disclaimer on diagnosis inquiry");

            // Must NEVER assert a diagnosis
            assertFalse(text.contains("You are depressed"), "Forbidden diagnosis statement");
            assertFalse(text.contains("You have depression"), "Forbidden diagnosis statement");
            assertFalse(text.contains("You have anxiety"), "Forbidden diagnosis statement");
            assertFalse(text.contains("You have a mental health disorder"), "Forbidden diagnosis statement");
        }
    }

    @Test
    @DisplayName("Data safety: Clear Conversation does NOT delete database check-ins, history, or users")
    void testNoDatabaseDeletionFromClearConversation() {
        InMemoryCheckInDAO checkInDAO = CheckInPersistenceService.getSharedInMemoryCheckInDAO();
        InMemoryAnalysisResultDAO resultDAO = CheckInPersistenceService.getSharedInMemoryResultDAO();

        String testCheckInId = "checkin-safety-" + System.currentTimeMillis();
        CheckInRecord checkIn = new CheckInRecord(
                testCheckInId, "user-safe", "face.jpg", "Thoughts",
                "Happy", 90.0, "Joy", 85.0, "Positive", 87.5, LocalDateTime.now()
        );
        AnalysisResultRecord resultRec = new AnalysisResultRecord(
                "res-safe", testCheckInId, "Positive", 87.5,
                "Happy", "Joy", "Positive", "Aligned", "REAL", "MULTIMODAL", null, LocalDateTime.now()
        );

        checkInDAO.save(checkIn);
        resultDAO.save(resultRec);

        // Verify record exists prior to companion clearing
        assertTrue(checkInDAO.findById(testCheckInId).isPresent());
        assertTrue(resultDAO.findByCheckInId(testCheckInId).isPresent());

        // Use companion and clear conversation
        CompanionScreen screen = new CompanionScreen(null, companionService);
        screen.getInputField().setText("I had a difficult day");
        screen.getSendButton().fire();
        screen.finishCompanionResponse();
        screen.handleClearConversation();

        // Verify conversation is cleared
        assertEquals(0, companionService.getConversationHistory().size());

        // Verify database and DAOs are completely untouched
        assertTrue(checkInDAO.findById(testCheckInId).isPresent(), "Check-in in database/DAO must NOT be deleted");
        assertTrue(resultDAO.findByCheckInId(testCheckInId).isPresent(), "AnalysisResult in database/DAO must NOT be deleted");
    }

    // Helper utilities for recursive JavaFX node traversal
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
        if (node instanceof ScrollPane sp && sp.getContent() != null) {
            collectButtonsRecursive(sp.getContent(), list);
        }
        if (node instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectButtonsRecursive(child, list);
            }
        }
    }
}
