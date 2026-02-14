/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CodepageConverterAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * GREEN Phase Tests for CCSID Factory Pattern.
 *
 * Verifies that CCSIDFactory and ConfigurableCodepageConverter
 * exist and produce valid converters for known CCSIDs.
 */
@DisplayName("CCSID Factory - GREEN Phase Tests")
class CCSIDFactoryRedTest {

    @Test
    @DisplayName("GREEN: CCSIDFactory creates converter for known CCSID")
    void testFactoryCreatesConverterForKnownCCSID() {
        CodepageConverterAdapter converter = CCSIDFactory.getConverter("37");
        assertNotNull(converter, "CCSIDFactory should produce converter for CCSID 37");
        assertEquals("37", converter.getName());
    }

    @Test
    @DisplayName("GREEN: CCSIDFactory returns null for unknown CCSID")
    void testFactoryReturnsNullForUnknownCCSID() {
        CodepageConverterAdapter converter = CCSIDFactory.getConverter("99999");
        assertNull(converter, "CCSIDFactory should return null for unknown CCSID");
    }
}
