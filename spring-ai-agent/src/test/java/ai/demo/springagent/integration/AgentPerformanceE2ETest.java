package ai.demo.springagent.integration;

import ai.demo.springagent.dto.ChatRequest;
import ai.demo.springagent.dto.ChatResponse;
import ai.demo.springagent.service.AgentChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance and load testing for the agent integration.
 *
 * <p>These tests validate the agent's performance under various load conditions,
 * including concurrent requests, sustained load, and stress testing.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AgentPerformanceE2ETest {

    @Autowired
    private AgentChatService agentChatService;

    private static final int CONCURRENT_THREADS = 10;
    private static final int STRESS_REQUESTS = 100;
    private static final int PERFORMANCE_REQUESTS = 50;

    @BeforeEach
    void setUp() {
        // Clean slate for performance tests
    }

    @Test
    @DisplayName("Agent should handle moderate concurrent load")
    void testModerateConcurrentLoad() throws Exception {
        int concurrentRequests = 20;
        int totalRequests = 100;

        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Long> responseTimes = new CopyOnWriteArrayList<>();

        Instant startTime = Instant.now();

        // When: Submitting concurrent requests
        for (int i = 0; i < totalRequests; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    Instant requestStart = Instant.now();
                    ChatRequest request = createTestChatRequest("Performance test " + requestId);
                    ChatResponse response = agentChatService.processChat(request);

                    long responseTime = Duration.between(requestStart, Instant.now()).toMillis();
                    responseTimes.add(responseTime);

                    assertNotNull(response, "Response should not be null");
                    assertNotNull(response.getChoices(), "Response should have choices");
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("Request " + requestId + " failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then: All requests should complete
        assertTrue(latch.await(60, TimeUnit.SECONDS), "All requests should complete within timeout");

        Instant endTime = Instant.now();
        Duration totalTime = Duration.between(startTime, endTime);

        executor.shutdown();

        // Validate results
        assertEquals(totalRequests, successCount.get() + errorCount.get(), "All requests should be accounted for");
        assertTrue(successCount.get() > totalRequests * 0.95, "At least 95% of requests should succeed");

        // Performance assertions
        assertTrue(totalTime.getSeconds() < 60, "Total time should be reasonable");

        if (!responseTimes.isEmpty()) {
            long avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).sum() / responseTimes.size();
            long maxResponseTime = responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);

            System.out.println("=== Performance Test Results ===");
            System.out.println("Total Requests: " + totalRequests);
            System.out.println("Successful Requests: " + successCount.get());
            System.out.println("Failed Requests: " + errorCount.get());
            System.out.println("Success Rate: " + (successCount.get() * 100.0 / totalRequests) + "%");
            System.out.println("Total Time: " + totalTime.toMillis() + "ms");
            System.out.println("Average Response Time: " + avgResponseTime + "ms");
            System.out.println("Max Response Time: " + maxResponseTime + "ms");
            System.out.println("Requests per Second: " + (totalRequests * 1000.0 / totalTime.toMillis()));

            assertTrue(avgResponseTime < 10000, "Average response time should be under 10 seconds");
        }
    }

    @Test
    @DisplayName("Agent should maintain performance under sustained load")
    void testSustainedLoad() throws Exception {
        int durationSeconds = 10;
        int requestsPerSecond = 5;
        int totalExpectedRequests = durationSeconds * requestsPerSecond;

        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        AtomicInteger requestCount = new AtomicInteger(0);

        Instant startTime = Instant.now();

        // Submit requests continuously for the test duration
        for (int second = 0; second < durationSeconds; second++) {
            for (int request = 0; request < requestsPerSecond; request++) {
                final int requestId = requestCount.incrementAndGet();

                executor.submit(() -> {
                    try {
                        Instant requestStart = Instant.now();
                        ChatRequest chatRequest = createTestChatRequest("Sustained load test " + requestId);
                        ChatResponse response = agentChatService.processChat(chatRequest);

                        long responseTime = Duration.between(requestStart, Instant.now()).toMillis();
                        totalResponseTime.addAndGet(responseTime);

                        assertNotNull(response, "Response should not be null");
                        successCount.incrementAndGet();

                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        System.err.println("Sustained load request failed: " + e.getMessage());
                    }
                });

                // Small delay between requests to achieve target rate
                Thread.sleep(1000 / requestsPerSecond);
            }
        }

        // Wait for all requests to complete
        Thread.sleep(5000); // Allow time for final requests
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        Instant endTime = Instant.now();
        Duration actualDuration = Duration.between(startTime, endTime);

        int totalRequests = successCount.get() + errorCount.get();
        double requestsPerSecondActual = (double) totalRequests / actualDuration.toSeconds();

        System.out.println("=== Sustained Load Test Results ===");
        System.out.println("Test Duration: " + actualDuration.getSeconds() + " seconds");
        System.out.println("Expected RPS: " + requestsPerSecond);
        System.out.println("Actual RPS: " + String.format("%.2f", requestsPerSecondActual));
        System.out.println("Total Requests: " + totalRequests);
        System.out.println("Success Rate: " + (successCount.get() * 100.0 / totalRequests) + "%");
        System.out.println("Average Response Time: " + (totalResponseTime.get() / Math.max(1, successCount.get())) + "ms");

        // Validate performance
        assertTrue(requestsPerSecondActual >= requestsPerSecond * 0.8,
                  "Should maintain at least 80% of target RPS");
        assertTrue(successCount.get() >= totalRequests * 0.95,
                  "Should maintain high success rate under sustained load");
    }

    @Test
    @DisplayName("Agent should handle burst load without degradation")
    void testBurstLoadHandling() throws Exception {
        int burstSize = 50;

        ExecutorService executor = Executors.newFixedThreadPool(burstSize);
        CountDownLatch latch = new CountDownLatch(burstSize);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Instant> completionTimes = new ArrayList<>();

        // When: Submitting burst of requests
        for (int i = 0; i < burstSize; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    ChatRequest request = createTestChatRequest("Burst test " + requestId);
                    ChatResponse response = agentChatService.processChat(request);

                    assertNotNull(response, "Burst response should not be null");
                    successCount.incrementAndGet();
                    completionTimes.add(Instant.now());

                } catch (Exception e) {
                    System.err.println("Burst request failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then: All burst requests should complete
        assertTrue(latch.await(30, TimeUnit.SECONDS), "Burst requests should complete");

        executor.shutdown();

        // Validate no significant performance degradation
        assertEquals(burstSize, successCount.get(), "All burst requests should succeed");

        if (completionTimes.size() > 1) {
            Instant firstCompletion = completionTimes.get(0);
            Instant lastCompletion = completionTimes.get(completionTimes.size() - 1);
            Duration burstDuration = Duration.between(firstCompletion, lastCompletion);

            System.out.println("=== Burst Load Test Results ===");
            System.out.println("Burst Size: " + burstSize);
            System.out.println("Success Rate: 100%");
            System.out.println("Burst Duration: " + burstDuration.toMillis() + "ms");

            assertTrue(burstDuration.getSeconds() < 20, "Burst should complete quickly");
        }
    }

    @Test
    @DisplayName("Agent should scale with increasing load")
    void testLoadScaling() throws Exception {
        int[] loadLevels = {5, 10, 20, 30, 50};

        for (int loadLevel : loadLevels) {
            System.out.println("Testing load level: " + loadLevel);

            ExecutorService executor = Executors.newFixedThreadPool(loadLevel);
            CountDownLatch latch = new CountDownLatch(loadLevel);
            AtomicInteger successCount = new AtomicInteger(0);
            List<Long> responseTimes = new CopyOnWriteArrayList<>();

            Instant startTime = Instant.now();

            // Submit requests for this load level
            for (int i = 0; i < loadLevel; i++) {
                final int requestId = i;
                executor.submit(() -> {
                    try {
                        Instant requestStart = Instant.now();
                        ChatRequest request = createTestChatRequest("Scaling test " + loadLevel + "-" + requestId);
                        ChatResponse response = agentChatService.processChat(request);

                        long responseTime = Duration.between(requestStart, Instant.now()).toMillis();
                        responseTimes.add(responseTime);

                        assertNotNull(response, "Response should not be null");
                        successCount.incrementAndGet();

                    } catch (Exception e) {
                        System.err.println("Scaling test request failed: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Wait for completion
            assertTrue(latch.await(60, TimeUnit.SECONDS), "Load level " + loadLevel + " should complete");
            executor.shutdown();

            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);

            if (!responseTimes.isEmpty()) {
                long avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).sum() / responseTimes.size();
                double throughput = (double) loadLevel / duration.toSeconds();

                System.out.println("  Load: " + loadLevel + ", Success: " + successCount.get() +
                                 ", Avg Time: " + avgResponseTime + "ms" +
                                 ", Throughput: " + String.format("%.2f", throughput) + " req/s");

                // Performance should not degrade significantly
                assertTrue(avgResponseTime < 15000, "Response time should remain reasonable at load " + loadLevel);
                assertTrue(successCount.get() >= loadLevel * 0.9, "Should maintain high success rate at load " + loadLevel);
            }
        }
    }

    @Test
    @DisplayName("Agent should handle memory pressure")
    void testMemoryPressureHandling() throws Exception {
        int memoryTestRequests = 30;

        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger successCount = new AtomicInteger(0);

        // Submit requests that create memory pressure
        for (int i = 0; i < memoryTestRequests; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    // Create a complex request that may use more memory
                    ChatRequest request = createLargeChatRequest("Memory pressure test " + requestId);
                    ChatResponse response = agentChatService.processChat(request);

                    assertNotNull(response, "Memory pressure response should not be null");
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    System.err.println("Memory pressure test request failed: " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        System.out.println("=== Memory Pressure Test Results ===");
        System.out.println("Total Requests: " + memoryTestRequests);
        System.out.println("Successful: " + successCount.get());
        System.out.println("Success Rate: " + (successCount.get() * 100.0 / memoryTestRequests) + "%");

        // Should handle memory pressure gracefully
        assertTrue(successCount.get() >= memoryTestRequests * 0.8,
                  "Should handle memory pressure with reasonable success rate");
    }

    @Test
    @DisplayName("Agent should demonstrate consistent response quality under load")
    void testResponseQualityUnderLoad() throws Exception {
        int qualityTestRequests = 20;

        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<String> responses = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(qualityTestRequests);

        // Submit requests that should generate quality responses
        for (int i = 0; i < qualityTestRequests; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    ChatRequest request = createTestChatRequest(
                        "Write a detailed explanation about machine learning concepts. Request " + requestId);
                    ChatResponse response = agentChatService.processChat(request);

                    if (response != null && !response.getChoices().isEmpty()) {
                        String content = response.getChoices().get(0).getMessage().getContent();
                        if (content != null && content.length() > 50) {
                            synchronized (responses) {
                                responses.add(content);
                            }
                        }
                    }

                } catch (Exception e) {
                    System.err.println("Quality test request failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(60, TimeUnit.SECONDS), "Quality test requests should complete");
        executor.shutdown();

        // Analyze response quality
        System.out.println("=== Response Quality Analysis ===");
        System.out.println("Quality Responses Received: " + responses.size());
        System.out.println("Expected Responses: " + qualityTestRequests);

        if (!responses.isEmpty()) {
            int avgLength = responses.stream().mapToInt(String::length).sum() / responses.size();
            int minLength = responses.stream().mapToInt(String::length).min().orElse(0);
            int maxLength = responses.stream().mapToInt(String::length).max().orElse(0);

            System.out.println("Average Response Length: " + avgLength + " characters");
            System.out.println("Min Response Length: " + minLength + " characters");
            System.out.println("Max Response Length: " + maxLength + " characters");

            // Validate quality metrics
            assertTrue(avgLength > 100, "Responses should be substantial under load");
            assertTrue(responses.size() >= qualityTestRequests * 0.8,
                      "Should maintain response quality under load");
        }
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

    private ChatRequest createLargeChatRequest(String message) {
        ChatRequest request = createTestChatRequest(message);
        request.setMaxTokens(200);
        request.setTemperature(0.8);
        return request;
    }
}