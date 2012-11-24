// SwingUtil.java

package util.swing;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.font.LineMetrics;

import javax.swing.AbstractAction;

import util.IntRange;
import util.Util;
import util.awt.HorizOrVert;

/** Miscellaneous Swing-related utililities. */
public class SwingUtil {
    /** Return 'a' + 'b'. */
    public static Point add(Point a, Point b)
    {
        return new Point(a.x + b.x, a.y + b.y);
    }
    
    /** Return 'a' - 'b'. */
    public static Point subtract(Point a, Point b)
    {
        return new Point(a.x - b.x, a.y - b.y);
    }
    
    /** Return a Polygon for the rectangle with UL at (x,y) and
      * 'width' and 'height'. */
    public static Polygon rectPolygon(int x, int y, int w, int h)
    {
        int[] px = new int[4];
        int[] py = new int[4];
        
        // Counterclockwise starting at upper left.
        px[0] = x;           py[0] = y;
        px[1] = x;           py[1] = y+h;
        px[2] = x+w;         py[2] = y+h;
        px[3] = x+w;         py[3] = y;
        
        return new Polygon(px, py, 4);
    }
    
    /** Return a Polygon for a Rectangle. */
    public static Polygon rectPolygon(Rectangle r)
    {
        return rectPolygon(r.x, r.y, r.width, r.height);
    }

    /** Return true if the Control button was held when 'e' was generated. */
    public static boolean controlPressed(InputEvent e)
    {
        return (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
    }

    /** Return true if the Shift button was held when 'e' was generated. */
    public static boolean shiftPressed(InputEvent e)
    {
        return (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
    }

    /** Return true if no keyboard modifiers or mouse buttons were held
      * when 'e' was generated. */
    public static boolean noModifiers(KeyEvent e)
    {
        return e.getModifiersEx() == 0;
    }
    
    /** Return the nearest multiple of 'snap'. */
    public static int snapInt(int x, int snap)
    {
        x += snap/2;
        x -= x % snap;
        return x;
    }
    
    /** Return a new point whose coordinates are the nearest multiples of 'snap'. */
    public static Point snapPoint(Point p, int snap)
    {
        return new Point(snapInt(p.x, snap), snapInt(p.y, snap));
    }
    
    /** Send a message to close a window.
      *
      * I do not really understand whether or why this is better than
      * simply calling dispose(), but I infer from code snippets that
      * it may be. */
    public static void closeWindow(Window window)
    {
        window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
    }
    
    /** An action to close a window or dialog. */
    public static class WindowCloseAction extends AbstractAction {
        private static final long serialVersionUID = -1615998248180527506L;
        
        /** The window that will be closed. */
        public Window window;
        
        public WindowCloseAction(Window window)
        {
            this.window = window;
        }

        @Override
        public void actionPerformed(ActionEvent event)
        {
            closeWindow(this.window);
        }
    }
    
    /** Get the center of 'r'. */
    public static Point getCenter(Rectangle r)
    {
        return new Point(r.x + r.width/2, r.y + r.height/2);
    }
    
    /** Draw 'str' centered at 'p'. */
    public static void drawCenteredText(Graphics g, Point p, String str)
    {
        FontMetrics fm = g.getFontMetrics();
        LineMetrics lm = fm.getLineMetrics(str, g);

        // Go to 'p', then add (a+d)/2 to get to the bottom
        // of the text, then subtract d; then simplify algebraically.
        int baseY = p.y + (int)((lm.getAscent() - lm.getDescent())/2);
        int baseX = p.x - fm.stringWidth(str)/2;
        
        g.drawString(str, baseX, baseY);
    }

    /** Return the midpoint of a line segment from 'p' to 'q'. */
    public static Point midPoint(Point p, Point q)
    {
        return new Point(Util.avg(p.x, q.x), Util.avg(p.y, q.y));
    }
    
    /** Draw 'str' at the given location, but process newlines by moving
      * to a new line. */
    public static void drawTextWithNewlines(Graphics g, String str, int x, int y)
    {
        String lines[] = str.split("\n");
        int lineHeight = g.getFontMetrics().getHeight();
        for (String s : lines) {
            g.drawString(s, x, y);
            y += lineHeight;
        }
    }
    
    /** Return a new rectangle that exceeds 'r' by 'd' pixels on all
      * sides, keeping the center the same.  * If 'd' is negative,
      * the new rectangle is smaller. */  
    public static Rectangle growRectangle(Rectangle r, int d)
    {
        return new Rectangle(r.x - d,
                             r.y - d,
                             r.width + d*2,
                             r.height + d*2);
    }

    /** Return the range of integer values encompassed by 'rect'
      * in the 'hv' dimension. */
    public static IntRange getRectRange(HorizOrVert hv, Rectangle rect)
    {
        if (hv.isHoriz()) {
            // The -1 is because, when drawn, the number of pixels equals
            // 'width', not width+1.
            return new IntRange(rect.x, Math.max(rect.x, rect.x + rect.width - 1));
        }
        else {
            return new IntRange(rect.y, Math.max(rect.y, rect.y + rect.height - 1));
        }
    }
}

// EOF
