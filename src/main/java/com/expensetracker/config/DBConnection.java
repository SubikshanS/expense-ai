package com.expensetracker.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DBConnection utility class managing JDBC database connectivity.
 *
 * Connection strategy:
 * 1. Try MySQL first.
 * 2. If MySQL cannot be connected, automatically use embedded SQLite.
 * 3. Automatically create the expenses table if it does not exist.
 *
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

    private static boolean usingSQLite = false;

    /**
     * Returns an active database connection.
     *
     * MySQL is attempted first. If MySQL cannot be connected,
     * SQLite is used automatically as a local fallback.
     *
     * @return active JDBC connection
     * @throws SQLException if both MySQL and SQLite connections fail
     */
    public static synchronized Connection getConnection() throws SQLException {

        if (!usingSQLite) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");

                Connection connection = DriverManager.getConnection(
                        MYSQL_URL,
                        MYSQL_USER,
                        MYSQL_PASS
                );

                initializeDatabaseSchema(connection);

                return connection;

            } catch (ClassNotFoundException e) {

                System.err.println(
                        "[DBConnection] MySQL driver not found. "
                                + "Switching to SQLite."
                );

            } catch (SQLException e) {

                System.err.println(
                        "[DBConnection] MySQL connection failed: "
                                + e.getMessage()
                );

                System.err.println(
                        "[DBConnection] Switching to embedded SQLite fallback."
                );
            }

            // MySQL failed, so permanently use SQLite for this application run.
            usingSQLite = true;
        }

        // SQLite fallback
        try {
            Class.forName("org.sqlite.JDBC");

            Connection connection =
                    DriverManager.getConnection(SQLITE_URL);

            initializeDatabaseSchema(connection);

            return connection;

        } catch (ClassNotFoundException e) {

            throw new SQLException(
                    "SQLite JDBC driver not found in classpath.",
                    e
            );

        } catch (SQLException e) {

            throw new SQLException(
                    "Unable to connect to both MySQL and SQLite.",
                    e
            );
        }
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

        String createTableSQL;

        if (usingSQLite) {

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
                            + (usingSQLite ? "SQLite" : "MySQL")
                            + ")"
            );
        }
    }

    /**
     * Returns whether the application is currently using SQLite.
     *
     * @return true if SQLite is being used, otherwise false
     */
    public static boolean isUsingSQLite() {
        return usingSQLite;
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