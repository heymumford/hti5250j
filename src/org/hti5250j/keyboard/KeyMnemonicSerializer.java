/*
 * SPDX-FileCopyrightText: Copyright (c) 2016
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Martin W. Kirst
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.keyboard;

import java.util.ArrayList;
import java.util.List;

public class KeyMnemonicSerializer {

    private final KeyMnemonicResolver keyMnemonicResolver = new KeyMnemonicResolver();

    public String serialize(KeyMnemonic[] keyMnemonics) {
        StringBuilder sb = new StringBuilder();
        if (keyMnemonics != null) {
            for (int i = 0; i < keyMnemonics.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(keyMnemonics[i].mnemonic);
            }
        }
        return sb.toString();
    }

    public KeyMnemonic[] deserialize(String keypadMnemonics) {
        if (keypadMnemonics == null) return new KeyMnemonic[0];
        String[] parts = keypadMnemonics.split(",");
        List<KeyMnemonic> result = new ArrayList<KeyMnemonic>();
        for (String part : parts) {
            KeyMnemonic mnemonic = keyMnemonicResolver.findMnemonic(part.trim());
            if (mnemonic != null) {
                result.add(mnemonic);
            }
        }
        return result.toArray(new KeyMnemonic[result.size()]);
    }

}
