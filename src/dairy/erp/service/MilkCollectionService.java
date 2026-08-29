package dairy.erp.service;

import dairy.erp.dao.CustomerDAO;
import dairy.erp.dao.MilkCollectionDAO;
import dairy.erp.model.Customer;
import dairy.erp.model.MilkCollection;
import dairy.erp.util.CurrencyUtil;
import dairy.erp.util.LogUtil;
import dairy.erp.util.ValidationUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

/**
 * Business logic for milk collection: validation, automatic rate/amount
 * calculation and persistence. Rate is snapshotted into the record so later
 * rate-chart changes never alter historical transactions.
 */
public class MilkCollectionService {

    private static final Logger LOG = LogUtil.getLogger(MilkCollectionService.class);

    private final MilkCollectionDAO milkCollectionDAO = new MilkCollectionDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final RateCalculationService rateCalculationService = new RateCalculationService();
    private final SettingsService settingsService = new SettingsService();

    /**
     * Resolves a customer by code for the milk entry screen.
     */
    public Customer findCustomerByCode(String code) {
        try {
            return customerDAO.findByCode(code);
        } catch (SQLException e) {
            LOG.severe("Customer lookup failed: " + e.getMessage());
            throw new RuntimeException("Could not find customer.", e);
        }
    }

    public boolean existsDuplicate(LocalDate date, int customerId, String shift, int excludeId) {
        try {
            return milkCollectionDAO.existsDuplicate(date, customerId, shift, excludeId);
        } catch (SQLException e) {
            LOG.severe("Duplicate check failed: " + e.getMessage());
            throw new RuntimeException("Could not check for duplicate entry.", e);
        }
    }

    /**
     * Calculates the rate and amount for a milk entry based on the customer,
     * quantity, FAT and SNF. Returns the resulting MilkCollection (not saved).
     *
     * @return a result object with the calculated collection plus rate source info,
     *         or {@code null} when the rate could not be determined.
     */
    public CalculationResult calculate(Customer customer, LocalDate date, String shift,
                                       String milkType, BigDecimal quantity,
                                       BigDecimal fat, BigDecimal snf,
                                       boolean manualOverride, BigDecimal manualRate) {
        BigDecimal rate;
        String source;
        String collectionMilkType = (milkType == null || milkType.isBlank())
                ? (customer == null ? null : customer.getMilkType()) : milkType;
        if (manualOverride && manualRate != null && manualRate.signum() > 0) {
            rate = manualRate;
            source = "MANUAL";
        } else {
            RateCalculationService.RateResult rr =
                    rateCalculationService.findRate(collectionMilkType, fat, snf, date);
            if (rr == null) {
                return null;
            }
            rate = rr.rate;
            source = rr.source;
        }
        BigDecimal amount = quantity.multiply(rate);
        amount = CurrencyUtil.round(amount);

        MilkCollection mc = new MilkCollection();
        mc.setCollectionDate(date);
        mc.setCustomerId(customer.getId());
        mc.setCustomerCode(customer.getCustomerCode());
        mc.setCustomerName(customer.getCustomerName());
        mc.setMilkType(collectionMilkType);
        mc.setShift(shift);
        mc.setQuantity(quantity);
        mc.setFat(fat);
        mc.setSnf(snf);
        mc.setRatePerLitre(rate);
        mc.setAmount(amount);
        return new CalculationResult(mc, rate, source);
    }

    public int add(MilkCollection m) {
        try {
            return milkCollectionDAO.add(m);
        } catch (SQLException e) {
            LOG.severe("Add milk collection failed: " + e.getMessage());
            throw new RuntimeException("Could not save milk collection: " + e.getMessage(), e);
        }
    }

    public void update(MilkCollection m) {
        try {
            milkCollectionDAO.update(m);
        } catch (SQLException e) {
            LOG.severe("Update milk collection failed: " + e.getMessage());
            throw new RuntimeException("Could not update milk collection: " + e.getMessage(), e);
        }
    }

    public void delete(int id) {
        try {
            milkCollectionDAO.delete(id);
        } catch (SQLException e) {
            LOG.severe("Delete milk collection failed: " + e.getMessage());
            throw new RuntimeException("Could not delete milk collection.", e);
        }
    }

    public List<MilkCollection> search(LocalDate from, LocalDate to, Integer customerId,
                                       String milkType, String shift) {
        try {
            return milkCollectionDAO.findByDateRange(from, to, customerId, milkType, shift);
        } catch (SQLException e) {
            LOG.severe("Milk collection search failed: " + e.getMessage());
            throw new RuntimeException("Could not load milk collection records.", e);
        }
    }

    public List<MilkCollection> all() {
        try {
            return milkCollectionDAO.findAll();
        } catch (SQLException e) {
            LOG.severe("Load milk collection failed: " + e.getMessage());
            throw new RuntimeException("Could not load milk collection records.", e);
        }
    }

    /** Imports validated milk records in a transaction; returns per-row skip errors. */
    public List<String> importMilk(List<MilkCollection> records) {
        try {
            return milkCollectionDAO.importBatch(records);
        } catch (SQLException e) {
            LOG.severe("Import milk collection failed: " + e.getMessage());
            throw new RuntimeException("Import failed: " + e.getMessage(), e);
        }
    }

    /** Nested result carrying the calculated record and rate source. */
    public static class CalculationResult {
        public final MilkCollection collection;
        public final BigDecimal rate;
        public final String source;

        public CalculationResult(MilkCollection collection, BigDecimal rate, String source) {
            this.collection = collection;
            this.rate = rate;
            this.source = source;
        }
    }
}
