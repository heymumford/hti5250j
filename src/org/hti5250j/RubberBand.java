/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.SwingUtilities;

public abstract class RubberBand {
    private volatile RubberBandCanvasIF canvas;
    protected volatile Point startPoint;
    protected volatile Point endPoint;
    private volatile boolean eraseSomething = false;
    private volatile boolean isSomethingBounded = false;
    private volatile boolean isDragging = false;

    private class MouseHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent mouseEvent) {
            if (!SwingUtilities.isRightMouseButton(mouseEvent)) {
                if (!isSomethingBounded) {
                    start(canvas.translateStart(mouseEvent.getPoint()));
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent mouseEvent) {
            isDragging = false;
        }

    }

    private class MouseMotionHandler extends MouseMotionAdapter {

        @Override
        public void mouseDragged(MouseEvent mouseEvent) {

            if (!SwingUtilities.isRightMouseButton(mouseEvent) && getCanvas().canDrawRubberBand(RubberBand.this)) {
                erase();
                if (!isDragging) {
                    reset();
                    start(canvas.translateStart(mouseEvent.getPoint()));
                }
                isDragging = true;
                stop(canvas.translateEnd(mouseEvent.getPoint()));
                notifyRubberBandCanvas();
                draw();
                notifyRubberBandCanvas();
            }
        }

    }

    public boolean isDragging() {
        return isDragging;
    }

    public RubberBand(RubberBandCanvasIF canvas) {
        super();
        setCanvas(canvas);
        getCanvas().addMouseListener(new MouseHandler());
        getCanvas().addMouseMotionListener(new MouseMotionHandler());
    }

    protected void draw() {
        Graphics graphics = getCanvas().getDrawingGraphics();

        if (graphics != null) {
            try {
                if (getCanvas().canDrawRubberBand(this)) {
                    graphics.setXORMode(canvas.getBackground());
                    drawRubberBand(graphics);
                    // we have drawn something, set the flag to indicate this
                    setEraseSomething(true);
                }
            } finally {
                graphics.dispose();
            }
        }
    }

    protected abstract void drawBoundingShape(
        Graphics graphics,
        int startX,
        int startY,
        int width,
        int height
    );

    protected void drawRubberBand(Graphics graphics) {

        if ((getEndPoint().x > getStartPoint().x) && (getEndPoint().y > getStartPoint().y)) {
            drawBoundingShape(graphics, getStartPoint().x, getStartPoint().y, getEndPoint().x - getStartPoint().x, getEndPoint().y - getStartPoint().y);
        } else if ((getEndPoint().x < getStartPoint().x) && (getEndPoint().y < getStartPoint().y)) {
            drawBoundingShape(graphics, getEndPoint().x, getEndPoint().y, getStartPoint().x - getEndPoint().x, getStartPoint().y - getEndPoint().y);
        } else if ((getEndPoint().x > getStartPoint().x) && (getEndPoint().y < getStartPoint().y)) {
            drawBoundingShape(graphics, getStartPoint().x, getEndPoint().y, getEndPoint().x - getStartPoint().x, getStartPoint().y - getEndPoint().y);
        } else if ((getEndPoint().x < getStartPoint().x) && (getEndPoint().y > getStartPoint().y)) {
            drawBoundingShape(graphics, getEndPoint().x, getStartPoint().y, getStartPoint().x - getEndPoint().x, getEndPoint().y - getStartPoint().y);
        }
        isSomethingBounded = true;

    }

    protected void erase() {

        if (getEraseSomething()) {
            draw();
            setEraseSomething(false);
        }

    }

    public final RubberBandCanvasIF getCanvas() {
        return this.canvas;
    }

    protected Point getEndPoint() {
        if (this.endPoint == null) {
            setEndPoint(new Point(0, 0));
        }
        return this.endPoint;
    }

    protected Point getStartPoint() {

        if (this.startPoint == null) {
            setStartPoint(new Point(0, 0));
        }
        return this.startPoint;

    }

    protected final boolean getEraseSomething() {
        return this.eraseSomething;
    }

    protected void notifyRubberBandCanvas() {

        int startX, startY, endX, endY;

        if (getStartPoint().x < getEndPoint().x) {
            startX = getStartPoint().x;
            endX = getEndPoint().x;
        } else {
            startX = getEndPoint().x;
            endX = getStartPoint().x;
        }
        if (getStartPoint().y < getEndPoint().y) {
            startY = getStartPoint().y;
            endY = getEndPoint().y;
        } else {
            startY = getEndPoint().y;
            endY = getStartPoint().y;
        }

        getCanvas().areaBounded(this, startX, startY, endX, endY);

    }

    public final void setCanvas(RubberBandCanvasIF canvas) {
        this.canvas = canvas;
    }

    protected final void setEndPoint(Point newValue) {
        this.endPoint = newValue;
    }

    protected final void setEraseSomething(boolean newValue) {
        this.eraseSomething = newValue;
    }

    protected final void setStartPoint(Point newValue) {
        this.startPoint = newValue;
        if (startPoint == null) {
            endPoint = null;
        }

    }

    protected void start(Point point) {
        setEndPoint(point);
        setStartPoint(point);
    }

    protected void stop(Point point) {

        if (point.x < 0) {
            point.x = 0;
        }

        if (point.y < 0) {
            point.y = 0;
        }

        setEndPoint(point);
    }

    protected void reset() {
        setStartPoint(null);
        setEndPoint(null);
        isSomethingBounded = false;

    }

    protected final boolean isAreaSelected() {
        return isSomethingBounded;
    }

}
