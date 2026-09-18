package com.checkin.dao;

import com.checkin.model.AnalysisResultRecord;
import java.util.Optional;

/**
 * Data Access Object interface for AnalysisResult persistence.
 */
public interface AnalysisResultDAO {
    boolean save(AnalysisResultRecord result);
    Optional<AnalysisResultRecord> findByCheckInId(String checkInId);
    Optional<AnalysisResultRecord> findById(String id);

    // Phase 4 named aliases
    default boolean saveAnalysisResult(AnalysisResultRecord result) {
        return save(result);
    }

    default Optional<AnalysisResultRecord> findResultByCheckInId(String checkInId) {
        return findByCheckInId(checkInId);
    }
}
