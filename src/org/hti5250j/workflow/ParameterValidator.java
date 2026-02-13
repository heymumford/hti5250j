/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates parameter references (${data.x}) exist in dataset.
 *
 * Checks workflow steps for ${data.fieldName} patterns and verifies
 * the field exists in the provided dataset.
 */
public class ParameterValidator {
    private static final Pattern PARAM_PATTERN = Pattern.compile("\\$\\{data\\.([^}]+)\\}");

    /**
     * Validate all parameter references in workflow against dataset.
     *
     * @param workflow The workflow to validate
     * @param dataset Map with column names (keys)
     * @return ValidationResult with warnings for missing columns
     */
    public ValidationResult validate(WorkflowSchema workflow, Map<String, ?> dataset) {
        ValidationResult result = new ValidationResult();

        if (workflow == null || workflow.getSteps() == null) {
            return result;
        }

        Set<String> datasetColumns = dataset != null ? dataset.keySet() : Set.of();

        for (int i = 0; i < workflow.getSteps().size(); i++) {
            StepDef step = workflow.getSteps().get(i);
            validateStepParameters(step, i, datasetColumns, result);
        }

        return result;
    }

    /**
     * Extract and validate parameters in a step.
     */
    private void validateStepParameters(StepDef step, int stepIndex,
                                        Set<String> datasetColumns, ValidationResult result) {
        List<String> references = extractParameterReferences(step);

        for (String ref : references) {
            if (!datasetColumns.contains(ref)) {
                result.addWarning(stepIndex, "parameter",
                    "Parameter ${data." + ref + "} not found in dataset");
            }
        }
    }

    /**
     * Extract all ${data.x} references from step fields.
     */
    private List<String> extractParameterReferences(StepDef step) {
        List<String> references = new ArrayList<>();

        if (step == null) {
            return references;
        }

        checkField(step.getHost(), references);
        checkField(step.getUser(), references);
        checkField(step.getPassword(), references);
        checkField(step.getScreen(), references);
        checkField(step.getKey(), references);
        checkField(step.getText(), references);
        checkField(step.getName(), references);

        if (step.getFields() != null) {
            for (String value : step.getFields().values()) {
                checkField(value, references);
            }
        }

        return references;
    }

    /**
     * Extract ${data.x} references from a string field.
     */
    private void checkField(String field, List<String> references) {
        if (field == null) {
            return;
        }

        Matcher matcher = PARAM_PATTERN.matcher(field);
        while (matcher.find()) {
            references.add(matcher.group(1));
        }
    }
}
