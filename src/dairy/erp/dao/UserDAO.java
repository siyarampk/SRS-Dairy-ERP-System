package dairy.erp.dao;

import dairy.erp.database.DatabaseManager;
import dairy.erp.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data access for the users table. Small surface needed by the single-user
 * authentication flow (find by username, update password).
 */
public class UserDAO {

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password, created_at, updated_at FROM users WHERE username = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    u.setPasswordHash(rs.getString("password"));
                    u.setCreatedAt(rs.getString("created_at"));
                    u.setUpdatedAt(rs.getString("updated_at"));
                    return u;
                }
            }
        }
        return null;
    }

    public boolean updatePasswordHash(int userId, String newHash) throws SQLException {
        String sql = "UPDATE users SET password = ?, updated_at = datetime('now') WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }
}
