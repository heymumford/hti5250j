/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.workflow;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class KeyboardStateMachineVerifier {

    private AtomicBoolean keyboardLocked = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private ScheduledFuture<?> unlockTask;
    private Object lockObject = new Object();

    public void setKeyboardLockedPermanently() {
        keyboardLocked.set(true);
    }

    public void setKeyboardLocked(boolean locked) {
        synchronized (lockObject) {
            keyboardLocked.set(locked);
            lockObject.notifyAll();
        }
    }

    public void setKeyboardLockedWithDelay(long delayMillis) {
        synchronized (lockObject) {
            keyboardLocked.set(true);
            if (unlockTask != null) {
                unlockTask.cancel(false);
            }
            unlockTask = scheduler.schedule(() -> {
                synchronized (lockObject) {
                    keyboardLocked.set(false);
                    lockObject.notifyAll();
                }
            }, delayMillis, TimeUnit.MILLISECONDS);
        }
    }

    public void setKeyboardLockedWithTransition(long unlockDelayMs, long relockDelayMs) {
        synchronized (lockObject) {
            keyboardLocked.set(true);
            if (unlockTask != null) {
                unlockTask.cancel(false);
            }
            scheduler.schedule(() -> {
                synchronized (lockObject) {
                    keyboardLocked.set(false);
                    lockObject.notifyAll();
                }
            }, unlockDelayMs, TimeUnit.MILLISECONDS);

            scheduler.schedule(() -> {
                synchronized (lockObject) {
                    keyboardLocked.set(true);
                    lockObject.notifyAll();
                }
            }, relockDelayMs, TimeUnit.MILLISECONDS);
        }
    }

    public void setKeyboardFluttering(long cycleDelayMs) {
        synchronized (lockObject) {
            keyboardLocked.set(true);
            scheduler.scheduleAtFixedRate(() -> {
                synchronized (lockObject) {
                    keyboardLocked.set(!keyboardLocked.get());
                    lockObject.notifyAll();
                }
            }, cycleDelayMs, cycleDelayMs, TimeUnit.MILLISECONDS);
        }
    }

    public boolean acquireKeyboardForLogin(long timeoutMillis) throws TimeoutException {
        return acquireKeyboard(timeoutMillis);
    }

    public boolean acquireKeyboardForNavigate(long timeoutMillis) throws TimeoutException {
        return acquireKeyboard(timeoutMillis);
    }

    public boolean acquireKeyboardForFill(long timeoutMillis) throws TimeoutException {
        return acquireKeyboard(timeoutMillis);
    }

    public boolean acquireKeyboardForSubmit(long timeoutMillis) throws TimeoutException {
        return acquireKeyboard(timeoutMillis);
    }

    public boolean canSubmitWithKeyboardTimeout(long timeoutMillis) {
        try {
            return acquireKeyboard(timeoutMillis);
        } catch (TimeoutException e) {
            return false;
        }
    }

    public boolean acquireKeyboardForAssert(long timeoutMillis) throws TimeoutException {
        return acquireKeyboard(timeoutMillis);
    }

    private boolean acquireKeyboard(long timeoutMillis) throws TimeoutException {
        long endTime = System.currentTimeMillis() + timeoutMillis;

        synchronized (lockObject) {
            while (keyboardLocked.get()) {
                long remaining = endTime - System.currentTimeMillis();
                if (remaining <= 0) {
                    throw new TimeoutException("Keyboard lock timeout after " + timeoutMillis + "ms");
                }

                try {
                    lockObject.wait(Math.min(100, remaining));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new TimeoutException("Interrupted while waiting for keyboard");
                }
            }
            return true;
        }
    }

    public void releaseKeyboard() {
        synchronized (lockObject) {
            keyboardLocked.set(false);
            lockObject.notifyAll();
        }
    }

    public boolean acquireKeyboardForLoginWithInterruption(long timeoutMillis) throws InterruptedException {
        Thread currentThread = Thread.currentThread();
        scheduler.schedule(() -> currentThread.interrupt(), 100, TimeUnit.MILLISECONDS);

        try {
            return acquireKeyboard(timeoutMillis);
        } catch (TimeoutException e) {
            throw new InterruptedException(e.getMessage());
        }
    }

    public int concurrentKeyboardAcquisitions(int threadCount, long timeoutMillis) throws InterruptedException {
        setKeyboardLocked(false);
        AtomicInteger successCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    if (acquireKeyboard(timeoutMillis)) {
                        successCount.incrementAndGet();
                    }
                } catch (TimeoutException e) {
                    // Failed to acquire
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        return successCount.get();
    }

    public void releaseKeyboardWithoutAcquire() throws IllegalStateException {
        releaseKeyboard();
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
