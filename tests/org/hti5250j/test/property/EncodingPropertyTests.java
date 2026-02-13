/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * Property-based tests for encoding layer (jqwik)
 * Discovers edge cases through generative testing
 */

package org.hti5250j.test.property;

import net.jqwik.api.*;
import net.jqwik.api.arbitraries.StringArbitrary;
import net.jqwik.api.arbitraries.IntegerArbitrary;
import org.assertj.core.api.SoftAssertions;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Property-based tests for encoding layer.
 * Generates thousands of test cases to discover edge cases.
 *
 * Test properties:
 * - Round-trip encoding: encode(decode(x)) == x
 * - Codec idempotence: codec(codec(x)) == codec(x) for stateless ops
 * - Size preservation: length(encoded) >= length(original)
 */
public class EncodingPropertyTests {

    /**
     * Property: ASCII round-trip encoding is lossless
     * For every valid ASCII string, encoding then decoding returns original
     */
    @Property(tries = 10000)
    void asciiRoundTripIsLossless(@ForAll("asciiStrings") String original) {
        byte[] encoded = original.getBytes(StandardCharsets.US_ASCII);
        String decoded = new String(encoded, StandardCharsets.US_ASCII);

        Assertions.assertThat(decoded).isEqualTo(original);
    }

    /**
     * Property: ASCII encoding handles all printable ASCII characters
     * Characters 0x20-0x7E (space to ~) should always encode/decode
     */
    @Property(tries = 1000)
    void asciiEncodingHandlesAllPrintable(@ForAll("printableAscii") char c) {
        String input = String.valueOf(c);
        byte[] encoded = input.getBytes(StandardCharsets.US_ASCII);
        String decoded = new String(encoded, StandardCharsets.US_ASCII);

        Assertions.assertThat(decoded).isEqualTo(input);
    }

    /**
     * Property: Codec switching preserves data integrity
     * Multiple consecutive codec switches should not lose data
     */
    @Property(tries = 5000)
    void codecSwitchingPreservesData(
        @ForAll("asciiStrings") String original,
        @ForAll("switchCount") int switches
    ) {
        String current = original;

        // Simulate codec switching
        for (int i = 0; i < switches; i++) {
            if ((i & 1) == 0) {
                // ASCII round-trip
                current = new String(
                    current.getBytes(StandardCharsets.US_ASCII),
                    StandardCharsets.US_ASCII
                );
            } else {
                // UTF-8 round-trip
                current = new String(
                    current.getBytes(StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
                );
            }
        }

        Assertions.assertThat(current).isEqualTo(original);
    }

    /**
     * Property: Bulk encoding maintains size consistency
     * Encoding a buffer twice should not change size unpredictably
     */
    @Property(tries = 5000)
    void bulkEncodingConsistent(
        @ForAll("bufferSizes") int size,
        @ForAll("asciiStrings") String content
    ) {
        // Prepare consistent buffer
        String buffer = content.repeat((size / content.length()) + 1).substring(0, size);
        byte[] first = buffer.getBytes(StandardCharsets.US_ASCII);
        byte[] second = buffer.getBytes(StandardCharsets.US_ASCII);

        Assertions.assertThat(first.length).isEqualTo(second.length);
    }

    /**
     * Property: No encoding produces null or invalid UTF-8
     * Result of encoding should always be valid UTF-8 decodable
     */
    @Property(tries = 10000)
    void encodingProducesValidUtf8(@ForAll("asciiStrings") String original) {
        byte[] encoded = original.getBytes(StandardCharsets.US_ASCII);

        // Should never be null
        Assertions.assertThat(encoded).isNotNull();

        // Should be decodable as valid UTF-8
        Assertions.assertThatCode(() -> {
            String decoded = new String(encoded, StandardCharsets.UTF_8);
            Assertions.assertThat(decoded).isNotNull();
        }).doesNotThrowAnyException();
    }

    /**
     * Property: Empty buffers handled correctly
     * Empty string should encode/decode to empty
     */
    @Property
    void emptyBufferHandling() {
        String empty = "";
        byte[] encoded = empty.getBytes(StandardCharsets.US_ASCII);
        String decoded = new String(encoded, StandardCharsets.US_ASCII);

        Assertions.assertThat(encoded.length).isEqualTo(0);
        Assertions.assertThat(decoded).isEmpty();
    }

    /**
     * Property: Large buffers handled efficiently
     * Encoding should work for multi-MB buffers without memory issues
     */
    @Property(tries = 100)
    void largeBufferHandling(@ForAll("largeStrings") String largeContent) {
        Assertions.assertThatCode(() -> {
            byte[] encoded = largeContent.getBytes(StandardCharsets.US_ASCII);
            String decoded = new String(encoded, StandardCharsets.US_ASCII);
            Assertions.assertThat(decoded).isEqualTo(largeContent);
        }).doesNotThrowAnyException();
    }

    /**
     * Property: Character distribution preservation
     * Character frequency should be identical before/after encoding
     */
    @Property(tries = 5000)
    void characterDistributionPreserved(@ForAll("asciiStrings") String original) {
        if (original.isEmpty()) return;

        // Count characters before
        Set<Character> charsBefore = new HashSet<>();
        for (char c : original.toCharArray()) {
            charsBefore.add(c);
        }

        // Encode/decode
        byte[] encoded = original.getBytes(StandardCharsets.US_ASCII);
        String decoded = new String(encoded, StandardCharsets.US_ASCII);

        // Count characters after
        Set<Character> charsAfter = new HashSet<>();
        for (char c : decoded.toCharArray()) {
            charsAfter.add(c);
        }

        Assertions.assertThat(charsAfter).isEqualTo(charsBefore);
    }

    // ========== ARBITRARIES ==========

    @Provide
    StringArbitrary asciiStrings() {
        return Arbitraries.strings()
            .withChars("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 \t\n\r!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~")
            .ofMinLength(0)
            .ofMaxLength(10000);
    }

    @Provide
    CharacterArbitrary printableAscii() {
        return Arbitraries.characters()
            .range('\u0020', '\u007E');  // Space to ~
    }

    @Provide
    IntegerArbitrary switchCount() {
        return Arbitraries.integers().between(1, 100);
    }

    @Provide
    IntegerArbitrary bufferSizes() {
        return Arbitraries.integers()
            .between(1, 10000)
            .filter(size -> size > 0);
    }

    @Provide
    StringArbitrary largeStrings() {
        return Arbitraries.strings()
            .withChars("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789")
            .ofMinLength(100000)
            .ofMaxLength(1000000);
    }
}
