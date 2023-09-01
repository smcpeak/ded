// ObjectGraphDialog.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.Component;

import org.json.JSONException;
import org.json.JSONObject;

import util.Util;

import ded.model.Diagram;
import ded.model.ObjectGraph;

import ded.ui.EditJsonDialog;


/** Dialog to edit Diagram.objectGraph. */
public class ObjectGraphDialog extends EditJsonDialog {
    // ---- data ----
    private static final long serialVersionUID = -7376121462630597313L;

    /** Diagram whose properties will be edited. */
    private Diagram m_diagram;

    // ---- methods ----
    public ObjectGraphDialog(Component parent, Diagram diagram)
        throws JSONException
    {
        super(parent,
            "Object Graph",
            Util.readResourceString_joinAdjacentLines(
                "/resources/helptext/ObjectGraphDialog.txt"),
            diagram.objectGraph.toJSON());

        m_diagram = diagram;
    }

    @Override
    protected void processEditedJSONObject(JSONObject json)
        throws JSONException
    {
        m_diagram.objectGraph = new ObjectGraph(json);
    }

    /** Show the dialog, waiting until the user closes the dialog
      * before returning.  If the user presses OK, 'diagram' will be updated and
      * true returned.  Otherwise, 'diagram' is not modified, and false is returned. */
    public static boolean exec(Component documentParent, Diagram diagram)
    {
        try {
            return (new ObjectGraphDialog(documentParent, diagram)).exec();
        }
        catch (JSONException e) {
            return handleJSONExceptionFromCtor(documentParent, e);
        }
    }
}

// EOF
