package dairy.erp.util;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.JOptionPane;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.RenderingHints;

/**
 * Prints plain-text reports using the standard java.awt.print APIs. A simple
 * text-lines Printable is used; no external reporting framework.
 */
public final class PrintUtil {

    private PrintUtil() {
    }

  
    private static final float LEFT_MARGIN = 12f;  // Changed from 20f
  
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
        TextDocument doc = new TextDocument(styledLines);
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            javax.print.PrintService ps =
                    javax.print.PrintServiceLookup.lookupDefaultPrintService();
            if (ps == null) {
                javax.print.PrintService[] all =
                        javax.print.PrintServiceLookup.lookupPrintServices(null, null);
                if (all != null && all.length > 0) {
                    ps = all[0];
                }
            }
            if (ps == null) {
                UIUtil.showMessage(owner,
                        "No printer found. Install a printer and set it as the "
                                + "default (Settings > Devices and Printers).",
                        "Print", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            job.setPrintService(ps);
            System.out.println("[PrintUtil] Direct print to: " + ps.getName()
                    + " | paper: " + pageFormat.getWidth() + " x "
                    + pageFormat.getHeight() + " pt");
            job.setJobName(title);
            job.setPrintable(doc, pageFormat);
            job.print();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            UIUtil.showMessage(owner,
                    "Printing failed: " + e.getMessage(),
                    "Print", JOptionPane.ERROR_MESSAGE);
            return false;
        }
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
