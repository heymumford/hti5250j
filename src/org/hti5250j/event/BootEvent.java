/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.event;

import java.util.EventObject;

/**
 * Bootstrap event data with immutable fields and record-like semantics.
 * <p>
 * All fields are final and initialized in the constructor. Deprecated setter
 * methods are no-ops retained for source compatibility. Thread-safe for
 * sharing across listeners without synchronization.
 */
public class BootEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    private final String bootOptions;
    private final String message;

    /**
     * Convenience constructor with source only.
     * Initializes boot options and message to empty strings.
     *
     * @param source The event source (must not be null per EventObject contract)
     * @throws NullPointerException if source is null
     */
    public BootEvent(Object source) {
        this(source, "", "");
    }

    /**
     * Constructor with source and boot options.
     * Initializes message to empty string.
     *
     * @param source The event source (must not be null)
     * @param bootOptions Session boot options (may be null, normalized to empty)
     * @throws NullPointerException if source is null
     */
    public BootEvent(Object source, String bootOptions) {
        this(source, bootOptions, "");
    }

    /**
     * Full constructor - canonical form with all components.
     *
     * This is the primary constructor that initializes all components.
     * Validates that source is non-null (required by EventObject contract).
     * Converts null values to empty strings for consistent access patterns.
     *
     * @param source The event source (must not be null per EventObject contract)
     * @param bootOptions Session boot options (may be null, converted to empty string)
     * @param message Bootstrap message (may be null, converted to empty string)
     * @throws NullPointerException if source is null
     */
    public BootEvent(Object source, String bootOptions, String message) {
        super(source);
        if (source == null) {
            throw new NullPointerException("Event source cannot be null");
        }
        // Normalize null values to empty strings for consistent access patterns
        this.bootOptions = bootOptions != null ? bootOptions : "";
        this.message = message != null ? message : "";
    }

    /** @return the event source */
    public Object source() {
        return getSource();
    }

    /** @return the boot options (never null) */
    public String bootOptions() {
        return bootOptions;
    }

    /** @return the message (never null) */
    public String message() {
        return message;
    }

    /** @return the boot options (never null) */
    public String getNewSessionOptions() {
        return bootOptions;
    }

    /**
     * @param options the new options (ignored -- this class is immutable)
     * @deprecated Create a new BootEvent instance instead
     */
    @Deprecated(forRemoval = true)
    public void setNewSessionOptions(String options) {
        // No-op: fields are final and immutable
    }

    /** @return the message (never null) */
    public String getMessage() {
        return message;
    }

    /**
     * @param msg the new message (ignored -- this class is immutable)
     * @deprecated Create a new BootEvent instance instead
     */
    @Deprecated(forRemoval = true)
    public void setMessage(String msg) {
        // No-op: fields are final and immutable
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BootEvent)) {
            return false;
        }

        BootEvent other = (BootEvent) obj;
        return (getSource() == null ? other.getSource() == null : getSource().equals(other.getSource())) &&
               bootOptions.equals(other.bootOptions) &&
               message.equals(other.message);
    }

    @Override
    public int hashCode() {
        int result = getSource() != null ? getSource().hashCode() : 0;
        result = 31 * result + bootOptions.hashCode();
        result = 31 * result + message.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "BootEvent{" +
                "source=" + getSource() +
                ", bootOptions='" + bootOptions + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
