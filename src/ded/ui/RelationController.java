// RelationController.java

package ded.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.font.LineMetrics;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import javax.swing.SwingUtilities;

import util.GeomUtil;
import util.HorizOrVert;
import util.IntRange;
import util.SwingUtil;
import util.Util;

import ded.model.Diagram;
import ded.model.Entity;
import ded.model.Relation;
import ded.model.RelationEndpoint;
import ded.model.RoutingAlgorithm;

/** Provides UI for manipulating a Relation. */
public class RelationController extends Controller {
    // ---------------------- constants -------------------------
    public static final Color arrowFillColor = Color.BLACK;
    
    public static final int selfRelationRadius = 20;
    public static final int arrowHeadLength = 10;
    public static final int relationBoundsSlop = 10;
    public static final int relationLabelOffset = 3;
    
    // -------------------- instance data -----------------------
    /** The Relation we are controlling. */
    public Relation relation;
    
    /** If 'selState' is SS_EXCLUSIVE, these are handles to the endpoints.
      * Otherwise, they are null. */ 
    public RelationEndpointController startHandle, endHandle;
    
    /** If 'selState' is SS_EXCLUSIVE, this contains controllers for
      * all of the interior control points.  Otherwise it is null. */
    public RelationControlPointController[] controlPointHandle;
    
    // ----------------------- methods -------------------------
    public RelationController(DiagramController dc, Relation r)
    {
        super(dc);
        this.relation = r;
        this.startHandle = this.endHandle = null;
        this.controlPointHandle = null;
        
        this.selfCheck();
    }

    /** Get one of the four line segments on the border of 'entity':
      *             0
      *     +---------------+
      *     |               |
      *    1|               |3
      *     |               |
      *     +---------------+
      *             2                    */
    private static Line2D.Double getEntitySegment(Entity entity, int which)
    {
        assert(0 <= which && which < 4);

        // Grab the opposite corners initially.
        Rectangle r = entity.getRect();
        double sx = r.x;
        double sy = r.y;
        double ex = r.x + r.width;
        double ey = r.y + r.height;
        
        // Move one of the points to an adjacent point so that the
        // two points are the desired line segment.
        switch (which) {
            case 0: ey -= r.height; break;
            case 1: ex -= r.width; break;
            case 2: sy += r.height; break;
            case 3: sx += r.width; break;
        }
        
        return new Line2D.Double(sx, sy, ex, ey);
    }

    /** Try to find a point where 'segment' intersects with the edge of
      * 'entity'.  Normally, 'segment' is expected to have one endpoint
      * inside the bounds of 'entity' (although I'm not sure if that is
      * guaranteed), so there should be at most one intersection point.
      * If one is found, return it.
      * 
      * If no intersection is found, look for a point on the edge that
      * intersects the infinite line through 'segment' and is "near"
      * the segment (using a somewhat unusual definition of proximity;
      * see the code).
      * 
      * If no such intersection can be found, meaning that the segment
      * is degenerate, or somehow almost parallel to all of the edges,
      * return null.
      * 
      * The purpose of this code is to decide where on the edge of
      * 'entity' to begin drawing a relation that connects to it, where
      * 'segment' is what we would draw if we just let it go inside
      * the entity (which would be ugly). */
    private static Point2D.Double intersectEntityWithSegment(
        Entity entity,
        Line2D.Double segment)
    {
        // Best intersection distance so far.
        double bestDistance = 1000.0;
        
        // Point where the best intersection is.
        Point2D.Double bestIntersection = null;
        
        // Try intersecting the line segment with each segment of the
        // entity edge.
        for (int i=0; i<4; i++) {
            // Line segment on border of entity.
            Line2D.Double entitySeg = getEntitySegment(entity, i);
            
            // Intersect with the given segments, yielding parameters
            // that indicate how far along each segment to go to reach
            // the intersection point.
            double t = GeomUtil.intersectLine2Ds(segment, entitySeg);
            double t2 = GeomUtil.intersectLine2Ds(entitySeg, segment);
            if (Util.isSpecialDouble(t) || Util.isSpecialDouble(t2)) {
                continue;
            }
            
            // Compute "distance" to perfect intersection: the amount
            // that 't' or 't2' is beyond the range [0,1], using the
            // larger distance if both are outside that range.  It is
            // zero if both are in [0,1], meaning the segments truly
            // intersect.
            double distance = 0.0;
            if (t < 0.0) {
                distance = Math.max(distance, 0.0 - t);
            }
            if (t > 1.0) {
                distance = Math.max(distance, t - 1.0);
            }
            if (t2 < 0.0) {
                distance = Math.max(distance, 0.0 - t2);
            }
            if (t2 > 1.0) {
                distance = Math.max(distance, t2 - 1.0);
            }

            // True intersection?
            if (distance == 0.0) {
                return GeomUtil.pointOnLine2D(entitySeg, t);
            }
            
            // Best so far?
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIntersection = GeomUtil.pointOnLine2D(entitySeg, t);
            }
        }
        
        // We might not have a true intersection, but we'll use it anyway.
        // Non-intersections seem to happen fairly frequently when the
        // intersection point is near a corner of the entity rectangle.
        return bestIntersection;
    }

    @Override
    public Point getLoc()
    {
        // Midway between endpoints.
        return SwingUtil.midPoint(this.relation.start.getCenter(),
                                  this.relation.end.getCenter());
    }
    
    @Override
    public void dragTo(Point pt)
    {
        // Convert to a delta relative to 'getLoc()'.
        Point delta = SwingUtil.subtract(pt, this.getLoc());
        
        // Apply the delta to the endpoints if they are specified as
        // points rather than Entities or Inheritances.
        if (this.relation.start.isPoint()) {
            this.relation.start.pt = SwingUtil.add(this.relation.start.pt, delta);
        }
        if (this.relation.end.isPoint()) {
            this.relation.end.pt = SwingUtil.add(this.relation.end.pt, delta);
        }
        
        // Apply the delta to all control points.
        for (int i=0; i < this.relation.controlPts.size(); i++) {
            Point cp = this.relation.controlPts.get(i);
            this.relation.controlPts.set(i, SwingUtil.add(cp, delta));
        }
    }
    
    @Override
    public Set<Polygon> getBounds()
    {
        ArrayList<Point> points = this.computePoints();
        
        if (points.size() == 1) {
            return getSelfLoopBounds(points.get(0));
        }
        
        // Enclose each segment with a rectangle.
        HashSet<Polygon> bounds = new HashSet<Polygon>();
        for (int i=1; i < points.size(); i++) {
            bounds.add(getSegmentBounds(points.get(i-1), points.get(i)));
        }
        return bounds;
    }

    /** Compute the sequence of points that determine the line segments
      * we will draw to represent the relation.  The start and end points 
      * are adjusted to not go inside the Entity or Inheritance they
      * connect to, if they do connect.  This does not include the points
      * that make up the arrowhead; on an end with an arrowhead, this
      * just has the single point that will be the tip of the arrowhead. */
    private ArrayList<Point> computePoints()
    { 
        switch (this.relation.routingAlg) {
            case RA_DIRECT:          return this.direct_computePoints();
            case RA_MANHATTAN_HORIZ:
            case RA_MANHATTAN_VERT:  return this.manhattan_computePoints();
        }
        assert(false);
        return null;         // Not reached.
    }

    /** Draw an arrowhead and a line segment from 'start' to 'end'.  The
      * size of the arrowhead is fixed; the distance from 'start' to
      * 'end' is irrelevant.
      * 
      * If 'owning' is true, draw it as a double arrowhead; otherwise
      * draw a single arrowhead. */
    private static void drawArrow(Graphics g0, Point start, Point end, boolean owning)
    {
        // Copy the Graphics object so settings changes are not persistent.
        Graphics g = g0.create();
        
        // Draw the line segment.
        g.drawLine(start.x, start.y, end.x, end.y);
        
        // Then the arrowhead.  First, calculate the main arrow body vector
        // with the origin at 'end', pointing towards 'start'.
        Point2D.Double body = 
            new Point2D.Double(start.x-end.x, start.y-end.y);
        
        // Scale it to 'arrowHeadLength' pixels.
        body = GeomUtil.scale2DVectorTo(body, arrowHeadLength);
        
        // Make versions rotated up and down by 30 degrees, this
        // creating a separation of 60 degrees, which will make the
        // triangle equilateral.
        Point2D.Double up = GeomUtil.rot2DVectorAngle(body, Math.PI / 6.0);
        Point2D.Double down = GeomUtil.rot2DVectorAngle(body, - Math.PI / 6.0);

        // TODO: Right now, the drawn arrowheads look bad.  I need to
        // somehow tweak the drawing routine to fix them.  My current
        // best idea is to use Graphics2D doubles so I can make very
        // slight adjustments to the shape.
        
        //                                       ^           .
        //                                        \ up       .
        //                                         \         .
        //                                     body \        .
        //   start *                         <-------* end   .
        //                                          /        .
        //                                         /         .
        //                                        /down      .
        //                                       V           .

        if (!owning) {
            // filled arrowhead:
            //                                       X           .
            //                                       XX          .
            //                                       XxX         .
            //                                       XXXX        .
            //   start *                             XXXXX end   .
            //                                       XXXX        .
            //                                       XxX         .
            //                                       XX          .
            //                                       X           .

            // Vertices of the arrowhead that are not on the main line.
            Point upPoint = SwingUtil.add(end, GeomUtil.toPoint(up));
            Point downPoint = SwingUtil.add(end, GeomUtil.toPoint(down));
            
            // Draw a filled polygon connecting the end and the two
            // rotated points.
            g.setColor(arrowFillColor);
            Polygon pts = new Polygon();
            pts.addPoint(end.x, end.y);
            pts.addPoint(upPoint.x, upPoint.y);
            pts.addPoint(downPoint.x, downPoint.y);
            g.fillPolygon(pts);
        }
        
        else {
            // double unfilled arrowhead:
            //                                 X     X           .
            //                                  X     X          .
            //                                   X     X         .
            //                                    X     X        .
            //   start *                           X     X end   .
            //                                    X     X        .
            //                                   X     X         .
            //                                  X     X          .
            //                                 X     X           .

            // Arrowhead nearest 'end'.
            Point upPoint = SwingUtil.add(end, GeomUtil.toPoint(up));
            Point downPoint = SwingUtil.add(end, GeomUtil.toPoint(down));
            g.drawLine(end.x, end.y, upPoint.x, upPoint.y);
            g.drawLine(end.x, end.y, downPoint.x, downPoint.y);
            
            // Second arrowhead.
            //
            // Scale factor 2/3 chosen by trial and error based on
            // aesthestics of the painted result.
            Point end2 = new Point(end.x + (int)(body.x * 2/3),
                                   end.y + (int)(body.y * 2/3));
            upPoint = SwingUtil.add(end2, GeomUtil.toPoint(up));
            downPoint = SwingUtil.add(end2, GeomUtil.toPoint(down));
            g.drawLine(end2.x, end2.y, upPoint.x, upPoint.y);
        }
    }

    @Override
    public void paint(Graphics g0)
    {
        super.paint(g0);
        Graphics2D g = (Graphics2D)g0.create();
        
        ArrayList<Point> points = computePoints();
        
        if (points.size() == 1) {
            this.paintSelfLoop(g, points.get(0));
            return;
        }
        
        // Special lines for inheritacne.
        if (this.relation.end.isInheritance()) {
            g.setStroke(new BasicStroke(InheritanceController.inheritLineWidth));
            g.setColor(InheritanceController.inheritLineColor);
        }
        
        // Draw each segment.
        for (int i=1; i < points.size(); i++) {
            Point a = points.get(i-1);
            Point b = points.get(i);
            
            if (i == points.size()-1 && !this.relation.end.isInheritance()) {
                // Draw last segment as an arrow.
                drawArrow(g, a, b, this.relation.owning);
            }
            else {
                // Not the last segment, or goes to an inheritance.
                g.drawLine(a.x, a.y, b.x, b.y);
            }
        }
        
        // Label near midpoint of first segment.
        this.drawLabelAtSegment(g, points.get(0), points.get(1), this.relation.label);
    }
    
    /** Return the angle of a line that goes from the center of an
      * ellipse with horizontal radius 'a' and vertical radius 'b'
      * to a point on it that is tangent to a line that is parallel
      * to the ray from the origin to point 'pq'.
      * 
      * I struggled a bit to figure out how to do this, but then found
      * this site, which provides the essence of the solution:
      * 
      * http://mathforum.org/library/drmath/view/55356.html
      * 
      * I think the way I did this is more complicated than it needs
      * to be since I could bypass the angle stuff, but for now I am
      * just going to port it to Java as-is. */
    private static double ellipseTangentAngle(double a, double b, Point pq)
    {
        // Slope of the line from the origin to 'pq'.
        assert(pq.x != 0);
        double slopePQ = (double)pq.y / (double)pq.x;
        
        // Scale the slope by the same amount that would be required to
        // scale the ellipse vertically to become a circle.
        assert(b != 0.0);
        double scaledSlopePQ = slopePQ * a / b;
        
        // Convert that slope to an angle (radians) from the x-axis.
        double scaledAnglePQ = Math.atan(scaledSlopePQ);
        
        // Subtract 90 degrees, yield the angle (theta) from the origin
        // to the tangent point on the circle.
        double scaledAngleTheta = scaledAnglePQ - (Math.PI / 2.0);
        
        // Convert that to a slope.
        double scaledSlopeTheta = Math.tan(scaledAngleTheta);
        
        // Scale that vertically, reversing the scaling that made the
        // ellipse a circle.
        double slopeTheta = scaledSlopeTheta * b / a;
        
        // Finally, convert that back to an angle.
        double theta = Math.atan(slopeTheta);
        
        // 'atan' always returns an angle in quadrant 1 or 4, but if the
        // original PQ angle was in Q3 or 4, then theta should be in Q2
        // or 3 (because we subtract 90 degrees).
        if (pq.y < 0) {
            theta += Math.PI;
        }
        else if (pq.y == 0 && pq.x < 0) {
            theta += Math.PI;      // Strange boundary case, don't quite understand.
        }
        
        return theta;
    }

    /** Compute the intersection with an ellipse with center at the origin,
      * horizontal radius 'a' and vertical radius 'b', and a ray that
      * starts at the origin and extends out with angle 'theta' from the
      * x-axis. */
    private Point2D.Double computeEllipsePointFromAngle(
        double a,
        double b,
        double theta)
    {
        return new Point2D.Double(a * Math.cos(theta), b * Math.sin(theta));
    }

    /** Draw 'label' near the midpoint of 'p' and 'q'.
      * 
      * What I want:
      *   - Compute a bounding rectangle for 'label'.
      *   - Inscribe an ellipse.
      *   - Position the ellipse such that:
      *     - The ellipse is to the left of PQ as you look from P.
      *     - The closest point to the ellipse on the line PQ is
      *       PQ's midpoint.
      *     - The closest point to the line on the ellipse is
      *       'relationLabelOffset' pixels from the midpoint.
      *       
      * Though this is a fair bit of work to accomplish, I coudn't think
      * of any quick hacks that seemed likely to be good enough.
      * 
      * This code was difficult to get right!  */
    private void drawLabelAtSegment(
        Graphics g,
        Point origP,
        Point origQ,
        String label)
    {
        Point p = new Point(origP);
        Point q = new Point(origQ);
        
        // Get the bounding rectangle dimensions.
        FontMetrics fm = g.getFontMetrics();
        LineMetrics lm = fm.getLineMetrics(label, g);
        int labelWidth = fm.stringWidth(label);
        
        // Compute dimensions of an inscribed ellipse.
        double a = (double)labelWidth / 2;                           // horizontal radius
        double b = (double)(lm.getAscent() + lm.getDescent()) / 2;   // vertical radius
        if (a <= 0 || b <= 0) {
            return;                    // Degenerate, bail.
        }
        
        // Midpoint of PQ.
        Point m = SwingUtil.midPoint(p, q);
        
        // Vertical segment?
        if (p.x == q.x) {
            // Must handle this specially because the 'ellipseTangent'
            // algorithm divides by the 'x' difference.
            if (p.y < q.y) {
                // Put label to the right of the segment.
                m.x += relationLabelOffset + (int)a;
            }
            else {
                // Put label to the left of the segment.
                m.x -= relationLabelOffset + (int)a;
            }
            SwingUtil.drawCenteredText(g, m, label);
        }
        
        // I'm having a hard time working out the following code in the
        // inverted coord system, so just flip the y coords of the inputs
        // so I can work in the usual cartesian coord system.
        p.y = -p.y;
        q.y = -q.y;
        
        // Compute an angle that points from the ellipse center to the
        // point on its edge tangent to PQ.
        double theta = ellipseTangentAngle(a, b, SwingUtil.subtract(q, p));
        
        // Compute the vector from the ellipse center to the tangent point.
        Point2D.Double edge = computeEllipsePointFromAngle(a, b, theta);
        
        // Vector PQ.
        Point2D.Double v = new Point2D.Double(q.x-p.x, q.y-p.y);
        
        // Rotate 90, normalize length to 'relationLabelOffset'.
        v = GeomUtil.rot2DVector90(v);
        v = GeomUtil.scale2DVectorTo(v, relationLabelOffset);
        
        // Compute destired center of ellipse.
        Point2D.Double center = GeomUtil.toPoint2D_Double(m);
        center = GeomUtil.add(center, v);
        center = GeomUtil.add(center, edge);
        
        // Return to usual AWT coordinate system.
        Point printSpot = GeomUtil.toPoint(center);
        printSpot.y = -printSpot.y;
        
        // Draw the label.
        SwingUtil.drawCenteredText(g, printSpot, label);
    }
    
    @Override
    public void selfCheck()
    {
        super.selfCheck();
        
        // Handles exist iff SS_EXCLUSIVE.
        if (this.selState != SelectionState.SS_EXCLUSIVE) {
            assert(this.startHandle == null);
            assert(this.endHandle == null);
            assert(this.controlPointHandle == null);
        }
        else {
            assert(this.startHandle != null);
            assert(this.endHandle != null);
            assert(this.controlPointHandle != null);
            
            assert(this.diagramController.contains(this.startHandle));
            assert(this.diagramController.contains(this.endHandle));
            
            int npts = this.relation.controlPts.size();
            for (int i=0; i<npts; i++) {
                assert(this.diagramController.contains(this.controlPointHandle[i]));
            }
        }
    }

    @Override
    public void setSelected(SelectionState ss)
    {
        this.selfCheck();

        // If transition away from exclusive, destroy handles.
        if (this.selState == SelectionState.SS_EXCLUSIVE && 
            ss != SelectionState.SS_EXCLUSIVE) 
        {
            this.diagramController.remove(this.startHandle);
            this.diagramController.remove(this.endHandle);
            this.startHandle = null;
            this.endHandle = null;
            
            int npts = this.relation.controlPts.size();
            for (int i=0; i<npts; i++) {
                this.diagramController.remove(this.controlPointHandle[i]);
            }
            this.controlPointHandle = null;
        }
        
        // Transition to exclusive: create handles.
        if (this.selState != SelectionState.SS_EXCLUSIVE &&
            ss == SelectionState.SS_EXCLUSIVE)
        {
            this.startHandle = new RelationEndpointController(
                this.diagramController, this, this.relation.start);
            this.endHandle = new RelationEndpointController(
                this.diagramController, this, this.relation.end);
            this.diagramController.add(this.startHandle);
            this.diagramController.add(this.endHandle);
            
            int npts = this.relation.controlPts.size();
            this.controlPointHandle = new RelationControlPointController[npts];
            for (int i=0; i<npts; i++) {
                this.diagramController.add(this.controlPointHandle[i] =
                    new RelationControlPointController(this.diagramController, this, i));
            }
        }
        
        super.setSelected(ss);
        
        this.selfCheck();
    }

    /** Get the controller for the end handle. */
    public RelationEndpointController getEndHandle()
    {
        this.selfCheck();
        assert(this.endHandle != null);
        return this.endHandle;
    }
    
    @Override
    public void edit()
    {
        if (RelationDialog.exec(this.diagramController, this.relation)) {
            // User pressed OK.
            this.diagramController.repaint();
        }
    }

    /** Remove this relation and its controller. */
    public void deleteSelfAndData(Diagram diagram)
    {
        this.selfCheck();
        
        // Unselect myself to get rid of handles.
        this.setSelected(SelectionState.SS_UNSELECTED);
        
        // Remove the relation from the diagram.
        diagram.relations.remove(this.relation);
        
        // Remove myself as a controller.
        this.diagramController.remove(this);
    }

    /** Insert a new control point. */
    private void insertControlPoint()
    {
        // Temporarily become unselected so handles will be rebuilt
        // if needed.
        SelectionState oldSel = this.getSelState();
        this.setSelected(SelectionState.SS_UNSELECTED);
        
        this.relation.controlPts.add(0, new Point(
            SwingUtil.midPoint(this.relation.start.getCenter(),
                               this.relation.end.getCenter())));
        
        this.setSelected(oldSel);
        
        this.diagramController.repaint();
    }

    /** Delete a control point.  This will delete and re-create
      * the point controllers if necessary. */
    public void deleteControlPoint(int which)
    {
        // As above, temporarily unselect.
        SelectionState oldSel = this.getSelState();
        this.setSelected(SelectionState.SS_UNSELECTED);
        
        this.relation.controlPts.remove(which);
        
        this.setSelected(oldSel);
        
        this.diagramController.repaint();
    }
    
    @Override
    public void mousePressed(MouseEvent ev)
    {
        if (SwingUtilities.isRightMouseButton(ev)) {
            // Context menu choices.
            String items[] = { "&Insert control point" };
            
            // Launch menu.
            switch (this.diagramController.popupMenu(ev.getPoint(), "Relation", items)) {
                case 0:
                    this.insertControlPoint();
                    break;
            }
            
            return;
        }
        
        this.mouseSelect(ev, false /*wantDrag*/);
    }
    
    @Override
    public void keyPressed(KeyEvent ev)
    {
        if (SwingUtil.noModifiers(ev)) {
            switch (ev.getKeyCode()) {
                case KeyEvent.VK_D:
                    this.relation.routingAlg = RoutingAlgorithm.RA_DIRECT;
                    break;
                    
                case KeyEvent.VK_H:
                    this.relation.routingAlg = RoutingAlgorithm.RA_MANHATTAN_HORIZ;
                    break;
                    
                case KeyEvent.VK_V:
                    this.relation.routingAlg = RoutingAlgorithm.RA_MANHATTAN_VERT;
                    break;
                    
                case KeyEvent.VK_O:
                    this.relation.owning = !this.relation.owning;
                    break;
                    
                default:
                    return;
            }
            
            this.diagramController.repaint();
        }
    }
    
    
    /** Get bounding rectangle, not necessarily horizontal or vertical,
      * for a segment from 'p1' to 'p2'.  This is the smallest rectangle
      * that encloses all points 'relationBoundsSlop' pixels or less
      * from the line segment. */
    private Polygon getSegmentBounds(Point p1, Point p2)
    {
        // Convert to Point2D.Double.
        return getSegmentBounds(GeomUtil.toPoint2D_Double(p1),
                                GeomUtil.toPoint2D_Double(p2));
    }
    
    /** See the other overload for spec. */
    private Polygon getSegmentBounds(Point2D.Double startPt, Point2D.Double endPt)
    {
        // Region's distance from line segment.
        int slop = relationBoundsSlop;
        
        // Get a vector that is paralle to the segment but has length 'slop'.
        Point2D.Double slopVector = GeomUtil.subtract(endPt, startPt);
        slopVector = GeomUtil.scale2DVectorTo(slopVector, slop);
        
        // Rotate that 90 degrees clockwise (since we're using AWT coords).
        Point2D.Double slopV90 = GeomUtil.rot2DVector90(slopVector);
        
        // Compute points on a rectangle that is +/0 slop all around.
        Polygon bounds = new Polygon();
        for (int i=0; i<4; i++) {
            // Start by going off the long way.
            Point2D.Double p;
            if (i==0 || i==3) {
                p = GeomUtil.subtract(startPt, slopVector);     // p = start - slop
            }
            else {
                p = GeomUtil.add(endPt, slopVector);            // p = end + slop
            }
            
            // Then the short way.
            if (i <= 1) {
                p = GeomUtil.subtract(p, slopV90);              // p -= slop90
            }
            else {
                p = GeomUtil.add(p, slopV90);                   // p += slop90
            }
            
            bounds.addPoint((int)p.x, (int)p.y);
        }
        
        return bounds;
    }

    /** Get a bounding polygon for a self-loop relation. */
    private HashSet<Polygon> getSelfLoopBounds(Point pt)
    {
        // Square that encloses the circle.
        int r = selfRelationRadius + arrowHeadLength/2;
        HashSet<Polygon> ret = new HashSet<Polygon>();
        ret.add(SwingUtil.rectPolygon(pt.x-r, pt.y-r, r*2, r*2));
        return ret;
    }

    /** Return a singleton list containing the start relation's center.
      * This is the sequence of points to connect for a self-loop. */
    private ArrayList<Point> selfLoop_computePoints()
    {
        ArrayList<Point> points = new ArrayList<Point>();
        points.add(this.relation.start.getCenter());
        return points;
    }

    /** Paint a self-loop relation.
      *
      * Currently, this draws a really dumb-looking arrow, so this only
      * happens temporarily, by mistake, in diagram editing.  Eventually
      * I hope to write a better implementation that will be useful,
      * since self-relations are fairly common. */
    private void paintSelfLoop(Graphics g, Point pt)
    {
        // Draw a 315-degree circle starting at 45 degrees and ending
        // at 0 degrees, with radius 20, centered on 'start'.
        int radius = selfRelationRadius;
        g.drawArc(pt.x - radius, pt.y - radius,
                  radius*2, radius*2,
                  45, 315);
        
        // Put an arrowhead at the 0 degree position.
        drawArrow(g, SwingUtil.add(pt, new Point(radius, 0)),
                     SwingUtil.add(pt, new Point(radius, -1)),
                     this.relation.owning);
                  
        // Label above the circle.
        int arrowLabelOffset = 20;
        Point labelPt = SwingUtil.add(pt, new Point(0, -radius - arrowLabelOffset));
        SwingUtil.drawCenteredText(g, labelPt, this.relation.label);
    }
    
    /** Get location of first control point, or the center of the
      * relation end if there are no control points. */ 
    private Point firstControlPoint()
    {
        if (this.relation.controlPts.isEmpty()) {
            // From the point of view of the start entity, the center of
            // the end entity is the first control point.
            return this.relation.end.getCenter();
        }
        else {
            return this.relation.controlPts.get(0);
        }
    }
    
    /** Get location of last control point or relation start. */
    private Point lastControlPoint()
    {
        if (this.relation.controlPts.isEmpty()) {
            // From the point of view of the end entity, the center of
            // the start entity is the last control point.
            return this.relation.start.getCenter();
        }
        else {
            return this.relation.controlPts.get(this.relation.controlPts.size()-1);
        }
    }

    // ----------------- RelationController: RA_DIRECT -----------------
    /** Find the point on the edge of 'entity' that intersects the line
      * segment from the center of 'entity' to 'p'.  If there is none,
      * return null. */
    private static Point entityEdgeIntersection(Entity entity, Point p)
    {
        // Construct the line segment.
        Line2D.Double line = new Line2D.Double(
            GeomUtil.toPoint2D_Double(entity.getCenter()),
            GeomUtil.toPoint2D_Double(
                SwingUtil.subtract(p, entity.getCenter())));
        
        // Intersect this segment with the entity borders.
        Point2D.Double ret = intersectEntityWithSegment(entity, line);
        if (ret == null) {
            return null;
        }
        else {
            return GeomUtil.toPoint(ret);
        }
    }
    
    /** Get the sequence of points to connect for a relation using the
      * direct-line algorithm. */
    private ArrayList<Point> direct_computePoints()
    {
        RelationEndpoint start = this.relation.start;
        RelationEndpoint end = this.relation.end;
        ArrayList<Point> cpts = this.relation.controlPts;
        
        ArrayList<Point> points = new ArrayList<Point>();
        
        // Start point.
        if (start.isEntity()) {
            // Find point on edge of 'start' that intersects the line
            // segment from the center of 'start' to the next point,
            // which is either the first control point or (if there
            // are none) the center of 'end'.
            Point next = this.firstControlPoint();
            Point intersection = entityEdgeIntersection(start.entity, next);
            if (intersection != null) {
                points.add(intersection);
            }
            else {
                return this.selfLoop_computePoints();
            }
        }
        else {
            points.add(start.getCenter());
        }
        
        // Interior control points.
        points.addAll(cpts);
        
        // End point.
        if (end.isEntity()) {
            // Similar to how first point is set.
            Point prev = this.lastControlPoint();
            Point intersection = entityEdgeIntersection(end.entity, prev);
            if (intersection != null) {
                points.add(intersection);
            }
            else {
                return this.selfLoop_computePoints();    // Degenerate?
            }
        }
        else {
            points.add(end.getCenter());
        }
        
        return points;
    }
    
    // ----------------- RelationController: RA_MANHATTAN_* ----------------
    /** Get the visible extent of 're' in dimension 'hv'. */
    private static IntRange getRange(HorizOrVert hv, RelationEndpoint re)
    {
        if (re.isEntity()) {
            return SwingUtil.getRectRange(hv, re.entity.getRect());
        }
        else {
            return IntRange.singleton(hv.get(re.getCenter()));
        }
    }
    
    /** If 'r1' and 'r2' overlap, return the midpoint of the overlapping
      * region.  Otherwise, return null. */ 
    private static Integer overlaps(IntRange r1, IntRange r2)
    {
        // Get the overlapping region (if exists).
        int low = Math.max(r1.low, r2.low);
        int high = Math.min(r1.high, r2.high);
        
        if (low <= high) {
            return Integer.valueOf(Util.avg(low, high));
        }
        else {
            return null;     // No overlap.
        }
    }
    
    /** Extend 'points' to go through 'target', heading along 'currentHV'
      * first when a corner must be turned.  Return the new value to use
      * in place of 'currentHV' by the caller. */
    private static HorizOrVert manhattan_hitNextControlPoint(
        ArrayList<Point> /*INOUT*/ points,
        HorizOrVert currentHV,
        Point target)
    {
        // Where does the chain end?
        int nPoints = points.size();
        Point source = points.get(nPoints-1);
        
        // Can hit w/o turning corner?
        for (HorizOrVert hv : EnumSet.allOf(HorizOrVert.class)) {
            if (hv.get(source) == hv.get(target)) {
                // Can hit with a single line along 'hv'; just append 'target'.
                points.add(target);
                return hv;             // Keep going this way next time.
            }
        }
        
        // Must turn a corner.
        
        // Call the dimension to proceed along first 'x'.
        HorizOrVert x = currentHV;
        HorizOrVert y = x.opposite();
        
        Point corner = new Point();
        x.set(corner, x.get(target));
        y.set(corner, y.get(source));
        points.add(corner);
        
        points.add(target);
        
        // Final edge was along 'y'.
        return y;
    }

    /** Return a point along the edge of 'start' suitable for connecting
      * via manhattan lines to 'target'.  If this can be done with a single
      * straight line, return a point that is in the proper line.  When
      * there is a choice, prefer an exit path that will go along
      * 'preferredHV' first. */
    private static Point manhattan_getEndpointEmergence(
        RelationEndpoint start,
        HorizOrVert preferredHV,
        Point target)
    {
        // Try for horiz/vert first.
        for (HorizOrVert hvIter : HorizOrVert.allValues()) {
            // modularAdd: possibly toggle which dimension is considered first
            //
            // In the comments and names that follow, I will pretend that 'x' is
            // D_HORIZ, but of course the point of abstracting the dimension is
            // that the code works when 'x' is D_VERT too.
            HorizOrVert x = hvIter.plus(preferredHV);
            HorizOrVert y = x.opposite();
            
            // Can hit with singe line along 'x'?
            if (getRange(y, start).contains(y.get(target))) {
                // Yes, use it.
                return manhattan_getEndpointEmergenceDim(start, x, target);
            }
        }
        
        // Must turn a corner, go along 'preferredHV' first.
        return manhattan_getEndpointEmergenceDim(start, preferredHV, target);
    }

    /** Similar to above, but forced to use exit path along 'x'. */
    private static Point manhattan_getEndpointEmergenceDim(
        RelationEndpoint start,
        HorizOrVert x,
        Point target)
    {
        HorizOrVert y = x.opposite();
        
        Point ret = new Point();
        
        // With what x coordwill the line emerge from 'start'?
        if (x.get(start.getCenter()) < x.get(target)) {
            x.set(ret, getRange(x, start).high);           // Right side.
        }
        else {
            x.set(ret, getRange(x, start).low);            // Left side.
        }
        
        // And the y coord?
        if (getRange(y, start).contains(y.get(target))) {
            // Use target's y coord.
            y.set(ret, y.get(target));
        }
        else {
            // Use center of start's range.
            y.set(ret, getRange(y, start).midPoint());
        }
        
        return ret;
    }

    /** Get the sequence of points to connect to draw a relation that
      * is drawn using either Manhattan algorithm. */
    private ArrayList<Point> manhattan_computePoints()
    {
        RelationEndpoint start = this.relation.start;
        RelationEndpoint end = this.relation.end;
        ArrayList<Point> cpts = this.relation.controlPts;
        Point startCenter = start.getCenter();
        Point endCenter = end.getCenter();
        
        // Return value.
        ArrayList<Point> points = new ArrayList<Point>();
        
        // Preferred starting dimension?
        HorizOrVert preferredHV =
            this.relation.routingAlg == RoutingAlgorithm.RA_MANHATTAN_HORIZ?
                HorizOrVert.HV_HORIZ : HorizOrVert.HV_VERT;
        
        if (cpts.isEmpty()) {
            // Degenerate?
            if (startCenter.equals(endCenter)) {
                points.add(startCenter);
                return points;
            }
            
            // Overlap in horiz/vert?
            for (HorizOrVert hvIter : HorizOrVert.allValues()) {
                // Like above.
                HorizOrVert x = hvIter.plus(preferredHV);
                HorizOrVert y = x.opposite();
                
                IntRange startYRange = getRange(y, start);
                IntRange endYRange = getRange(y, end);
                Integer midpt = overlaps(startYRange, endYRange);
                if (midpt != null) {
                    IntRange startXRange = getRange(x, start);
                    IntRange endXRange = getRange(x, end);
                    
                    Point p1 = new Point();
                    Point p2 = new Point();
                    
                    // Low to high?
                    if (x.get(startCenter) < x.get(endCenter)) {
                        x.set(p1, startXRange.high);
                        x.set(p2, endXRange.low);
                    }
                    else {
                        x.set(p1, startXRange.low);
                        x.set(p2, endXRange.high);
                    }
                    y.set(p1, midpt);
                    y.set(p2, midpt);
                    
                    points.add(p1);
                    points.add(p2);
                    return points;
                }
            }
        }
        
        // Determine the point of emergence from 'start' such that we
        // hit the first control point.
        points.add(manhattan_getEndpointEmergence
            (start, preferredHV, this.firstControlPoint()));
        
        // Hit successive control points.
        HorizOrVert currentHV = preferredHV;
        for (Point p : cpts) {
            currentHV = manhattan_hitNextControlPoint(points, currentHV, p);
        }
        
        // Determine the point of entrance to 'end', coming from
        // the last control point.
        Point finalPt = manhattan_getEndpointEmergence
            (end, currentHV.opposite(), this.lastControlPoint());
        
        // Hit this last point.
        manhattan_hitNextControlPoint(points, currentHV, finalPt);
        
        // Example output (no control points, start is D_HORIZ):
        //
        //    +---------+                        points[1]
        //    |         |                       /
        //    |  start  *----------------------*
        //    |         |\                     |
        //    +---------+ \                    |  points[2]
        //                 points[0]           | /
        //                                     V/
        //                                +----*-----+
        //                                |          |
        //                                |   end    |
        //                                |          |
        //                                +----------+
        
        return points;
    }
}

// EOF
