// EntityShape.java
// See toplevel license.txt for copyright and license terms.

package ded.model;

/** Set of possible entity shapes. */
public enum EntityShape {
    ES_NO_SHAPE                        // no outline
        ("No shape", 0),
    ES_RECTANGLE
        ("Rectangle", 0),
    ES_ELLIPSE
        ("Ellipse", 0),
    ES_CUBOID
        ("Cuboid", 2),
    ES_CYLINDER
        ("Cylinder", 0);
    
    /** Name of this shape as shown in the UI. */
    public final String displayName;
    
    /** Number of integer shape parameters needed to draw the shape. */
    public final int numParams;
    
    EntityShape(String dn, int n)
    {
        this.displayName = dn;
        this.numParams = n;
    }

    /** This is what JComboBox uses when it draws the items. */
    @Override
    public String toString()
    {
        return this.displayName;
    }
}

// EOF
