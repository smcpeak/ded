// HorizOrVert.java
// See toplevel license.txt for copyright and license terms.

package util.awt;

import java.awt.Point;
import java.util.EnumSet;

/** This abstracts the notion of dimension: horizontal or vertical.  This
  * is mainly useful when working with java.awt.Point and related classes. */
public enum HorizOrVert {
    HV_HORIZ,
    HV_VERT;

    /** True if this is HV_HORIZ, false if it is HV_VERT. */
    public boolean isHoriz()
    {
        return this == HV_HORIZ;
    }

    /** True iff this is HV_VERT. */
    public boolean isVert()
    {
        return this == HV_VERT;
    }

    /** Yield collecton of all values for convenient iteration. */
    public static EnumSet<HorizOrVert> allValues()
    {
        return EnumSet.allOf(HorizOrVert.class);
    }

    /** Yield the opposite dimension. */
    public HorizOrVert opposite()
    {
        return this == HV_HORIZ? HV_VERT : HV_HORIZ;
    }

    /** Add two values, with wrapping: H+H=H, H+V=V, V+V=H. */
    public HorizOrVert plus(HorizOrVert other)
    {
        return this == HV_HORIZ? other : other.opposite();
    }

    /** Extract this dimension from a Point. */
    public int get(Point p)
    {
        return this == HV_HORIZ? p.x : p.y;
    }

    /** Set the coordinate for this dimension in a Point. */
    public void set(Point p, int newValue)
    {
        if (this == HV_HORIZ) {
            p.x = newValue;
        }
        else {
            p.y = newValue;
        }
    }
}

// EOF
