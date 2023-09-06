// GraphNodeDialog.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import util.Util;
import util.swing.ModalDialog;
import util.swing.SwingUtil;

import ded.model.ObjectGraphConfig;
import ded.model.ObjectGraphNode;

import static util.StringUtil.fmt;

/** Dialog to show the details of a graph node. */
public class GraphNodeDialog extends ModalDialog
{
    // ---- types ----
    /** One entry in the table. */
    // This is package-private so the tests can access it.
    /*package*/ static class TableEntry implements Comparable<TableEntry> {
        /** True if this is an attribute, false if a pointer. */
        public boolean m_isAttribute;

        /** If not null, this key is shown, and its order is ascending
          * by the value.  Otherwise it is not shown. */
        public Float m_ordinal;

        /** Field name/key. */
        public String m_key;

        /** Field value. */
        public String m_value;

        public TableEntry(boolean isAttribute, Float ordinal,
                          String key, String value)
        {
            m_isAttribute = isAttribute;
            m_ordinal = ordinal;
            m_key = key;
            m_value = value;
        }

        /**
          Return neg/zero/pos if 'this' is lt/eq/gt 'other',
          lexicographically by the following rules, in order:

            - non-null 'm_ordinal' before null

            - ascending order of 'm_ordinal', when non-null

            - true 'm_isAttribute' before false

            - ascending 'm_key'

            - ascending 'm_value'
        */
        @Override
        public int compareTo(TableEntry other)
        {
            int res = - Util.compareNullability(m_ordinal,
                                                other.m_ordinal);
            if (res != 0) { return res; }

            if (m_ordinal != null) {
                res = m_ordinal.compareTo(other.m_ordinal);
                if (res != 0) { return res; }
            }

            res = - Boolean.compare(this.m_isAttribute, other.m_isAttribute);
            if (res != 0) { return res; }

            res = m_key.compareTo(other.m_key);
            if (res != 0) { return res; }

            res = m_value.compareTo(other.m_value);
            if (res != 0) { return res; }

            return 0;
        }

        // I don't think I need this, but it's easy with 'compareTo'.
        @Override
        public boolean equals(Object obj)
        {
            return Util.equalsViaCompare(this, obj);
        }

        // I don't really need this, but since I overrode 'equals'...
        @Override
        public int hashCode()
        {
            int h = 0;
            h = h*31 + Boolean.hashCode(m_isAttribute);
            h = h*31 + Util.nullableHashCode(m_ordinal);
            h = h*31 + m_key.hashCode();
            h = h*31 + m_value.hashCode();
            return 0;
        }
    }

    /** Table model providing data for the JTable. */
    private static class TableModel extends AbstractTableModel {
        // ---- class data ----
        private static final long serialVersionUID = 584594634602515823L;

        private static final String[] s_columnNames = {
            "Ord",
            "P",       // "Pointer?"
            "Name",
            "Value"
        };

        private static final Class<?>[] s_columnClasses = {
            Float.class,
            String.class,
            String.class,
            String.class
        };

        // ---- instance data ----
        /** Table of fields.  This is a copy of what is in the diagram
          * object, so we can freely modify it even if the user
          * eventually cancels. */
        public ArrayList<TableEntry> m_entries = new ArrayList<TableEntry>();

        /** The table widget, so I can send it messages. */
        public JTable m_jtable;

        // ---- methods ----
        public TableModel(ArrayList<TableEntry> entries)
        {
            m_entries = entries;
        }

        /** Set the jtable.  This has to be done after construction
          * because 'this' is an argument to the JTable's constructor. */
        public void setJTable(JTable jt)
        {
            this.m_jtable = jt;
        }

        @Override
        public int getRowCount()
        {
            return this.m_entries.size();
        }

        @Override
        public int getColumnCount()
        {
            return s_columnNames.length;
        }

        @Override
        public String getColumnName(int columnIndex)
        {
            return s_columnNames[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            return s_columnClasses[columnIndex];
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            // Only the ordinals are editable.
            return columnIndex == 0;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            TableEntry entry = this.m_entries.get(rowIndex);
            switch (columnIndex) {
                default:
                    assert(false);

                case 0:
                    return entry.m_ordinal;

                case 1:
                    return entry.m_isAttribute? "" : "P";

                case 2:
                    return entry.m_key;

                case 3:
                    return entry.m_value;
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col)
        {
            if (s_debug) {
                System.err.println("setValueAt: value="+value+
                                   " row="+row+" col="+col);
            }
            TableEntry entry = this.m_entries.get(row);
            switch (col) {
                default:
                    assert(false);
                    return;

                // Only one column is editable.
                case 0:
                    entry.m_ordinal = (Float)value;
                    break;
            }
            this.m_jtable.tableChanged(new TableModelEvent(this, row));
        }
    }

    // ---- private class data ----
    private static final long serialVersionUID = -87133330572297627L;

    /** True to enable diagnostic printouts. */
    private static final boolean s_debug = false;

    // ---- private instance data ----
    /** The controller of the entity associated with the node whose data
      * we are showing.  Not null. */
    private EntityController m_controller;

    /** The structure holding the data while it is being edited. */
    private TableModel m_tableModel;

    // Controls.
    private JTextField m_idTextField;
    private JTable m_jtable;

    // ---- methods ----
    public GraphNodeDialog(EntityController controller)
    {
        super(controller.diagramController, "Node Details");

        m_controller = controller;

        Box vb = ModalDialog.makeMarginVBox(this, ModalDialog.OUTER_MARGIN);

        // ID
        {
            Box hb = ModalDialog.makeHBox(vb);

            m_idTextField =
                ModalDialog.makeLineEdit(hb, "ID", 'i',
                    m_controller.entity.objectGraphNodeID);
            m_idTextField.setEditable(false);
        }

        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        // Attributes and pointers.
        {
            JLabel attributesLabel;
            {
                Box hb = ModalDialog.makeHBox(vb);
                attributesLabel = new JLabel("Attributes and pointers:");
                attributesLabel.setDisplayedMnemonic('a');
                hb.add(attributesLabel);
                hb.add(Box.createHorizontalGlue());
                vb.add(hb);
            }

            m_tableModel = new TableModel(getTableEntries());
            m_jtable = new JTable(m_tableModel);
            m_tableModel.setJTable(m_jtable);
            m_jtable.setPreferredScrollableViewportSize(new Dimension(800, 500));
            m_jtable.setFillsViewportHeight(true);

            // Make the "Ord" column narrow.
            m_jtable.getColumnModel().getColumn(0).setPreferredWidth(30);
            m_jtable.getColumnModel().getColumn(1).setPreferredWidth(15);
            m_jtable.getColumnModel().getColumn(2).setPreferredWidth(350);
            m_jtable.getColumnModel().getColumn(3).setPreferredWidth(350);

            JScrollPane colorTableScrollPane = new JScrollPane(m_jtable);

            attributesLabel.setLabelFor(m_jtable);

            vb.add(colorTableScrollPane);
        }

        vb.add(Box.createVerticalStrut(ModalDialog.CONTROL_PADDING));

        m_helpText = Util.readResourceString_joinAdjacentLines(
            "/resources/helptext/GraphNodeDialog.txt");

        this.finishBuildingDialog(vb);
    }

    /** Get the attributes and pointers of the node as an array of
      * table entries. */
    private ArrayList<TableEntry> getTableEntries()
    {
        ArrayList<TableEntry> entries = new ArrayList<TableEntry>();
        ObjectGraphConfig config = getObjectGraphConfig();

        ObjectGraphNode node = m_controller.getGraphNode();
        if (node == null) {
            entries.add(new TableEntry(true /*isAttribute*/, null, "",
                fmt("(No node with this ID exists.)\n")));
        }
        else {
            for (Object keyObj : node.m_attributes.keySet()) {
                String key = (String)keyObj;
                entries.add(
                    new TableEntry(
                        true /*isAttribute*/,
                        config.getFieldOrdinalFloatOpt(key),
                        key,
                        node.getAttributeString(key)));
            }

            for (String key : node.m_pointers.keySet()) {
                entries.add(
                    new TableEntry(
                        false /*isAttribute*/,
                        config.getFieldOrdinalFloatOpt(key),
                        key,
                        node.getPointerString(key)));
            }

            Collections.sort(entries);
        }

        return entries;
    }

    /** Get the relevant graph configuration. */
    private ObjectGraphConfig getObjectGraphConfig()
    {
        return m_controller.diagramController.diagram.m_objectGraphConfig;
    }

    @Override
    public void okPressed()
    {
        ObjectGraphConfig config = getObjectGraphConfig();

        // Update the configuration based on the edited table.
        config.setShowFields(
            computeNewShowFields(m_tableModel.m_entries, config.m_showFields));

        m_controller.diagramController.diagramChanged("Edited showFields");

        super.okPressed();
    }

    /** Given the table entries edited by the user, and the old sequence
      * of fields to show, compute a new sequence.  Do not modify either
      * of the input objects. */
    // This is package-private so the tests can access it.
    /*package*/ static ArrayList<String> computeNewShowFields(
        List<TableEntry> editedEntries,
        List<String> oldShowFields)
    {
        // Work on a copy of the table data so we do not modify the
        // input object.
        ArrayList<TableEntry> entries =
            new ArrayList<TableEntry>(editedEntries);

        // Add table entries for keys that were in the original show
        // fields set but not shown to the user.  They need to be
        // preserved.
        {
            // Get the set of fields in that were shown in the dialog.
            HashSet<String> nodeFields = new HashSet<String>();
            for (TableEntry entry : entries) {
                nodeFields.add(entry.m_key);
            }

            // Add non-node fields to 'm_entries'.
            int pos = 0;
            for (String key : oldShowFields) {
                if (!nodeFields.contains(key)) {
                    entries.add(
                        new TableEntry(
                            true /*isAttribute; irrelevant here*/,
                            Float.valueOf(pos),
                            key,
                            "" /*value; irrelevant*/));
                }
                ++pos;
            }
        }

        // Now sort the entries so all the fields we want in the new
        // set are at the top, in the proper order.
        Collections.sort(entries);

        // Convert the table entries into a new "show fields" sequence.
        ArrayList<String> newShowFields = new ArrayList<String>();
        for (TableEntry entry : entries) {
            if (entry.m_ordinal != null) {
                newShowFields.add(entry.m_key);
            }
            else {
                // Minor optimization: stop when we reach the entries
                // without ordinals.
                break;
            }
        }

        return newShowFields;
    }

    /** Show the dialog and return when the user dismisses it. */
    public static boolean exec(EntityController controller)
    {
        GraphNodeDialog dialog = new GraphNodeDialog(controller);
        return dialog.exec();
    }
}


// EOF
