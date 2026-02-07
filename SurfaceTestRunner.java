import java.lang.reflect.*;
import java.util.*;

/**
 * Simple test runner for JUnit 5 surface tests.
 * Discovers @Test methods and runs them, collecting results.
 */
public class SurfaceTestRunner {

    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;
    private static List<String> failures = new ArrayList<>();

    public static void main(String[] args) {
        String[] testClasses = {
            "org.hti5250j.surfaces.ProtocolRoundTripSurfaceTest",
            "org.hti5250j.surfaces.SchemaContractSurfaceTest",
            "org.hti5250j.surfaces.ConcurrencySurfaceTest"
        };

        System.out.println("=== Surface Test Suite Runner (JUnit 5) ===\n");

        for (String className : testClasses) {
            runTestClass(className);
        }

        printSummary();
    }

    private static void runTestClass(String className) {
        try {
            Class<?> testClass = Class.forName(className);
            System.out.println("Running " + testClass.getSimpleName() + "...");

            Method[] methods = testClass.getDeclaredMethods();
            for (Method method : methods) {
                if (isTestMethod(method)) {
                    runTestMethod(testClass, method);
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("ERROR: Could not find test class: " + className);
            testsFailed++;
        }
    }

    private static boolean isTestMethod(Method method) {
        // Check for @Test or @ParameterizedTest annotations
        try {
            Class<?> testAnnotation = Class.forName("org.junit.jupiter.api.Test");
            Class<?> paramAnnotation = Class.forName("org.junit.jupiter.params.ParameterizedTest");
            return method.isAnnotationPresent((Class<? extends java.lang.annotation.Annotation>) testAnnotation) ||
                   method.isAnnotationPresent((Class<? extends java.lang.annotation.Annotation>) paramAnnotation);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static void runTestMethod(Class<?> testClass, Method testMethod) {
        testsRun++;
        try {
            // Create instance
            Object instance = testClass.getDeclaredConstructor().newInstance();

            // Call @BeforeEach if it exists
            callBeforeEach(testClass, instance);

            // Run the test
            testMethod.invoke(instance);

            // Call @AfterEach if it exists
            callAfterEach(testClass, instance);

            testsPassed++;
            System.out.println("  ✓ " + testMethod.getName());

        } catch (Exception e) {
            testsFailed++;
            String error = testMethod.getName() + ": " + e.getCause();
            failures.add(error);
            System.out.println("  ✗ " + error);
        }
    }

    private static void callBeforeEach(Class<?> testClass, Object instance) throws Exception {
        Method[] methods = testClass.getDeclaredMethods();
        for (Method method : methods) {
            try {
                Class<?> beforeEach = Class.forName("org.junit.jupiter.api.BeforeEach");
                if (method.isAnnotationPresent((Class<? extends java.lang.annotation.Annotation>) beforeEach)) {
                    method.invoke(instance);
                }
            } catch (ClassNotFoundException e) {
                // Annotation not found, skip
            }
        }
    }

    private static void callAfterEach(Class<?> testClass, Object instance) throws Exception {
        Method[] methods = testClass.getDeclaredMethods();
        for (Method method : methods) {
            try {
                Class<?> afterEach = Class.forName("org.junit.jupiter.api.AfterEach");
                if (method.isAnnotationPresent((Class<? extends java.lang.annotation.Annotation>) afterEach)) {
                    method.invoke(instance);
                }
            } catch (ClassNotFoundException e) {
                // Annotation not found, skip
            }
        }
    }

    private static void printSummary() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("Test Results:");
        System.out.println("  Tests run: " + testsRun);
        System.out.println("  Passed: " + testsPassed);
        System.out.println("  Failed: " + testsFailed);

        if (!failures.isEmpty()) {
            System.out.println("\nFailures:");
            for (String failure : failures) {
                System.out.println("  - " + failure);
            }
        }

        System.exit(testsFailed > 0 ? 1 : 0);
    }
}
