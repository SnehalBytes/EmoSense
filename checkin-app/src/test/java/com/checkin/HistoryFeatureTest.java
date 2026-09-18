package com.checkin;

import com.checkin.dao.AnalysisResultDAO;
import com.checkin.dao.CheckInDAO;
import com.checkin.dao.InMemoryAnalysisResultDAO;
import com.checkin.dao.InMemoryCheckInDAO;
import com.checkin.model.AnalysisResult;
import com.checkin.model.AnalysisResultRecord;
import com.checkin.model.CheckInRecord;
import com.checkin.model.User;
import com.checkin.screens.HistoryScreen;
import com.checkin.services.CheckInPersistenceService;
import com.checkin.services.HistoryService;
import com.checkin.services.UserSession;
import com.checkin.utils.Navigation;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class HistoryFeatureTest {

    private InMemoryCheckInDAO checkInDAO;
    private InMemoryAnalysisResultDAO resultDAO;
    private HistoryService historyService;

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
        checkInDAO = new InMemoryCheckInDAO();
        resultDAO = new InMemoryAnalysisResultDAO();
        historyService = new HistoryService(checkInDAO, resultDAO);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    @Test
    @DisplayName("1. Save check-in with complete emotional signals and verify persistence")
    void testSaveCheckIn() {
        String userId = "user-test-01";
        LocalDateTime now = LocalDateTime.now();
        String checkInId = UUID.randomUUID().toString();

        CheckInRecord checkIn = new CheckInRecord(
                checkInId,
                userId,
                "selfie.jpg",
                "I felt excited and energized today!",
                "Happy",
                88.5,
                "Joy",
                82.0,
                "Positive / Uplifted Signal",
                85.25,
                now
        );

        AnalysisResultRecord result = new AnalysisResultRecord(
                UUID.randomUUID().toString(),
                checkInId,
                "Positive / Uplifted Signal",
                85.25,
                "Happy",
                "Joy",
                "Positive / Uplifted Signal",
                "Aligned Signals",
                "REAL",
                "MULTIMODAL",
                "FACIAL=Happy:0.885,Neutral:0.05|TEXT=joy:0.82,excitement:0.75",
                now
        );

        assertTrue(checkInDAO.save(checkIn));
        assertTrue(resultDAO.save(result));

        Optional<CheckInRecord> foundCheckIn = checkInDAO.findById(checkInId);
        assertTrue(foundCheckIn.isPresent());
        assertEquals("Happy", foundCheckIn.get().facialLabel());
        assertEquals(88.5, foundCheckIn.get().facialConfidence());
        assertEquals("Joy", foundCheckIn.get().textLabel());
        assertEquals("Positive / Uplifted Signal", foundCheckIn.get().combinedLabel());

        Optional<AnalysisResultRecord> foundResult = resultDAO.findByCheckInId(checkInId);
        assertTrue(foundResult.isPresent());
        assertEquals("REAL", foundResult.get().modelSource());
    }

    @Test
    @DisplayName("2. Retrieve current user's history")
    void testRetrieveCurrentUsersHistory() {
        User user = new User("user-002", "Jane Doe", "jane@example.com", "hash");
        UserSession.getInstance().login(user);

        CheckInRecord record = new CheckInRecord(
                "ci-101",
                user.getId(),
                "face.jpg",
                "Peaceful thoughts",
                "Neutral",
                74.0,
                "Calm",
                70.0,
                "Calm Signal",
                72.0,
                LocalDateTime.now()
        );
        checkInDAO.save(record);

        List<CheckInRecord> history = historyService.getUserHistory(user.getId());
        assertEquals(1, history.size());
        assertEquals("ci-101", history.get(0).id());
        assertEquals("Neutral", history.get(0).facialLabel());
    }

    @Test
    @DisplayName("3. User isolation: Users cannot view other users' check-ins")
    void testUserIsolation() {
        User userA = new User("user-A", "Alice", "alice@example.com", "hashA");
        User userB = new User("user-B", "Bob", "bob@example.com", "hashB");

        CheckInRecord recordA = new CheckInRecord(
                "ci-A", userA.getId(), "faceA.jpg", "Alice thoughts",
                "Happy", 80.0, "Joy", 75.0, "Positive", 78.0, LocalDateTime.now()
        );
        CheckInRecord recordB = new CheckInRecord(
                "ci-B", userB.getId(), "faceB.jpg", "Bob thoughts",
                "Sad", 65.0, "Grief", 60.0, "Somber", 62.0, LocalDateTime.now()
        );
        checkInDAO.save(recordA);
        checkInDAO.save(recordB);

        // Login as Alice
        UserSession.getInstance().login(userA);
        List<CheckInRecord> aliceHistory = historyService.getUserHistory(userA.getId());
        assertEquals(1, aliceHistory.size());
        assertEquals("ci-A", aliceHistory.get(0).id());

        // Alice cannot access Bob's history
        List<CheckInRecord> breachAttempt = historyService.getUserHistory(userB.getId());
        assertTrue(breachAttempt.isEmpty(), "History request for different user ID must return empty list");

        // Login as Bob
        UserSession.getInstance().login(userB);
        List<CheckInRecord> bobHistory = historyService.getUserHistory(userB.getId());
        assertEquals(1, bobHistory.size());
        assertEquals("ci-B", bobHistory.get(0).id());
    }

    @Test
    @DisplayName("4. Empty history display: Renders 'No check-ins yet' and 'Start a Check-In' button")
    void testEmptyHistory() {
        User user = new User("user-empty", "Empty User", "empty@example.com", "hash");
        UserSession.getInstance().login(user);

        AtomicBoolean startCheckInClicked = new AtomicBoolean(false);
        Navigation mockNav = new Navigation(null, null) {
            @Override
            public void showCheckIn() {
                startCheckInClicked.set(true);
            }
        };

        HistoryScreen screen = new HistoryScreen(mockNav, historyService);
        List<String> labels = collectAllLabelTexts(screen);

        assertTrue(labels.contains("No check-ins yet"), "Must display 'No check-ins yet'");
        assertTrue(labels.contains("Your completed check-ins will appear here."), "Must display empty description");

        List<Button> buttons = collectAllButtons(screen);
        Button startBtn = buttons.stream()
                .filter(b -> b.getText() != null && b.getText().contains("Start a Check-In"))
                .findFirst()
                .orElse(null);

        assertNotNull(startBtn, "Must contain 'Start a Check-In' button in empty state");
        startBtn.fire();
        assertTrue(startCheckInClicked.get(), "Clicking Start a Check-In must navigate to check-in screen");
    }

    @Test
    @DisplayName("5. History ordering: Records sorted newest first (descending timestamp)")
    void testHistoryOrderingByNewestFirst() {
        User user = new User("user-order", "Ordering User", "order@example.com", "hash");
        UserSession.getInstance().login(user);

        LocalDateTime t1 = LocalDateTime.of(2026, 9, 10, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 9, 12, 14, 30);
        LocalDateTime t3 = LocalDateTime.of(2026, 9, 15, 18, 45);

        checkInDAO.save(new CheckInRecord("id-old", user.getId(), null, "old", null, 0, "Joy", 70, "Joy", 70, t1));
        checkInDAO.save(new CheckInRecord("id-newest", user.getId(), null, "newest", null, 0, "Joy", 90, "Joy", 90, t3));
        checkInDAO.save(new CheckInRecord("id-mid", user.getId(), null, "mid", null, 0, "Joy", 80, "Joy", 80, t2));

        List<CheckInRecord> ordered = historyService.getUserHistory(user.getId());
        assertEquals(3, ordered.size());
        assertEquals("id-newest", ordered.get(0).id(), "Index 0 must be newest");
        assertEquals("id-mid", ordered.get(1).id(), "Index 1 must be middle");
        assertEquals("id-old", ordered.get(2).id(), "Index 2 must be oldest");
    }

    @Test
    @DisplayName("6. Stored result retrieval without running inference")
    void testStoredResultRetrieval() {
        String checkInId = "checkin-stored-01";
        LocalDateTime now = LocalDateTime.now();

        AnalysisResultRecord record = new AnalysisResultRecord(
                "res-01",
                checkInId,
                "Positive / Uplifted Signal",
                89.4,
                "Happy",
                "Joy",
                "Positive / Uplifted Signal",
                "Aligned Signals",
                "REAL",
                "MULTIMODAL",
                "FACIAL=Happy:0.894,Neutral:0.05,Surprise:0.02|TEXT=joy:0.85,optimism:0.75",
                now
        );
        resultDAO.save(record);

        Optional<AnalysisResult> resultOpt = historyService.getStoredResult(checkInId);
        assertTrue(resultOpt.isPresent());

        AnalysisResult result = resultOpt.get();
        assertEquals("Happy", result.getFacialLabel());
        assertEquals("Joy", result.getTextLabel());
        assertEquals("Positive / Uplifted Signal", result.getCombinedLabel());
        assertEquals(89.4, result.getCombinedConfidence());
        assertTrue(result.isFacialRealModel(), "Must restore REAL model status from stored record");
        assertEquals(0.894, result.getFacialProbabilities().get("Happy"));
    }

    @Test
    @DisplayName("7. View Result navigation displays stored result")
    void testViewResultNavigation() {
        User user = new User("user-nav", "Nav User", "nav@example.com", "hash");
        UserSession.getInstance().login(user);

        String checkInId = "ci-nav-01";
        CheckInRecord checkIn = new CheckInRecord(
                checkInId, user.getId(), "face.jpg", "Thoughts",
                "Happy", 85.0, "Joy", 78.0, "Positive Signal", 81.5, LocalDateTime.now()
        );
        checkInDAO.save(checkIn);

        AnalysisResultRecord resultRec = new AnalysisResultRecord(
                "res-nav-01", checkInId, "Positive Signal", 81.5,
                "Happy", "Joy", "Positive Signal", "Aligned", "REAL",
                "MULTIMODAL", "FACIAL=Happy:0.85|TEXT=joy:0.78", LocalDateTime.now()
        );
        resultDAO.save(resultRec);

        AtomicReference<AnalysisResult> displayedResult = new AtomicReference<>();
        Navigation mockNav = new Navigation(null, null) {
            @Override
            public void showResults(AnalysisResult result) {
                displayedResult.set(result);
            }
        };

        HistoryScreen screen = new HistoryScreen(mockNav, historyService);
        List<Button> buttons = collectAllButtons(screen);
        Button viewResultBtn = buttons.stream()
                .filter(b -> b.getText() != null && b.getText().equals("View Result"))
                .findFirst()
                .orElse(null);

        assertNotNull(viewResultBtn, "History card must contain 'View Result' button");
        viewResultBtn.fire();

        assertNotNull(displayedResult.get(), "Clicking View Result must navigate to results with stored result");
        assertEquals("Happy", displayedResult.get().getFacialLabel());
        assertEquals("Joy", displayedResult.get().getTextLabel());
        assertEquals("Positive Signal", displayedResult.get().getCombinedLabel());
    }

    @Test
    @DisplayName("8. Database failure handling renders 'Unable to load check-in history.'")
    void testDatabaseFailureHandling() {
        User user = new User("user-err", "Err User", "err@example.com", "hash");
        UserSession.getInstance().login(user);

        // Faulty DAO that always throws on query
        CheckInDAO failingDAO = new CheckInDAO() {
            @Override
            public boolean save(CheckInRecord record) { return false; }

            @Override
            public List<CheckInRecord> findByUserId(String userId) {
                throw new RuntimeException("Simulated connection timeout to MySQL");
            }

            @Override
            public Optional<CheckInRecord> findById(String id) { return Optional.empty(); }

            @Override
            public int countByUserId(String userId) { return 0; }
        };

        HistoryService failingService = new HistoryService(failingDAO, resultDAO);
        HistoryScreen screen = new HistoryScreen(null, failingService);

        List<String> labels = collectAllLabelTexts(screen);
        assertTrue(labels.contains("Unable to load check-in history."),
                "Screen must display 'Unable to load check-in history.' on database failure");

        List<Button> buttons = collectAllButtons(screen);
        assertTrue(buttons.stream().anyMatch(b -> b.getText() != null && b.getText().equals("Retry")),
                "Error state should offer a Retry action");
    }

    @Test
    @DisplayName("9. New Check-In navigation from History header")
    void testNewCheckInNavigation() {
        User user = new User("user-head", "Header User", "head@example.com", "hash");
        UserSession.getInstance().login(user);

        AtomicBoolean newCheckInClicked = new AtomicBoolean(false);
        Navigation mockNav = new Navigation(null, null) {
            @Override
            public void showCheckIn() {
                newCheckInClicked.set(true);
            }
        };

        HistoryScreen screen = new HistoryScreen(mockNav, historyService);
        List<Button> buttons = collectAllButtons(screen);
        Button newCheckInBtn = buttons.stream()
                .filter(b -> b.getText() != null && b.getText().equals("New Check-In"))
                .findFirst()
                .orElse(null);

        assertNotNull(newCheckInBtn, "History header must contain 'New Check-In' button");
        newCheckInBtn.fire();
        assertTrue(newCheckInClicked.get(), "Clicking New Check-In must navigate to check-in screen");
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
