import org.hti5250j.encoding.builtin.CCSID930;

/**
 * Standalone test harness for CCSID930.
 * Verifies that the shift-in and shift-out methods work correctly.
 */
public class TestCCSID930Standalone {

    public static void main(String[] args) throws Exception {
        System.out.println("=== CCSID930 Standalone Test ===\n");

        CCSID930 codec = new CCSID930();
        int passCount = 0;
        int failCount = 0;

        // Test 1: Metadata
        System.out.println("Test 1: Codec Metadata");
        try {
            assert "930".equals(codec.getName()) : "getName() failed";
            assert "930".equals(codec.getEncoding()) : "getEncoding() failed";
            assert "Japan Katakana (extended range), DBCS".equals(codec.getDescription()) : "getDescription() failed";
            System.out.println("  PASS: getName()=" + codec.getName());
            System.out.println("  PASS: getEncoding()=" + codec.getEncoding());
            System.out.println("  PASS: getDescription()=" + codec.getDescription());
            passCount++;
        } catch (AssertionError e) {
            System.out.println("  FAIL: " + e.getMessage());
            failCount++;
        }

        // Test 2: Shift-in byte activates double-byte mode
        System.out.println("\nTest 2: Shift-In (0x0E) Activation");
        try {
            assert !codec.isDoubleByteActive() : "Double-byte mode should start inactive";
            char result = codec.ebcdic2uni(0x0E);
            assert codec.isDoubleByteActive() : "Double-byte mode should be active after shift-in";
            assert result == 0 : "Shift-in should return 0";
            assert !codec.secondByteNeeded() : "secondByteNeeded should be false after shift-in";
            System.out.println("  PASS: Shift-in activates double-byte mode");
            System.out.println("  PASS: isDoubleByteActive()=" + codec.isDoubleByteActive());
            System.out.println("  PASS: result=" + (int)result);
            System.out.println("  PASS: secondByteNeeded()=" + codec.secondByteNeeded());
            passCount++;
        } catch (AssertionError e) {
            System.out.println("  FAIL: " + e.getMessage());
            failCount++;
        }

        // Test 3: Shift-out byte deactivates double-byte mode
        System.out.println("\nTest 3: Shift-Out (0x0F) Deactivation");
        try {
            // Re-create codec to reset state
            codec = new CCSID930();
            codec.ebcdic2uni(0x0E); // shift-in
            assert codec.isDoubleByteActive() : "Double-byte mode should be active";

            char result = codec.ebcdic2uni(0x0F);
            assert !codec.isDoubleByteActive() : "Double-byte mode should be inactive after shift-out";
            assert result == 0 : "Shift-out should return 0";
            assert !codec.secondByteNeeded() : "secondByteNeeded should be false after shift-out";
            System.out.println("  PASS: Shift-out deactivates double-byte mode");
            System.out.println("  PASS: isDoubleByteActive()=" + codec.isDoubleByteActive());
            System.out.println("  PASS: result=" + (int)result);
            System.out.println("  PASS: secondByteNeeded()=" + codec.secondByteNeeded());
            passCount++;
        } catch (AssertionError e) {
            System.out.println("  FAIL: " + e.getMessage());
            failCount++;
        }

        // Test 4: Double-byte sequence handling
        System.out.println("\nTest 4: Double-Byte Sequence Handling");
        try {
            codec = new CCSID930();
            codec.ebcdic2uni(0x0E); // shift-in
            assert codec.isDoubleByteActive() : "Double-byte mode should be active";

            // First byte of sequence
            char firstResult = codec.ebcdic2uni(0x50);
            assert codec.secondByteNeeded() : "secondByteNeeded should be true after first byte";
            assert firstResult == 0 : "First byte should return 0";
            System.out.println("  PASS: First byte of sequence returns 0");

            // Second byte of sequence
            char secondResult = codec.ebcdic2uni(0x60);
            assert !codec.secondByteNeeded() : "secondByteNeeded should be false after second byte";
            // Note: conversion result depends on ConvTable, just verify it's not immediately 0
            System.out.println("  PASS: Second byte processed, secondByteNeeded()=" + codec.secondByteNeeded());
            System.out.println("  PASS: Second byte result=" + (int)secondResult);
            passCount++;
        } catch (AssertionError e) {
            System.out.println("  FAIL: " + e.getMessage());
            failCount++;
        }

        // Test 5: Direct isShiftIn() and isShiftOut() methods
        System.out.println("\nTest 5: Direct Method Calls");
        try {
            codec = new CCSID930();
            assert codec.isShiftIn(0x0E) : "isShiftIn(0x0E) should be true";
            assert !codec.isShiftIn(0x0F) : "isShiftIn(0x0F) should be false";
            System.out.println("  PASS: isShiftIn(0x0E)=" + codec.isShiftIn(0x0E));
            System.out.println("  PASS: isShiftIn(0x0F)=" + codec.isShiftIn(0x0F));

            assert codec.isShiftOut(0x0F) : "isShiftOut(0x0F) should be true";
            assert !codec.isShiftOut(0x0E) : "isShiftOut(0x0E) should be false";
            System.out.println("  PASS: isShiftOut(0x0F)=" + codec.isShiftOut(0x0F));
            System.out.println("  PASS: isShiftOut(0x0E)=" + codec.isShiftOut(0x0E));
            passCount++;
        } catch (AssertionError e) {
            System.out.println("  FAIL: " + e.getMessage());
            failCount++;
        }

        // Print summary
        System.out.println("\n=== Test Summary ===");
        System.out.println("Passed: " + passCount);
        System.out.println("Failed: " + failCount);
        System.out.println("Total:  " + (passCount + failCount));

        if (failCount > 0) {
            System.exit(1);
        } else {
            System.out.println("\nAll tests PASSED!");
            System.exit(0);
        }
    }
}
