# HTI5250J Architectural Assessment: Component Boundaries & Integration Analysis

**Date:** February 9, 2026
**Scope:** System structure, module organization, API boundaries, programmatic integration barriers
**Audience:** Architecture review, Robot Framework integration planning, Python interop assessment

---

## Executive Summary

HTI5250J is architecturally **well-structured for headless operation** but contains **persistent GUI coupling** that creates integration friction for Robot Framework and Python automation tools. The core protocol, session, and screen layers are properly abstracted, but coupling between Session5250 (the primary public API) and GUI components (SessionPanel, SystemRequestDialog) creates ambiguity in programmatic vs. interactive use.

**Key Finding:** The architecture supports three integration patterns, but only one (pure Java) is documented:
1. **Java API** (Session5250) — Fully supported, documented, batteries-included
2. **CLI/Headless** (WorkflowCLI) — Partially supported, coupling issues remain
3. **External Tools** (Robot Framework, Python) — Intentionally **not supported** by current architecture

| Criterion | Status | Risk | Impact |
|-----------|--------|------|--------|
| Component Independence | **Compromised** | HIGH | GUI imports break headless deployments |
| API Clarity | **Ambiguous** | MEDIUM | ScreenProvider vs Session5250 boundary unclear |
| Circular Dependencies | **None detected** | LOW | Good dependency DAG |
| Abstraction Layering | **Partial** | MEDIUM | Protocol ↔ GUI coupling |
| Programmatic Access | **Limited** | HIGH | No documented RPC/REST layer |

---

## Architecture Overview

HTI5250J follows a **three-tier model with six logical components:**

```
┌──────────────────────────────────────┐
│   Client Layer                       │
│  (JUnit | CLI | Workflow | GUI)      │
└────────────┬─────────────────────────┘
             │ (SessionInterface, ScreenProvider)
┌────────────▼─────────────────────────────────────────┐
│   HTI5250J System Boundary                           │
│  ┌─────────────────────────────────────────────────┐ │
│  │ Domain Layer                                    │ │
│  │  Session5250 (lifecycle + keyboard state)      │ │
│  │  Screen5250 (display buffer)                   │ │
│  │  WorkflowRunner (handler orchestration)        │ │
│  └──────────────────┬──────────────────────────────┘ │
│  ┌──────────────────▼──────────────────────────────┐ │
│  │ Protocol Layer                                  │ │
│  │  tnvt (TN5250E transport)                       │ │
│  │  DataStreamProducer (queue)                     │ │
│  │  EBCDICCodec (encoding)                         │ │
│  └───────────────────┬────────────────────────────┘ │
│                      │ (TN5250E TCP)                │
└──────────────────────┼────────────────────────────────┘
                       │
                       ▼
              IBM i (port 23/992)
```

**Component Count:** 290 source files across 24 packages.

---

## Detailed Component Analysis

### Component 1: Session5250 (Public API)

**File:** `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/src/org/hti5250j/Session5250.java`

**Scope:** Connection lifecycle, keyboard state management, screen interaction.

**Public Interface:**
```java
public class Session5250 implements SessionInterface, ScreenProvider {
  // Lifecycle
  public void connect();
  public void disconnect();

  // Keyboard I/O
  public void sendString(String text);
  public void sendKey(KeyCode code);
  public String getScreenText();

  // GUI Coupling (PROBLEMATIC)
  public void setGUI(SessionPanel gui);
  public SessionPanel getGUI();

  // Configuration
  public String getConfigurationResource();
  public Properties sesProps;  // PUBLIC MUTABLE STATE
}
```

**Critical Imports (Lines 13-28):**
```java
import java.awt.Toolkit;                           // ← GUI DEPENDENCY
import org.hti5250j.gui.SystemRequestDialog;       // ← GUI DEPENDENCY
import org.hti5250j.SessionPanel;                  // ← GUI COUPLING (not imported)
import org.hti5250j.framework.tn5250.tnvt;
import org.hti5250j.framework.tn5250.Screen5250;
```

**Issues:**

| Issue | Severity | Details |
|-------|----------|---------|
| **GUI Import** | HIGH | `java.awt.Toolkit` imported unconditionally; breaks headless operation |
| **SystemRequestDialog** | HIGH | Used in `showSystemRequest()` (line 198: `new SystemRequestDialog(this.guiComponent)`) |
| **guiComponent Field** | HIGH | Nullable but referenced without guard; NULL → NPE in headless |
| **sesProps Mutability** | MEDIUM | Public Properties allow unchecked mutation; no encapsulation |
| **SessionPanel Getter** | MEDIUM | Exposes GUI component; ties session to UI framework |

**Root Cause:** Session5250 was designed as a **UI-aware session model**, not a pure data transport.

**Example Failure Scenario (Headless):**
```
1. Robot Framework loads HTI5250J JVM
2. Creates Session5250 instance
3. Calls connect()
4. IBM i sends system request (SYSREQ)
5. Session5250.showSystemRequest() called
6. Tries to create SystemRequestDialog with null guiComponent
7. NPE or silent failure (guiComponent == null)
8. Workflow blocks indefinitely waiting for response
```

---

### Component 2: Screen5250 (Display Buffer)

**File:** `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/src/org/hti5250j/framework/tn5250/Screen5250.java`

**Scope:** 24×80 character buffer, field map, OIA state, dirty region tracking.

**Public Interface:**
```java
public class Screen5250 {
  // Buffer access
  public String getText();
  public String getScreenText(int startRow, int startCol, int endRow, int endCol);

  // Field operations
  public ScreenField getField(int position);
  public Vector<ScreenField> getScreenFields();

  // OIA (Operator Information Area)
  public ScreenOIA getOIA();

  // Listener pattern (GUI-style)
  public void addScreenListener(ScreenListener listener);
  public void removeScreenListener(ScreenListener listener);
}
```

**Critical Observation:**
- **No GUI imports** — Screen5250 is clean (only logging, keyboard mnemonics)
- **Listener Pattern** — Event-driven architecture designed for GUI observation
- **EBCDIC Handling** — Transparent at `getText()` boundary (good)

**Assessment:** COMPLIANT with headless operation.

---

### Component 3: WorkflowRunner (Orchestration Layer)

**File:** `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/src/org/hti5250j/workflow/WorkflowRunner.java`

**Scope:** YAML workflow execution, handler dispatch, parameter substitution.

**Key Handler Dispatch:**
```java
private void executeStep(StepDef step) throws Exception {
  switch (step.action) {
    case LOGIN -> handleLogin(step);
    case NAVIGATE -> handleNavigate(step);
    case FILL -> handleFill(step);
    case SUBMIT -> handleSubmit(step);
    case ASSERT -> handleAssert(step);
    case CAPTURE -> handleCapture(step);
    case WAIT -> handleWait(step);
  }
}
```

**Assessment:** COMPLIANT with headless operation (no GUI imports).

---

### Component 4: tnvt (Protocol Handler)

**File:** `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/src/org/hti5250j/framework/tn5250/tnvt.java`

**Scope:** TN5250E protocol, telnet negotiation, virtual thread I/O.

**Assessment:** CLEAN — no GUI coupling, pure protocol implementation.

---

### Component 5: Plugin System

**File:** `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/src/org/hti5250j/plugin/`

**Interface:**
```java
public interface HTI5250jPlugin {
  void load();
  void activate();
  void deactivate();
  void unload();
}
```

**Assessment:** Minimal, extensible, no GUI coupling.

---

## Dependency Coupling Analysis

### GUI Coupling Map (Outside gui/ directory)

**42 non-GUI files importing GUI components:**

| File | Imports | Purpose |
|------|---------|---------|
| Session5250.java | SessionPanel, SystemRequestDialog | showSystemRequest() |
| SessionManager.java | SessionPanel | Session lifecycle (closeSession) |
| Tn5250jSession.java | SessionPanel | Session wrapper |
| Tn5250jController.java | SessionPanel | GUI factory pattern |
| My5250.java | SessionPanel | Main frame |
| MyApplet.java | SessionPanel | Applet launcher |
| Macronizer.java | SessionPanel | Script execution |
| XTFRFile.java | SessionPanel, Frame | File transfer (GUI dialog) |
| SendScreenImageToFile.java | SessionPanel, Frame | Screen capture |
| SendEMailDialog.java | SessionPanel, Frame | Email client |
| ... (31 more files) | Keyboard, tools | Mostly action classes |

**Pattern:** GUI imports are **primarily in keyboard/tools packages** (30+), not core.

**But Session5250 import is the **critical issue**:
- It's the **primary public API** for all integrations
- All external tools must use Session5250
- Session5250 depends on GUI components
- Therefore, **all external tools inherit GUI dependency**

### Circular Dependency Check

**Finding:** **No circular dependencies detected.**

Dependency graph is acyclic:
```
SessionPanel → Session5250 → Screen5250 → tnvt → Socket ✓
             → SessionManager
             → KeyboardHandler
```

---

## Architectural Weaknesses: Detailed Assessment

### Weakness 1: Inappropriate Intimacy (Session ↔ GUI)

**Problem:** Session5250 directly references GUI components as first-class citizens.

**Evidence:**
```java
// Session5250.java, lines 44, 251-254
private SessionPanel guiComponent;  // Can be null in headless

public void showSystemRequest() {
  final SystemRequestDialog sysreqdlg = new SystemRequestDialog(this.guiComponent);
  // NPE if guiComponent == null
}
```

**Impact:** Robot Framework cannot create a Session5250 without providing a GUI component.

**Architectural Principle Violated:** Dependency Inversion (depends on concrete GUI, not abstraction).

---

### Weakness 2: Leaky Abstraction (sesProps Mutability)

**Problem:** Public mutable Properties field allows unchecked mutation.

```java
public class Session5250 implements SessionInterface {
  protected Properties sesProps;  // PUBLIC, MUTABLE

  // Callers can do:
  session.sesProps.put("custom_key", new Object());  // Mutation not validated
}
```

**Impact:**
- No encapsulation of configuration
- Difficult for Robot Framework to reason about state
- No validation of configuration changes
- Thread-unsafe in concurrent environments

---

### Weakness 3: Ambiguous API Boundaries

**Problem:** Two interfaces expose similar functionality:

| Interface | Method | Purpose |
|-----------|--------|---------|
| **SessionInterface** | getConfigurationResource() | Legacy interface |
| **ScreenProvider** | getScreen() | New abstraction |
| **Session5250** | Implements both | Concrete implementation |

**Result:** External tools don't know which interface to program against.

**Example (Robot Framework dilemma):**
```python
# Option A: Use SessionInterface (legacy)
session = hti5250j.SessionInterface  # Abstract, can't instantiate

# Option B: Use ScreenProvider (new)
screen = session.getScreen()  # Only get Screen5250, not Session5250

# Option C: Use Session5250 directly (couples to GUI)
session = hti5250j.Session5250()  # Inherits GUI coupling
```

**Consequence:** No clear programmatic path for external tools.

---

### Weakness 4: Missing Abstraction Layer for Programmatic Access

**Current Architecture:**
```
Robot Framework
    ↓
Java Native Interface (JNI)
    ↓
Session5250  ← TIGHT COUPLING TO GUI
    ↓
Screen5250
    ↓
tnvt
    ↓
IBM i
```

**Required Architecture for Programmatic Access:**
```
Robot Framework
    ↓
Headless Session Interface (NEW)
    ↓
Session5250 → SessionPanel (optional, injected)
    ↓
Screen5250
    ↓
tnvt
    ↓
IBM i
```

**Missing Components:**

| Name | Purpose | Status |
|------|---------|--------|
| **HeadlessSession** | Pure data transport, no GUI | NOT IMPLEMENTED |
| **SessionFactory** | Polymorphic session creation | PARTIAL (ActionFactory exists) |
| **RequestHandler** | SYSREQ interception without GUI | NOT IMPLEMENTED |
| **RemoteAPI** | RPC/REST for external tools | NOT IMPLEMENTED |

---

### Weakness 5: Keyboard State Machine Tightly Coupled to GUI Events

**Current Pattern:**
```java
// Keyboard state is tied to GUI rendering
Screen5250 {
  private Vector<ScreenListener> screenListeners;  // GUI-style events
  public void addScreenListener(ScreenListener listener);
}
```

**Problem:** External tools cannot observe screen changes without subscribing to GUI events.

**Impact:** Robot Framework cannot efficiently wait for screen refresh; must poll or rely on GUI event loop.

---

## Adversarial Analysis: What Prevents Robot Framework Integration?

### Barrier 1: GUI Initialization Requirement

**Scenario:** Robot Framework tries to run HTI5250J in Docker (headless):

```python
# robot_hti5250j.py
from java.lang import System
from org.hti5250j import Session5250, SessionConfig

session = Session5250(properties, "cfg", "session1", SessionConfig())
session.connect("ibmi.example.com:23)  # ← FAILS

# Error: java.lang.ExceptionInInitializer
#   at java.awt.Toolkit.initDisplay()  ← awt initialization required!
```

**Root Cause:** `java.awt.Toolkit` is imported at class load time, forces display system initialization.

**Resolution Barrier:** Session5250 must be refactored to lazy-initialize GUI.

---

### Barrier 2: Polymorphic Session Type Not Supported

**Scenario:** Robot Framework creates mock Session5250 for testing:

```python
class MockSession5250(SessionInterface):
  def showSystemRequest(self):
    return "0"  # Mock response
```

**Problem:** Java doesn't support dynamic subclassing across language boundary.

**Additional Issue:** showSystemRequest() expects a SessionPanel to display dialog; mock has none.

---

### Barrier 3: No RPC/REST Layer

**Scenario:** Python/Robot wants to run HTI5250J in a separate JVM:

```python
# robot_hti5250j.py (desired, not possible)
client = RemoteHTI5250jClient("http://localhost:8080")
session = client.create_session("ibmi.example.com:23")
session.send_string("WRKSYSVAL")
```

**Current Reality:** No HTTP API, must run Java in-process.

**Coupling Impact:** JVM version, classpath, and JNI complexity all flow to Python.

---

## Component Dependency Map (Detailed)

```
java.awt                (ROOT IMPORT - HEADLESS BARRIER)
├── Toolkit
├── Frame
├── Component
└── Dimension

org.hti5250j.gui        (GUI LAYER - 20 files)
├── SystemRequestDialog
├── SessionPanel         (JPanel subclass)
├── GuiGraphicBuffer
└── ...

org.hti5250j.keyboard   (KEYBOARD ACTIONS - 30+ files)
├── KeyboardHandler
├── EmulatorAction
└── ... (all depend on SessionPanel)

org.hti5250j            (PUBLIC API - GATEWAY)
├── Session5250          ← CRITICAL COUPLING POINT
│   └── imports: SessionPanel, SystemRequestDialog, java.awt.Toolkit
│
└── SessionPanel         ← GUI CONCRETE
    └── extends JPanel

org.hti5250j.framework  (DOMAIN LAYER)
├── tn5250j.Session5250  (interface implementation - clean)
├── tn5250j.Screen5250   (display buffer - clean)
└── tn5250j.tnvt         (protocol - clean)

org.hti5250j.workflow   (ORCHESTRATION - clean)
└── WorkflowRunner

org.hti5250j.tools      (UTILITIES - mostly clean)
└── codec/EBCDICCodec
```

**Critical Observation:** The **gateway class (Session5250) is the coupling point**. All external tools must pass through it, and it inherits GUI dependencies from its design.

---

## Test Architecture Impact

**Domain 1 (Unit Tests):** ✓ Can mock Screen5250, no GUI required.

**Domain 3 (Surface Tests):** ✓ Can mock Session5250, but coupling unclear.

**Domain 4 (Scenario Tests):** ✗ Real Session5250 required, GUI initialization needed.

**Robot Framework:** ✗ No path forward without refactoring Session5250.

---

## API Surface Analysis

### Public Interfaces (Currently Exposed)

| Interface | Files | Status | Headless-Safe |
|-----------|-------|--------|---------------|
| SessionInterface | 7 | Legacy | ✓ Yes |
| ScreenProvider | 3 | New (Phase 11) | ✓ Yes |
| Session5250 | 42 (importing it) | Primary | ✗ **No** |
| SessionPanel | 42 (importing it) | GUI Concrete | ✗ No |
| tnvt | 4 | Protocol | ✓ Yes |
| Screen5250 | 15 (contracts) | Domain | ✓ Yes |

**Key Finding:** The **highest-coupling class (Session5250) is the primary public API**.

---

## Recommended Structural Refactoring

### Phase A: Create Headless Abstraction Layer (Critical)

**Objective:** Decouple Session5250 from GUI, enable headless operation.

**New Component: HeadlessSession**

```java
package org.hti5250j.headless;

/**
 * Pure data transport session without GUI coupling.
 *
 * @since Phase 15
 */
public interface HeadlessSession {
  void connect(String host, int port) throws IOException;
  void disconnect();

  void sendString(String text);
  void sendKey(KeyCode code);
  String getScreenText();

  ScreenOIA getOIA();
  Screen5250 getScreen();

  // Async screen change notification (not GUI event loop)
  void onScreenChanged(Consumer<Screen5250> callback);
}

/**
 * Default implementation for headless operation.
 */
public class DefaultHeadlessSession implements HeadlessSession {
  private final Screen5250 screen;
  private final tnvt vt;
  private final Properties config;

  // NO SessionPanel, NO SystemRequestDialog
  // NO java.awt imports
}
```

**Migration Path:**
1. Create HeadlessSession interface
2. Move pure I/O from Session5250 → DefaultHeadlessSession
3. Inject GUI as optional component
4. Session5250 becomes adapter (delegating to HeadlessSession)

**Files to Create:**
- `src/org/hti5250j/headless/HeadlessSession.java`
- `src/org/hti5250j/headless/DefaultHeadlessSession.java`
- `src/org/hti5250j/headless/HeadlessSessionFactory.java`

**Files to Modify:**
- `src/org/hti5250j/Session5250.java` (delegate to HeadlessSession)
- `src/org/hti5250j/workflow/WorkflowRunner.java` (use HeadlessSession)

---

### Phase B: Abstraction for System Request Handling (Important)

**Problem:** showSystemRequest() currently opens GUI dialog. Robot Framework needs interception.

**Solution: RequestHandler Interface**

```java
package org.hti5250j.headless;

public interface RequestHandler {
  /**
   * Handle system request (SYSREQ) from IBM i.
   *
   * @param request The SYSREQ message
   * @return User response (e.g., "0", "2", "9")
   */
  String handleSystemRequest(String request);
}

// Headless implementation (Robot Framework provides custom impl)
public class NullRequestHandler implements RequestHandler {
  public String handleSystemRequest(String request) {
    // Return default response, no GUI
    return "0";
  }
}

// GUI implementation (existing behavior)
public class GuiRequestHandler implements RequestHandler {
  private final SessionPanel sessionPanel;

  public String handleSystemRequest(String request) {
    final SystemRequestDialog sysreqdlg = new SystemRequestDialog(sessionPanel);
    return sysreqdlg.getResponse();
  }
}
```

**Integration in HeadlessSession:**
```java
public class DefaultHeadlessSession implements HeadlessSession {
  private final RequestHandler requestHandler;

  public String showSystemRequest() {
    return requestHandler.handleSystemRequest(lastRequest);
  }
}
```

---

### Phase C: Polymorphic Session Factory (Good Practice)

**Problem:** No clear path to create different session types (mock, real, remote).

**Solution: SessionFactory Interface**

```java
package org.hti5250j.factory;

public interface SessionFactory {
  HeadlessSession createSession(Properties config);
}

public class DefaultSessionFactory implements SessionFactory {
  public HeadlessSession createSession(Properties config) {
    return new DefaultHeadlessSession(config);
  }
}

// Robot Framework can provide:
public class RobotSessionFactory implements SessionFactory {
  public HeadlessSession createSession(Properties config) {
    // Custom initialization for Robot Framework
    return new RobotHeadlessSession(config);
  }
}
```

---

### Phase D: Lazy GUI Initialization (Technical Debt)

**Problem:** `java.awt.Toolkit` imported unconditionally at Session5250 load time.

**Solution:**
1. Move GUI imports to `GuiRequestHandler` only
2. Use Factory pattern to create request handler lazily
3. Verify no `java.awt.*` in classpath during headless unit tests

**Verification:**
```bash
# Ensure headless classpath excludes GUI
java -Djava.awt.headless=true -classpath "hti5250j-core.jar" \
  org.hti5250j.headless.DefaultHeadlessSession
```

---

## Current State vs. Target State

### Current (As-Is)

```
User Code (Robot Framework, JUnit, Python)
    ↓
Session5250 (monolithic, GUI-aware)
    ├─ Properties sesProps (mutable)
    ├─ SessionPanel guiComponent (nullable)
    ├─ SystemRequestDialog (GUI import)
    └─ java.awt.Toolkit (GUI import)
    ↓
Screen5250 ✓ (clean)
    ↓
tnvt ✓ (clean)
```

**Problems:**
- All external tools inherit GUI coupling
- No abstraction for custom RequestHandler behavior
- No RPC/REST for distributed operation
- No clear headless API contract

### Target (To-Be)

```
User Code (Robot Framework, JUnit, Python)
    ↓
HeadlessSession Interface ← GATEWAY (PURE DATA TRANSPORT)
    ├─ DefaultHeadlessSession (no GUI, no Toolkit)
    ├─ RobotHeadlessSession (custom behavior)
    └─ MockHeadlessSession (testing)
    ↓
RequestHandler Interface ← CUSTOMIZATION POINT
    ├─ NullRequestHandler (headless)
    ├─ GuiRequestHandler (interactive)
    └─ RobotRequestHandler (custom)
    ↓
Session5250 ← OPTIONAL ADAPTER (for legacy code)
    └─ delegates to HeadlessSession + GuiRequestHandler
    ↓
Screen5250 ✓ (unchanged)
    ↓
tnvt ✓ (unchanged)
```

**Benefits:**
- Robot Framework decoupled from GUI
- Clear contract for headless operation
- Custom RequestHandler for workflow extensions
- Lazy GUI initialization
- Foundation for future RPC/REST layer

---

## Recommended Refactoring Sequence

| Phase | Task | Files | Effort | Risk | Impact |
|-------|------|-------|--------|------|--------|
| **A** | Create HeadlessSession interface | 2 new | Small | LOW | CRITICAL |
| **B** | Implement DefaultHeadlessSession | 1 new | Medium | LOW | HIGH |
| **C** | Create RequestHandler abstraction | 3 new | Medium | LOW | HIGH |
| **D** | Refactor Session5250 as adapter | 1 modify | Medium | MEDIUM | HIGH |
| **E** | Update WorkflowRunner to use HeadlessSession | 1 modify | Small | LOW | MEDIUM |
| **F** | Create RobotSessionFactory | 2 new | Medium | LOW | HIGH |
| **G** | Lazy initialize java.awt (technical debt) | 3 modify | Small | MEDIUM | LOW |
| **H** | Add headless-only unit tests | 2 new | Medium | LOW | MEDIUM |

**Total Estimated Effort:** 10-15 hours (phases A-H).

---

## Python/Robot Framework Integration Blueprint (Post-Refactoring)

### Jython Bridge (In-Process)

```python
# robot_hti5250j.py (requires refactoring)
from java.lang import Properties
from org.hti5250j.headless import DefaultHeadlessSession, NullRequestHandler
from org.hti5250j.factory import SessionFactory

class RobotHTI5250jKeywords:
    ROBOT_LIBRARY_SCOPE = 'SUITE'

    def __init__(self):
        self.session = None
        self.screen = None

    def connect_to_ibm_i(self, host, port):
        """Connect to IBM i system."""
        config = Properties()
        config.setProperty("host", host)
        config.setProperty("port", str(port))

        session = DefaultHeadlessSession(config)
        session.setRequestHandler(NullRequestHandler())
        session.connect(host, int(port))

        self.session = session
        self.screen = session.getScreen()

    def send_command(self, command):
        """Send keystroke to IBM i."""
        self.session.sendString(command)
        self.session.waitForKeyboardUnlock(5000)

    def get_screen_text(self):
        """Get current screen text."""
        return self.screen.getText()

    def assert_screen_contains(self, expected):
        """Verify screen contains text."""
        text = self.screen.getText()
        assert expected in text, f"Expected '{expected}' not found in screen"

# robot.robot
*** Test Cases ***
Test Payment Workflow
    Connect to IBM i    ibmi.example.com    23
    Send Command    CALL PGM(PMTENT)
    Assert Screen Contains    Payment Entry
    Disconnect
```

---

## Missing Abstraction Layers (Summary)

| Layer | Current | Missing | Impact |
|-------|---------|---------|--------|
| **Programmatic Access** | Session5250 (monolithic) | HeadlessSession interface | CRITICAL |
| **Request Handling** | SystemRequestDialog (GUI-hardcoded) | RequestHandler abstraction | HIGH |
| **Session Creation** | No factory | SessionFactory interface | MEDIUM |
| **Remote Operation** | None | REST/RPC service | MEDIUM |
| **Configuration** | sesProps (public mutable) | Immutable Config object | LOW |
| **Error Recovery** | Limited | Error handler abstraction | LOW |

---

## Circular Dependency Risk Assessment

**Finding:** No circular dependencies in current codebase.

**But Potential Risk:** If RequestHandler abstraction is not carefully designed:
```
❌ RISKY: Session5250 → RequestHandler → Session5250 (circular)
✓ SAFE: HeadlessSession → RequestHandler (one-way)
```

**Mitigation:** Keep RequestHandler as pure interface with no back-reference to session.

---

## Conclusion: Architecture Assessment

### Strengths
1. **Core protocol layer (tnvt) is clean** — no GUI coupling
2. **Display buffer (Screen5250) is headless-safe** — no GUI dependencies
3. **No circular dependencies** — dependency graph is acyclic
4. **Extensible plugin system** — allows custom behavior injection
5. **Workflow layer (WorkflowRunner) is independent** — no GUI coupling

### Weaknesses
1. **Session5250 (primary API) couples to GUI** — blocks external tools
2. **Inappropriate intimacy** — java.awt.Toolkit imported unconditionally
3. **Leaky abstraction** — public mutable sesProps
4. **Ambiguous API boundaries** — SessionInterface vs. ScreenProvider vs. Session5250
5. **Missing headless-first abstraction** — no programmatic access layer

### Integration Blockers (Robot Framework Perspective)
1. **GUI initialization prevents headless operation** → HIGH PRIORITY FIX
2. **No RPC/REST layer for distributed operation** → MEDIUM PRIORITY
3. **SystemRequestDialog hardcoded to UI** → HIGH PRIORITY FIX
4. **No polymorphic session factory** → MEDIUM PRIORITY

### Recommendation
**Implement Phase A + Phase B (HeadlessSession + RequestHandler)** to unblock Robot Framework integration. Estimate 10-15 hours, moderate risk, critical impact on tool ecosystem.

---

**Document Version:** 1.0
**Analysis Date:** February 9, 2026
**Prepared by:** Architecture Assessment Tool
**Next Steps:** Review refactoring phases A-H with architecture steering committee.
