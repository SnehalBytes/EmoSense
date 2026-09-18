package com.checkin.services;

import com.checkin.dao.InMemoryUserDAO;
import com.checkin.dao.UserDAO;
import com.checkin.model.User;
import com.checkin.dao.JdbcUserDAO;
import com.checkin.database.DatabaseConnection;
import com.checkin.utils.PasswordHasher;
import com.checkin.utils.Validators;

import java.util.Optional;
import java.util.UUID;

/**
 * Service orchestrating authentication logic, registration validation,
 * secure password hashing, and user session management for EmoSense.
 */
public class AuthenticationService {

    private final UserDAO userDAO;
    private User currentUser;

    public AuthenticationService() {
        this(createDefaultUserDAO());
    }

    public AuthenticationService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    private static UserDAO createDefaultUserDAO() {
        try {
            DatabaseConnection db = DatabaseConnection.getInstance();
            if (db.testConnection()) {
                return new JdbcUserDAO(db);
            }
        } catch (Exception ignored) {}
        System.err.println("[AuthenticationService] Local MySQL not currently accessible; operating with prototype in-memory store.");
        return new InMemoryUserDAO();
    }

    public record AuthResult(boolean success, String message, User user) {}

    /**
     * Authenticate an existing user with email and password using cryptographic verification.
     */
    public AuthResult signIn(String email, String password) {
        if (!Validators.isNotEmpty(email)) {
            return new AuthResult(false, "Please enter your email address.", null);
        }
        if (!Validators.isValidEmail(email)) {
            return new AuthResult(false, "Please enter a valid email address.", null);
        }
        if (!Validators.isNotEmpty(password)) {
            return new AuthResult(false, "Please enter your password.", null);
        }

        try {
            Optional<User> userOpt = userDAO.findByEmail(email);
            if (userOpt.isEmpty()) {
                return new AuthResult(false, "No account found with this email.", null);
            }

            User user = userOpt.get();
            if (!PasswordHasher.verify(password, user.getPassword())) {
                return new AuthResult(false, "Incorrect password. Please try again.", null);
            }

            this.currentUser = user;
            UserSession.getInstance().login(user);
            return new AuthResult(true, "Signed in successfully.", user);
        } catch (Exception e) {
            System.err.println("[AuthenticationService] Sign in error: " + e.getMessage());
            return new AuthResult(false, "Database connection error. Please try again later.", null);
        }
    }

    /**
     * Register a new user account with cryptographic password hashing.
     */
    public AuthResult signUp(String fullName, String email, String password, String confirmPassword) {
        if (!Validators.isNotEmpty(fullName)) {
            return new AuthResult(false, "Please enter your full name.", null);
        }
        if (!Validators.isNotEmpty(email)) {
            return new AuthResult(false, "Please enter your email address.", null);
        }
        if (!Validators.isValidEmail(email)) {
            return new AuthResult(false, "Please enter a valid email address.", null);
        }
        if (!Validators.isNotEmpty(password)) {
            return new AuthResult(false, "Please enter a password.", null);
        }
        if (password.length() < 6) {
            return new AuthResult(false, "Password must be at least 6 characters long.", null);
        }
        if (!Validators.isNotEmpty(confirmPassword)) {
            return new AuthResult(false, "Please confirm your password.", null);
        }
        if (!password.equals(confirmPassword)) {
            return new AuthResult(false, "Passwords do not match.", null);
        }

        try {
            if (userDAO.existsByEmail(email)) {
                return new AuthResult(false, "An account with this email already exists. Please sign in.", null);
            }

            String passwordHash = PasswordHasher.hash(password);
            User newUser = new User(UUID.randomUUID().toString(), fullName.trim(), email.trim(), passwordHash);
            boolean saved = userDAO.save(newUser);
            if (!saved) {
                return new AuthResult(false, "Unable to save account to database. Please try again.", null);
            }

            return new AuthResult(true, "Account created successfully! You can now sign in.", newUser);
        } catch (Exception e) {
            System.err.println("[AuthenticationService] Registration error: " + e.getMessage());
            return new AuthResult(false, "Database error during registration. Please check server connection.", null);
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public void signOut() {
        this.currentUser = null;
        UserSession.getInstance().logout();
    }
}
