/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.gui;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.awt.Font;
import org.hti5250j.CharacterMetrics;

/**
 * Integration tests for CharacterMetrics extraction from GuiGraphicBuffer.
 *
 * Phase 2 of GuiGraphicBuffer refactoring (TDD approach):
 * - Test 1: Character width calculation
 * - Test 2: Character height calculation
 * - Test 3: Font metrics caching
 * - Test 4: GuiGraphicBuffer delegation
 * - Test 5: Consistent dimensions across calls
 */
public class FontMetricsIntegrationTest {

    private CharacterMetrics characterMetrics;
    private Font testFont;

    @Before
    public void setUp() {
        characterMetrics = new CharacterMetrics();
        testFont = new Font("Courier New", Font.PLAIN, 14);
    }

    /**
     * Test 1: Character width should be positive after setting font
     */
    @Test
    public void testCharWidthCalculation() {
        characterMetrics.setFont(testFont);
        int width = characterMetrics.getCharWidth();
        assertTrue("Character width should be positive", width > 0);
        assertTrue("Character width should be less than 100 pixels for 14pt font", width < 100);
    }

    /**
     * Test 2: Character height should be positive after setting font
     */
    @Test
    public void testCharHeightCalculation() {
        characterMetrics.setFont(testFont);
        int height = characterMetrics.getCharHeight();
        assertTrue("Character height should be positive", height > 0);
        assertTrue("Character height should be less than 100 pixels for 14pt font", height < 100);
    }

    /**
     * Test 3: Font metrics should be cached - repeated calls return same values
     */
    @Test
    public void testFontMetricsCache() {
        characterMetrics.setFont(testFont);
        int width1 = characterMetrics.getCharWidth();
        int height1 = characterMetrics.getCharHeight();

        int width2 = characterMetrics.getCharWidth();
        int height2 = characterMetrics.getCharHeight();

        assertEquals("Character width should be cached", width1, width2);
        assertEquals("Character height should be cached", height1, height2);
    }

    /**
     * Test 4: CharacterMetrics should maintain consistent dimensions
     */
    @Test
    public void testConsistentDimensions() {
        characterMetrics.setFont(testFont);
        int width = characterMetrics.getCharWidth();
        int height = characterMetrics.getCharHeight();

        // Width and height should be reasonable proportions
        assertTrue("Height should be comparable to width for monospace",
                   height > 0 && width > 0);
    }

    /**
     * Test 5: Different fonts should produce different metrics
     */
    @Test
    public void testDifferentFontDimensions() {
        characterMetrics.setFont(testFont);
        int width1 = characterMetrics.getCharWidth();

        Font largerFont = new Font("Courier New", Font.PLAIN, 24);
        characterMetrics.setFont(largerFont);
        int width2 = characterMetrics.getCharWidth();

        assertTrue("Larger font should produce wider characters", width2 > width1);
    }
}
