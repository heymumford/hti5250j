/*
 * SPDX-FileCopyrightText: Copyright (C) 2004 Seagull Software
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: bvansomeren (bvansomeren@seagull.nl)
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 */




package org.hti5250j.framework;

import org.hti5250j.framework.tn5250.Screen5250;
import org.hti5250j.SessionPanel;
import org.hti5250j.framework.tn5250.tnvt;

public class Tn5250jSession {
    private Screen5250 sessionScreen;
    private tnvt SessionTNVT;
    private SessionPanel session;

    protected Tn5250jSession(Screen5250 screen, tnvt vt, SessionPanel ses) {
        sessionScreen = screen;
        SessionTNVT = vt;
        session = ses;
    }

    public SessionPanel getSession() {
        return session;
    }

    public Screen5250 getSessionScreen() {
        return sessionScreen;
    }

    public tnvt getSessionTNVT() {
        return SessionTNVT;
    }

}
