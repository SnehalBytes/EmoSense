package com.checkin.dao;

import com.checkin.model.User;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory user repository for prototype testing.
 * Pre-seeded with a default demo user: test@emosense.com / password123.
 */
public class InMemoryUserDAO implements UserDAO {

    private final Map<String, User> usersByEmail = new ConcurrentHashMap<>();

    public InMemoryUserDAO() {
        // Seed default demo user with cryptographically hashed password
        User demoUser = new User(
                UUID.randomUUID().toString(),
                "Alex Rivera",
                "test@emosense.com",
                com.checkin.utils.PasswordHasher.hash("password123")
        );
        usersByEmail.put(demoUser.getEmail(), demoUser);
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
