/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.gui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import javax.swing.table.DefaultTableModel;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RED phase tests for table sorting functionality
 * These tests establish baseline behavior for column sorting, ascending/descending,
 * and header rendering with sort indicators.
 */
@DisplayName("Table Sorting Behavior Tests (RED Phase - Baseline)")
class TableSortingBehaviorTest {

    private SortTableModel testModel;
    private Vector<Vector<Object>> testData;
    private String[] columnNames;

    @BeforeEach
    void setUp() {
        // Create test data: 3 columns (Name, Age, City)
        columnNames = new String[]{"Name", "Age", "City"};
        testData = new Vector<>();

        // Add test rows
        addRow("Charlie", 30, "New York");
        addRow("Alice", 25, "Boston");
        addRow("Bob", 35, "Chicago");

        // Create test model
        testModel = new TestSortTableModel(testData, new Vector<>(java.util.Arrays.asList(columnNames)));
    }

    private void addRow(String name, int age, String city) {
        Vector<Object> row = new Vector<>();
        row.add(name);
        row.add(age);
        row.add(city);
        testData.add(row);
    }

    @Test
    @DisplayName("Column should be sortable when isSortable returns true")
    void testColumnSortable() {
        assertTrue(testModel.isSortable(0), "Column 0 should be sortable");
        assertTrue(testModel.isSortable(1), "Column 1 should be sortable");
        assertTrue(testModel.isSortable(2), "Column 2 should be sortable");
    }

    @Test
    @DisplayName("Data should be sorted ascending when sortColumn called with ascending=true")
    void testSortColumnAscending() {
        // Initial order: Charlie, Alice, Bob
        assertEquals("Charlie", testModel.getValueAt(0, 0));
        assertEquals("Alice", testModel.getValueAt(1, 0));
        assertEquals("Bob", testModel.getValueAt(2, 0));

        // Sort by name ascending
        testModel.sortColumn(0, true);

        // Expected order: Alice, Bob, Charlie
        assertEquals("Alice", testModel.getValueAt(0, 0), "First row after ascending sort should be Alice");
        assertEquals("Bob", testModel.getValueAt(1, 0), "Second row after ascending sort should be Bob");
        assertEquals("Charlie", testModel.getValueAt(2, 0), "Third row after ascending sort should be Charlie");
    }

    @Test
    @DisplayName("Data should be sorted descending when sortColumn called with ascending=false")
    void testSortColumnDescending() {
        // Sort by name descending
        testModel.sortColumn(0, false);

        // Expected order: Charlie, Bob, Alice
        assertEquals("Charlie", testModel.getValueAt(0, 0), "First row after descending sort should be Charlie");
        assertEquals("Bob", testModel.getValueAt(1, 0), "Second row after descending sort should be Bob");
        assertEquals("Alice", testModel.getValueAt(2, 0), "Third row after descending sort should be Alice");
    }

    @Test
    @DisplayName("Integer column should sort numerically in ascending order")
    void testNumericSortAscending() {
        // Initial order: 30, 25, 35
        assertEquals(30, testModel.getValueAt(0, 1));
        assertEquals(25, testModel.getValueAt(1, 1));
        assertEquals(35, testModel.getValueAt(2, 1));

        // Sort by age ascending
        testModel.sortColumn(1, true);

        // Expected order: 25, 30, 35
        assertEquals(25, testModel.getValueAt(0, 1), "First row after numeric ascending sort should be 25");
        assertEquals(30, testModel.getValueAt(1, 1), "Second row after numeric ascending sort should be 30");
        assertEquals(35, testModel.getValueAt(2, 1), "Third row after numeric ascending sort should be 35");
    }

    @Test
    @DisplayName("Integer column should sort numerically in descending order")
    void testNumericSortDescending() {
        // Sort by age descending
        testModel.sortColumn(1, false);

        // Expected order: 35, 30, 25
        assertEquals(35, testModel.getValueAt(0, 1), "First row after numeric descending sort should be 35");
        assertEquals(30, testModel.getValueAt(1, 1), "Second row after numeric descending sort should be 30");
        assertEquals(25, testModel.getValueAt(2, 1), "Third row after numeric descending sort should be 25");
    }

    @Test
    @DisplayName("Should preserve row data integrity when sorting (all columns)")
    void testSortPreservesRowIntegrity() {
        // Sort by name
        testModel.sortColumn(0, true);

        // Verify Alice's row data is intact
        assertEquals("Alice", testModel.getValueAt(0, 0));
        assertEquals(25, testModel.getValueAt(0, 1));
        assertEquals("Boston", testModel.getValueAt(0, 2));

        // Verify Bob's row data is intact
        assertEquals("Bob", testModel.getValueAt(1, 0));
        assertEquals(35, testModel.getValueAt(1, 1));
        assertEquals("Chicago", testModel.getValueAt(1, 2));

        // Verify Charlie's row data is intact
        assertEquals("Charlie", testModel.getValueAt(2, 0));
        assertEquals(30, testModel.getValueAt(2, 1));
        assertEquals("New York", testModel.getValueAt(2, 2));
    }

    @Test
    @DisplayName("Should handle multiple sorts in sequence")
    void testMultipleSorts() {
        // First sort by name ascending
        testModel.sortColumn(0, true);
        assertEquals("Alice", testModel.getValueAt(0, 0));

        // Then sort by age descending
        testModel.sortColumn(1, false);
        assertEquals(35, testModel.getValueAt(0, 1), "After sorting by age descending, first should be 35");

        // Then sort back by name
        testModel.sortColumn(0, true);
        assertEquals("Alice", testModel.getValueAt(0, 0), "After sorting back by name, first should be Alice");
    }

    @Test
    @DisplayName("Should handle case-insensitive string sorting")
    void testCaseInsensitiveSorting() {
        // Add mixed case names
        testData.clear();
        addRow("alice", 25, "Boston");
        addRow("CHARLIE", 30, "New York");
        addRow("Bob", 35, "Chicago");

        // Sort by name ascending (should ignore case)
        testModel.sortColumn(0, true);

        // Should be in order: alice, Bob, CHARLIE
        String first = testModel.getValueAt(0, 0).toString().toLowerCase();
        String second = testModel.getValueAt(1, 0).toString().toLowerCase();
        String third = testModel.getValueAt(2, 0).toString().toLowerCase();

        assertTrue(first.compareTo(second) <= 0, "First should be <= second");
        assertTrue(second.compareTo(third) <= 0, "Second should be <= third");
    }

    /**
     * Simple test implementation of SortTableModel for testing.
     * Extends DefaultTableModel to provide sorting capability.
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

    /**
     * Comparator for sorting table rows by a specific column.
     * Handles both string (case-insensitive) and numeric comparisons.
     */
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
            } else if (obj1 instanceof Number && obj2 instanceof Number) {
                result = Double.compare(((Number) obj1).doubleValue(), ((Number) obj2).doubleValue());
            } else if (obj1 instanceof Comparable) {
                Comparable<Object> comp = (Comparable<Object>) obj1;
                result = comp.compareTo(obj2);
            }

            return ascending ? result : -result;
        }
    }
}
