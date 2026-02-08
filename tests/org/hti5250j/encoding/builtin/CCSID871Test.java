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

import org.junit.jupiter.api.Test;
import org.hti5250j.encoding.CharMappings;
import org.hti5250j.encoding.ICodePage;
import org.hti5250j.encoding.builtin.CCSID871;

/**
 * Testing the correctness of {@link CCSID871} and comparing with existing implementation.
 *
 * @author master_jaf
 */
public class CCSID871Test {

    /**
     * Correctness test for old implementation ....
     * Testing byte -> Unicode -> byte
     */
    @Test
    public void testOldConverter871() {

        ICodePage cp = CharMappings.getCodePage("871");
        assertNotNull(cp,"At least an ASCII Codepage should be available.");

        for (int i = 0; i < 256; i++) {
            final byte beginvalue = (byte) i;
            final char converted = cp.ebcdic2uni(beginvalue);
            final byte afterall = cp.uni2ebcdic(converted);
            assertEquals(beginvalue, afterall,"Testing item #" + i);
        }

    }

    /**
     * Correctness test for new implementation ...
     * Testing byte -> Unicode -> byte
     */
    @Test
    public void testNewConverter871() {
        CCSID871 cp = new CCSID871();
        cp.init();
        assertNotNull(cp,"At least an ASCII Codepage should be available.");

        for (int i = 0; i < 256; i++) {
            final byte beginvalue = (byte) i;
            final char converted = cp.ebcdic2uni(beginvalue);
            final byte afterall = cp.uni2ebcdic(converted);
            assertEquals(beginvalue, afterall,"Testing item #" + i);
        }
    }

    /**
     * Testing for Correctness both implementations ...
     * Testing byte -> Unicode -> byte
     */
    @Test
    public void testBoth() {
        final ICodePage cp = CharMappings.getCodePage("871");
        final CCSID871 cpex = new CCSID871();
        cpex.init();
        assertNotNull(cpex,"At least an ASCII Codepage should be available.");

        for (int i = 0; i < 256; i++) {
            final byte beginvalue = (byte) i;
            assertEquals(cp.ebcdic2uni(beginvalue), cpex.ebcdic2uni(beginvalue),"Testing to EBCDIC item #" + i);
            final char converted = cp.ebcdic2uni(beginvalue);
            assertEquals(cp.uni2ebcdic(converted), cpex.uni2ebcdic(converted),"Testing to UNICODE item #" + i);
            final byte afterall = cp.uni2ebcdic(converted);
            assertEquals(beginvalue, afterall,"Testing before and after item #" + i);
        }
    }

}
