package dairy.erp.ui;

import dairy.erp.ui.dialogs.CalendarDialog;
import dairy.erp.util.DateUtil;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.time.LocalDate;

/**
 * A date picker field: a non-editable text field showing the chosen date plus
 * a button that opens the {@link CalendarDialog} for calendar selection.
 */
public class DatePicker extends JPanel {

    private final JTextField field = new JTextField(12);
    private final JButton button = new JButton("\u2026");

    public DatePicker() {
        super(new BorderLayout(6, 0));
        field.setEditable(false);
        button.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
        button.setText("📅");
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setBackground(new java.awt.Color(0x1A, 0x5F, 0x7A));
        button.setForeground(java.awt.Color.WHITE);
        button.setPreferredSize(new java.awt.Dimension(36, 28));
        button.addActionListener(e -> openDialog());
        add(field, BorderLayout.CENTER);
        add(button, BorderLayout.EAST);
    }

    protected void openDialog() {
        Frame owner = findOwner();
        CalendarDialog dlg = new CalendarDialog(owner, getDate());
        dlg.setVisible(true);
        LocalDate picked = dlg.getSelectedDate();
        if (picked != null) {
            setDate(picked);
        }
    }
    /**
     * Opens the calendar with optional date range.
     * @param min minimum selectable date (null = no restriction)
     * @param max maximum selectable date (null = no restriction)
     */
    public void openCalendar(java.time.LocalDate min, java.time.LocalDate max) {
        java.awt.Frame owner = findOwner();
        java.time.LocalDate current = getDate();
        CalendarDialog dlg;
        if (min == null && max == null) {
            dlg = new CalendarDialog(owner, current);
        } else {
            dlg = new CalendarDialog(owner, current, min, max);
        }
        dlg.setVisible(true);
        java.time.LocalDate picked = dlg.getSelectedDate();
        if (picked != null) {
            setDate(picked);
        }
    }



    public LocalDate getDate() {
        return DateUtil.parse(field.getText());
    }

    public void setDate(LocalDate date) {
        field.setText(DateUtil.toDisplay(date));
    }

    public void setFieldEditable(boolean editable) {
        field.setEditable(editable);
    }

    public JTextField getTextField() {
        return field;
    }

    public JButton getButton() {
        return button;
    }

    protected Frame findOwner() {
        Container c = this;
        while (c != null && !(c instanceof JFrame)) {
            c = c.getParent();
        }
        return (Frame) c;
    }
}
