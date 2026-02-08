/*
 * SPDX-FileCopyrightText: Copyright (c) 2001, 2002
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.keyboard;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import org.hti5250j.Session5250;
import org.hti5250j.SessionPanel;
import org.hti5250j.event.KeyChangeListener;
import org.hti5250j.framework.tn5250.Screen5250;
import org.hti5250j.tools.system.OperatingSystem;

/**
 *
 */
public abstract class KeyboardHandler extends KeyAdapter implements KeyChangeListener {

    protected Session5250 session;
    protected SessionPanel sessionGui;
    protected Screen5250 screen;
    protected boolean isLinux;
    protected boolean isAltGr;
    protected boolean keyProcessed = false;
    protected KeyMapper keyMap;
    protected String lastKeyStroke = null;
    protected StringBuffer recordBuffer;
    protected boolean recording;

    /**
     * Creates a new keyboard handler.
     * @param session The session that will be sent the keys
     */
    public KeyboardHandler(Session5250 session) {

        this.session = session;
        this.screen = session.getScreen();
        sessionGui = session.getGUI();

//      String os = System.getProperty("os.name");
//      if (os.toLowerCase().indexOf("linux") != -1) {
//         System.out.println("using os " + os);
//         isLinux = true;
//      }

        isLinux = OperatingSystem.isUnix();

        keyMap = new KeyMapper();
        KeyMapper.init();

        KeyMapper.addKeyChangeListener(this);

        // initialize the keybingings of the components InputMap
        initKeyBindings();


    }

    public static KeyboardHandler getKeyboardHandlerInstance(Session5250 session) {

        return new DefaultKeyboardHandler(session);
    }

    abstract void initKeyBindings();

    protected InputMap getInputMap() {

        return sessionGui.getInputMap();
    }

    protected ActionMap getActionMap() {

        return sessionGui.getActionMap();
    }

    public void onKeyChanged() {

        getInputMap().clear();
        getActionMap().clear();
        initKeyBindings();

    }

    public abstract boolean isKeyStrokeDefined(String accelKey);

    public abstract KeyStroke getKeyStroke(String accelKey);

    public String getRecordBuffer() {
        return recordBuffer.toString();
    }

    public void startRecording() {

        recording = true;
        recordBuffer = new StringBuffer();

    }

    public void stopRecording() {

        recording = false;
        recordBuffer = null;
    }

    public boolean isRecording() {

        return recording;
    }

    /**
     *  Remove the references to all listeners before closing
     *
     *  Added by Luc to fix a memory leak.
     */
    public void sessionClosed(SessionPanel session) {
        keyMap.removeKeyChangeListener(this);
    }

    protected boolean emulatorAction(KeyStroke ks, KeyEvent e) {

        if (sessionGui == null)
            return false;

        InputMap map = getInputMap();
        ActionMap am = getActionMap();

        if (map != null && am != null && sessionGui.isEnabled()) {
            Object binding = map.get(ks);
            Action action = (binding == null) ? null : am.get(binding);
            if (action != null) {
                return true;
            }
        }
        return false;
    }


    /**
     * Utility method, calls one of <code>keyPressed()</code>,
     * <code>keyReleased()</code>, or <code>keyTyped()</code>.
     */
    public void processKeyEvent(KeyEvent evt) {
        switch (evt.getID()) {
            case KeyEvent.KEY_TYPED:
                keyTyped(evt);
                break;
            case KeyEvent.KEY_PRESSED:
                keyPressed(evt);
                break;
            case KeyEvent.KEY_RELEASED:
                keyReleased(evt);
                break;
        }
    }

}
