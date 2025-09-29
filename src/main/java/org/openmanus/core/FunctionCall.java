package org.openmanus.core;

import java.util.Objects;

/**
 * Representation of a tool function call payload.
 */
public final class FunctionCall {
    private final String name;
    private final String arguments;

    public FunctionCall(String name, String arguments) {
        this.name = Objects.requireNonNull(name, "name");
        this.arguments = Objects.requireNonNull(arguments, "arguments");
    }

    public String getName() {
        return name;
    }

    public String getArguments() {
        return arguments;
    }
}
