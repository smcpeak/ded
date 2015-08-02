// InheritanceController.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import util.IntRange;
import util.awt.GeomUtil;
import util.awt.HorizOrVert;
import util.swing.SwingUtil;

import ded.model.Diagram;
import ded.model.Inheritance;
import ded.model.RelationEndpoint;

import static util.StringUtil.localize;

/** Controller for an Inheritance. */
public class InheritanceController extends Controller {
    // ----------------------- constants ------------------------

    // The shape I want to draw is an equilateral triangle with the
    // center 'pt' on the midpoint of the base edge AB.
    //
    //                         +C                       -
    //                        / \                       A
    //                       /   \                      |
    //                      /     \                     |
    //                     /       \                    |
    //                    /         \                   |
    //                   /           \                  |
    //                  /             \                 |
    //                 /               \                |
    //                /                 \               |height
    //               /                   \              |
    //              /                     \             |
    //             /                       \            |
    //            /                         \           |
    //           /                           \          |
    //          /                             \         |
    //         /                               \        |
    //        /            center 'pt'          \       |
    //       /                 |                 \      |
    //      /                  V                  \     V
    //    A+-------------------*-------------------+B   -
    //
    //     |<---- radius ----->|
    //
    //   angle CAB is 60 degrees
    //
    //                        height
    //   tan CAB   = tan 60 = ------ = sqrt(3)
    //                        radius

    public static final int radius = 10;
    public static final int height = (int)(Math.sqrt(3) * radius);
    public static final int selectionBoxMargin = 5;

    public static final int inheritLineWidth = 2;

    public static final Color inheritFillColor = Color.BLACK;
    public static final Color inheritLineColor = Color.BLACK;

    // -------------------- instance data -----------------------
    /** Thing being controlled. */
    public Inheritance inheritance;

    // ----------------------- methods --------------------------
    public InheritanceController(DiagramController dc, Inheritance i)
    {
        super(dc);
        this.inheritance = i;

        this.selfCheck();
    }

    private static Point distanceFromPointToRect(Rectangle rectangle, Point pt)
    {
        Point ret = new Point(0,0);

        for (HorizOrVert hv : HorizOrVert.allValues()) {
            IntRange r = GeomUtil.getRectRange(hv, rectangle);
            int p = hv.get(pt);
            if (r.contains(p)) {
                // Leave the 'hv' dimension at 0.
            }
            else {
                // Set it to the distance to the closer edge.
                hv.set(ret, p - r.closerEnd(p));
            }
        }

        return ret;
    }

    /** Return a 1-length vector that points towards the
      * parent entity, or (0,0) if we're right on top if it
      * so can't decide. */
    private Point getOrientation()
    {
        Point d = distanceFromPointToRect(
            this.inheritance.parent.getRect(), this.getLoc());
        if (d.x == 0 && d.y == 0) {
            return d;
        }

        // Actually wanted dist from rect to point.
        d.x = -d.x;
        d.y = -d.y;

        // Dominant direction?  Favor horizontal arbitrarily.
        if (Math.abs(d.x) > Math.abs(d.y)) {
            // Horizontal.
            d.y = 0;
            d.x = (d.x>0 ? 1 : -1);
        }
        else {
            // Vertical.
            d.x = 0;
            d.y = (d.y>0 ? 1 : -1);
        }

        return d;
    }

    /** Compute points in the triangle.
      *
      * From the diagram above, A is [0], B is [1], C is [2]. */
    private ArrayList<Point> computePoints()
    {
        Point orient = this.getOrientation();

        // NOTE: I think the labeling might be wrong, because these
        // rotations are for Cartesian coordinates, not AWT coordinates.

        ArrayList<Point> points = new ArrayList<Point>();
        points.add(GeomUtil.add(this.getLoc(), GeomUtil.mult(GeomUtil.row90ccw(orient), radius)));
        points.add(GeomUtil.add(this.getLoc(), GeomUtil.mult(GeomUtil.row90cw(orient), radius)));
        points.add(GeomUtil.add(this.getLoc(), GeomUtil.mult(orient, height)));

        return points;
    }

    @Override
    public Point getLoc()
    {
        return this.inheritance.pt;
    }

    @Override
    public Set<Polygon> getBounds()
    {
        HashSet<Polygon> bounds = new HashSet<Polygon>();

        // Tight bounds.
        ArrayList<Point> pts = this.computePoints();

        // Now make a bigger rectangle so I can select the inheritance
        // despite lots of overlapping relations.
        Rectangle r = GeomUtil.boundingBox(pts);
        r = GeomUtil.growRectangle(r, selectionBoxMargin);
        bounds.add(GeomUtil.rectPolygon(r));

        return bounds;
    }

    @Override
    public void paint(Graphics g0)
    {
        super.paint(g0);

        Graphics2D g = (Graphics2D)(g0.create());

        // Triangle coordinates.
        ArrayList<Point> trianglePoints = this.computePoints();
        Polygon trianglePolygon = GeomUtil.makePolygon(trianglePoints);
        Point tip = trianglePoints.get(2);

        // Triangle interior?
        if (!this.inheritance.open) {
            g.setColor(inheritFillColor);
            g.fillPolygon(trianglePolygon);
        }

        // Triangle outline.
        g.setColor(inheritLineColor);
        g.setStroke(new BasicStroke(
            inheritLineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g.drawPolygon(trianglePolygon);

        // Preferred dimension for line leaving parent, if we must turn
        // a corner, is opposite that of the triangle orientation so
        // that it arrives at the tip.
        HorizOrVert preferredHV =
            this.getOrientation().x == 0? HorizOrVert.HV_HORIZ : HorizOrVert.HV_VERT;

        // Determine the point of emergence from parent such that we are
        // set up to hit the tip of the triangle.
        ArrayList<Point> linePoints = new ArrayList<Point>();
        RelationEndpoint parent = new RelationEndpoint(this.inheritance.parent);
        linePoints.add(RelationController.manhattan_getEndpointEmergence(
            parent, preferredHV, tip));

        // Now hit the tip.
        RelationController.manhattan_hitNextControlPoint(linePoints, preferredHV, tip);

        // Draw the line segment or segments.
        for (int i=1; i < linePoints.size(); i++) {
            Point start = linePoints.get(i-1);
            Point end = linePoints.get(i);
            g.drawLine(start.x, start.y, end.x, end.y);
        }
    }

    @Override
    public void mousePressed(MouseEvent ev)
    {
        // No useful right-click menu right now, so not calling
        // super.mousePressed.

        this.mouseSelect(ev, true /*wantDrag*/);
    }

    @Override
    public void dragTo(Point pt)
    {
        this.inheritance.pt = pt;
    }

    @Override
    public void deleteSelfAndData(Diagram diagram)
    {
        // Make sure no resize handles are around.
        this.setSelected(SelectionState.SS_UNSELECTED);

        this.selfCheck();

        // Delete any relations that involve this inheritance.
        final Inheritance thisInh = this.inheritance;
        this.diagramController.deleteControllers(new ControllerFilter() {
            public boolean satisfies(Controller c) {
                return (c instanceof RelationController) &&
                       ((RelationController)c).relation.involvesInheritance(thisInh);
            }
        });

        // Now delete the inheritance.
        diagram.inheritances.remove(thisInh);

        // And the controller.
        this.diagramController.remove(this);
    }

    @Override
    public void setSelected(SelectionState ss)
    {
        this.selfCheck();
        super.setSelected(ss);
    }

    @Override
    public void globalSelfCheck(Diagram d)
    {
        super.globalSelfCheck(d);

        this.inheritance.globalSelfCheck(d);
    }

    @Override
    public boolean keyPressed(KeyEvent ev)
    {
        String commandDesc;

        if (SwingUtil.noModifiers(ev)) {
            switch (ev.getKeyCode()) {
                case KeyEvent.VK_O:
                    this.inheritance.open = !this.inheritance.open;
                    commandDesc = localize(this.inheritance.open?
                        "Change inheritance style to Open" :
                        "Change inheritance style to Closed");
                    break;

                default:
                    return false;
            }

            this.diagramController.diagramChanged(commandDesc);
            return true;
        }

        return false;
    }
}

// EOF
