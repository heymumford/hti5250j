/*
 * SPDX-FileCopyrightText: Copyright (c) 2001, 2002, 2003
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.interfaces;

import org.hti5250j.GlobalConfigure;

import java.util.Properties;

/**
 * An interface defining objects that can create Configure
 * instances.
 */
public abstract class ConfigureFactory {

    static final public String SESSIONS = "sessions";
    static final public String MACROS = "macros";
    static final public String KEYMAP = "keymap";
    private static ConfigureFactory factory;

    /**
     * @return An instance of the Configure.
     */
    public static ConfigureFactory getInstance() {
        ConfigureFactory.setFactory();
        return factory;
    }

    private static final void setFactory() {
        if (factory == null) {
            try {
                String className = System.getProperty(ConfigureFactory.class.getName());
                if (className != null) {
                    Class<?> classObject = Class.forName(className);
                    Object object = classObject.newInstance();
                    if (object instanceof ConfigureFactory) {
                        ConfigureFactory.factory = (ConfigureFactory) object;
                    }
                }
            } catch (Exception ex) {
                ;
            }
            if (ConfigureFactory.factory == null) { //take the default
//        ConfigureFactory.factory = new GlobalConfigureFactory();
                ConfigureFactory.factory = new GlobalConfigure();
            }
        }
    }

    abstract public void reloadSettings();

    abstract public void saveSettings();

    abstract public String getProperty(String regKey);

    abstract public String getProperty(String regKey, String defaultValue);

    abstract public void setProperties(String regKey, Properties regProps);

    abstract public void setProperties(String regKey, String fileName, String header);

    abstract public void setProperties(String regKey, String fileName, String header,
                                       boolean createFile);

    abstract public Properties getProperties(String regKey);

    abstract public Properties getProperties(String regKey, String fileName);

    abstract public Properties getProperties(String regKey, String fileName,
                                             boolean createFile, String header);

    abstract public Properties getProperties(String regKey, String fileName,
                                             boolean createFile, String header,
                                             boolean reloadIfLoaded);

    abstract public void saveSettings(String regKey);

    abstract public void saveSettings(String regKey, String header);

    abstract public void saveSettings(String regKey, String fileName, String header);

}
