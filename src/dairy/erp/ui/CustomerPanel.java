package dairy.erp.ui;

import dairy.erp.model.Customer;
import dairy.erp.service.CustomerService;
import dairy.erp.util.DateUtil;
import dairy.erp.util.PrintUtil;
import dairy.erp.util.UIUtil;
import dairy.erp.util.ValidationUtil;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
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
import java.awt.Image;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.File;
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

    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 16);
    private static final Font FIELD_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 16);

    private final CustomerService customerService = new CustomerService();
    private final dairy.erp.service.SettingsService settingsService = new dairy.erp.service.SettingsService();
    private final JLabel dairyNameLabel = new JLabel("", javax.swing.SwingConstants.CENTER);

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
        // Left column: logo header + customer fields. Right column: grid.
        JPanel left = new JPanel(new BorderLayout(8, 6));
        left.add(UIUtil.header("Customer Management"), BorderLayout.NORTH);
        left.add(buildForm(), BorderLayout.CENTER);
        left.setMinimumSize(new Dimension(350, 0));
        left.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));

        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.setBorder(BorderFactory.createTitledBorder("Customer List"));
        right.add(buildTableArea(), BorderLayout.CENTER);
        right.setMinimumSize(new Dimension(400, 0));

        // JSplitPane allows stretching left/right by dragging the divider
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(460);
        split.setResizeWeight(0.35);
        split.setContinuousLayout(true);

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

        // Readable striped table; shared with Rate Chart for an exact match.
        UIUtil.styleCustomerDetailsTable(table);
    }

    private JPanel buildForm() {
        // Outer container: left = fields, right = image placeholder, bottom = buttons
        JPanel form = new JPanel(new BorderLayout(12, 8));
        form.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new java.awt.Color(0x1a, 0x5f, 0x7a)),
                "Customer Details"));
        // Dairy name heading sits directly above the input fields.
        form.add(dairyNameLabel, BorderLayout.NORTH);

        // Left: labels + fields in a grid (small top inset keeps fields high up)
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setMinimumSize(new Dimension(350, 600));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(25, 10, 10, 10); 
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

                // Buttons: compact coloured buttons (2x4 grid so every button is visible).
        JPanel buttons = new JPanel(new GridLayout(2, 4, 8, 6));
        newBtn = new JButton("New");
        UIUtil.styleSmallButton(newBtn, new Color(0x1976D2));
        newBtn.addActionListener(e -> newRecord());
        buttons.add(newBtn);
        saveBtn = new JButton("Save");
        UIUtil.styleSmallButton(saveBtn, new Color(0x2E7D32));
        saveBtn.addActionListener(e -> saveRecord());
        buttons.add(saveBtn);
        updateBtn = new JButton("Update");
        UIUtil.styleSmallButton(updateBtn, new Color(0xEF6C00));
        updateBtn.addActionListener(e -> updateRecord());
        buttons.add(updateBtn);
        deleteBtn = new JButton("Delete");
        UIUtil.styleSmallButton(deleteBtn, new Color(0xC62828));
        deleteBtn.addActionListener(e -> deleteRecord());
        buttons.add(deleteBtn);

        form.add(fieldsPanel, BorderLayout.CENTER);
        form.add(buttons, BorderLayout.SOUTH);
        return form;
    }

    /**
     * The right-side image area. Shows only the registration-form banner image,
     * sized tightly to the scaled image (no extra border or padding). Falls back
     * to a small placeholder if the image cannot be loaded.
     */
    private JPanel buildImagePlaceholder() {
        BufferedImage img = loadSectionImage();
        if (img != null) {
            int targetWidth = 440;
            double ratio = (double) img.getHeight(null) / img.getWidth(null);
            int targetHeight = (int) Math.round(targetWidth * ratio);
            Image scaled = img.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);

            JPanel box = new JPanel(new BorderLayout());
            box.setOpaque(false);
            box.add(new JLabel(new ImageIcon(scaled)), BorderLayout.CENTER);
            box.setPreferredSize(new Dimension(targetWidth, targetHeight));
            return box;
        }

        // Fallback placeholder when the image file is unavailable
        JPanel box = new JPanel(new BorderLayout());
        JLabel placeholder = new JLabel("Photo", JLabel.CENTER);
        placeholder.setFont(LABEL_FONT.deriveFont(Font.BOLD));
        placeholder.setForeground(new java.awt.Color(0x88, 0x88, 0x88));
        box.add(placeholder, BorderLayout.CENTER);
        box.setPreferredSize(new Dimension(220, 250));
        box.setBorder(BorderFactory.createLineBorder(new java.awt.Color(0xcc, 0xcc, 0xcc), 2));
        return box;
    }


    /** Loads the registration-form banner image from resources/images. */
    private BufferedImage loadSectionImage() {
        File image = dairy.erp.config.AppConfig.getBaseDir()
                .resolve("resources/images/banner.png").toFile();
        try {
            if (image.exists()) {
                return javax.imageio.ImageIO.read(image);
            }
        } catch (Exception e) {
            // ignore; fall back to placeholder
        }
        return null;
    }



    /** Builds a flow panel containing the three milk-type radio buttons. */
    private JPanel milkTypePanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        p.add(cowRadio);
        p.add(buffaloRadio);
        p.add(mixRadio);
        return p;
    }

    /** Builds a flow panel containing the Active/Inactive status radio buttons. */
    private JPanel statusPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
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

    // private void addRow(JPanel form, GridBagConstraints g, int row, String label, java.awt.Component comp) {
    //     JLabel lbl = new JLabel(label);
    //     lbl.setFont(LABEL_FONT);
    //     g.gridx = 0;
    //     g.gridy = row;
    //     g.gridwidth = 1;
    //     g.weightx = 0;
    //     g.fill = GridBagConstraints.NONE;
    //     form.add(lbl, g);
    //     g.gridx = 1;
    //     g.gridwidth = 3;
    //     g.weightx = 2.0;
    //     g.fill = GridBagConstraints.HORIZONTAL;
    //     form.add(comp, g);
    // }

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
        UIUtil.styleSmallButton(go, new java.awt.Color(0x1e, 0x8e, 0x3e)); // green
        go.addActionListener(e -> doSearch());
        searchPanel.add(go);
        JButton reset = new JButton("Reset");
        UIUtil.styleSmallButton(reset, new java.awt.Color(0xd3, 0x2f, 0x2f)); // red
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
            JOptionPane.showMessageDialog(this, error, "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Customer c = collectForm();
        if (customerService.isCodeTaken(c.getCustomerCode(), -1)) {
            JOptionPane.showMessageDialog(this,
                    "Customer code '" + c.getCustomerCode() + "' already exists.",
                    "Duplicate", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            customerService.add(c);
            JOptionPane.showMessageDialog(this, "Customer saved successfully.",
                    "Save", JOptionPane.INFORMATION_MESSAGE);
            loadAll();
            clearFields();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateRecord() {
        if (editingId <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Double-click a row in the list to load it for updating.",
                    "Update", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String error = validateForm();
        if (!error.isEmpty()) {
            JOptionPane.showMessageDialog(this, error, "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Customer c = collectForm();
        c.setId(editingId);
        if (customerService.isCodeTaken(c.getCustomerCode(), editingId)) {
            JOptionPane.showMessageDialog(this,
                    "Customer code '" + c.getCustomerCode() + "' already exists.",
                    "Duplicate", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            customerService.update(c);
            JOptionPane.showMessageDialog(this, "Customer updated successfully.",
                    "Update", JOptionPane.INFORMATION_MESSAGE);
            loadAll();
            clearFields();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void deleteRecord() {
        if (editingId <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Double-click a row in the list to load it for deletion.",
                    "Delete", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String name = nameField.getText();
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this customer?\n" + name,
                "Delete Customer", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            String result = customerService.deleteOrDeactivate(editingId);
            String msg = "DELETED".equals(result)
                    ? "Customer deleted successfully."
                    : "Customer has transaction history, so it was archived as Inactive.";
            JOptionPane.showMessageDialog(this, msg, "Delete", JOptionPane.INFORMATION_MESSAGE);
            loadAll();
            clearFields();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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

