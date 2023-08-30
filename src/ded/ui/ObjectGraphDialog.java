// ObjectGraphDialog.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.json.JSONException;
import org.json.JSONObject;

import util.Util;
import util.swing.ModalDialog;

import ded.model.Diagram;
import ded.model.ObjectGraph;

public class ObjectGraphDialog extends ModalDialog {
    // ---- data ----
    private static final long serialVersionUID = 1753124297189267286L;

    /** Diagram whose properties will be edited. */
    private Diagram diagram;

    // Controls.
    private JTextArea graphJsonTextArea;

    // ---- methods ----
    public ObjectGraphDialog(Component parent, Diagram d)
    {
        super(parent, "Object Graph");
        this.diagram = d;

        Box vb = ModalDialog.makeMarginVBox(this, ModalDialog.OUTER_MARGIN);

        // This is of course an extremely crude editing interface.  I
        // envision primarily using it as a vehicle for copying and
        // pasting to or from, rather than a place to actually edit the
        // contents.
        this.graphJsonTextArea = new JTextArea(
            this.diagram.objectGraph.toString());
        disableTabInTextArea(this.graphJsonTextArea);

        JScrollPane scroll = new JScrollPane(this.graphJsonTextArea);
        scroll.setPreferredSize(new Dimension(500,500));

        vb.add(scroll);

        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        m_helpText = Util.readResourceString(
            "/resources/helptext/ObjectGraphDialog.txt");

        this.finishBuildingDialog(vb);
    }

    @Override
    public void okPressed()
    {
        try {
            JSONObject json = new JSONObject(
                this.graphJsonTextArea.getText());
            this.diagram.objectGraph = new ObjectGraph(json);
        }
        catch (JSONException e) {
            JOptionPane.showMessageDialog(this,
                e.getMessage(),
                "Error parsing JSON",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        super.okPressed();
    }

    /** Show the dialog, waiting until the user closes the dialog
      * before returning.  If the user presses OK, 'diagram' will be updated and
      * true returned.  Otherwise, 'diagram' is not modified, and false is returned. */
    public static boolean exec(Component documentParent, Diagram diagram)
    {
        return (new ObjectGraphDialog(documentParent, diagram)).exec();
    }
}

// EOF
