package com.checkin.preprocessing;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Preprocessing pipeline for input textual thoughts.
 *
 * Pipeline:
 * Raw String -> Length/Null Validation -> Cleaning & Normalization -> Tokenization -> Model Ready Representation
 */
public class TextPreprocessor {

    public static final int MAX_TEXT_LENGTH = 1000;
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    public record PreprocessedText(
            String rawText,
            String cleanedText,
            List<String> tokens,
            int characterCount,
            int wordCount
    ) {}

    public PreprocessedText process(String input) throws IllegalArgumentException {
        validate(input);

        String trimmed = input.trim();
        String normalized = WHITESPACE_PATTERN.matcher(trimmed).replaceAll(" ");
        String lower = normalized.toLowerCase();

        // Basic tokenization
        String[] rawTokens = lower.split("[^a-zA-Z0-9']");
        List<String> tokens = new ArrayList<>();
        for (String t : rawTokens) {
            if (!t.isBlank()) {
                tokens.add(t);
            }
        }

        return new PreprocessedText(
                input,
                normalized,
                tokens,
                trimmed.length(),
                tokens.size()
        );
    }

    public void validate(String input) throws IllegalArgumentException {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Thoughts text cannot be empty.");
        }
        if (input.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("Thoughts text exceeds maximum length of " + MAX_TEXT_LENGTH + " characters.");
        }
    }
}
