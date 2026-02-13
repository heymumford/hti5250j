# HTI5250J API 1.0 Readiness Checklist

**Purpose:** Track API stabilization tasks for production release
**Target Version:** 1.0.0
**Date Created:** February 13, 2026

---

## Phase 1: API Consolidation (Critical Path)

### [ ] 1.1 Remove HeadlessSession Class Duplicate
- **File:** `/src/org/hti5250j/headless/HeadlessSession.java`
- **Action:** Delete this class entirely
- **Reason:** Conflicts with `org.hti5250j.interfaces.HeadlessSession` interface
- **Impact:** High (fixes discoverability issue)
- **Effort:** 30 minutes
- **Testing:** Run `grep -r "org.hti5250j.headless.HeadlessSession" src/ tests/` to find usages

```bash
# Verify no references before deletion
grep -r "import org.hti5250j.headless.HeadlessSession" src/ tests/
rm src/org/hti5250j/headless/HeadlessSession.java
```

### [ ] 1.2 Rename tnvt to TN5250ETransport
- **File:** `/src/org/hti5250j/framework/tn5250/tnvt.java`
- **Action:** Rename class from `tnvt` to `TN5250ETransport`
- **Reason:** Nonsensical name; should indicate TN5250E protocol handling
- **Impact:** Medium (but easy find/replace)
- **Effort:** 2 hours (including all references)
- **Breaking Change:** Yes, deprecate old class name

```bash
# Find all references
grep -r "class tnvt" src/
grep -r " tnvt " src/ | grep -v "//" | wc -l
grep -r "new tnvt(" src/

# Rename
mv src/org/hti5250j/framework/tn5250/tnvt.java \
   src/org/hti5250j/framework/tn5250/TN5250ETransport.java

# Update references (use IDE refactoring)
```

### [ ] 1.3 Consolidate HeadlessSession API
- **Current State:** `Session5250` has methods like `sendString()`, `sendTab()`; `HeadlessSession` has `sendKeys()`
- **Action:** Normalize all keystroke submission to `sendKeys()` with mnemonics
- **Classes Affected:**
  - `Session5250.java` — deprecate `sendString()`, `sendKey()`, add helpers
  - `DefaultHeadlessSession.java` — already correct
- **Effort:** 4 hours
- **Breaking Change:** Deprecation warnings, migration guide needed

**Changes:**
```java
// Session5250: NEW methods (delegate to HeadlessSession)
@Deprecated(since = "0.13.0", forRemoval = true)
public void sendString(String text) {
    headlessDelegate.sendKeys(text);
}

@Deprecated(since = "0.13.0", forRemoval = true)
public void sendTab() {
    headlessDelegate.sendKeys("[tab]");
}

@Deprecated(since = "0.13.0", forRemoval = true)
public void sendEnter() {
    headlessDelegate.sendKeys("[enter]");
}

// Primary method (public)
public void sendKeys(String keys) {
    headlessDelegate.sendKeys(keys);
}
```

### [ ] 1.4 Update README Examples
- **File:** `/README.md` Quick Start section
- **Current Code:**
  ```java
  TN5250Session session = new TN5250Session("192.168.1.100", 23);
  session.sendKeys("USER");
  ```
- **Issues:**
  1. Class name `TN5250Session` doesn't exist (should be `Session5250`)
  2. No `sendKeys()` method on `Session5250` (only `sendString()`)
  3. No error handling shown
  4. No async connection handling mentioned
  5. `getScreenText()` not on `Session5250` (only on `HeadlessSession`)
- **Action:** Rewrite using correct API
- **Effort:** 1 hour

**Replacement:**
```java
// Option A: Using HeadlessSession (recommended)
HeadlessSession session = new HeadlessSessionBuilder()
    .to("192.168.1.100", 23)
    .build();

session.connect();
session.sendKeys("USER[tab]PASSWORD[enter]");
session.waitForKeyboardUnlock(5000);
String screen = session.getScreenAsText();
session.disconnect();

// Option B: Using Session5250 (legacy, to be deprecated)
Properties props = new Properties();
props.setProperty("host", "192.168.1.100");
props.setProperty("port", "23");
Session5250 session = new Session5250(props, "config", "session-1", new SessionConfig(...));
session.connect();
// ... interaction ...
session.disconnect();
```

---

## Phase 2: Error Handling Improvements

### [ ] 2.1 Create Custom Exception Hierarchy
- **Location:** `/src/org/hti5250j/exception/` (new package)
- **Classes to Create:**
  ```java
  HTI5250jException (extends Exception, base class)
  ├── ConnectionException (connection failures)
  ├── TimeoutException (operations that timeout)
  ├── KeyboardTimeoutException (extends TimeoutException, for keyboard-specific timeouts)
  ├── ScreenException (screen parsing/encoding failures)
  ├── FieldException (field-level errors)
  │   ├── FieldNotFoundException
  │   └── FieldValidationException
  ├── SecurityException (SSL/auth failures)
  └── ConfigurationException (invalid config)
  ```
- **Effort:** 3 hours
- **Breaking Change:** No (new classes, existing exceptions still thrown)

**Example Structure:**
```java
public class HTI5250jException extends Exception {
    private final ErrorCode errorCode;
    private final ErrorContext context;

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public ErrorContext getContext() {
        return context;
    }
}

public enum ErrorCode {
    CONNECTION_TIMEOUT("E001", "Failed to connect within timeout"),
    KEYBOARD_TIMEOUT("E002", "Keyboard locked beyond timeout"),
    NETWORK_IO_ERROR("E003", "Network I/O failure"),
    SSL_HANDSHAKE_FAILED("E004", "TLS negotiation failed"),
    AUTHENTICATION_FAILED("E005", "Cannot handle SYSREQ dialog"),
    SCREEN_ENCODING_ERROR("E006", "Cannot decode EBCDIC screen"),
    FIELD_NOT_FOUND("E007", "Field not found on screen"),
    ;

    private final String code;
    private final String description;

    ErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }
}

public class ErrorContext {
    private final String host;
    private final int port;
    private final String sessionName;
    private final String screenContent;
    private final long timestamp;

    // getters...
}
```

### [ ] 2.2 Enhance Exception Messages
- **Task:** Update all exception throws with structured error context
- **Classes to Update:**
  - `DefaultHeadlessSession.java` (already good, but add context)
  - `Session5250.java`
  - `Screen5250.java`
  - `tnvt.java` (or `TN5250ETransport.java`)
- **Effort:** 8 hours

**Pattern:**
```java
// BEFORE
throw new TimeoutException("Keyboard locked after 5000ms");

// AFTER
throw new KeyboardTimeoutException(
    "Keyboard not unlocked after 5000ms",
    ErrorCode.KEYBOARD_TIMEOUT,
    new ErrorContext(
        host, port, sessionName,
        screen.getScreenAsChars(),
        System.currentTimeMillis()
    ),
    "Suggested actions: (1) Increase timeout to 10000ms, (2) Check AS/400 system for hung jobs"
);
```

### [ ] 2.3 Add Error Recovery Guide
- **File:** `/docs/ERROR_HANDLING_GUIDE.md` (new)
- **Content:**
  - Common error scenarios and recovery strategies
  - Retry patterns with exponential backoff
  - Timeout tuning guide
  - Connection loss recovery
  - SSL/TLS troubleshooting
- **Effort:** 3 hours

---

## Phase 3: API Convenience Layers

### [ ] 3.1 Create HeadlessSessionBuilder
- **File:** `/src/org/hti5250j/HeadlessSessionBuilder.java`
- **Purpose:** Fluent configuration for session creation
- **Effort:** 4 hours
- **Testing:** 20+ unit tests covering builder patterns

**Checklist:**
- [ ] `to(host, port)` method
- [ ] `withCodePage(CodePage)` method
- [ ] `withTimeout(Duration)` method
- [ ] `withSSL(String truststore)` method
- [ ] `withCustomRequestHandler(RequestHandler)` method
- [ ] `withScreenSize(rows, cols)` method
- [ ] `build()` method returning configured HeadlessSession
- [ ] `connect()` convenience method (build + connect)
- [ ] Input validation in constructor
- [ ] Documentation with examples

### [ ] 3.2 Add Fluent API to HeadlessSession
- **Task:** Make sendKeys(), waitFor*() chainable
- **Interface Changes:**
  ```java
  public interface HeadlessSession {
      HeadlessSession sendKeys(String keys);
      HeadlessSession waitForKeyboardUnlock(Duration timeout) throws TimeoutException;
      HeadlessSession waitForText(String text, Duration timeout) throws TimeoutException;
      HeadlessSession screenshot(String filename) throws IOException;
      // ... rest of API ...
  }
  ```
- **Implementation:** Update `DefaultHeadlessSession` to return `this`
- **Effort:** 2 hours
- **Breaking Change:** No (additive)

### [ ] 3.3 Add Field-Level API
- **Files:** `/src/org/hti5250j/field/FieldAPI.java` (new)
- **Classes to Create:**
  - `ScreenField` (interface representing a form field)
  - `FieldFinder` (locate fields by name or position)
- **Methods to Add to HeadlessSession:**
  - `getField(String fieldName): ScreenField`
  - `getFieldValue(String fieldName): String`
  - `fillField(String fieldName, String value): HeadlessSession`
  - `getFieldLabels(): List<String>`
  - `getFields(): List<ScreenField>`
- **Effort:** 8 hours
- **Testing:** 30+ tests for field matching, edge cases

---

## Phase 4: Documentation

### [ ] 4.1 Complete Javadoc Coverage
- **Target:** Every public class, interface, and method has Javadoc
- **Validation:** `javadoc -private src/ > /tmp/javadoc.log 2>&1; grep -i "warning" /tmp/javadoc.log`
- **Effort:** 6 hours
- **Checklist:**
  - [ ] HeadlessSession interface (11 methods)
  - [ ] HeadlessSessionBuilder (10 methods)
  - [ ] Session5250 public API
  - [ ] Screen5250 public API
  - [ ] Exception classes

### [ ] 4.2 Create API Reference Guide
- **File:** `/docs/API_REFERENCE.md` (new)
- **Sections:**
  - Overview and design principles
  - Connection management (connect, disconnect, reconnect)
  - Screen interaction (sendKeys, waitFor, getScreen)
  - Field operations (getField, fillField)
  - Screenshots and rendering
  - Error handling and recovery
  - Asynchronous operations
- **Effort:** 4 hours

### [ ] 4.3 Create Migration Guides
- **File:** `/docs/MIGRATION_GUIDE.md` (new)
- **Content:**
  - From `Session5250` to `HeadlessSession`
  - From `sendString()` to `sendKeys()`
  - From listener patterns to CompletableFuture
  - From old exception types to new hierarchy
- **Effort:** 3 hours

### [ ] 4.4 Add Example Programs
- **Directory:** `/examples/` (enhance existing)
- **Examples to Add:**
  - [ ] Simple login and screen read
  - [ ] Form filling and submission
  - [ ] Error handling with retry
  - [ ] Session pooling for load testing
  - [ ] Concurrent workflow execution
  - [ ] Screenshot capture and storage
- **Effort:** 4 hours

---

## Phase 5: Testing and Validation

### [ ] 5.1 API Contract Tests
- **Purpose:** Verify API stability and backward compatibility
- **Tests to Add:**
  - [ ] `HeadlessSessionAPIContractTest` (all methods callable and documented)
  - [ ] `BuilderPatternTest` (builder methods chain correctly)
  - [ ] `ErrorHandlingContractTest` (exceptions have proper structure)
  - [ ] `DeprecationWarningTest` (deprecated methods log warnings)
- **Effort:** 4 hours

### [ ] 5.2 Example Program Tests
- **Purpose:** Verify README examples actually work
- **Approach:** Add integration tests that run examples
- **Effort:** 2 hours

### [ ] 5.3 Breaking Change Analysis
- **Task:** Document all breaking changes from 0.12 → 1.0
- **Output:** `BREAKING_CHANGES.md`
- **Effort:** 2 hours

**Changes to Document:**
1. Removal of `org.hti5250j.headless.HeadlessSession` class
2. Renaming of `tnvt` to `TN5250ETransport`
3. Deprecation of `Session5250.sendString()` and `sendTab()`
4. Exception type changes (org.hti5250j.exception.* vs java.util.concurrent.*)
5. Removal of listener-based event handling (when it happens in 1.1+)

---

## Phase 6: Performance and Scalability

### [ ] 6.1 Connection Pooling API
- **File:** `/src/org/hti5250j/pool/HeadlessSessionPool.java`
- **Interface Methods:**
  - `start()`
  - `borrowSession(Duration timeout)`
  - `returnSession(HeadlessSession)`
  - `shutdown(Duration gracefulWait)`
  - `getStatistics()`
- **Effort:** 6 hours
- **Testing:** Concurrency tests with 50+ concurrent sessions

### [ ] 6.2 Performance Baseline Tests
- **Purpose:** Document expected latency/throughput
- **Tests to Add:**
  - [ ] Connection latency (avg, p50, p95, p99)
  - [ ] Keystroke submission latency
  - [ ] Screen read latency
  - [ ] Screenshot generation cost
  - [ ] Pool borrowing cost
- **Effort:** 3 hours

---

## Phase 7: Release Checklist

### Before Releasing 1.0.0:

- [ ] All Phase 1-6 tasks completed
- [ ] No deprecation warnings in tests
- [ ] All examples run and pass
- [ ] Javadoc generation succeeds with no warnings
- [ ] Test coverage ≥85% for public API
- [ ] Breaking changes documented
- [ ] Migration guide available
- [ ] API reference complete
- [ ] Release notes drafted
- [ ] Version updated to 1.0.0 in:
  - [ ] `build.gradle`
  - [ ] `/README.md`
  - [ ] `CHANGELOG.md`
  - [ ] All Javadoc class headers

---

## Effort Summary

| Phase | Effort | Priority | Target |
|-------|--------|----------|--------|
| 1. API Consolidation | 10 hours | P0 | v0.13.0 |
| 2. Error Handling | 14 hours | P0 | v0.13.0 |
| 3. Convenience Layers | 18 hours | P1 | v0.14.0 |
| 4. Documentation | 14 hours | P1 | v0.14.0 |
| 5. Testing | 8 hours | P1 | v0.14.0 |
| 6. Performance | 9 hours | P2 | v1.0.0 |
| **Total** | **73 hours** | - | **1.0.0** |

---

## Success Criteria

A new developer should:
1. ✓ Copy-paste README example and have it work without modification
2. ✓ Understand API intent from method names alone (no Javadoc required)
3. ✓ Get actionable error messages with recovery suggestions
4. ✓ Build concurrent test scenarios with < 20 lines of code
5. ✓ Find examples for their use case in `/examples/` directory
6. ✓ Migrate from old API (Session5250) to new API (HeadlessSession) in < 30 minutes

---

## Tracking

**Assignee:** TBD
**Start Date:** TBD
**Target Completion:** TBD
**Status:** Not Started

### Progress by Phase:
- [ ] Phase 1: 0% (0/4 tasks)
- [ ] Phase 2: 0% (0/3 tasks)
- [ ] Phase 3: 0% (0/3 tasks)
- [ ] Phase 4: 0% (0/4 tasks)
- [ ] Phase 5: 0% (0/3 tasks)
- [ ] Phase 6: 0% (0/2 tasks)
- [ ] Phase 7: 0% (0/10 tasks)

---

## Notes

- **Backward Compatibility:** All Phase 1-2 changes should use deprecation, not removal. Removal happens in 1.1.
- **Testing Strategy:** Each phase should have unit + integration tests. Add contract tests to prevent regression.
- **Documentation:** Keep DRY — link from Javadoc to markdown docs, not duplicate content.
- **Communication:** Announce deprecations early (v0.13.0), removal happens in v1.1.0 (not 1.0).
