package com.checkin;

import com.checkin.dao.JdbcAnalysisResultDAO;
import com.checkin.dao.JdbcCheckInDAO;
import com.checkin.dao.JdbcUserDAO;
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
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dedicated Integration Test Suite for Real MySQL Database Persistence.
 * 
 * IMPORTANT: This test suite specifically targets a live MySQL database.
 * If MySQL is NOT installed or running on localhost:3306, the tests will
 * be skipped with an explicit note, rather than falsely claiming live persistence passed.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MySqlPersistenceIntegrationTest {

    private static DatabaseConnection db;
    private static boolean isMySqlAvailable = false;
    private static final String TEST_EMAIL = "integration_test_" + System.currentTimeMillis() + "@emosense.local";
    private static String createdUserId = null;

    @BeforeAll
    static void checkMySqlAvailability() {
        try {
            db = DatabaseConnection.getInstance();
            isMySqlAvailable = db.testConnection();
            if (isMySqlAvailable) {
                DatabaseInitializer.initialize(db);
            }
        } catch (Exception e) {
            isMySqlAvailable = false;
        }
    }

    @BeforeEach
    void requireLiveMySql() {
        Assumptions.assumeTrue(isMySqlAvailable,
                "MySQL Server is not installed/running on this Windows machine - live MySQL integration test skipped.");
    }

    @Test
    @Order(1)
    @DisplayName("Live MySQL: Schema and table initialization")
    void testLiveDatabaseInitialization() {
        boolean initialized = DatabaseInitializer.initialize(db);
        assertTrue(initialized, "DatabaseInitializer should succeed on active MySQL connection");
    }

    @Test
    @Order(2)
    @DisplayName("Live MySQL: User sign-up and BCrypt hash persistence in MySQL users table")
    void testLiveUserSignUpPersistence() {
        JdbcUserDAO userDao = new JdbcUserDAO(db);
        AuthenticationService auth = new AuthenticationService(userDao);

        var regResult = auth.signUp("Integration Tester", TEST_EMAIL, "StrongPassword!2026", "StrongPassword!2026");
        assertTrue(regResult.success());
        assertNotNull(regResult.user());
        createdUserId = regResult.user().getId();

        // Direct DB verification: user exists and password is a BCrypt hash, never plaintext
        Optional<User> queried = userDao.findByEmail(TEST_EMAIL);
        assertTrue(queried.isPresent());
        assertEquals(TEST_EMAIL, queried.get().getEmail());
        assertNotEquals("StrongPassword!2026", queried.get().getPassword());
        assertTrue(PasswordHasher.verify("StrongPassword!2026", queried.get().getPassword()));
    }

    @Test
    @Order(3)
    @DisplayName("Live MySQL: User lookup and sign-in across fresh service instance (simulating restart)")
    void testLiveUserSignInAcrossRestart() {
        // Create a completely new DAO and Service instance to simulate application restart
        JdbcUserDAO restartedDao = new JdbcUserDAO(db);
        AuthenticationService restartedAuth = new AuthenticationService(restartedDao);

        var loginResult = restartedAuth.signIn(TEST_EMAIL, "StrongPassword!2026");
        assertTrue(loginResult.success(), "Account must persist in MySQL across application restart");
        assertNotNull(loginResult.user());
        assertEquals("Integration Tester", loginResult.user().getFullName());
    }

    @Test
    @Order(4)
    @DisplayName("Live MySQL: Check-in and Analysis-Result transactional persistence")
    void testLiveCheckInAndAnalysisResultPersistence() {
        assertNotNull(createdUserId, "User ID from prior test required");

        CheckInPersistenceService persistenceService = new CheckInPersistenceService(db);
        String checkInId = UUID.randomUUID().toString();
        String resultId = UUID.randomUUID().toString();

        CheckInRecord checkIn = new CheckInRecord(
                checkInId, createdUserId, "live_selfie.png", "Live integration check-in",
                "Focused", 85.0, "Attentive", 80.0, "Balanced Focus", 82.5, LocalDateTime.now()
        );

        AnalysisResultRecord result = new AnalysisResultRecord(
                resultId, checkInId, "Balanced Focus", 82.5,
                "Focused", "Attentive", "Balanced Focus", "Aligned Signals", "REAL"
        );

        boolean saved = persistenceService.saveCheckInAndResult(checkIn, result);
        assertTrue(saved, "Check-in and result should save into MySQL");

        // Verify retrieval via HistoryService
        HistoryService historyService = new HistoryService(new JdbcCheckInDAO(db), new JdbcAnalysisResultDAO(db));
        UserSession.getInstance().login(new User(createdUserId, "Integration Tester", TEST_EMAIL, "hash"));

        List<CheckInRecord> history = historyService.getUserHistory(createdUserId);
        assertFalse(history.isEmpty());
        assertTrue(history.stream().anyMatch(r -> r.id().equals(checkInId)));
    }

    @AfterAll
    static void cleanupTestData() {
        if (!isMySqlAvailable || createdUserId == null || db == null) return;
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
            stmt.setString(1, createdUserId);
            stmt.executeUpdate();
        } catch (Exception ignored) {
            // Best-effort test cleanup
        }
        UserSession.getInstance().logout();
    }
}
