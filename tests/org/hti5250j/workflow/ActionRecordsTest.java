/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Action Records Tests")
class ActionRecordsTest {

    @Test
    @DisplayName("LoginAction record should be immutable")
    void testLoginActionImmutable() {
        LoginAction action = new LoginAction("testhost", "testuser", "testpass");

        assertEquals("testhost", action.host());
        assertEquals("testuser", action.user());
        assertEquals("testpass", action.password());
    }

    @Test
    @DisplayName("LoginAction should reject null host")
    void testLoginActionNullHost() {
        assertThrows(IllegalArgumentException.class,
            () -> new LoginAction(null, "user", "pass"));
    }

    @Test
    @DisplayName("LoginAction should reject null user")
    void testLoginActionNullUser() {
        assertThrows(IllegalArgumentException.class,
            () -> new LoginAction("host", null, "pass"));
    }

    @Test
    @DisplayName("LoginAction should reject null password")
    void testLoginActionNullPassword() {
        assertThrows(IllegalArgumentException.class,
            () -> new LoginAction("host", "user", null));
    }

    @Test
    @DisplayName("NavigateAction record should be immutable")
    void testNavigateActionImmutable() {
        NavigateAction action = new NavigateAction("Screen1", "[pf3]");

        assertEquals("Screen1", action.screen());
        assertEquals("[pf3]", action.keys());
    }

    @Test
    @DisplayName("NavigateAction should reject null screen")
    void testNavigateActionNullScreen() {
        assertThrows(IllegalArgumentException.class,
            () -> new NavigateAction(null, "[pf3]"));
    }

    @Test
    @DisplayName("FillAction record should be immutable")
    void testFillActionImmutable() {
        Map<String, String> fields = new HashMap<>();
        fields.put("Field1", "Value1");
        FillAction action = new FillAction(fields, null);

        assertNotNull(action.fields());
        assertTrue(action.fields().containsKey("Field1"));
    }

    @Test
    @DisplayName("FillAction should reject null fields")
    void testFillActionNullFields() {
        assertThrows(IllegalArgumentException.class,
            () -> new FillAction(null, null));
    }

    @Test
    @DisplayName("FillAction should reject empty fields")
    void testFillActionEmptyFields() {
        assertThrows(IllegalArgumentException.class,
            () -> new FillAction(new HashMap<>(), null));
    }

    @Test
    @DisplayName("SubmitAction record should be immutable")
    void testSubmitActionImmutable() {
        SubmitAction action = new SubmitAction("enter");

        assertEquals("enter", action.key());
    }

    @Test
    @DisplayName("SubmitAction should reject null key")
    void testSubmitActionNullKey() {
        assertThrows(IllegalArgumentException.class,
            () -> new SubmitAction(null));
    }

    @Test
    @DisplayName("AssertAction record should be immutable")
    void testAssertActionImmutable() {
        AssertAction action = new AssertAction("ExpectedText", null);

        assertEquals("ExpectedText", action.text());
        assertNull(action.screen());
    }

    @Test
    @DisplayName("WaitAction record should be immutable")
    void testWaitActionImmutable() {
        WaitAction action = new WaitAction(5000);

        assertEquals(5000, action.timeout());
    }

    @Test
    @DisplayName("CaptureAction record should be immutable")
    void testCaptureActionImmutable() {
        CaptureAction action = new CaptureAction("ScreenCapture");

        assertEquals("ScreenCapture", action.name());
    }

    @Test
    @DisplayName("Action records should support equality comparison")
    void testActionEquality() {
        LoginAction action1 = new LoginAction("host", "user", "pass");
        LoginAction action2 = new LoginAction("host", "user", "pass");
        LoginAction action3 = new LoginAction("host", "user", "different");

        assertEquals(action1, action2);
        assertNotEquals(action1, action3);
    }

    @Test
    @DisplayName("Action records should support hashCode")
    void testActionHashCode() {
        LoginAction action1 = new LoginAction("host", "user", "pass");
        LoginAction action2 = new LoginAction("host", "user", "pass");

        assertEquals(action1.hashCode(), action2.hashCode());
    }

    @Test
    @DisplayName("LoginAction should work as part of sealed Action interface")
    void testLoginActionAsAction() {
        Action action = new LoginAction("host", "user", "pass");

        assertNotNull(action);
        assertTrue(action instanceof LoginAction);
    }

    @Test
    @DisplayName("NavigateAction should work as part of sealed Action interface")
    void testNavigateActionAsAction() {
        Action action = new NavigateAction("Screen", "[pf3]");

        assertNotNull(action);
        assertTrue(action instanceof NavigateAction);
    }

    @Test
    @DisplayName("FillAction should work as part of sealed Action interface")
    void testFillActionAsAction() {
        Map<String, String> fields = new HashMap<>();
        fields.put("key", "value");
        Action action = new FillAction(fields, null);

        assertNotNull(action);
        assertTrue(action instanceof FillAction);
    }

    @Test
    @DisplayName("SubmitAction should work as part of sealed Action interface")
    void testSubmitActionAsAction() {
        Action action = new SubmitAction("enter");

        assertNotNull(action);
        assertTrue(action instanceof SubmitAction);
    }

    @Test
    @DisplayName("AssertAction should work as part of sealed Action interface")
    void testAssertActionAsAction() {
        Action action = new AssertAction("text", null);

        assertNotNull(action);
        assertTrue(action instanceof AssertAction);
    }

    @Test
    @DisplayName("WaitAction should work as part of sealed Action interface")
    void testWaitActionAsAction() {
        Action action = new WaitAction(1000);

        assertNotNull(action);
        assertTrue(action instanceof WaitAction);
    }

    @Test
    @DisplayName("CaptureAction should work as part of sealed Action interface")
    void testCaptureActionAsAction() {
        Action action = new CaptureAction("capture");

        assertNotNull(action);
        assertTrue(action instanceof CaptureAction);
    }

    @Test
    @DisplayName("Pattern matching should work with LoginAction")
    void testPatternMatchingLogin() {
        Action action = new LoginAction("host", "user", "pass");

        String result = switch (action) {
            case LoginAction login -> "LOGIN: " + login.host();
            default -> "OTHER";
        };

        assertEquals("LOGIN: host", result);
    }

    @Test
    @DisplayName("Pattern matching should work with NavigateAction")
    void testPatternMatchingNavigate() {
        Action action = new NavigateAction("Screen", "[pf3]");

        String result = switch (action) {
            case NavigateAction nav -> "NAVIGATE: " + nav.screen();
            default -> "OTHER";
        };

        assertEquals("NAVIGATE: Screen", result);
    }

    @Test
    @DisplayName("Action record toString() should include type and fields")
    void testActionToString() {
        LoginAction action = new LoginAction("host", "user", "pass");
        String str = action.toString();

        assertNotNull(str);
        assertTrue(str.contains("LoginAction"));
    }
}
