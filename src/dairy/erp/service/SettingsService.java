package dairy.erp.service;

import dairy.erp.dao.SettingsDAO;
import dairy.erp.util.CurrencyUtil;
import dairy.erp.util.LogUtil;

import java.math.BigDecimal;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Provides typed access to application settings and applies display
 * configuration (currency symbol, decimal places) at startup and on change.
 */
public class SettingsService {

    private static final Logger LOG = LogUtil.getLogger(SettingsService.class);
    private final SettingsDAO settingsDAO = new SettingsDAO();
    // Shared across all instances: when one screen (e.g. Settings) saves new
    // values, every other screen's next get() sees them immediately, so the
    // dairy name propagates to the Dashboard and everywhere else.
    private static Map<String, String> cache;

    public Map<String, String> allSettings() {
        try {
            if (cache == null) {
                cache = settingsDAO.loadAll();
            }
            return cache;
        } catch (Exception e) {
            LOG.severe("Could not load settings: " + e.getMessage());
            throw new RuntimeException("Could not load settings.", e);
        }
    }

    public void reload() {
        cache = null;
        allSettings();
    }

    public String get(String key) {
        String value = allSettings().get(key);
        return value == null ? "" : value;
    }

    public boolean getBoolean(String key, boolean fallback) {
        String v = get(key);
        if (v.isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(v);
    }

    public void saveAll(Map<String, String> settings) {
        try {
            for (Map.Entry<String, String> e : settings.entrySet()) {
                if (e.getKey().startsWith("app.") || e.getKey().startsWith("dairy.")
                        || e.getKey().startsWith("fat.") || e.getKey().startsWith("snf.")
                        || e.getKey().startsWith("paid.")) {
                    settingsDAO.put(e.getKey(), e.getValue());
                }
            }
            reload();
            applyDisplayConfig();
            // Propagate every saved setting live: screens registered with
            // AppBus.onSettingsChanged re-apply their defaults/locks at once,
            // so no restart is needed after changing Application Settings.
            dairy.erp.util.AppBus.fireSettingsChanged(settings);
        } catch (Exception e) {
            LOG.severe("Could not save settings: " + e.getMessage());
            throw new RuntimeException("Could not save settings.", e);
        }
    }

    public void applyDisplayConfig() {
        String symbol = get("app.currency_symbol");
        String places = get("app.decimal_places");
        int p;
        try {
            p = Integer.parseInt(places.isBlank() ? "2" : places);
        } catch (NumberFormatException e) {
            p = 2;
        }
        if (symbol.isBlank()) {
            symbol = "\u20B9";
        }
        CurrencyUtil.configure(symbol, p);
    }

    public BigDecimal minFat(String milkType) {
        return parse(get("fat.min_" + (milkType == null ? "cow" : milkType.toLowerCase())));
    }

    public BigDecimal maxFat(String milkType) {
        return parse(get("fat.max_" + (milkType == null ? "cow" : milkType.toLowerCase())));
    }

    public BigDecimal globalMinFat() {
        return parse(get("fat.min"));
    }

    public BigDecimal globalMaxFat() {
        return parse(get("fat.max"));
    }

    public BigDecimal minSnf() {
        return parse(get("snf.min"));
    }

    public BigDecimal maxSnf() {
        return parse(get("snf.max"));
    }

    public int decimalPlaces() {
        try {
            return Integer.parseInt(get("app.decimal_places").isBlank() ? "2" : get("app.decimal_places"));
        } catch (NumberFormatException e) {
            return 2;
        }
    }

    private BigDecimal parse(String s) {
        if (s == null || s.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
