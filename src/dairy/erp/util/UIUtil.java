package dairy.erp.util;

import dairy.erp.config.AppConfig;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;

/**
 * Small UI helpers shared across the Swing panels: auto-uppercase text fields,
 * an 18px global font theme, larger buttons and a reusable logo header so the
 * brand appears on every panel.
 */
public final class UIUtil {

    public static final float LABEL_FONT_SIZE = 18f;
    public static final float FIELD_FONT_SIZE = 18f;

    public static Color BRAND = new Color(0x1a, 0x5f, 0x7a);
    public static Color SUCCESS_GREEN = new Color(0x0b, 0x7a, 0x3e);

    /** Data tables that should be re-coloured live when the theme changes. */
    private static final java.util.List<JTable> TRACKED_TABLES =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    private UIUtil() {
    }

    /**
     * Applies a uniform 18px font theme to every Swing component the app uses.
     * Call once at start-up before showing any frame so all panels inherit the
     * same look. Larger, bold button fonts also make buttons visibly bigger.
     */
    /**
     * Applies a visual theme across the whole application: updates the static
     * {@link #BRAND} / {@link #SUCCESS_GREEN} accents, repaints the global
     * UIManager colours (menu/selection backgrounds), and fires a theme-changed
     * event so every open panel can re-colour itself immediately.
     *
     * @param theme the theme to apply
     */
    public static void applyTheme(Theme theme) {
        BRAND = theme.getBrand();
        SUCCESS_GREEN = theme.getSuccess();

        UIManager.put("Menu.selectionBackground", BRAND);
        UIManager.put("MenuItem.selectionBackground", BRAND);
        UIManager.put("ComboBox.selectionBackground", theme.getTint());

        // Re-colour every tracked data table's header and grid so the chosen
        // theme is applied live across all screens (row stripes are painted
        // from the brand at render time, so they follow automatically).
        for (JTable t : TRACKED_TABLES) {
            t.getTableHeader().setBackground(BRAND);
            t.getTableHeader().setForeground(Color.WHITE);
            t.setGridColor(tableGridColor());
            t.repaint();
        }

        // Let every registered panel repaint with the new colours.
        AppBus.fireThemeChanged(theme);
    }

    public static void applyGlobalFont() {
        Font label = new Font(Font.SANS_SERIF, Font.PLAIN, 18);
        Font button = new Font(Font.SANS_SERIF, Font.BOLD, 16);
        Font field = new Font(Font.SANS_SERIF, Font.PLAIN, 18);
        Font table = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
        Font header = new Font(Font.SANS_SERIF, Font.BOLD, 18);
        Font menu = new Font(Font.SANS_SERIF, Font.PLAIN, 16);

        UIManager.put("Label.font", label);
        UIManager.put("Button.font", button);
        UIManager.put("TextField.font", field);
        UIManager.put("PasswordField.font", field);
        UIManager.put("TextArea.font", field);
        UIManager.put("ComboBox.font", field);
        UIManager.put("RadioButton.font", label);
        UIManager.put("CheckBox.font", label);
        UIManager.put("TitledBorder.font", header);
        UIManager.put("TabbedPane.font", field);
        UIManager.put("Table.font", table);
        UIManager.put("TableHeader.font", header);
        UIManager.put("Menu.font", menu);
        UIManager.put("MenuItem.font", menu);
        // Modern flat theme touches: no dotted focus ring on buttons and
        // a soft selection colour for menu/combobox popups.
        UIManager.put("Button.focus", new Color(0, 0, 0, 0));
        UIManager.put("Menu.selectionBackground", BRAND);
        UIManager.put("MenuItem.selectionBackground", BRAND);
        UIManager.put("ComboBox.selectionBackground", new Color(0xdc, 0xe9, 0xef));
    }

    /**
     * Makes a button visibly larger with a modern flat theme: white surface,
     * brand-coloured text, rounded soft border and a hover highlight.
     */
    public static void styleButton(JButton b) {
        Dimension d = b.getPreferredSize();
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(16, (int) b.getFont().getSize2D())));
        b.setPreferredSize(new Dimension(Math.max(d.width + 24, 130), Math.max(d.height + 30, 45)));
                b.setMinimumSize(new Dimension(120, 45));
        b.setBackground(Color.WHITE);
        b.setForeground(BRAND);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xc9, 0xd3, 0xda), 1, true),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)));
        b.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        addHoverEffect(b);
    }

    /** Lightens a colour towards white (factor 0..1) for hover states. */
    public static Color lighten(Color c, float factor) {
        return new Color(
                Math.round(c.getRed() + (255 - c.getRed()) * factor),
                Math.round(c.getGreen() + (255 - c.getGreen()) * factor),
                Math.round(c.getBlue() + (255 - c.getBlue()) * factor));
    }

    /**
     * Accent-coloured quick-action button: light tinted background, coloured
     * border, dark text and an icon painted in the same accent colour, with a
     * hover highlight (used on the dashboard quick-action bar).
     */
    public static void styleAccentButton(JButton b, Color accent) {
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        b.setBackground(lighten(accent, 0.90f));
        b.setForeground(new Color(0x1f, 0x2d, 0x33));
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 1, true),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        b.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        final Color base = b.getBackground();
        final Color hover = lighten(accent, 0.76f);
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(hover);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(base);
            }
        });
    }

    /** Adds a subtle hover highlight: the button brightens while the mouse is over it. */
    private static void addHoverEffect(JButton b) {
        final Color base = b.getBackground();
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(lighten(base, 0.16f));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(base);
            }
        });
    }

    /** Styles a compact coloured action button (e.g. green Search, red Reset). */
    public static void styleSmallButton(JButton b, Color background) {
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        b.setBackground(background);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(background.darker()),
                BorderFactory.createEmptyBorder(5, 14, 5, 14)));
        b.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        addHoverEffect(b);
    }

    // ----- Theme-aware table colours -----

    /** Soft grid line colour derived from the current theme brand. */
    public static Color tableGridColor() {
        return lighten(BRAND, 0.55f);
    }

    /** Zebra stripe colour for odd rows, tinted from the theme brand. */
    public static Color tableOddRowColor() {
        return lighten(BRAND, 0.93f);
    }

    /** Light brand-tinted highlight for the currently selected row. */
    public static Color tableSelectedRowColor() {
        return lighten(BRAND, 0.85f);
    }

    /** Deeper brand highlight when the selected cell also has keyboard focus. */
    public static Color tableFocusedRowColor() {
        return lighten(BRAND, 0.72f);
    }

    /** Readable text colour on a selected row. */
    public static Color tableSelectedTextColor() {
        return BRAND.darker();
    }

    /** Light grey used for disabled / read-only form fields everywhere. */
    public static Color DISABLED_BG = new Color(0xEE, 0xEF, 0xF1);

    /**
     * Applies the shared read-only field look: light grey background (so every
     * disabled/auto-filled field reads as inactive across the whole app) on
     * top of the existing white rounded field styling.
     */
    public static void styleReadOnlyField(JTextField field) {
        field.setBackground(DISABLED_BG);
        field.setEditable(false);
        field.setFocusable(false);
    }

    /**
     * Shared zebra renderer. Colours are resolved at paint time from the
     * current {@link #BRAND}, so switching the theme re-colours stripes and
     * selection everywhere without rebuilding any table. The selected row
     * deepens when the cell has keyboard focus and a smooth brand border is
     * drawn around the focused cell so the cursor position is easy to follow.
     */
    public static javax.swing.table.DefaultTableCellRenderer zebraRenderer() {
        return new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable source, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component component = super.getTableCellRendererComponent(
                        source, value, isSelected, hasFocus, row, column);
                if (isSelected) {
                    component.setBackground(hasFocus ? tableFocusedRowColor() : tableSelectedRowColor());
                    component.setForeground(tableSelectedTextColor());
                } else {
                    component.setBackground(row % 2 == 0 ? Color.WHITE : tableOddRowColor());
                    component.setForeground(source.getForeground());
                }
                if (hasFocus) {
                    setBorder(BorderFactory.createLineBorder(BRAND, 2, true));
                } else {
                    setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                }
                return component;
            }
        };
    }

    /** Adds a table to the set that is re-coloured when the theme changes. */
    public static void trackTable(JTable table) {
        if (table != null && !TRACKED_TABLES.contains(table)) {
            TRACKED_TABLES.add(table);
        }
    }

    /**
     * Applies the shared data-table look: 15px data / 16px bold header font,
     * theme-coloured header and grid, zebra stripes and soft selection tint.
     * Applies to every table in the application so font size is uniform and
     * the colours always follow the chosen theme.
     */
    public static void styleTable(JTable table) {
        table.setRowHeight(32);
        table.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        header.setBackground(BRAND);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 34));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFillsViewportHeight(true);
        table.setShowGrid(true);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);
        table.setGridColor(tableGridColor());
        table.setIntercellSpacing(new Dimension(1, 1));
        // Register the selection colours so the selected row is always painted
        // with the light brand tint (and it deepens while the cell is focused).
        table.setSelectionBackground(tableSelectedRowColor());
        table.setSelectionForeground(tableSelectedTextColor());
        table.setDefaultRenderer(Object.class, zebraRenderer());
        // Repaint on focus gain/loss so the focused-row colour visibly changes
        // as the user navigates rows with the cursor or arrow keys.
        table.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                table.repaint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                table.repaint();
            }
        });
        trackTable(table);
    }

    /**
     * The enhanced grid used by Customer Details: a slightly taller variant of
     * the shared style so the zebra rows breathe a little more.
     */
    public static void styleCustomerDetailsTable(JTable table) {
        styleTable(table);
        table.setRowHeight(34);
    }


    /**
     * Restricts a text field to numeric input only: digits with at most one
     * decimal point (integer or fraction values). Alphabets, spaces, symbols
     * and multiple decimal points are rejected as the user types or pastes.
     */
    public static void allowDecimalOnly(JTextField field) {
        if (field.getDocument() instanceof AbstractDocument) {
            ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
                /** Only digits with a single optional decimal point are valid. */
                private boolean isValid(String text) {
                    return text.matches("\\d*(\\.\\d*)?");
                }

                private String currentText(FilterBypass fb) throws BadLocationException {
                    return fb.getDocument().getText(0, fb.getDocument().getLength());
                }

                @Override
                public void insertString(FilterBypass fb, int offset, String text, AttributeSet a)
                        throws BadLocationException {
                    if (text == null) {
                        return;
                    }
                    String candidate = currentText(fb);
                    candidate = candidate.substring(0, offset) + text
                            + candidate.substring(offset);
                    if (isValid(candidate)) {
                        fb.insertString(offset, text, a);
                    }
                }

                @Override
                public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet a)
                        throws BadLocationException {
                    if (text == null) {
                        return;
                    }
                    String candidate = currentText(fb);
                    candidate = candidate.substring(0, offset) + text
                            + candidate.substring(offset + length);
                    if (isValid(candidate)) {
                        fb.replace(offset, length, text, a);
                    }
                }
            });
        }
    }

    /** Makes a text field autouppercase as the user types. */
    public static void makeUpperCase(JTextField field) {
        if (field.getDocument() instanceof AbstractDocument) {
            ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset, String text, AttributeSet a)
                        throws BadLocationException {
                    fb.insertString(offset, text == null ? null : text.toUpperCase(), a);
                }

                @Override
                public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet a)
                        throws BadLocationException {
                    fb.replace(offset, length, text == null ? null : text.toUpperCase(), a);
                }
            });
        }
    }

    /** Applies a 16px font to a label. */
    public static void styleLabel(JLabel label) {
        label.setFont(label.getFont().deriveFont(LABEL_FONT_SIZE));
    }

    /** Applies a 16px font to a text field and widens it. */
    /**
     * Applies the shared form-field look used across every panel (matching the
     * Payment screen): 16px font, uniform 36px height, white background and a
     * soft rounded border. Columns still control the natural width, layouts
     * may stretch fields to fill their row.
     */
    public static void styleField(JTextField field, int columns) {
        field.setColumns(columns);
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        field.setBackground(Color.WHITE);
        field.setForeground(new Color(0x1F, 0x2D, 0x33));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xC9, 0xD3, 0xDA), 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        int prefW = field.getPreferredSize().width;
        field.setPreferredSize(new Dimension(prefW, 36));
        field.setMinimumSize(new Dimension(Math.max(110, prefW > 170 ? 150 : prefW), 36));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
    }

    /** Applies a uniform 16px font to a component (used for radio/combo etc). */
    public static void styleComponent(Component comp, float size) {
        if (comp instanceof JComponent) {
            Font f = comp.getFont();
            comp.setFont(f.deriveFont(size));
        } else {
            comp.setFont(comp.getFont().deriveFont(size));
        }
    }

    /**
     * Finds the best available logo file (banner jpeg first, then png
     * variants), returning the decoded image or {@code null}.
     */
    private static java.awt.image.BufferedImage readLogoImage() {
        String[] candidates = {
                 "resources/images/logo.png",
                 
        };
        for (String path : candidates) {
            try {
                java.io.File f = AppConfig.getBaseDir().resolve(path).toFile();
                if (f.exists()) {
                    java.awt.image.BufferedImage bi = javax.imageio.ImageIO.read(f);
                    if (bi != null) {
                        return bi;
                    }
                }
            } catch (Exception ignored) {
                // try the next candidate
            }
        }
        return null;
    }

    /** Loads the logo scaled to the given width, keeping its aspect ratio. */
    public static ImageIcon loadLogo(int width) {
        java.awt.image.BufferedImage bi = readLogoImage();
        if (bi == null) {
            return null;
        }
        double ratio = (double) bi.getHeight(null) / bi.getWidth(null);
        int targetHeight = Math.max(1, (int) Math.round(width * ratio));
        return new ImageIcon(bi.getScaledInstance(width, targetHeight, Image.SCALE_SMOOTH));
    }

    /**
     * Loads the logo scaled to the given height (width follows the aspect
     * ratio, capped at maxWidth) so wide banner logos stay clearly visible.
     */
    public static ImageIcon loadLogoByHeight(int height, int maxWidth) {
        java.awt.image.BufferedImage bi = readLogoImage();
        if (bi == null) {
            return null;
        }
        double ratio = (double) bi.getWidth(null) / bi.getHeight(null);
        int targetWidth = (int) Math.round(height * ratio);
        if (targetWidth > maxWidth) {
            targetWidth = maxWidth;
        }
        return new ImageIcon(bi.getScaledInstance(targetWidth, height, Image.SCALE_SMOOTH));
    }

    /**
     * Builds a compact header used at the top-left of every panel: the logo (if
     * available) plus the panel title, so the branding sits above the fields on
     * the left-hand side of the two-column layout.
     */
    public static JPanel header(String title) {
        // No coloured banner strip and no in-panel menu name any more — the
        // selected menu name is shown in the blue banner under the menu bar,
        // and the dairy name sits at the header row's top-right corner.
        // Only the logo is displayed (the title parameter is kept so all
        // existing call sites continue to work unchanged).
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        p.setOpaque(false);
        ImageIcon logo = loadLogoByHeight(100, 350);
        if (logo != null) {
            p.add(new JLabel(logo));
        }
        p.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        return p;
    }

    /**
     * Returns the title label inside a header built by {@link #header(String)}
     * so screens can update their heading text dynamically (e.g. switching
     * between "New Collection" and "Collection History").
     */
    public static javax.swing.JLabel headerTitle(JPanel header) {
        for (java.awt.Component c : header.getComponents()) {
            if (c instanceof javax.swing.JLabel
                    && ((javax.swing.JLabel) c).getText() != null
                    && !((javax.swing.JLabel) c).getText().isBlank()) {
                return (javax.swing.JLabel) c;
            }
        }
        return null;
    }

    /**
     * A green banner that collapses to zero height while its text is empty,
     * so an unused banner does not push the form fields down. Once a customer
     * is searched and the text is set, it regains its normal size.
     */
    public static JLabel collapsibleGreenBanner() {
        JLabel banner = greenBanner("");
        // Start collapsed (empty text).
        banner.setPreferredSize(new Dimension(1, 0));
        banner.addPropertyChangeListener("text", e -> {
            boolean empty = banner.getText() == null || banner.getText().isBlank();
            banner.setPreferredSize(empty ? new Dimension(1, 0) : new Dimension(420, 42));
            banner.revalidate();
            banner.repaint();
        });
        return banner;
    }

    /**
     * Builds a read-only, centered green banner used to show the searched
     * customer name (and mobile number) at the top centre of a panel.
     */
    public static JLabel greenBanner(String text) {
        JLabel label = new JLabel(text == null ? "" : text);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        label.setForeground(SUCCESS_GREEN);
        label.setHorizontalAlignment(JLabel.CENTER);
        label.setPreferredSize(new Dimension(420, 42));
        return label;
    }

    /** Puts a component inside a titled border for visual grouping. */
    public static JPanel titled(Component inner, String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.setOpaque(false);
        p.add(inner, BorderLayout.CENTER);
        return p;
    }

    /** Grey used for small trailing field icons (pin, phone, envelope, eye). */
    public static Color fieldIconGrey() {
        return new Color(0x5F, 0x6B, 0x76);
    }

    /**
     * Generic card background shared by all screens (white, matching the
     * Customer Details cards).
     */
    public static Color cardBackground() {
        return Color.WHITE;
    }

    /**
     * Generic rounded card border shared by all screens: soft grey line
     * plus inner padding.
     */
    public static javax.swing.border.Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xC9, 0xD3, 0xDA), 1, true),
                BorderFactory.createEmptyBorder(8, 14, 10, 14));
    }

    /**
     * Wraps a password field with an eye toggle at its right edge: clicking
     * the eye shows the characters in plain text (open-eye becomes a crossed
     * eye) and clicking again hides them. The field keeps whatever styling
     * the caller already applied; the wrapper only adds the soft border and
     * the clickable icon.
     */
    public static javax.swing.JComponent passwordWithEye(javax.swing.JPasswordField field) {
        field.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 4));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Color.WHITE);
        wrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xC9, 0xD3, 0xDA), 1, true),
                BorderFactory.createEmptyBorder(1, 1, 1, 8)));

        JLabel eye = new JLabel(ButtonIcons.of("Eye", fieldIconGrey()));
        eye.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        final char originalEcho = field.getEchoChar();
        final boolean[] visible = {false};
        eye.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                visible[0] = !visible[0];
                if (visible[0]) {
                    field.setEchoChar((char) 0);
                    eye.setIcon(ButtonIcons.of("EyeOff", fieldIconGrey()));
                } else {
                    field.setEchoChar(originalEcho);
                    eye.setIcon(ButtonIcons.of("Eye", fieldIconGrey()));
                }
            }
        });
        wrap.add(field, BorderLayout.CENTER);
        wrap.add(eye, BorderLayout.EAST);
        return wrap;
    }

    // ==================== Generic dialogs ====================

    /**
     * Shows the single shared styled dialog used across the whole application.
     * Only the title, message and button(s) differ per operation; the visual
     * design (coloured header banner, message body, icon'd buttons) stays the
     * same so every screen presents a consistent look.
     *
     * <p>The {@code type} accepts the standard {@link JOptionPane} type
     * constants ({@code INFORMATION_MESSAGE}, {@code WARNING_MESSAGE},
     * {@code ERROR_MESSAGE}) and selects the header colour and icon.
     */
    public static void showMessage(Component parent, Object message, String title, int type) {
        runDialog(parent, title, message, type, false);
    }

    /**
     * Shows the shared styled Yes/No confirmation dialog. Returns
     * {@link JOptionPane#YES_OPTION} when Yes is chosen, otherwise
     * {@link JOptionPane#NO_OPTION} (including when the window is closed).
     */
    public static int confirm(Component parent, Object message, String title) {
        return runDialog(parent, title, message, JOptionPane.QUESTION_MESSAGE, true);
    }

    /** Header colour for each dialog type. */
    private static Color dialogHeaderColor(int type) {
        if (type == JOptionPane.ERROR_MESSAGE) {
            return new Color(0xC6, 0x28, 0x28);
        }
        if (type == JOptionPane.WARNING_MESSAGE) {
            return new Color(0xEF, 0x6C, 0x00);
        }
        return BRAND;
    }

    private static int runDialog(Component parent, String title, Object message,
            int type, boolean confirm) {
        java.awt.Frame owner = parent == null ? null
                : (java.awt.Frame) SwingUtilities.getWindowAncestor(parent);

        final JDialog dlg = new JDialog(owner, true);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dlg.setUndecorated(false);

        // ---- Coloured header banner with an icon + the dialog title ----
        Color head = dialogHeaderColor(type);
        String iconName = confirm ? "Question"
                : (type == JOptionPane.ERROR_MESSAGE ? "Error"
                : (type == JOptionPane.WARNING_MESSAGE ? "Warning" : "Info"));

        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(head);
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JLabel iconLbl = new JLabel(ButtonIcons.of(iconName, Color.WHITE));
        JLabel titleLbl = new JLabel(title == null ? "" : title);
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        header.add(iconLbl, BorderLayout.WEST);
        header.add(titleLbl, BorderLayout.CENTER);

        // ---- Message body (wrapped, scrollable for long validation lists) ----
        JTextArea body = new JTextArea(message == null ? "" : String.valueOf(message));
        body.setEditable(false);
        body.setOpaque(false);
        body.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        body.setForeground(new Color(0x26, 0x32, 0x38));
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(430, 130));
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);

        // ---- Icon'd buttons: OK/Yes (green) and No (slate) ----
        final int[] choice = {JOptionPane.NO_OPTION};
        JButton ok = new JButton(confirm ? "Yes" : "OK", ButtonIcons.of("Check", Color.WHITE));
        styleSmallButton(ok, SUCCESS_GREEN);
        ok.setPreferredSize(new Dimension(120, 40));
        ok.addActionListener(e -> {
            choice[0] = JOptionPane.YES_OPTION;
            dlg.dispose();
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        buttons.setOpaque(false);
        buttons.add(ok);
        if (confirm) {
            JButton no = new JButton("No", ButtonIcons.of("Cross", Color.WHITE));
            styleSmallButton(no, new Color(0x60, 0x7D, 0x8B));
            no.setPreferredSize(new Dimension(100, 40));
            no.addActionListener(e -> {
                choice[0] = JOptionPane.NO_OPTION;
                dlg.dispose();
            });
            buttons.add(no);
        }
        // Closing via the title-bar X counts as "No".
        dlg.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                choice[0] = JOptionPane.NO_OPTION;
            }
        });

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(header, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.pack();
        dlg.setLocationRelativeTo(owner);
        dlg.setResizable(false);
        dlg.setVisible(true);
        return choice[0];
    }
}
