// ObjectGraphConfig.java
// See toplevel license.txt for copyright and license terms.

package ded.model;

import java.util.ArrayList;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import util.Util;
import util.json.JSONUtil;
import util.json.JSONable;

/** Configuration for how to interact with an object graph. */
public class ObjectGraphConfig implements JSONable {
    // ---- public data ----
    /** Sequence of fields to show in the attributes area of an
      * entity box.  Not null.  After changing, one must call
      * 'recomputeShowFieldsSet()'. */
    public ArrayList<String> m_showFields;

    /** Set of elements in 'm_showFields'. */
    public HashSet<String> m_showFieldsSet;

    // ---- public methods ----
    public ObjectGraphConfig()
    {
        m_showFields = new ArrayList<String>();
        recomputeShowFieldsSet();
    }

    /** Recompute 'm_showFieldsSet'. */
    public void recomputeShowFieldsSet()
    {
        m_showFieldsSet = new HashSet<String>();
        for (String f : m_showFields) {
            m_showFieldsSet.add(f);
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

        recomputeShowFieldsSet();
    }

    // ---- data class boilerplate ----
    public ObjectGraphConfig(ObjectGraphConfig src)
    {
        m_showFields = new ArrayList<String>(src.m_showFields);
        recomputeShowFieldsSet();
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
