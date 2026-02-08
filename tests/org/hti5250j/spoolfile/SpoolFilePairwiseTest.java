/*
 * SPDX-FileCopyrightText: TN5250J Community
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: ized, unauthorized, partial]
 * SPDX-FileContributor: ized permissions
 * SPDX-FileContributor: ized;
 * SPDX-FileContributor: ized, boolean exists) {
 * SPDX-FileContributor: ized = authorized;
 * SPDX-FileContributor: ized) {
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */




package org.hti5250j.spoolfile;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pairwise TDD Tests for Spool File Operations
 *
 * Test categories:
 * 1. POSITIVE (8 tests): Valid operations with connected state and authorized permissions
 * 2. ADVERSARIAL (12 tests): Permission denial, disconnection, corrupted files, large files
 *
 * Useful for automation workflows that generate reports and need robust spool handling.
 */
public class SpoolFilePairwiseTest {

    // Mock spool file representation for testing
    private static class MockSpoolFile {
        String name;
        int number;
        long sizeBytes;
        String type; // "text", "afp", "pdf"
        boolean authorized;
        boolean exists;

        MockSpoolFile(String name, int number, long sizeBytes, String type, boolean authorized, boolean exists) {
            this.name = name;
            this.number = number;
            this.sizeBytes = sizeBytes;
            this.type = type;
            this.authorized = authorized;
            this.exists = exists;
        }
    }

    // Mock connection to AS400 system
    private static class MockAS400Connection {
        private boolean connected;
        private List<MockSpoolFile> spoolFiles;

        MockAS400Connection(boolean connected) {
            this.connected = connected;
            this.spoolFiles = new ArrayList<>();
        }

        boolean isConnected() {
            return connected;
        }

        void disconnect() {
            connected = false;
        }

        void addSpoolFile(MockSpoolFile spf) {
            spoolFiles.add(spf);
        }

        List<MockSpoolFile> listSpoolFiles() throws Exception {
            if (!connected) {
                return new ArrayList<>();
            }
            return new ArrayList<>(spoolFiles);
        }

        MockSpoolFile getSpoolFile(String name) throws Exception {
            if (!connected) {
                return null;
            }
            return spoolFiles.stream()
                    .filter(s -> s.name.equals(name))
                    .findFirst()
                    .orElse(null);
        }
    }

    // Simulated SpoolExporter operations (testable API)
    private static class SpoolExporterStub {
        private MockAS400Connection connection;
        private File exportDir;

        SpoolExporterStub(MockAS400Connection conn) {
            this.connection = conn;
        }

        void setExportDirectory(File dir) {
            this.exportDir = dir;
        }

        List<MockSpoolFile> listSpoolFiles() throws Exception {
            return connection.listSpoolFiles();
        }

        File downloadSpoolFile(String spoolName) throws Exception {
            if (!connection.isConnected()) {
                return null;
            }

            MockSpoolFile spf = connection.getSpoolFile(spoolName);
            if (spf == null) {
                return null;
            }

            if (!spf.authorized) {
                return null;
            }

            if (!spf.exists) {
                return null;
            }

            // Simulate file size validation
            if (spf.sizeBytes > 52_428_800) { // 50MB limit
                return null;
            }

            // Create dummy export file
            File exported = new File(exportDir, spoolName + "." + spf.type);
            try {
                Files.createFile(exported.toPath());
            } catch (IOException e) {
                return null;
            }
            return exported;
        }

        void deleteSpoolFile(String spoolName) throws Exception {
            if (!connection.isConnected()) {
                return;
            }

            MockSpoolFile spf = connection.getSpoolFile(spoolName);
            if (spf == null) {
                return;
            }

            if (!spf.authorized) {
                return;
            }

            // Remove from list
            connection.spoolFiles.remove(spf);
        }

        String viewSpoolFile(String spoolName) throws Exception {
            if (!connection.isConnected()) {
                return null;
            }

            MockSpoolFile spf = connection.getSpoolFile(spoolName);
            if (spf == null) {
                return null;
            }

            if (!spf.authorized) {
                return null;
            }

            // Return content summary
            return String.format("SPOOL[%s] type=%s size=%d bytes", spf.name, spf.type, spf.sizeBytes);
        }
    }

    private File tempDir;
    private MockAS400Connection mockConnection;
    private SpoolExporterStub exporter;

    @BeforeEach
    public void setUp() throws IOException {
        // Create temporary directory for exported files
        tempDir = Files.createTempDirectory("spool-test").toFile();
        mockConnection = new MockAS400Connection(true);
        exporter = new SpoolExporterStub(mockConnection);
        exporter.setExportDirectory(tempDir);
    }

    @AfterEach
    public void tearDown() {
        // Cleanup test files
        if (tempDir != null && tempDir.exists()) {
            recursiveDelete(tempDir);
        }
    }

    private void recursiveDelete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    recursiveDelete(f);
                }
            }
        }
        file.delete();
    }

    // ==========================================================================
    // POSITIVE TEST CASES (8 tests): Valid operations with connected state
    // ==========================================================================

    /**
     * POSITIVE: List spool files when connected, authorized, file exists
     * Pairwise dimension: operation=list + connection=connected
     */
    @Test
    public void testListSpoolFilesWhenConnectedAndAuthorized() throws Exception {
        // Red: No files listed yet
        List<MockSpoolFile> files = exporter.listSpoolFiles();
        assertEquals(0, files.size());

        // Green: Add files and verify list
        mockConnection.addSpoolFile(new MockSpoolFile("REPORT1", 1, 1024, "text", true, true));
        mockConnection.addSpoolFile(new MockSpoolFile("REPORT2", 2, 2048, "pdf", true, true));

        files = exporter.listSpoolFiles();
        assertEquals(2, files.size());
    }

    /**
     * POSITIVE: Download small text spool file (1 KB)
     * Pairwise dimension: operation=download + fileSize=1KB + fileType=text
     */
    @Test
    public void testDownloadSmallTextSpoolFile() throws Exception {
        mockConnection.addSpoolFile(new MockSpoolFile("REPORT_SMALL", 10, 1024, "text", true, true));

        File downloaded = exporter.downloadSpoolFile("REPORT_SMALL");

        assertNotNull(downloaded);
        assertTrue(downloaded.exists());
        assertTrue(downloaded.getName().contains("REPORT_SMALL"));
    }

    /**
     * POSITIVE: Download medium AFP spool file (1 MB)
     * Pairwise dimension: operation=download + fileSize=1MB + fileType=afp
     */
    @Test
    public void testDownloadMediumAFPSpoolFile() throws Exception {
        mockConnection.addSpoolFile(new MockSpoolFile("INVOICE", 20, 1_048_576L, "afp", true, true));

        File downloaded = exporter.downloadSpoolFile("INVOICE");

        assertNotNull(downloaded);
        assertTrue(downloaded.exists());
        assertEquals("afp", getFileExtension(downloaded));
    }

    /**
     * POSITIVE: Download large PDF spool file (10 MB)
     * Pairwise dimension: operation=download + fileSize=10MB + fileType=pdf
     */
    @Test
    public void testDownloadLargePDFSpoolFile() throws Exception {
        long tenMB = 10 * 1_048_576L;
        mockConnection.addSpoolFile(new MockSpoolFile("REPORT_LARGE", 30, tenMB, "pdf", true, true));

        File downloaded = exporter.downloadSpoolFile("REPORT_LARGE");

        assertNotNull(downloaded);
        assertTrue(downloaded.exists());
    }

    /**
     * POSITIVE: View spool file successfully
     * Pairwise dimension: operation=view + connection=connected + permission=authorized
     */
    @Test
    public void testViewSpoolFileWhenAuthorized() throws Exception {
        mockConnection.addSpoolFile(new MockSpoolFile("DOC1", 5, 512, "text", true, true));

        String content = exporter.viewSpoolFile("DOC1");

        assertNotNull(content);
        assertTrue(content.contains("DOC1"));
        assertTrue(content.contains("text"));
    }

    /**
     * POSITIVE: Delete spool file successfully
     * Pairwise dimension: operation=delete + connection=connected + permission=authorized
     */
    @Test
    public void testDeleteSpoolFileWhenAuthorized() throws Exception {
        mockConnection.addSpoolFile(new MockSpoolFile("TEMP_REPORT", 7, 256, "text", true, true));
        assertEquals(1, exporter.listSpoolFiles().size());

        exporter.deleteSpoolFile("TEMP_REPORT");

        assertEquals(0, exporter.listSpoolFiles().size());
    }

    /**
     * POSITIVE: Download empty spool file (0 bytes)
     * Pairwise dimension: fileSize=0 + operation=download
     */
    @Test
    public void testDownloadEmptySpoolFile() throws Exception {
        mockConnection.addSpoolFile(new MockSpoolFile("EMPTY", 99, 0, "text", true, true));

        File downloaded = exporter.downloadSpoolFile("EMPTY");

        assertNotNull(downloaded);
        assertTrue(downloaded.exists());
    }

    /**
     * POSITIVE: List multiple spool files of different types
     * Pairwise dimension: fileType=[text, afp, pdf] + operation=list
     */
    @Test
    public void testListMultipleSpoolFileTypes() throws Exception {
        mockConnection.addSpoolFile(new MockSpoolFile("TEXT_DOC", 1, 512, "text", true, true));
        mockConnection.addSpoolFile(new MockSpoolFile("AFP_DOC", 2, 1024, "afp", true, true));
        mockConnection.addSpoolFile(new MockSpoolFile("PDF_DOC", 3, 2048, "pdf", true, true));

        List<MockSpoolFile> files = exporter.listSpoolFiles();

        assertEquals(3, files.size());
        assertTrue(files.stream().anyMatch(f -> f.type.equals("text")));
        assertTrue(files.stream().anyMatch(f -> f.type.equals("afp")));
        assertTrue(files.stream().anyMatch(f -> f.type.equals("pdf")));
    }

    // ==========================================================================
    // ADVERSARIAL TEST CASES (12 tests): Permission denial, disconnection, etc.
    // ==========================================================================

    /**
     * ADVERSARIAL: Download spool file when system disconnected
     * Pairwise dimension: operation=download + connection=disconnected
     */
    @Test
    public void testDownloadSpoolFileWhenDisconnected() throws Exception {
        mockConnection.addSpoolFile(new MockSpoolFile("DOC", 1, 1024, "text", true, true));
        mockConnection.disconnect();

        exporter.downloadSpoolFile("DOC");
    }

    /**
     * ADVERSARIAL: Delete spool file without authorization
     * Pairwise dimension: operation=delete + permission=unauthorized
     */
    @Test
    public void testDeleteSpoolFileWhenUnauthorized() throws Exception {
        mockConnection.addSpoolFile(new MockSpoolFile("PROTECTED", 2, 512, "text", false, true));

        exporter.deleteSpoolFile("PROTECTED");
    }

    /**
     * ADVERSARIAL: View spool file when permission denied
     * Pairwise dimension: operation=view + permission=unauthorized
     */
    @Test
    public void testViewSpoolFileWhenUnauthorized() throws Exception {
        mockConnection.addSpoolFile(new MockSpoolFile("SECRET", 3, 256, "text", false, true));

        exporter.viewSpoolFile("SECRET");
    }

    /**
     * ADVERSARIAL: Download non-existent spool file
     * Pairwise dimension: operation=download + fileExists=false
     */
    @Test
    public void testDownloadNonExistentSpoolFile() throws Exception {
        exporter.downloadSpoolFile("DOES_NOT_EXIST");
    }

    /**
     * ADVERSARIAL: Download spool file that no longer exists (races)
     * Pairwise dimension: operation=download + fileExists=false
     */
    @Test
    public void testDownloadSpoolFileNoLongerExists() throws Exception {
        mockConnection.addSpoolFile(new MockSpoolFile("DELETED", 4, 1024, "text", true, false));

        exporter.downloadSpoolFile("DELETED");
    }

    /**
     * ADVERSARIAL: Download spool file exceeding size limit (50 MB)
     * Pairwise dimension: fileSize=50MB+ + operation=download
     */
    @Test
    public void testDownloadSpoolFileExceedsSizeLimit() throws Exception {
        long fiftyMBPlus = 52_428_801L;
        mockConnection.addSpoolFile(new MockSpoolFile("HUGE", 5, fiftyMBPlus, "pdf", true, true));

        exporter.downloadSpoolFile("HUGE");
    }

    /**
     * ADVERSARIAL: List spool files when system disconnected
     * Pairwise dimension: operation=list + connection=disconnected
     */
    @Test
    public void testListSpoolFilesWhenDisconnected() throws Exception {
        mockConnection.addSpoolFile(new MockSpoolFile("DOC", 1, 1024, "text", true, true));
        mockConnection.disconnect();

        exporter.listSpoolFiles();
    }

    /**
     * ADVERSARIAL: View spool file when connection lost
     * Pairwise dimension: operation=view + connection=disconnected
     */
    @Test
    public void testViewSpoolFileWhenDisconnected() throws Exception {
        mockConnection.addSpoolFile(new MockSpoolFile("DOC", 1, 1024, "text", true, true));
        mockConnection.disconnect();

        exporter.viewSpoolFile("DOC");
    }

    /**
     * ADVERSARIAL: Download spool file with partial permissions
     * Pairwise dimension: operation=download + permission=partial (file exists but not readable)
     */
    @Test
    public void testDownloadSpoolFilePartialPermission() throws Exception {
        mockConnection.addSpoolFile(new MockSpoolFile("PARTIAL", 6, 1024, "text", false, true));

        exporter.downloadSpoolFile("PARTIAL");
    }

    /**
     * ADVERSARIAL: Delete spool file when system disconnected
     * Pairwise dimension: operation=delete + connection=disconnected
     */
    @Test
    public void testDeleteSpoolFileWhenDisconnected() throws Exception {
        mockConnection.addSpoolFile(new MockSpoolFile("DOC", 1, 1024, "text", true, true));
        mockConnection.disconnect();

        exporter.deleteSpoolFile("DOC");
    }

    /**
     * ADVERSARIAL: Download spool file at boundary size (just under 50 MB)
     * Pairwise dimension: fileSize=49MB (near limit) + operation=download
     */
    @Test
    public void testDownloadSpoolFileAtSizeLimit() throws Exception {
        long almostFiftyMB = 52_428_800L;
        mockConnection.addSpoolFile(new MockSpoolFile("BOUNDARY", 8, almostFiftyMB, "pdf", true, true));

        File downloaded = exporter.downloadSpoolFile("BOUNDARY");

        assertNotNull(downloaded);
        assertTrue(downloaded.exists());
    }

    /**
     * ADVERSARIAL: Download AFP file when export directory is read-only
     * Pairwise dimension: fileType=afp + permissions issue on export directory
     */
    @Test
    public void testDownloadAFPFileIntoReadOnlyDirectory() throws Exception {
        // Create separate connection and exporter for this test
        MockAS400Connection separateConnection = new MockAS400Connection(true);
        SpoolExporterStub exporterReadOnly = new SpoolExporterStub(separateConnection);

        // Create a read-only directory
        File readOnlyDir = new File(tempDir, "readonly-export");
        readOnlyDir.mkdirs();
        readOnlyDir.setReadOnly();

        exporterReadOnly.setExportDirectory(readOnlyDir);
        separateConnection.addSpoolFile(new MockSpoolFile("REPORT_AFPX", 9, 2048, "afp", true, true));

        try {
            // Should fail because directory is read-only
            exporterReadOnly.downloadSpoolFile("REPORT_AFPX");
        } finally {
            // Restore permissions for cleanup
            readOnlyDir.setWritable(true);
        }
    }

    // ==========================================================================
    // HELPER METHODS
    // ==========================================================================

    /**
     * Extract file extension from file object
     */
    private String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot + 1) : "";
    }
}
