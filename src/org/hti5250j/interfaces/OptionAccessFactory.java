/*
 * SPDX-FileCopyrightText: Copyright (c) 2001, 2002, 2003
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.interfaces;


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

    private static void setFactory() {
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
            }
            if (OptionAccessFactory.factory == null) {
                OptionAccessFactory.factory = new org.hti5250j.OptionAccess();
            }
        }
    }

    public abstract boolean isValidOption(String option);

    public abstract boolean isRestrictedOption(String option);

    public abstract int getNumberOfRestrictedOptions();

    public abstract void reload();

}
