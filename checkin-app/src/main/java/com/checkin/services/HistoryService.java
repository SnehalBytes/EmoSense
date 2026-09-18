package com.checkin.services;

import com.checkin.dao.*;
import com.checkin.database.DatabaseConnection;
import com.checkin.model.AnalysisResult;
import com.checkin.model.AnalysisResultRecord;
import com.checkin.model.CheckInRecord;

import java.util.*;

/**
 * Service orchestrating retrieval and management of emotional check-in history.
 * Enforces user isolation, sorts records by newest first, and reconstructs
 * historical results directly from stored database values without running inference.
 */
public class HistoryService {

    private final CheckInDAO checkInDAO;
    private final AnalysisResultDAO resultDAO;

    public HistoryService() {
        this(createDefaultCheckInDAO(), createDefaultResultDAO());
    }

    public HistoryService(CheckInDAO checkInDAO, AnalysisResultDAO resultDAO) {
        this.checkInDAO = checkInDAO != null ? checkInDAO : CheckInPersistenceService.getSharedInMemoryCheckInDAO();
        this.resultDAO = resultDAO != null ? resultDAO : CheckInPersistenceService.getSharedInMemoryResultDAO();
    }

    private static CheckInDAO createDefaultCheckInDAO() {
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            if (db.testConnection()) {
                return new JdbcCheckInDAO(db);
            }
        } catch (Exception ignored) {}
        return CheckInPersistenceService.getSharedInMemoryCheckInDAO();
    }

    private static AnalysisResultDAO createDefaultResultDAO() {
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            if (db.testConnection()) {
                return new JdbcAnalysisResultDAO(db);
            }
        } catch (Exception ignored) {}
        return CheckInPersistenceService.getSharedInMemoryResultDAO();
    }

    /**
     * Retrieves the check-in history for the specified user, sorted newest first.
     * Enforces strict user isolation.
     *
     * @param userId ID of the authenticated user
     * @return List of CheckInRecord instances sorted by newest first
     * @throws HistoryAccessException if database retrieval fails
     */
    public List<CheckInRecord> getUserHistory(String userId) {
        if (userId == null || userId.isBlank()) {
            return Collections.emptyList();
        }

        // Strict User Isolation: ensure requesting user matches active session if session exists
        String activeUserId = UserSession.getInstance().getCurrentUserId();
        if (activeUserId != null && !activeUserId.equals(userId)) {
            System.err.println("[HistoryService] Security Warning: Attempted history access across user boundary. Requested: "
                    + userId + ", Active: " + activeUserId);
            return Collections.emptyList();
        }

        try {
            List<CheckInRecord> records = checkInDAO.findByUserId(userId);
            if (records == null) {
                return Collections.emptyList();
            }

            // Guarantee descending chronological order (newest first)
            List<CheckInRecord> sorted = new ArrayList<>(records);
            sorted.sort((a, b) -> {
                if (a.createdAt() == null && b.createdAt() == null) return 0;
                if (a.createdAt() == null) return 1;
                if (b.createdAt() == null) return -1;
                return b.createdAt().compareTo(a.createdAt());
            });
            return Collections.unmodifiableList(sorted);

        } catch (Exception e) {
            System.err.println("[HistoryService] Database failure retrieving user history: " + e.getMessage());
            throw new HistoryAccessException("Unable to load check-in history.", e);
        }
    }

    /**
     * Retrieves the stored AnalysisResult for a given check-in ID without re-running models.
     *
     * @param checkInId ID of the check-in
     * @return Optional containing the reconstructed AnalysisResult from stored data
     */
    public Optional<AnalysisResult> getStoredResult(String checkInId) {
        if (checkInId == null || checkInId.isBlank()) {
            return Optional.empty();
        }

        try {
            Optional<AnalysisResultRecord> resultOpt = resultDAO.findByCheckInId(checkInId);
            if (resultOpt.isPresent()) {
                return Optional.of(resultOpt.get().toAnalysisResult());
            }

            // Fallback reconstruction from check-in record if analysis_results record missing
            Optional<CheckInRecord> checkInOpt = checkInDAO.findById(checkInId);
            if (checkInOpt.isPresent()) {
                CheckInRecord r = checkInOpt.get();
                return Optional.of(new AnalysisResult(
                        r.facialLabel(),
                        r.facialConfidence(),
                        r.textLabel(),
                        r.textConfidence(),
                        r.combinedLabel(),
                        r.combinedConfidence()
                ));
            }
        } catch (Exception e) {
            System.err.println("[HistoryService] Error retrieving stored result: " + e.getMessage());
            throw new HistoryAccessException("Unable to load stored analysis result.", e);
        }

        return Optional.empty();
    }

    public CheckInDAO getCheckInDAO() {
        return checkInDAO;
    }

    public AnalysisResultDAO getResultDAO() {
        return resultDAO;
    }

    /**
     * Unchecked exception indicating an error occurred during history database operations.
     */
    public static class HistoryAccessException extends RuntimeException {
        public HistoryAccessException(String message) {
            super(message);
        }
        public HistoryAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
