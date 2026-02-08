/*
 * SPDX-FileCopyrightText: Copyright (c) 2016
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Martin W. Kirst
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.keyboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.hti5250j.keyboard.KeyMnemonic.*;

public class KeyMnemonicSerializerTest {

    private KeyMnemonicSerializer serializer;

    @BeforeEach
    public void setUp() throws Exception {
        serializer = new KeyMnemonicSerializer();
    }

    @Test
    public void mnemonics_are_serialized_as_comma_separated_string() throws Exception {
        String actual = serializer.serialize(new KeyMnemonic[]{CLEAR, ATTN, COPY});

        assertEquals("[clear],[attn],[copy]", actual);
    }

    @Test
    public void serializer_is_null_safe() throws Exception {
        serializer.serialize(null);

        // assert no exception
    }

    @Test
    public void serializer_is_empty_array_safe() throws Exception {
        String actual = serializer.serialize(new KeyMnemonic[0]);

        assertEquals("", actual);
    }

    @Test
    public void mnemonics_are_deserialized_from_comma_separated_string() throws Exception {
        KeyMnemonic[] actual = serializer.deserialize("[clear], [attn] ,[copy]");

        assertTrue(actual.length == 3);
        assertEquals(actual[0], CLEAR);
        assertEquals(actual[1], ATTN);
        assertEquals(actual[2], COPY);
    }

    @Test
    public void deserializer_is_null_safe() throws Exception {
        KeyMnemonic[] actual = serializer.deserialize(null);

        assertNotNull(actual);
        assertTrue(actual.length == 0);
    }

    @Test
    public void deserializer_is_safe_with_empty_string() throws Exception {
        KeyMnemonic[] actual = serializer.deserialize("");

        assertNotNull(actual);
        assertTrue(actual.length == 0);
    }

    @Test
    public void deserializer_ignores_unknown_values() throws Exception {
        KeyMnemonic[] actual = serializer.deserialize("[clear],,[foobar],[attn]");

        assertTrue(actual.length == 2);
        assertEquals(actual[0], CLEAR);
        assertEquals(actual[1], ATTN);
    }
}
