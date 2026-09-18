package com.checkin;

import com.checkin.dao.InMemoryAnalysisResultDAO;
import com.checkin.dao.InMemoryCheckInDAO;
import com.checkin.model.AnalysisResult;
import com.checkin.model.AnalysisResultRecord;
import com.checkin.model.CheckInRecord;
import com.checkin.model.User;
import com.checkin.screens.DashboardScreen;
import com.checkin.screens.InsightsScreen;
import com.checkin.services.CheckInPersistenceService;
import com.checkin.services.HistoryService;
import com.checkin.services.InsightsService;
import com.checkin.services.UserSession;
import com.checkin.utils.Navigation;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class DashboardAndInsightsFeatureTest {

    private InMemoryCheckInDAO checkInDAO;
    private InMemoryAnalysisResultDAO resultDAO;
    private HistoryService historyService;
    private InsightsService insightsService;

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX already initialized
        }
    }

    @BeforeEach
    void setUp() {
        UserSession.getInstance().logout();
        checkInDAO = new InMemoryCheckInDAO();
        resultDAO = new InMemoryAnalysisResultDAO();
        historyService = new HistoryService(checkInDAO, resultDAO);
        insightsService = new InsightsService(historyService);
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    // =========================================================================
    // DASHBOARD SCREEN TESTS
    // =========================================================================

    @Test
    @DisplayName("1. Empty dashboard state: Displays 'No check-ins yet' and no fake statistics")
    void testEmptyDashboardState() {
        User user = new User("user-empty", "Alex Morgan", "empty@emosense.test", "password123");
        UserSession.getInstance().login(user);

        DashboardScreen screen = new DashboardScreen(null, historyService, insightsService);
        List<String> labels = collectAllLabelTexts(screen);

        assertTrue(labels.contains("Welcome back, Alex Morgan"));
        assertTrue(labels.contains("Understand your emotional signals through reflection."));
        assertTrue(labels.contains("Start a New Check-In"));
        assertTrue(labels.contains("Share a photo, your thoughts, or both."));
        assertTrue(labels.contains("No check-ins yet"), "Must show No check-ins yet when user has 0 records");
        assertTrue(labels.contains("Complete your first check-in to start discovering your emotional signals."));
        assertTrue(labels.contains("Start Your First Check-In"));

        // Verify NO fabricated statistics or fake confidence scores
        assertFalse(labels.stream().anyMatch(l -> l.contains("75% confidence") || l.contains("0% score")),
                "Must never fabricate confidence percentages on empty dashboard");
    }

    @Test
    @DisplayName("2. Latest check-in card displays actual stored database values")
    void testLatestCheckInDisplaysRealData() {
        User user = new User("user-1", "Jordan Lee", "user1@emosense.test", "password123");
        UserSession.getInstance().login(user);

        LocalDateTime time1 = LocalDateTime.now().minusHours(2);
        LocalDateTime time2 = LocalDateTime.now().minusMinutes(15);

        // Older check-in
        CheckInRecord r1 = new CheckInRecord("chk-1", "user-1", "old.jpg", "Old thoughts",
                "Sad", 82.0, "Sadness", 84.0, "Sad / Reflective", 83.0, time1);
        // Latest check-in
        CheckInRecord r2 = new CheckInRecord("chk-2", "user-1", "happy.jpg", "Great day today!",
                "Happy", 95.0, "Joy", 92.0, "Positive / Uplifted Signal", 93.5, time2);

        checkInDAO.save(r1);
        checkInDAO.save(r2);

        DashboardScreen screen = new DashboardScreen(null, historyService, insightsService);
        List<String> labels = collectAllLabelTexts(screen);

        assertTrue(labels.contains("LATEST CHECK-IN"), "Must display LATEST CHECK-IN label");
        assertTrue(labels.contains("REAL MODEL"), "Must display REAL MODEL badge");
        assertTrue(labels.contains("MULTIMODAL"), "Must display MULTIMODAL badge");
        assertTrue(labels.stream().anyMatch(l -> l.contains("Positive / Uplifted Signal")),
                "Must display latest multimodal insight");
        assertTrue(labels.stream().anyMatch(l -> l.contains("Happy (95% confidence)")),
                "Must display latest facial expression with confidence");
        assertTrue(labels.stream().anyMatch(l -> l.contains("Joy (92% confidence)")),
                "Must display latest textual cue with confidence");
        assertTrue(labels.stream().anyMatch(l -> l.contains("Great day today!")),
                "Must display latest thoughts excerpt snippet");

        // Old check-in data must not be displayed in the latest card
        assertFalse(labels.stream().anyMatch(l -> l.contains("Sad / Reflective")));
    }

    @Test
    @DisplayName("3. Latest result retrieval: View Result button retrieves stored result without inference")
    void testViewResultFromDashboard() {
        User user = new User("user-view", "Sam Rivera", "view@emosense.test", "password123");
        UserSession.getInstance().login(user);

        CheckInRecord record = new CheckInRecord("chk-view-1", "user-view", "face.jpg", "Feeling happy",
                "Happy", 94.0, "Joy", 90.0, "Positive", 92.0, LocalDateTime.now());
        AnalysisResultRecord resultRecord = new AnalysisResultRecord("res-view-1", "chk-view-1", "Positive", 92.0,
                "Happy", "Joy", "Positive", "Aligned", "REAL", "MULTIMODAL", null, LocalDateTime.now());

        checkInDAO.save(record);
        resultDAO.save(resultRecord);

        AtomicReference<AnalysisResult> resultPassed = new AtomicReference<>();
        Navigation mockNav = new Navigation(null, null) {
            @Override
            public void showResults(AnalysisResult result) {
                resultPassed.set(result);
            }
        };

        DashboardScreen screen = new DashboardScreen(mockNav, historyService, insightsService);
        List<Button> buttons = collectAllButtons(screen);

        Button viewResultBtn = buttons.stream()
                .filter(b -> b.getText() != null && b.getText().equals("View Result"))
                .findFirst()
                .orElse(null);

        assertNotNull(viewResultBtn, "Dashboard must have a View Result button on latest check-in card");
        viewResultBtn.fire();

        assertNotNull(resultPassed.get(), "Clicking View Result must pass stored AnalysisResult to showResults");
        assertEquals("Happy", resultPassed.get().getFacialLabel());
        assertEquals("Joy", resultPassed.get().getTextLabel());
        assertEquals("Positive", resultPassed.get().getCombinedLabel());
    }

    @Test
    @DisplayName("4. Navigation from Dashboard: Start Check-In, History, AI Companion, Insights")
    void testDashboardNavigation() {
        User user = new User("user-nav", "Taylor", "nav@emosense.test", "password123");
        UserSession.getInstance().login(user);

        AtomicBoolean checkInNav = new AtomicBoolean(false);
        AtomicBoolean historyNav = new AtomicBoolean(false);
        AtomicBoolean companionNav = new AtomicBoolean(false);
        AtomicBoolean insightsNav = new AtomicBoolean(false);

        Navigation mockNav = new Navigation(null, null) {
            @Override
            public void showCheckIn() {
                checkInNav.set(true);
            }
            @Override
            public void showHistory() {
                historyNav.set(true);
            }
            @Override
            public void showCompanion() {
                companionNav.set(true);
            }
            @Override
            public void showInsights() {
                insightsNav.set(true);
            }
        };

        DashboardScreen screen = new DashboardScreen(mockNav, historyService, insightsService);
        List<Button> buttons = collectAllButtons(screen);

        // 1. Primary Action: Start Check-In
        Button primaryBtn = buttons.stream()
                .filter(b -> b.getText() != null && b.getText().contains("Start Check-In"))
                .findFirst()
                .orElse(null);
        assertNotNull(primaryBtn);
        primaryBtn.fire();
        assertTrue(checkInNav.get(), "Primary button must navigate to Check-In screen");

        // 2. Quick Action: HISTORY
        Button historyBtn = buttons.stream()
                .filter(b -> b.getParent() != null && collectAllLabelTexts(b.getParent()).contains("HISTORY"))
                .findFirst()
                .orElse(null);
        assertNotNull(historyBtn);
        historyBtn.fire();
        assertTrue(historyNav.get(), "History card must navigate to History screen");

        // 3. Quick Action: AI COMPANION
        Button companionBtn = buttons.stream()
                .filter(b -> b.getParent() != null && collectAllLabelTexts(b.getParent()).contains("AI COMPANION"))
                .findFirst()
                .orElse(null);
        assertNotNull(companionBtn);
        companionBtn.fire();
        assertTrue(companionNav.get(), "AI Companion card must navigate to Companion screen");

        // 4. Quick Action: INSIGHTS
        Button insightsBtn = buttons.stream()
                .filter(b -> b.getParent() != null && collectAllLabelTexts(b.getParent()).contains("INSIGHTS"))
                .findFirst()
                .orElse(null);
        assertNotNull(insightsBtn);
        insightsBtn.fire();
        assertTrue(insightsNav.get(), "Insights card must navigate to Insights screen");
    }

    @Test
    @DisplayName("5. User isolation: User A never sees User B's dashboard records or latest check-in")
    void testUserIsolationOnDashboard() {
        // User B check-in
        CheckInRecord userBRecord = new CheckInRecord("chk-user-b", "user-b", "userB.jpg", "User B Secret Thoughts",
                "Fear", 99.0, "Nervousness", 98.0, "Fearful Signal", 98.5, LocalDateTime.now());
        checkInDAO.save(userBRecord);

        // Sign in as User A
        User userA = new User("user-a", "User A", "userA@emosense.test", "password123");
        UserSession.getInstance().login(userA);

        DashboardScreen screen = new DashboardScreen(null, historyService, insightsService);
        List<String> labels = collectAllLabelTexts(screen);

        // User A must see empty state, NOT User B's records
        assertTrue(labels.contains("No check-ins yet"), "User A should see No check-ins yet");
        assertFalse(labels.stream().anyMatch(l -> l.contains("User B Secret Thoughts")),
                "User A must never see User B's thoughts");
        assertFalse(labels.stream().anyMatch(l -> l.contains("Fearful Signal")),
                "User A must never see User B's emotional signals");
    }

    @Test
    @DisplayName("6. Refresh behavior: loadDashboardData reflects newly saved check-ins without restart")
    void testDashboardRefreshBehavior() {
        User user = new User("user-refresh", "Casey", "refresh@emosense.test", "password123");
        UserSession.getInstance().login(user);

        DashboardScreen screen = new DashboardScreen(null, historyService, insightsService);
        assertTrue(collectAllLabelTexts(screen).contains("No check-ins yet"));

        // Complete a new check-in
        CheckInRecord newRecord = new CheckInRecord("chk-new-1", "user-refresh", "smile.jpg", "Feeling accomplished!",
                "Happy", 96.0, "Pride", 91.0, "Positive / Accomplished", 93.5, LocalDateTime.now());
        checkInDAO.save(newRecord);

        // Refresh dashboard
        screen.loadDashboardData();

        List<String> labels = collectAllLabelTexts(screen);
        assertFalse(labels.contains("No check-ins yet"));
        assertTrue(labels.contains("LATEST CHECK-IN"));
        assertTrue(labels.stream().anyMatch(l -> l.contains("Feeling accomplished!")));
    }

    @Test
    @DisplayName("7. Database failure on Dashboard: Displays friendly error message with Retry button")
    void testDatabaseFailureOnDashboard() {
        User user = new User("user-err", "Error Test", "err@emosense.test", "password123");
        UserSession.getInstance().login(user);

        HistoryService failingHistoryService = new HistoryService(null, null) {
            @Override
            public List<CheckInRecord> getUserHistory(String userId) {
                throw new HistoryAccessException("Simulated database timeout", null);
            }
        };

        DashboardScreen screen = new DashboardScreen(null, failingHistoryService, insightsService);
        List<String> labels = collectAllLabelTexts(screen);

        assertTrue(labels.contains("Unable to load your latest check-in."));
        assertTrue(labels.contains("A database error occurred while accessing your stored check-ins."));

        List<Button> buttons = collectAllButtons(screen);
        assertTrue(buttons.stream().anyMatch(b -> b.getText() != null && b.getText().equals("Retry")));
    }

    // =========================================================================
    // INSIGHTS SCREEN & SERVICE TESTS
    // =========================================================================

    @Test
    @DisplayName("8. Empty insights state when user has 0 check-ins")
    void testEmptyInsightsState() {
        User user = new User("user-no-insights", "Robin", "noins@emosense.test", "password123");
        UserSession.getInstance().login(user);

        InsightsScreen screen = new InsightsScreen(null, insightsService, historyService);
        List<String> labels = collectAllLabelTexts(screen);

        assertTrue(labels.contains("No check-ins yet"));
        assertTrue(labels.contains("Complete your first check-in to start discovering your emotional patterns."));
        assertTrue(labels.contains("Start Your First Check-In"));
    }

    @Test
    @DisplayName("9. Insufficient-data state when user has 1-2 check-ins: Shows notice and no fake trends")
    void testInsufficientDataInsightsState() {
        User user = new User("user-insufficient", "Morgan", "insuf@emosense.test", "password123");
        UserSession.getInstance().login(user);

        // Save 2 check-ins (less than 3)
        checkInDAO.save(new CheckInRecord("chk-sub-1", "user-insufficient", "f1.jpg", "Thoughts 1",
                "Happy", 90.0, "Joy", 85.0, "Positive", 87.5, LocalDateTime.now().minusDays(1)));
        checkInDAO.save(new CheckInRecord("chk-sub-2", "user-insufficient", "f2.jpg", "Thoughts 2",
                "Surprise", 80.0, "Excitement", 82.0, "Uplifted", 81.0, LocalDateTime.now()));

        InsightsScreen screen = new InsightsScreen(null, insightsService, historyService);
        List<String> labels = collectAllLabelTexts(screen);

        assertTrue(labels.contains("CHECK-IN ACTIVITY"));
        assertTrue(labels.contains("More check-ins will help build your personal insights."),
                "Must display threshold notice when fewer than 3 check-ins exist");
        assertTrue(labels.stream().anyMatch(l -> l.contains("2 completed check-ins recorded")),
                "Must inform user of their current record count");

        // Full distribution charts should NOT be shown yet to avoid fabricating premature trends
        assertFalse(labels.contains("Facial Signal Distribution"));
        assertFalse(labels.contains("Textual Signal Distribution"));

        // However, recent signals list MUST still display actual recorded check-ins
        assertTrue(labels.contains("Recent Signals"));
    }

    @Test
    @DisplayName("10. Full Insights calculations: Activity, Facial Distribution, Text Distribution, and Alignment")
    void testFullInsightsCalculations() {
        User user = new User("user-full", "Dana", "full@emosense.test", "password123");
        UserSession.getInstance().login(user);

        // 4 check-ins (exceeds threshold of 3)
        // 1. Happy + Joy (Multimodal Aligned)
        checkInDAO.save(new CheckInRecord("c1", "user-full", "f1.jpg", "t1",
                "Happy", 92.0, "Joy", 90.0, "Positive", 91.0, LocalDateTime.now().minusDays(3)));
        // 2. Happy + Excitement (Multimodal Aligned)
        checkInDAO.save(new CheckInRecord("c2", "user-full", "f2.jpg", "t2",
                "Happy", 94.0, "Excitement", 88.0, "Positive", 91.0, LocalDateTime.now().minusDays(2)));
        // 3. Sad + Grief (Multimodal Aligned)
        checkInDAO.save(new CheckInRecord("c3", "user-full", "f3.jpg", "t3",
                "Sad", 86.0, "Grief", 84.0, "Sad", 85.0, LocalDateTime.now().minusDays(1)));
        // 4. Photo-only Surprise
        checkInDAO.save(new CheckInRecord("c4", "user-full", "f4.jpg", null,
                "Surprise", 88.0, null, 0.0, null, 0.0, LocalDateTime.now()));

        InsightsService.UserInsights insights = insightsService.computeUserInsights("user-full");

        assertEquals(4, insights.totalCheckIns());
        assertEquals(1, insights.photoCount());
        assertEquals(0, insights.textCount());
        assertEquals(3, insights.multimodalCount());
        assertTrue(insights.hasSufficientData());

        // Facial distribution across FER2013 classes
        assertEquals(2, insights.facialDistribution().get("Happy"));
        assertEquals(1, insights.facialDistribution().get("Sad"));
        assertEquals(1, insights.facialDistribution().get("Surprise"));
        assertEquals(0, insights.facialDistribution().get("Angry"));
        assertEquals(0, insights.facialDistribution().get("Neutral"));
        assertEquals("Happy", insights.mostFrequentFacial());

        // Textual distribution
        assertEquals(1, insights.textDistribution().get("Joy"));
        assertEquals(1, insights.textDistribution().get("Excitement"));
        assertEquals(1, insights.textDistribution().get("Grief"));

        // Alignment: 3 multimodal check-ins, all 3 aligned
        assertEquals(3, insights.multimodalAlignedCount());
        assertEquals(0, insights.multimodalDifferCount());
        assertEquals(100.0, insights.alignmentRate());

        // Test UI Screen with this data
        InsightsScreen screen = new InsightsScreen(null, insightsService, historyService);
        List<String> labels = collectAllLabelTexts(screen);

        assertTrue(labels.contains("CHECK-IN ACTIVITY"));
        assertTrue(labels.contains("4"));
        assertTrue(labels.contains("Facial Signal Distribution"));
        assertTrue(labels.contains("Textual Signal Distribution"));
        assertTrue(labels.contains("Facial vs Textual Modality Alignment"));
        assertTrue(labels.stream().anyMatch(l -> l.contains("100%")));
    }

    @Test
    @DisplayName("11. Native JavaFX BarChart uses actual stored facial distribution values")
    void testBarChartDataBinding() {
        User user = new User("user-chart", "Pat", "chart@emosense.test", "password123");
        UserSession.getInstance().login(user);

        // Add 3 check-ins with known facial labels
        checkInDAO.save(new CheckInRecord("c1", "user-chart", "f1.jpg", null, "Happy", 90.0, null, 0.0, null, 0.0, LocalDateTime.now().minusDays(2)));
        checkInDAO.save(new CheckInRecord("c2", "user-chart", "f2.jpg", null, "Happy", 92.0, null, 0.0, null, 0.0, LocalDateTime.now().minusDays(1)));
        checkInDAO.save(new CheckInRecord("c3", "user-chart", "f3.jpg", null, "Surprise", 85.0, null, 0.0, null, 0.0, LocalDateTime.now()));

        InsightsScreen screen = new InsightsScreen(null, insightsService, historyService);

        BarChart<String, Number> chart = findBarChart(screen);
        assertNotNull(chart, "Insights screen must contain a native JavaFX BarChart for facial distribution");

        assertFalse(chart.getData().isEmpty(), "BarChart must have a data series");
        XYChart.Series<String, Number> series = chart.getData().get(0);

        int happyCount = 0;
        int surpriseCount = 0;
        int angryCount = 0;

        for (XYChart.Data<String, Number> item : series.getData()) {
            if ("Happy".equals(item.getXValue())) happyCount = item.getYValue().intValue();
            if ("Surprise".equals(item.getXValue())) surpriseCount = item.getYValue().intValue();
            if ("Angry".equals(item.getXValue())) angryCount = item.getYValue().intValue();
        }

        assertEquals(2, happyCount, "BarChart Happy count must equal actual stored records");
        assertEquals(1, surpriseCount, "BarChart Surprise count must equal actual stored records");
        assertEquals(0, angryCount, "BarChart Angry count must be 0 (no fake numbers)");
    }

    @Test
    @DisplayName("12. Chronological newest-first ordering of recent signals in Insights")
    void testNewestFirstOrderingInInsights() {
        User user = new User("user-order", "Sam", "order@emosense.test", "password123");
        UserSession.getInstance().login(user);

        LocalDateTime now = LocalDateTime.now();
        checkInDAO.save(new CheckInRecord("c-old", "user-order", "old.jpg", null, "Sad", 80.0, null, 0.0, null, 0.0, now.minusDays(5)));
        checkInDAO.save(new CheckInRecord("c-mid", "user-order", "mid.jpg", null, "Neutral", 85.0, null, 0.0, null, 0.0, now.minusDays(2)));
        checkInDAO.save(new CheckInRecord("c-new", "user-order", "new.jpg", null, "Happy", 95.0, null, 0.0, null, 0.0, now));

        InsightsService.UserInsights insights = insightsService.computeUserInsights("user-order");
        List<CheckInRecord> recent = insights.recentRecords();

        assertEquals(3, recent.size());
        assertEquals("c-new", recent.get(0).id(), "First record must be newest");
        assertEquals("c-mid", recent.get(1).id(), "Second record must be middle");
        assertEquals("c-old", recent.get(2).id(), "Third record must be oldest");
    }

    @Test
    @DisplayName("13. User isolation in Insights: User A's check-ins never affect User B's statistics")
    void testUserIsolationInInsights() {
        // User B has 5 check-ins
        for (int i = 0; i < 5; i++) {
            checkInDAO.save(new CheckInRecord("b-" + i, "user-b-stats", "b.jpg", "t", "Fear", 90.0, "Fear", 90.0, "Fear", 90.0, LocalDateTime.now()));
        }

        // User A has 0 check-ins
        User userA = new User("user-a-stats", "User A", "userA@emosense.test", "password123");
        UserSession.getInstance().login(userA);

        InsightsService.UserInsights aInsights = insightsService.computeUserInsights("user-a-stats");
        assertEquals(0, aInsights.totalCheckIns());
        assertEquals(0, aInsights.facialDistribution().get("Fear"));

        InsightsScreen screen = new InsightsScreen(null, insightsService, historyService);
        assertTrue(collectAllLabelTexts(screen).contains("No check-ins yet"));
    }

    @Test
    @DisplayName("14. Database failure in Insights: Displays error message with Retry action")
    void testDatabaseFailureInInsights() {
        User user = new User("user-ins-err", "Err", "inserr@emosense.test", "password123");
        UserSession.getInstance().login(user);

        InsightsService failingService = new InsightsService(new HistoryService(null, null) {
            @Override
            public List<CheckInRecord> getUserHistory(String userId) {
                throw new HistoryAccessException("Connection lost", null);
            }
        });

        InsightsScreen screen = new InsightsScreen(null, failingService, historyService);
        List<String> labels = collectAllLabelTexts(screen);

        assertTrue(labels.contains("Unable to load your insights right now."));
        assertTrue(labels.contains("A database error occurred while calculating your insights. Please try again."));

        List<Button> buttons = collectAllButtons(screen);
        assertTrue(buttons.stream().anyMatch(b -> b.getText() != null && b.getText().equals("Retry")));
    }

    // Helper utilities
    private List<String> collectAllLabelTexts(Node root) {
        List<String> list = new ArrayList<>();
        collectLabelsRecursive(root, list);
        return list;
    }

    private void collectLabelsRecursive(Node node, List<String> list) {
        if (node instanceof javafx.scene.control.Labeled l && l.getText() != null) {
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

    @SuppressWarnings("unchecked")
    private BarChart<String, Number> findBarChart(Node root) {
        if (root instanceof BarChart<?, ?> bc) {
            return (BarChart<String, Number>) bc;
        }
        if (root instanceof ScrollPane sp && sp.getContent() != null) {
            BarChart<String, Number> c = findBarChart(sp.getContent());
            if (c != null) return c;
        }
        if (root instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                BarChart<String, Number> c = findBarChart(child);
                if (c != null) return c;
            }
        }
        return null;
    }
}
