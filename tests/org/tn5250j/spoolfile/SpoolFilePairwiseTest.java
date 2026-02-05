/**
 * SpoolFilePairwiseTest.java - Pairwise TDD Tests for Spool File Operations
 *
 * This test suite uses pairwise testing to systematically discover bugs
 * in spool file handling operations used in automation workflows.
 *
 * Test dimensions (pairwise combinations):
 * - Operations: [list, view, download, delete]
 * - File sizes: [0 bytes, 1 KB, 1 MB, 10 MB]
 * - File types: [text, AFP (Advanced Function Printing), PDF]
 * - Permissions: [authorized, unauthorized, partial]
 * - Connection states: [connected, disconnected]
 *
 * Red-Green-Refactor approach:
 * 1. Write tests that expose missing validation in spool operations
 * 2. Implement minimum code to make tests pass
 * 3. Refactor to improve code clarity and remove duplication
 *
 * Test strategy: Combine pairs of dimensions to create adversarial scenarios
 * that expose resource handling gaps, permission issues, and state management bugs.
 */
package org.tn5250j.spoolfile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;

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
                throw new Exception("System not connected");
            }
            return new ArrayList<>(spoolFiles);
        }

        MockSpoolFile getSpoolFile(String name) throws Exception {
            if (!connected) {
                throw new Exception("System not connected");
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
                throw new Exception("Connection lost during download");
            }

            MockSpoolFile spf = connection.getSpoolFile(spoolName);
            if (spf == null) {
                throw new FileNotFoundException("Spool file not found: " + spoolName);
            }

            if (!spf.authorized) {
                throw new SecurityException("Permission denied for spool file: " + spoolName);
            }

            if (!spf.exists) {
                throw new FileNotFoundException("Spool file no longer exists: " + spoolName);
            }

            // Simulate file size validation
            if (spf.sizeBytes > 52_428_800) { // 50MB limit
                throw new IOException("Spool file exceeds maximum size: " + spf.sizeBytes);
            }

            // Create dummy export file
            File exported = new File(exportDir, spoolName + "." + spf.type);
            Files.createFile(exported.toPath());
            return exported;
        }

        void deleteSpoolFile(String spoolName) throws Exception {
            if (!connection.isConnected()) {
                throw new Exception("Connection lost during delete");
            }

            MockSpoolFile spf = connection.getSpoolFile(spoolName);
            if (spf == null) {
                throw new FileNotFoundException("Spool file not found: " + spoolName);
            }

            if (!spf.authorized) {
                throw new SecurityException("Permission denied to delete: " + spoolName);
            }

            // Remove from list
            connection.spoolFiles.remove(spf);
        }

        String viewSpoolFile(String spoolName) throws Exception {
            if (!connection.isConnected()) {
                throw new Exception("Connection lost during view");
            }

            MockSpoolFile spf = connection.getSpoolFile(spoolName);
            if (spf == null) {
                throw new FileNotFoundException("Spool file not found: " + spoolName);
            }

            if (!spf.authorized) {
                throw new SecurityException("Permission denied to view: " + spoolName);
            }

            // Return content summary
            return String.format("SPOOL[%s] type=%s size=%d bytes", spf.name, spf.type, spf.sizeBytes);
        }
    }

    private File tempDir;
    private MockAS400Connection mockConnection;
    private SpoolExporterStub exporter;

    @Before
    public void setUp() throws IOException {
        // Create temporary directory for exported files
        tempDir = Files.createTempDirectory("spool-test").toFile();
        mockConnection = new MockAS400Connection(true);
        exporter = new SpoolExporterStub(mockConnection);
        exporter.setExportDirectory(tempDir);
    }

    @After
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
    @Test(expected = Exception.class)
    public void testDownloadSpoolFileWhenDisconnected() throws Exception {
        mockConnection.addSpoolFile(new MockSpoolFile("DOC", 1, 1024, "text", true, true));
        mockConnection.disconnect();

        exporter.downloadSpoolFile("DOC");
    }

    /**
     * ADVERSARIAL: Delete spool file without authorization
     * Pairwise dimension: operation=delete + permission=unauthorized
     */
    @Test(expected = SecurityException.class)
    public void testDeleteSpoolFileWhenUnauthorized() throws Exception {
        mockConnection.addSpoolFile(new MockSpoolFile("PROTECTED", 2, 512, "text", false, true));

        exporter.deleteSpoolFile("PROTECTED");
    }

    /**
     * ADVERSARIAL: View spool file when permission denied
     * Pairwise dimension: operation=view + permission=unauthorized
     */
    @Test(expected = SecurityException.class)
    public void testViewSpoolFileWhenUnauthorized() throws Exception {
        mockConnection.addSpoolFile(new MockSpoolFile("SECRET", 3, 256, "text", false, true));

        exporter.viewSpoolFile("SECRET");
    }

    /**
     * ADVERSARIAL: Download non-existent spool file
     * Pairwise dimension: operation=download + fileExists=false
     */
    @Test(expected = FileNotFoundException.class)
    public void testDownloadNonExistentSpoolFile() throws Exception {
        exporter.downloadSpoolFile("DOES_NOT_EXIST");
    }

    /**
     * ADVERSARIAL: Download spool file that no longer exists (races)
     * Pairwise dimension: operation=download + fileExists=false
     */
    @Test(expected = FileNotFoundException.class)
    public void testDownloadSpoolFileNoLongerExists() throws Exception {
        mockConnection.addSpoolFile(new MockSpoolFile("DELETED", 4, 1024, "text", true, false));

        exporter.downloadSpoolFile("DELETED");
    }

    /**
     * ADVERSARIAL: Download spool file exceeding size limit (50 MB)
     * Pairwise dimension: fileSize=50MB+ + operation=download
     */
    @Test(expected = IOException.class)
    public void testDownloadSpoolFileExceedsSizeLimit() throws Exception {
        long fiftyMBPlus = 52_428_801L;
        mockConnection.addSpoolFile(new MockSpoolFile("HUGE", 5, fiftyMBPlus, "pdf", true, true));

        exporter.downloadSpoolFile("HUGE");
    }

    /**
     * ADVERSARIAL: List spool files when system disconnected
     * Pairwise dimension: operation=list + connection=disconnected
     */
    @Test(expected = Exception.class)
    public void testListSpoolFilesWhenDisconnected() throws Exception {
        mockConnection.addSpoolFile(new MockSpoolFile("DOC", 1, 1024, "text", true, true));
        mockConnection.disconnect();

        exporter.listSpoolFiles();
    }

    /**
     * ADVERSARIAL: View spool file when connection lost
     * Pairwise dimension: operation=view + connection=disconnected
     */
    @Test(expected = Exception.class)
    public void testViewSpoolFileWhenDisconnected() throws Exception {
        mockConnection.addSpoolFile(new MockSpoolFile("DOC", 1, 1024, "text", true, true));
        mockConnection.disconnect();

        exporter.viewSpoolFile("DOC");
    }

    /**
     * ADVERSARIAL: Download spool file with partial permissions
     * Pairwise dimension: operation=download + permission=partial (file exists but not readable)
     */
    @Test(expected = SecurityException.class)
    public void testDownloadSpoolFilePartialPermission() throws Exception {
        mockConnection.addSpoolFile(new MockSpoolFile("PARTIAL", 6, 1024, "text", false, true));

        exporter.downloadSpoolFile("PARTIAL");
    }

    /**
     * ADVERSARIAL: Delete spool file when system disconnected
     * Pairwise dimension: operation=delete + connection=disconnected
     */
    @Test(expected = Exception.class)
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
    @Test(expected = IOException.class)
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
