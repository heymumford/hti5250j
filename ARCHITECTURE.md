# HTI5250J Architecture

**Date:** February 2026
**Phase:** 11 (Workflow Execution Handlers)
**Audience:** Contributors, integrators, and users seeking to understand system design

## Executive Summary

HTI5250J is a headless 5250 terminal emulator for IBM i (AS/400) systems. The architecture follows a three-tier model: clients (CLI, tests, libraries) invoke the Java API, which translates semantic workflow commands into TN5250E protocol operations, which are transmitted to the IBM i system.

This document describes the system using the C4 model: system context (C1), container diagram (C2), component breakdown (C3), and code-level detail (C4).

---

## C1: System Context

HTI5250J sits at the boundary between client automation (test frameworks, CLI tools, library consumers) and IBM i systems communicating via TN5250E protocol.

```
┌─────────────────────────────────────────────────────────┐
│              Client Layer                               │
│  ┌───────────────────────────────────────────────────┐  │
│  │  CLI (i5250 command)                              │  │
│  │  Test Framework (JUnit, assertion libraries)      │  │
│  │  Library Consumer (custom Java applications)      │  │
│  └───────────────────────────────────────────────────┘  │
└────────────┬──────────────────────────────────────────┘
             │ Java API (Session, Workflow, Screen)
┌────────────▼──────────────────────────────────────────┐
│          HTI5250J System Boundary                      │
│  ┌────────────────────────────────────────────────┐  │
│  │  Workflow Engine (validation, execution)       │  │
│  │  Session Management (5250 terminal lifecycle)  │  │
│  │  Screen Emulation (display buffer + state)     │  │
│  │  Protocol Handlers (TN5250E translation)       │  │
│  │  Plugin System (extensibility points)          │  │
│  └────────────────────────────────────────────────┘  │
└────────────┬──────────────────────────────────────────┘
             │ TN5250E Protocol (TCP, port 23 or 992)
┌────────────▼──────────────────────────────────────────┐
│          IBM i (AS/400) System                        │
│  ┌────────────────────────────────────────────────┐  │
│  │  Program: PMTENT (payment entry)               │  │
│  │  Program: LNINQ (line inquiry)                 │  │
│  │  Display: 5-field form + OIA status            │  │
│  └────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

**Key Relationships:**

| Actor | Relationship | Protocol |
|-------|-------------|----------|
| Client Code | Invokes HTI5250J | Java API (blocking calls) |
| HTI5250J | Communicates with IBM i | TN5250E (async streams + polling) |
| IBM i | Responds with screens | EBCDIC encoded, variable-length records |

**Data Flows:**

1. **Request:** Client calls `session.sendString("WRKSYSVAL")` → Java → EBCDIC bytes → TN5250E → IBM i
2. **Response:** IBM i sends screen update → EBCDIC → TN5250E → Screen5250 buffer → Client reads `session.getScreenText()`
3. **Polling:** Client may call `session.waitForKeyboard()` → HTI5250J polls OIA (every 100ms) → Async notification when ready

---

## C2: Container Diagram

HTI5250J is decomposed into 6 logical containers, each handling a specific responsibility:

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  WorkflowCLI / WorkflowRunner                        │  │
│  │  (YAML parsing, step execution, artifact collection)│  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────┬──────────────────────────────────────────┘
                 │
       ┌─────────┴─────────┐
       ▼                   ▼
┌────────────────┐  ┌────────────────────┐
│ Session5250    │  │ Screen5250         │
│ (lifecycle)    │  │ (buffer + state)   │
│ • connect()    │  │ • field map        │
│ • send*()      │  │ • OIA (keyboard)   │
│ • get*()       │  │ • dirty regions    │
│ • disconnect() │  │ • cursor position  │
└────────┬───────┘  └────────┬───────────┘
         │                   │
         └─────────┬─────────┘
                   ▼
        ┌──────────────────────┐
        │  tnvt (Telnet)       │
        │  • TN5250E protocol  │
        │  • Stream parsing    │
        │  • Virtual threads   │
        │  • SSL/TLS support   │
        └──────────┬───────────┘
                   │
        ┌──────────┴───────────┐
        ▼                      ▼
    ┌────────────┐    ┌──────────────┐
    │DataStream  │    │EBCDIC Codec  │
    │Producer    │    │(character    │
    │(queue)     │    │ encoding)    │
    └────────────┘    └──────────────┘
                   │
                   ▼
        ┌──────────────────────┐
        │  IBM i (AS/400)      │
        │  TN5250E protocol    │
        │  port 23/992         │
        └──────────────────────┘
```

**Container Responsibilities:**

### 1. WorkflowCLI / WorkflowRunner (Presentation)

**Purpose:** Orchestrate workflow execution end-to-end.

**Key Operations:**
- Load YAML workflow definition
- Load CSV dataset for parameter binding
- Invoke handlers (LoginHandler, NavigateHandler, FillHandler, etc.)
- Collect artifacts (screenshots, execution log)
- Report success/failure with error context

**Files:**
- `src/org/hti5250j/workflow/WorkflowCLI.java` (CLI entry point)
- `src/org/hti5250j/workflow/WorkflowRunner.java` (handler orchestration)
- `src/org/hti5250j/workflow/NavigationException.java` (error handling)
- `src/org/hti5250j/workflow/AssertionException.java` (error handling)

**External Dependency:** Calls `Session5250.sendString()`, `Session5250.getScreenText()`

---

### 2. Session5250 (Connection Lifecycle)

**Purpose:** Manage 5250 terminal session lifecycle (connection, authentication, screen reading).

**Key Operations:**
- `connect(host, port)` → Establish TCP connection, wait for telnet negotiation
- `sendString(text)` → Convert to EBCDIC, queue to tnvt
- `sendKey(keyCode)` → Send AID key (ENTER, TAB, etc.)
- `getScreenText()` → Return text representation of Screen5250 buffer
- `waitForKeyboard()` → Poll OIA until keyboard unlocked (30s timeout, 100ms intervals)
- `disconnect()` → Gracefully close connection

**Keyboard State Machine:**
```
[LOGIN]
  ├─ Session.connect()
  ├─ Session.waitForKeyboard(30s)   ← Poll OIA until keyboard available
  └─ Ready for NAVIGATE

[NAVIGATE]
  ├─ sendString(keystroke)          ← e.g., "WRKSYSVAL<ENTER>"
  ├─ Poll screen until changed      ← waitForKeyboardLockCycle()
  └─ Verify target screen text

[FILL]
  ├─ For each field:
  │  ├─ sendKey(HOME)               ← Reset cursor
  │  ├─ sendString(value)           ← Type form value
  │  └─ sendKey(TAB)                ← Move to next field
  └─ Ready for SUBMIT

[SUBMIT]
  ├─ sendKey(AID_ENTER)             ← e.g., ENTER, F5, etc.
  ├─ waitForKeyboardLockCycle()     ← Lock (processing) → Unlock (refreshed)
  └─ Ready for ASSERT

[ASSERT]
  ├─ getScreenText()
  └─ assertContains(expectedText)   ← Throws AssertionException if missing

[CAPTURE]
  ├─ getScreenText()
  └─ formatScreenDump(80 columns)   ← Write to artifacts/screenshots/
```

**Files:**
- `src/org/hti5250j/Session5250.java`
- `tests/org/hti5250j/SessionInterfaceContractTest.java` (54 contract tests)

**External Dependency:** Calls `Screen5250.getText()`, `Screen5250.getOIA()`, `tnvt.send()`

---

### 3. Screen5250 (Display Buffer + State)

**Purpose:** Maintain in-memory model of 5250 terminal display and status indicators.

**Key Operations:**
- `setText(position, text, attributes)` → Update buffer (called by tnvt data stream parser)
- `getText()` → Return entire screen as string (80 columns × 24 rows)
- `getField(position)` → Return ScreenField at cursor location
- `getOIA()` → Return operator information area (keyboard status, message indicators)
- `markDirty(region)` → Track which areas changed (for rendering optimization)

**State Maintained:**
```
Screen5250 {
  // Display buffer
  byte[] planes[3]              ← Data, extended attributes, display attributes

  // Field map (position → ScreenField)
  Map<Integer, ScreenField> fields

  // OIA (Operator Information Area)
  ScreenOIA oiaState {
    boolean keyboardLocked       ← Can user type?
    boolean messageWaiting       ← Is there a system message?
    String inputInhibited        ← Why is keyboard locked?
    int cursorPosition           ← Where is cursor? (0-1919 for 80×24)
  }

  // Dirty regions (for rendering)
  Rect dirtyRegion             ← What changed in last update?

  // Virtual thread I/O state
  AtomicBoolean screenChanged  ← Signal for waiters
}
```

**EBCDIC Handling:** Screen5250 stores EBCDIC bytes internally; conversion to UTF-8 happens at `getText()` boundary.

**Files:**
- `src/org/hti5250j/Screen5250.java` (1200+ lines, display buffer)
- `src/org/hti5250j/ScreenField.java` (field attributes)
- `src/org/hti5250j/ScreenOIA.java` (OIA state)
- `tests/org/hti5250j/Screen5250ContractTest.java` (15 contract tests)

**External Dependency:** Updated by `tnvt.parseDataStream()`, read by `Session5250`, `WorkflowRunner`

---

### 4. tnvt (Telnet Protocol Handler)

**Purpose:** Implement TN5250E protocol: stream parsing, negotiation, virtual thread I/O.

**Key Operations:**
- `connect(host, port)` → Establish TCP connection, perform telnet negotiation
- `send(bytes)` → Queue outgoing data to DataStreamProducer
- `receiveDataStream()` → Parse incoming TN5250E messages, update Screen5250
- Virtual thread I/O → Background thread reads from socket, parses protocol, updates Screen5250
- `disconnect()` → Gracefully close socket, stop virtual threads

**Virtual Thread Architecture:**

```
Main Thread (Session5250.connect())
  ├─ Create Socket
  └─ Launch virtual threads:
     │
     ├─ tnvt read thread (reads from socket)
     │  └─ Parses TN5250E packets
     │     └─ Calls Screen5250.setText()
     │        └─ Signals waiting threads
     │
     └─ DataStreamProducer thread (writes to socket)
        └─ Dequeues outgoing bytes from BlockingQueue
           └─ Encodes to TN5250E
              └─ Writes to socket
```

**Telnet Negotiation (RFC 854):**
```
Client                              Server (IBM i)
  |                                   |
  ├──────── IAC DO NAWS ─────────────>|  (Negotiate About Window Size)
  |                                   |
  |<───────── IAC WILL NAWS ─────────┤
  |                                   |
  ├──────── IAC SB NAWS 80 24 IAC SE ─>|  (Send 80×24)
  |                                   |
  |<───────── IAC DO EOR ─────────────┤  (End of Record)
  |                                   |
  └────────── Connection Ready ──────>|
```

**Files:**
- `src/org/hti5250j/tnvt.java` (1600+ lines, protocol + I/O)
- `src/org/hti5250j/DataStreamProducer.java` (queue-based outgoing stream)
- `tests/org/hti5250j/TnvtContractTest.java` (12 contract tests)

**External Dependency:** Calls `Screen5250.setText()`, uses `DataStreamProducer` for outgoing queue

---

### 5. EBCDIC Codec (Character Encoding)

**Purpose:** Convert between EBCDIC (IBM i wire format) and UTF-8 (Java string representation).

**Key Operations:**
- `ebcdicToString(byte[] buffer)` → EBCDIC 037 (US) → UTF-8 string
- `stringToEbcdic(String s)` → UTF-8 string → EBCDIC bytes
- Handle attribute bytes (not character data) in planes[1] and planes[2]

**Example:**
```
IBM i sends: 0xC8 0x85 0x93 0x93 0x96 (EBCDIC)
             ↓
Codec converts to "Hello" (UTF-8)
             ↓
Client calls session.getScreenText() → receives "Hello"
```

**Files:**
- `src/org/hti5250j/codec/EBCDICCodec.java`
- Tests included in Domain 1 (unit tests)

---

### 6. Plugin System (PluginManager / HTI5250jPlugin)

**Purpose:** Allow extensibility without modifying core.

**Key Operations:**
- `registerPlugin(HTI5250jPlugin)` → Register custom protocol handler
- `load()` → Initialize plugin
- `activate()` → Plugin becomes active in protocol pipeline
- `deactivate()` / `unload()` → Cleanup

**Example Use Cases:**
- Custom AID key definitions
- Logging/monitoring hooks
- Protocol extensions (future: WebSocket transport)

**Files:**
- `src/org/hti5250j/plugin/PluginManager.java`
- `src/org/hti5250j/plugin/HTI5250jPlugin.java` (interface)
- `tests/org/hti5250j/PluginManagerContractTest.java` (10 contract tests)

---

## C3: Components Per Container

### WorkflowRunner (Phase 11 Handler Orchestration)

The workflow runner implements six execution handlers, dispatched via switch expression:

```java
private void executeStep(StepDef step) throws Exception {
  switch (step.action) {
    case LOGIN -> handleLogin(step);           // Connect + auth
    case NAVIGATE -> handleNavigate(step);     // Screen transition
    case FILL -> handleFill(step);             // Form population
    case SUBMIT -> handleSubmit(step);         // AID key + refresh
    case ASSERT -> handleAssert(step);         // Content verification
    case CAPTURE -> handleCapture(step);       // Screenshot dump
    case WAIT -> handleWait(step);             // Timeout delay
  }
}
```

**Handler Details:**

| Handler | Input | Output | Polling | Timeout |
|---------|-------|--------|---------|---------|
| LOGIN | host, user, pass | Connected session | OIA unlock | 30s |
| NAVIGATE | keystroke, target screen | Verified screen | Screen changed | 5s |
| FILL | fields map, CSV data | Fields populated | OIA state | 5s |
| SUBMIT | AID key | Lock→Unlock cycle | Keyboard | 5s |
| ASSERT | expected text | Pass/fail + dump | None | N/A |
| CAPTURE | name | Screenshot file | None | N/A |

**Parameter Substitution:**

```yaml
step:
  action: FILL
  fields:
    account: "${data.account_id}"    ← Replaced with CSV column value
    amount: "${data.amount}"         ← Replaced with CSV column value
```

Resolution:
```
"${data.account_id}" → lookup column "account_id" in CSV → "ACC001"
```

**Error Handling:**

```java
// Pattern 1: Navigation failure
try {
  handleNavigate(step);
} catch (NavigationException e) {
  logError("Failed to reach: " + step.screen);
  throw e;  // Stop workflow, return error + artifacts
}

// Pattern 2: Assertion failure
try {
  handleAssert(step);
} catch (AssertionException e) {
  logError("Assertion failed: " + step.text);
  throw e;  // Includes screen dump in exception
}
```

**Files:**
- `src/org/hti5250j/workflow/WorkflowRunner.java` (handler dispatch)
- `src/org/hti5250j/workflow/handlers/*Handler.java` (6 implementations)
- `tests/org/hti5250j/workflow/WorkflowHandlerTest.java` (18 unit tests)
- `tests/org/hti5250j/workflow/WorkflowExecutionIntegrationTest.java` (10 integration tests)

---

### Screen5250 (Component Detail)

Screen5250 maintains three "planes" (IBM 5250 terminology):

```
planes[0] (Character Plane)
  └─ 1920 bytes (80 columns × 24 rows)
     └─ EBCDIC encoded characters

planes[1] (Extended Attributes Plane)
  └─ 1920 bytes
     └─ Color, blink, reverse video, etc.

planes[2] (Display Attributes Plane)
  └─ 1920 bytes
     └─ Field type, hidden, protected, etc.

fieldMap: Map<Integer, ScreenField>
  └─ Position → ScreenField (name, type, COMP-3 format, etc.)

oiaState: ScreenOIA
  ├─ keyboardLocked: boolean
  ├─ messageWaiting: String
  ├─ cursorPosition: int
  └─ inputInhibitedReason: String
```

**Dirty Region Tracking:**

```java
// Before update
dirtyScreen = new Rect(0, 0, 0, 0);  // Empty

// After setText(row: 5, col: 10, text: "Hello")
dirtyScreen = new Rect(col: 10, row: 5, width: 5, height: 1);

// Multiple updates expand dirty region
setText(row: 5, col: 15, text: "World")  // Adjacent
dirtyScreen = new Rect(col: 10, row: 5, width: 10, height: 1);
```

---

### tnvt (Component Detail: TN5250E Protocol)

tnvt implements the TN5250E state machine:

```
State: DISCONNECTED
  └─ connect() calls negotiateProtocol()

State: NEGOTIATING
  ├─ Send: IAC DO NAWS
  ├─ Recv: IAC WILL NAWS
  ├─ Send: IAC SB NAWS 80 24 IAC SE
  └─ Transition to: CONNECTED

State: CONNECTED
  ├─ receiveDataStream()
  │  └─ Parse TN5250E Record Format Table (RFT)
  │  └─ Update Screen5250
  │
  ├─ send(bytes)
  │  └─ Queue to DataStreamProducer
  │
  └─ disconnect()
     └─ Close socket, stop virtual threads

State: DISCONNECTED
```

---

## C4: Code-Level Detail (Keyboard State Machine)

The keyboard state machine is the core of Phase 11. It sequences operations to ensure IBM i screen refresh completes before client reads results.

### State Diagram (ASCII)

```
LOGGED_IN (keyboard: UNLOCKED)
     │
     │ sendString("WRKSYSVAL<ENTER>")
     ▼
NAVIGATE_SENT (keyboard: LOCKED - processing)
     │
     │ waitForKeyboardLockCycle()
     │  └─ while (oiaState.keyboardLocked) { sleep(100ms); }
     │     • Polls OIA every 100ms
     │     • Timeout: 5000ms
     │
     ▼
NAVIGATE_COMPLETE (keyboard: UNLOCKED - screen refreshed)
     │
     │ Verify screen text contains "Work with System Values"
     │
     ├─ Yes: Ready for FILL
     │
     └─ No: NavigationException (wrong screen)
```

### Code Pattern: waitForKeyboardUnlock()

```java
private void waitForKeyboardUnlock(long timeoutMs) throws TimeoutException {
  long deadline = System.currentTimeMillis() + timeoutMs;
  int pollCount = 0;

  while (true) {
    // Check timeout
    if (System.currentTimeMillis() > deadline) {
      throw new TimeoutException(
        String.format(
          "Keyboard unlock timeout (%dms). OIA state: %s",
          timeoutMs, getOiaDebugInfo()
        )
      );
    }

    // Poll OIA (every 100ms)
    if (screen.getOIA().isKeyboardAvailable()) {
      return;  // Ready for input
    }

    // Sleep before next poll
    Thread.sleep(100);  // 100ms poll interval
    pollCount++;
  }
}
```

### Code Pattern: handleFill()

```java
private void handleFill(StepDef step) throws Exception {
  Map<String, String> fields = step.fields;  // { "account": "ACC001", ... }

  for (Map.Entry<String, String> entry : fields.entrySet()) {
    String fieldName = entry.getKey();
    String value = entry.getValue();

    // 1. Reset cursor to field start
    session.sendKey(KeyCode.HOME);
    Thread.sleep(50);  // Let i5 process

    // 2. Type field value
    session.sendString(value);
    Thread.sleep(50);

    // 3. Move to next field
    session.sendKey(KeyCode.TAB);

    // 4. Wait for keyboard (field validation may be slow)
    waitForKeyboardUnlock(5000);
  }

  // All fields populated
  logArtifact("FILL completed: " + fields.size() + " fields");
}
```

### Error Handling Pattern: AssertionException with Dump

```java
private void handleAssert(StepDef step) throws AssertionException {
  String expectedText = step.text;
  String screenText = session.getScreenText();

  if (!screenText.contains(expectedText)) {
    // Include full screen dump for debugging
    String dump = formatScreenDump(screenText, 80);
    throw new AssertionException(
      String.format(
        "Screen did not contain: '%s'\nExpected: %s\nActual screen:\n%s",
        expectedText,
        screenText,  // Full text for search
        dump         // Formatted dump for visual inspection
      )
    );
  }
}
```

---

## Phase 11 Workflow Execution Pipeline

A complete workflow execution follows this sequence:

```
1. LOAD WORKFLOW
   └─ Parse YAML: WorkflowSchema
   └─ Validate: WorkflowValidator
   └─ Extract: steps[] array

2. LOAD DATASET
   └─ Parse CSV: Map<String, String> per row
   └─ Validate: ${data.x} references resolved

3. SESSION SETUP
   └─ Create Session5250
   └─ Create Screen5250 (empty buffer)
   └─ Create tnvt (virtual threads ready)

4. EXECUTE STEPS
   ├─ Step 0: LOGIN
   │  ├─ Extract: host, user, password (from step)
   │  ├─ Session.connect(host)
   │  ├─ Session.waitForKeyboard(30s)
   │  └─ Ready for NAVIGATE
   │
   ├─ Step 1: NAVIGATE
   │  ├─ Substitute: keystroke = "WRKSYSVAL<ENTER>"
   │  ├─ Session.sendString(keystroke)
   │  ├─ waitForKeyboardLockCycle() [lock → unlock]
   │  ├─ Verify: screenText contains step.screen
   │  └─ Ready for FILL or ASSERT
   │
   ├─ Step 2: FILL
   │  ├─ For each field:
   │  │  ├─ Substitute: value = ${data.account_id} → "ACC001"
   │  │  ├─ HOME key
   │  │  ├─ sendString(value)
   │  │  ├─ TAB key
   │  │  └─ waitForKeyboard()
   │  └─ Ready for SUBMIT
   │
   ├─ Step 3: SUBMIT
   │  ├─ Substitute: key = step.key (e.g., ENTER)
   │  ├─ Session.sendKey(key)
   │  ├─ waitForKeyboardLockCycle() [lock → unlock]
   │  └─ Ready for ASSERT
   │
   ├─ Step 4: ASSERT
   │  ├─ screenText = Session.getScreenText()
   │  ├─ Verify: screenText contains step.text
   │  └─ Throw AssertionException if missing (includes dump)
   │
   └─ Step 5: CAPTURE
      ├─ screenDump = formatScreenDump(80 columns)
      └─ Write to: artifacts/screenshots/step_5_assert.txt

5. CLEANUP
   ├─ Session.disconnect()
   └─ Return results (success/failure + artifacts)
```

**Artifact Output:**

```
artifacts/
├── ledger.txt (execution timeline)
│   └─ 2026-02-08 14:30:15.123 [LOGIN] Connecting to ibmi.example.com:23
│   └─ 2026-02-08 14:30:15.456 [LOGIN] Keyboard unlocked, ready for input
│   └─ 2026-02-08 14:30:15.478 [NAVIGATE] Sending: WRKSYSVAL<ENTER>
│   └─ 2026-02-08 14:30:16.234 [NAVIGATE] Screen verified: Work with System Values
│   └─ ...
│
└── screenshots/
    ├── step_0_login.txt (screen dump after LOGIN)
    ├── step_1_navigate.txt (screen dump after NAVIGATE)
    └── step_4_assert.txt (screen dump confirming ASSERT)
```

---

## Integration Points

### Client Library Usage

```java
// Explicit API (Session5250)
Session5250 session = new Session5250("ibmi.example.com", 23);
session.connect();
session.sendString("CALL PGM(MYAPP)");
String screen = session.getScreenText();
session.disconnect();

// Workflow API (WorkflowCLI)
WorkflowCLI cli = new WorkflowCLI();
cli.runWorkflow("payment.yaml", "payment_data.csv");
// → Produces artifacts/ directory with screenshots + ledger
```

### Testing Boundaries

| Domain | Tests | Focus | Dependency |
|--------|-------|-------|-----------|
| Domain 1 | Unit (53 existing) | EBCDIC codec, field parsing | No i5 required |
| Domain 3 | Surface (100+) | Handler correctness, boundary conditions | Mock Screen5250 |
| Domain 4 | Scenario (28) | Business workflow end-to-end | Mock or real i5 |

---

## Design Decisions & Rationale

### Decision 1: Virtual Threads for I/O

**Chosen:** Virtual threads (Project Loom, Java 21)

**Rationale:**
- Unlimited concurrent sessions (1000+ without OS thread limit)
- 1KB per thread vs 1MB per platform thread → 1000x memory efficiency
- Zero behavioral change (transparent to callers)

**Code:**
```java
// tnvt.java
Thread readThread = Thread.ofVirtual()
  .name("tnvt-" + sessionId)
  .start(this::receiveDataStream);
```

### Decision 2: OIA Polling (Not Blocking Reads)

**Chosen:** 100ms polling loop with timeout

**Rationale:**
- IBM i sends screen refresh asynchronously (no explicit "ready" signal)
- Polling detects screen refresh reliably
- Timeout prevents indefinite hangs

**Code:**
```java
// Session5250.java
while (System.currentTimeMillis() < deadline) {
  if (screen.getOIA().isKeyboardAvailable()) {
    return;  // Screen ready
  }
  Thread.sleep(100);  // Poll interval
}
throw new TimeoutException(...);
```

### Decision 3: Screen Buffer Over Socket Streaming

**Chosen:** In-memory Screen5250 buffer

**Rationale:**
- Clients need random access to screen content (not streaming)
- EBCDIC decoding happens once (not per-read)
- OIA state tracked centrally (not scatter across socket reads)

### Decision 4: Handler Dispatch via Switch Expression

**Chosen:** Java 14+ switch expression (not if-else chain)

**Rationale:**
- Exhaustiveness checking (compiler prevents missing cases)
- Cleaner syntax than if-else
- Type safety (action is enum, not string)

---

## Headless-First Philosophy

HTI5250J is designed headless-first, with no GUI dependencies in core:

- ✓ Core session API works in Docker, servers, CI/CD
- ✓ No Swing/AWT imports in critical path
- ✓ Protocol testing works offline
- ⚠ Legacy GUI code (SessionPanel.java) exists but is optional
- ⚠ Planned deprecation in Phase 14+

---

## Related Documentation

- [TESTING.md](./TESTING.md) — Four-domain test architecture
- [CODING_STANDARDS.md](./CODING_STANDARDS.md) — Development conventions
- [README.md](./README.md) — Quick start and usage examples
- [examples/README.md](./examples/README.md) — Workflow examples and execution

---

**Document Version:** 1.0
**Last Updated:** February 8, 2026
**Phase Reference:** Phase 11 (Workflow Execution Handlers, commit cef8929)
