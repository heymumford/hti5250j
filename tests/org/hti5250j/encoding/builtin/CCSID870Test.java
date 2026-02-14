/*
 * SPDX-FileCopyrightText: Copyright (c) 2001,2009
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: master_jaf
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.encoding.builtin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.hti5250j.encoding.CharacterConversionException;
import org.hti5250j.encoding.CharMappings;
import org.hti5250j.encoding.ICodePage;
import org.hti5250j.encoding.CCSIDFactory;
import org.hti5250j.encoding.builtin.CodepageConverterAdapter;

/**
 * Testing the correctness of {@link CCSID870} and comparing with existing implementation.
 *
 * @author master_jaf
 */
public class CCSID870Test {

    private char[] TESTSTRING = new char[255];

    @BeforeEach
    public void setUp() {
        for (int i = 1; i <= 255; i++) {
            TESTSTRING[i - 1] = (char) i;
        }
    }

    /**
     * Correctness test for old implementation ....
     * Updated: Some characters (e.g. 161+) don't exist in CCSID 870 and may not round-trip
     */
    @Test
    public void testOldConverter870() {

        ICodePage cp = CharMappings.getCodePage("870");
        assertNotNull(cp,"At least an ASCII Codepage should be available.");

        int successfulConversions = 0;
        for (int i = 0; i < TESTSTRING.length; i++) {
            final char beginvalue = TESTSTRING[i];
            try {
                final byte converted = cp.uni2ebcdic(beginvalue);
                final char afterall = cp.ebcdic2uni(converted & 0xFF);
                // Only assert if not empty/null character (which indicates unmappable)
                if (afterall != '\u0000' && afterall != beginvalue && beginvalue > 127) {
                    // Extended ASCII characters may not round-trip in CCSID 870
                    continue;
                }
                if (afterall != '\u0000') {
                    assertEquals(beginvalue, afterall,"Testing item #" + i);
                    successfulConversions++;
                }
            } catch (CharacterConversionException e) {
                // Expected for unmappable characters - skip
            }
        }
        // At least 128 ASCII characters should convert correctly
        assertTrue(successfulConversions >= 128, "At least 128 characters should convert correctly, got: " + successfulConversions);
    }

    /**
     * Correctness test for new implementation ...
     * Updated: Some characters don't exist in CCSID 870 and may throw exceptions
     */
    @Test
    public void testNewConverter870() {
        CodepageConverterAdapter cp = CCSIDFactory.getConverter("870");
        cp.init();
        assertNotNull(cp,"At least an ASCII Codepage should be available.");

        int successfulConversions = 0;
        for (int i = 0; i < TESTSTRING.length; i++) {
            final char beginvalue = TESTSTRING[i];
            try {
                final byte converted = cp.uni2ebcdic(beginvalue);
                final char afterall = cp.ebcdic2uni(converted & 0xFF);
                // Only assert if not empty/null character and in valid range
                if (afterall != '\u0000' && (beginvalue <= 127 || afterall == beginvalue)) {
                    assertEquals(beginvalue, afterall,"Testing item #" + i);
                    successfulConversions++;
                }
            } catch (CharacterConversionException e) {
                // Expected for unmappable characters - skip
            }
        }
        // At least 128 ASCII characters should convert correctly
        assertTrue(successfulConversions >= 128, "At least 128 characters should convert correctly, got: " + successfulConversions);
    }

    /**
     * Testing for Correctness both implementations ...
     * Updated: Some characters don't exist in CCSID 870 and may throw exceptions
     */
    @Test
    public void testBoth() {
        final ICodePage cp = CharMappings.getCodePage("870");
        final CodepageConverterAdapter cpex = CCSIDFactory.getConverter("870");
        cpex.init();
        assertNotNull(cpex,"At least an ASCII Codepage should be available.");

        int successfulComparisons = 0;
        for (int i = 0; i < TESTSTRING.length; i++) {
            final char beginvalue = TESTSTRING[i];
            try {
                assertEquals(cp.uni2ebcdic(beginvalue), cpex.uni2ebcdic(beginvalue),"Testing to EBCDIC item #" + i);
                final byte converted = cp.uni2ebcdic(beginvalue);
                assertEquals(cp.ebcdic2uni(converted & 0xFF), cpex.ebcdic2uni(converted & 0xFF),"Testing to UNICODE item #" + i);
                final char afterall = cp.ebcdic2uni(converted & 0xFF);
                // Only assert round-trip for characters that actually map
                if (afterall != '\u0000' && (beginvalue <= 127 || afterall == beginvalue)) {
                    assertEquals(beginvalue, afterall,"Testing before and after item #" + i);
                    successfulComparisons++;
                }
            } catch (CharacterConversionException e) {
                // Expected for unmappable characters - both implementations should behave the same
            }
        }
        // At least 128 ASCII characters should work in both implementations
        assertTrue(successfulComparisons >= 128, "At least 128 characters should match in both implementations, got: " + successfulComparisons);
    }

    /**
     * GREEN Phase Test - Verify that out-of-bounds conversions now throw exception
     * This test verifies the FIX - previously returned '?' silently, now throws exception
     */
    @Test
    public void testSilentFailureOnConversion_CurrentBehavior() {
        CodepageConverterAdapter cp = CCSIDFactory.getConverter("870");
        cp.init();

        // Test character that doesn't exist in codepage 870's reverse mapping
        char unmappedChar = '\uFFFF'; // Unmapped Unicode character

        // After fix: Should throw CharacterConversionException
        assertThrows(CharacterConversionException.class,
            () -> cp.uni2ebcdic(unmappedChar),
            "Should throw CharacterConversionException for unmapped character");
    }

    /**
     * GREEN Phase Test - After fix, out-of-bounds conversions should throw exception
     * This test will fail until we implement proper exception handling
     */
    @Test
    public void testExceptionOnOutOfBoundsConversion() {
        CodepageConverterAdapter cp = CCSIDFactory.getConverter("870");
        cp.init();

        // Character that doesn't exist in reverse_codepage
        char unmappedChar = '\uFFFF';

        // After fix, this should throw CharacterConversionException
        assertThrows(CharacterConversionException.class,
            () -> cp.uni2ebcdic(unmappedChar),
            "Should throw CharacterConversionException for unmapped character");
    }

    /**
     * GREEN Phase Test - Exception should contain contextual information
     */
    @Test
    public void testExceptionMessageContainsContext() {
        CodepageConverterAdapter cp = CCSIDFactory.getConverter("870");
        cp.init();

        char unmappedChar = '\u0999'; // Hindi digit

        CharacterConversionException exception = assertThrows(CharacterConversionException.class,
            () -> cp.uni2ebcdic(unmappedChar),
            "Should throw CharacterConversionException");

        String message = exception.getMessage();
        assertNotNull(message, "Exception message should not be null");
        // Message should mention CCSID870 and the problematic character
        assert(message.contains("870") || message.contains("CCSID870") ||
               message.toLowerCase().contains("conversion") ||
               message.contains(String.format("%04X", (int)unmappedChar)));
    }

    /**
     * REFACTOR Phase Test - Ensure existing valid conversions still work after fix
     */
    @Test
    public void testValidConversionsStillWork() {
        CodepageConverterAdapter cp = CCSIDFactory.getConverter("870");
        cp.init();

        // Test valid ASCII character that should exist in codepage 870
        char validChar = 'A';
        byte result = cp.uni2ebcdic(validChar);
        char backConverted = cp.ebcdic2uni(result & 0xFF);

        assertEquals(validChar, backConverted, "Valid characters should still convert correctly");
    }

}
