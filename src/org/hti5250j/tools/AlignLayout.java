/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.tools;

import java.awt.Container;
import java.awt.Component;
import java.util.Hashtable;

public class AlignLayout extends ENHGridLayout {

    private static final long serialVersionUID = 1L;
    protected Hashtable alignment;
    protected Hashtable resize_width, resize_height;
    public static final int TOP = 1;
    public static final int MIDDLE = 4;
    public static final int BOTTOM = 5;

    /**
     * Creates an aligner layout with 2 columns, a variable number of rows,
     * and a gap of 5 pixels.
     */
    public AlignLayout() {
        this(2, 5, 5);
    }

    /**
     * Creates an aligner layout with the specified number of columns and gaps.
     * @param cols the number of columns (should be a multiple of 2)
     * @param hgap the horizontal gap variable
     * @param vgap the vertical gap variable
     * @exception IllegalArgumentException If the rows and columns are invalid.
     */
    public AlignLayout(int cols, int hgap, int vgap) {
        super(VARIABLE, cols, hgap, vgap);
    }

    private int get(Hashtable table, Component component, int defaultValue) {
        Object value = (table != null) ? table.get("" + component.hashCode()) : null;
        return (value != null) ? ((Integer) value).intValue() : defaultValue;
    }

    private boolean get(Hashtable table, Component component, boolean defaultValue) {
        Object value = (table != null) ? table.get("" + component.hashCode()) : null;
        return (value != null) ? ((Boolean) value).booleanValue() : defaultValue;
    }

    /**
     * Gets the vertical position of a label relative to its control.
     * @see #setLabelVerticalAlignment
     */
    public int getLabelVerticalAlignment(Component child) {
        return get(alignment, child, MIDDLE);
    }

    /**
     * Sets the vertical position of a label relative to its control.
     * @param align TOP, MIDDLE (default), or BOTTOM.
     * @exception IllegalArgumentException If an invalid value is set
     */
    public void setLabelVerticalAlignment(Component child, int align) {
        if (alignment == null) alignment = new Hashtable(5);
        alignment.put("" + child.hashCode(), Integer.valueOf(align));
    }

    /** Gets the component's RezizeWidth value.
     * @see #setResizeWidth
     */
    public boolean getResizeWidth(Component child) {
        return get(resize_width, child, false);
    }

    /** Sets whether the control should be resized horizontally to its parent's
     * right edge if it is in the last column (default: false).
     */
    public void setResizeWidth(Component child, boolean shouldResize) {
        if (resize_width == null) resize_width = new Hashtable(5);
        resize_width.put("" + child.hashCode(), Boolean.valueOf(shouldResize));
    }

    /** Gets the component's RezizeHeight value.
     * @see #setResizeHeight
     */
    public boolean getResizeHeight(Component child) {
        return get(resize_height, child, false);
    }

    /** Sets whether the control should be resized vertically to the height of the
     * largest component in its row (default: false). This value is ignored for
     * labels (the components in odd columns).
     */
    public void setResizeHeight(Component child, boolean shouldResize) {
        if (resize_height == null) resize_height = new Hashtable(5);
        resize_height.put("" + child.hashCode(), Boolean.valueOf(shouldResize));
    }

    protected boolean isLabel(int col) {
        return (col % 2) == 0;
    }

    /**
     * Positions the component.
     * @param pos the component's index in its parents child list
     * @param row col component's position
     */
    protected void setBounds(int pos, int row, int col, Component component,
                             int x, int y, int col_width, int row_height) {

        int comp_w = col_width, comp_h = row_height;

        if (isLabel(col) || !getResizeHeight(component)) {
            comp_h = component.getPreferredSize().height;
        }

        /* Resize a control to its parent's right edge if its resizeWidth value
         * is true, and it is in the last column
         */
        if (!isLabel(col)) {
            if (col < colWidths.length - 1) {
            } else if (getResizeWidth(component)) {
                Container parent = component.getParent();
                comp_w = parent.getSize().width - parent.getInsets().right - x;
            } else {
                comp_w = component.getPreferredSize().width;
            }

            component.setBounds(x, y, comp_w, comp_h);

            return;
        }

        int control_h = row_height;
        if (pos < component.getParent().getComponentCount() - 1) {
            Component control = component.getParent().getComponents()[pos + 1];
            if (control != null && !getResizeHeight(control)) {
                control_h = control.getPreferredSize().height;
            }
        }

        // Adjust label vertical position relative to paired control component
        // MIDDLE: centered between control bounds; BOTTOM: aligned to bottom
        y += switch (getLabelVerticalAlignment(component)) {
            case MIDDLE -> (control_h - comp_h) / 2;  // Center label vertically
            case BOTTOM -> control_h - comp_h;        // Bottom-align label
            default -> 0;                              // TOP (default): no adjustment
        };
        component.setBounds(x, y, comp_w, comp_h);
    }

}
