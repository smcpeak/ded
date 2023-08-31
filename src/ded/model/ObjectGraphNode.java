// ObjectGraphNode.java
// See toplevel license.txt for copyright and license terms.

package ded.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import util.StringUtil;
import util.Util;
import util.json.JSONUtil;
import util.json.JSONable;

import static util.StringUtil.fmt;

/** An object with attributes and pointers to other objects. */
public class ObjectGraphNode implements JSONable {
    // ---------- public types ------------
    /** A 'Ptr' encodes a pointer from one object graph node to
      * another, along with an optional "preview" string to display
      * ahead of following that pointer interactively. */
    public static class Ptr implements JSONable {
        // ID of the node this points at.
        //
        // The pointee is identified by its ID rather than a direct
        // ObjectGraphNode reference so that nodes are more
        // self-contained, easing de/serialization and making it
        // possible to tolerate broken links.
        public String m_ptr;

        // An optional (nullable) string containing some indication of
        // what is in the referred object.
        public String m_preview;

        public Ptr(String ptr, String preview)
        {
            this.m_ptr = ptr;
            this.m_preview = preview;
        }

        // ---- de/serialization ----
        @Override
        public JSONObject toJSON()
        {
            JSONObject json = new JSONObject();

            try {
                json.put("ptr", m_ptr);
                if (m_preview != null) {
                    json.put("preview", m_preview);
                }
            }
            catch (JSONException e) {
                // The only reason the comments in JSONObject give for
                // possibly throwing from 'put' is if the key is null,
                // so this should be impossible.
                throw new RuntimeException(
                    "ObjectGraphNode.toJSON failed", e);
            }

            return json;
        }

        public Ptr(JSONObject json) throws JSONException
        {
            this.m_ptr = json.getString("ptr");
            this.m_preview = json.optString("preview", null);
        }

        // ---- data object boilerplate ----
        /** Deep copy. */
        public Ptr(Ptr src)
        {
            this.m_ptr = src.m_ptr;
            this.m_preview = src.m_preview;
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
            Ptr ptr = (Ptr)obj;

            return this.m_ptr.equals(ptr.m_ptr) &&
                   Util.nullableEquals(this.m_preview, ptr.m_preview);
        }

        @Override
        public int hashCode()
        {
            return 31 * m_ptr.hashCode() +
                   Util.nullableHashCode(m_preview);
        }

        @Override
        public String toString()
        {
            return this.toJSON().toString();
        }
    }

    // ---------- public class data ------------
    /** Enable various debugging printouts. */
    public static boolean s_debug = true;

    // ---------- public instance data ------------
    /** The identifier for this node, which must be unique among nodes
      * in the graph. */
    public String m_id;

    /** Set of non-pointer attributes. */
    public JSONObject m_attributes;

    /*
      Set of pointers to other nodes.

      Invariant:
          Util.disjointSets(m_attributes.keySet(), m_pointers.keySet())
    */
    public Map<String, Ptr> m_pointers;

    // ----------- public methods -----------
    public ObjectGraphNode(String id)
    {
        m_id = id;
        m_attributes = new JSONObject();
        m_pointers = new HashMap<String, Ptr>();
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

    /** Get the attribute value of 'key' as a string. */
    public String getAttributeString(String key)
    {
        try {
            if (!m_attributes.has(key)) {
                return fmt("<No such attr key: \"%1$s\".>", key);
            }

            return m_attributes.get(key).toString();
        }
        catch (JSONException e) {
            return "<exn: "+e+">";
        }
    }

    /** Get the pointer value of 'key' as a string. */
    public String getPointerString(String key)
    {
        Ptr ptr = m_pointers.get(key);
        if (ptr == null) {
            return fmt("<No such ptr key: \"%1$s\".>", key);
        }
        else {
            StringBuilder sb = new StringBuilder();

            sb.append("-> " + ptr.m_ptr);
            if (ptr.m_preview != null) {
                sb.append(" " + StringUtil.quoteAsJSONASCII(ptr.m_preview));
            }

            return sb.toString();
        }
    }

    public void selfCheck()
    {
        // Check that the key sets are disjoint.
        Util.disjointSets(m_attributes.keySet(), m_pointers.keySet());
    }

    // ------------------ serialization --------------------
    /** Serialize as JSON. */
    @Override
    public JSONObject toJSON()
    {
        JSONObject jsonNode = cloneAttributes();

        for (Map.Entry<String, Ptr> kv : m_pointers.entrySet()) {
            try {
                // Encode the value as an object with 'ptr' field.
                JSONObject jsonPtr = kv.getValue().toJSON();

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
      * a pointer, and if so, return it as a 'Ptr' object.  Otherwise
      * return null. */
    private Ptr getJSONNodePtrOpt(JSONObject jsonNode, String key)
    {
        JSONObject jsonPtr = jsonNode.optJSONObject(key);
        if (jsonPtr == null) {
            // Value not an object.
            return null;
        }

        if (!jsonPtr.has("ptr")) {
            return null;
        }

        int expectKeys = 1 + (jsonPtr.has("preview")? 1 : 0);
        if (jsonPtr.length() != expectKeys) {
            return null;
        }

        try {
            return new Ptr(jsonPtr);
        }
        catch (JSONException e) {
            if (s_debug) {
                System.err.println(fmt(
                    "jsonPtr failed to parse: %1$s",
                    Util.getExceptionMessage(e)));
            }

            // If it cannot be parsed as 'Ptr', just say it is not one.
            return null;
        }
    }

    /** Deserialize from JSON. */
    public ObjectGraphNode(String id, JSONObject jsonNode)
        throws JSONException
    {
        m_id = id;
        m_attributes = new JSONObject();
        m_pointers = new HashMap<String, Ptr>();

        Iterator it = jsonNode.keys();
        while (it.hasNext()) {
            String key = (String)it.next();

            Ptr ptr = getJSONNodePtrOpt(jsonNode, key);
            if (ptr != null) {
                m_pointers.put(key, ptr);
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

        this.m_pointers = new HashMap<String, Ptr>();
        for (Map.Entry<String, Ptr> kv : src.m_pointers.entrySet()) {
            this.m_pointers.put(kv.getKey(), new Ptr(kv.getValue()));
        }
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
