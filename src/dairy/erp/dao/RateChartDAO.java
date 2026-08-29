package dairy.erp.dao;

import dairy.erp.database.DatabaseManager;
import dairy.erp.model.RateChart;
import dairy.erp.util.DateUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the rate_chart table.
 */
public class RateChartDAO {

    private static final String COLUMNS =
            "id, milk_type, fat_min, fat_max, snf_min, snf_max, rate_per_litre, " +
            "effective_from, effective_to, active, created_at, updated_at";

    public int add(RateChart r) throws SQLException {
        String sql = "INSERT INTO rate_chart(milk_type, fat_min, fat_max, snf_min, snf_max, " +
                "rate_per_litre, effective_from, effective_to, active, created_at, updated_at) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, r);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public boolean update(RateChart r) throws SQLException {
        String sql = "UPDATE rate_chart SET milk_type = ?, fat_min = ?, fat_max = ?, snf_min = ?, " +
                "snf_max = ?, rate_per_litre = ?, effective_from = ?, effective_to = ?, " +
                "active = ?, updated_at = datetime('now') WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, r);
            ps.setInt(10, r.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM rate_chart WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public List<RateChart> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM rate_chart ORDER BY milk_type, fat_min, snf_min";
        return query(sql);
    }

    public List<RateChart> findByMilkType(String milkType) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM rate_chart WHERE milk_type = ? " +
                "ORDER BY fat_min, snf_min";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, milkType);
            try (ResultSet rs = ps.executeQuery()) {
                return mapAll(rs);
            }
        }
    }

    /** Marks a single rule as active and all others inactive for the same milk type. */
    public void setActive(int id, boolean active) throws SQLException {
        if (active) {
            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // First deactivate all rules for the same milk type.
                    String deactivateSql = "UPDATE rate_chart SET active = 0, updated_at = datetime('now') " +
                            "WHERE milk_type = (SELECT milk_type FROM rate_chart WHERE id = ?) AND id <> ?";
                    try (PreparedStatement ps = conn.prepareStatement(deactivateSql)) {
                        ps.setInt(1, id);
                        ps.setInt(2, id);
                        ps.executeUpdate();
                    }
                    // Then activate the selected rule (only if it's being activated).
                    String activateSql = "UPDATE rate_chart SET active = ?, updated_at = datetime('now') WHERE id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(activateSql)) {
                        ps.setInt(1, active ? 1 : 0);
                        ps.setInt(2, id);
                        ps.executeUpdate();
                    }
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        } else {
            // Just deactivate this single rule (no need to touch others).
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE rate_chart SET active = 0, updated_at = datetime('now') WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        }
    }

    private List<RateChart> query(String sql) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return mapAll(rs);
        }
    }

    private List<RateChart> mapAll(ResultSet rs) throws SQLException {
        List<RateChart> list = new ArrayList<>();
        while (rs.next()) {
            list.add(map(rs));
        }
        return list;
    }

    private void bind(PreparedStatement ps, RateChart r) throws SQLException {
        ps.setString(1, r.getMilkType());
        ps.setBigDecimal(2, r.getFatMin());
        ps.setBigDecimal(3, r.getFatMax());
        ps.setBigDecimal(4, r.getSnfMin());
        ps.setBigDecimal(5, r.getSnfMax());
        ps.setBigDecimal(6, r.getRatePerLitre());
        ps.setString(7, DateUtil.toDb(r.getEffectiveFrom()));
        ps.setString(8, DateUtil.toDb(r.getEffectiveTo()));
        ps.setInt(9, r.isActive() ? 1 : 0);
    }

    private RateChart map(ResultSet rs) throws SQLException {
        RateChart r = new RateChart();
        r.setId(rs.getInt("id"));
        r.setMilkType(rs.getString("milk_type"));
        r.setFatMin(rs.getBigDecimal("fat_min"));
        r.setFatMax(rs.getBigDecimal("fat_max"));
        r.setSnfMin(rs.getBigDecimal("snf_min"));
        r.setSnfMax(rs.getBigDecimal("snf_max"));
        r.setRatePerLitre(rs.getBigDecimal("rate_per_litre"));
        r.setEffectiveFrom(DateUtil.parseDb(rs.getString("effective_from")));
        r.setEffectiveTo(DateUtil.parseDb(rs.getString("effective_to")));
        r.setActive(rs.getInt("active") == 1);
        r.setCreatedAt(rs.getString("created_at"));
        r.setUpdatedAt(rs.getString("updated_at"));
        return r;
    }
}
