/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j;

import java.awt.Dimension;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.JApplet;
import javax.swing.SwingUtilities;

import org.hti5250j.framework.common.SessionManager;
import org.hti5250j.gui.HTI5250jSecurityAccessDialog;
import org.hti5250j.tools.LangTool;
import org.hti5250j.tools.logging.HTI5250jLogFactory;
import org.hti5250j.tools.logging.HTI5250jLogger;

//import org.hti5250j.swing.JTerminal;

/**
 * Legacy JApplet-based interface. Deprecated: Java applets were removed in Java 9.
 * This class exists for historical compatibility only. HTI5250j is headless;
 * new development should use headless APIs or Swing components directly.
 *
 * @deprecated JApplet has been removed from modern Java. Use SessionManager directly.
 */
@Deprecated(since = "0.8.0", forRemoval = true)
public class My5250Applet extends JApplet {

    private static final long serialVersionUID = 1L;

    boolean isStandalone = true;
    private SessionManager manager;

    private HTI5250jLogger log;

    /**
     * Get a parameter value
     */
    public String getParameter(String key, String def) {

        return isStandalone ? System.getProperty(key, def) :
                (getParameter(key) != null ? getParameter(key) : def);
    }

    /**
     * Construct the applet
     */
    public My5250Applet() {

    }

    /**
     * Initialize the applet
     */
    public void init() {
        try {
            jbInit();
        } catch (Exception exception) {
            if (log == null)
                System.out.println(exception.getMessage());
            else
                log.warn("In constructor: ", exception);
        }
    }

    /**
     * Component initialization
     */
    private void jbInit() throws Exception {
        this.setSize(new Dimension(400, 300));

        if (isSpecified("-L"))
            LangTool.init(parseLocale(getParameter("-L")));
        else
            LangTool.init();

        //Let's check some permissions
        try {
            System.getProperty(".java.policy");
        } catch (SecurityException securityException) {
            securityException.printStackTrace();
            HTI5250jSecurityAccessDialog.showErrorMessage(securityException);
            return;
        }
        log = HTI5250jLogFactory.getLogger(this.getClass());

        Properties sesProps = new Properties();
        log.info(" We have loaded a new one");

        // Start loading properties - Host must exist
        sesProps.put(HTI5250jConstants.SESSION_HOST, getParameter("host"));

        if (isSpecified("-e"))
            sesProps.put(HTI5250jConstants.SESSION_TN_ENHANCED, "1");

        if (isSpecified("-p")) {
            sesProps.put(HTI5250jConstants.SESSION_HOST_PORT, getParameter("-p"));
        }

//      if (isSpecified("-f",args))
//         propFileName = getParm("-f",args);

        if (isSpecified("-cp"))
            sesProps.put(HTI5250jConstants.SESSION_CODE_PAGE, getParameter("-cp"));

        if (isSpecified("-gui"))
            sesProps.put(HTI5250jConstants.SESSION_USE_GUI, "1");

        if (isSpecified("-t"))
            sesProps.put(HTI5250jConstants.SESSION_TERM_NAME_SYSTEM, "1");

        if (isSpecified("-132"))
            sesProps.put(HTI5250jConstants.SESSION_SCREEN_SIZE, HTI5250jConstants.SCREEN_SIZE_27X132_STR);
        else
            sesProps.put(HTI5250jConstants.SESSION_SCREEN_SIZE, HTI5250jConstants.SCREEN_SIZE_24X80_STR);

        // socks proxy host argument
        if (isSpecified("-sph")) {
            sesProps.put(HTI5250jConstants.SESSION_PROXY_HOST, getParameter("-sph"));
        }

        // socks proxy port argument
        if (isSpecified("-spp"))
            sesProps.put(HTI5250jConstants.SESSION_PROXY_PORT, getParameter("-spp"));

        // check if device name is specified
        if (isSpecified("-dn"))
            sesProps.put(HTI5250jConstants.SESSION_DEVICE_NAME, getParameter("-dn"));
        // are we to use a ssl and if we are what type

        if (isSpecified("-sslType")) {

            sesProps.put(HTI5250jConstants.SSL_TYPE, getParameter("-sslType"));
        }

        loadSystemProperty("SESSION_CONNECT_USER");
        loadSystemProperty("SESSION_CONNECT_PASSWORD");
        loadSystemProperty("SESSION_CONNECT_PROGRAM");
        loadSystemProperty("SESSION_CONNECT_LIBRARY");
        loadSystemProperty("SESSION_CONNECT_MENU");

        manager = SessionManager.instance();
        final Session5250 session = manager.openSession(sesProps, "", "Test Applet");
        final SessionPanel sessionPanel = new SessionPanel(session);
//      final JTerminal jt = new JTerminal(s);

        this.getContentPane().add(sessionPanel);

        session.connect();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
//            jt.grabFocus();
                sessionPanel.grabFocus();
            }
        });

    }

    private void loadSystemProperty(String param) {

        if (isSpecified(param))
            System.getProperties().put(param, getParameter(param));

    }

    /**
     * Get Applet information
     */
    public String getAppletInfo() {
        return "tn5250j - " + HTI5250jConstants.VERSION_INFO + " - Java tn5250 Client";
    }

    /**
     * Get parameter info
     */
    public String[][] getParameterInfo() {
        return null;
    }

    /**
     * Tests if a parameter was specified or not.
     */
    private boolean isSpecified(String parameterName) {

        if (getParameter(parameterName) != null) {
            log.info("Parameter " + parameterName + " is specified as: " + getParameter(parameterName));
            return true;
        }
        return false;
    }

    /**
     * Returns a local specified by the string localString
     */
    protected static Locale parseLocale(String localeString) {
        int index = 0;
        String[] localeParts = {"", "", ""};
        StringTokenizer tokenizer = new StringTokenizer(localeString, "_");
        while (tokenizer.hasMoreTokens()) {
            localeParts[index++] = tokenizer.nextToken();
        }
        return new Locale(localeParts[0], localeParts[1], localeParts[2]);
    }

    //static initializer for setting look & feel
    static {
        try {
            //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            //UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception exception) {
        }
    }
}
