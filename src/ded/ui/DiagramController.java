// DiagramController.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;

import java.lang.reflect.Field;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.json.JSONException;

import util.IdentityHashSet;
import util.ImageFileUtil;
import util.Util;
import util.awt.BitmapFont;
import util.awt.GeomUtil;
import util.swing.SwingUtil;

import ded.Ded;
import ded.model.ArrowStyle;
import ded.model.Diagram;
import ded.model.Entity;
import ded.model.EntityShape;
import ded.model.Inheritance;
import ded.model.Relation;
import ded.model.RelationEndpoint;
import ded.model.TextAlign;
import ded.model.UndoHistory;
import ded.model.UndoHistoryLimit;

import static util.StringUtil.fmt;
import static util.StringUtil.localize;

/** Widget to display and edit a diagram. */
public class DiagramController extends JPanel
    implements MouseListener, MouseMotionListener, KeyListener,
               ComponentListener, FocusListener, UndoHistoryLimit
{
    // ------------- constants ---------------
    private static final long serialVersionUID = 1266678840598864303L;

    /** Pixels from left/top edge to draw the file name label. */
    public static final int fileNameLabelMargin = 2;

    private static final String helpMessage =
        "H or F1 - This message\n"+
        "Q - Quit\n"+
        "S - Select mode\n"+
        "C - Create entity mode\n"+
        "A - Create relation (\"arrow\") mode\n"+
        "I - Create inheritance mode\n"+
        "Enter or Double click - Edit selected thing\n"+
        "Insert - Insert relation control point\n"+
        "Delete - Delete selected thing\n"+
        "Ctrl+C, Ctrl-V - Copy/paste, including across windows\n"+
        "Ctrl+S - Save to file and export to PNG (fname+\".png\")\n"+
        "Ctrl+O - Load from file (can import ER files)\n"+
        "Left click - select\n"+
        "Ctrl+Left click - multiselect\n"+
        "Left click+drag - multiselect rectangle\n"+
        "Right click - action menu for selection(s)\n"+
        "\n"+
        "When entity selected, F/B to move to front/back.\n"+
        "When relation selected, H/V/D to change routing,\n"+
        "comma/period to cycle arrow heads at start/end,\n"+
        "and S to swap start/end arrowheads.\n"+
        "When inheritance selected, O to change open/closed.\n"+
        "When dragging, hold Shift to turn off 5-pixel snap.\n"+
        "\n"+
        "See menu bar for commands without keybindings.";

    /** The existence and value of this field tells (via reflection)
      * Abbot, a GUI test tool, that it should record low-level mouse
      * events rather than converting them into "click" or "drag"
      * events. */
    public static String abbotRecorderClassName = "NoClickComponent";

    /** When true, turn on some extra diagnostics related to debugging
      * a problem with Abbot where it interferes with normal focus. */
    public static final boolean debugFocus = false;

    // ------------- static data ---------------
    /** Granularity of drag/move snap action. */
    public static final int SNAP_DIST = 5;

    // ------------- private types ---------------
    /** Primary "mode" of the editing interface, indicating what happens
      * when the left mouse button is clicked or released. */
    public static enum Mode {
        DCM_SELECT                     // click to select/move/resize
            ("Select"),
        DCM_CREATE_ENTITY              // click to create an entity
            ("Create entity"),
        DCM_CREATE_RELATION            // click to create a relation
            ("Create relation"),
        DCM_CREATE_INHERITANCE         // click to create an inheritance relation
            ("Create inheritance"),
        DCM_DRAGGING                   // currently drag-moving something
            ("Dragging"),
        DCM_RECT_LASSO                 // currently drag-lasso selecting
            ("Rectangle lasso selecting");

        /** User-visible description of the mode. */
        public final String description;

        private Mode(String d)
        {
            this.description = d;
        }
    }

    // ------------- instance data ---------------
    /** Parent diagram editor window. */
    private Ded dedWindow;

    /** The diagram we are editing. */
    public Diagram diagram;

    /** Set of controllers for elements of the diagram.  For the moment, the order
      * is supposed to be the same as the corresponding 'diagram' model elements,
      * but I'm not sure how I'm going to maintain that invariant or if it is
      * really what I want. */
    private ArrayList<Controller> controllers;

    /** Current primary editing mode. */
    private Mode mode;

    /** If DCM_RECT_LASSO, the point where the mouse button was originally pressed. */
    private Point lassoStart;

    /** If DCM_RECT_LASSO, the current mouse position. */
    private Point lassoEnd;

    /** If DCM_RECT_LASSO, the set of originally-selected controllers,
      * which is to be included in whatever is contained by the lasso. */
    private IdentityHashSet<Controller> lassoOriginalSelected =
        new IdentityHashSet<Controller>();

    /** If CFM_DRAGGING, this is the controller being moved. */
    private Controller dragging;

    /** If CFM_DRAGGING, this is the vector from the original mouse click point
      * to the Controller's original getLoc(). */
    private Point dragOffset;

    /** If CFM_DRAGGING, the localized command description of the effect
      * of the drag if the mouse were to be released now.  This is
      * initially null, meaning that no drag movement has occurred
      * (for example, a simple click to select). */
    private String dragCommandDescription;

    /** Most recently used file name, or "" if there is none. */
    private String fileName;

    /** Most recently used directory for loading/saving files. */
    private File currentFileChooserDirectory;

    /** When true, the in-memory Diagram has been modified since the
      * last time it was saved. */
    private boolean dirty;

    /** When true, the in-memory Diagram was loaded from a file that
      * was in the ER format.  This matters because we cannot *save*
      * the file in that format. */
    private boolean importedFile;

    /** Map from image file name to cached image.  A name can be
      * mapped to null, meaning we failed to load the image. */
    private HashMap<String, Image> imageCache;

    /** Accumulated log messages. */
    private StringBuilder logMessages;

    /** Undo/redo history. */
    private UndoHistory undoHistory;

    /** Maximum length of the undo history. */
    private int undoHistoryLimit = 100;

    /** Window for directly displaying the undo history. */
    private UndoHistoryWindow undoHistoryWindow;

    /** When not 0, we use a "triple buffer" render technique to
      * avoid problems on Apple HiDPI/Retina displays.  Mode -1
      * uses a "compatible" image.  Other values are treated as
      * "imageType" arguments to BufferedImage; for example, 1
      * is TYPE_INT_RGB, 2 is TYPE_INT_ARGB, etc.
      *
      * This might be unnecessary now that I've reimplemented the
      * text renderer.  I need some new experiments on a Mac. */
    private int tripleBufferMode = 0;

    /** When true, we render frames as fast as possible and measure
      * the resulting frames per second. */
    private boolean fpsMeasurementMode = false;

    /** Number of frames rendered since entering FPS mode. */
    private int fpsFrameCount = 0;

    /** System.currentTimeMillis() when we entered FPS mode. */
    private long fpsStartMillis = 0;

    /** Last FPS measurement. */
    private String fpsMeasurement = null;

    /** Number of FPS samples reported.  This is useful because
      * the effects of the JIT mean the number naturally climbs
      * over time, so I need to know about how long I have been
      * running FPS measurement so I can pick a consistent point
      * to stop and consider the measurement final. */
    private int fpsSampleCount = 0;

    // ------------- public methods ---------------
    public DiagramController(Ded dedWindow)
    {
        this.setBackground(Color.WHITE);

        this.dedWindow = dedWindow;
        this.diagram = new Diagram();
        this.controllers = new ArrayList<Controller>();
        this.mode = Mode.DCM_SELECT;
        this.fileName = "";
        this.currentFileChooserDirectory = Util.getWorkingDirectoryFile();
        this.dirty = false;
        this.importedFile = false;
        this.imageCache = new HashMap<String, Image>();

        this.logMessages = new StringBuilder();
        this.log("Diagram Editor started at "+(new Date()));

        this.undoHistory = new UndoHistory(this.diagram,
            fmt("Created empty diagram"), this);
        this.undoHistoryWindow = new UndoHistoryWindow(this);

        String tbm = System.getenv("DED_TRIPLE_BUFFER");
        if (tbm != null) {
            try {
                this.tripleBufferMode = Integer.valueOf(tbm);
            }
            catch (NumberFormatException e) {
                this.log("invalid DED_TRIPLE_BUFFER value \""+tbm+
                         "\": "+Util.getExceptionMessage(e));
            }
        }
        this.log("DED_TRIPLE_BUFFER: "+this.tripleBufferMode);

        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addKeyListener(this);
        this.addComponentListener(this);
        this.addFocusListener(this);

        this.setFocusable(true);

        // I want to see Tab and Shift Tab keys in my KeyListener.
        this.setFocusTraversalKeysEnabled(false);
    }

    public Diagram getDiagram()
    {
        return this.diagram;
    }

    /** Font to use for all text in the diagram area. */
    public BitmapFont getDiagramFont()
    {
        return this.dedWindow.diagramBitmapFont;
    }

    @Override
    public void paint(Graphics g)
    {
        // Swing JPanel is double buffered already, but that is not
        // sufficient to avoid rendering bugs on Apple computers
        // with HiDPI/Retina displays.  This is an attempt at a
        // hack that might circumvent it, effectively triple-buffering
        // the rendering step.
        if (this.tripleBufferMode != 0) {
            // The idea here is if I create an in-memory image with no
            // initial association with the display, whatever hacks Apple
            // has added should not kick in, and I get unscaled pixel
            // rendering.
            BufferedImage bi;

            if (this.tripleBufferMode == -1) {
                // This is not right because we might be drawing on a
                // different screen than the "default" screen.  Also, I
                // am worried that a "compatible" image might be one
                // subject to the scaling effects I'm trying to avoid.
                GraphicsDevice gd =
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                GraphicsConfiguration gc = gd.getDefaultConfiguration();
                bi = gc.createCompatibleImage(this.getWidth(), this.getHeight());
            }
            else {
                // This is not ideal because the color representation
                // for this hidden image may not match that of the display,
                // necessitating a conversion during 'drawImage'.
                try {
                    bi = new BufferedImage(this.getWidth(), this.getHeight(),
                                           this.tripleBufferMode);
                }
                catch (IllegalArgumentException e) {
                    // This would happen if 'tripleBufferMode' were invalid.
                    if (this.tripleBufferMode == BufferedImage.TYPE_INT_ARGB) {
                        // I don't know how this could happen.  Re-throw.
                        this.log("creating a BufferedImage with TYPE_INT_ARGB failed: "+Util.getExceptionMessage(e));
                        this.log("re-throwing exception...");
                        throw e;
                    }
                    else {
                        // Change it to something known to be valid and try again.
                        this.log("creating a BufferedImage with imageType "+this.tripleBufferMode+
                                 " failed: "+Util.getExceptionMessage(e));
                        this.log("switching type to TYPE_INT_ARGB and re-trying...");
                        this.tripleBufferMode = BufferedImage.TYPE_INT_ARGB;
                        this.paint(g);
                        return;
                    }
                }
            }
            Graphics g2 = bi.createGraphics();
            this.innerPaint(g2);
            g2.dispose();

            g.drawImage(bi, 0, 0, null /*imageObserver*/);
        }
        else {
            this.innerPaint(g);
        }

        if (this.fpsMeasurementMode) {
            // Immediately trigger another paint cycle.
            this.repaint();
        }
    }

    /** The core of the paint routine, after we decide whether to interpose
      * another buffer. */
    private void innerPaint(Graphics g)
    {
        super.paint(g);

        // Filename label.
        if (this.diagram.drawFileName && !this.fileName.isEmpty()) {
            String name = new File(this.fileName).getName();

            BitmapFont font = this.getDiagramFont();
            int x = fileNameLabelMargin;
            int y = fileNameLabelMargin + font.getAscent();
            font.drawString(g, name, x, y);
            y += font.getUnderlineOffset() + 1 /*...*/;
            g.drawLine(x, y, x + font.stringWidth(name), y);
        }

        // Controllers.
        for (Controller c : this.controllers) {
            if (c.isSelected()) {
                c.paintSelectionBackground(g);
            }
            c.paint(g);
        }

        // Description of current mode.
        String modeDescription = this.mode.description;

        // Lasso rectangle.
        if (this.mode == Mode.DCM_RECT_LASSO) {
            Rectangle r = this.getLassoRect();
            g.drawRect(r.x, r.y, r.width, r.height);
            modeDescription += " from ("+r.x+","+r.y+
                               ") to ("+(r.x+r.width)+","+(r.y+r.height)+
                               ") which is "+r.width+"x"+r.height;
        }

        // Current focused Component.
        if (debugFocus) {
            KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            Component fo = kfm.getFocusOwner();
            g.drawString("Focus: "+fo, 3, this.getHeight() - 22);
        }

        // Mode label.
        if (this.mode != Mode.DCM_SELECT) {
            g.drawString("Mode: " + modeDescription, 3, this.getHeight()-4);
        }
        else if (this.fpsMeasurementMode) {
            this.fpsFrameCount++;
            long current = System.currentTimeMillis();
            long millis = current - this.fpsStartMillis;
            if (millis > 1000) {
                // Update the FPS measurement with the results for this
                // interval.
                this.fpsSampleCount++;
                this.fpsMeasurement = "FPS: "+this.fpsFrameCount+
                    " (millis="+millis+", samples="+this.fpsSampleCount+")";

                // Reset the counters.
                this.fpsStartMillis = current;
                this.fpsFrameCount = 0;
            }
            g.drawString(this.fpsMeasurement + " (Ctrl+G to stop)",
                         3, this.getHeight()-4);
        }
    }

    /** Return the set of currently selected controllers as a freshly
      * created set object. */
    protected HashSet<Controller> getSelectionSet()
    {
        HashSet<Controller> ret = new HashSet<Controller>();

        for (Controller c : this.controllers) {
            if (c.isSelected()) {
                ret.add(c);
            }
        }

        return ret;
    }

    /** Set the selection state of all of the controllers in 'set' to 'state'. */
    protected void setMultipleSelected(Set<Controller> set, SelectionState state)
    {
        for (Controller c : set) {
            c.setSelected(state);
        }
    }

    /** Deselect all controllers and return the number that were previously selected. */
    public int deselectAll()
    {
        // Get selection set first, so we only change state after iterating.
        HashSet<Controller> toDeselect = getSelectionSet();

        // Unselect them.
        setMultipleSelected(toDeselect, SelectionState.SS_UNSELECTED);

        return toDeselect.size();
    }

    /** Return the top-most Controller that contains 'point' and satisfies 'filter'
      * (if it is not null), or null if none does. */
    private Controller hitTest(Point point, ControllerFilter filter)
    {
        // Go backwards for top-down order.
        for (int i = this.controllers.size()-1; i >= 0; i--) {
            Controller c = this.controllers.get(i);

            if (filter != null && filter.satisfies(c) == false) {
                continue;
            }

            if (c.boundsContains(point)) {
                return c;
            }
        }
        return null;
    }

    /** Hit test restricted to Entities. */
    private EntityController hitTestEntity(Point pt)
    {
        return (EntityController)hitTest(pt, new ControllerFilter() {
            public boolean satisfies(Controller c) {
                return c instanceof EntityController;
            }
        });
    }

    /** Hit test restricted to Inheritances. */
    private InheritanceController hitTestInheritance(Point pt)
    {
        return (InheritanceController)hitTest(pt, new ControllerFilter() {
            public boolean satisfies(Controller c) {
                return c instanceof InheritanceController;
            }
        });
    }

    /** This method is passed some of the input events.  I'm using it
      * as a convenient instrumentation point while experimenting with
      * and fixing bugs in Abbot.  In production usage, it should do
      * nothing. */
    private void eventReceived(AWTEvent e)
    {
        //System.out.println(e.toString());
    }

    /** Return e.getPoint(), except snapped to SNAP_DIST if shift not held. */
    public Point getSnappedPoint(MouseEvent e)
    {
        Point p = e.getPoint();

        // Snap if Shift not held.
        if (!SwingUtil.shiftPressed(e)) {
            p = GeomUtil.snapPoint(p, SNAP_DIST);
        }

        return p;
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        this.eventReceived(e);

        switch (this.mode) {
            case DCM_SELECT: {
                // Clicked a controller?
                Controller c = this.hitTest(e.getPoint(), null);
                if (c == null) {
                    // Missed controls, start a lasso selection
                    // if left button.
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        this.lassoOriginalSelected.clear();
                        if (SwingUtil.controlPressed(e)) {
                            // Control is pressed.  Keep current selections.
                            this.lassoOriginalSelected.addAll(this.getAllSelected());
                        }
                        else {
                            if (this.deselectAll() > 0) {
                                this.repaint();
                            }
                        }

                        // Enter lasso mode.
                        setMode(Mode.DCM_RECT_LASSO);
                        this.lassoStart = this.lassoEnd = e.getPoint();
                    }
                }
                else {
                    c.mousePressed(e);
                }
                break;
            }

            case DCM_CREATE_RELATION: {
                // Make a Relation that starts and ends at the current location.
                Point startPoint = this.getSnappedPoint(e);
                RelationEndpoint start = this.getRelationEndpoint(startPoint);
                RelationEndpoint end = new RelationEndpoint(start);
                end.arrowStyle = ArrowStyle.AS_FILLED_TRIANGLE;
                Relation r = new Relation(start, end);
                this.diagram.relations.add(r);

                // Build a controller and select it.
                RelationController rc = this.buildRelationController(r);
                this.selectOnly(rc);

                // Drag the end point while the mouse button is held.
                this.beginDragging(rc.getEndHandle(), startPoint);

                this.diagramChanged(
                    fmt("Create relation at (%1$d,%2$d)", startPoint.x, startPoint.y));
                break;
            }

            case DCM_CREATE_ENTITY: {
                // This does respect snap, but that happens inside 'createEntityAt'.
                EntityController.createEntityAt(this, e.getPoint());
                this.setMode(Mode.DCM_SELECT);
                this.diagramChanged(
                    fmt("Create entity at (%1$d,%2$d)", e.getPoint().x, e.getPoint().y));
                break;
            }

            case DCM_CREATE_INHERITANCE: {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    this.createInheritanceAt(e.getPoint());
                }
                break;
            }

            case DCM_DRAGGING:
            case DCM_RECT_LASSO:
                // These modes are entered with a mouse press and
                // exited with mouse release, so we should not get
                // a mouse press while already in such a mode.
                // Ignore it if it happens.
                break;
        }
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        this.eventReceived(e);

        if (this.mode == Mode.DCM_DRAGGING) {
            this.selfCheck();

            // Where are we going to move the dragged object's main point?
            Point destLoc = GeomUtil.subtract(e.getPoint(), this.dragOffset);

            // Snap if Shift not held.
            if (!SwingUtil.shiftPressed(e)) {
                destLoc = GeomUtil.snapPoint(destLoc, SNAP_DIST);
            }

            if (this.dragging.isSelected()) {
                // How far are we going to move the dragged object?
                Point delta = GeomUtil.subtract(destLoc, this.dragging.getLoc());

                // Move all selected controls by that amount.
                HashSet<Controller> selControllers = this.getSelectionSet();
                for (Controller c : selControllers) {
                    Point cur = c.getLoc();
                    c.dragTo(GeomUtil.add(cur, delta));
                }

                this.dragCommandDescription =
                    fmt("Drag %1$d elements to (%2$d,%3$d)",
                        selControllers.size(),
                        destLoc.x,
                        destLoc.y);
            }
            else {
                // Dragging item is not selected; must be a resize handle.
                this.dragging.dragTo(destLoc);
                this.dragCommandDescription =
                    fmt("Adjust shape by moving handle to (%1$d,%2$d)",
                        destLoc.x,
                        destLoc.y);
            }

            this.repaint();
        }

        if (this.mode == Mode.DCM_RECT_LASSO) {
            this.lassoEnd = e.getPoint();
            this.selectAccordingToLasso();
            this.repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        this.eventReceived(e);

        // Click+drag should only be initiated with left mouse button, so ignore
        // release of others.
        if (!SwingUtilities.isLeftMouseButton(e)) {
            return;
        }

        if (this.mode == Mode.DCM_DRAGGING && this.dragCommandDescription != null) {
            this.diagramChanged(this.dragCommandDescription);
        }

        if (this.mode == Mode.DCM_DRAGGING || this.mode == Mode.DCM_RECT_LASSO) {
            this.setMode(Mode.DCM_SELECT);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        this.eventReceived(e);

        // Double-click on control to edit it.
        if (SwingUtilities.isLeftMouseButton(e) && (e.getClickCount() == 2)) {
            Controller c = this.hitTest(e.getPoint(), null);
            if (c != null) {
                c.edit();
            }
        }
    }

    // MouseListener methods I do not care about.
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    // MouseMotionListener events I do not care about.
    @Override public void mouseMoved(MouseEvent e) {
        this.eventReceived(e);

        // Keep the focus display up to date if desired.
        if (debugFocus && e.getX() < 10) {
            this.repaint();
        }
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        this.eventReceived(e);

        // Note: Some of the key bindings shown in the help dialog
        // have been moved to the menu created in Ded.java.

        if (SwingUtil.controlPressed(e)) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_F:
                    if (this.fpsMeasurementMode == false) {
                        this.fpsMeasurementMode = true;
                        this.fpsStartMillis = System.currentTimeMillis();
                        this.fpsFrameCount = 0;
                        this.fpsMeasurement = "(FPS measurement in progress)";
                        this.repaint();
                    }
                    else {
                        // FPS mode already active.  We get lots of key
                        // events due to auto-repeat.
                    }
                    break;

                case KeyEvent.VK_G:
                    this.fpsMeasurementMode = false;
                    break;
            }
            return;
        }

        if (SwingUtil.altPressed(e)) {
            return;
        }

        // See if the selected controller wants this keypress.
        Controller sel = this.getUniqueSelected();
        if (sel != null) {
            if (sel.keyPressed(e)) {
                return;
            }
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_X:
                if (SwingUtil.shiftPressed(e)) {
                    assert(false);     // Make sure assertions are enabled.
                }
                else {
                    throw new RuntimeException("Test exception/error message.");
                }
                break;

            case KeyEvent.VK_H:
                this.showHelpBox();
                break;

            case KeyEvent.VK_TAB:
                this.selectNextController(!SwingUtil.shiftPressed(e) /*forward*/);
                break;
        }
    }

    /** Compare Controllers by their 'getLoc()'  point, ordering them
      * first top to bottom then left to right. */
    public static class ControllerLocationComparator implements Comparator<Controller> {
        @Override
        public int compare(Controller a, Controller b)
        {
            Point aLoc = a.getLoc();
            Point bLoc = b.getLoc();

            int cmp = Util.compareInts(aLoc.y, bLoc.y);
            if (cmp != 0) {
                return cmp;
            }

            cmp = Util.compareInts(aLoc.x, bLoc.x);
            if (cmp != 0) {
                return cmp;
            }

            return 0;
        }
    }

    /** If a controller is selected, cycle to either the next or
      * previous depending on 'forward' (true means next).
      *
      * Otherwise, select the first controller if there is one. */
    public void selectNextController(boolean forward)
    {
        // Make a list of cyclable controllers.  In particular, we need
        // to ignore resize handles.
        ArrayList<Controller> controllerCycle = new ArrayList<Controller>();
        for (Controller c : this.controllers) {
            if (c.wantLassoSelection()) {
                controllerCycle.add(c);
            }
        }

        // The insertion order isn't very meaningful and cannot be
        // easily changed by the user.  So, instead, sort by the
        // controllers' locations to make the cycle order more
        // predictable.
        Collections.sort(controllerCycle, new ControllerLocationComparator());

        // Locate the currently selected controller in the sorted cycle.
        int curSelIndex = controllerCycle.indexOf(this.getUniqueSelected());
        if (curSelIndex == -1) {
            // Nothing selected, start with first controller if
            // there is one.
            if (!controllerCycle.isEmpty()) {
                selectOnly(controllerCycle.get(0));
            }
            return;
        }

        // Compute index of next to select, cycling as necessary.
        // The extra size() term is to ensure the dividend does not
        // become negative.
        int nextSelIndex =
            (controllerCycle.size() + curSelIndex + (forward? +1 : -1)) %
            controllerCycle.size();

        // Select it.
        selectOnly(controllerCycle.get(nextSelIndex));
    }

    /** Show the box with the key bindings. */
    public void showHelpBox()
    {
        JOptionPane.showMessageDialog(this, helpMessage,
            "Diagram Editor Keybindings",
            JOptionPane.INFORMATION_MESSAGE);
    }

    /** Show a window with the log. */
    public void showLogWindow()
    {
        SwingUtil.logFileMessageBox(this, this.logMessages.toString(), "Diagram Editor Log");
    }

    /** Clear the current diagram. */
    public void newFile()
    {
        if (this.isDirty()) {
            int res = JOptionPane.showConfirmDialog(this,
                "There are unsaved changes.  Create new diagram anyway?",
                "New Diagram Confirmation", JOptionPane.YES_NO_OPTION);
            if (res != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // Reset file status.
        this.dirty = false;
        this.setFileName("");

        // Clear the diagram.
        this.setDiagram(new Diagram());
        this.undoHistory = new UndoHistory(this.diagram,
            fmt("Started a new, empty diagram"), this);
        this.undoHistoryWindow.updateHistory();
    }

    /** Change the Diagram to an entirely new one. */
    private void setDiagram(Diagram newDiagram)
    {
        this.diagram = newDiagram;

        // Sizing is achieved by specifying a preferred size for
        // the content pane, then packing other controls and the
        // window border stuff around it.
        this.setPreferredSize(this.diagram.windowSize);

        // When playing around with undo/redo involving resize,
        // sometimes 'setPreferredSize' is not enough.  I don't
        // understand why; it seems dependent on the specific size
        // values?  Perhaps an odd interaction with the window
        // manager's snap behavior?  Calling 'setSize' as well seems
        // to fix the problem.
        this.setSize(this.diagram.windowSize);

        this.dedWindow.pack();

        this.setBackground(this.diagram.getBackgroundColor());

        this.rebuildControllers();
        this.dedWindow.updateMenuState();
        this.repaint();
    }

    /** Prompt for a file name to load, then replace the current diagram with it. */
    public void loadFromFile()
    {
        if (this.isDirty()) {
            int res = JOptionPane.showConfirmDialog(this,
                "There are unsaved changes.  Load new diagram anyway?",
                "Load Confirmation", JOptionPane.YES_NO_OPTION);
            if (res != JOptionPane.YES_OPTION) {
                return;
            }
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(this.currentFileChooserDirectory);
        chooser.addChoosableFileFilter(
            new FileNameExtensionFilter(
                "Diagram and ER Editor Files (.ded, .png, .er)", "ded", "png", "er"));
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            this.currentFileChooserDirectory = chooser.getCurrentDirectory();
            this.loadFromNamedFile(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    /** Load from the given file, replacing the current diagram. */
    public void loadFromNamedFile(String name)
    {
        if (!new File(name).exists()) {
            SwingUtil.errorMessageBox(this, "File \""+name+"\" does not exist.");
            return;
        }

        try {
            Diagram d;

            // See if this is a PNG file with a DED-created comment.
            if (name.endsWith(".png") || name.endsWith(".PNG")) {
                d = loadFromPNG(name);
                if (d == null) {
                    return;     // canceled, or error already reported
                }
            }
            else {
                // For compatibility with the C++ implementation, start
                // by trying to read it in the ER format.
                d = Diagram.readFromERFile(name);
                if (d != null) {
                    // Success; but we need to indicate that the file will
                    // be saved in a different format, lest people lose
                    // their original file unexpectedly.
                    this.importedFile = true;
                }
                else {
                    // Read the file as JSON.
                    d = Diagram.readFromFile(name);
                    this.importedFile = false;
                }

                // Success.  Update file name.
                this.dirty = false;
                this.setFileName(name);
            }

            // Swap in the new diagram and rebuild the UI for it.
            this.setDiagram(d);
            this.undoHistory = new UndoHistory(this.diagram,
                fmt("Loaded file \"%1$s\"", name), this);
            this.undoHistoryWindow.updateHistory();
        }
        catch (Exception e) {
            this.exnErrorMessageBox("Error while reading \""+name+"\"", e);
        }
    }

    /** Try to load a diagram by reading the JSON out of the comment
      * section of the PNG file 'pngName'.  If that succeeds, return
      * non-null, and also set:
      *
      *   * this.importedFile
      *   * this.dirty
      *   * this.fileName
      *
      * Return null if this failed but we already explained the problem
      * to the user, or the user cancels; or throw an exception otherwise. */
    private Diagram loadFromPNG(String pngName)
        throws Exception
    {
        // Get the name of the image source file.
        String sourceFileName = pngName.substring(0, pngName.length()-4);
        File sourceFile = new File(sourceFileName);
        if (sourceFile.exists()) {
            if (SwingUtil.confirmationBox(this,
                    "You are trying to open a diagram PNG file \""+pngName+
                        "\", but the source DED file \""+sourceFileName+
                        "\" is right next to it.  Usually, you should open "+
                        "the source file instead.  Otherwise, that source file "+
                        "will be overwritten when you next save.  Are you sure you want to "+
                        "read the diagram out of the PNG comment?",
                    "Are you sure?", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            {
                return null;
            }
        }

        // Try to read the comment from the file.  If the file is corrupt
        // or cannot be read, this will throw.  But if the comment is merely
        // absent, then this will return null.
        String comment = ImageFileUtil.getPNGComment(new File(pngName));
        if (comment == null || comment.isEmpty()) {
            SwingUtil.errorMessageBox(this,
                "The PNG file \""+pngName+"\" does not contain a comment, "+
                "so it is not possible to read the diagram source from it.");
            return null;
        }

        // Do a preliminary sanity check on the comment.
        if (!comment.startsWith("{")) {
            SwingUtil.errorMessageBox(this,
                "The PNG file \""+pngName+"\" contains a comment, "+
                "but it does not begin with '{', so it is not a comment "+
                "created by DED, "+
                "so it is not possible to read the diagram source from it.");
            return null;
        }

        // Try parsing the comment as diagram JSON.
        Diagram d;
        try {
            d = Diagram.readFromReader(new StringReader(comment));
        }
        catch (Exception e) {
            this.exnErrorMessageBox(
                "The PNG file \""+pngName+"\" has a comment that might "+
                "have been created by DED, but parsing that comment as "+
                "a diagram source file failed", e);
            return null;
        }

        // That worked.  Update the editor state variables.
        this.importedFile = false;

        // Note: We chop off ".png" and treat that as the name for
        // subsequent saves.
        this.setFileName(sourceFileName);

        // The file is not considered dirty because they are no
        // unsaved changes, even though the next save may cause
        // on-disk changes due to overwriting a source file if the
        // user ignored the warning above.
        this.dirty = false;

        return d;
    }

    /** Rebuild all the controllers from 'diagram'. */
    private void rebuildControllers()
    {
        this.controllers.clear();

        for (Entity e : this.diagram.entities) {
            this.buildEntityController(e);
        }

        for (Relation r : this.diagram.relations) {
            this.buildRelationController(r);
        }

        for (Inheritance inh : this.diagram.inheritances) {
            this.buildInheritanceController(inh);
        }

        this.setMode(Mode.DCM_SELECT);
    }

    /** Prompt user for file name and save to it. */
    public void chooseAndSaveToFile()
    {
        // Prompt for a file name, confirming if the file already exists.
        String result = this.fileName;
        while (true) {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(this.currentFileChooserDirectory);
            chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("Diagram Editor Files (.ded)", "ded"));
            int res = chooser.showSaveDialog(this);
            if (res != JFileChooser.APPROVE_OPTION) {
                return;
            }
            this.currentFileChooserDirectory = chooser.getCurrentDirectory();
            result = chooser.getSelectedFile().getAbsolutePath();

            if (new File(result).exists()) {
                res = JOptionPane.showConfirmDialog(
                    this,
                    "A file called \""+result+"\" already exists.  Overwrite it?",
                    "Confirm Overwrite",
                    JOptionPane.YES_NO_OPTION);
                if (res != JOptionPane.YES_OPTION) {
                    continue;      // Ask again.
                }
            }

            break;
        }

        // Save to the chosen file.
        this.saveToNamedFile(result);
    }

    /** Save to the current file name.  If there is no file name,
      * prompt for a name. */
    public void saveCurrentFile()
    {
        if (this.fileName.isEmpty()) {
            this.chooseAndSaveToFile();
        }
        else {
            if (this.importedFile) {
                int res = SwingUtil.confirmationBox(
                    this,
                    "This diagram was loaded from \""+this.fileName+
                        "\", which uses the old binary ER format from the "+
                        "C++ ERED implementation.  If you save the file, it "+
                        "will be overwritten with the new JSON-based format "+
                        "used by the Java-based Diagram Editor, which the "+
                        "C++ ERED cannot read.\n"+
                        "\n"+
                        "In order to avoid confusion, it is probably best to "+
                        "save the new file with the \".ded\" extension rather "+
                        "than the traditional \".er\" extension so that others "+
                        "will know to use Ded to read it.\n"+
                        "\n"+
                        "Overwrite with the new format anyway?",
                    "Confirm Overwrite of Imported File",
                    JOptionPane.YES_NO_OPTION);
                if (res != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            this.saveToNamedFile(this.fileName);
        }
    }

    /** Save to the specified file. */
    private void saveToNamedFile(String fname)
    {
        if (fname.toLowerCase(Locale.ENGLISH).endsWith(".png")) {
            this.errorMessageBox(
                "The file name \""+fname+"\" is invalid because "+
                "you cannot save a diagram directly as a \".png\" file.  "+
                "Instead, give it a \".ded\" extension, and when that file is "+
                "saved, you will automatically get a \".ded.png\" file as well.");
            return;
        }

        try {
            this.diagram.saveToFile(fname);
        }
        catch (Exception e) {
            this.exnErrorMessageBox("Error while saving \""+fname+"\"", e);
            return;
        }

        // If it worked, remember the new name.
        this.dirty = false;
        this.importedFile = false;
        this.setFileName(fname);

        // Additionally, always export to PNG.
        String pngFname = fname+".png";
        try {
            // I will save the document source JSON as a comment in the image
            // file so if the source gets separated, I can still edit
            // the image.  One place this really helps is with diagrams
            // on a wiki: there is no easy way to upload both an image
            // and its source, nor even uninterpreted source files alone
            // for that matter.  It also helps with email attachments,
            // where again it is awkward to send pairs of files.

            // First, get the JSON as a string.
            String comment = this.diagram.toJSONString();

            // Now, this string might contain non-ASCII characters inside
            // the JSON strings.  They need to be changed to use JSON
            // escapes to conform to the requirements of comments in PNG
            // files.
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < comment.length(); i++) {
                char c = comment.charAt(i);
                if (c >= 127) {
                    // Render this using a JSON escape sequence.  (We
                    // simply assume that non-ASCII characters will only
                    // appear inside quoted strings.)
                    //
                    // JSON escapes use UTF-16 code units, with all the
                    // surrogate pair ugliness, just like Java Strings,
                    // so there is no transformation to do on them.
                    sb.append(String.format("\\u%04X", (int)c));
                }
                else {
                    // Note that 'c' here will be printable because the
                    // procedure for rendering JSON as a string already
                    // maps the control characters to escape sequences.
                    sb.append(c);
                }
            }

            // Write the image to the PNG file, including with the comment.
            writeToPNG(new File(pngFname), sb.toString());
        }
        catch (Exception e) {
            this.exnErrorMessageBox(
                "The primary diagram file \""+fname+"\" was saved successfully, "+
                "but exporting the PNG to \""+pngFname+"\" failed", e);
        }
    }

    /** Change the recent file name to 'name', updating window title too. */
    private void setFileName(String name)
    {
        this.fileName = name;
        this.updateWindowTitle();

        // Changing the file name affects the drawn name in the
        // main editing area (if enabled).
        this.repaint();
    }

    /** Write the diagram in PNG format to 'file' with an optional comment.
      * The comment must only use ASCII characters. */
    public void writeToPNG(File file, String comment)
        throws Exception
    {
        // For a large-ish diagram, this operation takes ~200ms.  For now,
        // I will just acknowledge the delay.  An idea for the future is
        // to add a status bar to the UI, then do the export work in a
        // separate thread, with an indicator in the status bar that will
        // reflect when the operation completes.  I consider it important
        // for the user to know when it finishes so if it takes a long
        // time, they don't in the meantime go copy or view the partially
        // written image.
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            // Based on code from:
            // http://stackoverflow.com/questions/5655908/export-jpanel-graphics-to-png-or-gif-or-jpg

            // First, render the image to an in-memory image buffer.
            BufferedImage bi =
                new BufferedImage(this.getSize().width, this.getSize().height,
                                  BufferedImage.TYPE_INT_ARGB);
            Graphics g = bi.createGraphics();
            this.paintWithoutSelectionsShowing(g);
            g.dispose();

            // Now, write that image to a file in PNG format.
            String warning = ImageFileUtil.writeImageToPNGFile(bi, file, comment);
            if (warning != null) {
                SwingUtil.warningMessageBox(this,
                    "File save completed successfully, but while exporting to PNG, "+
                    "there was a warning: "+warning);
            }
        }
        finally {
            this.setCursor(Cursor.getDefaultCursor());
        }
    }

    /** Paint diagram to 'g', except temporarily deselect everything
      * first so that the selection indicators do not not appear. */
    protected void paintWithoutSelectionsShowing(Graphics g)
    {
        // Turn off selections.
        HashSet<Controller> originalSelection = this.getSelectionSet();
        setMultipleSelected(originalSelection, SelectionState.SS_UNSELECTED);
        try {
            // Paint now that selections are turned off.
            //
            // This uses 'innerPaint' to bypass the additional buffering
            // logic, which is unnecessary here since we are already
            // rendering to a hidden image to write to a file.
            this.innerPaint(g);
        }
        finally {
            // Restore selection state.
            setSelectionSet(originalSelection);
        }
    }

    /** Get and log some details related to display scaling, particularly
      * to help diagnose the graphics bugs on HiDPI/Retina displays. */
    public void logDisplayScaling()
    {
        // Based on code from
        // http://lubosplavucha.com/java/2013/09/02/retina-support-in-java-for-awt-swing/

        try {
            // Dump a bunch of possibly interesting JVM properties.
            String propertyNames[] = {
                "awt.toolkit",
                "java.awt.graphicsenv",
                "java.runtime.name",
                "java.runtime.version",
                "java.vendor",
                "java.version",
                "java.vm.name",
                "java.vm.vendor",
                "java.vm.version",
            };
            for (String name : propertyNames) {
                this.log("property "+name+": "+System.getProperty(name));
            }

            // Try a property specific to the Apple JVM.
            this.log("apple.awt.contentScaleFactor: " +
                Toolkit.getDefaultToolkit().getDesktopProperty("apple.awt.contentScaleFactor"));

            // Try something specific to OpenJDK.  Here, we
            // reflectively query some private field.  Yuck.
            GraphicsDevice gd =
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            try {
                Field field = gd.getClass().getDeclaredField("scale");
                field.setAccessible(true);
                this.log("GraphicsEnvironment.scale: "+field.get(gd));
            }
            catch (NoSuchFieldException e) {
                this.log("GraphicsEnvironment does not have a 'scale' field");
            }

            // Check some details of "compatible" images.
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            BufferedImage bi = gc.createCompatibleImage(64,64);
            ColorModel cm = bi.getColorModel();
            this.log("compatible image color model: "+cm);

            // Do the same for a specific imageType that seems to be
            // commonly used, and that I am using when saving to PNG.
            bi = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            cm = bi.getColorModel();
            this.log("TYPE_INT_ARGB color model: "+cm);

            // And one more.
            bi = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
            cm = bi.getColorModel();
            this.log("TYPE_INT_RGB color model: "+cm);
        }
        catch (Exception e) {
            this.log("exception during logDisplayScaling(): " +
                Util.getExceptionMessage(e));
            this.logNoNewline(Util.getExceptionStackTrace(e));
        }
    }

    /** Called when the diagram has been changed by a top-level user
      * action.  The action performed the user is described by
      * 'command', which is used to label the new diagram
      * in the undo/redo history.  It should grammatically be a
      * command (imperative) spoken from the user's perspective,
      * with the diagram editor software being commanded to act.
      *
      * This also does a repaint and sets the dirty bit. */
    public void diagramChanged(String command)
    {
        //System.out.println("Diagram changed: "+command);
        this.undoHistory.recordDiagramChange(this.diagram, command);
        this.undoHistoryWindow.updateHistory();
        this.populateRedoAlternateMenu();

        this.setDirty();
        this.repaint();
    }

    /** Set 'dirty' to true.  This is private because all other classes
      * are supposed to use 'diagramChanged'. */
    private void setDirty()
    {
        if (!this.dirty) {
            this.dirty = true;
            this.updateWindowTitle();
        }
    }

    /** Clear the dirty bit. */
    public void clearDirty()
    {
        if (this.dirty) {
            this.dirty = false;
            this.updateWindowTitle();
        }
    }

    /** Return true if 'dirty'. */
    public boolean isDirty()
    {
        return this.dirty;
    }

    /** Set the window title to match current state. */
    private void updateWindowTitle()
    {
        String title = Ded.windowTitle;

        if (!this.fileName.isEmpty()) {
            // Do not include the directory here.
            title += ": " + new File(this.fileName).getName();
        }

        if (this.importedFile) {
            title += " (imported)";
        }

        if (this.dirty) {
            title += " *";
        }

        this.dedWindow.setTitle(title);
    }

    // KeyListener methods I do not care about.
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}

    /** Change the UI mode to 'm', maintaining a few invariants in the process. */
    public void setMode(Mode m)
    {
        this.mode = m;

        if (m != Mode.DCM_DRAGGING) {
            if (this.dragging != null) {
                this.dragging.stopDragging();
            }
            this.dragging = null;
            this.dragOffset = new Point(0,0);
            this.dragCommandDescription = null;
        }

        if (m != Mode.DCM_RECT_LASSO) {
            this.lassoStart = this.lassoEnd = new Point(0,0);
            this.lassoOriginalSelected.clear();
        }

        switch (m) {
            default:
                // I tried crosshair for lasso, but that is too annoying
                // when just clicking in empty space.  I also tried the
                // "move" cursor for dragging, but that cursor blocks too
                // much of the view of the area right under what is being
                // moved, making precise positioning difficult.
                //
                // Basically, I don't really need a different cursor when
                // the mouse button is pressed because the user has already
                // initiated an action and is therefore aware that something
                // unusual is happening.  And in most other cases, I don't
                // need a special cursor because the effect of pressing the
                // mouse is fairly obvious already.
                this.setCursor(Cursor.getDefaultCursor());
                break;

            case DCM_CREATE_ENTITY:
            case DCM_CREATE_INHERITANCE:
            case DCM_CREATE_RELATION:
                // The crosshair here is not particularly suggestive of
                // what the mode does, but it is noticeably different,
                // which clues the user to the altered behavior.
                this.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                break;
        }

        this.selfCheck();
        this.repaint();
    }

    /** Construct a controller for 'e' and add it to 'this'. */
    private EntityController buildEntityController(Entity e)
    {
        EntityController ec = new EntityController(this, e);
        this.add(ec);
        return ec;
    }

    /** Construct a controller for 'r' and add it to 'this'. */
    private RelationController buildRelationController(Relation r)
    {
        RelationController rc = new RelationController(this, r);
        this.add(rc);
        return rc;
    }

    /** Construct a controller for 'inh' and add it to 'this'. */
    private InheritanceController buildInheritanceController(Inheritance inh)
    {
        InheritanceController ic = new InheritanceController(this, inh);
        this.add(ic);
        return ic;
    }

    /** If there is exactly one controller selected, return it; otherwise
      * return null. */
    public Controller getUniqueSelected()
    {
        Controller ret = null;

        for (Controller c : this.controllers) {
            if (c.isSelected()) {
                if (ret != null) {
                    return null;      // More than one is selected.
                }
                ret = c;
            }
        }

        return ret;
    }

    /** Get all selected controllers. */
    public IdentityHashSet<Controller> getAllSelected()
    {
        return this.findControllers(new ControllerFilter() {
            public boolean satisfies(Controller c) {
                return c.isSelected();
            }
        });
    }

    /** Edit the selected controller and associated entity, if any. */
    public void editSelected()
    {
        if (this.mode == Mode.DCM_SELECT) {
            Controller c = this.getUniqueSelected();
            if (c != null) {
                c.edit();
            }
            else {
                this.errorMessageBox(
                    "There must be exactly one thing selected to edit it.");
            }
        }
    }

    /** Filter diagram elements for those that are selected.   The data
      * members of this class record which elements are selected. */
    private static class SelectedElementFilter extends Diagram.ElementFilter {
        private IdentityHashSet<Entity>      selEntities =     new IdentityHashSet<Entity>();
        private IdentityHashSet<Inheritance> selInheritances = new IdentityHashSet<Inheritance>();
        private IdentityHashSet<Relation>    selRelations =    new IdentityHashSet<Relation>();

        /** Initialize from the set of selected controllers. */
        public SelectedElementFilter(ArrayList<Controller> selControllers)
        {
            for (Controller c : selControllers) {
                if (c instanceof EntityController) {
                    this.selEntities.add(((EntityController)c).entity);
                }
                if (c instanceof InheritanceController) {
                    this.selInheritances.add(((InheritanceController)c).inheritance);
                }
                if (c instanceof RelationController) {
                    this.selRelations.add(((RelationController)c).relation);
                }
            }
        }

        public boolean testEntity(Entity e)
        {
            return this.selEntities.contains(e);
        }

        public boolean testInheritance(Inheritance i)
        {
            return this.selInheritances.contains(i);
        }

        public boolean testRelation(Relation r)
        {
            return this.selRelations.contains(r);
        }
    }

    /** Copy the selected entities to the (application) clipboard. */
    public void copySelected()
    {
        this.innerCopySelected(false /*isCutCommand*/);
    }

    /** Copy the selected elements to the clipboard.  Return true
      * If this was successful.  'isCopyCommand' indicates that we
      * are performing a Cut rather than a Copy. */
    public boolean innerCopySelected(boolean isCutCommand)
    {
        // Get selected controllers.
        ArrayList<Controller> selControllers = new ArrayList<Controller>();
        for (Controller c : this.controllers) {
            if (c.isSelected()) {
                selControllers.add(c);
            }
        }
        if (selControllers.isEmpty()) {
            this.errorMessageBox(fmt(isCutCommand?
                "Nothing is selected to cut." :
                "Nothing is selected to copy."));
            return false;
        }

        // Build a filter based on 'selControllers'.
        SelectedElementFilter filter = new SelectedElementFilter(selControllers);

        // Make a copy of the subset of the diagram that is selected.
        Diagram copy = new Diagram(this.diagram, filter);

        // Make sure the Diagram is well-formed.
        try {
            // This is quadratic...
            copy.selfCheck();
        }
        catch (Throwable t) {
            this.errorMessageBox("Internal error: failed to create a well-formed copy: "+t);
            return false;
        }

        // Copy it as a string to the system clipboard.
        StringSelection data = new StringSelection(copy.toJSONString());
        Clipboard clipboard =
            Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(data, data);
        clipboard =
            Toolkit.getDefaultToolkit().getSystemSelection();
        if (clipboard != null) {
            clipboard.setContents(data, data);
        }

        return true;
    }

    /** Try to read the clipboard contents as a Diagram.  Return null and
      * display an error if we cannot. */
    private Diagram getClipboardAsDiagram()
    {
        // Let's start with the "selection" because if it is valid JSON
        // then it's probably what we want.
        String selErrorMessage = null;
        String selContents = null;
        Clipboard clipboard =
            Toolkit.getDefaultToolkit().getSystemSelection();
        if (clipboard == null) {
            // Probably we're running on something other than X Windows,
            // so we thankfully don't have to deal with the screwy
            // "selection" concept.
        }
        else {
            Transferable clipData = clipboard.getContents(clipboard);
            if (clipData == null) {
                selErrorMessage = "Nothing is in the \"selection\".";
            }
            else {
                try {
                    if (clipData.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        selContents = (String)(clipData.getTransferData(
                            DataFlavor.stringFlavor));

                        // Try to parse it.
                        try {
                            return Diagram.parseJSONString(selContents);
                        }
                        catch (JSONException e) {
                            selErrorMessage = "Could not parse selection data as Diagram JSON: "+e;
                        }
                    }
                    else {
                        selErrorMessage = "The data in the selection is not a string.";
                    }
                }
                catch (Exception e) {
                    selErrorMessage = "Error while retrieving selection data: "+e;
                }
            }
        }

        // Now try again with the clipboard.
        String clipErrorMessage = null;
        String clipContents = null;
        clipboard =
            Toolkit.getDefaultToolkit().getSystemClipboard();
        if (clipboard == null) {
            clipErrorMessage = "getSystemClipboard returned null?!";
        }
        else {
            Transferable clipData = clipboard.getContents(clipboard);
            if (clipData == null) {
                clipErrorMessage = "Nothing is in the clipboard.";
            }
            else {
                try {
                    if (clipData.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        clipContents = (String)(clipData.getTransferData(
                            DataFlavor.stringFlavor));

                        try {
                            return Diagram.parseJSONString(clipContents);
                        }
                        catch (JSONException e) {
                            clipErrorMessage = "Could not parse clipboard data as Diagram JSON: "+e;
                        }
                    }
                    else {
                        clipErrorMessage = "The data in the clipboard is not a string.";
                    }
                }
                catch (Exception e) {
                    clipErrorMessage = "Error while retrieving clipboard data: "+e;
                }
            }
        }

        // Both methods failed, and we have one or two error messages.
        // Decide what to show.
        if (selErrorMessage == null) {
            this.errorMessageBox(clipErrorMessage);
        }
        else if (selContents == null && clipContents != null) {
            this.errorMessageBox(clipErrorMessage);
        }
        else if (selContents != null && clipContents == null) {
            this.errorMessageBox(selErrorMessage);
        }
        else if (selContents.equals(clipContents)) {
            this.errorMessageBox(
                clipErrorMessage+
                "  (The selection and clipboard contents are the same.)");
        }
        else {
            this.errorMessageBox(
                "Failed to read either the selection or the clipboard.  "+
                 selErrorMessage+"  "+clipErrorMessage);
        }

        return null;
    }

    /** Insert the clipboard contents into the diagram. */
    public void pasteClipboard()
    {
        // Try to parse what is in the clipboard as Diagram JSON.
        Diagram copy = this.getClipboardAsDiagram();
        if (copy == null) {
            // Already showed the error dialog.
            return;
        }

        // Prepare to select only the new controllers.
        this.deselectAll();

        // Insert the new entities, making controllers for them.
        final IdentityHashSet<Controller> newControllers =
            new IdentityHashSet<Controller>();
        for (Entity e : copy.entities) {
            this.diagram.entities.add(e);
            newControllers.add(this.buildEntityController(e));
        }
        for (Inheritance i : copy.inheritances) {
            this.diagram.inheritances.add(i);
            newControllers.add(this.buildInheritanceController(i));
        }
        for (Relation r : copy.relations) {
            this.diagram.relations.add(r);
            newControllers.add(this.buildRelationController(r));
        }

        // Make exactly the new controllers selected.
        this.selectAccordingToFilter(new ControllerFilter() {
            public boolean satisfies(Controller c)
            {
                return newControllers.contains(c);
            }
        });

        this.diagramChanged(fmt("Paste %1$d elements", newControllers.size()));
    }

    /** Implement Edit|Cut. */
    public void cutSelected()
    {
        if (this.innerCopySelected(true /*isCutCommand*/)) {
            this.innerDeleteSelected(true /*isCutCommand*/);
        }
    }

    /** Delete the selected controllers and associated entities, if any. */
    public void deleteSelected()
    {
        this.innerDeleteSelected(false /*isCutCommand*/);
    }

    /** Delete the selected elements.  'isCutCommand' is true if this
      * is being done as part of a Cut rather than Delete. */
    public void innerDeleteSelected(boolean isCutCommand)
    {
        if (this.mode == Mode.DCM_SELECT) {
            IdentityHashSet<Controller> sel = this.getAllSelected();
            int n = sel.size();
            this.deleteControllers(sel);
            this.diagramChanged(isCutCommand?
                fmt("Cut %1$d elements", n) :
                fmt("Delete %1$d elements", n));
        }
    }

    /** Insert a new control point into the selected controller and
      * associated entity, if any and applicable. */
    public void insertControlPoint()
    {
        if (this.mode == Mode.DCM_SELECT) {
            Controller c = this.getUniqueSelected();
            if (c != null) {
                c.insertControlPoint();
            }
            else {
                this.errorMessageBox(
                    "There must be exactly one thing selected to insert a control point.");
            }
        }
    }

    /** Toggle selection state of one controller. */
    public void toggleSelection(Controller c)
    {
        if (c.isSelected()) {
            c.setSelected(SelectionState.SS_UNSELECTED);
        }
        else {
            c.setSelected(SelectionState.SS_SELECTED);
        }

        this.normalizeExclusiveSelect();
        this.repaint();
    }

    /** If exactly one controller is selected, set its state to
      * SS_EXCLUSIVE; otherwise, set all selected controllers to
      * SS_SELECTED. */
    public void normalizeExclusiveSelect()
    {
        this.selectAccordingToFilter(new ControllerFilter() {
            public boolean satisfies(Controller c) {
                return c.isSelected();
            }
        });
    }

    /** Select a single controller. */
    public void selectOnly(Controller c)
    {
        this.deselectAll();
        c.setSelected(SelectionState.SS_EXCLUSIVE);
        this.repaint();
    }

    /** Change mode to DCM_DRAGGING, dragging 'c' from 'pt'. */
    public void beginDragging(Controller c, Point pt)
    {
        this.dragging = c;
        this.dragOffset = GeomUtil.subtract(pt, c.getLoc());
        this.dragCommandDescription = null;
        c.beginDragging(pt);
        this.setMode(Mode.DCM_DRAGGING);
    }

    /** Check internal invariants, throw assertion failure if violated. */
    public void selfCheck()
    {
        if (this.mode == Mode.DCM_DRAGGING) {
            assert(this.dragging != null);
        }
        else {
            assert(this.dragging == null);
            assert(this.dragCommandDescription == null);
        }

        for (Controller c : this.controllers) {
            c.globalSelfCheck(this.diagram);
        }
    }

    /** Set the set of selected controllers to those in 'toSelect'. */
    protected void setSelectionSet(final Set<Controller> toSelect)
    {
        selectAccordingToFilter(new ControllerFilter() {
            public boolean satisfies(Controller c) {
                return toSelect.contains(c);
            }
        });
    }

    /** Set the set of selected controllers according to the filter. */
    protected void selectAccordingToFilter(ControllerFilter filter)
    {
        // During the loop, merely collect the sets of controllers
        // to select and deselect, then set the selection state afterward;
        // otherwise, we risk trying to modify the set of controllers
        // while it is being iterated over, since changing the selection
        // state of a controller can add or remove resize handles.
        HashSet<Controller> toSelect = new HashSet<Controller>();
        HashSet<Controller> toDeselect = new HashSet<Controller>();

        for (Controller c : this.controllers) {
            if (filter.satisfies(c)) {
                toSelect.add(c);
            }
            else if (c.isSelected()) {
                toDeselect.add(c);
            }
            else {
                // 'c' is not selected and should not be; just leave
                // it alone.
            }
        }

        // Deselect everything that should not be selected but
        // previously was.
        setMultipleSelected(toDeselect, SelectionState.SS_UNSELECTED);

        if (toSelect.size() == 1) {
            // Exclusively select the one lasso'd controller.  (Using
            // the "multi" call is merely syntactically convenient.)
            //
            // This will show resize controls.
            setMultipleSelected(toSelect, SelectionState.SS_EXCLUSIVE);
        }
        else {
            // Set state of all selected controls.
            setMultipleSelected(toSelect, SelectionState.SS_SELECTED);
        }
    }

    /** Return the current lasso rectangle. */
    protected Rectangle getLassoRect()
    {
        return new Rectangle(
            Math.min(this.lassoStart.x, this.lassoEnd.x),
            Math.min(this.lassoStart.y, this.lassoEnd.y),
            Math.abs(this.lassoEnd.x - this.lassoStart.x),
            Math.abs(this.lassoEnd.y - this.lassoStart.y));
    }

    /** Set the set of selected controllers according to the lasso. */
    protected void selectAccordingToLasso()
    {
        final Rectangle lasso = this.getLassoRect();

        this.selectAccordingToFilter(new ControllerFilter() {
            public boolean satisfies(Controller c)
            {
                if (!c.wantLassoSelection()) {
                    // Do not consider resize handles, mainly because doing so
                    // causes them to flicker: lassoing a single control adds
                    // resize handles, making the lasso no longer enclose just
                    // one control, which turns off resize handles, etc.
                    //
                    // I'm actually not sure how the C++ ered tool avoids this
                    // effect.  I do not see any avoidance in the code...
                    return false;
                }

                // Keep the original set, if any.
                if (DiagramController.this.lassoOriginalSelected.contains(c)) {
                    return true;
                }

                return c.boundsIntersects(lasso);
            }
        });
    }

    /** Add an active controller. */
    public void add(Controller c)
    {
        this.controllers.add(c);
        this.repaint();
    }

    /** Remove an active controller. */
    public void remove(Controller c)
    {
        this.controllers.remove(c);
        this.repaint();
    }

    /** Return true if 'c' is among the active controllers for this diagram. */
    public boolean contains(Controller c)
    {
        return this.controllers.contains(c);
    }

    /** Return set of matching controllers. */
    private IdentityHashSet<Controller> findControllers(ControllerFilter filter)
    {
        IdentityHashSet<Controller> ret = new IdentityHashSet<Controller>();
        for (Controller c : this.controllers) {
            if (filter.satisfies(c)) {
                ret.add(c);
            }
        }
        return ret;
    }

    /** Delete matching controllers. */
    public void deleteControllers(ControllerFilter filter)
    {
        // Get the set of matching controllers first; we cannot remove
        // them while searching due to iterator invalidation issues.
        IdentityHashSet<Controller> ctls = this.findControllers(filter);

        this.deleteControllers(ctls);
    }

    /** Delete specified controllers.  Do not call 'diagramChanged'. */
    public void deleteControllers(IdentityHashSet<Controller> ctls)
    {
        for (Controller c : ctls) {
            // This is inefficient, but oh well: before deleting, check
            // if it is still in 'controllers'.  It might have been removed
            // due to deletion of another controller in 'ctls'.
            if (this.controllers.contains(c)) {
                c.deleteSelfAndData(this.diagram);
            }
        }
    }

    /** Find EntityControllers fully contained in a rectangle. */
    public IdentityHashSet<EntityController> findEntityControllersInRectangle(Rectangle rect)
    {
        IdentityHashSet<EntityController> ret =
            new IdentityHashSet<EntityController>();
        for (Controller c : this.controllers) {
            if (c instanceof EntityController) {
                EntityController ec = (EntityController)c;
                if (rect.contains(ec.getRect())) {
                    ret.add(ec);
                }
            }
        }
        return ret;
    }

    /** Map a Point to a RelationEndpoint: either an Entity or Inheritance
      * that contains the Point, or else just the point itself. */
    public RelationEndpoint getRelationEndpoint(Point pt)
    {
        // Entity?
        EntityController ec = this.hitTestEntity(pt);
        if (ec != null) {
            return new RelationEndpoint(ec.entity);
        }

        // Inheritance?
        InheritanceController ic = this.hitTestInheritance(pt);
        if (ic != null) {
            return new RelationEndpoint(ic.inheritance);
        }

        // No suitable intersecting controller, use the point itself.
        return new RelationEndpoint(pt);
    }

    /** Create an inheritance based on the user's click on 'point'.
      * Calls 'diagramChanged' if the click is valid. */
    private void createInheritanceAt(Point point)
    {
        // Must be clicking on an entity.
        EntityController parent = this.hitTestEntity(point);
        if (parent == null) {
            this.errorMessageBox(
                "To create an inheritance, start by clicking "+
                "and dragging on the parent Entity.");
            return;
        }

        // Make an Inheritance connected to 'parent' and
        // current location.
        Inheritance inh = new Inheritance(parent.entity, false /*open*/, point);
        this.diagram.inheritances.add(inh);

        // Build and select a controller.
        InheritanceController ic = buildInheritanceController(inh);
        this.selectOnly(ic);

        // Drag it while the mouse button is pressed.
        this.beginDragging(ic, point);

        this.diagramChanged(fmt("Create inheritance at (%1$d,%2$d)", point.x, point.y));
    }

    /** Change the selected entities' fill colors to the named color. */
    public void setSelectedEntitiesFillColor(String colorName)
    {
        // Iterate over selected entities, changing their color.
        for (EntityController ec : this.getSelectedEntities()) {
            ec.entity.setFillColor(colorName);
        }

        this.diagramChanged(fmt("Set fill color to \"%1$s\"", colorName));
    }

    /** Change the selected elements' text colors to the named color. */
    public void setSelectedElementsTextColor(String colorName)
    {
        for (Controller c : this.getSelectionSet()) {
            c.setTextColor(colorName);
        }
        this.diagramChanged(fmt("Set text color to \"%1$s\"", colorName));
    }

    /** Change the selected elements' line colors to the named color. */
    public void setSelectedElementsLineColor(String colorName)
    {
        for (Controller c : this.getSelectionSet()) {
            c.setLineColor(colorName);
        }
        this.diagramChanged(fmt("Set line color to \"%1$s\"", colorName));
    }

    /** Return a sequence containing all of the selected entity controllers. */
    public ArrayList<EntityController> getSelectedEntities()
    {
        ArrayList<EntityController> ret = new ArrayList<EntityController>();
        for (Controller c : this.controllers) {
            if (c.isSelected() && c instanceof EntityController) {
                ret.add((EntityController)c);
            }
        }
        return ret;
    }

    /** Change the selected entities' shapes to the indicated shape. */
    public void setSelectedEntitiesShape(EntityShape shape)
    {
        for (EntityController ec : this.getSelectedEntities()) {
            ec.entity.setShapeAndDefaults(shape);
        }

        // Changing the shape can change the set of handles.
        this.normalizeExclusiveSelect();

        this.diagramChanged(fmt("Set shape to \"%1$s\"", shape.displayName));
    }

    /** Change the selected elements' text alignment. */
    public void setSelectedElementsNameTextAlign(TextAlign newAlign)
    {
        for (Controller c : this.getSelectionSet()) {
            c.setNameTextAlign(newAlign);
        }
        this.diagramChanged(fmt("Set text align to \"%1$s\"", newAlign));
    }

    /** Align selected entities according to 'ac'. */
    public void alignSelectedEntities(AlignCommand ac)
    {
        ArrayList<EntityController> ents = this.getSelectedEntities();
        if (ents.isEmpty()) {
            return;
        }

        // Get the most extreme value for the chosen edge.
        int extreme = ents.get(0).getEdge(ac.ee);
        for (EntityController ec : ents) {
            int lt = ec.getEdge(ac.ee);
            if (isMoreExtremeThan(lt, extreme, ac.ee.extremeIsGreater)) {
                extreme = lt;
            }
        }

        // Go over the list again, adjusting entities to all of the same
        // extreme value.
        for (EntityController ec : ents) {
            ec.setEdge(ac.ee, ac.resize, extreme);
        }

        this.diagramChanged(localize(ac.label));
    }

    /** Return true if 'a' is more extreme than 'b', respecting 'extremeIsGreater'. */
    private static boolean isMoreExtremeThan(int a, int b, boolean extremeIsGreater)
    {
        if (extremeIsGreater) {
            return a > b;
        }
        else {
            return a < b;
        }
    }

    public static enum SetAnchorCommand {
        // Set the anchor name to equal the entity name.
        SAC_SET_TO_ENTITY_NAME,

        // Clear the anchor name.
        SAC_CLEAR
    }

    /** Change the selected entities' anchor names in accordance with
      * 'command'. */
    public void setSelectedEntitiesAnchorName(SetAnchorCommand command)
    {
        int count = 0;
        for (EntityController ec : this.getSelectedEntities()) {
            switch (command) {
                case SAC_SET_TO_ENTITY_NAME:
                    ec.entity.anchorName = ec.entity.name;
                    break;

                case SAC_CLEAR:
                    ec.entity.anchorName = "";
                    break;
            }
            count++;
        }

        if (count == 0) {
            errorMessageBox("There were no selected entities?  Should not happen.");
            return;
        }
        else {
            SwingUtil.informationMessageBox(this, "Updated entities",
                "Updated the anchor names of "+count+" entities.");
        }

        this.diagramChanged(fmt(command == SetAnchorCommand.SAC_CLEAR?
            "Clear anchor names" :
            "Set anchor name to entity name"));
    }

    /** Show an error message dialog box with 'message'. */
    public void errorMessageBox(String message)
    {
        SwingUtil.errorMessageBox(this, message);
    }

    /** Show an error message arising from Exception 'e'. */
    public void exnErrorMessageBox(String context, Exception e)
    {
        errorMessageBox(context+": "+Util.getExceptionMessage(e));
    }

    /** Get an image for a given file name.  Save the result in an
      * image cache.  Return null if it cannot be loaded. */
    public Image getImage(String imageFileName)
    {
        // Consult the cache.
        if (this.imageCache.containsKey(imageFileName)) {
            return this.imageCache.get(imageFileName);  // Might be null.
        }

        // Try to load the image from disk.
        Image image = this.innerGetImage(imageFileName);

        // Cache the result, whatever it was, even if null.
        this.imageCache.put(imageFileName, image);

        return image;
    }

    /** Get an image for a file name, not using the cache.  If there
      * is problem, log it and return null. */
    private Image innerGetImage(String imageFileName)
    {
        // What directory will we interpret a relative name as relative to?
        File relativeBase;
        if (this.fileName.isEmpty()) {
            // Use current working directory.
            relativeBase = Util.getWorkingDirectoryFile();
        }
        else {
            // Use the directory containing the diagram file.
            relativeBase = new File(this.fileName).getParentFile();
        }

        // Combine the base with the specified file.
        File imageFile = Util.getFileRelativeTo(relativeBase, imageFileName);

        // Try to load the file.
        FileInputStream is = null;
        try {
            // I explicitly create my own InputStream because ImageIO
            // does a poor job of reporting file read errors.
            is = new FileInputStream(imageFile);
            Image image = ImageIO.read(is);
            if (image == null) {
                this.log("no registered image reader for: "+imageFile);
                return null;
            }

            this.log("loaded: "+imageFileName);
            return image;
        }
        catch (Exception e) {
            this.log("while loading \""+imageFileName+"\": "+Util.getExceptionMessage(e));
            return null;
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {/*ignore*/}
            }
        }
    }

    /** Clear the image cache and redraw so we reload the images. */
    public void reloadEntityImages()
    {
        this.log("image cache cleared at "+(new Date()));

        this.imageCache.clear();

        // Reloading images might alter size-locked entity sizes.
        for (Controller c : this.controllers) {
            c.updateAfterImageReload();
        }

        this.repaint();
    }

    /** Log the given one-line message.  A newline will be added after
      * it to mark its end in the total accumulated log. */
    public void log(String message)
    {
        this.logNoNewline(message+"\n");
    }

    /** Add a string to the accumulated log without adding a newline.
      * This is appropriate when the message may have multiple lines,
      * all of which are already newline-terminated. */
    public void logNoNewline(String message)
    {
        this.logMessages.append(message);
    }

    /** Show the custom colors dialog. */
    public void editCustomColors()
    {
        if (DiagramColorsDialog.exec(this, this.diagram)) {
            this.diagramChanged("Edit diagram colors");
        }
    }

    /** Show the diagram properties dialog. */
    public void editDiagramProperties()
    {
        if (DiagramPropertiesDialog.exec(this, this.diagram)) {
            this.setBackground(this.diagram.getBackgroundColor());
            this.diagramChanged("Edit diagram properties");
        }
    }

    /** If 'front' is true, make selected entities top-most so they
      * are displayed on top of all others, preserving the relative
      * order of the selected entities.  If 'front' is false, move
      * them to the bottom. */
    public void moveSelectedEntitiesToFrontOrBack(boolean front)
    {
        // Collect the selected entities and controllers in their
        // current relative order.  I need 'selEntities' so I can
        // call 'removeAll' and 'addAll' with them.
        ArrayList<Entity> selEntities = new ArrayList<Entity>();
        ArrayList<EntityController> selControllers = this.getSelectedEntities();
        for (EntityController ec : selControllers) {
            selEntities.add(ec.entity);
        }

        if (selEntities.isEmpty()) {
            this.errorMessageBox("There are no selected entities to move.");
            return;
        }

        // Move the entities in the diagram.
        this.diagram.entities.removeAll(selEntities);
        if (front) {
            this.diagram.entities.addAll(selEntities);
        }
        else {
            this.diagram.entities.addAll(0, selEntities);
        }

        // Move the controller as well.
        this.controllers.removeAll(selControllers);
        if (front) {
            this.controllers.addAll(selControllers);
        }
        else {
            this.controllers.addAll(0, selControllers);
        }

        this.selfCheck();
        this.diagramChanged(localize(front?
            "Move to front" :
            "Move to back"));
    }

    @Override
    public void componentResized(ComponentEvent e)
    {
        this.diagram.windowSize = this.getSize();

        // I do not set the dirty bit here because resizing is not a
        // very important action, and I'm having some trouble avoiding
        // making things dirty on startup.
    }

    // ComponentListener events I do not care about.
    @Override public void componentMoved(ComponentEvent e) {}
    @Override public void componentShown(ComponentEvent e) {}
    @Override public void componentHidden(ComponentEvent e) {}

    @Override
    public void focusGained(FocusEvent e)
    {
        if (debugFocus) {
            System.out.println("DiagramController focusGained: "+e);
            this.repaint();
        }
    }

    @Override
    public void focusLost(FocusEvent e)
    {
        if (debugFocus) {
            System.out.println("DiagramController focusLost: "+e);
            this.repaint();
        }
    }

    /** Return a resource image, using an internal cache. */
    public Image getResourceImage(String resourceName)
    {
        return this.dedWindow.resourceImageCache.getResourceImage(resourceName);
    }

    /** Swap the endpoint arrowheads for all selected relations. */
    public void swapSelectedRelationEndpoints()
    {
        for (Controller c : this.controllers) {
            if (c.isSelected() && c instanceof RelationController) {
                RelationController rc = (RelationController)c;
                rc.relation.swapArrows();
            }
        }
        this.diagramChanged(fmt("Swap relation endpoints"));
    }

    /** Set every selected entity that has a line style to style 'lds'. */
    public void setSelectedEntitiesLineDashStyle(LineDashStyle lds)
    {
        for (Controller c : this.controllers) {
            if (c.isSelected() && c instanceof RelationController) {
                RelationController rc = (RelationController)c;
                if (lds.dashStructureString == null) {
                    rc.relation.dashStructure = new ArrayList<Integer>();
                }
                else {
                    rc.relation.dashStructure =
                        RelationDialog.stringToDashStructure(lds.dashStructureString);
                }
            }
        }
        this.diagramChanged(fmt("Set line dash style to \"%1$s\"", lds.name));
    }

    /** Respond to Edit|Undo. */
    public void editUndo()
    {
        if (this.undoHistory.canUndo()) {
            this.setDiagram(this.undoHistory.undo());
            this.undoHistoryWindow.updateHistory();
        }
        else {
            this.errorMessageBox("Cannot undo because there is no further undo history.");
        }
    }

    /** Respond to Edit|Redo. */
    public void editRedo()
    {
        if (this.undoHistory.canRedo()) {
            this.setDiagram(this.undoHistory.redoMostRecent());
            this.undoHistoryWindow.updateHistory();
        }
        else {
            this.errorMessageBox("Cannot redo because there are no more redo states on this future.");
        }
    }

    /** Respond to "Edit|Redo Alternate" choice. */
    public void editRedoAlternate(int whichRedo)
    {
        this.setDiagram(this.undoHistory.redo(whichRedo));
        this.undoHistoryWindow.updateHistory();
    }

    /** Show the Undo History window. */
    public void showUndoHistory()
    {
        // Move the undo history to the right of the editor window.
        int x = this.dedWindow.getLocation().x;
        int y = this.dedWindow.getLocation().y;
        int w = this.dedWindow.getWidth();
        this.undoHistoryWindow.setLocation(x+w, y);
        this.undoHistoryWindow.setVisible(true);
    }

    /** Dispose of any windows we own, since the enclosing window is
      * being disposed. */
    public void disposeOwnedWindows()
    {
        this.undoHistoryWindow.dispose();
    }

    /** Return the undo history object.  It is intended that callers
      * only *read* the data in the history. */
    public UndoHistory getUndoHistory()
    {
        return this.undoHistory;
    }

    @SuppressWarnings("serial")
    private /*non-static*/ class RedoAlternateAction extends AbstractAction {
        // ---- data ----
        /** Integer in [0,n-1] where n is this.undoHistory.numRedos(). */
        public int whichRedo;

        // ---- methods ----
        public RedoAlternateAction(String label, int w)
        {
            super(label);
            this.whichRedo = w;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            DiagramController.this.editRedoAlternate(this.whichRedo);
        }
    }

    /** Rebuild the "Redo Alternate" sub-menu. */
    public void populateRedoAlternateMenu()
    {
        JMenu redoSubmenu = this.dedWindow.redoSubmenu;
        redoSubmenu.removeAll();

        ArrayList<String> futures = this.undoHistory.describeRedos();
        if (futures.size() < 2) {
            // This is the common case.
            redoSubmenu.setEnabled(false);
        }
        else {
            // To get here, the user must undo some actions, then do
            // some more actions, then undo those as well.
            redoSubmenu.setEnabled(true);

            for (int i=0; i < futures.size()-1; i++) {
                String label = ""+(i+1)+": "+futures.get(i);
                redoSubmenu.add(new RedoAlternateAction(label, i));
            }
        }
    }

    @Override
    public int getUndoHistoryLimit()
    {
        return this.undoHistoryLimit;
    }

    @Override
    public void setUndoHistoryLimit(int newLimit)
    {
        this.undoHistoryLimit = newLimit;
    }
}

// EOF
