package com.checkin.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centralized database configuration for EmoSense.
 * Precedence hierarchy:
 * 1. System Properties (-Demosense.db.url=...)
 * 2. Environment Variables (EMOSENSE_DB_URL=...)
 * 3. Local database.properties file
 * 4. Secure local defaults
 */
public class DatabaseConfig {

    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "3306";
    private static final String DEFAULT_DB = "emosense_db";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";
    private static final String DEFAULT_URL = "jdbc:mysql://" + DEFAULT_HOST + ":" + DEFAULT_PORT + "/"
            + DEFAULT_DB + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private final String url;
    private final String user;
    private final String password;

    public DatabaseConfig() {
        Properties props = loadPropertiesFile();

        // 1. URL
        String resolvedUrl = System.getProperty("emosense.db.url");
        if (resolvedUrl == null || resolvedUrl.isBlank()) {
            resolvedUrl = System.getenv("EMOSENSE_DB_URL");
        }
        if (resolvedUrl == null || resolvedUrl.isBlank()) {
            resolvedUrl = props.getProperty("emosense.db.url", DEFAULT_URL);
        }
        this.url = resolvedUrl;

        // 2. User
        String resolvedUser = System.getProperty("emosense.db.user");
        if (resolvedUser == null || resolvedUser.isBlank()) {
            resolvedUser = System.getenv("EMOSENSE_DB_USER");
        }
        if (resolvedUser == null || resolvedUser.isBlank()) {
            resolvedUser = props.getProperty("emosense.db.user", DEFAULT_USER);
        }
        this.user = resolvedUser;

        // 3. Password
        String resolvedPass = System.getProperty("emosense.db.password");
        if (resolvedPass == null) {
            resolvedPass = System.getenv("EMOSENSE_DB_PASSWORD");
        }
        if (resolvedPass == null) {
            resolvedPass = props.getProperty("emosense.db.password", DEFAULT_PASSWORD);
        }
        this.password = resolvedPass;
    }

    public DatabaseConfig(String url, String user, String password) {
        this.url = url != null ? url : DEFAULT_URL;
        this.user = user != null ? user : DEFAULT_USER;
        this.password = password != null ? password : "";
    }

    private Properties loadPropertiesFile() {
        Properties props = new Properties();
        File externalFile = new File("database.properties");
        if (externalFile.exists() && externalFile.isFile()) {
            try (InputStream in = new FileInputStream(externalFile)) {
                props.load(in);
            } catch (Exception ignored) {
                // Ignore failure reading local file; fallback to defaults
            }
        }
        return props;
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    /**
     * Safe string representation that masks credentials.
     */
    public String toSafeString() {
        return "DatabaseConfig[url=" + url + ", user=" + user + ", password=******]";
    }
}
