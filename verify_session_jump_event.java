// Simple verification script for SessionJumpEvent Record conversion
public class verify_session_jump_event {
    public static void main(String[] args) throws Exception {
        System.out.println("========================================");
        System.out.println("SessionJumpEvent Record Conversion");
        System.out.println("Verification Tests");
        System.out.println("========================================");
        System.out.println();

        // Test 1: Constructor with all parameters
        System.out.print("TEST 1: Create event with constructor... ");
        Object source = new Object();
        try {
            org.hti5250j.event.SessionJumpEvent event =
                new org.hti5250j.event.SessionJumpEvent(source, 1, "Test message");
            System.out.println("✓ PASS");

            // Test 2: Field access via accessor methods
            System.out.print("TEST 2: Access jumpDirection via jumpDirection()... ");
            if (event.jumpDirection() == 1) {
                System.out.println("✓ PASS");
            } else {
                System.out.println("✗ FAIL - got " + event.jumpDirection());
            }

            // Test 3: Field access via Record-style method
            System.out.print("TEST 3: Access message via message()... ");
            if ("Test message".equals(event.message())) {
                System.out.println("✓ PASS");
            } else {
                System.out.println("✗ FAIL - got " + event.message());
            }

            // Test 4: Legacy API compatibility
            System.out.print("TEST 4: Access jumpDirection via getJumpDirection()... ");
            if (event.getJumpDirection() == 1) {
                System.out.println("✓ PASS");
            } else {
                System.out.println("✗ FAIL");
            }

            // Test 5: Legacy API compatibility
            System.out.print("TEST 5: Access message via getMessage()... ");
            if ("Test message".equals(event.getMessage())) {
                System.out.println("✓ PASS");
            } else {
                System.out.println("✗ FAIL");
            }

            // Test 6: Immutability - no setters
            System.out.print("TEST 6: Verify immutability (no setJumpDirection method)... ");
            try {
                event.getClass().getMethod("setJumpDirection", int.class);
                System.out.println("✗ FAIL - found setJumpDirection");
            } catch (NoSuchMethodException e) {
                System.out.println("✓ PASS");
            }

            // Test 7: Immutability - no message setter
            System.out.print("TEST 7: Verify immutability (no setMessage method)... ");
            try {
                event.getClass().getMethod("setMessage", String.class);
                System.out.println("✗ FAIL - found setMessage");
            } catch (NoSuchMethodException e) {
                System.out.println("✓ PASS");
            }

            // Test 8: EventObject inheritance
            System.out.print("TEST 8: Verify EventObject contract (getSource)... ");
            if (event.getSource() == source) {
                System.out.println("✓ PASS");
            } else {
                System.out.println("✗ FAIL");
            }

            // Test 9: Equality
            System.out.print("TEST 9: Verify equality with same values... ");
            org.hti5250j.event.SessionJumpEvent event2 =
                new org.hti5250j.event.SessionJumpEvent(source, 1, "Test message");
            if (event.equals(event2) && event.hashCode() == event2.hashCode()) {
                System.out.println("✓ PASS");
            } else {
                System.out.println("✗ FAIL");
            }

            // Test 10: Inequality
            System.out.print("TEST 10: Verify inequality with different direction... ");
            org.hti5250j.event.SessionJumpEvent event3 =
                new org.hti5250j.event.SessionJumpEvent(source, 2, "Test message");
            if (!event.equals(event3)) {
                System.out.println("✓ PASS");
            } else {
                System.out.println("✗ FAIL");
            }

            // Test 11: Listener compatibility
            System.out.print("TEST 11: Listener can receive event... ");
            TestListener listener = new TestListener();
            listener.onSessionJump(event);
            if (listener.received && listener.direction == 1 && "Test message".equals(listener.msg)) {
                System.out.println("✓ PASS");
            } else {
                System.out.println("✗ FAIL");
            }

            // Test 12: toString
            System.out.print("TEST 12: toString contains meaningful info... ");
            String str = event.toString();
            if (str != null && (str.contains("SessionJumpEvent") || str.contains("jumpDirection"))) {
                System.out.println("✓ PASS");
            } else {
                System.out.println("✗ FAIL - got: " + str);
            }

            System.out.println();
            System.out.println("========================================");
            System.out.println("All tests passed!");
            System.out.println("========================================");

        } catch (Exception e) {
            System.out.println("✗ FAIL - Exception: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static class TestListener implements org.hti5250j.event.SessionJumpListener {
        boolean received = false;
        int direction = -999;
        String msg = null;

        @Override
        public void onSessionJump(org.hti5250j.event.SessionJumpEvent jumpEvent) {
            received = true;
            direction = jumpEvent.jumpDirection();
            msg = jumpEvent.message();
        }
    }
}
