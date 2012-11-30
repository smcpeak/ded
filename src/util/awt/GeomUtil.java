// GeomUtil.java
// See toplevel license.txt for copyright and license terms.

package util.awt;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import util.IntRange;
import util.Util;

/** Utilities related to the java.awt.geom classes, particularly
  * various kinds of vector manipulations, where Point2D.Double
  * acts as the vector type. */
public class GeomUtil {
    /** Return 'a' - 'b'. */
    public static Point2D.Double subtract(Point2D.Double a, Point2D.Double b)
    {
        return new Point2D.Double(a.x - b.x, a.y - b.y);
    }

    /** Return the euclidean length of 'v'. */
    public static double length2DVector(Point2D.Double v)
    {
        return Math.sqrt(v.x * v.x + v.y * v.y);
    }

    /** Return the euclidean length of 'v'. */
    public static int lengthVector(Point v)
    {
        return (int)Math.sqrt(lengthVectorSquared(v));
    }

    /** Return the square of the length of 'v'. */
    public static double lengthVectorSquared(Point v)
    {
        return Math.sqrt((double)v.x * v.x + (double)v.y + v.y);
    }

    /** Return 'v' rotated 90 degrees counterclockwise in cartesian coordinates. */
    public static Point2D.Double rot2DVector90(Point2D.Double v)
    {
        return new Point2D.Double(-v.y, v.x);
    }

    /** Return 'v' rotated 'theta' radians counterclockwise in cartesian coordinates. */
    public static Point2D.Double rot2DVectorAngle(Point2D.Double v, double theta)
    {
        double cos_t = Math.cos(theta);
        double sin_t = Math.sin(theta);
        return new Point2D.Double(
            (v.x * cos_t) - (v.y * sin_t),
            (v.x * sin_t) + (v.y * cos_t));
    }

    /** Return 'v' scaled by 'factor'. */
    public static Point2D.Double scale2DVector(Point2D.Double v, double factor)
    {
        return new Point2D.Double(v.x * factor, v.y * factor);
    }

    /** Return 'v' scaled so its length is 'length'. */
    public static Point2D.Double scale2DVectorTo(Point2D.Double v, double length)
    {
        double origLen = length2DVector(v);
        return scale2DVector(v, length / origLen);
    }

    /** Convert 'p' to Point2D.Double. */
    public static Point2D.Double toPoint2D_Double(Point p)
    {
        return new Point2D.Double(p.x, p.y);
    }

    /** Return 'a' + 'b'. */
    public static Point2D.Double add(Point2D.Double a, Point2D.Double b)
    {
        return new Point2D.Double(a.x+b.x, a.y+b.y);
    }

    /** Convert 'p' to Point. */
    public static Point toPoint(Point2D.Double p)
    {
        return new Point((int)p.x, (int)p.y);
    }

    /** Return the start point of 'line'. */
    public static Point2D.Double getLineStart(Line2D.Double line)
    {
        return new Point2D.Double(line.x1, line.y1);
    }

    /** Return the end point of 'line'. */
    public static Point2D.Double getLineEnd(Line2D.Double line)
    {
        return new Point2D.Double(line.x2, line.y2);
    }

    /** Return the vector from the start of 'line' to its end. */
    public static Point2D.Double getLineVector(Line2D.Double line)
    {
        return new Point2D.Double(line.x2 - line.x1, line.y2 - line.y1);
    }

    /** Treat 'line' as parametric, where the start point is 0
      * and the end point is 1.  Return the point corresponding
      * to parameter value 't'. */
    public static Point2D.Double pointOnLine2D(Line2D.Double line, double t)
    {
        Point2D.Double vector = getLineVector(line);
        Point2D.Double scaledVector = scale2DVector(vector, t);
        return add(getLineStart(line), scaledVector);
    }

    /** Return true if at least one coordinate is nonzero. */
    public static boolean nonzero2DVector(Point2D.Double v)
    {
        return v.x != 0 || v.y != 0;
    }

    /** Given two lines, regarded as being in parametric form, return the
      * value 't' such that "qwline.origin + qwline.vector * t" is the
      * intersection point.  If the lines are parallel, returns NaN. */
    public static double intersectLine2Ds(Line2D.Double pvline, Line2D.Double qwline)
    {
        // Some convenient names.
        Point2D.Double p = getLineStart(pvline);
        Point2D.Double v = getLineVector(pvline);
        Point2D.Double q = getLineStart(qwline);
        Point2D.Double w = getLineVector(qwline);

        // If either segment is 0 length, bail.
        if (!( nonzero2DVector(v) && nonzero2DVector(w) )) {
            return Double.NaN;
        }

        // Solve for t, the multiplier considered to apply to 'qwline',
        // at the intersection point (this formula was worked out on paper).
        double t = ((p.x - q.x) + (v.x / v.y) * (q.y - p.y)) /
                // -----------------------------------------
                          (w.x - (v.x / v.y) * w.y) ;

        if (Util.isSpecialDouble(t)) {
            // The computation failed, either because 'v' is vertical, or
            // because the lines are parallel; so try to solve for 's'
            // instead, the multilpier considered to apply to 'pvline'
            // (same formula as above except p<->q and v<->w swapped).
            double s = ((q.x - p.x) + (w.x / w.y) * (p.y - q.y)) /
                    // -----------------------------------------
                            (v.x - (w.x / w.y) * v.y) ;

            if (Util.isSpecialDouble(s)) {
                // The lines must be parallel.
                return Double.NaN;
            }

            // The point given by "p + v*s" is the intersection point.
            // Use 's' to compute 't'.
            t = (p.x + (v.x * s) - q.x) / w.x;

            if (Util.isSpecialDouble(t)) {
                // Try the other formula, using 'y' components.
                t = (p.y + (v.y * s) - q.y) / w.y;

                if (Util.isSpecialDouble(t)) {
                    // I don't know what would cause this (other than
                    // malformation of v or w that nonzeroD2Vector
                    // doesn't cover, like NaN).
                    throw new RuntimeException(
                        "intersectLine2Ds: failed to compute t from s");
                }
            }
        }

        assert(!Util.isSpecialDouble(t));
        return t;
    }

    /** Return the range of integer values encompassed by 'rect'
      * in the 'hv' dimension. */
    public static IntRange getRectRange(HorizOrVert hv, Rectangle rect)
    {
        if (hv.isHoriz()) {
            // The -1 is because, when drawn, the number of pixels equals
            // 'width', not width+1.
            return new IntRange(rect.x, Math.max(rect.x, rect.x + rect.width - 1));
        }
        else {
            return new IntRange(rect.y, Math.max(rect.y, rect.y + rect.height - 1));
        }
    }

    /** Return a new rectangle that exceeds 'r' by 'd' pixels on all
      * sides, keeping the center the same.  * If 'd' is negative,
      * the new rectangle is smaller. */
    public static Rectangle growRectangle(Rectangle r, int d)
    {
        return new Rectangle(r.x - d,
                             r.y - d,
                             r.width + d*2,
                             r.height + d*2);
    }

    /** Return the midpoint of a line segment from 'p' to 'q'. */
    public static Point midPoint(Point p, Point q)
    {
        return new Point(Util.avg(p.x, q.x), Util.avg(p.y, q.y));
    }

    /** Get the center of 'r'. */
    public static Point getCenter(Rectangle r)
    {
        return new Point(r.x + r.width/2, r.y + r.height/2);
    }

    /** Return a new point whose coordinates are the nearest multiples of 'snap'. */
    public static Point snapPoint(Point p, int snap)
    {
        return new Point(GeomUtil.snapInt(p.x, snap), GeomUtil.snapInt(p.y, snap));
    }

    /** Return the nearest multiple of 'snap'. */
    public static int snapInt(int x, int snap)
    {
        x += snap/2;
        x -= x % snap;
        return x;
    }

    /** Return a Polygon for a Rectangle. */
    public static Polygon rectPolygon(Rectangle r)
    {
        return GeomUtil.rectPolygon(r.x, r.y, r.width, r.height);
    }

    /** Return a Polygon for the rectangle with UL at (x,y) and
      * 'width' and 'height'. */
    public static Polygon rectPolygon(int x, int y, int w, int h)
    {
        int[] px = new int[4];
        int[] py = new int[4];

        // Counterclockwise starting at upper left.
        px[0] = x;           py[0] = y;
        px[1] = x;           py[1] = y+h;
        px[2] = x+w;         py[2] = y+h;
        px[3] = x+w;         py[3] = y;

        return new Polygon(px, py, 4);
    }

    /** Return 'a' + 'b'. */
    public static Point add(Point a, Point b)
    {
        return new Point(a.x + b.x, a.y + b.y);
    }

    /** Return 'a' - 'b'. */
    public static Point subtract(Point a, Point b)
    {
        return new Point(a.x - b.x, a.y - b.y);
    }

    /** Return 'v' * 's'. */
    public static Point mult(Point v, int s)
    {
        return new Point(v.x * s, v.y * s);
    }

    /** Rotate 'p' 90 degrees clockwise in Cartesian coordinates. */
    public static Point row90cw(Point p)
    {
        return new Point(-p.y, p.x);
    }

    /** Rotate 'p' 90 degrees counterclockwise in Cartesian coordinates. */
    public static Point row90ccw(Point p)
    {
        return new Point(p.y, -p.x);
    }

    /** Return the smallest Rectangle that contains all in 'pts'. */
    public static Rectangle boundingBox(ArrayList<Point> pts)
    {
        assert(!pts.isEmpty());
        Rectangle r = new Rectangle(pts.get(0).x, pts.get(0).y, 1,1);

        for (Point p : pts) {
            if (p.x < r.x) {
                r.width += r.x - p.x;
                r.x = p.x;
            }
            if (p.y < r.y) {
                r.height += r.y - p.y;
                r.y = p.y;
            }
            if (p.x >= r.x + r.width) {
                r.width = p.x - r.x + 1;
            }
            if (p.y >= r.y + r.height) {
                r.height = p.y - r.y + 1;
            }
        }

        return r;
    }

    /** Create a polygon from a list of points. */
    public static Polygon makePolygon(List<Point> trianglePoints)
    {
        Polygon ret = new Polygon();
        for (Point p : trianglePoints) {
            ret.addPoint(p.x, p.y);
        }
        return ret;
    }

    /** Return the magnitude of the cross product of 'a' and 'a',
     * which is |a||b|sin(theta), theta being the positive angle
     * between 'a' to 'b'. */
   public static double crossProdZ2DVector(Point2D.Double a, Point2D.Double b)
   {
       return (a.x * b.y) - (a.y * b.x);
   }

   /** Return the dot product of 'a' and 'b',
     * which is |a||b|cos(theta), theta being the angle
     * between 'a' to 'b'. */
   public static double dotProd2DVector(Point2D.Double a, Point2D.Double b)
   {
       return (a.x * b.x) + (a.y * b.y);
   }

    /** Return the distance between 'point' and either the line through
      * 'lineSeg' or 'lineSeg' itself, depending on 'distToSeg'. */
    public static double distance2DPointLineOrSeg(
        Point2D.Double point,
        Line2D.Double lineSeg,
        boolean distToSeg)
    {
        // Let 'a' be the vector from the start of 'lineSeg' to 'pt'.
        Point2D.Double a = subtract(point, getLineStart(lineSeg));

        // Let 'b' be the vector from the start to end of 'lineSeg'.
        Point2D.Double b = getLineVector(lineSeg);

        if (distToSeg) {
            // Check to see if the closest point on the line is
            // between start and end.
            double dot = dotProd2DVector(a, b);      // |a||b|cos(theta)
            if (dot < 0) {
                // The start is the closest point.
                return length2DVector(a);
            }
            if (dot > dotProd2DVector(b, b)) {
                // The end is the closest point.
                return length2DVector(subtract(point, getLineEnd(lineSeg)));
            }

            // The line is closer than either endpoint, so get the
            // distance to it.
        }

        // This is "|a||b|sin(theta)", theta being the angle between a and b.
        double cross = crossProdZ2DVector(a, b);

        // This is "|a|sin(theta)".
        return Math.abs(cross / length2DVector(b));
    }

    /** Return the distance between 'point' and the line through 'lineSeg'. */
    public static double distance2DPointLine(
        Point2D.Double point,
        Line2D.Double lineSeg)
    {
        return distance2DPointLineOrSeg(point, lineSeg, false /*distToSeg*/);
    }

    /** Return the distance between 'point' and 'lineSeg'.  When 'point'
      * is far from the segment, the distance is usually the distance
      * to the closest endpoint.
      *
      * This differs from 'distance2DPointLine' in that we do not
      * consider the line through 'lineSeg', just the segment itself. */
    public static double distance2DPointLineSeg(
        Point2D.Double point,
        Line2D.Double lineSeg)
    {
        return distance2DPointLineOrSeg(point, lineSeg, true /*distToSeg*/);
    }
}

// EOF
