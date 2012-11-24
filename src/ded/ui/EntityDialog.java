// EntityDialog.java

package ded.ui;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EnumSet;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import util.swing.SwingUtil;

import ded.model.Entity;
import ded.model.EntityShape;

/** Dialog box to edit an Entity. */
public class EntityDialog extends JDialog {
    private static final long serialVersionUID = 1455207901388264571L;

    // -------------- private data --------------
    /** Entity being edited. */
    private Entity entity;
    
    // Controls.
    private JTextField nameText;
    private JTextArea attributeText;
    private JComboBox shapeChooser;
    private JTextField xText, yText, wText, hText;
    
    // -------------- public data --------------
    /** Initially false, this is set to true if the dialog is closed
     * by pressing the OK button. */
    public boolean okWasPressed;
   
    // -------------- methods ---------------
    public EntityDialog(Window documentParent, Entity entity)
    {
        super(documentParent, "Edit Entity", Dialog.ModalityType.DOCUMENT_MODAL);
        
        this.entity = entity;
        this.okWasPressed = false;
        
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // NOTE: This dialog is not laid out well.  I have not yet figured out
        // a good way to do dialog layout well with Swing (whereas it is easy
        // with Qt).  So the code here should not be treated as a good example
        // on which to base other dialog implementations.
        
        Box vb = SwingUtil.makeMarginVBox(this, 11);
      
        this.nameText = SwingUtil.makeLineEdit(vb, "Name:", 'n', this.entity.name);
        vb.add(Box.createVerticalStrut(5));
        
        // attributes
        {
            Box attrBox = SwingUtil.makeHBox(vb);
            
            JLabel lbl = new JLabel("Attributes:");
            lbl.setDisplayedMnemonic('a');
            attrBox.add(lbl);
            attrBox.add(Box.createHorizontalGlue());
            
            this.attributeText = new JTextArea(this.entity.attributes);
            lbl.setLabelFor(this.attributeText);

            // Tab and shift-tab should move the focus, not insert characters.
            // http://stackoverflow.com/questions/5042429/how-can-i-modify-the-behavior-of-the-tab-key-in-a-jtextarea
            this.attributeText.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
            this.attributeText.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
            
            JScrollPane scroll = new JScrollPane(this.attributeText);
            
            // This is what establishes the initial size of the dialog, as the
            // scroll pane is the main resizable thing.
            scroll.setPreferredSize(new Dimension(300,150));
            
            vb.add(scroll);
            vb.add(Box.createVerticalStrut(5));
        }

        // Shape
        {
            Box shapeHBox = SwingUtil.makeHBox(vb);
            
            JLabel lbl = new JLabel("Shape:");
            lbl.setDisplayedMnemonic('s');
            shapeHBox.add(lbl);
            shapeHBox.add(Box.createHorizontalStrut(5));
            
            // Put all shapes into a vector.
            EnumSet<EntityShape> esSet = EnumSet.allOf(EntityShape.class);
            Vector<EntityShape> esVector = new Vector<EntityShape>();
            esVector.addAll(esSet);
            
            this.shapeChooser = new JComboBox(esVector);
            this.shapeChooser.setSelectedItem(this.entity.shape);
            lbl.setLabelFor(this.shapeChooser);
            shapeHBox.add(this.shapeChooser);
            
            vb.add(Box.createVerticalStrut(5));
            SwingUtil.disallowVertStretch(shapeHBox);
        }
        
        // x, y
        {
            Box locBox = SwingUtil.makeHBox(vb);
            this.xText = SwingUtil.makeLineEdit(locBox, "X:", 'x', String.valueOf(this.entity.loc.x));
            locBox.add(Box.createHorizontalStrut(5));
            this.yText = SwingUtil.makeLineEdit(locBox, "Y:", 'y', String.valueOf(this.entity.loc.y));
            vb.add(Box.createVerticalStrut(5));
            SwingUtil.disallowVertStretch(locBox);
        }
         
        // w, h
        {
            Box sizeBox = SwingUtil.makeHBox(vb);
            this.wText = SwingUtil.makeLineEdit(sizeBox, "W:", 'w', String.valueOf(this.entity.size.width));
            sizeBox.add(Box.createHorizontalStrut(5));
            this.hText = SwingUtil.makeLineEdit(sizeBox, "H:", 'h', String.valueOf(this.entity.size.height));
            vb.add(Box.createVerticalStrut(5));
            SwingUtil.disallowVertStretch(sizeBox);
        }
        
        // Buttons
        {
            Box btnBox = SwingUtil.makeHBox(vb);
            btnBox.add(Box.createHorizontalGlue());
            
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(new SwingUtil.WindowCloseAction(this));
            btnBox.add(cancelButton);
            
            btnBox.add(Box.createHorizontalStrut(5));
            
            JButton okButton = new JButton("OK");
            okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    EntityDialog.this.okPressed();
                }
            });
            btnBox.add(okButton);
            this.getRootPane().setDefaultButton(okButton);
            
            SwingUtil.disallowVertStretch(btnBox);
        }

        SwingUtil.installEscapeCloseOperation(this);
        
        this.pack();
    }

    /** React to the OK button being pressed. */
    public void okPressed()
    {
        // Parse/validate all the integers first.
        int x, y, w, h;
        try {
            x = Integer.valueOf(this.xText.getText());
            y = Integer.valueOf(this.yText.getText());
            w = Integer.valueOf(this.wText.getText());
            h = Integer.valueOf(this.hText.getText());
        }
        catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
                "At least one of x/y/w/h is not a valid integer.",
                "Input Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Update the entity.
        this.entity.name = this.nameText.getText();
        this.entity.attributes = this.attributeText.getText();
        this.entity.shape = (EntityShape)this.shapeChooser.getSelectedItem();
        this.entity.loc.x = x;
        this.entity.loc.y = y;
        this.entity.size.width = w;
        this.entity.size.height = h;

        // Close dialog, signaling that a change was made.
        this.okWasPressed = true;
        SwingUtil.closeWindow(this);
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
        dialog.setVisible(true);        // Blocks until dialog is closed!
        
        return dialog.okWasPressed;
    }
}

// EOF
