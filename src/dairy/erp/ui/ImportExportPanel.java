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
import dairy.erp.util.ButtonIcons;
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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

    public ImportExportPanel(String username) {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(6, 10, 8, 10));
        logArea.setEditable(false);

        // Split: tools on top (vertically arranged, centered), log as footer.
        JPanel topWrap = new JPanel(new BorderLayout());
        JPanel toolsPanel = buildControls(username);
        JPanel logPanel = buildLogArea();
        logPanel.setMinimumSize(new Dimension(200, 100));
        topWrap.add(toolsPanel, BorderLayout.NORTH);
        topWrap.add(logPanel, BorderLayout.SOUTH);
        add(topWrap, BorderLayout.CENTER);
        append("Ready.");
    }

    private JPanel buildLogArea() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Activity Log"));
        logArea.setFont(new java.awt.Font(Font.SANS_SERIF, Font.PLAIN, 16));
        logArea.setBackground(Color.WHITE);
        logArea.setForeground(new Color(0x33, 0x33, 0x33));
        p.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return p;
    }


    private JPanel buildControls(String username) {
        JPanel top = new JPanel(new BorderLayout(0, 6));

        // Row 1: logo on the left, [ User ] [ Date ] on the right.
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.add(UIUtil.header("Import / Export / Backup"), BorderLayout.WEST);
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

        // Middle: title row.
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        JLabel title = new JLabel("DATA TOOLS");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        title.setForeground(UIUtil.BRAND);
        titleRow.add(title);
        top.add(titleRow, BorderLayout.CENTER);

        // Two balanced panels: Data Movement (Import + Export) on the left,
        // Database Maintenance (Backup + Restore) on the right.
        JPanel body = new JPanel(new GridLayout(1, 2, 18, 0));
        body.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JPanel left = new JPanel(new BorderLayout(0, 14));
        left.setBorder(BorderFactory.createTitledBorder("Data Movement"));
        left.setBackground(Color.WHITE);
        JPanel leftStack = new JPanel();
        leftStack.setLayout(new javax.swing.BoxLayout(leftStack, javax.swing.BoxLayout.Y_AXIS));
        leftStack.setBackground(Color.WHITE);
        leftStack.add(buildImportCard());
        leftStack.add(javax.swing.Box.createVerticalStrut(14));
        leftStack.add(buildExportCard());
        left.add(leftStack, BorderLayout.CENTER);
        body.add(left);

        JPanel right = new JPanel(new BorderLayout(0, 14));
        right.setBorder(BorderFactory.createTitledBorder("Database Maintenance"));
        right.setBackground(Color.WHITE);
        JPanel rightStack = new JPanel();
        rightStack.setLayout(new javax.swing.BoxLayout(rightStack, javax.swing.BoxLayout.Y_AXIS));
        rightStack.setBackground(Color.WHITE);
        rightStack.add(buildBackupCard());
        rightStack.add(javax.swing.Box.createVerticalStrut(14));
        rightStack.add(buildRestoreCard());
        right.add(rightStack, BorderLayout.CENTER);
        body.add(right);

        top.add(body, BorderLayout.SOUTH);
        return top;
    }

    private JPanel buildImportCard() {
        // Compact card like the reference: coloured title row, then one
        // full-width icon button.
        JPanel importCard = toolCard("IMPORT", new Color(0x45, 0xA2, 0xB8)); // teal
        JButton importBtn = new JButton("Import CSV", ButtonIcons.of("Upload", Color.WHITE));
        UIUtil.styleSmallButton(importBtn, new Color(0x45, 0xA2, 0xB8)); // teal
        importBtn.setPreferredSize(new Dimension(200, 40));
        importBtn.addActionListener(e -> importCsv());
        importCard.add(importBtn, BorderLayout.SOUTH); // stretches full card width
        return importCard;
    }

    private JPanel buildExportCard() {
        JPanel exportCard = toolCard("EXPORT", new Color(0x2E, 0x7D, 0x32)); // green
        // BorderLayout guarantees the Export CSV button is always visible:
        // the dropdown fills the remaining space and the button pins right.
        JPanel exportRow = new JPanel(new BorderLayout(8, 0));
        exportRow.setOpaque(false);
        exportTypeBox.setPreferredSize(new Dimension(120, 38));
        exportTypeBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        JButton exportBtn = new JButton("Export CSV", ButtonIcons.of("Upload", Color.WHITE));
        UIUtil.styleSmallButton(exportBtn, new Color(0x2E, 0x7D, 0x32)); // green
        exportBtn.setPreferredSize(new Dimension(180, 40));
        exportBtn.addActionListener(e -> exportCsv());
        exportRow.add(exportTypeBox, BorderLayout.CENTER);
        exportRow.add(exportBtn, BorderLayout.EAST);
        exportCard.add(exportRow, BorderLayout.SOUTH);
        return exportCard;
    }

    private JPanel buildBackupCard() {
        JPanel backupCard = toolCard("BACKUP", new Color(0xEF, 0x6C, 0x00)); // orange
        JButton backupBtn = new JButton("Backup Database", ButtonIcons.of("Cloud", Color.WHITE));
        UIUtil.styleSmallButton(backupBtn, new Color(0xEF, 0x6C, 0x00)); // orange
        backupBtn.setPreferredSize(new Dimension(200, 40));
        backupBtn.addActionListener(e -> backup());
        backupCard.add(backupBtn, BorderLayout.SOUTH); // stretches full card width
        return backupCard;
    }

    private JPanel buildRestoreCard() {
        JPanel restoreCard = toolCard("RESTORE", new Color(0x60, 0x7D, 0x8B)); // slate
        JButton restoreBtn = new JButton("Restore Database", ButtonIcons.of("Refresh", Color.WHITE));
        UIUtil.styleSmallButton(restoreBtn, new Color(0x60, 0x7D, 0x8B)); // slate
        restoreBtn.setPreferredSize(new Dimension(200, 40));
        restoreBtn.addActionListener(e -> restore());
        restoreCard.add(restoreBtn, BorderLayout.SOUTH); // stretches full card width
        return restoreCard;
    }

    /**
     * Builds one tool card in the reference style: accent strip + coloured
     * title on top, action button (added by the caller) below. Cards stretch
     * to the full width of their column and stay compact — no description
     * text, as in the screenshot.
     */
    private JPanel toolCard(String title, Color accent) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xC9, 0xD6, 0xDE)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        card.setBackground(Color.WHITE);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        JPanel accentStrip = new JPanel();
        accentStrip.setBackground(accent);
        accentStrip.setPreferredSize(new Dimension(6, 26));
        header.add(accentStrip, BorderLayout.WEST);
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        titleLbl.setForeground(accent);
        header.add(titleLbl, BorderLayout.CENTER);
        card.add(header, BorderLayout.NORTH);

        return card;
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
                UIUtil.showMessage(this, "CSV has no data rows.", "Import",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            List<String> header = rows.get(0);
            if (!isHeader(header)) {
                UIUtil.showMessage(this,
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
                UIUtil.showMessage(this, sb.toString(), "Import Validation",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (valid.isEmpty()) {
                UIUtil.showMessage(this, "No valid rows to import.", "Import",
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
            UIUtil.showMessage(this, sb.toString(), "Import Result",
                    skipErrors.isEmpty() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
            LOG.info("CSV import completed. Valid=" + valid.size() + " skipped=" + skipErrors.size());
        } catch (Exception ex) {
            LOG.severe("CSV import failed: " + ex.getMessage());
            UIUtil.showMessage(this, "Import failed: " + ex.getMessage(),
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
            UIUtil.showMessage(this, "Exported to " + path, "Export",
                    JOptionPane.INFORMATION_MESSAGE);
            LOG.info("Exported " + type + " to " + path);
        } catch (Exception ex) {
            LOG.severe("Export failed: " + ex.getMessage());
            UIUtil.showMessage(this, "Export failed: " + ex.getMessage(),
                    "Export", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportCustomers(Path path) throws Exception {
        List<List<String>> rows = new ArrayList<>();
        rows.add(listOf("Customer Code", "Customer Name", "Mobile", "Address",
                "Village", "Milk Type", "Customer Type", "Status", "Opening Balance", "Reg. Date"));
        for (Customer c : customerDAO.findAll()) {
            rows.add(listOf(c.getCustomerCode(), c.getCustomerName(),
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
            UIUtil.showMessage(this, "Database backed up to:\n" + file.getAbsolutePath(),
                    "Backup", JOptionPane.INFORMATION_MESSAGE);
            LOG.info("Backup completed: " + file.getAbsolutePath());
        } catch (Exception ex) {
            LOG.severe("Backup failed: " + ex.getMessage());
            UIUtil.showMessage(this, "Backup failed: " + ex.getMessage(),
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
        int confirm = UIUtil.confirm(this,
                "Are you sure you want to restore the database?\n"
                        + "Current data may be replaced.\n\n" + file.getName(),
                "Restore Database");
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            backupService.restore(file);
            append("Database restored from " + file.getName());
            UIUtil.showMessage(this,
                    "Database restored successfully.\nPlease restart the application.",
                    "Restore", JOptionPane.INFORMATION_MESSAGE);
            LOG.info("Database restored from " + file.getAbsolutePath());
        } catch (Exception ex) {
            LOG.severe("Restore failed: " + ex.getMessage());
            UIUtil.showMessage(this, "Restore failed: " + ex.getMessage(),
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

