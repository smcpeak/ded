// EntityDialog.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import java.util.EnumSet;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import util.swing.ModalDialog;

import ded.model.Diagram;
import ded.model.Entity;
import ded.model.EntityShape;
import ded.model.ImageFillStyle;
import ded.model.ShapeFlag;
import ded.model.TextAlign;

/** Dialog box to edit an Entity. */
public class EntityDialog extends ModalDialog
    implements ItemListener
{
    private static final long serialVersionUID = 1455207901388264571L;

    // -------------- private data --------------
    /** Entity being edited. */
    private Entity entity;

    /** Working copy of the shape flags.  This is what the shape flags
      * dialog edits.  We only copy it back into 'entity' if OK is
      * chosen on the outer entity dialog. */
    private EnumSet<ShapeFlag> shapeFlagsWorkingCopy;

    /** The shape that the working flags is based on.  At certain points,
      * we need to add default flags, and if the shape has changed, we
      * need to know what it changed from. */
    private EntityShape workingFlagsBaseShape;

    // Controls.
    private JTextField nameText;
    private JComboBox<TextAlign> nameAlignChooser;
    private JTextArea attributeText;
    private JComboBox<EntityShape> shapeChooser;
    private JButton shapeFlagsButton;
    private JComboBox<String> fillColorChooser;
    private JComboBox<String> lineColorChooser;
    private JComboBox<String> textColorChooser;
    private JTextField xText, yText, wText, hText;
    private JLabel paramsLabel;
    private JTextField pText, qText;
    private JTextField anchorNameText;
    private JTextField imageFileNameText;
    private JComboBox<ImageFillStyle> imageFillStyleChooser;

    // -------------- methods ---------------
    public EntityDialog(Component documentParent, Diagram diagram, Entity entity)
    {
        super(documentParent, "Edit Entity");

        this.entity = entity;
        this.shapeFlagsWorkingCopy = this.entity.shapeFlags.clone();
        this.workingFlagsBaseShape = this.entity.shape;

        // The outer vbox has the name, the column container, and the ok/cancel row:
        Box vb = ModalDialog.makeMarginVBox(this, ModalDialog.OUTER_MARGIN);

        // Name row
        {
            Box nameRowHb = ModalDialog.makeHBox(vb);

            this.nameText = ModalDialog.makeLineEdit(nameRowHb, "Name", 'n', this.entity.name);

            nameRowHb.add(Box.createHorizontalStrut(ModalDialog.CONTROL_PADDING*2));

            this.nameAlignChooser = ModalDialog.makeEnumChooser(
                nameRowHb,
                "Align",
                'g',
                TextAlign.class,
                this.entity.nameAlign);
        }
        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        // VBox for first column, which just contains the attributes.
        Box leftColumn = Box.createVerticalBox();
        makeLeftColumn(leftColumn);

        // Spacing separating the columns.
        //columnContainer.add(Box.createHorizontalStrut(ModalDialog.CONTROL_PADDING * 2));

        // Now a new vbox for the second column, containing most of the
        // remaining controls.
        Box rightColumn = Box.createVerticalBox();
        createRightColumn(diagram, rightColumn);

        // Wrap the columns in hboxes so I can insert padding.
        Box lhb = Box.createHorizontalBox();
        lhb.add(leftColumn);
        lhb.add(Box.createHorizontalStrut(ModalDialog.CONTROL_PADDING));
        Box rhb = Box.createHorizontalBox();
        rhb.add(Box.createHorizontalStrut(ModalDialog.CONTROL_PADDING*2));
        rhb.add(rightColumn);

        // Put both columns into a split pane in an hbox.
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                          lhb, rhb);
        split.setResizeWeight(1.0);
        Box hb = ModalDialog.makeHBox(vb);
        hb.add(split);

        // Finish up with the controls at the bottom.
        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
        this.updateControls();
        this.finishBuildingDialog(vb);
    }

    /** Create the right column controls, which is most of them in
      * the dialog, and put them into 'vb'. */
    public void createRightColumn(Diagram diagram, Box vb)
    {
        // shape
        {
            Box shapeBox = ModalDialog.makeHBox(vb);

            this.shapeChooser = ModalDialog.makeEnumChooser(
                shapeBox,
                "Shape",
                's',
                EntityShape.class,
                this.entity.shape);
            this.shapeChooser.addItemListener(this);

            shapeBox.add(Box.createHorizontalStrut(ModalDialog.CONTROL_PADDING));

            this.shapeFlagsButton = new JButton("Flags");
            this.shapeFlagsButton.setMnemonic(KeyEvent.VK_G);
            this.shapeFlagsButton.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    EntityDialog.this.openShapeFlagsDialog();
                }
            });
            shapeBox.add(this.shapeFlagsButton);

            vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
        }

        // colors
        this.fillColorChooser = makeColorChooser(diagram, vb, this.entity.fillColor, "Fill color", 'f');
        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
        this.lineColorChooser = makeColorChooser(diagram, vb, this.entity.lineColor, "Line color", 'l');
        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
        this.textColorChooser = makeColorChooser(diagram, vb, this.entity.textColor, "Text color", 't');
        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        // x, y
        {
            Box locBox = ModalDialog.makeHBox(vb);
            this.xText = ModalDialog.makeLineEdit(locBox, "X", 'x', String.valueOf(this.entity.loc.x));
            locBox.add(Box.createHorizontalStrut(ModalDialog.CONTROL_PADDING));
            this.yText = ModalDialog.makeLineEdit(locBox, "Y", 'y', String.valueOf(this.entity.loc.y));
            vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
            ModalDialog.disallowVertStretch(locBox);
        }

        // w, h
        {
            Box sizeBox = ModalDialog.makeHBox(vb);
            this.wText = ModalDialog.makeLineEdit(sizeBox, "W", 'w', String.valueOf(this.entity.size.width));
            sizeBox.add(Box.createHorizontalStrut(ModalDialog.CONTROL_PADDING));
            this.hText = ModalDialog.makeLineEdit(sizeBox, "H", 'h', String.valueOf(this.entity.size.height));
            vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
            ModalDialog.disallowVertStretch(sizeBox);
        }

        // paramsLabel
        {
            Box hb = ModalDialog.makeHBox(vb);
            hb.add(this.paramsLabel = new JLabel());    // Text set later.
            hb.add(Box.createHorizontalGlue());
        }

        // shapeParams
        {
            String p="", q="";
            int[] params = this.entity.shapeParams;
            if (params != null) {
                if (params.length >= 1) {
                    p = String.valueOf(params[0]);
                }
                if (params.length >= 2) {
                    q = String.valueOf(params[1]);
                }
            }

            Box hb = ModalDialog.makeHBox(vb);
            this.pText = ModalDialog.makeLineEdit(hb, "P", 'p', p);
            hb.add(Box.createHorizontalStrut(ModalDialog.CONTROL_PADDING));
            this.qText = ModalDialog.makeLineEdit(hb, "Q", 'q', q);
            vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
            ModalDialog.disallowVertStretch(hb);
        }

        // HTML anchor name
        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
        this.anchorNameText = ModalDialog.makeLineEditWithHelp(vb,
            "HTML anchor name", 'c', this.entity.anchorName, this,

            "The anchor name is used by the 'insert-ded-image-map' "+
            "script, which inserts an image map into an HTML "+
            "document.  The script is in the root of the 'ded' "+
            "repo, and has comments inside explaining how to "+
            "use it.  The anchor name should *not* contain an "+
            "initial '#' character.\n\n"+

            "Often, one wants the anchor name to be the same as the "+
            "entity name.  There is a right-click menu option on "+
            "entities that will set the anchor name to the entity name "+
            "for any set of selected entities.");

        // image file name
        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
        this.imageFileNameText = ModalDialog.makeLineEditWithHelp(vb,
            "Image file name", 'i', this.entity.imageFileName, this,

            "Name of an image file residing in the same directory as "+
            "this diagram's .ded file.  The image will be drawn inside "+
            "the entity box, possibly tiled, etc., according to the "+
            "image fill style dropdown.");

        // image fill style
        {
            vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
            this.imageFillStyleChooser = ModalDialog.makeEnumChooser(
                vb,
                "Image fill style",
                'm',
                ImageFillStyle.class,
                this.entity.imageFillStyle);
            this.imageFillStyleChooser.addItemListener(this);
        }
    }

    /** Create the controls for the left column, which is just the
      * edit box for the atributes, and put it into 'vb'. */
    public void makeLeftColumn(Box vb)
    {
        Box attrBox = ModalDialog.makeHBox(vb);

        JLabel lbl = new JLabel("Attributes:");
        lbl.setDisplayedMnemonic('a');
        attrBox.add(lbl);
        attrBox.add(Box.createHorizontalGlue());

        this.attributeText = new JTextArea(this.entity.attributes);
        lbl.setLabelFor(this.attributeText);

        // Tab and shift-tab should move the focus, not insert characters.
        // http://stackoverflow.com/questions/5042429/how-can-i-modify-the-behavior-of-the-tab-key-in-a-jtextarea
        this.attributeText.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        this.attributeText.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

        JScrollPane scroll = new JScrollPane(this.attributeText);

        // This is what establishes the initial size of the dialog, as the
        // scroll pane is the main resizable thing.
        scroll.setPreferredSize(new Dimension(300,150));

        vb.add(scroll);
        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));
    }

    /** Make a color chooser dropdown, add it to 'vb', and return it.
      * 'diagram' holds the list of all colors.  'currentColor' is
      * the initially selected value. */
    public static JComboBox<String> makeColorChooser(
            final Diagram diagram,
            final Box vb,
            String currentColor,
            String label,
            char mnemonic)
    {
        Box hbox = ModalDialog.makeHBox(vb);

        Vector<String> colors = new Vector<String>();

        // If the current entity color is not in the
        // diagram colors, add it to the vector so that it is
        // in the dropdown.
        if (!diagram.namedColors.containsKey(currentColor)) {
            colors.add(currentColor);
        }

        // Add the diagram colors.
        for (String c : diagram.namedColors.keySet()) {
            colors.add(c);
        }

        final JComboBox<String> ret = ModalDialog.makeVectorChooser(
            hbox,
            label,
            mnemonic,
            colors,
            currentColor);
        ret.setEditable(true);

        hbox.add(Box.createHorizontalStrut(ModalDialog.CONTROL_PADDING));

        JButton chooserButton = new JButton("Choose...");
        hbox.add(chooserButton);

        chooserButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                String editBoxColor = (String)ret.getSelectedItem();
                Color current = diagram.getNamedColor(editBoxColor, Color.BLACK);

                Color newColor =
                    JColorChooser.showDialog(vb, "Choose color", current);
                if (newColor != null) {
                    ret.setSelectedItem(Diagram.colorToRGBSpec(newColor));
                }
            }
        });

        hbox.add(Box.createHorizontalStrut(ModalDialog.CONTROL_PADDING));
        hbox.add(ModalDialog.makeHelpButton(ret, label,
            "The color can be one of a customizable set of named colors, or a string "+
            "of the form \"RGB(r,g,b)\" where r,g,b are integers in [0-255].  "+
            "The Choose button can be used to pick an RGB color visually.  "+
            "If the chosen color string is not recognized, a fallback color "+
            "will be used, usually black or gray."));

        return ret;
    }

    /** Open the dialog for choosing the shape flags. */
    private void openShapeFlagsDialog()
    {
        EntityShape shape = (EntityShape)this.shapeChooser.getSelectedItem();
        this.updateWorkingFlagsBaseShape(shape);
        ShapeFlagsDialog.exec(this, this.shapeFlagsWorkingCopy, shape);
    }

    /** Restrict the working flags to those appropriate for 'newShape', and
      * add any default flags that are applicable to 'newShape' but not to
      * 'workingFlagsBaseShape'.  Then update the latter to be 'newShape'. */
    private void updateWorkingFlagsBaseShape(EntityShape newShape)
    {
        // Update the flags for 'newShape'.
        this.shapeFlagsWorkingCopy =
            Entity.defaultFlagsForNewShape(this.workingFlagsBaseShape,
                                           this.shapeFlagsWorkingCopy,
                                           newShape);

        // Remember the new base shape.
        this.workingFlagsBaseShape = newShape;
    }

    /** React to the OK button being pressed. */
    @Override
    public void okPressed()
    {
        // Parse/validate all the fields first, including casting
        // objects from JComboBox to their expected type, so that
        // if there is a problem, we will bail before actually
        // modifying this.entity;

        TextAlign nameAlign = (TextAlign)this.nameAlignChooser.getSelectedItem();
        EntityShape shape = (EntityShape)this.shapeChooser.getSelectedItem();

        int x, y, w, h, p=0, q=0;
        try {
            x = Integer.valueOf(this.xText.getText());
            y = Integer.valueOf(this.yText.getText());
            w = Integer.valueOf(this.wText.getText());
            h = Integer.valueOf(this.hText.getText());
            if (shape.numParams >= 1) {
                p = Integer.valueOf(this.pText.getText());
            }
            if (shape.numParams >= 2) {
                q = Integer.valueOf(this.qText.getText());
            }
        }
        catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "At least one of X/Y/W/H/P/Q is not a valid integer.",
                "Input Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (shape.numParams > 0 && (p < 0 || q < 0)) {
            JOptionPane.showMessageDialog(this,
                "P and Q must be non-negative.",
                "Input Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        String fillColor = (String)this.fillColorChooser.getSelectedItem();
        String lineColor = (String)this.lineColorChooser.getSelectedItem();
        String textColor = (String)this.textColorChooser.getSelectedItem();

        ImageFillStyle imageFillStyle = (ImageFillStyle)this.imageFillStyleChooser.getSelectedItem();

        // Make sure the shape flags are appropriate for the chosen shape.
        this.updateWorkingFlagsBaseShape(shape);

        // Update the entity.
        this.entity.name = this.nameText.getText();
        this.entity.nameAlign = nameAlign;
        this.entity.attributes = this.attributeText.getText();
        this.entity.shapeFlags = this.shapeFlagsWorkingCopy.clone();
        this.entity.setFillColor(fillColor);
        this.entity.lineColor = lineColor;
        this.entity.textColor = textColor;

        // This sets 'shape', 'shapeParams', and may update 'flags'
        // (redundantly) and the line and fill colors.
        this.entity.setShapeAndDefaults(shape);

        this.entity.loc.x = x;
        this.entity.loc.y = y;
        this.entity.size.width = w;
        this.entity.size.height = h;
        this.entity.anchorName = this.anchorNameText.getText();
        this.entity.imageFileName = this.imageFileNameText.getText();
        this.entity.imageFillStyle = imageFillStyle;

        // Not completely general at this time.
        if (this.entity.shape.numParams == 2) {
            this.entity.shapeParams[0] = p;
            this.entity.shapeParams[1] = q;
        }

        // Close dialog, signaling that a change was made.
        super.okPressed();
    }

    /** React to the shape dropdown being changed. */
    @Override
    public void itemStateChanged(ItemEvent e)
    {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            updateControls();
        }
    }

    /** Update certain dialog control state based on how the
      * other controls are currently set. */
    private void updateControls()
    {
        EntityShape shape = (EntityShape)this.shapeChooser.getSelectedItem();

        // Enable or disable P/Q based on which shape is active.
        boolean en = (shape.numParams == 2);
        this.pText.setEnabled(en);
        this.qText.setEnabled(en);

        // Set the params caption.
        if (shape == EntityShape.ES_CUBOID) {
            this.paramsLabel.setText("Cuboid extends left by P, up by Q pixels:");
            if (this.pText.getText().isEmpty()) {
                // Set a reasonable default.
                this.pText.setText("5");
                this.qText.setText("10");
            }
        }
        else if (shape == EntityShape.ES_WINDOW) {
            this.paramsLabel.setText("Window auto-resize center at (P,Q):");
            if (this.pText.getText().isEmpty()) {
                this.pText.setText(String.valueOf(this.entity.size.width/2));
                this.qText.setText(String.valueOf(this.entity.size.height/2));
            }
        }
        else if (shape == EntityShape.ES_SCROLLBAR) {
            this.paramsLabel.setText("Thumb drawn from P% to Q%:");
            if (this.pText.getText().isEmpty()) {
                this.pText.setText("0");
                this.qText.setText("25");
            }
        }
        else {
            this.paramsLabel.setText("Shape params (none for this shape):");
        }

        // Enable or disable shape flags button.
        this.shapeFlagsButton.setEnabled(!ShapeFlag.allFlagsForShape(shape).isEmpty());
    }

    /** Show the edit dialog for Entity, waiting until the user closes the dialog
      * before returning.  If the user presses OK, 'entity' will be updated and
      * true returned.  Otherwise, 'entity' is not modified, and false is returned. */
    public static boolean exec(Component documentParent, Diagram diagram, Entity entity)
    {
        EntityDialog dialog = new EntityDialog(documentParent, diagram, entity);
        return dialog.exec();
    }
}

// EOF
