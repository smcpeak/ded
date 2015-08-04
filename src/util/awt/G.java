// G.java
// See toplevel license.txt for copyright and license terms.

package util.awt;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;

/** This is a collection of utility functions related to java.awt.Graphics
  * and its associated geometric object classes.  It is called "G" so that
  * uses in code are reasonably compact, since I can't add methods to the
  * classes I would modify if I could.
  *
  * This is sort of a poor man's operator overloading and class extension
  * combined. */
public class G {
    /** Return either x or y of 'r', depending on 'hv'. */
    public static int origin(Rectangle r, HorizOrVert hv)
    {
        return hv.isHoriz()? r.x : r.y;
    }

    /** Return either the width or height of 'r', depending on 'hv'. */
    public static int size(Rectangle r, HorizOrVert hv)
    {
        return hv.isHoriz()? r.width : r.height;
    }

    /** Return the upper-left corner of 'r' as a Point. */
    public static Point topLeft(Rectangle r)
    {
        return new Point(r.x, r.y);
    }

    /** Return the bottom-right corner of 'r' as a Point. */
    public static Point bottomRight(Rectangle r)
    {
        return new Point(r.x+r.width, r.y+r.height);
    }

    /** Return a Dimension with equal sides. */
    public static Dimension squareDim(int side)
    {
        return new Dimension(side, side);
    }

    /** Draw 'image' in 'g' with upper-left at 'pt' and scaled to 'dim'. */
    public static void drawImage(Graphics g, Image image, Point pt, Dimension dim)
    {
        g.drawImage(image, pt.x, pt.y, dim.width, dim.height, null /*observer*/);
    }

    /** Draw 'image' in 'g' with upper-left at 'pt' and scaled to 'dim' (a Point). */
    public static void drawImage(Graphics g, Image image, Point pt, Point dim)
    {
        drawImage(g, image, pt, toDimension(dim));
    }

    /** Draw 'image' in 'g' filling 'r'. */
    public static void drawImage(Graphics g, Image image, Rectangle r)
    {
        drawImage(g, image, topLeft(r), r.getSize());
    }

    /** Convert a Point to a Dimension. */
    public static Dimension toDimension(Point pt)
    {
        return new Dimension(pt.x, pt.y);
    }

    /** Convert a Dimension to a Point. */
    public static Point toPoint(Dimension dim)
    {
        return new Point(dim.width, dim.height);
    }

    /** Subtract 'dim' from 'pt'. */
    public static Point sub(Point pt, Dimension dim)
    {
        return new Point(pt.x - dim.width, pt.y - dim.height);
    }

    /** Return a new Point with x/y swapped. */
    public static Point transpose(Point pt)
    {
        return new Point(pt.y, pt.x);
    }

    /** Return a new Point with x and y negated. */
    public static Point negate(Point pt)
    {
        return new Point(-pt.x, -pt.y);
    }

    /** Return a new Rectangle based on 'r' where the top/left corner
      * is moved by 'dist', leaving its bottom/left corner unchanged. */
    public static Rectangle moveTopLeftBy(Rectangle r, Point dist)
    {
        return new Rectangle(
            r.x + dist.x,
            r.y + dist.y,
            r.width - dist.x,
            r.height - dist.y);
    }

    /** Return a new Rectangle based on 'r' where x or y, depending on 'hv',
      * has been moved by 'delta', but the bottom/right corner is unchanged. */
    public static Rectangle moveOriginBy(Rectangle r, HorizOrVert hv, int delta)
    {
        if (hv.isHoriz()) {
            return new Rectangle(
                r.x + delta,
                r.y,
                r.width - delta,
                r.height);
        }
        else {
            return new Rectangle(
                r.x,
                r.y + delta,
                r.width,
                r.height - delta);
        }
    }

    /** Return a new Rectangle based on 'r' where the bottom/right corner
      * is moved by 'dist', leaving its top/left corner unchanged. */
    public static Rectangle moveBottomRightBy(Rectangle r, Point dist)
    {
        return new Rectangle(
            r.x,
            r.y,
            r.width + dist.x,
            r.height + dist.y);
    }

    /** Return a new Rectangle based on 'r' where its x or y, depending
      * on 'hv', is set to 'nv', with size unchanged. */
    public static Rectangle setOrigin(Rectangle r, HorizOrVert hv, int nv)
    {
        return new Rectangle(
            hv.isHoriz()? nv : r.x,
            hv.isVert()? nv : r.y,
            r.width,
            r.height);
    }

    /** Return a new Rectangle based on 'r' where its x or y, depending
      * on 'hv', is increase by 'nv', with size unchanged. */
    public static Rectangle incOrigin(Rectangle r, HorizOrVert hv, int nv)
    {
        return setOrigin(r, hv, origin(r, hv) + nv);
    }

    /** Return a new Rectangle based on 'r' where its width or height, depending
      * on 'hv', is set to 'nv', with origin unchanged. */
    public static Rectangle setSize(Rectangle r, HorizOrVert hv, int nv)
    {
        return new Rectangle(
            r.x,
            r.y,
            hv.isHoriz()? nv : r.width,
            hv.isVert()? nv : r.height);
    }

    /** Return a new Rectangle based on 'r' where its width or height, depending
      * on 'hv', is increased by 'nv', with origin unchanged. */
    public static Rectangle incSize(Rectangle r, HorizOrVert hv, int nv)
    {
        return setSize(r, hv, size(r, hv) + nv);
    }

    /** Return a new Point that is 'v' with both dimension multiplied
      * by 'scalar'. */
    public static Point mul(Point v, int scalar)
    {
        return new Point(v.x * scalar, v.y * scalar);
    }

    /** Add two Points. */
    public static Point add(Point p1, Point p2)
    {
        return new Point(p1.x+p2.x, p1.y+p2.y);
    }

    /** Add a Dimension and a Point, yielding a Dimension. */
    public static Dimension add(Dimension dim, Point pt)
    {
        return new Dimension(dim.width + pt.x, dim.height + pt.y);
    }

    /** Return width or height of dim depending on 'hv'. */
    public static int size(Dimension dim, HorizOrVert hv)
    {
        return hv.isHoriz()? dim.width : dim.height;
    }

    /** Return (v,0) if hv.isHoriz(), or (0,v) if hv.isVert(). */
    public static Point hvVector(HorizOrVert hv, int v)
    {
        if (hv.isHoriz()) {
            return new Point(v, 0);
        }
        else {
            return new Point(0, v);
        }
    }
}
