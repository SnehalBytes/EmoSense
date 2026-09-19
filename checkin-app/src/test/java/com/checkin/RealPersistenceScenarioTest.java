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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Executes the EXACT Task 4 lifecycle verification scenario:
 * A. Start EmoSense.
 * B. Create a new account.
 * C. Close the application completely.
 * D. Start EmoSense again.
 * E. Sign in using the same account.
 * F. Confirm that the account still exists after restart.
 * G. Create a check-in.
 * H. Close and reopen the application.
 * I. Confirm that History still contains the previous check-in.
 * J. Confirm that analysis result data is persisted.
 */
public class RealPersistenceScenarioTest {

    private static final String TEST_EMAIL = "marcus.vance_" + System.currentTimeMillis() + "@emosense.org";
    private static final String TEST_PASSWORD = "Pass1234#Secure";
    private static final String TEST_NAME = "Dr. Marcus Vance";
    private static String createdUserId = null;

    @BeforeAll
    static void init() {
        DatabaseConnection db = DatabaseConnection.getInstance();
        assertTrue(db.testConnection(), "MySQL must be running for Task 4 real persistence verification");
        DatabaseInitializer.initialize(db);
    }

    @Test
    @DisplayName("Task 4: Full lifecycle persistence across application restarts")
    void testTask4FullLifecyclePersistence() {
        // A. Start EmoSense (Fresh Service instance connected to live MySQL)
        AuthenticationService appInstance1Auth = new AuthenticationService();
        assertTrue(appInstance1Auth.isPersistentStorageAvailable(), "Storage must be persistent MySQL");

        // B. Create a new account
        var regResult = appInstance1Auth.signUp(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_PASSWORD);
        assertTrue(regResult.success(), "Registration should succeed: " + regResult.message());
        assertNotNull(regResult.user());
        createdUserId = regResult.user().getId();
        assertTrue(regResult.message().contains("Account created successfully"));

        // C. Close the application completely
        appInstance1Auth.signOut();
        appInstance1Auth = null;
        UserSession.getInstance().logout();
        assertNull(UserSession.getInstance().getCurrentUser());

        // D. Start EmoSense again (Fresh Service and DAO instances simulating brand-new application process)
        AuthenticationService appInstance2Auth = new AuthenticationService();
        assertTrue(appInstance2Auth.isPersistentStorageAvailable());

        // E. Sign in using the same account
        var loginResult = appInstance2Auth.signIn(TEST_EMAIL, TEST_PASSWORD);

        // F. Confirm that the account still exists after restart
        assertTrue(loginResult.success(), "Account must be found in MySQL after restart");
        assertNotNull(loginResult.user());
        assertEquals(createdUserId, loginResult.user().getId());
        assertEquals(TEST_NAME, loginResult.user().getFullName());
        assertTrue(appInstance2Auth.isAuthenticated());

        // G. Create a check-in
        CheckInPersistenceService persistenceService = new CheckInPersistenceService();
        String checkInId = "checkin-" + UUID.randomUUID();
        String resultId = "result-" + UUID.randomUUID();

        CheckInRecord checkIn = new CheckInRecord(
                checkInId, createdUserId, "marcus_selfie.png", "Had a productive research session today.",
                "Focused", 88.5, "Productive", 84.0, "Attentive / Positive", 86.25, LocalDateTime.now()
        );
        AnalysisResultRecord result = new AnalysisResultRecord(
                resultId, checkInId, "Attentive / Positive", 86.25,
                "Focused", "Productive", "Attentive / Positive", "Aligned Signals", "REAL"
        );

        boolean saved = persistenceService.saveCheckInAndResult(checkIn, result);
        assertTrue(saved, "Check-in and analysis result must be successfully saved into MySQL");

        // H. Close and reopen the application
        appInstance2Auth.signOut();
        appInstance2Auth = null;
        persistenceService = null;
        UserSession.getInstance().logout();

        // Simulate reopen
        AuthenticationService appInstance3Auth = new AuthenticationService();
        var reLogin = appInstance3Auth.signIn(TEST_EMAIL, TEST_PASSWORD);
        assertTrue(reLogin.success());

        HistoryService historyService = new HistoryService();

        // I. Confirm that History still contains the previous check-in
        List<CheckInRecord> userHistory = historyService.getUserHistory(createdUserId);
        assertFalse(userHistory.isEmpty(), "User history must be retrieved from MySQL");
        Optional<CheckInRecord> foundCheckIn = userHistory.stream().filter(r -> r.id().equals(checkInId)).findFirst();
        assertTrue(foundCheckIn.isPresent(), "Previous check-in must be present in history");
        assertEquals("Had a productive research session today.", foundCheckIn.get().thoughtsText());
        assertEquals("marcus_selfie.png", foundCheckIn.get().photoFileName());

        // J. Confirm that analysis result data is persisted
        JdbcAnalysisResultDAO resultDAO = new JdbcAnalysisResultDAO();
        Optional<AnalysisResultRecord> persistedResult = resultDAO.findByCheckInId(checkInId);
        assertTrue(persistedResult.isPresent(), "Analysis result must be persisted in MySQL analysis_results table");
        assertEquals("Attentive / Positive", persistedResult.get().primarySignal());
        assertEquals(86.25, persistedResult.get().confidence());
        System.out.println("==================================================");
        System.out.println("[TASK 4 PERSISTENCE VERIFIED IN MYSQL]");
        System.out.println("USER ID: " + createdUserId);
        System.out.println("USER NAME: " + loginResult.user().getFullName());
        System.out.println("USER EMAIL: " + loginResult.user().getEmail());
        System.out.println("CHECK-IN ID: " + foundCheckIn.get().id());
        System.out.println("THOUGHTS: " + foundCheckIn.get().thoughtsText());
        System.out.println("PRIMARY SIGNAL: " + persistedResult.get().primarySignal());
        System.out.println("CONFIDENCE: " + persistedResult.get().confidence() + "%");
        System.out.println("MODEL SOURCE: " + persistedResult.get().modelSource());
        System.out.println("==================================================");
    }

    @AfterAll
    static void cleanup() {
        if (createdUserId == null) return;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
            stmt.setString(1, createdUserId);
            stmt.executeUpdate();
        } catch (Exception ignored) {}
        UserSession.getInstance().logout();
    }
}
