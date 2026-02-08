/*
 * SPDX-FileCopyrightText: Copyright (c) 2002
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.event;

import java.beans.PropertyChangeEvent;

public class SessionConfigEvent extends PropertyChangeEvent {


    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new <code>SessionConfigChangeEvent</code>.
     *
     * @param source  The bean that fired the event.
     * @param propertyName  The programmatic name of the property
     *		that was changed.
     * @param oldValue  The old value of the property.
     * @param newValue  The new value of the property.
     */
    public SessionConfigEvent(Object source, String propertyName,
                              Object oldValue, Object newValue) {

        super(source, propertyName, oldValue, newValue);

    }

}
