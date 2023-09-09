// ObjectGraphDialog.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import ded.model.Diagram;
import ded.model.ObjectGraph;
import ded.ui.EditJsonDialog;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JTextField;
import org.json.JSONException;
import org.json.JSONObject;
import util.Util;
import util.json.JSONUtil;
import util.swing.ModalDialog;
import util.swing.SwingUtil;

import static util.StringUtil.fmt;


/** Dialog to edit Diagram.objectGraph. */
public class ObjectGraphDialog extends EditJsonDialog {
    // ---- data ----
    private static final long serialVersionUID = -7376121462630597313L;

    /** Diagram controller containing the diagram whose properties will
        be edited. */
    private DiagramController m_diagramController;

    // Controls.
    private JTextField m_sourceFileNameField;

    // ---- methods ----
    public ObjectGraphDialog(DiagramController diagramController)
        throws JSONException
    {
        super(diagramController,
            "Object Graph",
            Util.readResourceString_joinAdjacentLines(
                "/resources/helptext/ObjectGraphDialog.txt"),
            diagramController.diagram.objectGraph.toJSON());

        m_diagramController = diagramController;

        final ObjectGraphDialog ths = this;

        // Used: mnemonics: hls
        //
        // H is for the dialog-wide Help button.

        Box vb = m_primaryVBox;
        vb.add(ModalDialog.makeVCPadStrut());

        // Source file and Load button.
        {
            Box hb = ModalDialog.makeHBox(vb);

            m_sourceFileNameField = ModalDialog.makeLineEdit(
                hb, "Source file", 's',
                getDiagram().m_objectGraphSourceFile);

            hb.add(makeHCPadStrut());

            JButton loadButton = new JButton("Load");
            loadButton.setMnemonic('l');
            loadButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ths.loadButtonPressed();
                }
            });
            hb.add(loadButton);

            hb.add(makeHCPadStrut());

            hb.add(makeHelpButton(this, "Source file",
                Util.readResourceString_joinAdjacentLines(
                    "/resources/helptext/ObjectGraphDialog-sourceFile.txt")));
        }

        vb.add(ModalDialog.makeVCPadStrut());
        this.finishBuildingDialog(vb);
    }

    private Diagram getDiagram()
    {
        return m_diagramController.diagram;
    }

    @Override
    protected boolean processEditedJSONObject(JSONObject json)
        throws JSONException
    {
        Diagram d = getDiagram();

        d.objectGraph = new ObjectGraph(json);

        d.m_objectGraphSourceFile = m_sourceFileNameField.getText();

        return true;
    }

    /** Respond to the user pressing the Load button. */
    private void loadButtonPressed()
    {
        String fname = m_sourceFileNameField.getText();
        if (fname.isEmpty()) {
            SwingUtil.errorMessageBox(this,
                "The source file name is empty.");
            return;
        }

        // First read the existing data so we can compare it to the new
        // data.
        JSONObject oldGraphObj = null;
        try {
            oldGraphObj = JSONUtil.readObjectFromString(
                m_jsonTextArea.getText());
        }
        catch (Exception e) {
            // Ignore, thereby leaving 'oldGraphObj' as null, and hence
            // not equal to whatever we read from the source file.
        }

        // Now load the new data.
        File sourceFile = m_diagramController.getRelativeFile(fname);
        try {
            JSONObject newGraphObj =
                JSONUtil.readObjectFromFile(sourceFile);

            if (JSONUtil.equalJSONObjects(oldGraphObj, newGraphObj)) {
                SwingUtil.informationMessageBox(this, "Load Graph Data",
                    fmt("The existing graph data is the same as what "+
                        "was loaded from \"%s\" (when compared as JSON), "+
                        "so has been discarded.",
                        fname));
                return;
            }

            // Update the control to contain the new data.
            m_jsonTextArea.setText(newGraphObj.toString(2 /*indent*/));

            SwingUtil.informationMessageBox(this, "Load Graph Data",
                fmt("Loaded new data from \"%s\".",
                    fname));
        }
        catch (Exception e) {
            SwingUtil.errorMessageBox(this, fmt(
                "While reading \"%s\": %s",
                sourceFile.toString(),
                Util.getExceptionMessage(e)));
        }
    }

    /** Show the dialog, waiting until the user closes the dialog before
        returning.  If the user presses OK, the diagram will be updated
        and true returned.  Otherwise, 'diagram' is not modified, and
        false is returned. */
    public static boolean exec(DiagramController diagramController)
    {
        try {
            return (new ObjectGraphDialog(diagramController)).exec();
        }
        catch (JSONException e) {
            return handleJSONExceptionFromCtor(diagramController, e);
        }
    }
}

// EOF
