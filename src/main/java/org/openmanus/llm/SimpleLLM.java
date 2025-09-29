package org.openmanus.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import org.openmanus.core.Message;
import org.openmanus.core.Role;

/**
 * Lightweight LLM stub that echoes the most recent user message. This allows the
 * Java port to be exercised without invoking remote APIs.
 */
public final class SimpleLLM implements LLM {
    @Override
    public String respond(
            List<Message> messages,
            Optional<List<Message>> systemMessages,
            Optional<Double> temperature) {
        List<Message> consolidated = new ArrayList<>();
        systemMessages.ifPresent(consolidated::addAll);
        consolidated.addAll(messages);

        for (int i = consolidated.size() - 1; i >= 0; i--) {
            Message message = consolidated.get(i);
            if (message.getRole() == Role.USER) {
                return message.getContent().orElse("(no content)");
            }
        }

        // Fallback: concatenate assistant messages so the agent has deterministic output.
        StringJoiner joiner = new StringJoiner(" ");
        consolidated.stream()
                .filter(msg -> msg.getRole() == Role.ASSISTANT)
                .map(msg -> msg.getContent().orElse(""))
                .filter(content -> !content.isBlank())
                .forEach(joiner::add);
        String fallback = joiner.toString();
        return fallback.isBlank() ? "(no message history)" : fallback;
    }
}
