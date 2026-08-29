package dairy.erp.ui;

import dairy.erp.service.SettingsService;
import dairy.erp.ui.dialogs.PasswordChangeDialog;
import dairy.erp.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.util.Map;

/**
 * Settings screen for dairy information, application behaviour, FAT/SNF
 * validation bounds and password change.
 */
public class SettingsPanel extends JPanel {

    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    private static final Color SECTION_TITLE = new Color(0x1a, 0x5f, 0x7a);

    private final SettingsService settingsService = new SettingsService();
    private final String username;

    // Dairy information
    private final JTextField dairyName = new JTextField(20);
    private final JTextField dairyOwner = new JTextField(20);
    private final JTextField dairyAddress = new JTextField(30);
    private final JTextField dairyMobile = new JTextField(15);
    private final JTextField dairyEmail = new JTextField(20);
    private final JTextField dairyGst = new JTextField(15);

    // Application settings
    private final JTextField dateFormat = new JTextField(10);
    private final JTextField currencySymbol = new JTextField(6);
    private final JTextField decimalPlaces = new JTextField(4);
    private final JComboBox<String> defaultShift = new JComboBox<>(new String[]{"Morning", "Evening"});
    private final JComboBox<String> defaultMilkType = new JComboBox<>(new String[]{"Cow", "Buffalo", "Mix"});
    private final JComboBox<String> manualRateOverride = new JComboBox<>(new String[]{"false", "true"});

    // FAT/SNF bounds
    private final JTextField fatMin = new JTextField(6);
    private final JTextField fatMax = new JTextField(6);
    private final JTextField snfMin = new JTextField(6);
    private final JTextField snfMax = new JTextField(6);
    private final JTextField fatMinCow = new JTextField(6);
    private final JTextField fatMaxCow = new JTextField(6);
    private final JTextField fatMinBuf = new JTextField(6);
    private final JTextField fatMaxBuf = new JTextField(6);
    private final JTextField fatMinMix = new JTextField(6);
    private final JTextField fatMaxMix = new JTextField(6);

    public SettingsPanel(String username) {
        super(new BorderLayout(8, 8));
        this.username = username;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        styleInputs();
        add(buildTop(), BorderLayout.CENTER);
        loadSettings();
    }

    /** Applies the same 18px field styling used on every other screen. */
    private void styleInputs() {
        UIUtil.styleField(dairyName, 20);
        UIUtil.styleField(dairyOwner, 20);
        UIUtil.styleField(dairyAddress, 24);
        UIUtil.styleField(dairyMobile, 15);
        UIUtil.styleField(dairyEmail, 20);
        UIUtil.styleField(dairyGst, 15);
        UIUtil.styleField(dateFormat, 10);
        UIUtil.styleField(currencySymbol, 6);
        UIUtil.styleField(decimalPlaces, 4);
        UIUtil.styleField(fatMin, 6);
        UIUtil.styleField(fatMax, 6);
        UIUtil.styleField(snfMin, 6);
        UIUtil.styleField(snfMax, 6);
        UIUtil.styleField(fatMinCow, 6);
        UIUtil.styleField(fatMaxCow, 6);
        UIUtil.styleField(fatMinBuf, 6);
        UIUtil.styleField(fatMaxBuf, 6);
        UIUtil.styleField(fatMinMix, 6);
        UIUtil.styleField(fatMaxMix, 6);
        UIUtil.styleComponent(defaultShift, 18);
        UIUtil.styleComponent(defaultMilkType, 18);
        UIUtil.styleComponent(manualRateOverride, 18);
    }

    private JPanel buildTop() {
        JPanel wrap = new JPanel(new BorderLayout(8, 6));
        wrap.add(UIUtil.header("Application Settings"), BorderLayout.NORTH);

        // Top half of the screen holds the three section panels;
        // the remaining bottom area is a dedicated button area.
        int halfHeight = Toolkit.getDefaultToolkit().getScreenSize().height / 2 - 40;

        JPanel sections = buildContent();
        Dimension preferred = sections.getPreferredSize();
        sections.setPreferredSize(new Dimension(preferred.width, halfHeight));

        // vgap=12 keeps the three panels 12px above the button area
        // (6px original gap + 6px extra top padding for the buttons).
        JPanel center = new JPanel(new BorderLayout(8, 12));
        center.add(sections, BorderLayout.NORTH);
        center.add(buildButtonArea(), BorderLayout.CENTER);

        wrap.add(center, BorderLayout.CENTER);
        return wrap;
    }


    private JPanel buildContent() {
        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        //g.insets = new Insets(25, 10, 10, 10); 
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.VERTICAL;

        JPanel dairy = section("Dairy Information");
        addField(dairy, 0, "Dairy Name:", dairyName);
        addField(dairy, 1, "Owner Name:", dairyOwner);
        addField(dairy, 2, "Address:", dairyAddress);
        addField(dairy, 3, "Mobile:", dairyMobile);
        addField(dairy, 4, "Email:", dairyEmail);
        addField(dairy, 5, "GST Number:", dairyGst);
        addVerticalFiller(dairy, 12, 1);

        JPanel app = section("Application Settings");
        addField(app, 0, "Date Format:", dateFormat);
        addField(app, 1, "Currency Symbol:", currencySymbol);
        addField(app, 2, "Decimal Places:", decimalPlaces);
        addFieldCombo(app, 3, "Default Shift:", defaultShift);
        addFieldCombo(app, 4, "Default Milk Type:", defaultMilkType);
        addFieldCombo(app, 5, "Manual Rate Override:", manualRateOverride);
        addVerticalFiller(app, 12, 1);

        JPanel fatSnf = section("FAT / SNF Settings");
        addFieldAt(fatSnf, 0, 0, "Minimum FAT:", fatMin);
        addFieldAt(fatSnf, 0, 1, "Maximum FAT:", fatMax);
        addFieldAt(fatSnf, 1, 0, "Minimum SNF:", snfMin);
        addFieldAt(fatSnf, 1, 1, "Maximum SNF:", snfMax);
        addFieldAt(fatSnf, 2, 0, "Cow FAT Min:", fatMinCow);
        addFieldAt(fatSnf, 2, 1, "Cow FAT Max:", fatMaxCow);
        addFieldAt(fatSnf, 3, 0, "Buffalo FAT Min:", fatMinBuf);
        addFieldAt(fatSnf, 3, 1, "Buffalo FAT Max:", fatMaxBuf);
        addFieldAt(fatSnf, 4, 0, "Mix FAT Min:", fatMinMix);
        addFieldAt(fatSnf, 4, 1, "Mix FAT Max:", fatMaxMix);
        addVerticalFiller(fatSnf, 10, 2);

        // Three equal panels side by side: each section gets exactly one
        // third of the width and the full height, so the areas are equal.
        g.gridx = 0; g.gridy = 0; g.gridwidth = 1;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.weightx = 1.0; g.weighty = 1.0; g.fill = GridBagConstraints.BOTH;
        content.add(dairy, g);
        g.gridx = 1;
        content.add(app, g);
        g.gridx = 2;
        content.add(fatSnf, g);
        return content;
    }

    /** Invisible filler that keeps a section's rows top-aligned when the section grows. */
    private void addVerticalFiller(JPanel section, int row, int gridwidth) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = row; g.gridwidth = gridwidth;
        g.weightx = 1.0; g.weighty = 1.0;
        g.fill = GridBagConstraints.BOTH;
        section.add(new JLabel(" "), g);
    }

    private JPanel section(String title) {
        JPanel p = new JPanel(new GridBagLayout());
        javax.swing.border.TitledBorder tb = BorderFactory.createTitledBorder(title);
        tb.setTitleFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        tb.setTitleColor(SECTION_TITLE);
        p.setBorder(tb);
        return p;
    }

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(LABEL_FONT);
        return l;
    }

    /**
     * Vertical mode: the label sits directly ABOVE its field (one label per
     * field row pair), and the field stretches to the full section width.
     */
    private void addField(JPanel panel, int row, String label, javax.swing.JComponent field) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; g.gridy = row * 2; g.gridwidth = 1;
        g.weightx = 1.0; g.fill = GridBagConstraints.HORIZONTAL;
        panel.add(styledLabel(label), g);
        g.gridy = row * 2 + 1;
        panel.add(field, g);
    }

    /**
     * Two-column variant of the vertical mode: each column keeps its own
     * label above its field, and both columns share the width equally.
     */
    private void addFieldAt(JPanel panel, int row, int col, String label, javax.swing.JComponent field) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        g.gridx = col; g.gridy = row * 2; g.gridwidth = 1;
        g.weightx = 1.0; g.fill = GridBagConstraints.HORIZONTAL;
        panel.add(styledLabel(label), g);
        g.gridy = row * 2 + 1;
        panel.add(field, g);
    }

    /** Combo-box variant of the vertical label-above-field layout. */
    private void addFieldCombo(JPanel panel, int row, String label, JComboBox<?> box) {
        addField(panel, row, label, box);
    }

    /**
     * Bottom area of the screen: large action buttons centered, filling the
     * space below the three section panels.
     */
    private JPanel buildButtonArea() {
        // Extra 5px top padding so the buttons always keep a visible gap
        // below the form fields above.
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        p.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        // Same button size, font, colours and border style as the
        // Customer Details page — styleSmallButton defaults, no custom size.
        JButton save = new JButton("Save Settings");
        UIUtil.styleSmallButton(save, new Color(0x2E7D32)); // green, app standard
        save.addActionListener(e -> saveSettings());
        p.add(save);

        JButton changePass = new JButton("Change Password");
        UIUtil.styleSmallButton(changePass, new Color(0x1976D2)); // blue, app standard
        changePass.addActionListener(e -> new PasswordChangeDialog(findOwner(), username).setVisible(true));
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
        setText(dairyGst, s.get("dairy.gst"));
        setText(dateFormat, s.get("app.date_format"));
        setText(currencySymbol, s.get("app.currency_symbol"));
        setText(decimalPlaces, s.get("app.decimal_places"));
        defaultShift.setSelectedItem(s.getOrDefault("app.default_shift", "Morning"));
        defaultMilkType.setSelectedItem(s.getOrDefault("app.default_milk_type", "Cow"));
        manualRateOverride.setSelectedItem(s.getOrDefault("app.manual_rate_override", "false"));
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
                    "dairy.email", dairyEmail.getText(), "dairy.gst", dairyGst.getText(),
                    "app.date_format", dateFormat.getText(), "app.currency_symbol", currencySymbol.getText(),
                    "app.decimal_places", decimalPlaces.getText(),
                    "app.default_shift", (String) defaultShift.getSelectedItem(),
                    "app.default_milk_type", (String) defaultMilkType.getSelectedItem(),
                    "app.manual_rate_override", (String) manualRateOverride.getSelectedItem(),
                    "fat.min", fatMin.getText(), "fat.max", fatMax.getText(),
                    "snf.min", snfMin.getText(), "snf.max", snfMax.getText(),
                    "fat.min_cow", fatMinCow.getText(), "fat.max_cow", fatMaxCow.getText(),
                    "fat.min_buffalo", fatMinBuf.getText(), "fat.max_buffalo", fatMaxBuf.getText(),
                    "fat.min_mix", fatMinMix.getText(), "fat.max_mix", fatMaxMix.getText()));
            JOptionPane.showMessageDialog(this, "Settings saved successfully.", "Settings",
                    JOptionPane.INFORMATION_MESSAGE);
            // Push the new dairy name to every open screen immediately.
            dairy.erp.util.AppBus.fireDairyNameChanged(dairyName.getText());
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
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

