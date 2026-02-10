# Phase 15: Real IBM i System Testing

**Date:** February 10, 2026
**Status:** Planning
**Goal:** Establish D2 contract test framework and verify HTI5250J against real IBM i system

---

## Context & Prerequisites

### What's Complete (Phases 0-14)
- ✅ Phases 0-13: Modernization (rebranding, virtual threads, sealed classes, workflows)
- ✅ Phase 14: Test ID traceability (21 surface tests with explicit IDs)
- ✅ D1 Unit tests: Codec API, field boundaries, protocol constants
- ✅ D3 Surface tests: Protocol negotiation, field isolation, virtual thread safety
- ✅ PROTOCOL_RESEARCH.md: Documented uncertainties and verified behaviors
- ✅ ARCHITECTURE.md: Test traceability mapping

### D2 Contract Test Gap
No real IBM i testing yet. D2 is designed for:
- **Continuous drift detection** between HTI5250J and actual IBM i
- **Real protocol verification** (replace RFC assumptions with observed behavior)
- **System integration testing** (network, authentication, data handling)

---

## Discovery Phase: Environment & Capabilities

### A. Identify Test IBM i System

**Options:**

1. **Local VM/Simulator** (Recommended for Phase 15)
   - IBM i development system (PASE, DB2, menu systems)
   - Test programs: PMTENT (payment entry), LNINQ (line inquiry) from Phase 1-14
   - Cost: Development license (~$free–$5K one-time)
   - Setup: 2-4 hours

2. **Cloud IBM i** (AWS, Azure, IBM Cloud)
   - Managed service instances
   - Cost: $500-2000/month
   - Setup: 30 min (purchase) + 1 hour (network config)

3. **Real Production System** (Only for Phase 16+)
   - Not recommended for Phase 15 (risk)
   - Requires approval, monitoring, rollback procedures

**Decision for Phase 15:** Assume local VM or simulator available; design tests agnostic to system.

### B. Test Credentials & Access

**Required:**
- IBM i hostname/IP (e.g., 192.168.1.100)
- TN5250E port (typically 23 or 992)
- User ID / password (test account)
- Menu system available (PMTENT, LNINQ, etc.)

**Security:**
- Credentials stored in `~/.env` ONLY (never committed)
- Test account: Read-only or sandboxed data
- Network: TLS (port 992) preferred over plain (port 23)

**Assumption for Phase 15:** Credentials available in test environment; methods parameterized for CI/CD injection.

---

## Contract Test Architecture (D2)

### What is a Contract Test?

A contract test verifies that:
1. Client (HTI5250J) sends commands to server (IBM i) **as documented**
2. Server responds with expected data **in documented format**
3. **Neither side violates the contract** (RFC 1205 TN5250E)

**Key difference from unit tests:**
- D1: Tests code in isolation ("Does CharMappings.getCodePage() work?")
- D2: Tests integration ("Does HTI5250J ↔ IBM i round-trip work?")
- D1 assumes RFC correct; D2 verifies RFC assumption matches reality

### Contract Elements

| Element | Example | Verified By |
|---------|---------|------------|
| **Negotiation** | Client sends IAC WILL TN5250E; server responds IAC DO | D2-PROTO-INIT-001 |
| **Screen Format** | Server sends screen with 80 columns, 24 rows | D2-SCHEMA-SCREEN-001 |
| **Field Attributes** | PROTECTED fields reject input; HIDDEN fields don't display | D2-SCHEMA-FIELD-001 |
| **Data Input** | Send "WRKSYSVAL" command; IBM i processes it | D2-ACTION-CMD-001 |
| **Keyboard State** | Keyboard locked while IBM i processes; unlocked when ready | D2-CONCUR-KBD-001 |
| **Session Cleanup** | Disconnect properly; no lingering connections | D2-CONCUR-CLEANUP-001 |

---

## Phase 15 Phases (A-E)

### Phase 15A: Environment Setup & Capability Discovery (2 hours)

**Goal:** Determine what IBM i system is available and what tests we can write.

**Tasks:**

1. **Inventory Available Systems**
   - [ ] Check for local IBM i VM/simulator
   - [ ] Check for cloud credentials (AWS, Azure, IBM Cloud)
   - [ ] Document system version, available programs
   - [ ] Test network connectivity (ping, telnet port 23/992)

2. **Identify Available Test Programs**
   - [ ] PMTENT (payment entry) — available?
   - [ ] LNINQ (line inquiry) — available?
   - [ ] Other menu systems we can test?
   - [ ] Sandbox databases available?

3. **Establish Credentials**
   - [ ] Create test account (or use existing)
   - [ ] Store in `~/.env` with format: `IBM_I_HOST=`, `IBM_I_USER=`, `IBM_I_PASS=`
   - [ ] Test login from terminal: `telnet $IBM_I_HOST 23`

4. **Document System Metadata**
   - [ ] OS version (e.g., IBM i 7.5)
   - [ ] CCSID (character set, usually 37 for USA)
   - [ ] Available subsystems
   - [ ] Known quirks or limitations

**Output:** `docs/IBM_I_TEST_ENVIRONMENT.md` (not committed, .gitignore)

---

### Phase 15B: Minimal D2-PROTO-INIT Test (3 hours)

**Goal:** Create smallest possible contract test that verifies TN5250E negotiation with real IBM i.

**File:** `tests/org/hti5250j/contracts/IBM_i_ProtocolInitTest.java`

**Test Structure:**

```java
@DisplayName("D2-PROTO-INIT: TN5250E Negotiation with Real IBM i")
public class IBM_i_ProtocolInitTest {
    
    private static String IBM_I_HOST = System.getenv("IBM_I_HOST");
    private static String IBM_I_USER = System.getenv("IBM_I_USER");
    private static String IBM_I_PASS = System.getenv("IBM_I_PASS");
    
    @BeforeEach
    void setup() {
        assumeTrue(IBM_I_HOST != null, "IBM_I_HOST not configured");
    }
    
    @Test
    @DisplayName("D2-PROTO-INIT-001: Connect and receive negotiation sequence")
    void testNegotiationWithRealIBMi() throws Exception {
        // GIVEN: Real IBM i system credentials
        // WHEN: Connect to TN5250E port
        // THEN: Receive IAC DO TN5250E (verify against RFC 1205)
    }
    
    @Test
    @DisplayName("D2-PROTO-INIT-002: Respond to negotiation with WILL")
    void testRespondToNegotiation() throws Exception {
        // GIVEN: Negotiation sequence received
        // WHEN: Send IAC WILL TN5250E
        // THEN: IBM i accepts and proceeds to screen transmission
    }
}
```

**Key Implementation:**
- Use raw socket connection (not Session5250) to verify protocol bytes
- Log hex dumps of negotiation sequence
- Verify against RFC 1205 Section 4.1
- Assume/skip test if `IBM_I_HOST` not configured

**Output:** 2-3 passing tests; protocol verification logs

---

### Phase 15C: D2 Screen Contract Tests (4 hours)

**Goal:** Verify that real IBM i screens match expected format (80×24, field encoding).

**File:** `tests/org/hti5250j/contracts/IBM_i_ScreenFormatTest.java`

**Tests:**

```
D2-SCHEMA-SCREEN-001: Receive initial display with menu
D2-SCHEMA-SCREEN-002: Screen dimensions (80 cols, 24 rows)
D2-SCHEMA-SCREEN-003: Field attributes encoded correctly
D2-SCHEMA-FIELD-001: PROTECTED fields reject input
D2-SCHEMA-FIELD-002: HIDDEN fields don't display but store data
D2-SCHEMA-FIELD-003: Numeric fields enforce digit-only constraint
```

**Test Flow:**
1. Connect and authenticate
2. Request menu (PMTENT if available)
3. Parse screen response
4. Verify field count, positions, attributes
5. Attempt invalid input to protected field
6. Verify field isolation (write to one field doesn't affect others)

**Output:** 6 tests; field encoding verification

---

### Phase 15D: D2 Action/Response Contract Tests (5 hours)

**Goal:** Verify that HTI5250J → IBM i commands execute correctly and return expected responses.

**Tests:**

```
D2-ACTION-CMD-001: Send WRKSYSVAL command
D2-ACTION-CMD-002: Send payment entry (PMTENT)
D2-ACTION-CMD-003: Send line inquiry (LNINQ)
D2-ACTION-RESP-001: Verify PMTENT screen data
D2-ACTION-RESP-002: Verify LNINQ screen data
D2-CONCUR-KBD-001: Keyboard lock while processing
D2-CONCUR-KBD-002: Keyboard unlock when ready
```

**Implementation:**
- Use Session5250 API (not raw socket)
- Execute real workflows (PMTENT: enter payment data)
- Verify response screens match expected format
- Test error cases (invalid input, timeout)

**Output:** 7 tests; end-to-end workflow verification

---

### Phase 15E: Documentation & Gap Analysis (2 hours)

**Goal:** Document what works, what doesn't, and what Phase 16 needs.

**Tasks:**

1. **Create D2-RESULTS.md**
   - Summary of passing/failing tests
   - Protocol compliance findings (RFC violations?)
   - System quirks discovered
   - Recommendations for Phase 16

2. **Update PROTOCOL_RESEARCH.md**
   - Mark verified items (from Phase 1.4)
   - Document new findings
   - Flag remaining uncertainties

3. **Update ARCHITECTURE.md**
   - Add D2 test traceability
   - Document system assumptions (IBM i 7.5, CCSID 37, etc.)

4. **Create Phase 16 Plan**
   - High-concurrency testing (1000 parallel sessions)
   - Error recovery scenarios
   - Security testing (credential validation, injection)
   - Performance benchmarks

**Output:** 4 documentation files; Phase 16 roadmap

---

## Success Criteria

| Phase | Goal | Pass Criteria |
|-------|------|---------------|
| **15A** | Environment discovery | IBM i system identified, credentials working |
| **15B** | Protocol verification | D2-PROTO-INIT tests passing (2-3 tests) |
| **15C** | Screen format contract | D2-SCHEMA tests passing (6 tests) |
| **15D** | Action/response contract | D2-ACTION tests passing (7 tests) |
| **15E** | Documentation | Phase 16 roadmap complete |

**Overall:** 15+ contract tests passing, zero regressions in D1/D3 tests, protocol compliance verified against RFC 1205.

---

## Blockers & Assumptions

### Required
- [ ] IBM i system available (local VM, cloud, or production)
- [ ] Network access to TN5250E port (23 or 992)
- [ ] Test account with menu access (PMTENT or equivalent)
- [ ] Credentials injectable via environment variables

### Assumptions
- IBM i OS version: 7.3+ (should work with older, but untested)
- CCSID 37 (USA) configured (protocol tests written for this)
- Menu systems available (PMTENT, LNINQ, or similar)
- No production data testing (use sandbox/test data only)

### Risk: IBM i Not Available
If no IBM i system available:
- Phase 15 → **DEFER to Phase 16** (when infrastructure ready)
- Continue with Phase 3 (SessionConfig migration) instead
- Document gaps in PROTOCOL_RESEARCH.md for future

---

## Timeline

| Phase | Hours | Duration | Blockers |
|-------|-------|----------|----------|
| **15A** | 2 | 30 min–2 hours | IBM i availability |
| **15B** | 3 | 1–3 hours | Network access |
| **15C** | 4 | 1–4 hours | PMTENT availability |
| **15D** | 5 | 2–5 hours | Test data setup |
| **15E** | 2 | 1–2 hours | Test results |
| **TOTAL** | 16 | 5–16 hours | Depends on system readiness |

---

## Decision Point

**Before proceeding:**

1. **Do we have IBM i access?**
   - YES → Execute Phase 15A immediately
   - NO → Execute Phase 3 (SessionConfig migration) instead, defer Phase 15

2. **What priority is Phase 15?**
   - CRITICAL → Continue immediately
   - OPTIONAL → Can defer to Phase 16

3. **What's the target IBM i system?**
   - Local VM / Simulator
   - Cloud service
   - Production (with caution)

---

## Next Step

**Execute Phase 15A:** Environment setup and capability discovery

Would you like to:
- **Proceed with Phase 15A** (requires IBM i access)
- **Defer Phase 15, execute Phase 3** (SessionConfig migration)
- **Plan Phase 15 further** (design more test scenarios)

