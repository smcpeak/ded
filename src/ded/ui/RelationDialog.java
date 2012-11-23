// RelationDialog.java

package ded.ui;

import java.awt.Component;

import javax.swing.JOptionPane;

import ded.model.Relation;

/** Dialog box to allow editing a Relation. */
public class RelationDialog {
    /** Launch the dialog, blocking until the dialog is dismissed.
      * If the user presses OK, 'relation' will be updated and true
      * returned.  Otherwise, false is returned and 'relation' is
      * not modified. */ 
    public static boolean exec(Component parent, Relation relation)
    {
        // TODO: code this
        JOptionPane.showMessageDialog(parent, "TODO: RelationDialog.exec");
        return false;
    }
}

// EOF
