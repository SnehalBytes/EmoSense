package com.checkin.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages JDBC connections to the MySQL database for EmoSense.
 * Centralizes connection pooling, connectivity testing, and error handling.
 */
public class DatabaseConnection {

    private static DatabaseConnection instance;
    private final DatabaseConfig config;

    public DatabaseConnection() {
        this(new DatabaseConfig());
    }

    public DatabaseConnection(DatabaseConfig config) {
        this.config = config != null ? config : new DatabaseConfig();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[EmoSense DB] MySQL JDBC Driver not found: " + e.getMessage());
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public static synchronized void setInstance(DatabaseConnection customConnection) {
        instance = customConnection;
    }

    /**
     * Establishes a new JDBC connection using configured credentials.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(config.getUrl(), config.getUser(), config.getPassword());
    }

    /**
     * Validates whether the database is accessible without throwing unhandled exceptions.
     * If the MySQL server is running but the database does not exist yet, attempts creation.
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed() && conn.isValid(2);
        } catch (Exception e) {
            // If server is reachable, attempt to create database if it doesn't exist yet
            if (DatabaseInitializer.createDatabaseIfNotExists(config)) {
                try (Connection conn2 = getConnection()) {
                    return conn2 != null && !conn2.isClosed() && conn2.isValid(2);
                } catch (Exception ignored) {}
            }
            return false;
        }
    }

    public DatabaseConfig getConfig() {
        return config;
    }
}
