/*
 * SPDX-FileCopyrightText: Copyright (c) 2001, 2002, 2003
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.event;

import java.util.EventObject;

public class EmulatorActionEvent extends EventObject {

    private static final long serialVersionUID = 1L;
    public static final int CLOSE_SESSION = 1;
    public static final int START_NEW_SESSION = 2;
    public static final int CLOSE_EMULATOR = 3;
    public static final int START_DUPLICATE = 4;

    public EmulatorActionEvent(Object obj) {
        super(obj);

    }

    public EmulatorActionEvent(Object obj, String s) {
        super(obj);
        message = s;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String s) {
        message = s;
    }

    public int getAction() {

        return action;
    }

    public void setAction(int s) {

        action = s;
    }

    private String message;
    private int action;
}
