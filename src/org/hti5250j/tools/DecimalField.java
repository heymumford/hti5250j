/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.tools;

import javax.swing.*;

import java.awt.Toolkit;
import java.text.*;

public class DecimalField extends JTextField {
    private static final long serialVersionUID = 1L;
    private NumberFormat numberFormat;

    public DecimalField(double value, int columns, NumberFormat numberFormat) {
        super(columns);
        setDocument(new FormattedDocument(numberFormat));
        this.numberFormat = numberFormat;
        setValue(value);
    }

    public double getValue() {
        double retVal = 0.0;

        try {
            retVal = numberFormat.parse(getText()).doubleValue();
        } catch (ParseException parseException) {
            // This should never happen because insertString allows
            // only properly formatted data to get in the field.
            Toolkit.getDefaultToolkit().beep();
            System.err.println("getValue: could not parse: " + getText());
        }
        return retVal;
    }

    public void setValue(double value) {
        setText(numberFormat.format(value));
    }
}
