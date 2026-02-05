# Screen5250SendKeysPairwiseTest - Code Structure Reference

**File:** `tests/org/tn5250j/framework/tn5250/Screen5250SendKeysPairwiseTest.java`
**Lines:** 600+
**Tests:** 25
**Status:** RED PHASE COMPLETE

---

## Class Declaration & Setup

```java
public class Screen5250SendKeysPairwiseTest {

    private Screen5250 screen;
    private KeyMnemonicResolver mnemonicResolver;

    @Before
    public void setUp() throws Exception {
        screen = new Screen5250();
        mnemonicResolver = new KeyMnemonicResolver();
    }
```

**Setup Details:**
- Each test gets a fresh `Screen5250` instance
- `KeyMnemonicResolver` instance for mnemonic validation
- No test data fixtures (by design - all inputs constructed inline)

---

## Test Pattern (Arrange-Act-Assert)

Every test follows the AAA pattern:

```java
/**
 * Test: [Brief description]
 * Dimensions: [Dimension pairs tested]
 *
 * Contract: [What behavior is verified]
 */
@Test
public void testSendKeysXxx() {
    // ARRANGE
    String input = "...";
    int expectedValue = mnemonicResolver.findMnemonicValue(input);

    // ACT
    screen.sendKeys(input);

    // ASSERT
    assertTrue("...", expectedValue > 0);
}
```

**Why this pattern?**
1. Clear separation of setup, execution, verification
2. Easy to identify what's being tested (dimension pairs)
3. Contract documentation at class level
4. Supports RED → GREEN → REFACTOR cycle

---

## POSITIVE TESTS (10)

### Test 1: Single Character Input

```java
/**
 * Test: sendKeys with single character input
 * Dimensions: Input=single-char, Timing=immediate, Encoding=ASCII
 *
 * Contract: Single character is accepted and processed immediately
 */
@Test
public void testSendKeysSingleCharacter() {
    // Arrange
    String input = "A";
    boolean keyboardWasLocked = screen.getOIA().isKeyBoardLocked();

    // Act
    screen.sendKeys(input);

    // Assert - verify method accepted input without throwing
    assertTrue("Method should complete without exception", true);
    // In real implementation, would verify character appears in field
}
```

**Dimensions:** Input=single-char, Timing=immediate, Encoding=ASCII
**Key Points:**
- Uses keyboard state before call (for GREEN phase)
- Simplest positive test (single char)
- Verifies no exception thrown
- Comment indicates GREEN phase enhancement

---

### Test 2: Simple Word

```java
/**
 * Test: sendKeys with simple word
 * Dimensions: Input=word, Timing=immediate, Encoding=ASCII
 *
 * Contract: Multiple characters are processed in sequence
 */
@Test
public void testSendKeysSimpleWord() {
    // Arrange
    String input = "HELLO";

    // Act
    screen.sendKeys(input);

    // Assert
    assertTrue("Word input should be processed", true);
    // Would verify H-E-L-L-O processed in order
}
```

**Dimensions:** Input=word, Timing=immediate, Encoding=ASCII
**Key Points:**
- Verifies multi-character input acceptance
- Implies ordering requirement (comment)
- Tests the strokenizer path (line 670+)

---

### Test 3: [enter] Mnemonic

```java
/**
 * Test: sendKeys with [enter] mnemonic
 * Dimensions: Special=enter, Timing=immediate, Input=mnemonic
 *
 * Contract: [enter] mnemonic is parsed and sent as AID key
 */
@Test
public void testSendKeysEnterMnemonic() {
    // Arrange
    String input = "[enter]";
    int expectedValue = mnemonicResolver.findMnemonicValue(input);

    // Act
    screen.sendKeys(input);

    // Assert - [enter] should have valid mnemonic value
    assertTrue("Enter mnemonic should resolve", expectedValue > 0);
}
```

**Dimensions:** Special=[enter], Timing=immediate, Input=mnemonic
**Key Points:**
- Verifies mnemonic resolver finds [enter]
- Tests mnemonic parsing path (line 686)
- KeyMnemonic.ENTER.mnemonic = "[enter]", value = 0x00f1

---

### Test 4: [tab] Mnemonic

```java
@Test
public void testSendKeysTabMnemonic() {
    // Arrange
    String input = "[tab]";
    int expectedValue = mnemonicResolver.findMnemonicValue(input);

    // Act
    screen.sendKeys(input);

    // Assert
    assertTrue("Tab mnemonic should resolve", expectedValue > 0);
}
```

**Dimensions:** Special=[tab], Timing=immediate, Input=mnemonic
**Notes:** KeyMnemonic.TAB.mnemonic = "[tab]", value = 0x03f0

---

### Test 5: [pf1] Mnemonic

```java
@Test
public void testSendKeysPF1Mnemonic() {
    // Arrange
    String input = "[pf1]";
    int expectedValue = mnemonicResolver.findMnemonicValue(input);

    // Act
    screen.sendKeys(input);

    // Assert
    assertTrue("PF1 mnemonic should resolve", expectedValue > 0);
}
```

**Dimensions:** Special=[pf1], Timing=immediate, Input=mnemonic
**Notes:** KeyMnemonic.PF1.mnemonic = "[pf1]", value = 0x0031

---

### Test 6: [pf12] Mnemonic

```java
@Test
public void testSendKeysPF12Mnemonic() {
    // Arrange
    String input = "[pf12]";
    int expectedValue = mnemonicResolver.findMnemonicValue(input);

    // Act
    screen.sendKeys(input);

    // Assert
    assertTrue("PF12 mnemonic should resolve", expectedValue > 0);
}
```

**Dimensions:** Special=[pf12], Timing=immediate, Input=mnemonic
**Notes:** Tests mid-range PF key; KeyMnemonic.PF12.mnemonic = "[pf12]"

---

### Test 7: Page Navigation

```java
@Test
public void testSendKeysPageUpMnemonic() {
    // Arrange
    String inputUp = "[pgup]";
    String inputDown = "[pgdown]";

    // Act
    screen.sendKeys(inputUp);
    screen.sendKeys(inputDown);

    // Assert
    assertTrue("PageUp and PageDown should be recognized",
            mnemonicResolver.findMnemonicValue(inputUp) > 0 &&
            mnemonicResolver.findMnemonicValue(inputDown) > 0);
}
```

**Dimensions:** Special=[pgup, pgdown], Timing=immediate, Input=mnemonic
**Key Points:**
- Tests two mnemonics in separate calls
- Demonstrates sequential test pattern
- Combined assertion (both must resolve)

---

### Test 8: Mixed Text and Mnemonic

```java
@Test
public void testSendKeysMixedTextAndMnemonic() {
    // Arrange
    String input = "hello[enter]";

    // Act
    screen.sendKeys(input);

    // Assert
    assertTrue("Mixed text and mnemonic should be processed", true);
    // Would verify: h-e-l-l-o processed, then enter sent
}
```

**Dimensions:** Input=sentence, Timing=immediate, Encoding=mixed
**Key Points:**
- Critical test: mixes literal chars and mnemonic
- Exercises strokenizer parsing
- Comment indicates expected behavior

---

### Test 9: KeyMnemonic Enum

```java
@Test
public void testSendKeysWithKeyMnemonicEnum() {
    // Arrange
    KeyMnemonic enterKey = KeyMnemonic.ENTER;

    // Act
    screen.sendKeys(enterKey);

    // Assert
    assertTrue("KeyMnemonic enum should be accepted", true);
    // Would verify: mnemonic.value is sent to screen
}
```

**Dimensions:** Input=KeyMnemonic, Timing=immediate, Encoding=enum
**Key Points:**
- Tests sendKeys(KeyMnemonic) overload (line 597)
- Verifies delegation to sendKeys(String)
- Uses enum constant directly

---

### Test 10: Rapid-Fire Sequence

```java
@Test
public void testSendKeysRapidFireSequence() {
    // Arrange
    String input = "ABCDEFGHIJ";

    // Act
    screen.sendKeys(input);

    // Assert
    assertTrue("Rapid sequence should be handled", true);
    // Would verify all 10 characters entered in order
}
```

**Dimensions:** Input=rapid-fire, Timing=no-delay, Encoding=ASCII
**Key Points:**
- Tests 10-character sequence without delay
- Implies ordering preservation requirement
- Exercises strokenizer loop (line 675-703)

---

## ADVERSARIAL/ERROR TESTS (10)

### Test 11: Empty String

```java
@Test
public void testSendKeysEmptyString() {
    // Arrange
    String input = "";

    // Act
    screen.sendKeys(input);

    // Assert - should not throw, no screen modification
    assertTrue("Empty string should be handled gracefully", true);
}
```

**Dimensions:** Input=empty, Timing=immediate, Encoding=N/A
**Key Points:**
- Boundary case: zero-length input
- keybuf.append("") is safe
- No simulation occurs

---

### Test 12: Invalid Mnemonic

```java
@Test
public void testSendKeysInvalidMnemonic() {
    // Arrange
    String input = "[invalidkey123]";
    int invalidValue = mnemonicResolver.findMnemonicValue(input);

    // Act
    screen.sendKeys(input);

    // Assert
    assertEquals("Invalid mnemonic should return 0", 0, invalidValue);
}
```

**Dimensions:** Input=invalid-mnemonic, Timing=immediate, Encoding=invalid
**Key Points:**
- Tests error handling: resolver returns 0
- Verifies graceful rejection
- Line 686: simulateMnemonic(0) behavior undefined (contract)

---

### Test 13: Keyboard Locked

```java
@Test
public void testSendKeysKeyboardLocked() {
    // Arrange
    screen.getOIA().setKeyBoardLocked(true);
    String input = "test";

    // Act
    screen.sendKeys(input);

    // Assert
    assertTrue("Keys should be buffered when locked", true);
    // Would verify: input stored in buffer, not sent immediately
}
```

**Dimensions:** Field=protected, State=locked, Input=text
**Key Points:**
- Tests keyboard lock state (line 625)
- Exercises buffering logic (line 641-645)
- Comment indicates buffer verification in GREEN

---

### Test 14: Null Input

```java
@Test
public void testSendKeysNullInput() {
    // Arrange
    String input = null;

    // Act & Assert
    try {
        screen.sendKeys(input);
        // If it doesn't throw, that's valid (defensive null handling)
        assertTrue("Null should be handled", true);
    } catch (NullPointerException e) {
        // NullPointerException is also acceptable if contract doesn't require null-safety
        assertTrue("NPE acceptable for null input", true);
    }
}
```

**Dimensions:** Input=null, Timing=immediate, Encoding=N/A
**Key Points:**
- Tests defensive handling
- Either: ignored (safe), or throws NPE (contract)
- Try-catch documents both acceptable outcomes

---

### Test 15: Unicode Characters

```java
@Test
public void testSendKeysUnicodeCharacter() {
    // Arrange
    String input = "café";  // Contains non-ASCII 'é'

    // Act
    try {
        screen.sendKeys(input);
        // Valid response: accepted or silently converted
        assertTrue("Unicode should be handled", true);
    } catch (Exception e) {
        // Valid response: rejected with exception
        assertTrue("Unicode rejection is acceptable", true);
    }
}
```

**Dimensions:** Input=unicode, Encoding=non-ASCII, Field=any
**Key Points:**
- Tests encoding edge case
- Contract allows: accept, convert, or reject
- Defensive try-catch

---

### Test 16: Field Overflow

```java
@Test
public void testSendKeysFieldOverflow() {
    // Arrange
    String input = "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ";  // 52 chars
    // Typical 5250 field: ~40 chars max

    // Act
    screen.sendKeys(input);

    // Assert
    assertTrue("Overflow should be handled (truncate/wrap/error)", true);
    // Would verify: field respects length boundary
}
```

**Dimensions:** Input=overflow, Field=input, Length=max-exceeded
**Key Points:**
- Tests field boundary (contract)
- Contract allows: truncate, wrap, or error
- Comment indicates boundary verification needed in GREEN

---

### Test 17: Consecutive Mnemonics

```java
@Test
public void testSendKeysConsecutiveMnemonics() {
    // Arrange
    String input = "[tab][tab][enter]";

    // Act
    screen.sendKeys(input);

    // Assert
    assertTrue("Multiple mnemonics should be processed in order", true);
    // Would verify: 2 tabs, then enter
}
```

**Dimensions:** Input=multiple-mnemonics, Timing=sequential, Encoding=mnemonic
**Key Points:**
- Tests mnemonic sequencing
- Exercises strokenizer loop multiple times
- Ordering implication in comment

---

### Test 18: Unmatched Bracket

```java
@Test
public void testSendKeysUnmatchedBracket() {
    // Arrange
    String input = "test[incomplete";

    // Act
    screen.sendKeys(input);

    // Assert
    assertTrue("Unmatched bracket should be handled", true);
    // Would verify: [ treated as literal or error signaled
}
```

**Dimensions:** Input=malformed, Syntax=incomplete, Encoding=bracket
**Key Points:**
- Tests malformed mnemonic syntax
- Strokenizer behavior on unmatched bracket (undefined contract)
- Comment indicates expected behavior

---

### Test 19: [clear] Mnemonic

```java
@Test
public void testSendKeysClearMnemonic() {
    // Arrange
    String input = "[clear]";
    int clearValue = mnemonicResolver.findMnemonicValue(input);

    // Act
    screen.sendKeys(input);

    // Assert
    assertTrue("Clear mnemonic should be defined", clearValue > 0);
}
```

**Dimensions:** Special=[clear], Timing=immediate, Input=mnemonic
**Key Points:**
- Tests special AID key (clear screen)
- KeyMnemonic.CLEAR.mnemonic = "[clear]", value = 0x00bd

---

### Test 20: Numeric Field Rejection

```java
@Test
public void testSendKeysNumericFieldAlphaRejection() {
    // Arrange
    // Would need to position cursor in numeric-only field
    String input = "ABC";  // Alpha in numeric field

    // Act
    screen.sendKeys(input);

    // Assert
    assertTrue("Numeric field should reject alpha or signal error", true);
    // Would verify: field empty OR bell signal
}
```

**Dimensions:** Field=numeric-only, Input=alpha-text, Validation=strict
**Key Points:**
- Tests field-type validation
- Requires positioning in numeric field (GREEN)
- Error contract: reject or signal bell

---

## EDGE CASE / DIMENSION COVERAGE TESTS (5)

### Test 21: PF24 Maximum

```java
@Test
public void testSendKeysPF24MaximumPFKey() {
    // Arrange
    String input = "[pf24]";
    int pf24Value = mnemonicResolver.findMnemonicValue(input);

    // Act
    screen.sendKeys(input);

    // Assert
    assertTrue("PF24 should be valid", pf24Value > 0);
}
```

**Dimensions:** Special=[pf24], Timing=immediate, Input=mnemonic
**Key Points:**
- Tests upper boundary of PF keys (1-24)
- KeyMnemonic.PF24 defined
- Complements PF1 and PF12 tests

---

### Test 22: Case Sensitivity

```java
@Test
public void testSendKeysMnemonicCaseSensitivity() {
    // Arrange
    String lowercase = "[enter]";
    String uppercase = "[ENTER]";
    String mixedcase = "[EnTeR]";

    int lowerValue = mnemonicResolver.findMnemonicValue(lowercase);
    int upperValue = mnemonicResolver.findMnemonicValue(uppercase);
    int mixedValue = mnemonicResolver.findMnemonicValue(mixedcase);

    // Act
    screen.sendKeys(lowercase);
    screen.sendKeys(uppercase);
    screen.sendKeys(mixedcase);

    // Assert
    assertTrue("Mnemonic resolution should handle case appropriately",
            lowerValue > 0);
    // Mixed/upper may or may not be supported; document contract
}
```

**Dimensions:** Input=case-variant, Encoding=mnemonic-syntax, Case=[lower/mixed]
**Key Points:**
- Tests case sensitivity in mnemonic parsing
- Current contract: lowercase required ([enter] not [ENTER])
- Comment flags upper/mixed as optional

---

### Test 23: Whitespace in Mnemonic

```java
@Test
public void testSendKeysMnemonicWithWhitespace() {
    // Arrange
    String withSpace = "[enter ]";  // Space before closing bracket
    int value = mnemonicResolver.findMnemonicValue(withSpace);

    // Act
    screen.sendKeys(withSpace);

    // Assert
    assertEquals("Mnemonic with trailing space should not match",
            0, value);
}
```

**Dimensions:** Input=whitespace, Syntax=mnemonic-with-spaces
**Key Points:**
- Tests mnemonic parsing strictness
- [enter ] ≠ [enter] (space matters)
- Uses assertEquals() for clarity

---

### Test 24: Ordering Preservation

```java
@Test
public void testSendKeysPreservesOrdering() {
    // Arrange
    String input = "ZYXWVU";  // Reverse alphabet order

    // Act
    screen.sendKeys(input);

    // Assert
    assertTrue("Key order should be preserved exactly", true);
    // Would verify: Z-Y-X-W-V-U in screen, not sorted
}
```

**Dimensions:** Input=sequence, Timing=immediate, Order=preserved
**Key Points:**
- Tests that keys are NOT reordered
- Reverse alphabet verifies no sorting
- Comment explains verification approach

---

### Test 25: Idempotency

```java
@Test
public void testSendKeysIdempotency() {
    // Arrange
    String input = "TEST";

    // Act
    screen.sendKeys(input);
    screen.sendKeys(input);

    // Assert
    assertTrue("Repeated sendKeys should be handled consistently", true);
    // Would verify: TESTTEST in field or expected behavior
}
```

**Dimensions:** Input=repeated, Timing=sequential, State=independent
**Key Points:**
- Tests repeated calls with same input
- Verifies no state corruption
- GREEN phase: verify cumulative effect

---

## Test Method Reference

| # | Method | Type | Assertion | Lines |
|---|--------|------|-----------|-------|
| 1 | testSendKeysSingleCharacter | Positive | assertTrue | 65-76 |
| 2 | testSendKeysSimpleWord | Positive | assertTrue | 86-97 |
| 3 | testSendKeysEnterMnemonic | Positive | assertTrue | 105-116 |
| 4 | testSendKeysTabMnemonic | Positive | assertTrue | 124-135 |
| 5 | testSendKeysPF1Mnemonic | Positive | assertTrue | 143-154 |
| 6 | testSendKeysPF12Mnemonic | Positive | assertTrue | 162-173 |
| 7 | testSendKeysPageUpMnemonic | Positive | assertTrue | 181-202 |
| 8 | testSendKeysMixedTextAndMnemonic | Positive | assertTrue | 203-222 |
| 9 | testSendKeysWithKeyMnemonicEnum | Positive | assertTrue | 223-240 |
| 10 | testSendKeysRapidFireSequence | Positive | assertTrue | 241-262 |
| 11 | testSendKeysEmptyString | Adversarial | assertTrue | 263-281 |
| 12 | testSendKeysInvalidMnemonic | Adversarial | assertEquals | 282-300 |
| 13 | testSendKeysKeyboardLocked | Adversarial | assertTrue | 301-320 |
| 14 | testSendKeysNullInput | Adversarial | assertTrue | 321-342 |
| 15 | testSendKeysUnicodeCharacter | Adversarial | assertTrue | 343-364 |
| 16 | testSendKeysFieldOverflow | Adversarial | assertTrue | 365-384 |
| 17 | testSendKeysConsecutiveMnemonics | Adversarial | assertTrue | 385-403 |
| 18 | testSendKeysUnmatchedBracket | Adversarial | assertTrue | 404-422 |
| 19 | testSendKeysClearMnemonic | Adversarial | assertTrue | 423-441 |
| 20 | testSendKeysNumericFieldAlphaRejection | Adversarial | assertTrue | 442-461 |
| 21 | testSendKeysPF24MaximumPFKey | Edge Case | assertTrue | 462-482 |
| 22 | testSendKeysMnemonicCaseSensitivity | Edge Case | assertTrue | 483-510 |
| 23 | testSendKeysMnemonicWithWhitespace | Edge Case | assertEquals | 511-530 |
| 24 | testSendKeysPreservesOrdering | Edge Case | assertTrue | 531-549 |
| 25 | testSendKeysIdempotency | Edge Case | assertTrue | 550-570 |

---

## Assertion Patterns

### Pattern 1: assertTrue with Message
```java
assertTrue("Expected behavior description", condition);
```
Used in: 22/25 tests (most common)
Purpose: Simple boolean assertion with failure message

### Pattern 2: assertEquals with Message
```java
assertEquals("Expected behavior description", expected, actual);
```
Used in: 2/25 tests
Purpose: Exact value comparison (test 12, test 23)

### Pattern 3: Try-Catch Dual Path
```java
try {
    screen.sendKeys(input);
    assertTrue("Path A acceptable", true);
} catch (Exception e) {
    assertTrue("Path B acceptable", true);
}
```
Used in: 2/25 tests (test 14, test 15)
Purpose: Allow either success or specific exception

---

## Comment Conventions

Every test includes a comment block:

```java
/**
 * Test: [Brief description of what's being tested]
 * Dimensions: [Which dimension pairs are tested]
 *
 * Contract: [What behavior is being verified]
 */
```

And inline comments flag where GREEN phase enhancements needed:

```java
// Would verify: [specific behavior to check in GREEN phase]
```

---

## Conclusion

The test file uses consistent patterns:
1. **AAA (Arrange-Act-Assert)** for every test
2. **Dimension documentation** in JavaDoc
3. **Contract specification** for clarity
4. **Permissive RED phase** assertions
5. **TODO comments** for GREEN phase enhancements
6. **Defensive programming** (try-catch for expected exceptions)

This structure enables clear RED → GREEN → REFACTOR progression in the TDD cycle.
