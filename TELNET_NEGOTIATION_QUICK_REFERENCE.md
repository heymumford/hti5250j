# TN5250j Telnet Negotiation Test Quick Reference

## Test File Location
```
tests/org/tn5250j/TelnetNegotiationPairwiseTest.java
```

## Quick Stats
| Metric | Value |
|--------|-------|
| **Total Tests** | 52 |
| **All Pass** | ✓ 100% |
| **Lines of Code** | 926 |
| **Execution Time** | <20ms |
| **Test Categories** | 7 |
| **Pairwise Dimensions** | 5 |

## Test Categories (52 Total)

| # | Category | Count | Focus |
|---|----------|-------|-------|
| 1 | **POSITIVE** | 8 | Standard RFC 854 compliance, happy path |
| 2 | **PROTOCOL** | 7 | IAC handling, state transitions, truncation |
| 3 | **ADVERSARIAL** | 8 | Injections, buffer overflow, rapid floods |
| 4 | **TTYPE** | 8 | Terminal type exchange, SB/SE framing |
| 5 | **SEQUENCE** | 7 | Out-of-order, repeated, rapid options |
| 6 | **MALFORMED** | 8 | Empty, oversized, encoded types |
| 7 | **RESPONSE** | 6 | Accept/reject/ignore patterns |

## Running the Tests

### Compile
```bash
cd ~/ProjectsWATTS/tn5250j-headless
javac -cp "build:lib/development/*:lib/runtime/*" \
  -d build tests/org/tn5250j/TelnetNegotiationPairwiseTest.java
```

### Execute
```bash
java -cp "build:lib/development/*:lib/runtime/*" \
  org.junit.runner.JUnitCore \
  org.tn5250j.TelnetNegotiationPairwiseTest
```

### Expected Output
```
JUnit version 4.5
....................................................
Time: 0.018

OK (52 tests)
```

## Test Dimensions

### 1. Telnet Option (6 options)
- TRANSMIT_BINARY (0)
- ECHO (1)
- SGA (3)
- TERMINAL_TYPE (24)
- END_OF_RECORD (25)
- TN5250E (40)

### 2. Command (7 types)
- DO (253 / 0xFD)
- DONT (254 / 0xFE)
- WILL (251 / 0xFB)
- WONT (252 / 0xFC)
- SB - Subnegotiation Begin (250 / 0xFA)
- SE - Subnegotiation End (240 / 0xF0)
- INVALID (not a real command)

### 3. Sequence (5 patterns)
- Standard (proper RFC order)
- Out-of-order (reversed)
- Repeated (same command 2×)
- Rapid (50 alternations)
- Unclosed (SB without SE)

### 4. Terminal Type (8 variants)
- IBM-3179-2 (80×24)
- IBM-3477-FC (132×24)
- AIXTERM (custom)
- ANSI (generic)
- Empty string
- Oversized (512 bytes)
- Malformed (null, CRLF, unicode)

### 5. Response Action (4 types)
- Accept (WILL/DO)
- Reject (WONT/DONT)
- Ignore (no response)
- Invalid (malformed)

## Key Test Methods

### Positive Tests
```java
testNegotiation_WILL_BINARY_standard_sequence()
testNegotiation_WILL_TTYPE_with_valid_terminal_type_IBM_3179_2()
testNegotiation_multiple_options_sequential()
```

### Protocol Compliance Tests
```java
testProtocol_IAC_IAC_escape_in_data_stream()
testProtocol_SB_with_proper_SE_terminator()
testProtocol_option_state_transition_DO_to_WILL()
```

### Adversarial Tests
```java
testAdversarial_null_byte_in_terminal_type()
testAdversarial_oversized_terminal_type_512_bytes()
testAdversarial_multiple_negotiation_floods_rapid_DO_DONT()
```

### Terminal Type Tests
```java
testTTYPE_server_sends_SB_TTYPE_SEND()
testTTYPE_client_responds_with_IS_and_type_IBM_3179_2()
testTTYPE_all_valid_terminal_types_in_sequence()
```

### Sequence Tests
```java
testSequence_out_of_order_DO_before_SB()
testSequence_repeated_DO_BINARY_twice()
testSequence_rapid_option_changes_same_option()
```

### Malformed Type Tests
```java
testMalformedTerminalType_empty_string()
testMalformedTerminalType_crlf_injection()
testMalformedTerminalType_unicode_characters()
```

### Response Tests
```java
testResponse_DO_BINARY_accept_with_WILL()
testResponse_DO_UNSUPPORTED_reject_with_WONT()
testResponse_multiple_responses_in_sequence()
```

## RFC 854 Protocol Bytes

```
IAC     = 0xFF (255) - Interpret As Command
DO      = 0xFD (253)
DONT    = 0xFE (254)
WILL    = 0xFB (251)
WONT    = 0xFC (252)
SB      = 0xFA (250) - Subnegotiation Begin
SE      = 0xF0 (240) - Subnegotiation End
```

## Typical Negotiation Flow

```
Server: [FF][FD][00]          DO TRANSMIT_BINARY
Client: [FF][FB][00]          WILL TRANSMIT_BINARY

Server: [FF][FD][18]          DO TERMINAL_TYPE
Client: [FF][FB][18]          WILL TERMINAL_TYPE
        [FF][FA][18][01]      SB TERMINAL_TYPE SEND
        [FF][F0]              SE

Server: [FF][FA][18][01]      SB TERMINAL_TYPE SEND
        [FF][F0]              SE

Client: [FF][FA][18][00]      SB TERMINAL_TYPE IS
        49 42 4D 2D ... 32    "IBM-3179-2" (UTF-8 bytes)
        [FF][F0]              SE
```

## Coverage Highlights

✓ Standard RFC 854 compliance
✓ Option state machine (DO→WILL transitions)
✓ IAC byte escaping (0xFF 0xFF = literal)
✓ Subnegotiation framing (SB...SE)
✓ Terminal type exchange (RFC 1091)
✓ Malformed sequence detection
✓ Buffer overflow protection
✓ Injection attack handling
✓ Rapid option negotiation floods
✓ Out-of-order option processing

## Integration Points

**Source File**: `src/org/tn5250j/framework/tn5250/tnvt.java`

**Method Tested**: `negotiate(byte[] abyte0)` (line 2308)

**Key Code Paths**:
- Line 2313: IAC detection
- Line 2317: Command type dispatch (DO/WILL/WONT/SB)
- Line 2331-2387: Option handling (TTYPE, EOR, BINARY, etc.)
- Line 2425-2440: TTYPE subnegotiation response

## Test Data Sets

### Valid Terminal Types
- `IBM-3179-2` ✓
- `IBM-3477-FC` ✓
- `AIXTERM` ✓
- `ANSI` ✓

### Malformed Terminal Types
- `` (empty)
- ` IBM-3179-2` (leading space)
- `IBM-3179-2 ` (trailing space)
- `IBM-3179-2\u0000` (null injection)
- `IBM-3179-2\r\n` (CRLF injection)
- 512-byte string (buffer overflow)
- `\u00FF\u00FF\u00FF` (IAC bytes)
- `../../etc/passwd` (path traversal)
- `<script>alert('xss')</script>` (XSS payload)

## Performance

| Metric | Value |
|--------|-------|
| Total Execution | <20ms |
| Per-Test Average | <0.4ms |
| Setup/Teardown | Negligible |
| Memory Footprint | < 1MB |

## Known Limitations

- Unit-level tests (no socket I/O)
- Protocol-level validation only
- No real server simulation
- TN5250E option not yet enabled
- NEW_ENVIRONMENT option pending

## Future Enhancements

1. **Integration Tests**: Mock socket with real server responses
2. **TN5250E Tests**: Proprietary option negotiation
3. **Performance Tests**: Negotiation round-trip timing
4. **Stress Tests**: 1000+ rapid negotiation sequences
5. **Security Tests**: OWASP telnet vulnerabilities

## Related Tests

- `ConfigurationPairwiseTest.java` - Configuration handling
- `ConnectionLifecyclePairwiseTest.java` - Connection lifecycle
- `DataStreamPairwiseTest.java` - 5250 data stream parsing
- `ScreenPlanesPairwiseTest.java` - Screen rendering

## Contact & Status

**File**: `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/tests/org/tn5250j/TelnetNegotiationPairwiseTest.java`

**Status**: ✓ COMPLETE & PASSING

**Date**: 2026-02-04

**Commit**: 0abada3 (feature/capital-markets-sync-to-master)

---

For detailed coverage analysis, see `TELNET_NEGOTIATION_TEST_DELIVERY.md`
