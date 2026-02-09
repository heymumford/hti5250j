/*
 * SPDX-FileCopyrightText: 2026 Eric C. Mumford <ericmumford@outlook.com>
 *
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.hti5250j.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ArgumentParser Tests")
class ArgumentParserTest {

    @Test
    @DisplayName("parse() should extract action and workflow file")
    void testParseBasic() {
        ArgumentParser result = ArgumentParser.parse(new String[]{"run", "workflow.yaml"});

        assertEquals("run", result.action());
        assertEquals("workflow.yaml", result.workflowFile());
        assertNull(result.dataFile());
        assertNull(result.environment());
    }

    @Test
    @DisplayName("parse() should extract --data optional argument")
    void testParseWithData() {
        ArgumentParser result = ArgumentParser.parse(new String[]{"validate", "workflow.yaml", "--data", "data.csv"});

        assertEquals("validate", result.action());
        assertEquals("workflow.yaml", result.workflowFile());
        assertEquals("data.csv", result.dataFile());
        assertNull(result.environment());
    }

    @Test
    @DisplayName("parse() should extract --env optional argument")
    void testParseWithEnvironment() {
        ArgumentParser result = ArgumentParser.parse(new String[]{"simulate", "workflow.yaml", "--env", "production"});

        assertEquals("simulate", result.action());
        assertEquals("workflow.yaml", result.workflowFile());
        assertNull(result.dataFile());
        assertEquals("production", result.environment());
    }

    @Test
    @DisplayName("parse() should extract both --data and --env arguments")
    void testParseWithBoth() {
        ArgumentParser result = ArgumentParser.parse(new String[]{"run", "workflow.yaml", "--data", "data.csv", "--env", "staging"});

        assertEquals("run", result.action());
        assertEquals("workflow.yaml", result.workflowFile());
        assertEquals("data.csv", result.dataFile());
        assertEquals("staging", result.environment());
    }

    @Test
    @DisplayName("parse() should handle --env before --data")
    void testParseArgumentOrder() {
        ArgumentParser result = ArgumentParser.parse(new String[]{"run", "workflow.yaml", "--env", "dev", "--data", "data.csv"});

        assertEquals("run", result.action());
        assertEquals("workflow.yaml", result.workflowFile());
        assertEquals("data.csv", result.dataFile());
        assertEquals("dev", result.environment());
    }

    @Test
    @DisplayName("parse() should throw when missing workflow file")
    void testParseMissingWorkflow() {
        assertThrows(IllegalArgumentException.class, () -> ArgumentParser.parse(new String[]{"run"}));
    }

    @Test
    @DisplayName("parse() should throw when no arguments provided")
    void testParseNoArguments() {
        assertThrows(IllegalArgumentException.class, () -> ArgumentParser.parse(new String[]{}));
    }

    @Test
    @DisplayName("parse() should ignore --data without value")
    void testParseDataWithoutValue() {
        ArgumentParser result = ArgumentParser.parse(new String[]{"run", "workflow.yaml", "--data"});

        assertEquals("run", result.action());
        assertNull(result.dataFile());
    }

    @Test
    @DisplayName("parse() should ignore --env without value")
    void testParseEnvWithoutValue() {
        ArgumentParser result = ArgumentParser.parse(new String[]{"run", "workflow.yaml", "--env"});

        assertEquals("run", result.action());
        assertNull(result.environment());
    }

    @Test
    @DisplayName("validate() should succeed for 'run' action")
    void testValidateRunAction() {
        ArgumentParser parser = new ArgumentParser("run", "workflow.yaml", null, null);
        assertDoesNotThrow(parser::validate);
    }

    @Test
    @DisplayName("validate() should succeed for 'validate' action")
    void testValidateValidateAction() {
        ArgumentParser parser = new ArgumentParser("validate", "workflow.yaml", null, null);
        assertDoesNotThrow(parser::validate);
    }

    @Test
    @DisplayName("validate() should succeed for 'simulate' action")
    void testValidateSimulateAction() {
        ArgumentParser parser = new ArgumentParser("simulate", "workflow.yaml", null, null);
        assertDoesNotThrow(parser::validate);
    }

    @Test
    @DisplayName("validate() should throw for unknown action")
    void testValidateUnknownAction() {
        ArgumentParser parser = new ArgumentParser("unknown", "workflow.yaml", null, null);
        assertThrows(IllegalArgumentException.class, parser::validate);
    }

    @Test
    @DisplayName("validate() should throw for invalid action with helpful message")
    void testValidateInvalidActionMessage() {
        ArgumentParser parser = new ArgumentParser("delete", "workflow.yaml", null, null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, parser::validate);
        assertTrue(ex.getMessage().contains("Unknown action"));
        assertTrue(ex.getMessage().contains("delete"));
    }

    @Test
    @DisplayName("Record should be immutable - action field")
    void testImmutableAction() {
        ArgumentParser parser = new ArgumentParser("run", "workflow.yaml", null, null);
        assertEquals("run", parser.action());
        // Record fields are final, so no setter exists
    }

    @Test
    @DisplayName("Record should be immutable - workflowFile field")
    void testImmutableWorkflowFile() {
        ArgumentParser parser = new ArgumentParser("run", "workflow.yaml", null, null);
        assertEquals("workflow.yaml", parser.workflowFile());
    }

    @Test
    @DisplayName("equals() should compare records by value")
    void testEquality() {
        ArgumentParser parser1 = new ArgumentParser("run", "workflow.yaml", "data.csv", "prod");
        ArgumentParser parser2 = new ArgumentParser("run", "workflow.yaml", "data.csv", "prod");
        ArgumentParser parser3 = new ArgumentParser("run", "workflow.yaml", "data.csv", "dev");

        assertEquals(parser1, parser2);
        assertNotEquals(parser1, parser3);
    }

    @Test
    @DisplayName("hashCode() should be consistent for equal records")
    void testHashCode() {
        ArgumentParser parser1 = new ArgumentParser("run", "workflow.yaml", "data.csv", "prod");
        ArgumentParser parser2 = new ArgumentParser("run", "workflow.yaml", "data.csv", "prod");

        assertEquals(parser1.hashCode(), parser2.hashCode());
    }
}
