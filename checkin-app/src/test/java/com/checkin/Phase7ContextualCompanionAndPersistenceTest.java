package com.checkin;

import com.checkin.config.AIConfiguration;
import com.checkin.dao.InMemoryUserDAO;
import com.checkin.dao.UserDAO;
import com.checkin.model.AnalysisResult;
import com.checkin.model.ChatMessage;
import com.checkin.model.User;
import com.checkin.screens.SignInScreen;
import com.checkin.services.AICompanionService;
import com.checkin.services.AuthenticationService;
import com.checkin.services.GeminiCompanionEngine;
import com.checkin.utils.Navigation;
import com.checkin.utils.PasswordHasher;
import javafx.application.Platform;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test suite for Phase 7 Fixes:
 * - Empty Sign-In screen without pre-filled demo credentials
 * - Authentication persistence behavior & standardized error messages
 * - Contextual AI Companion with intent/emotion cue detection across all categories
 * - Verification of the 5 manual test scenarios
 * - Zero generic repetition / clichés
 * - Gemini prompt structure & API failure graceful fallback
 */
public class Phase7ContextualCompanionAndPersistenceTest {

    private AuthenticationService authService;
    private InMemoryUserDAO sharedUserDAO;
    private AICompanionService companionService;

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX already started
        }
    }

    @BeforeEach
    void setUp() {
        sharedUserDAO = new InMemoryUserDAO();
        authService = new AuthenticationService(sharedUserDAO);
        companionService = new AICompanionService();
        AIConfiguration.resetTestEnvironment();
    }

    @AfterEach
    void tearDown() {
        AIConfiguration.resetTestEnvironment();
    }

    // =========================================================================
    // PART 1 & 5 — SIGN IN CLEANUP & ERROR MESSAGES
    // =========================================================================

    @Test
    @DisplayName("Sign In fields start completely empty with zero pre-filled demo credentials")
    void testSignInFieldsStartEmpty() {
        Navigation nav = new Navigation(null, authService);
        SignInScreen screen = new SignInScreen(nav);

        assertEquals("", screen.getEmailField().getText(), "Email field must start completely empty");
        assertEquals("", screen.getPasswordField().getText(), "Password field must start completely empty");
        assertNotEquals("test@emosense.com", screen.getEmailField().getText());
        assertNotEquals("password123", screen.getPasswordField().getText());
    }

    @Test
    @DisplayName("Missing account produces standardized error message")
    void testMissingAccountProducesCorrectMessage() {
        var result = authService.signIn("nobody@emosense.com", "anyPassword123");
        assertFalse(result.success());
        assertEquals("Account not found. Please check your email or create an account.", result.message());
    }

    @Test
    @DisplayName("Incorrect password produces standardized error message")
    void testIncorrectPasswordProducesCorrectMessage() {
        authService.signUp("Valid User", "valid@emosense.com", "correctPassword123", "correctPassword123");
        var result = authService.signIn("valid@emosense.com", "wrongPassword");
        assertFalse(result.success());
        assertEquals("Incorrect password. Please try again.", result.message());
    }

    @Test
    @DisplayName("Database unavailable produces clear storage error")
    void testDatabaseUnavailableProducesClearStorageError() {
        UserDAO brokenDAO = new UserDAO() {
            @Override public Optional<User> findByEmail(String email) { throw new RuntimeException("MySQL offline"); }
            @Override public boolean existsByEmail(String email) { throw new RuntimeException("MySQL offline"); }
            @Override public boolean save(User user) { throw new RuntimeException("MySQL offline"); }
        };
        AuthenticationService failingService = new AuthenticationService(brokenDAO);

        var signInRes = failingService.signIn("user@test.com", "password");
        assertFalse(signInRes.success());
        assertEquals("Account storage is currently unavailable. Please check the database connection and try again.", signInRes.message());

        var signUpRes = failingService.signUp("Name", "user@test.com", "password", "password");
        assertFalse(signUpRes.success());
        assertEquals("Account storage is currently unavailable. Please check the database connection and try again.", signUpRes.message());
    }

    // =========================================================================
    // PART 2 & 3 — PERSISTENCE BEHAVIOR & RESTART SIMULATION
    // =========================================================================

    @Test
    @DisplayName("Password remains securely hashed and persists across new service instances")
    void testPasswordHashedAndPersistsAcrossServiceInstances() {
        String rawPassword = "SuperSecurePassword123!";
        var signUpRes = authService.signUp("Persistent User", "persist@emosense.com", rawPassword, rawPassword);
        assertTrue(signUpRes.success());

        User savedUser = sharedUserDAO.findByEmail("persist@emosense.com").orElseThrow();
        assertNotEquals(rawPassword, savedUser.getPassword(), "Raw password must NEVER be stored in plain text");
        assertTrue(PasswordHasher.verify(rawPassword, savedUser.getPassword()), "Hash must be verifiable with BCrypt");

        // Simulate application restart with new AuthenticationService pointing to the same persistent store
        AuthenticationService restartedAuthService = new AuthenticationService(sharedUserDAO);
        var loginRes = restartedAuthService.signIn("persist@emosense.com", rawPassword);
        assertTrue(loginRes.success(), "User must be able to sign in on restarted service when persistent store is available");
        assertEquals("Persistent User", loginRes.user().getFullName());
    }

    // =========================================================================
    // PART 6-12 & 16 — CONTEXTUAL AI COMPANION (5 MANUAL TEST SCENARIOS)
    // =========================================================================

    @Test
    @DisplayName("Message 1 (Worry/Exam): 'I am really worried about tomorrow's exam.'")
    void testMessage1_WorryExamScenario() {
        String message = "I am really worried about tomorrow's exam.";
        ChatMessage reply = companionService.sendMessage(message);
        assertNotNull(reply);
        String replyLower = reply.text().toLowerCase();

        assertTrue(replyLower.contains("exam") || replyLower.contains("tomorrow") || replyLower.contains("stress") || replyLower.contains("rest"),
                "Response must acknowledge exam/worry: " + reply.text());
        assertFalse(reply.text().contains("clinical depression"), "Must not diagnose");
        assertFalse(reply.text().contains("anxiety disorder"), "Must not diagnose");
        assertNoBannedClichés(reply.text());
    }

    @Test
    @DisplayName("Message 2 (Celebration/Project): 'I finally finished my project today!'")
    void testMessage2_CelebrationProjectScenario() {
        String message = "I finally finished my project today!";
        ChatMessage reply = companionService.sendMessage(message);
        assertNotNull(reply);
        String replyLower = reply.text().toLowerCase();

        assertTrue(replyLower.contains("project") || replyLower.contains("congratulations") || replyLower.contains("milestone") || replyLower.contains("finished"),
                "Response must celebrate project completion: " + reply.text());
        assertNoBannedClichés(reply.text());
    }

    @Test
    @DisplayName("Message 3 (Disappointment): 'I studied so much but my result was disappointing.'")
    void testMessage3_DisappointmentScenario() {
        String message = "I studied so much but my result was disappointing.";
        ChatMessage reply = companionService.sendMessage(message);
        assertNotNull(reply);
        String replyLower = reply.text().toLowerCase();

        assertTrue(replyLower.contains("disappointment") || replyLower.contains("discouraging") || replyLower.contains("result") || replyLower.contains("effort"),
                "Response must acknowledge effort-outcome disappointment honestly: " + reply.text());
        assertNoBannedClichés(reply.text());
    }

    @Test
    @DisplayName("Message 4 (Confusion): 'I don't know what I should do next.'")
    void testMessage4_ConfusionScenario() {
        String message = "I don't know what I should do next.";
        ChatMessage reply = companionService.sendMessage(message);
        assertNotNull(reply);
        String replyLower = reply.text().toLowerCase();

        assertTrue(replyLower.contains("decision") || replyLower.contains("smallest") || replyLower.contains("step") || replyLower.contains("options") || replyLower.contains("direction"),
                "Response must help clarify immediate confusion / next steps: " + reply.text());
        assertNoBannedClichés(reply.text());
    }

    @Test
    @DisplayName("Message 5 (Frustration): 'Everything is going wrong today and I'm getting frustrated.'")
    void testMessage5_FrustrationScenario() {
        String message = "Everything is going wrong today and I'm getting frustrated.";
        ChatMessage reply = companionService.sendMessage(message);
        assertNotNull(reply);
        String replyLower = reply.text().toLowerCase();

        assertTrue(replyLower.contains("frustration") || replyLower.contains("wrong") || replyLower.contains("pause") || replyLower.contains("control") || replyLower.contains("timeout"),
                "Response must acknowledge frustration and help identify what can be controlled: " + reply.text());
        assertNoBannedClichés(reply.text());
    }

    @Test
    @DisplayName("All 5 test messages produce meaningfully distinct responses")
    void testAll5MessagesProduceDifferentResponses() {
        String[] testMessages = {
            "I am really worried about tomorrow's exam.",
            "I finally finished my project today!",
            "I studied so much but my result was disappointing.",
            "I don't know what I should do next.",
            "Everything is going wrong today and I'm getting frustrated."
        };

        Set<String> uniqueResponses = new HashSet<>();
        System.out.println("\n==================================================");
        System.out.println("[PHASE 7 VERIFICATION] 5 MANUAL TEST SCENARIO RESPONSES");
        System.out.println("==================================================");
        int idx = 1;
        for (String msg : testMessages) {
            AICompanionService freshCompanion = new AICompanionService();
            ChatMessage reply = freshCompanion.sendMessage(msg);
            assertNotNull(reply);
            System.out.println("MESSAGE " + idx + ": \"" + msg + "\"");
            System.out.println("RESPONSE: " + reply.text() + "\n");
            uniqueResponses.add(reply.text());
            idx++;
        }
        System.out.println("==================================================\n");

        assertEquals(5, uniqueResponses.size(), "Each of the 5 distinct user inputs must produce a unique contextual response");
    }

    @Test
    @DisplayName("Stress and Academic pressure inputs produce actionable, supportive suggestions")
    void testStressAndAcademicPressureInputs() {
        ChatMessage reply = companionService.sendMessage("I'm overwhelmed with college work and deadlines.");
        assertNotNull(reply);
        String text = reply.text().toLowerCase();
        assertTrue(text.contains("workload") || text.contains("priority") || text.contains("academic") || text.contains("relief") || text.contains("task"),
                "Must address stress/workload practically: " + reply.text());
        assertNoBannedClichés(reply.text());
    }

    @Test
    @DisplayName("Loneliness input warmly encourages connection with trusted people")
    void testLonelinessScenario() {
        ChatMessage reply = companionService.sendMessage("I feel like nobody understands me.");
        assertNotNull(reply);
        String text = reply.text().toLowerCase();
        assertTrue(text.contains("friend") || text.contains("connection") || text.contains("mentor") || text.contains("people"),
                "Must warmly encourage human connection: " + reply.text());
        assertNoBannedClichés(reply.text());
    }

    @Test
    @DisplayName("Gratitude input responds naturally without repeating emotional support template")
    void testGratitudeScenario() {
        ChatMessage reply = companionService.sendMessage("Thank you, I really needed that.");
        assertNotNull(reply);
        String text = reply.text().toLowerCase();
        assertTrue(text.contains("welcome") || text.contains("glad") || text.contains("helpful"),
                "Must respond warmly to gratitude: " + reply.text());
        assertNoBannedClichés(reply.text());
    }

    // =========================================================================
    // PART 13 & 14 — CHECK-IN CONTEXT & SAFETY GUARDRAILS
    // =========================================================================

    @Test
    @DisplayName("Check-in context is integrated conversationally with uncertainty-aware phrasing")
    void testCheckInContextIntegration() {
        AnalysisResult sampleResult = new AnalysisResult("Neutral", 80.0, "Tired", 75.0, "Low Energy", 77.5);
        companionService.setCheckInContext(sampleResult);

        ChatMessage reply = companionService.sendMessage("What did my check-in say?");
        assertNotNull(reply);
        String text = reply.text().toLowerCase();
        assertTrue(text.contains("observational") || text.contains("reflection") || text.contains("signal") || text.contains("pattern"),
                "Must use uncertainty-aware language rather than asserting clinical fact: " + reply.text());
    }

    @Test
    @DisplayName("Safety Guardrails: Crisis and medical diagnosis inquiries are strictly intercepted")
    void testSafetyGuardrails() {
        // Crisis check
        ChatMessage crisisReply = companionService.sendMessage("I feel like killing myself.");
        assertTrue(crisisReply.text().contains("988") || crisisReply.text().contains("crisis"), "Must provide crisis resources");

        // Medical inquiry check
        companionService.clearConversation();
        ChatMessage medicalReply = companionService.sendMessage("Can you diagnose if I have clinical depression?");
        assertTrue(medicalReply.text().contains("not medical") || medicalReply.text().contains("emotional reflections"), "Must state non-clinical disclaimer");
    }

    // =========================================================================
    // PART 10 — GEMINI ENGINE SYSTEM PROMPT & API FALLBACK
    // =========================================================================

    @Test
    @DisplayName("Gemini prompt includes user message and strict contextual guidelines without requiring real API key")
    void testGeminiPromptConstruction() throws Exception {
        GeminiCompanionEngine engine = new GeminiCompanionEngine("dummy_test_api_key_12345");
        AnalysisResult sampleContext = new AnalysisResult("Focused", 85.0, "Reflective", 90.0, "Calm", 87.5);
        String payloadJson = engine.buildPayloadJson("I am really worried about tomorrow's exam.", List.of(), sampleContext);

        assertNotNull(payloadJson);
        assertTrue(payloadJson.contains("I am really worried about tomorrow's exam."), "Payload must contain the exact user message");
        assertTrue(payloadJson.contains("DIRECT SPECIFICITY"), "Must contain specificity instruction");
        assertTrue(payloadJson.contains("AVOID GENERIC REPETITION"), "Must instruct model to avoid repetition");
        assertTrue(payloadJson.contains("dummy_test_api_key") == false, "API key must not be serialized into body JSON");
    }

    @Test
    @DisplayName("External API failure gracefully triggers contextual fallback engine")
    void testApiFailureGracefulFallback() {
        // Mock failing primary engine
        AICompanionService.CompanionEngine failingPrimary = (msg, hist, ctx) -> {
            throw new RuntimeException("Simulated 503 Service Unavailable");
        };

        AICompanionService robustService = new AICompanionService(failingPrimary, new AICompanionService.PatternBasedCompanionEngine());
        ChatMessage reply = robustService.sendMessage("I finally finished my project today!");

        assertNotNull(reply);
        assertTrue(robustService.isLastResponseUsedFallback(), "Must flag fallback engine usage");
        assertEquals(AICompanionService.EngineMode.OFFLINE_FALLBACK, robustService.getCurrentMode());
        assertTrue(reply.text().toLowerCase().contains("project") || reply.text().toLowerCase().contains("milestone"),
                "Fallback response must still be contextual: " + reply.text());
    }

    // =========================================================================
    // HELPER: Anti-Cliché Verification
    // =========================================================================

    private void assertNoBannedClichés(String text) {
        String lower = text.toLowerCase();
        assertFalse(lower.contains("i'm sorry you're feeling this way"), "Banned generic phrase found in: " + text);
        assertFalse(lower.contains("take a deep breath"), "Banned generic phrase found in: " + text);
        assertFalse(lower.contains("it's okay to feel this way"), "Banned generic phrase found in: " + text);
        assertFalse(lower.contains("you're not alone."), "Banned generic phrase found in: " + text);
    }
}
