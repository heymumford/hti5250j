/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.workflow;

import java.util.HashMap;
import java.util.Map;

public class ParameterSubstitutionVerifier {

    public Map<String, String> createDataRow(String fieldName, String fieldValue) {
        Map<String, String> data = new HashMap<>();
        data.put(fieldName, fieldValue);
        return data;
    }

    public Map<String, String> createDataRow(String... keyValuePairs) {
        Map<String, String> data = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            if (i + 1 < keyValuePairs.length) {
                data.put(keyValuePairs[i], keyValuePairs[i + 1]);
            }
        }
        return data;
    }

    public boolean verifySubstitution(String result, String expectedValue) {
        return result != null && result.equals(expectedValue);
    }

    public boolean verifyPlaceholderUnchanged(String result, String placeholder) {
        return result != null && result.contains(placeholder);
    }

    public boolean verifyAllPlaceholdersUnchanged(String result, String... placeholders) {
        for (String placeholder : placeholders) {
            if (!result.contains(placeholder)) {
                return false;
            }
        }
        return true;
    }

    public int countOccurrences(String str, String substring) {
        if (str == null || substring == null) return 0;
        return (str.length() - str.replace(substring, "").length()) / substring.length();
    }

    public boolean verifyAllOccurrencesSubstituted(String result, String placeholder) {
        return !result.contains(placeholder);
    }

    public Map<String, String> createSQLInjectionData() {
        return createDataRow("account", "'; DROP TABLE accounts; --");
    }

    public Map<String, String> createFormatStringAttackData() {
        return createDataRow("input", "%x %x %x %n");
    }

    public Map<String, String> createPathTraversalData() {
        return createDataRow("filename", "../../etc/passwd");
    }

    public Map<String, String> createXPathInjectionData() {
        return createDataRow("username", "' or '1'='1");
    }

    public Map<String, String> createSpecialCharacterData() {
        return createDataRow("value", "!@#$%^&*()_+-=[]{}|;:',.<>?/~`");
    }

    public Map<String, String> createUnicodeData() {
        return createDataRow("name", "User UNICODE");
    }

    public Map<String, String> createDataRowWithNull(String fieldName) {
        Map<String, String> data = new HashMap<>();
        data.put(fieldName, null);
        return data;
    }

    public Map<String, String> createDataRowWithEmpty(String fieldName) {
        return createDataRow(fieldName, "");
    }

    public Map<String, String> createDataRowWithWhitespace(String fieldName) {
        return createDataRow(fieldName, "  \t\n  ");
    }
}
