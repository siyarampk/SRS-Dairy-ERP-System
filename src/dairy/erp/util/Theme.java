package dairy.erp.util;

import java.awt.Color;

/**
 * Defines a visual theme for the application: the brand accent colour, the
 * success/action colour, and supporting shades used across panels, buttons,
 * charts and the dairy-name logo. Four built-in themes are provided; the
 * selected one is persisted as the {@code app.theme} setting.
 */
public final class Theme {

    /** Built-in themes, keyed by stable id (persisted in settings). */
    public static final Theme OCEAN_TEAL = new Theme("ocean_teal", "Ocean Teal",
            new Color(0x1a, 0x5f, 0x7a), new Color(0x0b, 0x7a, 0x3e), new Color(0xdc, 0xe9, 0xef));

    public static final Theme ROYAL_PURPLE = new Theme("royal_purple", "Royal Purple",
            new Color(0x6a, 0x1b, 0x9a), new Color(0x2e, 0x7d, 0x32), new Color(0xf3, 0xe5, 0xf5));

    public static final Theme FOREST_GREEN = new Theme("forest_green", "Forest Green",
            new Color(0x2e, 0x7d, 0x32), new Color(0x15, 0x65, 0xc0), new Color(0xe8, 0xf5, 0xe9));

    public static final Theme SUNSET_ORANGE = new Theme("sunset_orange", "Sunset Orange",
            new Color(0xef, 0x6c, 0x00), new Color(0x1a, 0x5f, 0x7a), new Color(0xff, 0xf3, 0xe0));

    public static final Theme[] ALL = {OCEAN_TEAL, ROYAL_PURPLE, FOREST_GREEN, SUNSET_ORANGE};

    private final String id;
    private final String displayName;
    private final Color brand;
    private final Color success;
    private final Color tint;

    private Theme(String id, String displayName, Color brand, Color success, Color tint) {
        this.id = id;
        this.displayName = displayName;
        this.brand = brand;
        this.success = success;
        this.tint = tint;
    }

    /** Stable identifier persisted in the settings table. */
    public String getId() {
        return id;
    }

    /** Human-readable name shown in the theme selector. */
    public String getDisplayName() {
        return displayName;
    }

    /** Primary accent colour (menu bar, borders, headings, icons). */
    public Color getBrand() {
        return brand;
    }

    /** Secondary action colour (positive buttons, banners, charts). */
    public Color getSuccess() {
        return success;
    }

    /** Light tint used for hover backgrounds and chart fills. */
    public Color getTint() {
        return tint;
    }

    /** A slightly darker shade of the brand for borders and hover states. */
    public Color getBrandDark() {
        return brand.darker();
    }

    /** A translucent version of the brand for subtle highlights. */
    public Color getBrandAlpha(int alpha) {
        return new Color(brand.getRed(), brand.getGreen(), brand.getBlue(), alpha);
    }

    /**
     * Looks up a theme by its persisted id, falling back to Ocean Teal when the
     * stored value is missing or unknown.
     */
    public static Theme byId(String id) {
        if (id != null) {
            for (Theme t : ALL) {
                if (t.id.equals(id)) {
                    return t;
                }
            }
        }
        return OCEAN_TEAL;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
