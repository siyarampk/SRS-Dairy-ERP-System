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
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.OrientationRequested;
import javax.swing.JOptionPane;


/**
 * Prints plain-text reports using the standard java.awt.print APIs.
 *
 * IMPORTANT:
 *  - Never falls back to "Save as PDF / Save to file".
 *  - Always targets a REAL physical printer (virtual printers like
 *    "Microsoft Print to PDF" are skipped — they are what caused the
 *    "Save print output as" dialog).
 *  - For slip/roll printers the page height is fitted exactly to the
 *    content, and the raw eject sets the form length (ESC C n) BEFORE
 *    the Form Feed so the LQ-310 stops/tears off right after the slip
 *    instead of rolling a full blank page.
 */
public final class PrintUtil {

    private PrintUtil() {
    }

    private static final float LEFT_MARGIN = 12f;
    private static final double BOTTOM_SLACK_IN = 0.60; // blank roll below the last line

    // ------------------------------------------------------------------
    // Dialog-based printing (reports etc.)
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
    // Direct (no-dialog) slip printing
    // ------------------------------------------------------------------

    /**
     * Prints directly to the physical printer with the caller's PageFormat.
     * The pageFormat height SHOULD be fitted to the content (see
     * {@link #createSlipPageFormat}) so the roll stops after the slip.
     */
    public static boolean printTextDirect(java.awt.Frame owner, String title,
            Collection<StyledLine> styledLines, PageFormat pageFormat) {

        System.out.println("[PrintUtil] Initializing direct print...");

        // FIX: never accept a virtual printer ("Microsoft Print to PDF",
        // XPS, OneNote, Fax...) — those pop a "Save print output as" dialog.
        PrintService ps = resolvePhysicalPrinter();
        if (ps == null) {
            UIUtil.showMessage(owner,
                    "No physical printer was found on this computer.\n\n"
                  + "Please install/connect a printer (and set it as default), then try again.",
                    "Print", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        System.out.println("[PrintUtil] Sending job to physical printer: " + ps.getName());

        TextDocument doc = new TextDocument(styledLines);
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(ps);
            job.setJobName(title);
            job.setPrintable(doc, pageFormat);

            PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
            attrs.add(OrientationRequested.PORTRAIT);

            // KEY FIX — stop the extra paper roll:
            // Set the ESC/P form (page) length to EXACTLY the slip height
            // BEFORE spooling the content job. The driver then advances the
            // roll only to the content instead of a full page. (The old code
            // sent the short-form command AFTER the content, and it included
            // ESC @  which resets the printer back to a long default form —
            // that is why extra blank paper kept rolling out.)
            setShortForm(ps, pageFormat.getHeight() / 72.0);

             System.out.println("[PrintUtil] Spooling slip content...");
            job.print(attrs);
            return true;

        } catch (Exception e) {
            // FIX: report the error — NEVER prompt to save a file.
            e.printStackTrace();
            UIUtil.showMessage(owner,
                    "Could not print to printer '" + ps.getName() + "'.\n\n"
                  + "Error: " + e.getMessage() + "\n\n"
                  + "Check that the printer is powered on, connected and has paper, "
                  + "then try again.",
                    "Print", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Convenience method for roll slips: builds a PageFormat whose HEIGHT is
     * fitted exactly to the supplied lines (no fixed 5"/8" page), then prints
     * directly.
     *
     * @param widthInches roll width, e.g. 2.5 for a 2.5 inch / 64 mm roll
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

        // Rendered height of every line (same 0.85 line compaction as the renderer).
        double contentPt = measureContentHeightPt(lines);

        // Total paper = top margin + content + real bottom slack + bottom margin.
        // The slack keeps the last line ("Thank You") above the tear bar.
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
        double y = 8f; // same top padding as TextDocument.print()
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
    // Printer selection
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
    // Raw ESC/P — short-form control (stops the extra paper roll)
    // ------------------------------------------------------------------

    /**
     * Sends raw bytes to the printer as its own print job.
     */
    private static void sendRaw(PrintService ps, byte[] command) {
        try {
            java.io.ByteArrayInputStream byteStream =
                    new java.io.ByteArrayInputStream(command);

            javax.print.Doc rawDoc = new javax.print.SimpleDoc(
                    byteStream,
                    javax.print.DocFlavor.INPUT_STREAM.AUTOSENSE,
                    null);

            javax.print.DocPrintJob rawJob = ps.createPrintJob();
            rawJob.print(rawDoc, (PrintRequestAttributeSet) null);
        } catch (Exception e) {
            System.err.println("[PrintUtil] Raw ESC/P send failed: " + e.getMessage());
        }
    }

    /**
     * Sets the ESC/P form (page) length with ESC C n — n in 1/6-inch lines —
     * so the paper advances only to the slip height and no extra blank roll
     * is left after the content. Applied BEFORE the content job, so the
     * driver already knows the short page length while rendering. Note that
     * ESC @ (initialize) must NOT be sent here/after: it resets the printer
     * back to a long default form and undoes this command — that was the
     * original cause of the extra rolling.
     */
    private static void setShortForm(PrintService ps, double heightInches) {
        // 6 lines per inch (default line spacing after ESC @).
        int lines = (int) Math.rint(heightInches * 6.0);
        if (lines < 1) {
            lines = 1;
        }
        if (lines > 127) {
            lines = 127; // ESC C n accepts 1..127
        }
        sendRaw(ps, new byte[] { 0x1B, 0x43, (byte) lines });
        System.out.println("[PrintUtil] Short form set: " + lines
                + " lines ≈ " + String.format(Locale.US, "%.2f", lines / 6.0) + " in");
    }

    /**
     * Single form feed (FF) — advances the paper to the tear-off edge of the
     * current (short) form, stopping right after the slip content.
     */
    private static void formFeed(PrintService ps) {
        sendRaw(ps, new byte[] { 0x0C });
        System.out.println("[PrintUtil] Form feed sent — slip stops at tear-off.");
    }

    // ------------------------------------------------------------------
    // Styled line + printable document
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

        public String getText() {
            return text;
        }

        public Font getFont() {
            return font;
        }

        public int getAlignment() {
            return alignment;
        }
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
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

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
                    if (drawX < 0) {
                        drawX = 0;
                    }
                } else {
                    drawX = startX;
                }

                g2.drawString(text, drawX, y + fm.getAscent());
                y += fm.getHeight() * 0.85f;

                if (y > pf.getImageableHeight()) {
                    break;
                }
            }
            return PAGE_EXISTS;
        }
    }
}
