// UndoHistoryWindow.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import util.swing.ModalDialog;
import util.swing.SwingUtil;

public class UndoHistoryWindow extends JFrame {
    // ---- constants ----
    private static final long serialVersionUID = -3494202437710226667L;

    // ---- data ----
    /** Diagram editor whose undo history we show. */
    public DiagramController diagramController;

    /** For now at least, show the debug dump of the history in a
      * text area. */
    public JTextArea historyTextArea;

    /** Button to show/change the history size limit. */
    public JButton historySizeLimitButton;

    // ---- methods ----
    public UndoHistoryWindow(DiagramController dc)
    {
        super("Undo History");
        this.diagramController = dc;
        this.setSize(450,600);

        Container content = this.getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        Box vb = Box.createVerticalBox();
        content.add(vb);

        this.historyTextArea = new JTextArea();
        this.historyTextArea.setEditable(false);
        vb.add(new JScrollPane(this.historyTextArea));

        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        // Button panel along the bottom.
        Box buttons = Box.createHorizontalBox();
        vb.add(buttons);

        buttons.add(Box.createRigidArea(new Dimension(ModalDialog.CONTROL_PADDING, 0)));

        this.historySizeLimitButton = new JButton("<placeholder>");
        this.setHistorySizeLimitButtonLabel();
        this.historySizeLimitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UndoHistoryWindow.this.changeHistorySizeLimit();
            }
        });
        buttons.add(this.historySizeLimitButton);

        buttons.add(Box.createHorizontalGlue());
        buttons.add(Box.createRigidArea(new Dimension(ModalDialog.CONTROL_PADDING, 0)));

        JButton helpButton = new JButton("Help");
        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UndoHistoryWindow.this.displayHelp();
            }
        });
        buttons.add(helpButton);
        buttons.add(Box.createRigidArea(new Dimension(ModalDialog.CONTROL_PADDING, 0)));

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UndoHistoryWindow.this.setVisible(false);
            }
        });
        buttons.add(closeButton);
        buttons.add(Box.createRigidArea(new Dimension(ModalDialog.CONTROL_PADDING, 0)));

        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        this.updateHistory();
    }

    /** Update the history display due to a change to that history. */
    public void updateHistory()
    {
        this.historyTextArea.setText(
            this.diagramController.getUndoHistory().dumpHistoryStructure());
    }

    /** Show the help dialog for this window. */
    public void displayHelp()
    {
        SwingUtil.informationMessageBox(this, "Undo History Help",
            "This window shows the recorded history of actions performed "+
            "during this editing session. The top action is the oldest.  "+
            "The asterisk (*) marks the current state. The Undo and Redo "+
            "menu commands in the editor window move the current state up "+
            "and down, respectively.\n\n"+

            "If you undo some actions, then make a change, the prior redo "+
            "is *not* discarded. Instead, it is available as an \"alternate\" "+
            "redo history. You can undo back to that point, then use the "+
            "\"Redo alternate\" menu command to replay the other history; "+
            "that makes the just-undone history into an alternate.\n\n"+

            "There is a configurable limit on the number of states on the path "+
            "from the current state "+
            "to the oldest ancestor state.  When you make a change that would "+
            "add a new leaf exceeding the limit, the oldest ancestor (and all "+
            "of its alternate histories) is discarded.\n\n"+

            "NOTE: Currently, the limit is not persisted, so if you start a "+
            "new 'ded' process, the limit will return to its default.");
    }

    /** Show the dialog that lets the user change the history size limit. */
    public void changeHistorySizeLimit()
    {
        int curLimit = this.diagramController.getUndoHistoryLimit();
        Long newLimit = SwingUtil.showIntegerInputDialog(
            this /*parent*/,
            "Enter new history limit (0 means no limit)",
            Long.valueOf(curLimit),
            0 /*minValue*/,
            Integer.MAX_VALUE /*maxValue*/);
        if (newLimit != null) {
            this.diagramController.setUndoHistoryLimit((int)(newLimit.longValue()));
            this.setHistorySizeLimitButtonLabel();
        }
    }

    /** Set the label on the history size limit button to indicate the
      * current limit value. */
    private void setHistorySizeLimitButtonLabel()
    {
        int limit = this.diagramController.getUndoHistoryLimit();
        String label = "History size limit: "+limit;
        if (limit == 0) {
            label += " (none)";
        }
        this.historySizeLimitButton.setText(label);
    }
}

// EOF
