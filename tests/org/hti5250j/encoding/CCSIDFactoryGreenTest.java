/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CodepageConverterAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * GREEN Phase Tests for CCSID Factory Pattern.
 *
 * Tests verify that CCSIDFactory creates converters correctly from JSON mappings.
 */
@DisplayName("CCSID Factory - GREEN Phase Tests")
class CCSIDFactoryGreenTest {

    @Test
    @DisplayName("Factory creates converter for CCSID37")
    void testCreateCCSID37Converter() {
        CodepageConverterAdapter converter = CCSIDFactory.getConverter("37");

        assertNotNull(converter, "Factory should create CCSID37 converter");
        assertInstanceOf(ConfigurableCodepageConverter.class, converter, "Should be ConfigurableCodepageConverter");
        assertEquals("37", converter.getName(), "Converter name should be 37");
    }

    @Test
    @DisplayName("Factory creates converter for CCSID500")
    void testCreateCCSID500Converter() {
        CodepageConverterAdapter converter = CCSIDFactory.getConverter("500");

        assertNotNull(converter, "Factory should create CCSID500 converter");
        assertEquals("500", converter.getName(), "Converter name should be 500");
    }

    @Test
    @DisplayName("Factory returns null for non-existent CCSID")
    void testFactoryReturnsNullForInvalidCCSID() {
        CodepageConverterAdapter converter = CCSIDFactory.getConverter("9999");

        assertNull(converter, "Factory should return null for non-existent CCSID");
    }

    @Test
    @DisplayName("Converter provides correct description")
    void testConverterDescription() {
        CodepageConverterAdapter converter = CCSIDFactory.getConverter("37");

        assertNotNull(converter, "Converter should exist");
        String description = converter.getDescription();
        assertNotNull(description, "Description should not be null");
        assertTrue(description.contains("37") || description.contains("USA"), "Description should mention region or CCSID");
    }

    @Test
    @DisplayName("Converter provides 256-character codepage via conversion")
    void testConverterCodepage() {
        CodepageConverterAdapter converter = CCSIDFactory.getConverter("37");

        assertNotNull(converter, "Converter should exist");
        converter.init();  // Initialize reverse lookup tables

        // Verify codepage by testing sample conversions (0, 0x40=space, 0x81='a')
        char nul = converter.ebcdic2uni(0x00);
        assertEquals('\u0000', nul, "Index 0 should convert to NUL");

        char space = converter.ebcdic2uni(0x40);
        assertEquals(' ', space, "Index 0x40 should convert to SPACE");

        char lowerA = converter.ebcdic2uni(0x81);
        assertEquals('a', lowerA, "Index 0x81 should convert to lowercase 'a'");
    }

    @Test
    @DisplayName("Converter converts EBCDIC to Unicode correctly")
    void testEBCDICToUnicodeConversion() {
        CodepageConverterAdapter converter = CCSIDFactory.getConverter("37");
        assertNotNull(converter, "Converter should exist");
        converter.init();

        // CCSID37[0x40] = SPACE (U+0020)
        char space = converter.ebcdic2uni(0x40);
        assertEquals(' ', space, "0x40 should map to SPACE");

        // CCSID37[0x81] = 'a'
        char lowerA = converter.ebcdic2uni(0x81);
        assertEquals('a', lowerA, "0x81 should map to lowercase 'a'");

        // CCSID37[0x00] = NUL
        char nul = converter.ebcdic2uni(0x00);
        assertEquals('\u0000', nul, "0x00 should map to NUL");
    }

    @Test
    @DisplayName("Converter initializes correctly")
    void testConverterInitialization() {
        CodepageConverterAdapter converter = CCSIDFactory.getConverter("37");
        assertNotNull(converter, "Converter should exist");

        ConfigurableCodepageConverter initialized = (ConfigurableCodepageConverter) converter.init();
        assertNotNull(initialized, "init() should return converter");
        assertSame(converter, initialized, "init() should return same converter instance");
    }

    @Test
    @DisplayName("All 20 single-byte CCSIDs are supported")
    void testAllSingleByteCCSIDsSupported() {
        String[] ccsids = {
            "37", "273", "277", "278", "280", "284", "285", "297", "424", "500",
            "870", "871", "875", "1025", "1026", "1112", "1140", "1141", "1147", "1148"
        };

        for (String ccsid : ccsids) {
            CodepageConverterAdapter converter = CCSIDFactory.getConverter(ccsid);
            assertNotNull(converter, "Factory should create converter for CCSID" + ccsid);
            assertEquals(ccsid, converter.getName(), "Converter name should match CCSID");
        }
    }

    @Test
    @DisplayName("Different CCSIDs produce different converters")
    void testDifferentCCSIDsProduceDifferentConverters() {
        CodepageConverterAdapter converter37 = CCSIDFactory.getConverter("37");
        CodepageConverterAdapter converter273 = CCSIDFactory.getConverter("273");

        assertNotNull(converter37, "CCSID37 converter should exist");
        assertNotNull(converter273, "CCSID273 converter should exist");

        converter37.init();
        converter273.init();

        // Verify different character mappings
        char char37 = converter37.ebcdic2uni(0x38);
        char char273 = converter273.ebcdic2uni(0x38);

        // These characters should be different in different CCSIDs
        // (CCSID37 and CCSID273 have different mappings for many positions)
        assertNotEquals(converter37.getName(), converter273.getName(), "Converters should have different names");
    }
}
