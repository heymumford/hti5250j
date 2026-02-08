package org.hti5250j.workflow;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;

class DatasetLoaderTest {

    @Test
    void testLoadCSVBindsColumns(@TempDir File tempDir) throws Exception {
        // Write CSV file
        File csv = new File(tempDir, "data.csv");
        try (FileWriter fw = new FileWriter(csv)) {
            fw.write("account_id,amount,description\n");
            fw.write("123,100.00,Payment\n");
            fw.write("456,250.50,Refund\n");
        }

        DatasetLoader loader = new DatasetLoader();
        Map<String, Map<String, String>> data = loader.loadCSV(csv);

        assertThat(data).hasSize(2);
        assertThat(data.get("123")).containsEntry("account_id", "123")
                                   .containsEntry("amount", "100.00")
                                   .containsEntry("description", "Payment");
        assertThat(data.get("456")).containsEntry("account_id", "456")
                                   .containsEntry("amount", "250.50")
                                   .containsEntry("description", "Refund");
    }

    @Test
    void testReplaceParametersSubstitutesValues() {
        DatasetLoader loader = new DatasetLoader();
        Map<String, String> data = Map.of(
            "account", "ACC-999",
            "amount", "1500.00"
        );

        String template = "Account: ${data.account}, Amount: ${data.amount}";
        String result = loader.replaceParameters(template, data);

        assertThat(result).isEqualTo("Account: ACC-999, Amount: 1500.00");
    }

    @Test
    void testReplaceParametersLeavesUnmatchedPlaceholders() {
        DatasetLoader loader = new DatasetLoader();
        Map<String, String> data = Map.of("account", "ACC-999");

        String template = "Account: ${data.account}, User: ${data.user}";
        String result = loader.replaceParameters(template, data);

        // Unmapped parameters remain unchanged
        assertThat(result).isEqualTo("Account: ACC-999, User: ${data.user}");
    }
}
