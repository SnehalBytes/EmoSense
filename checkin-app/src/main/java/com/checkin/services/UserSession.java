package com.checkin.services;

import com.checkin.model.User;

/**
 * Manages the authenticated user session for EmoSense.
 * Ensures check-ins, history, and insights are strictly bound to the active user.
 */
public class UserSession {

    private static UserSession instance;
    private User currentUser;

    public UserSession() {}

    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public static synchronized void setInstance(UserSession customSession) {
        instance = customSession;
    }

    public synchronized void login(User user) {
        this.currentUser = user;
    }

    public synchronized void logout() {
        this.currentUser = null;
    }

    public synchronized User getCurrentUser() {
        return currentUser;
    }

    public synchronized String getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : null;
    }

    public synchronized boolean isLoggedIn() {
        return currentUser != null;
    }
}
