/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.event;

import java.awt.Component;
import java.util.Objects;

/**
 * The event object for Wizard pages.
 *
 * Modernized for Java 21 with enhanced null-safety and improved documentation.
 * While WizardEvent cannot be a Record due to mutable fields required by
 * WizardListener implementations, it follows Record-like semantics:
 *
 * Immutable record-like fields (set in constructor, then read-only):
 * - source: The source of the event
 * - currentPage: The current wizard page
 * - isLastPage: Whether this is the last page in the sequence
 *
 * Mutable fields (can be modified by listeners to alter wizard flow):
 * - newPage: The page to navigate to (can be changed by setNewPage)
 * - allowChange: Whether navigation is allowed (can be changed by setAllowChange)
 *
 * DESIGN PATTERN: This is a "mutable event object" pattern used in Swing,
 * where event listeners can modify event state to affect event processing.
 * Examples: javax.swing.event.HyperlinkEvent, javax.swing.JComponent.repaint()
 *
 * @see WizardListener
 * @see java.util.EventObject
 */
public class WizardEvent extends java.util.EventObject {

    private static final long serialVersionUID = 1L;

    // Immutable record-like fields
    private final Component currentPage;
    private final boolean isLastPage;

    // Mutable fields (modifiable by listeners to alter wizard flow)
    private Component newPage;
    private boolean allowChange;

    /**
     * Constructs a WizardEvent with the specified parameters.
     *
     * Validates that source is non-null as required by EventObject.
     * Other parameters may be null to support various wizard navigation flows.
     *
     * @param source The source of the event (wizard component); must not be null
     * @param currentPage The wizard page currently displayed; may be null
     * @param newPage The wizard page to navigate to; may be null
     * @param isLastPage Whether the current page is the last page in the wizard
     * @param allowChange Whether the page change should be processed; true allows navigation
     *
     * @throws NullPointerException if source is null (EventObject requirement)
     */
    public WizardEvent(Object source, Component currentPage, Component newPage,
                       boolean isLastPage, boolean allowChange) {

        super(source); // Validates source is non-null
        this.currentPage = currentPage;
        this.newPage = newPage;
        this.isLastPage = isLastPage;
        this.allowChange = allowChange;
    }

    /**
     * Returns whether the current page is the last page in the wizard sequence.
     *
     * @return true if this is the last page; false otherwise
     */
    public boolean isLastPage() {
        return isLastPage;
    }

    /**
     * Returns whether the page change should be processed.
     *
     * Listeners can call setAllowChange(false) in nextBegin() or previousBegin()
     * to prevent navigation to the next or previous page. This allows listeners
     * to enforce validation or conditional page progression.
     *
     * @return true if the page change should be allowed; false to prevent it
     */
    public boolean getAllowChange() {
        return allowChange;
    }

    /**
     * Sets whether the page change should be processed.
     *
     * Typically called by WizardListener implementations during nextBegin() or
     * previousBegin() callbacks to approve or deny navigation. When called with
     * false, the wizard will not advance to the next or previous page.
     *
     * @param v true to allow the page change; false to prevent it
     */
    public void setAllowChange(boolean v) {
        allowChange = v;
    }

    /**
     * Returns the wizard page to navigate to.
     *
     * Listeners can call setNewPage(newComponent) in nextBegin() or previousBegin()
     * to redirect navigation to a different page than the default next or previous page.
     * This supports conditional page flows, skipping pages, or jumping to specific pages.
     *
     * @return the page to navigate to; may be null
     */
    public Component getNewPage() {
        return newPage;
    }

    /**
     * Sets the wizard page to navigate to.
     *
     * Allows WizardListener implementations to override the default next or previous
     * page navigation. Useful for implementing conditional page flows, page skipping,
     * or jumping to specific pages based on user input or validation results.
     *
     * @param p the page to navigate to; may be null
     */
    public void setNewPage(Component p) {
        newPage = p;
    }

    /**
     * Returns the wizard page currently being displayed.
     *
     * @return the current wizard page; may be null
     */
    public Component getCurrentPage() {
        return currentPage;
    }

    /**
     * Returns a string representation of this event.
     *
     * Generated automatically with field values for debugging purposes.
     *
     * @return a string representation of this WizardEvent
     */
    @Override
    public String toString() {
        return "WizardEvent{" +
                "source=" + source +
                ", currentPage=" + currentPage +
                ", newPage=" + newPage +
                ", isLastPage=" + isLastPage +
                ", allowChange=" + allowChange +
                '}';
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * Two WizardEvent objects are equal if they have the same source, currentPage,
     * newPage, isLastPage, and allowChange values.
     *
     * @param obj the reference object with which to compare
     * @return true if this object is the same as the obj argument; false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        WizardEvent other = (WizardEvent) obj;
        return isLastPage == other.isLastPage &&
               allowChange == other.allowChange &&
               Objects.equals(source, other.source) &&
               Objects.equals(currentPage, other.currentPage) &&
               Objects.equals(newPage, other.newPage);
    }

    /**
     * Returns a hash code value for this object.
     *
     * Computed from the same fields used in equals() to maintain the
     * equals-hashCode contract.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return Objects.hash(source, currentPage, newPage, isLastPage, allowChange);
    }

}
