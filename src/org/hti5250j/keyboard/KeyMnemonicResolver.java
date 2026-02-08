/*
 * SPDX-FileCopyrightText: Copyright (c) 2016
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Martin W. Kirst
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.keyboard;

import org.hti5250j.tools.LangTool;

import java.util.Arrays;

public class KeyMnemonicResolver {

    public int findMnemonicValue(String mnemonicStr) {
        for (KeyMnemonic mnemonic : KeyMnemonic.values()) {
            if (mnemonic.mnemonic.equals(mnemonicStr))
                return mnemonic.value;
        }
        return 0;
    }

    public KeyMnemonic findMnemonic(String mnemonicStr) {
        for (KeyMnemonic mnemonic : KeyMnemonic.values()) {
            if (mnemonic.mnemonic.equals(mnemonicStr))
                return mnemonic;
        }
        return null;
    }

    public String[] getMnemonics() {
        String[] result = new String[KeyMnemonic.values().length];
        int i = 0;
        for (KeyMnemonic keyMnemonic : KeyMnemonic.values()) {
            result[i++] = keyMnemonic.mnemonic;
        }
        return result;
    }

    public String[] getMnemonicsSorted() {
        String[] mnemonics = getMnemonics();
        Arrays.sort(mnemonics);
        return mnemonics;
    }

    public String[] getMnemonicDescriptions() {
        KeyMnemonic[] mnemonicData = KeyMnemonic.values();
        String[] result = new String[KeyMnemonic.values().length];
        int i = 0;
        for (KeyMnemonic mnemonic : mnemonicData) {
            result[i++] = getDescription(mnemonic);
        }
        return result;
    }

    public String getDescription(KeyMnemonic mnemonic) {
        return LangTool.getString("key." + mnemonic.mnemonic);
    }
}
