// HTextAlign.java
// See toplevel license.txt for copyright and license terms.

package ded.model;

/** Choices for how to horizontally align text. */
public enum HTextAlign {
    // These are called "TA_XXX" instead of "HTA_XXX" because the names
    // are used inside the .ded saved diagram files and I'd prefer not
    // to complicate things by having a set of legacy aliases.
    TA_LEFT("Left"),
    TA_CENTER("Center"),
    TA_RIGHT("Right");

    /** Display name for the GUI. */
    public String displayName;

    HTextAlign(String dn)
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
