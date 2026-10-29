package com.expensetracker.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DBConnection utility class managing JDBC database connectivity.
 * MySQL configuration can be provided through:
 * - Environment variables: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
 * - JVM properties: db.host, db.port, db.name, db.user, db.password
 */
public final class DBConnection {

    private DBConnection() {
        // Utility class - prevent object creation
    }

    // MySQL configuration
    private static final String MYSQL_HOST =
            getEnvOrProperty("DB_HOST", "db.host", "localhost");

    private static final String MYSQL_PORT =
            getEnvOrProperty("DB_PORT", "db.port", "3306");

    private static final String MYSQL_DB =
            getEnvOrProperty("DB_NAME", "db.name", "expense_tracker_db");

    private static final String MYSQL_USER =
            getEnvOrProperty("DB_USER", "db.user", "root");

    private static final String MYSQL_PASS =
            getEnvOrProperty("DB_PASSWORD", "db.password", "");

    private static final String MYSQL_URL =
            "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT + "/" + MYSQL_DB
                    + "?createDatabaseIfNotExist=true"
                    + "&useSSL=false"
                    + "&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=UTC";

    // SQLite embedded fallback
    private static final String SQLITE_URL =
            "jdbc:sqlite:expense_tracker.db";

    private static Boolean usingSQLite = null;
    private static boolean schemaInitialized = false;

    /**
     * Determines whether SQLite or MySQL should be used.
     * Evaluated once at initial connection time to avoid silent mid-session switching.
     */
    private static synchronized void detectDatabaseBackend() {
        if (usingSQLite != null) {
            return;
        }

        // Try connecting to MySQL first
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASS)) {
                usingSQLite = false;
                System.out.println("[DBConnection] Connected to MySQL successfully: " + MYSQL_HOST + ":" + MYSQL_PORT + "/" + MYSQL_DB);
                return;
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection] MySQL driver not found. Defaulting to embedded SQLite.");
        } catch (SQLException e) {
            System.err.println("[DBConnection] MySQL connection failed (" + e.getMessage() + "). Defaulting to embedded SQLite.");
        }

        usingSQLite = true;
    }

    /**
     * Returns an active database connection.
     *
     * MySQL is attempted first at startup. If unavailable, embedded SQLite is used.
     * Database mode is fixed once initialized to prevent mid-run data fragmentation.
     *
     * @return active JDBC connection
     * @throws SQLException if both MySQL and SQLite connections fail
     */
    public static synchronized Connection getConnection() throws SQLException {
        if (usingSQLite == null) {
            detectDatabaseBackend();
        }

        Connection connection;
        if (!usingSQLite) {
            try {
                connection = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASS);
            } catch (SQLException e) {
                // If MySQL was previously established but fails on this query,
                // do NOT silently switch to SQLite and write data to a different database.
                throw new SQLException("MySQL database connection error: " + e.getMessage(), e);
            }
        } else {
            try {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(SQLITE_URL);
            } catch (ClassNotFoundException e) {
                throw new SQLException("SQLite JDBC driver not found in classpath.", e);
            }
        }

        if (!schemaInitialized) {
            initializeDatabaseSchema(connection);
            schemaInitialized = true;
        }

        return connection;
    }

    /**
     * Creates the expenses table if it does not already exist.
     *
     * The SQL syntax differs slightly between MySQL and SQLite.
     *
     * @param connection active database connection
     * @throws SQLException if table creation fails
     */
    private static void initializeDatabaseSchema(Connection connection)
            throws SQLException {

        boolean isSqlite = usingSQLite != null && usingSQLite;
        String createTableSQL;

        if (isSqlite) {
            createTableSQL =
                    "CREATE TABLE IF NOT EXISTS expenses (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "amount REAL NOT NULL, " +
                    "category TEXT NOT NULL, " +
                    "description TEXT NOT NULL, " +
                    "date TEXT NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
        } else {
            createTableSQL =
                    "CREATE TABLE IF NOT EXISTS expenses (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "amount DOUBLE NOT NULL, " +
                    "category VARCHAR(50) NOT NULL, " +
                    "description VARCHAR(255) NOT NULL, " +
                    "date DATE NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSQL);
            System.out.println(
                    "[DBConnection] Database schema initialized successfully ("
                            + (isSqlite ? "SQLite" : "MySQL")
                            + ")"
            );
        }
    }

    /**
     * Returns whether the application is currently using SQLite.
     *
     * @return true if SQLite is being used, otherwise false
     */
    public static synchronized boolean isUsingSQLite() {
        if (usingSQLite == null) {
            detectDatabaseBackend();
        }
        return usingSQLite;
    }

    /**
     * Returns the human-readable database backend name currently in use.
     */
    public static synchronized String getDatabaseType() {
        return isUsingSQLite() ? "SQLite (Embedded)" : "MySQL 8.0";
    }

    /**
     * Reads a configuration value from an environment variable first,
     * then a JVM system property, and finally uses the supplied default.
     */
    private static String getEnvOrProperty(
            String environmentVariable,
            String systemProperty,
            String defaultValue) {

        String environmentValue =
                System.getenv(environmentVariable);

        if (environmentValue != null
                && !environmentValue.isBlank()) {

            return environmentValue;
        }

        return System.getProperty(
                systemProperty,
                defaultValue
        );
    }
}