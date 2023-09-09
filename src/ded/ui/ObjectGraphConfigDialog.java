// ObjectGraphConfigDialog.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.Component;

import org.json.JSONException;
import org.json.JSONObject;

import util.Util;

import ded.model.Diagram;
import ded.model.ObjectGraphConfig;

import ded.ui.EditJsonDialog;


/** Dialog to edit Diagram.m_objectGraphConfig. */
public class ObjectGraphConfigDialog extends EditJsonDialog {
    // ---- data ----
    private static final long serialVersionUID = -1525842890592454353L;

    /** Diagram whose properties will be edited. */
    private Diagram m_diagram;

    // ---- methods ----
    public ObjectGraphConfigDialog(Component parent, Diagram diagram)
        throws JSONException
    {
        super(parent,
            "Object Graph Configuration",
            Util.readResourceString_joinAdjacentLines(
                "/resources/helptext/ObjectGraphConfigDialog.txt"),
            diagram.m_objectGraphConfig.toJSON());

        m_diagram = diagram;

        this.finishBuildingDialog(m_primaryVBox);
    }

    @Override
    protected boolean processEditedJSONObject(JSONObject json)
        throws JSONException
    {
        m_diagram.m_objectGraphConfig =
            new ObjectGraphConfig(json);

        return true;
    }

    /** Show the dialog, waiting until the user closes the dialog
      * before returning.  If the user presses OK, 'diagram' will be updated and
      * true returned.  Otherwise, 'diagram' is not modified, and false is returned. */
    public static boolean exec(Component documentParent, Diagram diagram)
    {
        try {
            return (new ObjectGraphConfigDialog(documentParent, diagram)).exec();
        }
        catch (JSONException e) {
            return handleJSONExceptionFromCtor(documentParent, e);
        }
    }
}

// EOF
