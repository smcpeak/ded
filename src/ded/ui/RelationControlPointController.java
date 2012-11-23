// RelationControlPointController.java

package ded.ui;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Control the position of a control point in the middle of a Relation. */
public class RelationControlPointController extends ResizeController {
    // -------------------- instance data --------------------
    /** Relation controller we're a part of. */
    public RelationController rcontroller;
    
    /** Which control point is this for? */
    public int which;
    
    // ----------------------- methods -----------------------
    public RelationControlPointController(
        DiagramController diagramController,
        RelationController relationController, 
        int w)
    {
        super(diagramController);
        this.rcontroller = relationController;
        this.which = w;
        
        this.selfCheck();
    }

    @Override
    public Point getLoc()
    {
        return this.rcontroller.relation.controlPts.get(this.which);
    }
    
    @Override
    public void dragTo(Point pt)
    {
        this.rcontroller.relation.controlPts.set(this.which, pt);
    }

    @Override
    public void selfCheck()
    {
        assert(this.diagramController.contains(this.rcontroller));
        assert(0 <= this.which);
        assert(     this.which < this.rcontroller.relation.controlPts.size());
    }
    
    @Override
    public void mousePressed(MouseEvent ev)
    {
        super.mousePressed(ev);
        
        if (SwingUtilities.isRightMouseButton(ev)) {
            // Context menu choices.
            String items[] = {
                "&Properties",
                "&Delete"
            };
            
            // Launch menu.
            int choice = this.diagramController.popupMenu(
                ev.getPoint(), "Relation Control Point", items);
            switch (choice) {
                case 0:
                    JOptionPane.showMessageDialog(this.diagramController,
                        "TODO: Properties of control point");
                    break;
                
                case 1:
                    this.rcontroller.deleteControlPoint(this.which);
                    
                    // Careful: 'this' has been removed from the DiagramController.
                    
                    break;
            }
        }
    }
}

// EOF
