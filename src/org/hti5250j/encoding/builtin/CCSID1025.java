/*
 * SPDX-FileCopyrightText: Copyright (c) 2001,2009
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-FileContributor: master_jaf
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding.builtin;

import org.hti5250j.encoding.CCSIDFactory;

/**
 * EBCDIC CECP: Cyrillic (CCSID 1025) character encoding.
 *
 * Phase 3: Migrated to use factory pattern with JSON configuration.
 * This class now delegates to CCSIDFactory for character mapping.
 *
 * @author master_jaf
 *
 * @deprecated Use CCSIDFactory.getConverter("1025") directly.
 * This wrapper will be removed in a future release.
 */
@Deprecated(since = "Phase 3", forRemoval = true)
public final class CCSID1025 extends CodepageConverterAdapter {

    public final static String NAME = "1025";
    public final static String DESCR = "CECP: Cyrillic";

    private final CodepageConverterAdapter delegate;

    /**
     * Create a CCSID1025 converter using the factory pattern.
     * Delegates to CCSIDFactory which loads mappings from JSON configuration.
     */
    public CCSID1025() {
        this.delegate = CCSIDFactory.getConverter("1025");
        if (this.delegate == null) {
            throw new RuntimeException("CCSID1025 mappings not found in factory");
        }
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    public String getEncoding() {
        return NAME;
    }

    @Override
    public CodepageConverterAdapter init() {
        delegate.init();
        return this;
    }

    @Override
    public char ebcdic2uni(int index) {
        return delegate.ebcdic2uni(index);
    }

    @Override
    public byte uni2ebcdic(char index) {
        return delegate.uni2ebcdic(index);
    }

    @Override
    protected char[] getCodePage() {
        // This method is abstract in CodepageConverterAdapter and must be implemented
        // but we delegate all conversion work to the factory converter
        throw new UnsupportedOperationException("Use factory converter directly");
    }

    @Override
    public boolean isDoubleByteActive() {
        return delegate.isDoubleByteActive();
    }

    @Override
    public boolean secondByteNeeded() {
        return delegate.secondByteNeeded();
    }
}
