# HTI5250J Integration Barriers — Quick Reference

**For:** Python/Robot Framework adapter developers
**Last Updated:** February 9, 2026

---

## TL;DR: Can I integrate Python with HTI5250J?

**YES, but with caveats.**

| Use Case | Difficulty | Est. Time | Reverse Engineer? |
|----------|-----------|-----------|---|
| Read 5250 screen, send keystrokes | **Easy** | 4-6 hours | No |
| Automate with Robot Framework | **Easy** | 8-12 hours | No |
| Execute workflows programmatically | **Hard** | 15-20 hours | **Yes** |
| Record/replay macros | **Very Hard** | 30+ hours | **Yes** |

---

## The 3 Core Barriers

### Barrier #1: GUI Layer Coupling (Session5250)

**Problem:** Session5250 imports Swing/AWT even though headless mode exists.

```java
// Line 13 of Session5250.java
import java.awt.Toolkit;  // ❌ Forces GUI dependency

// Line 177-179: Will fail in headless environment
public void signalBell() {
    Toolkit.getDefaultToolkit().beep();  // ❌ No display available
}
```

**Impact:** Python adapters must subclass Session5250 and override these methods.

**Workaround:** Create a HeadlessSession5250 subclass:
```python
# Create Java stub
public class HeadlessSession5250 extends Session5250 {
    @Override
    public void signalBell() {
        // Log instead of beep
        logger.debug("Bell signal (headless mode)");
    }

    @Override
    public String showSystemRequest() {
        // Return null or log instead
        return null;
    }
}
```

**Effort to Fix:** 3 hours (HTI5250J maintainer)

---

### Barrier #2: No Workflow Executor API

**Problem:** Workflows can only be executed via CLI, not programmatically.

```bash
# ✓ This works
./gradlew run --args="payment.yaml payment_data.csv"

# ✗ This doesn't exist
WorkflowExecutor executor = new WorkflowExecutor();
executor.execute(workflow, dataset);
```

**Impact:** Python adapters MUST either:
1. Invoke HTI5250J CLI via subprocess (fragile, slow)
2. Reverse-engineer ActionFactory/WorkflowRunner (violates encapsulation)
3. Reimplement workflow logic in Python (duplication)

**Workaround:** Invoke CLI from Python:
```python
import subprocess

result = subprocess.run([
    "./gradlew", "run",
    "--args", "payment.yaml payment_data.csv"
], capture_output=True)
```

**Effort to Fix:** 6 hours (HTI5250J maintainer)

---

### Barrier #3: SessionPanel JPanel Requirement

**Problem:** Session5250 is designed with GUI-component awareness.

```java
// Line 79 of SessionPanel.java
public SessionPanel(Session5250 session) {
    this.session = session;
    session.setGUI(this);  // ← Back-reference creates coupling
    // ... lots of Swing initialization ...
}
```

**Impact:** Even headless Python code must be aware of SessionPanel existence.

**Workaround:** Don't instantiate SessionPanel for headless code:
```python
from jpype import JClass

Session5250 = JClass('org.hti5250j.Session5250')
config = JClass('org.hti5250j.SessionConfig')

# ✓ This is fine (SessionPanel optional)
session = Session5250(props, resource, name, config)
session.setGUI(None)  # Explicitly tell it no GUI
session.connect()
```

**Effort to Fix:** 4 hours (HTI5250J maintainer; extract headless base class)

---

## Public APIs That Work (Don't Reverse Engineer)

### ✓ Always Accessible (Pure API, No GUI)

```python
from jpype import JClass

Session5250 = JClass('org.hti5250j.Session5250')
session = Session5250(props, resource, name, config)

# ✓ These work
session.connect()                    # TCP to IBM i
session.disconnect()                 # Clean shutdown
session.isConnected()                # Check state

screen = session.getScreen()         # Get Screen5250
text = screen.getText()              # 1920 chars (80×24)
oia = screen.getOIA()                # Keyboard state
oia.isKeyboardAvailable()            # Polling interface
screen.moveCursor(100)               # Set cursor position
screen.sendKeys(ENTER)               # Send key code
```

### ✓ Configuration (Java Properties)

```python
props = JClass('java.util.Properties')()
props.setProperty('host', 'ibmi.example.com')
props.setProperty('port', '23')
props.setProperty('user', 'TESTUSER')

config = JClass('org.hti5250j.SessionConfig')('defaults.props', 'MY_SESSION')
config.getProperties().load(...)     # Load from file
config.getStringProperty('host')     # Read back
```

### ✓ Listeners (Event-driven)

```python
session.addSessionListener(my_listener)
session.removeSessionListener(my_listener)

# Listener will be called when connection state changes
```

### ✓ Virtual Threads (Transparent)

```python
# Can spawn 1000+ concurrent Session5250 instances
# No configuration needed; Java 21+ handles automatically
# Each uses ~1KB memory (vs 1MB for platform threads)

for i in range(1000):
    session = Session5250(...)
    session.connect()  # Efficient due to virtual threads
```

---

## APIs That Need Reverse Engineering

### ❌ Workflow Execution

**File:** `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/src/org/hti5250j/workflow/ActionFactory.java`

**Classes to reverse-engineer:**
- StepDef (YAML deserialized step)
- Action (sealed type with LOGIN, NAVIGATE, FILL, etc.)
- ActionFactory (converts StepDef → Action)
- WorkflowRunner (executes actions)

**Why needed:** No public `execute(workflow, data)` method

**Code example (reverse-engineered):**
```python
# This requires understanding HTI5250J internals
ActionFactory = JClass('org.hti5250j.workflow.ActionFactory')
stepDef = JClass('org.hti5250j.workflow.StepDef')
# ... much trial & error ...
action = ActionFactory.from(stepDef)
```

---

### ❌ Keyboard/Keypad Customization

**File:** `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/src/org/hti5250j/keyboard/KeyMnemonic.java`

**Classes to reverse-engineer:**
- KeyMnemonic (enum with F1, ENTER, TAB, etc.)
- KeyMnemonicResolver (maps key codes)

**Why needed:** Custom key bindings not exposed as API

---

### ❌ Macro Recording/Playback

**File:** `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/src/org/hti5250j/tools/Macronizer.java`

**Classes to reverse-engineer:**
- Macronizer (records/replays keystroke sequences)
- Macro format (likely proprietary binary format)

**Why needed:** SessionPanel-only, no headless API

**Note:** Probably not worth the effort; Robot Framework keywords achieve same goal.

---

## Recommended Integration Paths

### Path 1: Direct Session API (Recommended for 80% of Use Cases)

```python
# Minimal reverse engineering; uses public APIs directly

from jpype import JClass, JInt

# Setup
Session5250 = JClass('org.hti5250j.Session5250')
SessionConfig = JClass('org.hti5250j.SessionConfig')
Properties = JClass('java.util.Properties')
KeyMnemonic = JClass('org.hti5250j.keyboard.KeyMnemonic')

props = Properties()
props.setProperty('host', 'ibmi.example.com')
props.setProperty('port', '23')

config = SessionConfig('defaults.props', 'MY_SESSION')
session = Session5250(props, 'resource', 'session', config)

# Connect
session.connect()
# Pseudo: wait for login screen...

# Interact
screen = session.getScreen()
screen.sendKeys(KeyMnemonic.ENTER)
# Pseudo: wait for screen update...

text = screen.getText()
print(text[:80])  # First line of screen

session.disconnect()
```

**Effort:** 4-6 hours
**Can I use Robot Framework?** Yes (create keyword wrapper)
**Maintenance burden:** Low (only public APIs used)

---

### Path 2: Workflow CLI Wrapper (Pragmatic Middle Ground)

```python
# Use existing YAML/CSV infrastructure; invoke via subprocess

import subprocess
import json

def execute_workflow_via_cli(yaml_file, csv_file):
    result = subprocess.run(
        ['./gradlew', 'run', '--args', f'{yaml_file} {csv_file}'],
        capture_output=True,
        text=True
    )
    return {
        'success': result.returncode == 0,
        'stdout': result.stdout,
        'stderr': result.stderr
    }

# Call from Python
result = execute_workflow_via_cli('payment.yaml', 'payment_data.csv')
```

**Effort:** 2-3 hours
**Can I use Robot Framework?** Yes (keyword wrapper around subprocess)
**Maintenance burden:** Medium (depends on CLI stability; fragile to version changes)
**Performance:** Slow (subprocess overhead + full JVM startup per call)

---

### Path 3: REST Bridge (Most Flexible, Most Work)

```python
# Implement HTTP wrapper around HTI5250J (reverse-engineer required)

# Java side: Create REST controller
@RestController
@RequestMapping("/api/sessions")
public class SessionController {
    @PostMapping
    public SessionDTO create(@RequestBody SessionConfig config) { ... }

    @GetMapping("/{id}/screen")
    public ScreenDTO getScreen(@PathVariable String id) { ... }

    @PostMapping("/{id}/send")
    public void sendKeys(@PathVariable String id, @RequestBody KeyCommand cmd) { ... }
}

# Python side: Call REST API
import requests

class HTI5250JClient:
    def __init__(self, base_url='http://localhost:8080'):
        self.base_url = base_url

    def create_session(self, host, user, password):
        return requests.post(f'{self.base_url}/api/sessions', json={
            'host': host,
            'user': user,
            'password': password
        }).json()

    def get_screen(self, session_id):
        return requests.get(f'{self.base_url}/api/sessions/{session_id}/screen').json()

    def send_keys(self, session_id, key_code):
        return requests.post(f'{self.base_url}/api/sessions/{session_id}/send', json={
            'key': key_code
        })
```

**Effort:** 20-30 hours
**Can I use Robot Framework?** Yes (call HTTP wrapper)
**Maintenance burden:** High (need to maintain bridge code)
**Benefit:** Language-agnostic (Python, Node, Go, etc. all work)

---

## Decision Matrix

**Choose your path:**

```
Do you need to execute
YAML workflows
programmatically?
    ├─ NO  → Use Path 1 (Direct API) ✓
    │       - Read/write 5250 screens
    │       - Send keystrokes
    │       - Create Robot keywords
    │       - Effort: 4-6 hours
    │
    └─ YES → Choose between:
        ├─ Path 2 (CLI Wrapper)
        │  - Accept subprocess overhead
        │  - Effort: 2-3 hours
        │  - Only if you own the YAML files
        │
        ├─ Path 3 (REST Bridge)
        │  - Need language-agnostic API
        │  - Effort: 20-30 hours
        │  - Best for microservices/multi-language teams
        │
        └─ File GitHub Issue
           - Request public WorkflowExecutor API
           - Effort: 6 hours (if approved)
           - Best long-term solution
```

---

## Files You'll Need

### For Direct API Integration (Path 1)

- Main JAR: `build/libs/hti5250j-[version]-all.jar`
- Dependencies: None (all-in-one JAR)
- Source reference:
  - `/src/org/hti5250j/Session5250.java` (read for understanding)
  - `/src/org/hti5250j/framework/tn5250/Screen5250.java`
  - `/src/org/hti5250j/framework/tn5250/ScreenOIA.java`

### For CLI Wrapper (Path 2)

- Gradle wrapper: `./gradlew`
- Workflow YAML: `examples/*.yaml`
- CSV data: `examples/*.csv`
- CLI entry: `WorkflowCLI.main(String[])`

### For REST Bridge (Path 3)

- Same as Path 1, PLUS:
- Spring Boot knowledge (to implement REST layer)
- Understand HTTP/REST conventions
- Reverse-engineer WorkflowRunner internals

---

## Common Pitfalls

### Pitfall #1: Assuming GUI Is Not Required

**Problem:**
```python
# This will load org.hti5250j.gui.SystemRequestDialog
# Which requires Swing/AWT, which may fail on headless servers
session = Session5250(props, ...)
if some_condition:
    session.showSystemRequest()  # ❌ Will throw HeadlessException
```

**Solution:** Override in a headless subclass:
```java
public class HeadlessSession5250 extends Session5250 {
    @Override
    public String showSystemRequest() {
        return null;  // or log a warning
    }
}
```

---

### Pitfall #2: Not Polling OIA Before Reading Screen

**Problem:**
```python
# This returns screen state from BEFORE keystroke was processed
session.sendString("WRKSYSVAL")
text = session.getScreen().getText()  # ❌ Still shows old screen
```

**Solution:** Poll OIA until screen updates:
```python
session.sendString("WRKSYSVAL")

# Poll until keyboard becomes available (screen refreshed)
deadline = time.time() + 5.0
while time.time() < deadline:
    if session.getScreen().getOIA().isKeyboardAvailable():
        break
    time.sleep(0.1)

text = session.getScreen().getText()  # ✓ Now has new screen
```

---

### Pitfall #3: Using Properties File Without Loading

**Problem:**
```python
# This doesn't load from file; Properties are empty
config = SessionConfig('defaults.props', 'MY_SESSION')
```

**Solution:** Load from file explicitly (or use SessionConfigBuilder):
```python
config = SessionConfig('defaults.props', 'MY_SESSION')
# SessionConfig auto-loads during construction (check line 73)
```

---

### Pitfall #4: Forgetting to Disconnect

**Problem:**
```python
session = Session5250(props, ...)
session.connect()
if error_condition:
    return  # ❌ Socket still open, TCP connection leaks
```

**Solution:** Use try-finally or context manager:
```python
session = Session5250(props, ...)
try:
    session.connect()
    # Do work
finally:
    if session.isConnected():
        session.disconnect()
```

---

## Testing Your Integration

### 1. Unit Test (No i5 Required)

```python
from unittest.mock import Mock

def test_can_read_screen_text():
    session = create_test_session()
    screen = session.getScreen()
    text = screen.getText()
    assert len(text) == 1920  # 80 * 24
    assert text is not None
```

### 2. Contract Test (Validates Expected Behavior)

```bash
cd /Users/vorthruna/ProjectsWATTS/tn5250j-headless
./gradlew test --tests "SessionInterfaceContractTest"
```

### 3. Integration Test (Requires Real IBM i)

```robot
*** Settings ***
Library    MyHTI5250jAdapter

*** Test Cases ***
Can Connect To Real System
    [Tags]    integration    requires_ibmi
    Connect    ibmi.example.com    TESTUSER    TESTPASS
    Should Be Connected
    Disconnect
```

---

## Getting Help

### Documentation

- **Architecture:** `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/ARCHITECTURE.md` (detailed)
- **This file:** PYTHON_ROBOT_INTEGRATION_ANALYSIS.md (comprehensive)
- **Contributing:** `/Users/vorthruna/ProjectsWATTS/tn5250j-headless/CONTRIBUTING.md` (license, attribution)

### Reverse Engineering Resources

- **Extract method signatures:** `javap -private -cp build/classes org.hti5250j.workflow.ActionFactory`
- **Read source:** GitHub (https://github.com/heymumford/tn5250j)
- **Debug:** Enable `-Xmx` and add breakpoints in IDE

### File GitHub Issues

Request features:
- "Expose public WorkflowExecutor API for programmatic access"
- "Create SessionConfigBuilder for fluent configuration"
- "Export public MockSession5250 for testing"

---

## Summary

| Need | Effort | Use Public API? | Recommended |
|------|--------|---|---|
| Read 5250 screen | Easy (4h) | ✓ Yes | Path 1 |
| Send keystrokes | Easy (4h) | ✓ Yes | Path 1 |
| Create Robot Framework | Easy (8h) | ✓ Yes | Path 1 + keywords |
| Execute workflows | Hard (6h+) | ❌ No (CLI only) | Path 2 or 3 |
| Custom actions | Medium (10h) | ⚠ Partial | Path 3 + reverse-engineer |
| Record macros | Very Hard (30h) | ❌ No | Not recommended |

---

**For questions or issues:** File GitHub issue at https://github.com/heymumford/tn5250j/issues
