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
    private static final String DEFAULT_DB = "emosense";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";
    private static final String DEFAULT_URL = "jdbc:mysql://" + DEFAULT_HOST + ":" + DEFAULT_PORT + "/"
            + DEFAULT_DB + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private final String url;
    private final String user;
    private final String password;
    private final String host;
    private final String port;
    private final String databaseName;

    public DatabaseConfig() {
        Properties props = loadPropertiesFile();

        // 1. Host, Port, DB Name
        String resolvedHost = System.getProperty("emosense.db.host");
        if (resolvedHost == null || resolvedHost.isBlank()) {
            resolvedHost = System.getenv("EMOSENSE_DB_HOST");
        }
        if (resolvedHost == null || resolvedHost.isBlank()) {
            resolvedHost = props.getProperty("emosense.db.host", DEFAULT_HOST);
        }
        this.host = resolvedHost;

        String resolvedPort = System.getProperty("emosense.db.port");
        if (resolvedPort == null || resolvedPort.isBlank()) {
            resolvedPort = System.getenv("EMOSENSE_DB_PORT");
        }
        if (resolvedPort == null || resolvedPort.isBlank()) {
            resolvedPort = props.getProperty("emosense.db.port", DEFAULT_PORT);
        }
        this.port = resolvedPort;

        String resolvedDb = System.getProperty("emosense.db.name");
        if (resolvedDb == null || resolvedDb.isBlank()) {
            resolvedDb = System.getenv("EMOSENSE_DB_NAME");
        }
        if (resolvedDb == null || resolvedDb.isBlank()) {
            resolvedDb = props.getProperty("emosense.db.name", DEFAULT_DB);
        }
        this.databaseName = resolvedDb;

        // 2. URL
        String resolvedUrl = System.getProperty("emosense.db.url");
        if (resolvedUrl == null || resolvedUrl.isBlank()) {
            resolvedUrl = System.getenv("EMOSENSE_DB_URL");
        }
        if (resolvedUrl == null || resolvedUrl.isBlank()) {
            String constructedUrl = "jdbc:mysql://" + host + ":" + port + "/" + databaseName
                    + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            resolvedUrl = props.getProperty("emosense.db.url", constructedUrl);
        }
        this.url = resolvedUrl;

        // 3. User
        String resolvedUser = System.getProperty("emosense.db.user");
        if (resolvedUser == null || resolvedUser.isBlank()) {
            resolvedUser = System.getenv("EMOSENSE_DB_USER");
        }
        if (resolvedUser == null || resolvedUser.isBlank()) {
            resolvedUser = props.getProperty("emosense.db.user", DEFAULT_USER);
        }
        this.user = resolvedUser;

        // 4. Password
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
        this.host = DEFAULT_HOST;
        this.port = DEFAULT_PORT;
        this.databaseName = DEFAULT_DB;
    }

    private Properties loadPropertiesFile() {
        Properties props = new Properties();
        File externalFile = new File("database.properties");
        if (!externalFile.exists() || !externalFile.isFile()) {
            externalFile = new File("checkin-app/database.properties");
        }
        if (!externalFile.exists() || !externalFile.isFile()) {
            externalFile = new File("../database.properties");
        }
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

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * Connection URL targeting the MySQL server without selecting a specific database.
     * Useful for executing CREATE DATABASE IF NOT EXISTS during initialization.
     */
    public String getServerUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    /**
     * Safe string representation that masks credentials.
     */
    public String toSafeString() {
        return "DatabaseConfig[url=" + url + ", user=" + user + ", password=******]";
    }
}
