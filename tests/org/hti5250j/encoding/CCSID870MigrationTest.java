/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CCSID870;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RED Phase - CCSID870 Migration Compatibility Tests.
 *
 * These tests verify that CCSID870 maintains backward compatibility
 * while migrating from static array to factory pattern.
 */
@DisplayName("CCSID870 Migration Compatibility Tests")
class CCSID870MigrationTest {

    @Test
    @DisplayName("CCSID870 converts all 256 characters correctly")
    void testCCSID870AllCharacters() {
        CCSID870 converter = new CCSID870();
        converter.init();

        // Test all 256 EBCDIC codes can be converted
        for (int ebcdic = 0; ebcdic < 256; ebcdic++) {
            char unicode = converter.ebcdic2uni(ebcdic);
            assertNotNull(unicode, "EBCDIC byte 0x" + Integer.toHexString(ebcdic) + " should convert");
        }
    }

    @Test
    @DisplayName("CCSID870 space character (0x40) converts correctly")
    void testCCSID870SpaceCharacter() {
        CCSID870 converter = new CCSID870();
        converter.init();

        char space = converter.ebcdic2uni(0x40);
        assertEquals(' ', space, "EBCDIC 0x40 should convert to SPACE");
    }

    @Test
    @DisplayName("CCSID870 provides correct name")
    void testCCSID870Name() {
        CCSID870 converter = new CCSID870();

        assertEquals("870", converter.getName(), "CCSID870 name should be '870'");
    }

    @Test
    @DisplayName("CCSID870 provides description")
    void testCCSID870Description() {
        CCSID870 converter = new CCSID870();

        String description = converter.getDescription();
        assertNotNull(description, "CCSID870 should provide a description");
        assertTrue(description.length() > 0, "Description should not be empty");
    }

    @Test
    @DisplayName("CCSID870 init() returns converter for chaining")
    void testCCSID870InitChaining() {
        CCSID870 converter = new CCSID870();

        Object initialized = converter.init();
        assertNotNull(initialized, "init() should return converter");
        assertSame(converter, initialized, "init() should return same converter instance");
    }

    @Test
    @DisplayName("CCSID870 NUL character (0x00) converts correctly")
    void testCCSID870NULCharacter() {
        CCSID870 converter = new CCSID870();
        converter.init();

        char nul = converter.ebcdic2uni(0x00);
        assertEquals('\u0000', nul, "EBCDIC 0x00 should convert to NUL");
    }
}
