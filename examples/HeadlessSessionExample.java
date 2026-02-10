/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.examples;

import org.hti5250j.Session5250;
import org.hti5250j.SessionConfig;
import org.hti5250j.interfaces.HeadlessSession;
import org.hti5250j.interfaces.RequestHandler;
import org.hti5250j.session.NullRequestHandler;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Properties;
import javax.imageio.ImageIO;

/**
 * Headless Session Example — Pure Programmatic Automation (Phase 15B)
 * <p>
 * Demonstrates how to use HTI5250J as a programmatic automation tool:
 * <p>
 * 1. Create a headless session (no GUI required)
 * 2. Connect to IBM i system
 * 3. Navigate and interact using pure data APIs
 * 4. Capture screenshots (PNG) without persistent GUI components
 * 5. Handle system requests programmatically
 * <p>
 * Key Insight: Screen rendering (PNG) is generated on-demand via
 * HeadlessScreenRenderer. No SessionPanel or GuiGraphicBuffer needed.
 * <p>
 * This is the recommended pattern for:
 * - REST APIs (data-only responses)
 * - Robot Framework integration
 * - CI/CD automation pipelines
 * - Batch processing (1000+ concurrent sessions)
 * - Docker/container deployments (no X11 required)
 *
 * @since Phase 15B
 */
public class HeadlessSessionExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== HTI5250J Headless Session Example ===\n");

        // STEP 1: Configure session properties
        Properties props = createSessionProperties();

        // STEP 2: Create session configuration
        String configResource = "session.properties";
        String sessionName = "example-session";
        SessionConfig config = new SessionConfig(configResource, sessionName);

        // STEP 3: Create Session5250 (headless by default)
        Session5250 session = new Session5250(props, configResource, sessionName, config);

        try {
            // STEP 4: Connect to IBM i system
            System.out.println("Connecting to IBM i system...");
            session.connect();
            Thread.sleep(2000);  // Wait for connection to establish

            // STEP 5: Access via HeadlessSession interface
            // This is the recommended API for new code
            HeadlessSession headless = session.asHeadlessSession();

            // STEP 6: Verify connection
            if (!headless.isConnected()) {
                System.err.println("Failed to connect to IBM i system");
                System.exit(1);
            }
            System.out.println("✓ Connected to IBM i\n");

            // STEP 7: Take initial screenshot
            System.out.println("Capturing initial screen...");
            BufferedImage screenshot = headless.captureScreenshot();
            File outFile = new File("artifacts/initial_screen.png");
            outFile.getParentFile().mkdirs();
            ImageIO.write(screenshot, "PNG", outFile);
            System.out.println("✓ Screenshot saved: " + outFile.getAbsolutePath() + "\n");

            // STEP 8: Display screen as text (accessibility)
            String screenText = headless.getScreenAsText();
            System.out.println("Screen content (first 500 chars):");
            System.out.println(screenText.substring(0, Math.min(500, screenText.length())));
            System.out.println("\n");

            // STEP 9: Simulate user interaction
            System.out.println("Sending keys: CALL MYPGM [enter]");
            headless.sendKeys("CALL MYPGM");
            headless.sendKeys("[enter]");
            Thread.sleep(1000);

            // STEP 10: Wait for screen to refresh
            System.out.println("Waiting for keyboard unlock...");
            headless.waitForKeyboardUnlock(5000);
            System.out.println("✓ Screen refreshed\n");

            // STEP 11: Capture screen after navigation
            System.out.println("Capturing post-navigation screen...");
            screenshot = headless.captureScreenshot();
            outFile = new File("artifacts/after_navigation.png");
            ImageIO.write(screenshot, "PNG", outFile);
            System.out.println("✓ Screenshot saved: " + outFile.getAbsolutePath() + "\n");

            // STEP 12: Custom RequestHandler example
            // This demonstrates how to intercept SYSREQ (F3) handling
            System.out.println("Setting custom RequestHandler...");
            RequestHandler customHandler = new ExampleRequestHandler();
            session.setRequestHandler(customHandler);
            System.out.println("✓ Custom SYSREQ handler installed\n");

            System.out.println("=== Example Complete ===");
            System.out.println("Screenshots saved to artifacts/ directory");

        } finally {
            // STEP 13: Clean shutdown
            System.out.println("\nDisconnecting...");
            session.disconnect();
            System.out.println("✓ Disconnected");
        }
    }

    /**
     * Create session properties for headless connection.
     * <p>
     * Key settings:
     * - host: IBM i system hostname/IP
     * - port: Telnet port (default 23)
     * - screen-size: 24x80 or 27x132
     * - code-page: EBCDIC character encoding
     */
    private static Properties createSessionProperties() {
        Properties props = new Properties();

        // Connection settings (modify for your environment)
        props.setProperty("host", "ibm-i.example.com");
        props.setProperty("port", "23");

        // Screen settings
        props.setProperty("screen-size", "24x80");
        props.setProperty("code-page", "37");  // EBCDIC Code Page 37 (US)

        // Optional: SSL/TLS
        // props.setProperty("ssl-type", "TLS");

        return props;
    }

    /**
     * Custom RequestHandler example.
     * <p>
     * When user presses F3 (SYSREQ) on IBM i screen, this handler
     * is invoked with the current screen content. The handler can
     * decide how to respond (return to menu, select option, etc.).
     * <p>
     * This is the extension point for Robot Framework, Jython adapters,
     * and workflow automation logic.
     */
    private static class ExampleRequestHandler implements RequestHandler {

        @Override
        public String handleSystemRequest(String screenContent) {
            System.out.println("[SYSREQ Handler] F3 pressed, current screen:");
            System.out.println("  " + screenContent.substring(0, Math.min(100, screenContent.length()))
                              + "...");

            // Example: Automatically return to menu
            // In production, this could be delegated to Robot Framework,
            // Jython workflow engine, or custom application logic
            System.out.println("[SYSREQ Handler] Returning to menu");
            return null;  // null means "return to menu"
        }
    }

    /**
     * Summary of HeadlessSession API.
     * <p>
     * This is the recommended interface for new code. It provides:
     * <p>
     * Connection Management:
     * - connect() / disconnect()
     * - isConnected()
     * <p>
     * Screen Interaction:
     * - sendKeys(String) — Send keystrokes to screen
     * - waitForKeyboardUnlock(int) — Wait for server response
     * - waitForKeyboardLockCycle(int) — Wait for complete interaction cycle
     * <p>
     * Screen Capture:
     * - captureScreenshot() — Generate BufferedImage (PNG-ready)
     * - getScreenAsText() — Get screen as plain text
     * - getScreen() — Access raw Screen5250 data model
     * <p>
     * Metadata:
     * - getSessionName() — Session identifier
     * - getConfiguration() — SessionConfig settings
     * - getConnectionProperties() — Properties used for connection
     * <p>
     * Events:
     * - addSessionListener(SessionListener) — Listen for state changes
     * - removeSessionListener(SessionListener)
     * <p>
     * System Requests:
     * - handleSystemRequest() — Custom F3 (SYSREQ) handling
     * <p>
     * Note: HeadlessSession is backed by Session5250, so they operate
     * on the same underlying session. Use whichever API is most
     * convenient for your use case.
     */
}
