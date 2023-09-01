// EditJsonDialog.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import util.Util;
import util.swing.ModalDialog;

import ded.model.Diagram;
import ded.model.ObjectGraphConfig;

/** Dialog to show some data as JSON in order to allow it to be easily
  * imported or exported in that format. */
public class EditJsonDialog extends ModalDialog {
    // ---- data ----
    private static final long serialVersionUID = -5359211540205542816L;

    // Controls.
    private JTextArea m_jsonTextArea;

    // ---- methods ----
    public EditJsonDialog(
        Component parent,
        String title,
        String helpText,
        Object jsonValue) throws JSONException
    {
        super(parent, title);

        String initialJsonText;
        assert(jsonValue != null);
        if (jsonValue instanceof JSONObject) {
            initialJsonText = ((JSONObject)jsonValue).toString(2 /*indent*/);
        }
        else {
            initialJsonText = ((JSONArray)jsonValue).toString(2 /*indent*/);
        }

        Box vb = ModalDialog.makeMarginVBox(this, ModalDialog.OUTER_MARGIN);

        // This is of course an extremely crude editing interface.  I
        // envision primarily using it as a vehicle for copying and
        // pasting to or from, rather than a place to actually edit the
        // contents.
        m_jsonTextArea = new JTextArea(initialJsonText);
        disableTabInTextArea(m_jsonTextArea);

        // Use a monospace font for the JSON.
        Font origFont = m_jsonTextArea.getFont();
        Font newFont = new Font(Font.MONOSPACED, origFont.getStyle(),
                                origFont.getSize());
        m_jsonTextArea.setFont(newFont);

        JScrollPane scroll = new JScrollPane(m_jsonTextArea);
        scroll.setPreferredSize(new Dimension(500,500));

        vb.add(scroll);

        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        m_helpText = helpText;

        this.finishBuildingDialog(vb);
    }

    @Override
    public void okPressed()
    {
        try {
            // Parse using the tokener interface so we can handle either
            // an object or an array.
            JSONTokener tokener = new JSONTokener(
                m_jsonTextArea.getText());
            Object jsonValue = tokener.nextValue();

            if (jsonValue instanceof JSONObject) {
                processEditedJSONObject((JSONObject)jsonValue);
            }
            else if (jsonValue instanceof JSONArray) {
                processEditedJSONArray((JSONArray)jsonValue);
            }
            else {
                throw new JSONException(
                    "JSON text must start with '{' or '['.");
            }
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

    /** A subclass that uses JSON objects must override this. */
    protected void processEditedJSONObject(JSONObject json)
        throws JSONException
    {
        throw new JSONException(
            "A JSON object (text starting with '{') is not valid here.");
    }

    /** A subclass that uses JSON arrays must override this. */
    protected void processEditedJSONArray(JSONArray json)
        throws JSONException
    {
        throw new JSONException(
            "A JSON array (text starting with '[') is not valid here.");
    }

    /** If, while constructing the superclass dialog, a JSONException
      * is thrown, the handler should call this function and return its
      * return value. */
    protected static boolean handleJSONExceptionFromCtor(
        Component documentParent,
        JSONException e)
    {
        // This indicates a bug in the program, not a user mistake.
        JOptionPane.showMessageDialog(documentParent,
            Util.getExceptionMessage(e),
            "Internal error creating JSON from diagram element",
            JOptionPane.ERROR_MESSAGE);
        return false;
    }

    // There is no 'exec' here.  The caller must do something with the
    // new 'm_jsonValue' before dropping the dialog object reference.
}

// EOF
