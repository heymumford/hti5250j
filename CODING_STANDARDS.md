# HTI5250J Coding Standards

**Effective Date:** 2026-02-07
**Target Audience:** All contributors, with special focus on entry-grade Java engineers
**Language:** Java 17+ (cross-platform POSIX, target Temurin 21)

---

## EPISTEMOLOGICAL FOUNDATION

### Code is Evidence, Not Art

The fundamental purpose of HTI5250J code is to provide **evidence about what the system actually does**.

A reader of HTI5250J code must be able to answer three questions:
1. **"What does this code claim to do?"** — Stated in variable names, method names, comments
2. **"How would I know if that claim is false?"** — Falsifiable against i5 behavior, testable
3. **"Could i5 behavior break this code?"** — Explicit assumptions documented, verifiable

If the code doesn't clearly answer these, it's not world-class.

### Explicitness is Virtue; Implicitness is Debt

Implicit behavior (framework magic, hidden conventions, undocumented assumptions) creates **knowledge debt**:
- Someone must remember WHY it works
- Someone must debug WHEN it breaks
- New engineers must reverse-engineer the intent

**Explicit code is self-documenting** — it contains its own explanation.

### The Three-Way Contract

HTI5250J code stands at the intersection of three realities:

```
┌──────────────────┐
│   Client Code    │  ← What the test automation expects
└────────┬─────────┘
         │ "What are you promising?"
┌────────▼──────────────────────┐
│   HTI5250J (Translation Layer) │  ← The bridge implementation
└────────┬──────────────────────┘
         │ "Is that what i5 actually does?"
┌────────▼──────────────────────┐
│  IBM i5 PMTENT/LNINQ/etc.    │  ← The concrete reality
└───────────────────────────────┘
```

**Code standards follow this structure:**
- **Client-facing code** must be semantic (high-level, intent-clear, business language)
- **Bridge code** must be explicit (low-level, assumption-visible, verifiable transformations)
- **i5-facing code** must be auditable (verifiable against actual i5 protocol behavior)

---

## Philosophy: Self-Documenting Code

Code must be coherent to an entry-grade Java engineer without external documentation. This means:

> **Code expresses WHAT and HOW. Comments explain WHY.**

### Principle 1: Expressive Variable Names

❌ **Poor:**
```java
int x = adj & 0xc0;
if (x == 0x40) {
    // ...
}
```

✅ **Good:**
```java
// Extract orientation bits (bits 6-7 indicate field direction)
int orientationMask = adj & 0xc0;
if (orientationMask == ORIENTATION_RIGHT_TO_LEFT) {
    // ...
}
```

**Rules:**
- Use **full words** instead of abbreviations (except standard industry terms like `uid`, `xml`)
- **Avoid single-letter variables** (except loop counters `i`, `j`, `x`, `y`)
- **Prefix boolean variables** with `is`, `has`, `can`, `should`:
  - `isConnected` ✓
  - `hasScreenData` ✓
  - `canDrawRubberBand` ✓

### Principle 2: Industry-Standard Patterns

**Method naming:**
- `get*()` — accessor (idempotent, no side effects)
- `is*()` / `has*()` — boolean query
- `create*()` / `new*()` — factory methods
- `calculate*()` / `compute*()` — expensive operations
- `find*()` / `search*()` — query with possible miss
- `load*()` / `save*()` — I/O operations

**Example:**
```java
// Good: Clear operation intent
public Rect getBoundingArea() { ... }           // accessor
public boolean isConnected() { ... }            // state query
public void loadStream(byte[] buffer) { ... }   // I/O operation
public void setBounds(int x, int y, ...) { ... } // mutator (imperative verb)
```

### Principle 3: Comments Explain Intent, Not Logic

❌ **Bad (repeats code):**
```java
// Increment counter
counter++;
```

✅ **Good (explains why):**
```java
// Move to next field in tab order (skip hidden fields)
counter++;
```

❌ **Bad (technical debt warning ignored):**
```java
// Get the screen character
char c = planes.getChar(pos);
```

✅ **Good (explains context):**
```java
// Fetch extended attributes for word-wrapping decision
// (getChar alone doesn't indicate 5250 semantics)
char c = planes.getChar(pos);
```

**When to comment:**
- ✅ WHY a non-obvious choice exists
- ✅ What assumptions must hold (preconditions)
- ✅ Workarounds for i5 quirks or protocol edge cases
- ✅ Cross-file dependencies or contract boundaries
- ❌ What the code literally does (reader can see that)
- ❌ Obvious operations (`buffer[i] = 0`)

---

## Java 16+ Feature Adoption

These features are **mandatory** on new/refactored code:

### Records (Java 16+)

**Use for:** Immutable data carriers (field + accessor only)

```java
// ✅ Correct: Records for coordinate containers
public record Rect(int x, int y, int width, int height) { }

// ❌ Incorrect: Record with methods/logic
public record Session(String name) {
    public void connect() { ... }  // Use class instead
}
```

**Migration pattern:**
```java
// Old: Mutable setter
dirtyScreen.setBounds(x1, x2, w, h);

// New: Immutable assignment (records don't mutate)
dirtyScreen = new Rect(x1, x2, w, h);
```

### Pattern Matching (Java 16+)

**Use for:** Type narrowing in conditionals

```java
// ✅ Pattern matching (Java 16+)
if (event instanceof KeyEvent keyEvent) {
    keyEvent.processKey();  // keyEvent is guaranteed to be KeyEvent
}

// ❌ Old style (don't use)
if (event instanceof KeyEvent) {
    ((KeyEvent) event).processKey();
}
```

### Switch Expressions (Java 14+)

**Use for:** Single-value or enum dispatch with exhaustiveness checking

```java
// ✅ Switch expression (returns value, no fall-through)
int fieldType = switch (fieldAttribute) {
    case 0x00 -> UNFORMATTED;      // Input field
    case 0x01 -> NUMERIC;          // Numeric field
    case 0x02 -> SIGNED;           // Signed numeric
    default -> throw new IllegalArgumentException(
        "Unknown field attribute: 0x" + Integer.toHexString(fieldAttribute));
};

// ❌ Traditional switch (mutable, fall-through risk)
int fieldType;
switch (fieldAttribute) {
    case 0x00:
        fieldType = UNFORMATTED;
        break;
    case 0x01:
        fieldType = NUMERIC;
        break;
    // ... more cases
    default:
        fieldType = -1;
}
```

**When NOT to use:**
- Multi-statement branches → use traditional switch or if-chain
- Side effects in case labels → traditional switch
- Exhaustiveness not critical → if-chain is clearer

### Sealed Classes (Java 17+)

**Use for:** Explicit type hierarchies in plugin systems

```java
// ✅ Sealed interface (only specific implementations allowed)
public sealed interface FieldAttribute permits
    UnformattedField,
    NumericField,
    SignedField { }

// Compiler guarantees: switch is exhaustive
public String describe(FieldAttribute attr) {
    return switch (attr) {
        case UnformattedField f -> "Input: " + f.label();
        case NumericField f -> "Numeric: " + f.maxDigits();
        case SignedField f -> "Signed: " + f.decimalPlaces();
    };
}
```

### Text Blocks (Java 15+)

**Use for:** Multi-line strings (SQL, JSON, XML)

```java
// ✅ Text block (readable, maintains formatting)
String schema = """
    CREATE TABLE sessions (
        id INT PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        connected BOOLEAN DEFAULT FALSE
    )
    """;

// ❌ String concatenation (hard to maintain)
String schema = "CREATE TABLE sessions (" +
    "id INT PRIMARY KEY," +
    "name VARCHAR(255) NOT NULL," +
    ...;
```

---

## Architectural Patterns

### Thread Safety

**Virtual Threads (Java 21+):**

Use `Thread.ofVirtual()` for I/O-bound operations:

```java
// ✅ Virtual thread for network I/O
Thread ioThread = Thread.ofVirtual()
    .name("tnvt-session-" + sessionId)
    .start(this);

// Rationale: 1-2 KB per virtual thread vs 1 MB per platform thread
// Enables 1000+ concurrent sessions without OS limits
```

**Immutable Data:**

Use records and final fields for concurrent access:

```java
// ✅ Immutable (safe across threads)
private final Rect dirtyScreen = new Rect(0, 0, 0, 0);

// ❌ Mutable (requires synchronization)
private final Rect dirtyScreen = new Rect();
dirtyScreen.setBounds(...);  // Race condition!
```

### Error Handling

**At System Boundaries:**

```java
// ✅ Validate at entry point (user input)
public void sendKeyStroke(String keystroke) throws InvalidKeyException {
    if (keystroke == null || keystroke.isEmpty()) {
        throw new InvalidKeyException("Keystroke must not be empty");
    }
    // ... process
}

// ❌ Validate in internal methods (trust internal contracts)
private void internalProcess(String key) {
    if (key == null) {  // Unnecessary (input pre-validated)
        throw new NullPointerException();
    }
}
```

**Logging:**

```java
// ✅ Log context and intent
log.warn("Failed to negotiate TIMING MARK for session {}, retrying in {} ms",
    sessionId, RETRY_DELAY_MS);

// ❌ Log vague errors
log.error("Error");

// ✅ Log stack traces only for unexpected errors
catch (IOException ioError) {
    log.error("Network error connecting to i5 on host {}: {}",
        hostName, ioError.getMessage(), ioError);  // Stack trace only if unexpected
}
```

---

## Code Organization

### Method Length

**Target:** ≤30 lines (including javadoc)
**Maximum:** ≤50 lines

If longer, extract helper methods:

```java
// ❌ Too long (67 lines of intertwined logic)
public void updateScreenFromDataStream(byte[] stream) {
    // Parse header
    // Validate checksums
    // Apply field attributes
    // Update dirty region
    // Notify listeners
    // Log metrics
}

// ✅ Decomposed (each method ≤30 lines)
public void updateScreenFromDataStream(byte[] stream) {
    byte[] validatedData = validateAndDecompress(stream);
    applyFieldUpdates(validatedData);
    notifyScreenChanged();
}

private byte[] validateAndDecompress(byte[] stream) { /* 15 lines */ }
private void applyFieldUpdates(byte[] data) { /* 25 lines */ }
```

### Package Organization

```
src/org/hti5250j/
├── framework/          # Core protocol & session management
│   ├── Session5250.java
│   ├── tnvt.java       # Telnet negotiation
│   └── tn5250/
│       ├── Screen5250.java    # Screen buffer & rendering
│       ├── ScreenField.java
│       └── DataStreamProducer.java  # I/O thread (virtual)
├── tools/              # Utilities (filters, logging, layouts)
├── interfaces/         # Public APIs (plugin system)
└── event/              # Event classes & listeners
```

---

## Testing Standards

### Philosophical Foundation: Falsification Over Validation

HTI5250J testing **inverts the traditional test pyramid** because traditional pyramids don't protect against silent data loss at system boundaries.

**Core Insight:** Code passes unit tests but fails on i5 because the three sources of truth diverge:
- **Schema Truth** — What fields the i5 actually has (drifts independently)
- **Protocol Truth** — How telnet actually negotiates (discoverable only on wire)
- **Execution Truth** — How i5 processes operations (found only in real workflows)

**Testing Strategy:** Test against i5 reality, not against mocks. Catch drift continuously.

### The Four-Domain Test Architecture

Tests are organized into **four distinct domains**, each addressing a specific risk:

#### Domain 1: Unit Tests (Isolation, No i5)

**Purpose:** Fast feedback on logic errors in translation layer

**Characteristics:**
- ✅ No i5 connection required
- ✅ Run in <5 seconds (pre-commit gate)
- ✅ Test internal invariants: EBCDIC codec, decimal serialization, queue ordering
- ✅ Use mocks for external dependencies

**Example:**
```java
// ✅ Unit: EBCDIC round-trip (internal logic)
@Test
public void testEBCDICCodec_RoundTrip_ShouldPreserveUnicode() {
    String original = "ABC123@#$";
    byte[] encoded = EBCDICCodec.encode(original);
    String decoded = EBCDICCodec.decode(encoded);
    assertEquals(original, decoded);  // No i5 involved
}

// ✅ Unit: Decimal serialization edge case
@Test
public void testDecimalSerializer_MaxValue_ShouldNotOverflow() {
    byte[] bytes = DecimalSerializer.toBytes(99999999.99, 10, 2);
    assertEquals(10, bytes.length);  // Stays in field bounds
}
```

**Acceptance Criteria:**
- ≥80% line coverage of translation layer
- All logic branches tested (positive, negative, boundary cases)
- No flaky tests (deterministic results)

#### Domain 2: Continuous Contracts (Reality Monitoring, Real i5)

**Purpose:** Detect schema/protocol/execution drift in real-time

**Characteristics:**
- ✅ Runs against **real i5** (production or test environment)
- ✅ Executes every 5-30 minutes, 24/7
- ✅ Non-blocking (failures don't interrupt service)
- ✅ Three contract channels: Schema, Protocol, Execution

**Example:**
```java
// ✅ Continuous: Schema contract (detect field divergence)
@Test
public void schemaContract_PMTENT_FieldsMatch() throws Exception {
    // Connect to real i5
    Session session = realI5.connect("PMTENT");

    // Query actual field layout
    Map<String, Integer> actualFields = session.getFieldMap();

    // Compare to declared schema
    SchemaDefinition expected = SchemaRegistry.get("PMTENT");

    // If mismatch detected, alert immediately (don't fail silently)
    for (FieldDefinition field : expected.getFields()) {
        assertTrue(actualFields.containsKey(field.name()),
            "Field drift detected: " + field.name() +
            " missing on i5 (schema changed?)");
    }
}

// ✅ Continuous: Protocol contract (detect negotiation failure)
@Test
public void protocolContract_TelnetNegotiation_MatchesExpectation() throws Exception {
    // Capture telnet negotiation bytes
    byte[] negotiation = captureWireData(realI5.connect());

    // Verify RFC 854 compliance
    assertTrue(verifyTelnetStructure(negotiation),
        "Protocol drift: Telnet negotiation doesn't match RFC 854");
}

// ✅ Continuous: Execution contract (detect operation ordering)
@Test
public void executionContract_FieldEntry_IsAtomic() throws Exception {
    // Submit field entry + screen refresh as two operations
    session.fieldEntry("FIELD1", "DATA");
    session.refresh();

    // Verify atomicity: no other session can read stale data
    assertFalse(otherSession.getField("FIELD1").isEmpty(),
        "Execution drift: Operation ordering broken");
}
```

**Execution Frequency:**
- Schema contracts: Every 5 minutes (schema rarely changes, but drift is catastrophic)
- Protocol contracts: Every 30 seconds (negotiation should be fast)
- Execution contracts: Every 30 seconds (operation ordering affects data consistency)

**Acceptance Criteria:**
- Zero schema drift detected
- Protocol successfully negotiates <500ms
- All operations maintain ordering guarantees

#### Domain 3: Surface Tests (Critical Boundaries, Real i5)

**Purpose:** Verify translation layer correctly handles i5 responses at system boundary

**Characteristics:**
- ✅ Runs on **real i5** for critical paths
- ✅ Executes at every commit (protocol) or every 2 hours (schema)
- ✅ Tests four critical surfaces: Protocol, Schema, Concurrency, Fallback

**Example:**
```java
// ✅ Surface: Protocol round-trip (semantic → bytes → semantic)
@Test
public void surfaceTest_ProtocolRoundTrip_PaymentProcessing() throws Exception {
    // Start with user intent (semantic)
    PaymentOperation intent = new PaymentOperation(
        "ACCOUNT", "USD 100.00", "AUTHORIZE");

    // Translate to telnet bytes (protocol)
    byte[] telnetFrame = ProtocolTranslator.encode(intent);

    // Send to real i5, get response
    byte[] response = realI5.execute(telnetFrame);

    // Translate back to semantic
    PaymentResult result = ProtocolTranslator.decode(response);

    // Verify: semantic input → protocol → semantic output are consistent
    assertEquals("AUTHORIZED", result.status);
    assertEquals("100.00", result.amount);
}

// ✅ Surface: Schema contract boundary (verify field bounds)
@Test
public void surfaceTest_FieldBounds_PreventDataLoss() throws Exception {
    Session session = realI5.connect("PRICING");

    // Get declared schema
    ScreenField priceField = session.getField("PRICE");
    int maxLength = priceField.getLength();

    // Try to write longer value (should truncate, not corrupt)
    String longPrice = "999999999.99";  // Longer than field
    session.fieldEntry("PRICE", longPrice);

    // Verify: data fits within bounds
    String readBack = session.getField("PRICE").getText();
    assertTrue(readBack.length() <= maxLength,
        "Data loss: " + longPrice + " doesn't fit in field");
}

// ✅ Surface: Concurrency (operation ordering, idempotency)
@Test
public void surfaceTest_ConcurrentFieldEntry_IsSerialized() throws Exception {
    // Two sessions try to modify same field concurrently
    session1.fieldEntry("SHARED_FIELD", "VALUE_1");
    session2.fieldEntry("SHARED_FIELD", "VALUE_2");

    // Verify: operations executed in order, no interleaving
    String finalValue = session1.getField("SHARED_FIELD").getText();
    assertTrue(finalValue.equals("VALUE_1") || finalValue.equals("VALUE_2"),
        "Concurrency failure: interleaved operation detected");
}

// ✅ Surface: Fallback paths (what if protocol negotiation fails?)
@Test
public void surfaceTest_FallbackProtocol_WorksWhenNegotiationFails() throws Exception {
    // Block telnet negotiation options
    mockI5.blockTelnetOption(TIMING_MARK);

    // Session should fall back to no-negotiation mode
    Session session = new Session();
    session.setFallbackMode(true);
    session.connect();

    // Verify: still works, just without timing marks
    assertTrue(session.isConnected());
}
```

**Execution Frequency:**
- Protocol surface tests: Every commit (protocol is stable)
- Schema surface tests: Every 2 hours (schema might drift slowly)
- Concurrency surface tests: Every commit (ordering is critical)
- Fallback surface tests: Every 4 hours (fallbacks rarely tested)

**Acceptance Criteria:**
- Round-trip protocol tests pass (semantic → bytes → semantic preserves intent)
- Schema bounds verified (no truncation, no overflow)
- Concurrent operations maintain ordering
- Fallback paths work when primary fails

#### Domain 4: Scenario Tests (Real Workflows, Real i5)

**Purpose:** Verify complete user workflows end-to-end against real i5

**Characteristics:**
- ✅ Runs against **real i5** (production or test environment)
- ✅ Executes every commit or nightly (user-visible workflows)
- ✅ Tests complete business scenarios: payment processing, settlement, error recovery

**Example:**
```java
// ✅ Scenario: Payment processing workflow (complete path)
@Test
public void scenarioTest_PaymentProcessing_EndToEnd() throws Exception {
    Session session = realI5.connect("PAYMENTS");

    // User opens payment screen
    assertEquals("ENTER PAYMENT DETAILS", session.getTitle());

    // User enters account number
    session.fieldEntry("ACCOUNT", "123456789");
    session.fieldEntry("AMOUNT", "1000.00");
    session.sendKey(F5);  // Submit

    // i5 validates, shows confirmation
    Screen result = session.refreshScreen();
    assertEquals("CONFIRM PAYMENT", result.getTitle());

    // User confirms (F1)
    session.sendKey(F1);

    // i5 returns to main menu (success)
    Screen final_screen = session.refreshScreen();
    assertEquals("MAIN MENU", final_screen.getTitle());
}

// ✅ Scenario: Error recovery (payment declined, retry)
@Test
public void scenarioTest_PaymentRetryAfterDecline_ShouldShowRetryPrompt()
        throws Exception {
    Session session = realI5.connect("PAYMENTS");

    // Try payment with invalid card
    session.fieldEntry("ACCOUNT", "INVALID");
    session.fieldEntry("AMOUNT", "100.00");
    session.sendKey(F5);

    // i5 shows error
    Screen error = session.refreshScreen();
    assertTrue(error.getText().contains("INVALID ACCOUNT"),
        "Error message missing");

    // User retries with valid account
    session.fieldEntry("ACCOUNT", "123456789");
    session.sendKey(F5);

    // Should succeed
    Screen success = session.refreshScreen();
    assertEquals("MAIN MENU", success.getTitle());
}

// ✅ Scenario: Batch settlement (complex multi-step operation)
@Test
public void scenarioTest_BatchSettlement_ProcessesAllTransactions()
        throws Exception {
    Session session = realI5.connect("SETTLEMENT");

    // Select batch job
    session.fieldEntry("BATCH_ID", "BATCH_20260207");
    session.sendKey(F5);

    // i5 shows progress
    for (int i = 0; i < 100; i++) {
        Screen progress = session.refreshScreen();
        String status = progress.getField("PROGRESS").getText();
        assertTrue(Integer.parseInt(status) <= 100,
            "Progress out of bounds: " + status);
    }

    // Final confirmation
    Screen final_screen = session.refreshScreen();
    assertTrue(final_screen.getText().contains("COMPLETE"),
        "Batch didn't complete");
}
```

**Execution Frequency:**
- Commit scenarios: Every push (critical workflows)
- Nightly scenarios: Full suite every 24 hours
- Weekly scenarios: Load tests, stress tests

**Acceptance Criteria:**
- All business workflows complete successfully
- Error paths show appropriate messages
- Retry/recovery paths work
- Complex multi-step operations succeed

### Test Contract Naming Conventions

```java
// ✅ Clear naming: Domain + Focus + Scenario + Expected
@Test
public void unitTest_EBCDICCodec_RoundTrip_ShouldPreserveCharacters()

@Test
public void continuousTest_SchemaContract_PMTENT_FieldsMatch()

@Test
public void surfaceTest_ProtocolRoundTrip_PaymentProcessing_ShouldPreserveSemantic()

@Test
public void scenarioTest_PaymentProcessing_EndToEnd_ShouldCompleteSuccessfully()

// ❌ Vague naming
@Test
public void testCodec()

@Test
public void testPayment()
```

### Test Execution Strategy

| Phase | Tests | Duration | When | i5 Required |
|-------|-------|----------|------|------------|
| **Pre-commit** | Unit only | <5s | Before git add | NO |
| **Pre-push** | Unit + Surface | ~60s | Before git push | YES (critical paths) |
| **Pre-merge** | All 4 domains | 5-10 min | Before main merge | YES (full test) |
| **Production** | Continuous monitors | 24/7 | Background | YES (real i5) |

### Risk Mitigation Matrix

| Risk | Detected By | Frequency | Alert Level |
|------|------------|-----------|------------|
| **Logic bug** (wrong EBCDIC) | Domain 1 (Unit) | Pre-commit | BLOCK |
| **Schema drift** (i5 field missing) | Domain 2 (Continuous) | Every 5 min | WARN (24 hours) |
| **Protocol mismatch** (negotiation fails) | Domain 2 + 3 (Continuous + Surface) | Every 30 sec | ALERT |
| **Idempotency failure** (concurrent ops corrupt) | Domain 3 (Surface) | Per commit | BLOCK |
| **Silent data loss** (field truncation) | Domain 3 (Surface) | Per commit | BLOCK |
| **Latency SLA violation** (operations too slow) | Domain 4 (Scenario) | Nightly | WARN (1 hour) |

### Contract Tests (Foundational)

Every public interface has a contract test that establishes behavioral guarantees **before** refactoring:

```java
// ✅ Contract test (establishes behavioral guarantee)
@Test
public void testConnect_IdempotentSecondCall_ShouldNotRaiseException() {
    session.connect();
    session.connect();  // Second call must be safe (no double-connect)
    assertTrue(session.isConnected());
}

// ❌ Unit test (isolation theater, misses real bugs at boundaries)
@Test
public void testConnect_ShouldSetConnectedFlag() {
    // This passes with mock, breaks with real socket
}
```

### Test Ownership and Responsibilities

| Domain | Owner | Frequency | Environment | Tool |
|--------|-------|-----------|-------------|------|
| **Domain 1 (Unit)** | Developer | Per commit | Local | JUnit 5 |
| **Domain 2 (Continuous)** | Platform/SRE | Every 5-30 min | Background | JUnit 5 + i5 connector |
| **Domain 3 (Surface)** | QA + Dev | Per commit | CI/CD | JUnit 5 + i5 test env |
| **Domain 4 (Scenario)** | QA | Per commit (critical) / nightly | CI/CD + production | JUnit 5 + i5 production |

### Test Metrics Dashboard

Track confidence metrics across all domains:

```
Domain 1 (Unit):           Code Logic Confidence
  - Coverage: 82% lines
  - Passing: 487/487 tests
  - Status: ✅ GREEN

Domain 2 (Continuous):     Reality Match Confidence
  - Schema drift: 0 detected
  - Protocol failures: 0 in 24h
  - Status: ✅ GREEN

Domain 3 (Surface):        Interface Confidence
  - Protocol round-trips: 98% success
  - Schema bounds: verified
  - Concurrency: ordering maintained
  - Status: ✅ GREEN

Domain 4 (Scenario):       Workflow Confidence
  - Payment processing: 100% (50 runs)
  - Error recovery: 100% (25 runs)
  - Settlement batch: 100% (10 runs)
  - Status: ✅ GREEN
```

---

## Performance & Scalability

### Concurrency at Scale

```java
// ✅ Virtual threads for unlimited concurrency
ExecutorService sessionPool = Executors.newVirtualThreadPerTaskExecutor();

for (int i = 0; i < 10000; i++) {
    sessionPool.submit(() -> simulateSession(i));  // All 10K run concurrently
}

// ❌ Platform threads (OS-limited, ~10K total)
ExecutorService threadPool = Executors.newFixedThreadPool(100);
// Can't create 10K threads on any modern OS
```

### Immutability for Performance

```java
// ✅ Immutable (no synchronization needed)
private final Rect dirtyScreen = new Rect(0, 0, 0, 0);
dirtyScreen = new Rect(x, y, w, h);  // Assignment is atomic

// ❌ Mutable (requires locks)
private final Rect dirtyScreen = new Rect();
synchronized void updateBounds(...) {
    dirtyScreen.setBounds(...);  // Serialize all access
}
```

---

## Headless Architecture Notes

This codebase targets a **headless environment** (no GUI):

- GUI components (Swing, AWT) may be deprecated
- Focus on telnet protocol, screen buffer, and I/O
- Virtual threads enable massive concurrent i5 sessions
- No event dispatch bottlenecks (runs on server)

**Example of architectural intent:**
```java
// ✅ Headless-friendly: Core screen model
public class Screen5250 {
    private final Rect dirtyScreen;  // Immutable, queryable
    public Rect getDirtyRegion() { return dirtyScreen; }
}

// ❌ GUI-dependent: Swing component
public class SessionPanel extends JPanel {
    // Violates headless arch
}
```

---

## File Length Standards

### The Problem: Cognitive Load vs. Merge Conflicts

Large files (1000+ lines) suffer from:
- **Merge conflicts** — Multiple developers editing same file = constant conflicts
- **Hard to understand** — Holding 1000 lines in working memory is impossible
- **Hard to test** — Too many responsibilities, can't verify one thing
- **Hard to review** — PR changes span 100+ lines, reviewers miss issues
- **Hard to modify** — Change at line 250, break something at line 850

### The Standards

| Metric | Target | Rationale |
|--------|--------|-----------|
| **Optimal** | 250-350 lines | Fits one mental model; verifiable in one sitting |
| **Warning** | 300 lines | Time to ask: "Should this be split?" |
| **Maximum** | 400 lines | Hard limit; merge conflict risk still acceptable |
| **Critical** | 600 lines | Must refactor or document exception |
| **Emergency** | 1000+ lines | Refactor immediately (likely contains bugs) |

### Why 300-400 Lines?

**Test:** Can a new engineer verify the file's claims in one sitting (20 minutes)?

✅ **GOOD — 280 lines, verifiable**
```java
public class SchemaRegistry {
    /**
     * ONE clear responsibility: "Know what fields PMTENT expects"
     * Verification time: ~15 minutes
     * - Read schema() method (15 lines)
     * - Read register() method (20 lines)
     * - Read validate() method (30 lines)
     * - Read test alongside file
     * - Understand: "SchemaRegistry is correct"
     */
    // ... 280 lines total
}
```

❌ **BAD — 1200 lines, not verifiable**
```java
public class TerminalAutomationRuntimeMonolith {
    // Lines 1-50: Constructor
    // Lines 51-150: executeProgram()
    // Lines 151-250: Protocol encoding
    // Lines 251-350: Schema validation
    // Lines 351-450: Queue management
    // ... 11 more sections
    // Can you verify this in one sitting? NO (2+ hours)
}
```

### Merge Conflict Math

Probability of merge conflicts increases with file size:

| File Size | Conflict Prob | Weekly Impact |
|-----------|--------------|---------------|
| 100 lines | 2% | <30 min/week |
| 300 lines | 6% | ~1 hour/week |
| 600 lines | 12% | ~3 hours/week |
| 1000 lines | 20% | 5-6 hours/week |

**Standard:** Keep files ≤400 lines to maintain merge conflict overhead < 2 hours/week.

### The Three-Tier Structure

**Tier 1: Core Bridge Classes (250-350 lines)**
- `TerminalAutomationRuntime.java` (280 lines)
- `OperationQueue.java` (310 lines)
- `SchemaRegistry.java` (290 lines)
- `TN5250Protocol.java` (320 lines)

These are HTI5250J's heart. One clear responsibility each. Verifiable in a single sitting.

**Tier 2: Supporting Classes (150-250 lines)**
- `FieldEncoder.java` (180 lines)
- `EBCDICCodec.java` (220 lines)
- `SchemaValidator.java` (210 lines)

Helper classes. Focused, supporting core functionality.

**Tier 3: Data Classes & Utilities (50-150 lines)**
- `SchemaDefinition.java` (95 lines)
- `FieldDefinition.java` (87 lines)
- `ExecutionResult.java` (105 lines)

Data carriers, exceptions, small utilities.

### When a File Approaches 300 Lines: The Checklist

Ask these questions in order:

1. **Does this file have exactly ONE reason to change?**
   - YES → Keep it (up to 400 lines is fine)
   - NO → Split immediately (multiple responsibilities)

2. **Can a new engineer understand it in 20 minutes?**
   - YES → Keep it (good design)
   - NO → Simplify or split

3. **Does it need context from other files to verify?**
   - YES → Refactor (move related code together)
   - NO → Keep it (self-contained)

4. **How many public methods?**
   - 1-3 → Good size (keep it)
   - 4-6 → Watch it (split at 350 lines)
   - 7+ → Split immediately (too many responsibilities)

5. **How many test files verify this class?**
   - 1 → Good (cohesive)
   - 2+ → Split (concerns are separate)

### Acceptable Exceptions (Documented)

Rarely, a file may exceed 400 lines if:
- ONE clear, complex responsibility (cannot be cleanly split)
- Well-documented with section comments every 50 lines
- Full test suite alongside the file
- Exception documented in header comment

Example:
```java
/**
 * WHY THIS FILE IS LARGE (520 lines):
 * SchemaDriftDetector runs continuous verification against i5.
 * It has multiple detection channels (field positions, enum values, type limits,
 * performance profiles) that are too interdependent to split.
 *
 * WHEN TO SPLIT: If exceeds 600 lines, refactor into:
 * - SchemaDriftDetector (orchestrator, ~200 lines)
 * - FieldDriftDetector (specific checks, ~150 lines)
 * - EnumDriftDetector (specific checks, ~130 lines)
 * - PerformanceDriftDetector (specific checks, ~120 lines)
 */
public class SchemaDriftDetector {
    // Complex logic documented in sections...
}
```

---

## The Covenant: Core Standards at a Glance

| Standard | Purpose | Example |
|----------|---------|---------|
| **Minimal Code** | Clarity over cleverness | One function, one job |
| **Entry-Level Readable** | Reduce knowledge debt | Explicit naming, no abbreviations |
| **Expressive Where Critical** | Data loss prevention | Long names in PMTENT (payment system) |
| **Explicit Over Implicit** | Auditable assumptions | Comments on WHY, not WHAT |
| **Three-Level Bridge** | Clear responsibility | client → runtime → protocol |
| **Why Comments** | Future maintainer understanding | Explain design decisions |
| **Fail Loud** | Debugging ease | Detailed exception messages |
| **Testable by Design** | Quality-first architecture | Dependency injection everywhere |
| **Performance Comments** | Expectation setting | Document SLA, don't assume |
| **File Length ≤400** | Merge conflict prevention | Verifiable in one sitting |

### HTI5250J Coding Covenant

"I will write code that:
- Is **verifiable against i5 reality** (epistemology)
- **Explicitly shows what it claims and why** (ontology)
- Can be **understood by the person fixing bugs at 3am**
- **Fails loudly with evidence**, not silently
- **Prioritizes clarity over cleverness**
- **Documents assumptions**, not obvious facts
- **Is tested as an equal partner** to implementation
- **Respects the reader's time and intelligence**

I will not write code that:
- Requires **framework magic** to understand
- Hides **complexity in abbreviations or conventions**
- Prioritizes **my ego over team understanding**
- **Silently fails** or obscures error context
- **Optimizes prematurely**
- Treats **testing as an afterthought**
- Comments on **what code obviously does**"

This philosophy transforms HTI5250J from "a useful tool" into "a teaching artifact" — code that future engineers don't just use, but learn from.

---

## Refactoring Checklist

Before committing code changes:

- [ ] **Naming**: Variables/methods clearly express intent
- [ ] **Comments**: Explain WHY (not WHAT)
- [ ] **Tests**: Contract tests establish guarantees
- [ ] **Immutability**: Data classes are records or final
- [ ] **Java 16+**: Pattern matching, switch expressions, records
- [ ] **Error handling**: Validates at boundaries, logs context
- [ ] **Performance**: Virtual threads for I/O, no synchronization on immutables
- [ ] **Coherence**: Entry-grade engineer can read without external docs
- [ ] **File Length**: ≤400 lines (optimal 250-350), one clear responsibility

---

## References

- **Java Language Features**: [JEP Timeline](https://openjdk.org/jeps/0)
- **Virtual Threads**: [JEP 440](https://openjdk.org/jeps/440)
- **Records**: [JEP 395](https://openjdk.org/jeps/395)
- **Pattern Matching**: [JEP 405](https://openjdk.org/jeps/405)
- **Switch Expressions**: [JEP 361](https://openjdk.org/jeps/361)
- **Sealed Classes**: [JEP 409](https://openjdk.org/jeps/409)

---

## Enforcement

- All new files **must** adhere to these standards
- Refactored files **should** adopt standards incrementally
- Code reviews **will** check for compliance
- Violations documented in PR feedback (no blocking)

**Last Updated:** 2026-02-07
**Next Review:** After Phase 6 (Test Infrastructure Modernization)
