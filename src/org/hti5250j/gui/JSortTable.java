/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.gui;

import javax.swing.table.TableModel;

/**
 * Backward-compatible replacement for the original JSortTable class.
 * This implementation uses standard Java Swing components (TableRowSorter)
 * instead of custom code from unlicensed sources.
 *
 * DEPRECATION NOTICE: Use ModernTableSorter directly for new code.
 * This class exists only for backward compatibility with existing code
 * that references JSortTable.
 *
 * The original JSortTable contained code from a JavaPro magazine article
 * used without permission. This replacement provides equivalent functionality
 * using only standard Swing APIs available since Java 6.
 */
public class JSortTable extends ModernTableSorter {

    private static final long serialVersionUID = 1L;

    /**
     * Create a JSortTable with a SortTableModel.
     *
     * @param model A SortTableModel instance
     */
    public JSortTable(SortTableModel model) {
        super(model);
    }

    /**
     * Create a JSortTable with a generic TableModel.
     *
     * @param model A TableModel instance
     */
    public JSortTable(TableModel model) {
        super(model);
    }

    /**
     * Get the index of the currently sorted column.
     * Returns -1 if table is unsorted.
     *
     * @return Column index or -1
     */
    int getSortedColumnIndex() {
        var sortKeys = getRowSorter().getSortKeys();
        if (sortKeys.isEmpty()) {
            return -1;
        }
        return sortKeys.get(0).getColumn();
    }

    /**
     * Check if the current sort is ascending.
     *
     * @return true if ascending, false if descending
     */
    boolean isSortedColumnAscending() {
        var sortKeys = getRowSorter().getSortKeys();
        if (sortKeys.isEmpty()) {
            return true;
        }
        return sortKeys.get(0).getSortOrder() == javax.swing.SortOrder.ASCENDING;
    }
}
