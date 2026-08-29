package dairy.erp.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Central application configuration: names, version and runtime folder/database
 * paths. All runtime folders are resolved relative to the current working
 * directory so the application runs fully offline out of a single project tree.
 */
public final class AppConfig {

    public static final String APP_NAME = "Dairy ERP - Milk Collection Management System";
    public static final String APP_VERSION = "1.0";
    public static final String DB_FILE_NAME = "dairy.db";

    public static final String DEFAULT_ADMIN_USER = "admin";
    public static final String DEFAULT_ADMIN_PASS = "admin11";

    private static final Path BASE_DIR =
            Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();

    private AppConfig() {
    }

    public static Path getBaseDir() {
        return BASE_DIR;
    }

    public static Path getDataDir() {
        return BASE_DIR.resolve("data");
    }

    public static Path getBackupDir() {
        return BASE_DIR.resolve("backup");
    }

    public static Path getExportDir() {
        return BASE_DIR.resolve("export");
    }

    public static Path getLogDir() {
        return BASE_DIR.resolve("logs");
    }

    public static Path getDatabasePath() {
        return getDataDir().resolve(DB_FILE_NAME);
    }
}
