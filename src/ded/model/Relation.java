// Relation.java
// See toplevel license.txt for copyright and license terms.

package ded.model;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import util.FlattenInputStream;
import util.Util;
import util.XParse;
import util.awt.AWTJSONUtil;

/** Arrow, sometimes between Entities (boxes). */
public class Relation {
    // ------------------ instance data -----------------------
    /** Endpoints. */
    public RelationEndpoint start, end;
    
    /** Intermediate control points, if any. */
    public ArrayList<Point> controlPts;
    
    /** Routing algorithm for displaying relation onscreen. */
    public RoutingAlgorithm routingAlg;
    
    /** Text label for the relation. */
    public String label;
    
    /** True for an "owning" relation, drawn with the double arrowhead;
      * false for an ordinary or "shared" relation.  Initially false. */
    public boolean owning;
    
    // -------------------- methods ----------------------
    public Relation(RelationEndpoint start, RelationEndpoint end)
    {
        this.start = start;
        this.end = end;
        this.controlPts = new ArrayList<Point>();
        this.routingAlg = RoutingAlgorithm.RA_MANHATTAN_HORIZ;
        this.label = "";
        this.owning = false;
    }
    
    /** True if either endpoint is referentially equal to 'e'. */
    public boolean involvesEntity(Entity e)
    {
        return this.start.isSpecificEntity(e) || 
               this.end.isSpecificEntity(e);
    }
    
    /** True if either endpoint is referentially equal to 'inh'. */
    public boolean involvesInheritance(Inheritance inh)
    {
        return this.start.isSpecificInheritance(inh) ||
               this.end.isSpecificInheritance(inh);
    }
    
    public void globalSelfCheck(Diagram d)
    {
        this.start.globalSelfCheck(d);
        this.end.globalSelfCheck(d);
    }
    
    // -------------------- data object boilerplate --------------------
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass()) {
            Relation r = (Relation)obj;
            return this.start.equals(r.start) &&
                   this.end.equals(r.end) &&
                   this.controlPts.equals(r.controlPts) &&
                   this.routingAlg.equals(r.routingAlg) &&
                   this.label.equals(r.label) &&
                   this.owning == r.owning;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int h = 1;
        h = h*31 + this.start.hashCode();
        h = h*31 + this.end.hashCode();
        h = h*31 + Util.collectionHashCode(this.controlPts);
        h = h*31 + this.routingAlg.hashCode();
        h = h*31 + this.label.hashCode();
        h = h*31 + (this.owning?1:0);
        return h;
    }

    // -------------------- serialization ------------------------
    public JSONObject toJSON(HashMap<Entity, Integer> entityToInteger,
                             HashMap<Inheritance, Integer> inheritanceToInteger)
    {
        JSONObject o = new JSONObject();
        try {
            o.put("start", this.start.toJSON(entityToInteger, inheritanceToInteger));
            o.put("end", this.end.toJSON(entityToInteger, inheritanceToInteger));
            
            JSONArray pts = new JSONArray();
            for (Point p : this.controlPts) {
                pts.put(AWTJSONUtil.pointToJSON(p));
            }
            o.put("controlPts", pts);
            
            o.put("routingAlg", this.routingAlg.name());
            o.put("label", this.label);
            o.put("owning", this.owning);
        }
        catch (JSONException e) { assert(false); }
        return o;
    }
    
    public Relation(
        JSONObject o,
        ArrayList<Entity> integerToEntity,
        ArrayList<Inheritance> integerToInheritance)
        throws JSONException
    {
        this.start = new RelationEndpoint(o.getJSONObject("start"), 
                                          integerToEntity, integerToInheritance);
        this.end = new RelationEndpoint(o.getJSONObject("end"), 
                                        integerToEntity, integerToInheritance);
        
        this.controlPts = new ArrayList<Point>();
        JSONArray pts = o.getJSONArray("controlPts");
        for (int i=0; i < pts.length(); i++) {
            this.controlPts.add(AWTJSONUtil.pointFromJSON(pts.getJSONObject(i)));
        }
        
        this.routingAlg = RoutingAlgorithm.valueOf(RoutingAlgorithm.class,
                                                   o.getString("routingAlg"));
        this.label = o.getString("label");
        this.owning = o.getBoolean("owning");
    }
    
    // ------------------ legacy serialization -----------------
    /** Read a Relation from an ER FlattenInputStream. */
    public Relation(FlattenInputStream flat)
        throws XParse, IOException
    {
        // Defaults/initials, related to if file does not specify.
        this.routingAlg = RoutingAlgorithm.RA_MANHATTAN_HORIZ;
        this.controlPts = new ArrayList<Point>();
        this.owning = false;
        
        this.start = new RelationEndpoint(flat);
        this.end = new RelationEndpoint(flat);
        this.label = flat.readString();
        
        if (flat.version < 3) { return; }
        
        // routingAlg
        int r = flat.readInt();
        switch (r) {
            case 0: this.routingAlg = RoutingAlgorithm.RA_DIRECT; break;
            case 1: this.routingAlg = RoutingAlgorithm.RA_MANHATTAN_HORIZ; break;
            case 2: this.routingAlg = RoutingAlgorithm.RA_MANHATTAN_VERT; break;
            default:
                throw new XParse("invalid routingAlg code: "+r);
        }
        
        if (flat.version < 4) { return; }
        
        // controlPts
        {
            int numControlPts = flat.readInt();
            for (int i=0; i < numControlPts; i++) {
                this.controlPts.add(flat.readPoint());
            }
        }

        if (flat.version < 8) { return; }
        
        this.owning = flat.readBoolean();
    }
}

// EOF
