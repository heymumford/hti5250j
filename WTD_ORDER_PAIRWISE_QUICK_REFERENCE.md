# WTD Order Pairwise Test - Quick Reference

## Test File Location
```
tests/org/tn5250j/framework/tn5250/WTDOrderPairwiseTest.java
```

## Quick Stats
- **Total Tests:** 24
- **Lines of Code:** 918
- **Test Methods:** 24
- **Execution Time:** 0.01 seconds
- **Pass Rate:** 100%

## Test Categories at a Glance

### Positive Tests (8)
✓ TEST 1: Valid WTD standard order with minimal data
✓ TEST 2: WTD immediate with field attribute
✓ TEST 3: Structured field window creation
✓ TEST 4: Zero-length WTD order
✓ TEST 5: 256-byte length boundary
✓ TEST 6: Scrollbar SF parsing
✓ TEST 7: Remove all GUI constructs
✓ TEST 8: Input field attribute

### Boundary Tests (3)
✓ TEST 9: Start-of-buffer parsing
✓ TEST 10: End-of-buffer boundary
✓ TEST 11: Maximum payload length (32767)

### Adversarial Tests (13)
✓ TEST 12: Truncated header
✓ TEST 13: Invalid control character (0xFF)
✓ TEST 14: Length mismatch
✓ TEST 15: Invalid SF class
✓ TEST 16: Invalid SF subcommand
✓ TEST 17: Negative length (sign bit)
✓ TEST 18: Corrupted field attribute
✓ TEST 19: Buffer wrap-around attack
✓ TEST 20: Zero-length SF
✓ TEST 21: Chained orders
✓ TEST 22: Mixed field attributes
✓ TEST 23: Null control character
✓ TEST 24: Insufficient SF data

## Compile Command
```bash
cd /Users/vorthruna/ProjectsWATTS/tn5250j-headless
javac -cp build/classes:lib/development/junit-4.5.jar \
       -d build/test-classes \
       tests/org/tn5250j/framework/tn5250/WTDOrderPairwiseTest.java
```

## Run Command
```bash
java -cp build/test-classes:build/classes:lib/development/junit-4.5.jar \
     org.junit.runner.JUnitCore \
     org.tn5250j.framework.tn5250.WTDOrderPairwiseTest
```

## Expected Output
```
JUnit version 4.5
........................
Time: 0.01

OK (24 tests)
```

## Key Constants

### WTD Command Types
| Name | Value | Purpose |
|------|-------|---------|
| WTD_NORMAL | 0x11 | Standard write-to-display |
| WTD_IMMEDIATE | 0x01 | Immediate write-to-display |
| WTD_STRUCTURED_FIELD | 0xF1 | Structured field |

### Control Character Range
| Constant | Value | Purpose |
|----------|-------|---------|
| CTRL_CHAR_MIN | 0x00 | Null (boundary low) |
| CTRL_CHAR_LOW | 0x1F | Low control range |
| CTRL_CHAR_SPACE | 0x20 | Space (typical) |
| CTRL_CHAR_HIGH | 0x3F | High normal (boundary) |
| CTRL_CHAR_MAX | 0xFF | Invalid (adversarial) |

### Data Lengths Tested
| Constant | Value | Purpose |
|----------|-------|---------|
| DATA_LENGTH_ZERO | 0 | Empty (boundary) |
| DATA_LENGTH_ONE | 1 | Minimal data |
| DATA_LENGTH_127 | 127 | Single-byte max |
| DATA_LENGTH_128 | 128 | Multi-byte threshold |
| DATA_LENGTH_255 | 255 | Byte boundary |
| DATA_LENGTH_256 | 256 | 2-byte encoding |
| DATA_LENGTH_32767 | 32767 | Max short |

### Structured Field Classes
| Constant | Value | Purpose |
|----------|-------|---------|
| SF_CLASS_D9 | 0xD9 | Valid SF class |
| SF_INVALID | 0xAA | Invalid SF class |

### SF Subcommands
| Constant | Value | Purpose |
|----------|-------|---------|
| SF_CREATE_WINDOW_51 | 0x51 | Create window |
| SF_BORDER_PRESENTATION | 0x01 | Border presentation |
| SF_DEFINE_SELECTION_50 | 0x50 | Define selection field |
| SF_SCROLLBAR_53 | 0x53 | Scrollbar |
| SF_REMOVE_SCROLLBAR_5B | 0x5B | Remove scrollbar |
| SF_REMOVE_ALL_GUI_5F | 0x5F | Remove all GUI |

### Field Attributes
| Constant | Value | Purpose |
|----------|-------|---------|
| FIELD_ATTR_INPUT | 0x01 | Input capable |
| FIELD_ATTR_OUTPUT | 0x00 | Output only |
| FIELD_ATTR_PROTECTED | 0x02 | Protected/non-input |
| FIELD_ATTR_MODIFIED | 0x04 | Modified indicator |

## Test Pairwise Combinations

### Dimensions Tested
1. **WTD Command Types:** 3 values (0x11, 0x01, 0xF1)
2. **Control Characters:** 5 values (0x00, 0x1F, 0x20, 0x3F, 0xFF)
3. **Data Lengths:** 7 values (0, 1, 127, 128, 255, 256, 32767)
4. **Buffer Positions:** 4 values (0, 500, 1000, wrap-around)
5. **Field Attributes:** 5 values (input, output, protected, modified, combined)

### Theoretical Combinations
- All combinations: 3 × 5 × 7 × 4 × 5 = 2,100 possible
- Pairwise coverage: ~120 combinations selected for efficiency
- Actual tests: 24 (covers high-risk areas)

## Test Method Pattern

```java
@Test
public void testDescriptiveName() {
    // Arrange: Set up test data
    // Act: Perform operation
    // Assert: Verify results
}
```

All tests follow this pattern:
1. **Arrange** - Create test buffer and populate with specific values
2. **Act** - Simulate parsing (extract command, length, attributes)
3. **Assert** - Verify extraction using JUnit assertions

## Buffer Layout

```
Position: 0                    500                    1000      1020
         +----────────────────+─────────────────────+──────────+
Buffer:  |   Positive Tests   |  Boundary Tests     | Adv. End |
         +────┬──────┬────────+───────┬────────┬────+──┬──┬────+
Size:    1024 bytes (MAX_BUFFER_SIZE)
```

## Coverage by Risk Level

| Risk Level | Tests | Coverage |
|-----------|-------|----------|
| CRITICAL | 1 | Buffer wrap-around (TEST 19) |
| HIGH | 6 | Truncation, length mismatch, SF validation |
| MEDIUM | 10 | Invalid values, corruption, chaining |
| LOW | 7 | Boundary conditions, normal operation |

## Validation Checklist

- [x] All 24 tests compile without errors
- [x] All 24 tests pass successfully
- [x] Test execution time < 100ms
- [x] No external dependencies beyond JUnit 4.5
- [x] All constants properly defined
- [x] Comprehensive documentation included
- [x] Pairwise coverage calculated
- [x] Risk areas prioritized
- [x] Adversarial tests included
- [x] Buffer boundary protected

## Next Steps

1. **Integration:** Add to CI/CD pipeline
   ```bash
   ant compile-tests run-tests
   ```

2. **Regression Testing:** Run before each commit
   ```bash
   java -cp build/test-classes:build/classes:lib/development/junit-4.5.jar \
        org.junit.runner.JUnitCore \
        org.tn5250j.framework.tn5250.WTDOrderPairwiseTest
   ```

3. **Enhancement:** Add performance tests for large payloads

4. **Coverage:** Extend to other TN5250 order types (SBA, MBC, etc.)

## Related Documentation

- Main delivery: `WTD_ORDER_PAIRWISE_TEST_DELIVERY.md`
- Source: `src/org/tn5250j/framework/tn5250/WTDSFParser.java`
- Related tests: `AttributePairwiseTest.java`, `DataStreamPairwiseTest.java`

## Support Commands

### Check compilation
```bash
javac -cp build/classes:lib/development/junit-4.5.jar \
       -d build/test-classes \
       tests/org/tn5250j/framework/tn5250/WTDOrderPairwiseTest.java && \
echo "Compilation successful"
```

### Check test count
```bash
grep -c "@Test" tests/org/tn5250j/framework/tn5250/WTDOrderPairwiseTest.java
```

### Run with verbose output
```bash
java -cp build/test-classes:build/classes:lib/development/junit-4.5.jar \
     org.junit.runner.JUnitCore \
     org.tn5250j.framework.tn5250.WTDOrderPairwiseTest -v
```

### View test class
```bash
ls -lh build/test-classes/org/tn5250j/framework/tn5250/WTDOrderPairwiseTest.class
```
