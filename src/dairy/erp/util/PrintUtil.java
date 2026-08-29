package dairy.erp.util;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JOptionPane;

/**
 * Prints plain-text reports using the standard java.awt.print APIs. A simple
 * text-lines Printable is used; no external reporting framework.
 */
public final class PrintUtil {

    private PrintUtil() {
    }

    private static final float LINE_HEIGHT = 11f;
    private static final float TOP_MARGIN = 36f;
    private static final float LEFT_MARGIN = 30f;

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
            JOptionPane.showMessageDialog(owner,
                    "Printing failed: " + e.getMessage(),
                    "Print", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /** Wraps a list of text lines as a {@link Printable}. */
    public static Printable asPrintable(List<String> lines) {
        return new TextDocument(lines);
    }

    private static final class TextDocument implements Printable {
        private final List<String> lines;

        TextDocument(List<String> lines) {
            this.lines = lines == null ? new ArrayList<>() : new ArrayList<>(lines);
        }

        PageFormat pageFormat() {
            PrinterJob job = PrinterJob.getPrinterJob();
            return job.defaultPage();
        }

        @Override
        public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
            Graphics2D g2 = (Graphics2D) g;
            double pageHeight = pf.getImageableHeight();
            float y = TOP_MARGIN;
            int linesPerPage = Math.max(1, (int) ((pageHeight - TOP_MARGIN) / LINE_HEIGHT));
            int start = pageIndex * linesPerPage;
            if (start >= lines.size()) {
                return NO_SUCH_PAGE;
            }
            g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));
            float x = (float) (pf.getImageableX() + LEFT_MARGIN);
            int end = Math.min(start + linesPerPage, lines.size());
            for (int i = start; i < end; i++) {
                g2.drawString(lines.get(i), x, y);
                y += LINE_HEIGHT;
            }
            return PAGE_EXISTS;
        }
    }
}
