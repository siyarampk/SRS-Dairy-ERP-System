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
import dairy.erp.util.CSVUtil;
import dairy.erp.util.CurrencyUtil;
import dairy.erp.util.DateUtil;
import dairy.erp.util.PrintUtil;
import dairy.erp.util.UIUtil;
import dairy.erp.util.ValidationUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
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
 * and Payment report with date/customer/milk-type/shift filters, a weighted
 * summary, printing and CSV export.
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
    private final JTextField customerField = new JTextField(10);
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

    public ReportsPanel() {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        fromPicker.setDate(LocalDate.now().withDayOfMonth(1));
        toPicker.setDate(LocalDate.now());
        add(buildFilters(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    private JPanel buildFilters() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Report Filters"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0; p.add(new JLabel("Report:"), g);
        g.gridx = 1; p.add(reportTypeBox, g);
        g.gridx = 2; p.add(new JLabel("From:"), g);
        g.gridx = 3; p.add(fromPicker, g);
        g.gridx = 4; p.add(new JLabel("To:"), g);
        g.gridx = 5; p.add(toPicker, g);
        g.gridx = 6; p.add(new JLabel("Customer:"), g);
        g.gridx = 7; p.add(customerField, g);
        g.gridx = 8; p.add(new JLabel("Milk:"), g);
        g.gridx = 9; p.add(milkBox, g);
        g.gridx = 10; p.add(new JLabel("Shift:"), g);
        g.gridx = 11; p.add(shiftBox, g);

        JButton generate = new JButton("Generate");
        UIUtil.styleButton(generate);
        generate.addActionListener(e -> generate());
        g.gridx = 12; p.add(generate, g);
        JButton print = new JButton("Print");
        UIUtil.styleButton(print);
        print.addActionListener(e -> printReport());
        g.gridx = 13; p.add(print, g);
        JButton export = new JButton("Export CSV");
        UIUtil.styleButton(export);
        export.addActionListener(e -> exportCsv());
        g.gridx = 14; p.add(export, g);
        JPanel wrap = new JPanel(new BorderLayout(0, 4));
        wrap.add(UIUtil.header("Reports"), BorderLayout.NORTH);
        wrap.add(p, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(8, 8));
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        summaryArea.setEditable(false);
        summaryArea.setBorder(BorderFactory.createTitledBorder("Summary"));
        JPanel south = new JPanel(new BorderLayout());
        south.add(summaryArea, BorderLayout.CENTER);
        body.add(new JScrollPane(table), BorderLayout.CENTER);
        body.add(south, BorderLayout.SOUTH);
        return body;
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

    private Integer customerId() {
        String code = customerField.getText().trim();
        if (ValidationUtil.isBlank(code)) {
            return null;
        }
        Customer c = customerService.findByCode(code);
        return c == null ? null : c.getId();
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
        return name.isBlank() ? AppConfig.APP_NAME : name;
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
            JOptionPane.showMessageDialog(this, "Enter a customer code for the customer report.",
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
            JOptionPane.showMessageDialog(this, "Enter a customer code for the customer statement.",
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
            JOptionPane.showMessageDialog(this, "Generate the report first.", "Export",
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
            CSVUtil.write(file.toPath(), rows);
            JOptionPane.showMessageDialog(this, "Exported to " + file.getAbsolutePath(),
                    "Export", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(),
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

