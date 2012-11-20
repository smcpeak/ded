// Entity.java

package ded.model;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import util.AWTJSONUtil;
import util.json.JSONable;

/** An ER entity, represented as a box with a label and text contents. */
public class Entity implements JSONable {
    // ------------ public data ------------
    /** Location of upper-left corner, in pixels. */
    public Point loc;
    
    /** Size in pixels. */
    public Dimension size;
    
    /** Shape of the outline (or indication of its absence). */
    public EntityShape shape;
    
    /** Name/title of the entity. */
    public String name;
    
    /** Attributes as free text with newlines. */
    public String attributes;
    
    // ------------ public methods ------------
    public Entity()
    {
        this.loc = new Point(0,0);
        this.size = new Dimension(100, 50);
        this.shape = EntityShape.ES_RECTANGLE;
        this.name = "";
        this.attributes = "";
    }

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
    
    // ------------ serialization ------------
    @Override
    public JSONObject toJSON()
    {
        JSONObject o = new JSONObject();
        try {
            o.put("loc", AWTJSONUtil.pointToJSON(this.loc));
            o.put("size", AWTJSONUtil.dimensionToJSON(this.size));
            o.put("shape", this.shape.name());
            o.put("name", this.name);
            o.put("attributes", this.attributes);
        }
        catch (JSONException e) { assert(false); }
        return o;
    }
    
    public Entity(JSONObject o) throws JSONException
    {
        this.loc = AWTJSONUtil.pointFromJSON(o.getJSONObject("loc"));
        this.size = AWTJSONUtil.dimensionFromJSON(o.getJSONObject("size"));
        this.shape = EntityShape.valueOf(EntityShape.class, o.getString("shape"));
        this.name = o.getString("name");
        this.attributes = o.getString("attributes");
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
                   this.name.equals(e.name) &&
                   this.attributes.equals(e.attributes);
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
        h = h*31 + this.name.hashCode();
        h = h*31 + this.attributes.hashCode();
        return h;
    }
    
    @Override
    public String toString()
    {
        return toJSON().toString();
    }
}

// EOF
