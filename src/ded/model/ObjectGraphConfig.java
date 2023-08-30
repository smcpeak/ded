// ObjectGraphConfig.java
// See toplevel license.txt for copyright and license terms.

package ded.model;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import util.Util;
import util.json.JSONUtil;
import util.json.JSONable;

/** Configuration for how to interact with an object graph. */
public class ObjectGraphConfig implements JSONable {
    // ---- public types ----
    /** How to display a field. */
    public static class Field implements JSONable {
        // ---- public data ----
        /** The field key in ObjectGraphNode.m_attributes or
          * ObjectGraphNode.m_pointers. */
        public String m_key;

        /** If not null, an alternative (usually shorter) key to use in
          * the display. */
        public String m_displayKey;

        // ---- public methods ----
        public Field(String key, String displayKey)
        {
            m_key = key;
            m_displayKey = displayKey;
        }

        /** Get the key to use for display. */
        public String getDisplayKey()
        {
            return m_displayKey == null? m_key : m_displayKey;
        }

        // ---- serialization ----
        @Override
        public JSONObject toJSON()
        {
            JSONObject o = new JSONObject();
            try {
                o.put("key", m_key);

                if (m_displayKey != null) {
                    o.put("displayKey", m_displayKey);
                }
            }
            catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return o;
        }

        public Field(JSONObject o) throws JSONException
        {
            m_key = o.getString("key");
            m_displayKey = o.optString("displayKey", null);
        }

        // ---- data class boilerplate ----
        /** Deep copy. */
        public Field(Field src)
        {
            m_key = src.m_key;
            m_displayKey = src.m_displayKey;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null) {
                return false;
            }
            if (this.getClass() != obj.getClass()) {
                return false;
            }
            Field f = (Field)obj;

            return Util.nullableEquals(m_key, f.m_key) &&
                   Util.nullableEquals(m_displayKey, f.m_displayKey);
        }

        @Override
        public int hashCode()
        {
            int h = 0;
            h = h*31 + Util.nullableHashCode(m_key);
            h = h*31 + Util.nullableHashCode(m_displayKey);
            return h;
        }

        @Override
        public String toString()
        {
            return toJSON().toString();
        }
    }

    // ---- public data ----
    /** Sequence of fields to show in the attributes area of an
      * entity box.  Not null.  After changing, one must call
      * 'recomputeShowFieldsMap()'. */
    public ArrayList<Field> m_showFields;

    // I considered using a LinkedHashMap here, but the documentation
    // does not explain enough about how iteration works so I went with
    // something I could more confidently understand.
    /** Map from 'm_key' to its Field. */
    public HashMap<String, Field> m_showFieldsMap;

    // ---- public methods ----
    public ObjectGraphConfig()
    {
        m_showFields = new ArrayList<Field>();
        recomputeShowFieldsMap();
    }

    /** Recompute 'm_showFieldsMap'. */
    public void recomputeShowFieldsMap()
    {
        m_showFieldsMap = new HashMap<String, Field>();
        for (Field f : m_showFields) {
            m_showFieldsMap.put(f.m_key, f);
        }
    }

    /** Get the display key for 'key'. */
    public String getDisplayKey(String key)
    {
        Field f = m_showFieldsMap.get(key);
        if (f != null) {
            return f.getDisplayKey();
        }

        // Not configured to use a different display key.
        return key;
    }

    // ---- serialization ----
    @Override
    public JSONObject toJSON()
    {
        try {
            JSONObject o = new JSONObject();
            o.put("showFields",
                JSONUtil.collectionToJSON(m_showFields));
            return o;
        }
        catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public ObjectGraphConfig(JSONObject o) throws JSONException
    {
        m_showFields = new ArrayList<Field>();

        JSONArray arr = o.getJSONArray("showFields");
        for (int i=0; i < arr.length(); ++i) {
            m_showFields.add(new Field(arr.getJSONObject(i)));
        }

        recomputeShowFieldsMap();
    }

    // ---- data class boilerplate ----
    public ObjectGraphConfig(ObjectGraphConfig src)
    {
        m_showFields = new ArrayList<Field>();
        for (Field f : src.m_showFields) {
            m_showFields.add(new Field(f));
        }

        recomputeShowFieldsMap();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        ObjectGraphConfig c = (ObjectGraphConfig)obj;

        return m_showFields.equals(c.m_showFields);
    }

    @Override
    public int hashCode()
    {
        int h = 0;
        h = h*31 + m_showFields.hashCode();
        return h;
    }

    @Override
    public String toString()
    {
        try {
            return this.toJSON().toString(2 /*indentFactor*/);
        }
        catch (JSONException e) {
            return "<ObjectGraphConfig.toString() failed: "+e.getMessage()+">";
        }
    }
}


// EOF
