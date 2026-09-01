package dairy.erp.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/**
 * The dairy name painted like a logo title: golden gradient letters with a
 * dark outline and a soft shadow (matches the "अनिल डेयरी" reference).
 * Rendered with TextLayout so Devanagari/complex scripts shape correctly.
 * Shared by every panel that displays the dairy name.
 */
public class DairyNameLabel extends JComponent {

    private static final Font NAME_FONT = new Font(Font.DIALOG, Font.BOLD, 30);
    private String text = "";

    public DairyNameLabel() {
        setOpaque(false);
    }

    public void setText(String value) {
        text = value == null ? "" : value;
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        if (text.isBlank()) {
            return new Dimension(10, 44);
        }
        java.awt.font.FontRenderContext frc =
                new java.awt.font.FontRenderContext(null, true, true);
        java.awt.geom.Rectangle2D b = NAME_FONT.getStringBounds(text, frc);
        return new Dimension((int) Math.ceil(b.getWidth()) + 12,
                (int) Math.ceil(b.getHeight()) + 10);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (text.isBlank()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        java.awt.font.TextLayout layout =
                new java.awt.font.TextLayout(text, NAME_FONT, g2.getFontRenderContext());
        java.awt.geom.Rectangle2D b = layout.getBounds();
        float x = (float) ((getWidth() - b.getWidth()) / 2 - b.getX());
        float y = (float) ((getHeight() - b.getHeight()) / 2 - b.getY());
        java.awt.Shape shape = layout.getOutline(
                java.awt.geom.AffineTransform.getTranslateInstance(x, y));

        // Soft drop shadow behind the letters.
        g2.setColor(new Color(0x3a, 0x24, 0x06, 0x66));
        g2.fill(java.awt.geom.AffineTransform.getTranslateInstance(x + 2, y + 3)
                .createTransformedShape(layout.getOutline(null)));

        // Dark brown outline around the letters.
        g2.setColor(new Color(0x4a, 0x2c, 0x08));
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(shape);

        // Golden gradient fill: light gold at the top to deep gold at the bottom.
        g2.setPaint(new GradientPaint(0, (float) b.getMinY() + y, new Color(0xff, 0xeb, 0x9e),
                0, (float) b.getMaxY() + y, new Color(0xe2, 0x88, 0x14)));
        g2.fill(shape);
        g2.dispose();
    }
}