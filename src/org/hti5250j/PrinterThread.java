/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j;

import org.hti5250j.framework.tn5250.Screen5250;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;

class PrinterThread extends Thread implements Printable {

    private char[] screen;
    private char[] screenExtendedAttr;
    private char[] screenAttrPlace;
    private int numCols;
    private int numRows;
    private Font font;
    private SessionPanel session;
    private SessionConfig config;

    PrinterThread(Screen5250 screen5250, Font font, int cols, int rows,
                  Color colorBg, boolean toDefaultPrinter, SessionPanel sessionPanel) {


        setPriority(1);
        session = sessionPanel;
        session.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        config = sessionPanel.getSession().getConfiguration();

        int screenLength = screen5250.getScreenLength();
        screen = new char[screenLength];
        screenExtendedAttr = new char[screenLength];
        screenAttrPlace = new char[screenLength];
        screen5250.GetScreen(screen, screenLength, HTI5250jConstants.PLANE_TEXT);
        screen5250.GetScreen(screenExtendedAttr, screenLength, HTI5250jConstants.PLANE_EXTENDED);
        screen5250.GetScreen(screenAttrPlace, screenLength, HTI5250jConstants.PLANE_IS_ATTR_PLACE);

        numCols = cols;
        numRows = rows;
        this.font = font;
    }

    public void run() {
// Toolkit tk = Toolkit.getDefaultToolkit();
//int [][] range = new int[][] {
//new int[] { 1, 1 }
//};
// JobAttributes jobAttributes = new JobAttributes(1, JobAttributes.DefaultSelectionType.ALL, JobAttributes.DestinationType.PRINTER, JobAttributes.DialogType.NONE, "file", 1, 1, JobAttributes.MultipleDocumentHandlingType.SEPARATE_DOCUMENTS_COLLATED_COPIES, range, "HP LaserJet", JobAttributes.SidesType.ONE_SIDED);
//PrintJob job = tk.getPrintJob(null, "Print", jobAttributes, null);
//if (job != null) {
        //--- Create a printerJob object
        PrinterJob printJob = PrinterJob.getPrinterJob();
        printJob.setJobName("tn5250j");

        // will have to remember this for the next time.
        //   Always set a page format before call setPrintable to
        //   set the orientation.
        PageFormat pf = printJob.defaultPage();
        if (numCols == 132)
            pf.setOrientation(PageFormat.LANDSCAPE);
        else
            pf.setOrientation(PageFormat.PORTRAIT);


        if (numCols != 132) {
            if (config.getStringProperty("print.portWidth").length() != 0 &&
                    config.getStringProperty("print.portHeight").length() != 0 &&
                    config.getStringProperty("print.portImageWidth").length() != 0 &&
                    config.getStringProperty("print.portImageHeight").length() != 0 &&
                    config.getStringProperty("print.portImage.X").length() != 0 &&
                    config.getStringProperty("print.portImage.Y").length() != 0) {

                Paper paper = pf.getPaper();

                paper.setSize(
                        Double.parseDouble(config.getStringProperty("print.portWidth")),
                        Double.parseDouble(config.getStringProperty("print.portHeight")));

                paper.setImageableArea(
                        Double.parseDouble(config.getStringProperty("print.portImage.X")),
                        Double.parseDouble(config.getStringProperty("print.portImage.Y")),
                        Double.parseDouble(config.getStringProperty("print.portImageWidth")),
                        Double.parseDouble(config.getStringProperty("print.portImageHeight")));
                pf.setPaper(paper);
            }
        } else {

            if (config.getStringProperty("print.landWidth").length() != 0 &&
                    config.getStringProperty("print.landHeight").length() != 0 &&
                    config.getStringProperty("print.landImageWidth").length() != 0 &&
                    config.getStringProperty("print.landImageHeight").length() != 0 &&
                    config.getStringProperty("print.landImage.X").length() != 0 &&
                    config.getStringProperty("print.landImage.Y").length() != 0) {

                Paper paper = pf.getPaper();

                paper.setSize(
                        Double.parseDouble(config.getStringProperty("print.landWidth")),
                        Double.parseDouble(config.getStringProperty("print.landHeight")));

                paper.setImageableArea(
                        Double.parseDouble(config.getStringProperty("print.landImage.X")),
                        Double.parseDouble(config.getStringProperty("print.landImage.Y")),
                        Double.parseDouble(config.getStringProperty("print.landImageWidth")),
                        Double.parseDouble(config.getStringProperty("print.landImageHeight")));
            }
        }

        if (config.getStringProperty("print.font").length() > 0) {

            font = new Font(config.getStringProperty("print.font"),
                    Font.PLAIN, 8);
        }

        //--- Set the printable class to this one since we
        //--- are implementing the Printable interface
        printJob.setPrintable(this, pf);

        // set the cursor back
        session.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));


        //--- Show a print dialog to the user. If the user
        //--- clicks the print button, then print, otherwise
        //--- cancel the print job
        if (printJob.printDialog()) {
            try {
                // we do this because of loosing focus with jdk 1.4.0
                session.requestFocus();
                printJob.print();
            } catch (Exception printException) {
                printException.printStackTrace();
            }
        } else {
            // we do this because of loosing focus with jdk 1.4.0
            session.requestFocus();
        }

        session = null;

        screen = null;
        screenExtendedAttr = null;

    }

    /**
     * Method: print <p>
     * <p>
     * This routine is responsible for rendering a page using
     * the provided parameters. The result will be a screen
     * print of the current screen to the printer graphics object
     *
     * @param graphics   a value of type Graphics
     * @param pageFormat a value of type PageFormat
     * @param page       a value of type int
     * @return a value of type int
     */
    public int print(Graphics graphics, PageFormat pageFormat, int page) {

        Graphics2D graphics2D;

        //--- Validate the page number, we only print the first page
        if (page == 0) {

            //--- Create a graphic2D object and set the default parameters
            graphics2D = (Graphics2D) graphics;
            graphics2D.setColor(Color.black);

            //--- Translate the origin to be (0,0)
            graphics2D.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            int proposedWidth = (int) pageFormat.getImageableWidth() / numCols;     // proposed width
            int proposedHeight = (int) pageFormat.getImageableHeight() / numRows;     // proposed height


            LineMetrics lineMetrics;
            FontRenderContext fontRenderContext = null;

            float fontSize = 1;
            Font derivedFont;
            for (; fontSize < 50; fontSize++) {

                // derive the font and obtain the relevent information to compute
                // the width and height
                derivedFont = font.deriveFont(fontSize);
                fontRenderContext = new FontRenderContext(derivedFont.getTransform(), true, true);
                lineMetrics = derivedFont.getLineMetrics("Wy", fontRenderContext);

                if (
                        (proposedWidth < (int) derivedFont.getStringBounds("W", fontRenderContext).getWidth()) ||
                                proposedHeight < (int) (derivedFont.getStringBounds("y", fontRenderContext).getHeight() +
                                        lineMetrics.getDescent() + lineMetrics.getLeading())

                )
                    break;
            }

            // since we were looking for an overrun of the width or height we need
            // to adjust the font one down to get the last one that fit.
            derivedFont = font.deriveFont(--fontSize);
            fontRenderContext = new FontRenderContext(derivedFont.getTransform(), true, true);
            lineMetrics = derivedFont.getLineMetrics("Wy", fontRenderContext);

            // set the font of the print job
            graphics2D.setFont(derivedFont);

            // get the width and height of the character bounds
            int charWidth = (int) derivedFont.getStringBounds("W", fontRenderContext).getWidth();
            int charHeight = (int) (derivedFont.getStringBounds("y", fontRenderContext).getHeight() +
                    lineMetrics.getDescent() + lineMetrics.getLeading());
            int xPosition;
            int yPosition;

//         int pos = 0;

            // loop through all the screen characters and print them out.
            for (int m = 0; m < numRows; m++)
                for (int i = 0; i < numCols; i++) {
                    xPosition = charWidth * i;
                    yPosition = charHeight * (m + 1);

                    // only draw printable characters (in this case >= ' ')
                    if (screen[getPos(m, i)] >= ' ' && ((screenExtendedAttr[getPos(m, i)] & HTI5250jConstants.EXTENDED_5250_NON_DSP) == 0)) {

                        graphics2D.drawChars(
                                screen,
                                getPos(m, i),
                                1,
                                xPosition,
                                (int) (yPosition + charHeight - (lineMetrics.getDescent() + lineMetrics.getLeading()) - 2)
                        );

                    }

                    // if it is underlined then underline the character
//               if (screen[getPos(m,i)].underLine && !screen[getPos(m,i)].attributePlace)
                    if ((screenExtendedAttr[getPos(m, i)] & HTI5250jConstants.EXTENDED_5250_UNDERLINE) != 0 &&
                            screenAttrPlace[getPos(m, i)] != 1)
                        graphics.drawLine(
                                xPosition,
                                (int) (yPosition + (charHeight - lineMetrics.getLeading() - 3)),
                                (xPosition + charWidth),
                                (int) (yPosition + (charHeight - lineMetrics.getLeading()) - 3)
                        );

                }

            return (PAGE_EXISTS);
        } else
            return (NO_SUCH_PAGE);
    }

    private int getPos(int row, int col) {

        return (row * numCols) + col;
    }
}
