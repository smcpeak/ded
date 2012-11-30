// XParse.java
// See toplevel license.txt for copyright and license terms.

package util;

/** Exception thrown when there is a parse error. */
public class XParse extends Exception {
    private static final long serialVersionUID = 587527042715387305L;

    // ------------------ methods --------------------
    public XParse(String message)
    {
        super(message);
    }

    public XParse(String message, Throwable cause)
    {
        super(message, cause);
    }
}

// EOF
