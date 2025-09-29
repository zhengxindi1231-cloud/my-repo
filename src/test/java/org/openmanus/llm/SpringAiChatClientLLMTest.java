package org.openmanus.llm;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openmanus.core.FunctionCall;
import org.openmanus.core.Message;
import org.openmanus.core.ToolCall;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.AdvisorSpec;
import org.springframework.ai.chat.client.ChatClient.Builder;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.ChatClient.PromptSystemSpec;
import org.springframework.ai.chat.client.ChatClient.PromptUserSpec;
import org.springframework.ai.chat.client.ChatClient.StreamResponseSpec;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;

class SpringAiChatClientLLMTest {
    @Test
    @DisplayName("Messages are converted to Spring AI types and sent through ChatClient")
    void convertsMessagesBeforeDelegation() {
        StubChatClient stub = new StubChatClient("ok");
        SpringAiChatClientLLM llm = new SpringAiChatClientLLM(stub);

        List<Message> history = List.of(
                Message.user("What's the weather?"),
                Message.assistantWithTools(
                        "Let me check",
                        List.of(ToolCall.functionCall("tool-1", new FunctionCall("lookup", "{\"city\":\"Paris\"}")))),
                Message.tool("{\"forecast\":\"sunny\"}", "lookup", "tool-1"));

        Optional<List<Message>> system = Optional.of(List.of(Message.system("You are a helpful assistant.")));

        String response = llm.respond(history, system, Optional.empty());

        assertEquals("ok", response);
        List<org.springframework.ai.chat.messages.Message> captured = stub.getCapturedMessages();
        assertEquals(4, captured.size());
        assertInstanceOf(SystemMessage.class, captured.get(0));
        assertInstanceOf(UserMessage.class, captured.get(1));
        assertInstanceOf(AssistantMessage.class, captured.get(2));
        assertInstanceOf(ToolResponseMessage.class, captured.get(3));

        AssistantMessage assistantMessage = (AssistantMessage) captured.get(2);
        assertTrue(assistantMessage.hasToolCalls());
        assertEquals("lookup", assistantMessage.getToolCalls().get(0).name());

        ToolResponseMessage toolMessage = (ToolResponseMessage) captured.get(3);
        assertEquals("tool-1", toolMessage.getResponses().get(0).id());
    }

    @Test
    @DisplayName("Configurator receives temperature hint when provided")
    void configuratorReceivesTemperature() {
        StubChatClient stub = new StubChatClient("ack");
        AtomicReference<Optional<Double>> observed = new AtomicReference<>();
        SpringAiChatClientLLM llm = new SpringAiChatClientLLM(stub, (spec, temperature) -> observed.set(temperature));

        llm.respond(List.of(Message.user("ping")), Optional.empty(), Optional.of(0.5));

        assertEquals(Optional.of(0.5), observed.get());
    }

    private static final class StubChatClient implements ChatClient {
        private final String response;
        private List<org.springframework.ai.chat.messages.Message> captured = List.of();

        StubChatClient(String response) {
            this.response = response;
        }

        List<org.springframework.ai.chat.messages.Message> getCapturedMessages() {
            return captured;
        }

        @Override
        public ChatClientRequestSpec prompt() {
            return new StubRequestSpec();
        }

        @Override
        public ChatClientRequestSpec prompt(String prompt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ChatClientRequestSpec prompt(Prompt prompt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Builder mutate() {
            throw new UnsupportedOperationException();
        }

        private final class StubRequestSpec implements ChatClientRequestSpec {
            private List<org.springframework.ai.chat.messages.Message> messages = new ArrayList<>();

            @Override
            public Builder mutate() {
                throw new UnsupportedOperationException();
            }

            @Override
            public ChatClientRequestSpec advisors(Consumer<AdvisorSpec> advisors) {
                return this;
            }

            @Override
            public ChatClientRequestSpec advisors(org.springframework.ai.chat.client.advisor.api.Advisor... advisors) {
                return this;
            }

            @Override
            public ChatClientRequestSpec advisors(List<org.springframework.ai.chat.client.advisor.api.Advisor> advisors) {
                return this;
            }

            @Override
            public ChatClientRequestSpec messages(org.springframework.ai.chat.messages.Message... messages) {
                return messages(List.of(messages));
            }

            @Override
            public ChatClientRequestSpec messages(List<org.springframework.ai.chat.messages.Message> messages) {
                this.messages = new ArrayList<>(messages);
                return this;
            }

            @Override
            public <T extends ChatOptions> ChatClientRequestSpec options(T options) {
                return this;
            }

            @Override
            public ChatClientRequestSpec tools(String... tools) {
                return this;
            }

            @Override
            public ChatClientRequestSpec tools(FunctionCallback... toolCallbacks) {
                return this;
            }

            @Override
            public ChatClientRequestSpec tools(List<ToolCallback> toolCallbacks) {
                return this;
            }

            @Override
            public ChatClientRequestSpec tools(Object... tools) {
                return this;
            }

            @Override
            public ChatClientRequestSpec tools(ToolCallbackProvider... providers) {
                return this;
            }

            @Override
            public <I, O> ChatClientRequestSpec functions(FunctionCallback... functionCallbacks) {
                return this;
            }

            @Override
            public ChatClientRequestSpec functions(String... functions) {
                return this;
            }

            @Override
            public ChatClientRequestSpec toolContext(Map<String, Object> context) {
                return this;
            }

            @Override
            public ChatClientRequestSpec system(String systemText) {
                return this;
            }

            @Override
            public ChatClientRequestSpec system(Resource resource, Charset charset) {
                return this;
            }

            @Override
            public ChatClientRequestSpec system(Resource resource) {
                return this;
            }

            @Override
            public ChatClientRequestSpec system(Consumer<PromptSystemSpec> system) {
                return this;
            }

            @Override
            public ChatClientRequestSpec user(String userText) {
                return this;
            }

            @Override
            public ChatClientRequestSpec user(Resource resource, Charset charset) {
                return this;
            }

            @Override
            public ChatClientRequestSpec user(Resource resource) {
                return this;
            }

            @Override
            public ChatClientRequestSpec user(Consumer<PromptUserSpec> user) {
                return this;
            }

            @Override
            public CallResponseSpec call() {
                captured = List.copyOf(messages);
                return new StubCallResponseSpec();
            }

            @Override
            public StreamResponseSpec stream() {
                throw new UnsupportedOperationException();
            }

            private final class StubCallResponseSpec implements CallResponseSpec {
                @Override
                public <T> T entity(ParameterizedTypeReference<T> responseType) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public <T> T entity(StructuredOutputConverter<T> outputConverter) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public <T> T entity(Class<T> responseType) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public org.springframework.ai.chat.model.ChatResponse chatResponse() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public String content() {
                    return response;
                }

                @Override
                public <T> org.springframework.ai.chat.client.ResponseEntity<
                        org.springframework.ai.chat.model.ChatResponse, T> responseEntity(Class<T> responseType) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public <T> org.springframework.ai.chat.client.ResponseEntity<
                        org.springframework.ai.chat.model.ChatResponse, T> responseEntity(
                                ParameterizedTypeReference<T> responseType) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public <T> org.springframework.ai.chat.client.ResponseEntity<
                        org.springframework.ai.chat.model.ChatResponse, T> responseEntity(
                                StructuredOutputConverter<T> outputConverter) {
                    throw new UnsupportedOperationException();
                }
            }
        }
    }
}

