/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.plugin;

/**
 * PluginVersion - API version contract for plugin compatibility.
 * Supports semantic versioning: MAJOR.MINOR.PATCH
 */
public final class PluginVersion implements Comparable<PluginVersion> {
    public static final PluginVersion CURRENT = new PluginVersion(1, 0, 0);
    public static final PluginVersion LEGACY = new PluginVersion(0, 9, 0);
    public static final PluginVersion FUTURE = new PluginVersion(2, 0, 0);

    private final int major;
    private final int minor;
    private final int patch;

    public PluginVersion(int major, int minor, int patch) {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Version components must be non-negative");
        }
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public int getMajor() { return major; }
    public int getMinor() { return minor; }
    public int getPatch() { return patch; }

    public boolean isCompatibleWith(PluginVersion other) {
        return this.major == other.major;
    }

    @Override
    public int compareTo(PluginVersion o) {
        if (this.major != o.major) return this.major - o.major;
        if (this.minor != o.minor) return this.minor - o.minor;
        return this.patch - o.patch;
    }

    @Override
    public String toString() {
        return String.format("%d.%d.%d", major, minor, patch);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PluginVersion)) return false;
        PluginVersion v = (PluginVersion) o;
        return this.major == v.major && this.minor == v.minor && this.patch == v.patch;
    }

    @Override
    public int hashCode() {
        return (major * 10000) + (minor * 100) + patch;
    }
}
