package dairy.erp.service;

import dairy.erp.config.AppConfig;
import dairy.erp.util.LogUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * Backup and restore of the SQLite database file. A backup is a plain copy of
 * the .db file stored under the backup folder. Restore copies a chosen backup
 * back over the live database (callers are responsible for confirmation).
 */
public class BackupService {

    private static final Logger LOG = LogUtil.getLogger(BackupService.class);
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /** Creates a timestamped backup of the current database. */
    public File backup() throws IOException {
        Path db = AppConfig.getDatabasePath();
        if (!Files.exists(db)) {
            throw new IOException("Database file not found: " + db);
        }
        Path backupDir = AppConfig.getBackupDir();
        Files.createDirectories(backupDir);
        String name = "DairyERP_Backup_" + LocalDateTime.now().format(FILE_STAMP) + ".db";
        Path dest = backupDir.resolve(name);
        Files.copy(db, dest, StandardCopyOption.REPLACE_EXISTING);
        LOG.info("Database backed up to " + dest);
        return dest.toFile();
    }

    /** Restores the database from the given backup file, replacing current data. */
    public void restore(File backupFile) throws IOException {
        if (backupFile == null || !backupFile.exists() || !backupFile.isFile()) {
            throw new IOException("Backup file does not exist: " + backupFile);
        }
        Path db = AppConfig.getDatabasePath();
        Files.createDirectories(db.getParent());
        Files.copy(backupFile.toPath(), db, StandardCopyOption.REPLACE_EXISTING);
        LOG.info("Database restored from " + backupFile.getAbsolutePath());
    }
}
