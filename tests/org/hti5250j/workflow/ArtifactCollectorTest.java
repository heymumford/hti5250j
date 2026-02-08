package org.hti5250j.workflow;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.util.Scanner;

class ArtifactCollectorTest {

    @Test
    void testCaptureScreenCreatesScreenshot(@TempDir File tempDir) throws Exception {
        // Create a simple test image
        BufferedImage testImage = new BufferedImage(80, 24, BufferedImage.TYPE_INT_RGB);

        ArtifactCollector collector = new ArtifactCollector(tempDir);
        File screenshot = collector.captureScreen(testImage, "test_screen");

        assertThat(screenshot).exists().hasExtension("png");
        assertThat(screenshot.getName()).startsWith("test_screen");

        // Verify it's a valid PNG by reading it back
        BufferedImage read = ImageIO.read(screenshot);
        assertThat(read).isNotNull();
        assertThat(read.getWidth()).isEqualTo(80);
        assertThat(read.getHeight()).isEqualTo(24);
    }

    @Test
    void testAppendLedgerEntry(@TempDir File tempDir) throws Exception {
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        // Append two entries
        collector.appendLedger("LOGIN", "Connection established");
        collector.appendLedger("ASSERT", "Screen matches confirmation");

        // Read ledger file
        File ledger = new File(tempDir, "execution-ledger.jsonl");
        assertThat(ledger).exists();

        try (Scanner scanner = new Scanner(ledger)) {
            String line1 = scanner.nextLine();
            String line2 = scanner.nextLine();

            assertThat(line1).contains("\"action\":\"LOGIN\"");
            assertThat(line1).contains("\"status\":\"Connection established\"");

            assertThat(line2).contains("\"action\":\"ASSERT\"");
            assertThat(line2).contains("\"status\":\"Screen matches confirmation\"");
        }
    }

    @Test
    void testLedgerIncludesTimestamp(@TempDir File tempDir) throws Exception {
        ArtifactCollector collector = new ArtifactCollector(tempDir);

        collector.appendLedger("FILL", "Data entry completed");

        File ledger = new File(tempDir, "execution-ledger.jsonl");
        try (Scanner scanner = new Scanner(ledger)) {
            String line = scanner.nextLine();
            assertThat(line).contains("\"timestamp\":");
        }
    }
}
