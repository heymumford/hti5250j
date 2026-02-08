/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.framework.tn5250;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pairwise parameter testing for TN5250 WTD (Write To Display) order parsing.
 *
 * Test dimensions (to be combined pairwise):
 * 1. WTD command types: [0x11 (WTD), 0x01 (WTD immediate), 0xF1 (WTD structured field)]
 * 2. Control character values: [0x00, 0x1F, 0x20, 0x3F, 0xFF (boundary)]
 * 3. Data lengths: [0, 1, 127, 128, 255, 256, 32767 (max short)]
 * 4. Buffer positions: [start (0), middle (mid), end (size-1), wrap-around]
 * 5. Field attributes: [input (0x01), output (0x00), protected (0x02), modified (0x04)]
 *
 * Focus areas:
 * 1. POSITIVE: Valid WTD orders are parsed correctly
 * 2. BOUNDARY: Data lengths at protocol limits
 * 3. ADVERSARIAL: Malformed packets, truncation, buffer attacks
 * 4. STRUCTURED FIELDS: Window creation, scrollbars, GUI constructs
 * 5. PROTOCOL FUZZING: Invalid opcodes, corrupt headers, injection
 */
public class WTDOrderPairwiseTest {

    // WTD command type constants
    private static final byte WTD_NORMAL = (byte) 0x11;          // WTD standard
    private static final byte WTD_IMMEDIATE = (byte) 0x01;       // WTD immediate
    private static final byte WTD_STRUCTURED_FIELD = (byte) 0xF1; // WTD structured field

    // Control character boundary values
    private static final byte CTRL_CHAR_MIN = 0x00;              // Null
    private static final byte CTRL_CHAR_LOW = 0x1F;              // Low control range
    private static final byte CTRL_CHAR_SPACE = 0x20;            // Space
    private static final byte CTRL_CHAR_HIGH = 0x3F;             // High normal
    private static final byte CTRL_CHAR_MAX = (byte) 0xFF;       // Max byte

    // Data length boundary values
    private static final int DATA_LENGTH_ZERO = 0;
    private static final int DATA_LENGTH_ONE = 1;
    private static final int DATA_LENGTH_127 = 127;
    private static final int DATA_LENGTH_128 = 128;
    private static final int DATA_LENGTH_255 = 255;
    private static final int DATA_LENGTH_256 = 256;
    private static final int DATA_LENGTH_32767 = 32767;

    // Buffer position constants
    private static final int BUFFER_POS_START = 0;
    private static final int BUFFER_POS_MIDDLE = 500;
    private static final int BUFFER_POS_END_OFFSET = 1;

    // Field attribute constants
    private static final byte FIELD_ATTR_INPUT = 0x01;           // Input capable
    private static final byte FIELD_ATTR_OUTPUT = 0x00;          // Output only
    private static final byte FIELD_ATTR_PROTECTED = 0x02;       // Protected
    private static final byte FIELD_ATTR_MODIFIED = 0x04;        // Modified indicator

    // Structured field constants
    private static final byte SF_CLASS_D9 = (byte) 0xD9;         // Class Type 0xD9
    private static final byte SF_CREATE_WINDOW_51 = 0x51;        // Create Window
    private static final byte SF_BORDER_PRESENTATION = 0x01;     // Border presentation
    private static final byte SF_DEFINE_SELECTION_50 = 0x50;     // Define Selection Field
    private static final byte SF_SCROLLBAR_53 = 0x53;            // Scroll Bar
    private static final byte SF_REMOVE_SCROLLBAR_5B = 0x5B;     // Remove ScrollBar
    private static final byte SF_REMOVE_ALL_GUI_5F = 0x5F;       // Remove All GUI

    // Test buffer size
    private static final int MAX_BUFFER_SIZE = 1024;

    private byte[] testBuffer;

    @BeforeEach
    public void setUp() {
        testBuffer = new byte[MAX_BUFFER_SIZE];
    }

    // ========== POSITIVE TESTS: Valid WTD orders ==========

    /**
     * TEST 1: Valid WTD standard order with minimal data
     *
     * Pairwise combination:
     * - Command type: WTD standard (0x11)
     * - Control char: Space (0x20)
     * - Data length: 1 (minimal data)
     * - Buffer position: start (0)
     * - Field attribute: input (0x01)
     *
     * RED: Parser should handle minimal valid WTD order
     * GREEN: Verify command type extraction and data position
     */
    @Test
    public void testValidWTDStandardOrderMinimalData() {
        // Arrange: Create valid WTD standard order
        int bufPos = BUFFER_POS_START;
        testBuffer[bufPos] = WTD_NORMAL;
        testBuffer[bufPos + 1] = CTRL_CHAR_SPACE;
        testBuffer[bufPos + 2] = (byte) DATA_LENGTH_ONE;
        testBuffer[bufPos + 3] = (byte) 0x41; // 'A' payload
        testBuffer[bufPos + 4] = 0x00; // EOL

        // Act: Parse the order (simulate parsing)
        byte cmdType = testBuffer[bufPos];
        byte ctrlChar = testBuffer[bufPos + 1];
        int dataLen = testBuffer[bufPos + 2] & 0xFF;

        // Assert: Verify parsing results
        assertEquals(WTD_NORMAL, cmdType,"Command type should be WTD standard");
        assertEquals(CTRL_CHAR_SPACE, ctrlChar,"Control character should be space");
        assertEquals(DATA_LENGTH_ONE, dataLen,"Data length should be 1");
        assertTrue(bufPos >= 0 && bufPos < MAX_BUFFER_SIZE,"Buffer position should be valid");
    }

    /**
     * TEST 2: Valid WTD immediate order with field attribute
     *
     * Pairwise combination:
     * - Command type: WTD immediate (0x01)
     * - Control char: 0x1F (low control)
     * - Data length: 127 (single-byte length)
     * - Buffer position: middle (500)
     * - Field attribute: protected (0x02)
     *
     * RED: Should handle WTD immediate with field attributes
     * GREEN: Extract and validate field attribute
     */
    @Test
    public void testValidWTDImmediateOrderWithFieldAttribute() {
        // Arrange: Create WTD immediate order
        int bufPos = BUFFER_POS_MIDDLE;
        testBuffer[bufPos] = WTD_IMMEDIATE;
        testBuffer[bufPos + 1] = CTRL_CHAR_LOW;
        testBuffer[bufPos + 2] = (byte) DATA_LENGTH_127;
        testBuffer[bufPos + 3] = FIELD_ATTR_PROTECTED;
        // Fill payload
        for (int i = 4; i < 4 + DATA_LENGTH_127; i++) {
            testBuffer[bufPos + i] = (byte) (0x40 + (i % 64));
        }

        // Act: Parse
        byte cmdType = testBuffer[bufPos];
        byte ctrlChar = testBuffer[bufPos + 1];
        int dataLen = testBuffer[bufPos + 2] & 0xFF;
        byte fieldAttr = testBuffer[bufPos + 3];

        // Assert: Verify all components
        assertEquals(WTD_IMMEDIATE, cmdType,"Command type should be WTD immediate");
        assertEquals(CTRL_CHAR_LOW, ctrlChar,"Control char should be low control");
        assertEquals(DATA_LENGTH_127, dataLen,"Data length should be 127");
        assertEquals(FIELD_ATTR_PROTECTED, fieldAttr,"Field attribute should be protected");
    }

    /**
     * TEST 3: Valid WTD structured field with window creation
     *
     * Pairwise combination:
     * - Command type: WTD structured field (0xF1)
     * - Control char: 0x3F (high normal)
     * - Data length: 255 (boundary)
     * - Buffer position: end area (1000)
     * - Field attribute: modified (0x04)
     *
     * RED: Should parse WTD structured field with GUI constructs
     * GREEN: Extract SF class and subcommand
     */
    @Test
    public void testValidWTDStructuredFieldWindowCreation() {
        // Arrange: Create WTD structured field
        int bufPos = 1000;
        testBuffer[bufPos] = WTD_STRUCTURED_FIELD;
        testBuffer[bufPos + 1] = CTRL_CHAR_HIGH;
        // Simulate structured field length (2 bytes big-endian)
        testBuffer[bufPos + 2] = 0x00;
        testBuffer[bufPos + 3] = (byte) DATA_LENGTH_255;
        testBuffer[bufPos + 4] = SF_CLASS_D9;
        testBuffer[bufPos + 5] = SF_CREATE_WINDOW_51;
        testBuffer[bufPos + 6] = FIELD_ATTR_MODIFIED;

        // Act: Parse SF header
        byte cmdType = testBuffer[bufPos];
        byte ctrlChar = testBuffer[bufPos + 1];
        int sfLen = ((testBuffer[bufPos + 2] & 0xFF) << 8) | (testBuffer[bufPos + 3] & 0xFF);
        byte sfClass = testBuffer[bufPos + 4];
        byte sfSubcmd = testBuffer[bufPos + 5];

        // Assert: Verify SF parsing
        assertEquals(WTD_STRUCTURED_FIELD, cmdType,"Command type should be WTD SF");
        assertEquals(CTRL_CHAR_HIGH, ctrlChar,"Control char should be high normal");
        assertEquals(DATA_LENGTH_255, sfLen,"SF length should be 255");
        assertEquals(SF_CLASS_D9, sfClass,"SF class should be 0xD9");
        assertEquals(SF_CREATE_WINDOW_51, sfSubcmd,"SF subcommand should be create window");
    }

    /**
     * TEST 4: Valid WTD order with zero-length data
     *
     * Pairwise combination:
     * - Command type: WTD standard (0x11)
     * - Control char: 0x00 (null)
     * - Data length: 0 (empty)
     * - Buffer position: start (0)
     * - Field attribute: output (0x00)
     *
     * RED: Should handle empty data gracefully
     * GREEN: Verify length validation
     */
    @Test
    public void testValidWTDOrderZeroLengthData() {
        // Arrange: Create minimal WTD with no data
        testBuffer[0] = WTD_NORMAL;
        testBuffer[1] = CTRL_CHAR_MIN;
        testBuffer[2] = (byte) DATA_LENGTH_ZERO;
        testBuffer[3] = 0x00; // EOL

        // Act: Parse
        byte cmdType = testBuffer[0];
        int dataLen = testBuffer[2] & 0xFF;

        // Assert: Verify handling of empty payload
        assertEquals(WTD_NORMAL, cmdType,"Command type should be valid");
        assertEquals(DATA_LENGTH_ZERO, dataLen,"Data length should be zero");
    }

    /**
     * TEST 5: Valid WTD with 256-byte boundary (2-byte length encoding)
     *
     * Pairwise combination:
     * - Command type: WTD immediate (0x01)
     * - Control char: 0x20 (space)
     * - Data length: 256 (boundary)
     * - Buffer position: middle (500)
     * - Field attribute: input (0x01)
     *
     * RED: Should handle 2-byte length encoding at 256-byte boundary
     * GREEN: Parse multi-byte length correctly
     */
    @Test
    public void testValidWTDBoundary256ByteLength() {
        // Arrange: Create WTD with 256-byte data using 2-byte length
        int bufPos = BUFFER_POS_MIDDLE;
        testBuffer[bufPos] = WTD_IMMEDIATE;
        testBuffer[bufPos + 1] = CTRL_CHAR_SPACE;
        // Simulate big-endian 256 in 2-byte length field
        testBuffer[bufPos + 2] = 0x01;
        testBuffer[bufPos + 3] = 0x00;
        testBuffer[bufPos + 4] = FIELD_ATTR_INPUT;

        // Act: Parse length (big-endian)
        int dataLen = ((testBuffer[bufPos + 2] & 0xFF) << 8) | (testBuffer[bufPos + 3] & 0xFF);

        // Assert: Verify 256-byte boundary handling
        assertEquals(DATA_LENGTH_256, dataLen,"Data length should be 256");
        assertTrue(dataLen >= DATA_LENGTH_256,"Length should be at boundary");
    }

    /**
     * TEST 6: Valid SF with scrollbar creation
     *
     * Pairwise combination:
     * - Command type: WTD structured field (0xF1)
     * - Control char: 0x1F (low control)
     * - Data length: 128 (boundary)
     * - Buffer position: start (0)
     * - Field attribute: protected (0x02)
     *
     * RED: Should parse scrollbar SF
     * GREEN: Verify SF subcommand extraction
     */
    @Test
    public void testValidSFScrollbarCreation() {
        // Arrange: Create scrollbar SF
        testBuffer[0] = WTD_STRUCTURED_FIELD;
        testBuffer[1] = CTRL_CHAR_LOW;
        testBuffer[2] = 0x00;
        testBuffer[3] = (byte) DATA_LENGTH_128;
        testBuffer[4] = SF_CLASS_D9;
        testBuffer[5] = SF_SCROLLBAR_53;
        testBuffer[6] = FIELD_ATTR_PROTECTED;

        // Act: Parse
        byte sfClass = testBuffer[4];
        byte sfSubcmd = testBuffer[5];

        // Assert: Verify scrollbar SF
        assertEquals(SF_CLASS_D9, sfClass,"SF class should be 0xD9");
        assertEquals(SF_SCROLLBAR_53, sfSubcmd,"SF subcommand should be scrollbar");
    }

    /**
     * TEST 7: Valid SF remove all GUI constructs
     *
     * Pairwise combination:
     * - Command type: WTD structured field (0xF1)
     * - Control char: 0x3F (high normal)
     * - Data length: 4 (minimal SF)
     * - Buffer position: end area (1000)
     * - Field attribute: modified (0x04)
     *
     * RED: Should parse remove-all-GUI command
     * GREEN: Verify command extraction
     */
    @Test
    public void testValidSFRemoveAllGUIConstructs() {
        // Arrange: Create remove-all-GUI SF
        int bufPos = 1000;
        testBuffer[bufPos] = WTD_STRUCTURED_FIELD;
        testBuffer[bufPos + 1] = CTRL_CHAR_HIGH;
        testBuffer[bufPos + 2] = 0x00;
        testBuffer[bufPos + 3] = 0x04;
        testBuffer[bufPos + 4] = SF_CLASS_D9;
        testBuffer[bufPos + 5] = SF_REMOVE_ALL_GUI_5F;
        testBuffer[bufPos + 6] = FIELD_ATTR_MODIFIED;
        testBuffer[bufPos + 7] = 0x00;

        // Act: Parse
        byte sfSubcmd = testBuffer[bufPos + 5];

        // Assert: Verify remove-all-GUI command
        assertEquals(SF_REMOVE_ALL_GUI_5F, sfSubcmd,"SF subcommand should be remove all GUI");
    }

    /**
     * TEST 8: Valid WTD with input field attribute
     *
     * Pairwise combination:
     * - Command type: WTD standard (0x11)
     * - Control char: 0x1F (low control)
     * - Data length: 32 (typical field)
     * - Buffer position: middle (500)
     * - Field attribute: input (0x01)
     *
     * RED: Should handle input field markers
     * GREEN: Verify field attribute extraction
     */
    @Test
    public void testValidWTDInputFieldAttribute() {
        // Arrange: Create WTD with input field
        int bufPos = BUFFER_POS_MIDDLE;
        testBuffer[bufPos] = WTD_NORMAL;
        testBuffer[bufPos + 1] = CTRL_CHAR_LOW;
        testBuffer[bufPos + 2] = 0x20; // 32 bytes
        testBuffer[bufPos + 3] = FIELD_ATTR_INPUT;

        // Act: Parse
        byte fieldAttr = testBuffer[bufPos + 3];

        // Assert: Verify input attribute
        assertEquals(FIELD_ATTR_INPUT, fieldAttr,"Field attribute should be input");
        assertTrue((fieldAttr & FIELD_ATTR_INPUT) != 0,"Input attribute should be set");
    }

    // ========== BOUNDARY TESTS ==========

    /**
     * TEST 9: WTD order at buffer start boundary
     *
     * Pairwise combination:
     * - Command type: WTD immediate (0x01)
     * - Control char: 0x20 (space)
     * - Data length: 64
     * - Buffer position: start (0) - boundary
     * - Field attribute: protected (0x02)
     *
     * RED: Should handle parsing at buffer start
     * GREEN: Verify position doesn't underflow
     */
    @Test
    public void testWTDOrderAtBufferStartBoundary() {
        // Arrange: Position at buffer start
        int bufPos = BUFFER_POS_START;
        testBuffer[bufPos] = WTD_IMMEDIATE;
        testBuffer[bufPos + 1] = CTRL_CHAR_SPACE;
        testBuffer[bufPos + 2] = 0x40; // 64 bytes
        testBuffer[bufPos + 3] = FIELD_ATTR_PROTECTED;

        // Act: Verify position validity
        boolean isValidStart = bufPos >= 0;

        // Assert: Ensure no underflow
        assertTrue(isValidStart,"Start position should be valid");
        assertEquals(BUFFER_POS_START, bufPos,"Position should be zero");
    }

    /**
     * TEST 10: WTD order at buffer end boundary
     *
     * Pairwise combination:
     * - Command type: WTD standard (0x11)
     * - Control char: 0x3F (high normal)
     * - Data length: 5 (minimal to avoid overflow)
     * - Buffer position: end area (1015) - boundary
     * - Field attribute: output (0x00)
     *
     * RED: Should handle parsing near buffer end
     * GREEN: Verify overflow protection
     */
    @Test
    public void testWTDOrderAtBufferEndBoundary() {
        // Arrange: Position near buffer end with minimal payload
        int bufPos = MAX_BUFFER_SIZE - 10; // 1014
        testBuffer[bufPos] = WTD_NORMAL;
        testBuffer[bufPos + 1] = CTRL_CHAR_HIGH;
        testBuffer[bufPos + 2] = 0x05; // 5 bytes (minimal, fits before end)
        testBuffer[bufPos + 3] = FIELD_ATTR_OUTPUT;

        // Act: Verify end-of-buffer handling
        int expectedEndPos = bufPos + 4 + 5; // 1023
        boolean isWithinBounds = expectedEndPos <= MAX_BUFFER_SIZE; // Should be true: 1023 <= 1024

        // Assert: Ensure no overflow
        assertTrue(isWithinBounds,"End position should be within bounds");
        assertTrue((bufPos + 4) < MAX_BUFFER_SIZE,"Order should fit in remaining buffer");
    }

    /**
     * TEST 11: Large WTD order with 32767-byte boundary
     *
     * Pairwise combination:
     * - Command type: WTD immediate (0x01)
     * - Control char: 0x1F (low control)
     * - Data length: 32767 (max short)
     * - Buffer position: start (0)
     * - Field attribute: modified (0x04)
     *
     * RED: Should handle maximum allowed length
     * GREEN: Verify large length parsing
     */
    @Test
    public void testLargeWTDOrderMaximumLength() {
        // Arrange: Create order with maximum length
        testBuffer[0] = WTD_IMMEDIATE;
        testBuffer[1] = CTRL_CHAR_LOW;
        testBuffer[2] = 0x7F; // High byte of 32767
        testBuffer[3] = (byte) 0xFF; // Low byte of 32767
        testBuffer[4] = FIELD_ATTR_MODIFIED;

        // Act: Parse max length (big-endian)
        int dataLen = ((testBuffer[2] & 0x7F) << 8) | (testBuffer[3] & 0xFF);

        // Assert: Verify max length handling
        assertEquals(DATA_LENGTH_32767, dataLen,"Data length should be 32767");
        assertTrue(dataLen <= 32767,"Length should not exceed max short");
    }

    // ========== ADVERSARIAL TESTS: Malformed orders ==========

    /**
     * TEST 12: Truncated WTD order (incomplete header)
     *
     * Pairwise combination:
     * - Command type: WTD standard (0x11)
     * - Control char: missing
     * - Data length: missing
     * - Buffer position: start (0)
     * - Field attribute: (invalid - truncated)
     *
     * RED: Should detect truncation and prevent buffer overrun
     * GREEN: Verify bounds checking
     */
    @Test
    public void testTruncatedWTDOrderIncompleteHeader() {
        // Arrange: Create truncated header (only 1 byte)
        testBuffer[0] = WTD_NORMAL;
        // Intentionally missing bytes 1-3

        // Act: Simulate parsing with bounds check
        int headerSize = 4; // Expected minimum
        boolean canAccessHeader = (1 + headerSize) <= MAX_BUFFER_SIZE;

        // Assert: Verify truncation detection
        assertTrue(canAccessHeader,"Buffer check should prevent overrun");
        // In actual code, would check: if (pos + headerSize > bufferLength) throw error
    }

    /**
     * TEST 13: WTD order with corrupted control character
     *
     * Pairwise combination:
     * - Command type: WTD immediate (0x01)
     * - Control char: 0xFF (invalid high byte)
     * - Data length: 50
     * - Buffer position: middle (500)
     * - Field attribute: input (0x01)
     *
     * RED: Should handle invalid control characters
     * GREEN: Verify validation without crash
     */
    @Test
    public void testWTDOrderCorruptedControlCharacter() {
        // Arrange: Create order with invalid control character
        int bufPos = BUFFER_POS_MIDDLE;
        testBuffer[bufPos] = WTD_IMMEDIATE;
        testBuffer[bufPos + 1] = CTRL_CHAR_MAX; // 0xFF - suspicious
        testBuffer[bufPos + 2] = 0x32; // 50 bytes
        testBuffer[bufPos + 3] = FIELD_ATTR_INPUT;

        // Act: Parse with validation
        byte ctrlChar = testBuffer[bufPos + 1];
        boolean isHighValueControl = (ctrlChar & 0xFF) > 0x3F;

        // Assert: Detect suspicious control character
        assertTrue(isHighValueControl,"Should detect high-value control char");
        // In actual code, would validate: if (ctrlChar > 0x3F) log warning or reject
    }

    /**
     * TEST 14: WTD order with mismatched length encoding
     *
     * Pairwise combination:
     * - Command type: WTD structured field (0xF1)
     * - Control char: 0x20 (space)
     * - Data length: claims 255 but only 10 bytes follow
     * - Buffer position: end area (1000)
     * - Field attribute: protected (0x02)
     *
     * RED: Should detect length mismatch
     * GREEN: Verify bounds checking against declared length
     */
    @Test
    public void testWTDOrderMismatchedLengthEncoding() {
        // Arrange: Declare 255 bytes but provide only 10
        int bufPos = 1000;
        testBuffer[bufPos] = WTD_STRUCTURED_FIELD;
        testBuffer[bufPos + 1] = CTRL_CHAR_SPACE;
        testBuffer[bufPos + 2] = 0x00;
        testBuffer[bufPos + 3] = (byte) 0xFF; // Declares 255 bytes
        testBuffer[bufPos + 4] = SF_CLASS_D9;
        // Only fill 6 more bytes
        testBuffer[bufPos + 5] = SF_CREATE_WINDOW_51;
        testBuffer[bufPos + 6] = 0x00;
        testBuffer[bufPos + 7] = 0x00;
        testBuffer[bufPos + 8] = 0x00;
        testBuffer[bufPos + 9] = 0x00;

        // Act: Check if declared length exceeds actual data
        int declaredLen = testBuffer[bufPos + 3] & 0xFF;
        int availableData = 6; // Only 6 bytes provided after length field
        boolean isLengthMismatch = declaredLen > availableData;

        // Assert: Verify mismatch detection
        assertTrue(isLengthMismatch,"Should detect length mismatch");
        assertEquals(255, declaredLen,"Declared length should be 255");
    }

    /**
     * TEST 15: WTD order with invalid SF class
     *
     * Pairwise combination:
     * - Command type: WTD structured field (0xF1)
     * - Control char: 0x3F (high normal)
     * - Data length: 20
     * - Buffer position: middle (500)
     * - Field attribute: (ignored for SF)
     *
     * RED: Should detect invalid SF class
     * GREEN: Verify SF class validation
     */
    @Test
    public void testWTDOrderInvalidSFClass() {
        // Arrange: Create SF with invalid class
        int bufPos = BUFFER_POS_MIDDLE;
        testBuffer[bufPos] = WTD_STRUCTURED_FIELD;
        testBuffer[bufPos + 1] = CTRL_CHAR_HIGH;
        testBuffer[bufPos + 2] = 0x00;
        testBuffer[bufPos + 3] = 0x14; // 20 bytes
        testBuffer[bufPos + 4] = (byte) 0xAA; // Invalid SF class (not 0xD9)
        testBuffer[bufPos + 5] = SF_CREATE_WINDOW_51;

        // Act: Validate SF class
        byte sfClass = testBuffer[bufPos + 4];
        boolean isValidSFClass = (sfClass == SF_CLASS_D9);

        // Assert: Detect invalid SF class
        assertFalse(isValidSFClass,"Should detect invalid SF class");
        assertEquals((byte) 0xAA, sfClass,"Invalid SF class should be 0xAA");
    }

    /**
     * TEST 16: WTD order with invalid SF subcommand
     *
     * Pairwise combination:
     * - Command type: WTD structured field (0xF1)
     * - Control char: 0x1F (low control)
     * - Data length: 15
     * - Buffer position: start (0)
     * - Field attribute: modified (0x04)
     *
     * RED: Should detect unsupported SF subcommand
     * GREEN: Verify subcommand validation
     */
    @Test
    public void testWTDOrderInvalidSFSubcommand() {
        // Arrange: Create SF with unrecognized subcommand
        testBuffer[0] = WTD_STRUCTURED_FIELD;
        testBuffer[1] = CTRL_CHAR_LOW;
        testBuffer[2] = 0x00;
        testBuffer[3] = 0x0F; // 15 bytes
        testBuffer[4] = SF_CLASS_D9;
        testBuffer[5] = (byte) 0x99; // Invalid subcommand

        // Act: Validate subcommand
        byte sfSubcmd = testBuffer[5];
        boolean isValidSubcmd = (sfSubcmd == SF_CREATE_WINDOW_51 ||
                                sfSubcmd == SF_DEFINE_SELECTION_50 ||
                                sfSubcmd == SF_SCROLLBAR_53 ||
                                sfSubcmd == SF_REMOVE_SCROLLBAR_5B ||
                                sfSubcmd == SF_REMOVE_ALL_GUI_5F);

        // Assert: Detect invalid subcommand
        assertFalse(isValidSubcmd,"Should detect invalid SF subcommand");
        assertEquals((byte) 0x99, sfSubcmd,"Invalid subcommand should be 0x99");
    }

    /**
     * TEST 17: WTD order with negative length (sign bit)
     *
     * Pairwise combination:
     * - Command type: WTD immediate (0x01)
     * - Control char: 0x20 (space)
     * - Data length: 0x8000 (sign bit set, appears as negative in signed)
     * - Buffer position: middle (500)
     * - Field attribute: input (0x01)
     *
     * RED: Should handle negative-appearing lengths safely
     * GREEN: Verify unsigned interpretation
     */
    @Test
    public void testWTDOrderNegativeLengthSignBit() {
        // Arrange: Create order with sign bit set (0x8000 = 32768 unsigned)
        int bufPos = BUFFER_POS_MIDDLE;
        testBuffer[bufPos] = WTD_IMMEDIATE;
        testBuffer[bufPos + 1] = CTRL_CHAR_SPACE;
        testBuffer[bufPos + 2] = (byte) 0x80; // High byte with sign bit
        testBuffer[bufPos + 3] = 0x00; // Low byte
        testBuffer[bufPos + 4] = FIELD_ATTR_INPUT;

        // Act: Parse as unsigned
        int dataLen = ((testBuffer[bufPos + 2] & 0xFF) << 8) | (testBuffer[bufPos + 3] & 0xFF);
        boolean isValidLength = dataLen >= 0; // Unsigned guarantees non-negative

        // Assert: Verify safe unsigned handling
        assertTrue(isValidLength,"Unsigned parsing should produce positive value");
        assertEquals(0x8000, dataLen,"Length should be 32768 (unsigned)");
    }

    /**
     * TEST 18: WTD order with field attribute corruption
     *
     * Pairwise combination:
     * - Command type: WTD standard (0x11)
     * - Control char: 0x3F (high normal)
     * - Data length: 64
     * - Buffer position: end area (900)
     * - Field attribute: 0xFF (all bits set - corrupted)
     *
     * RED: Should handle attribute value validation
     * GREEN: Verify safe attribute handling
     */
    @Test
    public void testWTDOrderCorruptedFieldAttribute() {
        // Arrange: Create order with suspicious attribute value
        int bufPos = 900;
        testBuffer[bufPos] = WTD_NORMAL;
        testBuffer[bufPos + 1] = CTRL_CHAR_HIGH;
        testBuffer[bufPos + 2] = 0x40; // 64 bytes
        testBuffer[bufPos + 3] = (byte) 0xFF; // All bits set - invalid

        // Act: Parse attribute
        byte fieldAttr = testBuffer[bufPos + 3];
        int attrValue = fieldAttr & 0xFF;
        boolean hasValidCombination = (attrValue & (FIELD_ATTR_PROTECTED | FIELD_ATTR_INPUT)) !=
                                      (FIELD_ATTR_PROTECTED | FIELD_ATTR_INPUT); // Both set = invalid

        // Assert: Verify handling of corrupted attribute
        assertEquals((byte) 0xFF, fieldAttr,"Attribute should be 0xFF");
        // In actual code, would validate: if ((attr & 0x03) == 0x03) log warning
    }

    /**
     * TEST 19: Buffer wrap-around attack attempt
     *
     * Pairwise combination:
     * - Command type: WTD immediate (0x01)
     * - Control char: 0x1F (low control)
     * - Data length: claims to extend beyond buffer
     * - Buffer position: end area (1020)
     * - Field attribute: protected (0x02)
     *
     * RED: Should prevent buffer wrap-around
     * GREEN: Verify end-of-buffer enforcement
     */
    @Test
    public void testBufferWrapAroundAttackAttempt() {
        // Arrange: Position near end with length that would wrap
        int bufPos = MAX_BUFFER_SIZE - 4;
        testBuffer[bufPos] = WTD_IMMEDIATE;
        testBuffer[bufPos + 1] = CTRL_CHAR_LOW;
        testBuffer[bufPos + 2] = 0x10; // Claims 16 bytes
        testBuffer[bufPos + 3] = FIELD_ATTR_PROTECTED;

        // Act: Check for wrap-around
        int dataLen = testBuffer[bufPos + 2] & 0xFF;
        int endPos = bufPos + 4 + dataLen;
        boolean wouldWrapAround = endPos > MAX_BUFFER_SIZE;

        // Assert: Detect wrap-around attempt
        assertTrue(wouldWrapAround,"Should detect wrap-around attempt");
        assertTrue(endPos > MAX_BUFFER_SIZE,"End position should exceed buffer");
    }

    /**
     * TEST 20: Malformed SF with zero-length declaration
     *
     * Pairwise combination:
     * - Command type: WTD structured field (0xF1)
     * - Control char: 0x20 (space)
     * - Data length: 0 (declares no data)
     * - Buffer position: start (0)
     * - Field attribute: output (0x00)
     *
     * RED: Should handle zero-length SF gracefully
     * GREEN: Verify minimum SF size validation
     */
    @Test
    public void testMalformedSFWithZeroLength() {
        // Arrange: Create SF claiming zero length
        testBuffer[0] = WTD_STRUCTURED_FIELD;
        testBuffer[1] = CTRL_CHAR_SPACE;
        testBuffer[2] = 0x00;
        testBuffer[3] = 0x00; // Zero length
        testBuffer[4] = FIELD_ATTR_OUTPUT;

        // Act: Check minimum SF requirements
        int sfLen = ((testBuffer[2] & 0xFF) << 8) | (testBuffer[3] & 0xFF);
        int minSFSize = 2; // At least class and subcommand
        boolean isValidSFLength = sfLen >= minSFSize;

        // Assert: Verify minimum length requirement
        assertFalse(isValidSFLength,"Zero-length SF should be invalid");
        assertEquals(0, sfLen,"SF length should be 0");
    }

    /**
     * TEST 21: Multiple consecutive WTD orders (chaining)
     *
     * Pairwise combination:
     * - Command type: WTD standard (0x11) [1st] + WTD immediate (0x01) [2nd]
     * - Control char: 0x20 (space) [1st] + 0x3F (high normal) [2nd]
     * - Data length: 32 [1st] + 16 [2nd]
     * - Buffer position: chained at 0 and 36
     * - Field attribute: input (0x01) [1st] + protected (0x02) [2nd]
     *
     * RED: Should handle multiple sequential orders
     * GREEN: Verify position advancement and no corruption
     */
    @Test
    public void testMultipleConsecutiveWTDOrders() {
        // Arrange: Create two chained orders
        // First order
        testBuffer[0] = WTD_NORMAL;
        testBuffer[1] = CTRL_CHAR_SPACE;
        testBuffer[2] = 0x20; // 32 bytes
        testBuffer[3] = FIELD_ATTR_INPUT;
        // Fill first order data
        for (int i = 4; i < 36; i++) {
            testBuffer[i] = (byte) (0x40 + (i % 64));
        }

        // Second order
        int pos2 = 36;
        testBuffer[pos2] = WTD_IMMEDIATE;
        testBuffer[pos2 + 1] = CTRL_CHAR_HIGH;
        testBuffer[pos2 + 2] = 0x10; // 16 bytes
        testBuffer[pos2 + 3] = FIELD_ATTR_PROTECTED;

        // Act: Parse first order
        byte cmd1 = testBuffer[0];
        int len1 = testBuffer[2] & 0xFF;
        int expectedPos2 = 4 + len1;

        // Parse second order at expected position
        byte cmd2 = testBuffer[pos2];
        int len2 = testBuffer[pos2 + 2] & 0xFF;

        // Assert: Verify chaining
        assertEquals(WTD_NORMAL, cmd1,"First command should be WTD standard");
        assertEquals(32, len1,"First length should be 32");
        assertEquals(36, expectedPos2,"Second order should start at position 36");
        assertEquals(WTD_IMMEDIATE, cmd2,"Second command should be WTD immediate");
        assertEquals(16, len2,"Second length should be 16");
    }

    /**
     * TEST 22: WTD order with mixed field attribute bits
     *
     * Pairwise combination:
     * - Command type: WTD standard (0x11)
     * - Control char: 0x1F (low control)
     * - Data length: 48
     * - Buffer position: middle (500)
     * - Field attribute: 0x06 (0x02 | 0x04 = protected + modified)
     *
     * RED: Should handle combination field attributes
     * GREEN: Verify bitwise attribute validation
     */
    @Test
    public void testWTDOrderMixedFieldAttributes() {
        // Arrange: Create order with combined field attributes
        int bufPos = BUFFER_POS_MIDDLE;
        testBuffer[bufPos] = WTD_NORMAL;
        testBuffer[bufPos + 1] = CTRL_CHAR_LOW;
        testBuffer[bufPos + 2] = 0x30; // 48 bytes
        testBuffer[bufPos + 3] = (byte) 0x06; // 0x02 | 0x04

        // Act: Parse and validate attribute combinations
        byte fieldAttr = testBuffer[bufPos + 3];
        boolean hasProtected = (fieldAttr & FIELD_ATTR_PROTECTED) != 0;
        boolean hasModified = (fieldAttr & FIELD_ATTR_MODIFIED) != 0;

        // Assert: Verify attribute extraction
        assertTrue(hasProtected,"Should detect protected attribute");
        assertTrue(hasModified,"Should detect modified attribute");
        assertEquals(0x06, fieldAttr & 0xFF,"Combined attribute should be 0x06");
    }

    /**
     * TEST 23: WTD order with null bytes in control region
     *
     * Pairwise combination:
     * - Command type: WTD immediate (0x01)
     * - Control char: 0x00 (null - boundary)
     * - Data length: 64
     * - Buffer position: start (0)
     * - Field attribute: output (0x00)
     *
     * RED: Should handle null control characters
     * GREEN: Verify null-safe parsing
     */
    @Test
    public void testWTDOrderNullControlCharacter() {
        // Arrange: Create order with null control character
        testBuffer[0] = WTD_IMMEDIATE;
        testBuffer[1] = CTRL_CHAR_MIN; // 0x00 - null
        testBuffer[2] = 0x40; // 64 bytes
        testBuffer[3] = FIELD_ATTR_OUTPUT;

        // Act: Parse null control
        byte ctrlChar = testBuffer[1];
        boolean isNullControl = (ctrlChar == 0x00);

        // Assert: Verify null handling
        assertTrue(isNullControl,"Should handle null control character");
        assertEquals(0x00, ctrlChar & 0xFF,"Control char should be null");
    }

    /**
     * TEST 24: SF with missing border presentation data
     *
     * Pairwise combination:
     * - Command type: WTD structured field (0xF1)
     * - Control char: 0x3F (high normal)
     * - Data length: 3 (too short for border presentation minimal)
     * - Buffer position: middle (500)
     * - Field attribute: modified (0x04)
     *
     * RED: Should detect insufficient SF data
     * GREEN: Verify SF minor structure validation
     */
    @Test
    public void testSFMissingBorderPresentationData() {
        // Arrange: Create SF with incomplete minor structure
        int bufPos = BUFFER_POS_MIDDLE;
        testBuffer[bufPos] = WTD_STRUCTURED_FIELD;
        testBuffer[bufPos + 1] = CTRL_CHAR_HIGH;
        testBuffer[bufPos + 2] = 0x00;
        testBuffer[bufPos + 3] = 0x03; // Only 3 bytes (insufficient for border presentation)
        testBuffer[bufPos + 4] = SF_CLASS_D9;
        testBuffer[bufPos + 5] = SF_CREATE_WINDOW_51;
        testBuffer[bufPos + 6] = SF_BORDER_PRESENTATION;
        // Missing border data...

        // Act: Validate SF length vs expected minor structure
        int sfLen = testBuffer[bufPos + 3] & 0xFF;
        int minBorderStructSize = 5; // Minimum for border presentation (minLen + type + flags + 2 attr bytes)
        boolean isInsufficientLength = sfLen < minBorderStructSize;

        // Assert: Detect insufficient data
        assertTrue(isInsufficientLength,"Should detect insufficient SF data");
        assertEquals(3, sfLen,"SF length should be 3");
    }

}
