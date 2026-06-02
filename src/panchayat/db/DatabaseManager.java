package panchayat.db;

import java.sql.*;

/**
 * DatabaseManager — Central singleton for all SQLite connectivity.
 *
 * Design decision: Singleton pattern ensures only one connection is ever
 * opened, preventing file-lock conflicts that SQLite can throw when
 * multiple connections attempt to write simultaneously on desktop apps.
 *
 * On first launch createTables() builds the schema automatically, so no
 * external setup script is needed — perfect for non-technical staff.
 */
public class DatabaseManager {

    // SQLite file is stored next to the JAR for easy backup/migration
    private static final String DB_URL = "jdbc:sqlite:panchayat.db";

    /** Single shared connection for the lifetime of the application */
    private static Connection connection;

    // Private constructor enforces singleton usage
    private DatabaseManager() {}

    /**
     * Returns the shared SQLite connection, opening it on first call.
     *
     * Design decision: Using the default DELETE journal mode (not WAL)
     * because WAL's journal_mode=WAL pragma requires an exclusive lock
     * that can conflict with the immediate table-creation DDL on a fresh
     * database. For a single-user desktop application the default mode
     * gives correct behaviour with no concurrency trade-offs.
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            // Explicitly load the SQLite JDBC driver class.
            // Required because service-provider auto-detection via SPI can
            // fail when the JAR is added to the classpath manually rather
            // than discovered through a module system.
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                throw new SQLException("SQLite JDBC driver not found on classpath.\n" +
                    "Ensure sqlite-jdbc-*.jar is in the lib/ folder.", e);
            }
            connection = DriverManager.getConnection(DB_URL);
            connection.setAutoCommit(true); // explicit — DDL never waits for commit
            // Enable FK constraint enforcement (off by default in SQLite)
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys=ON;");
            }
        }
        return connection;
    }

    /**
     * Creates all tables on first launch if they don't already exist.
     * Called once from Main.java before the UI is shown.
     *
     * IF NOT EXISTS means this is safe to call on every startup — it
     * won't wipe data between sessions.
     */
    public static void createTables() {
        String complaintsTable = """
            CREATE TABLE IF NOT EXISTS complaints (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                citizen_name TEXT    NOT NULL,
                ward_number  TEXT    NOT NULL,
                category     TEXT    NOT NULL,
                description  TEXT    NOT NULL,
                date_filed   TEXT    NOT NULL,
                status       TEXT    NOT NULL DEFAULT 'Pending'
            );
        """;

        String issuesTable = """
            CREATE TABLE IF NOT EXISTS issues (
                id                   INTEGER PRIMARY KEY AUTOINCREMENT,
                title                TEXT NOT NULL,
                ward                 TEXT NOT NULL,
                type                 TEXT NOT NULL,
                severity             TEXT NOT NULL,
                assigned_officer     TEXT NOT NULL,
                date_reported        TEXT NOT NULL,
                expected_resolution  TEXT NOT NULL,
                status               TEXT NOT NULL DEFAULT 'Open'
            );
        """;

        String meetingsTable = """
            CREATE TABLE IF NOT EXISTS meetings (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                meeting_type TEXT NOT NULL,
                date         TEXT NOT NULL,
                attendees    TEXT NOT NULL,
                agenda       TEXT NOT NULL,
                resolutions  TEXT,
                action_items TEXT
            );
        """;

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(complaintsTable);
            stmt.execute(issuesTable);
            stmt.execute(meetingsTable);
            System.out.println("[DB] Tables verified / created successfully.");
        } catch (SQLException e) {
            System.err.println("[DB] Error creating tables: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /** Gracefully close the connection on application exit. */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error closing connection: " + e.getMessage());
        }
    }
}
