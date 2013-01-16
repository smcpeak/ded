// AWTJSONUtil.java
// See toplevel license.txt for copyright and license terms.

package util.awt;

import java.awt.Dimension;
import java.awt.Point;
import java.util.EnumSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Utilities related to serializing AWT objects as JSON. */
public class AWTJSONUtil {
    /** Serialize 'p' as JSON. */
    public static JSONObject pointToJSON(Point p)
    {
        JSONObject o = new JSONObject();
        try {
            o.put("x", p.x);
            o.put("y", p.y);
        }
        catch (JSONException e) { assert(false); }
        return o;
    }

    /** Deserialize a Point from 'o'. */
    public static Point pointFromJSON(JSONObject o) throws JSONException
    {
        Point p = new Point();
        p.x = (int)o.getLong("x");
        p.y = (int)o.getLong("y");
        return p;
    }

    /** Serialize 'd' as JSON. */
    public static JSONObject dimensionToJSON(Dimension d)
    {
        JSONObject o = new JSONObject();
        try {
            o.put("w", d.width);
            o.put("h", d.height);
        }
        catch (JSONException e) { assert(false); }
        return o;
    }

    /** Deserialize a Dimension from 'o'. */
    public static Dimension dimensionFromJSON(JSONObject o) throws JSONException
    {
        Dimension d = new Dimension();
        d.width = (int)o.getLong("w");
        d.height = (int)o.getLong("h");
        return d;
    }

    /** Render 'set' as a JSON array. */
    public static <T extends Enum<T>> JSONArray enumSetToJSON(EnumSet<T> set)
    {
        JSONArray ret = new JSONArray();
        for (T t : set) {
            ret.put(t.name());
        }
        return ret;
    }

    /** Deserialize an EnumSet from a JSON array. */
    public static <T extends Enum<T>>
    EnumSet<T> enumSetFromJSON(Class<T> enumType, JSONArray array) throws JSONException
    {
        EnumSet<T> ret = EnumSet.noneOf(enumType);
        for (int i=0; i < array.length(); i++) {
            String element = array.getString(i);
            try {
                T t = Enum.valueOf(enumType, element);
                ret.add(t);
            }
            catch (IllegalArgumentException e) {
                throw new JSONException(
                    "Unknown enumerator in set of "+enumType.getSimpleName()+
                    ": \""+element+"\": "+e.getMessage());
            }
        }
        return ret;
    }
}

// EOF
