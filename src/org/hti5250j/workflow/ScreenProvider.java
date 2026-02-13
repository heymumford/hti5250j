package org.hti5250j.workflow;

import org.hti5250j.framework.tn5250.Screen5250;

/**
 * Provider interface for accessing screen without coupling to Session5250.
 * Enables polymorphic session types in future (e.g., mock sessions, session pools).
 *
 * @since 0.12.0
 */
public interface ScreenProvider {
    /**
     * Get the screen for interaction.
     *
     * @return Screen5250 instance for current session
     * @throws IllegalStateException if screen not available
     */
    Screen5250 getScreen() throws IllegalStateException;
}
