// VTextAlign.java
// See toplevel license.txt for copyright and license terms.

package ded.model;

/** Choices for how to vertically align text. */
public enum VTextAlign {
    VTA_TOP("Top"),
    VTA_CENTER("Center"),
    VTA_BOTTOM("Bottom");

    /** Display name for the GUI. */
    public String displayName;

    VTextAlign(String dn)
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
