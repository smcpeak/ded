package ded.ui;

import java.awt.event.KeyEvent;

/** A line dash style is one of a fixed set of dash structures that
  * can be chosen via the right-click menu on a relation.  Other
  * structures can be specified directly in the relation edit dialog. */
public enum LineDashStyle {
    LDS_SOLID      ("Solid", KeyEvent.VK_S, null),
    LDS_DASHED     ("Dashed", KeyEvent.VK_D, "5 2"),
    LDS_DOTTED     ("Dotted", KeyEvent.VK_T, "1 2"),
    LDS_DASH_DOT   ("Dash-dot", KeyEvent.VK_A, "5 2 1 2");

    /** Name for the style as shown in the menu. */
    public String name;

    /** Mnemonic key code for the style's menu item, from among KeyEvent.VK_XXX. */
    public int mnemonicKey;

    /** Dash structure specification as a space-separated sequence of integers.
      * The solid line has a null string. */
    public String dashStructureString;

    LineDashStyle(String n, int mk, String dss) {
        this.name = n;
        this.mnemonicKey = mk;
        this.dashStructureString = dss;
    }
}
