// RelationControlPointDialog.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.Component;
import java.awt.Point;

import javax.swing.Box;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import ded.model.Relation;
import util.swing.ModalDialog;

/** Dialog to edit a Relation control point. */
public class RelationControlPointDialog extends ModalDialog {
    private static final long serialVersionUID = -6388398674386321322L;

    // ----------------- instance data ------------------
    /** Relation we are editing. */
    private Relation relation;

    /** The index of the control point to edit. */
    private int whichCP;

    // Controls.
    private JTextField xText, yText;

    // ------------------- methods ----------------------
    public RelationControlPointDialog(Component parent, Relation relation, int whichCP)
    {
        super(parent, "Edit Control Point");

        this.relation = relation;
        this.whichCP = whichCP;

        Box vb = ModalDialog.makeMarginVBox(this, ModalDialog.OUTER_MARGIN);

        // Don't let the dialog layout get too small.
        vb.add(Box.createHorizontalStrut(200));

        // x, y
        {
            Point curPt = this.relation.controlPts.get(this.whichCP);
            Box locBox = ModalDialog.makeHBox(vb);
            this.xText = ModalDialog.makeLineEdit(locBox, "X", 'x', String.valueOf(curPt.x));
            locBox.add(Box.createHorizontalStrut(ModalDialog.CONTROL_PADDING));
            this.yText = ModalDialog.makeLineEdit(locBox, "Y", 'y', String.valueOf(curPt.y));
            vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
            ModalDialog.disallowVertStretch(locBox);
        }

        this.finishBuildingDialog(vb);
    }

    @Override
    public void okPressed()
    {
        // Validate first.
        int x, y;
        try {
            x = Integer.valueOf(this.xText.getText());
            y = Integer.valueOf(this.yText.getText());
        }
        catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "At least one of x or y is not a valid integer.",
                "Input Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        Point pt = this.relation.controlPts.get(this.whichCP);
        pt.x = x;
        pt.y = y;

        super.okPressed();
    }
}

// EOF
