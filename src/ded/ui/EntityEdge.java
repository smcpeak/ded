// EntityEdge.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

/** Name of one of the four edges of an entity. */
public enum EntityEdge {
    EE_LEFT  (false),
    EE_TOP   (false),
    EE_RIGHT (true),
    EE_BOTTOM(true);

    /** True if more extreme values, for alignment purposes, are larger;
      * false if they are smaller.  Left/top is false, right/bottom is true. */
    public boolean extremeIsGreater;

    EntityEdge(boolean extremeIsGreater)
    {
        this.extremeIsGreater = extremeIsGreater;
    }
}

// EOF
