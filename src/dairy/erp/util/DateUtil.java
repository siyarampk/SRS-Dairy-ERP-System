package dairy.erp.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Date helpers. The UI displays dates as dd-MM-yyyy while the database stores
 * them as yyyy-MM-dd (SQLite ISO ordering).
 */
public final class DateUtil {

    public static final String DISPLAY_PATTERN = "dd-MM-yyyy";
    public static final String DB_PATTERN = "yyyy-MM-dd";

    public static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern(DISPLAY_PATTERN);
    public static final DateTimeFormatter DB = DateTimeFormatter.ofPattern(DB_PATTERN);

    private DateUtil() {
    }

    public static String toDisplay(LocalDate date) {
        return date == null ? "" : date.format(DISPLAY);
    }

    public static String toDb(LocalDate date) {
        return date == null ? "" : date.format(DB);
    }

    public static LocalDate today() {
        return LocalDate.now();
    }

    /**
     * Parses a display-format date (dd-MM-yyyy).
     * @return the date, or {@code null} if unparseable
     */
    public static LocalDate parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), DISPLAY);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Parses a database-format date (yyyy-MM-dd). */
    public static LocalDate parseDb(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), DB);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
