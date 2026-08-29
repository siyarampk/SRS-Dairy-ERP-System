package dairy.erp.util;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Configures java.util.logging for the whole application. Logs are written to
 * a file under the configured logs directory and mirrored to the console.
 * Uses only the JDK logging API (no external logging framework).
 */
public final class LogUtil {

    private static final Logger ROOT = Logger.getLogger("dairy.erp");
    private static volatile boolean initialized = false;

    private LogUtil() {
    }

    /** Initialises handlers. Safe to call multiple times (only first call does work). */
    public static synchronized void init(File logDir, String logFileName) {
        if (initialized) {
            return;
        }
        try {
            if (logDir != null && !logDir.exists()) {
                logDir.mkdirs();
            }
            Formatter formatter = new Formatter() {
                @Override
                public synchronized String format(LogRecord record) {
                    return String.format("%1$tY-%1$tm-%1$td %1$tT.%1$tL [%2$s] %3$s: %4$s%n",
                            new Date(record.getMillis()),
                            record.getLevel(),
                            record.getLoggerName(),
                            record.getMessage());
                }
            };

            FileHandler fileHandler = null;
            if (logDir != null) {
                fileHandler = new FileHandler(
                        new File(logDir, logFileName).getAbsolutePath(), true);
                fileHandler.setFormatter(formatter);
            }

            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(formatter);

            ROOT.setUseParentHandlers(false);
            if (fileHandler != null) {
                ROOT.addHandler(fileHandler);
            }
            ROOT.addHandler(consoleHandler);
            ROOT.setLevel(Level.ALL);
            initialized = true;
        } catch (IOException e) {
            System.err.println("Failed to configure file logging: " + e.getMessage());
        }
    }

    /** Returns a named logger derived from the calling class. */
    public static Logger getLogger(Class<?> clazz) {
        return Logger.getLogger(clazz.getName());
    }
}
