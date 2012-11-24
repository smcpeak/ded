// DiagramController.java

package ded.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import util.awt.GeomUtil;
import util.swing.SwingUtil;

import ded.Ded;
import ded.model.Diagram;
import ded.model.Entity;
import ded.model.Inheritance;
import ded.model.Relation;
import ded.model.RelationEndpoint;

/** Widget to display and edit a diagram. */
public class DiagramController extends JPanel
    implements MouseListener, MouseMotionListener, KeyListener, ComponentListener
{
    // ------------- constants ---------------
    private static final long serialVersionUID = 1266678840598864303L;
    
    private static final String helpMessage =
        "H - This message\n"+
        "Q - Quit\n"+
        "S - Select mode\n"+
        "C - Create entity mode\n"+
        "A - Create relation (\"arrow\") mode\n"+
        "I - Create inheritance mode\n"+
        "Enter or Double click - Edit selected thing\n"+
        "Insert - Insert relation control point\n"+
        "Delete - Delete selected thing\n"+
        "Ctrl+S - Save to file\n"+
        "Ctrl+O - Load from file\n"+
        "Left click - select\n"+
        "Ctrl+Left click - multiselect\n"+
        "Left click+drag - multiselect rectangle\n"+
        "Right click - properties\n"+
        "\n"+
        "When relation selected, H/V/D to change routing,\n"+
        "and O to toggle owned/shared.\n"+
        "When inheritance selected, O to change open/closed.\n"+
        "When dragging, hold Shift to turn off 5-pixel snap.\n";

    // ------------- static data ---------------
    /** Granularity of drag/move snap action. */
    public static final int SNAP_DIST = 5;
    
    // ------------- private types ---------------
    /** Primary "mode" of the editing interface, indicating what happens
      * when the left mouse button is clicked or released. */
    private static enum Mode {
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
    private Diagram diagram;
    
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

    /** If CFM_DRAGGING, this is the controller being moved. */
    private Controller dragging;

    /** If CFM_DRAGGING, this is the vector from the original mouse click point
      * to the Controller's original getLoc(). */ 
    private Point dragOffset;
    
    /** Most recently used file name. */
    private String fileName;
    
    // ------------- public methods ---------------
    public DiagramController(Ded dedWindow)
    {
        this.setBackground(Color.WHITE);
        
        this.dedWindow = dedWindow;
        this.diagram = new Diagram();
        this.controllers = new ArrayList<Controller>();
        this.mode = Mode.DCM_SELECT;
        this.fileName = "";
        
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addKeyListener(this);
        this.addComponentListener(this);
        
        this.setFocusable(true);
    }
    
    public Diagram getDiagram()
    {
        return this.diagram;
    }
    
    @Override
    public void paint(Graphics g)
    {
        super.paint(g);

        // Controllers.
        for (Controller c : this.controllers) {
            c.paint(g);
        }

        // Lasso rectangle.
        if (this.mode == Mode.DCM_RECT_LASSO) {
            Rectangle r = this.getLassoRect();
            g.drawRect(r.x, r.y, r.width, r.height);
        }
        
        if (this.mode != Mode.DCM_SELECT) {
            g.drawString("Mode: " + this.mode.description, 3, this.getHeight()-4);
        }
        
    }

    /** Deselect all controllers and return the number that were previously selected. */
    public int deselectAll()
    {
        // Change state after iterating.
        HashSet<Controller> toDeselect = new HashSet<Controller>();
        
        for (Controller c : this.controllers) {
            if (c.isSelected()) {
                toDeselect.add(c);
            }
        }
        
        for (Controller c : toDeselect) {
            c.setSelected(SelectionState.SS_UNSELECTED);
        }
        
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

    @Override
    public void mousePressed(MouseEvent e)
    {
        switch (this.mode) {
            case DCM_SELECT: {
                // Clicked a controller?
                Controller c = this.hitTest(e.getPoint(), null);
                if (c == null) {
                    if (SwingUtil.controlPressed(e)) {
                        // Control is pressed.  We missed all controls, so ignore.
                    }
                    else {
                        if (this.deselectAll() > 0) {
                            this.repaint();
                        }
                        
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            // Enter lasso mode.
                            setMode(Mode.DCM_RECT_LASSO);
                            this.lassoStart = this.lassoEnd = e.getPoint();
                        }
                    }
                }
                else {
                    c.mousePressed(e);
                }
                break;
            }
            
            case DCM_CREATE_RELATION: {
                // Make a Relation that starts and ends at the current location.
                RelationEndpoint endpt = this.getRelationEndpoint(e.getPoint());
                Relation r = new Relation(endpt, new RelationEndpoint(endpt));
                this.diagram.relations.add(r);
                
                // Build a controller and select it.
                RelationController rc = this.buildRelationController(r);
                this.selectOnly(rc);
                
                // Drag the end point while the mouse button is held.
                this.beginDragging(rc.getEndHandle(), e.getPoint());
                
                this.repaint();
                break;
            }
            
            case DCM_CREATE_ENTITY: {
                EntityController.createEntityAt(this, e.getPoint());
                this.setMode(Mode.DCM_SELECT);
                break;
            }
            
            case DCM_CREATE_INHERITANCE: {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    this.createInheritanceAt(e.getPoint());
                }
                break;
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
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
                for (Controller c : this.controllers) {
                    if (!c.isSelected()) { continue; }
                    
                    Point cur = c.getLoc();
                    c.dragTo(GeomUtil.add(cur, delta));
                }
            }
            else {
                // Dragging item is not selected; must be a resize handle.
                this.dragging.dragTo(destLoc);
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
        // Click+drag should only be initiated with left mouse button, so ignore
        // release of others.
        if (!SwingUtilities.isLeftMouseButton(e)) {
            return;
        }
        
        if (this.mode == Mode.DCM_DRAGGING || this.mode == Mode.DCM_RECT_LASSO) {
            this.setMode(Mode.DCM_SELECT);
        }
    }
    
    @Override
    public void mouseClicked(MouseEvent e) 
    {
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
    @Override public void mouseMoved(MouseEvent e) {}
    
    @Override
    public void keyPressed(KeyEvent e)
    {
        if (SwingUtil.controlPressed(e)) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_S:
                    this.saveToFile();
                    break;
                        
                case KeyEvent.VK_O:
                    this.loadFromFile();
                    break;
            }
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
            case KeyEvent.VK_H:
                JOptionPane.showMessageDialog(this, helpMessage, 
                    "Diagram Editor Keybindings",
                    JOptionPane.INFORMATION_MESSAGE);
                break;
                
            case KeyEvent.VK_Q:
                this.dedWindow.dispose();
                break;
                
            case KeyEvent.VK_X:
                if (SwingUtil.shiftPressed(e)) {
                    assert(false);     // Make sure assertions are enabled.
                }
                else {
                    throw new RuntimeException("Test exception/error message.");
                }
                break;
                
            case KeyEvent.VK_C:
                this.setMode(Mode.DCM_CREATE_ENTITY);
                break;
                
            case KeyEvent.VK_S:
                this.setMode(Mode.DCM_SELECT);
                break;
                
            case KeyEvent.VK_A:
                this.setMode(Mode.DCM_CREATE_RELATION);
                break;
                
            case KeyEvent.VK_I:
                this.setMode(Mode.DCM_CREATE_INHERITANCE);
                break;
                
            case KeyEvent.VK_ENTER:
                this.editSelected();
                break;
                
            case KeyEvent.VK_INSERT:
                this.insertControlPoint();
                break;
                
            case KeyEvent.VK_DELETE:
                this.deleteSelected();
                break;
        }
    }

    /** Prompt for a file name to load, then replace the current diagram with it. */
    private void loadFromFile()
    {
        String result =
            JOptionPane.showInputDialog(this, "File name to load from:", this.fileName);
        if (result != null) {
            loadFromNamedFile(result);
        }
    }
    
    /** Load from the given file, replacing the current diagram. */
    public void loadFromNamedFile(String name)
    {
        try {
            // Read the file.
            Diagram d = Diagram.readFromFile(name);
            
            // Success.  First, update file name.
            this.setFileName(name);
            
            // Sizing is achieved by specifying a preferred size for
            // the content pane, then packing other controls and the
            // window border stuff around it.
            this.setPreferredSize(d.windowSize);
            this.dedWindow.pack();

            // Swap in the new diagram and rebuild the UI for it.
            this.diagram = d;
            this.rebuildControllers();

            this.repaint();
        }
        catch (Exception e) {
            this.errorMessageBox(
                "Error while reading \""+name+"\": "+
                e.getClass().getSimpleName()+": "+e.getMessage());
        }
    }
    
    /** Rebuild all the controllers from 'diagram'. */
    private void rebuildControllers()
    {
        this.controllers.clear();
        
        for (Entity e : this.diagram.entities) {
            this.add(new EntityController(this, e));
        }
        
        for (Relation r : this.diagram.relations) {
            buildRelationController(r);
        }
        
        for (Inheritance inh : this.diagram.inheritances) {
            buildInheritanceController(inh);
        }
        
        this.setMode(Mode.DCM_SELECT);
    }

    /** Prompt user for file name and save to it. */
    private void saveToFile()
    {
        String result = 
            JOptionPane.showInputDialog(this, "File name to save to:", this.fileName);
        if (result != null) {
            try {
                this.diagram.saveToFile(result);
                
                // If it worked, remember the new name.
                this.setFileName(result);
            }
            catch (Exception e) {
                // Java error messages are really bad.  Maybe I will fix this
                // at some point.
                this.errorMessageBox(
                    "Error while writing to \""+result+"\": "+
                    e.getClass().getSimpleName()+": "+e.getMessage());
            }
        }
    }

    /** Change the recent file name to 'name', updating window title too. */
    private void setFileName(String name)
    {
        this.fileName = name;
        
        // Update window title.
        this.dedWindow.setTitle(Ded.windowTitle+": "+name);
    }

    // KeyListener methods I do not care about.
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}

    /** Change the UI mode to 'm', maintaining a few invariants in the process. */
    public void setMode(Mode m)
    {
        this.mode = m;
        
        if (m != Mode.DCM_DRAGGING) {
            this.dragging = null;
            this.dragOffset = new Point(0,0);
        }
        
        if (m != Mode.DCM_RECT_LASSO) {
            this.lassoStart = this.lassoEnd = new Point(0,0);
        }
        
        this.selfCheck();
        this.repaint();
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
    public Set<Controller> getAllSelected()
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
        }
    }
    
    /** Delete the selected controllers and associated entities, if any. */
    private void deleteSelected()
    {
        if (this.mode == Mode.DCM_SELECT) {
            Set<Controller> sel = this.getAllSelected();
            int n = sel.size();
            if (n > 1) {
                int choice = JOptionPane.showConfirmDialog(this, 
                    "Delete "+n+" elements?", "Confirm Deletion", 
                    JOptionPane.OK_CANCEL_OPTION);
                if (choice != JOptionPane.OK_OPTION) {
                    return;
                }
            }

            this.deleteControllers(sel);
        }
    }
    
    /** Insert a new control point into the selected controller and
      * associated entity, if any and applicable. */
    private void insertControlPoint()
    {
        if (this.mode == Mode.DCM_SELECT) {
            Controller c = this.getUniqueSelected();
            if (c != null) {
                c.insertControlPoint();
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
        }
        
        for (Controller c : this.controllers) {
            c.globalSelfCheck(this.diagram);
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
        for (Controller c : toDeselect) {
            c.setSelected(SelectionState.SS_UNSELECTED);
        }
        
        if (toSelect.size() == 1) {
            // Exclusively select the one lasso'd controller.  (Using
            // a 'for' loop is merely syntactically convenient.)
            for (Controller c : toSelect) {
                // This will show resize controls.
                c.setSelected(SelectionState.SS_EXCLUSIVE);
            }
        }
        else {
            // Set state of all selected controls.
            for (Controller c : toSelect) {
                c.setSelected(SelectionState.SS_SELECTED);
            }
        }
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
    private Set<Controller> findControllers(ControllerFilter filter)
    {
        HashSet<Controller> ret = new HashSet<Controller>();
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
        Set<Controller> ctls = this.findControllers(filter);
        
        this.deleteControllers(ctls);
    }
    
    /** Delete specified controllers. */
    public void deleteControllers(Set<Controller> ctls)
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
    
    /** Create an inheritance based on the user's click on 'point'. */
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
        
        this.repaint();
    }

    /** Show an error message dialog box with 'message'. */
    public void errorMessageBox(String message)
    {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void componentResized(ComponentEvent e)
    {
        this.diagram.windowSize = this.getSize();
    }

    // ComponentListener events I do not care about.
    @Override public void componentMoved(ComponentEvent e) {}
    @Override public void componentShown(ComponentEvent e) {}
    @Override public void componentHidden(ComponentEvent e) {}

    /** Pop up a menu at 'where', with menu title 'title', and menu
      * choices 'items'.  If the user chooses one, return its index
      * in 'items'.  Otherwise, return -1.  This blocks until the
      * user chooses something. */
    public int popupMenu(Point where, String title, String[] items)
    {
        // TODO: Code this.
        JOptionPane.showMessageDialog(this, "TODO: popupMenu");
        return -1;      // Nothing chosen.
    }
}

// EOF
