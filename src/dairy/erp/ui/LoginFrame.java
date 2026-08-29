package dairy.erp.ui;

import dairy.erp.config.AppConfig;
import dairy.erp.service.AuthenticationService;
import dairy.erp.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Consumer;

/**
 * Application login screen. Validates credentials via AuthenticationService and
 * invokes the {@code onLogin} callback on success so the controller can open
 * the main dashboard.
 */
public class LoginFrame extends JFrame {

    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final AuthenticationService authenticationService = new AuthenticationService();
    private Consumer<String> onLogin;

    public LoginFrame() {
        super(AppConfig.APP_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildUi();
        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void buildUi() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);

        javax.swing.ImageIcon logo = UIUtil.loadLogoByHeight(120, 400);
        if (logo != null) {
            JLabel logoLabel = new JLabel(logo);
            logoLabel.setHorizontalAlignment(JLabel.CENTER);
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            panel.add(logoLabel, gbc);
        }

        JLabel title = new JLabel("Dairy ERP");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setHorizontalAlignment(JLabel.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(title, gbc);

        JLabel subtitle = new JLabel("Milk Collection Management System");
        subtitle.setHorizontalAlignment(JLabel.CENTER);
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(subtitle, gbc);

        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        panel.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        // Pre-filled test credentials (from AppConfig) so the username and
        // password don't have to be typed on every launch during testing.
        usernameField.setText(AppConfig.DEFAULT_ADMIN_USER);
        passwordField.setText(AppConfig.DEFAULT_ADMIN_PASS);

        JButton loginButton = new JButton("Login");
        JButton exitButton = new JButton("Exit");
        UIUtil.styleButton(loginButton);
        UIUtil.styleButton(exitButton);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        buttonPanel.add(loginButton);
        buttonPanel.add(exitButton);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(buttonPanel, gbc);

        add(panel);
        getRootPane().setDefaultButton(loginButton);
        usernameField.requestFocusInWindow();

        loginButton.addActionListener(e -> attemptLogin());
        exitButton.addActionListener(e -> System.exit(0));
        passwordField.addActionListener(e -> attemptLogin());
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
    }

    private void attemptLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        if (username.isBlank() || password.isBlank()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both username and password.",
                    "Login", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            String authenticated = authenticationService.authenticate(username, password);
            if (authenticated == null) {
                JOptionPane.showMessageDialog(this,
                        "Invalid username or password.",
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
                passwordField.setText("");
                passwordField.requestFocusInWindow();
            } else if (onLogin != null) {
                dispose();
                onLogin.accept(authenticated);
            }
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this,
                    "Login could not be completed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Registers the callback invoked after successful login. */
    public void setOnLogin(Consumer<String> onLogin) {
        this.onLogin = onLogin;
    }
}
