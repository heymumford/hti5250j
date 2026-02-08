/*
 * SPDX-FileCopyrightText: Copyright (c) 2001,202,2003
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.tools.encoder;

import java.awt.Component;
import java.io.OutputStream;
import java.io.IOException;

/**
 * Interface that defines an encoder
 */
public interface Encoder {
    /**
     * Encode the specified component on the specified stream
     */
    public void encode(Component component, OutputStream stream) throws IOException, EncoderException;

}
