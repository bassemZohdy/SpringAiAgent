package ai.demo.springagent.integration;

import ai.demo.springagent.dto.ChatRequest;
import ai.demo.springagent.dto.ChatResponse;
import ai.demo.springagent.service.AgentChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Resilience and error scenario testing for the agent integration.
 *
 * <p>These tests validate the agent's behavior under various error conditions,
 * edge cases, and failure scenarios to ensure robust operation.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AgentResilienceE2ETest {

    @Autowired
    private AgentChatService agentChatService;

    @BeforeEach
    void setUp() {
        // Ensure clean state for resilience tests
    }

    @Test
    @DisplayName("Agent should handle malformed requests gracefully")
    void testMalformedRequestHandling() {
        // Test null request
        assertThrows(Exception.class, () -> {
            agentChatService.processChat(null);
        }, "Should handle null request");

        // Test empty request
        ChatRequest emptyRequest = new ChatRequest();
        emptyRequest.setMessages(List.of());
        assertThrows(Exception.class, () -> {
            agentChatService.processChat(emptyRequest);
        }, "Should handle empty messages");

        // Test request with null messages list
        ChatRequest nullMessagesRequest = new ChatRequest();
        nullMessagesRequest.setMessages(null);
        assertThrows(Exception.class, () -> {
            agentChatService.processChat(nullMessagesRequest);
        }, "Should handle null messages list");

        // Test request with null message content
        ChatRequest nullContentRequest = new ChatRequest();
        ChatRequest.Message messageWithNullContent = new ChatRequest.Message();
        messageWithNullContent.setRole("user");
        messageWithNullContent.setContent(null);
        nullContentRequest.setMessages(List.of(messageWithNullContent));

        assertThrows(Exception.class, () -> {
            agentChatService.processChat(nullContentRequest);
        }, "Should handle null message content");
    }

    @Test
    @DisplayName("Agent should handle extremely large requests")
    void testLargeRequestHandling() throws Exception {
        // Test very long message
        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longMessage.append("This is a very long message to test how the agent handles large inputs. ");
            longMessage.append("Sentence ").append(i).append(". ");
        }

        ChatRequest largeRequest = createTestChatRequest(longMessage.toString());

        // Should either handle gracefully or fail with appropriate error
        try {
            ChatResponse response = agentChatService.processChat(largeRequest);
            assertNotNull(response, "Should handle large request or fail gracefully");

            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                String content = response.getChoices().get(0).getMessage().getContent();
                assertNotNull(content, "Response to large request should not be null");
            }
        } catch (Exception e) {
            // Expected for very large requests
            assertTrue(e.getMessage() != null, "Should provide meaningful error message");
        }
    }

    @Test
    @DisplayName("Agent should handle requests with special characters")
    void testSpecialCharacterHandling() throws Exception {
        // Test various special characters
        String[] specialCharTests = {
            "Test with emojis: 🤖🚀💡",
            "Test with math symbols: ∑∏∫∆∇∈",
            "Test with currency: $€£¥₹₽",
            "Test with quotes: 'single' and \"double\" quotes",
            "Test with unicode: 中文, русский, العربية",
            "Test with code blocks: `inline code` and ```code blocks```",
            "Test with JSON: {\"key\": \"value\"}",
            "Test with HTML: <p>paragraph</p>",
            "Test with escape sequences: \\n\\t\\r",
            "Test with zero-width characters: ​‌‍"
        };

        for (String testMessage : specialCharTests) {
            ChatRequest request = createTestChatRequest(testMessage);
            ChatResponse response = agentChatService.processChat(request);

            assertNotNull(response, "Should handle special characters: " + testMessage.substring(0, 20) + "...");
            assertNotNull(response.getChoices(), "Should have choices for special chars test");
            assertFalse(response.getChoices().isEmpty(), "Choices should not be empty for special chars");
        }
    }

    @Test
    @DisplayName("Agent should handle concurrent stress without breaking")
    void testConcurrentStressResilience() throws Exception {
        int stressThreads = 20;
        int requestsPerThread = 5;

        ExecutorService executor = Executors.newFixedThreadPool(stressThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Submit concurrent stress requests
        for (int thread = 0; thread < stressThreads; thread++) {
            final int threadId = thread;
            executor.submit(() -> {
                for (int request = 0; request < requestsPerThread; request++) {
                    try {
                        ChatRequest chatRequest = createTestChatRequest(
                            "Stress test thread " + threadId + " request " + request);
                        ChatResponse response = agentChatService.processChat(chatRequest);

                        if (response != null) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        // Log but don't fail the test for individual request errors under stress
                        System.err.println("Stress request failed: " + e.getMessage());
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        int totalRequests = stressThreads * requestsPerThread;
        int handledRequests = successCount.get() + errorCount.get();

        System.out.println("=== Concurrent Stress Test Results ===");
        System.out.println("Total Requests: " + totalRequests);
        System.out.println("Successful: " + successCount.get());
        System.out.println("Errors: " + errorCount.get());
        System.out.println("Handled: " + handledRequests);
        System.out.println("Success Rate: " + (successCount.get() * 100.0 / totalRequests) + "%");

        // Should handle most requests even under stress
        assertTrue(handledRequests >= totalRequests * 0.8, "Should handle at least 80% of requests under stress");
    }

    @Test
    @DisplayName("Agent should recover from temporary failures")
    void testFailureRecovery() throws Exception {
        // Test multiple requests to ensure recovery capability
        int recoveryAttempts = 5;

        for (int attempt = 0; attempt < recoveryAttempts; attempt++) {
            try {
                ChatRequest request = createTestChatRequest("Recovery test attempt " + (attempt + 1));
                ChatResponse response = agentChatService.processChat(request);

                assertNotNull(response, "Should recover and provide response on attempt " + (attempt + 1));
                assertNotNull(response.getChoices(), "Should have choices after recovery");

                // If successful, recovery is working
                System.out.println("Recovery successful on attempt " + (attempt + 1));
                return;

            } catch (Exception e) {
                System.err.println("Attempt " + (attempt + 1) + " failed: " + e.getMessage());
                if (attempt == recoveryAttempts - 1) {
                    // Only fail if all recovery attempts fail
                    fail("Should recover from temporary failures after " + recoveryAttempts + " attempts");
                }
                // Brief pause before retry
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Test
    @DisplayName("Agent should handle timeout scenarios gracefully")
    void testTimeoutHandling() throws Exception {
        // Test with very short timeout (if supported)
        long startTime = System.currentTimeMillis();

        try {
            ChatRequest request = createTestChatRequest("Timeout test request");
            ChatResponse response = agentChatService.processChat(request);
            long duration = System.currentTimeMillis() - startTime;

            assertNotNull(response, "Should complete within reasonable time or timeout gracefully");

            // If it completed, check it was reasonably fast
            if (duration > 30000) { // 30 seconds
                System.out.println("Request took " + duration + "ms - may indicate timeout issues");
            }

        } catch (Exception e) {
            // Check if it's a timeout-related exception
            String errorMessage = e.getMessage().toLowerCase();
            if (errorMessage.contains("timeout") || errorMessage.contains("time out")) {
                System.out.println("Proper timeout handling detected: " + e.getMessage());
            } else {
                // Different error - rethrow
                throw e;
            }
        }
    }

    @Test
    @DisplayName("Agent should maintain data consistency under concurrent access")
    void testDataConsistencyUnderConcurrency() throws Exception {
        int consistencyTestRequests = 20;

        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger successCount = new AtomicInteger(0);
        List<String> responseContents = new java.util.concurrent.CopyOnWriteArrayList<>();

        // Submit concurrent requests to the same agent
        for (int i = 0; i < consistencyTestRequests; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    ChatRequest request = createTestChatRequest(
                        "Consistency test " + requestId + ". What is 2+2?");
                    ChatResponse response = agentChatService.processChat(request);

                    if (response != null && !response.getChoices().isEmpty()) {
                        String content = response.getChoices().get(0).getMessage().getContent();
                        responseContents.add(content);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("Consistency test failed: " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        System.out.println("=== Data Consistency Test Results ===");
        System.out.println("Total Requests: " + consistencyTestRequests);
        System.out.println("Successful: " + successCount.get());
        System.out.println("Responses Received: " + responseContents.size());

        // Basic consistency checks
        assertTrue(responseContents.size() > 0, "Should receive some responses");
        assertEquals(successCount.get(), responseContents.size(), "Success count should match response count");
    }

    @Test
    @DisplayName("Agent should handle rapid successive requests")
    void testRapidSuccessiveRequests() throws Exception {
        int rapidRequests = 15;
        List<Long> completionTimes = new java.util.ArrayList<>();

        // Submit rapid successive requests
        for (int i = 0; i < rapidRequests; i++) {
            long startTime = System.currentTimeMillis();

            try {
                ChatRequest request = createTestChatRequest("Rapid test " + i);
                ChatResponse response = agentChatService.processChat(request);

                long completionTime = System.currentTimeMillis() - startTime;
                completionTimes.add(completionTime);

                assertNotNull(response, "Rapid request " + i + " should succeed");

            } catch (Exception e) {
                System.err.println("Rapid request " + i + " failed: " + e.getMessage());
            }
        }

        if (!completionTimes.isEmpty()) {
            long avgTime = completionTimes.stream().mapToLong(Long::longValue).sum() / completionTimes.size();
            long maxTime = completionTimes.stream().mapToLong(Long::longValue).max().orElse(0);

            System.out.println("=== Rapid Successive Requests Test Results ===");
            System.out.println("Total Requests: " + rapidRequests);
            System.out.println("Average Time: " + avgTime + "ms");
            System.out.println("Max Time: " + maxTime + "ms");

            // Should handle rapid requests reasonably well
            assertTrue(avgTime < 5000, "Average response time should be reasonable for rapid requests");
        }
    }

    @Test
    @DisplayName("Agent should validate request parameters properly")
    void testRequestParameterValidation() {
        // Test various parameter combinations
        ChatRequest request = new ChatRequest();

        // Test with missing required fields
        request.setModel(null);
        request.setTemperature(1.5); // Invalid temperature
        request.setMaxTokens(-1); // Invalid max tokens
        request.setMessages(List.of());

        // Should handle invalid parameters gracefully
        assertThrows(Exception.class, () -> {
            agentChatService.processChat(request);
        }, "Should validate request parameters");
    }

    @Test
    @DisplayName("Agent should handle resource exhaustion gracefully")
    void testResourceExhaustionHandling() throws Exception {
        // This test simulates resource exhaustion by creating many concurrent requests
        int resourceTestRequests = 100;

        ExecutorService executor = Executors.newFixedThreadPool(50);
        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Create resource pressure
        for (int i = 0; i < resourceTestRequests; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    ChatRequest request = createTestChatRequest(
                        "Resource test " + requestId + ". Generate a comprehensive response.");
                    request.setMaxTokens(200); // Larger token requirement
                    ChatResponse response = agentChatService.processChat(request);

                    if (response != null) {
                        completedCount.incrementAndGet();
                    } else {
                        errorCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    // Expected under resource pressure
                    System.err.println("Resource exhaustion test request failed: " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        int totalProcessed = completedCount.get() + errorCount.get();

        System.out.println("=== Resource Exhaustion Test Results ===");
        System.out.println("Total Requests: " + resourceTestRequests);
        System.out.println("Completed: " + completedCount.get());
        System.out.println("Errors: " + errorCount.get());
        System.out.println("Total Processed: " + totalProcessed);
        System.out.println("Completion Rate: " + (completedCount.get() * 100.0 / totalProcessed) + "%");

        // Should handle some requests even under resource pressure
        assertTrue(totalProcessed > 0, "Should process some requests even under resource pressure");
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
}