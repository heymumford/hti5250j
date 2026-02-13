/*
 * SPDX-FileCopyrightText: Copyright (c) 2001, 2002, 2003
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j;

import org.hti5250j.interfaces.ConfigureFactory;
import org.hti5250j.interfaces.OptionAccessFactory;
import org.hti5250j.keyboard.KeyMnemonicResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Utility class for referencing the global options allowed for access
 * of which at most one instance can exist per VM.
 * <p>
 * Use OptionAccessFactory.instance() to access this instance.
 */
public class OptionAccess extends OptionAccessFactory {

    /**
     * A handle to the unique OptionAccess class
     */
    private static OptionAccess _instance;

    /**
     * A handle to non valid options.
     */
    private static List<String> restricted = new ArrayList<String>();

    private final KeyMnemonicResolver keyMnemonicResolver = new KeyMnemonicResolver();

    /**
     * The constructor is made protected to allow overriding.
     */
    public OptionAccess() {
        if (_instance == null) {
            // initialize the settings information
            initialize();
            // set our instance to this one.
            _instance = this;
        }
    }

    /**
     * @return The unique instance of this class.
     */
    public static OptionAccess instance() {

        if (_instance == null) {
            _instance = new OptionAccess();
        }
        return _instance;

    }

    /**
     * Initialize the properties registry for use later.
     */
    private void initialize() {

        loadOptions();
    }

    /**
     * Load a list of available options
     */
    private void loadOptions() {

        restricted.clear();
        String restrictedProp =
                ConfigureFactory.getInstance().getProperties(
                        ConfigureFactory.SESSIONS).getProperty("emul.restricted");

        if (restrictedProp != null) {
            StringTokenizer tokenizer = new StringTokenizer(restrictedProp, ";");
            while (tokenizer.hasMoreTokens()) {
                restricted.add(tokenizer.nextToken());
            }
        }

    }

    public boolean isValidOption(String option) {

        return !restricted.contains(option);
    }

    public boolean isRestrictedOption(String option) {

        return restricted.contains(option);
    }

    public int getNumberOfRestrictedOptions() {

        return restricted.size();
    }

    public void reload() {
        loadOptions();
    }
}
