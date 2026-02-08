/*
 * SPDX-FileCopyrightText: Copyright (c) 2002
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.interfaces;

import org.hti5250j.My5250;
import org.hti5250j.SessionPanel;
import org.hti5250j.event.SessionChangeEvent;
import org.hti5250j.event.SessionJumpEvent;
import org.hti5250j.gui.GenericTn5250JFrame;

/**
 * Abstract class for all main GUI interfaces.<br>
 * Direct known subclasses:
 * <ul>
 * <li>{@link org.hti5250j.Gui5250Frame} which shows a window with multiple tabs</li>
 * <li>{@link org.hti5250j.Gui5250MDIFrame}</li>
 * </ul>
 */
public abstract class GUIViewInterface extends GenericTn5250JFrame {

    private static final long serialVersionUID = 1L;
    protected static My5250 me;
    protected static int sequence;
    protected int frameSeq;

    public GUIViewInterface(My5250 m) {
        super();
        me = m;
    }

    public int getFrameSequence() {
        return frameSeq;
    }

    public abstract void addSessionView(String descText, SessionPanel session);

    public abstract void removeSessionView(SessionPanel targetSession);

    public abstract boolean containsSession(SessionPanel session);

    public abstract int getSessionViewCount();

    public abstract SessionPanel getSessionAt(int index);

    public abstract void onSessionJump(SessionJumpEvent jumpEvent);

    public abstract void onSessionChanged(SessionChangeEvent changeEvent);

}
