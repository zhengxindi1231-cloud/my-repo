package org.openmanus.core;

import java.util.Objects;

/**
 * Representation of a tool call attached to a message.
 */
public final class ToolCall {
    private final String id;
    private final String type;
    private final FunctionCall function;

    public ToolCall(String id, String type, FunctionCall function) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.function = Objects.requireNonNull(function, "function");
    }

    public static ToolCall functionCall(String id, FunctionCall function) {
        return new ToolCall(id, "function", function);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public FunctionCall getFunction() {
        return function;
    }
}
