// Entity.java
// See toplevel license.txt for copyright and license terms.

package ded.model;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import util.FlattenInputStream;
import util.XParse;
import util.awt.AWTJSONUtil;
import util.json.JSONable;

/** An ER entity, represented as a box with a label and text contents. */
public class Entity implements JSONable {
    // ------------ constants -------------
    /** Default shape. */
    public static final EntityShape defaultShape = EntityShape.ES_RECTANGLE;
    
    /** Default entity fill color.  Assumed when no color is
      * specified in the input file. */
    public static final String defaultFillColor = "Gray";
    
    // ------------ public data ------------
    /** Location of upper-left corner, in pixels. */
    public Point loc;
    
    /** Size in pixels. */
    public Dimension size;
    
    /** Shape of the outline (or indication of its absence). */
    public EntityShape shape = defaultShape;
    
    /** Name of fill color.  For the moment, this must be one of
      * a fixed set, but I plan on making it customizable. */
    public String fillColor = defaultFillColor;
    
    /** Name/title of the entity. */
    public String name = "";
    
    /** Attributes as free text with newlines. */
    public String attributes = "";
    
    /** Additional shape-specific geometry parameters.  May be null. */
    public int[] shapeParams = null;
    
    // ------------ public methods ------------
    public Entity()
    {
        this.loc = new Point(0,0);
        this.size = new Dimension(100, 50);
    }

    /** Return the primary bounding rectangle for this entity, used for
      * drawing the selection box and hit testing.  Some shapes might
      * extend a little outside this box visually. */
    public Rectangle getRect()
    {
        return new Rectangle(this.loc, this.size);
    }
    
    /** Return the point at the center of the Entity's bounding box. */
    public Point getCenter()
    {
        return new Point(this.loc.x + this.size.width/2, 
                         this.loc.y + this.size.height/2);
    }
    
    /** Set 'fillColor'.  For the moment, that is all. */
    public void setFillColor(String colorName)
    {
        this.fillColor = colorName;
    }
    
    /** Set 'shape'.  Adjust 'shapeParams' to match if needed. */
    public void setShape(EntityShape shape)
    {
        if (this.shape != shape) {
            this.shape = shape;
            
            if (shape.numParams == 0) {
                this.shapeParams = null;
            }
            else {
                this.shapeParams = new int[shape.numParams];
                for (int i=0; i < shape.numParams; i++) {
                    this.shapeParams[i] = 5 + 5*i;
                }
            }
        }
    }
    
    // ------------ serialization ------------
    @Override
    public JSONObject toJSON()
    {
        JSONObject o = new JSONObject();
        try {
            o.put("loc", AWTJSONUtil.pointToJSON(this.loc));
            o.put("size", AWTJSONUtil.dimensionToJSON(this.size));
            
            if (this.shape != defaultShape) {
                o.put("shape", this.shape.name());
            }
            
            if (!this.name.isEmpty()) {
                o.put("name", this.name);
            }
            
            if (!this.attributes.isEmpty()) {
                o.put("attributes", this.attributes);
            }
            
            if (this.shapeParams != null) {
                o.put("shapeParams", new JSONArray(this.shapeParams));
            }
            
            if (!this.fillColor.equals(defaultFillColor)) {
                o.put("fillColor", this.fillColor);
            }
        }
        catch (JSONException e) { assert(false); }
        return o;
    }
    
    public Entity(JSONObject o, int ver) throws JSONException
    {
        this.loc = AWTJSONUtil.pointFromJSON(o.getJSONObject("loc"));
        this.size = AWTJSONUtil.dimensionFromJSON(o.getJSONObject("size"));
        
        if (o.has("shape")) {
            this.shape = EntityShape.valueOf(EntityShape.class, o.getString("shape"));
        }
        
        this.name = o.optString("name", "");
        this.attributes = o.optString("attributes", "");
        
        JSONArray params = o.optJSONArray("shapeParams");
        if (params != null) {
            this.shapeParams = new int[params.length()];
            for (int i=0; i < params.length(); i++) {
                this.shapeParams[i] = params.getInt(i);
            }
        }
        
        if (ver >= 5) {
            this.fillColor = o.optString("fillColor", defaultFillColor);
        }
    }
    
    /** Return the value to which 'this' is mapped in 'entityToInteger'. */
    public int toJSONRef(HashMap<Entity, Integer> entityToInteger)
    {
        Integer index = entityToInteger.get(this);
        if (index == null) {
            throw new RuntimeException("internal error: entityToInteger mapping not found");
        }
        return index.intValue();
    }

    /** Return the value to which 'index' is mapped in 'integerToEntity'. */
    public static Entity fromJSONRef(ArrayList<Entity> integerToEntity, long index)
        throws JSONException
    {
        if (0 <= index && index < integerToEntity.size()) {
            return integerToEntity.get((int)index);
        }
        else {
            throw new JSONException("invalid entity ref "+index);
        }
    }

    // -------------- legacy serialization ---------------
    /** Construct an Entity by reading an ER FlattenInputStream. */
    public Entity(FlattenInputStream flat)
        throws XParse, IOException
    {
        this.loc = flat.readPoint();
        this.size = flat.readDimension();
        this.name = flat.readString();
        
        if (flat.version < 2) { return; }
        
        this.attributes = flat.readString();
        
        if (flat.version < 6) { return; }
        
        int s = flat.readInt();
        switch (s) {
            case 0: this.shape = EntityShape.ES_NO_SHAPE; break;
            case 1: this.shape = EntityShape.ES_RECTANGLE; break;
            case 2: this.shape = EntityShape.ES_ELLIPSE; break;
            default:
                throw new XParse("unrecognized entity shape code: "+s);
        }
    }
    
    // ------------- data object boilerplate -------------
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass()) {
            Entity e = (Entity)obj;
            return this.loc.equals(e.loc) &&
                   this.size.equals(e.size) &&
                   this.shape.equals(e.shape) &&
                   this.fillColor.equals(e.fillColor) &&
                   this.name.equals(e.name) &&
                   this.attributes.equals(e.attributes) &&
                   Arrays.equals(this.shapeParams, e.shapeParams);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int h = 1;
        h = h*31 + this.loc.hashCode();
        h = h*31 + this.size.hashCode();
        h = h*31 + this.shape.hashCode();
        h = h*31 + this.fillColor.hashCode();
        h = h*31 + this.name.hashCode();
        h = h*31 + this.attributes.hashCode();
        h = h*31 + Arrays.hashCode(this.shapeParams);
        return h;
    }
    
    @Override
    public String toString()
    {
        return toJSON().toString();
    }
}

// EOF
