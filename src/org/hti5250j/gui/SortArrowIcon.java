/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.gui;

/**
 * DEPRECATED: This class is no longer needed.
 *
 * The original SortArrowIcon was a custom icon renderer derived from
 * unlicensed JavaPro magazine code. This class is now a no-op provided
 * only for backward compatibility.
 *
 * REPLACEMENT: TableRowSorter (used by JSortTable) automatically manages
 * sort indicators in table headers using platform-native rendering.
 *
 * NOTE: If this class appears in your code, it can be safely removed.
 * Sort indicators are now provided automatically by the Swing framework.
 *
 * @deprecated Use JSortTable which automatically provides sort indicators
 */
@Deprecated(since = "0.9.0", forRemoval = false)
public class SortArrowIcon {

    /**
     * Constants maintained for backward compatibility but no longer used.
     */
    public static final int NONE = 0;
    public static final int DECENDING = 1;  // Note: typo preserved for compatibility
    public static final int ASCENDING = 2;

    /**
     * This class is deprecated and no longer provides any functionality.
     * TableRowSorter provides automatic sort indicators without custom icons.
     */
    public SortArrowIcon(int direction) {
        // No-op constructor for backward compatibility
    }

    /**
     * @deprecated Not used - icons are managed by TableRowSorter
     */
    @Deprecated
    public int getIconWidth() {
        return 0;
    }

    /**
     * @deprecated Not used - icons are managed by TableRowSorter
     */
    @Deprecated
    public int getIconHeight() {
        return 0;
    }

    /**
     * @deprecated Not used - icons are managed by TableRowSorter
     */
    @Deprecated
    public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
        // No-op
    }
}
