# GUI Component Tests - Quick Start Guide

## Running the Tests

### Option 1: Using Ant (Recommended)
```bash
cd /Users/vorthruna/ProjectsWATTS/tn5250j-headless
ant run-tests
```

This compiles all tests and runs them via JUnit. Test results appear in console output.

### Option 2: Running Only GUI Tests
```bash
cd /Users/vorthruna/ProjectsWATTS/tn5250j-headless
javac -d build -cp "$(find lib -name '*.jar' | tr '\n' ':'):build" \
  tests/org/tn5250j/gui/GuiComponentPairwiseTest.java

java -cp "$(find lib -name '*.jar' | tr '\n' ':'):build" \
  org.junit.runner.JUnitCore org.tn5250j.gui.GuiComponentPairwiseTest
```

Expected output:
```
JUnit version 4.5
....................
Time: 0.633

OK (20 tests)
```

### Option 3: IDE Integration
- **IntelliJ IDEA**: Right-click `GuiComponentPairwiseTest.java` → Run
- **Eclipse**: Right-click → Run As → JUnit Test
- **VS Code**: Install Test Explorer for Java extension

## Test File Location
```
tests/org/tn5250j/gui/GuiComponentPairwiseTest.java
```

## Test Coverage at a Glance

### Positive Path Tests (Happy Day Scenarios)
| Test | Dimension | What It Tests |
|------|-----------|---------------|
| testAddTab_SingleTabToEmptyPane_ShouldSucceed | [0→1] [add] | Single tab addition |
| testAddTabs_SequentialMultipleTabs_ShouldMaintainOrder | [0→2] [add] | Multiple tabs in order |
| testSelectTab_ValidIndex_ShouldSelectCorrectTab | [2] [select] | Valid index selection |
| testSelectTab_MiddleTabFromLargeSet_ShouldSelectCorrectly | [10] [mid] | Middle index (5 of 10) |
| testSelectTab_LastTabFromSet_ShouldSelectCorrectly | [10] [last] | Last index selection |
| testRemoveTab_MiddleTab_ShouldRemoveAndAdjustSelection | [10] [remove] | Middle removal |
| testRemoveTab_FirstTab_ShouldRemoveAndPreserveSecond | [2] [remove] | First removal |
| testRemoveTab_LastTab_ShouldRemoveCorrectly | [2] [remove] | Last removal |

### Adversarial Tests (Error Handling)
| Test | Risk Being Tested | Expected Behavior |
|------|-------------------|-------------------|
| testSelectTab_EmptyPane_ShouldHandleGracefully | NPE on empty access | Returns null safely |
| testSelectTab_OutOfBoundsIndex_ShouldThrowException | OOB index error | Throws IndexOutOfBoundsException |
| testRemoveTab_EmptyPane_ShouldHandleGracefully | NPE on empty removal | Throws exception |
| testTabOperations_RapidAddRemove_ShouldMaintainConsistency | State corruption | Remains consistent |
| testSelectTab_RapidSwitching_ShouldHandleConsistently | Selection race | Maintains valid index |
| testTabOperations_Concurrent_ShouldNotThrowNPE | Concurrent race | No NPE with 4 threads |
| testTabComponent_VisibilityStateChanges_ShouldReflectCorrectly | State transition | Visibility preserved |
| testTabComponent_DisposedComponent_ShouldHandleGracefully | Disposed access | No NPE when disposed |
| testGetComponentAt_OutOfBoundsIndex_ShouldHandleGracefully | OOB retrieval | Throws exception |
| testGetSelectedComponent_EmptyPane_ShouldReturnNull | Null safety | Returns null not NPE |
| testTabCount_ComplexOperationSequence_ShouldRemainConsistent | Complex sequence | Count matches state |
| testTabOperations_LargeScale_ShouldHandleHundredTabs | 100-tab scenario | All operations valid |

## Understanding Test Names

Test names follow pattern: `test[What][Scenario]_[Conditions]_Should[Expected]`

Example: `testSelectTab_MiddleTabFromLargeSet_ShouldSelectCorrectly`
- **What**: SelectTab (the action)
- **Scenario**: MiddleTabFromLargeSet (specific test case)
- **Conditions**: (implicit: tab_count=10, index=5)
- **Expected**: ShouldSelectCorrectly (expected outcome)

## Key NPE Vulnerabilities Tested

The suite specifically validates these 5 NPE vulnerabilities from the audit:

1. **Empty Pane Access** → Tests 9, 18
   - Problem: `getSelectedComponent()` on empty pane
   - Protected: Returns null, never throws NPE

2. **Out-of-Bounds Index** → Tests 10, 17
   - Problem: `setSelectedIndex(99)` with 2 tabs
   - Protected: Throws IndexOutOfBoundsException as expected

3. **Concurrent Modification** → Test 14
   - Problem: Simultaneous add/select/get operations
   - Protected: 4 threads, 50+ operations, no NPE

4. **Disposed Components** → Test 16
   - Problem: Accessing component marked as disposed
   - Protected: Disposed flag accessible, no NPE

5. **Rapid Add/Remove** → Tests 12, 19, 20
   - Problem: 100 rapid cycles cause state corruption
   - Protected: Count remains consistent, indices valid

## Extending the Tests

### Adding a New Positive Path Test

```java
/**
 * Test Case 21: [Your scenario]
 * Dimensions: [dimensions here]
 */
@Test
public void testYourFeature_YourScenario_ShouldYourExpectation() {
    // ARRANGE: Setup initial state
    MockSessionPanel tab1 = createMockSessionPanel("Tab 1");
    tabbedPane.addTab("Tab 1", tab1);
    assertEquals("Precondition check", 1, tabbedPane.getTabCount());

    // ACT: Perform the action
    tabbedPane.setSelectedIndex(0);

    // ASSERT: Verify the result
    assertEquals("Expected outcome", 0, tabbedPane.getSelectedIndex());
    assertEquals("Component check", tab1, tabbedPane.getSelectedComponent());
}
```

### Adding a New Adversarial Test

```java
/**
 * Test Case 22: [Your edge case]
 * Dimensions: [dimensions]
 */
@Test
public void testYourFeature_EdgeCase_ShouldHandleGracefully() {
    // ARRANGE: Create challenging scenario
    MockSessionPanel tab = createMockSessionPanel("Tab");
    tabbedPane.addTab("Tab", tab);

    // ACT & ASSERT: Handle gracefully without NPE
    try {
        // Your operation that might fail
        tabbedPane.setSelectedIndex(-5);
        fail("Should throw exception for negative index");
    } catch (IndexOutOfBoundsException e) {
        assertNotNull("Exception should be thrown", e);
    }
}
```

### Key Patterns to Follow

1. **Use MockSessionPanel for all test components** - Lightweight, no external dependencies
2. **Precondition checks** - Verify setup before acting
3. **Clear ACT/ASSERT separation** - Makes test intent obvious
4. **Descriptive failure messages** - Include what was expected vs actual
5. **No test interdependencies** - Each test standalone, any can run in any order

## Test Metrics

### Execution Performance
```
Total tests: 20
Passing: 20 (100%)
Failing: 0
Time: 633ms average
```

### Coverage by Dimension
- Tab count: 0, 1, 2, 10, 100 (5 values)
- Selected index: -1, 0, 1, 5, 9, 99 (6 values)
- Operations: add, remove, select, get (4 operations)
- Timing: sequential, rapid (100x), concurrent (4 threads) (3 patterns)
- States: normal, visible/hidden, disposed (3 states)

### Theoretical vs Actual
- Theoretical combinations: 5×6×4×3×3 = 2,160
- Actual tests: 20
- Reduction factor: 108:1 (pairwise optimization)

## Troubleshooting

### Test Won't Compile
```
Error: package org.junit... does not exist
```
**Solution**: Ensure JUnit 4.x is in classpath
```bash
echo "JUnit library locations:"
find lib -name "*junit*.jar"
```

### Test Hangs/Timeout
```
java.util.concurrent.TimeoutException
```
**Solution**: Concurrent test (Test 14) uses 10-second timeout
- If hardware is slow, increase in code: `finishLatch.await(10, TimeUnit.SECONDS)`
- Or run on faster machine

### Unexpected Test Failure
```
AssertionError: expected:<0> but was:<1>
```
**Diagnostic steps:**
1. Note which test failed
2. Check recent code changes to JTabbedPane usage
3. Verify no external test pollution (run in isolation)
4. Run with verbose output: add `println()` in test

## Integration with CI/CD

### GitHub Actions Example
```yaml
- name: Run GUI Component Tests
  run: |
    cd /Users/vorthruna/ProjectsWATTS/tn5250j-headless
    ant run-tests -Dtest.include="**/*GuiComponentPairwise*"
```

### GitLab CI Example
```yaml
test:gui:
  stage: test
  script:
    - cd tn5250j-headless
    - ant compile compile-tests
    - java -cp "$(find lib -name '*.jar' | tr '\n' ':'):build" \
        org.junit.runner.JUnitCore org.tn5250j.gui.GuiComponentPairwiseTest
  artifacts:
    reports:
      junit: build/*Test*.xml
```

## Documentation References

- **Main test file**: `tests/org/tn5250j/gui/GuiComponentPairwiseTest.java` (590 lines)
- **Test suite doc**: `PAIRWISE_GUI_TEST_SUITE.md` (dimensions, coverage)
- **Vulnerability analysis**: `GUI_TEST_VULNERABILITY_ANALYSIS.md` (5 NPE risks)
- **Execution report**: `GUI_TEST_EXECUTION_REPORT.txt` (latest test run)

## Quick Command Reference

```bash
# Compile only GUI tests
javac -d build -cp "$(find lib -name '*.jar' | tr '\n' ':'):build" \
  tests/org/tn5250j/gui/GuiComponentPairwiseTest.java

# Run with verbose output
java -cp "$(find lib -name '*.jar' | tr '\n' ':'):build" \
  -Dorg.junit.runner.verbosity=2 \
  org.junit.runner.JUnitCore org.tn5250j.gui.GuiComponentPairwiseTest

# Count assertions
grep -c "assertEquals\|assertTrue\|assertNull\|fail" \
  tests/org/tn5250j/gui/GuiComponentPairwiseTest.java

# Show test names only
grep "public void test" tests/org/tn5250j/gui/GuiComponentPairwiseTest.java
```

## Next Steps

1. Review `GUI_TEST_VULNERABILITY_ANALYSIS.md` for identified issues
2. Consider applying recommended fixes to `Gui5250Frame.java`
3. Add integration tests for `SessionPanel` lifecycle
4. Implement performance benchmarks for large-scale tab operations
5. Add visual regression tests using Playwright or Robot Framework
