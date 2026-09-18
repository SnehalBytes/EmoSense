package com.checkin.dao;

import com.checkin.model.AnalysisResultRecord;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory implementation of AnalysisResultDAO.
 * Complements InMemoryCheckInDAO for testing and offline environments.
 */
public class InMemoryAnalysisResultDAO implements AnalysisResultDAO {

    private final Map<String, AnalysisResultRecord> recordsById = new ConcurrentHashMap<>();
    private final Map<String, AnalysisResultRecord> recordsByCheckInId = new ConcurrentHashMap<>();

    @Override
    public boolean save(AnalysisResultRecord result) {
        if (result == null || result.id() == null) return false;
        recordsById.put(result.id(), result);
        if (result.checkInId() != null) {
            recordsByCheckInId.put(result.checkInId(), result);
        }
        return true;
    }

    @Override
    public Optional<AnalysisResultRecord> findByCheckInId(String checkInId) {
        if (checkInId == null) return Optional.empty();
        return Optional.ofNullable(recordsByCheckInId.get(checkInId));
    }

    @Override
    public Optional<AnalysisResultRecord> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(recordsById.get(id));
    }

    public void clear() {
        recordsById.clear();
        recordsByCheckInId.clear();
    }
}
