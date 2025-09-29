package org.openmanus.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Rolling memory buffer for conversation messages.
 */
public final class Memory {
    private final Deque<Message> messages = new ArrayDeque<>();
    private final int maxMessages;

    public Memory() {
        this(100);
    }

    public Memory(int maxMessages) {
        if (maxMessages <= 0) {
            throw new IllegalArgumentException("maxMessages must be positive");
        }
        this.maxMessages = maxMessages;
    }

    public void addMessage(Message message) {
        Objects.requireNonNull(message, "message");
        if (messages.size() == maxMessages) {
            messages.removeFirst();
        }
        messages.addLast(message);
    }

    public void addMessages(List<Message> newMessages) {
        Objects.requireNonNull(newMessages, "newMessages");
        for (Message message : newMessages) {
            addMessage(message);
        }
    }

    public void clear() {
        messages.clear();
    }

    public List<Message> asList() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    public Optional<Message> lastAssistantMessage() {
        return reverseStream()
                .filter(msg -> msg.getRole() == Role.ASSISTANT)
                .findFirst();
    }

    public Optional<Message> lastUserMessage() {
        return reverseStream()
                .filter(msg -> msg.getRole() == Role.USER)
                .findFirst();
    }

    public List<Message> recentMessages(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative");
        }
        return Collections.unmodifiableList(
                asList().subList(Math.max(0, messages.size() - count), messages.size()));
    }

    public long countAssistantMessagesWithContent(String content) {
        return messages.stream()
                .filter(msg -> msg.getRole() == Role.ASSISTANT)
                .filter(msg -> msg.getContent().map(content::equals).orElse(false))
                .count();
    }

    private java.util.stream.Stream<Message> reverseStream() {
        List<Message> copy = new ArrayList<>(messages);
        Collections.reverse(copy);
        return copy.stream();
    }
}
