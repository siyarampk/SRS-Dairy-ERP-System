package dairy.erp.database;

import dairy.erp.config.AppConfig;
import dairy.erp.util.LogUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Simple SQLite connection management for a single-user desktop application.
 * A fresh connection is opened per call and the SQLite foreign-key pragma is
 * enabled for every connection. No connection pool is used on purpose.
 */
public final class DatabaseManager {

    private static final Logger LOG = LogUtil.getLogger(DatabaseManager.class);

    private static volatile String dbUrl;

    private DatabaseManager() {
    }

    /**
     * Loads the SQLite JDBC driver and validates connectivity.
     * Called once during application startup.
     */
    public static void initialize() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver (org.sqlite.JDBC) is not on the classpath", e);
        }
        dbUrl = "jdbc:sqlite:" + AppConfig.getDatabasePath();
        try (Connection c = getConnection()) {
            // Connection verified; pragma is applied per connection in getConnection().
        }
        LOG.info("Database connection configured at " + AppConfig.getDatabasePath());
    }

    /** Returns a new connection with foreign keys enabled. Caller must close it. */
    public static Connection getConnection() throws SQLException {
        if (dbUrl == null) {
            throw new SQLException("DatabaseManager has not been initialized.");
        }
        Connection connection = DriverManager.getConnection(dbUrl);
        try (java.sql.Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }
}
