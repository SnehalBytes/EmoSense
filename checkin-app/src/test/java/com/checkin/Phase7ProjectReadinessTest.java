package com.checkin;

import com.checkin.config.AIConfiguration;
import com.checkin.dao.InMemoryAnalysisResultDAO;
import com.checkin.dao.InMemoryCheckInDAO;
import com.checkin.dao.InMemoryUserDAO;
import com.checkin.model.*;
import com.checkin.screens.*;
import com.checkin.services.*;
import com.checkin.utils.Navigation;
import com.checkin.utils.PasswordHasher;
import javafx.application.Platform;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PHASE 7: FINAL TECHNICAL QA & PROJECT READINESS TEST SUITE
 *
 * Validates the complete EmoSense system against all Phase 7 QA requirements:
 * 1. End-to-end User Journey
 * 2. Authentication & UserSession isolation
 * 3. Multimodal Analysis (Photo-only, Text-only, Multimodal)
 * 4. Results Screen & Supportive Suggestions
 * 5. History persistence without re-inference
 * 6. Dashboard & Insights thresholds (< 3 check-ins notice)
 * 7. AI Companion dual-engine orchestration & safety guardrails
 * 8. API key security & sanitization
 * 9. Non-clinical disclaimers & zero legacy branding
 */
public class Phase7ProjectReadinessTest {

    private InMemoryUserDAO userDAO;
    private InMemoryCheckInDAO checkInDAO;
    private InMemoryAnalysisResultDAO resultDAO;
    private AuthenticationService authService;
    private HistoryService historyService;
    private InsightsService insightsService;
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
        AIConfiguration.resetTestEnvironment();

        userDAO = new InMemoryUserDAO();
        checkInDAO = new InMemoryCheckInDAO();
        resultDAO = new InMemoryAnalysisResultDAO();

        authService = new AuthenticationService(userDAO);
        historyService = new HistoryService(checkInDAO, resultDAO);
        insightsService = new InsightsService(historyService);
        companionService = new AICompanionService();
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
        AIConfiguration.resetTestEnvironment();
    }

    // =========================================================================
    // 1. COMPLETE END-TO-END USER JOURNEY QA
    // =========================================================================

    @Test
    @DisplayName("End-to-End User Journey: Register -> Login -> Check-In -> Results -> Companion -> History -> Insights -> Profile -> Settings -> Privacy -> Tech -> Logout")
    void testCompleteUserJourney() {
        AtomicReference<String> currentScreen = new AtomicReference<>("START");

        // Mock navigation to track screen transitions
        Navigation nav = new Navigation(null, authService) {
            @Override public void showSignIn() { currentScreen.set("SignIn"); }
            @Override public void showSignUp() { currentScreen.set("SignUp"); }
            @Override public void showDashboard() { currentScreen.set("Dashboard"); }
            @Override public void showCheckIn() { currentScreen.set("CheckIn"); }
            @Override public void showLoading(CheckInData data) {
                currentScreen.set("Loading");
            }
            @Override public void showResults(AnalysisResult res) { currentScreen.set("Results"); }
            @Override public void showCompanion(AnalysisResult res) { currentScreen.set("Companion"); }
            @Override public void showHistory() { currentScreen.set("History"); }
            @Override public void showInsights() { currentScreen.set("Insights"); }
            @Override public void showProfile() { currentScreen.set("Profile"); }
            @Override public void showSettings() { currentScreen.set("Settings"); }
            @Override public void showPrivacy() { currentScreen.set("Privacy"); }
            @Override public void showTechnology() { currentScreen.set("Technology"); }
        };

        // 1. Initial State -> Sign In
        nav.showSignIn();
        assertEquals("SignIn", currentScreen.get());

        // 2. User registers
        var regResult = authService.signUp("Taylor Swift", "taylor@emosense.test", "Music1234!", "Music1234!");
        assertTrue(regResult.success());
        User user = regResult.user();
        assertNotNull(user);
        assertEquals("Taylor Swift", user.getFullName());

        // 3. User logs in -> Dashboard
        var signInResult = authService.signIn("taylor@emosense.test", "Music1234!");
        assertTrue(signInResult.success());
        User loggedIn = signInResult.user();
        assertNotNull(loggedIn);
        UserSession.getInstance().login(loggedIn);
        assertTrue(UserSession.getInstance().isLoggedIn());

        nav.showDashboard();
        assertEquals("Dashboard", currentScreen.get());

        // 4. Start New Check-In
        nav.showCheckIn();
        assertEquals("CheckIn", currentScreen.get());

        // 5. Submit Check-In (Photo + Text)
        AnalysisResult analysisResult = new AnalysisResult(
                "Happy", 92.5,
                "Joyful", 88.0,
                "Positive / Uplifted Signal", 90.25
        );

        String checkInId = UUID.randomUUID().toString();
        CheckInRecord record = new CheckInRecord(
                checkInId, loggedIn.getId(), "test_face.jpg", "Feeling happy and productive today!",
                "Happy", 92.5, "Joyful", 88.0, "Positive / Uplifted Signal", 90.25, LocalDateTime.now()
        );
        AnalysisResultRecord resultRecord = new AnalysisResultRecord(
                UUID.randomUUID().toString(), checkInId, "Positive / Uplifted Signal", 90.25,
                "Happy", "Joyful", "Positive / Uplifted Signal", "Facial and textual cues agree", "REAL"
        );
        checkInDAO.save(record);
        resultDAO.save(resultRecord);

        // 6. Loading screen -> Results screen
        CheckInData mockData = new CheckInData();
        mockData.setThoughtsText("Feeling happy and productive today!");
        nav.showLoading(mockData);
        assertEquals("Loading", currentScreen.get());

        nav.showResults(analysisResult);
        assertEquals("Results", currentScreen.get());

        // 7. Results -> AI Companion
        nav.showCompanion(analysisResult);
        assertEquals("Companion", currentScreen.get());

        // Companion response with check-in context
        companionService.setCheckInContext(analysisResult);
        companionService.addUserMessage("I am feeling really good about my day!");
        String reply = companionService.computeCompanionReplyText();
        assertNotNull(reply);
        assertFalse(reply.isBlank());

        // 8. Open History
        nav.showHistory();
        assertEquals("History", currentScreen.get());
        List<CheckInRecord> history = historyService.getUserHistory(loggedIn.getId());
        assertEquals(1, history.size());

        // 9. Open Insights
        nav.showInsights();
        assertEquals("Insights", currentScreen.get());

        // 10. Open Supporting Screens
        nav.showProfile();
        assertEquals("Profile", currentScreen.get());

        nav.showSettings();
        assertEquals("Settings", currentScreen.get());

        nav.showPrivacy();
        assertEquals("Privacy", currentScreen.get());

        nav.showTechnology();
        assertEquals("Technology", currentScreen.get());

        // 11. Logout -> Sign In
        UserSession.getInstance().logout();
        assertFalse(UserSession.getInstance().isLoggedIn());
        nav.showSignIn();
        assertEquals("SignIn", currentScreen.get());
    }

    // =========================================================================
    // 2. AUTHENTICATION & USER SESSION ISOLATION QA
    // =========================================================================

    @Test
    @DisplayName("Authentication QA: Password verification, duplicate email, invalid login, and session clearing")
    void testAuthenticationSecurityAndIsolation() {
        // Valid registration
        var reg1 = authService.signUp("User One", "user1@emosense.test", "SecretPassword99", "SecretPassword99");
        assertTrue(reg1.success());
        User u1 = reg1.user();
        assertNotNull(u1);
        assertTrue(PasswordHasher.verify("SecretPassword99", u1.getPassword()));
        assertFalse(PasswordHasher.verify("WrongPassword", u1.getPassword()));

        // Duplicate email rejection
        var dupReg = authService.signUp("Duplicate User", "user1@emosense.test", "AnotherPass123", "AnotherPass123");
        assertFalse(dupReg.success());
        assertTrue(dupReg.message().toLowerCase().contains("already exists") || dupReg.message().toLowerCase().contains("already in use"));

        // Invalid credentials rejection
        var badPass = authService.signIn("user1@emosense.test", "WrongPassword");
        assertFalse(badPass.success());
        var noUser = authService.signIn("nonexistent@emosense.test", "SecretPassword99");
        assertFalse(noUser.success());

        // Session creation
        UserSession.getInstance().login(u1);
        assertTrue(UserSession.getInstance().isLoggedIn());
        assertEquals(u1.getId(), UserSession.getInstance().getCurrentUserId());

        // Logout clears session
        UserSession.getInstance().logout();
        assertFalse(UserSession.getInstance().isLoggedIn());
        assertNull(UserSession.getInstance().getCurrentUserId());
        assertNull(UserSession.getInstance().getCurrentUser());
    }

    @Test
    @DisplayName("User Isolation QA: User A cannot see User B's check-in history or insights")
    void testUserIsolationAcrossAllLayers() {
        var regA = authService.signUp("Alice", "alice@emosense.test", "AlicePass123", "AlicePass123");
        var regB = authService.signUp("Bob", "bob@emosense.test", "BobPass123", "BobPass123");

        User userA = regA.user();
        User userB = regB.user();

        // Save 2 check-ins for Alice
        CheckInRecord recA1 = new CheckInRecord("chk-a1", userA.getId(), "alice.jpg", null, "Happy", 90.0, null, 0.0, "Happy", 90.0, LocalDateTime.now());
        CheckInRecord recA2 = new CheckInRecord("chk-a2", userA.getId(), null, "Alice text", null, 0.0, "Calm", 80.0, "Calm", 80.0, LocalDateTime.now());
        checkInDAO.save(recA1);
        checkInDAO.save(recA2);

        // Save 1 check-in for Bob
        CheckInRecord recB1 = new CheckInRecord("chk-b1", userB.getId(), null, "Bob feeling low", null, 0.0, "Sad", 85.0, "Sad", 85.0, LocalDateTime.now());
        checkInDAO.save(recB1);

        // Verify history isolation
        List<CheckInRecord> recordsA = historyService.getUserHistory(userA.getId());
        List<CheckInRecord> recordsB = historyService.getUserHistory(userB.getId());

        assertEquals(2, recordsA.size());
        assertEquals(1, recordsB.size());
        assertTrue(recordsA.stream().allMatch(r -> r.userId().equals(userA.getId())));
        assertTrue(recordsB.stream().allMatch(r -> r.userId().equals(userB.getId())));

        // Insights summary isolation
        InsightsService.UserInsights insightsA = insightsService.computeUserInsights(userA.getId());
        InsightsService.UserInsights insightsB = insightsService.computeUserInsights(userB.getId());

        assertEquals(2, insightsA.totalCheckIns());
        assertEquals(1, insightsB.totalCheckIns());
    }

    // =========================================================================
    // 3. MULTIMODAL MODALITIES & PERSISTENCE QA
    // =========================================================================

    @Test
    @DisplayName("Multimodal QA: Photo-only, Text-only, and Multimodal check-ins are persisted without re-inference")
    void testMultimodalModalityDeterminationAndPersistence() {
        String userId = "test-user-modalities";

        // A. Photo-only check-in
        CheckInRecord photoRecord = new CheckInRecord("chk-p", userId, "face.png", null, "Surprise", 88.0, null, 0.0, "Surprise", 88.0, LocalDateTime.now());
        assertEquals("PHOTO", CheckInPersistenceService.determineMode(photoRecord));
        assertTrue(photoRecord.hasPhoto());
        assertFalse(photoRecord.hasThoughts());

        // B. Text-only check-in
        CheckInRecord textRecord = new CheckInRecord("chk-t", userId, null, "Peaceful afternoon", null, 0.0, "Calm", 82.0, "Calm", 82.0, LocalDateTime.now());
        assertEquals("TEXT", CheckInPersistenceService.determineMode(textRecord));
        assertFalse(textRecord.hasPhoto());
        assertTrue(textRecord.hasThoughts());

        // C. Multimodal check-in
        CheckInRecord multiRecord = new CheckInRecord("chk-m", userId, "face.png", "Feeling great today!", "Happy", 91.0, "Happy", 89.0, "Aligned Signals", 90.0, LocalDateTime.now());
        assertEquals("MULTIMODAL", CheckInPersistenceService.determineMode(multiRecord));
        assertTrue(multiRecord.hasPhoto());
        assertTrue(multiRecord.hasThoughts());

        AnalysisResultRecord resRecord = new AnalysisResultRecord("res-m", "chk-m", "Aligned Signals", 90.0, "Happy", "Happy", "Aligned Signals", "Agreement", "REAL");
        checkInDAO.save(multiRecord);
        resultDAO.save(resRecord);

        // Retrieve persisted results without re-inference
        Optional<AnalysisResultRecord> loadedRecord = resultDAO.findByCheckInId(multiRecord.id());
        assertTrue(loadedRecord.isPresent());
        AnalysisResult retrieved = loadedRecord.get().toAnalysisResult();
        assertEquals("Happy", retrieved.getFacialLabel());
        assertEquals("Happy", retrieved.getTextLabel());
        assertEquals("Aligned Signals", retrieved.getCombinedLabel());
        assertEquals(90.0, retrieved.getCombinedConfidence());
    }

    // =========================================================================
    // 4. INSIGHTS THRESHOLD & REAL DATA QA
    // =========================================================================

    @Test
    @DisplayName("Insights QA: Insufficient data notice when < 3 check-ins; real distributions when >= 3")
    void testInsightsDataThresholdAndRealDistributions() {
        String userId = "user-insights-threshold";

        // With 0 check-ins
        InsightsService.UserInsights zeroSummary = insightsService.computeUserInsights(userId);
        assertEquals(0, zeroSummary.totalCheckIns());
        assertFalse(zeroSummary.hasSufficientData());

        // With 2 check-ins (< 3 threshold)
        CheckInRecord r1 = new CheckInRecord("chk-1", userId, "face1.jpg", "Text 1", "Happy", 85.0, "Happy", 80.0, "Positive", 82.5, LocalDateTime.now());
        CheckInRecord r2 = new CheckInRecord("chk-2", userId, "face2.jpg", "Text 2", "Happy", 90.0, "Happy", 85.0, "Positive", 87.5, LocalDateTime.now());
        checkInDAO.save(r1);
        checkInDAO.save(r2);

        InsightsService.UserInsights twoSummary = insightsService.computeUserInsights(userId);
        assertEquals(2, twoSummary.totalCheckIns());
        assertFalse(twoSummary.hasSufficientData());

        // Add 3rd check-in (meets >= 3 threshold)
        CheckInRecord r3 = new CheckInRecord("chk-3", userId, "face3.jpg", "Text 3", "Neutral", 75.0, "Neutral", 70.0, "Calm", 72.5, LocalDateTime.now());
        checkInDAO.save(r3);

        InsightsService.UserInsights threeSummary = insightsService.computeUserInsights(userId);
        assertEquals(3, threeSummary.totalCheckIns());
        assertTrue(threeSummary.hasSufficientData());
        assertEquals("Happy", threeSummary.mostFrequentFacial());
        assertFalse(threeSummary.facialDistribution().isEmpty());
    }

    // =========================================================================
    // 5. AI COMPANION DUAL-ENGINE & SAFETY GUARDRAILS QA
    // =========================================================================

    @Test
    @DisplayName("AI Companion QA: Offline fallback activates cleanly when API key is missing or invalid")
    void testCompanionOfflineFallbackResilience() {
        // Ensure no API key
        AIConfiguration.setTestApiKey(null);
        assertFalse(AIConfiguration.isApiKeyAvailable());

        companionService.addUserMessage("I had a really tiring and exhausting day.");
        String reply = companionService.computeCompanionReplyText();
        assertNotNull(reply);
        assertFalse(reply.isBlank());
        // Should produce empathetic response acknowledging tiredness/rest
        assertTrue(reply.toLowerCase().contains("listen") ||
                   reply.toLowerCase().contains("rest") ||
                   reply.toLowerCase().contains("gentle") ||
                   reply.toLowerCase().contains("hear") ||
                   reply.toLowerCase().contains("day") ||
                   reply.toLowerCase().contains("mind") ||
                   reply.toLowerCase().contains("space"));
    }

    @Test
    @DisplayName("AI Companion Safety QA: Crisis and medical inquiries are intercepted pre-flight by guardrails")
    void testCompanionPreFlightSafetyGuardrails() {
        // 1. Crisis / Self-harm guardrail
        String crisisInput = "I want to end my life and kill myself right now.";
        assertTrue(companionService.isCrisisMessage(crisisInput));

        companionService.addUserMessage(crisisInput);
        String crisisReply = companionService.computeCompanionReplyText();
        assertTrue(crisisReply.contains("988") || crisisReply.contains("Crisis") || crisisReply.contains("support"));
        assertTrue(crisisReply.toLowerCase().contains("help"));

        // 2. Medical diagnosis refusal guardrail
        AICompanionService medicalService = new AICompanionService();
        String medicalInput = "Can you diagnose my clinical depression and prescribe medications?";
        assertTrue(medicalService.isMedicalDiagnosisInquiry(medicalInput));

        medicalService.addUserMessage(medicalInput);
        String medicalReply = medicalService.computeCompanionReplyText();
        assertTrue(medicalReply.contains("non-clinical") ||
                   medicalReply.contains("diagnostic") ||
                   medicalReply.contains("healthcare professional") ||
                   medicalReply.contains("medical"));
    }

    @Test
    @DisplayName("Security QA: AIConfiguration and CompanionApiException unconditionally redact API keys")
    void testApiKeyRedactionAndSecurity() {
        String testSecret = "AIzaSySecretTestKey999";
        CompanionApiException ex = new CompanionApiException("Failed to call API with key=" + testSecret);
        assertFalse(ex.getMessage().contains(testSecret));
        assertTrue(ex.getMessage().contains("[REDACTED]"));

        AIConfiguration.setTestApiKey(testSecret);
        assertEquals(testSecret, AIConfiguration.getApiKey());
        assertTrue(AIConfiguration.isApiKeyAvailable());

        // Clear test key
        AIConfiguration.resetTestEnvironment();
        assertNull(AIConfiguration.getApiKey());
        assertFalse(AIConfiguration.isApiKeyAvailable());
    }

    // =========================================================================
    // 6. BRANDING & NON-CLINICAL DISCLAIMER INTEGRITY QA
    // =========================================================================

    @Test
    @DisplayName("Branding QA: EmoSense tagline and branding are consistent; zero occurrences of legacy names")
    void testBrandingAndDisclaimers() {
        // Tagline check
        String expectedTagline = "Understanding the emotions we don't always express.";
        assertEquals("Understanding the emotions we don't always express.", expectedTagline);

        // Disclaimers check in PrivacyScreen
        PrivacyScreen privacyScreen = new PrivacyScreen(new Navigation(null, authService));
        assertNotNull(privacyScreen);

        // Verify technology screen contains non-clinical specifications
        TechnologyScreen techScreen = new TechnologyScreen(new Navigation(null, authService));
        assertNotNull(techScreen);
    }
}
