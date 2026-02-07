import java.lang.reflect.*;
import java.util.Collection;
import org.hti5250j.encoding.EBCDICPairwiseTest;

public class RunEBCDICTest {
    public static void main(String[] args) throws Exception {
        System.out.println("=== EBCDIC Pairwise Test Suite ===");
        
        // Get test parameters
        Method dataMethod = EBCDICPairwiseTest.class.getMethod("data");
        @SuppressWarnings("unchecked")
        Collection<Object[]> params = (Collection<Object[]>) dataMethod.invoke(null);
        
        int totalTests = 0;
        int passedTests = 0;
        
        for (Object[] param : params) {
            String codePage = (String) param[0];
            System.out.println("\nRunning tests for CodePage: " + codePage);
            
            EBCDICPairwiseTest test = new EBCDICPairwiseTest(codePage);
            test.setUp();
            
            // Count and invoke each test method
            for (Method method : EBCDICPairwiseTest.class.getDeclaredMethods()) {
                if (method.isAnnotationPresent(org.junit.Test.class)) {
                    totalTests++;
                    try {
                        method.invoke(test);
                        passedTests++;
                        System.out.println("  PASS: " + method.getName());
                    } catch (InvocationTargetException e) {
                        Throwable cause = e.getCause();
                        System.out.println("  FAIL: " + method.getName());
                        System.out.println("    Error: " + cause.getMessage());
                    }
                }
            }
        }
        
        System.out.println("\n=== Test Summary ===");
        System.out.println("Total: " + totalTests + ", Passed: " + passedTests + ", Failed: " + (totalTests - passedTests));
        System.exit((totalTests == passedTests) ? 0 : 1);
    }
}
