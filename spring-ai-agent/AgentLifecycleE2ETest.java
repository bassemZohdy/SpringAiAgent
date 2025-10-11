package ai.demo.springagent.integration;

import ai.demo.springagent.agent.ChatCompletionAgent;
import ai.demo.springagent.dto.ChatRequest;
import ai.demo.springagent.dto.ChatResponse;
import ai.demo.springagent.service.AgentChatService;
import ai.demo.springagent.task.ChatTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for agent lifecycle management.
 *
 * <p>These tests validate the complete agent lifecycle including startup,
 * task processing, state transitions, memory management, and shutdown.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AgentLifecycleE2ETest {

    @Autowired
    private AgentChatService agentChatService;

    @Autowired
    private ChatCompletionAgent chatAgent;

    @BeforeEach
    void setUp() {
        // Ensure agent is in a clean state before each test
        if (chatAgent.isRunning()) {
            chatAgent.reset();
        }
    }

    @Test
    @DisplayName("Agent should start successfully and be ready for tasks")
    void testAgentStartup() {
        // Given: Agent is configured

        // When: Agent is started
        assertTrue(chatAgent.isRunning(), "Agent should be running after startup");

        // Then: Agent should be in proper state
        assertNotNull(chatAgent.getAgentId(), "Agent should have a valid ID");
        assertNotNull(chatAgent.getAgentName(), "Agent should have a name");
        assertNotNull(chatAgent.getVersion(), "Agent should have a version");

        // And: Should have working memory and metrics
        assertNotNull(chatAgent.getMemory(), "Agent should have memory initialized");
        assertNotNull(chatAgent.getMetrics(), "Agent should have metrics initialized");
    }

    @Test
    @DisplayName("Agent should process chat tasks through complete pipeline")
    void testAgentTaskProcessing() throws Exception {
        // Given: A simple chat request
        ChatRequest chatRequest = createSimpleChatRequest("Hello, agent!");
        ChatTask chatTask = new ChatTask(chatRequest);

        // When: Agent processes the task
        CompletableFuture<ChatResponse> future = chatAgent.process(chatTask);
        ChatResponse response = future.get();

        // Then: Response should be properly formatted
        assertNotNull(response, "Agent should return a response");
        assertNotNull(response.getId(), "Response should have an ID");
        assertNotNull(response.getChoices(), "Response should have choices");
        assertFalse(response.getChoices().isEmpty(), "Response should have at least one choice");

        // And: Content should be meaningful
        String content = response.getChoices().get(0).getMessage().getContent();
        assertNotNull(content, "Response content should not be null");
        assertFalse(content.trim().isEmpty(), "Response content should not be empty");

        // And: Task metrics should be updated
        assertTrue(chatAgent.getMetrics().getTasksProcessed() > 0,
                  "Agent should have processed at least one task");
    }

    @Test
    @DisplayName("Agent should manage state transitions correctly")
    void testAgentStateTransitions() throws Exception {
        // Given: Agent is initially running
        assertTrue(chatAgent.isRunning(), "Agent should start in running state");

        // When: Agent is paused
        chatAgent.pause();

        // Then: Agent should be paused
        assertFalse(chatAgent.isRunning(), "Agent should not be running when paused");

        // When: Agent is resumed
        chatAgent.start();

        // Then: Agent should be running again
        assertTrue(chatAgent.isRunning(), "Agent should be running after resume");

        // When: Agent is reset
        chatAgent.reset();

        // Then: Agent should be in initial state
        assertEquals("STARTED", chatAgent.getState(), "Agent should be in STARTED state after reset");
    }

    @Test
    @DisplayName("Agent should record execution history and learnings")
    void testAgentMemoryManagement() throws Exception {
        // Given: Agent processes several tasks
        List<String> testMessages = List.of(
            "What is your name?",
            "What can you do?",
            "Tell me a joke"
        );

        // When: Processing multiple tasks
        for (String message : testMessages) {
            ChatRequest chatRequest = createSimpleChatRequest(message);
            ChatTask chatTask = new ChatTask(chatRequest);
            chatAgent.process(chatTask).get();
        }

        // Then: Agent memory should contain execution history
        assertFalse(chatAgent.getMemory().isEmpty(), "Agent memory should not be empty after processing tasks");

        // And: Memory should contain learnings
        assertTrue(chatAgent.getMemory().getLearnings().size() > 0,
                  "Agent should have recorded learnings from task execution");

        // When: Memory is compacted
        int memorySizeBefore = chatAgent.getMemory().size();
        chatAgent.compactMemory();

        // Then: Memory should be optimized
        assertTrue(chatAgent.getMemory().size() <= memorySizeBefore,
                  "Memory size should be reduced or maintained after compaction");
    }

    @Test
    @DisplayName("Agent should provide accurate metrics")
    void testAgentMetricsCollection() throws Exception {
        // Given: Agent processes some tasks
        int tasksToProcess = 5;

        for (int i = 0; i < tasksToProcess; i++) {
            ChatRequest chatRequest = createSimpleChatRequest("Test message " + i);
            ChatTask chatTask = new ChatTask(chatRequest);
            chatAgent.process(chatTask).get();
        }

        // When: Metrics are retrieved
        var metrics = chatAgent.getMetrics();

        // Then: Metrics should be accurate
        assertEquals(tasksToProcess, metrics.getTasksProcessed(),
                    "Should track correct number of processed tasks");

        // And: Success rate should be calculated
        assertTrue(metrics.getSuccessRate() >= 0.0 && metrics.getSuccessRate() <= 1.0,
                  "Success rate should be between 0 and 1");

        // And: Average processing time should be calculated
        assertTrue(metrics.getAverageProcessingTime() >= 0.0,
                  "Average processing time should be non-negative");
    }

    @Test
    @DisplayName("Agent service should provide comprehensive status information")
    void testAgentServiceStatus() {
        // When: Agent metrics are requested through service
        Object metrics = agentChatService.getAgentMetrics();

        // Then: Should return comprehensive status information
        assertNotNull(metrics, "Agent service should return metrics");
        assertTrue(metrics instanceof Map, "Metrics should be a map");

        @SuppressWarnings("unchecked")
        Map<String, Object> metricsMap = (Map<String, Object>) metrics;

        // And: Should contain expected fields
        assertTrue(metricsMap.containsKey("agentId"), "Should contain agent ID");
        assertTrue(metricsMap.containsKey("agentName"), "Should contain agent name");
        assertTrue(metricsMap.containsKey("state"), "Should contain agent state");
        assertTrue(metricsMap.containsKey("isRunning"), "Should contain running status");
        assertTrue(metricsMap.containsKey("metrics"), "Should contain performance metrics");
        assertTrue(metricsMap.containsKey("memory"), "Should contain memory information");
    }

    @Test
    @DisplayName("Agent should handle concurrent task processing")
    void testAgentConcurrentProcessing() throws Exception {
        // Given: Multiple concurrent tasks
        int concurrentTasks = 10;
        List<CompletableFuture<ChatResponse>> futures = new java.util.ArrayList<>();

        // When: Tasks are submitted concurrently
        for (int i = 0; i < concurrentTasks; i++) {
            ChatRequest chatRequest = createSimpleChatRequest("Concurrent task " + i);
            ChatTask chatTask = new ChatTask(chatRequest);
            CompletableFuture<ChatResponse> future = chatAgent.process(chatTask);
            futures.add(future);
        }

        // Then: All tasks should complete successfully
        for (CompletableFuture<ChatResponse> future : futures) {
            ChatResponse response = future.get();
            assertNotNull(response, "Each task should return a valid response");
            assertNotNull(response.getChoices(), "Response should have choices");
            assertFalse(response.getChoices().isEmpty(), "Response should not be empty");
        }

        // And: Agent should have processed all tasks
        assertEquals(concurrentTasks, chatAgent.getMetrics().getTasksProcessed(),
                    "Should have processed all concurrent tasks");
    }

    private ChatRequest createSimpleChatRequest(String message) {
        ChatRequest request = new ChatRequest();
        request.setModel("test-model");
        request.setTemperature(0.7);
        request.setMaxTokens(100);

        ChatRequest.Message userMessage = new ChatRequest.Message();
        userMessage.setRole("user");
        userMessage.setContent(message);

        request.setMessages(List.of(userMessage));
        return request;
    }
}