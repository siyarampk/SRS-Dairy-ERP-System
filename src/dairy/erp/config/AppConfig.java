package dairy.erp.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Central application configuration: names, version and runtime folder/database
 * paths. All runtime folders are resolved relative to the current working
 * directory so the application runs fully offline out of a single project tree.
 */
public final class AppConfig {

    public static final String APP_NAME = "Dairy ERP";
    public static final String APP_PRODUCT_NAME = "SRS Dairy ERP";
    public static final String APP_TAGLINE = "Milk Collection & Dairy Management System";
    public static final String APP_VERSION = "1.0.0";
    public static final String APP_BUILD = "Build 2026.08";
    public static final java.awt.Color BRAND_COLOR = new java.awt.Color(0x1a, 0x5f, 0x7a);
    public static final String DB_FILE_NAME = "dairy.db";

    private static final Path BASE_DIR = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();

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
