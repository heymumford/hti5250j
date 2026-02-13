/*
 * SPDX-FileCopyrightText: Copyright (c) 2001,2012
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: master_jaf
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.encoding;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.hti5250j.encoding.builtin.*;
import org.hti5250j.tools.logging.HTI5250jLogFactory;
import org.hti5250j.tools.logging.HTI5250jLogger;

/**
 * Methods for built-in code page support.
 */
/* package */ class BuiltInCodePageFactory {

    private static BuiltInCodePageFactory singleton;

    private final List<Class<?>> clazzes = new ArrayList<Class<?>>();
    private final HTI5250jLogger log = HTI5250jLogFactory.getLogger(this.getClass());

    private BuiltInCodePageFactory() {
        register();
    }

    public static synchronized final BuiltInCodePageFactory getInstance() {
        if (singleton == null) {
            singleton = new BuiltInCodePageFactory();
        }
        return singleton;
    }

    private void register() {
        clazzes.add(CCSID37.class);
        clazzes.add(CCSID273.class);
        clazzes.add(CCSID277.class);
        clazzes.add(CCSID278.class);
        clazzes.add(CCSID280.class);
        clazzes.add(CCSID284.class);
        clazzes.add(CCSID285.class);
        clazzes.add(CCSID297.class);
        clazzes.add(CCSID424.class);
        clazzes.add(CCSID500.class);
        clazzes.add(CCSID870.class);
        clazzes.add(CCSID871.class);
        clazzes.add(CCSID875.class);
        clazzes.add(CCSID930.class);
        clazzes.add(CCSID1025.class);
        clazzes.add(CCSID1026.class);
        clazzes.add(CCSID1112.class);
        clazzes.add(CCSID1140.class);
        clazzes.add(CCSID1141.class);
        clazzes.add(CCSID1147.class);
        clazzes.add(CCSID1148.class);
    }

    /**
     * @return unsorted list of available code pages
     */
    public String[] getAvailableCodePages() {
        HashSet<String> cpset = new HashSet<String>();
        for (Class<?> clazz : clazzes) {
            final ICodepageConverter converter = getConverterFromClassName(clazz);
            if (converter != null) {
                cpset.add(converter.getName());
            }
        }
        return cpset.toArray(new String[cpset.size()]);
    }

    /**
     * @param encoding
     * @return an {@link ICodePage} object OR null, of not found
     */
    public ICodePage getCodePage(String encoding) {
        // Phase 2: Try factory-based converter first (20 single-byte CCSIDs)
        CodepageConverterAdapter factoryConverter = CCSIDFactory.getConverter(encoding);
        if (factoryConverter != null) {
            return factoryConverter.init();
        }

        // Fall back to reflection-based loading for remaining converters (CCSID930, etc.)
        for (Class<?> clazz : clazzes) {
            final ICodepageConverter converter = getConverterFromClassName(clazz);
            if (converter != null && converter.getName().equals(encoding)) {
                return converter;
            }
        }
        return null;
    }

    /**
     * Lazy loading converters takes time,
     * but doesn't happen so often and saves memory.
     *
     * @param clazz {@link ICodepageConverter}
     * @return
     */
    private ICodepageConverter getConverterFromClassName(Class<?> clazz) {
        try {
            final Constructor<?> constructor = clazz.getConstructor(new Class[0]);
            final ICodepageConverter converter = (ICodepageConverter) constructor.newInstance();
            converter.init();
            return converter;
        } catch (Exception e) {
            log.error("Couldn't load code page converter class:" + clazz.getCanonicalName(), e);
            return null;
        }
    }

}
