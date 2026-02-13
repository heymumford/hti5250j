/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.gui;

/**
 * DEPRECATED: This class is no longer needed.
 *
 * This class is now a no-op provided only for backward compatibility.
 * Previous implementations used custom header rendering.
 *
 * REPLACEMENT: JSortTable (which extends ModernTableSorter) now uses the
 * standard Swing default header renderer with built-in sort indicator support.
 * This renderer is automatically applied by TableRowSorter.
 *
 * NOTE: If this class appears in your code, it can be safely removed.
 * JSortTable will handle header rendering automatically.
 *
 * @deprecated Use JSortTable which automatically handles header rendering
 */
@Deprecated(since = "0.9.0", forRemoval = false)
public class SortHeaderRenderer {

    /**
     * This class is deprecated and no longer provides any functionality.
     * JSortTable uses Swing's built-in TableRowSorter which provides
     * automatic header rendering with sort indicators.
     */
    public SortHeaderRenderer() {
        // No-op constructor for backward compatibility
    }
}
