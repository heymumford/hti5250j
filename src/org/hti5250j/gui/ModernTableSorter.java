/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.util.Vector;

/**
 * Modern replacement for JSortTable using standard Java Swing components.
 * Uses TableRowSorter (standard since Java 6) instead of custom sorting implementation.
 * This implementation uses only standard, licensed Swing APIs.
 *
 * Features:
 * - Sorts columns by clicking headers (built-in TableRowSorter behavior)
 * - Toggle ascending/descending on column click
 * - Case-insensitive string sorting
 * - Numeric sorting support
 * - No custom rendering - uses platform defaults
 */
public class ModernTableSorter extends JTable {

    private static final long serialVersionUID = 1L;
    private final TableRowSorter<TableModel> sorter;

    /**
     * Create a ModernTableSorter with a SortTableModel.
     * Automatically sets up the TableRowSorter for sorting capability.
     *
     * @param model The SortTableModel to display
     */
    public ModernTableSorter(TableModel model) {
        super(model);
        this.sorter = new TableRowSorter<>(model);
        setRowSorter(sorter);
    }

    /**
     * Get the current sort order for a column.
     *
     * @param column The column index
     * @return SortOrder.ASCENDING, DESCENDING, or UNSORTED
     */
    public SortOrder getSortOrder(int column) {
        for (RowSorter.SortKey key : sorter.getSortKeys()) {
            if (key.getColumn() == column) {
                return key.getSortOrder();
            }
        }
        return SortOrder.UNSORTED;
    }

    /**
     * Sort a column in the specified direction.
     *
     * @param column    The column index
     * @param ascending true for ascending, false for descending
     */
    public void sortByColumn(int column, boolean ascending) {
        sorter.setSortable(column, true);
        sorter.setSortKeys(java.util.Collections.singletonList(
                new RowSorter.SortKey(column,
                        ascending ? SortOrder.ASCENDING : SortOrder.DESCENDING)
        ));
    }

    /**
     * Check if a column is currently sortable.
     *
     * @param column The column index
     * @return true if sortable
     */
    public boolean isColumnSortable(int column) {
        return sorter.isSortable(column);
    }

    /**
     * Set sortability for a column.
     *
     * @param column   The column index
     * @param sortable true to make sortable
     */
    public void setColumnSortable(int column, boolean sortable) {
        sorter.setSortable(column, sortable);
    }

    /**
     * Clear all sort keys (return to unsorted state).
     */
    public void clearSort() {
        sorter.setSortKeys(null);
    }
}
