package ai.demo.agent.metrics;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ChatAgentMetricsTest {

    @Test
    void testConversationOperations() {
        ChatAgentMetrics metrics = new ChatAgentMetrics();

        // Test conversation operations
        metrics.recordConversationStarted();
        Instant startTime = metrics.getLastConversationStartTime();
        metrics.recordConversationCompleted(5, 3, 2);
        metrics.recordConversationStarted();
        metrics.recordConversationAbandoned();

        assertEquals(2, metrics.getConversationsStarted());
        assertEquals(1, metrics.getConversationsCompleted());
        assertEquals(1, metrics.getConversationsAbandoned());
        assertEquals(0.5, metrics.getConversationCompletionRate());
        assertEquals(0.5, metrics.getConversationAbandonmentRate());
        assertEquals(5, metrics.getTotalMessages());
        assertEquals(3, metrics.getTotalUserMessages());
        assertEquals(2, metrics.getTotalAgentMessages());
        assertEquals(5.0, metrics.getAverageMessagesPerConversation()); // 5 messages / 1 completed conversation
        assertEquals(2, metrics.getMaxConcurrentConversations()); // Max concurrent reached
        assertNotNull(startTime);
        assertTrue(metrics.getLastConversationEndTime().isAfter(startTime));

        String summary = metrics.getSummary();
        assertTrue(summary.contains("conversations=2"));
        assertTrue(summary.contains("completed=1"));
        assertTrue(summary.contains("abandoned=1"));
        assertTrue(summary.contains("messages=5"));
        assertTrue(summary.contains("maxConcurrent=2"));
    }

    @Test
    void testTokenOperations() {
        ChatAgentMetrics metrics = new ChatAgentMetrics();

        metrics.recordMessageProcessed(Duration.ofMillis(10).toNanos(), 100, 50, true);  // User message
        metrics.recordMessageProcessed(Duration.ofMillis(15).toNanos(), 200, 100, false); // Agent message
        metrics.recordMessageProcessingFailed(Duration.ofMillis(5).toNanos(), 75, true);   // Failed user message

        assertEquals(375, metrics.getTotalInputTokens());  // 100 + 200 + 75
        assertEquals(150, metrics.getTotalOutputTokens()); // 50 + 100
        assertEquals(525, metrics.getTotalTokens());
        assertEquals(125.0, metrics.getAverageInputTokensPerMessage());  // 375 / 3
        assertEquals(50.0, metrics.getAverageOutputTokensPerMessage());  // 150 / 3
        assertEquals(2, metrics.getTotalUserMessages()); // 2 user messages (1 success + 1 failed)
        assertEquals(1, metrics.getTotalAgentMessages()); // 1 agent message

        String summary = metrics.getSummary();
        assertTrue(summary.contains("tokens=525"));
        assertTrue(summary.contains("in:375"));
        assertTrue(summary.contains("out:150"));
    }

    @Test
    void testActiveConversations() {
        ChatAgentMetrics metrics = new ChatAgentMetrics();

        // Start multiple conversations
        metrics.recordConversationStarted(); // 1 active
        assertEquals(1, metrics.getCurrentActiveConversations());
        assertEquals(1, metrics.getMaxConcurrentConversations());

        metrics.recordConversationStarted(); // 2 active
        assertEquals(2, metrics.getCurrentActiveConversations());
        assertEquals(2, metrics.getMaxConcurrentConcurrentConversations());

        metrics.recordConversationCompleted(3, 2, 1); // 1 active
        assertEquals(1, metrics.getCurrentActiveConversations());
        assertEquals(2, metrics.getMaxConcurrentConcurrentConversations());

        metrics.recordConversationCompleted(2, 1, 1); // 0 active
        assertEquals(0, metrics.getCurrentActiveConversations());
        assertEquals(2, metrics.getMaxConcurrentConcurrentConversations());
    }

    @Test
    void testResetClearsChatSpecificMetrics() {
        ChatAgentMetrics metrics = new ChatAgentMetrics();
        metrics.recordConversationStarted();
        metrics.recordConversationCompleted(5, 3, 2);
        metrics.recordMessageProcessed(Duration.ofMillis(10).toNanos(), 100, 50, true);

        metrics.reset();

        assertEquals(0, metrics.getConversationsStarted());
        assertEquals(0, metrics.getConversationsCompleted());
        assertEquals(0, metrics.getConversationsAbandoned());
        assertEquals(0, metrics.getCurrentActiveConversations());
        assertEquals(0, metrics.getMaxConcurrentConcurrentConversations());
        assertEquals(0, metrics.getTotalMessages());
        assertEquals(0, metrics.getTotalUserMessages());
        assertEquals(0, metrics.getTotalAgentMessages());
        assertEquals(0, metrics.getTotalInputTokens());
        assertEquals(0, metrics.getTotalOutputTokens());
        assertEquals(0, metrics.getTotalTokens());
        assertNull(metrics.getLastConversationStartTime());
        assertNull(metrics.getLastConversationEndTime());
    }
}