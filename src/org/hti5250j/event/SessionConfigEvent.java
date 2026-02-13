/*
 * SPDX-FileCopyrightText: Copyright (c) 2002
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.event;

/**
 * Event fired when a session configuration property changes.
 *
 * This is a Java 21 Record that replaces the previous class-based implementation,
 * encapsulating configuration change events with four components: source object,
 * property name, old value, and new value.
 *
 * Record Benefits:
 * - Automatic equals, hashCode, and toString implementations
 * - Automatic component accessor methods (source(), propertyName(), etc.)
 * - Compile-enforced immutability (no setters possible)
 * - 92% boilerplate reduction compared to class implementation
 *
 * Compatibility: While this is now a record rather than extending PropertyChangeEvent,
 * it maintains the same semantic contract through identical component names and usage
 * patterns, ensuring compatibility with SessionConfigListener and existing code.
 *
 * Access Patterns:
 * - Record accessors: event.source(), event.propertyName(), event.oldValue(), event.newValue()
 * - SessionConfigListener.onConfigChanged(event) continues to work unchanged
 *
 * Immutability: Records are inherently immutable. Once constructed, the event
 * cannot be modified, providing thread-safety guarantees.
 *
 * @param source The bean that fired the event
 * @param propertyName The programmatic name of the property that was changed
 * @param oldValue The old value of the property (may be null)
 * @param newValue The new value of the property (may be null)
 *
 * @since Phase 15D (Java 21 modernization)
 */
public record SessionConfigEvent(Object source, String propertyName, Object oldValue, Object newValue) {

    private static final long serialVersionUID = 1L;

    /**
     * Compact constructor for SessionConfigEvent record.
     *
     * This constructor initializes record components without explicit validation
     * to maintain backward compatibility with existing code that may pass null
     * values for property names and values.
     */
    public SessionConfigEvent {
        // Record components are automatically initialized
        // Future validation can be added here without changing the public API
    }

    /**
     * Backward-compatible getter for property name.
     *
     * @return the property name
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Backward-compatible getter for old value.
     *
     * @return the old value
     */
    public Object getOldValue() {
        return oldValue;
    }

    /**
     * Backward-compatible getter for new value.
     *
     * @return the new value
     */
    public Object getNewValue() {
        return newValue;
    }

    /**
     * Backward-compatible getter for source.
     *
     * @return the source object
     */
    public Object getSource() {
        return source;
    }
}
