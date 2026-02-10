# HTI5250J Protocol Research & Verification

**Date:** February 2026 (Phase 3)
**Status:** Verified behaviors documented; unverified findings listed for Phase 14 real i5 testing

---

## Verified Protocol Behaviors (RFC 1205)

### 1. System Request Codes (RFC 1205, Section 4.3)

**Location:** `src/org/hti5250j/framework/tn5250/tnvt.java:598`

System requests are single-character commands sent by the client to the server:
- '0' = No operation
- '1' = Shut down / Close session
- '2' = **Clear Data Queue** ✅ VERIFIED
- '3' = Clear screen
- Other codes reserved for specific functions

**Implementation:**
```java
// RFC 1205 Section 4.3 - System Request codes: '2' = Clear Data Queue
if (sr.charAt(0) == '2') {
    dsq.clear();
}
```

**Risk Level:** LOW (well-documented in RFC, matches implementation)

---

## Unverified Protocol Behaviors (Flagged for Phase 14)

These findings are documented in source code with uncertain comments. Phase 14 will verify against real IBM i systems.

### 1. Query Response Length Variations (RFC 1205, Section 5.5)

**Location:** `src/org/hti5250j/framework/tn5250/tnvt.java:2173-2176`

**Current Code:**
```java
//  the length between 58 and 64 seems to cause
//  different formatting codes to be sent from
//  the host ???????????????? why ???????
```

**Finding:** Query Response length field (bytes 7-8) may trigger different server behavior patterns depending on value range.

**RFC Reference:** RFC 1205 Section 5.5 (Query Response Structure)
- Byte count field (2 bytes) specifies response payload length
- Length 58-64 range may have special meaning in legacy i5 releases

**Verification Required:**
- [ ] Test query response with length 50, 58, 60, 64, 70 on real i5
- [ ] Document formatting code changes per length
- [ ] Verify if length field follows big-endian or mixed-endian convention

**Risk Level:** MEDIUM (may cause formatting issues on untested response sizes)

---

### 2. Cursor Positioning Control Bits

**Location:** `src/org/hti5250j/framework/tn5250/tnvt.java:2054-2057`

**Current Code:**
```java
// reset blinking cursor seems to control whether to set or not set the
// the cursor position. No documentation for this just testing and
// looking at the bit settings of this field. This was a pain in the ass!
```

**Finding:** OIA status byte bit patterns control cursor positioning:
- Blinking cursor bit (unclear which bit) appears to gate cursor position updates
- Cursor position may be ignored when blinking is active

**Verification Required:**
- [ ] Identify exact bit position (OIA byte 0, bits 0-7)
- [ ] Map bit combinations to cursor behavior (suppress/update/auto)
- [ ] Test on multiple i5 releases and code pages

**Risk Level:** HIGH (affects terminal responsiveness; may fail on untested releases)

---

### 3. Keyboard Lock/Unlock Heuristic

**Location:** `src/org/hti5250j/framework/tn5250/tnvt.java:2062`

**Current Code:**
```java
// this seems to work so far
if ((byte1 & 0x20) == 0x20 && (byte1 & 0x08) == 0x00) {
```

**Finding:** Keyboard lock status detected by OIA byte pattern:
- Bit 5 (0x20) = 1
- Bit 3 (0x08) = 0

**Interpretation:** Combination may indicate "application active, input allowed" state.

**Verification Required:**
- [ ] Verify RFC 1205 Section 3.1 OIA bit field definitions
- [ ] Test keyboard lock timing (duration, timeout behavior)
- [ ] Verify state machine transitions (unlock → re-lock scenarios)
- [ ] Check edge cases (network delay, rapid input, hung application)

**Risk Level:** MEDIUM (may desynchronize keyboard state on edge cases)

---

### 4. Roll Operation (CMD_ROLL, 0x23)

**Location:** `src/org/hti5250j/framework/tn5250/tnvt.java:1498`

**Current Code:**
```java
case CMD_ROLL: // 0x23 35 Roll Not sure what it does right now
```

**Finding:** Roll operation opcode is identified (0x23) but behavior undefined.

**Likely Behavior:** Screen scrolling operation used for paged displays.
- Roll up: Scroll visible area up N lines, reveal bottom N lines
- Roll down: Scroll visible area down N lines, reveal top N lines
- Common use: Multi-page reports, terminal scrollback

**Verification Required:**
- [ ] Read IBM 5250 Data Stream Reference manual (Section on Roll)
- [ ] Test roll operation with multi-page display
- [ ] Verify row/column parameters (if any)
- [ ] Check interaction with cursor position and focus

**Risk Level:** MEDIUM (feature incomplete; may affect multi-screen workflows)

---

## Protocol Implementation Status

### Complete (Tested)
- EBCDIC codec round-trip (Phase 1-5)
- Field attribute parsing (Phase 11)
- Session lifecycle (connection, disconnect, error recovery)
- Virtual thread polling loop (Phase 2)

### Partial (Tested but edge cases unverified)
- Cursor positioning (works "so far", untested edge cases)
- Keyboard lock detection (heuristic-based, not spec-based)
- Query response handling (length variations untested)

### Incomplete (Implemented but purpose unclear)
- Roll operation (opcode recognized, behavior undefined)

---

## Phase 14 Integration Plan

**Test Categories:**

| Finding | Test Type | Effort | Priority |
|---------|-----------|--------|----------|
| Query length variations | D3-PROTO-005 | 2 hours | MEDIUM |
| Cursor position bits | D3-PROTO-006 | 3 hours | HIGH |
| Keyboard lock edge cases | D3-CONCUR-004 | 2 hours | MEDIUM |
| Roll operation | D3-PROTO-004 | 2 hours | MEDIUM |

**Total Phase 14 effort impact:** ~9 hours (out of 13-hour estimate)

---

## References

- RFC 1205: "5250 Telnet Interface" (IETF Standard)
- IBM i Documentation: "5250 Data Stream Programming" (IBM Systems Documentation)
- HTI5250J Code: `/src/org/hti5250j/framework/tn5250/tnvt.java`, `Screen5250.java`

