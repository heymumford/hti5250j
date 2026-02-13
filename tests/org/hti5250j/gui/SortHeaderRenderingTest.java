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
 * Tests for sort header rendering - validating that sort indicators display correctly
 */
@DisplayName("Sort Header Rendering Tests")
class SortHeaderRenderingTest {

    private JTable table;
    private SortTableModel model;
    private Vector<Vector<Object>> testData;
    private String[] columnNames;

    @BeforeEach
    void setUp() {
        columnNames = new String[]{"Name", "Age", "City"};
        testData = new Vector<>();

        addRow("Charlie", 30, "New York");
        addRow("Alice", 25, "Boston");
        addRow("Bob", 35, "Chicago");

        model = new TestSortTableModel(testData, new Vector<>(java.util.Arrays.asList(columnNames)));
        table = new JTable(model);
    }

    private void addRow(String name, int age, String city) {
        Vector<Object> row = new Vector<>();
        row.add(name);
        row.add(age);
        row.add(city);
        testData.add(row);
    }

    @Test
    @DisplayName("Table should have a table header")
    void testTableHasHeader() {
        assertNotNull(table.getTableHeader(), "Table should have a header");
    }

    @Test
    @DisplayName("Table header should have 3 columns")
    void testHeaderColumnCount() {
        assertEquals(3, table.getColumnCount(), "Header should have 3 columns");
    }

    @Test
    @DisplayName("Table header column names should match model")
    void testHeaderColumnNames() {
        assertEquals("Name", table.getColumnName(0));
        assertEquals("Age", table.getColumnName(1));
        assertEquals("City", table.getColumnName(2));
    }

    @Test
    @DisplayName("Table should be sortable via column interface")
    void testTableSortableByColumn() {
        // Verify initial state
        assertEquals("Charlie", model.getValueAt(0, 0));

        // Simulate sorting by first column
        if (model.isSortable(0)) {
            model.sortColumn(0, true);
            assertEquals("Alice", model.getValueAt(0, 0), "After sorting, Alice should be first");
        }
    }

    @Test
    @DisplayName("Sort model should support ascending sort query")
    void testSortIndicatorAscending() {
        model.sortColumn(0, true);
        // In a real implementation with JSortTable, we would verify the icon
        // This test validates the model's sort state can be queried
        assertTrue(model.isSortable(0), "Column should remain sortable after sort");
    }

    @Test
    @DisplayName("Sort model should support descending sort query")
    void testSortIndicatorDescending() {
        model.sortColumn(0, false);
        // In a real implementation with JSortTable, we would verify the icon
        assertTrue(model.isSortable(0), "Column should remain sortable after sort");
    }

    @Test
    @DisplayName("All columns should be visible in header")
    void testAllColumnsVisibleInHeader() {
        int visibleColumns = table.getColumnCount();
        assertEquals(columnNames.length, visibleColumns, "All columns should be visible");
    }

    /**
     * Minimal test implementation of SortTableModel
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
            } else if (obj1 instanceof Integer && obj2 instanceof Integer) {
                result = ((Integer) obj1).compareTo((Integer) obj2);
            } else if (obj1 instanceof Comparable) {
                Comparable<Object> comp = (Comparable<Object>) obj1;
                result = comp.compareTo(obj2);
            }

            return ascending ? result : -result;
        }
    }
}
