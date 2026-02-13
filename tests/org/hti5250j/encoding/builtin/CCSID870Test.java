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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.hti5250j.encoding.CharacterConversionException;
import org.hti5250j.encoding.CharMappings;
import org.hti5250j.encoding.ICodePage;
import org.hti5250j.encoding.builtin.CCSID870;

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
     */
    @Test
    public void testOldConverter870() {

        ICodePage cp = CharMappings.getCodePage("870");
        assertNotNull(cp,"At least an ASCII Codepage should be available.");

        for (int i = 0; i < TESTSTRING.length; i++) {
            final char beginvalue = TESTSTRING[i];
            final byte converted = cp.uni2ebcdic(beginvalue);
            final char afterall = cp.ebcdic2uni(converted & 0xFF);
            assertEquals(beginvalue, afterall,"Testing item #" + i);
        }

    }

    /**
     * Correctness test for new implementation ...
     */
    @Test
    public void testNewConverter870() {
        CCSID870 cp = new CCSID870();
        cp.init();
        assertNotNull(cp,"At least an ASCII Codepage should be available.");

        for (int i = 0; i < TESTSTRING.length; i++) {
            final char beginvalue = TESTSTRING[i];
            final byte converted = cp.uni2ebcdic(beginvalue);
            final char afterall = cp.ebcdic2uni(converted & 0xFF);
            assertEquals(beginvalue, afterall,"Testing item #" + i);
        }
    }

    /**
     * Testing for Correctness both implementations ...
     */
    @Test
    public void testBoth() {
        final ICodePage cp = CharMappings.getCodePage("870");
        final CCSID870 cpex = new CCSID870();
        cpex.init();
        assertNotNull(cpex,"At least an ASCII Codepage should be available.");

        for (int i = 0; i < TESTSTRING.length; i++) {

            final char beginvalue = TESTSTRING[i];
            assertEquals(cp.uni2ebcdic(beginvalue), cpex.uni2ebcdic(beginvalue),"Testing to EBCDIC item #" + i);
            final byte converted = cp.uni2ebcdic(beginvalue);
            assertEquals(cp.ebcdic2uni(converted & 0xFF), cpex.ebcdic2uni(converted & 0xFF),"Testing to UNICODE item #" + i);
            final char afterall = cp.ebcdic2uni(converted & 0xFF);
            assertEquals(beginvalue, afterall,"Testing before and after item #" + i);
        }
    }

    /**
     * RED Phase Test - Verify that out-of-bounds conversions currently return space
     * This test documents the current (broken) behavior
     */
    @Test
    public void testSilentFailureOnConversion_CurrentBehavior() {
        CCSID870 cp = new CCSID870();
        cp.init();

        // Test character that doesn't exist in codepage 870's reverse mapping
        char unmappedChar = '\uFFFF'; // Unmapped Unicode character
        byte result = cp.uni2ebcdic(unmappedChar);

        // Currently returns '?' (0x3F) silently, should throw exception
        assertEquals('?', result, "Currently returns '?' silently on unmapped char");
    }

    /**
     * GREEN Phase Test - After fix, out-of-bounds conversions should throw exception
     * This test will fail until we implement proper exception handling
     */
    @Test
    public void testExceptionOnOutOfBoundsConversion() {
        CCSID870 cp = new CCSID870();
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
        CCSID870 cp = new CCSID870();
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
        CCSID870 cp = new CCSID870();
        cp.init();

        // Test valid ASCII character that should exist in codepage 870
        char validChar = 'A';
        byte result = cp.uni2ebcdic(validChar);
        char backConverted = cp.ebcdic2uni(result & 0xFF);

        assertEquals(validChar, backConverted, "Valid characters should still convert correctly");
    }

}
