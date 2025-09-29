package org.openmanus.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.openmanus.core.FunctionCall;
import org.openmanus.core.Message;
import org.openmanus.core.Role;
import org.openmanus.core.ToolCall;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * {@link LLM} implementation backed by Spring AI's {@link ChatClient} abstraction.
 *
 * <p>This adapter converts the OpenManus conversation model into the Spring AI
 * message types before delegating to the provided {@link ChatClient}. Callers can
 * supply an optional configurator to customize the request (for example, to apply
 * model-specific options or temperature values).</p>
 */
public final class SpringAiChatClientLLM implements LLM {
    private final ChatClient chatClient;
    private final BiConsumer<ChatClient.ChatClientRequestSpec, Optional<Double>> requestConfigurator;

    /**
     * Create an adapter with the provided {@link ChatClient} and no additional
     * request customization.
     *
     * @param chatClient the underlying Spring AI chat client
     */
    public SpringAiChatClientLLM(ChatClient chatClient) {
        this(chatClient, (spec, temperature) -> {});
    }

    /**
     * Create an adapter with a {@link ChatClient} and configurator invoked prior to
     * executing each request.
     *
     * @param chatClient the underlying Spring AI chat client
     * @param requestConfigurator hook for applying model specific options (for
     *     example, temperature) before the request is executed
     */
    public SpringAiChatClientLLM(ChatClient chatClient,
            BiConsumer<ChatClient.ChatClientRequestSpec, Optional<Double>> requestConfigurator) {
        this.chatClient = Objects.requireNonNull(chatClient, "chatClient");
        this.requestConfigurator = Objects.requireNonNull(requestConfigurator, "requestConfigurator");
    }

    @Override
    public String respond(
            List<Message> messages,
            Optional<List<Message>> systemMessages,
            Optional<Double> temperature) {
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt();

        List<org.springframework.ai.chat.messages.Message> payload = new ArrayList<>();
        systemMessages.ifPresent(list -> list.stream().map(this::toSpringMessage).forEach(payload::add));
        messages.stream().map(this::toSpringMessage).forEach(payload::add);

        if (!payload.isEmpty()) {
            spec.messages(payload);
        }

        requestConfigurator.accept(spec, temperature);

        String content = spec.call().content();
        return content != null ? content : "";
    }

    private org.springframework.ai.chat.messages.Message toSpringMessage(Message message) {
        return switch (message.getRole()) {
            case SYSTEM -> new SystemMessage(message.getContent().orElse(""));
            case USER -> new UserMessage(message.getContent().orElse(""));
            case ASSISTANT -> createAssistantMessage(message);
            case TOOL -> createToolResponseMessage(message);
        };
    }

    private AssistantMessage createAssistantMessage(Message message) {
        String content = message.getContent().orElse("");
        List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();
        for (ToolCall call : message.getToolCalls()) {
            FunctionCall function = call.getFunction();
            toolCalls.add(new AssistantMessage.ToolCall(
                    call.getId(),
                    call.getType(),
                    function.getName(),
                    function.getArguments()));
        }
        return toolCalls.isEmpty()
                ? new AssistantMessage(content)
                : new AssistantMessage(content, Map.of(), toolCalls);
    }

    private ToolResponseMessage createToolResponseMessage(Message message) {
        String toolCallId = message.getToolCallId().orElseThrow(
                () -> new IllegalArgumentException("Tool messages require a tool call id"));
        String name = message.getName().orElseThrow(
                () -> new IllegalArgumentException("Tool messages require a tool name"));
        String response = message.getContent().orElse("");

        List<ToolResponseMessage.ToolResponse> responses = List.of(
                new ToolResponseMessage.ToolResponse(toolCallId, name, response));
        return new ToolResponseMessage(responses);
    }
}

