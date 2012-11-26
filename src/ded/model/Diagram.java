// Diagram.java
// See toplevel license.txt for copyright and license terms.

package ded.model;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import util.FlattenInputStream;
import util.Util;
import util.XParse;
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
    
    /** When true, the editor will paint the diagram file name in the
      * upper-left corner of the editing area, and also include it
      * when exporting to other file formats.  Normally true. */
    public boolean drawFileName;
   
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
        this.drawFileName = true;
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
            o.put("version", 3);
            
            o.put("windowSize", AWTJSONUtil.dimensionToJSON(this.windowSize));
            o.put("drawFileName", this.drawFileName);
            
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
        if (!( 1 <= ver && ver <= 3 )) {
            throw new JSONException("unknown file version: "+ver);
        }
        
        this.windowSize = AWTJSONUtil.dimensionFromJSON(o.getJSONObject("windowSize"));

        if (ver >= 3) {
            this.drawFileName = o.getBoolean("drawFileName");
        }
        else {
            this.drawFileName = true;
        }

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

        if (ver >= 2) {
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

    /** Read a Diagram from a file, expect the JSON format only. */
    public static Diagram readFromFile(String fname)
        throws Exception
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
    
    /** Read a diagram from a file and return the new Diagram object.
      * This will auto-detect the ER or JSON file formats and read
      * the file appropriately. */
    public static Diagram readFromFileAutodetect(String fname) 
        throws Exception
    {
        // For compatibility with the C++ implementation, first attempt
        // to read it in the ER format.
        Diagram d = readFromERFile(fname);
        if (d != null) {
            return d;
        }
        else {
            // The file is not in the ER format.  Proceed with reading
            // it as JSON.  Exceptions will propagate out of
            // this method, as they indicate that the file *was* in the
            // ER format but some other problem occurred (or the file
            // could not be read at all, which is a problem no matter
            // what format we think the file is).
        }

        return readFromFile(fname);
    }

    // ------------------ legacy deserialization -------------------------
    /** Read a Diagram from an ER file and return the new Diagram object.
      * Return null if the file is not in the ER format; throw an
      * exception for all other problems. */
    public static Diagram readFromERFile(String fname)
        throws XParse, IOException
    {
        InputStream is = null;
        try {
            is = new FileInputStream(fname);
            return readFromERStream(is);
        }
        finally {
            if (is != null) {
                is.close();
            }
        }
    }
    
    /** Read a Diagram from an ER InputStream.  This will return null in
      * the one specific case where the file exists and is readable, but
      * the magic number is not present, meaning the file is probably
      * not in the ER format at all. */
    public static Diagram readFromERStream(InputStream is)
        throws XParse, IOException
    {
        FlattenInputStream flat = new FlattenInputStream(is);
        
        // Magic number identifier for the file format.
        int magic = flat.readInt();
        if (magic != 0x2B044C63) {
            // The file is not in the expected format.
            return null;
        }
        
        // File format version number.
        int ver = flat.readInt();
        if (!( 1 <= ver && ver <= 8 )) {
            throw new XParse("ER file format version is "+ver+
                             " but I only know how to read 1 through 8.");
        }
        flat.version = ver;
        
        return new Diagram(flat);
    }
    
    /** Read a Diagram from an ER FlattenInputStream */
    public Diagram(FlattenInputStream flat)
        throws XParse, IOException
    {
        // Defaults if file does not specify.
        this.drawFileName = true;
        this.entities = new ArrayList<Entity>();
        this.inheritances = new ArrayList<Inheritance>();
        this.relations = new ArrayList<Relation>();
        
        if (flat.version >= 5) {
            this.windowSize = flat.readDimension();
        }
        else {
            // Default size from C++ code.
            this.windowSize = new Dimension(400, 300);
        }
        
        // Entities
        {
            int numEntities = flat.readInt();
            for (int i=0; i < numEntities; i++) {
                Entity e = new Entity(flat);
                flat.noteOwner(e);
                this.entities.add(e);
            }
        }
        
        flat.checkpoint(0x64E2C40F);

        // Inheritances
        if (flat.version >= 7) {
            int numInheritances = flat.readInt();
            for (int i=0; i < numInheritances; i++) {
                Inheritance inh = new Inheritance(flat);
                flat.noteOwner(inh);
                this.inheritances.add(inh);
            }
            
            flat.checkpoint(0x144CB789);
        }
        
        // Relations
        {
            int numRelations = flat.readInt();
            for (int i=0; i < numRelations; i++) {
                Relation r = new Relation(flat);
                this.relations.add(r);
            }
        }
        
        flat.checkpoint(0x378264D9);

        this.selfCheck();
        
        // In the ER format, I needed to add titles manually.  But in
        // Ded, that is automatic.  So, look for a title entity and
        // remove it.
        for (Entity e : this.entities) {
            if (e.loc.x == 0 && e.loc.y == 0 && 
                e.attributes.equals(" ") && 
                e.shape == EntityShape.ES_NO_SHAPE)
            {
                // Looks like a title; remove it.
                this.entities.remove(e);
                
                // Paranoia: make sure we didn't mess up the Diagram
                // by doing that.  That would happen if there were a
                // Relation connected to the title.
                try {
                    this.selfCheck();
                }
                catch (AssertionError ae) {
                    throw new RuntimeException(
                        "Oops, I broke the file by removing the title element!  "+
                        "Complain to Scott.  :)");
                }

                // Cannot keep iterating, since we just modified the
                // collection we are iterating over.
                break;
            }
        }
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
                   this.drawFileName == d.drawFileName &&
                   this.entities.equals(d.entities);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int h = 1;
        h = h*31 + this.windowSize.hashCode();
        h = h*31 + (this.drawFileName? 1 : 0);
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
