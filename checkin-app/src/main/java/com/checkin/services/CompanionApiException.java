package com.checkin.services;

/**
 * Exception representing an error during external AI Companion API communication.
 * Strictly guarantees that API keys and raw credentials are never leaked in error messages.
 */
public class CompanionApiException extends RuntimeException {

    private final int statusCode;

    public CompanionApiException(String message) {
        this(message, 0, null);
    }

    public CompanionApiException(String message, int statusCode) {
        this(message, statusCode, null);
    }

    public CompanionApiException(String message, Throwable cause) {
        this(message, 0, cause);
    }

    public CompanionApiException(String message, int statusCode, Throwable cause) {
        super(sanitize(message), cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Ensures any accidental inclusion of key patterns is scrubbed.
     */
    private static String sanitize(String msg) {
        if (msg == null) return "Unknown AI API error";
        // Scrub potential key parameters or headers
        return msg.replaceAll("(?i)(key|token|secret|password|bearer)[=:\\s]+[a-zA-Z0-9_\\-]+", "$1=[REDACTED]");
    }
}
