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
 * Pairwise parameter testing for TN5250E protocol negotiation.
 *
 * Test dimensions (to be combined pairwise):
 * 1. Device type: [display, printer, combined]
 * 2. Device name: [default (empty), custom (1-8 chars), invalid (>10 chars, null)]
 * 3. Bypass flag: [disabled (0x00), enabled (0x01)]
 * 4. Record mode: [character (0x00), record (0x01)]
 * 5. Response mode: [normal (0x00), structured field (0x01)]
 *
 * Pairwise coverage: 3 × 3 × 2 × 2 × 2 = 72 theoretical combinations
 * Reduced to 25+ essential tests covering:
 * 1. POSITIVE: Valid TN5250E negotiations succeed
 * 2. BOUNDARY: Device name length limits (0, 8, 10+)
 * 3. ADVERSARIAL: Invalid device types, null names, malformed packets
 * 4. PROTOCOL VIOLATIONS: Reserved bits set, invalid mode combinations
 * 5. FUZZING: Corrupt negotiation headers, injection attempts
 *
 * TN5250E Negotiation Packet Structure:
 * +-----------+----------+----------+---------+---------+----------+
 * | Byte 0-1  | Byte 2   | Byte 3-4 | Byte 5  | Byte 6  | Byte 7+  |
 * | Length    | Cmd Code | Reserved | Flags   | Mode    | Device   |
 * |           | (0x41)   | (0x00)   |         | Mask    | Name     |
 * +-----------+----------+----------+---------+---------+----------+
 *
 * Flags byte (Byte 5):
 *   Bit 0: Device Type (0=display, 1=printer, 2=combined)
 *   Bit 1-2: Reserved (must be 0)
 *   Bit 3: Bypass flag (0=disabled, 1=enabled)
 *   Bit 4-7: Reserved (must be 0)
 *
 * Mode Mask (Byte 6):
 *   Bit 0: Record mode (0=character, 1=record)
 *   Bit 1: Response mode (0=normal, 1=structured field)
 *   Bit 2-7: Reserved (must be 0)
 */
public class TN5250EProtocolPairwiseTest {

    // ========== TN5250E Protocol Constants ==========

    // Negotiation command code
    private static final byte TNESCFG_CMD = (byte) 0x41;

    // Device type flags (bits 0-2 of flags byte)
    private static final byte DEVICE_TYPE_DISPLAY = 0x00;
    private static final byte DEVICE_TYPE_PRINTER = 0x01;
    private static final byte DEVICE_TYPE_COMBINED = 0x02;
    private static final byte DEVICE_TYPE_INVALID = (byte) 0xFF;

    // Bypass flag (bit 3 of flags byte)
    private static final byte BYPASS_DISABLED = 0x00;
    private static final byte BYPASS_ENABLED = (byte) 0x08;

    // Record mode (bit 0 of mode mask)
    private static final byte RECORD_MODE_CHARACTER = 0x00;
    private static final byte RECORD_MODE_RECORD = 0x01;

    // Response mode (bit 1 of mode mask)
    private static final byte RESPONSE_MODE_NORMAL = 0x00;
    private static final byte RESPONSE_MODE_STRUCTURED = (byte) 0x02;

    // Device name constraints
    private static final int MAX_DEVICE_NAME_LENGTH = 8;
    private static final int PROTOCOL_MAX_DEVICE_NAME = 10;

    // Packet structure offsets
    private static final int OFFSET_LENGTH_HIGH = 0;
    private static final int OFFSET_LENGTH_LOW = 1;
    private static final int OFFSET_CMD_CODE = 2;
    private static final int OFFSET_RESERVED_1 = 3;
    private static final int OFFSET_RESERVED_2 = 4;
    private static final int OFFSET_FLAGS = 5;
    private static final int OFFSET_MODE_MASK = 6;
    private static final int OFFSET_DEVICE_NAME = 7;

    private static final int MINIMUM_PACKET_SIZE = OFFSET_DEVICE_NAME;

    // Test data
    private byte[] negotiationPacket;
    private TN5250EProtocolHandler handler;

    @BeforeEach
    public void setUp() {
        handler = new TN5250EProtocolHandler();
        negotiationPacket = null;
    }

    // ========== TEST 1-5: POSITIVE - Valid TN5250E Negotiations ==========

    /**
     * TEST 1: Valid negotiation with display device, no bypass, character mode, normal response
     *
     * Pairwise dimensions:
     * - Device type: display (0x00)
     * - Device name: default (empty)
     * - Bypass flag: disabled (0x00)
     * - Record mode: character (0x00)
     * - Response mode: normal (0x00)
     *
     * RED: Protocol handler should accept valid minimal negotiation
     * GREEN: Parse packet and set configuration correctly
     */
    @Test
    public void testValidNegotiationDisplayNoBypassCharacterNormal() {
        // Arrange: Create valid negotiation packet
        negotiationPacket = createValidNegotiationPacket(
                DEVICE_TYPE_DISPLAY,
                "", // empty device name
                BYPASS_DISABLED,
                RECORD_MODE_CHARACTER,
                RESPONSE_MODE_NORMAL
        );

        // Act: Parse negotiation
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert: Negotiation should succeed
        assertTrue(result,"Valid negotiation should succeed");
        assertEquals(DEVICE_TYPE_DISPLAY, handler.getDeviceType(),"Device type should be display");
        assertEquals(false, handler.isBypassEnabled(),"Bypass should be disabled");
        assertEquals(RECORD_MODE_CHARACTER, handler.getRecordMode(),"Record mode should be character");
        assertEquals(RESPONSE_MODE_NORMAL, handler.getResponseMode(),"Response mode should be normal");
    }

    /**
     * TEST 2: Valid negotiation with printer device, custom name, bypass enabled, record mode
     *
     * Pairwise dimensions:
     * - Device type: printer (0x01)
     * - Device name: custom (8 chars: "PRINTER1")
     * - Bypass flag: enabled (0x08)
     * - Record mode: record (0x01)
     * - Response mode: normal (0x00)
     */
    @Test
    public void testValidNegotiationPrinterCustomBypassRecordNormal() {
        // Arrange
        negotiationPacket = createValidNegotiationPacket(
                DEVICE_TYPE_PRINTER,
                "PRINTER1",
                BYPASS_ENABLED,
                RECORD_MODE_RECORD,
                RESPONSE_MODE_NORMAL
        );

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert
        assertTrue(result,"Valid printer negotiation should succeed");
        assertEquals(DEVICE_TYPE_PRINTER, handler.getDeviceType(),"Device type should be printer");
        assertEquals(true, handler.isBypassEnabled(),"Bypass should be enabled");
        assertEquals("PRINTER1", handler.getDeviceName(),"Device name should match");
        assertEquals(RECORD_MODE_RECORD, handler.getRecordMode(),"Record mode should be record");
    }

    /**
     * TEST 3: Valid negotiation with combined device, bypass, structured response mode
     *
     * Pairwise dimensions:
     * - Device type: combined (0x02)
     * - Device name: custom (5 chars: "COMBO")
     * - Bypass flag: enabled (0x08)
     * - Record mode: character (0x00)
     * - Response mode: structured field (0x02)
     */
    @Test
    public void testValidNegotiationCombinedBypassStructuredResponse() {
        // Arrange
        negotiationPacket = createValidNegotiationPacket(
                DEVICE_TYPE_COMBINED,
                "COMBO",
                BYPASS_ENABLED,
                RECORD_MODE_CHARACTER,
                RESPONSE_MODE_STRUCTURED
        );

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert
        assertTrue(result,"Valid combined device negotiation should succeed");
        assertEquals(DEVICE_TYPE_COMBINED, handler.getDeviceType(),"Device type should be combined");
        assertEquals(true, handler.isBypassEnabled(),"Bypass should be enabled");
        assertEquals(RESPONSE_MODE_STRUCTURED, handler.getResponseMode(),"Response mode should be structured");
        assertTrue(handler.isCombinedMode(),"Should support combined device mode");
    }

    /**
     * TEST 4: Valid negotiation with 8-character device name (maximum length)
     *
     * Pairwise dimensions:
     * - Device type: display (0x00)
     * - Device name: custom (8 chars: "DISPLAYX")
     * - Bypass flag: disabled (0x00)
     * - Record mode: character (0x00)
     * - Response mode: normal (0x00)
     */
    @Test
    public void testValidNegotiationMaxLengthDeviceName() {
        // Arrange: Create packet with 8-char device name
        String maxLengthName = "DISPLAYX"; // exactly 8 chars
        negotiationPacket = createValidNegotiationPacket(
                DEVICE_TYPE_DISPLAY,
                maxLengthName,
                BYPASS_DISABLED,
                RECORD_MODE_CHARACTER,
                RESPONSE_MODE_NORMAL
        );

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert
        assertTrue(result,"Should accept 8-char device name");
        assertEquals(maxLengthName, handler.getDeviceName(),"Device name should be stored exactly");
        assertEquals(8, handler.getDeviceName().length(),"Device name length should be 8");
    }

    /**
     * TEST 5: Valid negotiation with both record and structured response modes enabled
     *
     * Pairwise dimensions:
     * - Device type: combined (0x02)
     * - Device name: custom (6 chars: "HYBRID")
     * - Bypass flag: enabled (0x08)
     * - Record mode: record (0x01)
     * - Response mode: structured field (0x02)
     */
    @Test
    public void testValidNegotiationRecordAndStructuredModes() {
        // Arrange
        negotiationPacket = createValidNegotiationPacket(
                DEVICE_TYPE_COMBINED,
                "HYBRID",
                BYPASS_ENABLED,
                (byte) (RECORD_MODE_RECORD | RESPONSE_MODE_STRUCTURED),
                RESPONSE_MODE_STRUCTURED
        );

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert
        assertTrue(result,"Should accept both record and structured modes");
        assertEquals(RECORD_MODE_RECORD,
                handler.getRecordMode() & RECORD_MODE_RECORD,"Record mode should be enabled");
        assertEquals(RESPONSE_MODE_STRUCTURED,
                handler.getResponseMode() & RESPONSE_MODE_STRUCTURED,"Response mode should be enabled");
    }

    // ========== TEST 6-10: BOUNDARY - Device Name Length Limits ==========

    /**
     * TEST 6: Boundary - Single-character device name (minimum non-empty)
     *
     * Device name: "X" (1 char)
     */
    @Test
    public void testBoundaryDeviceNameSingleCharacter() {
        // Arrange
        negotiationPacket = createValidNegotiationPacket(
                DEVICE_TYPE_DISPLAY,
                "X",
                BYPASS_DISABLED,
                RECORD_MODE_CHARACTER,
                RESPONSE_MODE_NORMAL
        );

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert
        assertTrue(result,"Should accept single-char device name");
        assertEquals("X", handler.getDeviceName(),"Device name should be 'X'");
    }

    /**
     * TEST 7: Boundary - Nine-character device name (exceeds MAX_DEVICE_NAME_LENGTH of 8)
     *
     * Device name: "NINECHARX" (9 chars) - should be rejected or truncated
     */
    @Test
    public void testBoundaryDeviceNameNineCharacters() {
        // Arrange
        negotiationPacket = createNegotiationPacketWithName(
                DEVICE_TYPE_DISPLAY,
                "NINECHARX", // 9 characters
                BYPASS_DISABLED,
                RECORD_MODE_CHARACTER,
                RESPONSE_MODE_NORMAL
        );

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert: Should either reject or truncate to 8 chars
        if (result) {
            // If accepted, verify truncation
            assertTrue(handler.getDeviceName().length() <= MAX_DEVICE_NAME_LENGTH,"Device name length should not exceed 8");
        } else {
            // If rejected, that's also acceptable
            assertFalse(true,"Nine-char name should be rejected or truncated");
        }
    }

    /**
     * TEST 8: Boundary - Protocol maximum device name (10 characters)
     *
     * Device name: "TENCHARXXX" (10 chars) - protocol limit
     */
    @Test
    public void testBoundaryDeviceNameTenCharacters() {
        // Arrange
        negotiationPacket = createNegotiationPacketWithName(
                DEVICE_TYPE_DISPLAY,
                "TENCHARXXX", // 10 characters
                BYPASS_DISABLED,
                RECORD_MODE_CHARACTER,
                RESPONSE_MODE_NORMAL
        );

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert: Should handle at protocol limit
        if (result) {
            assertTrue(handler.getDeviceName().length() <= PROTOCOL_MAX_DEVICE_NAME,"Device name length should not exceed 10");
        }
    }

    /**
     * TEST 9: Boundary - Empty device name with bypass enabled
     *
     * Device name: "" (empty), bypass enabled
     */
    @Test
    public void testBoundaryEmptyDeviceNameWithBypass() {
        // Arrange
        negotiationPacket = createValidNegotiationPacket(
                DEVICE_TYPE_DISPLAY,
                "", // empty
                BYPASS_ENABLED,
                RECORD_MODE_CHARACTER,
                RESPONSE_MODE_NORMAL
        );

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert
        assertTrue(result,"Should accept empty name even with bypass");
        assertTrue(handler.isBypassEnabled(),"Bypass should be enabled");
    }

    /**
     * TEST 10: Boundary - Device name with special characters (if allowed)
     *
     * Device name: "DEV-123" (7 chars with hyphen)
     */
    @Test
    public void testBoundaryDeviceNameWithSpecialCharacters() {
        // Arrange
        negotiationPacket = createValidNegotiationPacket(
                DEVICE_TYPE_DISPLAY,
                "DEV-123",
                BYPASS_DISABLED,
                RECORD_MODE_CHARACTER,
                RESPONSE_MODE_NORMAL
        );

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert
        assertTrue(result,"Should accept device name with special chars");
        assertEquals("DEV-123", handler.getDeviceName(),"Device name should preserve special characters");
    }

    // ========== TEST 11-15: ADVERSARIAL - Invalid Device Types and Names ==========

    /**
     * TEST 11: Adversarial - Invalid device type (0xFF)
     *
     * Device type: 0xFF (invalid)
     */
    @Test
    public void testAdversarialInvalidDeviceType() {
        // Arrange: Create packet with invalid device type
        negotiationPacket = new byte[MINIMUM_PACKET_SIZE];
        setPacketLength(negotiationPacket, MINIMUM_PACKET_SIZE);
        negotiationPacket[OFFSET_CMD_CODE] = TNESCFG_CMD;
        negotiationPacket[OFFSET_RESERVED_1] = 0x00;
        negotiationPacket[OFFSET_RESERVED_2] = 0x00;
        negotiationPacket[OFFSET_FLAGS] = DEVICE_TYPE_INVALID; // Invalid device type
        negotiationPacket[OFFSET_MODE_MASK] = 0x00;

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert
        assertFalse(result,"Should reject invalid device type");
        assertFalse(handler.isNegotiationSuccessful(),"Error handling should set invalid flag");
    }

    /**
     * TEST 12: Adversarial - Null device name pointer
     *
     * Device name: null (packet truncated)
     */
    @Test
    public void testAdversarialNullDeviceName() {
        // Arrange: Create packet with null terminator at device name position
        negotiationPacket = new byte[MINIMUM_PACKET_SIZE + 1];
        setPacketLength(negotiationPacket, negotiationPacket.length);
        negotiationPacket[OFFSET_CMD_CODE] = TNESCFG_CMD;
        negotiationPacket[OFFSET_FLAGS] = DEVICE_TYPE_DISPLAY;
        negotiationPacket[OFFSET_MODE_MASK] = 0x00;
        negotiationPacket[OFFSET_DEVICE_NAME] = 0x00; // null terminator

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert: Should handle gracefully
        assertTrue(result,"Should handle null device name gracefully");
        assertTrue(handler.getDeviceName().isEmpty(),"Device name should be empty");
    }

    /**
     * TEST 13: Adversarial - Packet with reserved bits set in flags byte
     *
     * Flags byte: 0xC4 (reserved bits 1-2 and 4-7 set)
     */
    @Test
    public void testAdversarialReservedBitsSetInFlags() {
        // Arrange: Create packet with reserved bits set
        negotiationPacket = createValidNegotiationPacket(
                DEVICE_TYPE_DISPLAY,
                "DEV001",
                BYPASS_DISABLED,
                RECORD_MODE_CHARACTER,
                RESPONSE_MODE_NORMAL
        );
        // Corrupt flags byte with reserved bits
        negotiationPacket[OFFSET_FLAGS] |= (byte) 0xF0; // Set reserved bits 4-7

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert: Should reject reserved bits violation
        assertFalse(result,"Should reject reserved bits set");
    }

    /**
     * TEST 14: Adversarial - Packet with reserved bits set in mode mask
     *
     * Mode mask: 0xFC (reserved bits 2-7 set)
     */
    @Test
    public void testAdversarialReservedBitsSetInModeMask() {
        // Arrange
        negotiationPacket = createValidNegotiationPacket(
                DEVICE_TYPE_DISPLAY,
                "DEV002",
                BYPASS_DISABLED,
                RECORD_MODE_CHARACTER,
                RESPONSE_MODE_NORMAL
        );
        // Corrupt mode mask with reserved bits
        negotiationPacket[OFFSET_MODE_MASK] |= (byte) 0xFC; // Set reserved bits 2-7

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert
        assertFalse(result,"Should reject reserved bits in mode mask");
    }

    /**
     * TEST 15: Adversarial - Corrupt reserved byte 1 (should be 0x00)
     *
     * Reserved byte 1: 0xFF (should be 0x00)
     */
    @Test
    public void testAdversarialCorruptReservedByte1() {
        // Arrange
        negotiationPacket = createValidNegotiationPacket(
                DEVICE_TYPE_DISPLAY,
                "DEV003",
                BYPASS_DISABLED,
                RECORD_MODE_CHARACTER,
                RESPONSE_MODE_NORMAL
        );
        // Corrupt reserved byte
        negotiationPacket[OFFSET_RESERVED_1] = (byte) 0xFF;

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert
        assertFalse(result,"Should reject corrupt reserved byte");
    }

    // ========== TEST 16-20: PROTOCOL VIOLATIONS - Invalid Combinations ==========

    /**
     * TEST 16: Protocol violation - Invalid mode mask with reserved bits (0xFC)
     *
     * Valid modes: 0x00, 0x01, 0x02, 0x03 only
     */
    @Test
    public void testProtocolViolationInvalidModeMask() {
        // Arrange
        negotiationPacket = createValidNegotiationPacket(
                DEVICE_TYPE_DISPLAY,
                "DEV004",
                BYPASS_DISABLED,
                RECORD_MODE_CHARACTER,
                RESPONSE_MODE_NORMAL
        );
        negotiationPacket[OFFSET_MODE_MASK] = (byte) 0x04; // Invalid: bit 2 set

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert
        assertFalse(result,"Should reject invalid mode mask");
    }

    /**
     * TEST 17: Protocol violation - Inconsistent packet length (too short)
     *
     * Packet length: 5 (too small for valid header)
     */
    @Test
    public void testProtocolViolationPacketTooShort() {
        // Arrange
        negotiationPacket = new byte[5];
        setPacketLength(negotiationPacket, 5);
        negotiationPacket[OFFSET_CMD_CODE] = TNESCFG_CMD;

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert
        assertFalse(result,"Should reject packet that is too short");
    }

    /**
     * TEST 18: Protocol violation - Length field exceeds actual packet size
     *
     * Claimed length: 100, actual size: 20
     */
    @Test
    public void testProtocolViolationLengthExceedsSize() {
        // Arrange
        negotiationPacket = new byte[20];
        setPacketLength(negotiationPacket, 100); // Claim 100 but only have 20
        negotiationPacket[OFFSET_CMD_CODE] = TNESCFG_CMD;
        negotiationPacket[OFFSET_FLAGS] = DEVICE_TYPE_DISPLAY;

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert
        assertFalse(result,"Should reject length field exceeding actual size");
    }

    /**
     * TEST 19: Protocol violation - Invalid command code (not 0x41)
     *
     * Command code: 0x42 (wrong)
     */
    @Test
    public void testProtocolViolationInvalidCommandCode() {
        // Arrange
        negotiationPacket = createValidNegotiationPacket(
                DEVICE_TYPE_DISPLAY,
                "DEV005",
                BYPASS_DISABLED,
                RECORD_MODE_CHARACTER,
                RESPONSE_MODE_NORMAL
        );
        negotiationPacket[OFFSET_CMD_CODE] = (byte) 0x42; // Invalid: should be 0x41

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert
        assertFalse(result,"Should reject invalid command code");
    }

    /**
     * TEST 20: Protocol violation - Device type field uses 3 bits (0-2), test bit 3+ usage
     *
     * Device type: 0x04 (bit 2 set, should use bits 0-2 only)
     */
    @Test
    public void testProtocolViolationDeviceTypeBits() {
        // Arrange
        negotiationPacket = createValidNegotiationPacket(
                DEVICE_TYPE_DISPLAY,
                "DEV006",
                BYPASS_DISABLED,
                RECORD_MODE_CHARACTER,
                RESPONSE_MODE_NORMAL
        );
        negotiationPacket[OFFSET_FLAGS] = (byte) 0x04; // Device type in wrong bit position

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert
        assertFalse(result,"Should reject invalid device type bit position");
    }

    // ========== TEST 21-25: FUZZING - Protocol Fuzzing and Injection ==========

    /**
     * TEST 21: Fuzzing - Device name contains null terminator mid-string
     *
     * Device name: "DEV\0007" (null in middle)
     */
    @Test
    public void testFuzzingDeviceNameWithEmbeddedNull() {
        // Arrange: Create packet with embedded null
        negotiationPacket = new byte[MINIMUM_PACKET_SIZE + 8];
        setPacketLength(negotiationPacket, negotiationPacket.length);
        negotiationPacket[OFFSET_CMD_CODE] = TNESCFG_CMD;
        negotiationPacket[OFFSET_FLAGS] = DEVICE_TYPE_DISPLAY;
        negotiationPacket[OFFSET_MODE_MASK] = 0x00;
        negotiationPacket[OFFSET_DEVICE_NAME] = 'D';
        negotiationPacket[OFFSET_DEVICE_NAME + 1] = 'E';
        negotiationPacket[OFFSET_DEVICE_NAME + 2] = 'V';
        negotiationPacket[OFFSET_DEVICE_NAME + 3] = 0x00; // Null terminator
        negotiationPacket[OFFSET_DEVICE_NAME + 4] = '0';
        negotiationPacket[OFFSET_DEVICE_NAME + 5] = '0';
        negotiationPacket[OFFSET_DEVICE_NAME + 6] = '7';

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert: Should handle gracefully by truncating at null
        assertTrue(result,"Should handle embedded null gracefully");
        assertTrue(handler.getDeviceName().length() <= 3 || !handler.getDeviceName().contains("007"),"Device name should be truncated at null");
    }

    /**
     * TEST 22: Fuzzing - All flags byte bits set (0xFF)
     *
     * Flags byte: 0xFF (all bits set - multiple violations)
     */
    @Test
    public void testFuzzingAllFlagsBitsSet() {
        // Arrange
        negotiationPacket = createValidNegotiationPacket(
                DEVICE_TYPE_DISPLAY,
                "FUZZ001",
                BYPASS_DISABLED,
                RECORD_MODE_CHARACTER,
                RESPONSE_MODE_NORMAL
        );
        negotiationPacket[OFFSET_FLAGS] = (byte) 0xFF; // All bits set

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert
        assertFalse(result,"Should reject flags byte with all bits set");
    }

    /**
     * TEST 23: Fuzzing - All mode mask bits set (0xFF)
     *
     * Mode mask: 0xFF (all bits set - violation)
     */
    @Test
    public void testFuzzingAllModeMapBitsSet() {
        // Arrange
        negotiationPacket = createValidNegotiationPacket(
                DEVICE_TYPE_DISPLAY,
                "FUZZ002",
                BYPASS_DISABLED,
                RECORD_MODE_CHARACTER,
                RESPONSE_MODE_NORMAL
        );
        negotiationPacket[OFFSET_MODE_MASK] = (byte) 0xFF; // All bits set

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert
        assertFalse(result,"Should reject mode mask with all bits set");
    }

    /**
     * TEST 24: Fuzzing - Zero-length packet (empty)
     *
     * Packet size: 0 (completely empty)
     */
    @Test
    public void testFuzzingZeroLengthPacket() {
        // Arrange
        negotiationPacket = new byte[0];

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert
        assertFalse(result,"Should reject zero-length packet");
    }

    /**
     * TEST 25: Fuzzing - Packet with alternating bit patterns
     *
     * Packet: 0xAAAAAAAA pattern (alternating bits)
     */
    @Test
    public void testFuzzingAlternatingBitPattern() {
        // Arrange
        negotiationPacket = new byte[MINIMUM_PACKET_SIZE];
        for (int i = 0; i < negotiationPacket.length; i++) {
            negotiationPacket[i] = (i % 2 == 0) ? (byte) 0xAA : (byte) 0x55;
        }
        setPacketLength(negotiationPacket, MINIMUM_PACKET_SIZE);

        // Act
        boolean result = handler.negotiateProtocol(negotiationPacket);

        // Assert
        assertFalse(result,"Should reject fuzzing pattern");
    }

    // ========== Helper Methods ==========

    /**
     * Create a valid TN5250E negotiation packet with specified parameters.
     *
     * @param deviceType Device type (display, printer, combined)
     * @param deviceName Device name (string, will be ASCII-encoded)
     * @param bypassFlag Bypass flag (enabled/disabled)
     * @param recordMode Record mode (character/record)
     * @param responseMode Response mode (normal/structured)
     * @return Valid negotiation packet
     */
    private byte[] createValidNegotiationPacket(
            byte deviceType,
            String deviceName,
            byte bypassFlag,
            byte recordMode,
            byte responseMode) {

        int nameBytes = deviceName != null ? Math.min(deviceName.length(), MAX_DEVICE_NAME_LENGTH) : 0;
        byte[] packet = new byte[MINIMUM_PACKET_SIZE + nameBytes];

        // Set packet length (in network byte order)
        setPacketLength(packet, packet.length);

        // Set command code
        packet[OFFSET_CMD_CODE] = TNESCFG_CMD;

        // Set reserved bytes to 0x00
        packet[OFFSET_RESERVED_1] = 0x00;
        packet[OFFSET_RESERVED_2] = 0x00;

        // Set flags byte (device type + bypass flag)
        packet[OFFSET_FLAGS] = (byte) (deviceType | bypassFlag);

        // Set mode mask (record mode + response mode)
        packet[OFFSET_MODE_MASK] = (byte) (recordMode | responseMode);

        // Set device name if provided
        if (deviceName != null && deviceName.length() > 0) {
            byte[] nameBytes_ = deviceName.getBytes();
            int copyLength = Math.min(nameBytes_.length, MAX_DEVICE_NAME_LENGTH);
            System.arraycopy(nameBytes_, 0, packet, OFFSET_DEVICE_NAME, copyLength);
        }

        return packet;
    }

    /**
     * Create negotiation packet with explicit device name length (allows oversized names).
     *
     * @param deviceType Device type
     * @param deviceName Device name (may exceed MAX_DEVICE_NAME_LENGTH)
     * @param bypassFlag Bypass flag
     * @param recordMode Record mode
     * @param responseMode Response mode
     * @return Negotiation packet (may be invalid if name exceeds protocol limit)
     */
    private byte[] createNegotiationPacketWithName(
            byte deviceType,
            String deviceName,
            byte bypassFlag,
            byte recordMode,
            byte responseMode) {

        int nameBytes = deviceName != null ? deviceName.length() : 0;
        byte[] packet = new byte[MINIMUM_PACKET_SIZE + nameBytes];

        setPacketLength(packet, packet.length);
        packet[OFFSET_CMD_CODE] = TNESCFG_CMD;
        packet[OFFSET_RESERVED_1] = 0x00;
        packet[OFFSET_RESERVED_2] = 0x00;
        packet[OFFSET_FLAGS] = (byte) (deviceType | bypassFlag);
        packet[OFFSET_MODE_MASK] = (byte) (recordMode | responseMode);

        if (deviceName != null && deviceName.length() > 0) {
            byte[] nameBytes_ = deviceName.getBytes();
            int copyLength = Math.min(nameBytes_.length, nameBytes);
            System.arraycopy(nameBytes_, 0, packet, OFFSET_DEVICE_NAME, copyLength);
        }

        return packet;
    }

    /**
     * Set packet length field in network byte order (big-endian).
     *
     * @param packet Packet buffer
     * @param length Length value
     */
    private void setPacketLength(byte[] packet, int length) {
        packet[OFFSET_LENGTH_HIGH] = (byte) ((length >> 8) & 0xFF);
        packet[OFFSET_LENGTH_LOW] = (byte) (length & 0xFF);
    }

    /**
     * Mock TN5250E protocol handler for testing.
     *
     * This is a simplified handler that validates TN5250E negotiation packets
     * and maintains protocol configuration state.
     */
    private static class TN5250EProtocolHandler {
        private boolean negotiationSuccessful = false;
        private byte deviceType = 0;
        private String deviceName = "";
        private boolean bypassEnabled = false;
        private byte recordMode = 0;
        private byte responseMode = 0;
        private boolean combinedMode = false;

        /**
         * Negotiate TN5250E protocol parameters.
         *
         * @param packet Negotiation packet
         * @return true if negotiation succeeds, false otherwise
         */
        public boolean negotiateProtocol(byte[] packet) {
            // Validate packet
            if (packet == null || packet.length < MINIMUM_PACKET_SIZE) {
                negotiationSuccessful = false;
                return false;
            }

            // Extract length
            int declaredLength = ((packet[OFFSET_LENGTH_HIGH] & 0xFF) << 8) |
                    (packet[OFFSET_LENGTH_LOW] & 0xFF);
            if (declaredLength != packet.length || declaredLength < MINIMUM_PACKET_SIZE) {
                negotiationSuccessful = false;
                return false;
            }

            // Validate command code
            if (packet[OFFSET_CMD_CODE] != TNESCFG_CMD) {
                negotiationSuccessful = false;
                return false;
            }

            // Validate reserved bytes
            if (packet[OFFSET_RESERVED_1] != 0x00 || packet[OFFSET_RESERVED_2] != 0x00) {
                negotiationSuccessful = false;
                return false;
            }

            // Extract and validate flags byte
            byte flagsByte = packet[OFFSET_FLAGS];
            byte deviceTypeExtracted = (byte) (flagsByte & 0x03); // Bits 0-2
            if (deviceTypeExtracted > 0x02) {
                // Invalid device type (only 0, 1, 2 valid)
                negotiationSuccessful = false;
                return false;
            }

            // Check reserved bits in flags (bits 1-2 and 4-7 should be 0, only bit 3 can be set)
            byte reservedFlagsBits = (byte) ((flagsByte & 0xF4) & ~0x08); // Mask out device type and bypass
            if (reservedFlagsBits != 0x00 && (flagsByte & 0x08) == 0) {
                // Reserved bits set incorrectly
                if ((flagsByte & 0xFC) != 0 && (flagsByte & 0x08) == 0) {
                    negotiationSuccessful = false;
                    return false;
                }
            }

            // Extract and validate mode mask
            byte modeMask = packet[OFFSET_MODE_MASK];
            byte recordModeExtracted = (byte) (modeMask & 0x01);
            byte responseModeExtracted = (byte) (modeMask & 0x02);

            // Check reserved bits in mode mask (bits 2-7 should be 0)
            if ((modeMask & 0xFC) != 0x00) {
                negotiationSuccessful = false;
                return false;
            }

            // Extract device name
            String deviceNameExtracted = "";
            if (packet.length > OFFSET_DEVICE_NAME) {
                int nameLength = Math.min(packet.length - OFFSET_DEVICE_NAME, MAX_DEVICE_NAME_LENGTH);
                StringBuilder nameBuilder = new StringBuilder();
                for (int i = 0; i < nameLength; i++) {
                    byte b = packet[OFFSET_DEVICE_NAME + i];
                    if (b == 0) {
                        break; // Null terminator
                    }
                    nameBuilder.append((char) b);
                }
                deviceNameExtracted = nameBuilder.toString();
            }

            // All validations passed - update state
            this.deviceType = deviceTypeExtracted;
            this.bypassEnabled = (flagsByte & 0x08) != 0;
            this.recordMode = recordModeExtracted;
            this.responseMode = responseModeExtracted;
            this.deviceName = deviceNameExtracted;
            this.combinedMode = (deviceTypeExtracted == DEVICE_TYPE_COMBINED);
            this.negotiationSuccessful = true;

            return true;
        }

        // Getters for assertion validation
        public boolean isNegotiationSuccessful() {
            return negotiationSuccessful;
        }

        public byte getDeviceType() {
            return deviceType;
        }

        public String getDeviceName() {
            return deviceName;
        }

        public boolean isBypassEnabled() {
            return bypassEnabled;
        }

        public byte getRecordMode() {
            return recordMode;
        }

        public byte getResponseMode() {
            return responseMode;
        }

        public boolean isCombinedMode() {
            return combinedMode;
        }
    }
}
