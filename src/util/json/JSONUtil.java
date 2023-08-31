// JSONUtil.java
// See toplevel license.txt for copyright and license terms.

package util.json;

import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Utilities for working with JSON objects. */
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

    /** Put all the serialized elements of 'c' into a JSONArray. */
    public static
    JSONArray stringCollectionToJSONArray(Collection<String> c)
        throws JSONException
    {
        JSONArray a = new JSONArray();
        for (String s : c) {
            a.put(s);
        }
        return a;
    }

    // I can't write 'collectionFromJSON' without reflection or some
    // factory classes (which I might decide to use at some point).

    /** Return true if 'a' and 'b' are equal. */
    public static boolean equalJSONObjects(JSONObject a, JSONObject b)
    {
        // This is crude, but JSONObject does not have a proper 'equals'
        // method.  Fortunately, its serialization code alphabetizes
        // the keys, so that does not cause problems here.
        String aString = a.toString();
        String bString = b.toString();

        if (aString == bString) {
            // This includes the case where both are null.  In that
            // case, in a sense both do not represent anything, and will
            // both serialize to "null", so calling them equal is
            // perhaps sensible.
            return true;
        }

        if (aString == null) {
            // This can happen when the structure contains values that
            // cannot be represented in JSON.
            return false;
        }

        return aString.equals(bString);
    }
}

// EOF
