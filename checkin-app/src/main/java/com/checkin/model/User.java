package com.checkin.model;

import java.time.LocalDateTime;

/**
 * Represents a registered user in EmoSense.
 */
public class User {
    private final String id;
    private final String fullName;
    private final String email;
    private final String password; // In a production setup, store hashed password
    private final LocalDateTime createdAt;

    public User(String id, String fullName, String email, String password) {
        this(id, fullName, email, password, LocalDateTime.now());
    }

    public User(String id, String fullName, String email, String password, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email != null ? email.trim().toLowerCase() : "";
        this.password = password;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
