package org.hti5250j.workflow;

import java.util.List;

public class WorkflowSchema {
    private String name;
    private String description;
    private String environment;
    private List<StepDef> steps;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public List<StepDef> getSteps() { return steps; }
    public void setSteps(List<StepDef> steps) { this.steps = steps; }
}
