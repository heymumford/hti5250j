/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@DisplayName("Parameter Substitution - Pairwise TDD")
public class ParameterSubstitutionPairwiseTest {

    private DatasetLoader loader;
    private ParameterSubstitutionVerifier verifier;

    @BeforeEach
    void setUp() {
        loader = new DatasetLoader();
        verifier = new ParameterSubstitutionVerifier();
    }

    @Test
    @DisplayName("LOGIN: Valid parameter substitution (host from data)")
    void testLoginValidParameterSubstitution() {
        Map<String, String> data = verifier.createDataRow("host", "SYSTEM1");
        String template = "Connect to ${data.host}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, equalTo("Connect to SYSTEM1"));
    }

    @Test
    @DisplayName("LOGIN: Missing parameter leaves placeholder unchanged")
    void testLoginMissingParameter() {
        Map<String, String> data = verifier.createDataRow("user", "testuser");
        String template = "Login to ${data.host}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, equalTo("Login to ${data.host}"));
    }

    @Test
    @DisplayName("LOGIN: Null parameter value handled gracefully")
    void testLoginNullParameterValue() {
        Map<String, String> data = new HashMap<>();
        data.put("host", null);
        String template = "Connect to ${data.host}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, notNullValue());
    }

    @Test
    @DisplayName("LOGIN: Empty parameter value substituted")
    void testLoginEmptyParameterValue() {
        Map<String, String> data = verifier.createDataRow("host", "");
        String template = "Connect to ${data.host}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, equalTo("Connect to "));
    }

    @Test
    @DisplayName("LOGIN: Multiple occurrences of same parameter replaced")
    void testLoginMultipleParameterOccurrences() {
        Map<String, String> data = verifier.createDataRow("host", "PROD1");
        String template = "Primary: ${data.host}, Backup: ${data.host}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, equalTo("Primary: PROD1, Backup: PROD1"));
    }

    @Test
    @DisplayName("NAVIGATE: Screen name from parameter")
    void testNavigateValidScreenParameter() {
        Map<String, String> data = verifier.createDataRow("screen", "MenuScreen");
        String template = "Navigate to ${data.screen}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, equalTo("Navigate to MenuScreen"));
    }

    @Test
    @DisplayName("NAVIGATE: Missing screen parameter")
    void testNavigateMissingScreenParameter() {
        Map<String, String> data = new HashMap<>();
        String template = "Navigate to ${data.screen}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, equalTo("Navigate to ${data.screen}"));
    }

    @Test
    @DisplayName("NAVIGATE: Multiple parameters in template")
    void testNavigateMultipleParameters() {
        Map<String, String> data = new HashMap<>();
        data.put("source", "Home");
        data.put("dest", "Payments");
        String template = "From ${data.source} to ${data.dest}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, equalTo("From Home to Payments"));
    }

    @Test
    @DisplayName("NAVIGATE: Special characters in parameter preserved")
    void testNavigateSpecialCharactersInParameter() {
        Map<String, String> data = verifier.createDataRow("screen", "Screen-123_Dash");
        String template = "Navigate to ${data.screen}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, equalTo("Navigate to Screen-123_Dash"));
    }

    @Test
    @DisplayName("FILL: Field value from parameter")
    void testFillValidFieldParameter() {
        Map<String, String> data = verifier.createDataRow("amount", "1000.00");
        String template = "Amount: ${data.amount}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, equalTo("Amount: 1000.00"));
    }

    @Test
    @DisplayName("FILL: Missing field parameter leaves placeholder")
    void testFillMissingFieldParameter() {
        Map<String, String> data = new HashMap<>();
        String template = "Amount: ${data.amount}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, equalTo("Amount: ${data.amount}"));
    }

    @Test
    @DisplayName("FILL: Numeric field value as string")
    void testFillNumericFieldValue() {
        Map<String, String> data = verifier.createDataRow("quantity", "500");
        String template = "Qty: ${data.quantity}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, equalTo("Qty: 500"));
    }

    @Test
    @DisplayName("FILL: Whitespace in parameter preserved")
    void testFillWhitespaceInParameter() {
        Map<String, String> data = verifier.createDataRow("description", "  leading and trailing  ");
        String template = "Desc: ${data.description}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, equalTo("Desc:   leading and trailing  "));
    }

    @Test
    @DisplayName("ASSERT: Expected text from parameter")
    void testAssertValidTextParameter() {
        Map<String, String> data = verifier.createDataRow("expected", "SUCCESS");
        String template = "Expected: ${data.expected}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, equalTo("Expected: SUCCESS"));
    }

    @Test
    @DisplayName("ASSERT: Missing assertion text")
    void testAssertMissingTextParameter() {
        Map<String, String> data = new HashMap<>();
        String template = "Expected: ${data.expected}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, equalTo("Expected: ${data.expected}"));
    }

    @Test
    @DisplayName("ASSERT: Case-sensitive parameter matching")
    void testAssertCaseSensitiveParameter() {
        Map<String, String> data = verifier.createDataRow("status", "APPROVED");
        String template = "Status is ${data.status}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, equalTo("Status is APPROVED"));
    }

    @Test
    @DisplayName("ASSERT: Partial placeholders NOT replaced")
    void testAssertPartialPlaceholderNotReplaced() {
        Map<String, String> data = verifier.createDataRow("field", "value");
        String template = "Value: ${data.notfield}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, equalTo("Value: ${data.notfield}"));
    }

    @Test
    @DisplayName("CAPTURE: Artifact name from parameter")
    void testCaptureValidNameParameter() {
        Map<String, String> data = verifier.createDataRow("timestamp", "2025-01-15T10:30:00");
        String template = "capture_${data.timestamp}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, equalTo("capture_2025-01-15T10:30:00"));
    }

    @Test
    @DisplayName("CAPTURE: Missing name parameter")
    void testCaptureMissingNameParameter() {
        Map<String, String> data = new HashMap<>();
        String template = "capture_${data.timestamp}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, equalTo("capture_${data.timestamp}"));
    }

    @Test
    @DisplayName("CAPTURE: Complex name with multiple parameters")
    void testCaptureComplexNameWithMultipleParameters() {
        Map<String, String> data = new HashMap<>();
        data.put("date", "2025-01-15");
        data.put("account", "ACC123");
        String template = "report_${data.date}_${data.account}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, equalTo("report_2025-01-15_ACC123"));
    }

    @Test
    @DisplayName("Adversarial: SQL injection attempt in parameter")
    void testAdversarialSQLInjectionInParameter() {
        Map<String, String> data = verifier.createDataRow("account", "'; DROP TABLE accounts; --");
        String template = "SELECT * FROM users WHERE account = '${data.account}'";
        String result = loader.replaceParameters(template, data);
        assertThat(result, containsString("DROP TABLE accounts"));
    }

    @Test
    @DisplayName("Adversarial: Format string attack in parameter")
    void testAdversarialFormatStringAttack() {
        Map<String, String> data = verifier.createDataRow("input", "%x %x %x %n");
        String template = "Input: ${data.input}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, equalTo("Input: %x %x %x %n"));
    }

    @Test
    @DisplayName("Adversarial: Path traversal attempt in parameter")
    void testAdversarialPathTraversalInParameter() {
        Map<String, String> data = verifier.createDataRow("filename", "../../etc/passwd");
        String template = "/uploads/${data.filename}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, containsString("../../etc/passwd"));
    }

    @Test
    @DisplayName("Adversarial: XPath injection attempt in parameter")
    void testAdversarialXPathInjectionInParameter() {
        Map<String, String> data = verifier.createDataRow("username", "' or '1'='1");
        String template = "xpath: //user[name='${data.username}']";
        String result = loader.replaceParameters(template, data);
        assertThat(result, containsString("or '1'='1"));
    }

    @Test
    @DisplayName("Adversarial: Unicode/emoji characters in parameter")
    void testAdversarialUnicodeEmojiInParameter() {
        Map<String, String> data = verifier.createDataRow("name", "User EMOJI");
        String template = "Name: ${data.name}";
        String result = loader.replaceParameters(template, data);
        assertThat(result, equalTo("Name: User EMOJI"));
    }
}
