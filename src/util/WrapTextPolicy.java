// WrapTextPolicy.java
// See toplevel license.txt for copyright and license terms.

package util;


/** Policy controlling the 'WrapText' class behavior. */
public enum WrapTextPolicy {
    // ---- enumerators ----
    /** Do not wrap the text at all. */
    NoWrap    ("Never"),

    /** Wrap on any whitespace. */
    Whitespace("At whitespace"),

    /** Wrap only after punctuation that ends a sentence or has a
        similar effect. */
    Sentence  ("After sentences");

    // ---- data ----
    /** The description to use in the GUI. */
    public final String m_description;

    // ---- methods ----
    private WrapTextPolicy(String description)
    {
        this.m_description = description;
    }

    @Override
    public String toString()
    {
        return m_description;
    }

    /** Return the policy corresponding to 'name', or null if it is not
        recognized. */
    public static WrapTextPolicy fromString(String name)
    {
        try {
            return WrapTextPolicy.valueOf(WrapTextPolicy.class, name);
        }
        catch (Exception e) {
            return null;
        }
    }
};


// EOF
