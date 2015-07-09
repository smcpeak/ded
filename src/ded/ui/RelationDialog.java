// RelationDialog.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.Component;

import java.util.ArrayList;

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
    private JComboBox<String> textColorChooser;
    private JTextField dashStructureField;

    // ------------------- methods ----------------------
    public RelationDialog(Component parent, Diagram diagram, Relation relation)
    {
        super(parent, "Edit Relation");

        this.relation = relation;

        Box vb = ModalDialog.makeMarginVBox(this, ModalDialog.OUTER_MARGIN);

        this.labelField = ModalDialog.makeLineEdit(vb, "Label", 'l', this.relation.label);
        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        this.lineWidthField = ModalDialog.makeLineEditWithHelp(vb, "Line width", 'w',
            this.relation.lineWidth==null? "" : String.valueOf(this.relation.lineWidth),
            parent,
            "This is the width of the relation lines in pixels.  "+
            "If this box is empty, the default is used: 1 for "+
            "normal relations, 2 for inheritances.");
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
            EntityDialog.makeColorChooser(diagram, vb, this.relation.lineColor, "Line color", 'i');
        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        this.textColorChooser =
            EntityDialog.makeColorChooser(diagram, vb, this.relation.textColor, "Text color", 't');
        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        this.dashStructureField = ModalDialog.makeLineEditWithHelp(vb, "Dash structure", 'd',
            dashStructureToString(this.relation.dashStructure),
            parent,
            "When this box is empty, the line is solid.  Otherwise, "+
            "the box contains a space-separated sequence of non-negative integers "+
            "representing the lengths, in pixels, of alternately opaque "+
            "and transparent line segments, beginning with opaque.  When "+
            "the sequence is exhausted, it repeats, starting again with opaque.\n"+
            "\n"+
            "Typical values:\n"+
            "* Dashed: 5 2\n"+
            "* Dotted: 1 2\n"+
            "* Dash-dot: 5 2 1 2\n"+
            "\n"+
            "Also useful is \"0 10 2\" with line width 2 for an ellipsis effect.");
        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        // It might be nice to allow the endpoints to be edited, but
        // that is challenging due to the ability to connect them to
        // Entities and Inheritances.

        this.finishBuildingDialog(vb);
    }

    /** Convert a Relation 'dashStructure' sequence into a string of
      * space-separated integers. */
    private static String dashStructureToString(ArrayList<Integer> dashStructure)
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Integer i : dashStructure) {
            if (!first) {
                sb.append(" ");
            }
            first = false;
            sb.append(i.toString());
        }
        return sb.toString();
    }

    /** Convert a space-separated sequence of integers into an array.
      * Throws RuntimeException if the input is malformed. */
    public static ArrayList<Integer> stringToDashStructure(String str)
    {
        ArrayList<Integer> ret = new ArrayList<Integer>();

        String intro = "The dash structure must consist of a space-separated sequence "+
                       "of non-negative integers.  ";

        boolean atLeastOne = false;
        boolean atLeastOnePositive = false;
        for (String tok : str.split("\\s+")) {
            if (tok.isEmpty()) {
                // Entire string is empty, and/or it begins with whitespace
                // (curiously, trailing whitespace is discarded by 'split').
                continue;
            }
            try {
                Integer i = Integer.valueOf(tok);
                if (i < 0) {
                    throw new RuntimeException(intro +
                        "However, the element \""+i+"\" is negative.");
                }
                ret.add(i);

                atLeastOne = true;
                if (i > 0) {
                    atLeastOnePositive = true;
                }
            }
            catch (NumberFormatException e) {
                throw new RuntimeException(intro +
                    "However, the element \""+tok+
                    "\" cannot be parsed as an integer.");
            }
        }

        if (atLeastOne && !atLeastOnePositive) {
            throw new RuntimeException(
                "All elements of the dash structure list are zero.  "+
                "At least one must be positive, or else the "+
                "list must be empty.");
        }

        return ret;
    }

    @Override
    public void okPressed()
    {
        RoutingAlgorithm ra = (RoutingAlgorithm)this.routingChooser.getSelectedItem();
        ArrowStyle startStyle = (ArrowStyle)this.startArrowStyleChooser.getSelectedItem();
        ArrowStyle endStyle = (ArrowStyle)this.endArrowStyleChooser.getSelectedItem();

        String label = this.labelField.getText();

        String lwt = this.lineWidthField.getText();
        Integer lineWidth = null;
        try {
            if (!lwt.isEmpty()) {
                lineWidth = Integer.valueOf(lwt);

                // I'd actually like 0 to be allowed and mean to not draw
                // the line.  But, at the moment, 0 is treated the same as
                // 1 during painting, so make it illegal.
                if (lineWidth < 1) {
                    SwingUtil.errorMessageBox(this,
                        "Line width must be positive: "+lineWidth);
                    return;      // no super.okPressed
                }
            }
        }
        catch (NumberFormatException e) {
            SwingUtil.errorMessageBox(this,
                "Cannot parse line width \""+lwt+"\": "+Util.getExceptionMessage(e));
            return;     // do *not* call super.okPressed()
        }

        ArrayList<Integer> dashStructure;
        try {
            dashStructure = stringToDashStructure(this.dashStructureField.getText());
        }
        catch (RuntimeException e) {
            SwingUtil.errorMessageBox(this, e.getLocalizedMessage());
            return;
        }

        // We are now committed to closing the dialog without an error message.
        // Update the underlying model object.
        this.relation.label = label;
        this.relation.routingAlg = ra;
        this.relation.start.arrowStyle = startStyle;
        this.relation.end.arrowStyle = endStyle;
        this.relation.lineColor = (String)this.lineColorChooser.getSelectedItem();
        this.relation.textColor = (String)this.textColorChooser.getSelectedItem();
        this.relation.lineWidth = lineWidth;
        this.relation.dashStructure = dashStructure;

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
