/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.yaml.snakeyaml.Yaml;
import java.io.File;
import java.io.FileReader;

/**
 * Loads workflow definitions from YAML files.
 * Encapsulates YAML parsing and file validation logic.
 */
public class WorkflowLoader {

    /**
     * Load workflow from YAML file.
     *
     * @param workflowFile the YAML file to load
     * @return parsed WorkflowSchema
     * @throws Exception if file not found or parsing fails
     */
    public static WorkflowSchema load(File workflowFile) throws Exception {
        if (workflowFile == null) {
            throw new IllegalArgumentException("Workflow file cannot be null");
        }

        if (!workflowFile.exists()) {
            throw new IllegalArgumentException("Workflow file not found: " + workflowFile.getAbsolutePath());
        }

        if (!workflowFile.isFile()) {
            throw new IllegalArgumentException("Workflow path is not a file: " + workflowFile.getAbsolutePath());
        }

        try (FileReader reader = new FileReader(workflowFile)) {
            Yaml yaml = WorkflowYAML.getInstance();
            WorkflowSchema workflow = yaml.loadAs(reader, WorkflowSchema.class);

            if (workflow == null) {
                throw new IllegalArgumentException("Workflow file is empty or invalid: " + workflowFile.getAbsolutePath());
            }

            return workflow;
        }
    }

    /**
     * Load workflow from file path string.
     *
     * @param path the file path to load
     * @return parsed WorkflowSchema
     * @throws Exception if file not found or parsing fails
     */
    public static WorkflowSchema load(String path) throws Exception {
        return load(new File(path));
    }
}
