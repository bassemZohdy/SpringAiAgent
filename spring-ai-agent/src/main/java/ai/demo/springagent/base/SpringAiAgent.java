package ai.demo.springagent.base;

import ai.demo.agent.base.*;
import ai.demo.agent.base.task.Task;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Spring AI implementation of the AiAgent interface for the base agent framework.
 * Provides automatic context enhancement including instructions, memory learnings,
 * and configuration preferences.
 *
 * @param <TASK> The type of task/input the agent processes (must extend Task)
 * @param <RESULT> The type of result/output the agent produces
 */
public abstract class SpringAiAgent<TASK extends Task, RESULT> extends BaseAgent<TASK, RESULT> implements AiAgent<TASK, Prompt, ChatResponse, RESULT> {

    private final ChatClient chatClient;

    /**
     * Construct a SpringAiAgent with specified configuration.
     *
     * @param agentName     the human-readable name for this agent
     * @param version       the version of this agent
     * @param configuration the agent configuration
     * @param capabilities  the list of capabilities this agent supports
     * @param chatClient    the Spring AI ChatClient for LLM interactions
     */
    protected SpringAiAgent(String agentName, String version, AgentConfiguration configuration,
                            List<String> capabilities, ChatClient chatClient) {
        super(agentName, version, configuration, capabilities);
        this.chatClient = Objects.requireNonNull(chatClient, "ChatClient cannot be null");
    }

    /**
     * Construct a SpringAiAgent with default configuration.
     *
     * @param agentName    the human-readable name for this agent
     * @param version      the version of this agent
     * @param capabilities the list of capabilities this agent supports
     * @param chatClient   the Spring AI ChatClient for LLM interactions
     */
    protected SpringAiAgent(String agentName, String version, List<String> capabilities, ChatClient chatClient) {
        super(agentName, version, capabilities);
        this.chatClient = Objects.requireNonNull(chatClient, "ChatClient cannot be null");
    }

    @Override
    protected final RESULT doProcess(TASK task) throws Exception {
        // Follow the transformation pipeline: TASK → PROMPT → CHAT_RESPONSE → RESULT
        Prompt basePrompt = transformToPrompt(task);
        ChatResponse response = call(basePrompt);
        return transformFromResponse(response);
    }

    @Override
    public final ChatResponse call(Prompt basePrompt) {
        // Enhance the prompt with context from configuration and memory
        Prompt enhancedPrompt = enhancePromptWithContext(basePrompt);
        return chatClient.call(enhancedPrompt);
    }

    /**
     * Enhance the base prompt with context from agent configuration and memory.
     * This automatically adds:
     * - System instructions from configuration
     * - Relevant learnings from agent memory
     * - Any additional context from subclass implementations
     *
     * @param basePrompt the base prompt to enhance
     * @return the enhanced prompt with full context
     */
    protected Prompt enhancePromptWithContext(Prompt basePrompt) {
        List<Message> messages = new ArrayList<>();

        // Add system instructions from configuration
        String instructions = getInstructions();
        if (instructions != null && !instructions.trim().isEmpty()) {
            messages.add(new SystemMessage(instructions));
        }

        // Add relevant learnings from memory
        String memoryContext = buildMemoryContext();
        if (memoryContext != null && !memoryContext.trim().isEmpty()) {
            messages.add(new SystemMessage("Previous experiences and learnings:\\n" + memoryContext));
        }

        // Add custom context from subclass
        String customContext = buildCustomContext();
        if (customContext != null && !customContext.trim().isEmpty()) {
            messages.add(new SystemMessage("Additional context:\\n" + customContext));
        }

        // Add the original prompt messages
        messages.addAll(basePrompt.getInstructions());

        return new Prompt(messages);
    }

    /**
     * Build memory context from recent agent experiences and learnings.
     * This creates a summary of relevant past experiences to inform current decisions.
     *
     * @return memory context string, or null if no relevant context
     */
    protected String buildMemoryContext() {
        AgentMemory memory = getMemory();
        if (memory.isEmpty()) {
            return null;
        }

        // Get memory summary if available
        String summary = memory.getSummary();
        if (summary != null) {
            return summary;
        }

        // Otherwise, build context from recent entries
        List<AgentMemory.MemoryEntry> recentEntries = memory.getRecentEntries(3);
        if (recentEntries.isEmpty()) {
            return null;
        }

        StringBuilder context = new StringBuilder();
        for (AgentMemory.MemoryEntry entry : recentEntries) {
            if (entry.getLearnings() != null && !entry.getLearnings().trim().isEmpty()) {
                context.append("- ").append(entry.getLearnings()).append("\\n");
            }
        }

        return context.length() > 0 ? context.toString() : null;
    }

    /**
     * Build custom context specific to the agent implementation.
     * Subclasses can override this to add domain-specific context.
     *
     * @return custom context string, or null if no custom context
     */
    protected String buildCustomContext() {
        return null; // Default implementation provides no custom context
    }

    /**
     * Get the underlying Spring AI ChatClient.
     * This allows subclasses to access advanced Spring AI features directly if needed.
     *
     * @return the ChatClient instance
     */
    protected final ChatClient getChatClient() {
        return chatClient;
    }

    /**
     * Helper method to create a simple user message prompt.
     * This is useful for agents that need to send basic user messages.
     *
     * @param message the message content
     * @return a Prompt containing the user message
     */
    protected final Prompt createUserMessagePrompt(String message) {
        return new Prompt(new UserMessage(message));
    }

    /**
     * Helper method to create a prompt with both system and user messages.
     *
     * @param systemMessage the system message content
     * @param userMessage   the user message content
     * @return a Prompt containing both messages
     */
    protected final Prompt createSystemUserPrompt(String systemMessage, String userMessage) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemMessage));
        messages.add(new UserMessage(userMessage));
        return new Prompt(messages);
    }
}