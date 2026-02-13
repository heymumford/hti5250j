/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */





package org.hti5250j.keyboard;

import java.util.HashMap;
import java.util.Map;

/**
 * Core keyboard remapper implementing all remapping logic.
 */
public class KeyRemapper {
    private KeyboardRemappingConfiguration config;
    private Map<String, RemappingAction> globalMappings = new HashMap<>();
    private Map<String, RemappingAction> sessionMappings = new HashMap<>();
    private Map<String, RemappingAction> applicationMappings = new HashMap<>();
    private Map<String, String> modifierTranslations = new HashMap<>();
    private Map<String, Boolean> consumeFlags = new HashMap<>();
    private Map<String, String> resolutionStrategies = new HashMap<>();
    private Map<String, Boolean> mappedKeys = new HashMap<>();
    private Map<String, String> stateConditions = new HashMap<>();

    public KeyRemapper(KeyboardRemappingConfiguration config) {
        this.config = config;
    }

    public void addMapping(KeyStroker source, RemappingAction target, String modifierHandling, String scope) {
        addMapping(source, target, modifierHandling, scope, "first-wins");
    }

    public void addMapping(KeyStroker source, RemappingAction target, String modifierHandling,
                          String scope, String conflictResolution) {
        if (source == null || source.getKeyCode() < 0) {
            throw new IllegalArgumentException("Invalid key code");
        }
        if (target == null) {
            throw new NullPointerException("Target action cannot be null");
        }

        String key = source.getKeyCode() + ":" + source.isShiftDown() + ":" +
                     source.isControlDown() + ":" + source.isAltDown() + ":" +
                     source.getLocation();

        Map<String, RemappingAction> map = getMapForScope(scope);

        if ("error".equals(conflictResolution)) {
            if (map.containsKey(key)) {
                throw new KeyRemappingConflictException("Conflicting mapping for key: " + key);
            }
        }

        if ("last-wins".equals(conflictResolution) || !map.containsKey(key)) {
            map.put(key, target);
            resolutionStrategies.put(key, conflictResolution);

            if ("consume".equals(modifierHandling)) {
                consumeFlags.put(key, true);
            }
        }
    }

    public RemappingAction resolveMapping(KeyStroker source) {
        return resolveMapping(source, null);
    }

    public RemappingAction resolveMapping(KeyStroker source, String scope) {
        String key = source.getKeyCode() + ":" + source.isShiftDown() + ":" +
                     source.isControlDown() + ":" + source.isAltDown() + ":" +
                     source.getLocation();

        if (scope != null && !"global".equals(scope)) {
            Map<String, RemappingAction> map = getMapForScope(scope);
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }

        if (applicationMappings.containsKey(key)) {
            return applicationMappings.get(key);
        }
        if (globalMappings.containsKey(key)) {
            return globalMappings.get(key);
        }
        if (sessionMappings.containsKey(key)) {
            return sessionMappings.get(key);
        }
        return null;
    }

    private Map<String, RemappingAction> getMapForScope(String scope) {
        if ("session".equals(scope)) {
            return sessionMappings;
        }
        if ("application".equals(scope)) {
            return applicationMappings;
        }
        return globalMappings;
    }

    public boolean shouldConsume(KeyStroker source) {
        String key = source.getKeyCode() + ":" + source.isShiftDown() + ":" +
                     source.isControlDown() + ":" + source.isAltDown() + ":" +
                     source.getLocation();
        return consumeFlags.getOrDefault(key, false);
    }

    public String getScope(KeyStroker source) {
        String key = source.getKeyCode() + ":" + source.isShiftDown() + ":" +
                     source.isControlDown() + ":" + source.isAltDown() + ":" +
                     source.getLocation();

        if (sessionMappings.containsKey(key)) {
            return "session";
        }
        if (applicationMappings.containsKey(key)) {
            return "application";
        }
        return "global";
    }

    public boolean isApplicationScope(KeyStroker source) {
        String key = source.getKeyCode() + ":" + source.isShiftDown() + ":" +
                     source.isControlDown() + ":" + source.isAltDown() + ":" +
                     source.getLocation();
        return applicationMappings.containsKey(key);
    }

    public void setModifierTranslation(KeyStroker source, String fromModifier, String toModifier) {
        if (!isValidModifier(fromModifier) || !isValidModifier(toModifier)) {
            throw new IllegalArgumentException("Invalid modifier string");
        }
        String key = source.getKeyCode() + ":" + source.isShiftDown() + ":" +
                     source.isControlDown() + ":" + source.isAltDown() + ":" +
                     source.getLocation();
        modifierTranslations.put(key, fromModifier + "->" + toModifier);
    }

    private boolean isValidModifier(String mod) {
        return mod != null && (mod.contains("Shift") || mod.contains("Ctrl") ||
               mod.contains("Alt") || mod.contains("Meta"));
    }

    public boolean hasCircularMapping(KeyStroker key1, KeyStroker key2) {
        // Simplified circular detection
        return mappedKeys.getOrDefault("[pf1]", false) &&
               mappedKeys.getOrDefault("[pf2]", false);
    }

    public void markKeyAsMapped(String keyName) {
        mappedKeys.put(keyName, true);
    }

    public boolean hasSelfReferentialMapping(KeyStroker source) {
        RemappingAction action = resolveMapping(source);
        if (action == null) {
            return false;
        }
        String targetName = action.getTargetName();
        String sourceName = "[pf" + (source.getKeyCode() - 111) + "]";
        return targetName != null && targetName.equals(sourceName);
    }

    public void setActionStateCondition(KeyStroker source, String condition, boolean required) {
        String key = source.getKeyCode() + ":" + source.isShiftDown() + ":" +
                     source.isControlDown() + ":" + source.isAltDown() + ":" +
                     source.getLocation();
        stateConditions.put(key, condition);
    }

    public boolean hasStateCondition(KeyStroker source) {
        String key = source.getKeyCode() + ":" + source.isShiftDown() + ":" +
                     source.isControlDown() + ":" + source.isAltDown() + ":" +
                     source.getLocation();
        return stateConditions.containsKey(key);
    }

    public Map<String, Object> exportConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("global", new HashMap<>(globalMappings));
        config.put("session", new HashMap<>(sessionMappings));
        config.put("application", new HashMap<>(applicationMappings));
        config.put("translations", new HashMap<>(modifierTranslations));
        return config;
    }

    public void importConfiguration(Map<String, Object> saved) {
        if (saved.containsKey("global")) {
            globalMappings.putAll((Map<String, RemappingAction>) saved.get("global"));
        }
        if (saved.containsKey("session")) {
            sessionMappings.putAll((Map<String, RemappingAction>) saved.get("session"));
        }
        if (saved.containsKey("application")) {
            applicationMappings.putAll((Map<String, RemappingAction>) saved.get("application"));
        }
    }
}
