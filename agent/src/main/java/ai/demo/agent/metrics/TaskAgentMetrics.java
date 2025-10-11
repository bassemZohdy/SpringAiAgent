package ai.demo.agent.metrics;

import ai.demo.agent.base.task.TaskPriority;

import java.util.concurrent.atomic.AtomicLong;

public class TaskAgentMetrics extends AgentMetrics {

    private final AtomicLong tasksProcessed = new AtomicLong(0);
    private final AtomicLong tasksSucceeded = new AtomicLong(0);
    private final AtomicLong tasksFailed = new AtomicLong(0);
    private final AtomicLong tasksRetried = new AtomicLong(0);
    private final AtomicLong totalInputSize = new AtomicLong(0);
    private final AtomicLong totalOutputSize = new AtomicLong(0);
    private final AtomicLong highPriorityTasks = new AtomicLong(0);
    private final AtomicLong mediumPriorityTasks = new AtomicLong(0);
    private final AtomicLong lowPriorityTasks = new AtomicLong(0);

    public TaskAgentMetrics() {
        super();
    }

    @Override
    public void recordOperationStarted() {
        super.recordOperationStarted();
    }

    public void recordTaskSucceeded(long processingTimeNanos, long inputSize, long outputSize, TaskPriority priority) {
        super.recordOperationSucceeded(processingTimeNanos);

        tasksProcessed.incrementAndGet();
        tasksSucceeded.incrementAndGet();
        totalInputSize.addAndGet(inputSize);
        totalOutputSize.addAndGet(outputSize);

        switch (priority) {
            case HIGH -> highPriorityTasks.incrementAndGet();
            case MEDIUM -> mediumPriorityTasks.incrementAndGet();
            case LOW -> lowPriorityTasks.incrementAndGet();
        }
    }

    public void recordTaskFailed(long processingTimeNanos, long inputSize, TaskPriority priority) {
        super.recordOperationFailed(processingTimeNanos);

        tasksProcessed.incrementAndGet();
        tasksFailed.incrementAndGet();
        totalInputSize.addAndGet(inputSize);

        switch (priority) {
            case HIGH -> highPriorityTasks.incrementAndGet();
            case MEDIUM -> mediumPriorityTasks.incrementAndGet();
            case LOW -> lowPriorityTasks.incrementAndGet();
        }
    }

    public void recordTaskRetry() {
        tasksRetried.incrementAndGet();
    }

    public long getTasksProcessed() {
        return tasksProcessed.get();
    }

    public long getTasksSucceeded() {
        return tasksSucceeded.get();
    }

    public long getTasksFailed() {
        return tasksFailed.get();
    }

    public long getTasksRetried() {
        return tasksRetried.get();
    }

    public double getTaskSuccessRate() {
        long total = tasksProcessed.get();
        return total > 0 ? (double) tasksSucceeded.get() / total : 0.0;
    }

    public double getTaskFailureRate() {
        long total = tasksProcessed.get();
        return total > 0 ? (double) tasksFailed.get() / total : 0.0;
    }

    public double getRetryRate() {
        long total = tasksProcessed.get();
        return total > 0 ? (double) tasksRetried.get() / total : 0.0;
    }

    public long getTotalInputSize() {
        return totalInputSize.get();
    }

    public long getTotalOutputSize() {
        return totalOutputSize.get();
    }

    public double getAverageInputSize() {
        long total = tasksProcessed.get();
        return total > 0 ? (double) totalInputSize.get() / total : 0.0;
    }

    public double getAverageOutputSize() {
        long total = tasksSucceeded.get();
        return total > 0 ? (double) totalOutputSize.get() / total : 0.0;
    }

    public long getHighPriorityTasks() {
        return highPriorityTasks.get();
    }

    public long getMediumPriorityTasks() {
        return mediumPriorityTasks.get();
    }

    public long getLowPriorityTasks() {
        return lowPriorityTasks.get();
    }

    @Override
    public void reset() {
        super.reset();
        tasksProcessed.set(0);
        tasksSucceeded.set(0);
        tasksFailed.set(0);
        tasksRetried.set(0);
        totalInputSize.set(0);
        totalOutputSize.set(0);
        highPriorityTasks.set(0);
        mediumPriorityTasks.set(0);
        lowPriorityTasks.set(0);
    }

    @Override
    public String getSummary() {
        return String.format(
            "%s{tasks=%d, succeeded=%d, failed=%d, retries=%d, successRate=%.2f%%, " +
            "avgInputSize=%.1f, avgOutputSize=%.1f, priorityDist[H:%d,M:%d,L:%d], %s}",
            this.getClass().getSimpleName(),
            getTasksProcessed(),
            getTasksSucceeded(),
            getTasksFailed(),
            getTasksRetried(),
            getTaskSuccessRate() * 100,
            getAverageInputSize(),
            getAverageOutputSize(),
            getHighPriorityTasks(),
            getMediumPriorityTasks(),
            getLowPriorityTasks(),
            super.getSummary().substring(this.getClass().getSimpleName().length() + 1)
        );
    }
}