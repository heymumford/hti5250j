/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.gui;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import org.hti5250j.CursorManager;

/**
 * Integration tests for CursorManager extraction from GuiGraphicBuffer.
 *
 * Phase 3 of GuiGraphicBuffer refactoring (TDD approach):
 * - Test 1: Cursor position tracking
 * - Test 2: Cursor visibility toggle
 * - Test 3: Cursor blink state management
 * - Test 4: GuiGraphicBuffer delegation to CursorManager
 * - Test 5: Persistent cursor state across operations
 */
public class CursorManagerIntegrationTest {

    private CursorManager cursorManager;

    @Before
    public void setUp() {
        cursorManager = new CursorManager();
    }

    /**
     * Test 1: Cursor position should be tracked and retrievable
     */
    @Test
    public void testCursorPositionTracking() {
        cursorManager.setCursorPosition(10, 20);
        assertEquals("Cursor X position should match", 10, cursorManager.getCursorX());
        assertEquals("Cursor Y position should match", 20, cursorManager.getCursorY());
    }

    /**
     * Test 2: Cursor visibility should be toggleable
     */
    @Test
    public void testCursorVisibilityToggle() {
        cursorManager.setCursorVisible(true);
        assertTrue("Cursor should be visible when set to true", cursorManager.isCursorVisible());

        cursorManager.setCursorVisible(false);
        assertFalse("Cursor should be invisible when set to false", cursorManager.isCursorVisible());
    }

    /**
     * Test 3: Cursor blink state should be manageable
     */
    @Test
    public void testCursorBlinkState() {
        assertFalse("Blink state should start as false", cursorManager.getBlinkState());

        cursorManager.toggleBlink();
        assertTrue("Blink state should be true after first toggle", cursorManager.getBlinkState());

        cursorManager.toggleBlink();
        assertFalse("Blink state should be false after second toggle", cursorManager.getBlinkState());
    }

    /**
     * Test 4: Cursor size presets should be available
     */
    @Test
    public void testCursorSizeSettings() {
        cursorManager.setCursorSize(0); // Line cursor
        assertEquals("Cursor size should be 0 for line", 0, cursorManager.getCursorSize());

        cursorManager.setCursorSize(1); // Half cursor
        assertEquals("Cursor size should be 1 for half", 1, cursorManager.getCursorSize());

        cursorManager.setCursorSize(2); // Full cursor
        assertEquals("Cursor size should be 2 for full", 2, cursorManager.getCursorSize());
    }

    /**
     * Test 5: Cursor state should persist across multiple operations
     */
    @Test
    public void testCursorStatePersistence() {
        cursorManager.setCursorPosition(5, 10);
        cursorManager.setCursorVisible(true);
        cursorManager.setCursorSize(1);

        // Verify state persists
        assertEquals("X position should persist", 5, cursorManager.getCursorX());
        assertEquals("Y position should persist", 10, cursorManager.getCursorY());
        assertTrue("Visibility should persist", cursorManager.isCursorVisible());
        assertEquals("Cursor size should persist", 1, cursorManager.getCursorSize());

        // Change one property
        cursorManager.setCursorPosition(15, 25);

        // Verify only that property changed
        assertEquals("X position should update", 15, cursorManager.getCursorX());
        assertEquals("Y position should update", 25, cursorManager.getCursorY());
        assertTrue("Visibility should still persist", cursorManager.isCursorVisible());
        assertEquals("Cursor size should still persist", 1, cursorManager.getCursorSize());
    }
}
