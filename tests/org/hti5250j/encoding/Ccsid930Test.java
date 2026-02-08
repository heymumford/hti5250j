/*
 * SPDX-FileCopyrightText: Copyright (c) 2001,2009,2021
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: nitram509
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.encoding;

import org.junit.jupiter.api.Test;
import org.hti5250j.encoding.builtin.CCSID930;

import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hti5250j.framework.tn5250.ByteExplainer.SHIFT_IN;
import static org.hti5250j.framework.tn5250.ByteExplainer.SHIFT_OUT;

public class Ccsid930Test {

    @Test
    public void double_byte_character_can_be_converted() throws UnsupportedEncodingException {
        CCSID930 ccsid930 = new CCSID930();
        char c;

        c = ccsid930.ebcdic2uni(SHIFT_IN);
        assertEquals(0, c,"SHIFT IN must be converted to zero");

        c = ccsid930.ebcdic2uni(0x43);
        assertEquals(0, c,"first byte must be converted to zero");
        c = ccsid930.ebcdic2uni(0x8C);
        assertEquals('\u30B5', c,"second byte must be converted to a japanese character");

        c = ccsid930.ebcdic2uni(0x43);
        assertEquals(0, c,"first byte must be converted to zero");
        c = ccsid930.ebcdic2uni(0xD1);
        assertEquals('\u30D6', c,"second byte must be converted to a japanese character");

        c = ccsid930.ebcdic2uni(SHIFT_OUT);
        assertEquals(0, c,"SHIFT OUT must be converted to zero");
    }
}
