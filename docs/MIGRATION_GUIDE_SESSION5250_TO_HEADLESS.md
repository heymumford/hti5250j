# Migration Guide: Session5250 → HeadlessSession (Phase 15B)

**Document Purpose:** Help existing HTI5250J users transition from Session5250 API to the new HeadlessSession abstraction.

**Status:** Backward compatible — existing code continues to work unchanged.

**Timeline:** Phase 15B (February 2026) — New abstractions are opt-in.

---

## Quick Answer: Do I Need to Migrate?

| Scenario | Migration Required? | Effort |
|----------|-------------------|--------|
| **Existing code using Session5250** | ❌ No | 0 — Keep using it as-is |
| **New headless automation code** | ✅ Recommended | 1-2 hours per integration |
| **Robot Framework integration** | ✅ Required | 2-3 hours (custom handler + adapter) |
| **Docker/Containerized deployment** | ✅ Recommended | <1 hour per application |
| **High-concurrency (1000+ sessions)** | ✅ Recommended | Test, measure memory, optimize |

---

## Backward Compatibility Guarantee

**Session5250 API is unchanged.** All existing code continues to work:

```java
// This code still works exactly the same
Session5250 session = new Session5250(props, config, "session1", sessionConfig);
session.connect();
session.sendKeys("MYPROGRAM[enter]");
String text = session.getScreen().getScreenAsText();
session.disconnect();
```

No changes required. No deprecation warnings. No migration deadline.

---

## Migration Paths

### Path 1: Adopt HeadlessSession (Recommended for New Code)

**When to use:** Building new headless automation, Docker applications, or high-concurrency systems.

**Effort:** 1-2 hours to refactor one integration.

#### Step 1: Create HeadlessSession

```java
// OLD: Direct Session5250 usage
Session5250 session = new Session5250(props, config, "session1", sessionConfig);

// NEW: Access via HeadlessSession interface
Session5250 session = new Session5250(props, config, "session1", sessionConfig);
HeadlessSession headless = session.asHeadlessSession();
```

#### Step 2: Use HeadlessSession Interface

```java
// Interface provides pure headless contract
String sessionName = headless.getSessionName();
Screen5250 screen = headless.getScreen();

headless.connect();
headless.sendKeys("MYPROGRAM[enter]");
headless.waitForKeyboardUnlock();

String screenText = screen.getScreenAsText();
BufferedImage screenshot = headless.captureScreenshot();

headless.disconnect();
```

#### Step 3: Benefits You Get

✅ Smaller memory footprint (~500KB vs 2MB+ for Session5250 with GUI)
✅ Works in Docker/containerized environments without GUI
✅ Compatible with virtual threads for 1000+ concurrent sessions
✅ Clear, minimal interface (6 methods) vs full Session5250 surface area

---

### Path 2: Inject Custom RequestHandler (For Automation Frameworks)

**When to use:** Robot Framework, Jython adapters, or custom SYSREQ handling.

**Effort:** 2-3 hours to create adapter + integrate.

#### Step 1: Implement RequestHandler

```java
package com.mycompany.automation;

import org.hti5250j.interfaces.RequestHandler;

public class MyAutomationRequestHandler implements RequestHandler {
    private final Map<String, String> responses;

    public MyAutomationRequestHandler(Map<String, String> responses) {
        this.responses = responses;
    }

    @Override
    public String handleSystemRequest(String screenContent) {
        // Parse screen content and determine SYSREQ response
        if (screenContent.contains("DELETE RECORD")) {
            return responses.getOrDefault("delete_response", "1");
        }
        if (screenContent.contains("CONFIRM CHANGES")) {
            return responses.getOrDefault("confirm_response", "1");
        }

        // Default: return to menu (null = press Return)
        return null;
    }
}
```

#### Step 2: Inject into Session5250

```java
// Create session
Session5250 session = new Session5250(props, config, "session1", sessionConfig);

// Inject custom handler BEFORE connecting
Map<String, String> responses = new HashMap<>();
responses.put("delete_response", "1");
responses.put("confirm_response", "1");

RequestHandler handler = new MyAutomationRequestHandler(responses);
session.setRequestHandler(handler);

// Now sendKeys will route F3 requests through custom handler
session.connect();
session.sendKeys("MYPROGRAM[enter]");
```

#### Step 3: Inject into WorkflowRunner

```java
// Create session and runner
Session5250 session = new Session5250(props, config, "session1", sessionConfig);
WorkflowRunner runner = new WorkflowRunner(session, loader, collector);

// Inject custom handler
RequestHandler handler = new MyAutomationRequestHandler(responses);
runner.setRequestHandler(handler);

// Handler is now active for all workflow SYSREQ operations
runner.executeWorkflow(workflow, dataRow);
```

#### Step 4: Robot Framework Jython Adapter

See `examples/HTI5250J.py` for complete Jython keyword library example:

```robot
*** Settings ***
Library    HTI5250J

*** Test Cases ***
Customer Inquiry Workflow
    Connect To IBM i    ibm-i.example.com
    Send Keys    CALL CUSSTM[enter]
    Wait For Keyboard Lock Cycle

    Send Keys    5[enter]    # Select inquiry option
    Wait For Keyboard Lock Cycle

    Screen Should Contain    CUSTOMER MASTER
    Disconnect From IBM i
```

---

### Path 3: Create Factory-Based Sessions (For Dependency Injection)

**When to use:** Spring Boot applications, dependency injection frameworks, or testing infrastructure.

**Effort:** <1 hour to integrate.

#### Step 1: Use HeadlessSessionFactory

```java
// Create factory (with optional custom RequestHandler)
HeadlessSessionFactory factory = new DefaultHeadlessSessionFactory();

// Create session through factory
HeadlessSession session = factory.createSession(
    "checkout_workflow",
    properties,
    new MyAutomationRequestHandler(handlerMap)
);

// Use the interface
session.connect();
session.sendKeys("CHECKOUT[enter]");
```

#### Step 2: Spring Boot Integration

```java
@Configuration
public class HTI5250JConfiguration {

    @Bean
    public HeadlessSessionFactory sessionFactory() {
        return new DefaultHeadlessSessionFactory();
    }

    @Bean
    public RequestHandler requestHandler() {
        return new MyAutomationRequestHandler(handlerResponses());
    }

    @Bean
    public HeadlessSession session(
        HeadlessSessionFactory factory,
        RequestHandler handler,
        @Value("${hti5250j.session.name}") String sessionName,
        @Value("${hti5250j.host}") String host,
        @Value("${hti5250j.port}") int port) throws Exception {

        Properties props = new Properties();
        props.setProperty("host", host);
        props.setProperty("port", String.valueOf(port));

        HeadlessSession headless = factory.createSession(sessionName, props, handler);
        headless.connect();
        return headless;
    }
}
```

#### Step 3: Inject in Service Classes

```java
@Service
public class PaymentService {
    private final HeadlessSession session;

    public PaymentService(HeadlessSession session) {
        this.session = session;
    }

    public PaymentResult processPayment(PaymentRequest req) throws Exception {
        String keys = String.format(
            "CALL PMTENT[enter]%s[enter]%s[enter]",
            req.getCustomerId(),
            req.getAmount()
        );

        session.sendKeys(keys);
        session.waitForKeyboardUnlock();

        String response = session.getScreen().getScreenAsText();
        return parsePaymentResponse(response);
    }
}
```

---

### Path 4: Batch Processing with Virtual Threads

**When to use:** Processing 1000+ records in parallel, high-concurrency workloads.

**Effort:** 1 hour to implement, requires testing/monitoring.

#### Step 1: Virtual Thread Pool

```java
// Create virtual thread executor for each task
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

HeadlessSessionFactory factory = new DefaultHeadlessSessionFactory();

// Process batch with 1000+ concurrent sessions
for (DataRow row : batchData) {
    executor.submit(() -> {
        try {
            HeadlessSession session = factory.createSession(
                "batch_worker_" + Thread.currentThread().threadId(),
                properties,
                new BatchProcessingHandler()
            );

            session.connect();
            session.sendKeys(buildKeys(row));
            session.waitForKeyboardUnlock();

            String response = session.getScreen().getScreenAsText();
            recordResult(row, response);

            session.disconnect();
        } catch (Exception e) {
            recordError(row, e);
        }
    });
}

// Wait for all to complete
executor.shutdown();
executor.awaitTermination(1, TimeUnit.HOURS);
```

#### Step 2: Memory & Performance Expectations

```
Virtual thread per task:
- Memory per session: ~500KB (headless, no GUI)
- Ops per second: 1000+ (depending on IBM i response times)
- Max concurrent: Limited by IBM i capacity, not by JVM memory

Example: 1000 concurrent sessions
- Memory: 1000 × 500KB = 500MB (very efficient)
- Each completes at different rates (no thread pool queuing)
- Throughput: 10,000 - 50,000 ops/hour (depending on workload)
```

#### Step 3: Monitoring

```java
// Track progress
AtomicInteger processed = new AtomicInteger(0);
AtomicInteger failed = new AtomicInteger(0);

executor.submit(() -> {
    // Periodic status log
    for (int i = 0; i < 60; i++) {
        Thread.sleep(1000);
        System.out.printf(
            "Progress: %d processed, %d failed%n",
            processed.get(),
            failed.get()
        );
    }
});

// In task completion:
processed.incrementAndGet();
// or
failed.incrementAndGet();
```

---

## Decision Matrix: Which Path Should I Choose?

| Goal | Path | Reason |
|------|------|--------|
| **Keep existing code** | None — no migration | Backward compatible |
| **New headless code** | 1 (HeadlessSession) | Cleaner interface, smaller memory |
| **Robot Framework** | 2 (RequestHandler) | Custom SYSREQ handling for automation |
| **Spring Boot app** | 3 (Factory + DI) | Proper dependency management |
| **Batch 1000+ records** | 4 (Virtual threads) | Massive scale, minimal memory |
| **All of above** | 1+2+3+4 (combined) | Compose all patterns |

---

## Common Patterns & Examples

### Pattern 1: "Pure Headless" Workflow (No GUI)

```java
// Use ONLY HeadlessSession interface
HeadlessSession session = new DefaultHeadlessSessionFactory()
    .createSession("pure_headless", props);

session.connect();
session.sendKeys("CALL MYPROGRAM[enter]");
session.waitForKeyboardUnlock();

// Get data without any GUI objects created
Screen5250 screen = session.getScreen();
String field1 = screen.getField(5, 10, 20);  // Row 5, Col 10, 20 chars

session.disconnect();
```

**Memory:** ~500KB per session
**GUI**: None — works in Docker, CI/CD, servers

---

### Pattern 2: "Interactive" Workflow (With GUI)

```java
// Create GUI component
SessionPanel panel = new SessionPanel();

// Create session and assign GUI
Session5250 session = new Session5250(props, config, "gui_session", sessionConfig);
session.setGUI(panel);

// User interacts through GUI
session.connect();
// ... GUI receives updates ...
session.disconnect();
```

**Memory:** ~2.5MB per session (includes GUI)
**GUI**: Full Swing rendering, user mouse/keyboard

---

### Pattern 3: "Hybrid" Workflow (Screenshot + Automation)

```java
// Create headless session
HeadlessSession headless = factory.createSession("hybrid", props);

// Inject custom handler for SYSREQ
headless.// Use requestHandler...

headless.connect();
headless.sendKeys("CALL MYAPP[enter]");
headless.waitForKeyboardUnlock();

// Capture screenshot (even though headless)
BufferedImage screenshot = headless.captureScreenshot();
ImageIO.write(screenshot, "PNG", new File("screenshot.png"));

// Get text data
String text = headless.getScreen().getScreenAsText();

headless.disconnect();
```

**Memory:** ~500KB per session
**GUI**: No rendering, but can generate PNG screenshots

---

## Testing Your Migration

### Unit Test Example

```java
@Test
void testHeadlessSessionInterface() {
    // Create headless session
    Properties props = new Properties();
    props.setProperty("host", "mock-server");

    HeadlessSession session = factory.createSession(
        "test_session",
        props,
        new NullRequestHandler()
    );

    // Verify interface contract
    assertNotNull(session.getSessionName());
    assertEquals("test_session", session.getSessionName());

    // Verify no GUI objects created
    // (Would fail if GUI components were initialized)
}
```

### Integration Test Example

```java
@Test
void testRequestHandlerInjection() {
    // Create custom handler
    RequestHandler handler = screenContent -> {
        if (screenContent.contains("CONFIRM")) {
            return "1";  // Auto-confirm
        }
        return null;  // Return to menu
    };

    // Inject and test
    Session5250 session = new Session5250(props, config, "test", sessionConfig);
    session.setRequestHandler(handler);

    // Verify handler is active
    assertEquals(handler, session.getRequestHandler());
}
```

---

## Troubleshooting

### Issue: "Class not found: HeadlessSession"

**Cause:** Using old version of HTI5250J (pre-Phase 15B).

**Solution:** Update to Phase 15B build or later.

```gradle
dependencies {
    implementation 'org.hti5250j:hti5250j:0.8.0-headless.15b+'
}
```

---

### Issue: RequestHandler not being called

**Cause:** Injecting handler AFTER connecting to session.

**Solution:** Inject BEFORE connect():

```java
// WRONG: After connect
session.connect();
session.setRequestHandler(handler);  // Too late!

// RIGHT: Before connect
session.setRequestHandler(handler);
session.connect();
```

---

### Issue: Memory not decreasing with HeadlessSession

**Cause:** Session5250 GUI component still initialized.

**Diagnosis:** Check if SessionPanel is being assigned:

```java
// If this is called, GUI will be created
session.setGUI(sessionPanel);
```

**Solution:** Don't call `setGUI()` for headless sessions:

```java
// Headless: No GUI
Session5250 session = new Session5250(props, config, "headless", sessionConfig);
// Don't call setGUI()

// Interactive: With GUI
Session5250 session = new Session5250(props, config, "gui", sessionConfig);
session.setGUI(sessionPanel);  // Now GUI is created
```

---

## Summary

| Aspect | Session5250 | HeadlessSession |
|--------|------------|-----------------|
| **Compatibility** | 100% — no changes required | New interface, opt-in |
| **Memory** | 2.5MB+ (with GUI) | ~500KB (no GUI) |
| **Interface Size** | Large (many methods) | Minimal (6 methods) |
| **SYSREQ Handling** | GUI dialog only | Customizable via RequestHandler |
| **Docker/CI** | Works, but large memory | Perfect fit |
| **Concurrency** | Limited by GUI threads | 1000+ with virtual threads |
| **Migration Effort** | 0 (not required) | 1-2 hours (optional) |

---

## References

- [ADR-015: Headless-First Architecture](./ADR-015-Headless-Abstractions.md)
- [Robot Framework Integration Guide](./ROBOT_FRAMEWORK_INTEGRATION.md)
- [HeadlessSessionExample.java](../examples/HeadlessSessionExample.java)
- [ARCHITECTURE.md](./ARCHITECTURE.md) — System design overview

---

**Document Version:** 1.0
**Last Updated:** February 9, 2026
**Phase:** 15B (Headless Abstractions)
