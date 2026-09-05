package dairy.erp.util;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import javax.print.attribute.standard.PrinterState;
import javax.swing.JFileChooser;                 
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;


/**
 * Prints plain-text reports using the standard java.awt.print APIs. A simple
 * text-lines Printable is used; no external reporting framework.
 */
public final class PrintUtil {

    private PrintUtil() {
    }

    private static final float LEFT_MARGIN = 12f; // Changed from 20f

    /**
     * Prints the given text lines with a caller-supplied font and left margin.
     * Used by the milk slip to render in a heavier-weight monospaced font
     * closer to the left edge.
     */
    public static boolean printText(java.awt.Frame owner, String title, List<String> lines,
            boolean preview, Font font, float leftMargin) {
        TextDocument doc = new TextDocument(lines, font, leftMargin);
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName(title);
            job.setPrintable(doc, doc.pageFormat());
            if (!job.printDialog()) {
                return false;
            }
            job.print();
            return true;
        } catch (PrinterException e) {
            UIUtil.showMessage(owner,
                    "Printing failed: " + e.getMessage(),
                    "Print", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Prints the given text lines. When {@code preview} is true the standard
     * print dialog's preview is shown; otherwise it prints directly.
     *
     * @return true if the job was printed (or previewed and confirmed)
     */
    public static boolean printText(java.awt.Frame owner, String title, List<String> lines, boolean preview) {
        TextDocument doc = new TextDocument(lines);
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName(title);
            job.setPrintable(doc, doc.pageFormat());
            if (!job.printDialog()) {
                return false;
            }
            job.print();
            return true;
        } catch (PrinterException e) {
            UIUtil.showMessage(owner,
                    "Printing failed: " + e.getMessage(),
                    "Print", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /** Wraps a list of text lines as a {@link Printable}. */
    public static Printable asPrintable(List<String> lines) {
        return new TextDocument(lines);
    }

    /**
     * Prints the given styled lines (each with its own font) using the standard
     * print dialog. Allows mixing font sizes within a single print job — e.g. a
     * larger dairy-name header with smaller data rows on a thermal slip.
     *
     * @return true if the job was printed (or previewed and confirmed)
     */
    public static boolean printText(java.awt.Frame owner, String title,
            Collection<StyledLine> styledLines, boolean preview) {
        TextDocument doc = new TextDocument(styledLines);
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName(title);
            job.setPrintable(doc, doc.pageFormat());
            if (!job.printDialog()) {
                return false;
            }
            job.print();
            return true;
        } catch (PrinterException e) {
            UIUtil.showMessage(owner,
                    "Printing failed: " + e.getMessage(),
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
            job.setPrintable(doc, pageFormat);
            if (!job.printDialog()) {
                return false;
            }
            job.print();
            return true;
        } catch (PrinterException e) {
            UIUtil.showMessage(owner,
                    "Printing failed: " + e.getMessage(),
                    "Print", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

     public static boolean printTextDirect(java.awt.Frame owner, String title,
            Collection<StyledLine> styledLines, PageFormat pageFormat) {

        System.out.println("[PrintUtil] Looking for printer...");

        javax.print.PrintService ps = javax.print.PrintServiceLookup.lookupDefaultPrintService();
        if (ps == null) {
            javax.print.PrintService[] all = javax.print.PrintServiceLookup.lookupPrintServices(null, null);
            if (all != null && all.length > 0) {
                ps = all[0];
                System.out.println("[PrintUtil] Using first available printer: " + ps.getName());
            }
        }

        // Case 1: no printer installed/found at all.
        if (ps == null) {
            System.out.println("[PrintUtil] ❌ No printer found — opening PDF save dialog.");
            return saveAsPdf(owner, title, styledLines, pageFormat);
        }

        System.out.println("[PrintUtil] Printer found: " + ps.getName());

        // Case 2: a printer is configured but currently offline / not accepting jobs.
        if (isPrinterOffline(ps)) {
            System.out.println("[PrintUtil]  Printer is offline — opening PDF save dialog.");
            return saveAsPdf(owner, title, styledLines, pageFormat);
        }

        TextDocument doc = new TextDocument(styledLines);
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintService(ps);
            System.out.println("[PrintUtil] ✓ Printing to: " + ps.getName());
            job.setJobName(title);
            job.setPrintable(doc, pageFormat);
            job.print();
            return true;
        } catch (Exception e) {
            // Case 3: printer looked available but failed at print time
            e.printStackTrace();
            System.out.println("[PrintUtil] ❌ Print failed (" + e.getMessage() + ") — opening PDF save dialog.");
            return saveAsPdf(owner, title, styledLines, pageFormat);
        }
    }
    /**
     * Returns true when the given print service reports itself as not
     * accepting jobs or in a stopped/error state — i.e. plugged in/installed
     * but effectively offline (powered off, disconnected, out of paper/jam, etc.).
     */
    private static boolean isPrinterOffline(javax.print.PrintService ps) {
        try {
            System.out.println("[PrintUtil] Checking printer status for: " + ps.getName());

            PrinterIsAcceptingJobs accepting = (PrinterIsAcceptingJobs) ps.getAttribute(PrinterIsAcceptingJobs.class);

            // FIXED: Check for null before comparing
            if (accepting != null && accepting == PrinterIsAcceptingJobs.NOT_ACCEPTING_JOBS) {
                System.out.println("[PrintUtil] Printer NOT_ACCEPTING_JOBS");
                return true;
            }

            PrinterState state = (PrinterState) ps.getAttribute(PrinterState.class);
            if (state != null && state == PrinterState.STOPPED) {
                System.out.println("[PrintUtil] Printer STOPPED");
                return true;
            }

            System.out.println("[PrintUtil] Printer appears online");
        } catch (Exception e) {
            System.out.println("[PrintUtil] Could not check printer status: " + e.getMessage());
        }
        return false;
    }
   
    private static boolean saveAsPdf(java.awt.Frame owner, String title,
            Collection<StyledLine> styledLines, PageFormat pageFormat) {
        try {
            System.out.println("[PrintUtil] Opening PDF save dialog...");

            String stamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String safeTitle = title == null ? "Slip" : title.replaceAll("[^a-zA-Z0-9]+", "_");
            String defaultName = safeTitle + "_" + stamp + ".pdf";

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Printer not available — Save Slip as PDF");
            chooser.setSelectedFile(new File(defaultName));
            chooser.setFileFilter(new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf"));

            System.out.println("[PrintUtil] Showing save dialog...");
            int result = chooser.showSaveDialog(owner);
            System.out.println("[PrintUtil] Dialog result: " + result);

            if (result != JFileChooser.APPROVE_OPTION) {
                System.out.println("[PrintUtil] User cancelled save dialog");
                return false;
            }

            File file = chooser.getSelectedFile();
            if (file == null) {
                System.out.println("[PrintUtil] No file selected");
                return false;
            }

            // Ensure the .pdf extension is present
            if (!file.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
                file = new File(file.getParentFile(), file.getName() + ".pdf");
            }

            System.out.println("[PrintUtil] Saving PDF to: " + file.getAbsolutePath());
            writeSimplePdf(file, styledLines, pageFormat);

            UIUtil.showMessage(owner,
                    "No printer available. The slip has been saved as a PDF:\n"
                            + file.getAbsolutePath(),
                    "Saved as PDF", JOptionPane.INFORMATION_MESSAGE);

            try {
                if (Desktop.isDesktopSupported()
                        && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(file);
                }
            } catch (Exception ignore) {
                // Ignore if no PDF viewer registered
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            UIUtil.showMessage(owner,
                    "Could not save the slip as PDF: " + e.getMessage(),
                    "Print", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private static void writeSimplePdf(File file, Collection<StyledLine> styledLines,
            PageFormat pageFormat) throws IOException {
        double widthPt = pageFormat.getWidth();
        double heightPt = pageFormat.getHeight();
        double marginX = pageFormat.getImageableX();
        double marginY = pageFormat.getImageableY();

        StringBuilder content = new StringBuilder();
        content.append("BT\n");
        double y = heightPt - marginY - 12; // start near the top of the imageable area
        for (StyledLine line : styledLines) {
            int size = Math.max(6, line.getFont().getSize());
            content.append("/F1 ").append(size).append(" Tf\n");
            content.append(String.format(java.util.Locale.US, "1 0 0 1 %.2f %.2f Tm\n", marginX, y));
            content.append("(").append(escapePdfText(line.getText())).append(") Tj\n");
            y -= size + 2;
        }
        content.append("ET\n");
        byte[] contentBytes = content.toString().getBytes(StandardCharsets.ISO_8859_1);

        try (OutputStream raw = new FileOutputStream(file)) {
            java.util.List<Integer> offsets = new java.util.ArrayList<>();
            StringBuilder pdf = new StringBuilder();
            pdf.append("%PDF-1.4\n");

            offsets.add(pdf.length());
            pdf.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

            offsets.add(pdf.length());
            pdf.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

            offsets.add(pdf.length());
            pdf.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ")
                    .append(String.format(java.util.Locale.US, "%.2f %.2f", widthPt, heightPt))
                    .append("] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n");

            offsets.add(pdf.length());
            pdf.append("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier-Bold >>\nendobj\n");

            offsets.add(pdf.length());
            pdf.append("5 0 obj\n<< /Length ").append(contentBytes.length).append(" >>\nstream\n")
                    .append(content)
                    .append("endstream\nendobj\n");

            int xrefStart = pdf.length();
            pdf.append("xref\n0 ").append(offsets.size() + 1).append("\n");
            pdf.append("0000000000 65535 f \n");
            for (int off : offsets) {
                pdf.append(String.format("%010d 00000 n \n", off));
            }
            pdf.append("trailer\n<< /Size ").append(offsets.size() + 1).append(" /Root 1 0 R >>\n");
            pdf.append("startxref\n").append(xrefStart).append("\n%%EOF");

            raw.write(pdf.toString().getBytes(StandardCharsets.ISO_8859_1));
        }
    }
   
    private static String escapePdfText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    /** A text line paired with the font it should be rendered in. */
    public static final class StyledLine {
        private final String text;
        private final Font font;

        public StyledLine(String text, Font font) {
            this.text = text == null ? "" : text;
            this.font = font != null ? font : new Font(Font.MONOSPACED, Font.PLAIN, 9);
        }

        public String getText() {
            return text;
        }

        public Font getFont() {
            return font;
        }
    }

    private static final class TextDocument implements Printable {
        private final List<StyledLine> styledLines;
        private final float leftMargin;
        private final boolean centerBlock;

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
            this.centerBlock = false;
        }

        TextDocument(Collection<StyledLine> styledLines) {
            this.styledLines = new ArrayList<>(styledLines);
            this.leftMargin = LEFT_MARGIN;
            this.centerBlock = true;
        }

        PageFormat pageFormat() {
            PrinterJob job = PrinterJob.getPrinterJob();
            return job.defaultPage();
        }

        @Override
        public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }

            Graphics2D g2 = (Graphics2D) g;

            // Disable antialiasing for crisp dot-matrix output
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

            // Translate to printable area origin
            g2.translate(pf.getImageableX(), pf.getImageableY());

            g2.setColor(Color.BLACK);

            float y = 10f; // Top offset
            float x = leftMargin; // use the 18pt configured margin (was hardcoded 2f)

            for (int i = 0; i < styledLines.size(); i++) {
                PrintUtil.StyledLine line = styledLines.get(i);

                Font font = line.getFont();
                g2.setFont(font);

                FontMetrics fm = g2.getFontMetrics(font);

                // Calculate X position for centering
                int textWidth = fm.stringWidth(line.getText());
                float drawX = x;

                g2.drawString(line.getText(), drawX, y + fm.getAscent());

                // Standard line spacing for receipts
                y += fm.getHeight() + 2f;
            }

            return PAGE_EXISTS;
        }

        /** Returns the rendered width of the widest line for the given fonts. */
        private static float widestAt(Graphics2D g2, List<StyledLine> lines, Font[] fonts) {
            float widest = 0f;
            for (int i = 0; i < lines.size(); i++) {
                g2.setFont(fonts[i]);
                float w = g2.getFontMetrics().stringWidth(lines.get(i).getText());
                if (w > widest) {
                    widest = w;
                }
            }
            return widest;
        }
    }
}
