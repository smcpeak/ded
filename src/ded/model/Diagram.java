// Diagram.java

package ded.model;

import java.awt.Dimension;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import util.AWTJSONUtil;
import util.json.JSONUtil;
import util.json.JSONable;

/** Complete diagram. */
public class Diagram implements JSONable {
    // ---------- constants ------------
    public static final String jsonType = "Diagram Editor Diagram";
    
    // ---------- public data ------------
    /** Size of window to display diagram.  Some elements might not fit
      * in the current size. */
    public Dimension windowSize;
    
    /** Entities, in display order.  The last entity will appear on top
      * of all others. */
    public ArrayList<Entity> entities;
    
    // ----------- public methods -----------
    public Diagram()
    {
        this.windowSize = new Dimension(800, 800);
        this.entities = new ArrayList<Entity>();
    }

    @Override
    public JSONObject toJSON()
    {
        JSONObject o = new JSONObject();
        try {
            o.put("type", jsonType);
            o.put("version", 1);
            o.put("windowSize", AWTJSONUtil.dimensionToJSON(this.windowSize));
            o.put("entities", JSONUtil.collectionToJSON(this.entities));
        }
        catch (JSONException e) {/*impossible*/}
        return o;
    }
    
    /** Deserialize from 'o'. */
    public void fromJSON(JSONObject o) throws JSONException
    {
        String type = o.getString("type");
        if (!type.equals(jsonType)) {
            throw new JSONException("unexpected file type: \""+type+"\"");
        }
        
        int ver = (int)o.getLong("version");
        if (ver != 1) {
            throw new JSONException("unknown file version: "+ver);
        }
        
        this.windowSize = AWTJSONUtil.dimensionFromJSON(o.getJSONObject("windowSize"));
        
        this.entities.clear();
        JSONArray a = o.getJSONArray("entities");
        for (int i=0; i < a.length(); i++) {
            Entity e = new Entity();
            e.fromJSON(a.getJSONObject(i));
            this.entities.add(e);
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof Diagram) {
            Diagram d = (Diagram)obj;
            return this.windowSize.equals(d.windowSize) &&
                   this.entities.equals(d.entities);
        }
        return false;
    }
    
    @Override
    public String toString()
    {
        return this.toJSON().toString();
    }
}

// EOF
