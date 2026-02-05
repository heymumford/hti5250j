package org.tn5250j.keyboard;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration holder for keyboard remapping settings.
 */
public class KeyboardRemappingConfiguration {
    private Map<String, Object> settings = new HashMap<>();

    public Object getSetting(String key) {
        return settings.get(key);
    }

    public void setSetting(String key, Object value) {
        settings.put(key, value);
    }
}
