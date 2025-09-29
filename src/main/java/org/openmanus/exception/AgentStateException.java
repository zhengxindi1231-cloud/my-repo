package org.openmanus.exception;

/**
 * Signals that an agent was invoked while in an invalid state.
 */
public class AgentStateException extends RuntimeException {
    public AgentStateException(String message) {
        super(message);
    }
}
