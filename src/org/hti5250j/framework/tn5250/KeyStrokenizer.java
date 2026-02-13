/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.framework.tn5250;

import org.hti5250j.tools.logging.HTI5250jLogFactory;
import org.hti5250j.tools.logging.HTI5250jLogger;


public class KeyStrokenizer {

    private StringBuffer keyStrokes;
    private StringBuffer sb;
    private int index;
    private int length;

    private final HTI5250jLogger log = HTI5250jLogFactory.getLogger(this.getClass());

    public KeyStrokenizer() {

        sb = new StringBuffer();
        setKeyStrokes(null);
    }

    public void setKeyStrokes(String strokes) {

        if (strokes != null) {
            keyStrokes.setLength(0);
            log.debug("set " + keyStrokes);
            length = strokes.length();
        } else {

            keyStrokes = new StringBuffer();
            length = 0;

        }
        keyStrokes.append(strokes);
        index = 0;

    }

    public boolean hasMoreKeyStrokes() {
        return length > index;
    }

    public String nextKeyStroke() {

        String s = "";
        boolean gotOne = false;
        if (length > index) {
            sb.setLength(0);

            char c = keyStrokes.charAt(index);
            switch (c) {
                case '[':
                    sb.append(c);
                    index++;

                    // we need to throw an error here
                    if (index >= length) {
                        log.warn(" mnemonic key was incomplete :1 " +
                                "at position " + index + " len " + length);
                    } else {
                        c = keyStrokes.charAt(index);

                        if (c == '[') {
                            index++;
                        } else {
                            while (!gotOne) {

                                if (c == ']') { // did we find an ending
                                    sb.append(c);
                                    index++;
                                    gotOne = true;
                                } else {
                                    sb.append(c);
                                    index++;
                                    // we need to throw an error here because we did not
                                    //   find an ending for the potential mnemonic
                                    if (index >= length) {
                                        log.warn(
                                                " mnemonic key was incomplete ending not found :2 " +
                                                        "at position " + index);
                                    }
                                    c = keyStrokes.charAt(index);
                                }
                            }
                        }
                    }
                    break;

                case ']':
                    index++;
                    if (index >= length) {
                        log.warn(
                                " mnemonic key was incomplete ending not found :3 " +
                                        "at position " + index);
                        sb.append(c);
                        index++;

                    } else {
                        c = keyStrokes.charAt(index);
                        if (c == ']') {
                            sb.append(c);
                            index++;
                        } else {
                            log.warn(
                                    " mnemonic key was incomplete beginning not found :4 " +
                                            "at position " + index);
                        }
                    }
                    break;
                default:
                    sb.append(c);
                    index++;
                    break;
            }
            if (sb != null) {
                s = new String(sb);
            }

        }
        log.debug("next " + keyStrokes);

        return s;
    }

    public String getUnprocessedKeyStroked() {
        if (index >= length) {
            return null;
        }
        return keyStrokes.substring(index);
    }

}
