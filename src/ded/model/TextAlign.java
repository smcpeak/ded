// TextAlign.java
// See toplevel license.txt for copyright and license terms.

package ded.model;

/** Choices for how to horizontally align text. */
public enum TextAlign {
    TA_LEFT("Left"),
    TA_CENTER("Center"),
    TA_RIGHT("Right");

    /** Display name for the GUI. */
    public String displayName;

    TextAlign(String dn)
    {
        this.displayName = dn;
    }

    @Override
    public String toString()
    {
        return this.displayName;
    }
}

// EOF
