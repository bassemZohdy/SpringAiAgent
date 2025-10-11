package ai.demo.springagent.integration;

import ai.demo.springagent.agent.ChatCompletionAgent;
import ai.demo.springagent.dto.ChatRequest;
import ai.demo.springagent.dto.ChatResponse;
import ai.demo.springagent.task.ChatTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests for the agent transformation pipeline.
 *
 * <p>These tests validate the complete AI transformation pipeline:
 * TASK → PROMPT → CHAT_RESPONSE → RESULT</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AgentTransformationPipelineE2ETest {

    @Autowired
    private ChatCompletionAgent chatAgent;

    @BeforeEach
    void setUp() {
        if (!chatAgent.isRunning()) {
            chatAgent.start();
        }
    }

    @Test
    @DisplayName("Agent should transform ChatTask to Prompt correctly")
    void testTaskToPromptTransformation() {
        // Given: A ChatTask with complex message history
        ChatRequest chatRequest = createComplexChatRequest();
        ChatTask chatTask = new ChatTask(chatRequest);

        // When: Agent transforms task to prompt
        Prompt prompt = chatAgent.transformToPrompt(chatTask);

        // Then: Prompt should be properly formatted
        assertNotNull(prompt, "Transformed prompt should not be null");
        assertNotNull(prompt.getContents(), "Prompt contents should not be null");
        assertFalse(prompt.getContents().isEmpty(), "Prompt should have content");

        // And: Should preserve conversation context
        String promptText = prompt.getContents().get(0).getContent();
        assertNotNull(promptText, "Prompt text should not be null");
        assertTrue(promptText.contains("USER:") || promptText.contains("ASSISTANT:"),
                  "Prompt should contain conversation roles");
    }

    @Test
    @DisplayName("Agent should transform AI response to ChatResponse correctly")
    void testResponseToChatResponseTransformation() {
        // Given: A sample AI response
        String aiResponse = "Hello! I'm an AI assistant ready to help you with your questions.";

        // When: Agent transforms AI response to ChatResponse
        ChatResponse chatResponse = chatAgent.transformFromResponse(aiResponse);

        // Then: ChatResponse should be properly formatted
        assertNotNull(chatResponse, "Transformed response should not be null");
        assertNotNull(chatResponse.getId(), "Response should have an ID");
        assertNotNull(chatResponse.getObject(), "Response should have object type");
        assertTrue(chatResponse.getCreated() > 0, "Response should have creation timestamp");

        // And: Should contain proper choices
        assertNotNull(chatResponse.getChoices(), "Response should have choices");
        assertEquals(1, chatResponse.getChoices().size(), "Should have exactly one choice");

        ChatResponse.Choice choice = chatResponse.getChoices().get(0);
        assertEquals("assistant", choice.getMessage().getRole(), "Message role should be assistant");
        assertEquals(aiResponse, choice.getMessage().getContent(), "Content should match AI response");
        assertEquals("stop", choice.getFinishReason(), "Finish reason should be 'stop'");

        // And: Should include usage information
        assertNotNull(chatResponse.getUsage(), "Response should have usage information");
        assertTrue(chatResponse.getUsage().getTotalTokens() > 0, "Should have token count");
    }

    @Test
    @DisplayName("Agent should execute complete transformation pipeline")
    void testCompleteTransformationPipeline() throws Exception {
        // Given: A chat task
        ChatRequest chatRequest = createSimpleChatRequest("Explain quantum computing in simple terms");
        ChatTask chatTask = new ChatTask(chatRequest);

        // When: Agent processes the complete pipeline
        CompletableFuture<ChatResponse> future = chatAgent.process(chatTask);
        ChatResponse response = future.get();

        // Then: Should return valid ChatResponse
        assertNotNull(response, "Should return a response");
        assertNotNull(response.getChoices(), "Should have choices");
        assertFalse(response.getChoices().isEmpty(), "Should have at least one choice");

        // And: Response should be meaningful
        String content = response.getChoices().get(0).getMessage().getContent();
        assertNotNull(content, "Response content should not be null");
        assertFalse(content.trim().isEmpty(), "Response content should not be empty");
        assertTrue(content.length() > 50, "Response should be substantial");

        // And: Should contain relevant information
        assertTrue(content.toLowerCase().contains("quantum") ||
                  content.toLowerCase().contains("computing") ||
                  content.toLowerCase().contains("computer"),
                  "Response should be relevant to the query");
    }

    @Test
    @DisplayName("Agent should handle multi-turn conversations")
    void testMultiTurnConversation() throws Exception {
        // Given: A conversation with multiple turns
        List<String> conversation = List.of(
            "What is machine learning?",
            "Can you give me a simple example?",
            "What are the main types of machine learning?"
        );

        ChatRequest.ChatResponse accumulatedResponse = null;

        // When: Processing each turn
        for (String message : conversation) {
            ChatRequest chatRequest = createSimpleChatRequest(message);
            ChatTask chatTask = new ChatTask(chatRequest);
            CompletableFuture<ChatResponse> future = chatAgent.process(chatTask);
            ChatResponse response = future.get();
            accumulatedResponse = new ChatRequest.ChatResponse(response);
        }

        // Then: Each turn should be processed successfully
        assertNotNull(accumulatedResponse, "All conversation turns should be processed");

        // And: Agent should have built up context
        assertTrue(chatAgent.getMemory().size() > 0,
                  "Agent should have accumulated conversation context");
    }

    @Test
    @DisplayName("Agent should handle edge cases in transformation")
    void testEdgeCaseHandling() throws Exception {
        // Test empty message
        ChatRequest emptyRequest = createSimpleChatRequest("");
        ChatTask emptyTask = new ChatTask(emptyRequest);
        CompletableFuture<ChatResponse> emptyFuture = chatAgent.process(emptyTask);
        ChatResponse emptyResponse = emptyFuture.get();
        assertNotNull(emptyResponse, "Should handle empty messages gracefully");

        // Test very long message
        String longMessage = "Explain " + "very ".repeat(100) + "long message processing";
        ChatRequest longRequest = createSimpleChatRequest(longMessage);
        ChatTask longTask = new ChatTask(longRequest);
        CompletableFuture<ChatResponse> longFuture = chatAgent.process(longTask);
        ChatResponse longResponse = longFuture.get();
        assertNotNull(longResponse, "Should handle long messages gracefully");

        // Test special characters
        ChatRequest specialRequest = createSimpleChatRequest("Test with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?");
        ChatTask specialTask = new ChatTask(specialRequest);
        CompletableFuture<ChatResponse> specialFuture = chatAgent.process(specialTask);
        ChatResponse specialResponse = specialFuture.get();
        assertNotNull(specialResponse, "Should handle special characters gracefully");
    }

    @Test
    @DisplayName("Agent should build appropriate context")
    void testContextBuilding() {
        // When: Custom context is requested
        String context = chatAgent.buildCustomContext();

        // Then: Should contain agent information
        assertNotNull(context, "Context should not be null");
        assertTrue(context.contains(chatAgent.getAgentName()),
                  "Context should contain agent name");
        assertTrue(context.contains(chatAgent.getVersion()),
                  "Context should contain version");

        // And: Should contain capabilities
        assertTrue(context.contains("capabilities"),
                  "Context should mention capabilities");

        // And: Should contain performance metrics if available
        var metrics = chatAgent.getMetrics();
        if (metrics.getTasksProcessed() > 0) {
            assertTrue(context.contains("Performance"),
                      "Context should contain performance info");
        }
    }

    @Test
    @DisplayName("Agent should handle errors in transformation pipeline")
    void testErrorHandling() {
        // Given: A malformed task
        ChatRequest malformedRequest = new ChatRequest();
        malformedRequest.setMessages(null); // Invalid state
        ChatTask malformedTask = new ChatTask(malformedRequest);

        // When/Then: Should handle gracefully
        assertDoesNotThrow(() -> {
            try {
                Prompt prompt = chatAgent.transformToPrompt(malformedTask);
                assertNotNull(prompt, "Should handle malformed input gracefully");
            } catch (Exception e) {
                // Expected behavior for malformed input
                assertNotNull(e.getMessage(), "Error should have descriptive message");
            }
        });
    }

    @Test
    @DisplayName("Agent should track learning from transformation results")
    void testLearningTracking() throws Exception {
        // Given: Multiple successful transformations
        int learningTasks = 3;
        long initialLearningCount = chatAgent.getMemory().getLearnings().size();

        // When: Processing multiple tasks
        for (int i = 0; i < learningTasks; i++) {
            ChatRequest chatRequest = createSimpleChatRequest("Learning test task " + i);
            ChatTask chatTask = new ChatTask(chatRequest);
            chatAgent.process(chatTask).get();
        }

        // Then: Should record learnings
        assertTrue(chatAgent.getMemory().getLearnings().size() > initialLearningCount,
                  "Should have recorded new learnings from task processing");
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

    private ChatRequest createComplexChatRequest() {
        ChatRequest request = new ChatRequest();
        request.setModel("test-model");
        request.setTemperature(0.7);
        request.setMaxTokens(200);

        // Create a conversation history
        ChatRequest.Message systemMessage = new ChatRequest.Message();
        systemMessage.setRole("system");
        systemMessage.setContent("You are a helpful AI assistant.");

        ChatRequest.Message userMessage1 = new ChatRequest.Message();
        userMessage1.setRole("user");
        userMessage1.setContent("Hello!");

        ChatRequest.Message assistantMessage1 = new ChatRequest.Message();
        assistantMessage1.setRole("assistant");
        assistantMessage1.setContent("Hello! How can I help you today?");

        ChatRequest.Message userMessage2 = new ChatRequest.Message();
        userMessage2.setRole("user");
        userMessage2.setContent("Can you explain artificial intelligence?");

        request.setMessages(List.of(systemMessage, userMessage1, assistantMessage1, userMessage2));
        return request;
    }
}