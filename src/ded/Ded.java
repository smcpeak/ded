// Ded.java

package ded;

import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import ded.model.Entity;
import ded.ui.DiagramController;
import ded.ui.EntityDialog;

/** Diagram editor. */
public class Ded extends JFrame {
    // ---------- constants -------------
    private static final long serialVersionUID = -7931792812267064160L;
    
    /** Window title when there is no file name, or prefix of it when there is. */
    public static final String windowTitle = "Diagram Editor";

    // ---------- static data ----------------
    /** Window icon. */
    public static Image iconImage;
    
    // ---------- private data --------------
    private DiagramController diagramController;
    
    // ---------- public methods -------------
    public Ded()
    {
        super(windowTitle);
        
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setSize(800,600);
        
        // Load the Window icon if we haven't already.
        synchronized (Ded.class) {
            if (iconImage == null) {
                // For now, this requires that I'm running it out of
                // the build tree.  My plan is to add the ability to
                // package everything in a JAR.
                try {
                    iconImage = new ImageIcon("src/ded/ui/boxarrow.png").getImage();
                }
                catch (Exception e) {
                    // This is never called; it seems that ImageIcon just
                    // silently does nothing when it can't load the file.
                    System.out.println("could not load icon: "+e.getMessage());
                }
            }
        }
        
        if (iconImage != null) {
            this.setIconImage(iconImage);
        }
        
        this.diagramController = new DiagramController(this);
        this.diagramController.setOpaque(true);
        this.setContentPane(this.diagramController);
    }
    
    @Override
    public void dispose()
    {
        super.dispose();

        // Unfortunately, there is a 1-2 second delay between when I
        // hit 'q' or the X button and when the process exits unless
        // I manually shut down the JVM.  I hate that delay, so I do
        // this even though it is considered bad form in Java.
        //
        // If/when I add support for editing multiple documents in
        // one process, I'll have to make the logic here smarter.
        System.exit(0);
    }
    
    public static void main(final String[] args)
    {
        // Use the Nimbus L+F.
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        }
        catch (Exception e) {
            System.err.println("Could not use Nimbus look and feel: "+e);
            System.err.println("Falling back to default L+F.");
        }
        
        // Show exceptions in the UI.
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, final Throwable e) {
                // For the moment, I'm going to say that any exception
                // that gets here is a bug, so dump a trace to the console.
                e.printStackTrace();
                
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        String msg = e.getClass().getSimpleName();
                        
                        String m = e.getMessage();
                        if (m != null) {
                            msg += ": " + m;
                        }
                        
                        JOptionPane.showMessageDialog(null, 
                            msg, "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
        });
        
        // Kick off the Swing app.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // for testing during development: just run one dialog
                if (false) {
                    Entity e = new Entity();
                    e.name = "name";
                    e.attributes = "attr1\nattr2\nattr3\n";
                    e.loc.x = 10;
                    e.loc.y = 20;
                    e.size.width = 30;
                    e.size.height = 40;
                    System.out.println("e: "+e);
                    
                    boolean okPressed = EntityDialog.exec(null, e);
                    
                    System.out.println("okPressed: "+okPressed);
                    System.out.println("e: "+e);
                }

                else {
                    Ded ded = new Ded();
                    
                    // Open specified file if any.
                    if (args.length >= 1) {
                        ded.diagramController.loadFromNamedFile(args[0]);
                    }

                    ded.setVisible(true);
                }
            }
        });
    }
}

// EOF
