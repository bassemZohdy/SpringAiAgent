package ai.demo.agent.metrics;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class AgentMetricsTest {

    @Test
    void testTaskAgentMetricsBasicOperations() {
        TaskAgentMetrics metrics = new TaskAgentMetrics();

        // Test basic operations
        metrics.recordOperationStarted();
        metrics.recordOperationSucceeded(Duration.ofMillis(50).toNanos());
        metrics.recordOperationFailed(Duration.ofMillis(100).toNanos());

        assertEquals(2, metrics.getOperationsProcessed());
        assertEquals(1, metrics.getOperationsSucceeded());
        assertEquals(1, metrics.getOperationsFailed());
        assertEquals(0.5, metrics.getSuccessRate());
        assertEquals(0.5, metrics.getFailureRate());
        assertEquals(Duration.ofMillis(75), metrics.getAverageProcessingTime());
        assertEquals(Duration.ofMillis(50), metrics.getMinProcessingTime());
        assertEquals(Duration.ofMillis(100), metrics.getMaxProcessingTime());
        assertEquals(Duration.ofMillis(150), metrics.getTotalProcessingTime());
        assertNotNull(metrics.getLastOperationStartTime());
        assertNotNull(metrics.getLastOperationEndTime());
        assertTrue(metrics.getUptime().toMillis() >= 0);
        assertTrue(metrics.getThroughput() >= 0.0);

        String summary = metrics.getSummary();
        assertTrue(summary.contains("processed=2"));
        assertTrue(summary.contains("succeeded=1"));
        assertTrue(summary.contains("failed=1"));
    }

    @Test
    void testTaskAgentMetricsTaskSpecificOperations() {
        TaskAgentMetrics metrics = new TaskAgentMetrics();

        // Test task-specific operations
        metrics.recordTaskSucceeded(Duration.ofMillis(10).toNanos(), 100, 200, ai.demo.agent.base.task.TaskPriority.HIGH);
        metrics.recordTaskFailed(Duration.ofMillis(20).toNanos(), 50, ai.demo.agent.base.task.TaskPriority.MEDIUM);

        assertEquals(2, metrics.getTasksProcessed());
        assertEquals(1, metrics.getTasksSucceeded());
        assertEquals(1, metrics.getTasksFailed());
        assertEquals(0.5, metrics.getTaskSuccessRate());
        assertEquals(0.5, metrics.getTaskFailureRate());
        assertEquals(150.0, metrics.getAverageInputSize()); // (100 + 50) / 2
        assertEquals(200.0, metrics.getAverageOutputSize()); // Only successful tasks produce output
        assertEquals(1, metrics.getHighPriorityTasks());
        assertEquals(1, metrics.getMediumPriorityTasks());
        assertEquals(0, metrics.getLowPriorityTasks());

        String summary = metrics.getSummary();
        assertTrue(summary.contains("tasks=2"));
        assertTrue(summary.contains("succeeded=1"));
        assertTrue(summary.contains("failed=1"));
        assertTrue(summary.contains("priorityDist[H:1,M:1,L:0]"));
    }

    @Test
    void testChatAgentMetricsBasicOperations() {
        ChatAgentMetrics metrics = new ChatAgentMetrics();

        // Test conversation operations
        metrics.recordConversationStarted();
        metrics.recordConversationCompleted(5, 3, 2);
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

        String summary = metrics.getSummary();
        assertTrue(summary.contains("conversations=2"));
        assertTrue(summary.contains("completed=1"));
        assertTrue(summary.contains("abandoned=1"));
        assertTrue(summary.contains("messages=5"));
    }

    @Test
    void testChatAgentMetricsTokenOperations() {
        ChatAgentMetrics metrics = new ChatAgentMetrics();

        metrics.recordMessageProcessed(Duration.ofMillis(10).toNanos(), 100, 50, true);  // User message
        metrics.recordMessageProcessed(Duration.ofMillis(15).toNanos(), 200, 100, false); // Agent message

        assertEquals(150, metrics.getTotalInputTokens());  // 100 + 200
        assertEquals(150, metrics.getTotalOutputTokens()); // 50 + 100
        assertEquals(300, metrics.getTotalTokens());
        assertEquals(75.0, metrics.getAverageInputTokensPerMessage());  // 150 / 2
        assertEquals(75.0, metrics.getAverageOutputTokensPerMessage()); // 150 / 2

        String summary = metrics.getSummary();
        assertTrue(summary.contains("tokens=300"));
        assertTrue(summary.contains("in:150"));
        assertTrue(summary.contains("out:150"));
    }

    @Test
    void testResetClearsMetrics() {
        TaskAgentMetrics metrics = new TaskAgentMetrics();
        metrics.recordTaskSucceeded(Duration.ofMillis(10).toNanos(), 100, 200, ai.demo.agent.base.task.TaskPriority.NORMAL);
        metrics.recordTaskFailed(Duration.ofMillis(20).toNanos(), 50, ai.demo.agent.base.task.TaskPriority.NORMAL);

        metrics.reset();

        assertEquals(0, metrics.getOperationsProcessed());
        assertEquals(0, metrics.getOperationsSucceeded());
        assertEquals(0, metrics.getOperationsFailed());
        assertEquals(0, metrics.getTasksProcessed());
        assertEquals(0, metrics.getTasksSucceeded());
        assertEquals(0, metrics.getTasksFailed());
        assertEquals(Duration.ZERO, metrics.getAverageProcessingTime());
        assertEquals(Duration.ZERO, metrics.getMinProcessingTime());
        assertEquals(Duration.ZERO, metrics.getMaxProcessingTime());
        assertNull(metrics.getLastOperationStartTime());
        assertNull(metrics.getLastOperationEndTime());
    }
}