// MenuAction.java
// See toplevel license.txt for copyright and license terms.

package util.swing;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;

/** An Action sutable for use in a menu. */
public abstract class MenuAction extends AbstractAction {
    private static final long serialVersionUID = -7514077079980788428L;

    /** Create a menu Action with specified title and mnemonic.
      * The latter should be a KeyEvent.VK_XXX constant. */
    public MenuAction(String title, int mnemonicKeycode)
    {
        super(title);
        this.putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnemonicKeycode));
    }

    /** Create a menu Action with specified title, mnemonic and acclerator.
      * Both key codes should be KeyEvent.VK_XXX constants.
      * 'accelModifiers' should be 0 or some bitwise combination of
      * ActionEvent.XXX_MASK constants. */
    public MenuAction(String title, int mnemonicKeyCode, int accelKeyCode, int accelModifiers)
    {
        super(title);
        this.putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnemonicKeyCode));
        this.putValue(Action.ACCELERATOR_KEY,
            KeyStroke.getKeyStroke(accelKeyCode, accelModifiers));
    }
}

// EOF
