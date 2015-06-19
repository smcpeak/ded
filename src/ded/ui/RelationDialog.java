// RelationDialog.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.Component;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import util.Util;
import util.swing.ModalDialog;
import util.swing.SwingUtil;

import ded.model.ArrowStyle;
import ded.model.Diagram;
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
    private JTextField lineWidthField;
    private JComboBox<RoutingAlgorithm> routingChooser;
    private JComboBox<ArrowStyle> startArrowStyleChooser,
                                  endArrowStyleChooser;
    private JComboBox<String> lineColorChooser;

    // ------------------- methods ----------------------
    public RelationDialog(Component parent, Diagram diagram, Relation relation)
    {
        super(parent, "Edit Relation");

        this.relation = relation;

        Box vb = ModalDialog.makeMarginVBox(this, ModalDialog.OUTER_MARGIN);

        this.labelField = ModalDialog.makeLineEdit(vb, "Label", 'l', this.relation.label);
        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        this.lineWidthField = ModalDialog.makeLineEdit(vb, "Line width", 'w',
                this.relation.lineWidth==null? "" : String.valueOf(this.relation.lineWidth));
        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        // Routing algorithm.
        this.routingChooser = ModalDialog.makeEnumChooser(
            vb,
            "Routing algorithm",
            'r',
            RoutingAlgorithm.class,
            this.relation.routingAlg);
        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        // Arrow styles.
        this.startArrowStyleChooser = ModalDialog.makeEnumChooser(
            vb,
            "Start arrow style",
            's',
            ArrowStyle.class,
            this.relation.start.arrowStyle);
        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
        this.endArrowStyleChooser = ModalDialog.makeEnumChooser(
            vb,
            "End arrow style",
            'e',
            ArrowStyle.class,
            this.relation.end.arrowStyle);
        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        this.lineColorChooser =
            EntityDialog.makeColorChooser(diagram, vb, this.relation.lineColor);

        // It might be nice to allow the endpoints to be edited, but
        // that is challenging due to the ability to connect them to
        // Entities and Inheritances.

        this.finishBuildingDialog(vb);
    }

    @Override
    public void okPressed()
    {
        RoutingAlgorithm ra = (RoutingAlgorithm)this.routingChooser.getSelectedItem();
        ArrowStyle startStyle = (ArrowStyle)this.startArrowStyleChooser.getSelectedItem();
        ArrowStyle endStyle = (ArrowStyle)this.endArrowStyleChooser.getSelectedItem();

        this.relation.label = this.labelField.getText();

        String lwt = this.lineWidthField.getText();
        try {
            if (lwt.isEmpty()) {
                this.relation.lineWidth = null;
            }
            else {
                this.relation.lineWidth = Integer.valueOf(lwt);

                // I'd actually like 0 to be allowed and mean to not draw
                // the line.  But, at the moment, 0 is treated the same as
                // 1 during painting, so make it illegal.
                if (this.relation.lineWidth < 1) {
                    SwingUtil.errorMessageBox(this,
                        "Line width must be positive: "+this.relation.lineWidth);
                    return;      // no super.okPressed
                }
            }
        }
        catch (NumberFormatException e) {
            SwingUtil.errorMessageBox(this,
                "Cannot parse line width \""+lwt+"\": "+Util.getExceptionMessage(e));
            return;     // do *not* call super.okPressed()
        }

        this.relation.routingAlg = ra;
        this.relation.start.arrowStyle = startStyle;
        this.relation.end.arrowStyle = endStyle;
        this.relation.lineColor = (String)this.lineColorChooser.getSelectedItem();

        super.okPressed();
    }

    /** Launch the dialog, blocking until the dialog is dismissed.
      * If the user presses OK, 'relation' will be updated and true
      * returned.  Otherwise, false is returned and 'relation' is
      * not modified. */
    public static boolean exec(Component parent, Diagram diagram, Relation relation)
    {
        return (new RelationDialog(parent, diagram, relation)).exec();
    }
}

// EOF
