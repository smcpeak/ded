// DiagramController.java

package ded;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

/** Widget to display and edit a diagram. */
public class DiagramController extends JPanel {
    private static final long serialVersionUID = 1266678840598864303L;

    public DiagramController()
    {
        this.setBackground(Color.WHITE);
    }
    
    @Override
    public void paint(Graphics g)
    {
        super.paint(g);
        
        g.drawString("hello", 20, 20);
        
        g.setColor(Color.RED);
        
        int w = this.getSize().width;
        int h = this.getSize().height;
        g.drawRect(0,0, w-1,h-1);
        
        g.drawLine(20,20, 20,20);
    }
}

// EOF
