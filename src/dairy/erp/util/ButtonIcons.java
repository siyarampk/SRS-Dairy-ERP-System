package dairy.erp.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;

/**
 * Crisp vector line-art icons for the dashboard quick-action buttons, painted
 * in the brand colour so every button gets a proper matching icon instead of
 * an emoji glyph. All icons are drawn inside a 20x20 box at any scale.
 */
public final class ButtonIcons {

    /** Painting routine for one icon (s = icon size, c = brand colour). */
    private interface Painter {
        void paint(Graphics2D g2, float s, Color c);
    }

    private ButtonIcons() {
    }

    /** Returns the icon for a dashboard quick-action name in the brand colour. */
    public static Icon of(String action) {
        return of(action, UIUtil.BRAND);
    }

    /** Returns the icon for a dashboard quick-action name in a custom colour. */
    public static Icon of(String action, Color color) {
        return new VectorIcon(painterFor(action), color);
    }

    private static Painter painterFor(String action) {
        switch (action) {
            case "New Milk Entry":     return ButtonIcons::paintMilkEntry;
            case "Customers":          return ButtonIcons::paintCustomers;
            case "Rate Chart":         return ButtonIcons::paintRateChart;
            case "Today's Collection": return ButtonIcons::paintClipboard;
            case "Customer Report":    return ButtonIcons::paintDocument;
            case "Monthly Report":     return ButtonIcons::paintCalendar;
            case "Payments":           return ButtonIcons::paintBanknote;
            case "Backup":             return ButtonIcons::paintShield;
            case "Plus":               return ButtonIcons::paintPlus;
            case "Save":               return ButtonIcons::paintSave;
            case "Refresh":            return ButtonIcons::paintRefresh;
            case "Trash":              return ButtonIcons::paintTrash;
            case "EditSquare":         return ButtonIcons::paintEditSquare;
            case "Pencil":             return ButtonIcons::paintPencil;
            case "Cross":              return ButtonIcons::paintCross;
            case "Key":                return ButtonIcons::paintKey;
            case "MapPin":             return ButtonIcons::paintMapPin;
            case "Phone":              return ButtonIcons::paintPhone;
            case "Envelope":           return ButtonIcons::paintEnvelope;
            case "Eye":                return ButtonIcons::paintEye;
            case "EyeOff":             return ButtonIcons::paintEyeOff;
            case "Printer":            return ButtonIcons::paintPrinter;
            case "Download":           return ButtonIcons::paintDownload;
            case "Lock":               return ButtonIcons::paintLock;
            case "Search":             return ButtonIcons::paintSearch;
            case "Upload":             return ButtonIcons::paintUpload;
            case "Cloud":              return ButtonIcons::paintCloud;
            case "Check":              return ButtonIcons::paintCheck;
            case "Info":               return ButtonIcons::paintInfo;
            case "Warning":            return ButtonIcons::paintWarning;
            case "Error":              return ButtonIcons::paintError;
            case "Question":           return ButtonIcons::paintQuestion;
            default:                   return ButtonIcons::paintDocument;
        }
    }

    private static final class VectorIcon implements Icon {
        private final Painter painter;
        private final Color color;

        VectorIcon(Painter painter, Color color) {
            this.painter = painter;
            this.color = color;
        }

        @Override
        public int getIconWidth() {
            return 20;
        }

        @Override
        public int getIconHeight() {
            return 20;
        }

        @Override
        public void paintIcon(Component comp, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            painter.paint(g2, 20f, color);
            g2.dispose();
        }
    }

    private static BasicStroke stroke(float s, float w) {
        return new BasicStroke(s * w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    // ---- individual icons ----

    private static void paintMilkEntry(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.08f));
        Path2D drop = new Path2D.Float();
        drop.moveTo(s * 0.42f, s * 0.10f);
        drop.curveTo(s * 0.28f, s * 0.26f, s * 0.16f, s * 0.42f, s * 0.16f, s * 0.60f);
        drop.quadTo(s * 0.16f, s * 0.88f, s * 0.42f, s * 0.88f);
        drop.quadTo(s * 0.68f, s * 0.88f, s * 0.68f, s * 0.60f);
        drop.curveTo(s * 0.68f, s * 0.42f, s * 0.56f, s * 0.26f, s * 0.42f, s * 0.10f);
        drop.closePath();
        g2.draw(drop);
        g2.draw(new Line2D.Float(s * 0.74f, s * 0.30f, s * 0.94f, s * 0.30f));
        g2.draw(new Line2D.Float(s * 0.84f, s * 0.20f, s * 0.84f, s * 0.40f));
    }

    private static void paintCustomers(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.075f));
        g2.draw(new Ellipse2D.Float(s * 0.14f, s * 0.14f, s * 0.24f, s * 0.24f));
        g2.draw(new Ellipse2D.Float(s * 0.58f, s * 0.14f, s * 0.24f, s * 0.24f));
        Path2D left = new Path2D.Float();
        left.moveTo(s * 0.08f, s * 0.86f);
        left.quadTo(s * 0.08f, s * 0.50f, s * 0.26f, s * 0.50f);
        left.quadTo(s * 0.44f, s * 0.50f, s * 0.44f, s * 0.86f);
        g2.draw(left);
        Path2D right = new Path2D.Float();
        right.moveTo(s * 0.56f, s * 0.86f);
        right.quadTo(s * 0.56f, s * 0.50f, s * 0.74f, s * 0.50f);
        right.quadTo(s * 0.92f, s * 0.50f, s * 0.92f, s * 0.86f);
        g2.draw(right);
    }

    private static void paintRateChart(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.075f));
        Path2D axis = new Path2D.Float();
        axis.moveTo(s * 0.16f, s * 0.12f);
        axis.lineTo(s * 0.16f, s * 0.84f);
        axis.lineTo(s * 0.90f, s * 0.84f);
        g2.draw(axis);
        g2.fill(new Rectangle2D.Float(s * 0.26f, s * 0.54f, s * 0.13f, s * 0.30f));
        g2.fill(new Rectangle2D.Float(s * 0.44f, s * 0.38f, s * 0.13f, s * 0.46f));
        g2.fill(new Rectangle2D.Float(s * 0.62f, s * 0.24f, s * 0.13f, s * 0.60f));
    }

    private static void paintClipboard(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.075f));
        g2.draw(new RoundRectangle2D.Float(s * 0.22f, s * 0.16f, s * 0.56f, s * 0.72f,
                s * 0.10f, s * 0.10f));
        g2.fill(new RoundRectangle2D.Float(s * 0.40f, s * 0.10f, s * 0.20f, s * 0.12f,
                s * 0.06f, s * 0.06f));
        g2.draw(new Line2D.Float(s * 0.32f, s * 0.42f, s * 0.68f, s * 0.42f));
        g2.draw(new Line2D.Float(s * 0.32f, s * 0.54f, s * 0.68f, s * 0.54f));
        g2.draw(new Line2D.Float(s * 0.32f, s * 0.66f, s * 0.56f, s * 0.66f));
    }

    private static void paintDocument(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.075f));
        Path2D page = new Path2D.Float();
        page.moveTo(s * 0.30f, s * 0.10f);
        page.lineTo(s * 0.62f, s * 0.10f);
        page.lineTo(s * 0.74f, s * 0.24f);
        page.lineTo(s * 0.74f, s * 0.88f);
        page.lineTo(s * 0.30f, s * 0.88f);
        page.closePath();
        g2.draw(page);
        g2.draw(new Line2D.Float(s * 0.62f, s * 0.10f, s * 0.62f, s * 0.24f));
        g2.draw(new Line2D.Float(s * 0.62f, s * 0.24f, s * 0.74f, s * 0.24f));
        g2.draw(new Line2D.Float(s * 0.38f, s * 0.44f, s * 0.66f, s * 0.44f));
        g2.draw(new Line2D.Float(s * 0.38f, s * 0.56f, s * 0.66f, s * 0.56f));
        g2.draw(new Line2D.Float(s * 0.38f, s * 0.68f, s * 0.58f, s * 0.68f));
    }

    private static void paintCalendar(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.075f));
        g2.draw(new RoundRectangle2D.Float(s * 0.16f, s * 0.18f, s * 0.68f, s * 0.68f,
                s * 0.08f, s * 0.08f));
        g2.draw(new Line2D.Float(s * 0.16f, s * 0.34f, s * 0.84f, s * 0.34f));
        g2.draw(new Line2D.Float(s * 0.32f, s * 0.10f, s * 0.32f, s * 0.24f));
        g2.draw(new Line2D.Float(s * 0.68f, s * 0.10f, s * 0.68f, s * 0.24f));
        float d = s * 0.08f;
        g2.fill(new Ellipse2D.Float(s * 0.30f, s * 0.44f, d, d));
        g2.fill(new Ellipse2D.Float(s * 0.46f, s * 0.44f, d, d));
        g2.fill(new Ellipse2D.Float(s * 0.62f, s * 0.44f, d, d));
        g2.fill(new Ellipse2D.Float(s * 0.30f, s * 0.62f, d, d));
        g2.fill(new Ellipse2D.Float(s * 0.46f, s * 0.62f, d, d));
        g2.fill(new Ellipse2D.Float(s * 0.62f, s * 0.62f, d, d));
    }

    private static void paintBanknote(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.075f));
        g2.draw(new RoundRectangle2D.Float(s * 0.08f, s * 0.28f, s * 0.84f, s * 0.44f,
                s * 0.10f, s * 0.10f));
        g2.draw(new Ellipse2D.Float(s * 0.40f, s * 0.36f, s * 0.20f, s * 0.28f));
    }

    private static void paintShield(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.08f));
        Path2D shield = new Path2D.Float();
        shield.moveTo(s * 0.50f, s * 0.08f);
        shield.lineTo(s * 0.78f, s * 0.18f);
        shield.lineTo(s * 0.78f, s * 0.46f);
        shield.quadTo(s * 0.78f, s * 0.72f, s * 0.50f, s * 0.90f);
        shield.quadTo(s * 0.22f, s * 0.72f, s * 0.22f, s * 0.46f);
        shield.lineTo(s * 0.22f, s * 0.18f);
        shield.closePath();
        g2.draw(shield);
        g2.draw(new Line2D.Float(s * 0.37f, s * 0.48f, s * 0.46f, s * 0.58f));
        g2.draw(new Line2D.Float(s * 0.46f, s * 0.58f, s * 0.63f, s * 0.38f));
    }

    /** Plus sign (New button). */
    private static void paintPlus(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.09f));
        g2.draw(new Line2D.Float(s * 0.50f, s * 0.14f, s * 0.50f, s * 0.86f));
        g2.draw(new Line2D.Float(s * 0.14f, s * 0.50f, s * 0.86f, s * 0.50f));
    }

    /** Floppy disk (Save button). */
    private static void paintSave(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.07f));
        // Disk body with a clipped top-right corner.
        Path2D body = new Path2D.Float();
        body.moveTo(s * 0.16f, s * 0.16f);
        body.lineTo(s * 0.70f, s * 0.16f);
        body.lineTo(s * 0.84f, s * 0.30f);
        body.lineTo(s * 0.84f, s * 0.84f);
        body.lineTo(s * 0.16f, s * 0.84f);
        body.closePath();
        g2.draw(body);
        // Shutter at the top with its sliding window.
        g2.draw(new Rectangle2D.Float(s * 0.32f, s * 0.16f, s * 0.32f, s * 0.22f));
        g2.draw(new Line2D.Float(s * 0.58f, s * 0.20f, s * 0.58f, s * 0.34f));
        // Label at the bottom.
        g2.draw(new Rectangle2D.Float(s * 0.30f, s * 0.52f, s * 0.40f, s * 0.32f));
    }

    /** Circular refresh arrows (Update button). */
    private static void paintRefresh(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.075f));
        // Two arcs forming a broken circle.
        g2.draw(new Arc2D.Float(s * 0.18f, s * 0.18f, s * 0.64f, s * 0.64f,
                30f, 120f, Arc2D.OPEN));
        g2.draw(new Arc2D.Float(s * 0.18f, s * 0.18f, s * 0.64f, s * 0.64f,
                210f, 120f, Arc2D.OPEN));
        // Arrowheads at the ends of each arc.
        Path2D head1 = new Path2D.Float();
        head1.moveTo(s * 0.88f, s * 0.26f);
        head1.lineTo(s * 0.68f, s * 0.26f);
        head1.lineTo(s * 0.79f, s * 0.44f);
        head1.closePath();
        g2.fill(head1);
        Path2D head2 = new Path2D.Float();
        head2.moveTo(s * 0.12f, s * 0.74f);
        head2.lineTo(s * 0.32f, s * 0.74f);
        head2.lineTo(s * 0.21f, s * 0.56f);
        head2.closePath();
        g2.fill(head2);
    }

    /** Trash can (Delete button). */
    private static void paintTrash(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.07f));
        // Lid and handle.
        g2.draw(new Line2D.Float(s * 0.18f, s * 0.26f, s * 0.82f, s * 0.26f));
        g2.draw(new Line2D.Float(s * 0.40f, s * 0.26f, s * 0.40f, s * 0.14f));
        g2.draw(new Line2D.Float(s * 0.40f, s * 0.14f, s * 0.60f, s * 0.14f));
        g2.draw(new Line2D.Float(s * 0.60f, s * 0.14f, s * 0.60f, s * 0.26f));
        // Can body.
        g2.draw(new RoundRectangle2D.Float(s * 0.26f, s * 0.26f, s * 0.48f, s * 0.62f,
                s * 0.06f, s * 0.06f));
        // Ridges inside the body.
        g2.draw(new Line2D.Float(s * 0.42f, s * 0.40f, s * 0.42f, s * 0.74f));
        g2.draw(new Line2D.Float(s * 0.50f, s * 0.40f, s * 0.50f, s * 0.74f));
        g2.draw(new Line2D.Float(s * 0.58f, s * 0.40f, s * 0.58f, s * 0.74f));
    }

    /** Square with an overlapping pencil (row edit action). */
    private static void paintEditSquare(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.08f));
        // Open square whose top-right side is left open for the pencil.
        Path2D square = new Path2D.Float();
        square.moveTo(s * 0.78f, s * 0.52f);
        square.lineTo(s * 0.78f, s * 0.86f);
        square.lineTo(s * 0.14f, s * 0.86f);
        square.lineTo(s * 0.14f, s * 0.22f);
        square.lineTo(s * 0.48f, s * 0.22f);
        g2.draw(square);
        // Pencil body lying diagonally over the square's corner.
        g2.draw(new Line2D.Float(s * 0.46f, s * 0.54f, s * 0.76f, s * 0.24f));
        g2.draw(new Line2D.Float(s * 0.76f, s * 0.24f, s * 0.86f, s * 0.34f));
        g2.draw(new Line2D.Float(s * 0.86f, s * 0.34f, s * 0.56f, s * 0.64f));
        Path2D tip = new Path2D.Float();
        tip.moveTo(s * 0.42f, s * 0.58f);
        tip.lineTo(s * 0.56f, s * 0.64f);
        tip.lineTo(s * 0.46f, s * 0.74f);
        tip.closePath();
        g2.fill(tip);
    }

    /** Diagonal pencil (row rate-edit action). */
    private static void paintPencil(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.08f));
        // Body drawn as a slim rotated rectangle.
        g2.draw(new Line2D.Float(s * 0.24f, s * 0.76f, s * 0.72f, s * 0.28f));
        g2.draw(new Line2D.Float(s * 0.62f, s * 0.18f, s * 0.14f, s * 0.66f));
        g2.draw(new Line2D.Float(s * 0.72f, s * 0.28f, s * 0.62f, s * 0.18f));
        g2.draw(new Line2D.Float(s * 0.14f, s * 0.66f, s * 0.24f, s * 0.76f));
        // Lead tip.
        Path2D tip = new Path2D.Float();
        tip.moveTo(s * 0.24f, s * 0.76f);
        tip.lineTo(s * 0.14f, s * 0.66f);
        tip.lineTo(s * 0.06f, s * 0.94f);
        tip.closePath();
        g2.fill(tip);
    }

    /** X / close cross (Clear button). */
    private static void paintCross(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.10f));
        g2.draw(new Line2D.Float(s * 0.22f, s * 0.22f, s * 0.78f, s * 0.78f));
        g2.draw(new Line2D.Float(s * 0.78f, s * 0.22f, s * 0.22f, s * 0.78f));
    }

    /** Key (Change Password button). */
    private static void paintKey(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.09f));
        // Round bow with a hole.
        g2.draw(new Ellipse2D.Float(s * 0.12f, s * 0.36f, s * 0.34f, s * 0.34f));
        g2.draw(new Ellipse2D.Float(s * 0.23f, s * 0.47f, s * 0.12f, s * 0.12f));
        // Shaft running to the bottom-right.
        g2.draw(new Line2D.Float(s * 0.44f, s * 0.56f, s * 0.88f, s * 0.22f));
        // Two teeth pointing down from the shaft.
        g2.draw(new Line2D.Float(s * 0.62f, s * 0.42f, s * 0.72f, s * 0.52f));
        g2.draw(new Line2D.Float(s * 0.74f, s * 0.30f, s * 0.84f, s * 0.40f));
    }

    /** Map pin (Address field trailing icon). */
    private static void paintMapPin(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.09f));
        g2.draw(new Arc2D.Float(s * 0.24f, s * 0.08f, s * 0.52f, s * 0.52f, 0, 360, Arc2D.OPEN));
        g2.draw(new Line2D.Float(s * 0.31f, s * 0.54f, s * 0.50f, s * 0.92f));
        g2.draw(new Line2D.Float(s * 0.69f, s * 0.54f, s * 0.50f, s * 0.92f));
        g2.draw(new Ellipse2D.Float(s * 0.42f, s * 0.26f, s * 0.16f, s * 0.16f));
    }

    /** Telephone handset (Mobile field trailing icon). */
    private static void paintPhone(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(new BasicStroke(s * 0.12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D h = new Path2D.Float();
        h.moveTo(s * 0.26f, s * 0.22f);
        h.quadTo(s * 0.18f, s * 0.44f, s * 0.32f, s * 0.62f);
        h.quadTo(s * 0.46f, s * 0.84f, s * 0.70f, s * 0.82f);
        h.quadTo(s * 0.84f, s * 0.80f, s * 0.80f, s * 0.72f);
        g2.draw(h);
    }

    /** Envelope (Email field trailing icon). */
    private static void paintEnvelope(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.08f));
        g2.draw(new Rectangle2D.Float(s * 0.12f, s * 0.24f, s * 0.76f, s * 0.52f));
        g2.draw(new Line2D.Float(s * 0.14f, s * 0.28f, s * 0.50f, s * 0.58f));
        g2.draw(new Line2D.Float(s * 0.86f, s * 0.28f, s * 0.50f, s * 0.58f));
    }

    /** Open eye (password shown — clicking hides it). */
    private static void paintEye(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.08f));
        // Almond outline from two quadratic curves meeting at the corners.
        Path2D eye = new Path2D.Float();
        eye.moveTo(s * 0.06f, s * 0.50f);
        eye.quadTo(s * 0.50f, s * 0.04f, s * 0.94f, s * 0.50f);
        eye.quadTo(s * 0.50f, s * 0.96f, s * 0.06f, s * 0.50f);
        eye.closePath();
        g2.draw(eye);
        // Pupil.
        g2.fill(new Ellipse2D.Float(s * 0.38f, s * 0.38f, s * 0.24f, s * 0.24f));
    }

    /** Crossed-out eye (password hidden — clicking shows it). */
    private static void paintEyeOff(Graphics2D g2, float s, Color c) {
        paintEye(g2, s, c);
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.09f));
        g2.draw(new Line2D.Float(s * 0.14f, s * 0.86f, s * 0.86f, s * 0.14f));
    }

    /** Printer: paper on top, body with output slot (New / print actions). */
    private static void paintPrinter(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.07f));
        // Paper feeding in at the top.
        g2.draw(new Rectangle2D.Float(s * 0.30f, s * 0.10f, s * 0.40f, s * 0.22f));
        // Printer body.
        g2.draw(new Rectangle2D.Float(s * 0.14f, s * 0.34f, s * 0.72f, s * 0.30f));
        // Output slot paper at the bottom.
        g2.draw(new Rectangle2D.Float(s * 0.30f, s * 0.64f, s * 0.40f, s * 0.24f));
        // Status light on the body.
        g2.fill(new Rectangle2D.Float(s * 0.70f, s * 0.44f, s * 0.07f, s * 0.07f));
    }

    /** Padlock: read-only / auto-calculated field marker. */
    private static void paintLock(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.08f));
        // Shackle.
        g2.draw(new Arc2D.Float(s * 0.30f, s * 0.12f, s * 0.40f, s * 0.44f, 0, 180, Arc2D.OPEN));
        // Body.
        g2.draw(new RoundRectangle2D.Float(s * 0.22f, s * 0.44f, s * 0.56f, s * 0.42f, s * 0.08f, s * 0.08f));
        // Keyhole.
        g2.fill(new Rectangle2D.Float(s * 0.46f, s * 0.56f, s * 0.08f, s * 0.18f));
    }

    /** Magnifying glass (Customer Code field trailing search icon). */
    private static void paintSearch(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.09f));
        g2.draw(new Ellipse2D.Float(s * 0.16f, s * 0.16f, s * 0.46f, s * 0.46f));
        g2.draw(new Line2D.Float(s * 0.58f, s * 0.58f, s * 0.86f, s * 0.86f));
    }

    /** Download: arrow dropping into a tray (Export actions). */
    private static void paintDownload(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.08f));
        // Vertical shaft of the arrow.
        g2.draw(new Line2D.Float(s * 0.50f, s * 0.10f, s * 0.50f, s * 0.55f));
        // Arrow head.
        Path2D head = new Path2D.Float();
        head.moveTo(s * 0.32f, s * 0.38f);
        head.lineTo(s * 0.50f, s * 0.58f);
        head.lineTo(s * 0.68f, s * 0.38f);
        g2.draw(head);
        // Tray at the bottom.
        Path2D tray = new Path2D.Float();
        tray.moveTo(s * 0.14f, s * 0.68f);
        tray.lineTo(s * 0.14f, s * 0.86f);
        tray.lineTo(s * 0.86f, s * 0.86f);
        tray.lineTo(s * 0.86f, s * 0.68f);
        g2.draw(tray);
    }

    /** Upload: arrow rising out of a tray (Import / Export CSV actions). */
    private static void paintUpload(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.08f));
        // Vertical shaft of the arrow.
        g2.draw(new Line2D.Float(s * 0.50f, s * 0.55f, s * 0.50f, s * 0.10f));
        // Arrow head pointing up.
        Path2D head = new Path2D.Float();
        head.moveTo(s * 0.32f, s * 0.28f);
        head.lineTo(s * 0.50f, s * 0.08f);
        head.lineTo(s * 0.68f, s * 0.28f);
        g2.draw(head);
        // Tray at the bottom.
        Path2D tray = new Path2D.Float();
        tray.moveTo(s * 0.14f, s * 0.68f);
        tray.lineTo(s * 0.14f, s * 0.86f);
        tray.lineTo(s * 0.86f, s * 0.86f);
        tray.lineTo(s * 0.86f, s * 0.68f);
        g2.draw(tray);
    }

    /** Cloud: puffy cloud with an up arrow (Backup Database action). */
    private static void paintCloud(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.08f));
        // Cloud body: one large centre lobe and two side lobes.
        g2.draw(new Arc2D.Float(s * 0.10f, s * 0.36f, s * 0.40f, s * 0.40f, 0, 360, Arc2D.OPEN));
        g2.draw(new Arc2D.Float(s * 0.32f, s * 0.20f, s * 0.36f, s * 0.36f, 0, 360, Arc2D.OPEN));
        g2.draw(new Arc2D.Float(s * 0.50f, s * 0.36f, s * 0.40f, s * 0.40f, 0, 360, Arc2D.OPEN));
        // Base line of the cloud.
        g2.draw(new Line2D.Float(s * 0.16f, s * 0.76f, s * 0.84f, s * 0.76f));
        // Up arrow inside the cloud.
        g2.draw(new Line2D.Float(s * 0.50f, s * 0.62f, s * 0.50f, s * 0.40f));
        Path2D head = new Path2D.Float();
        head.moveTo(s * 0.40f, s * 0.48f);
        head.lineTo(s * 0.50f, s * 0.38f);
        head.lineTo(s * 0.60f, s * 0.48f);
        g2.draw(head);
    }

    /** Check mark (OK / Yes buttons). */
    private static void paintCheck(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.10f));
        Path2D check = new Path2D.Float();
        check.moveTo(s * 0.24f, s * 0.55f);
        check.lineTo(s * 0.42f, s * 0.72f);
        check.lineTo(s * 0.78f, s * 0.30f);
        g2.draw(check);
    }

    /** Info badge: circle with an i. */
    private static void paintInfo(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.09f));
        g2.draw(new Ellipse2D.Float(s * 0.14f, s * 0.14f, s * 0.72f, s * 0.72f));
        g2.fill(new Ellipse2D.Float(s * 0.48f, s * 0.28f, s * 0.06f, s * 0.06f));
        g2.draw(new Line2D.Float(s * 0.50f, s * 0.42f, s * 0.50f, s * 0.72f));
    }

    /** Warning badge: triangle with an exclamation mark. */
    private static void paintWarning(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.09f));
        Path2D tri = new Path2D.Float();
        tri.moveTo(s * 0.50f, s * 0.10f);
        tri.lineTo(s * 0.90f, s * 0.86f);
        tri.lineTo(s * 0.10f, s * 0.86f);
        tri.closePath();
        g2.draw(tri);
        g2.draw(new Line2D.Float(s * 0.50f, s * 0.34f, s * 0.50f, s * 0.62f));
        g2.fill(new Ellipse2D.Float(s * 0.47f, s * 0.73f, s * 0.06f, s * 0.06f));
    }

    /** Error badge: circle with a cross. */
    private static void paintError(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.09f));
        g2.draw(new Ellipse2D.Float(s * 0.14f, s * 0.14f, s * 0.72f, s * 0.72f));
        // Cross inside the circle.
        Path2D cross = new Path2D.Float();
        cross.moveTo(s * 0.38f, s * 0.38f);
        cross.lineTo(s * 0.62f, s * 0.62f);
        cross.moveTo(s * 0.62f, s * 0.38f);
        cross.lineTo(s * 0.38f, s * 0.62f);
        g2.draw(cross);
    }

    /** Question badge: circle with a question mark. */
    private static void paintQuestion(Graphics2D g2, float s, Color c) {
        g2.setColor(c);
        g2.setStroke(stroke(s, 0.09f));
        g2.draw(new Ellipse2D.Float(s * 0.14f, s * 0.14f, s * 0.72f, s * 0.72f));
        // Question mark: short vertical with a hook.
        Path2D q = new Path2D.Float();
        q.moveTo(s * 0.50f, s * 0.50f);
        q.curveTo(s * 0.50f, s * 0.34f, s * 0.34f, s * 0.34f, s * 0.38f, s * 0.44f);
        g2.draw(q);
        g2.draw(new Line2D.Float(s * 0.50f, s * 0.56f, s * 0.50f, s * 0.62f));
        g2.fill(new Ellipse2D.Float(s * 0.47f, s * 0.71f, s * 0.06f, s * 0.06f));
    }
}