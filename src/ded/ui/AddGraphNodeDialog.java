// AddGraphNodeDialog.java
// See toplevel license.txt for copyright and license terms.

package ded.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.awt.Dimension;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

import javax.swing.table.AbstractTableModel;

import util.MapUtil;
import util.Util;

import util.swing.ModalDialog;
import util.swing.SwingUtil;

import ded.model.Entity;
import ded.model.ObjectGraph;

import ded.ui.DiagramController;


/** Show a dialog that allows the user to pick a node to add. */
public class AddGraphNodeDialog extends ModalDialog {
    // ---- types ----
    /** One entry in the table. */
    private static class TableEntry implements Comparable<TableEntry> {
        /** Count of existing entities with the ID. */
        public int m_entityCount;

        /** The node ID. */
        public String m_id;

        public TableEntry(int entityCount, String id)
        {
            m_entityCount = entityCount;
            m_id = id;
        }

        @Override
        public int compareTo(TableEntry other)
        {
            // Sort by ID.
            return m_id.compareTo(other.m_id);
        }
    }

    /** Table model providing data for the JTable. */
    private static class TableModel extends AbstractTableModel {
        // ---- class data ----
        private static final long serialVersionUID = -9127244580221510146L;

        private static final String[] s_columnNames = {
            "Count",
            "Node ID",
        };

        private static final Class<?>[] s_columnClasses = {
            Integer.class,
            String.class,
        };

        public static final int[] s_columnWidths = {
            60,    // Large enough for the column label.
            300,
        };

        // ---- instance data ----
        /** Table data. */
        public List<TableEntry> m_entries = new ArrayList<TableEntry>();

        // ---- methods ----
        public TableModel()
        {}

        @Override
        public int getRowCount()
        {
            return m_entries.size();
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
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            TableEntry entry = this.m_entries.get(rowIndex);
            switch (columnIndex) {
                default:
                    assert(false);

                case 0:
                    return Integer.valueOf(entry.m_entityCount);

                case 1:
                    return entry.m_id;
            }
        }
    }

    // ---- private class data ----
    private static final long serialVersionUID = 8342894600860734823L;

    // ---- private instance data ----
    /** Diagram controller with graph data and diagram. */
    private DiagramController m_diagramController;

    /** Table data to show. */
    private TableModel m_tableModel;

    /** The chosen ID if the user presses Ok. */
    private String m_chosenID = null;

    // Controls.
    private JTable m_jtable = null;

    // ---- methods ----
    public AddGraphNodeDialog(DiagramController diagramController)
    {
        super(diagramController, "Add Graph Node");

        m_diagramController = diagramController;

        m_tableModel = new TableModel();
        populateTable();

        Box vb = ModalDialog.makeMarginVBox(this, ModalDialog.OUTER_MARGIN);

        // Table
        {
            JLabel tableLabel;
            {
                Box hb = ModalDialog.makeHBox(vb);
                tableLabel = new JLabel("Nodes:");
                tableLabel.setDisplayedMnemonic('n');
                hb.add(tableLabel);
                hb.add(Box.createHorizontalGlue());
                vb.add(hb);
            }

            m_jtable = new JTable(m_tableModel);
            tableLabel.setLabelFor(m_jtable);
            SwingUtil.setJTableSizes(m_jtable,
                TableModel.s_columnWidths, 500 /*height*/);
            m_jtable.getSelectionModel().setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

            final AddGraphNodeDialog ths = this;

            // Intercept double-click to mean "create".
            m_jtable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    ths.tableClicked(e);
                }
            });

            // Intercept Enter to make "create".
            InputMap inputMap = m_jtable.getInputMap();
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                         "Create");
            ActionMap actionMap = m_jtable.getActionMap();
            actionMap.put("Create", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    ths.tableCreateEntity();
                }
            });

            // Allow Tab to escape from the table.
            disableTabInComponent(m_jtable);

            JScrollPane tableScrollPane = new JScrollPane(m_jtable);
            vb.add(tableScrollPane);
        }

        vb.add(ModalDialog.makeVCPadStrut());

        m_helpText = Util.readResourceString_joinAdjacentLines(
            "/resources/helptext/AddGraphNodeDialog.txt");

        this.finishBuildingDialog(vb);
    }

    /** Populate 'm_tableModel' with data from the diagram. */
    private void populateTable()
    {
        ObjectGraph graph = m_diagramController.diagram.objectGraph;

        // Count the number of entities with each ID.
        Map<String, Integer> idToEntityCount =
            new HashMap<String, Integer>();

        for (Entity entity : m_diagramController.diagram.entities) {
            if (!entity.hasObjectGraphNodeID()) {
                continue;
            }

            MapUtil.incrementValue(idToEntityCount,
                entity.objectGraphNodeID);
        }

        // Populate the table with IDs and counts.
        for (String id : graph.idSet()) {
            m_tableModel.m_entries.add(new TableEntry(
                idToEntityCount.getOrDefault(id, 0),
                id));
        }

        // Sort by ID.
        Collections.sort(m_tableModel.m_entries);
    }

    /** Respond to double-clicking on a row like choosing it and
        pressing Ok. */
    private void tableClicked(MouseEvent e)
    {
        if (!SwingUtil.isDoubleClick(e)) {
            return;
        }

        int row = m_jtable.rowAtPoint(e.getPoint());
        if (row < 0) {
            // Ignore if not on a row.
            return;
        }

        createEntityForRow(row);
        super.okPressed();
    }

    /** Create the entity at the selected row, if any. */
    private void tableCreateEntity()
    {
        int row = m_jtable.getSelectedRow();
        if (row < 0) {
            return;
        }

        createEntityForRow(row);
        super.okPressed();
    }

    @Override
    public void okPressed()
    {
        int row = m_jtable.getSelectedRow();
        if (row < 0) {
            SwingUtil.errorMessageBox(this,
                "First select the row containing the ID of the node "+
                "to create, then press Ok.  Or, press Cancel to not "+
                "create any node.");
            return;
        }

        createEntityForRow(row);
        super.okPressed();
    }

    /** Create an entity corresponding to the node whose ID is at table
        row 'row'. */
    private void createEntityForRow(int row)
    {
        TableEntry tableEntry = m_tableModel.m_entries.get(row);
        String id = tableEntry.m_id;

        // This calls 'diagramChanged'.
        EntityController entityController =
            m_diagramController.createEntityControllerWithGraphID(
                id, m_diagramController.getViewportCenter());

        m_diagramController.selectOnly(entityController);
    }

    /** Show the dialog and return when the user dismisses it.  If this
        returns true, the diagram has already been modified and
        'diagramChanged' called. */
    public static boolean exec(DiagramController diagramController)
    {
        AddGraphNodeDialog dialog =
            new AddGraphNodeDialog(diagramController);
        return dialog.exec();
    }
}


// EOF
