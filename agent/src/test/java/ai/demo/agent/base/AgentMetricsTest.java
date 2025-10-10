package ai.demo.agent.base;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class AgentMetricsTest {

    @Test
    void testRecordingMetricsAndSummary() {
        AgentMetrics metrics = new AgentMetrics();

        metrics.recordTaskStarted();
        metrics.recordTaskSucceeded(Duration.ofMillis(50).toNanos());
        metrics.recordTaskFailed(Duration.ofMillis(100).toNanos());

        assertEquals(2, metrics.getTasksProcessed());
        assertEquals(1, metrics.getTasksSucceeded());
        assertEquals(1, metrics.getTasksFailed());
        assertEquals(0.5, metrics.getSuccessRate());
        assertEquals(0.5, metrics.getFailureRate());
        assertEquals(Duration.ofMillis(75), metrics.getAverageProcessingTime());
        assertEquals(Duration.ofMillis(50), metrics.getMinProcessingTime());
        assertEquals(Duration.ofMillis(100), metrics.getMaxProcessingTime());
        assertEquals(Duration.ofMillis(150), metrics.getTotalProcessingTime());
        assertNotNull(metrics.getLastTaskStartTime());
        assertNotNull(metrics.getLastTaskEndTime());
        assertTrue(metrics.getUptime().toMillis() >= 0);
        assertTrue(metrics.getThroughput() >= 0.0);

        String summary = metrics.getSummary();
        assertTrue(summary.contains("processed=2"));
        assertTrue(summary.contains("succeeded=1"));
        assertTrue(summary.contains("failed=1"));
    }

    @Test
    void testResetClearsMetrics() {
        AgentMetrics metrics = new AgentMetrics();
        metrics.recordTaskSucceeded(Duration.ofMillis(10).toNanos());
        metrics.recordTaskFailed(Duration.ofMillis(20).toNanos());

        metrics.reset();

        assertEquals(0, metrics.getTasksProcessed());
        assertEquals(0, metrics.getTasksSucceeded());
        assertEquals(0, metrics.getTasksFailed());
        assertEquals(Duration.ZERO, metrics.getAverageProcessingTime());
        assertEquals(Duration.ZERO, metrics.getMinProcessingTime());
        assertEquals(Duration.ZERO, metrics.getMaxProcessingTime());
        assertNull(metrics.getLastTaskStartTime());
        assertNull(metrics.getLastTaskEndTime());
    }
}
