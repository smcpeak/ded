// SwingUtil.java
// See toplevel license.txt for copyright and license terms.

package util.swing;

import java.awt.Component;
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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


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

        // Go to 'p', then add a/2 to get to the baseline.
        // I ignore the descent because it looks better to center without
        // regard to descenders.
        int baseY = p.y + (int)(lm.getAscent()/2);
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

    /** Create a JOptionPane instance that word-wraps its message. */
    public static JOptionPane makeWordWrapJOptionPane()
    {
        // The basic problem is described in this bug report:
        //
        //   http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4104906
        //
        // The workaround there requires adding a scrollbar to the
        // message, which I do not want to do.

        // I tried these solutions, but they do not work (anymore?):
        //
        //   http://stackoverflow.com/questions/4330076/joptionpane-showmessagedialog-truncates-jtextarea-message
        //   http://www.coderanch.com/t/339970/GUI/java/wrap-large-message-JOptionPane-showConfirmDialog
        //
        // Most other solutions involve manually inserting newlines.

        // Thankfully, this one actually does work:
        //
        //   http://www.jroller.com/Fester/entry/joptionpane_with_word_wrapping

        @SuppressWarnings("serial")
        JOptionPane pane = new JOptionPane() {
            @Override
            public int getMaxCharactersPerLineCount()
            {
                return 80;
            }
        };
        return pane;
    }

    /** Show a message dialog box with message word wrapping, specified
      * title and type.  Type must be one of the JOptionPane *_MESSAGE
      * constants.
      *
      * @see JOptionPane.setMessageType */
    public static void messageBox(Component parent, String title,
                                  int messageType, String message)
    {
        JOptionPane pane = makeWordWrapJOptionPane();
        pane.setMessage(message);
        pane.setMessageType(messageType);

        JDialog dialog = pane.createDialog(parent, title);
        dialog.setVisible(true);
    }

    /** Show an error message dialog box with message word wrapping. */
    public static void errorMessageBox(Component parent, String message)
    {
        messageBox(parent, "Error", JOptionPane.ERROR_MESSAGE, message);
    }

    /** Show a warning message dialog box with message word wrapping. */
    public static void warningMessageBox(Component parent, String message)
    {
        messageBox(parent, "Warning", JOptionPane.WARNING_MESSAGE, message);
    }

    /** Show an information message dialog box with message word wrapping. */
    public static void informationMessageBox(Component parent,
        String caption, String message)
    {
        messageBox(parent, caption, JOptionPane.INFORMATION_MESSAGE, message);
    }

    /** Show a confirmation message box with line wrapped message.
      *
      * 'optionType' is one of the JOptionPane.XXX_OPTION combination
      * constants, and the return value is one of the single-value
      * constants. */
    public static int confirmationBox(
        Component parent,
        String message,
        String title,
        int optionType)
    {
        JOptionPane pane = makeWordWrapJOptionPane();
        pane.setMessage(message);
        pane.setMessageType(JOptionPane.QUESTION_MESSAGE);
        pane.setOptionType(optionType);

        JDialog dialog = pane.createDialog(parent, title);
        dialog.setVisible(true);

        Object result = pane.getValue();
        if (result == null || !(result instanceof Integer)) {
            return JOptionPane.CLOSED_OPTION;
        }
        else {
            return ((Integer)result).intValue();
        }
    }

    /** Show a message box with a long multi-line piece of text,
      * like a log file. */
    public static void logFileMessageBox(
        Component parent,
        String logMessage,
        String title)
    {
        // A scrollable text area holds the text.
        JTextArea textArea = new JTextArea(20, 80);
        textArea.setText(logMessage);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        // Show it.
        JOptionPane.showMessageDialog(parent, scrollPane, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /** Assign names to the implicit panes of a JFrame so that when
      * the component hierarchy is printed, they are intelligible. */
    public static void assignJFrameImplicitPaneNames(JFrame frame)
    {
        JRootPane root = frame.getRootPane();
        root.setName("root");
        root.getLayeredPane().setName("layeredPane");
        root.getGlassPane().setName("glassPane");
    }

    /** Prompt the user for an integer.
      *
      * @param parent         Parent component.
      * @param prompt         Prompt string saying what input we want.
      * @param initialValue   If non-null, the current value.
      * @param minValue       Minimum allowable value.
      * @param maxValue       Maximum allowable value.
      * @return               The new value, or null if user canceled.
      */
    public static Long showIntegerInputDialog(
        Component parent,
        String prompt,
        Long initialValue,
        long minValue,
        long maxValue)
    {
        while (true) {
            String result = JOptionPane.showInputDialog(parent, prompt, initialValue);
            if (result == null) {
                return null;
            }

            try {
                long ret = Long.parseLong(result);
                if (minValue <= ret && ret <= maxValue) {
                    return Long.valueOf(ret);
                }
                else {
                    JOptionPane.showMessageDialog(parent,
                        "The value must be at least " + minValue +
                        " and at most " + maxValue +
                        "; the chosen value of " + ret +
                        " is outside this range.");
                }
            }
            catch (NumberFormatException e) {
                // The exception object does not carry any useful information
                // besides its type.
                JOptionPane.showMessageDialog(parent,
                    "Malformed integer: \"" + result + "\".");
            }
        }
    }
}

// EOF
