// Ded.java

package ded;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import util.swing.MenuAction;

import ded.model.Entity;
import ded.ui.DiagramController;
import ded.ui.EntityDialog;

/** Diagram editor. */
public class Ded extends JFrame implements WindowListener {
    // ---------- constants -------------
    private static final long serialVersionUID = -7931792812267064160L;
    
    /** Window title when there is no file name, or prefix of it when there is. */
    public static final String windowTitle = "Diagram Editor";

    // ---------- static data ----------------
    /** Window icons. */
    public static ImageIcon windowIcon16, windowIcon32;
    
    // ---------- private data --------------
    private DiagramController diagramController;
    
    // ---------- public methods -------------
    public Ded()
    {
        super(windowTitle);
        
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(this);
        
        this.setSize(800,600);
        
        // Load the Window icon if we haven't already.
        synchronized (Ded.class) {
            if (windowIcon16 == null) {
                // For now, this requires that I'm running it out of
                // the build tree.  My plan is to add the ability to
                // package everything in a JAR.
                try {
                    URL url = Ded.class.getResource("/ded/ui/boxarrow16.png");
                    if (url != null) {
                        windowIcon16 = new ImageIcon(url);
                    }
                    else {
                        // Try loading from the file system, which is
                        // needed when running from Eclipse.
                        windowIcon16 = new ImageIcon("src/ded/ui/boxarrow16.png");
                    }
                    
                    url = Ded.class.getResource("/ded/ui/boxarrow32.png");
                    if (url != null) {
                        windowIcon32 = new ImageIcon(url);
                    }
                    else {
                        windowIcon32 = new ImageIcon("src/ded/ui/boxarrow32.png");
                    }
                }
                catch (Exception e) {
                    // This is never called; it seems that ImageIcon just
                    // silently does nothing when it can't load the file.
                    System.out.println("could not load icon: "+e.getMessage());
                }
            }
        }
        
        if (windowIcon16 != null) {
            ArrayList<Image> icons = new ArrayList<Image>();
            icons.add(windowIcon16.getImage());
            icons.add(windowIcon32.getImage());
            this.setIconImages(icons);
        }
        
        this.diagramController = new DiagramController(this);
        this.diagramController.setOpaque(true);
        this.setContentPane(this.diagramController);

        this.buildMenuBar();
    }

    /** Build the menu. */
    private void buildMenuBar()
    {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(buildFileMenu());
        menuBar.add(buildEditMenu());
        menuBar.add(buildModeMenu());
        menuBar.add(buildHelpMenu());
        this.setJMenuBar(menuBar);
    }
    
    @SuppressWarnings("serial")
    private JMenu buildFileMenu()
    {
        JMenu m = new JMenu("File");
        m.setMnemonic(KeyEvent.VK_F);
        
        m.add(new MenuAction("New", KeyEvent.VK_N) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.newFile();
            }
        });
        
        m.add(new MenuAction("Open ...", KeyEvent.VK_O, KeyEvent.VK_O, ActionEvent.CTRL_MASK) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.loadFromFile();
            }
        });
        
        m.add(new MenuAction("Save", KeyEvent.VK_S, KeyEvent.VK_S, ActionEvent.CTRL_MASK) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.saveCurrentFile();
            }
        });
        
        m.add(new MenuAction("Save as ...", KeyEvent.VK_A) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.chooseAndSaveToFile();
            }
        });
        
        m.addSeparator();
        
        m.add(new MenuAction("Quit", KeyEvent.VK_Q, KeyEvent.VK_Q, 0) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.tryCloseWindow();
            }
        });

        return m;
    }
    
    @SuppressWarnings("serial")
    private JMenu buildEditMenu()
    {
        JMenu m = new JMenu("Edit");
        m.setMnemonic(KeyEvent.VK_E);
        
        m.add(new MenuAction("Edit selected ...", KeyEvent.VK_ENTER, KeyEvent.VK_ENTER, 0) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.editSelected();
            }
        });
        
        m.add(new MenuAction("Insert control point", KeyEvent.VK_INSERT, KeyEvent.VK_INSERT, 0) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.insertControlPoint();
            }
        });
        
        m.add(new MenuAction("Delete selected", KeyEvent.VK_DELETE, KeyEvent.VK_DELETE, 0) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.deleteSelected();
            }
        });
        
        return m;
    }
    
    @SuppressWarnings("serial")
    private JMenu buildModeMenu()
    {
        JMenu m = new JMenu("Mode");
        m.setMnemonic(KeyEvent.VK_M);
        
        m.add(new MenuAction("Select (normal)", KeyEvent.VK_S, KeyEvent.VK_S, 0) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.setMode(DiagramController.Mode.DCM_SELECT);
            }
        });
        
        m.add(new MenuAction("Create entity", KeyEvent.VK_C, KeyEvent.VK_C, 0) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.setMode(DiagramController.Mode.DCM_CREATE_ENTITY);
            }
        });
        
        m.add(new MenuAction("Create relation (arrow)", KeyEvent.VK_A, KeyEvent.VK_A, 0) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.setMode(DiagramController.Mode.DCM_CREATE_RELATION);
            }
        });
        
        m.add(new MenuAction("Create inheritance", KeyEvent.VK_I, KeyEvent.VK_I, 0) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.setMode(DiagramController.Mode.DCM_CREATE_INHERITANCE);
            }
        });
        
        return m;
    }
    
    @SuppressWarnings("serial")
    private JMenu buildHelpMenu()
    {
        JMenu m = new JMenu("Help");
        m.setMnemonic(KeyEvent.VK_H);

        // I would like the 'H' key displayed as an accelerator here,
        // but it conflicts with the use of 'H' when a relation is
        // selected.  There does not appear to be a way for the menu
        // item to display an accelerator key but ignore it.
        m.add(new MenuAction("Help ...", KeyEvent.VK_H) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.showHelpBox();
            }
        });

        m.addSeparator();
        
        m.add(new MenuAction("About ...", KeyEvent.VK_A) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.showAboutBox();
            }
        });
        
        return m;
    }
    
    private void showAboutBox()
    {
        JOptionPane.showMessageDialog(
            this,
            "Diagram Editor (DED)\n"+
                "Copyright 2012 Scott McPeak",
            "About Diagram Editor",
            JOptionPane.INFORMATION_MESSAGE,
            windowIcon32);
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
    
    /** Close the window; but prompt if dirty. */
    public void tryCloseWindow()
    {
        if (this.diagramController.isDirty()) {
            int res = JOptionPane.showConfirmDialog(this, 
                "There are unsaved changes.  Quit anyway?",
                "Quit Confirmation", JOptionPane.YES_NO_OPTION);
            if (res != JOptionPane.YES_OPTION) {
                return;
            }
        }
        this.dispose();
    }

    @Override
    public void windowClosing(WindowEvent e)
    {
        this.tryCloseWindow();
    }

    // WindowListener events I do not care about.
    @Override public void windowOpened(WindowEvent e) {}
    @Override public void windowClosed(WindowEvent e) {}
    @Override public void windowIconified(WindowEvent e) {}
    @Override public void windowDeiconified(WindowEvent e) {}
    @Override public void windowActivated(WindowEvent e) {}
    @Override public void windowDeactivated(WindowEvent e) {}
    
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
