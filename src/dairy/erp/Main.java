package dairy.erp;

import dairy.erp.config.AppConfig;
import dairy.erp.database.DatabaseInitializer;
import dairy.erp.service.SettingsService;
import dairy.erp.ui.LoginFrame;
import dairy.erp.ui.MainFrame;
import dairy.erp.util.LogUtil;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.logging.Logger;

/**
 * Application entry point for Dairy ERP.
 * <p>
 * Startup sequence:
 * <ol>
 *   <li>Initialise logging to {@code logs/dairy-erp.log}.</li>
 *   <li>Initialise the SQLite database (folders, schema, baseline data).</li>
 *   <li>Show the login screen; on success open the main dashboard.</li>
 * </ol>
 * <p>
 * Pass {@code --init} as the first argument to run only the database
 * initialisation and exit (useful for CLI verification / CI).
 */
public class Main {

    private static final Logger LOG = LogUtil.getLogger(Main.class);

    public static void main(String[] args) {
        boolean initOnly = args.length > 0 && "--init".equalsIgnoreCase(args[0]);
        initializeFoundation();
        startApplication(initOnly);
    }

    private static void initializeFoundation() {
        LogUtil.init(AppConfig.getLogDir().toFile(), "dairy-erp.log");
        LOG.info("Starting " + AppConfig.APP_NAME + " version " + AppConfig.APP_VERSION);
    }

    private static void startApplication(boolean initOnly) {
        try {
            boolean isNew = DatabaseInitializer.initialize();
            LOG.info("Database initialised successfully. New database = " + isNew);
            if (initOnly) {
                System.out.println("INIT_OK new=" + isNew
                        + " db=" + AppConfig.getDatabasePath());
                return;
            }
            // Apply display configuration (currency symbol, decimal places) saved in Settings,
            // so custom values persist across restarts (not just during a save).
            new SettingsService().applyDisplayConfig();
            // Apply a uniform 18px theme and larger buttons to every panel.
            dairy.erp.util.UIUtil.applyGlobalFont();
            SwingUtilities.invokeLater(Main::showLogin);
        } catch (Exception e) {
            LOG.severe("Application failed to initialise: " + e.getMessage());
            if (initOnly) {
                System.err.println("INIT_FAIL: " + e.getMessage());
                System.exit(1);
            }
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Failed to initialise the application:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void showLogin() {
        LOG.info("Showing login screen.");
        LoginFrame login = new LoginFrame();
        login.setOnLogin(Main::onLoginSuccess);
        login.setVisible(true);
    }

    private static void onLoginSuccess(String username) {
        LOG.info("Login successful: " + username);
        try {
            MainFrame mainFrame = new MainFrame(username, Main::showLogin);
            mainFrame.setVisible(true);
        } catch (RuntimeException ex) {
            LOG.severe("MainFrame creation failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}

