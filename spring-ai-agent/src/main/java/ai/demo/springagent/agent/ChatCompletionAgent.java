package ai.demo.springagent.agent;

import ai.demo.agent.base.AgentConfiguration;
import ai.demo.springagent.base.SpringAiAgent;
import ai.demo.springagent.dto.ChatRequest;
import ai.demo.springagent.dto.ChatResponse;
import ai.demo.springagent.task.ChatTask;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.UUID;

/**
 * Concrete implementation of an AI agent for chat completions using the new agent abstraction.
 *
 * <p>This agent processes ChatTask instances and produces ChatResponse results
 * through the AI transformation pipeline: TASK → PROMPT → CHAT_RESPONSE → RESULT.
 */
public class ChatCompletionAgent extends SpringAiAgent<ChatTask, ChatResponse> {

    /**
     * Create a new ChatCompletionAgent with the specified configuration.
     *
     * @param chatClient the Spring AI ChatClient for LLM interactions
     */
    public ChatCompletionAgent(ChatClient chatClient) {
        super("ChatCompletionAgent", "1.0.0",
              AgentConfiguration.builder()
                  .instructions("You are a helpful AI assistant. Provide clear, accurate, and thoughtful responses to user queries.")
                  .build(),
              List.of("chat-completion", "conversation", "question-answering", "text-generation"),
              chatClient);
    }

    /**
     * Create a new ChatCompletionAgent with custom configuration.
     *
     * @param chatClient the Spring AI ChatClient for LLM interactions
     * @param configuration custom agent configuration
     */
    public ChatCompletionAgent(ChatClient chatClient, AgentConfiguration configuration) {
        super("ChatCompletionAgent", "1.0.0", configuration,
              List.of("chat-completion", "conversation", "question-answering", "text-generation"),
              chatClient);
    }

    @Override
    public Prompt transformToPrompt(ChatTask task) {
        ChatRequest chatRequest = task.getChatRequest();

        // Build conversation context from message history
        StringBuilder conversationBuilder = new StringBuilder();

        if (chatRequest.getMessages() != null && !chatRequest.getMessages().isEmpty()) {
            for (ChatRequest.Message message : chatRequest.getMessages()) {
                conversationBuilder.append(message.getRole().toUpperCase())
                                 .append(": ")
                                 .append(message.getContent())
                                 .append("\n");
            }
        } else {
            // Fallback for empty message list
            conversationBuilder.append("USER: Please help me with this request.\n");
        }

        // Create a simple prompt with the conversation context
        String userMessage = conversationBuilder.toString().trim();

        return createUserMessagePrompt(userMessage);
    }

    @Override
    public ChatResponse transformFromResponse(String response) {
        // Create a ChatResponse from the AI-generated text
        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setId("chatcmpl-" + UUID.randomUUID().toString().replace("-", ""));
        chatResponse.setObject("chat.completion");
        chatResponse.setCreated(System.currentTimeMillis() / 1000);
        chatResponse.setModel("gpt-5-nano"); // Could be made configurable

        ChatResponse.Choice choice = new ChatResponse.Choice();
        choice.setIndex(0);
        choice.setFinishReason("stop");

        ChatResponse.Message message = new ChatResponse.Message();
        message.setRole("assistant");
        message.setContent(response);
        choice.setMessage(message);

        chatResponse.setChoices(List.of(choice));

        // Add usage information (placeholder values)
        ChatResponse.Usage usage = new ChatResponse.Usage();
        // Estimate tokens (4 chars per token as rough estimate)
        int estimatedTokens = response.length() / 4;
        usage.setPromptTokens(estimatedTokens / 2); // Rough estimate
        usage.setCompletionTokens(estimatedTokens / 2);
        usage.setTotalTokens(estimatedTokens);
        chatResponse.setUsage(usage);

        return chatResponse;
    }

    @Override
    protected String buildCustomContext() {
        // Add context about the agent's capabilities and current state
        StringBuilder context = new StringBuilder();
        context.append("Agent Information:\n");
        context.append("- Name: ").append(getAgentName()).append("\n");
        context.append("- Version: ").append(getVersion()).append("\n");
        context.append("- State: ").append(getState()).append("\n");
        context.append("- Capabilities: ").append(String.join(", ", getCapabilities())).append("\n");

        // Add performance metrics if available
        var metrics = getMetrics();
        if (metrics.getTasksProcessed() > 0) {
            context.append("- Performance: ").append(metrics.getTasksProcessed()).append(" tasks processed, ");
            context.append(String.format("%.1f%% success rate\n", metrics.getSuccessRate() * 100));
        }

        return context.toString();
    }

    @Override
    public void onTaskStarted(ChatTask task) {
        super.onTaskStarted(task);
        // Log task start for monitoring
        System.out.println("[" + getAgentName() + "] Started processing chat task: " + task.getId());
    }

    @Override
    public void onTaskCompleted(ChatTask task, ChatResponse result) {
        super.onTaskCompleted(task, result);
        // Log successful completion
        System.out.println("[" + getAgentName() + "] Successfully completed chat task: " + task.getId());

        // Record learning about successful patterns
        String learning = "Successfully processed chat request with " +
                         task.getChatRequest().getMessages().size() + " messages";
        recordLearning(task, result, learning);
    }

    @Override
    public void onTaskFailed(ChatTask task, Throwable exception) {
        super.onTaskFailed(task, exception);
        // Log failure for debugging
        System.err.println("[" + getAgentName() + "] Failed to process chat task: " + task.getId());
        System.err.println("Error: " + exception.getMessage());

        // Record learning about failure patterns
        String learning = "Failed to process chat request due to: " + exception.getClass().getSimpleName();
        recordLearning(task, null, learning);
    }
}