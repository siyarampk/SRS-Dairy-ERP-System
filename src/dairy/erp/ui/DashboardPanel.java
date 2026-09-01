package dairy.erp.ui;

import dairy.erp.model.DashboardSummary;
import dairy.erp.service.DashboardService;
import dairy.erp.util.CurrencyUtil;
import dairy.erp.util.DairyNameLabel;
import dairy.erp.util.DateUtil;
import dairy.erp.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.function.Consumer;

/**
 * Main dashboard shown after login — redesigned to match the reference layout:
 * three columns (Today's Summary | Collection Shifts | Milk Type Breakdown)
 * with icon-based cards, plus a quick-action button bar at the bottom.
 */
public class DashboardPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Color CARD_BORDER = new Color(0xd8, 0xde, 0xe4);
    private static final Color MORNING_GREEN = new Color(0x2e, 0x7d, 0x32);
    private static final Color EVENING_ORANGE = new Color(0xef, 0x6c, 0x00);

    private final DashboardService dashboardService = new DashboardService();
    private final dairy.erp.service.SettingsService settingsService = new dairy.erp.service.SettingsService();
    private final DairyNameLabel dairyNameLabel = new DairyNameLabel();

    // ---- value labels ----
    private final JLabel customersValue = valueLabel(UIUtil.BRAND);
    private final JLabel milkValue = valueLabel(UIUtil.BRAND);
    private final JLabel amountValue = valueLabel(UIUtil.BRAND);
    private final JLabel morningValue = valueLabel(MORNING_GREEN);
    private final JLabel eveningValue = valueLabel(EVENING_ORANGE);
    private final JLabel cowValue = valueLabel(UIUtil.BRAND);
    private final JLabel buffaloValue = valueLabel(UIUtil.BRAND);
    private final JLabel mixValue = valueLabel(UIUtil.BRAND);

    // Animated milk-fill vessels for the three milk-type panels.
    private final MilkLevelPanel cowLevelPanel = new MilkLevelPanel();
    private final MilkLevelPanel buffaloLevelPanel = new MilkLevelPanel();
    private final MilkLevelPanel mixLevelPanel = new MilkLevelPanel();

    // Mini 7-day earnings sparkline inside the Total Amount card.
    private final MiniAreaChart amountChart = new MiniAreaChart();

    public DashboardPanel(Consumer<String> onQuickAction) {
        super(new BorderLayout());
        setBackground(new Color(0xf4, 0xf6, 0xf8));
        setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        // Register once (not in loadDairyName, which runs on every refresh).
        dairy.erp.util.AppBus.onDairyNameChanged(this::applyDairyName);
        loadDairyName();
        add(buildHeader(), BorderLayout.NORTH);
        add(buildSummary(), BorderLayout.CENTER);
        add(buildQuickActions(onQuickAction), BorderLayout.SOUTH);
        refresh();
    }

                // Golden logo-style dairy name (shared DairyNameLabel component, same as Customer panel).
    private void loadDairyName() {
        dairyNameLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 10));
        applyDairyName(settingsService.get("dairy.name"));
    }

    private void applyDairyName(String name) {
        dairyNameLabel.setText(name == null || name.isBlank() ? "SRS Dairy ERP" : name);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        javax.swing.ImageIcon logo = UIUtil.loadLogoByHeight(56, 260);
        if (logo != null) {
            left.add(new JLabel(logo));
        }
        JLabel title = new JLabel("Today's Collection  [" + DateUtil.toDisplay(DateUtil.today()) + "]");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setForeground(new Color(0x22, 0x22, 0x22));
        left.add(title);
        p.add(left, BorderLayout.WEST);

                // Golden logo-style dairy name (shared DairyNameLabel component).
        p.add(dairyNameLabel, BorderLayout.EAST);
        return p;
    }

    private JPanel buildSummary() {
        // ---- Column 1: Today's Summary (3 stacked cards) ----
        JPanel summaryCol = new JPanel(new BorderLayout(0, 4));
        summaryCol.setOpaque(false);
        summaryCol.add(sectionLabel("Today's Summary"), BorderLayout.NORTH);
        JPanel summaryCards = new JPanel(new GridLayout(3, 1, 8, 8));
        summaryCards.setOpaque(false);
        summaryCards.add(statCard("\uD83D\uDC64", customersValue, "Total Customers"));
        summaryCards.add(statCard(new MilkCanIcon(44), milkValue, "Total Milk"));
        summaryCards.add(buildAmountCard());
        summaryCol.add(summaryCards, BorderLayout.CENTER);

        // ---- Middle column: the small collection-shift cards sit above the
        // dashboard overview image, and the image is pushed down below them ----
        JPanel middleCol = new JPanel(new BorderLayout(0, 8));
        middleCol.setOpaque(false);
        middleCol.add(buildShiftCards(), BorderLayout.NORTH);
        middleCol.add(buildImagePanel(), BorderLayout.CENTER);

        // ---- Column 3: Milk Type Breakdown (3 tall mini cards) ----
        JPanel typeCol = new JPanel(new BorderLayout(0, 4));
        typeCol.setOpaque(false);
        typeCol.add(sectionLabel("Milk Type Breakdown"), BorderLayout.NORTH);
        JPanel typeRow = new JPanel(new GridLayout(1, 3, 8, 0));
        typeRow.setOpaque(false);
        // Cow Milk: Pale Yellowish-White (light cream / warm yellow tint)
        typeRow.add(tallCard(cowLevelPanel, cowValue, "\uD83D\uDC04 Cow Milk",
                new Color(0xFF, 0xFD, 0xE7)));
        // Buffalo Milk: Bright Pure White (opaque, stark white)
        typeRow.add(tallCard(buffaloLevelPanel, buffaloValue, "\uD83D\uDC03 Buffalo Milk",
                new Color(0xF8, 0xF8, 0xFF)));
        // Mixed Milk: Off-White / Pale Cream
        typeRow.add(tallCard(mixLevelPanel, mixValue, "\u267F Mix Milk",
                new Color(0xFF, 0xF8, 0xE7)));
        typeCol.add(typeRow, BorderLayout.CENTER);

        JPanel center = new JPanel(new GridLayout(1, 3, 12, 0));
        center.setOpaque(false);
        center.add(summaryCol);
        center.add(middleCol);
        center.add(typeCol);
        return center;
    }

    /** Small "Collection Shifts" section: two compact cards side by side. */
    private JPanel buildShiftCards() {
        JPanel shiftsCol = new JPanel(new BorderLayout(0, 4));
        shiftsCol.setOpaque(false);
        shiftsCol.add(sectionLabel("Collection Shifts"), BorderLayout.NORTH);
        JPanel shiftRow = new JPanel(new GridLayout(1, 2, 10, 0));
        shiftRow.setOpaque(false);
        shiftRow.add(smallShiftCard(new SunriseIcon(30), morningValue,
                "Morning Collection", MORNING_GREEN));
        shiftRow.add(smallShiftCard("\uD83C\uDF19", eveningValue,
                "Evening Collection", EVENING_ORANGE));
        shiftsCol.add(shiftRow, BorderLayout.CENTER);
        return shiftsCol;
    }

    /** Compact horizontal shift card: accent strip, small icon, value + caption. */
    private JPanel smallShiftCard(Object icon, JLabel value, String caption, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 1, true),
                BorderFactory.createEmptyBorder(10, 0, 10, 8)));

        JPanel strip = new JPanel();
        strip.setBackground(accent);
        strip.setPreferredSize(new Dimension(7, 1));
        card.add(strip, BorderLayout.WEST);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(iconLabel(icon, 30));

        JPanel text = new JPanel(new BorderLayout(0, 0));
        text.setOpaque(false);
        value.setFont(value.getFont().deriveFont(Font.BOLD, 20f));
        value.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        text.add(value, BorderLayout.NORTH);
        JLabel cap = new JLabel(caption);
        cap.setFont(cap.getFont().deriveFont(Font.PLAIN, 14f));
        cap.setForeground(new Color(0x44, 0x44, 0x44));
        text.add(cap, BorderLayout.SOUTH);
        left.add(text);

        card.add(left, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(0, 240));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        return card;
    }

    /** White card holding the scaled dashboard overview image. */
    private JPanel buildImagePanel() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 6, 25, 6)));
        JPanel pad = new JPanel(new BorderLayout(0, 0));
        pad.setOpaque(false);
        JLabel image = loadDashboardImage();
        if (image != null) {
            image.setAlignmentX(Component.CENTER_ALIGNMENT);
            pad.add(image, BorderLayout.NORTH);
        } else {
            JLabel placeholder = new JLabel("Dashboard image not found",
                    javax.swing.SwingConstants.CENTER);
            placeholder.setForeground(new Color(0x99, 0x99, 0x99));
            pad.add(placeholder, BorderLayout.CENTER);
        }
        card.add(pad, BorderLayout.CENTER);
        return card;
    }

    /** Loads resources/images/dashbord.png scaled to fit the middle column. */
    private JLabel loadDashboardImage() {
        try {
            File file = new File("resources/images/dashbord.png");
            if (!file.exists()) {
                return null;
            }
            Image raw = javax.imageio.ImageIO.read(file);
            if (raw == null) {
                return null;
            }
            // 1008x1051 resource — scale to a compact size that fits under the
            // shift cards, keeping the aspect ratio.
            int w = 300;
            int h = Math.round(w * (float) raw.getHeight(null) / raw.getWidth(null));
            Image scaled = raw.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            JLabel label = new JLabel(new ImageIcon(scaled), javax.swing.SwingConstants.CENTER);
            label.setPreferredSize(new Dimension(w, h));
            return label;
        } catch (Exception e) {
            return null;
        }
    }

    /** Small bold section title above a column. */
    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 15f));
        l.setForeground(new Color(0x33, 0x33, 0x33));
        l.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 0));
        return l;
    }

    /** Builds an icon/visual for the cards: painted icon, widget or emoji. */
    private static javax.swing.JComponent iconLabel(Object icon, int emojiFontSize) {
        if (icon instanceof java.awt.Component) {
            javax.swing.JComponent c = (javax.swing.JComponent) icon;
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
            return c;
        }
        if (icon instanceof javax.swing.Icon) {
            JLabel l = new JLabel((javax.swing.Icon) icon, javax.swing.SwingConstants.CENTER);
            l.setAlignmentX(Component.CENTER_ALIGNMENT);
            return l;
        }
        JLabel l = new JLabel(String.valueOf(icon), javax.swing.SwingConstants.CENTER);
        l.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, emojiFontSize));
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    /** White card: big icon on top, value, caption below. */
    private JPanel statCard(Object icon, JLabel value, String caption) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(14, 10, 14, 10)));

        card.add(Box.createVerticalGlue());
        card.add(iconLabel(icon, 40));
        card.add(Box.createVerticalStrut(6));

        value.setAlignmentX(Component.CENTER_ALIGNMENT);
        value.setFont(value.getFont().deriveFont(Font.BOLD, 24f));
        card.add(value);
        card.add(Box.createVerticalStrut(6));

        JLabel cap = new JLabel(caption, javax.swing.SwingConstants.CENTER);
        cap.setFont(cap.getFont().deriveFont(Font.PLAIN, 15f));
        cap.setForeground(new Color(0x44, 0x44, 0x44));
        cap.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(cap);
        card.add(Box.createVerticalGlue());
        return card;
    }

    /** Tall narrow card for milk types: icon + value, caption right under the value. */
    private JPanel tallCard(Object icon, JLabel value, String caption, Color fillColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(fillColor);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(12, 6, 12, 6)));

        JPanel middle = new JPanel();
        middle.setOpaque(false);
        middle.setLayout(new BoxLayout(middle, BoxLayout.Y_AXIS));
        middle.add(Box.createVerticalGlue());
        middle.add(iconLabel(icon, 38));
        middle.add(Box.createVerticalStrut(10));
        value.setAlignmentX(Component.CENTER_ALIGNMENT);
        value.setFont(value.getFont().deriveFont(Font.BOLD, 20f));
        middle.add(value);
        // Caption comes immediately after the LTR value.
        middle.add(Box.createVerticalStrut(4));
        JLabel cap = new JLabel(caption, javax.swing.SwingConstants.CENTER);
        cap.setFont(cap.getFont().deriveFont(Font.PLAIN, 14f));
        cap.setForeground(new Color(0x44, 0x44, 0x44));
        cap.setAlignmentX(Component.CENTER_ALIGNMENT);
        middle.add(cap);
        middle.add(Box.createVerticalGlue());
        card.add(middle, BorderLayout.CENTER);
        return card;
    }

    /**
     * Total Amount card styled like the reference design: the amount on top
     * and a compact smooth area chart of the last 7 days' earnings below it
     * (Sun..Sat labels, no axis clutter).
     */
    private JPanel buildAmountCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1, true),
                BorderFactory.createEmptyBorder(10, 10, 8, 10)));

        card.add(Box.createVerticalGlue());
        amountValue.setAlignmentX(Component.CENTER_ALIGNMENT);
        amountValue.setFont(amountValue.getFont().deriveFont(Font.BOLD, 24f));
        card.add(amountValue);
        card.add(Box.createVerticalStrut(2));

        JLabel cap = new JLabel("Total Amount", javax.swing.SwingConstants.CENTER);
        cap.setFont(cap.getFont().deriveFont(Font.PLAIN, 14f));
        cap.setForeground(new Color(0x44, 0x44, 0x44));
        cap.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(cap);
        card.add(Box.createVerticalStrut(6));

        amountChart.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(amountChart);
        card.add(Box.createVerticalGlue());
        return card;
    }

    private JPanel buildQuickActions(Consumer<String> onAction) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        p.setOpaque(false);
        // Each action gets its own accent colour (icon + border + tint),
        // matching the reference design.
        String[][] actions = {
                {"New Milk Entry", "#1976D2"},   // blue pencil
                {"Customers", "#2E7D32"},        // green people
                {"Rate Chart", "#EF6C00"},       // orange chart
                {"Today's Collection", "#43A047"}, // bright green clipboard
                {"Customer Report", "#8E24AA"},  // purple document
                {"Monthly Report", "#00897B"},   // teal calendar
                {"Payments", "#039BE5"},         // cyan banknote
                {"Backup", "#1565C0"}            // navy shield
        };
        for (String[] a : actions) {
            String name = a[0];
            Color accent = Color.decode(a[1]);
            JButton b = new JButton(name);
            // Matching vector icon + accent theme, uniform button size.
            b.setIcon(dairy.erp.util.ButtonIcons.of(name, accent));
            b.setIconTextGap(9);
            b.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
            b.setVerticalTextPosition(javax.swing.SwingConstants.CENTER);
            UIUtil.styleAccentButton(b, accent);
            // Width adapts to the label so long names (e.g. "Today's
            // Collection") are never truncated; height stays uniform.
            Dimension d = b.getPreferredSize();
            b.setPreferredSize(new Dimension(Math.max(d.width + 18, 170), 46));
            b.addActionListener(e -> {
                if (onAction != null) {
                    onAction.accept(name);
                }
            });
            p.add(b);
        }
        return p;
    }

    private static JLabel valueLabel(Color color) {
        JLabel l = new JLabel("-");
        l.setForeground(color);
        return l;
    }

    /** Hand-drawn milk can: bail handle on top, lid, side ear-handles, milk fill. */
    private static class MilkCanIcon implements javax.swing.Icon {
        private final int size;

        MilkCanIcon(int size) {
            this.size = size;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, java.awt.Graphics g, int x, int y) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            Color ink = new Color(0x37, 0x47, 0x4f);
            g2.setStroke(new java.awt.BasicStroke(2.4f, java.awt.BasicStroke.CAP_ROUND,
                    java.awt.BasicStroke.JOIN_ROUND));

            // Top bail handle (arc over the lid)
            g2.setColor(ink);
            g2.drawArc(x + Math.round(size * 0.32f), y + Math.round(size * 0.015f),
                    Math.round(size * 0.36f), Math.round(size * 0.17f), 0, 180);
            // Lid (rounded cap at the neck)
            int lidL = Math.round(size * 0.32f);
            int lidR = Math.round(size * 0.68f);
            int lidT = Math.round(size * 0.16f);
            int lidH = Math.round(size * 0.09f);
            g2.setColor(new Color(0xd7, 0xe1, 0xe8));
            g2.fillRoundRect(x + lidL, y + lidT, lidR - lidL, lidH, 10, 10);
            g2.setColor(ink);
            g2.drawRoundRect(x + lidL, y + lidT, lidR - lidL, lidH, 10, 10);

            // Body: narrow neck flaring out into a rounded can, milky fill
            int neckL = Math.round(size * 0.35f);
            int neckR = Math.round(size * 0.65f);
            int neckB = Math.round(size * 0.25f);
            int bodyL = Math.round(size * 0.22f);
            int bodyR = Math.round(size * 0.78f);
            int bodyB = Math.round(size * 0.90f);
            int r = Math.round(size * 0.08f);
            java.awt.geom.GeneralPath body = new java.awt.geom.GeneralPath();
            body.moveTo(x + neckL, y + neckB);
            body.quadTo(x + bodyL, y + Math.round(size * 0.42f), x + bodyL, y + bodyB - r);
            body.quadTo(x + bodyL, y + bodyB, x + bodyL + r, y + bodyB);
            body.lineTo(x + bodyR - r, y + bodyB);
            body.quadTo(x + bodyR, y + bodyB, x + bodyR, y + bodyB - r);
            body.quadTo(x + bodyR, y + Math.round(size * 0.42f), x + neckR, y + neckB);
            body.closePath();
            g2.setColor(new Color(0xef, 0xf6, 0xfa));
            g2.fill(body);
            g2.setColor(ink);
            g2.draw(body);

            // Side ear-handles (left + right, on the shoulders of the can)
            int earY = Math.round(size * 0.33f);
            int earW = Math.round(size * 0.12f);
            int earH = Math.round(size * 0.09f);
            int earInset = Math.round(size * 0.13f);
            g2.setColor(new Color(0xd7, 0xe1, 0xe8));
            g2.fillRoundRect(x + earInset, y + earY, earW, earH, 8, 8);
            g2.fillRoundRect(x + size - earInset - earW, y + earY, earW, earH, 8, 8);
            g2.setColor(ink);
            g2.drawRoundRect(x + earInset, y + earY, earW, earH, 8, 8);
            g2.drawRoundRect(x + size - earInset - earW, y + earY, earW, earH, 8, 8);

            // Milk level line (teal accent) inside the can
            g2.setColor(UIUtil.BRAND);
            g2.drawLine(x + bodyL + 4, y + Math.round(size * 0.58f),
                    x + bodyR - 4, y + Math.round(size * 0.58f));
            g2.dispose();
        }
    }

    /** Sunrise icon: orange half-sun with rays rising over the horizon line. */
    private static class SunriseIcon implements javax.swing.Icon {
        private final int size;

        SunriseIcon(int size) {
            this.size = size;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, java.awt.Graphics g, int x, int y) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

            int horizonY = y + Math.round(size * 0.74f);
            int cx = x + Math.round(size * 0.50f);
            int sunR = Math.round(size * 0.27f);

            // Bold rays fanning out above the horizon.
            g2.setColor(new Color(0xf5, 0x9e, 0x0b));
            g2.setStroke(new java.awt.BasicStroke(2.6f, java.awt.BasicStroke.CAP_ROUND,
                    java.awt.BasicStroke.JOIN_ROUND));
            for (int angle = 30; angle <= 150; angle += 30) {
                double rad = Math.toRadians(angle);
                int x1 = cx + Math.round(sunR * 1.35f * (float) Math.cos(rad));
                int y1 = horizonY - Math.round(sunR * 1.35f * (float) Math.sin(rad));
                int x2 = cx + Math.round(sunR * 1.80f * (float) Math.cos(rad));
                int y2 = horizonY - Math.round(sunR * 1.80f * (float) Math.sin(rad));
                g2.drawLine(x1, y1, x2, y2);
            }

            // Half-sun rising above the horizon.
            java.awt.geom.GeneralPath sun = new java.awt.geom.GeneralPath();
            sun.moveTo(cx - sunR, horizonY);
            sun.quadTo(cx - sunR, horizonY - Math.round(sunR * 1.45f),
                    cx, horizonY - Math.round(sunR * 1.45f));
            sun.quadTo(cx + sunR, horizonY - Math.round(sunR * 1.45f), cx + sunR, horizonY);
            sun.closePath();
            g2.setColor(new Color(0xf6, 0x9c, 0x1d));
            g2.fill(sun);
            g2.setColor(new Color(0xd9, 0x77, 0x06));
            g2.draw(sun);

            // Bold horizon line with a fainter ground shade below it.
            g2.setColor(new Color(0x37, 0x47, 0x4f));
            g2.setStroke(new java.awt.BasicStroke(2.8f, java.awt.BasicStroke.CAP_ROUND,
                    java.awt.BasicStroke.JOIN_ROUND));
            g2.drawLine(x + Math.round(size * 0.06f), horizonY,
                    x + Math.round(size * 0.94f), horizonY);
            g2.setColor(new Color(0xbf, 0xd3, 0xdc));
            g2.drawLine(x + Math.round(size * 0.18f), horizonY + Math.round(size * 0.09f),
                    x + Math.round(size * 0.82f), horizonY + Math.round(size * 0.09f));
            g2.dispose();
        }
    }

    /** Mix Milk icon: one big milk droplet of three blended colour bands. */
    private static class MixIcon implements javax.swing.Icon {
        private final int size;

        MixIcon(int size) {
            this.size = size;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, java.awt.Graphics g, int x, int y) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            float s = size;

            // One large milk droplet (pointed top, round bottom).
            java.awt.geom.Path2D.Float drop = new java.awt.geom.Path2D.Float();
            drop.moveTo(x + s * 0.50f, y + s * 0.08f);
            drop.curveTo(x + s * 0.36f, y + s * 0.24f, x + s * 0.23f, y + s * 0.40f,
                    x + s * 0.23f, y + s * 0.62f);
            drop.quadTo(x + s * 0.23f, y + s * 0.90f, x + s * 0.50f, y + s * 0.90f);
            drop.quadTo(x + s * 0.77f, y + s * 0.90f, x + s * 0.77f, y + s * 0.62f);
            drop.curveTo(x + s * 0.77f, y + s * 0.40f, x + s * 0.64f, y + s * 0.24f,
                    x + s * 0.50f, y + s * 0.08f);
            drop.closePath();

            // Three colour bands clipped inside the droplet: cow (teal),
            // mixed (green) and buffalo (orange) — one blend, "all in one".
            g2.setClip(drop);
            g2.setColor(new Color(0x1a, 0x5f, 0x7a));
            g2.fill(new java.awt.geom.Rectangle2D.Float(x, y, s * 0.41f, s));
            g2.setColor(new Color(0x2e, 0x7d, 0x32));
            g2.fill(new java.awt.geom.Rectangle2D.Float(x + s * 0.41f, y, s * 0.18f, s));
            g2.setColor(new Color(0xef, 0x6c, 0x00));
            g2.fill(new java.awt.geom.Rectangle2D.Float(x + s * 0.59f, y, s * 0.41f, s));
            g2.setClip(null);

            // Bold outline so the droplet reads clearly on the white card.
            g2.setStroke(new java.awt.BasicStroke(Math.max(2.4f, s * 0.055f),
                    java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(0x37, 0x47, 0x4f));
            g2.draw(drop);
            g2.dispose();
        }
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

        // 7-day earnings sparkline in the Total Amount card.
        amountChart.setDailyData(dashboardService.dailyEarningsLast7Days(DateUtil.today()));

        // Milk-type cans fill dynamically relative to the day's largest type
        // quantity, but never completely: 0 (empty) when the type has no milk,
        // otherwise base ~40% up to a maximum of 80% for the day's biggest type.
        double maxType = Math.max(
                s.getCowQty().doubleValue(),
                Math.max(s.getBuffaloQty().doubleValue(), s.getMixQty().doubleValue()));
        cowLevelPanel.setLevel(fillFor(s.getCowQty().doubleValue(), maxType));
        buffaloLevelPanel.setLevel(fillFor(s.getBuffaloQty().doubleValue(), maxType));
        mixLevelPanel.setLevel(fillFor(s.getMixQty().doubleValue(), maxType));
    }

    /**
     * Fill level for a type: zero quantity shows an empty can; otherwise the
     * fill is scaled between the min fill and the MAX_FILL cap, so a can is
     * always partly filled but never completely full.
     */
    private static double fillFor(double qty, double maxType) {
        if (qty <= 0) {
            return 0; // no milk -> no fill
        }
        return MilkLevelPanel.MIN_FILL
                + (MilkLevelPanel.MAX_FILL - MilkLevelPanel.MIN_FILL) * (qty / maxType);
    }

    /**
     * A glass tumbler that fills up with milk like a vessel being filled —
     * the milk level animates smoothly toward its target quantity and the
     * surface carries a gentle "milk shake" wave while it is filling.
     */
    private static class MilkLevelPanel extends javax.swing.JComponent {
        private static final Color INK = new Color(0x37, 0x47, 0x4f);
        // Milk never fills the can completely: it rests between MIN_FILL
        // (always partly filled, like a can in use) and MAX_FILL even when
        // its milk type has the day's highest quantity.
        private static final double MIN_FILL = 0.40;
        private static final double MAX_FILL = 0.80;
        private double targetLevel = MIN_FILL; // requested fill level 0..1
        private double level;         // currently shown (animated) fill 0..1
        private double wavePhase;     // advances the surface wave
        private final javax.swing.Timer animator;

        MilkLevelPanel() {
            setPreferredSize(new Dimension(64, 64));
            setOpaque(false);
            setAlignmentX(Component.CENTER_ALIGNMENT);
            animator = new javax.swing.Timer(33, e -> {
                wavePhase += 0.16;
                level += (targetLevel - level) * 0.12;
                if (isShowing()) {
                    repaint();
                }
            });
            animator.start();
        }

        /** Sets the requested fill level (0..1); the fill animates toward it. */
        void setLevel(double level) {
            targetLevel = Math.max(0.0, Math.min(1.0, level));
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            float s = Math.min(getWidth(), getHeight());
            float ox = (getWidth() - s) / 2f;
            float oy = (getHeight() - s) / 2f;

            // ---- Transparent milk can (drum): bail handle, lid, body ----
            float canL = ox + s * 0.24f;
            float canR = ox + s * 0.80f;
            float neckL = ox + s * 0.36f;
            float neckR = ox + s * 0.70f;
            float lidT = oy + s * 0.16f;
            float lidB = oy + s * 0.24f;
            float bodyT = oy + s * 0.24f;
            float bodyB = oy + s * 0.92f;
            float r = s * 0.07f;

            // Can body: narrow neck flaring into a rounded drum.
            java.awt.geom.Path2D.Float can = new java.awt.geom.Path2D.Float();
            can.moveTo(neckL, bodyT);
            can.quadTo(canL, oy + s * 0.40f, canL, bodyB - r);
            can.quadTo(canL, bodyB, canL + r, bodyB);
            can.lineTo(ox + s * 0.80f - r, bodyB);
            can.quadTo(canR, bodyB, canR, bodyB - r);
            can.quadTo(canR, oy + s * 0.42f, neckR, bodyT);
            can.closePath();

            // Milk fill: rises toward the target with a wavy, shaking surface.
            float fillTop = bodyT + s * 0.03f;
            float fillY = bodyB - (bodyB - fillTop) * (float) level;
            // The wave shakes harder while the milk is still filling.
            double settling = Math.min(1.0, Math.abs(targetLevel - level) * 4.0);
            float amp = (float) (s * (0.015 + 0.035 * settling));

            g2.setClip(can);
            g2.setColor(new Color(0xdc, 0xed, 0xf5)); // milky tint
            g2.fill(wavePath(ox, oy, fillY, amp, s, true));
            // Wavy surface line on the milk.
            g2.setColor(UIUtil.BRAND);
            g2.setStroke(new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_ROUND,
                    java.awt.BasicStroke.JOIN_ROUND));
            g2.draw(wavePath(ox, oy, fillY, amp, s, false));
            g2.setClip(null);

            // Transparent can outline drawn over the fill.
            g2.setStroke(new java.awt.BasicStroke(2.2f, java.awt.BasicStroke.CAP_ROUND,
                    java.awt.BasicStroke.JOIN_ROUND));
            g2.setColor(INK);
            g2.draw(can);

            // Lid (rounded cap) on the neck.
            g2.setColor(new Color(0xd7, 0xe1, 0xe8, 0xB4));
            g2.fillRoundRect(Math.round(ox + s * 0.31f), Math.round(lidT),
                    Math.round(s * 0.36f), Math.round(s * 0.09f), 10, 10);
            g2.setColor(INK);
            g2.drawRoundRect(Math.round(ox + s * 0.32f), Math.round(lidT),
                    Math.round(s * 0.36f), Math.round(s * 0.09f), 10, 10);

            // Bail handle arcing over the lid.
            g2.drawArc(Math.round(ox + s * 0.36f), Math.round(oy + s * 0.02f),
                    Math.round(s * 0.36f), Math.round(s * 0.14f), 0, 180);

            // Side ear-handles on the shoulders.
            g2.setColor(new Color(0xd7, 0xe1, 0xe8, 0xB4));
            g2.fillRoundRect(Math.round(ox + s * 0.14f), Math.round(bodyT + s * 0.03f),
                    Math.round(s * 0.10f), Math.round(s * 0.08f), 8, 8);
            g2.fillRoundRect(Math.round(ox + s * 0.76f), Math.round(bodyT + s * 0.0f),
                    Math.round(s * 0.10f), Math.round(s * 0.08f), 8, 8);
            g2.setColor(INK);
            g2.drawRoundRect(Math.round(ox + s * 0.14f), Math.round(bodyT),
                    Math.round(s * 0.10f), Math.round(s * 0.08f), 8, 8);
            g2.drawRoundRect(Math.round(ox + s * 0.76f), Math.round(bodyT),
                    Math.round(s * 0.10f), Math.round(s * 0.08f), 8, 8);
            g2.dispose();
        }

        /** Wavy line across the glass; when closed, a filled milk region. */
        private java.awt.geom.Path2D.Float wavePath(float ox, float oy, float fillY,
                float amp, float s, boolean closed) {
            java.awt.geom.Path2D.Float wave = new java.awt.geom.Path2D.Float();
            boolean first = true;
            for (float px = 0; px <= s; px += Math.max(2f, s * 0.04f)) {
                float py = (float) (fillY + amp * Math.sin(wavePhase + px * 0.12));
                if (first) {
                    wave.moveTo(ox + px, py);
                    first = false;
                } else {
                    wave.lineTo(ox + px, py);
                }
            }
            if (closed) {
                wave.lineTo(ox + s, oy + s);
                wave.lineTo(ox, oy + s);
                wave.closePath();
            }
            return wave;
        }
    }

    /**
     * Compact sparkline of the last 7 days' earnings, styled like the
     * reference: smooth line with a light-blue area fill and just the day
     * names (Sun..Sat) underneath — no axes or gridlines.
     */
    private static class MiniAreaChart extends javax.swing.JComponent {
        private static final Color LINE = new Color(0x1a, 0x5f, 0x7a);
        private static final Color LABEL = new Color(0x8a, 0x94, 0x9a);
        private java.util.LinkedHashMap<java.time.LocalDate, java.math.BigDecimal> data =
                new java.util.LinkedHashMap<>();

        MiniAreaChart() {
            setPreferredSize(new Dimension(190, 86));
            setOpaque(false);
            for (int i = 6; i >= 0; i--) {
                data.put(java.time.LocalDate.now().minusDays(i), java.math.BigDecimal.ZERO);
            }
        }

        /** Replaces the plotted data (oldest first) and repaints. */
        void setDailyData(java.util.Map<java.time.LocalDate, java.math.BigDecimal> daily) {
            data = new java.util.LinkedHashMap<>(daily);
            repaint();
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int padX = 6;
            int padT = 8;
            int labelH = 14;
            int plotH = Math.max(20, h - padT - labelH);
            float yBase = padT + plotH;

            int n = data.size();
            Point2D.Float[] pts = new Point2D.Float[n];
            java.time.LocalDate[] dates = new java.time.LocalDate[n];
            double max = data.values().stream()
                    .mapToDouble(java.math.BigDecimal::doubleValue).max().orElse(0);
            if (max <= 0) {
                max = 1; // all-zero week: flat line near the bottom
            }
            int i = 0;
            for (java.util.Map.Entry<java.time.LocalDate, java.math.BigDecimal> e
                    : data.entrySet()) {
                float px = padX + (w - 2 * padX) * i / (float) (n - 1);
                float py = yBase - (float) ((plotH - 2) * (e.getValue().doubleValue() / max));
                pts[i] = new Point2D.Float(px, py);
                dates[i] = e.getKey();
                i++;
            }

            // Smooth curve through the points (Catmull-Rom to beziers).
            Path2D.Float curve = new Path2D.Float();
            curve.moveTo(pts[0].x, pts[0].y);
            for (int k = 0; k < n - 1; k++) {
                Point2D.Float p0 = pts[Math.max(0, k - 1)];
                Point2D.Float p1 = pts[k];
                Point2D.Float p2 = pts[k + 1];
                Point2D.Float p3 = pts[Math.min(n - 1, k + 2)];
                curve.curveTo(
                        p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f,
                        p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f,
                        p2.x, p2.y);
            }

            // Light-blue area shading under the curve.
            Path2D.Float area = new Path2D.Float(curve);
            area.lineTo(pts[n - 1].x, yBase);
            area.lineTo(pts[0].x, yBase);
            area.closePath();
            g2.setColor(new Color(0xd8, 0xec, 0xf7));
            g2.fill(area);

            // Thin dark-blue line.
            g2.setColor(LINE);
            g2.setStroke(new java.awt.BasicStroke(1.8f, java.awt.BasicStroke.CAP_ROUND,
                    java.awt.BasicStroke.JOIN_ROUND));
            g2.draw(curve);

            // Day names below: Sun Mon Tue Wed Thu Fri Sat (bilingual).
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            g2.setColor(LABEL);
            i = 0;
            for (java.time.LocalDate d : data.keySet()) {
                String day = dairy.erp.util.I18n.t(d.getDayOfWeek().name().substring(0, 1)
                        + d.getDayOfWeek().name().substring(1, 3).toLowerCase());
                int tw = g2.getFontMetrics().stringWidth(day);
                g2.drawString(day, pts[i].x - tw / 2f, yBase + labelH - 3);
                i++;
            }
            g2.dispose();
        }
    }
}
