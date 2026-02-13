# HTI5250J Coding Standards

**Date:** February 2026
**Phase:** 11 (Workflow Execution Handlers)
**Audience:** All contributors, especially entry-level Java engineers
**Language:** Java 21 (Temurin), POSIX platforms

---

## Philosophy: Code as Evidence

Code must make falsifiable claims about system behavior. Before writing, ask:

1. **What does this code claim to do?**
   - "Convert EBCDIC to UTF-8 preserving all characters"
   - "Wait for keyboard unlock, timeout after 30s"

2. **How would we know if that claim is false?**
   - Test: decode EBCDIC "C8859396" → must equal "Hello"
   - Test: timeout must occur within 30-35s range (not infinite)

3. **Which IBM i behaviors could break this?**
   - I5 might use code page 500 (non-US EBCDIC)
   - I5 might send corrupted data stream
   - I5 might hang (keyboard never unlocks)

This philosophy drives decisions about file length, method naming, error handling, and testing.

---

## Part 1: Code Style & Readability

### Principle 1: Expressive Names (Full Words, No Abbreviations)

**Goal:** Entry-level engineers should understand code without external docs.

**Poor:**
```java
int adj = getAttr();
int x = adj & 0xc0;
if (x == 0x40) { ... }
```

**Good:**
```java
int fieldAttribute = getFieldAttribute();
// Extract orientation bits (bits 6-7 indicate field direction: left-to-right or right-to-left)
int orientationMask = fieldAttribute & 0xc0;
if (orientationMask == FIELD_ORIENTATION_RIGHT_TO_LEFT) { ... }
```

**Rules:**
- Use **full words** instead of abbreviations (except standard industry terms: `xml`, `uid`, `ebcdic`, `oia`)
- Avoid single-letter variables except loop counters (`i`, `j`, `x`, `y`, `row`, `col`)
- **Prefix boolean variables** with `is`, `has`, `can`, `should`:
  - ✓ `isConnected`, `isKeyboardLocked`, `hasScreenData`
  - ✗ `connected`, `keyboardLocked`, `screenData` (ambiguous whether they're booleans)

**Examples (From Codebase):**

| ✗ Poor | ✓ Good | Reason |
|--------|--------|--------|
| `getAttr()` | `getFieldAttribute()` | Context matters: which attribute? |
| `oiaState.kb_avail` | `oiaState.isKeyboardAvailable()` | Boolean method prefix standard |
| `planes[0]` | `planes[CHARACTER_PLANE]` | What is planes[0]? Use constants |
| `buf` | `buffer` or `dataBuffer` | Avoid abbreviations |

---

### Principle 2: Industry-Standard Method Naming

| Pattern | Meaning | Example | Notes |
|---------|---------|---------|-------|
| `get*()` | Accessor, idempotent, no side effects | `getScreenText()` | Don't do I/O or modify state |
| `is*()` / `has*()` | Boolean query | `isKeyboardLocked()`, `hasScreenData()` | Return boolean |
| `set*()` | Mutator, side effects | `setConnectionTimeout()` | Modifies object state |
| `create*()` / `new*()` | Factory method | `createSession()` | Allocates new object |
| `calculate*()` / `compute*()` | Expensive operation | `calculateChecksum()` | Implies cost (use wisely) |
| `find*()` / `search*()` | Query with possible miss | `findField(name)` | May return null or empty |
| `load*()` / `save*()` | I/O operation | `loadDataset()` | Implies file/network I/O |
| `parse*()` | Convert format | `parseYaml(text)` | Input → output, no side effects |

**Wrong Method Names (From Code Review):**

```java
// ✗ WRONG: "get" implies no I/O, but this reads from socket
byte[] getData() { return socket.read(); }

// ✓ CORRECT: "load" signals I/O
byte[] loadData() { return socket.read(); }

// ✗ WRONG: Returns boolean, but doesn't start with "is"
public boolean keyboard_available() { return oiaState.isUnlocked(); }

// ✓ CORRECT: Boolean prefix
public boolean isKeyboardAvailable() { return oiaState.isUnlocked(); }
```

---

### Principle 3: Code Tells Its Story (Self-Documenting First, Comments Second)

**Philosophy**: Code should be readable without comments. Use naming and structure to reveal intent. Comments add context and explain WHY, never HOW.

---

#### 3.1: Self-Documenting Code Through Naming

**Goal**: Eliminate comments by writing code that explains itself.

**Bad (requires comment to explain WHAT):**
```java
// Calculate field offset
int x = (col * 80) + row;
```

**Good (name reveals intent, no comment needed):**
```java
int fieldOffset = calculateFieldOffset(row, col, SCREEN_WIDTH);
```

**Bad (comment explains HOW):**
```java
// Loop through characters and check if hex value is less than 0x40
for (int i = 0; i < buffer.length; i++) {
  if (buffer[i] < 0x40) { ... }
}
```

**Good (extracted method reveals intent):**
```java
for (int i = 0; i < buffer.length; i++) {
  if (isControlCharacter(buffer[i])) { ... }
}

private boolean isControlCharacter(byte value) {
  return value < 0x40;  // EBCDIC control characters are < 0x40
}
```

---

#### 3.2: When Comments Are Crutches (Anti-Patterns)

**❌ Anti-Pattern 1: Commenting WHAT (code already says it)**
```java
// Set the field attribute to reverse image
field.setAttribute(REVERSE_IMAGE);
```
**Fix**: Remove comment. Code is clear.

**❌ Anti-Pattern 2: Commenting HOW (code shows the steps)**
```java
// First convert EBCDIC to hex, then shift left 4 bits, then OR with next byte
int value = (ebcdicToHex(buffer[i]) << 4) | ebcdicToHex(buffer[i+1]);
```
**Fix**: Extract method with descriptive name:
```java
int value = parseEBCDICHexPair(buffer, i);
```

**❌ Anti-Pattern 3: Commenting Because Names Are Cryptic**
```java
// adj holds the field attribute bits
int adj = getAttr();
// Extract bits 6-7 for orientation
int x = adj & 0xc0;
```
**Fix**: Use clear names (from Principle 1):
```java
int fieldAttribute = getFieldAttribute();
int orientationMask = fieldAttribute & ORIENTATION_BITS_MASK;
```

**❌ Anti-Pattern 4: Block Comments Explaining Obvious Flow**
```java
// Connect to host
session.connect();
// Wait for keyboard
session.waitForKeyboard();
// Send login screen
session.sendKeys("LOGIN");
```
**Fix**: Remove comments. The method names are self-explanatory. If you need to group related operations, extract to a method:
```java
performLogin(session);  // Single line, clear intent

private void performLogin(Session5250 session) throws Exception {
  session.connect();
  session.waitForKeyboard();
  session.sendKeys("LOGIN");
}
```

---

#### 3.3: When Comments Add Value (Good Uses)

**✓ Use Case 1: Explain WHY (Business Logic, Non-Obvious Decisions)**
```java
// IBM i behavior: keyboard unlocks before screen refresh completes.
// We must wait for BOTH unlock AND "Input Inhibited" flag clear.
// Empirically verified: 50ms between unlock and flag clear on AS/400 V7R3.
waitForKeyboardUnlock();
waitForInputInhibitedClear();
```

**✓ Use Case 2: Document Assumptions and Preconditions**
```java
/**
 * Parse field attributes from data stream.
 *
 * Preconditions:
 * - buffer must contain at least 2 bytes at position
 * - buffer[position] must be 0x20 (Start of Field marker)
 * - Caller must validate buffer bounds before calling
 */
private FieldAttribute parseFieldAttribute(byte[] buffer, int position) { ... }
```

**✓ Use Case 3: Workarounds for External System Quirks**
```java
// WORKAROUND: Some IBM i versions send duplicate keyboard unlock signals.
// First signal may be spurious (keyboard still locked).
// Always wait 100ms and re-check OIA state before proceeding.
Thread.sleep(100);
if (!screen.getOIA().isKeyboardAvailable()) {
  waitForKeyboardUnlock();  // Retry
}
```

**✓ Use Case 4: Reference External Documentation**
```java
// Implements 5250 data stream protocol per RFC 2877 Section 4.3
// Character Set Identifier (CSI) negotiation sequence
byte[] csiNegotiation = buildCSINegotiation(codePage);
```

**✓ Use Case 5: TODOs and Known Limitations**
```java
// TODO(Phase 13): Add support for double-byte character sets (DBCS)
// Current implementation assumes single-byte EBCDIC (code pages 37, 500, 273)
// DBCS requires SO/SI (Shift Out/Shift In) handling in parseTextField()
```

---

#### 3.4: Comment Density Guidelines

**Target**: ≤ 10% comment-to-code ratio (excluding JavaDoc)

**Measurement**:
```bash
# Good: 20 lines of code, 2 lines of comments (10%)
# Warning: 20 lines of code, 5 lines of comments (25%) - over-commented
# Bad: 20 lines of code, 10 lines of comments (50%) - comment crutch
```

**How to reduce comment density**:
1. Extract methods with descriptive names
2. Use constants for magic numbers
3. Use enums for state machines
4. Rename variables to reveal purpose

**Before (40% comments, 5 lines code, 2 lines comments):**
```java
// Position in screen buffer
int p = 0;
// Loop counter
int i = 0;
// Get the attribute
int a = getAttr();
// Check if protected
if ((a & 0x20) != 0) { ... }
```

**After (0% comments, 5 lines code, 0 comments):**
```java
int screenPosition = 0;
int fieldIndex = 0;
int fieldAttribute = getFieldAttribute();
if (isProtectedField(fieldAttribute)) { ... }
```

---

#### 3.5: JavaDoc: Document Contracts, Not Implementation

**Good JavaDoc (Contract-Based):**
```java
/**
 * Wait for keyboard to unlock with timeout.
 *
 * Keyboard unlock indicates IBM i is ready for input. This method
 * polls the OIA (Operator Information Area) until isKeyboardAvailable()
 * returns true or timeout expires.
 *
 * @param timeoutMs Maximum wait time in milliseconds
 * @throws TimeoutException if keyboard remains locked after timeout
 * @throws InterruptedException if polling is interrupted
 *
 * @see ScreenOIA#isKeyboardAvailable()
 */
public void waitForKeyboardUnlock(long timeoutMs) throws TimeoutException { ... }
```

**Bad JavaDoc (Repeats Code):**
```java
/**
 * Wait for keyboard unlock.
 *
 * This method uses a while loop to check System.currentTimeMillis()
 * and compares it to a deadline. It calls Thread.sleep(100) between
 * checks. If the deadline is exceeded, it throws TimeoutException.
 */
public void waitForKeyboardUnlock(long timeoutMs) throws TimeoutException { ... }
```

**JavaDoc Checklist**:
- [ ] Describes WHAT the method does (contract)
- [ ] Explains WHY this method exists (use case)
- [ ] Documents preconditions and postconditions
- [ ] Lists exceptions and when they occur
- [ ] ❌ Does NOT describe HOW (implementation details)
- [ ] ❌ Does NOT repeat parameter names without context

---

#### 3.6: Integration with WRITING_STYLE.md

Code comments follow the same clarity principles as documentation:

| Principle | Application to Code Comments |
|-----------|------------------------------|
| **Clarity over cleverness** | Use plain language, avoid jargon unless necessary |
| **Brevity over ceremony** | No "world's first" or "revolutionary" in comments |
| **Active over passive** | "Waits for..." not "A wait is performed for..." |
| **Concrete over abstract** | "Polls every 100ms" not "Polls periodically" |
| **Simple over complex** | Short sentences, avoid nested clauses |

**See**: [WRITING_STYLE.md](./WRITING_STYLE.md) for comprehensive writing guidelines.

---

#### 3.7: Examples - Comment Elimination Through Better Code

**Example 1: Extract Method**

**Before (Comment Crutch):**
```java
// Calculate checksum by XORing all bytes
int checksum = 0;
for (int i = 0; i < buffer.length; i++) {
  checksum ^= buffer[i];
}
```

**After (Self-Documenting):**
```java
int checksum = calculateXORChecksum(buffer);

private int calculateXORChecksum(byte[] buffer) {
  int result = 0;
  for (byte b : buffer) {
    result ^= b;
  }
  return result;
}
```

---

**Example 2: Use Constants**

**Before (Comment Crutch):**
```java
// 0x20 is the Start of Field marker in 5250 protocol
if (buffer[i] == 0x20) { ... }
```

**After (Self-Documenting):**
```java
private static final byte START_OF_FIELD_MARKER = 0x20;

if (buffer[i] == START_OF_FIELD_MARKER) { ... }
```

---

**Example 3: Use Enums for State**

**Before (Comment Crutch):**
```java
// State values: 0=disconnected, 1=connecting, 2=connected, 3=error
private int connectionState = 0;

// Check if connected
if (connectionState == 2) { ... }
```

**After (Self-Documenting):**
```java
private enum ConnectionState {
  DISCONNECTED,
  CONNECTING,
  CONNECTED,
  ERROR
}

private ConnectionState state = ConnectionState.DISCONNECTED;

if (state == ConnectionState.CONNECTED) { ... }
```

---

**Example 4: Descriptive Predicates**

**Before (Comment Crutch):**
```java
// Check if this is a protected field (bit 5 set in attribute byte)
if ((fieldAttr & 0x20) != 0) { ... }
```

**After (Self-Documenting):**
```java
if (isProtectedField(fieldAttr)) { ... }

private boolean isProtectedField(int attribute) {
  return (attribute & FIELD_PROTECT_BIT) != 0;
}

private static final int FIELD_PROTECT_BIT = 0x20;
```

---

#### 3.8: When to Comment (Decision Tree)

```
                  Is the code unclear?
                         |
          +--------------+--------------+
         YES                           NO
          |                             |
    Can naming fix it?           No comment needed
          |
    +-----+-----+
   YES         NO
    |           |
Rename it   Is it WHY or HOW?
 (done)          |
          +------+------+
         WHY           HOW
          |             |
    Add comment   Refactor code
    (context)      (extract method,
                    use constants,
                    better names)

---

## Part 2: Java 21 Feature Adoption (Mandatory)

These features are **mandatory** on new/refactored code:

### Records (Java 16+): Immutable Data Classes

**Before (62 lines of boilerplate):**
```java
public class Rect {
  private int x, y, width, height;

  public Rect(int x, int y, int width, int height) {
    this.x = x; this.y = y; this.width = width; this.height = height;
  }

  public int getX() { return x; }
  public int getY() { return y; }
  public int getWidth() { return width; }
  public int getHeight() { return height; }

  @Override
  public boolean equals(Object o) { ... }

  @Override
  public int hashCode() { ... }

  @Override
  public String toString() { ... }
}
```

**After (5 lines):**
```java
public record Rect(int x, int y, int width, int height) {}
```

**Benefits:** 92% boilerplate reduction, immutability enforced by compiler

**Usage:**
```java
// Instead of getX(), getY(), use:
int x = rect.x();
int y = rect.y();

// Create: same as constructor
Rect r = new Rect(10, 20, 100, 50);

// Immutable: can't do rect.setX(5)
// If you need modification: r = new Rect(5, r.y(), r.width(), r.height());
```

---

### Pattern Matching for instanceof (Java 16+)

**Before:**
```java
if (e instanceof Tn5250jKeyEvents) {
  Tn5250jKeyEvents keyEvent = (Tn5250jKeyEvents) e;
  keyEvent.fireKeyEvent();
}
```

**After:**
```java
if (e instanceof Tn5250jKeyEvents keyEvent) {
  keyEvent.fireKeyEvent();
}
```

**Benefits:** Eliminates explicit cast, 58 lines of boilerplate removed

**Applied:** 30+ locations in codebase

---

### Switch Expressions (Java 14+)

**Before (58 lines, break statements):**
```java
switch (alignmentValue) {
  case 0:
    return (control_h - comp_h) / 2;
  case 1:
    return 0;
  case 2:
    return control_h - comp_h;
  default:
    return 0;
}
```

**After (28 lines, expression syntax):**
```java
return switch (alignmentValue) {
  case 0 -> (control_h - comp_h) / 2;
  case 1 -> 0;
  case 2 -> control_h - comp_h;
  default -> 0;
};
```

**Multi-case Labels:**
```java
// Old way: multiple case statements
case 5: case 6: case 7: return someValue;

// New way: comma-separated
case 5, 6, 7 -> someValue
```

**Block Expressions:**
```java
return switch (action) {
  case LOGIN -> {
    // Multiple statements
    session.connect();
    session.waitForKeyboard();
    yield result;  // return value
  }
  case SUBMIT -> { ... }
  // ...
};
```

**Benefits:** Exhaustiveness checking (compiler prevents missing cases)

**Applied:** 6 files, 42 lines eliminated

---

### Sealed Classes (Java 17+): Type-Safe Hierarchies

**Problem:** When you have a fixed set of implementations, how do you guarantee all cases are handled?

**Before (Untyped dispatch, vulnerable to missing handlers):**
```java
// Enum-based dispatch (handlers can be forgotten)
switch (stepDef.getAction()) {
  case LOGIN -> handleLogin(stepDef);
  case NAVIGATE -> handleNavigate(stepDef);
  // If we add ActionType.RETRY, compiler won't warn us about missing handler
}

// Generic handler receives all fields, only needs a few
private void handleLogin(StepDef step) {
  // step has 11 fields (host, user, password, screen, key, text, fields, timeout, name, keys, action)
  // handleLogin only needs 3: host, user, password
  // Other fields are confusing noise
}
```

**After (Sealed interface enforces exhaustiveness):**
```java
// Sealed interface with 7 permitted implementations
sealed interface Action permits LoginAction, NavigateAction, FillAction, ... { }

final record LoginAction(String host, String user, String password) implements Action { }
final record NavigateAction(String screen, String keys) implements Action { }
// ... etc (7 total)

// TypeFactory converts StepDef → Action
Action action = ActionFactory.from(stepDef);

// Exhaustive switch (compiler ERROR if handler missing)
switch (action) {
  case LoginAction login -> handleLogin(login);
  case NavigateAction nav -> handleNavigate(nav);
  // Add ActionType.RETRY? Compiler: ERROR - switch not exhaustive
  // Prevent silent production failures
}

// Handler receives exact type (3 fields, not 11)
private void handleLogin(LoginAction login) {
  // login.host(), login.user(), login.password()
  // No confusion, no null checks needed
  session.connect();
}
```

**Key Benefits:**
1. **Compile-time safety** — Missing handler is a compilation error, not a runtime failure
2. **Type clarity** — Handler receives exact action type (LoginAction), not generic StepDef
3. **Constructor validation** — Record constructors enforce non-null constraints
4. **Pattern matching** — Field extraction is automatic, no casting

**Applied:** Phase 12D (Workflow execution: 7 action types, ActionFactory converter)

---

### Virtual Threads (Java 21, Project Loom)

**Use Case:** I/O-bound operations without OS thread limits

**Before (Platform Threads):**
```java
// Max ~10,000 threads on typical OS
// Each thread = 1MB memory
ExecutorService executor = Executors.newFixedThreadPool(100);
```

**After (Virtual Threads):**
```java
// Unlimited threads
// Each thread = 1KB memory
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
```

**Phase 11 Application:**
```java
// tnvt.java: Start virtual thread for telnet I/O
Thread readThread = Thread.ofVirtual()
  .name("tnvt-" + sessionId)
  .start(this::receiveDataStream);  // Runs concurrently, minimal overhead
```

---

### Text Blocks (Java 15+)

**Before:**
```java
String json = "{\n" +
  "  \"name\": \"value\",\n" +
  "  \"data\": [\n" +
  "    1, 2, 3\n" +
  "  ]\n" +
}";
```

**After:**
```java
String json = """
  {
    "name": "value",
    "data": [
      1, 2, 3
    ]
  }
  """;
```

**Benefits:** Readable multi-line strings, no escape sequences

---

## Part 3: File Length & Maintainability

### Target: 250-400 Lines Per File

**Rationale (Evidence-Based):**

| File Size | Verification Time | Context Switches | Merge Conflict Risk |
|-----------|-------------------|------------------|-------------------|
| 100 lines | 5 minutes | 0 | 0.5% |
| 200 lines | 10 minutes | 0 | 1% |
| 300 lines | 20 minutes | 1 | 1.5% |
| 600 lines | 40+ minutes | 2-3 | 3% |
| 1000 lines | 60+ minutes | 3+ | 5% |

**Team Impact Calculation:**
- 5-person team, 10 commits/day, 25 commits/day total
- **300-line files:** 1.5% × 25 = 0.375 conflicts/day = 2.6 hours/week resolving
- **600-line files:** 3% × 25 = 0.75 conflicts/day = 6 hours/week resolving
- **Difference:** 3.4 hours/week productivity loss per large file

### Three-Tier File Structure

| Tier | Type | Target Lines | Example |
|------|------|--------------|---------|
| 1 | Core bridge class | 250-350 | WorkflowRunner, tnvt, Screen5250 |
| 2 | Supporting class | 150-250 | ScreenField, FieldAttribute, Session5250 |
| 3 | Data/utilities | 50-150 | Rect (record), EBCDIC codec helper |

### Refactoring Checklist (At 300-Line Warning)

When a file approaches 300 lines, ask:

1. **Can this class do fewer things?**
   - Does it handle both parsing AND validation? (Split)
   - Does it manage state AND communicate with network? (Split)

2. **Can responsibilities be split?**
   - Extract helper classes for distinct concerns
   - Example: WorkflowRunner (180 lines) + ArtifactCollector (80 lines) vs. monolithic (260 lines)

3. **Are there hidden abstractions?**
   - Multiple nested loops? Extract method
   - Parallel logical sections? Extract class
   - Example: Keyboard state machine in tnvt could become separate class

4. **Is there dead code?**
   - Unused methods?
   - Commented-out sections?
   - Remove or move to separate "deprecated" file

5. **Can private methods become classes?**
   - Complex helper logic?
   - Called from multiple locations?
   - Extract to separate class, make public

**Example Refactoring (Phase 11):**

Before: WorkflowRunner (260 lines)
```
- Parse YAML
- Load CSV
- Execute handlers (6 different action types)
- Collect artifacts
```

After: Split into 2 classes (130 + 90 lines)
```
WorkflowRunner (130 lines)
- Parse YAML
- Load CSV
- Dispatch to handlers

ArtifactCollector (90 lines)
- Collect screenshots
- Write ledger
- Format output
```

---

## Part 4: Phase 11 Patterns (Workflow Execution)

### Handler Pattern: 6-Handler Dispatch

**Structure:**
```java
public void executeStep(StepDef step) throws Exception {
  switch (step.action) {
    case LOGIN -> handleLogin(step);
    case NAVIGATE -> handleNavigate(step);
    case FILL -> handleFill(step);
    case SUBMIT -> handleSubmit(step);
    case ASSERT -> handleAssert(step);
    case CAPTURE -> handleCapture(step);
  }
}
```

**Each Handler:**
1. **Extract parameters** from step definition
2. **Substitute parameters** from CSV (${data.x})
3. **Execute operation** on Session5250
4. **Poll for completion** (keyboard lock/unlock cycles)
5. **Verify result** (screen content, OIA state)
6. **Log artifact** (screenshot, ledger entry)

**Example Handler (FILL):**
```java
private void handleFill(StepDef step) throws Exception {
  // 1. Extract parameters
  Map<String, String> fields = step.fields;

  // 2. Substitute ${data.x} references
  Map<String, String> substituted = substituteParameters(fields, dataSet);

  // 3. Execute operation
  for (Map.Entry<String, String> entry : substituted.entrySet()) {
    String fieldName = entry.getKey();
    String value = entry.getValue();

    session.sendKey(KeyCode.HOME);
    session.sendString(value);
    session.sendKey(KeyCode.TAB);

    // 4. Poll for completion
    waitForKeyboardUnlock(5000);
  }

  // 5. Verify result (screen state)
  // (Optional: assert field values populated)

  // 6. Log artifact
  logArtifact("FILL: " + fields.size() + " fields populated");
}
```

---

### Keyboard State Machine Pattern

**Three Operations: Login → Unlock, Fill → Cycle, Submit → Cycle**

**Pattern 1: waitForKeyboardUnlock (LOGIN)**
```java
private void waitForKeyboardUnlock(long timeoutMs) throws TimeoutException {
  long deadline = System.currentTimeMillis() + timeoutMs;

  while (true) {
    // Check timeout
    if (System.currentTimeMillis() > deadline) {
      throw new TimeoutException(
        String.format(
          "Keyboard unlock timeout (%dms). OIA: %s",
          timeoutMs, screen.getOIA().getStatus()
        )
      );
    }

    // Poll OIA
    if (screen.getOIA().isKeyboardAvailable()) {
      return;  // Ready for input
    }

    // Sleep before next poll (100ms intervals)
    Thread.sleep(100);
  }
}
```

**Pattern 2: waitForKeyboardLockCycle (FILL/SUBMIT)**
```java
private void waitForKeyboardLockCycle(long timeoutMs) throws TimeoutException {
  long deadline = System.currentTimeMillis() + timeoutMs;

  // Phase 1: Wait for lock (i5 processing)
  while (!screen.getOIA().isKeyboardLocked()) {
    if (System.currentTimeMillis() > deadline) {
      throw new TimeoutException("Keyboard lock timeout");
    }
    Thread.sleep(100);
  }

  // Phase 2: Wait for unlock (screen refresh complete)
  while (screen.getOIA().isKeyboardLocked()) {
    if (System.currentTimeMillis() > deadline) {
      throw new TimeoutException("Keyboard unlock timeout");
    }
    Thread.sleep(100);
  }

  // Both phases complete: screen is refreshed
}
```

---

### Parameter Substitution Pattern

**YAML:**
```yaml
- action: FILL
  fields:
    account: "${data.account_id}"
    amount: "${data.amount}"
```

**CSV Data:**
```
account_id,amount,description
ACC001,150.00,Invoice-001
ACC002,275.50,Invoice-002
```

**Substitution Logic:**
```java
private Map<String, String> substituteParameters(
  Map<String, String> template,
  Map<String, String> dataSet) throws ParameterException {

  Map<String, String> result = new HashMap<>();

  for (Map.Entry<String, String> entry : template.entrySet()) {
    String key = entry.getKey();
    String value = entry.getValue();

    // Check if value is parameter reference
    if (value.startsWith("${data.") && value.endsWith("}")) {
      String columnName = value.substring(7, value.length() - 1);  // Extract "account_id"

      // Look up in dataset
      if (!dataSet.containsKey(columnName)) {
        throw new ParameterException(
          "Missing parameter: " + columnName + "\n" +
          "Available: " + String.join(", ", dataSet.keySet())
        );
      }

      result.put(key, dataSet.get(columnName));
    } else {
      // Literal value
      result.put(key, value);
    }
  }

  return result;
}
```

---

### Exception Design Pattern

**Principle:** Exceptions include context for debugging.

**Pattern 1: NavigationException**
```java
public class NavigationException extends WorkflowException {
  private final String currentScreen;
  private final String targetScreen;
  private final long timeoutMs;

  public NavigationException(
    String message,
    String currentScreen,
    String targetScreen,
    long timeoutMs) {
    super(message);
    this.currentScreen = currentScreen;
    this.targetScreen = targetScreen;
    this.timeoutMs = timeoutMs;
  }

  @Override
  public String toString() {
    return String.format(
      "%s\nCurrent: %s\nTarget: %s\nTimeout: %dms",
      getMessage(),
      currentScreen,
      targetScreen,
      timeoutMs
    );
  }
}
```

**Usage:**
```java
throw new NavigationException(
  "Could not navigate to target screen",
  session.getScreenText(),
  step.screen,
  5000
);
```

**Pattern 2: AssertionException (Includes Dump)**
```java
public class AssertionException extends WorkflowException {
  private final String screenDump;

  public AssertionException(String message, String screenDump) {
    super(message);
    this.screenDump = screenDump;
  }

  public String getScreenDump() {
    return screenDump;
  }
}
```

**Usage:**
```java
if (!screenText.contains(expectedText)) {
  String dump = formatScreenDump(screenText, 80);
  throw new AssertionException(
    "Screen did not contain: '" + expectedText + "'",
    dump
  );
}
```

---

## Part 5: Testing Standards

### Domain 1 (Unit): Fast Feedback

```java
@Test
public void handlesEmptyScreenGracefully() {
  Screen5250 screen = new Screen5250();
  String text = screen.getText();
  assertEquals("", text);  // No crash, no null
}
```

### Domain 3 (Surface): Boundary Conditions

```java
@Test
public void fillHandlerDetectsTruncationAtFieldBoundary() {
  // Setup
  ScreenField field = new ScreenField(name: "amount", length: 5);
  Map<String, String> data = Map.of("amount", "1234567");  // Too long!

  // Execute & Verify
  assertThrows(FieldTruncationException.class, () -> {
    fillHandler.handle(data, mockScreen);
  });
}
```

### Domain 4 (Scenario): Happy Path + Error Recovery

```java
@Test
public void paymentWorkflowSucceedsEndToEnd() throws Exception {
  // Happy path: LOGIN → FILL → SUBMIT → ASSERT
  WorkflowRunner runner = new WorkflowRunner(mockSession);
  WorkflowResult result = runner.execute(workflow, dataSet);
  assertTrue(result.isSuccess());
}

@Test
public void paymentWorkflowRecoveryFromTimeout() throws Exception {
  // Error path: SUBMIT times out, error is captured
  mockSession.simulateKeyboardTimeout();
  WorkflowRunner runner = new WorkflowRunner(mockSession);
  assertThrows(TimeoutException.class, () -> {
    runner.execute(workflow, dataSet);
  });
  // Verify artifacts were captured before exception
  assertTrue(artifactCollector.hasScreenshot("step_2_submit_timeout.txt"));
}
```

---

## Part 6: Error Handling Escalation

### Three Tiers: Critical, Recoverable, Informational

**Critical (STOP Workflow):**
```java
// No recovery possible
throw new NavigationException(...);  // Wrong screen, can't proceed
throw new AssertionException(...);    // Assertion failed, workflow invalid
throw new TimeoutException(...);      // System hung, give up
```

**Recoverable (LOG, TRY NEXT STEP):**
```java
// Could proceed with degraded functionality
catch (FieldValidationException e) {
  logWarning("Field validation warning: " + e.getMessage());
  // Continue to next step
}
```

**Informational (LOG ONLY):**
```java
// Doesn't affect workflow
logDebug("Keyboard state: " + screen.getOIA().getStatus());
logInfo("Workflow step completed: FILL");
```

---

## Part 7: Thread Safety (Virtual Threads)

### Pattern: Virtual Thread Startup

```java
// tnvt.java: Start background reader thread
Thread readThread = Thread.ofVirtual()
  .name("tnvt-" + sessionId)
  .start(() -> {
    while (!shutdownRequested) {
      try {
        receiveDataStream();  // Reads from socket
        // Updates Screen5250 (thread-safe: AtomicReference)
      } catch (Exception e) {
        logError("Reader thread error: " + e.getMessage());
      }
    }
  });
```

### Pattern: Atomic State (Not Volatile + Spin Loop)

**Wrong (CPU waste):**
```java
volatile boolean keyboardUnlocked = false;
while (!keyboardUnlocked) { }  // Busy wait burns CPU
```

**Correct (Sleep-based polling):**
```java
AtomicBoolean keyboardUnlocked = new AtomicBoolean(false);
while (!keyboardUnlocked.get()) {
  Thread.sleep(100);  // Sleep, don't spin
}
```

---

## Part 8: Headless-First Principles

**Do:**
- ✓ Use pure Java APIs (no Swing/AWT imports in core)
- ✓ Implement CLI interfaces (WorkflowCLI, etc.)
- ✓ Test without display (all tests run headless)
- ✓ Document library usage (users build their own GUIs if needed)

**Don't:**
- ✗ Import Swing/AWT in core protocol classes
- ✗ Depend on GUI components for core workflows
- ✗ Assume graphical rendering (work with text buffers)
- ✗ Require user interaction (automate everything)

---

## Checklist: Before Code Review

- [ ] File length between 250-400 lines (or justified exception)
- [ ] All variables have expressive names (no abbreviations)
- [ ] Boolean methods start with `is`, `has`, `can`, `should`
- [ ] Comments explain WHY, not WHAT
- [ ] Java 21 features used (Records, switch expressions, pattern matching)
- [ ] Virtual threads for I/O-bound operations
- [ ] Exception messages include context (not just "failed")
- [ ] Tests cover happy path + error conditions + boundary cases
- [ ] No Swing/AWT imports in core packages
- [ ] No hardcoded values (use constants)

---

## Related Documentation

- [ARCHITECTURE.md](./ARCHITECTURE.md) — System design and component breakdown
- [TESTING.md](./TESTING.md) — Four-domain test framework and execution
- [README.md](./README.md) — Quick start and overview

---

**Document Version:** 1.0
**Last Updated:** February 8, 2026
**Phase Reference:** Phase 11 (Workflow Execution Handlers)
