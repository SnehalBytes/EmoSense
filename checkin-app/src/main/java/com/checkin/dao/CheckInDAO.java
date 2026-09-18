package com.checkin.dao;

import com.checkin.model.CheckInRecord;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for user check-in history.
 * Structured so that a JDBC MySQL implementation can replace
 * the prototype in-memory store without changing UI or service layers.
 */
public interface CheckInDAO {
    boolean save(CheckInRecord record);
    List<CheckInRecord> findByUserId(String userId);
    Optional<CheckInRecord> findById(String id);
    int countByUserId(String userId);

    // Phase 4 named aliases
    default boolean saveCheckIn(CheckInRecord record) {
        return save(record);
    }

    default List<CheckInRecord> findCheckInsByUser(String userId) {
        return findByUserId(userId);
    }

    default Optional<CheckInRecord> findCheckInById(String id) {
        return findById(id);
    }
}
