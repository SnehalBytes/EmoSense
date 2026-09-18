package com.checkin.dao;

import com.checkin.database.DatabaseConnection;
import com.checkin.model.AnalysisResultRecord;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * JDBC MySQL implementation of AnalysisResultDAO using PreparedStatements.
 */
public class JdbcAnalysisResultDAO implements AnalysisResultDAO {

    private final DatabaseConnection db;

    public JdbcAnalysisResultDAO() {
        this(DatabaseConnection.getInstance());
    }

    public JdbcAnalysisResultDAO(DatabaseConnection db) {
        this.db = db;
    }

    @Override
    public boolean save(AnalysisResultRecord result) {
        if (result == null || result.id() == null || result.checkInId() == null) return false;

        String sql = "INSERT INTO analysis_results (id, check_in_id, primary_signal, confidence, "
                   + "photo_signal, text_signal, combined_signal, signal_agreement, model_source, created_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, result.id());
            stmt.setString(2, result.checkInId());
            stmt.setString(3, result.primarySignal());
            stmt.setDouble(4, result.confidence());
            stmt.setString(5, result.photoSignal());
            stmt.setString(6, result.textSignal());
            stmt.setString(7, result.combinedSignal());
            stmt.setString(8, result.signalAgreement());
            stmt.setString(9, result.modelSource() != null ? result.modelSource() : "MOCK");
            stmt.setTimestamp(10, result.createdAt() != null ? Timestamp.valueOf(result.createdAt()) : new Timestamp(System.currentTimeMillis()));

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[JdbcAnalysisResultDAO] Error saving analysis result: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<AnalysisResultRecord> findByCheckInId(String checkInId) {
        if (checkInId == null || checkInId.isBlank()) return Optional.empty();

        String sql = "SELECT id, check_in_id, primary_signal, confidence, photo_signal, text_signal, "
                   + "combined_signal, signal_agreement, model_source, created_at "
                   + "FROM analysis_results WHERE check_in_id = ? ORDER BY created_at DESC LIMIT 1";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, checkInId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    LocalDateTime created = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

                    AnalysisResultRecord record = new AnalysisResultRecord(
                            rs.getString("id"),
                            rs.getString("check_in_id"),
                            rs.getString("primary_signal"),
                            rs.getDouble("confidence"),
                            rs.getString("photo_signal"),
                            rs.getString("text_signal"),
                            rs.getString("combined_signal"),
                            rs.getString("signal_agreement"),
                            rs.getString("model_source"),
                            created
                    );
                    return Optional.of(record);
                }
            }
        } catch (SQLException e) {
            System.err.println("[JdbcAnalysisResultDAO] Error querying result by checkInId: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<AnalysisResultRecord> findById(String id) {
        if (id == null || id.isBlank()) return Optional.empty();

        String sql = "SELECT id, check_in_id, primary_signal, confidence, photo_signal, text_signal, "
                   + "combined_signal, signal_agreement, model_source, created_at "
                   + "FROM analysis_results WHERE id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("created_at");
                    LocalDateTime created = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

                    AnalysisResultRecord record = new AnalysisResultRecord(
                            rs.getString("id"),
                            rs.getString("check_in_id"),
                            rs.getString("primary_signal"),
                            rs.getDouble("confidence"),
                            rs.getString("photo_signal"),
                            rs.getString("text_signal"),
                            rs.getString("combined_signal"),
                            rs.getString("signal_agreement"),
                            rs.getString("model_source"),
                            created
                    );
                    return Optional.of(record);
                }
            }
        } catch (SQLException e) {
            System.err.println("[JdbcAnalysisResultDAO] Error querying result by id: " + e.getMessage());
        }
        return Optional.empty();
    }
}
