package dairy.erp.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Currency and number formatting for financial values. Uses BigDecimal-safe
 * rounding and a configurable currency symbol and decimal places.
 */
public final class CurrencyUtil {

    private static volatile String symbol = "\u20B9";
    private static volatile int decimalPlaces = 2;

    private CurrencyUtil() {
    }

    public static synchronized void configure(String currencySymbol, int places) {
        if (currencySymbol != null && !currencySymbol.isBlank()) {
            symbol = currencySymbol;
        }
        if (places >= 0 && places <= 6) {
            decimalPlaces = places;
        }
    }

    public static String getSymbol() {
        return symbol;
    }

    public static int getDecimalPlaces() {
        return decimalPlaces;
    }

    /** Rounds a value to the configured number of decimal places. */
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(decimalPlaces, RoundingMode.HALF_UP);
    }

    /** Formats a plain number to the configured decimal places. */
    public static String format(BigDecimal value) {
        DecimalFormat df = new DecimalFormat("#,##0." + "0".repeat(decimalPlaces));
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return df.format(value);
    }

    /** Formats a plain number (no currency symbol), e.g. for FAT/SNF. */
    public static String formatPlain(BigDecimal value) {
        if (value == null) {
            return formatPlain(BigDecimal.ZERO);
        }
        return value.setScale(decimalPlaces, RoundingMode.HALF_UP).toPlainString();
    }

    /** Formats a value as a currency amount, e.g. ₹1,234.50. */
    public static String formatMoney(BigDecimal value) {
        DecimalFormat df = new DecimalFormat("#,##0." + "0".repeat(decimalPlaces));
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return symbol + df.format(value);
    }

    /** Formats a percentage value (FAT/SNF) with the given scale. */
    public static String formatPercent(BigDecimal value, int scale) {
        return value.setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }
}
