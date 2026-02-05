package org.tn5250j.keyboard;

/**
 * Remapping action representing a target for a key remapping.
 */
public class RemappingAction {
    public static final int TYPE_5250_KEY = 1;
    public static final int TYPE_LOCAL_ACTION = 2;
    public static final int TYPE_MACRO = 3;
    public static final int TYPE_DISABLED = 4;

    private String targetName;
    private int type;

    public RemappingAction(String targetName, int type) {
        this.targetName = targetName;
        this.type = type;
    }

    public String getTargetName() {
        return targetName;
    }

    public int getType() {
        return type;
    }
}
