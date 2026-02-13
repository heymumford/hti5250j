# HTI5250J API Ergonomics & Developer Experience Assessment
**Date:** February 13, 2026
**Audience:** API design review, developer onboarding, SDK 1.0 planning
**Assessment Type:** Comprehensive DX audit covering API design, naming, error handling, and onboarding friction

---

## Executive Summary

HTI5250J has a **well-intentioned but fragmented API surface**. The core issue is **dual API personalities**: a legacy `Session5250` interface (backward-compatible, event-driven, listener-based) coexists with a modern `HeadlessSession` interface (data-focused, blocking, exception-based).

**The verdict:** For a 1.0 SDK, this creates **decision paralysis and documentation burden** for new users. The project needs to pick one API direction and provide clear migration paths.

**Rating: 6.5/10**
- **Strengths:** HeadlessSession API is clean and well-documented; good exception hierarchy; comprehensive tests
- **Weaknesses:** Naming inconsistency (sendKeys vs sendString); no fluent API builder; sparse error context; SessionConfig complexity; no connection pooling API; unclear async/blocking semantics

---

## 1. API Ergonomics Assessment

### 1.1 Current API Surface

**Primary Entry Points:**
```
org.hti5250j.Session5250           (legacy, event-driven)
org.hti5250j.interfaces.HeadlessSession  (modern, programmatic)
org.hti5250j.workflow.WorkflowRunner      (declarative, YAML-based)
org.hti5250j.headless.HeadlessSession    (separate copy of API?!)
```

**Problem #1: API Duplication**
- `org.hti5250j.interfaces.HeadlessSession` (interface, 223 lines, well-documented)
- `org.hti5250j.headless.HeadlessSession` (class, 120 lines, minimal docs)

These are **not the same thing** but have identical names. This is catastrophic for discoverability. A new user searching for "HeadlessSession" gets two different classes in different packages with different implementations.

**Recommendation:** Consolidate these immediately. Keep the interface-based design (composition over inheritance) from `org.hti5250j.interfaces.HeadlessSession`, delete `org.hti5250j.headless.HeadlessSession`.

---

### 1.2 Naming Inconsistency

**Critical Issue: Method Naming Variance**

| Method | Class | Semantics | Issue |
|--------|-------|-----------|-------|
| `sendKeys(String)` | HeadlessSession | Send mnemonic keys (`"[enter]"`, `"[f1]"`) | Inconsistent with... |
| `sendString(String)` | Session5250 | Send literal EBCDIC string | ...similar intent, different names |
| `sendKey(KeyCode)` | Session5250 | Send AID key by code | Three different APIs for one concept |
| `sendTab()` | Session5250 | Helper for TAB | Convenience method, but inconsistent pattern |

**Impact:** A developer trying to connect Session5250 + HeadlessSession APIs (which they should, given the README examples) encounters **three naming patterns for keystroke submission**:

```java
// Pattern 1: HeadlessSession (mnemonic strings)
headless.sendKeys("USER[tab]PASSWORD[enter]");

// Pattern 2: Session5250 (literal strings + key codes)
session.sendString("USER");
session.sendTab();
session.sendString("PASSWORD");
session.sendKeys(new KeyCode(AID_ENTER));

// Pattern 3: Session5250 (helper methods)
session.sendEnter();
session.sendTab();
```

**Recommendation for 1.0:**
- **Normalize to HeadlessSession API** as the primary interface (already in README examples)
- Mnemonic strings are more discoverable and user-friendly than key codes
- Session5250 helpers (sendEnter, sendTab) should be thin wrappers: `sendKeys("[enter]")`
- Deprecate Session5250.sendString() in favor of mnemonic-based sendKeys()

**Before/After:**
```java
// BEFORE (confusing)
session.sendString("USER");
session.sendTab();
session.sendKeys(someKeyCode);

// AFTER (consistent)
session.sendKeys("USER[tab]");
```

---

### 1.3 HeadlessSession API Design

**Strengths:**
- Clean interface contract (11 methods, clear responsibilities)
- Excellent Javadoc with examples and error cases
- Composition-based design (wraps Session5250, doesn't inherit)
- Proper exception typing (IllegalStateException, TimeoutException)

**Weaknesses:**

1. **Missing convenience methods**
   ```java
   // User has to do this:
   headless.sendKeys("USER");
   headless.sendKeys("[tab]");
   headless.sendKeys("PASSWORD");
   headless.sendKeys("[enter]");
   headless.waitForKeyboardLockCycle(5000);

   // Should support this:
   headless.sendKeys("USER[tab]PASSWORD[enter]")
           .waitForKeyboardLockCycle(5000);  // Fluent API
   ```

2. **No builder pattern for session creation**
   ```java
   // Current: scattered configuration
   Properties props = new Properties();
   props.setProperty("host", "...");
   props.setProperty("port", "23");
   Session5250 session = new Session5250(props, resource, name, config);
   HeadlessSession headless = session.asHeadlessSession();

   // Desired: fluent builder
   HeadlessSession headless = HeadlessSessionBuilder
       .to("ibmi.example.com", 23)
       .withCodePage(CodePage.CCSID37)
       .withTimeout(Duration.ofSeconds(30))
       .withSSL(true, trustStore)
       .build();
   ```

3. **Missing field-level API**
   ```java
   // User wants to fill form fields by name
   // Currently has to:
   headless.getScreen().getField("account_id");  // Exposes internal Screen5250 API

   // Should have:
   headless.fillField("account_id", "12345");
   headless.getFieldValue("confirmation_msg");
   ```

4. **Async/blocking semantics unclear**
   - `connect()` is asynchronous (spawns daemon thread) but returns immediately
   - `sendKeys()` is synchronous (blocks until keyboard processed)
   - `waitForKeyboardUnlock()` is blocking with timeout
   - **No consistent pattern for timeout handling** — sometimes milliseconds, sometimes Duration

5. **Screenshot capture is expensive but not documented**
   ```java
   // No indication this renders the entire 24x80 character grid
   // Could be slow in tight loops
   BufferedImage img = headless.captureScreenshot();  // ~1-2ms, but opaque
   ```

---

### 1.4 Session5250 API Issues

**Design Problem: Listener Pattern Anti-Pattern**

```java
public interface SessionInterface {
    void addSessionListener(SessionListener listener);
    void removeSessionListener(SessionListener listener);
}

// User has to implement this:
session.addSessionListener(new SessionListener() {
    @Override
    public void onSessionChanged(SessionChangeEvent event) {
        if (event.getState() == SessionListener.CONNECTED) {
            // Boilerplate callback
        }
    }
});
```

This is **1990s Java Swing patterns**. Modern Java has **CompletableFuture, callbacks, and reactive streams**. Requiring event listeners for basic connection management is ergonomically wrong for headless automation.

**Better approach:**
```java
// Use CompletableFuture for connection lifecycle
headless.connectAsync()
    .thenRun(() -> System.out.println("Connected"))
    .exceptionally(ex -> {
        log.error("Connection failed", ex);
        return null;
    });

// Or blocking with timeout semantics:
headless.connect(Duration.ofSeconds(10));
```

---

## 2. Naming Consistency Review

### 2.1 Method Naming Patterns

**Pattern 1: Verb-Based (Good)**
- `connect()` ✓
- `disconnect()` ✓
- `sendKeys()` ✓
- `getScreenText()` ✓
- `waitForKeyboardUnlock()` ✓

**Pattern 2: Property Accessors (Inconsistent)**
- `getSessionName()` — standard JavaBean style ✓
- `getScreen()` — but returns Screen5250, not just data
- `getConnectionProperties()` — returns mutable Properties object (bad!)
- `getConfiguration()` — ambiguous (which configuration?)

**Pattern 3: Predicates (Good)**
- `isConnected()` ✓

**Pattern 4: Void Operations with Side Effects (Problematic)**
- `signalBell()` — unclear what "bell" means in headless context
- `handleSystemRequest()` — returns String (side effect), method name suggests it returns void

**Recommendation:**
```java
// BEFORE
Properties props = headless.getConnectionProperties();
props.setProperty("host", "newhost");  // Mutations leak!

// AFTER: Immutable accessor
Map<String, String> props = headless.getConnectionProperties();
// Read-only, or:
String host = headless.getHost();
int port = headless.getPort();
```

---

### 2.2 Class Naming Conventions

**Strengths:**
- `Session5250` clearly indicates IBM 5250 domain
- `HeadlessSession` clearly indicates no GUI
- `Screen5250` clearly indicates 5250 screen model
- `RequestHandler` clearly indicates extension point

**Weaknesses:**
- `DefaultHeadlessSession` — "Default" suggests multiple implementations exist; is there a NonDefaultHeadlessSession?
- `NullRequestHandler` — "Null" pattern is confusing; should be `NoOpRequestHandler` or `AutoReturnRequestHandler`
- `tnvt` — **nonsensical class name** (legacy); should be `TelnetVirtualTerminal` or `TN5250ETransport`
- Package names inconsistent: `org.hti5250j.interfaces` vs `org.hti5250j.headless` vs `org.hti5250j.session`

---

## 3. Error Handling DX Analysis

### 3.1 Exception Hierarchy

**Current Design:**
```
Throwable
├── Exception
│   ├── TimeoutException (java.util.concurrent)
│   ├── InterruptedException (java.lang)
│   ├── NavigationException (custom)
│   ├── AssertionException (custom)
│   └── IllegalStateException (standard)
```

**Issues:**

1. **TimeoutException not imported from org.hti5250j.***
   ```java
   // User gets this
   throw new java.util.concurrent.TimeoutException("Keyboard locked after 5000ms");

   // But java.util.concurrent isn't in HTI5250J scope
   // Should be:
   throw new org.hti5250j.TimeoutException("Keyboard locked after 5000ms");
   ```

2. **No context in exception messages**
   ```java
   // Current error message
   "Keyboard locked after 5000ms"

   // Better error context
   "Keyboard locked after 5000ms at host ibmi.example.com (session: prod-01). Screen state: [first 100 chars]. Recommendations: (1) Increase timeout, (2) Check AS/400 system for hung jobs"
   ```

3. **No structured error codes**
   ```java
   // Can't programmatically handle specific errors
   try {
       headless.waitForKeyboardUnlock(5000);
   } catch (TimeoutException e) {
       // Is this a network timeout? Keyboard locked? Connection loss?
       // e.getMessage() doesn't say.
   }

   // Better approach:
   try {
       headless.waitForKeyboardUnlock(5000);
   } catch (TimeoutException e) {
       if (e.getErrorCode() == ErrorCode.KEYBOARD_TIMEOUT) {
           // Retry with longer timeout
       } else if (e.getErrorCode() == ErrorCode.NETWORK_TIMEOUT) {
           // Reconnect
       }
   }
   ```

4. **Silent failures in screen operations**
   ```java
   String screenText = headless.getScreenAsText();  // Never throws
   // But what if screen is corrupted? Encoding failed? Returns empty string silently.
   ```

5. **No error recovery guidance**
   - Exceptions don't suggest next steps
   - No documented retry patterns
   - No exponential backoff helpers

### 3.2 Connection Error Scenarios

**Missing error handling for these scenarios:**

| Scenario | Current Behavior | Desired Behavior |
|----------|------------------|------------------|
| Host unreachable | IOException (not caught, bubbles to client) | `ConnectionException("Unable to reach ibmi.example.com:23. Check hostname/DNS.")` |
| SSL handshake failure | SSLException (not caught) | `SecurityException("TLS handshake failed. Check: (1) Certificate validity, (2) Server cipher compatibility")` |
| Authentication failure | No special handling | `AuthenticationException("SYSREQ dialog not auto-handled. Implement custom RequestHandler.")` |
| Keyboard timeout after N retries | TimeoutException | `KeyboardTimeoutException` with retry count + last screen state |
| Unexpected disconnect | IllegalStateException | `ConnectionClosedException` with reason code (clean vs abnormal) |

---

## 4. Onboarding Friction Analysis

### 4.1 Documentation Gaps

**README Quick Start:**
```java
TN5250Session session = new TN5250Session("192.168.1.100", 23);
session.connect();
session.sendKeys("USER");
session.sendKeys("PASSWORD");
String screen = session.getScreenText();
session.disconnect();
```

**Problems:**
1. **Class name `TN5250Session` doesn't exist** in current codebase. (Should be `Session5250`)
2. No explanation of mnemonic syntax (`[enter]` vs literal text)
3. No error handling shown (what if connect() fails?)
4. No async connection semantics (connect() returns immediately, but connection happens in thread)
5. No timeout configuration shown
6. `sendKeys("USER")` is not the same as `sendString("USER")`

### 4.2 First-Time User Onboarding Friction

**Scenario: User wants to connect, authenticate, read screen**

```java
// What they try first (from README)
Session5250 session = new Session5250(...);
session.connect();
session.sendKeys("USER");
session.sendKeys("PASSWORD");
String screen = session.getScreenText();  // FAILS: NullPointerException
```

**Why it fails:**
1. `connect()` is async; connection not established yet
2. `sendKeys()` on Session5250 uses key codes, not mnemonics
3. `getScreenText()` on Session5250 doesn't exist (only on HeadlessSession)

**User confusion:** "Which API should I use? Session5250 or HeadlessSession?"

**Better onboarding:**
```java
// Step 1: Clear, documented entry point
HeadlessSession session = new HeadlessSessionBuilder()
    .connect("192.168.1.100", 23)
    .build();  // Synchronous, clear error handling

// Step 2: Intuitive interaction
session.sendKeys("USER[tab]PASSWORD[enter]");
session.waitForKeyboardUnlock(5000);  // Clear blocking semantics

// Step 3: Data retrieval
String screen = session.getScreenAsText();  // Clear return type
if (screen.contains("Main Menu")) {
    System.out.println("Authentication successful");
}
```

---

### 4.3 SessionConfig Complexity

**Current design requires:**
```java
Properties props = createSessionProperties();
String configResource = "session.properties";
String sessionName = "example-session";
SessionConfig config = new SessionConfig(configResource, sessionName);
Session5250 session = new Session5250(props, configResource, sessionName, config);
```

**Problems:**
1. Five parameters for one concept (session connection)
2. No clear semantics for `configResource` vs `sessionName`
3. SessionConfig constructor unclear (what's `configResource`?)
4. Properties object is mutable; configuration can be changed after creation

**Better design:**
```java
SessionConfig config = SessionConfig.builder()
    .host("ibmi.example.com")
    .port(23)
    .codePage(CodePage.CCSID37)
    .screenSize(80, 24)
    .connectTimeout(Duration.ofSeconds(10))
    .readTimeout(Duration.ofSeconds(30))
    .ssl(SSLMode.TLS, trustStore)
    .name("production-session")
    .build();

HeadlessSession session = config.createSession();
session.connect();
```

---

## 5. API Improvement Recommendations

### 5.1 Builder Pattern for Session Creation

**Recommendation: Introduce `HeadlessSessionBuilder`**

```java
public class HeadlessSessionBuilder {
    private String host;
    private int port = 23;
    private CodePage codePage = CodePage.CCSID37;
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration readTimeout = Duration.ofSeconds(30);
    private int screenRows = 24;
    private int screenColumns = 80;
    private SSLConfig sslConfig;
    private RequestHandler requestHandler;

    public HeadlessSessionBuilder to(String host, int port) {
        this.host = host;
        this.port = port;
        return this;
    }

    public HeadlessSessionBuilder withCodePage(CodePage codePage) {
        this.codePage = codePage;
        return this;
    }

    public HeadlessSessionBuilder withTimeout(Duration timeout) {
        this.connectTimeout = timeout;
        return this;
    }

    public HeadlessSessionBuilder withSSL(String truststorePath, String password) {
        this.sslConfig = new SSLConfig(truststorePath, password);
        return this;
    }

    public HeadlessSessionBuilder withCustomRequestHandler(RequestHandler handler) {
        this.requestHandler = handler;
        return this;
    }

    public HeadlessSession build() throws ConfigurationException {
        validate();
        SessionConfig config = new SessionConfig(...);
        Session5250 session = new Session5250(...);
        return new DefaultHeadlessSession(session, requestHandler);
    }

    public HeadlessSession connect() throws ConnectionException {
        HeadlessSession session = build();
        session.connect();
        waitForConnected(connectTimeout);
        return session;
    }
}
```

**Usage:**
```java
HeadlessSession session = HeadlessSessionBuilder
    .to("ibmi.example.com", 23)
    .withCodePage(CodePage.CCSID37)
    .withTimeout(Duration.ofSeconds(30))
    .build();

session.connect();
session.sendKeys("USER[tab]PASSWORD[enter]");
```

---

### 5.2 Fluent API for Session Interactions

**Recommendation: Chainable operations**

```java
public interface HeadlessSession {
    HeadlessSession sendKeys(String keys) throws IllegalStateException;
    HeadlessSession fillField(String fieldName, String value) throws FieldNotFoundException;
    HeadlessSession pressKey(FunctionKey key) throws IllegalStateException;
    HeadlessSession waitForKeyboardUnlock(Duration timeout) throws TimeoutException;
    HeadlessSession waitForText(String text, Duration timeout) throws TimeoutException;
    HeadlessSession screenshot(String filename) throws IOException;
    String getScreenAsText();
}
```

**Usage:**
```java
session.sendKeys("CALL MYPROG")
       .pressKey(FunctionKey.ENTER)
       .waitForKeyboardUnlock(Duration.ofSeconds(5))
       .waitForText("Processing complete", Duration.ofSeconds(10))
       .screenshot("artifacts/success.png");
```

---

### 5.3 High-Level Field API

**Recommendation: Field-level interaction**

```java
public interface HeadlessSession {
    ScreenField getField(String fieldName) throws FieldNotFoundException;
    String getFieldValue(String fieldName) throws FieldNotFoundException;
    HeadlessSession fillField(String fieldName, String value) throws FieldNotFoundException;
    List<String> getFieldLabels();
    List<ScreenField> getFields();
}

public interface ScreenField {
    String getName();
    String getValue();
    int getRow();
    int getColumn();
    int getLength();
    void setValue(String value) throws FieldValidationException;
    boolean isProtected();
    boolean isRequired();
}
```

**Usage:**
```java
// Current (field access requires Screen API knowledge)
session.getScreen().getField(row, col);

// Desired (high-level field API)
String account = session.getFieldValue("account_id");
session.fillField("amount", "999.99");
session.fillField("reason", "Invoice 2026-001");
session.pressKey(FunctionKey.ENTER);
```

---

### 5.4 Connection Pooling API

**Recommendation: Session pool for concurrent testing**

```java
public interface HeadlessSessionPool extends AutoCloseable {
    void start();
    void shutdown(Duration gracefulShutdown);
    HeadlessSession borrowSession(Duration timeout) throws TimeoutException;
    void returnSession(HeadlessSession session);
    PoolStatistics getStatistics();

    static HeadlessSessionPool create(String host, int port) {
        return new DefaultHeadlessSessionPool(host, port);
    }
}

public class HeadlessSessionPoolBuilder {
    public HeadlessSessionPoolBuilder minSize(int minSize) { ... }
    public HeadlessSessionPoolBuilder maxSize(int maxSize) { ... }
    public HeadlessSessionPoolBuilder borrowTimeout(Duration timeout) { ... }
    public HeadlessSessionPoolBuilder idleTimeout(Duration timeout) { ... }
    public HeadlessSessionPool build() { ... }
}
```

**Usage:**
```java
HeadlessSessionPool pool = HeadlessSessionPoolBuilder
    .to("ibmi.example.com", 23)
    .minSize(5)
    .maxSize(20)
    .borrowTimeout(Duration.ofSeconds(5))
    .build();

pool.start();

// Concurrent use
try (HeadlessSession session = pool.borrowSession(Duration.ofSeconds(5))) {
    session.sendKeys("CALL MYPROG[enter]");
    String result = session.getScreenAsText();
} finally {
    pool.shutdown(Duration.ofSeconds(10));
}
```

---

### 5.5 Async/Reactive API

**Recommendation: Non-blocking API for high-concurrency scenarios**

```java
public interface AsyncHeadlessSession {
    CompletableFuture<Void> connectAsync();
    CompletableFuture<Void> sendKeysAsync(String keys);
    CompletableFuture<String> getScreenAsTextAsync();
    CompletableFuture<Void> waitForKeyboardUnlockAsync(Duration timeout);
    CompletableFuture<BufferedImage> captureScreenshotAsync();
}

// Or reactive streams:
public interface ReactiveHeadlessSession {
    Mono<Void> connect();
    Mono<Void> sendKeys(String keys);
    Mono<String> getScreenAsText();
    Flux<ScreenUpdate> onScreenChanged();
}
```

**Usage:**
```java
AsyncHeadlessSession async = session.asAsync();

async.connectAsync()
    .thenCompose(v -> async.sendKeysAsync("USER[tab]PASSWORD[enter]"))
    .thenCompose(v -> async.waitForKeyboardUnlockAsync(Duration.ofSeconds(5)))
    .thenCompose(v -> async.captureScreenshotAsync())
    .thenAccept(img -> ImageIO.write(img, "PNG", new File("screenshot.png")))
    .exceptionally(ex -> {
        log.error("Async automation failed", ex);
        return null;
    });
```

---

## 6. Documentation-Driven API Design

### 6.1 Missing Documentation Sections

**For 1.0 API stability, add:**

1. **API Reference (Javadoc)**
   - [ ] Every public class has class-level Javadoc
   - [ ] Every public method has method-level Javadoc
   - [ ] @param, @return, @throws documented
   - [ ] Examples for complex methods (sendKeys with mnemonics, waitForKeyboardUnlock)

2. **Error Handling Guide**
   - [ ] Common exceptions and recovery strategies
   - [ ] Retry patterns (with exponential backoff)
   - [ ] Timeout configuration best practices
   - [ ] Connection loss recovery

3. **Migration Guides**
   - [ ] Session5250 → HeadlessSession (current code)
   - [ ] Legacy GUI listeners → CompletableFuture
   - [ ] Synchronous → Asynchronous patterns

4. **Performance Guide**
   - [ ] Throughput characteristics (messages/sec)
   - [ ] Latency SLAs (connect, read, write)
   - [ ] Screenshot rendering cost
   - [ ] Pool sizing recommendations

5. **Security Guide**
   - [ ] SSL/TLS configuration
   - [ ] Credential handling (no plaintext in logs)
   - [ ] Connection string best practices
   - [ ] Secrets management (env vars vs config files)

### 6.2 API Documentation Quality Gaps

**Example: HeadlessSession.sendKeys()**

**Current Javadoc:**
```java
/**
 * Send keys to the host (synchronous operation).
 *
 * Mnemonic syntax:
 * - [enter] — ENTER key
 * - [tab] — TAB key
 * - [f1] through [f24] — Function keys
 * ...
 * Example: "sendKeys("CALL MYPGM[enter]")" sends literal text + ENTER
 *
 * @param keys mnemonic key sequence
 * @throws IllegalStateException if not connected
 * @throws IllegalArgumentException if mnemonic syntax invalid
 */
void sendKeys(String keys);
```

**Better Documentation:**
```java
/**
 * Send keys to the host (synchronous operation).
 *
 * This method sends a sequence of keystrokes to the IBM i host. It supports:
 * - Literal text characters (automatically EBCDIC-encoded)
 * - Mnemonics for special keys (see mnemonic table below)
 * - Multiple keys in one call (e.g., "USER[tab]PASSWORD[enter]")
 *
 * Mnemonic Syntax (case-insensitive):
 * <table>
 *   <tr><th>Mnemonic</th><th>Key</th><th>Examples</th></tr>
 *   <tr><td>[enter]</td><td>ENTER (AID key)</td><td>sendKeys("[enter]")</td></tr>
 *   <tr><td>[tab]</td><td>TAB</td><td>sendKeys("VALUE[tab]")</td></tr>
 *   <tr><td>[f1]-[f24]</td><td>Function keys</td><td>sendKeys("[f5]")</td></tr>
 *   <tr><td>[backspace]</td><td>Backspace</td><td>sendKeys("[backspace]")</td></tr>
 * </table>
 *
 * Execution Semantics:
 * - Synchronous: blocks until keys are sent to the host
 * - No wait for host response: use {@link #waitForKeyboardUnlock(int)} separately
 * - Character encoding: automatic conversion to EBCDIC using session code page
 *
 * Performance:
 * - ~50-100ms for typical keystroke submission
 * - Scales linearly with input length
 *
 * @param keys mnemonic key sequence (non-null, non-empty)
 * @throws IllegalStateException if session not connected
 * @throws IllegalArgumentException if mnemonic syntax invalid (e.g., "[invalid]")
 * @throws RuntimeException if EBCDIC encoding fails (e.g., unsupported character)
 *
 * @example
 * // Simple login
 * session.sendKeys("USER[tab]PASSWORD[enter]");
 *
 * // Multi-step navigation
 * session.sendKeys("CALL MYPROG");
 * session.sendKeys("[enter]");
 * session.waitForKeyboardUnlock(5000);
 *
 * // Tab-separated fields
 * session.sendKeys("Field1[tab]Field2[tab]Field3");
 *
 * @see #waitForKeyboardUnlock(int)
 * @see #waitForKeyboardLockCycle(int)
 * @see HeadlessSessionBuilder
 * @since 0.12.0
 */
void sendKeys(String keys) throws IllegalStateException, IllegalArgumentException;
```

---

## 7. SDK Design Patterns for 1.0

### 7.1 Recommended API Layers

**Layer 1: Low-Level (Protocol)**
- Class: `TN5250ETransport` (renamed from `tnvt`)
- Purpose: Raw telnet + 5250 protocol handling
- Audience: Advanced users, protocol researchers
- Visibility: Package-private by default

**Layer 2: Mid-Level (Session)**
- Class: `HeadlessSession` (interface)
- Purpose: Terminal session lifecycle (connect, disconnect, I/O)
- Audience: Test automation, integration testing
- Visibility: Public, stable API

**Layer 3: High-Level (Domain)**
- Class: `FormSession` (proposed)
- Purpose: Form-based interaction (fill field, submit, assert)
- Audience: Business automation, workflow engineers
- Visibility: Public, but beta (subject to change)

**Layer 4: Orchestration (Workflow)**
- Class: `WorkflowRunner` (existing)
- Purpose: YAML-based scenario execution
- Audience: CI/CD pipelines, non-developers
- Visibility: Public, stable API

### 7.2 Version Strategy

**0.12.x (Current):** Stabilize HeadlessSession API
- Fix naming inconsistencies
- Remove HeadlessSession class duplicate
- Add comprehensive error handling
- Improve exception messages

**0.13.x (Planned):** Add convenience layers
- Introduce `HeadlessSessionBuilder`
- Add field-level API (`FormSession`)
- Add session pooling API
- Deprecate Session5250 listeners

**1.0 (Goal):** Stable, production-ready
- Remove all deprecated APIs
- Remove Session5250 from public surface (only HeadlessSession)
- Finalize error codes and exception hierarchy
- Complete documentation and examples

---

## 8. Summary of Recommendations

| Priority | Issue | Recommendation | Effort | Impact |
|----------|-------|----------------|--------|--------|
| **P0** | HeadlessSession class duplicate | Delete `/src/org/hti5250j/headless/HeadlessSession.java` | Low | High |
| **P0** | Naming inconsistency (sendKeys vs sendString) | Normalize to mnemonic-based `sendKeys()` | Medium | High |
| **P1** | No builder pattern | Add `HeadlessSessionBuilder` | Medium | High |
| **P1** | Sparse error context | Add error codes + recovery guidance | Medium | Medium |
| **P1** | Session creation complexity | Simplify via builder, hide Properties | Medium | High |
| **P2** | Missing field-level API | Add `getFieldValue()`, `fillField()` | Medium | Medium |
| **P2** | No fluent API | Make `sendKeys()`, `waitFor*()` chainable | Low | Medium |
| **P2** | Async/blocking unclear | Document with examples, add async layer | High | Medium |
| **P3** | SessionConfig over-complex | Simplify, validate in builder | Low | Low |
| **P3** | Listener pattern outdated | Add `connectAsync()` with CompletableFuture | High | Medium |

---

## 9. Examples for Documentation

### 9.1 "Happy Path" Example

```java
// Connect and run a simple transaction
HeadlessSession session = HeadlessSessionBuilder
    .to("ibmi.example.com", 23)
    .withCodePage(CodePage.CCSID37)
    .build();

try {
    session.connect();

    // Login
    session.sendKeys("USER[tab]PASSWORD[enter]");
    session.waitForKeyboardUnlock(Duration.ofSeconds(10));

    // Navigate
    session.sendKeys("CALL MYPROG[enter]");
    session.waitForText("Form Entry", Duration.ofSeconds(5));

    // Interact
    session.fillField("account_id", "123456");
    session.fillField("amount", "999.99");
    session.pressKey(FunctionKey.ENTER);
    session.waitForKeyboardUnlock(Duration.ofSeconds(5));

    // Verify
    String screen = session.getScreenAsText();
    assert screen.contains("Transaction accepted");

} catch (ConnectionException e) {
    System.err.println("Failed to connect: " + e.getMessage());
} catch (TimeoutException e) {
    System.err.println("Operation timed out: " + e.getMessage());
} finally {
    session.disconnect();
}
```

### 9.2 Error Handling Example

```java
// Robust error handling with retry
int maxRetries = 3;
Duration retryDelay = Duration.ofSeconds(2);

for (int attempt = 1; attempt <= maxRetries; attempt++) {
    try {
        session.connect(Duration.ofSeconds(10));
        break;  // Success
    } catch (ConnectionException e) {
        if (attempt < maxRetries) {
            System.out.println("Connection failed, retrying in " + retryDelay.getSeconds() + "s");
            Thread.sleep(retryDelay.toMillis());
        } else {
            throw new RuntimeException("Failed to connect after " + maxRetries + " attempts", e);
        }
    }
}
```

### 9.3 Concurrent Testing Example

```java
// Use session pool for load testing
HeadlessSessionPool pool = HeadlessSessionPoolBuilder
    .to("ibmi.example.com", 23)
    .minSize(5)
    .maxSize(20)
    .borrowTimeout(Duration.ofSeconds(5))
    .build();

pool.start();

try {
    ExecutorService executor = Executors.newFixedThreadPool(10);

    for (int i = 0; i < 100; i++) {
        executor.submit(() -> {
            try (HeadlessSession session = pool.borrowSession(Duration.ofSeconds(5))) {
                session.sendKeys("CALL TEST[enter]");
                session.waitForKeyboardUnlock(Duration.ofSeconds(5));
                String result = session.getScreenAsText();
                // Assert result...
            }
        });
    }

    executor.shutdown();
    executor.awaitTermination(2, TimeUnit.MINUTES);

} finally {
    pool.shutdown(Duration.ofSeconds(10));
}
```

---

## 10. Conclusion

HTI5250J's API is on the right track with `HeadlessSession`, but needs **decisive action for 1.0**:

1. **Pick one primary API** (HeadlessSession) and remove duplicates
2. **Normalize naming** (sendKeys for all keystroke submission)
3. **Add convenience layers** (builder, fluent API, field-level operations)
4. **Improve error ergonomics** (richer error context, recovery guidance)
5. **Stabilize configuration** (builder pattern instead of scattered properties)

**Success Metric:** A new developer should be able to:
- Copy-paste the README example and have it work
- Understand API intent without Javadoc
- Get actionable error messages with recovery suggestions
- Build concurrent test scenarios with session pooling

**Current Rating: 6.5/10** → **Target for 1.0: 8.5/10**

The good news: All recommendations are achievable with moderate effort. The API's foundation is solid; it just needs clarity and consistency.
