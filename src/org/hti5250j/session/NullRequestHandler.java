/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.session;

import org.hti5250j.interfaces.RequestHandler;

/**
 * No-op RequestHandler for headless environments.
 * <p>
 * Returns a fixed response to SYSREQ without user interaction.
 * Suitable for:
 * - Docker containers (no display system)
 * - CI/CD pipelines (no user input)
 * - Automated workflows (deterministic behavior)
 * <p>
 * Default response is null (return to main menu).
 * Can be configured with a fixed option during construction.
 *
 * @since Phase 15B
 */
public class NullRequestHandler implements RequestHandler {

    private final String fixedResponse;

    /**
     * Create handler with null response (return to menu).
     */
    public NullRequestHandler() {
        this(null);
    }

    /**
     * Create handler with fixed response.
     *
     * @param fixedResponse menu option (e.g., "1"), or null to return to menu
     */
    public NullRequestHandler(String fixedResponse) {
        this.fixedResponse = fixedResponse;
    }

    @Override
    public String handleSystemRequest(String screenContent) {
        return fixedResponse;
    }

}
