package com.checkin.dao;

import com.checkin.model.User;
import java.util.Optional;

/**
 * Data Access Object interface for user persistence.
 * Structured so that a JDBC MySQL implementation can replace
 * the prototype in-memory store without changing UI or service layers.
 */
public interface UserDAO {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean save(User user);

    // Phase 4 named aliases
    default boolean createUser(User user) {
        return save(user);
    }

    default Optional<User> findUserByEmail(String email) {
        return findByEmail(email);
    }

    default boolean emailExists(String email) {
        return existsByEmail(email);
    }
}
