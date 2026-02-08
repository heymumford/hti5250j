package org.hti5250j.workflow;

import java.util.Map;

public class StepDef {
    private ActionType action;
    private String host;
    private String user;
    private String password;
    private String screen;
    private String key;
    private String text;
    private Map<String, String> fields;
    private Integer timeout;
    private String name;

    public ActionType getAction() { return action; }
    public void setAction(ActionType action) { this.action = action; }

    // Support YAML string-to-enum conversion
    public void setAction(String actionString) {
        if (actionString != null) {
            this.action = ActionType.valueOf(actionString.toUpperCase());
        }
    }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getScreen() { return screen; }
    public void setScreen(String screen) { this.screen = screen; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public Map<String, String> getFields() { return fields; }
    public void setFields(Map<String, String> fields) { this.fields = fields; }

    public Integer getTimeout() { return timeout; }
    public void setTimeout(Integer timeout) { this.timeout = timeout; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
