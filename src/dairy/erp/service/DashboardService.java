package dairy.erp.service;

import dairy.erp.database.DatabaseManager;
import dairy.erp.model.DashboardSummary;
import dairy.erp.util.LogUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.logging.Logger;

/**
 * Produces the aggregate figures shown on the daily dashboard by querying
 * today's milk collection records from the database.
 */
public class DashboardService {

    private static final Logger LOG = LogUtil.getLogger(DashboardService.class);

    public static final String SHIFT_MORNING = "Morning";
    public static final String SHIFT_EVENING = "Evening";
    public static final String TYPE_COW = "Cow";
    public static final String TYPE_BUFFALO = "Buffalo";
    public static final String TYPE_MIX = "Mix";

    public DashboardSummary todaySummary(LocalDate date) {
        DashboardSummary summary = new DashboardSummary();
        summary.setDate(date);
        String sql =
                "SELECT milk_type, shift, " +
                "  COUNT(DISTINCT customer_id) AS customers, " +
                "  COALESCE(SUM(quantity),0) AS qty, " +
                "  COALESCE(SUM(amount),0) AS amt, " +
                "  COALESCE(SUM(quantity * fat),0) AS fatW, " +
                "  COALESCE(SUM(quantity * snf),0) AS snfW " +
                "FROM milk_collection WHERE collection_date = ? GROUP BY milk_type, shift";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                BigDecimal totalQty = BigDecimal.ZERO;
                BigDecimal totalAmt = BigDecimal.ZERO;
                BigDecimal fatW = BigDecimal.ZERO;
                BigDecimal snfW = BigDecimal.ZERO;
                long customers = 0;
                while (rs.next()) {
                    String type = rs.getString("milk_type") == null ? "" : rs.getString("milk_type");
                    String shift = rs.getString("shift") == null ? "" : rs.getString("shift");
                    customers += rs.getLong("customers");
                    BigDecimal qty = rs.getBigDecimal("qty");
                    BigDecimal amt = rs.getBigDecimal("amt");
                    totalQty = totalQty.add(qty == null ? BigDecimal.ZERO : qty);
                    totalAmt = totalAmt.add(amt == null ? BigDecimal.ZERO : amt);
                    fatW = fatW.add(rs.getBigDecimal("fatW") == null ? BigDecimal.ZERO : rs.getBigDecimal("fatW"));
                    snfW = snfW.add(rs.getBigDecimal("snfW") == null ? BigDecimal.ZERO : rs.getBigDecimal("snfW"));

                    BigDecimal q = qty == null ? BigDecimal.ZERO : qty;
                    if (SHIFT_MORNING.equals(shift)) {
                        summary.setMorningQty(summary.getMorningQty().add(q));
                    } else if (SHIFT_EVENING.equals(shift)) {
                        summary.setEveningQty(summary.getEveningQty().add(q));
                    }
                    if (TYPE_COW.equalsIgnoreCase(type)) {
                        summary.setCowQty(summary.getCowQty().add(q));
                    } else if (TYPE_BUFFALO.equalsIgnoreCase(type)) {
                        summary.setBuffaloQty(summary.getBuffaloQty().add(q));
                    } else if (TYPE_MIX.equalsIgnoreCase(type)) {
                        summary.setMixQty(summary.getMixQty().add(q));
                    }
                }
                summary.setTotalCustomers(customers);
                summary.setTotalQuantity(totalQty);
                summary.setTotalAmount(totalAmt);
                summary.setAvgFat(weightedAverage(fatW, totalQty));
                summary.setAvgSnf(weightedAverage(snfW, totalQty));
            }
        } catch (SQLException e) {
            LOG.severe("Dashboard query failed: " + e.getMessage());
            throw new RuntimeException("Could not load dashboard summary.", e);
        }
        return summary;
    }

    /**
     * Total milk-collection earnings per day for the seven days ending on
     * (and including) {@code endDate}. Days without collections are included
     * with zero so the chart always shows a full week.
     */
    public java.util.Map<LocalDate, BigDecimal> dailyEarningsLast7Days(LocalDate endDate) {
        LocalDate start = endDate.minusDays(6);
        String sql = "SELECT collection_date, COALESCE(SUM(amount),0) AS amt "
                + "FROM milk_collection WHERE collection_date BETWEEN ? AND ? "
                + "GROUP BY collection_date";
        java.util.Map<LocalDate, BigDecimal> found = new java.util.HashMap<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, start.toString());
            ps.setString(2, endDate.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        found.put(LocalDate.parse(rs.getString("collection_date")),
                                rs.getBigDecimal("amt"));
                    } catch (Exception parse) {
                        LOG.warning("Skipping unparseable collection_date row: "
                                + parse.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            LOG.severe("Daily earnings query failed: " + e.getMessage());
            throw new RuntimeException("Could not load daily earnings.", e);
        }
        java.util.Map<LocalDate, BigDecimal> result = new java.util.LinkedHashMap<>();
        for (LocalDate d = start; !d.isAfter(endDate); d = d.plusDays(1)) {
            result.put(d, found.getOrDefault(d, BigDecimal.ZERO));
        }
        return result;
    }

    private BigDecimal weightedAverage(BigDecimal weightedSum, BigDecimal totalQty) {
        if (totalQty == null || totalQty.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return weightedSum.divide(totalQty, 2, RoundingMode.HALF_UP);
    }
}
