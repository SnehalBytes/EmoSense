package com.checkin.dao;

import com.checkin.model.User;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory user repository for prototype testing and offline fallback.
 * Starts completely empty with zero hardcoded/pre-seeded demo credentials.
 */
public class InMemoryUserDAO implements UserDAO {

    private final Map<String, User> usersByEmail = new ConcurrentHashMap<>();

    public InMemoryUserDAO() {
        // Starts completely empty; no pre-seeded demo credentials in production
    }

    /**
     * Helper for test suites to pre-seed users deterministically if needed.
     */
    public void seedUser(User user) {
        if (user != null && user.getEmail() != null) {
            usersByEmail.put(user.getEmail().trim().toLowerCase(), user);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return Optional.ofNullable(usersByEmail.get(email.trim().toLowerCase()));
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null) return false;
        return usersByEmail.containsKey(email.trim().toLowerCase());
    }

    @Override
    public boolean save(User user) {
        if (user == null || user.getEmail().isBlank()) return false;
        usersByEmail.put(user.getEmail().trim().toLowerCase(), user);
        return true;
    }
}
