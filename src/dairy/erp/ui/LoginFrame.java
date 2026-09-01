package dairy.erp.ui;

import dairy.erp.config.AppConfig;
import dairy.erp.service.AuthenticationService;
import dairy.erp.service.SettingsService;
import dairy.erp.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Application login screen, restyled after the "SRS Dairy" branded mock-up:
 * a deep-navy gradient backdrop with a milk wave and subtle cow silhouettes,
 * a rounded brand card (logo badge, product title) and a glass form panel
 * holding Username, Password (with show/hide toggle), Shift selection
 * (Morning / Evening), Language, a "Remember Me" option, a full-width
 * gradient Login button and a footer with a live date/time stamp and Exit.
 * <p>
 * Credentials are validated via {@link AuthenticationService}; the selected
 * shift, language and remember-me choice are persisted to the settings table
 * on successful login (the shift is picked up by the milk-collection screen
 * as its {@code app.default_shift}). On success the {@code onLogin} callback
 * is invoked so the controller can open the main dashboard.
 */
public class LoginFrame extends JFrame {

    // ---- palette -------------------------------------------------------
    private static final Color SKY_TOP = new Color(0x0b, 0x24, 0x3d);
    private static final Color SKY_BOTTOM = new Color(0x14, 0x3a, 0x5c);
    private static final Color CARD_BG = new Color(0x12, 0x33, 0x51, 236);
    private static final Color CARD_BORDER = new Color(0x7f, 0xb6, 0xe3, 190);
    private static final Color GLASS_BG = new Color(0xd8, 0xe3, 0xed, 235);
    private static final Color LABEL_FG = new Color(0x17, 0x2a, 0x3c);
    private static final Color FIELD_BG = new Color(0xfb, 0xfd, 0xfe);
    private static final Color FIELD_BORDER = new Color(0xa9, 0xbd, 0xcc);
    private static final Color ACCENT = new Color(0x1b, 0x5f, 0x9e);
    private static final Color ACCENT_DARK = new Color(0x11, 0x3c, 0x66);
    private static final Color SHIFT_SELECTED = new Color(0xc9, 0xe2, 0xf6);
    private static final Color MILK_WHITE = new Color(0xf6, 0xf9, 0xfc);

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("dd.MM.yyyy, hh:mm a");

    // ---- components ----------------------------------------------------
    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JCheckBox rememberBox = new JCheckBox("Remember Me");
    private final JComboBox<String> languageBox =
            new JComboBox<>(new String[]{dairy.erp.util.I18n.ENGLISH, dairy.erp.util.I18n.HINDI});
    private final JButton morningButton = new JButton("\u2600 Morning");
    private final JButton eveningButton = new JButton("\uD83C\uDF19 Evening");
    private final JButton eyeButton = new JButton("\uD83D\uDC41");
    private final JLabel timeLabel = new JLabel("", SwingConstants.CENTER);

    private final AuthenticationService authenticationService = new AuthenticationService();
    private final SettingsService settingsService = new SettingsService();
    private Consumer<String> onLogin;

    /** Creates and lays out the login window (not yet visible). */
    public LoginFrame() {
        super(AppConfig.APP_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildUi();
        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    // =====================================================================
    // UI construction
    // =====================================================================

    private void buildUi() {
        BackgroundPanel backdrop = new BackgroundPanel();
        backdrop.setLayout(new GridBagLayout());
        setContentPane(backdrop);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        backdrop.add(buildBrandCard(), gbc);

        // Username is pre-filled only when the user chose "Remember me";
        // otherwise both fields stay empty — no hard-coded credentials in code.
        usernameField.setText(resolveStoredUser());
        rememberBox.setSelected(settingBool("app.remember_user", true));

        String savedLanguage = setting("app.language");
        if (!savedLanguage.isBlank()) {
            languageBox.setSelectedItem(savedLanguage);
        }
        // Apply the saved language to this screen immediately, and re-apply
        // whenever the user flips the selector (bilingual English / Hindi).
        dairy.erp.util.I18n.setLanguage(savedLanguage.isBlank()
                ? dairy.erp.util.I18n.ENGLISH : savedLanguage);
        dairy.erp.util.I18n.apply(getContentPane());
        languageBox.addActionListener(e -> {
            dairy.erp.util.I18n.setLanguage(String.valueOf(languageBox.getSelectedItem()));
            dairy.erp.util.I18n.apply(getContentPane());
        });
        if ("Evening".equals(setting("app.default_shift"))) {
            restyleShift(eveningButton, morningButton);
        } else {
            restyleShift(morningButton, eveningButton);
        }

        // Live clock in the footer.
        updateTimeLabel();
        new Timer(10_000, e -> updateTimeLabel()).start();
    }

    /** Reads a setting, tolerating a not-yet-initialised database. */
    private static String setting(String key) {
        try {
            return new SettingsService().get(key);
        } catch (RuntimeException e) {
            return "";
        }
    }

    /** Reads a boolean setting, tolerating a not-yet-initialised database. */
    private static boolean settingBool(String key, boolean fallback) {
        try {
            return new SettingsService().getBoolean(key, fallback);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    /** Resolves the username to pre-fill: the remembered user, or blank. */
    private String resolveStoredUser() {
        if (settingBool("app.remember_user", false)) {
            String remembered = setting("app.login_user");
            if (!remembered.isBlank()) {
                return remembered;
            }
        }
        return "";
    }

    /**
     * Builds the rounded brand card: logo badge, product title/tagline, the
     * glass form panel and the footer (timestamp + Exit).
     */
    private JComponent buildBrandCard() {
        JPanel card = roundedPanel(CARD_BG, 20);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 2, true),
                new EmptyBorder(0, 34, 22, 34)));
        card.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 2, 0);
        card.add(buildLogoBadge(), gbc);

        JLabel title = new JLabel("SRS Dairy ERP");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
        title.setForeground(Color.WHITE);
        title.setHorizontalAlignment(JLabel.CENTER);
        gbc.gridy = 1;
        gbc.insets = new Insets(6, 0, 0, 0);
        card.add(title, gbc);

        JLabel tagline = new JLabel("Milk Collection & Management System");
        tagline.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        tagline.setForeground(new Color(0xdc, 0xe9, 0xf4));
        tagline.setHorizontalAlignment(JLabel.CENTER);
        gbc.gridy = 2;
        gbc.insets = new Insets(2, 0, 14, 0);
        card.add(tagline, gbc);

        gbc.gridy = 3;
        card.add(buildFormPanel(), gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(14, 0, 0, 0);
        card.add(buildFooter(), gbc);

        return card;
    }

    /** The elevated rounded logo badge at the top of the card. */
    private JComponent buildLogoBadge() {
        JPanel badge = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, ACCENT, getWidth(), getHeight(), ACCENT_DARK));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                g2.setColor(new Color(255, 255, 255, 60));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 18, 18));
                g2.dispose();
            }
        };
        badge.setOpaque(false);
        badge.setPreferredSize(new Dimension(190, 148));

        ImageIcon logo = UIUtil.loadLogoByHeight(96, 150);
        if (logo != null) {
            badge.add(new JLabel(logo));
        } else {
            JLabel fallback = new JLabel("SRS DAIRY", SwingConstants.CENTER);
            fallback.setForeground(Color.WHITE);
            fallback.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
            badge.add(fallback);
        }
        return badge;
    }

    /** The translucent glass panel that holds all form rows. */
    private JComponent buildFormPanel() {
        JPanel form = roundedPanel(GLASS_BG, 14);
        form.setLayout(new GridBagLayout());
        form.setBorder(new EmptyBorder(16, 18, 16, 18));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;

        // Username --------------------------------------------------------
        addRow(form, gbc, "Username", fieldWithIcon(usernameField, "\uD83D\uDC64", null));

        // Password + eye toggle inside the pill ----------------------------
        passwordField.setEchoChar('\u25CF');
        styleEyeButton();
        addRow(form, gbc, "Password", fieldWithIcon(passwordField, "\uD83D\uDD12", eyeButton));

        // Shift selection -------------------------------------------------
        JPanel shiftRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        shiftRow.setOpaque(false);
        styleShiftButton(morningButton);
        styleShiftButton(eveningButton);
        shiftRow.add(morningButton);
        shiftRow.add(eveningButton);
        JLabel shiftHelp = new JLabel(" (?)");
        shiftHelp.setForeground(new Color(0x5a, 0x6d, 0x7d));
        shiftHelp.setToolTipText("Default shift pre-selected on the milk collection screen after login.");
        shiftRow.add(shiftHelp);
        addRow(form, gbc, "Shift Selection", shiftRow);

        // Language --------------------------------------------------------
        languageBox.setBackground(Color.WHITE);
        languageBox.setPreferredSize(new Dimension(330, 34));
        addRow(form, gbc, "Language", languageBox);

        // Remember me -----------------------------------------------------
        rememberBox.setOpaque(false);
        rememberBox.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        rememberBox.setForeground(LABEL_FG);
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 8, 2, 6);
        form.add(rememberBox, gbc);
        gbc.gridwidth = 1;
        gbc.gridy++;

        // Login button (full width, like the reference) --------------------
        JButton loginButton = gradientButton("Login  \u2713");
        loginButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 21));
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(8, 6, 2, 6);
        form.add(loginButton, gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        getRootPane().setDefaultButton(loginButton);

        // Behaviour -------------------------------------------------------
        loginButton.addActionListener(e -> attemptLogin());
        eyeButton.addActionListener(e -> togglePasswordVisibility());
        morningButton.addActionListener(e -> restyleShift(morningButton, eveningButton));
        eveningButton.addActionListener(e -> restyleShift(eveningButton, morningButton));
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());

        return form;
    }

    /** Footer with the live date/time stamp and the Exit button. */
    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        timeLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        timeLabel.setForeground(new Color(0xd5, 0xe4, 0xf1));
        footer.add(timeLabel, BorderLayout.CENTER);

        JButton exit = new JButton("Exit");
        exit.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        exit.setBackground(MILK_WHITE);
        exit.setForeground(ACCENT);
        exit.setFocusPainted(false);
        exit.setOpaque(true);
        exit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exit.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 2, true),
                new EmptyBorder(4, 18, 4, 18)));
        exit.addActionListener(e -> System.exit(0));
        footer.add(exit, BorderLayout.EAST);
        return footer;
    }

    // =====================================================================
    // Small builders / styling helpers
    // =====================================================================

    /** Rounded, translucent panel used for the card and glass form. */
    private static JPanel roundedPanel(Color bg, int radius) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        return panel;
    }

    /** Adds a "label + field" row into the glass form's grid. */
    private static void addRow(JPanel form, GridBagConstraints gbc, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(6, 6, 6, 10);
        JLabel l = new JLabel(label);
        l.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        l.setForeground(LABEL_FG);
        form.add(l, gbc);
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(6, 0, 6, 6);
        form.add(field, gbc);
        gbc.gridy++;
    }

    /** Wraps a text component in a bordered white pill with a leading icon. */
    private static JComponent fieldWithIcon(JComponent field, String icon, JComponent trailing) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(FIELD_BG);
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        iconLabel.setForeground(new Color(0x51, 0x68, 0x7a));
        iconLabel.setBorder(new EmptyBorder(0, 10, 0, 4));
        wrap.add(iconLabel, BorderLayout.WEST);
        if (field instanceof JTextField tf) {
            tf.setBorder(new EmptyBorder(7, 4, 7, 8));
            tf.setOpaque(true);
            tf.setBackground(FIELD_BG);
        }
        wrap.add(field, BorderLayout.CENTER);
        if (trailing != null) {
            trailing.setOpaque(false);
            trailing.setBorder(new EmptyBorder(0, 2, 0, 6));
            wrap.add(trailing, BorderLayout.EAST);
        }
        wrap.setPreferredSize(new Dimension(330, 36));
        wrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER, 1, true),
                new EmptyBorder(0, 2, 0, 8)));
        return wrap;
    }

    /** Flat, hand-cursor toggle for password visibility. */
    private void styleEyeButton() {
        eyeButton.setContentAreaFilled(false);
        eyeButton.setBorderPainted(false);
        eyeButton.setFocusPainted(false);
        eyeButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        eyeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        eyeButton.setToolTipText("Show / hide password");
    }

    /** Toggles the password field between masked and plain text. */
    private void togglePasswordVisibility() {
        boolean hidden = passwordField.getEchoChar() != 0;
        passwordField.setEchoChar(hidden ? (char) 0 : '\u25CF');
        eyeButton.setText(hidden ? "\uD83D\uDE48" : "\uD83D\uDC41");
    }

    /** Applies the neutral (unselected) styling to a shift toggle button. */
    private static void styleShiftButton(JButton b) {
        b.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(Color.WHITE);
        b.setForeground(LABEL_FG);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER, 1, true),
                new EmptyBorder(5, 12, 5, 12)));
    }

    /** Highlights the selected shift button and resets the other one. */
    private static void restyleShift(JButton selected, JButton other) {
        selected.setBackground(SHIFT_SELECTED);
        selected.setForeground(new Color(0x0e, 0x30, 0x50));
        selected.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 2, true),
                new EmptyBorder(4, 11, 4, 11)));
        styleShiftButton(other);
    }

    /** Blue gradient pill button (the primary Login action). */
    private static JButton gradientButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Color top = getModel().isRollover()
                        ? new Color(0x2c, 0x7c, 0xc2) : ACCENT;
                g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), ACCENT_DARK));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setBorder(new EmptyBorder(6, 24, 6, 24));
        button.setOpaque(false);
        button.setForeground(Color.WHITE);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    /** Refreshes the footer timestamp (dd.MM.yyyy, hh:mm AM/PM). */
    private void updateTimeLabel() {
        timeLabel.setText(LocalDateTime.now().format(STAMP).toUpperCase(Locale.ROOT));
    }

    // =====================================================================
    // Login logic (unchanged behaviour + preference persistence)
    // =====================================================================

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isBlank() || password.isBlank()) {
            UIUtil.showMessage(this,
                    "Please enter both username and password.",
                    "Login", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            String authenticated = authenticationService.authenticate(username, password);
            if (authenticated == null) {
                UIUtil.showMessage(this,
                        "Invalid username or password.",
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
                passwordField.setText("");
                passwordField.requestFocusInWindow();
            } else if (onLogin != null) {
                persistPreferences(username);
                dispose();
                onLogin.accept(authenticated);
            }
        } catch (RuntimeException ex) {
            UIUtil.showMessage(this,
                    "Login could not be completed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Persists shift / language / remember-me choices for the next session. */
    private void persistPreferences(String username) {
        try {
            Map<String, String> updates = new LinkedHashMap<>();
            updates.put("app.default_shift",
                    SHIFT_SELECTED.equals(eveningButton.getBackground()) ? "Evening" : "Morning");
            updates.put("app.language", String.valueOf(languageBox.getSelectedItem()));
            updates.put("app.remember_user", String.valueOf(rememberBox.isSelected()));
            updates.put("app.login_user", rememberBox.isSelected() ? username : "");
            settingsService.saveAll(updates);
        } catch (RuntimeException ignored) {
            // Preference persistence must never block a successful login.
        }
    }

    /** Registers the callback invoked after successful login. */
    public void setOnLogin(Consumer<String> onLogin) {
        this.onLogin = onLogin;
    }

    // =====================================================================
    // Decorated backdrop: navy gradient, milk wave and cow silhouettes
    // =====================================================================

    /**
     * The full-window backdrop painted to match the reference mock-up: a
     * vertical navy gradient, faint low-poly cow silhouettes in the corners
     * and a soft white "milk wave" rising across the lower third.
     */
    private static final class BackgroundPanel extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            // Sky gradient.
            g2.setPaint(new GradientPaint(0, 0, SKY_TOP, 0, h, SKY_BOTTOM));
            g2.fillRect(0, 0, w, h);

            // Faint cow silhouettes in the sky (mirrored pair in the corners).
            drawCow(g2, w - 230, 30, 1.6, new Color(255, 255, 255, 16), true);
            drawCow(g2, 26, 55, 1.2, new Color(255, 255, 255, 14), false);

            // Milk wave across the bottom third.
            GeneralPath wave = new GeneralPath();
            int crest = h - 200;
            wave.moveTo(0, crest + 40);
            wave.curveTo(w * 0.18, crest - 30, w * 0.34, crest + 70, w * 0.52, crest + 20);
            wave.curveTo(w * 0.70, crest - 25, w * 0.86, crest + 45, w, crest - 10);
            wave.lineTo(w, h);
            wave.lineTo(0, h);
            wave.closePath();
            g2.setColor(MILK_WHITE);
            g2.fill(wave);

            // Small cows standing on the wave, painted after it so they stay
            // visible in a soft blue-grey against the milk white.
            Color waveCow = new Color(0xbf, 0xcf, 0xdf);
            drawCow(g2, 30, h - 175, 1.0, waveCow, false);
            drawCow(g2, w - 180, h - 160, 0.9, waveCow, true);

            g2.dispose();
        }

        /**
         * Draws a simple low-poly standing cow built from polygons, mirrored
         * when {@code flip} is set. Purely decorative.
         */
        private static void drawCow(Graphics2D g2, int x, int y, double scale, Color color, boolean flip) {
            Graphics2D cow = (Graphics2D) g2.create();
            cow.translate(x, y);
            if (flip) {
                cow.scale(-scale, scale);
            } else {
                cow.scale(scale, scale);
            }
            cow.setColor(color);

            // Body.
            int[] bx = {0, 46, 104, 122, 118, 96, 40, 8};
            int[] by = {44, 28, 24, 42, 66, 74, 76, 62};
            cow.fillPolygon(bx, by, bx.length);

            // Head.
            int[] hx = {104, 134, 146, 140, 118};
            int[] hy = {26, 16, 38, 62, 48};
            cow.fillPolygon(hx, hy, hx.length);

            // Ears + horns.
            cow.fillPolygon(new int[]{132, 146, 152}, new int[]{14, 2, 14}, 3);
            cow.fillPolygon(new int[]{104, 96, 100}, new int[]{24, 10, 30}, 3);

            // Legs.
            cow.fillRect(14, 72, 10, 46);
            cow.fillRect(42, 74, 10, 44);
            cow.fillRect(84, 72, 10, 46);
            cow.fillRect(106, 66, 10, 48);

            // Udder highlight.
            cow.fillOval(62, 64, 22, 12);
            cow.dispose();
        }
    }
}
