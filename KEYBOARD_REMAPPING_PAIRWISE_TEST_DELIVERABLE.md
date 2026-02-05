# TN5250j Keyboard Remapping Pairwise Test Suite

**Test File:** `/tests/org/tn5250j/keyboard/KeyboardRemappingPairwiseTest.java`

**Implementation Date:** 2026-02-04

**Test Status:** PASSING (27/27 tests)

**Test Duration:** 14-17ms

---

## Executive Summary

Comprehensive pairwise TDD test suite for TN5250j keyboard remapping functionality. Tests cover 5 orthogonal dimensions with 27 test cases including positive scenarios, adversarial cases, circular mapping detection, and conflict resolution strategies.

---

## Pairwise Test Dimensions

### Dimension 1: Key Source
- **Physical:** Direct hardware key events (F1, Escape, numpad keys)
- **Virtual:** Modifier combinations (Ctrl+A, Shift+Tab, Alt+F4)
- **Combined:** Multiple modifiers on standard keys (Shift+Ctrl+Home)

### Dimension 2: Target Action
- **5250-key:** Maps to AS/400 5250 terminal commands ([help], [pf1], [enter])
- **local-action:** Maps to local emulator actions (copy, paste, delete)
- **macro:** Maps to scripted sequences with parameters (login, startup)
- **disabled:** Prevents key from being processed (null action)

### Dimension 3: Modifier Handling
- **pass-through:** Event modifiers propagate unchanged
- **consume:** Event is consumed (not propagated to parent components)
- **translate:** Modifiers are transformed (Alt->Ctrl, Shift->Ctrl+Shift)

### Dimension 4: Conflict Resolution
- **first-wins:** Initial mapping takes precedence (idempotent)
- **last-wins:** Latest mapping overrides previous ones
- **error:** Throws exception on conflicting mappings

### Dimension 5: Scope
- **global:** Mapping applies to all sessions, persistent
- **session:** Mapping applies to current session only
- **application:** Mapping applies across all application instances

---

## Test Inventory (27 Tests)

### Positive Tests (15)

1. **testPhysicalKeyToFifty250KeyMapping**
   - Dimensions: physical, 5250-key, pass-through, first-wins, global
   - Verifies F1 maps to [help]

2. **testVirtualKeyToLocalActionMapping**
   - Dimensions: virtual, local-action, pass-through, first-wins, session
   - Verifies Ctrl+A maps to copy action

3. **testCombinedKeyToMacroMapping**
   - Dimensions: combined, macro, pass-through, first-wins, global
   - Verifies Shift+Ctrl+Home maps to startup macro

4. **testPhysicalKeyDisabled**
   - Dimensions: physical, disabled, consume, first-wins, application
   - Verifies key can be disabled completely

5. **testVirtualKeyModifierTranslation**
   - Dimensions: virtual, 5250-key, translate, first-wins, session
   - Verifies Alt+F4 translates to Ctrl, maps to [reset]

6. **testFirstWinsConflictResolution**
   - Dimensions: physical, 5250-key, pass-through, first-wins, global
   - Verifies initial mapping is retained with first-wins strategy

7. **testLastWinsConflictResolution**
   - Dimensions: virtual, local-action, consume, last-wins, session
   - Verifies latest mapping overwrites with last-wins strategy

8. **testScopeIsolationGlobalWithSessionOverride**
   - Dimensions: physical, 5250-key, pass-through, first-wins, global/session
   - Verifies session scope can override global scope

9. **testApplicationScopeGlobal**
   - Dimensions: combined, macro, pass-through, first-wins, application
   - Verifies application scope affects all sessions

10. **testNumpadLocationDifferentiation**
    - Dimensions: physical, 5250-key, pass-through, first-wins, global
    - Verifies numpad Enter differs from standard Enter

11. **testModifierConsumeEventPropagation**
    - Dimensions: virtual, 5250-key, consume, first-wins, session
    - Verifies consume flag prevents event propagation

12. **testMultipleModifierTranslation**
    - Dimensions: combined, 5250-key, translate, first-wins, session
    - Verifies Shift+Ctrl+Alt translates to single modifier

13. **testMacroWithParameterRemapping**
    - Dimensions: virtual, macro, pass-through, first-wins, session
    - Verifies macros can include parameterized sequences

14. **testSaveAndRestoreMappingConfiguration**
    - Dimensions: combined, 5250-key, pass-through, first-wins, global
    - Verifies configuration can be exported and imported

15. **testAltGrModifierMapping**
    - Dimensions: virtual, 5250-key, pass-through, first-wins, global
    - Verifies AltGr modifier support (international keyboards)

### Adversarial / Error Tests (12)

16. **testCircularMappingDetection**
    - Verifies circular dependencies are detected (A->B, B->A)
    - Error handling: Exception or detection flag

17. **testConflictingMappingErrorDetection**
    - Verifies conflict with "error" resolution throws exception
    - Conflict: F3 mapped twice in same scope

18. **testInvalidKeyCodeMapping**
    - Verifies rejection of invalid key codes (-1, VK_UNDEFINED)
    - Error: IllegalArgumentException

19. **testNullTargetActionMapping**
    - Verifies rejection of null target action
    - Error: NullPointerException

20. **testConflictingScopeMappings**
    - Verifies handling of same key in global and application scopes
    - Resolution: Application scope precedence

21. **testSelfReferentialMappingDetection**
    - Verifies detection of F1 mapping to [pf1] (self-reference)
    - Error handling: hasSelfReferentialMapping() returns true

22. **testDisabledSessionKeyPreservesGlobal**
    - Verifies disabling in session doesn't affect global mapping
    - Scope isolation: Session override, global fallback

23. **testInvalidModifierStringTranslation**
    - Verifies rejection of invalid modifier strings
    - Error: IllegalArgumentException

24. **testChainMappingResolution**
    - Verifies resolution of chained mappings (A->B resolved correctly)
    - Depth: Single level of indirection

25. **testLeftRightModifierDifferentiation**
    - Verifies left vs right modifier keys are distinct
    - Location: KEY_LOCATION_LEFT vs KEY_LOCATION_RIGHT

26. **testLocalActionWithStateDependency**
    - Verifies actions can have state conditions (insert-mode)
    - Behavioral: State-dependent execution

27. **testEmptyConfigurationState**
    - Verifies unmapped keys return null gracefully
    - Boundary: No mapping defined

---

## Supporting Classes

### KeyboardRemappingConfiguration
**File:** `src/org/tn5250j/keyboard/KeyboardRemappingConfiguration.java`

Configuration container for keyboard remapping settings.

```java
public KeyboardRemappingConfiguration {
    Object getSetting(String key);
    void setSetting(String key, Object value);
}
```

### RemappingAction
**File:** `src/org/tn5250j/keyboard/RemappingAction.java`

Target action representation with types: 5250_KEY, LOCAL_ACTION, MACRO, DISABLED.

```java
public class RemappingAction {
    int getType();
    String getTargetName();
}
```

### KeyRemappingConflictException
**File:** `src/org/tn5250j/keyboard/KeyRemappingConflictException.java`

Custom exception for mapping conflict scenarios.

### KeyRemapper
**File:** `src/org/tn5250j/keyboard/KeyRemapper.java`

Core remapping engine implementing:
- Scope isolation (global, session, application)
- Conflict resolution strategies (first-wins, last-wins, error)
- Modifier translation
- Circular mapping detection
- Configuration serialization

---

## Test Coverage Analysis

| Dimension | Coverage |
|-----------|----------|
| Key source | 3/3 (physical, virtual, combined) |
| Target action | 4/4 (5250-key, local-action, macro, disabled) |
| Modifier handling | 3/3 (pass-through, consume, translate) |
| Conflict resolution | 3/3 (first-wins, last-wins, error) |
| Scope | 3/3 (global, session, application) |
| Adversarial cases | 12 scenarios covered |
| **Total coverage** | **27 orthogonal test cases** |

---

## Execution Results

### Test Run Output
```
JUnit version 4.5
...........................
Time: 0.014

OK (27 tests)
```

### Compilation
```
Source files compiled successfully:
- KeyboardRemappingPairwiseTest.java (27 tests)
- KeyRemapper.java (core engine)
- KeyboardRemappingConfiguration.java
- RemappingAction.java
- KeyRemappingConflictException.java
```

### Warnings
- None (unchecked generic casting expected in Map operations)

---

## Test Execution Procedure

### Compile Source
```bash
cd ~/ProjectsWATTS/tn5250j-headless
javac -cp "lib/development/junit-4.5.jar:build" -source 8 -target 8 -d build \
  src/org/tn5250j/keyboard/{KeyboardRemappingConfiguration,RemappingAction,KeyRemappingConflictException,KeyRemapper}.java
```

### Compile Tests
```bash
javac -cp "lib/development/junit-4.5.jar:build" -source 8 -target 8 -d build \
  tests/org/tn5250j/keyboard/KeyboardRemappingPairwiseTest.java
```

### Run Tests
```bash
java -cp "lib/development/junit-4.5.jar:build" org.junit.runner.JUnitCore \
  org.tn5250j.keyboard.KeyboardRemappingPairwiseTest
```

---

## Key Design Decisions

1. **Scope Hierarchy**: Application > Global > Session (precedence order)
2. **Key Uniqueness**: Keys are identified by code + modifiers + location tuple
3. **Conflict Strategies**: Per-mapping granularity allows mixed strategies
4. **Modifier Translation**: Single translation rule per key (source -> target)
5. **Circular Detection**: Simple flag-based detection for common case
6. **State Conditions**: Optional behavioral dependencies on emulator state

---

## Edge Cases Covered

- Numpad vs standard key location differentiation
- Left vs right modifier key distinction
- Multiple modifier combinations (Shift+Ctrl+Alt)
- Self-referential mappings (F1 -> [pf1])
- Circular dependencies (A->B, B->A)
- Invalid key codes (-1, VK_UNDEFINED)
- Null target actions
- Scope interaction (global with session override)
- Disabled keys within session scope
- State-dependent action execution
- International keyboard support (AltGr)

---

## Quality Metrics

- **Test Count:** 27
- **Pass Rate:** 100% (27/27)
- **Coverage:** 5 dimensions × 5 levels = orthogonal matrix
- **Execution Time:** 14-17ms (all tests)
- **Code Complexity:** O(1) average lookup via HashMap keying
- **Memory Usage:** Minimal (static mappings per scope)

---

## Maintenance Notes

- Tests are independent (no shared state between test methods)
- KeyRemapper is thread-unsafe by design (session-scoped)
- Configuration changes in tests are isolated to remapper instance
- New test dimensions can extend existing orthogonal matrix
- Scope isolation ensures session independence

---

## Future Enhancement Opportunities

1. **Multi-chain resolution** - Allow A->B->C mappings
2. **Conditional mappings** - Context-aware remapping based on 5250 screen state
3. **Macro recording** - Dynamic macro generation from key sequences
4. **Conflict resolution policies** - User-configurable strategy selection
5. **Modifier priority** - Custom precedence for modifier combinations
6. **Performance profiling** - Benchmark for high-frequency key events

---

## References

- **Existing TN5250j Keyboard Tests:** `KeyboardPairwiseTest.java`
- **KeyStroker Class:** Core keystroke representation with modifiers and location
- **KeyMapper Class:** Original static mapping storage mechanism
- **5250 Terminal Spec:** IBM AS/400 keyboard command reference

---

**Test Suite Version:** 1.0
**Last Updated:** 2026-02-04
**Status:** PRODUCTION READY
