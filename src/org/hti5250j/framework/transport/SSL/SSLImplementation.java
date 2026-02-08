/*
 * SPDX-FileCopyrightText: Copyright (c) 2001
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: Stephen M. Kennedy
 * SPDX-FileContributor: Stephen M. Kennedy <skennedy@tenthpowertech.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.framework.transport.SSL;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Locale;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.swing.JOptionPane;

import org.hti5250j.GlobalConfigure;
import org.hti5250j.framework.transport.SSLInterface;
import org.hti5250j.tools.logging.HTI5250jLogFactory;
import org.hti5250j.tools.logging.HTI5250jLogger;

/**
 * <p>
 * This class implements the SSLInterface and is used to create SSL socket
 * instances.
 * </p>
 *
 * @author Stephen M. Kennedy <skennedy@tenthpowertech.com>
 */
public class SSLImplementation implements SSLInterface, X509TrustManager {

    SSLContext sslContext = null;

    KeyStore userks = null;
    private String userKsPath;
    private char[] userksPassword;

    KeyManagerFactory userkmf = null;

    TrustManagerFactory usertmf = null;

    TrustManager[] userTrustManagers = null;

    X509Certificate[] acceptedIssuers;

    HTI5250jLogger logger;

    public SSLImplementation() {
        logger = HTI5250jLogFactory.getLogger(getClass());
        userksPassword = resolveKeystorePassword();
    }

    public void init(String sslType) {
        try {
            if (sslType == null || sslType.trim().isEmpty()) {
                sslContext = null;
                return;
            }
            String normalizedType = sslType.trim().toUpperCase(Locale.ROOT);
            if ("SSLV3".equals(normalizedType) || "SSLV2".equals(normalizedType)) {
                logger.error("Rejected insecure SSL protocol: " + sslType);
                sslContext = null;
                return;
            }

            logger.debug("Initializing User KeyStore");
            userKsPath = System.getProperty("user.home") + File.separator
                    + GlobalConfigure.TN5250J_FOLDER + File.separator + "keystore";
            File userKsFile = new File(userKsPath);
            userks = KeyStore.getInstance(KeyStore.getDefaultType());
            boolean keystoreLoaded = false;
            try {
                if (userKsFile.exists()) {
                    try (FileInputStream input = new FileInputStream(userKsFile)) {
                        userks.load(input, userksPassword);
                    }
                } else {
                    userks.load(null, userksPassword);
                }
                keystoreLoaded = true;
            } catch (Exception loadError) {
                logger.error("Error loading keystore with configured password [" + loadError.getMessage() + "]");
            }

            if (!keystoreLoaded && userKsFile.exists()) {
                char[] legacyPassword = "changeit".toCharArray();
                try (FileInputStream input = new FileInputStream(userKsFile)) {
                    userks.load(input, legacyPassword);
                    keystoreLoaded = true;
                    char[] newPassword = generateSecurePassword();
                    try (FileOutputStream output = new FileOutputStream(userKsFile)) {
                        userks.store(output, newPassword);
                        userksPassword = newPassword;
                    } catch (Exception storeError) {
                        logger.error("Error migrating keystore password [" + storeError.getMessage() + "]");
                        userksPassword = legacyPassword;
                    }
                } catch (Exception legacyError) {
                    logger.error("Error loading keystore with legacy password [" + legacyError.getMessage() + "]");
                }
            }

            if (!keystoreLoaded) {
                try {
                    userks.load(null, userksPassword);
                    if (userKsFile.getParentFile() != null) {
                        userKsFile.getParentFile().mkdirs();
                    }
                    try (FileOutputStream output = new FileOutputStream(userKsFile)) {
                        userks.store(output, userksPassword);
                    }
                } catch (Exception storeError) {
                    logger.error("Error initializing empty keystore [" + storeError.getMessage() + "]");
                }
            }

            logger.debug("Initializing User Key Manager Factory");
            userkmf = KeyManagerFactory.getInstance(KeyManagerFactory
                    .getDefaultAlgorithm());
            userkmf.init(userks, userksPassword);
            logger.debug("Initializing User Trust Manager Factory");
            usertmf = TrustManagerFactory.getInstance(TrustManagerFactory
                    .getDefaultAlgorithm());
            usertmf.init(userks);
            userTrustManagers = usertmf.getTrustManagers();
            logger.debug("Initializing SSL Context");
            sslContext = SSLContext.getInstance(sslType);
            sslContext.init(userkmf.getKeyManagers(), new TrustManager[]{this}, null);
        } catch (Exception ex) {
            logger.error("Error initializing SSL [" + ex.getMessage() + "]");
            sslContext = null;
        }

    }

    public Socket createSSLSocket(String destination, int port) {
        if (sslContext == null) {
            throw new IllegalStateException("SSL Context Not Initialized");
        }
        if (destination == null || destination.trim().isEmpty()) {
            throw new IllegalArgumentException("Destination is required");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
        try {
            return (SSLSocket) sslContext.getSocketFactory().createSocket(destination, port);
        } catch (Exception e) {
            throw new RuntimeException("Error creating ssl socket [" + e.getMessage() + "]", e);
        }
    }

    // X509TrustManager Methods

    /*
     * (non-Javadoc)
     *
     * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
     */
    public X509Certificate[] getAcceptedIssuers() {
        return acceptedIssuers;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * javax.net.ssl.X509TrustManager#checkClientTrusted(java.security.cert.
     * X509Certificate[], java.lang.String)
     */
    public void checkClientTrusted(X509Certificate[] arg0, String arg1)
            throws CertificateException {
        throw new SecurityException("checkClientTrusted unsupported");

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * javax.net.ssl.X509TrustManager#checkServerTrusted(java.security.cert.
     * X509Certificate[], java.lang.String)
     */
    public void checkServerTrusted(X509Certificate[] chain, String type)
            throws CertificateException {
        if (chain == null || chain.length == 0) {
            throw new CertificateException("Certificate chain is empty");
        }
        if (userTrustManagers == null || userTrustManagers.length == 0) {
            throw new CertificateException("No trust managers available");
        }
        try {
            for (int i = 0; i < userTrustManagers.length; i++) {
                if (userTrustManagers[i] instanceof X509TrustManager) {
                    X509TrustManager trustManager = (X509TrustManager) userTrustManagers[i];
                    X509Certificate[] calist = trustManager
                            .getAcceptedIssuers();
                    if (calist.length > 0) {
                        trustManager.checkServerTrusted(chain, type);
                    } else {
                        throw new CertificateException(
                                "Empty list of accepted issuers (a.k.a. root CA list).");
                    }
                }
            }
            return;
        } catch (CertificateException ce) {
            X509Certificate cert = chain[0];
            String certInfo = "Version: " + cert.getVersion() + "\n";
            certInfo = certInfo.concat("Serial Number: "
                    + cert.getSerialNumber() + "\n");
            certInfo = certInfo.concat("Signature Algorithm: "
                    + cert.getSigAlgName() + "\n");
            certInfo = certInfo.concat("Issuer: "
                    + cert.getIssuerDN().getName() + "\n");
            certInfo = certInfo.concat("Valid From: " + cert.getNotBefore()
                    + "\n");
            certInfo = certInfo
                    .concat("Valid To: " + cert.getNotAfter() + "\n");
            certInfo = certInfo.concat("Subject DN: "
                    + cert.getSubjectDN().getName() + "\n");
            certInfo = certInfo.concat("Public Key: "
                    + cert.getPublicKey().getFormat() + "\n");

            int accept = JOptionPane
                    .showConfirmDialog(null, certInfo, "Unknown Certificate - Do you accept it?",
                            javax.swing.JOptionPane.YES_NO_OPTION);
            if (accept != JOptionPane.YES_OPTION) {
                throw new java.security.cert.CertificateException(
                        "Certificate Rejected");
            }

            int save = JOptionPane.showConfirmDialog(null,
                    "Remember this certificate?", "Save Certificate",
                    javax.swing.JOptionPane.YES_NO_OPTION);

            if (save == JOptionPane.YES_OPTION) {
                try {
                    userks.setCertificateEntry(cert.getSubjectDN().getName(),
                            cert);
                    userks.store(new FileOutputStream(userKsPath),
                            userksPassword);
                } catch (Exception e) {
                    logger.error("Error saving certificate [" + e.getMessage()
                            + "]");
                    e.printStackTrace();
                }
            }
        }

    }

    private char[] resolveKeystorePassword() {
        String configured = System.getProperty("hti5250j.keystore.password");
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getenv("HTI5250J_KEYSTORE_PASSWORD");
        }
        if (configured != null && !configured.trim().isEmpty()) {
            return configured.toCharArray();
        }
        return generateSecurePassword();
    }

    private char[] generateSecurePassword() {
        final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        char[] generated = new char[24];
        for (int i = 0; i < generated.length; i++) {
            generated[i] = alphabet.charAt(random.nextInt(alphabet.length()));
        }
        return generated;
    }
}
