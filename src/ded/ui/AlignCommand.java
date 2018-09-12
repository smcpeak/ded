// AlignCommand.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.event.KeyEvent;

/** A command to issue to a set of entities to align them in some way. */
public enum AlignCommand {
    AC_LEFT_MOVE    (EntityEdge.EE_LEFT,   false, "Align left edges by moving", KeyEvent.VK_L),
    AC_LEFT_RESIZE  (EntityEdge.EE_LEFT,   true,  "Align left edges by resizing", 0),
    AC_RIGHT_MOVE   (EntityEdge.EE_RIGHT,  false, "Align right edges by moving", 0),
    AC_RIGHT_RESIZE (EntityEdge.EE_RIGHT,  true,  "Align right edges by resizing", KeyEvent.VK_R),
    AC_TOP_MOVE     (EntityEdge.EE_TOP,    false, "Align top edges by moving", KeyEvent.VK_T),
    AC_TOP_RESIZE   (EntityEdge.EE_TOP,    true,  "Align top edges by resizing", 0),
    AC_BOTTOM_MOVE  (EntityEdge.EE_BOTTOM, false, "Align bottom edges by moving", 0),
    AC_BOTTOM_RESIZE(EntityEdge.EE_BOTTOM, true,  "Align bottom edges by resizing", KeyEvent.VK_B);

    /** Which edge is affected. */
    EntityEdge ee;

    /** True to resize, false to move. */
    boolean resize;

    /** The label of this command in the menu. */
    public String label;

    /** The mnemonic key as a KeyEvent.VK_XXX constant, or 0 for none. */
    public int mnemonic;

    AlignCommand(EntityEdge ee, boolean resize, String label, int mnemonic)
    {
        this.ee = ee;
        this.resize = resize;
        this.label = label;
        this.mnemonic = mnemonic;
    }
}

// EOF
