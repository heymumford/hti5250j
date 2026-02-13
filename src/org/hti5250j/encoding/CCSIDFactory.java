/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CodepageConverterAdapter;

/**
 * Factory for creating CCSID converter instances.
 *
 * Phase 2: Factory pattern implementation for dynamic CCSID converter instantiation.
 * Replaces hardcoded if-statements in BuiltInCodePageFactory with data-driven
 * configuration from ccsid-mappings.json.
 */
public class CCSIDFactory {

    /**
     * Create a converter for the specified CCSID.
     *
     * @param ccsidId CCSID identifier (e.g., "37", "273", "500")
     * @return ConfigurableCodepageConverter for the CCSID, or null if not available
     */
    public static CodepageConverterAdapter getConverter(String ccsidId) {
        // Verify CCSID is available
        if (!CCSIDMappingLoader.isAvailable(ccsidId)) {
            return null;
        }

        // Create and return converter with JSON-loaded mappings
        return new ConfigurableCodepageConverter(ccsidId);
    }
}
