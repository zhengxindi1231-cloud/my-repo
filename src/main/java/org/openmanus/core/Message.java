package org.openmanus.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Conversation message data structure.
 */
public final class Message {
    private final Role role;
    private final String content;
    private final List<ToolCall> toolCalls;
    private final String name;
    private final String toolCallId;

    private Message(Builder builder) {
        this.role = Objects.requireNonNull(builder.role, "role");
        this.content = builder.content;
        this.toolCalls = builder.toolCalls == null
                ? List.of()
                : List.copyOf(builder.toolCalls);
        this.name = builder.name;
        this.toolCallId = builder.toolCallId;
    }

    public Role getRole() {
        return role;
    }

    public Optional<String> getContent() {
        return Optional.ofNullable(content);
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<String> getToolCallId() {
        return Optional.ofNullable(toolCallId);
    }

    public Map<String, Object> toMap() {
        var map = new java.util.LinkedHashMap<String, Object>();
        map.put("role", role.toWireValue());
        if (content != null) {
            map.put("content", content);
        }
        if (!toolCalls.isEmpty()) {
            List<Map<String, Object>> calls = new ArrayList<>();
            for (ToolCall call : toolCalls) {
                Map<String, Object> callMap = new java.util.LinkedHashMap<>();
                callMap.put("id", call.getId());
                callMap.put("type", call.getType());
                Map<String, Object> fn = new java.util.LinkedHashMap<>();
                fn.put("name", call.getFunction().getName());
                fn.put("arguments", call.getFunction().getArguments());
                callMap.put("function", fn);
                calls.add(callMap);
            }
            map.put("tool_calls", calls);
        }
        if (name != null) {
            map.put("name", name);
        }
        if (toolCallId != null) {
            map.put("tool_call_id", toolCallId);
        }
        return Collections.unmodifiableMap(map);
    }

    public static Message user(String content) {
        return builder().role(Role.USER).content(content).build();
    }

    public static Message system(String content) {
        return builder().role(Role.SYSTEM).content(content).build();
    }

    public static Message assistant(String content) {
        return builder().role(Role.ASSISTANT).content(content).build();
    }

    public static Message assistantWithTools(String content, List<ToolCall> toolCalls) {
        return builder().role(Role.ASSISTANT).content(content).toolCalls(toolCalls).build();
    }

    public static Message tool(String content, String name, String toolCallId) {
        return builder()
                .role(Role.TOOL)
                .content(content)
                .name(name)
                .toolCallId(toolCallId)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Role role;
        private String content;
        private List<ToolCall> toolCalls;
        private String name;
        private String toolCallId;

        private Builder() {}

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder toolCalls(List<ToolCall> toolCalls) {
            this.toolCalls = new ArrayList<>(toolCalls);
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder toolCallId(String toolCallId) {
            this.toolCallId = toolCallId;
            return this;
        }

        public Message build() {
            return new Message(this);
        }
    }
}
