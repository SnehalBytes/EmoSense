package com.checkin.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Entity representing an individual message in an AI Companion conversation.
 */
public record ChatMessage(
        Sender sender,
        String text,
        LocalDateTime timestamp
) {
    public enum Sender {
        USER,
        COMPANION,
        SYSTEM
    }

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    public ChatMessage(Sender sender, String text) {
        this(sender, text, LocalDateTime.now());
    }

    public static ChatMessage user(String text) {
        return new ChatMessage(Sender.USER, text);
    }

    public static ChatMessage companion(String text) {
        return new ChatMessage(Sender.COMPANION, text);
    }

    public static ChatMessage system(String text) {
        return new ChatMessage(Sender.SYSTEM, text);
    }

    public boolean isUser() {
        return sender == Sender.USER;
    }

    public boolean isCompanion() {
        return sender == Sender.COMPANION;
    }

    public boolean isSystem() {
        return sender == Sender.SYSTEM;
    }

    public String getFormattedTime() {
        return timestamp != null ? timestamp.format(TIME_FORMATTER) : "";
    }
}
