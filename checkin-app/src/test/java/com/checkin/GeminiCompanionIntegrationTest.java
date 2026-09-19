package com.checkin;

import com.checkin.config.AIConfiguration;
import com.checkin.model.AnalysisResult;
import com.checkin.model.ChatMessage;
import com.checkin.screens.CompanionScreen;
import com.checkin.services.AICompanionService;
import com.checkin.services.CompanionApiException;
import com.checkin.services.GeminiCompanionEngine;
import javafx.application.Platform;
import org.junit.jupiter.api.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration and unit tests for EmoSense Phase 5B AI Companion.
 * Validates:
 * 1. API key missing -> fallback engine
 * 2. API key present -> construction without exposure
 * 3. Invalid API response -> fallback engine
 * 4. HTTP 401/403 -> fallback engine
 * 5. HTTP 429 -> fallback engine
 * 6. Timeout -> fallback engine
 * 7. Network failure -> fallback engine
 * 8. Empty API response -> fallback engine
 * 9. Multi-turn conversation history
 * 10. Check-in context payload without images/tensors
 * 11. Safety/crisis guardrail evaluated before API call
 * 12. Thinking animation removal on API success
 * 13. Thinking animation removal on API failure
 * 14. Send button re-enabled after response
 * 15. Credential privacy: key is never leaked in toString, exceptions, or logs
 */
public class GeminiCompanionIntegrationTest {

    private static final String TEST_KEY = "AIzaSyFakeTestGeminiKey_ABC123";

    @BeforeAll
    static void initJavaFx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already started
        }
    }

    @BeforeEach
    void setUp() {
        AIConfiguration.resetTestEnvironment();
    }

    @AfterEach
    void tearDown() {
        AIConfiguration.resetTestEnvironment();
    }

    // =========================================================================
    // 1. API KEY MISSING -> FALLBACK ENGINE
    // =========================================================================
    @Test
    @DisplayName("1. When API key is missing, PatternBasedCompanionEngine fallback is used")
    void testApiKeyMissingUsesFallbackEngine() {
        AIConfiguration.setTestApiKey(null);
        assertFalse(AIConfiguration.isApiKeyAvailable());

        AICompanionService service = new AICompanionService();
        assertFalse(service.isOnlineEngineConfigured(), "Online engine must not be configured when key is null");
        assertEquals(AICompanionService.EngineMode.OFFLINE_FALLBACK, service.getCurrentMode());

        ChatMessage reply = service.sendMessage("I won the running competition in my college");
        assertNotNull(reply);
        assertTrue(service.isLastResponseUsedFallback(), "Must record fallback engine usage");
        String text = reply.text().toLowerCase();
        assertTrue(text.contains("running competition") || text.contains("proud") || text.contains("wonderful"),
                "Fallback engine must provide high quality reflection");
    }

    // =========================================================================
    // 2. API KEY PRESENT -> CONSTRUCT ENGINE WITHOUT EXPOSING KEY
    // =========================================================================
    @Test
    @DisplayName("2. When API key is present, Gemini engine can be constructed without exposing the key")
    void testApiKeyPresentConstructsWithoutExposingKey() {
        AIConfiguration.setTestApiKey(TEST_KEY);
        assertTrue(AIConfiguration.isApiKeyAvailable());
        assertEquals("gemini-1.5-flash", AIConfiguration.getModelName());

        GeminiCompanionEngine engine = new GeminiCompanionEngine();
        assertNotNull(engine);
        assertEquals("gemini-1.5-flash", engine.getModelName());

        String representation = engine.toString();
        assertFalse(representation.contains(TEST_KEY), "API key must NEVER be included in engine.toString()");
        assertTrue(representation.contains("GeminiCompanionEngine"), "Must show engine class name");
    }

    // =========================================================================
    // 3. INVALID API RESPONSE -> FALLBACK ENGINE
    // =========================================================================
    @Test
    @DisplayName("3. Invalid or malformed JSON from API triggers fallback engine seamlessly")
    void testInvalidApiResponseTriggersFallback() {
        HttpClient fakeClient = createFakeClient(req -> new FakeHttpResponse(200, "{ invalid json not closed ...", req));
        GeminiCompanionEngine gemini = new GeminiCompanionEngine(TEST_KEY, "gemini-1.5-flash", fakeClient);
        AICompanionService service = new AICompanionService(gemini);

        ChatMessage reply = service.sendMessage("I had a very difficult day today");
        assertNotNull(reply);
        assertTrue(service.isLastResponseUsedFallback(), "Must fall back to pattern engine on malformed JSON");
        String text = reply.text().toLowerCase();
        assertTrue(text.contains("difficult") || text.contains("sorry"), "Must return empathetic fallback response");
    }

    // =========================================================================
    // 4. HTTP 401 & 403 -> FALLBACK ENGINE
    // =========================================================================
    @Test
    @DisplayName("4. HTTP 401 and 403 authentication errors trigger fallback engine")
    void testHttp401And403TriggersFallback() {
        // Test 401 Unauthorized
        HttpClient client401 = createFakeClient(req -> new FakeHttpResponse(401, "{\"error\": {\"message\": \"API key invalid\"}}", req));
        GeminiCompanionEngine gemini401 = new GeminiCompanionEngine(TEST_KEY, "gemini-1.5-flash", client401);
        AICompanionService service401 = new AICompanionService(gemini401);

        ChatMessage reply401 = service401.sendMessage("Hello there");
        assertNotNull(reply401);
        assertTrue(service401.isLastResponseUsedFallback());

        // Test 403 Forbidden
        HttpClient client403 = createFakeClient(req -> new FakeHttpResponse(403, "{\"error\": {\"message\": \"Permission denied\"}}", req));
        GeminiCompanionEngine gemini403 = new GeminiCompanionEngine(TEST_KEY, "gemini-1.5-flash", client403);
        AICompanionService service403 = new AICompanionService(gemini403);

        ChatMessage reply403 = service403.sendMessage("Good evening");
        assertNotNull(reply403);
        assertTrue(service403.isLastResponseUsedFallback());
    }

    // =========================================================================
    // 5. HTTP 429 RATE LIMIT -> FALLBACK ENGINE
    // =========================================================================
    @Test
    @DisplayName("5. HTTP 429 Rate Limit error triggers fallback engine")
    void testHttp429RateLimitTriggersFallback() {
        HttpClient client429 = createFakeClient(req -> new FakeHttpResponse(429, "{\"error\": {\"message\": \"Resource exhausted: quota exceeded\"}}", req));
        GeminiCompanionEngine gemini = new GeminiCompanionEngine(TEST_KEY, "gemini-1.5-flash", client429);
        AICompanionService service = new AICompanionService(gemini);

        ChatMessage reply = service.sendMessage("I feel completely overwhelmed by my work");
        assertNotNull(reply);
        assertTrue(service.isLastResponseUsedFallback());
        String text = reply.text().toLowerCase();
        assertTrue(text.contains("pressure") || text.contains("overwhelming") || text.contains("breathing"),
                "Must return supportive fallback response: " + reply.text());
    }

    // =========================================================================
    // 6. TIMEOUT -> FALLBACK ENGINE
    // =========================================================================
    @Test
    @DisplayName("6. Request timeout triggers fallback engine without locking the application")
    void testTimeoutTriggersFallback() {
        HttpClient timeoutClient = createThrowingClient(new HttpTimeoutException("Request timed out after 15s"));
        GeminiCompanionEngine gemini = new GeminiCompanionEngine(TEST_KEY, "gemini-1.5-flash", timeoutClient);
        AICompanionService service = new AICompanionService(gemini);

        ChatMessage reply = service.sendMessage("I won first place in my competition!");
        assertNotNull(reply);
        assertTrue(service.isLastResponseUsedFallback());
        String text = reply.text().toLowerCase();
        assertTrue(text.contains("achievement") || text.contains("proud") || text.contains("competition"),
                "Must return supportive fallback response");
    }

    // =========================================================================
    // 7. NETWORK FAILURE -> FALLBACK ENGINE
    // =========================================================================
    @Test
    @DisplayName("7. Network/IO failure triggers fallback engine without crashing")
    void testNetworkFailureTriggersFallback() {
        HttpClient netErrorClient = createThrowingClient(new IOException("Connection refused: no internet access"));
        GeminiCompanionEngine gemini = new GeminiCompanionEngine(TEST_KEY, "gemini-1.5-flash", netErrorClient);
        AICompanionService service = new AICompanionService(gemini);

        ChatMessage reply = service.sendMessage("I'm feeling really tired and drained");
        assertNotNull(reply);
        assertTrue(service.isLastResponseUsedFallback());
        String text = reply.text().toLowerCase();
        assertTrue(text.contains("fatigue") || text.contains("exhaustion") || text.contains("rest"),
                "Must return fatigue-supportive fallback");
    }

    // =========================================================================
    // 8. EMPTY API RESPONSE -> FALLBACK ENGINE
    // =========================================================================
    @Test
    @DisplayName("8. Empty candidates or blank text response triggers fallback engine")
    void testEmptyApiResponseTriggersFallback() {
        // Case A: empty candidates array
        HttpClient emptyCandidatesClient = createFakeClient(req -> new FakeHttpResponse(200, "{\"candidates\": []}", req));
        GeminiCompanionEngine geminiA = new GeminiCompanionEngine(TEST_KEY, "gemini-1.5-flash", emptyCandidatesClient);
        AICompanionService serviceA = new AICompanionService(geminiA);

        ChatMessage replyA = serviceA.sendMessage("Hello");
        assertNotNull(replyA);
        assertTrue(serviceA.isLastResponseUsedFallback());

        // Case B: candidates present but blank text part
        String blankJson = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [ {"text": "   "} ]
                      }
                    }
                  ]
                }
                """;
        HttpClient blankClient = createFakeClient(req -> new FakeHttpResponse(200, blankJson, req));
        GeminiCompanionEngine geminiB = new GeminiCompanionEngine(TEST_KEY, "gemini-1.5-flash", blankClient);
        AICompanionService serviceB = new AICompanionService(geminiB);

        ChatMessage replyB = serviceB.sendMessage("Hello again");
        assertNotNull(replyB);
        assertTrue(serviceB.isLastResponseUsedFallback());
    }

    // =========================================================================
    // 9. CONVERSATION HISTORY PRESERVATION
    // =========================================================================
    @Test
    @DisplayName("9. Multi-turn conversation history is maintained across turns")
    void testConversationHistoryStillWorks() {
        String successGeminiJson = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [ {"text": "Winning that race is a tremendous achievement! How did you feel at the finish line?"} ]
                      }
                    }
                  ]
                }
                """;
        HttpClient fakeClient = createFakeClient(req -> new FakeHttpResponse(200, successGeminiJson, req));
        GeminiCompanionEngine gemini = new GeminiCompanionEngine(TEST_KEY, "gemini-1.5-flash", fakeClient);
        AICompanionService service = new AICompanionService(gemini);

        // Turn 1
        ChatMessage r1 = service.sendMessage("I won the running competition in my college");
        assertNotNull(r1);
        assertEquals(2, service.getConversationHistory().size());
        assertTrue(service.getConversationHistory().get(0).isUser());
        assertTrue(service.getConversationHistory().get(1).isCompanion());

        // Turn 2
        ChatMessage r2 = service.sendMessage("I was super nervous before it began");
        assertNotNull(r2);
        assertEquals(4, service.getConversationHistory().size());
        assertEquals("I was super nervous before it began", service.getConversationHistory().get(2).text());
        assertTrue(service.getConversationHistory().get(3).isCompanion());

        // Clear conversation
        service.clearConversation();
        assertEquals(0, service.getConversationHistory().size());
    }

    // =========================================================================
    // 10. CHECK-IN CONTEXT WITHOUT IMAGE OR TENSOR LEAKAGE
    // =========================================================================
    @Test
    @DisplayName("10. Check-in context is formatted in payload without uploading photos or tensors")
    void testCheckInContextWorksWithoutPrivacyLeakage() throws Exception {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        AtomicReference<HttpHeaders> capturedHeaders = new AtomicReference<>();

        HttpClient fakeClient = createFakeClient(req -> {
            capturedHeaders.set(req.headers());
            // Read body from request publisher
            return new FakeHttpResponse(200, """
                    {
                      "candidates": [
                        {
                          "content": {
                            "parts": [ {"text": "I notice your check-in reflected joy and excitement today!"} ]
                          }
                        }
                      ]
                    }
                    """, req);
        });

        GeminiCompanionEngine gemini = new GeminiCompanionEngine(TEST_KEY, "gemini-1.5-flash", fakeClient);
        AICompanionService service = new AICompanionService(gemini);

        AnalysisResult result = new AnalysisResult(
                "Happy", 92.5,
                "Excitement", 95.0,
                "Positive / Uplifted Signal", 93.8
        );
        service.setCheckInContext(result);

        String payload = gemini.buildPayloadJson("Tell me about my check in", service.getConversationHistory(), result);
        assertNotNull(payload);

        // Verification: Contains textual context
        assertTrue(payload.contains("Facial signal: Happy"));
        assertTrue(payload.contains("Textual cue: Excitement"));
        assertTrue(payload.contains("Multimodal insight: Positive / Uplifted Signal"));

        // Verification: PRIVACY GUARANTEES (no images, no raw tensors, no database passwords)
        assertFalse(payload.contains(".jpg"), "Must not include image file paths");
        assertFalse(payload.contains(".png"), "Must not include image file paths");
        assertFalse(payload.contains("base64"), "Must not include base64 photo data");
        assertFalse(payload.contains("tensor"), "Must not leak internal tensor objects");
        assertFalse(payload.contains("float32"), "Must not leak internal tensor types");
        assertFalse(payload.contains("password"), "Must not leak credentials");
    }

    // =========================================================================
    // 11. SAFETY / CRISIS GUARDRAIL EVALUATED BEFORE API CALL
    // =========================================================================
    @Test
    @DisplayName("11. Crisis and self-harm messages trigger immediate safety response WITHOUT calling external API")
    void testSafetyCrisisGuardrailPreFlightBypassesApi() {
        AtomicBoolean apiCalled = new AtomicBoolean(false);
        HttpClient fakeClient = createFakeClient(req -> {
            apiCalled.set(true);
            return new FakeHttpResponse(200, "{\"candidates\": []}", req);
        });

        GeminiCompanionEngine gemini = new GeminiCompanionEngine(TEST_KEY, "gemini-1.5-flash", fakeClient);
        AICompanionService service = new AICompanionService(gemini);

        List<String> crisisPrompts = List.of(
                "I want to kill myself",
                "I feel like ending my life",
                "I want to die right now",
                "I might hurt myself tonight"
        );

        for (String crisisText : crisisPrompts) {
            apiCalled.set(false);
            service.clearConversation();

            ChatMessage reply = service.sendMessage(crisisText);
            assertNotNull(reply);

            // 1. Must NEVER call the external API for sensitive crisis messages
            assertFalse(apiCalled.get(), "Crisis message MUST NOT be sent to external Gemini API: " + crisisText);

            // 2. Must contain empathetic crisis support hotline resources (988)
            String text = reply.text();
            assertTrue(text.contains("988") || text.contains("crisis line"), "Must include 988 crisis resources");
            assertTrue(text.contains("You don't have to carry this alone"), "Must provide empathetic crisis response");
        }
    }

    @Test
    @DisplayName("11b. Medical diagnosis inquiries return immediate non-clinical disclaimer WITHOUT calling external API")
    void testMedicalDiagnosisDisclaimerPreFlightBypassesApi() {
        AtomicBoolean apiCalled = new AtomicBoolean(false);
        HttpClient fakeClient = createFakeClient(req -> {
            apiCalled.set(true);
            return new FakeHttpResponse(200, "{\"candidates\": []}", req);
        });

        GeminiCompanionEngine gemini = new GeminiCompanionEngine(TEST_KEY, "gemini-1.5-flash", fakeClient);
        AICompanionService service = new AICompanionService(gemini);

        apiCalled.set(false);
        ChatMessage reply = service.sendMessage("Can you diagnose me with depression?");

        assertFalse(apiCalled.get(), "Medical diagnosis inquiries must NOT be sent to external API");
        assertNotNull(reply);
        assertTrue(reply.text().contains("EmoSense provides emotional reflections, not medical or psychological diagnosis"));
    }

    // =========================================================================
    // 12. THINKING STATE REMOVED AFTER API SUCCESS
    // =========================================================================
    @Test
    @DisplayName("12. Thinking animation state is correctly cleaned up and removed after successful API reply")
    void testThinkingStateRemovedAfterSuccess() {
        String geminiSuccess = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [ {"text": "I hear you, and it sounds like that was a meaningful moment."} ]
                      }
                    }
                  ]
                }
                """;
        HttpClient fakeClient = createFakeClient(req -> new FakeHttpResponse(200, geminiSuccess, req));
        GeminiCompanionEngine gemini = new GeminiCompanionEngine(TEST_KEY, "gemini-1.5-flash", fakeClient);
        AICompanionService service = new AICompanionService(gemini);

        CompanionScreen screen = new CompanionScreen(null, service);
        screen.getInputField().setText("Today was a good day");
        screen.getSendButton().fire();

        assertTrue(screen.isResponding());
        assertNotNull(screen.getCurrentThinkingBubble());

        screen.finishCompanionResponse();

        assertFalse(screen.isResponding(), "Screen must not be in responding state after finish");
        assertNull(screen.getCurrentThinkingBubble(), "Thinking bubble must be removed");
        assertFalse(screen.isSendButtonDisabled(), "Send button must be re-enabled");
        assertEquals(2, service.getConversationHistory().size());
    }

    // =========================================================================
    // 13. THINKING STATE REMOVED AFTER API FAILURE
    // =========================================================================
    @Test
    @DisplayName("13. Thinking animation state is correctly cleaned up and removed even when API fails and falls back")
    void testThinkingStateRemovedAfterFailure() {
        HttpClient throwingClient = createThrowingClient(new IOException("Network down"));
        GeminiCompanionEngine gemini = new GeminiCompanionEngine(TEST_KEY, "gemini-1.5-flash", throwingClient);
        AICompanionService service = new AICompanionService(gemini);

        CompanionScreen screen = new CompanionScreen(null, service);
        screen.getInputField().setText("I feel sad today");
        screen.getSendButton().fire();

        assertTrue(screen.isResponding());
        assertNotNull(screen.getCurrentThinkingBubble());

        screen.finishCompanionResponse();

        assertFalse(screen.isResponding(), "Screen must not be in responding state after failure finish");
        assertNull(screen.getCurrentThinkingBubble(), "Thinking bubble must be removed after fallback");
        assertFalse(screen.isSendButtonDisabled(), "Send button must be re-enabled after fallback");
        assertEquals(2, service.getConversationHistory().size());
        assertTrue(service.isLastResponseUsedFallback());
    }

    // =========================================================================
    // 14. SEND BUTTON RE-ENABLED AFTER COMPLETION
    // =========================================================================
    @Test
    @DisplayName("14. Send button is disabled during processing and re-enabled upon response delivery")
    void testSendButtonReenabledAfterCompletion() {
        AICompanionService service = new AICompanionService();
        CompanionScreen screen = new CompanionScreen(null, service);

        assertFalse(screen.isSendButtonDisabled());
        screen.getInputField().setText("Test message");
        screen.getSendButton().fire();

        assertTrue(screen.isSendButtonDisabled(), "Send button must be disabled while responding");
        assertTrue(screen.getInputField().isDisabled(), "Input field must be disabled while responding");

        screen.finishCompanionResponse();

        assertFalse(screen.isSendButtonDisabled(), "Send button must be re-enabled after completion");
        assertFalse(screen.getInputField().isDisabled(), "Input field must be re-enabled after completion");
    }

    // =========================================================================
    // 15. CREDENTIAL PRIVACY ASSERTIONS
    // =========================================================================
    @Test
    @DisplayName("15. CompanionApiException and logs sanitize and redact API keys unconditionally")
    void testCredentialPrivacySanitization() {
        // Test CompanionApiException sanitization
        CompanionApiException ex = new CompanionApiException("Failed to authenticate with key=AIzaSySecretKey123456 and token=abcde");
        assertFalse(ex.getMessage().contains("AIzaSySecretKey123456"), "Exception message must redact API key");
        assertTrue(ex.getMessage().contains("[REDACTED]"));

        // Test engine toString() does not reveal key
        GeminiCompanionEngine engine = new GeminiCompanionEngine("SUPER_SECRET_KEY_9999", "gemini-2.0-flash");
        assertFalse(engine.toString().contains("SUPER_SECRET_KEY_9999"), "toString must not expose key");
    }

    // =========================================================================
    // TEST HELPER UTILITIES: PURE JAVA 17 FAKE HTTP CLIENT & RESPONSE
    // =========================================================================

    private static HttpClient createFakeClient(Function<HttpRequest, HttpResponse<String>> handler) {
        return new FakeHttpClient(handler);
    }

    private static HttpClient createThrowingClient(Exception exceptionToThrow) {
        return new FakeHttpClient(req -> {
            if (exceptionToThrow instanceof RuntimeException re) throw re;
            throw new RuntimeException(exceptionToThrow);
        }) {
            @Override
            public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseHandler) throws IOException, InterruptedException {
                if (exceptionToThrow instanceof HttpTimeoutException te) throw te;
                if (exceptionToThrow instanceof IOException ioe) throw ioe;
                if (exceptionToThrow instanceof InterruptedException ie) throw ie;
                throw new IOException("Simulated network failure", exceptionToThrow);
            }
        };
    }

    static class FakeHttpClient extends HttpClient {
        private final Function<HttpRequest, HttpResponse<String>> responseProvider;

        FakeHttpClient(Function<HttpRequest, HttpResponse<String>> responseProvider) {
            this.responseProvider = responseProvider;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseHandler) throws IOException, InterruptedException {
            HttpResponse<String> resp = responseProvider.apply(request);
            if (resp == null) {
                throw new IOException("Null response from fake HTTP provider");
            }
            return (HttpResponse<T>) resp;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseHandler) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return send(request, responseHandler);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            return sendAsync(request, responseHandler);
        }

        @Override public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
        @Override public Optional<Duration> connectTimeout() { return Optional.of(Duration.ofSeconds(5)); }
        @Override public Redirect followRedirects() { return Redirect.NEVER; }
        @Override public Optional<ProxySelector> proxy() { return Optional.empty(); }
        @Override public SSLContext sslContext() { try { return SSLContext.getDefault(); } catch (Exception e) { return null; } }
        @Override public SSLParameters sslParameters() { return new SSLParameters(); }
        @Override public Optional<Authenticator> authenticator() { return Optional.empty(); }
        @Override public Version version() { return Version.HTTP_2; }
        @Override public Optional<Executor> executor() { return Optional.empty(); }
    }

    static class FakeHttpResponse implements HttpResponse<String> {
        private final int statusCode;
        private final String body;
        private final HttpRequest request;

        FakeHttpResponse(int statusCode, String body, HttpRequest request) {
            this.statusCode = statusCode;
            this.body = body;
            this.request = request;
        }

        @Override public int statusCode() { return statusCode; }
        @Override public HttpRequest request() { return request; }
        @Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
        @Override public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (k, v) -> true); }
        @Override public String body() { return body; }
        @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
        @Override public URI uri() { return request != null ? request.uri() : URI.create("https://generativelanguage.googleapis.com"); }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_2; }
    }
}
