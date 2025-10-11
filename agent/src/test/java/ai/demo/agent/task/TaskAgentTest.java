package ai.demo.agent.task;

import ai.demo.agent.base.Agent;
import ai.demo.agent.base.AgentConfiguration;
import ai.demo.agent.base.AgentState;
import ai.demo.agent.base.AgentException;
import ai.demo.agent.base.BaseAgent;
import ai.demo.agent.base.task.Task;
import ai.demo.agent.base.task.TaskPriority;
import ai.demo.agent.base.task.TaskStatus;
import ai.demo.agent.base.task.TaskSize;
import ai.demo.agent.metrics.TaskAgentMetrics;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TaskAgentTest {

    @Test
    void testTaskAgentInterface() {
        TestTaskAgent agent = new TestTaskAgent();

        // Test that it implements both Agent and TaskAgent
        assertTrue(agent instanceof Agent);
        assertTrue(agent instanceof TaskAgent);
        assertTrue(agent instanceof BaseAgent);

        // Test that it returns TaskAgentMetrics
        assertTrue(agent.getMetrics() instanceof TaskAgentMetrics);
    }

    @Test
    void testTaskProcessing() throws AgentException {
        TestTaskAgent agent = new TestTaskAgent();
        agent.start();

        SimpleTask task = SimpleTask.success("test-task");
        CompletableFuture<String> future = agent.process(task);
        String result = future.join();

        assertEquals("Processed: " + task.getDescription(), result);
        assertEquals(1, agent.getStartedCount());
        assertEquals(1, agent.getCompletedCount());

        // Test metrics
        TaskAgentMetrics metrics = (TaskAgentMetrics) agent.getMetrics();
        assertEquals(1, metrics.getTasksProcessed());
        assertEquals(1, metrics.getTasksSucceeded());
        assertEquals(0, metrics.getTasksFailed());
    }

    @Test
    void testTaskFailureHandling() throws AgentException {
        TestTaskAgent agent = new TestTaskAgent();
        agent.start();

        SimpleTask task = SimpleTask.failure("fail-task");
        CompletableFuture<String> future = agent.process(task);
        assertThrows(RuntimeException.class, future::join);

        assertEquals(1, agent.getFailedCount());

        TaskAgentMetrics metrics = (TaskAgentMetrics) agent.getMetrics();
        assertEquals(1, metrics.getTasksProcessed());
        assertEquals(0, metrics.getTasksSucceeded());
        assertEquals(1, metrics.getTasksFailed());
    }

    @Test
    void testTaskPriorityTracking() throws AgentException {
        TestTaskAgent agent = new TestTaskAgent();
        agent.start();

        // Process tasks with different priorities
        SimpleTask highPriorityTask = SimpleTask.success("high", TaskPriority.HIGH);
        SimpleTask mediumPriorityTask = SimpleTask.success("medium", TaskPriority.MEDIUM);
        SimpleTask lowPriorityTask = SimpleTask.success("low", TaskPriority.LOW);

        agent.process(highPriorityTask).join();
        agent.process(mediumPriorityTask).join();
        agent.process(lowPriorityTask).join();

        TaskAgentMetrics metrics = (TaskAgentMetrics) agent.getMetrics();
        assertEquals(3, metrics.getTasksProcessed());
        assertEquals(3, metrics.getTasksSucceeded());
        assertEquals(1, metrics.getHighPriorityTasks());
        assertEquals(1, metrics.getMediumPriorityTasks());
        assertEquals(1, metrics.getLowPriorityTasks());
    }

    @Test
    void testTaskSizeTracking() throws AgentException {
        TestTaskAgent agent = new TestTaskAgent();
        agent.start();

        SimpleTask smallTask = SimpleTask.success("small", TaskPriority.NORMAL, TaskSize.SMALL);
        SimpleTask largeTask = SimpleTask.success("large", TaskPriority.NORMAL, TaskSize.LARGE);

        agent.process(smallTask).join();
        agent.process(largeTask).join();

        TaskAgentMetrics metrics = (TaskAgentMetrics) agent.getMetrics();
        assertEquals(2, metrics.getTasksProcessed());
        assertEquals(100.0, metrics.getAverageInputSize()); // (100 + 100) / 2
        assertEquals(150.0, metrics.getAverageOutputSize()); // (100 + 200) / 2
    }

    @Test
    void testTaskRetryHandling() throws AgentException {
        TestTaskAgent agent = new TestTaskAgent();
        agent.start();

        SimpleTask task = SimpleTask.success("retry-task");
        // Simulate a retry scenario
        agent.process(task).join();

        TaskAgentMetrics metrics = (TaskAgentMetrics) agent.getMetrics();
        assertEquals(1, metrics.getTasksRetried());
        assertEquals(0.5, metrics.getRetryRate()); // 1 retry / 2 total (original + retry)
    }

    @Test
    void testProcessWhenNotRunningFails() {
        TestTaskAgent agent = new TestTaskAgent();

        SimpleTask task = SimpleTask.success("idle");
        CompletableFuture<String> future = agent.process(task);
        assertThrows(AgentException.class, future::join);
    }

    private static final class TestTaskAgent extends BaseAgent<SimpleTask, String> implements AutoCloseable {
        private final AtomicInteger startedCount = new AtomicInteger();
        private final AtomicInteger completedCount = new AtomicInteger();
        private final AtomicInteger failedCount = new AtomicInteger();

        private TestTaskAgent() {
            super(
                "TestTaskAgent",
                "1.0.0",
                AgentConfiguration.builder()
                    .instructions("Process tasks and return results")
                    .build(),
                List.of("process", "summarize")
            );
        }

        @Override
        protected String doProcess(SimpleTask task) {
            if (task.shouldFail()) {
                throw new IllegalStateException("Intentional task failure");
            }

            // Simulate input/output size tracking
            long inputSize = task.getSize() == TaskSize.SMALL ? 100 : 1000;
            long outputSize = task.getSize() == TaskSize.SMALL ? 100 : 200;

            // Record task-specific metrics
            ((TaskAgentMetrics) getMetrics()).recordTaskSucceeded(
                System.nanoTime() - System.nanoTime() + 10000000, // fake 10ms
                inputSize,
                outputSize,
                task.getPriority()
            );

            return "Processed: " + task.getDescription();
        }

        @Override
        public void onTaskStarted(SimpleTask task) {
            startedCount.incrementAndGet();
        }

        @Override
        public void onTaskCompleted(SimpleTask task, String result) {
            completedCount.incrementAndGet();
        }

        @Override
        public void onTaskFailed(SimpleTask task, Throwable exception) {
            failedCount.incrementAndGet();

            // Record task failure
            ((TaskAgentMetrics) getMetrics()).recordTaskFailed(
                System.nanoTime() - System.nanoTime() + 5000000, // fake 5ms
                task.getSize() == TaskSize.SMALL ? 100 : 1000,
                task.getPriority()
            );
        }

        int getStartedCount() {
            return startedCount.get();
        }

        int getCompletedCount() {
            return completedCount.get();
        }

        int getFailedCount() {
            return failedCount.get();
        }

        @Override
        public void close() {
            AgentState currentState = getState();
            if (currentState == AgentState.STARTED || currentState == AgentState.PAUSED) {
                try {
                    stop();
                } catch (AgentException ignored) {
                    // ignored for tests
                }
            }
        }
    }

    private static final class SimpleTask implements Task {
        private final String id;
        private final Instant createdAt;
        private final String description;
        private final TaskPriority priority;
        private final TaskStatus status;
        private final TaskSize size;
        private final String completionCriteria;
        private final double progress;
        private final Map<String, Object> metadata;
        private final Task parent;
        private final List<Task> subTasks;
        private final List<ai.demo.agent.base.task.TaskAttempt> attempts;
        private final int maxAttempts;
        private final ai.demo.agent.base.task.TaskAttempt currentAttempt;
        private final boolean shouldFail;

        private SimpleTask(String id, TaskPriority priority, TaskSize size, boolean shouldFail) {
            this.id = id;
            this.createdAt = Instant.now();
            this.description = "Task-" + id;
            this.priority = priority;
            this.status = TaskStatus.CREATED;
            this.size = size;
            this.completionCriteria = "Complete the task";
            this.progress = 0.0;
            this.metadata = Map.of("id", id);
            this.parent = null;
            this.subTasks = List.of();
            this.attempts = List.of();
            this.maxAttempts = 3;
            this.currentAttempt = null;
            this.shouldFail = shouldFail;
        }

        static SimpleTask success(String id) {
            return new SimpleTask(id, TaskPriority.NORMAL, TaskSize.SMALL, false);
        }

        static SimpleTask success(String id, TaskPriority priority) {
            return new SimpleTask(id, priority, TaskSize.SMALL, false);
        }

        static SimpleTask success(String id, TaskPriority priority, TaskSize size) {
            return new SimpleTask(id, priority, size, false);
        }

        static SimpleTask failure(String id) {
            return new SimpleTask(id, TaskPriority.NORMAL, TaskSize.SMALL, true);
        }

        boolean shouldFail() {
            return shouldFail;
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
            return status;
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
            return progress;
        }

        @Override
        public Map<String, Object> getMetadata() {
            return metadata;
        }

        @Override
        public Task getParent() {
            return parent;
        }

        @Override
        public List<Task> getSubTasks() {
            return subTasks;
        }

        @Override
        public List<ai.demo.agent.base.task.TaskAttempt> getAttempts() {
            return attempts;
        }

        @Override
        public int getMaxAttempts() {
            return maxAttempts;
        }

        @Override
        public ai.demo.agent.base.task.TaskAttempt getCurrentAttempt() {
            return currentAttempt;
        }

        @Override
        public String toString() {
            return description;
        }
    }
}