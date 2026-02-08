/*
 * SPDX-FileCopyrightText: /*Copyright (C) 2004 Seagull Software
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: bvansomeren (bvansomeren@seagull.nl)
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */




package org.hti5250j.framework;

import org.hti5250j.framework.tn5250.Screen5250;
import org.hti5250j.framework.tn5250.ScreenFields;

public class Tn5250jEvent {

    private Screen5250 screen;
    private char[] data;
    private ScreenFields fields;

    public Tn5250jEvent() {
        screen = null;
    }

    public Tn5250jEvent(Screen5250 newscreen) {
        screen = newscreen;
        // changed by Kenneth - This should be replaced with a call to
        //   getPlane method of screen object when they are implemented.  These
        //   new methods will also do the array copy.
        char[] original = screen.getCharacters();
        data = new char[original.length];
        System.arraycopy(original, 0, data, 0, original.length);
        this.fields = newscreen.getScreenFields();
    }

    public char[] getData() {
        return data;
    }

    public Screen5250 getScreen() {
        return screen;
    }

    public ScreenFields getFields() {
        return this.fields;
    }
}
