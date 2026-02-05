package org.tn5250j.plugin;

import java.awt.Graphics2D;

/**
 * ScreenDecoratorPlugin - Extension point for screen rendering decorators.
 *
 * Plugins implement this to customize screen rendering:
 * - Overlay highlighting
 * - Visual indicators
 * - Performance metrics display
 * - Accessibility features
 */
public interface ScreenDecoratorPlugin extends TN5250jPlugin {

    /**
     * Render decoration on top of screen.
     * Must complete quickly (< 50ms) to avoid frame rate impact.
     *
     * @param g2d graphics context for rendering
     * @param width screen width in pixels
     * @param height screen height in pixels
     */
    void decorate(Graphics2D g2d, int width, int height);

    /**
     * Get z-order for rendering (higher = on top)
     */
    int getZOrder();

    /**
     * Check if decorator is currently enabled
     */
    boolean isEnabled();
}
