/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.event;

import java.awt.Component;

/**
 * The event object for Wizard pages.
 */
public class WizardEvent extends java.util.EventObject {

    private static final long serialVersionUID = 1L;
    protected Component currentPage;
    protected Component newPage;
    protected boolean isLastPage;
    protected boolean allowChange;

    public WizardEvent(Object source, Component current_page, Component new_page,
                       boolean is_last_page, boolean allow_change) {

        super(source);
        this.currentPage = current_page;
        this.newPage = new_page;
        this.isLastPage = is_last_page;
        this.allowChange = allow_change;
    }

    /**
     * Returns whether the page is the last page.
     */
    public boolean isLastPage() {
        return isLastPage;
    }

    /**
     * Returns whether the event should be allowed to finish processing.
     */
    public boolean getAllowChange() {
        return allowChange;
    }

    /**
     * Sets whether the event should be allowed to finish processing.
     */
    public void setAllowChange(boolean v) {
        allowChange = v;
    }

    /**
     * Returns the next page.
     */
    public Component getNewPage() {
        return newPage;
    }

    /**
     * Sets the next page.
     */
    public void setNewPage(Component p) {
        newPage = p;
    }

    /**
     * Returns the current page on which the <code>JCWizardEvent</code> occured.
     */
    public Component getCurrentPage() {
        return currentPage;
    }

}
