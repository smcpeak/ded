// Controller.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import java.util.HashSet;
import java.util.Set;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import ded.model.Diagram;
import ded.model.TextAlign;

import util.swing.MenuAction;
import util.swing.SwingUtil;


/** Generic UI object that can be interacted with to edit some part of the diagram. */
public abstract class Controller {
    // ----------- static data -------------
    public static final Color selectedColor = new Color(135, 193, 255);
    public static final Color resizeHandleColor = Color.BLACK;

    public static final int resizeHandleSize = 10;         // 10x10

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

    /** Paint the background with the selection color. */
    public void paintSelectionBackground(Graphics g)
    {
        this.paintBounds(g, Controller.selectedColor);
    }

    /** Draw a representation of the controller and the thing it is
      * controlling. */
    public void paint(Graphics g)
    {}

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

    /** This is called when we're beginning a drag operation.  Normally,
      * controllers can ignore this and just respond to 'dragTo', but if
      * they need to remember some state from the drag start, this is
      * the time to do it.  'pt' is where the mouse was, relative to the
      * diagram up/left, when dragging began. */
    public void beginDragging(Point pt)
    {}

    /** This is called when dragging has ceased. */
    public void stopDragging()
    {}

    /** Show the right click popup menu. */
    @SuppressWarnings("serial")
    public void rightClickMenu(final MouseEvent ev)
    {
        final Controller ths = this;

        JPopupMenu menu = new JPopupMenu();

        // I tried making Enter an Accel key so it would show up in
        // the right click menu as a key binding hint, but that causes
        // a press of Enter while the menu is open to activate the Edit
        // entry even if another menu item is selected.
        menu.add(new MenuAction("Edit...", 0) {
            public void actionPerformed(ActionEvent e) {
                ths.edit();
            }
        });

        this.addToRightClickMenu(menu, ev);

        menu.show(this.diagramController, ev.getPoint().x, ev.getPoint().y);
    }

    /** Add more items to the right click menu if desired.  'ev' is the
      * click that opened the menu, which can be useful if a menu item
      * will do something with the clicked location. */
    protected void addToRightClickMenu(JPopupMenu menu, MouseEvent ev)
    {}

    /** Respond to a mouse press on the controller.  The default action
      * opens the right click menu on right click. */
    public void mousePressed(MouseEvent e)
    {
        if (SwingUtilities.isRightMouseButton(e)) {
            // If this controller is not selected, it should become
            // the only selection.
            if (!this.isSelected()) {
                this.diagramController.selectOnly(this);
            }

            this.rightClickMenu(e);
        }
    }

    /** Handle a key pressed while the controller is selected.  If it
      * returns true, the controller is treated as having processed
      * the key, so the parent widget will ignore it. */
    public boolean keyPressed(KeyEvent e)
    {
        return false;
    }

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

    /** Edit the attributes of the controlled element using a dialog box,
      * or show an error dialog if that is not possible. */
    public void edit()
    {
        this.diagramController.errorMessageBox(
            "This kind of element cannot be edited.");
    }

    /** Insert a control point, or show an error dialog if that is not
      * possible. */
    public void insertControlPoint()
    {
        this.diagramController.errorMessageBox(
            "This kind of element cannot have control points inserted.");
    }

    /** Delete this control and its data. */
    public void deleteSelfAndData(Diagram diagram)
    {}

    /** Return true if this controller should be subject to selection
      * using the lasso.  The default is true. */
    public boolean wantLassoSelection()
    {
        return true;
    }

    /** This is called after we reload the referenced images in the
      * diagram.  The controller might need to take further action. */
    public void updateAfterImageReload()
    {}

    /** Update this element's line color, if it has one. */
    public void setLineColor(String color)
    {}

    /** Update this element's text color if it has one. */
    public void setTextColor(String color)
    {}

    /** Update this element's name text align if it has one.
      * This does *not* change the alignment of the attribute
      * text of an Entity. */
    public void setNameTextAlign(TextAlign align)
    {}
}

// EOF
