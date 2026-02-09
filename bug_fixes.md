# Bug Fixes - TDD-Based Implementation

## Fix 1: CRITICAL - Percentile Calculation Off-by-One (BatchMetrics.java)

### Bug Description
Lines 61-62 calculate percentile indices incorrectly using floor division. For a sorted list of 10 elements [1..10]:
- Current: p50 index = (10 * 50) / 100 = 5 → value 6 (WRONG)
- Correct: p50 should be at index 4 → value 5 (per nearest rank method)

### Root Cause
Using `(size * pct) / 100` produces floor index, missing off-by-one adjustment for percentiles.

### Fix Code
```java
// BEFORE (lines 61-62)
long p50 = latencies.isEmpty() ? 0 : latencies.get((latencies.size() * 50) / 100);
long p99 = latencies.isEmpty() ? 0 : latencies.get((latencies.size() * 99) / 100);

// AFTER - Use nearest rank method: ceil(p * n) - 1
long p50 = latencies.isEmpty() ? 0 :
    latencies.get(Math.max(0, (int) Math.ceil(latencies.size() * 0.50) - 1));
long p99 = latencies.isEmpty() ? 0 :
    latencies.get(Math.max(0, (int) Math.ceil(latencies.size() * 0.99) - 1));
```

### Test
```java
@Test
void testPercentileCalculationAccuracy() {
    List<WorkflowResult> results = IntStream.range(1, 11)
        .mapToObj(i -> new WorkflowResult(true, i * 100L, new HashMap<>(), null, "success"))
        .toList();

    BatchMetrics metrics = BatchMetrics.from(results, 0, 1000_000_000);

    // For [100, 200, ..., 1000] sorted latencies
    assertEquals(500, metrics.p50LatencyMs());  // Median of 10 values
    assertEquals(1000, metrics.p99LatencyMs()); // 99th percentile
}
```

---

## Fix 2: CRITICAL - Null Dereference (DatasetLoader.java)

### Bug Description
Line 49: `entry.getValue()` can be null (CSV allows null values), causing NullPointerException when passed to String.replace().

### Root Cause
No null check on Map entry values before concatenating with String.replace().

### Fix Code
```java
// BEFORE (line 49)
result = result.replace(placeholder, entry.getValue());

// AFTER - Add null safety check
if (entry.getValue() != null) {
    result = result.replace(placeholder, entry.getValue());
}
```

### Test
```java
@Test
void testReplaceParametersWithNullValues() {
    DatasetLoader loader = new DatasetLoader();
    Map<String, String> data = new HashMap<>();
    data.put("amount", null);
    data.put("account", "12345");

    String template = "Account ${data.account} amount ${data.amount}";
    String result = loader.replaceParameters(template, data);

    // Should handle null gracefully
    assertEquals("Account 12345 amount ${data.amount}", result);
}
```

---

## Fix 3: CRITICAL - Resource Management Verified (WorkflowRunner.java)

### Status: VERIFIED SAFE
Line 212 uses try-with-resources properly:
```java
try (FileWriter writer = new FileWriter(captureFile)) {
    writer.write(screenContent);
}
```
No fix needed - resources are properly closed.

---

## Fix 4: HIGH - Concurrency Safety (DatasetLoader.java)

### Bug Description
DatasetLoader.loadCSV() uses instance state but is called from parallel virtual threads in BatchExecutor. CSVParser state is not thread-safe.

### Root Cause
Stateless utility class called from concurrent contexts without synchronization.

### Fix Code
```java
// BEFORE - Instance method (not thread-safe when called concurrently)
public Map<String, Map<String, String>> loadCSV(File csvFile) throws Exception { ... }

// AFTER - Make static (or synchronize if instance state required)
public static Map<String, Map<String, String>> loadCSV(File csvFile) throws Exception { ... }

// Update callers to use DatasetLoader.loadCSV() instead of instance method
```

### Test
```java
@Test
void testConcurrentCSVLoading() throws Exception {
    File testCSV = createTestCSVFile("test.csv");
    DatasetLoader loader = new DatasetLoader();

    List<Map<String, Map<String, String>>> results =
        IntStream.range(0, 10)
        .parallel()
        .mapToObj(i -> {
            try {
                return loader.loadCSV(testCSV);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })
        .toList();

    // All results should be identical (no concurrent corruption)
    for (int i = 1; i < results.size(); i++) {
        assertEquals(results.get(0), results.get(i));
    }
}
```

---

## Fix 5: HIGH - NPE in WorkflowSimulator (WorkflowSimulator.java)

### Bug Description
Line 98-112: If step.getFields() is non-null but contains entries with null values, NPE on line 103 (fieldValue.length()).

### Root Cause
Null check on Map but not on Map values inside the loop.

### Fix Code
```java
// BEFORE (line 98-112)
if ("FILL".equals(stepName) && step.getFields() != null) {
    for (Map.Entry<String, String> field : step.getFields().entrySet()) {
        String fieldValue = field.getValue();
        if (fieldValue != null && fieldValue.length() > 255) { ... }

// AFTER - Explicit null handling
if ("FILL".equals(stepName) && step.getFields() != null) {
    for (Map.Entry<String, String> field : step.getFields().entrySet()) {
        String fieldValue = field.getValue();
        if (fieldValue == null) {
            warnings.add(String.format("Step %d FILL: field '%s' has null value", i, field.getKey()));
            continue;
        }
        if (fieldValue.length() > 255) { ... }
```

### Test
```java
@Test
void testSimulateWithNullFieldValues() {
    WorkflowSchema workflow = new WorkflowSchema("test", new ArrayList<>());
    StepDef step = new StepDef();
    step.setAction(ActionType.FILL);
    step.setFields(new HashMap<>());
    step.getFields().put("name", null);
    step.getFields().put("amount", "123");
    workflow.getSteps().add(step);

    WorkflowSimulator simulator = new WorkflowSimulator();
    WorkflowSimulation result = simulator.simulate(workflow, new HashMap<>(), WorkflowTolerance.defaults("test"));

    // Should not throw NPE, should warn about null field
    assertTrue(result.warnings().stream().anyMatch(w -> w.contains("null value")));
}
```

---

## Fix 6: HIGH - Exception Context Loss (WorkflowCLI.java)

### Bug Description
Line 70: Generic "Error" message loses exception cause and stack trace in production logs.

### Root Cause
catch-all exception handler doesn't preserve exception details.

### Fix Code
```java
// BEFORE (line 70)
} catch (Exception e) {
    TerminalAdapter.printError("Error", e);
    System.exit(1);
}

// AFTER - Log full context
} catch (Exception e) {
    String context = String.format("Failed at: %s | Action: %s",
        Thread.currentThread().getStackTrace()[2], e.getMessage());
    TerminalAdapter.printError(context, e);
    e.printStackTrace(System.err);  // Preserve stack trace
    System.exit(1);
}
```

### Test
```java
@Test
void testExceptionContextPreservation() {
    // Capture stderr to verify stack trace is printed
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    System.setErr(new PrintStream(stderr));

    try {
        throw new IOException("Test connection failed");
    } catch (Exception e) {
        // Verify exception details are logged
        assertTrue(stderr.toString().contains("IOException"));
        assertTrue(stderr.toString().contains("Test connection failed"));
    }
}
```

---

## Fix 7: HIGH - CSV Batch Detection Logic (WorkflowCLI.java)

### Bug Description
Line 50: `allRows.size() > 1` treats 2-row CSV (1 header + 1 data) as batch mode, but should only activate for 2+ data rows.

### Root Cause
CSV parser includes header row in size count.

### Fix Code
```java
// BEFORE (line 48-50)
Map<String, Map<String, String>> allRows = loader.loadCSV(new File(parsed.dataFile()));
if (allRows.size() > 1) {
    // Batch mode

// AFTER - Account for header row (CSV has size = data rows only, no header)
Map<String, Map<String, String>> allRows = loader.loadCSV(new File(parsed.dataFile()));
if (allRows.size() > 1) {  // This is already correct (parser excludes header)
    // No fix needed - CSVFormat.withFirstRecordAsHeader() removes header from result
```

**Status**: This requires verification of CSVParser behavior. If header is already excluded, no fix needed.

### Test
```java
@Test
void testBatchDetectionWithCSV() throws Exception {
    // Create test CSV with header + 2 data rows
    File testCSV = createTestCSVFile("header1,header2\nval1,val2\nval3,val4");

    DatasetLoader loader = new DatasetLoader();
    Map<String, Map<String, String>> rows = loader.loadCSV(testCSV);

    // Should be exactly 2 rows (header excluded by CSVFormat)
    assertEquals(2, rows.size());
}
```

---

## Fix 8: MEDIUM - Incomplete SUBMIT Validation (StepOrderValidator.java)

### Bug Description
Lines 40-51: Only checks SUBMIT after FILL/NAVIGATE, misses case of SUBMIT as first step (i=0, should be invalid).

### Root Cause
Validation only runs when `i > 0`, skipping first step edge case.

### Fix Code
```java
// BEFORE (lines 40-51)
for (int i = 0; i < steps.size(); i++) {
    StepDef step = steps.get(i);
    if (step.getAction() == ActionType.SUBMIT && i > 0) {  // Misses i=0
        StepDef prevStep = steps.get(i - 1);
        if (prevStep.getAction() != ActionType.FILL && prevStep.getAction() != ActionType.NAVIGATE) {
            result.addWarning(...);
        }
    }
}

// AFTER - Add check for first step
for (int i = 0; i < steps.size(); i++) {
    StepDef step = steps.get(i);
    if (step.getAction() == ActionType.SUBMIT) {
        if (i == 0) {
            result.addError(i, "action", "SUBMIT cannot be first step (must follow LOGIN, FILL, or NAVIGATE)");
        } else {
            StepDef prevStep = steps.get(i - 1);
            if (prevStep.getAction() != ActionType.FILL && prevStep.getAction() != ActionType.NAVIGATE) {
                result.addWarning(...);
            }
        }
    }
}
```

### Test
```java
@Test
void testSUBMITCannotBeFirstStep() {
    List<StepDef> steps = new ArrayList<>();
    StepDef submitStep = new StepDef();
    submitStep.setAction(ActionType.SUBMIT);
    submitStep.setKey("enter");
    steps.add(submitStep);

    StepOrderValidator validator = new StepOrderValidator();
    ValidationResult result = validator.validate(steps);

    assertTrue(result.hasErrors());
    assertTrue(result.errors().stream()
        .anyMatch(e -> e.message().contains("cannot be first")));
}
```

---

## Status Summary

| ID | Status | Severity | Next Step |
|----|--------|----------|-----------|
| 1 | Ready to Fix | CRITICAL | Implement + Test |
| 2 | Ready to Fix | CRITICAL | Implement + Test |
| 3 | Verified Safe | CRITICAL | No Fix Needed |
| 4 | Ready to Fix | HIGH | Implement + Test |
| 5 | Ready to Fix | HIGH | Implement + Test |
| 6 | Ready to Fix | HIGH | Implement + Test |
| 7 | Pending Verification | HIGH | Verify CSVParser Behavior |
| 8 | Ready to Fix | MEDIUM | Implement + Test |

## Cycle 3 Progress
- [ ] Fix #1: BatchMetrics percentile calculation
- [ ] Fix #2: DatasetLoader null dereference
- [ ] Fix #4: DatasetLoader concurrency
- [ ] Fix #5: WorkflowSimulator null field values
- [ ] Fix #6: WorkflowCLI exception context
- [ ] Fix #7: Verify CSV batch detection logic
- [ ] Fix #8: StepOrderValidator SUBMIT validation
- [ ] Run all tests to verify zero regressions
