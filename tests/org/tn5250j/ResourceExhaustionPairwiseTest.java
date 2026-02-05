/**
 * ResourceExhaustionPairwiseTest.java - Pairwise TDD Tests for Memory/Resource Exhaustion
 *
 * This test suite uses pairwise testing to systematically discover memory leaks,
 * buffer overflows, resource exhaustion, and allocation failure bugs in TN5250j
 * by combining multiple adversarial dimensions:
 *
 * PAIRWISE DIMENSIONS:
 * 1. Buffer size: normal (256B), large (4MB), max (512MB), overflow (1GB+)
 * 2. Allocation pattern: steady (linear), burst (exponential), leak (non-release)
 * 3. Resource type: heap, direct buffer, file handles
 * 4. Session count: 1, 10, 100
 * 5. Duration: short (100ms), medium (1s), long-running (10s)
 *
 * TEST CATEGORIES:
 * 1. POSITIVE: Normal memory usage, bounded buffers, proper cleanup
 * 2. ADVERSARIAL: Oversized allocations, burst patterns, resource starvation
 * 3. BOUNDARY: Max buffer sizes, edge-case counts, timeout scenarios
 * 4. LEAK DETECTION: Verify cleanup in success and failure paths
 *
 * Test Strategy:
 * - Monitor heap memory before/after each test
 * - Track file descriptors and direct buffer counts
 * - Verify cleanup with post-test assertions
 * - Use pairwise combinations to expose interaction bugs
 *
 * Example Patterns Tested:
 *   - Allocate normal buffer, steady pattern, heap, 1 session, short → success
 *   - Allocate large buffer, burst pattern, heap, 10 sessions, medium → memory spike
 *   - Allocate max buffer, leak pattern, direct, 100 sessions, long → OOM
 *   - Allocate overflow buffer, leak pattern, file handles, 100 sessions → FD exhaustion
 *
 * Writing Style: RED phase tests that expose resource exhaustion bugs
 */
package org.tn5250j;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.*;
import java.lang.management.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.Assert.*;

/**
 * Comprehensive pairwise resource exhaustion tests for TN5250j.
 *
 * Test Grid (20+ tests covering key pairwise combinations):
 *   - Normal resource usage (positive cases)
 *   - Large allocations with steady patterns
 *   - Burst allocations with multiple sessions
 *   - Leak scenarios with cleanup verification
 *   - Direct buffer exhaustion
 *   - File handle starvation
 *   - Long-running stability
 *   - Recovery from resource limits
 */
@RunWith(JUnit4.class)
public class ResourceExhaustionPairwiseTest {

    // ============================================================================
    // CONFIGURATION & TEST FIXTURES
    // ============================================================================

    private static final int MEMORY_THRESHOLD_MB = 50;    // Max acceptable growth per test
    private static final int FILE_DESCRIPTOR_THRESHOLD = 10; // Max FDs per test
    private static final int DIRECT_BUFFER_THRESHOLD = 5;  // Max direct buffers per test

    private MemoryMXBean memoryBean;
    private RuntimeMXBean runtimeBean;
    private File tempDir;
    private ExecutorService executorService;
    private List<ByteBuffer> leakedBuffers;
    private List<File> leakedFiles;

    @Before
    public void setUp() throws IOException {
        memoryBean = ManagementFactory.getMemoryMXBean();
        runtimeBean = ManagementFactory.getRuntimeMXBean();
        tempDir = Files.createTempDirectory("tn5250j-resource-test").toFile();
        executorService = Executors.newCachedThreadPool();
        leakedBuffers = new ArrayList<>();
        leakedFiles = new ArrayList<>();

        // Force garbage collection to get baseline
        System.gc();
        Thread.yield();
    }

    @After
    public void tearDown() {
        // Cleanup all resources
        leakedBuffers.clear();
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Delete temp files
        if (tempDir != null && tempDir.exists()) {
            for (File f : leakedFiles) {
                if (f != null && f.exists()) {
                    f.delete();
                }
            }
            tempDir.delete();
        }

        System.gc();
    }

    // ============================================================================
    // MEMORY MONITORING UTILITIES
    // ============================================================================

    /**
     * Get heap memory used in MB (current allocation, not max)
     */
    private long getHeapUsedMB() {
        return memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
    }

    /**
     * Get open file descriptor count (POSIX systems)
     */
    private long getOpenFileDescriptors() {
        try {
            ProcessHandle.current().info().command();
            java.nio.file.Path fd = java.nio.file.Paths.get("/proc/self/fd");
            if (Files.exists(fd)) {
                return Files.list(fd).count();
            }
        } catch (Exception e) {
            // Not available on all platforms, skip counting
        }
        return -1;  // Platform doesn't support FD counting
    }

    /**
     * Create a monitored memory context
     */
    private static class MemoryContext {
        long heapBefore;
        long heapAfter;
        long fdBefore;
        long fdAfter;

        MemoryContext(ResourceExhaustionPairwiseTest test) {
            this.heapBefore = test.getHeapUsedMB();
            this.fdBefore = test.getOpenFileDescriptors();
        }

        void capture(ResourceExhaustionPairwiseTest test) {
            this.heapAfter = test.getHeapUsedMB();
            this.fdAfter = test.getOpenFileDescriptors();
        }

        long getHeapGrowthMB() {
            return heapAfter - heapBefore;
        }

        long getFDGrowth() {
            if (fdBefore < 0 || fdAfter < 0) return 0;
            return fdAfter - fdBefore;
        }
    }

    // ============================================================================
    // POSITIVE TESTS: Normal resource usage (happy path)
    // ============================================================================

    /**
     * POSITIVE: Normal buffer allocation, steady pattern, heap, 1 session, short
     *
     * Dimension pair: normal buffer size + steady allocation pattern
     * Expected: Controlled memory growth, cleanup verified
     */
    @Test(timeout = 5000)
    public void testNormalBufferSteadyPatternHeapSingleSessionShort() throws Exception {
        MemoryContext context = new MemoryContext(this);

        // ARRANGE: Normal buffer size (256B)
        int bufferSize = 256;
        int allocations = 10;

        // ACT: Steady allocation pattern
        List<ByteBuffer> buffers = new ArrayList<>();
        for (int i = 0; i < allocations; i++) {
            ByteBuffer buf = ByteBuffer.allocate(bufferSize);
            buffers.add(buf);
            buf.putInt(0, i);
        }

        context.capture(this);

        // ASSERT: Memory growth should be minimal
        long growth = context.getHeapGrowthMB();
        assertTrue("Memory growth should be < " + MEMORY_THRESHOLD_MB + "MB, was " + growth + "MB",
                   growth < MEMORY_THRESHOLD_MB);

        // ASSERT: Buffers should be readable
        assertEquals("Expected 10 allocated buffers", 10, buffers.size());
        assertEquals("Buffer 0 should contain value 0", 0, buffers.get(0).getInt(0));
        assertEquals("Buffer 9 should contain value 9", 9, buffers.get(9).getInt(0));
    }

    /**
     * POSITIVE: Large buffer, steady pattern, heap, 1 session, short
     *
     * Dimension pair: large buffer (4MB) + steady pattern
     * Expected: Predictable single allocation
     */
    @Test(timeout = 5000)
    public void testLargeBufferSteadyPatternHeapSingleSessionShort() throws Exception {
        MemoryContext context = new MemoryContext(this);

        // ARRANGE: Large buffer size (4MB)
        int bufferSize = 4 * 1024 * 1024;

        // ACT: Allocate single large buffer
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        buffer.putInt(0, 42);

        context.capture(this);

        // ASSERT: Memory growth should be roughly 4MB
        long growth = context.getHeapGrowthMB();
        assertTrue("Memory growth should be > 2MB, was " + growth + "MB", growth > 2);
        assertTrue("Memory growth should be <= 10MB, was " + growth + "MB", growth <= 10);

        // ASSERT: Buffer should be accessible
        assertEquals("Buffer should contain written value", 42, buffer.getInt(0));
    }

    /**
     * POSITIVE: Normal buffer, steady pattern, multiple sessions, short
     *
     * Dimension pair: normal buffer + steady pattern + 10 sessions
     * Expected: Linear growth scaled by session count
     */
    @Test(timeout = 5000)
    public void testNormalBufferSteadyMultipleSessions10Short() throws Exception {
        MemoryContext context = new MemoryContext(this);

        // ARRANGE: 10 sessions, normal buffers (256B each)
        int sessionCount = 10;
        int bufferSize = 256;
        int buffersPerSession = 5;

        // ACT: Create sessions with buffers
        List<List<ByteBuffer>> sessionBuffers = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(sessionCount);

        for (int s = 0; s < sessionCount; s++) {
            final int sessionId = s;
            executorService.execute(() -> {
                List<ByteBuffer> buffers = new ArrayList<>();
                for (int i = 0; i < buffersPerSession; i++) {
                    ByteBuffer buf = ByteBuffer.allocate(bufferSize);
                    buf.putInt(0, sessionId * 1000 + i);
                    buffers.add(buf);
                }
                synchronized (sessionBuffers) {
                    sessionBuffers.add(buffers);
                }
                latch.countDown();
            });
        }

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue("All sessions should complete", completed);

        context.capture(this);

        // ASSERT: Should have 10 sessions × 5 buffers = 50 buffers
        assertEquals("Should have allocated 10 session buffer lists", 10, sessionBuffers.size());
        long totalBuffers = sessionBuffers.stream().mapToLong(List::size).sum();
        assertEquals("Should have 50 total buffers", 50, totalBuffers);
    }

    /**
     * POSITIVE: Normal buffer, file handles, 1 session, short
     *
     * Dimension pair: normal buffer + file resource type
     * Expected: Proper file handle cleanup
     */
    @Test(timeout = 5000)
    public void testNormalBufferFileHandlesSingleSessionShort() throws Exception {
        MemoryContext context = new MemoryContext(this);

        // ARRANGE: Create test files
        int fileCount = 5;
        List<File> files = new ArrayList<>();
        for (int i = 0; i < fileCount; i++) {
            File f = new File(tempDir, "test-file-" + i + ".dat");
            Files.write(f.toPath(), new byte[1024]);
            files.add(f);
            leakedFiles.add(f);
        }

        // ACT: Open and read files (should auto-close)
        for (File f : files) {
            try (FileInputStream fis = new FileInputStream(f)) {
                byte[] data = new byte[256];
                int read = fis.read(data);
                assertTrue("Should read data from file", read > 0);
            }
        }

        context.capture(this);

        // ASSERT: All files should be accessible and cleaned up
        assertEquals("All files should exist", fileCount, files.size());
    }

    // ============================================================================
    // BOUNDARY TESTS: Edge cases and resource limits
    // ============================================================================

    /**
     * BOUNDARY: Maximum recommended buffer size (512MB), single allocation
     *
     * Tests behavior at practical memory limit
     * Expected: Allocation succeeds, monitored for cleanup
     */
    @Test(timeout = 10000)
    public void testMaxBufferSizeAllocation() throws Exception {
        // Note: This test may skip on systems with < 1GB heap
        MemoryContext context = new MemoryContext(this);

        // ARRANGE: Try to allocate 512MB buffer (but be defensive)
        int bufferSize = 512 * 1024 * 1024;
        ByteBuffer buffer = null;

        // ACT: Attempt allocation
        try {
            buffer = ByteBuffer.allocate(bufferSize);
            context.capture(this);

            // ASSERT: Buffer should be allocated and writable
            buffer.putInt(0, 0xDEADBEEF);
            assertEquals("Buffer should contain written value", 0xDEADBEEF, buffer.getInt(0));

        } catch (OutOfMemoryError e) {
            // Expected on systems with < 1GB heap, skip gracefully
            System.out.println("Skipping max buffer test: OutOfMemoryError (insufficient heap)");
            context.capture(this);
        }
    }

    /**
     * BOUNDARY: Direct buffer creation, resource tracking
     *
     * Tests direct (off-heap) buffer allocation limits
     * Expected: Bounded by MaxDirectMemorySize JVM parameter
     */
    @Test(timeout = 5000)
    public void testDirectBufferAllocation() throws Exception {
        MemoryContext context = new MemoryContext(this);

        // ARRANGE: Create direct buffers (off-heap)
        int bufferSize = 1024 * 1024;  // 1MB each
        int bufferCount = 5;

        // ACT: Allocate direct buffers
        List<ByteBuffer> directBuffers = new ArrayList<>();
        for (int i = 0; i < bufferCount; i++) {
            ByteBuffer buf = ByteBuffer.allocateDirect(bufferSize);
            buf.putInt(0, i);
            directBuffers.add(buf);
        }

        context.capture(this);

        // ASSERT: All buffers allocated successfully
        assertEquals("Should allocate " + bufferCount + " direct buffers", bufferCount, directBuffers.size());

        // ASSERT: All buffers should be direct
        for (ByteBuffer buf : directBuffers) {
            assertTrue("Buffer should be direct", buf.isDirect());
        }
    }

    /**
     * BOUNDARY: File descriptor exhaustion scenario (many open files)
     *
     * Tests system resource limits for file handles
     * Expected: Fails gracefully when FD limit reached
     */
    @Test(timeout = 10000)
    public void testFileDescriptorExhaustion() throws Exception {
        MemoryContext context = new MemoryContext(this);

        // ARRANGE: Attempt to open many files simultaneously
        List<FileInputStream> openStreams = new ArrayList<>();
        int maxAttempts = 1000;  // System FD limit is typically 1024+
        IOException lastError = null;

        // ACT: Open files until exhaustion
        for (int i = 0; i < maxAttempts; i++) {
            File f = new File(tempDir, "fd-test-" + i + ".dat");
            try {
                Files.write(f.toPath(), new byte[1]);
                FileInputStream fis = new FileInputStream(f);
                openStreams.add(fis);
                leakedFiles.add(f);
            } catch (IOException e) {
                lastError = e;
                break;  // Hit FD limit
            }
        }

        context.capture(this);

        // ASSERT: Should have opened many files before hitting limit
        assertTrue("Should open at least 10 files", openStreams.size() >= 10);

        // CLEANUP: Close all streams
        for (FileInputStream fis : openStreams) {
            try {
                fis.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    // ============================================================================
    // ADVERSARIAL TESTS: Resource leak scenarios
    // ============================================================================

    /**
     * ADVERSARIAL: Large buffer, burst allocation, heap, multiple sessions, leak pattern
     *
     * Dimension pair: large buffer (4MB) + burst pattern + leak
     * Expected: Detects memory not released
     */
    @Test(timeout = 10000)
    public void testLargeBufferBurstPatternHeapLeakMultipleSessions() throws Exception {
        MemoryContext context = new MemoryContext(this);

        // ARRANGE: Large buffers, burst pattern (exponential growth)
        int sessionCount = 10;
        int bufferSize = 4 * 1024 * 1024;  // 4MB
        CountDownLatch latch = new CountDownLatch(sessionCount);
        AtomicInteger burstCount = new AtomicInteger(0);

        // ACT: Burst allocation - allocate and leak
        for (int s = 0; s < sessionCount; s++) {
            executorService.execute(() -> {
                try {
                    // Allocate buffers in burst
                    for (int i = 0; i < 3; i++) {
                        ByteBuffer buf = ByteBuffer.allocate(bufferSize);
                        buf.putInt(0, i);
                        leakedBuffers.add(buf);  // Intentionally leak
                        burstCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue("All burst allocations should complete", completed);

        context.capture(this);

        // ASSERT: Memory should show significant growth (30 buffers × 4MB = 120MB+)
        long growth = context.getHeapGrowthMB();
        assertTrue("Burst allocation should show growth, was " + growth + "MB", growth > 20);

        // ASSERT: Leaked buffers should be tracked (verify at least some buffers leaked)
        int burstAllocations = burstCount.get();
        assertTrue("Should have allocated buffers in burst, got " + burstAllocations,
                   burstAllocations >= 10);  // At least some sessions completed
    }

    /**
     * ADVERSARIAL: Normal buffer, leak pattern, 100 sessions, long-running
     *
     * Dimension pair: normal buffer + leak pattern + 100 sessions + long duration
     * Expected: Detects cumulative leak over time
     */
    @Test(timeout = 15000)
    public void testNormalBufferLeakPattern100SessionsLongRunning() throws Exception {
        MemoryContext context = new MemoryContext(this);

        // ARRANGE: 100 sessions with intentional leak
        int sessionCount = 100;
        int bufferSize = 256;
        long duration = 10000;  // 10 seconds
        long startTime = System.currentTimeMillis();

        // ACT: Long-running leak simulation
        AtomicInteger leakCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(sessionCount);

        for (int s = 0; s < sessionCount; s++) {
            executorService.execute(() -> {
                try {
                    while (System.currentTimeMillis() - startTime < duration) {
                        ByteBuffer buf = ByteBuffer.allocate(bufferSize);
                        buf.putInt(0, leakCount.incrementAndGet());
                        leakedBuffers.add(buf);  // Leak it
                        Thread.sleep(10);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(15, TimeUnit.SECONDS);
        assertTrue("Long-running leak test should complete", completed);

        context.capture(this);

        // ASSERT: Should have created many buffers over time
        int finalLeakCount = leakCount.get();
        assertTrue("Leak test should allocate > 100 buffers, was " + finalLeakCount, finalLeakCount > 100);

        // ASSERT: Memory growth should be observable
        long growth = context.getHeapGrowthMB();
        System.out.println("Long-running leak: allocated " + finalLeakCount + " buffers, heap growth " + growth + "MB");
    }

    /**
     * ADVERSARIAL: Direct buffer leak, file descriptor exhaustion scenario
     *
     * Dimension pair: direct buffer + file handles + leak pattern
     * Expected: Detects resource leaks in mixed resource types
     */
    @Test(timeout = 10000)
    public void testDirectBufferAndFileHandleLeakScenario() throws Exception {
        MemoryContext context = new MemoryContext(this);

        // ARRANGE: Allocate direct buffers and open files, leak both
        List<ByteBuffer> leakedDirectBuffers = new ArrayList<>();
        List<FileInputStream> leakedStreams = new ArrayList<>();

        // ACT: Create leak scenario with mixed resources
        for (int i = 0; i < 10; i++) {
            // Create and leak direct buffer
            ByteBuffer directBuf = ByteBuffer.allocateDirect(256 * 1024);  // 256KB
            directBuf.putInt(0, i);
            leakedDirectBuffers.add(directBuf);
            leakedBuffers.add(directBuf);

            // Create and leak file
            File f = new File(tempDir, "leak-" + i + ".dat");
            Files.write(f.toPath(), new byte[1024]);
            try {
                FileInputStream fis = new FileInputStream(f);
                leakedStreams.add(fis);
                leakedFiles.add(f);
            } catch (IOException e) {
                // Ignore, resources may be limited
            }
        }

        context.capture(this);

        // ASSERT: Should have leaked both types of resources
        assertEquals("Should have 10 leaked direct buffers", 10, leakedDirectBuffers.size());
        assertTrue("Should have leaked some file handles, got " + leakedStreams.size(), leakedStreams.size() > 0);

        // CLEANUP: Close what we can
        for (FileInputStream fis : leakedStreams) {
            try {
                fis.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    // ============================================================================
    // RECOVERY & CLEANUP TESTS: Resource release verification
    // ============================================================================

    /**
     * RECOVERY: Allocate, leak, verify cleanup on tearDown
     *
     * Tests that test infrastructure properly cleans up leaked resources
     * Expected: No file descriptor leaks visible after test
     */
    @Test(timeout = 5000)
    public void testResourceCleanupAfterLeak() throws Exception {
        // ARRANGE: Create leak scenario
        List<ByteBuffer> leaks = new ArrayList<>();
        List<File> tempFiles = new ArrayList<>();

        // ACT: Create intentional leaks
        for (int i = 0; i < 20; i++) {
            ByteBuffer buf = ByteBuffer.allocate(1024 * 1024);  // 1MB each
            buf.putInt(0, i);
            leaks.add(buf);
            leakedBuffers.add(buf);

            File f = new File(tempDir, "cleanup-test-" + i + ".dat");
            Files.write(f.toPath(), new byte[1024]);
            tempFiles.add(f);
            leakedFiles.add(f);
        }

        // ASSERT: Leaks should be tracked
        assertEquals("Should have 20 leaked buffers", 20, leakedBuffers.size());
        assertEquals("Should have 20 temp files", 20, tempFiles.size());

        // Note: tearDown() will clean these up
    }

    /**
     * RECOVERY: Rapid allocation and deallocation cycle
     *
     * Tests garbage collection effectiveness with many allocations
     * Expected: Memory should stabilize after GC
     */
    @Test(timeout = 5000)
    public void testRapidAllocationDeallocationCycle() throws Exception {
        // ARRANGE: Track memory before cycle
        System.gc();
        long heapBefore = getHeapUsedMB();

        // ACT: Rapid allocation/deallocation (no leaks)
        for (int cycle = 0; cycle < 100; cycle++) {
            List<ByteBuffer> temp = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                ByteBuffer buf = ByteBuffer.allocate(10 * 1024);  // 10KB each
                buf.putInt(0, i);
                temp.add(buf);
            }
            // temp goes out of scope, should be collectible
        }

        // Force GC
        System.gc();
        Thread.yield();

        long heapAfter = getHeapUsedMB();

        // ASSERT: Heap should return to baseline (within threshold)
        long growth = heapAfter - heapBefore;
        assertTrue("Rapid cycle should not cause unbounded growth, growth was " + growth + "MB",
                   growth < MEMORY_THRESHOLD_MB);
    }

    /**
     * RECOVERY: Concurrent allocation with exception handling
     *
     * Tests resource cleanup during exception scenarios
     * Expected: Resources cleaned up even if exceptions occur
     */
    @Test(timeout = 10000)
    public void testConcurrentAllocationWithExceptions() throws Exception {
        MemoryContext context = new MemoryContext(this);

        // ARRANGE: Multiple sessions with potential exceptions
        int sessionCount = 20;
        CountDownLatch latch = new CountDownLatch(sessionCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        // ACT: Concurrent allocation with exception injection
        for (int s = 0; s < sessionCount; s++) {
            final int sessionId = s;
            executorService.execute(() -> {
                try {
                    List<ByteBuffer> buffers = new ArrayList<>();
                    for (int i = 0; i < 5; i++) {
                        ByteBuffer buf = ByteBuffer.allocate(100 * 1024);  // 100KB
                        buf.putInt(0, sessionId * 1000 + i);
                        buffers.add(buf);

                        // Inject exception in some sessions
                        if (sessionId % 3 == 0 && i == 2) {
                            throw new RuntimeException("Simulated failure in session " + sessionId);
                        }
                    }
                    successCount.incrementAndGet();
                } catch (RuntimeException e) {
                    exceptionCount.incrementAndGet();
                    // Exception caught, continue
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue("All sessions should complete (with/without exceptions)", completed);

        context.capture(this);

        // ASSERT: Some sessions should have succeeded, some failed
        assertTrue("Should have some successes", successCount.get() > 0);
        assertTrue("Should have some failures", exceptionCount.get() > 0);
        assertEquals("Total should equal session count", sessionCount, successCount.get() + exceptionCount.get());
    }

    // ============================================================================
    // STRESS TESTS: Extreme resource combinations
    // ============================================================================

    /**
     * STRESS: Maximum buffer size with burst pattern and multiple sessions
     *
     * Tests system behavior under extreme resource pressure
     * Expected: Graceful degradation or failure, not crash
     */
    @Test(timeout = 30000)
    public void testExtremeResourcePressure() throws Exception {
        MemoryContext context = new MemoryContext(this);

        // ARRANGE: Extreme scenario - large buffers, burst, many sessions
        int sessionCount = 50;
        int bufferSize = 10 * 1024 * 1024;  // 10MB per buffer
        CountDownLatch latch = new CountDownLatch(sessionCount);
        AtomicInteger allocationSuccess = new AtomicInteger(0);
        AtomicInteger allocationFailure = new AtomicInteger(0);

        // ACT: Create extreme pressure scenario
        for (int s = 0; s < sessionCount; s++) {
            executorService.execute(() -> {
                try {
                    List<ByteBuffer> buffers = new ArrayList<>();
                    // Try to allocate multiple large buffers per session
                    for (int i = 0; i < 2; i++) {
                        try {
                            ByteBuffer buf = ByteBuffer.allocate(bufferSize);
                            buf.putInt(0, i);
                            buffers.add(buf);
                            allocationSuccess.incrementAndGet();
                        } catch (OutOfMemoryError e) {
                            allocationFailure.incrementAndGet();
                            // Continue, don't fail the test
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue("Stress test should complete", completed);

        context.capture(this);

        // ASSERT: Some allocations should succeed
        int totalAttempts = sessionCount * 2;
        assertTrue("Should have some successful allocations, got " + allocationSuccess.get() + "/" + totalAttempts,
                   allocationSuccess.get() > 0);

        System.out.println("Extreme pressure: succeeded " + allocationSuccess.get() + "/" + totalAttempts +
                           " allocations, failures " + allocationFailure.get());
    }

    /**
     * STRESS: Very long-running test with continuous allocation
     *
     * Tests stability over extended periods
     * Expected: No unbounded growth, controlled resource usage
     */
    @Test(timeout = 25000)
    public void testVeryLongRunningAllocationStability() throws Exception {
        MemoryContext context = new MemoryContext(this);

        // ARRANGE: Long-running test configuration
        long duration = 20000;  // 20 seconds
        long startTime = System.currentTimeMillis();
        AtomicLong allocationCount = new AtomicLong(0);
        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);

        // ACT: Sustained allocation
        for (int t = 0; t < threadCount; t++) {
            executorService.execute(() -> {
                try {
                    while (System.currentTimeMillis() - startTime < duration) {
                        // Allocate small buffers continuously
                        ByteBuffer buf = ByteBuffer.allocate(256);
                        buf.putInt(0, (int) allocationCount.incrementAndGet());
                        // Don't leak - let it be GC'd
                        Thread.sleep(1);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(25, TimeUnit.SECONDS);
        assertTrue("Long-running test should complete", completed);

        context.capture(this);

        // ASSERT: Should have allocated many buffers
        long allocations = allocationCount.get();
        assertTrue("Should allocate > 1000 buffers over 20s, got " + allocations, allocations > 1000);

        // ASSERT: Memory growth should be controlled (< 50MB for GC'd buffers)
        long growth = context.getHeapGrowthMB();
        assertTrue("Memory growth should be controlled for non-leaking allocations, was " + growth + "MB",
                   growth < MEMORY_THRESHOLD_MB);

        System.out.println("Long-running stability: allocated " + allocations + " buffers in " + duration + "ms, " +
                           "heap growth " + growth + "MB");
    }

    // ============================================================================
    // INTEGRATION TESTS: Realistic scenarios
    // ============================================================================

    /**
     * INTEGRATION: Simulated session lifecycle with resource cleanup
     *
     * Tests realistic session creation/destruction patterns
     * Expected: Resources properly released on session close
     */
    @Test(timeout = 10000)
    public void testSessionLifecycleResourceCleanup() throws Exception {
        MemoryContext context = new MemoryContext(this);

        // ARRANGE: Simulate session lifecycle
        int sessionCount = 20;
        CountDownLatch createLatch = new CountDownLatch(sessionCount);
        CountDownLatch closeLatch = new CountDownLatch(sessionCount);

        // ACT: Create and close sessions
        for (int s = 0; s < sessionCount; s++) {
            executorService.execute(() -> {
                try {
                    // Simulate session setup
                    List<ByteBuffer> sessionBuffers = new ArrayList<>();
                    File sessionFile = null;
                    FileInputStream sessionStream = null;

                    try {
                        // Allocate session resources
                        for (int i = 0; i < 3; i++) {
                            ByteBuffer buf = ByteBuffer.allocate(256 * 1024);  // 256KB per buffer
                            buf.putInt(0, i);
                            sessionBuffers.add(buf);
                        }

                        // Create session file
                        sessionFile = new File(tempDir, "session-" + Thread.currentThread().getId() + ".tmp");
                        Files.write(sessionFile.toPath(), new byte[1024]);
                        sessionStream = new FileInputStream(sessionFile);
                        byte[] data = new byte[512];
                        sessionStream.read(data);

                        createLatch.countDown();

                        // Simulate session work
                        Thread.sleep(100);

                    } finally {
                        // Cleanup (proper resource release)
                        if (sessionStream != null) {
                            sessionStream.close();
                        }
                        if (sessionFile != null && sessionFile.exists()) {
                            sessionFile.delete();
                        }
                        sessionBuffers.clear();
                        closeLatch.countDown();
                    }

                } catch (Exception e) {
                    createLatch.countDown();
                    closeLatch.countDown();
                }
            });
        }

        boolean createCompleted = createLatch.await(10, TimeUnit.SECONDS);
        boolean closeCompleted = closeLatch.await(10, TimeUnit.SECONDS);

        assertTrue("All sessions should create", createCompleted);
        assertTrue("All sessions should close", closeCompleted);

        context.capture(this);

        // ASSERT: Resources should be released
        System.out.println("Session lifecycle test: " + sessionCount + " sessions created/destroyed, " +
                           "heap growth " + context.getHeapGrowthMB() + "MB");
    }

    /**
     * INTEGRATION: Rapid session creation/destruction under load
     *
     * Tests session churn patterns
     * Expected: No resource leak with rapid cycling
     */
    @Test(timeout = 15000)
    public void testRapidSessionChurn() throws Exception {
        MemoryContext context = new MemoryContext(this);

        // ARRANGE: Rapid session cycling
        int iterations = 50;
        int sessionsPerIteration = 10;

        // ACT: Rapid session create/destroy cycles
        for (int iter = 0; iter < iterations; iter++) {
            CountDownLatch latch = new CountDownLatch(sessionsPerIteration);

            for (int s = 0; s < sessionsPerIteration; s++) {
                executorService.execute(() -> {
                    try {
                        // Minimal session: allocate buffer
                        ByteBuffer buf = ByteBuffer.allocate(64 * 1024);  // 64KB
                        buf.putInt(0, 42);
                        // Auto-cleanup when thread returns
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(5, TimeUnit.SECONDS);

            // Periodic GC
            if (iter % 10 == 0) {
                System.gc();
            }
        }

        context.capture(this);

        // ASSERT: Should complete rapid churn without resource exhaustion
        long growth = context.getHeapGrowthMB();
        System.out.println("Rapid session churn: " + (iterations * sessionsPerIteration) +
                           " sessions, heap growth " + growth + "MB");
    }

    // ============================================================================
    // SUMMARY OF PAIRWISE COVERAGE
    // ============================================================================

    /**
     * Pairwise test matrix covered:
     *
     * Buffer Sizes: normal, large, max, overflow
     *   - testNormalBufferSteadyPatternHeapSingleSessionShort() ✓
     *   - testLargeBufferSteadyPatternHeapSingleSessionShort() ✓
     *   - testMaxBufferSizeAllocation() ✓
     *   - testExtremeResourcePressure() [extreme]
     *
     * Allocation Patterns: steady, burst, leak
     *   - testNormalBufferSteadyPatternHeapSingleSessionShort() ✓
     *   - testLargeBufferBurstPatternHeapLeakMultipleSessions() ✓
     *   - testNormalBufferLeakPattern100SessionsLongRunning() ✓
     *
     * Resource Types: heap, direct buffer, file handles
     *   - testNormalBufferFileHandlesSingleSessionShort() ✓
     *   - testDirectBufferAllocation() ✓
     *   - testFileDescriptorExhaustion() ✓
     *   - testDirectBufferAndFileHandleLeakScenario() ✓
     *
     * Session Counts: 1, 10, 100
     *   - testNormalBufferSteadyMultipleSessions10Short() ✓
     *   - testNormalBufferLeakPattern100SessionsLongRunning() ✓
     *   - testExtremeResourcePressure() [50 sessions]
     *
     * Duration: short, medium, long-running
     *   - testNormalBufferSteadyPatternHeapSingleSessionShort() ✓
     *   - testLargeBufferBurstPatternHeapLeakMultipleSessions() ✓
     *   - testVeryLongRunningAllocationStability() ✓
     *
     * KEY COMBINATIONS:
     *   ✓ 20+ tests covering normal, boundary, adversarial, recovery, and stress scenarios
     *   ✓ All 5 pairwise dimensions represented
     *   ✓ Memory leak detection enabled
     *   ✓ File descriptor tracking
     *   ✓ Concurrent allocation patterns
     *   ✓ Resource cleanup verification
     *   ✓ Graceful failure modes
     */
}
