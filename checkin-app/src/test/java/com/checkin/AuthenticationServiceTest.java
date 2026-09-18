package com.checkin;

import com.checkin.dao.InMemoryUserDAO;
import com.checkin.services.AuthenticationService;
import com.checkin.utils.Validators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthenticationServiceTest {

    private AuthenticationService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthenticationService(new InMemoryUserDAO());
    }

    @Test
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
    void testDefaultDemoUserCanSignIn() {
        var result = authService.signIn("test@emosense.com", "password123");
        assertTrue(result.success());
        assertNotNull(result.user());
        assertEquals("Alex Rivera", result.user().getFullName());
        assertTrue(authService.isAuthenticated());
        assertEquals(result.user(), authService.getCurrentUser());
    }

    @Test
    void testSignInWithEmptyFields() {
        var res1 = authService.signIn("", "password123");
        assertFalse(res1.success());
        assertEquals("Please enter your email address.", res1.message());

        var res2 = authService.signIn("test@emosense.com", "");
        assertFalse(res2.success());
        assertEquals("Please enter your password.", res2.message());
    }

    @Test
    void testSignInWithWrongPassword() {
        var res = authService.signIn("test@emosense.com", "wrongPass");
        assertFalse(res.success());
        assertEquals("Incorrect password. Please try again.", res.message());
    }

    @Test
    void testSignInWithNonexistentUser() {
        var res = authService.signIn("nonexistent@emosense.com", "password123");
        assertFalse(res.success());
        assertEquals("No account found with this email.", res.message());
    }

    @Test
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
    void testSuccessfulSignUpAndSubsequentSignIn() {
        var regResult = authService.signUp("Jane Doe", "jane@example.com", "securePass123", "securePass123");
        assertTrue(regResult.success());
        assertEquals("Account created successfully! You can now sign in.", regResult.message());

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
    void testSignOut() {
        authService.signIn("test@emosense.com", "password123");
        assertTrue(authService.isAuthenticated());

        authService.signOut();
        assertFalse(authService.isAuthenticated());
        assertNull(authService.getCurrentUser());
    }
}
