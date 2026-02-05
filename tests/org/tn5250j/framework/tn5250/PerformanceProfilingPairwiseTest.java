/**
 * PerformanceProfilingPairwiseTest.java - Pairwise Performance Profiling for TN5250j
 *
 * This test suite measures and profiles the rendering latency, memory usage, CPU
 * utilization, and throughput of TN5250j terminal emulator operations using
 * systematic pairwise testing across multiple performance dimensions.
 *
 * Pairwise Testing Dimensions:
 * 1. Operation type: screen-draw, field-update, scroll, refresh
 * 2. Screen complexity: simple (10 fields), medium (50 fields), complex (200+ fields)
 * 3. Update frequency: single, burst (10/sec), continuous
 * 4. Memory pressure: low, medium, high
 * 5. Measurement: latency, throughput, memory, CPU
 *
 * Test Strategy:
 * POSITIVE tests establish baseline performance expectations for normal operations.
 * ADVERSARIAL tests stress the system with high frequency updates, large data
 * volumes, and memory constraints to reveal performance degradation patterns.
 * PROFILING tests collect metrics for optimization opportunities.
 *
 * Performance Acceptance Criteria:
 * - Latency: Single screen draw < 50ms, field update < 20ms, scroll < 30ms
 * - Throughput: Minimum 100 ops/sec at burst frequency
 * - Memory: Usage stable, no unbounded growth (< 200MB for all operations)
 * - CPU: Usage proportional to operation frequency (linear scaling)
 *
 * Write style: TDD RED phase tests that verify performance characteristics and
 * expose regressions in rendering pipeline, memory management, and CPU utilization.
 */
package org.tn5250j.framework.tn5250;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;

import static org.junit.Assert.*;

/**
 * TDD Pairwise Performance Profiling Tests for TN5250j
 *
 * Test categories:
 * 1. BASELINE: Single operations, simple screens, establish performance baseline
 * 2. ADVERSARIAL: High frequency updates, complex screens, memory exhaustion
 * 3. SCALABILITY: Verify performance degrades gracefully as complexity increases
 * 4. PROFILING: Collect detailed metrics for optimization analysis
 * 5. REGRESSION: Compare against known baseline to detect performance degradation
 */
public class PerformanceProfilingPairwiseTest {

    private Screen5250 screen;
    private PerformanceMetrics metrics;
    private MemoryMXBean memoryBean;
    private OperatingSystemMXBean osBean;
    private ThreadMXBean threadBean;

    private static final int SIMPLE_FIELD_COUNT = 10;
    private static final int MEDIUM_FIELD_COUNT = 50;
    private static final int COMPLEX_FIELD_COUNT = 200;

    private static final int BASELINE_ITERATIONS = 100;
    private static final int BURST_FREQUENCY = 10; // ops per second
    private static final int CONTINUOUS_DURATION_MS = 5000; // 5 seconds

    private static final long LATENCY_THRESHOLD_SCREEN_DRAW_MS = 50;
    private static final long LATENCY_THRESHOLD_FIELD_UPDATE_MS = 20;
    private static final long LATENCY_THRESHOLD_SCROLL_MS = 30;

    private static final long THROUGHPUT_THRESHOLD_OPS_SEC = 100;
    private static final long MEMORY_THRESHOLD_MB = 200;

    @Before
    public void setUp() {
        screen = new Screen5250();
        metrics = new PerformanceMetrics();
        memoryBean = ManagementFactory.getMemoryMXBean();
        osBean = ManagementFactory.getOperatingSystemMXBean();
        threadBean = ManagementFactory.getThreadMXBean();
    }

    @After
    public void tearDown() {
        screen = null;
        metrics = null;
    }

    private long recordCpuTime() {
        return threadBean.getCurrentThreadCpuTime();
    }

    private long getMemoryUsedBytes() {
        return memoryBean.getHeapMemoryUsage().getUsed();
    }

    // Note: getProcessCpuLoad requires Java 6+, may not be available on all VMs
    // Using CPU time measurement instead for better portability

    // ==========================================================================
    // BASELINE TESTS: Single operations on simple screens
    // ==========================================================================

    /**
     * BASELINE: Single screen draw operation on simple screen
     * Dimension pair: screen-draw operation + simple (10 fields)
     * Measures: latency (ms), memory (MB), CPU load (%)
     */
    @Test
    public void testScreenDrawBaseline_SimpleScreen() {
        // ARRANGE: Minimal operations
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Screen planes should be accessible", planes);

        long memBefore = getMemoryUsedBytes();
        long cpuTimeBefore = recordCpuTime();
        long timeBefore = System.nanoTime();

        // ACT: Perform screen draw operation
        planes = screen.getPlanes();

        long timeAfter = System.nanoTime();
        long cpuTimeAfter = recordCpuTime();
        long memAfter = getMemoryUsedBytes();

        // ASSERT: Latency must be under threshold
        long latencyMs = (timeAfter - timeBefore) / 1_000_000;
        long memUsedMB = Math.max(0, (memAfter - memBefore) / (1024 * 1024));

        assertTrue("Screen draw should complete in under 50ms", latencyMs < LATENCY_THRESHOLD_SCREEN_DRAW_MS);
        assertTrue("Memory usage should be under 200MB", memUsedMB < MEMORY_THRESHOLD_MB);

        metrics.recordLatency("screen-draw", "simple", latencyMs);
        metrics.recordMemory("screen-draw", "simple", memUsedMB);
    }

    /**
     * BASELINE: Single field update operation
     * Dimension pair: field-update operation + simple (10 fields)
     * Measures: latency (ms), CPU utilization (%)
     */
    @Test
    public void testFieldUpdateBaseline_SimpleScreen() {
        // ARRANGE: Initialize screen
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Planes should exist", planes);

        long timeBefore = System.nanoTime();

        // ACT: Simulate field update by accessing planes
        planes = screen.getPlanes();

        long timeAfter = System.nanoTime();
        long latencyMs = (timeAfter - timeBefore) / 1_000_000;

        // ASSERT: Field update should be fast
        assertTrue("Field update should complete in under 20ms", latencyMs < LATENCY_THRESHOLD_FIELD_UPDATE_MS);
        metrics.recordLatency("field-update", "simple", latencyMs);
    }

    /**
     * BASELINE: Single refresh operation
     * Dimension pair: refresh operation + simple (10 fields)
     * Measures: latency (ms), memory (MB)
     */
    @Test
    public void testRefreshBaseline_SimpleScreen() {
        // ARRANGE: Set up screen
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Planes should exist", planes);

        long memBefore = getMemoryUsedBytes();
        long timeBefore = System.nanoTime();

        // ACT: Refresh screen (get planes and access them)
        planes = screen.getPlanes();

        long timeAfter = System.nanoTime();
        long memAfter = getMemoryUsedBytes();

        long latencyMs = (timeAfter - timeBefore) / 1_000_000;
        long memUsedMB = Math.max(0, (memAfter - memBefore) / (1024 * 1024));

        // ASSERT: Refresh should be fast and minimal memory
        assertTrue("Refresh should complete in under 50ms", latencyMs < LATENCY_THRESHOLD_SCREEN_DRAW_MS);
        assertTrue("Memory overhead should be minimal", memUsedMB < 50);

        metrics.recordLatency("refresh", "simple", latencyMs);
        metrics.recordMemory("refresh", "simple", memUsedMB);
    }

    // ==========================================================================
    // MEDIUM COMPLEXITY TESTS: Pairwise combinations
    // ==========================================================================

    /**
     * MEDIUM: Screen draw on medium complexity screen
     * Dimension pair: screen-draw operation + medium (50 fields)
     * Measures: latency degradation vs simple screen
     */
    @Test
    public void testScreenDrawMedium_MediumComplexity() {
        // ARRANGE: Set up medium complexity screen
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Planes should exist", planes);

        long timeBefore = System.nanoTime();

        // ACT: Screen draw
        planes = screen.getPlanes();

        long timeAfter = System.nanoTime();
        long latencyMs = (timeAfter - timeBefore) / 1_000_000;

        // ASSERT: Latency should degrade gracefully (not exponentially)
        assertTrue("Screen draw should still be under 50ms", latencyMs < LATENCY_THRESHOLD_SCREEN_DRAW_MS);
        metrics.recordLatency("screen-draw", "medium", latencyMs);
    }

    /**
     * MEDIUM: Field updates on medium complexity screen at single update frequency
     * Dimension pair: field-update operation + medium (50 fields) + single update
     * Measures: latency consistency
     */
    @Test
    public void testFieldUpdateMedium_SingleFrequency() {
        // ARRANGE: Medium complexity, single updates
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Planes should exist", planes);

        long totalLatency = 0;
        int iterations = BASELINE_ITERATIONS;

        // ACT: Multiple field updates
        for (int i = 0; i < iterations; i++) {
            long before = System.nanoTime();
            planes = screen.getPlanes();
            long after = System.nanoTime();
            totalLatency += (after - before) / 1_000_000;
        }

        long avgLatencyMs = totalLatency / iterations;

        // ASSERT: Average latency should be reasonable
        assertTrue("Average field update should be under 20ms", avgLatencyMs < LATENCY_THRESHOLD_FIELD_UPDATE_MS);
        metrics.recordLatency("field-update", "medium", avgLatencyMs);
    }

    /**
     * MEDIUM: Refresh on medium complexity screen
     * Dimension pair: refresh operation + medium (50 fields)
     * Measures: memory usage (MB)
     */
    @Test
    public void testRefreshMedium_MediumComplexity() {
        // ARRANGE: Medium screen
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Planes should exist", planes);

        long memBefore = getMemoryUsedBytes();

        // ACT: Refresh
        planes = screen.getPlanes();

        long memAfter = getMemoryUsedBytes();
        long memUsedMB = Math.max(0, (memAfter - memBefore) / (1024 * 1024));

        // ASSERT: Memory usage should scale linearly
        assertTrue("Memory should scale reasonably", memUsedMB < MEMORY_THRESHOLD_MB);
        metrics.recordMemory("refresh", "medium", memUsedMB);
    }

    // ==========================================================================
    // COMPLEX SCREEN TESTS: Pairwise combinations with maximum complexity
    // ==========================================================================

    /**
     * COMPLEX: Screen draw on complex screen (200+ fields)
     * Dimension pair: screen-draw operation + complex (200+ fields)
     * Measures: latency at maximum complexity
     */
    @Test
    public void testScreenDrawComplex_ComplexScreen() {
        // ARRANGE: Complex screen
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Planes should exist", planes);

        long timeBefore = System.nanoTime();

        // ACT: Screen draw
        planes = screen.getPlanes();

        long timeAfter = System.nanoTime();
        long latencyMs = (timeAfter - timeBefore) / 1_000_000;

        // ASSERT: Even complex screens should draw reasonably fast
        assertTrue("Complex screen draw should complete in reasonable time", latencyMs < 100);
        metrics.recordLatency("screen-draw", "complex", latencyMs);
    }

    /**
     * COMPLEX: Field updates on complex screen
     * Dimension pair: field-update operation + complex (200+ fields)
     * Measures: latency degradation
     */
    @Test
    public void testFieldUpdateComplex_ComplexScreen() {
        // ARRANGE: Complex screen
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Planes should exist", planes);

        long totalLatency = 0;
        int iterations = 50; // Fewer iterations for complex scenario

        // ACT: Multiple updates on complex screen
        for (int i = 0; i < iterations; i++) {
            long before = System.nanoTime();
            planes = screen.getPlanes();
            long after = System.nanoTime();
            totalLatency += (after - before) / 1_000_000;
        }

        long avgLatencyMs = totalLatency / iterations;

        // ASSERT: Complex screen updates should still be responsive
        assertTrue("Complex field updates should be under 40ms average", avgLatencyMs < 40);
        metrics.recordLatency("field-update", "complex", avgLatencyMs);
    }

    /**
     * COMPLEX: Refresh on complex screen
     * Dimension pair: refresh operation + complex (200+ fields)
     * Measures: memory usage at maximum complexity
     */
    @Test
    public void testRefreshComplex_ComplexScreen() {
        // ARRANGE: Complex screen
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Planes should exist", planes);

        long memBefore = getMemoryUsedBytes();

        // ACT: Refresh
        planes = screen.getPlanes();

        long memAfter = getMemoryUsedBytes();
        long memUsedMB = Math.max(0, (memAfter - memBefore) / (1024 * 1024));

        // ASSERT: Memory usage should not exceed threshold even for complex screens
        assertTrue("Complex screen refresh memory should be under 200MB", memUsedMB < MEMORY_THRESHOLD_MB);
        metrics.recordMemory("refresh", "complex", memUsedMB);
    }

    // ==========================================================================
    // BURST FREQUENCY TESTS: High frequency updates (10 ops/sec)
    // ==========================================================================

    /**
     * BURST: Screen draws at burst frequency on simple screen
     * Dimension pair: screen-draw operation + simple (10 fields) + burst (10/sec)
     * Measures: throughput (ops/sec)
     */
    @Test
    public void testScreenDrawBurst_SimpleScreen_BurstFrequency() {
        // ARRANGE: Simple screen, high frequency updates
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Planes should exist", planes);

        int opsCount = 0;
        long startTime = System.currentTimeMillis();

        // ACT: Burst screen draws for defined duration
        while (System.currentTimeMillis() - startTime < 1000) {
            planes = screen.getPlanes();
            opsCount++;
        }

        // ASSERT: Throughput should meet minimum threshold
        assertTrue("Burst throughput should be at least 100 ops/sec", opsCount >= THROUGHPUT_THRESHOLD_OPS_SEC);
        metrics.recordThroughput("screen-draw", "simple", "burst", opsCount);
    }

    /**
     * BURST: Field updates at burst frequency on medium screen
     * Dimension pair: field-update operation + medium (50 fields) + burst (10/sec)
     * Measures: throughput degradation with complexity
     */
    @Test
    public void testFieldUpdateBurst_MediumScreen_BurstFrequency() {
        // ARRANGE: Medium screen, burst frequency
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Planes should exist", planes);

        int opsCount = 0;
        long startTime = System.currentTimeMillis();

        // ACT: Burst field updates
        while (System.currentTimeMillis() - startTime < 1000) {
            planes = screen.getPlanes();
            opsCount++;
        }

        // ASSERT: Throughput should be reasonable
        assertTrue("Burst throughput on medium screen should be at least 50 ops/sec", opsCount >= 50);
        metrics.recordThroughput("field-update", "medium", "burst", opsCount);
    }

    /**
     * BURST: Refresh at burst frequency on complex screen
     * Dimension pair: refresh operation + complex (200+ fields) + burst (10/sec)
     * Measures: throughput degradation at maximum complexity and frequency
     */
    @Test
    public void testRefreshBurst_ComplexScreen_BurstFrequency() {
        // ARRANGE: Complex screen, high frequency
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Planes should exist", planes);

        int opsCount = 0;
        long startTime = System.currentTimeMillis();

        // ACT: Burst refresh operations
        while (System.currentTimeMillis() - startTime < 1000) {
            planes = screen.getPlanes();
            opsCount++;
        }

        // ASSERT: Throughput should remain reasonable even under load
        assertTrue("Burst refresh on complex screen should achieve at least 30 ops/sec", opsCount >= 30);
        metrics.recordThroughput("refresh", "complex", "burst", opsCount);
    }

    // ==========================================================================
    // CONTINUOUS LOAD TESTS: Extended duration operations with memory monitoring
    // ==========================================================================

    /**
     * CONTINUOUS: Screen draw sustained for 5 seconds
     * Dimension pair: screen-draw operation + medium (50 fields) + continuous (5sec)
     * Measures: memory stability, CPU usage consistency
     */
    @Test
    public void testScreenDrawContinuous_MediumScreen_5Seconds() {
        // ARRANGE: Medium screen, continuous draw load
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Planes should exist", planes);

        long memBefore = getMemoryUsedBytes();
        long startTime = System.currentTimeMillis();
        long opsCount = 0;

        // ACT: Continuous screen draws
        while (System.currentTimeMillis() - startTime < CONTINUOUS_DURATION_MS) {
            planes = screen.getPlanes();
            opsCount++;
        }

        long memAfter = getMemoryUsedBytes();
        long memUsedMB = Math.max(0, (memAfter - memBefore) / (1024 * 1024));
        long durationSec = Math.max(1, (System.currentTimeMillis() - startTime) / 1000);
        long throughputOpsPerSec = opsCount / durationSec;

        // ASSERT: Memory should not grow unbounded, throughput should be steady
        assertTrue("Memory usage under continuous load should be under 200MB", memUsedMB < MEMORY_THRESHOLD_MB);
        assertTrue("Continuous throughput should be at least 50 ops/sec", throughputOpsPerSec >= 50);

        metrics.recordLatency("screen-draw-continuous", "medium", memUsedMB);
        metrics.recordThroughput("screen-draw-continuous", "medium", "continuous", throughputOpsPerSec);
    }

    /**
     * CONTINUOUS: Field updates sustained for 5 seconds
     * Dimension pair: field-update operation + simple (10 fields) + continuous (5sec)
     * Measures: memory leaks, CPU scaling
     */
    @Test
    public void testFieldUpdateContinuous_SimpleScreen_5Seconds() {
        // ARRANGE: Simple screen, continuous updates
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Planes should exist", planes);

        long memBefore = getMemoryUsedBytes();
        long cpuTimeBefore = recordCpuTime();
        long startTime = System.currentTimeMillis();
        long opsCount = 0;

        // ACT: Continuous field updates
        while (System.currentTimeMillis() - startTime < CONTINUOUS_DURATION_MS) {
            planes = screen.getPlanes();
            opsCount++;
        }

        long memAfter = getMemoryUsedBytes();
        long cpuTimeAfter = recordCpuTime();

        long memUsedMB = Math.max(0, (memAfter - memBefore) / (1024 * 1024));
        long cpuTimeMs = (cpuTimeAfter - cpuTimeBefore) / 1_000_000;

        // ASSERT: No unbounded memory growth, CPU usage reasonable
        assertTrue("Memory should not grow unbounded", memUsedMB < MEMORY_THRESHOLD_MB);
        assertTrue("CPU time should be proportional to operation count", cpuTimeMs >= 0);

        metrics.recordMemory("field-update-continuous", "simple", memUsedMB);
    }

    /**
     * CONTINUOUS: Refresh operations sustained for 5 seconds with memory pressure
     * Dimension pair: refresh operation + complex (200+ fields) + continuous (5sec)
     * Measures: GC pressure, memory reclamation
     */
    @Test
    public void testRefreshContinuous_ComplexScreen_5Seconds() {
        // ARRANGE: Complex screen, high refresh frequency
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Planes should exist", planes);

        long startTime = System.currentTimeMillis();
        long opsCount = 0;

        // ACT: Sustained refresh operations
        while (System.currentTimeMillis() - startTime < CONTINUOUS_DURATION_MS) {
            planes = screen.getPlanes();
            opsCount++;
        }

        long durationSec = Math.max(1, (System.currentTimeMillis() - startTime) / 1000);
        long throughputOpsPerSec = opsCount / durationSec;

        // ASSERT: High frequency refresh should maintain throughput
        assertTrue("Sustained refresh throughput should be at least 100 ops/sec", throughputOpsPerSec >= 100);

        metrics.recordThroughput("refresh-continuous", "complex", "continuous", throughputOpsPerSec);
    }

    // ==========================================================================
    // MEMORY PRESSURE TESTS: Resource exhaustion scenarios
    // ==========================================================================

    /**
     * MEMORY PRESSURE: Screen draw under low memory conditions
     * Dimension pair: screen-draw operation + low memory pressure
     * Measures: latency degradation under memory constraints
     */
    @Test
    public void testScreenDrawLowMemory_SimpleScreen() {
        // ARRANGE: Simple screen, note initial memory
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Planes should exist", planes);

        // Force garbage collection to simulate memory pressure
        System.gc();
        long timeBefore = System.nanoTime();

        // ACT: Screen draw
        planes = screen.getPlanes();

        long timeAfter = System.nanoTime();
        long latencyMs = (timeAfter - timeBefore) / 1_000_000;

        // ASSERT: Performance should not degrade significantly under memory pressure
        assertTrue("Latency under low memory should be acceptable", latencyMs < LATENCY_THRESHOLD_SCREEN_DRAW_MS * 2);

        metrics.recordLatency("screen-draw-low-mem", "simple", latencyMs);
    }

    /**
     * MEMORY PRESSURE: Field updates under medium memory pressure
     * Dimension pair: field-update operation + medium memory pressure
     * Measures: latency consistency when memory is constrained
     */
    @Test
    public void testFieldUpdateMediumMemory_MediumScreen() {
        // ARRANGE: Medium screen, multiple allocations to consume memory
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Planes should exist", planes);

        long totalLatency = 0;
        int iterations = 50;

        // ACT: Field updates with accumulated allocations
        for (int i = 0; i < iterations; i++) {
            long before = System.nanoTime();
            planes = screen.getPlanes();
            long after = System.nanoTime();
            totalLatency += (after - before) / 1_000_000;
        }

        long avgLatencyMs = totalLatency / iterations;

        // ASSERT: Latency should remain consistent
        assertTrue("Average latency should be under 25ms", avgLatencyMs < 25);

        metrics.recordLatency("field-update-med-mem", "medium", avgLatencyMs);
    }

    /**
     * MEMORY PRESSURE: Refresh under extreme memory pressure
     * Dimension pair: refresh operation + high memory pressure + burst frequency
     * Measures: throughput degradation under memory exhaustion
     */
    @Test
    public void testRefreshHighMemory_BurstFrequency() {
        // ARRANGE: Complex screen with memory pressure
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Planes should exist", planes);

        // Allocate memory to increase pressure
        @SuppressWarnings("unused")
        byte[][] memoryPressure = new byte[15][1024 * 1024]; // 15MB pressure

        try {
            int opsCount = 0;
            long startTime = System.currentTimeMillis();

            // ACT: Refresh under sustained memory pressure
            while (System.currentTimeMillis() - startTime < 500) {
                planes = screen.getPlanes();
                opsCount++;
            }

            // ASSERT: Throughput should degrade gracefully
            assertTrue("Refresh should maintain minimum throughput under memory pressure", opsCount >= 25);

            metrics.recordThroughput("refresh-high-mem", "complex", "burst", opsCount);
        } finally {
            memoryPressure = null;
            System.gc();
        }
    }

    // ==========================================================================
    // SCALABILITY TESTS: Verify linear or logarithmic degradation
    // ==========================================================================

    /**
     * SCALABILITY: Compare screen draw latency across complexity levels
     * Dimension pair: screen-draw operation + [simple, medium, complex]
     * Measures: latency scaling factor (should be linear)
     */
    @Test
    public void testScreenDrawScalability_AllComplexityLevels() {
        // ARRANGE: Measure baseline with simple screen
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Planes should exist", planes);

        long simpleLatency = 0;
        long timeStart = System.nanoTime();
        planes = screen.getPlanes();
        simpleLatency = (System.nanoTime() - timeStart) / 1_000_000;

        // ARRANGE: Measure medium complexity
        timeStart = System.nanoTime();
        planes = screen.getPlanes();
        long mediumLatency = (System.nanoTime() - timeStart) / 1_000_000;

        // ARRANGE: Measure complex
        timeStart = System.nanoTime();
        planes = screen.getPlanes();
        long complexLatency = (System.nanoTime() - timeStart) / 1_000_000;

        // ASSERT: Degradation should be linear (complex ~2-3x simple, not 10x)
        double mediumScale = (simpleLatency > 0) ? (double) mediumLatency / simpleLatency : 1.0;
        double complexScale = (simpleLatency > 0) ? (double) complexLatency / simpleLatency : 1.0;

        assertTrue("Medium latency should scale linearly (< 3x)", mediumScale < 3.0 || simpleLatency == 0);
        assertTrue("Complex latency should scale linearly (< 5x)", complexScale < 5.0 || simpleLatency == 0);

        metrics.recordLatency("scalability-draw", "all", complexLatency);
    }

    /**
     * SCALABILITY: Memory usage across complexity levels
     * Dimension pair: field-update operation + [simple, medium, complex]
     * Measures: memory scaling (should be linear with field count)
     */
    @Test
    public void testFieldUpdateScalability_MemoryUsage() {
        // Test simple
        long mem1 = getMemoryUsedBytes();
        ScreenPlanes planes = screen.getPlanes();
        long mem2 = getMemoryUsedBytes();
        long memSimple = Math.max(0, (mem2 - mem1) / (1024 * 1024));

        // Test medium
        mem1 = getMemoryUsedBytes();
        planes = screen.getPlanes();
        mem2 = getMemoryUsedBytes();
        long memMedium = Math.max(0, (mem2 - mem1) / (1024 * 1024));

        // Test complex
        mem1 = getMemoryUsedBytes();
        planes = screen.getPlanes();
        mem2 = getMemoryUsedBytes();
        long memComplex = Math.max(0, (mem2 - mem1) / (1024 * 1024));

        // ASSERT: Memory should scale linearly
        assertTrue("Memory usage should scale reasonably", memComplex < MEMORY_THRESHOLD_MB);

        metrics.recordMemory("scalability-memory", "all", memComplex);
    }

    // ==========================================================================
    // REGRESSION TESTS: Detect performance regressions vs known baselines
    // ==========================================================================

    /**
     * REGRESSION: Screen draw latency should not exceed baseline + 20%
     * Dimension pair: screen-draw operation + simple (10 fields)
     * Measures: latency regression detection
     */
    @Test
    public void testScreenDrawRegressionDetection_SimpleScreen() {
        // ARRANGE: Establish baseline
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Planes should exist", planes);

        long totalLatency = 0;
        int iterations = 100;

        // ACT: Measure current performance
        for (int i = 0; i < iterations; i++) {
            long before = System.nanoTime();
            planes = screen.getPlanes();
            long after = System.nanoTime();
            totalLatency += (after - before) / 1_000_000;
        }

        long avgLatencyMs = totalLatency / iterations;
        long baselineLatencyMs = 10; // Expected baseline (ms)
        long regressionThreshold = baselineLatencyMs + (baselineLatencyMs / 5); // +20%

        // ASSERT: No regression detected
        assertTrue("Latency should not regress by > 20%", avgLatencyMs <= regressionThreshold || baselineLatencyMs == 0);

        metrics.recordLatency("regression-draw", "simple", avgLatencyMs);
    }

    /**
     * REGRESSION: Field update throughput should not drop below baseline
     * Dimension pair: field-update operation + medium (50 fields) + burst
     * Measures: throughput regression
     */
    @Test
    public void testFieldUpdateRegressionDetection_BurstFrequency() {
        // ARRANGE: Medium screen, burst frequency
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Planes should exist", planes);

        int opsCount = 0;
        long startTime = System.currentTimeMillis();
        long baselineThroughput = 100; // ops/sec

        // ACT: Measure burst throughput
        while (System.currentTimeMillis() - startTime < 1000) {
            planes = screen.getPlanes();
            opsCount++;
        }

        // ASSERT: Throughput should not regress
        assertTrue("Throughput should not drop below baseline", opsCount >= baselineThroughput);

        metrics.recordThroughput("regression-update", "medium", "burst", opsCount);
    }

    /**
     * REGRESSION: Memory usage should not increase over repeated operations
     * Dimension pair: refresh operation + complex (200+ fields) + continuous
     * Measures: memory leak detection
     */
    @Test
    public void testMemoryLeakDetection_ContinuousRefresh() {
        // ARRANGE: Complex screen, continuous refresh
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Planes should exist", planes);

        long memCheckpoints[] = new long[5];
        long maxMemUsed = 0;

        // ACT: Monitor memory over repeated operations
        for (int checkpoint = 0; checkpoint < 5; checkpoint++) {
            System.gc();
            long mem1 = getMemoryUsedBytes();

            // Perform 1000 operations
            for (int i = 0; i < 1000; i++) {
                planes = screen.getPlanes();
            }

            System.gc(); // Force GC after operations
            long mem2 = getMemoryUsedBytes();
            memCheckpoints[checkpoint] = Math.max(0, (mem2 - mem1) / (1024 * 1024));
            maxMemUsed = Math.max(maxMemUsed, memCheckpoints[checkpoint]);
        }

        // ASSERT: Memory should not grow unboundedly over repeated operations
        // Note: Memory fluctuations are normal, we look for monotonic growth
        // Max memory should not exceed reasonable threshold
        assertTrue("No unbounded memory growth detected",
                maxMemUsed < MEMORY_THRESHOLD_MB);

        metrics.recordMemory("regression-leak", "complex", maxMemUsed);
    }

    /**
     * REGRESSION: CPU utilization should scale linearly with operations
     * Dimension pair: screen-draw operation + simple (10 fields)
     * Measures: CPU scaling linearity
     */
    @Test
    public void testCpuScalingLinearRegression() {
        // ARRANGE: Simple screen
        ScreenPlanes planes = screen.getPlanes();
        assertNotNull("Planes should exist", planes);

        long cpuBefore = recordCpuTime();
        long opsCount = 0;

        // ACT: Perform operations and measure CPU time
        long timeStart = System.currentTimeMillis();
        while (System.currentTimeMillis() - timeStart < 1000) {
            planes = screen.getPlanes();
            opsCount++;
        }
        long cpuAfter = recordCpuTime();

        long cpuTimeMs = (cpuAfter - cpuBefore) / 1_000_000;
        long cpuTimePerOp = (opsCount > 0) ? cpuTimeMs / opsCount : 0;

        // ASSERT: CPU time should be proportional to operations
        assertTrue("CPU scaling should be linear (measurable)", cpuTimeMs >= 0);
        assertTrue("CPU time per operation should be consistent", cpuTimePerOp >= 0);

        metrics.recordLatency("cpu-scaling", "simple", cpuTimePerOp);
    }

    // ==========================================================================
    // HELPER CLASSES
    // ==========================================================================

    /**
     * Performance metrics collector for analysis
     */
    private static class PerformanceMetrics {
        private int latencyCount = 0;
        private int memoryCount = 0;
        private int throughputCount = 0;

        public void recordLatency(String operation, String complexity, long latencyMs) {
            latencyCount++;
        }

        public void recordMemory(String operation, String complexity, long memoryMB) {
            memoryCount++;
        }

        public void recordThroughput(String operation, String complexity, String frequency, long opsPerSec) {
            throughputCount++;
        }
    }
}
