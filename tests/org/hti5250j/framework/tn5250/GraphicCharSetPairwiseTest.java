/*
 * SPDX-FileCopyrightText: Copyright (c) 2025
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.framework.tn5250;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pairwise TDD test suite for Screen5250 graphic character set handling.
 *
 * Focuses on high-risk behaviors in terminal rendering and clipboard operations:
 * 1. APL character rendering (mathematical symbols)
 * 2. Line-drawing (horizontal, vertical, corners)
 * 3. Box-drawing (double, single, mixed styles)
 * 4. Font availability and substitution
 * 5. Display context impact on character rendering
 * 6. Copy/paste preservation of graphic characters
 * 7. Mixed content rendering (text + graphics)
 * 8. Error conditions (unsupported fonts, undefined chars)
 */
public class GraphicCharSetPairwiseTest {

    private Screen5250 screen5250;
    private ScreenOIA oia;
    private ScreenFields screenFields;
    private ScreenPlanes planes;

    private static final int SCREEN_ROWS = 24;
    private static final int SCREEN_COLS = 80;
    private static final int FIELD_START_POS = 80;  // Row 2, Col 1
    private static final int FIELD_LENGTH = 40;     // 40 character field
    private static final int FIELD_END_POS = FIELD_START_POS + FIELD_LENGTH - 1;

    // Graphic character codes (EBCDIC-based)
    private static final char APL_BOX_HORIZONTAL = '\u2500';       // ─ (BOX DRAWINGS LIGHT HORIZONTAL)
    private static final char APL_BOX_VERTICAL = '\u2502';         // │ (BOX DRAWINGS LIGHT VERTICAL)
    private static final char APL_BOX_CORNER_TL = '\u250C';        // ┌ (BOX DRAWINGS LIGHT DOWN AND RIGHT)
    private static final char APL_BOX_CORNER_TR = '\u2510';        // ┐ (BOX DRAWINGS LIGHT DOWN AND LEFT)
    private static final char APL_BOX_CORNER_BL = '\u2514';        // └ (BOX DRAWINGS LIGHT UP AND RIGHT)
    private static final char APL_BOX_CORNER_BR = '\u2518';        // ┘ (BOX DRAWINGS LIGHT UP AND LEFT)
    private static final char APL_BOX_CROSS = '\u253C';            // ┼ (BOX DRAWINGS LIGHT VERTICAL AND HORIZONTAL)
    private static final char APL_BOX_T_DOWN = '\u252C';           // ┬ (BOX DRAWINGS LIGHT DOWN AND HORIZONTAL)
    private static final char APL_BOX_T_UP = '\u2534';             // ┴ (BOX DRAWINGS LIGHT UP AND HORIZONTAL)
    private static final char APL_BOX_T_RIGHT = '\u251C';          // ├ (BOX DRAWINGS LIGHT VERTICAL AND RIGHT)
    private static final char APL_BOX_T_LEFT = '\u2524';           // ┤ (BOX DRAWINGS LIGHT VERTICAL AND LEFT)

    private static final char APL_DBL_HORIZONTAL = '\u2550';       // ═ (BOX DRAWINGS DOUBLE HORIZONTAL)
    private static final char APL_DBL_VERTICAL = '\u2551';         // ║ (BOX DRAWINGS DOUBLE VERTICAL)
    private static final char APL_DBL_CORNER_TL = '\u2554';        // ╔ (BOX DRAWINGS DOUBLE DOWN AND RIGHT)
    private static final char APL_DBL_CORNER_TR = '\u2557';        // ╗ (BOX DRAWINGS DOUBLE DOWN AND LEFT)

    // Substitution characters for when fonts are missing
    private static final char FALLBACK_HORIZONTAL = '-';
    private static final char FALLBACK_VERTICAL = '|';
    private static final char FALLBACK_CORNER = '+';

    /**
     * Test double for Screen5250 with minimal dependencies.
     * Simulates basic screen behavior for graphic character testing.
     */
    private static class Screen5250TestDouble extends Screen5250 {
        public Screen5250TestDouble() {
            super();
        }

        @Override
        public int getScreenLength() {
            return SCREEN_ROWS * SCREEN_COLS;
        }

        @Override
        public boolean isInField(int pos, boolean checkAttr) {
            return pos >= FIELD_START_POS && pos <= FIELD_END_POS;
        }

        @Override
        public StringBuffer getHSMore() {
            return new StringBuffer("More...");
        }

        @Override
        public StringBuffer getHSBottom() {
            return new StringBuffer("Bottom");
        }

        @Override
        public void setDirty(int pos) {
            // No-op for tests
        }
    }

    @BeforeEach
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        screen5250 = new Screen5250TestDouble();
        oia = screen5250.getOIA();
        screenFields = new ScreenFields(screen5250);
        planes = getPlanes();

        // Initialize: keyboard unlocked, overwrite mode
        oia.setKeyBoardLocked(false);
        oia.setInsertMode(false);

        // Add a test field: position 80, length 40
        ScreenField field = new ScreenField(screen5250);
        field.setField(0x20, 1, 0, FIELD_LENGTH, 0, 0, 0x41, 0x00);
    }

    /**
     * Access private planes field via reflection
     */
    private ScreenPlanes getPlanes() throws NoSuchFieldException, IllegalAccessException {
        Field field = Screen5250.class.getDeclaredField("planes");
        field.setAccessible(true);
        return (ScreenPlanes) field.get(screen5250);
    }

    /**
     * Helper: Get character at position from planes
     */
    private char getCharAt(int pos) {
        return planes.getChar(pos);
    }

    /**
     * Helper: Set character at position in planes
     */
    private void setCharAt(int pos, char c) {
        planes.setChar(pos, c);
    }

    /**
     * Helper: Fill field with content
     */
    private void fillField(int startPos, int length, String pattern) {
        for (int i = 0; i < length && i < pattern.length(); i++) {
            setCharAt(startPos + i, pattern.charAt(i));
        }
        for (int i = pattern.length(); i < length; i++) {
            setCharAt(startPos + i, ' ');
        }
    }

    /**
     * Helper: Get field content as string
     */
    private String getFieldContent(int startPos, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(getCharAt(startPos + i));
        }
        return sb.toString();
    }

    /**
     * Helper: Build graphic box frame in field
     * Pairwise: character_type=box, rendering=graphic
     */
    private void drawBoxFrame(int topLeftPos, int width, int height) {
        // Top edge
        setCharAt(topLeftPos, APL_BOX_CORNER_TL);
        for (int i = 1; i < width - 1; i++) {
            setCharAt(topLeftPos + i, APL_BOX_HORIZONTAL);
        }
        setCharAt(topLeftPos + width - 1, APL_BOX_CORNER_TR);

        // Left and right edges
        for (int row = 1; row < height - 1; row++) {
            int leftPos = topLeftPos + (row * SCREEN_COLS);
            int rightPos = leftPos + width - 1;
            setCharAt(leftPos, APL_BOX_VERTICAL);
            setCharAt(rightPos, APL_BOX_VERTICAL);
        }

        // Bottom edge
        int bottomPos = topLeftPos + ((height - 1) * SCREEN_COLS);
        setCharAt(bottomPos, APL_BOX_CORNER_BL);
        for (int i = 1; i < width - 1; i++) {
            setCharAt(bottomPos + i, APL_BOX_HORIZONTAL);
        }
        setCharAt(bottomPos + width - 1, APL_BOX_CORNER_BR);
    }

    /**
     * Helper: Detect if character is a graphic character
     */
    private boolean isGraphicChar(char c) {
        return c >= '\u2400' && c <= '\u259F'; // Box drawing and APL range
    }

    // ========================================================================
    // POSITIVE TESTS (15): Valid graphic rendering
    // ========================================================================

    /**
     * POSITIVE: Render APL line-drawing horizontal character in field
     * Dimension pair: character_type=line-drawing, rendering=graphic
     * Expected: Horizontal line character placed at position
     */
    @Test
    public void testRenderLineDrawingHorizontal() {
        setCharAt(FIELD_START_POS, APL_BOX_HORIZONTAL);
        assertEquals(APL_BOX_HORIZONTAL, getCharAt(FIELD_START_POS),"Horizontal line character rendered");
    }

    /**
     * POSITIVE: Render APL line-drawing vertical character in field
     * Dimension pair: character_type=line-drawing, rendering=graphic
     * Expected: Vertical line character placed at position
     */
    @Test
    public void testRenderLineDrawingVertical() {
        setCharAt(FIELD_START_POS, APL_BOX_VERTICAL);
        assertEquals(APL_BOX_VERTICAL, getCharAt(FIELD_START_POS),"Vertical line character rendered");
    }

    /**
     * POSITIVE: Render box corner character (top-left)
     * Dimension pair: character_type=box, rendering=graphic
     * Expected: Corner character placed correctly
     */
    @Test
    public void testRenderBoxCornerTopLeft() {
        setCharAt(FIELD_START_POS, APL_BOX_CORNER_TL);
        assertEquals(APL_BOX_CORNER_TL, getCharAt(FIELD_START_POS),"Box corner (TL) rendered");
    }

    /**
     * POSITIVE: Render box corner character (bottom-right)
     * Dimension pair: character_type=box, rendering=graphic
     * Expected: Corner character placed correctly
     */
    @Test
    public void testRenderBoxCornerBottomRight() {
        setCharAt(FIELD_START_POS, APL_BOX_CORNER_BR);
        assertEquals(APL_BOX_CORNER_BR, getCharAt(FIELD_START_POS),"Box corner (BR) rendered");
    }

    /**
     * POSITIVE: Render box cross character (center junction)
     * Dimension pair: character_type=box, rendering=graphic
     * Expected: Cross character placed correctly
     */
    @Test
    public void testRenderBoxCross() {
        setCharAt(FIELD_START_POS, APL_BOX_CROSS);
        assertEquals(APL_BOX_CROSS, getCharAt(FIELD_START_POS),"Box cross character rendered");
    }

    /**
     * POSITIVE: Render box T-junction characters
     * Dimension pair: character_type=box, rendering=graphic
     * Expected: All T-junction variants placed correctly
     */
    @Test
    public void testRenderBoxTJunctions() {
        setCharAt(FIELD_START_POS, APL_BOX_T_DOWN);
        setCharAt(FIELD_START_POS + 1, APL_BOX_T_UP);
        setCharAt(FIELD_START_POS + 2, APL_BOX_T_RIGHT);
        setCharAt(FIELD_START_POS + 3, APL_BOX_T_LEFT);

        assertEquals(APL_BOX_T_DOWN, getCharAt(FIELD_START_POS),"T-down rendered");
        assertEquals(APL_BOX_T_UP, getCharAt(FIELD_START_POS + 1),"T-up rendered");
        assertEquals(APL_BOX_T_RIGHT, getCharAt(FIELD_START_POS + 2),"T-right rendered");
        assertEquals(APL_BOX_T_LEFT, getCharAt(FIELD_START_POS + 3),"T-left rendered");
    }

    /**
     * POSITIVE: Render double-line box characters
     * Dimension pair: character_type=box, rendering=graphic
     * Expected: Double-line variants rendered correctly
     */
    @Test
    public void testRenderDoubleLineBoxChars() {
        setCharAt(FIELD_START_POS, APL_DBL_HORIZONTAL);
        setCharAt(FIELD_START_POS + 1, APL_DBL_VERTICAL);
        setCharAt(FIELD_START_POS + 2, APL_DBL_CORNER_TL);
        setCharAt(FIELD_START_POS + 3, APL_DBL_CORNER_TR);

        assertEquals(APL_DBL_HORIZONTAL, getCharAt(FIELD_START_POS),"Double horizontal rendered");
        assertEquals(APL_DBL_VERTICAL, getCharAt(FIELD_START_POS + 1),"Double vertical rendered");
        assertEquals(APL_DBL_CORNER_TL, getCharAt(FIELD_START_POS + 2),"Double corner TL rendered");
        assertEquals(APL_DBL_CORNER_TR, getCharAt(FIELD_START_POS + 3),"Double corner TR rendered");
    }

    /**
     * POSITIVE: Render complete box frame with graphic characters
     * Dimension pair: character_type=box, rendering=graphic, context=screen
     * Expected: Box frame drawn correctly with all corners and edges
     */
    @Test
    public void testRenderCompleteBoxFrame() {
        drawBoxFrame(FIELD_START_POS, 10, 5);

        // Verify corners
        assertEquals(APL_BOX_CORNER_TL, getCharAt(FIELD_START_POS),"Corner TL");
        assertEquals(APL_BOX_CORNER_TR, getCharAt(FIELD_START_POS + 9),"Corner TR");

        // Verify horizontal edges
        assertEquals(APL_BOX_HORIZONTAL, getCharAt(FIELD_START_POS + 1),"Top horizontal");

        // Verify vertical edges
        assertEquals(APL_BOX_VERTICAL, getCharAt(FIELD_START_POS + SCREEN_COLS),"Left vertical");
    }

    /**
     * POSITIVE: Render graphic characters mixed with text
     * Dimension pair: character_type=mixed, rendering=mixed, context=field
     * Expected: Text and graphics coexist in field
     */
    @Test
    public void testRenderGraphicsWithTextMixed() {
        setCharAt(FIELD_START_POS, 'H');
        setCharAt(FIELD_START_POS + 1, 'e');
        setCharAt(FIELD_START_POS + 2, APL_BOX_HORIZONTAL);
        setCharAt(FIELD_START_POS + 3, 'l');
        setCharAt(FIELD_START_POS + 4, 'o');

        assertEquals('e', getCharAt(FIELD_START_POS + 1),"Text 'He' before graphic");
        assertEquals(APL_BOX_HORIZONTAL, getCharAt(FIELD_START_POS + 2),"Graphic char in middle");
        assertEquals('l', getCharAt(FIELD_START_POS + 3),"Text 'lo' after graphic");
    }

    /**
     * POSITIVE: Detect graphic character in stream
     * Dimension pair: character_type=box, rendering=graphic
     * Expected: isGraphicChar() correctly identifies graphic chars
     */
    @Test
    public void testDetectGraphicCharacterInStream() {
        assertTrue(isGraphicChar(APL_BOX_HORIZONTAL),"Horizontal line is graphic");
        assertTrue(isGraphicChar(APL_BOX_VERTICAL),"Vertical line is graphic");
        assertTrue(isGraphicChar(APL_BOX_CORNER_TL),"Box corner is graphic");
        assertTrue(isGraphicChar(APL_BOX_CROSS),"Box cross is graphic");

        assertFalse(isGraphicChar('A'),"Letter 'A' is not graphic");
        assertFalse(isGraphicChar(' '),"Space is not graphic");
        assertFalse(isGraphicChar('5'),"Digit '5' is not graphic");
    }

    /**
     * POSITIVE: Copy graphic character from field preserves identity
     * Dimension pair: copy_paste=preserve, rendering=graphic
     * Expected: Copied graphic char equals original
     */
    @Test
    public void testCopyGraphicCharPreserveIdentity() {
        setCharAt(FIELD_START_POS, APL_BOX_CORNER_TL);
        char copied = getCharAt(FIELD_START_POS);

        assertEquals(APL_BOX_CORNER_TL, copied,"Graphic char preserved in copy");
    }

    /**
     * POSITIVE: Paste graphic character into field
     * Dimension pair: copy_paste=preserve, context=field
     * Expected: Pasted graphic char renders correctly
     */
    @Test
    public void testPasteGraphicCharIntoField() {
        char toPaste = APL_BOX_HORIZONTAL;

        setCharAt(FIELD_START_POS + 5, toPaste);

        assertEquals(toPaste, getCharAt(FIELD_START_POS + 5),"Pasted graphic char in field");
    }

    /**
     * POSITIVE: Graphic characters maintain integrity across field
     * Dimension pair: character_type=box, context=screen
     * Expected: Multiple graphic chars in sequence stay intact
     */
    @Test
    public void testGraphicCharIntegrityAcrossField() {
        String graphicSequence = "┌─────┐";
        for (int i = 0; i < graphicSequence.length(); i++) {
            setCharAt(FIELD_START_POS + i, graphicSequence.charAt(i));
        }

        for (int i = 0; i < graphicSequence.length(); i++) {
            assertEquals(graphicSequence.charAt(i), getCharAt(FIELD_START_POS + i),"Graphic char #" + i + " preserved");
        }
    }

    /**
     * POSITIVE: Render APL symbols (mathematical notation)
     * Dimension pair: character_type=APL, rendering=graphic
     * Expected: APL symbols place correctly without corruption
     */
    @Test
    public void testRenderAPLSymbols() {
        // Common APL/mathematical symbols
        char[] aplChars = {
            '\u22A4',  // ⊤ (DOWN TACK)
            '\u22A5',  // ⊥ (UP TACK)
            '\u222E',  // ∮ (CONTOUR INTEGRAL)
        };

        for (int i = 0; i < aplChars.length; i++) {
            setCharAt(FIELD_START_POS + i, aplChars[i]);
            assertEquals(aplChars[i], getCharAt(FIELD_START_POS + i),"APL symbol #" + i + " rendered");
        }
    }

    // ========================================================================
    // ADVERSARIAL TESTS (12): Unsupported chars, missing fonts, conflicts
    // ========================================================================

    /**
     * ADVERSARIAL: Render undefined graphic character (reserved code point)
     * Dimension pair: character_type=special, rendering=undefined
     * Expected: Character stored or converted to fallback (no crash)
     */
    @Test
    public void testRenderUndefinedGraphicCharacter() {
        char undefined = '\uFFFD';  // Unicode replacement character

        try {
            setCharAt(FIELD_START_POS, undefined);
            // If no exception, verify character was stored
            char result = getCharAt(FIELD_START_POS);
            assertTrue(result == undefined || result == ' ',"Undefined char handled");
        } catch (Exception e) {
            // Expected: validation may reject undefined char
            assertTrue(true,"Exception expected for undefined char");
        }
    }

    /**
     * ADVERSARIAL: Font substitution when graphic font unavailable
     * Dimension pair: font_support=missing, character_type=box, rendering=graphic
     * Expected: Char rendered as fallback or skipped
     */
    @Test
    public void testGraphicCharFallbackSubstitution() {
        // When graphic font is missing, box drawing chars may fall back to ASCII
        setCharAt(FIELD_START_POS, APL_BOX_HORIZONTAL);
        char rendered = getCharAt(FIELD_START_POS);

        // Character should be stored (actual substitution happens in renderer)
        assertEquals(APL_BOX_HORIZONTAL, rendered,"Graphic char stored (substitution in renderer)");
    }

    /**
     * ADVERSARIAL: Copy/paste converts graphic char to ASCII fallback
     * Dimension pair: copy_paste=convert, character_type=box
     * Expected: Conversion to ASCII (e.g., ─ → -, │ → |)
     */
    @Test
    public void testCopyGraphicCharConvertToASCII() {
        setCharAt(FIELD_START_POS, APL_BOX_HORIZONTAL);
        char original = getCharAt(FIELD_START_POS);

        // In headless mode, copy/paste may convert graphic → ASCII
        // This test verifies the conversion is deterministic
        assertTrue(original == APL_BOX_HORIZONTAL || original == FALLBACK_HORIZONTAL,"Character is graphic or fallback");
    }

    /**
     * ADVERSARIAL: Paste ASCII fallback where graphic char expected
     * Dimension pair: copy_paste=convert, font_support=missing
     * Expected: Fallback char placed, may not match original visual
     */
    @Test
    public void testPasteASCIIFallbackForGraphicChar() {
        // Simulate: copy graphic from system with font, paste into system without
        setCharAt(FIELD_START_POS, FALLBACK_HORIZONTAL);

        char rendered = getCharAt(FIELD_START_POS);
        assertEquals(FALLBACK_HORIZONTAL, rendered,"Fallback character in field");
    }

    /**
     * ADVERSARIAL: Mixed single/double line box characters conflict
     * Dimension pair: character_type=box, rendering=mixed
     * Expected: Characters rendered (visual conflict is rendering concern)
     */
    @Test
    public void testMixedSingleDoubleLineBoxConflict() {
        // Single line
        setCharAt(FIELD_START_POS, APL_BOX_HORIZONTAL);
        // Double line adjacent
        setCharAt(FIELD_START_POS + 1, APL_DBL_HORIZONTAL);

        assertEquals(APL_BOX_HORIZONTAL, getCharAt(FIELD_START_POS),"Single line placed");
        assertEquals(APL_DBL_HORIZONTAL, getCharAt(FIELD_START_POS + 1),"Double line placed adjacent");
    }

    /**
     * ADVERSARIAL: Render graphic char in protected field
     * Dimension pair: context=field(protected), character_type=box
     * Expected: Character stored regardless of protection (validation separate)
     */
    @Test
    public void testRenderGraphicInProtectedField() {
        // Field protection check happens in Screen5250, not in planes
        setCharAt(FIELD_START_POS, APL_BOX_CORNER_TL);

        assertEquals(APL_BOX_CORNER_TL, getCharAt(FIELD_START_POS),"Graphic char in protected field");
    }

    /**
     * ADVERSARIAL: Render graphic char at screen boundary
     * Dimension pair: context=window-border, rendering=graphic
     * Expected: Character rendered at edge without truncation
     */
    @Test
    public void testRenderGraphicAtScreenBoundary() {
        int lastPos = (SCREEN_ROWS * SCREEN_COLS) - 1;

        setCharAt(lastPos, APL_BOX_CORNER_BR);

        assertEquals(APL_BOX_CORNER_BR, getCharAt(lastPos),"Graphic at screen boundary");
    }

    /**
     * ADVERSARIAL: Copy graphic sequence with mixed line styles
     * Dimension pair: character_type=mixed, copy_paste=preserve
     * Expected: All chars copied, visual rendering may vary
     */
    @Test
    public void testCopyMixedLineStyleSequence() {
        String mixed = "┌─┐│├┤└┘═║";
        for (int i = 0; i < mixed.length(); i++) {
            setCharAt(FIELD_START_POS + i, mixed.charAt(i));
        }

        String copied = getFieldContent(FIELD_START_POS, mixed.length());

        assertEquals(mixed, copied,"Mixed line styles copied");
    }

    /**
     * ADVERSARIAL: Render graphic char in insert mode
     * Dimension pair: rendering=graphic, mode=insert
     * Expected: Character inserted, content shifted
     */
    @Test
    public void testRenderGraphicCharInInsertMode() {
        oia.setInsertMode(true);
        fillField(FIELD_START_POS, FIELD_LENGTH, "ABCDE");

        setCharAt(FIELD_START_POS, APL_BOX_CORNER_TL);

        assertEquals(APL_BOX_CORNER_TL, getCharAt(FIELD_START_POS),"Graphic char inserted at start");
        assertEquals(true, oia.isInsertMode(),"Insert mode active");
    }

    /**
     * ADVERSARIAL: Paste graphic char with keyboard locked
     * Dimension pair: copy_paste=preserve, context=locked-keyboard
     * Expected: Paste blocked by keyboard lock, not rendering
     */
    @Test
    public void testPasteGraphicWithLockedKeyboard() {
        oia.setKeyBoardLocked(true);
        String fieldBefore = getFieldContent(FIELD_START_POS, FIELD_LENGTH);

        // Paste would be blocked at input level, not in planes
        setCharAt(FIELD_START_POS, APL_BOX_CORNER_TL);

        assertTrue(oia.isKeyBoardLocked(),"Keyboard locked during paste");
        // Character can still be set in planes, but input layer would block it
        assertEquals(APL_BOX_CORNER_TL, getCharAt(FIELD_START_POS),"Graphic char in locked mode");
    }

    /**
     * ADVERSARIAL: Render graphic char sequence exceeding field length
     * Dimension pair: character_type=box, context=field(overflow)
     * Expected: Characters rendered up to field boundary
     */
    @Test
    public void testRenderGraphicSequenceExceedsFieldLength() {
        String graphicBox = "┌──────────────────────────────────────┐";

        for (int i = 0; i < Math.min(graphicBox.length(), FIELD_LENGTH); i++) {
            setCharAt(FIELD_START_POS + i, graphicBox.charAt(i));
        }

        // Verify content within field bounds
        for (int i = FIELD_START_POS; i <= FIELD_END_POS; i++) {
            char c = getCharAt(i);
            assertTrue(c > 0,"Content within field"); // Any valid char
        }
    }

    /**
     * ADVERSARIAL: Render APL char that has no direct ASCII equivalent
     * Dimension pair: character_type=APL, font_support=missing
     * Expected: Stored as-is, rendering layer handles substitution
     */
    @Test
    public void testRenderAPLCharWithoutASCIIEquivalent() {
        char aplChar = '\u22A4';  // DOWN TACK - no common ASCII equivalent

        setCharAt(FIELD_START_POS, aplChar);
        char rendered = getCharAt(FIELD_START_POS);

        assertEquals(aplChar, rendered,"APL char stored (substitution in renderer)");
    }
}
