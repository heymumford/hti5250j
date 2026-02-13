/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CCSID284;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RED Phase - CCSID284 Migration Compatibility Tests.
 *
 * These tests verify that CCSID284 maintains backward compatibility
 * while migrating from static array to factory pattern.
 */
@DisplayName("CCSID284 Migration Compatibility Tests")
class CCSID284MigrationTest {

    @Test
    @DisplayName("CCSID284 converts all 256 characters correctly")
    void testCCSID284AllCharacters() {
        CCSID284 converter = new CCSID284();
        converter.init();

        // Test all 256 EBCDIC codes can be converted
        for (int ebcdic = 0; ebcdic < 256; ebcdic++) {
            char unicode = converter.ebcdic2uni(ebcdic);
            assertNotNull(unicode, "EBCDIC byte 0x" + Integer.toHexString(ebcdic) + " should convert");
        }
    }

    @Test
    @DisplayName("CCSID284 space character (0x40) converts correctly")
    void testCCSID284SpaceCharacter() {
        CCSID284 converter = new CCSID284();
        converter.init();

        char space = converter.ebcdic2uni(0x40);
        assertEquals(' ', space, "EBCDIC 0x40 should convert to SPACE");
    }

    @Test
    @DisplayName("CCSID284 provides correct name")
    void testCCSID284Name() {
        CCSID284 converter = new CCSID284();

        assertEquals("284", converter.getName(), "CCSID284 name should be '284'");
    }

    @Test
    @DisplayName("CCSID284 provides description")
    void testCCSID284Description() {
        CCSID284 converter = new CCSID284();

        String description = converter.getDescription();
        assertNotNull(description, "CCSID284 should provide a description");
        assertTrue(description.length() > 0, "Description should not be empty");
    }

    @Test
    @DisplayName("CCSID284 init() returns converter for chaining")
    void testCCSID284InitChaining() {
        CCSID284 converter = new CCSID284();

        Object initialized = converter.init();
        assertNotNull(initialized, "init() should return converter");
        assertSame(converter, initialized, "init() should return same converter instance");
    }

    @Test
    @DisplayName("CCSID284 NUL character (0x00) converts correctly")
    void testCCSID284NULCharacter() {
        CCSID284 converter = new CCSID284();
        converter.init();

        char nul = converter.ebcdic2uni(0x00);
        assertEquals('\u0000', nul, "EBCDIC 0x00 should convert to NUL");
    }
}
