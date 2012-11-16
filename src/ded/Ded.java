// Ded.java

package ded;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import ded.ui.DiagramController;

/** Diagram editor. */
public class Ded extends JFrame {
    // ---------- private static data -------------
    private static final long serialVersionUID = -7931792812267064160L;

    // ---------- private data --------------
    private DiagramController diagramController;
    
    // ---------- public methods -------------
    public Ded()
    {
        super("Diagram editor");
        
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setSize(800,600);
        
        this.diagramController = new DiagramController();
        this.diagramController.setOpaque(true);
        this.setContentPane(this.diagramController);
    }
    
    public static void main(String[] args)
    {
        // Show exceptions in the UI.
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, final Throwable e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        JOptionPane.showMessageDialog(null, e.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
        });
        
        // Kick off the Swing app.
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            Ded ded = new Ded();
            ded.setVisible(true);
          }
        });
    }
}

// EOF
