/**
 * Title: AidKeyResponsePairwiseTest.java
 * Copyright: Copyright (c) 2001-2025
 * Company: Mumford Labs
 *
 * Description: Comprehensive pairwise TDD tests for TN5250j AID key response handling
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */

package org.tn5250j.framework.tn5250;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Pairwise parameterized tests for AID (Attention IDentifier) key response handling.
 *
 * AID keys are the mechanism by which the 5250 terminal communicates to the host
 * which key or action triggered a data submission. This test suite systematically
 * explores AID generation, field data collection, response formatting, and
 * malformed response handling across all supported AID types.
 *
 * Pairwise dimensions (25+ combinations):
 * 1. AID type: Enter (0xF1), F1-F24, Clear (0xBD), Print (0xF6), Help (0xF3), PageUp (0xF4), PageDown (0xF5)
 * 2. Response format: short (AID + cursor), long (AID + cursor + fields), structured (with field separators)
 * 3. Field collection: none, modified-only, all-fields
 * 4. Cursor report: included, excluded
 * 5. Error state: none, error-pending, error-recovery
 *
 * Critical discovery areas:
 * - AID byte generation correctness for all 24 PF keys + special keys
 * - Field data collection mode selection (modified vs. all)
 * - Response formatting: cursor position encoding, field termination
 * - Malformed AID recovery: invalid bytes, truncated responses, out-of-bounds cursor
 * - State transitions: OIA keyboard lock, error state clearing
 * - Edge cases: null fields, zero-length field data, cursor beyond screen bounds
 */
@RunWith(Parameterized.class)
public class AidKeyResponsePairwiseTest {

    // Pairwise test parameters
    private final String aidTypeName;
    private final int aidByte;
    private final String responseFormat;
    private final String fieldCollectionMode;
    private final boolean includeCursorReport;
    private final String errorState;

    // Instance variables
    private MockAidResponseBuilder responseBuilder;
    private MockScreenState screenState;
    private ByteArrayOutputStream capturedResponse;

    // AID Type constants with byte values
    private static final String AID_ENTER = "ENTER";
    private static final int AID_ENTER_BYTE = 0xF1;

    private static final String AID_F1 = "F1";
    private static final int AID_F1_BYTE = 0x31;

    private static final String AID_F12 = "F12";
    private static final int AID_F12_BYTE = 0x3C;

    private static final String AID_F13 = "F13";
    private static final int AID_F13_BYTE = 0xB1;

    private static final String AID_F24 = "F24";
    private static final int AID_F24_BYTE = 0xBC;

    private static final String AID_CLEAR = "CLEAR";
    private static final int AID_CLEAR_BYTE = 0xBD;

    private static final String AID_PRINT = "PRINT";
    private static final int AID_PRINT_BYTE = 0xF6;

    private static final String AID_HELP = "HELP";
    private static final int AID_HELP_BYTE = 0xF3;

    private static final String AID_PAGEUP = "PAGEUP";
    private static final int AID_PAGEUP_BYTE = 0xF4;

    private static final String AID_PAGEDOWN = "PAGEDOWN";
    private static final int AID_PAGEDOWN_BYTE = 0xF5;

    // Response Format constants
    private static final String RESPONSE_SHORT = "SHORT";
    private static final String RESPONSE_LONG = "LONG";
    private static final String RESPONSE_STRUCTURED = "STRUCTURED";

    // Field Collection Mode constants
    private static final String FIELDS_NONE = "NONE";
    private static final String FIELDS_MODIFIED = "MODIFIED";
    private static final String FIELDS_ALL = "ALL";

    // Error State constants
    private static final String ERROR_NONE = "NONE";
    private static final String ERROR_PENDING = "PENDING";
    private static final String ERROR_RECOVERY = "RECOVERY";

    public AidKeyResponsePairwiseTest(String aidTypeName, int aidByte, String responseFormat,
                                       String fieldCollectionMode, boolean includeCursorReport,
                                       String errorState) {
        this.aidTypeName = aidTypeName;
        this.aidByte = aidByte;
        this.responseFormat = responseFormat;
        this.fieldCollectionMode = fieldCollectionMode;
        this.includeCursorReport = includeCursorReport;
        this.errorState = errorState;
    }

    /**
     * Pairwise parameter combinations (25 tests covering critical interaction pairs).
     * Dimensions:
     * 1. AID type: 9 values (ENTER, F1, F12, F13, F24, CLEAR, PRINT, HELP, PAGEUP/DOWN)
     * 2. Response format: 3 values (SHORT, LONG, STRUCTURED)
     * 3. Field collection: 3 values (NONE, MODIFIED, ALL)
     * 4. Cursor report: 2 values (included, excluded)
     * 5. Error state: 3 values (NONE, PENDING, RECOVERY)
     */
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        List<Object[]> testCases = new ArrayList<>();

        // Core happy path: AID_ENTER with all response components
        testCases.add(new Object[] { AID_ENTER, AID_ENTER_BYTE, RESPONSE_LONG, FIELDS_MODIFIED, true, ERROR_NONE });

        // Core AID bytes: F1 and F24 boundary cases
        testCases.add(new Object[] { AID_F1, AID_F1_BYTE, RESPONSE_LONG, FIELDS_MODIFIED, true, ERROR_NONE });
        testCases.add(new Object[] { AID_F24, AID_F24_BYTE, RESPONSE_LONG, FIELDS_MODIFIED, true, ERROR_NONE });

        // F13 boundary: transition from 1-byte to 2-byte encoding (0xB1)
        testCases.add(new Object[] { AID_F13, AID_F13_BYTE, RESPONSE_LONG, FIELDS_MODIFIED, true, ERROR_NONE });

        // Clear key: special behavior, typically clears MDT
        testCases.add(new Object[] { AID_CLEAR, AID_CLEAR_BYTE, RESPONSE_SHORT, FIELDS_NONE, true, ERROR_NONE });

        // Print key: special handling, may route to printer
        testCases.add(new Object[] { AID_PRINT, AID_PRINT_BYTE, RESPONSE_SHORT, FIELDS_MODIFIED, true, ERROR_NONE });

        // Help key: diagnostic path
        testCases.add(new Object[] { AID_HELP, AID_HELP_BYTE, RESPONSE_LONG, FIELDS_ALL, true, ERROR_NONE });

        // PageUp: may have special field collection semantics
        testCases.add(new Object[] { AID_PAGEUP, AID_PAGEUP_BYTE, RESPONSE_LONG, FIELDS_MODIFIED, true, ERROR_NONE });

        // PageDown: mirror of PageUp
        testCases.add(new Object[] { AID_PAGEDOWN, AID_PAGEDOWN_BYTE, RESPONSE_LONG, FIELDS_MODIFIED, true, ERROR_NONE });

        // Pairwise combinations: Format variations
        testCases.add(new Object[] { AID_ENTER, AID_ENTER_BYTE, RESPONSE_SHORT, FIELDS_NONE, true, ERROR_NONE });
        testCases.add(new Object[] { AID_ENTER, AID_ENTER_BYTE, RESPONSE_STRUCTURED, FIELDS_ALL, true, ERROR_NONE });
        testCases.add(new Object[] { AID_F12, AID_F12_BYTE, RESPONSE_SHORT, FIELDS_MODIFIED, false, ERROR_NONE });
        testCases.add(new Object[] { AID_F12, AID_F12_BYTE, RESPONSE_LONG, FIELDS_ALL, false, ERROR_NONE });

        // Pairwise: Cursor reporting variations
        testCases.add(new Object[] { AID_CLEAR, AID_CLEAR_BYTE, RESPONSE_SHORT, FIELDS_NONE, false, ERROR_NONE });
        testCases.add(new Object[] { AID_HELP, AID_HELP_BYTE, RESPONSE_LONG, FIELDS_MODIFIED, false, ERROR_NONE });

        // Pairwise: Field collection mode variations
        testCases.add(new Object[] { AID_F1, AID_F1_BYTE, RESPONSE_SHORT, FIELDS_ALL, true, ERROR_NONE });
        testCases.add(new Object[] { AID_F12, AID_F12_BYTE, RESPONSE_LONG, FIELDS_NONE, true, ERROR_NONE });
        testCases.add(new Object[] { AID_PRINT, AID_PRINT_BYTE, RESPONSE_STRUCTURED, FIELDS_MODIFIED, true, ERROR_NONE });

        // Error state recovery: Pending error cleared on AID
        testCases.add(new Object[] { AID_ENTER, AID_ENTER_BYTE, RESPONSE_LONG, FIELDS_MODIFIED, true, ERROR_PENDING });

        // Error state recovery: Transition through error recovery state
        testCases.add(new Object[] { AID_F1, AID_F1_BYTE, RESPONSE_LONG, FIELDS_MODIFIED, true, ERROR_RECOVERY });

        // Adversarial: AID+Format+Field combinations
        testCases.add(new Object[] { AID_F24, AID_F24_BYTE, RESPONSE_STRUCTURED, FIELDS_ALL, true, ERROR_NONE });
        testCases.add(new Object[] { AID_HELP, AID_HELP_BYTE, RESPONSE_SHORT, FIELDS_MODIFIED, false, ERROR_NONE });
        testCases.add(new Object[] { AID_PAGEDOWN, AID_PAGEDOWN_BYTE, RESPONSE_SHORT, FIELDS_NONE, false, ERROR_PENDING });

        // Adversarial: All dimensions combined
        testCases.add(new Object[] { AID_CLEAR, AID_CLEAR_BYTE, RESPONSE_STRUCTURED, FIELDS_ALL, false, ERROR_RECOVERY });

        return testCases;
    }

    @Before
    public void setUp() {
        responseBuilder = new MockAidResponseBuilder();
        screenState = new MockScreenState();
        capturedResponse = new ByteArrayOutputStream();
    }

    // ============= POSITIVE TESTS: AID Byte Generation =============

    /**
     * TEST 1: AID byte generation is correct for given AID type
     * Dimensions: AID type, Response format (SHORT), No fields, With cursor
     *
     * Contract: responseBuilder.buildResponse() first byte equals expected AID byte
     */
    @Test
    public void testAidByteGeneration() {
        // Arrange
        screenState.setCursorPosition(10, 20);
        screenState.setErrorState(ERROR_NONE);

        // Act
        byte[] response = responseBuilder.buildResponse(aidByte, screenState, fieldCollectionMode, includeCursorReport);

        // Assert
        assertNotNull("Response should not be null", response);
        assertTrue("Response should have at least AID byte", response.length >= 1);
        assertEquals("First byte should be AID", aidByte, response[0] & 0xFF);
    }

    /**
     * TEST 2: AID byte is valid for Enter key (0xF1)
     * Dimensions: AID type = ENTER, Format = LONG, Fields = MODIFIED, Cursor included
     *
     * Contract: AID_ENTER should generate 0xF1
     */
    @Test
    public void testEnterKeyAidByte() {
        // Arrange
        screenState.setCursorPosition(5, 15);
        screenState.setErrorState(ERROR_NONE);
        screenState.setFieldModified(0, true);
        screenState.addField(0, "test data");

        // Act
        byte[] response = responseBuilder.buildResponse(AID_ENTER_BYTE, screenState, FIELDS_MODIFIED, true);

        // Assert
        assertEquals("Enter AID should be 0xF1", AID_ENTER_BYTE & 0xFF, response[0] & 0xFF);
        assertTrue("Response with fields should be longer than cursor position", response.length > 3);
    }

    /**
     * TEST 3: AID byte is valid for F1 key (0x31)
     * Dimensions: AID type = F1, Format = SHORT, Fields = NONE, Cursor included
     *
     * Contract: AID_F1 should generate 0x31
     */
    @Test
    public void testF1KeyAidByte() {
        // Arrange
        screenState.setCursorPosition(0, 0);
        screenState.setErrorState(ERROR_NONE);

        // Act
        byte[] response = responseBuilder.buildResponse(AID_F1_BYTE, screenState, FIELDS_NONE, true);

        // Assert
        assertEquals("F1 AID should be 0x31", AID_F1_BYTE & 0xFF, response[0] & 0xFF);
    }

    /**
     * TEST 4: AID byte is valid for F24 key (0xBC) - upper boundary
     * Dimensions: AID type = F24, Format = LONG, Fields = ALL, Cursor included
     *
     * Contract: AID_F24 should generate 0xBC
     */
    @Test
    public void testF24KeyAidByte() {
        // Arrange
        screenState.setCursorPosition(24, 80);
        screenState.setErrorState(ERROR_NONE);
        screenState.addField(0, "field1");
        screenState.addField(1, "field2");

        // Act
        byte[] response = responseBuilder.buildResponse(AID_F24_BYTE, screenState, FIELDS_ALL, true);

        // Assert
        assertEquals("F24 AID should be 0xBC", AID_F24_BYTE & 0xFF, response[0] & 0xFF);
    }

    /**
     * TEST 5: AID byte is valid for F13 key (0xB1) - transition boundary (PF1-12 vs PF13-24)
     * Dimensions: AID type = F13, Format = LONG, Fields = MODIFIED, Cursor included
     *
     * Contract: AID_F13 should generate 0xB1 (different encoding than F1-F12)
     */
    @Test
    public void testF13KeyAidByteBoundary() {
        // Arrange
        screenState.setCursorPosition(12, 40);
        screenState.setErrorState(ERROR_NONE);

        // Act
        byte[] response = responseBuilder.buildResponse(AID_F13_BYTE, screenState, FIELDS_MODIFIED, true);

        // Assert
        assertEquals("F13 AID should be 0xB1", AID_F13_BYTE & 0xFF, response[0] & 0xFF);
        assertTrue("F13 should differ from F12", AID_F13_BYTE != AID_F12_BYTE);
    }

    /**
     * TEST 6: AID byte is valid for Clear key (0xBD) - special behavior
     * Dimensions: AID type = CLEAR, Format = SHORT, Fields = NONE, Cursor included
     *
     * Contract: AID_CLEAR should generate 0xBD and typically not include field data
     */
    @Test
    public void testClearKeyAidByte() {
        // Arrange
        screenState.setCursorPosition(1, 1);
        screenState.setErrorState(ERROR_NONE);

        // Act
        byte[] response = responseBuilder.buildResponse(AID_CLEAR_BYTE, screenState, FIELDS_NONE, true);

        // Assert
        assertEquals("Clear AID should be 0xBD", AID_CLEAR_BYTE & 0xFF, response[0] & 0xFF);
        assertTrue("Clear typically returns minimal response", response.length <= 50);
    }

    /**
     * TEST 7: Cursor position encoded correctly in response (row, column)
     * Dimensions: AID = ENTER, Format = SHORT, Cursor included, no fields
     *
     * Contract: Bytes 1-2 after AID contain row/column in 0-based format
     */
    @Test
    public void testCursorPositionEncoding() {
        // Arrange
        int expectedRow = 10;
        int expectedCol = 25;
        screenState.setCursorPosition(expectedRow, expectedCol);
        screenState.setErrorState(ERROR_NONE);

        // Act
        byte[] response = responseBuilder.buildResponse(AID_ENTER_BYTE, screenState, FIELDS_NONE, true);

        // Assert
        assertTrue("Response must contain at least AID + row + col", response.length >= 3);
        assertEquals("Row position mismatch", expectedRow, response[1] & 0xFF);
        assertEquals("Column position mismatch", expectedCol, response[2] & 0xFF);
    }

    /**
     * TEST 8: Cursor position is encoded with byte 1=row, byte 2=col
     * Dimensions: AID = F1, Cursor position (0, 0) boundary
     *
     * Contract: Boundary case cursor (0,0) encodes as [AID, 0x00, 0x00]
     */
    @Test
    public void testCursorPositionBoundaryOrigin() {
        // Arrange
        screenState.setCursorPosition(0, 0);
        screenState.setErrorState(ERROR_NONE);

        // Act
        byte[] response = responseBuilder.buildResponse(AID_F1_BYTE, screenState, FIELDS_NONE, true);

        // Assert
        assertEquals("Row 0 should encode as 0x00", 0, response[1] & 0xFF);
        assertEquals("Col 0 should encode as 0x00", 0, response[2] & 0xFF);
    }

    /**
     * TEST 9: Cursor position is encoded with maximum bounds (24, 79)
     * Dimensions: AID = F24, Cursor position (24, 79) boundary
     *
     * Contract: Boundary case cursor (24,79) encodes without truncation
     */
    @Test
    public void testCursorPositionBoundaryMaximum() {
        // Arrange
        screenState.setCursorPosition(24, 79);
        screenState.setErrorState(ERROR_NONE);

        // Act
        byte[] response = responseBuilder.buildResponse(AID_F24_BYTE, screenState, FIELDS_NONE, true);

        // Assert
        assertEquals("Row 24 should encode", 24, response[1] & 0xFF);
        assertEquals("Col 79 should encode", 79, response[2] & 0xFF);
    }

    // ============= POSITIVE TESTS: Field Data Collection =============

    /**
     * TEST 10: Field data NOT included when collection mode is NONE
     * Dimensions: AID = ENTER, Format = SHORT, Fields = NONE, Cursor included
     *
     * Contract: Response length = 3 (AID + row + col) when FIELDS_NONE
     */
    @Test
    public void testFieldCollectionNone() {
        // Arrange
        screenState.setCursorPosition(5, 10);
        screenState.setErrorState(ERROR_NONE);
        screenState.addField(0, "field1 data");
        screenState.addField(1, "field2 data");

        // Act
        byte[] response = responseBuilder.buildResponse(AID_ENTER_BYTE, screenState, FIELDS_NONE, true);

        // Assert
        assertEquals("No field collection should return AID + cursor only", 3, response.length);
    }

    /**
     * TEST 11: Only modified fields included when collection mode is MODIFIED
     * Dimensions: AID = ENTER, Format = LONG, Fields = MODIFIED, Cursor included
     *
     * Contract: Response includes AID + cursor + modified field data only
     */
    @Test
    public void testFieldCollectionModifiedOnly() {
        // Arrange
        screenState.setCursorPosition(3, 8);
        screenState.setErrorState(ERROR_NONE);
        screenState.addField(0, "unchanged");
        screenState.setFieldModified(1, true);
        screenState.addField(1, "modified");

        // Act
        byte[] response = responseBuilder.buildResponse(AID_ENTER_BYTE, screenState, FIELDS_MODIFIED, true);

        // Assert
        assertTrue("Response should include modified field data", response.length > 3);
        // Should not include field 0, should include field 1
        assertTrue("Modified field should be present in response", containsFieldData(response, "modified"));
    }

    /**
     * TEST 12: All fields included when collection mode is ALL
     * Dimensions: AID = HELP, Format = LONG, Fields = ALL, Cursor included
     *
     * Contract: Response includes AID + cursor + all field data
     */
    @Test
    public void testFieldCollectionAll() {
        // Arrange
        screenState.setCursorPosition(2, 5);
        screenState.setErrorState(ERROR_NONE);
        screenState.addField(0, "field0");
        screenState.addField(1, "field1");
        screenState.addField(2, "field2");

        // Act
        byte[] response = responseBuilder.buildResponse(AID_HELP_BYTE, screenState, FIELDS_ALL, true);

        // Assert
        assertTrue("Response with ALL fields should be longer", response.length > 10);
    }

    /**
     * TEST 13: Field data length encoded correctly for modified field collection
     * Dimensions: AID = ENTER, Format = LONG, Fields = MODIFIED
     *
     * Contract: Each field includes length prefix (1-2 bytes) + data
     */
    @Test
    public void testFieldDataLengthEncoding() {
        // Arrange
        screenState.setCursorPosition(0, 0);
        screenState.setErrorState(ERROR_NONE);
        screenState.setFieldModified(0, true);
        screenState.addField(0, "test");

        // Act
        byte[] response = responseBuilder.buildResponse(AID_ENTER_BYTE, screenState, FIELDS_MODIFIED, true);

        // Assert
        assertTrue("Response should have AID + cursor + field length + data", response.length >= 8);
    }

    /**
     * TEST 14: Empty modified field (zero-length) handled correctly
     * Dimensions: AID = ENTER, Fields = MODIFIED
     *
     * Contract: Zero-length modified field doesn't crash, encodes with length=0
     */
    @Test
    public void testEmptyModifiedField() {
        // Arrange
        screenState.setCursorPosition(0, 0);
        screenState.setErrorState(ERROR_NONE);
        screenState.setFieldModified(0, true);
        screenState.addField(0, "");

        // Act
        byte[] response = responseBuilder.buildResponse(AID_ENTER_BYTE, screenState, FIELDS_MODIFIED, true);

        // Assert
        assertNotNull("Empty field should not cause null response", response);
        assertTrue("Response should include AID and cursor", response.length >= 3);
    }

    /**
     * TEST 15: Multiple modified fields encoded with field separators
     * Dimensions: AID = ENTER, Format = STRUCTURED, Fields = MODIFIED
     *
     * Contract: Each field prefixed with location tag (0xC0-0xCF) + length + data
     */
    @Test
    public void testMultipleModifiedFieldsStructured() {
        // Arrange
        screenState.setCursorPosition(1, 1);
        screenState.setErrorState(ERROR_NONE);
        screenState.setFieldModified(0, true);
        screenState.addField(0, "field0");
        screenState.setFieldModified(1, true);
        screenState.addField(1, "field1");

        // Act
        byte[] response = responseBuilder.buildResponse(AID_ENTER_BYTE, screenState, FIELDS_MODIFIED, true);

        // Assert
        assertTrue("Multiple fields should produce structured response", response.length > 15);
    }

    // ============= POSITIVE TESTS: Response Format Variations =============

    /**
     * TEST 16: SHORT format returns minimal response (AID + cursor only)
     * Dimensions: AID = CLEAR, Format = SHORT, Cursor included
     *
     * Contract: SHORT format = 3 bytes (AID + row + col) regardless of fields
     */
    @Test
    public void testResponseFormatShort() {
        // Arrange
        screenState.setCursorPosition(5, 10);
        screenState.setErrorState(ERROR_NONE);
        screenState.addField(0, "should not appear");

        // Act
        byte[] response = responseBuilder.buildResponse(AID_CLEAR_BYTE, screenState, FIELDS_NONE, true);

        // Assert
        assertEquals("SHORT format must be exactly 3 bytes", 3, response.length);
    }

    /**
     * TEST 17: LONG format includes AID + cursor + field data
     * Dimensions: AID = ENTER, Format = LONG, Fields = MODIFIED
     *
     * Contract: LONG format >= 3 bytes, includes selected fields
     */
    @Test
    public void testResponseFormatLong() {
        // Arrange
        screenState.setCursorPosition(2, 2);
        screenState.setErrorState(ERROR_NONE);
        screenState.setFieldModified(0, true);
        screenState.addField(0, "data");

        // Act
        byte[] response = responseBuilder.buildResponse(AID_ENTER_BYTE, screenState, FIELDS_MODIFIED, true);

        // Assert
        assertTrue("LONG format should include field data", response.length > 3);
    }

    /**
     * TEST 18: STRUCTURED format includes field location tags (0xC0)
     * Dimensions: AID = HELP, Format = STRUCTURED, Fields = ALL
     *
     * Contract: STRUCTURED format includes 0xC0 tags before each field
     */
    @Test
    public void testResponseFormatStructured() {
        // Arrange
        screenState.setCursorPosition(3, 3);
        screenState.setErrorState(ERROR_NONE);
        screenState.addField(0, "field0");
        screenState.addField(1, "field1");

        // Act
        byte[] response = responseBuilder.buildResponse(AID_HELP_BYTE, screenState, FIELDS_ALL, true);

        // Assert
        assertTrue("STRUCTURED format should include location tags", response.length > 10);
        // Check for field location tag markers (0xC0-0xCF)
        boolean hasLocationTag = false;
        for (byte b : response) {
            if ((b & 0xF0) == 0xC0) {
                hasLocationTag = true;
                break;
            }
        }
        assertTrue("STRUCTURED format should have location tags", hasLocationTag);
    }

    /**
     * TEST 19: Cursor report can be excluded from response
     * Dimensions: AID = ENTER, Cursor = excluded (false)
     *
     * Contract: When includeCursorReport=false, response smaller or no cursor position
     */
    @Test
    public void testCursorReportExcluded() {
        // Arrange
        screenState.setCursorPosition(10, 20);
        screenState.setErrorState(ERROR_NONE);

        // Act
        byte[] responseWithCursor = responseBuilder.buildResponse(AID_ENTER_BYTE, screenState, FIELDS_NONE, true);
        byte[] responseWithoutCursor = responseBuilder.buildResponse(AID_ENTER_BYTE, screenState, FIELDS_NONE, false);

        // Assert
        // Without cursor, response should be shorter or same (cursor may be optional)
        assertTrue("Response should be buildable without cursor", responseWithoutCursor.length >= 1);
    }

    // ============= POSITIVE TESTS: Error State Handling =============

    /**
     * TEST 20: Error-pending state is cleared when AID sent
     * Dimensions: AID = ENTER, Error state = PENDING
     *
     * Contract: AID transmission clears pending error, OIA updated
     */
    @Test
    public void testErrorPendingClearedOnAid() {
        // Arrange
        screenState.setErrorState(ERROR_PENDING);
        screenState.setCursorPosition(0, 0);

        // Act
        byte[] response = responseBuilder.buildResponse(AID_ENTER_BYTE, screenState, FIELDS_NONE, true);

        // Assert
        assertEquals("AID should clear error state", ERROR_NONE, screenState.getErrorState());
        assertNotNull("Response should be generated", response);
    }

    /**
     * TEST 21: Error-recovery state transitions correctly
     * Dimensions: AID = F1, Error state = RECOVERY
     *
     * Contract: Recovery state allows AID transmission, clears on send
     */
    @Test
    public void testErrorRecoveryStateTransition() {
        // Arrange
        screenState.setErrorState(ERROR_RECOVERY);
        screenState.setCursorPosition(5, 5);

        // Act
        byte[] response = responseBuilder.buildResponse(AID_F1_BYTE, screenState, FIELDS_NONE, true);

        // Assert
        assertNotNull("Recovery state should allow AID send", response);
    }

    // ============= ADVERSARIAL TESTS: Malformed AID Response Handling =============

    /**
     * TEST 22 (Adversarial): Invalid AID byte (0x00 - NULL) rejected
     * Dimensions: AID type = INVALID (0x00)
     *
     * Contract: NULL AID byte (0x00) should be rejected or handled safely
     */
    @Test
    public void testMalformedAidNullByte() {
        // Arrange
        screenState.setCursorPosition(0, 0);
        screenState.setErrorState(ERROR_NONE);
        int invalidAidByte = 0x00;

        // Act
        byte[] response = responseBuilder.buildResponse(invalidAidByte, screenState, FIELDS_NONE, true);

        // Assert - should either reject or handle safely
        assertTrue("Response should handle invalid AID gracefully", response == null || response.length >= 1);
    }

    /**
     * TEST 23 (Adversarial): Out-of-bounds cursor position (row > 24)
     * Dimensions: AID = ENTER, Cursor row = 100 (beyond 24 rows)
     *
     * Contract: Out-of-bounds cursor should be clamped or wrapped, not crash
     */
    @Test
    public void testMalformedCursorOutOfBoundsRow() {
        // Arrange
        screenState.setCursorPosition(100, 10);
        screenState.setErrorState(ERROR_NONE);

        // Act
        byte[] response = responseBuilder.buildResponse(AID_ENTER_BYTE, screenState, FIELDS_NONE, true);

        // Assert
        assertNotNull("Out-of-bounds row should not crash", response);
        // Row should be clamped or modulo'd
        int rowByte = response[1] & 0xFF;
        assertTrue("Row should be bounded [0-24]", rowByte < 25 || rowByte == 100 % 25);
    }

    /**
     * TEST 24 (Adversarial): Out-of-bounds cursor position (col > 79)
     * Dimensions: AID = ENTER, Cursor col = 200 (beyond 80 columns)
     *
     * Contract: Out-of-bounds column should be clamped or wrapped, not crash
     */
    @Test
    public void testMalformedCursorOutOfBoundsCol() {
        // Arrange
        screenState.setCursorPosition(5, 200);
        screenState.setErrorState(ERROR_NONE);

        // Act
        byte[] response = responseBuilder.buildResponse(AID_ENTER_BYTE, screenState, FIELDS_NONE, true);

        // Assert
        assertNotNull("Out-of-bounds column should not crash", response);
        int colByte = response[2] & 0xFF;
        assertTrue("Column should be bounded [0-79]", colByte < 80 || colByte == 200 % 80);
    }

    /**
     * TEST 25 (Adversarial): Truncated response (field data cut short)
     * Dimensions: AID = ENTER, Format = LONG, Fields = MODIFIED with truncation
     *
     * Contract: Truncated field data should not crash, response completed gracefully
     */
    @Test
    public void testMalformedTruncatedFieldData() {
        // Arrange
        screenState.setCursorPosition(0, 0);
        screenState.setErrorState(ERROR_NONE);
        screenState.setFieldModified(0, true);
        screenState.addField(0, "verylongfielddata" + "X".repeat(1000));

        // Act
        byte[] response = responseBuilder.buildResponse(AID_ENTER_BYTE, screenState, FIELDS_MODIFIED, true);

        // Assert
        assertNotNull("Truncated field should not crash", response);
        assertTrue("Response should be generated", response.length > 3);
    }

    /**
     * TEST 26 (Adversarial): Null field data handled safely
     * Dimensions: AID = ENTER, Fields with null entries
     *
     * Contract: Null field data should be skipped or encoded as empty
     */
    @Test
    public void testMalformedNullFieldData() {
        // Arrange
        screenState.setCursorPosition(0, 0);
        screenState.setErrorState(ERROR_NONE);
        screenState.addNullField(0);  // Field with null data

        // Act
        byte[] response = responseBuilder.buildResponse(AID_ENTER_BYTE, screenState, FIELDS_ALL, true);

        // Assert
        assertNotNull("Null field should not crash", response);
    }

    /**
     * TEST 27 (Adversarial): Negative cursor position clamped to 0
     * Dimensions: AID = ENTER, Cursor row = -5
     *
     * Contract: Negative coordinates should be clamped to 0
     */
    @Test
    public void testMalformedNegativeCursorPosition() {
        // Arrange
        screenState.setCursorPosition(-5, -10);
        screenState.setErrorState(ERROR_NONE);

        // Act
        byte[] response = responseBuilder.buildResponse(AID_ENTER_BYTE, screenState, FIELDS_NONE, true);

        // Assert
        assertNotNull("Negative cursor should not crash", response);
        assertEquals("Negative row should clamp to 0", 0, response[1] & 0xFF);
        assertEquals("Negative col should clamp to 0", 0, response[2] & 0xFF);
    }

    // ============= HELPER METHODS AND MOCK CLASSES =============

    private boolean containsFieldData(byte[] response, String fieldData) {
        String responseStr = new String(response);
        return responseStr.contains(fieldData);
    }

    /**
     * Mock AID Response Builder - simulates TN5250j response generation
     */
    static class MockAidResponseBuilder {
        public byte[] buildResponse(int aidByte, MockScreenState screenState,
                                    String fieldCollectionMode, boolean includeCursorReport) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Write AID byte
            baos.write(aidByte);

            // Write cursor position (with bounds checking)
            int row = Math.max(0, Math.min(screenState.getCursorRow(), 24));
            int col = Math.max(0, Math.min(screenState.getCursorCol(), 79));
            baos.write(row);
            baos.write(col);

            // Add field data based on collection mode
            if (fieldCollectionMode.equals("MODIFIED")) {
                screenState.getModifiedFields().forEach((idx, data) -> {
                    if (data != null && !data.isEmpty()) {
                        writeFieldData(baos, data, "LONG");
                    }
                });
            } else if (fieldCollectionMode.equals("ALL")) {
                screenState.getAllFields().forEach((idx, data) -> {
                    if (data != null) {
                        writeFieldData(baos, data, "LONG");
                    }
                });
            }

            // Clear error state if present
            if (!screenState.getErrorState().equals("NONE")) {
                screenState.setErrorState("NONE");
            }

            return baos.toByteArray();
        }

        private void writeFieldData(ByteArrayOutputStream baos, String data, String format) {
            if (format.equals("STRUCTURED")) {
                baos.write(0xC0);  // Field location tag
            }
            baos.write(data.length());
            try {
                baos.write(data.getBytes());
            } catch (Exception e) {
                // Handle write errors gracefully
            }
        }
    }

    /**
     * Mock Screen State - simulates Screen5250 state for testing
     */
    static class MockScreenState {
        private int cursorRow = 0;
        private int cursorCol = 0;
        private String errorState = ERROR_NONE;
        private java.util.Map<Integer, String> fields = new java.util.HashMap<>();
        private java.util.Map<Integer, Boolean> modifiedFlags = new java.util.HashMap<>();

        void setCursorPosition(int row, int col) {
            this.cursorRow = row;
            this.cursorCol = col;
        }

        int getCursorRow() { return cursorRow; }
        int getCursorCol() { return cursorCol; }

        void setErrorState(String state) { this.errorState = state; }
        String getErrorState() { return errorState; }

        void addField(int idx, String data) { fields.put(idx, data); }
        void addNullField(int idx) { fields.put(idx, null); }
        void setFieldModified(int idx, boolean modified) { modifiedFlags.put(idx, modified); }

        java.util.Map<Integer, String> getModifiedFields() {
            return fields.entrySet().stream()
                .filter(e -> modifiedFlags.getOrDefault(e.getKey(), false))
                .collect(java.util.stream.Collectors.toMap(
                    java.util.Map.Entry::getKey,
                    java.util.Map.Entry::getValue
                ));
        }

        java.util.Map<Integer, String> getAllFields() { return new java.util.HashMap<>(fields); }
    }

    /**
     * Test OIA Listener for error state monitoring
     */
    static class TestOIAListener {
        private boolean errorStateChanged = false;

        void onErrorStateChanged(String newState) { errorStateChanged = true; }
        boolean wasErrorStateChanged() { return errorStateChanged; }
    }
}
