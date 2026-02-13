/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CCSID424;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RED Phase - CCSID424 Migration Compatibility Tests.
 *
 * These tests verify that CCSID424 maintains backward compatibility
 * while migrating from static array to factory pattern.
 */
@DisplayName("CCSID424 Migration Compatibility Tests")
class CCSID424MigrationTest {

    @Test
    @DisplayName("CCSID424 converts all 256 characters correctly")
    void testCCSID424AllCharacters() {
        CCSID424 converter = new CCSID424();
        converter.init();

        // Test all 256 EBCDIC codes can be converted
        for (int ebcdic = 0; ebcdic < 256; ebcdic++) {
            char unicode = converter.ebcdic2uni(ebcdic);
            assertNotNull(unicode, "EBCDIC byte 0x" + Integer.toHexString(ebcdic) + " should convert");
        }
    }

    @Test
    @DisplayName("CCSID424 space character (0x40) converts correctly")
    void testCCSID424SpaceCharacter() {
        CCSID424 converter = new CCSID424();
        converter.init();

        char space = converter.ebcdic2uni(0x40);
        assertEquals(' ', space, "EBCDIC 0x40 should convert to SPACE");
    }

    @Test
    @DisplayName("CCSID424 provides correct name")
    void testCCSID424Name() {
        CCSID424 converter = new CCSID424();

        assertEquals("424", converter.getName(), "CCSID424 name should be '424'");
    }

    @Test
    @DisplayName("CCSID424 provides description")
    void testCCSID424Description() {
        CCSID424 converter = new CCSID424();

        String description = converter.getDescription();
        assertNotNull(description, "CCSID424 should provide a description");
        assertTrue(description.length() > 0, "Description should not be empty");
    }

    @Test
    @DisplayName("CCSID424 init() returns converter for chaining")
    void testCCSID424InitChaining() {
        CCSID424 converter = new CCSID424();

        Object initialized = converter.init();
        assertNotNull(initialized, "init() should return converter");
        assertSame(converter, initialized, "init() should return same converter instance");
    }

    @Test
    @DisplayName("CCSID424 NUL character (0x00) converts correctly")
    void testCCSID424NULCharacter() {
        CCSID424 converter = new CCSID424();
        converter.init();

        char nul = converter.ebcdic2uni(0x00);
        assertEquals('\u0000', nul, "EBCDIC 0x00 should convert to NUL");
    }
}
