/*
 * SPDX-FileCopyrightText: Copyright (c) 2001, 2002, 2003
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.tools.logging;

import java.util.*;

import org.hti5250j.interfaces.ConfigureFactory;

import static java.lang.Integer.parseInt;
import static org.hti5250j.interfaces.ConfigureFactory.SESSIONS;
import static org.hti5250j.tools.logging.HTI5250jLogger.INFO;

/**
 * An interface defining objects that can create Configure
 * instances.
 * <p>
 * The model for the HashMap implementation of loggers came from the POI project
 * thanks to Nicola Ken Barozzi (nicolaken at apache.org) for the reference.
 */
public final class HTI5250jLogFactory {

    // map of HTI5250jLogger instances, with classes as keys
    private static final Map<String, HTI5250jLogger> _loggers = new HashMap<String, HTI5250jLogger>();
    private static boolean log4j;
    private static String customLogger;
    private static int level = INFO;

    /*
     * Here we try to do a little more work up front.
     */
    static {
        try {
            initOrResetLogger();
        } catch (Exception ignore) {
            // ignore
        }
    }

    static void initOrResetLogger() {
        Properties props = ConfigureFactory.getInstance().getProperties(SESSIONS);
        level = parseInt(props.getProperty("emul.logLevel", Integer.toString(INFO)));

        customLogger = System.getProperty(HTI5250jLogFactory.class.getName());
        if (customLogger == null) {
            try {
                Class.forName("org.apache.log4j.Logger");
                log4j = true;
            } catch (Exception ignore) {
                // ignore
            }
        }
    }

    /**
     * Set package access only so we have to use getLogger() to return a logger object.
     */
    HTI5250jLogFactory() {

    }

    /**
     * @return An instance of the HTI5250jLogger.
     */
    public static HTI5250jLogger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * @return An instance of the HTI5250jLogger.
     */
    public static HTI5250jLogger getLogger(String clazzName) {
        HTI5250jLogger logger = null;

        if (_loggers.containsKey(clazzName)) {
            logger = _loggers.get(clazzName);
        } else {

            if (customLogger != null) {
                try {

                    Class<?> classObject = Class.forName(customLogger);
                    Object object = classObject.newInstance();
                    if (object instanceof HTI5250jLogger) {
                        logger = (HTI5250jLogger) object;
                    }
                } catch (Exception ex) {
                    // ignore
                }
            } else {
                if (log4j) {
                    logger = new Log4jLogger();
                } else {
                    // take the default logger.
                    logger = new ConsoleLogger();
                }
                logger.initialize(clazzName);
                logger.setLevel(level);
                _loggers.put(clazzName, logger);
            }
        }
        return logger;
    }

    public static boolean isLog4j() {
        return log4j;
    }

    public static void setLogLevels(int newLevel) {
        if (level != newLevel) {
            level = newLevel;
            Set<String> loggerSet = _loggers.keySet();
            Iterator<String> loggerIterator = loggerSet.iterator();
            while (loggerIterator.hasNext()) {
                HTI5250jLogger logger = _loggers.get(loggerIterator.next());
                logger.setLevel(newLevel);
            }
        }
    }

}
