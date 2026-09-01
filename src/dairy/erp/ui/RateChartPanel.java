package dairy.erp.ui;

import dairy.erp.dao.RateChartDAO;
import dairy.erp.model.RateChart;
import dairy.erp.util.ButtonIcons;
import dairy.erp.util.CurrencyUtil;
import dairy.erp.util.UIUtil;
import dairy.erp.util.ValidationUtil;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import javax.swing.JSplitPane;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Rate Chart management: create/edit/delete rate rules, set an active rule,
 * view and print the rate chart.
 */
public class RateChartPanel extends JPanel {

    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 16);

    private final RateChartDAO rateChartDAO = new RateChartDAO();
    private final dairy.erp.service.SettingsService settingsService = new dairy.erp.service.SettingsService();
    private final dairy.erp.util.DairyNameLabel dairyNameLabel = new dairy.erp.util.DairyNameLabel();

    private final JRadioButton cowRadio = new JRadioButton("Cow");
    private final JRadioButton buffaloRadio = new JRadioButton("Buffalo");
    private final JRadioButton mixRadio = new JRadioButton("Mix");
    private final ButtonGroup milkTypeGroup = new ButtonGroup();
    private final JTextField fatMinField = new JTextField(8);
    private final JTextField fatMaxField = new JTextField(8);
    private final JTextField snfMinField = new JTextField(8);
    private final JTextField snfMaxField = new JTextField(8);
    private final JTextField rateField = new JTextField(10);
    private final DatePickerFuture fromPicker = new DatePickerFuture();
    private final DatePickerFuture toPicker = new DatePickerFuture();
    private final JRadioButton activeYesRadio = new JRadioButton("Yes");
    private final JRadioButton activeNoRadio = new JRadioButton("No");
    private final ButtonGroup activeGroup = new ButtonGroup();

    // Button references for enable/disable control
    private JButton newBtn;
    private JButton saveBtn;
    private JButton updateBtn;
    private JButton deleteBtn;

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Milk Type", "FAT Min", "FAT Max", "SNF Min", "SNF Max",
                    "Rate/LTR", "Active"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    /** Rate rules parallel to the table rows (the table shows no ID column). */
    private final List<RateChart> chartCache = new ArrayList<>();

    private int editingId = -1;

    private JPanel buildForm() {
        // Fields + one-row action bar; the white card border comes from buildMain().
        JPanel form = new JPanel(new BorderLayout(0, 4));
        form.setOpaque(false);
        form.add(buildFormFields(), BorderLayout.CENTER);
        form.add(buildButtonBar(), BorderLayout.SOUTH);
        return form;
    }

    private JPanel buildFormFields() {
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(9, 12, 9, 12);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addRow(fieldsPanel, g, row++, "Milk Type:", milkTypePanel());
        addRow(fieldsPanel, g, row++, "FAT Min:", fatMinField);
        addRow(fieldsPanel, g, row++, "FAT Max:", fatMaxField);
        addRow(fieldsPanel, g, row++, "SNF Min:", snfMinField);
        addRow(fieldsPanel, g, row++, "SNF Max:", snfMaxField);
        addRow(fieldsPanel, g, row++, "Rate / LTR:", rateField);
        addRow(fieldsPanel, g, row++, "Active:", activePanel());
        addRow(fieldsPanel, g, row++, "Effective From:", fromPicker);
        addRow(fieldsPanel, g, row++, "Effective To:", toPicker);

        return fieldsPanel;
    }

    /**
     * Action buttons in a two-column grid — New/Save, Update/Delete and a
     * full-width Clear — mirroring the Customer Details sidebar so every
     * button stays visible inside the fixed-width panel.
     */
    private JPanel buildButtonBar() {
        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.setOpaque(false);
        GridBagConstraints bg = new GridBagConstraints();
        bg.insets = new Insets(4, 4, 4, 4);
        bg.fill = GridBagConstraints.HORIZONTAL;
        bg.weightx = 1.0;

        newBtn = new JButton("New", ButtonIcons.of("Plus", Color.WHITE));
        UIUtil.styleSmallButton(newBtn, new Color(0x1976D2));
        newBtn.addActionListener(e -> resetForNewEntry());
        bg.gridx = 0; bg.gridy = 0;
        buttons.add(newBtn, bg);
        saveBtn = new JButton("Save", ButtonIcons.of("Save", Color.WHITE));
        UIUtil.styleSmallButton(saveBtn, new Color(0x2E7D32));
        saveBtn.addActionListener(e -> saveRecord());
        bg.gridx = 1; bg.gridy = 0;
        buttons.add(saveBtn, bg);
        updateBtn = new JButton("Update", ButtonIcons.of("Refresh", Color.WHITE));
        UIUtil.styleSmallButton(updateBtn, new Color(0xEF6C00));
        updateBtn.addActionListener(e -> updateRecord());
        bg.gridx = 0; bg.gridy = 1;
        buttons.add(updateBtn, bg);
        deleteBtn = new JButton("Delete", ButtonIcons.of("Trash", Color.WHITE));
        UIUtil.styleSmallButton(deleteBtn, new Color(0xC62828));
        deleteBtn.addActionListener(e -> deleteRecord());
        bg.gridx = 1; bg.gridy = 1;
        buttons.add(deleteBtn, bg);
        JButton clearBtn = new JButton("Clear", ButtonIcons.of("Cross", Color.WHITE));
        UIUtil.styleSmallButton(clearBtn, new Color(0x607D8B));
        clearBtn.addActionListener(e -> resetForNewEntry());
        // Same size as the other buttons: single grid cell, no full-width span.
        bg.gridx = 0; bg.gridy = 2; bg.gridwidth = 1;
        buttons.add(clearBtn, bg);
        return buttons;
    }

    /** Adds one label + full-width field row to a two-column vertical form. */
    private void addRow(JPanel form, GridBagConstraints g, int row,
                        String labelText, java.awt.Component comp) {
        g.gridx = 0; g.gridy = row; g.gridwidth = 1;
        g.weightx = 0; g.fill = GridBagConstraints.NONE;
        form.add(styledLabel(labelText), g);
        g.gridx = 1; g.gridwidth = 1;
        g.weightx = 1.0; g.fill = GridBagConstraints.HORIZONTAL;
        form.add(comp, g);
    }

    

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(LABEL_FONT);
        return l;
    }

    /** Builds a preferred-width row so Cow, Buffalo and Mix text never clips. */
    private JPanel milkTypePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(0, 0, 0, 14);
        gc.gridx = 0;
        panel.add(cowRadio, gc);
        gc.gridx = 1;
        panel.add(buffaloRadio, gc);
        gc.gridx = 2;
        gc.insets = new Insets(0, 0, 0, 0);
        panel.add(mixRadio, gc);
        return panel;
    }

    /** Builds a preferred-width row with the Active Yes/No radio buttons. */
    private JPanel activePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(0, 0, 0, 14);
        gc.gridx = 0;
        panel.add(activeYesRadio, gc);
        gc.gridx = 1;
        gc.insets = new Insets(0, 0, 0, 0);
        panel.add(activeNoRadio, gc);
        return panel;
    }

    /** Disables New/Save while editing and restores them after the operation. */
    private void toggleButtons(boolean editing) {
        newBtn.setEnabled(!editing);
        saveBtn.setEnabled(!editing);
        updateBtn.setEnabled(editing);
        deleteBtn.setEnabled(editing);
    }

    private JPanel buildTable() {
        JPanel area = new JPanel(new BorderLayout());
        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(Color.WHITE);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    loadSelected();
                }
            }
        });
        area.add(scroll, BorderLayout.CENTER);
        return area;
    }

    /** Column widths plus the Yes/No pill renderer. */
    private void configureTableColumns() {
        int[] widths = {110, 80, 80, 80, 80, 90, 90};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(6).setCellRenderer(new YesNoBadgeRenderer());
    }


    public RateChartPanel() {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        UIUtil.styleField(fatMinField, 10);
        UIUtil.styleField(fatMaxField, 10);
        UIUtil.styleField(snfMinField, 10);
        UIUtil.styleField(snfMaxField, 10);
        UIUtil.styleField(rateField, 12);

        UIUtil.styleComponent(fromPicker.getTextField(), 18);
        UIUtil.styleComponent(fromPicker.getButton(), 18);
        UIUtil.styleComponent(toPicker.getTextField(), 18);
        UIUtil.styleComponent(toPicker.getButton(), 18);
        milkTypeGroup.add(cowRadio);
        milkTypeGroup.add(buffaloRadio);
        milkTypeGroup.add(mixRadio);
        activeGroup.add(activeYesRadio);
        activeGroup.add(activeNoRadio);
        loadDairyName();

        // Style the radio buttons
        UIUtil.styleComponent(cowRadio, 18);
        UIUtil.styleComponent(buffaloRadio, 18);
        UIUtil.styleComponent(mixRadio, 18);
        UIUtil.styleComponent(activeYesRadio, 18);
        UIUtil.styleComponent(activeNoRadio, 18);
        // Use exactly the same shared styling as the Customer Details grid.
        UIUtil.styleCustomerDetailsTable(table);
        configureTableColumns();
        add(buildMain(), BorderLayout.CENTER);
        resetForNewEntry();
        loadAll();
    }

    /** Shows the dairy name in the golden logo style — identical on every panel. */
    private void loadDairyName() {
        dairyNameLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        applyDairyName(settingsService.get("dairy.name"));
        // Live update: re-apply whenever Settings saves a new dairy name.
        dairy.erp.util.AppBus.onDairyNameChanged(this::applyDairyName);
    }

    private void applyDairyName(String name) {
        dairyNameLabel.setText(name == null || name.isBlank() ? "SRS Dairy ERP" : name);
    }
    private JPanel buildMain() {
        // Left column: white sidebar card — header row, "Manage Rate Chart"
        // heading, the fields and the one-row action bar (as in the design).
        JPanel left = new JPanel(new BorderLayout(0, 4));
        left.setBackground(Color.WHITE);
        left.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xd0, 0xd7, 0xde), 1, true),
                BorderFactory.createEmptyBorder(2, 8, 8, 8)));

        // Header row: logo on the left, dairy name on the right (as in the design).
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.add(UIUtil.header("Rate Chart"), BorderLayout.CENTER);
        headerRow.add(dairyNameLabel, BorderLayout.EAST);
        left.add(headerRow, BorderLayout.NORTH);

        // Card content: "Manage Rate Chart" heading + fields + action buttons,
        // structured exactly like the Customer Details sidebar so the buttons
        // are always visible without stretching the panel.
        JPanel formCard = new JPanel(new BorderLayout(0, 4));
        formCard.setOpaque(false);
        JPanel manageHeadingPanel = new JPanel(new BorderLayout(0, 2));
        manageHeadingPanel.setOpaque(false);
        JLabel manageHeading = new JLabel("Manage Rate Chart");
        manageHeading.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        manageHeading.setBorder(BorderFactory.createEmptyBorder(4, 2, 2, 2));
        manageHeadingPanel.add(manageHeading, BorderLayout.NORTH);
        manageHeadingPanel.add(new JSeparator(), BorderLayout.SOUTH);
        formCard.add(manageHeadingPanel, BorderLayout.NORTH);
        formCard.add(buildForm(), BorderLayout.CENTER);
        left.add(formCard, BorderLayout.CENTER);
        // Fixed width wide enough for every field row to be fully visible
        // on load; the user can still drag the divider to widen it.
        left.setPreferredSize(new Dimension(460, 0));
        left.setMinimumSize(new Dimension(460, 0));

        // Right column: white card with a "Data Table" heading above the grid.
        JPanel right = new JPanel(new BorderLayout(4, 4));
        right.setBackground(Color.WHITE);
        right.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xd0, 0xd7, 0xde), 1, true),
                BorderFactory.createEmptyBorder(2, 10, 8, 10)));
        JPanel tableHeadingPanel = new JPanel(new BorderLayout(0, 2));
        tableHeadingPanel.setOpaque(false);
        JLabel tableHeading = new JLabel("Rate Chart");
        tableHeading.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        tableHeading.setBorder(BorderFactory.createEmptyBorder(6, 2, 2, 2));
        tableHeadingPanel.add(tableHeading, BorderLayout.NORTH);
        tableHeadingPanel.add(new JSeparator(), BorderLayout.SOUTH);
        right.add(tableHeadingPanel, BorderLayout.NORTH);
        right.add(buildTable(), BorderLayout.CENTER);
        right.setMinimumSize(new Dimension(400, 0));

        // JSplitPane allows stretching left/right by dragging the divider.
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(460);
        // All extra width goes to the table side so the form keeps its size.
        split.setResizeWeight(0.0);
        // JSplitPane clamps the divider while the panel has no size yet, which
        // squeezes the form fields on load — apply the fixed width once the
        // panel gets its real size (one-shot), so every field is visible.
        split.addComponentListener(new java.awt.event.ComponentAdapter() {
            private boolean applied = false;

            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (!applied && split.getWidth() > 0) {
                    applied = true;
                    split.setDividerLocation(460);
                }
            }
        });
        split.setContinuousLayout(true);
        split.setBorder(null);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(split, BorderLayout.CENTER);
        return wrapper;
    }
    private void loadAll() {
        // Refresh the whole grid: clear rows and the parallel rule cache first
        // so save/update/delete never leaves duplicate rows behind.
        tableModel.setRowCount(0);
        chartCache.clear();
        try {
            for (RateChart r : rateChartDAO.findAll()) {
                chartCache.add(r);
                tableModel.addRow(new Object[]{
                        r.getMilkType(),
                        CurrencyUtil.format(r.getFatMin()), CurrencyUtil.format(r.getFatMax()),
                        CurrencyUtil.format(r.getSnfMin()), CurrencyUtil.format(r.getSnfMax()),
                        CurrencyUtil.format(r.getRatePerLitre()), r.isActive() ? "Yes" : "No"
                });
            }
        } catch (SQLException e) {
            UIUtil.showMessage(this, "Could not load rate chart: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void resetForNewEntry() {
        clearFields();
        fromPicker.setDate(LocalDate.now());
        toPicker.setDate(LocalDate.now());
        editingId = -1;
        // Default selection: Cow
        milkTypeGroup.clearSelection();
        cowRadio.setSelected(true);
        activeYesRadio.setSelected(true);
        toggleButtons(false);
    }

    private String validateForm() {
        if (ValidationUtil.parseDecimal(fatMinField.getText()) == null
                || ValidationUtil.parseDecimal(fatMaxField.getText()) == null
                || ValidationUtil.parseDecimal(snfMinField.getText()) == null
                || ValidationUtil.parseDecimal(snfMaxField.getText()) == null) {
            return "FAT and SNF minimum/maximum must be valid numbers.";
        }
        if (!ValidationUtil.isPositive(ValidationUtil.parseDecimal(rateField.getText()))) {
            return "Rate / LTR must be greater than zero.";
        }
        LocalDate from = fromPicker.getDate();
        if (from == null ) {
            return "Effective From date must be selected.";
        }
        return "";
    }

    private RateChart collectForm() {
        RateChart r = new RateChart();
        r.setMilkType(selectedMilkType());
        r.setFatMin(ValidationUtil.parseDecimal(fatMinField.getText()));
        r.setFatMax(ValidationUtil.parseDecimal(fatMaxField.getText()));
        r.setSnfMin(ValidationUtil.parseDecimal(snfMinField.getText()));
        r.setSnfMax(ValidationUtil.parseDecimal(snfMaxField.getText()));
        r.setRatePerLitre(ValidationUtil.parseDecimal(rateField.getText()));
        r.setEffectiveFrom(fromPicker.getDate());
        r.setEffectiveTo(toPicker.getDate());
        r.setActive(activeYesRadio.isSelected());
        return r;
    }

    private void saveRecord() {
        String error = validateForm();
        if (!error.isEmpty()) {
            UIUtil.showMessage(this, error, "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            rateChartDAO.add(collectForm());
            UIUtil.showMessage(this, "Rate rule saved successfully.", "Save",
                    JOptionPane.INFORMATION_MESSAGE);
            loadAll();
            clearFields();
        } catch (SQLException e) {
            UIUtil.showMessage(this, "Could not save: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateRecord() {
        if (editingId <= 0) {
            UIUtil.showMessage(this, "Double-click a rule to edit it first.",
                    "Update", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String error = validateForm();
        if (!error.isEmpty()) {
            UIUtil.showMessage(this, error, "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        RateChart r = collectForm();
        r.setId(editingId);
        try {
            rateChartDAO.update(r);
            UIUtil.showMessage(this, "Rate rule updated successfully.", "Update",
                    JOptionPane.INFORMATION_MESSAGE);
            loadAll();
            clearFields();
        } catch (SQLException e) {
            UIUtil.showMessage(this, "Could not update: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void deleteRecord() {
        if (editingId <= 0) {
            UIUtil.showMessage(this, "Double-click a rule to delete it first.",
                    "Delete", JOptionPane.WARNING_MESSAGE);
            return;
        }
        deleteById(editingId);
    }

    /** Confirms and deletes the rule with the given id, then reloads the grid. */
    private void deleteById(int id) {
        int confirm = UIUtil.confirm(this,
                "Are you sure you want to delete this rate rule?", "Delete Rate Rule");
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            rateChartDAO.delete(id);
            UIUtil.showMessage(this, "Rate rule deleted.", "Delete",
                    JOptionPane.INFORMATION_MESSAGE);
            loadAll();
            clearFields();
        } catch (SQLException e) {
            UIUtil.showMessage(this, "Could not delete: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

        
    private void loadSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        if (modelRow >= chartCache.size()) {
            return;
        }
        RateChart r = chartCache.get(modelRow);
        editingId = r.getId();
        selectMilkType(r.getMilkType());
        fatMinField.setText(r.getFatMin().toPlainString());
        fatMaxField.setText(r.getFatMax().toPlainString());
        snfMinField.setText(r.getSnfMin().toPlainString());
        snfMaxField.setText(r.getSnfMax().toPlainString());
        rateField.setText(r.getRatePerLitre().toPlainString());
        fromPicker.setDate(r.getEffectiveFrom());
        toPicker.setDate(r.getEffectiveTo());
        selectActive(r.isActive() ? "Yes" : "No");
        toggleButtons(true);
    }


    private void clearFields() {
        milkTypeGroup.clearSelection();
        fatMinField.setText("");
        fatMaxField.setText("");
        snfMinField.setText("");
        snfMaxField.setText("");
        rateField.setText("");
        fromPicker.setDate(null);
        toPicker.setDate(null);
        activeGroup.clearSelection();
        editingId = -1;
        toggleButtons(false);
    }

    /** Returns the currently selected Milk Type radio button's label. */
    private String selectedMilkType() {
        if (cowRadio.isSelected()) return "Cow";
        if (buffaloRadio.isSelected()) return "Buffalo";
        if (mixRadio.isSelected()) return "Mix";
        return "Cow"; // default
    }

    /** Selects the Milk Type radio button matching the given type. */
    private void selectMilkType(String type) {
        milkTypeGroup.clearSelection();
        if ("Buffalo".equals(type)) buffaloRadio.setSelected(true);
        else if ("Mix".equals(type)) mixRadio.setSelected(true);
        else cowRadio.setSelected(true);
    }

    /** Selects the Active Yes/No radio button matching the given value. */
    private void selectActive(String value) {
        activeGroup.clearSelection();
        if ("Yes".equals(value)) activeYesRadio.setSelected(true);
        else activeNoRadio.setSelected(true);
    }

    /**
     * Renderer for the Active column: paints the row background with the same
     * striping/selection colours as the shared table style, then draws a
     * coloured pill — green for Yes, grey for No — as in the design.
     */
    private static class YesNoBadgeRenderer extends JPanel
            implements javax.swing.table.TableCellRenderer {

        private String text = "";
        private Color rowBackground = Color.WHITE;

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable t, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            text = value == null ? "" : value.toString().trim();
            rowBackground = isSelected
                    ? new Color(0xd2, 0xec, 0xd9)
                    : (row % 2 == 0 ? Color.WHITE : new Color(0xe8, 0xf1, 0xf7));
            return this;
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(rowBackground);
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (!text.isEmpty()) {
                boolean no = "No".equalsIgnoreCase(text);
                Color pill = no ? new Color(0xe0, 0xe0, 0xe0) : new Color(0xc8, 0xe6, 0xc9);
                Color textColour = no ? new Color(0x61, 0x61, 0x61) : new Color(0x1b, 0x5e, 0x20);
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(text);
                int pillWidth = Math.max(24, Math.min(getWidth() - 12, textWidth + 20));
                int pillHeight = Math.min(getHeight() - 12, 26);
                int px = 6;
                int py = (getHeight() - pillHeight) / 2;
                g2.setColor(pill);
                g2.fillRoundRect(px, py, pillWidth, pillHeight, pillHeight, pillHeight);
                g2.setColor(textColour);
                int tx = px + (pillWidth - textWidth) / 2;
                int ty = py + (pillHeight - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, tx, ty);
            }
            g2.dispose();
        }
    }

    private java.awt.Frame findOwner() {
        java.awt.Container c = this;
        while (c != null && !(c instanceof JFrame)) {
            c = c.getParent();
        }
        return (java.awt.Frame) c;
    }
}

