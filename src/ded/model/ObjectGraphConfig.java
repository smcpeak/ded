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

    /** The name to assign a newly created graph node.  If null, we use
      * a hardcoded default. */
    public String m_newNodeName = null;

    /** The attributes to assign a newly created graph node.  If null,
      * we use a hardcoded default. */
    public String m_newNodeAttributes = null;

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

    /** Get the string to put into the 'name' member of a newly
      * created Entity that represents a graph node. */
    public String getNewNodeName()
    {
        if (m_newNodeName == null) {
            return "$(graphNodeID)";
        }
        else {
            return m_newNodeName;
        }
    }

    /** Get the string to put into the 'attributes' member of a newly
      * created Entity that represents a graph node. */
    public String getNewNodeAttributes()
    {
        if (m_newNodeAttributes == null) {
            // This separation helps to easily see the pointers when I
            // want to, and also makes it easy to hide them by adjusting
            // the entity height (without leaving part of a line showing
            // at the edge).
            return "$(graphNodeShowFieldsAttrs)\n"+
                   "\n"+
                   "$(graphNodeShowFieldsPtrs)\n";
        }
        else {
            return m_newNodeAttributes;
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
            JSONUtil.putExplicitNullable(o, "newNodeName", m_newNodeName);
            JSONUtil.putExplicitNullable(o, "newNodeAttributes", m_newNodeAttributes);

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

        m_newNodeName = o.optString("newNodeName", null);
        m_newNodeAttributes = o.optString("newNodeAttributes", null);
    }

    // ---- data class boilerplate ----
    public ObjectGraphConfig(ObjectGraphConfig src)
    {
        m_showFields = new ArrayList<String>(src.m_showFields);
        recomputeShowFieldsMap();

        m_newNodeName = src.m_newNodeName;
        m_newNodeAttributes = src.m_newNodeAttributes;
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

        return m_showFields.equals(c.m_showFields) &&
               Util.nullableEquals(m_newNodeName, c.m_newNodeName) &&
               Util.nullableEquals(m_newNodeAttributes, c.m_newNodeAttributes);
    }

    @Override
    public int hashCode()
    {
        int h = 0;
        h = h*31 + m_showFields.hashCode();
        h = h*31 + Util.nullableHashCode(m_newNodeName);
        h = h*31 + Util.nullableHashCode(m_newNodeAttributes);
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
