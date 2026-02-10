# HTI5250J — Robot Framework Integration Guide

## Overview

HTI5250J (Phase 15B) provides clean headless abstractions enabling seamless Robot Framework integration for IBM i (AS/400) test automation.

**Key Advantage:** No GUI coupling. Tests run in Docker, CI/CD pipelines, and headless servers without X11 or display system.

---

## Architecture

```
┌─────────────────────────────────┐
│ Robot Framework Test Suite      │
│ (*.robot files)                 │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ Jython Keyword Library          │
│ (HTI5250J Bridge)               │
├─────────────────────────────────┤
│ ○ Connect To IBM i              │
│ ○ Send Keys To Screen           │
│ ○ Capture Screen                │
│ ○ Assert Screen Contains        │
│ ○ Handle System Request         │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ HeadlessSession (Phase 15B)      │
│ (Pure data API, no GUI)         │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│ Session5250                     │
│ (Facade with RequestHandler)    │
└────────────┬────────────────────┘
             │
      ┌──────┴──────┐
      ▼             ▼
  Screen5250     tnvt Protocol
  (Data Model)   (TN5250E)
```

---

## Prerequisites

### Required
- **Jython 2.7.x** or **Jython 3.x** (Python 2/3 for Robot Framework)
- **Robot Framework 5.x+**
- **HTI5250J** (Java 21+)
- **IBM Toolbox for Java** (jt400.jar) — already included in lib/runtime/

### Optional
- **Docker** (for containerized tests)
- **Docker Compose** (for multi-session workflows)

### System Requirements
- **No X11 display system required** ✓ (headless)
- **No GUI libraries** ✓ (pure data APIs)
- **~500KB per session** ✓ (vs 2MB+ with GUI)

---

## Installation

### Step 1: Set Up Jython Environment

```bash
# Download Jython 2.7.x or 3.x
wget https://repo1.maven.org/maven2/org/python/jython/jython-installer/2.7.3/jython-installer-2.7.3.jar
java -jar jython-installer-2.7.3.jar -s -d ~/jython

# Add Jython to PATH
export PATH=~/jython/bin:$PATH
jython --version
```

### Step 2: Install Robot Framework

```bash
# Using Jython's pip equivalent
jython -m pip install robotframework==5.1.1
robot --version
```

### Step 3: Build HTI5250J

```bash
cd /path/to/tn5250j-headless
./gradlew build
# Creates: build/classes/java/main/*.jar
```

### Step 4: Create Keyword Library

Create `lib/HTI5250J.py`:

```python
"""
HTI5250J Robot Framework Keyword Library

Provides keywords for IBM i 5250 screen automation via HeadlessSession interface.
"""

from java.util import Properties
from java.io import File
from javax.imageio import ImageIO

from org.hti5250j import Session5250, SessionConfig
from org.hti5250j.interfaces import RequestHandler
from org.hti5250j.session import NullRequestHandler


class HTI5250J:
    """Robot Framework library for 5250 screen automation."""

    ROBOT_LIBRARY_SCOPE = 'SUITE'

    def __init__(self):
        self.session = None
        self.headless_session = None

    def connect_to_ibm_i(self, host, port='23', screen_size='24x80', code_page='37'):
        """
        Connect to IBM i system via 5250 protocol.

        Arguments:
        - host: Hostname or IP address of IBM i system
        - port: Telnet port (default: 23)
        - screen_size: Screen dimensions (default: 24x80, also supports 27x132)
        - code_page: EBCDIC code page (default: 37 = US)

        Example:
        | Connect To IBM i | ibm-i.example.com | port=23 |
        """
        props = Properties()
        props.setProperty('host', host)
        props.setProperty('port', str(port))
        props.setProperty('screen-size', screen_size)
        props.setProperty('code-page', code_page)

        config = SessionConfig('robot-session', 'robot-session')
        self.session = Session5250(props, 'robot-session', 'robot-session', config)
        self.headless_session = self.session.asHeadlessSession()

        # Connect in background thread
        self.session.connect()

        # Wait for connection
        import time
        time.sleep(2)

        if not self.headless_session.isConnected():
            raise Exception("Failed to connect to IBM i system: " + host)

        print("Connected to IBM i: " + host)

    def disconnect_from_ibm_i(self):
        """
        Disconnect from IBM i system.

        Example:
        | Disconnect From IBM i |
        """
        if self.session:
            self.session.disconnect()
            print("Disconnected from IBM i")

    def send_keys(self, keys):
        """
        Send keystrokes to 5250 screen.

        Arguments:
        - keys: Key sequence (e.g., "CALL MYPGM[enter]")

        Special keys:
        - [enter]: Enter key
        - [tab]: Tab key
        - [home]: Home key
        - [f1]-[f24]: Function keys
        - [esc]: Escape key
        - [pf1]-[pf24]: Program Function keys (PA1-PA3, PF1-PF24)

        Example:
        | Send Keys | [home] |
        | Send Keys | MYUSER |
        | Send Keys | [tab] |
        | Send Keys | MYPASS[enter] |
        """
        if not self.headless_session or not self.headless_session.isConnected():
            raise Exception("Not connected to IBM i")

        self.headless_session.sendKeys(keys)

    def wait_for_keyboard_unlock(self, timeout_ms=30000):
        """
        Wait for server to unlock keyboard (response received).

        Arguments:
        - timeout_ms: Maximum wait time in milliseconds (default: 30000 = 30s)

        Raises exception if timeout exceeded.

        Example:
        | Send Keys | MYCOMMAND[enter] |
        | Wait For Keyboard Unlock | timeout_ms=5000 |
        """
        if not self.headless_session or not self.headless_session.isConnected():
            raise Exception("Not connected to IBM i")

        self.headless_session.waitForKeyboardUnlock(int(timeout_ms))

    def wait_for_keyboard_lock_cycle(self, timeout_ms=5000):
        """
        Wait for complete keyboard lock cycle (submission + refresh).

        Waits for:
        1. Keyboard to lock (server accepted submission)
        2. Keyboard to unlock (screen refreshed with response)

        Arguments:
        - timeout_ms: Maximum wait time in milliseconds (default: 5000 = 5s)

        Example:
        | Send Keys | [enter] |
        | Wait For Keyboard Lock Cycle |
        """
        if not self.headless_session or not self.headless_session.isConnected():
            raise Exception("Not connected to IBM i")

        self.headless_session.waitForKeyboardLockCycle(int(timeout_ms))

    def capture_screenshot(self, name='screenshot'):
        """
        Capture 5250 screen as PNG image.

        Arguments:
        - name: Screenshot name (saved as artifacts/{name}.png)

        Returns: Path to PNG file

        Note: Screenshot generation requires NO GUI components (headless).
        Uses HeadlessScreenRenderer for on-demand rendering.

        Example:
        | Capture Screenshot | name=login_screen |
        | Capture Screenshot | name=transaction_complete |
        """
        if not self.headless_session or not self.headless_session.isConnected():
            raise Exception("Not connected to IBM i")

        # Generate screenshot
        image = self.headless_session.captureScreenshot()

        # Save to artifacts directory
        artifact_dir = File('artifacts')
        if not artifact_dir.exists():
            artifact_dir.mkdirs()

        file_path = File(artifact_dir, name + '.png')
        ImageIO.write(image, 'PNG', file_path)

        print("Screenshot saved: " + str(file_path))
        return str(file_path)

    def get_screen_as_text(self):
        """
        Get 5250 screen content as plain text.

        Returns: Screen content (80×24 or 132×27 characters)

        Example:
        | ${content}= | Get Screen As Text |
        | Should Contain | ${content} | WELCOME |
        """
        if not self.headless_session or not self.headless_session.isConnected():
            raise Exception("Not connected to IBM i")

        content = self.headless_session.getScreenAsText()
        return content

    def screen_should_contain(self, text):
        """
        Assert that 5250 screen contains specified text.

        Arguments:
        - text: Text to search for

        Raises exception if text not found.

        Example:
        | Screen Should Contain | MENU COMPLETED |
        | Screen Should Contain | TRANSACTION ACCEPTED |
        """
        content = self.get_screen_as_text()
        if text not in content:
            raise AssertionError("Screen does not contain: " + text)

    def screen_should_not_contain(self, text):
        """
        Assert that 5250 screen does NOT contain specified text.

        Example:
        | Screen Should Not Contain | ERROR |
        """
        content = self.get_screen_as_text()
        if text in content:
            raise AssertionError("Screen contains unexpected text: " + text)

    def set_system_request_handler(self, handler_class_name):
        """
        Set custom RequestHandler for F3 (SYSREQ) key handling.

        Enables workflow-specific logic to intercept and handle system
        request dialogs programmatically.

        Arguments:
        - handler_class_name: Fully qualified Java class name implementing RequestHandler

        Example:
        | Set System Request Handler | com.example.RobotFrameworkRequestHandler |

        Custom Handler Implementation (Java):
        | public class RobotFrameworkRequestHandler implements RequestHandler {
        |     @Override
        |     public String handleSystemRequest(String screenContent) {
        |         // Custom logic: parse screen, return menu option
        |         return "1";  // Select menu option 1
        |     }
        | }
        """
        if not self.session:
            raise Exception("Not connected to IBM i")

        # Load custom handler class
        from java.lang import Class
        handler_class = Class.forName(handler_class_name)
        handler = handler_class.newInstance()

        self.session.setRequestHandler(handler)
        print("Set custom RequestHandler: " + handler_class_name)
```

### Step 5: Create Test Suite

Create `tests/login_test.robot`:

```robot
*** Settings ***
Documentation     IBM i 5250 Login Test
Library           HTI5250J

*** Test Cases ***
Login To IBM i Successfully
    [Documentation]    Test successful login to IBM i system
    [Setup]            Connect To IBM i    ibm-i.example.com    port=23

    # Initial screen should appear
    Wait For Keyboard Unlock
    Capture Screenshot    initial_screen
    Screen Should Contain    SIGN ON

    # Enter credentials
    Send Keys    MYUSER
    Send Keys    [tab]
    Send Keys    MYPASS
    Send Keys    [enter]

    # Wait for response
    Wait For Keyboard Lock Cycle
    Capture Screenshot    login_response

    # Verify successful login
    Screen Should Contain    MAIN MENU
    Screen Should Not Contain    INVALID

    [Teardown]    Disconnect From IBM i

Test Screen Navigation
    [Documentation]    Test navigation between screens
    [Setup]            Connect To IBM i    ibm-i.example.com    port=23

    # Navigate to subsystem
    Send Keys    CALL MYPGM[enter]
    Wait For Keyboard Lock Cycle
    Capture Screenshot    subsystem_screen

    # Send command
    Send Keys    [home]
    Wait For Keyboard Unlock
    Send Keys    COMMAND1[enter]
    Wait For Keyboard Lock Cycle

    # Verify navigation
    Screen Should Contain    SUBSYSTEM READY

    [Teardown]    Disconnect From IBM i
```

### Step 6: Run Tests

```bash
# Run single test file
robot tests/login_test.robot

# Run with output directory
robot --outputdir results tests/login_test.robot

# Run with browser-friendly report
robot --outputdir results --report report.html tests/

# Run in Docker (no X11 needed)
docker run --rm \
  -v $(pwd)/tests:/tests \
  -v $(pwd)/artifacts:/artifacts \
  jython:latest \
  robot /tests/login_test.robot
```

---

## Advanced Usage

### Custom RequestHandler for Workflow Logic

```python
# File: lib/WorkflowRequestHandler.py

from org.hti5250j.interfaces import RequestHandler

class WorkflowRequestHandler(RequestHandler):
    """
    Custom F3 (SYSREQ) handler for Robot Framework workflows.

    Parses screen content and returns appropriate menu selections
    based on workflow state.
    """

    def __init__(self):
        self.workflow_state = 'LOGIN'

    def handleSystemRequest(self, screenContent):
        """
        Intercept SYSREQ and return menu selection.

        Arguments:
        - screenContent: Current 5250 screen as text

        Returns: Menu option number or None (return to menu)
        """
        if 'MAIN MENU' in screenContent:
            if 'INQUIRY' in screenContent:
                return '1'  # Select Inquiry
            elif 'UPDATE' in screenContent:
                return '2'  # Select Update

        # Default: return to menu
        return None
```

Then in Robot Framework:

```robot
*** Test Cases ***
Workflow With Custom SYSREQ Handler
    Connect To IBM i    ibm-i.example.com
    Set System Request Handler    lib.WorkflowRequestHandler

    # F3 key will now be handled by custom logic
    Send Keys    [pf3]
    Wait For Keyboard Lock Cycle
    Screen Should Contain    INQUIRY SUBSYSTEM
```

### Batch Processing (Multiple Sessions)

```python
# File: lib/BatchProcessor.py

from org.hti5250j.interfaces import HeadlessSession
from org.hti5250j.session import DefaultHeadlessSessionFactory
from java.util.concurrent import Executors, TimeUnit

class BatchProcessor:
    """Process multiple IBM i sessions in parallel via virtual threads."""

    @staticmethod
    def process_batch(data_file, threads=100):
        """
        Process batch of records in parallel.

        Each record triggers a new virtual thread session.
        """
        executor = Executors.newVirtualThreadPerTaskExecutor()
        factory = DefaultHeadlessSessionFactory()

        with open(data_file) as f:
            for line in f:
                record = line.strip().split(',')
                executor.submit(lambda r: BatchProcessor.process_record(factory, r), record)

        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.HOURS)

    @staticmethod
    def process_record(factory, record):
        """Process single record in virtual thread."""
        session = factory.createSession('batch-' + record[0], 'batch.properties', {})
        session.connect()

        try:
            # Process record
            session.sendKeys(record[1])
            session.waitForKeyboardUnlock(5000)
            # ... more processing
        finally:
            session.disconnect()
```

---

## Performance Characteristics

| Scenario | Memory | Latency | Throughput |
|----------|--------|---------|-----------|
| Single headless session | 500KB | 100-200ms (key→response) | 1 session/thread |
| 100 concurrent sessions (virtual threads) | 50MB | Same | 1000+ ops/sec |
| 100 concurrent sessions (platform threads) | 1GB+ | Same | 100+ ops/sec |
| Screenshot generation | +50KB | 50-100ms | 20-30 imgs/sec |

**Key Finding (Phase 13):** Virtual threads reduce overhead from 1MB to 1KB per session, enabling 10× higher concurrency.

---

## Troubleshooting

### Connection Timeout
```
ERROR: Failed to connect to IBM i system: ibm-i.example.com
```
- Check hostname/IP and port
- Verify firewall allows port 23 (or configured port)
- Ensure IBM i system is running

### Screen Rendering Issues
```
ERROR: Screenshot generation failed
```
- Verify SessionConfig is properly initialized
- Check code page matches IBM i system (usually 37 for US)
- HeadlessScreenRenderer doesn't require GUI, but needs Screen5250 to be valid

### Virtual Thread Errors
```
ERROR: WorkflowRunner on platform thread
```
- Use Java 21+ with virtual thread support
- Ensure tests run under virtual thread executor
- For Robot Framework, Jython may use platform threads—this is OK but suboptimal

---

## Best Practices

### 1. Use HeadlessSession Interface
```python
# ✓ Good: Use modern headless API
session_5250 = Session5250(props, config_resource, session_name, config)
headless = session_5250.asHeadlessSession()
headless.connect()

# ✗ Old: SessionInterface (still works but less flexible)
session = session_5250  # As SessionInterface
```

### 2. Custom RequestHandlers for Automation
```python
# ✓ Good: Intercept F3 (SYSREQ) for workflow logic
class MyHandler(RequestHandler):
    def handleSystemRequest(self, screenContent):
        # Parse screen, decide response
        return "1"  # Select option

session.setRequestHandler(MyHandler())

# ✗ Avoid: Hardcoded key sequences for every scenario
```

### 3. Screenshots for Debugging
```python
# ✓ Good: Capture on error or assertion failure
try:
    # ... automation ...
except Exception as e:
    capture_screenshot('error_state')
    raise

# ✗ Avoid: Capturing every step (I/O overhead)
```

### 4. Virtual Threads for Batch Processing
```python
# ✓ Good: Use virtual threads (Phase 13)
executor = Executors.newVirtualThreadPerTaskExecutor()
for item in large_dataset:
    executor.submit(process_item, item)

# ✗ Avoid: Platform threads (1MB overhead per thread)
```

---

## Architecture Decision Record

**ADR-015: Headless-First Architecture**

**Status:** Accepted (Phase 15B)

**Context:** HTI5250J positioned as programmatic automation tool (REST APIs, batch processing, Robot Framework) but GUI components were eagerly initialized even in headless mode.

**Decision:** Implement four-layer headless abstraction:
1. HeadlessSession interface (pure data API)
2. RequestHandler abstraction (extensible F3 handling)
3. DefaultHeadlessSession wrapper (composition pattern)
4. Session5250 facade (backward compatible)

**Benefits:**
- No GUI coupling (Docker, CI/CD, headless servers)
- ~500KB per session vs 2MB+ with GUI
- Extensible SYSREQ handling (Robot Framework, custom logic)
- Virtual thread compatible (Phase 13 integration)

**Constraints:**
- Breaks no existing code (SessionInterface still works)
- HeadlessSession is optional (new code use it)
- Screenshot generation still requires Screen5250 data model

**Related:** Phase 13 (Virtual Threads), Phase 15A (Lazy GUI Initialization)

---

## License

HTI5250J is licensed under GPL-2.0-or-later. See COPYING file for details.

This example code is provided AS-IS for educational and automation purposes.
