package org.openmanus.agent;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openmanus.core.AgentState;
import org.openmanus.core.Memory;
import org.openmanus.core.Role;
import org.openmanus.exception.AgentStateException;
import org.openmanus.llm.LLM;

class EchoAgentTest {
    @Test
    @DisplayName("EchoAgent copies the latest user message into the assistant response")
    void echoAgentMirrorsUserInput() {
        EchoAgent agent = new EchoAgent("echo");
        agent.setMaxSteps(3);

        List<String> steps = agent.run("Hello OpenManus");

        assertEquals(1, steps.size());
        assertTrue(steps.get(0).contains("Hello OpenManus"));
        assertEquals(AgentState.IDLE, agent.getState());

        var history = agent.getMemory().asList();
        assertEquals(2, history.size());
        assertEquals(Role.USER, history.get(0).getRole());
        assertEquals(Role.ASSISTANT, history.get(1).getRole());
        assertEquals("Hello OpenManus", history.get(1).getContent().orElseThrow());
    }

    @Test
    @DisplayName("Agents cannot be executed when not IDLE")
    void runningAgentWhileBusyThrows() {
        class BusyAgent extends BaseAgent {
            BusyAgent() {
                super("busy", null, null, new Memory());
            }

            void forceState(AgentState state) {
                setState(state);
            }

            @Override
            protected String step() {
                updateMemory(Role.ASSISTANT, "noop");
                finish();
                return "noop";
            }
        }

        BusyAgent agent = new BusyAgent();
        agent.forceState(AgentState.RUNNING);

        assertThrows(AgentStateException.class, () -> agent.run("test"));
    }

    @Test
    @DisplayName("Custom LLM implementations integrate with the agent loop")
    void customLlmIsInvoked() {
        class CountingLLM implements LLM {
            private int invocations = 0;

            @Override
            public String respond(java.util.List<org.openmanus.core.Message> messages,
                    java.util.Optional<java.util.List<org.openmanus.core.Message>> systemMessages,
                    java.util.Optional<Double> temperature) {
                invocations++;
                return "call-" + invocations;
            }
        }

        CountingLLM llm = new CountingLLM();
        EchoAgent agent = new EchoAgent("counter", null, llm, new Memory());
        agent.setMaxSteps(2);

        List<String> steps = agent.run("trigger");

        assertEquals(List.of("Step 1: call-1"), steps);
        assertEquals("call-1",
                agent.getMemory().asList().get(1).getContent().orElseThrow());
    }
}
