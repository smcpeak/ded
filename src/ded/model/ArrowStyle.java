// ArrowStyle.java
// See toplevel license.txt for copyright and license terms.

package ded.model;

/** Ways of styling an arrowhead. */
public enum ArrowStyle {
    AS_NONE                            // No arrowhead.
        ("None"),
    AS_FILLED_TRIANGLE                 // Default solid equilateral triangle.
        ("Filled triangle"),
    AS_DOUBLE_ANGLE                    // Like "--->>".
        ("Double angle");

    /** Arrow style name for display in the UI. */
    public String description;

    ArrowStyle(String d)
    {
        this.description = d;
    }

    @Override
    public String toString()
    {
        return this.description;
    }
}

// EOF
