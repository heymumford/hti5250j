/*
 * SPDX-FileCopyrightText: Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.encoding;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads CCSID character mappings from JSON configuration file.
 *
 * Reads ccsid-mappings.json resource and provides access to 256-character
 * Unicode mapping arrays for each CCSID.
 *
 * @author Wave 3A Agent 1 - Refactoring Task
 */
public class CCSIDMappingLoader {

    private static final String CONFIG_PATH = "ccsid-mappings.json";
    private static final Map<String, CCSIDMapping> MAPPINGS = new HashMap<>();
    private static volatile boolean initialized = false;

    /**
     * Internal data structure for CCSID mappings.
     */
    static class CCSIDMapping {
        final String ccsidId;
        final String name;
        final String description;
        final char[] codepage;

        CCSIDMapping(String ccsidId, String name, String description, char[] codepage) {
            this.ccsidId = ccsidId;
            this.name = name;
            this.description = description;
            this.codepage = codepage;
        }
    }

    static {
        try {
            loadMappings();
            initialized = true;
        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to load CCSID mappings from " + CONFIG_PATH, e
            );
        }
    }

    /**
     * Load all CCSID mappings from JSON resource file.
     *
     * @throws IOException if resource cannot be read
     */
    private static void loadMappings() throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    CCSIDMappingLoader.class
                        .getResourceAsStream("/" + CONFIG_PATH)
                )
            )) {

            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line).append('\n');
            }

            Gson gson = new Gson();
            JsonObject root = gson.fromJson(json.toString(), JsonObject.class);
            JsonArray mappingsArray = root.getAsJsonArray("ccsid_mappings");

            for (JsonElement element : mappingsArray) {
                JsonObject obj = element.getAsJsonObject();
                String ccsidId = obj.get("ccsid_id").getAsString();
                String name = obj.get("name").getAsString();
                String description = obj.get("description").getAsString();

                // Parse codepage array
                JsonArray codepageArray = obj.getAsJsonArray("codepage");
                char[] codepage = new char[256];
                for (int i = 0; i < 256; i++) {
                    codepage[i] = (char) codepageArray.get(i).getAsInt();
                }

                CCSIDMapping mapping = new CCSIDMapping(
                    ccsidId, name, description, codepage
                );
                MAPPINGS.put(ccsidId, mapping);
            }
        }
    }

    /**
     * Get a character mapping array for the specified CCSID.
     *
     * @param ccsidId CCSID identifier (e.g., "37", "500")
     * @return 256-character Unicode mapping array, or null if CCSID not found
     */
    public static char[] loadToUnicode(String ccsidId) {
        CCSIDMapping mapping = MAPPINGS.get(ccsidId);
        if (mapping == null) {
            return null;
        }
        return mapping.codepage;
    }

    /**
     * Get the description for a specific CCSID.
     *
     * @param ccsidId CCSID identifier
     * @return description string, or null if not found
     */
    public static String getDescription(String ccsidId) {
        CCSIDMapping mapping = MAPPINGS.get(ccsidId);
        if (mapping == null) {
            return null;
        }
        return mapping.description;
    }

    /**
     * Check if a specific CCSID is available.
     *
     * @param ccsidId CCSID identifier
     * @return true if CCSID mapping is loaded
     */
    public static boolean isAvailable(String ccsidId) {
        return MAPPINGS.containsKey(ccsidId);
    }

    /**
     * Get all available CCSID IDs.
     *
     * @return array of CCSID IDs
     */
    public static String[] getAvailableCCSIDs() {
        return MAPPINGS.keySet().toArray(new String[0]);
    }
}
