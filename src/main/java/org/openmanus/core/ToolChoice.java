package org.openmanus.core;

/**
 * Tool selection strategy, equivalent to the Python {@code ToolChoice} enum.
 */
public enum ToolChoice {
    NONE,
    AUTO,
    REQUIRED;

    public String toWireValue() {
        return name().toLowerCase();
    }
}
