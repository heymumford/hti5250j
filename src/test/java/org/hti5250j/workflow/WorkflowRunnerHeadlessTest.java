/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.hti5250j.Session5250;
import org.hti5250j.SessionConfig;
import org.hti5250j.interfaces.RequestHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WorkflowRunner headless integration (Phase 15B).
 * <p>
 * Verifies:
 * 1. setRequestHandler() injects handler into Session5250
 * 2. getSession5250() provides access to underlying session
 * 3. RequestHandler flows through WorkflowRunner to Session5250
 */
@DisplayName("WorkflowRunner Headless Integration Tests")
class WorkflowRunnerHeadlessTest {

    private Session5250 session;
    private WorkflowRunner runner;

    @BeforeEach
    void setUp() {
        Properties props = new Properties();
        props.setProperty("host", "localhost");
        props.setProperty("port", "23");

        SessionConfig config = new SessionConfig("workflow-test", "workflow-test");
        session = new Session5250(props, "workflow-test", "workflow-test", config);

        // Mock WorkflowRunner dependencies (or use nulls for basic test)
        runner = new WorkflowRunner(session, null, null);
    }

    @Test
    @DisplayName("setRequestHandler requires non-null handler")
    void testSetRequestHandlerRequiresNonNull() {
        assertThrows(NullPointerException.class, () ->
            runner.setRequestHandler(null)
        );
    }

    @Test
    @DisplayName("setRequestHandler accepts valid RequestHandler")
    void testSetRequestHandlerValid() {
        RequestHandler handler = new TestRequestHandler("response");
        // Should not throw
        runner.setRequestHandler(handler);
    }

    @Test
    @DisplayName("setRequestHandler updates Session5250 handler")
    void testSetRequestHandlerUpdatesSession() {
        RequestHandler handler = new TestRequestHandler("workflow-response");
        runner.setRequestHandler(handler);

        // Verify handler is set on session
        String response = session.showSystemRequest();
        assertEquals("workflow-response", response);
    }

    @Test
    @DisplayName("getSession5250 returns underlying session when available")
    void testGetSession5250ReturnsSession() {
        Session5250 retrieved = runner.getSession5250();
        assertNotNull(retrieved);
        assertEquals(session.getSessionName(), retrieved.getSessionName());
    }

    @Test
    @DisplayName("getSession5250 returns same session passed to constructor")
    void testGetSession5250ReturnsSameSession() {
        Session5250 retrieved = runner.getSession5250();
        assertEquals("workflow-test", retrieved.getSessionName());
    }

    @Test
    @DisplayName("Custom RequestHandler from WorkflowRunner flows to Session5250")
    void testCustomHandlerFlowsToSession() {
        // Workflow-specific handler
        RequestHandler workflowHandler = new WorkflowRequestHandler();
        runner.setRequestHandler(workflowHandler);

        // When Session5250 receives SYSREQ (F3), it uses our handler
        String response = session.showSystemRequest();
        assertNotNull(response);  // Workflow handler can return something
    }

    @Test
    @DisplayName("Multiple setRequestHandler calls succeed")
    void testMultipleSetRequestHandlerCalls() {
        RequestHandler handler1 = new TestRequestHandler("handler1");
        runner.setRequestHandler(handler1);

        RequestHandler handler2 = new TestRequestHandler("handler2");
        runner.setRequestHandler(handler2);

        // Latest handler should be active
        String response = session.showSystemRequest();
        assertEquals("handler2", response);
    }

    /**
     * Test RequestHandler implementation.
     */
    private static class TestRequestHandler implements RequestHandler {
        private final String response;

        TestRequestHandler(String response) {
            this.response = response;
        }

        @Override
        public String handleSystemRequest(String screenContent) {
            return response;
        }
    }

    /**
     * Example workflow-specific RequestHandler.
     */
    private static class WorkflowRequestHandler implements RequestHandler {
        @Override
        public String handleSystemRequest(String screenContent) {
            // Example: Parse screen and decide workflow action
            if (screenContent.contains("CONFIRM")) {
                return "1";  // Auto-confirm in workflow
            }
            return null;  // Return to menu
        }
    }
}
