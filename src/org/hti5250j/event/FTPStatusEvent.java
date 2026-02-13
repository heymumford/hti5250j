/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.event;

import java.util.EventObject;
import java.util.Objects;

/**
 * FTP status event with immutable fields and record-like semantics.
 * Extends EventObject for listener compatibility. Thread-safe for
 * distribution across listeners.
 */
public class FTPStatusEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    static final int OK = 0;
    static final int ERROR = 1;
    static final int ERROR_NULLS_ALLOWED = 2;

    private final String message;
    private final int fileLength;
    private final int currentRecord;
    private final int messageType;

    /**
     * Constructs an FTPStatusEvent with source only.
     * Message type defaults to OK (0).
     *
     * @param obj The source object (required)
     * @throws NullPointerException if source is null
     */
    public FTPStatusEvent(Object obj) {
        this(obj, null, 0, 0, OK);
    }

    /**
     * Constructs an FTPStatusEvent with source and message.
     * Message type defaults to OK (0).
     *
     * @param obj The source object (required)
     * @param s The status message (may be null)
     * @throws NullPointerException if source is null
     */
    public FTPStatusEvent(Object obj, String s) {
        this(obj, s, 0, 0, OK);
    }

    /**
     * Constructs an FTPStatusEvent with source, message, and type.
     * File length and current record default to 0.
     *
     * @param obj The source object (required)
     * @param s The status message (may be null)
     * @param type The message type (OK, ERROR, ERROR_NULLS_ALLOWED)
     * @throws NullPointerException if source is null
     */
    public FTPStatusEvent(Object obj, String s, int type) {
        this(obj, s, 0, 0, type);
    }

    /**
     * Canonical constructor for all FTPStatusEvent fields.
     *
     * @param obj The source object (required, from EventObject)
     * @param message The status message (may be null)
     * @param fileLength Total file length in bytes
     * @param currentRecord Current record being processed
     * @param messageType Type of message (OK, ERROR, ERROR_NULLS_ALLOWED)
     * @throws NullPointerException if source is null
     */
    public FTPStatusEvent(Object obj, String message, int fileLength, int currentRecord, int messageType) {
        super(obj);
        this.message = message;
        this.fileLength = fileLength;
        this.currentRecord = currentRecord;
        this.messageType = messageType;
    }

    /** @return the message, or null if not set */
    public String message() {
        return message;
    }

    /** @return the message, or null if not set */
    public String getMessage() {
        return message;
    }

    /**
     * @param s The message to set
     * @deprecated FTPStatusEvent is immutable; create a new instance instead
     */
    @Deprecated(forRemoval = true)
    public void setMessage(String s) {
        throw new UnsupportedOperationException("FTPStatusEvent is immutable; use new FTPStatusEvent(...) to create new events");
    }

    /** @return the message type */
    public int messageType() {
        return messageType;
    }

    /** @return the message type */
    public int getMessageType() {
        return messageType;
    }

    /**
     * @param type The message type to set
     * @deprecated FTPStatusEvent is immutable; create a new instance instead
     */
    @Deprecated(forRemoval = true)
    public void setMessageType(int type) {
        throw new UnsupportedOperationException("FTPStatusEvent is immutable; use new FTPStatusEvent(...) to create new events");
    }

    /** @return the file length */
    public int fileLength() {
        return fileLength;
    }

    /** @return the file length */
    public int getFileLength() {
        return fileLength;
    }

    /**
     * @param len The file length to set
     * @deprecated FTPStatusEvent is immutable; create a new instance instead
     */
    @Deprecated(forRemoval = true)
    public void setFileLength(int len) {
        throw new UnsupportedOperationException("FTPStatusEvent is immutable; use new FTPStatusEvent(...) to create new events");
    }

    /** @return the current record number */
    public int currentRecord() {
        return currentRecord;
    }

    /** @return the current record number */
    public int getCurrentRecord() {
        return currentRecord;
    }

    /**
     * @param current The current record to set
     * @deprecated FTPStatusEvent is immutable; create a new instance instead
     */
    @Deprecated(forRemoval = true)
    public void setCurrentRecord(int current) {
        throw new UnsupportedOperationException("FTPStatusEvent is immutable; use new FTPStatusEvent(...) to create new events");
    }

    /** @return the source object */
    public Object source() {
        return getSource();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FTPStatusEvent)) {
            return false;
        }

        FTPStatusEvent other = (FTPStatusEvent) obj;
        return Objects.equals(getSource(), other.getSource()) &&
               Objects.equals(message, other.message) &&
               fileLength == other.fileLength &&
               currentRecord == other.currentRecord &&
               messageType == other.messageType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSource(), message, fileLength, currentRecord, messageType);
    }

    @Override
    public String toString() {
        return "FTPStatusEvent{" +
                "source=" + getSource() +
                ", message='" + message + '\'' +
                ", fileLength=" + fileLength +
                ", currentRecord=" + currentRecord +
                ", messageType=" + messageType +
                '}';
    }
}
