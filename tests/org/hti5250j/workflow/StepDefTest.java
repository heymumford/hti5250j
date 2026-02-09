/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StepDef Tests")
class StepDefTest {

    @Test
    @DisplayName("should create empty StepDef")
    void testCreateEmpty() {
        StepDef step = new StepDef();

        assertNull(step.getAction());
        assertNull(step.getHost());
        assertNull(step.getUser());
        assertNull(step.getPassword());
        assertNull(step.getScreen());
        assertNull(step.getKey());
        assertNull(step.getText());
        assertNull(step.getFields());
        assertNull(step.getTimeout());
        assertNull(step.getName());
        assertNull(step.getKeys());
    }

    @Test
    @DisplayName("should set and get action (enum)")
    void testActionEnum() {
        StepDef step = new StepDef();
        step.setAction(ActionType.LOGIN);

        assertEquals(ActionType.LOGIN, step.getAction());
    }

    @ParameterizedTest
    @ValueSource(strings = {"LOGIN", "login", "NAVIGATE", "navigate", "FILL", "fill"})
    @DisplayName("should convert string action to enum (case-insensitive)")
    void testActionStringConversion(String actionString) {
        StepDef step = new StepDef();
        step.setAction(actionString);

        assertNotNull(step.getAction());
        assertEquals(ActionType.valueOf(actionString.toUpperCase()), step.getAction());
    }

    @Test
    @DisplayName("should throw when setting invalid action string")
    void testActionStringInvalid() {
        StepDef step = new StepDef();

        assertThrows(IllegalArgumentException.class, () -> step.setAction("INVALID_ACTION"));
    }

    @Test
    @DisplayName("should handle null action string")
    void testActionStringNull() {
        StepDef step = new StepDef();
        step.setAction("LOGIN");
        step.setAction((String) null);

        // Should remain as previously set value (no change)
        assertEquals(ActionType.LOGIN, step.getAction());
    }

    @Test
    @DisplayName("should set and get host")
    void testHost() {
        StepDef step = new StepDef();
        step.setHost("192.168.1.100");

        assertEquals("192.168.1.100", step.getHost());
    }

    @Test
    @DisplayName("should set and get user")
    void testUser() {
        StepDef step = new StepDef();
        step.setUser("testuser");

        assertEquals("testuser", step.getUser());
    }

    @Test
    @DisplayName("should set and get password")
    void testPassword() {
        StepDef step = new StepDef();
        step.setPassword("secretpass");

        assertEquals("secretpass", step.getPassword());
    }

    @Test
    @DisplayName("should set and get screen")
    void testScreen() {
        StepDef step = new StepDef();
        step.setScreen("MainMenu");

        assertEquals("MainMenu", step.getScreen());
    }

    @Test
    @DisplayName("should set and get key")
    void testKey() {
        StepDef step = new StepDef();
        step.setKey("enter");

        assertEquals("enter", step.getKey());
    }

    @Test
    @DisplayName("should set and get text")
    void testText() {
        StepDef step = new StepDef();
        step.setText("Payment confirmation");

        assertEquals("Payment confirmation", step.getText());
    }

    @Test
    @DisplayName("should set and get fields map")
    void testFields() {
        StepDef step = new StepDef();
        Map<String, String> fields = new HashMap<>();
        fields.put("AccountNumber", "123456");
        fields.put("Amount", "500.00");
        step.setFields(fields);

        assertEquals(fields, step.getFields());
        assertEquals("123456", step.getFields().get("AccountNumber"));
    }

    @Test
    @DisplayName("should set and get timeout")
    void testTimeout() {
        StepDef step = new StepDef();
        step.setTimeout(5000);

        assertEquals(5000, step.getTimeout());
    }

    @Test
    @DisplayName("should set and get name")
    void testName() {
        StepDef step = new StepDef();
        step.setName("LoginStep");

        assertEquals("LoginStep", step.getName());
    }

    @Test
    @DisplayName("should set and get keys")
    void testKeys() {
        StepDef step = new StepDef();
        step.setKeys("[pf3][enter]");

        assertEquals("[pf3][enter]", step.getKeys());
    }

    @Test
    @DisplayName("should handle complete LOGIN step definition")
    void testCompleteLoginStep() {
        StepDef step = new StepDef();
        step.setAction("LOGIN");
        step.setHost("ibmi.example.com");
        step.setUser("TESTUSER");
        step.setPassword("PASSWORD123");
        step.setTimeout(30000);

        assertEquals(ActionType.LOGIN, step.getAction());
        assertEquals("ibmi.example.com", step.getHost());
        assertEquals("TESTUSER", step.getUser());
        assertEquals("PASSWORD123", step.getPassword());
        assertEquals(30000, step.getTimeout());
    }

    @Test
    @DisplayName("should handle complete FILL step definition")
    void testCompleteFillStep() {
        StepDef step = new StepDef();
        step.setAction("FILL");
        Map<String, String> fields = new HashMap<>();
        fields.put("CustomerID", "CUST001");
        fields.put("Amount", "1000.00");
        step.setFields(fields);
        step.setTimeout(5000);

        assertEquals(ActionType.FILL, step.getAction());
        assertEquals(2, step.getFields().size());
        assertEquals("CUST001", step.getFields().get("CustomerID"));
    }

    @Test
    @DisplayName("should handle complete ASSERT step definition")
    void testCompleteAssertStep() {
        StepDef step = new StepDef();
        step.setAction("ASSERT");
        step.setText("Transaction complete");
        step.setName("PaymentAssertion");

        assertEquals(ActionType.ASSERT, step.getAction());
        assertEquals("Transaction complete", step.getText());
        assertEquals("PaymentAssertion", step.getName());
    }

    @Test
    @DisplayName("should support independent field updates")
    void testIndependentFieldUpdates() {
        StepDef step = new StepDef();

        // Update fields independently
        step.setAction("NAVIGATE");
        step.setScreen("MenuScreen");
        step.setKeys("[pf3]");
        step.setTimeout(3000);

        assertEquals(ActionType.NAVIGATE, step.getAction());
        assertEquals("MenuScreen", step.getScreen());
        assertEquals("[pf3]", step.getKeys());
        assertEquals(3000, step.getTimeout());

        // Update again
        step.setScreen("SecondScreen");
        assertEquals("SecondScreen", step.getScreen());
    }

    @Test
    @DisplayName("should allow null fields for optional properties")
    void testNullFields() {
        StepDef step = new StepDef();
        step.setFields(new HashMap<>());
        step.setFields(null);

        assertNull(step.getFields());
    }

    @Test
    @DisplayName("should preserve empty fields map")
    void testEmptyFieldsMap() {
        StepDef step = new StepDef();
        Map<String, String> emptyFields = new HashMap<>();
        step.setFields(emptyFields);

        assertNotNull(step.getFields());
        assertTrue(step.getFields().isEmpty());
    }
}
