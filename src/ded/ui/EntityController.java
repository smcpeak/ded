// EntityController.java
// See toplevel license.txt for copyright and license terms.

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

import util.awt.GeomUtil;
import util.swing.SwingUtil;

import ded.model.Diagram;
import ded.model.Entity;
import ded.model.EntityShape;

/** Controller for Entity. */
public class EntityController extends Controller {
    // ----------- static data -------------
    public static final Color defaultEntityFillColor = new Color(192, 192, 192);
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
        this.diagramController.setDirty();
    }
    
    @Override
    public void paint(Diagram diagram, Graphics g0)
    {
        Graphics g = g0.create();
        
        super.paint(diagram, g);
        
        // Get bounding rectangle.
        Rectangle r = this.entity.getRect();
        
        // If cuboid, draw visible side faces beside the front face,
        // outside 'r'.
        if (this.entity.shape == EntityShape.ES_CUBOID) {
            this.drawCuboidSides(diagram, g, r);
        }
        
        // All further options are clipped to the rectangle.
        g.setClip(r.x, r.y, r.width, r.height);
        
        // Entity outline with proper shape.
        switch (this.entity.shape) {
            case ES_NO_SHAPE:
                g.setColor(entityOutlineColor);
                break;
                
            case ES_RECTANGLE:
            case ES_CUBOID:
                if (!this.isSelected()) {
                    // Fill with the normal entity color (selected controllers
                    // get filled with selection color by super.paint).
                    g.setColor(this.getFillColor(diagram));
                    g.fillRect(r.x, r.y, r.width-1, r.height-1);
                    
                }
                
                g.setColor(entityOutlineColor);
                g.drawRect(r.x, r.y, r.width-1, r.height-1); 
                break;
                
            case ES_ELLIPSE:
                if (!this.isSelected()) {
                    g.setColor(this.getFillColor(diagram));
                    g.fillOval(r.x, r.y, r.width-1, r.height-1);
                    
                }
                
                g.setColor(entityOutlineColor);
                g.drawOval(r.x, r.y, r.width-1, r.height-1);
                break;
                
            case ES_CYLINDER:
                this.drawCylinder(diagram, g, r);
                break;
        }
        
        if (this.entity.attributes.isEmpty()) {
            // Name is vertically and horizontally centered in the space.
            SwingUtil.drawCenteredText(g, GeomUtil.getCenter(r), this.entity.name);
        }
        else {
            // Name.
            Rectangle nameRect = new Rectangle(r);
            nameRect.height = entityNameHeight;
            SwingUtil.drawCenteredText(g, GeomUtil.getCenter(nameRect), this.entity.name);
            
            if (this.entity.shape != EntityShape.ES_CYLINDER) {
                // Divider between name and attributes.
                g.drawLine(nameRect.x, nameRect.y+nameRect.height-1,
                           nameRect.x+nameRect.width-1, nameRect.y+nameRect.height-1);
            }
            else {
                // The lower half of the upper ellipse plays the role
                // of a divider.
            }
            
            // Attributes.
            Rectangle attributeRect = new Rectangle(r);
            attributeRect.y += nameRect.height;
            attributeRect.height -= nameRect.height;
            attributeRect = GeomUtil.growRectangle(attributeRect, -entityAttributeMargin);
            g.clipRect(attributeRect.x, attributeRect.y,
                       attributeRect.width, attributeRect.height);
            SwingUtil.drawTextWithNewlines(g,
                this.entity.attributes,
                attributeRect.x,
                attributeRect.y + g.getFontMetrics().getMaxAscent());
        }
    }
    
    /** Get the color to use to fill this Entity. */
    public Color getFillColor(Diagram diagram)
    {
        Color c = diagram.namedColors.get(this.entity.fillColor);
        if (c != null) {
            return c;
        }
        else {
            // Fall back on default if color is not recognized.
            return defaultEntityFillColor;
        }
    }
    
    /** Draw the part of a cuboid outside the main rectangle 'r'. */
    public void drawCuboidSides(Diagram diagram, Graphics g, Rectangle r)
    {
        int[] params = this.entity.shapeParams;
        if (params == null || params.length < 2) {
            return;
        }
        
        // Distance to draw to left/up.
        int left = params[0];
        int up = params[1];
        
        // Distance to right/bottom.
        int w = r.width-1;
        int h = r.height-1;
        
        //          r.x
        //      left|        w
        //       <->|<---------------->
        //          V
        //       C                    D
        //       *--------------------*        ^
        //       |\                    \       |up
        //       | \ F                  \      V
        //       |  *--------------------*E  <---- r.y
        //       |  |                    |     ^
        //      B*  |                    |     |
        //        \ |                    |     |h
        //         \|                    |     |
        //         A*--------------------*     V
        //
        // Construct polygon ABCDEFA.
        Polygon p = new Polygon();
        p.addPoint(r.x,            r.y + h);       // A
        p.addPoint(r.x     - left, r.y + h - up);  // B
        p.addPoint(r.x     - left, r.y     - up);  // C
        p.addPoint(r.x + w - left, r.y     - up);  // D
        p.addPoint(r.x + w,        r.y);           // E
        p.addPoint(r.x,            r.y);           // F
        p.addPoint(r.x,            r.y + h);       // A
        
        // Fill it and draw its edges.
        g.setColor(this.getFillColor(diagram));
        g.fillPolygon(p);
        g.setColor(entityOutlineColor);
        g.drawPolygon(p);
        
        // Draw line CF.
        p = new Polygon();
        g.drawLine(r.x     - left, r.y     - up,   // C
                   r.x,            r.y);           // F
        g.drawPolygon(p);
    }

    /** Draw the cylinder shape into 'r'. */
    public void drawCylinder(Diagram diagram, Graphics g, Rectangle r)
    {
        g.setColor(this.getFillColor(diagram));
        
        // Fill upper ellipse.  I do not quite understand why I
        // have to subtract one from the width and height here,
        // but experimentation shows that if I do not do that,
        // then I get fill color pixels peeking out from behind
        // the outline.
        g.fillOval(r.x, r.y, 
                   r.width - 1, entityNameHeight - 1);
        
        // Fill lower ellipse.
        g.fillOval(r.x, r.y + r.height - entityNameHeight,
                   r.width - 1, entityNameHeight - 1); 
        
        // Fill rectangle between them.
        g.fillRect(r.x, r.y + entityNameHeight/2,
                   r.width, r.height - entityNameHeight);
        
        g.setColor(entityOutlineColor);
        
        // Draw upper ellipse.
        g.drawOval(r.x, r.y,
                   r.width-1, entityNameHeight-1);
        
        // Draw lower ellipse, lower half of it.
        g.drawArc(r.x, r.y + r.height - entityNameHeight,
                  r.width-1, entityNameHeight-1,
                  180, 180);
        
        // Draw left side.
        g.drawLine(r.x, r.y + entityNameHeight/2,
                   r.x, r.y + r.height - entityNameHeight/2);
        
        // Draw right side.
        g.drawLine(r.x + r.width - 1, r.y + entityNameHeight/2,
                   r.x + r.width - 1, r.y + r.height - entityNameHeight/2);

    }
    
    /** Return the rectangle describing this controller's bounds. */
    public Rectangle getRect()
    {
        return this.entity.getRect();
    }
    
    @Override
    public Set<Polygon> getBounds()
    {
        Polygon p = GeomUtil.rectPolygon(this.getRect());
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
        ent.loc = GeomUtil.snapPoint(new Point(p.x - ent.size.width/2,
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
        if (EntityDialog.exec(this.diagramController,
                              this.diagramController.diagram, 
                              this.entity)) {
            this.diagramController.diagramChanged();
        }
    }
    
    @Override
    public void deleteSelfAndData(Diagram diagram)
    {
        // Unselect myself so resize controllers are gone.
        this.setSelected(SelectionState.SS_UNSELECTED);
        
        this.selfCheck();
        
        final Entity thisEntity = this.entity;
        
        // Delete any relations or inheritances that involve this entity.
        this.diagramController.deleteControllers(new ControllerFilter() {
            public boolean satisfies(Controller c) 
            {
                if (c instanceof RelationController) {
                    RelationController rc = (RelationController)c;
                    return rc.relation.involvesEntity(thisEntity);
                }
                /*
                if (c instanceof InheritanceController) {
                    InheritanceController ic = (InheritanceController)c;
                    return ic.inheritance.parent == thisEntity;
                }
                */
                return false;
            }
        });
        
        // Remove the entity and this controller.
        diagram.entities.remove(this.entity);
        this.diagramController.remove(this);
        
        this.diagramController.selfCheck();
    }
}

// EOF
