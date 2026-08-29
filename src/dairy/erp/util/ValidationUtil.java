package dairy.erp.util;

import java.math.BigDecimal;

/**
 * Lightweight input validation helpers reused across forms.
 */
public final class ValidationUtil {

    private ValidationUtil() {
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static boolean isValidMobile(String mobile) {
        if (isBlank(mobile)) {
            return false;
        }
        return mobile.matches("\\d{10}");
    }

    /**
     * Parses a decimal from user input.
     * @return the parsed value, or {@code null} if not a valid number
     */
    public static BigDecimal parseDecimal(String s) {
        if (isBlank(s)) {
            return null;
        }
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** True when the value is strictly greater than zero. */
    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    /** True when the value lies within the inclusive [min, max] range. */
    public static boolean isWithin(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value != null && min != null && max != null
                && value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }
}
