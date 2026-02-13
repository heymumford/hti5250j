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
 * This is a Java 21 Record encapsulating configuration change events with
 * four components: source object, property name, old value, and new value.
 * Records are inherently immutable, providing thread-safety guarantees.
 *
 * @param source The bean that fired the event
 * @param propertyName The programmatic name of the property that was changed
 * @param oldValue The old value of the property (may be null)
 * @param newValue The new value of the property (may be null)
 */
public record SessionConfigEvent(Object source, String propertyName, Object oldValue, Object newValue) {

    private static final long serialVersionUID = 1L;

    public SessionConfigEvent {
    }

    /** @return the property name */
    public String getPropertyName() {
        return propertyName;
    }

    /** @return the old value */
    public Object getOldValue() {
        return oldValue;
    }

    /** @return the new value */
    public Object getNewValue() {
        return newValue;
    }

    /** @return the source object */
    public Object getSource() {
        return source;
    }
}
