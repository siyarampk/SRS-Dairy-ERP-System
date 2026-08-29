package dairy.erp.ui;

import dairy.erp.config.AppConfig;
import dairy.erp.ui.dialogs.PasswordChangeDialog;

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
        registerPanel("ledger", "Customer Ledger", new CustomerLedgerPanel());
        registerPanel("reports", "Reports", new ReportsPanel());
        registerPanel("settings", "Settings", new SettingsPanel(username));
        registerPanel("data", "Import / Export / Backup", new ImportExportPanel());

        add(cardPanel, BorderLayout.CENTER);

        // ---- Current menu title bar ----
        JPanel menuBar = new JPanel(new BorderLayout());
        menuBar.setBackground(new java.awt.Color(0x1a, 0x5f, 0x7a));
        menuBar.setBorder(BorderFactory.createEmptyBorder(6, 15, 6, 15));
        menuLabel = new JLabel("Dashboard");
        menuLabel.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
        menuLabel.setForeground(java.awt.Color.WHITE);
        menuBar.add(menuLabel, BorderLayout.WEST);
        add(menuBar, BorderLayout.NORTH);

        JPanel status = new JPanel(new BorderLayout());
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
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
        String title = panelNames.getOrDefault(key, "Dashboard");
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
        setTitle(AppConfig.APP_NAME + "  — " + title);
        statusLabel.setText(title);
        menuLabel.setText(title);
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
        masterMenu.add(menuItem("Dashboard", 0, e -> showPanel("dashboard")));
        masterMenu.addSeparator();
        masterMenu.add(menuItem("Customer Management", KeyEvent.VK_F1, e -> showPanel("customers")));
        masterMenu.add(menuItem("Rate Chart", KeyEvent.VK_F2, e -> showPanel("ratechart")));
        masterMenu.add(menuItem("Settings", 0, e -> showPanel("settings")));

        JMenu milkMenu = new JMenu("Milk Collection");
        bar.add(milkMenu);
        milkMenu.add(menuItem("New Collection", KeyEvent.VK_N, e -> showPanel("milkentry", "new")));
        milkMenu.add(menuItem("Collection History", 0, e -> showPanel("milkentry", "history")));

        JMenu paymentMenu = new JMenu("Payments");
        bar.add(paymentMenu);
        paymentMenu.add(menuItem("New Payment", KeyEvent.VK_N, e -> showPanel("payments", "new")));
        paymentMenu.add(menuItem("Payment History", 0, e -> showPanel("payments", "history")));
        paymentMenu.add(menuItem("Customer Ledger", 0, e -> showPanel("ledger")));

        JMenu reportMenu = new JMenu("Reports");
        bar.add(reportMenu);
        // Only one menu item — every report type opens the same Reports screen,
        // where all report types are chosen from a single dropdown.
        reportMenu.add(menuItem("Open Reports", KeyEvent.VK_F3, e -> showPanel("reports")));

        JMenu dataMenu = new JMenu("Data");
        bar.add(dataMenu);
        dataMenu.add(menuItem("Import CSV", 0, e -> showPanel("data")));
        dataMenu.add(menuItem("Export CSV", 0, e -> showPanel("data")));
        dataMenu.addSeparator();
        dataMenu.add(menuItem("Backup Database", KeyEvent.VK_B, e -> showPanel("data")));
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

        setJMenuBar(bar);
    }

    private JMenuItem menuItem(String text, int keyEvent, java.awt.event.ActionListener listener) {
        JMenuItem item = new JMenuItem(text);
        if (keyEvent != 0) {
            item.setAccelerator(KeyStroke.getKeyStroke(keyEvent, 0));
        }
        item.addActionListener(listener);
        return item;
    }


    // ---- function-key bindings (F1/F2/F3) ----

    private void installFunctionKeys() {
        JComponent root = (JComponent) getContentPane();
        bindFunctionKey(root, KeyEvent.VK_F1, () -> showPanel("customers"));
        bindFunctionKey(root, KeyEvent.VK_F2, () -> showPanel("ratechart"));
        bindFunctionKey(root, KeyEvent.VK_F3, () -> showPanel("reports"));
        bindFunctionKey(root, KeyEvent.VK_ESCAPE, this::showDashboard);
        bindFunctionKey(root, KeyEvent.VK_B, () -> showPanel("data"));
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
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?", "Logout", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            dispose();
            if (onLogout != null) {
                onLogout.run();
            }
        }
    }

    private void confirmExit() {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to exit?", "Exit", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }

    private void showAbout() {
        String about =
                "SRS Dairy ERP – Milk Collection Management System\n\n"
                + "SRS Dairy ERP is a simple and reliable solution designed to make daily dairy\n"
                + "operations easier. It helps manage customer details, milk collection, milk\n"
                + "quantity, rates, payments, and reports efficiently in one place.\n\n"
                + "Developer: Siyaram Meena\n"
                + "Company: SRS Pvt. Ltd.\n"
                + "Email: siyarampk@gmail.com\n"
                + "Version: " + AppConfig.APP_VERSION;
        JOptionPane.showMessageDialog(this, about, "About", JOptionPane.INFORMATION_MESSAGE);
    }
}

