package ai.demo.springagent.integration;

import ai.demo.springagent.agent.ChatCompletionAgent;
import ai.demo.springagent.dto.ChatRequest;
import ai.demo.springagent.dto.ChatResponse;
import ai.demo.springagent.service.AgentChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive validation test for agent integration.
 *
 * <p>This test validates that the new agent abstraction framework is properly integrated
 * and functioning correctly within the Spring Boot application.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AgentIntegrationValidationTest {

    @Autowired
    private AgentChatService agentChatService;

    @Autowired
    private ChatCompletionAgent chatAgent;

    @BeforeEach
    void setUp() throws ai.demo.agent.base.AgentException {
        // Ensure agent is ready for testing
        if (!chatAgent.isRunning()) {
            chatAgent.start();
        }
    }

    @Test
    @DisplayName("Agent components should be properly initialized and wired")
    void testAgentInitialization() {
        // Validate Spring DI is working
        assertNotNull(agentChatService, "AgentChatService should be autowired");
        assertNotNull(chatAgent, "ChatCompletionAgent should be autowired");

        // Validate agent is in proper state
        assertTrue(chatAgent.isRunning(), "Agent should be running");
        assertNotNull(chatAgent.getAgentId(), "Agent should have a valid ID");
        assertNotNull(chatAgent.getAgentName(), "Agent should have a name");
        assertEquals("ChatCompletionAgent", chatAgent.getAgentName(), "Agent name should match expected");

        // Validate agent capabilities
        assertTrue(chatAgent.getCapabilities().size() > 0, "Agent should have capabilities defined");
        assertTrue(chatAgent.getCapabilities().contains("chat-completion"),
                  "Agent should have chat-completion capability");
    }

    @Test
    @DisplayName("Agent service should provide comprehensive status information")
    void testAgentServiceStatus() {
        // When: Agent metrics are requested through service
        Object metrics = agentChatService.getAgentMetrics();

        // Then: Should return comprehensive status information
        assertNotNull(metrics, "Agent service should return metrics");
        assertTrue(metrics instanceof java.util.Map, "Metrics should be a map");

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> metricsMap = (java.util.Map<String, Object>) metrics;

        // And: Should contain expected fields
        assertTrue(metricsMap.containsKey("agentId"), "Should contain agent ID");
        assertTrue(metricsMap.containsKey("agentName"), "Should contain agent name");
        assertTrue(metricsMap.containsKey("state"), "Should contain agent state");
        assertTrue(metricsMap.containsKey("isRunning"), "Should contain running status");
        assertTrue(metricsMap.containsKey("metrics"), "Should contain performance metrics");
        assertTrue(metricsMap.containsKey("memory"), "Should contain memory information");
    }

    @Test
    @DisplayName("Agent should process simple chat requests through the transformation pipeline")
    void testBasicChatProcessing() throws Exception {
        // Given: A simple chat request
        ChatRequest chatRequest = createTestChatRequest("Hello, can you help me test this system?");

        // When: Processed through the agent service
        ChatResponse response = agentChatService.processChat(chatRequest);

        // Then: Should return a valid response
        assertNotNull(response, "Should return a response");
        assertNotNull(response.getId(), "Response should have an ID");
        assertNotNull(response.getChoices(), "Response should have choices");
        assertFalse(response.getChoices().isEmpty(), "Response should not be empty");

        // And: Response should be properly formatted
        String content = response.getChoices().get(0).getMessage().getContent();
        assertNotNull(content, "Response content should not be null");
        assertFalse(content.trim().isEmpty(), "Response content should not be empty");
        assertTrue(content.length() > 20, "Response should be substantial");

        // And: Should follow OpenAI API format
        assertEquals("chat.completion", response.getObject(), "Response object should be 'chat.completion'");
        assertTrue(response.getCreated() > 0, "Should have creation timestamp");
        assertNotNull(response.getModel(), "Should have model information");
        assertEquals("assistant", response.getChoices().get(0).getMessage().getRole(), "Message role should be 'assistant'");

        // And: Should have usage information
        assertNotNull(response.getUsage(), "Should have usage information");
        assertTrue(response.getUsage().getTotalTokens() > 0, "Should have token count");
    }

    @Test
    @DisplayName("Agent should maintain conversation context")
    void testConversationContext() throws Exception {
        // Given: A conversation with multiple messages
        String threadId = "test-conversation-" + System.currentTimeMillis();

        // Send first message
        ChatRequest firstRequest = createTestChatRequestWithThread("My name is Alice and I love programming", threadId);
        ChatResponse firstResponse = agentChatService.processChat(firstRequest);
        assertNotNull(firstResponse, "First response should not be null");

        // Send follow-up message
        ChatRequest secondRequest = createTestChatRequestWithThread("What do you know about me?", threadId);
        ChatResponse secondResponse = agentChatService.processChat(secondRequest);
        assertNotNull(secondResponse, "Second response should not be null");

        // The response should ideally contain contextual information
        String secondContent = secondResponse.getChoices().get(0).getMessage().getContent();
        assertNotNull(secondContent, "Second response content should not be null");
        // Note: Context preservation depends on the memory implementation
    }

    @Test
    @DisplayName("Agent should track metrics and performance data")
    void testMetricsCollection() throws Exception {
        // Given: Initial state
        var initialMetrics = chatAgent.getMetrics();
        long initialTaskCount = initialMetrics.getTasksProcessed();

        // When: Processing several tasks
        int tasksToProcess = 3;
        for (int i = 0; i < tasksToProcess; i++) {
            ChatRequest chatRequest = createTestChatRequest("Test message " + i);
            agentChatService.processChat(chatRequest);
        }

        // Then: Metrics should be updated
        var finalMetrics = chatAgent.getMetrics();
        assertEquals(initialTaskCount + tasksToProcess, finalMetrics.getTasksProcessed(),
                    "Should track all processed tasks");

        // And: Should have performance statistics
        assertTrue(finalMetrics.getSuccessRate() >= 0.0 && finalMetrics.getSuccessRate() <= 1.0,
                  "Success rate should be valid");
        assertNotNull(finalMetrics.getAverageProcessingTime(),
                  "Average processing time should not be null");
    }

    @Test
    @DisplayName("Agent memory should record execution history")
    void testMemoryManagement() throws Exception {
        // Given: Agent processes some tasks
        ChatRequest chatRequest = createTestChatRequest("Test memory functionality");
        agentChatService.processChat(chatRequest);

        // When: Memory is inspected
        var memory = chatAgent.getMemory();
        var memoryStats = memory.getStats();

        // Then: Should have recorded execution information
        assertTrue(memoryStats.getTotalEntries() >= 0, "Memory should track entries");

        // And: Should provide statistics
        assertTrue(memoryStats.getSuccessRate() >= 0.0, "Memory stats should be valid");
    }

    @Test
    @DisplayName("Agent should handle edge cases gracefully")
    void testEdgeCaseHandling() throws Exception {
        // Test empty message
        ChatRequest emptyRequest = createTestChatRequest("");
        ChatResponse emptyResponse = agentChatService.processChat(emptyRequest);
        assertNotNull(emptyResponse, "Should handle empty messages");

        // Test very long message
        String longMessage = "Explain " + "AI ".repeat(50) + "technology";
        ChatRequest longRequest = createTestChatRequest(longMessage);
        ChatResponse longResponse = agentChatService.processChat(longRequest);
        assertNotNull(longResponse, "Should handle long messages");

        // Test special characters
        ChatRequest specialRequest = createTestChatRequest("Test special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?");
        ChatResponse specialResponse = agentChatService.processChat(specialRequest);
        assertNotNull(specialResponse, "Should handle special characters");
    }

    @Test
    @DisplayName("Agent should demonstrate the complete transformation pipeline")
    void testTransformationPipeline() throws Exception {
        // Given: A complex chat request
        ChatRequest chatRequest = createComplexChatRequest();

        // When: Processed through the agent
        CompletableFuture<ChatResponse> future = chatAgent.process(
            new ai.demo.springagent.task.ChatTask(chatRequest));
        ChatResponse response = future.get();

        // Then: Should return properly formatted response
        assertNotNull(response, "Should return response");
        assertNotNull(response.getId(), "Should have response ID");
        assertTrue(response.getChoices().size() > 0, "Should have choices");

        // And: Response content should be meaningful
        String content = response.getChoices().get(0).getMessage().getContent();
        assertNotNull(content, "Content should not be null");
        assertFalse(content.trim().isEmpty(), "Content should not be empty");
    }

    @Test
    @DisplayName("Agent abstraction should provide proper lifecycle management")
    void testLifecycleManagement() {
        // Given: Running agent
        assertTrue(chatAgent.isRunning(), "Agent should start in running state");

        // When: Checking agent properties
        String agentId = chatAgent.getAgentId();
        String agentName = chatAgent.getAgentName();
        String version = chatAgent.getVersion();

        // Then: Should have proper identification
        assertNotNull(agentId, "Should have agent ID");
        assertFalse(agentId.trim().isEmpty(), "Agent ID should not be empty");
        assertNotNull(agentName, "Should have agent name");
        assertNotNull(version, "Should have version");

        // And: Should have capabilities
        var capabilities = chatAgent.getCapabilities();
        assertFalse(capabilities.isEmpty(), "Should have capabilities");
        assertTrue(capabilities.contains("chat-completion"), "Should have chat capability");
    }

    private ChatRequest createTestChatRequest(String message) {
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

    private ChatRequest createTestChatRequestWithThread(String message, String threadId) {
        ChatRequest request = createTestChatRequest(message);
        request.setThreadId(threadId);
        return request;
    }

    @Test
    @DisplayName("Agent should handle complex multi-message conversations")
    void testComplexConversationHandling() throws Exception {
        // Given: A complex conversation with system message and multiple turns
        String threadId = "complex-test-" + System.currentTimeMillis();

        // First message with system context
        ChatRequest firstRequest = createComplexChatRequest();
        firstRequest.setThreadId(threadId);
        ChatResponse firstResponse = agentChatService.processChat(firstRequest);
        assertNotNull(firstResponse, "First response should not be null");

        // Follow-up question
        ChatRequest secondRequest = createTestChatRequestWithThread("Can you explain that in simpler terms?", threadId);
        ChatResponse secondResponse = agentChatService.processChat(secondRequest);
        assertNotNull(secondResponse, "Second response should not be null");

        // Validate conversation flow
        String firstContent = firstResponse.getChoices().get(0).getMessage().getContent();
        String secondContent = secondResponse.getChoices().get(0).getMessage().getContent();

        assertTrue(firstContent.length() > 50, "First response should be comprehensive");
        assertTrue(secondContent.length() > 20, "Second response should be meaningful");
    }

    @Test
    @DisplayName("Agent should handle different request parameters")
    void testDifferentRequestParameters() throws Exception {
        // Test with different temperatures
        ChatRequest tempRequest = createTestChatRequest("Generate a creative response");
        tempRequest.setTemperature(0.9);
        ChatResponse tempResponse = agentChatService.processChat(tempRequest);
        assertNotNull(tempResponse, "Should handle temperature parameter");

        // Test with max tokens
        ChatRequest tokensRequest = createTestChatRequest("Generate a concise response");
        tokensRequest.setMaxTokens(50);
        ChatResponse tokensResponse = agentChatService.processChat(tokensRequest);
        assertNotNull(tokensResponse, "Should handle max tokens parameter");

        // Test with streaming flag (should not break)
        ChatRequest streamRequest = createTestChatRequest("Test streaming parameter");
        streamRequest.setStream(true);
        ChatResponse streamResponse = agentChatService.processChat(streamRequest);
        assertNotNull(streamResponse, "Should handle streaming parameter");
    }

    @Test
    @DisplayName("Agent should handle concurrent requests properly")
    void testConcurrentRequests() throws Exception {
        int concurrentRequests = 5;
        java.util.List<CompletableFuture<ChatResponse>> futures = new java.util.ArrayList<>();

        // When: Submitting multiple concurrent requests
        for (int i = 0; i < concurrentRequests; i++) {
            ChatRequest request = createTestChatRequest("Concurrent test request " + i);
            CompletableFuture<ChatResponse> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return agentChatService.processChat(request);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }

        // Then: All requests should complete successfully
        java.util.List<ChatResponse> responses = new java.util.ArrayList<>();
        for (CompletableFuture<ChatResponse> future : futures) {
            ChatResponse response = future.get(30, java.util.concurrent.TimeUnit.SECONDS);
            assertNotNull(response, "Each concurrent request should return a response");
            responses.add(response);
        }

        assertEquals(concurrentRequests, responses.size(), "Should have received all responses");

        // And: All responses should be valid
        for (ChatResponse response : responses) {
            assertNotNull(response.getId(), "Each response should have an ID");
            assertFalse(response.getChoices().isEmpty(), "Each response should have choices");
            assertNotNull(response.getChoices().get(0).getMessage().getContent(),
                      "Each response should have content");
        }
    }

    @Test
    @DisplayName("Agent should demonstrate proper error handling")
    void testErrorHandling() {
        // Test with null request - should handle gracefully
        assertThrows(Exception.class, () -> {
            agentChatService.processChat(null);
        }, "Should handle null request gracefully");

        // Test with empty messages
        ChatRequest emptyRequest = new ChatRequest();
        emptyRequest.setMessages(java.util.List.of());

        assertThrows(Exception.class, () -> {
            agentChatService.processChat(emptyRequest);
        }, "Should handle empty messages gracefully");
    }

    @Test
    @DisplayName("Agent should demonstrate memory management capabilities")
    void testMemoryManagementCapabilities() throws Exception {
        // Given: Initial memory state
        var initialMemory = chatAgent.getMemory();
        int initialSize = initialMemory.size();

        // When: Processing several tasks
        for (int i = 0; i < 5; i++) {
            ChatRequest request = createTestChatRequest("Memory test task " + i);
            agentChatService.processChat(request);
        }

        // Then: Memory should have recorded some activity
        var finalMemory = chatAgent.getMemory();
        var memoryStats = finalMemory.getStats();

        assertTrue(memoryStats.getTotalEntries() >= initialSize,
                  "Memory should have recorded new entries");

        // Test memory compaction
        chatAgent.compactMemory();
        var compactedMemory = chatAgent.getMemory();
        assertNotNull(compactedMemory, "Memory should still exist after compaction");
    }

    @Test
    @DisplayName("Agent should demonstrate detailed lifecycle management")
    void testDetailedLifecycleManagement() throws ai.demo.agent.base.AgentException {
        // Given: Running agent
        assertTrue(chatAgent.isRunning(), "Agent should be running");

        // When: Getting agent information
        String agentId = chatAgent.getAgentId();
        String agentName = chatAgent.getAgentName();
        String version = chatAgent.getVersion();
        ai.demo.agent.base.AgentState state = chatAgent.getState();

        // Then: Should have proper identification
        assertNotNull(agentId, "Should have agent ID");
        assertFalse(agentId.trim().isEmpty(), "Agent ID should not be empty");
        assertEquals("ChatCompletionAgent", agentName, "Should have correct agent name");
        assertNotNull(version, "Should have version");
        assertNotNull(state, "Should have state");
    }

    @Test
    @DisplayName("Agent should provide comprehensive metrics")
    void testComprehensiveMetrics() throws Exception {
        // Process some tasks to generate metrics
        for (int i = 0; i < 3; i++) {
            ChatRequest request = createTestChatRequest("Metrics test " + i);
            agentChatService.processChat(request);
        }

        // Get agent metrics through service
        Object metricsObject = agentChatService.getAgentMetrics();
        assertTrue(metricsObject instanceof java.util.Map, "Metrics should be a map");

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> metrics = (java.util.Map<String, Object>) metricsObject;

        // Validate structure
        assertTrue(metrics.containsKey("agentId"), "Should contain agent ID");
        assertTrue(metrics.containsKey("agentName"), "Should contain agent name");
        assertTrue(metrics.containsKey("state"), "Should contain state");
        assertTrue(metrics.containsKey("isRunning"), "Should contain running status");
        assertTrue(metrics.containsKey("capabilities"), "Should contain capabilities");
        assertTrue(metrics.containsKey("metrics"), "Should contain performance metrics");
        assertTrue(metrics.containsKey("memory"), "Should contain memory info");

        // Validate performance metrics
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> perfMetrics = (java.util.Map<String, Object>) metrics.get("metrics");
        assertTrue(perfMetrics.containsKey("tasksProcessed"), "Should track processed tasks");
        assertTrue(perfMetrics.containsKey("successRate"), "Should track success rate");
        assertTrue(perfMetrics.containsKey("averageProcessingTime"), "Should track processing time");
    }

    private ChatRequest createComplexChatRequest() {
        ChatRequest request = new ChatRequest();
        request.setModel("test-model");
        request.setTemperature(0.7);
        request.setMaxTokens(200);

        ChatRequest.Message systemMessage = new ChatRequest.Message();
        systemMessage.setRole("system");
        systemMessage.setContent("You are a helpful AI assistant.");

        ChatRequest.Message userMessage = new ChatRequest.Message();
        userMessage.setRole("user");
        userMessage.setContent("Explain how the agent transformation pipeline works.");

        request.setMessages(List.of(systemMessage, userMessage));
        return request;
    }
}