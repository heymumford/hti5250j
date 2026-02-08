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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import static org.hti5250j.keyboard.KeyMnemonic.CLOSE;


/**
 * Display session attributes
 */
public class CloseAction extends EmulatorAction {

    private static final long serialVersionUID = 1L;

    public CloseAction(SessionPanel session, KeyMapper keyMap) {
        super(session,
                CLOSE.mnemonic,
                KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.ALT_MASK),
                keyMap);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        session.confirmCloseSession(true);
    }
}
