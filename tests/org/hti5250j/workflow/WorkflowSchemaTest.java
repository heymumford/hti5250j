package org.hti5250j.workflow;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class WorkflowSchemaTest {

    private Yaml createYaml() {
        return new Yaml();
    }

    @Test
    void testParseMinimalWorkflow() {
        String yaml = """
            name: Login Test
            description: Simple login workflow
            environment: dev
            steps:
              - action: LOGIN
                host: i5.example.com
                user: testuser
                password: testpass
            """;

        Yaml yml = createYaml();
        WorkflowSchema workflow = yml.loadAs(yaml, WorkflowSchema.class);

        assertThat(workflow.getName()).isEqualTo("Login Test");
        assertThat(workflow.getSteps()).hasSize(1);

        StepDef step = workflow.getSteps().get(0);
        assertThat(step.getAction()).isEqualTo(ActionType.LOGIN);
        assertThat(step.getHost()).isEqualTo("i5.example.com");
    }

    @Test
    void testParseComplexWorkflow() {
        String yaml = """
            name: Payment Flow
            description: End-to-end payment workflow
            environment: prod
            steps:
              - action: LOGIN
                host: i5.prod.com
                user: ${env.user}
                password: ${env.password}
              - action: NAVIGATE
                screen: menu_screen
              - action: FILL
                fields:
                  account: ${data.account}
                  amount: ${data.amount}
              - action: SUBMIT
                key: ENTER
              - action: ASSERT
                screen: confirmation_screen
                text: "Transaction approved"
            """;

        Yaml yml = createYaml();
        WorkflowSchema workflow = yml.loadAs(yaml, WorkflowSchema.class);

        assertThat(workflow.getSteps()).hasSize(5);
        assertThat(workflow.getSteps().get(1).getAction()).isEqualTo(ActionType.NAVIGATE);
        assertThat(workflow.getSteps().get(2).getFields()).containsKeys("account", "amount");
    }
}
