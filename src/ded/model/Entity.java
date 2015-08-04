// Entity.java
// See toplevel license.txt for copyright and license terms.

package ded.model;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import util.FlattenInputStream;
import util.Util;
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

    /** Default entity line color. */
    public static final String defaultLineColor = "Black";

    /** Default entity text color. */
    public static final String defaultTextColor = "Black";

    /** Default name text alignment. */
    public static final TextAlign defaultNameAlign = TextAlign.TA_CENTER;

    /** Default image fill style. */
    public static final ImageFillStyle defaultImageFillStyle = ImageFillStyle.IFS_UPPER_LEFT;

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

    /** Name of line color. */
    public String lineColor = defaultLineColor;

    /** Name of text color. */
    public String textColor = defaultTextColor;

    /** Name/title of the entity. */
    public String name = "";

    /** How to align the name. */
    public TextAlign nameAlign = defaultNameAlign;

    /** Attributes as free text with newlines. */
    public String attributes = "";

    /** Additional shape-specific geometry parameters.  May be null. */
    public int[] shapeParams = null;

    /** Shape-specific flags. */
    public EnumSet<ShapeFlag> shapeFlags = ShapeFlag.defaultFlagsForShape(defaultShape);

    /** Name of anchor in an HTML document for a section that describes
      * this entity.  This is only used by a separate Python script,
      * 'insert-ded-image-map' that reads the 'ded' JSON format and
      * writes an HTML image map. */
    public String anchorName = "";

    /** File name of image to draw as the entity background instead of
      * 'fillColor' (which is then not used for most shapes).  When
      * rendered, if relative, the this will be interpreted as relative
      * to the directory where the containing .ded file is.  This is
      * only used if it is not empty. */
    public String imageFileName = "";

    /** When 'imageFileName' is not empty, this specifies how we fill
      * the entity rectangle with it. */
    public ImageFillStyle imageFillStyle = defaultImageFillStyle;

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
    public void setShape(EntityShape newShape)
    {
        if (this.shape != newShape) {
            this.shape = newShape;

            if (newShape.numParams == 0) {
                this.shapeParams = null;
            }
            else {
                this.shapeParams = new int[newShape.numParams];
                for (int i=0; i < newShape.numParams; i++) {
                    this.shapeParams[i] = 5 + 5*i;
                }
            }
        }
    }

    /** Set shape, and also set default values for other fields if they
      * are not already set to non-default values. */
    public void setShapeAndDefaults(EntityShape newShape)
    {
        if (this.shape != newShape) {
            EntityShape oldShape = this.shape;
            this.setShape(newShape);

            this.shapeFlags = defaultFlagsForNewShape(oldShape, this.shapeFlags, newShape);

            // If we're changing to a text edit or combo box control, and neither of
            // the colors have been changed, adjust to gray lines and
            // white fill, since that is what the rendering is tuned for.
            if ((newShape == EntityShape.ES_TEXT_EDIT || newShape == EntityShape.ES_COMBO_BOX) &&
                this.lineColor.equals(defaultLineColor) &&
                this.fillColor.equals(defaultFillColor))
            {
                this.lineColor = "Gray";
                this.fillColor = "White";
            }
        }
    }

    /** Get shape param 'i', or 0 if it is not set. */
    public int getShapeParam(int i)
    {
        if (this.shapeParams != null && this.shapeParams.length > i) {
            return this.shapeParams[i];
        }
        else {
            return 0;
        }
    }

    /** Set the shape parameters. */
    public void setShapeParams(int p, int q)
    {
        this.shapeParams = new int[2];
        this.shapeParams[0] = p;
        this.shapeParams[1] = q;
    }

    /** Restrict 'oldFlags' to those appropriate for 'newShape', and
      * add any default flags that are applicable to 'newShape' but not to
      * 'oldShape'.  Return the updated flag set. */
    public static EnumSet<ShapeFlag> defaultFlagsForNewShape(
        EntityShape oldShape,
        EnumSet<ShapeFlag> oldFlags,
        EntityShape newShape)
    {
        // Flags for old shape.
        EnumSet<ShapeFlag> allOldFlags = ShapeFlag.allFlagsForShape(oldShape);

        // Flags for new shape.
        EnumSet<ShapeFlag> allNewFlags = ShapeFlag.allFlagsForShape(newShape);

        // Restrict flags to new.
        EnumSet<ShapeFlag> ret = oldFlags.clone();
        ret.retainAll(allNewFlags);

        // Add any default flag in 'allNewFlags - allOldFlags'.
        for (ShapeFlag flag : allNewFlags) {
            if (flag.isDefault && !allOldFlags.contains(flag)) {
                ret.add(flag);
            }
        }

        return ret;
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

            if (this.nameAlign != defaultNameAlign) {
                o.put("nameAlign", this.nameAlign.name());
            }

            if (!this.attributes.isEmpty()) {
                o.put("attributes", this.attributes);
            }

            if (this.shapeParams != null) {
                o.put("shapeParams", new JSONArray(this.shapeParams));
            }

            if (!this.shapeFlags.isEmpty()) {
                o.put("shapeFlags", AWTJSONUtil.enumSetToJSON(this.shapeFlags));
            }

            if (!this.fillColor.equals(defaultFillColor)) {
                o.put("fillColor", this.fillColor);
            }

            if (!this.lineColor.equals(defaultLineColor)) {
                o.put("lineColor", this.lineColor);
            }

            if (!this.textColor.equals(defaultTextColor)) {
                o.put("textColor", this.textColor);
            }

            if (!this.anchorName.isEmpty()) {
                o.put("anchorName", this.anchorName);
            }

            if (!this.imageFileName.isEmpty()) {
                o.put("imageFileName", this.imageFileName);
            }

            if (this.imageFillStyle != defaultImageFillStyle) {
                o.put("imageFillStyle", this.imageFillStyle.name());
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
        if (o.has("nameAlign")) {
            this.nameAlign = TextAlign.valueOf(TextAlign.class, o.getString("nameAlign"));
        }

        this.attributes = o.optString("attributes", "");

        JSONArray params = o.optJSONArray("shapeParams");
        if (params != null) {
            this.shapeParams = new int[params.length()];
            for (int i=0; i < params.length(); i++) {
                this.shapeParams[i] = params.getInt(i);
            }
        }

        JSONArray flags = o.optJSONArray("shapeFlags");
        if (flags != null) {
            this.shapeFlags = AWTJSONUtil.enumSetFromJSON(ShapeFlag.class, flags);
        }

        if (ver >= 5) {
            this.fillColor = o.optString("fillColor", defaultFillColor);
        }

        this.lineColor = o.optString("lineColor", defaultLineColor);
        this.textColor = o.optString("textColor", defaultTextColor);

        if (ver >= 12) {
            this.anchorName = o.optString("anchorName", "");
        }

        if (ver >= 7) {
            this.imageFileName = o.optString("imageFileName", "");
        }

        if (ver >= 8 && o.has("imageFillStyle")) {
            this.imageFillStyle =
                ImageFillStyle.valueOf(ImageFillStyle.class, o.getString("imageFillStyle"));
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
    /** Deep clone copy constructor. */
    public Entity(Entity obj)
    {
        this.loc = new Point(obj.loc);
        this.size = new Dimension(obj.size);
        this.shape = obj.shape;
        this.fillColor = obj.fillColor;
        this.lineColor = obj.lineColor;
        this.textColor = obj.textColor;
        this.name = obj.name;
        this.nameAlign = obj.nameAlign;
        this.attributes = obj.attributes;
        this.shapeParams = Util.copyArray(obj.shapeParams);
        this.shapeFlags = obj.shapeFlags.clone();
        this.anchorName = obj.anchorName;
        this.imageFileName = obj.imageFileName;
        this.imageFillStyle = obj.imageFillStyle;
    }

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
                   this.lineColor.equals(e.lineColor) &&
                   this.textColor.equals(e.textColor) &&
                   this.name.equals(e.name) &&
                   this.nameAlign.equals(e.nameAlign) &&
                   this.attributes.equals(e.attributes) &&
                   Arrays.equals(this.shapeParams, e.shapeParams) &&
                   this.shapeFlags.equals(e.shapeFlags) &&
                   this.anchorName.equals(e.anchorName) &&
                   this.imageFileName.equals(e.imageFileName) &&
                   this.imageFillStyle.equals(e.imageFillStyle);
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
        h = h*31 + this.lineColor.hashCode();
        h = h*31 + this.textColor.hashCode();
        h = h*31 + this.name.hashCode();
        h = h*31 + this.nameAlign.hashCode();
        h = h*31 + this.attributes.hashCode();
        h = h*31 + Arrays.hashCode(this.shapeParams);
        h = h*31 + this.shapeFlags.hashCode();
        h = h*31 + this.anchorName.hashCode();
        h = h*31 + this.imageFileName.hashCode();
        h = h*31 + this.imageFillStyle.hashCode();
        return h;
    }

    @Override
    public String toString()
    {
        return toJSON().toString();
    }
}

// EOF
