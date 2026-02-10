# HTI5250J Pattern Remediation Guide

**Companion to:** PATTERN_ANALYSIS.md
**Date:** February 9, 2026
**Scope:** Actionable fixes ranked by impact and effort

---

## Critical Issue #1: GUI Coupling in tnvt.java

### Location
- **File:** `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/src/org/hti5250j/framework/tn5250/tnvt.java`
- **Lines:** 238, 290, (and implicit throughout)
- **Imports:** `import javax.swing.*;` (line 16)

### Problem Code
```java
// Line 238 - Protocol handling calls GUI directly
SwingUtilities.invokeAndWait(new Runnable() {
    public void run() {
        // protocol processing with side effects
    }
});
```

### Why It Blocks Robot Framework
- Robot Framework runs in headless environment (no GUI event dispatch thread)
- `SwingUtilities.invokeAndWait()` hangs waiting for non-existent EDT
- Timeout occurs; workflow fails
- Python process can't orchestrate i5 interactions

### Root Cause
The original design coupled protocol parsing (network I/O) with GUI rendering (Swing).
This was acceptable for GUI-only application but breaks programmatic use cases.

### Remediation Strategy

**Step 1: Extract Protocol Handler (2 days)**

Create new file: `Tn5250Protocol.java`
```java
package org.hti5250j.framework.tn5250;

import java.io.InputStream;

/**
 * Headless TN5250 protocol handler.
 * Parses incoming bytes, produces protocol events.
 * NO GUI dependencies.
 */
public class Tn5250Protocol {
    private final ScreenBuffer buffer;

    public Tn5250Protocol(ScreenBuffer buffer) {
        this.buffer = buffer;
    }

    /**
     * Parse protocol bytes and update screen state.
     *
     * @param inputStream Raw bytes from i5
     * @return List of protocol events (field updates, alerts, etc.)
     * @throws ProtocolException if parsing fails
     */
    public List<ProtocolEvent> processBytes(InputStream inputStream)
        throws ProtocolException {

        // Move lines 238+ protocol logic here
        // Return events instead of calling SwingUtilities

        List<ProtocolEvent> events = new ArrayList<>();
        // ... parse bytes ...
        return events;
    }
}
```

**Step 2: Create GUI Event Handler (1 day)**

File: `Tn5250GUIHandler.java`
```java
/**
 * GUI-specific event handler.
 * Converts ProtocolEvents to Swing updates.
 * Optional: used only when GUI is present.
 */
public class Tn5250GUIHandler {
    public void onProtocolEvent(ProtocolEvent event) {
        SwingUtilities.invokeAndWait(() -> {
            // Render event to screen
        });
    }
}
```

**Step 3: Refactor tnvt.java (1 day)**

```java
// Before
SwingUtilities.invokeAndWait(() -> {
    // 100 lines of protocol + rendering
});

// After
protocol = new Tn5250Protocol(screenBuffer);
List<ProtocolEvent> events = protocol.processBytes(inputStream);

if (guiMode) {
    guiHandler = new Tn5250GUIHandler(screen);
    events.forEach(guiHandler::onProtocolEvent);
}
```

### Impact on Robot Framework
- ✅ Headless protocol execution possible
- ✅ Events can be collected without GUI
- ✅ Python can orchestrate without EDT thread
- ✅ Existing GUI code unchanged

### Verification
```bash
# Before: Hangs
java -cp hti5250j.jar MyHeadlessTest

# After: Completes
java -cp hti5250j.jar -Djava.awt.headless=true MyHeadlessTest
```

---

## Critical Issue #2: Screen5250 God Object

### Location
- **File:** `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/src/org/hti5250j/framework/tn5250/Screen5250.java`
- **Size:** 3,411 lines
- **Methods:** 122 public/protected
- **Responsibilities:** Rendering, state management, I/O, events, protocol dispatch

### Symptom
```java
Screen5250 screen = session.getScreen();
// Now what? 122 methods to choose from. No clear API contract.
screen.drawField(...);      // Rendering
screen.getCurrentField();   // State query
screen.handleDataStream(); // I/O
screen.addScreenListener(l); // Events
```

### Why It Blocks Robot Framework
- Python caller must understand entire 3,400-line class
- No clear "read-only" vs "mutating" separation
- Side effects in getters (e.g., `getCurrentField()` may modify state)
- Hard to mock for testing; easy to leak state between runs

### Remediation Strategy

**Decompose into 4 focused classes (2 weeks):**

```
Screen5250 (current: 3411 lines)
    ↓
┌───────────────────────────────────────┐
│ ScreenBuffer (state only)             │  ← 600 LOC
│  - getField(row, col)                 │
│  - getFieldContent(name)              │
│  - getCursorPosition()                │
│  (Immutable snapshots only)           │
└───────────────────────────────────────┘
     ↑
     │ uses
     ↓
┌───────────────────────────────────────┐
│ ScreenRenderEngine (rendering logic)  │  ← 800 LOC
│  - drawField(field)                   │
│  - renderAttribute(x, y, attr)        │
│  - drawGraphic(x, y, char)            │
└───────────────────────────────────────┘
     ↑
     │ notifies
     ↓
┌───────────────────────────────────────┐
│ ScreenStateManager (mutations + sync) │  ← 900 LOC
│  - updateField(name, value)           │
│  - setCurrentField(field)             │
│  - clearScreen()                      │
│  (Thread-safe with locks)             │
└───────────────────────────────────────┘
     ↑
     │ emits
     ↓
┌───────────────────────────────────────┐
│ ScreenEventBroker (listener dispatch) │  ← 600 LOC
│  - addScreenListener(l)               │
│  - fireScreenChanged(e)               │
│  - fireFieldChanged(e)                │
│  (Observer pattern, thread-safe)      │
└───────────────────────────────────────┘

Facade (for backward compatibility):
    Screen5250 → delegates to all four
```

### Phase Implementation

**Week 1: Extract ScreenBuffer**
```java
public final class ScreenBuffer {
    private final char[][] buffer;
    private final Attribute[][] attributes;

    public String getFieldContent(String fieldName) {
        // Read-only query
    }

    public char getCharAt(int row, int col) {
        // No mutations
    }

    /**
     * Snapshot of current state (defensive copy)
     */
    public ScreenSnapshot capture() {
        return new ScreenSnapshot(buffer, attributes);
    }
}
```

**Week 1.5: Extract ScreenEventBroker**
```java
public class ScreenEventBroker {
    private final List<ScreenListener> listeners =
        Collections.synchronizedList(new ArrayList<>());

    public void addScreenListener(ScreenListener l) { ... }
    public void fireScreenChanged(ScreenChangeEvent e) {
        listeners.forEach(l -> l.onScreenChange(e));
    }
}
```

**Week 2: Extract ScreenStateManager**
```java
public class ScreenStateManager {
    private final ScreenBuffer buffer;
    private final ScreenEventBroker broker;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public void updateField(String fieldName, String value) {
        lock.writeLock().lock();
        try {
            // Mutate buffer
            buffer.setFieldValue(fieldName, value);
            // Notify listeners
            broker.fireScreenChanged(new FieldUpdatedEvent(fieldName));
        } finally {
            lock.writeLock().unlock();
        }
    }
}
```

**Week 2.5: Extract ScreenRenderEngine**
```java
public class ScreenRenderEngine {
    private final ScreenBuffer buffer;
    private final Graphics2D graphics;
    private final Font font;

    public void render() {
        for (Field f : buffer.getAllFields()) {
            renderField(f);
        }
    }

    private void renderField(Field f) {
        // Swing painting logic (GUI-aware)
    }
}
```

### Backward Compatibility

**Keep Screen5250 as facade:**
```java
public class Screen5250 {
    private final ScreenBuffer buffer;
    private final ScreenStateManager stateManager;
    private final ScreenEventBroker eventBroker;
    private final ScreenRenderEngine renderer;

    // Delegate old methods to new classes
    public String getFieldContent(String name) {
        return buffer.getFieldContent(name);
    }

    public void setField(String name, String value) {
        stateManager.updateField(name, value);
    }

    // Mark old methods @Deprecated for next major release
    @Deprecated(since = "21.0", forRemoval = true)
    public void drawField(...) {
        renderer.drawField(...);
    }
}
```

### Impact on Robot Framework
- ✅ Clear read-only API (ScreenBuffer)
- ✅ Clear mutation API (ScreenStateManager)
- ✅ Easier to mock; clear responsibilities
- ✅ Easier to document; each class has one job
- ✅ No existing code breaks (facade pattern)

---

## High Priority Issue #3: SessionConfig Property Exposure (Phase 3)

### Location
- **File:** `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/src/org/hti5250j/SessionConfig.java`
- **Lines:** 59 (sesProps field), 119 (getSesProps() getter)
- **Problem:** Direct property access bypasses validation

### Current Code
```java
public class SessionConfig {
    private Properties sesProps;  // Line 59

    public Properties getSesProps() {  // Line 119
        return sesProps;  // Direct exposure!
    }
}

// Callers do this:
SessionConfig config = new SessionConfig("default.props", "session1");
String host = config.getSesProps().getProperty("host");  // Leaky!
config.getSesProps().setProperty("port", "invalid");     // No validation!
```

### Why It Blocks Automation
- No validation of property values
- No type safety (all properties are strings)
- No change notifications
- SessionConfigListener never fires for direct property mutations
- Robot Framework can't trust programmatic API

### Remediation Strategy (Phase 3 Complete)

**Step 1: Create SessionConfigBuilder (1 day)**

```java
public class SessionConfigBuilder {
    private String host;
    private int port;
    private boolean sslEnabled;
    private int rows = 24, cols = 80;

    public static SessionConfigBuilder withDefaults() {
        return new SessionConfigBuilder();
    }

    public SessionConfigBuilder host(String host) {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Host cannot be empty");
        }
        this.host = host;
        return this;
    }

    public SessionConfigBuilder port(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be 1-65535, got " + port);
        }
        this.port = port;
        return this;
    }

    public SessionConfigBuilder sslEnabled(boolean ssl) {
        this.sslEnabled = ssl;
        return this;
    }

    public SessionConfig validateAndBuild() {
        if (host == null) {
            throw new IllegalArgumentException("Host is required");
        }
        if (port == 0) {
            throw new IllegalArgumentException("Port is required");
        }

        return new SessionConfig(host, port, sslEnabled, rows, cols);
    }
}
```

**Step 2: Add Property-Specific Getters (1 day)**

```java
public class SessionConfig {
    private String host;
    private int port;
    private boolean sslEnabled;
    private int rows, cols;
    // ... others

    public String getHost() { return host; }
    public int getPort() { return port; }
    public boolean isSslEnabled() { return sslEnabled; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }

    // Deprecated property access
    @Deprecated(since = "21.0", forRemoval = true)
    public Properties getSesProps() {
        throw new UnsupportedOperationException(
            "Direct property access removed. Use host(), port(), etc.");
    }
}
```

**Step 3: Migrate Existing Callers (3 days)**

Current usage (40+ files):
```java
// OLD
config.getSesProps().getProperty("host");

// NEW
config.getHost();
```

Find all usages:
```bash
grep -r "getSesProps\|\.sesProps" src --include="*.java" | wc -l
```

Expected: 40-50 lines across multiple files.

**Step 4: Add Validation Tests (1 day)**

```java
@Test
void testPortValidation() {
    assertThrows(IllegalArgumentException.class, () ->
        SessionConfigBuilder.withDefaults()
            .host("localhost")
            .port(0)  // Invalid
            .validateAndBuild()
    );
}

@Test
void testValidConfig() {
    SessionConfig config = SessionConfigBuilder.withDefaults()
        .host("192.168.1.1")
        .port(23)
        .sslEnabled(true)
        .validateAndBuild();

    assertEquals("192.168.1.1", config.getHost());
    assertEquals(23, config.getPort());
    assertTrue(config.isSslEnabled());
}
```

### Impact on Robot Framework
- ✅ Type-safe configuration API
- ✅ Validation at build time, not runtime
- ✅ No leaky property access
- ✅ Clear error messages for bad config
- ✅ Immutable after construction (can't mutate port mid-session)

---

## High Priority Issue #4: Exception Handling (Generic Exception Throws)

### Location
- **Files:** 384 instances across codebase
- **Pattern:** `throws Exception` or bare catches

### Examples
```java
// Current
public void connect() throws Exception { ... }  // What could go wrong?
public Screen5250 getScreen() throws Exception { ... }

// Caller can't distinguish:
try {
    session.connect();
} catch (Exception e) {
    // Is it a connection timeout? Network down? Bad credentials?
    // Is it retryable? Fatal?
}
```

### Remediation Strategy

**Step 1: Create Exception Hierarchy (1 day)**

```java
/**
 * Base exception for all workflow-related errors.
 * Sealed to prevent unchecked exceptions leaking.
 */
public sealed class WorkflowException extends Exception
    permits ConnectionException,
            ProtocolException,
            ValidationException,
            TimeoutException,
            AuthenticationException {

    private final ErrorCode errorCode;
    private final long timestamp = System.currentTimeMillis();

    public WorkflowException(ErrorCode code, String message) {
        super(message);
        this.errorCode = code;
    }

    public WorkflowException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = code;
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public long getTimestamp() { return timestamp; }
    public boolean isRetryable() {
        return errorCode.isRetryable();
    }
}

public non-sealed class ConnectionException extends WorkflowException {
    private final int retryCount;
    private final Duration nextRetryIn;

    public ConnectionException(String host, int port, Throwable cause) {
        super(ErrorCode.CONNECTION_FAILED,
            "Failed to connect to " + host + ":" + port, cause);
    }

    public boolean isRetryable() { return true; }
}

public non-sealed class AuthenticationException extends WorkflowException {
    public AuthenticationException(String message) {
        super(ErrorCode.AUTHENTICATION_FAILED, message);
    }

    public boolean isRetryable() { return false; }
}

public non-sealed class TimeoutException extends WorkflowException {
    private final Duration timeout;

    public TimeoutException(Duration timeout) {
        super(ErrorCode.TIMEOUT,
            "Operation timed out after " + timeout);
        this.timeout = timeout;
    }

    public boolean isRetryable() { return true; }
}

public enum ErrorCode {
    CONNECTION_FAILED(true, "C001"),
    AUTHENTICATION_FAILED(false, "A001"),
    PROTOCOL_ERROR(false, "P001"),
    VALIDATION_ERROR(false, "V001"),
    TIMEOUT(true, "T001");

    private final boolean retryable;
    private final String code;

    ErrorCode(boolean retryable, String code) {
        this.retryable = retryable;
        this.code = code;
    }

    public boolean isRetryable() { return retryable; }
    public String getCode() { return code; }
}
```

**Step 2: Update Key Methods (2 days)**

```java
// BEFORE
public Session5250 connect() throws Exception { ... }
public void sendKeys(String keys) throws Exception { ... }

// AFTER
public Session5250 connect() throws WorkflowException {
    try {
        // connection logic
    } catch (IOException e) {
        if (e instanceof ConnectException) {
            throw new ConnectionException(host, port, e);
        }
        throw new ProtocolException("Unexpected I/O error", e);
    } catch (SecurityException e) {
        throw new AuthenticationException(e.getMessage());
    }
}

public void sendKeys(String keys) throws WorkflowException {
    try {
        // send logic
    } catch (SocketTimeoutException e) {
        throw new TimeoutException(Duration.ofSeconds(30));
    }
}
```

**Step 3: Update Callers**

```java
// Python code (now with better error handling)
try:
    session.connect()
except hti5250j.ConnectionException as e:
    if e.isRetryable():
        # Wait and retry
        time.sleep(1)
    else:
        # Fatal error
        raise
except hti5250j.AuthenticationException as e:
    # User provided wrong credentials
    print(f"Auth failed: {e.getMessage()}")
```

### Impact on Robot Framework
- ✅ Python can distinguish error types
- ✅ Retry logic can be automatic vs manual
- ✅ Better error messages with error codes
- ✅ Timestamp helps with troubleshooting
- ✅ Sealed exceptions prevent mistakes

---

## Medium Priority Issue #5: Missing PythonBridge Adapter

### Location
- **Missing:** No dedicated adapter for Python/Robot Framework use cases

### Why Needed
Current approach:
```
Python → (JNI/Jython) → Session5250 → Screen5250 (3,400 lines)
         Direct coupling, hard to understand
```

Better approach:
```
Python → WorkflowAPI (JSON/gRPC) → PythonBridge → WorkflowExecutor
         Clean contract, serializable events
```

### Remediation Strategy

**Create `PythonBridge.java` (3 days)**

```java
public class PythonBridge {

    /**
     * Serialize workflow execution result to JSON.
     * Python caller gets machine-readable output.
     */
    public static String serializeWorkflowResult(WorkflowResult result) {
        return new JSONBuilder()
            .put("success", result.isSuccessful())
            .put("completedSteps", result.getCompletedStepCount())
            .put("totalSteps", result.getTotalStepCount())
            .put("durationMs", result.getDurationMs())
            .put("artifacts",
                result.getArtifacts().stream()
                    .collect(toMap(
                        Artifact::getName,
                        Artifact::getPath
                    )))
            .put("errors",
                result.getErrors().stream()
                    .map(PythonBridge::serializeError)
                    .collect(toList()))
            .build();
    }

    private static String serializeError(ValidationError error) {
        return new JSONBuilder()
            .put("step", error.stepIndex())
            .put("field", error.fieldName())
            .put("message", error.message())
            .put("code", error.errorCode())
            .put("retryable", error.isRetryable())
            .build();
    }

    /**
     * Serialize screen state to JSON snapshot.
     */
    public static String serializeScreenState(ScreenBuffer screen) {
        return new JSONBuilder()
            .put("cursorRow", screen.getCursorRow())
            .put("cursorCol", screen.getCursorCol())
            .put("messageText", screen.getMessageLineText())
            .put("fields",
                screen.getAllFields().stream()
                    .collect(toMap(
                        Field::getName,
                        f -> new JSONBuilder()
                            .put("content", f.getContent())
                            .put("row", f.getRow())
                            .put("col", f.getCol())
                            .put("length", f.getLength())
                            .put("protected", f.isProtected())
                            .build()
                    )))
            .build();
    }

    /**
     * Deserialize Action from JSON (sent by Python).
     */
    public static Action deserializeAction(String json) {
        // Parse JSON
        // Dispatch to appropriate Action constructor
        // Return strongly-typed Action
    }
}
```

**Create `WorkflowAPIService.java` (2 days)**

```java
public class WorkflowAPIService {

    /**
     * Primary API: Execute workflow from YAML.
     */
    public String executeWorkflow(String yamlPath, String dataFilePath) {
        WorkflowResult result = WorkflowLoader
            .fromFile(yamlPath)
            .withDataFile(dataFilePath)
            .execute();

        return PythonBridge.serializeWorkflowResult(result);
    }

    /**
     * Query current screen state without executing.
     */
    public String getScreenState() {
        ScreenBuffer screen = session.getScreen().getScreenBuffer();
        return PythonBridge.serializeScreenState(screen);
    }

    /**
     * Send individual keystroke.
     */
    public String sendKeys(String keys) throws WorkflowException {
        session.sendKeys(keys);
        return PythonBridge.serializeScreenState(
            session.getScreen().getScreenBuffer());
    }

    /**
     * Get field value.
     */
    public String getFieldValue(String fieldName) {
        return session.getScreen()
            .getScreenBuffer()
            .getFieldContent(fieldName);
    }
}
```

### Impact on Robot Framework
- ✅ Clean, serializable API
- ✅ JSON responses understood by all languages
- ✅ No JNI complexity
- ✅ Can be exposed via gRPC/REST easily
- ✅ Python sees structure, not Java objects

---

## Summary: All Remediations Ranked

| Issue | Effort | Impact | Priority | Blocker |
|-------|--------|--------|----------|---------|
| Extract Tn5250Protocol from tnvt | 2 days | HIGH | CRITICAL | YES |
| Decompose Screen5250 God Object | 2 weeks | CRITICAL | CRITICAL | YES |
| Complete SessionConfig Phase 3 | 2 days | HIGH | HIGH | NO |
| Create Exception Hierarchy | 1 week | HIGH | HIGH | NO |
| Build PythonBridge Adapter | 3 days | MEDIUM | MEDIUM | NO |
| Add gRPC Service Layer | 2 weeks | MEDIUM | MEDIUM | NO |
| Stream API modernization | 5 days | LOW | LOW | NO |

### Minimum Viable Path (Robot Framework Support)

**Total: 4 weeks**

1. Extract Tn5250Protocol (2 days) — **BLOCKER #1**
2. Decompose Screen5250 (2 weeks) — **BLOCKER #2**
3. Complete SessionConfig Phase 3 (2 days)
4. Create Exception Hierarchy (1 week)
5. Build PythonBridge Adapter (3 days)

**After this:** Robot Framework can reliably orchestrate HTI5250J workflows.

---

## Validation Checklist

After implementing remediations, verify:

- [ ] No `javax.swing` imports in `framework/tn5250/` package
- [ ] Screen5250 < 600 lines (or broken into 4 classes)
- [ ] SessionConfig has zero `getSesProps()` calls (all migrated)
- [ ] All public methods throw checked WorkflowException subclasses
- [ ] PythonBridge serializes all public types to JSON
- [ ] Robot Framework can execute sample workflow without GUI
- [ ] Python client can parse all JSON responses
- [ ] Existing tests still pass (no regressions)
- [ ] Headless mode documentation added
- [ ] Python/Robot Framework integration examples added

