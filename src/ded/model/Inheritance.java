// Inheritance.java
// See toplevel license.txt for copyright and license terms.

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

/** Node that indicates an inheritance relationship; it directly points
  * at the parent Entity, and the children are then attached via
  * Relations. */
public class Inheritance {
    // --------------------- instance data ------------------------
    /** Super-entity (generalization) of the related child
      * (specialization) Entities. */
    public Entity parent;

    /** True if the inheritance is "open", meaning the parent can
      * be instantiated without instantiating any child. */
    public boolean open;

    /** Location of the inheritance node. */
    public Point pt;

    // ----------------------- methods ----------------------------
    public Inheritance(Entity parent, boolean open, Point pt)
    {
        this.parent = parent;
        this.open = open;
        this.pt = pt;
    }

    /** Deep copy constructor, except for parent entity. */
    public Inheritance(Inheritance obj, Entity parent)
    {
        this.parent = parent;
        this.open = obj.open;
        this.pt = new Point(obj.pt);
    }

    public void globalSelfCheck(Diagram d)
    {
        assert(d.entities.contains(this.parent));
    }

    // ------------------ data object boilerplate ----------------
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass()) {
            Inheritance inh = (Inheritance)obj;
            return this.parent.equals(inh.parent) &&
                   this.open == inh.open &&
                   this.pt.equals(inh.pt);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int h = 1;
        h = h*31 + this.parent.hashCode();
        h = h*31 + (this.open?1:0);
        h = h*31 + this.pt.hashCode();
        return h;
    }

    // ------------------- serialization -------------------
    public JSONObject toJSON(HashMap<Entity, Integer> entityToInteger)
    {
        JSONObject o = new JSONObject();

        try {
            Integer parentIndex = entityToInteger.get(this.parent);
            if (parentIndex == null) {
                throw new RuntimeException("internal error: entityToInteger mapping not found");
            }
            o.put("parentRef", parentIndex.intValue());

            o.put("open", this.open);
            o.put("pt", AWTJSONUtil.pointToJSON(this.pt));
        }
        catch (JSONException e) { assert(false); }

        return o;
    }

    public Inheritance(JSONObject o, ArrayList<Entity> integerToEntity) throws JSONException
    {
        this.parent = Entity.fromJSONRef(integerToEntity, o.getLong("parentRef"));
        this.open = o.getBoolean("open");
        this.pt = AWTJSONUtil.pointFromJSON(o.getJSONObject("pt"));
    }

    /** Return the value to which 'this' is mapped in 'inheritanceToInteger'. */
    public int toJSONRef(HashMap<Inheritance, Integer> inheritanceToInteger)
    {
        Integer index = inheritanceToInteger.get(this);
        if (index == null) {
            throw new RuntimeException("internal error: inheritanceToInteger mapping not found");
        }
        return index.intValue();
    }

    /** Return the value to which 'index' is mapped in 'integerToInheritance'. */
    public static Inheritance fromJSONRef(ArrayList<Inheritance> integerToInheritance, long index)
        throws JSONException
    {
        if (0 <= index && index < integerToInheritance.size()) {
            return integerToInheritance.get((int)index);
        }
        else {
            throw new JSONException("invalid entity ref "+index);
        }
    }

    // ------------------- legacy serialization -------------------
    /** Read an Inheritance from an ER FlattenInputStream. */
    public Inheritance(FlattenInputStream flat)
        throws XParse, IOException
    {
        Object par = flat.readSerf();
        if (par instanceof Entity) {
            this.parent = (Entity)par;
        }
        else {
            throw new XParse("Inheritance.parent: expected Entity");
        }

        this.open = flat.readBoolean();
        this.pt = flat.readPoint();
    }
}

// EOF
