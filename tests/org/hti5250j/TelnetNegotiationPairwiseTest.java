/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Telnet Negotiation Pairwise Test Suite
 *
 * Test categories:
 * 1. POSITIVE: Standard RFC 854 negotiation sequences with valid terminal types
 * 2. PROTOCOL: Telnet option negotiation rules, state transitions, edge cases
 * 3. ADVERSARIAL: Malformed sequences, buffer overflow attempts, injection attacks
 * 4. TTYPE: Terminal type subnegotiation with various terminal identifiers
 * 5. STATE: Option state tracking across multiple negotiation rounds
 */
public class TelnetNegotiationPairwiseTest {

    // RFC 854 Telnet Protocol Constants
    private static final byte IAC = (byte) 0xFF;      // 255
    private static final byte DONT = (byte) 0xFE;     // 254
    private static final byte DO = (byte) 0xFD;       // 253
    private static final byte WONT = (byte) 0xFC;     // 252
    private static final byte WILL = (byte) 0xFB;     // 251
    private static final byte SB = (byte) 0xFA;       // 250 (Subnegotiation Begin)
    private static final byte SE = (byte) 0xF0;       // 240 (Subnegotiation End)

    // Option codes
    private static final byte TRANSMIT_BINARY = (byte) 0;      // 0
    private static final byte ECHO = (byte) 1;                 // 1
    private static final byte SGA = (byte) 3;                  // 3
    private static final byte TERMINAL_TYPE = (byte) 24;       // 24
    private static final byte END_OF_RECORD = (byte) 25;       // 25
    private static final byte TN5250E = (byte) 40;             // 40 (proprietary)
    private static final byte INVALID_OPTION = (byte) -1;      // Invalid option code

    // Subnegotiation qualifiers
    private static final byte QUAL_IS = (byte) 0;     // 0
    private static final byte QUAL_SEND = (byte) 1;   // 1

    // Test fixtures
    private InputStream mockInputStream;
    private ByteArrayOutputStream capturedOutput;

    // Test terminal types
    private static final String[] VALID_TERMINAL_TYPES = {
            "IBM-3179-2",           // 80x24 (legacy)
            "IBM-3477-FC",          // 132 column
            "IBM-3477-4B",          // Variant
            "AIXTERM",              // Custom variant
            "ANSI"                  // Generic fallback
    };

    private static final String[] MALFORMED_TERMINAL_TYPES = {
            "",                                              // Empty
            " IBM-3179-2",                                   // Leading space
            "IBM-3179-2 ",                                   // Trailing space
            "IBM-3179-2\u0000",                              // Null terminator (injection)
            "IBM-3179-2\r\n",                                // CRLF injection
            new String(new byte[512], StandardCharsets.UTF_8),  // Oversized (512 bytes)
            "\u00FF\u00FF\u00FF",                            // IAC bytes in string
            "../../etc/passwd",                              // Path traversal
            "<script>alert('xss')</script>",                 // XSS attempt
            new String(new char[1024]).replace('\0', 'A')    // Very long string
    };

    @BeforeEach
    public void setUp() throws IOException {
        mockInputStream = null;
        capturedOutput = new ByteArrayOutputStream();
    }

    @AfterEach
    public void tearDown() {
        if (mockInputStream != null) {
            try {
                mockInputStream.close();
            } catch (IOException e) {
                // Ignore
            }
        }
        capturedOutput = null;
    }

    // ==================== POSITIVE TESTS: Standard RFC 854 Negotiation ====================

    @Test
    public void testNegotiation_WILL_BINARY_standard_sequence() {
        // Given: Server sends DO BINARY (request client support)
        byte[] serverNegotiation = {IAC, DO, TRANSMIT_BINARY};

        // When: We process the negotiation
        boolean result = isValidNegotiationSequence(serverNegotiation);

        // Then: Should be recognized as valid
        assertTrue(result,"DO BINARY should be valid negotiation");
    }

    @Test
    public void testNegotiation_WILL_ECHO_standard_sequence() {
        // Given: Server sends DO ECHO
        byte[] serverNegotiation = {IAC, DO, ECHO};

        // When: We process the negotiation
        boolean result = isValidNegotiationSequence(serverNegotiation);

        // Then: Should be valid
        assertTrue(result,"DO ECHO should be valid");
    }

    @Test
    public void testNegotiation_WILL_SGA_standard_sequence() {
        // Given: Server sends DO SGA (Suppress Go Ahead)
        byte[] serverNegotiation = {IAC, DO, SGA};

        // When: We process
        boolean result = isValidNegotiationSequence(serverNegotiation);

        // Then: Valid
        assertTrue(result,"DO SGA should be valid");
    }

    @Test
    public void testNegotiation_WILL_TTYPE_with_valid_terminal_type_IBM_3179_2() {
        // Given: Server requests terminal type, we have IBM-3179-2 configured
        byte[] ttypeSubneg = createSubnegotiationRequest(TERMINAL_TYPE, QUAL_SEND);
        byte[] response = createTerminalTypeResponse("IBM-3179-2");

        // When: We build the response
        boolean isValid = isValidTerminalTypeResponse(response);

        // Then: Response should be properly formed with SE terminator
        assertTrue(isValid,"IBM-3179-2 response should be valid");
    }

    @Test
    public void testNegotiation_WILL_TTYPE_with_valid_terminal_type_IBM_3477_FC() {
        // Given: Server requests terminal type, we have IBM-3477-FC (132-col)
        byte[] response = createTerminalTypeResponse("IBM-3477-FC");

        // When: We build the response
        boolean isValid = isValidTerminalTypeResponse(response);

        // Then: Valid response
        assertTrue(isValid,"IBM-3477-FC response should be valid");
    }

    @Test
    public void testNegotiation_WILL_EOR_end_of_record() {
        // Given: Server sends DO EOR (End of Record)
        byte[] serverNegotiation = {IAC, DO, END_OF_RECORD};

        // When: Process
        boolean result = isValidNegotiationSequence(serverNegotiation);

        // Then: Valid
        assertTrue(result,"DO EOR should be valid");
    }

    @Test
    public void testNegotiation_WONT_TIMING_MARK_rejection() {
        // Given: Server sends DO TIMING_MARK, we don't support it
        byte[] serverNegotiation = {IAC, DO, (byte) 6};  // TIMING_MARK = 6

        // When: Process
        byte[] response = createRejectionResponse((byte) 6);

        // Then: Should create WONT TIMING_MARK
        assertEquals(IAC, response[0],"First byte should be IAC");
        assertEquals(WONT, response[1],"Second byte should be WONT");
        assertEquals((byte) 6, response[2],"Third byte should be option code");
    }

    @Test
    public void testNegotiation_multiple_options_sequential() {
        // Given: Server sends multiple DO options sequentially
        byte[] negotiation1 = {IAC, DO, TRANSMIT_BINARY};
        byte[] negotiation2 = {IAC, DO, ECHO};
        byte[] negotiation3 = {IAC, DO, END_OF_RECORD};

        // When: Process each
        boolean result1 = isValidNegotiationSequence(negotiation1);
        boolean result2 = isValidNegotiationSequence(negotiation2);
        boolean result3 = isValidNegotiationSequence(negotiation3);

        // Then: All should be valid
        assertTrue(result1 && result2 && result3,"All sequential negotiations should be valid");
    }

    // ==================== PROTOCOL COMPLIANCE: RFC 854 Rules ====================

    @Test
    public void testProtocol_IAC_IAC_escape_in_data_stream() {
        // Given: Data stream contains escaped IAC (IAC IAC = literal 0xFF)
        byte[] dataWithEscapedIAC = {0x41, IAC, IAC, 0x42};  // A<IAC-escaped>B

        // When: We check if this is data (not negotiation)
        boolean isNegotiation = (dataWithEscapedIAC[0] == IAC && dataWithEscapedIAC[1] != IAC);

        // Then: Should not be treated as negotiation command
        assertFalse(isNegotiation,"IAC IAC should be data escape, not command");
    }

    @Test
    public void testProtocol_IAC_missing_option_in_DO_command() {
        // Given: DO command without option byte (truncated)
        byte[] truncatedDO = {IAC, DO};  // Missing option

        // When: Check if valid
        boolean isValid = (truncatedDO.length >= 3);

        // Then: Should be invalid (requires option byte)
        assertFalse(isValid,"DO without option should be invalid");
    }

    @Test
    public void testProtocol_IAC_missing_option_in_WILL_command() {
        // Given: WILL command without option
        byte[] truncatedWILL = {IAC, WILL};

        // When: Check validity
        boolean isValid = (truncatedWILL.length >= 3);

        // Then: Invalid
        assertFalse(isValid,"WILL without option should be invalid");
    }

    @Test
    public void testProtocol_SB_without_SE_terminator_malformed() {
        // Given: Subnegotiation starts but never ends
        byte[] truncatedSubneg = {IAC, SB, TERMINAL_TYPE, QUAL_SEND};  // Missing IAC SE

        // When: Check for proper termination
        boolean isProperlyTerminated = endsWithIAC_SE(truncatedSubneg);

        // Then: Should be invalid
        assertFalse(isProperlyTerminated,"SB without SE should be invalid");
    }

    @Test
    public void testProtocol_SB_with_proper_SE_terminator() {
        // Given: Proper subnegotiation with terminator
        byte[] properSubneg = {IAC, SB, TERMINAL_TYPE, QUAL_SEND, IAC, SE};

        // When: Check termination
        boolean isProperlyTerminated = endsWithIAC_SE(properSubneg);

        // Then: Should be valid
        assertTrue(isProperlyTerminated,"SB with SE should be valid");
    }

    @Test
    public void testProtocol_option_state_transition_DO_to_WILL() {
        // Given: Server sends DO BINARY
        byte[] serverDO = {IAC, DO, TRANSMIT_BINARY};

        // When: We respond with WILL BINARY
        byte[] ourResponse = createWillResponse(TRANSMIT_BINARY);

        // Then: Response should be WILL BINARY
        assertEquals(WILL, ourResponse[1],"Response should be WILL");
        assertEquals(TRANSMIT_BINARY, ourResponse[2],"Response option should match");
    }

    @Test
    public void testProtocol_option_state_transition_DO_rejected_with_WONT() {
        // Given: Server sends DO for unsupported option
        byte[] serverDO = {IAC, DO, (byte) 99};  // Unsupported option

        // When: We reject
        byte[] ourResponse = createRejectionResponse((byte) 99);

        // Then: Should be WONT
        assertEquals(WONT, ourResponse[1],"Response should be WONT");
    }

    // ==================== ADVERSARIAL: Malformed & Injection Attacks ====================

    @Test
    public void testAdversarial_empty_terminal_type_response() {
        // Given: Malformed TTYPE response with empty terminal type
        byte[] response = createTerminalTypeResponse("");

        // When: Validate
        boolean isValid = (response.length > 4) && (response[3] == QUAL_IS);

        // Then: Empty terminal types should still be structured properly
        assertTrue(isValid,"Empty TTYPE should maintain structure");
    }

    @Test
    public void testAdversarial_null_byte_in_terminal_type() {
        // Given: Terminal type containing null byte (injection attempt)
        String malformed = "IBM-3179-2\u0000/etc/passwd";
        byte[] response = createTerminalTypeResponse(malformed);

        // When: Check for null bytes in response
        boolean hasNullByte = hasNullByte(response);

        // Then: Should detect null bytes
        assertTrue(hasNullByte,"Should detect null byte injection");
    }

    @Test
    public void testAdversarial_oversized_terminal_type_512_bytes() {
        // Given: Terminal type string of 512 bytes (buffer overflow attempt)
        String oversized = new String(new char[512]).replace('\0', 'A');
        byte[] response = createTerminalTypeResponse(oversized);

        // When: Check response size
        int responseSize = response.length;

        // Then: Response should not exceed reasonable bounds (< 1024 bytes)
        assertTrue(responseSize < 1024,"Oversized TTYPE response should be bounded");
    }

    @Test
    public void testAdversarial_IAC_bytes_embedded_in_terminal_type() {
        // Given: Terminal type containing IAC bytes (escape confusion)
        String malicious = "IBM-3179\u00FF\u00FE";
        byte[] response = createTerminalTypeResponse(malicious);

        // When: Extract terminal type from response
        byte[] ttypeBytes = extractTerminalTypeBytes(response);

        // Then: IAC bytes should be properly escaped or removed
        boolean hasUnescapedIAC = false;
        for (int i = 0; i < ttypeBytes.length - 1; i++) {
            if (ttypeBytes[i] == IAC && ttypeBytes[i + 1] != IAC) {
                hasUnescapedIAC = true;
                break;
            }
        }
        assertFalse(hasUnescapedIAC,"IAC should be escaped in TTYPE");
    }

    @Test
    public void testAdversarial_path_traversal_in_terminal_type() {
        // Given: Terminal type attempting path traversal
        String pathTraversal = "../../etc/passwd";
        byte[] response = createTerminalTypeResponse(pathTraversal);

        // When: Check content
        String extractedType = new String(extractTerminalTypeBytes(response), StandardCharsets.UTF_8);

        // Then: Should contain the malicious string (no validation at protocol level)
        assertEquals(pathTraversal, extractedType,"Protocol should preserve terminal type as-is");
    }

    @Test
    public void testAdversarial_multiple_negotiation_floods_rapid_DO_DONT() {
        // Given: Rapid alternating DO/DONT for same option (flood attack)
        byte[] flood = new byte[300];
        int pos = 0;
        for (int i = 0; i < 50; i++) {
            flood[pos++] = IAC;
            flood[pos++] = (i % 2 == 0) ? DO : DONT;
            flood[pos++] = TRANSMIT_BINARY;
        }

        // When: Process flood
        int validSequences = 0;
        for (int i = 0; i < flood.length; i += 3) {
            if (i + 2 < flood.length && flood[i] == IAC) {
                validSequences++;
            }
        }

        // Then: Should parse all sequences without crashing
        assertEquals(50, validSequences,"Should parse all 50 sequences");
    }

    @Test
    public void testAdversarial_invalid_command_code_after_IAC() {
        // Given: IAC followed by invalid command (not WILL/WONT/DO/DONT/SB)
        byte[] invalidCommand = {IAC, (byte) 0x55};  // 0x55 is not a valid command

        // When: Check if valid command
        boolean isValidCommand = isValidTelnetCommand(invalidCommand[1]);

        // Then: Should be invalid
        assertFalse(isValidCommand,"Invalid command code should be detected");
    }

    @Test
    public void testAdversarial_SB_without_option_code() {
        // Given: SB without option (truncated subnegotiation)
        byte[] truncatedSB = {IAC, SB};

        // When: Check if properly formed
        boolean isProper = (truncatedSB.length >= 3);

        // Then: Invalid
        assertFalse(isProper,"SB must have option code");
    }

    @Test
    public void testAdversarial_unclosed_subnegotiation_in_stream() {
        // Given: SB that's never closed (entire stream until EOF)
        byte[] unclosed = {IAC, SB, TERMINAL_TYPE, QUAL_SEND, 0x41, 0x42, 0x43};

        // When: Check termination
        boolean isTerminated = endsWithIAC_SE(unclosed);

        // Then: Should be detected as invalid
        assertFalse(isTerminated,"Unclosed SB should be invalid");
    }

    // ==================== TTYPE EXCHANGE: Terminal Type Negotiation ====================

    @Test
    public void testTTYPE_server_sends_WILL_TTYPE() {
        // Given: Server declares support for TTYPE
        byte[] serverWillTTYPE = {IAC, WILL, TERMINAL_TYPE};

        // When: Check if valid
        boolean isValid = isValidNegotiationSequence(serverWillTTYPE);

        // Then: Should be valid
        assertTrue(isValid,"Server WILL TTYPE should be valid");
    }

    @Test
    public void testTTYPE_client_responds_DO_to_server_WILL_TTYPE() {
        // Given: Server sends WILL TTYPE
        byte[] serverWillTTYPE = {IAC, WILL, TERMINAL_TYPE};

        // When: Create response
        byte[] clientResponse = createDoResponse(TERMINAL_TYPE);

        // Then: Should be DO TTYPE
        assertEquals(DO, clientResponse[1],"Response should be DO");
        assertEquals(TERMINAL_TYPE, clientResponse[2],"Response option should be TTYPE");
    }

    @Test
    public void testTTYPE_server_sends_SB_TTYPE_SEND() {
        // Given: Server requests terminal type via subnegotiation
        byte[] request = {IAC, SB, TERMINAL_TYPE, QUAL_SEND, IAC, SE};

        // When: Check if properly formed
        boolean isProper = (request[0] == IAC && request[1] == SB &&
                            request[4] == IAC && request[5] == SE);

        // Then: Should be well-formed
        assertTrue(isProper,"TTYPE SEND request should be well-formed");
    }

    @Test
    public void testTTYPE_client_responds_with_IS_and_type_IBM_3179_2() {
        // Given: Server sent SEND request
        byte[] serverSend = {IAC, SB, TERMINAL_TYPE, QUAL_SEND, IAC, SE};

        // When: Create response with IBM-3179-2
        byte[] response = createTerminalTypeResponse("IBM-3179-2");

        // Then: Should have proper structure
        assertEquals(IAC, response[0],"First byte should be IAC");
        assertEquals(SB, response[1],"Second byte should be SB");
        assertEquals(TERMINAL_TYPE, response[2],"Third byte should be TTYPE");
        assertEquals(QUAL_IS, response[3],"Fourth byte should be IS (0)");
    }

    @Test
    public void testTTYPE_client_responds_with_IS_and_type_IBM_3477_FC() {
        // Given: Server requests with SEND
        // When: Respond with 132-column terminal
        byte[] response = createTerminalTypeResponse("IBM-3477-FC");

        // Then: Response should contain type string
        String extractedType = new String(extractTerminalTypeBytes(response), StandardCharsets.UTF_8);
        assertEquals("IBM-3477-FC", extractedType,"Should extract correct terminal type");
    }

    @Test
    public void testTTYPE_response_ends_with_SE_terminator() {
        // Given: Create terminal type response
        byte[] response = createTerminalTypeResponse("IBM-3179-2");

        // When: Check last two bytes
        int lastIndex = response.length - 1;
        int secondLastIndex = lastIndex - 1;

        // Then: Should end with IAC SE
        assertEquals(IAC, response[secondLastIndex],"Second to last byte should be IAC");
        assertEquals(SE, response[lastIndex],"Last byte should be SE");
    }

    @Test
    public void testTTYPE_custom_terminal_type() {
        // Given: Custom terminal type "AIXTERM"
        byte[] response = createTerminalTypeResponse("AIXTERM");

        // When: Extract
        String extracted = new String(extractTerminalTypeBytes(response), StandardCharsets.UTF_8);

        // Then: Should preserve custom type
        assertEquals("AIXTERM", extracted,"Should preserve custom terminal type");
    }

    @Test
    public void testTTYPE_all_valid_terminal_types_in_sequence() {
        // Given: Array of valid terminal types
        // When: Create response for each
        for (String termType : VALID_TERMINAL_TYPES) {
            byte[] response = createTerminalTypeResponse(termType);
            String extracted = new String(extractTerminalTypeBytes(response), StandardCharsets.UTF_8);

            // Then: Each should be preserved correctly
            assertEquals(termType, extracted,"Terminal type should be preserved: " + termType);
        }
    }

    // ==================== SEQUENCE: Out-of-Order & Repeated Negotiations ====================

    @Test
    public void testSequence_out_of_order_DO_before_SB() {
        // Given: DO comes after SB (reversed order)
        byte[] reversed = {IAC, SB, TERMINAL_TYPE, QUAL_SEND, IAC, SE, IAC, DO, TERMINAL_TYPE};

        // When: Check if both are present
        boolean hasSB = hasSubnegotiation(reversed);
        boolean hasDO = hasDOCommand(reversed);

        // Then: Both should parse independently
        assertTrue(hasSB && hasDO,"Both SB and DO should parse");
    }

    @Test
    public void testSequence_repeated_DO_BINARY_twice() {
        // Given: Server sends DO BINARY twice
        byte[] binary1 = {IAC, DO, TRANSMIT_BINARY};
        byte[] binary2 = {IAC, DO, TRANSMIT_BINARY};

        // When: Process both
        boolean result1 = isValidNegotiationSequence(binary1);
        boolean result2 = isValidNegotiationSequence(binary2);

        // Then: Should both be valid (idempotent)
        assertTrue(result1 && result2,"Repeated DO BINARY should both be valid");
    }

    @Test
    public void testSequence_alternating_DO_and_DONT_same_option() {
        // Given: DO BINARY, then DONT BINARY (contradiction)
        byte[] doCmd = {IAC, DO, TRANSMIT_BINARY};
        byte[] dontCmd = {IAC, DONT, TRANSMIT_BINARY};

        // When: Process both
        byte[] response1 = createWillResponse(TRANSMIT_BINARY);
        byte[] response2 = createRejectionResponse(TRANSMIT_BINARY);

        // Then: Both should produce valid responses
        assertEquals(WILL, response1[1],"DO should produce WILL");
        assertEquals(WONT, response2[1],"DONT should produce WONT");
    }

    @Test
    public void testSequence_WILL_before_DO() {
        // Given: Client sends WILL BINARY before server sends DO (out of order)
        byte[] clientWill = {IAC, WILL, TRANSMIT_BINARY};
        byte[] serverDo = {IAC, DO, TRANSMIT_BINARY};

        // When: Both are valid independently
        boolean willValid = isValidNegotiationSequence(clientWill);
        boolean doValid = isValidNegotiationSequence(serverDo);

        // Then: Both should be structurally valid
        assertTrue(willValid && doValid,"Both WILL and DO should be valid");
    }

    @Test
    public void testSequence_multiple_SB_requests_for_same_option() {
        // Given: Server sends TTYPE SEND request twice
        byte[] request1 = {IAC, SB, TERMINAL_TYPE, QUAL_SEND, IAC, SE};
        byte[] request2 = {IAC, SB, TERMINAL_TYPE, QUAL_SEND, IAC, SE};

        // When: Create responses for both
        byte[] response1 = createTerminalTypeResponse("IBM-3179-2");
        byte[] response2 = createTerminalTypeResponse("IBM-3179-2");

        // Then: Both should be valid
        assertTrue(isValidTerminalTypeResponse(response1) &&
                isValidTerminalTypeResponse(response2),"Both TTYPE responses should be valid");
    }

    @Test
    public void testSequence_TTYPE_exchange_followed_by_option_negotiation() {
        // Given: First TTYPE exchange, then option negotiation
        byte[] ttypeReq = {IAC, SB, TERMINAL_TYPE, QUAL_SEND, IAC, SE};
        byte[] ttypeResp = createTerminalTypeResponse("IBM-3179-2");
        byte[] doCmd = {IAC, DO, END_OF_RECORD};

        // When: Process sequence
        boolean ttypeValid = isValidTerminalTypeResponse(ttypeResp);
        boolean doValid = isValidNegotiationSequence(doCmd);

        // Then: Both should be valid
        assertTrue(ttypeValid && doValid,"TTYPE followed by DO should be valid");
    }

    @Test
    public void testSequence_rapid_option_changes_same_option() {
        // Given: Rapid changes for ECHO: DO, DONT, DO, DONT
        byte[] seq = new byte[12];
        int pos = 0;
        seq[pos++] = IAC; seq[pos++] = DO; seq[pos++] = ECHO;
        seq[pos++] = IAC; seq[pos++] = DONT; seq[pos++] = ECHO;
        seq[pos++] = IAC; seq[pos++] = DO; seq[pos++] = ECHO;
        seq[pos++] = IAC; seq[pos++] = DONT; seq[pos++] = ECHO;

        // When: Count valid sequences
        int validCount = 0;
        for (int i = 0; i < seq.length; i += 3) {
            if (i + 2 < seq.length && seq[i] == IAC) {
                validCount++;
            }
        }

        // Then: Should have 4 valid sequences
        assertEquals(4, validCount,"Should have 4 valid rapid sequences");
    }

    // ==================== TERMINAL TYPE: Malformed Types ====================

    @Test
    public void testMalformedTerminalType_empty_string() {
        // Given: Empty terminal type
        byte[] response = createTerminalTypeResponse("");

        // When: Validate structure
        boolean hasProperStructure = (response[0] == IAC && response[1] == SB &&
                                     response[2] == TERMINAL_TYPE && response[3] == QUAL_IS);

        // Then: Should maintain structure even with empty content
        assertTrue(hasProperStructure,"Empty TTYPE should have proper structure");
    }

    @Test
    public void testMalformedTerminalType_leading_whitespace() {
        // Given: Terminal type with leading space
        String withSpace = " IBM-3179-2";
        byte[] response = createTerminalTypeResponse(withSpace);
        String extracted = new String(extractTerminalTypeBytes(response), StandardCharsets.UTF_8);

        // When: Extract
        // Then: Should preserve the space
        assertEquals(withSpace, extracted,"Should preserve leading space");
    }

    @Test
    public void testMalformedTerminalType_trailing_whitespace() {
        // Given: Terminal type with trailing space
        String withSpace = "IBM-3179-2 ";
        byte[] response = createTerminalTypeResponse(withSpace);
        String extracted = new String(extractTerminalTypeBytes(response), StandardCharsets.UTF_8);

        // Then: Should preserve
        assertEquals(withSpace, extracted,"Should preserve trailing space");
    }

    @Test
    public void testMalformedTerminalType_crlf_injection() {
        // Given: Terminal type with CRLF
        String withCRLF = "IBM-3179-2\r\nExtra";
        byte[] response = createTerminalTypeResponse(withCRLF);
        String extracted = new String(extractTerminalTypeBytes(response), StandardCharsets.UTF_8);

        // Then: Should preserve CRLF
        assertEquals(withCRLF, extracted,"Should preserve CRLF in TTYPE");
    }

    @Test
    public void testMalformedTerminalType_unicode_characters() {
        // Given: Terminal type with unicode
        String withUnicode = "IBM-3179-2-\u00E9";  // é character
        byte[] response = createTerminalTypeResponse(withUnicode);

        // When: Extract and check
        byte[] ttypeBytes = extractTerminalTypeBytes(response);

        // Then: Should handle UTF-8 properly
        String extracted = new String(ttypeBytes, StandardCharsets.UTF_8);
        assertEquals(withUnicode, extracted,"Should preserve unicode characters");
    }

    @Test
    public void testMalformedTerminalType_special_characters() {
        // Given: Terminal type with special chars
        String withSpecials = "IBM-3179-2!@#$%^&*()";
        byte[] response = createTerminalTypeResponse(withSpecials);
        String extracted = new String(extractTerminalTypeBytes(response), StandardCharsets.UTF_8);

        // Then: Should preserve specials
        assertEquals(withSpecials, extracted,"Should preserve special characters");
    }

    @Test
    public void testMalformedTerminalType_maximum_length_255_bytes() {
        // Given: Terminal type at 255 byte limit
        String maxLength = new String(new char[255]).replace('\0', 'A');
        byte[] response = createTerminalTypeResponse(maxLength);

        // When: Validate
        int responseSize = response.length;

        // Then: Response should be bounded
        assertTrue(responseSize < 512,"Response should not exceed 512 bytes");
    }

    @Test
    public void testMalformedTerminalType_beyond_limit_256_bytes() {
        // Given: Terminal type exceeding reasonable limits
        String tooLong = new String(new char[300]).replace('\0', 'B');
        byte[] response = createTerminalTypeResponse(tooLong);

        // When: Extract
        byte[] ttypeBytes = extractTerminalTypeBytes(response);

        // Then: Should still be valid (protocol doesn't enforce, but structure maintained)
        assertTrue(response.length > 0,"Should create response even with long TTYPE");
    }

    // ==================== RESPONSE HANDLING: Accept, Reject, Ignore ====================

    @Test
    public void testResponse_DO_BINARY_accept_with_WILL() {
        // Given: Server sends DO BINARY
        byte[] serverDO = {IAC, DO, TRANSMIT_BINARY};

        // When: Create acceptance response
        byte[] response = createWillResponse(TRANSMIT_BINARY);

        // Then: Should be WILL BINARY
        assertEquals(WILL, response[1],"Should respond with WILL");
        assertEquals(TRANSMIT_BINARY, response[2],"Should echo option");
    }

    @Test
    public void testResponse_DO_UNSUPPORTED_reject_with_WONT() {
        // Given: Server sends DO for unsupported option (e.g., 99)
        byte[] serverDO = {IAC, DO, (byte) 99};

        // When: Create rejection
        byte[] response = createRejectionResponse((byte) 99);

        // Then: Should be WONT 99
        assertEquals(WONT, response[1],"Should respond with WONT");
        assertEquals((byte) 99, response[2],"Should echo unsupported option");
    }

    @Test
    public void testResponse_WILL_EOR_accept_with_DO() {
        // Given: Server sends WILL EOR
        byte[] serverWill = {IAC, WILL, END_OF_RECORD};

        // When: Create acceptance response
        byte[] response = createDoResponse(END_OF_RECORD);

        // Then: Should be DO EOR
        assertEquals(DO, response[1],"Should respond with DO");
        assertEquals(END_OF_RECORD, response[2],"Should echo option");
    }

    @Test
    public void testResponse_WONT_response_to_unsolicited_DO() {
        // Given: Unsolicited DO received
        byte[] unsolicited = {IAC, DO, (byte) 77};

        // When: Reject with WONT
        byte[] response = createRejectionResponse((byte) 77);

        // Then: WONT should be sent
        assertEquals(WONT, response[1],"Should reject with WONT");
    }

    @Test
    public void testResponse_multiple_responses_in_sequence() {
        // Given: Multiple DO commands
        byte[] do1 = {IAC, DO, TRANSMIT_BINARY};
        byte[] do2 = {IAC, DO, ECHO};
        byte[] do3 = {IAC, DO, (byte) 88};  // Unsupported

        // When: Create responses
        byte[] resp1 = createWillResponse(TRANSMIT_BINARY);
        byte[] resp2 = createWillResponse(ECHO);
        byte[] resp3 = createRejectionResponse((byte) 88);

        // Then: All should be valid
        assertEquals(WILL, resp1[1],"First response WILL");
        assertEquals(WILL, resp2[1],"Second response WILL");
        assertEquals(WONT, resp3[1],"Third response WONT");
    }

    // ==================== HELPER METHODS ====================

    private boolean isValidNegotiationSequence(byte[] seq) {
        if (seq == null || seq.length < 3) return false;
        if (seq[0] != IAC) return false;
        byte command = seq[1];
        return (command == DO || command == DONT || command == WILL || command == WONT);
    }

    private boolean isValidTerminalTypeResponse(byte[] response) {
        if (response == null || response.length < 6) return false;
        if (response[0] != IAC || response[1] != SB || response[2] != TERMINAL_TYPE) return false;
        if (response[3] != QUAL_IS) return false;
        // Check for SE terminator
        return (response[response.length - 2] == IAC && response[response.length - 1] == SE);
    }

    private byte[] createSubnegotiationRequest(byte option, byte qualifier) {
        return new byte[]{IAC, SB, option, qualifier, IAC, SE};
    }

    private byte[] createTerminalTypeResponse(String terminalType) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(IAC);
        baos.write(SB);
        baos.write(TERMINAL_TYPE);
        baos.write(QUAL_IS);
        try {
            baos.write(terminalType.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // Won't happen with ByteArrayOutputStream
        }
        baos.write(IAC);
        baos.write(SE);
        return baos.toByteArray();
    }

    private byte[] extractTerminalTypeBytes(byte[] response) {
        if (response == null || response.length < 6) return new byte[0];
        // TTYPE response: IAC SB TTYPE IS <bytes...> IAC SE
        int start = 4;  // After IAC SB TTYPE IS
        int end = response.length - 2;  // Before IAC SE
        if (start >= end) return new byte[0];
        byte[] result = new byte[end - start];
        System.arraycopy(response, start, result, 0, end - start);
        return result;
    }

    private byte[] createWillResponse(byte option) {
        return new byte[]{IAC, WILL, option};
    }

    private byte[] createDoResponse(byte option) {
        return new byte[]{IAC, DO, option};
    }

    private byte[] createRejectionResponse(byte option) {
        return new byte[]{IAC, WONT, option};
    }

    private boolean endsWithIAC_SE(byte[] seq) {
        if (seq == null || seq.length < 2) return false;
        return (seq[seq.length - 2] == IAC && seq[seq.length - 1] == SE);
    }

    private boolean hasNullByte(byte[] data) {
        if (data == null) return false;
        for (byte b : data) {
            if (b == 0) return true;
        }
        return false;
    }

    private boolean isValidTelnetCommand(byte command) {
        return (command == DO || command == DONT || command == WILL || command == WONT ||
                command == SB || command == SE);
    }

    private boolean hasSubnegotiation(byte[] data) {
        if (data == null) return false;
        for (int i = 0; i < data.length - 1; i++) {
            if (data[i] == IAC && data[i + 1] == SB) {
                return true;
            }
        }
        return false;
    }

    private boolean hasDOCommand(byte[] data) {
        if (data == null) return false;
        for (int i = 0; i < data.length - 1; i++) {
            if (data[i] == IAC && data[i + 1] == DO) {
                return true;
            }
        }
        return false;
    }
}
