package dairy.erp.ui;

import dairy.erp.service.SettingsService;
import dairy.erp.ui.dialogs.PasswordChangeDialog;
import dairy.erp.util.AppBus;
import dairy.erp.util.ButtonIcons;
import dairy.erp.util.Theme;
import dairy.erp.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Map;

/**
 * Settings screen rebuilt to the approved design: a logo header, three white
 * cards side by side — Dairy Information, Application Settings and FAT / SNF
 * Settings — each with a bold slate heading above it and light tinted fields,
 * plus a centred Save Settings / Change Password action row underneath.
 * <p>
 * Behaviour is unchanged: every value loads from and saves through
 * {@link SettingsService}, the dairy name is broadcast on save, the selected
 * theme applies live, and Change Password opens the existing dialog.
 */
public class SettingsPanel extends JPanel {

    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    private static final Font FIELD_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
    private static final Color TEXT_DARK = new Color(0x1F, 0x2D, 0x33);
    private static final Color HEADING = new Color(0x2E, 0x40, 0x53);
    private static final Color FIELD_BG = new Color(0xF6, 0xF9, 0xFB);
    private static final Color FIELD_BORDER = new Color(0xC9, 0xD3, 0xDA);
    private static final Color CARD_BORDER = new Color(0xD0, 0xD7, 0xDE);
    private static final Color HINT_GREY = new Color(0x9A, 0xA7, 0xB3);
    private static final Color ICON_GREY = new Color(0x5F, 0x6B, 0x76);
    private static final Color SAVE_TEAL = new Color(0x1F, 0x6E, 0x5B);
    private static final Color PASS_BLUE = new Color(0x64, 0xB5, 0xF6);
    private static final String GST_HINT = "GST Number (preservdor)";

    private final SettingsService settingsService = new SettingsService();
    private final String username;

    // Dairy information
    private final JTextField dairyName = new JTextField();
    private final JTextField dairyOwner = new JTextField();
    private final JTextField dairyAddress = new JTextField();
    private final JTextField dairyMobile = new JTextField();
    private final JTextField dairyEmail = new JTextField();
    private final JTextField dairyGst = new JTextField();

    // Application settings
    private final JTextField dateFormat = new JTextField();
    private final JTextField currencySymbol = new JTextField();
    private final JTextField decimalPlaces = new JTextField();
    private final JComboBox<String> defaultShift = new JComboBox<>(new String[]{"Morning", "Evening"});
    private final JComboBox<String> defaultMilkType = new JComboBox<>(new String[]{"Cow", "Buffalo", "Mix"});
    private final JComboBox<String> allowRateAdjustment = new JComboBox<>(new String[]{"false", "true"});
    private final JComboBox<String> allowDateAdjustment = new JComboBox<>(new String[]{"false", "true"});
    private final JComboBox<Theme> themeSelector = new JComboBox<>(Theme.ALL);

    // FAT/SNF bounds
    private final JTextField fatMin = new JTextField();
    private final JTextField fatMax = new JTextField();
    private final JTextField snfMin = new JTextField();
    private final JTextField snfMax = new JTextField();
    private final JTextField fatMinCow = new JTextField();
    private final JTextField fatMaxCow = new JTextField();
    private final JTextField fatMinBuf = new JTextField();
    private final JTextField fatMaxBuf = new JTextField();
    private final JTextField fatMinMix = new JTextField();
    private final JTextField fatMaxMix = new JTextField();

    public SettingsPanel(String username) {
        super(new BorderLayout(0, 8));
        this.username = username;
        setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        add(UIUtil.header("Settings"), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);
        center.add(buildCards(), BorderLayout.NORTH);
        // The action buttons sit directly under the cards, near the top of
        // the space left below them (as in the design).
        JPanel below = new JPanel(new BorderLayout());
        below.setOpaque(false);
        below.add(buildButtonArea(), BorderLayout.NORTH);
        center.add(below, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        loadSettings();
    }

    /** The three white section cards laid out in one equal-width row. */
    private JPanel buildCards() {
        JPanel cards = new JPanel(new GridBagLayout());
        cards.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 6, 2, 6);
        g.fill = GridBagConstraints.BOTH;
        g.weightx = 1.0;
        g.weighty = 1.0;
        g.gridx = 0;
        cards.add(dairyCard(), g);
        g.gridx = 1;
        cards.add(appCard(), g);
        g.gridx = 2;
        cards.add(fatSnfCard(), g);
        return cards;
    }

    private JComponent dairyCard() {
        JPanel rows = new JPanel(new GridBagLayout());
        rows.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        int row = 0;
        row = addFieldRow(rows, g, row, "Dairy Name:", styledField(dairyName));
        row = addFieldRow(rows, g, row, "Owner Name:", styledField(dairyOwner));
        row = addFieldRow(rows, g, row, "Address:", fieldWithIcon(dairyAddress, "MapPin"));
        row = addFieldRow(rows, g, row, "Mobile:", fieldWithIcon(dairyMobile, "Phone"));
        row = addFieldRow(rows, g, row, "Email:", fieldWithIcon(dairyEmail, "Envelope"));
        addFieldRow(rows, g, row, "GST Number:", gstField());
        return card("Dairy Information", rows);
    }

    private JComponent appCard() {
        JPanel rows = new JPanel(new GridBagLayout());
        rows.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        int row = 0;
        row = addFieldRow(rows, g, row, "Date Format:", styledField(dateFormat));
        row = addFieldRow(rows, g, row, "Currency Symbol:", styledField(currencySymbol));
        row = addFieldRow(rows, g, row, "Decimal Places:", styledField(decimalPlaces));
        row = addFieldRow(rows, g, row, "Default Shift:", styledCombo(defaultShift));
        row = addFieldRow(rows, g, row, "Default Milk Type:", styledCombo(defaultMilkType));
        row = addFieldRow(rows, g, row, "Allow Rate Adjustment:", styledCombo(allowRateAdjustment));
        row = addFieldRow(rows, g, row, "Allow Collection Date Adjustment:", styledCombo(allowDateAdjustment));
        addFieldRow(rows, g, row, "App Theme:", styledCombo(themeSelector));
        return card("Application Settings", rows);
    }

    private JComponent fatSnfCard() {
        JPanel rows = new JPanel(new GridBagLayout());
        rows.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        int row = 0;
        row = addFieldRowAt(rows, g, row, 0, "Minimum FAT:", styledField(fatMin));
        addFieldRowAt(rows, g, row - 1, 1, "Maximum FAT:", styledField(fatMax));
        row = addFieldRowAt(rows, g, row, 0, "Minimum SNF:", styledField(snfMin));
        addFieldRowAt(rows, g, row - 1, 1, "Maximum SNF:", styledField(snfMax));
        row = addFieldRowAt(rows, g, row, 0, "Cow FAT Min:", styledField(fatMinCow));
        addFieldRowAt(rows, g, row - 1, 1, "Cow FAT Max:", styledField(fatMaxCow));
        row = addFieldRowAt(rows, g, row, 0, "Buffalo FAT Min:", styledField(fatMinBuf));
        addFieldRowAt(rows, g, row - 1, 1, "Buffalo FAT Max:", styledField(fatMaxBuf));
        row = addFieldRowAt(rows, g, row, 0, "Mix FAT Min:", styledField(fatMinMix));
        addFieldRowAt(rows, g, row - 1, 1, "Mix FAT Max:", styledField(fatMaxMix));
        return card("FAT / SNF Settings", rows);
    }

    /** A white card with a rounded light border and its heading above it. */
    private JPanel card(String title, JComponent content) {
        JPanel outer = new JPanel(new BorderLayout(0, 4));
        outer.setOpaque(false);
        JLabel heading = new JLabel(title);
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        heading.setForeground(HEADING);
        heading.setBorder(BorderFactory.createEmptyBorder(2, 4, 0, 4));
        outer.add(heading, BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        body.add(content, BorderLayout.NORTH);
        outer.add(body, BorderLayout.CENTER);
        return outer;
    }

    /** Label-above-field row in the single-column cards. */
    private int addFieldRow(JPanel panel, GridBagConstraints g, int row,
            String label, JComponent field) {
        g.insets = new Insets(3, 2, 1, 8);
        g.gridx = 0;
        g.gridy = row * 2;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        panel.add(fieldLabel(label), g);
        g.insets = new Insets(1, 2, 5, 8);
        g.gridy = row * 2 + 1;
        panel.add(field, g);
        return row + 1;
    }

    /** Label-above-field row in the two-column FAT / SNF card. */
    private int addFieldRowAt(JPanel panel, GridBagConstraints g, int row, int col,
            String label, JComponent field) {
        g.insets = new Insets(3, 2, 1, 8);
        g.gridx = col;
        g.gridy = row * 2;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        panel.add(fieldLabel(label), g);
        g.insets = new Insets(1, 2, 5, 8);
        g.gridy = row * 2 + 1;
        panel.add(field, g);
        return row + 1;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(LABEL_FONT);
        l.setForeground(TEXT_DARK);
        return l;
    }

    /** Shared white rounded field, identical to the rest of the application. */
    private JTextField styledField(JTextField field) {
        UIUtil.styleField(field, 16);
        return field;
    }

    private JComboBox<?> styledCombo(JComboBox<?> combo) {
        combo.setFont(FIELD_FONT);
        combo.setBackground(FIELD_BG);
        return combo;
    }

    /** Field wrapped so a small grey trailing icon sits inside its right edge. */
    private JComponent fieldWithIcon(JTextField field, String iconKey) {
        styledField(field);
        field.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 4));
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(FIELD_BG);
        wrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(1, 1, 1, 8)));
        JLabel icon = new JLabel(ButtonIcons.of(iconKey, ICON_GREY));
        icon.setOpaque(false);
        wrap.add(field, BorderLayout.CENTER);
        wrap.add(icon, BorderLayout.EAST);
        return wrap;
    }

    /**
     * GST field showing the design's grey placeholder hint while empty; the
     * hint itself is never saved (see {@link #gstValue()}).
     */
    private JComponent gstField() {
        styledField(dairyGst);
        dairyGst.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (GST_HINT.equals(dairyGst.getText())) {
                    dairyGst.setText("");
                    dairyGst.setForeground(TEXT_DARK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (dairyGst.getText().isBlank()) {
                    dairyGst.setText(GST_HINT);
                    dairyGst.setForeground(HINT_GREY);
                }
            }
        });
        return dairyGst;
    }

    /** Centred action row: dark-teal Save Settings and light-blue Change Password. */
    private JPanel buildButtonArea() {
        JPanel p = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 14, 4));
        p.setOpaque(false);

        JButton save = new JButton("Save Settings", ButtonIcons.of("Save", Color.WHITE));
        UIUtil.styleSmallButton(save, SAVE_TEAL);
        save.addActionListener(e -> saveSettings());
        p.add(save);

        JButton changePass = new JButton("Change Password", ButtonIcons.of("Key", TEXT_DARK));
        UIUtil.styleAccentButton(changePass, PASS_BLUE);
        changePass.setForeground(TEXT_DARK);
        changePass.addActionListener(e ->
                new PasswordChangeDialog(findOwner(), username).setVisible(true));
        p.add(changePass);

        return p;
    }

    private void loadSettings() {
        Map<String, String> s = settingsService.allSettings();
        setText(dairyName, s.get("dairy.name"));
        setText(dairyOwner, s.get("dairy.owner"));
        setText(dairyAddress, s.get("dairy.address"));
        setText(dairyMobile, s.get("dairy.mobile"));
        setText(dairyEmail, s.get("dairy.email"));
        if (s.get("dairy.gst") == null || s.get("dairy.gst").isBlank()) {
            dairyGst.setText(GST_HINT);
            dairyGst.setForeground(HINT_GREY);
        } else {
            dairyGst.setText(s.get("dairy.gst"));
            dairyGst.setForeground(TEXT_DARK);
        }
        setText(dateFormat, s.get("app.date_format"));
        setText(currencySymbol, s.get("app.currency_symbol"));
        setText(decimalPlaces, s.get("app.decimal_places"));
        defaultShift.setSelectedItem(s.getOrDefault("app.default_shift", "Morning"));
        defaultMilkType.setSelectedItem(s.getOrDefault("app.default_milk_type", "Cow"));
        allowRateAdjustment.setSelectedItem(s.getOrDefault("app.allow_rate_adjustment", "false"));
        allowDateAdjustment.setSelectedItem(s.getOrDefault("app.allow_date_adjustment", "false"));
        themeSelector.setSelectedItem(Theme.byId(s.get("app.theme")));
        setText(fatMin, s.get("fat.min"));
        setText(fatMax, s.get("fat.max"));
        setText(snfMin, s.get("snf.min"));
        setText(snfMax, s.get("snf.max"));
        setText(fatMinCow, s.get("fat.min_cow"));
        setText(fatMaxCow, s.get("fat.max_cow"));
        setText(fatMinBuf, s.get("fat.min_buffalo"));
        setText(fatMaxBuf, s.get("fat.max_buffalo"));
        setText(fatMinMix, s.get("fat.min_mix"));
        setText(fatMaxMix, s.get("fat.max_mix"));
    }

    private void saveSettings() {
        try {
            settingsService.saveAll(mapOf(
                    "dairy.name", dairyName.getText(), "dairy.owner", dairyOwner.getText(),
                    "dairy.address", dairyAddress.getText(), "dairy.mobile", dairyMobile.getText(),
                    "dairy.email", dairyEmail.getText(), "dairy.gst", gstValue(),
                    "app.date_format", dateFormat.getText(), "app.currency_symbol", currencySymbol.getText(),
                    "app.decimal_places", decimalPlaces.getText(),
                    "app.default_shift", (String) defaultShift.getSelectedItem(),
                    "app.default_milk_type", (String) defaultMilkType.getSelectedItem(),
                    "app.allow_rate_adjustment", (String) allowRateAdjustment.getSelectedItem(),
                    "app.allow_date_adjustment", (String) allowDateAdjustment.getSelectedItem(),
                    "app.theme", ((Theme) themeSelector.getSelectedItem()).getId(),
                    "fat.min", fatMin.getText(), "fat.max", fatMax.getText(),
                    "snf.min", snfMin.getText(), "snf.max", snfMax.getText(),
                    "fat.min_cow", fatMinCow.getText(), "fat.max_cow", fatMaxCow.getText(),
                    "fat.min_buffalo", fatMinBuf.getText(), "fat.max_buffalo", fatMaxBuf.getText(),
                    "fat.min_mix", fatMinMix.getText(), "fat.max_mix", fatMaxMix.getText()));
            UIUtil.showMessage(this, "Settings saved successfully.", "Settings",
                    JOptionPane.INFORMATION_MESSAGE);
            // Push the new dairy name to every open screen immediately.
            AppBus.fireDairyNameChanged(dairyName.getText());
            // Apply the selected theme live across the whole application.
            UIUtil.applyTheme((Theme) themeSelector.getSelectedItem());
        } catch (RuntimeException ex) {
            UIUtil.showMessage(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** The GST value to persist — the placeholder hint counts as empty. */
    private String gstValue() {
        String t = dairyGst.getText();
        return GST_HINT.equals(t) ? "" : t;
    }

    private Map<String, String> mapOf(String... kv) {
        Map<String, String> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(kv[i], kv[i + 1]);
        }
        return map;
    }

    private void setText(JTextField field, String value) {
        field.setText(value == null ? "" : value);
    }

    private java.awt.Frame findOwner() {
        java.awt.Container c = this;
        while (c != null && !(c instanceof JFrame)) {
            c = c.getParent();
        }
        return (java.awt.Frame) c;
    }
}