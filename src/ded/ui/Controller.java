// Controller.java

package ded.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import ded.model.Diagram;

import util.SwingUtil;


/** Generic UI object that can be interacted with to edit some part of the diagram. */
public abstract class Controller {
    // ----------- private static data -------------
    private static final Color selectedColor = new Color(135, 193, 255);
    //private static final Color resizeHandleColor = Color.BLACK;

    // ----------- protected data ------------
    /** When the controller is selected, it can be manipulated with keyboard
      * commands and it is drawn differently. */ 
    protected SelectionState selState;
    
    // ----------- public data ------------
    /** Owning diagram controller.  Each controller has this reference 
      * so it can adjust the set of controllers. */
    public DiagramController diagramController;
    
    // ----------- methods ----------
    public Controller(DiagramController dc)
    {
        this.selState = SelectionState.SS_UNSELECTED;
        this.diagramController = dc;
    }
    
    /** Principal location for this controller, like a registration point.
      * This is used as a reference point for drag operations. */
    public abstract Point getLoc();
    
    /** Draw a representation of the controller and the thing it is controlling. */
    public void paint(Graphics g)
    {
        if (this.isSelected()) {
            this.paintBounds(g, Controller.selectedColor);
        }
    }

    /** Paint 'getBounds' with a solid color. */
    protected void paintBounds(Graphics g0, Color c)
    {
        // Use a new object so changes are local.
        Graphics g = g0.create();
        
        g.setColor(c);
        
        Set<Polygon> polys = this.getBounds();
        for (Polygon p : polys) {
            g.fillPolygon(p);
        }
    }

    public SelectionState getSelState()
    {
        return this.selState;
    }
    
    public boolean isSelected()
    {
        return this.selState != SelectionState.SS_UNSELECTED;
    }

    /** Set 'selState'. */
    public void setSelected(SelectionState ss)
    {
        this.selState = ss;
    }

    /** Return a set of polygons describing this controller's click boundary. */
    public Set<Polygon> getBounds()
    {
        return new HashSet<Polygon>();
    }
    
    /** Return true if 'point' is within this controller's click boundary. */
    public boolean boundsContains(Point point)
    {
        Set<Polygon> bounds = getBounds();
        for (Polygon p : bounds) {
            if (p.contains(point)) {
                return true;
            }
        }
        return false;
    }
    
    /** Return true if 'rect' intersects the click boundary. */
    public boolean boundsIntersects(Rectangle rect)
    {
        Set<Polygon> bounds = getBounds();
        for (Polygon p : bounds) {
            if (p.intersects(rect)) {
                return true;
            }
        }
        return false;
    }

    /** Respond to a click by changing selection state.  If 'wantDrag' is true,
      * then also begin dragging when appropriate. */  
    public void mouseSelect(MouseEvent e, boolean wantDrag)
    {
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (SwingUtil.controlPressed(e)) {
                // Toggle selection state
                this.diagramController.toggleSelection(this);
            }
            else {
                if (!this.isSelected()) {
                    // Select myself (only).
                    this.diagramController.selectOnly(this);
                }
                if (wantDrag) {
                    // begin dragging
                    this.diagramController.beginDragging(this, e.getPoint());
                }
            }
        }
    }
    
    // Input handlers.
    public void mousePressed(MouseEvent e) {}

    /** Assert invariants. */
    public void selfCheck()
    {}
    
    /** Assert invariants against diagram too. */
    public void globalSelfCheck(Diagram diagram)
    {
        this.selfCheck();
    }

    /** Move the 'getLoc()' point to 'p'.  Default implementation is no-op. */
    // Should this be renamed to 'setLoc'?
    public void dragTo(Point p)
    {}

    /** Edit the attributes of the controlled element using a dialog box. */
    public void edit()
    {}
}

// EOF
