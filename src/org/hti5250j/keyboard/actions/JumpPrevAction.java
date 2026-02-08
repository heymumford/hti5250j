/*
 * SPDX-FileCopyrightText: Copyright (c) 2001,2002
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.keyboard.actions;

import org.hti5250j.SessionPanel;
import org.hti5250j.keyboard.KeyMapper;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static org.hti5250j.keyboard.KeyMnemonic.JUMP_PREV;

/**
 * Jump to the Previous session action
 */
public class JumpPrevAction extends EmulatorAction {

    private static final long serialVersionUID = 1L;

    public JumpPrevAction(SessionPanel session, KeyMapper keyMap) {
        super(session,
                JUMP_PREV.mnemonic,
                KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, KeyEvent.ALT_MASK),
                keyMap);
    }

    public void actionPerformed(ActionEvent e) {
        session.prevSession();
    }
}
