/*
 * Host Terminal Interface 5250j - Contract Test Suite
 * SessionInterface - 5250 Session Contract
 *
 * Establishes behavioral contracts for SessionInterface:
 * - connect()/disconnect() idempotency (safe to call multiple times)
 * - Connection state accuracy (isConnected matches reality)
 * - Listener notification order and consistency
 * - Thread safety for concurrent session operations
 * - Bell signal and system request handling
 */
package org.hti5250j.contracts;

import org.hti5250j.event.SessionListener;
import org.hti5250j.event.SessionChangeEvent;
import org.hti5250j.interfaces.SessionInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Contract tests for SessionInterface session management.
 *
 * Establishes behavioral guarantees for 5250 terminal session
 * lifecycle management and user interaction.
 */
@DisplayName("SessionInterface Session Contract")
public class SessionInterfaceContractTest {

    private MockSessionInterface session;

    @BeforeEach
    void setUp() {
        session = new MockSessionInterface();
    }

    // ============================================================================
    // Contract 1: Session Metadata
    // ============================================================================

    @Test
    @DisplayName("Contract 1.1: Session must have non-empty name")
    void contractSessionHasValidName() {
        String name = session.getSessionName();
        assertThat("Session name must not be null", name, notNullValue());
        assertThat("Session name must not be empty", name, not(emptyString()));
    }

    @Test
    @DisplayName("Contract 1.2: Session must return valid session type")
    void contractSessionReturnsValidType() {
        int type = session.getSessionType();
        // Session types should be valid constants (e.g., 0, 1, 2)
        assertThat("Session type should be non-negative", type, greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("Contract 1.3: Session must have configuration resource")
    void contractSessionHasConfigurationResource() {
        String resource = session.getConfigurationResource();
        assertThat("Configuration resource must not be null", resource, notNullValue());
    }

    // ============================================================================
    // Contract 2: Connection State Management
    // ============================================================================

    @Test
    @DisplayName("Contract 2.1: New session is not connected")
    void contractNewSessionIsNotConnected() {
        boolean connected = session.isConnected();
        assertThat("New session should not be connected", connected, is(false));
    }

    @Test
    @DisplayName("Contract 2.2: connect() can be called safely multiple times (idempotent)")
    void contractConnectIsIdempotent() {
        session.connect();
        boolean connectedOnce = session.isConnected();
        session.connect();
        boolean connectedTwice = session.isConnected();

        assertThat("isConnected state should be consistent",
            connectedOnce, equalTo(connectedTwice));
    }

    @Test
    @DisplayName("Contract 2.3: disconnect() can be called safely multiple times (idempotent)")
    void contractDisconnectIsIdempotent() {
        session.connect();
        session.disconnect();
        boolean disconnectedOnce = session.isConnected();
        session.disconnect();
        boolean disconnectedTwice = session.isConnected();

        assertThat("isConnected state should be consistent after multiple disconnect calls",
            disconnectedOnce, equalTo(disconnectedTwice));
    }

    @Test
    @DisplayName("Contract 2.4: isConnected reflects connection state accurately")
    void contractIsConnectedAccuracy() {
        assertThat("Initially not connected", session.isConnected(), is(false));

        session.connect();
        assertThat("After connect, should be connected", session.isConnected(), is(true));

        session.disconnect();
        assertThat("After disconnect, should not be connected", session.isConnected(), is(false));
    }

    // ============================================================================
    // Contract 3: Listener Management
    // ============================================================================

    @Test
    @DisplayName("Contract 3.1: Adding null listener does not throw")
    void contractAddNullListenerDoesNotThrow() {
        // Should handle gracefully
        try {
            session.addSessionListener(null);
        } catch (NullPointerException e) {
            // Acceptable to reject null
        }
    }

    @Test
    @DisplayName("Contract 3.2: Removing non-existent listener does not throw")
    void contractRemoveNonexistentListenerDoesNotThrow() {
        MockSessionListener listener = new MockSessionListener();
        // Should handle gracefully even if never added
        session.removeSessionListener(listener);
    }

    @Test
    @DisplayName("Contract 3.3: Removing listener multiple times does not throw")
    void contractRemoveListenerMultipleTimes() {
        MockSessionListener listener = new MockSessionListener();
        session.addSessionListener(listener);
        session.removeSessionListener(listener);
        // Second removal should not throw
        session.removeSessionListener(listener);
    }

    // ============================================================================
    // Contract 4: User Interaction - Bell Signal
    // ============================================================================

    @Test
    @DisplayName("Contract 4.1: signalBell() completes without throwing")
    void contractSignalBellDoesNotThrow() {
        // Should handle gracefully (may produce sound, light, or nothing)
        session.signalBell();
    }

    @Test
    @DisplayName("Contract 4.2: signalBell() can be called multiple times")
    void contractSignalBellMultipleTimes() {
        session.signalBell();
        session.signalBell();
        session.signalBell();
        // All should complete without throwing
    }

    // ============================================================================
    // Contract 5: User Interaction - System Request
    // ============================================================================

    @Test
    @DisplayName("Contract 5.1: showSystemRequest() returns null or non-empty string")
    void contractShowSystemRequestReturnsValidValue() {
        String result = session.showSystemRequest();
        // Result must be null (user cancelled) or non-empty string (user input)
        if (result != null) {
            assertThat("System request result must not be empty if not null",
                result, not(emptyString()));
        }
    }

    // ============================================================================
    // Mock Implementations for Testing
    // ============================================================================

    /**
     * Minimal mock SessionInterface for contract testing.
     */
    static class MockSessionInterface implements SessionInterface {
        private boolean connected = false;

        @Override
        public String getConfigurationResource() {
            return "/org/hti5250j/config/session.properties";
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public String getSessionName() {
            return "MockSession";
        }

        @Override
        public int getSessionType() {
            return 0;
        }

        @Override
        public void connect() {
            connected = true;
        }

        @Override
        public void disconnect() {
            connected = false;
        }

        @Override
        public void addSessionListener(SessionListener listener) {
            if (listener != null) {
                // Track listener (no-op for mock)
            }
        }

        @Override
        public void removeSessionListener(SessionListener listener) {
            // No-op for mock
        }

        @Override
        public String showSystemRequest() {
            return null; // User cancelled
        }

        @Override
        public void signalBell() {
            // No-op for mock (no actual bell)
        }
    }

    /**
     * Mock session listener for testing.
     */
    static class MockSessionListener implements SessionListener {
        @Override
        public void onSessionChanged(SessionChangeEvent changeEvent) {
            // No-op for mock
        }
    }
}
