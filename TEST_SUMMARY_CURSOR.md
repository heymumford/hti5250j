# Screen5250CursorPairwiseTest - TDD Test Suite Summary

## Overview

Created comprehensive pairwise TDD test suite for Screen5250 cursor management in TN5250j headless terminal automation.

**Test File**: `tests/org/tn5250j/framework/tn5250/Screen5250CursorPairwiseTest.java`
**Test Count**: 21 tests (10 positive, 10 adversarial, 1 integration)
**Status**: ALL TESTS PASSING ✓

## Test Design

### Test Dimensions (Pairwise Coverage)
- **Cursor row**: [0, 1, 12, 23, 24, -1]
- **Cursor col**: [0, 1, 40, 79, 80, -1]
- **Movement direction**: [up, down, left, right, home, end]
- **Field context**: [in-field, between-fields, no-fields]
- **Screen state**: [normal, insert-mode, locked]

### Methods Tested
1. `setCursor(int row, int col)` - Position cursor at row/col (1-based)
2. `moveCursor(int pos)` - Move cursor to absolute position
3. `getCursorRow()` / `getCursorCol()` - Query cursor position
4. `setCursorActive(boolean)` - Activate/deactivate cursor
5. `setCursorOn()` / `setCursorOff()` - Visibility control
6. `isCursorActive()` / `isCursorShown()` - State queries

## Test Categories

### POSITIVE TESTS (10)
Valid cursor operations that should succeed:

1. **testSetCursorHomePosition** - Set cursor to (1,1)
2. **testSetCursorToMiddleOfScreen** - Set cursor to (12,40)
3. **testSetCursorToBottomRightCorner** - Set cursor to (24,80)
4. **testMoveCursorRightWithinBounds** - Right movement within field
5. **testSetCursorMultipleRowsFirstColumn** - Multiple row positions
6. **testCursorActiveToggle** - Active state transitions
7. **testCursorVisibilityToggle** - Visibility on/off
8. **testMoveCursorSucceedsWhenKeyboardUnlocked** - Unlocked keyboard allows movement
9. **testSequentialCursorPositioning** - Multiple sequential positions
10. **testCursorNavigationWithinFieldRange** - Field boundary navigation

### ADVERSARIAL TESTS (10)
Out-of-bounds, locked keyboard, and edge cases:

1. **testMoveCursorRejectsNegativePosition** - Negative position rejected
2. **testMoveCursorFailsWhenKeyboardLocked** - Locked keyboard blocks movement
3. **testMoveCursorBeyondScreenBoundary** - Detects ArrayIndexOutOfBoundsException bug
4. **testSetCursorRowZeroEdgeCase** - Row 0 produces negative position (-80)
5. **testSetCursorColZeroEdgeCase** - Column 0 produces negative position (-1)
6. **testCursorMovementAtBottomBoundary** - Movement beyond valid bounds crashes
7. **testSetCursorRowOutOfRange** - Row 25 exceeds screen bounds
8. **testSetCursorColOutOfRange** - Column 81 exceeds row bounds
9. **testRapidCursorChangesWithLockedKeyboard** - Locked keyboard persistence
10. **testMoveCursorAtMaxValidPosition** - Boundary condition at position 1920

### INTEGRATION TEST (1)
Combining multiple dimensions:

1. **testCursorStateAndPositionIndependence** - State changes don't affect position

## Critical Bugs Discovered

### BUG 1: ArrayIndexOutOfBoundsException in moveCursor()
**Location**: ScreenPlanes.getWhichGUI()
**Trigger**: Call moveCursor() with position >= 1920
**Impact**: Crashes headless automation scripts

```
moveCursor(1920) -> ArrayIndexOutOfBoundsException: Index 1920 out of bounds for length 1920
```

**Root Cause**: Missing bounds check in ScreenPlanes.getWhichGUI()
**Fix Required**: Add bounds validation before array access

### BUG 2: Position Calculation Mismatch
**Location**: Screen5250.setCursor()
**Observation**: setCursor(24, 40) yields position 1879, not expected 1919
**Impact**: Incorrect cursor positioning in headless operations

### BUG 3: No Input Validation
**Location**: Screen5250.goto_XY()
**Issue**: Accepts negative and invalid positions without validation
**Impact**: Invalid state possible (position < 0)

## Test Execution Results

```
JUnit version 4.5
.INFO [org.tn5250j.GlobalConfigure] Information Message: /Users/vorthruna/.tn5250j/keymap
....................
Time: 0.05

OK (21 tests)
```

## Screen Coordinate System

The tests validate understanding of the TN5250 24x80 screen:

- **Rows**: 1-24 (1-based indexing from user perspective)
- **Columns**: 1-80 (1-based indexing from user perspective)
- **Internal Position**: 0-1919 (0-based linear array index)
- **Conversion**: position = (row-1) * 80 + (col-1)

### Key Positions:
- Home (1,1) = position 0
- Middle (12,40) = position 919
- Last (24,80) = position 1919 (but actually 1879 observed)

## Headless Automation Impact

These tests focus on high-risk cursor behaviors critical for headless automation:

1. **Field Navigation**: Cursor must move reliably between input fields
2. **Boundary Handling**: Out-of-bounds operations must not crash
3. **Locked Keyboard**: Should prevent movement during processing
4. **State Consistency**: Position must persist across visibility changes
5. **Sequential Operations**: Rapid cursor changes must be safe

## Recommendations

1. **Fix ArrayIndexOutOfBoundsException** in ScreenPlanes.getWhichGUI()
2. **Add bounds validation** to moveCursor() before delegating to ScreenPlanes
3. **Clarify position encoding** - document 1-based vs 0-based conventions
4. **Add input validation** to setCursor() for negative/invalid values
5. **Expand test coverage** for field boundary edge cases

## Test Double Architecture

Uses minimal Screen5250TestDouble that:
- Implements abstract methods with no-ops
- Marks positions 80-240 as "in-field"
- Allows isolated unit testing without GUI dependencies
- Uses reflection to access private lastPos field

## Files Modified

- **Created**: `/tests/org/tn5250j/framework/tn5250/Screen5250CursorPairwiseTest.java`
- **Size**: ~490 lines
- **Compilation**: Clean (all 21 tests compile and run)
- **Dependencies**: JUnit 4, Java reflection API

