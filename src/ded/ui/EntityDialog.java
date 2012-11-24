// EntityDialog.java

package ded.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import util.swing.ModalDialog;

import ded.model.Entity;
import ded.model.EntityShape;

/** Dialog box to edit an Entity. */
public class EntityDialog extends ModalDialog {
    private static final long serialVersionUID = 1455207901388264571L;

    // -------------- private data --------------
    /** Entity being edited. */
    private Entity entity;
    
    // Controls.
    private JTextField nameText;
    private JTextArea attributeText;
    private JComboBox shapeChooser;
    private JTextField xText, yText, wText, hText;
    
    // -------------- methods ---------------
    public EntityDialog(Component documentParent, Entity entity)
    {
        super(documentParent, "Edit Entity");
        
        this.entity = entity;
        
        // NOTE: This dialog is not laid out well.  I have not yet figured out
        // a good way to do dialog layout well with Swing (whereas it is easy
        // with Qt).  So the code here should not be treated as a good example
        // on which to base other dialog implementations.
        
        Box vb = ModalDialog.makeMarginVBox(this, ModalDialog.OUTER_MARGIN);
      
        this.nameText = ModalDialog.makeLineEdit(vb, "Name:", 'n', this.entity.name);
        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
        
        // attributes
        {
            Box attrBox = ModalDialog.makeHBox(vb);
            
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
            vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
        }

        // Shape
        {
            this.shapeChooser = ModalDialog.makeEnumChooser(
                vb,
                "Shape:",
                's',
                EntityShape.class,
                this.entity.shape);
            vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
        }
        
        // x, y
        {
            Box locBox = ModalDialog.makeHBox(vb);
            this.xText = ModalDialog.makeLineEdit(locBox, "X:", 'x', String.valueOf(this.entity.loc.x));
            locBox.add(Box.createHorizontalStrut(ModalDialog.CONTROL_PADDING));
            this.yText = ModalDialog.makeLineEdit(locBox, "Y:", 'y', String.valueOf(this.entity.loc.y));
            vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
            ModalDialog.disallowVertStretch(locBox);
        }
         
        // w, h
        {
            Box sizeBox = ModalDialog.makeHBox(vb);
            this.wText = ModalDialog.makeLineEdit(sizeBox, "W:", 'w', String.valueOf(this.entity.size.width));
            sizeBox.add(Box.createHorizontalStrut(ModalDialog.CONTROL_PADDING));
            this.hText = ModalDialog.makeLineEdit(sizeBox, "H:", 'h', String.valueOf(this.entity.size.height));
            vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
            ModalDialog.disallowVertStretch(sizeBox);
        }
        
        this.finishBuildingDialog(vb);
    }

    /** React to the OK button being pressed. */
    @Override
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
        super.okPressed();
    }
    
    /** Show the edit dialog for Entity, waiting until the user closes the dialog
      * before returning.  If the user presses OK, 'entity' will be updated and
      * true returned.  Otherwise, 'entity' is not modified, and false is returned. */
    public static boolean exec(Component documentParent, Entity entity)
    {
        EntityDialog dialog = new EntityDialog(documentParent, entity);
        return dialog.exec();
    }
}

// EOF
