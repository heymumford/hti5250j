/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.keyboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KeyMnemonicResolverTest {

    private KeyMnemonicResolver resolver;

    @BeforeEach
    public void setUp() throws Exception {
        resolver = new KeyMnemonicResolver();
    }

    @Test
    public void if_mnemonic_not_fount_return_ZERO() throws Exception {
        int value = resolver.findMnemonicValue("illegal value");
        assertEquals(0, value);
    }

    @Test
    public void search_is_null_safe() throws Exception {
        int value = resolver.findMnemonicValue(null);
        assertEquals(0, value);
    }
}
