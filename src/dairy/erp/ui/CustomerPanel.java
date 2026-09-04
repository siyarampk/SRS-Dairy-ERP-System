package dairy.erp.ui;

import dairy.erp.model.Customer;
import dairy.erp.service.CustomerService;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer / Member master screen: create, edit, search, delete (with safe
 * archival), print, along with automatic customer code generation.
 * <p>
 * UI notes: text fields are auto-uppercased, milk type uses radio buttons,
 * registration date uses a calendar picker, and the table is read-only —
 * double-click a row to load its data into the form for updating.
 */
public class CustomerPanel extends JPanel {

    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 17);
    private static final Font FIELD_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 17);

    /** Fixed width of the left entry panel — sized so every field is fully
     * visible without stretching the panel or the window. */
    private static final int LEFT_PANEL_WIDTH = 560;

    private final CustomerService customerService = new CustomerService();
    private final dairy.erp.service.SettingsService settingsService = new dairy.erp.service.SettingsService();
    private final dairy.erp.util.DairyNameLabel dairyNameLabel = new dairy.erp.util.DairyNameLabel();

    private final JTextField codeField = new JTextField(12);
    private final JTextField nameField = new JTextField(20);
    private final JTextField mobileField = new JTextField(14);
    private final JTextField addressField = new JTextField(24);
    private final JTextField villageField = new JTextField(16);
    private final JRadioButton cowRadio = new JRadioButton("Cow");
    private final JRadioButton buffaloRadio = new JRadioButton("Buffalo");
    private final JRadioButton mixRadio = new JRadioButton("Mix");
    private final ButtonGroup milkGroup = new ButtonGroup();
    private final JRadioButton activeRadio = new JRadioButton("Active");
    private final JRadioButton inactiveRadio = new JRadioButton("Inactive");
    private final ButtonGroup statusGroup = new ButtonGroup();
    private final DatePicker regDatePicker = new DatePicker();
    private final JTextField remarksField = new JTextField(24);

    private final JTextField searchCodeField = new JTextField(10);
    private final JTextField searchNameField = new JTextField(14);
    private final JTextField searchMobileField = new JTextField(14);
    private final JComboBox<String> searchStatusBox = new JComboBox<>(new String[]{"All", "Active", "Inactive"});

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Customer Code", "Name", "Mobile", "Milk Type", "Village", "Status", "Reg. Date"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false; // read-only table; edit via the form
        }
    };
    private final JTable table = new JTable(tableModel);

    private Customer current;
    private int editingId = -1;

    // Button references for enable/disable control (same logic as Rate Rule screen)
    private JButton newBtn;
    private JButton saveBtn;
    private JButton updateBtn;
    private JButton deleteBtn;

    public CustomerPanel() {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        initComponents();
        loadDairyName();
        add(buildMain(), BorderLayout.CENTER);
        newRecord(); // start in create mode: New/Save enabled, Update/Delete disabled
        loadAll();
    }

    /** Shows the dairy name (from Settings) in red, 28px bold, above the input fields. */
    private void loadDairyName() {
        // Golden logo-style dairy name (shared DairyNameLabel component).
        dairyNameLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        applyDairyName(settingsService.get("dairy.name"));
        // Live update: re-apply whenever Settings saves a new dairy name.
        dairy.erp.util.AppBus.onDairyNameChanged(this::applyDairyName);
    }

    private void applyDairyName(String name) {
        dairyNameLabel.setText(name == null || name.isBlank() ? "SRS Dairy ERP" : name);
    }

    private JPanel buildMain() {
        // Left column: logo header + "Customer Details" card (fixed sidebar like the design).
        JPanel left = new JPanel(new BorderLayout(8, 4));
        left.setBackground(Color.WHITE);
        left.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xd0, 0xd7, 0xde), 1, true),
                BorderFactory.createEmptyBorder(2, 8, 8, 8)));

        // Header row: logo on the left, dairy name on the right (as in the design).
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.add(UIUtil.header("Customer Management"), BorderLayout.CENTER);
        headerRow.add(dairyNameLabel, BorderLayout.EAST);
        left.add(headerRow, BorderLayout.NORTH);

        // "Customer Details" bold heading above the form fields.
        JPanel formCard = new JPanel(new BorderLayout(0, 4));
        formCard.setOpaque(false);
        JLabel detailsHeading = new JLabel("Customer Details");
        detailsHeading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        detailsHeading.setBorder(BorderFactory.createEmptyBorder(4, 2, 2, 2));
        formCard.add(detailsHeading, BorderLayout.NORTH);
        formCard.add(buildForm(), BorderLayout.CENTER);
        left.add(formCard, BorderLayout.CENTER);
        // Fixed opening size: the entry form always opens LEFT_PANEL_WIDTH px
        // wide and can never shrink below that, so every field is visible
        // properly; extra window width always goes to the table.
        left.setPreferredSize(new Dimension(LEFT_PANEL_WIDTH, 0));
        left.setMinimumSize(new Dimension(LEFT_PANEL_WIDTH, 0));

        // Right column: "Customer List" bold heading above the search bar and table.
        JPanel right = new JPanel(new BorderLayout(4, 4));
        JLabel listHeading = new JLabel("Customer List");
        listHeading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        listHeading.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 2));
        right.add(listHeading, BorderLayout.NORTH);
        right.add(buildTableArea(), BorderLayout.CENTER);
        right.setMinimumSize(new Dimension(400, 0));

        // JSplitPane layout: the left entry panel is kept at its fixed width
        // on every window resize — no dragging needed; all spare width goes
        // to the customer list on the right.
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(LEFT_PANEL_WIDTH);
        // All extra width goes to the table side so the form keeps its size.
        split.setResizeWeight(0.0);
        // A divider location set while the panel has no size yet is clamped,
        // so (re)apply the fixed width once the panel has its real size and
        // again on every subsequent resize — the panel always stays fixed.
        split.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                split.setDividerLocation(LEFT_PANEL_WIDTH);
            }
        });
        split.setContinuousLayout(true);
        split.setBorder(null);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(split, BorderLayout.CENTER);
        return wrapper;
    }

    private void initComponents() {
        // Auto-uppercase text fields
        UIUtil.makeUpperCase(codeField);
        UIUtil.makeUpperCase(nameField);
        UIUtil.makeUpperCase(mobileField);
        UIUtil.makeUpperCase(addressField);
        UIUtil.makeUpperCase(villageField);
        UIUtil.makeUpperCase(remarksField);
        UIUtil.makeUpperCase(searchCodeField);
        UIUtil.makeUpperCase(searchNameField);
        UIUtil.makeUpperCase(searchMobileField);

        // Fonts and sizing
        UIUtil.styleField(codeField, 12);
        // Customer code is auto-generated and must not be edited by the user.
        codeField.setEditable(false);
        codeField.setFocusable(false);
        codeField.setBackground(UIUtil.DISABLED_BG); // light grey read-only
        UIUtil.styleField(nameField, 20);
        UIUtil.styleField(mobileField, 14);
        UIUtil.styleField(addressField, 24);
        UIUtil.styleField(villageField, 16);
        UIUtil.styleField(remarksField, 24);
        UIUtil.styleField(searchCodeField, 10);
        UIUtil.styleField(searchNameField, 14);
        UIUtil.styleField(searchMobileField, 14);
        // Status radio group (Active / Inactive) — filter keeps its combo box.
        statusGroup.add(activeRadio);
        statusGroup.add(inactiveRadio);
        activeRadio.setSelected(true);
        activeRadio.setFont(FIELD_FONT);
        inactiveRadio.setFont(FIELD_FONT);
        // No background behind the radio buttons — they sit on the white card.
        activeRadio.setOpaque(false);
        inactiveRadio.setOpaque(false);
        UIUtil.styleComponent(searchStatusBox, 18);
        UIUtil.styleComponent(regDatePicker.getTextField(), 18);
        UIUtil.styleComponent(regDatePicker.getButton(), 18);

        // Milk type radio group
        milkGroup.add(cowRadio);
        milkGroup.add(buffaloRadio);
        milkGroup.add(mixRadio);
        cowRadio.setSelected(true);
        cowRadio.setFont(FIELD_FONT);
        buffaloRadio.setFont(FIELD_FONT);
        mixRadio.setFont(FIELD_FONT);
        // No background behind the radio buttons — they sit on the white card.
        cowRadio.setOpaque(false);
        buffaloRadio.setOpaque(false);
        mixRadio.setOpaque(false);

        // Readable striped table; shared with Rate Chart for an exact match.
        UIUtil.styleCustomerDetailsTable(table);
        // Balanced column widths so every field reads clearly.
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setPreferredWidth(130);
        table.getColumnModel().getColumn(5).setPreferredWidth(100);
        table.getColumnModel().getColumn(6).setPreferredWidth(110);
        // Status column shows a coloured pill (green Active / grey Inactive).
        table.getColumnModel().getColumn(5).setCellRenderer(new StatusBadgeRenderer());
    }

    private JPanel buildForm() {
        // Fields + action buttons; the white card border comes from buildMain().
        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.setOpaque(false);

        // Left: labels + fields in a grid (small top inset keeps fields high up)
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 12, 10, 12); 
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;


        int row = 0;
        addRow(fieldsPanel, g, row++, "Customer Code:", codeField);
        addRow(fieldsPanel, g, row++, "Customer Name:", nameField);
        addRow(fieldsPanel, g, row++, "Mobile:", mobileField);
        addRow(fieldsPanel, g, row++, "Address:", addressField);
        addRow(fieldsPanel, g, row++, "Village:", villageField);
        addRow(fieldsPanel, g, row++, "Milk Type:", milkTypePanel());
        addRow(fieldsPanel, g, row++, "Status:", statusPanel());
        addRow(fieldsPanel, g, row++, "Registration Date:", regDatePicker);
        addRow(fieldsPanel, g, row++, "Remarks:", remarksField);

        // Buttons: 2x2 coloured grid (New / Save / Update / Delete) as in the design.
        JPanel buttons = new JPanel(new GridLayout(2, 2, 10, 8));
        newBtn = new JButton("+ New");
        UIUtil.styleSmallButton(newBtn, new Color(0x19, 0x76, 0xd2));
        newBtn.setIcon(dairy.erp.util.ButtonIcons.of("Plus", Color.WHITE));
        newBtn.addActionListener(e -> newRecord());
        buttons.add(newBtn);
        saveBtn = new JButton("Save");
        UIUtil.styleSmallButton(saveBtn, new Color(0x2e, 0x7d, 0x32));
        saveBtn.setIcon(dairy.erp.util.ButtonIcons.of("Save", Color.WHITE));
        saveBtn.addActionListener(e -> saveRecord());
        buttons.add(saveBtn);
        updateBtn = new JButton("Update");
        UIUtil.styleSmallButton(updateBtn, new Color(0xef, 0x6c, 0x00));
        updateBtn.setIcon(dairy.erp.util.ButtonIcons.of("Refresh", Color.WHITE));
        updateBtn.addActionListener(e -> updateRecord());
        buttons.add(updateBtn);
        deleteBtn = new JButton("Delete");
        UIUtil.styleSmallButton(deleteBtn, new Color(0xc6, 0x28, 0x28));
        deleteBtn.setIcon(dairy.erp.util.ButtonIcons.of("Trash", Color.WHITE));
        deleteBtn.addActionListener(e -> deleteRecord());
        buttons.add(deleteBtn);

        form.add(fieldsPanel, BorderLayout.CENTER);
        form.add(buttons, BorderLayout.SOUTH);
        return form;
    }

    /** Builds a flow panel containing the three milk-type radio buttons. */
    private JPanel milkTypePanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        p.setOpaque(false); // blend with the white card
        p.add(cowRadio);
        p.add(buffaloRadio);
        p.add(mixRadio);
        return p;
    }

    /** Builds a flow panel containing the Active/Inactive status radio buttons. */
    private JPanel statusPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        p.setOpaque(false); // blend with the white card
        p.add(activeRadio);
        p.add(inactiveRadio);
        return p;
    }

    private String selectedStatus() {
        return inactiveRadio.isSelected() ? "Inactive" : "Active";
    }

    private void setSelectedStatus(String status) {
        if ("Inactive".equalsIgnoreCase(status)) {
            inactiveRadio.setSelected(true);
        } else {
            activeRadio.setSelected(true);
        }
    }



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


    private JPanel buildTableArea() {
        // Single-line search filter (fields sized to fit on one row).
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search"));
        searchPanel.add(styledLabel("Code:"));
        searchPanel.add(searchCodeField);
        searchPanel.add(styledLabel("Name:"));
        searchPanel.add(searchNameField);
        searchPanel.add(styledLabel("Mobile:"));
        searchPanel.add(searchMobileField);
        searchPanel.add(styledLabel("Status:"));
        searchPanel.add(searchStatusBox);
        JButton go = new JButton("Search");
        UIUtil.styleSmallButton(go, new java.awt.Color(0x15, 0x65, 0xc0)); // blue
        go.addActionListener(e -> doSearch());
        searchPanel.add(go);
        JButton reset = new JButton("Reset");
        UIUtil.styleSmallButton(reset, new java.awt.Color(0x0d, 0x47, 0xa1)); // dark blue
        reset.addActionListener(e -> {
            searchCodeField.setText("");
            searchNameField.setText("");
            searchMobileField.setText("");
            searchStatusBox.setSelectedIndex(0);
            loadAll();
        });
        searchPanel.add(reset);

        // Space between the filter row and the table for a cleaner look.
        JPanel area = new JPanel(new BorderLayout(0, 12));
        area.add(searchPanel, BorderLayout.NORTH);
        // The shared Customer Details table style is applied during initialization.
        JScrollPane scroll = new JScrollPane(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    loadSelectedIntoForm();
                }
            }
        });
        area.add(scroll, BorderLayout.CENTER);
        return area;
    }

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(LABEL_FONT);
        return l;
    }

    private void loadAll() {
        tableModel.setRowCount(0);
        for (Customer c : customerService.listAll()) {
            addRowToTable(c);
        }
    }

    private void addRowToTable(Customer c) {
        tableModel.addRow(new Object[]{
                c.getCustomerCode(), c.getCustomerName(), c.getMobile(),
                c.getMilkType(), c.getVillage(), c.getStatus(),
                DateUtil.toDisplay(c.getRegistrationDate())
        });
    }

    private void doSearch() {
        String status = "All".equals(searchStatusBox.getSelectedItem())
                ? null : (String) searchStatusBox.getSelectedItem();
        List<Customer> list = customerService.search(
                searchCodeField.getText(), searchNameField.getText(),
                searchMobileField.getText(), status);
        tableModel.setRowCount(0);
        for (Customer c : list) {
            addRowToTable(c);
        }
    }


    // ---- record operations ----

    /**
     * Resets the form to blank/new-entry mode.
     * Called automatically when navigating to the Customer Management screen.
     */
    public void resetForNewEntry() {
        clearFields();
        try {
            codeField.setText(customerService.nextCode());
        } catch (Exception ignored) { }
        editingId = -1;
        current = null;
        toggleButtons(false);
    }


    private void newRecord() {
        clearFields();
        codeField.setText(customerService.nextCode());
        regDatePicker.setDate(LocalDate.now());
        editingId = -1;
        current = null;
        toggleButtons(false);
        nameField.requestFocusInWindow();
    }

    private void saveRecord() {
        String error = validateForm();
        if (!error.isEmpty()) {
            UIUtil.showMessage(this, error, "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Customer c = collectForm();
        // Always generate the code fresh at save time so a code pre-filled
        // earlier (while the form sat open) can never collide with a customer
        // added in the meantime.
        String generated = customerService.nextCode();
        c.setCustomerCode(generated);
        codeField.setText(generated);
        if (customerService.isCodeTaken(c.getCustomerCode(), -1)) {
            UIUtil.showMessage(this,
                    "Customer code '" + c.getCustomerCode() + "' already exists.",
                    "Duplicate", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            customerService.add(c);
            UIUtil.showMessage(this, "Customer saved successfully.",
                    "Save", JOptionPane.INFORMATION_MESSAGE);
            loadAll();
            newRecord(); // clear the form and generate the next customer code
        } catch (RuntimeException ex) {
            UIUtil.showMessage(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateRecord() {
        if (editingId <= 0) {
            UIUtil.showMessage(this,
                    "Double-click a row in the list to load it for updating.",
                    "Update", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String error = validateForm();
        if (!error.isEmpty()) {
            UIUtil.showMessage(this, error, "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Customer c = collectForm();
        c.setId(editingId);
        if (customerService.isCodeTaken(c.getCustomerCode(), editingId)) {
            UIUtil.showMessage(this,
                    "Customer code '" + c.getCustomerCode() + "' already exists.",
                    "Duplicate", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            customerService.update(c);
            UIUtil.showMessage(this, "Customer updated successfully.",
                    "Update", JOptionPane.INFORMATION_MESSAGE);
            loadAll();
            clearFields();
        } catch (RuntimeException ex) {
            UIUtil.showMessage(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void deleteRecord() {
        if (editingId <= 0) {
            UIUtil.showMessage(this,
                    "Double-click a row in the list to load it for deletion.",
                    "Delete", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String name = nameField.getText();
        int confirm = UIUtil.confirm(this,
                "Are you sure you want to delete this customer?\n" + name,
                "Delete Customer");
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            String result = customerService.deleteOrDeactivate(editingId);
            String msg = "DELETED".equals(result)
                    ? "Customer deleted successfully."
                    : "Customer has transaction history, so it was archived as Inactive.";
            UIUtil.showMessage(this, msg, "Delete", JOptionPane.INFORMATION_MESSAGE);
            loadAll();
            clearFields();
        } catch (RuntimeException ex) {
            UIUtil.showMessage(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printList() {
        List<String> lines = new ArrayList<>();
        lines.add("Customer List");
        lines.add("=".repeat(70));
        lines.add(String.format("%-10s %-18s %-12s %-9s %-14s %-9s %s",
                "Code", "Name", "Mobile", "Milk Type", "Village", "Status", "Reg. Date"));
        lines.add("-".repeat(70));
        for (Customer c : customerService.listAll()) {
            lines.add(String.format("%-10s %-18s %-12s %-9s %-14s %-9s %s",
                    c.getCustomerCode(), truncate(c.getCustomerName(), 18),
                    nullToEmpty(c.getMobile()), nullToEmpty(c.getMilkType()),
                    truncate(nullToEmpty(c.getVillage()), 14), nullToEmpty(c.getStatus()),
                    DateUtil.toDisplay(c.getRegistrationDate())));
        }
        PrintUtil.printText(findOwner(), "Customer List", lines, true);
    }


    // ---- helpers ----

    private String validateForm() {
        if (ValidationUtil.isBlank(codeField.getText())) {
            return "Customer Code is required.";
        }
        if (ValidationUtil.isBlank(nameField.getText())) {
            return "Customer Name is required.";
        }
        if (!ValidationUtil.isBlank(mobileField.getText()) && !ValidationUtil.isValidMobile(mobileField.getText())) {
            return "Mobile number must be 10 digits.";
        }
        return "";
    }

    private Customer collectForm() {
        Customer c = new Customer();
        c.setCustomerCode(codeField.getText().trim());
        c.setCustomerName(nameField.getText().trim());
        c.setMobile(mobileField.getText().trim());
        c.setAddress(addressField.getText().trim());
        c.setVillage(villageField.getText().trim());
        c.setMilkType(getSelectedMilkType());
        c.setStatus(selectedStatus());
        c.setRegistrationDate(regDatePicker.getDate());
        c.setRemarks(remarksField.getText());
        return c;
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


    private void clearFields() {
        codeField.setText("");
        nameField.setText("");
        mobileField.setText("");
        addressField.setText("");
        villageField.setText("");
        remarksField.setText("");
        cowRadio.setSelected(true);
        activeRadio.setSelected(true);
        regDatePicker.setDate(LocalDate.now());
        editingId = -1;
        current = null;
        toggleButtons(false);
    }

    private void loadSelectedIntoForm() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        String code = (String) tableModel.getValueAt(row, 0);
        Customer c = customerService.findByCode(code);
        if (c == null) {
            return;
        }
        current = c;
        editingId = c.getId();
        codeField.setText(c.getCustomerCode());
        nameField.setText(c.getCustomerName());
        mobileField.setText(c.getMobile());
        addressField.setText(c.getAddress());
        villageField.setText(c.getVillage());
        remarksField.setText(c.getRemarks());
        selectMilkType(c.getMilkType() == null ? "Cow" : c.getMilkType());
        setSelectedStatus(c.getStatus() == null ? "Active" : c.getStatus());
        regDatePicker.setDate(c.getRegistrationDate() == null ? LocalDate.now() : c.getRegistrationDate());
        toggleButtons(true);
    }

    /** Disables New/Save while editing and restores them after the operation (Rate Rule logic). */
    private void toggleButtons(boolean editing) {
        newBtn.setEnabled(!editing);
        saveBtn.setEnabled(!editing);
        updateBtn.setEnabled(editing);
        deleteBtn.setEnabled(editing);
    }

    private void selectMilkType(String milkType) {
        if ("Buffalo".equalsIgnoreCase(milkType)) {
            buffaloRadio.setSelected(true);
        } else if ("Mix".equalsIgnoreCase(milkType)) {
            mixRadio.setSelected(true);
        } else {
            cowRadio.setSelected(true);
        }
    }

    /**
     * Renderer for the Status column: paints the row background with the same
     * striping/selection colours as the shared table style, then draws a
     * coloured pill — green for Active, grey for Inactive — as in the design.
     */
    private static class StatusBadgeRenderer extends JPanel
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
                boolean inactive = "Inactive".equalsIgnoreCase(text);
                Color pill = inactive ? new Color(0xe0, 0xe0, 0xe0) : new Color(0xc8, 0xe6, 0xc9);
                Color textColour = inactive ? new Color(0x61, 0x61, 0x61) : new Color(0x1b, 0x5e, 0x20);
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

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s == null ? "" : s;
        }
        return s.substring(0, max);
    }

    private java.awt.Frame findOwner() {
        java.awt.Container c = this;
        while (c != null && !(c instanceof JFrame)) {
            c = c.getParent();
        }
        return (java.awt.Frame) c;
    }
}
