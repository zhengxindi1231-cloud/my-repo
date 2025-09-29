package org.openmanus.core;

/**
 * Roles supported by OpenManus messages.
 */
public enum Role {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL;

    /**
     * Convert the enum to the lowercase representation used by the Python code.
     */
    public String toWireValue() {
        return name().toLowerCase();
    }
}
