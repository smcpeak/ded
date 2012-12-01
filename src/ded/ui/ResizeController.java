// ResizeController.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.SwingUtilities;

import util.awt.GeomUtil;

/** Functionality common to resize handles. */
public abstract class ResizeController extends Controller {
    // ------------ methods ----------------
    public ResizeController(DiagramController dc)
    {
        super(dc);
    }

    /** Return the location of a resize handle centered at 'pt'. */
    private static Rectangle getResizeHandleRect(Point pt)
    {
        int s = resizeHandleSize;
        return new Rectangle(pt.x - s/2, pt.y - s/2, s, s);
    }

    @Override
    public Set<Polygon> getBounds()
    {
        HashSet<Polygon> ret = new HashSet<Polygon>();
        ret.add(GeomUtil.rectPolygon(getResizeHandleRect(this.getLoc())));
        return ret;
    }

    @Override
    public void paint(Graphics g)
    {
        super.paint(g);
        this.paintBounds(g, resizeHandleColor);
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        if (SwingUtilities.isLeftMouseButton(e)) {
            this.diagramController.beginDragging(this, e.getPoint());
        }
    }

    @Override
    public boolean wantLassoSelection()
    {
        return false;
    }
}

// EOF
