// RelationDialog.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.Component;
import java.awt.event.KeyEvent;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import util.swing.ModalDialog;

import ded.model.Relation;
import ded.model.RoutingAlgorithm;

/** Dialog box to allow editing a Relation. */
public class RelationDialog extends ModalDialog {
    private static final long serialVersionUID = -566343397304437909L;

    // ----------------- instance data ------------------
    /** Relation we are editing. */
    private Relation relation;

    // Controls.
    private JTextField labelField;
    private JComboBox routingChooser;
    private JCheckBox owningCheckbox;

    // ------------------- methods ----------------------
    public RelationDialog(Component parent, Relation relation)
    {
        super(parent, "Edit Relation");

        this.relation = relation;

        Box vb = ModalDialog.makeMarginVBox(this, ModalDialog.OUTER_MARGIN);

        this.labelField = ModalDialog.makeLineEdit(vb, "Label:", 'l', this.relation.label);
        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        // Routing algorithm.
        this.routingChooser = ModalDialog.makeEnumChooser(
            vb,
            "Routing algorithm:",
            'r',
            RoutingAlgorithm.class,
            this.relation.routingAlg);
        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        {
            Box hb = ModalDialog.makeHBox(vb);
            hb.add(this.owningCheckbox =
                new JCheckBox("Owning relation", this.relation.owning));
            this.owningCheckbox.setMnemonic(KeyEvent.VK_O);
            hb.add(Box.createHorizontalGlue());
            vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
        }

        // It might be nice to allow the endpoints to be edited, but
        // that is challenging due to the ability to connect them to
        // Entities and Inheritances.

        this.finishBuildingDialog(vb);
    }

    @Override
    public void okPressed()
    {
        RoutingAlgorithm ra = (RoutingAlgorithm)this.routingChooser.getSelectedItem();

        this.relation.label = this.labelField.getText();
        this.relation.routingAlg = ra;
        this.relation.owning = this.owningCheckbox.isSelected();

        super.okPressed();
    }

    /** Launch the dialog, blocking until the dialog is dismissed.
      * If the user presses OK, 'relation' will be updated and true
      * returned.  Otherwise, false is returned and 'relation' is
      * not modified. */
    public static boolean exec(Component parent, Relation relation)
    {
        return (new RelationDialog(parent, relation)).exec();
    }
}

// EOF
