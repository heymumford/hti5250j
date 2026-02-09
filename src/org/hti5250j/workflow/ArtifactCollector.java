package org.hti5250j.workflow;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ArtifactCollector {
    private final File artifactDir;
    private final File ledgerFile;

    public ArtifactCollector(File artifactDir) {
        this.artifactDir = artifactDir;
        this.ledgerFile = new File(artifactDir, "execution-ledger.jsonl");
    }

    /**
     * Get the artifact directory for this collector.
     *
     * @return artifact directory File
     */
    public File getArtifactDir() {
        return artifactDir;
    }

    /**
     * Capture screen as PNG screenshot with timestamp in filename.
     * Returns File reference to the created PNG.
     */
    public File captureScreen(BufferedImage screen, String screenName) throws IOException {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000); // seconds
        String filename = screenName + "_" + timestamp + ".png";
        File screenshotFile = new File(artifactDir, filename);

        ImageIO.write(screen, "png", screenshotFile);
        return screenshotFile;
    }

    /**
     * Append execution step to JSON ledger file.
     * Creates JSONL format (one JSON object per line).
     */
    public void appendLedger(String action, String status) throws IOException {
        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
        String jsonLine = String.format(
            "{\"timestamp\":\"%s\",\"action\":\"%s\",\"status\":\"%s\"}",
            timestamp, action, status
        );

        try (FileWriter writer = new FileWriter(ledgerFile, true)) {
            writer.write(jsonLine);
            writer.write("\n");
        }
    }
}
