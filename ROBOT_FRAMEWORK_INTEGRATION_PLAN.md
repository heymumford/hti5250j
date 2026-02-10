# Robot Framework Integration Plan: HTI5250J Headless Refactoring

**Date:** February 9, 2026
**Status:** Assessment & Planning Phase
**Objective:** Enable Robot Framework automation of IBM i 5250 screens via HTI5250J

---

## Current State: Why Robot Framework Cannot Use HTI5250J Today

### Blocker 1: GUI Initialization in Class Load

**Error:**
```
Jython / Robot Framework
    ↓
from org.hti5250j import Session5250
    ↓
java.lang.ClassNotFoundException or
java.awt.HeadlessException
    ↓
Reason: java.awt.Toolkit imported unconditionally
```

**Evidence (Session5250.java:13):**
```java
import java.awt.Toolkit;  // ← Class-level import (static init)
```

**Impact:** Session5250 cannot be loaded in headless environments (Docker, CI/CD, server farms).

---

### Blocker 2: SystemRequestDialog Hardcoded to GUI

**Scenario:** IBM i sends SYSREQ (system request) to client.

```
IBM i                HTI5250J              Robot Framework
                                           (Docker container)
send SYSREQ          ↓
            ├─ Screen5250.setSystemRequest()
            │
            └─ tnvt.receiveDataStream()
                        ↓
                    Session5250.showSystemRequest()
                        ↓
                    new SystemRequestDialog()  ← GUI DIALOG
                        ↓
                    ✗ FAILS: No display system
```

**Current Code (Session5250.java:198-205):**
```java
public String showSystemRequest() {
  final SystemRequestDialog sysreqdlg =
    new SystemRequestDialog(this.guiComponent);  // ← NPE if null

  return sysreqdlg.getResponse();  // ← Blocks waiting for user
}
```

**Problem:** SYSREQ handling is not configurable. Robot Framework cannot intercept.

---

### Blocker 3: Polymorphic Session Creation Not Supported

**Robot Framework Expectation:**
```python
# Create session for automation (no GUI)
session = hti5250j.create_session(mode="headless", handler=NullRequestHandler())

# vs. Create session for interactive use (with GUI)
session = hti5250j.create_session(mode="interactive", handler=GuiRequestHandler())
```

**Current Reality:**
```java
// Only way to create Session5250
Session5250 session = new Session5250(props, cfg, name, sesConfig);
// No factory, no polymorphism, inherits GUI coupling
```

---

## Proposed Solution: HeadlessSession Abstraction

### Architecture (High-Level)

```
Robot Framework (Jython)
    │
    ├─ HeadlessSessionFactory
    │  │ (creates automation-friendly sessions)
    │  │
    │  └─ DefaultHeadlessSession
    │     ├─ (pure data transport)
    │     ├─ (no java.awt imports)
    │     ├─ (injectable RequestHandler)
    │     │
    │     └─ connect(), sendString(), getScreenText()
    │
    ├─ RequestHandler (interface)
    │  │
    │  └─ NullRequestHandler
    │     └─ (default responses for SYSREQ)
    │
    └─ Screen5250
       └─ (unchanged, already headless-safe)
```

---

## Implementation Roadmap

### Phase 1: Create HeadlessSession Interface (Critical)

**File:** `src/org/hti5250j/headless/HeadlessSession.java`

```java
package org.hti5250j.headless;

import java.io.IOException;
import org.hti5250j.framework.tn5250.Screen5250;
import org.hti5250j.framework.tn5250.ScreenOIA;
import org.hti5250j.keyboard.KeyCode;

/**
 * Pure data transport session for headless (non-GUI) operation.
 *
 * This interface enables Robot Framework, Python, and other external
 * tools to interact with IBM i via HTI5250J without requiring:
 * - GUI display system (java.awt)
 * - GUI dialog boxes (SystemRequestDialog)
 * - Visual components (SessionPanel)
 *
 * Designed for automation, not interactive use.
 *
 * @since Phase 15
 */
public interface HeadlessSession {
    /**
     * Connect to IBM i system.
     *
     * @param host IBM i hostname or IP address
     * @param port TN5250E port (typically 23 or 992 for SSL)
     * @throws IOException if connection fails
     */
    void connect(String host, int port) throws IOException;

    /**
     * Disconnect from IBM i system.
     */
    void disconnect();

    /**
     * Send string to IBM i (type text).
     *
     * @param text Text to send (converted to EBCDIC)
     */
    void sendString(String text);

    /**
     * Send AID key to IBM i.
     *
     * @param keyCode Key code (e.g., ENTER, TAB, F5)
     */
    void sendKey(KeyCode keyCode);

    /**
     * Get current screen text as string.
     *
     * @return 24x80 character display
     */
    String getScreenText();

    /**
     * Get operator information area (keyboard status).
     *
     * @return OIA state (locked/unlocked, messages)
     */
    ScreenOIA getOIA();

    /**
     * Get underlying screen object.
     *
     * @return Screen5250 instance
     */
    Screen5250 getScreen();

    /**
     * Wait for keyboard unlock with timeout.
     *
     * @param timeoutMs Maximum wait time
     * @throws InterruptedException if interrupted
     * @throws java.util.concurrent.TimeoutException if timeout exceeded
     */
    void waitForKeyboardUnlock(long timeoutMs)
        throws InterruptedException, java.util.concurrent.TimeoutException;

    /**
     * Set request handler for system requests (SYSREQ).
     *
     * @param handler Custom request handler (e.g., NullRequestHandler)
     */
    void setRequestHandler(RequestHandler handler);

    /**
     * Check if connected to IBM i.
     *
     * @return true if connected
     */
    boolean isConnected();
}
```

---

### Phase 2: Create RequestHandler Interface (High Priority)

**File:** `src/org/hti5250j/headless/RequestHandler.java`

```java
package org.hti5250j.headless;

/**
 * Abstraction for handling system requests (SYSREQ) from IBM i.
 *
 * IBM i can send a system request (e.g., SYSREQ key), which prompts
 * the terminal emulator to handle one of several actions:
 * - 0: End the job
 * - 1: Activate work station
 * - 2: Transfer the job to another device
 * - 3: Display previous menu
 * - 9: Help for SYSREQ
 *
 * This interface allows different implementations:
 * - NullRequestHandler: Return fixed response (no user interaction)
 * - GuiRequestHandler: Show dialog to user (interactive)
 * - RobotRequestHandler: Delegate to Robot Framework (automation)
 *
 * @since Phase 15
 */
public interface RequestHandler {
    /**
     * Handle a system request from IBM i.
     *
     * @param request The SYSREQ message (if any)
     * @return User response code (typically "0" to end job)
     *         Return should be from accepted codes: 0, 1, 2, 3, 9
     */
    String handleSystemRequest(String request);

    /**
     * Check if handler is interactive (requires user input).
     *
     * @return true if handler needs user input (GUI dialog, etc.)
     */
    default boolean isInteractive() {
        return false;
    }
}
```

---

### Phase 3: Implement DefaultHeadlessSession (Core)

**File:** `src/org/hti5250j/headless/DefaultHeadlessSession.java`

```java
package org.hti5250j.headless;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.hti5250j.framework.tn5250.Screen5250;
import org.hti5250j.framework.tn5250.ScreenOIA;
import org.hti5250j.framework.tn5250.tnvt;
import org.hti5250j.keyboard.KeyCode;

/**
 * Default implementation of HeadlessSession for headless operation.
 *
 * This class provides pure data transport with NO GUI coupling:
 * - No java.awt imports
 * - No SystemRequestDialog
 * - No SessionPanel
 * - Injectable RequestHandler for custom SYSREQ behavior
 *
 * @since Phase 15
 */
public class DefaultHeadlessSession implements HeadlessSession {
    private final Screen5250 screen;
    private final tnvt protocol;
    private final Properties config;
    private RequestHandler requestHandler;
    private boolean connected = false;

    /**
     * Construct headless session with configuration.
     *
     * @param config Session properties (host, port, user, etc.)
     */
    public DefaultHeadlessSession(Properties config) {
        this.config = config != null ? config : new Properties();
        this.screen = new Screen5250();  // ✓ No GUI coupling
        this.protocol = new tnvt(this.screen);  // ✓ No GUI coupling
        this.requestHandler = new NullRequestHandler();  // Default
    }

    @Override
    public void connect(String host, int port) throws IOException {
        if (connected) {
            throw new IllegalStateException("Already connected");
        }

        protocol.connect(host, port);
        connected = true;

        // Wait for login screen
        try {
            waitForKeyboardUnlock(30000);  // 30s timeout for login
        } catch (InterruptedException | TimeoutException e) {
            disconnect();
            throw new IOException("Timeout waiting for login screen", e);
        }
    }

    @Override
    public void disconnect() {
        if (protocol != null) {
            protocol.disconnect();
        }
        connected = false;
    }

    @Override
    public void sendString(String text) {
        if (!connected) {
            throw new IllegalStateException("Not connected");
        }
        protocol.sendText(text);  // ✓ Pure I/O
    }

    @Override
    public void sendKey(KeyCode keyCode) {
        if (!connected) {
            throw new IllegalStateException("Not connected");
        }
        protocol.sendKey(keyCode);  // ✓ Pure I/O
    }

    @Override
    public String getScreenText() {
        return screen.getText();  // ✓ Screen5250 is headless-safe
    }

    @Override
    public ScreenOIA getOIA() {
        return screen.getOIA();
    }

    @Override
    public Screen5250 getScreen() {
        return screen;
    }

    @Override
    public void waitForKeyboardUnlock(long timeoutMs)
            throws InterruptedException, TimeoutException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        int pollCount = 0;

        while (true) {
            if (System.currentTimeMillis() > deadline) {
                throw new TimeoutException(
                    String.format("Keyboard unlock timeout (%dms)", timeoutMs)
                );
            }

            if (screen.getOIA().isKeyboardAvailable()) {
                return;  // Keyboard ready
            }

            Thread.sleep(100);  // Poll every 100ms
            pollCount++;
        }
    }

    @Override
    public void setRequestHandler(RequestHandler handler) {
        this.requestHandler = handler != null ? handler : new NullRequestHandler();
    }

    @Override
    public boolean isConnected() {
        return connected && protocol.isConnected();
    }

    /**
     * Called by protocol when SYSREQ is received.
     * (Internal method, called by tnvt)
     */
    protected String onSystemRequest(String request) {
        return requestHandler.handleSystemRequest(request);
    }
}
```

---

### Phase 4: Implement RequestHandler Implementations

**File:** `src/org/hti5250j/headless/NullRequestHandler.java`

```java
package org.hti5250j.headless;

/**
 * Request handler for headless operation.
 *
 * Returns fixed responses without user interaction.
 * Suitable for automated workflows in CI/CD, Docker, etc.
 */
public class NullRequestHandler implements RequestHandler {
    private final String defaultResponse;

    public NullRequestHandler() {
        this.defaultResponse = "0";  // End job (default)
    }

    public NullRequestHandler(String defaultResponse) {
        this.defaultResponse = defaultResponse;
    }

    @Override
    public String handleSystemRequest(String request) {
        // Return fixed response, no user interaction
        return defaultResponse;
    }

    @Override
    public boolean isInteractive() {
        return false;  // Fully automated
    }
}
```

**File:** `src/org/hti5250j/headless/GuiRequestHandler.java`

```java
package org.hti5250j.headless;

import org.hti5250j.gui.SystemRequestDialog;
import org.hti5250j.SessionPanel;

/**
 * Request handler for interactive (GUI) operation.
 *
 * Shows dialog to user and waits for response.
 * Used when SessionPanel is available.
 */
public class GuiRequestHandler implements RequestHandler {
    private final SessionPanel sessionPanel;

    public GuiRequestHandler(SessionPanel sessionPanel) {
        this.sessionPanel = sessionPanel;
    }

    @Override
    public String handleSystemRequest(String request) {
        if (sessionPanel == null) {
            throw new IllegalStateException(
                "GUI handler requires SessionPanel, but none provided"
            );
        }

        final SystemRequestDialog dialog = new SystemRequestDialog(sessionPanel);
        return dialog.getResponse();
    }

    @Override
    public boolean isInteractive() {
        return true;  // Requires user input
    }
}
```

---

### Phase 5: Create SessionFactory Interface (Extensibility)

**File:** `src/org/hti5250j/headless/HeadlessSessionFactory.java`

```java
package org.hti5250j.headless;

import java.util.Properties;

/**
 * Factory for creating HeadlessSession instances.
 *
 * Allows different implementations:
 * - DefaultHeadlessSession: Standard headless operation
 * - RobotHeadlessSession: Custom behavior for Robot Framework
 * - MockHeadlessSession: Testing with fake responses
 *
 * @since Phase 15
 */
public interface HeadlessSessionFactory {
    /**
     * Create a headless session.
     *
     * @param config Session properties
     * @return New HeadlessSession instance
     */
    HeadlessSession createSession(Properties config);
}
```

**File:** `src/org/hti5250j/headless/DefaultHeadlessSessionFactory.java`

```java
package org.hti5250j.headless;

import java.util.Properties;

public class DefaultHeadlessSessionFactory implements HeadlessSessionFactory {
    @Override
    public HeadlessSession createSession(Properties config) {
        return new DefaultHeadlessSession(config);
    }
}
```

---

### Phase 6: Robot Framework Integration Layer (Usage)

**File:** `examples/robot_hti5250j.py` (Example for documentation)

```python
"""
HTI5250J Robot Framework Library

This library provides Robot keywords for automating IBM i screens
via HTI5250J headless session.

Requires Jython (Java/Python bridge):
  pip install jython

Usage:
  *** Settings ***
  Library    examples.robot_hti5250j.HTI5250jLibrary

  *** Test Cases ***
  Test Payment Workflow
    Connect to IBM i    ibmi.example.com    23
    Send Command    CALL PGM(PMTENT)
    Assert Screen Contains    Payment Entry
    Fill Field    ACCOUNT    ACC001
    Fill Field    AMOUNT    1000.00
    Submit Form
    Assert Screen Contains    Payment Accepted
    Disconnect
"""

from java.lang import Properties
from org.hti5250j.headless import DefaultHeadlessSessionFactory, NullRequestHandler


class HTI5250jLibrary:
    """Robot Framework library for HTI5250J automation."""

    ROBOT_LIBRARY_SCOPE = 'SUITE'

    def __init__(self):
        self.session = None
        self.factory = DefaultHeadlessSessionFactory()

    def connect_to_ibm_i(self, host, port=23):
        """
        Connect to IBM i system.

        :param host: IBM i hostname or IP address
        :param port: TN5250E port (default: 23)
        """
        config = Properties()
        config.setProperty("host", host)
        config.setProperty("port", str(port))

        self.session = self.factory.createSession(config)

        # Use null handler (no interactive dialogs)
        from org.hti5250j.headless import NullRequestHandler
        self.session.setRequestHandler(NullRequestHandler())

        self.session.connect(host, int(port))

    def send_command(self, command):
        """
        Send command to IBM i.

        :param command: Command text (e.g., "WRKSYSVAL")
        """
        if not self.session:
            raise RuntimeError("Not connected")

        self.session.sendString(command)
        self.session.sendKey(KeyCode.ENTER)
        self.session.waitForKeyboardUnlock(5000)

    def get_screen_text(self):
        """
        Get current screen as text.

        :return: Screen text (24×80 characters)
        """
        if not self.session:
            raise RuntimeError("Not connected")

        return self.session.getScreenText()

    def assert_screen_contains(self, expected):
        """
        Assert screen contains text.

        :param expected: Text to find
        """
        text = self.get_screen_text()
        if expected not in text:
            raise AssertionError(
                f"Expected '{expected}' not found in screen:\n{text}"
            )

    def fill_field(self, field_name, value):
        """
        Fill form field with value.

        :param field_name: Field name
        :param value: Value to enter
        """
        if not self.session:
            raise RuntimeError("Not connected")

        # Navigate to field (simplified)
        self.session.sendString(value)
        self.session.sendKey(KeyCode.TAB)

    def submit_form(self):
        """Submit form (press ENTER)."""
        if not self.session:
            raise RuntimeError("Not connected")

        self.session.sendKey(KeyCode.ENTER)
        self.session.waitForKeyboardUnlock(5000)

    def disconnect(self):
        """Disconnect from IBM i."""
        if self.session:
            self.session.disconnect()
            self.session = None
```

---

## Implementation Timeline

| Phase | Task | Files | Effort | Risk | Blocking |
|-------|------|-------|--------|------|----------|
| **1** | HeadlessSession interface | 1 | 1-2h | LOW | Robot Framework blocked |
| **2** | RequestHandler interface | 1 | 0.5h | LOW | SYSREQ handling |
| **3** | DefaultHeadlessSession | 1 | 2-3h | MEDIUM | Core functionality |
| **4** | RequestHandler impls | 2 | 1-2h | LOW | SYSREQ variants |
| **5** | HeadlessSessionFactory | 2 | 0.5h | LOW | Extensibility |
| **6** | Refactor Session5250 | 1 | 1-2h | MEDIUM | Backward compatibility |
| **7** | Update WorkflowRunner | 1 | 0.5h | LOW | Workflow support |
| **8** | Robot Framework example | 1 | 1-2h | LOW | Documentation |
| **9** | Unit tests (headless) | 3 | 2-3h | LOW | Quality |
| **10** | Integration tests | 2 | 2-3h | MEDIUM | Verification |

**Total Estimated Effort:** 12-19 hours

---

## Success Criteria

### Acceptance Test 1: Headless Session Creation
```
Given: No java.awt.Toolkit imported
When:  Create DefaultHeadlessSession in Docker
Then:  Session created successfully without display errors
```

### Acceptance Test 2: SYSREQ Handling Without GUI
```
Given: DefaultHeadlessSession with NullRequestHandler
When:  IBM i sends SYSREQ
Then:  Handler returns "0" without opening dialog
```

### Acceptance Test 3: Robot Framework Integration
```
Given: Jython bridge to HTI5250J
When:  Robot test creates HeadlessSession
Then:  Automation runs in Docker/CI without GUI
```

### Acceptance Test 4: Backward Compatibility
```
Given: Existing code using Session5250
When:  Session5250 refactored as adapter
Then:  All existing tests pass unchanged
```

---

## Risk Mitigation

| Risk | Probability | Mitigation |
|------|-------------|-----------|
| Backward compatibility break | MEDIUM | Maintain Session5250 as facade, add integration tests |
| tnvt coupling issues | LOW | tnvt already clean, no changes needed |
| RequestHandler not extensible | LOW | Design as interface, add factory pattern |
| Jython/Robot version conflicts | MEDIUM | Document tested versions, add CI pipeline |

---

## Next Steps

1. **Review architectural assessment** (ARCHITECTURE_ASSESSMENT.md)
2. **Validate HeadlessSession design** with team
3. **Create test plan** for headless operation
4. **Implement Phase 1-3** (core abstractions)
5. **Integration test with Robot Framework** (Jython)
6. **Document migration guide** for existing code

---

**Document Version:** 1.0
**Date:** February 9, 2026
**Status:** Ready for implementation planning
