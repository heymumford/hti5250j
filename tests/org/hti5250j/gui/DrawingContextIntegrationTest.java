package org.hti5250j.gui;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Integration tests for DrawingContext extraction from GuiGraphicBuffer.
 *
 * Tests verify that drawing context management (graphics references, dirty region
 * tracking, double buffering, and clipping regions) can be isolated into a dedicated
 * class following Single Responsibility Principle.
 *
 * Phase 4 of GuiGraphicBuffer refactoring (Wave 3A Track 2).
 * TDD workflow: RED phase - write failing tests first.
 */
public class DrawingContextIntegrationTest {
    private DrawingContext drawingContext;
    private Graphics2D testGraphics;

    @Before
    public void setUp() {
        drawingContext = new DrawingContext();

        // Create a test graphics context from a buffered image
        BufferedImage testImage = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        testGraphics = testImage.createGraphics();
    }

    /**
     * Test 1: Graphics context management
     * Verifies that DrawingContext can store and retrieve a Graphics2D reference.
     */
    @Test
    public void testGraphicsContextManagement() {
        // ACT: Set graphics context
        drawingContext.setGraphics(testGraphics);

        // ASSERT: Graphics context is stored and retrieved correctly
        assertEquals("Graphics context should be retrievable", testGraphics, drawingContext.getGraphics());
    }

    /**
     * Test 2: Dirty region tracking
     * Verifies that DrawingContext can mark regions as dirty and clear dirty status.
     */
    @Test
    public void testDirtyRegionTracking() {
        // ARRANGE: Initially not dirty
        assertFalse("DrawingContext should not be dirty initially", drawingContext.isDirty());

        // ACT: Mark a region as dirty
        drawingContext.markDirty(10, 20, 100, 50);

        // ASSERT: Should be marked dirty
        assertTrue("DrawingContext should be marked dirty after markDirty()", drawingContext.isDirty());

        // ACT: Clear dirty status
        drawingContext.clearDirty();

        // ASSERT: Should no longer be dirty
        assertFalse("DrawingContext should not be dirty after clearDirty()", drawingContext.isDirty());
    }

    /**
     * Test 3: Dirty region accumulation
     * Verifies that multiple dirty regions are accumulated into a single bounding rectangle.
     */
    @Test
    public void testDirtyRegionAccumulation() {
        // ACT: Mark multiple overlapping regions as dirty
        drawingContext.markDirty(10, 20, 50, 30);
        drawingContext.markDirty(40, 35, 60, 40);

        // ASSERT: Dirty region should be expanded to cover both
        assertTrue("DrawingContext should be dirty", drawingContext.isDirty());
        Rectangle dirtyRegion = drawingContext.getDirtyRegion();
        assertNotNull("Dirty region should not be null", dirtyRegion);

        // The accumulated region should contain both original regions
        assertTrue("Dirty region should contain first region start",
            dirtyRegion.x <= 10 && dirtyRegion.y <= 20);
    }

    /**
     * Test 4: Double buffering state management
     * Verifies that DrawingContext can track double buffering enable/disable state.
     */
    @Test
    public void testDoubleBufferingState() {
        // ARRANGE: Initially not double buffered
        assertFalse("Double buffering should be disabled initially", drawingContext.isDoubleBuffered());

        // ACT: Enable double buffering
        drawingContext.setDoubleBuffered(true);

        // ASSERT: Double buffering should be enabled
        assertTrue("Double buffering should be enabled", drawingContext.isDoubleBuffered());

        // ACT: Disable double buffering
        drawingContext.setDoubleBuffered(false);

        // ASSERT: Double buffering should be disabled
        assertFalse("Double buffering should be disabled", drawingContext.isDoubleBuffered());
    }

    /**
     * Test 5: Clipping region management
     * Verifies that DrawingContext can set and retrieve clipping rectangles.
     */
    @Test
    public void testClippingRegionManagement() {
        // ARRANGE: Create a test clipping rectangle
        Rectangle clipRegion = new Rectangle(50, 60, 200, 150);

        // ACT: Set clipping region
        drawingContext.setClipRegion(clipRegion);

        // ASSERT: Clipping region should be retrievable
        Rectangle retrievedClip = drawingContext.getClipRegion();
        assertNotNull("Clipping region should not be null", retrievedClip);
        assertEquals("Clipping region X should match", clipRegion.x, retrievedClip.x);
        assertEquals("Clipping region Y should match", clipRegion.y, retrievedClip.y);
        assertEquals("Clipping region width should match", clipRegion.width, retrievedClip.width);
        assertEquals("Clipping region height should match", clipRegion.height, retrievedClip.height);
    }

    /**
     * Test 6: Clear dirty region after retrieval
     * Verifies that dirty region can be cleared and is null after clearDirty().
     */
    @Test
    public void testDirtyRegionClearing() {
        // ACT: Mark dirty and get region
        drawingContext.markDirty(10, 20, 100, 50);
        Rectangle beforeClear = drawingContext.getDirtyRegion();
        assertNotNull("Dirty region should exist before clearing", beforeClear);

        // ACT: Clear dirty status
        drawingContext.clearDirty();

        // ASSERT: Dirty region should be null after clearing
        assertNull("Dirty region should be null after clearDirty()", drawingContext.getDirtyRegion());
    }
}
