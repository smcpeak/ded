// DiagramController.java

package ded.ui;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

import ded.model.Diagram;

/** Widget to display and edit a diagram. */
public class DiagramController extends JPanel {
    // ------------- private static data ---------------
    private static final long serialVersionUID = 1266678840598864303L;

    // ------------- private data ---------------
    /** The diagram we are editing. */
    private Diagram diagram;
    
    // ------------- public methods ---------------
    public DiagramController()
    {
        this.setBackground(Color.WHITE);
        
        this.diagram = new Diagram();
    }
    
    public Diagram getDiagram()
    {
        return this.diagram;
    }
    
    @Override
    public void paint(Graphics g)
    {
        super.paint(g);

        
    }
}

// EOF
