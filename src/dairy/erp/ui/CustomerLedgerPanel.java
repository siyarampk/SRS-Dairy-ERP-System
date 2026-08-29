package dairy.erp.ui;

import dairy.erp.model.Customer;
import dairy.erp.model.LedgerEntry;
import dairy.erp.service.CustomerService;
import dairy.erp.service.ReportService;
import dairy.erp.util.CurrencyUtil;
import dairy.erp.util.DateUtil;
import dairy.erp.util.PrintUtil;
import dairy.erp.util.UIUtil;
import dairy.erp.util.ValidationUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer ledger: chronological milk credits and payments with a running
 * balance, opening and closing balance, plus print support.
 */
public class CustomerLedgerPanel extends JPanel {

    private final ReportService reportService = new ReportService();
    private final CustomerService customerService = new CustomerService();

    private final JTextField customerCodeField = new JTextField(10);
    private final JTextField customerNameField = new JTextField(20);
    private final DatePicker fromPicker = new DatePicker();
    private final DatePicker toPicker = new DatePicker();
    private final JLabel openingLabel = new JLabel("0.00");
    private final JLabel closingLabel = new JLabel("0.00");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Date", "Type", "Description", "Qty", "Amount", "Payment", "Balance"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    private Customer selectedCustomer;

    public CustomerLedgerPanel() {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        fromPicker.setDate(LocalDate.now().withDayOfMonth(1));
        toPicker.setDate(LocalDate.now());
        customerNameField.setEditable(false);
        // Read-only name field: small, disabled, cream-coloured.
        customerNameField.setColumns(14);
        customerNameField.setBackground(new Color(0xFF, 0xF8, 0xDC));
        customerNameField.setDisabledTextColor(new Color(0x33, 0x33, 0x33));
        UIUtil.makeUpperCase(customerCodeField);
        customerCodeField.addActionListener(e -> loadCustomer());
        add(buildTop(), BorderLayout.NORTH);
        add(buildTableArea(), BorderLayout.CENTER);
    }

    private JPanel buildTop() {
        JPanel header = UIUtil.header("Customer Ledger / Statement");

        JPanel top = new JPanel(new GridBagLayout());
        top.setBorder(BorderFactory.createTitledBorder("Customer Ledger"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;

        // All inputs on a single line: Code, Name, From, To + action buttons.
        g.gridy = 0;
        g.gridx = 0; top.add(new JLabel("Customer Code:"), g);
        g.gridx = 1; top.add(customerCodeField, g);
        g.gridx = 2; top.add(new JLabel("Name:"), g);
        g.gridx = 3; top.add(customerNameField, g);
        g.gridx = 4; top.add(new JLabel("From:"), g);
        g.gridx = 5; top.add(fromPicker, g);
        g.gridx = 6; top.add(new JLabel("To:"), g);
        g.gridx = 7; top.add(toPicker, g);
        JButton show = new JButton("Show");
        UIUtil.styleSmallButton(show, new Color(0x2E7D32)); // green, app standard
        show.addActionListener(e -> refresh());
        g.gridx = 8; top.add(show, g);
        JButton print = new JButton("Print");
        UIUtil.styleSmallButton(print, new Color(0x607D8B)); // grey, app standard
        print.addActionListener(e -> printLedger());
        g.gridx = 9; top.add(print, g);

        JPanel wrap = new JPanel(new BorderLayout(0, 4));
        wrap.add(header, BorderLayout.NORTH);
        wrap.add(top, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildTableArea() {
        JPanel area = new JPanel(new BorderLayout());
        // Same grid look as the Customer Details table: fonts, colours,
        // row height, zebra striping.
        UIUtil.styleCustomerDetailsTable(table);
        JScrollPane scroll = new JScrollPane(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        area.add(scroll, BorderLayout.CENTER);
        return area;
    }

    private void loadCustomer() {
        String code = customerCodeField.getText().trim();
        if (ValidationUtil.isBlank(code)) {
            return;
        }
        selectedCustomer = customerService.findByCode(code);
        if (selectedCustomer == null) {
            JOptionPane.showMessageDialog(this, "Customer not found: " + code,
                    "Customer", JOptionPane.WARNING_MESSAGE);
            customerNameField.setText("");
            return;
        }
        customerNameField.setText(selectedCustomer.getCustomerName());
        refresh();
    }

    private void refresh() {
        tableModel.setRowCount(0);
        if (selectedCustomer == null) {
            openingLabel.setText("0.00");
            closingLabel.setText("0.00");
            return;
        }
        LocalDate from = fromPicker.getDate();
        LocalDate to = toPicker.getDate();
        if (from == null) {
            from = LocalDate.now().withDayOfMonth(1);
        }
        if (to == null) {
            to = LocalDate.now();
        }
        List<LedgerEntry> entries = reportService.customerLedger(selectedCustomer.getId(), from, to);
        BigDecimal opening = selectedCustomer.getOpeningBalance() == null
                ? BigDecimal.ZERO : selectedCustomer.getOpeningBalance();
        for (LedgerEntry e : entries) {
            tableModel.addRow(new Object[]{
                    DateUtil.toDisplay(e.getDate()), e.getTransactionType(), e.getDescription(),
                    CurrencyUtil.format(e.getQuantity()),
                    "PAYMENT".equals(e.getTransactionType()) ? "" : CurrencyUtil.formatMoney(e.getAmount()),
                    "PAYMENT".equals(e.getTransactionType()) ? CurrencyUtil.formatMoney(e.getPayment()) : "",
                    CurrencyUtil.formatMoney(e.getBalance())
            });
        }
        BigDecimal closing = entries.isEmpty() ? opening : entries.get(entries.size() - 1).getBalance();
        openingLabel.setText(CurrencyUtil.formatMoney(opening));
        closingLabel.setText(CurrencyUtil.formatMoney(closing));
    }

    private void printLedger() {
        if (selectedCustomer == null) {
            return;
        }
        List<String> lines = new ArrayList<>();
        lines.add("Customer Ledger");
        lines.add("=".repeat(80));
        lines.add("Customer: " + selectedCustomer.getCustomerCode() + " - " + selectedCustomer.getCustomerName());
        lines.add("Period   : " + DateUtil.toDisplay(fromPicker.getDate()) + " to "
                + DateUtil.toDisplay(toPicker.getDate()));
        lines.add(String.format("%-10s %-9s %-24s %-8s %-10s %-10s %s",
                "Date", "Type", "Description", "Qty", "Amount", "Payment", "Balance"));
        lines.add("-".repeat(80));
        LocalDate from = fromPicker.getDate();
        LocalDate to = toPicker.getDate();
        for (LedgerEntry e : reportService.customerLedger(selectedCustomer.getId(), from, to)) {
            if (from == null) from = LocalDate.now().withDayOfMonth(1);
            lines.add(String.format("%-10s %-9s %-24s %-8s %-10s %-10s %s",
                    DateUtil.toDisplay(e.getDate()), e.getTransactionType(),
                    truncate(e.getDescription(), 24), CurrencyUtil.format(e.getQuantity()),
                    "PAYMENT".equals(e.getTransactionType()) ? "" : CurrencyUtil.formatMoney(e.getAmount()),
                    "PAYMENT".equals(e.getTransactionType()) ? CurrencyUtil.formatMoney(e.getPayment()) : "",
                    CurrencyUtil.formatMoney(e.getBalance())));
        }
        lines.add("-".repeat(80));
        lines.add("Closing Balance: " + closingLabel.getText());
        PrintUtil.printText(findOwner(), "Customer Ledger", lines, true);
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    private java.awt.Frame findOwner() {
        java.awt.Container c = this;
        while (c != null && !(c instanceof JFrame)) {
            c = c.getParent();
        }
        return (java.awt.Frame) c;
    }
}

