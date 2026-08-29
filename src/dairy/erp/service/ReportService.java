package dairy.erp.service;

import dairy.erp.dao.CustomerDAO;
import dairy.erp.dao.MilkCollectionDAO;
import dairy.erp.dao.PaymentDAO;
import dairy.erp.model.Customer;
import dairy.erp.model.CustomerSummaryRow;
import dairy.erp.model.LedgerEntry;
import dairy.erp.model.MilkCollection;
import dairy.erp.model.Payment;
import dairy.erp.model.ReportSummary;
import dairy.erp.util.LogUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Computes reports: customer ledger, customer statement, weekly/monthly
 * customer summaries and weighted-average report summaries.
 */
public class ReportService {

    private static final Logger LOG = LogUtil.getLogger(ReportService.class);

    private final MilkCollectionDAO milkDAO = new MilkCollectionDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();

    public List<MilkCollection> milkRecords(LocalDate from, LocalDate to, Integer customerId,
                                            String milkType, String shift) {
        try {
            return milkDAO.findByDateRange(from, to, customerId, milkType, shift);
        } catch (SQLException e) {
            LOG.severe("Report milk query failed: " + e.getMessage());
            throw new RuntimeException("Could not load milk records for report.", e);
        }
    }

    public List<Payment> payments(Integer customerId, LocalDate from, LocalDate to) {
        try {
            return paymentDAO.findByCustomerDateRange(customerId == null ? -1 : customerId, from, to);
        } catch (SQLException e) {
            LOG.severe("Report payment query failed: " + e.getMessage());
            throw new RuntimeException("Could not load payments for report.", e);
        }
    }

    public Customer findCustomer(int id) {
        try {
            return customerDAO.findById(id);
        } catch (SQLException e) {
            LOG.severe("Customer lookup failed: " + e.getMessage());
            throw new RuntimeException("Could not load customer.", e);
        }
    }

    /**
     * Builds a customer ledger: opening balance, then milk credits and payments
     * interleaved chronologically with a running balance.
     */
    public List<LedgerEntry> customerLedger(int customerId, LocalDate from, LocalDate to) {
        List<LedgerEntry> entries = new ArrayList<>();
        Customer customer = findCustomer(customerId);
        if (customer == null) {
            return entries;
        }

        Map<LocalDate, List<LedgerEntry>> byDate = new LinkedHashMap<>();
        for (MilkCollection m : milkRecords(from, to, customerId, null, null)) {
            LedgerEntry e = new LedgerEntry();
            e.setDate(m.getCollectionDate());
            e.setTransactionType("MILK");
            e.setDescription("Milk Collection (" + (m.getShift() == null ? "" : m.getShift()) + ")");
            e.setQuantity(m.getQuantity());
            e.setAmount(m.getAmount());
            byDate.computeIfAbsent(e.getDate(), k -> new ArrayList<>()).add(e);
        }
        for (Payment p : payments(customerId, from, to)) {
            LedgerEntry e = new LedgerEntry();
            e.setDate(p.getPaymentDate());
            e.setTransactionType("PAYMENT");
            e.setDescription("Payment (" + (p.getPaymentMode() == null ? "" : p.getPaymentMode()) + ")");
            e.setPayment(p.getAmount());
            byDate.computeIfAbsent(e.getDate(), k -> new ArrayList<>()).add(e);
        }

        List<LocalDate> sortedDates = new ArrayList<>(byDate.keySet());
        sortedDates.sort(LocalDate::compareTo);

        BigDecimal balance = customer.getOpeningBalance() == null
                ? BigDecimal.ZERO : customer.getOpeningBalance();
        for (LocalDate d : sortedDates) {
            for (LedgerEntry e : byDate.get(d)) {
                if ("PAYMENT".equals(e.getTransactionType())) {
                    balance = balance.subtract(e.getPayment());
                } else {
                    balance = balance.add(e.getAmount());
                }
                e.setBalance(balance);
                entries.add(e);
            }
        }
        return entries;
    }


    /** Summaries for a list of milk records (weighted FAT/SNF, amount-based average rate). */
    public ReportSummary summarize(List<MilkCollection> records) {
        ReportSummary s = new ReportSummary();
        BigDecimal qty = BigDecimal.ZERO;
        BigDecimal amt = BigDecimal.ZERO;
        BigDecimal fatW = BigDecimal.ZERO;
        BigDecimal snfW = BigDecimal.ZERO;
        long ctr = 0;
        for (MilkCollection m : records) {
            qty = qty.add(nz(m.getQuantity()));
            amt = amt.add(nz(m.getAmount()));
            fatW = fatW.add(nz(m.getQuantity()).multiply(nz(m.getFat())));
            snfW = snfW.add(nz(m.getQuantity()).multiply(nz(m.getSnf())));
            ctr++;
        }
        s.setTotalQuantity(qty);
        s.setTotalAmount(amt);
        s.setTotalRecords(ctr);
        if (qty.signum() != 0) {
            s.setAvgFat(fatW.divide(qty, 2, RoundingMode.HALF_UP));
            s.setAvgSnf(snfW.divide(qty, 2, RoundingMode.HALF_UP));
            s.setAvgRate(amt.divide(qty, 2, RoundingMode.HALF_UP));
        }
        return s;
    }

    /** Weekly/Monthly report grouped per customer. */
    public List<CustomerSummaryRow> customerGroupedReport(LocalDate from, LocalDate to,
                                                          Integer customerId, String milkType,
                                                          String shift) {
        List<MilkCollection> records = milkRecords(from, to, customerId, milkType, shift);
                Map<String, CustomerSummaryRow> rows = new LinkedHashMap<>();
        // Track raw weighted sums so averages are computed from exact values,
        // not reconstructed from already-rounded per-customer averages.
        Map<String, BigDecimal> fatSums = new LinkedHashMap<>();
        Map<String, BigDecimal> snfSums = new LinkedHashMap<>();
        for (MilkCollection m : records) {
            String key = m.getCustomerCode() + "|" + m.getMilkType();
            CustomerSummaryRow row = rows.computeIfAbsent(key, k -> {
                CustomerSummaryRow r = new CustomerSummaryRow();
                r.setCustomerCode(m.getCustomerCode());
                r.setCustomerName(m.getCustomerName());
                r.setMilkType(m.getMilkType());
                return r;
            });
            BigDecimal q = nz(m.getQuantity());
            BigDecimal newQty = row.getTotalQuantity().add(q);
            BigDecimal newAmt = row.getTotalAmount().add(nz(m.getAmount()));
            BigDecimal fatSum = fatSums.getOrDefault(key, BigDecimal.ZERO).add(q.multiply(nz(m.getFat())));
            BigDecimal snfSum = snfSums.getOrDefault(key, BigDecimal.ZERO).add(q.multiply(nz(m.getSnf())));
            fatSums.put(key, fatSum);
            snfSums.put(key, snfSum);
            row.setTotalQuantity(newQty);
            row.setTotalAmount(newAmt);
            row.setAvgFat(fatSum.divide(newQty, 2, RoundingMode.HALF_UP));
            row.setAvgSnf(snfSum.divide(newQty, 2, RoundingMode.HALF_UP));
        }
        for (CustomerSummaryRow row : rows.values()) {
            if (row.getTotalQuantity().signum() != 0) {
                row.setAvgRate(row.getTotalAmount().divide(row.getTotalQuantity(), 2, RoundingMode.HALF_UP));
            }
        }
        return new ArrayList<>(rows.values());
    }

    private BigDecimal nz(BigDecimal b) {
        return b == null ? BigDecimal.ZERO : b;
    }
}

