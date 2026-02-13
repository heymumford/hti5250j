# HTI5250J -- Robot Framework Integration Guide

## Overview

HTI5250J provides headless abstractions enabling seamless Robot Framework integration for IBM i (AS/400) test automation.

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
│ HeadlessSession                 │
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

No X11 display system required. ~500KB per session (vs 2MB+ with GUI).

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
        """Connect to IBM i system via 5250 protocol.

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

        self.session.connect()

        import time
        time.sleep(2)

        if not self.headless_session.isConnected():
            raise Exception("Failed to connect to IBM i system: " + host)

    def disconnect_from_ibm_i(self):
        """Disconnect from IBM i system."""
        if self.session:
            self.session.disconnect()

    def send_keys(self, keys):
        """Send keystrokes to 5250 screen.

        Special keys: [enter], [tab], [home], [f1]-[f24], [esc], [pf1]-[pf24]

        | Send Keys | MYUSER[tab]MYPASS[enter] |
        """
        if not self.headless_session or not self.headless_session.isConnected():
            raise Exception("Not connected to IBM i")
        self.headless_session.sendKeys(keys)

    def wait_for_keyboard_unlock(self, timeout_ms=30000):
        """Wait for server to unlock keyboard. Raises on timeout."""
        if not self.headless_session or not self.headless_session.isConnected():
            raise Exception("Not connected to IBM i")
        self.headless_session.waitForKeyboardUnlock(int(timeout_ms))

    def wait_for_keyboard_lock_cycle(self, timeout_ms=5000):
        """Wait for complete lock/unlock cycle (submission + screen refresh)."""
        if not self.headless_session or not self.headless_session.isConnected():
            raise Exception("Not connected to IBM i")
        self.headless_session.waitForKeyboardLockCycle(int(timeout_ms))

    def capture_screenshot(self, name='screenshot'):
        """Capture 5250 screen as PNG to artifacts/{name}.png. Returns file path."""
        if not self.headless_session or not self.headless_session.isConnected():
            raise Exception("Not connected to IBM i")

        image = self.headless_session.captureScreenshot()

        artifact_dir = File('artifacts')
        if not artifact_dir.exists():
            artifact_dir.mkdirs()

        file_path = File(artifact_dir, name + '.png')
        ImageIO.write(image, 'PNG', file_path)
        return str(file_path)

    def get_screen_as_text(self):
        """Get 5250 screen content as plain text (80x24 or 132x27)."""
        if not self.headless_session or not self.headless_session.isConnected():
            raise Exception("Not connected to IBM i")
        return self.headless_session.getScreenAsText()

    def screen_should_contain(self, text):
        """Assert that 5250 screen contains specified text."""
        content = self.get_screen_as_text()
        if text not in content:
            raise AssertionError("Screen does not contain: " + text)

    def screen_should_not_contain(self, text):
        """Assert that 5250 screen does NOT contain specified text."""
        content = self.get_screen_as_text()
        if text in content:
            raise AssertionError("Screen contains unexpected text: " + text)

    def set_system_request_handler(self, handler_class_name):
        """Set custom RequestHandler for F3 (SYSREQ) key handling.

        | Set System Request Handler | com.example.MyHandler |
        """
        if not self.session:
            raise Exception("Not connected to IBM i")

        from java.lang import Class
        handler_class = Class.forName(handler_class_name)
        handler = handler_class.newInstance()
        self.session.setRequestHandler(handler)
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

For batch processing with multiple concurrent sessions, see [Virtual Thread Integration](./VIRTUAL_THREADS.md).

---

## Performance Characteristics

| Scenario | Memory | Latency | Throughput |
|----------|--------|---------|-----------|
| Single headless session | 500KB | 100-200ms (key→response) | 1 session/thread |
| 100 concurrent sessions (virtual threads) | 50MB | Same | 1000+ ops/sec |
| 100 concurrent sessions (platform threads) | 1GB+ | Same | 100+ ops/sec |
| Screenshot generation | +50KB | 50-100ms | 20-30 imgs/sec |

Virtual threads reduce overhead from 1MB to 1KB per session, enabling 10x higher concurrency.

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

1. **Use HeadlessSession interface** for all new automation code. SessionInterface still works but is less flexible.
2. **Use custom RequestHandlers** to intercept F3 (SYSREQ) for workflow logic instead of hardcoded key sequences.
3. **Capture screenshots on error** for debugging, not on every step (I/O overhead).
4. **Use virtual threads** for batch processing (1KB overhead vs 1MB for platform threads).

---

For the full architecture decision, see [ADR-015: Headless-First Architecture](./ADR-015-Headless-Abstractions.md).

## License

HTI5250J is licensed under GPL-2.0-or-later. See COPYING file for details.
