package org.openmanus.core;

/**
 * Execution lifecycle for an agent. Mirrors the Python {@code AgentState}
 * enumeration from the OpenManus project.
 */
public enum AgentState {
    IDLE,
    RUNNING,
    FINISHED,
    ERROR
}
