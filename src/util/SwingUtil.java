// SwingUtil.java

package util;

import java.awt.Point;
import java.awt.Polygon;

/** Miscellaneous Swing-related utililities. */
public class SwingUtil {
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
}

// EOF
