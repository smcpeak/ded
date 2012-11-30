// JSONUtil.java
// See toplevel license.txt for copyright and license terms.

package util.json;

import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONException;

/** Utilities for de/serializing with JSONable objects. */
public class JSONUtil {
    /** Put all the serialized elements of 'c' into a JSONArray. */
    public static <T extends JSONable>
    JSONArray collectionToJSON(Collection<T> c) throws JSONException
    {
        JSONArray a = new JSONArray();
        for (T t : c) {
            a.put(t.toJSON());
        }
        return a;
    }

    // I can't write 'collectionFromJSON' without reflection or some
    // factory classes (which I might decide to use at some point).
}

// EOF
