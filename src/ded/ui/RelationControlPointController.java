// RelationControlPointController.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import util.swing.MenuAction;

import static util.StringUtil.fmt;

/** Control the position of a control point in the middle of a Relation. */
public class RelationControlPointController extends ResizeController {
    // -------------------- instance data --------------------
    /** Relation controller we're a part of. */
    public RelationController rcontroller;

    /** Which control point is this for?  This is a 0-based index into
      * rcontroller.relation.controlPts[]. */
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

        // Do not set dirty bit.  Wait for mouse button release.
    }

    @Override
    public void selfCheck()
    {
        assert(this.diagramController.contains(this.rcontroller));
        assert(0 <= this.which);
        assert(     this.which < this.rcontroller.relation.controlPts.size());
    }

    @SuppressWarnings("serial")
    @Override
    public void mousePressed(MouseEvent ev)
    {
        super.mousePressed(ev);

        if (SwingUtilities.isRightMouseButton(ev)) {
            final RelationControlPointController thisController = this;

            // Construct menu.
            JPopupMenu menu = new JPopupMenu("Relation Control Point");

            menu.add(new MenuAction("Properties", KeyEvent.VK_P) {
                public void actionPerformed(ActionEvent e) {
                    thisController.showProperties();
                }
            });

            menu.add(new MenuAction("Delete", KeyEvent.VK_D) {
                public void actionPerformed(ActionEvent e) {
                    thisController.deleteControlPoint();
                }
            });

            // Show the popup menu.  This does *not* wait for the choice to be made.
            menu.show(this.diagramController, ev.getPoint().x, ev.getPoint().y);
        }
    }

    /** Show the properties dialog for the control point. */
    private void showProperties()
    {
        if ((new RelationControlPointDialog(this.diagramController,
                                            this.rcontroller.relation,
                                            this.which)).exec()) {
            this.diagramController.diagramChanged(
                fmt("Edit control point %1$d of %2$d",
                    this.which+1,
                    this.rcontroller.relation.controlPts.size()));
        }
    }

    /** Delete this control point. */
    private void deleteControlPoint()
    {
        this.rcontroller.deleteControlPoint(this.which);

        // Careful: 'this' has been removed from the DiagramController.
    }
}

// EOF
