package org.tn5250j.plugin;

import java.awt.event.KeyEvent;

/**
 * KeyHandlerPlugin - Extension point for custom keyboard handling.
 *
 * Plugins implement this to intercept and customize key processing:
 * - Remapping keys
 * - Adding hotkeys
 * - Filtering sensitive keys
 * - Macro expansion
 */
public interface KeyHandlerPlugin extends TN5250jPlugin {

    /**
     * Process a keyboard event before normal TN5250j handling.
     * Return true to consume the event and prevent normal processing.
     * Return false to allow normal processing to continue.
     *
     * @param event the KeyEvent to process
     * @return true if event was consumed, false otherwise
     */
    boolean processKey(KeyEvent event);

    /**
     * Get priority (higher = processed first).
     * Range: 0-100, where 50 is normal priority.
     */
    int getPriority();

    /**
     * Check if this handler is enabled
     */
    boolean isEnabled();
}
