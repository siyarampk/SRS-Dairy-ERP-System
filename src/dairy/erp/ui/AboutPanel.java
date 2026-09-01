package dairy.erp.ui;

import dairy.erp.config.AppConfig;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Branded About dialog for SRS Dairy ERP.
 * Displays the product logo, version, developer, environment information and
 * action buttons (Check for Updates, Copy Diagnostics, Close) in a clean,
 * modern layout that matches the application colour scheme.
 */
public class AboutPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Color WHITE_80 = new Color(255, 255, 255, 204);
    private static final Color GREEN_LINK = new Color(0x2e, 0x7d, 0x32);

    /**
     * Creates and shows the about dialog.
     *
     * @param parent the parent frame for modality
     */
    public static void showAboutDialog(java.awt.Frame parent) {
        JDialog dialog = new JDialog(parent,
                dairy.erp.util.I18n.t("About SRS Dairy ERP"), true);
        dialog.setUndecorated(true);
        dialog.add(new AboutPanel());
        // Bilingual support: translate the dialog contents.
        dairy.erp.util.I18n.apply(dialog);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        dialog.dispose();
    }

    private JLabel copyStatusLabel;

    public AboutPanel() {
                setLayout(new BorderLayout(0, 0));
        setBackground(Color.WHITE);

        // ---- Header: blue bar with white title + X close ----
        JPanel header = new JPanel(new BorderLayout()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color from = new Color(0x1a, 0x5f, 0x7a);
                Color to = new Color(0x2a, 0x7a, 0x9a);
                GradientPaint gp = new GradientPaint(0, 0, from, getWidth(), 0, to);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setPreferredSize(new Dimension(560, 46));
        header.setBorder(new EmptyBorder(0, 20, 0, 14));

        JLabel titleLabel = new JLabel("About SRS Dairy ERP", SwingConstants.LEFT);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        header.add(titleLabel, BorderLayout.WEST);

        // X close button
        JButton closeBtn = new JButton("X");
        styleCloseButton(closeBtn);
        closeBtn.addActionListener(e -> SwingUtilities.getWindowAncestor(this).dispose());
        JPanel closeWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        closeWrapper.setOpaque(false);
        closeWrapper.add(closeBtn);
        header.add(closeWrapper, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // ---- Content ----
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(24, 40, 16, 40));
        content.setBackground(Color.WHITE);

                // Logo + product info
                content.add(buildLogoSection());
        content.add(Box.createVerticalStrut(14));

        // Separator
        content.add(buildSeparator());
        content.add(Box.createVerticalStrut(18));

        // Developer & Support
        content.add(buildSectionTitle("DEVELOPER & SUPPORT"));
        content.add(buildInfoRow("Company:", "SRS Pvt. Ltd."));
        content.add(buildInfoRow("Developer:", "Siyaram Meena"));
        content.add(buildInfoRow("Support Email:", "siyarampk@gmail.com"));
        content.add(Box.createVerticalStrut(18));

        // Environment & System Info
        content.add(buildSectionTitle("ENVIRONMENT & SYSTEM INFO"));
        String javaVersion = System.getProperty("java.version", "Unknown");
        content.add(buildInfoRow("Runtime:", "Java Runtime Environment " + javaVersion));
        content.add(buildInfoRow("Database:", "SQLite / Embedded (Connected)"));
        content.add(buildInfoRow("User Session:", getUsername()));
        content.add(Box.createVerticalStrut(18));

        // Separator
        content.add(buildSeparator());
        content.add(Box.createVerticalStrut(14));

        // Copyright footer
        JLabel copyright = new JLabel("© 2026 SRS Pvt. Ltd. All rights reserved.");
        copyright.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        copyright.setForeground(new Color(0x66, 0x66, 0x66));
        content.add(copyright);
        content.add(Box.createVerticalStrut(10));

        add(content, BorderLayout.CENTER);

        // ---- Bottom bar with buttons ----
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 12));
        bottomBar.setBackground(Color.WHITE);
        bottomBar.setBorder(new EmptyBorder(0, 0, 16, 16));

        JButton checkUpdates = new JButton("Check for Updates");
        styleSecondaryButton(checkUpdates);
        checkUpdates.addActionListener(e -> openUpdateLink());

        JButton copyBtn = new JButton("Copy Diagnostics");
        stylePrimaryButton(copyBtn);
        copyBtn.addActionListener(e -> copyDiagnostics());

        JButton closeBtn2 = new JButton("Close");
        styleSecondaryButton(closeBtn2);
        closeBtn2.addActionListener(e -> SwingUtilities.getWindowAncestor(this).dispose());

        bottomBar.add(checkUpdates);
        bottomBar.add(copyBtn);
        bottomBar.add(closeBtn2);

                add(bottomBar, BorderLayout.SOUTH);
    }

    private JPanel buildLogoSection() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.add(Box.createHorizontalGlue());

        JLabel logo = null;
        try {
            String projectRoot = System.getProperty("user.dir");
            File logoFile = new File(projectRoot, "resources/images/logo.png");
            if (logoFile.exists()) {
                BufferedImage raw = javax.imageio.ImageIO.read(logoFile);
                int sz = 96;
                Image scaled = raw.getScaledInstance(sz, sz, Image.SCALE_SMOOTH);
                logo = new JLabel(new ImageIcon(scaled));
            }
        } catch (Exception ignored) {
        }
        if (logo == null) {
            logo = new LogoCircle();
        }

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(new EmptyBorder(0, 18, 0, 0));

        JLabel nameLabel = new JLabel(AppConfig.APP_PRODUCT_NAME);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        nameLabel.setForeground(AppConfig.BRAND_COLOR);
        info.add(nameLabel);

        JLabel taglineLabel = new JLabel(AppConfig.APP_TAGLINE);
        taglineLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        taglineLabel.setForeground(new Color(0x55, 0x55, 0x55));
        info.add(taglineLabel);

        JLabel versionLabel = new JLabel("Version " + AppConfig.APP_VERSION + " (" + AppConfig.APP_BUILD + ")");
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        versionLabel.setForeground(new Color(0x77, 0x77, 0x77));
        info.add(versionLabel);

        info.add(Box.createVerticalStrut(6));

        p.add(logo);
        p.add(info);
        p.add(Box.createHorizontalGlue());
        return p;
    }

    private JPanel buildSeparator() {
        JPanel sep = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(0xdd, 0xdd, 0xdd));
                g.drawLine(0, 0, getWidth(), 0);
            }
        };
        sep.setPreferredSize(new Dimension(520, 1));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    private JLabel buildSectionTitle(String title) {
        JLabel l = new JLabel(title);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(AppConfig.BRAND_COLOR);
        return l;
    }

    private JPanel buildInfoRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(12, 4));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(6, 0, 0, 0));

        JLabel labelLabel = new JLabel("<html><b>" + label + "</b></html>");
        labelLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        labelLabel.setForeground(new Color(0x44, 0x44, 0x44));

        JLabel valueLabel = new JLabel("<html><div style='text-align: left; margin-left: 8px;'>" + value + "</div></html>");
        valueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        valueLabel.setForeground(new Color(0x33, 0x33, 0x33));

        row.add(labelLabel, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);
        return row;
    }

    private void copyDiagnostics() {
        String javaVersion = System.getProperty("java.version", "Unknown");
        String info =
                "SRS Dairy ERP Diagnostics\n" +
                "=========================\n" +
                "Product:    " + AppConfig.APP_PRODUCT_NAME + "\n" +
                "Tagline:    " + AppConfig.APP_TAGLINE + "\n" +
                "Version:    " + AppConfig.APP_VERSION + " (" + AppConfig.APP_BUILD + ")\n" +
                "Date:       " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "\n" +
                "User:       " + getUsername() + "\n" +
                "Java:       " + javaVersion + "\n" +
                "Database:   SQLite / Embedded (Connected)\n" +
                "Developer:  Siyaram Meena\n" +
                "Company:    SRS Pvt. Ltd.\n" +
                "Email:      siyarampk@gmail.com\n";

                StringSelection stringSelection = new StringSelection(info);
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
    }

    private String getUsername() {
        try {
            java.lang.reflect.Field f = DashboardPanel.class.getDeclaredField("username");
            f.setAccessible(true);
            return (String) f.get(null);
        } catch (Exception e) {
            return System.getProperty("user.name", "Unknown");
        }
    }

    private void openUpdateLink() {
        java.awt.Desktop desktop = java.awt.Desktop.isDesktopSupported() ? java.awt.Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
            try {
                desktop.browse(java.net.URI.create("https://github.com/siyarammeena/SRS-Dairy-ERP/releases"));
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    private void stylePrimaryButton(JButton btn) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(new Color(0x1a, 0x5f, 0x7a));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 18, 8, 18),
                BorderFactory.createEmptyBorder()));
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
    }

    private void styleSecondaryButton(JButton btn) {
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setBackground(new Color(0xf0, 0xf0, 0xf0));
        btn.setForeground(new Color(0x33, 0x33, 0x33));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 18, 8, 18),
                BorderFactory.createLineBorder(new Color(0xcc, 0xcc, 0xcc))));
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
    }

    private void styleCloseButton(JButton btn) {
        btn.setPreferredSize(new Dimension(24, 24));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(new Color(255, 255, 255, 204));
        btn.setBackground(new Color(0, 0, 0, 40));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(0, 0, 0, 70));
                btn.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(0, 0, 0, 40));
                btn.setForeground(new Color(255, 255, 255, 204));
            }
        });
    }

    // ---- Logo circle icon ----
    private static class LogoCircle extends JLabel {
        private static final long serialVersionUID = 1L;
        private static final int SIZE = 60;

        LogoCircle() {
            setPreferredSize(new Dimension(SIZE, SIZE));
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
            setToolTipText("SRS Dairy ERP");
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color from = new Color(0x1a, 0x5f, 0x7a);
            Color to = new Color(0x2a, 0x7a, 0x9a);
            GradientPaint gp = new GradientPaint(0, 0, from, SIZE, SIZE, to);
            g2.setPaint(gp);
            g2.fill(new Ellipse2D.Double(2, 2, SIZE - 4, SIZE - 4));

            g2.setColor(WHITE_80);
            g2.setStroke(new java.awt.BasicStroke(2.5f));
            g2.draw(new Ellipse2D.Double(3, 3, SIZE - 6, SIZE - 6));

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            String text = "SRS DAIRY";
            java.awt.FontMetrics fm = g2.getFontMetrics();
            int x = (SIZE - fm.stringWidth(text)) / 2 - 2;
            int y = SIZE / 2 + fm.getAscent() / 2 - 3;
            g2.drawString(text, x, y);

            g2.dispose();
        }
    }
}