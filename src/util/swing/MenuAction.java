// MenuAction.java

package util.swing;

import javax.swing.AbstractAction;
import javax.swing.Action;

/** An Action sutable for use in a menu. */
public abstract class MenuAction extends AbstractAction {
    private static final long serialVersionUID = -7514077079980788428L;

    public MenuAction(String title, int mnemonicKeycode)
    {
        super(title);
        this.putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnemonicKeycode));
    }
}

// EOF
