package com.checkin.dao;

import com.checkin.database.DatabaseConnection;
import com.checkin.model.User;

import java.sql.*;
import java.util.Optional;

/**
 * JDBC MySQL implementation of UserDAO using parameterized PreparedStatements.
 */
public class JdbcUserDAO implements UserDAO {

    private final DatabaseConnection db;

    public JdbcUserDAO() {
        this(DatabaseConnection.getInstance());
    }

    public JdbcUserDAO(DatabaseConnection db) {
        this.db = db;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();

        String sql = "SELECT id, full_name, email, password_hash, created_at FROM users WHERE LOWER(email) = LOWER(?)";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    User user = new User(
                            rs.getString("id"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("password_hash"),
                            ts != null ? ts.toLocalDateTime() : null
                    );
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            System.err.println("[JdbcUserDAO] Error querying user by email: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) return false;

        String sql = "SELECT 1 FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[JdbcUserDAO] Error checking email existence: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean save(User user) {
        if (user == null || user.getId() == null || user.getEmail().isBlank()) return false;

        String sql = "INSERT INTO users (id, full_name, email, password_hash, created_at) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getId());
            stmt.setString(2, user.getFullName());
            stmt.setString(3, user.getEmail().trim().toLowerCase());
            stmt.setString(4, user.getPassword()); // Stored as cryptographic hash
            stmt.setTimestamp(5, user.getCreatedAt() != null ? Timestamp.valueOf(user.getCreatedAt()) : new Timestamp(System.currentTimeMillis()));

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[JdbcUserDAO] Error saving user: " + e.getMessage());
            return false;
        }
    }
}
