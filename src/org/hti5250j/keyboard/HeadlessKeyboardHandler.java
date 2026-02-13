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
 * @since Wave 3A Track 3 (KeyboardHandler Extraction)
 */
public class HeadlessKeyboardHandler implements IKeyHandler {

    // Instance state
    private KeyMapper keyMapper;
    private StringBuffer recordBuffer;
    private boolean recording;
    private boolean altGraphDown; // AltGr state tracking for Linux
    private String lastKeyStroke;
    private boolean keyProcessed;

    /**
     * Create a headless keyboard handler.
     *
     * Initializes the handler without requiring Swing components
     * or a Session5250 context.
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

    /**
     * Process a key event in headless mode.
     *
     * @param event the key event to process
     * @return true if the key was handled, false otherwise
     */
    @Override
    public boolean handleKey(IKeyEvent event) {
        // Reject null events
        if (event == null) {
            return false;
        }

        // Skip consumed events
        if (event.isConsumed()) {
            return false;
        }

        // Reset keyProcessed flag for this event
        keyProcessed = false;

        // Track AltGraph state (Linux compatibility)
        if (event.getKeyCode() == java.awt.event.KeyEvent.VK_ALT_GRAPH) {
            altGraphDown = true;
            return false;
        }

        // Handle special modifier keys
        if (isModifierKey(event.getKeyCode())) {
            return false;
        }

        // Map the key using KeyMapper
        String keyStroke = getKeyStrokeText(event);

        if (keyStroke != null && !keyStroke.isEmpty() && !keyStroke.equals("null")) {
            // Handle special keys (function keys, arrow keys, etc.)
            if (keyStroke.startsWith("[")) {
                // Terminal function key or special key
                if (recording) {
                    recordBuffer.append(keyStroke);
                }
                keyProcessed = true;
            } else {
                // Regular character or macro
                if (recording) {
                    recordBuffer.append(keyStroke);
                }
                keyProcessed = true;
            }
        }

        // Update state for next event
        if (!keyProcessed && event.getKeyCode() == java.awt.event.KeyEvent.VK_ALT_GRAPH) {
            altGraphDown = false;
        }

        return keyProcessed;
    }

    /**
     * Set the key mapper for this handler.
     *
     * @param mapper the KeyMapper to use
     */
    @Override
    public void setKeyMapper(KeyMapper mapper) {
        if (mapper != null) {
            this.keyMapper = mapper;
        }
    }

    /**
     * Reset the handler state.
     */
    @Override
    public void reset() {
        this.recordBuffer = null;
        this.recording = false;
        this.altGraphDown = false;
        this.lastKeyStroke = null;
        this.keyProcessed = false;
    }

    /**
     * Get the recording buffer.
     *
     * @return the buffer contents, or null if not recording
     */
    @Override
    public String getRecordingBuffer() {
        return (recordBuffer != null) ? recordBuffer.toString() : null;
    }

    /**
     * Start recording keystrokes.
     */
    @Override
    public void startRecording() {
        recording = true;
        recordBuffer = new StringBuffer();
    }

    /**
     * Stop recording keystrokes.
     */
    @Override
    public void stopRecording() {
        recording = false;
        // Keep recordBuffer for retrieval via getRecordingBuffer()
    }

    /**
     * Check if recording is active.
     *
     * @return true if currently recording
     */
    @Override
    public boolean isRecording() {
        return recording;
    }

    /**
     * Check if a key code is a modifier key.
     *
     * @param keyCode the key code to check
     * @return true if the key is a modifier
     */
    private boolean isModifierKey(int keyCode) {
        return keyCode == java.awt.event.KeyEvent.VK_CAPS_LOCK ||
               keyCode == java.awt.event.KeyEvent.VK_SHIFT ||
               keyCode == java.awt.event.KeyEvent.VK_ALT ||
               keyCode == java.awt.event.KeyEvent.VK_ALT_GRAPH ||
               keyCode == java.awt.event.KeyEvent.VK_CONTROL;
    }

    /**
     * Get the keystroke text for an event.
     *
     * Uses KeyMapper to translate the raw key event to a
     * mnemonic or character string.
     *
     * @param event the key event
     * @return the keystroke text, or null if unmappable
     */
    private String getKeyStrokeText(IKeyEvent event) {
        try {
            lastKeyStroke = KeyMapper.getKeyStrokeText(event);
            return lastKeyStroke;
        } catch (Exception e) {
            // Log the exception but don't crash
            System.err.println("Error mapping key event: " + e.getMessage());
            return null;
        }
    }
}
