package dairy.erp.util;

import dairy.erp.config.AppConfig;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
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

    public static final Color BRAND = new Color(0x1a, 0x5f, 0x7a);
    public static final Color SUCCESS_GREEN = new Color(0x0b, 0x7a, 0x3e);

    private UIUtil() {
    }

    /**
     * Applies a uniform 18px font theme to every Swing component the app uses.
     * Call once at start-up before showing any frame so all panels inherit the
     * same look. Larger, bold button fonts also make buttons visibly bigger.
     */
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
    }

    /** Makes a button visibly larger. */
    public static void styleButton(JButton b) {
        Dimension d = b.getPreferredSize();
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(16, (int) b.getFont().getSize2D())));
        b.setPreferredSize(new Dimension(Math.max(d.width + 24, 130), Math.max(d.height + 30, 45)));
                b.setMinimumSize(new Dimension(120, 45));
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
    }

    /** Applies standard table styling: row height, font, header. */
    public static void styleTable(JTable table) {
        table.setRowHeight(32);
        table.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        header.setBackground(BRAND);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 36));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFillsViewportHeight(true);
    }

    /**
     * Applies the exact enhanced grid appearance used by Customer Details.
     * Reuse this method wherever a table must visually match that grid.
     */
    public static void styleCustomerDetailsTable(JTable table) {
        styleTable(table);
        table.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        table.setRowHeight(40);
        table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 19));
        table.setShowGrid(true);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);
        table.setGridColor(new Color(0xb9, 0xc6, 0xce));
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            private final Color even = new Color(0xff, 0xff, 0xff);
            private final Color odd = new Color(0xe8, 0xf1, 0xf7);
            private final Color selected = new Color(0xd2, 0xec, 0xd9);

            @Override
            public Component getTableCellRendererComponent(JTable source, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component component = super.getTableCellRendererComponent(
                        source, value, isSelected, hasFocus, row, column);
                if (isSelected) {
                    component.setBackground(selected);
                    component.setForeground(new Color(0x0b, 0x3a, 0x22));
                } else {
                    component.setBackground(row % 2 == 0 ? even : odd);
                    component.setForeground(source.getForeground());
                }
                setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                return component;
            }
        });
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
    public static void styleField(JTextField field, int columns) {
        field.setColumns(columns);
        field.setFont(field.getFont().deriveFont(FIELD_FONT_SIZE));
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
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        p.setBackground(new Color(0xf3, 0xf6, 0xf8));
        ImageIcon logo = loadLogoByHeight(100, 350);
        if (logo != null) {
            p.add(new JLabel(logo));
        }
        JLabel label = new JLabel(" " + title);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        label.setForeground(BRAND);
        p.add(label);
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
}
