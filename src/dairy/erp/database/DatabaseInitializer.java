package dairy.erp.database;

import dairy.erp.config.AppConfig;
import dairy.erp.util.HashUtil;
import dairy.erp.util.LogUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Ensures the SQLite database exists with the required schema and seeds
 * essential baseline data (admin user, default settings, sample data on a
 * brand-new database). Runs automatically on first application startup.
 */
public final class DatabaseInitializer {

    private static final Logger LOG = LogUtil.getLogger(DatabaseInitializer.class);

    private DatabaseInitializer() {
    }

    private static final String USERS_DDL =
            "CREATE TABLE IF NOT EXISTS users (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  username TEXT NOT NULL UNIQUE," +
            "  password TEXT NOT NULL," +
            "  created_at TEXT," +
            "  updated_at TEXT" +
            ")";

    private static final String CUSTOMERS_DDL =
            "CREATE TABLE IF NOT EXISTS customers (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  customer_code TEXT NOT NULL UNIQUE," +
            "  customer_name TEXT NOT NULL," +
            "  father_name TEXT," +
            "  mobile TEXT," +
            "  address TEXT," +
            "  village TEXT," +
            "  milk_type TEXT," +
            "  customer_type TEXT," +
            "  status TEXT," +
            "  opening_balance NUMERIC DEFAULT 0," +
            "  registration_date TEXT," +
            "  remarks TEXT," +
            "  created_at TEXT," +
            "  updated_at TEXT" +
            ")";

    private static final String RATE_CHART_DDL =
            "CREATE TABLE IF NOT EXISTS rate_chart (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  milk_type TEXT," +
            "  fat_min NUMERIC," +
            "  fat_max NUMERIC," +
            "  snf_min NUMERIC," +
            "  snf_max NUMERIC," +
            "  rate_per_litre NUMERIC," +
            "  effective_from TEXT," +
            "  effective_to TEXT," +
            "  active INTEGER DEFAULT 1," +
            "  created_at TEXT," +
            "  updated_at TEXT" +
            ")";

    private static final String MILK_COLLECTION_DDL =
            "CREATE TABLE IF NOT EXISTS milk_collection (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  collection_date TEXT NOT NULL," +
            "  customer_id INTEGER NOT NULL," +
            "  milk_type TEXT," +
            "  shift TEXT," +
            "  quantity NUMERIC," +
            "  fat NUMERIC," +
            "  snf NUMERIC," +
            "  rate_per_litre NUMERIC," +
            "  amount NUMERIC," +
            "  remarks TEXT," +
            "  created_at TEXT," +
            "  updated_at TEXT," +
            "  UNIQUE (collection_date, customer_id, shift)" +
            ")";

    private static final String PAYMENTS_DDL =
            "CREATE TABLE IF NOT EXISTS payments (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  customer_id INTEGER NOT NULL," +
            "  payment_date TEXT," +
            "  amount NUMERIC," +
            "  payment_mode TEXT," +
            "  reference TEXT," +
            "  remarks TEXT," +
            "  created_at TEXT," +
            "  updated_at TEXT" +
            ")";

    private static final String SETTINGS_DDL =
            "CREATE TABLE IF NOT EXISTS settings (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  setting_key TEXT UNIQUE," +
            "  setting_value TEXT" +
            ")";

    private static final String APP_LOG_DDL =
            "CREATE TABLE IF NOT EXISTS app_log (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  action TEXT," +
            "  description TEXT," +
            "  created_at TEXT" +
            ")";

    /** Sample seed data is intentionally disabled: the app starts with a clean database. */
    private static final boolean SEED_SAMPLE_DATA = false;

    /**
     * Runs the full initialization sequence.
     * @return true when the database is brand new (and sample data was loaded)
     */
    public static boolean initialize() throws SQLException {
        ensureRuntimeFolders();
        DatabaseManager.initialize();
        createTables();
        boolean isNew = isCustomersTableEmpty();
        seedAdminUser();
        seedDefaultSettings();
        if (isNew) {
            if (SEED_SAMPLE_DATA) {
                seedSampleData();
                LOG.info("Sample data loaded because database is new.");
            } else {
                LOG.info("Sample data seeding disabled; starting with a clean database.");
            }
        }
        recordStartup("APPLICATION_START", "Application initialized. New database = " + isNew);
        LOG.info("Database initialized successfully. New database = " + isNew);
        return isNew;
    }

    private static void ensureRuntimeFolders() throws SQLException {
        Path[] dirs = { AppConfig.getDataDir(), AppConfig.getBackupDir(),
                AppConfig.getExportDir(), AppConfig.getLogDir() };
        for (Path dir : dirs) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new SQLException("Could not create runtime folder: " + dir, e);
            }
        }
    }

    private static void createTables() throws SQLException {
        String[] ddl = { USERS_DDL, CUSTOMERS_DDL, RATE_CHART_DDL,
                MILK_COLLECTION_DDL, PAYMENTS_DDL, SETTINGS_DDL, APP_LOG_DDL };
        try (Connection c = DatabaseManager.getConnection();
             Statement st = c.createStatement()) {
            for (String sql : ddl) {
                st.executeUpdate(sql);
            }
        }
        LOG.info("Database tables ensured.");
    }

    private static boolean isCustomersTableEmpty() throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM customers");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }


    /** The two admin accounts the application ships with. */
    private static final String[][] ADMIN_USERS = {
            {"ramkesh", "ramkesh@86"},
            {"siyarampk", "Siyaram@123"}
    };

    private static void seedAdminUser() throws SQLException {
        try (Connection c = DatabaseManager.getConnection()) {
            for (String[] u : ADMIN_USERS) {
                insertUserIfMissing(c, u[0], u[1]);
            }
            LOG.info("Admin users ensured: " + ADMIN_USERS.length + " accounts.");
        }
    }

    /** Inserts a user with the given (hashed) password unless the username exists. */
    private static void insertUserIfMissing(Connection c, String username, String password) throws SQLException {
        try (PreparedStatement check = c.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?")) {
            check.setString(1, username);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }
        }
        String hashed = HashUtil.hashPassword(password);
        try (PreparedStatement ins = c.prepareStatement(
                "INSERT INTO users(username, password, created_at, updated_at) " +
                "VALUES(?, ?, datetime('now'), datetime('now'))")) {
            ins.setString(1, username);
            ins.setString(2, hashed);
            ins.executeUpdate();
        }
    }

    private static void seedDefaultSettings() throws SQLException {
        Map<String, String> defaults = defaultSettings();
        try (Connection c = DatabaseManager.getConnection()) {
            for (Map.Entry<String, String> e : defaults.entrySet()) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT OR IGNORE INTO settings(setting_key, setting_value) VALUES(?, ?)")) {
                    ps.setString(1, e.getKey());
                    ps.setString(2, e.getValue());
                    ps.executeUpdate();
                }
            }
        }
    }

    private static Map<String, String> defaultSettings() {
        Map<String, String> map = new LinkedHashMap<>();
        // Dairy information
        map.put("dairy.name", "");
        map.put("dairy.owner", "");
        map.put("dairy.address", "");
        map.put("dairy.mobile", "");
        map.put("dairy.email", "");
        map.put("dairy.gst", "");
        // Application settings
        map.put("app.date_format", "dd-MM-yyyy");
        map.put("app.currency_symbol", "\u20B9");
        map.put("app.decimal_places", "2");
        map.put("app.default_shift", "Morning");
        map.put("app.default_milk_type", "Cow");
        map.put("app.manual_rate_override", "false");
        // Global FAT/SNF bounds
        map.put("fat.min", "2.0");
        map.put("fat.max", "12.0");
        map.put("snf.min", "6.0");
        map.put("snf.max", "10.0");
        // Per milk type FAT bounds
        map.put("fat.min_cow", "2.0");
        map.put("fat.max_cow", "8.0");
        map.put("fat.min_buffalo", "4.0");
        map.put("fat.max_buffalo", "12.0");
        map.put("fat.min_mix", "2.0");
        map.put("fat.max_mix", "12.0");
        return map;
    }

    private static void seedSampleData() throws SQLException {
        seedSampleCustomers();
        seedSampleRateChart();
    }


    private static void seedSampleCustomers() throws SQLException {
        Object[][] customers = {
                {"CUST001", "Ramesh", "Cow"},
                {"CUST002", "Suresh", "Buffalo"},
                {"CUST003", "Mahesh", "Mix"},
                {"CUST004", "Kamla", "Cow"},
                {"CUST005", "Geeta", "Buffalo"}
        };
        String sql = "INSERT INTO customers(customer_code, customer_name, milk_type, customer_type, " +
                "status, opening_balance, registration_date, created_at, updated_at) " +
                "VALUES(?, ?, ?, 'Supplier', 'Active', 0, ?, datetime('now'), datetime('now'))";
        String today = LocalDate.now().toString();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (Object[] row : customers) {
                ps.setString(1, (String) row[0]);
                ps.setString(2, (String) row[1]);
                ps.setString(3, (String) row[2]);
                ps.setString(4, today);
                ps.executeUpdate();
            }
        }
        LOG.info("Sample customers seeded.");
    }

    private static void seedSampleRateChart() throws SQLException {
        Object[][] rates = {
                {"Cow", 3.0, 3.2, 8.5, 8.5, 40.00},
                {"Cow", 3.5, 3.7, 8.5, 8.5, 42.00},
                {"Cow", 4.0, 4.2, 8.5, 8.5, 44.00},
                {"Buffalo", 6.0, 6.2, 8.5, 8.5, 50.00},
                {"Buffalo", 6.5, 6.7, 8.5, 8.5, 55.00},
                {"Buffalo", 7.0, 7.2, 8.5, 8.5, 60.00},
                {"Mix", 4.0, 4.2, 8.5, 8.5, 45.00}
        };
        String sql = "INSERT INTO rate_chart(milk_type, fat_min, fat_max, snf_min, snf_max, " +
                "rate_per_litre, effective_from, active, created_at, updated_at) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, 1, datetime('now'), datetime('now'))";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (Object[] row : rates) {
                ps.setString(1, (String) row[0]);
                ps.setDouble(2, (Double) row[1]);
                ps.setDouble(3, (Double) row[2]);
                ps.setDouble(4, (Double) row[3]);
                ps.setDouble(5, (Double) row[4]);
                ps.setDouble(6, (Double) row[5]);
                ps.setString(7, LocalDate.now().toString());
                ps.executeUpdate();
            }
        }
        LOG.info("Sample rate chart seeded.");
    }

    private static void recordStartup(String action, String description) throws SQLException {
        String sql = "INSERT INTO app_log(action, description, created_at) VALUES(?, ?, datetime('now'))";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, action);
            ps.setString(2, description);
            ps.executeUpdate();
        }
    }
}
