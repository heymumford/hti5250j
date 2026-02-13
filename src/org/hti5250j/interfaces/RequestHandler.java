/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.interfaces;

/**
 * Abstraction for handling system requests (SYSREQ key / F3 in IBM i).
 * <p>
 * When user presses SYSREQ on IBM i terminal, the host pauses and displays
 * a system request menu. The client (HTI5250J) must respond with a menu choice.
 * <p>
 * This interface enables different strategies:
 * - GuiRequestHandler: Pop interactive dialog (GUI mode)
 * - NullRequestHandler: Return fixed response (headless mode)
 * - CustomRequestHandler: Robot Framework, workflow automation
 * <p>
 * Example Robot Framework handler (Jython):
 * <pre>
 * class RobotRequestHandler(RequestHandler):
 *     def handle_system_request(self, screen_content):
 *         # Extract expected menu option from workflow
 *         menu_option = self.workflow_context.get("sysreq_response")
 *         return menu_option  # Return "1" for first option, etc.
 * </pre>
 *
 */
public interface RequestHandler {

    /**
     * Handle a system request (SYSREQ key pressed).
     * <p>
     * The host has paused and is waiting for a menu selection.
     * The screen content shows the available options.
     *
     * @param screenContent current screen text (for context/decision making)
     * @return menu option (e.g., "1", "2", "9"), or null if no action needed
     */
    String handleSystemRequest(String screenContent);

}
