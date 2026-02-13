/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CodepageConverterAdapter;

/**
 * Factory for creating CCSID converter instances.
 *
 * Uses a factory pattern for dynamic CCSID converter instantiation,
 * loading character mappings from ccsid-mappings.json instead of
 * hardcoded if-statements.
 */
public class CCSIDFactory {

    /**
     * Create a converter for the specified CCSID.
     *
     * @param ccsidId CCSID identifier (e.g., "37", "273", "500")
     * @return ConfigurableCodepageConverter for the CCSID, or null if not available
     */
    public static CodepageConverterAdapter getConverter(String ccsidId) {
        if (!CCSIDMappingLoader.isAvailable(ccsidId)) {
            return null;
        }

        return new ConfigurableCodepageConverter(ccsidId);
    }
}
