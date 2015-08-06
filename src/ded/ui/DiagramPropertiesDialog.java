// DiagramPropertiesDialog.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.Component;

import javax.swing.Box;
import javax.swing.JComboBox;

import util.swing.ModalDialog;

import ded.model.Diagram;

public class DiagramPropertiesDialog extends ModalDialog {
    // ---- data ----
    private static final long serialVersionUID = 1753124297189267286L;

    /** Diagram whose properties will be edited. */
    private Diagram diagram;

    // Controls.
    private JComboBox<String> backgroundColorChooser;

    // ---- methods ----
    public DiagramPropertiesDialog(Component parent, Diagram d)
    {
        super(parent, "Diagram Properties");
        this.diagram = d;

        Box vb = ModalDialog.makeMarginVBox(this, ModalDialog.OUTER_MARGIN);

        this.backgroundColorChooser = EntityDialog.makeColorChooser(
            this.diagram, vb, this.diagram.backgroundColor, "Diagram background color", 'b');
        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        this.finishBuildingDialog(vb);
    }

    @Override
    public void okPressed()
    {
        this.diagram.backgroundColor =
            (String)this.backgroundColorChooser.getSelectedItem();
        super.okPressed();
    }

    /** Show the dialog, waiting until the user closes the dialog
      * before returning.  If the user presses OK, 'diagram' will be updated and
      * true returned.  Otherwise, 'diagram' is not modified, and false is returned. */
    public static boolean exec(Component documentParent, Diagram diagram)
    {
        return (new DiagramPropertiesDialog(documentParent, diagram)).exec();
    }
}

// EOF
