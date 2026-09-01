package dairy.erp.ui;

import dairy.erp.config.AppConfig;
import dairy.erp.model.Customer;
import dairy.erp.model.CustomerSummaryRow;
import dairy.erp.model.LedgerEntry;
import dairy.erp.model.MilkCollection;
import dairy.erp.model.Payment;
import dairy.erp.model.ReportSummary;
import dairy.erp.service.CustomerService;
import dairy.erp.service.ReportService;
import dairy.erp.service.SettingsService;
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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reports centre: Daily, Weekly, Monthly, Customer report, Customer statement
 * and Payment report with a clean filter panel (report type, date range,
 * customer dropdown, milk type and shift), a summary and a transaction grid
 * styled exactly like the Customer Details grid.
 */
public class ReportsPanel extends JPanel {

    private final ReportService reportService = new ReportService();
    private final CustomerService customerService = new CustomerService();
    private final SettingsService settingsService = new SettingsService();

    private final JComboBox<String> reportTypeBox = new JComboBox<>(new String[]{
            "Daily Report", "Weekly Report", "Monthly Report",
            "Customer Report", "Customer Statement", "Payment Report"
    });
    private final DatePicker fromPicker = new DatePicker();
    private final DatePicker toPicker = new DatePicker();
    private final JComboBox<Customer> customerCombo = new JComboBox<>();
    private final JComboBox<String> milkBox = new JComboBox<>(new String[]{"All", "Cow", "Buffalo", "Mix"});
    private final JComboBox<String> shiftBox = new JComboBox<>(new String[]{"All", "Morning", "Evening"});

    private final JTextArea summaryArea = new JTextArea(5, 70);
    private final DefaultTableModel tableModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private String generatedTitle = "";

    private Customer selectedCustomer;

    public ReportsPanel(String username) {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createEmptyBorder(6, 10, 8, 10));
        fromPicker.setDate(LocalDate.now().withDayOfMonth(1));
        toPicker.setDate(LocalDate.now());
        // Narrow the date fields so only the date itself is visible.
        fromPicker.getTextField().setColumns(8);
        toPicker.getTextField().setColumns(8);

        loadCustomers();
        // Live propagation: reload the customer combo whenever a customer is
        // added/updated/deleted on the Customer screen, keeping the selection.
        AppBus.onCustomersChanged(v -> reloadCustomers());
        add(buildFilters(username), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    /** Fills the customer dropdown with an "All Customers" entry + every customer. */
    private void loadCustomers() {
        customerCombo.addItem(null); // "All Customers"
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
                if (value == null) {
                    setText("All Customers");
                } else {
                    Customer c = (Customer) value;
                    setText(c.getCustomerCode() + " - " + c.getCustomerName());
                }
                return this;
            }
        });
        customerCombo.addActionListener(e -> selectedCustomer = (Customer) customerCombo.getSelectedItem());
    }

    /** Reloads the customer dropdown after add/update/delete, preserving selection. */
    private void reloadCustomers() {
        Customer sel = (Customer) customerCombo.getSelectedItem();
        int selId = sel == null ? -1 : sel.getId();
        customerCombo.removeAllItems();
        customerCombo.addItem(null); // "All Customers"
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
        } else {
            customerCombo.setSelectedIndex(0); // back to "All Customers"
        }
    }

/** Header row (logo + user/date) and a two-row filter panel. */
    private JPanel buildFilters(String username) {
        JPanel top = new JPanel(new BorderLayout(0, 6));

        // Row 1: logo on the left, [ User ] [ Date ] on the right.
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.add(UIUtil.header("Reports"), BorderLayout.WEST);
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

        // Section label above the cream filter card (like the reference image).
        JLabel section = new JLabel("Report Filters");
        section.setFont(section.getFont().deriveFont(Font.BOLD, 15f));
        JPanel filterWrap = new JPanel(new BorderLayout(0, 2));
        filterWrap.setOpaque(false);
        filterWrap.add(section, BorderLayout.NORTH);

        // Cream -> generic white card, same as the Customer Details cards.
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(UIUtil.cardBackground());
        card.setBorder(UIUtil.cardBorder());

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 8, 4, 8);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.NONE;

        g.gridy = 0;
        g.gridx = 0;
        g.gridwidth = 6;
        JLabel title = new JLabel("FILTER & GENERATE");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        title.setForeground(UIUtil.BRAND);
        fields.add(title, g);

        g.gridwidth = 1;
        g.gridy = 1;
        g.gridx = 0;
        fields.add(new JLabel("Report:"), g);
        g.gridx = 1;
        reportTypeBox.setPreferredSize(new Dimension(190, 32));
        fields.add(reportTypeBox, g);
        g.gridx = 2;
        fields.add(new JLabel("From:"), g);
        g.gridx = 3;
        fields.add(fromPicker, g);
        g.gridx = 4;
        fields.add(new JLabel("To:"), g);
        g.gridx = 5;
        fields.add(toPicker, g);

        g.gridy = 2;
        g.gridx = 0;
        fields.add(new JLabel("Customer:"), g);
        g.gridx = 1;
        customerCombo.setPreferredSize(new Dimension(190, 32));
        fields.add(customerCombo, g);
        g.gridx = 2;
        fields.add(new JLabel("Milk:"), g);
        g.gridx = 3;
        milkBox.setPreferredSize(new Dimension(110, 32));
        fields.add(milkBox, g);
        g.gridx = 4;
        fields.add(new JLabel("Shift:"), g);
        g.gridx = 5;
        shiftBox.setPreferredSize(new Dimension(110, 32));
        fields.add(shiftBox, g);
        card.add(fields, BorderLayout.CENTER);

        // Action buttons: same icon-left style as the other screens, pinned to
        // the top-right corner of the filter card with breathing room above
        // so they stay fully visible next to the two filter rows.
        JPanel actionsWrap = new JPanel(new BorderLayout());
        actionsWrap.setOpaque(false);
        actionsWrap.setBorder(BorderFactory.createEmptyBorder(12, 10, 0, 4)); // top padding
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        JButton generate = new JButton("Generate Report", ButtonIcons.of("Document", Color.WHITE));
        UIUtil.styleSmallButton(generate, new Color(0x2E, 0x7D, 0x32)); // green
        generate.addActionListener(e -> generate());
        actions.add(generate);
        JButton print = new JButton("Print Report", ButtonIcons.of("Printer", Color.WHITE));
        UIUtil.styleSmallButton(print, new Color(0x60, 0x7D, 0x8B)); // slate
        print.addActionListener(e -> printReport());
        actions.add(print);
        JButton export = new JButton("Export to CSV", ButtonIcons.of("Download", Color.WHITE));
        UIUtil.styleSmallButton(export, new Color(0x19, 0x76, 0xD2)); // blue
        export.addActionListener(e -> exportCsv());
        actions.add(export);
        actionsWrap.add(actions, BorderLayout.NORTH);
        card.add(actionsWrap, BorderLayout.EAST);

        filterWrap.add(card, BorderLayout.CENTER);
        top.add(filterWrap, BorderLayout.CENTER);
        return top;
    }
    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(8, 8));
        // Same grid look as the Customer Details table: fonts, colours,
        // row height, zebra striping.
        UIUtil.styleCustomerDetailsTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(32);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        // Right-align numeric columns for a report-style grid.
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable source, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(
                        source, value, isSelected, hasFocus, row, column);
                JLabel l = (JLabel) c;
                l.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                String header = tableModel.getColumnName(column);
                boolean numeric = isNumericColumn(header);
                l.setHorizontalAlignment(numeric ? SwingConstants.RIGHT
                        : (column == 0 || column == 2 ? SwingConstants.LEFT : SwingConstants.CENTER));
                if (isSelected) {
                    l.setBackground(hasFocus ? UIUtil.tableFocusedRowColor() : UIUtil.tableSelectedRowColor());
                    l.setForeground(UIUtil.tableSelectedTextColor());
                } else {
                    l.setBackground(row % 2 == 0 ? Color.WHITE : UIUtil.tableOddRowColor());
                    l.setForeground(source.getForeground());
                }
                l.setBorder(hasFocus
                        ? BorderFactory.createLineBorder(UIUtil.BRAND, 2, true)
                        : BorderFactory.createEmptyBorder(0, 6, 0, 6));
                return c;
            }
        });

        // REPORT VIEW section: bold heading + rounded panel around the grid.
        JPanel viewSection = new JPanel(new BorderLayout(0, 4));
        viewSection.setOpaque(false);
        JLabel viewTitle = new JLabel("REPORT VIEW");
        viewTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        viewTitle.setForeground(UIUtil.BRAND);
        viewSection.add(viewTitle, BorderLayout.NORTH);
        JPanel tableWrap = new JPanel(new BorderLayout());
        tableWrap.setBackground(Color.WHITE);
        tableWrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xC9, 0xD3, 0xDA), 1, true),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        tableWrap.add(new JScrollPane(table), BorderLayout.CENTER);
        viewSection.add(tableWrap, BorderLayout.CENTER);

        // SUMMARY section: bold heading + rounded grey card with the totals.
        JPanel sumSection = new JPanel(new BorderLayout(0, 4));
        sumSection.setOpaque(false);
        JLabel sumHead = new JLabel("SUMMARY");
        sumHead.setFont(sumHead.getFont().deriveFont(Font.BOLD, 15f));
        sumSection.add(sumHead, BorderLayout.NORTH);

        JPanel sumCard = new JPanel(new BorderLayout(0, 6));
        sumCard.setBackground(UIUtil.cardBackground());
        sumCard.setBorder(UIUtil.cardBorder());
        JLabel sumTitle = new JLabel("REPORT SUMMARY");
        sumTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        sumTitle.setForeground(UIUtil.BRAND);
        sumCard.add(sumTitle, BorderLayout.NORTH);

        summaryArea.setEditable(false);
        summaryArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        summaryArea.setOpaque(false);
        summaryArea.setBorder(null);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        sumCard.add(summaryArea, BorderLayout.SOUTH);
        sumSection.add(sumCard, BorderLayout.CENTER);

        body.add(viewSection, BorderLayout.CENTER);
        body.add(sumSection, BorderLayout.SOUTH);
        return body;
    }

    /** True for columns whose content is numeric (Qty, FAT, SNF, Rate, Amount, Balance...). */
    private static boolean isNumericColumn(String header) {
        return "Qty".equalsIgnoreCase(header)
                || "FAT".equalsIgnoreCase(header)
                || "SNF".equalsIgnoreCase(header)
                || "Rate".equalsIgnoreCase(header)
                || "Amount".equalsIgnoreCase(header)
                || "Total Qty".equalsIgnoreCase(header)
                || "Avg FAT".equalsIgnoreCase(header)
                || "Avg SNF".equalsIgnoreCase(header)
                || "Avg Rate".equalsIgnoreCase(header)
                || "Total Amount".equalsIgnoreCase(header)
                || "Payment".equalsIgnoreCase(header)
                || "Balance".equalsIgnoreCase(header);
    }
// ---- report generation ----

    private LocalDate from() {
        LocalDate d = fromPicker.getDate();
        return d == null ? LocalDate.now().withDayOfMonth(1) : d;
    }

    private LocalDate to() {
        LocalDate d = toPicker.getDate();
        return d == null ? LocalDate.now() : d;
    }

    /** Returns the selected customer id, or null for "All Customers". */
    private Integer customerId() {
        return selectedCustomer == null ? null : selectedCustomer.getId();
    }

    private String milkType() {
        return "All".equals(milkBox.getSelectedItem()) ? null : (String) milkBox.getSelectedItem();
    }

    private String shift() {
        return "All".equals(shiftBox.getSelectedItem()) ? null : (String) shiftBox.getSelectedItem();
    }

    private void generate() {
        String type = (String) reportTypeBox.getSelectedItem();
        switch (type == null ? "" : type) {
            case "Daily Report": renderDaily(); break;
            case "Weekly Report": renderGrouped(false); break;
            case "Monthly Report": renderGrouped(true); break;
            case "Customer Report": renderCustomerReport(); break;
            case "Customer Statement": renderStatement(); break;
            case "Payment Report": renderPaymentReport(); break;
            default: renderDaily();
        }
    }

    private String dairyName() {
        String name = settingsService.get("dairy.name");
        return name == null || name.isBlank() ? AppConfig.APP_NAME : name;
    }

    private void renderDaily() {
        List<MilkCollection> records = reportService.milkRecords(from(), to(), customerId(), milkType(), shift());
        tableModel.setColumnIdentifiers(new Object[]{
                "Date", "Shift", "Code", "Name", "Milk", "FAT", "SNF", "Qty", "Rate", "Amount"});
        tableModel.setRowCount(0);
        for (MilkCollection m : records) {
            tableModel.addRow(new Object[]{
                    DateUtil.toDisplay(m.getCollectionDate()), m.getShift(), m.getCustomerCode(),
                    m.getCustomerName(), m.getMilkType(),
                    CurrencyUtil.format(m.getFat()), CurrencyUtil.format(m.getSnf()),
                    CurrencyUtil.format(m.getQuantity()), CurrencyUtil.format(m.getRatePerLitre()),
                    CurrencyUtil.formatMoney(m.getAmount())
            });
        }
        ReportSummary s = reportService.summarize(records);
        generatedTitle = "Daily Report";
        setSummary("Daily Report", s);
    }

    private void renderGrouped(boolean monthly) {
        List<CustomerSummaryRow> rows = reportService.customerGroupedReport(
                from(), to(), customerId(), milkType(), shift());
        tableModel.setColumnIdentifiers(new Object[]{
                "Customer Code", "Customer Name", "Milk Type", "Total Qty", "Avg FAT", "Avg SNF", "Avg Rate", "Total Amount"});
        tableModel.setRowCount(0);
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmt = BigDecimal.ZERO;
        BigDecimal fatW = BigDecimal.ZERO;
        BigDecimal snfW = BigDecimal.ZERO;
        for (CustomerSummaryRow r : rows) {
            tableModel.addRow(new Object[]{
                    r.getCustomerCode(), r.getCustomerName(), r.getMilkType(),
                    CurrencyUtil.format(r.getTotalQuantity()), CurrencyUtil.format(r.getAvgFat()),
                    CurrencyUtil.format(r.getAvgSnf()), CurrencyUtil.format(r.getAvgRate()),
                    CurrencyUtil.formatMoney(r.getTotalAmount())
            });
            totalQty = totalQty.add(r.getTotalQuantity());
            totalAmt = totalAmt.add(r.getTotalAmount());
            fatW = fatW.add(r.getTotalQuantity().multiply(r.getAvgFat()));
            snfW = snfW.add(r.getTotalQuantity().multiply(r.getAvgSnf()));
        }
        generatedTitle = monthly ? "Monthly Report" : "Weekly Report";
        StringBuilder sb = new StringBuilder();
        sb.append("Total Customers: ").append(rows.size()).append('\n');
        sb.append("Total Milk: ").append(CurrencyUtil.format(totalQty)).append(" LTR\n");
        sb.append("Total Amount: ").append(CurrencyUtil.formatMoney(totalAmt)).append('\n');
        if (totalQty.signum() != 0) {
            sb.append("Average FAT: ").append(CurrencyUtil.format(fatW.divide(totalQty, 2, java.math.RoundingMode.HALF_UP)))
                    .append('%').append('\n');
            sb.append("Average SNF: ").append(CurrencyUtil.format(snfW.divide(totalQty, 2, java.math.RoundingMode.HALF_UP)))
                    .append('%').append('\n');
        }
        summaryArea.setText(sb.toString());
    }

    private void renderCustomerReport() {
        Integer cid = customerId();
        if (cid == null) {
            UIUtil.showMessage(this, "Select a customer for the customer report.",
                    "Customer Report", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<MilkCollection> records = reportService.milkRecords(from(), to(), cid, milkType(), shift());
        tableModel.setColumnIdentifiers(new Object[]{
                "Date", "Shift", "Milk", "FAT", "SNF", "Qty", "Rate", "Amount"});
        tableModel.setRowCount(0);
        for (MilkCollection m : records) {
            tableModel.addRow(new Object[]{
                    DateUtil.toDisplay(m.getCollectionDate()), m.getShift(), m.getMilkType(),
                    CurrencyUtil.format(m.getFat()), CurrencyUtil.format(m.getSnf()),
                    CurrencyUtil.format(m.getQuantity()), CurrencyUtil.format(m.getRatePerLitre()),
                    CurrencyUtil.formatMoney(m.getAmount())
            });
        }
        ReportSummary s = reportService.summarize(records);
        generatedTitle = "Customer Report";
        setSummary("Customer Report", s);
    }
    private void renderStatement() {
        Integer cid = customerId();
        if (cid == null) {
            UIUtil.showMessage(this, "Select a customer for the customer statement.",
                    "Customer Statement", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Customer customer = customerService.findById(cid);
        List<LedgerEntry> entries = reportService.customerLedger(cid, from(), to());
        tableModel.setColumnIdentifiers(new Object[]{
                "Date", "Description", "Qty", "Amount", "Payment", "Balance"});
        tableModel.setRowCount(0);
        for (LedgerEntry e : entries) {
            tableModel.addRow(new Object[]{
                    DateUtil.toDisplay(e.getDate()), e.getDescription(),
                    CurrencyUtil.format(e.getQuantity()),
                    "PAYMENT".equals(e.getTransactionType()) ? "" : CurrencyUtil.formatMoney(e.getAmount()),
                    "PAYMENT".equals(e.getTransactionType()) ? CurrencyUtil.formatMoney(e.getPayment()) : "",
                    CurrencyUtil.formatMoney(e.getBalance())
            });
        }
        generatedTitle = "Customer Statement";
        BigDecimal closing = entries.isEmpty()
                ? (customer == null || customer.getOpeningBalance() == null ? BigDecimal.ZERO : customer.getOpeningBalance())
                : entries.get(entries.size() - 1).getBalance();
        summaryArea.setText("Customer: " + (customer == null ? "" : customer.getCustomerCode() + " - " + customer.getCustomerName())
                + "\nClosing Balance: " + CurrencyUtil.formatMoney(closing));
    }

    private void renderPaymentReport() {
        List<Payment> payments = reportService.payments(customerId(), from(), to());
        tableModel.setColumnIdentifiers(new Object[]{
                "Date", "Code", "Name", "Amount", "Mode", "Reference", "Remarks"});
        tableModel.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;
        for (Payment p : payments) {
            tableModel.addRow(new Object[]{
                    DateUtil.toDisplay(p.getPaymentDate()), p.getCustomerCode(), p.getCustomerName(),
                    CurrencyUtil.formatMoney(p.getAmount()), p.getPaymentMode(), p.getReference(), p.getRemarks()
            });
            total = total.add(p.getAmount() == null ? BigDecimal.ZERO : p.getAmount());
        }
        generatedTitle = "Payment Report";
        summaryArea.setText("Total Payments: " + CurrencyUtil.formatMoney(total) + "  (" + payments.size() + " records)");
    }

    private void setSummary(String title, ReportSummary s) {
        generatedTitle = title;
        StringBuilder sb = new StringBuilder();
        sb.append("Total Quantity: ").append(CurrencyUtil.format(s.getTotalQuantity())).append(" LTR\n");
        sb.append("Total Amount: ").append(CurrencyUtil.formatMoney(s.getTotalAmount())).append('\n');
        sb.append("Average FAT: ").append(CurrencyUtil.format(s.getAvgFat())).append("%\n");
        sb.append("Average SNF: ").append(CurrencyUtil.format(s.getAvgSnf())).append("%\n");
        sb.append("Average Rate: ").append(CurrencyUtil.formatMoney(s.getAvgRate())).append("/LTR");
        summaryArea.setText(sb.toString());
    }
// ---- print and export ----

    private void printReport() {
        List<String> lines = new ArrayList<>();
        lines.add(dairyName());
        lines.add(generatedTitle);
        lines.add("Period: " + DateUtil.toDisplay(fromPicker.getDate()) + " to "
                + DateUtil.toDisplay(toPicker.getDate()));
        lines.add("Generated: " + DateUtil.toDisplay(LocalDate.now()));
        lines.add("=".repeat(90));
        StringBuilder header = new StringBuilder();
        for (int c = 0; c < tableModel.getColumnCount(); c++) {
            if (c > 0) {
                header.append(" | ");
            }
            header.append(tableModel.getColumnName(c));
        }
        lines.add(header.toString());
        lines.add("-".repeat(90));
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            StringBuilder row = new StringBuilder();
            for (int c = 0; c < tableModel.getColumnCount(); c++) {
                if (c > 0) {
                    row.append(" | ");
                }
                Object v = tableModel.getValueAt(r, c);
                row.append(v == null ? "" : v.toString());
            }
            lines.add(row.toString());
        }
        lines.add("-".repeat(90));
        lines.add(summaryArea.getText());
        PrintUtil.printText(findOwner(), generatedTitle, lines, true);
    }

    private void exportCsv() {
        if (tableModel.getRowCount() == 0) {
            UIUtil.showMessage(this, "Generate the report first.", "Export",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser(AppConfig.getExportDir().toFile());
        chooser.setDialogTitle("Export Report to CSV");
        chooser.setSelectedFile(new File(AppConfig.getExportDir().toFile(),
                generatedTitle.replace(' ', '_') + "_" + LocalDate.now() + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        try {
            List<List<String>> rows = new ArrayList<>();
            List<String> header = new ArrayList<>();
            for (int c = 0; c < tableModel.getColumnCount(); c++) {
                header.add(tableModel.getColumnName(c));
            }
            rows.add(header);
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                List<String> row = new ArrayList<>();
                for (int c = 0; c < tableModel.getColumnCount(); c++) {
                    Object v = tableModel.getValueAt(r, c);
                    row.add(v == null ? "" : v.toString());
                }
                rows.add(row);
            }
            Files.createDirectories(file.toPath().getParent());
            CSVUtil.write(file.toPath(), rows);
            UIUtil.showMessage(this, "Exported to " + file.getAbsolutePath(),
                    "Export", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            UIUtil.showMessage(this, "Export failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
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