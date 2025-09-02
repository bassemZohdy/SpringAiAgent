package ai.demo.agent.base;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Metrics collection for agent performance monitoring.
 * All methods are thread-safe for concurrent access.
 */
public class AgentMetrics {
    
    private final AtomicLong tasksProcessed = new AtomicLong(0);
    private final AtomicLong tasksSucceeded = new AtomicLong(0);
    private final AtomicLong tasksFailed = new AtomicLong(0);
    private final AtomicLong totalProcessingTimeNanos = new AtomicLong(0);
    private final AtomicLong minProcessingTimeNanos = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxProcessingTimeNanos = new AtomicLong(0);
    private final AtomicReference<Instant> lastTaskStartTime = new AtomicReference<>();
    private final AtomicReference<Instant> lastTaskEndTime = new AtomicReference<>();
    private final Instant agentStartTime;
    
    /**
     * Create new agent metrics instance.
     */
    public AgentMetrics() {
        this.agentStartTime = Instant.now();
    }
    
    /**
     * Record the start of a task processing.
     */
    public void recordTaskStarted() {
        lastTaskStartTime.set(Instant.now());
    }
    
    /**
     * Record the successful completion of a task.
     * 
     * @param processingTimeNanos the time taken to process the task in nanoseconds
     */
    public void recordTaskSucceeded(long processingTimeNanos) {
        tasksProcessed.incrementAndGet();
        tasksSucceeded.incrementAndGet();
        totalProcessingTimeNanos.addAndGet(processingTimeNanos);
        updateMinMaxProcessingTime(processingTimeNanos);
        lastTaskEndTime.set(Instant.now());
    }
    
    /**
     * Record the failure of a task.
     * 
     * @param processingTimeNanos the time taken before the task failed in nanoseconds
     */
    public void recordTaskFailed(long processingTimeNanos) {
        tasksProcessed.incrementAndGet();
        tasksFailed.incrementAndGet();
        totalProcessingTimeNanos.addAndGet(processingTimeNanos);
        updateMinMaxProcessingTime(processingTimeNanos);
        lastTaskEndTime.set(Instant.now());
    }
    
    private void updateMinMaxProcessingTime(long processingTimeNanos) {
        minProcessingTimeNanos.accumulateAndGet(processingTimeNanos, Long::min);
        maxProcessingTimeNanos.accumulateAndGet(processingTimeNanos, Long::max);
    }
    
    /**
     * Get the total number of tasks processed (succeeded + failed).
     * 
     * @return the total number of tasks processed
     */
    public long getTasksProcessed() {
        return tasksProcessed.get();
    }
    
    /**
     * Get the number of tasks that completed successfully.
     * 
     * @return the number of successful tasks
     */
    public long getTasksSucceeded() {
        return tasksSucceeded.get();
    }
    
    /**
     * Get the number of tasks that failed.
     * 
     * @return the number of failed tasks
     */
    public long getTasksFailed() {
        return tasksFailed.get();
    }
    
    /**
     * Get the success rate as a percentage (0.0 to 1.0).
     * 
     * @return the success rate, or 0.0 if no tasks have been processed
     */
    public double getSuccessRate() {
        long total = tasksProcessed.get();
        return total > 0 ? (double) tasksSucceeded.get() / total : 0.0;
    }
    
    /**
     * Get the failure rate as a percentage (0.0 to 1.0).
     * 
     * @return the failure rate, or 0.0 if no tasks have been processed
     */
    public double getFailureRate() {
        long total = tasksProcessed.get();
        return total > 0 ? (double) tasksFailed.get() / total : 0.0;
    }
    
    /**
     * Get the average processing time per task.
     * 
     * @return the average processing time, or Duration.ZERO if no tasks have been processed
     */
    public Duration getAverageProcessingTime() {
        long total = tasksProcessed.get();
        if (total > 0) {
            long avgNanos = totalProcessingTimeNanos.get() / total;
            return Duration.ofNanos(avgNanos);
        }
        return Duration.ZERO;
    }
    
    /**
     * Get the minimum processing time recorded.
     * 
     * @return the minimum processing time, or Duration.ZERO if no tasks have been processed
     */
    public Duration getMinProcessingTime() {
        long min = minProcessingTimeNanos.get();
        return min == Long.MAX_VALUE ? Duration.ZERO : Duration.ofNanos(min);
    }
    
    /**
     * Get the maximum processing time recorded.
     * 
     * @return the maximum processing time, or Duration.ZERO if no tasks have been processed
     */
    public Duration getMaxProcessingTime() {
        return Duration.ofNanos(maxProcessingTimeNanos.get());
    }
    
    /**
     * Get the total processing time for all tasks.
     * 
     * @return the total processing time
     */
    public Duration getTotalProcessingTime() {
        return Duration.ofNanos(totalProcessingTimeNanos.get());
    }
    
    /**
     * Get the time when the last task started.
     * 
     * @return the last task start time, or null if no tasks have been started
     */
    public Instant getLastTaskStartTime() {
        return lastTaskStartTime.get();
    }
    
    /**
     * Get the time when the last task ended.
     * 
     * @return the last task end time, or null if no tasks have been completed
     */
    public Instant getLastTaskEndTime() {
        return lastTaskEndTime.get();
    }
    
    /**
     * Get the uptime of the agent since metrics collection started.
     * 
     * @return the agent uptime
     */
    public Duration getUptime() {
        return Duration.between(agentStartTime, Instant.now());
    }
    
    /**
     * Get the throughput in tasks per second.
     * 
     * @return the throughput, or 0.0 if no tasks have been processed
     */
    public double getThroughput() {
        long total = tasksProcessed.get();
        if (total > 0) {
            Duration uptime = getUptime();
            double uptimeSeconds = uptime.toMillis() / 1000.0;
            return uptimeSeconds > 0 ? total / uptimeSeconds : 0.0;
        }
        return 0.0;
    }
    
    /**
     * Reset all metrics to their initial values.
     */
    public void reset() {
        tasksProcessed.set(0);
        tasksSucceeded.set(0);
        tasksFailed.set(0);
        totalProcessingTimeNanos.set(0);
        minProcessingTimeNanos.set(Long.MAX_VALUE);
        maxProcessingTimeNanos.set(0);
        lastTaskStartTime.set(null);
        lastTaskEndTime.set(null);
    }
    
    /**
     * Get a summary of the current metrics.
     * 
     * @return a formatted metrics summary
     */
    public String getSummary() {
        return String.format(
            "AgentMetrics{processed=%d, succeeded=%d, failed=%d, successRate=%.2f%%, " +
            "avgTime=%s, minTime=%s, maxTime=%s, throughput=%.2f tasks/sec, uptime=%s}",
            getTasksProcessed(),
            getTasksSucceeded(), 
            getTasksFailed(),
            getSuccessRate() * 100,
            getAverageProcessingTime(),
            getMinProcessingTime(),
            getMaxProcessingTime(),
            getThroughput(),
            getUptime()
        );
    }
    
    @Override
    public String toString() {
        return getSummary();
    }
}