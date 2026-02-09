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
     */
    public Map<String, Map<String, String>> loadCSV(File csvFile) throws Exception {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();

        try (FileReader reader = new FileReader(csvFile);
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {

            for (CSVRecord record : parser) {
                // Use first column value as row key
                String rowKey = record.get(0);
                Map<String, String> row = new LinkedHashMap<>();

                // Populate all columns for this row
                for (String header : parser.getHeaderNames()) {
                    row.put(header, record.get(header));
                }

                result.put(rowKey, row);
            }
        }

        return result;
    }

    /**
     * Replace ${data.fieldName} placeholders with values from data map.
     * Unmapped placeholders remain unchanged.
     * Null values in the map are replaced with "null" string.
     */
    public String replaceParameters(String template, Map<String, String> data) {
        String result = template;

        for (Map.Entry<String, String> entry : data.entrySet()) {
            String placeholder = "${data." + entry.getKey() + "}";
            String value = entry.getValue();
            // Handle null values safely - replace with "null" string
            if (value == null) {
                result = result.replace(placeholder, "null");
            } else {
                result = result.replace(placeholder, value);
            }
        }

        return result;
    }
}
