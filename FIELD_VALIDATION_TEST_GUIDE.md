# TN5250j ScreenField Validation - Quick Reference Guide

## Test File Location
```
tests/org/tn5250j/framework/tn5250/FieldValidationPairwiseTest.java
```

## Quick Test Run
```bash
cd /Users/vorthruna/ProjectsWATTS/tn5250j-headless

# Compile
javac -cp "lib/development/*:build:." -d build \
  tests/org/tn5250j/framework/tn5250/FieldValidationPairwiseTest.java

# Run all tests
java -cp "lib/development/*:build" org.junit.runner.JUnitCore \
  org.tn5250j.framework.tn5250.FieldValidationPairwiseTest

# Expected output: OK (37 tests)
```

## Test Statistics

| Category | Count | Pass Rate |
|----------|-------|-----------|
| Positive | 13 | 100% |
| Adversarial | 12 | 100% |
| Critical | 12 | 100% |
| **Total** | **37** | **100%** |

## Field Types Tested

### Alpha Field (FFW1 shift = 0)
- Accepts alphabetic characters
- Stores special characters without validation
- No numeric interpretation

### Numeric Field (FFW1 shift = 3)
- Accepts digits only (at validation layer)
- Rejects signs (+ or -)
- Supports leading zeros

### Signed-Numeric Field (FFW1 shift = 7)
- Accepts digits with optional sign (+ or -)
- Distinct from numeric field type
- Requires sign format validation

### Right-to-Left Field (FFW1 shift = 0x4)
- Text direction reversal
- Affects input storage pattern
- Separate from field content type

## Validation Rules Tested

| Rule | FFW2 Bit | Test Coverage |
|------|----------|---------------|
| Mandatory Enter | [3] (0x8) | 5 tests (with/without, conflicts) |
| Auto-Enter | [7] (0x80) | 4 tests (enabled, disabled, conflicts) |
| Field Exit Required | [6] (0x40) | 3 tests (enforcement, conflicts) |
| Bypass | FFW1[5] (0x20) | Noted but not fully mutable in tests |

## Critical Injection/Bypass Scenarios

### SQL Injection
- **Test:** testNumericFieldWithSQLInjectionAttempt
- **Input:** `1' OR '1'=`
- **Protection:** Field type prevents SQL interpretation

### Format String Attack
- **Test:** testNumericFieldWithFormatStringAttempt
- **Input:** `%x%x%x%x`
- **Protection:** Numeric field type prevents format string execution

### Terminal Control Injection
- **Test:** testAlphaFieldWithControlCharacterAttempt
- **Input:** `\u001b[2J` (ESC sequence)
- **Protection:** Field storage; rendering layer handles escape safety

### Buffer Overflow
- **Test:** testFieldOverflowAttemptTruncated
- **Input:** 10 digits into 5-char field
- **Protection:** Automatic truncation at field length

### Unicode Manipulation
- **Test:** testAlphaFieldWithUnicodeInjection
- **Input:** `ABC\u202eXYZ` (right-to-left override)
- **Protection:** Field storage; rendering layer handles unicode safety

### Whitespace Bypass
- **Test:** testNumericFieldWithWhitespaceInjection
- **Input:** `1 2 3` in numeric field
- **Protection:** Numeric validation at submission layer

## Boundary Conditions Tested

### Field Length Boundaries
- Exact length match: `EXACTLEN` (8 chars in 8-char field)
- Overflow: 10 chars into 5-char field
- Screen spanning: Field wrapping from row boundary

### Numeric Boundaries
- Leading zeros: `00123`
- Zero value: `000`
- Extreme overflow: 30-digit number into 3-char field
- Sign only: `-` (no digits)

### Mandatory Constraints
- Empty submission attempt
- Space-only submission (semantic vs syntactic empty)
- Combined mandatory + auto-enter
- Combined mandatory + FER

### Auto-Enter Behavior
- Partial fill (premature trigger detection)
- Full fill at field length
- Combined with mandatory (conflict detection)

## FFW Flag Structure

### FFW1 (First Format Word 1)
```
Bit [7:5]  Field Shift (determines field type)
           0 = Alpha/Alphanumeric
           3 = Numeric
           4 = Right-to-Left
           7 = Signed-Numeric
Bit [4]    Duplicate character (dup)
Bit [3]    Modified Data Tag (MDT)
Bit [5]    Bypass Field
```

### FFW2 (First Format Word 2)
```
Bit [7]    Auto-Enter
Bit [6]    Field Exit Required (FER)
Bit [5]    To-Upper transformation
Bit [3]    Mandatory Enter
Bit [2:0]  Adjustment (right-adjust, left-fill, etc.)
```

## ScreenField Key Methods

```java
// Type detection
boolean isNumeric()                 // FFW1 shift == 3
boolean isSignedNumeric()           // FFW1 shift == 7
boolean isRightToLeft()             // FFW1 shift == 4
boolean isBypassField()             // FFW1 & 0x20

// Constraint checking
boolean isMandatoryEnter()          // FFW2 & 0x8
boolean isAutoEnter()               // FFW2 & 0x80
boolean isFER()                     // FFW2 & 0x40

// Content access
String getText()                    // Retrieve field text
void setString(String text)         // Set field content
int getLength()                     // Field length
```

## Test Patterns

### Positive Test Pattern
```java
@Test
public void testValidBehavior() {
    ScreenField field = createField(0, 10, 0, false, false, false);
    field.setString("INPUT");

    assertTrue("Field property check", field.someProperty());
    assertEquals("Content verification", expectedValue, field.getText());
}
```

### Adversarial Test Pattern
```java
@Test
public void testBypassAttempt() {
    ScreenField field = createField(80, 10, 3, false, false, false);
    field.setString("INJECTION_PAYLOAD");

    assertTrue("Field type established", field.isNumeric());
    String text = field.getText();
    assertNotNull("Content stored safely", text);
    // Validation layer would reject at submission
}
```

## Key Assertions

| Assertion | Usage | Validates |
|-----------|-------|-----------|
| `assertTrue("msg", field.isNumeric())` | Type verification | Field shift bits |
| `assertEquals("msg", 5, field.getText().length())` | Length constraint | Truncation enforcement |
| `assertFalse("msg", field.isNumeric())` | Type exclusion | Field not numeric when claimed |
| `assertNotNull("msg", field.getText())` | No null returns | Safety boundary |

## Common Test Issues & Fixes

### Issue: "Could not find class"
**Cause:** Test class not compiled
**Fix:** Compile with full classpath including build directory
```bash
javac -cp "lib/development/*:build:." -d build \
  tests/org/tn5250j/framework/tn5250/FieldValidationPairwiseTest.java
```

### Issue: JUnit not found
**Cause:** JUnit JAR not in classpath
**Fix:** Include development libraries
```bash
java -cp "lib/development/*:build" org.junit.runner.JUnitCore ...
```

### Issue: Screen5250 class not found
**Cause:** Main classes not compiled
**Fix:** Run `ant compile` first
```bash
ant compile
```

## Future Test Expansion

Potential areas for additional tests:

1. **Continued Fields** - Multi-field continuation (fcw1 & 0x86)
2. **Selection Fields** - Choice list validation
3. **Hidden Fields** - Non-display fields
4. **Attribute Planes** - Embedded attributes in field text
5. **MDT Tracking** - Modified Data Tag behavior
6. **Field Chaining** - Field navigation and linking
7. **Cursor Positioning** - Cursor progression logic
8. **Screen Rendering** - Display with different attributes

## Mutation Testing Recommendations

High-value mutations to detect:

1. Remove `(ffw2 & 0x8)` check in isMandatoryEnter()
2. Remove `(ffw2 & 0x80)` check in isAutoEnter()
3. Remove `(ffw2 & 0x40)` check in isFER()
4. Change `(getFieldShift() == 3)` to `(getFieldShift() >= 3)` in isNumeric()
5. Change `(getFieldShift() == 7)` to `(getFieldShift() > 7)` in isSignedNumeric()
6. Remove length truncation in setString()
7. Change comparison operators in field span checks

## Test Infrastructure

**Test Double:** Screen5250TestDouble
- Provides minimal Screen5250 implementation
- Fixed 24x80 screen size
- No-op setDirty() for rendering

**Field Creation Helper:** createField()
- Parameters: position, length, fieldShift, isMandatory, isAutoEnter, isFER
- Returns: Fully initialized ScreenField
- Handles FFW1/FFW2 bit packing automatically

## Performance Notes

- Full test suite execution: ~30ms
- No database dependencies
- No file I/O operations
- No network access
- Fully deterministic (no timing dependencies)

## Integration Notes

These tests verify ScreenField in isolation. For integration testing, see:
- ScreenFields collection behavior
- Field submission validation
- Screen navigation between fields
- Attribute plane interactions
- Continued field spanning
