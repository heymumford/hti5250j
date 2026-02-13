/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.gui;

import java.util.*;
import javax.swing.table.*;

/**
 * Default implementation of SortTableModel using DefaultTableModel.
 *
 * This class provides backward compatibility with code expecting SortTableModel.
 * However, the sortColumn() method is primarily for backward compatibility.
 * When used with JSortTable, sorting is handled automatically by TableRowSorter.
 *
 * NOTE: This implementation uses only standard Swing APIs and encourages
 * migration to using JSortTable or ModernTableSorter directly.
 */
public class DefaultSortTableModel extends DefaultTableModel
        implements SortTableModel {

    private static final long serialVersionUID = 1L;

    public DefaultSortTableModel() {
    }

    public DefaultSortTableModel(int rows, int cols) {
        super(rows, cols);
    }

    public DefaultSortTableModel(Object[][] data, Object[] names) {
        super(data, names);
    }

    public DefaultSortTableModel(Object[] names, int rows) {
        super(names, rows);
    }

    public DefaultSortTableModel(Vector<?> names, int rows) {
        super(names, rows);
    }

    @SuppressWarnings("unchecked")
    public DefaultSortTableModel(Vector<? extends Vector> data, Vector<?> names) {
        super((Vector<Vector>) data, names);
    }

    /**
     * All columns are sortable by default.
     */
    @Override
    public boolean isSortable(int col) {
        return true;
    }

    /**
     * Sort the table data by a specific column.
     *
     * NOTE: When using JSortTable, this method may not be called automatically.
     * JSortTable uses TableRowSorter for sorting instead. This method is provided
     * for backward compatibility and direct use of this model.
     *
     * @param col       Column index
     * @param ascending true for ascending sort
     */
    @Override
    @SuppressWarnings("unchecked")
    public void sortColumn(int col, boolean ascending) {
        Collections.sort((Vector<Vector>) getDataVector(),
                new ColumnComparator(col, ascending));
        fireTableDataChanged();
    }

    /**
     * Comparator for sorting table rows by column.
     * Handles String (case-insensitive) and Comparable types.
     */
    @SuppressWarnings("rawtypes")
    private static class ColumnComparator implements Comparator<Vector> {
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
            } else if (obj1 instanceof Number && obj2 instanceof Number) {
                result = Double.compare(
                        ((Number) obj1).doubleValue(),
                        ((Number) obj2).doubleValue());
            } else if (obj1 instanceof Comparable) {
                result = ((Comparable) obj1).compareTo(obj2);
            }

            return ascending ? result : -result;
        }
    }
}
