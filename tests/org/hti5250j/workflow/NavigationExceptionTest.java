/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for NavigationException.
 * Verifies screen dump truncation and context preservation.
 */
public class NavigationExceptionTest {

    @Test
    public void testWithScreenDumpMessage() {
        String message = "Screen transition failed";
        String screenDump = "SAMPLE SCREEN CONTENT";

        NavigationException ex = NavigationException.withScreenDump(message, screenDump);

        assertThat(ex.getMessage())
            .contains(message)
            .contains(screenDump);
    }

    @Test
    public void testWithScreenDumpTruncatesLargeContent() {
        String message = "Navigation failed";
        // Create a very large screen dump (10KB+)
        StringBuilder largeScreenDump = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            largeScreenDump.append("Line ").append(i).append(": ")
                .append("x".repeat(50)).append("\n");
        }

        NavigationException ex = NavigationException.withScreenDump(message, largeScreenDump.toString());

        // Exception message should be reasonable size
        String exceptionMsg = ex.getMessage();
        assertThat(exceptionMsg.length()).isLessThan(10_000);  // Should be truncated from >10KB

        // Should still contain message
        assertThat(exceptionMsg).contains(message);

        // Should contain at least some screen content
        assertThat(exceptionMsg).contains("Screen content:");
    }

    @Test
    public void testWithScreenDumpPreservesFirstLines() {
        String message = "Navigation failed";
        StringBuilder screenDump = new StringBuilder();
        screenDump.append("LINE 1: IMPORTANT HEADER\n");
        for (int i = 2; i <= 100; i++) {
            screenDump.append("LINE ").append(i).append("\n");
        }

        NavigationException ex = NavigationException.withScreenDump(message, screenDump.toString());

        // Important header should be preserved
        assertThat(ex.getMessage()).contains("LINE 1: IMPORTANT HEADER");
    }

    @Test
    public void testWithScreenDumpNullScreenContent() {
        String message = "Navigation failed";

        // Should handle null screen dump gracefully
        NavigationException ex = NavigationException.withScreenDump(message, null);

        assertThat(ex.getMessage())
            .contains(message)
            .contains("null");
    }

    @Test
    public void testWithScreenDumpEmptyScreenContent() {
        String message = "Navigation failed";
        String screenDump = "";

        NavigationException ex = NavigationException.withScreenDump(message, screenDump);

        assertThat(ex.getMessage()).contains(message);
    }

    @Test
    public void testWithScreenDumpMaxLines() {
        String message = "Navigation failed";
        StringBuilder screenDump = new StringBuilder();
        // Create exactly 80 lines (typical screen size)
        for (int i = 1; i <= 80; i++) {
            screenDump.append("Line ").append(i).append(": Content\n");
        }

        NavigationException ex = NavigationException.withScreenDump(message, screenDump.toString());

        String exceptionMsg = ex.getMessage();
        assertThat(exceptionMsg).contains(message);
        // Should not truncate content at 80 lines
        assertThat(exceptionMsg).contains("Line 80");
    }

    @Test
    public void testWithScreenDumpExceeds80Lines() {
        String message = "Navigation failed";
        StringBuilder screenDump = new StringBuilder();
        // Create 150 lines (exceeds 80-line limit)
        for (int i = 1; i <= 150; i++) {
            screenDump.append("Line ").append(i).append(": Content\n");
        }

        NavigationException ex = NavigationException.withScreenDump(message, screenDump.toString());

        String exceptionMsg = ex.getMessage();
        assertThat(exceptionMsg).contains(message);
        // Should truncate to ~80 lines max
        long lineCount = exceptionMsg.split("\n").length;
        // Allow some overhead for message + truncation indicator
        assertThat(lineCount).isLessThan(100);
    }

    @Test
    public void testSimpleConstructor() {
        NavigationException ex = new NavigationException("Test message");

        assertThat(ex.getMessage()).isEqualTo("Test message");
    }

    @Test
    public void testConstructorWithCause() {
        Exception cause = new Exception("Root cause");
        NavigationException ex = new NavigationException("Navigation failed", cause);

        assertThat(ex.getMessage()).isEqualTo("Navigation failed");
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
