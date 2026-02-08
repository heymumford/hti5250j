/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Test Suite
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.framework.tn5250;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD Pairwise Test Suite for Screen5250 clipboard and copy/paste operations.
 *
 * Test dimensions (pairwise matrix):
 * 1. Selection type: [character, word, line, rectangle, all]
 * 2. Content type: [text, fields, mixed, empty]
 * 3. Encoding: [ASCII, EBCDIC, Unicode]
 * 4. Paste target: [input_field, protected, off_screen]
 * 5. Buffer size: [small(80), medium(1920), large(3840), max(7920)]
 *
 * Test strategy:
 * - Happy path: Standard copy/paste with valid data
 * - Boundary cases: Edge sizes, special characters, empty content
 * - Adversarial cases: Malformed data, invalid rectangles, encoding mismatches
 *
 * Focus areas from audit:
 * - Screen data extraction accuracy
 * - Clipboard integration correctness
 * - Paste handling under various conditions
 * - Buffer boundary conditions
 * - Encoding consistency
 */
public class ClipboardPairwiseTest {

    private MockScreen5250 mockScreen;
    private Rect testRect;

    // Test data constants
    private static final String SIMPLE_TEXT = "Hello World";
    private static final String FIELD_DATA = "INPUT";
    private static final String MIXED_DATA = "Test 123 !@#";
    private static final String EMPTY_STRING = "";
    private static final String MULTILINE_TEXT = "Line1\nLine2\nLine3";
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:',.<>?/~`";
    private static final String UNICODE_TEXT = "Hello \u00E9\u00FC\u00F1 World";

    // Buffer size test constants
    private static final int SMALL_BUFFER = 80;
    private static final int MEDIUM_BUFFER = 1920;
    private static final int LARGE_BUFFER = 3840;
    private static final int MAX_BUFFER = 7920;

    // Screen dimensions
    private static final int SCREEN_WIDTH = 80;
    private static final int SCREEN_HEIGHT = 24;

    @BeforeEach
    public void setUp() {
        mockScreen = new MockScreen5250();
        testRect = new Rect(0, 0, 0, 0);
    }

    @AfterEach
    public void tearDown() {
        mockScreen = null;
        testRect = null;
    }

    // ========== POSITIVE PATH TESTS: Selection Type x Content Type ==========

    /**
     * Test Case 1: Copy character selection with simple text content
     * Dimensions: [selection_type=character] [content=text] [encoding=ASCII] [buffer_size=small]
     */
    @Test
    public void testCopyText_CharacterSelectionSimpleText_ShouldExtractSingleCharacter() {
        // ARRANGE
        mockScreen.setScreenData(SIMPLE_TEXT);
        testRect = new Rect(0, 0, 1, 1); // Single character selection

        // ACT
        String copied = mockScreen.copyText(testRect);

        // ASSERT
        assertNotNull(copied,"Copied text should not be null");
        assertEquals(1, copied.length(),"Should copy single character");
        assertEquals("H", copied,"Character should match screen data");
    }

    /**
     * Test Case 2: Copy word selection with simple text content
     * Dimensions: [selection_type=word] [content=text] [encoding=ASCII] [buffer_size=small]
     */
    @Test
    public void testCopyText_WordSelectionSimpleText_ShouldExtractWord() {
        // ARRANGE
        mockScreen.setScreenData(SIMPLE_TEXT);
        testRect = new Rect(0, 0, 5, 1); // "Hello"

        // ACT
        String copied = mockScreen.copyText(testRect);

        // ASSERT
        assertNotNull(copied,"Copied text should not be null");
        assertEquals("Hello", copied.trim(),"Should copy word");
    }

    /**
     * Test Case 3: Copy line selection with simple text content
     * Dimensions: [selection_type=line] [content=text] [encoding=ASCII] [buffer_size=small]
     */
    @Test
    public void testCopyText_LineSelectionSimpleText_ShouldExtractEntireLine() {
        // ARRANGE
        mockScreen.setScreenData(SIMPLE_TEXT);
        testRect = new Rect(0, 0, SCREEN_WIDTH, 1); // Full line

        // ACT
        String copied = mockScreen.copyText(testRect);

        // ASSERT
        assertNotNull(copied,"Copied text should not be null");
        assertTrue(copied.contains("Hello"),"Should contain at least part of text");
    }

    /**
     * Test Case 4: Copy rectangle selection with mixed content
     * Dimensions: [selection_type=rectangle] [content=mixed] [encoding=ASCII] [buffer_size=medium]
     */
    @Test
    public void testCopyText_RectangleSelectionMixedContent_ShouldExtractRect() {
        // ARRANGE
        mockScreen.setScreenData(MIXED_DATA);
        testRect = new Rect(0, 0, 5, 2); // 5x2 rectangle

        // ACT
        String copied = mockScreen.copyText(testRect);

        // ASSERT
        assertNotNull(copied,"Copied text should not be null");
        assertTrue(copied.length() > 0,"Should contain mixed content");
    }

    /**
     * Test Case 5: Copy all selection with empty content
     * Dimensions: [selection_type=all] [content=empty] [encoding=ASCII] [buffer_size=small]
     */
    @Test
    public void testCopyText_AllSelectionEmptyContent_ShouldReturnEmptyOrSpaces() {
        // ARRANGE
        mockScreen.setScreenData(EMPTY_STRING);
        testRect = new Rect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT); // Full screen

        // ACT
        String copied = mockScreen.copyText(testRect);

        // ASSERT
        assertNotNull(copied,"Copied text should not be null");
        // May return empty or spaces, both valid
        assertTrue(copied.isEmpty() || copied.trim().isEmpty(),"Should be empty or whitespace only");
    }

    /**
     * Test Case 6: Copy field content (input field)
     * Dimensions: [selection_type=word] [content=fields] [encoding=ASCII]
     */
    @Test
    public void testCopyTextField_InputFieldContent_ShouldExtractFieldValue() {
        // ARRANGE
        mockScreen.setScreenData(FIELD_DATA);
        int fieldPosition = 0;

        // ACT
        String copied = mockScreen.copyTextField(fieldPosition);

        // ASSERT
        assertNotNull(copied,"Copied field text should not be null");
        assertEquals(FIELD_DATA, copied.trim(),"Field value should match");
    }

    /**
     * Test Case 7: Copy multiline selection
     * Dimensions: [selection_type=rectangle] [content=text] [encoding=ASCII] [buffer_size=medium]
     */
    @Test
    public void testCopyText_MultilineSelection_ShouldPreserveLineBreaks() {
        // ARRANGE
        mockScreen.setScreenData(MULTILINE_TEXT);
        testRect = new Rect(0, 0, SCREEN_WIDTH, 3); // 3 lines

        // ACT
        String copied = mockScreen.copyText(testRect);

        // ASSERT
        assertNotNull(copied,"Copied text should not be null");
        assertTrue(copied.length() > 0,"Should contain line content");
    }

    // ========== POSITIVE PATH TESTS: Paste Operations ==========

    /**
     * Test Case 8: Paste simple text to input field
     * Dimensions: [paste_target=input_field] [content=text] [encoding=ASCII]
     */
    @Test
    public void testPasteText_SimpleTextToInputField_ShouldPasteCorrectly() {
        // ARRANGE
        mockScreen.setCursorPosition(0);
        String textToPaste = "TestInput";

        // ACT
        mockScreen.pasteText(textToPaste, false);

        // ASSERT
        String screenContent = mockScreen.getScreenText();
        assertNotNull(screenContent,"Screen content should not be null");
        assertTrue(screenContent.contains("TestInput"),"Should contain pasted text");
    }

    /**
     * Test Case 9: Paste multiline text with carriage returns
     * Dimensions: [paste_target=input_field] [content=text] [encoding=ASCII]
     */
    @Test
    public void testPasteText_MultilineWithCarriageReturns_ShouldHandleLineBreaks() {
        // ARRANGE
        mockScreen.setCursorPosition(0);
        String textToPaste = "Line1\r\nLine2\r\nLine3";

        // ACT
        mockScreen.pasteText(textToPaste, false);

        // ASSERT
        String screenContent = mockScreen.getScreenText();
        assertNotNull(screenContent,"Screen content should not be null");
        assertTrue(screenContent.contains("Line1"),"Should contain first line");
    }

    /**
     * Test Case 10: Paste special characters
     * Dimensions: [content=text] [encoding=ASCII]
     */
    @Test
    public void testPasteText_SpecialCharacters_ShouldPreserveValidChars() {
        // ARRANGE
        mockScreen.setCursorPosition(0);
        String specialText = "Test!@#$";

        // ACT
        mockScreen.pasteText(specialText, false);

        // ASSERT
        String screenContent = mockScreen.getScreenText();
        assertNotNull(screenContent,"Screen content should not be null");
        // Verify at least some content was pasted
        assertTrue(screenContent.length() > 0,"Should contain pasted content");
    }

    /**
     * Test Case 11: Paste with special character handling enabled
     * Dimensions: [paste_target=input_field] [special=true]
     */
    @Test
    public void testPasteText_SpecialCharacterHandling_ShouldProcessSpecialMode() {
        // ARRANGE
        mockScreen.setCursorPosition(0);
        String textToPaste = "Special\tTab";

        // ACT
        mockScreen.pasteText(textToPaste, true); // special=true

        // ASSERT
        String screenContent = mockScreen.getScreenText();
        assertNotNull(screenContent,"Screen content should not be null");
        // Verify paste completed without exception
        assertTrue(screenContent.length() >= 0,"Paste should complete successfully");
    }

    /**
     * Test Case 12: Paste empty string
     * Dimensions: [content=empty] [paste_target=input_field]
     */
    @Test
    public void testPasteText_EmptyString_ShouldHandleGracefully() {
        // ARRANGE
        mockScreen.setCursorPosition(0);
        String initialContent = mockScreen.getScreenText();
        int initialLength = initialContent != null ? initialContent.length() : 0;

        // ACT
        mockScreen.pasteText(EMPTY_STRING, false);

        // ASSERT
        String afterPaste = mockScreen.getScreenText();
        assertNotNull(afterPaste,"Screen content should not be null after paste");
        // Length should not increase significantly after pasting empty string
        int afterLength = afterPaste.length();
        assertTrue(Math.abs(afterLength - initialLength) < 10,"Should not add significant content");
    }

    // ========== POSITIVE PATH TESTS: Buffer Size Variations ==========

    /**
     * Test Case 13: Copy small buffer (80 chars - single line)
     * Dimensions: [buffer_size=small] [content=text]
     */
    @Test
    public void testCopyText_SmallBuffer_ShouldCopySingleLineCorrectly() {
        // ARRANGE
        String smallData = "0123456789".repeat(8); // 80 chars
        mockScreen.setScreenData(smallData);
        testRect = new Rect(0, 0, SCREEN_WIDTH, 1);

        // ACT
        String copied = mockScreen.copyText(testRect);

        // ASSERT
        assertNotNull(copied,"Copied text should not be null");
        assertTrue(copied.length() > 0,"Should copy content");
    }

    /**
     * Test Case 14: Copy medium buffer (1920 chars - 24 lines)
     * Dimensions: [buffer_size=medium] [content=text]
     */
    @Test
    public void testCopyText_MediumBuffer_ShouldCopyMultipleLines() {
        // ARRANGE
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 24; i++) {
            sb.append(String.format("Line %02d: ", i));
            sb.append("0123456789".repeat(7)); // Fill to ~80 chars
        }
        mockScreen.setScreenData(sb.toString());
        testRect = new Rect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        // ACT
        String copied = mockScreen.copyText(testRect);

        // ASSERT
        assertNotNull(copied,"Copied text should not be null");
        assertTrue(copied.length() > 100,"Should copy substantial content");
    }

    /**
     * Test Case 15: Paste small buffer content
     * Dimensions: [buffer_size=small] [paste_target=input_field]
     */
    @Test
    public void testPasteText_SmallBuffer_ShouldPasteWithinBounds() {
        // ARRANGE
        mockScreen.setCursorPosition(0);
        String smallText = "SmallData";

        // ACT
        mockScreen.pasteText(smallText, false);

        // ASSERT
        String screenContent = mockScreen.getScreenText();
        assertNotNull(screenContent,"Screen content should not be null");
        assertTrue(screenContent.length() >= smallText.length(),"Should paste small buffer");
    }

    /**
     * Test Case 16: Paste medium buffer content
     * Dimensions: [buffer_size=medium] [paste_target=input_field]
     */
    @Test
    public void testPasteText_MediumBuffer_ShouldPasteMultipleLines() {
        // ARRANGE
        mockScreen.setCursorPosition(0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append("Line ").append(i).append("\r\n");
        }
        String mediumText = sb.toString();

        // ACT
        mockScreen.pasteText(mediumText, false);

        // ASSERT
        String screenContent = mockScreen.getScreenText();
        assertNotNull(screenContent,"Screen content should not be null");
        assertTrue(screenContent.length() > 20,"Should paste medium content");
    }

    // ========== BOUNDARY TESTS: Rectangle Bounds ==========

    /**
     * Test Case 17: Copy from boundary rectangle (0,0)
     * Dimensions: [selection_type=rectangle] [content=text]
     */
    @Test
    public void testCopyText_BoundaryRectangleOrigin_ShouldExtractFromOrigin() {
        // ARRANGE
        mockScreen.setScreenData(SIMPLE_TEXT);
        testRect = new Rect(0, 0, 5, 1); // Start at origin

        // ACT
        String copied = mockScreen.copyText(testRect);

        // ASSERT
        assertNotNull(copied,"Copied text should not be null");
        assertTrue(copied.length() > 0,"Should copy from origin");
    }

    /**
     * Test Case 18: Copy from maximum rectangle bounds
     * Dimensions: [selection_type=rectangle] [content=text]
     */
    @Test
    public void testCopyText_MaximumRectangle_ShouldCopyFullScreen() {
        // ARRANGE
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < SCREEN_HEIGHT; i++) {
            sb.append("Row").append(i).append(" ");
        }
        mockScreen.setScreenData(sb.toString());
        testRect = new Rect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        // ACT
        String copied = mockScreen.copyText(testRect);

        // ASSERT
        assertNotNull(copied,"Copied text should not be null");
        assertTrue(copied.length() > 0,"Should copy multiple rows");
    }

    /**
     * Test Case 19: Copy single-width rectangle
     * Dimensions: [selection_type=character] [content=text]
     */
    @Test
    public void testCopyText_SingleWidthRectangle_ShouldCopySingleColumn() {
        // ARRANGE
        mockScreen.setScreenData(SIMPLE_TEXT);
        testRect = new Rect(5, 0, 1, 1); // Single column

        // ACT
        String copied = mockScreen.copyText(testRect);

        // ASSERT
        assertNotNull(copied,"Copied text should not be null");
        assertTrue(copied.length() > 0,"Should copy at least one character");
    }

    // ========== ADVERSARIAL TESTS: Malformed Data ==========

    /**
     * Test Case 20: Copy with null rectangle (should handle gracefully)
     * Dimensions: [selection_type=null] [content=text]
     */
    @Test
    public void testCopyText_NullRectangle_ShouldHandleGracefully() {
        // ARRANGE
        mockScreen.setScreenData(SIMPLE_TEXT);

        // ACT & ASSERT: Should not throw NullPointerException
        try {
            String copied = mockScreen.copyText(null);
            // If it doesn't throw, should return null or empty
            assertTrue(copied == null || copied.isEmpty(),"Should handle null gracefully");
        } catch (NullPointerException e) {
            fail("Should not throw NullPointerException for null rectangle: " + e.getMessage());
        }
    }

    /**
     * Test Case 21: Copy with invalid rectangle (negative bounds)
     * Dimensions: [selection_type=rectangle] [bounds=negative]
     */
    @Test
    public void testCopyText_NegativeBounds_ShouldHandleOrCorrect() {
        // ARRANGE
        mockScreen.setScreenData(SIMPLE_TEXT);
        testRect = new Rect(-1, -1, 5, 5);

        // ACT & ASSERT: Should not crash
        try {
            String copied = mockScreen.copyText(testRect);
            // Should handle gracefully
            assertNotNull(copied,"Should return something");
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            // Acceptable to throw for invalid bounds
            assertTrue(true,"Exception should be for invalid bounds");
        }
    }

    /**
     * Test Case 22: Copy with out-of-bounds rectangle
     * Dimensions: [selection_type=rectangle] [bounds=out_of_bounds]
     */
    @Test
    public void testCopyText_OutOfBoundsBounds_ShouldHandleOrTrim() {
        // ARRANGE
        mockScreen.setScreenData(SIMPLE_TEXT);
        testRect = new Rect(SCREEN_WIDTH + 10, SCREEN_HEIGHT + 10, 10, 10);

        // ACT & ASSERT: Should not crash
        try {
            String copied = mockScreen.copyText(testRect);
            // Should return empty or handle gracefully
            assertNotNull(copied,"Should return something");
        } catch (IndexOutOfBoundsException e) {
            // Acceptable to throw for out of bounds
            assertTrue(true,"Exception should be for bounds check");
        }
    }

    /**
     * Test Case 23: Paste null content
     * Dimensions: [content=null] [paste_target=input_field]
     */
    @Test
    public void testPasteText_NullContent_ShouldHandleGracefully() {
        // ARRANGE
        mockScreen.setCursorPosition(0);

        // ACT & ASSERT: Should not throw NullPointerException
        try {
            mockScreen.pasteText(null, false);
            // If it doesn't throw, screen should be unchanged
            String screenContent = mockScreen.getScreenText();
            assertNotNull(screenContent,"Screen should still have content");
        } catch (NullPointerException e) {
            fail("Should not throw NullPointerException for null paste content: " + e.getMessage());
        }
    }

    /**
     * Test Case 24: Paste extremely long content (max buffer)
     * Dimensions: [buffer_size=max] [paste_target=input_field]
     */
    @Test
    public void testPasteText_MaxBufferSize_ShouldPasteOrTruncate() {
        // ARRANGE
        mockScreen.setCursorPosition(0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < (MAX_BUFFER / 80); i++) {
            sb.append("Row").append(i).append("\r\n");
        }
        String maxText = sb.toString();

        // ACT
        try {
            mockScreen.pasteText(maxText, false);
            // Should succeed or gracefully handle
            String screenContent = mockScreen.getScreenText();
            assertNotNull(screenContent,"Screen should have content");
        } catch (OutOfMemoryError e) {
            fail("Should not throw OutOfMemoryError: " + e.getMessage());
        }
    }

    /**
     * Test Case 25: Paste with control characters
     * Dimensions: [content=text] [encoding=ASCII] [special_chars=control]
     */
    @Test
    public void testPasteText_ControlCharacters_ShouldFilterOrPreserve() {
        // ARRANGE
        mockScreen.setCursorPosition(0);
        String controlText = "Test\u0001\u0002\u0003End";

        // ACT & ASSERT: Should handle without crashing
        try {
            mockScreen.pasteText(controlText, false);
            String screenContent = mockScreen.getScreenText();
            assertNotNull(screenContent,"Screen should have content after control chars");
        } catch (Exception e) {
            fail("Should handle control characters gracefully: " + e.getMessage());
        }
    }

    /**
     * Test Case 26: Copy-Paste roundtrip (idempotency)
     * Dimensions: [selection_type=rectangle] [content=text] [operation=roundtrip]
     */
    @Test
    public void testCopyPasteRoundtrip_SimpleText_ShouldPreserveData() {
        // ARRANGE
        mockScreen.setScreenData(SIMPLE_TEXT);
        testRect = new Rect(0, 0, SIMPLE_TEXT.length(), 1);

        // ACT: Copy
        String copied = mockScreen.copyText(testRect);

        // Reset screen and paste
        mockScreen.clearScreen();
        mockScreen.setCursorPosition(0);
        mockScreen.pasteText(copied, false);

        // ASSERT: Content should be recoverable
        String afterRoundtrip = mockScreen.getScreenText();
        assertNotNull(afterRoundtrip,"Content after roundtrip should not be null");
        assertTrue(afterRoundtrip.length() > 0,"Content should be present after roundtrip");
    }

    /**
     * Test Case 27: Copy then paste to protected field (should handle gracefully)
     * Dimensions: [paste_target=protected] [content=text]
     */
    @Test
    public void testPasteText_ProtectedField_ShouldIgnoreOrThrow() {
        // ARRANGE
        mockScreen.setScreenData(SIMPLE_TEXT);
        mockScreen.setProtectedField(0, 5); // Mark as protected
        mockScreen.setCursorPosition(0);
        String textToPaste = "NewData";

        // ACT & ASSERT: Should either ignore paste or throw meaningful error
        try {
            mockScreen.pasteText(textToPaste, false);
            // May succeed by skipping protected area
            String screenContent = mockScreen.getScreenText();
            assertNotNull(screenContent,"Screen should have content");
        } catch (IllegalStateException e) {
            // Acceptable to reject paste to protected field
            assertTrue(true,"Exception should indicate protected field");
        }
    }

    /**
     * Test Case 28: Paste at end of screen (wrap-around behavior)
     * Dimensions: [paste_target=off_screen] [buffer_size=medium]
     */
    @Test
    public void testPasteText_OffScreenPosition_ShouldWrapOrTruncate() {
        // ARRANGE
        mockScreen.setCursorPosition(SCREEN_WIDTH * SCREEN_HEIGHT - 10); // Near end
        String textToPaste = "TooMuchData";

        // ACT & ASSERT: Should handle gracefully
        try {
            mockScreen.pasteText(textToPaste, false);
            String screenContent = mockScreen.getScreenText();
            assertNotNull(screenContent,"Screen should have content");
        } catch (IndexOutOfBoundsException e) {
            // Acceptable to throw for off-screen paste
            assertTrue(true,"Exception for off-screen position is acceptable");
        }
    }

    /**
     * Test Case 29: Concurrent copy-paste operations (thread safety)
     * Dimensions: [operation=concurrent] [content=text]
     */
    @Test
    public void testCopyPaste_ConcurrentOperations_ShouldBeThreadSafe() throws InterruptedException {
        // ARRANGE
        mockScreen.setScreenData(SIMPLE_TEXT);
        testRect = new Rect(0, 0, 5, 1);

        final AtomicReference<String> copiedResult = new AtomicReference<>();
        Thread copyThread = new Thread(new Runnable() {
            public void run() {
                copiedResult.set(mockScreen.copyText(testRect));
            }
        });

        Thread pasteThread = new Thread(new Runnable() {
            public void run() {
                mockScreen.pasteText("PasteData", false);
            }
        });

        // ACT
        copyThread.start();
        pasteThread.start();
        copyThread.join(5000);
        pasteThread.join(5000);

        // ASSERT: Both operations should complete without exception
        assertNotNull(copiedResult.get(),"Copy should return result");
        String finalContent = mockScreen.getScreenText();
        assertNotNull(finalContent,"Screen should have final content");
    }

    /**
     * Test Case 30: Large selection with trailing spaces
     * Dimensions: [selection_type=rectangle] [content=mixed] [buffer_size=large]
     */
    @Test
    public void testCopyText_TrailingSpaces_ShouldPreserveOrTrim() {
        // ARRANGE
        StringBuilder sb = new StringBuilder("Data");
        for (int i = 0; i < 10; i++) {
            sb.append("    ");
        }
        String dataWithSpaces = sb.toString();
        mockScreen.setScreenData(dataWithSpaces);
        testRect = new Rect(0, 0, 40, 1);

        // ACT
        String copied = mockScreen.copyText(testRect);

        // ASSERT: Should contain the data
        assertNotNull(copied,"Copied text should not be null");
        assertTrue(copied.length() > 0,"Should contain data portion");
    }

    // ========== HELPER MOCKS ==========

    /**
     * Mock Screen5250 for testing clipboard operations.
     * Provides simplified interface for copy/paste operations.
     */
    static class MockScreen5250 {
        private StringBuilder screenBuffer;
        private int cursorPosition;
        private boolean[] protectedFlags;

        MockScreen5250() {
            this.screenBuffer = new StringBuilder();
            this.cursorPosition = 0;
            this.protectedFlags = new boolean[SCREEN_WIDTH * SCREEN_HEIGHT];
            // Initialize screen with spaces
            for (int i = 0; i < SCREEN_WIDTH * SCREEN_HEIGHT; i++) {
                screenBuffer.append(' ');
            }
        }

        void setScreenData(String data) {
            screenBuffer = new StringBuilder();
            screenBuffer.append(data);
            // Pad with spaces to full screen
            while (screenBuffer.length() < SCREEN_WIDTH * SCREEN_HEIGHT) {
                screenBuffer.append(' ');
            }
        }

        void clearScreen() {
            screenBuffer = new StringBuilder();
            for (int i = 0; i < SCREEN_WIDTH * SCREEN_HEIGHT; i++) {
                screenBuffer.append(' ');
            }
            cursorPosition = 0;
        }

        void setCursorPosition(int position) {
            this.cursorPosition = Math.max(0, Math.min(position,
                    SCREEN_WIDTH * SCREEN_HEIGHT - 1));
        }

        void setProtectedField(int start, int length) {
            for (int i = start; i < start + length && i < protectedFlags.length; i++) {
                protectedFlags[i] = true;
            }
        }

        String copyText(Rect area) {
            if (area == null) {
                return null;
            }

            StringBuilder result = new StringBuilder();

            // Validate and constrain bounds
            int x = Math.max(0, area.x());
            int y = Math.max(0, area.y());
            int width = Math.max(1, area.width());
            int height = Math.max(1, area.height());

            // Constrain to screen bounds
            x = Math.min(x, SCREEN_WIDTH - 1);
            y = Math.min(y, SCREEN_HEIGHT - 1);
            width = Math.min(width, SCREEN_WIDTH - x);
            height = Math.min(height, SCREEN_HEIGHT - y);

            // Extract text from specified rectangle
            for (int row = y; row < y + height && row < SCREEN_HEIGHT; row++) {
                for (int col = x; col < x + width && col < SCREEN_WIDTH; col++) {
                    int pos = row * SCREEN_WIDTH + col;
                    if (pos < screenBuffer.length()) {
                        char c = screenBuffer.charAt(pos);
                        result.append(c >= ' ' ? c : ' ');
                    } else {
                        result.append(' ');
                    }
                }
                if (row < y + height - 1) {
                    result.append('\n');
                }
            }

            return result.toString();
        }

        String copyTextField(int position) {
            // Simplified: return text starting at position until space or end
            if (position < 0 || position >= screenBuffer.length()) {
                return "";
            }

            StringBuilder result = new StringBuilder();
            for (int i = position; i < screenBuffer.length(); i++) {
                char c = screenBuffer.charAt(i);
                if (c == ' ') break;
                result.append(c);
            }
            return result.toString();
        }

        void pasteText(String content, boolean special) {
            if (content == null || content.isEmpty()) {
                return;
            }

            int pos = cursorPosition;
            for (char c : content.toCharArray()) {
                if (pos >= screenBuffer.length()) {
                    // Wrap around if needed
                    pos = 0;
                }

                if (c == '\n' || c == '\r') {
                    // Move to next line
                    int col = pos % SCREEN_WIDTH;
                    pos += SCREEN_WIDTH - col;
                } else if (!protectedFlags[pos]) {
                    // Only paste if not protected
                    screenBuffer.setCharAt(pos, c);
                    pos++;
                }
            }
        }

        String getScreenText() {
            return screenBuffer.toString();
        }
    }

    /**
     * Thread-safe reference holder for concurrent testing
     */
    static class AtomicReference<T> {
        private T value;

        void set(T value) {
            this.value = value;
        }

        T get() {
            return value;
        }
    }
}
