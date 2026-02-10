/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.hti5250j.event.SessionChangeEvent;
import org.hti5250j.event.SessionListener;
import org.hti5250j.framework.common.SessionManager;
import org.hti5250j.framework.tn5250.Screen5250;
import org.hti5250j.framework.tn5250.tnvt;
import org.hti5250j.gui.SystemRequestDialog;
import org.hti5250j.interfaces.ScanListener;
import org.hti5250j.interfaces.SessionInterface;
import org.hti5250j.workflow.ScreenProvider;

/**
 * A host session
 */
public class Session5250 implements SessionInterface, ScreenProvider {

    private String configurationResource;
    private String sessionName;
    private int sessionType;
    protected Properties sesProps;
    private boolean heartBeat;
    private final String propFileName;
    private final SessionConfig sesConfig;
    private tnvt vt;
    private final Screen5250 screen;
    private SessionPanel guiComponent;

    private List<SessionListener> sessionListeners = null;
    private final ReadWriteLock sessionListenerLock = new ReentrantReadWriteLock();

    private boolean scan; // = false;
    private List<ScanListener> scanListeners = null;
    private final ReadWriteLock scanListenerLock = new ReentrantReadWriteLock();

    public Session5250(Properties props, String configurationResource,
                       String sessionName,
                       SessionConfig config) {

        propFileName = config.getConfigurationResource();

        sesConfig = config;
        this.configurationResource = configurationResource;
        this.sessionName = sessionName;
        sesProps = props;

        if (sesProps.containsKey(HTI5250jConstants.SESSION_HEART_BEAT))
            heartBeat = true;

        screen = new Screen5250();

        //screen.setVT(vt);

    }

    @Override
    public String getConfigurationResource() {

        return configurationResource;

    }

    public SessionConfig getConfiguration() {

        return sesConfig;
    }

    public SessionManager getSessionManager() {
        return SessionManager.instance();
    }

    @Override
    public boolean isConnected() {
        if (vt == null) {
            return false;
        }
        return vt.isConnected();

    }

    /**
     * @return true when SSL is used and socket is connected.
     * @see {@link tnvt#isSslSocket()}
     */
    public boolean isSslSocket() {
        if (this.vt != null) {
            return this.vt.isSslSocket();
        } else {
            return false;
        }
    }

    /**
     * @return true when SSL is configured but not necessary in use
     * @see {@link #isSslSocket()}
     */
    public boolean isSslConfigured() {
        if (sesProps.get(HTI5250jConstants.SSL_TYPE) != null) {
            final String sslType = (String) sesProps.get(HTI5250jConstants.SSL_TYPE);
            if (!HTI5250jConstants.SSL_TYPE_NONE.equals(sslType)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSendKeepAlive() {
        return heartBeat;
    }

    /**
     * @return true if configured, that the host name should be
     */
    public boolean isUseSystemName() {
        return sesProps.getProperty(HTI5250jConstants.SESSION_TERM_NAME_SYSTEM) != null;
    }

    public Properties getConnectionProperties() {
        return sesProps;
    }

    public void setGUI(SessionPanel gui) {
        guiComponent = gui;
    }

    public SessionPanel getGUI() {
        return guiComponent;
    }

    @Override
    public String getSessionName() {
        return sessionName;
    }

    public String getAllocDeviceName() {
        if (vt != null) {
            return vt.getAllocatedDeviceName();
        }
        return null;
    }

    @Override
    public int getSessionType() {

        return sessionType;

    }

    public String getHostName() {
        return vt.getHostName();
    }

    public Screen5250 getScreen() {

        return screen;

    }

    @Override
    public void signalBell() {
        Toolkit.getDefaultToolkit().beep();
    }

    /* (non-Javadoc)
     * @see org.hti5250j.interfaces.SessionInterface#displaySystemRequest()
     */
    @Override
    public String showSystemRequest() {
        // In headless mode, guiComponent may be null
        if (guiComponent == null) {
            return null;
        }
        final SystemRequestDialog sysreqdlg = new SystemRequestDialog(this.guiComponent);
        return sysreqdlg.show();
    }

    @Override
    public void connect() {

        String proxyPort = "1080"; // default socks proxy port
        boolean enhanced = false;
        boolean support132 = false;
        int port = 23; // default telnet port

        enhanced = sesProps.containsKey(HTI5250jConstants.SESSION_TN_ENHANCED);

        if (sesProps.containsKey(HTI5250jConstants.SESSION_SCREEN_SIZE))
            if ((sesProps.getProperty(HTI5250jConstants.SESSION_SCREEN_SIZE)).equals(HTI5250jConstants.SCREEN_SIZE_27X132_STR))
                support132 = true;

        final tnvt vt = new tnvt(this, screen, enhanced, support132);
        setVT(vt);

        //      vt.setController(this);

        if (sesProps.containsKey(HTI5250jConstants.SESSION_PROXY_PORT))
            proxyPort = sesProps.getProperty(HTI5250jConstants.SESSION_PROXY_PORT);

        if (sesProps.containsKey(HTI5250jConstants.SESSION_PROXY_HOST))
            vt.setProxy(sesProps.getProperty(HTI5250jConstants.SESSION_PROXY_HOST),
                    proxyPort);

        final String sslType;
        if (sesProps.containsKey(HTI5250jConstants.SSL_TYPE)) {
            sslType = sesProps.getProperty(HTI5250jConstants.SSL_TYPE);
        } else {
            // set default to none
            sslType = HTI5250jConstants.SSL_TYPE_NONE;
        }
        vt.setSSLType(sslType);

        if (sesProps.containsKey(HTI5250jConstants.SESSION_CODE_PAGE))
            vt.setCodePage(sesProps.getProperty(HTI5250jConstants.SESSION_CODE_PAGE));

        if (sesProps.containsKey(HTI5250jConstants.SESSION_DEVICE_NAME))
            vt.setDeviceName(sesProps.getProperty(HTI5250jConstants.SESSION_DEVICE_NAME));

        if (sesProps.containsKey(HTI5250jConstants.SESSION_HOST_PORT)) {
            port = Integer.parseInt(sesProps.getProperty(HTI5250jConstants.SESSION_HOST_PORT));
        } else {
            // set to default 23 of telnet
            port = 23;
        }

        final String ses = sesProps.getProperty(HTI5250jConstants.SESSION_HOST);
        final int portp = port;

        // lets set this puppy up to connect within its own thread
        Runnable connectIt = new Runnable() {
            @Override
            public void run() {
                vt.connect(ses, portp);
            }

        };

        // now lets set it to connect within its own daemon thread
        //    this seems to work better and is more responsive than using
        //    swingutilities's invokelater
        Thread ct = new Thread(connectIt);
        ct.setDaemon(true);
        ct.start();

    }

    @Override
    public void disconnect() {
        vt.disconnect();
    }

    // WVL - LDC : TR.000300 : Callback scenario from 5250
    protected void setVT(tnvt v) {
        vt = v;
        screen.setVT(vt);
        if (vt != null)
            vt.setScanningEnabled(this.scan);
    }

    public tnvt getVT() {
        return vt;
    }

    // WVL - LDC : TR.000300 : Callback scenario from 5250

    /**
     * Enables or disables scanning.
     *
     * @param scan enables scanning when true; disables otherwise.
     * @see tnvt#setCommandScanning(boolean);
     * @see tnvt#isCommandScanning();
     * @see tnvt#scan();
     * @see tnvt#parseCommand();
     * @see scanned(String,String)
     */
    public void setScanningEnabled(boolean scan) {
        this.scan = scan;

        if (this.vt != null)
            this.vt.setScanningEnabled(scan);
    }

    // WVL - LDC : TR.000300 : Callback scenario from 5250

    /**
     * Checks whether scanning is enabled.
     *
     * @return true if command scanning is enabled; false otherwise.
     * @see tnvt#setCommandScanning(boolean);
     * @see tnvt#isCommandScanning();
     * @see tnvt#scan();
     * @see tnvt#parseCommand();
     * @see scanned(String,String)
     */
    public boolean isScanningEnabled() {
        if (this.vt != null)
            return this.vt.isScanningEnabled();

        return this.scan;
    }

    // WVL - LDC : TR.000300 : Callback scenario from 5250

    /**
     * This is the callback method for the TNVT when sensing the action cmd
     * screen pattern (!# at position 0,0).
     * <p>
     * This is a thread safe method and will be called
     * from the TNVT read thread!
     *
     * @param command   discovered in the 5250 stream.
     * @param remainder are all the other characters on the screen.
     * @see tnvt#setCommandScanning(boolean);
     * @see tnvt#isCommandScanning();
     * @see tnvt#scan();
     * @see tnvt#parseCommand();
     * @see scanned(String,String)
     */
    public final void fireScanned(String command, String remainder) {
        scanListenerLock.readLock().lock();
        try {
            if (this.scanListeners != null) {
                for (ScanListener listener : this.scanListeners) {
                    listener.scanned(command, remainder);
                }
            }
        } finally {
            scanListenerLock.readLock().unlock();
        }
    }

    /**
     * @param listener
     */
    public final void addScanListener(ScanListener listener) {
        scanListenerLock.writeLock().lock();
        try {
            if (scanListeners == null) {
                scanListeners = new ArrayList<ScanListener>(3);
            }
            scanListeners.add(listener);
        } finally {
            scanListenerLock.writeLock().unlock();
        }
    }

    /**
     * @param listener
     */
    public final void removeScanListener(ScanListener listener) {
        scanListenerLock.writeLock().lock();
        try {
            if (scanListeners != null) {
                scanListeners.remove(listener);
            }
        } finally {
            scanListenerLock.writeLock().unlock();
        }
    }

    /**
     * Notify all registered listeners of the onSessionChanged event.
     *
     * @param state The state change property object.
     */
    public final void fireSessionChanged(int state) {
        sessionListenerLock.readLock().lock();
        try {
            if (this.sessionListeners != null) {
                for (SessionListener listener : this.sessionListeners) {
                    SessionChangeEvent sce = new SessionChangeEvent(this);
                    sce.setState(state);
                    listener.onSessionChanged(sce);
                }
            }
        } finally {
            sessionListenerLock.readLock().unlock();
        }
    }

    /**
     * Add a SessionListener to the listener list.
     *
     * @param listener The SessionListener to be added
     */
    @Override
    public final void addSessionListener(SessionListener listener) {
        sessionListenerLock.writeLock().lock();
        try {
            if (sessionListeners == null) {
                sessionListeners = new ArrayList<SessionListener>(3);
            }
            sessionListeners.add(listener);
        } finally {
            sessionListenerLock.writeLock().unlock();
        }
    }

    /**
     * Remove a SessionListener from the listener list.
     *
     * @param listener The SessionListener to be removed
     */
    @Override
    public final void removeSessionListener(SessionListener listener) {
        sessionListenerLock.writeLock().lock();
        try {
            if (sessionListeners != null) {
                sessionListeners.remove(listener);
            }
        } finally {
            sessionListenerLock.writeLock().unlock();
        }
    }

}
