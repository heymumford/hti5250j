/*
 * SPDX-FileCopyrightText: Copyright (c) 2001, 2002
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.keyboard;

import org.hti5250j.Session5250;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * The default keyboard input handler.
 */
class DefaultKeyboardHandler extends KeyboardHandler {

    /**
     * Creates a new keyboard handler
     *
     * @param session The session to which the keys should be sent
     */
    DefaultKeyboardHandler(Session5250 session) {
        super(session);
    }

    public boolean isKeyStrokeDefined(String accelKey) {

        return KeyMapper.isKeyStrokeDefined(accelKey);
    }

    public KeyStroke getKeyStroke(String accelKey) {
        return KeyMapper.getKeyStroke(accelKey);
    }

    /*
     * We have to jump through some hoops to avoid
     * trying to print non-printing characters
     * such as Shift.  (Not only do they not print,
     * but if you put them in a String, the characters
     * afterward won't show up in the text area.)
     */
    protected void displayInfo(KeyEvent e, String s) {
        String charString, keyCodeString, modString, tmpString, isString;

        char c = e.getKeyChar();
        int keyCode = e.getKeyCode();
        int modifiers = e.getModifiers();

        if (Character.isISOControl(c)) {
            charString = "key character = "
                    + "(an unprintable control character)";
        } else {
            charString = "key character = '"
                    + c + "'";
        }

        keyCodeString = "key code = " + keyCode
                + " ("
                + KeyEvent.getKeyText(keyCode)
                + ")";
        if (keyCode == KeyEvent.VK_PREVIOUS_CANDIDATE) {

            keyCodeString += " previous candidate ";

        }

        if (keyCode == KeyEvent.VK_DEAD_ABOVEDOT ||
                keyCode == KeyEvent.VK_DEAD_ABOVERING ||
                keyCode == KeyEvent.VK_DEAD_ACUTE ||
                keyCode == KeyEvent.VK_DEAD_BREVE ||
                keyCode == KeyEvent.VK_DEAD_CIRCUMFLEX

        ) {

            keyCodeString += " dead key ";

        }

        modString = "modifiers = " + modifiers;
        tmpString = KeyEvent.getKeyModifiersText(modifiers);
        if (tmpString.length() > 0) {
            modString += " (" + tmpString + ")";
        } else {
            modString += " (no modifiers)";
        }

        isString = "isKeys = isActionKey (" + e.isActionKey() + ")" +
                " isAltDown (" + e.isAltDown() + ")" +
                " isAltGraphDown (" + e.isAltGraphDown() + ")" +
                " isAltGraphDownLinux (" + isAltGr + ")" +
                " isControlDown (" + e.isControlDown() + ")" +
                " isMetaDown (" + e.isMetaDown() + ")" +
                " isShiftDown (" + e.isShiftDown() + ")";


        String newline = "\n";
        System.out.println(s + newline
                + "    " + charString + newline
                + "    " + keyCodeString + newline
                + "    " + modString + newline
                + "    " + isString + newline);

    }

    /**
     * This is here for keybindings using the swing input map - the preferred
     * way to use the keyboard.
     */
    void initKeyBindings() {
        // GUI-specific keyboard bindings have been removed (dead code)
        // This method is kept as a stub for backward compatibility
    }

    /**
     * Forwards key events directly to the input handler.
     * This is slightly faster than using a KeyListener
     * because some Swing overhead is avoided.
     */
    public void processKeyEvent(KeyEvent evt) {

        if (evt.isConsumed()) {
            return;
        }

        switch (evt.getID()) {
            case KeyEvent.KEY_TYPED:
                processVTKeyTyped(evt);
                break;
            case KeyEvent.KEY_PRESSED:
                processVTKeyPressed(evt);
                break;
            case KeyEvent.KEY_RELEASED:
                processVTKeyReleased(evt);
                break;
            default:
                break;
        }

    }

    private void processVTKeyPressed(KeyEvent e) {


        keyProcessed = true;
        int keyCode = e.getKeyCode();

        if (isLinux && keyCode == KeyEvent.VK_ALT_GRAPH) {

            isAltGr = true;
        }

        if (keyCode == KeyEvent.VK_CAPS_LOCK ||
                keyCode == KeyEvent.VK_SHIFT ||
                keyCode == KeyEvent.VK_ALT ||
                keyCode == KeyEvent.VK_ALT_GRAPH
        ) {

            return;
        }

        KeyStroke ks = KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers(), false);

        if (emulatorAction(ks, e)) {

            return;
        }

        if (isLinux) {
            lastKeyStroke = KeyMapper.getKeyStrokeText(e, isAltGr);
        } else {
            lastKeyStroke = KeyMapper.getKeyStrokeText(e);
        }

        if (lastKeyStroke != null && !lastKeyStroke.equals("null")) {

            if (lastKeyStroke.startsWith("[") || lastKeyStroke.length() == 1) {

                screen.sendKeys(lastKeyStroke);
                if (recording) {
                    recordBuffer.append(lastKeyStroke);
                }
            }
        } else {
            keyProcessed = false;
        }

        if (keyProcessed) {
            e.consume();
        }

    }

    private void processVTKeyTyped(KeyEvent e) {

        char kc = e.getKeyChar();
        // Hack to make german umlauts work under Linux
        // The problem is that these umlauts don't generate a keyPressed event
        // and so keyProcessed is true (even if is hasn't been processed)
        // so we check if it's a letter (with or without shift) and skip return
        if (isLinux) {

            if (!((Character.isLetter(kc) || kc == '\u20AC') && (e.getModifiers() == 0
                    || e.getModifiers() == KeyEvent.SHIFT_MASK))) {

                if (Character.isISOControl(kc) || keyProcessed) {
                    return;
                }
            }
        } else {
            if (Character.isISOControl(kc) || keyProcessed) {
                return;
            }
        }
        if (!session.isConnected()) {
            return;
        }
        screen.sendKeys(Character.toString(kc));
        if (recording) {
            recordBuffer.append(kc);
        }
        keyProcessed = true;
        e.consume();
    }

    private void processVTKeyReleased(KeyEvent e) {


        if (isLinux && e.getKeyCode() == KeyEvent.VK_ALT_GRAPH) {

            isAltGr = false;
        }

        if (Character.isISOControl(e.getKeyChar()) || keyProcessed || e.isConsumed()) {
            return;
        }

        String s = KeyMapper.getKeyStrokeText(e);

        if (s != null) {

            if (s.startsWith("[")) {
                screen.sendKeys(s);
                if (recording) {
                    recordBuffer.append(s);
                }
            }

        } else {
            keyProcessed = false;
        }

        if (keyProcessed) {
            e.consume();
        }
    }

}
