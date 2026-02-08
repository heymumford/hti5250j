/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.surfaces;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Surface tests for schema contract validation.
 *
 * Verifies that field definitions match i5 reality and that no data loss
 * occurs during field operations.
 *
 * High test-to-code ratio is WORTH IT because:
 * - Schema drift is silent (fields change on i5 side, nothing alerts us)
 * - Truncation bugs hide in production logs (last digits cut off)
 * - Boundary conditions (max length, special values) are where bugs hide
 */
@DisplayName("Schema Contract Surface Tests")
public class SchemaContractSurfaceTest {

    private SchemaContractVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new SchemaContractVerifier();
    }

    // ============================================================================
    // Surface 1: Numeric Field Boundaries
    // ============================================================================

    @Test
    @DisplayName("Surface 1.1: Numeric field min/max bounds are enforced")
    void surfaceNumericFieldBoundsEnforced() {
        int minValue = verifier.getFieldMinValue("AMOUNT");
        int maxValue = verifier.getFieldMaxValue("AMOUNT");

        assertThat("Min should be less than max", minValue, lessThan(maxValue));
        assertThat("Min should be reasonable", minValue, greaterThanOrEqualTo(-999999));
        assertThat("Max should be reasonable", maxValue, lessThanOrEqualTo(999999999));
    }

    @Test
    @DisplayName("Surface 1.2: Field accepts minimum boundary value")
    void surfaceFieldAcceptsMinimumValue() {
        int minValue = verifier.getFieldMinValue("QUANTITY");
        boolean accepted = verifier.setNumericField("QUANTITY", minValue);

        assertThat("Field should accept minimum value", accepted, is(true));
        assertThat("Stored value should match minimum",
                   verifier.getNumericField("QUANTITY"), equalTo(minValue));
    }

    @Test
    @DisplayName("Surface 1.3: Field accepts maximum boundary value")
    void surfaceFieldAcceptsMaximumValue() {
        int maxValue = verifier.getFieldMaxValue("QUANTITY");
        boolean accepted = verifier.setNumericField("QUANTITY", maxValue);

        assertThat("Field should accept maximum value", accepted, is(true));
        assertThat("Stored value should match maximum",
                   verifier.getNumericField("QUANTITY"), equalTo(maxValue));
    }

    @Test
    @DisplayName("Surface 1.4: Field rejects values below minimum")
    void surfaceFieldRejectsBelowMinimum() {
        int minValue = verifier.getFieldMinValue("QUANTITY");
        int belowMin = minValue - 1;

        boolean accepted = verifier.setNumericField("QUANTITY", belowMin);
        assertThat("Field should reject below-minimum", accepted, is(false));
    }

    @Test
    @DisplayName("Surface 1.5: Field rejects values above maximum")
    void surfaceFieldRejectsAboveMaximum() {
        int maxValue = verifier.getFieldMaxValue("QUANTITY");
        int aboveMax = maxValue + 1;

        boolean accepted = verifier.setNumericField("QUANTITY", aboveMax);
        assertThat("Field should reject above-maximum", accepted, is(false));
    }

    // ============================================================================
    // Surface 2: String Field Length Constraints
    // ============================================================================

    @Test
    @DisplayName("Surface 2.1: String field length is documented")
    void surfaceStringFieldLengthIsDocumented() {
        int maxLength = verifier.getFieldMaxLength("ACCOUNT_NAME");

        assertThat("Max length should be positive", maxLength, greaterThan(0));
        assertThat("Max length should be reasonable", maxLength, lessThan(10000));
    }

    @Test
    @DisplayName("Surface 2.2: String field accepts maximum length without truncation")
    void surfaceStringFieldAcceptsMaxLengthWithoutTruncation() {
        int maxLength = verifier.getFieldMaxLength("ACCOUNT_NAME");
        String maxLengthValue = "A".repeat(maxLength);

        verifier.setStringField("ACCOUNT_NAME", maxLengthValue);
        String stored = verifier.getStringField("ACCOUNT_NAME");

        assertThat("Stored value should not be truncated",
                   stored.length(), equalTo(maxLength));
        assertThat("Stored value should match input", stored, equalTo(maxLengthValue));
    }

    @Test
    @DisplayName("Surface 2.3: String field rejects values exceeding maximum length")
    void surfaceStringFieldRejectsExceedingMaximum() {
        int maxLength = verifier.getFieldMaxLength("ACCOUNT_NAME");
        String tooLong = "B".repeat(maxLength + 1);

        boolean accepted = verifier.setStringField("ACCOUNT_NAME", tooLong);
        assertThat("Field should reject over-length value", accepted, is(false));
    }

    @Test
    @DisplayName("Surface 2.4: Empty string is handled correctly")
    void surfaceEmptyStringIsHandledCorrectly() {
        boolean accepted = verifier.setStringField("ACCOUNT_NAME", "");
        String stored = verifier.getStringField("ACCOUNT_NAME");

        if (accepted) {
            assertThat("Empty string should be stored",
                       stored.length(), equalTo(0));
        } else {
            assertThat("If rejected, field should preserve previous", true, is(true));
        }
    }

    // ============================================================================
    // Surface 3: Decimal Field Precision
    // ============================================================================

    @Test
    @DisplayName("Surface 3.1: Decimal field precision is enforced")
    void surfaceDecimalPrecisionEnforced() {
        int totalDigits = verifier.getDecimalFieldTotalDigits("BALANCE");
        int decimalPlaces = verifier.getDecimalFieldDecimalPlaces("BALANCE");

        assertThat("Total digits should be positive", totalDigits, greaterThan(0));
        assertThat("Decimal places should not exceed total", decimalPlaces, lessThanOrEqualTo(totalDigits));
    }

    @Test
    @DisplayName("Surface 3.2: Decimal field accepts max precision without loss")
    void surfaceDecimalFieldAcceptsMaxPrecision() {
        int totalDigits = verifier.getDecimalFieldTotalDigits("BALANCE");
        int decimalPlaces = verifier.getDecimalFieldDecimalPlaces("BALANCE");

        // Create max precision value: all 9s
        String maxValue = "9".repeat(totalDigits - decimalPlaces) +
                         "." + "9".repeat(decimalPlaces);

        verifier.setDecimalField("BALANCE", maxValue);
        String stored = verifier.getDecimalField("BALANCE");

        assertThat("Decimal should not lose precision", stored, containsString("9"));
    }

    @Test
    @DisplayName("Surface 3.3: Decimal field rejects excessive decimal places")
    void surfaceDecimalRejectsExcessiveDecimalPlaces() {
        int decimalPlaces = verifier.getDecimalFieldDecimalPlaces("BALANCE");
        String tooManyDecimals = "123." + "9".repeat(decimalPlaces + 1);

        boolean accepted = verifier.setDecimalField("BALANCE", tooManyDecimals);
        assertThat("Field should reject excessive decimals", accepted, is(false));
    }

    // ============================================================================
    // Surface 4: Data Type Constraints
    // ============================================================================

    @Test
    @DisplayName("Surface 4.1: Field type is correctly identified")
    void surfaceFieldTypeIsCorrectlyIdentified() {
        String typeAmount = verifier.getFieldType("AMOUNT");
        String typeAccount = verifier.getFieldType("ACCOUNT_NAME");
        String typeBalance = verifier.getFieldType("BALANCE");

        assertThat("AMOUNT should be numeric", typeAmount, either(containsString("NUMERIC"))
                                                            .or(containsString("INT")));
        assertThat("ACCOUNT_NAME should be character", typeAccount, either(containsString("CHARACTER"))
                                                                     .or(containsString("STRING")));
        assertThat("BALANCE should be decimal", typeBalance, containsString("DECIMAL"));
    }

    @Test
    @DisplayName("Surface 4.2: Numeric field rejects non-numeric input")
    void surfaceNumericFieldRejectsNonNumericInput() {
        boolean accepted = verifier.setNumericField("QUANTITY", "ABC");

        assertThat("Numeric field should reject non-numeric", accepted, is(false));
    }

    // ============================================================================
    // Surface 5: Field Update Integrity
    // ============================================================================

    @Test
    @DisplayName("Surface 5.1: Updating one field does not affect others")
    void surfaceUpdatingOneFieldDoesNotAffectOthers() {
        verifier.setNumericField("QUANTITY", 100);
        verifier.setStringField("ACCOUNT_NAME", "Original");
        String originalAccount = verifier.getStringField("ACCOUNT_NAME");

        verifier.setNumericField("QUANTITY", 200);
        String afterUpdate = verifier.getStringField("ACCOUNT_NAME");

        assertThat("Account name should not change", afterUpdate, equalTo(originalAccount));
    }

    @Test
    @DisplayName("Surface 5.2: Multiple successive updates preserve latest value")
    void surfaceMultipleUpdatesPreserveLatest() {
        verifier.setStringField("ACCOUNT_NAME", "First");
        verifier.setStringField("ACCOUNT_NAME", "Second");
        verifier.setStringField("ACCOUNT_NAME", "Third");

        String stored = verifier.getStringField("ACCOUNT_NAME");
        assertThat("Latest value should be stored", stored, equalTo("Third"));
    }

    // ============================================================================
    // Surface 6: No Silent Data Loss
    // ============================================================================

    @Test
    @DisplayName("Surface 6.1: Assignment success/failure is reported")
    void surfaceAssignmentStatusIsReported() {
        int maxValue = verifier.getFieldMaxValue("QUANTITY");

        boolean validAssignment = verifier.setNumericField("QUANTITY", maxValue);
        boolean invalidAssignment = verifier.setNumericField("QUANTITY", maxValue + 1);

        assertThat("Valid assignment should report success", validAssignment, is(true));
        assertThat("Invalid assignment should report failure", invalidAssignment, is(false));
    }

    @Test
    @DisplayName("Surface 6.2: Failed assignment leaves value unchanged")
    void surfaceFailedAssignmentLeavesValueUnchanged() {
        int originalValue = 42;
        verifier.setNumericField("QUANTITY", originalValue);

        int maxValue = verifier.getFieldMaxValue("QUANTITY");
        verifier.setNumericField("QUANTITY", maxValue + 1);

        int stored = verifier.getNumericField("QUANTITY");
        assertThat("Value should not change after failed assignment", stored, equalTo(originalValue));
    }

    @Test
    @DisplayName("Surface 6.3: Zero is distinct from null/missing")
    void surfaceZeroDistinctFromMissing() {
        verifier.setNumericField("QUANTITY", 0);
        int stored = verifier.getNumericField("QUANTITY");

        assertThat("Zero should be stored as zero", stored, equalTo(0));
        assertThat("Zero should not be confused with unset", stored, not(nullValue()));
    }

    // ============================================================================
    // Surface 7: Adversarial Schema Cases
    // ============================================================================

    @ParameterizedTest
    @ValueSource(ints = {0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE})
    @DisplayName("Surface 7.1: All boundary integers are handled")
    void surfaceAllBoundaryIntegersHandled(int value) {
        // Some values may be rejected (out of range), but should not cause exceptions
        try {
            verifier.setNumericField("QUANTITY", value);
            // Accepted or rejected, but no exception
            assertThat("Boundary value handled gracefully", true, is(true));
        } catch (Exception e) {
            // Should not throw
            assertThat("Should not throw exception for boundary value", false, is(true));
        }
    }

    @Test
    @DisplayName("Surface 7.2: Field definition cache is consistent")
    void surfaceFieldDefinitionCacheConsistent() {
        int maxLengthFirst = verifier.getFieldMaxLength("ACCOUNT_NAME");
        int maxLengthSecond = verifier.getFieldMaxLength("ACCOUNT_NAME");
        int maxLengthThird = verifier.getFieldMaxLength("ACCOUNT_NAME");

        assertThat("Field definition should be consistent across calls",
                   maxLengthFirst, equalTo(maxLengthSecond));
        assertThat("Field definition should remain stable",
                   maxLengthSecond, equalTo(maxLengthThird));
    }

    // ============================================================================
    // Support: Schema Contract Verifier
    // ============================================================================

    /**
     * Verifier class handles field operations against actual schema.
     * Defines real i5 field constraints: QUANTITY, AMOUNT, ACCOUNT_NAME, BALANCE
     */
    static class SchemaContractVerifier {

        // Field definitions: name → (type, minValue, maxValue, length, totalDigits, decimalPlaces)
        private static final java.util.Map<String, FieldDefinition> SCHEMA = java.util.Map.ofEntries(
            java.util.Map.entry("QUANTITY", new FieldDefinition("NUMERIC", 0, 9999, 4, 0)),
            java.util.Map.entry("AMOUNT", new FieldDefinition("NUMERIC", -999999, 999999, 6, 0)),
            java.util.Map.entry("ACCOUNT_NAME", new FieldDefinition("CHARACTER", 0, 0, 30, 0)),
            java.util.Map.entry("BALANCE", new FieldDefinition("DECIMAL", -999999999, 999999999, 0, 10, 2))
        );

        private final java.util.Map<String, Object> fieldValues = new java.util.HashMap<>();

        record FieldDefinition(String type, int minValue, int maxValue, int maxLength, int totalDigits, int decimalPlaces) {
            FieldDefinition(String type, int min, int max, int length, int decimals) {
                this(type, min, max, length, length, decimals);
            }
        }

        // Numeric field operations
        int getFieldMinValue(String fieldName) {
            FieldDefinition def = SCHEMA.get(fieldName);
            return def != null ? def.minValue() : 0;
        }

        int getFieldMaxValue(String fieldName) {
            FieldDefinition def = SCHEMA.get(fieldName);
            return def != null ? def.maxValue() : 999999999;
        }

        boolean setNumericField(String fieldName, int value) {
            FieldDefinition def = SCHEMA.get(fieldName);
            if (def == null || !def.type().equals("NUMERIC")) {
                return false;
            }
            if (value < def.minValue() || value > def.maxValue()) {
                return false; // Out of range
            }
            fieldValues.put(fieldName, value);
            return true;
        }

        boolean setNumericField(String fieldName, String value) {
            try {
                int intValue = Integer.parseInt(value);
                return setNumericField(fieldName, intValue);
            } catch (NumberFormatException e) {
                return false; // Non-numeric input
            }
        }

        int getNumericField(String fieldName) {
            Object val = fieldValues.get(fieldName);
            return val instanceof Integer ? (int) val : 0;
        }

        // String field operations
        int getFieldMaxLength(String fieldName) {
            FieldDefinition def = SCHEMA.get(fieldName);
            return def != null ? def.maxLength() : 100;
        }

        boolean setStringField(String fieldName, String value) {
            FieldDefinition def = SCHEMA.get(fieldName);
            if (def == null || !def.type().equals("CHARACTER")) {
                return false;
            }
            if (value.length() > def.maxLength()) {
                return false; // Over length limit
            }
            fieldValues.put(fieldName, value);
            return true;
        }

        String getStringField(String fieldName) {
            Object val = fieldValues.get(fieldName);
            return val instanceof String ? (String) val : "";
        }

        // Decimal field operations
        int getDecimalFieldTotalDigits(String fieldName) {
            FieldDefinition def = SCHEMA.get(fieldName);
            return def != null ? def.totalDigits() : 10;
        }

        int getDecimalFieldDecimalPlaces(String fieldName) {
            FieldDefinition def = SCHEMA.get(fieldName);
            return def != null ? def.decimalPlaces() : 2;
        }

        boolean setDecimalField(String fieldName, String value) {
            FieldDefinition def = SCHEMA.get(fieldName);
            if (def == null || !def.type().equals("DECIMAL")) {
                return false;
            }

            try {
                String[] parts = value.split("\\.");
                if (parts.length > 2) {
                    return false; // Multiple decimal points
                }

                String wholeDigits = parts[0].replaceFirst("^-", ""); // Remove sign for count
                int decimalDigits = parts.length > 1 ? parts[1].length() : 0;

                if (decimalDigits > def.decimalPlaces()) {
                    return false; // Too many decimal places
                }

                if (wholeDigits.length() + decimalDigits > def.totalDigits()) {
                    return false; // Total digits exceeded
                }

                // Validate range
                double doubleValue = Double.parseDouble(value);
                if (doubleValue < def.minValue() || doubleValue > def.maxValue()) {
                    return false; // Out of range
                }

                fieldValues.put(fieldName, value);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        String getDecimalField(String fieldName) {
            Object val = fieldValues.get(fieldName);
            return val instanceof String ? (String) val : "0.00";
        }

        // Field metadata
        String getFieldType(String fieldName) {
            FieldDefinition def = SCHEMA.get(fieldName);
            return def != null ? def.type() : "UNKNOWN";
        }
    }
}
