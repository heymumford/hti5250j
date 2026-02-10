# HTI5250J Technical Patterns vs Industry Standards

**Date:** February 2026
**Purpose:** Code-level comparison of HTI5250J patterns against industry standard libraries
**Audience:** Engineers evaluating integration patterns and implementation approaches

---

## Section 1: Session/Connection Lifecycle Patterns

### Pattern 1A: Session Cleanup (Current HTI5250J)

**Implementation:**
```java
// Current pattern (manual cleanup)
Session5250 session = new Session5250("host", 23);
try {
  session.connect();
  session.sendString("WRKSYSVAL");
  String screen = session.getScreenText();
} finally {
  session.disconnect();  // Manual cleanup required
}
```

**Problem:**
- If `connect()` throws, `disconnect()` never executes
- Requires explicit try-finally on every usage
- Forgotten finally → socket leak

**Verdict:** ⚠️ Pre-Java-7 pattern (Java 7+ expects AutoCloseable)

---

### Pattern 1B: Session Cleanup (Industry Standard - Java)

**Paramiko (Python model, Java equivalent):**
```python
with paramiko.SSHClient() as client:
    client.connect('host', 'user', 'password')
    # Auto cleanup on exit
```

**Java Equivalent (try-with-resources):**
```java
try (Session5250 session = new Session5250("host", 23)) {
  session.connect();
  session.sendString("WRKSYSVAL");
  String screen = session.getScreenText();
} // Guaranteed cleanup, even if exception
```

**Implementation (AutoCloseable):**
```java
public class Session5250 implements AutoCloseable {
  @Override
  public void close() {
    disconnect();  // Guaranteed cleanup
  }
}
```

**Verdict:** ✓ Industry standard since Java 7 (2011)

---

### Pattern 1C: Async Session Cleanup (Python Industry Standard)

**HTTPX (async HTTP client):**
```python
async with httpx.AsyncClient() as client:
    response = await client.get("https://example.com")
    # Auto cleanup on exit
```

**aiohttp (async HTTP client):**
```python
async with aiohttp.ClientSession() as session:
    async with session.get('http://api.example.com') as resp:
        data = await resp.text()
```

**Java Equivalent (if HTI5250J added async Kotlin):**
```kotlin
suspend fun useSession(host: String) {
  Session5250(host).use { session ->
    session.connect()
    session.sendString("WRKSYSVAL")
    val screen = session.getScreenText()
  }
}
```

**Verdict:** ✓ Modern async pattern (Python standard)

---

## Section 2: Connection Pooling Patterns

### Pattern 2A: Connection Pooling (Current HTI5250J)

**Current approach:**
```java
// Each Session5250 creates new socket
Session5250 session1 = new Session5250("host", 23);
session1.connect();
// ...
session1.disconnect();

Session5250 session2 = new Session5250("host", 23);
session2.connect();
// ... each session pays full TCP handshake cost
```

**Problem:**
- No connection reuse
- High overhead for 100+ sessions
- Each connect() → full telnet negotiation (RFC 854)
- Virtual threads help concurrency but not latency

**Benchmark Impact (estimated):**
- 1 session: 150ms (connect + negotiate)
- 100 sessions sequential: 15,000ms (15s)
- 100 sessions pooled: 150ms (reuse) + 100ms (multiplexing) = 250ms

---

### Pattern 2B: Connection Pooling (Requests/HTTP Standard)

**Requests (Python HTTP):**
```python
import requests
from requests.adapters import HTTPAdapter

session = requests.Session()  # Auto connection pooling
session.mount('http://', HTTPAdapter(pool_connections=10, pool_maxsize=10))

response1 = session.get('http://api.example.com/users')
response2 = session.get('http://api.example.com/posts')
# Both reuse TCP connection
```

**Paramiko (SSH):**
```python
client = paramiko.SSHClient()
client.connect('host', 'user', 'password')
transport = client.get_transport()

# Multiplex multiple channels over single Transport
channel1 = transport.open_session()
channel2 = transport.open_session()
# Both use same TCP connection
```

**HTTPX (modern HTTP):**
```python
client = httpx.Client(limits=httpx.Limits(max_connections=100))
response1 = client.get('http://api.example.com/users')
response2 = client.get('http://api.example.com/posts')
# Both reuse TCP connection, up to 100 concurrent
```

---

### Pattern 2C: Session Pooling (Recommended for HTI5250J)

**Proposed interface:**
```java
public interface Session5250Pool {
  /**
   * Borrow a session from the pool.
   * @return Session ready for use
   * @throws PoolExhaustedException if all sessions in use
   */
  Session5250 borrowSession() throws PoolExhaustedException;

  /**
   * Return a session to the pool for reuse.
   */
  void returnSession(Session5250 session);

  /**
   * Shutdown all pooled connections.
   */
  void shutdown();
}

public class Session5250PoolImpl implements Session5250Pool {
  private final String host;
  private final int port;
  private final Queue<Session5250> availableSessions;
  private final AtomicInteger activeSessions;
  private final int maxPoolSize;

  public Session5250PoolImpl(String host, int port, int maxPoolSize) {
    this.host = host;
    this.port = port;
    this.maxPoolSize = maxPoolSize;
    this.availableSessions = new ConcurrentLinkedQueue<>();
    this.activeSessions = new AtomicInteger(0);
  }

  @Override
  public Session5250 borrowSession() throws PoolExhaustedException {
    // Try to reuse existing session
    Session5250 session = availableSessions.poll();
    if (session != null && session.isConnected()) {
      return session;
    }

    // Create new session if pool not exhausted
    if (activeSessions.incrementAndGet() <= maxPoolSize) {
      Session5250 newSession = new Session5250(host, port);
      try {
        newSession.connect();
        return newSession;
      } catch (Exception e) {
        activeSessions.decrementAndGet();
        throw e;
      }
    }

    activeSessions.decrementAndGet();
    throw new PoolExhaustedException(
      String.format("All %d sessions in use", maxPoolSize)
    );
  }

  @Override
  public void returnSession(Session5250 session) {
    if (session.isConnected()) {
      availableSessions.offer(session);  // Reuse
    } else {
      activeSessions.decrementAndGet();  // Mark as released
    }
  }

  @Override
  public void shutdown() {
    Session5250 session;
    while ((session = availableSessions.poll()) != null) {
      try {
        session.disconnect();
      } catch (Exception e) {
        // Log, don't throw
      }
    }
  }
}
```

**Usage:**
```java
Session5250Pool pool = new Session5250PoolImpl("host", 23, 10);

try {
  Session5250 session = pool.borrowSession();
  try {
    session.sendString("WRKSYSVAL");
    String screen = session.getScreenText();
  } finally {
    pool.returnSession(session);  // Return to pool
  }
} finally {
  pool.shutdown();
}
```

**Verdict:** ⚠️ Not yet implemented, but follows industry standards

---

## Section 3: Error Context Patterns

### Pattern 3A: Basic Exception (Common Anti-Pattern)

**Anti-pattern:**
```java
throw new Exception("Failed");  // No context
```

**Problem:**
- Impossible to debug
- Lost causation chain
- No state information

---

### Pattern 3B: HTI5250J Exception Pattern (Good)

**AssertionException (from Phase 11):**
```java
public class AssertionException extends WorkflowException {
  private final String expectedText;
  private final String screenDump;  // Full screen context
  private final long timeoutMs;

  public AssertionException(
    String message,
    String expectedText,
    String screenDump,
    long timeoutMs) {
    super(message);
    this.expectedText = expectedText;
    this.screenDump = screenDump;
    this.timeoutMs = timeoutMs;
  }

  @Override
  public String toString() {
    return String.format(
      "%s\nExpected: %s\nScreen dump:\n%s\nTimeout: %dms",
      getMessage(),
      expectedText,
      screenDump,
      timeoutMs
    );
  }
}
```

**Usage:**
```java
if (!screenText.contains(expectedText)) {
  String dump = formatScreenDump(screenText, 80);
  throw new AssertionException(
    "Screen did not contain expected text",
    expectedText,
    dump,
    5000
  );
}
```

**Verdict:** ✓ Excellent context inclusion

---

### Pattern 3C: Industry Standard Exception Pattern

**Java.io pattern (established):**
```java
public class NavigationException extends IOException {
  private final String currentScreen;
  private final String targetScreen;
  private final Throwable cause;

  public NavigationException(
    String message,
    String currentScreen,
    String targetScreen,
    Throwable cause) {
    super(message, cause);  // Preserve cause chain
    this.currentScreen = currentScreen;
    this.targetScreen = targetScreen;
  }

  public String getCurrentScreen() {
    return currentScreen;
  }

  public String getTargetScreen() {
    return targetScreen;
  }
}

// Usage:
try {
  navigateToScreen(target);
} catch (IOException e) {
  throw new NavigationException(
    "Cannot navigate: " + e.getMessage(),
    currentScreen,
    targetScreen,
    e  // Preserve cause
  );
}
```

**Verdict:** ✓ HTI5250J matches industry standard

---

## Section 4: Synchronous Polling Pattern

### Pattern 4A: Polling Loop (HTI5250J Current)

**Implementation:**
```java
private void waitForKeyboardUnlock(long timeoutMs)
    throws TimeoutException {
  long deadline = System.currentTimeMillis() + timeoutMs;

  while (true) {
    // Check timeout
    if (System.currentTimeMillis() > deadline) {
      throw new TimeoutException(
        String.format(
          "Keyboard unlock timeout (%dms). OIA: %s",
          timeoutMs, screen.getOIA().getStatus()
        )
      );
    }

    // Poll OIA (every 100ms)
    if (screen.getOIA().isKeyboardAvailable()) {
      return;  // Ready
    }

    // Sleep before next poll
    Thread.sleep(100);  // 100ms interval
  }
}
```

**Characteristics:**
- ✓ Simple and clear
- ✓ Virtual threads support (1KB per thread)
- ✓ Timeout prevents hangs
- ✓ Poll interval tuned (100ms)

**Verdict:** ✓ Appropriate for Java with virtual threads

---

### Pattern 4B: Async/Await Alternative (Python)

**Python (async version):**
```python
async def wait_for_keyboard_unlock(screen, timeout_ms=5000):
    """Wait for keyboard to unlock asynchronously."""
    deadline = time.time() + timeout_ms / 1000.0

    while True:
        if time.time() > deadline:
            raise TimeoutError(
                f"Keyboard unlock timeout ({timeout_ms}ms). "
                f"OIA: {screen.oia.status}"
            )

        if screen.oia.is_keyboard_available():
            return  # Ready

        await asyncio.sleep(0.1)  # 100ms interval
```

**Characteristics:**
- ✓ True async (no threads)
- ✓ Efficient (sleeps without thread)
- ✓ Works with asyncio ecosystem
- ✗ Java doesn't have true await syntax

**Note:** Project Loom (virtual threads) provides equivalent efficiency to async/await in Python.

---

### Pattern 4C: Reactive Streams Alternative (Java)

**Project Reactor (Flux):**
```java
public Mono<Void> waitForKeyboardUnlock(Duration timeout) {
  return Mono.create(sink -> {
    Instant deadline = Instant.now().plus(timeout);

    Flux.interval(Duration.ofMillis(100))
      .takeUntil(_ -> Instant.now().isAfter(deadline))
      .flatMap(_ -> {
        if (screen.getOIA().isKeyboardAvailable()) {
          return Mono.empty();
        }
        return Mono.error(new TimeoutException("..."));
      })
      .subscribe(
        _ -> sink.success(),
        sink::error
      );
  });
}
```

**Characteristics:**
- ✓ Reactive (composable)
- ✓ Works with Spring Boot Reactive
- ✗ More complex than polling
- ✗ Overkill for simple wait-for-state scenario

**Verdict:** ✗ Over-engineered for HTI5250J use case

---

## Section 5: Test Fixture Patterns

### Pattern 5A: JUnit 5 Test Fixture (Current HTI5250J)

**Domain 1 (Unit Test):**
```java
@Test
public void ebcdicDecoderHandlesBasicCharacters() {
  byte[] ebcdic = new byte[] {(byte) 0xC8, (byte) 0x85, (byte) 0x93, (byte) 0x93, (byte) 0x96};
  String result = EBCDICCodec.ebcdicToString(ebcdic);
  assertEquals("Hello", result);
}
```

**Domain 3 (Surface Test with Mock):**
```java
@Test
public void fillHandlerPopulatesFieldsCorrectly() {
  // Setup
  Screen5250 mockScreen = mock(Screen5250.class);
  Session5250 mockSession = mock(Session5250.class);
  FillHandler handler = new FillHandler(mockSession);

  // Execute
  Map<String, String> fields = Map.of(
    "account", "ACC001",
    "amount", "150.00"
  );
  handler.handle(fields);

  // Verify
  verify(mockSession).sendString("ACC001");
  verify(mockSession).sendKey(KeyCode.TAB);
}
```

**Domain 4 (Scenario Test with Real Session - commented, requires i5):**
```java
@Disabled("Requires real IBM i system")
@Test
public void paymentWorkflowSucceedsEndToEnd() throws Exception {
  Session5250 session = new Session5250("ibmi.example.com", 23);
  try {
    session.connect();
    session.sendString("WRKSYSVAL");
    session.sendKey(KeyCode.ENTER);
    String screen = session.getScreenText();
    assertTrue(screen.contains("Work with System Values"));
  } finally {
    session.disconnect();
  }
}
```

**Verdict:** ✓ Four-domain architecture well-designed

---

### Pattern 5B: pytest-asyncio Fixture (Python Standard)

**Async fixture (FastAPI):**
```python
import pytest
from httpx import AsyncClient
from fastapi import FastAPI

app = FastAPI()

@pytest.fixture
async def client():
    async with AsyncClient(app=app, base_url="http://test") as ac:
        yield ac  # Test runs here with client
    # Cleanup guaranteed here

@pytest.mark.asyncio
async def test_get_users(client):
    response = await client.get("/users")
    assert response.status_code == 200
```

**Characteristics:**
- ✓ Async-first
- ✓ Guaranteed cleanup (after yield)
- ✓ Works with pytest's event loop handling
- ✗ Only for async code

---

### Pattern 5C: Proposed HTI5250J Fixture Pattern

**With AutoCloseable (Java):**
```java
@Test
public void fillHandlerPopulatesFields() {
  try (Session5250 session = new Session5250("mock-host", 23)) {
    // Setup mock
    session.connect();

    // Test logic
    session.sendString("TEST");

    // Cleanup guaranteed
  }
}
```

**Verdict:** Simple, matches Java idiom

---

## Section 6: Parameter Substitution Patterns

### Pattern 6A: HTI5250J Current (String Template Substitution)

**YAML:**
```yaml
- action: FILL
  fields:
    account: "${data.account_id}"
    amount: "${data.amount}"
```

**CSV:**
```
account_id,amount,description
ACC001,150.00,Invoice-001
```

**Substitution Logic:**
```java
private Map<String, String> substituteParameters(
  Map<String, String> template,
  Map<String, String> dataSet) {

  Map<String, String> result = new HashMap<>();
  for (Map.Entry<String, String> entry : template.entrySet()) {
    String value = entry.getValue();

    if (value.startsWith("${data.") && value.endsWith("}")) {
      String columnName = value.substring(7, value.length() - 1);
      if (!dataSet.containsKey(columnName)) {
        throw new ParameterException("Missing: " + columnName);
      }
      result.put(entry.getKey(), dataSet.get(columnName));
    } else {
      result.put(entry.getKey(), value);
    }
  }
  return result;
}
```

**Verdict:** ✓ Simple and effective for data-driven testing

---

### Pattern 6B: Selenium WebDriver Parameter Pattern (Comparative)

**Selenium (TestNG + data provider):**
```java
@DataProvider
public Object[][] loginData() {
  return new Object[][] {
    { "user1", "pass1" },
    { "user2", "pass2" }
  };
}

@Test(dataProvider = "loginData")
public void loginTest(String user, String password) {
  driver.findElement(By.name("username")).sendKeys(user);
  driver.findElement(By.name("password")).sendKeys(password);
}
```

**Characteristics:**
- ✓ Data comes from code (type-safe)
- ✗ Not declarative (requires Java knowledge)
- ✗ Hard to share data with QA team

**Verdict:** HTI5250J's YAML approach is better for non-technical users

---

## Section 7: Multi-Language API Design

### Pattern 7A: Selenium WebDriver (Multi-Language Reference)

**Available Bindings:**
- Java (official)
- Python (official)
- C# (official)
- Ruby (official)
- JavaScript/Node.js (official)
- Kotlin (community)
- Go (community)

**Python Example:**
```python
from selenium import webdriver
from selenium.webdriver.common.by import By

driver = webdriver.Chrome()
driver.get("https://example.com")
driver.find_element(By.ID, "search").send_keys("test")
driver.quit()
```

**Java Example:**
```java
WebDriver driver = new ChromeDriver();
driver.get("https://example.com");
driver.findElement(By.id("search")).sendKeys("test");
driver.quit();
```

**Characteristics:**
- ✓ API parity across languages
- ✓ Same semantics (method names, error handling)
- ✓ Large ecosystem (tools, IDE support)

---

### Pattern 7B: Paramiko (Pure Python, Java Wrapper Alternative)

**Paramiko (pure Python):**
```python
client = paramiko.SSHClient()
client.connect('host', username='user', password='pass')
stdin, stdout, stderr = client.exec_command('ls -la')
```

**Java Wrapper Option (via JSch):**
```java
JSch jsch = new JSch();
Session session = jsch.getSession("user", "host", 22);
session.setPassword("pass");
session.setConfig("StrictHostKeyChecking", "no");
session.connect();
ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
channelExec.setCommand("ls -la");
// ...
```

**Characteristics:**
- ✓ Python implementation established (standard)
- ✗ Java wrapper requires learning two libraries
- Lesson: Python-first may be strategic (then Java wrapper)

---

### Pattern 7C: Proposed HTI5250J Multi-Language Strategy

**Tier 1 (Official):**
- Java (existing)
- Python (add via Py4J or cffi bridge)

**Tier 2 (Community):**
- REST API (enables any language)
- TypeScript/JavaScript (optional)

**Implementation Priority:**
1. Python client (Phase 15B) — reaches RPA/data science
2. Robot Framework library (Phase 15C) — reaches enterprise testing
3. REST API (Phase 16) — enables polyglot integration

---

## Section 8: Sealed Classes for Type Safety

### Pattern 8A: Unseal (Pre-Java 17, HTI5250J Alternative)

**Anti-pattern (vulnerable):**
```java
enum ActionType { LOGIN, NAVIGATE, FILL, SUBMIT, ASSERT, CAPTURE }

class StepDef {
  ActionType action;
  String host;
  String user;
  String password;
  // ... 11 more fields, most unused per action
}

// Dispatch (vulnerable to missing handler)
switch (action) {
  case LOGIN -> handleLogin(step);
  case NAVIGATE -> handleNavigate(step);
  // Compiler doesn't warn if ASSERT handler is missing
}

private void handleLogin(StepDef step) {
  // step has 14 fields, only needs 3
  // Confusing: what is step.screen? Is it used here?
  // Potential null pointer if field not set
}
```

**Problem:**
- Generic StepDef has all fields
- Handler receives unused fields (confusion)
- Missing switch case is silent failure (not compile error)

---

### Pattern 8B: Sealed Interface (Java 17+, HTI5250J Phase 12D)

**Sealed interface:**
```java
sealed interface Action permits
  LoginAction, NavigateAction, FillAction,
  SubmitAction, AssertAction, CaptureAction {}

// Each action is a record with only needed fields
final record LoginAction(String host, String user, String password) implements Action {}
final record NavigateAction(String screen, String keys) implements Action {}
final record FillAction(Map<String, String> fields) implements Action {}
final record SubmitAction(String key) implements Action {}
final record AssertAction(String text) implements Action {}
final record CaptureAction(String name) implements Action {}

// Factory converts StepDef → Action
Action action = ActionFactory.from(stepDef);

// Exhaustive switch (compiler ERROR if handler missing)
switch (action) {
  case LoginAction login -> handleLogin(login);
  case NavigateAction nav -> handleNavigate(nav);
  case FillAction fill -> handleFill(fill);
  // ... all cases required, or compiler error
}

// Handler receives exact type
private void handleLogin(LoginAction login) {
  // Only 3 fields: host, user, password
  // No confusion, no null checks needed
}
```

**Characteristics:**
- ✓ Compile-time exhaustiveness checking
- ✓ Type clarity (handler knows exact fields)
- ✓ Records enforce immutability
- ✓ Pattern matching support

**Verdict:** ✓ HTI5250J already implements (Phase 12D)

---

### Pattern 8C: Comparison with Rust Enums

**Rust (for reference):**
```rust
enum Action {
  Login { host: String, user: String, password: String },
  Navigate { screen: String, keys: String },
  Fill { fields: HashMap<String, String> },
}

match action {
  Action::Login { host, user, password } => handle_login(host, user, password),
  Action::Navigate { screen, keys } => handle_navigate(screen, keys),
  Action::Fill { fields } => handle_fill(fields),
}
```

**Characteristics:**
- ✓ Exhaustiveness checking
- ✓ Type safety
- ✓ No inheritance overhead

**Verdict:** Java sealed classes + records ≈ Rust enums

---

## Section 9: Performance Characteristics

### Benchmark 9A: Virtual Threads vs Platform Threads

**Test Scenario:**
- 1000 concurrent HTI5250j sessions
- Each session: connect, send, receive, disconnect
- Measure: throughput (ops/sec), memory (MB), latency (ms)

**Results (from Phase 2 documentation):**

| Metric | Platform Threads | Virtual Threads | Improvement |
|--------|------------------|-----------------|-------------|
| Throughput | 58K ops/sec | 587K ops/sec | 10x |
| Memory per Thread | 1MB | 1KB | 1000x |
| Max Concurrent | 10K | Unlimited | Unlimited |
| Context Switch | High | Low | Better |

**Source:** TESTING_EPISTEMOLOGY.md (Phase 2 verification)

**Verdict:** ✓ Virtual threads provide dramatic scalability

---

### Benchmark 9B: Session Pooling Impact (Projected)

**Scenario:**
- 100 sequential operations to same host
- Without pool: 100 × full TCP handshake
- With pool: 1 × full handshake + 99 × reuse

**Projected Improvement:**
- Per-connect cost: ~150ms (TCP handshake + telnet negotiation)
- 100 operations without pool: 15,000ms
- 100 operations with pool: ~200ms (minimal overhead)
- **Improvement: 75x** faster

**Note:** Not yet benchmarked (pool not yet implemented)

---

## Section 10: Recommended Implementation Roadmap

### Quick Reference Table

| Pattern | Current | Needed | Priority | Effort |
|---------|---------|--------|----------|--------|
| AutoCloseable cleanup | ✗ | ✓ | 1 | 6h |
| Session pooling | ✗ | ✓ | 2 | 20h |
| Python client | ✗ | ✓ | 1 | 30h |
| Robot Framework | ✗ | ✓ | 2 | 40h |
| REST API | ✗ | ✓ | 3 | 50h |
| Async API (Kotlin) | ✗ | ⚠️ | 3 | 40h |
| Structured logging | ✗ | ⚠️ | 3 | 20h |

---

**Document Version:** 1.0
**Last Updated:** February 9, 2026
**Status:** Reference material for Phase 15+ implementation
