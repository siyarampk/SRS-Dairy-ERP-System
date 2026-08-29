package dairy.erp.ui;

import dairy.erp.model.DashboardSummary;
import dairy.erp.service.DashboardService;
import dairy.erp.util.CurrencyUtil;
import dairy.erp.util.DateUtil;
import dairy.erp.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.function.Consumer;

/**
 * Main dashboard shown after login. Displays today's collection summary as
 * cards and offers quick-access buttons for common actions.
 */
public class DashboardPanel extends JPanel {

    private final DashboardService dashboardService = new DashboardService();
    private final dairy.erp.service.SettingsService settingsService = new dairy.erp.service.SettingsService();
    private final JLabel dairyNameLabel = new JLabel("", javax.swing.SwingConstants.CENTER);
    private final JLabel customersValue = valueLabel();
    private final JLabel milkValue = valueLabel();
    private final JLabel amountValue = valueLabel();
    private final JLabel morningValue = valueLabel();
    private final JLabel eveningValue = valueLabel();
    private final JLabel cowValue = valueLabel();
    private final JLabel buffaloValue = valueLabel();
    private final JLabel mixValue = valueLabel();

    public DashboardPanel(Consumer<String> onQuickAction) {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        // Register once (not in loadDairyName, which runs on every refresh).
        dairy.erp.util.AppBus.onDairyNameChanged(this::applyDairyName);
        loadDairyName();
        add(buildHeader(), BorderLayout.NORTH);
        add(buildSummary(), BorderLayout.CENTER);
        add(buildQuickActions(onQuickAction), BorderLayout.SOUTH);
        refresh();
    }

    /** Shows the dairy name (from Settings) in red, 28px bold — parallel to the title. */
    private void loadDairyName() {
        dairyNameLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        dairyNameLabel.setForeground(Color.RED);
        // Row-friendly padding so the name lines up with the logo/title in one line.
        dairyNameLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        applyDairyName(settingsService.get("dairy.name"));
    }

    private void applyDairyName(String name) {
        dairyNameLabel.setText(name == null || name.isBlank() ? "SRS Dairy ERP" : name);
    }

    private JPanel buildHeader() {
        // Left: logo + title. Right: dairy name — all on the same row.
        JPanel p = new JPanel(new BorderLayout());
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        javax.swing.ImageIcon logo = UIUtil.loadLogoByHeight(90, 340);
        if (logo != null) {
            left.add(new JLabel(logo));
        }
        JLabel title = new JLabel("Today's Collection  (" + DateUtil.toDisplay(DateUtil.today()) + ")");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        left.add(title);
        p.add(left, BorderLayout.WEST);
        dairyNameLabel.setVerticalAlignment(JLabel.CENTER);
        p.add(dairyNameLabel, BorderLayout.EAST);
        return p;
    }

    private JPanel buildSummary() {
        JPanel grid = new JPanel(new GridLayout(3, 1, 8, 10));
        grid.add(cardRow("Total Customers", customersValue));
        grid.add(cardRow("Total Milk", milkValue));
        grid.add(cardRow("Total Amount", amountValue));

        JPanel breakRow = new JPanel(new GridLayout(2, 1, 8, 8));
        breakRow.add(cardRow("Morning", morningValue));
        breakRow.add(cardRow("Evening", eveningValue));

        JPanel typeRow = new JPanel(new GridLayout(1, 3, 8, 8));
        typeRow.add(miniCard("Cow", cowValue));
        typeRow.add(miniCard("Buffalo", buffaloValue));
        typeRow.add(miniCard("Mix", mixValue));

        JPanel center = new JPanel(new GridLayout(1, 3, 12, 8));
        center.add(grid);
        center.add(breakRow);
        center.add(typeRow);
        return center;
    }

    private JPanel cardRow(String label, JLabel value) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createTitledBorder(label));
        card.setBackground(Color.WHITE);
        value.setHorizontalAlignment(JLabel.CENTER);
        value.setFont(value.getFont().deriveFont(Font.BOLD, 22f));
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel miniCard(String label, JLabel value) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createTitledBorder(label));
        card.setBackground(Color.WHITE);
        value.setHorizontalAlignment(JLabel.CENTER);
        value.setFont(value.getFont().deriveFont(Font.PLAIN, 16f));
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildQuickActions(Consumer<String> onAction) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 12));
        String[] actions = {
                "New Milk Entry", "Customers", "Rate Chart", "Today's Collection",
                "Customer Report", "Monthly Report", "Payments", "Backup"
        };
        for (String action : actions) {
            JButton b = new JButton(action);
            UIUtil.styleButton(b);
            b.addActionListener(e -> {
                if (onAction != null) {
                    onAction.accept(action);
                }
            });
            p.add(b);
        }
        return p;
    }

    private static JLabel valueLabel() {
        JLabel l = new JLabel("-");
        l.setForeground(new Color(0x1a, 0x5f, 0x7a));
        return l;
    }

    /** Reloads today's summary from the database and the dairy name from Settings. */
    public void refresh() {
        loadDairyName();
        DashboardSummary s = dashboardService.todaySummary(DateUtil.today());
        customersValue.setText(String.valueOf(s.getTotalCustomers()));
        milkValue.setText(CurrencyUtil.format(s.getTotalQuantity()) + " LTR");
        amountValue.setText(CurrencyUtil.formatMoney(s.getTotalAmount()));
        morningValue.setText(CurrencyUtil.format(s.getMorningQty()) + " LTR");
        eveningValue.setText(CurrencyUtil.format(s.getEveningQty()) + " LTR");
        cowValue.setText(CurrencyUtil.format(s.getCowQty()) + " LTR");
        buffaloValue.setText(CurrencyUtil.format(s.getBuffaloQty()) + " LTR");
        mixValue.setText(CurrencyUtil.format(s.getMixQty()) + " LTR");
    }
}
