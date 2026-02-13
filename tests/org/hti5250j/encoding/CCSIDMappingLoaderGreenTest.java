/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * GREEN Phase Tests for CCSID Mapping Loader.
 *
 * Tests verify minimal implementation that passes core requirements.
 */
@DisplayName("CCSID Mapping Loader - GREEN Phase Tests")
class CCSIDMappingLoaderGreenTest {

    @Test
    @DisplayName("Load CCSID37 mapping returns 256-character array")
    void testLoadCCSID37Mapping() {
        char[] mapping = CCSIDMappingLoader.loadToUnicode("37");

        assertNotNull(mapping, "CCSID37 mapping should not be null");
        assertEquals(256, mapping.length, "CCSID37 mapping should have exactly 256 characters");
    }

    @Test
    @DisplayName("Load CCSID37 at index 0 returns NUL character (U+0000)")
    void testCCSID37Index0IsNul() {
        char[] mapping = CCSIDMappingLoader.loadToUnicode("37");
        assertEquals('\u0000', mapping[0], "Index 0 should be NUL (U+0000)");
    }

    @Test
    @DisplayName("Load CCSID37 at index 0x40 returns SPACE character")
    void testCCSID37Index40IsSpace() {
        char[] mapping = CCSIDMappingLoader.loadToUnicode("37");
        assertEquals(' ', mapping[0x40], "Index 0x40 should be SPACE (U+0020)");
    }

    @Test
    @DisplayName("Load CCSID37 at index 0x81 returns 'a' character")
    void testCCSID37Index81IsLowercaseA() {
        char[] mapping = CCSIDMappingLoader.loadToUnicode("37");
        assertEquals('a', mapping[0x81], "Index 0x81 should be lowercase 'a'");
    }

    @Test
    @DisplayName("Load CCSID500 mapping returns 256-character array")
    void testLoadCCSID500Mapping() {
        char[] mapping = CCSIDMappingLoader.loadToUnicode("500");

        assertNotNull(mapping, "CCSID500 mapping should not be null");
        assertEquals(256, mapping.length, "CCSID500 mapping should have exactly 256 characters");
    }

    @Test
    @DisplayName("Load CCSID273 (Austria/Germany) mapping")
    void testLoadCCSID273Mapping() {
        char[] mapping = CCSIDMappingLoader.loadToUnicode("273");
        assertNotNull(mapping);
        assertEquals(256, mapping.length);
    }

    @Test
    @DisplayName("Load all 20 single-byte CCSIDs")
    void testLoadAllCCSIDMappings() {
        String[] ccsids = {
            "37", "273", "277", "278", "280", "284", "285", "297", "424", "500",
            "870", "871", "875", "1025", "1026", "1112", "1140", "1141", "1147", "1148"
        };

        for (String ccsid : ccsids) {
            char[] mapping = CCSIDMappingLoader.loadToUnicode(ccsid);
            assertNotNull(mapping, "CCSID " + ccsid + " should be loaded");
            assertEquals(256, mapping.length, "CCSID " + ccsid + " should have 256 chars");
        }
    }

    @Test
    @DisplayName("Get description for CCSID37")
    void testGetCCSID37Description() {
        String desc = CCSIDMappingLoader.getDescription("37");
        assertNotNull(desc, "Description should not be null");
        assertTrue(desc.contains("USA"), "Description should mention USA");
        assertTrue(desc.contains("Canada"), "Description should mention Canada");
    }

    @Test
    @DisplayName("Non-existent CCSID returns null")
    void testNonExistentCCSIDReturnsNull() {
        char[] mapping = CCSIDMappingLoader.loadToUnicode("9999");
        assertNull(mapping, "Non-existent CCSID should return null");
    }

    @Test
    @DisplayName("Check CCSID availability")
    void testIsAvailable() {
        assertTrue(CCSIDMappingLoader.isAvailable("37"), "CCSID37 should be available");
        assertTrue(CCSIDMappingLoader.isAvailable("500"), "CCSID500 should be available");
        assertFalse(CCSIDMappingLoader.isAvailable("9999"), "CCSID9999 should not be available");
    }

    @Test
    @DisplayName("Get list of available CCSIDs")
    void testGetAvailableCCSIDs() {
        String[] ccsids = CCSIDMappingLoader.getAvailableCCSIDs();
        assertNotNull(ccsids, "Available CCSIDs list should not be null");
        assertEquals(20, ccsids.length, "Should have exactly 20 single-byte CCSIDs");
        assertTrue(contains(ccsids, "37"), "List should contain CCSID37");
        assertTrue(contains(ccsids, "500"), "List should contain CCSID500");
    }

    private boolean contains(String[] array, String value) {
        for (String s : array) {
            if (s.equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("CCSID273 differences from CCSID37")
    void testCCSID273DifferentFromCCSID37() {
        char[] mapping37 = CCSIDMappingLoader.loadToUnicode("37");
        char[] mapping273 = CCSIDMappingLoader.loadToUnicode("273");

        // These should be different because they map to different EBCDIC character sets
        // While some positions may be the same, there should be differences
        boolean hasDifferences = false;
        for (int i = 0; i < 256; i++) {
            if (mapping37[i] != mapping273[i]) {
                hasDifferences = true;
                break;
            }
        }
        assertTrue(hasDifferences, "CCSID273 should differ from CCSID37");
    }
}
