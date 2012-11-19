// EntityController.java

package ded.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import util.SwingUtil;

import ded.model.Entity;

/** Controller for Entity. */
public class EntityController extends Controller {
    // ----------- static data -------------
    public static final Color entityFillColor = new Color(192, 192, 192);
    public static final Color entityOutlineColor = new Color(0, 0, 0);
    
    public static final int entityNameHeight = 20;
    public static final int entityAttributeMargin = 5;
    public static final int minimumEntitySize = 20;       // 20x20
    
    // ----------- instance data -------------
    /** The thing being controlled. */
    public Entity entity;
    
    /** If 'selState' is SS_EXCLUSIVE, then this is an array of
      * ResizeHandle.NUM_RESIZE_HANDLES resize handles.  Otherwise,
      * it is null. */
    public EntityResizeController[] handle;
    
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

    public int getLeft() { return this.entity.loc.x; }
    public int getTop() { return this.entity.loc.y; }
    public int getRight() { return this.entity.loc.x + this.entity.size.width; }
    public int getBottom() { return this.entity.loc.y + this.entity.size.height; }
    
    /** Set left edge w/o changing other locations. */
    public void setLeft(int v)
    {
        int diff = v - this.getLeft();
        this.entity.loc.x += diff;
        this.entity.size.width -= diff;
    }
    
    /** Set top edge w/o changing other locations. */
    public void setTop(int v)
    {
        int diff = v - this.getTop();
        this.entity.loc.y += diff;
        this.entity.size.height -= diff;
    }
    
    /** Set right edge w/o changing other locations. */
    public void setRight(int v)
    {
        int diff = v - this.getRight();
        this.entity.size.width += diff;
    }
    
    /** Set bottom edge w/o changing other locations. */
    public void setBottom(int v)
    {
        int diff = v - this.getBottom();
        this.entity.size.height += diff;
    }
    
    @Override
    public void dragTo(Point p)
    {
        this.entity.loc = p;
    }
    
    @Override
    public void paint(Graphics g0)
    {
        Graphics g = g0.create();
        
        super.paint(g);
        
        // Entity outline with proper shape.
        Rectangle r = this.entity.getRect();
        switch (this.entity.shape) {
            case ES_NO_SHAPE:
                g.setColor(entityOutlineColor);
                break;
                
            case ES_RECTANGLE:
                if (!this.isSelected()) {
                    // Fill with the normal entity color (selected controllers
                    // get filled with selection color by super.paint).
                    g.setColor(entityFillColor);
                    g.fillRect(r.x, r.y, r.width-1, r.height-1);
                    
                }
                
                g.setColor(entityOutlineColor);
                g.drawRect(r.x, r.y, r.width-1, r.height-1); 
                break;
                
            case ES_ELLIPSE:
                if (!this.isSelected()) {
                    g.setColor(entityFillColor);
                    g.fillOval(r.x, r.y, r.width-1, r.height-1);
                    
                }
                
                g.setColor(entityOutlineColor);
                g.drawOval(r.x, r.y, r.width-1, r.height-1);
                break;
        }
        
        if (this.entity.attributes.isEmpty()) {
            // Name is vertically and horizontally centered in the space.
            SwingUtil.drawCenteredText(g, r, this.entity.name);
        }
        else {
            // Name.
            Rectangle nameRect = new Rectangle(r);
            nameRect.height = entityNameHeight;
            SwingUtil.drawCenteredText(g, nameRect, this.entity.name);
            
            // Divider between name and attributes.
            g.drawLine(nameRect.x, nameRect.y+nameRect.height,
                       nameRect.x+nameRect.width-1, nameRect.y+nameRect.height);
            
            // Attributes.
            Rectangle attributeRect = new Rectangle(r);
            attributeRect.y += nameRect.height;
            attributeRect.height -= nameRect.height;
            attributeRect = SwingUtil.growRectangle(attributeRect, -entityAttributeMargin);
            g.clipRect(attributeRect.x, attributeRect.y,
                       attributeRect.width, attributeRect.height);
            SwingUtil.drawTextWithNewlines(g,
                this.entity.attributes,
                attributeRect.x,
                attributeRect.y + g.getFontMetrics().getMaxAscent());
        }
    }

    /** Return the rectangle describing this controller's bounds. */
    public Rectangle getRect()
    {
        return this.entity.getRect();
    }
    
    @Override
    public Set<Polygon> getBounds()
    {
        Polygon p = SwingUtil.rectPolygon(this.getRect());
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
        ent.loc = SwingUtil.snapPoint(new Point(p.x - ent.size.width/2,
                                                p.y - ent.size.height/2),
                                      DiagramController.SNAP_DIST);
        dc.getDiagram().entities.add(ent);
        
        EntityController ec = new EntityController(dc, ent);
        dc.add(ec);
        dc.selectOnly(ec);
    }
    
    @Override
    public void setSelected(SelectionState ss)
    {
        this.selfCheck();
        
        // When 'exclusive' transitions off, destroy handles.
        if (this.selState == SelectionState.SS_EXCLUSIVE && 
            ss != SelectionState.SS_EXCLUSIVE)
        {
            for (EntityResizeController erc : this.handle) {
                this.diagramController.remove(erc);
            }
            this.handle = null;
        }
        
        // When 'exclusive' transitions on, create handles.
        if (this.selState != SelectionState.SS_EXCLUSIVE &&
            ss == SelectionState.SS_EXCLUSIVE)
        {
            this.handle = new EntityResizeController[ResizeHandle.NUM_RESIZE_HANDLES];
            for (ResizeHandle h : EnumSet.allOf(ResizeHandle.class)) {
                EntityResizeController erc =
                    new EntityResizeController(this.diagramController, this, h);
                this.handle[h.ordinal()] = erc;
                this.diagramController.add(erc);
            }
        }
        
        super.setSelected(ss);
    }
    
    @Override
    public void selfCheck()
    {
        super.selfCheck();
        
        if (this.selState == SelectionState.SS_EXCLUSIVE) {
            assert(this.handle != null);
            assert(this.handle.length == ResizeHandle.NUM_RESIZE_HANDLES);
            for (EntityResizeController erc : this.handle) {
                assert(this.diagramController.contains(erc));
            }
        }
        else {
            assert(this.handle == null);
        }
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
