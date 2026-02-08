/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.keyboard;

/**
 * Custom exception for keyboard remapping conflicts.
 */
public class KeyRemappingConflictException extends RuntimeException {
    public KeyRemappingConflictException(String message) {
        super(message);
    }
}
