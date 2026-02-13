package org.hti5250j.workflow;

import java.io.File;
import java.io.FileReader;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class DatasetLoader {

    /**
     * Load CSV file into Map<rowKey, Map<columnName, value>>.
     * Uses first column as row key.
     *
     * @param csvFile the CSV file to load
     * @return map with row keys and column values
     * @throws Exception if file not found or CSV parsing fails
     */
    public Map<String, Map<String, String>> loadCSV(File csvFile) throws Exception {
        if (csvFile == null) {
            throw new IllegalArgumentException("CSV file cannot be null");
        }

        if (!csvFile.exists()) {
            throw new IllegalArgumentException("CSV file not found: " + csvFile.getAbsolutePath());
        }

        Map<String, Map<String, String>> result = new LinkedHashMap<>();

        try (FileReader reader = new FileReader(csvFile);
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {

            for (CSVRecord record : parser) {
                try {
                    String rowKey = record.get(0);
                    Map<String, String> row = new LinkedHashMap<>();

                    for (String header : parser.getHeaderNames()) {
                        row.put(header, record.get(header));
                    }

                    result.put(rowKey, row);
                } catch (Exception recordEx) {
                    throw new IllegalArgumentException(
                        "Failed to parse CSV record at line " + record.getRecordNumber() +
                        " in file: " + csvFile.getAbsolutePath(), recordEx);
                }
            }
        } catch (java.io.IOException ioEx) {
            throw new IllegalArgumentException(
                "Failed to read CSV file: " + csvFile.getAbsolutePath(), ioEx);
        } catch (IllegalArgumentException argEx) {
            throw argEx;
        } catch (Exception parseEx) {
            throw new IllegalArgumentException(
                "Failed to parse CSV file: " + csvFile.getAbsolutePath(), parseEx);
        }

        return result;
    }

    /**
     * Replace ${data.fieldName} placeholders with values from data map.
     * Unmapped placeholders remain unchanged.
     * Null values in the map are replaced with "null" string.
     *
     * @param template the template string with ${data.X} placeholders
     * @param data the data map to substitute from
     * @return the string with placeholders replaced
     * @throws IllegalArgumentException if template or data is null
     */
    public String replaceParameters(String template, Map<String, String> data) {
        if (template == null) {
            throw new IllegalArgumentException("Template cannot be null");
        }
        if (data == null) {
            throw new IllegalArgumentException("Data map cannot be null");
        }

        String result = template;

        for (Map.Entry<String, String> entry : data.entrySet()) {
            String placeholder = "${data." + entry.getKey() + "}";
            String value = entry.getValue();
            if (value == null) {
                result = result.replace(placeholder, "null");
            } else {
                result = result.replace(placeholder, value);
            }
        }

        return result;
    }
}
