// EntityResizeController.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.Point;
import java.awt.Rectangle;

import util.awt.GeomUtil;

/** Controller to allow resizing an entity. */
public class EntityResizeController extends ResizeController {
    // -------------- instance data --------------
    /** Entity controller we are helping to resize. */
    public EntityController econtroller;

    /** Which resize handle this is. */
    public ResizeHandle whichHandle;

    // -------------- methods ----------------
    public EntityResizeController(
        DiagramController dc,
        EntityController ec,
        ResizeHandle wh)
    {
        super(dc);
        this.econtroller = ec;
        this.whichHandle = wh;

        this.selfCheck();
    }

    @Override
    public Point getLoc()
    {
        // Center of the handle, computed from the controlled entity's location.
        Rectangle r = this.econtroller.getRect();
        Point p = new Point(r.x, r.y);
        p.x += r.width * this.whichHandle.handleX / 2;
        p.y += r.height * this.whichHandle.handleY / 2;
        return p;
    }

    @Override
    public void dragTo(Point pt)
    {
        this.selfCheck();

        // How far do we want to move the handle?
        Point delta = GeomUtil.subtract(pt, this.getLoc());

        int minSize = EntityController.minimumEntitySize;

        // Adjust the handle location by 'delta'.  For top/left, we
        // protect minimum size here, but bottom/right is handled
        // inside the 'resize' call.  (There is not a very good
        // reason for the inconsistency.)

        switch (this.whichHandle.handleX) {
            case 0: {
                int newLeft = this.econtroller.getLeft() + delta.x;
                newLeft = Math.min(this.econtroller.getRight() - minSize, newLeft);
                this.econtroller.resizeSetLeft(newLeft);
                break;
            }

            case 2: {
                int newRight = this.econtroller.getRight() + delta.x;
                this.econtroller.resizeSetRight(newRight, true /*direct*/);
                break;
            }
        }

        switch (this.whichHandle.handleY) {
            case 0: {
                int newTop = this.econtroller.getTop() + delta.y;
                newTop = Math.min(this.econtroller.getBottom() - minSize, newTop);
                this.econtroller.resizeSetTop(newTop);
                break;
            }

            case 2: {
                int newBottom = this.econtroller.getBottom() + delta.y;
                this.econtroller.resizeSetBottom(newBottom, true /*direct*/);
                break;
            }
        }

        // Do not set dirty bit.  Wait until mouse is released.
    }

    @Override
    public void edit()
    {
        // If I double-click on an entity, sometimes I hit the resize
        // controller since it appears on the first click.  Just delegate
        // the message to the entity controller.
        this.econtroller.edit();
    }

    @Override
    public void selfCheck()
    {
        super.selfCheck();

        // The entity controller should be an active controller.
        assert(this.diagramController.contains(this.econtroller));
    }
}

// EOF
