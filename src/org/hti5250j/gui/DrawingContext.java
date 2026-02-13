/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.gui;

import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Drawing context and state management for GuiGraphicBuffer.
 *
 * Extracted from GuiGraphicBuffer to follow Single Responsibility Principle.
 * This class encapsulates drawing context management concerns including:
 * - Graphics context (Graphics2D reference for drawing operations)
 * - Dirty region tracking (rectangular regions needing redraw)
 * - Double buffering state (optimization for rendering)
 * - Clipping regions (bounds for graphics operations)
 *
 * Design:
 * - Simple, focused responsibility (drawing context only)
 * - No rendering logic (delegated to GuiGraphicBuffer)
 * - Thread-safe for reference assignments
 * - Supports dirty region accumulation via union operations
 *
 * Phase 4 of GuiGraphicBuffer refactoring (Wave 3A Track 2).
 *
 * @author Eric C. Mumford
 * @since 2026-02-13
 */
public class DrawingContext {

    // Graphics context reference
    private Graphics2D graphics;

    // Dirty region tracking for optimized repaints
    private Rectangle dirtyRegion;
    private boolean dirty;

    // Double buffering state
    private boolean doubleBuffered;

    // Clipping region (bounds for graphics operations)
    private Rectangle clipRegion;

    /**
     * Construct a new DrawingContext with default (clean) state.
     */
    public DrawingContext() {
        this.graphics = null;
        this.dirtyRegion = null;
        this.dirty = false;
        this.doubleBuffered = false;
        this.clipRegion = null;
    }

    /**
     * Set the graphics context reference for drawing operations.
     *
     * @param g the Graphics2D context to use for drawing
     */
    public void setGraphics(Graphics2D g) {
        this.graphics = g;
    }

    /**
     * Get the current graphics context reference.
     *
     * @return the Graphics2D context, or null if not set
     */
    public Graphics2D getGraphics() {
        return graphics;
    }

    /**
     * Mark a rectangular region as dirty (needing redraw).
     *
     * Multiple calls accumulate into a single bounding rectangle via union operation.
     * This supports efficient dirty region tracking for optimized screen updates.
     *
     * @param x      the x coordinate of the region
     * @param y      the y coordinate of the region
     * @param width  the width of the region
     * @param height the height of the region
     */
    public void markDirty(int x, int y, int width, int height) {
        if (dirtyRegion == null) {
            // First dirty region: create new rectangle
            dirtyRegion = new Rectangle(x, y, width, height);
        } else {
            // Accumulate: union with existing dirty region
            dirtyRegion.add(new Rectangle(x, y, width, height));
        }
        dirty = true;
    }

    /**
     * Clear the dirty flag and reset the dirty region.
     *
     * Called after the dirty region has been redrawn and screen is up-to-date.
     */
    public void clearDirty() {
        dirty = false;
        dirtyRegion = null;
    }

    /**
     * Check if any region is marked as dirty.
     *
     * @return true if any region has been marked dirty and not yet cleared
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Get the accumulated dirty region rectangle.
     *
     * If multiple regions were marked dirty, this returns the bounding rectangle
     * that encompasses all of them.
     *
     * @return the dirty region rectangle, or null if not dirty
     */
    public Rectangle getDirtyRegion() {
        return dirtyRegion;
    }

    /**
     * Enable or disable double buffering.
     *
     * Double buffering is a rendering optimization that reduces flicker
     * by rendering to an off-screen buffer before displaying.
     *
     * @param enabled true to enable double buffering, false to disable
     */
    public void setDoubleBuffered(boolean enabled) {
        this.doubleBuffered = enabled;
    }

    /**
     * Check if double buffering is enabled.
     *
     * @return true if double buffering is enabled
     */
    public boolean isDoubleBuffered() {
        return doubleBuffered;
    }

    /**
     * Set the clipping region for graphics operations.
     *
     * The clipping region constrains where graphics operations can draw,
     * preventing accidental rendering outside the intended bounds.
     *
     * @param clip the Rectangle defining the clipping bounds, or null for no clipping
     */
    public void setClipRegion(Rectangle clip) {
        this.clipRegion = clip;
    }

    /**
     * Get the current clipping region.
     *
     * @return the clipping Rectangle, or null if no clipping is set
     */
    public Rectangle getClipRegion() {
        return clipRegion;
    }
}
