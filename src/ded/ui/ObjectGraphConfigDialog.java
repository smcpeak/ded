// ObjectGraphConfigDialog.java
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
import ded.model.ObjectGraphConfig;

public class ObjectGraphConfigDialog extends ModalDialog {
    // ---- data ----
    private static final long serialVersionUID = -1525842890592454353L;

    /** Diagram whose properties will be edited. */
    private Diagram diagram;

    // Controls.
    private JTextArea jsonTextArea;

    // ---- methods ----
    public ObjectGraphConfigDialog(Component parent, Diagram d)
    {
        super(parent, "Object Graph Configuration");
        this.diagram = d;

        Box vb = ModalDialog.makeMarginVBox(this, ModalDialog.OUTER_MARGIN);

        // This is of course an extremely crude editing interface.  I
        // envision primarily using it as a vehicle for copying and
        // pasting to or from, rather than a place to actually edit the
        // contents.
        this.jsonTextArea = new JTextArea(
            this.diagram.m_objectGraphConfig.toString());
        disableTabInTextArea(this.jsonTextArea);

        JScrollPane scroll = new JScrollPane(this.jsonTextArea);
        scroll.setPreferredSize(new Dimension(500,500));

        vb.add(scroll);

        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        m_helpText = Util.readResourceString(
            "/resources/helptext/ObjectGraphConfigDialog.txt");

        this.finishBuildingDialog(vb);
    }

    @Override
    public void okPressed()
    {
        try {
            JSONObject json = new JSONObject(
                this.jsonTextArea.getText());
            this.diagram.m_objectGraphConfig =
                new ObjectGraphConfig(json);
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
        return (new ObjectGraphConfigDialog(documentParent, diagram)).exec();
    }
}

// EOF
