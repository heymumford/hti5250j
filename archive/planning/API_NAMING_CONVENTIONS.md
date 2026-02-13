# HTI5250J API Naming Conventions & Style Guide

**Purpose:** Establish consistent naming patterns for HTI5250J 1.0 API
**Date:** February 13, 2026
**Scope:** Public API only (classes, interfaces, methods, packages)

---

## 1. Package Naming

### Current State (Inconsistent)
```
org.hti5250j
├── (root)                    # High-level classes: Session5250, SessionConfig
├── interfaces                # Public API: HeadlessSession, RequestHandler
├── headless                  # Internal: HeadlessSession (duplicate!)
├── session                    # Internal: DefaultHeadlessSession, NullRequestHandler
├── framework.tn5250          # Protocol: tnvt, Screen5250
├── framework.transport       # Transport: IBMiConnectionFactory
├── workflow                   # Workflow: WorkflowRunner, ActionFactory
├── keyboard                   # Input: KeyStroker, KeyMnemonic
├── encoding                   # EBCDIC: ICodePage, CharacterConversionException
└── event                      # Events: SessionListener, ScreenListener
```

### Recommended Structure for 1.0

```
org.hti5250j
├── api/                       # Public API (stable, backward-compatible)
│   ├── HeadlessSession
│   ├── HeadlessSessionBuilder
│   ├── Session5250            # Legacy, deprecated in 1.0
│   ├── SessionConfig
│   ├── RequestHandler
│   └── pool/                  # Session pooling
│       └── HeadlessSessionPool
│
├── domain/                    # High-level domain objects
│   ├── ScreenField
│   ├── FormSession (new)
│   └── field/
│       ├── FieldValue
│       └── FieldValidationError
│
├── exception/                 # Exception hierarchy
│   ├── HTI5250jException
│   ├── ConnectionException
│   ├── TimeoutException
│   ├── KeyboardTimeoutException
│   ├── ScreenException
│   ├── FieldException
│   └── SecurityException
│
├── internal/                  # Internal implementation (not for public use)
│   ├── screen/
│   │   └── Screen5250         # Rename from framework.tn5250
│   ├── transport/
│   │   ├── TN5250ETransport   # Rename from tnvt
│   │   └── IBMiConnectionFactory
│   ├── encoding/
│   │   └── CharacterCodePage
│   └── event/
│       ├── SessionListener
│       └── ScreenListener
│
├── workflow/                  # Workflow orchestration
│   ├── WorkflowRunner
│   ├── Action
│   └── handler/
│       ├── LoginHandler
│       ├── NavigateHandler
│       └── SubmitHandler
│
└── examples/                  # Example programs (not part of SDK)
    ├── SimpleLogin.java
    ├── ConcurrentTesting.java
    └── ErrorHandling.java
```

**Rationale:**
- **api/** — Everything a user imports is here; clear signal of stability
- **domain/** — High-level business objects (forms, fields)
- **exception/** — All exceptions in one place for discoverability
- **internal/** — Everything else (protocol, transport, events); clearly marked as not public API

---

## 2. Class Naming Conventions

### Rule 1: Suffix Indicates Type

| Type | Suffix | Example | Usage |
|------|--------|---------|-------|
| Interface | `(none)` | `HeadlessSession` | Defines contract |
| Implementation | `Default*` | `DefaultHeadlessSession` | Default impl of interface |
| Alternative Impl | `Null*, Mock*, Stub*` | `NullRequestHandler` | Special case handling |
| Builder | `*Builder` | `HeadlessSessionBuilder` | Fluent configuration |
| Exception | `*Exception` | `ConnectionException` | Exceptional state |
| Factory | `*Factory` | `CodePageFactory` | Creates instances |
| Listener | `*Listener` | `SessionListener` | Event subscriber |
| Adapter | `*Adapter` | `AwtKeyEventAdapter` | Bridges APIs |
| Utility | `*Util, *Helper` | `As400Util` | Static helper methods |

### Rule 2: Verb Prefixes (When Needed)

| Prefix | Meaning | Example |
|--------|---------|---------|
| `is*` | Boolean predicate | `isConnected()` |
| `get*` | Accessor (returns object) | `getScreen()` |
| `set*` | Mutator | `setCodePage()` |
| `send*` | Transmits to host | `sendKeys()` |
| `wait*` | Blocking operation with timeout | `waitForKeyboardUnlock()` |
| `add*` | Adds to collection | `addSessionListener()` |
| `remove*` | Removes from collection | `removeSessionListener()` |

### Rule 3: Package-Qualified Types

When a class name alone is ambiguous, use package context:

| Ambiguous | Clarified |
|-----------|-----------|
| `Session` | `HeadlessSession` (in `api/`) |
| `Exception` | `ConnectionException` (in `exception/`) |
| `ScreenField` (Field on 5250 screen) | No change; context clear from package |

**Exception:** Do NOT use package-qualified class names. The class name alone must convey intent.

---

## 3. Method Naming Conventions

### Keystroke Submission (Unified Pattern)

**Current Inconsistency:**
```java
// Session5250
session.sendString("TEXT");
session.sendKey(keyCode);
session.sendTab();
session.sendEnter();

// HeadlessSession
headless.sendKeys("[enter]");
```

**Unified Pattern for 1.0:**
```java
// Only one method, multiple patterns
headless.sendKeys("TEXT[tab]PASSWORD[enter]");  // Preferred
headless.sendKeys("[f5]");                       // Function keys
headless.sendKeys("A");                          // Literal character
```

**Deprecation:**
```java
@Deprecated(since = "0.13.0", forRemoval = true,
    message = "Use sendKeys(String) with mnemonics instead")
public void sendString(String text) {
    sendKeys(text);
}

@Deprecated(since = "0.13.0", forRemoval = true,
    message = "Use sendKeys(\"[tab]\") instead")
public void sendTab() {
    sendKeys("[tab]");
}
```

### Screen Reading Methods

| Method | Returns | Purpose | Clear? |
|--------|---------|---------|--------|
| `getScreen()` | `Screen5250` | Get internal screen object | ❌ Unclear (exposes internal) |
| `getScreenAsText()` | `String` | Get screen as 80×24 text | ✓ Clear |
| `getScreenAsChars()` | `char[]` | Get raw character array | ✓ Clear |
| `captureScreenshot()` | `BufferedImage` | Get PNG-renderable image | ✓ Clear |

**Recommendation:**
- Deprecate `getScreen()` (exposes internal API)
- Keep `getScreenAsText()`, `captureScreenshot()`
- Add `getFieldValue(String fieldName)` for high-level field API

### Waiting Methods

| Method | Semantics | Clear? |
|--------|-----------|--------|
| `waitForKeyboardUnlock(int ms)` | Wait until keyboard available | ✓ Clear (but timing unclear) |
| `waitForKeyboardLockCycle(int ms)` | Wait for lock → unlock cycle | ❌ Unclear (what's a "cycle"?) |
| `waitForText(String text, int ms)` | Wait until text appears | ✓ Clear |

**Improvements:**
```java
// Current (semantic confusion)
session.waitForKeyboardLockCycle(5000);

// Better (explicit about what we're waiting for)
session.waitForScreenUpdate(Duration.ofSeconds(5));
session.waitForText("Order accepted", Duration.ofSeconds(10));
session.waitForField("total_amount", Duration.ofSeconds(5));
```

### Properties and Configuration

| Bad | Good | Reason |
|-----|------|--------|
| `getConnectionProperties()` | `getConnectionConfig()` or `getHost()`/`getPort()` | Properties are mutable; should use value objects |
| `getConfiguration()` | `getSessionConfig()` | Ambiguous ("which" configuration?) |
| `getScreen()` | `getScreenContent()` or `getScreenAsText()` | "screen" is ambiguous (the object? the text?) |

---

## 4. Interface Naming

### Rule: Use Descriptive Names, Not "I" Prefix

| Bad (Hungarian Notation) | Good |
|--------------------------|------|
| `IHeadlessSession` | `HeadlessSession` |
| `IScreenProvider` | `ScreenProvider` |
| `IRequestHandler` | `RequestHandler` |

**Exception:** Framework interfaces that extend Java interfaces:
```java
// OKAY: Clearly implements Closeable
public interface ManagedSession extends AutoCloseable {
    // ...
}

// NOT OKAY: "I" prefix is redundant
public interface IAutoCloseable extends AutoCloseable {
    // ...
}
```

---

## 5. Exception Naming Conventions

### Rule: Suffix with "Exception"

| Class | Extends | Use Case |
|-------|---------|----------|
| `HTI5250jException` | `Exception` | Base class for all HTI5250J exceptions |
| `ConnectionException` | `HTI5250jException` | Cannot connect to host |
| `TimeoutException` | `HTI5250jException` | Operation did not complete in time |
| `KeyboardTimeoutException` | `TimeoutException` | Keyboard stayed locked too long |
| `ScreenException` | `HTI5250jException` | Screen encoding/parsing failed |
| `FieldException` | `HTI5250jException` | Field operation failed (base) |
| `FieldNotFoundException` | `FieldException` | Field not found on screen |
| `FieldValidationException` | `FieldException` | Field value invalid |
| `SecurityException` | `HTI5250jException` | SSL/TLS/auth failure |
| `ConfigurationException` | `HTI5250jException` | Invalid configuration |

**Anti-Pattern (Do Not Use):**
```java
// BAD: Doesn't clearly indicate it's an exception
public class KeyboardLocked {
    // ...
}

// GOOD: Clear that this is an exception
public class KeyboardTimeoutException extends TimeoutException {
    // ...
}
```

---

## 6. Constant Naming Conventions

### Rule: ALL_CAPS with Underscores

```java
public interface HeadlessSession {
    // Constants
    int DEFAULT_CONNECT_TIMEOUT_MS = 10000;
    int DEFAULT_READ_TIMEOUT_MS = 5000;
    int DEFAULT_SCREEN_ROWS = 24;
    int DEFAULT_SCREEN_COLS = 80;

    // Enum values
    enum CodePage {
        CCSID37,     // US English
        CCSID500,    // International
        CCSID930,    // Japanese
    }

    enum FunctionKey {
        ENTER,
        F1, F2, F3, /* ... */, F24,
        PAGE_UP,
        PAGE_DOWN,
    }
}
```

---

## 7. Method Parameter Naming

### Rule: Use Descriptive Names, Avoid "i", "x", "val"

| Bad | Good | Reason |
|-----|------|--------|
| `sendKeys(String s)` | `sendKeys(String keys)` | Clarity |
| `connect(String h, int p)` | `connect(String hostname, int port)` | Self-documenting |
| `wait(int t)` | `wait(Duration timeout)` or `waitMs(int timeoutMs)` | Units explicit |
| `fillField(String f, String v)` | `fillField(String fieldName, String value)` | Clarity |

**Exception:** Loop counters (acceptable for tight loops)
```java
// OKAY: Tight, obvious loop
for (int i = 0; i < fields.size(); i++) {
    ScreenField field = fields.get(i);
}

// BETTER: More descriptive
for (ScreenField field : fields) {
    // ...
}
```

---

## 8. Boolean Property Naming

### Rule: Use "is*" Prefix for Predicates

```java
public interface HeadlessSession {
    boolean isConnected();        // ✓ Good
    boolean isKeyboardLocked();   // ✓ Good
    boolean isSSLEnabled();       // ✓ Good

    // NOT:
    // boolean connected();       // ✗ Ambiguous
    // boolean keyboardLocked();  // ✗ Reads like setter
}

public class ScreenField {
    boolean isProtected();        // ✓ Good
    boolean isRequired();         // ✓ Good
    boolean isVisible();          // ✓ Good
}
```

---

## 9. Callback/Listener Naming

### Rule: Use Verb Form, NOT "On" Prefix (Modern Java)

| Old Style (Swing) | Modern Style |
|------------------|-------------|
| `onSessionChanged(event)` | `sessionChanged(event)` |
| `onScreenUpdated(event)` | `screenUpdated(event)` |

**However:** Keep existing listener pattern for backward compatibility:
```java
// Existing (keep for 1.0)
public interface SessionListener {
    void onSessionChanged(SessionChangeEvent event);
}

// Future (Java 8+ patterns, post-1.0)
session.onConnected(() -> System.out.println("Connected"));
session.onScreenChanged(content -> processScreen(content));
```

---

## 10. Builder Pattern Naming

### Rule: Fluent, Verb-Based, Progressive Refinement

```java
public class HeadlessSessionBuilder {
    // Start
    public static HeadlessSessionBuilder to(String host, int port) { ... }

    // Configure (in any order)
    public HeadlessSessionBuilder withCodePage(CodePage page) { ... }
    public HeadlessSessionBuilder withTimeout(Duration timeout) { ... }
    public HeadlessSessionBuilder withSSL(SSLConfig config) { ... }

    // Optional shortcuts
    public HeadlessSessionBuilder secure() { ... }     // withSSL enabled
    public HeadlessSessionBuilder fast() { ... }       // withTimeout(1s)

    // Build
    public HeadlessSession build() { ... }

    // Convenience: Build + Connect
    public HeadlessSession connect() throws ConnectionException { ... }
}
```

**Usage (reads like English):**
```java
var session = HeadlessSessionBuilder
    .to("host", 23)
    .withCodePage(CodePage.CCSID37)
    .withTimeout(Duration.ofSeconds(30))
    .secure()
    .build();
```

---

## 11. Enum Naming Conventions

### Rule: Uppercase, Singular Form

```java
// GOOD: Singular, clear intent
public enum CodePage {
    CCSID37,           // US
    CCSID500,          // International
    CCSID930,          // Japanese
}

public enum FunctionKey {
    ENTER,
    ESCAPE,
    PAGE_UP,
    PAGE_DOWN,
    F1, F2, /* ... */ F24,
}

// NOT:
// public enum CodePages { ... }         // Plural (wrong)
// public enum FUNCTIONKEYS { ... }      // All caps (hard to read)
```

---

## 12. Package Organization Summary

### For API 1.0, Enforce:

✓ **Public API Packages** (clear intent):
- `org.hti5250j.api` — Stable, backward-compatible
- `org.hti5250j.domain` — High-level objects
- `org.hti5250j.exception` — Exception hierarchy

✓ **Internal Packages** (clearly marked):
- `org.hti5250j.internal.*` — Not for public use
- Javadoc clearly states: "Internal API. Subject to change."

✗ **Avoid:**
- `org.hti5250j.impl` — Unclear intent (impl of what?)
- `org.hti5250j.util.internal` — Double nesting, confusing

---

## 13. Documentation String Conventions

### Javadoc Format

```java
/**
 * Brief description of what the method does.
 *
 * More detailed explanation if needed, including:
 * - Semantics (synchronous vs. asynchronous)
 * - Error conditions
 * - Performance characteristics
 *
 * @param paramName description
 * @return description of return value
 * @throws SpecificException when this error occurs
 *
 * @example
 * // Show how to use this method
 * session.sendKeys("USER[tab]PASSWORD[enter]");
 *
 * @see RelatedClass#relatedMethod()
 * @since 0.12.0
 */
public void methodName(String paramName) throws SpecificException {
    // ...
}
```

**Good Example (from existing codebase):**
```java
/**
 * Send keys to the host (synchronous operation).
 * Mnemonic syntax: [enter], [tab], [f1]...[f24], etc.
 * Example: sendKeys("CALL MYPGM[enter]")
 *
 * @param keys mnemonic key sequence
 * @throws IllegalStateException if not connected
 * @throws IllegalArgumentException if mnemonic syntax invalid
 */
void sendKeys(String keys) throws IllegalStateException, IllegalArgumentException;
```

---

## 14. Naming Conventions Checklist for Code Review

Before submitting new public API:

- [ ] Class names are singular (`ScreenField`, not `ScreenFields`)
- [ ] Exception classes end with `Exception`
- [ ] Boolean properties use `is*` prefix
- [ ] Methods use verb prefixes (`send*`, `wait*`, `get*`, `fill*`)
- [ ] No "I" prefix on interfaces
- [ ] No Hungarian notation (no `i*`, `s*` prefixes)
- [ ] Constants use UPPER_CASE_WITH_UNDERSCORES
- [ ] Package organization follows the structure above
- [ ] Javadoc present on all public classes/methods
- [ ] No ambiguous names (clarified by package + class + method)
- [ ] Similar operations use similar naming (`sendKeys`, not `sendString` + `sendKeys`)

---

## 15. Deprecation Pattern

When renaming or removing APIs:

```java
@Deprecated(
    since = "0.13.0",
    forRemoval = true,
    message = "Use sendKeys(String) with mnemonic strings instead. " +
              "Migration: session.sendString('X') → session.sendKeys('X')"
)
public void sendString(String text) {
    // Delegate to new API
    sendKeys(text);
}
```

Then document in migration guide:
```markdown
## Session5250.sendString() → HeadlessSession.sendKeys()

### Before (0.12.x)
session.sendString("USER");
session.sendTab();

### After (1.0+)
session.sendKeys("USER[tab]");
```

---

## Summary: Golden Rules

1. **One verb per operation** — `sendKeys()` for all keystroke submission, not `sendString()` + `sendKeys()`
2. **Clear intents** — Names should read like English
3. **Consistent patterns** — All building uses `*Builder`, all reading uses `get*`, all waiting uses `wait*`
4. **Avoid ambiguity** — Use full words (`hostname`, not `h`); use package context
5. **Self-documenting** — Javadoc should explain "why", not "what" (code shows what)

When in doubt: **Would a developer understand this API without reading Javadoc?** If no, rename.
