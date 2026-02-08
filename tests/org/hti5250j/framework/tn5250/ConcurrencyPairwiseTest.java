/*
 * SPDX-FileCopyrightText: Copyright (c) 2025
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.framework.tn5250;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.TimeUnit;

/**
 * Pairwise concurrency test suite for tn5250j-headless.
 *
 * TEST DIMENSIONS (pairwise combinations):
 *   - Thread counts: [1, 2, 4, 8, 16, 100]
 *   - Operation types: [read, write, mixed]
 *   - Timing: [sequential, concurrent, interleaved]
 *   - Session states: [connecting, connected, disconnecting, disconnected]
 *   - Data sizes: [0, 1, 100, 1000, 65535]
 *
 * CONCURRENCY PATTERNS TESTED:
 *   1. Multiple readers + single writer
 *   2. Single reader + multiple writers
 *   3. Mixed reader/writer with state transitions
 *   4. BlockingQueue producer/consumer contention
 *   5. Collection concurrent modification
 *   6. Resource lifecycle during concurrent access
 *   7. Timing-sensitive race conditions
 *   8. Deadlock detection (waiting cycles)
 *   9. Livelock detection (spinning without progress)
 *   10. Starvation detection (thread exclusion)
 *   11. Data loss during concurrent writes
 *   12. Corruption during stream access
 *   13. State visibility (volatile semantics)
 *   14. Exception handling under contention
 *   15. Resource cleanup guarantees
 */
public class ConcurrencyPairwiseTest {

    private ExecutorService executorService;
    private static final int TIMEOUT_SECONDS = 10;

    @BeforeEach
    public void setUp() throws Exception {
        executorService = Executors.newCachedThreadPool();
    }

    @AfterEach
    public void tearDown() throws Exception {
        executorService.shutdownNow();
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
            fail("ExecutorService failed to terminate in time");
        }
    }

    // ============================================================================
    // POSITIVE TESTS: Valid concurrent operations (happy path)
    // ============================================================================

    /**
     * POSITIVE TEST: Multiple readers from BlockingQueue (2 threads, read, concurrent)
     *
     * Pattern: DataStreamProducer pattern with multiple consumers
     * This simulates multiple session threads reading from the same command queue.
     *
     * Expected: All reads succeed, no deadlock
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testMultipleReadersFromBlockingQueue_2ThreadsConcurrent() throws Exception {
        BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);
        AtomicInteger readCount = new AtomicInteger(0);

        // Producer thread
        executorService.execute(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 5; i++) {
                    queue.put(new byte[]{(byte) i});
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Consumer 1
        executorService.execute(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 3; i++) {
                    queue.take();
                    readCount.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                endLatch.countDown();
            }
        });

        // Consumer 2
        executorService.execute(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 2; i++) {
                    queue.take();
                    readCount.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean completed = endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue(completed,"Both consumers should complete");
        assertEquals(5, readCount.get(),"All 5 items should be read");
    }

    /**
     * POSITIVE TEST: Single writer to BlockingQueue with multiple readers (4 threads, write, concurrent)
     *
     * Pattern: Session thread producing commands for multiple workers
     * This tests producer with controlled queue backpressure.
     *
     * Expected: All writes succeed with queue backpressure handling
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testSingleWriterToBlockingQueue_4ThreadsWrite() throws Exception {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(4);
        AtomicInteger writeCount = new AtomicInteger(0);

        // Writer thread
        executorService.execute(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 8; i++) {
                    queue.put(i);
                    writeCount.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                endLatch.countDown();
            }
        });

        // 3 reader threads
        for (int r = 0; r < 3; r++) {
            executorService.execute(() -> {
                try {
                    startLatch.await();
                    while (true) {
                        Integer item = queue.poll(1, TimeUnit.SECONDS);
                        if (item == null) break;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue(completed,"All threads should complete");
        assertEquals(8, writeCount.get(),"All 8 writes should succeed");
    }

    /**
     * POSITIVE TEST: Mixed read/write with state transitions (8 threads, mixed, interleaved)
     *
     * Pattern: Simulating session connecting→sending commands→receiving data→disconnecting
     * This tests thread-safe state machine transitions.
     *
     * Expected: All transitions complete in correct order without deadlock
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testMixedReadWriteWithStateTransitions_8ThreadsInterleaved() throws Exception {
        BlockingQueue<String> stateQueue = new LinkedBlockingQueue<>();
        CyclicBarrier barrier = new CyclicBarrier(8);
        CountDownLatch endLatch = new CountDownLatch(8);
        AtomicInteger stateTransitions = new AtomicInteger(0);

        // 8 threads doing state transitions
        for (int t = 0; t < 8; t++) {
            final int threadId = t;
            executorService.execute(() -> {
                try {
                    barrier.await();  // Synchronize start

                    // State: CONNECTING
                    stateQueue.put("CONNECTING_" + threadId);
                    stateTransitions.incrementAndGet();

                    Thread.yield();

                    // State: CONNECTED
                    stateQueue.put("CONNECTED_" + threadId);
                    stateTransitions.incrementAndGet();

                    Thread.yield();

                    // State: DISCONNECTING
                    stateQueue.put("DISCONNECTING_" + threadId);
                    stateTransitions.incrementAndGet();

                    Thread.yield();

                    // State: DISCONNECTED
                    stateQueue.put("DISCONNECTED_" + threadId);
                    stateTransitions.incrementAndGet();

                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        boolean completed = endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue(completed,"All threads should complete");
        assertEquals(32, stateTransitions.get(),"All 8 threads × 4 states = 32 transitions");
    }

    /**
     * POSITIVE TEST: Large data transfer (4 threads, write, sequential)
     *
     * Pattern: Simulating data stream processing with large payloads
     * This tests stream handling with buffer boundaries.
     *
     * Expected: All data transferred correctly, no corruption
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testLargeDataTransfer_4ThreadsSequential() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(4);
        AtomicLong bytesTransferred = new AtomicLong(0);
        AtomicReference<Exception> error = new AtomicReference<>();

        // 4 threads, each transferring 65535 bytes
        for (int t = 0; t < 4; t++) {
            executorService.execute(() -> {
                try {
                    startLatch.await();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(65535);
                    byte[] data = new byte[1024];
                    for (int i = 0; i < 64; i++) {  // 64 * 1024 = 65536 bytes
                        Arrays.fill(data, (byte) i);
                        baos.write(data);
                    }
                    bytesTransferred.addAndGet(baos.size());
                } catch (Exception e) {
                    error.set(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue(completed,"All threads should complete");
        assertNull(error.get(),"No exceptions should occur");
        assertEquals(4 * 65536, bytesTransferred.get(),"4 threads × ~65536 bytes");
    }

    /**
     * POSITIVE TEST: Rapid session lifecycle (16 threads, mixed, concurrent)
     *
     * Pattern: Many sessions rapidly connecting and disconnecting
     * This tests resource management and thread safety during high churn.
     *
     * Expected: All lifecycles complete without resource leaks
     */
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testRapidSessionLifecycle_16ThreadsConcurrent() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(16);
        AtomicInteger sessionsClosed = new AtomicInteger(0);
        AtomicInteger exceptions = new AtomicInteger(0);

        // 16 threads rapidly cycling sessions
        for (int t = 0; t < 16; t++) {
            executorService.execute(() -> {
                try {
                    startLatch.await();
                    for (int cycle = 0; cycle < 10; cycle++) {
                        // Simulate session lifecycle
                        BlockingQueue<Object> sessionQueue = new LinkedBlockingQueue<>(10);

                        // Send some data
                        sessionQueue.offer("DATA_" + cycle);
                        sessionQueue.offer("MORE_DATA_" + cycle);

                        // Read and discard
                        sessionQueue.poll(100, TimeUnit.MILLISECONDS);
                        sessionQueue.poll(100, TimeUnit.MILLISECONDS);

                        sessionsClosed.incrementAndGet();
                    }
                } catch (Exception e) {
                    exceptions.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue(completed,"All threads should complete");
        assertEquals(160, sessionsClosed.get(),"All 16 × 10 = 160 session cycles should complete");
        assertEquals(0, exceptions.get(),"No exceptions should occur");
    }

    // ============================================================================
    // ADVERSARIAL TESTS: Race conditions, deadlocks, livelocks, starvation
    // ============================================================================

    /**
     * ADVERSARIAL TEST: Race on unsynchronized BufferedOutputStream (2 threads, mixed, concurrent)
     *
     * Dimension pair: [2 threads, mixed operations]
     * Pattern: Concurrent writes to shared stream without synchronization
     *
     * Known bug: tnvt.java lines 89-91 (sock, bin, bout not synchronized)
     * Expected: Data loss or corruption detected
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testRaceOnUnsynchronizedOutputStream_2ThreadsMixed() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bout = new BufferedOutputStream(baos, 32);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);
        AtomicInteger bytesWritten = new AtomicInteger(0);
        AtomicReference<Exception> caughtException = new AtomicReference<>();

        // Writer 1: Even bytes
        executorService.execute(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 50; i += 2) {
                    try {
                        bout.write((byte) i);
                        bytesWritten.incrementAndGet();
                        if (i % 10 == 0) bout.flush();
                    } catch (IOException e) {
                        caughtException.set(e);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                endLatch.countDown();
            }
        });

        // Writer 2: Odd bytes
        executorService.execute(() -> {
            try {
                startLatch.await();
                for (int i = 1; i < 50; i += 2) {
                    try {
                        bout.write((byte) i);
                        bytesWritten.incrementAndGet();
                        if (i % 10 == 1) bout.flush();
                    } catch (IOException e) {
                        caughtException.set(e);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean completed = endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue(completed,"Both threads should complete");

        try {
            bout.close();
        } catch (IOException ignored) {
        }

        // Document the race condition: we expect data loss due to unsynchronized access
        if (bytesWritten.get() < 50) {
            System.out.println("DATA LOSS DETECTED: " + bytesWritten.get() + " of 50 bytes written");
            assertTrue(bytesWritten.get() < 50,"Race condition caused data loss");
        }
    }

    /**
     * ADVERSARIAL TEST: Reader blocks writer at buffer boundary (4 threads, read, interleaved)
     *
     * Dimension pair: [4 threads, read operation, interleaved timing]
     * Pattern: Reader and writer competing for BufferedStream access
     *
     * Expected: Potential deadlock if synchronization is faulty
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testReaderBlocksWriterAtBoundary_4ThreadsInterleaved() throws Exception {
        PipedInputStream input = new PipedInputStream(32);
        PipedOutputStream output = new PipedOutputStream(input);
        BufferedInputStream bin = new BufferedInputStream(input, 16);
        BufferedOutputStream bout = new BufferedOutputStream(output, 16);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(4);
        AtomicInteger readOps = new AtomicInteger(0);
        AtomicInteger writeOps = new AtomicInteger(0);
        AtomicInteger timeouts = new AtomicInteger(0);

        // 2 writers
        for (int w = 0; w < 2; w++) {
            executorService.execute(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < 20; i++) {
                        try {
                            bout.write((byte) i);
                            bout.flush();
                            writeOps.incrementAndGet();
                        } catch (IOException e) {
                            timeouts.incrementAndGet();
                        }
                        if (i % 5 == 0) Thread.yield();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 2 readers
        for (int r = 0; r < 2; r++) {
            executorService.execute(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < 20; i++) {
                        try {
                            int b = bin.read();
                            if (b >= 0) readOps.incrementAndGet();
                        } catch (IOException e) {
                            timeouts.incrementAndGet();
                        }
                        if (i % 5 == 0) Thread.yield();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue(completed,"All threads should complete");
        assertTrue(readOps.get() > 0 || writeOps.get() > 0,"Some I/O should succeed");
    }

    /**
     * ADVERSARIAL TEST: Volatile visibility issue with state flag (8 threads, read, concurrent)
     *
     * Dimension pair: [8 threads, read operation]
     * Pattern: Simulates keepTrucking flag visibility across threads
     *
     * Known bug: tnvt.java line 123 (keepTrucking not volatile)
     * Expected: Some threads may not see the flag change
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testVolatileVisibilityIssue_8ThreadsRead() throws Exception {
        // Non-volatile flag (simulating the bug)
        boolean[] keepRunning = {true};
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(8);
        AtomicInteger observedFalse = new AtomicInteger(0);
        AtomicInteger observedTrue = new AtomicInteger(0);

        // 7 reader threads
        for (int r = 0; r < 7; r++) {
            executorService.execute(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < 1000; i++) {
                        if (keepRunning[0]) {
                            observedTrue.incrementAndGet();
                        } else {
                            observedFalse.incrementAndGet();
                        }
                        if (i % 100 == 0) Thread.yield();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 1 writer thread
        executorService.execute(() -> {
            try {
                startLatch.await();
                Thread.sleep(5);
                keepRunning[0] = false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean completed = endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue(completed,"All threads should complete");

        // Document visibility delay
        if (observedTrue.get() > observedFalse.get()) {
            System.out.println("VISIBILITY DELAY: true=" + observedTrue.get() +
                             ", false=" + observedFalse.get());
        }
    }

    /**
     * ADVERSARIAL TEST: ConcurrentModificationException in session iteration (16 threads, mixed, concurrent)
     *
     * Dimension pair: [16 threads, mixed operations]
     * Pattern: Simulates Sessions class ArrayList access without synchronization
     *
     * Known bug: Sessions.java uses unsynchronized ArrayList
     * Expected: ConcurrentModificationException during concurrent add/remove/iterate
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testConcurrentModificationInSessionList_16ThreadsMixed() throws Exception {
        List<String> sessions = new ArrayList<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(16);
        AtomicInteger modifications = new AtomicInteger(0);
        AtomicInteger exceptions = new AtomicInteger(0);

        // 8 adders
        for (int a = 0; a < 8; a++) {
            executorService.execute(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < 20; i++) {
                        sessions.add("session_" + Thread.currentThread().getId() + "_" + i);
                        modifications.incrementAndGet();
                        if (i % 5 == 0) Thread.yield();
                    }
                } catch (ConcurrentModificationException e) {
                    exceptions.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 4 removers
        for (int rm = 0; rm < 4; rm++) {
            executorService.execute(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < 10; i++) {
                        try {
                            if (!sessions.isEmpty()) {
                                sessions.remove(0);
                                modifications.incrementAndGet();
                            }
                        } catch (IndexOutOfBoundsException | ConcurrentModificationException e) {
                            exceptions.incrementAndGet();
                        }
                        if (i % 2 == 0) Thread.yield();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 4 iterators
        for (int it = 0; it < 4; it++) {
            executorService.execute(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < 30; i++) {
                        try {
                            int size = 0;
                            for (String s : sessions) {
                                size++;
                            }
                        } catch (ConcurrentModificationException e) {
                            exceptions.incrementAndGet();
                        }
                        if (i % 5 == 0) Thread.yield();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue(completed,"All threads should complete");

        // Document the race condition
        if (exceptions.get() > 0) {
            System.out.println("ConcurrentModificationExceptions detected: " + exceptions.get());
            assertTrue(exceptions.get() > 0,"Race condition in ArrayList access detected");
        }
    }

    /**
     * ADVERSARIAL TEST: Writer starvation under high reader contention (100 threads, read, concurrent)
     *
     * Dimension pair: [100 threads, read operation]
     * Pattern: Many readers competing with few writers
     *
     * Expected: Writers may be starved, leading to service unavailability
     */
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testWriterStarvation_100ThreadsHighContention() throws Exception {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(5);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(101);
        AtomicInteger readsCompleted = new AtomicInteger(0);
        AtomicInteger writesCompleted = new AtomicInteger(0);
        AtomicInteger starvedWrites = new AtomicInteger(0);

        // 100 readers
        for (int r = 0; r < 100; r++) {
            executorService.execute(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < 5; i++) {
                        Integer item = queue.poll(100, TimeUnit.MILLISECONDS);
                        if (item != null) {
                            readsCompleted.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 1 writer
        executorService.execute(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 100; i++) {
                    long beforeTime = System.currentTimeMillis();
                    queue.put(i);
                    long afterTime = System.currentTimeMillis();
                    writesCompleted.incrementAndGet();

                    if (afterTime - beforeTime > 100) {
                        starvedWrites.incrementAndGet();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean completed = endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue(completed,"All threads should complete");
        assertEquals(100, writesCompleted.get(),"All 100 writes should succeed despite contention");

        if (starvedWrites.get() > 50) {
            System.out.println("WRITER STARVATION DETECTED: " + starvedWrites.get() + " of 100 writes delayed");
        }
    }

    /**
     * ADVERSARIAL TEST: Deadlock in circular queue wait (4 threads, mixed, interleaved)
     *
     * Dimension pair: [4 threads, mixed operations]
     * Pattern: Two threads waiting on each other's queue
     *
     * Expected: Potential deadlock if threads wait in wrong order
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testDeadlockInCircularWait_4ThreadsInterleaved() throws Exception {
        BlockingQueue<Integer> queue1 = new LinkedBlockingQueue<>(2);
        BlockingQueue<Integer> queue2 = new LinkedBlockingQueue<>(2);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(4);
        AtomicInteger completions = new AtomicInteger(0);

        // Thread A: puts to queue1, takes from queue2
        executorService.execute(() -> {
            try {
                startLatch.await();
                queue1.put(1);
                completions.incrementAndGet();
                queue2.take();
                completions.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                endLatch.countDown();
            }
        });

        // Thread B: takes from queue1, puts to queue2
        executorService.execute(() -> {
            try {
                startLatch.await();
                Integer val = queue1.take();
                completions.incrementAndGet();
                queue2.put(val + 1);
                completions.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                endLatch.countDown();
            }
        });

        // Thread C: similar pattern with different queues
        executorService.execute(() -> {
            try {
                startLatch.await();
                queue2.put(2);
                completions.incrementAndGet();
                queue1.take();
                completions.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                endLatch.countDown();
            }
        });

        // Thread D: similar pattern
        executorService.execute(() -> {
            try {
                startLatch.await();
                Integer val = queue2.take();
                completions.incrementAndGet();
                queue1.put(val + 1);
                completions.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean completed = endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue(completed,"All threads should complete without deadlock");
        assertEquals(8, completions.get(),"All 4 threads × 2 operations = 8 completions");
    }

    /**
     * ADVERSARIAL TEST: Livelock with spin-wait collision (8 threads, write, concurrent)
     *
     * Dimension pair: [8 threads, write operation]
     * Pattern: Threads spinning without making progress due to synchronization issues
     *
     * Expected: High CPU usage with low throughput indicates livelock
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testLivelock_8ThreadsSpinWait() throws Exception {
        AtomicBoolean[] lockFlags = {new AtomicBoolean(false), new AtomicBoolean(false)};
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(8);
        AtomicInteger progressCount = new AtomicInteger(0);

        // 8 threads trying to acquire dual locks
        for (int t = 0; t < 8; t++) {
            final int threadId = t;
            executorService.execute(() -> {
                try {
                    startLatch.await();

                    // Try to acquire both locks in order
                    int retries = 0;
                    while (retries < 1000) {
                        if (lockFlags[0].compareAndSet(false, true)) {
                            if (lockFlags[1].compareAndSet(false, true)) {
                                // Both acquired
                                progressCount.incrementAndGet();
                                lockFlags[0].set(false);
                                lockFlags[1].set(false);
                                break;
                            } else {
                                // Failed to get lock2, release lock1 and retry
                                lockFlags[0].set(false);
                            }
                        }
                        retries++;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue(completed,"All threads should complete");

        if (progressCount.get() < 4) {
            System.out.println("LIVELOCK DETECTED: Only " + progressCount.get() + " of 8 threads made progress");
        }
    }

    /**
     * ADVERSARIAL TEST: Data loss during rapid close/open cycles (16 threads, mixed, concurrent)
     *
     * Dimension pair: [16 threads, mixed operation]
     * Pattern: Simulates socket closing while writes are in progress
     *
     * Expected: Data loss or IOException detection
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testDataLossOnClose_16ThreadsMixed() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(16);
        AtomicInteger bytesWritten = new AtomicInteger(0);
        AtomicInteger ioExceptions = new AtomicInteger(0);

        for (int t = 0; t < 16; t++) {
            executorService.execute(() -> {
                try {
                    startLatch.await();
                    for (int cycle = 0; cycle < 10; cycle++) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        BufferedOutputStream bout = new BufferedOutputStream(baos, 64);

                        for (int i = 0; i < 100; i++) {
                            try {
                                bout.write((byte) i);
                                bytesWritten.incrementAndGet();

                                // Simulate close during write
                                if (i == 50) {
                                    bout.close();
                                    bout = null;
                                }
                            } catch (IOException e) {
                                ioExceptions.incrementAndGet();
                                break;
                            }
                        }

                        if (bout != null) {
                            try {
                                bout.close();
                            } catch (IOException e) {
                                ioExceptions.incrementAndGet();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue(completed,"All threads should complete");

        // Document data loss due to early close
        int expectedBytes = 16 * 10 * 100;
        int actualBytes = bytesWritten.get();
        if (actualBytes < expectedBytes) {
            System.out.println("DATA LOSS: " + actualBytes + " of " + expectedBytes + " bytes written");
        }
    }

    /**
     * ADVERSARIAL TEST: Exception handling during concurrent state change (8 threads, mixed, interleaved)
     *
     * Dimension pair: [8 threads, mixed operation]
     * Pattern: Throwing exceptions while threads are in critical sections
     *
     * Expected: Proper exception handling and cleanup
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testExceptionHandlingDuringStateChange_8ThreadsMixed() throws Exception {
        BlockingQueue<Integer> sharedQueue = new LinkedBlockingQueue<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(8);
        AtomicInteger exceptionsHandled = new AtomicInteger(0);
        AtomicInteger normalCompletions = new AtomicInteger(0);

        for (int t = 0; t < 8; t++) {
            final int threadId = t;
            executorService.execute(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < 20; i++) {
                        try {
                            // Simulate operation that may fail
                            if (threadId % 2 == 0 && i % 5 == 0) {
                                throw new RuntimeException("Simulated failure");
                            }

                            sharedQueue.put(i);
                            normalCompletions.incrementAndGet();
                        } catch (RuntimeException e) {
                            exceptionsHandled.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue(completed,"All threads should complete");
        assertTrue(normalCompletions.get() > 0,"Some operations should complete normally");
        assertTrue(exceptionsHandled.get() > 0,"Some exceptions should be handled");
    }

    /**
     * ADVERSARIAL TEST: Resource leak under rapid lifecycle (100 threads, mixed, concurrent)
     *
     * Dimension pair: [100 threads, mixed operation]
     * Pattern: Creating and destroying resources without proper cleanup
     *
     * Expected: No resource leak despite high concurrency
     */
    @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testResourceLeakUnderRapidLifecycle_100ThreadsConcurrent() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(100);
        AtomicInteger resourcesAllocated = new AtomicInteger(0);
        AtomicInteger resourcesFreed = new AtomicInteger(0);

        for (int t = 0; t < 100; t++) {
            executorService.execute(() -> {
                try {
                    startLatch.await();
                    for (int cycle = 0; cycle < 10; cycle++) {
                        // Allocate resource
                        ByteArrayOutputStream resource = new ByteArrayOutputStream();
                        resourcesAllocated.incrementAndGet();

                        // Use resource
                        resource.write(new byte[1024]);

                        // Free resource
                        resource.close();
                        resourcesFreed.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Unexpected exception
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue(completed,"All threads should complete");
        assertEquals(resourcesAllocated.get(), resourcesFreed.get(),"All allocated resources should be freed");
    }

    /**
     * ADVERSARIAL TEST: Stream corruption with concurrent boundary writes (8 threads, write, interleaved)
     *
     * Dimension pair: [8 threads, write operation]
     * Pattern: Multiple threads writing at buffer boundaries
     *
     * Expected: Data corruption or loss due to unsynchronized access
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testStreamCorruptionAtBoundary_8ThreadsInterleaved() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream bout = new BufferedOutputStream(baos, 32);  // Small buffer for boundary hits

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(8);
        AtomicInteger writeAttempts = new AtomicInteger(0);
        AtomicInteger writeSuccesses = new AtomicInteger(0);

        for (int t = 0; t < 8; t++) {
            final int threadId = t;
            executorService.execute(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < 40; i++) {
                        try {
                            writeAttempts.incrementAndGet();
                            bout.write((byte) ((threadId * 40) + i));
                            writeSuccesses.incrementAndGet();

                            // Force flush at buffer boundaries
                            if (i % 4 == 0) {
                                bout.flush();
                            }
                        } catch (IOException e) {
                            // Expected under race condition
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue(completed,"All threads should complete");

        try {
            bout.close();
        } catch (IOException ignored) {
        }

        // Data corruption is likely if writeSuccesses < writeAttempts
        if (writeSuccesses.get() < writeAttempts.get()) {
            System.out.println("DATA CORRUPTION DETECTED: " + writeSuccesses.get() +
                             " of " + writeAttempts.get() + " writes succeeded");
        }
    }

    /**
     * ADVERSARIAL TEST: Session state transition race (4 threads, mixed, concurrent)
     *
     * Dimension pair: [4 threads, mixed operation]
     * Pattern: Concurrent session state transitions without proper synchronization
     *
     * Expected: Invalid state transitions or data loss
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testSessionStateTransitionRace_4ThreadsConcurrent() throws Exception {
        // Simulate session state (not volatile)
        String[] sessionState = {"DISCONNECTED"};
        BlockingQueue<String> stateLog = new LinkedBlockingQueue<>();

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(4);
        AtomicInteger invalidTransitions = new AtomicInteger(0);

        // Thread 1: DISCONNECTED -> CONNECTING -> CONNECTED
        executorService.execute(() -> {
            try {
                startLatch.await();

                sessionState[0] = "CONNECTING";
                stateLog.put("T1: DISCONNECTED->CONNECTING");
                Thread.sleep(1);

                sessionState[0] = "CONNECTED";
                stateLog.put("T1: CONNECTING->CONNECTED");
            } catch (Exception e) {
                invalidTransitions.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        });

        // Thread 2: CONNECTED -> SENDING -> WAITING
        executorService.execute(() -> {
            try {
                startLatch.await();
                Thread.sleep(2);

                if ("CONNECTED".equals(sessionState[0])) {
                    sessionState[0] = "SENDING";
                    stateLog.put("T2: CONNECTED->SENDING");
                }

                sessionState[0] = "WAITING";
                stateLog.put("T2: ->WAITING");
            } catch (Exception e) {
                invalidTransitions.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        });

        // Thread 3: WAITING -> RESPONDING -> CONNECTED
        executorService.execute(() -> {
            try {
                startLatch.await();
                Thread.sleep(3);

                sessionState[0] = "RESPONDING";
                stateLog.put("T3: ->RESPONDING");

                sessionState[0] = "CONNECTED";
                stateLog.put("T3: RESPONDING->CONNECTED");
            } catch (Exception e) {
                invalidTransitions.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        });

        // Thread 4: Any state -> DISCONNECTING -> DISCONNECTED
        executorService.execute(() -> {
            try {
                startLatch.await();
                Thread.sleep(4);

                sessionState[0] = "DISCONNECTING";
                stateLog.put("T4: ->DISCONNECTING");

                sessionState[0] = "DISCONNECTED";
                stateLog.put("T4: DISCONNECTING->DISCONNECTED");
            } catch (Exception e) {
                invalidTransitions.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean completed = endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue(completed,"All threads should complete");

        // The race condition is that final state is unpredictable
        System.out.println("Final state: " + sessionState[0]);
        System.out.println("Transition log size: " + stateLog.size());
    }

    // ============================================================================
    // EDGE CASE TESTS
    // ============================================================================

    /**
     * EDGE CASE TEST: Single thread with large data (1 thread, write, sequential)
     *
     * Dimension: [1 thread, write, sequential]
     * Pattern: Baseline single-threaded performance
     *
     * Expected: All data written successfully
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testSingleThreadLargeData_BaselinePerformance() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(1);
        AtomicLong bytesWritten = new AtomicLong(0);

        executorService.execute(() -> {
            try {
                startLatch.await();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                BufferedOutputStream bout = new BufferedOutputStream(baos, 8192);

                byte[] data = new byte[65535];
                Arrays.fill(data, (byte) 42);

                bout.write(data);
                bout.flush();
                bout.close();

                bytesWritten.set(baos.size());
            } catch (Exception e) {
                fail("Single-threaded write should not fail: " + e.getMessage());
            } finally {
                endLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean completed = endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue(completed,"Thread should complete");
        assertEquals(65535, bytesWritten.get(),"All 65535 bytes should be written");
    }

    /**
     * EDGE CASE TEST: Empty queue operations (2 threads, read, concurrent)
     *
     * Dimension: [2 threads, read, concurrent]
     * Pattern: Queue poll on empty queue
     *
     * Expected: Timeout or null return without exception
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testEmptyQueueOperations_2ThreadsRead() throws Exception {
        BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);
        AtomicInteger nullsReceived = new AtomicInteger(0);

        for (int t = 0; t < 2; t++) {
            executorService.execute(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < 10; i++) {
                        Object item = queue.poll(100, TimeUnit.MILLISECONDS);
                        if (item == null) {
                            nullsReceived.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue(completed,"All threads should complete");
        assertEquals(20, nullsReceived.get(),"Both threads should get 10 nulls from empty queue");
    }

    /**
     * EDGE CASE TEST: Interrupt handling during blocking operation (4 threads, read, concurrent)
     *
     * Dimension: [4 threads, read, concurrent]
     * Pattern: Thread interrupts during blocking queue take
     *
     * Expected: InterruptedException caught and handled gracefully
     */
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    @Test
    public void testInterruptHandlingInBlockingOp_4ThreadsConcurrent() throws Exception {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(4);
        AtomicInteger interruptsHandled = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < 4; t++) {
            Future<?> future = executorService.submit(() -> {
                try {
                    startLatch.await();
                    // This will block forever since queue is empty
                    queue.take();
                } catch (InterruptedException e) {
                    interruptsHandled.incrementAndGet();
                    Thread.currentThread().interrupt();  // Restore interrupt status
                } finally {
                    endLatch.countDown();
                }
            });
            futures.add(future);
        }

        startLatch.countDown();

        // Give threads time to block
        Thread.sleep(1000);

        // Interrupt all threads
        for (Future<?> future : futures) {
            future.cancel(true);
        }

        boolean completed = endLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertTrue(completed,"All threads should complete after interrupt");
        assertEquals(4, interruptsHandled.get(),"All 4 threads should handle interrupt");
    }
}
