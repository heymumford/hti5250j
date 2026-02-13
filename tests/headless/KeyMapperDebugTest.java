/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.tests.headless;

import org.junit.jupiter.api.*;
import org.hti5250j.keyboard.KeyMapper;
import org.hti5250j.interfaces.IKeyEvent;
import org.hti5250j.headless.HeadlessKeyEvent;

@DisplayName("KeyMapper Debug Tests")
class KeyMapperDebugTest {

    @BeforeEach
    void setUp() {
        KeyMapper.init();
    }

    @Test
    @DisplayName("Debug: Get mnemonic for Enter key")
    void testDebugEnterKey() {
        IKeyEvent keyEvent = new HeadlessKeyEvent(10); // Enter key
        String mnemonic = KeyMapper.getKeyStrokeMnemonic(keyEvent);

        System.out.println("Enter key mnemonic: [" + mnemonic + "]");
        System.out.println("Is null: " + (mnemonic == null));
    }
}
