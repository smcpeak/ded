// Diagram.java

package ded.model;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import util.Util;
import util.awt.AWTJSONUtil;
import util.json.JSONable;

/** Complete diagram. */
public class Diagram implements JSONable {
    // ---------- constants ------------
    public static final String jsonType = "Diagram Editor Diagram";
    
    // ---------- public data ------------
    /** Size of window to display diagram.  Some elements might not fit
      * in the current size.
      * 
      * Currently, this is the size of the visible content area.  The
      * surrounding window is generally larger, but that depends on
      * the window system. */
    public Dimension windowSize;
    
    /** Entities, in display order.  The last entity will appear on top
      * of all others. */
    public ArrayList<Entity> entities;
    
    /** Inheritance nodes. */
    public ArrayList<Inheritance> inheritances;
    
    /** Relations. */
    public ArrayList<Relation> relations;
    
    // ----------- public methods -----------
    public Diagram()
    {
        this.windowSize = new Dimension(800, 800);
        this.entities = new ArrayList<Entity>();
        this.inheritances = new ArrayList<Inheritance>();
        this.relations = new ArrayList<Relation>();
    }

    public void selfCheck()
    {
        for (Relation r : this.relations) {
            r.globalSelfCheck(this);
        }
        
        for (Inheritance i : this.inheritances) {
            i.globalSelfCheck(this);
        }
    }
    
    // ------------------ serialization --------------------
    @Override
    public JSONObject toJSON()
    {
        JSONObject o = new JSONObject();
        try {
            o.put("type", jsonType);
            o.put("version", 2);
            
            o.put("windowSize", AWTJSONUtil.dimensionToJSON(this.windowSize));
            
            // Map from an entity to its position in the serialized
            // 'entities' array, so it can be referenced by inheritances
            // and relations.
            HashMap<Entity, Integer> entityToInteger = 
                new HashMap<Entity, Integer>();
            
            // Entities.
            JSONArray arr = new JSONArray();
            int index = 0;
            for (Entity e : this.entities) {
                entityToInteger.put(e, index++);
                arr.put(e.toJSON());
            }
            o.put("entities", arr);
            
            // Map from inheritance to serialized position.
            HashMap<Inheritance, Integer> inheritanceToInteger = 
                new HashMap<Inheritance, Integer>();
            
            // Inheritances.
            arr = new JSONArray();
            index = 0;
            for (Inheritance inh : this.inheritances) {
                inheritanceToInteger.put(inh, index++);
                arr.put(inh.toJSON(entityToInteger));
            }
            o.put("inheritances", arr);

            // Relations.
            arr = new JSONArray();
            index = 0;
            for (Relation rel : this.relations) {
                arr.put(rel.toJSON(entityToInteger, inheritanceToInteger));
            }
            o.put("relations", arr);
        }
        catch (JSONException e) { assert(false); }
        return o;
    }
    
    /** Deserialize from 'o'. */
    public Diagram(JSONObject o) throws JSONException
    {
        String type = o.getString("type");
        if (!type.equals(jsonType)) {
            throw new JSONException("unexpected file type: \""+type+"\"");
        }
        
        int ver = (int)o.getLong("version");
        if (!( 1 <= ver && ver <= 2 )) {
            throw new JSONException("unknown file version: "+ver);
        }
        
        this.windowSize = AWTJSONUtil.dimensionFromJSON(o.getJSONObject("windowSize"));

        // Make the lists now; this is particularly useful for handling
        // older file formats.
        this.entities = new ArrayList<Entity>();
        this.inheritances = new ArrayList<Inheritance>();
        this.relations = new ArrayList<Relation>();
        
        // Map from serialized position to deserialized Entity.
        ArrayList<Entity> integerToEntity = new ArrayList<Entity>();
        
        // Entities.
        JSONArray a = o.getJSONArray("entities");
        for (int i=0; i < a.length(); i++) {
            Entity e = new Entity(a.getJSONObject(i));
            this.entities.add(e);
            integerToEntity.add(e);
        }

        if (ver == 1) {
            // Version 1 did not have inheritances or relations.
            return;
        }
        
        // Map from serialized position to deserialized Inheritance.
        ArrayList<Inheritance> integerToInheritance = new ArrayList<Inheritance>();
        
        // Inheritances.
        a = o.getJSONArray("inheritances");
        for (int i=0; i < a.length(); i++) {
            Inheritance inh = 
                new Inheritance(a.getJSONObject(i), integerToEntity);
            this.inheritances.add(inh);
            integerToInheritance.add(inh);
        }
        
        // Relations.
        a = o.getJSONArray("relations");
        for (int i=0; i < a.length(); i++) {
            Relation rel =
                new Relation(a.getJSONObject(i), integerToEntity, integerToInheritance);
            this.relations.add(rel);
        }
    }

    /** Write this diagram to the specified file. */
    public void saveToFile(String fname) throws Exception
    {
        JSONObject serialized = this.toJSON();
        Writer w = null;
        try {
            w = new BufferedWriter(new FileWriter(fname));
            serialized.write(w, 2, 0);
            w.append('\n');
        }
        finally {
            if (w != null) {
                w.close();
            }
        }
    }

    /** Read a diagram from a file and return the new Diagram object. */
    public static Diagram readFromFile(String fname) throws Exception
    {
        Reader r = null;
        JSONObject obj;
        try {
            r = new BufferedReader(new FileReader(fname));
            obj = new JSONObject(new JSONTokener(r));
        }
        finally {
            if (r != null) {
                r.close();
            }
        }
        return new Diagram(obj);
    }
    
    // ------------------ data object boilerplate ------------------------
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass()) {
            Diagram d = (Diagram)obj;
            return this.windowSize.equals(d.windowSize) &&
                   this.entities.equals(d.entities);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int h = 1;
        h = h*31 + this.windowSize.hashCode();
        h = h*31 + Util.collectionHashCode(this.entities);
        return h;
    }
    
    @Override
    public String toString()
    {
        return this.toJSON().toString();
    }
}

// EOF
