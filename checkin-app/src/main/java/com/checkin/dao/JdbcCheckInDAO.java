package com.checkin.dao;

import com.checkin.database.DatabaseConnection;
import com.checkin.model.CheckInRecord;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * JDBC MySQL implementation of CheckInDAO using PreparedStatements.
 * Joins with analysis_results to seamlessly reconstruct complete CheckInRecord entities.
 */
public class JdbcCheckInDAO implements CheckInDAO {

    private final DatabaseConnection db;

    public JdbcCheckInDAO() {
        this(DatabaseConnection.getInstance());
    }

    public JdbcCheckInDAO(DatabaseConnection db) {
        this.db = db;
    }

    @Override
    public boolean save(CheckInRecord record) {
        if (record == null || record.id() == null) return false;

        String mode = determineMode(record);
        String sql = "INSERT INTO check_ins (id, user_id, has_photo, has_text, photo_file_name, thoughts_text, analysis_mode, created_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, record.id());
            stmt.setString(2, record.userId());
            stmt.setBoolean(3, record.hasPhoto());
            stmt.setBoolean(4, record.hasThoughts());
            stmt.setString(5, record.photoFileName());
            stmt.setString(6, record.thoughtsText());
            stmt.setString(7, mode);
            stmt.setTimestamp(8, record.createdAt() != null ? Timestamp.valueOf(record.createdAt()) : new Timestamp(System.currentTimeMillis()));

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[JdbcCheckInDAO] Error saving check-in: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<CheckInRecord> findByUserId(String userId) {
        if (userId == null || userId.isBlank()) return Collections.emptyList();

        String sql = "SELECT c.id, c.user_id, c.photo_file_name, c.thoughts_text, c.created_at, "
                   + "       r.photo_signal, r.confidence, r.text_signal, r.combined_signal "
                   + "FROM check_ins c "
                   + "LEFT JOIN analysis_results r ON c.id = r.check_in_id "
                   + "WHERE c.user_id = ? "
                   + "ORDER BY c.created_at DESC";

        List<CheckInRecord> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    LocalDateTime created = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

                    CheckInRecord record = new CheckInRecord(
                            rs.getString("id"),
                            rs.getString("user_id"),
                            rs.getString("photo_file_name"),
                            rs.getString("thoughts_text"),
                            rs.getString("photo_signal"),
                            rs.getDouble("confidence"),
                            rs.getString("text_signal"),
                            rs.getDouble("confidence"),
                            rs.getString("combined_signal"),
                            rs.getDouble("confidence"),
                            created
                    );
                    list.add(record);
                }
            }
        } catch (SQLException e) {
            System.err.println("[JdbcCheckInDAO] Error querying check-ins by userId: " + e.getMessage());
        }
        return list;
    }

    @Override
    public Optional<CheckInRecord> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();

        String sql = "SELECT c.id, c.user_id, c.photo_file_name, c.thoughts_text, c.created_at, "
                   + "       r.photo_signal, r.confidence, r.text_signal, r.combined_signal "
                   + "FROM check_ins c "
                   + "LEFT JOIN analysis_results r ON c.id = r.check_in_id "
                   + "WHERE c.id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    LocalDateTime created = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

                    CheckInRecord record = new CheckInRecord(
                            rs.getString("id"),
                            rs.getString("user_id"),
                            rs.getString("photo_file_name"),
                            rs.getString("thoughts_text"),
                            rs.getString("photo_signal"),
                            rs.getDouble("confidence"),
                            rs.getString("text_signal"),
                            rs.getDouble("confidence"),
                            rs.getString("combined_signal"),
                            rs.getDouble("confidence"),
                            created
                    );
                    return Optional.of(record);
                }
            }
        } catch (SQLException e) {
            System.err.println("[JdbcCheckInDAO] Error querying check-in by id: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public int countByUserId(String userId) {
        if (userId == null || userId.isBlank()) return 0;

        String sql = "SELECT COUNT(*) FROM check_ins WHERE user_id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[JdbcCheckInDAO] Error counting check-ins: " + e.getMessage());
        }
        return 0;
    }

    private String determineMode(CheckInRecord record) {
        if (record.hasPhoto() && record.hasThoughts()) {
            return "MULTIMODAL";
        } else if (record.hasPhoto()) {
            return "PHOTO";
        } else {
            return "TEXT";
        }
    }
}
