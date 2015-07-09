// Diagram.java
// See toplevel license.txt for copyright and license terms.

package ded.model;

import java.awt.Color;
import java.awt.Dimension;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import util.FlattenInputStream;
import util.StringUtil;
import util.Util;
import util.XParse;
import util.awt.AWTJSONUtil;
import util.json.JSONable;

/** Complete diagram. */
public class Diagram implements JSONable {
    // ---------- constants ------------
    /** Value of the "type" attribute in toplevel JSON object.  This
      * should never be changed. */
    public static final String jsonType = "Diagram Editor Diagram";

    /** Value to write as the "version" attribute in toplevel JSON
      * object, and maximum value we can read there.  This should
      * be bumped every time something is added or changed in the
      * file format that would cause an older version of this code
      * to be unable to read the current files.  That includes
      * adding new enumerators to existing enumerations; although
      * the serialization code does not literally change, its
      * behavior does, because it then reads and writes a new
      * string value for that enumerator.  Even adding a new field
      * should include a bump--even though the old code might be
      * able to read the file without choking, the semantics would
      * not be preserved. */
    public static final int currentFileVersion = 18;

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

    /** Map from color names to Colors. */
    public LinkedHashMap<String, Color> namedColors;

    // ----------- public methods -----------
    public Diagram()
    {
        this.windowSize = new Dimension(800, 800);
        this.drawFileName = true;
        this.entities = new ArrayList<Entity>();
        this.inheritances = new ArrayList<Inheritance>();
        this.relations = new ArrayList<Relation>();
        this.namedColors = makeDefaultColors();
    }

    public static LinkedHashMap<String, Color> makeDefaultColors()
    {
        LinkedHashMap<String, Color> m = new LinkedHashMap<String, Color>();

        // These colors are non-standard.  I chose them manually,
        // based on factors like readability, garishness and
        // ability to differentiate from each other.  One of them
        // is the same as the selection color, which introduces
        // some ambiguity, but it is a really nice color so I do
        // not want to lose it in either place.
        m.put("Black", Color.BLACK);
        m.put("Gray", new Color(192, 192, 192));
        m.put("White", Color.WHITE);
        m.put("Light Gray", new Color(224, 224, 224));
        m.put("Orange", new Color(236, 125, 70));
        m.put("Yellow", new Color(234, 236, 52));
        m.put("Green", new Color(76, 222, 76));
        m.put("Sky Blue", new Color(135, 193, 255));  // Selection color.
        m.put("Purple", new Color(161, 140, 237));
        m.put("Pink", new Color(227, 120, 236));
        m.put("Red", new Color(248, 50, 50));         // Very intense...

        return m;
    }

    /** Given a color name, get a Color object.  Return 'fallback' if the
      * name cannot be found or interpreted. */
    public Color getNamedColor(String namedColor, Color fallback)
    {
        Color c = this.namedColors.get(namedColor);
        if (c != null) {
            return c;
        }
        else {
            // Check for the RGB syntax.
            String[] elts = StringUtil.parseByRegex(namedColor, "^RGB\\(([0-9]+),([0-9]+),([0-9]+)\\)$");
            if (elts != null) {
                try {
                    int r = Integer.valueOf(elts[1]);
                    int g = Integer.valueOf(elts[2]);
                    int b = Integer.valueOf(elts[3]);
                    return new Color(r, g, b);
                }
                catch (Exception e) {
                    return fallback;
                }
            }

            return fallback;
        }
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
            o.put("version", currentFileVersion);

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
        if (ver < 1) {
            throw new JSONException(
                "Invalid file version: "+ver+".  Valid version "+
                "numbers are and will always be positive.");
        }
        else if (ver > currentFileVersion) {
            throw new JSONException(
                "The file has version "+ver+
                " but the largest version this program is capable of "+
                "reading is "+currentFileVersion+".  You need to get "+
                "a later version of the program in order to read "+
                "this file.");
        }

        this.windowSize = AWTJSONUtil.dimensionFromJSON(o.getJSONObject("windowSize"));
        this.namedColors = makeDefaultColors();

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
            Entity e = new Entity(a.getJSONObject(i), ver);
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
                    new Relation(a.getJSONObject(i), integerToEntity, integerToInheritance, ver);
                this.relations.add(rel);
            }
        }
    }

    /** Write this diagram to the specified file. */
    public void saveToFile(String fname) throws Exception
    {
        JSONObject serialized = this.toJSON();
        FileOutputStream fos = new FileOutputStream(fname);
        try {
            Writer w = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
            try {
                serialized.write(w, 2, 0);
                w.append('\n');
            }
            finally {
                w.close();
                fos = null;
            }
        }
        finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    /** Read a Diagram from a file, expect the JSON format only. */
    public static Diagram readFromFile(String fname)
        throws Exception
    {
        FileInputStream fis = new FileInputStream(fname);
        try {
            Reader r = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            try {
                return readFromReader(r);
            }
            finally {
                r.close();
                fis = null;
            }
        }
        finally {
            if (fis != null) {
                fis.close();
            }
        }
    }

    /** Read Diagram JSON out of 'r'. */
    public static Diagram readFromReader(Reader r)
        throws Exception
    {
        // Parse the raw characters into a JSON tree.
        JSONObject obj = new JSONObject(new JSONTokener(r));

        // Parse the JSON tree into a Diagram object graph.
        return new Diagram(obj);
    }

    /** Serialize as a JSON string. */
    public String toJSONString()
    {
        return this.toJSON().toString();
    }

    /** Deserialize a JSON string; may throw JSONException. */
    public static Diagram parseJSONString(String json)
        throws JSONException
    {
        return new Diagram(new JSONObject(new JSONTokener(new StringReader(json))));
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
        this.namedColors = makeDefaultColors();

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
                   this.entities.equals(d.entities) &&
                   this.inheritances.equals(d.inheritances) &&
                   this.relations.equals(d.relations) &&
                   this.namedColors.equals(d.namedColors);
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
        h = h*31 + Util.collectionHashCode(this.inheritances);
        h = h*31 + Util.collectionHashCode(this.relations);
        h = h*31 + this.namedColors.hashCode();
        return h;
    }

    @Override
    public String toString()
    {
        return this.toJSONString();
    }
}

// EOF
