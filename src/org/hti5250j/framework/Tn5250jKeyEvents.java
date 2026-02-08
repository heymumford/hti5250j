/*
 * SPDX-FileCopyrightText: Copyright (C) 2004 Seagull Software
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: bvansomeren (bvansomeren@seagull.nl)
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */




package org.hti5250j.framework;

//import org.hti5250j.Screen5250;

import org.hti5250j.framework.tn5250.Screen5250;

public class Tn5250jKeyEvents extends Tn5250jEvent {
    private String keystrokes;

    public Tn5250jKeyEvents(Screen5250 screen, String strokes) {
        super(screen);
        this.keystrokes = strokes;
    }

    public String getKeystrokes() {
        return this.keystrokes;
    }

}
