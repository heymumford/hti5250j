/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.session;

import org.hti5250j.gui.SystemRequestDialog;
import org.hti5250j.interfaces.RequestHandler;

import java.awt.*;

/**
 * GUI RequestHandler using SystemRequestDialog.
 * <p>
 * Displays interactive dialog for SYSREQ responses in GUI mode.
 * This is the current behavior of Session5250.showSystemRequest().
 * <p>
 * Note: Requires display system and GUI component.
 * Do not use in headless environments (use NullRequestHandler instead).
 *
 * @since Phase 15B
 */
public class GuiRequestHandler implements RequestHandler {

    private final Component parentComponent;

    /**
     * Create handler with GUI parent component.
     *
     * @param parentComponent Component to parent SystemRequestDialog (e.g., SessionPanel)
     * @throws NullPointerException if parentComponent is null
     */
    public GuiRequestHandler(Component parentComponent) {
        if (parentComponent == null) {
            throw new NullPointerException("Parent component cannot be null");
        }
        this.parentComponent = parentComponent;
    }

    @Override
    public String handleSystemRequest(String screenContent) {
        SystemRequestDialog dialog = new SystemRequestDialog(parentComponent);
        return dialog.show();
    }

}
