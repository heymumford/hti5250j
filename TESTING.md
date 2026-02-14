# Testing Framework & Strategy

HTI5250J uses an **inverted test pyramid** that prioritizes boundary correctness and real-world behavior over isolated unit tests. The framework is built on four test domains, each addressing distinct risks.

## The Four-Domain Model

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

| Test Class / Area | Focus |
|-------------------|-------|
| `encoding/` (CCSID*, EBCDICPairwise) | Character encoding (EBCDIC ↔ UTF-8), code page mapping |
| `contracts/` (Plugin, Session, Screen5250, Tnvt) | Interface contracts, plugin lifecycle, session state machine |
| `workflow/` (Validator, Schema, Handler, Runner) | Parameter substitution, schema validation, workflow execution |
| `framework/tn5250/ScreenFields*` | Field position, bounds, navigation |

### Running Domain 1 Tests

```bash
# All unit tests (excludes surface, scenario, and integration)
./gradlew test --tests "*Test" \
  --exclude "*SurfaceTest" \
  --exclude "*ScenarioTest" \
  --exclude "*IntegrationTest"

# Specific test class
./gradlew test --tests "EBCDICPairwiseTest"
```

---

## Domain 2: Continuous Contracts (Background Monitoring)

**Purpose:** Detect schema/protocol/execution drift in real-time without blocking development.

**Characteristics:**
- Requires real IBM i connection (configured via environment variables)
- Runs 24/7 in background (separate job, not in CI)
- Non-blocking (failures generate alerts, not test failures)
- Detects problems that Unit/Surface tests can't (external system changes)

### Three Contract Channels

| Channel | Frequency | What It Checks | Alert Condition |
|---------|-----------|-----------------|-----------------|
| **Schema** | Every 5 min | Field names, lengths, types match declared schema | Field renamed, length/type changed, missing |
| **Protocol** | Every 30 sec | TN5250E negotiation and stream handling | Negotiation fails, hangs, or unexpected response |
| **Execution** | Every 30 sec | Operation ordering and atomicity (FILL->SUBMIT->ASSERT) | Keyboard lock cycle incomplete, race detected |

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

**Domain 3 tests run in 5-30 seconds each.**

### Running Domain 3 Tests

```bash
# All surface tests
./gradlew test --tests "*SurfaceTest"

# Specific surface test
./gradlew test --tests "ProtocolRoundTripSurfaceTest"

# With verbose output
./gradlew test --tests "*SurfaceTest" --info
```

### Key Test Scenarios (Domain 3)

- **Protocol round-trip:** Verify EBCDIC encode -> transmit -> decode preserves data without truncation or charset corruption
- **Field boundary validation:** Ensure oversized input produces an error, never silent truncation
- **Keyboard lock cycle (concurrency):** 1000 concurrent SUBMIT operations all observe lock->unlock with no hangs or races

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

**Domain 4 tests run in 10-60 seconds each.**

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

### Key Test Scenarios (Domain 4)

- **Payment processing:** LOGIN -> NAVIGATE -> FILL -> SUBMIT -> ASSERT end-to-end with no data truncation
- **Error recovery:** Stuck keyboard lock triggers timeout at ~5s, returns diagnostic artifacts, allows client retry
- **Stress (1000 sessions):** Virtual thread pool with 1000 concurrent workflows; asserts no OOM, no thread leaks, no hung sessions

---

## Execution Cadence (CI/CD Integration)

| Gate | Command | Duration | Scope |
|------|---------|----------|-------|
| **Pre-commit** | `./gradlew test --tests "*Test" --exclude "*SurfaceTest" --exclude "*ScenarioTest"` | <5 sec | Unit only |
| **Pre-push** | `./gradlew test --tests "*Test" -x "*StressScenarioTest"` | <60 sec | Unit + Surface + Scenario (non-stress) |
| **Pre-merge** | `./gradlew test` | 5-10 min | All tests including stress |
| **Continuous** | `./gradlew run:contracts` | Background | Domain 2 contract monitoring (non-blocking alerts) |

---

## Test Artifacts

| Domain | Artifacts Produced |
|--------|--------------------|
| **Domain 1** | JUnit XML, failure stack traces, coverage report |
| **Domain 3** | Screen snapshots, field state dumps, keystroke timelines, verifier output |
| **Domain 4** | Execution ledger, per-step screenshots, performance metrics (latency, throughput) |

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

| Error Message | Likely Root Cause | Fix |
|---------------|-------------------|-----|
| "Keyboard unlock timeout" | Mock/verifier `getOIA()` never returns `isKeyboardAvailable=true` | Ensure `verifier.getOIA().setKeyboardAvailable(true)` in test setup |
| "Screen did not contain: '...'" | NAVIGATE handler reached wrong screen | Check screen dump in exception; verify keystroke sequence matches target i5 |
| "Field truncation detected" | FILL handler input exceeds declared field length | Check field length in schema vs. input length; EBCDIC encoding may expand characters |

---

## Performance Baselines

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

- [ARCHITECTURE.md](./ARCHITECTURE.md) -- System design and workflow execution
- [README.md](./README.md) -- Quick start and overview
