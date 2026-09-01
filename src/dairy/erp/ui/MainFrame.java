package dairy.erp.ui;

import dairy.erp.config.AppConfig;
import dairy.erp.ui.dialogs.PasswordChangeDialog;
import dairy.erp.util.AppBus;
import dairy.erp.util.Theme;
import dairy.erp.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Main application window shown after login. Provides the full ERP menu,
 * F1/F2/F3 function-key shortcuts (via Swing keyboard bindings), a status bar
 * and a CardLayout for switching between the dashboard and management panels.
 */
public class MainFrame extends JFrame {

    private final String username;
    private final Runnable onLogout;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final DashboardPanel dashboardPanel;
    private final Map<String, String> panelNames = new HashMap<>();
    private final JLabel statusLabel = new JLabel();
    private JLabel menuLabel = new JLabel();
    private javax.swing.JMenuBar menuBar;
    private JPanel menuTitleBar;

    public MainFrame(String username, Runnable onLogout) {
        super(AppConfig.APP_NAME);
        this.username = username;
        this.onLogout = onLogout;
        dashboardPanel = new DashboardPanel(this::onDashboardAction);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                confirmExit();
            }
        });
        setSize(1180, 800);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);
        buildMenu();
        buildContent();
        installFunctionKeys();
        showDashboard();
        // Translate every label, button, menu and tab of the whole window
        // into the language chosen on the login screen (English / Hindi).
        dairy.erp.util.I18n.apply(this);

        // Re-colour the frame chrome whenever the theme changes.
        AppBus.onThemeChanged(this::applyTheme);
    }

    /** Applies the given theme's colours to the menu bar and status bar. */
    private void applyTheme(Theme theme) {
        if (menuBar != null) {
            menuBar.setBackground(theme.getBrand());
            menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, theme.getBrandDark()));
        }
        if (menuTitleBar != null) {
            menuTitleBar.setBackground(theme.getBrand());
        }
        if (statusLabel != null) {
            statusLabel.getParent().setBackground(theme.getTint());
        }
        getRootPane().repaint();
    }


    // ---- content / navigation ----

    private void registerPanel(String key, String title, JPanel panel) {
        cardPanel.add(panel, key);
        panelNames.put(key, title);
    }

    private void buildContent() {
        registerPanel("dashboard", "Dashboard", dashboardPanel);
        registerPanel("customers", "Customer Management", new CustomerPanel());
        registerPanel("ratechart", "Rate Chart", new RateChartPanel());
        registerPanel("milkentry", "Milk Collection", new MilkCollectionPanel());
        registerPanel("payments", "Payments", new PaymentPanel());
        registerPanel("ledger", "Customer Ledger", new CustomerLedgerPanel(username));
        registerPanel("reports", "Reports", new ReportsPanel(username));
        registerPanel("settings", "Settings", new SettingsPanel(username));
        registerPanel("data", "Import / Export / Backup", new ImportExportPanel(username));

        add(cardPanel, BorderLayout.CENTER);

        // ---- Current menu title bar ----
        menuTitleBar = new JPanel(new BorderLayout());
        menuTitleBar.setBackground(UIUtil.BRAND);
        menuTitleBar.setBorder(BorderFactory.createEmptyBorder(6, 15, 6, 15));
        menuLabel = new JLabel("Dashboard");
        menuLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
        menuLabel.setForeground(java.awt.Color.WHITE);
        menuTitleBar.add(menuLabel, BorderLayout.WEST);
        add(menuTitleBar, BorderLayout.NORTH);

        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(new Color(0xec, 0xf0, 0xf3));
        status.setOpaque(true);
        status.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xd8, 0xde, 0xe4)));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        left.setOpaque(false);
        left.add(new JLabel("User: " + username));
        left.add(new JLabel("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
        status.add(left, BorderLayout.WEST);
        status.add(statusLabel, BorderLayout.EAST);
        add(status, BorderLayout.SOUTH);
    }

    private void showPanel(String key) {
        showPanel(key, "new");
    }

    /**
     * Opens a panel, optionally in a sub-mode: "new" (entry form) or
     * "history" (records). The panel header and action buttons follow the
     * mode (e.g. New Collection vs Collection History).
     */
    private void showPanel(String key, String mode) {
        String title = dairy.erp.util.I18n.t(panelNames.getOrDefault(key, "Dashboard"));
        if ("dashboard".equals(key)) {
            dashboardPanel.refresh();
        } else if ("customers".equals(key)) {
            // Reset customer form to blank mode every time the screen is opened
            ((CustomerPanel) cardPanel.getComponent(1)).resetForNewEntry();
        } else if ("milkentry".equals(key)) {
            MilkCollectionPanel milk = (MilkCollectionPanel) cardPanel.getComponent(3);
            milk.setMode(mode);
            if ("new".equals(mode)) {
                milk.resetForm();
            }
        } else if ("payments".equals(key)) {
            PaymentPanel payments = (PaymentPanel) cardPanel.getComponent(4);
            payments.setMode(mode);
            if ("new".equals(mode)) {
                payments.resetForm();
            }
        } else if ("ratechart".equals(key)) {
            ((RateChartPanel) cardPanel.getComponent(2)).resetForNewEntry();
        }

        cardLayout.show(cardPanel, key);

        // Mode-aware heading (e.g. "New Payment" vs "Payment History"): the
        // window title, status bar and the blue banner must all follow the
        // selected menu item, not just the panel name.
        String heading = title;
        if ("milkentry".equals(key)) {
            heading = "history".equals(mode) ? "Collection History" : "New Collection";
        } else if ("payments".equals(key)) {
            heading = "history".equals(mode) ? "Payment History" : "New Payment";
        }
        heading = dairy.erp.util.I18n.t(heading);

        setTitle(AppConfig.APP_NAME + "  — " + heading);
        statusLabel.setText(heading);

        // The blue banner below the menu bar shows the exact menu item the
        // user selected (e.g. "Collection History", "New Payment").
        menuLabel.setText(heading);
    }

    private void showDashboard() {
        showPanel("dashboard");
    }

    private void onDashboardAction(String action) {
        switch (action) {
            case "New Milk Entry": showPanel("milkentry"); break;
            case "Customers": showPanel("customers"); break;
            case "Rate Chart": showPanel("ratechart"); break;
            case "Today's Collection": showPanel("reports"); break;
            case "Customer Report": showPanel("reports"); break;
            case "Monthly Report": showPanel("reports"); break;
            case "Payments": showPanel("payments"); break;
            case "Backup": showPanel("data"); break;
            default: showDashboard();
        }
    }

    // ---- menu ----

    private void buildMenu() {
        JMenuBar bar = new JMenuBar();
        // Brand-themed menu bar: teal background with white menu titles.
        bar.setBackground(UIUtil.BRAND);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIUtil.BRAND.darker()));
        menuBar = bar;

        JMenu fileMenu = new JMenu("File");
        JMenuItem logoutItem = new JMenuItem("Logout");
        logoutItem.addActionListener(e -> performLogout());
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> confirmExit());
        fileMenu.add(logoutItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        bar.add(fileMenu);

        JMenu masterMenu = new JMenu("Master");
        bar.add(masterMenu);
        masterMenu.add(menuItem("Dashboard", KeyEvent.VK_F1, e -> showPanel("dashboard")));
        masterMenu.addSeparator();
        masterMenu.add(menuItem("Customer Management", KeyEvent.VK_F2, e -> showPanel("customers")));
        masterMenu.add(menuItem("Rate Chart", KeyEvent.VK_F3, e -> showPanel("ratechart")));
        masterMenu.add(menuItem("Settings", KeyEvent.VK_F9, e -> showPanel("settings")));

        JMenu milkMenu = new JMenu("Milk Collection");
        bar.add(milkMenu);
        milkMenu.add(menuItem("New Collection", KeyEvent.VK_F4, e -> showPanel("milkentry", "new")));
        milkMenu.add(menuItem("Collection History", 0, e -> showPanel("milkentry", "history")));

        JMenu paymentMenu = new JMenu("Payments");
        bar.add(paymentMenu);
        paymentMenu.add(menuItem("New Payment", KeyEvent.VK_F5, e -> showPanel("payments", "new")));
        paymentMenu.add(menuItem("Payment History", 0, e -> showPanel("payments", "history")));
        paymentMenu.add(menuItem("Customer Ledger", KeyEvent.VK_F6, e -> showPanel("ledger")));

        JMenu reportMenu = new JMenu("Reports");
        bar.add(reportMenu);
        // Only one menu item — every report type opens the same Reports screen,
        // where all report types are chosen from a single dropdown.
        reportMenu.add(menuItem("Open Reports", KeyEvent.VK_F7, e -> showPanel("reports")));

        JMenu dataMenu = new JMenu("Data");
        bar.add(dataMenu);
        dataMenu.add(menuItem("Import CSV", 0, e -> showPanel("data")));
        dataMenu.add(menuItem("Export CSV", 0, e -> showPanel("data")));
        dataMenu.addSeparator();
        dataMenu.add(menuItem("Backup Database", KeyEvent.VK_F8, e -> showPanel("data")));
        dataMenu.add(menuItem("Restore Database", 0, e -> showPanel("data")));

        JMenu helpMenu = new JMenu("Help");
        bar.add(helpMenu);
        JMenuItem changePasswordItem = new JMenuItem("Change Password");
        changePasswordItem.addActionListener(e -> new PasswordChangeDialog(this, username).setVisible(true));
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(changePasswordItem);
        helpMenu.addSeparator();
        helpMenu.add(aboutItem);

        // White menu titles on the teal bar (applied after all menus are added).
        for (int i = 0; i < bar.getMenuCount(); i++) {
            JMenu m = bar.getMenu(i);
            if (m != null) {
                m.setForeground(Color.WHITE);
                m.setOpaque(false);
            }
        }

        setJMenuBar(bar);
    }

    private JMenuItem menuItem(String text, int keyEvent, java.awt.event.ActionListener listener) {
        JMenuItem item = new JMenuItem(text);
        if (keyEvent != 0) {
            // Function keys (F1-F12) are safe as unmodified shortcuts because
            // they never generate text input. Letter shortcuts must be combined
            // with the platform menu-shortcut modifier (Ctrl on Windows/Linux,
            // Cmd on macOS) so they never fire while typing in a text field.
            boolean isFunctionKey = keyEvent >= KeyEvent.VK_F1 && keyEvent <= KeyEvent.VK_F12;
            int modifier = isFunctionKey ? 0
                    : java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
            item.setAccelerator(KeyStroke.getKeyStroke(keyEvent, modifier));
        }
        item.addActionListener(listener);
        return item;
    }


    // ---- function-key bindings (F1/F2/F3) ----

    private void installFunctionKeys() {
        JComponent root = (JComponent) getContentPane();
        // Full function-key navigation across every screen, matching the
        // accelerators shown in the menu bar.
        bindFunctionKey(root, KeyEvent.VK_F1, () -> showPanel("dashboard"));
        bindFunctionKey(root, KeyEvent.VK_F2, () -> showPanel("customers"));
        bindFunctionKey(root, KeyEvent.VK_F3, () -> showPanel("ratechart"));
        bindFunctionKey(root, KeyEvent.VK_F4, () -> showPanel("milkentry", "new"));
        bindFunctionKey(root, KeyEvent.VK_F5, () -> showPanel("payments", "new"));
        bindFunctionKey(root, KeyEvent.VK_F6, () -> showPanel("ledger"));
        bindFunctionKey(root, KeyEvent.VK_F7, () -> showPanel("reports"));
        bindFunctionKey(root, KeyEvent.VK_F8, () -> showPanel("data"));
        bindFunctionKey(root, KeyEvent.VK_F9, () -> showPanel("settings"));
        bindFunctionKey(root, KeyEvent.VK_ESCAPE, this::showDashboard);
        // NOTE: Only F-keys and Escape are bound here. Plain letter bindings
        // (e.g. VK_B) must be avoided — they fire from within text fields while
        // the user is typing and hijack navigation. Letter shortcuts now use
        // Ctrl/Cmd via the menu accelerators in buildMenu().
    }

    private void bindFunctionKey(JComponent component, int key, Runnable action) {
        String name = "nav-" + key;
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(key, 0), name);
        component.getActionMap().put(name, new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    // ---- lifecycle ----

    private void performLogout() {
        int result = UIUtil.confirm(this,
                dairy.erp.util.I18n.t("Are you sure you want to logout?"),
                dairy.erp.util.I18n.t("Logout"));
        if (result == JOptionPane.YES_OPTION) {
            dispose();
            if (onLogout != null) {
                onLogout.run();
            }
        }
    }

    private void confirmExit() {
        int result = UIUtil.confirm(this,
                dairy.erp.util.I18n.t("Are you sure you want to exit?"),
                dairy.erp.util.I18n.t("Exit"));
        if (result == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    private void showAbout() {
        AboutPanel.showAboutDialog(this);
    }
}

