/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CCSID500;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RED Phase - CCSID500 Migration Compatibility Tests.
 *
 * These tests verify that CCSID500 maintains backward compatibility
 * while migrating from static array to factory pattern.
 */
@DisplayName("CCSID500 Migration Compatibility Tests")
class CCSID500MigrationTest {

    @Test
    @DisplayName("CCSID500 converts all 256 characters correctly")
    void testCCSID500AllCharacters() {
        CCSID500 converter = new CCSID500();
        converter.init();

        // Test all 256 EBCDIC codes can be converted
        for (int ebcdic = 0; ebcdic < 256; ebcdic++) {
            char unicode = converter.ebcdic2uni(ebcdic);
            assertNotNull(unicode, "EBCDIC byte 0x" + Integer.toHexString(ebcdic) + " should convert");
        }
    }

    @Test
    @DisplayName("CCSID500 space character (0x40) converts correctly")
    void testCCSID500SpaceCharacter() {
        CCSID500 converter = new CCSID500();
        converter.init();

        char space = converter.ebcdic2uni(0x40);
        assertEquals(' ', space, "EBCDIC 0x40 should convert to SPACE");
    }

    @Test
    @DisplayName("CCSID500 provides correct name")
    void testCCSID500Name() {
        CCSID500 converter = new CCSID500();

        assertEquals("500", converter.getName(), "CCSID500 name should be '500'");
    }

    @Test
    @DisplayName("CCSID500 provides description")
    void testCCSID500Description() {
        CCSID500 converter = new CCSID500();

        String description = converter.getDescription();
        assertNotNull(description, "CCSID500 should provide a description");
        assertTrue(description.length() > 0, "Description should not be empty");
    }

    @Test
    @DisplayName("CCSID500 init() returns converter for chaining")
    void testCCSID500InitChaining() {
        CCSID500 converter = new CCSID500();

        Object initialized = converter.init();
        assertNotNull(initialized, "init() should return converter");
        assertSame(converter, initialized, "init() should return same converter instance");
    }

    @Test
    @DisplayName("CCSID500 NUL character (0x00) converts correctly")
    void testCCSID500NULCharacter() {
        CCSID500 converter = new CCSID500();
        converter.init();

        char nul = converter.ebcdic2uni(0x00);
        assertEquals('\u0000', nul, "EBCDIC 0x00 should convert to NUL");
    }
}
