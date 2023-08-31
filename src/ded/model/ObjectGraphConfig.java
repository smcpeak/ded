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
    // ---- private data ----
    /** Map of elements in 'm_showFields' to their 0-based ordinal
      * position. */
    private HashMap<String, Integer> m_showFieldsMap;

    // ---- public data ----
    /** Sequence of fields to show in the attributes area of an
      * entity box.  Not null.  After changing, one must call
      * 'recomputeShowFieldsMap()'. */
    public ArrayList<String> m_showFields;

    // ---- public methods ----
    public ObjectGraphConfig()
    {
        setShowFields(new ArrayList<String>());
    }

    /** Update the "show fields" sequence and recompute the map. */
    public void setShowFields(ArrayList<String> newShowFields)
    {
        m_showFields = newShowFields;
        recomputeShowFieldsMap();
    }

    /** Recompute 'm_showFieldsMap'. */
    public void recomputeShowFieldsMap()
    {
        m_showFieldsMap = new HashMap<String, Integer>();

        int pos = 0;
        for (String f : m_showFields) {
            m_showFieldsMap.put(f, pos);
            ++pos;
        }
    }

    /** If 'key' is in 'm_showFields', return its 0-based ordinal
      * position as a Float with integer value.  Otherwise return null.
      */
    public Float getFieldOrdinalFloatOpt(String key)
    {
        Integer i = m_showFieldsMap.get(key);
        if (i != null) {
            return Float.valueOf(i);
        }
        else {
            return null;
        }
    }

    // ---- serialization ----
    @Override
    public JSONObject toJSON()
    {
        try {
            JSONObject o = new JSONObject();
            o.put("showFields",
                JSONUtil.stringCollectionToJSONArray(m_showFields));
            return o;
        }
        catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public ObjectGraphConfig(JSONObject o) throws JSONException
    {
        m_showFields = new ArrayList<String>();

        JSONArray arr = o.getJSONArray("showFields");
        for (int i=0; i < arr.length(); ++i) {
            m_showFields.add(arr.getString(i));
        }

        recomputeShowFieldsMap();
    }

    // ---- data class boilerplate ----
    public ObjectGraphConfig(ObjectGraphConfig src)
    {
        m_showFields = new ArrayList<String>(src.m_showFields);
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
