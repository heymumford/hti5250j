/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.gui;

import javax.swing.table.TableModel;

/**
 * Interface for table models that support sorting operations.
 *
 * BACKWARD COMPATIBILITY NOTE: This interface is deprecated and maintained
 * only for backward compatibility. New code should use TableModel directly
 * with JSortTable, which handles sorting via standard TableRowSorter.
 *
 * This re-implementation uses only standard Swing APIs and is fully GPL-2.0 licensed.
 */
public interface SortTableModel extends TableModel {

    /**
     * Determine if a column can be sorted.
     *
     * @param col Column index
     * @return true if column is sortable
     */
    boolean isSortable(int col);

    /**
     * Sort the table by a specific column.
     *
     * NOTE: This method is provided for backward compatibility but may not be
     * called automatically by JSortTable (which uses TableRowSorter instead).
     * Applications should call this method explicitly if using this interface.
     *
     * @param col       Column index
     * @param ascending true for ascending sort, false for descending
     */
    void sortColumn(int col, boolean ascending);
}
