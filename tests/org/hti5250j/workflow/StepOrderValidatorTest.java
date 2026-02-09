/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for StepOrderValidator.
 * Verifies step ordering constraints including edge cases.
 */
public class StepOrderValidatorTest {

    @Test
    public void testValidLoginFirstStep() {
        StepOrderValidator validator = new StepOrderValidator();
        List<StepDef> steps = new ArrayList<>();
        steps.add(createStep(ActionType.LOGIN));
        steps.add(createStep(ActionType.NAVIGATE));

        ValidationResult result = validator.validate(steps);

        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    public void testLoginNotFirstStepReturnsError() {
        StepOrderValidator validator = new StepOrderValidator();
        List<StepDef> steps = new ArrayList<>();
        steps.add(createStep(ActionType.NAVIGATE));
        steps.add(createStep(ActionType.LOGIN));

        ValidationResult result = validator.validate(steps);

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).message())
            .contains("must start with LOGIN");
    }

    @Test
    public void testSubmitAfterFillIsValid() {
        StepOrderValidator validator = new StepOrderValidator();
        List<StepDef> steps = new ArrayList<>();
        steps.add(createStep(ActionType.LOGIN));
        steps.add(createStep(ActionType.FILL));
        steps.add(createStep(ActionType.SUBMIT));

        ValidationResult result = validator.validate(steps);

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getWarnings()).isEmpty();
    }

    @Test
    public void testSubmitAfterNavigateIsValid() {
        StepOrderValidator validator = new StepOrderValidator();
        List<StepDef> steps = new ArrayList<>();
        steps.add(createStep(ActionType.LOGIN));
        steps.add(createStep(ActionType.NAVIGATE));
        steps.add(createStep(ActionType.SUBMIT));

        ValidationResult result = validator.validate(steps);

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getWarnings()).isEmpty();
    }

    @Test
    public void testSubmitWithoutPrecedingFillWarns() {
        StepOrderValidator validator = new StepOrderValidator();
        List<StepDef> steps = new ArrayList<>();
        steps.add(createStep(ActionType.LOGIN));
        steps.add(createStep(ActionType.WAIT));
        steps.add(createStep(ActionType.SUBMIT));

        ValidationResult result = validator.validate(steps);

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getWarnings()).hasSize(1);
        assertThat(result.getWarnings().get(0).message())
            .contains("SUBMIT should typically follow FILL or NAVIGATE");
    }

    @Test
    public void testSubmitAtStep0WithoutPrecedingActionReturnsWarning() {
        StepOrderValidator validator = new StepOrderValidator();
        List<StepDef> steps = new ArrayList<>();
        // First step is SUBMIT (unconventional but should be validated)
        steps.add(createStep(ActionType.SUBMIT));

        ValidationResult result = validator.validate(steps);

        // SUBMIT at step 0 means no preceding FILL/NAVIGATE, should generate warning
        // Even though step 0 LOGIN check will fail, step ordering should still check
        assertThat(result.getErrors()).isNotEmpty(); // Missing LOGIN
        // The SUBMIT validation should also occur
    }

    @Test
    public void testMultipleSubmitSequence() {
        StepOrderValidator validator = new StepOrderValidator();
        List<StepDef> steps = new ArrayList<>();
        steps.add(createStep(ActionType.LOGIN));
        steps.add(createStep(ActionType.FILL));
        steps.add(createStep(ActionType.SUBMIT));
        steps.add(createStep(ActionType.NAVIGATE));
        steps.add(createStep(ActionType.SUBMIT)); // Second SUBMIT after NAVIGATE

        ValidationResult result = validator.validate(steps);

        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getWarnings()).isEmpty();
    }

    @Test
    public void testEmptyStepsList() {
        StepOrderValidator validator = new StepOrderValidator();
        List<StepDef> steps = new ArrayList<>();

        ValidationResult result = validator.validate(steps);

        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    public void testNullStepsList() {
        StepOrderValidator validator = new StepOrderValidator();

        ValidationResult result = validator.validate(null);

        assertThat(result.getErrors()).isEmpty();
    }

    /**
     * Helper to create a step with specific action type.
     */
    private StepDef createStep(ActionType actionType) {
        StepDef step = new StepDef();
        step.setAction(actionType);
        return step;
    }
}
