/*
 * SPDX-FileCopyrightText: SaschaS93 Copyright (c) 2001,2018
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: SaschaS93
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.tools.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author SaschaS93
 */
@ResourceLock("HTI5250jLogFactory")
public class HTI5250jLogFactoryTest {

    private static final String LOGGER_MOCK_CLASS_NAME = "org.hti5250j.tools.logging.CustomHTI5250jLoggerMock";

    @BeforeEach
    public void setUp() throws Exception {
        System.setProperty(HTI5250jLogFactory.class.getName(), LOGGER_MOCK_CLASS_NAME);
        HTI5250jLogFactory.initOrResetLogger();
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty(HTI5250jLogFactory.class.getName());
        HTI5250jLogFactory.initOrResetLogger();
    }

    @Test
    public void testCustomLogger() {
        HTI5250jLogger logger = HTI5250jLogFactory.getLogger(this.getClass());

        String msg = "The loaded logger must be an instance of " + LOGGER_MOCK_CLASS_NAME;
        System.out.println(logger.getClass());
        assertTrue(logger instanceof CustomHTI5250jLoggerMock,msg);
    }

}
