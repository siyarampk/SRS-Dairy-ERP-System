package dairy.erp.ui;

import dairy.erp.model.Customer;
import dairy.erp.model.MilkCollection;
import dairy.erp.service.MilkCollectionService;
import dairy.erp.service.SettingsService;
import dairy.erp.util.CurrencyUtil;
import dairy.erp.util.DateUtil;
import dairy.erp.util.PrintUtil;
import dairy.erp.util.UIUtil;
import dairy.erp.util.ValidationUtil;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.border.Border;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.Box;
import java.awt.Insets;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterJob;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.time.format.DateTimeFormatter;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.Collection;
import java.awt.FontMetrics;
import java.awt.RenderingHints;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import javax.print.attribute.standard.PrinterState;
 

/**
 * Milk Collection entry — the primary daily workflow. Optimised for keyboard
 * operation: enter customer code then tab through quantity/FAT/SNF; the rate
 * and amount are calculated automatically and after saving focus returns to
 * the customer code field for fast multi-customer entry.
 */
public class MilkCollectionPanel extends JPanel {

    private static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 16);
    private static final Font FIELD_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 16);

    /** Fixed width of the left entry panel — sized so every field is fully
     * visible without stretching the panel or the window. */
    private static final int LEFT_PANEL_WIDTH = 560;

    // Slip print formatting: date like "30-Aug-2026" and time like "07:10:32".
    private static final java.time.format.DateTimeFormatter SLIP_DATE_FMT = java.time.format.DateTimeFormatter
            .ofPattern("dd-MMM-yyyy", java.util.Locale.ENGLISH);
    private static final java.time.format.DateTimeFormatter SLIP_TIME_FMT = java.time.format.DateTimeFormatter
            .ofPattern("HH:mm:ss");

    private final MilkCollectionService service = new MilkCollectionService();
    private final SettingsService settingsService = new SettingsService();
    private final dairy.erp.util.DairyNameLabel dairyNameLabel = new dairy.erp.util.DairyNameLabel();

    private final DatePicker datePicker = new DatePicker();
    private final JTextField customerCodeField = new JTextField(12);
    private final JTextField customerNameField = new JTextField(18);
    private final JRadioButton cowRadio = new JRadioButton("Cow");
    private final JRadioButton buffaloRadio = new JRadioButton("Buffalo");
    private final JRadioButton mixRadio = new JRadioButton("Mix");
    private final ButtonGroup milkGroup = new ButtonGroup();
    private final JRadioButton morningRadio = new JRadioButton("Morning");
    private final JRadioButton eveningRadio = new JRadioButton("Evening");
    private final ButtonGroup shiftGroup = new ButtonGroup();
    private final JTextField quantityField = new JTextField(10);
    private final JTextField fatField = new JTextField(8);
    private final JTextField rateField = new JTextField(10);
    private final JTextField amountField = new JTextField(12);
    private final JTextField remarksField = new JTextField(16);
    private final JLabel customerBanner = UIUtil.collapsibleGreenBanner();

    // filter controls
    private final DatePicker filterFromPicker = new DatePicker();
    private final DatePicker filterToPicker = new DatePicker();
    private final JComboBox<String> filterCustomerBox = new JComboBox<>();
    private final JComboBox<String> filterShiftBox = new JComboBox<>(new String[] { "All", "Morning", "Evening" });

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] { "Date", "Shift", "Code", "Name", "Milk", "FAT", "Qty", "Rate", "Amount" }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    private Customer selectedCustomer;
    private int editingId = -1;
    // Guards against re-entrant lookups: loadCustomer() moves focus to the
    // quantity field, which fires focusLost on the code field and would call
    // loadCustomer() again — repeating the "customer not found" dialog.
    private boolean inLookup = false;
    // buildForm() re-runs on every refreshForm(); this keeps the button action
    // listeners from being attached more than once.
    private boolean actionListenersAttached = false;
    // Same guard for the field listeners (customer lookup / recalculation).
    private boolean fieldListenersAttached = false;

    // Panel mode: "new" (entry form) or "history" (records).
    private boolean historyMode = false;
    private final JPanel headerPanel = UIUtil.header("New Collection");
    private final JButton newButton = new JButton("New");
    private final JButton savePrintButton = new JButton("Save & Print");
    private final JButton updatePrintButton = new JButton("Update & Print");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton clearButton = new JButton("Clear");
    private final JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 8, 6)) {
        @Override
        public boolean isOpaque() {
            return false;
        }
    };

    private JPanel formContainer;

    public MilkCollectionPanel() {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        initDefaults();
        loadCustomerFilter();
        // Propagation: reload the filter combo whenever customers change on
        // any other screen (Customer master add/update/delete).
        dairy.erp.util.AppBus.onCustomersChanged(v -> loadCustomerFilter());
        // Propagation: re-apply Application Settings (default shift/milk type,
        // date & rate lock switches) the moment they are saved in Settings —
        // no restart needed.
        dairy.erp.util.AppBus.onSettingsChanged(v -> applyLiveSettings());
        loadDairyName();
        add(buildMain(), BorderLayout.CENTER);
        loadTable(null);
    }

    /** Fills the Customer filter combo with all customers (reference: dropdown). */
    private void loadCustomerFilter() {
        // Keep the current selection when the list reloads (e.g. after a
        // customer is added on the Customer screen) so the filter does not
        // silently jump back to "All Customers".
        Object previous = filterCustomerBox.getSelectedItem();
        filterCustomerBox.removeAllItems();
        filterCustomerBox.addItem("All Customers");
        try {
            for (Customer c : new dairy.erp.service.CustomerService().listAll()) {
                filterCustomerBox.addItem(c.getCustomerCode() + " - " + c.getCustomerName());
            }
        } catch (RuntimeException ex) {
            // Combo stays with "All Customers" if the lookup fails.
        }
        if (previous != null) {
            for (int i = 0; i < filterCustomerBox.getItemCount(); i++) {
                if (previous.equals(filterCustomerBox.getItemAt(i))) {
                    filterCustomerBox.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private JPanel buildMain() {
        // Left panel: form entry area directly under the logo header — same
        // structure and white card look as Customer Details / Rate Chart.
        JPanel left = new JPanel(new BorderLayout(10, 6));
        left.setBackground(Color.WHITE);
        left.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xd0, 0xd7, 0xde), 1, true),
                BorderFactory.createEmptyBorder(2, 8, 8, 8)));
        // Header row: logo + title on the left, dairy name in the top-right
        // corner — same as the Dashboard panel.
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.add(headerPanel, BorderLayout.CENTER);
        headerRow.add(dairyNameLabel, BorderLayout.EAST);
        left.add(headerRow, BorderLayout.NORTH);
        formContainer = left;
        left.add(buildForm(), BorderLayout.CENTER);
        // Fixed opening size: the entry form always opens LEFT_PANEL_WIDTH px
        // wide and can never shrink below that, so every field is visible
        // properly; extra window width always goes to the table.
        left.setPreferredSize(new Dimension(LEFT_PANEL_WIDTH, 1));
        left.setMinimumSize(new Dimension(LEFT_PANEL_WIDTH, 0));

        // Right panel: records area with headings, filter and table.
        JPanel right = new JPanel(new BorderLayout(0, 4));
        right.add(buildTableArea(), BorderLayout.CENTER);

        // JSplitPane layout: the left entry panel is kept at its fixed width
        // on every window resize — no dragging needed; all spare width goes
        // to the records table on the right.
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setResizeWeight(0.0);
        split.setContinuousLayout(true);
        // A divider location set while the panel has no size yet is clamped,
        // so (re)apply the fixed width once the panel has its real size and
        // again on every subsequent resize — the panel always stays fixed.
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                split.setDividerLocation(LEFT_PANEL_WIDTH);
            }
        });

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(split, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel shiftPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        p.setOpaque(false);
        p.add(morningRadio);
        p.add(eveningRadio);
        return p;
    }

    private JPanel milkPanel() {
        milkGroup.add(cowRadio);
        milkGroup.add(buffaloRadio);
        milkGroup.add(mixRadio);
        cowRadio.setSelected(true);
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        p.setOpaque(false);
        p.add(cowRadio);
        p.add(Box.createHorizontalStrut(6));
        p.add(buffaloRadio);
        p.add(Box.createHorizontalStrut(6));
        p.add(mixRadio);
        return p;
    }

    private String getSelectedMilkType() {
        if (buffaloRadio.isSelected()) {
            return "Buffalo";
        }
        if (mixRadio.isSelected()) {
            return "Mix";
        }
        return "Cow";
    }

    private void setSelectedMilkType(String milkType) {
        if (milkType == null) {
            cowRadio.setSelected(true);
        } else if ("Buffalo".equalsIgnoreCase(milkType)) {
            buffaloRadio.setSelected(true);
        } else if ("Mix".equalsIgnoreCase(milkType)) {
            mixRadio.setSelected(true);
        } else {
            cowRadio.setSelected(true);
        }
    }

    private String getSelectedShift() {
        return morningRadio.isSelected() ? "Morning" : "Evening";
    }

    private void selectShift(String shift) {
        if ("Evening".equalsIgnoreCase(shift)) {
            eveningRadio.setSelected(true);
        } else {
            morningRadio.setSelected(true);
        }
    }

    private void loadDairyName() {
        // Golden logo-style name (shared DairyNameLabel component).
        dairyNameLabel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        applyDairyName(settingsService.get("dairy.name"));
        dairy.erp.util.AppBus.onDairyNameChanged(this::applyDairyName);
    }

    private void applyDairyName(String name) {
        dairyNameLabel.setText(name == null || name.isBlank() ? "SRS Dairy ERP" : name);
    }

    private void initDefaults() {
        // Customer banner sits on its own row directly below the form title:
        // 20px bold with 5px padding, and collapsed (zero height) while empty
        // so it does not push the form fields down.
        customerBanner.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        customerBanner.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        customerBanner.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        customerBanner.setPreferredSize(new Dimension(1, 0));
        customerBanner.addPropertyChangeListener("text", e -> {
            boolean empty = customerBanner.getText() == null || customerBanner.getText().isBlank();
            customerBanner.setPreferredSize(empty ? new Dimension(1, 0) : new Dimension(320, 36));
            customerBanner.revalidate();
            customerBanner.repaint();
        });
        datePicker.setDate(LocalDate.now());
        UIUtil.styleComponent(datePicker.getTextField(), 18);
        UIUtil.styleComponent(datePicker.getButton(), 18);
        UIUtil.makeUpperCase(customerCodeField);
        // Quantity and FAT accept only integer/fraction values — alphabets
        // and other characters are blocked while typing or pasting.
        UIUtil.allowDecimalOnly(quantityField);
        UIUtil.allowDecimalOnly(fatField);
        UIUtil.styleField(customerCodeField, 14);
        UIUtil.styleField(customerNameField, 18);
        UIUtil.styleField(quantityField, 12);
        UIUtil.styleField(fatField, 10);
        UIUtil.styleField(rateField, 12);
        UIUtil.styleField(amountField, 14);
        UIUtil.styleField(remarksField, 20);
        shiftGroup.add(morningRadio);
        shiftGroup.add(eveningRadio);
        morningRadio.setSelected(true);
        morningRadio.setFont(FIELD_FONT);
        eveningRadio.setFont(FIELD_FONT);
        cowRadio.setFont(FIELD_FONT);
        buffaloRadio.setFont(FIELD_FONT);
        mixRadio.setFont(FIELD_FONT);
        morningRadio.setOpaque(false);
        eveningRadio.setOpaque(false);
        cowRadio.setOpaque(false);
        buffaloRadio.setOpaque(false);
        mixRadio.setOpaque(false);
        String defaultShift = settingsService.get("app.default_shift");
        if ("Evening".equals(defaultShift)) {
            eveningRadio.setSelected(true);
        } else {
            morningRadio.setSelected(true);
        }
        String defaultMilk = settingsService.get("app.default_milk_type");
        setSelectedMilkType(defaultMilk);
        rateField.setEditable(false);
        rateField.setFocusable(false);
        amountField.setEditable(false);
        amountField.setFocusable(false);
        customerNameField.setEditable(false);
        customerNameField.setFocusable(false);
    }

    private JPanel buildForm() {
        // White entry card exactly like Customer Details: plain bold heading,
        // flat fields, locked (auto) fields keep the cream tint + padlock.
        JPanel form = new JPanel(new BorderLayout(12, 8));
        Color lockedCream = UIUtil.DISABLED_BG; // light grey locked fields
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD8, 0xCE, 0xA8)),
                BorderFactory.createEmptyBorder(8, 12, 12, 12)));

        // Heading: "Milk Collection Entry" on its own single row, with the
        // searched customer name (and mobile) shown directly below the title
        // in the blank area.
        JPanel headingRow = new JPanel(new BorderLayout(0, 2));
        headingRow.setOpaque(false);
        JLabel heading = new JLabel("Milk Collection Entry");
        heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        heading.setBorder(BorderFactory.createEmptyBorder(4, 0, 2, 0));
        headingRow.add(heading, BorderLayout.NORTH);
        headingRow.add(customerBanner, BorderLayout.SOUTH);
        form.add(headingRow, BorderLayout.NORTH);

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBackground(Color.WHITE);
        fieldsPanel.setOpaque(true);
        fieldsPanel.setMinimumSize(new Dimension(380, 560));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        // Vertical stack: one field per row, each field spanning the full
        // remaining width of the panel so every value is clearly visible.
        int row = 0;
        addRow(fieldsPanel, g, row++, "Date:", dateComponent());
        addRow(fieldsPanel, g, row++, "Shift:", shiftPanel());
        addRow(fieldsPanel, g, row++, "Customer Code:", customerCodeField);
        addRow(fieldsPanel, g, row++, "Customer Name:", withLockIcon(customerNameField, lockedCream));
        addRow(fieldsPanel, g, row++, "Milk Type:", milkPanel());
        addRow(fieldsPanel, g, row++, "Quantity (LTR):", quantityField);
        addRow(fieldsPanel, g, row++, "FAT (%):", fatField);
        addRow(fieldsPanel, g, row++, "Rate / LTR:", rateComponent());
        addRow(fieldsPanel, g, row++, "Amount:", withLockIcon(amountField, lockedCream));
        addRow(fieldsPanel, g, row++, "Remarks:", remarksField);

        // Buttons: mode-aware grid with the same look as the Customer Details
        // page — New/Save in entry mode; Update/Delete/Print in history mode.
        styleActionButtons();
        rebuildButtons();

        // Enter in customer code loads the customer; the same lookup also runs
        // when the field loses focus (Tab out or click elsewhere), so both the
        // Enter key and moving out of the field load the customer details.
        // Listeners are attached once — buildForm() re-runs on every
        // refreshForm() and re-adding them would fire the lookup/recalc
        // multiple times per keystroke event.
        if (!fieldListenersAttached) {
            fieldListenersAttached = true;
            attachFieldListeners();
        }
        // Fixed-width entry panel: wrap the fields in a vertical scroll pane
        // so every field stays fully visible and usable — when the window is
        // short, the fields scroll instead of being squeezed or clipped.
        JScrollPane formScroll = new JScrollPane(fieldsPanel);
        formScroll.setBorder(null);
        formScroll.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        form.add(formScroll, BorderLayout.CENTER);
        form.add(buttonPanel, BorderLayout.SOUTH);
        return form;
    }

    /** Attaches the one-time field listeners (lookup, recalculation). */
    private void attachFieldListeners() {
        customerCodeField.addActionListener(e -> loadCustomer());
        customerCodeField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                String code = customerCodeField.getText().trim();
                if (code.isEmpty()) {
                    return;
                }
                // Skip when focus moves to a Save/Update button — saving does
                // its own silent lookup; otherwise the button click would pop
                // the "customer not found" dialog before the save even starts.
                Object opposite = e.getOppositeComponent();
                if (opposite == savePrintButton || opposite == updatePrintButton) {
                    return;
                }
                boolean alreadyLoaded = selectedCustomer != null
                        && code.equalsIgnoreCase(selectedCustomer.getCustomerCode());
                if (!alreadyLoaded) {
                    loadCustomer();
                }
            }
        });
        // Recalculate on quantity/FAT entry (Enter or focus loss); SNF is
        // handled in the background from the default per milk type.
        java.awt.event.FocusAdapter recalcAdapter = new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                recalc();
            }
        };
        quantityField.addActionListener(e -> recalc());
        fatField.addActionListener(e -> recalc());
        quantityField.addFocusListener(recalcAdapter);
        fatField.addFocusListener(recalcAdapter);
    }

    /** Adds one label + full-width field row to a two-column vertical form. */
    private void addRow(JPanel form, GridBagConstraints g, int row,
            String labelText, java.awt.Component comp) {
        g.gridx = 0;
        g.gridy = row;
        g.gridwidth = 1;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        form.add(styledLabel(labelText), g);
        g.gridx = 1;
        g.gridwidth = 1;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        form.add(comp, g);
    }

    private void addButton(JPanel panel, String text, java.awt.event.ActionListener listener) {
        JButton b = new JButton(text);
        UIUtil.styleButton(b);
        b.addActionListener(listener);
        panel.add(b);
    }

    private void newBtn(JPanel panel, String text, Color bg, java.awt.event.ActionListener listener) {
        JButton b = new JButton(text);
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(100, 36));
        b.addActionListener(listener);
        panel.add(b);
    }

    /**
     * Styles every action button with the same look as the Customer Details page.
     */
    /**
     * Styles every action button with the same look as the Customer Details page.
     * Listeners are attached exactly once: buildForm() re-runs on every
     * refreshForm(), and re-adding listeners would fire saveAndPrint() multiple
     * times per click — the extra fires hit the already-reset form and popped a
     * bogus "enter a valid customer code" dialog after a successful save.
     */
    private void styleActionButtons() {
        UIUtil.styleSmallButton(newButton, new Color(0x1976D2)); // blue
        UIUtil.styleSmallButton(savePrintButton, new Color(0x2E7D32)); // green
        UIUtil.styleSmallButton(updatePrintButton, new Color(0xEF6C00)); // orange
        UIUtil.styleSmallButton(deleteButton, new Color(0xC62828)); // red
        UIUtil.styleSmallButton(clearButton, new Color(0xd3, 0x2f, 0x2f)); // red
        // White line-art icons on the coloured buttons, per the reference.
        newButton.setIcon(dairy.erp.util.ButtonIcons.of("Printer", Color.WHITE));
        savePrintButton.setIcon(dairy.erp.util.ButtonIcons.of("Save", Color.WHITE));
        updatePrintButton.setIcon(dairy.erp.util.ButtonIcons.of("Refresh", Color.WHITE));
        deleteButton.setIcon(dairy.erp.util.ButtonIcons.of("Trash", Color.WHITE));
        clearButton.setIcon(dairy.erp.util.ButtonIcons.of("Cross", Color.WHITE));
        if (actionListenersAttached) {
            return;
        }
        actionListenersAttached = true;
        newButton.addActionListener(e -> resetForm());
        savePrintButton.addActionListener(e -> saveAndPrint());
        updatePrintButton.addActionListener(e -> updateAndPrint());
        deleteButton.addActionListener(e -> deleteRecord());
        clearButton.addActionListener(e -> resetForm());
    }

    /** Rebuilds the visible button set for the current mode. */
    private void rebuildButtons() {
        buttonPanel.removeAll();
        if (historyMode) {
            // History mode: single button does both operations — update the
            // record and print, same as the entry-mode Save & Print.
            buttonPanel.setLayout(new GridLayout(1, 2, 8, 6));
            buttonPanel.add(updatePrintButton);
            buttonPanel.add(deleteButton);
        } else {
            // Entry mode: single button does both operations — save the
            // collection and print, so the daily workflow needs one click.
            buttonPanel.setLayout(new GridLayout(1, 3, 8, 6));
            buttonPanel.add(newButton);
            buttonPanel.add(savePrintButton);
            buttonPanel.add(clearButton);
        }
        buttonPanel.revalidate();
        buttonPanel.repaint();
    }

    /**
     * Switches the panel between "new" (entry form) and "history" (records).
     * The visible action buttons follow the mode; the panel's titled borders
     * are translated in place by {@link dairy.erp.util.I18n#apply}.
     */
    public void setMode(String mode) {
        historyMode = "history".equals(mode);
        rebuildButtons();
        refreshForm();
    }

    /**
     * Rebuilds the entry form so that the Date and Rate fields reflect the
     * current "Allow Collection Date Adjustment" and "Allow Rate Adjustment"
     * settings. Called every time the panel is shown via setMode().
     */
    private void refreshForm() {
        if (formContainer == null) {
            return;
        }
        // Remove the old form (it sits in the CENTER of formContainer).
        java.awt.Component[] comps = formContainer.getComponents();
        for (java.awt.Component c : comps) {
            if (BorderLayout.CENTER.equals(((java.awt.BorderLayout) formContainer.getLayout()).getConstraints(c))) {
                formContainer.remove(c);
                break;
            }
        }
        formContainer.add(buildForm(), BorderLayout.CENTER);
        formContainer.revalidate();
        formContainer.repaint();
    }

    /**
     * Entry-mode combined action: saves the milk collection and, only when
     * the save succeeds, prints a single-customer milk slip for exactly that
     * record — one click for both operations of the daily workflow.
     */
    private void saveAndPrint() {
        MilkCollection saved = saveRecord();
        if (saved != null) {
            printSlip(saved);
        }
    }

    /**
     * History-mode Update: loads the selected record into the form the first
     * time, then commits the edited values through the normal update path.
     * Returns the committed record on success, or {@code null} when the form
     * was merely loaded for editing or the save failed.
     */
    private MilkCollection updateSelected() {
        if (editingId == -1) {
            if (table.getSelectedRow() < 0) {
                UIUtil.showMessage(this,
                        "Select a record from the history table first.",
                        "Update & Print", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            loadSelected();
            return null; // record loaded — edit the fields, then press Update & Print again to save
        }
        return saveRecord();
    }

    /**
     * History-mode combined action: commits the edited record and, only when
     * the update succeeds, prints a single-customer milk slip for exactly that
     * record — one click for both operations.
     */
    private void updateAndPrint() {
        MilkCollection saved = updateSelected();
        if (saved != null) {
            printSlip(saved);
        }
    }

    /** Creates a GridBagConstraints for a single cell. */
    private GridBagConstraints gbc(GridBagConstraints g, int x, int y, int width, int fill, double weightx) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = width;
        c.insets = new Insets(6, 10, 6, 10);
        c.fill = fill;
        c.weightx = weightx;
        c.anchor = GridBagConstraints.WEST;
        return c;
    }

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(LABEL_FONT);
        return l;
    }

    /**
     * Wraps a read-only (auto-calculated) field with a small padlock icon on
     * its right edge, exactly like the reference design, and paints the field
     * in the darker cream locked colour.
     */
    private javax.swing.JComponent withLockIcon(javax.swing.JTextField field, Color ignored) {
        field.setEditable(false);
        field.setFocusable(false);
        // Keep the original border and overlay the lock icon inside the field.
        // Guard against stacking multiple lock borders when the form is rebuilt.
        Border current = field.getBorder();
        if (current instanceof javax.swing.border.CompoundBorder) {
            javax.swing.border.CompoundBorder cb = (javax.swing.border.CompoundBorder) current;
            if (cb.getInsideBorder() instanceof LockIconBorder
                    || cb.getOutsideBorder() instanceof LockIconBorder) {
                return field; // lock icon already present
            }
        }
        field.setBorder(BorderFactory.createCompoundBorder(current, new LockIconBorder()));
        return field;
    }

    /**
     * Returns the Date field component: the calendar picker shown editable
     * when "Allow Collection Date Adjustment" is true, or locked (padlock
     * icon, disabled calendar button) when false. The lock state is applied
     * by {@link #applyDateAdjustment()} so it can also be re-applied live
     * whenever the setting changes on the Settings screen.
     */
    private JComponent dateComponent() {
        applyDateAdjustment();
        return datePicker;
    }

    /** Re-applies the "Allow Collection Date Adjustment" lock state live. */
    private void applyDateAdjustment() {
        boolean editable = settingsService.getBoolean("app.allow_date_adjustment", false);
        // Same tuned sizing for both states so the overall field width
        // matches the other form fields.
        datePicker.getTextField().setColumns(10);
        datePicker.getButton().setPreferredSize(new java.awt.Dimension(30, 28));
        if (editable) {
            removeLockIcon(datePicker.getTextField());
            datePicker.getButton().setEnabled(true);
            return;
        }
        // Locked: show today's date with a padlock icon and disable the
        // calendar button so no date can be picked or typed.
        datePicker.setDate(LocalDate.now());
        withLockIcon(datePicker.getTextField(), null);
        datePicker.getButton().setEnabled(false);
    }

    /**
     * Returns the Rate field component: editable when "Allow Rate Adjustment"
     * is true, or a locked field showing the auto-calculated rate when false.
     * The lock state is applied by {@link #applyRateAdjustment()} so it can
     * also be re-applied live whenever the setting changes.
     */
    private JComponent rateComponent() {
        applyRateAdjustment();
        return rateField;
    }

    /** Re-applies the "Allow Rate Adjustment" lock state live. */
    private void applyRateAdjustment() {
        boolean editable = settingsService.getBoolean("app.allow_rate_adjustment", false);
        if (editable) {
            removeLockIcon(rateField);
            rateField.setEditable(true);
            rateField.setFocusable(true);
            rateField.setBackground(Color.WHITE);
            return;
        }
        rateField.setText("");
        rateField.setEditable(false);
        rateField.setFocusable(false);
        withLockIcon(rateField, null);
    }

    /**
     * Live propagation of Application Settings saved on the Settings screen:
     * re-applies the default shift and milk type selections and the Date /
     * Rate field lock switches, then recalculates so the displayed rate and
     * amount follow the new settings immediately — no restart needed.
     */
    private void applyLiveSettings() {
        selectShift(settingsService.get("app.default_shift"));
        setSelectedMilkType(settingsService.get("app.default_milk_type"));
        applyDateAdjustment();
        applyRateAdjustment();
        recalc();
    }

    /**
     * Removes a previously added {@link LockIconBorder} so a field that was
     * locked can become editable again (e.g. when "Allow Rate Adjustment" is
     * switched on while the Milk Collection screen is open).
     */
    private void removeLockIcon(javax.swing.JTextField field) {
        Border current = field.getBorder();
        if (current instanceof javax.swing.border.CompoundBorder) {
            javax.swing.border.CompoundBorder cb = (javax.swing.border.CompoundBorder) current;
            if (cb.getInsideBorder() instanceof LockIconBorder) {
                field.setBorder(cb.getOutsideBorder());
            } else if (cb.getOutsideBorder() instanceof LockIconBorder) {
                field.setBorder(cb.getInsideBorder());
            }
        } else if (current instanceof LockIconBorder) {
            field.setBorder(null);
        }
    }

    /**
     * A custom border that paints a small padlock icon inside the trailing
     * edge of a text field, visually indicating a locked/read-only field.
     * Used as the inner border of a CompoundBorder (outer = original field border).
     */
    private static class LockIconBorder extends javax.swing.border.AbstractBorder {
        private static final int ICON_W = 18;
        private static final int ICON_H = 18;
        private static final int PAD = 4;
        private static final Icon LOCK = dairy.erp.util.ButtonIcons.of("Lock", new Color(0x8a, 0x93, 0x9c));

        @Override
        public java.awt.Insets getBorderInsets(java.awt.Component c) {
            return new java.awt.Insets(0, 0, 0, ICON_W + PAD * 2);
        }

        @Override
        public void paintBorder(java.awt.Component c, java.awt.Graphics g, int x, int y, int w, int h) {
            int iconX = x + w - ICON_W - PAD;
            int iconY = y + (h - ICON_H) / 2;
            LOCK.paintIcon(c, g, iconX, iconY);
        }
    }

    /**
     * The SNF value used for the background rate calculation. The SNF input
     * field is not shown on the form (reference design), so a sensible default
     * per milk type is applied: override it with the {@code app.default_snf}
     * setting when needed.
     */
    private java.math.BigDecimal defaultSnf() {
        String v = settingsService.get("app.default_snf");
        if (v != null && !v.isBlank()) {
            java.math.BigDecimal d = ValidationUtil.parseDecimal(v);
            if (d != null && d.signum() > 0) {
                return d;
            }
        }
        String milkType = getSelectedMilkType();
        if ("Buffalo".equals(milkType)) {
            return new java.math.BigDecimal("9.0");
        }
        if ("Mix".equals(milkType)) {
            return new java.math.BigDecimal("8.75");
        }
        return new java.math.BigDecimal("8.5");
    }

    private JPanel buildTableArea() {
        // Bold headings exactly like the reference: "Collection Records"
        // above "Filter / Search", then the one-line filter bar.
        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new javax.swing.BoxLayout(north, javax.swing.BoxLayout.Y_AXIS));

        JLabel recordsHeading = new JLabel("Collection Records");
        recordsHeading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        recordsHeading.setBorder(BorderFactory.createEmptyBorder(2, 4, 0, 4));
        recordsHeading.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        north.add(recordsHeading);

        JLabel filterHeading = new JLabel("Filter / Search");
        filterHeading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        filterHeading.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        filterHeading.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        north.add(filterHeading);

        JPanel filter = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        filter.setOpaque(false);
        filter.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        // Narrow date pickers: only the date itself is visible.
        filterFromPicker.getTextField().setColumns(8);
        filterToPicker.getTextField().setColumns(8);
        filter.add(styledLabel("From:"));
        filter.add(filterFromPicker);
        filter.add(styledLabel("To:"));
        filter.add(filterToPicker);
        filter.add(styledLabel("Customer:"));
        filterCustomerBox.setPrototypeDisplayValue("0000 - XXXXXXXXXXXXXXX");
        filter.add(filterCustomerBox);
        filter.add(styledLabel("Shift:"));
        filterShiftBox.setPrototypeDisplayValue("Morning");
        filter.add(filterShiftBox);
        JButton go = new JButton("Search");
        UIUtil.styleSmallButton(go, new Color(0x15, 0x65, 0xc0)); // blue — same as Customer Details
        go.addActionListener(e -> applyFilter());
        filter.add(go);
        JButton reset = new JButton("Reset");
        UIUtil.styleSmallButton(reset, new Color(0x0d, 0x47, 0xa1)); // dark blue — same as Customer Details
        reset.addActionListener(e -> {
            filterFromPicker.setDate(null);
            filterToPicker.setDate(null);
            filterCustomerBox.setSelectedIndex(0);
            filterShiftBox.setSelectedIndex(0);
            loadTable(null);
        });
        filter.add(reset);
        north.add(filter);

        // Space between the filter row and the table for a cleaner look.
        JPanel area = new JPanel(new BorderLayout(0, 12));
        area.add(north, BorderLayout.NORTH);
        UIUtil.styleCustomerDetailsTable(table);
        JScrollPane scroll = new JScrollPane(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    loadSelected();
                }
            }
        });
        area.add(scroll, BorderLayout.CENTER);
        return area;
    }

    private void loadTable(List<MilkCollection> records) {
        tableModel.setRowCount(0);
        if (records == null) {
            records = service.all();
        }
        for (MilkCollection m : records) {
            tableModel.addRow(new Object[] {
                    DateUtil.toDisplay(m.getCollectionDate()), m.getShift(), m.getCustomerCode(),
                    m.getCustomerName(), m.getMilkType(),
                    CurrencyUtil.format(m.getFat()),
                    CurrencyUtil.format(m.getQuantity()), CurrencyUtil.format(m.getRatePerLitre()),
                    CurrencyUtil.formatMoney(m.getAmount())
            });
        }
    }

    private void applyFilter() {
        LocalDate from = filterFromPicker.getDate();
        LocalDate to = filterToPicker.getDate();
        Integer customerId = null;
        Object sel = filterCustomerBox.getSelectedItem();
        if (sel != null && !sel.toString().startsWith("All")) {
            // Items look like "CUST001 - RAMESH"; the code is before " - ".
            String code = sel.toString().split(" - ")[0].trim();
            Customer c = service.findCustomerByCode(code);
            if (c != null) {
                customerId = c.getId();
            }
        }
        // Milk type is no longer on the filter bar — always search all types.
        String shift = "All".equals(filterShiftBox.getSelectedItem())
                ? null
                : (String) filterShiftBox.getSelectedItem();
        loadTable(service.search(from, to, customerId, null, shift));
    }

    // ---- customer loading and calculation ----

    private void loadCustomer() {
        // Re-entrancy guard: the lookup itself moves focus (quantityField),
        // which re-fires the code field's focusLost listener. Without this
        // guard one wrong code could pop the "not found" dialog repeatedly.
        if (inLookup) {
            return;
        }
        inLookup = true;
        try {
            doLoadCustomer();
        } finally {
            inLookup = false;
        }
    }

    /** Body of {@link #loadCustomer()}, run under the re-entrancy guard. */
    private void doLoadCustomer() {
        String code = customerCodeField.getText().trim();
        if (ValidationUtil.isBlank(code)) {
            return;
        }
        Customer c = service.findCustomerByCode(code);
        if (c == null) {
            UIUtil.showMessage(this,
                    "Customer not found with code: " + code,
                    "Customer", JOptionPane.WARNING_MESSAGE);
            selectedCustomer = null;
            customerNameField.setText("");
            updateBanner();
            quantityField.requestFocusInWindow();
            return;
        }
        selectedCustomer = c;
        customerNameField.setText(c.getCustomerName());
        updateBanner();
        // Only auto-set milk type for new entries — never override the user's
        // selection when editing an existing record.
        if (editingId == -1 && c.getMilkType() != null) {
            setSelectedMilkType(c.getMilkType());
        }
        if ("Inactive".equals(c.getStatus())) {
            UIUtil.showMessage(this,
                    "This customer is inactive.", "Customer", JOptionPane.WARNING_MESSAGE);
        }
        quantityField.requestFocusInWindow();
    }

    /**
     * Silently resolves the customer code typed in the code field — no dialogs.
     * Used by {@link #saveRecord()} so that saving never pops the customer
     * dialog; validation handles an unresolvable code with one message.
     */
    private void loadCustomerSilently() {
        String code = customerCodeField.getText().trim();
        if (ValidationUtil.isBlank(code)) {
            return;
        }
        Customer c = service.findCustomerByCode(code);
        if (c == null) {
            return;
        }
        selectedCustomer = c;
        customerNameField.setText(c.getCustomerName());
        updateBanner();
        // Only auto-set milk type for new entries — never override the user's
        // selection when editing an existing record.
        if (editingId == -1 && c.getMilkType() != null) {
            setSelectedMilkType(c.getMilkType());
        }
    }

    /**
     * Shows the customer name (and mobile) in a centred green banner at the top.
     */
    private void updateBanner() {
        updateBanner(null);
    }

    private void updateBanner(String message) {
        if (message != null && !message.isEmpty()) {
            customerBanner.setText(message);
        } else if (selectedCustomer != null) {
            String mobile = selectedCustomer.getMobile();
            customerBanner.setText(selectedCustomer.getCustomerName()
                    + (mobile == null || mobile.isBlank() ? "" : "  |  " + mobile));
        } else {
            customerBanner.setText("");
        }
    }

    private void recalc() {
        if (selectedCustomer == null) {
            return;
        }
        BigDecimal qty = ValidationUtil.parseDecimal(quantityField.getText());
        BigDecimal fat = ValidationUtil.parseDecimal(fatField.getText());
        // SNF is not on the form any more — the default per milk type is used.
        BigDecimal snf = defaultSnf();
        if (qty == null || fat == null || qty.signum() <= 0) {
            rateField.setText("");
            amountField.setText("");
            return;
        }
        boolean manualOverride = settingsService.getBoolean("app.allow_rate_adjustment", false);
        BigDecimal manualRate = ValidationUtil.parseDecimal(rateField.getText());
        LocalDate date = datePicker.getDate();
        if (date == null) {
            date = LocalDate.now();
        }
        MilkCollectionService.CalculationResult result = service.calculate(
                selectedCustomer, date, getSelectedShift(),
                getSelectedMilkType(), qty, fat, snf,
                manualOverride, manualRate);
        if (result == null) {
            rateField.setText("");
            amountField.setText("");
            UIUtil.showMessage(this,
                    "No rate rule found for this milk type/FAT/SNF.", "Rate",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        rateField.setText(CurrencyUtil.format(result.rate));
        amountField.setText(CurrencyUtil.formatMoney(result.collection.getAmount()));
    }

    // ---- record operations ----

    /**
     * Validates and saves the form. Returns the saved {@link MilkCollection}
     * on success (the caller uses it, e.g. to print a slip immediately), or
     * {@code null} when validation/saving failed or a duplicate exists.
     */
    private MilkCollection saveRecord() {
        // Nothing entered at all — e.g. a double-click fired the Save action
        // twice: the first click saved and reset the form, so the second must
        // not pop a "enter a valid customer code" validation dialog.
        if (selectedCustomer == null
                && ValidationUtil.isBlank(customerCodeField.getText())
                && ValidationUtil.isBlank(quantityField.getText())) {
            return null;
        }
        // Resolve the typed code silently (no dialogs) when the user clicked
        // Save without pressing Enter/tabbing out of the code field. A wrong
        // code is reported once by validateForm() below — saving must never
        // pop the "customer not found" dialog by itself.
        if (selectedCustomer == null) {
            loadCustomerSilently();
        }
        String error = validateForm();
        if (!error.isEmpty()) {
            UIUtil.showMessage(this, error, "Validation", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        LocalDate date = datePicker.getDate();
        String shift = getSelectedShift();
        BigDecimal qty = ValidationUtil.parseDecimal(quantityField.getText());
        BigDecimal fat = ValidationUtil.parseDecimal(fatField.getText());
        // SNF stays a background value: default per milk type / setting.
        BigDecimal snf = defaultSnf();

        // Recompute authoritative values (regardless of manual edits).
        boolean manualOverride = settingsService.getBoolean("app.allow_rate_adjustment", false);
        BigDecimal manualRate = ValidationUtil.parseDecimal(rateField.getText());
        MilkCollectionService.CalculationResult result = service.calculate(
                selectedCustomer, date, shift, getSelectedMilkType(),
                qty, fat, snf, manualOverride, manualRate);
        if (result == null) {
            UIUtil.showMessage(this,
                    "No rate rule found for this milk type/FAT/SNF.", "Rate",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        MilkCollection m = result.collection;
        m.setRemarks(remarksField.getText());

        try {
            if (editingId > 0) {
                m.setId(editingId);
                if (service.existsDuplicate(date, selectedCustomer.getId(), shift, editingId)) {
                    UIUtil.showMessage(this,
                            "A milk collection entry already exists for this customer, date and shift.",
                            "Duplicate Entry", JOptionPane.WARNING_MESSAGE);
                    return null;
                }
                service.update(m);
            } else {
                if (service.existsDuplicate(date, selectedCustomer.getId(), shift, -1)) {
                    UIUtil.showMessage(this,
                            "A milk collection entry already exists for this customer, date and shift.",
                            "Duplicate Entry", JOptionPane.WARNING_MESSAGE);
                    return null;
                }
                int id = service.add(m);
                m.setId(id);
            }
            UIUtil.showMessage(this, "Milk collection saved successfully.",
                    "Save", JOptionPane.INFORMATION_MESSAGE);
            loadTable(service.all());
            resetForNextEntry();
            return m;
        } catch (RuntimeException ex) {
            UIUtil.showMessage(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void deleteRecord() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtil.showMessage(this, "Select a record in the table to delete.",
                    "Delete", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = UIUtil.confirm(this,
                "Are you sure you want to delete this milk collection record?",
                "Delete Entry");
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        int id;
        MilkCollection mc = recordAtRow(row);
        if (mc == null) {
            UIUtil.showMessage(this, "Could not locate the underlying record.",
                    "Delete", JOptionPane.WARNING_MESSAGE);
            return;
        }
        id = mc.getId();
        try {
            service.delete(id);
            loadTable(service.all());
        } catch (RuntimeException ex) {
            UIUtil.showMessage(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            UIUtil.showMessage(this, "Select a record in the table to edit.",
                    "Edit", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String code = (String) tableModel.getValueAt(row, 2);
        Customer c = service.findCustomerByCode(code);
        if (c == null) {
            return;
        }
        selectedCustomer = c;
        MilkCollection mc = recordAtRow(row);
        if (mc == null) {
            UIUtil.showMessage(this, "Could not locate the underlying record.",
                    "Edit", JOptionPane.WARNING_MESSAGE);
            return;
        }
        editingId = mc.getId();
        customerCodeField.setText(c.getCustomerCode());
        customerNameField.setText(c.getCustomerName());
        datePicker.setDate(DateUtil.parse((String) tableModel.getValueAt(row, 0)));
        selectShift((String) tableModel.getValueAt(row, 1));
        setSelectedMilkType((String) tableModel.getValueAt(row, 4));
        fatField.setText(((String) tableModel.getValueAt(row, 5)).replace(",", ""));
        quantityField.setText(((String) tableModel.getValueAt(row, 6)).replace(",", ""));
        rateField.setText(((String) tableModel.getValueAt(row, 7)).replace(",", ""));
        amountField.setText((String) tableModel.getValueAt(row, 8));
        remarksField.setText(mc.getRemarks());
        updateBanner();
    }

    private MilkCollection recordAtRow(int row) {
        // The id is not shown in the table; resolve via customer+date+shift.
        String code = (String) tableModel.getValueAt(row, 2);
        String dateStr = (String) tableModel.getValueAt(row, 0);
        String shift = (String) tableModel.getValueAt(row, 1);
        LocalDate date = DateUtil.parse(dateStr);
        Customer c = service.findCustomerByCode(code);
        if (c == null) {
            return null;
        }
        // The UNIQUE constraint on (collection_date, customer_id, shift) ensures
        // at most one record, so we can safely return the first match.
        List<MilkCollection> results = service.search(date, date, c.getId(), null, shift);
        return results.isEmpty() ? null : results.get(0);
    }

    private String validateForm() {
        if (datePicker.getDate() == null) {
            return "A valid date (dd-MM-yyyy) is required.";
        }
        if (selectedCustomer == null) {
            return "Please enter a valid customer code.";
        }
        BigDecimal qty = ValidationUtil.parseDecimal(quantityField.getText());
        if (qty == null || qty.signum() <= 0) {
            return "Quantity must be greater than zero.";
        }
        BigDecimal fat = ValidationUtil.parseDecimal(fatField.getText());
        if (fat == null || fat.signum() <= 0) {
            return "FAT must be a number greater than zero.";
        }
        BigDecimal minFat = settingsService.minFat(getSelectedMilkType());
        BigDecimal maxFat = settingsService.maxFat(getSelectedMilkType());
        if (minFat != null && maxFat != null && (fat.compareTo(minFat) < 0 || fat.compareTo(maxFat) > 0)) {
            return "FAT must be between " + CurrencyUtil.format(minFat) + " and "
                    + CurrencyUtil.format(maxFat) + " for this milk type.";
        }
        return "";
    }

    private void resetForNextEntry() {
        selectedCustomer = null;
        editingId = -1;
        customerCodeField.setText("");
        customerNameField.setText("");
        quantityField.setText("");
        fatField.setText("");
        rateField.setText("");
        amountField.setText("");
        remarksField.setText("");
        updateBanner();
    }

    public void resetForm() {
        selectedCustomer = null;
        editingId = -1;
        customerCodeField.setText("");
        customerNameField.setText("");
        quantityField.setText("");
        fatField.setText("");
        rateField.setText("");
        amountField.setText("");
        remarksField.setText("");
        datePicker.setDate(LocalDate.now());
        updateBanner();
    }

      private void printSlip(MilkCollection m) {
        String dairyName = settingsService.get("dairy.name");
        if (dairyName == null || dairyName.isBlank()) {
            dairyName = "ANIL DAIRY";
        }
        String softwareName = settingsService.get("dairy.software_name");
        if (softwareName == null || softwareName.isBlank()) {
            softwareName = "Siyaram Dairy Software";
        }

        List<PrintUtil.StyledLine> lines = new ArrayList<>();

        // Fonts
        Font headerFont = new Font(Font.DIALOG, Font.BOLD, 16); // Supports Hindi/Gujarati
        Font bodyFont = new Font("Courier New", Font.BOLD, 12); // Slightly larger for 2.5" (was 9)
        Font smallFont = new Font("Courier New", Font.BOLD, 10);

    
        int SLIP_WIDTH = 34;

        // Border must match exact width
        String border = "=================================="; // 34

        // Header (Dairy Name) - do NOT pre-pad with spaces; PrintUtil centers it
        lines.add(new PrintUtil.StyledLine(
                safeTruncate(dairyName.toUpperCase(), SLIP_WIDTH),
                headerFont,
                PrintUtil.StyledLine.ALIGN_CENTER // <--- CENTER THIS LINE
        ));
        lines.add(new PrintUtil.StyledLine(border, bodyFont));

        // Data fields
        String formattedDate = m.getCollectionDate() == null ? ""
                : m.getCollectionDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss");
        String timeText = LocalTime.now().format(timeFormatter);
        String shiftAbbr = "Evening".equalsIgnoreCase(shiftName(m.getShift())) ? "E" : "M";
        String timeWithShift = timeText + " (" + shiftAbbr + ")";

        // Using fixed-width fields: Label(6)+": "+Value = aligned columns
        lines.add(new PrintUtil.StyledLine(
                slipField("Name", nullToEmpty(m.getCustomerName()).toUpperCase(), SLIP_WIDTH), bodyFont));
        lines.add(new PrintUtil.StyledLine(slipField("Date", formattedDate, SLIP_WIDTH), bodyFont));
        lines.add(new PrintUtil.StyledLine(slipField("Shift", timeWithShift, SLIP_WIDTH), bodyFont));
        lines.add(new PrintUtil.StyledLine(slipField("Code",nullToEmpty(m.getCustomerCode()).toUpperCase() + " " + milkAbbrev(m.getMilkType()), SLIP_WIDTH),bodyFont));
        lines.add(new PrintUtil.StyledLine(slipField("Liter", CurrencyUtil.format(m.getQuantity()), SLIP_WIDTH),
                bodyFont));
        lines.add(new PrintUtil.StyledLine(slipField("Fat", trimZeros(m.getFat()) + " %", SLIP_WIDTH), bodyFont));
        lines.add(new PrintUtil.StyledLine(slipField("Rs.", CurrencyUtil.formatPlain(m.getAmount()), SLIP_WIDTH),
                bodyFont));

        // Footer - truncate long software names to prevent cutoff
        String eoeLine = "-------- E&OE --------"; // or customize dash count as needed
        lines.add(new PrintUtil.StyledLine(eoeLine,smallFont, PrintUtil.StyledLine.ALIGN_CENTER));
        lines.add(new PrintUtil.StyledLine(safeTruncate(softwareName.toUpperCase(), SLIP_WIDTH), smallFont,PrintUtil.StyledLine.ALIGN_CENTER));
        lines.add(new PrintUtil.StyledLine("Thank You", smallFont,PrintUtil.StyledLine.ALIGN_CENTER));

        // Use 2.5-inch page format
        PrintUtil.printSlipDirect(findOwner(), "Milk Slip", lines, 2.5);
    }

    /** Truncates with ... if exceeds maxLen */
    private static String safeTruncate(String text, int maxLen) {
        if (text == null)
            return "";
        return text.length() <= maxLen ? text : text.substring(0, Math.max(0, maxLen - 1)) + "…";
    }

    private PageFormat create2InchPageFormat() {
        double width = 2.0 * 72.0; // change to 3.0 if your roll is 3" wide
        double height = 4.0 * 72.0; // 8 inches = 576 points (matches printer + driver)
        Paper paper = new Paper();
        paper.setSize(width, height);
        paper.setImageableArea(2, 0, width - 4, height);
        PageFormat pf = new PageFormat();
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.PORTRAIT);
        return pf;
    }

    private PageFormat create2Dot5InchPageFormat() {
        double width = 2.5 * 72.0; // 180pt = 64mm (2.5")

        // THIS IS THE FIX: 5 inches exactly (was probably 8.0 or 11.0 before)
        double height = 2.5 * 50.0; // 360pt = 127mm (5")

        Paper paper = new Paper();
        paper.setSize(width, height);

        // LQ-210/LQ-310 needs 10pt left margin to clear sprocket holes
        // If text prints too far right, reduce to 6.0; if prints in sprockets, increase
        // to 12.0
        double marginX = 10.0;
        double marginY = 8.0;

        // Imageable area: where ink goes (must be smaller than total size)
        paper.setImageableArea(marginX, marginY,
                width - (marginX * 2),
                height - marginY);
        

        PageFormat pf = new PageFormat();
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.PORTRAIT);
        return pf;
    }

     private static String centerPad(String text, int width) {
        if (text == null)
            text = "";
        int left = Math.max(0, (width - text.length()) / 2);
        return " ".repeat(left) + text;
    }

    private static String slipField(String label, String value, int width) {
        if (value == null)
            value = "";
        // Fixed-width label: 6 chars left-aligned + ": " (2 chars) = 8 char prefix
        // Example: "Name : " (6+2=8), "Rs. : " (6+2=8)
        String head = String.format("%-5s: ", label);
        int maxValLen = width - head.length();

        if (maxValLen < 1)
            maxValLen = 1; // Minimum 1 char for value

        // Strict truncation - never exceed total width
        if (value.length() > maxValLen) {
            value = value.substring(0, maxValLen);
        }
        return head + value;
    }

    /** Returns the full shift name: "Morning" or "Evening". */
    private static String shiftName(String shift) {
        if ("Morning".equalsIgnoreCase(shift == null ? "" : shift)) {
            return "Morning";
        }
        if ("Evening".equalsIgnoreCase(shift == null ? "" : shift)) {
            return "Evening";
        }
        return nullToEmpty(shift);
    }

    /** Formats a number without trailing zeros, e.g. 4.20 -> 4.2. */
   private static String trimZeros(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        BigDecimal t = value.stripTrailingZeros();
        return t.scale() < 0 ? t.setScale(0).toPlainString() : t.toPlainString();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private java.awt.Frame findOwner() {
        java.awt.Container c = this;
        while (c != null && !(c instanceof JFrame)) {
            c = c.getParent();
        }
        return (java.awt.Frame) c;
    }
  
    private static String milkAbbrev(String milkType) {
        if (milkType == null || milkType.isBlank()) {
            return "";
        }
        String t = milkType.trim();
        return t.length() > 4 ? t.substring(0, 4) : t;
    }
}
