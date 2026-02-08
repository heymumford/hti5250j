/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates validation errors and warnings.
 *
 * Errors invalidate the workflow (isValid() = false).
 * Warnings are informational (don't affect validity).
 */
public class ValidationResult {
    private final List<ValidationError> errors = new ArrayList<>();
    private final List<ValidationWarning> warnings = new ArrayList<>();

    /**
     * Returns true if no errors exist (warnings are allowed).
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * Add an error with suggested fix.
     */
    public void addError(int stepIndex, String fieldName, String message, String suggestedFix) {
        errors.add(new ValidationError(stepIndex, fieldName, message, suggestedFix));
    }

    /**
     * Add a warning (no suggested fix).
     */
    public void addWarning(int stepIndex, String fieldName, String message) {
        warnings.add(new ValidationWarning(stepIndex, fieldName, message));
    }

    /**
     * Returns unmodifiable list of errors.
     */
    public List<ValidationError> getErrors() {
        return List.copyOf(errors);
    }

    /**
     * Returns unmodifiable list of warnings.
     */
    public List<ValidationWarning> getWarnings() {
        return List.copyOf(warnings);
    }

    /**
     * Merge another result's errors and warnings into this one.
     */
    public void merge(ValidationResult other) {
        this.errors.addAll(other.errors);
        this.warnings.addAll(other.warnings);
    }
}
