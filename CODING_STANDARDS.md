# HTI5250J Coding Standards

**Effective Date:** 2026-02-07
**Target Audience:** All contributors, with special focus on entry-grade Java engineers
**Language:** Java 17+ (cross-platform POSIX, target Temurin 21)

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

### Contract Tests (Before Refactoring)

Every public interface needs a contract test:

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

### Test Naming

```java
// ✅ Clear behavior (testMethod_Scenario_ExpectedOutcome)
public void testCopyText_RectangleSelection_ShouldReturnSelectedCharacters()

// ❌ Vague
public void testCopy1()

// ✅ Parametrized (data-driven)
@ParameterizedTest
@ValueSource(ints = {0, -1, Integer.MAX_VALUE})
public void testGetRow_EdgeCases_ShouldHandleWithoutCrash(int position)

// ❌ Multiple assertions (test one thing)
@Test
public void testScreenUpdate() {
    screen.update(data);
    assertTrue(screen.isDirty());       // ❌ Assertion 2
    assertEquals(10, screen.cursorPos()); // ❌ Assertion 3
}
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
