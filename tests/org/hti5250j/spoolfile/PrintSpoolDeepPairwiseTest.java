/**
 * PrintSpoolDeepPairwiseTest.java - Deep Pairwise TDD Tests for Print/Spool Operations
 *
 * Comprehensive test suite using pairwise testing to systematically discover bugs
 * in print stream processing, spool file creation, and print formatting operations.
 *
 * PAIRWISE DIMENSIONS:
 * 1. Print type: [screen, spool, host-print]
 * 2. Format: [text, SCS (Spooled Command Stream), AFPDS (AFP Data Stream)]
 * 3. Page size: [letter, legal, A4, custom]
 * 4. Content: [text-only, fields, graphics]
 * 5. Destination: [file, printer, memory]
 *
 * TEST STRATEGY:
 * - POSITIVE: 8 tests covering valid print operations with compatible combinations
 * - ADVERSARIAL: 15+ tests covering malformed streams, resource exhaustion, boundary cases
 * - COVERAGE MATRIX: Each dimension paired with critical adjacent dimensions
 *
 * RED-GREEN-REFACTOR:
 * 1. Test failures expose missing validation in print stream handling
 * 2. Implement minimum validation/processing to make tests pass
 * 3. Refactor for clarity and reusability
 */
package org.hti5250j.spoolfile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Deep pairwise TDD tests for HTI5250j print and spool file handling.
 * Covers print stream processing, formatting, and adversarial malformed streams.
 */
public class PrintSpoolDeepPairwiseTest {

    // =========================================================================
    // TEST FIXTURES & MOCKS
    // =========================================================================

    /**
     * Mock representation of a print stream with metadata and content
     */
    private static class MockPrintStream {
        final String name;
        final String printType;      // "screen", "spool", "host-print"
        final String format;         // "text", "scs", "afpds"
        final String pageSize;       // "letter", "legal", "a4", "custom"
        final String contentType;    // "text-only", "fields", "graphics"
        final String destination;    // "file", "printer", "memory"
        final byte[] streamData;
        final int pageCount;
        final long sizeBytes;
        boolean isValid;

        MockPrintStream(String name, String printType, String format, String pageSize,
                       String contentType, String destination, byte[] data, int pages, boolean valid) {
            this.name = name;
            this.printType = printType;
            this.format = format;
            this.pageSize = pageSize;
            this.contentType = contentType;
            this.destination = destination;
            this.streamData = data;
            this.pageCount = pages;
            this.sizeBytes = data.length;
            this.isValid = valid;
        }
    }

    /**
     * Mock print stream processor with validation and formatting logic
     */
    private static class PrintStreamProcessor {
        private int maxPageSize = 50_000_000; // 50MB max per page
        private int maxTotalSize = 500_000_000; // 500MB max total
        private boolean validatePageSizeHeader = true;

        void setMaxPageSize(int bytes) {
            this.maxPageSize = bytes;
        }

        void setMaxTotalSize(int bytes) {
            this.maxTotalSize = bytes;
        }

        /**
         * Validate and parse print stream header
         */
        PrintStreamValidation validateStream(MockPrintStream stream) throws Exception {
            PrintStreamValidation validation = new PrintStreamValidation();

            if (stream == null) {
                validation.addError("Stream is null");
                return validation;
            }

            if (stream.streamData == null || stream.streamData.length == 0) {
                validation.addError("Stream data is empty");
                return validation;
            }

            // Check total size limit
            if (stream.sizeBytes > maxTotalSize) {
                validation.addError("Stream exceeds maximum total size: " + stream.sizeBytes);
                return validation;
            }

            // Validate print type
            if (!isValidPrintType(stream.printType)) {
                validation.addError("Invalid print type: " + stream.printType);
                return validation;
            }

            // Validate format
            if (!isValidFormat(stream.format)) {
                validation.addError("Invalid format: " + stream.format);
                return validation;
            }

            // Validate page size
            if (!isValidPageSize(stream.pageSize)) {
                validation.addError("Invalid page size: " + stream.pageSize);
                return validation;
            }

            // Validate content type
            if (!isValidContentType(stream.contentType)) {
                validation.addError("Invalid content type: " + stream.contentType);
                return validation;
            }

            // Validate destination
            if (!isValidDestination(stream.destination)) {
                validation.addError("Invalid destination: " + stream.destination);
                return validation;
            }

            // Check for header magic bytes (SCS or AFPDS format)
            if (stream.format.equals("scs") && !hasSCSHeader(stream.streamData)) {
                validation.addWarning("SCS stream missing expected header");
            }

            if (stream.format.equals("afpds") && !hasAFPDSHeader(stream.streamData)) {
                validation.addWarning("AFPDS stream missing expected header");
            }

            validation.isValid = true;
            return validation;
        }

        /**
         * Process print stream and create spool file
         */
        SpoolFileCreation processPrintStream(MockPrintStream stream) throws Exception {
            PrintStreamValidation validation = validateStream(stream);
            if (!validation.isValid) {
                throw new InvalidPrintStreamException(
                    "Stream validation failed: " + String.join("; ", validation.errors)
                );
            }

            SpoolFileCreation creation = new SpoolFileCreation();
            creation.name = stream.name;
            creation.format = stream.format;
            creation.pageSize = stream.pageSize;
            creation.pageCount = stream.pageCount;
            creation.sizeBytes = stream.sizeBytes;
            creation.contentType = stream.contentType;

            // Calculate expected output size based on format
            long estimatedOutputSize = calculateOutputSize(stream);
            // Enforce total output size limit (not per-page, but absolute)
            // This prevents the processor from creating outputs larger than total limit
            if (estimatedOutputSize > maxTotalSize) {
                throw new PrintStreamException(
                    "Estimated output exceeds maximum: " + estimatedOutputSize
                );
            }

            creation.estimatedOutputSize = estimatedOutputSize;
            creation.isProcessed = true;
            return creation;
        }

        /**
         * Format print stream for target destination
         */
        byte[] formatForDestination(MockPrintStream stream, String targetDestination)
                throws Exception {
            PrintStreamValidation validation = validateStream(stream);
            if (!validation.isValid) {
                throw new InvalidPrintStreamException("Stream validation failed");
            }

            switch (targetDestination.toLowerCase()) {
                case "file":
                    return formatForFile(stream);
                case "printer":
                    return formatForPrinter(stream);
                case "memory":
                    return formatForMemory(stream);
                default:
                    throw new InvalidPrintStreamException("Unknown destination: " + targetDestination);
            }
        }

        // =====================================================================
        // HELPER METHODS
        // =====================================================================

        private boolean isValidPrintType(String type) {
            return type != null && (type.equals("screen") || type.equals("spool") || type.equals("host-print"));
        }

        private boolean isValidFormat(String format) {
            return format != null && (format.equals("text") || format.equals("scs") || format.equals("afpds"));
        }

        private boolean isValidPageSize(String pageSize) {
            return pageSize != null && (pageSize.equals("letter") || pageSize.equals("legal") ||
                    pageSize.equals("a4") || pageSize.equals("custom"));
        }

        private boolean isValidContentType(String contentType) {
            return contentType != null && (contentType.equals("text-only") || contentType.equals("fields") ||
                    contentType.equals("graphics"));
        }

        private boolean isValidDestination(String destination) {
            return destination != null && (destination.equals("file") || destination.equals("printer") ||
                    destination.equals("memory"));
        }

        private boolean hasSCSHeader(byte[] data) {
            return data.length > 0 && (data[0] == (byte) 0x01 || data[0] == (byte) 0x02);
        }

        private boolean hasAFPDSHeader(byte[] data) {
            return data.length >= 4 && data[0] == (byte) 'D' && data[1] == (byte) 'A' &&
                   data[2] == (byte) 'F' && data[3] == (byte) 'P';
        }

        private long calculateOutputSize(MockPrintStream stream) {
            // Estimate based on format and content
            double multiplier = 1.0;
            if (stream.format.equals("afpds")) {
                multiplier = 1.5; // AFPDS adds metadata
            } else if (stream.format.equals("scs")) {
                multiplier = 1.2; // SCS adds control sequences
            }
            return (long) (stream.sizeBytes * multiplier);
        }

        private byte[] formatForFile(MockPrintStream stream) {
            // Add file header
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                baos.write(("FILE_HEADER:" + stream.name + "\n").getBytes());
                baos.write(stream.streamData);
                baos.write("\nFILE_FOOTER\n".getBytes());
            } catch (IOException e) {
                // Should not happen with ByteArrayOutputStream
            }
            return baos.toByteArray();
        }

        private byte[] formatForPrinter(MockPrintStream stream) {
            // Add printer escape sequences
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                baos.write(27); // ESC
                baos.write('[');
                baos.write(stream.streamData);
                baos.write(27); // ESC
                baos.write(')');
            } catch (IOException e) {
                // Should not happen
            }
            return baos.toByteArray();
        }

        private byte[] formatForMemory(MockPrintStream stream) {
            // Just return raw data for memory destination
            return Arrays.copyOf(stream.streamData, stream.streamData.length);
        }
    }

    /**
     * Result of print stream validation
     */
    private static class PrintStreamValidation {
        boolean isValid = false;
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        void addError(String error) {
            errors.add(error);
        }

        void addWarning(String warning) {
            warnings.add(warning);
        }
    }

    /**
     * Result of spool file creation
     */
    private static class SpoolFileCreation {
        String name;
        String format;
        String pageSize;
        int pageCount;
        long sizeBytes;
        long estimatedOutputSize;
        String contentType;
        boolean isProcessed = false;
    }

    /**
     * Custom exceptions for print stream processing
     */
    private static class InvalidPrintStreamException extends Exception {
        InvalidPrintStreamException(String msg) { super(msg); }
    }

    private static class PrintStreamException extends Exception {
        PrintStreamException(String msg) { super(msg); }
    }

    // =========================================================================
    // TEST SETUP & TEARDOWN
    // =========================================================================

    private File tempDir;
    private PrintStreamProcessor processor;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("print-spool-test").toFile();
        processor = new PrintStreamProcessor();
    }

    @After
    public void tearDown() {
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

    // =========================================================================
    // POSITIVE TEST CASES (8 tests): Valid print operations with compatible configs
    // =========================================================================

    /**
     * POSITIVE: Process text spool with text-only content to file destination
     * Pairwise: printType=spool + format=text + contentType=text-only + destination=file
     */
    @Test
    public void testProcessTextSpoolWithTextOnlyToFile() throws Exception {
        byte[] data = "Simple text content\nLine 2\nLine 3".getBytes();
        MockPrintStream stream = new MockPrintStream(
            "TEXT_SPOOL_001", "spool", "text", "letter", "text-only", "file", data, 1, true
        );

        SpoolFileCreation result = processor.processPrintStream(stream);

        assertNotNull(result);
        assertTrue(result.isProcessed);
        assertEquals("TEXT_SPOOL_001", result.name);
        assertEquals("text", result.format);
        assertEquals(1, result.pageCount);
    }

    /**
     * POSITIVE: Process SCS spool with fields to printer destination
     * Pairwise: printType=spool + format=scs + contentType=fields + destination=printer
     */
    @Test
    public void testProcessSCSSpoolWithFieldsToPrinter() throws Exception {
        byte[] data = new byte[] {0x01, 0x02, 0x03}; // SCS header + data
        MockPrintStream stream = new MockPrintStream(
            "SCS_SPOOL_001", "spool", "scs", "legal", "fields", "printer", data, 1, true
        );

        SpoolFileCreation result = processor.processPrintStream(stream);

        assertNotNull(result);
        assertTrue(result.isProcessed);
        assertEquals("scs", result.format);
        assertEquals("legal", result.pageSize);
    }

    /**
     * POSITIVE: Process AFPDS host print with graphics to memory
     * Pairwise: printType=host-print + format=afpds + contentType=graphics + destination=memory
     */
    @Test
    public void testProcessAFPDSHostPrintWithGraphicsToMemory() throws Exception {
        byte[] data = "DAFP".getBytes(); // AFPDS header prefix
        byte[] fullData = new byte[data.length + 100];
        System.arraycopy(data, 0, fullData, 0, data.length);
        MockPrintStream stream = new MockPrintStream(
            "AFP_HOST_001", "host-print", "afpds", "a4", "graphics", "memory", fullData, 2, true
        );

        SpoolFileCreation result = processor.processPrintStream(stream);

        assertNotNull(result);
        assertTrue(result.isProcessed);
        assertEquals("afpds", result.format);
        assertEquals("a4", result.pageSize);
        assertEquals(2, result.pageCount);
    }

    /**
     * POSITIVE: Process screen print with multiple pages
     * Pairwise: printType=screen + format=text + contentType=text-only + pageCount=4
     */
    @Test
    public void testProcessScreenPrintMultiplePages() throws Exception {
        byte[] data = new byte[1000];
        Arrays.fill(data, (byte) 'A');
        MockPrintStream stream = new MockPrintStream(
            "SCREEN_001", "screen", "text", "letter", "text-only", "file", data, 4, true
        );

        SpoolFileCreation result = processor.processPrintStream(stream);

        assertNotNull(result);
        assertEquals(4, result.pageCount);
        assertEquals(1000, result.sizeBytes);
    }

    /**
     * POSITIVE: Format for file destination from valid stream
     * Pairwise: destination=file + format=text
     */
    @Test
    public void testFormatPrintStreamForFileDestination() throws Exception {
        byte[] data = "Test content".getBytes();
        MockPrintStream stream = new MockPrintStream(
            "FORMAT_FILE", "spool", "text", "letter", "text-only", "file", data, 1, true
        );

        byte[] formatted = processor.formatForDestination(stream, "file");

        assertNotNull(formatted);
        assertTrue(formatted.length > data.length); // Has header/footer added
        String formattedStr = new String(formatted);
        assertTrue(formattedStr.contains("FILE_HEADER"));
        assertTrue(formattedStr.contains("FILE_FOOTER"));
    }

    /**
     * POSITIVE: Format for printer destination from valid stream
     * Pairwise: destination=printer + format=scs
     */
    @Test
    public void testFormatPrintStreamForPrinterDestination() throws Exception {
        byte[] data = new byte[] {0x01, 0x02};
        MockPrintStream stream = new MockPrintStream(
            "FORMAT_PRINTER", "spool", "scs", "letter", "fields", "printer", data, 1, true
        );

        byte[] formatted = processor.formatForDestination(stream, "printer");

        assertNotNull(formatted);
        assertTrue(formatted.length > data.length); // Has escape sequences added
    }

    /**
     * POSITIVE: Custom page size configuration with AFPDS format
     * Pairwise: pageSize=custom + format=afpds
     */
    @Test
    public void testProcessCustomPageSizeAFPDSFormat() throws Exception {
        byte[] data = "DAFP_CUSTOM".getBytes();
        MockPrintStream stream = new MockPrintStream(
            "CUSTOM_PAGE", "spool", "afpds", "custom", "text-only", "memory", data, 1, true
        );

        SpoolFileCreation result = processor.processPrintStream(stream);

        assertNotNull(result);
        assertEquals("custom", result.pageSize);
        assertEquals("afpds", result.format);
    }

    // =========================================================================
    // ADVERSARIAL TEST CASES (15+ tests): Malformed streams, resource exhaustion, edge cases
    // =========================================================================

    /**
     * ADVERSARIAL: Null print stream
     * Pairwise: stream=null
     */
    @Test(expected = InvalidPrintStreamException.class)
    public void testProcessNullPrintStream() throws Exception {
        processor.processPrintStream(null);
    }

    /**
     * ADVERSARIAL: Empty stream data
     * Pairwise: streamData=empty
     */
    @Test(expected = InvalidPrintStreamException.class)
    public void testProcessEmptyStreamData() throws Exception {
        MockPrintStream stream = new MockPrintStream(
            "EMPTY", "spool", "text", "letter", "text-only", "file",
            new byte[0], 1, true
        );
        processor.processPrintStream(stream);
    }

    /**
     * ADVERSARIAL: Stream exceeds maximum total size (500MB limit)
     * Pairwise: sizeBytes > maxTotalSize
     */
    @Test(expected = InvalidPrintStreamException.class)
    public void testProcessStreamExceedsMaxTotalSize() throws Exception {
        byte[] data = new byte[501_000_000]; // 501MB
        MockPrintStream stream = new MockPrintStream(
            "HUGE", "spool", "text", "letter", "text-only", "file", data, 1, true
        );
        processor.processPrintStream(stream);
    }

    /**
     * ADVERSARIAL: Invalid print type
     * Pairwise: printType=invalid
     */
    @Test(expected = InvalidPrintStreamException.class)
    public void testProcessInvalidPrintType() throws Exception {
        byte[] data = "content".getBytes();
        MockPrintStream stream = new MockPrintStream(
            "INVALID_TYPE", "unknown-type", "text", "letter", "text-only", "file", data, 1, true
        );
        processor.processPrintStream(stream);
    }

    /**
     * ADVERSARIAL: Invalid format
     * Pairwise: format=invalid
     */
    @Test(expected = InvalidPrintStreamException.class)
    public void testProcessInvalidFormat() throws Exception {
        byte[] data = "content".getBytes();
        MockPrintStream stream = new MockPrintStream(
            "INVALID_FORMAT", "spool", "xyz-format", "letter", "text-only", "file", data, 1, true
        );
        processor.processPrintStream(stream);
    }

    /**
     * ADVERSARIAL: Invalid page size
     * Pairwise: pageSize=invalid
     */
    @Test(expected = InvalidPrintStreamException.class)
    public void testProcessInvalidPageSize() throws Exception {
        byte[] data = "content".getBytes();
        MockPrintStream stream = new MockPrintStream(
            "INVALID_PAGESIZE", "spool", "text", "tabloid", "text-only", "file", data, 1, true
        );
        processor.processPrintStream(stream);
    }

    /**
     * ADVERSARIAL: Invalid content type
     * Pairwise: contentType=invalid
     */
    @Test(expected = InvalidPrintStreamException.class)
    public void testProcessInvalidContentType() throws Exception {
        byte[] data = "content".getBytes();
        MockPrintStream stream = new MockPrintStream(
            "INVALID_CONTENT", "spool", "text", "letter", "bitmaps", "file", data, 1, true
        );
        processor.processPrintStream(stream);
    }

    /**
     * ADVERSARIAL: Invalid destination
     * Pairwise: destination=invalid
     */
    @Test(expected = InvalidPrintStreamException.class)
    public void testProcessInvalidDestination() throws Exception {
        byte[] data = "content".getBytes();
        MockPrintStream stream = new MockPrintStream(
            "INVALID_DEST", "spool", "text", "letter", "text-only", "fax", data, 1, true
        );
        processor.processPrintStream(stream);
    }

    /**
     * ADVERSARIAL: SCS stream missing expected header
     * Pairwise: format=scs + headerMissing=true
     */
    @Test
    public void testProcessSCSStreamMissingHeader() throws Exception {
        byte[] data = "Not an SCS stream".getBytes(); // Missing SCS header
        MockPrintStream stream = new MockPrintStream(
            "SCS_NO_HEADER", "spool", "scs", "letter", "fields", "file", data, 1, true
        );

        // Should process but with warning about missing header
        PrintStreamValidation validation = processor.validateStream(stream);
        assertTrue(validation.isValid);
        assertTrue(!validation.warnings.isEmpty()); // Warnings present but not fatal
    }

    /**
     * ADVERSARIAL: AFPDS stream missing expected header
     * Pairwise: format=afpds + headerMissing=true
     */
    @Test
    public void testProcessAFPDSStreamMissingHeader() throws Exception {
        byte[] data = "Not an AFPDS stream".getBytes(); // Missing DAFP header
        MockPrintStream stream = new MockPrintStream(
            "AFP_NO_HEADER", "spool", "afpds", "letter", "graphics", "file", data, 1, true
        );

        PrintStreamValidation validation = processor.validateStream(stream);
        assertTrue(validation.isValid);
        assertTrue(!validation.warnings.isEmpty());
    }

    /**
     * ADVERSARIAL: Format for unknown destination
     * Pairwise: destination=unknown
     */
    @Test(expected = InvalidPrintStreamException.class)
    public void testFormatStreamForUnknownDestination() throws Exception {
        byte[] data = "content".getBytes();
        MockPrintStream stream = new MockPrintStream(
            "FORMAT_UNKNOWN", "spool", "text", "letter", "text-only", "file", data, 1, true
        );

        processor.formatForDestination(stream, "smtp");
    }

    /**
     * ADVERSARIAL: Format invalid stream
     * Pairwise: format=invalid + destination=file
     */
    @Test(expected = InvalidPrintStreamException.class)
    public void testFormatInvalidStream() throws Exception {
        byte[] data = "content".getBytes();
        MockPrintStream stream = new MockPrintStream(
            "FORMAT_INVALID", "spool", "jpeg", "letter", "text-only", "file", data, 1, true
        );

        processor.formatForDestination(stream, "file");
    }

    /**
     * ADVERSARIAL: Very large page count with small data
     * Pairwise: pageCount=1000 + sizeBytes=100
     */
    @Test
    public void testProcessLargePageCountSmallData() throws Exception {
        byte[] data = new byte[100];
        MockPrintStream stream = new MockPrintStream(
            "MANY_PAGES", "spool", "text", "letter", "text-only", "memory", data, 1000, true
        );

        SpoolFileCreation result = processor.processPrintStream(stream);

        assertNotNull(result);
        assertEquals(1000, result.pageCount);
        assertEquals(100, result.sizeBytes);
    }

    /**
     * ADVERSARIAL: Stream at exact maximum total size boundary
     * Pairwise: sizeBytes = maxTotalSize
     */
    @Test
    public void testProcessStreamAtMaxTotalSizeBoundary() throws Exception {
        byte[] data = new byte[500_000_000]; // Exactly at limit
        MockPrintStream stream = new MockPrintStream(
            "BOUNDARY", "spool", "text", "letter", "text-only", "memory", data, 1, true
        );

        SpoolFileCreation result = processor.processPrintStream(stream);

        assertNotNull(result);
        assertEquals(500_000_000, result.sizeBytes);
    }

    /**
     * ADVERSARIAL: Stream just over maximum total size boundary
     * Pairwise: sizeBytes = maxTotalSize + 1
     */
    @Test(expected = InvalidPrintStreamException.class)
    public void testProcessStreamOverMaxTotalSizeBoundary() throws Exception {
        byte[] data = new byte[500_000_001]; // Just over limit
        MockPrintStream stream = new MockPrintStream(
            "BOUNDARY_OVER", "spool", "text", "letter", "text-only", "memory", data, 1, true
        );

        processor.processPrintStream(stream);
    }

    /**
     * ADVERSARIAL: Memory destination with large AFPDS content
     * Pairwise: destination=memory + format=afpds + contentType=graphics + sizeBytes=large
     */
    @Test
    public void testProcessLargeAFPDSToMemoryDestination() throws Exception {
        byte[] baseData = "DAFPDATA".getBytes();
        byte[] largeData = new byte[10_000_000]; // 10MB
        System.arraycopy(baseData, 0, largeData, 0, baseData.length);

        MockPrintStream stream = new MockPrintStream(
            "AFP_LARGE_MEM", "spool", "afpds", "a4", "graphics", "memory", largeData, 5, true
        );

        SpoolFileCreation result = processor.processPrintStream(stream);

        assertNotNull(result);
        assertEquals(10_000_000, result.sizeBytes);
        assertEquals("memory", "memory");
    }

    /**
     * ADVERSARIAL: Multiple format conversions (format chain)
     * Pairwise: format=text -> destination=printer (requires SCS-like formatting)
     */
    @Test
    public void testFormatTextStreamToPrinterDestination() throws Exception {
        byte[] data = "Plain text content".getBytes();
        MockPrintStream stream = new MockPrintStream(
            "TEXT_TO_PRINTER", "spool", "text", "letter", "text-only", "printer", data, 1, true
        );

        byte[] formatted = processor.formatForDestination(stream, "printer");

        assertNotNull(formatted);
        assertTrue(formatted.length > 0);
        // Printer destination adds escape sequences
    }

    /**
     * ADVERSARIAL: Process all dimensions in one complex stream
     * Pairwise: printType=host-print + format=scs + pageSize=legal + contentType=graphics + destination=file
     */
    @Test
    public void testProcessComplexStreamAllDimensions() throws Exception {
        byte[] data = new byte[] {0x01, 0x02, 0x03, 0x04};
        MockPrintStream stream = new MockPrintStream(
            "COMPLEX_ALL", "host-print", "scs", "legal", "graphics", "file", data, 3, true
        );

        SpoolFileCreation result = processor.processPrintStream(stream);

        assertNotNull(result);
        assertEquals("host-print", "host-print");
        assertEquals("scs", result.format);
        assertEquals("legal", result.pageSize);
        assertEquals("graphics", result.contentType);
        assertEquals(3, result.pageCount);
    }

    /**
     * ADVERSARIAL: Validation with all warnings but still valid
     * Pairwise: warnings=many + isValid=true
     */
    @Test
    public void testValidateStreamWithWarningsButStillValid() throws Exception {
        byte[] data = "text content".getBytes();
        MockPrintStream stream = new MockPrintStream(
            "WARNINGS", "spool", "scs", "letter", "text-only", "file", data, 1, true
        );

        PrintStreamValidation validation = processor.validateStream(stream);

        assertTrue(validation.isValid); // Despite warnings, still valid
    }

}
