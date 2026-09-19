package com.checkin.config;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

/**
 * Configuration utility for EmoSense AI Companion.
 *
 * Responsibilities:
 * - Reads EMOSENSE_AI_API_KEY from environment variables
 * - Configures model name (default: gemini-1.5-flash, or via EMOSENSE_AI_MODEL)
 * - Safe detection of API key presence without ever logging or printing key values
 * - Strict non-exposure to UI, database, or persistent files
 * - Hermetic testing overrides for automated test suites
 */
public class AIConfiguration {

    public static final String ENV_API_KEY = "EMOSENSE_AI_API_KEY";
    public static final String ENV_MODEL_NAME = "EMOSENSE_AI_MODEL";
    public static final String DEFAULT_MODEL = "gemini-1.5-flash";

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(15);

    // Optional override provider for hermetic automated unit tests
    private static Function<String, String> testEnvProvider = null;

    private AIConfiguration() {
        // Utility class
    }

    /**
     * Resolves an environment variable, using the test provider if configured.
     */
    private static String getEnv(String variableName) {
        if (testEnvProvider != null) {
            return testEnvProvider.apply(variableName);
        }
        try {
            return System.getenv(variableName);
        } catch (SecurityException ignored) {
            return null;
        }
    }

    /**
     * Checks if the Gemini API key is available in the environment.
     * Never prints or logs the key contents.
     */
    public static boolean isApiKeyAvailable() {
        String key = getEnv(ENV_API_KEY);
        return key != null && !key.trim().isEmpty();
    }

    /**
     * Retrieves the API key for direct secure HTTP header assignment.
     * Note: Must NEVER be printed, logged, or displayed.
     */
    public static String getApiKey() {
        String key = getEnv(ENV_API_KEY);
        return key != null ? key.trim() : null;
    }

    /**
     * Resolves the configured model name or defaults to gemini-1.5-flash.
     */
    public static String getModelName() {
        String model = getEnv(ENV_MODEL_NAME);
        if (model != null && !model.trim().isEmpty()) {
            return model.trim();
        }
        return DEFAULT_MODEL;
    }

    public static Duration getConnectTimeout() {
        return DEFAULT_CONNECT_TIMEOUT;
    }

    public static Duration getRequestTimeout() {
        return DEFAULT_REQUEST_TIMEOUT;
    }

    /**
     * Sets a test-specific environment provider for automated unit testing.
     * Allows tests to simulate key presence, absence, or model overrides without mutating OS env.
     */
    public static synchronized void setTestEnvironment(Function<String, String> provider) {
        testEnvProvider = provider;
    }

    /**
     * Convenience method for setting a test API key.
     */
    public static synchronized void setTestApiKey(String key) {
        if (key == null) {
            testEnvProvider = name -> null;
        } else {
            testEnvProvider = name -> {
                if (ENV_API_KEY.equals(name)) return key;
                if (ENV_MODEL_NAME.equals(name)) return DEFAULT_MODEL;
                return null;
            };
        }
    }

    /**
     * Resets any test environment override back to system environment variables.
     */
    public static synchronized void resetTestEnvironment() {
        testEnvProvider = null;
    }
}
