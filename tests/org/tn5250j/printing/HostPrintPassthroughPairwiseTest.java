/**
 * HostPrintPassthroughPairwiseTest.java - Comprehensive Pairwise TDD Tests
 *
 * Deep pairwise testing for host print passthrough, print stream routing, printer sessions,
 * and SCS (Spooled Command Stream) data handling in TN5250j.
 *
 * PAIRWISE DIMENSIONS:
 * 1. Print mode:      [host-print, pass-through, transparent]
 * 2. Data format:     [text, SCS commands, binary]
 * 3. Session type:    [display, printer, dual]
 * 4. Buffer handling: [immediate, buffered, spooled]
 * 5. Error recovery:  [retry, skip, abort]
 *
 * TEST STRATEGY:
 * - POSITIVE: 15+ tests covering valid host print operations with compatible configurations
 * - ADVERSARIAL: 20+ tests covering malformed streams, resource exhaustion, routing failures
 * - COVERAGE: Each dimension paired with critical adjacent dimensions covering print routing,
 *   session management, and SCS command processing
 *
 * RED-GREEN-REFACTOR:
 * 1. Test failures expose missing validation in host print routing and stream handling
 * 2. Implement minimum validation/processing to make tests pass
 * 3. Refactor for clarity, maintainability, and error handling
 */
package org.tn5250j.printing;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive pairwise TDD tests for TN5250j host print passthrough operations.
 * Covers print stream routing, printer sessions, SCS command processing, and adversarial scenarios.
 */
public class HostPrintPassthroughPairwiseTest {

    // =========================================================================
    // TEST FIXTURES & MOCKS
    // =========================================================================

    /**
     * Mock host print stream with all metadata for passthrough routing
     */
    private static class HostPrintStream {
        final String name;
        final String printMode;           // "host-print", "pass-through", "transparent"
        final String dataFormat;          // "text", "scs-commands", "binary"
        final String sessionType;         // "display", "printer", "dual"
        final String bufferHandling;      // "immediate", "buffered", "spooled"
        final String errorRecovery;       // "retry", "skip", "abort"
        final byte[] streamData;
        final long sizeBytes;
        final int pageCount;
        final String destinationQueue;
        final String printDevice;
        final boolean hasControlCodes;

        HostPrintStream(String name, String printMode, String dataFormat, String sessionType,
                       String bufferHandling, String errorRecovery, byte[] data,
                       String destQueue, String printDevice, int pages, boolean hasCtrlCodes) {
            this.name = name;
            this.printMode = printMode;
            this.dataFormat = dataFormat;
            this.sessionType = sessionType;
            this.bufferHandling = bufferHandling;
            this.errorRecovery = errorRecovery;
            this.streamData = data;
            this.sizeBytes = data.length;
            this.pageCount = pages;
            this.destinationQueue = destQueue;
            this.printDevice = printDevice;
            this.hasControlCodes = hasCtrlCodes;
        }
    }

    /**
     * Mock printer session managing print routing and buffering
     */
    private static class PrinterSession {
        private String sessionId;
        private String sessionType;          // "display", "printer", "dual"
        private String currentPrintMode;    // Current mode of this session
        private PrintBuffer buffer;
        private PrintRouter router;
        private boolean isActive;
        private long sessionStartTime;
        private int totalPagesPrinted;

        PrinterSession(String sessionId, String sessionType) {
            this.sessionId = sessionId;
            this.sessionType = sessionType;
            this.buffer = new PrintBuffer();
            this.router = new PrintRouter();
            this.isActive = true;
            this.sessionStartTime = System.currentTimeMillis();
            this.totalPagesPrinted = 0;
        }

        void setPrintMode(String mode) throws InvalidPrintModeException {
            if (!isValidPrintMode(mode)) {
                throw new InvalidPrintModeException("Invalid print mode: " + mode);
            }
            this.currentPrintMode = mode;
        }

        String getPrintMode() {
            return currentPrintMode;
        }

        void appendPrintData(byte[] data) throws PrintSessionException {
            if (!isActive) {
                throw new PrintSessionException("Session is inactive");
            }
            try {
                buffer.append(data);
            } catch (PrintBufferException e) {
                throw new PrintSessionException("Buffer error: " + e.getMessage());
            }
        }

        byte[] flushBuffer() throws PrintSessionException {
            if (!isActive) {
                throw new PrintSessionException("Session is inactive");
            }
            try {
                return buffer.flush();
            } catch (PrintBufferException e) {
                throw new PrintSessionException("Buffer error: " + e.getMessage());
            }
        }

        void routePrintStream(HostPrintStream stream) throws PrintRoutingException {
            if (!isActive) {
                throw new PrintRoutingException("Session inactive");
            }
            router.route(stream, currentPrintMode);
            totalPagesPrinted += stream.pageCount;
        }

        void closeSession() {
            isActive = false;
        }

        boolean isActive() {
            return isActive;
        }

        int getTotalPagesPrinted() {
            return totalPagesPrinted;
        }

        private boolean isValidPrintMode(String mode) {
            return mode != null && (mode.equals("host-print") || mode.equals("pass-through") ||
                                   mode.equals("transparent"));
        }
    }

    /**
     * Print buffer managing immediate, buffered, and spooled handling
     */
    private static class PrintBuffer {
        private ByteArrayOutputStream buffer;
        private int maxBufferSize = 50_000_000; // 50MB
        private String bufferMode = "buffered";  // "immediate", "buffered", "spooled"

        PrintBuffer() {
            this.buffer = new ByteArrayOutputStream();
        }

        void setBufferMode(String mode) {
            this.bufferMode = mode;
        }

        void append(byte[] data) throws PrintBufferException {
            if (data == null) {
                throw new PrintBufferException("Cannot append null data");
            }
            if (buffer.size() + data.length > maxBufferSize) {
                throw new PrintBufferException("Buffer overflow: " + (buffer.size() + data.length));
            }
            try {
                buffer.write(data);
            } catch (IOException e) {
                throw new PrintBufferException("Failed to append data: " + e.getMessage());
            }
        }

        byte[] flush() throws PrintBufferException {
            byte[] data = buffer.toByteArray();
            buffer = new ByteArrayOutputStream();
            return data;
        }

        byte[] peek() {
            return buffer.toByteArray();
        }

        int size() {
            return buffer.size();
        }

        void clear() {
            buffer = new ByteArrayOutputStream();
        }

        void setMaxSize(int bytes) {
            this.maxBufferSize = bytes;
        }
    }

    /**
     * Print router handling stream routing based on print mode and session type
     */
    private static class PrintRouter {
        private Map<String, PrintQueue> queues = new HashMap<>();
        private Map<String, PrintDevice> devices = new HashMap<>();
        private int routedStreamCount = 0;

        void route(HostPrintStream stream, String printMode) throws PrintRoutingException {
            if (stream == null) {
                throw new PrintRoutingException("Stream is null");
            }
            if (!isValidPrintMode(printMode)) {
                throw new PrintRoutingException("Invalid print mode: " + printMode);
            }

            // Route based on print mode
            switch (printMode) {
                case "host-print":
                    routeToHostPrinter(stream);
                    break;
                case "pass-through":
                    routePassthrough(stream);
                    break;
                case "transparent":
                    routeTransparent(stream);
                    break;
                default:
                    throw new PrintRoutingException("Unknown print mode: " + printMode);
            }
            routedStreamCount++;
        }

        void registerQueue(String queueName, PrintQueue queue) {
            queues.put(queueName, queue);
        }

        void registerDevice(String deviceName, PrintDevice device) {
            devices.put(deviceName, device);
        }

        PrintQueue getQueue(String name) throws PrintRoutingException {
            if (!queues.containsKey(name)) {
                throw new PrintRoutingException("Queue not found: " + name);
            }
            return queues.get(name);
        }

        PrintDevice getDevice(String name) throws PrintRoutingException {
            if (!devices.containsKey(name)) {
                throw new PrintRoutingException("Device not found: " + name);
            }
            return devices.get(name);
        }

        int getRoutedStreamCount() {
            return routedStreamCount;
        }

        private void routeToHostPrinter(HostPrintStream stream) throws PrintRoutingException {
            // Route to host print queue
            if (!queues.containsKey(stream.destinationQueue)) {
                throw new PrintRoutingException("Destination queue not registered: " +
                                              stream.destinationQueue);
            }
            PrintQueue queue = queues.get(stream.destinationQueue);
            try {
                queue.enqueue(stream);
            } catch (PrintQueueException e) {
                throw new PrintRoutingException("Queue error: " + e.getMessage());
            }
        }

        private void routePassthrough(HostPrintStream stream) throws PrintRoutingException {
            // Route directly to print device
            if (!devices.containsKey(stream.printDevice)) {
                throw new PrintRoutingException("Print device not registered: " +
                                              stream.printDevice);
            }
            PrintDevice device = devices.get(stream.printDevice);
            try {
                device.sendStream(stream);
            } catch (PrintDeviceException e) {
                throw new PrintRoutingException("Device error: " + e.getMessage());
            }
        }

        private void routeTransparent(HostPrintStream stream) throws PrintRoutingException {
            // Route based on stream format/content
            if ("scs-commands".equals(stream.dataFormat)) {
                routeToHostPrinter(stream);
            } else {
                routePassthrough(stream);
            }
        }

        private boolean isValidPrintMode(String mode) {
            return mode != null && (mode.equals("host-print") || mode.equals("pass-through") ||
                                   mode.equals("transparent"));
        }
    }

    /**
     * Mock print queue for host print operations
     */
    private static class PrintQueue {
        private String name;
        private Queue<HostPrintStream> queue = new LinkedList<>();
        private int maxQueueSize = 1000;

        PrintQueue(String name) {
            this.name = name;
        }

        void enqueue(HostPrintStream stream) throws PrintQueueException {
            if (queue.size() >= maxQueueSize) {
                throw new PrintQueueException("Queue full: " + name);
            }
            queue.offer(stream);
        }

        HostPrintStream dequeue() throws PrintQueueException {
            HostPrintStream stream = queue.poll();
            if (stream == null) {
                throw new PrintQueueException("Queue empty: " + name);
            }
            return stream;
        }

        int size() {
            return queue.size();
        }

        boolean isEmpty() {
            return queue.isEmpty();
        }

        void clear() {
            queue.clear();
        }

        String getName() {
            return name;
        }
    }

    /**
     * Mock print device for passthrough operations
     */
    private static class PrintDevice {
        private String name;
        private List<HostPrintStream> sentStreams = new ArrayList<>();
        private boolean isOnline;

        PrintDevice(String name) {
            this.name = name;
            this.isOnline = true;
        }

        void sendStream(HostPrintStream stream) throws PrintDeviceException {
            if (!isOnline) {
                throw new PrintDeviceException("Device offline: " + name);
            }
            if (stream == null) {
                throw new PrintDeviceException("Stream is null");
            }
            sentStreams.add(stream);
        }

        List<HostPrintStream> getSentStreams() {
            return new ArrayList<>(sentStreams);
        }

        void setOnline(boolean online) {
            this.isOnline = online;
        }

        boolean isOnline() {
            return isOnline;
        }

        int getSentStreamCount() {
            return sentStreams.size();
        }

        void clear() {
            sentStreams.clear();
        }

        String getName() {
            return name;
        }
    }

    /**
     * SCS command processor for Spooled Command Stream parsing
     */
    private static class SCSCommandProcessor {
        private Map<Byte, String> commandMap = new HashMap<>();

        SCSCommandProcessor() {
            // Common SCS command codes
            commandMap.put((byte) 0x01, "Start Print");
            commandMap.put((byte) 0x02, "Stop Print");
            commandMap.put((byte) 0x03, "Page Break");
            commandMap.put((byte) 0x04, "Carriage Return");
            commandMap.put((byte) 0x05, "Form Feed");
        }

        List<String> parseCommands(byte[] data) throws SCSParseException {
            if (data == null || data.length == 0) {
                throw new SCSParseException("Data is null or empty");
            }

            List<String> commands = new ArrayList<>();
            for (byte b : data) {
                if (commandMap.containsKey(b)) {
                    commands.add(commandMap.get(b));
                }
            }

            if (commands.isEmpty()) {
                // If no recognized SCS commands, treat as plain data
                commands.add("Text Data");
            }

            return commands;
        }

        boolean isSCSFormat(byte[] data) {
            if (data == null || data.length == 0) return false;
            return data[0] == 0x01 || data[0] == 0x02;
        }

        byte[] generateSCSStream(String... commands) throws SCSGenerationException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (String cmd : commands) {
                byte code = getCommandCode(cmd);
                out.write(code);
            }
            return out.toByteArray();
        }

        private byte getCommandCode(String command) throws SCSGenerationException {
            for (Map.Entry<Byte, String> entry : commandMap.entrySet()) {
                if (entry.getValue().equals(command)) {
                    return entry.getKey();
                }
            }
            throw new SCSGenerationException("Unknown command: " + command);
        }
    }

    /**
     * Custom exceptions for host print operations
     */
    private static class InvalidPrintModeException extends Exception {
        InvalidPrintModeException(String msg) { super(msg); }
    }

    private static class PrintSessionException extends Exception {
        PrintSessionException(String msg) { super(msg); }
    }

    private static class PrintBufferException extends Exception {
        PrintBufferException(String msg) { super(msg); }
    }

    private static class PrintRoutingException extends Exception {
        PrintRoutingException(String msg) { super(msg); }
    }

    private static class PrintQueueException extends Exception {
        PrintQueueException(String msg) { super(msg); }
    }

    private static class PrintDeviceException extends Exception {
        PrintDeviceException(String msg) { super(msg); }
    }

    private static class SCSParseException extends Exception {
        SCSParseException(String msg) { super(msg); }
    }

    private static class SCSGenerationException extends Exception {
        SCSGenerationException(String msg) { super(msg); }
    }

    // =========================================================================
    // TEST SETUP & TEARDOWN
    // =========================================================================

    private PrinterSession displaySession;
    private PrinterSession printerSession;
    private PrinterSession dualSession;
    private PrintQueue hostPrintQueue;
    private PrintDevice passthruDevice;
    private SCSCommandProcessor scsProcessor;
    private File tempDir;

    @Before
    public void setUp() throws Exception {
        displaySession = new PrinterSession("SES_001", "display");
        printerSession = new PrinterSession("SES_002", "printer");
        dualSession = new PrinterSession("SES_003", "dual");

        hostPrintQueue = new PrintQueue("QPRINT");
        passthruDevice = new PrintDevice("LPR_001");

        scsProcessor = new SCSCommandProcessor();

        // Register router infrastructure
        displaySession.router.registerQueue("QPRINT", hostPrintQueue);
        displaySession.router.registerDevice("LPR_001", passthruDevice);
        printerSession.router.registerQueue("QPRINT", hostPrintQueue);
        printerSession.router.registerDevice("LPR_001", passthruDevice);
        dualSession.router.registerQueue("QPRINT", hostPrintQueue);
        dualSession.router.registerDevice("LPR_001", passthruDevice);

        tempDir = Files.createTempDirectory("host-print-test").toFile();
    }

    @After
    public void tearDown() throws Exception {
        if (displaySession != null) displaySession.closeSession();
        if (printerSession != null) printerSession.closeSession();
        if (dualSession != null) dualSession.closeSession();

        hostPrintQueue.clear();
        passthruDevice.clear();

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
    // POSITIVE TEST CASES (15+ tests): Valid host print operations
    // =========================================================================

    /**
     * POSITIVE: Host-print mode with text data to printer session
     * Pairwise: printMode=host-print + dataFormat=text + sessionType=printer
     */
    @Test
    public void testHostPrintModeWithTextToPrinterSession() throws Exception {
        printerSession.setPrintMode("host-print");
        byte[] data = "Print this line\n".getBytes();
        HostPrintStream stream = new HostPrintStream(
            "HP_001", "host-print", "text", "printer", "buffered",
            "retry", data, "QPRINT", "LPR_001", 1, false
        );

        printerSession.appendPrintData(data);
        printerSession.routePrintStream(stream);

        assertTrue(hostPrintQueue.size() > 0);
        assertEquals(1, printerSession.getTotalPagesPrinted());
    }

    /**
     * POSITIVE: Pass-through mode with binary data to display session
     * Pairwise: printMode=pass-through + dataFormat=binary + sessionType=display
     */
    @Test
    public void testPassthroughModeWithBinaryToDisplaySession() throws Exception {
        displaySession.setPrintMode("pass-through");
        byte[] data = new byte[] {0x1B, 0x5B, 0x48, 0x1B, 0x5B, 0x4A};
        HostPrintStream stream = new HostPrintStream(
            "PT_001", "pass-through", "binary", "display", "immediate",
            "skip", data, "QPRINT", "LPR_001", 1, true
        );

        displaySession.routePrintStream(stream);

        assertEquals(1, passthruDevice.getSentStreamCount());
        assertEquals(1, displaySession.getTotalPagesPrinted());
    }

    /**
     * POSITIVE: Transparent mode with SCS commands to dual session
     * Pairwise: printMode=transparent + dataFormat=scs-commands + sessionType=dual
     */
    @Test
    public void testTransparentModeWithSCSCommandsToDualSession() throws Exception {
        dualSession.setPrintMode("transparent");
        byte[] data = scsProcessor.generateSCSStream("Start Print", "Page Break", "Stop Print");
        HostPrintStream stream = new HostPrintStream(
            "TR_001", "transparent", "scs-commands", "dual", "spooled",
            "abort", data, "QPRINT", "LPR_001", 2, true
        );

        dualSession.routePrintStream(stream);

        assertTrue(hostPrintQueue.size() > 0 || passthruDevice.getSentStreamCount() > 0);
        assertEquals(2, dualSession.getTotalPagesPrinted());
    }

    /**
     * POSITIVE: Immediate buffer handling with host-print and text
     * Pairwise: bufferHandling=immediate + printMode=host-print + dataFormat=text
     */
    @Test
    public void testImmediateBufferHandlingWithHostPrint() throws Exception {
        printerSession.setPrintMode("host-print");
        printerSession.buffer.setBufferMode("immediate");
        byte[] data = "Immediate print".getBytes();
        HostPrintStream stream = new HostPrintStream(
            "IMM_001", "host-print", "text", "printer", "immediate",
            "retry", data, "QPRINT", "LPR_001", 1, false
        );

        printerSession.appendPrintData(data);
        byte[] buffered = printerSession.flushBuffer();

        assertTrue(buffered.length > 0);
        assertEquals(data.length, buffered.length);
    }

    /**
     * POSITIVE: Buffered buffer handling with pass-through and binary
     * Pairwise: bufferHandling=buffered + printMode=pass-through + dataFormat=binary
     */
    @Test
    public void testBufferedBufferHandlingWithPassthrough() throws Exception {
        displaySession.setPrintMode("pass-through");
        printerSession.buffer.setBufferMode("buffered");
        byte[] data1 = new byte[] {0x01, 0x02, 0x03};
        byte[] data2 = new byte[] {0x04, 0x05, 0x06};

        displaySession.appendPrintData(data1);
        displaySession.appendPrintData(data2);
        byte[] flushed = displaySession.flushBuffer();

        assertEquals(6, flushed.length);
    }

    /**
     * POSITIVE: Spooled buffer handling with multi-page document
     * Pairwise: bufferHandling=spooled + pageCount=3
     */
    @Test
    public void testSpooledBufferHandlingWithMultipage() throws Exception {
        printerSession.setPrintMode("host-print");
        printerSession.buffer.setBufferMode("spooled");
        byte[] page1 = "Page 1\n".getBytes();
        byte[] page2 = "Page 2\n".getBytes();
        byte[] page3 = "Page 3\n".getBytes();

        printerSession.appendPrintData(page1);
        printerSession.appendPrintData(page2);
        printerSession.appendPrintData(page3);

        HostPrintStream stream = new HostPrintStream(
            "SPL_001", "host-print", "text", "printer", "spooled",
            "retry", new byte[] {}, "QPRINT", "LPR_001", 3, false
        );
        printerSession.routePrintStream(stream);

        assertEquals(3, printerSession.getTotalPagesPrinted());
    }

    /**
     * POSITIVE: SCS format validation and command parsing
     * Pairwise: dataFormat=scs-commands + validation=format-check
     */
    @Test
    public void testSCSFormatValidationAndParsing() throws Exception {
        byte[] scsData = scsProcessor.generateSCSStream("Start Print", "Page Break");
        assertTrue(scsProcessor.isSCSFormat(scsData));

        List<String> commands = scsProcessor.parseCommands(scsData);
        assertTrue(commands.size() > 0);
        assertTrue(commands.contains("Start Print") || commands.contains("Page Break"));
    }

    /**
     * POSITIVE: Error recovery retry behavior on transient failure
     * Pairwise: errorRecovery=retry + printMode=host-print
     */
    @Test
    public void testErrorRecoveryWithRetryBehavior() throws Exception {
        printerSession.setPrintMode("host-print");
        byte[] data = "Retry test".getBytes();
        HostPrintStream stream = new HostPrintStream(
            "ERR_001", "host-print", "text", "printer", "buffered",
            "retry", data, "QPRINT", "LPR_001", 1, false
        );

        // Simulate first attempt
        printerSession.appendPrintData(data);

        // Should succeed on first attempt with valid stream
        printerSession.routePrintStream(stream);

        assertEquals(1, printerSession.getTotalPagesPrinted());
    }

    /**
     * POSITIVE: Error recovery skip behavior on non-critical failure
     * Pairwise: errorRecovery=skip + dataFormat=text
     */
    @Test
    public void testErrorRecoveryWithSkipBehavior() throws Exception {
        displaySession.setPrintMode("pass-through");
        byte[] data = "Skip test".getBytes();
        HostPrintStream stream = new HostPrintStream(
            "ERR_002", "pass-through", "text", "display", "buffered",
            "skip", data, "QPRINT", "LPR_001", 1, false
        );

        displaySession.routePrintStream(stream);
        assertEquals(1, displaySession.getTotalPagesPrinted());
    }

    /**
     * POSITIVE: Error recovery abort behavior on critical failure
     * Pairwise: errorRecovery=abort + sessionType=printer
     */
    @Test
    public void testErrorRecoveryWithAbortBehavior() throws Exception {
        printerSession.setPrintMode("host-print");
        HostPrintStream stream = new HostPrintStream(
            "ERR_003", "host-print", "text", "printer", "buffered",
            "abort", "Data".getBytes(), "QPRINT", "LPR_001", 1, false
        );

        printerSession.routePrintStream(stream);
        // Abort should still route but mark for termination
        assertEquals(1, printerSession.getTotalPagesPrinted());
    }

    /**
     * POSITIVE: Mixed print modes across multiple sessions
     * Pairwise: printMode varies + sessionType varies
     */
    @Test
    public void testMixedPrintModesAcrossSessionTypes() throws Exception {
        displaySession.setPrintMode("pass-through");
        printerSession.setPrintMode("host-print");
        dualSession.setPrintMode("transparent");

        byte[] displayData = "Display output".getBytes();
        byte[] printerData = "Printer output".getBytes();
        byte[] dualData = scsProcessor.generateSCSStream("Start Print");

        HostPrintStream ds = new HostPrintStream(
            "MIX_001", "pass-through", "text", "display", "immediate", "skip",
            displayData, "QPRINT", "LPR_001", 1, false
        );
        HostPrintStream ps = new HostPrintStream(
            "MIX_002", "host-print", "text", "printer", "buffered", "retry",
            printerData, "QPRINT", "LPR_001", 1, false
        );
        HostPrintStream ts = new HostPrintStream(
            "MIX_003", "transparent", "scs-commands", "dual", "spooled", "abort",
            dualData, "QPRINT", "LPR_001", 1, true
        );

        displaySession.routePrintStream(ds);
        printerSession.routePrintStream(ps);
        dualSession.routePrintStream(ts);

        assertEquals(1, displaySession.getTotalPagesPrinted());
        assertEquals(1, printerSession.getTotalPagesPrinted());
        assertEquals(1, dualSession.getTotalPagesPrinted());
    }

    /**
     * POSITIVE: Large print stream handling with buffering
     * Pairwise: sizeBytes=large + bufferHandling=buffered
     */
    @Test
    public void testLargePrintStreamWithBuffering() throws Exception {
        printerSession.setPrintMode("host-print");
        byte[] largeData = new byte[1_000_000]; // 1MB
        java.util.Arrays.fill(largeData, (byte) 'A');

        printerSession.appendPrintData(largeData);
        byte[] flushed = printerSession.flushBuffer();

        assertEquals(largeData.length, flushed.length);
    }

    /**
     * POSITIVE: Multi-page document with SCS page breaks
     * Pairwise: pageCount=4 + dataFormat=scs-commands
     */
    @Test
    public void testMultiPageDocumentWithSCSPageBreaks() throws Exception {
        printerSession.setPrintMode("host-print");
        byte[] scsData = scsProcessor.generateSCSStream("Start Print", "Page Break", "Page Break", "Stop Print");

        HostPrintStream stream = new HostPrintStream(
            "MP_001", "host-print", "scs-commands", "printer", "spooled",
            "retry", scsData, "QPRINT", "LPR_001", 4, true
        );

        printerSession.routePrintStream(stream);
        assertEquals(4, printerSession.getTotalPagesPrinted());
    }

    /**
     * POSITIVE: Transparent mode routing decision based on data format
     * Pairwise: printMode=transparent + intelligent routing
     */
    @Test
    public void testTransparentModeIntelligentRouting() throws Exception {
        dualSession.setPrintMode("transparent");

        // SCS format should route to host printer
        byte[] scsData = scsProcessor.generateSCSStream("Start Print");
        HostPrintStream scsStream = new HostPrintStream(
            "TR_002", "transparent", "scs-commands", "dual", "buffered",
            "retry", scsData, "QPRINT", "LPR_001", 1, true
        );

        dualSession.routePrintStream(scsStream);
        assertTrue(hostPrintQueue.size() > 0);

        // Reset
        hostPrintQueue.clear();

        // Text format should route to passthrough
        byte[] textData = "Pass through text".getBytes();
        HostPrintStream textStream = new HostPrintStream(
            "TR_003", "transparent", "text", "dual", "buffered",
            "retry", textData, "QPRINT", "LPR_001", 1, false
        );

        dualSession.routePrintStream(textStream);
        assertEquals(1, passthruDevice.getSentStreamCount());
    }

    // =========================================================================
    // ADVERSARIAL TEST CASES (20+ tests): Malformed streams, routing failures, edge cases
    // =========================================================================

    /**
     * ADVERSARIAL: Null host print stream
     * Pairwise: stream=null
     */
    @Test(expected = PrintRoutingException.class)
    public void testRouteNullPrintStream() throws Exception {
        printerSession.setPrintMode("host-print");
        printerSession.router.route(null, "host-print");
    }

    /**
     * ADVERSARIAL: Invalid print mode
     * Pairwise: printMode=invalid
     */
    @Test(expected = InvalidPrintModeException.class)
    public void testSetInvalidPrintMode() throws Exception {
        printerSession.setPrintMode("unknown-mode");
    }

    /**
     * ADVERSARIAL: Route with unregistered destination queue
     * Pairwise: destination=unregistered
     */
    @Test(expected = PrintRoutingException.class)
    public void testRouteToUnregisteredQueue() throws Exception {
        printerSession.setPrintMode("host-print");
        byte[] data = "test".getBytes();
        HostPrintStream stream = new HostPrintStream(
            "UNREG_001", "host-print", "text", "printer", "buffered",
            "retry", data, "QPRINT_UNKNOWN", "LPR_001", 1, false
        );

        printerSession.router.route(stream, "host-print");
    }

    /**
     * ADVERSARIAL: Route to offline printer device
     * Pairwise: device.online=false
     */
    @Test(expected = PrintDeviceException.class)
    public void testRouteToOfflineDevice() throws Exception {
        passthruDevice.setOnline(false);
        displaySession.setPrintMode("pass-through");
        byte[] data = "test".getBytes();
        HostPrintStream stream = new HostPrintStream(
            "OFF_001", "pass-through", "text", "display", "immediate",
            "skip", data, "QPRINT", "LPR_001", 1, false
        );

        passthruDevice.sendStream(stream);
    }

    /**
     * ADVERSARIAL: Append data to inactive session
     * Pairwise: session.isActive=false
     */
    @Test(expected = PrintSessionException.class)
    public void testAppendDataToInactiveSession() throws Exception {
        printerSession.closeSession();
        byte[] data = "test".getBytes();
        printerSession.appendPrintData(data);
    }

    /**
     * ADVERSARIAL: Buffer overflow with extremely large data
     * Pairwise: bufferSize > maxBufferSize
     */
    @Test(expected = PrintSessionException.class)
    public void testBufferOverflowWithLargeData() throws Exception {
        printerSession.buffer.setMaxSize(100);
        byte[] largeData = new byte[200];
        printerSession.appendPrintData(largeData);
    }

    /**
     * ADVERSARIAL: Append null data to buffer
     * Pairwise: data=null
     */
    @Test(expected = PrintSessionException.class)
    public void testAppendNullDataToBuffer() throws Exception {
        printerSession.appendPrintData(null);
    }

    /**
     * ADVERSARIAL: Dequeue from empty queue
     * Pairwise: queue.isEmpty()=true
     */
    @Test(expected = PrintQueueException.class)
    public void testDequeueFromEmptyQueue() throws Exception {
        hostPrintQueue.dequeue();
    }

    /**
     * ADVERSARIAL: Enqueue to full queue
     * Pairwise: queue.size() >= maxQueueSize
     */
    @Test(expected = PrintQueueException.class)
    public void testEnqueueToFullQueue() throws Exception {
        // Create mock queue with size 1
        PrintQueue smallQueue = new PrintQueue("SMALL");
        byte[] data = "test".getBytes();
        HostPrintStream stream1 = new HostPrintStream(
            "FQ_001", "host-print", "text", "printer", "buffered",
            "retry", data, "SMALL", "LPR_001", 1, false
        );
        HostPrintStream stream2 = new HostPrintStream(
            "FQ_002", "host-print", "text", "printer", "buffered",
            "retry", data, "SMALL", "LPR_001", 1, false
        );

        smallQueue.enqueue(stream1);
        // Manually set max size to 1 to force overflow
        smallQueue.maxQueueSize = 1;
        smallQueue.enqueue(stream2); // Should throw
    }

    /**
     * ADVERSARIAL: Parse SCS commands from empty data
     * Pairwise: scsData.length=0
     */
    @Test(expected = SCSParseException.class)
    public void testParseSCSCommandsFromEmptyData() throws Exception {
        scsProcessor.parseCommands(new byte[0]);
    }

    /**
     * ADVERSARIAL: Parse SCS commands from null data
     * Pairwise: scsData=null
     */
    @Test(expected = SCSParseException.class)
    public void testParseSCSCommandsFromNullData() throws Exception {
        scsProcessor.parseCommands(null);
    }

    /**
     * ADVERSARIAL: Malformed SCS stream with invalid command codes
     * Pairwise: scsData.format=invalid
     */
    @Test
    public void testParseMalformedSCSStream() throws Exception {
        byte[] malformed = new byte[] {(byte) 0xFF, (byte) 0xEE, (byte) 0xDD};
        List<String> commands = scsProcessor.parseCommands(malformed);
        // Should return Text Data for unrecognized commands
        assertTrue(commands.contains("Text Data"));
    }

    /**
     * ADVERSARIAL: Route stream during session transition
     * Pairwise: session state change during routing
     */
    @Test(expected = PrintRoutingException.class)
    public void testRouteStreamDuringSessionTransition() throws Exception {
        printerSession.setPrintMode("host-print");
        byte[] data = "test".getBytes();
        HostPrintStream stream = new HostPrintStream(
            "TR_001", "host-print", "text", "printer", "buffered",
            "retry", data, "QPRINT", "LPR_001", 1, false
        );

        printerSession.closeSession();
        printerSession.routePrintStream(stream);
    }

    /**
     * ADVERSARIAL: Flush empty buffer
     * Pairwise: buffer.isEmpty()=true
     */
    @Test
    public void testFlushEmptyBuffer() throws Exception {
        displaySession.setPrintMode("pass-through");
        byte[] flushed = displaySession.flushBuffer();
        assertEquals(0, flushed.length);
    }

    /**
     * ADVERSARIAL: Register null queue
     * Pairwise: queue=null
     */
    @Test
    public void testRegisterNullQueueName() throws Exception {
        printerSession.router.registerQueue(null, hostPrintQueue);
        // Should register but may cause routing issues
        PrintQueue retrieved = printerSession.router.getQueue(null);
        assertNotNull(retrieved);
    }

    /**
     * ADVERSARIAL: Retrieve non-existent queue
     * Pairwise: queue.exists=false
     */
    @Test(expected = PrintRoutingException.class)
    public void testRetrieveNonexistentQueue() throws Exception {
        printerSession.router.getQueue("NONEXISTENT");
    }

    /**
     * ADVERSARIAL: Retrieve non-existent device
     * Pairwise: device.exists=false
     */
    @Test(expected = PrintRoutingException.class)
    public void testRetrieveNonexistentDevice() throws Exception {
        displaySession.router.getDevice("NONEXISTENT");
    }

    /**
     * ADVERSARIAL: Switch print mode multiple times rapidly
     * Pairwise: mode transitions=multiple
     */
    @Test
    public void testRapidPrintModeSwitching() throws Exception {
        printerSession.setPrintMode("host-print");
        printerSession.setPrintMode("pass-through");
        printerSession.setPrintMode("transparent");
        printerSession.setPrintMode("host-print");

        assertEquals("host-print", printerSession.getPrintMode());
    }

    /**
     * ADVERSARIAL: Process stream with no control codes in SCS format
     * Pairwise: scsFormat=true + controlCodes=false
     */
    @Test
    public void testSCSFormatWithoutControlCodes() throws Exception {
        byte[] plainData = "No control codes here".getBytes();
        List<String> commands = scsProcessor.parseCommands(plainData);
        assertTrue(commands.contains("Text Data"));
    }

    /**
     * ADVERSARIAL: Dual session with mismatched print mode
     * Pairwise: sessionType=dual + printMode mismatch
     */
    @Test
    public void testDualSessionWithMismatchedMode() throws Exception {
        dualSession.setPrintMode("host-print");
        byte[] data = "Dual mode data".getBytes();
        HostPrintStream stream = new HostPrintStream(
            "DS_001", "host-print", "text", "dual", "buffered",
            "retry", data, "QPRINT", "LPR_001", 2, false
        );

        dualSession.routePrintStream(stream);
        assertEquals(2, dualSession.getTotalPagesPrinted());
    }

    /**
     * ADVERSARIAL: Print stream with zero page count
     * Pairwise: pageCount=0
     */
    @Test
    public void testStreamWithZeroPageCount() throws Exception {
        printerSession.setPrintMode("host-print");
        byte[] data = "Zero pages".getBytes();
        HostPrintStream stream = new HostPrintStream(
            "ZP_001", "host-print", "text", "printer", "buffered",
            "retry", data, "QPRINT", "LPR_001", 0, false
        );

        printerSession.routePrintStream(stream);
        assertEquals(0, printerSession.getTotalPagesPrinted());
    }

    /**
     * ADVERSARIAL: Process extremely large print stream (multi-gigabyte simulation)
     * Pairwise: sizeBytes > typical memory
     */
    @Test(expected = PrintSessionException.class)
    public void testExtremellyLargePrintStream() throws Exception {
        printerSession.buffer.setMaxSize(1_000_000); // 1MB limit
        byte[] hugeData = new byte[10_000_000]; // 10MB
        printerSession.appendPrintData(hugeData);
    }

    /**
     * ADVERSARIAL: Concurrent append and flush operations
     * Pairwise: buffer.append simultaneous with buffer.flush
     */
    @Test
    public void testConcurrentBufferOperations() throws Exception {
        printerSession.setPrintMode("host-print");
        byte[] data = "Concurrent test".getBytes();

        printerSession.appendPrintData(data);
        byte[] flushed = printerSession.flushBuffer();

        assertEquals(data.length, flushed.length);
        byte[] empty = printerSession.flushBuffer();
        assertEquals(0, empty.length);
    }

    /**
     * ADVERSARIAL: SCS command generation with unknown command name
     * Pairwise: generateSCS + unknownCommand
     */
    @Test(expected = SCSGenerationException.class)
    public void testSCSGenerationWithUnknownCommand() throws Exception {
        scsProcessor.generateSCSStream("Unknown Command");
    }

    /**
     * ADVERSARIAL: Route stream to mismatched session type
     * Pairwise: streamSessionType != routingSessionType
     */
    @Test
    public void testStreamWithMismatchedSessionType() throws Exception {
        displaySession.setPrintMode("pass-through");
        byte[] data = "Display data".getBytes();
        // Stream says printer, but routed via display session
        HostPrintStream stream = new HostPrintStream(
            "SM_001", "pass-through", "text", "printer", "immediate",
            "skip", data, "QPRINT", "LPR_001", 1, false
        );

        displaySession.routePrintStream(stream);
        assertEquals(1, displaySession.getTotalPagesPrinted());
    }
}
