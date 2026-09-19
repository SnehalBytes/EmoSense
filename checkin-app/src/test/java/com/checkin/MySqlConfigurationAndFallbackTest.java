package com.checkin;

import com.checkin.dao.*;
import com.checkin.database.DatabaseConfig;
import com.checkin.database.DatabaseConnection;
import com.checkin.database.DatabaseInitializer;
import com.checkin.model.AnalysisResultRecord;
import com.checkin.model.CheckInRecord;
import com.checkin.model.User;
import com.checkin.services.AuthenticationService;
import com.checkin.services.CheckInPersistenceService;
import com.checkin.services.HistoryService;
import com.checkin.services.UserSession;
import com.checkin.utils.PasswordHasher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests verifying MySQL configuration, schema initialization logic,
 * authentication security, and fallback behaviors for EmoSense.
 */
public class MySqlConfigurationAndFallbackTest {

    @BeforeEach
    void setUp() {
        UserSession.getInstance().logout();
    }

    @AfterEach
    void tearDown() {
        UserSession.getInstance().logout();
    }

    // 1. MySQL Configuration
    @Test
    @DisplayName("1. MySQL Configuration defaults to localhost:3306 and database emosense")
    void testMySqlConfigurationDefaults() {
        DatabaseConfig config = new DatabaseConfig();
        assertEquals("localhost", config.getHost());
        assertEquals("3306", config.getPort());
        assertEquals("emosense", config.getDatabaseName());
        assertTrue(config.getUrl().contains("localhost:3306/emosense"));
        assertTrue(config.getServerUrl().contains("localhost:3306/"));
        assertFalse(config.getServerUrl().contains("localhost:3306/emosense"));

        // Password masking
        String safeRepresentation = config.toSafeString();
        assertTrue(safeRepresentation.contains("password=******"));
        assertFalse(safeRepresentation.contains(config.getPassword().isEmpty() ? "___NOMATCH___" : config.getPassword()));
    }

    @Test
    @DisplayName("1b. MySQL Configuration supports custom overrides and environment variables")
    void testMySqlConfigurationCustomParameters() {
        DatabaseConfig customConfig = new DatabaseConfig("jdbc:mysql://remotehost:3307/custom_db", "admin", "secret_pass");
        assertEquals("jdbc:mysql://remotehost:3307/custom_db", customConfig.getUrl());
        assertEquals("admin", customConfig.getUser());
        assertEquals("secret_pass", customConfig.getPassword());
        assertTrue(customConfig.toSafeString().contains("password=******"));
        assertFalse(customConfig.toSafeString().contains("secret_pass"));
    }

    // 2. Database Initialization Logic
    @Test
    @DisplayName("2. Database initialization handles null connection and server unavailability safely")
    void testDatabaseInitializationSafelyHandlesOfflineState() {
        assertFalse(DatabaseInitializer.initialize(null));
        assertFalse(DatabaseInitializer.createDatabaseIfNotExists(null));

        // When DB connection fails, testConnection() is false and initialize() returns false without throwing
        DatabaseConnection conn = new DatabaseConnection(new DatabaseConfig("jdbc:mysql://127.0.0.1:9999/dummy", "u", "p"));
        assertFalse(conn.testConnection());
        assertFalse(DatabaseInitializer.initialize(conn));
    }

    // 3. User Creation
    @Test
    @DisplayName("3. User entity creation with valid UUID, name, email, and password hash")
    void testUserCreation() {
        String id = UUID.randomUUID().toString();
        String hash = PasswordHasher.hash("Pass@1234");
        User user = new User(id, "Dr. John Doe", "john.doe@example.com", hash);

        assertEquals(id, user.getId());
        assertEquals("Dr. John Doe", user.getFullName());
        assertEquals("john.doe@example.com", user.getEmail());
        assertEquals(hash, user.getPassword());
        assertNotNull(user.getCreatedAt());
    }

    // 4. Secure Password Hashing
    @Test
    @DisplayName("4. Cryptographic password hashing generates unique salts and verifies correctly")
    void testPasswordHashing() {
        String rawPassword = "SecurePassword#2026";
        String hash1 = PasswordHasher.hash(rawPassword);
        String hash2 = PasswordHasher.hash(rawPassword);

        // Salt uniqueness: different hash for the same raw password
        assertNotEquals(hash1, hash2);
        assertTrue(hash1.contains("$"));

        // Verifications
        assertTrue(PasswordHasher.verify(rawPassword, hash1));
        assertTrue(PasswordHasher.verify(rawPassword, hash2));
        assertFalse(PasswordHasher.verify("WrongPassword", hash1));
        assertFalse(PasswordHasher.verify("", hash1));
        assertFalse(PasswordHasher.verify(null, hash1));
    }

    // 5. User Lookup
    @Test
    @DisplayName("5. User lookup by email handles case-insensitivity and missing users")
    void testUserLookup() {
        InMemoryUserDAO dao = new InMemoryUserDAO();
        String hash = PasswordHasher.hash("mypassword");
        User user = new User(UUID.randomUUID().toString(), "Alice Wonder", "alice@domain.org", hash);
        dao.save(user);

        // Exact match
        Optional<User> foundExact = dao.findByEmail("alice@domain.org");
        assertTrue(foundExact.isPresent());
        assertEquals("Alice Wonder", foundExact.get().getFullName());

        // Case-insensitive lookup
        Optional<User> foundUpper = dao.findByEmail("ALICE@DOMAIN.ORG");
        assertTrue(foundUpper.isPresent());
        assertEquals(user.getId(), foundUpper.get().getId());

        // Non-existent user
        Optional<User> missing = dao.findByEmail("nobody@domain.org");
        assertTrue(missing.isEmpty());
        assertFalse(dao.existsByEmail("nobody@domain.org"));
        assertTrue(dao.existsByEmail("Alice@Domain.Org"));
    }

    // 6. Sign-in Authentication
    @Test
    @DisplayName("6. Sign-in authentication checks credentials and manages user session")
    void testSignInAuthentication() {
        InMemoryUserDAO dao = new InMemoryUserDAO();
        AuthenticationService auth = new AuthenticationService(dao);

        auth.signUp("Bob Dylan", "bob@example.com", "folkrock1965", "folkrock1965");

        // Successful sign-in
        var resSuccess = auth.signIn("bob@example.com", "folkrock1965");
        assertTrue(resSuccess.success());
        assertNotNull(resSuccess.user());
        assertEquals("Bob Dylan", resSuccess.user().getFullName());
        assertTrue(auth.isAuthenticated());
        assertEquals("bob@example.com", UserSession.getInstance().getCurrentUser().getEmail());

        // Sign in with invalid password
        var resWrong = auth.signIn("bob@example.com", "wrongpassword");
        assertFalse(resWrong.success());
        assertEquals("Incorrect password. Please try again.", resWrong.message());

        // Sign in with unknown user
        var resUnknown = auth.signIn("unknown@example.com", "folkrock1965");
        assertFalse(resUnknown.success());
        assertTrue(resUnknown.message().contains("Account not found"));
    }

    // 7. Duplicate Account Handling
    @Test
    @DisplayName("7. Duplicate account registration is rejected cleanly")
    void testDuplicateAccountHandling() {
        InMemoryUserDAO dao = new InMemoryUserDAO();
        AuthenticationService auth = new AuthenticationService(dao);

        var firstReg = auth.signUp("Original User", "unique@example.com", "password123", "password123");
        assertTrue(firstReg.success());

        var duplicateReg = auth.signUp("Impostor User", "unique@example.com", "password456", "password456");
        assertFalse(duplicateReg.success());
        assertEquals("An account with this email already exists. Please sign in.", duplicateReg.message());
    }

    // 8. MySQL Unavailable Behavior
    @Test
    @DisplayName("8. MySQL unavailable behavior produces clean offline indication without crashing")
    void testMySqlUnavailableBehavior() {
        // Pointing to a closed port on loopback
        DatabaseConfig offlineConfig = new DatabaseConfig("jdbc:mysql://localhost:33069/nonexistent", "root", "");
        DatabaseConnection offlineConn = new DatabaseConnection(offlineConfig);

        assertFalse(offlineConn.testConnection());

        // DAO operating against offline connection handles error gracefully
        JdbcUserDAO offlineDao = new JdbcUserDAO(offlineConn);
        assertFalse(offlineDao.existsByEmail("test@test.com"));
        assertTrue(offlineDao.findByEmail("test@test.com").isEmpty());
        assertFalse(offlineDao.save(new User("id", "Name", "email@test.com", "hash")));
    }

    // 9. In-Memory Fallback Behavior
    @Test
    @DisplayName("9. In-memory fallback allows session testing but warns accounts will not persist")
    void testInMemoryFallbackBehavior() {
        InMemoryUserDAO fallbackDao = new InMemoryUserDAO();
        AuthenticationService auth = new AuthenticationService(fallbackDao);

        assertFalse(auth.isPersistentStorageAvailable());

        var regResult = auth.signUp("Fallback Tester", "fallback@session.local", "sessionPass123", "sessionPass123");
        assertTrue(regResult.success());
        // Must clearly state temporary / session-only storage
        assertTrue(regResult.message().contains("will not persist") || regResult.message().contains("in-memory"));

        // New application restart simulation: fresh DAO is completely empty
        InMemoryUserDAO restartedDao = new InMemoryUserDAO();
        AuthenticationService restartedAuth = new AuthenticationService(restartedDao);
        var loginAfterRestart = restartedAuth.signIn("fallback@session.local", "sessionPass123");
        assertFalse(loginAfterRestart.success(), "In-memory account should NOT persist after application restart");
    }

    // 10. User Isolation
    @Test
    @DisplayName("10. User check-ins and history are strictly isolated between accounts")
    void testUserIsolation() {
        InMemoryCheckInDAO checkInDAO = new InMemoryCheckInDAO();
        InMemoryAnalysisResultDAO resultDAO = new InMemoryAnalysisResultDAO();
        HistoryService historyService = new HistoryService(checkInDAO, resultDAO);

        String userAlice = "user-alice-001";
        String userBob = "user-bob-002";

        CheckInRecord aliceCheckIn = new CheckInRecord(
                "checkin-alice-1", userAlice, "alice_photo.jpg", "Feeling great today!",
                "Happy", 85.0, "Joy", 80.0, "Positive / Uplifted Signal", 82.5, LocalDateTime.now().minusHours(2)
        );

        CheckInRecord bobCheckIn = new CheckInRecord(
                "checkin-bob-1", userBob, null, "A quiet contemplative afternoon",
                null, 0.0, "Calm", 75.0, "Calm", 75.0, LocalDateTime.now().minusHours(1)
        );

        checkInDAO.save(aliceCheckIn);
        checkInDAO.save(bobCheckIn);

        // Logged in as Alice
        UserSession.getInstance().login(new User(userAlice, "Alice", "alice@example.com", "hash"));
        List<CheckInRecord> aliceHistory = historyService.getUserHistory(userAlice);
        assertEquals(1, aliceHistory.size());
        assertEquals("checkin-alice-1", aliceHistory.get(0).id());

        // Alice attempting to query Bob's data directly is rejected
        List<CheckInRecord> alicePeekingBob = historyService.getUserHistory(userBob);
        assertTrue(alicePeekingBob.isEmpty(), "Cross-user data access must be denied");
    }

    // 11. Check-In Persistence
    @Test
    @DisplayName("11. Check-in records preserve photo, thoughts, and mode")
    void testCheckInPersistence() {
        InMemoryCheckInDAO dao = new InMemoryCheckInDAO();
        CheckInRecord record = new CheckInRecord(
                "rec-123", "user-xyz", "selfie_test.png", "Writing unit tests for EmoSense",
                "Focused", 88.0, "Neutral", 70.0, "Balanced Emotional State", 79.0, LocalDateTime.now()
        );

        assertTrue(dao.save(record));
        Optional<CheckInRecord> fetched = dao.findById("rec-123");
        assertTrue(fetched.isPresent());
        assertEquals("user-xyz", fetched.get().userId());
        assertEquals("selfie_test.png", fetched.get().photoFileName());
        assertEquals("Writing unit tests for EmoSense", fetched.get().thoughtsText());
        assertEquals("MULTIMODAL", CheckInPersistenceService.determineMode(record));
    }

    // 12. Analysis Result Persistence
    @Test
    @DisplayName("12. Analysis result records preserve signal data and model source")
    void testAnalysisResultPersistence() {
        InMemoryAnalysisResultDAO dao = new InMemoryAnalysisResultDAO();
        AnalysisResultRecord result = new AnalysisResultRecord(
                "res-456", "rec-123", "Positive / Uplifted Signal", 89.5,
                "Joyful Expression", "Optimistic Words", "Positive / Uplifted Signal",
                "High Agreement", "REAL"
        );

        assertTrue(dao.save(result));
        Optional<AnalysisResultRecord> fetched = dao.findByCheckInId("rec-123");
        assertTrue(fetched.isPresent());
        assertEquals("Positive / Uplifted Signal", fetched.get().primarySignal());
        assertEquals(89.5, fetched.get().confidence());
        assertEquals("REAL", fetched.get().modelSource());
    }
}
