package dairy.erp.dao;

import dairy.erp.database.DatabaseManager;
import dairy.erp.model.ApplicationSetting;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Data access for the settings table (key/value application settings).
 */
public class SettingsDAO {

    /** Loads all settings into a map keyed by setting_key. */
    public Map<String, String> loadAll() throws SQLException {
        Map<String, String> map = new LinkedHashMap<>();
        String sql = "SELECT setting_key, setting_value FROM settings";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString(1), rs.getString(2));
            }
        }
        return map;
    }

    public String get(String key) throws SQLException {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** Inserts or updates a single setting. */
    public void put(String key, String value) throws SQLException {
        String sql = "INSERT INTO settings(setting_key, setting_value) VALUES(?, ?) " +
                "ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }
}
