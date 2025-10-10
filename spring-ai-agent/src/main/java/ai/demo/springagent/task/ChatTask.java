package ai.demo.springagent.task;

import ai.demo.agent.base.task.Task;
import ai.demo.agent.base.task.TaskPriority;
import ai.demo.agent.base.task.TaskSize;
import ai.demo.agent.base.task.TaskStatus;
import ai.demo.springagent.dto.ChatRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Task representation for chat completions using the new agent abstraction.
 *
 * <p>This task wraps a ChatRequest and provides the necessary metadata
 * for the agent framework to process chat interactions as discrete tasks.
 */
public class ChatTask implements Task {

    private final String id;
    private final Instant createdAt;
    private final String description;
    private final ChatRequest chatRequest;
    private final TaskPriority priority;
    private final TaskSize size;
    private final String completionCriteria;
    private final Map<String, Object> metadata;

    public ChatTask(ChatRequest chatRequest) {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.chatRequest = chatRequest;

        // Build description from chat request
        StringBuilder descBuilder = new StringBuilder("Chat completion request");
        if (chatRequest.getModel() != null) {
            descBuilder.append(" using model: ").append(chatRequest.getModel());
        }
        if (chatRequest.getThreadId() != null) {
            descBuilder.append(" for thread: ").append(chatRequest.getThreadId());
        }
        this.description = descBuilder.toString();

        // Determine task characteristics
        this.priority = determinePriority(chatRequest);
        this.size = determineSize(chatRequest);
        this.completionCriteria = "Generate appropriate response based on chat context";
        this.metadata = Map.of(
            "model", chatRequest.getModel() != null ? chatRequest.getModel() : "unknown",
            "threadId", chatRequest.getThreadId() != null ? chatRequest.getThreadId() : "none",
            "stream", chatRequest.isStream(),
            "temperature", chatRequest.getTemperature(),
            "maxTokens", chatRequest.getMaxTokens()
        );
    }

    private TaskPriority determinePriority(ChatRequest request) {
        // Higher priority for shorter messages and direct conversations
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            return TaskPriority.NORMAL;
        }

        int totalChars = request.getMessages().stream()
                .mapToInt(msg -> msg.getContent() != null ? msg.getContent().length() : 0)
                .sum();

        if (totalChars < 100) return TaskPriority.HIGH;
        if (totalChars < 500) return TaskPriority.NORMAL;
        return TaskPriority.LOW;
    }

    private TaskSize determineSize(ChatRequest request) {
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            return TaskSize.SMALL;
        }

        int totalChars = request.getMessages().stream()
                .mapToInt(msg -> msg.getContent() != null ? msg.getContent().length() : 0)
                .sum();

        if (totalChars < 200) return TaskSize.SMALL;
        if (totalChars < 1000) return TaskSize.MEDIUM;
        return TaskSize.LARGE;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public TaskPriority getPriority() {
        return priority;
    }

    @Override
    public TaskStatus getStatus() {
        return TaskStatus.CREATED;
    }

    @Override
    public TaskSize getSize() {
        return size;
    }

    @Override
    public String getCompletionCriteria() {
        return completionCriteria;
    }

    @Override
    public double getProgress() {
        return 0.0; // Tasks start with 0 progress
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public ai.demo.agent.base.task.Task getParent() {
        return null; // Chat tasks don't have parents
    }

    @Override
    public List<ai.demo.agent.base.task.Task> getSubTasks() {
        return List.of(); // Chat tasks don't have subtasks
    }

    @Override
    public List<ai.demo.agent.base.task.TaskAttempt> getAttempts() {
        return List.of(); // Attempts tracked separately
    }

    @Override
    public int getMaxAttempts() {
        return 3; // Standard retry limit
    }

    @Override
    public ai.demo.agent.base.task.TaskAttempt getCurrentAttempt() {
        return null; // Managed by framework
    }

    public ChatRequest getChatRequest() {
        return chatRequest;
    }

    @Override
    public String toString() {
        return String.format("ChatTask{id='%s', model='%s', threadId='%s'}",
                id, chatRequest.getModel(), chatRequest.getThreadId());
    }
}