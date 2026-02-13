/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package headless;

import org.hti5250j.interfaces.UIDispatcherFactory;
import org.hti5250j.interfaces.IUIDispatcher;
import org.hti5250j.interfaces.IScheduler;
import org.hti5250j.headless.HeadlessUIDispatcher;
import org.hti5250j.headless.HeadlessScheduler;
import org.hti5250j.headless.HeadlessKeyEvent;
import org.hti5250j.keyboard.KeyCodes;
import org.hti5250j.keyboard.KeyStroker;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for headless protocol operation.
 * Tests MUST pass without requiring X11 or GUI environment.
 */
@DisplayName("Headless Protocol Tests")
public class HeadlessProtocolTest {

    @BeforeAll
    static void setHeadlessMode() {
        // Force headless mode for all tests
        System.setProperty("java.awt.headless", "true");
        UIDispatcherFactory.setHeadlessMode(true);
    }

    @Test
    @DisplayName("TDD RED: Headless mode should not load Swing classes")
    void testHeadlessModeNoSwing() {
        // This test verifies Swing classes are NOT loaded
        assertFalse(isClassLoaded("javax.swing.JComponent"),
                "JComponent should not be loaded in headless mode");
        assertFalse(isClassLoaded("javax.swing.SwingUtilities"),
                "SwingUtilities should not be loaded in headless mode");
    }

    @Test
    @DisplayName("TDD GREEN: UI Dispatcher works in headless mode")
    void testHeadlessUIDispatcher() throws Exception {
        IUIDispatcher dispatcher = new HeadlessUIDispatcher();

        final boolean[] taskExecuted = {false};

        dispatcher.invokeAndWait(() -> {
            taskExecuted[0] = true;
        });

        assertTrue(taskExecuted[0], "Task should execute in headless mode");
    }

    @Test
    @DisplayName("TDD GREEN: Scheduler works in headless mode")
    void testHeadlessScheduler() throws InterruptedException {
        final int[] executionCount = {0};

        Runnable task = () -> executionCount[0]++;

        IScheduler scheduler = new HeadlessScheduler(task, 100);
        scheduler.start();

        Thread.sleep(350); // Allow ~3 executions

        scheduler.stop();

        assertTrue(executionCount[0] >= 2,
                "Scheduler should execute task multiple times, got: " + executionCount[0]);
    }

    @Test
    @DisplayName("TDD GREEN: KeyStroker works without KeyEvent")
    void testKeyStrokerWithoutAWT() {
        // Create KeyStroker without java.awt.event.KeyEvent
        KeyStroker stroker = new KeyStroker(
                10, // Enter key
                false, // no shift
                false, // no control
                false, // no alt
                false, // no altGr
                KeyCodes.KEY_LOCATION_STANDARD
        );

        assertEquals(10, stroker.getKeyCode());
        assertFalse(stroker.isShiftDown());
        assertEquals(KeyCodes.KEY_LOCATION_STANDARD, stroker.getLocation());
    }

    @Test
    @DisplayName("TDD GREEN: HeadlessKeyEvent provides IKeyEvent implementation")
    void testHeadlessKeyEvent() {
        HeadlessKeyEvent event = new HeadlessKeyEvent(
                10, // keyCode
                true, // shift
                false, // control
                true, // alt
                false, // altGr
                KeyCodes.KEY_LOCATION_STANDARD,
                '\n' // character
        );

        assertEquals(10, event.getKeyCode());
        assertTrue(event.isShiftDown());
        assertFalse(event.isControlDown());
        assertTrue(event.isAltDown());
        assertEquals('\n', event.getKeyChar());

        assertFalse(event.isConsumed());
        event.consume();
        assertTrue(event.isConsumed());
    }

    @Test
    @DisplayName("TDD REFACTOR: Factory auto-detects headless mode")
    void testFactoryAutoDetectsHeadless() {
        // Factory should return HeadlessUIDispatcher when java.awt.headless=true
        IUIDispatcher dispatcher = UIDispatcherFactory.getDefaultDispatcher();

        assertNotNull(dispatcher);
        assertTrue(dispatcher instanceof HeadlessUIDispatcher,
                "Factory should create HeadlessUIDispatcher in headless mode");
    }

    /**
     * Helper to check if a class has been loaded.
     * @param className fully qualified class name
     * @return true if class is loaded
     */
    private boolean isClassLoaded(String className) {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            java.lang.reflect.Method findLoadedClass = ClassLoader.class.getDeclaredMethod(
                    "findLoadedClass", String.class);
            findLoadedClass.setAccessible(true);
            return findLoadedClass.invoke(classLoader, className) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
