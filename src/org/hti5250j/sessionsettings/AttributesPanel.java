/*
 * SPDX-FileCopyrightText: Copyright (c) 2001,2002
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.sessionsettings;

import org.hti5250j.SessionConfig;
import org.hti5250j.tools.LangTool;
import org.hti5250j.tools.logging.HTI5250jLogFactory;
import org.hti5250j.tools.logging.HTI5250jLogger;

import javax.swing.*;
import java.awt.*;

/**
 * Base class for all attribute panels
 */
abstract class AttributesPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String nodePrefix = "sa.node";

    private final HTI5250jLogger log = HTI5250jLogFactory.getLogger(this.getClass());

    private String name;
    SessionConfig changes = null;
    // content pane to be used if needed by subclasses
    JPanel contentPane;

    AttributesPanel(SessionConfig config) {
        this(config, "", nodePrefix);
    }

    AttributesPanel(SessionConfig config, String name) {
        this(config, name, nodePrefix);
    }

    AttributesPanel(SessionConfig config, String name, String prefix) {
        super();
        changes = config;
        this.name = LangTool.getString(prefix + name);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        try {
            initPanel();
        } catch (Exception e) {
            log.error(e);
        }
    }

    public abstract void initPanel() throws Exception;

    public abstract void applyAttributes();

    protected final String getStringProperty(String prop) {

        if (changes.isPropertyExists(prop)) {
            return changes.getStringProperty(prop);
        } else {
            return "";
        }

    }

    protected final String getStringProperty(String prop, String defaultValue) {

        if (changes.isPropertyExists(prop)) {
            String p = changes.getStringProperty(prop);
            if (p.length() > 0) {
                return p;
            } else {
                return defaultValue;
            }
        } else {
            return defaultValue;
        }

    }

    protected final Color getColorProperty(String prop) {

        if (changes.isPropertyExists(prop)) {
            Color c = new Color(changes.getIntegerProperty(prop));
            return c;
        } else {
            return null;
        }

    }

    protected Color getColorProperty(String prop, Color defColor) {

        if (changes.isPropertyExists(prop)) {
            Color c = new Color(changes.getIntegerProperty(prop));
            return c;
        } else {
            return defColor;
        }

    }

    protected final boolean getBooleanProperty(String prop, boolean dflt) {

        if (changes.isPropertyExists(prop)) {
            String b = changes.getStringProperty(prop).toLowerCase();
            return b.equals("yes") || b.equals("true");
        } else {
            return dflt;
        }

    }

    protected Rectangle getRectangleProperty(String key) {
        return changes.getRectangleProperty(key);
    }

    protected void setRectangleProperty(String key, Rectangle rect) {
        changes.setRectangleProperty(key, rect);
    }

    protected final void setProperty(String key, String val) {
        changes.setProperty(key, val);
    }

    public String toString() {
        return name;
    }

}
