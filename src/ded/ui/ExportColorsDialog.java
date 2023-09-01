// ExportColorsDialog.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.Component;

import org.json.JSONException;
import org.json.JSONArray;

import util.Util;

import ded.model.Diagram;
import ded.model.ObjectGraph;

import ded.ui.EditJsonDialog;


/** Dialog to edit Diagram.namedColors. */
public class ExportColorsDialog extends EditJsonDialog {
    // ---- data ----
    private static final long serialVersionUID = -6764708521662411004L;

    /** Diagram whose properties will be edited. */
    private Diagram m_diagram;

    // ---- methods ----
    public ExportColorsDialog(Component parent, Diagram diagram)
        throws JSONException
    {
        super(parent,
            "Export/Import Custom Colors",
            Util.readResourceString_joinAdjacentLines(
                "/resources/helptext/ExportColorsDialog.txt"),
            Diagram.colorTableToJSON(diagram.namedColors));

        m_diagram = diagram;
    }

    @Override
    protected void processEditedJSONArray(JSONArray json)
        throws JSONException
    {
        m_diagram.namedColors =
            Diagram.parseColorTableFromJSON(json);
    }

    /** Show the dialog, waiting until the user closes the dialog
      * before returning.  If the user presses OK, 'diagram' will be updated and
      * true returned.  Otherwise, 'diagram' is not modified, and false is returned. */
    public static boolean exec(Component documentParent, Diagram diagram)
    {
        try {
            return (new ExportColorsDialog(documentParent, diagram)).exec();
        }
        catch (JSONException e) {
            return handleJSONExceptionFromCtor(documentParent, e);
        }
    }
}

// EOF
