package com.checkin.services;

import com.checkin.database.DatabaseConnection;
import com.checkin.model.AnalysisResult;
import com.checkin.model.AnalysisResultRecord;
import com.checkin.model.CheckInData;
import com.checkin.model.CheckInRecord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service orchestrating transactional persistence of CheckIns and AnalysisResults.
 * Ensures atomicity using JDBC commit and rollback.
 */
public class CheckInPersistenceService {

    private final DatabaseConnection db;

    public CheckInPersistenceService() {
        this(DatabaseConnection.getInstance());
    }

    public CheckInPersistenceService(DatabaseConnection db) {
        this.db = db;
    }

    private static final com.checkin.dao.InMemoryCheckInDAO SHARED_IN_MEMORY_CHECK_IN_DAO = new com.checkin.dao.InMemoryCheckInDAO();
    private static final com.checkin.dao.InMemoryAnalysisResultDAO SHARED_IN_MEMORY_RESULT_DAO = new com.checkin.dao.InMemoryAnalysisResultDAO();

    public static com.checkin.dao.InMemoryCheckInDAO getSharedInMemoryCheckInDAO() {
        return SHARED_IN_MEMORY_CHECK_IN_DAO;
    }

    public static com.checkin.dao.InMemoryAnalysisResultDAO getSharedInMemoryResultDAO() {
        return SHARED_IN_MEMORY_RESULT_DAO;
    }

    /**
     * Atomically stores a CheckIn and its associated AnalysisResult in MySQL if available,
     * with graceful fallback to the shared in-memory store.
     */
    public boolean saveCheckInAndResult(CheckInRecord checkIn, AnalysisResultRecord result) {
        if (checkIn == null || result == null) return false;

        // Always keep shared in-memory store synchronized
        SHARED_IN_MEMORY_CHECK_IN_DAO.save(checkIn);
        SHARED_IN_MEMORY_RESULT_DAO.save(result);

        if (db == null || !db.testConnection()) {
            // MySQL is offline; successfully saved to in-memory session store
            return true;
        }

        String insertCheckInSql = "INSERT INTO check_ins (id, user_id, has_photo, has_text, photo_file_name, thoughts_text, analysis_mode, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        String insertResultSql = "INSERT INTO analysis_results (id, check_in_id, primary_signal, confidence, "
                + "photo_signal, text_signal, combined_signal, signal_agreement, model_source, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = db.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Insert CheckIn
            try (PreparedStatement checkInStmt = conn.prepareStatement(insertCheckInSql)) {
                checkInStmt.setString(1, checkIn.id());
                checkInStmt.setString(2, checkIn.userId());
                checkInStmt.setBoolean(3, checkIn.hasPhoto());
                checkInStmt.setBoolean(4, checkIn.hasThoughts());
                checkInStmt.setString(5, checkIn.photoFileName());
                checkInStmt.setString(6, checkIn.thoughtsText());
                checkInStmt.setString(7, determineMode(checkIn));
                checkInStmt.setTimestamp(8, checkIn.createdAt() != null ? Timestamp.valueOf(checkIn.createdAt()) : new Timestamp(System.currentTimeMillis()));

                checkInStmt.executeUpdate();
            }

            // 2. Insert AnalysisResult
            try (PreparedStatement resultStmt = conn.prepareStatement(insertResultSql)) {
                resultStmt.setString(1, result.id());
                resultStmt.setString(2, checkIn.id());
                resultStmt.setString(3, result.primarySignal());
                resultStmt.setDouble(4, result.confidence());
                resultStmt.setString(5, result.photoSignal());
                resultStmt.setString(6, result.textSignal());
                resultStmt.setString(7, result.combinedSignal());
                resultStmt.setString(8, result.signalAgreement());
                resultStmt.setString(9, result.modelSource() != null ? result.modelSource() : "REAL");
                resultStmt.setTimestamp(10, result.createdAt() != null ? Timestamp.valueOf(result.createdAt()) : new Timestamp(System.currentTimeMillis()));

                resultStmt.executeUpdate();
            }

            conn.commit(); // Atomic commit
            return true;

        } catch (SQLException e) {
            System.err.println("[CheckInPersistenceService] MySQL write failed, falling back to memory: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("[CheckInPersistenceService] Rollback error: " + rollbackEx.getMessage());
                }
            }
            return true; // Still true because in-memory store was updated
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    // Ignore close exception
                }
            }
        }
    }

    /**
     * Helper to create and persist entities directly from CheckInData and AnalysisResult.
     */
    public boolean persistCheckIn(CheckInData data, AnalysisResult result, String userId) {
        if (data == null || result == null || userId == null) return false;

        String checkInId = UUID.randomUUID().toString();
        String resultId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        String photoName = data.hasPhoto() ? data.getPhotoFile().getName() : null;
        String thoughts = data.hasThoughts() ? data.getThoughtsText() : null;

        CheckInRecord checkInRecord = new CheckInRecord(
                checkInId,
                userId,
                photoName,
                thoughts,
                result.getFacialLabel(),
                result.getFacialConfidence(),
                result.getTextLabel(),
                result.getTextConfidence(),
                result.getCombinedLabel(),
                result.getCombinedConfidence(),
                now
        );

        String primarySignal = result.getCombinedLabel();
        double primaryConfidence = result.getCombinedConfidence();
        if (primarySignal == null || primarySignal.isBlank()) {
            if (result.hasFacial()) {
                primarySignal = result.getFacialLabel();
                primaryConfidence = result.getFacialConfidence();
            } else if (result.hasText()) {
                primarySignal = result.getTextLabel();
                primaryConfidence = result.getTextConfidence();
            } else {
                primarySignal = "No Signal";
            }
        }

        String modelSource;
        if ("Analysis unavailable".equalsIgnoreCase(primarySignal)) {
            modelSource = "UNAVAILABLE";
        } else if (com.checkin.ml.ModelManager.getInstance().isMockMode()) {
            modelSource = "MOCK";
        } else if (com.checkin.ml.ModelManager.getInstance().isFacialModelAvailable() || com.checkin.ml.ModelManager.getInstance().isTextModelAvailable()) {
            modelSource = "REAL";
        } else {
            modelSource = "UNAVAILABLE";
        }

        StringBuilder probBuilder = new StringBuilder();
        if (result.getFacialProbabilities() != null && !result.getFacialProbabilities().isEmpty()) {
            probBuilder.append("FACIAL=");
            result.getFacialProbabilities().forEach((k, v) -> probBuilder.append(k).append(":").append(v).append(","));
        }
        if (result.getTextProbabilities() != null && !result.getTextProbabilities().isEmpty()) {
            if (probBuilder.length() > 0) probBuilder.append("|");
            probBuilder.append("TEXT=");
            result.getTextProbabilities().forEach((k, v) -> probBuilder.append(k).append(":").append(v).append(","));
        }

        String modality = "UNKNOWN";
        if (result.hasFacial() && result.hasText()) {
            modality = "MULTIMODAL";
        } else if (result.hasFacial()) {
            modality = "FACIAL";
        } else if (result.hasText()) {
            modality = "TEXTUAL";
        }

        AnalysisResultRecord resultRecord = new AnalysisResultRecord(
                resultId,
                checkInId,
                primarySignal,
                primaryConfidence,
                result.getFacialLabel(),
                result.getTextLabel(),
                result.getCombinedLabel(),
                "Agreement Analysis",
                modelSource,
                modality,
                probBuilder.length() > 0 ? probBuilder.toString() : null,
                now
        );

        return saveCheckInAndResult(checkInRecord, resultRecord);
    }

    public static String determineMode(CheckInRecord record) {
        if (record.hasPhoto() && record.hasThoughts()) {
            return "MULTIMODAL";
        } else if (record.hasPhoto()) {
            return "PHOTO";
        } else {
            return "TEXT";
        }
    }
}
