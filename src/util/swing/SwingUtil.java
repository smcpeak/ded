// SwingUtil.java

package util.swing;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.font.LineMetrics;

import javax.swing.AbstractAction;


/** Miscellaneous Swing-related utililities. */
public class SwingUtil {
    /** Return true if the Control button was held when 'e' was generated. */
    public static boolean controlPressed(InputEvent e)
    {
        return (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
    }

    /** Return true if the Alt button was held when 'e' was generated. */
    public static boolean altPressed(InputEvent e)
    {
        return (e.getModifiersEx() & InputEvent.ALT_DOWN_MASK) != 0;
    }

    /** Return true if the Shift button was held when 'e' was generated. */
    public static boolean shiftPressed(InputEvent e)
    {
        return (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;
    }

    /** Return true if no keyboard modifiers or mouse buttons were held
      * when 'e' was generated. */
    public static boolean noModifiers(KeyEvent e)
    {
        return e.getModifiersEx() == 0;
    }
    
    /** Send a message to close a window.
      *
      * I do not really understand whether or why this is better than
      * simply calling dispose(), but I infer from code snippets that
      * it may be. */
    public static void closeWindow(Window window)
    {
        window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
    }
    
    /** An action to close a window or dialog. */
    public static class WindowCloseAction extends AbstractAction {
        private static final long serialVersionUID = -1615998248180527506L;
        
        /** The window that will be closed. */
        public Window window;
        
        public WindowCloseAction(Window window)
        {
            this.window = window;
        }

        @Override
        public void actionPerformed(ActionEvent event)
        {
            closeWindow(this.window);
        }
    }
    
    /** Draw 'str' centered at 'p'. */
    public static void drawCenteredText(Graphics g, Point p, String str)
    {
        FontMetrics fm = g.getFontMetrics();
        LineMetrics lm = fm.getLineMetrics(str, g);

        // Go to 'p', then add (a+d)/2 to get to the bottom
        // of the text, then subtract d; then simplify algebraically.
        int baseY = p.y + (int)((lm.getAscent() - lm.getDescent())/2);
        int baseX = p.x - fm.stringWidth(str)/2;
        
        g.drawString(str, baseX, baseY);
    }

    /** Draw 'str' at the given location, but process newlines by moving
      * to a new line. */
    public static void drawTextWithNewlines(Graphics g, String str, int x, int y)
    {
        String lines[] = str.split("\n");
        int lineHeight = g.getFontMetrics().getHeight();
        for (String s : lines) {
            g.drawString(s, x, y);
            y += lineHeight;
        }
    }
}

// EOF
