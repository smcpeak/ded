// StringMeasurer.java
// See toplevel license.txt for copyright and license terms.

package util;


/** Measure strings in order to compute wrapped text. */
public interface StringMeasurer {
    /** Return the width, in pixels, that 's.substring(start, end)'
        would take to draw. */
    public int substringWidth(String s, int start, int end);
}


// EOF
