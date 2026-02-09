# Phase 12D: Sealed Classes for Type Safety

**Phase:** 12D — Java 21 Modernization (Sealed Classes)
**Status:** PENDING — Ready to execute
**Date Created:** 2026-02-08
**Java Version:** 21.0.10 LTS (Temurin) ✓

---

## Phase 12C Conclusion (Fowler's Principles Applied)

### Status: COMPLETE ✓

**Why Phase 12C is done:**

1. **High-value Records already delivered (Phase 12A):**
   - ✅ `ValidationError` record (5 lines) — validation results immutable
   - ✅ `ValidationWarning` record (5 lines) — validation results immutable
   - ✅ `ArgumentParser` record (22 lines) — CLI args immutable

2. **Low-value Records blocked (StepDef, WorkflowSchema):**
   - `WorkflowSchema` (22 lines) — 11 lines boilerplate
   - `StepDef` (57 lines) — 30 lines boilerplate
   - **Cost to convert:** 100+ lines custom SnakeYAML deserializer
   - **Fowler's verdict:** YAGNI — "You aren't gonna need" custom serialization for 41 lines

3. **Fowler's Evolutionary Design principle:**
   - SnakeYAML works (proven, tests pass)
   - POJOs are simple (no complex logic mixed with data)
   - Boilerplate is minimal
   - Refactor when pain points emerge, not preemptively

**Rationale:** Phase 12C was always meant to identify "records where possible." We found them (Phase 12A). The two candidates require external library changes that don't justify the payoff. This is pragmatism, not procrastination.

---

## Phase 12D: Sealed Classes for Type Safety

### Goal

Implement Java 21 sealed classes for compile-time type safety and exhaustive pattern matching. This provides **higher ROI than Records** because:

1. **Compile-time enforcement** — Prevent invalid ActionType states at compile time, not runtime
2. **No library conflicts** — Pure Java 17+ feature, no YAML/serialization concerns
3. **Exhaustive switch expressions** — Compiler prevents missing action handler cases
4. **Better IDE support** — Autocomplete for permitted types

### Current State

```
ActionType (enum)
  ├── LOGIN
  ├── NAVIGATE
  ├── FILL
  ├── SUBMIT
  ├── ASSERT
  ├── WAIT
  └── CAPTURE

ActionValidator (interface)
  ├── LoginActionValidator
  ├── NavigateActionValidator
  ├── FillActionValidator
  ├── SubmitActionValidator
  ├── AssertActionValidator
  ├── WaitActionValidator
  └── CaptureActionValidator

WorkflowRunner (handler dispatch)
  ├── handleLogin()
  ├── handleNavigate()
  ├── handleFill()
  ├── handleSubmit()
  ├── handleAssert()
  ├── handleWait()
  └── handleCapture()
```

### Problem: Untyped Dispatch

Current implementation uses untyped string dispatch:

```java
// WorkflowRunner.java (Phase 11)
switch (step.getAction().toString()) {
    case "LOGIN" -> handleLogin(step, session);
    case "NAVIGATE" -> handleNavigate(step, session);
    // ... but if we add a new ActionType, COMPILER DOESN'T WARN US
    // Silent bug: handler missing for new action type
}
```

**Risk:** Add ActionType.RETRY, forget handleRetry() method → runtime failure in production.

### Solution: Sealed Classes + Exhaustive Switching

```java
// Phase 12D approach:
sealed interface Action permits LoginAction, NavigateAction, FillAction, ... {
    String type();
}

final record LoginAction(String host, String user, String password) implements Action { ... }
final record NavigateAction(String screen, String keys) implements Action { ... }
// ... etc

// WorkflowRunner dispatch:
public void execute(Action action) {
    switch (action) {
        case LoginAction login -> handleLogin(login);
        case NavigateAction nav -> handleNavigate(nav);
        // Compiler: ERROR if you forget a case!
    }
}
```

**Benefits:**
- ✅ Compiler enforces all ActionType cases are handled
- ✅ New action types cause compilation errors (not runtime bugs)
- ✅ Pattern matching extracts fields directly (no casting)
- ✅ Type-safe down-casting (LoginAction vs generic StepDef)

---

## Implementation Plan

### Task 1: Analyze Current ActionType Usage

**Scope:** Identify all places ActionType is used, understand dispatch logic

**Files to check:**
- `src/org/hti5250j/workflow/StepDef.java` — ActionType field holder
- `src/org/hti5250j/workflow/WorkflowRunner.java` — Handler dispatch logic
- `src/org/hti5250j/workflow/WorkflowValidator.java` — Validation switch
- `tests/org/hti5250j/workflow/WorkflowHandlerTest.java` — Test dispatch patterns
- `tests/org/hti5250j/workflow/WorkflowExecutionIntegrationTest.java` — Integration patterns

**Questions to answer:**
1. How many places dispatch on ActionType?
2. How many places validate ActionType?
3. Are there any direct field accesses (e.g., `step.getHost()` in FILL handler)?
4. Do any handlers need ALL fields, or just action-specific subset?

**Deliverable:** Dispatch map showing:
- Dispatch points (where switch/if-else happens)
- Which fields each handler needs
- Test coverage for each action type

---

### Task 2: Design Sealed Action Hierarchy

**Scope:** Create sealed interface with 7 final implementations (one per ActionType)

**Design decision:** Two options:

**Option A: Sealed Interface + Records (Recommended)**
```java
sealed interface Action permits LoginAction, NavigateAction, ... {
    default void validate(ValidationResult result) { }
}

final record LoginAction(String host, String user, String password)
    implements Action { }
final record NavigateAction(String screen, String keys)
    implements Action { }
// ... etc (7 total)
```

**Pros:**
- Clean separation: Each action is immutable record with only fields it needs
- Validation can be method override (LoginAction.validate() vs NavigateAction.validate())
- Type-safe casting in handlers (no instanceof checks)

**Cons:**
- Changes public API (migration required from StepDef → Action)
- Requires new factory: ActionFactory.fromStepDef(StepDef step)

**Option B: Sealed Enum Enhancement**
```java
sealed interface ActionType permits LOGIN, NAVIGATE, FILL, ... {
    int fieldCount();
    String[] fieldNames();
}
// Keep ActionType enum, add sealed contract
```

**Pros:**
- Minimal migration (ActionType already exists)
- Faster rollout (don't change StepDef API)

**Cons:**
- Doesn't add type safety (still untyped StepDef fields)
- Pattern matching doesn't help

**Recommendation:** **Option A** — Higher safety value, justify migration cost.

---

### Task 3: Create Sealed Action Hierarchy

**Files to create:**
1. `Action.java` — Sealed interface (10 lines)
2. `LoginAction.java` — Record for LOGIN action (8 lines)
3. `NavigateAction.java` — Record for NAVIGATE action (8 lines)
4. `FillAction.java` — Record for FILL action (8 lines)
5. `SubmitAction.java` — Record for SUBMIT action (8 lines)
6. `AssertAction.java` — Record for ASSERT action (8 lines)
7. `WaitAction.java` — Record for WAIT action (8 lines)
8. `CaptureAction.java` — Record for CAPTURE action (8 lines)

**Total new code:** ~76 lines (records are compact)

**Each record includes:**
- Exact fields needed for that action
- Constructor validation (non-null checks)
- Optional validate() method override

**Example:**
```java
// LoginAction.java
public final record LoginAction(String host, String user, String password)
    implements Action {
    public LoginAction {
        if (host == null || host.isEmpty())
            throw new IllegalArgumentException("host required");
        if (user == null || user.isEmpty())
            throw new IllegalArgumentException("user required");
        if (password == null || password.isEmpty())
            throw new IllegalArgumentException("password required");
    }
}
```

---

### Task 4: Create Action Factory

**File:** `ActionFactory.java` (NEW)

**Purpose:** Convert StepDef (YAML-deserialized) → Action (type-safe)

**Methods:**
```java
public static Action from(StepDef step) {
    return switch (step.getAction()) {
        case LOGIN -> new LoginAction(step.getHost(), step.getUser(), step.getPassword());
        case NAVIGATE -> new NavigateAction(step.getScreen(), step.getKeys());
        case FILL -> new FillAction(step.getFields(), step.getTimeout());
        case SUBMIT -> new SubmitAction(step.getKey());
        case ASSERT -> new AssertAction(step.getText(), step.getScreen());
        case WAIT -> new WaitAction(step.getTimeout());
        case CAPTURE -> new CaptureAction(step.getName());
    };
}
```

**Size:** ~40 lines

---

### Task 5: Refactor WorkflowRunner Handler Dispatch

**File:** `WorkflowRunner.java` (MODIFIED)

**Current code (Phase 11):**
```java
private void executeStep(StepDef step, Session5250 session) {
    switch (step.getAction()) {
        case LOGIN -> handleLogin(step, session);
        case NAVIGATE -> handleNavigate(step, session);
        // ... but step contains ALL fields, handler only needs 3
    }
}
```

**New code (Phase 12D):**
```java
private void executeStep(StepDef stepDef, Session5250 session) {
    Action action = ActionFactory.from(stepDef);  // Convert to type-safe

    switch (action) {
        case LoginAction login ->
            handleLogin(login, session);  // login.host(), login.user(), login.password()
        case NavigateAction nav ->
            handleNavigate(nav, session);  // nav.screen(), nav.keys()
        case FillAction fill ->
            handleFill(fill, session);    // fill.fields(), fill.timeout()
        // ... compiler ERROR if we forget a case!
    }
}
```

**Benefits:**
- ✅ Compiler enforces exhaustiveness
- ✅ Handler methods have clear parameter types (LoginAction not StepDef)
- ✅ Eliminates null checks (records validate in constructor)
- ✅ Pattern matching extracts fields directly

**Impact on handlers:**
```java
// BEFORE (Phase 11):
private void handleLogin(StepDef step, Session5250 session) {
    String host = step.getHost();      // null-safe? only if we trust YAML
    String user = step.getUser();      // potential null
    String password = step.getPassword(); // potential null
}

// AFTER (Phase 12D):
private void handleLogin(LoginAction login, Session5250 session) {
    String host = login.host();        // null-safe: record constructor validates
    String user = login.user();        // never null
    String password = login.password(); // never null
}
```

---

### Task 6: Update Handler Method Signatures

**Files:** `WorkflowRunner.java`

**Changes:**
- `handleLogin(LoginAction, Session5250)` instead of `handleLogin(StepDef, Session5250)`
- `handleNavigate(NavigateAction, Session5250)` instead of `handleNavigate(StepDef, Session5250)`
- Similar for all 7 handlers

**Impact:** ~35 lines of handler signature updates, ~0 lines of logic changes

---

### Task 7: Refactor WorkflowValidator to Use Sealed Actions

**File:** `WorkflowValidator.java` (MODIFIED)

**Current code (Phase 10):**
```java
public ValidationResult validate(StepDef step, int stepIndex) {
    ActionValidator validator = switch (step.getAction()) {
        case LOGIN -> new LoginActionValidator();
        case NAVIGATE -> new NavigateActionValidator();
        // ...
    };
    return validator.validate(step, stepIndex);
}
```

**New code (Phase 12D):**
```java
public ValidationResult validate(StepDef stepDef, int stepIndex) {
    // First validate that StepDef can convert to Action
    Action action = ActionFactory.from(stepDef);

    // Then dispatch validator (still interface-based, but now exhaustive)
    ActionValidator validator = switch (action) {
        case LoginAction login -> new LoginActionValidator();
        case NavigateAction nav -> new NavigateActionValidator();
        // ...
    };
    return validator.validate(action, stepIndex);  // Pass typed action
}
```

**Benefit:** Validator methods can be overloaded per action type:
```java
// ActionValidator interface
public interface ActionValidator {
    ValidationResult validate(Action action, int stepIndex);
}

// LoginActionValidator
public class LoginActionValidator implements ActionValidator {
    public ValidationResult validate(Action action, int stepIndex) {
        if (action instanceof LoginAction login) {
            // Can now validate login.host(), login.user(), login.password()
            // Compiler knows these exist, not null-safe yet but declared
        }
    }
}
```

---

### Task 8: Update Tests

**Files to update:**
1. `WorkflowHandlerTest.java` — Mock LoginAction, NavigateAction, etc. instead of StepDef
2. `WorkflowExecutionIntegrationTest.java` — Create actions via ActionFactory
3. `WorkflowValidationIntegrationTest.java` — Validate StepDef → Action conversion

**Example:**
```java
// BEFORE:
StepDef step = new StepDef();
step.setAction(ActionType.LOGIN);
step.setHost("host");
step.setUser("user");
step.setPassword("pass");

// AFTER:
Action action = new LoginAction("host", "user", "pass");
```

**New test class:** `ActionFactoryTest.java`
- Test each ActionType → Action conversion
- Test that invalid StepDef throws exception in factory
- Test that Action constructor validates (non-null, non-empty)

---

## Risk Analysis

| Risk | Impact | Mitigation |
|------|--------|-----------|
| ActionFactory exceptions | Workflow halts if StepDef → Action fails | Add try-catch in executeStep(), log error |
| Test coverage gaps | Some ActionType paths untested | Expand ActionFactoryTest to cover all 7 types |
| Handler signature changes | Compilation errors in tests | Batch update all test files in one commit |
| YAML deserialization unchanged | StepDef still has setters | Keep StepDef as-is, convert only at runtime via ActionFactory |

---

## Success Criteria

✅ **Phase 12D is complete when:**

1. **Compilation**: All 128 source files + 149 test files compile with 0 errors
2. **Sealed enforcement**: Compiler rejects switch statements that miss an Action case
3. **Test coverage**: 100% of ActionType enum values have corresponding Action + handler test
4. **Zero regressions**: 12,921 tests still pass (Phase 12A baseline maintained)
5. **Type safety**: No instanceof checks in WorkflowRunner (pure pattern matching)
6. **Documentation**: Update CODING_STANDARDS.md with sealed classes pattern

---

## Commits

### Commit 1: Create Sealed Action Hierarchy (8 files)
```
feat(phase-12d): implement sealed Action interface with 7 final records

- Create sealed interface Action with 7 permitted implementations
- Implement LoginAction, NavigateAction, FillAction, SubmitAction,
  AssertAction, WaitAction, CaptureAction as final records
- Each record includes constructor validation (non-null fields)
- Enables compile-time exhaustiveness checking for action dispatch

Files:
+ Action.java (10 lines)
+ LoginAction.java (8 lines)
+ NavigateAction.java (8 lines)
+ FillAction.java (8 lines)
+ SubmitAction.java (8 lines)
+ AssertAction.java (8 lines)
+ WaitAction.java (8 lines)
+ CaptureAction.java (8 lines)
```

### Commit 2: Create ActionFactory (1 file)
```
feat(phase-12d): implement ActionFactory for type-safe conversion

- Create ActionFactory.from(StepDef) to convert YAML-deserialized StepDef
  into typed Action objects
- Handles all 7 ActionType cases
- Throws exception if StepDef cannot convert (invalid data)
- Enables client code to work with typed Action instead of generic StepDef

Files:
+ ActionFactory.java (40 lines)
```

### Commit 3: Refactor WorkflowRunner for Pattern Matching (1 file)
```
refactor(phase-12d): use sealed actions for type-safe handler dispatch

- Update executeStep() to convert StepDef → Action via ActionFactory
- Replace switch(step.getAction()) with switch(action) for exhaustiveness
- Update all 7 handler methods to accept typed Action parameter
- Remove instanceof checks, use pattern matching directly
- Improves type safety: compiler enforces all ActionType cases handled

Files:
~ WorkflowRunner.java (handlers use pattern matching, +35 lines signature updates)
```

### Commit 4: Update Tests (3-4 files)
```
test(phase-12d): update workflow tests for sealed Action types

- WorkflowHandlerTest: Create typed actions instead of StepDef
- WorkflowExecutionIntegrationTest: Use ActionFactory.from() in integration tests
- ActionFactoryTest (NEW): Test each StepDef → Action conversion
- Verify 100% of 7 action types covered in tests

Files:
~ WorkflowHandlerTest.java
~ WorkflowExecutionIntegrationTest.java
+ ActionFactoryTest.java
```

### Commit 5: Update Validator and Cleanup (2 files)
```
refactor(phase-12d): align validator dispatch with sealed actions

- Update WorkflowValidator to dispatch on Action type instead of ActionType enum
- ActionValidator.validate() signature remains interface contract
- Add exhaustiveness check to validator dispatch
- Update CODING_STANDARDS.md with sealed classes pattern

Files:
~ WorkflowValidator.java
~ CODING_STANDARDS.md
```

---

## Execution Timeline

| Task | Duration | Dependencies |
|------|----------|--------------|
| 1: Analyze ActionType Usage | 15 min | None |
| 2: Design Sealed Hierarchy | 15 min | Task 1 |
| 3: Create Action Records | 20 min | Task 2 |
| 4: Create ActionFactory | 20 min | Task 3 |
| 5: Refactor WorkflowRunner | 30 min | Task 4 |
| 6: Update Handler Signatures | 20 min | Task 5 |
| 7: Refactor Validator | 20 min | Task 6 |
| 8: Update Tests | 30 min | Tasks 5-7 |
| **Total** | **~170 min** | Sequential tasks 1-2, parallel 3-4, sequential 5-8 |

---

## Key Metrics

**Code changes:**
- New files: 9 (Action + 7 records + ActionFactory)
- Modified files: 4 (WorkflowRunner, WorkflowValidator, CODING_STANDARDS, tests)
- Lines added: ~200 (new classes) + ~35 (handler signatures) + ~30 (validator updates)
- Lines removed: ~0 (can keep StepDef for YAML compatibility)
- **Net impact:** +265 lines, -0 lines = +265 lines (new functionality, not refactoring)

**Safety metrics:**
- Type safety: ⬆️ HIGH (sealed classes prevent invalid ActionType combinations)
- Compiler enforcement: ⬆️ HIGH (exhaustiveness checking for action dispatch)
- Test coverage: ⟷ SAME (100% before, 100% after)
- Regressions: ⬇️ ZERO (Phase 12A baseline maintained)

**Architectural improvements:**
- ✅ Handler methods have clear contracts (LoginAction vs generic StepDef)
- ✅ Record constructors validate non-null/non-empty constraints
- ✅ Pattern matching eliminates casting and instanceof checks
- ✅ Compiler prevents missing ActionType handler implementation

---

## Architecture Decision Record (ADR-012D-001)

**Title:** Sealed Classes for Type-Safe Action Dispatch

**Status:** PENDING (DRAFT)

**Context:**
HTI5250J has 7 action types (LOGIN, NAVIGATE, FILL, SUBMIT, ASSERT, WAIT, CAPTURE). Current implementation uses untyped string-based dispatch via ActionType enum, leading to potential runtime bugs if new action types are added without corresponding handlers.

**Decision:**
Implement sealed interface Action with 7 permitted final record implementations. Replace untyped StepDef dispatch with typed Action dispatch in WorkflowRunner and WorkflowValidator.

**Rationale:**
1. **Compile-time safety** — Sealed classes + exhaustive switching catch missing handlers at compilation
2. **Type clarity** — Handler methods receive LoginAction, not generic StepDef with 11 optional fields
3. **Constructor validation** — Record constructors enforce non-null constraints automatically
4. **No library conflicts** — Pure Java 17+ feature, doesn't require SnakeYAML changes
5. **Higher ROI** — Worth more than Records (Phase 12C) because prevents runtime failures in production

**Alternatives considered:**
1. Keep ActionType enum (REJECTED: no type safety, no compiler enforcement)
2. Add sealed enum enhancement (REJECTED: doesn't add type safety, pattern matching can't extract fields)
3. Sealed abstract class hierarchy (REJECTED: records are cleaner for data classes)

**Consequences:**
- ✅ Positive: Compiler enforces action handler completeness
- ✅ Positive: Handler methods have precise parameter types
- ✅ Positive: Pattern matching eliminates casting boilerplate
- ⚠️ Negative: Requires ActionFactory to convert StepDef → Action at runtime
- ⚠️ Negative: Adds ~265 lines of new code
- ✓ Mitigated: No changes to YAML deserialization (StepDef unchanged)
- ✓ Mitigated: No test regressions (Phase 12A baseline preserved)

**Metrics:**
- Type safety: 3/5 → 5/5 (compile-time enforcement added)
- Code clarity: 2/5 → 4/5 (handler contracts explicit)
- Compile time: no impact (sealed classes don't slow compilation)
- Runtime performance: no impact (ActionFactory creates objects in O(1))

---

## Next Steps (After Phase 12D)

### Phase 12E: Sealed Action Validators

Extend sealed class hierarchy to validators. Each ActionValidator could be sealed:

```java
sealed interface ActionValidator permits LoginValidator, NavigateValidator, ... {
    ValidationResult validate(Action action, int stepIndex);
}
```

**Benefit:** Validation dispatched by compiler exhaustiveness checking, not switch statements.

### Phase 12F: Virtual Thread Integration for Action Execution

Execute actions as virtual threads:

```java
Thread.ofVirtual()
    .name("action-" + action.type())
    .start(() -> executeAction(action))
    .join();
```

---

## References

- Java 21 Sealed Classes: https://openjdk.org/jeps/409
- Java 21 Pattern Matching: https://openjdk.org/jeps/440
- Sealed Classes Best Practices: https://dev.java/
- Fowler, Martin. "Evolutionary Architecture" — Design for today's needs, refactor when pain emerges
- Previous phases: Phase 12A (SRP), Phase 12B (blocked), Phase 12C (Fowler analysis)

