// GeomUtil.java

package util;

import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

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
        
        // Sanity check.
        assert(nonzero2DVector(v));
        assert(nonzero2DVector(w));

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
}

// EOF
