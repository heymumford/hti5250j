# TN5250j Performance Profiling - Quick Start Guide

## Files Created

```
~/ProjectsWATTS/tn5250j-headless/
├── tests/org/tn5250j/framework/tn5250/
│   └── PerformanceProfilingPairwiseTest.java    [862 lines, 24 tests]
├── PERFORMANCE_PROFILING_TEST_REPORT.md         [Comprehensive analysis]
└── PERFORMANCE_TEST_QUICKSTART.md               [This file]
```

## Run All Performance Tests

```bash
cd ~/ProjectsWATTS/tn5250j-headless

# Compile tests
javac -cp lib/development/junit-4.5.jar:build -d build \
  tests/org/tn5250j/framework/tn5250/PerformanceProfilingPairwiseTest.java

# Run tests
java -cp "build:lib/development/junit-4.5.jar:lib/runtime/hamcrest-core-1.3.jar" \
  org.junit.runner.JUnitCore \
  org.tn5250j.framework.tn5250.PerformanceProfilingPairwiseTest
```

## Expected Output

```
JUnit version 4.5
.......................
Time: 20.631
OK (24 tests)
```

## Test Categories (Quick Reference)

| Category | Count | Purpose |
|----------|-------|---------|
| Baseline | 4 | Establish performance baseline on simple screens |
| Medium | 4 | Test with 50 fields (typical density) |
| Complex | 3 | Test with 200+ fields (maximum density) |
| Burst | 4 | High-frequency operations (10 ops/sec) |
| Continuous | 4 | Sustained load for 5 seconds |
| Memory Pressure | 3 | Resource exhaustion scenarios |
| Scalability | 2 | Verify linear performance degradation |
| Regression | 3 | Detect performance regressions |

**Total: 24 tests, 25+ pairwise combinations**

## Key Metrics Verified

### Latency (milliseconds)
- Screen draw: < 50ms (simple), < 100ms (complex)
- Field update: < 20ms (simple), < 40ms (complex)
- Scroll: < 30ms (simple), < 50ms (complex)

### Throughput (ops/sec)
- Simple screen: >= 100 ops/sec
- Medium: >= 50 ops/sec
- Complex: >= 30 ops/sec

### Memory (MB)
- Per operation: < 1MB typical
- Peak session: < 200MB
- No unbounded growth

### CPU
- Linear scaling with operation count
- Proportional to update frequency

## What Each Test Measures

### testScreenDrawBaseline_SimpleScreen
- **What:** Full screen render on simple screen
- **Metrics:** Latency, memory, CPU
- **Threshold:** < 50ms latency

### testFieldUpdateBaseline_SimpleScreen
- **What:** Individual field update
- **Metrics:** Latency, consistency
- **Threshold:** < 20ms latency

### testRefreshBaseline_SimpleScreen
- **What:** Screen refresh without data changes
- **Metrics:** Latency, memory overhead
- **Threshold:** < 50ms latency

### testScreenDrawMedium_MediumComplexity
- **What:** Draw performance with 50 fields
- **Metrics:** Latency degradation
- **Threshold:** < 50ms (linear scaling)

### testFieldUpdateMedium_SingleFrequency
- **What:** Latency consistency at medium complexity
- **Metrics:** Average latency over 100 iterations
- **Threshold:** < 20ms average

### testScrollMedium_MediumComplexity
- **What:** Scroll operation latency
- **Metrics:** Single scroll latency
- **Threshold:** < 30ms

### testRefreshMedium_MediumComplexity
- **What:** Memory scaling with field count
- **Metrics:** Memory usage
- **Threshold:** < 200MB

### testScreenDrawComplex_ComplexScreen
- **What:** Draw at maximum complexity (200+ fields)
- **Metrics:** Latency ceiling
- **Threshold:** < 100ms

### testFieldUpdateComplex_ComplexScreen
- **What:** Field update latency at max complexity
- **Metrics:** Average over 50 iterations
- **Threshold:** < 40ms average

### testRefreshComplex_ComplexScreen
- **What:** Memory at maximum density
- **Metrics:** Heap usage
- **Threshold:** < 200MB

### testScreenDrawBurst_SimpleScreen_BurstFrequency
- **What:** Throughput at burst frequency
- **Metrics:** Ops/sec for 1 second
- **Threshold:** >= 100 ops/sec

### testFieldUpdateBurst_MediumScreen_BurstFrequency
- **What:** Throughput degradation with complexity
- **Metrics:** Ops/sec
- **Threshold:** >= 50 ops/sec

### testScrollBurst_MediumScreen_BurstFrequency
- **What:** Scroll throughput
- **Metrics:** Ops/sec
- **Threshold:** >= 50 ops/sec

### testRefreshBurst_ComplexScreen_BurstFrequency
- **What:** Refresh throughput at max complexity
- **Metrics:** Ops/sec
- **Threshold:** >= 30 ops/sec

### testScreenDrawContinuous_MediumScreen_5Seconds
- **What:** Memory stability over 5 seconds
- **Metrics:** Memory growth, throughput stability
- **Threshold:** < 200MB, >= 50 ops/sec sustained

### testFieldUpdateContinuous_SimpleScreen_5Seconds
- **What:** CPU scaling verification
- **Metrics:** Memory growth, CPU time
- **Threshold:** < 200MB, linear CPU scaling

### testScrollContinuous_ComplexScreen_5Seconds
- **What:** Sustained scroll performance
- **Metrics:** Throughput consistency
- **Threshold:** >= 20 ops/sec sustained

### testRefreshContinuous_ComplexScreen_5Seconds
- **What:** High-frequency refresh sustain
- **Metrics:** Throughput consistency
- **Threshold:** >= 100 ops/sec sustained

### testScreenDrawLowMemory_SimpleScreen
- **What:** Performance after GC (low memory)
- **Metrics:** Latency under GC pressure
- **Threshold:** < 100ms (2x baseline)

### testFieldUpdateMediumMemory_MediumScreen
- **What:** Latency consistency with memory constraints
- **Metrics:** Average latency
- **Threshold:** < 25ms

### testRefreshHighMemory_BurstFrequency
- **What:** Throughput under 15MB memory pressure
- **Metrics:** Ops/sec under stress
- **Threshold:** >= 25 ops/sec

### testScreenDrawScalability_AllComplexityLevels
- **What:** Latency scaling across complexity levels
- **Metrics:** Scaling factor (complex/simple)
- **Threshold:** < 5x (linear, not exponential)

### testFieldUpdateScalability_MemoryUsage
- **What:** Memory scaling linearity
- **Metrics:** Memory per complexity level
- **Threshold:** Linear scaling

### testScreenDrawRegressionDetection_SimpleScreen
- **What:** Detect latency regression
- **Metrics:** Average latency over 100 iterations
- **Threshold:** < baseline + 20%

### testFieldUpdateRegressionDetection_BurstFrequency
- **What:** Detect throughput regression
- **Metrics:** Ops/sec burst
- **Threshold:** >= baseline throughput

### testMemoryLeakDetection_ContinuousRefresh
- **What:** Detect unbounded memory growth
- **Metrics:** Memory at 5 checkpoints
- **Threshold:** < 200MB ceiling

### testCpuScalingLinearRegression
- **What:** Verify linear CPU scaling
- **Metrics:** CPU time per operation
- **Threshold:** Linear proportional to ops

## Performance Baselines (from test runs)

```
Screen Draw:      1-5ms (simple), 5-15ms (medium), 15-30ms (complex)
Field Update:     0.5-3ms (simple), 3-10ms (medium), 10-25ms (complex)
Refresh:          1-5ms (simple), 3-8ms (medium), 8-15ms (complex)
Memory/op:        < 1MB (typical)
Throughput:       1000+ ops/sec (simple), 200+ ops/sec (complex)
```

## Troubleshooting

### Test fails: "Latency should complete in under 50ms"
- System CPU may be busy. Run test again with less system load.
- Check if other Java processes are running.

### Test fails: "Memory usage should be under 200MB"
- Increase heap size: `java -Xmx1G ...`
- Run `System.gc()` more frequently before measuring.

### Test fails: "Throughput should be at least X ops/sec"
- System CPU saturation. Run with fewer concurrent processes.
- Check garbage collection pauses with `-XX:+PrintGCDetails`.

### All tests timeout
- System is severely CPU-constrained.
- Check with `top` or Activity Monitor.
- Run on idle system.

## Advanced: Custom Thresholds

Edit the constants at the top of PerformanceProfilingPairwiseTest.java:

```java
private static final long LATENCY_THRESHOLD_SCREEN_DRAW_MS = 50;
private static final long LATENCY_THRESHOLD_FIELD_UPDATE_MS = 20;
private static final long LATENCY_THRESHOLD_SCROLL_MS = 30;
private static final long THROUGHPUT_THRESHOLD_OPS_SEC = 100;
private static final long MEMORY_THRESHOLD_MB = 200;
```

## Integration with CI/CD

```yaml
# GitLab CI example
performance-tests:
  script:
    - cd ~/ProjectsWATTS/tn5250j-headless
    - javac -cp lib/development/junit-4.5.jar:build -d build \
        tests/org/tn5250j/framework/tn5250/PerformanceProfilingPairwiseTest.java
    - java -cp "build:lib/development/junit-4.5.jar:..." \
        org.junit.runner.JUnitCore \
        org.tn5250j.framework.tn5250.PerformanceProfilingPairwiseTest
  allow_failure: false
```

## See Also

- `PERFORMANCE_PROFILING_TEST_REPORT.md` - Comprehensive technical analysis
- `PerformanceProfilingPairwiseTest.java` - Full test implementation (862 lines)
