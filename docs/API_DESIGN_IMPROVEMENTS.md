# HTI5250J API Design Improvements

**Date:** February 2026
**Scope:** Recommended enhancements for third-party library consumption
**Priority:** Post-Phase-1 (backward compatible changes)

---

## Overview

The current Java API (Session5250, HeadlessSession, WorkflowRunner) is well-designed for library consumers but lacks some ergonomics and explicit contracts. This document proposes backward-compatible improvements that make third-party integration easier.

---

## 1. Builder Pattern for Session Creation

### Current API (Verbose)

```java
// Current approach: constructor + properties
Properties props = new Properties();
props.setProperty("host", "ibmi.example.com");
props.setProperty("port", "23");
props.setProperty("code-page", "37");
props.setProperty("screen-size", "24x80");

SessionConfig config = new SessionConfig("myapp", "myapp");
Session5250 session = new Session5250(props, "resource", "myapp", config);
```

**Problems:**
- Verbose property initialization
- Magic strings for configuration keys
- No IDE autocomplete for property names
- Type safety lost (all properties are strings)

### Proposed API (Fluent Builder)

```java
// New fluent builder approach
Session5250 session = new Session5250Builder()
    .host("ibmi.example.com")
    .port(23)
    .codePage(37)
    .screenSize(80, 24)
    .sessionName("myapp")
    .sslMode(SSLMode.TLS_1_3)
    .timeout(Duration.ofSeconds(30))
    .heartbeatEnabled(true)
    .requestHandler(new CustomRequestHandler())
    .build();
```

### Implementation

```java
public class Session5250Builder {
    private String host;
    private int port = 23;
    private int codePage = 37;
    private int screenWidth = 80;
    private int screenHeight = 24;
    private String sessionName = "default";
    private SSLMode sslMode = SSLMode.NONE;
    private Duration timeout = Duration.ofSeconds(30);
    private boolean heartbeatEnabled = false;
    private RequestHandler requestHandler = new NullRequestHandler();

    public Session5250Builder host(String host) {
        this.host = host;
        return this;
    }

    public Session5250Builder port(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be 1-65535");
        }
        this.port = port;
        return this;
    }

    public Session5250Builder codePage(int ccsid) {
        // Validate CCSID
        if (!VALID_CCSIDS.contains(ccsid)) {
            throw new IllegalArgumentException("Unsupported CCSID: " + ccsid);
        }
        this.codePage = ccsid;
        return this;
    }

    public Session5250Builder screenSize(int width, int height) {
        if (!(width == 80 || width == 132) || !(height == 24 || height == 27)) {
            throw new IllegalArgumentException("Invalid screen size");
        }
        this.screenWidth = width;
        this.screenHeight = height;
        return this;
    }

    public Session5250Builder sessionName(String name) {
        this.sessionName = name;
        return this;
    }

    public Session5250Builder sslMode(SSLMode mode) {
        this.sslMode = mode;
        return this;
    }

    public Session5250Builder timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public Session5250Builder heartbeatEnabled(boolean enabled) {
        this.heartbeatEnabled = enabled;
        return this;
    }

    public Session5250Builder requestHandler(RequestHandler handler) {
        this.requestHandler = handler;
        return this;
    }

    public Session5250 build() {
        if (host == null) {
            throw new IllegalStateException("host() is required");
        }

        // Create internal Properties object
        Properties props = new Properties();
        props.setProperty(HTI5250jConstants.SESSION_HOST, host);
        props.setProperty(HTI5250jConstants.SESSION_PORT, String.valueOf(port));
        props.setProperty(HTI5250jConstants.SESSION_CODE_PAGE, String.valueOf(codePage));
        props.setProperty(HTI5250jConstants.SESSION_SCREEN_SIZE, screenWidth + "x" + screenHeight);
        if (heartbeatEnabled) {
            props.setProperty(HTI5250jConstants.SESSION_HEART_BEAT, "true");
        }
        if (sslMode != SSLMode.NONE) {
            props.setProperty(HTI5250jConstants.SSL_TYPE, sslMode.name());
        }

        SessionConfig config = new SessionConfig(sessionName, sessionName);
        Session5250 session = new Session5250(props, sessionName, sessionName, config);
        session.setRequestHandler(requestHandler);
        return session;
    }
}

public enum SSLMode {
    NONE, TLS_1_2, TLS_1_3
}
```

### Usage Examples

```java
// Minimal (required host only)
Session5250 session = new Session5250Builder()
    .host("prod.ibmi.com")
    .build();

// Full customization
Session5250 session = new Session5250Builder()
    .host("prod.ibmi.com")
    .port(992)
    .codePage(273)  // German
    .screenSize(132, 27)
    .sessionName("payment-processor")
    .sslMode(SSLMode.TLS_1_3)
    .timeout(Duration.ofSeconds(10))
    .heartbeatEnabled(true)
    .requestHandler(new AutoAnswerRequestHandler())
    .build();

// In tests with mocks
Session5250 session = new Session5250Builder()
    .host("mock-ibmi")
    .requestHandler(new MockRequestHandler(testData))
    .build();
```

### Benefits

- ✅ Type-safe configuration (compile-time validation)
- ✅ IDE autocomplete for all options
- ✅ Fluent/chainable syntax (readable)
- ✅ Explicit required parameters (host)
- ✅ Default values for optional parameters
- ✅ Backward compatible (existing constructors unchanged)

### Migration Path

1. Keep existing `Session5250(Properties, ...)` constructors (no deprecation)
2. Add `Session5250Builder` in parallel
3. Document builder as recommended approach in README
4. Provide migration guide in MIGRATION_GUIDE.md

---

## 2. Explicit API Stability Guarantees

### Current State

No deprecation policy; unclear which classes are stable vs internal.

### Proposed Policy

**Public API (Stable, backward compatible until v1.0):**
- `org.hti5250j.Session5250`
- `org.hti5250j.interfaces.HeadlessSession`
- `org.hti5250j.interfaces.RequestHandler`
- `org.hti5250j.workflow.WorkflowRunner`
- `org.hti5250j.workflow.WorkflowSchema`
- `org.hti5250j.framework.tn5250.Screen5250`
- `org.hti5250j.framework.tn5250.ScreenOIA`

**Internal API (No backward compatibility guarantee):**
- `org.hti5250j.framework.tn5250.tnvt`
- `org.hti5250j.tools.*`
- `org.hti5250j.encoding.*`
- Classes marked `@InternalAPI`

### Deprecation Policy

```java
/**
 * Deprecation policy:
 * - @Deprecated(since = "0.13.0", forRemoval = true)
 * - Removed in 0.15.0 (2 versions after deprecation)
 * - Migration guide provided for each deprecated API
 */

@Deprecated(since = "0.13.0", forRemoval = true)
@ReplaceWith("Session5250Builder.host(...).build()")
public Session5250(Properties props, String resource, String name, SessionConfig config) {
    // Still works, but warns at compile time in IDEs
}
```

### Implementation

Create `docs/API_STABILITY.md`:

```markdown
# API Stability Guarantee

## Semantic Versioning

HTI5250J follows semantic versioning (MAJOR.MINOR.PATCH):
- **MAJOR:** Breaking API changes
- **MINOR:** New features, backward compatible
- **PATCH:** Bug fixes, backward compatible

## Backward Compatibility

Public API classes (listed above) will maintain backward compatibility until v1.0.

Removed APIs:
- Deprecated for 2 minor versions before removal
- Migration guide provided for each deprecation
- Compiler warnings emitted via `@Deprecated(forRemoval = true)`

## Internal API

Classes in internal packages or marked `@InternalAPI` have no stability guarantee.
If relying on internal APIs, open a GitHub issue to request public API addition.

## Example

```
0.12.0 (current)
  |
  ├─ 0.13.0: @Deprecated legacyMethod() → Use newMethod() instead
  |
  ├─ 0.14.0: Still supports legacyMethod() (with warning)
  |
  └─ 0.15.0: legacyMethod() removed (BREAKING)
```
```

---

## 3. Async API Variants

### Current State

All APIs are blocking (appropriate for virtual threads, but inconvenient for callback-based systems).

### Proposed API (Optional)

Add `AsyncHeadlessSession` interface for non-blocking workflows:

```java
public interface AsyncHeadlessSession extends HeadlessSession {
    /**
     * Asynchronously connect to IBM i system.
     * @return CompletableFuture that completes when connected
     */
    CompletableFuture<Void> connectAsync();

    /**
     * Asynchronously send keys and wait for screen change.
     * @return CompletableFuture with new screen text
     */
    CompletableFuture<String> sendKeysAsync(String keys);

    /**
     * Asynchronously get current screen content.
     * @return CompletableFuture with screen text
     */
    CompletableFuture<String> getScreenAsTextAsync();

    /**
     * Asynchronously wait for keyboard unlock.
     * @return CompletableFuture that completes when keyboard available
     */
    CompletableFuture<Void> waitForKeyboardUnlockAsync(int timeoutMs);

    /**
     * Asynchronously disconnect.
     * @return CompletableFuture that completes when disconnected
     */
    CompletableFuture<Void> disconnectAsync();
}
```

### Usage Example

```java
AsyncHeadlessSession session = (AsyncHeadlessSession) HeadlessSessionFactory
    .create("prod.ibmi.com", config);

session.connectAsync()
    .thenCompose(v -> session.waitForKeyboardUnlockAsync(30000))
    .thenCompose(v -> session.sendKeysAsync("CALL PGM(MYAPP)[enter]"))
    .thenCompose(v -> session.getScreenAsTextAsync())
    .thenAccept(screenText -> {
        System.out.println("Screen: " + screenText);
    })
    .exceptionally(ex -> {
        System.err.println("Error: " + ex.getMessage());
        return null;
    });
```

### Benefits

- ✅ Reactive programming support
- ✅ Callback-based integration (e.g., Spring WebFlux)
- ✅ Better composability with modern async frameworks
- ✅ Optional (blocking APIs still available)

### Implementation

```java
public class DefaultAsyncHeadlessSession implements AsyncHeadlessSession {
    private final HeadlessSession delegate;
    private final ExecutorService executor;

    public DefaultAsyncHeadlessSession(HeadlessSession delegate) {
        this.delegate = delegate;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public CompletableFuture<Void> connectAsync() {
        return CompletableFuture.runAsync(delegate::connect, executor);
    }

    @Override
    public CompletableFuture<String> sendKeysAsync(String keys) {
        return CompletableFuture.supplyAsync(() -> {
            delegate.sendKeys(keys);
            return delegate.getScreenAsText();
        }, executor);
    }

    // ... other methods
}
```

---

## 4. Explicit Exception Hierarchy

### Current State

Custom exceptions (AssertionException, NavigationException) exist but lack clear inheritance.

### Proposed Hierarchy

```java
/**
 * Base exception for all HTI5250J operations.
 */
public abstract class HTI5250jException extends Exception {
    public HTI5250jException(String message) { ... }
    public HTI5250jException(String message, Throwable cause) { ... }
}

/**
 * Session/connection errors (network, authentication).
 */
public class SessionException extends HTI5250jException {
    public static class ConnectException extends SessionException { }
    public static class AuthenticationException extends SessionException { }
    public static class DisconnectException extends SessionException { }
}

/**
 * Workflow execution errors.
 */
public class WorkflowException extends HTI5250jException {
    public static class ValidationException extends WorkflowException { }
    public static class ExecutionException extends WorkflowException { }
    public static class TimeoutException extends WorkflowException { }
}

/**
 * Screen interaction errors.
 */
public class ScreenException extends HTI5250jException {
    public static class NavigationException extends ScreenException { }
    public static class AssertionException extends ScreenException { }
    public static class FieldException extends ScreenException { }
}

/**
 * Protocol/encoding errors.
 */
public class ProtocolException extends HTI5250jException {
    public static class EncodingException extends ProtocolException { }
    public static class TelnetException extends ProtocolException { }
}
```

### Usage

```java
try {
    session.connect();
} catch (SessionException.ConnectException e) {
    logger.error("Failed to connect to host", e);
} catch (SessionException.AuthenticationException e) {
    logger.error("Authentication failed", e);
} catch (SessionException e) {
    logger.error("Session error", e);
}

try {
    runner.executeStep(step, dataRow);
} catch (ScreenException.AssertionException e) {
    logger.error("Assertion failed on step: {}", step.name(), e);
    // Artifact collection already done by runner
} catch (ScreenException.NavigationException e) {
    logger.error("Navigation failed: expected screen not found", e);
} catch (WorkflowException e) {
    logger.error("Workflow error", e);
}
```

### Benefits

- ✅ Precise error handling (catch specific errors)
- ✅ Clear error taxonomy
- ✅ Better error recovery logic
- ✅ IDE autocomplete for exception types

---

## 5. Metrics API Interface

### Current State

Metrics collected internally (BatchMetrics) but no hook for external systems.

### Proposed API

```java
/**
 * Listener interface for workflow/session metrics.
 * Enables integration with external monitoring systems.
 */
public interface MetricsListener {
    /**
     * Called when workflow starts.
     */
    void onWorkflowStart(String workflowName, Map<String, String> parameters);

    /**
     * Called when workflow step starts.
     */
    void onStepStart(String stepName, ActionType actionType);

    /**
     * Called when workflow step completes.
     */
    void onStepComplete(String stepName, long durationMs, boolean success);

    /**
     * Called when workflow completes.
     */
    void onWorkflowComplete(String workflowName, long durationMs, boolean success, WorkflowResult result);

    /**
     * Called when session connects.
     */
    void onSessionConnect(String sessionName, String host, int port);

    /**
     * Called when session disconnects.
     */
    void onSessionDisconnect(String sessionName);

    /**
     * Called when assertion fails.
     */
    void onAssertionFailure(String expectedText, String actualScreen);

    /**
     * Called when keyboard wait times out.
     */
    void onKeyboardTimeout(long timeoutMs);
}

/**
 * Composite listener for chaining multiple metrics listeners.
 */
public class CompositeMetricsListener implements MetricsListener {
    private final List<MetricsListener> listeners;

    public CompositeMetricsListener(MetricsListener... listeners) {
        this.listeners = Arrays.asList(listeners);
    }

    public void onWorkflowStart(String workflowName, Map<String, String> parameters) {
        for (MetricsListener listener : listeners) {
            listener.onWorkflowStart(workflowName, parameters);
        }
    }

    // ... other methods delegate to all listeners
}
```

### Usage Example

```java
// Prometheus metrics listener
MetricsListener prometheusListener = new PrometheusMetricsListener(meterRegistry);

// Custom logging listener
MetricsListener loggingListener = new LoggingMetricsListener(logger);

// Composite listener
MetricsListener composite = new CompositeMetricsListener(
    prometheusListener,
    loggingListener,
    new DatadogMetricsListener(datadogClient)
);

// Register with WorkflowRunner
WorkflowRunner runner = new WorkflowRunner(session, datasetLoader, artifactCollector);
runner.setMetricsListener(composite);

// Execute workflow (metrics automatically collected)
runner.executeWorkflow(workflow, dataRow);
```

### Implementation (Prometheus Example)

```java
public class PrometheusMetricsListener implements MetricsListener {
    private final MeterRegistry meterRegistry;
    private final Timer.Sample sample;

    @Override
    public void onWorkflowStart(String workflowName, Map<String, String> parameters) {
        sample = Timer.start(meterRegistry);
    }

    @Override
    public void onWorkflowComplete(String workflowName, long durationMs, boolean success, WorkflowResult result) {
        sample.stop(Timer.builder("workflow.duration")
            .tag("workflow", workflowName)
            .tag("status", success ? "success" : "failure")
            .register(meterRegistry)
        );

        Counter.builder("workflow.total")
            .tag("workflow", workflowName)
            .tag("status", success ? "success" : "failure")
            .register(meterRegistry)
            .increment();
    }

    // ... other methods
}
```

---

## 6. Configuration Object Enhancement

### Current State

Configuration scattered across Properties object and SessionConfig.

### Proposed API

```java
/**
 * Type-safe, fluent configuration object.
 * Replaces Properties-based configuration.
 */
public class HTI5250jConfig {
    private final String host;
    private final int port;
    private final int codePage;
    private final int screenWidth;
    private final int screenHeight;
    private final Duration connectionTimeout;
    private final Duration keyboardLockTimeout;
    private final Duration keyboardUnlockTimeout;
    private final boolean sslEnabled;
    private final String sslProtocol;
    private final boolean heartbeatEnabled;
    private final int heartbeatInterval;
    private final RequestHandler requestHandler;
    private final MetricsListener metricsListener;

    public static HTI5250jConfigBuilder builder() {
        return new HTI5250jConfigBuilder();
    }

    public static class HTI5250jConfigBuilder {
        // ... fluent builder methods
    }

    // Immutable getters
    public String getHost() { return host; }
    public int getPort() { return port; }
    public int getCodePage() { return codePage; }
    // ... etc

    /**
     * Convert to Properties (for backward compatibility).
     */
    public Properties toProperties() {
        Properties props = new Properties();
        props.setProperty("host", host);
        props.setProperty("port", String.valueOf(port));
        // ...
        return props;
    }

    /**
     * Create from Properties (for backward compatibility).
     */
    public static HTI5250jConfig fromProperties(Properties props) {
        return builder()
            .host(props.getProperty("host"))
            .port(Integer.parseInt(props.getProperty("port", "23")))
            // ...
            .build();
    }
}
```

### Usage

```java
// New type-safe approach
HTI5250jConfig config = HTI5250jConfig.builder()
    .host("prod.ibmi.com")
    .port(992)
    .codePage(37)
    .connectionTimeout(Duration.ofSeconds(30))
    .keyboardUnlockTimeout(Duration.ofSeconds(5))
    .sslEnabled(true)
    .sslProtocol("TLSv1.3")
    .heartbeatEnabled(true)
    .metricsListener(prometheusListener)
    .build();

Session5250 session = new Session5250Builder()
    .from(config)  // Use configuration object
    .build();
```

---

## 7. Resource Management (Try-With-Resources)

### Current State

No AutoCloseable support; manual disconnect() required.

### Proposed API

```java
/**
 * Make Session5250 and HeadlessSession implement AutoCloseable.
 */
public interface HeadlessSession extends AutoCloseable {
    // ... existing methods

    @Override
    void close() throws Exception;  // Implement via disconnect()
}

public class Session5250 implements SessionInterface, AutoCloseable {
    // ... existing code

    @Override
    public void close() {
        disconnect();  // Auto-cleanup on close()
    }
}
```

### Usage

```java
// Old approach (manual cleanup)
Session5250 session = new Session5250Builder().host("ibmi.com").build();
try {
    session.connect();
    // ... workflow
} finally {
    session.disconnect();
}

// New approach (automatic cleanup)
try (Session5250 session = new Session5250Builder().host("ibmi.com").build()) {
    session.connect();
    // ... workflow
}  // Auto-disconnected here

// Works with streams too
List<Session5250> sessions = Stream.range(0, 10)
    .map(i -> new Session5250Builder().host("ibmi.com").build())
    .collect(Collectors.toList());

for (Session5250 session : sessions) {
    try (session) {
        // ... workflow
    }
}
```

### Benefits

- ✅ RAII pattern (Resource Acquisition Is Initialization)
- ✅ Automatic cleanup (no forgotten disconnect())
- ✅ Works with try-with-resources
- ✅ Exception-safe (cleanup happens even on error)

---

## 8. Validation API

### Current State

Validation scattered across handlers; no unified validation interface.

### Proposed API

```java
/**
 * Validation interface for workflows and configuration.
 */
public interface Validator<T> {
    ValidationResult validate(T target);
}

/**
 * Configuration validator.
 */
public class ConfigValidator implements Validator<HTI5250jConfig> {
    @Override
    public ValidationResult validate(HTI5250jConfig config) {
        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();

        if (config.getHost() == null || config.getHost().isEmpty()) {
            errors.add(new ValidationError("Host is required"));
        }

        if (config.getPort() < 1 || config.getPort() > 65535) {
            errors.add(new ValidationError("Port must be 1-65535"));
        }

        if (config.getConnectionTimeout().isNegative()) {
            errors.add(new ValidationError("Connection timeout must be positive"));
        }

        if (config.isHeartbeatEnabled() && config.getHeartbeatInterval() > 60000) {
            warnings.add(new ValidationWarning("Heartbeat interval > 60s may cause connection timeout"));
        }

        return new ValidationResult(errors, warnings);
    }
}

/**
 * Workflow validator.
 */
public class WorkflowValidator implements Validator<WorkflowSchema> {
    @Override
    public ValidationResult validate(WorkflowSchema workflow) {
        // Validate workflow structure, step definitions, etc.
        return new ValidationResult(...);
    }
}

/**
 * Dataset validator (CSV).
 */
public class DatasetValidator implements Validator<Map<String, Map<String, String>>> {
    @Override
    public ValidationResult validate(Map<String, Map<String, String>> dataset) {
        // Validate CSV columns, data types, etc.
        return new ValidationResult(...);
    }
}
```

### Usage

```java
HTI5250jConfig config = HTI5250jConfig.builder()
    .host("ibmi.com")
    .port(23)
    .build();

ConfigValidator validator = new ConfigValidator();
ValidationResult result = validator.validate(config);

if (!result.isValid()) {
    result.getErrors().forEach(error -> System.err.println(error.message()));
    System.exit(1);
}

result.getWarnings().forEach(warning -> System.out.println("⚠️  " + warning.message()));
```

---

## Priority & Timeline

### P0 (Phase 1 — Q1 2026)
- [ ] Builder pattern (Session5250Builder)
- [ ] API stability documentation (API_STABILITY.md)
- [ ] Exception hierarchy refinement
- [ ] AutoCloseable support

### P1 (Phase 2 — Q2 2026)
- [ ] Async API variants (AsyncHeadlessSession)
- [ ] Metrics listener interface
- [ ] Configuration object (HTI5250jConfig)

### P2 (Phase 3 — Q3 2026)
- [ ] Validation API
- [ ] Advanced configuration options
- [ ] Plugin system modernization (if needed)

---

## Backward Compatibility

All proposed changes are **100% backward compatible**:
- Existing constructors remain unchanged
- New builders/interfaces added in parallel
- Old code works without modification
- Deprecation warnings added for future removal

---

**Document Version:** 1.0
**Last Updated:** February 2026
**Status:** Ready for Implementation
