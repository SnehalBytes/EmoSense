package com.checkin;

import com.checkin.dao.*;
import com.checkin.database.DatabaseConfig;
import com.checkin.model.AnalysisResult;
import com.checkin.model.AnalysisResultRecord;
import com.checkin.model.CheckInRecord;
import com.checkin.model.User;
import com.checkin.services.CheckInPersistenceService;
import com.checkin.services.InsightsService;
import com.checkin.services.UserSession;
import com.checkin.utils.PasswordHasher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseAndPersistenceTest {

    @BeforeEach
    void setUp() {
        UserSession.getInstance().logout();
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    @Test
    void testPasswordHasher() {
        String rawPassword = "MySecretPassword!123";

        String hash1 = PasswordHasher.hash(rawPassword);
        String hash2 = PasswordHasher.hash(rawPassword);

        assertNotNull(hash1);
        assertNotNull(hash2);
        // Salt uniqueness ensures two hashes of identical password are mathematically different
        assertNotEquals(hash1, hash2);

        // Verification
        assertTrue(PasswordHasher.verify(rawPassword, hash1));
        assertTrue(PasswordHasher.verify(rawPassword, hash2));
        assertFalse(PasswordHasher.verify("WrongPassword", hash1));
        assertFalse(PasswordHasher.verify("", hash1));
        assertFalse(PasswordHasher.verify(null, hash1));
    }

    @Test
    void testUserSessionIsolation() {
        User userA = new User("user-a-123", "Alice Smith", "alice@example.com", "hashA");
        User userB = new User("user-b-456", "Bob Jones", "bob@example.com", "hashB");

        UserSession session = UserSession.getInstance();
        assertFalse(session.isLoggedIn());
        assertNull(session.getCurrentUserId());

        session.login(userA);
        assertTrue(session.isLoggedIn());
        assertEquals("user-a-123", session.getCurrentUserId());
        assertEquals("Alice Smith", session.getCurrentUser().getFullName());

        session.login(userB);
        assertEquals("user-b-456", session.getCurrentUserId());
        assertEquals("Bob Jones", session.getCurrentUser().getFullName());

        session.logout();
        assertFalse(session.isLoggedIn());
        assertNull(session.getCurrentUserId());
    }

    @Test
    void testDatabaseConfigSafety() {
        DatabaseConfig config = new DatabaseConfig("jdbc:mysql://localhost:3306/emosense_db", "test_user", "super_secret_password");
        assertEquals("jdbc:mysql://localhost:3306/emosense_db", config.getUrl());
        assertEquals("test_user", config.getUser());
        assertEquals("super_secret_password", config.getPassword());

        String safe = config.toSafeString();
        assertTrue(safe.contains("******"));
        assertFalse(safe.contains("super_secret_password"));
    }

    @Test
    void testCheckInModeDetermination() {
        LocalDateTime now = LocalDateTime.now();

        // Photo-only
        CheckInRecord photoRecord = new CheckInRecord("id1", "u1", "selfie.jpg", null,
                "Happy", 75.0, null, 0.0, "Happy", 75.0, now);
        assertEquals("PHOTO", CheckInPersistenceService.determineMode(photoRecord));

        // Text-only
        CheckInRecord textRecord = new CheckInRecord("id2", "u1", null, "I had a great day today",
                null, 0.0, "Joy", 70.0, "Joy", 70.0, now);
        assertEquals("TEXT", CheckInPersistenceService.determineMode(textRecord));

        // Multimodal
        CheckInRecord multiRecord = new CheckInRecord("id3", "u1", "selfie.jpg", "Feeling calm and peaceful",
                "Calm", 80.0, "Calm", 75.0, "Aligned Signals", 78.0, now);
        assertEquals("MULTIMODAL", CheckInPersistenceService.determineMode(multiRecord));
    }

    @Test
    void testCheckInHistoryUserIsolation() {
        CheckInDAO dao = new InMemoryCheckInDAO();
        LocalDateTime now = LocalDateTime.now();

        String userA = "user-A-" + UUID.randomUUID();
        String userB = "user-B-" + UUID.randomUUID();

        CheckInRecord recA1 = new CheckInRecord(UUID.randomUUID().toString(), userA, "face1.jpg", "Thoughts A1",
                "Happy", 80.0, "Joy", 75.0, "Positive / Uplifted Signal", 78.0, now.minusMinutes(10));
        CheckInRecord recA2 = new CheckInRecord(UUID.randomUUID().toString(), userA, null, "Thoughts A2",
                null, 0.0, "Calm", 70.0, "Calm", 70.0, now);
        CheckInRecord recB1 = new CheckInRecord(UUID.randomUUID().toString(), userB, "faceB.jpg", null,
                "Sad", 65.0, null, 0.0, "Sad", 65.0, now);

        dao.save(recA1);
        dao.save(recA2);
        dao.save(recB1);

        // Verify User A only sees User A's records
        List<CheckInRecord> recordsA = dao.findByUserId(userA);
        assertEquals(2, recordsA.size());
        assertTrue(recordsA.stream().allMatch(r -> r.userId().equals(userA)));

        // Verify User B only sees User B's records
        List<CheckInRecord> recordsB = dao.findByUserId(userB);
        assertEquals(1, recordsB.size());
        assertEquals(userB, recordsB.get(0).userId());
        assertFalse(recordsB.stream().anyMatch(r -> r.userId().equals(userA)));

        assertEquals(2, dao.countByUserId(userA));
        assertEquals(1, dao.countByUserId(userB));
    }

    @Test
    void testAnalysisResultRecordConversion() {
        String checkInId = UUID.randomUUID().toString();
        AnalysisResultRecord record = new AnalysisResultRecord(
                UUID.randomUUID().toString(),
                checkInId,
                "Positive / Uplifted Signal",
                77.5,
                "Happy Expression",
                "Joyful Cue",
                "Positive / Uplifted Signal",
                "Aligned Signals",
                "MOCK"
        );

        assertEquals("MOCK", record.modelSource());
        assertEquals(checkInId, record.checkInId());

        AnalysisResult result = record.toAnalysisResult();
        assertEquals("Happy Expression", result.getFacialLabel());
        assertEquals("Joyful Cue", result.getTextLabel());
        assertEquals("Positive / Uplifted Signal", result.getCombinedLabel());
        assertEquals(77.5, result.getCombinedConfidence());
    }

    @Test
    void testSchemaSqlFileExistsAndContainsTables() throws Exception {
        File schemaFile = new File("database/schema.sql");
        if (!schemaFile.exists()) {
            schemaFile = new File("checkin-app/database/schema.sql");
        }

        assertTrue(schemaFile.exists(), "schema.sql should exist in database/ folder");
        String content = Files.readString(schemaFile.toPath());

        assertTrue(content.contains("CREATE DATABASE IF NOT EXISTS emosense_db"));
        assertTrue(content.contains("CREATE TABLE IF NOT EXISTS users"));
        assertTrue(content.contains("CREATE TABLE IF NOT EXISTS check_ins"));
        assertTrue(content.contains("CREATE TABLE IF NOT EXISTS analysis_results"));
        assertTrue(content.contains("FOREIGN KEY (user_id) REFERENCES users(id)"));
        assertTrue(content.contains("FOREIGN KEY (check_in_id) REFERENCES check_ins(id)"));
        assertTrue(content.contains("password_hash"));
        assertTrue(content.contains("analysis_mode"));
        assertTrue(content.contains("model_source"));
    }
}
