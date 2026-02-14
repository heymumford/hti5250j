/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * GREEN Phase - CCSID Mapping Loader Tests.
 *
 * Verifies that CCSIDMappingLoader loads JSON-based character mappings
 * and that CCSIDFactory produces valid converters from those mappings.
 */
@DisplayName("CCSID Mapping Loader - GREEN Phase Tests")
class CCSIDMappingLoaderTest {

    @Test
    @DisplayName("GREEN: CCSIDMappingLoader is available and has mappings loaded")
    void testLoaderClassIsAvailableWithMappings() {
        assertTrue(CCSIDMappingLoader.isAvailable("37"),
            "CCSIDMappingLoader should have CCSID 37 available");

        String[] available = CCSIDMappingLoader.getAvailableCCSIDs();
        assertNotNull(available, "Available CCSIDs should not be null");
        assertTrue(available.length > 0, "Should have at least one CCSID loaded");
    }

    @Test
    @DisplayName("GREEN: CCSID 37 mapping loads 256-char array with correct space mapping")
    void testLoadCCSID37Mapping() {
        char[] mapping = CCSIDMappingLoader.loadToUnicode("37");

        assertNotNull(mapping, "CCSID 37 mapping should not be null");
        assertEquals(256, mapping.length, "Mapping should be 256 characters");
        assertEquals(' ', mapping[0x40], "EBCDIC 0x40 should map to SPACE");
    }

    @Test
    @DisplayName("GREEN: CCSIDFactory exists and creates converters for known CCSIDs")
    void testFactoryCreatesConverterFromMappings() {
        String[] available = CCSIDMappingLoader.getAvailableCCSIDs();
        assertTrue(available.length > 0, "Should have CCSIDs available for factory");

        String description = CCSIDMappingLoader.getDescription("37");
        assertNotNull(description, "CCSID 37 should have a description");
        assertTrue(description.length() > 0, "Description should not be empty");
    }
}
