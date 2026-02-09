# Testing Framework & Strategy

**Date:** February 2026
**Phase:** 11 (Workflow Execution Handlers)
**Audience:** Contributors, QA, and anyone running the test suite

---

## Overview

HTI5250J uses an **inverted test pyramid** that prioritizes boundary correctness and real-world behavior over isolated unit tests. The framework is built on four test domains, each addressing distinct risks.

### The Four-Domain Model

| Domain | Purpose | Environment | Cadence | Gate | Typical Risk Mitigated |
|--------|---------|-------------|---------|------|------------------------|
| **Domain 1: Unit** | Validate translation-layer logic | Local, no i5 | Pre-commit | Blocking | Logic bugs in codecs, field parsing |
| **Domain 2: Continuous Contracts** | Detect schema/protocol/execution drift | Real IBM i | 24/7 background | Alert | Schema drift, protocol mismatch |
| **Domain 3: Surface** | Verify boundary correctness & idempotency | Real IBM i or mocks | Per-commit | Blocking | Silent data loss, field truncation, race conditions |
| **Domain 4: Scenario** | Validate end-to-end business workflows | Real IBM i or mocks | Per-commit + nightly | Blocking | Workflow failure, error recovery bugs |

---

## Domain 1: Unit Tests (Local, Fast)

**Purpose:** Validate core translation logic without requiring IBM i connection.

**Characteristics:**
- Run locally in <5 seconds
- No external dependencies (no network, no IBM i)
- Isolated from system behavior
- Pre-commit gate (blocks development)

### Test Classes & Coverage

| Test Class | Tests | Focus | Example |
|------------|-------|-------|---------|
| `EBCDICCodecTest` | 12 | Character encoding (EBCDIC ↔ UTF-8) | "C8859396" → "Hello" |
| `FieldAttributeTest` | 8 | Field attribute byte parsing | 0x20 → PROTECTED \| HIDDEN |
| `DecimalSerializationTest` | 6 | COMP-3 packed decimal encoding | 12345.67 → packed bytes |
| `ScreenFieldTest` | 10 | Field position & bounds validation | Row 5, Column 10, length 20 |
| Plugin contract tests | 10 | Plugin lifecycle (load→activate→deactivate) | Mock plugins |
| Session contract tests | 54 | Session state machine, idempotency | Connect twice → error |
| Workflow validation tests | 18 | Parameter substitution, schema validation | ${data.x} resolution |

**Total Domain 1: ~130 tests, <5 seconds**

### Running Domain 1 Tests

```bash
# All unit tests (Domain 1 only)
./gradlew test --tests "*Test" \
  --exclude "*SurfaceTest" \
  --exclude "*ScenarioTest" \
  --exclude "*IntegrationTest"

# Specific test
./gradlew test --tests "EBCDICCodecTest"

# With coverage report
./gradlew test --tests "*Test" --exclude "*SurfaceTest" \
  jacocoTestReport
```

**Coverage Target:** 80%+ for core protocol

---

## Domain 2: Continuous Contracts (Background Monitoring)

**Purpose:** Detect schema/protocol/execution drift in real-time without blocking development.

**Characteristics:**
- Requires real IBM i connection (configured via environment variables)
- Runs 24/7 in background (separate job, not in CI)
- Non-blocking (failures generate alerts, not test failures)
- Detects problems that Unit/Surface tests can't (external system changes)

### Three Contract Channels

#### A. Schema Contracts (Every 5 minutes)

**What:** Compare declared field schema against real i5 behavior

**Example Contract:**
```
Program: PMTENT (Payment Entry)
Screen: Main Form
Expected Fields:
  - ACCOUNT_ID: length 10, COMP-3 (numeric)
  - AMOUNT: length 9, COMP-3 decimal (2 decimals)
  - DESCRIPTION: length 50, text

Check: Connect to PMTENT, verify fields exist with correct attributes
Alert If: Field renamed, length changed, type changed, or missing
```

**Detection Method:**
```java
// Pseudo-code: Schema contract
Schema expected = Schema.forProgram("PMTENT");
Session session = connect();
Screen actual = session.getScreen();

for (Field f : expected.fields()) {
  if (!actual.hasField(f.name, f.length, f.type)) {
    alert("Schema drift: " + f.name);  // Non-blocking alert
  }
}
```

#### B. Protocol Contracts (Every 30 seconds)

**What:** Verify TN5250E negotiation and stream handling

**Example Contract:**
```
Negotiation: Server must respond to NAWS (window size) request
Constraint: Timeout 5 seconds (dead i5 detection)
Alert If: Negotiation fails, hangs, or returns unexpected response
```

#### C. Execution Contracts (Every 30 seconds)

**What:** Verify operation ordering and atomicity

**Example Contract:**
```
Sequence: FILL → SUBMIT → ASSERT
Constraint: Keyboard must transition UNLOCKED → LOCKED → UNLOCKED
Alert If: Lock cycle doesn't complete, keyboard stuck locked, race detected
```

### Running Domain 2 Tests

Domain 2 is not run in CI (requires real i5). Instead, deploy as background job:

```bash
# Deploy continuous contracts (separate from CI)
# Requires: IBMI_HOST, IBMI_USER, IBMI_PASS env vars
./gradlew run:contracts

# View contract alerts (collected in monitoring system)
# E.g., CloudWatch, Datadog, Splunk, etc.
```

---

## Domain 3: Surface Tests (Critical Boundaries)

**Purpose:** Verify translation layer correctly handles screen/OIA responses.

**Characteristics:**
- Run per-commit (blocking gate)
- Real IBM i **or** mocked Screen5250 (tests use verifiers)
- Test three critical surfaces:
  1. Protocol round-trip (EBCDIC encoding, field parsing)
  2. Schema contract (field bounds, no truncation)
  3. Concurrency (operation ordering, idempotency)

### Test Classes & Coverage

| Test Class | Tests | Focus | Mock/Real |
|------------|-------|-------|-----------|
| `ProtocolRoundTripSurfaceTest` | 31 | Protocol translation, EBCDIC, decimal, cursors | Mock |
| `SchemaContractSurfaceTest` | 27 | Field boundaries, no silent truncation | Mock |
| `ConcurrencySurfaceTest` | 42 | Race conditions, operation ordering, 1000-session stress | Mock |
| Integration tests | 10 | Full workflow via mocked Session5250 | Mock |

**Total Domain 3: ~110 tests, 5-30 seconds per test**

### Running Domain 3 Tests

```bash
# All surface tests
./gradlew test --tests "*SurfaceTest"

# Specific surface test
./gradlew test --tests "ProtocolRoundTripSurfaceTest"

# With verbose output
./gradlew test --tests "*SurfaceTest" --info
```

### Domain 3 Examples

#### Example 1: Protocol Round-Trip (EBCDIC Integrity)

**Test Goal:** Verify EBCDIC encoding → transmission → decoding preserves data

**Test Code:**
```
Setup:
  - Screen5250 contains: "Hello World"
  - Field at position 0, length 11

Round-Trip:
  1. getText() → UTF-8 "Hello World"
  2. Encode → EBCDIC bytes
  3. Decode → UTF-8 "Hello World"
  4. Verify: Original == Decoded

Expected Failure Modes:
  - Truncation: "Hello Wor" (lost "ld")
  - Corruption: "Hello Wörld" (charset mismatch)
  - Position mismatch: Data at position 5 instead of 0
```

#### Example 2: Field Boundary Validation

**Test Goal:** Ensure no silent truncation when field is too small

**Test Code:**
```
Scenario: Form field expects amount, declared length 5

Tests:
  1. VALID: Submit "123.45" (5 chars) → accepted
  2. VALID: Submit "99.99" (5 chars) → accepted
  3. INVALID: Submit "1234.56" (7 chars) → error or truncation
     Assert: NOT silent truncation
            Error message explains boundary
            Artifact shows what happened
```

#### Example 3: Keyboard Lock Cycle (Concurrency)

**Test Goal:** Verify lock→unlock detection works under concurrency

**Test Code:**
```
Scenario: 1000 concurrent SUBMIT operations

Setup:
  - Each thread: sendKey(ENTER)
  - Wait for: lock (keyboard blocked)
  - Wait for: unlock (screen refresh)

Assertions:
  - All 1000 complete (no hangs)
  - Each thread observes lock→unlock
  - No race conditions (AtomicBoolean coordination)
  - Timeout < 5000ms per operation
```

---

## Domain 4: Scenario Tests (Business Workflows)

**Purpose:** Validate end-to-end workflows work correctly.

**Characteristics:**
- Run per-commit for critical paths, nightly for full suite
- Real IBM i **or** mocked verifiers
- Coverage: Payment processing, settlement batches, error recovery, stress

### Test Classes & Coverage

| Test Class | Tests | Focus | Example Workflow |
|------------|-------|-------|------------------|
| `PaymentProcessingScenarioTest` | 6 | Single-transaction payment flow | LOGIN → NAVIGATE → FILL amount → SUBMIT → ASSERT success |
| `SettlementBatchScenarioTest` | 7 | Multi-transaction batch with atomicity | Batch 10 transactions with verification |
| `ErrorRecoveryScenarioTest` | 9 | Failure modes and recovery paths | Timeout → retry → success |
| `StressScenarioTest` | 6 | 1000 concurrent sessions, 10K+ operations | Virtual thread pool performance baseline |

**Total Domain 4: ~28 tests, 10-60 seconds per test**

### Running Domain 4 Tests

```bash
# All scenario tests
./gradlew test --tests "*ScenarioTest"

# Critical scenarios only (fast subset)
./gradlew test --tests "*ScenarioTest" --exclude "*StressScenarioTest"

# Stress test (long-running, 5-10 minutes)
./gradlew test --tests "StressScenarioTest" --timeout=600000

# With detailed output
./gradlew test --tests "*ScenarioTest" --info
```

### Domain 4 Examples

#### Example 1: Payment Processing Workflow

**Test Goal:** Verify complete payment transaction end-to-end

**Test Code:**
```
Setup:
  - CSV data: account_id="ACC001", amount="150.00"

Workflow:
  1. LOGIN (credentials from env)
  2. NAVIGATE to "Payment Entry"
  3. FILL: account="ACC001", amount="150.00"
  4. SUBMIT: Press ENTER
  5. ASSERT: Screen contains "Transaction accepted"
  6. CAPTURE: Screenshot of confirmation

Assertions:
  - All 6 steps complete successfully
  - Artifacts contain ledger + screenshots
  - Transaction actually processed (if real i5)
  - No data truncation in amount field
```

#### Example 2: Error Recovery Workflow

**Test Goal:** Verify timeout recovery and retry logic

**Test Code:**
```
Scenario: Screen lock never releases (stuck i5)

Workflow:
  1. FILL field
  2. SUBMIT key → Start polling
  3. After 2000ms: No keyboard unlock (i5 hung)
  4. Timeout exception → Stop workflow
  5. Return error + artifacts (show stuck state)

Assertions:
  - Timeout triggers at ~5000ms (not infinite)
  - Exception includes OIA state (locked, why)
  - Artifacts show where failure occurred
  - Client can read error and retry
```

#### Example 3: Stress Testing (1000 Sessions)

**Test Goal:** Verify virtual thread scalability and no resource leaks

**Test Code:**
```
Setup: Executors.newVirtualThreadPerTaskExecutor() → 1000 threads

Workload:
  - Each thread: Execute payment workflow (LOGIN → SUBMIT)
  - Concurrent: All 1000 run simultaneously
  - Duration: 5-10 minutes

Metrics:
  - P50 latency: < 200ms
  - P99 latency: < 500ms
  - Throughput: > 5000 ops/sec
  - Memory: < 100MB total
  - No timeouts (all 1000 complete)

Assertions:
  - No OutOfMemoryError
  - No thread leaks
  - No hung sessions
  - Consistent latency (no degradation over time)
```

---

## Execution Cadence (CI/CD Integration)

### Pre-Commit (Local Developer)

```bash
./gradlew test --tests "*Test" \
  --exclude "*SurfaceTest" \
  --exclude "*ScenarioTest"

# Output: "53 tests passed in 4.2s"
# Gate: BLOCK push if failures
```

**Duration:** <5 seconds
**Risk:** Logic bugs only
**Action:** Fix before commit

---

### Pre-Push (Developer Branch)

```bash
./gradlew test --tests "*Test" -x "*StressScenarioTest"

# Output: "200 tests passed in 45s"
# Includes: Unit + Surface + Scenario (non-stress)
# Gate: BLOCK push if failures
```

**Duration:** <60 seconds
**Risk:** Logic bugs, boundary issues, workflow failures
**Action:** Fix before pushing to origin

---

### Pre-Merge (CI/CD Gate)

```bash
# All tests including stress
./gradlew test

# Output: "250 tests passed in 8m 30s"
# Includes: Unit + Surface + Scenario (all)
# Gate: BLOCK merge if failures
```

**Duration:** 5-10 minutes
**Risk:** All risks (logic, boundary, workflow, stress)
**Action:** Fix before merging to main

---

### Continuous (Background, 24/7)

```bash
# Domain 2 contracts (separate job)
./gradlew run:contracts

# Output: Alerts if schema/protocol drift
# Gate: Non-blocking (alerts only)
# Frequency: Every 5-30 minutes
```

**Duration:** Indefinite (background)
**Risk:** External system drift
**Action:** Alert SRE if contract breach

---

## Risk Mitigation Matrix

| Risk | Probability | Impact | Domain Detects | Frequency | Action |
|------|-------------|--------|-----------------|-----------|--------|
| Logic bug (EBCDIC codec) | High | High | Domain 1 | Pre-commit | BLOCK |
| Schema drift (i5 field changed) | Low | High | Domain 2 | 5 min | ALERT |
| Silent data loss (truncation) | Medium | Critical | Domain 3 | Per-commit | BLOCK |
| Field boundary mismatch | Medium | High | Domain 3 | Per-commit | BLOCK |
| Race condition (keyboard lock) | Low | High | Domain 3 | Per-commit | BLOCK |
| Workflow failure (wrong screen) | Medium | High | Domain 4 | Per-commit | BLOCK |
| Timeout hang | Low | High | Domain 4 | Per-commit | BLOCK |
| Memory leak (1000 sessions) | Low | High | Domain 4 | Nightly | BLOCK |

---

## Test Artifacts & Evidence

Each test run produces artifacts for debugging:

### Domain 1 (Unit Tests)
- JUnit XML summary
- Test output (failure messages + stack traces)
- Coverage report (line coverage %, uncovered lines)

### Domain 3 (Surface Tests)
- Screen snapshots (before/after operations)
- Field state dumps (position, attributes, value)
- Keystroke timeline (what was sent, when)
- Verifier output (what assertions checked)

### Domain 4 (Scenario Tests)
- Execution ledger (timestamped step log)
- Screenshots (one per step)
- Transaction records (if applicable)
- Performance metrics (latency histogram, throughput)

---

## Writing New Tests

### Pattern 1: Unit Test (Domain 1)

```java
@Test
public void ebcdicCodecDecodesHelloCorrectly() {
  byte[] ebcdic = { (byte)0xC8, (byte)0x85, (byte)0x93, (byte)0x93, (byte)0x96 };
  String result = EBCDICCodec.decode(ebcdic);
  assertEquals("Hello", result);
}
```

### Pattern 2: Surface Test with Mock (Domain 3)

```java
@Test
public void fillHandlerPopulatesAllFieldsWithoutTruncation() {
  // Setup mock Screen5250
  Screen5250 mockScreen = mock(Screen5250.class);
  when(mockScreen.getField("amount")).thenReturn(
    new ScreenField(name: "amount", length: 10, type: NUMERIC)
  );

  // Execute handler
  Map<String, String> fields = Map.of("amount", "12345.67");
  fillHandler.handle(fields, mockScreen);

  // Verify no truncation
  ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
  verify(mockScreen).setText(captor.capture());
  assertEquals("12345.67", captor.getValue());  // Full value, not "12345"
}
```

### Pattern 3: Scenario Test with Verifier (Domain 4)

```java
@Test
public void paymentProcessingWorkflowSucceeds() throws Exception {
  // Setup
  PaymentProcessingScenarioVerifier verifier = new PaymentProcessingScenarioVerifier();
  Session session = new MockSession(verifier);

  // Execute workflow
  WorkflowRunner runner = new WorkflowRunner(session);
  runner.execute(loadWorkflow("payment.yaml"), Map.of(
    "account_id", "ACC001",
    "amount", "150.00"
  ));

  // Verify results
  assertTrue(verifier.transactionProcessed());
  assertEquals("ACC001", verifier.getAccount());
  assertEquals(15000, verifier.getAmountCents());  // 150.00 * 100
}
```

---

## Troubleshooting Test Failures

### Failure: "Keyboard unlock timeout"

**Symptoms:** Test hangs for 30s, then fails with TimeoutException

**Root Cause:** Screen5250.getOIA() never returns `isKeyboardAvailable=true`

**Debugging:**
```
1. Check mock/verifier: does getOIA() return KeyboardUnlocked?
2. Check poll interval: is Thread.sleep(100) being called?
3. Check deadline: is timeout calculation correct?

Fix: Ensure verifier.getOIA().setKeyboardAvailable(true) in test setup
```

### Failure: "Screen did not contain: 'Payment Entry'"

**Symptoms:** AssertionException with screen dump

**Root Cause:** NAVIGATE handler found wrong screen

**Debugging:**
```
1. Check screen dump (included in exception)
2. Verify expected screen name matches actual i5 output
3. Check if keystroke sequence is correct for this i5

Fix: Update test data or keystroke sequence
```

### Failure: "Field truncation detected"

**Symptoms:** Domain 3 surface test fails

**Root Cause:** FILL handler silently truncated value

**Debugging:**
```
1. Check field length in schema
2. Check input value length in test data
3. Verify EBCDIC encoding (some characters expand)

Fix: Ensure field length accommodates input + error message
```

---

## Performance Baselines (Domain 4 Stress)

These metrics establish what "normal" looks like for optimization work:

| Operation | P50 Latency | P99 Latency | Throughput |
|-----------|-------------|-------------|-----------|
| Session connect | 500ms | 1000ms | 1-2 ops/sec |
| LOGIN handler | 2000ms | 5000ms | 0.2 ops/sec |
| NAVIGATE handler | 1000ms | 2000ms | 1 ops/sec |
| FILL handler (5 fields) | 500ms | 1500ms | 2 ops/sec |
| SUBMIT handler | 1500ms | 3000ms | 0.67 ops/sec |
| Full workflow (LOGIN→SUBMIT) | 6500ms | 12000ms | 0.15 ops/sec |
| 1000 concurrent sessions | 500ms P99 | (no 1000+ sessions currently) | >5000 ops/sec |

---

## Related Documentation

- [ARCHITECTURE.md](./ARCHITECTURE.md) — System design and workflow execution
- [CODING_STANDARDS.md](./CODING_STANDARDS.md) — Test patterns and conventions
- [README.md](./README.md) — Quick start and overview

---

**Document Version:** 1.0
**Last Updated:** February 8, 2026
**Phase Reference:** Phase 11 (Workflow Execution Handlers, commit cef8929)
