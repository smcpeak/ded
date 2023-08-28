// ObjectGraph.java
// See toplevel license.txt for copyright and license terms.

package ded.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import util.json.JSONable;

/** Set of objects with attributes and pointers. */
public class ObjectGraph implements JSONable {
    // ---------- public data ------------
    /** The set of nodes. */
    Map<String, ObjectGraphNode> m_nodes;

    // ---------- public methods ------------
    public ObjectGraph()
    {
        m_nodes = new HashMap<String, ObjectGraphNode>();
    }

    /** Retrieve the node for 'id', or null if there is none. */
    public ObjectGraphNode getOptNode(String id)
    {
        return m_nodes.get(id);
    }

    /** Retrieve the node for 'id', insisting that it exists. */
    public ObjectGraphNode getExistingNode(String id)
    {
        ObjectGraphNode n = getOptNode(id);
        if (n == null) {
            throw new RuntimeException("Node not found: "+id);
        }
        return n;
    }

    // ------------------ serialization --------------------
    /** Serialize as JSON. */
    @Override
    public JSONObject toJSON()
    {
        JSONObject json = new JSONObject();

        for (Map.Entry<String, ObjectGraphNode> kv : m_nodes.entrySet()) {
            try {
                json.put(kv.getKey(), kv.getValue().toJSON());
            }
            catch (JSONException e) {
                // Should be impossible.
                throw new RuntimeException("ObjectGraph.toJSON failed", e);
            }
        }

        return json;
    }

    /** Deserialize from JSON.

        The fields of an object are treated as either attributes or
        pointers.  Pointers are recognized by having a value that is an
        object with a single 'ptr' field whose string-typed value
        matches the key for some node.

        Example:

          {
            "n1": {
              "title": "one",
              "succ": { "ptr": "n2" },
            },
            "n2": {
              "color": "green",
              "pred": { "ptr": "n1" },
            },
          }
      */
    public ObjectGraph(JSONObject jsonGraph) throws JSONException
    {
        m_nodes = new HashMap<String, ObjectGraphNode>();

        Iterator it = jsonGraph.keys();
        while (it.hasNext()) {
            String id = (String)it.next();

            JSONObject jsonNode = jsonGraph.optJSONObject(id);
            if (jsonNode == null) {
                throw new JSONException(
                    "While parsing the object graph JSON, the value for "+
                    "key \""+id+"\" is not an object.");
            }

            ObjectGraphNode node = new ObjectGraphNode(id, jsonNode);
            m_nodes.put(id, node);
        }
    }

    // ------------------ data object boilerplate --------------------
    /** Deep copy. */
    public ObjectGraph(ObjectGraph src)
    {
        this.m_nodes = new HashMap<String, ObjectGraphNode>();

        // Build clones of the objects.
        for (Map.Entry<String, ObjectGraphNode> kv :
                 src.m_nodes.entrySet()) {
            this.m_nodes.put(kv.getKey(),
                new ObjectGraphNode(kv.getValue()));
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
        ObjectGraph graph = (ObjectGraph)obj;

        if (this.m_nodes.size() != graph.m_nodes.size()) {
            return false;
        }

        // Check that every ID in 'this' is also in 'graph', and with
        // equal values.
        for (Map.Entry<String, ObjectGraphNode> kv :
                 this.m_nodes.entrySet()) {
            String id = kv.getKey();
            ObjectGraphNode thisNode = kv.getValue();
            assert(thisNode != null);

            ObjectGraphNode graphNode = graph.m_nodes.get(id);
            if (graphNode == null) {
                return false;
            }

            if (!thisNode.equals(graphNode)) {
                return false;
            }
        }

        // Since the number of keys is the same, every key is unique in
        // its map, and every key in 'this' is in 'graph', it is not
        // possible for 'graph' to have a key that 'this' is missing.

        return true;
    }

    @Override
    public int hashCode()
    {
        // This incorporates the node hash codes.
        return m_nodes.hashCode();
    }

    @Override
    public String toString()
    {
        try {
            return this.toJSON().toString(2 /*indentFactor*/);
        }
        catch (JSONException e) {
            // The documentation mentions this could happen if the
            // object contains an "invalid number".  Hmmm.
            return "<ObjectGraph.toString() failed: "+e.getMessage()+">";
        }
    }
}

// EOF
