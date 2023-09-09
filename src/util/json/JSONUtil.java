// JSONUtil.java
// See toplevel license.txt for copyright and license terms.

package util.json;

import java.util.Collection;

import java.util.zip.Deflater;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;


/** Utilities for working with JSON objects. */
public class JSONUtil {
    /** True to enable some debug printouts. */
    public static final boolean s_debug = false;

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

    /** Return true if 'a' and 'b' are equal.  This tolerates either
        being null, and regards null as only equal to itself. */
    public static boolean equalJSONObjects(JSONObject a, JSONObject b)
    {
        if (a == null || b == null) {
            return a == b;
        }

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

    /** Get the number of bytes required to encode 'o' as JSON when
      * using the specified amount of indentation.  When 'indent' is 0,
      * no indentation, spacing, or newlines are used.  If 'compressed',
      * then we compress using Deflate and count the compressed size. */
    public static int jsonEncodingBytes(
        JSONObject o,
        int indent,
        boolean compressed) throws JSONException
    {
        String jsonString = o.toString(indent);
        if (s_debug) {
            System.err.println(jsonString);
        }
        byte[] jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);

        if (compressed) {
            Deflater deflater =
                new Deflater(Deflater.BEST_COMPRESSION, true /*nowrap*/);
            deflater.setInput(jsonBytes);
            deflater.finish();

            // Is this how it's supposed to be used?
            byte[] compressedBytes = new byte[jsonBytes.length + 10];
            int compressedLen = deflater.deflate(compressedBytes);
            assert(compressedLen != 0);

            return compressedLen;
        }
        else {
            return jsonBytes.length;
        }
    }

    /** Put (key,value) into 'o'.  If 'value' is null, explicitly use
      * JSONObject.NULL as the value, whereas, otherwise, null means to
      * remove the key from the object. */
    public static void putExplicitNullable(
        JSONObject o, String key, Object value) throws JSONException
    {
        if (value == null) {
            o.put(key, JSONObject.NULL);
        }
        else {
            o.put(key, value);
        }
    }

    /** Read a JSON object from a file, given its name. */
    public static JSONObject readObjectFromFileName(String fname)
        throws IOException, JSONException
    {
        return readObjectFromFile(new File(fname));
    }

    /** Read a JSON object from a File object. */
    public static JSONObject readObjectFromFile(File file)
        throws IOException, JSONException
    {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            return readObjectFromInputStream(inputStream);
        }
        finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    /** Read a JSON object from an InputStream. */
    public static JSONObject readObjectFromInputStream(InputStream inputStream)
        throws IOException, JSONException
    {
        return readObjectFromReader(
            new BufferedReader(
                new InputStreamReader(inputStream, "UTF-8")));
    }

    /** Read a JSON object from a Reader. */
    public static JSONObject readObjectFromReader(Reader r)
        throws IOException, JSONException
    {
        return new JSONObject(new JSONTokener(r));
    }

    /** Read a JSON object from a String containing JSON.

        This just calls 'new JSONObject(s)', but I am providing the
        method for uniformity with the ones above. */
    public static JSONObject readObjectFromString(String s)
        throws JSONException
    {
        return new JSONObject(s);
    }
}

// EOF
