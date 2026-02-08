/*
 * SPDX-FileCopyrightText: Copyright (c) 2001,2009
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: master_jaf
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.encoding;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author master_jaf
 */
public class JavaCodePageTest {

    /**
     * Test method for {@link org.hti5250j.encoding.JavaCodePageFactory#ebcdic2uni(int)}.
     */
    @Test
    public void testEbcdic2uni() {
        ICodePage jcp = JavaCodePageFactory.getCodePage("ASCII");
        assertNotNull(jcp,"At least an ASCII Codepage should be available.");

        char actual = jcp.ebcdic2uni(97);
        assertEquals('a', actual,"simple test for character 'a'");
    }

    /**
     * Test method for {@link org.hti5250j.encoding.JavaCodePageFactory#uni2ebcdic(char)}.
     */
    @Test
    public void testUni2ebcdic() {
        ICodePage jcp = JavaCodePageFactory.getCodePage("ASCII");
        assertNotNull(jcp,"At least an ASCII Codepage should be available.");

        byte actual = jcp.uni2ebcdic('a');
        assertEquals(97, actual,"simple test for character 'a' = bytecode 97");
    }

    /**
     * Test for a not existing codepage
     */
    @Test
    public void testNotExistingCodePage() {
        ICodePage jcp = JavaCodePageFactory.getCodePage("FOOBAR");
        assertNull(jcp,"There should be no such Codepage available");
    }
}
