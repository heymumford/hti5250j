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
import org.hti5250j.interfaces.IKeyEvent;
import org.hti5250j.headless.HeadlessKeyEvent;

/**
 * Swing-compatible keyboard handler with headless delegation.
 *
 * This class bridges Swing KeyEvent processing to the headless-compatible
 * IKeyHandler interface, enabling operation in both GUI and server
 * environments.
 *
 * Wave 3A Track 3: IKeyHandler interface extraction (DEPRECATED wrapper)
 *
 * @deprecated Use IKeyHandler and HeadlessKeyboardHandler directly for
 *             new code. This class is maintained for backward compatibility.
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
     * Headless-compatible delegate for cross-platform operation.
     * @since Wave 3A Track 3
     */
    protected IKeyHandler headlessDelegate;

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

    /**
     * Bridge method to support headless operation via IKeyHandler.
     *
     * Converts Swing KeyEvent to IKeyEvent and delegates to
     * headless handler for cross-platform compatibility.
     *
     * @param evt the Swing KeyEvent
     * @return true if the event was handled
     *
     * @since Wave 3A Track 3
     */
    public boolean processKeyEventHeadless(KeyEvent evt) {
        if (headlessDelegate == null) {
            return false;
        }

        try {
            // Convert Swing KeyEvent to platform-independent IKeyEvent
            IKeyEvent keyEvent = new HeadlessKeyEvent(
                evt.getKeyCode(),
                evt.isShiftDown(),
                evt.isControlDown(),
                evt.isAltDown(),
                evt.isAltGraphDown(),
                evt.getKeyLocation(),
                evt.getKeyChar()
            );

            return headlessDelegate.handleKey(keyEvent);
        } catch (Exception e) {
            System.err.println("Error processing key event in headless mode: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the headless delegate handler.
     *
     * @return the IKeyHandler delegate, or null if not set
     *
     * @since Wave 3A Track 3
     */
    public IKeyHandler getHeadlessDelegate() {
        return headlessDelegate;
    }

    /**
     * Set the headless delegate handler.
     *
     * Used for dependency injection of custom key handler implementations.
     *
     * @param delegate the IKeyHandler to use
     *
     * @since Wave 3A Track 3
     */
    public void setHeadlessDelegate(IKeyHandler delegate) {
        this.headlessDelegate = delegate;
        if (delegate != null) {
            delegate.setKeyMapper(keyMap);
        }
    }

}
