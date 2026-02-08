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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.hti5250j.encoding.CharMappings;
import org.hti5250j.encoding.ICodePage;
import org.hti5250j.encoding.builtin.CCSID285;

/**
 * Testing the correctness of {@link CCSID285Ex} and comparing with existing implementation.
 *
 * @author master_jaf
 */
public class CCSID285Test {

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
    public void testOldConverter285() {

        ICodePage cp = CharMappings.getCodePage("285");
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
    public void testNewConverter285() {
        CCSID285 cp = new CCSID285();
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
        final ICodePage cp = CharMappings.getCodePage("285");
        final CCSID285 cpex = new CCSID285();
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

}
