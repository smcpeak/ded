// EntityController.java

package ded.ui;

import java.awt.Graphics;
import java.awt.Point;

import ded.model.Entity;

/** Controller for Entity. */
public class EntityController extends Controller {
    // ----------- public data -------------
    /** The thing being controlled. */
    public Entity entity;
    
    // ----------- public methods -----------
    public EntityController(DiagramController dc, Entity e)
    {
        super(dc);
        this.entity = e;
    }

    @Override
    public Point getLoc()
    {
        return this.entity.loc;
    }
    
    @Override
    public void paint(Graphics g)
    {
        g.drawRect(this.entity.loc.x, this.entity.loc.y, 
                   this.entity.size.width-1, this.entity.size.height-1);
    }
}

// EOF
