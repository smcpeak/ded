// RelationEndpoint.java

package ded.model;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import util.FlattenInputStream;
import util.XParse;
import util.awt.AWTJSONUtil;

/** Start or end point of a relation (arrow). */
public class RelationEndpoint {
    // ------------- instance data ---------------
    /** If this is not null, then the endpoint is an Entity. */
    public Entity entity;
    
    /** Otherwise, if this is not null, then the endpoint is an Inheritance. */
    public Inheritance inheritance;
    
    /** Otherwise, endpoint is an arbitrary point in space. */
    public Point pt;
    
    // -------------- methods ---------------
    public RelationEndpoint(Entity e)
    {
        this.entity = e;
        this.inheritance = null;
        this.pt = null;
    }
    
    public RelationEndpoint(Inheritance i)
    {
        this.entity = null;
        this.inheritance = i;
        this.pt = null;
    }

    public RelationEndpoint(Point p)
    {
        this.entity = null;
        this.inheritance = null;
        this.pt = p;
    }

    /** True if the endpoint is an Entity. */
    public boolean isEntity()
    {
        return this.entity != null;
    }
    
    /** True if the endpoint is a specific entity as compared by
      * reference equality. */
    public boolean isSpecificEntity(Entity e)
    {
        return this.entity == e;
    }
    
    /** True if the endpoint is an inheritance. */
    public boolean isInheritance()
    {
        return this.inheritance != null;
    }
    
    /** True if the endpoint is a specific inheritance. */
    public boolean isSpecificInheritance(Inheritance inh)
    {
        return this.inheritance == inh;
    }
    
    /** True if the endpoint is an arbitrary point. */
    public boolean isPoint()
    {
        return this.pt != null;
    }

    /** Return the center of the entity or inheritance, or the point. */
    public Point getCenter()
    {
        if (this.isEntity()) {
            return this.entity.getCenter();
        }
        else if (this.isInheritance()) {
            return this.inheritance.pt;
        }
        else {
            return this.pt;
        }
    }
    
    public void globalSelfCheck(Diagram d)
    {
        assert((this.entity==null?0:1) +
               (this.inheritance==null?0:1) +
               (this.pt==null?0:1) 
                   == 1);
        
        if (this.isEntity()) {
            assert(d.entities.contains(this.entity));
        }
        if (this.isInheritance()) {
            assert(d.inheritances.contains(this.inheritance));
        }
    }

    // -------------------- data object boilerplate ------------------------
    public RelationEndpoint(RelationEndpoint re)
    {
        this.setTo(re);
    }
    
    public void setTo(RelationEndpoint re)
    {
        this.entity = re.entity;
        this.inheritance = re.inheritance;
        if (re.pt == null) {
            this.pt = null;
        }
        else {
            this.pt = new Point(re.pt);
        }
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass()) {
            RelationEndpoint re = (RelationEndpoint)obj;
            if (this.isEntity()) {
                return this.entity.equals(re.entity);
            }
            else if (this.isInheritance()) {
                return this.inheritance.equals(re.inheritance);
            }
            else {
                return this.pt.equals(re.pt);
            }
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        if (this.isEntity()) {
            return 1 + 31 * this.entity.hashCode();
        }
        else if (this.isInheritance()) {
            return 2 + 31 * this.inheritance.hashCode();
        }
        else {
            return 3 + 31 * this.pt.hashCode();
        }
    }
   
    // ------------------- serialization -----------------------
    public JSONObject toJSON(HashMap<Entity, Integer> entityToInteger,
                             HashMap<Inheritance, Integer> inheritanceToInteger)
    {
        JSONObject o = new JSONObject();
        try {
            if (this.entity != null) {
                o.put("entityRef", this.entity.toJSONRef(entityToInteger));
            }
            else if (this.inheritance != null) {
                o.put("inheritanceRef", this.inheritance.toJSONRef(inheritanceToInteger));
            }
            else {
                o.put("pt", AWTJSONUtil.pointToJSON(this.pt));
            }
        }
        catch (JSONException e) { assert(false); }
        return o;
    }
    
    public RelationEndpoint(
        JSONObject o, 
        ArrayList<Entity> integerToEntity,
        ArrayList<Inheritance> integerToInheritance)
        throws JSONException
    {
        this.entity = null;
        this.inheritance = null;
        this.pt = null;

        if (o.has("entityRef")) {
            this.entity = Entity.fromJSONRef(integerToEntity, o.getLong("entityRef"));
            return;
        }

        if (o.has("inheritanceRef")) {
            this.inheritance = Inheritance.fromJSONRef(integerToInheritance, 
                                                       o.getLong("inheritanceRef"));
            return;
        }

        this.pt = AWTJSONUtil.pointFromJSON(o.getJSONObject("pt"));
    }
    
    // ------------------ legacy serialization ------------------
    /** Read a RelationEndpoint from an ER FlattenInputStream. */
    public RelationEndpoint(FlattenInputStream flat)
        throws XParse, IOException
    {
        this.entity = null;
        this.inheritance = null;
        this.pt = null;
        
        // entity
        Object ent = flat.readSerf();
        if (ent != null) {
            if (ent instanceof Entity) {
                this.entity = (Entity)ent;
            }
            else {
                throw new XParse("RelationEndpoint.entity: expected an Entity");
            }
        }
        
        if (flat.version >= 7) {
            Object inh = flat.readSerf();
            if (inh != null) {
                if (inh instanceof Inheritance) {
                    this.inheritance = (Inheritance)inh;
                }
                else {
                    throw new XParse("RelationEndpoint.inheritance: expected an Inheritance");
                }
            }
        }
        
        // The C++ serialization code contains a bug: it serializes
        // 'pt' whenever 'entity' is NULL, ignoring the fact that
        // a non-NULL 'inheritance' makes it irrelevant.  So, I will
        // read a Point in the same circumstance, but only use it if
        // there is also no inheritance.
        if (this.entity == null) {
            Point p = flat.readPoint();
            
            if (this.inheritance == null) {
                this.pt = p;
            }
        }
    }
}

// EOF
