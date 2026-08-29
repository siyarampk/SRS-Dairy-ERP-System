package dairy.erp.service;

import dairy.erp.dao.RateChartDAO;
import dairy.erp.model.Customer;
import dairy.erp.model.RateChart;
import dairy.erp.util.LogUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Determines the applicable milk rate from the rate chart for a given milk
 * type, FAT and SNF. Exact FAT/SNF matches are preferred; a configurable
 * fallback finds the nearest FAT band for the same milk type. Leaving the
 * business rule here in one place makes it easy to change later.
 */
public class RateCalculationService {

    private static final Logger LOG = LogUtil.getLogger(RateCalculationService.class);
    private final RateChartDAO rateChartDAO = new RateChartDAO();

    /** Result of a rate lookup. */
    public static class RateResult {
        public final BigDecimal rate;
        public final String source;
        public final int rateChartId;

        public RateResult(BigDecimal rate, String source, int rateChartId) {
            this.rate = rate;
            this.source = source;
            this.rateChartId = rateChartId;
        }
    }

    /**
     * Finds the applicable rate for a milk type on a given date.
     * If no rate rule matches, returns null so the caller can decide.
     */
    public RateResult findRate(String milkType, BigDecimal fat, BigDecimal snf, LocalDate date) {
        if (milkType == null || fat == null || snf == null) {
            return null;
        }
        try {
            List<RateChart> rules = rateChartDAO.findByMilkType(milkType);
            if (rules.isEmpty()) {
                return null;
            }

            // 1) Exact band match (FAT and SNF within range), active or any effective.
            for (RateChart r : rules) {
                if (inRange(fat, r.getFatMin(), r.getFatMax())
                        && inRange(snf, r.getSnfMin(), r.getSnfMax())) {
                    return new RateResult(r.getRatePerLitre(), "EXACT_BAND", r.getId());
                }
            }

            // 2) Fallback: nearest FAT band for the same milk type.
            RateChart nearest = rules.stream()
                    .min(Comparator.comparing(r -> distance(fat, r.getFatMin(), r.getFatMax())))
                    .orElse(null);
            if (nearest != null) {
                return new RateResult(nearest.getRatePerLitre(), "NEAREST_FAT", nearest.getId());
            }
        } catch (SQLException e) {
            LOG.severe("Rate lookup failed: " + e.getMessage());
            throw new RuntimeException("Could not determine milk rate.", e);
        }
        return null;
    }

    private boolean inRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }

    private BigDecimal distance(BigDecimal fat, BigDecimal min, BigDecimal max) {
        BigDecimal midpoint = min.add(max).divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
        return fat.subtract(midpoint).abs();
    }
}
