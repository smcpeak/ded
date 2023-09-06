// MapUtil.java
// See toplevel license.txt for copyright and license terms.

package util;

import java.util.Map;


/** Utilities related to maps. */
public class MapUtil {
    /** For a map to Integer, increment the value at 'k', creating it if
        necessary. */
    public static <K> void incrementValue(Map<K, Integer> m, K k)
    {
        Integer i = m.get(k);
        if (i == null) {
            m.put(k, Integer.valueOf(1));
        }
        else {
            m.put(k, Integer.valueOf(i + 1));
        }
    }
}


// EOF
