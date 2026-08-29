package dairy.erp.ui;

import dairy.erp.config.AppConfig;
import dairy.erp.dao.CustomerDAO;
import dairy.erp.dao.PaymentDAO;
import dairy.erp.model.Customer;
import dairy.erp.model.MilkCollection;
import dairy.erp.model.Payment;
import dairy.erp.service.BackupService;
import dairy.erp.service.CustomerService;
import dairy.erp.service.MilkCollectionService;
import dairy.erp.service.ReportService;
import dairy.erp.util.CSVUtil;
import dairy.erp.util.CurrencyUtil;
import dairy.erp.util.DateUtil;
import dairy.erp.util.LogUtil;
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
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Import/export and backup/restore centre. CSV import validates every row and
 * commits inside a transaction so partial corruption is avoided.
 */
public class ImportExportPanel extends JPanel {

    private static final Logger LOG = LogUtil.getLogger(ImportExportPanel.class);

    private final MilkCollectionService milkService = new MilkCollectionService();
    private final CustomerService customerService = new CustomerService();
    private final ReportService reportService = new ReportService();
    private final BackupService backupService = new BackupService();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    private final JComboBox<String> exportTypeBox = new JComboBox<>(new String[]{
            "Customers", "Milk Collection", "Payments", "Customer Ledger", "Daily Report", "Monthly Report"});
    private final JTextArea logArea = new JTextArea(10, 70);

    public ImportExportPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        logArea.setEditable(false);
        add(buildControls(), BorderLayout.NORTH);
        add(new JScrollPane(logArea), BorderLayout.CENTER);
        append("Ready.");
    }


    private JPanel buildControls() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("Data Tools"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0;
        JButton importBtn = new JButton("Import CSV (Milk Collection)");
        UIUtil.styleButton(importBtn);
        importBtn.addActionListener(e -> importCsv());
        p.add(importBtn, g);

        g.gridx = 1; g.gridy = 0;
        p.add(new JLabel("Export:"), g);
        g.gridx = 2;
        p.add(exportTypeBox, g);
        g.gridx = 3;
        JButton exportBtn = new JButton("Export CSV");
        UIUtil.styleButton(exportBtn);
        exportBtn.addActionListener(e -> exportCsv());
        p.add(exportBtn, g);

        g.gridx = 4; g.gridy = 0;
        JButton backupBtn = new JButton("Backup Database");
        UIUtil.styleButton(backupBtn);
        backupBtn.addActionListener(e -> backup());        p.add(backupBtn, g);
        g.gridx = 5;
        JButton restoreBtn = new JButton("Restore Database");
        UIUtil.styleButton(restoreBtn);
        restoreBtn.addActionListener(e -> restore());
        p.add(restoreBtn, g);
        JPanel wrap = new JPanel(new BorderLayout(0, 4));
        wrap.add(UIUtil.header("Import / Export / Backup"), BorderLayout.NORTH);
        wrap.add(p, BorderLayout.CENTER);
        return wrap;
    }

    private void append(String msg) {
        logArea.append(msg);
        if (!msg.endsWith("\n")) {
            logArea.append("\n");
        }
    }

    private boolean isHeader(List<String> header) {
        return header.size() >= 7
                && header.get(0).equalsIgnoreCase("date")
                && header.get(1).equalsIgnoreCase("customercode");
    }

    private void importCsv() {
        JFileChooser chooser = new JFileChooser(AppConfig.getBaseDir().toFile());
        chooser.setDialogTitle("Select CSV file to import");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        try {
            List<List<String>> rows = CSVUtil.read(file.toPath());
            if (rows.size() < 2) {
                JOptionPane.showMessageDialog(this, "CSV has no data rows.", "Import",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            List<String> header = rows.get(0);
            if (!isHeader(header)) {
                JOptionPane.showMessageDialog(this,
                        "Header must be: Date,CustomerCode,MilkType,Shift,Quantity,FAT,SNF",
                        "Import", JOptionPane.WARNING_MESSAGE);
                return;
            }
            List<String> errors = new ArrayList<>();
            List<MilkCollection> valid = new ArrayList<>();
            for (int i = 1; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                if (row.size() < 7) {
                    errors.add("Row " + (i + 1) + ": not enough columns (expected 7).");
                    continue;
                }
                String csvDate = row.get(0).trim();
                String code = row.get(1).trim();
                String milkType = row.get(2).trim();
                String shift = row.get(3).trim();
                BigDecimal qty = ValidationUtil.parseDecimal(row.get(4));
                BigDecimal fat = ValidationUtil.parseDecimal(row.get(5));
                BigDecimal snf = ValidationUtil.parseDecimal(row.get(6));

                LocalDate date = DateUtil.parse(csvDate);
                if (date == null) {
                    date = DateUtil.parseDb(csvDate);
                }
                boolean ok = true;
                if (date == null) { errors.add("Row " + (i + 1) + ": invalid date '" + csvDate + "'."); ok = false; }
                if (qty == null || qty.signum() <= 0) { errors.add("Row " + (i + 1) + ": invalid quantity."); ok = false; }
                if (fat == null || fat.signum() <= 0) { errors.add("Row " + (i + 1) + ": invalid FAT."); ok = false; }
                if (snf == null || snf.signum() <= 0) { errors.add("Row " + (i + 1) + ": invalid SNF."); ok = false; }
                if (!"Morning".equals(shift) && !"Evening".equals(shift)) {
                    errors.add("Row " + (i + 1) + ": shift must be Morning or Evening."); ok = false;
                }
                if (!"Cow".equalsIgnoreCase(milkType) && !"Buffalo".equalsIgnoreCase(milkType)
                        && !"Mix".equalsIgnoreCase(milkType)) {
                    errors.add("Row " + (i + 1) + ": milk type must be Cow, Buffalo or Mix."); ok = false;
                }
                Customer customer = customerService.findByCode(code);
                if (customer == null) { errors.add("Row " + (i + 1) + ": unknown customer code '" + code + "'."); ok = false; }
                if (!ok) {
                    continue;
                }
                MilkCollectionService.CalculationResult result = milkService.calculate(
                        customer, date, shift, milkType, qty, fat, snf, false, null);
                if (result == null) {
                    errors.add("Row " + (i + 1) + ": no rate rule for this milk type/FAT/SNF.");
                    continue;
                }
                MilkCollection m = result.collection;
                m.setRemarks("Imported");
                valid.add(m);
            }

            if (!errors.isEmpty()) {
                StringBuilder sb = new StringBuilder("Validation errors — nothing was imported:\n");
                for (String e : errors) {
                    sb.append("  ").append(e).append('\n');
                }
                append(sb.toString());
                JOptionPane.showMessageDialog(this, sb.toString(), "Import Validation",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (valid.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No valid rows to import.", "Import",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            List<String> skipErrors = milkService.importMilk(valid);
            StringBuilder sb = new StringBuilder("Imported " + (valid.size() - skipErrors.size())
                    + " of " + valid.size() + " records.\n");
            for (String e : skipErrors) {
                sb.append("  ").append(e).append('\n');
            }
            append(sb.toString());
            JOptionPane.showMessageDialog(this, sb.toString(), "Import Result",
                    skipErrors.isEmpty() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
            LOG.info("CSV import completed. Valid=" + valid.size() + " skipped=" + skipErrors.size());
        } catch (Exception ex) {
            LOG.severe("CSV import failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Import failed: " + ex.getMessage(),
                    "Import", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void exportCsv() {
        String type = (String) exportTypeBox.getSelectedItem();
        JFileChooser chooser = new JFileChooser(AppConfig.getExportDir().toFile());
        chooser.setDialogTitle("Export to CSV");
        chooser.setSelectedFile(new File(AppConfig.getExportDir().toFile(),
                type.replace(' ', '_') + "_" + LocalDate.now() + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path path = chooser.getSelectedFile().toPath();
        try {
            switch (type == null ? "" : type) {
                case "Customers": exportCustomers(path); break;
                case "Milk Collection": exportMilk(path); break;
                case "Payments": exportPayments(path); break;
                case "Customer Ledger": exportLedger(path); break;
                case "Daily Report": exportDailyReport(path); break;
                default: exportMonthlyReport(path); break;
            }
            append("Exported " + type + " to " + path);
            JOptionPane.showMessageDialog(this, "Exported to " + path, "Export",
                    JOptionPane.INFORMATION_MESSAGE);
            LOG.info("Exported " + type + " to " + path);
        } catch (Exception ex) {
            LOG.severe("Export failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(),
                    "Export", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportCustomers(Path path) throws Exception {
        List<List<String>> rows = new ArrayList<>();
        rows.add(listOf("Customer Code", "Customer Name", "Father Name", "Mobile", "Address",
                "Village", "Milk Type", "Customer Type", "Status", "Opening Balance", "Reg. Date"));
        for (Customer c : customerDAO.findAll()) {
            rows.add(listOf(c.getCustomerCode(), c.getCustomerName(), c.getFatherName(),
                    c.getMobile(), c.getAddress(), c.getVillage(), c.getMilkType(),
                    c.getCustomerType(), c.getStatus(),
                    c.getOpeningBalance() == null ? "" : c.getOpeningBalance().toPlainString(),
                    DateUtil.toDisplay(c.getRegistrationDate())));
        }
        CSVUtil.write(path, rows);
    }

    private void exportMilk(Path path) throws Exception {
        List<List<String>> rows = new ArrayList<>();
        rows.add(listOf("Date", "Customer Code", "Customer Name", "Milk Type", "Shift",
                "Quantity", "FAT", "SNF", "Rate", "Amount"));
        for (MilkCollection m : milkService.all()) {
            rows.add(listOf(DateUtil.toDisplay(m.getCollectionDate()), m.getCustomerCode(),
                    m.getCustomerName(), m.getMilkType(), m.getShift(),
                    m.getQuantity().toPlainString(), m.getFat().toPlainString(),
                    m.getSnf().toPlainString(), m.getRatePerLitre().toPlainString(),
                    m.getAmount().toPlainString()));
        }
        CSVUtil.write(path, rows);
    }

    private void exportPayments(Path path) throws Exception {
        List<List<String>> rows = new ArrayList<>();
        rows.add(listOf("Payment Date", "Customer Code", "Customer Name", "Amount", "Mode", "Reference"));
        for (Payment p : paymentDAO.findAll(null, null)) {
            rows.add(listOf(DateUtil.toDisplay(p.getPaymentDate()), p.getCustomerCode(),
                    p.getCustomerName(), p.getAmount().toPlainString(), p.getPaymentMode(), p.getReference()));
        }
        CSVUtil.write(path, rows);
    }


    private void exportLedger(Path path) throws Exception {
        LocalDate from = DateUtil.today().withDayOfMonth(1);
        LocalDate to = DateUtil.today();
        List<List<String>> rows = new ArrayList<>();
        rows.add(listOf("Customer Code", "Date", "Type", "Description", "Qty", "Amount", "Payment", "Balance"));
        for (Customer c : customerDAO.findAll()) {
            for (dairy.erp.model.LedgerEntry e : reportService.customerLedger(c.getId(), from, to)) {
                rows.add(listOf(c.getCustomerCode(), DateUtil.toDisplay(e.getDate()),
                        e.getTransactionType(), e.getDescription(),
                        e.getQuantity().toPlainString(), e.getAmount().toPlainString(),
                        e.getPayment().toPlainString(), e.getBalance().toPlainString()));
            }
        }
        CSVUtil.write(path, rows);
    }

    private void exportDailyReport(Path path) throws Exception {
        LocalDate today = DateUtil.today();
        List<List<String>> rows = new ArrayList<>();
        rows.add(listOf("Date", "Customer Code", "Customer Name", "Milk Type", "Shift",
                "Qty", "FAT", "SNF", "Rate", "Amount"));
        for (MilkCollection m : reportService.milkRecords(today, today, null, null, null)) {
            rows.add(listOf(DateUtil.toDisplay(m.getCollectionDate()), m.getCustomerCode(),
                    m.getCustomerName(), m.getMilkType(), m.getShift(),
                    m.getQuantity().toPlainString(), m.getFat().toPlainString(),
                    m.getSnf().toPlainString(), m.getRatePerLitre().toPlainString(),
                    m.getAmount().toPlainString()));
        }
        CSVUtil.write(path, rows);
    }

    private void exportMonthlyReport(Path path) throws Exception {
        LocalDate from = DateUtil.today().withDayOfMonth(1);
        LocalDate to = DateUtil.today();
        List<List<String>> rows = new ArrayList<>();
        rows.add(listOf("Customer Code", "Customer Name", "Milk Type", "Total Qty",
                "Avg FAT", "Avg SNF", "Avg Rate", "Total Amount"));
        for (dairy.erp.model.CustomerSummaryRow r : reportService.customerGroupedReport(from, to, null, null, null)) {
            rows.add(listOf(r.getCustomerCode(), r.getCustomerName(), r.getMilkType(),
                    r.getTotalQuantity().toPlainString(), r.getAvgFat().toPlainString(),
                    r.getAvgSnf().toPlainString(), r.getAvgRate().toPlainString(),
                    r.getTotalAmount().toPlainString()));
        }
        CSVUtil.write(path, rows);
    }


    private void backup() {
        try {
            File file = backupService.backup();
            append("Backup created: " + file.getAbsolutePath());
            JOptionPane.showMessageDialog(this, "Database backed up to:\n" + file.getAbsolutePath(),
                    "Backup", JOptionPane.INFORMATION_MESSAGE);
            LOG.info("Backup completed: " + file.getAbsolutePath());
        } catch (Exception ex) {
            LOG.severe("Backup failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Backup failed: " + ex.getMessage(),
                    "Backup", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void restore() {
        JFileChooser chooser = new JFileChooser(AppConfig.getBackupDir().toFile());
        chooser.setDialogTitle("Select backup to restore");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to restore the database?\n"
                        + "Current data may be replaced.\n\n" + file.getName(),
                "Restore Database", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            backupService.restore(file);
            append("Database restored from " + file.getName());
            JOptionPane.showMessageDialog(this,
                    "Database restored successfully.\nPlease restart the application.",
                    "Restore", JOptionPane.INFORMATION_MESSAGE);
            LOG.info("Database restored from " + file.getAbsolutePath());
        } catch (Exception ex) {
            LOG.severe("Restore failed: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Restore failed: " + ex.getMessage(),
                    "Restore", JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<String> listOf(String... values) {
        List<String> list = new ArrayList<>();
        for (String v : values) {
            list.add(v == null ? "" : v);
        }
        return list;
    }

    private java.awt.Frame findOwner() {
        java.awt.Container c = this;
        while (c != null && !(c instanceof JFrame)) {
            c = c.getParent();
        }
        return (java.awt.Frame) c;
    }
}

