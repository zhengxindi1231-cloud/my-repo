package org.openmanus.agent;

import java.util.List;
import java.util.Optional;
import org.openmanus.core.Memory;
import org.openmanus.core.Message;
import org.openmanus.core.Role;
import org.openmanus.llm.LLM;

/**
 * Demonstration agent that mirrors a small portion of the behaviour of the
 * Python {@code ManusAgent}. It asks the backing {@link LLM} for a response to
 * the full conversation and stores the reply as an assistant message.
 */
public final class EchoAgent extends BaseAgent {
    public EchoAgent(String name, String description, LLM llm, Memory memory) {
        super(name, description, llm, memory);
    }

    public EchoAgent(String name) {
        this(name, null, null, null);
    }

    @Override
    protected String step() {
        Optional<List<Message>> systemMessages =
                getSystemPrompt().map(prompt -> List.of(Message.system(prompt)));
        Optional<Double> temperature =
                getNextStepPrompt().filter(prompt -> !prompt.isBlank()).map(prompt -> 0.7d);

        String reply = getLlm().respond(getMemory().asList(), systemMessages, temperature);

        updateMemory(Role.ASSISTANT, reply);
        finish();
        return reply;
    }
}
