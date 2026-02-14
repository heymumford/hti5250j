# HTI5250J Architecture

**Date:** February 2026
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
│  │  EBCDIC Encoding (23+ code pages)               │  │
│  └────────────────────────────────────────────────┘  │
└────────────┬──────────────────────────────────────────┘
             │ TN5250E Protocol (TCP, port 23 or 992)
┌────────────▼──────────────────────────────────────────┐
│          IBM i (AS/400) System                        │
│  ┌────────────────────────────────────────────────┐  │
│  │  Application programs and displays             │  │
│  │  5250 data streams + OIA status                │  │
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
- `src/org/hti5250j/framework/tn5250/Screen5250.java` (1200+ lines, display buffer)
- `src/org/hti5250j/framework/tn5250/ScreenField.java` (field attributes)
- `src/org/hti5250j/framework/tn5250/ScreenOIA.java` (OIA state)
- `tests/org/hti5250j/contracts/Screen5250ContractTest.java` (15 contract tests)

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
- `src/org/hti5250j/framework/tn5250/tnvt.java` (1600+ lines, protocol + I/O)
- `src/org/hti5250j/framework/tn5250/DataStreamProducer.java` (queue-based outgoing stream)
- `tests/org/hti5250j/contracts/TnvtContractTest.java` (12 contract tests)
- `tests/org/hti5250j/framework/tn5250/TnvtProtocolContractTest.java` (13 protocol tests)

**External Dependency:** Calls `Screen5250.setText()`, uses `DataStreamProducer` for outgoing queue

---

### 5. EBCDIC Encoding (Character Encoding)

**Purpose:** Convert between EBCDIC (IBM i wire format) and Unicode (Java string representation).

**Key Operations:**
- `ICodePage.ebcdic2uni(int)` — EBCDIC byte to Unicode character
- `ICodePage.uni2ebcdic(char)` — Unicode character to EBCDIC byte
- `CharMappings.getCodePage(String ccsid)` — Factory for code page instances
- `JavaCodePageFactory` — Fallback using Java Charset for unsupported CCSIDs

**Supported Code Pages:** 23+ built-in CCSIDs including 37 (US), 273 (German), 277 (Danish/Norwegian), 278 (Finnish/Swedish), 280 (Italian), 284 (Spanish), 285 (UK), 297 (French), 424 (Hebrew), 500 (International), 870 (Polish/Slovak), 871 (Icelandic), 875 (Greek), 930 (Japanese), 1025 (Cyrillic), 1026 (Turkish), 1112 (Baltic), 1122 (Estonian), plus Euro variants (1140, 1141, 1147, 1148).

**Files:**
- `src/org/hti5250j/encoding/ICodePage.java` (interface)
- `src/org/hti5250j/encoding/CharMappings.java` (factory)
- `src/org/hti5250j/encoding/builtin/CCSID*.java` (23+ implementations)
- `tests/org/hti5250j/encoding/builtin/CCSID*Test.java` (round-trip encoding tests)

---

## C3: Components Per Container

### WorkflowRunner (Handler Orchestration)

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

**Parameter Substitution:** YAML fields use `${data.<column>}` syntax, resolved at runtime from CSV dataset values (e.g., `${data.account_id}` resolves to the `account_id` column value for the current row).

**Error Handling:** Handler exceptions (`NavigationException`, `AssertionException`) stop workflow execution and propagate with collected artifacts (screen dumps, execution log) for debugging.

**Files:**
- `src/org/hti5250j/workflow/WorkflowRunner.java` (handler dispatch)
- `src/org/hti5250j/workflow/handlers/*Handler.java` (6 implementations)
- `tests/org/hti5250j/workflow/WorkflowHandlerTest.java` (18 unit tests)
- `tests/org/hti5250j/workflow/WorkflowExecutionIntegrationTest.java` (10 integration tests)

---

### Screen5250 (Component Detail)

Screen5250 maintains three planes of 1920 bytes each (80x24): character data (EBCDIC), extended attributes (color, blink, reverse video), and display attributes (field type, hidden, protected). The `fieldMap` maps positions to `ScreenField` objects, and `oiaState` tracks keyboard lock, cursor position, and message indicators. See the state breakdown in C2 above.

**Dirty Region Tracking:** Screen5250 maintains a `dirtyScreen` rectangle that expands to encompass all updated regions since last read, enabling efficient rendering.

---

### tnvt (Component Detail: TN5250E Protocol)

tnvt follows a three-state lifecycle: DISCONNECTED -> NEGOTIATING (IAC handshake, see C2) -> CONNECTED. In the CONNECTED state, `receiveDataStream()` parses TN5250E Record Format Table (RFT) packets and updates Screen5250, while `send()` queues bytes to `DataStreamProducer`. The `disconnect()` call closes the socket and stops virtual threads.

---

## C4: Code-Level Detail (Keyboard State Machine)

The keyboard state machine sequences operations to ensure IBM i screen refresh completes before client reads results.

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
  while (true) {
    if (System.currentTimeMillis() > deadline)
      throw new TimeoutException("Keyboard unlock timeout: " + timeoutMs + "ms");
    if (screen.getOIA().isKeyboardAvailable())
      return;
    Thread.sleep(100);  // 100ms poll interval
  }
}
```

### Code Pattern: handleFill()

Each field is populated with a HOME (reset cursor), sendString (type value), TAB (advance) sequence, with a `waitForKeyboardUnlock(5000)` call after each field to accommodate server-side field validation.

### Error Handling: AssertionException

When `handleAssert()` fails to find expected text on screen, it throws `AssertionException` with both the raw screen text and a formatted 80-column dump for visual debugging.

---

## Workflow Execution Pipeline

A workflow execution proceeds through five stages: load YAML definition, load CSV dataset, create session objects, execute steps (LOGIN, NAVIGATE, FILL, SUBMIT, ASSERT, CAPTURE), and cleanup. Each step uses the keyboard state machine described in C4 above.

**Artifact Output:**

```
artifacts/
├── ledger.txt          (timestamped execution log)
└── screenshots/        (screen dumps per step)
```

---

## Integration Points

### Client Library Usage

```java
// Explicit API (Session5250 + HeadlessSession)
Properties props = new Properties();
props.setProperty("host", "ibmi.example.com");
props.setProperty("port", "23");
SessionConfig config = new SessionConfig("session.properties", "my-session");
Session5250 session = new Session5250(props, "session.properties", "my-session", config);
session.connect();
HeadlessSession headless = session.asHeadlessSession();
headless.sendKeys("CALL PGM(MYAPP)[enter]");
String screen = headless.getScreenAsText();
session.disconnect();

// Workflow API (WorkflowCLI)
WorkflowCLI cli = new WorkflowCLI();
cli.runWorkflow("payment.yaml", "payment_data.csv");
// → Produces artifacts/ directory with screenshots + ledger
```

### Testing Boundaries

| Domain | Tests | Focus | Dependency |
|--------|-------|-------|-----------|
| Domain 1 | Unit (60+) | EBCDIC encoding, field parsing, contracts | No i5 required |
| Domain 3 | Surface (100+) | Handler correctness, boundary conditions | Mock Screen5250 |
| Domain 4 | Scenario (28) | Business workflow end-to-end | Mock or real i5 |
| Reliability | Property-based (10+) | Property testing, chaos injection | jqwik, resilience4j |

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

See `waitForKeyboardUnlock()` in the C4 section above for the implementation pattern.

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

### Decision 5: HeadlessSession Abstraction for Extensibility

**Chosen:** Four-interface abstraction (HeadlessSession, RequestHandler, Factory, Facade pattern)

**Problem:**
- Session5250 contained GUI coupling (SessionPanel initialization, hardcoded SYSREQ dialogs)
- Automation frameworks (Robot Framework, Jython) couldn't inject custom SYSREQ handlers
- Headless sessions consumed 2MB+ for GUI objects never used

**Solution:**

1. **HeadlessSession Interface** — Pure data contract
   ```java
   public interface HeadlessSession {
       String getSessionName();
       Screen5250 getScreen();
       void sendKeys(String keys) throws Exception;
       void waitForKeyboardUnlock() throws InterruptedException;
       BufferedImage captureScreenshot() throws IOException;
       void connect() throws Exception;
       void disconnect() throws Exception;
   }
   ```
   **Rationale:** Zero GUI imports in call chain, minimal surface area (6 methods)

2. **RequestHandler Interface** — Extensibility for SYSREQ handling
   ```java
   public interface RequestHandler {
       String handleSystemRequest(String screenContent);
   }
   ```
   **Implementations:**
   - `NullRequestHandler` (headless default, returns null = return to menu)
   - `GuiRequestHandler` (interactive, opens SystemRequestDialog)
   - Custom implementations (Robot Framework, Jython adapters)

   **Rationale:** Single-method interface, enables framework integration, headless-safe

3. **Session5250 as Facade** — 100% backward compatible. Adds injectable `requestHandler` (defaults to NullRequestHandler), `asHeadlessSession()` for opt-in access, and headless-safe `signalBell()`. All existing APIs unchanged.

4. **HeadlessSessionFactory** — Polymorphic creation via `createSession(name, props)` and `createSession(name, props, handler)` overloads. Supports dependency injection and Spring Boot integration.

**Impact:**
- Memory efficiency: Headless sessions ~500KB (10x reduction)
- Extensibility: Custom SYSREQ handlers for automation frameworks
- Concurrency: 1000+ virtual thread sessions without 2MB GUI overhead
- Backward compatibility: Session5250 unchanged, existing code works as-is
- Clear contract: HeadlessSession documents the pure headless API

**Files:**
- Interfaces: `HeadlessSession.java`, `RequestHandler.java`, `HeadlessSessionFactory.java`
- Implementations: `DefaultHeadlessSession.java`, `NullRequestHandler.java`, `GuiRequestHandler.java`, `DefaultHeadlessSessionFactory.java`
- Documentation: ADR-015, migration guide, Robot Framework example

---

## Headless-First Philosophy

HTI5250J is designed headless-first. Core session APIs have no GUI dependencies and work in Docker, servers, and CI/CD. Legacy GUI code (SessionPanel.java) exists but is optional. HeadlessSession (see Decision 5 above) is the recommended API for all new code.

| Use Case | API | Memory | Custom SYSREQ |
|----------|-----|--------|---------------|
| **Interactive terminal** | Session5250 + GUI | 2.5MB | Dialog only |
| **Headless automation** | HeadlessSession | 500KB | Custom handler |
| **Robot Framework** | HeadlessSession + RequestHandler | 500KB | Jython adapter |
| **High-concurrency** | Virtual threads + HeadlessSession | 500KB x 1000s | Custom handler |

**Migration:** See [MIGRATION_GUIDE_SESSION5250_TO_HEADLESS.md](./MIGRATION_GUIDE_SESSION5250_TO_HEADLESS.md)

---

## Related Documentation

**Architecture & Design:**
- [ADR-015-Headless-Abstractions.md](./ADR-015-Headless-Abstractions.md) — HeadlessSession decision record
- [MIGRATION_GUIDE_SESSION5250_TO_HEADLESS.md](./MIGRATION_GUIDE_SESSION5250_TO_HEADLESS.md) — HeadlessSession adoption guide

**Development Standards:**
- [TESTING.md](./TESTING.md) — Four-domain test architecture
- [CODING_STANDARDS.md](./CODING_STANDARDS.md) — Development conventions
- [ROBOT_FRAMEWORK_INTEGRATION.md](./ROBOT_FRAMEWORK_INTEGRATION.md) — Robot Framework automation

**Quick Reference:**
- [README.md](./README.md) — Quick start and usage examples
- [examples/README.md](./examples/README.md) — Workflow examples and execution
- [examples/HeadlessSessionExample.java](./examples/HeadlessSessionExample.java) — Java headless tutorial
- [examples/HTI5250J.py](./examples/HTI5250J.py) — Jython Robot Framework library

---

**Document Version:** 2.2
**Last Updated:** February 2026
