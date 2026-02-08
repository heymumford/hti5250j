/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j;

import org.hti5250j.framework.tn5250.Screen5250;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import static org.hti5250j.keyboard.KeyMnemonic.PAGE_DOWN;
import static org.hti5250j.keyboard.KeyMnemonic.PAGE_UP;

/**
 * Session Scroller to allow the use of the mouse wheel to move the list on the
 * screen up and down.
 */
public class SessionScroller implements MouseWheelListener {

    private Screen5250 screen = null;

    public void addMouseWheelListener(SessionPanel sessionPanel) {
        this.screen = sessionPanel.getScreen();
        sessionPanel.addMouseWheelListener(this);
    }

    public void removeMouseWheelListener(SessionPanel sessionPanel) {
        this.screen = null;
        sessionPanel.removeMouseWheelListener(this);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent) {
        if (this.screen != null) {
            int notches = mouseWheelEvent.getWheelRotation();
            if (notches < 0) {
                screen.sendKeys(PAGE_UP);
            } else {
                screen.sendKeys(PAGE_DOWN);
            }
        }
    }

}
