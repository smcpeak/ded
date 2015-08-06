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

import util.IdentityHashSet;
import util.awt.BitmapFont;
import util.awt.G;
import util.awt.GeomUtil;
import util.awt.HorizOrVert;
import util.swing.MenuAction;

import ded.model.Diagram;
import ded.model.Entity;
import ded.model.EntityShape;
import ded.model.ImageFillStyle;
import ded.model.ShapeFlag;
import ded.model.TextAlign;

import static util.StringUtil.fmt;

/** Controller for Entity. */
public class EntityController extends Controller
{
    // ----------- constants -------------
    /** Default color to fill the entity interior if the chosen color
      * is somehow invalid. */
    public static final Color fallbackEntityFillColor = new Color(192, 192, 192);

    /** Line color when it is invalid. */
    public static final Color fallbackEntityLineColor = Color.BLACK;

    /** Width of border to draw with XOR around selected entities,
      * beyond changing the background color (which may be ignored). */
    public static final int selectedEntityXORBorderWidth = 3;

    /** Height of the name box. */
    public static final int entityNameHeight = 20;

    /** Distance between entity box sides and the attribute text. */
    public static final int entityAttributeMargin = 5;

    /** Minimum side size for an entity when resizing using the handles.
      * Note that a smaller entity can be made by using the edit dialog. */
    public static final int minimumEntitySize = 20;       // 20x20

    /** Number of pixels to expand the selection box on all sides beyond
      * the normal hit-test rectangle. */
    public static final int selectionBoxExpansion = 0;

    /** Color of the automatic resize handle in Windows. */
    public static final Color windowResizeCenterHandleColor = new Color(255, 0, 0);

    /** Color of the top-left side of a beveled control. */
    public static final Color bevelLightColor = Color.WHITE;

    /** Color of the bottom-right side of a beveled control. */
    public static final Color bevelDarkColor = Color.BLACK;

    /** Color of the non-bevel part of the scroll thumb. */
    public static final Color scrollThumbColor = new Color(220, 220, 220);

    // ----------- instance data -------------
    /** The thing being controlled. */
    public Entity entity;

    /** If 'wantResizeHandles()', then this is an array of
      * ResizeHandle.NUM_RESIZE_HANDLES resize handles.  Otherwise,
      * it is null. */
    public EntityResizeController[] resizeHandles;

    /** If 'wantCenterHandle()', then this is a handle for the
      * window auto-resize center.  Otherwise, null. */
    public WindowCenterController windowCenterHandle;

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
    public void resizeSetLeft(int v)
    {
        int diff = v - this.getLeft();
        this.entity.loc.x += diff;
        this.entity.size.width -= diff;
    }

    /** Set top edge w/o changing other locations. */
    public void resizeSetTop(int v)
    {
        int diff = v - this.getTop();
        this.entity.loc.y += diff;
        this.entity.size.height -= diff;
    }

    /** Get right or bottom edge, depending on 'hv', */
    public int getBottomOrRight(HorizOrVert hv)
    {
        return hv.isHoriz() ? this.getRight() : this.getBottom();
    }

    /** Set right or bottom edge, depending on 'hv', w/o changing other locations.
      * 'direct' should be true if the user is directly resizing this entity,
      * false if the resize happens because it is contained in a window entity. */
    public void resizeSetBottomOrRight(int v, HorizOrVert hv, boolean direct)
    {
        int diff = v - this.getBottomOrRight(hv);

        if (G.size(this.entity.size, hv) + diff < minimumEntitySize) {
            // Set 'diff' to value that will make width equal minimum.
            diff = minimumEntitySize - G.size(this.entity.size, hv);
        }

        // 'diff' as a vector.
        Point diffv = G.hvVector(hv, diff);

        if (direct && this.entity.shape == EntityShape.ES_WINDOW) {
            // Calculate the region inside the window that is not the
            // title bar area.  Controllers are only automatically
            // moved and resized in this area, partly to avoid
            // confusion if two windows should happen to have exactly
            // the same region.
            Rectangle moveRegion = this.getAttributeRect();

            // Get all the entities that are inside the region.
            IdentityHashSet<EntityController> contained =
                this.diagramController.findEntityControllersInRectangle(moveRegion);

            // Now calculate the region in which a control point must lie
            // for it to be moved.
            int pq = this.entity.getShapeParam(hv.isHoriz()? 0 : 1);
            pq -= (hv.isVert()? entityNameHeight : 0);    // rel. to attrib rect
            moveRegion = G.moveOriginBy(moveRegion, hv, pq);

            // Move/resize some contained entities.
            for (Controller c : contained) {
                EntityController ec = (EntityController)c;
                if (moveRegion.contains(ec.getRect())) {
                    // Fully contained: move.
                    ec.entity.loc = G.add(ec.entity.loc, diffv);
                }
                else if (ec.getBottomOrRight(hv) >= G.origin(moveRegion, hv)) {
                    // Bottom or right edge contained: resize.
                    ec.resizeSetBottomOrRight(ec.getBottomOrRight(hv) + diff,
                                              hv, false /*direct*/);
                }
            }
        }

        this.entity.size = G.add(this.entity.size, diffv);
    }

    /** Set right edge w/o changing other locations. */
    public void resizeSetRight(int v, boolean direct)
    {
        resizeSetBottomOrRight(v, HorizOrVert.HV_HORIZ, direct);
    }

    /** Set bottom edge w/o changing other locations. */
    public void resizeSetBottom(int v, boolean direct)
    {
        resizeSetBottomOrRight(v, HorizOrVert.HV_VERT, direct);
    }

    /** Get one of the edge values, selected by 'ee'. */
    public int getEdge(EntityEdge ee)
    {
        switch (ee) {
            default: assert(false);
            case EE_LEFT: return getLeft();
            case EE_TOP: return getTop();
            case EE_RIGHT: return getRight();
            case EE_BOTTOM: return getBottom();
        }
    }

    /** Set one of the edge values to 'newValue', either by moving or resizing. */
    public void setEdge(EntityEdge ee, boolean resize, int newValue)
    {
        switch (ee) {
            default: assert(false);

            case EE_LEFT:
                if (resize) {
                    resizeSetLeft(newValue);
                }
                else {
                    this.entity.loc.x = newValue;
                }
                break;

            case EE_TOP:
                if (resize) {
                    resizeSetTop(newValue);
                }
                else {
                    this.entity.loc.y = newValue;
                }
                break;

            case EE_RIGHT:
                if (resize) {
                    resizeSetRight(newValue, true /*direct*/);
                }
                else {
                    this.entity.loc.x += (newValue - this.getRight());
                }
                break;

            case EE_BOTTOM:
                if (resize) {
                    resizeSetBottom(newValue, true /*direct*/);
                }
                else {
                    this.entity.loc.y += (newValue - this.getBottom());
                }
                break;
        }
    }

    /** Get the part of entity rectangle excluding the name/title portion.
      * This assumes the name is shown (it is meant for ES_WINDOW). */
    public Rectangle getAttributeRect()
    {
        Rectangle r = this.getRect();
        r.y += entityNameHeight;
        r.height -= entityNameHeight;
        return r;
    }

    @Override
    public void dragTo(Point p)
    {
        this.entity.loc = p;

        // This does not set the dirty bit or call diagramChanged.
        // Instead, that happens when the mouse is released so we
        // only create one undo/redo record for the entire drag.
        // That does mean that the dirty bit is clear during the
        // drag, which is slightly wrong, but I think acceptable.
    }

    @Override
    public void paintSelectionBackground(Graphics g0)
    {
        Graphics g = g0.create();
        Rectangle r = this.entity.getRect();

        // Paint selection box.  This is a little bigger than the actual
        // hit-test bounds because a lot of my new options for drawing
        // entities completely obscure the hit-test rectangle.
        if (this.isSelected()) {
            g.setColor(Controller.selectedColor);
            g.fillRect(r.x - selectionBoxExpansion,
                       r.y - selectionBoxExpansion,
                       r.width + selectionBoxExpansion*2,
                       r.height + selectionBoxExpansion*2);
        }
    }

    /** Draw 'str' in 'r', centered vertically, and horizontally aligned per 'align'. */
    public void drawAlignedText(Graphics g0, Rectangle r, String str, TextAlign align)
    {
        Graphics g = g0.create();
        g.setClip(r);

        Point center = GeomUtil.getCenter(r);

        // Go to 'p', then add a/2 to get to the baseline.
        // I ignore the descent because it looks better to center without
        // regard to descenders.
        BitmapFont bitmapFont = this.diagramController.getDiagramFont();
        int baseY = center.y + bitmapFont.getAscent()/2;

        // Compute x coordinate based on horizontal alignment.
        int baseX = 0;
        switch (align) {
            case TA_LEFT:
                baseX = r.x + 4;
                break;

            case TA_CENTER:
                baseX = center.x - bitmapFont.stringWidth(str)/2;
                break;

            case TA_RIGHT:
                baseX = r.x + r.width - 4 - bitmapFont.stringWidth(str);
                break;
        }

        bitmapFont.drawString(g, str, baseX, baseY);
    }

    @Override
    public void paint(Graphics g0)
    {
        Graphics g = g0.create();

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

        if (this.isSelected() && this.entity.shape == EntityShape.ES_WINDOW) {
            // Draw the auto-resize location.
            g.setColor(windowResizeCenterHandleColor);

            int p = r.x + this.entity.getShapeParam(0);
            int q = r.y + this.entity.getShapeParam(1);
            g.drawLine(p, r.y+entityNameHeight, p, r.y+r.height);  // vertical line
            g.drawLine(r.x, q, r.x+r.width, q);                    // horizontal line
        }

        // True to use the name rendering code below the 'switch'
        // statement.  Set to false when a switch case does its own
        // name rendering.
        boolean wantNameRender = true;

        // Entity outline with proper shape.
        switch (this.entity.shape) {
            case ES_NO_SHAPE:
                g.setColor(this.getLineColor());
                break;

            case ES_RECTANGLE:
            case ES_CUBOID:
            case ES_WINDOW:
            case ES_SCROLLBAR:
            case ES_PUSHBUTTON:
            case ES_TEXT_EDIT:
            case ES_DROPDOWN:
            case ES_COMBO_BOX:
                if (wantSolidBackground) {
                    // Fill with the normal entity color (selected controllers
                    // get filled with selection color by super.paint).
                    g.setColor(this.getFillColor());
                    g.fillRect(r.x, r.y, r.width-1, r.height-1);

                }

                g.setColor(this.getLineColor());
                g.drawRect(r.x, r.y, r.width-1, r.height-1);

                if (this.entity.shape == EntityShape.ES_SCROLLBAR) {
                    this.drawScrollbar(g, r);
                }
                if (this.entity.shape == EntityShape.ES_PUSHBUTTON ||
                    this.entity.shape == EntityShape.ES_DROPDOWN)
                {
                    Rectangle inner = (Rectangle)r.clone();
                    inner.x++;
                    inner.y++;
                    inner.width -= 2;
                    inner.height -= 2;
                    this.drawBevel(g, inner);

                    if (this.entity.shape == EntityShape.ES_DROPDOWN) {
                        this.drawDropdownButton(g, inner);
                    }
                }
                if (this.entity.shape == EntityShape.ES_TEXT_EDIT) {
                    this.drawTextEdit(g, r);
                }
                if (this.entity.shape == EntityShape.ES_COMBO_BOX) {
                    this.drawTextEdit(g, r);
                    this.drawDropdownButton(g, r);
                }
                break;

            case ES_ELLIPSE:
                if (wantSolidBackground) {
                    g.setColor(this.getFillColor());
                    g.fillOval(r.x, r.y, r.width-1, r.height-1);

                }

                g.setColor(this.getLineColor());
                g.drawOval(r.x, r.y, r.width-1, r.height-1);
                break;

            case ES_CYLINDER:
                this.drawCylinder(g, r, wantSolidBackground);
                break;

            case ES_CHECK_BOX:
            case ES_RADIO_BUTTON:
                this.drawCheckOrRadio(g, r);
                wantNameRender = false;
                break;
        }

        if (!wantNameRender) {
            // Skip the name render code.
        }
        else if (this.entity.attributes.isEmpty() &&
                 this.entity.shape != EntityShape.ES_WINDOW)
        {
            // Name is vertically and horizontally centered in the space.
            g.setColor(this.getTextColor());
            drawAlignedText(g, r, this.entity.name, this.entity.nameAlign);
        }
        else {
            // Name.
            Rectangle nameRect = new Rectangle(r);
            if (this.entity.name.isEmpty() && this.entity.shape != EntityShape.ES_WINDOW) {
                // Do not take up space, do not draw divider.
                nameRect.height = 0;
            }
            else {
                nameRect.height = entityNameHeight;

                if (this.entity.shape != EntityShape.ES_CYLINDER) {
                    // Divider between name and attributes.
                    g.drawLine(nameRect.x, nameRect.y+nameRect.height-1,
                               nameRect.x+nameRect.width-1, nameRect.y+nameRect.height-1);
                }
                else {
                    // The lower half of the upper ellipse plays the role
                    // of a divider.
                }

                if (this.entity.shape == EntityShape.ES_WINDOW) {
                    // Draw controls in the title bar.
                    EnumSet<ShapeFlag> flags = this.entity.shapeFlags;
                    if (flags.contains(ShapeFlag.SF_HAS_WINDOW_OPS)) {
                        this.drawWindowTitleButton(g, nameRect, true /*left*/, "window-ops-button.png");
                    }
                    if (flags.contains(ShapeFlag.SF_HAS_CLOSE)) {
                        this.drawWindowTitleButton(g, nameRect, false /*left*/, "window-close-button.png");
                    }
                    if (flags.contains(ShapeFlag.SF_HAS_MAXIMIZE)) {
                        this.drawWindowTitleButton(g, nameRect, false /*left*/, "window-maximize-button.png");
                    }
                    if (flags.contains(ShapeFlag.SF_HAS_MINIMIZE)) {
                        this.drawWindowTitleButton(g, nameRect, false /*left*/, "window-minimize-button.png");
                    }
                }

                g.setColor(this.getTextColor());
                drawAlignedText(g, nameRect, this.entity.name, this.entity.nameAlign);
            }

            // Attributes.
            Rectangle attributeRect = new Rectangle(r);
            attributeRect.y += nameRect.height;
            attributeRect.height -= nameRect.height;
            attributeRect = GeomUtil.growRectangle(attributeRect, -entityAttributeMargin);
            Graphics g2 = g.create();      // localize effect of clipRect
            g2.clipRect(attributeRect.x, attributeRect.y,
                        attributeRect.width, attributeRect.height);
            g2.setColor(this.getTextColor());
            BitmapFont font = this.diagramController.getDiagramFont();
            int maxAscent = font.getMaxAscent();
            font.drawTextWithNewlines(g2,
                this.entity.attributes,
                attributeRect.x,
                attributeRect.y + maxAscent);
        }

        // Try to make sure selected objects are noticeable, even when
        // using a fill image.
        if (this.isSelected()) {
            // Must be white to ensure that at least one bit is flipped.
            g.setXORMode(Color.WHITE);

            // We start at 1 so that the border itself is left alone.
            // This is important with the default black border on a white
            // background, since XOR with white will make it white, and
            // then the entity seems to be one pixel smaller while it is
            // selected, making visualizing its position more difficult.
            for (int i=1; i <= selectedEntityXORBorderWidth; i++) {
                g.drawRect(r.x + i, r.y + i,
                           r.width-1 - i*2, r.height-1 - i*2);
            }
        }
    }

    /** Draw the window operations menu button in left end of 'titleRect',
      * updating it to reflect the remaining space. */
    private void drawWindowTitleButton(
        Graphics g,
        Rectangle titleRect,
        boolean leftAlign,
        String resourceName)
    {
        // Get the image to draw.
        Image image = this.diagramController.getResourceImage(resourceName);
        if (image == null) {
            return;
        }

        // Get its width.
        int imageWidth = image.getWidth(null);
        if (imageWidth < 0) {
            imageWidth = 0;       // still loading?
        }

        // Draw, and adjust rectangle 'x' if needed.
        if (leftAlign) {
            g.drawImage(image, titleRect.x+1, titleRect.y+1, null /*obs*/);
            titleRect.x += imageWidth;
        }
        else {
            g.drawImage(image, titleRect.x + titleRect.width-1 - imageWidth,
                        titleRect.y+1, null /*obs*/);
        }

        // Adjust rectangle width.
        titleRect.width -= imageWidth;
        if (titleRect.width < 0) {
            titleRect.width = 0;
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
            case IFS_LOCK_SIZE: {
                // Bugfix: Make sure not to ask to draw more of the image
                // than exists.  If I do, weird things happen!
                int w = imageWidth;
                if (w < 0 || w > r.width) {
                    w = r.width;
                }
                int h = imageHeight;
                if (h < 0 || h > r.height) {
                    h = r.height;
                }

                // I first tried the simplest drawImage call, but it is
                // significantly slower than specifying all of the
                // coordinates, even when the image is not clipped (?).
                //
                // The API docs do not say that it is ok to pass null
                // as the observer, but I saw code that did it online,
                // and so far it seems to work.
                g.drawImage(image, r.x, r.y, r.x + w, r.y + h,
                                   0,0, w, h, null);
                break;
            }

            case IFS_CENTER:
                g.drawImage(image, r.x + r.width/2 - imageWidth/2,
                                   r.y + r.height/2 - imageHeight/2,
                                   r.x + r.width/2 + imageWidth/2,
                                   r.y + r.height/2 + imageHeight/2,
                                   0,0, imageWidth, imageHeight, null);
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
        return this.diagramController.diagram.getNamedColor(
            this.entity.fillColor, fallbackEntityFillColor);
    }

    /** Get the color to use to draw Entity lines. */
    public Color getLineColor()
    {
        return this.diagramController.diagram.getNamedColor(
            this.entity.lineColor, fallbackEntityLineColor);
    }

    /** Get the color to use to draw text inside the Entity. */
    public Color getTextColor()
    {
        return this.diagramController.diagram.getNamedColor(
            this.entity.textColor, Color.BLACK);
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
        g.setColor(this.getLineColor());
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

        g.setColor(this.getLineColor());

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

    /** Draw the scrollbar image into 'r'. */
    public void drawScrollbar(Graphics g, Rectangle r)
    {
        int p = this.entity.getShapeParam(0);
        int q = this.entity.getShapeParam(1);

        // The long and short directions for the scrollbar.
        HorizOrVert hvLong, hvShort;
        if (r.height >= r.width) {
            // Draw vertical orientation.
            hvLong = HorizOrVert.HV_VERT;
            hvShort = HorizOrVert.HV_HORIZ;
        }
        else {
            // Horizontal orientation.
            hvLong = HorizOrVert.HV_HORIZ;
            hvShort = HorizOrVert.HV_VERT;
        }

        // Size of the short dimension.
        int shortSide = G.size(r, hvShort);

        // Calculate a 1-length vector along the track direction from
        // the "decrease" button to the "increase" button, and another
        // that is perpendicular.
        Point trackv = G.hvVector(hvLong, 1);        // along the track
        Point crossv = G.hvVector(hvShort, 1);       // across the track

        // Endpoint button images.
        Image decreaseButton = this.diagramController.getResourceImage(
            hvLong.isVert()? "scroll-up-button.png" : "scroll-left-button.png");
        Image increaseButton = this.diagramController.getResourceImage(
            hvLong.isVert()? "scroll-down-button.png" : "scroll-right-button.png");

        if (G.size(r, hvLong) > shortSide*2) {
            // Two square buttons plus thumb.
            Dimension shortSideSquare = G.squareDim(shortSide);
            G.drawImage(g, decreaseButton, G.topLeft(r), shortSideSquare);
            G.drawImage(g, increaseButton, G.sub(G.bottomRight(r), shortSideSquare), shortSideSquare);

            // Narrow 'r' to just the thumb track.
            r = G.moveTopLeftBy(r, G.mul(trackv, shortSide));
            r = G.moveBottomRightBy(r, G.mul(trackv, -1 * shortSide));

            // Then to the thumb location.
            int thumbStart = G.origin(r, hvLong) + G.size(r, hvLong) * p / 100;
            int thumbEnd = G.origin(r, hvLong) + G.size(r, hvLong) * q / 100;
            r = G.setOrigin(r, hvLong, thumbStart);
            r = G.setSize(r, hvLong, thumbEnd-thumbStart+1);
            r = G.incOrigin(r, hvShort, 1);
            r = G.incSize(r, hvShort, -2);

            this.drawScrollThumb(g, r);
        }
        else {
            // Spinner: squash the up/down to fit.
            int mid = G.size(r, hvLong) / 2;
            G.drawImage(g, decreaseButton,
                        G.topLeft(r),
                        G.add(G.mul(crossv, shortSide),
                              G.mul(trackv, mid)));
            G.drawImage(g, increaseButton,
                        G.add(G.topLeft(r), G.mul(trackv, mid)),
                        G.add(G.mul(crossv, shortSide),
                              G.mul(trackv, G.size(r, hvLong) - mid)));
        }
    }

    /** Draw the scroll thumb image into 'r'. */
    private void drawScrollThumb(Graphics g, Rectangle r)
    {
        g.setColor(scrollThumbColor);
        g.fillRect(r.x, r.y, r.width, r.height);

        this.drawBevel(g, r);
    }

    /** Draw a bevel just inside 'r'. */
    private void drawBevel(Graphics g0, Rectangle r)
    {
        Graphics g = g0.create();
        g.setClip(r.x, r.y, r.width, r.height);

        g.setColor(bevelLightColor);
        g.drawLine(r.x, r.y, r.x+r.width-2, r.y);           // outer top
        g.drawLine(r.x+1, r.y+1, r.x+r.width-3, r.y+1);     // inner top
        g.drawLine(r.x, r.y, r.x, r.y+r.height-2);          // outer left
        g.drawLine(r.x+1, r.y+1, r.x+1, r.y+r.height-3);    // inner left

        g.setColor(bevelDarkColor);
        g.drawLine(r.x+r.width-1, r.y+r.height-1, r.x+r.width-1, r.y+1);   // outer right
        g.drawLine(r.x+r.width-2, r.y+r.height-1, r.x+r.width-2, r.y+2);   // inner right
        g.drawLine(r.x+r.width-1, r.y+r.height-1, r.x+1, r.y+r.height-1);  // outer bottom
        g.drawLine(r.x+r.width-1, r.y+r.height-2, r.x+2, r.y+r.height-2);  // inner bottom
    }

    /** Draw the text edit control shape into 'r', assuming that it has
      * already been filled and outlined like a normal entity. */
    public void drawTextEdit(Graphics g0, Rectangle r)
    {
        Graphics g = g0.create();
        g.setClip(r.x, r.y, r.width, r.height);

        // The graphic design here is meant to suggest that the inner
        // frame of the edit box curves away from the viewer, into the
        // plane of the screen.  When lit from the upper left, that
        // causes the top and left inner frames to gradually darken as
        // we move in, while the bottom and right inner frames gradually
        // lighten.  However, all that really matters for the purpose of
        // making UI wire frames is that this is visually distinct from
        // the other rectangles, which is accomplished mainly by thickening
        // the frame.

        // Compute lighter and darker gradients based on the current
        // color.  These values are tuned for the "Gray" color, in the UI,
        // which is RGB(192,192,192), although it should more or less work
        // for other colors as well.  (The main problem is with colors that
        // are significantly darker or lighter, as the 'adjustBrightness'
        // calls tend to max out and therefore lose contrast.)
        Color mid = this.getLineColor();
        Color darker2 = adjustBrightness(mid, -0.30f);
        Color darker1 = adjustBrightness(mid, -0.20f);
        Color lighter1 = adjustBrightness(mid, +0.10f);
        Color lighter2 = adjustBrightness(mid, +0.15f);

        // Left and top +1
        g.setColor(darker1);
        g.drawLine(r.x+1, r.y+1, r.x+r.width-2, r.y+1);
        g.drawLine(r.x+1, r.y+1, r.x+1, r.y+r.height-2);

        // Left and top +2
        g.setColor(darker2);
        g.drawLine(r.x+2, r.y+2, r.x+r.width-3, r.y+2);
        g.drawLine(r.x+2, r.y+2, r.x+2, r.y+r.height-3);

        // Right and bottom -1
        g.setColor(lighter1);
        g.drawLine(r.x+r.width-2, r.y+r.height-2, r.x+r.width-2, r.y+2);
        g.drawLine(r.x+r.width-2, r.y+r.height-2, r.x+2, r.y+r.height-2);

        // Right and bottom -2
        g.setColor(lighter2);
        g.drawLine(r.x+r.width-3, r.y+r.height-3, r.x+r.width-3, r.y+3);
        g.drawLine(r.x+r.width-3, r.y+r.height-3, r.x+3, r.y+r.height-3);
    }

    /** Return a new Color that has the same hue and saturation as 'orig',
      * but with a brightness that has 'brightnessOffset' (which may be
      * negative) added to it. */
    static Color adjustBrightness(Color orig, float brightnessOffset)
    {
        float[] hsb = Color.RGBtoHSB(orig.getRed(), orig.getGreen(), orig.getBlue(), null);
        float b = hsb[2] + brightnessOffset;
        if (b < 0.0f) {
            b = 0.0f;
        }
        else if (b > 1.0f) {
            b = 1.0f;
        }
        return Color.getHSBColor(hsb[0], hsb[1], b);
    }

    /** Draw a dropdown/combo-box button on the right side of 'r'. */
    private void drawDropdownButton(Graphics g, Rectangle r)
    {
        Image button = this.diagramController.getResourceImage("scroll-down-button.png");

        // Shrink the rectangle by 2 on all sides.  This puts it
        // inside the bevel of a button and inside the "curved"
        // insets of a text edit.
        Rectangle r2 = (Rectangle)r.clone();
        r2.x += 2;
        r2.y += 2;
        r2.width -= 4;
        r2.height -= 4;

        // Compute the largest square that fits into the right side of
        // the rectangle 'r'.  (This assumes the rectangle is wider than
        // it is tall.)
        Rectangle square = G.moveTopLeftBy(r2, new Point(r2.width - r2.height, 0));

        G.drawImage(g, button, square);
    }

    /** Draw either a check box or radio button into 'r'. */
    private void drawCheckOrRadio(Graphics g, Rectangle r)
    {
        // Get image button image for chosen state.
        Image buttonImage;
        {
            EnumSet<ShapeFlag> flags = this.entity.shapeFlags;
            String stateSuffix =
                flags.contains(ShapeFlag.SF_CHECKED)?   "checked" :
                flags.contains(ShapeFlag.SF_TRI_STATE)? "tri-state" :
                                                        "unchecked";
            buttonImage = this.diagramController.getResourceImage(
                this.entity.shape == EntityShape.ES_CHECK_BOX?
                    ("check-box-"+stateSuffix+".png") :
                    ("radio-button-"+stateSuffix+".png"));
        }

        // Calculate size of square in left side.
        int side = r.height;
        Rectangle square = G.setSize(r, HorizOrVert.HV_HORIZ, side);
        Rectangle labelArea = G.moveTopLeftBy(r, new Point(side, 0));

        G.drawImage(g, buttonImage, square);

        g.setColor(this.getTextColor());
        drawAlignedText(g, labelArea, this.entity.name, this.entity.nameAlign);
    }

    /** Return the rectangle describing this controller's bounds. */
    public Rectangle getRect()
    {
        return this.entity.getRect();
    }

    @Override
    public Set<Polygon> getBounds()
    {
        Rectangle r = this.getRect();
        if (this.entity.shape == EntityShape.ES_WINDOW) {
            // Windows normally contain lots of other controls.  It is
            // annoying when selecting the other controls to have the
            // window get selected if I miss, etc.  So only select the
            // window itself from its title bar.
            r.height = entityNameHeight;
        }
        Polygon p = GeomUtil.rectPolygon(r);
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

        JMenu fillColorMenu = new JMenu("Set fill color");
        fillColorMenu.setMnemonic(KeyEvent.VK_F);
        for (final String color : this.diagramController.diagram.namedColors.keySet()) {
            fillColorMenu.add(new AbstractAction(color) {
                public void actionPerformed(ActionEvent e) {
                    ths.diagramController.setSelectedEntitiesFillColor(color);
                }
            });
        }
        menu.add(fillColorMenu);

        JMenu textColorMenu = new JMenu("Set text color");
        textColorMenu.setMnemonic(KeyEvent.VK_T);
        for (final String color : this.diagramController.diagram.namedColors.keySet()) {
            textColorMenu.add(new AbstractAction(color) {
                public void actionPerformed(ActionEvent e) {
                    ths.diagramController.setSelectedElementsTextColor(color);
                }
            });
        }
        menu.add(textColorMenu);

        JMenu lineColorMenu = new JMenu("Set line color");
        lineColorMenu.setMnemonic(KeyEvent.VK_L);
        for (final String color : this.diagramController.diagram.namedColors.keySet()) {
            lineColorMenu.add(new AbstractAction(color) {
                public void actionPerformed(ActionEvent e) {
                    EntityController.this.diagramController.setSelectedElementsLineColor(color);
                }
            });
        }
        menu.add(lineColorMenu);

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

        JMenu textAlignMenu = new JMenu("Set name text alignment");
        textAlignMenu.setMnemonic(KeyEvent.VK_X);
        for (final TextAlign align : EnumSet.allOf(TextAlign.class)) {
            textAlignMenu.add(new AbstractAction(align.toString()) {
                public void actionPerformed(ActionEvent e) {
                    ths.diagramController.setSelectedElementsNameTextAlign(align);
                }
            });
        }
        menu.add(textAlignMenu);

        JMenu alignMenu = new JMenu("Align edges");
        alignMenu.setMnemonic(KeyEvent.VK_A);
        for (final AlignCommand ac : EnumSet.allOf(AlignCommand.class)) {
            alignMenu.add(new MenuAction(ac.label, ac.mnemonic) {
                public void actionPerformed(ActionEvent e) {
                    ths.diagramController.alignSelectedEntities(ac);
                }
            });
        }
        menu.add(alignMenu);

        menu.add(new AbstractAction("Set anchor name to entity name") {
            public void actionPerformed(ActionEvent e) {
                ths.diagramController.setSelectedEntitiesAnchorName(
                    DiagramController.SetAnchorCommand.SAC_SET_TO_ENTITY_NAME);
            }
        });
        menu.add(new AbstractAction("Clear anchor name") {
            public void actionPerformed(ActionEvent e) {
                ths.diagramController.setSelectedEntitiesAnchorName(
                    DiagramController.SetAnchorCommand.SAC_CLEAR);
            }
        });
    }

    @Override
    public void setLineColor(String color)
    {
        this.entity.lineColor = color;
    }

    @Override
    public void setTextColor(String color)
    {
        this.entity.textColor = color;
    }

    @Override
    public void setNameTextAlign(TextAlign align)
    {
        this.entity.nameAlign = align;
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

        // Similar logic for the center handle.
        boolean wantCenter = wantCenterHandle();

        if (wantCenter == false && this.windowCenterHandle != null) {
            this.diagramController.remove(this.windowCenterHandle);
            this.windowCenterHandle = null;
        }

        if (wantCenter == true && this.windowCenterHandle == null) {
            this.windowCenterHandle = new WindowCenterController(this.diagramController, this);
            this.diagramController.add(this.windowCenterHandle);
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

    /** Return true if this controller should have a window center handle
      * right now. */
    private boolean wantCenterHandle()
    {
        return
            this.selState == SelectionState.SS_EXCLUSIVE &&
            this.entity.shape == EntityShape.ES_WINDOW;
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

        if (this.wantCenterHandle()) {
            assert(this.windowCenterHandle != null);
            assert(this.diagramController.contains(this.windowCenterHandle));
        }
        else {
            assert(this.windowCenterHandle == null);
        }
    }

    @Override
    public void edit()
    {
        // Name the entity had *before* editing.
        String origName = this.entity.name;

        if (EntityDialog.exec(this.diagramController,
                              this.diagramController.diagram,
                              this.entity)) {
            this.updateAfterImageReload();

            // Make sure the presence or absence of resize handles
            // is consistent with the image fill style.
            this.setSelected(this.selState);

            String newName = this.entity.name;

            this.diagramController.diagramChanged(
                (origName.equals(newName)?
                    fmt("Edit entity \"%1$s\"", origName) :
                    fmt("Edit entity \"%1$s\" (old name: \"%2$s\")", newName, origName)));
        }
        else {
            // There is an odd bug in Swing: if I double-click an entity,
            // then inside the entity dialog, open a help bubble, then
            // cancel all the way out, then the portion of the entity box
            // that was hidden by the entity dialog will not be redrawn
            // correctly.  So, force a repaint when the dialog closes.
            this.diagramController.repaint();
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
