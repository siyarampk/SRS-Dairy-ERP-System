package dairy.erp.ui;

import dairy.erp.model.Customer;
import dairy.erp.model.Payment;
import dairy.erp.service.PaymentService;
import dairy.erp.util.CurrencyUtil;
import dairy.erp.util.DateUtil;
import dairy.erp.util.PrintUtil;
import dairy.erp.util.UIUtil;
import dairy.erp.util.ValidationUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import javax.swing.JSplitPane;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.awt.Font;
import java.awt.Color;

/**
 * Payment management: record/edit/delete customer payments, search and print.
 */
public class PaymentPanel extends JPanel {

    private static final java.awt.Font LABEL_FONT = new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 16);

    private final PaymentService paymentService = new PaymentService();
    private final dairy.erp.service.SettingsService settingsService = new dairy.erp.service.SettingsService();
    private final JLabel dairyNameLabel = new JLabel("", javax.swing.SwingConstants.CENTER);

    private final DatePicker datePicker = new DatePicker();
    private final JTextField customerCodeField = new JTextField(12);
    private final JTextField customerNameField = new JTextField(18);
    private final JTextField amountField = new JTextField(12);
    private final JComboBox<String> modeBox = new JComboBox<>(new String[]{"Cash", "Bank", "UPI", "Other"});
    private final JTextField referenceField = new JTextField(16);
    private final JTextField remarksField = new JTextField(16);
    private final JLabel customerBanner = UIUtil.greenBanner("");

    // Panel mode: "new" (entry form) or "history" (records).
    private boolean historyMode = false;
    private final JPanel headerPanel = UIUtil.header("New Payment");
    private final JButton newButton = new JButton("New");
    private final JButton saveButton = new JButton("Save");
    private final JButton updateButton = new JButton("Update");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton printButton = new JButton("Print");
    private final JPanel buttonPanel = new JPanel(new GridLayout(2, 4, 8, 6));

   

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Date", "Code", "Name", "Amount", "Mode", "Reference", "Remarks"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    private Customer selectedCustomer;
    private int editingId = -1;

    public PaymentPanel() {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        datePicker.setDate(LocalDate.now());
        customerNameField.setEditable(false);
        UIUtil.makeUpperCase(customerCodeField);
        customerCodeField.addActionListener(e -> loadCustomer());
        loadDairyName();
        add(buildMain(), BorderLayout.CENTER);
        loadTable(paymentService.search(0, null, null));
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
        // Left column: logo header + payment fields. Right column: grid.
        JPanel left = new JPanel(new BorderLayout(10, 6));
        left.add(headerPanel, BorderLayout.NORTH);
        JPanel formHome = new JPanel(new BorderLayout(8, 4));
        formHome.add(customerBanner, BorderLayout.NORTH);
        formHome.add(buildForm(), BorderLayout.CENTER);
        left.add(formHome, BorderLayout.CENTER);
        left.setMinimumSize(new Dimension(350, 0));
        left.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));

        JPanel right = new JPanel(new BorderLayout(6, 6));
        right.setBorder(BorderFactory.createTitledBorder("Payment History"));
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




    private JPanel buildForm() {
        // Outer container mirrors Customer Details: dairy name heading on top,
        // fields in the centre, buttons pinned at the bottom.
        JPanel outer = new JPanel(new BorderLayout(12, 8));
        outer.setMinimumSize(new Dimension(350, 600));
        outer.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0x1a, 0x5f, 0x7a)),
                "Payment"));
        // Dairy name heading sits directly above the input fields,
        // loaded exactly like the Customer Details screen.
        outer.add(dairyNameLabel, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        //g.insets = new Insets(6, 10, 6, 10);
        g.insets = new Insets(25, 10, 10, 10); 
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.VERTICAL;
        g.weightx = 1.0; 

        int row = 0;
        // Vertical stack: one field per row, full remaining width.
        addRow(form, g, row++, "Payment Date:", datePicker);
        addRow(form, g, row++, "Payment Mode:", modeBox);
        addRow(form, g, row++, "Customer Code:", customerCodeField);
        addRow(form, g, row++, "Customer Name:", customerNameField);
        addRow(form, g, row++, "Amount:", amountField);
        addRow(form, g, row++, "Reference:", referenceField);
        addRow(form, g, row++, "Remarks:", remarksField);

        // Buttons: mode-aware grid with the same size, font and colours as
        // Customer Details — New/Save in entry mode; Update/Delete/Print in history.
        styleActionButtons();
        rebuildButtons();

        outer.add(form, BorderLayout.CENTER);
        outer.add(buttonPanel, BorderLayout.SOUTH);
        return outer;
    }

    /** Styles every action button with the same look as the Customer Details page. */
    private void styleActionButtons() {
        UIUtil.styleSmallButton(newButton, new Color(0x1976D2));    // blue
        UIUtil.styleSmallButton(saveButton, new Color(0x2E7D32));   // green
        UIUtil.styleSmallButton(updateButton, new Color(0xEF6C00)); // orange
        UIUtil.styleSmallButton(deleteButton, new Color(0xC62828)); // red
        UIUtil.styleSmallButton(printButton, new Color(0x607D8B));  // grey
        newButton.addActionListener(e -> resetForm());
        saveButton.addActionListener(e -> saveRecord());
        updateButton.addActionListener(e -> updateSelected());
        deleteButton.addActionListener(e -> deleteSelected());
        printButton.addActionListener(e -> printList());
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
            title.setText(historyMode ? " Payment History" : " New Payment");
        }
        rebuildButtons();
    }

    /**
     * History-mode Update: loads the selected payment into the form the first
     * time, then commits the edited values through the normal update path.
     */
    private void updateSelected() {
        if (editingId == -1) {
            if (table.getSelectedRow() < 0) {
                JOptionPane.showMessageDialog(this,
                        "Select a payment from the history table first.",
                        "Update", JOptionPane.WARNING_MESSAGE);
                return;
            }
            loadSelected();
            return; // record loaded — edit the fields, then press Update again to save
        }
        saveRecord();
    }

    /** History-mode Delete: works on the selected history row, loaded or not. */
    private void deleteSelected() {
        if (editingId <= 0 && table.getSelectedRow() >= 0) {
            loadSelected();
        }
        deleteRecord();
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

    private void addButton(JPanel panel, String text, java.awt.event.ActionListener listener) {
        JButton b = new JButton(text);
        UIUtil.styleButton(b);
        b.addActionListener(listener);
        panel.add(b);
    }

    /** Adds a colored button styled exactly like the Customer Management buttons. */
    private void addColoredButton(JPanel panel, String text, Color bg, java.awt.event.ActionListener listener) {
        JButton b = new JButton(text);
        UIUtil.styleSmallButton(b, bg);
        b.addActionListener(listener);
        panel.add(b);
    }

    private JPanel buildTable() {
        JPanel area = new JPanel(new BorderLayout());
        JScrollPane scroll = new JScrollPane(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UIUtil.styleCustomerDetailsTable(table);
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

    private void loadCustomer() {
        String code = customerCodeField.getText().trim();
        if (ValidationUtil.isBlank(code)) {
            return;
        }
        Customer c = paymentService.findCustomerByCode(code);
        if (c == null) {
            JOptionPane.showMessageDialog(this, "Customer not found: " + code,
                    "Customer", JOptionPane.WARNING_MESSAGE);
            selectedCustomer = null;
            customerNameField.setText("");
            customerBanner.setText("");
            return;
        }
        selectedCustomer = c;
        customerNameField.setText(c.getCustomerName());
        String mobile = c.getMobile();
        customerBanner.setText(c.getCustomerName()
                + (mobile == null || mobile.isBlank() ? "" : "  |  " + mobile));
        amountField.requestFocusInWindow();
    }

    private void loadTable(List<Payment> list) {
        tableModel.setRowCount(0);
        for (Payment p : list) {
            tableModel.addRow(new Object[]{
                    DateUtil.toDisplay(p.getPaymentDate()), p.getCustomerCode(), p.getCustomerName(),
                    CurrencyUtil.formatMoney(p.getAmount()), p.getPaymentMode(),
                    p.getReference(), p.getRemarks()
            });
        }
    }

    private void saveRecord() {
        String error = validateForm();
        if (!error.isEmpty()) {
            JOptionPane.showMessageDialog(this, error, "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Payment p = new Payment();
        p.setCustomerId(selectedCustomer.getId());
        p.setPaymentDate(datePicker.getDate());
        p.setAmount(ValidationUtil.parseDecimal(amountField.getText()));
        p.setPaymentMode((String) modeBox.getSelectedItem());
        p.setReference(referenceField.getText());
        p.setRemarks(remarksField.getText());
        try {
            if (editingId > 0) {
                p.setId(editingId);
                paymentService.update(p);
            } else {
                paymentService.add(p);
            }
            JOptionPane.showMessageDialog(this, "Payment saved successfully.", "Save",
                    JOptionPane.INFORMATION_MESSAGE);
            loadTable(paymentService.search(0, null, null));
            resetForm();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteRecord() {
        if (editingId <= 0) {
            JOptionPane.showMessageDialog(this, "Select a payment (Update) to delete first.",
                    "Delete", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this payment?", "Delete Payment",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            paymentService.delete(editingId);
            loadTable(paymentService.search(0, null, null));
            resetForm();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void loadSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a payment row to edit.", "Edit",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String code = (String) tableModel.getValueAt(row, 1);
        Customer c = paymentService.findCustomerByCode(code);
        if (c == null) {
            return;
        }
        List<Payment> all = paymentService.search(c.getId(), null, null);
        String dateStr = (String) tableModel.getValueAt(row, 0);
        BigDecimal amt = parseMoney((String) tableModel.getValueAt(row, 3));
        for (Payment p : all) {
            if (DateUtil.toDisplay(p.getPaymentDate()).equals(dateStr)
                    && p.getAmount().compareTo(amt) == 0) {
                editingId = p.getId();
                selectedCustomer = c;
                customerCodeField.setText(c.getCustomerCode());
                customerNameField.setText(c.getCustomerName());
                customerBanner.setText(c.getCustomerName()
                        + (c.getMobile() == null || c.getMobile().isBlank() ? ""
                        : "  |  " + c.getMobile()));
                datePicker.setDate(p.getPaymentDate());
                amountField.setText(p.getAmount().toPlainString());
                modeBox.setSelectedItem(p.getPaymentMode());
                referenceField.setText(p.getReference());
                remarksField.setText(p.getRemarks());
                return;
            }
        }
    }

    private BigDecimal parseMoney(String s) {
        if (s == null) {
            return BigDecimal.ZERO;
        }
        String cleaned = s.replace(CurrencyUtil.getSymbol(), "").replace(",", "").replace(" ", "");
        BigDecimal parsed = ValidationUtil.parseDecimal(cleaned);
        return parsed == null ? BigDecimal.ZERO : parsed;
    }

    private String validateForm() {
        if (datePicker.getDate() == null) {
            return "A valid payment date (dd-MM-yyyy) is required.";
        }
        if (selectedCustomer == null) {
            return "Please enter a valid customer code.";
        }
        BigDecimal amt = ValidationUtil.parseDecimal(amountField.getText());
        if (amt == null || amt.signum() <= 0) {
            return "Amount must be greater than zero.";
        }
        return "";
    }


    public void resetForm() {
        selectedCustomer = null;
        editingId = -1;
        customerCodeField.setText("");
        customerNameField.setText("");
        amountField.setText("");
        referenceField.setText("");
        remarksField.setText("");
        datePicker.setDate(LocalDate.now());
        modeBox.setSelectedIndex(0);
        customerBanner.setText("");
        customerCodeField.requestFocusInWindow();
    }

    private void printList() {
        List<String> lines = new ArrayList<>();
        lines.add("Payment Report");
        lines.add("=".repeat(70));
        lines.add(String.format("%-10s %-8s %-16s %-10s %-6s %s",
                "Date", "Code", "Name", "Amount", "Mode", "Reference"));
        lines.add("-".repeat(70));
        for (Payment p : paymentService.search(0, null, null)) {
            lines.add(String.format("%-10s %-8s %-16s %-10s %-6s %s",
                    DateUtil.toDisplay(p.getPaymentDate()), nullToEmpty(p.getCustomerCode()),
                    truncate(nullToEmpty(p.getCustomerName()), 16),
                    CurrencyUtil.formatMoney(p.getAmount()), nullToEmpty(p.getPaymentMode()),
                    nullToEmpty(p.getReference())));
        }
        PrintUtil.printText(findOwner(), "Payment Report", lines, true);
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

