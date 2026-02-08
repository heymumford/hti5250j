# Phase 10: WorkflowValidator Implementation Plan

> **Execution:** Use `/build execute` (batch or subagent mode)

**Goal:** Add pre-execution validation to catch workflow schema violations, type errors, and configuration problems before runtime.

**Architecture:** Three-layer validation stack — (1) DTO layer (ValidationResult, ValidationError), (2) core validator (WorkflowValidator delegates to step/action validators), (3) CLI integration.

**Tech Stack:** Java 21, JUnit 5, SnakeYAML (existing)

---

## Task 1: ValidationResult DTO

**Files:**
- Create: `src/org/hti5250j/workflow/ValidationResult.java`
- Test: `tests/org/hti5250j/workflow/ValidationResultTest.java`

**Step 1: Write failing test**
```java
@Test
void testValidationResultAccumulates() {
    ValidationResult result = new ValidationResult();

    assertThat(result.isValid()).isTrue();

    result.addError("step0", "host", "Host is required");
    assertThat(result.isValid()).isFalse();
    assertThat(result.getErrors()).hasSize(1);

    result.addWarning("step1", "timeout", "Timeout exceeds recommended max");
    assertThat(result.getWarnings()).hasSize(1);
}

@Test
void testMergeResults() {
    ValidationResult result1 = new ValidationResult();
    result1.addError("step0", "action", "Action is null");

    ValidationResult result2 = new ValidationResult();
    result2.addWarning("step1", "timeout", "High timeout value");

    result1.merge(result2);

    assertThat(result1.getErrors()).hasSize(1);
    assertThat(result1.getWarnings()).hasSize(1);
    assertThat(result1.isValid()).isFalse();
}
```

**Step 2: Run, verify failure**
```bash
./gradlew compileTestJava
```
Expected: Cannot find symbol `ValidationResult`

**Step 3: Minimal implementation**
```java
package org.hti5250j.workflow;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
    private final List<ValidationError> errors = new ArrayList<>();
    private final List<ValidationWarning> warnings = new ArrayList<>();

    public boolean isValid() {
        return errors.isEmpty();
    }

    public void addError(String step, String field, String message) {
        errors.add(new ValidationError(step, field, message));
    }

    public void addWarning(String step, String field, String message) {
        warnings.add(new ValidationWarning(step, field, message));
    }

    public List<ValidationError> getErrors() {
        return List.copyOf(errors);
    }

    public List<ValidationWarning> getWarnings() {
        return List.copyOf(warnings);
    }

    public void merge(ValidationResult other) {
        this.errors.addAll(other.errors);
        this.warnings.addAll(other.warnings);
    }

    public static record ValidationError(String step, String field, String message) {}
    public static record ValidationWarning(String step, String field, String message) {}
}
```

**Step 4: Run, verify pass**
```bash
./gradlew test --tests ValidationResultTest
```
Expected: BUILD SUCCESSFUL

**Step 5: Commit with rationale**
```bash
git commit -m "feat(workflow): add ValidationResult DTO (Phase 10 Task 1)

Rationale: Accumulates validation errors and warnings separately. Records
allow immutable data transfer. Merge method enables composite validation."
```

---

## Task 2: ValidationError & ValidationWarning Records

**Files:**
- Create: `src/org/hti5250j/workflow/ValidationError.java`
- Create: `src/org/hti5250j/workflow/ValidationWarning.java`
- Test: `tests/org/hti5250j/workflow/ValidationErrorTest.java`

**Step 1: Write failing test**
```java
@Test
void testValidationErrorWithSuggestedFix() {
    ValidationError error = new ValidationError(
        0, "timeout", "Timeout must be integer, got: hello",
        "Change to numeric value (milliseconds)"
    );

    assertThat(error.stepIndex()).isEqualTo(0);
    assertThat(error.fieldName()).isEqualTo("timeout");
    assertThat(error.message()).contains("integer");
    assertThat(error.suggestedFix()).contains("numeric");
}

@Test
void testValidationWarningNoFix() {
    ValidationWarning warning = new ValidationWarning(
        1, "timeout", "Timeout 60000ms exceeds recommended 30000ms"
    );

    assertThat(warning.stepIndex()).isEqualTo(1);
    assertThat(warning.fieldName()).isEqualTo("timeout");
}
```

**Step 2: Run, verify failure**
```bash
./gradlew compileTestJava
```
Expected: Cannot find symbol `ValidationError`, `ValidationWarning`

**Step 3: Minimal implementation**
```java
// ValidationError.java
package org.hti5250j.workflow;

public record ValidationError(
    int stepIndex,
    String fieldName,
    String message,
    String suggestedFix
) {}

// ValidationWarning.java
package org.hti5250j.workflow;

public record ValidationWarning(
    int stepIndex,
    String fieldName,
    String message
) {}
```

**Step 4: Run, verify pass**
```bash
./gradlew test --tests ValidationErrorTest
```
Expected: BUILD SUCCESSFUL

**Step 5: Commit with rationale**
```bash
git commit -m "feat(workflow): add ValidationError and ValidationWarning records (Phase 10 Task 2)

Rationale: Records provide immutable data transfer with clear semantics.
ValidationError includes suggestedFix for user guidance. ValidationWarning
has simpler structure (no fix needed for warnings)."
```

---

## Task 3: WorkflowValidator Core

**Files:**
- Create: `src/org/hti5250j/workflow/WorkflowValidator.java`
- Test: `tests/org/hti5250j/workflow/WorkflowValidatorTest.java`

**Step 1: Write failing test**
```java
@Test
void testValidateWorkflowStructure() {
    WorkflowSchema workflow = new WorkflowSchema();
    workflow.setName("Test");

    StepDef step = new StepDef();
    step.setAction(ActionType.LOGIN);
    step.setHost("example.com");
    step.setUser("user");
    step.setPassword("pass");
    workflow.setSteps(List.of(step));

    WorkflowValidator validator = new WorkflowValidator();
    ValidationResult result = validator.validate(workflow);

    assertThat(result.isValid()).isTrue();
}

@Test
void testValidateDetectsNullWorkflowName() {
    WorkflowSchema workflow = new WorkflowSchema();
    workflow.setSteps(List.of());

    WorkflowValidator validator = new WorkflowValidator();
    ValidationResult result = validator.validate(workflow);

    assertThat(result.isValid()).isFalse();
    assertThat(result.getErrors()).anySatisfy(e ->
        assertThat(e.message()).contains("name")
    );
}

@Test
void testValidateDetectsEmptySteps() {
    WorkflowSchema workflow = new WorkflowSchema();
    workflow.setName("Test");
    workflow.setSteps(List.of());

    WorkflowValidator validator = new WorkflowValidator();
    ValidationResult result = validator.validate(workflow);

    assertThat(result.isValid()).isFalse();
}
```

**Step 2: Run, verify failure**
```bash
./gradlew compileTestJava
```
Expected: Cannot find symbol `WorkflowValidator`

**Step 3: Minimal implementation**
```java
package org.hti5250j.workflow;

public class WorkflowValidator {

    public ValidationResult validate(WorkflowSchema workflow) {
        ValidationResult result = new ValidationResult();

        // Validate workflow structure
        if (workflow.getName() == null || workflow.getName().isBlank()) {
            result.addError(-1, "name", "Workflow name is required", "Add 'name:' field to YAML");
        }

        if (workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
            result.addError(-1, "steps", "Workflow must have at least one step",
                "Add at least one step to 'steps:' list");
        } else {
            // Validate each step
            for (int i = 0; i < workflow.getSteps().size(); i++) {
                validateStep(workflow.getSteps().get(i), i, result);
            }
        }

        return result;
    }

    private void validateStep(StepDef step, int stepIndex, ValidationResult result) {
        if (step.getAction() == null) {
            result.addError(stepIndex, "action", "Step action is required",
                "Add 'action:' field (LOGIN, NAVIGATE, FILL, etc)");
        }
    }
}
```

**Step 4: Run, verify pass**
```bash
./gradlew test --tests WorkflowValidatorTest
```
Expected: BUILD SUCCESSFUL

**Step 5: Commit with rationale**
```bash
git commit -m "feat(workflow): add WorkflowValidator core (Phase 10 Task 3)

Rationale: Validates workflow-level structure (name, steps) and delegates
step validation. Detects null/blank name and empty steps list. Foundation
for step and action-specific validators."
```

---

## Task 4: StepValidator with Timeout Checking

**Files:**
- Create: `src/org/hti5250j/workflow/StepValidator.java`
- Modify: `src/org/hti5250j/workflow/WorkflowValidator.java` (integrate)
- Test: `tests/org/hti5250j/workflow/StepValidatorTest.java`

**Step 1: Write failing test**
```java
@Test
void testValidateStepTimeout() {
    StepDef step = new StepDef();
    step.setAction(ActionType.WAIT);
    step.setTimeout(5000); // valid: 100-300000

    StepValidator validator = new StepValidator();
    ValidationResult result = validator.validate(step, 0);

    assertThat(result.isValid()).isTrue();
}

@Test
void testValidateDetectsTimeoutTooLow() {
    StepDef step = new StepDef();
    step.setAction(ActionType.WAIT);
    step.setTimeout(50); // too low: < 100

    StepValidator validator = new StepValidator();
    ValidationResult result = validator.validate(step, 0);

    assertThat(result.isValid()).isFalse();
}

@Test
void testValidateDetectsTimeoutTooHigh() {
    StepDef step = new StepDef();
    step.setAction(ActionType.WAIT);
    step.setTimeout(600000); // too high: > 300000

    StepValidator validator = new StepValidator();
    ValidationResult result = validator.validate(step, 0);

    assertThat(result.isValid()).isFalse();
}
```

**Step 2: Run, verify failure**
```bash
./gradlew compileTestJava
```
Expected: Cannot find symbol `StepValidator`

**Step 3: Minimal implementation**
```java
package org.hti5250j.workflow;

public class StepValidator {
    private static final int MIN_TIMEOUT = 100;
    private static final int MAX_TIMEOUT = 300000; // 5 minutes

    public ValidationResult validate(StepDef step, int stepIndex) {
        ValidationResult result = new ValidationResult();

        // Validate timeout if present
        if (step.getTimeout() != null) {
            if (step.getTimeout() < MIN_TIMEOUT) {
                result.addError(stepIndex, "timeout",
                    "Timeout must be >= " + MIN_TIMEOUT + "ms",
                    "Increase timeout to at least " + MIN_TIMEOUT + "ms");
            } else if (step.getTimeout() > MAX_TIMEOUT) {
                result.addError(stepIndex, "timeout",
                    "Timeout must be <= " + MAX_TIMEOUT + "ms (5 minutes)",
                    "Reduce timeout or split into multiple steps");
            }
        }

        return result;
    }
}
```

**Step 4: Run, verify pass**
```bash
./gradlew test --tests StepValidatorTest
```
Expected: BUILD SUCCESSFUL

**Step 5: Commit with rationale**
```bash
git commit -m "feat(workflow): add StepValidator with timeout bounds (Phase 10 Task 4)

Rationale: Validates timeout is within safe bounds (100ms-300000ms). Prevents
misconfiguration like 1000000ms (16+ minutes). Will be extended with
action-specific validators in Task 5."
```

---

## Task 5: ActionValidators (7 implementations)

**Files:**
- Create: `src/org/hti5250j/workflow/ActionValidator.java` (interface)
- Create: `src/org/hti5250j/workflow/validators/LoginActionValidator.java`
- Create: `src/org/hti5250j/workflow/validators/NavigateActionValidator.java`
- Create: `src/org/hti5250j/workflow/validators/FillActionValidator.java`
- Create: `src/org/hti5250j/workflow/validators/SubmitActionValidator.java`
- Create: `src/org/hti5250j/workflow/validators/AssertActionValidator.java`
- Create: `src/org/hti5250j/workflow/validators/WaitActionValidator.java`
- Create: `src/org/hti5250j/workflow/validators/CaptureActionValidator.java`
- Modify: `src/org/hti5250j/workflow/WorkflowValidator.java` (integrate)
- Test: `tests/org/hti5250j/workflow/ActionValidatorTest.java`

**Step 1: Write failing test**
```java
@Test
void testLoginActionValidation() {
    StepDef step = new StepDef();
    step.setAction(ActionType.LOGIN);
    step.setHost("example.com");
    step.setUser("user");
    step.setPassword("pass");

    LoginActionValidator validator = new LoginActionValidator();
    ValidationResult result = validator.validate(step, 0);

    assertThat(result.isValid()).isTrue();
}

@Test
void testLoginActionRequiresHost() {
    StepDef step = new StepDef();
    step.setAction(ActionType.LOGIN);
    // no host
    step.setUser("user");
    step.setPassword("pass");

    LoginActionValidator validator = new LoginActionValidator();
    ValidationResult result = validator.validate(step, 0);

    assertThat(result.isValid()).isFalse();
}

@Test
void testFillActionRequiresFields() {
    StepDef step = new StepDef();
    step.setAction(ActionType.FILL);
    // no fields

    FillActionValidator validator = new FillActionValidator();
    ValidationResult result = validator.validate(step, 0);

    assertThat(result.isValid()).isFalse();
}

@Test
void testAssertActionRequiresScreenOrText() {
    StepDef step = new StepDef();
    step.setAction(ActionType.ASSERT);
    step.setScreen("confirmation");
    // has screen, should pass

    AssertActionValidator validator = new AssertActionValidator();
    ValidationResult result = validator.validate(step, 0);

    assertThat(result.isValid()).isTrue();
}
```

**Step 2: Run, verify failure**
```bash
./gradlew compileTestJava
```
Expected: Cannot find symbol for all validators

**Step 3: Minimal implementation**
```java
// ActionValidator.java
package org.hti5250j.workflow;

public interface ActionValidator {
    ValidationResult validate(StepDef step, int stepIndex);
}

// LoginActionValidator.java
package org.hti5250j.workflow.validators;

public class LoginActionValidator implements ActionValidator {
    @Override
    public ValidationResult validate(StepDef step, int stepIndex) {
        ValidationResult result = new ValidationResult();

        if (step.getHost() == null || step.getHost().isBlank()) {
            result.addError(stepIndex, "host", "LOGIN requires host", "Add 'host:' field");
        }
        if (step.getUser() == null || step.getUser().isBlank()) {
            result.addError(stepIndex, "user", "LOGIN requires user", "Add 'user:' field");
        }
        if (step.getPassword() == null || step.getPassword().isBlank()) {
            result.addError(stepIndex, "password", "LOGIN requires password", "Add 'password:' field");
        }

        return result;
    }
}

// FillActionValidator.java
package org.hti5250j.workflow.validators;

public class FillActionValidator implements ActionValidator {
    @Override
    public ValidationResult validate(StepDef step, int stepIndex) {
        ValidationResult result = new ValidationResult();

        if (step.getFields() == null || step.getFields().isEmpty()) {
            result.addError(stepIndex, "fields", "FILL requires fields map",
                "Add 'fields:' section with field names and values");
        }

        return result;
    }
}

// ... (5 more similar implementations)
// NavigateActionValidator: requires screen
// SubmitActionValidator: requires key
// AssertActionValidator: requires screen OR text
// WaitActionValidator: optional timeout (validated in StepValidator)
// CaptureActionValidator: optional name (generates warning if missing)
```

**Step 4: Run, verify pass**
```bash
./gradlew test --tests ActionValidatorTest
```
Expected: BUILD SUCCESSFUL

**Step 5: Commit with rationale**
```bash
git commit -m "feat(workflow): add 7 ActionValidators (Phase 10 Task 5)

Rationale: Each action type has specific validation rules (LOGIN requires
host/user/password, FILL requires fields, ASSERT requires screen or text).
Integrates with WorkflowValidator to provide complete step validation."
```

---

## Task 6: Integration into WorkflowValidator

**Files:**
- Modify: `src/org/hti5250j/workflow/WorkflowValidator.java`
- Test: `tests/org/hti5250j/workflow/WorkflowValidatorIntegrationTest.java`

**Step 1: Write failing test**
```java
@Test
void testCompleteValidationOfValidWorkflow() {
    WorkflowSchema workflow = new WorkflowSchema();
    workflow.setName("Payment Flow");

    StepDef loginStep = new StepDef();
    loginStep.setAction(ActionType.LOGIN);
    loginStep.setHost("i5.example.com");
    loginStep.setUser("user");
    loginStep.setPassword("pass");

    StepDef fillStep = new StepDef();
    fillStep.setAction(ActionType.FILL);
    fillStep.setFields(Map.of("account", "ACC-123", "amount", "500.00"));

    workflow.setSteps(List.of(loginStep, fillStep));

    WorkflowValidator validator = new WorkflowValidator();
    ValidationResult result = validator.validate(workflow);

    assertThat(result.isValid()).isTrue();
}

@Test
void testDetectsMultipleErrors() {
    WorkflowSchema workflow = new WorkflowSchema();
    // no name

    StepDef step = new StepDef();
    step.setAction(ActionType.LOGIN);
    // missing host, user, password

    workflow.setSteps(List.of(step));

    WorkflowValidator validator = new WorkflowValidator();
    ValidationResult result = validator.validate(workflow);

    assertThat(result.isValid()).isFalse();
    assertThat(result.getErrors()).hasSizeGreaterThanOrEqualTo(4); // name + host + user + password
}
```

**Step 2: Run, verify failure**
```bash
./gradlew test --tests WorkflowValidatorIntegrationTest
```
Expected: ActionValidator integration missing

**Step 3: Minimal implementation**
Update `WorkflowValidator.validateStep()` to delegate to action validators:
```java
private void validateStep(StepDef step, int stepIndex, ValidationResult result) {
    if (step.getAction() == null) {
        result.addError(stepIndex, "action", "Step action is required",
            "Add 'action:' field");
        return;
    }

    // Validate step-level constraints (timeout)
    StepValidator stepValidator = new StepValidator();
    result.merge(stepValidator.validate(step, stepIndex));

    // Delegate to action-specific validator
    ActionValidator actionValidator = createActionValidator(step.getAction());
    if (actionValidator != null) {
        result.merge(actionValidator.validate(step, stepIndex));
    }
}

private ActionValidator createActionValidator(ActionType action) {
    return switch (action) {
        case LOGIN -> new LoginActionValidator();
        case NAVIGATE -> new NavigateActionValidator();
        case FILL -> new FillActionValidator();
        case SUBMIT -> new SubmitActionValidator();
        case ASSERT -> new AssertActionValidator();
        case WAIT -> new WaitActionValidator();
        case CAPTURE -> new CaptureActionValidator();
    };
}
```

**Step 4: Run, verify pass**
```bash
./gradlew test --tests WorkflowValidatorIntegrationTest
```
Expected: BUILD SUCCESSFUL

**Step 5: Commit with rationale**
```bash
git commit -m "feat(workflow): integrate ActionValidators into WorkflowValidator (Phase 10 Task 6)

Rationale: WorkflowValidator now orchestrates full validation pipeline:
step structure + timeout bounds + action-specific rules. Factory method
creates appropriate validator for each action type. Validation results merged
to accumulate all errors."
```

---

## Task 7: CLI Integration

**Files:**
- Modify: `src/org/hti5250j/workflow/WorkflowCLI.java`
- Test: `tests/org/hti5250j/workflow/WorkflowCLIValidationTest.java`

**Step 1: Write failing test**
```java
@Test
void testValidateFlagInvokesValidator() {
    String[] args = {"validate", "examples/login.yaml"};
    // Should not throw, should invoke validator
}

@Test
void testValidateWithDatasetFile() {
    String[] args = {"validate", "examples/payment.yaml", "--data", "examples/data.csv"};
    // Should validate both workflow and parameter references
}
```

**Step 2: Run, verify failure**
```bash
./gradlew compileTestJava
```
Expected: No validate action in WorkflowCLI

**Step 3: Minimal implementation**
Add to `WorkflowCLI.main()`:
```java
if ("validate".equals(parsed.action())) {
    WorkflowSchema workflow = loadWorkflow(workflowFile);
    WorkflowValidator validator = new WorkflowValidator();
    ValidationResult result = validator.validate(workflow);

    if (result.isValid()) {
        System.out.println("✓ Workflow is valid");
    } else {
        System.out.println("✗ Validation failed:");
        for (ValidationError error : result.getErrors()) {
            System.out.println("  Step " + error.stepIndex() + ", field '" +
                error.fieldName() + "': " + error.message());
            System.out.println("    → " + error.suggestedFix());
        }
        System.exit(1);
    }
}
```

**Step 4: Run, verify pass**
```bash
./gradlew test --tests WorkflowCLIValidationTest
```
Expected: BUILD SUCCESSFUL

**Step 5: Commit with rationale**
```bash
git commit -m "feat(workflow): add --validate flag to WorkflowCLI (Phase 10 Task 7)

Rationale: Users can validate workflows before execution: i5250 validate workflow.yaml
Displays detailed error messages with suggested fixes. Exits with code 1 on
validation failure for scripting integration."
```

---

## Task 8: Integration Tests & Example Validation

**Files:**
- Create: `tests/org/hti5250j/workflow/ExampleWorkflowValidationTest.java`
- Test: Verify examples/login.yaml and examples/payment.yaml validate successfully

**Step 1: Write failing test**
```java
@Test
void testLoginWorkflowIsValid() throws Exception {
    WorkflowSchema workflow = WorkflowCLI.loadWorkflow(new File("examples/login.yaml"));
    WorkflowValidator validator = new WorkflowValidator();
    ValidationResult result = validator.validate(workflow);

    assertThat(result.isValid()).isTrue();
}

@Test
void testPaymentWorkflowIsValid() throws Exception {
    WorkflowSchema workflow = WorkflowCLI.loadWorkflow(new File("examples/payment.yaml"));
    WorkflowValidator validator = new WorkflowValidator();
    ValidationResult result = validator.validate(workflow);

    assertThat(result.isValid()).isTrue();
}

@Test
void testInvalidWorkflowDetected() {
    WorkflowSchema workflow = new WorkflowSchema();
    workflow.setName("Bad Workflow");

    StepDef step = new StepDef();
    step.setAction(ActionType.FILL);
    // missing fields

    workflow.setSteps(List.of(step));

    WorkflowValidator validator = new WorkflowValidator();
    ValidationResult result = validator.validate(workflow);

    assertThat(result.isValid()).isFalse();
}
```

**Step 2: Run, verify failure**
```bash
./gradlew test --tests ExampleWorkflowValidationTest
```
Expected: Validation fails on invalid workflow

**Step 3: No implementation needed - tests should pass**

**Step 4: Run, verify pass**
```bash
./gradlew test --tests ExampleWorkflowValidationTest
```
Expected: BUILD SUCCESSFUL

**Step 5: Commit with rationale**
```bash
git commit -m "feat(workflow): add validation integration tests (Phase 10 Task 8)

Rationale: Validates that example workflows (login.yaml, payment.yaml) pass
all validation checks. Tests invalid workflows are properly detected. Final
verification that Phase 10 validation system is complete and working."
```

---

## Execution Notes

- **Batch Mode Recommended:** 8 tasks, execute 3 per batch (Tasks 1-3, 4-6, 7-8)
- **Model:** haiku for tdd-builder (execution phase)
- **Parallel Execution:** None (sequential dependency)
- **Risk:** Low - validation is isolated, no side effects
- **Rollback:** Simple - git reset to before Phase 10 if needed

