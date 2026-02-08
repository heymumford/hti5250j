package org.hti5250j.workflow;

import org.yaml.snakeyaml.Yaml;

public class WorkflowYAML {
    public static Yaml getInstance() {
        return new Yaml();
    }
}
