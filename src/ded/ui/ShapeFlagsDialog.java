// ShapeFlagsDialog.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.Component;
import java.awt.Dimension;

import java.util.ArrayList;
import java.util.EnumSet;

import javax.swing.Box;
import javax.swing.JCheckBox;

import util.swing.ModalDialog;

import ded.model.EntityShape;
import ded.model.ShapeFlag;

/** Dialog to edit a set of shape flags. */
public class ShapeFlagsDialog extends ModalDialog {
    // ------------------------ constants -----------------------
    private static final long serialVersionUID = 1317320085090932650L;

    // ---------------------- instance data ---------------------
    /** Set of flags initially set, and the set to update upon OK. */
    private EnumSet<ShapeFlag> shapeFlags;

    /** List of all checkboxes, in order created. */
    private ArrayList<JCheckBox> checkboxList = new ArrayList<JCheckBox>();

    /** List of all the flags for the current shape, in same order as
      * 'checkboxList'. */
    private ArrayList<ShapeFlag> allFlagsList = new ArrayList<ShapeFlag>();

    // ------------------------- methods ------------------------
    public ShapeFlagsDialog(
        Component parent,
        EnumSet<ShapeFlag> shapeFlags,
        EntityShape shape)
    {
        super(parent, "Shape flags for "+shape);
        this.shapeFlags = shapeFlags;

        Box vb = ModalDialog.makeMarginVBox(this, ModalDialog.OUTER_MARGIN);

        // Ensure the dialog does not get so thin that its title
        // is abbreviated to nothing.
        vb.add(Box.createRigidArea(new Dimension(300, 0)));

        // Flags checkboxes.
        EnumSet<ShapeFlag> allFlags = ShapeFlag.allFlagsForShape(shape);
        for (ShapeFlag flag : allFlags) {
            Box hb = ModalDialog.makeHBox(vb);
            JCheckBox cb = new JCheckBox(flag.toString());
            if (this.shapeFlags.contains(flag)) {
                cb.setSelected(true);
            }
            hb.add(cb);
            hb.add(Box.createHorizontalGlue());

            this.checkboxList.add(cb);
            this.allFlagsList.add(flag);

            vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
        }

        this.finishBuildingDialog(vb);
    }

    @Override
    public void okPressed()
    {
        this.shapeFlags.clear();
        for (int i=0; i < this.checkboxList.size(); i++) {
            JCheckBox cb = this.checkboxList.get(i);
            if (cb.isSelected()) {
                this.shapeFlags.add(this.allFlagsList.get(i));
            }
        }

        super.okPressed();
    }

    /** Show the edit dialog for ShapeFlags, waiting until the user closes the dialog
      * before returning.  If the user presses OK, 'shapeFlags' will be updated and
      * true returned.  Otherwise, 'shapeFlags' is not modified, and false is returned.
      *
      * 'shape' is the current shape. */
    public static boolean exec(
        Component parent,
        EnumSet<ShapeFlag> shapeFlags,
        EntityShape shape)
    {
        ShapeFlagsDialog dialog = new ShapeFlagsDialog(parent, shapeFlags, shape);
        return dialog.exec();
    }
}
