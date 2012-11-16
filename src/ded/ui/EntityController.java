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

    /** Create a new entity at location 'p' in 'dc'.  This corresponds to
      * the user left-clicking on 'p' while in entity creation mode. */
    public static void createEntityAt(DiagramController dc, Point p)
    {
        Entity ent = new Entity();
        ent.loc = new Point(p.x - ent.size.width/2,
                            p.y - ent.size.height/2);
        dc.getDiagram().entities.add(ent);
        dc.addController(new EntityController(dc, ent));
    }
}

// EOF
