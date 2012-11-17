// EntityController.java

package ded.ui;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import util.SwingUtil;

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
    public void dragTo(Point p)
    {
        this.entity.loc = p;
    }
    
    @Override
    public void paint(Graphics g)
    {
        super.paint(g);
        g.drawRect(this.entity.loc.x, this.entity.loc.y, 
                   this.entity.size.width, this.entity.size.height);
    }

    @Override
    public Set<Polygon> getBounds()
    {
        Polygon p = SwingUtil.rectPolygon(
            this.entity.loc.x,
            this.entity.loc.y,
            this.entity.size.width,
            this.entity.size.height);
        
        Set<Polygon> ret = new HashSet<Polygon>();
        ret.add(p);
        return ret;
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        this.mouseSelect(e, true /*wantDrag*/);
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
    
    @Override
    public void edit()
    {
        if (EntityDialog.exec(this.diagramController, this.entity)) {
            this.diagramController.repaint();
        }
    }
}

// EOF
