package dairy.erp.ui.dialogs;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CalendarDialog extends JDialog {

    private static final Color BG              = Color.WHITE;
    private static final Color GREEN           = new Color(0x0b, 0x7a, 0x3e);
    private static final Color HEADER_BG        = new Color(0x1a, 0x5f, 0x7a);
    private static final Color HEADER_FG        = Color.WHITE;
    private static final Color WEEK_A           = new Color(0xE8, 0xF4, 0xF8);
    private static final Color WEEK_B           = new Color(0xF0, 0xF7, 0xFB);
    private static final Color SELECTED_BG      = new Color(0x1a, 0x5f, 0x7a);
    private static final Color SELECTED_FG      = Color.WHITE;
    private static final Color TODAY_BG         = new Color(0xE8, 0xF5, 0xE9);
    private static final Color TODAY_BORDER    = GREEN;
    private static final Color SUNDAY_FG        = new Color(0xD3, 0x2F, 0x2F);
    private static final Color SAT_FG           = new Color(0x15, 0x65, 0xC0);
    private static final Color OTHER_FG         = new Color(0x37, 0x47, 0x5F);
    private static final Color EMPTY_BG         = new Color(0xF5, 0xF7, 0xFA);
    private static final Color DISABLED_BG     = new Color(0xEE, 0xEE, 0xEE);
    private static final Color DISABLED_FG     = new Color(0xBB, 0xBB, 0xBB);
    private static final Color WEEKDAY_HDR      = HEADER_BG;
    private static final Color WEEK_BORDER_A    = new Color(0x1a, 0x5f, 0x7a);
    private static final Color WEEK_BORDER_B    = new Color(0x8B, 0xC4, 0xD8);
    private static final Color OUTER_BORDER     = new Color(0xD0, 0xE6, 0xED);
    private static final Color NAV_HOVER        = new Color(0x14, 0x48, 0x60);
    private static final Color BTN_BG            = new Color(0x0b, 0x6a, 0x5a);
    private static final Color BTN_HOVER2        = new Color(0x0D, 0x58, 0x4C);
    private static final Color BTN_BORDER        = new Color(0x0D, 0x4A, 0x3C);

    private static final Font FONT_MONTH    = new Font("Arial", Font.BOLD, 20);
    private static final Font FONT_WEEKDAY  = new Font("Arial", Font.BOLD, 14);
    private static final Font FONT_DAY      = new Font("Arial", Font.BOLD, 14);
    private static final Font FONT_NAV      = new Font("Arial", Font.BOLD, 18);

    private static final int CELL   = 44;
    private static final int HEADER = 52;
    private static final int FOOTER = 42;
    private static final int COLS   = 7;

    private YearMonth currentMonth;
    private LocalDate selectedDate;
    private final JLabel monthLabel;
    private final List<JPanel> weekRows = new ArrayList<>(6);

    // ===== Validation =====
    private final LocalDate maxDate;
    private final LocalDate minDate;

    // Opens calendar restricted to dates on or before today (no future dates).
    public CalendarDialog(Frame owner, LocalDate initial) {
        this(owner, initial, null, LocalDate.now());
    }

    // Opens calendar with custom date range.
    public CalendarDialog(Frame owner, LocalDate initial, LocalDate min, LocalDate max) {
        super(owner, "Select Date", true);
        this.minDate = min;
        this.maxDate = max;
        LocalDate today = LocalDate.now();
        LocalDate earliest = (min != null) ? min : LocalDate.of(2000, 1, 1);
        LocalDate latest   = (max != null) ? max : LocalDate.of(2099, 12, 31);
        if (initial != null && !initial.isBefore(earliest) && !initial.isAfter(latest)) {
            this.selectedDate = initial;
        } else if (!today.isBefore(earliest) && !today.isAfter(latest)) {
            this.selectedDate = today;
        } else {
            this.selectedDate = earliest;
        }
        this.currentMonth = YearMonth.from(this.selectedDate);
        this.monthLabel   = new JLabel("", SwingConstants.CENTER);
        buildUi();
        render();
        // Bilingual support: translate this dialog into the chosen language.
        setTitle(dairy.erp.util.I18n.t("Select Date"));
        dairy.erp.util.I18n.apply(getContentPane());
        int h = HEADER + FOOTER + (CELL + 2) * 6 + 14;
        setSize(332, h);
        setLocationRelativeTo(owner);
        setResizable(false);
    }
    // Opens calendar restricted to dates on or before today (no future dates).

    public LocalDate getSelectedDate() { return selectedDate; }

    private boolean isDateAllowed(LocalDate date) {
        if (minDate != null && date.isBefore(minDate)) return false;
        if (maxDate != null && date.isAfter(maxDate))  return false;
        return true;
    }

    private void buildUi() {
        getContentPane().setBackground(BG);
        getContentPane().setLayout(new BorderLayout(0, 0));

        // ========== HEADER ==========
        JPanel header = new JPanel(new BorderLayout(0, 0));
        header.setBackground(HEADER_BG);
        header.setPreferredSize(new Dimension(340, HEADER));
        header.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));

        // Navigation buttons
        JButton prevYear  = navBtn("«", e -> { currentMonth = currentMonth.minusYears(1);  render(); });
        JButton prevMonth = navBtn("<",  e -> { currentMonth = currentMonth.minusMonths(1); render(); });
        JButton nextMonth = navBtn(">",  e -> { currentMonth = currentMonth.plusMonths(1);  render(); });
        JButton nextYear  = navBtn("»", e -> { currentMonth = currentMonth.plusYears(1);   render(); });

        JPanel leftNav  = new JPanel(new FlowLayout(FlowLayout.LEFT,  1, 4));
        JPanel rightNav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 1, 4));
        leftNav.setOpaque(false);  rightNav.setOpaque(false);
        leftNav.add(prevYear);  leftNav.add(prevMonth);
        rightNav.add(nextMonth); rightNav.add(nextYear);

        monthLabel.setFont(FONT_MONTH);
        monthLabel.setForeground(HEADER_FG);
        monthLabel.setText("Month Year");

        header.add(leftNav,    BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(rightNav,   BorderLayout.EAST);

        getContentPane().add(header, BorderLayout.NORTH);

        // ========== WEEKDAY HEADERS ==========
        JPanel weekdayRow = new JPanel(new GridLayout(1, COLS, 1, 0));
        weekdayRow.setPreferredSize(new Dimension(340, FOOTER));
        weekdayRow.setBackground(new Color(0xD0, 0xE8, 0xF0));
        weekdayRow.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x8B, 0xC4, 0xD8)));
        String[] shortNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < COLS; i++) {
            JLabel lbl = new JLabel(shortNames[i], SwingConstants.CENTER);
            lbl.setFont(FONT_WEEKDAY);
            lbl.setForeground(i == 0 ? SUNDAY_FG : (i == 6 ? SAT_FG : WEEKDAY_HDR));
            weekdayRow.add(lbl);
        }
                    
        // ========== DAYS CONTAINER ==========
        JPanel daysContainer = new JPanel(new BorderLayout(0, 1));
        daysContainer.setBackground(OUTER_BORDER);
        daysContainer.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        daysContainer.add(weekdayRow, BorderLayout.NORTH);

        JPanel weeksPanel = new JPanel(new GridLayout(0, 1, 1, 1));
        weeksPanel.setBackground(OUTER_BORDER);
        for (int i = 0; i < 6; i++) {
            Color rowBorder = (i % 2 == 0) ? WEEK_BORDER_A : WEEK_BORDER_B;
            JPanel weekRow = new JPanel(new GridLayout(1, COLS, 1, 1));
            weekRow.setBackground(i % 2 == 0 ? WEEK_A : WEEK_B);
            weekRow.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, rowBorder),
                    BorderFactory.createEmptyBorder(0, 1, 0, 1)));
            for (int d = 0; d < COLS; d++) weekRow.add(createEmptyCell());
            weeksPanel.add(weekRow);
            weekRows.add(weekRow);
        }
        daysContainer.add(weeksPanel, BorderLayout.CENTER);

        // ========== FOOTER ==========
        JButton todayBtn = new JButton("Today");
        todayBtn.setFont(new Font("Arial", Font.BOLD, 15));
        todayBtn.setForeground(Color.WHITE);
        todayBtn.setOpaque(true);
        todayBtn.setBackground(GREEN);
        todayBtn.setFocusPainted(false);
        todayBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GREEN.darker(), 1),
                BorderFactory.createEmptyBorder(4, 14, 4, 14)));
        todayBtn.addActionListener(e -> {
            selectedDate = LocalDate.now();
            currentMonth = YearMonth.from(selectedDate);
            dispose();
        });
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 6));
        footer.setBackground(new Color(0xD0, 0xE8, 0xF0)); footer.add(todayBtn);

        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setBackground(BG);
        center.add(daysContainer, BorderLayout.CENTER);
        getContentPane().add(center, BorderLayout.CENTER);
        getContentPane().add(footer, BorderLayout.SOUTH);
    }

    private JButton navBtn(String text, java.awt.event.ActionListener al) {
        JButton b = new JButton(text);
        b.setFont(FONT_NAV);
        b.setForeground(HEADER_FG);
        b.setBackground(BTN_BG);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorderPainted(true);
            b.setBorder(BorderFactory.createLineBorder(BTN_BORDER, 1));
        b.setPreferredSize(new Dimension(36, 36));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(BTN_HOVER2); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(BTN_BG); }
        });
        b.addActionListener(al);
        return b;
    }

    private JLabel createEmptyCell() {
        JLabel l = new JLabel("", SwingConstants.CENTER);
        l.setPreferredSize(new Dimension(CELL, CELL));
        l.setBackground(EMPTY_BG);
        l.setOpaque(true);
        return l;
    }

    private void render() {
        monthLabel.setText(
                currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                        + " " + currentMonth.getYear());
        int daysInMonth = currentMonth.lengthOfMonth();
        int startOffset = (currentMonth.atDay(1).getDayOfWeek().getValue() % 7);
        LocalDate today = LocalDate.now();
        int idx = 0;
        for (JPanel weekRow : weekRows) {
            for (int col = 0; col < COLS; col++) {
                int cellIdx = idx++;
                int dayNum  = cellIdx - startOffset + 1;
                if (dayNum < 1 || dayNum > daysInMonth) {
                    weekRow.remove(col); weekRow.add(createEmptyCell(), col);
                } else {
                    LocalDate date = currentMonth.atDay(dayNum);
                    weekRow.remove(col); weekRow.add(dayBtn(date, today), col);
                }
            }
        }
        for (JPanel wp : weekRows) wp.revalidate();
        getContentPane().repaint();
    }

    private JButton dayBtn(LocalDate date, LocalDate today) {
        JButton b = new JButton(String.valueOf(date.getDayOfMonth()));
        b.setFont(FONT_DAY);
        b.setPreferredSize(new Dimension(CELL, CELL));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setFocusPainted(false);
        b.setBorderPainted(true);
            b.setBorder(BorderFactory.createLineBorder(BTN_BORDER, 1));
        b.setOpaque(true);
        b.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        boolean isSun      = (date.getDayOfWeek() == DayOfWeek.SUNDAY);
        boolean isSat      = (date.getDayOfWeek() == DayOfWeek.SATURDAY);
        boolean isToday    = date.equals(today);
        boolean isSelected = date.equals(selectedDate);
        boolean allowed    = isDateAllowed(date);
        if (!allowed) {
            b.setBackground(DISABLED_BG);
            b.setForeground(DISABLED_FG);
            b.setBorderPainted(true);
            b.setBorder(BorderFactory.createLineBorder(DISABLED_FG, 1));
            b.setEnabled(false);
            b.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
        } else if (isSelected) {
            b.setBackground(SELECTED_BG);
            b.setForeground(SELECTED_FG);
            b.setBorderPainted(true);
            b.setBorder(BorderFactory.createLineBorder(BTN_BORDER, 2));
        } else if (isToday) {
            b.setBackground(TODAY_BG);
            b.setForeground(TODAY_BORDER);
            b.setBorderPainted(true);
            b.setBorder(BorderFactory.createLineBorder(TODAY_BORDER, 2, true));
        } else {
            b.setBackground(Color.WHITE);
            b.setForeground(isSun ? SUNDAY_FG : (isSat ? SAT_FG : OTHER_FG));
        }
        b.addActionListener(e -> { if (isDateAllowed(date)) { selectedDate = date; dispose(); } });
        return b;
    }
}
