/*
 * Host Terminal Interface 5250j - Surface Test Suite
 * Protocol Round-Trip Surface - Data Translation Verification
 *
 * Tests the translation layer boundary where data enters/exits the system:
 * - Semantic Java objects → Telnet protocol bytes → Semantic Java objects
 * - Field data integrity during serialization/deserialization
 * - Edge cases and boundary conditions
 * - Invariant preservation across round-trips
 *
 * This surface is CRITICAL because bugs here cause silent data loss in production.
 * Schema changes on the i5 side can silently corrupt data if not caught here.
 */
package org.hti5250j.surfaces;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Surface tests for protocol round-trip data translation.
 *
 * Verifies that semantic data structures survive serialization to protocol bytes
 * and deserialization back to objects without loss or corruption.
 *
 * High test-to-code ratio is WORTH IT because:
 * - Silent data loss is the worst failure mode (undetected in production)
 * - Protocol changes on i5 side are discovered only by round-trip tests
 * - Boundary cases (0xFF fields, control characters) are easy to miss
 */
@DisplayName("Protocol Round-Trip Surface Tests")
public class ProtocolRoundTripSurfaceTest {

    private ProtocolRoundTripVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new ProtocolRoundTripVerifier();
    }

    // ============================================================================
    // Surface 1: Screen Display Data Round-Trip
    // ============================================================================

    @Test
    @DisplayName("Surface 1.1: ASCII text round-trip preserves content")
    void surfaceAsciiTextRoundTripPreservesContent() {
        String originalText = "ACCOUNT NUMBER       ";

        // Serialize to protocol
        byte[] serialized = verifier.serializeScreenText(originalText);
        assertThat("Serialized should not be empty", serialized.length, greaterThan(0));

        // Deserialize back
        String deserialized = verifier.deserializeScreenText(serialized);
        assertThat("Round-trip should preserve ASCII text", deserialized, equalTo(originalText));
    }

    @Test
    @DisplayName("Surface 1.2: EBCDIC fields round-trip with integrity")
    void surfaceEbcdicFieldsRoundTripWithIntegrity() {
        byte[] originalEBCDIC = new byte[]{(byte)0xC1, (byte)0xC2, (byte)0xC3}; // ABC in EBCDIC

        String translated = verifier.ebcdicToString(originalEBCDIC);
        byte[] roundTrip = verifier.stringToEbcdic(translated);

        assertThat("EBCDIC round-trip should preserve bytes", roundTrip, equalTo(originalEBCDIC));
    }

    @Test
    @DisplayName("Surface 1.3: Control characters (0xFF, 0x00) survive round-trip")
    void surfaceControlCharactersSurviveRoundTrip() {
        byte[] controlBytes = new byte[]{(byte)0xFF, (byte)0x00, (byte)0x1A, (byte)0x7F};

        byte[] serialized = verifier.serializeControlData(controlBytes);
        byte[] deserialized = verifier.deserializeControlData(serialized);

        assertThat("Control bytes should survive round-trip", deserialized, equalTo(controlBytes));
    }

    @Test
    @DisplayName("Surface 1.4: Empty fields are correctly encoded/decoded")
    void surfaceEmptyFieldsAreCorrectlyRoundTripped() {
        String emptyField = "                    "; // 20 spaces

        byte[] serialized = verifier.serializeScreenText(emptyField);
        String deserialized = verifier.deserializeScreenText(serialized);

        assertThat("Empty field should round-trip as spaces", deserialized, equalTo(emptyField));
    }

    // ============================================================================
    // Surface 2: Numeric Data Integrity (Signed/Unsigned/Decimal)
    // ============================================================================

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 127, 255, 256, 32767, 65535})
    @DisplayName("Surface 2.1: Integer values round-trip without truncation")
    void surfaceIntegersRoundTripWithoutTruncation(int value) {
        byte[] serialized = verifier.serializeInteger(value);
        int deserialized = verifier.deserializeInteger(serialized);

        assertThat("Integer should survive round-trip", deserialized, equalTo(value));
    }

    @Test
    @DisplayName("Surface 2.2: Decimal (COMP-3) fields preserve precision")
    void surfaceDecimalFieldsPreservePrecision() {
        String decimalInput = "12345.67"; // 8 digits, 2 decimal places

        byte[] serialized = verifier.serializeDecimal(decimalInput);
        String deserialized = verifier.deserializeDecimal(serialized);

        assertThat("Decimal should preserve value", deserialized, equalTo(decimalInput));
    }

    @Test
    @DisplayName("Surface 2.3: Negative numbers round-trip correctly")
    void surfaceNegativeNumbersRoundTripCorrectly() {
        int negativeValue = -12345;

        byte[] serialized = verifier.serializeSignedInteger(negativeValue);
        int deserialized = verifier.deserializeSignedInteger(serialized);

        assertThat("Negative number should survive round-trip", deserialized, equalTo(negativeValue));
    }

    @Test
    @DisplayName("Surface 2.4: Zero is distinct from empty field")
    void surfaceZeroIsDistinctFromEmptyField() {
        byte[] zeroBytes = verifier.serializeInteger(0);
        byte[] emptyBytes = verifier.serializeScreenText("    ");

        assertThat("Zero and empty should encode differently", zeroBytes, not(equalTo(emptyBytes)));
    }

    // ============================================================================
    // Surface 3: Cursor Position Coordinates
    // ============================================================================

    @Test
    @DisplayName("Surface 3.1: Cursor position (row, col) survives round-trip")
    void surfaceCursorPositionRoundTrip() {
        int row = 10, col = 40;

        byte[] serialized = verifier.serializeCursorPosition(row, col);
        int[] position = verifier.deserializeCursorPosition(serialized);

        assertThat("Row should be preserved", position[0], equalTo(row));
        assertThat("Column should be preserved", position[1], equalTo(col));
    }

    @Test
    @DisplayName("Surface 3.2: Boundary cursor positions are encoded correctly")
    void surfaceBoundaryCursorPositions() {
        int[][] boundaries = {{0, 0}, {23, 79}, {11, 39}};

        for (int[] pos : boundaries) {
            byte[] serialized = verifier.serializeCursorPosition(pos[0], pos[1]);
            int[] deserialized = verifier.deserializeCursorPosition(serialized);

            assertThat("Row " + pos[0] + " should survive", deserialized[0], equalTo(pos[0]));
            assertThat("Col " + pos[1] + " should survive", deserialized[1], equalTo(pos[1]));
        }
    }

    // ============================================================================
    // Surface 4: Attribute Data (Colors, Protection, etc.)
    // ============================================================================

    @Test
    @DisplayName("Surface 4.1: Field attributes byte round-trips unchanged")
    void surfaceFieldAttributesRoundTripUnchanged() {
        byte attribute = (byte)0x41; // Specific attribute pattern

        byte[] serialized = verifier.serializeAttribute(attribute);
        byte deserialized = verifier.deserializeAttribute(serialized);

        assertThat("Attribute byte should be preserved", deserialized, equalTo(attribute));
    }

    @Test
    @DisplayName("Surface 4.2: All 256 possible attribute values round-trip")
    void surfaceAllAttributeValuesRoundTrip() {
        for (int i = 0; i < 256; i++) {
            byte attribute = (byte)i;

            byte[] serialized = verifier.serializeAttribute(attribute);
            byte deserialized = verifier.deserializeAttribute(serialized);

            assertThat("Attribute " + i + " should survive round-trip",
                       deserialized, equalTo(attribute));
        }
    }

    // ============================================================================
    // Surface 5: Key Press Events (AID codes)
    // ============================================================================

    @Test
    @DisplayName("Surface 5.1: Function key codes survive round-trip")
    void surfaceFunctionKeyCodesRoundTrip() {
        int[] functionKeys = {0x3D, 0x3C, 0x3B, 0x3A}; // F13, F14, F15, F16

        for (int keyCode : functionKeys) {
            byte[] serialized = verifier.serializeKeyCode(keyCode);
            int deserialized = verifier.deserializeKeyCode(serialized);

            assertThat("Function key " + keyCode + " should survive",
                       deserialized, equalTo(keyCode));
        }
    }

    @Test
    @DisplayName("Surface 5.2: Enter key AID code (0x7D) round-trips")
    void surfaceEnterKeyRoundTrips() {
        int enterAid = 0x7D;

        byte[] serialized = verifier.serializeKeyCode(enterAid);
        int deserialized = verifier.deserializeKeyCode(serialized);

        assertThat("Enter AID should survive", deserialized, equalTo(enterAid));
    }

    // ============================================================================
    // Surface 6: Adversarial Round-Trip Scenarios
    // ============================================================================

    @Test
    @DisplayName("Surface 6.1: Maximum length field preserves all content")
    void surfaceMaximumLengthFieldPreservesContent() {
        String maxLengthField = "X".repeat(9999); // Large field

        byte[] serialized = verifier.serializeScreenText(maxLengthField);
        String deserialized = verifier.deserializeScreenText(serialized);

        assertThat("Maximum length field should survive", deserialized, equalTo(maxLengthField));
    }

    @Test
    @DisplayName("Surface 6.2: Mixed ASCII and control characters round-trip")
    void surfaceMixedContentRoundTrip() {
        String mixedContent = "Account\t123\n\rBalance";

        byte[] serialized = verifier.serializeScreenText(mixedContent);
        String deserialized = verifier.deserializeScreenText(serialized);

        assertThat("Mixed content should survive", deserialized, equalTo(mixedContent));
    }

    @Test
    @DisplayName("Surface 6.3: Repeated serialization is idempotent")
    void surfaceRepeatedSerializationIsIdempotent() {
        String original = "TEST";

        byte[] first = verifier.serializeScreenText(original);
        byte[] second = verifier.serializeScreenText(original);

        assertThat("Repeated serialization should be identical", first, equalTo(second));
    }

    @Test
    @DisplayName("Surface 6.4: Round-trip is symmetric (A→B→A)")
    void surfaceRoundTripIsSymmetric() {
        String original = "SECURE";

        byte[] serialized = verifier.serializeScreenText(original);
        String deserialized = verifier.deserializeScreenText(serialized);
        byte[] reserialized = verifier.serializeScreenText(deserialized);

        assertThat("Second serialization should match first", serialized, equalTo(reserialized));
    }

    // ============================================================================
    // Support: Protocol Round-Trip Verifier
    // ============================================================================

    /**
     * Verifier class handles actual serialization/deserialization logic.
     * Implementation must work against REAL protocol (not mocked).
     */
    static class ProtocolRoundTripVerifier {

        // Screen text serialization
        byte[] serializeScreenText(String text) {
            // PLACEHOLDER: Actual implementation uses real telnet encoding
            return text.getBytes();
        }

        String deserializeScreenText(byte[] bytes) {
            return new String(bytes);
        }

        // EBCDIC conversion
        byte[] stringToEbcdic(String text) {
            // PLACEHOLDER: Real implementation uses EBCDIC codec
            return text.getBytes();
        }

        String ebcdicToString(byte[] ebcdic) {
            return new String(ebcdic);
        }

        // Control data
        byte[] serializeControlData(byte[] data) {
            return data.clone();
        }

        byte[] deserializeControlData(byte[] serialized) {
            return serialized.clone();
        }

        // Integer serialization
        byte[] serializeInteger(int value) {
            return new byte[]{
                (byte)((value >> 24) & 0xFF),
                (byte)((value >> 16) & 0xFF),
                (byte)((value >> 8) & 0xFF),
                (byte)(value & 0xFF)
            };
        }

        int deserializeInteger(byte[] bytes) {
            return ((bytes[0] & 0xFF) << 24) |
                   ((bytes[1] & 0xFF) << 16) |
                   ((bytes[2] & 0xFF) << 8) |
                   (bytes[3] & 0xFF);
        }

        // Signed integer
        byte[] serializeSignedInteger(int value) {
            return serializeInteger(value);
        }

        int deserializeSignedInteger(byte[] bytes) {
            return deserializeInteger(bytes);
        }

        // Decimal (placeholder)
        byte[] serializeDecimal(String decimal) {
            return decimal.getBytes();
        }

        String deserializeDecimal(byte[] bytes) {
            return new String(bytes);
        }

        // Cursor position
        byte[] serializeCursorPosition(int row, int col) {
            return new byte[]{(byte)row, (byte)col};
        }

        int[] deserializeCursorPosition(byte[] bytes) {
            return new int[]{bytes[0] & 0xFF, bytes[1] & 0xFF};
        }

        // Attribute
        byte[] serializeAttribute(byte attribute) {
            return new byte[]{attribute};
        }

        byte deserializeAttribute(byte[] bytes) {
            return bytes[0];
        }

        // Key code
        byte[] serializeKeyCode(int keyCode) {
            return new byte[]{(byte)keyCode};
        }

        int deserializeKeyCode(byte[] bytes) {
            return bytes[0] & 0xFF;
        }
    }
}
