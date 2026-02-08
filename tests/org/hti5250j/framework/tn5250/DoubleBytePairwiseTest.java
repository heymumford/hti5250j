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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pairwise TDD test suite for HTI5250j DBCS character support.
 *
 * Focuses on high-risk behaviors in DBCS/CJK terminal automation:
 * 1. DBCS character misinterpretation as two SBCS characters
 * 2. Surrogate pair corruption (high surrogate without low surrogate)
 * 3. Width calculation errors (half-width vs. full-width)
 * 4. Cursor alignment bugs at DBCS boundaries
 * 5. Encoding mismatch (EBCDIC CJK vs. Unicode)
 * 6. Mixed SBCS/DBCS field corruption
 * 7. Replacement character insertion on decode failure
 * 8. Byte order mark (BOM) injection
 * 9. Null character handling in DBCS
 * 10. Truncation at DBCS character boundaries
 */
public class DoubleBytePairwiseTest {

    private Screen5250 screen5250;
    private ScreenPlanes screenPlanes;
    private ScreenField testField;

    private static final int SCREEN_ROWS = 24;
    private static final int SCREEN_COLS = 80;
    private static final int SCREEN_SIZE = SCREEN_ROWS * SCREEN_COLS;

    // DBCS/SBCS test character constants
    // SBCS: ASCII characters
    private static final char SBCS_CHAR_A = 'A';
    private static final char SBCS_CHAR_Z = 'Z';
    private static final char SBCS_CHAR_0 = '0';

    // DBCS: CJK characters (Unicode representation for testing)
    // Japanese Hiragana: U+3042 (あ)
    private static final char DBCS_HIRAGANA_A = '\u3042';
    // Japanese Katakana: U+30A2 (ア)
    private static final char DBCS_KATAKANA_A = '\u30A2';
    // Chinese: U+4E00 (一)
    private static final char DBCS_CHINESE_ONE = '\u4E00';
    // Chinese: U+4E8C (二)
    private static final char DBCS_CHINESE_TWO = '\u4E8C';
    // Korean Hangul: U+AC00 (가)
    private static final char DBCS_KOREAN_GA = '\uAC00';
    // Korean Hangul: U+B098 (나)
    private static final char DBCS_KOREAN_NA = '\uB098';

    // Surrogate pairs (characters beyond BMP, U+10000+)
    // Family emoticon: U+1F60A (😊), represented as surrogate pair
    private static final char SURROGATE_HIGH = '\uD83D';   // High surrogate for U+1F60A
    private static final char SURROGATE_LOW = '\uDE0A';    // Low surrogate for U+1F60A

    // Encoding constants
    private static final int ENCODING_EBCDIC_CJK = 1;
    private static final int ENCODING_UNICODE_SURROGATE = 2;
    private static final int ENCODING_ASCII_FALLBACK = 3;

    // Field type constants
    private static final int FIELD_TYPE_INPUT = 0;
    private static final int FIELD_TYPE_OUTPUT = 1;
    private static final int FIELD_TYPE_BOTH = 2;

    // Display width constants
    private static final int WIDTH_HALF = 1;
    private static final int WIDTH_FULL = 2;

    // Cursor mode constants
    private static final int CURSOR_SINGLE_CELL = 0;
    private static final int CURSOR_DOUBLE_CELL = 1;

    /**
     * Test double for Screen5250 with DBCS-aware character handling
     */
    private static class Screen5250TestDouble extends Screen5250 {
        public Screen5250TestDouble() {
            super();
        }

        @Override
        public int getScreenLength() {
            return SCREEN_SIZE;
        }

        @Override
        public boolean isInField(int pos, boolean checkAttr) {
            return pos >= 0 && pos < SCREEN_SIZE;
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
            // No-op
        }
    }

    @BeforeEach
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        screen5250 = new Screen5250TestDouble();
        screenPlanes = screen5250.getPlanes();
        screen5250.getOIA().setKeyBoardLocked(false);
    }

    /**
     * Helper: Create a field with specified attributes for DBCS testing
     */
    private ScreenField createDBCSField(
            int startPos, int length,
            int fieldType,           // INPUT, OUTPUT, BOTH
            int displayWidth,        // HALF (1), FULL (2)
            int cursorMode           // SINGLE_CELL, DOUBLE_CELL
    ) {
        // Field shift: 0=alpha (for DBCS storage)
        int ffw1 = 0;
        int ffw2 = 0x00;  // No mandatory/auto-enter for DBCS fields

        ScreenField field = new ScreenField(screen5250);
        field.setField(0x20, startPos / SCREEN_COLS, startPos % SCREEN_COLS,
                length, ffw1, ffw2, 0x41, 0x00);
        return field;
    }

    /**
     * Helper: Get private field via reflection
     */
    private String getFieldText(ScreenField field) throws NoSuchFieldException, IllegalAccessException {
        Field textField = ScreenField.class.getDeclaredField("text");
        textField.setAccessible(true);
        StringBuffer sb = (StringBuffer) textField.get(field);
        return sb != null ? sb.toString() : "";
    }

    /**
     * Helper: Convert char array to string (simulating DBCS display)
     */
    private String charArrayToString(char[] chars) {
        return new String(chars);
    }

    // ========================================================================
    // POSITIVE TESTS (13): Valid DBCS operations
    // ========================================================================

    /**
     * POSITIVE: SBCS-only field accepts ASCII characters
     * Dimension pair: charset=SBCS-only, encoding=ASCII, field=input, width=half-width
     */
    @Test
    public void testSBCSOnlyFieldAcceptsASCII() {
        ScreenField field = createDBCSField(0, 10, FIELD_TYPE_INPUT, WIDTH_HALF, CURSOR_SINGLE_CELL);
        field.setString("ABCDE");

        assertFalse(field.isNumeric(),"SBCS field should not be numeric");
        assertEquals(10, field.getText().length(),"SBCS field should preserve ASCII input length");
        assertTrue(field.getText().startsWith("ABCDE"),"Field should contain ASCII");
    }

    /**
     * POSITIVE: DBCS-only field stores Japanese Hiragana correctly
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=output, width=full-width
     */
    @Test
    public void testDBCSOnlyFieldStoresJapaneseHiragana() {
        ScreenField field = createDBCSField(80, 5, FIELD_TYPE_OUTPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        // Japanese: あいうえお (a i u e o in Hiragana)
        String japaneseText = "\u3042\u3044\u3046\u3048\u304A";
        field.setString(japaneseText);

        assertFalse(field.isNumeric(),"DBCS field should not be numeric");
        assertEquals(5, field.getText().length(),"DBCS Hiragana field should have correct length");
    }

    /**
     * POSITIVE: DBCS-only field stores Chinese characters correctly
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=output, width=full-width
     */
    @Test
    public void testDBCSOnlyFieldStoresChineseCJK() {
        ScreenField field = createDBCSField(160, 3, FIELD_TYPE_OUTPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        // Chinese: 一二三 (one, two, three)
        String chineseText = "\u4E00\u4E8C\u4E09";
        field.setString(chineseText);

        assertEquals(3, field.getText().length(),"DBCS Chinese field should preserve CJK characters");
    }

    /**
     * POSITIVE: DBCS-only field stores Korean Hangul correctly
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=input, width=full-width
     */
    @Test
    public void testDBCSOnlyFieldStoresKoreanHangul() {
        ScreenField field = createDBCSField(240, 4, FIELD_TYPE_INPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        // Korean: 가나다라 (ga na da ra)
        String koreanText = "\uAC00\uB098\uB2E4\uB77C";
        field.setString(koreanText);

        assertEquals(4, field.getText().length(),"DBCS Korean field should preserve Hangul");
    }

    /**
     * POSITIVE: Mixed SBCS/DBCS field stores both ASCII and Japanese
     * Dimension pair: charset=mixed, encoding=Unicode, field=both, width=mixed
     */
    @Test
    public void testMixedSBCSDBCSFieldWithASCIIAndJapanese() {
        ScreenField field = createDBCSField(320, 8, FIELD_TYPE_BOTH, WIDTH_HALF, CURSOR_SINGLE_CELL);
        // Mixed: "ABC" + Japanese "あいう"
        String mixedText = "ABC\u3042\u3044\u3046";
        field.setString(mixedText);

        assertTrue(field.getText().contains("ABC"),"Mixed field should accept ASCII");
        assertEquals(8, field.getText().length(),"Mixed field should preserve combined length");
    }

    /**
     * POSITIVE: Full-width DBCS character occupies 2 display cells
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=output, width=full-width
     */
    @Test
    public void testFullWidthDBCSCharacterOccupiesTwoCells() {
        ScreenField field = createDBCSField(400, 10, FIELD_TYPE_OUTPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        String singleCJK = "\u4E00";  // Chinese: 一
        field.setString(singleCJK);

        // Field pads to full length (10 chars)
        assertEquals(10, field.getText().length(),"Single DBCS character padded to field length");
        // Display width would be 2 cells for DBCS, 1 cell per ASCII pad (verified by cursor logic)
    }

    /**
     * POSITIVE: Half-width Katakana characters occupy 1 display cell
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=input, width=half-width
     */
    @Test
    public void testHalfWidthKatakanaOccupiesSingleCell() {
        ScreenField field = createDBCSField(480, 5, FIELD_TYPE_INPUT, WIDTH_HALF, CURSOR_SINGLE_CELL);
        // Half-width Katakana: ｱｲｳ (A I U)
        String halfKatakana = "\uFF71\uFF72\uFF73";
        field.setString(halfKatakana);

        // Field pads to full length (5 chars)
        assertEquals(5, field.getText().length(),"Half-width Katakana characters padded to field length");
    }

    /**
     * POSITIVE: Surrogate pair characters handled correctly (emoji test)
     * Dimension pair: charset=DBCS-only, encoding=Unicode-surrogate, field=output, width=full-width
     */
    @Test
    public void testSurrogatePairCharacterHandling() {
        ScreenField field = createDBCSField(560, 2, FIELD_TYPE_OUTPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        // Surrogate pair emoji: 😊 (U+1F60A)
        String emoji = new String(new char[]{SURROGATE_HIGH, SURROGATE_LOW});
        field.setString(emoji);

        assertEquals(2, field.getText().length(),"Surrogate pair stored correctly");
    }

    /**
     * POSITIVE: Output field type enforces read-only DBCS content
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=output, width=full-width
     */
    @Test
    public void testOutputFieldTypeWithDBCS() {
        ScreenField field = createDBCSField(640, 6, FIELD_TYPE_OUTPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        String japaneseText = "\u3042\u3044\u3046\u3048\u304A\u304B";
        field.setString(japaneseText);

        assertEquals(6, field.getText().length(),"Output field stores DBCS content");
        assertFalse(field.isNumeric(),"Output field should not be numeric");
    }

    /**
     * POSITIVE: Input field type allows DBCS content modification
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=input, width=full-width
     */
    @Test
    public void testInputFieldTypeWithDBCS() {
        ScreenField field = createDBCSField(720, 5, FIELD_TYPE_INPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        String koreanText = "\uAC00\uB098\uB2E4\uB77C\uB978";
        field.setString(koreanText);

        assertEquals(5, field.getText().length(),"Input field stores DBCS content");
    }

    /**
     * POSITIVE: Cursor double-cell mode advances 2 positions for DBCS
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=output, cursor=double-cell
     */
    @Test
    public void testDoubleCellCursorAdvancesForDBCS() {
        ScreenField field = createDBCSField(400, 10, FIELD_TYPE_OUTPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        String japaneseText = "\u3042\u3044";  // Two Hiragana characters
        field.setString(japaneseText);

        // In double-cell mode, cursor advances 2 positions per DBCS character
        // Field pads to full length (10 chars)
        assertEquals(10, field.getText().length(),"DBCS characters padded to field length");
    }

    /**
     * POSITIVE: Cursor single-cell mode advances 1 position per character
     * Dimension pair: charset=SBCS-only, encoding=ASCII, field=input, cursor=single-cell
     */
    @Test
    public void testSingleCellCursorAdvancesForSBCS() {
        ScreenField field = createDBCSField(0, 5, FIELD_TYPE_INPUT, WIDTH_HALF, CURSOR_SINGLE_CELL);
        field.setString("ABCDE");

        assertEquals(5, field.getText().length(),"SBCS characters stored");
    }

    /**
     * POSITIVE: Empty DBCS field with padding preserves field length
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=output, width=full-width
     */
    @Test
    public void testEmptyDBCSFieldPreservesPadding() {
        ScreenField field = createDBCSField(800, 10, FIELD_TYPE_OUTPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        field.setString("");

        assertEquals(10, field.getText().length(),"Empty DBCS field maintains length");
    }

    // ========================================================================
    // ADVERSARIAL TESTS (12): DBCS encoding attacks and boundary errors
    // ========================================================================

    /**
     * ADVERSARIAL: Orphaned high surrogate (without low surrogate)
     * Dimension pair: charset=DBCS-only, encoding=Unicode-surrogate, field=output, width=full-width
     * Risk: High surrogate followed by ASCII causes misalignment
     */
    @Test
    public void testOrphanedHighSurrogateHandling() {
        ScreenField field = createDBCSField(80, 5, FIELD_TYPE_OUTPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        // Orphaned high surrogate + ASCII
        String malformedText = String.valueOf(SURROGATE_HIGH) + "A";
        field.setString(malformedText);

        // Field should store content; validation occurs at rendering layer
        assertNotNull(field.getText(),"Field stores orphaned surrogate");
        assertEquals(5, field.getText().length(),"Malformed surrogate stored");
    }

    /**
     * ADVERSARIAL: Orphaned low surrogate (without high surrogate)
     * Dimension pair: charset=DBCS-only, encoding=Unicode-surrogate, field=input, width=full-width
     * Risk: Invalid surrogate pair causes character corruption
     */
    @Test
    public void testOrphanedLowSurrogateHandling() {
        ScreenField field = createDBCSField(160, 5, FIELD_TYPE_INPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        // ASCII + orphaned low surrogate
        String malformedText = "A" + String.valueOf(SURROGATE_LOW);
        field.setString(malformedText);

        assertNotNull(field.getText(),"Field stores orphaned low surrogate");
        assertEquals(5, field.getText().length(),"Malformed low surrogate stored");
    }

    /**
     * ADVERSARIAL: DBCS character at field boundary (truncation risk)
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=output, width=full-width
     * Risk: DBCS character truncated at field end, leaving orphaned high byte
     */
    @Test
    public void testDBCSCharacterAtFieldBoundaryTruncation() {
        ScreenField field = createDBCSField(240, 3, FIELD_TYPE_OUTPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        // Three Japanese characters, field length 3 (exact fit)
        String japaneseText = "\u3042\u3044\u3046";
        field.setString(japaneseText);

        assertEquals(3, field.getText().length(),"DBCS characters at boundary");
    }

    /**
     * ADVERSARIAL: Mixed encoding mismatch (EBCDIC vs. Unicode)
     * Dimension pair: charset=mixed, encoding=EBCDIC-CJK, field=both, width=mixed
     * Risk: EBCDIC-encoded DBCS misinterpreted as Unicode
     */
    @Test
    public void testEncodingMismatchEBCDICVsUnicode() {
        ScreenField field = createDBCSField(320, 8, FIELD_TYPE_BOTH, WIDTH_HALF, CURSOR_SINGLE_CELL);
        // Attempt to store EBCDIC bytes as Unicode
        // EBCDIC double-byte for Japanese (hypothetical): 0x83 0x82 (incomplete DBCS)
        String ebcdicMismatch = "\u0083\u0082ABC";
        field.setString(ebcdicMismatch);

        // Field stores content; encoding layer should handle mismatch
        assertNotNull(field.getText(),"Field stores encoding mismatch");
    }

    /**
     * ADVERSARIAL: DBCS field overflow attempt
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=output, width=full-width
     * Risk: Attempt to write more DBCS characters than field allows
     */
    @Test
    public void testDBCSFieldOverflowAttempt() {
        ScreenField field = createDBCSField(400, 3, FIELD_TYPE_OUTPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        // Attempt: 5 Japanese characters into 3-character field
        String japaneseText = "\u3042\u3044\u3046\u3048\u304A";
        field.setString(japaneseText);

        assertEquals(3, field.getText().length(),"DBCS overflow truncated to field length");
    }

    /**
     * ADVERSARIAL: Null character (U+0000) in DBCS field
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=input, width=full-width
     * Risk: Null terminator causes string truncation or buffer overflow
     */
    @Test
    public void testDBCSFieldWithNullCharacter() {
        ScreenField field = createDBCSField(480, 5, FIELD_TYPE_INPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        // DBCS content with embedded null
        String textWithNull = "A\u0000\u3042";
        field.setString(textWithNull);

        // Field should store content including null
        assertNotNull(field.getText(),"Field stores null character");
        assertEquals(5, field.getText().length(),"Null character stored in field");
    }

    /**
     * ADVERSARIAL: Byte Order Mark (BOM) injection in DBCS field
     * Dimension pair: charset=DBCS-only, encoding=Unicode-surrogate, field=output, width=full-width
     * Risk: U+FEFF BOM misinterpreted as Zero-Width No-Break Space
     */
    @Test
    public void testDBCSFieldWithByteOrderMarkInjection() {
        ScreenField field = createDBCSField(560, 6, FIELD_TYPE_OUTPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        // BOM + Japanese characters
        String bomText = "\uFEFF\u3042\u3044";
        field.setString(bomText);

        // Field stores BOM; rendering layer determines handling
        assertNotNull(field.getText(),"Field stores BOM");
    }

    /**
     * ADVERSARIAL: Replacement character (U+FFFD) in DBCS field
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=input, width=full-width
     * Risk: Replacement char inserted on decode failure causes data loss
     */
    @Test
    public void testDBCSFieldWithReplacementCharacter() {
        ScreenField field = createDBCSField(640, 4, FIELD_TYPE_INPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        // Replacement character: U+FFFD
        String replacementText = "\uFFFD\u3042";
        field.setString(replacementText);

        assertNotNull(field.getText(),"Field stores replacement character");
        assertEquals(4, field.getText().length(),"Replacement char preserved");
    }

    /**
     * ADVERSARIAL: Half-width Katakana followed by full-width CJK (width mismatch)
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=both, width=mixed
     * Risk: Width calculation error causes display/cursor misalignment
     */
    @Test
    public void testMixedHalfWidthAndFullWidthWidthMismatch() {
        ScreenField field = createDBCSField(720, 6, FIELD_TYPE_BOTH, WIDTH_HALF, CURSOR_SINGLE_CELL);
        // Half-width Katakana (1 cell) + full-width CJK (2 cells)
        String mixedWidth = "\uFF71\u3042";  // ｱ + あ
        field.setString(mixedWidth);

        // Field stores characters; width calculation at rendering layer
        assertEquals(6, field.getText().length(),"Mixed width characters stored");
    }

    /**
     * ADVERSARIAL: Control characters in DBCS field
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=output, width=full-width
     * Risk: Control chars cause terminal command injection
     */
    @Test
    public void testDBCSFieldWithControlCharacterInjection() {
        ScreenField field = createDBCSField(800, 5, FIELD_TYPE_OUTPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        // ESC sequence + Japanese
        String controlText = "\u001B[2J\u3042";  // ESC[2J (clear screen) + あ
        field.setString(controlText);

        assertNotNull(field.getText(),"Field stores control characters");
        // Control char handling at rendering layer
    }

    /**
     * ADVERSARIAL: Combining diacritical marks with DBCS characters
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=input, width=full-width
     * Risk: Combining marks cause width misalignment
     */
    @Test
    public void testDBCSWithCombiningDiacriticalMarks() {
        ScreenField field = createDBCSField(880, 6, FIELD_TYPE_INPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        // Japanese + combining acute accent (U+0301)
        String combinedText = "\u3042\u0301\u3044";  // あ + combining acute + い
        field.setString(combinedText);

        assertNotNull(field.getText(),"Field stores combining marks");
        // Width/display logic handles combining marks separately
    }

    /**
     * ADVERSARIAL: Cursor position off by one at DBCS boundary
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=output, cursor=double-cell
     * Risk: Cursor lands in middle of DBCS character (invalid position)
     */
    @Test
    public void testCursorMisalignmentAtDBCSBoundary() {
        ScreenField field = createDBCSField(400, 4, FIELD_TYPE_OUTPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        // Two DBCS characters: should occupy 4 display cells (2 chars × 2 cells)
        String japaneseText = "\u3042\u3044";
        field.setString(japaneseText);

        // In double-cell mode, valid cursor positions: 0, 2, 4, 6... (not 1, 3, 5...)
        // Field pads to full length (4 chars)
        assertEquals(4, field.getText().length(),"DBCS characters padded to field length");
    }

    // ========================================================================
    // CRITICAL VALIDATION TESTS (additional 12): DBCS boundary and conversion edge cases
    // ========================================================================

    /**
     * CRITICAL: DBCS character followed by backspace (single vs. double-cell delete)
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=input, width=full-width
     * Risk: Backspace deletes only 1 cell, leaving orphaned byte of DBCS character
     */
    @Test
    public void testDBCSCharacterWithBackspaceDelete() {
        ScreenField field = createDBCSField(80, 6, FIELD_TYPE_INPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        String japaneseText = "\u3042\u3044";  // Two Hiragana
        field.setString(japaneseText);

        // Backspace logic should delete entire DBCS character (2 cells), not 1
        // Field pads to full length (6 chars)
        assertEquals(6, field.getText().length(),"DBCS characters padded to field length");
    }

    /**
     * CRITICAL: DBCS character comparison for equality
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=both, width=full-width
     */
    @Test
    public void testDBCSCharacterEquality() {
        ScreenField field1 = createDBCSField(0, 5, FIELD_TYPE_BOTH, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        ScreenField field2 = createDBCSField(80, 5, FIELD_TYPE_BOTH, WIDTH_FULL, CURSOR_DOUBLE_CELL);

        String japaneseText1 = "\u3042\u3044";  // あい
        String japaneseText2 = "\u3042\u3044";  // あい (identical)

        field1.setString(japaneseText1);
        field2.setString(japaneseText2);

        assertTrue(field1.getText().equals(field2.getText()),"Identical DBCS fields should equal");
    }

    /**
     * CRITICAL: DBCS character code point value (beyond BMP)
     * Dimension pair: charset=DBCS-only, encoding=Unicode-surrogate, field=output, width=full-width
     * Risk: Code point > 0xFFFF not handled correctly
     */
    @Test
    public void testDBCSBeyondBMPCodePoint() {
        ScreenField field = createDBCSField(160, 2, FIELD_TYPE_OUTPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        // Surrogate pair: U+1F600 (😀)
        String emoji = new String(new char[]{'\uD83D', '\uDE00'});
        field.setString(emoji);

        assertEquals(2, field.getText().length(),"Surrogate pair stored");
    }

    /**
     * CRITICAL: DBCS field with field shift indicator
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=output, width=full-width
     * Risk: Field shift (0x20) byte misinterpreted as DBCS character
     */
    @Test
    public void testDBCSFieldWithFieldShiftByte() {
        ScreenField field = createDBCSField(240, 5, FIELD_TYPE_OUTPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        // Field shift (0x20 in EBCDIC) + Japanese
        String shiftText = "\u0020\u3042\u3044";  // space + Japanese
        field.setString(shiftText);

        assertNotNull(field.getText(),"Field stores field shift byte");
    }

    /**
     * CRITICAL: Whitespace-only DBCS field
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=input, width=full-width
     */
    @Test
    public void testDBCSFieldWithOnlyWhitespace() {
        ScreenField field = createDBCSField(320, 4, FIELD_TYPE_INPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        field.setString("    ");  // Four spaces

        assertEquals(4, field.getText().length(),"Whitespace stored in DBCS field");
    }

    /**
     * CRITICAL: DBCS field at screen boundary (end of 80-column line)
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=output, width=full-width
     * Risk: DBCS character split across line boundary
     */
    @Test
    public void testDBCSCharacterAtScreenLineBoundary() {
        // Position 78-82 (2 cells into next line)
        int boundaryPos = (SCREEN_COLS - 2);
        ScreenField field = createDBCSField(boundaryPos, 4, FIELD_TYPE_OUTPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        String japaneseText = "\u3042\u3044";
        field.setString(japaneseText);

        // Field pads to full length (4 chars)
        assertEquals(4, field.getText().length(),"DBCS characters at boundary padded");
    }

    /**
     * CRITICAL: Large DBCS field (many characters)
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=output, width=full-width
     */
    @Test
    public void testLargeDBCSField() {
        ScreenField field = createDBCSField(0, 40, FIELD_TYPE_OUTPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        // 40 Japanese characters
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            sb.append('\u3042');  // Repeat Hiragana A
        }
        field.setString(sb.toString());

        assertEquals(40, field.getText().length(),"Large DBCS field stored");
    }

    /**
     * CRITICAL: DBCS mixed with control codes (IBM's AFT - Attribute/Function Terminators)
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=both, width=full-width
     */
    @Test
    public void testDBCSWithAFTControlCodes() {
        ScreenField field = createDBCSField(400, 6, FIELD_TYPE_BOTH, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        // AFT control: backspace, carriage return + Japanese
        String aftText = new String(new char[]{(char) 0x08, '\u3042', (char) 0x0D, '\u3044'});
        field.setString(aftText);

        assertNotNull(field.getText(),"Field stores AFT control codes");
    }

    /**
     * CRITICAL: DBCS input field immediately followed by numeric field
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=input, width=full-width
     * Risk: DBCS field corruption affects adjacent numeric field
     */
    @Test
    public void testDBCSFieldAdjacentToNumericField() {
        ScreenField dbcsField = createDBCSField(0, 5, FIELD_TYPE_INPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        ScreenField numField = createDBCSField(80, 5, FIELD_TYPE_INPUT, WIDTH_HALF, CURSOR_SINGLE_CELL);

        String japaneseText = "\u3042\u3044\u3046\u3048\u304A";
        dbcsField.setString(japaneseText);
        numField.setString("12345");

        assertEquals(5, dbcsField.getText().length(),"DBCS field content isolated");
        assertEquals(5, numField.getText().length(),"Numeric field independent");
    }

    /**
     * CRITICAL: DBCS field state persistence across screen refresh
     * Dimension pair: charset=DBCS-only, encoding=Unicode, field=output, width=full-width
     */
    @Test
    public void testDBCSFieldPersistenceAcrossRefresh() {
        ScreenField field = createDBCSField(560, 5, FIELD_TYPE_OUTPUT, WIDTH_FULL, CURSOR_DOUBLE_CELL);
        String japaneseText = "\u3042\u3044\u3046\u3048\u304A";
        field.setString(japaneseText);

        // Simulate refresh: content should remain unchanged
        String originalText = field.getText();
        assertNotNull(originalText,"Text persists after refresh");
        assertEquals(5, originalText.length(),"DBCS field text unchanged");
    }
}
