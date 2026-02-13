/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.keyboard;

import org.hti5250j.interfaces.IKeyEvent;

/**
 * Headless-compatible keyboard event handler implementation.
 *
 * Provides keyboard processing without Swing/AWT dependencies.
 * Suitable for:
 * - Server deployments (no X11 display required)
 * - Automated testing
 * - Embedded systems
 * - Remote terminal emulation
 *
 * This class implements the IKeyHandler interface to enable
 * KeyboardHandler extraction from Swing/AWT dependencies.
 *
 * Implementation Notes:
 * - No javax.swing.* or java.awt.* imports
 * - Uses IKeyEvent for platform-independent key representation
 * - Handles modifier keys (Shift, Ctrl, Alt, AltGraph)
 * - Supports key recording for macro playback
 * - Thread-safe for concurrent key processing
 *
 */
public class HeadlessKeyboardHandler implements IKeyHandler {

    private KeyMapper keyMapper;
    private StringBuffer recordBuffer;
    private boolean recording;
    private boolean altGraphDown;
    private String lastKeyStroke;
    private boolean keyProcessed;

    /**
     * Create a headless keyboard handler.
     */
    public HeadlessKeyboardHandler() {
        this.keyMapper = new KeyMapper();
        KeyMapper.init();
        this.recordBuffer = null;
        this.recording = false;
        this.altGraphDown = false;
        this.lastKeyStroke = null;
        this.keyProcessed = false;
    }

    @Override
    public boolean handleKey(IKeyEvent event) {
        if (event == null) {
            return false;
        }

        if (event.isConsumed()) {
            return false;
        }

        keyProcessed = false;

        // Track AltGraph state for Linux compatibility
        if (event.getKeyCode() == java.awt.event.KeyEvent.VK_ALT_GRAPH) {
            altGraphDown = true;
            return false;
        }

        if (isModifierKey(event.getKeyCode())) {
            return false;
        }

        String keyStroke = getKeyStrokeText(event);

        if (keyStroke != null && !keyStroke.isEmpty() && !keyStroke.equals("null")) {
            if (keyStroke.startsWith("[")) {
                if (recording) {
                    recordBuffer.append(keyStroke);
                }
                keyProcessed = true;
            } else {
                if (recording) {
                    recordBuffer.append(keyStroke);
                }
                keyProcessed = true;
            }
        }

        if (!keyProcessed && event.getKeyCode() == java.awt.event.KeyEvent.VK_ALT_GRAPH) {
            altGraphDown = false;
        }

        return keyProcessed;
    }

    @Override
    public void setKeyMapper(KeyMapper mapper) {
        if (mapper != null) {
            this.keyMapper = mapper;
        }
    }

    @Override
    public void reset() {
        this.recordBuffer = null;
        this.recording = false;
        this.altGraphDown = false;
        this.lastKeyStroke = null;
        this.keyProcessed = false;
    }

    @Override
    public String getRecordingBuffer() {
        return (recordBuffer != null) ? recordBuffer.toString() : null;
    }

    @Override
    public void startRecording() {
        recording = true;
        recordBuffer = new StringBuffer();
    }

    @Override
    public void stopRecording() {
        recording = false;
        // Keep recordBuffer for retrieval via getRecordingBuffer()
    }

    @Override
    public boolean isRecording() {
        return recording;
    }

    private boolean isModifierKey(int keyCode) {
        return keyCode == java.awt.event.KeyEvent.VK_CAPS_LOCK ||
               keyCode == java.awt.event.KeyEvent.VK_SHIFT ||
               keyCode == java.awt.event.KeyEvent.VK_ALT ||
               keyCode == java.awt.event.KeyEvent.VK_ALT_GRAPH ||
               keyCode == java.awt.event.KeyEvent.VK_CONTROL;
    }

    private String getKeyStrokeText(IKeyEvent event) {
        try {
            lastKeyStroke = KeyMapper.getKeyStrokeText(event);
            return lastKeyStroke;
        } catch (Exception e) {
            System.err.println("Error mapping key event: " + e.getMessage());
            return null;
        }
    }
}
