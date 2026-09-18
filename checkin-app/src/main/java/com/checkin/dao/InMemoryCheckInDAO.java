package com.checkin.dao;

import com.checkin.model.CheckInRecord;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe in-memory implementation of CheckInDAO.
 */
public class InMemoryCheckInDAO implements CheckInDAO {

    private final Map<String, CheckInRecord> recordsById = new ConcurrentHashMap<>();

    @Override
    public boolean save(CheckInRecord record) {
        if (record == null || record.id() == null) return false;
        recordsById.put(record.id(), record);
        return true;
    }

    @Override
    public List<CheckInRecord> findByUserId(String userId) {
        if (userId == null) return Collections.emptyList();
        return recordsById.values().stream()
                .filter(r -> userId.equals(r.userId()))
                .sorted(Comparator.comparing(CheckInRecord::createdAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public Optional<CheckInRecord> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(recordsById.get(id));
    }

    @Override
    public int countByUserId(String userId) {
        if (userId == null) return 0;
        return (int) recordsById.values().stream()
                .filter(r -> userId.equals(r.userId()))
                .count();
    }
}
