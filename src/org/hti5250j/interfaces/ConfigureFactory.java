/*
 * SPDX-FileCopyrightText: Copyright (c) 2001, 2002, 2003
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.interfaces;

import org.hti5250j.GlobalConfigure;
import org.hti5250j.tools.logging.HTI5250jLogFactory;
import org.hti5250j.tools.logging.HTI5250jLogger;

import java.util.Properties;

/**
 * An interface defining objects that can create Configure
 * instances.
 */
public abstract class ConfigureFactory {

    public static final String SESSIONS = "sessions";
    public static final String MACROS = "macros";
    public static final String KEYMAP = "keymap";
    private static final HTI5250jLogger log = HTI5250jLogFactory.getLogger(ConfigureFactory.class);
    private static ConfigureFactory factory;

    /**
     * @return An instance of the Configure.
     */
    public static ConfigureFactory getInstance() {
        ConfigureFactory.setFactory();
        return factory;
    }

    private static void setFactory() {
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
                log.warn("Failed to load custom ConfigureFactory", ex);
            }
            if (ConfigureFactory.factory == null) {
                ConfigureFactory.factory = new GlobalConfigure();
            }
        }
    }

    public abstract void reloadSettings();

    public abstract void saveSettings();

    public abstract String getProperty(String regKey);

    public abstract String getProperty(String regKey, String defaultValue);

    public abstract void setProperties(String regKey, Properties regProps);

    public abstract void setProperties(String regKey, String fileName, String header);

    public abstract void setProperties(String regKey, String fileName, String header,
                                       boolean createFile);

    public abstract Properties getProperties(String regKey);

    public abstract Properties getProperties(String regKey, String fileName);

    public abstract Properties getProperties(String regKey, String fileName,
                                             boolean createFile, String header);

    public abstract Properties getProperties(String regKey, String fileName,
                                             boolean createFile, String header,
                                             boolean reloadIfLoaded);

    public abstract void saveSettings(String regKey);

    public abstract void saveSettings(String regKey, String header);

    public abstract void saveSettings(String regKey, String fileName, String header);

}
