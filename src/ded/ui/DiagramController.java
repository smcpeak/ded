// DiagramController.java

package ded.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import util.SwingUtil;

import ded.model.Diagram;

/** Widget to display and edit a diagram. */
public class DiagramController extends JPanel
    implements MouseListener, KeyListener
{
    // ------------- private static data ---------------
    private static final long serialVersionUID = 1266678840598864303L;
    
    private static final String helpMessage =
        "H - This message\n"+
        "Q - Quit\n"+
        "S - Select mode\n"+
        "C - Create entity mode\n"+
        "A - Create relation (\"arrow\") mode\n"+
        "I - Create inheritance mode\n"+
        "Enter - Edit selected thing\n"+
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
        "When inheritance selected, O to change open/closed.\n";
    
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

    // ------------- private data ---------------
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
    
    // ------------- public methods ---------------
    public DiagramController()
    {
        this.setBackground(Color.WHITE);
        
        this.diagram = new Diagram();
        this.controllers = new ArrayList<Controller>();
        this.mode = Mode.DCM_SELECT;
        
        this.addMouseListener(this);
        this.addKeyListener(this);
        
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

        if (this.mode != Mode.DCM_SELECT) {
            g.drawString("Mode: " + this.mode.description, 3, this.getHeight()-4);
        }
        
        for (Controller c : this.controllers) {
            c.paint(g);
        }
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        switch (this.mode) {
            case DCM_SELECT: {
                // Clicked a controller?
                Controller c = this.hitTest(e.getPoint(), null);
                if (c == null) {
                    if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
                        // Control is pressed.  We missed all controls, so ignore.
                    }
                    else {
                        if (this.deselectAll() > 0) {
                            this.repaint();
                        }
                        
                        if (false && e.getButton() == MouseEvent.BUTTON1) {
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
            
            case DCM_CREATE_ENTITY: {
                EntityController.createEntityAt(this, e.getPoint());
                this.setMode(Mode.DCM_SELECT);
                break;
            }
        }
    }

    /** Deselect all controllers and return the number that were previously selected. */
    public int deselectAll()
    {
        int ct = 0;
        for (Controller c : this.controllers) {
            if (c.isSelected()) {
                c.setSelected(SelectionState.SS_UNSELECTED);
                ct++;
            }
        }
        return ct;
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

    // MouseListener methods I do not care about.
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    public void addController(Controller c)
    {
        this.controllers.add(c);
        this.repaint();
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_H:
                JOptionPane.showMessageDialog(this, helpMessage, 
                    "Diagram Editor Keybindings",
                    JOptionPane.INFORMATION_MESSAGE);
                break;
                
            case KeyEvent.VK_Q:
                SwingUtilities.getWindowAncestor(this).dispose();
                break;
                
            case KeyEvent.VK_X:
                throw new RuntimeException("Test exception/error message.");
                
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
        }
    }

    private void setMode(Mode m)
    {
        this.mode = m;
        this.repaint();
    }

    // KeyListener methods I do not care about.
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}

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
        // First selected controller found.
        Controller sel = null;

        for (Controller c : this.controllers) {
            if (c.isSelected()) {
                if (sel != null) {
                    // More than one is selected.
                    if (sel.getSelState() != SelectionState.SS_SELECTED) {
                        sel.setSelected(SelectionState.SS_SELECTED);
                    }
                    c.setSelected(SelectionState.SS_SELECTED);
                }
                else {
                    // Exactly one selected (so far).
                    sel = c;
                    sel.setSelected(SelectionState.SS_EXCLUSIVE);
                }
            }
        }
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
        this.dragOffset = SwingUtil.subtract(pt, c.getLoc());
        this.setMode(Mode.DCM_DRAGGING);
    }
}

// EOF
