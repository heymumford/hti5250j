package org.hti5250j.keyboard;

/**
 * Custom exception for keyboard remapping conflicts.
 */
public class KeyRemappingConflictException extends RuntimeException {
    public KeyRemappingConflictException(String message) {
        super(message);
    }
}
