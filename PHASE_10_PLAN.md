# Phase 10: WorkflowValidator

## Goal
Design and validate workflow specifications before execution to prevent silent failures at runtime.

## Problem Statement
**Current State:** WorkflowRunner executes workflows without pre-validation. Invalid workflows (missing fields, bad timeouts, unresolved parameters) fail silently during execution.

**Risk:** Production workflows fail at runtime without clear error messages. Users can't debug malformed YAML before deployment.

**Solution:** Pre-execution validator that catches schema violations, parameter mismatches, and configuration errors before WorkflowRunner starts.

---

## Key Questions

1. **What should validation check?**
   - Schema compliance (required fields present)
   - Field type correctness (timeouts are numbers, not strings)
   - Parameter references validity (${data.x} must exist in dataset)
   - Action-specific rules (LOGIN requires host/user/password)
   - Timeout bounds (reasonable min/max values)

2. **When should validation run?**
   - After YAML load, before workflow execution
   - Optional: CLI integration (`--validate` flag)
   - Optional: IDE plugin hints

3. **What should validation output?**
   - Pass/Fail status
   - Clear error messages with line numbers (if possible)
   - Warnings vs Errors (warnings: soft constraints, errors: must-fix)

4. **Should validation know about dataset?**
   - No: Validate only workflow structure (not data availability)
   - Yes: Cross-check parameter references against dataset columns

---

## Decisions Made

**Decision 1: Structural + Reference Validation (2-tier approach)**
- **Tier 1 (Structural):** Always runs. Checks schema, types, required fields, timeout bounds.
- **Tier 2 (Reference):** Optional. Checks parameter references (${data.x}) exist in provided dataset.
- **Rationale:** Decouples workflow validation from data validation. Workflows valid independently of data.

**Decision 2: Detailed Error Messages**
- Each validation failure includes: field name, expected type, actual type, suggested fix.
- Collect all errors before reporting (not fail-first).
- **Rationale:** Users see all problems at once, not iteratively.

**Decision 3: Action-Specific Validators**
- Each ActionType (LOGIN, FILL, ASSERT, etc.) has validation rules.
- LOGIN: requires host, user, password.
- FILL: requires fields Map with non-empty values.
- SUBMIT: requires key.
- ASSERT: requires screen or text.
- **Rationale:** Prevents invalid action configurations.

**Decision 4: Timeout Validation**
- Optional field, but if present: 100ms ≤ timeout ≤ 300000ms (5 minutes max).
- **Rationale:** Catches misconfigured timeouts (e.g., 1000000ms = 16+ minutes).

---

## Architecture

```
WorkflowValidator (main class)
├── validate(workflow): ValidationResult
├── validateWithDataset(workflow, dataset): ValidationResult
└── ValidationResult
    ├── isValid: boolean
    ├── errors: List<ValidationError>
    └── warnings: List<ValidationWarning>

ValidationError
├── step: int (step index)
├── field: String (field name)
├── message: String
└── suggestedFix: String

ActionValidator (interface)
├── validateAction(step): List<ValidationError>
└── implementations:
    ├── LoginActionValidator
    ├── FillActionValidator
    ├── AssertActionValidator
    ├── ... (7 total)
```

---

## Implementation Tasks

**Task 1: ValidationResult DTO**
- Record with `isValid`, `errors`, `warnings` fields
- Helper methods: `addError()`, `addWarning()`, `merge()`
- Supports reporting all errors collected

**Task 2: ValidationError & ValidationWarning**
- Records with step index, field name, message, suggested fix
- Enable detailed error reporting with context

**Task 3: WorkflowValidator Core**
- Main `validate(workflow)` method
- Validates workflow structure (non-null name, non-empty steps)
- Delegates step validation to step validators

**Task 4: StepValidator**
- Validates individual step structure
- Checks: action not null, timeout is integer and in bounds
- Delegates action-specific validation to ActionValidator

**Task 5: ActionValidators (7 implementations)**
- LoginActionValidator: host, user, password required
- NavigateActionValidator: screen required
- FillActionValidator: fields Map required, not empty
- SubmitActionValidator: key required
- AssertActionValidator: screen or text required
- WaitActionValidator: timeout > 0
- CaptureActionValidator: name recommended

**Task 6: ParameterValidator (optional)**
- `validateParameters(workflow, dataset)`: checks ${data.x} references exist
- Cross-references step fields against dataset columns
- Generates warnings for missing columns

**Task 7: CLI Integration**
- Add `--validate` flag to WorkflowCLI
- Load workflow + dataset (if provided) and report validation results
- Exit with code 0 (valid) or 1 (invalid)

**Task 8: Integration Tests**
- Valid workflow passes all checks
- Invalid workflows fail with clear errors
- Example workflows (login.yaml, payment.yaml) validate successfully

---

## Success Criteria

✅ All 8 tasks complete with passing tests
✅ Validation catches schema violations (missing fields)
✅ Validation catches type errors (string timeout instead of int)
✅ Validation catches action-specific rule violations
✅ Timeout bounds enforced (100ms - 5min)
✅ Parameter references validated against dataset (optional)
✅ CLI integration working (`i5250 --validate workflow.yaml`)
✅ Example workflows pass validation
✅ No regressions in Phase 9 tests

---

## Status

**Phase 1: Design** - ✅ Complete
- Problem scoped
- Architecture defined
- 4 design decisions made
- 8 tasks identified

**Phase 2: Planning** - ✅ Complete
- Detailed implementation plan created (docs/plans/2026-02-08-phase-10-workflow-validator.md)
- 8 tasks with exact code, test structure, and commit rationale
- Execution ready: batch mode (3 tasks per batch) recommended

**Phase 3: Execution** - Ready to start
