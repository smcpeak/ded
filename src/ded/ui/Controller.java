// Controller.java

package ded.ui;

import java.awt.Graphics;
import java.awt.Point;

/** Generic UI object that can be interacted with to edit some part of the diagram. */
public abstract class Controller {
    // ----------- public data ------------
    /** Owning diagram controller.  Each controller has this reference 
      * so it can adjust the set of controllers. */
    public DiagramController diagramController;
    
    // ----------- public methods ----------
    public Controller(DiagramController dc)
    {
        this.diagramController = dc;
    }
    
    /** Principal location for this controller, like a registration point.
      * This is used as a reference point for drag operations. */
    public abstract Point getLoc();
    
    /** Draw a representation of the controller and the thing it is controlling. */
    public void paint(Graphics g)
    {
        // TODO: Selection polygon.
    }
}

// EOF
