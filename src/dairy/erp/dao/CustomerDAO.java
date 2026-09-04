package dairy.erp.dao;

import dairy.erp.database.DatabaseManager;
import dairy.erp.model.Customer;
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
 * Data access for the customers table using PreparedStatement exclusively.
 */
public class CustomerDAO {

    private static final String COLUMNS =
            "id, customer_code, customer_name, father_name, mobile, address, village, " +
            "milk_type, customer_type, status, opening_balance, registration_date, remarks, " +
            "created_at, updated_at";

    public int addCustomer(Customer c) throws SQLException {
        String sql = "INSERT INTO customers(customer_code, customer_name, father_name, mobile, " +
                "address, village, milk_type, customer_type, status, opening_balance, " +
                "registration_date, remarks, created_at, updated_at) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindInsert(ps, c);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public boolean updateCustomer(Customer c) throws SQLException {
        String sql = "UPDATE customers SET customer_name = ?, father_name = ?, mobile = ?, " +
                "address = ?, village = ?, milk_type = ?, customer_type = ?, status = ?, " +
                "opening_balance = ?, registration_date = ?, remarks = ?, updated_at = datetime('now') " +
                "WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindUpdate(ps, c);
            ps.setInt(12, c.getId());
            return ps.executeUpdate() > 0;
        }
    }

    /** Deletes a customer only when they have no related records (safe delete). */
    public boolean deleteCustomer(int id) throws SQLException {
        String sql = "DELETE FROM customers WHERE id = ? AND NOT EXISTS " +
                "(SELECT 1 FROM milk_collection WHERE customer_id = ?) " +
                "AND NOT EXISTS (SELECT 1 FROM payments WHERE customer_id = ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, id);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean markInactive(int id, boolean inactive) throws SQLException {
        String sql = "UPDATE customers SET status = ?, updated_at = datetime('now') WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, inactive ? "Inactive" : "Active");
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        }
    }

    public Customer findById(int id) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT " + COLUMNS + " FROM customers WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Customer findByCode(String code) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT " + COLUMNS + " FROM customers WHERE customer_code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Customer> findAll() throws SQLException {
        return query("SELECT " + COLUMNS + " FROM customers ORDER BY customer_code");
    }

    public List<Customer> search(String code, String name, String mobile, String status) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM customers WHERE 1=1");
        List<Object> params = new ArrayList<>();
        appendLike(sql, params, "customer_code", code);
        appendLike(sql, params, "customer_name", name);
        appendLike(sql, params, "mobile", mobile);
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY customer_code");
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, params.get(i).toString());
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Customer> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        }
    }

    public String lastCustomerCode() throws SQLException {
        // Highest numeric code so far — immune to out-of-order insertion or
        // trailing non-numeric codes; codes are never reused.
        String sql = "SELECT customer_code FROM customers " +
                "WHERE TRIM(customer_code) GLOB '[0-9]*' " +
                "ORDER BY CAST(TRIM(customer_code) AS INTEGER) DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private List<Customer> query(String sql) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Customer> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        }
    }

    private void appendLike(StringBuilder sql, List<Object> params, String column, String value) {
        if (value != null && !value.isBlank()) {
            sql.append(" AND ").append(column).append(" LIKE ?");
            params.add("%" + value.trim() + "%");
        }
    }


    /** Binds the 12 insert parameters in INSERT column order (code first). */
    private void bindInsert(PreparedStatement ps, Customer c) throws SQLException {
        ps.setString(1, c.getCustomerCode());
        ps.setString(2, c.getCustomerName());
        ps.setString(3, c.getFatherName());
        ps.setString(4, c.getMobile());
        ps.setString(5, c.getAddress());
        ps.setString(6, c.getVillage());
        ps.setString(7, c.getMilkType());
        ps.setString(8, c.getCustomerType());
        ps.setString(9, c.getStatus());
        ps.setBigDecimal(10, c.getOpeningBalance() == null ? BigDecimal.ZERO : c.getOpeningBalance());
        ps.setString(11, DateUtil.toDb(c.getRegistrationDate()));
        ps.setString(12, c.getRemarks());
    }

    /** Binds the 11 update parameters (no customer code); caller sets id. */
    private void bindUpdate(PreparedStatement ps, Customer c) throws SQLException {
        ps.setString(1, c.getCustomerName());
        ps.setString(2, c.getFatherName());
        ps.setString(3, c.getMobile());
        ps.setString(4, c.getAddress());
        ps.setString(5, c.getVillage());
        ps.setString(6, c.getMilkType());
        ps.setString(7, c.getCustomerType());
        ps.setString(8, c.getStatus());
        ps.setBigDecimal(9, c.getOpeningBalance() == null ? BigDecimal.ZERO : c.getOpeningBalance());
        ps.setString(10, DateUtil.toDb(c.getRegistrationDate()));
        ps.setString(11, c.getRemarks());
    }

    private Customer map(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("id"));
        c.setCustomerCode(rs.getString("customer_code"));
        c.setCustomerName(rs.getString("customer_name"));
        c.setFatherName(rs.getString("father_name"));
        c.setMobile(rs.getString("mobile"));
        c.setAddress(rs.getString("address"));
        c.setVillage(rs.getString("village"));
        c.setMilkType(rs.getString("milk_type"));
        c.setCustomerType(rs.getString("customer_type"));
        c.setStatus(rs.getString("status"));
        c.setOpeningBalance(rs.getBigDecimal("opening_balance"));
        c.setRegistrationDate(DateUtil.parseDb(rs.getString("registration_date")));
        c.setRemarks(rs.getString("remarks"));
        c.setCreatedAt(rs.getString("created_at"));
        c.setUpdatedAt(rs.getString("updated_at"));
        return c;
    }
}

