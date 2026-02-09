/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Sealed interface representing a workflow action.
 *
 * Permitted implementations ensure compile-time exhaustiveness checking
 * for action dispatch. Each implementation is immutable and validates
 * constructor arguments for type safety.
 */
public sealed interface Action permits
        LoginAction,
        NavigateAction,
        FillAction,
        SubmitAction,
        AssertAction,
        WaitAction,
        CaptureAction {
}
