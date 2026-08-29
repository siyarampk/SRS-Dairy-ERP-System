package dairy.erp.ui.dialogs;

import dairy.erp.service.AuthenticationService;
import dairy.erp.util.UIUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Lets the single admin user change their password. The current password must
 * be verified before the new one is stored.
 */
public class PasswordChangeDialog extends JDialog {

    private final String username;
    private final AuthenticationService authenticationService = new AuthenticationService();
    private final JPasswordField currentField = new JPasswordField(18);
    private final JPasswordField newField = new JPasswordField(18);
    private final JPasswordField confirmField = new JPasswordField(18);

    public PasswordChangeDialog(Frame owner, String username) {
        super(owner, "Change Password", true);
        this.username = username;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUi();
        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 14, 14, 14));

        // Brand-coloured header banner with the logged-in user on the right.
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIUtil.BRAND);
        header.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        JLabel title = new JLabel("Change Password");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);
        JLabel userLabel = new JLabel("User: " + username + "  ");
        userLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        userLabel.setForeground(new Color(0xd9, 0xe8, 0xf1));
        header.add(userLabel, BorderLayout.EAST);

        // Titled panel holding the fields, vertical labels (label above field).
        JPanel fields = new JPanel(new GridBagLayout());
        javax.swing.border.TitledBorder tb = BorderFactory.createTitledBorder("Password Details");
        tb.setTitleFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        tb.setTitleColor(UIUtil.BRAND);
        fields.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.BRAND),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(4, 8, 8, 8), tb)));
        addVerticalField(fields, 0, "Current Password:", currentField);
        addVerticalField(fields, 1, "New Password:", newField);
        addVerticalField(fields, 2, "Confirm New Password:", confirmField);
        UIUtil.styleComponent(currentField, 18);
        UIUtil.styleComponent(newField, 18);
        UIUtil.styleComponent(confirmField, 18);

        // Buttons in the app-standard colours: green Save, grey Cancel.
        JButton save = new JButton("Save");
        UIUtil.styleSmallButton(save, new Color(0x2E7D32)); // green
        JButton cancel = new JButton("Cancel");
        UIUtil.styleSmallButton(cancel, new Color(0x607D8B)); // grey
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        buttons.add(save);
        buttons.add(cancel);

        root.add(header, BorderLayout.NORTH);
        root.add(fields, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);

        add(root);
        getRootPane().setDefaultButton(save);

        save.addActionListener(e -> savePassword());
        cancel.addActionListener(e -> dispose());
    }

    /** One label stacked directly above its password field, full width. */
    private void addVerticalField(JPanel panel, int row, String label, JPasswordField field) {
        GridBagConstraints g = new GridBagConstraints();
        g.anchor = GridBagConstraints.WEST;
        g.gridx = 0;
        g.gridy = row * 2;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(8, 10, 2, 10);
        JLabel l = new JLabel(label);
        l.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        panel.add(l, g);
        g.gridy = row * 2 + 1;
        g.insets = new Insets(2, 10, 4, 10);
        panel.add(field, g);
    }

    private void savePassword() {
        String current = new String(currentField.getPassword());
        String next = new String(newField.getPassword());
        String confirm = new String(confirmField.getPassword());
        String result = authenticationService.changePassword(username, current, next, confirm);
        if (result.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password changed successfully.",
                    "Change Password", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, result,
                    "Change Password", JOptionPane.WARNING_MESSAGE);
        }
    }
}
