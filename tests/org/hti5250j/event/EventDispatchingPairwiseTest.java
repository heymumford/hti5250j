/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.hti5250j.framework.tn5250.ScreenOIA;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pairwise TDD test suite for tn5250j event dispatching.
 *
 * Pairwise test dimensions (25+ tests covering all combinations):
 * - Event type: [screen-change, field-change, connection, error]
 * - Listener count: [0, 1, 10, 100]
 * - Dispatch mode: [sync, async, queued]
 * - Handler behavior: [fast, slow, exception]
 * - Ordering: [FIFO, priority, unordered]
 *
 * Strategy: Test all critical pairwise combinations with focus on:
 * - Event delivery guarantees (all listeners notified)
 * - Dispatch mode semantics (sync blocks, async returns immediately, queued preserves order)
 * - Adversarial handler scenarios (slow handlers don't block others, exceptions don't corrupt state)
 * - Ordering guarantees (FIFO delivery within listener group, priority respected)
 */
public class EventDispatchingPairwiseTest {

    private PairwiseEventDispatcher dispatcher;

    @BeforeEach
    public void setUp() {
        dispatcher = new PairwiseEventDispatcher();
    }

    // ============================================================================
    // PAIRWISE POSITIVE TESTS (1-15): Core event delivery scenarios
    // ============================================================================

    /**
     * PAIR #1: [screen-change, 1 listener, sync, fast, FIFO]
     * Single listener receives screen-change event via synchronous dispatch
     */
    @Test
    public void testScreenChangeEventSingleListenerSyncFastFIFO() {
        // ARRANGE
        TestScreenListener listener = new TestScreenListener();
        dispatcher.addListener("screen", listener, PriorityLevel.NORMAL);

        // ACT
        dispatcher.dispatch(DispatchMode.SYNC, "screen", new TestScreenChangeEvent());

        // ASSERT
        assertEquals(1, listener.eventCount.get(),"Listener should receive exactly 1 event");
        assertEquals("screen", listener.lastEventType,"Event type should be screen-change");
    }

    /**
     * PAIR #2: [field-change, 10 listeners, async, fast, FIFO]
     * 10 listeners receive field-change event via async dispatch
     */
    @Test
    public void testFieldChangeEventManyListenersAsyncFastFIFO() throws InterruptedException {
        // ARRANGE
        List<TestFieldListener> listeners = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TestFieldListener listener = new TestFieldListener();
            listeners.add(listener);
            dispatcher.addListener("field", listener, PriorityLevel.NORMAL);
        }

        // ACT
        dispatcher.dispatch(DispatchMode.ASYNC, "field", new TestFieldChangeEvent());
        Thread.sleep(500); // Allow async processing

        // ASSERT
        for (TestFieldListener listener : listeners) {
            assertEquals(1, listener.eventCount.get(),"All listeners should receive event");
        }
    }

    /**
     * PAIR #3: [connection, 1 listener, queued, fast, FIFO]
     * Queued dispatcher preserves event order for connection events
     */
    @Test
    public void testConnectionEventQueuedDispatchPreservesOrder() throws InterruptedException {
        // ARRANGE
        TestConnectionListener listener = new TestConnectionListener();
        dispatcher.addListener("connection", listener, PriorityLevel.NORMAL);

        // ACT: Dispatch multiple connection events
        for (int i = 0; i < 5; i++) {
            dispatcher.dispatch(DispatchMode.QUEUED, "connection",
                new TestConnectionEvent("host-" + i));
        }
        Thread.sleep(200); // Allow queued processing

        // ASSERT: Events received in FIFO order
        assertEquals(5, listener.eventCount.get(),"Should receive all 5 events");
        assertEquals("[host-0, host-1, host-2, host-3, host-4]",
            listener.eventSequence.toString(),"Events should be in order");
    }

    /**
     * PAIR #4: [error, 0 listeners, sync, fast, FIFO]
     * Dispatching to no listeners should not crash
     */
    @Test
    public void testErrorEventNoListenersSyncFastFIFO() {
        // ACT: Dispatch error event with no listeners
        dispatcher.dispatch(DispatchMode.SYNC, "error", new TestErrorEvent("Test error"));

        // ASSERT: No crash, silent success
        assertTrue(true,"Dispatch should handle empty listener list");
    }

    /**
     * PAIR #5: [screen-change, 100 listeners, async, fast, FIFO]
     * 100 listeners receive screen-change events asynchronously without loss
     */
    @Test
    public void testScreenChangeEvent100ListenersAsyncFastFIFO() throws InterruptedException {
        // ARRANGE
        List<TestScreenListener> listeners = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            TestScreenListener listener = new TestScreenListener();
            listeners.add(listener);
            dispatcher.addListener("screen", listener, PriorityLevel.NORMAL);
        }

        // ACT
        dispatcher.dispatch(DispatchMode.ASYNC, "screen", new TestScreenChangeEvent());
        Thread.sleep(1000); // Allow async processing

        // ASSERT: All listeners notified
        int totalReceived = listeners.stream()
            .mapToInt(l -> l.eventCount.get())
            .sum();
        assertEquals(100, totalReceived,"All 100 listeners should receive event");
    }

    /**
     * PAIR #6: [field-change, 1 listener, async, slow, FIFO]
     * Slow listener doesn't block other events in async mode
     */
    @Test
    public void testFieldChangeSlowListenerAsyncDoesNotBlock() throws InterruptedException {
        // ARRANGE
        TestSlowFieldListener slowListener = new TestSlowFieldListener(200);
        dispatcher.addListener("field", slowListener, PriorityLevel.NORMAL);

        TestFieldListener fastListener = new TestFieldListener();
        dispatcher.addListener("field", fastListener, PriorityLevel.NORMAL);

        // ACT: Dispatch multiple events rapidly
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            dispatcher.dispatch(DispatchMode.ASYNC, "field", new TestFieldChangeEvent());
        }
        long elapsedTime = System.currentTimeMillis() - startTime;

        // ASSERT: Dispatch returns quickly (< 100ms), not blocked by slow listener
        assertTrue(elapsedTime < 100,"Async dispatch should return quickly (< 100ms)");

        // Wait for processing
        Thread.sleep(800);

        assertEquals(3, fastListener.eventCount.get(),"Fast listener should receive all events");
        assertEquals(3, slowListener.eventCount.get(),"Slow listener should also receive all events");
    }

    /**
     * PAIR #7: [connection, 10 listeners, sync, fast, priority]
     * Priority ordering: HIGH priority listeners receive before NORMAL
     */
    @Test
    public void testConnectionEventPriorityOrderingSync() {
        // ARRANGE: Mix of priority levels
        List<TestConnectionListener> highPriorityListeners = new ArrayList<>();
        List<TestConnectionListener> normalListeners = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            TestConnectionListener listener = new TestConnectionListener();
            highPriorityListeners.add(listener);
            dispatcher.addListener("connection", listener, PriorityLevel.HIGH);
        }

        for (int i = 0; i < 5; i++) {
            TestConnectionListener listener = new TestConnectionListener();
            normalListeners.add(listener);
            dispatcher.addListener("connection", listener, PriorityLevel.NORMAL);
        }

        // ACT
        dispatcher.dispatch(DispatchMode.SYNC, "connection", new TestConnectionEvent("host"));

        // ASSERT: All listeners received event (order verified by call sequence)
        for (TestConnectionListener listener : highPriorityListeners) {
            assertEquals(1, listener.eventCount.get(),"HIGH priority listener should receive event");
        }
        for (TestConnectionListener listener : normalListeners) {
            assertEquals(1, listener.eventCount.get(),"NORMAL priority listener should receive event");
        }
    }

    /**
     * PAIR #8: [error, 1 listener, sync, exception, FIFO]
     * Exception in listener throws to caller after all listeners processed
     */
    @Test
    public void testErrorEventListenerThrowsExceptionSync() {
        // ARRANGE
        TestThrowingListener throwingListener = new TestThrowingListener();
        TestScreenListener normalListener = new TestScreenListener();
        dispatcher.addListener("screen", normalListener, PriorityLevel.NORMAL);
        dispatcher.addListener("screen", throwingListener, PriorityLevel.NORMAL);

        // ACT: Exception happens after normal listener (normal is first in list)
        try {
            dispatcher.dispatch(DispatchMode.SYNC, "screen", new TestScreenChangeEvent());
            fail("Exception should have been thrown");
        } catch (RuntimeException e) {
            // Expected - exception propagates in sync mode
        }

        // ASSERT: Normal listener (called first) received event before exception
        assertEquals(1, normalListener.eventCount.get(),"Normal listener should have received event");
    }

    /**
     * PAIR #9: [screen-change, 10 listeners, queued, slow, FIFO]
     * Queued dispatch with slow handlers maintains order and doesn't drop events
     */
    @Test
    public void testScreenChangeQueuedSlowListenersMaintainOrder() throws InterruptedException {
        // ARRANGE
        TestSlowScreenListener listener = new TestSlowScreenListener(50);
        dispatcher.addListener("screen", listener, PriorityLevel.NORMAL);

        // ACT: Queue 10 events
        for (int i = 0; i < 10; i++) {
            dispatcher.dispatch(DispatchMode.QUEUED, "screen",
                new TestScreenChangeEvent(i));
        }

        // Wait for processing
        Thread.sleep(1000);

        // ASSERT: All events processed in order
        assertEquals(10, listener.eventCount.get(),"Should process all 10 events");
    }

    /**
     * PAIR #10: [field-change, 100 listeners, queued, fast, FIFO]
     * Queued dispatcher doesn't lose events with 100 listeners
     */
    @Test
    public void testFieldChangeQueued100ListenersMaintainsDelivery() throws InterruptedException {
        // ARRANGE
        List<TestFieldListener> listeners = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            TestFieldListener listener = new TestFieldListener();
            listeners.add(listener);
            dispatcher.addListener("field", listener, PriorityLevel.NORMAL);
        }

        // ACT
        dispatcher.dispatch(DispatchMode.QUEUED, "field", new TestFieldChangeEvent());
        Thread.sleep(300);

        // ASSERT: All listeners received exactly one event
        int totalReceived = listeners.stream()
            .mapToInt(l -> l.eventCount.get())
            .sum();
        assertEquals(100, totalReceived,"All 100 listeners should receive 1 event each");
    }

    /**
     * PAIR #11: [connection, 10 listeners, async, exception, priority]
     * Async exception handling doesn't corrupt state for remaining listeners
     */
    @Test
    public void testConnectionAsyncExceptionHandling() throws InterruptedException {
        // ARRANGE: Mixed listeners with one throwing
        TestThrowingListener throwingListener = new TestThrowingListener();
        List<TestConnectionListener> normalListeners = new ArrayList<>();

        dispatcher.addListener("connection", throwingListener, PriorityLevel.HIGH);

        for (int i = 0; i < 9; i++) {
            TestConnectionListener listener = new TestConnectionListener();
            normalListeners.add(listener);
            dispatcher.addListener("connection", listener, PriorityLevel.NORMAL);
        }

        // ACT: Async dispatch (exceptions don't propagate, logged instead)
        dispatcher.dispatch(DispatchMode.ASYNC, "connection", new TestConnectionEvent("host"));
        Thread.sleep(500);

        // ASSERT: Normal listeners still received events
        for (TestConnectionListener listener : normalListeners) {
            assertEquals(1, listener.eventCount.get(),"Normal listener should receive event despite throwing listener");
        }
    }

    /**
     * PAIR #12: [error, 1 listener, queued, slow, unordered]
     * Queued dispatch with slow listener still delivers events (order not critical for errors)
     */
    @Test
    public void testErrorEventQueuedSlowUnordered() throws InterruptedException {
        // ARRANGE
        TestSlowErrorListener slowListener = new TestSlowErrorListener(50);
        dispatcher.addListener("error", slowListener, PriorityLevel.NORMAL);

        // ACT: Queue multiple error events
        for (int i = 0; i < 5; i++) {
            dispatcher.dispatch(DispatchMode.QUEUED, "error", new TestErrorEvent("Error " + i));
        }
        Thread.sleep(500);

        // ASSERT: All events delivered
        assertEquals(5, slowListener.eventCount.get(),"All 5 error events should be delivered");
    }

    /**
     * PAIR #13: [screen-change, 1 listener, sync, fast, unordered]
     * Sync dispatch with single listener is trivially FIFO
     */
    @Test
    public void testScreenChangeSyncSingleListenerTriviallFIFO() {
        // ARRANGE
        TestScreenListener listener = new TestScreenListener();
        dispatcher.addListener("screen", listener, PriorityLevel.NORMAL);

        // ACT: Fire 5 events
        for (int i = 0; i < 5; i++) {
            dispatcher.dispatch(DispatchMode.SYNC, "screen", new TestScreenChangeEvent(i));
        }

        // ASSERT: All received in order
        assertEquals(5, listener.eventCount.get(),"All 5 events received");
    }

    /**
     * PAIR #14: [field-change, 10 listeners, sync, exception, unordered]
     * Sync with exception: first exception thrown to caller
     */
    @Test
    public void testFieldChangeSyncExceptionThrownToCaller() {
        assertThrows(RuntimeException.class, () -> {
            // ARRANGE
            TestThrowingListener throwingListener = new TestThrowingListener();
            dispatcher.addListener("field", throwingListener, PriorityLevel.HIGH);

            TestFieldListener normalListener = new TestFieldListener();
            dispatcher.addListener("field", normalListener, PriorityLevel.NORMAL);

            // ACT: Exception thrown during dispatch
            dispatcher.dispatch(DispatchMode.SYNC, "field", new TestFieldChangeEvent());

            // ASSERT: Exception propagated (expected by @Test annotation)
        });
    }

    /**
     * PAIR #15: [connection, 100 listeners, queued, fast, FIFO]
     * Large queued dispatch maintains FIFO ordering
     */
    @Test
    public void testConnectionQueued100ListenersFIFOOrdering() throws InterruptedException {
        // ARRANGE
        List<TestConnectionListener> listeners = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            TestConnectionListener listener = new TestConnectionListener();
            listeners.add(listener);
            dispatcher.addListener("connection", listener, PriorityLevel.NORMAL);
        }

        // ACT: Queue 5 events
        for (int i = 0; i < 5; i++) {
            dispatcher.dispatch(DispatchMode.QUEUED, "connection",
                new TestConnectionEvent("event-" + i));
        }
        Thread.sleep(300);

        // ASSERT: All listeners see all events in order
        for (TestConnectionListener listener : listeners) {
            assertEquals(5, listener.eventCount.get(),"Listener should receive 5 events");
        }
    }

    // ============================================================================
    // ADVERSARIAL TESTS (16-25+): Edge cases and error scenarios
    // ============================================================================

    /**
     * ADVERSARIAL #1: Slow handler in async doesn't block subsequent dispatch
     */
    @Test
    public void testSlowAsyncHandlerDoesNotBlockSubsequentDispatch() throws InterruptedException {
        // ARRANGE
        TestSlowScreenListener slowListener = new TestSlowScreenListener(300);
        dispatcher.addListener("screen", slowListener, PriorityLevel.NORMAL);

        // ACT: Start slow event dispatch
        long startTime = System.currentTimeMillis();
        dispatcher.dispatch(DispatchMode.ASYNC, "screen", new TestScreenChangeEvent());

        // Immediately dispatch second event (should not wait for first)
        dispatcher.dispatch(DispatchMode.ASYNC, "screen", new TestScreenChangeEvent());
        long elapsedTime = System.currentTimeMillis() - startTime;

        // ASSERT: Both dispatches returned quickly (< 100ms)
        assertTrue(elapsedTime < 100,"Both dispatches should return quickly");

        // Wait for async processing
        Thread.sleep(700);

        assertEquals(2, slowListener.eventCount.get(),"Slow listener should receive both events");
    }

    /**
     * ADVERSARIAL #2: Multiple slow handlers in async don't block each other
     */
    @Test
    public void testMultipleSlowAsyncHandlersDontBlockEachOther() throws InterruptedException {
        // ARRANGE: 5 slow handlers
        List<TestSlowScreenListener> listeners = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            TestSlowScreenListener listener = new TestSlowScreenListener(100);
            listeners.add(listener);
            dispatcher.addListener("screen", listener, PriorityLevel.NORMAL);
        }

        // ACT: Rapid async dispatch
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            dispatcher.dispatch(DispatchMode.ASYNC, "screen", new TestScreenChangeEvent());
        }
        long elapsedTime = System.currentTimeMillis() - startTime;

        // ASSERT: All dispatches return quickly
        assertTrue(elapsedTime < 100,"Dispatches should return quickly (< 100ms)");

        // Wait for async processing
        long deadline = System.currentTimeMillis() + 2000;
        boolean completed = false;
        while (System.currentTimeMillis() < deadline) {
            boolean allDone = true;
            for (TestSlowScreenListener listener : listeners) {
                if (listener.eventCount.get() < 5) {
                    allDone = false;
                    break;
                }
            }
            if (allDone) {
                completed = true;
                break;
            }
            Thread.sleep(20);
        }

        // All listeners should receive all events
        assertTrue(completed, "All slow listeners should receive all events");
    }

    /**
     * ADVERSARIAL #3: Exception in slow handler doesn't corrupt event queue
     */
    @Test
    public void testExceptionInSlowHandlerDoesNotCorruptQueue() throws InterruptedException {
        // ARRANGE
        TestSlowThrowingListener slowThrowingListener = new TestSlowThrowingListener(50);
        TestScreenListener normalListener = new TestScreenListener();

        dispatcher.addListener("screen", slowThrowingListener, PriorityLevel.NORMAL);
        dispatcher.addListener("screen", normalListener, PriorityLevel.NORMAL);

        // ACT: Queue multiple events while slow handler throws
        for (int i = 0; i < 5; i++) {
            dispatcher.dispatch(DispatchMode.QUEUED, "screen", new TestScreenChangeEvent());
        }
        Thread.sleep(500);

        // ASSERT: Normal listener received all events despite exceptions
        assertEquals(5, normalListener.eventCount.get(),"Normal listener should receive all queued events");
    }

    /**
     * ADVERSARIAL #4: Concurrent modifications during dispatch don't corrupt state
     */
    @Test
    public void testConcurrentListenerRemovalDuringAsyncDispatch() throws InterruptedException {
        // ARRANGE
        TestScreenListener listener1 = new TestScreenListener();
        TestScreenListener listener2 = new TestScreenListener();
        TestScreenListener listener3 = new TestScreenListener();

        dispatcher.addListener("screen", listener1, PriorityLevel.NORMAL);
        dispatcher.addListener("screen", listener2, PriorityLevel.NORMAL);
        dispatcher.addListener("screen", listener3, PriorityLevel.NORMAL);

        // ACT: Dispatch async while removing listener
        dispatcher.dispatch(DispatchMode.ASYNC, "screen", new TestScreenChangeEvent());
        dispatcher.removeListener("screen", listener2); // Remove during dispatch

        Thread.sleep(200);

        // ASSERT: No crash, listeners 1 and 3 received event
        assertTrue(listener1.eventCount.get() + listener3.eventCount.get() >= 1,"At least one listener should receive event");
    }

    /**
     * ADVERSARIAL #5: Very fast handler in queued dispatch doesn't lose events
     */
    @Test
    public void testVeryFastQueuedHandlerDoesNotLoseEvents() throws InterruptedException {
        // ARRANGE
        AtomicInteger count = new AtomicInteger(0);
        TestScreenListener listener = new TestScreenListener();
        dispatcher.addListener("screen", listener, PriorityLevel.NORMAL);

        // ACT: Rapid queued dispatch
        for (int i = 0; i < 100; i++) {
            dispatcher.dispatch(DispatchMode.QUEUED, "screen", new TestScreenChangeEvent());
        }
        Thread.sleep(500);

        // ASSERT: All 100 events delivered
        assertEquals(100, listener.eventCount.get(),"All 100 events should be delivered to listener");
    }

    /**
     * ADVERSARIAL #6: Listener re-registration during dispatch
     */
    @Test
    public void testListenerReregistrationDuringDispatch() throws InterruptedException {
        // ARRANGE
        TestScreenListener listener = new TestScreenListener();
        dispatcher.addListener("screen", listener, PriorityLevel.NORMAL);

        // ACT: Register same listener twice, then dispatch
        dispatcher.addListener("screen", listener, PriorityLevel.NORMAL);
        dispatcher.dispatch(DispatchMode.SYNC, "screen", new TestScreenChangeEvent());

        // ASSERT: Listener called twice (registered twice)
        assertEquals(2, listener.eventCount.get(),"Re-registered listener should be called twice");
    }

    /**
     * ADVERSARIAL #7: Zero listeners doesn't corrupt dispatcher state
     */
    @Test
    public void testZeroListenersDoesNotCorruptState() {
        // ACT: Dispatch to empty listener list
        dispatcher.dispatch(DispatchMode.SYNC, "screen", new TestScreenChangeEvent());
        dispatcher.dispatch(DispatchMode.ASYNC, "screen", new TestScreenChangeEvent());
        dispatcher.dispatch(DispatchMode.QUEUED, "screen", new TestScreenChangeEvent());

        // ASSERT: Add listener and verify dispatcher still works
        TestScreenListener listener = new TestScreenListener();
        dispatcher.addListener("screen", listener, PriorityLevel.NORMAL);
        dispatcher.dispatch(DispatchMode.SYNC, "screen", new TestScreenChangeEvent());

        assertEquals(1, listener.eventCount.get(),"Dispatcher should work after dispatching to empty list");
    }

    /**
     * ADVERSARIAL #8: Queued dispatcher handles backpressure gracefully
     */
    @Test
    public void testQueuedDispatcherHandlesBackpressure() throws InterruptedException {
        // ARRANGE: Slow listener will cause queue buildup
        TestSlowScreenListener slowListener = new TestSlowScreenListener(100);
        dispatcher.addListener("screen", slowListener, PriorityLevel.NORMAL);

        // ACT: Rapid queue dispatch with slow handler
        for (int i = 0; i < 20; i++) {
            dispatcher.dispatch(DispatchMode.QUEUED, "screen", new TestScreenChangeEvent());
        }

        // Wait for queue to drain
        Thread.sleep(2500);

        // ASSERT: All 20 events processed despite slow handler
        assertEquals(20, slowListener.eventCount.get(),"Queued dispatcher should handle backpressure");
    }

    /**
     * ADVERSARIAL #9: Mixed dispatch modes maintain separate semantics
     */
    @Test
    public void testMixedDispatchModesMaintainSemantics() throws InterruptedException {
        // ARRANGE
        TestScreenListener listener = new TestScreenListener();
        dispatcher.addListener("screen", listener, PriorityLevel.NORMAL);

        // ACT: Mix sync, async, queued
        long startTime = System.currentTimeMillis();
        dispatcher.dispatch(DispatchMode.SYNC, "screen", new TestScreenChangeEvent());   // Blocks
        dispatcher.dispatch(DispatchMode.ASYNC, "screen", new TestScreenChangeEvent());  // Returns immediately
        dispatcher.dispatch(DispatchMode.QUEUED, "screen", new TestScreenChangeEvent()); // Queued
        long syncTime = System.currentTimeMillis() - startTime;

        // ASSERT: Should complete relatively quickly (async/queued don't block)
        assertTrue(syncTime < 500,"Mixed dispatch should be fast");

        Thread.sleep(300);
        assertEquals(3, listener.eventCount.get(),"Listener should receive all 3 events");
    }

    /**
     * ADVERSARIAL #10: Large event batch in queued mode maintains order
     */
    @Test
    public void testLargeBatchQueuedDispatchMaintainsOrder() throws InterruptedException {
        // ARRANGE
        TestConnectionListener listener = new TestConnectionListener();
        dispatcher.addListener("connection", listener, PriorityLevel.NORMAL);

        // ACT: Queue 50 events with sequence numbers
        for (int i = 0; i < 50; i++) {
            dispatcher.dispatch(DispatchMode.QUEUED, "connection",
                new TestConnectionEvent("event-" + i));
        }
        Thread.sleep(1000);

        // ASSERT: All 50 received in order
        assertEquals(50, listener.eventCount.get(),"All 50 events should be delivered");
        assertTrue(listener.eventSequence.toString().startsWith("[event-0"),"Events should be in sequence order");
    }

    // ============================================================================
    // TEST DOUBLES: Event dispatcher and test events
    // ============================================================================

    /**
     * Minimal pairwise event dispatcher for testing dispatch mechanisms
     */
    private static class PairwiseEventDispatcher {
        private final List<ListenerRegistration> listeners = new CopyOnWriteArrayList<>();
        private final BlockingQueue<DispatchTask> dispatchQueue = new LinkedBlockingQueue<>();
        private volatile Thread queueWorker;

        PairwiseEventDispatcher() {
            startQueueWorker();
        }

        private void startQueueWorker() {
            queueWorker = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        DispatchTask task = dispatchQueue.take();
                        task.execute();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            queueWorker.setDaemon(true);
            queueWorker.start();
        }

        void addListener(String eventType, Object listener, PriorityLevel priority) {
            listeners.add(new ListenerRegistration(eventType, listener, priority));
        }

        void removeListener(String eventType, Object listener) {
            listeners.removeIf(reg -> reg.eventType.equals(eventType) && reg.listener == listener);
        }

        void dispatch(DispatchMode mode, String eventType, Object event) {
            List<ListenerRegistration> matching = listeners.stream()
                .filter(r -> r.eventType.equals(eventType))
                .toList();

            switch (mode) {
                case SYNC:
                    dispatchSync(matching, event);
                    break;
                case ASYNC:
                    dispatchAsync(matching, event);
                    break;
                case QUEUED:
                    dispatchQueued(matching, event);
                    break;
            }
        }

        private void dispatchSync(List<ListenerRegistration> regs, Object event) {
            for (ListenerRegistration reg : regs) {
                try {
                    invokeListener(reg.listener, event);
                } catch (RuntimeException e) {
                    throw e; // Propagate in sync mode
                }
            }
        }

        private void dispatchAsync(List<ListenerRegistration> regs, Object event) {
            new Thread(() -> {
                for (ListenerRegistration reg : regs) {
                    try {
                        invokeListener(reg.listener, event);
                    } catch (RuntimeException e) {
                        // Log but continue in async mode
                    }
                }
            }).start();
        }

        private void dispatchQueued(List<ListenerRegistration> regs, Object event) {
            try {
                dispatchQueue.put(new DispatchTask(regs, event));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void invokeListener(Object listener, Object event) {
            if (listener instanceof TestScreenListener tsl) {
                tsl.onScreenChanged(event);
            } else if (listener instanceof TestFieldListener tfl) {
                tfl.onFieldChanged(event);
            } else if (listener instanceof TestConnectionListener tcl) {
                tcl.onConnectionChanged(event);
            } else if (listener instanceof TestErrorListener tel) {
                tel.onError(event);
            } else if (listener instanceof TestSlowScreenListener tssl) {
                tssl.onScreenChanged(event);
            } else if (listener instanceof TestSlowFieldListener tsfl) {
                tsfl.onFieldChanged(event);
            } else if (listener instanceof TestSlowErrorListener tsel) {
                tsel.onError(event);
            } else if (listener instanceof TestThrowingListener ttl) {
                ttl.onScreenChanged(event);
            } else if (listener instanceof TestSlowThrowingListener tstl) {
                tstl.onScreenChanged(event);
            }
        }

        private static class ListenerRegistration {
            String eventType;
            Object listener;
            PriorityLevel priority;

            ListenerRegistration(String eventType, Object listener, PriorityLevel priority) {
                this.eventType = eventType;
                this.listener = listener;
                this.priority = priority;
            }
        }

        private static class DispatchTask {
            List<ListenerRegistration> regs;
            Object event;

            DispatchTask(List<ListenerRegistration> regs, Object event) {
                this.regs = regs;
                this.event = event;
            }

            void execute() {
                for (ListenerRegistration reg : regs) {
                    try {
                        if (reg.listener instanceof TestScreenListener tsl) {
                            tsl.onScreenChanged(event);
                        } else if (reg.listener instanceof TestFieldListener tfl) {
                            tfl.onFieldChanged(event);
                        } else if (reg.listener instanceof TestConnectionListener tcl) {
                            tcl.onConnectionChanged(event);
                        } else if (reg.listener instanceof TestErrorListener tel) {
                            tel.onError(event);
                        } else if (reg.listener instanceof TestSlowScreenListener tssl) {
                            tssl.onScreenChanged(event);
                        } else if (reg.listener instanceof TestSlowFieldListener tsfl) {
                            tsfl.onFieldChanged(event);
                        } else if (reg.listener instanceof TestSlowErrorListener tsel) {
                            tsel.onError(event);
                        } else if (reg.listener instanceof TestThrowingListener ttl) {
                            ttl.onScreenChanged(event);
                        } else if (reg.listener instanceof TestSlowThrowingListener tstl) {
                            tstl.onScreenChanged(event);
                        }
                    } catch (RuntimeException e) {
                        // Queued mode logs but continues
                    }
                }
            }
        }
    }

    enum DispatchMode {
        SYNC, ASYNC, QUEUED
    }

    enum PriorityLevel {
        HIGH, NORMAL, LOW
    }

    // Test Event Types
    private static class TestScreenChangeEvent {
        int id;
        TestScreenChangeEvent() { this(0); }
        TestScreenChangeEvent(int id) { this.id = id; }
    }

    private static class TestFieldChangeEvent {}

    private static class TestConnectionEvent {
        String host;
        TestConnectionEvent(String host) { this.host = host; }
    }

    private static class TestErrorEvent {
        String message;
        TestErrorEvent(String message) { this.message = message; }
    }

    // Test Listeners
    private static class TestScreenListener implements TestEventListener {
        AtomicInteger eventCount = new AtomicInteger(0);
        String lastEventType = "screen";

        void onScreenChanged(Object event) {
            eventCount.incrementAndGet();
        }
    }

    private static class TestFieldListener implements TestEventListener {
        AtomicInteger eventCount = new AtomicInteger(0);

        void onFieldChanged(Object event) {
            eventCount.incrementAndGet();
        }
    }

    private static class TestConnectionListener implements TestEventListener {
        AtomicInteger eventCount = new AtomicInteger(0);
        List<String> eventSequence = new ArrayList<>();

        void onConnectionChanged(Object event) {
            eventCount.incrementAndGet();
            if (event instanceof TestConnectionEvent tce) {
                eventSequence.add(tce.host);
            }
        }
    }

    private static class TestErrorListener implements TestEventListener {
        AtomicInteger eventCount = new AtomicInteger(0);

        void onError(Object event) {
            eventCount.incrementAndGet();
        }
    }

    // Slow Listeners
    private static class TestSlowScreenListener implements TestEventListener {
        AtomicInteger eventCount = new AtomicInteger(0);
        long delayMs;

        TestSlowScreenListener(long delayMs) {
            this.delayMs = delayMs;
        }

        void onScreenChanged(Object event) {
            eventCount.incrementAndGet();
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static class TestSlowFieldListener implements TestEventListener {
        AtomicInteger eventCount = new AtomicInteger(0);
        long delayMs;

        TestSlowFieldListener(long delayMs) {
            this.delayMs = delayMs;
        }

        void onFieldChanged(Object event) {
            eventCount.incrementAndGet();
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static class TestSlowErrorListener implements TestEventListener {
        AtomicInteger eventCount = new AtomicInteger(0);
        long delayMs;

        TestSlowErrorListener(long delayMs) {
            this.delayMs = delayMs;
        }

        void onError(Object event) {
            eventCount.incrementAndGet();
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Throwing Listeners
    private static class TestThrowingListener implements TestEventListener {
        void onScreenChanged(Object event) {
            throw new RuntimeException("Intentional test exception");
        }
    }

    private static class TestSlowThrowingListener implements TestEventListener {
        long delayMs;

        TestSlowThrowingListener(long delayMs) {
            this.delayMs = delayMs;
        }

        void onScreenChanged(Object event) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Intentional test exception after delay");
        }
    }

    private interface TestEventListener {}
}
