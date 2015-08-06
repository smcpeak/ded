// ModalDialog.java
// See toplevel license.txt for copyright and license terms.

package util.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;

import java.util.EnumSet;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/** Base class with common functionality for modal dialogs. */
public class ModalDialog extends JDialog {
    // --------------- constants ---------------
    private static final long serialVersionUID = -5968176808231360009L;

    /** Size of outer margin in a dialog box. */
    public static final int OUTER_MARGIN = 11;

    /** Space between controls. */
    public static final int CONTROL_PADDING = 5;

    // -------------- public data --------------
    /** Initially false, this is set to true if the dialog is closed
      * by pressing the OK button. */
    public boolean okWasPressed;

    // ---------------- methods ----------------
    /** Create a new dialog.  'documentParent' is a Component that
      * originated the request; the top-level window that contains
      * it will be blocked from interaction until this dialog closes. */
    public ModalDialog(Component documentParent, String title)
    {
        super(documentParent!=null? SwingUtilities.getWindowAncestor(documentParent) : null,
              title, Dialog.ModalityType.DOCUMENT_MODAL);

        this.okWasPressed = false;

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        installEscapeCloseOperation(this);
    }

    /** Run the dialog, blocking until it is dismissed.  Returns true
      * if the user pressed OK, false if Cancel. */
    public boolean exec()
    {
        // This blocks until the dialog is dismissed.
        this.setVisible(true);

        return this.okWasPressed;
    }

    /** Print component sizes for debugging. */
    public static void printSizes(String label, Component c)
    {
        System.out.println(label+" preferred size: "+c.getPreferredSize());
        System.out.println(label+" max size: "+c.getMaximumSize());
        System.out.println(label+" min size: "+c.getMinimumSize());
        System.out.println(label+" cur size: "+c.getSize());
    }

    /** Create a new JButton with the specified label, mnemonic
      * (KeyEvent.VK_XXX code, or 0 for none), and action listener. */
    public static JButton makeButton(String label, int mnemonic, ActionListener listener)
    {
        JButton button = new JButton(label);
        button.setMnemonic(mnemonic);
        button.addActionListener(listener);
        return button;
    }

    /** Create a Cancel button and set its action to close the dialog. */
    public JButton makeCancelButton()
    {
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new SwingUtil.WindowCloseAction(this));
        return cancelButton;
    }

    /** Create an OK button and set its action to close the dialog,
      * indicating that changes should be preserved. */
    public JButton makeOKButton()
    {
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ModalDialog.this.okPressed();
            }
        });
        this.getRootPane().setDefaultButton(okButton);
        return okButton;
    }

    /** React to the OK button being pressed.  The base class
      * implementation remembers that it was pressed and closes the dialog.
      * Derived classes should copy data from controls into the object that
      * the dialog is meant to edit, then call super.okPressed().
      *
      * If some inputs need to be validated, do so before calling
      * super.okPressed(); and if validation fails, do not call it at
      * all, so the dialog will remain open. */
    public void okPressed()
    {
        this.okWasPressed = true;
        SwingUtil.closeWindow(this);
    }

    /** Make a vertical layout box. */
    public static Box makeVBox(Container parent)
    {
        Box b = Box.createVerticalBox();
        parent.add(b);
        return b;
    }

    /** Make a horizontal layout box. */
    public static Box makeHBox(Container parent)
    {
        Box b = Box.createHorizontalBox();
        parent.add(b);
        return b;
    }

    /** Make a vertical layout box with the given margin. */
    public static Box makeMarginVBox(Container parent, int margin)
    {
        Box hb = ModalDialog.makeHBox(parent);
        hb.add(Box.createHorizontalStrut(margin));

        Box vb = ModalDialog.makeVBox(hb);
        vb.add(Box.createVerticalStrut(margin));

        Box ret = ModalDialog.makeVBox(vb);

        vb.add(Box.createVerticalStrut(margin));
        hb.add(Box.createHorizontalStrut(margin));

        return ret;
    }

    /** Create a line edit control and associated label. */
    public static JTextField makeLineEdit(Container parent, String label, char mnemonic,
                                          String initialValue)
    {
        Box hbox = ModalDialog.makeHBox(parent);
        JLabel labelControl = new JLabel(label+":");
        labelControl.setDisplayedMnemonic(mnemonic);
        hbox.add(labelControl);

        hbox.add(Box.createHorizontalStrut(CONTROL_PADDING));

        final JTextField ret = new JTextField(initialValue);
        hbox.add(ret);
        labelControl.setLabelFor(ret);

        // Arrange to select all the text when the box receives focus.
        // http://stackoverflow.com/questions/1178312/how-to-select-all-text-in-a-jformattedtextfield-when-it-gets-focus
        ret.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        ret.selectAll();
                    }
                });
            }

            // This refinement removes focus when we leave.  The Swing
            // text controls draw the selected text with the selection
            // background even when the control does not have the focus,
            // which is different from how Qt does it and looks dumb
            // since tabbing from text control to text control then
            // works differently from tabbing from text control to some
            // other kind of control (like a dropdown or button).
            @Override
            public void focusLost(FocusEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        ret.select(0,0);
                    }
                });
            }
        });

        disallowVertStretch(hbox);

        return ret;
    }

    /** Same as 'makeLineEdit', except also add a help button that will
      * pop up a help dialog on top of 'parentWindow' with 'helpText'. */
    public static JTextField makeLineEditWithHelp(
        Container parentBox,
        String label,
        char mnemonic,
        String initialValue,
        Component parentWindow,
        String helpText)
    {
        Box hb = ModalDialog.makeHBox(parentBox);
        JTextField textField = ModalDialog.makeLineEdit(hb,
            label, mnemonic, initialValue);
        hb.add(Box.createHorizontalStrut(ModalDialog.CONTROL_PADDING));
        hb.add(ModalDialog.makeHelpButton(parentWindow, label, helpText));
        return textField;
    }

    /** Set min/max height to preferred height in order to disallow
      * vertical stretching. */
    public static void disallowVertStretch(Component c)
    {
        Dimension pref = c.getPreferredSize();
        if (pref == null) {
            // Coverity analysis claims this might return null.  The
            // documentation is not clear.  I guess if it does return
            // null I'll just skip trying to disable vertical stretch.
            return;
        }

        Dimension max = c.getMaximumSize();
        Dimension min = c.getMinimumSize();
        max.height = pref.height;
        min.height = pref.height;
        c.setMaximumSize(max);
        c.setMinimumSize(min);
    }

    /** Arrange to close a dialog when Escape is pressed.
      *
      * Based on code from:
      *  http://stackoverflow.com/questions/642925/swing-how-do-i-close-a-dialog-when-the-esc-key-is-pressed
      */
    public static void installEscapeCloseOperation(final JDialog dialog)
    {
        JRootPane rootPane = dialog.getRootPane();
        rootPane.registerKeyboardAction(
            new SwingUtil.WindowCloseAction(dialog),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** Build a dropdown control for choosing among elements of an
      * enumeration.
      *
      * 'containingBox' is the Box into which we will put an hbox
      * to contain the combo box and its label.
      *
      * 'label' labels the combo box, and should end in a colon (":").
      * 'labelMnemonic' is its keyboard shortcut.
      *
      * 'elementType' is the enumeration type.
      *
      * 'initialValue' what to initially set the box to. */
    public static <E extends Enum<E>> JComboBox<E> makeEnumChooser(
        Box containingBox,
        String label,
        char labelMnemonic,
        Class<E> elementType,
        E initialValue)
    {
        // Put all enumerators into a vector.
        EnumSet<E> eSet = EnumSet.allOf(elementType);
        Vector<E> eVector = new Vector<E>();
        eVector.addAll(eSet);

        return makeVectorChooser(containingBox, label, labelMnemonic,
                                 eVector, initialValue);
    }

    /** Build a dropdown control for choosing among elements of a
      * vector.
      *
      * 'containingBox' is the Box into which we will put an hbox
      * to contain the combo box and its label.
      *
      * 'label' labels the combo box, and should end in a colon (":").
      * 'labelMnemonic' is its keyboard shortcut.
      *
      * 'elements' is the vector of choices.
      *
      * 'initialValue' what to initially set the box to. */
    public static <E> JComboBox<E> makeVectorChooser(
        Box containingBox,
        String label,
        char labelMnemonic,
        Vector<E> elements,
        E initialValue)
    {
        Box hbox = ModalDialog.makeHBox(containingBox);

        JLabel lbl = new JLabel(label+":");
        lbl.setDisplayedMnemonic(labelMnemonic);
        hbox.add(lbl);
        hbox.add(Box.createHorizontalStrut(ModalDialog.CONTROL_PADDING));

        // Build the combo box.
        JComboBox<E> comboBox = new JComboBox<E>(elements);
        comboBox.setSelectedItem(initialValue);
        lbl.setLabelFor(comboBox);
        hbox.add(comboBox);

        ModalDialog.disallowVertStretch(hbox);

        return comboBox;
    }

    /** Create a button that, when pressed, shows a help dialog with
      * 'helpText' in it.  The help dialog caption will be
      * "Help: $labelText". */
    public static JButton makeHelpButton(
        final Component parentWindow,
        final String labelText,
        final String helpText)
    {
        // This label causes the help button to be a bit wider than
        // I would like, but I tried setting the size explicitly and
        // it did not work (was ignored).
        JButton helpButton = new JButton("?");

        helpButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                SwingUtil.informationMessageBox(parentWindow,
                    "Help: "+labelText, helpText);
            }
        });
        return helpButton;
    }

    /** Create Cancel and OK buttons and add them to 'containingVBox'. */
    public void createCancelAndOkButtons(Box containingVBox)
    {
        Box btnBox = ModalDialog.makeHBox(containingVBox);

        // Buttons will be on the right side of the dialog.
        btnBox.add(Box.createHorizontalGlue());

        JButton cancelButton = this.makeCancelButton();
        btnBox.add(cancelButton);

        btnBox.add(Box.createHorizontalStrut(ModalDialog.CONTROL_PADDING));

        JButton okButton = this.makeOKButton();
        btnBox.add(okButton);

        ModalDialog.disallowVertStretch(btnBox);
    }

    /** Do the usual final actions to create the dialog: create the
      * Cancel and OK buttons, pack the dialog, and set the location
      * relative to the parent. */
    public void finishBuildingDialog(Box containingVBox)
    {
        this.createCancelAndOkButtons(containingVBox);
        this.pack();
        this.setLocationRelativeTo(this.getParent());
    }
}

// EOF
