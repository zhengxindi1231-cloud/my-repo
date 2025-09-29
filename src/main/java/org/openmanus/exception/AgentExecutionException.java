package org.openmanus.exception;

/**
 * Wraps unexpected errors that occur while running an agent step loop.
 */
public class AgentExecutionException extends RuntimeException {
    public AgentExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
