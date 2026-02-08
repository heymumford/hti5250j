/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * TDD Red-Phase Tests for Resource Leak Detection
 *
 * These tests intentionally fail to expose the resource leak bugs.
 * The implementation should fix these by properly closing all streams.
 */
public class ResourceLeakTest {

    private File tempDir;
    private File testSettingsFile;

    @BeforeEach
    public void setUp() throws IOException {
        // Create temporary directory for test files
        tempDir = Files.createTempDirectory("tn5250j-leak-test").toFile();
        testSettingsFile = new File(tempDir, "tn5250jstartup.cfg");
    }

    @AfterEach
    public void tearDown() {
        // Cleanup test files
        if (testSettingsFile != null && testSettingsFile.exists()) {
            testSettingsFile.delete();
        }
        if (tempDir != null && tempDir.exists()) {
            tempDir.delete();
        }
    }

    /**
     * BUG 1: FileInputStream leak in GlobalConfigure.loadSettings()
     *
     * Lines 187-192 in GlobalConfigure.java:
     *   in = new FileInputStream(settingsFile);
     *   settings.load(in);
     *   // in never closed - RESOURCE LEAK
     *
     * This test demonstrates the leak pattern by showing the stream
     * is not closed in the buggy code path.
     */
    @Test
    public void testGlobalConfigureLoadSettingsStreamLeakPattern() throws IOException {
        TestGlobalConfigure configure = new TestGlobalConfigure();
        System.clearProperty("emulator.settingsDirectory");
        configure.reloadSettings();
        if (!configure.lastInput.wasClosed()) {
            fail("FileInputStream resource leak detected in GlobalConfigure.loadSettings(): stream not closed");
        }
    }

    /**
     * BUG 2: FileOutputStream leak in GlobalConfigure.saveSettings()
     *
     * Lines 301-302 in GlobalConfigure.java:
     *   FileOutputStream out = new FileOutputStream(settingsDirectory() + settingsFile);
     *   settings.store(out, "...");
     *   // out never flushed or closed - RESOURCE LEAK
     *
     * This test demonstrates the leak by showing the stream is not closed.
     */
    @Test
    public void testGlobalConfigureSaveSettingsStreamLeakPattern() throws IOException {
        TestGlobalConfigure configure = new TestGlobalConfigure();
        configure.saveSettings();
        if (!configure.lastOutput.wasClosed()) {
            fail("FileOutputStream resource leak detected in GlobalConfigure.saveSettings(): stream not closed");
        }
    }

    /**
     * BUG 3: URL stream leak in SessionConfig.loadPropertiesFromResource()
     *
     * Line 235 in SessionConfig.java:
     *   properties.load(url.openStream());
     *   // Stream never closed - RESOURCE LEAK
     *
     * This test demonstrates the leak by showing that the stream
     * from url.openStream() is never closed in the actual code.
     */
    @Test
    public void testSessionConfigLoadPropertiesFromResourceStreamLeakPattern() throws IOException {
        TestSessionConfig sessionConfig = new TestSessionConfig();
        sessionConfig.loadResourceForTest("TN5250JDefaults.props");
        if (sessionConfig.lastInput == null) {
            fail("Resource TN5250JDefaults.props not found on classpath");
        }
        if (!sessionConfig.lastInput.wasClosed()) {
            fail("URL stream resource leak detected in SessionConfig.loadPropertiesFromResource(): stream not closed");
        }
    }

    /**
     * Direct test of the actual leak pattern in GlobalConfigure.loadSettings
     * This test directly exposes Bug 1 by showing the stream is not closed.
     */
    @Test
    public void testGlobalConfigureLoadSettingsStreamNotClosed() throws IOException {
        TestGlobalConfigure configure = new TestGlobalConfigure();
        System.clearProperty("emulator.settingsDirectory");
        configure.reloadSettings();
        if (!configure.lastInput.wasClosed()) {
            fail("FileInputStream not closed in GlobalConfigure.loadSettings()");
        }
    }

    /**
     * Direct test of the actual leak pattern in GlobalConfigure.saveSettings
     * This test directly exposes Bug 2 by showing the stream is not closed.
     */
    @Test
    public void testGlobalConfigureSaveSettingsStreamNotClosed() throws IOException {
        TestGlobalConfigure configure = new TestGlobalConfigure();
        configure.saveSettings();
        if (!configure.lastOutput.wasClosed()) {
            fail("FileOutputStream not closed in GlobalConfigure.saveSettings()");
        }
    }

    private static final class CloseTrackingInputStream extends ByteArrayInputStream {
        private boolean closed;

        CloseTrackingInputStream(byte[] data) {
            super(data);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        boolean wasClosed() {
            return closed;
        }
    }

    private static final class CloseTrackingOutputStream extends ByteArrayOutputStream {
        private boolean closed;

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        boolean wasClosed() {
            return closed;
        }
    }

    private static final class TestGlobalConfigure extends GlobalConfigure {
        private CloseTrackingInputStream lastInput;
        private CloseTrackingOutputStream lastOutput;

        @Override
        protected java.io.InputStream openSettingsInputStream(String path) {
            lastInput = new CloseTrackingInputStream("test.key=test.value".getBytes());
            return lastInput;
        }

        @Override
        protected java.io.OutputStream openSettingsOutputStream(String path) {
            lastOutput = new CloseTrackingOutputStream();
            return lastOutput;
        }
    }

    private static final class TestSessionConfig extends SessionConfig {
        private CloseTrackingInputStream lastInput;

        TestSessionConfig() {
            super("TN5250JDefaults.props", "test", true);
        }

        void loadResourceForTest(String resourceName) throws IOException {
            loadPropertiesFromResource(resourceName);
        }

        @Override
        protected java.io.InputStream openResourceStream(java.net.URL url) {
            lastInput = new CloseTrackingInputStream("test.key=test.value".getBytes());
            return lastInput;
        }
    }

}
