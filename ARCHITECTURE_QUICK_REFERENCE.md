# HTI5250J Architecture: Quick Reference Guide

**Analysis Date:** February 9, 2026
**Scope:** Component boundaries, integration barriers, refactoring roadmap
**Audience:** Architects, developers, integrators

---

## System Architecture (Current)

```
┌─────────────────────────────────────────┐
│ External Tools (JUnit, CLI, Workflow)   │
└────────────┬────────────────────────────┘
             │
             ▼
    ┌────────────────────┐
    │ Session5250 (API)  │ ← PRIMARY GATEWAY
    │ ✗ GUI Coupling     │   42 files import this
    │ ✗ java.awt.Toolkit │   All tools inherit coupling
    └────────┬───────────┘
             │
    ┌────────┴──────────┐
    │                   │
    ▼                   ▼
┌──────────┐        ┌──────────────┐
│ Screen   │        │ SessionPanel │
│ 5250     │        │ (GUI)        │
│ ✓ Clean  │        │ ✗ Couples    │
└──────────┘        └──────────────┘
    │
    ▼
  tnvt ✓ (Clean)
    │
    ▼
  IBM i
```

---

## Key Architectural Problems

### Problem 1: GUI Initialization Blocker (CRITICAL)

**What:** `java.awt.Toolkit` imported unconditionally in Session5250

**Why It Fails:**
```
Robot Framework (in Docker)
    ↓
load Session5250 class
    ↓
import java.awt.Toolkit
    ↓
java.awt.Toolkit.initDisplay()  ← Tries to initialize X11/Wayland
    ↓
❌ FAILS: No DISPLAY environment variable
```

**Impact:** Robot Framework, Python, Docker, CI/CD systems cannot use HTI5250J

**File:** `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/src/org/hti5250j/Session5250.java:13`

---

### Problem 2: SYSREQ Handling Not Configurable (HIGH)

**What:** SystemRequestDialog hardcoded for system request handling

**Code:**
```java
// Session5250.java, lines 198-205
public String showSystemRequest() {
  final SystemRequestDialog sysreqdlg =
    new SystemRequestDialog(this.guiComponent);  // ← GUI hardcoded
  return sysreqdlg.getResponse();  // ← No way to intercept
}
```

**Problem:** Robot Framework cannot customize SYSREQ responses without GUI

**Impact:** Automation workflows cannot handle system requests

---

### Problem 3: No Polymorphic Session Factory (MEDIUM)

**What:** No way to create different session types (headless, mock, remote)

**Code:**
```java
// Only way to create Session5250
Session5250 session = new Session5250(props, cfg, name, sesConfig);
// No factory, no polymorphism
```

**Problem:** Cannot create custom session for testing, automation, or distributed use

**Impact:** Couples all callers to concrete Session5250 implementation

---

## Architecture Strengths (Good News)

| Component | Status | Note |
|-----------|--------|------|
| **tnvt** (Protocol) | ✓ Clean | No GUI coupling, virtual threads |
| **Screen5250** (Display Buffer) | ✓ Clean | Headless-safe, EBCDIC handling |
| **WorkflowRunner** (Orchestration) | ✓ Clean | No GUI coupling |
| **Plugin System** | ✓ Extensible | Allows custom behavior |
| **No Circular Dependencies** | ✓ Good | Dependency graph is acyclic |

---

## Solution: HeadlessSession Abstraction (RECOMMENDED)

### Step 1: Create HeadlessSession Interface (CRITICAL)

**Purpose:** Pure data transport, no GUI coupling

**Interfaces to Create:**
```
org.hti5250j.headless.HeadlessSession (interface)
├─ org.hti5250j.headless.DefaultHeadlessSession (impl, no GUI)
├─ org.hti5250j.headless.RobotHeadlessSession (custom, Robot Framework)
└─ org.hti5250j.headless.MockHeadlessSession (testing)
```

**Key Features:**
- ✓ No java.awt imports
- ✓ Lazy GUI initialization
- ✓ Injectable RequestHandler
- ✓ Virtual thread support

---

### Step 2: Create RequestHandler Interface (HIGH)

**Purpose:** Abstraction for system request handling

**Implementations:**
```
org.hti5250j.headless.RequestHandler (interface)
├─ org.hti5250j.headless.NullRequestHandler (headless, fixed response)
├─ org.hti5250j.headless.GuiRequestHandler (interactive, dialog)
└─ org.hti5250j.headless.RobotRequestHandler (custom, Robot Framework)
```

**Enables:**
- Headless automation (no dialog)
- Interactive GUI (dialog to user)
- Custom behavior (Robot Framework integration)

---

### Step 3: Create HeadlessSessionFactory Interface (MEDIUM)

**Purpose:** Polymorphic session creation

```
org.hti5250j.headless.HeadlessSessionFactory (interface)
├─ org.hti5250j.headless.DefaultHeadlessSessionFactory
├─ org.hti5250j.headless.RobotSessionFactory
└─ org.hti5250j.headless.MockSessionFactory
```

---

### Step 4: Refactor Session5250 (BACKWARD COMPATIBLE)

**Strategy:** Convert to adapter over HeadlessSession

```java
public class Session5250 implements SessionInterface {
  private final HeadlessSession delegate;  // ← Delegates to headless impl

  public Session5250(Properties props, ...) {
    this.delegate = new DefaultHeadlessSession(props);
  }

  // All public methods delegate to delegate
  public void connect() { delegate.connect(...); }
  // etc.
}
```

**Benefit:** Backward compatible, existing code unaffected

---

## File Structure Changes

### Current (As-Is)
```
src/org/hti5250j/
├── Session5250.java                    (PROBLEMATIC)
├── SessionPanel.java                   (GUI)
├── framework/tn5250/Screen5250.java    (OK)
└── ...
```

### Target (Post-Refactoring)
```
src/org/hti5250j/
├── headless/                           (NEW)
│   ├── HeadlessSession.java            (interface)
│   ├── DefaultHeadlessSession.java      (impl)
│   ├── RequestHandler.java             (interface)
│   ├── NullRequestHandler.java         (impl)
│   ├── GuiRequestHandler.java          (impl)
│   └── HeadlessSessionFactory.java     (interface)
│
├── Session5250.java                    (REFACTORED, delegates)
├── SessionPanel.java                   (GUI, optional)
├── framework/tn5250/Screen5250.java    (UNCHANGED)
└── ...
```

---

## Integration Path: Robot Framework

### Before Refactoring (BLOCKED)
```python
from org.hti5250j import Session5250  # ← FAILS in Docker

session = Session5250(props, cfg, name, sesConfig)
# ❌ java.awt.Toolkit initialization error
```

### After Refactoring (WORKS)
```python
from org.hti5250j.headless import (
    DefaultHeadlessSession, NullRequestHandler
)

config = Properties()
session = DefaultHeadlessSession(config)
session.setRequestHandler(NullRequestHandler())
session.connect("ibmi.example.com", 23)
# ✓ Works in Docker, CI/CD, headless environments

text = session.getScreenText()
session.sendString("WRKSYSVAL")
session.disconnect()
```

---

## Implementation Roadmap

| Phase | Task | Effort | Risk | Blocking |
|-------|------|--------|------|----------|
| **1** | HeadlessSession interface | 1-2h | LOW | Robot Framework |
| **2** | RequestHandler interface | 0.5h | LOW | SYSREQ handling |
| **3** | DefaultHeadlessSession | 2-3h | MEDIUM | Core functionality |
| **4** | RequestHandler implementations | 1-2h | LOW | Variants |
| **5** | Session5250 refactoring | 1-2h | MEDIUM | Backward compat |
| **6** | Unit tests (headless) | 2-3h | LOW | Quality |
| **7** | Robot Framework integration test | 1-2h | MEDIUM | Verification |

**Total:** 12-19 hours

---

## Dependency Map (Current Coupling)

```
java.awt.* (ROOT)
  ↓
gui/* (20 files)
  ↓
Session5250 ← ALL EXTERNAL TOOLS
  ↓
42 non-GUI files (keyboard, tools, scripting, etc.)
  ↓
Robot Framework ❌ BLOCKED
Python ❌ BLOCKED
Docker ❌ BLOCKED
CI/CD ❌ BLOCKED
```

---

## Circular Dependency Analysis

**Finding:** ✓ NO circular dependencies detected

**Dependency Graph:**
```
Session5250 → Screen5250 → tnvt → Socket ✓
           → SessionManager
           → KeyboardHandler
(All dependencies flow downward, no cycles)
```

**Risk Post-Refactoring:** LOW (maintain interface-based design)

---

## SOLID Principles Assessment

| Principle | Current | Target | Gap |
|-----------|---------|--------|-----|
| **Single Responsibility** | VIOLATED | COMPLIANT | Session5250 handles both data + UI |
| **Open/Closed** | LIMITED | ENHANCED | No extension points for SYSREQ |
| **Liskov Substitution** | OK | OK | No subclass violations |
| **Interface Segregation** | POOR | GOOD | HeadlessSession splits concerns |
| **Dependency Inversion** | VIOLATED | COMPLIANT | Session5250 depends on concrete GUI |

---

## Risk Assessment

| Risk | Probability | Severity | Mitigation |
|------|-------------|----------|-----------|
| Backward compatibility break | MEDIUM | HIGH | Facade pattern, comprehensive tests |
| Coupling not fully eliminated | LOW | MEDIUM | Interface design review |
| Jython/Robot version conflicts | MEDIUM | MEDIUM | Document tested versions, CI pipeline |
| tnvt coupling issues | LOW | LOW | tnvt already clean |

---

## Success Criteria

### Criterion 1: Headless Operation
```
java -Djava.awt.headless=true \
  -cp hti5250j-core.jar \
  org.hti5250j.headless.DefaultHeadlessSession
✓ PASS: No display errors
```

### Criterion 2: Robot Framework Integration
```
robot --pythonpath . test_hti5250j.robot
✓ PASS: All tests pass in Docker
```

### Criterion 3: Backward Compatibility
```
./gradlew test
✓ PASS: All existing tests pass unchanged
```

### Criterion 4: SYSREQ Handling
```
// Custom handler for Robot Framework
session.setRequestHandler(new RobotRequestHandler());
// ✓ SYSREQ intercepted without GUI dialog
```

---

## Files Affected (Refactoring Scope)

### Create (New)
- `src/org/hti5250j/headless/HeadlessSession.java`
- `src/org/hti5250j/headless/DefaultHeadlessSession.java`
- `src/org/hti5250j/headless/RequestHandler.java`
- `src/org/hti5250j/headless/NullRequestHandler.java`
- `src/org/hti5250j/headless/GuiRequestHandler.java`
- `src/org/hti5250j/headless/HeadlessSessionFactory.java`
- `src/org/hti5250j/headless/DefaultHeadlessSessionFactory.java`
- Tests (6-8 new test files)

### Modify (Existing)
- `src/org/hti5250j/Session5250.java` (refactor as facade)
- `src/org/hti5250j/workflow/WorkflowRunner.java` (use HeadlessSession)
- `src/org/hti5250j/framework/common/SessionManager.java` (factory usage)

### Unchanged
- `src/org/hti5250j/framework/tn5250/Screen5250.java` ✓
- `src/org/hti5250j/framework/tn5250/tnvt.java` ✓
- All protocol/transport code ✓
- All GUI code ✓

---

## Next Steps

1. **Review** ARCHITECTURE_ASSESSMENT.md for detailed analysis
2. **Validate** HeadlessSession interface design
3. **Estimate** detailed implementation schedule
4. **Create** work breakdown structure (WBS)
5. **Establish** Jython/Robot Framework test environment
6. **Implement** Phase 1-3 as proof-of-concept
7. **Integration test** with Robot Framework in Docker

---

## Key Metrics (Before/After Refactoring)

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| GUI imports in core API | 2+ | 0 | 100% elimination |
| External tool compatibility | 1 (Java only) | 3+ (Java, Python, Robot, CLI) | 3x improvement |
| Abstraction layers | 1 | 4+ | Better separation of concerns |
| Extensibility points | Limited | High (RequestHandler, Factory) | Significant |
| Circular dependencies | 0 | 0 | Maintained |

---

## References

**Full Analysis Documents:**
- `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/ARCHITECTURE_ASSESSMENT.md`
- `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/COMPONENT_DEPENDENCY_DIAGRAM.md`
- `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/ROBOT_FRAMEWORK_INTEGRATION_PLAN.md`

**Existing Project Docs:**
- `ARCHITECTURE.md` (current system design)
- `CODING_STANDARDS.md` (development practices)
- `TESTING_EPISTEMOLOGY.md` (test architecture)

---

**Document Version:** 1.0
**Generated:** February 9, 2026
**Status:** Ready for architecture review and implementation planning
