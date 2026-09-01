package dairy.erp.ui;

import dairy.erp.config.AppConfig;
import dairy.erp.model.Customer;
import dairy.erp.model.LedgerEntry;
import dairy.erp.service.CustomerService;
import dairy.erp.service.ReportService;
import dairy.erp.util.AppBus;
import dairy.erp.util.ButtonIcons;
import dairy.erp.util.CSVUtil;
import dairy.erp.util.CurrencyUtil;
import dairy.erp.util.DateUtil;
import dairy.erp.util.PrintUtil;
import dairy.erp.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer ledger / statement screen matching the "Customer Ledger" design:
 * a customer dropdown, date range, two summary cards, export + print actions,
 * a transaction grid and a bold TOTAL row.
 */
public class CustomerLedgerPanel extends JPanel {

    private final ReportService reportService = new ReportService();
    private final CustomerService customerService = new CustomerService();

    private final JComboBox<Customer> customerCombo = new JComboBox<>();
    private final DatePicker fromPicker = new DatePicker();
    private final DatePicker toPicker = new DatePicker();

    private final JLabel codeValue = new JLabel("--");
    private final JLabel nameValue = new JLabel("--");
    private final JLabel mobileValue = new JLabel("--");
    private final JLabel balanceValue = new JLabel(CurrencyUtil.formatMoney(BigDecimal.ZERO));
    private final JLabel statusValue = new JLabel("--");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Date", "Type", "Description", "Qty", "Amount", "Payment", "Balance"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    private Customer selectedCustomer;

    public CustomerLedgerPanel(String username) {
        super(new BorderLayout(4, 8));
        setBorder(BorderFactory.createEmptyBorder(6, 10, 8, 10));

        fromPicker.setDate(LocalDate.now().withDayOfMonth(1));
        toPicker.setDate(LocalDate.now());

        // Populate the customer dropdown (CODE - NAME).
        List<Customer> customers;
        try {
            customers = customerService.listAll();
        } catch (RuntimeException e) {
            customers = new ArrayList<>();
        }
        for (Customer c : customers) {
            customerCombo.addItem(c);
        }
        customerCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                Customer c = (Customer) value;
                setText(c == null ? "" : c.getCustomerCode() + " - " + c.getCustomerName());
                return this;
            }
        });
        if (customers.size() > 0) {
            customerCombo.setSelectedIndex(0);
        }
        customerCombo.addActionListener(e -> loadCustomer());

        // Auto-refresh the grid whenever a date is picked.
        wireDateRefresh(fromPicker);
        wireDateRefresh(toPicker);

        // Narrow the date fields so only the date itself is visible.
        fromPicker.getTextField().setColumns(8);
        toPicker.getTextField().setColumns(8);

        // Live propagation: reload the customer combo whenever a customer is
        // added/updated/deleted on the Customer screen, keeping the selection.
        AppBus.onCustomersChanged(v -> reloadCustomers());

        add(buildTop(username), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        refresh();
    }

    /** Reloads the customer dropdown after add/update/delete, preserving selection. */
    private void reloadCustomers() {
        Customer sel = (Customer) customerCombo.getSelectedItem();
        int selId = sel == null ? -1 : sel.getId();
        customerCombo.removeAllItems();
        List<Customer> customers;
        try {
            customers = customerService.listAll();
        } catch (RuntimeException e) {
            customers = new ArrayList<>();
        }
        int restore = -1;
        for (Customer c : customers) {
            customerCombo.addItem(c);
            if (c.getId() == selId) {
                restore = customerCombo.getItemCount() - 1;
            }
        }
        if (restore >= 0) {
            customerCombo.setSelectedIndex(restore);
        } else if (customers.size() > 0) {
            customerCombo.setSelectedIndex(0);
        } else {
            selectedCustomer = null;
            refresh();
        }
    }

    private void wireDateRefresh(DatePicker picker) {
        DocumentListener dl = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refresh();
            }
        };
        picker.getTextField().getDocument().addDocumentListener(dl);
    }
/** Header row + screen heading + customer/date filter line. */
    private JPanel buildTop(String username) {
        JPanel top = new JPanel(new BorderLayout(0, 6));

        // Row 1: logo on the left, [ User ] [ Date ] on the right.
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.add(UIUtil.header("Customer Ledger"), BorderLayout.WEST);
        JPanel info = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 6));
        JLabel userLbl = new JLabel("User: " + username);
        JLabel dateLbl = new JLabel("Date: " + DateUtil.toDisplay(DateUtil.today()));
        userLbl.setFont(userLbl.getFont().deriveFont(Font.BOLD, 15f));
        dateLbl.setFont(dateLbl.getFont().deriveFont(Font.BOLD, 15f));
        userLbl.setForeground(UIUtil.BRAND);
        dateLbl.setForeground(UIUtil.BRAND);
        info.add(userLbl);
        info.add(dateLbl);
        headerRow.add(info, BorderLayout.EAST);
        top.add(headerRow, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        // Row 2: screen title.
        g.gridy = 0;
        g.gridx = 0;
        g.gridwidth = GridBagConstraints.REMAINDER;
        JLabel title = new JLabel("CUSTOMER LEDGER");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        title.setForeground(UIUtil.BRAND);
        center.add(title, g);

        // Row 3: customer dropdown + date range + Search button.
        g.gridy = 1;
        g.gridwidth = 1;
        g.gridx = 0;
        center.add(new JLabel("Customer:"), g);
        g.gridx = 1;
        customerCombo.setPreferredSize(new Dimension(320, 34));
        center.add(customerCombo, g);
        g.gridx = 2;
        center.add(new JLabel("Date Range:"), g);
        g.gridx = 3;
        center.add(fromPicker, g);
        g.gridx = 4;
        center.add(new JLabel("to"), g);
        g.gridx = 5;
        center.add(toPicker, g);
        g.gridx = 6;
        JButton searchBtn = new JButton("Search", ButtonIcons.of("Search", Color.WHITE));
        UIUtil.styleSmallButton(searchBtn, new Color(0x15, 0x65, 0xC0));
        searchBtn.addActionListener(e -> refresh());
        center.add(searchBtn, g);
        top.add(center, BorderLayout.CENTER);
        return top;
    }

    /** Two summary cards on the left; Export / Print actions on the right. */
    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 8));

        JPanel cardsRow = new JPanel(new BorderLayout());
        JPanel cards = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        cards.add(infoCard("Customer Info",
                new String[]{"Customer Code", "Name", "Mobile"},
                new JLabel[]{codeValue, nameValue, mobileValue}));
        cards.add(infoCard("Account Summary",
                new String[]{"Net Balance", "Status"},
                new JLabel[]{balanceValue, statusValue}));
        cardsRow.add(cards, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));

        // Export button with download icon that opens a dropdown menu.
        JButton exportBtn = new JButton("Export \u25BE", ButtonIcons.of("Download", Color.WHITE));
        UIUtil.styleSmallButton(exportBtn, new Color(0x15, 0x65, 0xC0));
        JPopupMenu exportMenu = new JPopupMenu();
        JMenuItem pdfItem = new JMenuItem("Export PDF", ButtonIcons.of("Download", UIUtil.BRAND));
        JMenuItem excelItem = new JMenuItem("Export Excel (CSV)", ButtonIcons.of("Download", UIUtil.BRAND));
        pdfItem.addActionListener(e -> exportPdf());
        excelItem.addActionListener(e -> exportExcel());
        exportMenu.add(pdfItem);
        exportMenu.add(excelItem);
        exportBtn.addActionListener(e ->
                exportMenu.show(exportBtn, 0, exportBtn.getHeight()));
        actions.add(exportBtn);

        // Print button with printer icon.
        JButton printBtn = new JButton("Print", ButtonIcons.of("Printer", Color.WHITE));
        UIUtil.styleSmallButton(printBtn, UIUtil.BRAND);
        printBtn.addActionListener(e -> printLedger());
        actions.add(printBtn);

        cardsRow.add(actions, BorderLayout.EAST);
        body.add(cardsRow, BorderLayout.NORTH);
        body.add(buildTableArea(), BorderLayout.CENTER);
        return body;
    }

    /**
     * White rounded summary card like the reference design: a bold title on
     * top followed by label / value rows (Customer Info, Account Summary).
     */
    private JPanel infoCard(String title, String[] labels, JLabel[] values) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(UIUtil.cardBackground());
        p.setBorder(UIUtil.cardBorder());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(0, 0, 6, 0);
        JLabel t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 17f));
        p.add(t, g);
        g.gridwidth = 1;
        for (int i = 0; i < labels.length; i++) {
            g.gridy = i + 1;
            g.gridx = 0;
            g.insets = new Insets(3, 0, 3, 0);
            JLabel l = new JLabel(labels[i] + ":");
            l.setFont(l.getFont().deriveFont(Font.BOLD, 15f));
            p.add(l, g);
            g.gridx = 1;
            g.insets = new Insets(3, 18, 3, 0);
            values[i].setFont(values[i].getFont().deriveFont(Font.PLAIN, 15f));
            p.add(values[i], g);
        }
        return p;
    }


    private JPanel buildTableArea() {
        JPanel area = new JPanel(new BorderLayout());
        UIUtil.styleCustomerDetailsTable(table);
        table.setRowHeight(34);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        // Column widths for a report-like layout.
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(260);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(100);
        table.getColumnModel().getColumn(6).setPreferredWidth(100);

        // Renderer: zebra rows + right-aligned numbers + bold TOTAL row.
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private final Color totalsBg = new Color(0xff, 0xf3, 0xd6);

            @Override
            public Component getTableCellRendererComponent(JTable source, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(
                        source, value, isSelected, hasFocus, row, column);
                JLabel l = (JLabel) c;
                l.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                boolean totalsRow = "TOTALS".equals(tableModel.getValueAt(row, 0));
                if (totalsRow) {
                    l.setFont(l.getFont().deriveFont(Font.BOLD, 15f));
                    l.setBackground(totalsBg);
                    l.setForeground(new Color(0x8A, 0x5A, 0x00));
                    l.setHorizontalAlignment(column >= 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                } else {
                    if (isSelected) {
                        l.setBackground(hasFocus ? UIUtil.tableFocusedRowColor() : UIUtil.tableSelectedRowColor());
                        l.setForeground(UIUtil.tableSelectedTextColor());
                    } else {
                        l.setBackground(row % 2 == 0 ? Color.WHITE : UIUtil.tableOddRowColor());
                    }
                    l.setForeground(source.getForeground());
                    l.setBorder(hasFocus
                            ? BorderFactory.createLineBorder(UIUtil.BRAND, 2, true)
                            : BorderFactory.createEmptyBorder(0, 6, 0, 6));
                    if (column == 0 || column >= 3) {
                        l.setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.RIGHT);
                    } else {
                        l.setHorizontalAlignment(SwingConstants.CENTER);
                    }
                }
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        area.add(scroll, BorderLayout.CENTER);
        return area;
    }

    private void loadCustomer() {
        selectedCustomer = (Customer) customerCombo.getSelectedItem();
        refresh();
    }

    private void refresh() {
        tableModel.setRowCount(0);
        if (selectedCustomer == null) {
            codeValue.setText("--");
            nameValue.setText("--");
            mobileValue.setText("--");
            balanceValue.setText(CurrencyUtil.formatMoney(BigDecimal.ZERO));
            balanceValue.setForeground(Color.BLACK);
            statusValue.setText("--");
            statusValue.setForeground(Color.BLACK);
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

        codeValue.setText(selectedCustomer.getCustomerCode());
        nameValue.setText(selectedCustomer.getCustomerName());
        String mobile = selectedCustomer.getMobile();
        mobileValue.setText(mobile == null || mobile.isBlank() ? "--" : mobile);

        String status = selectedCustomer.getStatus() == null ? "Active" : selectedCustomer.getStatus();
        statusValue.setText(status);
        statusValue.setForeground("Inactive".equalsIgnoreCase(status)
                ? new Color(0xC6, 0x28, 0x28) : UIUtil.SUCCESS_GREEN);

        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmt = BigDecimal.ZERO;
        BigDecimal totalPay = BigDecimal.ZERO;
        for (LedgerEntry e : entries) {
            tableModel.addRow(new Object[]{
                    DateUtil.toDisplay(e.getDate()),
                    e.getTransactionType(),
                    e.getDescription(),
                    CurrencyUtil.format(e.getQuantity()),
                    "PAYMENT".equals(e.getTransactionType()) ? "" : CurrencyUtil.formatMoney(e.getAmount()),
                    "PAYMENT".equals(e.getTransactionType()) ? CurrencyUtil.formatMoney(e.getPayment()) : "",
                    CurrencyUtil.formatMoney(e.getBalance())
            });
            totalQty = totalQty.add(e.getQuantity() == null ? BigDecimal.ZERO : e.getQuantity());
            totalAmt = totalAmt.add(e.getAmount() == null ? BigDecimal.ZERO : e.getAmount());
            totalPay = totalPay.add(e.getPayment() == null ? BigDecimal.ZERO : e.getPayment());
        }

        BigDecimal closing = entries.isEmpty()
                ? (selectedCustomer.getOpeningBalance() == null ? BigDecimal.ZERO : selectedCustomer.getOpeningBalance())
                : entries.get(entries.size() - 1).getBalance();
        balanceValue.setText(CurrencyUtil.formatMoney(closing));
        balanceValue.setForeground(closing.signum() < 0 ? new Color(0xC6, 0x28, 0x28) : UIUtil.SUCCESS_GREEN);

        if (!entries.isEmpty()) {
            tableModel.addRow(new Object[]{"TOTALS", "", "",
                    CurrencyUtil.format(totalQty),
                    CurrencyUtil.formatMoney(totalAmt),
                    CurrencyUtil.formatMoney(totalPay),
                    CurrencyUtil.formatMoney(closing)});
        }
    }

    /** "Export Excel" - writes a CSV file so it opens directly in Excel. */
    private void exportExcel() {
        if (selectedCustomer == null) {
            UIUtil.showMessage(this, "Select a customer first.",
                    "Export", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser(AppConfig.getExportDir().toFile());
        chooser.setDialogTitle("Export Customer Ledger to Excel (CSV)");
        chooser.setSelectedFile(new File(AppConfig.getExportDir().toFile(),
                selectedCustomer.getCustomerCode() + "_Ledger_" + LocalDate.now() + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Path path = chooser.getSelectedFile().toPath();
            List<List<String>> rows = new ArrayList<>();
            rows.add(sl("Date", "Type", "Description", "Qty", "Amount", "Payment", "Balance"));
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                rows.add(sl(
                        String.valueOf(tableModel.getValueAt(i, 0)),
                        String.valueOf(tableModel.getValueAt(i, 1)),
                        String.valueOf(tableModel.getValueAt(i, 2)),
                        String.valueOf(tableModel.getValueAt(i, 3)),
                        String.valueOf(tableModel.getValueAt(i, 4)),
                        String.valueOf(tableModel.getValueAt(i, 5)),
                        String.valueOf(tableModel.getValueAt(i, 6))));
            }
            CSVUtil.write(path, rows);
            UIUtil.showMessage(this, "Exported to " + path,
                    "Export", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            UIUtil.showMessage(this, "Export failed: " + ex.getMessage(),
                    "Export", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** "Export PDF" - opens the print dialog; use "Save as PDF" there. */
    private void exportPdf() {
        if (selectedCustomer == null) {
            UIUtil.showMessage(this, "Select a customer first.",
                    "Export", JOptionPane.WARNING_MESSAGE);
            return;
        }
        printLedger();
    }

    private void printLedger() {
        if (selectedCustomer == null) {
            UIUtil.showMessage(this, "Select a customer first.",
                    "Print", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<String> lines = new ArrayList<>();
        lines.add("Customer Ledger - " + selectedCustomer.getCustomerCode() + " - " + selectedCustomer.getCustomerName());
        lines.add("=".repeat(80));
        lines.add("Customer: " + selectedCustomer.getCustomerCode() + " - " + selectedCustomer.getCustomerName());
        lines.add("Period   : " + DateUtil.toDisplay(fromPicker.getDate()) + " to "
                + DateUtil.toDisplay(toPicker.getDate()));
        lines.add(String.format("%-10s %-7s %-24s %-7s %-10s %-10s %s",
                "Date", "Type", "Description", "Qty", "Amount", "Payment", "Balance"));
        lines.add("-".repeat(80));
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            lines.add(String.format("%-10s %-7s %-24s %-7s %-10s %-10s %s",
                    String.valueOf(tableModel.getValueAt(i, 0)),
                    String.valueOf(tableModel.getValueAt(i, 1)),
                    truncate(String.valueOf(tableModel.getValueAt(i, 2)), 24),
                    String.valueOf(tableModel.getValueAt(i, 3)),
                    String.valueOf(tableModel.getValueAt(i, 4)),
                    String.valueOf(tableModel.getValueAt(i, 5)),
                    String.valueOf(tableModel.getValueAt(i, 6))));
        }
        lines.add("-".repeat(80));
        lines.add("Net Balance: " + balanceValue.getText());
        PrintUtil.printText(findOwner(), "Customer Ledger", lines, true);
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    private static List<String> sl(String... values) {
        List<String> list = new ArrayList<>();
        for (String v : values) {
            list.add(v == null ? "" : v);
        }
        return list;
    }

    private java.awt.Frame findOwner() {
        java.awt.Container c = this;
        while (c != null && !(c instanceof javax.swing.JFrame)) {
            c = c.getParent();
        }
        return (java.awt.Frame) c;
    }
}