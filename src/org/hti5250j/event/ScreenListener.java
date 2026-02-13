/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.event;

public interface ScreenListener {

    void onScreenChanged(int inUpdate, int startRow, int startCol,
                                int endRow, int endCol);

    void onScreenSizeChanged(int rows, int cols);

}
