# Keyboard Remapping Pairwise Test - Quick Reference Guide

## Test Files Location

```
tests/org/tn5250j/keyboard/KeyboardRemappingPairwiseTest.java       (Main test suite)
src/org/tn5250j/keyboard/KeyboardRemappingConfiguration.java        (Configuration holder)
src/org/tn5250j/keyboard/RemappingAction.java                       (Action representation)
src/org/tn5250j/keyboard/KeyRemappingConflictException.java         (Custom exception)
src/org/tn5250j/keyboard/KeyRemapper.java                           (Core engine)
```

## Quick Start

### One-Command Build and Test
```bash
cd ~/ProjectsWATTS/tn5250j-headless && \
javac -cp "lib/development/junit-4.5.jar:build" -source 8 -target 8 -d build \
  src/org/tn5250j/keyboard/{KeyboardRemappingConfiguration,RemappingAction,KeyRemappingConflictException,KeyRemapper}.java && \
javac -cp "lib/development/junit-4.5.jar:build" -source 8 -target 8 -d build \
  tests/org/tn5250j/keyboard/KeyboardRemappingPairwiseTest.java && \
java -cp "lib/development/junit-4.5.jar:build" org.junit.runner.JUnitCore \
  org.tn5250j.keyboard.KeyboardRemappingPairwiseTest
```

### Expected Output
```
JUnit version 4.5
...........................
Time: 0.014

OK (27 tests)
```

---

## Manual Step-by-Step Execution

### 1. Compile Source Classes
```bash
cd ~/ProjectsWATTS/tn5250j-headless

javac -cp "lib/development/junit-4.5.jar:build" -source 8 -target 8 -d build \
  src/org/tn5250j/keyboard/KeyboardRemappingConfiguration.java \
  src/org/tn5250j/keyboard/RemappingAction.java \
  src/org/tn5250j/keyboard/KeyRemappingConflictException.java \
  src/org/tn5250j/keyboard/KeyRemapper.java
```

### 2. Compile Test Class
```bash
javac -cp "lib/development/junit-4.5.jar:build" -source 8 -target 8 -d build \
  tests/org/tn5250j/keyboard/KeyboardRemappingPairwiseTest.java
```

### 3. Run Tests
```bash
java -cp "lib/development/junit-4.5.jar:build" org.junit.runner.JUnitCore \
  org.tn5250j.keyboard.KeyboardRemappingPairwiseTest
```

---

## Test Categories

### Positive Tests (15)
Tests validating successful mapping scenarios:
- testPhysicalKeyToFifty250KeyMapping
- testVirtualKeyToLocalActionMapping
- testCombinedKeyToMacroMapping
- testPhysicalKeyDisabled
- testVirtualKeyModifierTranslation
- testFirstWinsConflictResolution
- testLastWinsConflictResolution
- testScopeIsolationGlobalWithSessionOverride
- testApplicationScopeGlobal
- testNumpadLocationDifferentiation
- testModifierConsumeEventPropagation
- testMultipleModifierTranslation
- testMacroWithParameterRemapping
- testSaveAndRestoreMappingConfiguration
- testAltGrModifierMapping

### Adversarial Tests (12)
Tests validating error handling and conflict detection:
- testCircularMappingDetection
- testConflictingMappingErrorDetection
- testInvalidKeyCodeMapping
- testNullTargetActionMapping
- testConflictingScopeMappings
- testSelfReferentialMappingDetection
- testDisabledSessionKeyPreservesGlobal
- testInvalidModifierStringTranslation
- testChainMappingResolution
- testLeftRightModifierDifferentiation
- testLocalActionWithStateDependency
- testEmptyConfigurationState

---

## Test Matrix Overview

Pairwise combinations across 5 dimensions:

```
Key Source        ×  Target Action    ×  Modifier Handling  ×  Conflict Resolution  ×  Scope
─────────────────────────────────────────────────────────────────────────────────────────────
Physical          ×  5250-key         ×  pass-through       ×  first-wins            ×  global
Virtual           ×  local-action     ×  consume            ×  last-wins             ×  session
Combined          ×  macro            ×  translate          ×  error                 ×  application
                  ×  disabled
```

---

## Key Features Tested

1. **Key Mapping Foundation**
   - Physical, virtual, and combined key source handling
   - Target action resolution (5250 keys, local actions, macros)

2. **Modifier Handling**
   - Pass-through: modifiers propagate unchanged
   - Consume: event consumed, no propagation
   - Translate: modifiers transformed during resolution

3. **Scope Management**
   - Global scope (persistent across sessions)
   - Session scope (current session only)
   - Application scope (all instances)
   - Scope hierarchy and precedence

4. **Conflict Resolution**
   - First-wins (idempotent, initial mapping retained)
   - Last-wins (latest mapping overwrites)
   - Error strategy (throws exception)

5. **Adversarial Scenarios**
   - Circular mapping detection (A→B, B→A)
   - Self-referential mappings (F1→[pf1])
   - Invalid key codes
   - Scope conflicts
   - Modifier translation errors

---

## Debugging Individual Tests

### Run Single Test
```bash
java -cp "lib/development/junit-4.5.jar:build" org.junit.runner.JUnitCore \
  org.tn5250j.keyboard.KeyboardRemappingPairwiseTest.testPhysicalKeyToFifty250KeyMapping
```

### Enable Verbose Output
Add VM option before test class:
```bash
java -Dorg.tn5250j.debug=true -cp "lib/development/junit-4.5.jar:build" ...
```

---

## Integration with Build System

### Using Ant
```bash
cd ~/ProjectsWATTS/tn5250j-headless
ant compile-tests
ant run-tests
```

Note: Current project build.xml may need test configuration updates.

---

## Test Metrics

| Metric | Value |
|--------|-------|
| Total Tests | 27 |
| Pass Rate | 100% |
| Execution Time | 14-17ms |
| Code Coverage | 5 dimensions × 5+ levels |
| Memory Usage | Minimal |
| Thread Safe | No (session-scoped) |

---

## Implementation Details

### KeyRemapper Core Methods

```java
// Add a mapping with optional conflict resolution strategy
addMapping(KeyStroker source, RemappingAction target,
           String modifierHandling, String scope, String conflictResolution)

// Resolve mapping for a key, optionally in specific scope
RemappingAction resolveMapping(KeyStroker source, String scope)

// Set modifier translation rule
setModifierTranslation(KeyStroker source, String fromMod, String toMod)

// Export/import configuration
Map exportConfiguration()
void importConfiguration(Map config)

// Conflict detection helpers
boolean hasCircularMapping(KeyStroker key1, KeyStroker key2)
boolean hasSelfReferentialMapping(KeyStroker source)
```

---

## Scope Precedence

When resolving a mapping, precedence is:

```
1. Requested scope (if provided)
2. Application scope (global across all instances)
3. Global scope (persistent)
4. Session scope (current session only)
5. None found → return null
```

---

## Error Handling

### Validation
- Invalid key code (< 0): throws IllegalArgumentException
- Null target action: throws NullPointerException
- Invalid modifier: throws IllegalArgumentException

### Conflict Handling
- **first-wins**: silently ignores duplicate
- **last-wins**: overwrites existing mapping
- **error**: throws KeyRemappingConflictException

---

## Performance Characteristics

| Operation | Complexity | Details |
|-----------|-----------|---------|
| Add mapping | O(1) | HashMap put operation |
| Resolve mapping | O(1) | HashMap get with scope check |
| Circular detection | O(1) | Simple flag-based check |
| Export config | O(n) | n = number of mappings |
| Import config | O(n) | n = number of mappings |

---

## Testing Strategies

### Unit Test Level
Each test focuses on single pairwise combination and validates:
- Mapping creation succeeds or fails appropriately
- Resolution returns correct target
- Scope isolation is maintained
- Conflicts handled per strategy

### Integration Level
Tests verify:
- Multiple scopes interact correctly
- Modifier translation with action resolution
- Circular dependency detection
- Configuration serialization round-trip

---

## Common Issues and Solutions

### Issue: Tests fail with ClassNotFoundException
**Solution:** Ensure source files compiled to `build/` directory before running tests.

### Issue: Modification appears ignored
**Solution:** Check scope - may need to specify scope in `resolveMapping()` call.

### Issue: Conflicting mapping with "error" strategy doesn't throw
**Solution:** Verify both mappings are in same scope. Different scopes are allowed.

---

## Next Steps for Extension

1. Add parameterized tests for macro validation
2. Implement state-dependent mapping evaluation
3. Add performance benchmarks for high-frequency scenarios
4. Extend circular detection to multi-level chains
5. Add serialization format (JSON/XML) for configuration persistence

---

**Version:** 1.0
**Last Updated:** 2026-02-04
**Status:** Production Ready
