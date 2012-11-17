// EntityDialog.java

package ded.ui;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Window;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import util.SwingUtil;

import ded.model.Entity;

/** Dialog box to edit an Entity. */
public class EntityDialog extends JDialog {
    private static final long serialVersionUID = 1455207901388264571L;

    // -------------- private data --------------
    /** Entity being edited. */
    private Entity entity;
    
    // Controls.
    private JTextField nameText;
    
    // -------------- methods ---------------
    public EntityDialog(Window documentParent, Entity entity)
    {
        super(documentParent, "Edit Entity", Dialog.ModalityType.DOCUMENT_MODAL);
        
        this.entity = entity;
        
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        Box vb = SwingUtil.makeMarginVBox(this, 11);
      
        this.nameText = SwingUtil.makeLineEdit(vb, "Name:", 'n', "initial name");
        vb.add(Box.createVerticalStrut(5));
        
        // attributes
        {
            Box attrBox = SwingUtil.makeHBox(vb);
            
            JLabel lbl = new JLabel("Attributes:");
            lbl.setDisplayedMnemonic('a');
            attrBox.add(lbl);
            attrBox.add(Box.createHorizontalGlue());
            
            JTextArea ta = new JTextArea("initial attribute1\ninitial attribute2\ninitial attribute3\n");
            lbl.setLabelFor(ta);

            // Tab and shift-tab should move the focus, not insert characters.
            // http://stackoverflow.com/questions/5042429/how-can-i-modify-the-behavior-of-the-tab-key-in-a-jtextarea
            ta.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
            ta.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
            
            JScrollPane scroll = new JScrollPane(ta);
            
            // This is what establishes the initial size of the dialog, as the
            // scroll pane is the main resizable thing.
            scroll.setPreferredSize(new Dimension(300,150));
            
            vb.add(scroll);
            vb.add(Box.createVerticalStrut(5));
        }

        // x, y
        {
            Box locBox = SwingUtil.makeHBox(vb);
            SwingUtil.makeLineEdit(locBox, "X:", 'x', "50");
            locBox.add(Box.createHorizontalStrut(5));
            SwingUtil.makeLineEdit(locBox, "Y:", 'y', "60");
            vb.add(Box.createVerticalStrut(5));
            SwingUtil.disallowVertStretch(locBox);
        }
         
        // w, h
        {
            Box sizeBox = SwingUtil.makeHBox(vb);
            SwingUtil.makeLineEdit(sizeBox, "W:", 'w', "70");
            sizeBox.add(Box.createHorizontalStrut(5));
            SwingUtil.makeLineEdit(sizeBox, "H:", 'h', "80");
            vb.add(Box.createVerticalStrut(5));
            SwingUtil.disallowVertStretch(sizeBox);
        }
        
        // Buttons
        {
            Box btnBox = SwingUtil.makeHBox(vb);
            btnBox.add(Box.createHorizontalGlue());
            btnBox.add(new JButton("Cancel"));
            btnBox.add(Box.createHorizontalStrut(5));
            btnBox.add(new JButton("OK"));
            SwingUtil.disallowVertStretch(btnBox);
        }

        this.pack();
    }

    /** Print component sizes for debugging. */
    public static void printSizes(String label, Component c)
    {
        System.out.println(label+" preferred size: "+c.getPreferredSize());
        System.out.println(label+" max size: "+c.getMaximumSize());
        System.out.println(label+" min size: "+c.getMinimumSize());
        System.out.println(label+" cur size: "+c.getSize());
    }
    
    /** Show the edit dialog for Entity, waiting until the user closes the dialog
      * before returning.  If the user presses OK, 'entity' will be updated and
      * true returned.  Otherwise, 'entity' is not modified, and false is returned. */
    public static boolean exec(Component documentParent, Entity entity)
    {
        Window parentWindow =
            documentParent!=null?
                SwingUtilities.getWindowAncestor(documentParent) : null;
        EntityDialog dialog = new EntityDialog(parentWindow, entity);
        //dialog.pack();
        dialog.setVisible(true);        // Blocks until dialog is closed!
        
        return false;
    }
}

// EOF
