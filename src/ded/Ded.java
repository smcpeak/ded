// Ded.java
// See toplevel license.txt for copyright and license terms.

package ded;

import java.awt.Frame;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.URL;

import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import util.awt.AWTUtil;
import util.awt.BDFParser;
import util.awt.BitmapFont;
import util.awt.ResourceImageCache;
import util.swing.MenuAction;
import util.swing.SwingUtil;

import ded.model.Diagram;
import ded.ui.DiagramController;

import static util.StringUtil.fmt;

/** Diagram editor. */
public class Ded extends JFrame implements WindowListener {
    // ---------- constants -------------
    private static final long serialVersionUID = -7931792812267064160L;

    /** Window title when there is no file name, or prefix of it when there is. */
    public static final String windowTitle = "Diagram Editor";

    // ---------- static data ----------------
    /** Window icons. */
    public static ImageIcon windowIcon16, windowIcon32;

    // ---------- instance data --------------
    /** The font I want to use in the diagram area.  There should be no
      * use of the AWT fonts for drawing. */
    public BitmapFont diagramBitmapFont;

    /** Image cache. */
    public ResourceImageCache resourceImageCache = new ResourceImageCache();

    /** The main diagram editor pane. */
    private DiagramController diagramController;

    /** The menu item associated with Diagram.drawFileName. */
    private JCheckBoxMenuItem drawFileNameCheckbox;

    /** The sub-menu for redo alternate.  This is public so that
      * DiagramController can populate it as needed. */
    public JMenu redoSubmenu;

    // ---------- public methods -------------
    public Ded()
    {
        super(windowTitle);

        SwingUtil.assignJFrameImplicitPaneNames(this);

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

        // Use a bitmap font packaged with 'ded' itself.  (After many
        // attempts, I was unable to find a portable alternative.)
        InputStream in = null;
        try {
            String fname = "resources/helvR12sm.bdf.gz";

            // First try loading it from the JAR file.
            URL url = Ded.class.getResource("/"+fname);
            if (url != null) {
                in = url.openStream();
            }
            else {
                // Then try loading from file system.
                // (Maybe getResource already tries this?)
                in = new FileInputStream(fname);
            }
            in = new GZIPInputStream(in);
            this.diagramBitmapFont = new BitmapFont(new BDFParser(in));
        }
        catch (Exception e) {
            System.err.println("cannot load bitmap font resource: "+e);
            System.exit(2);
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                }
                catch (IOException e) {/*don't care*/}
            }
        }

        this.diagramController = new DiagramController(this);
        this.diagramController.setName("diagramController");
        this.diagramController.setOpaque(true);
        this.setContentPane(this.diagramController);

        this.buildMenuBar();
    }

    /** Build the menu. */
    private void buildMenuBar()
    {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setName("menuBar");
        menuBar.add(buildFileMenu());
        menuBar.add(buildEditMenu());
        menuBar.add(buildModeMenu());
        menuBar.add(buildDiagramMenu());
        menuBar.add(buildHelpMenu());
        this.setJMenuBar(menuBar);
    }

    @SuppressWarnings("serial")
    private JMenu buildFileMenu()
    {
        JMenu m = new JMenu("File");
        m.setName("file");
        m.setMnemonic(KeyEvent.VK_F);

        m.add(new MenuAction("New diagram", KeyEvent.VK_N) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.newFile();
            }
        });

        m.add(new MenuAction("Open or import...", KeyEvent.VK_O, KeyEvent.VK_O, ActionEvent.CTRL_MASK) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.loadFromFile();
            }
        });

        m.add(new MenuAction("Save (and export to PNG)", KeyEvent.VK_S, KeyEvent.VK_S, ActionEvent.CTRL_MASK) {
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
        m.setName("edit");
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

        m.add(new MenuAction("Cut", KeyEvent.VK_T, KeyEvent.VK_X, ActionEvent.CTRL_MASK) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.cutSelected();
            }
        });

        m.add(new MenuAction("Copy", KeyEvent.VK_C, KeyEvent.VK_C, ActionEvent.CTRL_MASK) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.copySelected();
            }
        });

        m.add(new MenuAction("Paste", KeyEvent.VK_P, KeyEvent.VK_V, ActionEvent.CTRL_MASK) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.pasteClipboard();
            }
        });

        m.add(new MenuAction("Delete", KeyEvent.VK_DELETE, KeyEvent.VK_DELETE, 0) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.deleteSelected();
            }
        });

        m.addSeparator();

        m.add(new MenuAction("Undo", KeyEvent.VK_U, KeyEvent.VK_Z, ActionEvent.CTRL_MASK) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.editUndo();
            }
        });

        m.add(new MenuAction("Redo", KeyEvent.VK_R, KeyEvent.VK_Y, ActionEvent.CTRL_MASK) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.editRedo();
            }
        });

        // Create an empty sub-menu for the "Redo Alternate" command.
        // DiagramController will populate it.
        this.redoSubmenu = new JMenu("Redo Alternate");
        this.redoSubmenu.setMnemonic(KeyEvent.VK_A);
        this.redoSubmenu.setEnabled(false);
        m.add(this.redoSubmenu);

        m.add(new MenuAction("Show the undo history window...", KeyEvent.VK_W) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.showUndoHistory();
            }
        });

        return m;
    }

    @SuppressWarnings("serial")
    private JMenu buildModeMenu()
    {
        JMenu m = new JMenu("Mode");
        m.setName("mode");
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
    private JMenu buildDiagramMenu()
    {
        JMenu m = new JMenu("Diagram");
        m.setName("diagram");
        m.setMnemonic(KeyEvent.VK_D);

        this.drawFileNameCheckbox =
            new JCheckBoxMenuItem("Draw file name in upper-left corner", true);
        this.drawFileNameCheckbox.setMnemonic(KeyEvent.VK_N);
        this.drawFileNameCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Ded.this.toggleDrawFileName();
            }
        });
        m.add(this.drawFileNameCheckbox);

        m.add(new MenuAction("Diagram properties...", KeyEvent.VK_P) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.editDiagramProperties();
            }
        });

        m.add(new MenuAction("Edit custom colors...", KeyEvent.VK_C) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.editCustomColors();
            }
        });

        m.add(new MenuAction("Reload entity images", KeyEvent.VK_R, KeyEvent.VK_F5, 0) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.reloadEntityImages();
            }
        });

        m.addSeparator();

        m.add(new MenuAction("Bring selected entities to front", KeyEvent.VK_F, KeyEvent.VK_F, 0) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.moveSelectedEntitiesToFrontOrBack(true /*front*/);
            }
        });

        m.add(new MenuAction("Send selected entities to back", KeyEvent.VK_B, KeyEvent.VK_B, 0) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.moveSelectedEntitiesToFrontOrBack(false /*front*/);
            }
        });

        m.addSeparator();

        m.add(new MenuAction("Check data consistency invariants", KeyEvent.VK_I) {
           public void actionPerformed(ActionEvent e) {
               try {
                   Ded.this.diagramController.diagram.selfCheck();
                   SwingUtil.informationMessageBox(Ded.this,
                       "Self check passed", "Self check passed.");
               }
               catch (Exception exn) {
                   SwingUtil.warningMessageBox(Ded.this,
                       "Self check failed: "+exn.getLocalizedMessage()+
                       ".  Additional info may have been printed to console (stderr).");
               }
           }
        });

        return m;
    }

    @SuppressWarnings("serial")
    private JMenu buildHelpMenu()
    {
        JMenu m = new JMenu("Help");
        m.setName("help");
        m.setMnemonic(KeyEvent.VK_H);

        // I would like the 'H' key displayed as an accelerator here,
        // but it conflicts with the use of 'H' when a relation is
        // selected.  There does not appear to be a way for the menu
        // item to display an accelerator key but ignore it.
        m.add(new MenuAction("Help ...", KeyEvent.VK_H, KeyEvent.VK_F1, 0) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.showHelpBox();
            }
        });

        m.add(new MenuAction("Show log window...", KeyEvent.VK_L, KeyEvent.VK_L, ActionEvent.CTRL_MASK) {
            public void actionPerformed(ActionEvent e) {
                Ded.this.diagramController.showLogWindow();
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

    private void toggleDrawFileName()
    {
        this.diagramController.diagram.drawFileName =
            !this.diagramController.diagram.drawFileName;
        this.diagramController.diagramChanged(
            fmt("Set \"Draw file name\" to %1$b",
                this.diagramController.diagram.drawFileName));
        this.updateMenuState();
    }

    /** Update the state of stateful menu items (checkboxes)
      * to match the current diagram. */
    public void updateMenuState()
    {
        this.drawFileNameCheckbox.setState(this.diagramController.diagram.drawFileName);
        this.diagramController.populateRedoAlternateMenu();
    }

    private void showAboutBox()
    {
        // Get the version number.
        String version;
        {
            InputStream is = this.getClass().getResourceAsStream("/resources/version.txt");
            if (is != null) {
                try {
                    version = new BufferedReader(new InputStreamReader(is, "UTF-8")).readLine();
                }
                catch (IOException e) {
                    version = "(While retrieving version: "+e.getMessage()+")";
                }
                finally {
                    try {
                        is.close();
                    }
                    catch (IOException e) {/*ignore*/}
                }
            }
            else {
                version = "(No version information found.)";
            }
        }

        JOptionPane.showMessageDialog(
            this,
            "Diagram Editor (DED)\n"+
                "Copyright (c) 2012-2015 Scott McPeak\n"+
                "Program version: "+version+"\n"+
                "Maximum file version: "+Diagram.currentFileVersion+"\n"+
                "This software is made available under the terms of the BSD license:\n"+
                "http://opensource.org/licenses/BSD-2-Clause\n"+
                "\n"+
                "It uses the following third-party libraries:\n"+
                "json.org JSON parser, JRE runtime.",
            "About Diagram Editor",
            JOptionPane.INFORMATION_MESSAGE,
            windowIcon32);
    }

    @Override
    public void dispose()
    {
        super.dispose();
        this.diagramController.disposeOwnedWindows();

        // Unfortunately, there is a 1-2 second delay between when I
        // hit 'q' or the X button and when the process exits unless
        // I manually shut down the JVM.  I hate that delay, so I do
        // this even though it is considered bad form in Java.
        //
        // If/when I add support for editing multiple documents in
        // one process, I'll have to make the logic here smarter.
        //
        // This causes Abbot to stack overflow, so I disabled it.
        //
        // But I'm not using Abbot anymore, and on OpenJDK, without
        // this exit call, the process does not exit, so I've put
        // this back in.
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

    @Override
    public void windowOpened(WindowEvent e)
    {
        this.diagramController.logDisplayScaling();
    }

    // WindowListener events I do not care about.
    @Override public void windowClosed(WindowEvent e) {}
    @Override public void windowIconified(WindowEvent e) {}
    @Override public void windowDeiconified(WindowEvent e) {}
    @Override public void windowActivated(WindowEvent e) {}
    @Override public void windowDeactivated(WindowEvent e) {}

    /** Check that the current diagram is equivalent to the diagram
      * in the named file.  This is meant to be called from Abbot for
      * UI testing.  If the diagrams do not match, throws. */
    public static void checkDiagram(String correctFname)
        throws Exception
    {
        // Search for a Ded window.  (I cannot figure out how to pass
        // a reference to a Component from Abbot, even though it seems
        // to have that capability.)
        Frame[] frames = Frame.getFrames();
        for (Frame f : frames) {
            if (f instanceof Ded) {
                Ded ded = (Ded)f;
                ded.innerCheckDiagram(correctFname);
                return;
            }
        }

        throw new RuntimeException("could not find Ded window");
    }

    /** Non-static helper for 'checkDiagram'. */
    private void innerCheckDiagram(String correctFname)
        throws Exception
    {
        // First, load the "correct" diagram.
        Diagram correctDiagram = Diagram.readFromFile(correctFname);

        // Check that it equals() the one we're editing.
        if (!correctDiagram.equals(this.diagramController.diagram)) {
            throw new RuntimeException(
                "current and correct diagrams are not equals()");
        }

        // Paranoia about equals() being incomplete: serialize both to
        // the current JSON format ('correctFname' might be an older
        // version) and compare that.
        String correctJSON = correctDiagram.toString();
        String currentJSON = this.diagramController.diagram.toString();
        if (!correctJSON.equals(currentJSON)) {
            throw new RuntimeException(
                "equals() was true but JSON strings are not equal!");
        }
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
                System.err.println("Uncaught exception at Diagram Editor top-level:");
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
                Ded ded = new Ded();
                ded.setName("ded");

                // Open specified file if any.
                if (args.length >= 1) {
                    ded.diagramController.loadFromNamedFile(args[0]);
                }

                ded.setVisible(true);

                if (false) {
                    AWTUtil.dumpFrameTrees();
                }
            }
        });
    }
}

// EOF
