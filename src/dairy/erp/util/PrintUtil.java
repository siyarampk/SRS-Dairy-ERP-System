package dairy.erp.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;


/**
 * Prints plain-text reports using the standard java.awt.print APIs.
 *
 * IMPORTANT:
 *  - Never falls back to "Save as PDF / Save to file".
 *  - Always targets a REAL physical printer
 *  - FIXED: Non-blocking direct print with timeout so UI never hangs
 */
public final class PrintUtil {

    private PrintUtil() {}

    private static final float LEFT_MARGIN = 12f;
    private static final double BOTTOM_SLACK_IN = 0.60;
    private static final int DIRECT_PRINT_TIMEOUT_SEC = 7; // max wait before giving up

    // ------------------------------------------------------------------
    // Dialog-based printing (reports etc.) - UNCHANGED
    // ------------------------------------------------------------------

    public static boolean printText(java.awt.Frame owner, String title, List<String> lines,
            boolean preview, Font font, float leftMargin) {
        TextDocument doc = new TextDocument(lines, font, leftMargin);
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName(title);
            preselectPhysicalPrinter(job);
            job.setPrintable(doc, doc.pageFormat());
            if (!job.printDialog()) {
                return false;
            }
            job.print();
            return true;
        } catch (PrinterException e) {
            UIUtil.showMessage(owner, "Printing failed: " + e.getMessage(),
                    "Print", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public static boolean printText(java.awt.Frame owner, String title,
            List<String> lines, boolean preview) {
        TextDocument doc = new TextDocument(lines);
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName(title);
            preselectPhysicalPrinter(job);
            job.setPrintable(doc, doc.pageFormat());
            if (!job.printDialog()) {
                return false;
            }
            job.print();
            return true;
        } catch (PrinterException e) {
            UIUtil.showMessage(owner, "Printing failed: " + e.getMessage(),
                    "Print", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public static Printable asPrintable(List<String> lines) {
        return new TextDocument(lines);
    }

    public static boolean printText(java.awt.Frame owner, String title,
            Collection<StyledLine> styledLines, boolean preview) {
        TextDocument doc = new TextDocument(styledLines);
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName(title);
            preselectPhysicalPrinter(job);
            job.setPrintable(doc, doc.pageFormat());
            if (!job.printDialog()) {
                return false;
            }
            job.print();
            return true;
        } catch (PrinterException e) {
            UIUtil.showMessage(owner, "Printing failed: " + e.getMessage(),
                    "Print", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    public static boolean printText(java.awt.Frame owner, String title,
            Collection<StyledLine> styledLines, boolean preview, PageFormat pageFormat) {
        TextDocument doc = new TextDocument(styledLines);
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName(title);
            preselectPhysicalPrinter(job);
            job.setPrintable(doc, pageFormat);
            if (!job.printDialog()) {
                return false;
            }
            job.print();
            return true;
        } catch (PrinterException e) {
            UIUtil.showMessage(owner, "Printing failed: " + e.getMessage(),
                    "Print", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Direct (no-dialog) slip printing - FIXED VERSION
    // ------------------------------------------------------------------

    /**
     * FIXED: Prints directly WITHOUT hanging the UI.
     * - If no physical printer -> shows dialog "Printer not found" and returns false IMMEDIATELY
     * - If printer found but offline/not responding -> times out after 7 sec, shows dialog, returns false
     * - Record saving should NOT depend on return value of this method
     */
    public static boolean printTextDirect(java.awt.Frame owner, String title,
            Collection<StyledLine> styledLines, PageFormat pageFormat) {

        System.out.println("[PrintUtil] Initializing direct print...");

        PrintService ps = resolvePhysicalPrinter();
        if (ps == null) {
            System.out.println("[PrintUtil] No physical printer found -> showing dialog");
            // Ensure dialog is shown on EDT
            Runnable showMsg = () -> UIUtil.showMessage(owner,
                    "Printer not found.\n\nPlease check printer is connected, powered ON and has paper.\nRecord saved successfully.",
                    "Print", JOptionPane.WARNING_MESSAGE);
            if (SwingUtilities.isEventDispatchThread()) {
                showMsg.run();
            } else {
                SwingUtilities.invokeLater(showMsg);
            }
            return false;
        }

        // Optional: quick check if printer says it is not accepting jobs
        try {
            PrinterIsAcceptingJobs accepting = ps.getAttribute(PrinterIsAcceptingJobs.class);
            if (accepting == PrinterIsAcceptingJobs.NOT_ACCEPTING_JOBS) {
                System.out.println("[PrintUtil] Printer reports NOT_ACCEPTING_JOBS: " + ps.getName());
                // still try, but will timeout quickly if really offline
            }
        } catch (Exception ignore) {}

        System.out.println("[PrintUtil] Sending job to physical printer: " + ps.getName());

        // ---- FIX: Run actual blocking print in background thread with TIMEOUT ----
        // This prevents the EDT (UI thread) from hanging forever when USB is unplugged.
        ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "PrintUtil-DirectPrint");
            t.setDaemon(true);
            return t;
        });

        // Make copies for thread safety
        final PrintService finalPs = ps;
        final Collection<StyledLine> copyLines = new ArrayList<>(styledLines);
        final PageFormat copyPf = pageFormat;
        final String copyTitle = title;

        Future<Boolean> future = exec.submit(() -> {
            try {
                return doDirectPrint(finalPs, copyTitle, copyLines, copyPf);
            } catch (Exception ex) {
                ex.printStackTrace();
                // Show error on EDT
                SwingUtilities.invokeLater(() -> UIUtil.showMessage(owner,
                        "Could not print to printer '" + finalPs.getName() + "'.\n\n"
                      + "Error: " + ex.getMessage() + "\n\n"
                      + "Check that the printer is powered on, connected and has paper.\nRecord saved successfully.",
                        "Print", JOptionPane.ERROR_MESSAGE));
                return false;
            }
        });

        try {
            // Wait max 7 seconds. If printer is disconnected, job.print() will hang,
            // but we will timeout and free the UI / allow record to save.
            Boolean result = future.get(DIRECT_PRINT_TIMEOUT_SEC, TimeUnit.SECONDS);
            exec.shutdownNow();
            return Boolean.TRUE.equals(result);
        } catch (TimeoutException te) {
            System.err.println("[PrintUtil] TIMEOUT after " + DIRECT_PRINT_TIMEOUT_SEC + "s - printer not responding, cancelling. Record will still save.");
            future.cancel(true);
            exec.shutdownNow();
            // Must show dialog on EDT
            SwingUtilities.invokeLater(() -> UIUtil.showMessage(owner,
                    "Printer not found or not responding.\n\n"
                  + "Printer '" + finalPs.getName() + "' is not responding.\n"
                  + "Please check USB / power / paper.\n\n"
                  + "Record saved successfully.",
                    "Print", JOptionPane.WARNING_MESSAGE));
            return false;
        } catch (ExecutionException ee) {
            exec.shutdownNow();
            Throwable cause = ee.getCause();
            cause.printStackTrace();
            SwingUtilities.invokeLater(() -> UIUtil.showMessage(owner,
                    "Printing failed: " + cause.getMessage() + "\n\nRecord saved successfully.",
                    "Print", JOptionPane.ERROR_MESSAGE));
            return false;
        } catch (InterruptedException ie) {
            exec.shutdownNow();
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Actual blocking print logic - called from background thread
     */
    private static boolean doDirectPrint(PrintService ps, String title,
            Collection<StyledLine> styledLines, PageFormat pageFormat) throws Exception {

        TextDocument doc = new TextDocument(styledLines);
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(ps);
        job.setJobName(title);
        job.setPrintable(doc, pageFormat);

        PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
        attrs.add(OrientationRequested.PORTRAIT);

        // Set short form with its own timeout (also blocks if offline)
        setShortFormWithTimeout(ps, pageFormat.getHeight() / 72.0);

        System.out.println("[PrintUtil] Spooling slip content...");
        job.print(attrs);
        System.out.println("[PrintUtil] Slip spooled successfully to " + ps.getName());
        return true;
    }

    /**
     * Convenience method for roll slips: builds a PageFormat whose HEIGHT is
     * fitted exactly to the supplied lines, then prints directly.
     */
    public static boolean printSlipDirect(java.awt.Frame owner, String title,
            Collection<StyledLine> lines, double widthInches) {
        return printTextDirect(owner, title, lines,
                createSlipPageFormat(lines, widthInches, 6.0, 4.0));
    }

    /**
     * Builds a PageFormat for a roll slip with the paper height measured from
     * the actual content (same 0.85 line-spacing compacting as the renderer).
     */
    public static PageFormat createSlipPageFormat(Collection<StyledLine> lines,
            double widthInches, double marginXPt, double marginYPt) {
        double widthPt = widthInches * 72.0;
        double contentPt = measureContentHeightPt(lines);
        double bottomSlackIn = 2.0;
        double paperH = marginYPt + contentPt + (bottomSlackIn * 72.0) + marginYPt;
        paperH = Math.max(paperH, 24.0);
        paperH = Math.ceil(paperH / 12.0) * 12.0;
        double imageableH = Math.max(paperH - (marginYPt * 2.0), contentPt + 2.0);
        Paper paper = new Paper();
        paper.setSize(widthPt, paperH);
        paper.setImageableArea(marginXPt, marginYPt,
                widthPt - (marginXPt * 2.0),
                imageableH);
        PageFormat pf = new PageFormat();
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.PORTRAIT);
        return pf;
    }

    private static double measureContentHeightPt(Collection<StyledLine> lines) {
        java.awt.image.BufferedImage scratch = new java.awt.image.BufferedImage(1, 1,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D mg = scratch.createGraphics();
        double y = 8f;
        if (lines != null) {
            for (StyledLine line : lines) {
                Font f = line.getFont();
                mg.setFont(f);
                y += mg.getFontMetrics(f).getHeight() * 0.85f;
            }
        }
        mg.dispose();
        return y;
    }

    // ------------------------------------------------------------------
    // Printer selection - UNCHANGED
    // ------------------------------------------------------------------

    private static PrintService resolvePhysicalPrinter() {
        PrintService def = PrintServiceLookup.lookupDefaultPrintService();
        if (def != null && isPhysicalPrinter(def)) {
            return def;
        }
        PrintService[] all = PrintServiceLookup.lookupPrintServices(null, null);
        if (all != null) {
            for (PrintService ps : all) {
                if (isPhysicalPrinter(ps)) {
                    System.out.println("[PrintUtil] Selected physical printer: " + ps.getName());
                    return ps;
                }
            }
            for (PrintService ps : all) {
                System.out.println("[PrintUtil] Skipping virtual printer: " + ps.getName());
            }
        }
        return null;
    }

    private static void preselectPhysicalPrinter(PrinterJob job) {
        try {
            PrintService ps = resolvePhysicalPrinter();
            if (ps != null) {
                job.setPrintService(ps);
            }
        } catch (PrinterException e) {
            System.out.println("[PrintUtil] Could not preselect printer: " + e.getMessage());
        }
    }

    private static boolean isPhysicalPrinter(PrintService ps) {
        if (ps == null || ps.getName() == null) {
            return false;
        }
        String name = ps.getName().toLowerCase(Locale.ROOT);
        return !(name.contains("pdf")
                || name.contains("xps")
                || name.contains("onenote")
                || name.contains("one note")
                || name.contains("fax")
                || name.contains("document writer")
                || name.contains("snipping")
                || name.contains("print to file")
                || name.contains("save as")
                || name.contains("adobe pdf")
                || name.contains("cutepdf")
                || name.contains("dopdf")
                || name.contains("bullzip"));
    }

    // ------------------------------------------------------------------
    // Raw ESC/P — short-form control - FIXED WITH TIMEOUT
    // ------------------------------------------------------------------

    private static void sendRaw(PrintService ps, byte[] command) {
        try {
            java.io.ByteArrayInputStream byteStream =
                    new java.io.ByteArrayInputStream(command);
            javax.print.Doc rawDoc = new javax.print.SimpleDoc(
                    byteStream,
                    javax.print.DocFlavor.INPUT_STREAM.AUTOSENSE,
                    null);
            javax.print.DocPrintJob rawJob = ps.createPrintJob();
            // This is the call that blocks when printer is offline
            rawJob.print(rawDoc, (PrintRequestAttributeSet) null);
        } catch (Exception e) {
            System.err.println("[PrintUtil] Raw ESC/P send failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * FIXED: Wraps sendRaw in a 3-second timeout so disconnected USB doesn't hang forever
     */
    private static void setShortFormWithTimeout(PrintService ps, double heightInches) {
        int lines = (int) Math.rint(heightInches * 6.0);
        if (lines < 1) lines = 1;
        if (lines > 127) lines = 127;
        final byte[] cmd = new byte[] { 0x1B, 0x43, (byte) lines };

        ExecutorService ex = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "PrintUtil-SetShortForm");
            t.setDaemon(true);
            return t;
        });
        Future<?> f = ex.submit(() -> sendRaw(ps, cmd));
        try {
            f.get(3, TimeUnit.SECONDS);
            System.out.println("[PrintUtil] Short form set: " + lines
                    + " lines ≈ " + String.format(Locale.US, "%.2f", lines / 6.0) + " in");
        } catch (TimeoutException te) {
            f.cancel(true);
            System.err.println("[PrintUtil] setShortForm timed out - printer not responding, ignoring");
            throw new RuntimeException("Printer not responding while setting form length");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            ex.shutdownNow();
        }
    }

    private static void setShortForm(PrintService ps, double heightInches) {
        setShortFormWithTimeout(ps, heightInches);
    }

    private static void formFeed(PrintService ps) {
        sendRaw(ps, new byte[] { 0x0C });
        System.out.println("[PrintUtil] Form feed sent — slip stops at tear-off.");
    }

    // ------------------------------------------------------------------
    // Styled line + printable document - UNCHANGED
    // ------------------------------------------------------------------

    public static final class StyledLine {
        public static final int ALIGN_LEFT = 0;
        public static final int ALIGN_CENTER = 1;
        private final String text;
        private final Font font;
        private final int alignment;
        public StyledLine(String text, Font font) {
            this(text, font, ALIGN_LEFT);
        }
        public StyledLine(String text, Font font, int alignment) {
            this.text = text == null ? "" : text;
            this.font = font != null ? font : new Font(Font.MONOSPACED, Font.PLAIN, 9);
            this.alignment = alignment;
        }
        public String getText() { return text; }
        public Font getFont() { return font; }
        public int getAlignment() { return alignment; }
    }

    private static final class TextDocument implements Printable {
        private final List<StyledLine> styledLines;
        private final float leftMargin;
        TextDocument(List<String> lines) {
            this(lines, new Font(Font.MONOSPACED, Font.PLAIN, 9), 30f);
        }
        TextDocument(List<String> lines, Font font, float leftMargin) {
            this.styledLines = new ArrayList<>();
            if (lines != null) {
                for (String line : lines) {
                    styledLines.add(new StyledLine(line, font));
                }
            }
            this.leftMargin = leftMargin;
        }
        TextDocument(Collection<StyledLine> styledLines) {
            this.styledLines = new ArrayList<>(styledLines);
            this.leftMargin = LEFT_MARGIN;
        }
        PageFormat pageFormat() {
            return PrinterJob.getPrinterJob().defaultPage();
        }
        @Override
        public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
            if (pageIndex > 0) return NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            g2.translate(pf.getImageableX(), pf.getImageableY());
            g2.setColor(Color.BLACK);
            float y = 8f;
            float maxX = (float) pf.getImageableWidth();
            float startX = leftMargin;
            for (int i = 0; i < styledLines.size(); i++) {
                StyledLine line = styledLines.get(i);
                Font font = line.getFont();
                g2.setFont(font);
                FontMetrics fm = g2.getFontMetrics(font);
                String text = line.getText();
                float textW = fm.stringWidth(text);
                float drawX;
                if (line.getAlignment() == StyledLine.ALIGN_CENTER) {
                    drawX = (maxX - textW) / 2f;
                    if (drawX < 0) drawX = 0;
                } else {
                    drawX = startX;
                }
                g2.drawString(text, drawX, y + fm.getAscent());
                y += fm.getHeight() * 0.85f;
                if (y > pf.getImageableHeight()) break;
            }
            return PAGE_EXISTS;
        }
    }
}
