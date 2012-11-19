// EntityResizeController.java

package ded.ui;

import java.awt.Point;
import java.awt.Rectangle;

import util.SwingUtil;

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
        Point delta = SwingUtil.subtract(pt, this.getLoc());
        
        int minSize = EntityController.minimumEntitySize;
        
        // Adjust the handle location by 'delta', taking care not to make
        // the width or height smaller than 'minSize'.
        
        switch (this.whichHandle.handleX) {
            case 0: {
                int newLeft = this.econtroller.getLeft() + delta.x;
                newLeft = Math.min(this.econtroller.getRight() - minSize, newLeft);
                this.econtroller.setLeft(newLeft);
                break;
            }
            
            case 2: {
                int newRight = this.econtroller.getRight() + delta.x;
                newRight = Math.max(this.econtroller.getLeft() + minSize, newRight);
                this.econtroller.setRight(newRight);
                break;
            }
        }
        
        switch (this.whichHandle.handleY) {
            case 0: {
                int newTop = this.econtroller.getTop() + delta.y;
                newTop = Math.min(this.econtroller.getBottom() - minSize, newTop);
                this.econtroller.setTop(newTop);
                break;
            }
            
            case 2: {
                int newBottom = this.econtroller.getBottom() + delta.y;
                newBottom = Math.max(this.econtroller.getTop() + minSize, newBottom);
                this.econtroller.setBottom(newBottom);
                break;
            }
        }
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
