/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.gui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GREEN phase: Tests that validate new implementation maintains backward compatibility
 * with existing code that uses JSortTable.
 */
@DisplayName("Backward Compatibility Tests (GREEN Phase)")
class BackwardCompatibilityTest {

    private JSortTable table;
    private SortTableModel model;
    private Vector<Vector<Object>> testData;

    @BeforeEach
    void setUp() {
        // Replicate setup from ConnectDialog.java
        String[] columnNames = new String[]{"Session Name", "Host", "Default"};
        testData = new Vector<>();

        addRow("Session1", "host1.example.com", "Yes");
        addRow("Session2", "host2.example.com", "No");
        addRow("Session3", "host3.example.com", "No");

        model = new TestSortTableModel(testData, new Vector<>(java.util.Arrays.asList(columnNames)));
        table = new JSortTable(model);
    }

    private void addRow(String name, String host, String isDefault) {
        Vector<Object> row = new Vector<>();
        row.add(name);
        row.add(host);
        row.add(isDefault);
        testData.add(row);
    }

    @Test
    @DisplayName("JSortTable can be instantiated with SortTableModel")
    void testJSortTableCreation() {
        assertNotNull(table, "JSortTable should be created successfully");
        assertNotNull(table.getModel(), "JSortTable should have a model");
    }

    @Test
    @DisplayName("JSortTable can be instantiated with any TableModel")
    void testJSortTableWithGenericModel() {
        DefaultTableModel genericModel = new DefaultTableModel();
        genericModel.addColumn("Name");
        genericModel.addColumn("Value");
        genericModel.addRow(new Object[]{"Test", "Data"});

        JTable genericTable = new JSortTable(genericModel);
        assertNotNull(genericTable, "JSortTable should accept generic TableModel");
        assertEquals(1, genericTable.getRowCount());
    }

    @Test
    @DisplayName("Can access table header after JSortTable creation")
    void testTableHeaderAccessible() {
        javax.swing.table.JTableHeader header = table.getTableHeader();
        assertNotNull(header, "Table header should be accessible");
        assertEquals(3, header.getColumnModel().getColumnCount());
    }

    @Test
    @DisplayName("Can set column widths on JSortTable")
    void testColumnWidthConfiguration() {
        // This is how ConnectDialog.java configures columns
        table.getColumnModel().getColumn(0).setPreferredWidth(250);
        table.getColumnModel().getColumn(1).setPreferredWidth(250);
        table.getColumnModel().getColumn(2).setPreferredWidth(65);

        assertEquals(250, table.getColumnModel().getColumn(0).getPreferredWidth());
        assertEquals(250, table.getColumnModel().getColumn(1).getPreferredWidth());
        assertEquals(65, table.getColumnModel().getColumn(2).getPreferredWidth());
    }

    @Test
    @DisplayName("Can set selection mode on JSortTable")
    void testSelectionModeConfiguration() {
        // This is how externals table is configured in ConnectDialog
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        assertEquals(ListSelectionModel.SINGLE_SELECTION, table.getSelectionModel().getSelectionMode());
    }

    @Test
    @DisplayName("Can set grid visibility on JSortTable")
    void testGridVisibilityConfiguration() {
        // This is how externals table is configured in ConnectDialog
        table.setShowGrid(false);
        assertFalse(table.getShowHorizontalLines());
        assertFalse(table.getShowVerticalLines());
    }

    @Test
    @DisplayName("Can get sorted column index from JSortTable")
    void testGetSortedColumnIndex() {
        int index = table.getSortedColumnIndex();
        assertEquals(-1, index, "Initial sorted column index should be -1 (unsorted)");
    }

    @Test
    @DisplayName("Can check if sort is ascending from JSortTable")
    void testIsSortedColumnAscending() {
        boolean ascending = table.isSortedColumnAscending();
        assertTrue(ascending, "Default sort should be ascending");
    }

    @Test
    @DisplayName("Model.isSortable() works after JSortTable creation")
    void testModelSortableAfterCreation() {
        assertTrue(model.isSortable(0));
        assertTrue(model.isSortable(1));
        assertTrue(model.isSortable(2));
    }

    @Test
    @DisplayName("Can call model.sortColumn() directly")
    void testDirectModelSortCall() {
        // Initial state
        assertEquals("Session1", model.getValueAt(0, 0));

        // Sort by name ascending
        model.sortColumn(0, true);
        assertEquals("Session1", model.getValueAt(0, 0), "Session1 is first alphabetically");
        assertEquals("Session2", model.getValueAt(1, 0), "Session2 is second");
        assertEquals("Session3", model.getValueAt(2, 0), "Session3 is third");
    }

    @Test
    @DisplayName("Table data remains accessible after sorting")
    void testDataAccessibleAfterSort() {
        model.sortColumn(0, true);

        // Verify all data is still present
        assertEquals(3, table.getRowCount(), "All rows should be present");

        // Verify we can read each row
        for (int i = 0; i < table.getRowCount(); i++) {
            assertNotNull(table.getValueAt(i, 0), "Column 0 should have data");
            assertNotNull(table.getValueAt(i, 1), "Column 1 should have data");
        }
    }

    @Test
    @DisplayName("Deprecated SortHeaderRenderer can be instantiated")
    void testDeprecatedSortHeaderRenderer() {
        // This should not throw an exception
        @SuppressWarnings("deprecation")
        SortHeaderRenderer renderer = new SortHeaderRenderer();
        assertNotNull(renderer);
    }

    @Test
    @DisplayName("Deprecated SortArrowIcon can be instantiated")
    void testDeprecatedSortArrowIcon() {
        // This should not throw an exception
        @SuppressWarnings("deprecation")
        SortArrowIcon icon = new SortArrowIcon(SortArrowIcon.ASCENDING);
        assertNotNull(icon);
    }

    /**
     * Test implementation of SortTableModel
     */
    static class TestSortTableModel extends DefaultTableModel implements SortTableModel {
        private static final long serialVersionUID = 1L;

        TestSortTableModel(Vector<Vector<Object>> data, Vector<String> columnNames) {
            super(data, columnNames);
        }

        @Override
        public boolean isSortable(int col) {
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void sortColumn(int col, boolean ascending) {
            java.util.Collections.sort((Vector<Vector>) getDataVector(),
                    new ColumnComparator(col, ascending));
            fireTableDataChanged();
        }
    }

    @SuppressWarnings("rawtypes")
    static class ColumnComparator implements java.util.Comparator<Vector> {
        private final int column;
        private final boolean ascending;

        ColumnComparator(int column, boolean ascending) {
            this.column = column;
            this.ascending = ascending;
        }

        @Override
        @SuppressWarnings("unchecked")
        public int compare(Vector v1, Vector v2) {
            Object obj1 = v1.get(column);
            Object obj2 = v2.get(column);

            int result = 0;

            if (obj1 instanceof String && obj2 instanceof String) {
                result = ((String) obj1).compareToIgnoreCase((String) obj2);
            } else if (obj1 instanceof Comparable) {
                Comparable<Object> comp = (Comparable<Object>) obj1;
                result = comp.compareTo(obj2);
            }

            return ascending ? result : -result;
        }
    }
}
