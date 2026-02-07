package org.hti5250j.plugin;

/**
 * ProtocolFilterPlugin - Extension point for protocol data filtering.
 *
 * Plugins implement this to inspect and modify protocol streams:
 * - Security filtering
 * - Data logging
 * - Performance instrumentation
 * - Custom protocol extensions
 */
public interface ProtocolFilterPlugin extends HTI5250jPlugin {

    /**
     * Process incoming data from server before normal parsing.
     * May return modified data or null to block.
     *
     * @param data raw bytes from server
     * @return modified data or null to block processing
     */
    byte[] filterIncoming(byte[] data);

    /**
     * Process outgoing data to server before transmission.
     * May return modified data.
     *
     * @param data raw bytes to send to server
     * @return modified data
     */
    byte[] filterOutgoing(byte[] data);

    /**
     * Get filter order (higher = applied first).
     * Range: 0-100, where 50 is normal.
     */
    int getFilterOrder();

    /**
     * Check if this filter is enabled
     */
    boolean isEnabled();
}
