package dairy.erp.util;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;
import java.awt.Component;
import java.awt.Container;
import java.util.HashMap;
import java.util.Map;

/**
 * Bilingual (English / Hindi) UI text support. The user picks the language on
 * the login screen; the choice is persisted as the {@code app.language}
 * setting and applied to the whole application.
 * <p>
 * Two helpers do all the work:
 * <ul>
 *   <li>{@link #t(String)} — translates a single UI string (exact-match
 *       lookup against the Hindi dictionary; unknown strings fall back to the
 *       given English text, so nothing ever breaks).</li>
 *   <li>{@link #apply(java.awt.Component)} — walks a component tree and
 *       translates the text of every label, button, check/radio box, menu
 *       item, tab title and titled border in place. Field values, combo boxes
 *       and tables (live data) are deliberately left untouched.</li>
 * </ul>
 * The lookup is two-way: in English mode {@link #t(String)} also converts a
 * known Hindi string back to its English key, so switching the combo on the
 * login screen re-translates the screen in either direction.
 */
public final class I18n {

    public static final String ENGLISH = "English";
    public static final String HINDI = "हिंदी";

    private static volatile String language = ENGLISH;

    /** English UI string → Hindi translation (exact match). */
    private static final Map<String, String> HINDI_TEXT = new HashMap<>();
    /** Reverse view (Hindi → English) used when switching back to English. */
    private static final Map<String, String> ENGLISH_OF_HINDI = new HashMap<>();

    static {
        // ---- Login screen ----
        put("Login", "लॉगिन");
        put("Login  ✓", "लॉगिन  ✓");
        put("Username", "उपयोगकर्ता नाम");
        put("Password", "पासवर्ड");
        put("Shift Selection", "शिफ्ट चयन");
        put("Language", "भाषा");
        put("Remember Me", "मुझे याद रखें");
        put("Exit", "बाहर");
        put("☀ Morning", "☀ सुबह");
        put("🌙 Evening", "🌙 शाम");
        put("Morning", "सुबह");
        put("Evening", "शाम");

        // ---- Menus / window (MainFrame) ----
        put("File", "फ़ाइल");
        put("Master", "मास्टर");
        put("Data", "डेटा");
        put("Help", "सहायता");
        put("Logout", "लॉग आउट");
        put("Change Password", "पासवर्ड बदलें");
        put("About", "के बारे में");
        put("Dashboard", "डैशबोर्ड");
        put("Customer Management", "ग्राहक प्रबंधन");
        put("Rate Chart", "दर सूची");
        put("Milk Collection", "दूध संग्रह");
        put("Payments", "भुगतान");
        put("Customer Ledger", "ग्राहक खाता");
        put("Reports", "रिपोर्ट्स");
        put("Settings", "सेटिंग्स");
        put("Import / Export / Backup", "आयात / निर्यात / बैकअप");

        // ---- Dashboard quick actions ----
        put("New Milk Entry", "नई दूध एंट्री");
        put("Customers", "ग्राहक");
        put("Customer", "ग्राहक");
        put("Today's Collection", "आज का संग्रह");
        put("Customer Report", "ग्राहक रिपोर्ट");
        put("Monthly Report", "मासिक रिपोर्ट");
        put("Backup", "बैकअप");
        put("New Collection", "नया संग्रह");
        put("Collection History", "संग्रह इतिहास");
        put("New Payment", "नया भुगतान");
        put("Payment History", "भुगतान इतिहास");
        put("Payment Date:", "भुगतान दिनांक:");
        put("Payment Mode:", "भुगतान मोड:");
        put("Reference:", "संदर्भ:");
        put("Payment Entry", "भुगतान प्रविष्टि");
        put("Today's Summary", "आज का सारांश");
        put("Collection Shifts", "संग्रह शिफ्ट");
        put("Milk Type Breakdown", "दूध प्रकार विवरण");
        put("Morning Collection", "सुबह का संग्रह");
        put("Evening Collection", "शाम का संग्रह");
        put("Total Customers", "कुल ग्राहक");
        put("Total Milk", "कुल दूध");
        put("Cow Milk", "गाय का दूध");
        put("Buffalo Milk", "भैंस का दूध");
        put("Mix Milk", "मिश्रित दूध");
        put("\uD83D\uDC04 Cow Milk", "\uD83D\uDC04 गाय का दूध");
        put("\uD83D\uDC03 Buffalo Milk", "\uD83D\uDC03 भैंस का दूध");
        put("\u267F Mix Milk", "\u267F मिश्रित दूध");

        // ---- Common buttons / actions ----
        put("Save", "सहेजें");
        put("Cancel", "रद्द करें");
        put("Delete", "हटाएँ");
        put("Update", "अपडेट करें");
        put("Update & Print", "अपडेट व प्रिंट");
        put("Edit", "संपादित करें");
        put("New", "नया");
        put("Search", "खोजें");
        put("Close", "बंद करें");
        put("Refresh", "रीफ्रेश");
        put("Print", "प्रिंट");
        put("Export", "निर्यात");
        put("Import", "आयात");
        put("Yes", "हाँ");
        put("No", "नहीं");
        put("All", "सभी");
        put("Error", "त्रुटि");

        // ---- Field labels ----
        put("Date", "दिनांक");
        put("Shift", "शिफ्ट");
        put("Name", "नाम");
        put("Code", "कोड");
        put("Customer Code", "ग्राहक कोड");
        put("Customer Name", "ग्राहक का नाम");
        put("Mobile", "मोबाइल");
        put("Address", "पता");
        put("Village", "गाँव");
        put("Milk Type", "दूध प्रकार");
        put("Cow", "गाय");
        put("Buffalo", "भैंस");
        put("Mix", "मिश्रित");
        put("Quantity", "मात्रा");
        put("Qty", "मात्रा");
        put("FAT", "फैट");
        put("SNF", "एसएनएफ");
        put("Rate", "दर");
        put("Amount", "राशि");
        put("Total Amount", "कुल राशि");
        put("Total Qty", "कुल मात्रा");
        put("Balance", "शेष");
        put("Status", "स्थिति");
        put("Active", "सक्रिय");
        put("Inactive", "निष्क्रिय");
        put("Description", "विवरण");
        put("Reference", "संदर्भ");
        put("Mode", "मोड");
        put("Payment", "भुगतान");
        put("Reg. Date", "पंजी. दिनांक");
        put("All Customers", "सभी ग्राहक");

        // ---- Reports / data tools ----
        put("Daily Report", "दैनिक रिपोर्ट");
        put("Weekly Report", "साप्ताहिक रिपोर्ट");
        put("Payment Report", "भुगतान रिपोर्ट");
        put("Customer Statement", "ग्राहक विवरण");
        put("Open Reports", "रिपोर्ट खोलें");
        put("Import CSV", "CSV आयात");
        put("Backup Database", "डेटाबेस बैकअप");
        put("Export PDF", "PDF निर्यात");
        put("Export Excel", "Excel निर्यात");
        put("Export CSV", "CSV निर्यात");
        put("Restore Database", "डेटाबेस पुनर्स्थापित करें");

        // ---- Customers / ledger panel ----
        put("Customer Details", "ग्राहक विवरण");
        put("Customer List", "ग्राहक सूची");
        put("Customer Type", "ग्राहक प्रकार");
        put("Father Name", "पिता का नाम");
        put("Photo", "फ़ोटो");
        put("Delete Customer", "ग्राहक हटाएँ");
        put("Active:", "सक्रिय:");
        put("Address:", "पता:");
        put("Code:", "कोड:");
        put("Customer:", "ग्राहक:");
        put("Customer Code:", "ग्राहक कोड:");
        put("Customer Name:", "ग्राहक का नाम:");
        put("Mobile:", "मोबाइल:");
        put("Name:", "नाम:");
        put("Status:", "स्थिति:");
        put("Village:", "गाँव:");
        put("ID", "आईडी");
        put("Liter", "लीटर");
        put("Other", "अन्य");
        put("Duplicate", "प्रतिलिपि");
        put("CUSTOMER LEDGER", "ग्राहक खाता");

        // ---- Milk collection / payment fields ----
        put("Remarks", "टिप्पणी");
        put("Remarks:", "टिप्पणी:");
        put("Type", "प्रकार");
        put("Cash", "नकद");
        put("UPI", "यूपीआई");
        put("Milk Slip", "दूध पर्ची");
        put("Milk", "दूध");
        put("PAYMENT", "भुगतान");

        // ---- Rate chart ----
        put("Delete Rate Rule", "दर नियम हटाएँ");
        put("Effective From", "प्रभावी से");
        put("Effective To", "प्रभावी तक");
        put("Effective From:", "प्रभावी से:");
        put("Effective To:", "प्रभावी तक:");
        put("FAT Max", "अधिकतम फैट");
        put("SNF Max", "अधिकतम एसएनएफ");
        put("FAT Max:", "अधिकतम फैट:");
        put("SNF Max:", "अधिकतम एसएनएफ:");
        put("Maximum FAT:", "अधिकतम फैट:");
        put("Maximum SNF:", "अधिकतम एसएनएफ:");
        put("Minimum FAT:", "न्यूनतम फैट:");
        put("Minimum SNF:", "न्यूनतम एसएनएफ:");
        put("FAT (%):", "फैट (%):");
        put("SNF (%):", "एसएनएफ (%):");
        put("Rate / LTR:", "दर / लीटर:");
        put("Rate/LTR", "दर/लीटर");
        put("Cow FAT Max:", "गाय अधिकतम फैट:");
        put("Buffalo FAT Max:", "भैंस अधिकतम फैट:");
        put("Mix FAT Max:", "मिश्रित अधिकतम फैट:");
        put("Avg FAT", "औसत फैट");
        put("Avg SNF", "औसत एसएनएफ");
        put("Avg Rate", "औसत दर");

        // ---- Reports / filters / export ----
        put("Report Filters", "रिपोर्ट फ़िल्टर");
        put("Report:", "रिपोर्ट:");
        put("Filter / Search", "फ़िल्टर / खोज");
        put("Summary", "सारांश");
        put("Today", "आज");
        put("Reset", "रीसेट");
        put("Clear", "साफ़ करें");
        put("Restore", "पुनर्स्थापित करें");
        put("Export ...", "निर्यात ...");
        put("Export to CSV", "CSV में निर्यात करें");
        put("Export Report to CSV", "रिपोर्ट CSV में निर्यात करें");
        put("Export Customer Ledger to Excel (CSV)", "ग्राहक खाता Excel (CSV) में निर्यात करें");
        put("Save as PDF", "PDF के रूप में सहेजें");
        put("Rs.", "₹");
        put("From:", "से:");
        put("To:", "तक:");
        put("REPORTS", "रिपोर्ट्स");
        put("EXPORT", "निर्यात");
        put("IMPORT", "आयात");
        put("BACKUP", "बैकअप");
        put("RESTORE", "पुनर्स्थापना");
        put("TOTALS", "कुल");

        // ---- Settings panel ----
        put("App Theme:", "ऐप थीम:");
        put("Dairy Name:", "डेयरी नाम:");
        put("Date Format:", "दिनांक प्रारूप:");
        put("Decimal Places:", "दशमलव स्थान:");
        put("Database:", "डेटाबेस:");
        put("Default Milk Type:", "डिफ़ॉल्ट दूध प्रकार:");
        put("Default Shift:", "डिफ़ॉल्ट शिफ्ट:");
        put("GST Number:", "GST नंबर:");
        put("Email:", "ईमेल:");
        put("Select Date", "दिनांक चुनें");

        // ---- Milk collection entry panel ----
        put("Milk Collection Entry", "दूध संग्रह प्रविष्टि");
        put("Collection Records", "संग्रह अभिलेख");
        put("Save & Print", "सहेजें व प्रिंट");
        // Dynamic titles are set with a leading space — keep it in Hindi too.
        put(" Payment History", " भुगतान इतिहास");
        put(" New Payment", " नया भुगतान");
        put(" Collection History", " संग्रह इतिहास");
        put(" New Collection", " नया संग्रह");
        put("Save & Update", "सहेजें व अपडेट");
        put("Shift:", "शिफ्ट:");
        put("Milk Type:", "दूध प्रकार:");
        put("Quantity (LTR):", "मात्रा (लीटर):");
        put("Amount:", "राशि:");
        put("Registration Date:", "पंजीकरण दिनांक:");

        // ---- About / help dialogs ----
        put("About SRS Dairy ERP", "SRS Dairy ERP के बारे में");
        put("Check for Updates", "अपडेट के लिए जाँचें");
        put("Developer:", "डेवलपर:");
        put("Support Email:", "सहायता ईमेल:");
        put("Product:", "उत्पाद:");
        put("ENVIRONMENT & SYSTEM INFO", "परिवेश व सिस्टम जानकारी");
        put("DEVELOPER & SUPPORT", "डेवलपर व सहायता");
        put("DATA TOOLS", "डेटा उपकरण");
        put("Password Details", "पासवर्ड विवरण");
        put("New Password:", "नया पासवर्ड:");
        put("Activity Log", "गतिविधि लॉग");

        // ---- Confirmation dialogs ----
        put("Are you sure you want to logout?", "क्या आप वाकई लॉग आउट करना चाहते हैं?");
        put("Are you sure you want to exit?", "क्या आप वाकई बाहर निकलना चाहते हैं?");

        // ---- Weekday short names (chart axis) ----
        put("Mon", "सोम");
        put("Tue", "मंगल");
        put("Wed", "बुध");
        put("Thu", "गुरु");
        put("Fri", "शुक्र");
        put("Sat", "शनि");
        put("Sun", "रवि");
    }

    /** Registers an English → Hindi pair in both lookup directions. */
    private static void put(String english, String hindi) {
        HINDI_TEXT.put(english, hindi);
        ENGLISH_OF_HINDI.put(hindi, english);
    }

    /**
     * Dynamic composite strings built at runtime as "prefix + value"
     * (e.g. "Today's Collection  [31-08-2026]" or "User: admin"). Only the
     * prefix is translated; the dynamic value is kept as-is.
     */
    private static final String[][] PREFIXES = {
            {"Today's Collection", "आज का संग्रह"},
            {"Customer Ledger - ", "ग्राहक खाता - "},
            {"User:", "उपयोगकर्ता:"},
            {"Date:", "दिनांक:"},
            {"Customer:", "ग्राहक:"},
            {"Period", "अवधि"},
            {"From:", "से:"},
            {"To:", "तक:"},
            {"Average FAT:", "औसत फैट:"},
            {"Average Rate:", "औसत दर:"},
            {"Average SNF:", "औसत एसएनएफ:"},
            {"Total Customers:", "कुल ग्राहक:"},
            {"Total Milk:", "कुल दूध:"},
            {"Backup completed: ", "बैकअप पूर्ण: "},
            {"Backup created: ", "बैकअप बनाया गया: "},
            {"Database restored from ", "डेटाबेस पुनर्स्थापित किया गया: "},
            {"Exported to ", "निर्यातित किया गया: "},
            {"Exported ", "निर्यातित "},
            {"Imported ", "आयातित "},
    };

    private I18n() {
    }

    /**
     * Selects the UI language. Accepts the display names shown in the login
     * combo ("English" / "हिंदी"), the persisted setting value, or short
     * codes ("en" / "hi"). Unknown values fall back to English.
     */
    public static void setLanguage(String name) {
        if (name == null) {
            language = ENGLISH;
            return;
        }
        String n = name.trim();
        if (n.equalsIgnoreCase("hi") || n.equalsIgnoreCase("hi-IN") || n.equals(HINDI)) {
            language = HINDI;
        } else {
            language = ENGLISH;
        }
    }

    /** Currently selected display name ("English" or "हिंदी"). */
    public static String language() {
        return language;
    }

    /** True when Hindi UI text should be produced. */
    public static boolean isHindi() {
        return HINDI.equals(language);
    }

    /**
     * Translates a single UI string. Unknown strings (dynamic data, messages
     * not in the dictionary) are returned unchanged, so nothing ever breaks.
     * Composite strings that start with a known prefix ("User: admin",
     * "Today's Collection [31-08-2026]" ...) get their prefix translated and
     * the dynamic value kept as-is. In English mode a known Hindi string is
     * converted back to English.
     */
    public static String t(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        if (isHindi()) {
            String exact = HINDI_TEXT.get(text);
            if (exact != null) {
                return exact;
            }
            for (String[] p : PREFIXES) {
                if (text.startsWith(p[0])) {
                    return p[1] + text.substring(p[0].length());
                }
            }
            return text;
        }
        String exact = ENGLISH_OF_HINDI.get(text);
        if (exact != null) {
            return exact;
        }
        for (String[] p : PREFIXES) {
            if (text.startsWith(p[1])) {
                return p[0] + text.substring(p[1].length());
            }
        }
        return text;
    }

    /**
     * Walks the component tree rooted at {@code root} and translates every
     * label, button, check box, radio button, menu item, tab title and
     * titled border in place. Text fields, combo boxes and tables (which
     * carry live data rather than static UI text) are left untouched.
     */
    public static void apply(Component root) {
        applyComponent(root);
    }

    private static void applyComponent(Component c) {
        if (c == null) {
            return;
        }
        if (c instanceof JLabel label) {
            label.setText(t(label.getText()));
        } else if (c instanceof AbstractButton button) {
            button.setText(t(button.getText()));
        } else if (c instanceof JTabbedPane tabs) {
            for (int i = 0; i < tabs.getTabCount(); i++) {
                tabs.setTitleAt(i, t(tabs.getTitleAt(i)));
            }
        } else if (c instanceof javax.swing.JTable table) {
            translateTableHeaders(table);
        }
        if (c instanceof JComponent comp
                && comp.getBorder() instanceof TitledBorder titled
                && titled.getTitle() != null) {
            titled.setTitle(t(titled.getTitle()));
        }
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyComponent(child);
            }
        }
    }

    /**
     * Translates the column header values of a table in place, and installs a
     * display renderer that translates known enum-like cell values (Cow,
     * Buffalo, Morning, Evening, Active, Cash, ...) so the data columns read
     * bilingual too. The underlying model values stay unchanged, so saving,
     * filtering and exporting are not affected.
     */
    private static void translateTableHeaders(javax.swing.JTable table) {
        try {
            table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
                @Override
                protected void setValue(Object value) {
                    super.setValue(value instanceof String s ? t(s) : value);
                }
            });
            for (int i = 0; i < table.getColumnCount(); i++) {
                javax.swing.table.TableColumn column =
                        table.getColumnModel().getColumn(i);
                if (column.getHeaderValue() instanceof String header) {
                    column.setHeaderValue(t(header));
                }
            }
        } catch (RuntimeException ignored) {
            // Header translation must never break table construction.
        }
    }
}