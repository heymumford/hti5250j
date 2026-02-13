/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j;

import java.awt.Color;

/**
 * Color palette for the HTI5250J terminal emulator.
 *
 * Manages configurable terminal colors for foreground, background, and UI elements.
 */
public class ColorPalette {
    private Color colorBlack = new Color(0, 0, 0);
    private Color colorRed = new Color(255, 0, 0);
    private Color colorGreen = new Color(0, 255, 0);
    private Color colorYellow = new Color(255, 255, 0);
    private Color colorBlue = new Color(140, 120, 255);
    private Color colorTurquoise = new Color(0, 255, 255);
    private Color colorWhite = new Color(255, 255, 255);
    private Color colorPink = new Color(255, 192, 203);
    private Color colorBg = new Color(0, 0, 128);
    private Color guiFieldColor = new Color(0, 0, 128);
    private Color cursorColor = new Color(255, 255, 0);
    private Color separatorColor = new Color(128, 128, 128);
    private Color hexAttrColor = new Color(200, 200, 200);
    private Color background = new Color(0, 0, 128);
    private Color text = new Color(255, 255, 255);
    private boolean guiInterface = false;

    public Color getBlue() {
        return colorBlue;
    }
    public Color getRed() {
        return colorRed;
    }
    public Color getGreen() {
        return colorGreen;
    }
    public Color getYellow() {
        return colorYellow;
    }
    public Color getTurquoise() {
        return colorTurquoise;
    }
    public Color getWhite() {
        return colorWhite;
    }
    public Color getPink() {
        return colorPink;
    }
    public Color getBlack() {
        return colorBlack;
    }
    public Color getBg() {
        return colorBg;
    }
    public Color getGuiField() {
        return guiFieldColor;
    }
    public Color getCursor() {
        return cursorColor;
    }
    public Color getSeparator() {
        return separatorColor;
    }
    public Color getHexAttr() {
        return hexAttrColor;
    }
    public Color getBackground() {
        return background;
    }
    public Color getText() {
        return text;
    }

    public void setBlue(Color c) {
        this.colorBlue = c;
    }
    public void setRed(Color c) {
        this.colorRed = c;
    }
    public void setGreen(Color c) {
        this.colorGreen = c;
    }
    public void setYellow(Color c) {
        this.colorYellow = c;
    }
    public void setTurquoise(Color c) {
        this.colorTurquoise = c;
    }
    public void setWhite(Color c) {
        this.colorWhite = c;
    }
    public void setPink(Color c) {
        this.colorPink = c;
    }
    public void setBlack(Color c) {
        this.colorBlack = c;
    }
    public void setBg(Color c) {
        this.colorBg = c;
    }
    public void setGuiField(Color c) {
        this.guiFieldColor = c;
    }
    public void setCursor(Color c) {
        this.cursorColor = c;
    }
    public void setSeparator(Color c) {
        this.separatorColor = c;
    }
    public void setHexAttr(Color c) {
        this.hexAttrColor = c;
    }
    public void setBackground(Color c) {
        this.background = c;
    }
    public void setText(Color c) {
        this.text = c;
    }
    public void setGuiInterface(boolean value) {
        this.guiInterface = value;
    }
    public boolean isGuiInterface() {
        return guiInterface;
    }

    public Color getForegroundColor(char colorConstant) {
        switch (colorConstant) {
            case 0: return colorBlack;
            case 1: return colorRed;
            case 2: return colorGreen;
            case 3: return colorYellow;
            case 4: return colorBlue;
            case 5: return colorTurquoise;
            case 6: return colorWhite;
            case 7: return colorPink;
            default: return new Color(255, 165, 0); // orange for unknown
        }
    }

    public Color getBackgroundColor(char colorValue) {
        char bgColor = (char) ((colorValue >> 8) & 0xFF);
        return getForegroundColor(bgColor);
    }

    public Color getForegroundColor(int colorConstant) {
        return getForegroundColor((char) colorConstant);
    }
}
