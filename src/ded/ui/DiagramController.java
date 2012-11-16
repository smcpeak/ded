// DiagramController.java

package ded.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

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
        DCM_SELECT,                    // click to select/move/resize
        DCM_CREATE_ENTITY,             // click to create an entity
        DCM_CREATE_RELATION,           // click to create a relation
        DCM_CREATE_INHERITANCE,        // click to create an inheritance relation
        DCM_DRAGGING,                  // currently drag-moving something
        DCM_RECT_LASSO,                // currently drag-lasso selecting
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
    
    // ------------- public methods ---------------
    public DiagramController()
    {
        this.setBackground(Color.WHITE);
        
        this.diagram = new Diagram();
        this.controllers = new ArrayList<Controller>();
        this.mode = Mode.DCM_CREATE_ENTITY;
        
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

        g.drawString("Mode: " + mode, 3, this.getHeight()-4);
        
        for (Controller c : controllers) {
            c.paint(g);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        switch (mode) {
            case DCM_CREATE_ENTITY: {
                EntityController.createEntityAt(this, e.getPoint());
                break;
            }
            
            default:
                // TODO: handle other modes
        }
    }

    // MouseListener methods I do not care about.
    @Override public void mousePressed(MouseEvent e) {}
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
        }
    }

    // KeyListener methods I do not care about.
    @Override public void keyTyped(KeyEvent e) {}
    @Override public void keyReleased(KeyEvent e) {}
}

// EOF
