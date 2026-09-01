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

    private static final float LINE_HEIGHT = 12f; // Dot matrix standard line height
    private static final float TOP_MARGIN = 36f;
    private static final float LEFT_MARGIN = 20f; // Closer to edge for slip printing

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

        // @Override
        // public int print(Graphics g, PageFormat pf, int pageIndex) throws
        // PrinterException {
        // if (pageIndex > 0) {
        // return NO_SUCH_PAGE;
        // }

        // Graphics2D g2 = (Graphics2D) g;

        // // 1. Disable Anti-Aliasing (Anti-aliasing creates grey pixels that look like
        // ink blurs on dot-matrix)
        // g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
        // RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        // g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        // RenderingHints.VALUE_ANTIALIAS_OFF);

        // // 2. Enable Fractional Metrics OFF for crisp pixel/pin alignment
        // g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
        // RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

        // // 1. Allow full physical width (prevents text over-shrinking)
        // float maxRollWidth = 220f;
        // float imgW = (float) Math.min(pf.getImageableWidth(), maxRollWidth);

        // // 2. Clear left cutoff by starting at 26pt margin
        // float effectiveLeftMargin = 26f;
        // float availW = Math.max(imgW - effectiveLeftMargin, 20f);

        // Font[] fonts = new Font[styledLines.size()];
        // for (int i = 0; i < styledLines.size(); i++) {
        // fonts[i] = styledLines.get(i).getFont();
        // }

        // // Auto-shrink font logic with safe floor of 9pt
        // float widest = widestAt(g2, styledLines, fonts);
        // if (widest > availW) {
        // float factor = availW / widest;
        // for (int i = 0; i < fonts.length; i++) {
        // Font f = fonts[i];
        // int size = Math.max(9, Math.round(f.getSize2D() * factor));
        // fonts[i] = new Font(f.getFamily(), f.getStyle(), size);
        // }

        // widest = widestAt(g2, styledLines, fonts);
        // while (widest > availW && widest > 0f) {
        // boolean anyReduced = false;
        // for (int i = 0; i < fonts.length; i++) {
        // Font f = fonts[i];
        // if (fonts[i].getSize() > 9) {
        // fonts[i] = new Font(f.getFamily(), f.getStyle(), f.getSize() - 1);
        // anyReduced = true;
        // }
        // }
        // if (!anyReduced)
        // break;
        // widest = widestAt(g2, styledLines, fonts);
        // }
        // }

        // // 3. Render lines
        // g2.setColor(Color.BLACK);
        // float y = 14f;

        // for (int i = 0; i < styledLines.size(); i++) {
        // PrintUtil.StyledLine line = styledLines.get(i);
        // Font font = fonts[i];
        // g2.setFont(font);

        // FontMetrics fm = g2.getFontMetrics(font);
        // y += fm.getAscent();

        // g2.drawString(line.getText(), effectiveLeftMargin, y);

        // y += fm.getDescent() + fm.getLeading() + 2;
        // }

        // return PAGE_EXISTS;
        // }

        @Override
        public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }

            Graphics2D g2 = (Graphics2D) g;

            // Crisp 24-pin dot-matrix rendering settings
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            

            // Fixed left margin matching the target sample
            g2.translate(pf.getImageableX()-6, pf.getImageableY()+10);

            float effectiveLeftMargin = 12f;
            g2.setColor(Color.BLACK);

            float y = 12f; // Top offset

            for (int i = 0; i < styledLines.size(); i++) {
                PrintUtil.StyledLine line = styledLines.get(i);

                // Enforce Courier New / Monospaced font
                Font font = line.getFont();
                g2.setFont(font);

                FontMetrics fm = g2.getFontMetrics(font);
                y += fm.getAscent();

                g2.drawString(line.getText(), effectiveLeftMargin, y);

                // Line spacing matching standard roll receipt pitch
                y += fm.getDescent() + fm.getLeading() + 1f;
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
