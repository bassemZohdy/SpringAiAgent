package ai.demo.springagent.integration;

import ai.demo.springagent.agent.ChatCompletionAgent;
import ai.demo.springagent.service.AgentChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simplified validation test for agent framework integration.
 *
 * <p>This test validates that the agent abstraction framework is properly integrated
 * and configured correctly within the Spring Boot application without requiring external API calls.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AgentFrameworkValidationTest {

    @Autowired
    private AgentChatService agentChatService;

    @Autowired
    private ChatCompletionAgent chatAgent;

    @Test
    @DisplayName("Agent components should be properly initialized and wired")
    void testAgentInitialization() {
        // Validate Spring DI is working
        assertNotNull(agentChatService, "AgentChatService should be autowired");
        assertNotNull(chatAgent, "ChatCompletionAgent should be autowired");

        // Validate agent has proper identification
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
    @DisplayName("Agent should track metrics and performance data")
    void testMetricsCollection() {
        // Given: Initial state
        var initialMetrics = chatAgent.getMetrics();
        long initialTaskCount = initialMetrics.getTasksProcessed();

        // Metrics should be accessible even without processing tasks
        assertNotNull(initialMetrics, "Should have metrics available");
        assertTrue(initialTaskCount >= 0, "Task count should be non-negative");

        // And: Should have performance statistics
        assertTrue(initialMetrics.getSuccessRate() >= 0.0 && initialMetrics.getSuccessRate() <= 1.0,
                  "Success rate should be valid");
    }

    @Test
    @DisplayName("Agent memory should be functional")
    void testMemoryManagement() {
        // Given: Initial memory state
        var memory = chatAgent.getMemory();
        assertNotNull(memory, "Memory should be available");
        var memoryStats = memory.getStats();
        assertNotNull(memoryStats, "Memory stats should be available");

        // Then: Should provide statistics
        assertTrue(memoryStats.getTotalEntries() >= 0, "Memory should track entries");
        assertTrue(memoryStats.getSuccessRate() >= 0.0, "Memory stats should be valid");

        // Test memory compaction
        int initialSize = memory.size();
        chatAgent.compactMemory();
        var compactedMemory = chatAgent.getMemory();
        assertNotNull(compactedMemory, "Memory should still exist after compaction");
        assertEquals(initialSize, compactedMemory.size(), "Memory size should be consistent after compaction");
    }

    @Test
    @DisplayName("Agent should demonstrate proper lifecycle management")
    void testLifecycleManagement() {
        // Given: Running agent
        assertTrue(chatAgent.isRunning(), "Agent should start in running state");

        // When: Checking agent properties
        String agentId = chatAgent.getAgentId();
        String agentName = chatAgent.getAgentName();
        String version = chatAgent.getVersion();
        ai.demo.agent.base.AgentState state = chatAgent.getState();

        // Then: Should have proper identification
        assertNotNull(agentId, "Should have agent ID");
        assertFalse(agentId.trim().isEmpty(), "Agent ID should not be empty");
        assertNotNull(agentName, "Should have agent name");
        assertNotNull(version, "Should have version");
        assertNotNull(state, "Should have state");
    }

    @Test
    @DisplayName("Agent should provide comprehensive metrics")
    void testComprehensiveMetrics() {
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

    @Test
    @DisplayName("Agent should handle error scenarios gracefully")
    void testErrorHandling() {
        // Test with null request - should handle gracefully
        assertThrows(Exception.class, () -> {
            agentChatService.processChat(null);
        }, "Should handle null request gracefully");

        // Test with empty messages - should handle gracefully
        ai.demo.springagent.dto.ChatRequest emptyRequest = new ai.demo.springagent.dto.ChatRequest();
        emptyRequest.setMessages(java.util.List.of());

        assertThrows(Exception.class, () -> {
            agentChatService.processChat(emptyRequest);
        }, "Should handle empty messages gracefully");
    }
}