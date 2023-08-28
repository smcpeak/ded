// ObjectGraphNode.java
// See toplevel license.txt for copyright and license terms.

package ded.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import util.json.JSONUtil;
import util.json.JSONable;

/** An object with attributes and pointers to other objects. */
public class ObjectGraphNode implements JSONable {
    // ---------- public data ------------
    // The identifier for this node, which must be unique among nodes in
    // the graph.
    public String m_id;

    // Set of non-pointer attributes.
    public JSONObject m_attributes;

    // Set of pointers to other nodes.  The pointee is identified by its
    // ID rather than a direct reference so that nodes are more
    // self-contained, easing de/serialization and making it possible to
    // tolerate broken links.
    public Map<String, String> m_pointers;

    // ----------- public methods -----------
    public ObjectGraphNode(String id)
    {
        m_id = id;
        m_attributes = new JSONObject();
        m_pointers = new HashMap<String, String>();
    }

    /** Return a deep copy of the attributes. */
    public JSONObject cloneAttributes()
    {
        // It's sort of nuts to clone this by serializing and
        // deserializing but I'm not seeing an easy alternative.
        try {
            return new JSONObject(m_attributes.toString());
        }
        catch (JSONException e) {
            throw new RuntimeException(
                "Round-trip JSON array serialization failed", e);
        }
    }

    /** Add a pointer named 'key' at 'targetID', insisting that the key
      * not already be mapped. */
    void addNewPointer(String key, String targetID)
    {
        if (m_pointers.containsKey(key)) {
            throw new RuntimeException("Key already mapped: "+key);
        }
        m_pointers.put(key, targetID);
    }

    /** Get a name for this node suitable for display. */
    public String getNameForDisplay()
    {
        // If 'name' is set, then use that.
        String name = m_attributes.optString("name", null);
        if (name != null) {
            return name;
        }

        // Fallback on the ID.
        return m_id;
    }

    /** Get the attribute value of 'key' as a string. */
    public String getAttributeString(String key)
    {
        try {
            return m_attributes.get(key).toString();
        }
        catch (JSONException e) {
            return "<exn: "+e+">";
        }
    }

    // ------------------ serialization --------------------
    /** Serialize as JSON. */
    @Override
    public JSONObject toJSON()
    {
        JSONObject jsonNode = cloneAttributes();

        for (Map.Entry<String, String> kv : m_pointers.entrySet()) {
            try {
                // Encode the value as an object with 'ptr' field.
                JSONObject jsonPtr = new JSONObject();
                jsonPtr.put("ptr", kv.getValue());

                // Put that into the JSON.  If an attribute and a
                // pointer have the same key, the pointer will win.
                jsonNode.put(kv.getKey(), jsonPtr);
            }
            catch (JSONException e) {
                // This should be impossible because all of the inputs
                // to 'put' are known to be valid.
                throw new RuntimeException(
                    "ObjectGraphNode.toJSON failed: k="+kv.getKey()+
                    " v="+kv.getValue(), e);
            }
        }

        return jsonNode;
    }

    /** See if the value at 'key' in 'jsonNode' is the JSON encoding of
      * a pointer, and if so, return the encoded ID.  Otherwise return
      * null. */
    private String getJSONNodeTargetID(String key, JSONObject jsonNode)
    {
        JSONObject jsonPtr = jsonNode.optJSONObject(key);
        if (jsonPtr == null) {
            // Value not an object.
            return null;
        }

        if (!jsonPtr.has("ptr")) {
            return null;
        }

        if (jsonPtr.length() != 1) {
            return null;
        }

        String targetID = jsonPtr.optString("ptr", null);
        if (targetID == null) {
            // Value was not a string.
            return null;
        }

        return targetID;
    }

    /** Deserialize from JSON. */
    public ObjectGraphNode(String id, JSONObject jsonNode)
        throws JSONException
    {
        m_id = id;
        m_attributes = new JSONObject();
        m_pointers = new HashMap<String, String>();

        Iterator it = jsonNode.keys();
        while (it.hasNext()) {
            String key = (String)it.next();

            String targetID = getJSONNodeTargetID(key, jsonNode);
            if (targetID != null) {
                m_pointers.put(key, targetID);
            }
            else {
                // Could be any kind of JSON data.
                m_attributes.put(key, jsonNode.get(key));
            }
        }
    }

    // ------------- data object boilerplate -------------
    /** Deep copy. */
    public ObjectGraphNode(ObjectGraphNode src)
    {
        this.m_id = src.m_id;
        this.m_attributes = src.cloneAttributes();
        this.m_pointers = new HashMap<String, String>(src.m_pointers);
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
        ObjectGraphNode node = (ObjectGraphNode)obj;

        return this.m_id.equals(node.m_id) &&
               JSONUtil.equalJSONObjects(
                   this.m_attributes, node.m_attributes) &&
               this.m_pointers.equals(node.m_pointers);
    }
}

// EOF
