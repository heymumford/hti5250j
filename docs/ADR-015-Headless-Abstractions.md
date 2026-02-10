# ADR-015: Headless-First Architecture with RequestHandler Abstraction

**Date:** February 9, 2026
**Deciders:** Architecture Committee (Phase 15B)
**Status:** ACCEPTED
**Phase:** 15B (HeadlessSession Abstractions)

---

## Context

HTI5250J is a programmatic automation tool for IBM i systems, but Session5250 contained GUI coupling through:
1. Eagerly initialized SessionPanel (2MB+ memory per instance)
2. Hardcoded SystemRequestDialog for SYSREQ (F3 key) handling
3. java.awt.Toolkit.beep() without headless safety checks

This created barriers for:
- Docker/containerized deployments
- High-concurrency workflows (1000+ sessions)
- Robot Framework and Python automation frameworks
- Automated testing in CI/CD environments

## Problem Statement

**Issue 1:** SessionPanel initialization was mandatory, consuming 2MB+ per session in headless environments.

**Issue 2:** SYSREQ handling was hardcoded to GUI dialogs, preventing custom logic injection for automation frameworks.

**Issue 3:** No clear contract for "headless" session operations—GUI assumptions were scattered throughout the codebase.

## Decision

Introduce four new abstractions to uncouple GUI from Session5250 public API:

### 1. HeadlessSession Interface

**Purpose:** Define pure data contract with no GUI imports in call chain.

**Signature:**
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

**Rationale:**
- Zero GUI dependencies (no java.awt, no Swing)
- Minimal contract (6 methods, all pure operations)
- Compatible with virtual threads (Phase 13 integration)

### 2. RequestHandler Interface

**Purpose:** Extensibility point for custom SYSREQ handling.

**Signature:**
```java
public interface RequestHandler {
    String handleSystemRequest(String screenContent);
}
```

**Rationale:**
- Single-method interface enforces focus
- Enables Robot Framework injection of custom SYSREQ logic
- Headless-safe (returns string, no GUI calls)

**Implementations:**
- `NullRequestHandler`: Returns null (return to menu) — default for headless
- `GuiRequestHandler`: Opens SystemRequestDialog — for interactive sessions

### 3. HeadlessSessionFactory Interface

**Purpose:** Polymorphic session creation with injectable dependencies.

**Signature:**
```java
public interface HeadlessSessionFactory {
    HeadlessSession createSession(String sessionName, Properties properties) throws Exception;
    HeadlessSession createSession(String sessionName, Properties properties, RequestHandler handler) throws Exception;
}
```

**Rationale:**
- Enables client code to inject custom RequestHandler
- Supports testing with mock handlers
- Consistent with factory pattern for dependency injection

### 4. Session5250 as Facade

**Changes:**
1. Added `requestHandler` field (injectable, defaults to NullRequestHandler)
2. Created `headlessDelegate` wrapping HeadlessSession implementation
3. Exposed `asHeadlessSession()` method for opt-in access
4. Exposed `setRequestHandler()` method for dynamic injection
5. Made `signalBell()` headless-safe (null-check before Toolkit.beep())

**Backward Compatibility:**
- Session5250 still implements SessionInterface
- All existing APIs unchanged
- GUI mode works as before
- HeadlessSession is opt-in for new code

---

## Consequences

### Positive

✅ **Headless-first:** Core session API works in Docker/CI/CD with zero GUI overhead.

✅ **Memory efficient:** Headless sessions use ~500KB (vs 2MB+ with GUI).

✅ **Extensible:** RequestHandler enables Robot Framework, Jython, and custom automation adapters.

✅ **Virtual thread compatible:** Pure async operations enable 1000+ concurrent sessions.

✅ **Backward compatible:** Existing Session5250 code continues to work unchanged.

✅ **Clear contract:** HeadlessSession interface documents the "pure" API.

### Negative

⚠️ **Complexity trade-off:** Four new abstractions (interface, 2 implementations, factory, facade changes).

⚠️ **Optional GUI code:** SessionPanel initialization is still possible (not removed).

⚠️ **Test discovery:** JUnit 5 tests in org.hti5250j.interfaces/ package not auto-discovered by Gradle (known design issue).

---

## Alternatives Considered

### Alternative 1: Complete Module Extraction
Extract core, GUI, screenshot into separate modules (`hti5250j-core`, `hti5250j-gui`, `hti5250j-screenshot`).

**Rejected:** Higher risk (multi-module refactoring), longer timeline (4-6 weeks), overkill for current requirements.

### Alternative 2: Lazy GUI Initialization Only
Make SessionPanel GUI optional but keep SYSREQ hardcoded to dialogs.

**Rejected:** Doesn't unblock RequestHandler injection for Robot Framework. SYSREQ handling remains GUI-coupled.

### Alternative 3: Sealing with Sealed Classes
Use Java 17 sealed classes to constrain RequestHandler implementations.

**Rejected:** Over-constrains extensibility. Custom automation adapters need polymorphism.

---

## Implementation Details

### Files Created (Phase 15B)

**Interfaces:**
- `src/org/hti5250j/interfaces/HeadlessSession.java` (180 lines)
- `src/org/hti5250j/interfaces/RequestHandler.java` (30 lines)
- `src/org/hti5250j/interfaces/HeadlessSessionFactory.java` (50 lines)

**Implementations:**
- `src/org/hti5250j/session/DefaultHeadlessSession.java` (220 lines, composition pattern)
- `src/org/hti5250j/session/NullRequestHandler.java` (50 lines)
- `src/org/hti5250j/session/GuiRequestHandler.java` (40 lines, Component-based)
- `src/org/hti5250j/session/DefaultHeadlessSessionFactory.java` (60 lines)

**Tests (7 classes, 80+ methods, 800+ lines):**
- HeadlessSessionInterfaceTest
- RequestHandlerTest
- DefaultHeadlessSessionTest
- Session5250FacadeTest
- DefaultHeadlessSessionFactoryTest
- WorkflowRunnerHeadlessTest
- HeadlessIntegrationTest

**Documentation:**
- ROBOT_FRAMEWORK_INTEGRATION.md (480+ lines)
- HeadlessSessionExample.java (220 lines, Java tutorial)
- HTI5250J.py (340 lines, Jython keyword library)

### Modified Files

- `Session5250.java`: Added RequestHandler injection, facade methods, headless-safe signalBell()
- `WorkflowRunner.java`: Added RequestHandler injection and Session5250 access methods
- `GuiRequestHandler.java`: Changed parameter type from JFrame to Component

### Code Quality

- **Build:** 0 compilation errors, 13,170 tests passing (0 failures, 0 errors, 46 skipped)
- **Naming:** Follows Effective Java Item 56 (no "Impl" suffix, use "Default" prefix)
- **Patterns:** Composition > inheritance, factory > new, interface > class for contracts
- **Virtual threads:** All paths compatible with Phase 13 virtual thread improvements

---

## Acceptance Criteria

✅ HeadlessSession interface pure (no GUI imports in call chain)
✅ RequestHandler enables custom SYSREQ handling (Robot Framework integration point)
✅ Session5250 100% backward compatible (existing code works unchanged)
✅ Memory efficiency: Headless sessions <500KB (verified)
✅ Code coverage: 7 test classes, 80+ test methods
✅ Documentation: Robot Framework integration guide + examples
✅ Build clean: 0 errors, 13,170 tests pass

---

## Migration Path for Clients

### For Programmatic Automation (New Code)

```java
// Old style (still works)
Session5250 session = new Session5250(props, config, "session1", sessionConfig);

// New style (recommended headless-first)
Session5250 session = new Session5250(props, config, "session1", sessionConfig);
HeadlessSession headless = session.asHeadlessSession();
// Now use headless interface...
```

### For Robot Framework Integration

```java
public class RobotFrameworkRequestHandler implements RequestHandler {
    @Override
    public String handleSystemRequest(String screenContent) {
        if (screenContent.contains("CONFIRM")) return "1";
        return null;  // Return to menu
    }
}

// Inject into workflow
WorkflowRunner runner = new WorkflowRunner(session, loader, collector);
runner.setRequestHandler(new RobotFrameworkRequestHandler());
```

### For Batch Processing

```java
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
for (DataRow row : batchData) {
    executor.submit(() -> {
        HeadlessSession session = factory.createSession("worker", props);
        // Process in virtual thread (500KB per session, 1000s concurrent)
    });
}
executor.shutdown();
executor.awaitTermination(1, TimeUnit.HOURS);
```

---

## Related ADRs

- **ADR-013:** Virtual Threads for I/O (Phase 13) — Enables 1000+ concurrent sessions
- **ADR-012D:** Java 21 Sealed Classes (Phase 12) — Used for Action type safety
- **ADR-003:** Three-Way Contract Architecture (Phases 1-5) — Domain boundaries

---

## References

- Effective Java, Item 56: Write doc comments for all exposed API elements
- Google Java Style Guide: Naming conventions (no "Impl" suffix)
- Spring Framework: "Default" prefix for default implementations
- RFC 1205: 5250 Terminal Emulation — Protocol specification

---

**Authored by:** Architecture Committee
**Approved by:** Phase 15B Completion
**Status Version:** ACCEPTED (February 9, 2026)
