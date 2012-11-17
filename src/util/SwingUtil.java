// SwingUtil.java

package util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.InputEvent;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JTextField;

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
    
    /** Make a vertical layout box. */
    public static Box makeVBox(Container parent)
    {
        Box b = Box.createVerticalBox();
        parent.add(b);
        return b;
    }
    
    /** Make a horizontal layout box. */
    public static Box makeHBox(Container parent)
    {
        Box b = Box.createHorizontalBox();
        parent.add(b);
        return b;
    }
    
    /** Make a vertical layout box with the given margin. */
    public static Box makeMarginVBox(Container parent, int margin)
    {
        Box hb = makeHBox(parent);
        hb.add(Box.createHorizontalStrut(margin));
        
        Box vb = makeVBox(hb);
        vb.add(Box.createVerticalStrut(margin));
        
        Box ret = makeVBox(vb);
        
        vb.add(Box.createVerticalStrut(margin));
        hb.add(Box.createHorizontalStrut(margin));

        return ret;
    }
    
    /** Create a line edit control and associated label. */
    public static JTextField makeLineEdit(Container parent, String label, char mnemonic,
                                          String initialValue)
    {
        Box hbox = makeHBox(parent);
        JLabel labelControl = new JLabel(label);
        labelControl.setDisplayedMnemonic(mnemonic);
        hbox.add(labelControl);
        
        hbox.add(Box.createHorizontalStrut(5));
        
        JTextField ret = new JTextField(initialValue);
        hbox.add(ret);
        labelControl.setLabelFor(ret);

        disallowVertStretch(hbox);
        
        return ret;
    }
    
    /** Set min/max height to preferred height in order to disallow
      * vertical stretching. */
    public static void disallowVertStretch(Component c)
    {
        Dimension pref = c.getPreferredSize();
        Dimension max = c.getMaximumSize();
        Dimension min = c.getMinimumSize();
        max.height = pref.height;
        min.height = pref.height;
        c.setMaximumSize(max);
        c.setMinimumSize(min);
    }
}

// EOF
