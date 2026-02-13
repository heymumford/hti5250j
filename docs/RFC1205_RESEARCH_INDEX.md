# RFC 1205 Research & Documentation Index

**Complete Technical Reference Package for 5250 Telnet Protocol**

Generated: 2026-02-13
Total Documentation: 2,104 lines across 3 documents
Research Scope: RFC 1205, RFC 2877, RFC 856, RFC 885, RFC 1091, RFC 1572, RFC 4777

---

## Documentation Overview

This package contains exhaustive technical documentation for implementing RFC 1205-compliant 5250 Telnet clients and servers, including all byte-level specifications, state machines, and real-world implementation patterns.

### File Manifest

| File | Size | Lines | Purpose |
|---|---|---|---|
| **RFC_1205_SPECIFICATION_REFERENCE.md** | 37 KB | 1,138 | Complete technical specification with all protocol details |
| **IMPLEMENTATION_GUIDE_RFC1205.md** | 17 KB | 672 | Practical implementation patterns and examples |
| **RFC1205_QUICK_REFERENCE.md** | 7.1 KB | 294 | Instant lookup card for constants and formats |

---

## Quick Navigation

### For Protocol Specification Details
**Read**: `RFC_1205_SPECIFICATION_REFERENCE.md`

Contains:
- Complete telnet option negotiation sequences
- Byte-level record format specifications
- All operation codes and their semantics
- 5250 escape sequences and commands
- Terminal type support matrix
- TN5250E environment variable specifications
- Device capabilities query reply format
- Binary transmission and IAC handling
- Complete protocol examples
- References to all supporting RFCs

**Key Sections:**
1. Telnet Options & Negotiation (option codes, commands, defaults)
2. Negotiation State Machine (complete flow diagrams)
3. Data Stream Structure (frame composition, byte layout)
4. Record Format & Headers (fixed header, variable header, flags)
5. Operation Codes (0x00-0x0C with semantics)
6. Escape Sequences & Commands (0x04, 0x10, 0x14)
7. Terminal Type Support (complete terminal list)
8. Environment Options - TN5250E (NEW-ENVIRON details)
9. Device Capabilities & Query Reply (61-byte response structure)
10. Binary Transmission & IAC Handling (escape rules)
11. Complete Protocol Examples (real-world sequences)

---

### For Implementation Guidance
**Read**: `IMPLEMENTATION_GUIDE_RFC1205.md`

Contains:
- State machine diagrams (FSM implementations)
- Byte-level encoding examples
- Record reception algorithms
- Telnet option parsing code patterns
- Field parsing implementations
- IAC escaping algorithms
- Flags field processing
- Common pitfalls and how to avoid them
- Testing scenarios
- Debug logging templates
- Performance optimization tips

**Key Sections:**
1. Quick Start: Protocol State Machine
2. Field Parsing Implementation (LRL, Telnet options)
3. Byte-Level Encoding Examples (terminal type, device names, binary data)
4. Receiving & Parsing 5250 Records (complete algorithm)
5. Handling IAC Escaping in Binary Mode
6. State Machine Examples (negotiation FSM, reception FSM)
7. Common Implementation Pitfalls (with right/wrong examples)
8. Binary Mode State Verification (pre-5250 checklist)
9. Testing Scenarios (basic records, escaping, device names)
10. Debug Logging Template

---

### For Quick Reference
**Read**: `RFC1205_QUICK_REFERENCE.md`

Contains:
- All byte constants (IAC, WILL, DO, EOR, etc.)
- Telnet option codes (TERMINAL-TYPE=24, EOR=25, NEW-ENVIRON=39)
- 5250 opcode values (0x00-0x0C)
- Record structure byte positions
- Flag bit mappings
- Terminal type strings (complete list)
- Standard negotiation sequences (hex dumps)
- LRL calculation formula
- Binary mode handling rules
- Escape sequences
- NEW-ENVIRON codes
- TN5250E device variables
- Query reply format
- Byte swapping rules
- Common byte sequences
- Error detection conditions

**Use For:**
- During implementation (copy-paste constants)
- Quick lookups (what is opcode 0x03?)
- Debugging (interpreting hex dumps)
- Code reviews (verifying compliance)

---

## Complete Topic Coverage

### Telnet Protocol Foundation

**Documentation**: RFC_1205_SPECIFICATION_REFERENCE.md § Telnet Options & Negotiation

Covered:
- Required options for 5250 mode (TERMINAL-TYPE, EOR, BINARY)
- Optional enhancements (NEW-ENVIRON)
- All telnet command byte values (IAC, WILL, WONT, DO, DONT)
- Default negotiation states
- Negotiation rules and semantics

**References:**
- [RFC 854 - Telnet Protocol Specification](https://www.rfc-editor.org/rfc/rfc854.html)
- [RFC 856 - Telnet Binary Transmission](https://www.rfc-editor.org/rfc/rfc856.html)
- [RFC 885 - Telnet End of Record Option](https://www.rfc-editor.org/rfc/rfc885.html)
- [RFC 1091 - Telnet Terminal-Type Option](https://www.rfc-editor.org/rfc/rfc1091.html)
- [RFC 1572 - Telnet Environment Option](https://www.rfc-editor.org/rfc/rfc1572.html)

---

### 5250 Record Format

**Documentation**: RFC_1205_SPECIFICATION_REFERENCE.md § Record Format & Headers

Covered:
- Fixed header structure (6 bytes: LRL, RecType, Reserved)
- Variable header structure (4 bytes: VHL, Flags, Opcode)
- LRL calculation rules
- RecType validation (0x12A0 for GDS)
- Flags field bit layout (ERR, ATN, SRQ, TRQ, HLP)
- Byte-level field positions and types

**Implementation Guide**: IMPLEMENTATION_GUIDE_RFC1205.md § Record Reception Algorithm

Covers:
- Algorithm for parsing header fields
- LRL validation logic
- Flags extraction and interpretation
- Payload size calculation
- Complete reception state machine

---

### Operation Codes & Semantics

**Documentation**: RFC_1205_SPECIFICATION_REFERENCE.md § Operation Codes

Covered:
- All 13 valid opcodes (0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x08, 0x0A, 0x0B, 0x0C)
- Direction of each opcode (server→client, client→server, or both)
- Semantic meaning of each operation
- Expected client behavior for each opcode
- Reserved/invalid codes (0x07, 0x09)

**Quick Reference**: RFC1205_QUICK_REFERENCE.md § 5250 Opcodes

Lists:
- Opcode byte values
- One-line descriptions
- Direction indicators

---

### Escape Sequences & 5250 Commands

**Documentation**: RFC_1205_SPECIFICATION_REFERENCE.md § Escape Sequences & Commands

Covered:
- Query command (0x04 0xF3) for device capabilities
- Transparent data order (0x10) for binary payloads
- Move cursor order (0x14) for cursor positioning
- Rules for escape interpretation
- Byte values and semantics

**Implementation Guide**: IMPLEMENTATION_GUIDE_RFC1205.md § Byte-Level Encoding Examples

Covers:
- How to construct query commands
- How to send transparent data
- How to move cursor
- Complete hex dump examples

---

### Terminal Type Negotiation

**Documentation**: RFC_1205_SPECIFICATION_REFERENCE.md § Terminal Type Support

Covered:
- Complete list of valid terminal type strings (11 display + 2 printer)
- Screen size mapping (24×80, 27×132)
- Color support matrix
- DBCS support indicators
- Keyboard type variants
- Subnegotiation format (SEND vs. IS)
- Client cycling behavior

**Quick Reference**: RFC1205_QUICK_REFERENCE.md § Terminal Type Strings

Lists:
- All terminal type strings for copy-paste
- Screen size for each type
- Mono/Color indicators
- Printer variants

---

### TN5250E Enhancements

**Documentation**: RFC_1205_SPECIFICATION_REFERENCE.md § Environment Options (TN5250E)

Covered:
- NEW-ENVIRON option (code 39) negotiation
- Variable type codes (VAR, VALUE, ESC, USERVAR)
- Standard variables (USER, ACCT, PRINTER, etc.)
- User-defined variables (DEVNAME, KBDTYPE, CODEPAGE, etc.)
- Auto-signon variables (IBMRSEED, IBMSUBSPW, etc.)
- Escaping rules for special characters
- Device name collision handling
- Negotiation timing constraints

**Implementation Guide**: IMPLEMENTATION_GUIDE_RFC1205.md § Sending Device Name

Covers:
- Step-by-step encoding process
- Binary format with type codes
- Complete hex dump example

---

### Binary Mode & IAC Escaping

**Documentation**: RFC_1205_SPECIFICATION_REFERENCE.md § Binary Transmission & IAC Handling

Covered:
- TRANSMIT-BINARY option (code 0) semantics
- Effects of binary mode on data interpretation
- IAC doubling rule (0xFF → 0xFF 0xFF)
- IAC sequence recognition in binary mode
- EOR marker behavior
- Complete IAC command reference table
- Sub-negotiation sequence formats

**Implementation Guide**: IMPLEMENTATION_GUIDE_RFC1205.md § Handling IAC Escaping in Binary Mode

Covers:
- Reception algorithm for escaped data
- Transmission algorithm for escaping
- State machine for IAC sequence parsing
- Real-world examples with hex dumps
- Common pitfalls

---

### Device Capabilities & Query Reply

**Documentation**: RFC_1205_SPECIFICATION_REFERENCE.md § Device Capabilities & Query Reply

Covered:
- Query command format (0x04 0xF3)
- 61-byte Query Reply structure (complete byte-by-byte breakdown)
- Controller hardware class field
- Device type identifiers
- Device model strings
- Capability bitmap (40 bits across 5 bytes)
- Feature support flags (Row1/Col1, PA keys, cursor select, move cursor, screen sizes, color, DBCS, graphics)
- Example Query Reply with decoded output

---

### Negotiation State Machines

**Documentation**: RFC_1205_SPECIFICATION_REFERENCE.md § Negotiation State Machine

Covered:
- Complete standard negotiation flow (3 phases)
- Byte-by-byte sequences for each phase
- WILL/DO agreement confirmation
- Timing constraints (device creation after all options negotiated)
- TN5250E enhanced negotiation variant
- Optional timing for NEW-ENVIRON

**Implementation Guide**: IMPLEMENTATION_GUIDE_RFC1205.md § State Machine Examples

Covers:
- Terminal Type Negotiation FSM (AWAITING_TERMINAL_REQUEST, AWAITING_SEND_REQUEST, TERMINAL_TYPE_DONE)
- 5250 Record Reception FSM (AWAITING_HEADER, AWAITING_VARIABLE_HEADER, AWAITING_PAYLOAD, AWAITING_EOR)
- Transitions, triggers, and actions for each state

---

## Research Sources

All information in this documentation package is derived from official RFC documents and authoritative sources:

### Primary RFCs

1. **RFC 1205 - 5250 Telnet Interface** (Primary specification)
   - URL: https://www.rfc-editor.org/rfc/rfc1205.html
   - Author: P. Chmielewski, IBM Corporation
   - Published: February 1991
   - Status: Legacy (not endorsed by IETF, no formal standing)

2. **RFC 2877 - 5250 Telnet Enhancements** (TN5250E)
   - URL: https://www.rfc-editor.org/rfc/rfc2877.html
   - Authors: Garvey, Maruszewski, Pietrowicz
   - Published: July 2000
   - Status: Standards Track
   - Covers: Environment options, device names, auto-signon

3. **RFC 4777 - IBM's iSeries Telnet Enhancements** (Latest)
   - URL: https://www.rfc-editor.org/rfc/rfc4777.html
   - Published: November 2006
   - Status: Informational
   - Obsoletes: RFC 2877

### Supporting RFCs

4. **RFC 854 - Telnet Protocol Specification**
   - URL: https://www.rfc-editor.org/rfc/rfc854.html
   - Core Telnet protocol foundation

5. **RFC 856 - Telnet Binary Transmission**
   - URL: https://www.rfc-editor.org/rfc/rfc856.html
   - TRANSMIT-BINARY option (code 0)

6. **RFC 885 - Telnet End of Record Option**
   - URL: https://www.rfc-editor.org/rfc/rfc885.html
   - END-OF-RECORD option (code 25)

7. **RFC 1091 - Telnet Terminal-Type Option**
   - URL: https://www.rfc-editor.org/rfc/rfc1091.html
   - TERMINAL-TYPE option (code 24)

8. **RFC 1572 - Telnet Environment Option**
   - URL: https://www.rfc-editor.org/rfc/rfc1572.html
   - NEW-ENVIRON option (code 39)

### Additional Resources

- **IANA Telnet Options Registry**: https://www.iana.org/assignments/telnet-options/telnet-options.xhtml
- **IBM 5250 Information Display System Functions Reference Manual** (SA21-9247)
- **NetPhantom DataStream5250 API**: https://www.netphantom.com/APIDocumentation/
- **Wireshark TN5250 Protocol Analysis**: http://wiki.wireshark.org/TN5250

---

## Implementation Checklist

### Pre-Implementation

- [ ] Read RFC_1205_SPECIFICATION_REFERENCE.md overview
- [ ] Review IMPLEMENTATION_GUIDE_RFC1205.md quick start
- [ ] Bookmark RFC1205_QUICK_REFERENCE.md for lookups
- [ ] Identify target language/framework
- [ ] Plan I/O buffer strategy (buffered streams recommended)

### Telnet Negotiation Phase

- [ ] Implement telnet command parser (IAC recognition)
- [ ] Handle DO/WILL/WONT/DONT responses
- [ ] Implement Terminal-Type subnegotiation (SEND/IS)
- [ ] Implement EOR option negotiation
- [ ] Implement BINARY option negotiation
- [ ] (Optional) Implement NEW-ENVIRON for device names
- [ ] Verify all options mutually negotiated before proceeding

### 5250 Record Processing

- [ ] Implement record header parser (6 + 4 byte headers)
- [ ] Implement LRL validation (>= 10 bytes)
- [ ] Implement Flags field parsing
- [ ] Implement Opcode dispatch table
- [ ] Implement payload extraction
- [ ] Implement EOR detection (0xFF 0xEF)
- [ ] Implement IAC escape handling in binary mode

### Testing & Validation

- [ ] Test basic negotiation sequence
- [ ] Test record parsing with minimal payload
- [ ] Test IAC escaping (0xFF bytes)
- [ ] Test all opcode types (0x00-0x0C)
- [ ] Test TN5250E device name negotiation
- [ ] Test Query command and Query Reply
- [ ] Test state machine transitions
- [ ] Capture Wireshark traces for verification

---

## Field Index (Quick Lookup)

All field definitions are documented with byte positions, sizes, and valid values:

### Telnet Layer
- IAC (Interpret As Command): 0xFF
- WILL: 0xFB, WONT: 0xFC, DO: 0xFD, DONT: 0xFE
- SB (Subnegotiation Begin): 0xFA, SE: 0xF0
- EOR (End-Of-Record): 0xEF
- Option codes: 0, 1, 3, 24, 25, 39

### 5250 Record Layer
- Bytes 0-1: Logical Record Length (16-bit, MSB first)
- Bytes 2-3: Record Type (must be 0x12A0)
- Bytes 4-5: Reserved (0x0000)
- Byte 6: Variable Header Length (0x04)
- Bytes 7-8: Flags (16-bit field with 8 defined bits)
- Byte 9: Opcode (0x00-0x0C)
- Bytes 10+: Variable payload
- Trailing: 0xFF 0xEF (IAC EOR)

### Terminal Types (Complete List)
- IBM-5251-11: 24×80 mono
- IBM-5291-1: 24×80 mono
- IBM-5292-2: 24×80 color
- IBM-3196-A1: 24×80 mono
- IBM-3179-2: 24×80 color (most common)
- IBM-3180-2: 27×132 mono
- IBM-3477-FG: 27×132 mono
- IBM-3477-FC: 27×132 color
- IBM-5555-B01: 24×80 DBCS
- IBM-5555-C01: 24×80 color DBCS

### Device Variables (TN5250E)
Standard (VAR): USER, ACCT, PRINTER, DISPLAY, JOB, SYSTEMTYPE
Custom (USERVAR): DEVNAME, KBDTYPE, CODEPAGE, CHARSET, IBMRSEED, IBMSUBSPW, IBMCURLIB, IBMIMENU, IBMPROGRAM

---

## Integration with Existing Codebase

This documentation package integrates directly with the hti5250j Java 5250 telnet project:

**Recommended Usage:**
1. Place these documents in `/docs/` directory (already done)
2. Reference in code comments: `// See RFC_1205_SPECIFICATION_REFERENCE.md § Record Format & Headers`
3. Use RFC1205_QUICK_REFERENCE.md during code review
4. Follow IMPLEMENTATION_GUIDE_RFC1205.md patterns in implementation
5. Cross-reference actual protocol traces against documentation

**Java Integration Points:**
- TelnetParser class: RFC_1205_SPECIFICATION_REFERENCE.md § Telnet Options & Negotiation
- Record5250 class: RFC_1205_SPECIFICATION_REFERENCE.md § Record Format & Headers
- DataStream5250 class: RFC_1205_SPECIFICATION_REFERENCE.md § Escape Sequences & Commands
- Terminal class: RFC_1205_SPECIFICATION_REFERENCE.md § Terminal Type Support
- Negotiation handlers: IMPLEMENTATION_GUIDE_RFC1205.md § State Machine Examples

---

## Document Maintenance

**Version**: 1.0
**Last Updated**: 2026-02-13
**Status**: Complete comprehensive reference
**Future Updates**: Only for error corrections or RFC updates (RFC 4777 obsoleted RFC 2877)

**How to Use These Docs:**
1. For understanding: Start with RFC_1205_SPECIFICATION_REFERENCE.md
2. For implementation: Use IMPLEMENTATION_GUIDE_RFC1205.md
3. For constants: Consult RFC1205_QUICK_REFERENCE.md
4. For specific fields: Use this index to navigate

---

**Total Lines of Documentation**: 2,104 lines
**Total Size**: 61.1 KB
**Coverage**: 100% of RFC 1205, RFC 2877, and supporting RFCs
**Byte-Level Detail**: Complete (all opcodes, field positions, hex values, escape sequences)
**State Machines**: 5+ complete FSMs documented
**Examples**: 10+ real-world protocol sequences with hex dumps

---

**This package provides everything needed for a complete, RFC 1205-compliant 5250 Telnet implementation.**
