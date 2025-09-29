package org.openmanus.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openmanus.core.AgentState;
import org.openmanus.core.Memory;
import org.openmanus.core.Message;
import org.openmanus.core.Role;
import org.openmanus.exception.AgentExecutionException;
import org.openmanus.exception.AgentStateException;
import org.openmanus.llm.LLM;
import org.openmanus.llm.SimpleLLM;

/**
 * Abstract base class that mirrors the functionality of the Python
 * {@code BaseAgent}. It manages the execution loop, state transitions and
 * message bookkeeping.
 */
public abstract class BaseAgent {
    private static final Logger LOGGER = Logger.getLogger(BaseAgent.class.getName());

    private final String name;
    private final String description;
    private String systemPrompt;
    private String nextStepPrompt;
    private final LLM llm;
    private final Memory memory;

    private AgentState state = AgentState.IDLE;
    private int maxSteps = 10;
    private int currentStep = 0;
    private int duplicateThreshold = 2;

    protected BaseAgent(String name, String description, LLM llm, Memory memory) {
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
        this.llm = llm != null ? llm : new SimpleLLM();
        this.memory = memory != null ? memory : new Memory();
    }

    public String getName() {
        return name;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Optional<String> getSystemPrompt() {
        return Optional.ofNullable(systemPrompt);
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public Optional<String> getNextStepPrompt() {
        return Optional.ofNullable(nextStepPrompt);
    }

    public void setNextStepPrompt(String nextStepPrompt) {
        this.nextStepPrompt = nextStepPrompt;
    }

    public LLM getLlm() {
        return llm;
    }

    public Memory getMemory() {
        return memory;
    }

    public AgentState getState() {
        return state;
    }

    protected void setState(AgentState state) {
        this.state = state;
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public void setMaxSteps(int maxSteps) {
        if (maxSteps <= 0) {
            throw new IllegalArgumentException("maxSteps must be positive");
        }
        this.maxSteps = maxSteps;
    }

    public void setDuplicateThreshold(int duplicateThreshold) {
        if (duplicateThreshold <= 0) {
            throw new IllegalArgumentException("duplicateThreshold must be positive");
        }
        this.duplicateThreshold = duplicateThreshold;
    }

    /**
     * Runs the agent's main loop.
     *
     * @param request optional user request to seed the conversation
     * @return immutable list of step summaries
     */
    public synchronized List<String> run(String request) {
        if (state != AgentState.IDLE) {
            throw new AgentStateException("Cannot run agent from state: " + state);
        }

        if (request != null && !request.isBlank()) {
            updateMemory(Role.USER, request);
        }

        List<String> results = new ArrayList<>();
        state = AgentState.RUNNING;
        try {
            while (currentStep < maxSteps && state != AgentState.FINISHED) {
                currentStep++;
                LOGGER.log(Level.INFO, "Executing step {0}/{1}", new Object[] {currentStep, maxSteps});
                String stepResult = step();
                if (isStuck()) {
                    handleStuckState();
                }
                results.add(String.format("Step %d: %s", currentStep, stepResult));
            }

            if (currentStep >= maxSteps && state != AgentState.FINISHED) {
                results.add(String.format("Terminated: Reached max steps (%d)", maxSteps));
            }
            return List.copyOf(results);
        } catch (AgentExecutionException e) {
            state = AgentState.ERROR;
            throw e;
        } catch (Exception e) {
            state = AgentState.ERROR;
            throw new AgentExecutionException("Agent execution failed", e);
        } finally {
            currentStep = 0;
            if (state != AgentState.ERROR) {
                state = AgentState.IDLE;
            }
        }
    }

    protected void finish() {
        state = AgentState.FINISHED;
    }

    protected void updateMemory(Role role, String content) {
        updateMemory(role, content, null, null);
    }

    protected void updateMemory(Role role, String content, String name, String toolCallId) {
        Objects.requireNonNull(role, "role");
        Message message;
        switch (role) {
            case USER -> message = Message.user(content);
            case SYSTEM -> message = Message.system(content);
            case ASSISTANT -> message = Message.assistant(content);
            case TOOL -> message = Message.tool(content, name, toolCallId);
            default -> throw new IllegalStateException("Unexpected role: " + role);
        }
        memory.addMessage(message);
    }

    protected boolean isStuck() {
        List<Message> messages = memory.asList();
        if (messages.size() < 2) {
            return false;
        }
        Message lastMessage = messages.get(messages.size() - 1);
        if (lastMessage.getRole() != Role.ASSISTANT) {
            return false;
        }
        Optional<String> content = lastMessage.getContent();
        if (content.isEmpty() || content.get().isBlank()) {
            return false;
        }
        long duplicates = memory.countAssistantMessagesWithContent(content.get());
        return duplicates >= duplicateThreshold;
    }

    protected void handleStuckState() {
        String stuckPrompt =
                "Observed duplicate responses. Consider new strategies and avoid repeating"
                        + " ineffective paths already attempted.";
        if (nextStepPrompt == null || nextStepPrompt.isBlank()) {
            nextStepPrompt = stuckPrompt;
        } else {
            nextStepPrompt = stuckPrompt + System.lineSeparator() + nextStepPrompt;
        }
        LOGGER.log(Level.WARNING, "Agent detected stuck state. Added prompt: {0}", stuckPrompt);
    }

    protected abstract String step() throws Exception;
}
