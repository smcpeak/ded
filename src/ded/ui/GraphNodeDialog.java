// GraphNodeDialog.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import util.swing.ModalDialog;

import ded.model.ObjectGraphNode;

/** Dialog to show the details of a graph node. */
public class GraphNodeDialog extends ModalDialog
{
    private static final long serialVersionUID = -87133330572297627L;

    // ---- private data ----
    /** The controller of the entity associated with the node whose data
      * we are showing.  Not null. */
    private EntityController m_controller;

    // Controls.
    private JTextField m_idTextField;
    private JTextArea m_attributesTextArea;

    // ---- methods ----
    public GraphNodeDialog(EntityController controller)
    {
        super(controller.diagramController, "Node Details");

        m_controller = controller;

        Box vb = ModalDialog.makeMarginVBox(this, ModalDialog.OUTER_MARGIN);

        // ID
        {
            Box hb = ModalDialog.makeHBox(vb);

            m_idTextField =
                ModalDialog.makeLineEdit(hb, "ID", 'i',
                    m_controller.entity.objectGraphNodeID);
            m_idTextField.setEditable(false);
        }

        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        // Attributes and pointers.
        {
            JLabel attributesLabel;
            {
                Box hb = ModalDialog.makeHBox(vb);
                attributesLabel = new JLabel("Attributes and pointers:");
                attributesLabel.setDisplayedMnemonic('a');
                hb.add(attributesLabel);
                hb.add(Box.createHorizontalGlue());
                vb.add(hb);
            }

            m_attributesTextArea = new JTextArea(getAttributesString());
            m_attributesTextArea.setEditable(false);
            attributesLabel.setLabelFor(m_attributesTextArea);

            JScrollPane scroll = new JScrollPane(m_attributesTextArea);
            scroll.setPreferredSize(new Dimension(500,500));
            vb.add(scroll);
        }

        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        this.finishBuildingDialog(vb);
    }

    /** Get the attributes and pointers of the node as a string. */
    private String getAttributesString()
    {
        StringBuilder sb = new StringBuilder();

        ObjectGraphNode node = m_controller.getGraphNode();
        if (node == null) {
            sb.append("(No node with this ID exists.)\n");
        }
        else {
            sb.append(m_controller.getGraphNodeAttributesString(node));
            sb.append("\n");
            sb.append(m_controller.getGraphNodeFollowablePtrsString(node));
        }

        return sb.toString();
    }

    /** Show the dialog and return when the user dismisses it. */
    public static boolean exec(EntityController controller)
    {
        GraphNodeDialog dialog = new GraphNodeDialog(controller);
        return dialog.exec();
    }
}


// EOF
