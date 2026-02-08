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
import org.hti5250j.encoding.builtin.CCSID277;

/**
 * Testing the correctness of {@link CCSID277} and comparing with existing implementation.
 *
 * @author master_jaf
 */
public class CCSID277dkTest {

    /**
     * Correctness test for old implementation ....
     * Testing byte -> Unicode -> byte
     */
    @Test
    public void testOldConverter277() {

        ICodePage cp = CharMappings.getCodePage("277-dk");
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
    public void testNewConverter277() {
        CCSID277 cp = new CCSID277();
        cp.init();
        assertNotNull(cp,"At least an ASCII Codepage should be available.");

        for (int i = 0; i < 256; i++) {
            final byte beginvalue = (byte) i;
            final char converted = cp.ebcdic2uni(beginvalue);
            final byte afterall = cp.uni2ebcdic(converted);
            assertEquals(beginvalue, afterall,"Testing item #" + i);
        }
    }

}
