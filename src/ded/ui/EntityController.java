// EntityController.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import util.awt.GeomUtil;
import util.swing.SwingUtil;

import ded.model.Diagram;
import ded.model.Entity;
import ded.model.EntityShape;
import ded.model.ImageFillStyle;

/** Controller for Entity. */
public class EntityController extends Controller
{
    // ----------- static data -------------
    public static final Color defaultEntityFillColor = new Color(192, 192, 192);
    public static final Color entityOutlineColor = new Color(0, 0, 0);

    public static final int entityNameHeight = 20;
    public static final int entityAttributeMargin = 5;
    public static final int minimumEntitySize = 20;       // 20x20

    // ----------- instance data -------------
    /** The thing being controlled. */
    public Entity entity;

    /** If 'wantResizeHandles()', then this is an array of
      * ResizeHandle.NUM_RESIZE_HANDLES resize handles.  Otherwise,
      * it is null. */
    public EntityResizeController[] resizeHandles;

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
    public void paint(Graphics g0)
    {
        Graphics g = g0.create();

        super.paint(g);

        // Get bounding rectangle.
        Rectangle r = this.entity.getRect();

        // If cuboid, draw visible side faces beside the front face,
        // outside 'r'.
        if (this.entity.shape == EntityShape.ES_CUBOID) {
            this.drawCuboidSides(g, r);
        }

        // All further options are clipped to the rectangle.
        g.setClip(r.x, r.y, r.width, r.height);

        // Should we draw a solid background?  As a first cut, we
        // want it unless we are selected, since in that case,
        // super.paint already painted the background in the
        // selection color.
        boolean wantSolidBackground = !this.isSelected();

        // Image background.
        if (!this.entity.imageFileName.isEmpty()) {
            this.drawImage(g, r);

            // Do not draw a solid background; the image will
            // act as the background.
            wantSolidBackground = false;
        }

        // Entity outline with proper shape.
        switch (this.entity.shape) {
            case ES_NO_SHAPE:
                g.setColor(entityOutlineColor);
                break;

            case ES_RECTANGLE:
            case ES_CUBOID:
                if (wantSolidBackground) {
                    // Fill with the normal entity color (selected controllers
                    // get filled with selection color by super.paint).
                    g.setColor(this.getFillColor());
                    g.fillRect(r.x, r.y, r.width-1, r.height-1);

                }

                g.setColor(entityOutlineColor);
                g.drawRect(r.x, r.y, r.width-1, r.height-1);
                break;

            case ES_ELLIPSE:
                if (wantSolidBackground) {
                    g.setColor(this.getFillColor());
                    g.fillOval(r.x, r.y, r.width-1, r.height-1);

                }

                g.setColor(entityOutlineColor);
                g.drawOval(r.x, r.y, r.width-1, r.height-1);
                break;

            case ES_CYLINDER:
                this.drawCylinder(g, r, wantSolidBackground);
                break;
        }

        if (this.entity.attributes.isEmpty()) {
            // Name is vertically and horizontally centered in the space.
            SwingUtil.drawCenteredText(g, GeomUtil.getCenter(r), this.entity.name);
        }
        else {
            // Name.
            Rectangle nameRect = new Rectangle(r);
            if (this.entity.name.isEmpty()) {
                // Do not take up space, do not draw divider.
                nameRect.height = 0;
            }
            else {
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

    /** Draw the named image onto 'g' in 'r'. */
    public void drawImage(Graphics g, Rectangle r)
    {
        Image image = this.diagramController.getImage(this.entity.imageFileName);
        if (image == null) {
            this.drawBrokenImageIndicator(g, r);
            return;
        }

        ImageFillStyle ifs = this.entity.imageFillStyle;
        int imageWidth = image.getWidth(null);
        int imageHeight = image.getHeight(null);
        if (imageWidth < 0 || imageHeight < 0) {
            ifs = ImageFillStyle.IFS_UPPER_LEFT;      // fallback
        }

        switch (ifs) {
            case IFS_UPPER_LEFT:
            case IFS_LOCK_SIZE:
                // I first tried the simplest drawImage call, but it is
                // significantly slower than specifying all of the
                // coordinates, even when the image is not clipped (?).
                //
                // The API docs do not say that it is ok to pass null
                // as the observer, but I saw code that did it online,
                // and so far it seems to work.
                g.drawImage(image, r.x, r.y, r.x+r.width, r.y+r.height,
                                   0,0, r.width, r.height, null);
                break;

            case IFS_STRETCH:
                g.drawImage(image, r.x, r.y, r.x+r.width, r.y+r.height,
                                   0,0, imageWidth, imageHeight, null);
                break;

            case IFS_TILE:
                for (int x = r.x; x < r.x+r.width; x += imageWidth) {
                    for (int y = r.y; y < r.y+r.height; y += imageWidth) {
                        g.drawImage(image, x, y, x+imageWidth, y+imageHeight,
                                    0,0, imageWidth, imageHeight, null);
                    }
                }
                break;
        }
    }

    /** Draw an indicator on 'r' that we could not load the image. */
    private void drawBrokenImageIndicator(Graphics g0, Rectangle r)
    {
        Graphics g = g0.create();

        // Draw a red box with a red X through it.
        g.setColor(Color.RED);
        int w = r.width-1;
        int h = r.height-1;
        g.drawRect(r.x, r.y, w, h);
        g.drawLine(r.x, r.y, r.x+w, r.y+h);
        g.drawLine(r.x+w, r.y, r.x, r.y+h);
    }

    /** Get the color to use to fill this Entity. */
    public Color getFillColor()
    {
        Color c = this.diagramController.diagram.namedColors.get(this.entity.fillColor);
        if (c != null) {
            return c;
        }
        else {
            // Fall back on default if color is not recognized.
            return defaultEntityFillColor;
        }
    }

    /** Draw the part of a cuboid outside the main rectangle 'r'. */
    public void drawCuboidSides(Graphics g, Rectangle r)
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
        g.setColor(this.getFillColor());
        g.fillPolygon(p);
        g.setColor(entityOutlineColor);
        g.drawPolygon(p);

        // Draw line CF.
        g.drawLine(r.x     - left, r.y     - up,   // C
                   r.x,            r.y);           // F
    }

    /** Draw the cylinder shape into 'r'. */
    public void drawCylinder(Graphics g, Rectangle r, boolean wantSolidBackground)
    {
        if (wantSolidBackground) {
            g.setColor(this.getFillColor());

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
        }

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
        super.mousePressed(e);
        this.mouseSelect(e, true /*wantDrag*/);
    }

    @SuppressWarnings("serial")
    @Override
    protected void addToRightClickMenu(JPopupMenu menu, MouseEvent ev)
    {
        final EntityController ths = this;

        JMenu colorMenu = new JMenu("Set fill color");
        colorMenu.setMnemonic(KeyEvent.VK_C);
        for (final String color : this.diagramController.diagram.namedColors.keySet()) {
            colorMenu.add(new AbstractAction(color) {
                public void actionPerformed(ActionEvent e) {
                    ths.diagramController.setSelectedEntitiesFillColor(color);
                }
            });
        }
        menu.add(colorMenu);

        JMenu shapeMenu = new JMenu("Set shape");
        shapeMenu.setMnemonic(KeyEvent.VK_S);
        for (final EntityShape shape : EntityShape.allValues()) {
            shapeMenu.add(new AbstractAction(shape.toString()) {
                public void actionPerformed(ActionEvent e) {
                    ths.diagramController.setSelectedEntitiesShape(shape);
                }
            });
        }
        menu.add(shapeMenu);
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
        super.setSelected(ss);

        boolean wantHandles = this.wantResizeHandles();

        // Destroy unwanted handles.
        if (wantHandles == false && this.resizeHandles != null)
        {
            for (EntityResizeController erc : this.resizeHandles) {
                this.diagramController.remove(erc);
            }
            this.resizeHandles = null;
        }

        // Create wanted handles.
        if (wantHandles == true && this.resizeHandles == null)
        {
            this.resizeHandles = new EntityResizeController[ResizeHandle.NUM_RESIZE_HANDLES];
            for (ResizeHandle h : EnumSet.allOf(ResizeHandle.class)) {
                EntityResizeController erc =
                    new EntityResizeController(this.diagramController, this, h);
                this.resizeHandles[h.ordinal()] = erc;
                this.diagramController.add(erc);
            }
        }
    }

    /** Return true if this controller should have resize handles right now. */
    private boolean wantResizeHandles()
    {
        if (this.selState != SelectionState.SS_EXCLUSIVE) {
            // We never have resize handles if not selected.
            return false;
        }

        if (this.entity.imageFillStyle == ImageFillStyle.IFS_LOCK_SIZE &&
            this.getImageDimension() != null)
        {
            // The size is locked to an image, so no resize handles.
            return false;
        }

        return true;
    }

    @Override
    public void selfCheck()
    {
        super.selfCheck();

        boolean wantHandles = this.wantResizeHandles();
        if (wantHandles) {
            assert(this.resizeHandles != null);
            assert(this.resizeHandles.length == ResizeHandle.NUM_RESIZE_HANDLES);
            for (EntityResizeController erc : this.resizeHandles) {
                assert(this.diagramController.contains(erc));
            }
        }
        else {
            assert(this.resizeHandles == null);
        }
    }

    @Override
    public void edit()
    {
        if (EntityDialog.exec(this.diagramController,
                              this.diagramController.diagram,
                              this.entity)) {
            this.updateAfterImageReload();

            // Make sure the presence or absence of resize handles
            // is consistent with the image fill style.
            this.setSelected(this.selState);

            this.diagramController.diagramChanged();
        }
    }

    @Override
    public void updateAfterImageReload()
    {
        if (this.entity.imageFillStyle == ImageFillStyle.IFS_LOCK_SIZE) {
            Dimension imageDim = this.getImageDimension();
            if (imageDim != null) {
                this.entity.size = imageDim;
            }
        }
    }

    /** Get the dimensions of the fill image for this entity, or null
      * if they cannot be obtained. */
    private Dimension getImageDimension()
    {
        if (this.entity.imageFileName.isEmpty()) {
            return null;               // No fill image.
        }

        Image image = this.diagramController.getImage(this.entity.imageFileName);
        if (image == null) {
            return null;               // Cannot load fill image.
        }

        int imageWidth = image.getWidth(null);
        int imageHeight = image.getHeight(null);
        if (imageWidth < 0 || imageHeight < 0) {
            return null;               // Some delayed loading thing?
        }

        return new Dimension(imageWidth, imageHeight);
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
                if (c instanceof InheritanceController) {
                    InheritanceController ic = (InheritanceController)c;
                    return ic.inheritance.parent == thisEntity;
                }
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
