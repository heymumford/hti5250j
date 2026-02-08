/*
 * SPDX-FileCopyrightText: Copyright (c) 2002
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.event;

import java.util.EventObject;

public class SessionJumpEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    private String message;
    private int jumpDirection;

    public SessionJumpEvent(Object obj) {
        super(obj);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getJumpDirection() {
        return jumpDirection;
    }

    public void setJumpDirection(int direction) {
        this.jumpDirection = direction;
    }

}
