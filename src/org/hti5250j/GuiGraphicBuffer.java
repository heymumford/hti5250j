/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.LineMetrics;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.SwingUtilities;

import org.hti5250j.event.ScreenListener;
import org.hti5250j.event.ScreenOIAListener;
import org.hti5250j.event.SessionConfigEvent;
import org.hti5250j.event.SessionConfigListener;
import org.hti5250j.framework.tn5250.Screen5250;
import org.hti5250j.framework.tn5250.ScreenOIA;
import org.hti5250j.gui.DrawingContext;
import org.hti5250j.sessionsettings.ColumnSeparator;
import org.hti5250j.tools.GUIGraphicsUtils;
import org.hti5250j.tools.logging.HTI5250jLogFactory;
import org.hti5250j.tools.logging.HTI5250jLogger;

public class GuiGraphicBuffer implements ScreenOIAListener,
        ScreenListener,
        PropertyChangeListener,
        SessionConfigListener,
        ActionListener {

    // Dup Character array for display output
    private static final transient char[] dupChar = {'*'};

    private BufferedImage bi;
    private final Object lock = new Object();
    private Line2D separatorLine = new Line2D.Float();
    private Rectangle2D tArea; // text area
    private Rectangle2D aArea; // all screen area
    private Rectangle2D cArea; // command line area
    private Rectangle2D sArea; // status area
    private Rectangle2D pArea; // position area (cursor etc..)
    private Rectangle2D mArea; // message area
    private Rectangle2D iArea; // insert indicator
    private Rectangle2D kbArea; // keybuffer indicator
    private Rectangle2D scriptArea; // script indicator
    private static final String xSystem = "X - System";
    private static final String xError = "X - II";
    private int crossRow;
    private Rectangle crossRect = new Rectangle();
    private int offTop = 0;   // offset from top
    private int offLeft = 0;  // offset from left
    private boolean antialiased = true;
    private Screen5250 screen;
    private Data updateRect;
    protected int columnWidth;
    protected int rowHeight;
    private SessionPanel gui;

    private LineMetrics lm;
    /*default*/ Font font;
    private int lenScreen;
    private boolean showHex;
    private ColorPalette colorPalette;
    private CharacterMetrics characterMetrics;
    private CursorManager cursorManager;
    private final DrawingContext drawingContext = new DrawingContext();
    private ScreenRenderer screenRenderer;
    /*default*/ Color colorBlack = new Color(0, 0, 0);
    /*default*/ Color colorRed = new Color(255, 0, 0);
    /*default*/ Color colorGreen = new Color(0, 255, 0);
    /*default*/ Color colorYellow = new Color(255, 255, 0);
    /*default*/ Color colorBlue = new Color(140, 120, 255);
    /*default*/ Color colorTurq = new Color(0, 255, 255);
    /*default*/ Color colorWhite = new Color(255, 255, 255);
    /*default*/ Color colorPink = new Color(255, 192, 203);
    /*default*/ Color colorBg = new Color(0, 0, 128);
    /*default*/ Color colorCursor = new Color(255, 255, 0);
    /*default*/ Color colorGuiField = new Color(0, 0, 128);
    /*default*/ Color colorGUIField = new Color(0, 0, 128);
    /*default*/ Color colorSeparator = new Color(128, 128, 128);
    /*default*/ Color colorSep = new Color(128, 128, 128);
    /*default*/ Color colorHexAttr = new Color(200, 200, 200);
    protected int crossHair = 0;
    private boolean updateFont;
    protected boolean hotSpots = false;
    private float sfh = 1.2f; // font scale height
    private float sfw = 1.0f; // font scale height
    private float ps132 = 0; // Font point size
    private boolean cfg_guiInterface = false;
    private boolean cfg_guiShowUnderline = true;
    private boolean rulerFixed;
    private ColumnSeparator colSepLine;
    private final StringBuffer hsMore = new StringBuffer("More...");
    private final StringBuffer hsBottom = new StringBuffer("Bottom");
    private Rectangle workR = new Rectangle();

    private boolean colSep = false;
    private boolean underLine = false;
    private boolean nonDisplay = false;
    private Color fg;
    private Color bg;

    private SessionConfig config;

    protected Rectangle clipper;

    private final HTI5250jLogger log = HTI5250jLogFactory.getLogger("GFX");

    public GuiGraphicBuffer(Screen5250 screen, SessionPanel gui, SessionConfig config) {

        this.screen = screen;
        this.config = config;
        this.gui = gui;

        config.addSessionConfigListener(this);

        this.colorPalette = new ColorPalette();
        this.characterMetrics = new CharacterMetrics();
        this.cursorManager = new CursorManager();
        this.screenRenderer = new ScreenRenderer(colorPalette, characterMetrics, cursorManager, drawingContext);

        loadProps();

        String fontName = null;
        if (config.isPropertyExists("font")) {
            fontName = getStringProperty("font");
            if (!GUIGraphicsUtils.isFontNameExists(fontName)) {
                fontName = null;
            }
        }

        if (fontName == null) {
            font = new Font(GUIGraphicsUtils.getDefaultFont(), Font.PLAIN, 14);
            config.setProperty("font", font.getFontName());
        } else {
            font = new Font(fontName, Font.PLAIN, 14);
        }

        gui.setFont(font);

        getSettings();
        characterMetrics.setFont(font);
        lm = characterMetrics.getLineMetrics();
        columnWidth = characterMetrics.getCharWidth();
        rowHeight = characterMetrics.getCharHeight();

        screen.getOIA().addOIAListener(this);
        screen.addScreenListener(this);
        tArea = new Rectangle2D.Float();
        cArea = new Rectangle2D.Float();
        aArea = new Rectangle2D.Float();
        sArea = new Rectangle2D.Float();
        pArea = new Rectangle2D.Float();
        mArea = new Rectangle2D.Float();
        iArea = new Rectangle2D.Float();
        kbArea = new Rectangle2D.Float();
        scriptArea = new Rectangle2D.Float();

    }

    /**
     * This is for blinking cursor but should be moved out
     */
    public void actionPerformed(ActionEvent actionevent) {
        if (actionevent.getSource() instanceof javax.swing.Timer) {
            if (screen.isCursorActive()) {
                screen.setCursorActive(false);
            } else {
                screen.setCursorActive(true);
            }
        }
    }

    public boolean isBlinkCursor() {
        return cursorManager.isBlinkEnabled();

    }

    public void resize(int width, int height) {

        if (bi.getWidth() != width || bi.getHeight() != height) {
            bi = null;
            bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            getSettings();
        }

    }

    private void getSettings() {

        lenScreen = screen.getScreenLength();

    }

    protected final void loadColors() {
        colorPalette.setGuiInterface(cfg_guiInterface);

        if (!config.isPropertyExists("colorBg")) {
            setProperty("colorBg", Integer.toString(colorPalette.getBackground().getRGB()));
        } else {
            colorPalette.setBackground(getColorProperty("colorBg"));
        }

        gui.setBackground(colorPalette.getBackground());

        if (!config.isPropertyExists("colorBlue")) {
            setProperty("colorBlue", Integer.toString(colorPalette.getBlue().getRGB()));
        } else {
            colorPalette.setBlue(getColorProperty("colorBlue"));
        }

        if (!config.isPropertyExists("colorTurq")) {
            setProperty("colorTurq", Integer.toString(colorPalette.getTurquoise().getRGB()));
        } else {
            colorPalette.setTurquoise(getColorProperty("colorTurq"));
        }

        if (!config.isPropertyExists("colorRed")) {
            setProperty("colorRed", Integer.toString(colorPalette.getRed().getRGB()));
        } else {
            colorPalette.setRed(getColorProperty("colorRed"));
        }

        if (!config.isPropertyExists("colorWhite")) {
            setProperty("colorWhite", Integer.toString(colorPalette.getWhite().getRGB()));
        } else {
            colorPalette.setWhite(getColorProperty("colorWhite"));
        }

        if (!config.isPropertyExists("colorYellow")) {
            setProperty("colorYellow", Integer.toString(colorPalette.getYellow().getRGB()));
        } else {
            colorPalette.setYellow(getColorProperty("colorYellow"));
        }

        if (!config.isPropertyExists("colorGreen")) {
            setProperty("colorGreen", Integer.toString(colorPalette.getGreen().getRGB()));
        } else {
            colorPalette.setGreen(getColorProperty("colorGreen"));
        }

        if (!config.isPropertyExists("colorPink")) {
            setProperty("colorPink", Integer.toString(colorPalette.getPink().getRGB()));
        } else {
            colorPalette.setPink(getColorProperty("colorPink"));
        }

        if (!config.isPropertyExists("colorGUIField")) {
            setProperty("colorGUIField", Integer.toString(colorPalette.getGuiField().getRGB()));
        } else {
            colorPalette.setGuiField(getColorProperty("colorGUIField"));
        }

        if (!config.isPropertyExists("colorCursor")) {
            setProperty("colorCursor", Integer.toString(colorPalette.getCursor().getRGB()));
        } else {
            colorPalette.setCursor(getColorProperty("colorCursor"));
        }

        if (!config.isPropertyExists("colorSep")) {
            setProperty("colorSep", Integer.toString(colorPalette.getSeparator().getRGB()));
        } else {
            colorPalette.setSeparator(getColorProperty("colorSep"));
        }

        if (!config.isPropertyExists("colorHexAttr")) {
            setProperty("colorHexAttr", Integer.toString(colorPalette.getHexAttr().getRGB()));
        } else {
            colorPalette.setHexAttr(getColorProperty("colorHexAttr"));
        }
    }

    public void loadProps() {

        loadColors();

        colSepLine = ColumnSeparator.getFromName(getStringProperty("colSeparator"));

        if (config.isPropertyExists("showAttr")) {
            if (getStringProperty("showAttr").equals("Hex")) {
                showHex = true;
            }
        }

        if (config.isPropertyExists("guiInterface")) {
            if (getStringProperty("guiInterface").equals("Yes")) {
                screen.setUseGUIInterface(true);
                cfg_guiInterface = true;
            } else {
                screen.setUseGUIInterface(false);
                cfg_guiInterface = false;
            }
        }

        if (config.isPropertyExists("guiShowUnderline")) {
            if (getStringProperty("guiShowUnderline").equals("Yes")) {
                cfg_guiShowUnderline = true;
            } else {
                cfg_guiShowUnderline = false;
            }
        }

        if (config.isPropertyExists("hotspots")) {
            if (getStringProperty("hotspots").equals("Yes")) {
                hotSpots = true;
            } else {
                hotSpots = false;
            }
        }

        if (config.isPropertyExists("hsMore")) {
            if (getStringProperty("hsMore").length() > 0) {
                hsMore.setLength(0);
                hsMore.append(getStringProperty("hsMore"));
            }
        }

        if (config.isPropertyExists("hsBottom")) {
            if (getStringProperty("hsBottom").length() > 0) {
                hsBottom.setLength(0);
                hsBottom.append(getStringProperty("hsBottom"));
            }
        }

        if (config.isPropertyExists("cursorSize")) {
            if (getStringProperty("cursorSize").equals("Full")) {
                cursorManager.setCursorSize(2);
            }
            if (getStringProperty("cursorSize").equals("Half")) {
                cursorManager.setCursorSize(1);
            }
            if (getStringProperty("cursorSize").equals("Line")) {
                cursorManager.setCursorSize(0);
            }

        }

        if (config.isPropertyExists("crossHair")) {
            if (getStringProperty("crossHair").equals("None")) {
                crossHair = 0;
            }
            if (getStringProperty("crossHair").equals("Horz")) {
                crossHair = 1;
            }
            if (getStringProperty("crossHair").equals("Vert")) {
                crossHair = 2;
            }
            if (getStringProperty("crossHair").equals("Both")) {
                crossHair = 3;
            }

        }

        if (config.isPropertyExists("rulerFixed")) {

            if (getStringProperty("rulerFixed").equals("Yes")) {
                rulerFixed = true;
            } else {
                rulerFixed = false;
            }

        }

        if (config.isPropertyExists("fontScaleHeight")) {
            sfh = getFloatProperty("fontScaleHeight");
        }

        if (config.isPropertyExists("fontScaleWidth")) {
            sfw = getFloatProperty("fontScaleWidth");
        }

        if (config.isPropertyExists("fontPointSize")) {
            ps132 = getFloatProperty("fontPointSize");
        }

        if (config.isPropertyExists("cursorBottOffset")) {
            cursorManager.setCursorBottOffset(getIntProperty("cursorBottOffset"));
        }

        if (config.isPropertyExists("resetRequired")) {
            if (getStringProperty("resetRequired").equals("Yes")) {
                screen.setResetRequired(true);
            } else {
                screen.setResetRequired(false);
            }
        }

        if (config.isPropertyExists("useAntialias")) {

            if (getStringProperty("useAntialias").equals("Yes")) {
                antialiased = true;
            } else {
                antialiased = false;
            }

        }

        if (config.getStringProperty("cursorBlink").equals("Yes")) {
            javax.swing.Timer blinker = new javax.swing.Timer(500, this);
            cursorManager.setBlinker(blinker);
            blinker.start();
        }

        if (config.isPropertyExists("backspaceError")) {
            if (getStringProperty("backspaceError").equals("Yes")) {
                screen.setBackspaceError(true);
            } else {
                screen.setBackspaceError(false);
            }
        }
    }

    protected final String getStringProperty(String prop) {

        return config.getStringProperty(prop);

    }

    protected final Color getColorProperty(String prop) {

        return config.getColorProperty(prop);

    }

    protected final float getFloatProperty(String prop) {

        return config.getFloatProperty(prop);

    }

    protected final int getIntProperty(String prop) {

        return config.getIntegerProperty(prop);

    }

    protected final void setProperty(String key, String val) {

        config.setProperty(key, val);

    }

    /**
     * Update the configuration settings
     * @param pce
     */
    public void onConfigChanged(SessionConfigEvent pce) {
        // Handle SessionConfigEvent directly - it has PropertyChangeEvent-compatible API
        handlePropertyChange(pce.getPropertyName(), pce.getNewValue());
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        // Handle standard PropertyChangeEvent
        handlePropertyChange(event.getPropertyName(), event.getNewValue());
    }

    /**
     * Common property change handling logic for both PropertyChangeEvent and SessionConfigEvent.
     * Extracted to avoid code duplication.
     *
     * @param pn the property name
     * @param newValue the new value
     */
    private void handlePropertyChange(String pn, Object newValue) {
        boolean resetAttr = false;

        if (pn.equals("colorBg")) {
            colorPalette.setBackground((Color) newValue);
            resetAttr = true;
        }

        if (pn.equals("colorBlue")) {
            colorPalette.setBlue((Color) newValue);
            resetAttr = true;
        }

        if (pn.equals("colorTurq")) {
            colorPalette.setTurquoise((Color) newValue);
            resetAttr = true;
        }

        if (pn.equals("colorRed")) {
            colorPalette.setRed((Color) newValue);
            resetAttr = true;
        }

        if (pn.equals("colorWhite")) {
            colorPalette.setWhite((Color) newValue);
            resetAttr = true;
        }

        if (pn.equals("colorYellow")) {
            colorPalette.setYellow((Color) newValue);
            resetAttr = true;
        }

        if (pn.equals("colorGreen")) {
            colorPalette.setGreen((Color) newValue);
            resetAttr = true;
        }

        if (pn.equals("colorPink")) {
            colorPalette.setPink((Color) newValue);
            resetAttr = true;
        }

        if (pn.equals("colorGUIField")) {
            colorPalette.setGuiField((Color) newValue);
            resetAttr = true;
        }

        if (pn.equals("colorCursor")) {
            colorPalette.setCursor((Color) newValue);
            resetAttr = true;
        }

        if (pn.equals("colorSep")) {
            colorPalette.setSeparator((Color) newValue);
            resetAttr = true;
        }

        if (pn.equals("colorHexAttr")) {
            colorPalette.setHexAttr((Color) newValue);
            resetAttr = true;
        }

        if (pn.equals("cursorSize")) {
            if (newValue.equals("Full")) {
                cursorManager.setCursorSize(2);
            }
            if (newValue.equals("Half")) {
                cursorManager.setCursorSize(1);
            }
            if (newValue.equals("Line")) {
                cursorManager.setCursorSize(0);
            }

        }

        if (pn.equals("crossHair")) {
            if (newValue.equals("None")) {
                crossHair = 0;
            }
            if (newValue.equals("Horz")) {
                crossHair = 1;
            }
            if (newValue.equals("Vert")) {
                crossHair = 2;
            }
            if (newValue.equals("Both")) {
                crossHair = 3;
            }
        }

        if (pn.equals("rulerFixed")) {
            if (newValue.equals("Yes")) {
                rulerFixed = true;
            } else {
                rulerFixed = false;
            }
        }

        colSepLine = ColumnSeparator.getFromName(newValue.toString());

        if (pn.equals("showAttr")) {
            if (newValue.equals("Hex")) {
                showHex = true;
            } else {
                showHex = false;
            }
        }

        if (pn.equals("guiInterface")) {
            if (newValue.equals("Yes")) {
                screen.setUseGUIInterface(true);
                cfg_guiInterface = true;
            } else {
                screen.setUseGUIInterface(true);
                cfg_guiInterface = false;
            }
        }

        if (pn.equals("guiShowUnderline")) {
            if (newValue.equals("Yes")) {
                cfg_guiShowUnderline = true;
            } else {
                cfg_guiShowUnderline = false;
            }
        }

        if (pn.equals("hotspots")) {
            if (newValue.equals("Yes")) {
                hotSpots = true;
            } else {
                hotSpots = false;
            }
        }

        if (pn.equals("resetRequired")) {
            if (newValue.equals("Yes")) {
                screen.setResetRequired(true);
            } else {
                screen.setResetRequired(false);
            }
        }

        if (pn.equals("hsMore")) {
            hsMore.setLength(0);
            hsMore.append((String) newValue);

        }

        if (pn.equals("hsBottom")) {
            hsBottom.setLength(0);
            hsBottom.append((String) newValue);

        }

        if (pn.equals("font")) {
            font = new Font((String) newValue, Font.PLAIN, 14);
            characterMetrics.setFont(font);
            updateFont = true;
        }

        if (pn.equals("useAntialias")) {
            if (newValue.equals("Yes")) {
                setUseAntialias(true);
            } else {
                setUseAntialias(false);
            }
            updateFont = true;
        }

        if (pn.equals("fontScaleHeight")) {
            sfh = Float.parseFloat((String) newValue);
            updateFont = true;
        }

        if (pn.equals("fontScaleWidth")) {
            sfw = Float.parseFloat((String) newValue);
            updateFont = true;
        }

        if (pn.equals("fontPointSize")) {
            ps132 = Float.parseFloat((String) newValue);
            updateFont = true;
        }

        if (pn.equals("cursorBottOffset")) {
            cursorManager.setCursorBottOffset(getIntProperty("cursorBottOffset"));
        }

        if (pn.equals("cursorBlink")) {

            log.debug(getStringProperty("cursorBlink"));
            if (newValue.equals("Yes")) {

                if (cursorManager.getBlinker() == null) {

                    javax.swing.Timer blinker = new javax.swing.Timer(500, this);
                    cursorManager.setBlinker(blinker);
                    blinker.start();
                }
            } else {

                javax.swing.Timer blinker = cursorManager.getBlinker();
                if (blinker != null) {
                    blinker.stop();
                    cursorManager.setBlinker(null);
                }
            }
        }

        if (pn.equals("backspaceError")) {
            if (newValue.equals("Yes")) {
                screen.setBackspaceError(true);
            } else {
                screen.setBackspaceError(false);
            }
        }

        if (updateFont) {
            Rectangle drawingBounds = gui.getDrawingBounds();
            resizeScreenArea(drawingBounds.width, drawingBounds.height);
            updateFont = false;
        }

        if (resetAttr) {
            drawOIA();
        }

        gui.validate();
        gui.repaint();
    }

    /**
     *
     *
     * @param x
     * @param y
     * @return
     */
    public int getPosFromView(int x, int y) {

        // we have to translate the point into a an upper left 0,0 based format
        // to get the position into the character array which is 0,0 based.
        // we take the point of x,y and subtract the screen offsets.

        x -= offLeft;
        y -= offTop;

        if (x > tArea.getMaxX()) {
            x = (int) tArea.getMaxX() - 1;
        }
        if (y > tArea.getMaxY()) {
            y = (int) tArea.getMaxY() - 1;
        }
        if (x < tArea.getMinX()) {
            x = 0;
        }
        if (y < tArea.getMinY()) {
            y = 0;
        }

        int s0 = y / rowHeight;
        int s1 = x / columnWidth;

        return screen.getPos(s0, s1);

    }

    /**
     * Return the row column based on the screen x,y position coordinates
     *
     * It will calculate a 0,0 based row and column based on the screen point
     * coordinate.
     *
     * @param x
     *            screen x position
     * @param y
     *            screen y position
     *
     * @return screen array position based 0,0 so position row 1 col 3 would be
     *         2
     */
    public int getRowColFromPoint(int x, int y) {

        if (x > tArea.getMaxX()) {
            x = (int) tArea.getMaxX() - 1;
        }
        if (y > tArea.getMaxY()) {
            y = (int) tArea.getMaxY() - 1;
        }
        if (x < tArea.getMinX()) {
            x = 0;
        }
        if (y < tArea.getMinY()) {
            y = 0;
        }

        int s0 = y / rowHeight;
        int s1 = x / columnWidth;

        return screen.getPos(s0, s1);

    }


    /**
     * This will return the screen coordinates of a row and column.
     *
     * @param row
     * @param col
     * @param point
     */
    public void getPointFromRowCol(int row, int col, Point point) {

        // here the x + y coordinates of the row and column are obtained from
        // the character array which is based on a upper left 0,0 coordinate
        //  we will then add to that the offsets to get the screen position point
        //  x,y coordinates. Maybe change this to a translate routine method or
        //  something.
        point.x = (columnWidth * col) + offLeft;
        point.y = (rowHeight * row) + offTop;

    }

    public boolean isWithinScreenArea(int x, int y) {

        return tArea.contains(x, y);

    }

    /**
     *
     * RubberBanding start code
     *
     */

    /**
     * Translate the starting point of mouse movement to encompass a full
     * character
     *
     * @param start
     * @return Point
     */
    public Point translateStart(Point start) {

        // because getRowColFromPoint returns position offset as 1,1 we need
        // to translate as offset 0,0
        int pos = getPosFromView(start.x, start.y);
        int x = columnWidth * screen.getCol(pos);
        int y = rowHeight * screen.getRow(pos);
        start.setLocation(x, y);
        return start;

    }

    /**
     * Translate the ending point of mouse movement to encompass a full
     * character
     *
     * @param end
     * @return Point
     */
    public Point translateEnd(Point end) {

        // because getRowColFromPoint returns position offset as 1,1 we need
        // to translate as offset 0,0
        int pos = getPosFromView(end.x, end.y);

        if (pos >= lenScreen) {
            pos = lenScreen - 1;
        }
        int x = ((columnWidth * screen.getCol(pos)) + columnWidth) - 1;
        int y = ((rowHeight * screen.getRow(pos)) + rowHeight) - 1;

        end.setLocation(x, y);

        return end;
    }

    /**
     * Fills the passed Rectangle with the starting row and column and width and
     * height of the selected area.
     *
     * 1 BASED so column 1 row one is returned 1,1
     *
     * If there is no area bounded then the full screen area is returned.
     *
     * @param bounds
     */
    public void getBoundingArea(Rectangle bounds) {

        // check to see if there is an area selected. If not then return all
        //    screen area.
        if (!gui.rubberband.isAreaSelected()) {

            bounds.setBounds(1, 1, screen.getColumns(), screen.getRows());
        } else {
            // lets get the bounding area using a rectangle that we have already
            // allocated
            gui.rubberband.getBoundingArea(workR);

            // get starting row and column
            int sPos = getRowColFromPoint(workR.x, workR.y);
            // get the width and height
            int ePos = getRowColFromPoint(workR.width, workR.height);

            int row = screen.getRow(sPos) + 1;
            int col = screen.getCol(sPos) + 1;

            bounds.setBounds(row, col, screen.getCol(ePos) + 1, screen.getRow(ePos) + 1);
        }
    }

    /**
     * Convinience method to resize the screen area such as when the parent
     * frame is resized.
     *
     * @param width
     * @param height
     */
    protected final void resizeScreenArea(int width, int height) {

        Font derivedFont = GUIGraphicsUtils.getDerivedFont(font, width, height, screen.getRows(),
                screen.getColumns(), sfh, sfw, ps132);

        if (font.getSize() != derivedFont.getSize() || updateFont
                || (offLeft != (width - bi.getWidth()) / 2)
                || (offTop != (height - bi.getHeight()) / 2)) {

            // set up all the variables that are used in calculating the new
            // size
            font = derivedFont;
            characterMetrics.setFont(font);
            lm = characterMetrics.getLineMetrics();
            columnWidth = characterMetrics.getCharWidth();
            rowHeight = characterMetrics.getCharHeight();

            resize(columnWidth * screen.getColumns(), rowHeight * (screen.getRows() + 2));

            // set the offsets for the screen centering.
            offLeft = (width - getWidth()) / 2;
            offTop = (height - getHeight()) / 2;
            if (offLeft < 0) {
                offLeft = 0;
            }
            if (offTop < 0) {
                offTop = 0;
            }

            drawOIA();

            updateFont = false;
        }

    }

    public final Dimension getPreferredSize() {

        return new Dimension(columnWidth * screen.getColumns(), rowHeight * (screen.getRows() + 2));

    }

    public BufferedImage getImageBuffer(int width, int height) {


        int width2 = columnWidth * screen.getColumns();
        int height2 = rowHeight * (screen.getRows() + 2);
        if (bi == null || bi.getWidth() != width2 || bi.getHeight() != height2) {
            bi = new BufferedImage(width2, height2, BufferedImage.TYPE_INT_RGB);
        }
        drawOIA();
        return bi;
    }

    /**
     * Draw the operator information area
     */
    public Graphics2D drawOIA() {

        int numRows = screen.getRows();

        Graphics2D g2d;

        // get ourselves a global pointer to the graphics
        g2d = getDrawingArea();

        if (antialiased) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                RenderingHints.VALUE_COLOR_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);
        g2d.setFont(font);


        g2d.setColor(colorBg);
        g2d.fillRect(0, 0, bi.getWidth(null), bi.getHeight(null));
        tArea.setRect(0, 0, bi.getWidth(null), (rowHeight * (numRows)));
        cArea.setRect(0, rowHeight * (numRows + 1), bi.getWidth(null), rowHeight * (numRows + 1));
        aArea.setRect(0, 0, bi.getWidth(null), bi.getHeight(null));
        sArea.setRect(columnWidth * 9, rowHeight * (numRows + 1), columnWidth * 20, rowHeight);
        pArea.setRect(bi.getWidth(null) - columnWidth * 6, rowHeight * (numRows + 1), columnWidth * 6, rowHeight);
        mArea.setRect((float) (sArea.getX() + sArea.getWidth()) + columnWidth + columnWidth,
                rowHeight * (numRows + 1),
                columnWidth + columnWidth,
                rowHeight);
        kbArea.setRect((float) (sArea.getX() + sArea.getWidth()) + (20 * columnWidth),
                rowHeight * (numRows + 1),
                columnWidth + columnWidth,
                rowHeight);
        scriptArea.setRect((float) (sArea.getX() + sArea.getWidth()) + (16 * columnWidth),
                rowHeight * (numRows + 1),
                columnWidth + columnWidth,
                rowHeight);
        iArea.setRect((float) (sArea.getX() + sArea.getWidth()) + (25 * columnWidth),
                rowHeight * (numRows + 1),
                columnWidth,
                rowHeight);

        separatorLine.setLine(0,
                (rowHeight * (numRows + 1)) - (rowHeight / 2),
                bi.getWidth(null),
                (rowHeight * (numRows + 1)) - (rowHeight / 2));

        g2d.setColor(colorBlue);
        g2d.draw(separatorLine);
        drawingContext.setGraphics(g2d);
        return g2d;
    }


    public void drawCursor(int row, int col) {

        int botOffset = cursorManager.getCursorBottOffset();
        boolean insertMode = screen.getOIA().isInsertMode();

        Graphics2D g2 = getDrawingArea();

        switch (cursorManager.getCursorSize()) {
            case 0:
                cursorManager.setCursorBounds(
                        columnWidth * (col),
                        (rowHeight * (row + 1)) - botOffset,
                        columnWidth,
                        1
                );
                break;
            case 1:
                cursorManager.setCursorBounds(
                        columnWidth * (col),
                        (rowHeight * (row + 1) - rowHeight / 2),
                        columnWidth,
                        (rowHeight / 2) - botOffset
                );
                break;
            case 2:
                cursorManager.setCursorBounds(
                        columnWidth * (col),
                        (rowHeight * row),
                        columnWidth,
                        rowHeight - botOffset
                );
                break;
            default:
                break;
        }

        if (insertMode && cursorManager.getCursorSize() != 1) {
            cursorManager.setCursorBounds(
                    columnWidth * (col),
                    (rowHeight * (row + 1) - rowHeight / 2),
                    columnWidth,
                    (rowHeight / 2) - botOffset
            );
        }

        Rectangle cursorBounds = cursorManager.getCursorBounds().getBounds();
        cursorBounds.setSize(cursorBounds.width, cursorBounds.height);

        g2.setColor(colorCursor);
        g2.setXORMode(colorBg);

        g2.fill(cursorManager.getCursorBounds());

        updateImage(cursorBounds);

        if (!rulerFixed) {
            crossRow = row;
            crossRect.setBounds(cursorBounds);
        } else {
            if (crossHair == 0) {
                crossRow = row;
                crossRect.setBounds(cursorBounds);
            }
        }

        switch (crossHair) {
            case 1:  // horizontal
                g2.drawLine(0, (rowHeight * (crossRow + 1)) - botOffset,
                        bi.getWidth(null),
                        (rowHeight * (crossRow + 1)) - botOffset);
                updateImage(0, rowHeight * (crossRow + 1) - botOffset,
                        bi.getWidth(null), 1);
                break;
            case 2:  // vertical
                g2.drawLine(crossRect.x, 0, crossRect.x, bi.getHeight(null) - rowHeight - rowHeight);
                updateImage(crossRect.x, 0, 1, bi.getHeight(null) - rowHeight - rowHeight);
                break;

            case 3:  // horizontal & vertical
                g2.drawLine(0, (rowHeight * (crossRow + 1)) - botOffset,
                        bi.getWidth(null),
                        (rowHeight * (crossRow + 1)) - botOffset);
                g2.drawLine(crossRect.x, 0, crossRect.x, bi.getHeight(null) - rowHeight - rowHeight);
                updateImage(0, rowHeight * (crossRow + 1) - botOffset,
                        bi.getWidth(null), 1);
                updateImage(crossRect.x, 0, 1, bi.getHeight(null) - rowHeight - rowHeight);
                break;
            default:
                break;
        }

        g2.dispose();
        g2 = getWritingArea(font);
        g2.setPaint(colorBg);

        g2.fill(pArea);
        g2.setColor(colorWhite);

        g2.drawString((row + 1) + "/" + (col + 1),
                 (float) pArea.getX(),
                (float) pArea.getY() + rowHeight);
        updateImage(pArea.getBounds());
        g2.dispose();

    }

    private void drawScriptRunning(Color color) {

        Graphics2D g2d;

        // get ourselves a global pointer to the graphics
        g2d = (Graphics2D) bi.getGraphics();

        g2d.setColor(color);

        // set the points for the polygon
        int[] xs = {(int) scriptArea.getX(),
                (int) scriptArea.getX(),
                (int) scriptArea.getX() + (int) (scriptArea.getWidth())};
        int[] ys = {(int) scriptArea.getY(),
                (int) scriptArea.getY() + (int) scriptArea.getHeight(),
                (int) scriptArea.getY() + (int) (scriptArea.getHeight() / 2)};

        // now lets draw it
        g2d.fillPolygon(xs, ys, 3);
        g2d.setClip(scriptArea);

        // get rid of the pointers
        g2d.dispose();


    }

    private void eraseScriptRunning(Color color) {

        Graphics2D g2d;

        // get ourselves a global pointer to the graphics
        g2d = (Graphics2D) bi.getGraphics();

        g2d.setColor(color);
        g2d.fill(scriptArea);
        g2d.dispose();


    }

    /**
     * Returns a pointer to the graphics area that we can draw on
     */
    public Graphics2D getDrawingArea() {

        Graphics2D g2;

        g2 = bi.createGraphics();
        return g2;
    }

    public synchronized void drawImageBuffer(Graphics2D gg2d, int x, int y, int width, int height) {

        if (gg2d == null) {
            log.debug(" we got a null graphic object ");
            return;
        }

        gg2d.drawImage(bi.getSubimage(x, y, width, height), null, x + offLeft, y + offTop);

    }

    protected void updateImage(int x, int y, int width, int height) {


        // check for selected area and erase it before updating screen
        if (gui.rubberband != null && gui.rubberband.isAreaSelected()) {
            gui.rubberband.erase();
        }

        Graphics2D gg2d = drawingContext.getGraphics();
        gg2d.setClip(x, y, width, height);
        // Redraw the selected area rectangle.
        if (gui.rubberband != null && gui.rubberband.isAreaSelected()) {
            gui.rubberband.draw();
        }

        if (x == 0) {
            width += offLeft;
        } else {
            x += offLeft;
        }
        if (y == 0) {
            height += offTop;
        } else {
            y += offTop;
        }

        final int heightf = height;
        final int widthf = width;
        final int xf = x;
        final int yf = y;
        try {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    gui.repaint(xf, yf, widthf, heightf);
                }
            });

        } catch (Exception exc) {
            log.warn("setStatus(ON) " + exc.getMessage());

        }

    }

    protected void updateImage(Rectangle bounds) {
        updateImage(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public synchronized void drawImageBuffer(Graphics2D gg2d) {

        if (gg2d == null) {
            log.debug(" we got a null graphic object ");
            return;
        }

        gg2d.drawImage(bi, null, offLeft, offTop);

    }

    /**
     * Returns a pointer to the graphics area that we can write on
     */
    public Graphics2D getWritingArea(Font font) {

        Graphics2D g2;

        g2 = bi.createGraphics();

        if (g2 != null) {
            if (antialiased) {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
            }
            g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                    RenderingHints.VALUE_COLOR_RENDER_SPEED);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_SPEED);
            g2.setFont(font);
        }

        return g2;
    }

    public void setUseAntialias(boolean antialiased) {
        this.antialiased = antialiased;
    }

    private void setStatus(ScreenOIA oia) {

        int attr = oia.getLevel();
        int value = oia.getInputInhibited();
        String inhibitedText = oia.getInhibitedText();
        Graphics2D g2d = getWritingArea(font);
        if (g2d == null) {
            return;
        }

        try {
            g2d.setColor(colorBg);
            g2d.fill(sArea);

            float baselineY = ((int) sArea.getY() + rowHeight) - (lm.getLeading() + lm.getDescent());

            switch (attr) {

                case ScreenOIA.OIA_LEVEL_INPUT_INHIBITED:
                    if (value == ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT) {
                        g2d.setColor(colorWhite);

                        if (inhibitedText != null) {
                            g2d.drawString(inhibitedText, (float) sArea.getX(), baselineY);
                        } else {
                            g2d.drawString(xSystem, (float) sArea.getX(), baselineY);
                        }
                    }
                    break;
                case ScreenOIA.OIA_LEVEL_INPUT_ERROR:
                    if (value == ScreenOIA.INPUTINHIBITED_SYSTEM_WAIT) {
                        g2d.setColor(colorRed);

                        if (inhibitedText != null) {
                            g2d.drawString(inhibitedText, (float) sArea.getX(), baselineY);
                        } else {
                            g2d.drawString(xError, (float) sArea.getX(), baselineY);
                        }

                    }
                    break;

                default:
                    break;
            }
            updateImage(sArea.getBounds());
            g2d.dispose();
        } catch (Exception exception) {

            log.warn(" gui graphics setStatus " + exception.getMessage());

        }
    }

    public final void drawChar(Graphics2D graphics, int pos, int row, int col) {
        Rectangle csArea = new Rectangle();
        char sChar[] = new char[1];
        int attr = updateRect.attr[pos];
        sChar[0] = updateRect.text[pos];
        setDrawAttr(pos);
        boolean attributePlace = updateRect.isAttr[pos] != 0;
        int whichGui = updateRect.graphic[pos];
        boolean useGui = whichGui != 0;

        csArea = modelToView(row, col, csArea);

        int x = csArea.x;
        int y = csArea.y;
        int cy = (int) (y + rowHeight - (lm.getDescent() + lm.getLeading()));

        if (showHex && attributePlace) {
            Font currentFont = graphics.getFont();

            Font halfSizeFont = currentFont.deriveFont(currentFont.getSize2D() / 2);
            graphics.setFont(halfSizeFont);
            graphics.setColor(colorHexAttr);
            char[] hexChars = Integer.toHexString(attr).toCharArray();
            graphics.drawChars(hexChars, 0, 1, x, y + (rowHeight / 2));
            graphics.drawChars(hexChars, 1, 1, x + (columnWidth / 2),
                    (int) (y + rowHeight - (lm.getDescent() + lm.getLeading()) - 2));
            graphics.setFont(currentFont);
        }

        if (!nonDisplay && !attributePlace) {

            if (!useGui) {
                graphics.setColor(bg);
                graphics.fill(csArea);
            } else {

                if (bg == colorBg && whichGui >= HTI5250jConstants.FIELD_LEFT && whichGui <= HTI5250jConstants.FIELD_ONE) {
                    graphics.setColor(colorGUIField);
                } else {
                    graphics.setColor(bg);
                }

                graphics.fill(csArea);

            }

            if (useGui && (whichGui < HTI5250jConstants.FIELD_LEFT)) {

                graphics.setColor(fg);

                switch (whichGui) {

                    case HTI5250jConstants.UPPER_LEFT:
                        if (sChar[0] == '.') {
                            if (screen.isUsingGuiInterface()) {
                                GUIGraphicsUtils.drawWinUpperLeft(graphics,
                                        GUIGraphicsUtils.WINDOW_GRAPHIC,
                                        colorBlue,
                                        x, y, columnWidth, rowHeight);

                            } else {

                                GUIGraphicsUtils.drawWinUpperLeft(graphics,
                                        GUIGraphicsUtils.WINDOW_NORMAL,
                                        fg,
                                        x, y, columnWidth, rowHeight);

                            }
                        }
                        break;
                    case HTI5250jConstants.UPPER:
                        if (sChar[0] == '.') {

                            if (screen.isUsingGuiInterface()) {
                                GUIGraphicsUtils.drawWinUpper(graphics,
                                        GUIGraphicsUtils.WINDOW_GRAPHIC,
                                        colorBlue,
                                        x, y, columnWidth, rowHeight);


                            } else {

                                GUIGraphicsUtils.drawWinUpper(graphics,
                                        GUIGraphicsUtils.WINDOW_NORMAL,
                                        fg,
                                        x, y, columnWidth, rowHeight);
                            }
                        }
                        break;
                    case HTI5250jConstants.UPPER_RIGHT:
                        if (sChar[0] == '.') {
                            if (screen.isUsingGuiInterface()) {

                                GUIGraphicsUtils.drawWinUpperRight(graphics,
                                        GUIGraphicsUtils.WINDOW_GRAPHIC,
                                        colorBlue,
                                        x, y, columnWidth, rowHeight);


                            } else {

                                GUIGraphicsUtils.drawWinUpperRight(graphics,
                                        GUIGraphicsUtils.WINDOW_NORMAL,
                                        fg,
                                        x, y, columnWidth, rowHeight);

                            }
                        }
                        break;
                    case HTI5250jConstants.GUI_LEFT:
                        if (sChar[0] == ':') {
                            if (screen.isUsingGuiInterface()) {
                                GUIGraphicsUtils.drawWinLeft(graphics,
                                        GUIGraphicsUtils.WINDOW_GRAPHIC,
                                        bg,
                                        x, y, columnWidth, rowHeight);


                            } else {

                                GUIGraphicsUtils.drawWinLeft(graphics,
                                        GUIGraphicsUtils.WINDOW_NORMAL,
                                        fg,
                                        x, y, columnWidth, rowHeight);

                                graphics.drawLine(x + columnWidth / 2,
                                        y,
                                        x + columnWidth / 2,
                                        y + rowHeight);
                            }
                        }
                        break;
                    case HTI5250jConstants.GUI_RIGHT:
                        if (sChar[0] == ':') {
                            if (screen.isUsingGuiInterface()) {
                                GUIGraphicsUtils.drawWinRight(graphics,
                                        GUIGraphicsUtils.WINDOW_GRAPHIC,
                                        bg,
                                        x, y, columnWidth, rowHeight);


                            } else {
                                GUIGraphicsUtils.drawWinRight(graphics,
                                        GUIGraphicsUtils.WINDOW_NORMAL,
                                        fg,
                                        x, y, columnWidth, rowHeight);

                            }
                        }
                        break;
                    case HTI5250jConstants.LOWER_LEFT:
                        if (sChar[0] == ':') {

                            if (screen.isUsingGuiInterface()) {

                                GUIGraphicsUtils.drawWinLowerLeft(graphics,
                                        GUIGraphicsUtils.WINDOW_GRAPHIC,
                                        bg,
                                        x, y, columnWidth, rowHeight);


                            } else {

                                GUIGraphicsUtils.drawWinLowerLeft(graphics,
                                        GUIGraphicsUtils.WINDOW_NORMAL,
                                        fg,
                                        x, y, columnWidth, rowHeight);
                            }
                        }
                        break;
                    case HTI5250jConstants.BOTTOM:
                        if (sChar[0] == '.') {

                            if (screen.isUsingGuiInterface()) {


                                GUIGraphicsUtils.drawWinBottom(graphics,
                                        GUIGraphicsUtils.WINDOW_GRAPHIC,
                                        bg,
                                        x, y, columnWidth, rowHeight);


                            } else {

                                GUIGraphicsUtils.drawWinBottom(graphics,
                                        GUIGraphicsUtils.WINDOW_NORMAL,
                                        fg,
                                        x, y, columnWidth, rowHeight);
                            }
                        }
                        break;

                    case HTI5250jConstants.LOWER_RIGHT:
                        if (sChar[0] == ':') {
                            if (screen.isUsingGuiInterface()) {

                                GUIGraphicsUtils.drawWinLowerRight(graphics,
                                        GUIGraphicsUtils.WINDOW_GRAPHIC,
                                        bg,
                                        x, y, columnWidth, rowHeight);

                            } else {

                                GUIGraphicsUtils.drawWinLowerRight(graphics,
                                        GUIGraphicsUtils.WINDOW_NORMAL,
                                        fg,
                                        x, y, columnWidth, rowHeight);

                            }
                        }
                        break;

                    default:
                        break;
                }
            } else {
                if (sChar[0] != 0x0) {
                    // use this until we define colors for gui stuff
                    if ((useGui && whichGui < HTI5250jConstants.BUTTON_LEFT) && (fg == colorGUIField)) {

                        graphics.setColor(Color.black);
                    } else {
                        graphics.setColor(fg);
                    }

                    try {
                        if (useGui) {

                            if (sChar[0] == 0x1C) {
                                graphics.drawChars(dupChar, 0, 1, x + 1, cy - 2);
                            } else {
                                graphics.drawChars(sChar, 0, 1, x + 1, cy - 2);
                            }
                        } else if (sChar[0] == 0x1C) {
                            graphics.drawChars(dupChar, 0, 1, x, cy - 2);
                        } else {
                            graphics.drawChars(sChar, 0, 1, x, cy - 2);
                        }
                    } catch (IllegalArgumentException iae) {
                        System.out.println(" drawChar iae " + iae.getMessage());

                    }
                }
                if (underLine) {

                    if (!useGui || cfg_guiShowUnderline) {
                        graphics.setColor(fg);
                        graphics.drawLine(x, (int) (y + (rowHeight - (lm.getLeading() + lm.getDescent()))), (x + columnWidth), (int) (y + (rowHeight - (lm.getLeading() + lm.getDescent()))));

                    }
                }

                if (colSep) {
                    graphics.setColor(colorSep);
                    switch (colSepLine) {
                        case Line:  // line
                            graphics.drawLine(x, y, x, y + rowHeight - 1);
                            graphics.drawLine(x + columnWidth - 1, y, x + columnWidth - 1, y + rowHeight);
                            break;
                        case ShortLine:  // short line
                            graphics.drawLine(x, y + rowHeight - (int) lm.getLeading() - 4, x, y + rowHeight);
                            graphics.drawLine(x + columnWidth - 1, y + rowHeight - (int) lm.getLeading() - 4, x + columnWidth - 1, y + rowHeight);
                            break;
                        case Dot:  // dot
                            graphics.drawLine(x, y + rowHeight - (int) lm.getLeading() - 3, x, y + rowHeight - (int) lm.getLeading() - 4);
                            graphics.drawLine(x + columnWidth - 1, y + rowHeight - (int) lm.getLeading() - 3, x + columnWidth - 1, y + rowHeight - (int) lm.getLeading() - 4);
                            break;
                        case Hide:  // hide
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        if (useGui && (whichGui >= HTI5250jConstants.FIELD_LEFT)) {

            switch (whichGui) {

                case HTI5250jConstants.FIELD_LEFT:
                    GUIGraphicsUtils.draw3DLeft(graphics, GUIGraphicsUtils.INSET, x, y,
                            columnWidth, rowHeight);

                    break;
                case HTI5250jConstants.FIELD_MIDDLE:
                    GUIGraphicsUtils.draw3DMiddle(graphics, GUIGraphicsUtils.INSET, x, y,
                            columnWidth, rowHeight);
                    break;
                case HTI5250jConstants.FIELD_RIGHT:
                    GUIGraphicsUtils.draw3DRight(graphics, GUIGraphicsUtils.INSET, x, y,
                            columnWidth, rowHeight);
                    break;

                case HTI5250jConstants.FIELD_ONE:
                    GUIGraphicsUtils.draw3DOne(graphics, GUIGraphicsUtils.INSET, x, y,
                            columnWidth, rowHeight);

                    break;

                case HTI5250jConstants.BUTTON_LEFT:
                case HTI5250jConstants.BUTTON_LEFT_UP:
                case HTI5250jConstants.BUTTON_LEFT_DN:
                case HTI5250jConstants.BUTTON_LEFT_EB:

                    GUIGraphicsUtils.draw3DLeft(graphics, GUIGraphicsUtils.RAISED, x, y,
                            columnWidth, rowHeight);

                    break;

                case HTI5250jConstants.BUTTON_MIDDLE:
                case HTI5250jConstants.BUTTON_MIDDLE_UP:
                case HTI5250jConstants.BUTTON_MIDDLE_DN:
                case HTI5250jConstants.BUTTON_MIDDLE_EB:

                    GUIGraphicsUtils.draw3DMiddle(graphics, GUIGraphicsUtils.RAISED, x, y,
                            columnWidth, rowHeight);
                    break;

                case HTI5250jConstants.BUTTON_RIGHT:
                case HTI5250jConstants.BUTTON_RIGHT_UP:
                case HTI5250jConstants.BUTTON_RIGHT_DN:
                case HTI5250jConstants.BUTTON_RIGHT_EB:

                    GUIGraphicsUtils.draw3DRight(graphics, GUIGraphicsUtils.RAISED, x, y,
                            columnWidth, rowHeight);

                    break;

                // scroll bar
                case HTI5250jConstants.BUTTON_SB_UP:
                    GUIGraphicsUtils.drawScrollBar(graphics, GUIGraphicsUtils.RAISED, 1, x, y,
                            columnWidth, rowHeight,
                            colorWhite, colorBg);
                    break;

                // scroll bar
                case HTI5250jConstants.BUTTON_SB_DN:

                    GUIGraphicsUtils.drawScrollBar(graphics, GUIGraphicsUtils.RAISED, 2, x, y,
                            columnWidth, rowHeight,
                            colorWhite, colorBg);


                    break;
                // scroll bar
                case HTI5250jConstants.BUTTON_SB_GUIDE:

                    GUIGraphicsUtils.drawScrollBar(graphics, GUIGraphicsUtils.INSET, 0, x, y,
                            columnWidth, rowHeight,
                            colorWhite, colorBg);


                    break;

                // scroll bar
                case HTI5250jConstants.BUTTON_SB_THUMB:

                    GUIGraphicsUtils.drawScrollBar(graphics, GUIGraphicsUtils.INSET, 3, x, y,
                            columnWidth, rowHeight,
                            colorWhite, colorBg);


                    break;

                default:
                    break;
            }
        }

    }

    public void onScreenSizeChanged(int rows, int cols) {
        log.info("screen size change");
        gui.resizeMe();
    }

    public void onScreenChanged(int which, int sr, int sc, int er, int ec) {
        if (which == 3 || which == 4) {
            drawCursor(sr, sc);
            return;
        }

        if (hotSpots) {
            screen.checkHotSpots();
        }

        updateRect = new Data(sr, sc, er, ec);

        Rectangle clipper = new Rectangle();
        clipper.x = sc * columnWidth;
        clipper.y = sr * rowHeight;
        clipper.width = ((ec - sc) + 1) * columnWidth;
        clipper.height = ((er - sr) + 1) * rowHeight;

        Graphics2D gg2d = drawingContext.getGraphics();
        gg2d.setClip(clipper.getBounds());

        gg2d.setColor(colorBg);

        gg2d.fillRect(clipper.x, clipper.y, clipper.width, clipper.height);

        int pos = 0;
        while (sr <= er) {
            int cols = ec - sc;
            int lc = sc;
            while (cols-- >= 0) {
                if (sc + cols <= ec) {
                    drawChar(gg2d, pos++, sr, lc);
                    lc++;
                }
            }
            sr++;
        }
        updateImage(clipper);
    }

    public void onOIAChanged(ScreenOIA changedOIA, int change) {

        switch (changedOIA.getLevel()) {

            case ScreenOIA.OIA_LEVEL_KEYS_BUFFERED:
                if (changedOIA.isKeysBuffered()) {
                    Graphics2D g2d = getWritingArea(font);
                    float Y = (rowHeight * (screen.getRows() + 2))
                            - (lm.getLeading() + lm.getDescent());
                    g2d.setColor(colorYellow);
                    g2d.drawString("KB", (float) kbArea.getX(), Y);

                    updateImage(kbArea.getBounds());
                    g2d.dispose();
                } else {
                    Graphics2D g2d = getWritingArea(font);
                    g2d.setColor(colorBg);
                    g2d.fill(kbArea);
                    updateImage(kbArea.getBounds());
                    g2d.dispose();
                }
                break;
            case ScreenOIA.OIA_LEVEL_MESSAGE_LIGHT_OFF:
                Graphics2D g2d = getWritingArea(font);

                g2d.setColor(colorBg);
                g2d.fill(mArea);
                updateImage(mArea.getBounds());
                g2d.dispose();
                break;
            case ScreenOIA.OIA_LEVEL_MESSAGE_LIGHT_ON:
                g2d = getWritingArea(font);
                float Y = (rowHeight * (screen.getRows() + 2))
                        - (lm.getLeading() + lm.getDescent());
                g2d.setColor(colorBlue);
                g2d.drawString("MW", (float) mArea.getX(), Y);
                updateImage(mArea.getBounds());
                g2d.dispose();
                break;
            case ScreenOIA.OIA_LEVEL_SCRIPT:
                if (changedOIA.isScriptActive()) {
                    drawScriptRunning(colorGreen);
                    updateImage(scriptArea.getBounds());
                } else {
                    eraseScriptRunning(colorBg);
                    updateImage(scriptArea.getBounds());

                }
                break;
            case ScreenOIA.OIA_LEVEL_INPUT_INHIBITED:
            case ScreenOIA.OIA_LEVEL_NOT_INHIBITED:
            case ScreenOIA.OIA_LEVEL_INPUT_ERROR:
                setStatus(changedOIA);
                break;
            case ScreenOIA.OIA_LEVEL_INSERT_MODE:
                if (changedOIA.isInsertMode()) {
                    g2d = getWritingArea(font);
                    Y = (rowHeight * (screen.getRows() + 2))
                            - (lm.getLeading() + lm.getDescent());
                    g2d.setColor(colorBlue);
                    g2d.drawLine((int) iArea.getX(), (int) Y, (int) (iArea.getX() + ((iArea.getWidth() / 2) - 1)), (int) (Y - (rowHeight / 2)));
                    g2d.drawLine((int) (iArea.getX() + iArea.getWidth() - 1), (int) Y, (int) (iArea.getX() + (iArea.getWidth() / 2)), (int) (Y - (rowHeight / 2)));
                    //g2d.drawString("I", (float) iArea.getX(), Y);

                    updateImage(iArea.getBounds());
                    g2d.dispose();
                } else {

                    g2d = getWritingArea(font);

                    g2d.setColor(colorBg);
                    g2d.fill(iArea);
                    updateImage(iArea.getBounds());
                    g2d.dispose();

                }
                break;

            default:
                break;
        }
    }

    /**
     * get the
     */
    public Rectangle2D getTextArea() {
        return tArea;
    }

    public Rectangle2D getScreenArea() {
        return aArea;
    }

    public Rectangle2D getCommandLineArea() {
        return cArea;
    }

    public Rectangle2D getStatusArea() {
        return sArea;
    }

    public Rectangle2D getPositionArea() {
        return pArea;
    }

    public Rectangle2D getMessageArea() {
        return mArea;
    }

    public Rectangle2D getInsertIndicatorArea() {
        return iArea;
    }

    public Rectangle2D getKBIndicatorArea() {
        return kbArea;
    }

    public Rectangle2D getScriptIndicatorArea() {
        return scriptArea;
    }

    public int getWidth() {

        synchronized (lock) {
            // tell waiting threads to wake up
            lock.notifyAll();
            return bi.getWidth();
        }

    }

    public int getHeight() {

        synchronized (lock) {
            // tell waiting threads to wake up
            lock.notifyAll();
            return bi.getHeight();
        }
    }

    public int getWidth(ImageObserver io) {

        synchronized (lock) {
            // tell waiting threads to wake up
            lock.notifyAll();
            return bi.getWidth(io);
        }
    }

    public int getHeight(ImageObserver io) {

        synchronized (lock) {
            // tell waiting threads to wake up
            lock.notifyAll();
            return bi.getHeight(io);
        }
    }


    protected Data fillData(int startRow, int startCol, int endRow, int endCol) {

        return new Data(startRow, startCol, endRow, endCol);

    }

    protected class Data {

        public char[] text;
        public char[] attr;
        public char[] isAttr;
        public char[] color;
        public char[] extended;
        public final char[] graphic;
        public final char[] field;

        public Data(char[] text, char[] attr, char[] color, char[] extended, char[] graphic) {
            this.text = text;
            this.color = color;
            this.extended = extended;
            this.graphic = graphic;
            this.attr = attr;
            this.field = null;
        }

        public Data(int startRow, int startCol, int endRow, int endCol) {
            startRow++;
            startCol++;
            endRow++;
            endCol++;
            int size = ((endCol - startCol) + 1) * ((endRow - startRow) + 1);

            text = new char[size];
            attr = new char[size];
            isAttr = new char[size];
            color = new char[size];
            extended = new char[size];
            graphic = new char[size];
            field = new char[size];

            if (size == lenScreen) {
                screen.GetScreen(text, size, HTI5250jConstants.PLANE_TEXT);
                screen.GetScreen(attr, size, HTI5250jConstants.PLANE_ATTR);
                screen.GetScreen(isAttr, size, HTI5250jConstants.PLANE_IS_ATTR_PLACE);
                screen.GetScreen(color, size, HTI5250jConstants.PLANE_COLOR);
                screen.GetScreen(extended, size, HTI5250jConstants.PLANE_EXTENDED);
                screen.GetScreen(graphic, size, HTI5250jConstants.PLANE_EXTENDED_GRAPHIC);
                screen.GetScreen(field, size, HTI5250jConstants.PLANE_FIELD);
            } else {
                screen.GetScreenRect(text, size, startRow, startCol, endRow, endCol, HTI5250jConstants.PLANE_TEXT);
                screen.GetScreenRect(attr, size, startRow, startCol, endRow, endCol, HTI5250jConstants.PLANE_ATTR);
                screen.GetScreenRect(isAttr, size, startRow, startCol, endRow, endCol, HTI5250jConstants.PLANE_IS_ATTR_PLACE);
                screen.GetScreenRect(color, size, startRow, startCol, endRow, endCol, HTI5250jConstants.PLANE_COLOR);
                screen.GetScreenRect(extended, size, startRow, startCol, endRow, endCol, HTI5250jConstants.PLANE_EXTENDED);
                screen.GetScreenRect(graphic, size, startRow, startCol, endRow, endCol, HTI5250jConstants.PLANE_EXTENDED_GRAPHIC);
                screen.GetScreenRect(field, size, startRow, startCol, endRow, endCol, HTI5250jConstants.PLANE_FIELD);
            }
        }

    }

    public final Rectangle modelToView(int row, int col) {
        return modelToView(row, col, new Rectangle());
    }

    public final Rectangle modelToView(int row, int col, Rectangle bounds) {

        // right now row and column is 1,1 offset based.  This will need
        //   to be changed to 0,0 offset based by subtracting 1 from them
        //   when the screen is being passed this way
        //     r.x      =  (col - 1) * columnWidth;
        //     r.y      =  (row - 1) * rowHeight;
        bounds.x = col * columnWidth;
        bounds.y = row * rowHeight;
        bounds.width = columnWidth;
        bounds.height = rowHeight;
        return bounds;
    }

    protected Color getColor(char color, boolean background) {
        int colorValue = 0;
        if (background) {
            // background
            colorValue = (color & 0xff00) >> 8;
        } else {
            // foreground
            colorValue = color & 0x00ff;
        }

        switch (colorValue) {
            case HTI5250jConstants.COLOR_FG_BLACK:
                return colorBg;
            case HTI5250jConstants.COLOR_FG_GREEN:
                return colorGreen;
            case HTI5250jConstants.COLOR_FG_BLUE:
                return colorBlue;
            case HTI5250jConstants.COLOR_FG_RED:
                return colorRed;
            case HTI5250jConstants.COLOR_FG_YELLOW:
                return colorYellow;
            case HTI5250jConstants.COLOR_FG_CYAN:
                return colorTurq;
            case HTI5250jConstants.COLOR_FG_WHITE:
                return colorWhite;
            case HTI5250jConstants.COLOR_FG_MAGENTA:
                return colorPink;
            default:
                return Color.orange;
        }
    }

    /**
     * Determines if GUI rendering should be applied based on enable flag and field type.
     *
     * This method encapsulates the logic for checking if GUI field boundaries should
     * be drawn. It uses logical AND (&&) for proper short-circuit evaluation and
     * semantic correctness when combining boolean conditions.
     *
     * @param useGui     The GUI enable flag (true = GUI rendering enabled)
     * @param whichGui   The GUI field type constant to check
     * @param minValue   The minimum valid GUI field type value for rendering
     * @return           true if both GUI is enabled AND field type is valid for rendering
     */
    private boolean shouldApplyGuiRendering(boolean useGui, int whichGui, int minValue) {
        // Using logical AND ensures:
        // 1. Proper semantics: both conditions must be true for GUI rendering
        // 2. Short-circuit evaluation: if useGui is false, second condition is not evaluated
        // 3. Clarity: the intent is explicit (both conditions must be true)
        return useGui && (whichGui >= minValue);
    }

    private void setDrawAttr(int pos) {

        colSep = false;
        underLine = false;
        nonDisplay = false;

        fg = getColor(updateRect.color[pos], false);
        bg = getColor(updateRect.color[pos], true);
        underLine = (updateRect.extended[pos] & HTI5250jConstants.EXTENDED_5250_UNDERLINE) != 0;
        colSep = (updateRect.extended[pos] & HTI5250jConstants.EXTENDED_5250_COL_SEP) != 0;
        nonDisplay = (updateRect.extended[pos] & HTI5250jConstants.EXTENDED_5250_NON_DSP) != 0;

    }

    /**
     * Get the background color from the color palette.
     * @return the current background color
     */
    public Color getBackgroundColor() {
        return colorPalette.getBg();
    }


}
