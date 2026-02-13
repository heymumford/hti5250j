/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CCSID1026;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RED Phase - CCSID1026 Migration Compatibility Tests.
 *
 * These tests verify that CCSID1026 maintains backward compatibility
 * while migrating from static array to factory pattern.
 */
@DisplayName("CCSID1026 Migration Compatibility Tests")
class CCSID1026MigrationTest {

    @Test
    @DisplayName("CCSID1026 converts all 256 characters correctly")
    void testCCSID1026AllCharacters() {
        CCSID1026 converter = new CCSID1026();
        converter.init();

        // Test all 256 EBCDIC codes can be converted
        for (int ebcdic = 0; ebcdic < 256; ebcdic++) {
            char unicode = converter.ebcdic2uni(ebcdic);
            assertNotNull(unicode, "EBCDIC byte 0x" + Integer.toHexString(ebcdic) + " should convert");
        }
    }

    @Test
    @DisplayName("CCSID1026 space character (0x40) converts correctly")
    void testCCSID1026SpaceCharacter() {
        CCSID1026 converter = new CCSID1026();
        converter.init();

        char space = converter.ebcdic2uni(0x40);
        assertEquals(' ', space, "EBCDIC 0x40 should convert to SPACE");
    }

    @Test
    @DisplayName("CCSID1026 provides correct name")
    void testCCSID1026Name() {
        CCSID1026 converter = new CCSID1026();

        assertEquals("1026", converter.getName(), "CCSID1026 name should be '1026'");
    }

    @Test
    @DisplayName("CCSID1026 provides description")
    void testCCSID1026Description() {
        CCSID1026 converter = new CCSID1026();

        String description = converter.getDescription();
        assertNotNull(description, "CCSID1026 should provide a description");
        assertTrue(description.length() > 0, "Description should not be empty");
    }

    @Test
    @DisplayName("CCSID1026 init() returns converter for chaining")
    void testCCSID1026InitChaining() {
        CCSID1026 converter = new CCSID1026();

        Object initialized = converter.init();
        assertNotNull(initialized, "init() should return converter");
        assertSame(converter, initialized, "init() should return same converter instance");
    }

    @Test
    @DisplayName("CCSID1026 NUL character (0x00) converts correctly")
    void testCCSID1026NULCharacter() {
        CCSID1026 converter = new CCSID1026();
        converter.init();

        char nul = converter.ebcdic2uni(0x00);
        assertEquals('\u0000', nul, "EBCDIC 0x00 should convert to NUL");
    }
}
