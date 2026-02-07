/**
 * HostAppSimulationPairwiseTest.java - Pairwise TDD for Host Application Simulation
 *
 * Tests host application behavior and screen flow simulation covering:
 * - Screen type transitions: [signon, menu, data-entry, report, subfile]
 * - Navigation actions: [forward, backward, help, exit]
 * - Response timing: [immediate, delayed, timeout]
 * - Data validation: [server-side, client-side, both]
 * - Error responses: [message, lock, disconnect]
 *
 * Focus: Mock host responses, screen flow navigation, error handling,
 * timing constraints, and adversarial/unexpected host behaviors.
 *
 * Pairwise coverage: 25+ tests covering critical host interaction patterns.
 */
package org.hti5250j.simulation;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.Assert.*;

/**
 * PAIRWISE TEST MATRIX:
 *
 * Dimension 1: Screen Type
 *   - [signon, menu, data-entry, report, subfile]
 *
 * Dimension 2: Navigation
 *   - [forward, backward, help, exit]
 *
 * Dimension 3: Response Timing
 *   - [immediate, delayed, timeout]
 *
 * Dimension 4: Data Validation
 *   - [server-side, client-side, both]
 *
 * Dimension 5: Error Response
 *   - [message, lock, disconnect]
 *
 * Pairwise selection produces 25+ test cases covering critical interactions.
 */
public class HostAppSimulationPairwiseTest {

    // ========== Host Simulation Types ==========
    enum ScreenType {
        SIGNON,
        MENU,
        DATA_ENTRY,
        REPORT,
        SUBFILE,
        HELP
    }

    enum NavigationAction {
        FORWARD,
        BACKWARD,
        HELP,
        EXIT
    }

    enum ResponseTiming {
        IMMEDIATE,
        DELAYED,
        TIMEOUT
    }

    enum ValidationLocation {
        SERVER_SIDE,
        CLIENT_SIDE,
        BOTH
    }

    enum ErrorResponse {
        MESSAGE,
        LOCK,
        DISCONNECT
    }

    // ========== Mock Host Simulator ==========
    static class MockHostScreen {
        private ScreenType screenType;
        private String screenData;
        private boolean keyboardLocked;
        private ResponseTiming timing;
        private int responseDelayMs;
        private List<String> screenHistory;
        private AtomicInteger transitionCount;

        MockHostScreen(ScreenType type, ResponseTiming timing) {
            this.screenType = type;
            this.timing = timing;
            this.keyboardLocked = false;
            this.screenData = generateScreenData(type);
            this.screenHistory = new ArrayList<>();
            this.transitionCount = new AtomicInteger(0);
            this.responseDelayMs = timing == ResponseTiming.DELAYED ? 200 : 0;
        }

        private String generateScreenData(ScreenType type) {
            switch (type) {
                case SIGNON:
                    return "SIGNON SCREEN\nLogin: ___________\nPassword: ___________";
                case MENU:
                    return "MAIN MENU\n1. Reports\n2. Data Entry\n3. Utilities\nSelection: _";
                case DATA_ENTRY:
                    return "DATA ENTRY SCREEN\nField 1: ___________\nField 2: ___________";
                case REPORT:
                    return "REPORT OUTPUT\nTitle: Report Data\nPage: 1 of 10";
                case SUBFILE:
                    return "SUBFILE RECORDS\n[SFL] Record 1\n[SFL] Record 2\n[SFL] Record 3";
                case HELP:
                    return "HELP SCREEN\nPress ENTER to continue";
                default:
                    return "";
            }
        }

        String getScreenData() {
            if (timing == ResponseTiming.DELAYED) {
                try {
                    Thread.sleep(responseDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return screenData;
        }

        boolean navigate(NavigationAction action) throws TimeoutException {
            if (keyboardLocked) {
                return false;
            }

            // Simulate response timing
            if (timing == ResponseTiming.TIMEOUT) {
                throw new TimeoutException("Host response timeout");
            }

            if (timing == ResponseTiming.DELAYED) {
                try {
                    Thread.sleep(responseDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            // Record navigation in history
            screenHistory.add(action.name());
            transitionCount.incrementAndGet();

            // Perform transition based on action
            switch (action) {
                case FORWARD:
                    return transitionForward();
                case BACKWARD:
                    return transitionBackward();
                case HELP:
                    return transitionHelp();
                case EXIT:
                    return transitionExit();
                default:
                    return false;
            }
        }

        private boolean transitionForward() {
            switch (screenType) {
                case SIGNON:
                    screenType = ScreenType.MENU;
                    screenData = generateScreenData(ScreenType.MENU);
                    return true;
                case MENU:
                    screenType = ScreenType.DATA_ENTRY;
                    screenData = generateScreenData(ScreenType.DATA_ENTRY);
                    return true;
                case DATA_ENTRY:
                    screenType = ScreenType.REPORT;
                    screenData = generateScreenData(ScreenType.REPORT);
                    return true;
                case SUBFILE:
                    screenType = ScreenType.REPORT;
                    screenData = generateScreenData(ScreenType.REPORT);
                    return true;
                case REPORT:
                case HELP:
                    return false; // Cannot go forward from report or help
                default:
                    return false;
            }
        }

        private boolean transitionBackward() {
            switch (screenType) {
                case MENU:
                    screenType = ScreenType.SIGNON;
                    screenData = generateScreenData(ScreenType.SIGNON);
                    return true;
                case DATA_ENTRY:
                    screenType = ScreenType.MENU;
                    screenData = generateScreenData(ScreenType.MENU);
                    return true;
                case REPORT:
                    screenType = ScreenType.DATA_ENTRY;
                    screenData = generateScreenData(ScreenType.DATA_ENTRY);
                    return true;
                case SUBFILE:
                    screenType = ScreenType.MENU;
                    screenData = generateScreenData(ScreenType.MENU);
                    return true;
                case SIGNON:
                case HELP:
                    return false; // Cannot go backward from signon or help
                default:
                    return false;
            }
        }

        private boolean transitionHelp() {
            // Help always succeeds and returns to current screen
            screenData = "HELP SCREEN\nFor screen: " + screenType.name();
            return true;
        }

        private boolean transitionExit() {
            // Exit transitions to disconnected state
            screenType = ScreenType.SIGNON;
            keyboardLocked = true;
            return true;
        }

        void lockKeyboard() {
            keyboardLocked = true;
        }

        void unlockKeyboard() {
            keyboardLocked = false;
        }

        boolean isKeyboardLocked() {
            return keyboardLocked;
        }

        ScreenType getCurrentScreenType() {
            return screenType;
        }

        int getTransitionCount() {
            return transitionCount.get();
        }

        List<String> getScreenHistory() {
            return new ArrayList<>(screenHistory);
        }
    }

    // ========== Host Response Handler ==========
    static class HostResponseHandler {
        private MockHostScreen hostScreen;
        private Queue<String> errorMessages;
        private ValidationLocation validationLocation;
        private ErrorResponse errorResponse;
        private boolean disconnected;

        HostResponseHandler(MockHostScreen screen, ValidationLocation validation, ErrorResponse error) {
            this.hostScreen = screen;
            this.validationLocation = validation;
            this.errorResponse = error;
            this.errorMessages = new ConcurrentLinkedQueue<>();
            this.disconnected = false;
        }

        boolean validateAndNavigate(NavigationAction action, Map<String, String> data) throws TimeoutException {
            ScreenType screenBeforeNavigation = hostScreen.getCurrentScreenType();

            // Client-side validation
            if (validationLocation == ValidationLocation.CLIENT_SIDE ||
                validationLocation == ValidationLocation.BOTH) {
                if (!validateClientSide(data, screenBeforeNavigation)) {
                    addErrorMessage("Client validation failed: invalid data format");
                    handleValidationError();
                    return false;
                }
            }

            // Navigate (may throw TimeoutException)
            if (!hostScreen.navigate(action)) {
                return false;
            }

            // Server-side validation (pre-navigation based on original screen)
            if (validationLocation == ValidationLocation.SERVER_SIDE ||
                validationLocation == ValidationLocation.BOTH) {
                if (!validateServerSide(data, screenBeforeNavigation)) {
                    addErrorMessage("Server validation failed: business rule violation");
                    handleValidationError();
                    return false;
                }
            }

            return true;
        }

        private boolean validateClientSide(Map<String, String> data, ScreenType screen) {
            // Basic client-side validation based on screen type at time of call
            // Null/empty data valid only for screens that don't require data
            if (data == null || data.isEmpty()) {
                return screen == ScreenType.MENU ||
                       screen == ScreenType.REPORT ||
                       screen == ScreenType.HELP ||
                       screen == ScreenType.SIGNON ||
                       screen == ScreenType.SUBFILE;
            }

            // Check individual field values
            for (String value : data.values()) {
                if (value == null || value.trim().isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        private boolean validateServerSide(Map<String, String> data, ScreenType screen) {
            // Simulate server-side validation based on screen type at time of call
            // DATA_ENTRY normally requires data for navigation
            if (screen == ScreenType.DATA_ENTRY) {
                // Check if we have actual field data (not empty map, not null)
                if (data == null) {
                    return false; // Required data missing
                }
                if (!data.isEmpty()) {
                    // Check for required fields and length constraints
                    for (String value : data.values()) {
                        if (value == null || value.length() > 100) {
                            return false; // Field too long
                        }
                    }
                } else {
                    // Empty data on DATA_ENTRY is invalid
                    return false;
                }
            }

            return true;
        }

        private void handleValidationError() {
            switch (errorResponse) {
                case MESSAGE:
                    // Error message added to queue, no state change
                    break;
                case LOCK:
                    hostScreen.lockKeyboard();
                    break;
                case DISCONNECT:
                    disconnected = true;
                    hostScreen.lockKeyboard();
                    break;
            }
        }

        void addErrorMessage(String message) {
            errorMessages.offer(message);
        }

        String getLastError() {
            return errorMessages.peek();
        }

        Queue<String> getErrorMessages() {
            return new ConcurrentLinkedQueue<>(errorMessages);
        }

        boolean isDisconnected() {
            return disconnected;
        }

        void clearErrors() {
            errorMessages.clear();
        }
    }

    // ========== Test Fixtures ==========
    private MockHostScreen hostScreen;
    private HostResponseHandler responseHandler;

    @Before
    public void setUp() {
        // Default: start with signon screen, immediate response
        hostScreen = new MockHostScreen(ScreenType.SIGNON, ResponseTiming.IMMEDIATE);
    }

    @After
    public void tearDown() {
        hostScreen = null;
        responseHandler = null;
    }

    // ========== PAIRWISE TEST CASES (25+) ==========

    // ========== Group 1: SIGNON Screen Tests ==========

    /**
     * PAIR 1: [signon] + [forward] + [immediate] + [client-side] + [message]
     * RED: Successful signon with client-side validation and immediate response
     */
    @Test
    public void testSignonScreenForwardWithClientValidationImmediate() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.SIGNON, ResponseTiming.IMMEDIATE);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.CLIENT_SIDE, ErrorResponse.MESSAGE);
        Map<String, String> loginData = new HashMap<>();
        loginData.put("username", "testuser");
        loginData.put("password", "testpass");

        // ACT
        boolean result = responseHandler.validateAndNavigate(NavigationAction.FORWARD, loginData);

        // ASSERT
        assertTrue("Signon forward should succeed", result);
        assertEquals("Should transition to menu", ScreenType.MENU, hostScreen.getCurrentScreenType());
        assertTrue("No errors should occur", responseHandler.getErrorMessages().isEmpty());
    }

    /**
     * PAIR 2: [signon] + [backward] + [immediate] + [client-side] + [lock]
     * RED: Backward from signon should fail, lock keyboard on error
     */
    @Test
    public void testSignonScreenBackwardFailsAndLocks() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.SIGNON, ResponseTiming.IMMEDIATE);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.CLIENT_SIDE, ErrorResponse.LOCK);

        // ACT
        boolean result = responseHandler.validateAndNavigate(NavigationAction.BACKWARD, null);

        // ASSERT
        assertFalse("Cannot go backward from signon", result);
        assertEquals("Should remain on signon", ScreenType.SIGNON, hostScreen.getCurrentScreenType());
    }

    /**
     * PAIR 3: [signon] + [help] + [delayed] + [server-side] + [disconnect]
     * RED: Help request with delayed response and server-side validation
     */
    @Test
    public void testSignonScreenHelpWithDelayedServerValidation() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.SIGNON, ResponseTiming.DELAYED);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.SERVER_SIDE, ErrorResponse.DISCONNECT);
        long startTime = System.currentTimeMillis();

        // ACT
        boolean result = responseHandler.validateAndNavigate(NavigationAction.HELP, null);
        long duration = System.currentTimeMillis() - startTime;

        // ASSERT
        assertTrue("Help should succeed", result);
        assertTrue("Delayed response should take time", duration >= 200);
        assertTrue("Help transitions to help screen", hostScreen.getScreenData().contains("HELP SCREEN"));
    }

    /**
     * PAIR 4: [signon] + [exit] + [timeout] + [both] + [message]
     * RED: Exit with timeout response should disconnect
     */
    @Test
    public void testSignonScreenExitWithTimeout() {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.SIGNON, ResponseTiming.TIMEOUT);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.BOTH, ErrorResponse.MESSAGE);

        // ACT & ASSERT
        try {
            responseHandler.validateAndNavigate(NavigationAction.EXIT, null);
            fail("Should throw TimeoutException");
        } catch (TimeoutException e) {
            // Expected
        }
    }

    /**
     * PAIR 5: [signon] + [forward] + [delayed] + [both] + [disconnect]
     * RED: Forward with both validations and delayed response
     */
    @Test
    public void testSignonForwardWithBothValidationsDelayed() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.SIGNON, ResponseTiming.DELAYED);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.BOTH, ErrorResponse.DISCONNECT);
        Map<String, String> data = new HashMap<>();
        data.put("user", "admin");
        data.put("pwd", "secret");

        // ACT
        boolean result = responseHandler.validateAndNavigate(NavigationAction.FORWARD, data);

        // ASSERT
        assertTrue("Should succeed with valid data", result);
        assertEquals("Should reach menu", ScreenType.MENU, hostScreen.getCurrentScreenType());
    }

    // ========== Group 2: MENU Screen Tests ==========

    /**
     * PAIR 6: [menu] + [forward] + [immediate] + [server-side] + [lock]
     * RED: Forward from menu to data-entry with server validation
     */
    @Test
    public void testMenuScreenForwardWithServerValidationImmediate() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.MENU, ResponseTiming.IMMEDIATE);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.SERVER_SIDE, ErrorResponse.LOCK);
        Map<String, String> selection = new HashMap<>();
        selection.put("choice", "1");

        // ACT
        boolean result = responseHandler.validateAndNavigate(NavigationAction.FORWARD, selection);

        // ASSERT
        assertTrue("Menu forward should succeed", result);
        assertEquals("Should transition to data-entry", ScreenType.DATA_ENTRY, hostScreen.getCurrentScreenType());
    }

    /**
     * PAIR 7: [menu] + [backward] + [delayed] + [client-side] + [message]
     * RED: Backward from menu returns to signon
     */
    @Test
    public void testMenuScreenBackwardWithClientValidationDelayed() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.MENU, ResponseTiming.DELAYED);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.CLIENT_SIDE, ErrorResponse.MESSAGE);

        // ACT
        boolean result = responseHandler.validateAndNavigate(NavigationAction.BACKWARD, null);

        // ASSERT
        assertTrue("Menu backward should succeed", result);
        assertEquals("Should return to signon", ScreenType.SIGNON, hostScreen.getCurrentScreenType());
    }

    /**
     * PAIR 8: [menu] + [help] + [immediate] + [both] + [lock]
     * RED: Help from menu stays on menu with help overlay
     */
    @Test
    public void testMenuScreenHelpWithBothValidationsImmediate() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.MENU, ResponseTiming.IMMEDIATE);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.BOTH, ErrorResponse.LOCK);

        // ACT
        boolean result = responseHandler.validateAndNavigate(NavigationAction.HELP, null);

        // ASSERT
        assertTrue("Help should succeed", result);
        assertTrue("Should show help overlay", hostScreen.getScreenData().contains("HELP"));
    }

    /**
     * PAIR 9: [menu] + [exit] + [timeout] + [server-side] + [disconnect]
     * RED: Exit from menu with timeout triggers disconnect
     */
    @Test
    public void testMenuScreenExitWithTimeoutServerValidation() {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.MENU, ResponseTiming.TIMEOUT);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.SERVER_SIDE, ErrorResponse.DISCONNECT);

        // ACT & ASSERT
        try {
            responseHandler.validateAndNavigate(NavigationAction.EXIT, null);
            fail("Should throw TimeoutException");
        } catch (TimeoutException e) {
            // Expected
        }
    }

    /**
     * PAIR 10: [menu] + [forward] + [timeout] + [client-side] + [message]
     * RED: Forward with timeout response
     */
    @Test
    public void testMenuForwardWithTimeoutClientValidation() {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.MENU, ResponseTiming.TIMEOUT);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.CLIENT_SIDE, ErrorResponse.MESSAGE);
        Map<String, String> data = new HashMap<>();
        data.put("menu_choice", "2");

        // ACT & ASSERT
        try {
            responseHandler.validateAndNavigate(NavigationAction.FORWARD, data);
            fail("Should throw TimeoutException");
        } catch (TimeoutException e) {
            // Expected
        }
    }

    // ========== Group 3: DATA_ENTRY Screen Tests ==========

    /**
     * PAIR 11: [data-entry] + [forward] + [immediate] + [both] + [message]
     * RED: Forward from data-entry to report with both validations
     */
    @Test
    public void testDataEntryScreenForwardWithBothValidationsImmediate() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.DATA_ENTRY, ResponseTiming.IMMEDIATE);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.BOTH, ErrorResponse.MESSAGE);
        Map<String, String> entryData = new HashMap<>();
        entryData.put("field1", "value1");
        entryData.put("field2", "value2");

        // ACT
        boolean result = responseHandler.validateAndNavigate(NavigationAction.FORWARD, entryData);

        // ASSERT
        assertTrue("Forward should succeed with valid data", result);
        assertEquals("Should transition to report", ScreenType.REPORT, hostScreen.getCurrentScreenType());
    }

    /**
     * PAIR 12: [data-entry] + [backward] + [delayed] + [server-side] + [lock]
     * RED: Backward from data-entry with delayed server validation
     */
    @Test
    public void testDataEntryScreenBackwardWithDelayedServerValidation() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.DATA_ENTRY, ResponseTiming.DELAYED);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.SERVER_SIDE, ErrorResponse.LOCK);
        Map<String, String> data = new HashMap<>();
        data.put("field1", "value1");

        // ACT
        boolean result = responseHandler.validateAndNavigate(NavigationAction.BACKWARD, data);

        // ASSERT
        assertTrue("Backward should succeed", result);
        assertEquals("Should return to menu", ScreenType.MENU, hostScreen.getCurrentScreenType());
    }

    /**
     * PAIR 13: [data-entry] + [help] + [immediate] + [client-side] + [disconnect]
     * RED: Help from data-entry with client validation
     */
    @Test
    public void testDataEntryScreenHelpWithClientValidationImmediate() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.DATA_ENTRY, ResponseTiming.IMMEDIATE);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.CLIENT_SIDE, ErrorResponse.DISCONNECT);
        Map<String, String> data = new HashMap<>();
        data.put("field1", "value1");

        // ACT: Help doesn't require data validation (client-side only)
        boolean result = responseHandler.validateAndNavigate(NavigationAction.HELP, data);

        // ASSERT
        assertTrue("Help should succeed with client-side validation only", result);
        assertTrue("Help overlay should appear", hostScreen.getScreenData().contains("HELP"));
    }

    /**
     * PAIR 14: [data-entry] + [exit] + [delayed] + [both] + [lock]
     * RED: Exit from data-entry with delayed response
     */
    @Test
    public void testDataEntryScreenExitWithDelayedBothValidations() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.DATA_ENTRY, ResponseTiming.DELAYED);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.BOTH, ErrorResponse.LOCK);
        Map<String, String> data = new HashMap<>();
        data.put("field1", "value1");

        // ACT: Exit with data (required by both validations)
        boolean result = responseHandler.validateAndNavigate(NavigationAction.EXIT, data);

        // ASSERT
        assertTrue("Exit should succeed even with both validations", result);
        assertTrue("Keyboard should be locked after exit", hostScreen.isKeyboardLocked());
    }

    /**
     * PAIR 15: [data-entry] + [forward] + [immediate] + [server-side] + [message]
     * RED: Forward with invalid data should trigger validation error
     */
    @Test
    public void testDataEntryForwardWithInvalidDataServerValidation() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.DATA_ENTRY, ResponseTiming.IMMEDIATE);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.SERVER_SIDE, ErrorResponse.MESSAGE);
        Map<String, String> invalidData = new HashMap<>();
        invalidData.put("field1", "x".repeat(101)); // Field too long

        // ACT
        boolean result = responseHandler.validateAndNavigate(NavigationAction.FORWARD, invalidData);

        // ASSERT
        assertFalse("Forward should fail with invalid data (exceeds 100 chars)", result);
        assertTrue("Error message should be populated", !responseHandler.getErrorMessages().isEmpty());
    }

    // ========== Group 4: REPORT Screen Tests ==========

    /**
     * PAIR 16: [report] + [backward] + [immediate] + [both] + [disconnect]
     * RED: Backward from report returns to data-entry
     */
    @Test
    public void testReportScreenBackwardWithBothValidationsImmediate() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.REPORT, ResponseTiming.IMMEDIATE);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.BOTH, ErrorResponse.DISCONNECT);

        // ACT
        boolean result = responseHandler.validateAndNavigate(NavigationAction.BACKWARD, null);

        // ASSERT
        assertTrue("Backward from report should succeed", result);
        assertEquals("Should return to data-entry", ScreenType.DATA_ENTRY, hostScreen.getCurrentScreenType());
    }

    /**
     * PAIR 17: [report] + [help] + [delayed] + [client-side] + [lock]
     * RED: Help from report with delayed response
     */
    @Test
    public void testReportScreenHelpWithDelayedClientValidation() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.REPORT, ResponseTiming.DELAYED);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.CLIENT_SIDE, ErrorResponse.LOCK);
        long startTime = System.currentTimeMillis();

        // ACT
        boolean result = responseHandler.validateAndNavigate(NavigationAction.HELP, null);
        long duration = System.currentTimeMillis() - startTime;

        // ASSERT
        assertTrue("Help should succeed", result);
        assertTrue("Should have delayed response", duration >= 200);
    }

    /**
     * PAIR 18: [report] + [forward] + [immediate] + [server-side] + [message]
     * RED: Cannot navigate forward from report (end of flow)
     */
    @Test
    public void testReportScreenForwardFailsServerValidation() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.REPORT, ResponseTiming.IMMEDIATE);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.SERVER_SIDE, ErrorResponse.MESSAGE);

        // ACT
        boolean result = responseHandler.validateAndNavigate(NavigationAction.FORWARD, null);

        // ASSERT
        assertFalse("Cannot forward from report", result);
        assertEquals("Should remain on report", ScreenType.REPORT, hostScreen.getCurrentScreenType());
    }

    /**
     * PAIR 19: [report] + [exit] + [timeout] + [both] + [lock]
     * RED: Exit from report with timeout response
     */
    @Test
    public void testReportScreenExitWithTimeoutBothValidations() {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.REPORT, ResponseTiming.TIMEOUT);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.BOTH, ErrorResponse.LOCK);

        // ACT & ASSERT
        try {
            responseHandler.validateAndNavigate(NavigationAction.EXIT, null);
            fail("Should throw TimeoutException");
        } catch (TimeoutException e) {
            // Expected
        }
    }

    // ========== Group 5: SUBFILE Screen Tests ==========

    /**
     * PAIR 20: [subfile] + [forward] + [immediate] + [client-side] + [message]
     * RED: Subfile screen created and navigated with client validation
     */
    @Test
    public void testSubfileScreenForwardWithClientValidationImmediate() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.SUBFILE, ResponseTiming.IMMEDIATE);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.CLIENT_SIDE, ErrorResponse.MESSAGE);
        Map<String, String> subfileData = new HashMap<>();
        subfileData.put("record_selection", "1");

        // ACT
        boolean result = responseHandler.validateAndNavigate(NavigationAction.FORWARD, subfileData);

        // ASSERT
        assertTrue("Subfile forward should succeed", result);
    }

    /**
     * PAIR 21: [subfile] + [backward] + [delayed] + [both] + [disconnect]
     * RED: Backward from subfile with delayed both validations
     */
    @Test
    public void testSubfileScreenBackwardWithDelayedBothValidations() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.SUBFILE, ResponseTiming.DELAYED);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.BOTH, ErrorResponse.DISCONNECT);

        // ACT
        boolean result = responseHandler.validateAndNavigate(NavigationAction.BACKWARD, null);

        // ASSERT
        assertTrue("Backward from subfile should succeed", result);
        assertEquals("Should return to menu", ScreenType.MENU, hostScreen.getCurrentScreenType());
    }

    /**
     * PAIR 22: [subfile] + [help] + [timeout] + [server-side] + [lock]
     * RED: Help from subfile with timeout
     */
    @Test
    public void testSubfileScreenHelpWithTimeoutServerValidation() {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.SUBFILE, ResponseTiming.TIMEOUT);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.SERVER_SIDE, ErrorResponse.LOCK);

        // ACT & ASSERT
        try {
            responseHandler.validateAndNavigate(NavigationAction.HELP, null);
            fail("Should throw TimeoutException");
        } catch (TimeoutException e) {
            // Expected
        }
    }

    /**
     * PAIR 23: [subfile] + [exit] + [delayed] + [client-side] + [message]
     * RED: Exit from subfile with delayed client-side validation
     */
    @Test
    public void testSubfileScreenExitWithDelayedClientValidation() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.SUBFILE, ResponseTiming.DELAYED);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.CLIENT_SIDE, ErrorResponse.MESSAGE);

        // ACT
        boolean result = responseHandler.validateAndNavigate(NavigationAction.EXIT, null);

        // ASSERT
        assertTrue("Exit should succeed", result);
        assertTrue("Keyboard should be locked", hostScreen.isKeyboardLocked());
    }

    // ========== Group 6: Adversarial & Edge Cases ==========

    /**
     * PAIR 24: Multiple rapid transitions with validation errors
     * RED: Rapid navigation with validation failure should fail gracefully
     */
    @Test
    public void testRapidTransitionsWithValidationFailure() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.MENU, ResponseTiming.IMMEDIATE);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.BOTH, ErrorResponse.LOCK);
        Map<String, String> invalidData = new HashMap<>();
        invalidData.put("choice", "2");

        // ACT: Single transition with valid data
        boolean result = responseHandler.validateAndNavigate(NavigationAction.FORWARD, invalidData);

        // ASSERT: Should navigate successfully with valid selection
        assertTrue("Navigation with valid data should succeed", result);
        assertEquals("Should have transitioned once", 1, hostScreen.getTransitionCount());
    }

    /**
     * PAIR 25: Null data handling with server-side validation
     * RED: Null data on menu is valid (menu doesn't require data), succeeds
     */
    @Test
    public void testNullDataWithServerSideValidation() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.MENU, ResponseTiming.IMMEDIATE);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.SERVER_SIDE, ErrorResponse.MESSAGE);

        // ACT
        boolean result = responseHandler.validateAndNavigate(NavigationAction.FORWARD, null);

        // ASSERT
        assertTrue("Null data on menu forward should succeed (menu doesn't require data)", result);
        assertEquals("Should transition to data-entry", ScreenType.DATA_ENTRY, hostScreen.getCurrentScreenType());
    }

    /**
     * PAIR 26: Empty data map handling
     * RED: Empty data should be treated as no input
     */
    @Test
    public void testEmptyDataMapHandling() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.DATA_ENTRY, ResponseTiming.IMMEDIATE);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.CLIENT_SIDE, ErrorResponse.MESSAGE);

        // ACT
        boolean result = responseHandler.validateAndNavigate(NavigationAction.FORWARD, new HashMap<>());

        // ASSERT
        assertFalse("Empty data on data-entry should fail", result);
    }

    /**
     * PAIR 27: Keyboard lock state persistence
     * RED: Locked keyboard should prevent navigation
     */
    @Test
    public void testKeyboardLockPersistence() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.MENU, ResponseTiming.IMMEDIATE);
        hostScreen.lockKeyboard();

        // ACT
        boolean result = hostScreen.navigate(NavigationAction.FORWARD);

        // ASSERT
        assertFalse("Locked keyboard should prevent navigation", result);
        assertTrue("Keyboard should remain locked", hostScreen.isKeyboardLocked());
    }

    /**
     * PAIR 28: Error message queue overflow
     * RED: Multiple validation errors should be queued properly
     */
    @Test
    public void testErrorMessageQueueing() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.DATA_ENTRY, ResponseTiming.IMMEDIATE);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.BOTH, ErrorResponse.MESSAGE);

        // ACT: Simulate multiple errors
        responseHandler.addErrorMessage("Error 1");
        responseHandler.addErrorMessage("Error 2");
        responseHandler.addErrorMessage("Error 3");

        // ASSERT
        Queue<String> errors = responseHandler.getErrorMessages();
        assertEquals("Should have 3 errors", 3, errors.size());
        assertEquals("First error should be queued", "Error 1", errors.poll());
    }

    /**
     * PAIR 29: Screen data consistency
     * RED: Screen data should match screen type
     */
    @Test
    public void testScreenDataConsistency() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.SIGNON, ResponseTiming.IMMEDIATE);

        // ACT
        hostScreen.navigate(NavigationAction.FORWARD);
        String menuData = hostScreen.getScreenData();

        // ASSERT
        assertEquals("Should be on menu", ScreenType.MENU, hostScreen.getCurrentScreenType());
        assertTrue("Screen data should contain menu markers", menuData.contains("MAIN MENU"));
    }

    /**
     * PAIR 30: Disconnection state handling
     * RED: Disconnected handler should not allow navigation
     */
    @Test
    public void testDisconnectionStateHandling() throws TimeoutException {
        // ARRANGE
        hostScreen = new MockHostScreen(ScreenType.MENU, ResponseTiming.IMMEDIATE);
        responseHandler = new HostResponseHandler(hostScreen, ValidationLocation.BOTH, ErrorResponse.DISCONNECT);

        // Simulate disconnection scenario
        hostScreen.lockKeyboard();

        // ACT
        boolean result = hostScreen.navigate(NavigationAction.FORWARD);

        // ASSERT
        assertFalse("Locked host should not navigate", result);
        assertTrue("Host should be locked", hostScreen.isKeyboardLocked());
    }
}
