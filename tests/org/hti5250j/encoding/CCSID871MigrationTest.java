/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CCSID871;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RED Phase - CCSID871 Migration Compatibility Tests.
 *
 * These tests verify that CCSID871 maintains backward compatibility
 * while migrating from static array to factory pattern.
 */
@DisplayName("CCSID871 Migration Compatibility Tests")
class CCSID871MigrationTest {

    @Test
    @DisplayName("CCSID871 converts all 256 characters correctly")
    void testCCSID871AllCharacters() {
        CCSID871 converter = new CCSID871();
        converter.init();

        // Test all 256 EBCDIC codes can be converted
        for (int ebcdic = 0; ebcdic < 256; ebcdic++) {
            char unicode = converter.ebcdic2uni(ebcdic);
            assertNotNull(unicode, "EBCDIC byte 0x" + Integer.toHexString(ebcdic) + " should convert");
        }
    }

    @Test
    @DisplayName("CCSID871 space character (0x40) converts correctly")
    void testCCSID871SpaceCharacter() {
        CCSID871 converter = new CCSID871();
        converter.init();

        char space = converter.ebcdic2uni(0x40);
        assertEquals(' ', space, "EBCDIC 0x40 should convert to SPACE");
    }

    @Test
    @DisplayName("CCSID871 provides correct name")
    void testCCSID871Name() {
        CCSID871 converter = new CCSID871();

        assertEquals("871", converter.getName(), "CCSID871 name should be '871'");
    }

    @Test
    @DisplayName("CCSID871 provides description")
    void testCCSID871Description() {
        CCSID871 converter = new CCSID871();

        String description = converter.getDescription();
        assertNotNull(description, "CCSID871 should provide a description");
        assertTrue(description.length() > 0, "Description should not be empty");
    }

    @Test
    @DisplayName("CCSID871 init() returns converter for chaining")
    void testCCSID871InitChaining() {
        CCSID871 converter = new CCSID871();

        Object initialized = converter.init();
        assertNotNull(initialized, "init() should return converter");
        assertSame(converter, initialized, "init() should return same converter instance");
    }

    @Test
    @DisplayName("CCSID871 NUL character (0x00) converts correctly")
    void testCCSID871NULCharacter() {
        CCSID871 converter = new CCSID871();
        converter.init();

        char nul = converter.ebcdic2uni(0x00);
        assertEquals('\u0000', nul, "EBCDIC 0x00 should convert to NUL");
    }
}
