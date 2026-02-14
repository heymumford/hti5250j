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
import org.hti5250j.encoding.CCSIDFactory;
import org.hti5250j.encoding.builtin.CodepageConverterAdapter;

/**
 * Testing the correctness of {@link CCSID500Ex} and comparing with existing implementation.
 *
 * @author master_jaf
 */
public class CCSID500Test {

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
    public void testOldConverter500() {

        ICodePage cp = CharMappings.getCodePage("500-ch");
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
    public void testNewConverter500() {
        CodepageConverterAdapter cp = CCSIDFactory.getConverter("500");
        cp.init();
        assertNotNull(cp,"At least an ASCII Codepage should be available.");

        for (int i = 0; i < TESTSTRING.length; i++) {
            final char beginvalue = TESTSTRING[i];
            final byte converted = cp.uni2ebcdic(beginvalue);
            final char afterall = cp.ebcdic2uni(converted & 0xFF);
            assertEquals(beginvalue, afterall,"Testing item #" + i);
        }
    }

}
