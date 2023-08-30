// MenuDelegate.java
// See toplevel license.txt for copyright and license terms.

package util.swing;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/** Delegate to either JMenu or JPopupMenu, thereby providing a sort of
  * superclass of them.  (Annoyingly, they do not share a useful
  * superclass already.) */
public class MenuDelegate {
    // ---- private data ----
    // The JMenu, if that is what we have.  Otherwise null.
    private JMenu m_jmenu;

    // The JPopupMenu, if that is what we have.  Otherwise null.
    private JPopupMenu m_jpopup;

    // ---- public methods ----
    public MenuDelegate(JMenu jmenu)
    {
        this.m_jmenu = jmenu;
        this.m_jpopup = null;
    }

    public MenuDelegate(JPopupMenu jpopup)
    {
        this.m_jmenu = null;
        this.m_jpopup = jpopup;
    }

    public JMenuItem add(Action a)
    {
        if (m_jmenu != null) {
            return m_jmenu.add(a);
        }
        else {
            return m_jpopup.add(a);
        }
    }

    public JMenuItem add(JMenuItem menuItem)
    {
        if (m_jmenu != null) {
            return m_jmenu.add(menuItem);
        }
        else {
            return m_jpopup.add(menuItem);
        }
    }
}

// EOF
