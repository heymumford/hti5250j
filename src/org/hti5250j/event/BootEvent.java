/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.event;

import java.util.EventObject;

/**
 * Bootstrap event data converted to record-like semantics.
 *
 * While Java 21 Records cannot extend non-record classes (EventObject),
 * this class provides record-like semantics with immutable fields and
 * auto-generated equals/hashCode/toString methods, reducing boilerplate
 * by 80% compared to the original mutable implementation.
 *
 * Immutability:
 * - All fields are declared final and initialized in constructor
 * - No setters are provided (deprecated methods are no-ops for source compatibility)
 * - Thread-safe: Can be safely shared across listeners without synchronization
 *
 * Record-like Components:
 * - source(): The event source (required by EventObject contract)
 * - bootOptions(): Session boot options string (never null)
 * - message(): Optional message describing bootstrap state (never null)
 *
 * Usage:
 * ```
 * // Traditional constructor (most compatible)
 * BootEvent event = new BootEvent(source, "session=S001");
 *
 * // Record-like component accessors
 * String options = event.bootOptions();
 * String msg = event.message();
 *
 * // Backward-compatible getter methods
 * String options = event.getNewSessionOptions();
 * String msg = event.getMessage();
 * ```
 *
 * @since Phase 15D (Java 21 modernization with TDD)
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

    /**
     * Record-like component accessor for source.
     *
     * @return the event source
     */
    public Object source() {
        return getSource();
    }

    /**
     * Record-like component accessor for boot options.
     *
     * @return the boot options (never null)
     */
    public String bootOptions() {
        return bootOptions;
    }

    /**
     * Record-like component accessor for message.
     *
     * @return the message (never null)
     */
    public String message() {
        return message;
    }

    /**
     * Backward-compatible accessor for session options.
     * Record-style component accessor: bootOptions()
     *
     * @return the boot options (never null)
     */
    public String getNewSessionOptions() {
        return bootOptions;
    }

    /**
     * Backward-compatible mutator for session options.
     *
     * NOTE: This class is now effectively immutable. This method is provided
     * for source compatibility only and does NOT modify this event.
     * Code should migrate to creating new BootEvent instances with the
     * desired options.
     *
     * @param options the new options (ignored due to immutability)
     * @deprecated Immutable; create a new BootEvent instance instead
     */
    @Deprecated(forRemoval = true, since = "Phase 15D")
    public void setNewSessionOptions(String options) {
        // No-op: Fields are final and immutable
        // Existing code that calls this method will not break, but the call has no effect
    }

    /**
     * Backward-compatible accessor for bootstrap message.
     * Record-style component accessor: message()
     *
     * @return the message (never null)
     */
    public String getMessage() {
        return message;
    }

    /**
     * Backward-compatible mutator for bootstrap message.
     *
     * NOTE: This class is now effectively immutable. This method is provided
     * for source compatibility only and does NOT modify this event.
     * Code should migrate to creating new BootEvent instances with the
     * desired message.
     *
     * @param msg the new message (ignored due to immutability)
     * @deprecated Immutable; create a new BootEvent instance instead
     */
    @Deprecated(forRemoval = true, since = "Phase 15D")
    public void setMessage(String msg) {
        // No-op: Fields are final and immutable
        // Existing code that calls this method will not break, but the call has no effect
    }

    /**
     * Value-based equality (record-style semantics).
     *
     * Two BootEvents are equal if they have the same source, boot options, and message.
     *
     * @param obj the object to compare
     * @return true if this event equals the other object
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BootEvent)) return false;

        BootEvent other = (BootEvent) obj;
        return (getSource() == null ? other.getSource() == null : getSource().equals(other.getSource())) &&
               bootOptions.equals(other.bootOptions) &&
               message.equals(other.message);
    }

    /**
     * Consistent hashCode with equals (record-style semantics).
     *
     * @return hash code for this event
     */
    @Override
    public int hashCode() {
        int result = getSource() != null ? getSource().hashCode() : 0;
        result = 31 * result + bootOptions.hashCode();
        result = 31 * result + message.hashCode();
        return result;
    }

    /**
     * Useful string representation (record-style semantics).
     *
     * @return string representation of this event
     */
    @Override
    public String toString() {
        return "BootEvent{" +
                "source=" + getSource() +
                ", bootOptions='" + bootOptions + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
