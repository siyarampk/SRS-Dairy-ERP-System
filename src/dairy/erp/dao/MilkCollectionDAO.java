package dairy.erp.dao;

import dairy.erp.database.DatabaseManager;
import dairy.erp.model.MilkCollection;
import dairy.erp.util.DateUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the milk_collection table.
 */
public class MilkCollectionDAO {

    private static final String SELECT_WITH_CUSTOMER =
            "SELECT mc.id, mc.collection_date, mc.customer_id, mc.milk_type, mc.shift, " +
            "mc.quantity, mc.fat, mc.snf, mc.rate_per_litre, mc.amount, mc.remarks, " +
            "mc.created_at, mc.updated_at, c.customer_code, c.customer_name " +
            "FROM milk_collection mc LEFT JOIN customers c ON c.id = mc.customer_id ";

    public int add(MilkCollection m) throws SQLException {
        String sql = "INSERT INTO milk_collection(collection_date, customer_id, milk_type, shift, " +
                "quantity, fat, snf, rate_per_litre, amount, remarks, created_at, updated_at) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, m);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public boolean update(MilkCollection m) throws SQLException {
        String sql = "UPDATE milk_collection SET collection_date = ?, customer_id = ?, milk_type = ?, " +
                "shift = ?, quantity = ?, fat = ?, snf = ?, rate_per_litre = ?, amount = ?, " +
                "remarks = ?, updated_at = datetime('now') WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, m);
            ps.setInt(11, m.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM milk_collection WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /** True when a record already exists for the same customer/date/shift. */
    public boolean existsDuplicate(LocalDate date, int customerId, String shift, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM milk_collection " +
                "WHERE collection_date = ? AND customer_id = ? AND shift = ? AND id <> ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, DateUtil.toDb(date));
            ps.setInt(2, customerId);
            ps.setString(3, shift);
            ps.setInt(4, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Inserts a batch of milk records inside a single transaction, skipping
     * duplicates. Returns a list of human-readable errors per skipped record.
     */
    public List<String> importBatch(List<MilkCollection> records) throws SQLException {
        List<String> errors = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int seq = 0;
                for (MilkCollection m : records) {
                    seq++;
                    if (existsDuplicate(conn, m.getCollectionDate(), m.getCustomerId(), m.getShift(), -1)) {
                        errors.add("Row " + seq + ": duplicate entry for customer/date/shift ("
                                + m.getCustomerCode() + ", " + m.getCollectionDate() + ", " + m.getShift() + ").");
                        continue;
                    }
                    insert(conn, m);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return errors;
    }

    private void insert(Connection conn, MilkCollection m) throws SQLException {
        String sql = "INSERT INTO milk_collection(collection_date, customer_id, milk_type, shift, " +
                "quantity, fat, snf, rate_per_litre, amount, remarks, created_at, updated_at) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, m);
            ps.executeUpdate();
        }
    }

    private boolean existsDuplicate(Connection conn, LocalDate date, int customerId, String shift, int excludeId)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM milk_collection " +
                "WHERE collection_date = ? AND customer_id = ? AND shift = ? AND id <> ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, DateUtil.toDb(date));
            ps.setInt(2, customerId);
            ps.setString(3, shift);
            ps.setInt(4, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }


    public List<MilkCollection> findByDateRange(LocalDate from, LocalDate to,
                                                Integer customerId, String milkType,
                                                String shift) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_WITH_CUSTOMER + " WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (from != null) {
            sql.append(" AND mc.collection_date >= ?");
            params.add(DateUtil.toDb(from));
        }
        if (to != null) {
            sql.append(" AND mc.collection_date <= ?");
            params.add(DateUtil.toDb(to));
        }
        if (customerId != null) {
            sql.append(" AND mc.customer_id = ?");
            params.add(customerId);
        }
        if (milkType != null && !milkType.isBlank()) {
            sql.append(" AND mc.milk_type = ?");
            params.add(milkType);
        }
        if (shift != null && !shift.isBlank()) {
            sql.append(" AND mc.shift = ?");
            params.add(shift);
        }
        sql.append(" ORDER BY mc.collection_date, mc.shift, c.customer_code");
        return query(sql.toString(), params);
    }

    public List<MilkCollection> findAll() throws SQLException {
        return query(SELECT_WITH_CUSTOMER + " ORDER BY mc.collection_date, mc.shift, c.customer_code",
                new ArrayList<>());
    }

    private List<MilkCollection> query(String sql, List<Object> params) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<MilkCollection> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        }
    }


    private void bind(PreparedStatement ps, MilkCollection m) throws SQLException {
        ps.setString(1, DateUtil.toDb(m.getCollectionDate()));
        ps.setInt(2, m.getCustomerId());
        ps.setString(3, m.getMilkType());
        ps.setString(4, m.getShift());
        ps.setBigDecimal(5, m.getQuantity() == null ? BigDecimal.ZERO : m.getQuantity());
        ps.setBigDecimal(6, m.getFat() == null ? BigDecimal.ZERO : m.getFat());
        ps.setBigDecimal(7, m.getSnf() == null ? BigDecimal.ZERO : m.getSnf());
        ps.setBigDecimal(8, m.getRatePerLitre() == null ? BigDecimal.ZERO : m.getRatePerLitre());
        ps.setBigDecimal(9, m.getAmount() == null ? BigDecimal.ZERO : m.getAmount());
        ps.setString(10, m.getRemarks());
    }

    private MilkCollection map(ResultSet rs) throws SQLException {
        MilkCollection m = new MilkCollection();
        m.setId(rs.getInt("id"));
        m.setCollectionDate(DateUtil.parseDb(rs.getString("collection_date")));
        m.setCustomerId(rs.getInt("customer_id"));
        m.setCustomerCode(rs.getString("customer_code"));
        m.setCustomerName(rs.getString("customer_name"));
        m.setMilkType(rs.getString("milk_type"));
        m.setShift(rs.getString("shift"));
        m.setQuantity(rs.getBigDecimal("quantity"));
        m.setFat(rs.getBigDecimal("fat"));
        m.setSnf(rs.getBigDecimal("snf"));
        m.setRatePerLitre(rs.getBigDecimal("rate_per_litre"));
        m.setAmount(rs.getBigDecimal("amount"));
        m.setRemarks(rs.getString("remarks"));
        m.setCreatedAt(rs.getString("created_at"));
        m.setUpdatedAt(rs.getString("updated_at"));
        return m;
    }
}

