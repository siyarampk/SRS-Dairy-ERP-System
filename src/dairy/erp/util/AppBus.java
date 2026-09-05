package dairy.erp.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Tiny in-process event bus so screens stay in sync live: when shared data
 * (e.g. the dairy name or the selected theme) is changed in one panel such as
 * Settings, every panel displaying that data updates immediately — no restart
 * needed. Listeners run on the Swing event dispatch thread of the caller.
 */
public final class AppBus {

    private static final List<Consumer<String>> DAIRY_NAME_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<Consumer<Theme>> THEME_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<Consumer<Void>> CUSTOMERS_LISTENERS = new CopyOnWriteArrayList<>();
    private static final List<Consumer<Map<String, String>>> SETTINGS_LISTENERS = new CopyOnWriteArrayList<>();

    private AppBus() {
    }

    /** Registers a listener that receives the new dairy name whenever it changes. */
    public static void onDairyNameChanged(Consumer<String> listener) {
        DAIRY_NAME_LISTENERS.add(listener);
    }

    /** Fired after the dairy name is saved successfully in Settings. */
    public static void fireDairyNameChanged(String newName) {
        for (Consumer<String> listener : DAIRY_NAME_LISTENERS) {
            listener.accept(newName);
        }
    }

    /** Registers a listener that receives the new theme whenever it changes. */
    public static void onThemeChanged(Consumer<Theme> listener) {
        THEME_LISTENERS.add(listener);
    }

    /** Fired after the theme is saved successfully in Settings. */
    public static void fireThemeChanged(Theme newTheme) {
        for (Consumer<Theme> listener : THEME_LISTENERS) {
            listener.accept(newTheme);
        }
    }

    /** Registers a listener notified whenever customer master data changes. */
    public static void onCustomersChanged(Consumer<Void> listener) {
        CUSTOMERS_LISTENERS.add(listener);
    }

    /**
     * Fired after a customer is added, updated or deleted/deactivated so
     * every screen showing customer data (e.g. the Milk Collection filter
     * combo) reloads immediately — no restart needed.
     */
    public static void fireCustomersChanged() {
        for (Consumer<Void> listener : CUSTOMERS_LISTENERS) {
            listener.accept(null);
        }
    }

    /**
     * Registers a listener notified whenever application settings are saved.
     * The listener receives the map of saved key/value pairs so screens can
     * re-apply only what they care about (default shift, milk type, field
     * locks, ...).
     */
    public static void onSettingsChanged(Consumer<Map<String, String>> listener) {
        SETTINGS_LISTENERS.add(listener);
    }

    /**
     * Fired after settings are saved successfully (centralised in
     * {@code SettingsService.saveAll}) so every open screen re-applies the
     * changed values immediately — no restart needed.
     */
    public static void fireSettingsChanged(Map<String, String> savedSettings) {
        for (Consumer<Map<String, String>> listener : SETTINGS_LISTENERS) {
            listener.accept(savedSettings);
        }
    }
}
