/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j;

import java.awt.*;
import java.awt.event.*;

public interface RubberBandCanvasIF {
    void addMouseListener(MouseListener mouseListener);

    void addMouseMotionListener(MouseMotionListener mouseMotionListener);

    void areaBounded(RubberBand rubberBand, int startX, int startY, int endX, int endY);

    boolean canDrawRubberBand(RubberBand band);

    Point translateStart(Point startPoint);

    Point translateEnd(Point endPoint);

    Color getBackground();

    Graphics getDrawingGraphics();

}
