/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.gui;

import java.util.*;

/**
 * Generic column comparator for sorting table rows.
 * This class is used to compare Vector rows by a specific column index.
 *
 * NOTE: This class is provided for backward compatibility with custom
 * SortTableModel implementations. For new code, rely on TableRowSorter
 * (used by ModernTableSorter/JSortTable) which handles sorting automatically.
 *
 * IMPLEMENTATION NOTE: The original ColumnComparator was derived from
 * unlicensed JavaPro magazine code. This version is a re-implementation
 * using only standard Java APIs.
 */
@SuppressWarnings("rawtypes")
public class ColumnComparator implements Comparator<Vector> {
    protected int index;
    protected boolean ascending;

    /**
     * Create a comparator for sorting by a specific column.
     *
     * @param index     The column index to sort by
     * @param ascending true for ascending sort, false for descending
     */
    public ColumnComparator(int index, boolean ascending) {
        this.index = index;
        this.ascending = ascending;
    }

    /**
     * Compare two Vector objects by the specified column.
     *
     * @param one First Vector to compare
     * @param two Second Vector to compare
     * @return Comparison result suitable for Collections.sort()
     */
    @Override
    @SuppressWarnings("unchecked")
    public int compare(Vector one, Vector two) {
        if (one == null || two == null) {
            return 0;
        }

        if (index < 0 || index >= one.size() || index >= two.size()) {
            return 0;
        }

        Object oOne = one.get(index);
        Object oTwo = two.get(index);

        int result = compareObjects(oOne, oTwo);
        return ascending ? result : -result;
    }

    /**
     * Compare two objects using appropriate comparison method.
     * Handles Comparable types, Strings (case-insensitive), and Numbers.
     *
     * @param oOne First object
     * @param oTwo Second object
     * @return Comparison result
     */
    private int compareObjects(Object oOne, Object oTwo) {
        if (oOne == null && oTwo == null) {
            return 0;
        }
        if (oOne == null) {
            return -1;
        }
        if (oTwo == null) {
            return 1;
        }

        // String comparison (case-insensitive)
        if (oOne instanceof String && oTwo instanceof String) {
            return ((String) oOne).compareToIgnoreCase((String) oTwo);
        }

        // Numeric comparison
        if (oOne instanceof Number && oTwo instanceof Number) {
            return Double.compare(
                    ((Number) oOne).doubleValue(),
                    ((Number) oTwo).doubleValue()
            );
        }

        // Generic Comparable comparison
        if (oOne instanceof Comparable) {
            try {
                @SuppressWarnings("unchecked")
                Comparable<Object> comp = (Comparable<Object>) oOne;
                return comp.compareTo(oTwo);
            } catch (ClassCastException | NullPointerException e) {
                return 0;
            }
        }

        return 0;
    }
}
