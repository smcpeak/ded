// Util.java

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
}

// EOF
