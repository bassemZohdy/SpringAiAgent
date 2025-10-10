package ai.demo.agent.base;

import ai.demo.agent.base.task.Task;
import ai.demo.agent.base.task.TaskAttempt;
import ai.demo.agent.base.task.TaskPriority;
import ai.demo.agent.base.task.TaskSize;
import ai.demo.agent.base.task.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BaseAgentTest {

    @Test
    void testLifecycleTransitions() throws AgentException {
        try (TestAgent agent = new TestAgent()) {
            assertEquals(AgentState.CREATED, agent.getState());
            assertFalse(agent.isRunning());

            agent.start();
            assertEquals(AgentState.STARTED, agent.getState());
            assertTrue(agent.isRunning());

            agent.pause();
            assertEquals(AgentState.PAUSED, agent.getState());
            assertTrue(agent.isPaused());

            agent.start();
            assertEquals(AgentState.STARTED, agent.getState());

            agent.stop();
            assertEquals(AgentState.STOPPED, agent.getState());

            agent.reset();
            assertEquals(AgentState.CREATED, agent.getState());
            assertEquals(0, agent.getMetrics().getTasksProcessed());
            assertTrue(agent.getMemory().isEmpty());
        }
    }

    @Test
    void testStartTwiceThrowsException() throws AgentException {
        try (TestAgent agent = new TestAgent()) {
            agent.start();
            AgentException exception = assertThrows(AgentException.class, agent::start);
            assertEquals(AgentState.STARTED, exception.getCurrentState());
        }
    }

    @Test
    void testProcessSuccessfulTaskRecordsMetricsAndEvents() throws AgentException {
        try (TestAgent agent = new TestAgent()) {
            agent.start();
            SimpleTask task = SimpleTask.success("1");

            CompletableFuture<String> future = agent.process(task);
            String result = future.join();

            assertEquals("Processed: " + task.getDescription(), result);
            assertEquals(1, agent.getMetrics().getTasksProcessed());
            assertEquals(1, agent.getMetrics().getTasksSucceeded());
            assertEquals(0, agent.getMetrics().getTasksFailed());
            assertEquals(1, agent.getStartedCount());
            assertEquals(1, agent.getCompletedCount());
            assertEquals(0, agent.getFailedCount());

            AgentMemory.MemoryEntry entry = agent.getMemory().getEntries().get(0);
            assertTrue(entry.isSuccess());
            assertEquals(task.toString(), entry.getTaskDescription());
            assertEquals("Processed: " + task.getDescription(), entry.getResultDescription());
        }
    }

    @Test
    void testProcessFailureRecordsMetricsAndEvents() throws AgentException {
        try (TestAgent agent = new TestAgent()) {
            agent.start();
            SimpleTask task = SimpleTask.failure("fail");

            CompletionException exception = assertThrows(CompletionException.class, () -> agent.process(task).join());
            assertTrue(exception.getCause() instanceof RuntimeException);

            assertEquals(1, agent.getMetrics().getTasksProcessed());
            assertEquals(0, agent.getMetrics().getTasksSucceeded());
            assertEquals(1, agent.getMetrics().getTasksFailed());
            assertEquals(1, agent.getFailedCount());

            AgentMemory.MemoryEntry entry = agent.getMemory().getEntries().get(0);
            assertFalse(entry.isSuccess());
            assertTrue(entry.getLearnings().contains("IllegalStateException"));
        }
    }

    @Test
    void testProcessWhenNotRunningFailsImmediately() {
        try (TestAgent agent = new TestAgent()) {
            SimpleTask task = SimpleTask.success("idle");
            CompletableFuture<String> future = agent.process(task);
            CompletionException exception = assertThrows(CompletionException.class, future::join);
            assertTrue(exception.getCause() instanceof AgentException);
            AgentException agentException = (AgentException) exception.getCause();
            assertEquals(AgentState.CREATED, agentException.getCurrentState());
        }
    }

    @Test
    void testCompactMemoryGeneratesSummary() {
        try (TestAgent agent = new TestAgent()) {
            for (int i = 0; i < 12; i++) {
                SimpleTask task = SimpleTask.success("task-" + i);
                agent.getMemory().recordExecution(task, "result" + i, true, i, "learning " + i);
            }

            assertNull(agent.getMemory().getSummary());
            agent.compactMemory();
            String summary = agent.getMemory().getSummary();
            assertNotNull(summary);
            assertTrue(summary.contains("Agent Memory Summary"));
            assertTrue(summary.contains("TestAgent"));
        }
    }

    @Test
    void testRecordLearningStoresEntry() {
        try (TestAgent agent = new TestAgent()) {
            SimpleTask task = SimpleTask.success("memory");
            agent.recordCustomLearning(task, "Remember this");

            AgentMemory.MemoryEntry entry = agent.getMemory().getEntries().get(0);
            assertTrue(entry.isSuccess());
            assertEquals("Remember this", entry.getLearnings());
        }
    }

    @Test
    void testAgentIdentityAndConfiguration() throws AgentException {
        try (TestAgent agent = new TestAgent()) {
            agent.start();
            assertNotNull(agent.getAgentId());
            assertEquals("TestAgent", agent.getAgentName());
            assertEquals("1.0.0", agent.getVersion());
            assertEquals("Follow the test instructions", agent.getInstructions());
            List<String> capabilities = agent.getCapabilities();
            assertEquals(List.of("echo", "summarize"), capabilities);
            assertThrows(UnsupportedOperationException.class, () -> capabilities.add("other"));
            assertThrows(UnsupportedOperationException.class, () -> agent.setInstructions("New"));
        }
    }

    private static final class TestAgent extends BaseAgent<SimpleTask, String> implements AutoCloseable {
        private final AtomicInteger startedCount = new AtomicInteger();
        private final AtomicInteger completedCount = new AtomicInteger();
        private final AtomicInteger failedCount = new AtomicInteger();

        private TestAgent() {
            super(
                "TestAgent",
                "1.0.0",
                AgentConfiguration.builder()
                    .instructions("Follow the test instructions")
                    .build(),
                List.of("echo", "summarize")
            );
        }

        @Override
        protected String doProcess(SimpleTask task) {
            if (task.shouldFail()) {
                throw new IllegalStateException("Intentional failure");
            }
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
        }

        void recordCustomLearning(SimpleTask task, String learning) {
            recordLearning(task, "Processed: " + task.getDescription(), learning);
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
        private final List<TaskAttempt> attempts;
        private final int maxAttempts;
        private final TaskAttempt currentAttempt;
        private final boolean shouldFail;

        private SimpleTask(String id, boolean shouldFail) {
            this.id = id;
            this.createdAt = Instant.now();
            this.description = "Task-" + id;
            this.priority = TaskPriority.NORMAL;
            this.status = TaskStatus.CREATED;
            this.size = TaskSize.SMALL;
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
            return new SimpleTask(id, false);
        }

        static SimpleTask failure(String id) {
            return new SimpleTask(id, true);
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
        public List<TaskAttempt> getAttempts() {
            return attempts;
        }

        @Override
        public int getMaxAttempts() {
            return maxAttempts;
        }

        @Override
        public TaskAttempt getCurrentAttempt() {
            return currentAttempt;
        }

        @Override
        public String toString() {
            return description;
        }
    }
}

