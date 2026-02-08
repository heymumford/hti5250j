/*
 * SPDX-FileCopyrightText: Copyright (c) 2001,2009
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: master_jaf
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.framework.tn5250;


/**
 * Simplified rectangle class. Very much similar like java.awt.Rectangle,
 * but we want to decouple the packages ...
 *
 * Immutable record (Java 16+) ensures thread-safe coordinate storage.
 */
public record Rect(int x, int y, int width, int height) {
}
