// WindowCenterController.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.Point;

import ded.model.Entity;
import ded.model.EntityShape;

/** Controller that allows the user to move the window resize center for
  * an entity that represents a Window in a UI wireframe. */
public class WindowCenterController extends ResizeController {
    // --------------- instance data ---------------
    /** The controller for the entity whose center we are controlling. */
    public EntityController econtroller;

    // ----------------- methods --------------------
    public WindowCenterController(DiagramController dc, EntityController econtroller)
    {
        super(dc);
        this.econtroller = econtroller;
        assert(this.getEntity().shape == EntityShape.ES_WINDOW);
    }

    /** Get the entity being controlled. */
    public Entity getEntity()
    {
        return this.econtroller.entity;
    }

    @Override
    public Point getLoc()
    {
        Entity e = this.getEntity();
        return new Point(e.loc.x + e.getShapeParam(0),
                         e.loc.y + e.getShapeParam(1));
    }

    @Override
    public void edit()
    {
        // Same as in EntityResizeController.
        this.econtroller.edit();
    }

    @Override
    public void dragTo(Point pt)
    {
        Entity e = this.getEntity();
        this.getEntity().setShapeParams(pt.x - e.loc.x, pt.y - e.loc.y);
    }
}
