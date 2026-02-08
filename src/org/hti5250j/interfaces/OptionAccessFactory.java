/*
 * SPDX-FileCopyrightText: Copyright (c) 2001, 2002, 2003
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.interfaces;

import java.util.Vector;

/**
 * An interface defining objects that can create OptionAccess
 * instances.
 */
public abstract class OptionAccessFactory {

    private static OptionAccessFactory factory;

    /**
     * @return An instance of the OptionAccess.
     */
    public static OptionAccessFactory getInstance() {
        OptionAccessFactory.setFactory();
        return factory;
    }

    private static final void setFactory() {
        if (factory == null) {
            try {
                String className = System.getProperty(OptionAccessFactory.class.getName());
                if (className != null) {
                    Class<?> classObject = Class.forName(className);
                    Object object = classObject.newInstance();
                    if (object instanceof OptionAccessFactory) {
                        OptionAccessFactory.factory = (OptionAccessFactory) object;
                    }
                }
            } catch (Exception ex) {
                ;
            }
            if (OptionAccessFactory.factory == null) { //take the default
                OptionAccessFactory.factory = new org.hti5250j.OptionAccess();
            }
        }
    }

    abstract public boolean isValidOption(String option);

    abstract public boolean isRestrictedOption(String option);

    abstract public int getNumberOfRestrictedOptions();

    abstract public void reload();

}
