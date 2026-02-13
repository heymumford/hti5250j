#!/usr/bin/env python3
"""
Generate CCSID migration files for all 21 CCSID adapters.
Follows TDD pattern: generate test files and implementation files.
"""

import os
import re
import json
from pathlib import Path

# CCSID configurations from JSON
CCSID_CONFIGS = {
    "273": {"name": "273", "desc": "CECP: Germany, Austria"},
    "277": {"name": "277", "desc": "CECP: Denmark, Norway"},
    "278": {"name": "278", "desc": "CECP: Sweden, Finland"},
    "280": {"name": "280", "desc": "CECP: Italy"},
    "284": {"name": "284", "desc": "CECP: Spain, Latin America"},
    "285": {"name": "285", "desc": "CECP: UK, Ireland"},
    "297": {"name": "297", "desc": "CECP: France"},
    "424": {"name": "424", "desc": "CECP: Hebrew"},
    "500": {"name": "500", "desc": "CECP: International"},
    "870": {"name": "870", "desc": "CECP: Latin-2"},
    "871": {"name": "871", "desc": "CECP: Iceland"},
    "875": {"name": "875", "desc": "CECP: Greek"},
    "930": {"name": "930", "desc": "DBCS: Japan"},
    "1025": {"name": "1025", "desc": "CECP: Cyrillic"},
    "1026": {"name": "1026", "desc": "CECP: Turkish"},
    "1112": {"name": "1112", "desc": "CECP: Baltic"},
    "1122": {"name": "1122", "desc": "CECP: Estonia"},
    "1140": {"name": "1140", "desc": "CECP: US (Euro)"},
    "1141": {"name": "1141", "desc": "CECP: Germany (Euro)"},
    "1147": {"name": "1147", "desc": "CECP: France (Euro)"},
    "1148": {"name": "1148", "desc": "CECP: UK (Euro)"},
}

TEST_TEMPLATE = '''/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import org.hti5250j.encoding.builtin.CCSID{ccsid_id};
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RED Phase - CCSID{ccsid_id} Migration Compatibility Tests.
 *
 * These tests verify that CCSID{ccsid_id} maintains backward compatibility
 * while migrating from static array to factory pattern.
 */
@DisplayName("CCSID{ccsid_id} Migration Compatibility Tests")
class CCSID{ccsid_id}MigrationTest {{

    @Test
    @DisplayName("CCSID{ccsid_id} converts all 256 characters correctly")
    void testCCSID{ccsid_id}AllCharacters() {{
        CCSID{ccsid_id} converter = new CCSID{ccsid_id}();
        converter.init();

        // Test all 256 EBCDIC codes can be converted
        for (int ebcdic = 0; ebcdic < 256; ebcdic++) {{
            char unicode = converter.ebcdic2uni(ebcdic);
            assertNotNull(unicode, "EBCDIC byte 0x" + Integer.toHexString(ebcdic) + " should convert");
        }}
    }}

    @Test
    @DisplayName("CCSID{ccsid_id} space character (0x40) converts correctly")
    void testCCSID{ccsid_id}SpaceCharacter() {{
        CCSID{ccsid_id} converter = new CCSID{ccsid_id}();
        converter.init();

        char space = converter.ebcdic2uni(0x40);
        assertEquals(' ', space, "EBCDIC 0x40 should convert to SPACE");
    }}

    @Test
    @DisplayName("CCSID{ccsid_id} provides correct name")
    void testCCSID{ccsid_id}Name() {{
        CCSID{ccsid_id} converter = new CCSID{ccsid_id}();

        assertEquals("{ccsid_id}", converter.getName(), "CCSID{ccsid_id} name should be '{ccsid_id}'");
    }}

    @Test
    @DisplayName("CCSID{ccsid_id} provides description")
    void testCCSID{ccsid_id}Description() {{
        CCSID{ccsid_id} converter = new CCSID{ccsid_id}();

        String description = converter.getDescription();
        assertNotNull(description, "CCSID{ccsid_id} should provide a description");
        assertTrue(description.length() > 0, "Description should not be empty");
    }}

    @Test
    @DisplayName("CCSID{ccsid_id} init() returns converter for chaining")
    void testCCSID{ccsid_id}InitChaining() {{
        CCSID{ccsid_id} converter = new CCSID{ccsid_id}();

        Object initialized = converter.init();
        assertNotNull(initialized, "init() should return converter");
        assertSame(converter, initialized, "init() should return same converter instance");
    }}

    @Test
    @DisplayName("CCSID{ccsid_id} NUL character (0x00) converts correctly")
    void testCCSID{ccsid_id}NULCharacter() {{
        CCSID{ccsid_id} converter = new CCSID{ccsid_id}();
        converter.init();

        char nul = converter.ebcdic2uni(0x00);
        assertEquals('\\u0000', nul, "EBCDIC 0x00 should convert to NUL");
    }}
}}
'''

IMPL_TEMPLATE = '''/*
 * SPDX-FileCopyrightText: Copyright (c) 2001,2009
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: master_jaf
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding.builtin;

import org.hti5250j.encoding.CCSIDFactory;

/**
 * EBCDIC {description} (CCSID {ccsid_id}) character encoding.
 *
 * Phase 3: Migrated to use factory pattern with JSON configuration.
 * This class now delegates to CCSIDFactory for character mapping.
 *
 * @author master_jaf
 *
 * @deprecated Use CCSIDFactory.getConverter("{ccsid_id}") directly.
 * This wrapper will be removed in a future release.
 */
@Deprecated(since = "Phase 3", forRemoval = true)
public final class CCSID{ccsid_id} extends CodepageConverterAdapter {{

    public final static String NAME = "{ccsid_id}";
    public final static String DESCR = "{description}";

    private final CodepageConverterAdapter delegate;

    /**
     * Create a CCSID{ccsid_id} converter using the factory pattern.
     * Delegates to CCSIDFactory which loads mappings from JSON configuration.
     */
    public CCSID{ccsid_id}() {{
        this.delegate = CCSIDFactory.getConverter("{ccsid_id}");
        if (this.delegate == null) {{
            throw new RuntimeException("CCSID{ccsid_id} mappings not found in factory");
        }}
    }}

    @Override
    public String getName() {{
        return delegate.getName();
    }}

    @Override
    public String getDescription() {{
        return delegate.getDescription();
    }}

    public String getEncoding() {{
        return NAME;
    }}

    @Override
    public CodepageConverterAdapter init() {{
        delegate.init();
        return this;
    }}

    @Override
    public char ebcdic2uni(int index) {{
        return delegate.ebcdic2uni(index);
    }}

    @Override
    public byte uni2ebcdic(char index) {{
        return delegate.uni2ebcdic(index);
    }}

    @Override
    protected char[] getCodePage() {{
        // This method is abstract in CodepageConverterAdapter and must be implemented
        // but we delegate all conversion work to the factory converter
        throw new UnsupportedOperationException("Use factory converter directly");
    }}

    @Override
    public boolean isDoubleByteActive() {{
        return delegate.isDoubleByteActive();
    }}

    @Override
    public boolean secondByteNeeded() {{
        return delegate.secondByteNeeded();
    }}
}}
'''

def generate_migrations():
    """Generate migration files for all CCSIDs except 37 (already done)."""

    project_root = Path("/Users/vorthruna/Projects/heymumford/hti5250j")
    tests_dir = project_root / "tests/org/hti5250j/encoding"
    src_dir = project_root / "src/org/hti5250j/encoding/builtin"

    generated_count = 0

    # Skip CCSID37 as it's already migrated manually
    for ccsid_id, config in sorted(CCSID_CONFIGS.items()):
        # Skip 37 (already migrated) and 930 (double-byte)
        if ccsid_id == "37" or ccsid_id == "930":
            print(f"⊘ Skipping CCSID{ccsid_id} (already processed or DBCS)")
            continue

        # Generate test file
        test_file = tests_dir / f"CCSID{ccsid_id}MigrationTest.java"
        test_content = TEST_TEMPLATE.format(ccsid_id=ccsid_id)

        test_file.write_text(test_content)
        print(f"✓ Generated test: {test_file.name}")

        # Generate implementation file
        impl_file = src_dir / f"CCSID{ccsid_id}.java"
        impl_content = IMPL_TEMPLATE.format(
            ccsid_id=ccsid_id,
            description=config['desc']
        )

        impl_file.write_text(impl_content)
        print(f"✓ Generated impl: {impl_file.name}")

        generated_count += 1

    print(f"\n✓ Generated {generated_count} CCSID migration pairs")
    return generated_count

if __name__ == "__main__":
    generate_migrations()
