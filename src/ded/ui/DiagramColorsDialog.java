// CustomColorsDialog.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import util.swing.ModalDialog;
import util.swing.SwingUtil;

import ded.model.Diagram;

/** All the user to edit the set of colors associated
  * with the diagram. */
public class DiagramColorsDialog extends ModalDialog {
    // ---- types ----
    /** One entry in the table. */
    private static class ColorTableEntry {
        /** Name of the color.  This corresponds to the lookup key
          * in Diagram.namedColors. */
        public String name;

        /** Value of the color.  These are the values stored in
          * Diagram.namedColors. */
        public Color color;

        public ColorTableEntry(String n, Color c)
        {
            this.name = n;
            this.color = c;
        }
    }

    /** Table model providing data for the JTable. */
    private static class ColorTableModel extends AbstractTableModel {
        // ---- data ----
        private static final long serialVersionUID = -1612457674929291915L;

        private static final String[] columnNames = {
            "Color Name",
            "RGB Spec",
            "Color Sample"
        };

        private static final Class<?>[] columnClasses = {
            String.class,
            String.class,
            Color.class
        };

        /** Table of colors to edit.  This is a copy of those stored
          * in the diagram object, so we can freely modify it even
          * if the user eventually cancels. */
        public ArrayList<ColorTableEntry> colors = new ArrayList<ColorTableEntry>();

        /** The table widget, so I can send it messages. */
        public JTable jtable;

        // ---- methods ----
        public ColorTableModel(Diagram diagram)
        {
            setColorTable(diagram.namedColors);
        }

        /** Change the entire color table to match 'namedColors'. */
        public void setColorTable(LinkedHashMap<String, Color> namedColors)
        {
            this.colors.clear();
            for (Map.Entry<String, Color> entry : namedColors.entrySet()) {
                this.colors.add(new ColorTableEntry(entry.getKey(), entry.getValue()));
            }
        }

        /** Set the jtable.  This has to be done after construction
          * because 'this' is an argument to the JTable's constructor. */
        public void setJTable(JTable jt)
        {
            this.jtable = jt;
        }

        @Override
        public int getRowCount()
        {
            return this.colors.size();
        }

        @Override
        public int getColumnCount()
        {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int columnIndex)
        {
            return columnNames[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            return columnClasses[columnIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return true;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            ColorTableEntry entry = this.colors.get(rowIndex);
            switch (columnIndex) {
                default:
                    assert(false);

                case 0:
                    return entry.name;

                case 1:
                    return Diagram.colorToRGBSpec(entry.color);

                case 2:
                    return entry.color;
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col)
        {
            if (debug) {
                System.out.println("setValueAt: value="+value+
                                   " row="+row+" col="+col);
            }
            ColorTableEntry entry = this.colors.get(row);
            switch (col) {
                default:
                    assert(false);
                    return;

                case 0:
                    entry.name = (String)value;
                    break;

                case 1: {
                    Color c = Diagram.rgbSpecToColor((String)value);
                    if (c == null) {
                        // This isn't a great UI because it throws away what
                        // the user entered, but I do not expect users to
                        // hit this very often, and the JTable mechanism for
                        // data validation is significant work to implement.
                        SwingUtil.errorMessageBox(this.jtable,
                            "Invalid RGB spec \""+value+
                            "\".  It must have the form \"RGB(r,g,b)\" "+
                            "where each of r, g, and b is in [0,255].");
                    }
                    else {
                        entry.color = c;
                    }
                    break;
                }

                case 2:
                    entry.color = (Color)value;
                    break;
            }
            this.jtable.tableChanged(new TableModelEvent(this, row));
        }
    }

    /** Custom table cell renderer for the color swatches. */
    public static class ColorRenderer extends JLabel
                                      implements TableCellRenderer {
        // ---- data ----
        private static final long serialVersionUID = 7995335677535282832L;

        private Border unselectedBorder;
        private Border selectedBorder;

        // ---- methods ----
        public ColorRenderer(JTable table)
        {
            setOpaque(true);   // so 'setBackground' has an effect

            this.unselectedBorder = BorderFactory.createMatteBorder(1,1,1,1,
                table.getBackground());
            this.selectedBorder = BorderFactory.createMatteBorder(1,1,1,1,
                table.getSelectionBackground());
        }

        @Override
        public Component getTableCellRendererComponent(
            JTable table, Object value,
            boolean isSelected, boolean hasFocus,
            int row, int column)
        {
            setBackground((Color)value);
            if (isSelected) {
                this.setBorder(this.selectedBorder);
            } else {
                this.setBorder(this.unselectedBorder);
            }
            return this;
        }
    }

    /** Editor for the color cells, closely modeled on the one
      * in the Oracle JTable tutorial. */
    public class ColorCellEditor extends AbstractCellEditor
                                 implements TableCellEditor,
                                            ActionListener {
        // ---- data ----
        private static final long serialVersionUID = -7949676852910493963L;

        /** I do not fully understand the role of this member. */
        private Color currentColor;

        /** Button that responds to (e.g.) clicking on the cell. */
        private JButton button;

        /** Dialog that lets the user choose a color.  Its lifetime
          * matches the JTable, and the dialog is merely hidden when
          * the user is not choosing a color. */
        private JColorChooser colorChooser;
        private JDialog dialog;

        /** Action command name the button uses to communicate with
          * 'this' (as an ActionListener). */
        protected static final String EDIT = "edit";

        // ---- methods ----
        public ColorCellEditor()
        {
            // Make the button that will act as the "edit" interface.
            // The important thing here is simply that it (1) draw itself
            // with the right color, and (2) react to clicks and certain
            // keyboard keys (enter, space) by notifying 'this' of the
            // user's intent to edit.
            this.button = new JButton();
            this.button.setActionCommand(EDIT);
            this.button.addActionListener(this);
            this.button.setBorderPainted(false);

            // Set up the dialog that the button brings up.
            this.colorChooser = new JColorChooser();
            this.dialog = JColorChooser.createDialog(
                this.button,
                "Choose a Color",
                true,  // modal
                this.colorChooser,
                this,  // OK button handler
                null); // no CANCEL button handler
        }

        /** Respond to notifications from either the button or the
          * dialog. */
        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (EDIT.equals(e.getActionCommand())) {
                // The user has clicked the cell, so
                // bring up the dialog.
                this.button.setBackground(this.currentColor);
                this.colorChooser.setColor(this.currentColor);
                this.dialog.setVisible(true);

                // Make the cell renderer reappear.
                //
                // Note: At this point, the color chooser dialog
                // is still shown.  We're saying we are done editing
                // so the cell returns to the passive state, but from
                // the user's point of view, editing is still happening.
                fireEditingStopped();
            }
            else {
                // User pressed dialog's "OK" button.  (I think it is a
                // bit sloppy to not perform a stronger check of 'e', but
                // this is how the tutorial does it, and I don't know what
                // to check.)
                this.currentColor = this.colorChooser.getColor();
                if (debug) {
                    System.out.println("ColorEditor.actionPerformed: currentColor="+this.currentColor);
                }
            }
        }

        @Override
        public Object getCellEditorValue()
        {
            if (debug) {
                System.out.println("getCellEditorValue returning: "+this.currentColor);
            }
            return this.currentColor;
        }

        // Return the component that plays the role of editor for the
        // given cell (which is known to be in column 2 due to this
        // editor being associated with the Class of column 2).
        @Override
        public Component getTableCellEditorComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     int row,
                                                     int column)
        {
            if (debug) {
                System.out.println("getTableCellEditorComponent: value="+value+
                                   " row="+row+" col="+column);
            }
            this.currentColor = (Color)value;
            this.button.setBackground(this.currentColor);
            return this.button;
        }
    }

    // ---- data ----
    private static final long serialVersionUID = 7844419514178978870L;

    /** True to enable diagnostic printouts. */
    private static final boolean debug = false;

    /** The diagram whose colors will be edited. */
    private Diagram diagram;

    /** The structure holding the data while it is being edited. */
    private ColorTableModel tableModel;

    /** The table widget. */
    private JTable jtable;

    // ---- methods ----
    public DiagramColorsDialog(Component parent, Diagram d)
    {
        super(parent, "Custom Diagram Colors");
        this.diagram = d;

        this.tableModel = new ColorTableModel(this.diagram);
        this.jtable = new JTable(this.tableModel);
        this.tableModel.setJTable(this.jtable);
        this.jtable.setPreferredScrollableViewportSize(new Dimension(400, 300));
        this.jtable.setFillsViewportHeight(true);
        this.jtable.setDefaultRenderer(Color.class, new ColorRenderer(this.jtable));
        this.jtable.setDefaultEditor(Color.class, new ColorCellEditor());
        JScrollPane colorTableScrollPane = new JScrollPane(this.jtable);

        JButton moveUp10Button = makeButton("Move up 10", 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DiagramColorsDialog.this.moveSelectedItems(-10);
            }
        });

        JButton moveUp1Button = makeButton("Move up 1", KeyEvent.VK_U, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DiagramColorsDialog.this.moveSelectedItems(-1);
            }
        });

        JButton moveDown1Button = makeButton("Move down 1", KeyEvent.VK_D, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DiagramColorsDialog.this.moveSelectedItems(+1);
            }
        });

        JButton moveDown10Button = makeButton("Move down 10", 0, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DiagramColorsDialog.this.moveSelectedItems(+10);
            }
        });

        JButton addButton = makeButton("Add", KeyEvent.VK_A, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DiagramColorsDialog.this.addNewColor();
            }
        });

        JButton deleteButton = makeButton("Delete", KeyEvent.VK_E, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DiagramColorsDialog.this.deleteSelectedColors();
            }
        });

        JButton helpButton = makeButton("Help...", KeyEvent.VK_H, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DiagramColorsDialog.this.showHelpDialog();
            }
        });

        JButton resetButton = makeButton("Reset to default", KeyEvent.VK_R, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                DiagramColorsDialog.this.resetToDefault();
            }
        });

        JButton cancelButton = this.makeCancelButton();
        JButton okButton = this.makeOKButton();

        // Construct the layout.
        Box vb = ModalDialog.makeMarginVBox(this, ModalDialog.OUTER_MARGIN);

        // Table and move buttons.
        {
            Box hb = Box.createHorizontalBox();
            hb.add(colorTableScrollPane);

            Box vb2 = Box.createVerticalBox();
            vb2.add(Box.createVerticalGlue());
            vb2.add(moveUp10Button);
            vb2.add(moveUp1Button);
            vb2.add(moveDown1Button);
            vb2.add(moveDown10Button);
            vb2.add(Box.createVerticalGlue());
            hb.add(vb2);

            vb.add(hb);
        }

        // Add/edit/delete buttons.
        {
            Box hb = Box.createHorizontalBox();
            hb.add(addButton);
            hb.add(deleteButton);
            hb.add(Box.createHorizontalGlue());
            vb.add(hb);
        }

        // Help/edit/cancel/ok buttons.
        {
            Box hb = Box.createHorizontalBox();
            hb.add(helpButton);
            hb.add(resetButton);
            hb.add(Box.createHorizontalGlue());
            hb.add(cancelButton);
            hb.add(okButton);
            vb.add(hb);
        }

        this.pack();
        this.setLocationRelativeTo(this.getParent());
    }

    @Override
    public void okPressed()
    {
        // Rebuild a new color table to put in the diagram.
        LinkedHashMap<String, Color> newTable = new LinkedHashMap<String, Color>();
        for (ColorTableEntry entry : this.tableModel.colors) {
            if (newTable.containsKey(entry.name)) {
                SwingUtil.errorMessageBox(this, "Color \""+entry.name+"\" appears more than once.");
                return;     // Do not close the dialog.
            }
            newTable.put(entry.name, entry.color);
        }

        // Install the new table in the diagram.
        this.diagram.namedColors = newTable;

        super.okPressed();
    }

    /** Respond to the user pressing the Add button. */
    public void addNewColor()
    {
        // It is ok that this name might not be unique.  A duplicate
        // name will be detected in 'okPressed'.
        this.tableModel.colors.add(new ColorTableEntry("New Color", Color.BLACK));
        this.jtable.tableChanged(new TableModelEvent(this.tableModel));
    }

    /** Respond to the user pressing the Delete button. */
    public void deleteSelectedColors()
    {
        int[] selRows = this.jtable.getSelectedRows();
        if (selRows.length == 0) {
            SwingUtil.errorMessageBox(this,
                "You must select at least one row to delete it.");
            return;
        }
        Arrays.sort(selRows);

        // Go backwards to avoid each removal invalidating the
        // indices yet to be processed.
        for (int i = selRows.length-1; i >= 0; i--) {
            int row = selRows[i];
            this.tableModel.colors.remove(row);
        }

        this.jtable.tableChanged(new TableModelEvent(this.tableModel));
    }

    /** Respond to one of the Move buttons.  All selected items
      * should change their index by 'delta'. */
    public void moveSelectedItems(int delta)
    {
        assert(delta != 0);

        int[] selRows = this.jtable.getSelectedRows();
        if (selRows.length == 0) {
            SwingUtil.errorMessageBox(this,
                "You must select at least one row to delete it.");
            return;
        }
        Arrays.sort(selRows);

        // Build a parallel array of selection bits so we can track
        // their movement too.
        ArrayList<Boolean> selBits = new ArrayList<Boolean>(this.tableModel.colors.size());
        for (int row=0; row < this.tableModel.colors.size(); row++) {
            selBits.add(false);
        }
        for (int i=0; i < selRows.length; i++) {
            int row = selRows[i];
            selBits.set(row, true);
        }

        // Use a loop to implement larger movements than one.
        // (Not very efficient, but easy to code.)
        int n = Math.abs(delta);
        for (int i=0; i < n; i++) {
            moveSelectedItemsByOne(selBits, delta);
        }

        // Fire table changed now?
        this.jtable.tableChanged(new TableModelEvent(this.tableModel));

        // Now update the selected rows based on the movement of
        // the underlying rows.
        this.jtable.clearSelection();
        for (int row=0; row < selBits.size(); row++) {
            if (selBits.get(row)) {
                this.jtable.addRowSelectionInterval(row, row);
            }
        }
    }

    /** Move the rows marked 'true' in 'selBits' either up or down one
      * space depending on the sign of 'delta': negative=up, positive=down.
      *
      * The 'selBits' is also updated to track which rows should be
      * selected afterward (or next, in the case of moving rows by
      * multiple spaces at once). */
    private void moveSelectedItemsByOne(ArrayList<Boolean> selBits, int delta)
    {
        if (delta > 0) {
            // The largest index a row can have and be able to move.
            int limit = selBits.size()-2;

            // Moving down, go backwards.
            for (int row = selBits.size()-1; row >= 0; row--) {
                if (!selBits.get(row)) {
                    // This row does not want to move.
                }
                else {
                    if (row > limit) {
                        // The row wants to but cannot move.
                    }
                    else {
                        this.tableModel.colors.add(row+1,
                            this.tableModel.colors.remove(row));
                        selBits.add(row+1,
                            selBits.remove(row));
                    }
                    limit--;
                }
            }
        }
        else {
            // The smallest index a row can have and be able to move.
            int limit = 1;

            // Moving up, go forwards
            for (int row=0; row < selBits.size(); row++) {
                if (!selBits.get(row)) {
                    // Row does not want to move.
                }
                else {
                    if (row < limit) {
                        // Cannot move.
                    }
                    else {
                        this.tableModel.colors.add(row-1,
                            this.tableModel.colors.remove(row));
                        selBits.add(row-1,
                            selBits.remove(row));
                    }
                    limit++;
                }
            }
        }
    }

    /** Respond to the user pressing the Reset button. */
    public void resetToDefault()
    {
        this.tableModel.setColorTable(Diagram.makeDefaultColors());
        this.jtable.tableChanged(new TableModelEvent(this.tableModel));
    }

    /** Respond to the user pressing the Help button. */
    public void showHelpDialog()
    {
        SwingUtil.informationMessageBox(this, "Diagram Colors Help",
            "The diagram colors are a set of named colors that can be "+
            "used within the diagram elements as fill colors, line colors, "+
            "etc.  Changing the value of any color will change the "+
            "appearance of any element using that named color.\n\n"+

            "Selected entries can be moved up and down to change their order in "+
            "the combo boxes in dialogs, and in right-click menus.  The "+
            "order only affects the user interface behavior, not the "+
            "diagram appearance.\n\n"+

            // The quirky keyboard controls are the default behavior of
            // JTable, and I haven't bothered to fix it.  Space will
            // actually enter edit mode for string columns, except the
            // text cursor is invisible if you do it that way.
            "Double-click a cell to edit it.  Or, use the keyboard to "+
            "navigate to a cell and press F2 (text columns) or Space "+
            "(color column).\n\n"+

            "The Add button adds a new color.  The Delete button deletes "+
            "the selected color or colors.\n\n"+

            "The Reset button restores the \"ded\" default color table, "+
            "which means no color table will be saved to the .ded file.\n\n"+

            "The Cancel button will discard all changes made since the dialog "+
            "was opened.");
    }

    /** Show the dialog, waiting until the user closes the dialog
      * before returning.  If the user presses OK, 'diagram' will be updated and
      * true returned.  Otherwise, 'diagram' is not modified, and false is returned. */
    public static boolean exec(Component documentParent, Diagram diagram)
    {
        DiagramColorsDialog dialog = new DiagramColorsDialog(documentParent, diagram);
        return dialog.exec();
    }
}

// EOF
