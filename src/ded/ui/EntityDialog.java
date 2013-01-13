// EntityDialog.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.EnumSet;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import util.swing.ModalDialog;

import ded.model.Diagram;
import ded.model.Entity;
import ded.model.EntityShape;
import ded.model.ImageFillStyle;
import ded.model.ShapeFlag;

/** Dialog box to edit an Entity. */
public class EntityDialog extends ModalDialog
    implements ItemListener
{
    private static final long serialVersionUID = 1455207901388264571L;

    // -------------- private data --------------
    /** Entity being edited. */
    private Entity entity;

    /** Working copy of the shape flags.  This is what the shape flags
      * dialog edits.  We only copy it back into 'entity' if OK is
      * chosen on the outer entity dialog. */
    private EnumSet<ShapeFlag> shapeFlagsWorkingCopy;

    /** The shape that the working flags is based on.  At certain points,
      * we need to add default flags, and if the shape has changed, we
      * need to know what it changed from. */
    private EntityShape workingFlagsBaseShape;

    // Controls.
    private JTextField nameText;
    private JTextArea attributeText;
    private JComboBox shapeChooser;
    private JButton shapeFlagsButton;
    private JComboBox fillColorChooser;
    private JTextField xText, yText, wText, hText;
    private JLabel paramsLabel;
    private JTextField pText, qText;
    private JTextField imageFileNameText;
    private JComboBox imageFillStyleChooser;

    // -------------- methods ---------------
    public EntityDialog(Component documentParent, Diagram diagram, Entity entity)
    {
        super(documentParent, "Edit Entity");

        this.entity = entity;
        this.shapeFlagsWorkingCopy = this.entity.shapeFlags.clone();
        this.workingFlagsBaseShape = this.entity.shape;

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

        // shape
        {
            Box shapeBox = ModalDialog.makeHBox(vb);

            this.shapeChooser = ModalDialog.makeEnumChooser(
                shapeBox,
                "Shape:",
                's',
                EntityShape.class,
                this.entity.shape);
            this.shapeChooser.addItemListener(this);

            shapeBox.add(Box.createHorizontalStrut(ModalDialog.CONTROL_PADDING));

            this.shapeFlagsButton = new JButton("Flags");
            this.shapeFlagsButton.setMnemonic(KeyEvent.VK_G);
            this.shapeFlagsButton.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    EntityDialog.this.openShapeFlagsDialog();
                }
            });
            shapeBox.add(this.shapeFlagsButton);

            vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
        }

        // fill color
        {
            Vector<String> colors = new Vector<String>();

            // Defensive: If the current entity color is not in the
            // diagram colors, add it to the vector so that it is
            // in the dropdown.
            if (!diagram.namedColors.containsKey(this.entity.fillColor)) {
                colors.add(this.entity.fillColor);
            }

            // Add the diagram colors.
            for (String c : diagram.namedColors.keySet()) {
                colors.add(c);
            }

            this.fillColorChooser = ModalDialog.makeVectorChooser(
                vb,
                "Fill color:",
                'f',
                colors,
                this.entity.fillColor);

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

        // paramsLabel
        {
            Box hb = ModalDialog.makeHBox(vb);
            hb.add(this.paramsLabel = new JLabel());    // Text set later.
            hb.add(Box.createHorizontalGlue());
        }

        // shapeParams
        {
            String p="", q="";
            int[] params = this.entity.shapeParams;
            if (params != null) {
                if (params.length >= 1) {
                    p = String.valueOf(params[0]);
                }
                if (params.length >= 2) {
                    q = String.valueOf(params[1]);
                }
            }

            Box hb = ModalDialog.makeHBox(vb);
            this.pText = ModalDialog.makeLineEdit(hb, "P:", 'p', p);
            hb.add(Box.createHorizontalStrut(ModalDialog.CONTROL_PADDING));
            this.qText = ModalDialog.makeLineEdit(hb, "Q:", 'q', q);
            vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
            ModalDialog.disallowVertStretch(hb);
        }

        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
        this.imageFileNameText = ModalDialog.makeLineEdit(vb,
            "Image file name:", 'i', this.entity.imageFileName);

        // shape
        {
            vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
            this.imageFillStyleChooser = ModalDialog.makeEnumChooser(
                vb,
                "Image fill style:",
                'm',
                ImageFillStyle.class,
                this.entity.imageFillStyle);
            this.imageFillStyleChooser.addItemListener(this);
        }

        this.updateControls();
        this.finishBuildingDialog(vb);
    }

    /** Open the dialog for choosing the shape flags. */
    private void openShapeFlagsDialog()
    {
        EntityShape shape = (EntityShape)this.shapeChooser.getSelectedItem();
        this.updateWorkingFlagsBaseShape(shape);
        ShapeFlagsDialog.exec(this, this.shapeFlagsWorkingCopy, shape);
    }

    /** Restrict the working flags to those appropriate for 'newShape', and
      * add any default flags that are applicable to 'newShape' but not to
      * 'workingFlagsBaseShape'.  Then update the latter to be 'newShape'. */
    private void updateWorkingFlagsBaseShape(EntityShape newShape)
    {
        // Flags for old shape.
        EnumSet<ShapeFlag> allOldFlags = ShapeFlag.allFlagsForShape(this.workingFlagsBaseShape);

        // Flags for new shape.
        EnumSet<ShapeFlag> allNewFlags = ShapeFlag.allFlagsForShape(newShape);

        // Restrict flags to new.
        this.shapeFlagsWorkingCopy.retainAll(allNewFlags);

        // Add any default flag in 'allNewFlags - allOldFlags'.
        for (ShapeFlag flag : allNewFlags) {
            if (flag.isDefault && !allOldFlags.contains(flag)) {
                this.shapeFlagsWorkingCopy.add(flag);
            }
        }

        // Remember the new base shape.
        this.workingFlagsBaseShape = newShape;
    }

    /** React to the OK button being pressed. */
    @Override
    public void okPressed()
    {
        // Parse/validate all the fields first, including casting
        // objects from JComboBox to their expected type, so that
        // if there is a problem, we will bail before actually
        // modifying this.entity;

        int x, y, w, h, p=0, q=0;
        try {
            x = Integer.valueOf(this.xText.getText());
            y = Integer.valueOf(this.yText.getText());
            w = Integer.valueOf(this.wText.getText());
            h = Integer.valueOf(this.hText.getText());
            if (this.entity.shape.numParams >= 1) {
                p = Integer.valueOf(this.pText.getText());
            }
            if (this.entity.shape.numParams >= 2) {
                q = Integer.valueOf(this.qText.getText());
            }
        }
        catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "At least one of X/Y/W/H/P/Q is not a valid integer.",
                "Input Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        EntityShape shape = (EntityShape)this.shapeChooser.getSelectedItem();
        if (shape.numParams > 0 && (p < 0 || q < 0)) {
            JOptionPane.showMessageDialog(this,
                "P and Q must be non-negative.",
                "Input Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        String fillColor = (String)this.fillColorChooser.getSelectedItem();

        ImageFillStyle imageFillStyle = (ImageFillStyle)this.imageFillStyleChooser.getSelectedItem();

        // Make sure the shape flags are appropriate for the chosen shape.
        this.updateWorkingFlagsBaseShape(shape);

        // Update the entity.
        this.entity.name = this.nameText.getText();
        this.entity.attributes = this.attributeText.getText();
        this.entity.setShape(shape);      // Sets 'shapeParams' too.
        this.entity.shapeFlags = this.shapeFlagsWorkingCopy.clone();
        this.entity.setFillColor(fillColor);
        this.entity.loc.x = x;
        this.entity.loc.y = y;
        this.entity.size.width = w;
        this.entity.size.height = h;
        this.entity.imageFileName = this.imageFileNameText.getText();
        this.entity.imageFillStyle = imageFillStyle;

        // Not completely general at this time.
        if (this.entity.shape.numParams == 2) {
            this.entity.shapeParams[0] = p;
            this.entity.shapeParams[1] = q;
        }

        // Close dialog, signaling that a change was made.
        super.okPressed();
    }

    /** React to the shape dropdown being changed. */
    @Override
    public void itemStateChanged(ItemEvent e)
    {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            updateControls();
        }
    }

    /** Update certain dialog control state based on how the
      * other controls are currently set. */
    private void updateControls()
    {
        EntityShape shape = (EntityShape)this.shapeChooser.getSelectedItem();

        // Enable or disable P/Q based on which shape is active.
        boolean en = (shape.numParams == 2);
        this.pText.setEnabled(en);
        this.qText.setEnabled(en);

        // Set the params caption.
        if (shape == EntityShape.ES_CUBOID) {
            this.paramsLabel.setText("Cuboid extends left by P, up by Q pixels:");
            if (this.pText.getText().isEmpty()) {
                // Set a reasonable default.
                this.pText.setText("5");
                this.qText.setText("10");
            }
        }
        else if (shape == EntityShape.ES_WINDOW) {
            this.paramsLabel.setText("Window auto-resize center at (P,Q):");
            if (this.pText.getText().isEmpty()) {
                this.pText.setText(String.valueOf(this.entity.size.width/2));
                this.qText.setText(String.valueOf(this.entity.size.height/2));
            }
        }
        else {
            this.paramsLabel.setText("Shape params (none for this shape):");
        }

        // Enable or disable shape flags button.
        this.shapeFlagsButton.setEnabled(!ShapeFlag.allFlagsForShape(shape).isEmpty());
    }

    /** Show the edit dialog for Entity, waiting until the user closes the dialog
      * before returning.  If the user presses OK, 'entity' will be updated and
      * true returned.  Otherwise, 'entity' is not modified, and false is returned. */
    public static boolean exec(Component documentParent, Diagram diagram, Entity entity)
    {
        EntityDialog dialog = new EntityDialog(documentParent, diagram, entity);
        return dialog.exec();
    }
}

// EOF
