package dairy.erp.dao;

import dairy.erp.database.DatabaseManager;
import dairy.erp.model.Payment;
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
 * Data access for the payments table.
 */
public class PaymentDAO {

    private static final String SELECT_WITH_CUSTOMER =
            "SELECT p.id, p.customer_id, p.payment_date, p.amount, p.payment_mode, " +
            "p.reference, p.remarks, p.created_at, p.updated_at, c.customer_code, c.customer_name " +
            "FROM payments p LEFT JOIN customers c ON c.id = p.customer_id ";

    public int add(Payment p) throws SQLException {
        String sql = "INSERT INTO payments(customer_id, payment_date, amount, payment_mode, " +
                "reference, remarks, created_at, updated_at) " +
                "VALUES(?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getCustomerId());
            ps.setString(2, DateUtil.toDb(p.getPaymentDate()));
            ps.setBigDecimal(3, p.getAmount() == null ? BigDecimal.ZERO : p.getAmount());
            ps.setString(4, p.getPaymentMode());
            ps.setString(5, p.getReference());
            ps.setString(6, p.getRemarks());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public boolean update(Payment p) throws SQLException {
        String sql = "UPDATE payments SET customer_id = ?, payment_date = ?, amount = ?, " +
                "payment_mode = ?, reference = ?, remarks = ?, updated_at = datetime('now') WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getCustomerId());
            ps.setString(2, DateUtil.toDb(p.getPaymentDate()));
            ps.setBigDecimal(3, p.getAmount() == null ? BigDecimal.ZERO : p.getAmount());
            ps.setString(4, p.getPaymentMode());
            ps.setString(5, p.getReference());
            ps.setString(6, p.getRemarks());
            ps.setInt(7, p.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM payments WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Payment> findByCustomerDateRange(int customerId, LocalDate from, LocalDate to)
            throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_WITH_CUSTOMER + " WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (customerId > 0) {
            sql.append(" AND p.customer_id = ?");
            params.add(customerId);
        }
        if (from != null) {
            sql.append(" AND p.payment_date >= ?");
            params.add(DateUtil.toDb(from));
        }
        if (to != null) {
            sql.append(" AND p.payment_date <= ?");
            params.add(DateUtil.toDb(to));
        }
        sql.append(" ORDER BY p.payment_date");
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Payment> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        }
    }

    public List<Payment> findAll(LocalDate from, LocalDate to) throws SQLException {
        return findByCustomerDateRange(-1, from, to);
    }

    private Payment map(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setId(rs.getInt("id"));
        p.setCustomerId(rs.getInt("customer_id"));
        p.setCustomerCode(rs.getString("customer_code"));
        p.setCustomerName(rs.getString("customer_name"));
        p.setPaymentDate(DateUtil.parseDb(rs.getString("payment_date")));
        p.setAmount(rs.getBigDecimal("amount"));
        p.setPaymentMode(rs.getString("payment_mode"));
        p.setReference(rs.getString("reference"));
        p.setRemarks(rs.getString("remarks"));
        p.setCreatedAt(rs.getString("created_at"));
        p.setUpdatedAt(rs.getString("updated_at"));
        return p;
    }
}
