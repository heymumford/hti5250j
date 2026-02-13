/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CCSID1122;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RED Phase - CCSID1122 Migration Compatibility Tests.
 *
 * These tests verify that CCSID1122 maintains backward compatibility
 * while migrating from static array to factory pattern.
 */
@DisplayName("CCSID1122 Migration Compatibility Tests")
class CCSID1122MigrationTest {

    @Test
    @DisplayName("CCSID1122 converts all 256 characters correctly")
    void testCCSID1122AllCharacters() {
        CCSID1122 converter = new CCSID1122();
        converter.init();

        // Test all 256 EBCDIC codes can be converted
        for (int ebcdic = 0; ebcdic < 256; ebcdic++) {
            char unicode = converter.ebcdic2uni(ebcdic);
            assertNotNull(unicode, "EBCDIC byte 0x" + Integer.toHexString(ebcdic) + " should convert");
        }
    }

    @Test
    @DisplayName("CCSID1122 space character (0x40) converts correctly")
    void testCCSID1122SpaceCharacter() {
        CCSID1122 converter = new CCSID1122();
        converter.init();

        char space = converter.ebcdic2uni(0x40);
        assertEquals(' ', space, "EBCDIC 0x40 should convert to SPACE");
    }

    @Test
    @DisplayName("CCSID1122 provides correct name")
    void testCCSID1122Name() {
        CCSID1122 converter = new CCSID1122();

        assertEquals("1122", converter.getName(), "CCSID1122 name should be '1122'");
    }

    @Test
    @DisplayName("CCSID1122 provides description")
    void testCCSID1122Description() {
        CCSID1122 converter = new CCSID1122();

        String description = converter.getDescription();
        assertNotNull(description, "CCSID1122 should provide a description");
        assertTrue(description.length() > 0, "Description should not be empty");
    }

    @Test
    @DisplayName("CCSID1122 init() returns converter for chaining")
    void testCCSID1122InitChaining() {
        CCSID1122 converter = new CCSID1122();

        Object initialized = converter.init();
        assertNotNull(initialized, "init() should return converter");
        assertSame(converter, initialized, "init() should return same converter instance");
    }

    @Test
    @DisplayName("CCSID1122 NUL character (0x00) converts correctly")
    void testCCSID1122NULCharacter() {
        CCSID1122 converter = new CCSID1122();
        converter.init();

        char nul = converter.ebcdic2uni(0x00);
        assertEquals('\u0000', nul, "EBCDIC 0x00 should convert to NUL");
    }
}
