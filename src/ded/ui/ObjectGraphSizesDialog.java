// ObjectGraphSizesDialog.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.util.HashSet;
import java.util.Set;

import java.awt.Component;

import javax.swing.Box;
import javax.swing.JTextField;

import util.swing.ModalDialog;

import ded.model.Diagram;
import ded.model.Entity;
import ded.model.ObjectGraphNode;


/** Dialog to show sizes of the object graph and allow the user to
  * prune the nodes that are not used in the diagram. */
public class ObjectGraphSizesDialog extends ModalDialog {
    // ---- private class data ----
    private static final long serialVersionUID = 4005317867975062319L;

    // ---- private instance data ----
    /** Diagram to show and edit. */
    private Diagram m_diagram;

    // Controls
    private JTextField m_numTotalNodesText;
    private JTextField m_numTotalJSONBytesText;
    private JTextField m_numTotalPNGBytesText;
    private JTextField m_numUsedNodesText;
    private JTextField m_numEntitiesWithInvalidIDsText;

    // ---- methods -----
    public ObjectGraphSizesDialog(Component parent, Diagram diagram)
    {
        super(parent, "Object Graph Sizes");
        m_diagram = diagram;

        // Used mnemonics: ijpu

        Box vb = ModalDialog.makeMarginVBox(this, ModalDialog.OUTER_MARGIN);

        m_numTotalNodesText = makeNumberTextDisplay(
            vb, "Number of nodes", 'n',
            "Number of nodes in the graph, including those that are "+
            "not used in the diagram.");

        m_numTotalJSONBytesText = makeNumberTextDisplay(
            vb, "Number of JSON bytes", 'j',
            "Number of bytes required to store the graph in the JSON "+
            ".ded file, including for indentation.");

        m_numTotalPNGBytesText = makeNumberTextDisplay(
            vb, "Number of PNG bytes", 'p',
            "Number of bytes required to store the graph in the PNG "+
            "file, taking into account the compression and lack of "+
            "indentation.");

        m_numUsedNodesText = makeNumberTextDisplay(
            vb, "Number of used nodes", 'u',
            "Number of nodes in the graph that are used by at least "+
            "one entity in the diagram.");

        m_numEntitiesWithInvalidIDsText = makeNumberTextDisplay(
            vb, "Number of entities with invalid node IDs", 'i',
            "Number of entities in the diagram that have a non-empty "+
            "object node ID, but that ID does not correspond to any "+
            "node in the graph.");

        recomputeSizes();

        this.finishBuildingDialog(vb);
    }

    /** Create a control to display the value of a number.  Also add
      * vertical control padding afterward. */
    private JTextField makeNumberTextDisplay(
        Box vb,
        String label,
        char mnemonic,
        String helpText)
    {
        JTextField textField = makeLineEditWithHelp(
            vb, label, mnemonic, "" /*initVal*/, this, helpText);

        // This is just a display, so not editable.
        textField.setEditable(false);

        // Right-align due to displaying numeric data.
        textField.setHorizontalAlignment(JTextField.RIGHT);

        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        return textField;
    }

    /** Compute the sizes shown in the text fields based on
      * 'm_diagram'. */
    private void recomputeSizes()
    {
        Set<String> validIDs = m_diagram.objectGraph.m_nodes.keySet();

        Set<String> usedIDs = new HashSet<String>();
        int numEntitiesWithInvalidID = 0;
        for (Entity entity : m_diagram.entities) {
            if (entity.hasObjectGraphNodeID()) {
                String id = entity.objectGraphNodeID;
                if (validIDs.contains(id)) {
                    usedIDs.add(id);
                }
                else {
                    ++numEntitiesWithInvalidID;
                }
            }
        }

        m_numTotalNodesText.setText(""+
            validIDs.size());
        m_numTotalJSONBytesText.setText(""+
            Diagram.numGraphJSONBytes(m_diagram.objectGraph));
        m_numTotalPNGBytesText.setText(""+
            Diagram.numGraphPNGBytes(m_diagram.objectGraph));
        m_numUsedNodesText.setText(""+
            usedIDs.size());
        m_numEntitiesWithInvalidIDsText.setText(""+
            numEntitiesWithInvalidID);
    }

    @Override
    public void okPressed()
    {
        super.okPressed();
    }

    /** Show the dialog and return when the user dismisses it. */
    public static boolean exec(Component parent, Diagram diagram)
    {
        ObjectGraphSizesDialog dialog =
            new ObjectGraphSizesDialog(parent, diagram);
        return dialog.exec();
    }
}


// EOF
