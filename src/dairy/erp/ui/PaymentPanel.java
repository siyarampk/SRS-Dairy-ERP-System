package dairy.erp.ui;

import dairy.erp.model.Customer;
import dairy.erp.model.Payment;
import dairy.erp.service.PaymentService;
import dairy.erp.util.ButtonIcons;
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
import javax.swing.JSplitPane;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Payment management: record/edit/delete customer payments, search and print.
 * <p>
 * UI (rebuilt from the "New Payment" reference design): a fixed-width left
 * column with the logo + dairy name, a "New Payment Entry" heading, and a
 * white rounded card of fields — Payment Date, Payment Mode, Customer Code
 * (with a trailing magnifier icon), read-only Customer Name, Amount,
 * Reference and Remarks — each shown as grey placeholder text while empty.
 * Blue "+ New" and green "Save & Print" buttons with white icons are pinned
 * at the bottom. The right column shows a bold "Payment History" heading
 * above the teal-headed striped history table.
 */
public class PaymentPanel extends JPanel {

    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 16);
    private static final Color FIELD_BORDER = new Color(0xC9, 0xD3, 0xDA);
    private static final Color GHOST_GREY = new Color(0x9a, 0xa5, 0xaf);

    private final PaymentService paymentService = new PaymentService();
    private final dairy.erp.service.SettingsService settingsService = new dairy.erp.service.SettingsService();
    private final dairy.erp.util.DairyNameLabel dairyNameLabel = new dairy.erp.util.DairyNameLabel();

    private final DatePicker datePicker = new DatePicker();
    private final JTextField customerCodeField = new GhostField("Customer Code", 12);
    private final JTextField customerNameField = new GhostField("Customer Name", 18);
    private final JTextField amountField = new GhostField("Amount", 12);
    private final JComboBox<String> modeBox = new JComboBox<>(new String[]{"Cash", "Bank", "UPI", "Other"});
    private final JTextField referenceField = new GhostField("Reference", 16);
    // Remarks is a multi-line box in the reference design.
    private final javax.swing.JTextArea remarksArea = new javax.swing.JTextArea(6, 18);
    // Form heading follows the selected menu: "New Payment Entry" (new) or
    // "Payment" (history), as in the reference screenshots.
    private final JLabel entryHeading = new JLabel("New Payment Entry");

    // Panel mode: "new" (entry form) or "history" (records).
    private boolean historyMode = false;
    private final JPanel headerPanel = UIUtil.header("New Payment");
    private final JButton newButton = new JButton("New");
    private final JButton savePrintButton = new JButton("Save & Print");
    private final JButton updatePrintButton = new JButton("Update & Print");
    private final JButton deleteButton = new JButton("Delete");
    private final JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 8)) {
        @Override
        public boolean isOpaque() {
            return false;
        }
    };

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
        customerNameField.setFocusable(false);
        UIUtil.makeUpperCase(customerCodeField);
        // Amount accepts only integer/fraction values.
        UIUtil.allowDecimalOnly(amountField);
        styleFields();
        // Enter in customer code loads the customer; the same lookup also
        // runs when the field loses focus (Tab out or click elsewhere).
        customerCodeField.addActionListener(e -> loadCustomer());
        customerCodeField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                String code = customerCodeField.getText().trim();
                if (code.isEmpty()) {
                    return;
                }
                boolean alreadyLoaded = selectedCustomer != null
                        && code.equalsIgnoreCase(selectedCustomer.getCustomerCode());
                if (!alreadyLoaded) {
                    loadCustomer();
                }
            }
        });
        loadDairyName();
        add(buildMain(), BorderLayout.CENTER);
        loadTable(paymentService.search(0, null, null));
    }

    /** Field fonts + rounded soft borders matching the reference design. */
    private void styleFields() {
        UIUtil.styleField(customerCodeField, 12);
        UIUtil.styleField(customerNameField, 16);
        UIUtil.styleField(amountField, 12);
        UIUtil.styleField(referenceField, 14);
        styleRoundedField(customerNameField);
        // Customer name is auto-filled/read-only: light grey like every other
        // disabled field in the application.
        customerNameField.setBackground(UIUtil.DISABLED_BG);
        styleRoundedField(amountField);
        styleRoundedField(referenceField);
        // Remarks: multi-line, word-wrapped, rounded border like the fields.
        remarksArea.setLineWrap(true);
        remarksArea.setWrapStyleWord(true);
        remarksArea.setFont(remarksArea.getFont().deriveFont(16f));
        remarksArea.setBackground(Color.WHITE);
        remarksArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        // Payment mode combo and the date picker's text field.
        UIUtil.styleComponent(modeBox, 16);
        modeBox.setBackground(Color.WHITE);
        datePicker.getTextField().setColumns(9);
        UIUtil.styleComponent(datePicker.getTextField(), 16);
        datePicker.getTextField().setBackground(Color.WHITE);
        datePicker.getTextField().setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        datePicker.getButton().setPreferredSize(new Dimension(34, 30));
    }

    /** Shows the dairy name in the golden logo style, beside the logo. */
    private void loadDairyName() {
        dairyNameLabel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 12));
        applyDairyName(settingsService.get("dairy.name"));
        // Live update: re-apply whenever Settings saves a new dairy name.
        dairy.erp.util.AppBus.onDairyNameChanged(this::applyDairyName);
    }

    private void applyDairyName(String name) {
        dairyNameLabel.setText(name == null || name.isBlank() ? "SRS Dairy ERP" : name);
    }

    private JPanel buildMain() {
        // Left column: white card — logo + dairy name, "New Payment Entry"
        // heading, the field card, and the action buttons at the bottom.
        JPanel left = new JPanel(new BorderLayout(8, 6));
        left.setBackground(Color.WHITE);
        left.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xd0, 0xd7, 0xde), 1, true),
                BorderFactory.createEmptyBorder(2, 8, 8, 8)));
        // Header row: logo on the left, dairy name ("ANIL DAIRY") beside it.
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.add(headerPanel, BorderLayout.CENTER);
        headerRow.add(dairyNameLabel, BorderLayout.EAST);
        left.add(headerRow, BorderLayout.NORTH);
        left.add(buildForm(), BorderLayout.CENTER);
        // Fixed opening width; extra window width always goes to the table.
        left.setPreferredSize(new Dimension(420, 0));
        left.setMinimumSize(new Dimension(420, 0));

        // Right column: bold "Payment History" heading above the table.
        JPanel right = new JPanel(new BorderLayout(4, 4));
        JLabel historyHeading = new JLabel("Payment History");
        historyHeading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        historyHeading.setBorder(BorderFactory.createEmptyBorder(2, 6, 6, 2));
        right.add(historyHeading, BorderLayout.NORTH);
        right.add(buildTable(), BorderLayout.CENTER);
        right.setMinimumSize(new Dimension(400, 0));

        // JSplitPane allows stretching left/right by dragging the divider.
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(420);
        split.setResizeWeight(0.0);
        split.setContinuousLayout(true);
        split.setBorder(null);
        // JSplitPane clamps the divider while the panel has no size yet —
        // apply the fixed width once the panel gets its real size (one-shot).
        split.addComponentListener(new java.awt.event.ComponentAdapter() {
            private boolean applied = false;

            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (!applied && split.getWidth() > 0) {
                    applied = true;
                    split.setDividerLocation(420);
                }
            }
        });

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(split, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildForm() {
        // "New Payment Entry" bold heading above a white rounded field card,
        // with the action buttons pinned at the bottom of the column.
        JPanel form = new JPanel(new BorderLayout(0, 8));
        form.setOpaque(false);

        JLabel heading = entryHeading;
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        heading.setBorder(BorderFactory.createEmptyBorder(6, 2, 6, 2));
        form.add(heading, BorderLayout.NORTH);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xd0, 0xd7, 0xde), 1, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(12, 6, 12, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;

        int row = 0;
        addRow(card, g, row++, "Payment Date:", datePicker);
        addRow(card, g, row++, "Payment Mode:", modeBox);
        addRow(card, g, row++, "Customer Code:", withSearchIcon(customerCodeField));
        addRow(card, g, row++, "Customer Name:", customerNameField);
        addRow(card, g, row++, "Amount:", amountField);
        addRow(card, g, row++, "Reference:", referenceField);
        // Multi-line remarks box inside its own rounded scroll pane.
        JScrollPane remarksScroll = new JScrollPane(remarksArea);
        remarksScroll.setBorder(BorderFactory.createLineBorder(FIELD_BORDER, 1, true));
        remarksScroll.setPreferredSize(new Dimension(10, 130));
        addRow(card, g, row++, "Remarks:", remarksScroll);

        styleActionButtons();
        rebuildButtons();

        form.add(card, BorderLayout.CENTER);
        form.add(buttonPanel, BorderLayout.SOUTH);
        return form;
    }

    /** Wraps the customer code field with a trailing magnifier icon. */
    private javax.swing.JComponent withSearchIcon(JTextField field) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Color.WHITE);
        wrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(2, 2, 2, 8)));
        field.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JLabel icon = new JLabel(ButtonIcons.of("Search", UIUtil.fieldIconGrey()));
        icon.setOpaque(false);
        wrap.add(field, BorderLayout.CENTER);
        wrap.add(icon, BorderLayout.EAST);
        return wrap;
    }

    /** White field with a rounded soft border, as in the reference design. */
    private void styleRoundedField(JTextField field) {
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }

    /** Styles every action button with the same look as Customer Management. */
    private void styleActionButtons() {
        UIUtil.styleSmallButton(newButton, new Color(0x1976D2));         // blue
        UIUtil.styleSmallButton(savePrintButton, new Color(0x2E7D32));   // green
        UIUtil.styleSmallButton(updatePrintButton, new Color(0xEF6C00)); // orange
        UIUtil.styleSmallButton(deleteButton, new Color(0xC62828));      // red
        // White line-art icons on the coloured buttons, per the reference.
        newButton.setIcon(ButtonIcons.of("Plus", Color.WHITE));
        savePrintButton.setIcon(ButtonIcons.of("Printer", Color.WHITE));
        updatePrintButton.setIcon(ButtonIcons.of("Printer", Color.WHITE));
        deleteButton.setIcon(ButtonIcons.of("Trash", Color.WHITE));
        newButton.addActionListener(e -> resetForm());
        savePrintButton.addActionListener(e -> saveAndPrint());
        updatePrintButton.addActionListener(e -> updateAndPrint());
        deleteButton.addActionListener(e -> deleteSelected());
    }

    /** Rebuilds the visible button set for the current mode. */
    private void rebuildButtons() {
        buttonPanel.removeAll();
        if (historyMode) {
            // History mode: update the payment and print, or delete it.
            buttonPanel.add(updatePrintButton);
            buttonPanel.add(deleteButton);
        } else {
            buttonPanel.add(newButton);
            buttonPanel.add(savePrintButton);
        }
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    /**
     * Switches the panel between "new" (entry form) and "history" (records).
     * The visible action buttons follow the mode; the visible texts are
     * translated in place by {@link dairy.erp.util.I18n#apply}.
     */
    public void setMode(String mode) {
        historyMode = "history".equals(mode);
        // The form heading follows the selected menu item — "Payment" on the
        // history screen, "New Payment Entry" on the entry screen.
        entryHeading.setText(historyMode ? "Payment" : "New Payment Entry");
        rebuildButtons();
    }

    /**
     * History-mode Update: loads the selected payment into the form the first
     * time, then commits the edited values through the normal update path.
     */
    private boolean updateSelected() {
        if (editingId == -1) {
            if (table.getSelectedRow() < 0) {
                UIUtil.showMessage(this,
                        "Select a payment from the history table first.",
                        "Update & Print", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            loadSelected();
            return false; // record loaded — edit the fields, then press Update & Print again to save
        }
        return saveRecord();
    }

    /**
     * History-mode combined action: commits the edited payment and, only when
     * the update succeeds, prints the payment report — one click for both
     * operations.
     */
    private void updateAndPrint() {
        if (updateSelected()) {
            printList();
        }
    }

    /**
     * Entry-mode combined action: saves the payment and, only when the save
     * succeeds, prints the payment report — one click for both operations.
     */
    private void saveAndPrint() {
        if (saveRecord()) {
            printList();
        }
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
        g.gridx = 0;
        g.gridy = row;
        g.gridwidth = 1;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        form.add(styledLabel(labelText), g);
        g.gridx = 1;
        g.gridwidth = 1;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        form.add(comp, g);
    }

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(LABEL_FONT);
        return l;
    }

    private JPanel buildTable() {
        JPanel area = new JPanel(new BorderLayout());
        JScrollPane scroll = new JScrollPane(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Teal-headed striped grid — the shared Customer Details table style.
        UIUtil.styleCustomerDetailsTable(table);
        // Reference design highlights the selected row in light blue.
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable source, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(
                        source, value, isSelected, hasFocus, row, column);
                if (isSelected) {
                    c.setBackground(hasFocus ? UIUtil.tableFocusedRowColor() : UIUtil.tableSelectedRowColor());
                    c.setForeground(UIUtil.tableSelectedTextColor());
                } else {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : UIUtil.tableOddRowColor());
                    c.setForeground(source.getForeground());
                }
                setBorder(hasFocus
                        ? BorderFactory.createLineBorder(UIUtil.BRAND, 2, true)
                        : BorderFactory.createEmptyBorder(0, 6, 0, 6));
                return c;
            }
        });
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
            UIUtil.showMessage(this, "Customer not found: " + code,
                    "Customer", JOptionPane.WARNING_MESSAGE);
            selectedCustomer = null;
            customerNameField.setText("");
            return;
        }
        selectedCustomer = c;
        customerNameField.setText(c.getCustomerName());
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

    private boolean saveRecord() {
        String error = validateForm();
        if (!error.isEmpty()) {
            UIUtil.showMessage(this, error, "Validation", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        Payment p = new Payment();
        p.setCustomerId(selectedCustomer.getId());
        p.setPaymentDate(datePicker.getDate());
        p.setAmount(ValidationUtil.parseDecimal(amountField.getText()));
        p.setPaymentMode((String) modeBox.getSelectedItem());
        p.setReference(referenceField.getText());
        p.setRemarks(remarksArea.getText());
        try {
            if (editingId > 0) {
                p.setId(editingId);
                paymentService.update(p);
            } else {
                paymentService.add(p);
            }
            UIUtil.showMessage(this, "Payment saved successfully.", "Save",
                    JOptionPane.INFORMATION_MESSAGE);
            loadTable(paymentService.search(0, null, null));
            resetForm();
            return true;
        } catch (RuntimeException ex) {
            UIUtil.showMessage(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void deleteRecord() {
        if (editingId <= 0) {
            UIUtil.showMessage(this, "Select a payment (Update) to delete first.",
                    "Delete", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = UIUtil.confirm(this,
                "Are you sure you want to delete this payment?", "Delete Payment");
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            paymentService.delete(editingId);
            loadTable(paymentService.search(0, null, null));
            resetForm();
        } catch (RuntimeException ex) {
            UIUtil.showMessage(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtil.showMessage(this, "Select a payment row to edit.", "Edit",
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
                datePicker.setDate(p.getPaymentDate());
                amountField.setText(p.getAmount().toPlainString());
                modeBox.setSelectedItem(p.getPaymentMode());
                referenceField.setText(p.getReference());
                remarksArea.setText(p.getRemarks());
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
        remarksArea.setText("");
        datePicker.setDate(LocalDate.now());
        modeBox.setSelectedIndex(0);
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

    /**
     * Text field that paints a grey hint text while it is empty and not
     * focused — the document itself stays empty, so lookup, validation and
     * save logic are completely unaffected by the placeholder.
     */
    private static class GhostField extends JTextField {
        private final String ghost;

        GhostField(String ghost, int columns) {
            super(columns);
            this.ghost = ghost;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GHOST_GREY);
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(ghost, getInsets().left + 2, y);
                g2.dispose();
            }
        }
    }
}








