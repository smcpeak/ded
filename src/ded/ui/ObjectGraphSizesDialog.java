// ObjectGraphSizesDialog.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.util.HashSet;
import java.util.Set;

import java.awt.Component;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JTextField;

import util.swing.ModalDialog;
import util.swing.SwingUtil;

import ded.model.Diagram;
import ded.model.Entity;
import ded.model.ObjectGraph;
import ded.model.ObjectGraphNode;

import static util.StringUtil.fmt;


/** Dialog to show sizes of the object graph and allow the user to
  * prune the nodes that are not used in the diagram. */
public class ObjectGraphSizesDialog extends ModalDialog {
    // ---- private class data ----
    private static final long serialVersionUID = 4005317867975062319L;

    // ---- private instance data ----
    /** Diagram to show and edit.  This is not modified until the
      * user presses Ok. */
    private Diagram m_diagram;

    /** Graph we are working on.  This is a mutable copy of the one in
      * 'm_diagram'. */
    private ObjectGraph m_graph;

    // Controls
    private JTextField m_numTotalNodesText;
    private JTextField m_numTotalDEDBytesText;
    private JTextField m_numTotalPNGBytesText;
    private JTextField m_numUsedNodesText;
    private JTextField m_numEntitiesWithInvalidIDsText;

    // ---- methods -----
    public ObjectGraphSizesDialog(Component parent, Diagram diagram)
    {
        super(parent, "Object Graph Sizes");

        m_diagram = diagram;

        // Work with a copy of the graph until the user presses Ok.
        m_graph = new ObjectGraph(m_diagram.objectGraph);

        // Used mnemonics: diptu

        Box vb = ModalDialog.makeMarginVBox(this, ModalDialog.OUTER_MARGIN);

        m_numTotalNodesText = makeNumberTextDisplay(
            vb, "Number of nodes", 'n',
            "Number of nodes in the graph, including those that are "+
            "not used in the diagram.");

        m_numTotalDEDBytesText = makeNumberTextDisplay(
            vb, "Number of DED bytes", 'd',
            "Number of bytes required to store the graph in the DED "+
            "(JSON) file, including for indentation.");

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

        // Trim button
        {
            Box hb = ModalDialog.makeHBox(vb);
            hb.add(Box.createHorizontalGlue());
            hb.add(makeButton("Trim unused nodes", 't',
                              new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ObjectGraphSizesDialog.this.trimPressed();
                }
            }));
            hb.add(Box.createHorizontalStrut(ModalDialog.CONTROL_PADDING));
            hb.add(ModalDialog.makeHelpButton(this,
                "Help: Trim unused nodes",
                "Pressing this button removes all nodes from the "+
                "graph that are not referenced by any entity, thereby "+
                "saving space in the DED and PNG files.  This can be "+
                "undone using Edit -> Undo menu afterwards (or by "+
                "pressing Cancel before pressing Ok)."));

            vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
        }

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
      * 'm_graph'. */
    private void recomputeSizes()
    {
        Set<String> validIDs = m_graph.m_nodes.keySet();

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
        m_numTotalDEDBytesText.setText(""+
            Diagram.numGraphDEDBytes(m_graph));
        m_numTotalPNGBytesText.setText(""+
            Diagram.numGraphPNGBytes(m_graph));
        m_numUsedNodesText.setText(""+
            usedIDs.size());
        m_numEntitiesWithInvalidIDsText.setText(""+
            numEntitiesWithInvalidID);
    }

    /** Remove unused nodes from the graph. */
    private void trimPressed()
    {
        int numRemoved = trimGraph(m_diagram, m_graph);

        // Update the controls now so the user can see the new values
        // behind the message box.
        recomputeSizes();

        SwingUtil.informationMessageBox(this,
            "Graph Trimmed",
            fmt("Trimming removed %1$d unused nodes.", numRemoved));
    }

    /** Apply the trimming algorithm to 'graph', using 'diagram' as a
      * reference, modifying 'graph' in the process.  Return the number
      * of nodes removed. */
    // Package-private to allow access by test code.
    /*package*/ static int trimGraph(Diagram diagram, ObjectGraph graph)
    {
        // What is used?
        Set<String> usedIDs = new HashSet<String>();
        for (Entity entity : diagram.entities) {
            if (entity.hasObjectGraphNodeID()) {
                usedIDs.add(entity.objectGraphNodeID);
            }
        }

        // What is not used?
        Set<String> toRemove = new HashSet<String>();
        for (String id : graph.m_nodes.keySet()) {
            if (!usedIDs.contains(id)) {
                toRemove.add(id);
            }
        }

        // Remove the unused.
        for (String id : toRemove) {
            graph.m_nodes.remove(id);
        }

        return toRemove.size();
    }

    @Override
    public void okPressed()
    {
        // Move the potentially modified graph back into the diagram.
        m_diagram.objectGraph = m_graph;

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
