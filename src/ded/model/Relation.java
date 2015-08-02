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
    // ------------------- constants --------------------------
    /** Default routing algorithm.  Used to save space in serialized form. */
    public static final RoutingAlgorithm defaultRoutingAlgorithm =
        RoutingAlgorithm.RA_MANHATTAN_HORIZ;

    /** Default line color when none is specified in file. */
    public static final String defaultLineColor = "Black";

    /** Default text color when none is specified in file. */
    public static final String defaultTextColor = "Black";

    // ------------------ instance data -----------------------
    /** Endpoints, including their arrow style. */
    public RelationEndpoint start, end;

    /** Intermediate control points, if any. */
    public ArrayList<Point> controlPts = new ArrayList<Point>();

    /** Routing algorithm for displaying relation onscreen. */
    public RoutingAlgorithm routingAlg = defaultRoutingAlgorithm;

    /** Text label for the relation. */
    public String label = "";

    /** Optional line width.  If null, use default, which depends
      * on whether this is an inheritance edge. */
    public Integer lineWidth = null;

    /** Color of the line. */
    public String lineColor = defaultLineColor;

    /** Color of the text label. */
    public String textColor = defaultTextColor;

    /** Dash structure of the line.  When empty, the line is solid.
      * Otherwise, this is a sequence of lengths, in pixels, of
      * alternating segments of opaque and transparent
      * segments, starting with an opaque segment.  All elements must
      * be non-negative, and at least one must be positive.  Once
      * the sequence is exhausted, it repeats, starting again with
      * an opaque segment. */
    public ArrayList<Integer> dashStructure = new ArrayList<Integer>();

    // -------------------- methods ----------------------
    public Relation(RelationEndpoint start, RelationEndpoint end)
    {
        this.start = start;
        this.end = end;
    }

    /** Deep copy constructor, except for supplied endpoints. */
    public Relation(Relation obj, RelationEndpoint start, RelationEndpoint end)
    {
        this.start = start;
        this.end = end;

        for (Point pt : obj.controlPts) {
            this.controlPts.add(new Point(pt));
        }

        this.routingAlg = obj.routingAlg;
        this.label = obj.label;
        this.lineWidth = obj.lineWidth;
        this.lineColor = obj.lineColor;
        this.textColor = obj.textColor;
        this.dashStructure = new ArrayList<Integer>(obj.dashStructure);
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

    /** Set 'end.arrowStyle' based on the value of the legacy 'owning' value. */
    private void setLegacyOwning(boolean owning)
    {
        this.end.arrowStyle =
            (owning? ArrowStyle.AS_DOUBLE_ANGLE : ArrowStyle.AS_FILLED_TRIANGLE);
    }

    /** Swap the arrowheads between start and end. */
    public void swapArrows()
    {
        ArrowStyle tmp = this.start.arrowStyle;
        this.start.arrowStyle = this.end.arrowStyle;
        this.end.arrowStyle = tmp;
    }

    public void globalSelfCheck(Diagram d)
    {
        this.start.globalSelfCheck(d);
        this.end.globalSelfCheck(d);

        if (!this.dashStructure.isEmpty()) {
            int numPositive = 0;
            for (Integer i : this.dashStructure) {
                assert(i >= 0);
                if (i > 0) {
                    numPositive++;
                }
            }
            assert(numPositive > 0);
        }
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
                   Util.nullableEquals(this.lineWidth, r.lineWidth) &&
                   this.lineColor.equals(r.lineColor) &&
                   this.textColor.equals(r.textColor) &&
                   this.dashStructure.equals(r.dashStructure);
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
        h = h*31 + Util.nullableHashCode(this.lineWidth);
        h = h*31 + this.lineColor.hashCode();
        h = h*31 + this.textColor.hashCode();
        h = h*31 + Util.collectionHashCode(this.dashStructure);
        return h;
    }

    // -------------------- serialization ------------------------
    public JSONObject toJSON(HashMap<Entity, Integer> entityToInteger,
                             HashMap<Inheritance, Integer> inheritanceToInteger)
    {
        JSONObject o = new JSONObject();
        try {
            o.put("start", this.start.toJSON(entityToInteger,
                inheritanceToInteger, ArrowStyle.AS_NONE));
            o.put("end", this.end.toJSON(entityToInteger,
                inheritanceToInteger, ArrowStyle.AS_FILLED_TRIANGLE));

            if (!this.controlPts.isEmpty()) {
                JSONArray pts = new JSONArray();
                for (Point p : this.controlPts) {
                    pts.put(AWTJSONUtil.pointToJSON(p));
                }
                o.put("controlPts", pts);
            }

            if (this.routingAlg != defaultRoutingAlgorithm) {
                o.put("routingAlg", this.routingAlg.name());
            }

            if (!this.label.isEmpty()) {
                o.put("label", this.label);
            }

            if (this.lineWidth != null) {
                o.put("lineWidth", this.lineWidth.intValue());
            }

            if (!this.lineColor.equals(defaultLineColor)) {
                o.put("lineColor", this.lineColor);
            }

            if (!this.dashStructure.isEmpty()) {
                JSONArray lengths = new JSONArray();
                for (Integer i : this.dashStructure) {
                    lengths.put(i);
                }
                o.put("dashStructure", lengths);
            }

            if (!this.textColor.equals(defaultTextColor)) {
                o.put("textColor", this.textColor);
            }
        }
        catch (JSONException e) { assert(false); }
        return o;
    }

    public Relation(
        JSONObject o,
        ArrayList<Entity> integerToEntity,
        ArrayList<Inheritance> integerToInheritance,
        int version)
        throws JSONException
    {
        this.start = new RelationEndpoint(o.getJSONObject("start"),
            integerToEntity, integerToInheritance, ArrowStyle.AS_NONE, version);
        this.end = new RelationEndpoint(o.getJSONObject("end"),
            integerToEntity, integerToInheritance, ArrowStyle.AS_FILLED_TRIANGLE, version);

        JSONArray pts = o.optJSONArray("controlPts");
        if (pts != null) {
            for (int i=0; i < pts.length(); i++) {
                this.controlPts.add(AWTJSONUtil.pointFromJSON(pts.getJSONObject(i)));
            }
        }

        if (o.has("routingAlg")) {
            this.routingAlg = RoutingAlgorithm.valueOf(RoutingAlgorithm.class,
                                                       o.getString("routingAlg"));
        }

        this.label = o.optString("label", "");

        if (version < 9) {
            // The end arrowhead style was associated with the relation itself.
            this.setLegacyOwning(o.optBoolean("owning", false));
        }

        if (o.has("lineWidth")) {
            this.lineWidth = Integer.valueOf(o.getInt("lineWidth"));
        }

        if (o.has("lineColor")) {
            this.lineColor = o.getString("lineColor");
        }

        if (o.has("dashStructure")) {
            JSONArray lengths = o.getJSONArray("dashStructure");
            for (int i=0; i < lengths.length(); i++) {
                this.dashStructure.add(lengths.getInt(i));
            }
        }

        if (o.has("textColor")) {
            this.textColor = o.getString("textColor");
        }
    }

    // ------------------ legacy serialization -----------------
    /** Read a Relation from an ER FlattenInputStream. */
    public Relation(FlattenInputStream flat)
        throws XParse, IOException
    {
        this.start = new RelationEndpoint(flat);
        this.end = new RelationEndpoint(flat);
        this.label = flat.readString();
        this.setLegacyOwning(false);

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

        this.setLegacyOwning(flat.readBoolean());
    }
}

// EOF
