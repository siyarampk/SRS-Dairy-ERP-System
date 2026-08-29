package dairy.erp.ui;

import dairy.erp.dao.RateChartDAO;
import dairy.erp.model.RateChart;
import dairy.erp.util.CurrencyUtil;
import dairy.erp.util.DateUtil;
import dairy.erp.util.PrintUtil;
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
import java.awt.GridLayout;
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

    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 16);

    private final RateChartDAO rateChartDAO = new RateChartDAO();
    private final dairy.erp.service.SettingsService settingsService = new dairy.erp.service.SettingsService();
    private final JLabel dairyNameLabel = new JLabel("", javax.swing.SwingConstants.CENTER);

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
            new String[]{"ID", "Milk Type", "FAT Min", "FAT Max", "SNF Min", "SNF Max", "Rate/LTR", "Active"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    private int editingId = -1;

    private JPanel buildForm() {
        JPanel outer = new JPanel(new BorderLayout(12, 8));
        outer.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new java.awt.Color(0x1a, 0x5f, 0x7a)),
                "Rate Chart"));
        outer.add(dairyNameLabel, BorderLayout.NORTH);
        outer.add(buildFormFields(), BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildFormFields() {
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setMinimumSize(new Dimension(350, 600));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(25, 10, 10, 10); 
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

        g.gridx = 0; g.gridy = row; g.gridwidth = 2;
        g.anchor = GridBagConstraints.CENTER;
        g.fill = GridBagConstraints.NONE;
        JPanel form = new JPanel(new BorderLayout());
        form.add(fieldsPanel, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        JButton newBtn = new JButton("New");
        UIUtil.styleSmallButton(newBtn, new Color(0x1976D2));
        newBtn.addActionListener(e -> resetForNewEntry());
        this.newBtn = newBtn;
        buttons.add(newBtn);
        saveBtn = new JButton("Save");
        UIUtil.styleSmallButton(saveBtn, new Color(0x2E7D32));
        saveBtn.addActionListener(e -> saveRecord());
        this.saveBtn = saveBtn;
        buttons.add(saveBtn);
        updateBtn = new JButton("Update");
        UIUtil.styleSmallButton(updateBtn, new Color(0xEF6C00));
        updateBtn.addActionListener(e -> updateRecord());
        this.updateBtn = updateBtn;
        buttons.add(updateBtn);
        deleteBtn = new JButton("Delete");
        UIUtil.styleSmallButton(deleteBtn, new Color(0xC62828));
        deleteBtn.addActionListener(e -> deleteRecord());
        this.deleteBtn = deleteBtn;
        buttons.add(deleteBtn);
        JButton clearBtn = new JButton("Clear");
        UIUtil.styleSmallButton(clearBtn, new Color(0x607D8B));
        clearBtn.addActionListener(e -> clearFields());
        buttons.add(clearBtn);
        JButton printBtn = new JButton("Print");
        UIUtil.styleSmallButton(printBtn, new Color(0x607D8B));
        printBtn.addActionListener(e -> printList());
        buttons.add(printBtn);
        form.add(fieldsPanel, BorderLayout.CENTER);
        form.add(buttons, BorderLayout.SOUTH);
        return form;
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
        add(buildMain(), BorderLayout.CENTER);
        resetForNewEntry();
        loadAll();
    }

    /** Shows the dairy name (from Settings) in red, 28px bold — identical to Customer Details. */
    private void loadDairyName() {
        dairyNameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        dairyNameLabel.setForeground(Color.RED);
        dairyNameLabel.setBorder(BorderFactory.createEmptyBorder(40, 20, 20, 20));
        applyDairyName(settingsService.get("dairy.name"));
        // Live update: re-apply whenever Settings saves a new dairy name.
        dairy.erp.util.AppBus.onDairyNameChanged(this::applyDairyName);
    }

    private void applyDairyName(String name) {
        dairyNameLabel.setText(name == null || name.isBlank() ? "SRS Dairy ERP" : name);
    }
    private JPanel buildMain() {
        // Left column: logo header + rate rule fields. Right column: grid.
        JPanel left = new JPanel(new BorderLayout(8, 6));
        left.add(UIUtil.header("Rate Chart"), BorderLayout.NORTH);
        left.add(buildForm(), BorderLayout.CENTER);
        left.setMinimumSize(new Dimension(350, 0));
        left.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));

        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.setBorder(BorderFactory.createTitledBorder("Rate Chart"));
        right.add(buildTable(), BorderLayout.CENTER);
        right.setMinimumSize(new Dimension(400, 0));

        // JSplitPane allows stretching left/right by dragging the divider
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(450);
        split.setResizeWeight(0.35);
        split.setContinuousLayout(true);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(split, BorderLayout.CENTER);
        return wrapper;
    }
    private void loadAll() {


        try {
            for (RateChart r : rateChartDAO.findAll()) {
                tableModel.addRow(new Object[]{
                        r.getId(), r.getMilkType(),
                        CurrencyUtil.format(r.getFatMin()), CurrencyUtil.format(r.getFatMax()),
                        CurrencyUtil.format(r.getSnfMin()), CurrencyUtil.format(r.getSnfMax()),
                        CurrencyUtil.format(r.getRatePerLitre()), r.isActive() ? "Yes" : "No"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Could not load rate chart: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void resetForNewEntry() {
        clearFields();
        fromPicker.setDate(LocalDate.now());
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
            JOptionPane.showMessageDialog(this, error, "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            rateChartDAO.add(collectForm());
            JOptionPane.showMessageDialog(this, "Rate rule saved successfully.", "Save",
                    JOptionPane.INFORMATION_MESSAGE);
            loadAll();
            clearFields();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Could not save: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateRecord() {
        if (editingId <= 0) {
            JOptionPane.showMessageDialog(this, "Double-click a rule to edit it first.",
                    "Update", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String error = validateForm();
        if (!error.isEmpty()) {
            JOptionPane.showMessageDialog(this, error, "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        RateChart r = collectForm();
        r.setId(editingId);
        try {
            rateChartDAO.update(r);
            JOptionPane.showMessageDialog(this, "Rate rule updated successfully.", "Update",
                    JOptionPane.INFORMATION_MESSAGE);
            loadAll();
            clearFields();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Could not update: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void deleteRecord() {
        if (editingId <= 0) {
            JOptionPane.showMessageDialog(this, "Double-click a rule to delete it first.",
                    "Delete", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this rate rule?", "Delete Rate Rule",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            rateChartDAO.delete(editingId);
            JOptionPane.showMessageDialog(this, "Rate rule deleted.", "Delete",
                    JOptionPane.INFORMATION_MESSAGE);
            loadAll();
            clearFields();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Could not delete: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

        
    private void loadSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        int id = ((Number) tableModel.getValueAt(row, 0)).intValue();
        try {
            for (RateChart r : rateChartDAO.findAll()) {
                if (r.getId() == id) {
                    editingId = id;
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
                    break;
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Could not load rule: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
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

    private void printList() {
        List<String> lines = new ArrayList<>();
        lines.add("Rate Chart");
        lines.add("=".repeat(70));
        lines.add(String.format("%-9s %-8s %-8s %-8s %-8s %-8s %s",
                "Milk", "FAT Min", "FAT Max", "SNF Min", "SNF Max", "Rate/LTR", "Active"));
        lines.add("-".repeat(70));
        try {
            for (RateChart r : rateChartDAO.findAll()) {
                lines.add(String.format("%-9s %-8s %-8s %-8s %-8s %-8s %s",
                        nullToEmpty(r.getMilkType()),
                        CurrencyUtil.format(r.getFatMin()), CurrencyUtil.format(r.getFatMax()),
                        CurrencyUtil.format(r.getSnfMin()), CurrencyUtil.format(r.getSnfMax()),
                        CurrencyUtil.format(r.getRatePerLitre()), r.isActive() ? "Yes" : "No"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Could not load rate chart: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        PrintUtil.printText(findOwner(), "Rate Chart", lines, true);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private java.awt.Frame findOwner() {
        java.awt.Container c = this;
        while (c != null && !(c instanceof JFrame)) {
            c = c.getParent();
        }
        return (java.awt.Frame) c;
    }
}

