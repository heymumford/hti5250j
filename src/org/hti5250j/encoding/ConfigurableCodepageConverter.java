/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CodepageConverterAdapter;

/**
 * Configurable CCSID converter using JSON-loaded character mappings.
 *
 * Phase 2: Replaces individual CCSID*.java classes by dynamically loading
 * character mappings from ccsid-mappings.json at runtime.
 *
 * This class extends CodepageConverterAdapter and implements the getCodePage()
 * method to return mappings loaded by CCSIDMappingLoader.
 */
public class ConfigurableCodepageConverter extends CodepageConverterAdapter {

    private final String ccsidId;
    private final char[] codepage;
    private final String description;

    /**
     * Create a converter for the specified CCSID.
     *
     * @param ccsidId CCSID identifier (e.g., "37", "273", "500")
     * @throws RuntimeException if CCSID mappings cannot be loaded
     */
    public ConfigurableCodepageConverter(String ccsidId) {
        this.ccsidId = ccsidId;
        this.codepage = CCSIDMappingLoader.loadToUnicode(ccsidId);
        this.description = CCSIDMappingLoader.getDescription(ccsidId);

        if (this.codepage == null) {
            throw new RuntimeException("CCSID " + ccsidId + " mappings not found");
        }
    }

    @Override
    protected char[] getCodePage() {
        return codepage;
    }

    @Override
    public String getName() {
        return ccsidId;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public ConfigurableCodepageConverter init() {
        super.init();
        return this;
    }
}
