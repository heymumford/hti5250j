/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.session;

import org.hti5250j.interfaces.RequestHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract tests for RequestHandler abstraction (Phase 15B).
 * <p>
 * Verifies:
 * 1. NullRequestHandler returns null/fixed response
 * 2. RequestHandler contract is simple and extensible
 * 3. Custom handlers can implement their own logic
 */
@DisplayName("RequestHandler Abstraction Tests")
class RequestHandlerTest {

    @Test
    @DisplayName("NullRequestHandler returns null by default")
    void testNullRequestHandlerDefault() {
        NullRequestHandler handler = new NullRequestHandler();
        String response = handler.handleSystemRequest("SOME SCREEN CONTENT");
        assertNull(response);
    }

    @Test
    @DisplayName("NullRequestHandler returns configured response")
    void testNullRequestHandlerConfigured() {
        NullRequestHandler handler = new NullRequestHandler("5");
        String response = handler.handleSystemRequest("SOME SCREEN CONTENT");
        assertEquals("5", response);
    }

    @Test
    @DisplayName("NullRequestHandler handles empty screen content")
    void testNullRequestHandlerEmptyContent() {
        NullRequestHandler handler = new NullRequestHandler();
        String response = handler.handleSystemRequest("");
        assertNull(response);
    }

    @Test
    @DisplayName("NullRequestHandler constructor with null response creates null handler")
    void testNullRequestHandlerNullConstructor() {
        NullRequestHandler handler = new NullRequestHandler(null);
        String response = handler.handleSystemRequest("CONTENT");
        assertNull(response);
    }

    @Test
    @DisplayName("NullRequestHandler is suitable for headless environments")
    void testNullRequestHandlerHeadlessSafe() {
        // Should not require any GUI or display system
        NullRequestHandler handler = new NullRequestHandler("auto-select");

        // Can handle any screen content without throwing
        String[] testScreens = {
            "",
            "MAIN MENU",
            "SYSTEM REQUEST",
            "ERROR MESSAGE",
            "VERY LONG SCREEN CONTENT ".repeat(100)
        };

        for (String screen : testScreens) {
            assertDoesNotThrow(() -> handler.handleSystemRequest(screen),
                              "Should handle screen: " + screen.substring(0, Math.min(20, screen.length())));
        }
    }

    @Test
    @DisplayName("CustomRequestHandler can parse screen and respond intelligently")
    void testCustomRequestHandlerParsingLogic() {
        // Example: Custom handler that analyzes screen content
        CustomTestHandler handler = new CustomTestHandler();

        String confirmScreen = "PRESS 1 TO CONFIRM OR 2 TO CANCEL";
        String response = handler.handleSystemRequest(confirmScreen);
        assertEquals("1", response);

        String errorScreen = "ERROR: INVALID INPUT";
        response = handler.handleSystemRequest(errorScreen);
        assertEquals("3", response);  // Error handling
    }

    @Test
    @DisplayName("RequestHandler interface is minimal (single method)")
    void testRequestHandlerInterfaceIsMinimal() {
        // Verify interface only defines one method
        java.lang.reflect.Method[] methods = RequestHandler.class.getDeclaredMethods();
        assertEquals(1, methods.length, "RequestHandler should define exactly one method");
        assertEquals("handleSystemRequest", methods[0].getName());
    }

    /**
     * Custom test RequestHandler implementation.
     */
    private static class CustomTestHandler implements RequestHandler {
        @Override
        public String handleSystemRequest(String screenContent) {
            if (screenContent.contains("CONFIRM")) {
                return "1";  // Confirm
            } else if (screenContent.contains("ERROR")) {
                return "3";  // Error recovery
            }
            return null;  // Return to menu
        }
    }
}
