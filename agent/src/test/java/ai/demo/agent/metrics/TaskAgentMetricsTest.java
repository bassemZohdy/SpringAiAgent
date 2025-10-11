package ai.demo.agent.metrics;

import ai.demo.agent.base.task.TaskPriority;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class TaskAgentMetricsTest {

    @Test
    void testTaskSpecificOperations() {
        TaskAgentMetrics metrics = new TaskAgentMetrics();

        // Test task-specific operations
        metrics.recordTaskSucceeded(Duration.ofMillis(10).toNanos(), 100, 200, TaskPriority.HIGH);
        metrics.recordTaskFailed(Duration.ofMillis(20).toNanos(), 50, TaskPriority.MEDIUM);
        metrics.recordTaskRetry();

        assertEquals(2, metrics.getTasksProcessed());
        assertEquals(1, metrics.getTasksSucceeded());
        assertEquals(1, metrics.getTasksFailed());
        assertEquals(1, metrics.getTasksRetried());
        assertEquals(0.5, metrics.getTaskSuccessRate());
        assertEquals(0.5, metrics.getTaskFailureRate());
        assertEquals(0.5, metrics.getRetryRate());
        assertEquals(150.0, metrics.getAverageInputSize()); // (100 + 50) / 2
        assertEquals(200.0, metrics.getAverageOutputSize()); // Only successful tasks produce output
        assertEquals(1, metrics.getHighPriorityTasks());
        assertEquals(1, metrics.getMediumPriorityTasks());
        assertEquals(0, metrics.getLowPriorityTasks());

        String summary = metrics.getSummary();
        assertTrue(summary.contains("tasks=2"));
        assertTrue(summary.contains("succeeded=1"));
        assertTrue(summary.contains("failed=1"));
        assertTrue(summary.contains("retries=1"));
        assertTrue(summary.contains("priorityDist[H:1,M:1,L:0]"));
    }

    @Test
    void testPriorityDistribution() {
        TaskAgentMetrics metrics = new TaskAgentMetrics();

        metrics.recordTaskSucceeded(Duration.ofMillis(10).toNanos(), 100, 200, TaskPriority.HIGH);
        metrics.recordTaskSucceeded(Duration.ofMillis(15).toNanos(), 150, 250, TaskPriority.HIGH);
        metrics.recordTaskSucceeded(Duration.ofMillis(20).toNanos(), 120, 220, TaskPriority.MEDIUM);
        metrics.recordTaskSucceeded(Duration.ofMillis(25).toNanos(), 80, 180, TaskPriority.LOW);

        assertEquals(4, metrics.getTasksProcessed());
        assertEquals(2, metrics.getHighPriorityTasks());
        assertEquals(1, metrics.getMediumPriorityTasks());
        assertEquals(1, metrics.getLowPriorityTasks());
    }

    @Test
    void testResetClearsTaskSpecificMetrics() {
        TaskAgentMetrics metrics = new TaskAgentMetrics();
        metrics.recordTaskSucceeded(Duration.ofMillis(10).toNanos(), 100, 200, TaskPriority.NORMAL);
        metrics.recordTaskRetry();

        metrics.reset();

        assertEquals(0, metrics.getTasksProcessed());
        assertEquals(0, metrics.getTasksSucceeded());
        assertEquals(0, metrics.getTasksFailed());
        assertEquals(0, metrics.getTasksRetried());
        assertEquals(0, metrics.getTotalInputSize());
        assertEquals(0, metrics.getTotalOutputSize());
        assertEquals(0, metrics.getHighPriorityTasks());
        assertEquals(0, metrics.getMediumPriorityTasks());
        assertEquals(0, metrics.getLowPriorityTasks());
    }
}