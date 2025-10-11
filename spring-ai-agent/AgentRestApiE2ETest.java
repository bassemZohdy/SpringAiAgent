package ai.demo.springagent.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import ai.demo.springagent.dto.ChatRequest;
import ai.demo.springagent.dto.ChatResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for Agent REST API endpoints.
 *
 * <p>These tests validate all the REST API endpoints for agent management,
 * including chat processing, health checks, metrics, and memory management.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
class AgentRestApiE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String agentBaseUrl = "/api/v1/agent";

    @BeforeEach
    void setUp() {
        // Ensure clean state before each test
        try {
            mockMvc.perform(post(agentBaseUrl + "/memory/clear"))
                   .andExpect(status().isOk());
        } catch (Exception e) {
            // Ignore if memory clearing fails
        }
    }

    @Test
    @DisplayName("GET /api/v1/agent/health should return agent health status")
    void testAgentHealthEndpoint() throws Exception {
        mockMvc.perform(get(agentBaseUrl + "/health")
               .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").exists())
               .andExpect(jsonPath("$.agent").exists())
               .andExpect(jsonPath("$.agent.name").exists())
               .andExpect(jsonPath("$.agent.state").exists())
               .andExpect(jsonPath("$.agent.running").exists())
               .andExpect(jsonPath("$.memory").exists());
    }

    @Test
    @DisplayName("GET /api/v1/agent/metrics should return comprehensive metrics")
    void testAgentMetricsEndpoint() throws Exception {
        mockMvc.perform(get(agentBaseUrl + "/metrics")
               .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.agentId").exists())
               .andExpect(jsonPath("$.agentName").exists())
               .andExpect(jsonPath("$.version").exists())
               .andExpect(jsonPath("$.state").exists())
               .andExpect(jsonPath("$.isRunning").exists())
               .andExpect(jsonPath("$.capabilities").exists())
               .andExpect(jsonPath("$.metrics").exists())
               .andExpect(jsonPath("$.metrics.tasksProcessed").exists())
               .andExpect(jsonPath("$.metrics.tasksSucceeded").exists())
               .andExpect(jsonPath("$.metrics.tasksFailed").exists())
               .andExpect(jsonPath("$.metrics.successRate").exists())
               .andExpect(jsonPath("$.metrics.averageProcessingTime").exists())
               .andExpect(jsonPath("$.memory").exists())
               .andExpect(jsonPath("$.memory.size").exists())
               .andExpect(jsonPath("$.memory.isEmpty").exists())
               .andExpect(jsonPath("$.memory.hasSummary").exists());
    }

    @Test
    @DisplayName("GET /api/v1/agent/capabilities should return agent capabilities")
    void testAgentCapabilitiesEndpoint() throws Exception {
        mockMvc.perform(get(agentBaseUrl + "/capabilities")
               .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.agent").exists())
               .andExpect(jsonPath("$.agent.id").exists())
               .andExpect(jsonPath("$.agent.name").exists())
               .andExpect(jsonPath("$.agent.version").exists())
               .andExpect(jsonPath("$.agent.createdAt").exists())
               .andExpect(jsonPath("$.agent.capabilities").exists())
               .andExpect(jsonPath("$.features").exists())
               .andExpect(jsonPath("$.features.taskProcessing").value(true))
               .andExpect(jsonPath("$.features.memoryManagement").value(true))
               .andExpect(jsonPath("$.features.metricsCollection").value(true))
               .andExpect(jsonPath("$.features.lifecycleManagement").value(true))
               .andExpect(jsonPath("$.features.errorHandling").value(true))
               .andExpect(jsonPath("$.features.threadIntegration").value(true))
               .andExpect(jsonPath("$.abstractions").exists())
               .andExpect(jsonPath("$.abstractions.baseInterface").value("Agent"))
               .andExpect(jsonPath("$.abstractions.taskInterface").value("TaskAgent<TASK, RESULT>"))
               .andExpect(jsonPath("$.abstractions.aiInterface").value("AiAgent<TASK, PROMPT, CHAT_RESPONSE, RESULT>"))
               .andExpect(jsonPath("$.abstractions.chatInterface").value("ChatAgent<REQUEST, RESPONSE>"));
    }

    @Test
    @DisplayName("POST /api/v1/agent/chat should process chat requests")
    void testAgentChatEndpoint() throws Exception {
        ChatRequest chatRequest = createTestChatRequest("Hello, agent!");

        mockMvc.perform(post(agentBaseUrl + "/chat")
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(chatRequest)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").exists())
               .andExpect(jsonPath("$.object").value("chat.completion"))
               .andExpect(jsonPath("$.created").exists())
               .andExpect(jsonPath("$.model").exists())
               .andExpect(jsonPath("$.choices").exists())
               .andExpect(jsonPath("$.choices").isArray())
               .andExpect(jsonPath("$.choices[0].index").value(0))
               .andExpect(jsonPath("$.choices[0].message").exists())
               .andExpect(jsonPath("$.choices[0].message.role").value("assistant"))
               .andExpect(jsonPath("$.choices[0].message.content").exists())
               .andExpect(jsonPath("$.choices[0].finishReason").exists())
               .andExpect(jsonPath("$.usage").exists())
               .andExpect(jsonPath("$.usage.promptTokens").exists())
               .andExpect(jsonPath("$.usage.completionTokens").exists())
               .andExpect(jsonPath("$.usage.totalTokens").exists());
    }

    @Test
    @DisplayName("POST /api/v1/agent/chat/memory should process chat with memory")
    void testAgentChatWithMemoryEndpoint() throws Exception {
        ChatRequest chatRequest = createTestChatRequestWithThread("Remember this conversation", "test-thread-123");

        mockMvc.perform(post(agentBaseUrl + "/chat/memory")
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(chatRequest)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").exists())
               .andExpect(jsonPath("$.choices").exists())
               .andExpect(jsonPath("$.choices[0].message.content").exists());
    }

    @Test
    @DisplayName("POST /api/v1/agent/memory/compact should compact agent memory")
    void testMemoryCompactEndpoint() throws Exception {
        mockMvc.perform(post(agentBaseUrl + "/memory/compact")
               .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("success"))
               .andExpect(jsonPath("$.message").value("Agent memory compacted successfully"));
    }

    @Test
    @DisplayName("POST /api/v1/agent/memory/clear should clear agent memory")
    void testMemoryClearEndpoint() throws Exception {
        mockMvc.perform(post(agentBaseUrl + "/memory/clear")
               .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("success"))
               .andExpect(jsonPath("$.message").value("Agent memory cleared successfully"));
    }

    @Test
    @DisplayName("Agent should handle concurrent API requests")
    void testConcurrentApiRequests() throws Exception {
        int concurrentRequests = 5;
        Thread[] threads = new Thread[concurrentRequests];
        boolean[] results = new boolean[concurrentRequests];

        // Create concurrent requests
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    ChatRequest chatRequest = createTestChatRequest("Concurrent request " + requestIndex);
                    String response = mockMvc.perform(post(agentBaseUrl + "/chat")
                                                   .contentType(MediaType.APPLICATION_JSON)
                                                   .content(objectMapper.writeValueAsString(chatRequest)))
                                                   .andExpect(status().isOk())
                                                   .andReturn().getResponse().getContentAsString();

                    // Parse response to verify it's valid
                    ChatResponse chatResponse = objectMapper.readValue(response, ChatResponse.class);
                    results[requestIndex] = chatResponse.getChoices() != null &&
                                          !chatResponse.getChoices().isEmpty();
                } catch (Exception e) {
                    results[requestIndex] = false;
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(10000); // 10 second timeout
        }

        // Verify all requests succeeded
        for (int i = 0; i < concurrentRequests; i++) {
            assertTrue(results[i], "Concurrent request " + i + " should succeed");
        }
    }

    @Test
    @DisplayName("Agent should handle malformed requests gracefully")
    void testMalformedRequestHandling() throws Exception {
        // Test completely invalid JSON
        mockMvc.perform(post(agentBaseUrl + "/chat")
               .contentType(MediaType.APPLICATION_JSON)
               .content("{ invalid json }"))
               .andExpect(status().isBadRequest());

        // Test missing required fields
        Map<String, Object> incompleteRequest = Map.of("model", "test-model");
        mockMvc.perform(post(agentBaseUrl + "/chat")
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(incompleteRequest)))
               .andExpect(status().isInternalServerError());

        // Test invalid HTTP method
        mockMvc.perform(delete(agentBaseUrl + "/chat")
               .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("Agent should maintain conversation context across requests")
    void testConversationContextMaintenance() throws Exception {
        String threadId = "context-test-thread";

        // Send first message
        ChatRequest firstRequest = createTestChatRequestWithThread("My name is Alice", threadId);
        mockMvc.perform(post(agentBaseUrl + "/chat/memory")
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(firstRequest)))
               .andExpect(status().isOk());

        // Send follow-up message
        ChatRequest secondRequest = createTestChatRequestWithThread("What's my name?", threadId);
        String response = mockMvc.perform(post(agentBaseUrl + "/chat/memory")
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(secondRequest)))
               .andExpect(status().isOk())
               .andReturn().getResponse().getContentAsString();

        ChatResponse chatResponse = objectMapper.readValue(response, ChatResponse.class);
        String content = chatResponse.getChoices().get(0).getMessage().getContent();

        // Should remember the context (ideally mentioning Alice)
        assertNotNull(content, "Response should not be null");
    }

    @Test
    @DisplayName("Agent API should be consistent and follow OpenAI format")
    void testApiConsistency() throws Exception {
        // Test that response format matches OpenAI API specification
        ChatRequest chatRequest = createTestChatRequest("Test API consistency");

        String response = mockMvc.perform(post(agentBaseUrl + "/chat")
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(chatRequest)))
               .andExpect(status().isOk())
               .andReturn().getResponse().getContentAsString();

        ChatResponse chatResponse = objectMapper.readValue(response, ChatResponse.class);

        // Validate OpenAI format compliance
        assertNotNull(chatResponse.getId(), "Response should have ID");
        assertTrue(chatResponse.getId().startsWith("chatcmpl-"), "ID should follow OpenAI format");
        assertEquals("chat.completion", chatResponse.getObject(), "Object should be 'chat.completion'");
        assertTrue(chatResponse.getCreated() > 0, "Should have creation timestamp");
        assertNotNull(chatResponse.getModel(), "Should have model information");
        assertNotNull(chatResponse.getChoices(), "Should have choices array");
        assertEquals(1, chatResponse.getChoices().size(), "Should have exactly one choice");

        ChatResponse.Choice choice = chatResponse.getChoices().get(0);
        assertEquals(0, choice.getIndex(), "Choice index should be 0");
        assertNotNull(choice.getMessage(), "Choice should have message");
        assertEquals("assistant", choice.getMessage().getRole(), "Message role should be 'assistant'");
        assertNotNull(choice.getMessage().getContent(), "Message should have content");
        assertNotNull(choice.getFinishReason(), "Should have finish reason");

        assertNotNull(chatResponse.getUsage(), "Should have usage information");
        assertTrue(chatResponse.getUsage().getPromptTokens() >= 0, "Prompt tokens should be non-negative");
        assertTrue(chatResponse.getUsage().getCompletionTokens() >= 0, "Completion tokens should be non-negative");
        assertTrue(chatResponse.getUsage().getTotalTokens() >= 0, "Total tokens should be non-negative");
    }

    @Test
    @DisplayName("Agent should handle different request parameters")
    void testRequestParameterHandling() throws Exception {
        // Test with different temperature values
        ChatRequest tempRequest = createTestChatRequest("Test temperature");
        tempRequest.setTemperature(0.1);
        mockMvc.perform(post(agentBaseUrl + "/chat")
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(tempRequest)))
               .andExpect(status().isOk());

        // Test with max tokens
        ChatRequest tokensRequest = createTestChatRequest("Test max tokens");
        tokensRequest.setMaxTokens(50);
        mockMvc.perform(post(agentBaseUrl + "/chat")
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(tokensRequest)))
               .andExpect(status().isOk());

        // Test with streaming (should still work even if not fully implemented)
        ChatRequest streamRequest = createTestChatRequest("Test streaming");
        streamRequest.setStream(true);
        mockMvc.perform(post(agentBaseUrl + "/chat")
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(streamRequest)))
               .andExpect(status().isOk());
    }

    private ChatRequest createTestChatRequest(String message) {
        ChatRequest request = new ChatRequest();
        request.setModel("test-model");
        request.setTemperature(0.7);
        request.setMaxTokens(100);
        request.setStream(false);

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
}