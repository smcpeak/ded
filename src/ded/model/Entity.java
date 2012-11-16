// Entity.java

package ded.model;

import java.awt.Dimension;
import java.awt.Point;

/** An ER entity, represented as a box with a label and text contents. */
public class Entity {
    // ------------ public data ------------
    /** Location of upper-left corner, in pixels. */
    public Point loc;
    
    /** Size in pixels. */
    public Dimension size;
    
    // ------------ public methods ------------
    public Entity()
    {
        this.loc = new Point(0,0);
        this.size = new Dimension(100, 50);
    }
}

// EOF
