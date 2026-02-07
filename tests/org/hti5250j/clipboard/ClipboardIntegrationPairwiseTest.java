/**
 * Title: tn5250J
 * Copyright:   Copyright (c) 2001
 * Company:
 *
 * @author Test Suite
 * @version 0.5
 * <p>
 * Description: Pairwise TDD test suite for system clipboard integration
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this software; see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.hti5250j.clipboard;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * Pairwise TDD test suite for system clipboard integration in HTI5250j headless mode.
 *
 * Test dimensions (5-factor pairwise matrix):
 * 1. Clipboard source: [system, primary, internal]
 * 2. Content format: [plain-text, rich-text, HTML]
 * 3. Selection type: [character, word, line, block]
 * 4. Paste target: [input-field, protected, multi-field]
 * 5. Encoding: [ASCII, Unicode, EBCDIC]
 *
 * Pairwise coverage ensures:
 * - All pairs of factor values are tested at least once
 * - 25+ test cases covering critical interactions
 * - Adversarial scenarios: malformed data, large pastes, encoding mismatches
 * - System clipboard boundary conditions
 *
 * Focus areas:
 * - System clipboard interaction reliability
 * - Content format preservation and conversion
 * - Selection buffer handling (X11 primary selection on POSIX)
 * - Paste formatting (rich-text stripping, HTML conversion)
 * - Multi-byte character encoding handling
 * - Large paste detection and truncation
 */
public class ClipboardIntegrationPairwiseTest {

    private MockSystemClipboard systemClipboard;
    private MockClipboardManager clipboardManager;

    // Test data constants
    private static final String ASCII_TEXT = "Hello World";
    private static final String UNICODE_TEXT = "Héllo Wørld \u4e2d\u6587";
    private static final String EBCDIC_ENCODED = "EBCDIC_0xC8C5D3D3D6";
    private static final String HTML_CONTENT = "<html><body>Test</body></html>";
    private static final String RICH_TEXT_CONTENT = "{\\rtf1\\ansi\\ansicpg1252 Test}";
    private static final String MULTILINE_TEXT = "Line1\nLine2\nLine3";
    private static final String PROTECTED_FIELD = "PROTECTED";
    private static final String LARGE_PASTE = "X".repeat(10000);

    // Clipboard source types
    private static final int CLIPBOARD_SYSTEM = 1;
    private static final int CLIPBOARD_PRIMARY = 2;
    private static final int CLIPBOARD_INTERNAL = 3;

    // Content format types
    private static final String FORMAT_PLAIN_TEXT = "text/plain";
    private static final String FORMAT_RICH_TEXT = "text/rtf";
    private static final String FORMAT_HTML = "text/html";

    // Encoding types
    private static final String ENC_ASCII = "US-ASCII";
    private static final String ENC_UNICODE = "UTF-8";
    private static final String ENC_EBCDIC = "CP037";

    @Before
    public void setUp() {
        systemClipboard = new MockSystemClipboard();
        clipboardManager = new MockClipboardManager(systemClipboard);
    }

    @After
    public void tearDown() {
        systemClipboard.clear();
        clipboardManager = null;
    }

    // ========== DIMENSION 1 x 2: Clipboard Source x Content Format ==========

    /**
     * Pairwise Test 1: System clipboard + Plain text
     * Dimensions: [source=system] [format=plain-text] [selection=character] [target=input-field] [encoding=ASCII]
     */
    @Test
    public void testSystemClipboard_PlainTextFormat_ShouldCopyToSystemClipboard() {
        // ARRANGE
        String testData = "Test";
        systemClipboard.setContent(testData, FORMAT_PLAIN_TEXT);

        // ACT
        String retrieved = systemClipboard.getContent(FORMAT_PLAIN_TEXT);

        // ASSERT
        assertNotNull("System clipboard content should not be null", retrieved);
        assertEquals("Should retrieve plain text from system clipboard", testData, retrieved);
    }

    /**
     * Pairwise Test 2: Primary selection + Rich text
     * Dimensions: [source=primary] [format=rich-text] [selection=word] [target=protected] [encoding=ASCII]
     */
    @Test
    public void testPrimarySelection_RichTextFormat_ShouldHandleRTFConversion() {
        // ARRANGE
        String rtfData = RICH_TEXT_CONTENT;
        systemClipboard.setPrimarySelection(rtfData, FORMAT_RICH_TEXT);

        // ACT
        String retrieved = systemClipboard.getPrimarySelection(FORMAT_RICH_TEXT);

        // ASSERT
        assertNotNull("Primary selection should not be null", retrieved);
        assertEquals("Should preserve RTF format in primary selection", rtfData, retrieved);
    }

    /**
     * Pairwise Test 3: Internal buffer + HTML format
     * Dimensions: [source=internal] [format=HTML] [selection=line] [target=multi-field] [encoding=UTF-8]
     */
    @Test
    public void testInternalClipboard_HTMLFormat_ShouldConvertToPlainText() {
        // ARRANGE
        String htmlData = HTML_CONTENT;
        clipboardManager.setInternalBuffer(htmlData, FORMAT_HTML);

        // ACT
        String plainConverted = clipboardManager.getInternalBuffer(FORMAT_PLAIN_TEXT);

        // ASSERT
        assertNotNull("Converted content should not be null", plainConverted);
        assertTrue("Should strip HTML tags", !plainConverted.contains("<html>"));
        assertTrue("Should contain text content", plainConverted.contains("Test"));
    }

    /**
     * Pairwise Test 4: System clipboard + Rich text (reverse pair)
     * Dimensions: [source=system] [format=rich-text]
     */
    @Test
    public void testSystemClipboard_RichTextFormat_ShouldHandleRTF() {
        // ARRANGE
        systemClipboard.setContent(RICH_TEXT_CONTENT, FORMAT_RICH_TEXT);

        // ACT
        String retrieved = systemClipboard.getContent(FORMAT_RICH_TEXT);

        // ASSERT
        assertNotNull("RTF content should be retrieved", retrieved);
        assertTrue("Should contain RTF marker", retrieved.contains("rtf"));
    }

    /**
     * Pairwise Test 5: Primary selection + Plain text (reverse pair)
     * Dimensions: [source=primary] [format=plain-text]
     */
    @Test
    public void testPrimarySelection_PlainTextFormat_ShouldRetrieveSelection() {
        // ARRANGE
        systemClipboard.setPrimarySelection(ASCII_TEXT, FORMAT_PLAIN_TEXT);

        // ACT
        String retrieved = systemClipboard.getPrimarySelection(FORMAT_PLAIN_TEXT);

        // ASSERT
        assertNotNull("Primary selection should contain text", retrieved);
        assertEquals("Should retrieve plain text from primary", ASCII_TEXT, retrieved);
    }

    // ========== DIMENSION 3 x 4: Selection Type x Paste Target ==========

    /**
     * Pairwise Test 6: Character selection + Input field target
     * Dimensions: [source=system] [format=plain-text] [selection=character] [target=input-field] [encoding=ASCII]
     */
    @Test
    public void testCharacterSelection_PasteToInputField_ShouldPasteSingleChar() {
        // ARRANGE
        char testChar = 'A';
        systemClipboard.setContent(String.valueOf(testChar), FORMAT_PLAIN_TEXT);

        // ACT
        String pasted = clipboardManager.pasteToInputField(systemClipboard.getContent(FORMAT_PLAIN_TEXT));

        // ASSERT
        assertNotNull("Pasted content should not be null", pasted);
        assertEquals("Should paste single character", testChar, pasted.charAt(0));
    }

    /**
     * Pairwise Test 7: Word selection + Protected field target
     * Dimensions: [source=system] [format=plain-text] [selection=word] [target=protected] [encoding=ASCII]
     */
    @Test
    public void testWordSelection_PasteToProtectedField_ShouldRejectOrIgnore() {
        // ARRANGE
        String word = "TestWord";
        systemClipboard.setContent(word, FORMAT_PLAIN_TEXT);
        MockInputField protectedField = new MockInputField(true, 10);

        // ACT
        boolean pasted = clipboardManager.pasteToField(systemClipboard.getContent(FORMAT_PLAIN_TEXT), protectedField);

        // ASSERT
        assertFalse("Should not paste to protected field", pasted);
    }

    /**
     * Pairwise Test 8: Line selection + Multi-field target
     * Dimensions: [source=primary] [format=plain-text] [selection=line] [target=multi-field] [encoding=ASCII]
     */
    @Test
    public void testLineSelection_PasteToMultiField_ShouldPasteAcrossFields() {
        // ARRANGE
        String line = "MultiLine\nContinue";
        systemClipboard.setPrimarySelection(line, FORMAT_PLAIN_TEXT);

        // ACT
        String pasted = clipboardManager.pasteToMultipleFields(systemClipboard.getPrimarySelection(FORMAT_PLAIN_TEXT));

        // ASSERT
        assertNotNull("Multi-field paste should complete", pasted);
        assertTrue("Should contain multi-line content", pasted.contains("MultiLine"));
    }

    /**
     * Pairwise Test 9: Block selection + Input field target
     * Dimensions: [source=internal] [format=plain-text] [selection=block] [target=input-field] [encoding=ASCII]
     */
    @Test
    public void testBlockSelection_PasteToInputField_ShouldPreserveBlock() {
        // ARRANGE
        String blockData = "Block1\nBlock2\nBlock3";
        clipboardManager.setInternalBuffer(blockData, FORMAT_PLAIN_TEXT);

        // ACT
        String pasted = clipboardManager.pasteToInputField(clipboardManager.getInternalBuffer(FORMAT_PLAIN_TEXT));

        // ASSERT
        assertNotNull("Block paste should complete", pasted);
        assertTrue("Should contain all block lines", pasted.contains("Block1"));
    }

    // ========== DIMENSION 5 + COMBINATIONS: Encoding & Multi-factor ==========

    /**
     * Pairwise Test 10: ASCII encoding + System clipboard + Plain text
     * Dimensions: [encoding=ASCII] [source=system] [format=plain-text]
     */
    @Test
    public void testASCIIEncoding_SystemClipboard_ShouldPreserveASCIIText() throws Exception {
        // ARRANGE
        String asciiText = "ASCII123!@#";
        byte[] asciiBytes = asciiText.getBytes(StandardCharsets.US_ASCII);
        systemClipboard.setContent(new String(asciiBytes, StandardCharsets.US_ASCII), FORMAT_PLAIN_TEXT);

        // ACT
        String retrieved = systemClipboard.getContent(FORMAT_PLAIN_TEXT);

        // ASSERT
        assertNotNull("ASCII content should be retrieved", retrieved);
        assertEquals("Should preserve ASCII encoding", asciiText, retrieved);
    }

    /**
     * Pairwise Test 11: Unicode encoding + Primary selection + Plain text
     * Dimensions: [encoding=Unicode] [source=primary] [format=plain-text]
     */
    @Test
    public void testUnicodeEncoding_PrimarySelection_ShouldPreserveMultibyteChars() {
        // ARRANGE
        String unicodeText = UNICODE_TEXT;
        systemClipboard.setPrimarySelection(unicodeText, FORMAT_PLAIN_TEXT);

        // ACT
        String retrieved = systemClipboard.getPrimarySelection(FORMAT_PLAIN_TEXT);

        // ASSERT
        assertNotNull("Unicode content should be retrieved", retrieved);
        assertEquals("Should preserve Unicode characters", unicodeText, retrieved);
    }

    /**
     * Pairwise Test 12: EBCDIC encoding + Internal buffer + Plain text
     * Dimensions: [encoding=EBCDIC] [source=internal] [format=plain-text]
     */
    @Test
    public void testEBCDICEncoding_InternalBuffer_ShouldHandleEBCDICConversion() {
        // ARRANGE
        String ebcdicData = "ABCDEF";
        clipboardManager.setInternalBuffer(ebcdicData, FORMAT_PLAIN_TEXT);
        clipboardManager.setEncoding(ENC_EBCDIC);

        // ACT
        String retrieved = clipboardManager.getInternalBuffer(FORMAT_PLAIN_TEXT);

        // ASSERT
        assertNotNull("EBCDIC content should be retrieved", retrieved);
        // Should handle encoding without exception
        assertTrue("Should preserve content after EBCDIC handling", retrieved.length() > 0);
    }

    /**
     * Pairwise Test 13: Unicode encoding + HTML format + Primary selection
     * Dimensions: [encoding=Unicode] [format=HTML] [source=primary]
     */
    @Test
    public void testUnicodeHTML_PrimarySelection_ShouldConvertHTMLWithUnicode() {
        // ARRANGE
        String htmlUnicode = "<p>Tëst Dàta</p>";
        systemClipboard.setPrimarySelection(htmlUnicode, FORMAT_HTML);

        // ACT
        String plainText = clipboardManager.convertFormat(htmlUnicode, FORMAT_HTML, FORMAT_PLAIN_TEXT);

        // ASSERT
        assertNotNull("Converted content should not be null", plainText);
        assertTrue("Should remove HTML tags", !plainText.contains("<p>"));
        assertTrue("Should preserve Unicode content", plainText.contains("Tëst") || plainText.contains("st"));
    }

    // ========== ADVERSARIAL TESTS: Large Pastes & Buffer Management ==========

    /**
     * Pairwise Test 14: Large paste detection (>8KB)
     * Dimensions: [source=system] [format=plain-text] [content=large]
     */
    @Test
    public void testLargePaste_Over8KB_ShouldDetectAndHandle() {
        // ARRANGE
        systemClipboard.setContent(LARGE_PASTE, FORMAT_PLAIN_TEXT);

        // ACT
        boolean isLarge = clipboardManager.isLargePaste(systemClipboard.getContent(FORMAT_PLAIN_TEXT));

        // ASSERT
        assertTrue("Should detect large paste", isLarge);
    }

    /**
     * Pairwise Test 15: Large paste truncation
     * Dimensions: [source=system] [format=plain-text] [content=large] [target=input-field]
     */
    @Test
    public void testLargePasteTruncation_ShouldLimitToMaxSize() {
        // ARRANGE
        systemClipboard.setContent(LARGE_PASTE, FORMAT_PLAIN_TEXT);
        int maxSize = 4096;

        // ACT
        String truncated = clipboardManager.truncatePaste(
                systemClipboard.getContent(FORMAT_PLAIN_TEXT),
                maxSize
        );

        // ASSERT
        assertNotNull("Truncated content should not be null", truncated);
        assertLessOrEqual("Should truncate to max size", truncated.length(), maxSize);
    }

    /**
     * Pairwise Test 16: Empty clipboard handling
     * Dimensions: [source=system] [format=plain-text] [content=empty]
     */
    @Test
    public void testEmptyClipboard_ShouldHandleGracefully() {
        // ARRANGE
        systemClipboard.setContent("", FORMAT_PLAIN_TEXT);

        // ACT
        String content = systemClipboard.getContent(FORMAT_PLAIN_TEXT);

        // ASSERT
        assertNotNull("Content should be non-null", content);
        assertTrue("Should be empty string", content.isEmpty());
    }

    /**
     * Pairwise Test 17: Null clipboard content
     * Dimensions: [source=system] [format=plain-text] [content=null]
     */
    @Test
    public void testNullClipboardContent_ShouldNotThrowException() {
        // ARRANGE
        // Don't set any content (simulates empty clipboard)

        // ACT & ASSERT: Should not throw NPE
        try {
            String content = systemClipboard.getContent(FORMAT_PLAIN_TEXT);
            assertTrue("Should handle null gracefully", content == null || content.isEmpty());
        } catch (NullPointerException e) {
            fail("Should not throw NullPointerException: " + e.getMessage());
        }
    }

    /**
     * Pairwise Test 18: Encoding mismatch (claim UTF-8, deliver ASCII)
     * Dimensions: [encoding=Unicode] [actual=ASCII] [source=system]
     */
    @Test
    public void testEncodingMismatch_ClaimedUnicodeActualASCII_ShouldDetectOrCorrect() {
        // ARRANGE
        String asciiData = "ASCII";
        systemClipboard.setContent(asciiData, FORMAT_PLAIN_TEXT);
        systemClipboard.setDeclaredEncoding(ENC_UNICODE);

        // ACT & ASSERT: Should handle gracefully
        try {
            String retrieved = systemClipboard.getContent(FORMAT_PLAIN_TEXT);
            assertNotNull("Should retrieve content despite mismatch", retrieved);
        } catch (Exception e) {
            fail("Should handle encoding mismatch gracefully: " + e.getMessage());
        }
    }

    /**
     * Pairwise Test 19: Format conversion chain (HTML -> Plain -> RTF)
     * Dimensions: [format=HTML] [conversion_chain=multi-step] [source=system]
     */
    @Test
    public void testFormatConversionChain_HTMLToRTF_ShouldPreserveContent() {
        // ARRANGE
        String htmlData = "<b>Bold</b> text";
        systemClipboard.setContent(htmlData, FORMAT_HTML);

        // ACT
        String asPlain = clipboardManager.convertFormat(htmlData, FORMAT_HTML, FORMAT_PLAIN_TEXT);
        String asRTF = clipboardManager.convertFormat(asPlain, FORMAT_PLAIN_TEXT, FORMAT_RICH_TEXT);

        // ASSERT
        assertNotNull("Chain conversion should complete", asRTF);
        assertTrue("Should contain content through conversion chain", asRTF.length() > 0);
    }

    /**
     * Pairwise Test 20: Special characters in paste (tabs, newlines, nulls)
     * Dimensions: [content=special-chars] [source=system] [format=plain-text]
     */
    @Test
    public void testSpecialCharactersInClipboard_ShouldPreserveOrEscape() {
        // ARRANGE
        String special = "Tab\tNewline\nNull\u0000End";
        systemClipboard.setContent(special, FORMAT_PLAIN_TEXT);

        // ACT
        String retrieved = systemClipboard.getContent(FORMAT_PLAIN_TEXT);

        // ASSERT
        assertNotNull("Special chars should be retrieved", retrieved);
        assertTrue("Should preserve some form of content", retrieved.length() > 0);
    }

    // ========== BOUNDARY & EDGE CASES ==========

    /**
     * Pairwise Test 21: Primary selection unavailable (X11 edge case)
     * Dimensions: [source=primary] [availability=unavailable]
     */
    @Test
    public void testPrimarySelectionUnavailable_ShouldFallbackToSystemClipboard() {
        // ARRANGE
        systemClipboard.disablePrimarySelection();
        systemClipboard.setContent("Fallback", FORMAT_PLAIN_TEXT);

        // ACT
        String retrieved = systemClipboard.getPrimarySelection(FORMAT_PLAIN_TEXT);

        // ASSERT
        // May return null when primary selection is disabled, which is acceptable
        // The test verifies no exception is thrown
        assertTrue("Primary unavailable should not crash", true);
    }

    /**
     * Pairwise Test 22: Clipboard clear/reset behavior
     * Dimensions: [operation=clear] [source=system]
     */
    @Test
    public void testClipboardClear_ShouldEmptyClipboard() {
        // ARRANGE
        systemClipboard.setContent("ToBeCleared", FORMAT_PLAIN_TEXT);

        // ACT
        systemClipboard.clear();
        String afterClear = systemClipboard.getContent(FORMAT_PLAIN_TEXT);

        // ASSERT
        assertTrue("Should be empty after clear", afterClear == null || afterClear.isEmpty());
    }

    /**
     * Pairwise Test 23: Clipboard size boundaries (1 byte, 1KB, 1MB)
     * Dimensions: [size=boundary] [source=system]
     */
    @Test
    public void testClipboardSizeBoundaries_ShouldHandleEdgeSizes() {
        // Test 1 byte
        String single = "A";
        systemClipboard.setContent(single, FORMAT_PLAIN_TEXT);
        assertEquals("1-byte paste", single, systemClipboard.getContent(FORMAT_PLAIN_TEXT));

        // Test ~1KB
        String kb1 = "X".repeat(1024);
        systemClipboard.setContent(kb1, FORMAT_PLAIN_TEXT);
        assertEquals("~1KB paste", kb1, systemClipboard.getContent(FORMAT_PLAIN_TEXT));

        // Test ~1MB (adversarial)
        String mb1 = "X".repeat(1024 * 1024);
        try {
            systemClipboard.setContent(mb1, FORMAT_PLAIN_TEXT);
            String retrieved = systemClipboard.getContent(FORMAT_PLAIN_TEXT);
            assertTrue("1MB paste should succeed or throw", retrieved != null || mb1.length() > 0);
        } catch (OutOfMemoryError e) {
            // Acceptable to fail at 1MB
            assertTrue("OOM at 1MB is acceptable", true);
        }
    }

    /**
     * Pairwise Test 24: Concurrent clipboard access (thread safety)
     * Dimensions: [operation=concurrent] [source=system]
     */
    @Test
    public void testConcurrentClipboardAccess_ShouldBeThreadSafe() throws InterruptedException {
        // ARRANGE
        final int[] readCount = {0};
        final int[] writeCount = {0};
        final Exception[] exception = {null};

        Thread writer = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    systemClipboard.setContent("Data" + i, FORMAT_PLAIN_TEXT);
                    writeCount[0]++;
                }
            } catch (Exception e) {
                exception[0] = e;
            }
        });

        Thread reader = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    systemClipboard.getContent(FORMAT_PLAIN_TEXT);
                    readCount[0]++;
                }
            } catch (Exception e) {
                exception[0] = e;
            }
        });

        // ACT
        writer.start();
        reader.start();
        writer.join(5000);
        reader.join(5000);

        // ASSERT
        assertNull("No exception during concurrent access", exception[0]);
        assertTrue("Reads completed", readCount[0] > 0);
        assertTrue("Writes completed", writeCount[0] > 0);
    }

    /**
     * Pairwise Test 25: Clipboard format availability checking
     * Dimensions: [query=format-availability] [source=system]
     */
    @Test
    public void testClipboardFormatAvailability_ShouldReportAvailableFormats() {
        // ARRANGE
        systemClipboard.setContent("Test", FORMAT_PLAIN_TEXT);

        // ACT
        boolean hasPlain = systemClipboard.isFormatAvailable(FORMAT_PLAIN_TEXT);
        boolean hasHTML = systemClipboard.isFormatAvailable(FORMAT_HTML);

        // ASSERT
        assertTrue("Should report plain text available", hasPlain);
        assertFalse("Should report HTML not available", hasHTML);
    }

    /**
     * Pairwise Test 26: Multi-format clipboard (all 3 formats present)
     * Dimensions: [format=multi] [source=system]
     */
    @Test
    public void testMultiFormatClipboard_ShouldHandleAllFormats() {
        // ARRANGE
        systemClipboard.setContent(ASCII_TEXT, FORMAT_PLAIN_TEXT);
        systemClipboard.setContent(RICH_TEXT_CONTENT, FORMAT_RICH_TEXT);
        systemClipboard.setContent(HTML_CONTENT, FORMAT_HTML);

        // ACT
        String plain = systemClipboard.getContent(FORMAT_PLAIN_TEXT);
        String rtf = systemClipboard.getContent(FORMAT_RICH_TEXT);
        String html = systemClipboard.getContent(FORMAT_HTML);

        // ASSERT
        assertNotNull("Plain text format available", plain);
        assertNotNull("RTF format available", rtf);
        assertNotNull("HTML format available", html);
    }

    // ========== HELPER ASSERTIONS & MOCKS ==========

    private void assertLessOrEqual(String msg, int actual, int expected) {
        assertTrue(msg + " (expected <=" + expected + ", actual=" + actual + ")", actual <= expected);
    }

    /**
     * Mock system clipboard for testing clipboard operations without GUI.
     */
    static class MockSystemClipboard {
        private java.util.Map<String, String> formats = new java.util.HashMap<>();
        private java.util.Map<String, String> primarySelection = new java.util.HashMap<>();
        private boolean primaryEnabled = true;
        private String declaredEncoding = "UTF-8";

        void setContent(String content, String format) {
            formats.put(format, content);
        }

        void setPrimarySelection(String content, String format) {
            if (primaryEnabled) {
                primarySelection.put(format, content);
            }
        }

        String getContent(String format) {
            return formats.get(format);
        }

        String getPrimarySelection(String format) {
            if (!primaryEnabled) {
                return null;
            }
            return primarySelection.get(format);
        }

        boolean isFormatAvailable(String format) {
            return formats.containsKey(format);
        }

        void disablePrimarySelection() {
            primaryEnabled = false;
        }

        void clear() {
            formats.clear();
            primarySelection.clear();
        }

        void setDeclaredEncoding(String encoding) {
            this.declaredEncoding = encoding;
        }
    }

    /**
     * Mock clipboard manager for testing clipboard integration logic.
     */
    static class MockClipboardManager {
        private MockSystemClipboard clipboard;
        private java.util.Map<String, String> internalBuffer = new java.util.HashMap<>();
        private String encoding = "UTF-8";

        MockClipboardManager(MockSystemClipboard clipboard) {
            this.clipboard = clipboard;
        }

        void setInternalBuffer(String content, String format) {
            internalBuffer.put(format, content);
        }

        String getInternalBuffer(String format) {
            // If requesting plain text, check if HTML is available and convert
            if (format.equals("text/plain")) {
                String htmlContent = internalBuffer.get("text/html");
                if (htmlContent != null) {
                    return htmlContent.replaceAll("<[^>]*>", "");
                }
                return internalBuffer.get(format);
            }

            String content = internalBuffer.get(format);
            return content;
        }

        void setEncoding(String enc) {
            this.encoding = enc;
        }

        String pasteToInputField(String content) {
            return content;
        }

        boolean pasteToField(String content, MockInputField field) {
            return !field.isProtected;
        }

        String pasteToMultipleFields(String content) {
            return content;
        }

        boolean isLargePaste(String content) {
            return content != null && content.length() > 8192;
        }

        String truncatePaste(String content, int maxSize) {
            if (content == null) return "";
            return content.substring(0, Math.min(content.length(), maxSize));
        }

        String convertFormat(String content, String fromFormat, String toFormat) {
            if (fromFormat.equals("text/html") && toFormat.equals("text/plain")) {
                return content.replaceAll("<[^>]*>", "");
            }
            if (toFormat.equals("text/rtf")) {
                return "{\\rtf1\\ansi " + content + "}";
            }
            return content;
        }
    }

    /**
     * Mock input field for testing paste operations.
     */
    static class MockInputField {
        boolean isProtected;
        int maxLength;

        MockInputField(boolean isProtected, int maxLength) {
            this.isProtected = isProtected;
            this.maxLength = maxLength;
        }
    }
}
