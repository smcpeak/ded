// EntityGeometryAttribute.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.event.KeyEvent;

/** One of x/y/w/h, this identifies a geometry attribute of an entity.
  * This is used, for example, to allow the right-click menu code to
  * abstract the particular attribute to modify. */
public enum EntityGeometryAttribute {
    EGA_LEFT  ("left",   "Left",   KeyEvent.VK_L),
    EGA_TOP   ("top",    "Top",    KeyEvent.VK_T),
    EGA_WIDTH ("width",  "Width",  KeyEvent.VK_W),
    EGA_HEIGHT("height", "Height", KeyEvent.VK_H);

    /** Human-readable name of this attribute for use in constructing
      * sentences. */
    public String m_name;

    /** Stand-alone name for a menu item that identifies this attribute. */
    public String m_label;

    /** The mnemonic key as a KeyEvent.VK_XXX constant. */
    public int m_mnemonic;

    EntityGeometryAttribute(String name, String label, int mnemonic)
    {
        m_name = name;
        m_label = label;
        m_mnemonic = mnemonic;
    }
}

// EOF
