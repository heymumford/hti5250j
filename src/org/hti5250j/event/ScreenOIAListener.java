/*
 * SPDX-FileCopyrightText: Copyright (c) 2000 - 2002
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.event;

import org.hti5250j.framework.tn5250.ScreenOIA;

public interface ScreenOIAListener {

    int OIA_CHANGED_INSERT_MODE = 0;
    int OIA_CHANGED_KEYS_BUFFERED = 1;
    int OIA_CHANGED_KEYBOARD_LOCKED = 2;
    int OIA_CHANGED_MESSAGELIGHT = 3;
    int OIA_CHANGED_SCRIPT = 4;
    int OIA_CHANGED_BELL = 5;
    int OIA_CHANGED_CLEAR_SCREEN = 6;
    int OIA_CHANGED_INPUTINHIBITED = 7;
    int OIA_CHANGED_CURSOR = 8;


    void onOIAChanged(ScreenOIA oia, int change);

}
