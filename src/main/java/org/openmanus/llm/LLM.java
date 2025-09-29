package org.openmanus.llm;

import java.util.List;
import java.util.Optional;
import org.openmanus.core.Message;

/**
 * Minimal interface representing the language model abstraction used by OpenManus.
 */
public interface LLM {
    /**
     * Generate a response for the provided conversation history.
     *
     * @param messages conversation messages in chronological order
     * @param systemMessages optional system prompt messages prepended to the request
     * @param temperature optional temperature override
     * @return generated response text
     */
    String respond(List<Message> messages, Optional<List<Message>> systemMessages, Optional<Double> temperature);
}
