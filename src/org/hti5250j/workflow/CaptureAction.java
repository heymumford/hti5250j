/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Immutable capture action with optional screenshot name.
 * If name is null or empty, defaults to "screenshot" at runtime.
 */
public final record CaptureAction(String name) implements Action {
}
