/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.keyboard;

import org.hti5250j.interfaces.IKeyEvent;

/**
 * Platform-independent keyboard event handler interface.
 *
 * Abstracts keyboard handling to support:
 * - Headless operation (servers, automated testing)
 * - GUI operation (Swing/AWT via wrapper)
 * - Custom key handling implementations
 *
 * This interface enables KeyboardHandler to operate without
 * dependencies on java.awt.event.KeyEvent or Swing components.
 *
 * @since Wave 3A Track 3 (KeyboardHandler Extraction)
 */
public interface IKeyHandler {

    /**
     * Process a key event.
     *
     * Handles key translation, mapping lookup, and session integration
     * without Swing/AWT dependencies.
     *
     * @param event the key event to process (from IKeyEvent)
     * @return true if the key was handled, false if it was rejected
     *         or the event was null/consumed
     */
    boolean handleKey(IKeyEvent event);

    /**
     * Set the key mapper for this handler.
     *
     * The key mapper is responsible for translating raw key events
     * into application-specific actions (e.g., terminal function keys,
     * macros, remappings).
     *
     * @param mapper the KeyMapper to use for key translation
     */
    void setKeyMapper(KeyMapper mapper);

    /**
     * Reset the handler state.
     *
     * Clears any internal buffers, flags, and state (e.g., recording
     * buffer, altGr state, keyProcessed flag).
     *
     * Used when:
     * - Starting a new session
     * - Recovering from error conditions
     * - Switching between different session contexts
     */
    void reset();

    /**
     * Get the current recording buffer.
     *
     * Returns the concatenation of all keys processed while
     * recording mode is active.
     *
     * @return the recording buffer contents, or null if not recording
     */
    String getRecordingBuffer();

    /**
     * Start recording keystrokes.
     *
     * All subsequent key events will be appended to the recording
     * buffer until stopRecording() is called.
     */
    void startRecording();

    /**
     * Stop recording keystrokes.
     *
     * Closes the current recording session. The buffer contents
     * can still be retrieved via getRecordingBuffer() until
     * startRecording() is called again.
     */
    void stopRecording();

    /**
     * Check if recording is currently active.
     *
     * @return true if currently recording, false otherwise
     */
    boolean isRecording();
}
