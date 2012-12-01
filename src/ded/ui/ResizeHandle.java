// ResizeHandle.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

/** Set of possible handles to resize something. */
public enum ResizeHandle {
    RH_TOP_LEFT         (0,0),
    RH_TOP              (1,0),
    RH_TOP_RIGHT        (2,0),
    RH_RIGHT            (2,1),
    RH_BOTTOM_RIGHT     (2,2),
    RH_BOTTOM           (1,2),
    RH_BOTTOM_LEFT      (0,2),
    RH_LEFT             (0,1);

    public static final int NUM_RESIZE_HANDLES = 8;

    /** Entity-relative location of this handle: 0=left, 1=mid, 2=right. */
    public final int handleX;

    /** 0=top, 1=mid, 2=bottom. */
    public final int handleY;

    ResizeHandle(int handleX, int handleY)
    {
        this.handleX = handleX;
        this.handleY = handleY;
    }
}

// EOF
