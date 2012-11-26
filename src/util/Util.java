// Util.java
// See toplevel license.txt for copyright and license terms.

package util;

import java.util.Collection;

/** Generic Java utilities. */
public class Util {
    /** Compute hash code for a collection. */
    public static <T> int collectionHashCode(Collection<T> coll)
    {
        if (coll == null) {
            return 0;
        }
        
        int h = 1;
        for (T t : coll) {
            h = h*31 + t.hashCode(); 
        }
        return h;
    }

    /** Compute the average of two integers without overflow. */
    public static int avg(int a, int b)
    {
        // Clever solution from here:
        // http://code-o-matic.blogspot.com/2010/02/find-average-between-two-ints-facing.html
        //
        // This works because a&b has all the bits where 'a' and 'b'
        // agree, and a^b has where they differ.  But where they differ,
        // you can swap the bits without affecting the sum, and hence
        // the average.  Therefore, you can transform the pair (a,b) to
        // the pair ((a&b),(a&b)+(a^b)) one bit at a time w/o changing
        // the average.
        //
        // Now, (a&b)+(a^b) cannot overflow since it equals (a&b)|(a^b),
        // since no bits are in common, which means that adding a number
        // closer to 0 also cannot overflow.  The return expression
        // is the average of the transformed pair, and will not overflow.
        return (a&b) + (a^b)/2;
    }

    /** Return true if 'v' is NaN or one of the infinities. */
    public static boolean isSpecialDouble(double v)
    {
        return Double.isInfinite(v) || Double.isNaN(v);
    }
}

// EOF
