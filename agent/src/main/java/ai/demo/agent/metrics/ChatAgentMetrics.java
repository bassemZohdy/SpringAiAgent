package ai.demo.agent.metrics;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class ChatAgentMetrics extends AgentMetrics {

    private final AtomicLong conversationsStarted = new AtomicLong(0);
    private final AtomicLong conversationsCompleted = new AtomicLong(0);
    private final AtomicLong conversationsAbandoned = new AtomicLong(0);
    private final AtomicLong totalMessages = new AtomicLong(0);
    private final AtomicLong totalUserMessages = new AtomicLong(0);
    private final AtomicLong totalAgentMessages = new AtomicLong(0);
    private final AtomicLong totalInputTokens = new AtomicLong(0);
    private final AtomicLong totalOutputTokens = new AtomicLong(0);
    private final AtomicReference<Instant> lastConversationStartTime = new AtomicReference<>();
    private final AtomicReference<Instant> lastConversationEndTime = new AtomicReference<>();
    private final AtomicLong currentActiveConversations = new AtomicLong(0);
    private final AtomicLong maxConcurrentConversations = new AtomicLong(0);

    public ChatAgentMetrics() {
        super();
    }

    public void recordConversationStarted() {
        conversationsStarted.incrementAndGet();
        currentActiveConversations.incrementAndGet();
        lastConversationStartTime.set(Instant.now());
        maxConcurrentConversations.accumulateAndGet(currentActiveConversations.get(), Long::max);
    }

    public void recordConversationCompleted(long totalMessages, long totalUserMessages, long totalAgentMessages) {
        conversationsCompleted.incrementAndGet();
        currentActiveConversations.decrementAndGet();
        lastConversationEndTime.set(Instant.now());
        this.totalMessages.addAndGet(totalMessages);
        this.totalUserMessages.addAndGet(totalUserMessages);
        this.totalAgentMessages.addAndGet(totalAgentMessages);
    }

    public void recordConversationAbandoned() {
        conversationsAbandoned.incrementAndGet();
        currentActiveConversations.decrementAndGet();
        lastConversationEndTime.set(Instant.now());
    }

    public void recordMessageProcessed(long processingTimeNanos, long inputTokens, long outputTokens, boolean isUserMessage) {
        super.recordOperationSucceeded(processingTimeNanos);
        totalInputTokens.addAndGet(inputTokens);
        totalOutputTokens.addAndGet(outputTokens);
        if (isUserMessage) {
            totalUserMessages.incrementAndGet();
        } else {
            totalAgentMessages.incrementAndGet();
        }
    }

    public void recordMessageProcessingFailed(long processingTimeNanos, long inputTokens, boolean isUserMessage) {
        super.recordOperationFailed(processingTimeNanos);
        totalInputTokens.addAndGet(inputTokens);
        if (isUserMessage) {
            totalUserMessages.incrementAndGet();
        } else {
            totalAgentMessages.incrementAndGet();
        }
    }

    public long getConversationsStarted() {
        return conversationsStarted.get();
    }

    public long getConversationsCompleted() {
        return conversationsCompleted.get();
    }

    public long getConversationsAbandoned() {
        return conversationsAbandoned.get();
    }

    public long getCurrentActiveConversations() {
        return currentActiveConversations.get();
    }

    public long getMaxConcurrentConversations() {
        return maxConcurrentConversations.get();
    }

    public double getConversationCompletionRate() {
        long total = conversationsStarted.get();
        return total > 0 ? (double) conversationsCompleted.get() / total : 0.0;
    }

    public double getConversationAbandonmentRate() {
        long total = conversationsStarted.get();
        return total > 0 ? (double) conversationsAbandoned.get() / total : 0.0;
    }

    public long getTotalMessages() {
        return totalMessages.get();
    }

    public long getTotalUserMessages() {
        return totalUserMessages.get();
    }

    public long getTotalAgentMessages() {
        return totalAgentMessages.get();
    }

    public long getTotalInputTokens() {
        return totalInputTokens.get();
    }

    public long getTotalOutputTokens() {
        return totalOutputTokens.get();
    }

    public long getTotalTokens() {
        return totalInputTokens.get() + totalOutputTokens.get();
    }

    public double getAverageMessagesPerConversation() {
        long completed = conversationsCompleted.get();
        return completed > 0 ? (double) totalMessages.get() / completed : 0.0;
    }

    public double getAverageInputTokensPerMessage() {
        long messages = totalMessages.get();
        return messages > 0 ? (double) totalInputTokens.get() / messages : 0.0;
    }

    public double getAverageOutputTokensPerMessage() {
        long messages = totalMessages.get();
        return messages > 0 ? (double) totalOutputTokens.get() / messages : 0.0;
    }

    public Instant getLastConversationStartTime() {
        return lastConversationStartTime.get();
    }

    public Instant getLastConversationEndTime() {
        return lastConversationEndTime.get();
    }

    @Override
    public void reset() {
        super.reset();
        conversationsStarted.set(0);
        conversationsCompleted.set(0);
        conversationsAbandoned.set(0);
        totalMessages.set(0);
        totalUserMessages.set(0);
        totalAgentMessages.set(0);
        totalInputTokens.set(0);
        totalOutputTokens.set(0);
        lastConversationStartTime.set(null);
        lastConversationEndTime.set(null);
        currentActiveConversations.set(0);
        maxConcurrentConversations.set(0);
    }

    @Override
    public String getSummary() {
        return String.format(
            "%s{conversations=%d(started),%d(completed),%d(abandoned), completionRate=%.2f%%, " +
            "active=%d, maxConcurrent=%d, messages=%d(user:%d,agent:%d), tokens=%d(in:%d,out:%d), %s}",
            this.getClass().getSimpleName(),
            getConversationsStarted(),
            getConversationsCompleted(),
            getConversationsAbandoned(),
            getConversationCompletionRate() * 100,
            getCurrentActiveConversations(),
            getMaxConcurrentConversations(),
            getTotalMessages(),
            getTotalUserMessages(),
            getTotalAgentMessages(),
            getTotalTokens(),
            getTotalInputTokens(),
            getTotalOutputTokens(),
            super.getSummary().substring(this.getClass().getSimpleName().length() + 1)
        );
    }
}