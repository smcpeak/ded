// Diagram.java

package ded.model;

import java.awt.Dimension;
import java.util.ArrayList;

/** Complete diagram. */
public class Diagram {
    // ---------- public data ------------
    /** Size of window to display diagram.  Some elements might not fit
      * in the current size. */
    public Dimension windowSize;
    
    /** Entities, in display order.  The last entity will appear on top
      * of all others. */
    public ArrayList<Entity> entities;
    
    // ----------- public methods -----------
    public Diagram()
    {
        this.windowSize = new Dimension(800, 800);
        this.entities = new ArrayList<Entity>();
    }
}

// EOF
