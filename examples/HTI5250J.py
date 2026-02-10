#!/usr/bin/env jython
# -*- coding: utf-8 -*-

"""
HTI5250J Robot Framework Keyword Library

Provides keywords for IBM i 5250 screen automation via HeadlessSession interface.
Designed for Robot Framework 5.x+ and Jython 2.7+ or 3.x.

Usage in Robot Framework:
    *** Settings ***
    Library    HTI5250J

Example Test:
    *** Test Cases ***
    Login Test
        Connect To IBM i    ibm-i.example.com
        Send Keys          MYUSER[tab]MYPASS[enter]
        Wait For Keyboard Unlock
        Screen Should Contain    MENU
        Disconnect From IBM i

Phase 15B: Headless session automation without GUI dependencies.
"""

from java.util import Properties
from java.io import File
from javax.imageio import ImageIO

from org.hti5250j import Session5250, SessionConfig
from org.hti5250j.interfaces import RequestHandler


class HTI5250J:
    """Robot Framework library for 5250 screen automation (Phase 15B)."""

    ROBOT_LIBRARY_SCOPE = 'SUITE'
    ROBOT_LIBRARY_VERSION = '1.0.0'

    def __init__(self):
        """Initialize library (per suite)."""
        self.session = None
        self.headless_session = None
        self._timeout_ms = 30000

    def connect_to_ibm_i(self, host, port='23', screen_size='24x80', code_page='37'):
        """
        Connect to IBM i system via 5250 protocol.

        This method creates a headless session without GUI components,
        suitable for CI/CD pipelines, Docker containers, and batch automation.

        Arguments:
        | host | Hostname or IP address of IBM i system |
        | port | Telnet port (default: 23) |
        | screen_size | Screen dimensions: 24x80 or 27x132 (default: 24x80) |
        | code_page | EBCDIC code page: 37 (US), 273 (German), etc. (default: 37) |

        Examples:
        | Connect To IBM i | ibm-i.example.com |
        | Connect To IBM i | 192.168.1.100 | port=23 |
        | Connect To IBM i | ibm-i.example.com | screen_size=27x132 |

        Note: Connection happens in background thread. Wait for keyboard unlock
        before sending keys.
        """
        try:
            # Create session properties
            props = Properties()
            props.setProperty('host', host)
            props.setProperty('port', str(port))
            props.setProperty('screen-size', screen_size)
            props.setProperty('code-page', code_page)

            # Create session configuration
            config = SessionConfig('robot-session', 'robot-session')

            # Create Session5250 (headless by default)
            self.session = Session5250(props, 'robot-session', 'robot-session', config)
            self.headless_session = self.session.asHeadlessSession()

            # Initiate connection
            self.session.connect()

            # Wait for connection to establish
            import time
            time.sleep(2)

            # Verify connection
            if not self.headless_session.isConnected():
                raise Exception("Failed to connect to IBM i system: " + host)

            print("✓ Connected to IBM i: " + host)

        except Exception as e:
            raise Exception("Connection failed: " + str(e))

    def disconnect_from_ibm_i(self):
        """
        Disconnect from IBM i system.

        Closes the 5250 session and releases resources.

        Example:
        | Disconnect From IBM i |

        Note: Safe to call even if not connected. Errors are logged but
        do not raise exceptions.
        """
        try:
            if self.session:
                self.session.disconnect()
                self.session = None
                self.headless_session = None
                print("✓ Disconnected from IBM i")
        except Exception as e:
            print("Warning: Disconnect error: " + str(e))

    def send_keys(self, keys, wait=False, timeout_ms=None):
        """
        Send keystrokes to 5250 screen.

        Arguments:
        | keys | Key sequence to send |
        | wait | If True, wait for keyboard unlock after sending (default: False) |
        | timeout_ms | Timeout in milliseconds (default: 30000) |

        Special Keys:
        | [enter] | Enter key |
        | [tab] | Tab key (field navigation) |
        | [home] | Home key (move to start) |
        | [f1]-[f24] | Function keys |
        | [esc] | Escape key |
        | [pf1]-[pf3] | Program Function keys (PA1-PA3) |
        | [pa4]-[pa6] | Additional program keys |

        Examples:
        | Send Keys | MYCOMMAND |
        | Send Keys | MYUSER | wait=True |
        | Send Keys | MYPASS[enter] |
        | Send Keys | [home] |
        | Send Keys | [f3] |

        Note: Keys are sent immediately. Use "Wait For Keyboard *" keywords
        to synchronize with server responses.
        """
        self._verify_connected()

        try:
            self.headless_session.sendKeys(keys)
            print("Sent keys: " + keys)

            if wait:
                actual_timeout = int(timeout_ms) if timeout_ms else self._timeout_ms
                self.wait_for_keyboard_unlock(actual_timeout)

        except Exception as e:
            raise Exception("Failed to send keys: " + str(e))

    def wait_for_keyboard_unlock(self, timeout_ms=None):
        """
        Wait for server to unlock keyboard (response received).

        Blocks until the 5250 server responds to the last command by
        unlocking the keyboard, indicating the screen is ready.

        Arguments:
        | timeout_ms | Maximum wait time in milliseconds (default: 30000 = 30s) |

        Raises exception if timeout exceeded.

        Examples:
        | Send Keys | MYCOMMAND[enter] |
        | Wait For Keyboard Unlock |
        | Wait For Keyboard Unlock | timeout_ms=5000 |

        Related: "Wait For Keyboard Lock Cycle" (for full submit cycle)
        """
        self._verify_connected()

        try:
            actual_timeout = int(timeout_ms) if timeout_ms else self._timeout_ms
            self.headless_session.waitForKeyboardUnlock(actual_timeout)
            print("Keyboard unlocked (response received)")
        except Exception as e:
            raise Exception("Timeout waiting for keyboard unlock: " + str(e))

    def wait_for_keyboard_lock_cycle(self, timeout_ms=None):
        """
        Wait for complete keyboard lock cycle (submit + refresh).

        Waits for:
        1. Keyboard to lock (server accepted submission)
        2. Keyboard to unlock (screen refreshed with response)

        This is the complete round-trip: client sends command, server
        processes and responds, screen updates.

        Arguments:
        | timeout_ms | Maximum wait time in milliseconds (default: 5000 = 5s) |

        Examples:
        | Send Keys | [enter] |
        | Wait For Keyboard Lock Cycle |
        | Send Keys | [f12] |
        | Wait For Keyboard Lock Cycle | timeout_ms=10000 |

        Note: Use this after submitting data or commands. Use
        "Wait For Keyboard Unlock" when just reading the screen.

        Related: "Wait For Keyboard Unlock" (just wait for response)
        """
        self._verify_connected()

        try:
            actual_timeout = int(timeout_ms) if timeout_ms else 5000
            self.headless_session.waitForKeyboardLockCycle(actual_timeout)
            print("Keyboard cycle complete (screen refreshed)")
        except Exception as e:
            raise Exception("Timeout waiting for keyboard lock cycle: " + str(e))

    def capture_screenshot(self, name='screenshot'):
        """
        Capture 5250 screen as PNG image.

        Generates on-demand PNG screenshot without requiring GUI components.
        Useful for debugging, documentation, and visual verification.

        Arguments:
        | name | Screenshot name (saved as artifacts/{name}.png) |

        Returns: Path to PNG file

        Examples:
        | Capture Screenshot |
        | Capture Screenshot | login_screen |
        | Capture Screenshot | transaction_complete |

        Note: Screenshots are saved to artifacts/ directory (created if needed).
        No GUI framework required (headless mode).
        """
        self._verify_connected()

        try:
            # Generate screenshot using HeadlessScreenRenderer
            image = self.headless_session.captureScreenshot()

            # Create artifacts directory if needed
            artifact_dir = File('artifacts')
            if not artifact_dir.exists():
                artifact_dir.mkdirs()

            # Save PNG file
            file_path = File(artifact_dir, name + '.png')
            ImageIO.write(image, 'PNG', file_path)

            result_path = str(file_path)
            print("Screenshot saved: " + result_path)
            return result_path

        except Exception as e:
            raise Exception("Failed to capture screenshot: " + str(e))

    def get_screen_as_text(self):
        """
        Get 5250 screen content as plain text.

        Returns: Screen content as string (80×24 or 132×27 characters)

        Useful for text-based assertions and debugging.

        Examples:
        | ${content}= | Get Screen As Text |
        | Log | ${content} |
        | Should Contain | ${content} | WELCOME |

        See also: "Screen Should Contain", "Screen Should Not Contain"
        """
        self._verify_connected()

        try:
            content = self.headless_session.getScreenAsText()
            return content
        except Exception as e:
            raise Exception("Failed to get screen content: " + str(e))

    def screen_should_contain(self, text):
        """
        Assert that 5250 screen contains specified text.

        Arguments:
        | text | Text to search for |

        Raises AssertionError if text not found.

        Examples:
        | Screen Should Contain | MAIN MENU |
        | Screen Should Contain | TRANSACTION ACCEPTED |
        | Screen Should Contain | ERROR |

        Case Sensitive: Search is case-sensitive (EBCDIC preserved).

        Related: "Screen Should Not Contain", "Get Screen As Text"
        """
        try:
            content = self.get_screen_as_text()
            if text not in content:
                raise AssertionError("Screen does not contain: " + text)
            print("✓ Screen contains: " + text)
        except AssertionError:
            raise

    def screen_should_not_contain(self, text):
        """
        Assert that 5250 screen does NOT contain specified text.

        Arguments:
        | text | Text that should NOT be present |

        Raises AssertionError if text is found.

        Examples:
        | Screen Should Not Contain | ERROR |
        | Screen Should Not Contain | INVALID CREDENTIALS |

        Related: "Screen Should Contain", "Get Screen As Text"
        """
        try:
            content = self.get_screen_as_text()
            if text in content:
                raise AssertionError("Screen contains unexpected text: " + text)
            print("✓ Screen does not contain: " + text)
        except AssertionError:
            raise

    def set_system_request_handler(self, handler_class_name):
        """
        Set custom RequestHandler for F3 (SYSREQ) key handling.

        Enables workflow-specific logic to intercept and handle system
        request dialogs (F3 key) programmatically instead of GUI dialogs.

        Arguments:
        | handler_class_name | Fully qualified Java class implementing RequestHandler |

        Raises exception if class cannot be loaded or instantiated.

        Examples:
        | Set System Request Handler | com.example.RobotFrameworkRequestHandler |
        | Set System Request Handler | org.hti5250j.session.NullRequestHandler |

        Custom Handler (Java):
        ```java
        public class RobotFrameworkRequestHandler implements RequestHandler {
            @Override
            public String handleSystemRequest(String screenContent) {
                // Parse screen content
                if (screenContent.contains("CONFIRM DELETION")) {
                    return "1";  // Yes
                } else {
                    return null;  // Return to menu
                }
            }
        }
        ```

        Default Handlers:
        | NullRequestHandler | Returns to menu (default for headless) |
        | GuiRequestHandler | Interactive dialog (GUI mode only) |

        Related: Phase 15B RequestHandler abstraction
        """
        self._verify_connected()

        try:
            from java.lang import Class
            handler_class = Class.forName(handler_class_name)
            handler = handler_class.newInstance()
            self.session.setRequestHandler(handler)
            print("✓ Set RequestHandler: " + handler_class_name)
        except Exception as e:
            raise Exception("Failed to set RequestHandler: " + str(e))

    def set_timeout(self, timeout_ms):
        """
        Set default timeout for wait operations.

        Arguments:
        | timeout_ms | Timeout in milliseconds |

        This value is used as default for "Wait For Keyboard *" keywords
        when timeout_ms is not explicitly specified.

        Example:
        | Set Timeout | 60000 |
        """
        self._timeout_ms = int(timeout_ms)
        print("Timeout set to: " + str(timeout_ms) + "ms")

    # ===== Private Helper Methods =====

    def _verify_connected(self):
        """Verify that session is connected. Raise exception if not."""
        if not self.session or not self.headless_session:
            raise Exception("Not connected to IBM i. Use 'Connect To IBM i' first.")

        if not self.headless_session.isConnected():
            raise Exception("Session is not connected to IBM i system.")


# Export library
__all__ = ['HTI5250J']
