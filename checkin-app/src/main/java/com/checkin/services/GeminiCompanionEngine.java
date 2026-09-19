package com.checkin.services;

import com.checkin.config.AIConfiguration;
import com.checkin.model.AnalysisResult;
import com.checkin.model.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Production-ready CompanionEngine calling the official Google Gemini API over HTTPS.
 *
 * Adheres to EmoSense guidelines:
 * - Employs java.net.http.HttpClient with configurable timeouts
 * - Uses API key from EMOSENSE_AI_API_KEY via AIConfiguration
 * - Never prints, logs, or leaks API credentials
 * - Sends privacy-preserving minimal JSON payload (no images, no DB records, no raw tensors)
 * - Structures empathetic, non-clinical supportive prompt instructions
 * - Bounded conversation history to prevent unbounded payload growth
 * - Jackson-based response parsing and structured error classification
 */
public class GeminiCompanionEngine implements AICompanionService.CompanionEngine {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final int MAX_HISTORY_MESSAGES = 8;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String apiKey;
    private final String modelName;
    private final HttpClient httpClient;
    private final Duration requestTimeout;

    public GeminiCompanionEngine() {
        this(AIConfiguration.getApiKey(), AIConfiguration.getModelName(), null, AIConfiguration.getRequestTimeout());
    }

    public GeminiCompanionEngine(String apiKey) {
        this(apiKey, AIConfiguration.getModelName(), null, AIConfiguration.getRequestTimeout());
    }

    public GeminiCompanionEngine(String apiKey, String modelName) {
        this(apiKey, modelName, null, AIConfiguration.getRequestTimeout());
    }

    public GeminiCompanionEngine(String apiKey, String modelName, HttpClient httpClient) {
        this(apiKey, modelName, httpClient, AIConfiguration.getRequestTimeout());
    }

    public GeminiCompanionEngine(String apiKey, String modelName, HttpClient httpClient, Duration requestTimeout) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Gemini API key cannot be null or blank.");
        }
        this.apiKey = apiKey.trim();
        this.modelName = (modelName != null && !modelName.isBlank()) ? modelName.trim() : AIConfiguration.DEFAULT_MODEL;
        this.requestTimeout = requestTimeout != null ? requestTimeout : AIConfiguration.getRequestTimeout();
        this.httpClient = httpClient != null ? httpClient : HttpClient.newBuilder()
                .connectTimeout(AIConfiguration.getConnectTimeout())
                .build();
    }

    public String getModelName() {
        return modelName;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public String generateReply(String userMessage, List<ChatMessage> history, AnalysisResult context) throws Exception {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return null;
        }

        String requestBody = buildPayloadJson(userMessage.trim(), history, context);
        URI endpoint = URI.create(GEMINI_BASE_URL + modelName + ":generateContent");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(endpoint)
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new CompanionApiException("Gemini API request timed out after " + requestTimeout.toSeconds() + "s", 408, e);
        } catch (IOException e) {
            throw new CompanionApiException("Gemini network communication failure", 0, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompanionApiException("Gemini API request was interrupted", 0, e);
        }

        int statusCode = response.statusCode();
        String responseBody = response.body();

        if (statusCode >= 200 && statusCode < 300) {
            return parseSuccessResponse(responseBody);
        } else {
            handleErrorResponse(statusCode, responseBody);
            return null; // unreachable due to exception
        }
    }

    /**
     * Builds a minimal, privacy-respecting Gemini JSON request payload.
     * Never sends photos, embeddings, tensor shapes, or database identifiers.
     */
    public String buildPayloadJson(String currentUserMessage, List<ChatMessage> history, AnalysisResult context) throws Exception {
        ObjectNode rootNode = MAPPER.createObjectNode();

        // 1. System Instruction
        ObjectNode systemInstruction = rootNode.putObject("system_instruction");
        ArrayNode systemParts = systemInstruction.putArray("parts");
        systemParts.addObject().put("text", buildSystemPrompt(context));

        // 2. Multi-turn conversation contents
        ArrayNode contentsArray = rootNode.putArray("contents");
        List<ChatMessage> boundedHistory = filterAndBoundHistory(history, currentUserMessage);

        for (ChatMessage msg : boundedHistory) {
            ObjectNode contentObj = contentsArray.addObject();
            contentObj.put("role", msg.isUser() ? "user" : "model");
            ArrayNode parts = contentObj.putArray("parts");
            parts.addObject().put("text", msg.text());
        }

        // 3. Generation configuration
        ObjectNode genConfig = rootNode.putObject("generationConfig");
        genConfig.put("temperature", 0.7);
        genConfig.put("maxOutputTokens", 350);

        return MAPPER.writeValueAsString(rootNode);
    }

    /**
     * Forms the strict supportive guidelines and optional non-clinical check-in context.
     */
    private String buildSystemPrompt(AnalysisResult context) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are EmoSense AI Companion, a warm, conversational, non-clinical companion for emotional reflection and well-being.\n\n");
        sb.append("Your Core Principles:\n");
        sb.append("1. DIRECT SPECIFICITY & ACTIVE LISTENING:\n");
        sb.append("   - Directly address the specific details of the user's message (e.g., specific exams, workload, project milestones, interpersonal conflicts).\n");
        sb.append("   - Never give a vague, one-size-fits-all supportive response. Address what the user actually said.\n");
        sb.append("   - If the user discusses academic or work pressure, offer practical, manageable steps (e.g., picking one focused topic for 20 minutes, prioritizing the most urgent task, or stepping away for a short reset).\n");
        sb.append("2. AVOID GENERIC REPETITION & CLICHÉS:\n");
        sb.append("   - DO NOT repeatedly start with 'I'm sorry you're feeling this way...' or 'It sounds like you're...'.\n");
        sb.append("   - DO NOT reflexively tell the user to 'Take a deep breath' or say 'It's okay to feel this way' for every message.\n");
        sb.append("   - DO NOT repeatedly conclude with 'You're not alone.' Vary your closing or ask a relevant, focused question.\n");
        sb.append("   - Avoid toxic or empty positivity (e.g., 'Everything happens for a reason'); acknowledge disappointment or effort mismatches honestly.\n");
        sb.append("3. CONVERSATIONAL & PRACTICAL TONE:\n");
        sb.append("   - Sound like a thoughtful, empathetic peer or mentor, NOT a robotic script or clinical assessment.\n");
        sb.append("   - Keep responses concise, natural, and engaging (typically 2 to 4 sentences).\n");
        sb.append("   - Ask a meaningful follow-up question when appropriate to help the user unpack their situation.\n");
        sb.append("4. STRICT SAFETY & NON-CLINICAL BOUNDARIES:\n");
        sb.append("   - NEVER diagnose clinical depression, anxiety disorders, bipolar, or any medical/psychiatric condition.\n");
        sb.append("   - NEVER claim medical authority or prescribe medications or therapies.\n");
        sb.append("   - Never pretend to be a doctor, psychiatrist, or licensed medical professional.\n");

        if (context != null && (context.hasFacial() || context.hasText() || context.getCombinedLabel() != null)) {
            sb.append("\nActive Session Check-In Context (use naturally with uncertainty-aware phrasing; do NOT treat as definitive clinical fact):\n");
            if (context.hasFacial() && context.getFacialLabel() != null) {
                sb.append("• Facial signal: ").append(context.getFacialLabel());
                if (context.getFacialConfidence() > 0) {
                    sb.append(String.format(" (%.0f%% confidence)", context.getFacialConfidence()));
                }
                sb.append("\n");
            }
            if (context.hasText() && context.getTextLabel() != null) {
                sb.append("• Textual cue: ").append(context.getTextLabel());
                if (context.getTextConfidence() > 0) {
                    sb.append(String.format(" (%.0f%% confidence)", context.getTextConfidence()));
                }
                sb.append("\n");
            }
            if (context.getCombinedLabel() != null && !context.getCombinedLabel().isBlank()) {
                sb.append("• Multimodal insight: ").append(context.getCombinedLabel());
                if (context.getCombinedConfidence() > 0) {
                    sb.append(String.format(" (%.0f%% confidence)", context.getCombinedConfidence()));
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Extracts a bounded, valid alternating turn history ending with the current user message.
     */
    private List<ChatMessage> filterAndBoundHistory(List<ChatMessage> history, String currentUserMessage) {
        List<ChatMessage> candidateList = new ArrayList<>();
        if (history != null) {
            for (ChatMessage msg : history) {
                if (msg != null && (msg.isUser() || msg.isCompanion()) && msg.text() != null && !msg.text().isBlank()) {
                    candidateList.add(msg);
                }
            }
        }

        // If current user message is not already the last entry in candidateList, add it
        if (candidateList.isEmpty() || !candidateList.get(candidateList.size() - 1).text().equals(currentUserMessage)) {
            candidateList.add(ChatMessage.user(currentUserMessage));
        }

        // Bound to the most recent MAX_HISTORY_MESSAGES
        if (candidateList.size() > MAX_HISTORY_MESSAGES) {
            candidateList = candidateList.subList(candidateList.size() - MAX_HISTORY_MESSAGES, candidateList.size());
        }

        // Gemini requires that conversation contents start with role: 'user'
        while (!candidateList.isEmpty() && !candidateList.get(0).isUser()) {
            candidateList.remove(0);
        }

        if (candidateList.isEmpty()) {
            candidateList.add(ChatMessage.user(currentUserMessage));
        }

        return candidateList;
    }

    /**
     * Parses the assistant text from Gemini's generateContent JSON response.
     */
    public String parseSuccessResponse(String json) {
        if (json == null || json.isBlank()) {
            throw new CompanionApiException("Gemini returned an empty response body", 200);
        }

        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new CompanionApiException("No candidate responses found in Gemini response", 200);
            }

            JsonNode firstCandidate = candidates.get(0);
            JsonNode parts = firstCandidate.path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                throw new CompanionApiException("No text parts found in Gemini candidate", 200);
            }

            StringBuilder fullReply = new StringBuilder();
            for (JsonNode part : parts) {
                String text = part.path("text").asText("");
                if (!text.isBlank()) {
                    fullReply.append(text);
                }
            }

            String result = fullReply.toString().trim();
            if (result.isEmpty()) {
                throw new CompanionApiException("Gemini returned blank response text", 200);
            }
            return result;
        } catch (CompanionApiException e) {
            throw e;
        } catch (Exception e) {
            throw new CompanionApiException("Malformed JSON structure in Gemini response", 200, e);
        }
    }

    /**
     * Formulates sanitized exceptions for various HTTP status codes without exposing credentials.
     */
    private void handleErrorResponse(int statusCode, String body) {
        String baseMessage;
        switch (statusCode) {
            case 400 -> baseMessage = "Bad Request to Gemini API";
            case 401, 403 -> baseMessage = "Authentication or permission error on Gemini API (HTTP " + statusCode + ")";
            case 429 -> baseMessage = "Gemini API rate limit exceeded (HTTP 429)";
            case 500, 502, 503, 504 -> baseMessage = "Gemini API service temporarily unavailable (HTTP " + statusCode + ")";
            default -> baseMessage = "Gemini API returned unexpected HTTP status: " + statusCode;
        }
        throw new CompanionApiException(baseMessage, statusCode);
    }

    @Override
    public String toString() {
        // Redacts API key unconditionally
        return "GeminiCompanionEngine[model=" + modelName + ", timeout=" + requestTimeout.toSeconds() + "s]";
    }
}
