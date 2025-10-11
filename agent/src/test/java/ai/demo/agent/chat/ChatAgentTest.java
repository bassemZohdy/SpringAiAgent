package ai.demo.agent.chat;

import ai.demo.agent.base.Agent;
import ai.demo.agent.base.AgentConfiguration;
import ai.demo.agent.base.AgentState;
import ai.demo.agent.base.AgentException;
import ai.demo.agent.base.BaseAgent;
import ai.demo.agent.base.task.Task;
import ai.demo.agent.base.task.TaskPriority;
import ai.demo.agent.base.task.TaskStatus;
import ai.demo.agent.base.task.TaskSize;
import ai.demo.agent.metrics.ChatAgentMetrics;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ChatAgentTest {

    @Test
    void testChatAgentInterface() {
        TestChatAgent agent = new TestChatAgent();

        // Test that it implements both Agent and ChatAgent
        assertTrue(agent instanceof Agent);
        assertTrue(agent instanceof ChatAgent);
        assertTrue(agent instanceof BaseAgent);

        // Test that it returns ChatAgentMetrics
        assertTrue(agent.getMetrics() instanceof ChatAgentMetrics);
    }

    @Test
    void testChatOperations() throws AgentException {
        TestChatAgent agent = new TestChatAgent();
        agent.start();

        String request = "Hello";
        CompletableFuture<String> future = agent.chat(request);
        String response = future.join();

        assertEquals("Chat response: " + request, response);
        assertEquals(1, agent.getStartedCount());
        assertEquals(1, agent.getCompletedCount());

        // Test metrics
        ChatAgentMetrics metrics = (ChatAgentMetrics) agent.getMetrics();
        assertEquals(1, metrics.getConversationsStarted());
        assertEquals(1, metrics.getConversationsCompleted());
        assertEquals(1, metrics.getTotalMessages());
        assertEquals(1, metrics.getTotalUserMessages());
        assertEquals(1, metrics.getTotalAgentMessages());
    }

    @Test
    void testConversationLifecycle() throws AgentException {
        TestChatAgent agent = new TestChatAgent();
        agent.start();

        // Test conversation lifecycle events
        agent.chat("First message");
        agent.chat("Second message");

        assertEquals(2, agent.getStartedCount());
        assertEquals(2, agent.getCompletedCount());

        ChatAgentMetrics metrics = (ChatAgentMetrics) agent.getMetrics();
        assertEquals(1, metrics.getConversationsStarted());
        assertEquals(1, metrics.getConversationsCompleted());
        assertEquals(2, metrics.getTotalMessages());
        assertEquals(2, metrics.getTotalUserMessages());
        assertEquals(2, metrics.getTotalAgentMessages());
    }

    @Test
    void testChatWhenNotRunningFails() {
        TestChatAgent agent = new TestChatAgent();

        CompletableFuture<String> future = agent.chat("test");
        assertThrows(AgentException.class, future::join);
    }

    @Test
    void testChatFailureHandling() throws AgentException {
        TestChatAgent agent = new TestChatAgent(true); // This agent will fail
        agent.start();

        CompletableFuture<String> future = agent.chat("error message");
        assertThrows(RuntimeException.class, future::join);

        assertEquals(1, agent.getFailedCount());
        ChatAgentMetrics metrics = (ChatAgentMetrics) agent.getMetrics();
        assertEquals(1, metrics.getConversationsStarted());
        assertEquals(0, metrics.getConversationsCompleted());
        assertEquals(1, agent.getConversationsAbandoned());
    }

    private static final class TestChatAgent extends BaseAgent<ChatRequest, String> implements ChatAgent<ChatRequest, String> {
        private final AtomicInteger startedCount = new AtomicInteger();
        private final AtomicInteger completedCount = new AtomicInteger();
        private final AtomicInteger failedCount = new AtomicInteger();
        private final boolean shouldFail;

        private TestChatAgent() {
            this(false);
        }

        private TestChatAgent(boolean shouldFail) {
            super(
                "TestChatAgent",
                "1.0.0",
                AgentConfiguration.builder()
                    .instructions("Respond to chat messages")
                    .build(),
                List.of("chat", "echo")
            );
            this.shouldFail = shouldFail;
        }

        @Override
        protected String doProcess(ChatRequest request) {
            if (shouldFail) {
                throw new RuntimeException("Intentional chat failure");
            }
            return "Chat response: " + request.message();
        }

        @Override
        public CompletableFuture<String> chat(ChatRequest request) {
            if (!isRunning()) {
                return CompletableFuture.failedFuture(
                    new AgentException("Agent is not running", getAgentId(), getState()));
            }

            return CompletableFuture.supplyAsync(() -> {
                long startTime = System.nanoTime();
                getMetrics().recordOperationStarted();
                onConversationStarted(request);

                try {
                    String result = doProcess(request);
                    long processingTime = System.nanoTime() - startTime;

                    // Record as a single message operation for simplicity
                    ((ChatAgentMetrics) getMetrics()).recordMessageProcessed(
                        processingTime,
                        estimateInputTokens(request),
                        estimateOutputTokens(result),
                        true // User message
                    );

                    onConversationCompleted(request, result);
                    return result;

                } catch (Exception e) {
                    long processingTime = System.nanoTime() - startTime;

                    ((ChatAgentMetrics) getMetrics()).recordMessageProcessingFailed(
                        processingTime,
                        estimateInputTokens(request),
                        true // User message
                    );

                    onConversationFailed(request, e);
                    throw new RuntimeException("Chat processing failed", e);
                }
            }, getExecutor());
        }

        @Override
        public void onConversationStarted(ChatRequest request) {
            startedCount.incrementAndGet();
            ((ChatAgentMetrics) getMetrics()).recordConversationStarted();
        }

        @Override
        public void onConversationCompleted(ChatRequest request, String response) {
            completedCount.incrementAndGet();
            // For simplicity, record a single message exchange
            ((ChatAgentMetrics) getMetrics()).recordConversationCompleted(1, 1, 1);
        }

        @Override
        public void onConversationFailed(ChatRequest request, Throwable error) {
            failedCount.incrementAndGet();
            ((ChatAgentMetrics) getMetrics()).recordConversationAbandoned();
        }

        private long estimateInputTokens(ChatRequest request) {
            return request.message().length(); // Simple estimation
        }

        private long estimateOutputTokens(String response) {
            return response.length(); // Simple estimation
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
    }

    private static final class ChatRequest {
        private final String message;

        public ChatRequest(String message) {
            this.message = message;
        }

        public String message() {
            return message;
        }
    }
}