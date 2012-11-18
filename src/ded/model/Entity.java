// Entity.java

package ded.model;

import java.awt.Dimension;
import java.awt.Point;

import util.StringUtil;

/** An ER entity, represented as a box with a label and text contents. */
public class Entity {
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
    
    public String toString()
    {
        return "{loc:["+this.loc.x+","+this.loc.y+"]"+
               ",size:["+this.size.width+","+this.size.height+"]"+
               ",shape:\""+this.shape.name()+"\""+
               ",name:"+StringUtil.quoteAsJSONASCII(this.name)+
               ",attributes:"+StringUtil.quoteAsJSONASCII(this.attributes)+
               "}";
    }
}

// EOF
