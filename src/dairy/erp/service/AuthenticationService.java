package dairy.erp.service;

import dairy.erp.dao.UserDAO;
import dairy.erp.model.User;
import dairy.erp.util.HashUtil;

import java.sql.SQLException;

/**
 * Business logic for authentication: login validation and password change.
 * Passwords are stored only as salted hashes.
 */
public class AuthenticationService {

    private final UserDAO userDAO = new UserDAO();

    /**
     * Validates credentials.
     * @return the authenticated username, or {@code null} when invalid
     */
    public String authenticate(String username, String password) {
        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            return null;
        }
        try {
            User user = userDAO.findByUsername(username.trim());
            if (user != null && HashUtil.verifyPassword(password, user.getPasswordHash())) {
                return user.getUsername();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not verify login credentials.", e);
        }
        return null;
    }

    /**
     * Changes the admin password after verifying the current password.
     * @return an empty string on success, otherwise a message describing the error
     */
    public String changePassword(String username, String currentPassword,
                                 String newPassword, String confirmPassword) {
        try {
            User user = userDAO.findByUsername(username);
            if (user == null) {
                return "User not found.";
            }
            if (currentPassword == null || !HashUtil.verifyPassword(currentPassword, user.getPasswordHash())) {
                return "Current password is incorrect.";
            }
            if (newPassword == null || newPassword.length() < 5) {
                return "New password must be at least 5 characters.";
            }
            if (!newPassword.equals(confirmPassword)) {
                return "New password and confirmation do not match.";
            }
            String newHash = HashUtil.hashPassword(newPassword);
            userDAO.updatePasswordHash(user.getId(), newHash);
            return "";
        } catch (SQLException e) {
            throw new RuntimeException("Could not change password.", e);
        }
    }
}
