/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.gui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REFACTOR phase: Tests to verify that copyright violations have been removed
 * from the sorting table implementation.
 *
 * This test suite reads the source files and verifies that:
 * 1. No unlicensed JavaPro magazine code comments remain
 * 2. No "I have NOT asked for permission" statements exist
 * 3. Proper SPDX license headers are in place
 */
@DisplayName("Copyright Compliance Verification (REFACTOR Phase)")
class CopyrightComplianceTest {

    private static final String PROJECT_ROOT = System.getProperty("user.dir");
    private static final String SRC_DIR = PROJECT_ROOT + "/src/org/hti5250j/gui";

    @Test
    @DisplayName("JSortTable.java should not contain unlicensed code notice")
    void testJSortTableNoUnlicensedCode() throws IOException {
        String content = readFile(SRC_DIR + "/JSortTable.java");

        assertFalse(content.contains("I have NOT asked for permission"),
                "JSortTable should not contain unlicensed code attribution");
        assertFalse(content.contains("JavaPro magazine"),
                "JSortTable should not reference JavaPro magazine");
        assertFalse(content.contains("Claude Duguay"),
                "JSortTable should not contain original author attribution");
    }

    @Test
    @DisplayName("SortTableModel.java should not contain unlicensed code notice")
    void testSortTableModelNoUnlicensedCode() throws IOException {
        String content = readFile(SRC_DIR + "/SortTableModel.java");

        assertFalse(content.contains("I have NOT asked for permission"),
                "SortTableModel should not contain unlicensed code attribution");
        assertFalse(content.contains("JavaPro magazine"),
                "SortTableModel should not reference JavaPro magazine");
    }

    @Test
    @DisplayName("SortHeaderRenderer.java should not contain unlicensed code")
    void testSortHeaderRendererNoUnlicensedCode() throws IOException {
        String content = readFile(SRC_DIR + "/SortHeaderRenderer.java");

        assertFalse(content.contains("I have NOT asked for permission"),
                "SortHeaderRenderer should not contain unlicensed code attribution");
        assertFalse(content.contains("JavaPro magazine"),
                "SortHeaderRenderer should not reference JavaPro magazine");
    }

    @Test
    @DisplayName("SortArrowIcon.java should not contain unlicensed code")
    void testSortArrowIconNoUnlicensedCode() throws IOException {
        String content = readFile(SRC_DIR + "/SortArrowIcon.java");

        assertFalse(content.contains("I have NOT asked for permission"),
                "SortArrowIcon should not contain unlicensed code attribution");
        assertFalse(content.contains("JavaPro magazine"),
                "SortArrowIcon should not reference JavaPro magazine");
    }

    @Test
    @DisplayName("DefaultSortTableModel.java should not contain unlicensed code")
    void testDefaultSortTableModelNoUnlicensedCode() throws IOException {
        String content = readFile(SRC_DIR + "/DefaultSortTableModel.java");

        assertFalse(content.contains("I have NOT asked for permission"),
                "DefaultSortTableModel should not contain unlicensed code attribution");
        assertFalse(content.contains("JavaPro magazine"),
                "DefaultSortTableModel should not reference JavaPro magazine");
    }

    @Test
    @DisplayName("ColumnComparator.java should not contain unlicensed code")
    void testColumnComparatorNoUnlicensedCode() throws IOException {
        String content = readFile(SRC_DIR + "/ColumnComparator.java");

        assertFalse(content.contains("I have NOT asked for permission"),
                "ColumnComparator should not contain unlicensed code attribution");
        assertFalse(content.contains("Created by Claude Duguay"),
                "ColumnComparator should not contain original author attribution");
    }

    @Test
    @DisplayName("JSortTable.java should have proper SPDX license header")
    void testJSortTableHasLicenseHeader() throws IOException {
        String content = readFile(SRC_DIR + "/JSortTable.java");

        assertTrue(content.contains("SPDX-FileCopyrightText"),
                "JSortTable should have SPDX copyright header");
        assertTrue(content.contains("SPDX-License-Identifier: GPL-2.0-or-later"),
                "JSortTable should have SPDX license identifier");
    }

    @Test
    @DisplayName("SortTableModel.java should have proper SPDX license header")
    void testSortTableModelHasLicenseHeader() throws IOException {
        String content = readFile(SRC_DIR + "/SortTableModel.java");

        assertTrue(content.contains("SPDX-FileCopyrightText"),
                "SortTableModel should have SPDX copyright header");
        assertTrue(content.contains("SPDX-License-Identifier: GPL-2.0-or-later"),
                "SortTableModel should have SPDX license identifier");
    }

    @Test
    @DisplayName("New ModernTableSorter.java should exist with proper licensing")
    void testModernTableSorterExists() throws IOException {
        String content = readFile(SRC_DIR + "/ModernTableSorter.java");

        assertNotNull(content, "ModernTableSorter should exist");
        assertTrue(content.contains("SPDX-License-Identifier: GPL-2.0-or-later"),
                "ModernTableSorter should have proper license");
        assertTrue(content.contains("TableRowSorter"),
                "ModernTableSorter should use standard Swing TableRowSorter");
        assertFalse(content.contains("JavaPro"),
                "ModernTableSorter should not reference unlicensed sources");
    }

    @Test
    @DisplayName("All sorting source files should use only standard Swing APIs")
    void testUsesStandardSwingAPIs() throws IOException {
        String jsortContent = readFile(SRC_DIR + "/JSortTable.java");
        String modernContent = readFile(SRC_DIR + "/ModernTableSorter.java");

        assertTrue(jsortContent.contains("javax.swing"),
                "Sorting implementation should use standard Swing");
        assertTrue(modernContent.contains("TableRowSorter"),
                "Modern implementation should use standard TableRowSorter");
    }

    @Test
    @DisplayName("Deprecated classes should provide migration guidance")
    void testDeprecationGuidance() throws IOException {
        String headerRendererContent = readFile(SRC_DIR + "/SortHeaderRenderer.java");
        String arrowIconContent = readFile(SRC_DIR + "/SortArrowIcon.java");

        assertTrue(headerRendererContent.contains("@Deprecated"),
                "SortHeaderRenderer should be marked @Deprecated");
        assertTrue(headerRendererContent.contains("JSortTable"),
                "SortHeaderRenderer should reference JSortTable as replacement");

        assertTrue(arrowIconContent.contains("@Deprecated"),
                "SortArrowIcon should be marked @Deprecated");
        assertTrue(arrowIconContent.contains("TableRowSorter"),
                "SortArrowIcon should reference TableRowSorter as replacement");
    }

    /**
     * Read a file and return its contents as a string.
     *
     * @param filePath Path to the file
     * @return File contents
     * @throws IOException If file cannot be read
     */
    private String readFile(String filePath) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(filePath));
        return new String(encoded, StandardCharsets.UTF_8);
    }
}
