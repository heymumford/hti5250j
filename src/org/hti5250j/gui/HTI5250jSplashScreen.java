/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.gui;


import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Window;
import java.awt.Cursor;
import javax.swing.ImageIcon;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.MediaTracker;
import java.awt.Canvas;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.Color;

import org.hti5250j.tools.GUIGraphicsUtils;

/**
 * Uses an Icon or the location of an image to create an application's introductory screen.
 */
public class HTI5250jSplashScreen extends Canvas {

    private static final long serialVersionUID = 1L;
    protected Window dialog = null;
    protected Frame f = null;
    protected Image image;
    private Image offScreenBuffer;
    private Graphics offScreenBufferGraphics;
    private int steps;
    private int progress;
    private Object lock = new Object();

    /**
     * Creates a splash screen given the location of the image.
     * The image location is the package path of the image and must be in
     * the classpath. For example, if an image was located in
     * /test/examples/image.gif, and the classpath specified contains /test,
     * the constructor should be passed "/examples/image.gif".
     */
    public HTI5250jSplashScreen(String image_location) {

        initialize(GUIGraphicsUtils.createImageIcon(image_location));

    }

    /**
     * Creates a splash screen given an Icon image.
     */
    public HTI5250jSplashScreen(ImageIcon image) {
        initialize(image);
    }

    /**
     * Creates the Splash screen window and configures it.
     */
    protected void initialize(ImageIcon iimage) {

        image = iimage.getImage();
        if (image == null) {
            throw new IllegalArgumentException("Image specified is invalid.");
        }

        MediaTracker tracker = new MediaTracker(this);
        tracker.addImage(image, 0);

        try {
            tracker.waitForAll();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        f = new Frame();
        dialog = new Window(f);
        dialog.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        Dimension s = new Dimension(image.getWidth(this) + 2,
                image.getHeight(this) + 2);
        setSize(s);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(s);

        dialog.add(this, BorderLayout.CENTER);
        dialog.pack();

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screen.width - s.width) / 2;
        if (x < 0) {
            x = 0;
        }

        int y = (screen.height - s.height) / 2;
        if (y < 0) {
            y = 0;
        }

        dialog.setLocation(x, y);
        dialog.validate();

    }

    public void setSteps(int step) {
        steps = step;
    }

    public synchronized void updateProgress(int prog) {

        if (dialog == null || f == null) {
            return;
        }

        progress = prog;
        repaint();

        try {
            wait();
        } catch (InterruptedException ie) {
            System.out.println(" updateProgress " + ie.getMessage());
        }
    }

    public void update(Graphics g) {
        paint(g);
    }

    public synchronized void paint(Graphics g) {

        int inset = 5;
        int height = 14;

        Dimension size = getSize();
        if (offScreenBuffer == null) {
            offScreenBuffer = createImage(size.width, size.height);
            offScreenBufferGraphics = offScreenBuffer.getGraphics();
        }

        offScreenBufferGraphics.drawImage(image, 1, 1, this);

        offScreenBufferGraphics.setColor(new Color(204, 204, 255));
        offScreenBufferGraphics.draw3DRect(0, 0, size.width - 1, size.height - 1, true);

        offScreenBufferGraphics.setColor(new Color(204, 204, 255).darker());
        offScreenBufferGraphics.fill3DRect(inset - 1,
                image.getHeight(this) - (height + 2),
                image.getWidth(this) - (inset * 2),
                height + 1,
                false);

        offScreenBufferGraphics.setColor(new Color(204, 204, 255));
        offScreenBufferGraphics.fillRect(inset,
                image.getHeight(this) - (height + 1),
                ((image.getWidth(this) - (inset * 2)) / steps) * progress,
                height);

        g.drawImage(offScreenBuffer, 0, 0, this);

        notify();
    }

    /**
     * This method will show or hide the splash screen.  Once the splash
     * screen is hidden, the splash screen window will be disposed. This means
     * the splash screen cannot become visible again.
     */
    public void setVisible(boolean show) {
        if (show && dialog != null && f != null && !dialog.isVisible()) {
            dialog.setVisible(true);
        } else {

            if (dialog != null) {
                updateProgress(steps + 1);
                dialog.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                dialog.dispose();
            }
            if (f != null) {
                f.dispose();
            }
            dialog = null;
            f = null;
        }
    }

}
