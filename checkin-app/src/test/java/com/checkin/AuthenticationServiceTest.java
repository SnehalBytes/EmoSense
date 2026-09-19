package com.checkin;

import com.checkin.dao.InMemoryUserDAO;
import com.checkin.dao.UserDAO;
import com.checkin.model.User;
import com.checkin.services.AuthenticationService;
import com.checkin.utils.Validators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AuthenticationServiceTest {

    private InMemoryUserDAO userDAO;
    private AuthenticationService authService;

    @BeforeEach
    void setUp() {
        userDAO = new InMemoryUserDAO();
        authService = new AuthenticationService(userDAO);
    }

    @Test
    @DisplayName("Email validation checks RFC formatting")
    void testEmailValidation() {
        assertTrue(Validators.isValidEmail("user@example.com"));
        assertTrue(Validators.isValidEmail("name.surname@domain.co.uk"));
        assertFalse(Validators.isValidEmail("invalid-email"));
        assertFalse(Validators.isValidEmail("user@"));
        assertFalse(Validators.isValidEmail("@domain.com"));
        assertFalse(Validators.isValidEmail(""));
        assertFalse(Validators.isValidEmail(null));
    }

    @Test
    @DisplayName("No pre-seeded demo user: repository starts empty by default")
    void testNoPreSeededUserCanSignInByDefault() {
        var result = authService.signIn("test@emosense.com", "password123");
        assertFalse(result.success());
        assertNull(result.user());
        assertEquals("Account not found. Please check your email or create an account.", result.message());
        assertFalse(authService.isAuthenticated());
    }

    @Test
    @DisplayName("Sign in with empty fields prompts clear validation errors")
    void testSignInWithEmptyFields() {
        var res1 = authService.signIn("", "password123");
        assertFalse(res1.success());
        assertEquals("Please enter your email address.", res1.message());

        var res2 = authService.signIn("test@emosense.com", "");
        assertFalse(res2.success());
        assertEquals("Please enter your password.", res2.message());
    }

    @Test
    @DisplayName("Sign in with incorrect password produces specific message")
    void testSignInWithWrongPassword() {
        // Register user first
        authService.signUp("Test User", "test@emosense.com", "password123", "password123");

        var res = authService.signIn("test@emosense.com", "wrongPass");
        assertFalse(res.success());
        assertEquals("Incorrect password. Please try again.", res.message());
    }

    @Test
    @DisplayName("Sign in with nonexistent user produces account not found message")
    void testSignInWithNonexistentUser() {
        var res = authService.signIn("nonexistent@emosense.com", "password123");
        assertFalse(res.success());
        assertEquals("Account not found. Please check your email or create an account.", res.message());
    }

    @Test
    @DisplayName("Sign up validation prevents invalid names, emails, and passwords")
    void testSignUpValidation() {
        // Missing name
        var r1 = authService.signUp("", "new@example.com", "pass123", "pass123");
        assertFalse(r1.success());
        assertEquals("Please enter your full name.", r1.message());

        // Invalid email
        var r2 = authService.signUp("User", "not-an-email", "pass123", "pass123");
        assertFalse(r2.success());
        assertEquals("Please enter a valid email address.", r2.message());

        // Short password
        var r3 = authService.signUp("User", "user@test.com", "123", "123");
        assertFalse(r3.success());
        assertEquals("Password must be at least 6 characters long.", r3.message());

        // Password mismatch
        var r4 = authService.signUp("User", "user@test.com", "pass123", "pass456");
        assertFalse(r4.success());
        assertEquals("Passwords do not match.", r4.message());
    }

    @Test
    @DisplayName("Successful sign up in offline fallback clearly indicates temporary storage")
    void testSuccessfulSignUpAndSubsequentSignIn() {
        assertFalse(authService.isPersistentStorageAvailable());

        var regResult = authService.signUp("Jane Doe", "jane@example.com", "securePass123", "securePass123");
        assertTrue(regResult.success());
        assertTrue(regResult.message().contains("Temporary in-memory account created") || regResult.message().contains("will not persist"));

        // Duplicate registration should fail
        var dupResult = authService.signUp("Jane Doe Duplicate", "jane@example.com", "securePass123", "securePass123");
        assertFalse(dupResult.success());
        assertEquals("An account with this email already exists. Please sign in.", dupResult.message());

        // Now sign in with new account
        var loginResult = authService.signIn("jane@example.com", "securePass123");
        assertTrue(loginResult.success());
        assertNotNull(loginResult.user());
        assertEquals("Jane Doe", loginResult.user().getFullName());
    }

    @Test
    @DisplayName("Sign out clears current user and session")
    void testSignOut() {
        authService.signUp("Alex Rivera", "alex@example.com", "password123", "password123");
        authService.signIn("alex@example.com", "password123");
        assertTrue(authService.isAuthenticated());

        authService.signOut();
        assertFalse(authService.isAuthenticated());
        assertNull(authService.getCurrentUser());
    }

    @Test
    @DisplayName("Database unavailable throws produce standardized safe error message")
    void testDatabaseUnavailableError() {
        UserDAO failingDAO = new UserDAO() {
            @Override
            public Optional<User> findByEmail(String email) {
                throw new RuntimeException("Simulated connection timeout to MySQL");
            }

            @Override
            public boolean existsByEmail(String email) {
                throw new RuntimeException("Simulated connection timeout to MySQL");
            }

            @Override
            public boolean save(User user) {
                throw new RuntimeException("Simulated connection timeout to MySQL");
            }
        };

        AuthenticationService failingService = new AuthenticationService(failingDAO);

        var signInRes = failingService.signIn("user@example.com", "password123");
        assertFalse(signInRes.success());
        assertEquals("Account storage is currently unavailable. Please check the database connection and try again.", signInRes.message());

        var signUpRes = failingService.signUp("User", "user@example.com", "password123", "password123");
        assertFalse(signUpRes.success());
        assertEquals("Account storage is currently unavailable. Please check the database connection and try again.", signUpRes.message());
    }
}
