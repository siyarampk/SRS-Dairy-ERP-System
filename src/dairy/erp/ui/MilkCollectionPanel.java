package dairy.erp.ui;

import dairy.erp.model.Customer;
import dairy.erp.model.MilkCollection;
import dairy.erp.service.MilkCollectionService;
import dairy.erp.service.SettingsService;
import dairy.erp.util.CurrencyUtil;
import dairy.erp.util.DateUtil;
import dairy.erp.util.PrintUtil;
import dairy.erp.util.UIUtil;
import dairy.erp.util.ValidationUtil;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.Box;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Milk Collection entry — the primary daily workflow. Optimised for keyboard
 * operation: enter customer code then tab through quantity/FAT/SNF; the rate
 * and amount are calculated automatically and after saving focus returns to
 * the customer code field for fast multi-customer entry.
 */
public class MilkCollectionPanel extends JPanel {

    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 16);
    private static final Font FIELD_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 16);

    private final MilkCollectionService service = new MilkCollectionService();
    private final SettingsService settingsService = new SettingsService();
    private final JLabel dairyNameLabel = new JLabel("", javax.swing.SwingConstants.CENTER);

    private final JTextField dateField = new JTextField(DateUtil.toDisplay(LocalDate.now()), 12);
    private final JTextField customerCodeField = new JTextField(12);
    private final JTextField customerNameField = new JTextField(18);
    private final JRadioButton cowRadio = new JRadioButton("Cow");
    private final JRadioButton buffaloRadio = new JRadioButton("Buffalo");
    private final JRadioButton mixRadio = new JRadioButton("Mix");
    private final ButtonGroup milkGroup = new ButtonGroup();
    private final JRadioButton morningRadio = new JRadioButton("Morning");
    private final JRadioButton eveningRadio = new JRadioButton("Evening");
    private final ButtonGroup shiftGroup = new ButtonGroup();
    private final JTextField quantityField = new JTextField(10);
    private final JTextField fatField = new JTextField(8);
    private final JTextField snfField = new JTextField(8);
    private final JTextField rateField = new JTextField(10);
    private final JTextField amountField = new JTextField(12);
    private final JTextField remarksField = new JTextField(16);
    private final JLabel customerBanner = UIUtil.greenBanner("");

    // filter controls
    private final DatePicker filterFromPicker = new DatePicker();
    private final DatePicker filterToPicker = new DatePicker();
    private final JTextField filterCustomerField = new JTextField(10);
    private final JComboBox<String> filterMilkBox = new JComboBox<>(new String[]{"All", "Cow", "Buffalo", "Mix"});
    private final JComboBox<String> filterShiftBox = new JComboBox<>(new String[]{"All", "Morning", "Evening"});

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Date", "Shift", "Code", "Name", "Milk", "FAT", "SNF", "Qty", "Rate", "Amount"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    private Customer selectedCustomer;
    private int editingId = -1;

    // Panel mode: "new" (entry form) or "history" (records).
    private boolean historyMode = false;
    private final JPanel headerPanel = UIUtil.header("New Collection");
    private final JButton newButton = new JButton("New");
    private final JButton saveButton = new JButton("Save");
    private final JButton updateButton = new JButton("Update");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton printButton = new JButton("Print");
    private final JButton clearButton = new JButton("Clear");
    private final JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 8, 6));

    public MilkCollectionPanel() {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        initDefaults();
        loadDairyName();
        add(buildMain(), BorderLayout.CENTER);
        loadTable(null);
    }
    private JPanel buildMain() {
        // Left panel: form entry area with header, customer banner and form fields
        JPanel left = new JPanel(new BorderLayout(10, 6));
        left.add(headerPanel, BorderLayout.NORTH);

        JPanel formHome = new JPanel(new BorderLayout(8, 4));
        formHome.add(customerBanner, BorderLayout.NORTH);
        formHome.add(buildForm(), BorderLayout.CENTER);
        left.add(formHome, BorderLayout.CENTER);
        left.setMinimumSize(new Dimension(350, 0));
        left.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));

        // Right panel: table area with filter and records
        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.setBorder(BorderFactory.createTitledBorder("Collection Records"));
        right.add(buildTableArea(), BorderLayout.CENTER);
        right.setMinimumSize(new Dimension(400, 0));

        // JSplitPane allows stretching left/right by dragging the divider
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(500);
        split.setResizeWeight(0.35);
        split.setContinuousLayout(true);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(split, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel shiftPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        p.add(morningRadio);
        p.add(eveningRadio);
        return p;
    }

    private JPanel milkPanel() {
        milkGroup.add(cowRadio);
        milkGroup.add(buffaloRadio);
        milkGroup.add(mixRadio);
        cowRadio.setSelected(true);
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        p.add(cowRadio);
        p.add(Box.createHorizontalStrut(6));
        p.add(buffaloRadio);
        p.add(Box.createHorizontalStrut(6));
        p.add(mixRadio);
        return p;
    }
    private String getSelectedMilkType() {
        if (buffaloRadio.isSelected()) {
            return "Buffalo";
        }
        if (mixRadio.isSelected()) {
            return "Mix";
        }
        return "Cow";
    }

    private void setSelectedMilkType(String milkType) {
        if (milkType == null) {
            cowRadio.setSelected(true);
        } else if ("Buffalo".equalsIgnoreCase(milkType)) {
            buffaloRadio.setSelected(true);
        } else if ("Mix".equalsIgnoreCase(milkType)) {
            mixRadio.setSelected(true);
        } else {
            cowRadio.setSelected(true);
        }
    }

    private String getSelectedShift() {
        return morningRadio.isSelected() ? "Morning" : "Evening";
    }

    private void selectShift(String shift) {
        if ("Evening".equalsIgnoreCase(shift)) {
            eveningRadio.setSelected(true);
        } else {
            morningRadio.setSelected(true);
        }
    }

    private void loadDairyName() {
        dairyNameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        dairyNameLabel.setForeground(Color.RED);
        dairyNameLabel.setBorder(BorderFactory.createEmptyBorder(40, 20, 20, 20));
        applyDairyName(settingsService.get("dairy.name"));
        dairy.erp.util.AppBus.onDairyNameChanged(this::applyDairyName);
    }

    private void applyDairyName(String name) {
        dairyNameLabel.setText(name == null || name.isBlank() ? "SRS Dairy ERP" : name);
    }

    private void initDefaults() {
        dateField.setText(DateUtil.toDisplay(LocalDate.now()));
        UIUtil.makeUpperCase(customerCodeField);
        UIUtil.styleField(customerCodeField, 14);
        UIUtil.styleField(customerNameField, 18);
        UIUtil.styleField(quantityField, 12);
        UIUtil.styleField(fatField, 10);
        UIUtil.styleField(snfField, 10);
        UIUtil.styleField(rateField, 12);
        UIUtil.styleField(amountField, 14);
        UIUtil.styleField(remarksField, 20);
        shiftGroup.add(morningRadio);
        shiftGroup.add(eveningRadio);
        morningRadio.setSelected(true);
        morningRadio.setFont(FIELD_FONT);
        eveningRadio.setFont(FIELD_FONT);
        cowRadio.setFont(FIELD_FONT);
        buffaloRadio.setFont(FIELD_FONT);
        mixRadio.setFont(FIELD_FONT);
        UIUtil.styleComponent(dateField, 18);
        dateField.setEditable(false);
        dateField.setFocusable(false);
        dateField.setBackground(new Color(0xF5, 0xDE, 0xB3)); // dark cream
        String defaultShift = settingsService.get("app.default_shift");
        if ("Evening".equals(defaultShift)) {
            eveningRadio.setSelected(true);
        } else {
            morningRadio.setSelected(true);
        }
        String defaultMilk = settingsService.get("app.default_milk_type");
        setSelectedMilkType(defaultMilk);
        rateField.setEditable(false);
        rateField.setFocusable(false);
        rateField.setBackground(new Color(0xF5, 0xDE, 0xB3)); // dark cream
        amountField.setEditable(false);
        amountField.setFocusable(false);
        amountField.setBackground(new Color(0xF5, 0xDE, 0xB3)); // dark cream
        customerNameField.setEditable(false);
        customerNameField.setFocusable(false);
        customerNameField.setBackground(new Color(0xF5, 0xDE, 0xB3)); // dark cream
    }

        
    private JPanel buildForm() {
        JPanel form = new JPanel(new BorderLayout(12, 8));
        form.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new java.awt.Color(0x1a, 0x5f, 0x7a)),
                "Milk Collection Entry"));
        form.add(dairyNameLabel, BorderLayout.NORTH);

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setMinimumSize(new Dimension(350, 600));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(25, 10, 10, 10); 
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        // Vertical stack: one field per row, each field spanning the full
        // remaining width of the panel so every value is clearly visible.
        int row = 0;
        addRow(fieldsPanel, g, row++, "Date:", dateField);
        addRow(fieldsPanel, g, row++, "Shift:", shiftPanel());
        addRow(fieldsPanel, g, row++, "Customer Code:", customerCodeField);
        addRow(fieldsPanel, g, row++, "Customer Name:", customerNameField);
        addRow(fieldsPanel, g, row++, "Milk Type:", milkPanel());
        addRow(fieldsPanel, g, row++, "Quantity (LTR):", quantityField);
        addRow(fieldsPanel, g, row++, "FAT (%):", fatField);
        addRow(fieldsPanel, g, row++, "SNF (%):", snfField);
        addRow(fieldsPanel, g, row++, "Rate / LTR:", rateField);
        addRow(fieldsPanel, g, row++, "Amount:", amountField);
        addRow(fieldsPanel, g, row++, "Remarks:", remarksField);

        // Buttons: mode-aware grid with the same look as the Customer Details
        // page — New/Save in entry mode; Update/Delete/Print in history mode.
        styleActionButtons();
        rebuildButtons();
        form.add(fieldsPanel, BorderLayout.CENTER);
        form.add(buttonPanel, BorderLayout.SOUTH);

        // Enter in customer code loads the customer.
        customerCodeField.addActionListener(e -> loadCustomer());
        // Recalculate on quantity/FAT/SNF entry (Enter or focus loss).
        java.awt.event.FocusAdapter recalcAdapter = new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                recalc();
            }
        };
        quantityField.addActionListener(e -> recalc());
        fatField.addActionListener(e -> recalc());
        snfField.addActionListener(e -> recalc());
        quantityField.addFocusListener(recalcAdapter);
        fatField.addFocusListener(recalcAdapter);
        snfField.addFocusListener(recalcAdapter);

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

    private void addButton(JPanel panel, String text, java.awt.event.ActionListener listener) {
        JButton b = new JButton(text);
        UIUtil.styleButton(b);
        b.addActionListener(listener);
        panel.add(b);
    }

    private void newBtn(JPanel panel, String text, Color bg, java.awt.event.ActionListener listener) {
        JButton b = new JButton(text);
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(100, 36));
        b.addActionListener(listener);
        panel.add(b);
    }

    /** Styles every action button with the same look as the Customer Details page. */
    private void styleActionButtons() {
        UIUtil.styleSmallButton(newButton, new Color(0x1976D2));   // blue
        UIUtil.styleSmallButton(saveButton, new Color(0x2E7D32));  // green
        UIUtil.styleSmallButton(updateButton, new Color(0xEF6C00)); // orange
        UIUtil.styleSmallButton(deleteButton, new Color(0xC62828)); // red
        UIUtil.styleSmallButton(printButton, new Color(0x607D8B));  // grey
        UIUtil.styleSmallButton(clearButton, new Color(0xd3, 0x2f, 0x2f)); // red
        newButton.addActionListener(e -> resetForm());
        saveButton.addActionListener(e -> saveRecord());
        updateButton.addActionListener(e -> updateSelected());
        deleteButton.addActionListener(e -> deleteRecord());
        printButton.addActionListener(e -> printList());
        clearButton.addActionListener(e -> resetForm());
    }

    /** Rebuilds the visible button set for the current mode. */
    private void rebuildButtons() {
        buttonPanel.removeAll();
        if (historyMode) {
            buttonPanel.add(updateButton);
            buttonPanel.add(deleteButton);
            buttonPanel.add(printButton);
        } else {
            buttonPanel.add(newButton);
            buttonPanel.add(saveButton);
            buttonPanel.add(printButton);
            buttonPanel.add(clearButton);
        }
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    /**
     * Switches the panel between "new" (entry form) and "history" (records).
     * The panel header text and the visible action buttons follow the mode.
     */
    public void setMode(String mode) {
        historyMode = "history".equals(mode);
        javax.swing.JLabel title = UIUtil.headerTitle(headerPanel);
        if (title != null) {
            title.setText(historyMode ? " Collection History" : " New Collection");
        }
        rebuildButtons();
    }

    /**
     * History-mode Update: loads the selected record into the form the first
     * time, then commits the edited values through the normal update path.
     */
    private void updateSelected() {
        if (editingId == -1) {
            if (table.getSelectedRow() < 0) {
                JOptionPane.showMessageDialog(this,
                        "Select a record from the history table first.",
                        "Update", JOptionPane.WARNING_MESSAGE);
                return;
            }
            loadSelected();
            return; // record loaded — edit the fields, then press Update again to save
        }
        saveRecord();
    }

    /** Creates a GridBagConstraints for a single cell. */
    private GridBagConstraints gbc(GridBagConstraints g, int x, int y, int width, int fill, double weightx) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = width;
        c.insets = new Insets(6, 10, 6, 10);
        c.fill = fill;
        c.weightx = weightx;
        c.anchor = GridBagConstraints.WEST;
        return c;
    }

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(LABEL_FONT);
        return l;
    }

    private JPanel buildTableArea() {
        UIUtil.makeUpperCase(filterCustomerField);
        // Filter with each label immediately followed by its field, on one line.
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        filter.setBorder(BorderFactory.createTitledBorder("Filter / Search"));
        filter.add(styledLabel("From:"));
        filter.add(filterFromPicker);
        filter.add(styledLabel("To:"));
        filter.add(filterToPicker);
        filter.add(styledLabel("Customer:"));
        filter.add(filterCustomerField);
        filter.add(styledLabel("Shift:"));
        filter.add(filterShiftBox);
        JButton go = new JButton("Search");
        UIUtil.styleSmallButton(go, new Color(0x1e, 0x8e, 0x3e));
        go.addActionListener(e -> applyFilter());
        filter.add(go);
        JButton reset = new JButton("Reset");
        UIUtil.styleSmallButton(reset, new Color(0xd3, 0x2f, 0x2f));
        reset.addActionListener(e -> {
            filterFromPicker.setDate(null);
            filterToPicker.setDate(null);
            filterCustomerField.setText("");
            
            filterShiftBox.setSelectedIndex(0);
            loadTable(null);
        });
        filter.add(reset);

        // Space between the filter row and the table for a cleaner look.
        JPanel area = new JPanel(new BorderLayout(0, 12));
        area.add(filter, BorderLayout.NORTH);
        UIUtil.styleCustomerDetailsTable(table);
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

    private void loadTable(List<MilkCollection> records) {
        tableModel.setRowCount(0);
        if (records == null) {
            records = service.all();
        }
        for (MilkCollection m : records) {
            tableModel.addRow(new Object[]{
                    DateUtil.toDisplay(m.getCollectionDate()), m.getShift(), m.getCustomerCode(),
                    m.getCustomerName(), m.getMilkType(),
                    CurrencyUtil.format(m.getFat()), CurrencyUtil.format(m.getSnf()),
                    CurrencyUtil.format(m.getQuantity()), CurrencyUtil.format(m.getRatePerLitre()),
                    CurrencyUtil.formatMoney(m.getAmount())
            });
        }
    }

    private void applyFilter() {
        LocalDate from = filterFromPicker.getDate();
        LocalDate to = filterToPicker.getDate();
        Integer customerId = null;
        if (!ValidationUtil.isBlank(filterCustomerField.getText())) {
            Customer c = service.findCustomerByCode(filterCustomerField.getText().trim());
            if (c != null) {
                customerId = c.getId();
            }
        }
        String milkType = "All".equals(filterMilkBox.getSelectedItem())
                ? null : (String) filterMilkBox.getSelectedItem();
        String shift = "All".equals(filterShiftBox.getSelectedItem())
                ? null : (String) filterShiftBox.getSelectedItem();
        loadTable(service.search(from, to, customerId, milkType, shift));
    }


    // ---- customer loading and calculation ----

    private void loadCustomer() {
        String code = customerCodeField.getText().trim();
        if (ValidationUtil.isBlank(code)) {
            return;
        }
        Customer c = service.findCustomerByCode(code);
        if (c == null) {
            JOptionPane.showMessageDialog(this,
                    "Customer not found with code: " + code,
                    "Customer", JOptionPane.WARNING_MESSAGE);
            selectedCustomer = null;
            customerNameField.setText("");
            updateBanner();
            quantityField.requestFocusInWindow();
            return;
        }
        selectedCustomer = c;
        customerNameField.setText(c.getCustomerName());
        updateBanner();
        if ("Inactive".equals(c.getStatus())) {
            JOptionPane.showMessageDialog(this,
                    "This customer is inactive.", "Customer", JOptionPane.WARNING_MESSAGE);
        }
        if (c.getMilkType() != null) {
            setSelectedMilkType(c.getMilkType());
        }
        quantityField.requestFocusInWindow();
    }

    /** Shows the customer name (and mobile) in a centred green banner at the top. */
    private void updateBanner() {
        updateBanner(null);
    }

    private void updateBanner(String message) {
        if (message != null && !message.isEmpty()) {
            customerBanner.setText(message);
        } else if (selectedCustomer != null) {
            String mobile = selectedCustomer.getMobile();
            customerBanner.setText(selectedCustomer.getCustomerName()
                    + (mobile == null || mobile.isBlank() ? "" : "  |  " + mobile));
        } else {
            customerBanner.setText("");
        }
    }

    private void recalc() {
        if (selectedCustomer == null) {
            return;
        }
        BigDecimal qty = ValidationUtil.parseDecimal(quantityField.getText());
        BigDecimal fat = ValidationUtil.parseDecimal(fatField.getText());
        BigDecimal snf = ValidationUtil.parseDecimal(snfField.getText());
        if (qty == null || fat == null || snf == null || qty.signum() <= 0) {
            rateField.setText("");
            amountField.setText("");
            return;
        }
        boolean manualOverride = settingsService.getBoolean("app.manual_rate_override", false);
        BigDecimal manualRate = ValidationUtil.parseDecimal(rateField.getText());
        LocalDate date = DateUtil.parse(dateField.getText());
        if (date == null) {
            date = LocalDate.now();
        }
        MilkCollectionService.CalculationResult result = service.calculate(
                selectedCustomer, date, getSelectedShift(),
                getSelectedMilkType(), qty, fat, snf,
                manualOverride, manualRate);
        if (result == null) {
            rateField.setText("");
            amountField.setText("");
            JOptionPane.showMessageDialog(this,
                    "No rate rule found for this milk type/FAT/SNF.", "Rate",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        rateField.setText(CurrencyUtil.format(result.rate));
        amountField.setText(CurrencyUtil.formatMoney(result.collection.getAmount()));
    }


    // ---- record operations ----

    private void saveRecord() {
        String error = validateForm();
        if (!error.isEmpty()) {
            JOptionPane.showMessageDialog(this, error, "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        LocalDate date = DateUtil.parse(dateField.getText());
        String shift = getSelectedShift();
        BigDecimal qty = ValidationUtil.parseDecimal(quantityField.getText());
        BigDecimal fat = ValidationUtil.parseDecimal(fatField.getText());
        BigDecimal snf = ValidationUtil.parseDecimal(snfField.getText());

        // Recompute authoritative values (regardless of manual edits).
        boolean manualOverride = settingsService.getBoolean("app.manual_rate_override", false);
        BigDecimal manualRate = ValidationUtil.parseDecimal(rateField.getText());
        MilkCollectionService.CalculationResult result = service.calculate(
                selectedCustomer, date, shift, getSelectedMilkType(),
                qty, fat, snf, manualOverride, manualRate);
        if (result == null) {
            JOptionPane.showMessageDialog(this,
                    "No rate rule found for this milk type/FAT/SNF.", "Rate",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        MilkCollection m = result.collection;
        m.setRemarks(remarksField.getText());

        try {
            if (editingId > 0) {
                m.setId(editingId);
                if (service.existsDuplicate(date, selectedCustomer.getId(), shift, editingId)) {
                    JOptionPane.showMessageDialog(this,
                            "A milk collection entry already exists for this customer, date and shift.",
                            "Duplicate Entry", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                service.update(m);
            } else {
                if (service.existsDuplicate(date, selectedCustomer.getId(), shift, -1)) {
                    JOptionPane.showMessageDialog(this,
                            "A milk collection entry already exists for this customer, date and shift.",
                            "Duplicate Entry", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                service.add(m);
            }
            JOptionPane.showMessageDialog(this, "Milk collection saved successfully.",
                    "Save", JOptionPane.INFORMATION_MESSAGE);
            loadTable(service.all());
            resetForNextEntry();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteRecord() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a record in the table to delete.",
                    "Delete", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this milk collection record?",
                "Delete Entry", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        int id;
        MilkCollection mc = recordAtRow(row);
        if (mc == null) {
            JOptionPane.showMessageDialog(this, "Could not locate the underlying record.",
                    "Delete", JOptionPane.WARNING_MESSAGE);
            return;
        }
        id = mc.getId();
        try {
            service.delete(id);
            loadTable(service.all());
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void loadSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a record in the table to edit.",
                    "Edit", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String code = (String) tableModel.getValueAt(row, 2);
        Customer c = service.findCustomerByCode(code);
        if (c == null) {
            return;
        }
        selectedCustomer = c;
        MilkCollection mc = recordAtRow(row);
        if (mc == null) {
            JOptionPane.showMessageDialog(this, "Could not locate the underlying record.",
                    "Edit", JOptionPane.WARNING_MESSAGE);
            return;
        }
        editingId = mc.getId();
        customerCodeField.setText(c.getCustomerCode());
        customerNameField.setText(c.getCustomerName());
        dateField.setText(DateUtil.toDisplay(DateUtil.parse((String) tableModel.getValueAt(row, 0))));
        selectShift((String) tableModel.getValueAt(row, 1));
        setSelectedMilkType((String) tableModel.getValueAt(row, 4));
        fatField.setText(((String) tableModel.getValueAt(row, 5)).replace(",", ""));
        snfField.setText(((String) tableModel.getValueAt(row, 6)).replace(",", ""));
        quantityField.setText(((String) tableModel.getValueAt(row, 7)).replace(",", ""));
        rateField.setText(((String) tableModel.getValueAt(row, 8)).replace(",", ""));
        amountField.setText((String) tableModel.getValueAt(row, 9));
        remarksField.setText(mc.getRemarks());
        updateBanner();
    }

    private MilkCollection recordAtRow(int row) {
        // The id is not shown in the table; resolve via customer+date+shift.
        String code = (String) tableModel.getValueAt(row, 2);
        String dateStr = (String) tableModel.getValueAt(row, 0);
        String shift = (String) tableModel.getValueAt(row, 1);
        LocalDate date = DateUtil.parse(dateStr);
        Customer c = service.findCustomerByCode(code);
        if (c == null) {
            return null;
        }
        // The UNIQUE constraint on (collection_date, customer_id, shift) ensures
        // at most one record, so we can safely return the first match.
        List<MilkCollection> results = service.search(date, date, c.getId(), null, shift);
        return results.isEmpty() ? null : results.get(0);
    }

    private String validateForm() {
        if (DateUtil.parse(dateField.getText()) == null) {
            return "A valid date (dd-MM-yyyy) is required.";
        }
        if (selectedCustomer == null) {
            return "Please enter a valid customer code.";
        }
        BigDecimal qty = ValidationUtil.parseDecimal(quantityField.getText());
        if (qty == null || qty.signum() <= 0) {
            return "Quantity must be greater than zero.";
        }
        BigDecimal fat = ValidationUtil.parseDecimal(fatField.getText());
        BigDecimal snf = ValidationUtil.parseDecimal(snfField.getText());
        if (fat == null || fat.signum() <= 0) {
            return "FAT must be a number greater than zero.";
        }
        if (snf == null || snf.signum() <= 0) {
            return "SNF must be a number greater than zero.";
        }
        BigDecimal minFat = settingsService.minFat(getSelectedMilkType());
        BigDecimal maxFat = settingsService.maxFat(getSelectedMilkType());
        if (minFat != null && maxFat != null && (fat.compareTo(minFat) < 0 || fat.compareTo(maxFat) > 0)) {
            return "FAT must be between " + CurrencyUtil.format(minFat) + " and "
                    + CurrencyUtil.format(maxFat) + " for this milk type.";
        }
        return "";
    }

    private void resetForNextEntry() {
        selectedCustomer = null;
        editingId = -1;
        customerCodeField.setText("");
        customerNameField.setText("");
        quantityField.setText("");
        fatField.setText("");
        snfField.setText("");
        rateField.setText("");
        amountField.setText("");
        remarksField.setText("");
        updateBanner();
        customerCodeField.requestFocusInWindow();
    }

    public void resetForm() {
        selectedCustomer = null;
        editingId = -1;
        customerCodeField.setText("");
        customerNameField.setText("");
        quantityField.setText("");
        fatField.setText("");
        snfField.setText("");
        rateField.setText("");
        amountField.setText("");
        remarksField.setText("");
        dateField.setText(DateUtil.toDisplay(LocalDate.now()));
        updateBanner();
        customerCodeField.requestFocusInWindow();
    }

    private void printList() {
        List<String> lines = new ArrayList<>();
        lines.add("Milk Collection Report");
        lines.add("=".repeat(88));
        lines.add(String.format("%-10s %-8s %-8s %-16s %-5s %-5s %-5s %-8s %-8s %s",
                "Date", "Shift", "Code", "Name", "Milk", "FAT", "SNF", "Qty", "Rate", "Amount"));
        lines.add("-".repeat(88));
        List<MilkCollection> records = service.all();
        for (MilkCollection m : records) {
            lines.add(String.format("%-10s %-8s %-8s %-16s %-5s %-5s %-5s %-8s %-8s %s",
                    DateUtil.toDisplay(m.getCollectionDate()),
                    nullToEmpty(m.getShift()), nullToEmpty(m.getCustomerCode()),
                    truncate(nullToEmpty(m.getCustomerName()), 16),
                    nullToEmpty(m.getMilkType()),
                    CurrencyUtil.format(m.getFat()), CurrencyUtil.format(m.getSnf()),
                    CurrencyUtil.format(m.getQuantity()), CurrencyUtil.format(m.getRatePerLitre()),
                    CurrencyUtil.formatMoney(m.getAmount())));
        }
        PrintUtil.printText(findOwner(), "Milk Collection Report", lines, true);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private java.awt.Frame findOwner() {
        java.awt.Container c = this;
        while (c != null && !(c instanceof JFrame)) {
            c = c.getParent();
        }
        return (java.awt.Frame) c;
    }
}

