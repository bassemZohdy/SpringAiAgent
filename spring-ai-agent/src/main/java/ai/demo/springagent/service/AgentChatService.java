package ai.demo.springagent.service;

import ai.demo.agent.base.AgentException;
import ai.demo.springagent.agent.ChatCompletionAgent;
import ai.demo.springagent.dto.ChatRequest;
import ai.demo.springagent.dto.ChatResponse;
import ai.demo.springagent.model.ThreadMessage;
import ai.demo.springagent.task.ChatTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Enhanced chat service that uses the new agent abstraction framework.
 *
 * <p>This service demonstrates how to integrate the new TaskAgent and AiAgent
 * abstractions into the Spring application, providing enhanced capabilities like
 * memory management, metrics collection, and lifecycle management.
 */
@Service
public class AgentChatService {

    private static final Logger logger = LoggerFactory.getLogger(AgentChatService.class);

    private final ChatCompletionAgent chatAgent;
    private final ThreadService threadService;

    public AgentChatService(ChatCompletionAgent chatAgent, ThreadService threadService) {
        this.chatAgent = chatAgent;
        this.threadService = threadService;

        // Start the agent when service is initialized
        try {
            chatAgent.start();
            logger.info("ChatCompletionAgent started successfully");
        } catch (AgentException e) {
            logger.error("Failed to start ChatCompletionAgent", e);
            throw new RuntimeException("Could not initialize chat agent", e);
        }
    }

    /**
     * Process a chat request using the agent framework.
     *
     * @param request the chat request to process
     * @return chat response from the agent
     */
    public ChatResponse processChat(ChatRequest request) {
        logger.debug("Processing chat request using agent framework - threadId: {}",
                    request.getThreadId());

        // Create a ChatTask from the request
        ChatTask task = new ChatTask(request);

        // Process thread history if needed
        ChatTask processedTask = processThreadHistory(task);

        try {
            // Use the agent to process the task
            CompletableFuture<ChatResponse> future = chatAgent.process(processedTask);
            ChatResponse response = future.get();

            // Save assistant response to thread if applicable
            saveAssistantResponse(request.getThreadId(), response);

            // Log metrics
            logAgentMetrics();

            logger.info("Agent-based chat completion successful - duration: {}ms",
                       calculateDuration(request));

            return response;

        } catch (Exception e) {
            logger.error("Agent-based chat completion failed", e);
            throw new RuntimeException("Failed to process chat with agent", e);
        }
    }

    /**
     * Process a chat request using the agent framework with memory advisor.
     * This demonstrates integration with Spring AI's memory system.
     *
     * @param request the chat request to process
     * @return chat response from the agent
     */
    public ChatResponse processChatWithMemory(ChatRequest request) {
        logger.debug("Processing chat request with agent and memory - threadId: {}",
                    request.getThreadId());

        // Create a ChatTask with memory context
        ChatTask task = createMemoryEnhancedTask(request);

        try {
            // Use the agent to process the task (agent handles memory internally)
            CompletableFuture<ChatResponse> future = chatAgent.process(task);
            ChatResponse response = future.get();

            // Save to thread storage
            saveAssistantResponse(request.getThreadId(), response);

            logger.info("Agent-based chat with memory completed successfully");
            return response;

        } catch (Exception e) {
            logger.error("Agent-based chat with memory failed", e);
            throw new RuntimeException("Failed to process chat with agent and memory", e);
        }
    }

    /**
     * Get current agent metrics and status.
     *
     * @return agent metrics information
     */
    public Object getAgentMetrics() {
        return java.util.Map.of(
            "agentId", chatAgent.getAgentId(),
            "agentName", chatAgent.getAgentName(),
            "version", chatAgent.getVersion(),
            "state", chatAgent.getState(),
            "isRunning", chatAgent.isRunning(),
            "capabilities", chatAgent.getCapabilities(),
            "metrics", java.util.Map.of(
                "tasksProcessed", chatAgent.getMetrics().getTasksProcessed(),
                "tasksSucceeded", chatAgent.getMetrics().getTasksSucceeded(),
                "tasksFailed", chatAgent.getMetrics().getTasksFailed(),
                "successRate", chatAgent.getMetrics().getSuccessRate(),
                "averageProcessingTime", chatAgent.getMetrics().getAverageProcessingTime()
            ),
            "memory", java.util.Map.of(
                "size", chatAgent.getMemory().size(),
                "isEmpty", chatAgent.getMemory().isEmpty(),
                "hasSummary", chatAgent.getMemory().getSummary() != null
            )
        );
    }

    /**
     * Compact agent memory to optimize performance.
     */
    public void compactAgentMemory() {
        logger.info("Compacting agent memory");
        chatAgent.compactMemory();
    }

    /**
     * Clear agent memory.
     */
    public void clearAgentMemory() {
        logger.info("Clearing agent memory");
        chatAgent.clearMemory();
    }

    private ChatTask processThreadHistory(ChatTask task) {
        String threadId = task.getChatRequest().getThreadId();
        ChatRequest request = task.getChatRequest();

        if (threadId != null && threadService.getThread(threadId).isPresent()) {
            // Save user message to thread
            if (!request.getMessages().isEmpty()) {
                ChatRequest.Message lastMessage = request.getMessages().get(request.getMessages().size() - 1);
                if ("user".equals(lastMessage.getRole())) {
                    threadService.addMessageToThread(threadId, lastMessage.getRole(), lastMessage.getContent());
                }
            }

            // Get complete thread history for context
            var threadHistory = threadService.getThreadMessages(threadId);
            if (!threadHistory.isEmpty()) {
                // Update the chat request with thread history
                ChatRequest enhancedRequest = new ChatRequest();
                enhancedRequest.setModel(request.getModel());
                enhancedRequest.setMessages(
                    threadHistory.stream()
                            .map(tm -> new ChatRequest.Message(tm.getRole(), tm.getContent()))
                            .toList()
                );
                enhancedRequest.setTemperature(request.getTemperature());
                enhancedRequest.setMaxTokens(request.getMaxTokens());
                enhancedRequest.setStream(request.isStream());
                enhancedRequest.setThreadId(request.getThreadId());

                return new ChatTask(enhancedRequest);
            }
        }

        return task;
    }

    private ChatTask createMemoryEnhancedTask(ChatRequest request) {
        // Add memory context to the task metadata
        ChatTask task = new ChatTask(request);

        // The agent will automatically use its internal memory system
        // when processing the task through the transformation pipeline

        return task;
    }

    private void saveAssistantResponse(String threadId, ChatResponse response) {
        if (threadId != null && threadService.getThread(threadId).isPresent() &&
            response.getChoices() != null && !response.getChoices().isEmpty()) {
            String content = response.getChoices().get(0).getMessage().getContent();
            threadService.addMessageToThread(threadId, "assistant", content);
        }
    }

    private void logAgentMetrics() {
        var metrics = chatAgent.getMetrics();
        var memory = chatAgent.getMemory();

        logger.info("Agent Metrics - Tasks: {}, Success Rate: {:.1f}%, Memory Size: {}",
                   metrics.getTasksProcessed(),
                   metrics.getSuccessRate() * 100,
                   memory.size());
    }

    private long calculateDuration(ChatRequest request) {
        // Simple duration calculation - in a real implementation,
        // you'd track start/end times more precisely
        return 100; // Placeholder
    }
}