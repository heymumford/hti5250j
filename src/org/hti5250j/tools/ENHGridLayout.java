/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.tools;

import java.awt.GridLayout;
import java.awt.Container;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;

/**
 * <code>ENHGridLayout</code> is an improved subclass of <code>GridLayout</code>.
 * It lays out a grid of rows and columns based on the attributes of the
 * individual rows and columns. <code>ENHGridLayout</code>
 * uses the widest element in a column to set the width of that
 * column, and the tallest element in a row to set the height of
 * that row.
 */
public class ENHGridLayout extends GridLayout {

    private static final long serialVersionUID = 1L;

    /** The horiztonal gap between items. */
    protected int hgap;

    /** The vertical gap between items. */
    protected int vgap;

    /** The number of rows in the layout, as set by the user.
     * This number may not correspond exactly to the number of
     * rows in the layout.
     */
    protected int rows;

    /** The number of columns in the layout, as set by the user.
     * This number may not correspond exactly to the number of
     * columns in the layout.
     */
    protected int cols;

    /** Array of row heights.
     * It is accurate only after a call to getGridSizes()
     */
    protected int[] rowHeights = new int[0];

    /** Array of column widths.
     * It is accurate only after a call to getGridSizes()
     */
    protected int[] colWidths = new int[0];

    public static final int VARIABLE = 0;

    /**
     * Creates a grid layout with the specified number of rows and columns.
     * @param rows the number of rows in the layout
     * @param cols the number of columns in the layout
     */
    public ENHGridLayout(int rows, int cols) {
        this(rows, cols, 0, 0);
    }

    /**
     * Creates a grid layout with the specified rows, columns,
     * horizontal gap, and vertical gap.
     * @param rows the rows; VARIABLE (0) means 'any number.'
     * @param cols the columns; VARIABLE (0) means 'any number.'
     * Only one of 'rows' and 'cols' can be VARIABLE, not both.
     * @param hgap the horizontal gap variable
     * @param vgap the vertical gap variable
     * @exception IllegalArgumentException If the rows and columns are invalid.
     */
    public ENHGridLayout(int rows, int cols, int hgap, int vgap) {
        super(rows, cols, hgap, vgap);
        this.rows = rows;
        this.cols = cols;
        this.hgap = hgap;
        this.vgap = vgap;
    }

    /**
     * Traverses the children and determines row heights and column widths.
     * @param parent the component which needs to be laid out
     * @param min if true, the minimum size is used. Otherwise, the preferred size
     * is used.
     */
    protected void getGridSizes(Container parent, boolean useMinimum) {
        int componentCount = parent.getComponentCount();
        if (componentCount == 0) {
            return;
        }
        int rowCount = rows, columnCount = cols;
        if (rowCount > 0) {
            columnCount = (componentCount + rowCount - 1) / rowCount;
        } else {
            rowCount = (componentCount + columnCount - 1) / columnCount;
        }

        rowHeights = new int[rowCount];
        colWidths = new int[columnCount];

        for (int index = 0; index < componentCount; index++) {
            Component component = parent.getComponent(index);
            Dimension size = useMinimum ? component.getMinimumSize() :
                    component.getPreferredSize();

            int row = index / columnCount;
            if (size.height > rowHeights[row]) {
                rowHeights[row] = size.height;
            }

            int col = index % columnCount;
            if (size.width > colWidths[col]) {
                colWidths[col] = size.width;
            }
        }
    }

    /**
     * Sums the items of an array
     */
    final int sum(int[] array) {
        if (array == null) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < array.length; i++) {
            total += array[i];
        }
        return total;
    }

    /**
     * Calculates the preferred size for this layout.
     * @param parent the component which needs to be laid out
     */
    public Dimension preferredLayoutSize(Container parent) {

        Insets insets = parent.getInsets();
        getGridSizes(parent, false);
        return new Dimension(insets.left + insets.right + sum(colWidths)
                + (colWidths.length + 1) * hgap,
                insets.top + insets.bottom + sum(rowHeights)
                        + (rowHeights.length + 1) * vgap);
    }

    /**
     * Returns the minimum dimensions needed to layout the components
     * contained in the specified panel.
     * @param parent the component which needs to be laid out
     */
    public Dimension minimumLayoutSize(Container parent) {

        Insets insets = parent.getInsets();
        getGridSizes(parent, true);
        return new Dimension(insets.left + insets.right + sum(colWidths)
                + (colWidths.length + 1) * hgap,
                insets.top + insets.bottom + sum(rowHeights)
                        + (rowHeights.length + 1) * vgap);
    }

    /**
     * Positions the component.
     * @param pos the component's index in its parents child list
     * @param row col component's position
     */
    protected void setBounds(int pos, int row, int col,
                             Component component, int x, int y, int width, int height) {

        component.setBounds(x, y, width, height);

    }

    /**
     * Performs the layout of the children.
     * It calculates the number of actual rows and columns
     * based on the user's settings, retrieves row height and column
     * width information, then moves all the children to the appropriate places.
     * @param parent the specified component being laid out
     * @see #reshape
     */
    public void layoutContainer(Container parent) {
        int componentCount = parent.getComponentCount();
        if (componentCount == 0) {
            return;
        }
        Insets insets = parent.getInsets();

        getGridSizes(parent, false);
        int rowCount = rows, columnCount = cols;

        if (rowCount > 0) {
            columnCount = (componentCount + rowCount - 1) / rowCount;
        } else {
            rowCount = (componentCount + columnCount - 1) / columnCount;
        }

        Dimension parentSize = parent.getSize();
        for (int col = 0, x = insets.left + hgap; col < columnCount; col++) {
            for (int row = 0, y = insets.top + vgap; row < rowCount; row++) {
                int index = row * columnCount + col;
                if (index < componentCount) {
                    int width = Math.max(0, Math.min(colWidths[col],
                            parentSize.width - insets.right - x));
                    int height = Math.max(0, Math.min(rowHeights[row],
                            parentSize.height - insets.bottom - y));
                    setBounds(index, row, col, parent.getComponent(index), x, y, width, height);
                }
                y += rowHeights[row] + vgap;
            }
            x += colWidths[col] + hgap;
        }
    }

}
