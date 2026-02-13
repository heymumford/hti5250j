/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

import org.hti5250j.interfaces.UIDispatcherFactory;
import org.hti5250j.interfaces.IUIDispatcher;
import org.hti5250j.interfaces.IScheduler;
import org.hti5250j.headless.HeadlessUIDispatcher;
import org.hti5250j.headless.HeadlessScheduler;
import org.hti5250j.headless.HeadlessKeyEvent;
import org.hti5250j.keyboard.KeyCodes;
import org.hti5250j.keyboard.KeyStroker;

/**
 * Simple manual test runner for headless mode (no JUnit dependencies).
 * Run with: java -Djava.awt.headless=true -cp ... HeadlessTestRunner
 */
public class HeadlessTestRunner {

    public static void main(String[] args) throws Exception {
        System.out.println("=== HEADLESS PROTOCOL TESTS ===\n");

        // Set headless mode
        System.setProperty("java.awt.headless", "true");
        UIDispatcherFactory.setHeadlessMode(true);

        System.out.println("java.awt.headless = " + System.getProperty("java.awt.headless"));

        // Test 1: UI Dispatcher
        System.out.println("\n[Test 1] UI Dispatcher in headless mode...");
        IUIDispatcher dispatcher = new HeadlessUIDispatcher();
        final boolean[] taskExecuted = {false};
        dispatcher.invokeAndWait(() -> taskExecuted[0] = true);
        System.out.println("  Result: " + (taskExecuted[0] ? "PASS" : "FAIL"));

        // Test 2: Scheduler
        System.out.println("\n[Test 2] Scheduler in headless mode...");
        final int[] executionCount = {0};
        IScheduler scheduler = new HeadlessScheduler(() -> executionCount[0]++, 100);
        scheduler.start();
        Thread.sleep(350);
        scheduler.stop();
        System.out.println("  Executions: " + executionCount[0]);
        System.out.println("  Result: " + (executionCount[0] >= 2 ? "PASS" : "FAIL"));

        // Test 3: KeyStroker without AWT
        System.out.println("\n[Test 3] KeyStroker without AWT KeyEvent...");
        KeyStroker stroker = new KeyStroker(10, false, false, false, false,
                KeyCodes.KEY_LOCATION_STANDARD);
        System.out.println("  KeyCode: " + stroker.getKeyCode());
        System.out.println("  Shift: " + stroker.isShiftDown());
        System.out.println("  Location: " + stroker.getLocation());
        System.out.println("  Result: " + (stroker.getKeyCode() == 10 ? "PASS" : "FAIL"));

        // Test 4: HeadlessKeyEvent
        System.out.println("\n[Test 4] HeadlessKeyEvent...");
        HeadlessKeyEvent event = new HeadlessKeyEvent(10, true, false, true, false,
                KeyCodes.KEY_LOCATION_STANDARD, '\n');
        System.out.println("  KeyCode: " + event.getKeyCode());
        System.out.println("  Shift: " + event.isShiftDown());
        System.out.println("  Alt: " + event.isAltDown());
        System.out.println("  Char: " + (int) event.getKeyChar());
        boolean pass4 = event.getKeyCode() == 10 && event.isShiftDown() && event.isAltDown();
        System.out.println("  Result: " + (pass4 ? "PASS" : "FAIL"));

        // Test 5: Factory auto-detection
        System.out.println("\n[Test 5] Factory auto-detects headless mode...");
        IUIDispatcher factoryDispatcher = UIDispatcherFactory.getDefaultDispatcher();
        boolean isHeadless = factoryDispatcher instanceof HeadlessUIDispatcher;
        System.out.println("  Dispatcher class: " + factoryDispatcher.getClass().getSimpleName());
        System.out.println("  Result: " + (isHeadless ? "PASS" : "FAIL"));

        // Test 6: No Swing classes loaded
        System.out.println("\n[Test 6] Verify Swing NOT loaded...");
        boolean swingNotLoaded = !isClassLoaded("javax.swing.JComponent") &&
                                  !isClassLoaded("javax.swing.SwingUtilities");
        System.out.println("  JComponent loaded: " + isClassLoaded("javax.swing.JComponent"));
        System.out.println("  SwingUtilities loaded: " + isClassLoaded("javax.swing.SwingUtilities"));
        System.out.println("  Result: " + (swingNotLoaded ? "PASS" : "FAIL"));

        System.out.println("\n=== ALL TESTS COMPLETE ===");
    }

    private static boolean isClassLoaded(String className) {
        try {
            ClassLoader classLoader = HeadlessTestRunner.class.getClassLoader();
            java.lang.reflect.Method findLoadedClass = ClassLoader.class.getDeclaredMethod(
                    "findLoadedClass", String.class);
            findLoadedClass.setAccessible(true);
            return findLoadedClass.invoke(classLoader, className) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
