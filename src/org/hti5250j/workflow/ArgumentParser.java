/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

/**
 * Immutable command-line arguments record.
 */
public record ArgumentParser(
    String action,
    String workflowFile,
    String dataFile,
    String environment
) {
    /**
     * Parse command-line arguments.
     * Usage: i5250 run <workflow.yaml> [--data <data.csv>] [--env <environment>]
     *        i5250 validate <workflow.yaml> [--data <data.csv>]
     *        i5250 simulate <workflow.yaml> [--data <data.csv>]
     */
    public static ArgumentParser parse(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: i5250 run|validate|simulate <workflow.yaml> [--data <data.csv>] [--env <environment>]");
        }

        String action = args[0];
        String workflowFile = args[1];
        String dataFile = null;
        String environment = null;

        for (int i = 2; i < args.length; i++) {
            if ("--data".equals(args[i]) && i + 1 < args.length) {
                dataFile = args[++i];
            } else if ("--env".equals(args[i]) && i + 1 < args.length) {
                environment = args[++i];
            }
        }

        return new ArgumentParser(action, workflowFile, dataFile, environment);
    }

    /**
     * Validate that action is recognized.
     */
    public void validate() {
        if (!"run".equals(action) && !"validate".equals(action) && !"simulate".equals(action)) {
            throw new IllegalArgumentException("Unknown action: " + action + ". Use 'run', 'validate', or 'simulate'.");
        }
    }
}
