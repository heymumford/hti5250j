/*
 * SPDX-FileCopyrightText: Copyright (c) 2001, 2002, 2003
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Kenneth J. Pouncey
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j;

import org.hti5250j.interfaces.ConfigureFactory;
import org.hti5250j.tools.logging.HTI5250jLogFactory;
import org.hti5250j.tools.logging.HTI5250jLogger;

import javax.swing.*;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.Properties;

/**
 * Utility class for referencing global settings and functions of which at most
 * one instance can exist per VM.
 * <p>
 * Use GlobalConfigure.instance() to access this instance.
 */
public class GlobalConfigure extends ConfigureFactory {

    public static final String TN5250J_FOLDER = ".tn5250j";

    /**
     * A handle to the unique GlobalConfigure class
     */
    private static GlobalConfigure _instance;

    /**
     * A handle to the the Global Properties
     */
    private static Properties settings;

    private static Hashtable registry = new Hashtable();
    private static Hashtable headers = new Hashtable();

    public static final File ses = new File(SESSIONS);

    private static final String settingsFile = "tn5250jstartup.cfg";
    private final HTI5250jLogger log = HTI5250jLogFactory.getLogger(this.getClass());

    /**
     * The constructor is made protected to allow overriding.
     */
    public GlobalConfigure() {
        if (_instance == null) {
            initialize();
            _instance = this;
        }
    }

    /**
     * @return The unique instance of this class.
     */
    public static GlobalConfigure instance() {

        if (_instance == null) {
            _instance = new GlobalConfigure();
        }
        return _instance;

    }

    /**
     * Initialize the properties registry for use later.
     */
    private void initialize() {
        verifiySettingsFolder();
        loadSettings();
        loadSessions();
        loadMacros();
        loadKeyStrokes();
    }

    /**
     * check if folder %USERPROFILE%/.tn5250j exists
     * and create if necessary
     */
    private void verifiySettingsFolder() {
        final String settingsfolder = System.getProperty("user.home") + File.separator + TN5250J_FOLDER;
        final File settingsDir = new File(settingsfolder);
        if (!settingsDir.exists()) {
            try {
                if (log.isInfoEnabled()) {
                    log.info("Settings folder '" + settingsfolder + "' doesn't exist. Will created now.");
                }
                settingsDir.mkdir();
            } catch (Exception exception) {
                if (log.isWarnEnabled()) {
                    log.warn("Couldn't create settings folder '" + settingsfolder + "'", exception);
                }
            }
        }
    }

    /**
     * Load the sessions properties
     */
    private void loadSessions() {

        setProperties(SESSIONS, SESSIONS, "------ Sessions --------", true);
    }

    /**
     * Load the macros
     */
    private void loadMacros() {

        setProperties(MACROS, MACROS, "------ Macros --------", true);

    }

    private void loadKeyStrokes() {

        setProperties(KEYMAP, KEYMAP,
                "------ Key Map key=keycode,isShiftDown,isControlDown,isAltDown,isAltGrDown --------",
                true);

    }

    /**
     * Reload the environment settings.
     */
    @Override
    public void reloadSettings() {
        if (log.isInfoEnabled()) {
            log.info("reloading settings");
        }
        loadSettings();
        loadSessions();
        loadMacros();
        loadKeyStrokes();
        if (log.isInfoEnabled()) {
            log.info("Done (reloading settings).");
        }
    }

    /**
     * Loads the emulator setting from the setting(s) file
     */
    private void loadSettings() {

        settings = new Properties();

        if (System.getProperties().containsKey("emulator.settingsDirectory")) {
            settings.setProperty("emulator.settingsDirectory",
                    System.getProperty("emulator.settingsDirectory") +
                            File.separator);
            checkDirs();
        } else {
            settings.setProperty("emulator.settingsDirectory",
                    System.getProperty("user.home") + File.separator +
                            TN5250J_FOLDER + File.separator);
            try {
                try (InputStream in = openSettingsInputStream(settingsFile)) {
                    settings.load(in);
                }
            } catch (FileNotFoundException fnfe) {
                try {
                    try (InputStream again = openSettingsInputStream(settingsDirectory() + settingsFile)) {
                        settings.load(again);
                    }
                } catch (FileNotFoundException fnfea) {
                    log.info(" Information Message: "
                            + fnfea.getMessage() + ".  The file " + settingsFile
                            + " will be created for first time use.");
                    checkLegacy();
                    saveSettings();
                } catch (IOException ioea) {
                    log.warn("IO Exception accessing File "
                            + settingsFile + " for the following reason : "
                            + ioea.getMessage());
                } catch (SecurityException sea) {
                    log.warn("Security Exception for file "
                            + settingsFile + "  This file can not be "
                            + "accessed because : " + sea.getMessage());
                }
            } catch (IOException ioe) {
                log.warn("IO Exception accessing File "
                        + settingsFile + " for the following reason : "
                        + ioe.getMessage());
            } catch (SecurityException se) {
                log.warn("Security Exception for file "
                        + settingsFile + "  This file can not be "
                        + "accessed because : " + se.getMessage());
            }
        }
    }

    private void checkDirs() {
        File sd = new File(settings.getProperty("emulator.settingsDirectory"));
        if (!sd.isDirectory()) {
            sd.mkdirs();
        }
    }

    private void checkLegacy() {
        if (ses.exists()) {
            int cfc;
            cfc = JOptionPane.showConfirmDialog(null,
                    "Dear User,\n\n" +
                            "Seems you are using an old version of tn5250j.\n" +
                            "In meanwhile the application became multi-user capable,\n" +
                            "which means ALL the config- and settings-files are\n" +
                            "placed in your home-dir to avoid further problems in\n" +
                            "the near future.\n\n" +
                            "You have the choice to choose if you want the files\n" +
                            "to be copied or not, please make your choice !\n\n" +
                            "Shall we copy the files to the new location ?",
                    "Old install detected", JOptionPane.WARNING_MESSAGE,
                    JOptionPane.YES_NO_OPTION);
            if (cfc == 0) {
                checkDirs();
                copyConfigs(SESSIONS);
                copyConfigs(MACROS);
                copyConfigs(KEYMAP);
            } else {
                JOptionPane.showMessageDialog(null,
                        "Dear User,\n\n" +
                                "You choosed not to copy the file.\n" +
                                "This means the program will end here.\n\n" +
                                "To use this NON-STANDARD behaviour start tn5250j\n" +
                                "with -Demulator.settingsDirectory=<settings-dir> \n" +
                                "as a parameter to avoid this question all the time.",
                        "Using NON-STANDARD behaviour", JOptionPane.WARNING_MESSAGE);
                System.exit(0);
            }
        }
    }

    private void copyConfigs(String sesFile) {
        String srcFile = System.getProperty("user.dir") + File.separator + sesFile;
        String dest = System.getProperty("user.home") +
                File.separator + TN5250J_FOLDER + File.separator + sesFile;
        File rmvFile = new File(sesFile);
        try {
            FileReader reader = new FileReader(srcFile);
            BufferedReader bufferedReader = new BufferedReader(reader);

            FileWriter writer = new FileWriter(dest);
            PrintWriter printWriter = new PrintWriter(writer);
            String line = bufferedReader.readLine();
            while (line != null) {
                printWriter.println(line);
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
            printWriter.close();
            rmvFile.delete();
        } catch (FileNotFoundException fileNotFoundException) {
            log.warn(srcFile + " not found !");
        } catch (IOException ioException) {
            log.warn("Global io-error !");
        } catch (ArrayIndexOutOfBoundsException arrayIndexException) {
        }
    }

    /**
     * Save the settings for the global configuration
     */
    @Override
    public void saveSettings() {

        try {
            try (OutputStream out = openSettingsOutputStream(settingsDirectory() + settingsFile)) {
                settings.store(out, "----------------- tn5250j Global Settings --------------");
            }
        } catch (FileNotFoundException fnfe) {
        } catch (IOException ioe) {
        }
    }

    /**
     * Save the setting in the registry using the key passed in with no header
     * output.
     *
     * @param regKey
     */
    @Override
    public void saveSettings(String regKey) {

        saveSettings(regKey, "");
    }

    /**
     * Save the settings in the registry using the key passed with a header
     * in the output.
     *
     * @param regKey
     * @param header
     */
    @Override
    public void saveSettings(String regKey, String header) {

        saveSettings(regKey, regKey, header);
    }

    /**
     * Save the settings in the registry using the key passed with a header
     * in the output.
     *
     * @param regKey
     * @param header
     */
    @Override
    public void saveSettings(String regKey, String fileName, String header) {

        if (registry.containsKey(regKey)) {
            try {
                try (OutputStream out = openSettingsOutputStream(settingsDirectory() + fileName)) {
                    Properties props = (Properties) registry.get(regKey);
                    props.store(out, header);
                }
            } catch (FileNotFoundException fnfe) {
                log.warn("File not found : writing file "
                        + fileName + ".  Description of error is "
                        + fnfe.getMessage());
            } catch (IOException ioe) {
                log.warn("IO Exception : writing file "
                        + fileName + ".  Description of error is "
                        + ioe.getMessage());
            } catch (SecurityException se) {
                log.warn("Security Exception : writing file "
                        + fileName + ".  Description of error is "
                        + se.getMessage());
            }

        }

    }

    protected InputStream openSettingsInputStream(String path) throws FileNotFoundException {
        return new FileInputStream(path);
    }

    protected OutputStream openSettingsOutputStream(String path) throws FileNotFoundException, SecurityException {
        // Validate path to prevent directory traversal attacks (CWE-22)
        validateSettingsPath(path);
        return new FileOutputStream(path);
    }

    /**
     * Validate that the given path is safe for file operations.
     * Ensures path does not contain ".." or escape the intended directory.
     *
     * @param path the path to validate
     * @throws SecurityException if path is invalid or escapes directory bounds
     */
    protected void validateSettingsPath(String path) throws SecurityException {
        if (path == null || path.isEmpty()) {
            throw new SecurityException("Path cannot be null or empty");
        }

        try {
            Path settingsPath = Paths.get(settingsDirectory()).normalize().toAbsolutePath();
            Path filePath = Paths.get(path).normalize().toAbsolutePath();

            // Verify path stays within settings directory
            if (!filePath.startsWith(settingsPath) && !filePath.equals(settingsPath)) {
                throw new SecurityException("Path escapes settings directory: " + path);
            }
        } catch (Exception e) {
            throw new SecurityException("Invalid path: " + path, e);
        }
    }

    /**
     * Place the Properties in the registry under a given registry name
     *
     * @param regKey
     * @param regProps
     */
    @Override
    public void setProperties(String regKey, Properties regProps) {

        registry.put(regKey, regProps);

    }

    /**
     * Set the properties for the given registry key.
     *
     * @param regKey
     * @param fileName
     * @param header
     */
    @Override
    public void setProperties(String regKey, String fileName, String header) {
        setProperties(regKey, fileName, header, false);
    }

    /**
     * Set the properties for the given registry key.
     *
     * @param regKey
     * @param fileName
     * @param header
     * @param createFile
     */
    @Override
    public void setProperties(String regKey, String fileName, String header,
                              boolean createFile) {

        FileInputStream in = null;
        Properties props = new Properties();
        headers.put(regKey, header);

        try {
            in = new FileInputStream(settingsDirectory()
                    + fileName);
            props.load(in);

        } catch (FileNotFoundException fnfe) {

            if (createFile) {
                log.info(" Information Message: " + fnfe.getMessage()
                        + ".  The file " + fileName + " will"
                        + " be created for first time use.");

                saveSettings(regKey, header);

            } else {

                log.info(" Information Message: " + fnfe.getMessage()
                        + ".");

            }
        } catch (IOException ioe) {
            log.warn("IO Exception accessing File " + fileName +
                    " for the following reason : "
                    + ioe.getMessage());
        } catch (SecurityException se) {
            log.warn("Security Exception for file " + fileName
                    + ".  This file can not be accessed because : "
                    + se.getMessage());
        }

        registry.put(regKey, props);

    }

    /**
     * Returns the properties associated with a given registry key.
     *
     * @param regKey
     * @return
     */
    @Override
    public Properties getProperties(String regKey) {

        if (registry.containsKey(regKey)) {
            return (Properties) registry.get(regKey);
        }
        return null;
    }

    public Properties getProperties() {
        return settings;
    }

    @Override
    public Properties getProperties(String regKey, String fileName) {
        return getProperties(regKey, fileName, false, "", false);
    }

    @Override
    public Properties getProperties(String regKey, String fileName,
                                    boolean createFile, String header) {
        return getProperties(regKey, fileName, false, "", false);
    }

    @Override
    public Properties getProperties(String regKey, String fileName,
                                    boolean createFile, String header,
                                    boolean reloadIfLoaded) {

        if (!registry.containsKey(regKey) || reloadIfLoaded) {

            FileInputStream in = null;
            Properties props = new Properties();
            headers.put(regKey, header);

            try {
                Path settingsPath = Paths.get(settingsDirectory()).normalize().toAbsolutePath();
                Path filePath = settingsPath.resolve(fileName).normalize();

                if (!filePath.startsWith(settingsPath)) {
                    throw new SecurityException("Path traversal attempt detected: " + fileName);
                }

                in = new FileInputStream(filePath.toFile());
                props.load(in);

            } catch (FileNotFoundException fnfe) {

                if (createFile) {
                    log.info(" Information Message: " + fnfe.getMessage()
                            + ".  The file " + fileName + " will"
                            + " be created for first time use.");

                    registry.put(regKey, props);

                    saveSettings(regKey, header);

                    return props;

                } else {

                    log.info(" Information Message: " + fnfe.getMessage()
                            + ".");

                }
            } catch (IOException ioe) {
                log.warn("IO Exception accessing File " + fileName +
                        " for the following reason : "
                        + ioe.getMessage());
            } catch (SecurityException se) {
                log.warn("Security Exception for file " + fileName
                        + ".  This file can not be accessed because : "
                        + se.getMessage());
            }

            registry.put(regKey, props);

            return props;
        } else {
            return (Properties) registry.get(regKey);
        }
    }

    /**
     * Returns the setting from the given key of the global properties or the
     * default passed if the property does not exist.
     *
     * @param key
     * @param def
     * @return
     */
    @Override
    public String getProperty(String key, String def) {
        if (settings.containsKey(key)) {
            return settings.getProperty(key);
        } else {
            return def;
        }
    }

    /**
     * Returns the setting from the given key of the global properties.
     *
     * @param key
     * @return
     */
    @Override
    public String getProperty(String key) {
        return settings.getProperty(key);
    }

    /**
     * Private helper to return the settings directory
     *
     * @return
     */
    private String settingsDirectory() {
        return settings.getProperty("emulator.settingsDirectory");

    }

    /**
     * Not sure yet so be careful using this.
     *
     * @return
     */
    public ClassLoader getClassLoader() {

        ClassLoader loader = GlobalConfigure.class.getClassLoader();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }

        return loader;
    }

}
