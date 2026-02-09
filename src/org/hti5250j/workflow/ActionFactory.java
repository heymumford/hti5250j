/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Factory for converting YAML-deserialized StepDef into typed Action objects.
 *
 * Provides type-safe conversion with validation. Each StepDef is converted
 * to the corresponding Action type, enabling exhaustive pattern matching
 * and compile-time safety in handlers.
 */
public class ActionFactory {
    /**
     * Convert StepDef to typed Action.
     *
     * @param stepDef YAML-deserialized step definition
     * @return Typed Action object (never null)
     * @throws IllegalArgumentException if StepDef cannot be converted
     * @throws NullPointerException if stepDef is null
     */
    public static Action from(StepDef stepDef) {
        if (stepDef == null) {
            throw new NullPointerException("StepDef cannot be null");
        }

        return switch (stepDef.getAction()) {
            case LOGIN -> new LoginAction(
                stepDef.getHost(),
                stepDef.getUser(),
                stepDef.getPassword()
            );
            case NAVIGATE -> new NavigateAction(
                stepDef.getScreen(),
                stepDef.getKeys()
            );
            case FILL -> new FillAction(
                stepDef.getFields(),
                stepDef.getTimeout()
            );
            case SUBMIT -> new SubmitAction(
                stepDef.getKey()
            );
            case ASSERT -> new AssertAction(
                stepDef.getText(),
                stepDef.getScreen()
            );
            case WAIT -> new WaitAction(
                stepDef.getTimeout()
            );
            case CAPTURE -> new CaptureAction(
                stepDef.getName()
            );
        };
    }

    private ActionFactory() {
        // Utility class, no instantiation
    }
}
