package ai.demo.springagent.controller;

import ai.demo.springagent.dto.ChatRequest;
import ai.demo.springagent.dto.ChatResponse;
import ai.demo.springagent.service.AgentChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller that demonstrates the new agent framework capabilities.
 *
 * <p>This controller provides endpoints that showcase the enhanced functionality
 * provided by the new TaskAgent and AiAgent abstractions, including metrics,
 * memory management, and improved lifecycle handling.
 */
@RestController
@RequestMapping("/api/v1/agent")
@ConditionalOnBean(AgentChatService.class)
public class AgentController {

    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

    private final AgentChatService agentChatService;

    public AgentController(AgentChatService agentChatService) {
        this.agentChatService = agentChatService;
    }

    /**
     * Process a chat request using the new agent framework.
     *
     * @param request the chat request
     * @return chat response
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chatWithAgent(@RequestBody ChatRequest request) {
        logger.info("Received agent chat request - threadId: {}", request.getThreadId());

        try {
            ChatResponse response = agentChatService.processChat(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Agent chat processing failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Process a chat request using the agent framework with enhanced memory.
     *
     * @param request the chat request
     * @return chat response
     */
    @PostMapping("/chat/memory")
    public ResponseEntity<ChatResponse> chatWithAgentMemory(@RequestBody ChatRequest request) {
        logger.info("Received agent chat request with memory - threadId: {}", request.getThreadId());

        try {
            ChatResponse response = agentChatService.processChatWithMemory(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Agent chat with memory processing failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get current agent metrics and status.
     *
     * @return agent metrics and status information
     */
    @GetMapping("/metrics")
    public ResponseEntity<Object> getAgentMetrics() {
        try {
            Object metrics = agentChatService.getAgentMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            logger.error("Failed to get agent metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get agent health status.
     *
     * @return health status information
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getAgentHealth() {
        try {
            Object metrics = agentChatService.getAgentMetrics();

            // Extract relevant health information
            Map<String, Object> healthInfo = (Map<String, Object>) metrics;
            String state = (String) healthInfo.get("state");
            Boolean isRunning = (Boolean) healthInfo.get("isRunning");

            boolean isHealthy = "STARTED".equals(state) && Boolean.TRUE.equals(isRunning);

            return ResponseEntity.ok(java.util.Map.of(
                "status", isHealthy ? "UP" : "DOWN",
                "agent", java.util.Map.of(
                    "name", healthInfo.get("agentName"),
                    "state", state,
                    "running", isRunning,
                    "tasksProcessed", ((Map<?, ?>) healthInfo.get("metrics")).get("tasksProcessed")
                ),
                "memory", ((Map<?, ?>) healthInfo.get("memory")).get("size")
            ));
        } catch (Exception e) {
            logger.error("Failed to get agent health", e);
            return ResponseEntity.ok(java.util.Map.of(
                "status", "DOWN",
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Compact agent memory to optimize performance.
     *
     * @return operation result
     */
    @PostMapping("/memory/compact")
    public ResponseEntity<Map<String, String>> compactAgentMemory() {
        logger.info("Compacting agent memory via API request");

        try {
            agentChatService.compactAgentMemory();
            return ResponseEntity.ok(java.util.Map.of(
                "status", "success",
                "message", "Agent memory compacted successfully"
            ));
        } catch (Exception e) {
            logger.error("Failed to compact agent memory", e);
            return ResponseEntity.internalServerError().body(java.util.Map.of(
                "status", "error",
                "message", "Failed to compact memory: " + e.getMessage()
            ));
        }
    }

    /**
     * Clear agent memory.
     *
     * @return operation result
     */
    @PostMapping("/memory/clear")
    public ResponseEntity<Map<String, String>> clearAgentMemory() {
        logger.info("Clearing agent memory via API request");

        try {
            agentChatService.clearAgentMemory();
            return ResponseEntity.ok(java.util.Map.of(
                "status", "success",
                "message", "Agent memory cleared successfully"
            ));
        } catch (Exception e) {
            logger.error("Failed to clear agent memory", e);
            return ResponseEntity.internalServerError().body(java.util.Map.of(
                "status", "error",
                "message", "Failed to clear memory: " + e.getMessage()
            ));
        }
    }

    /**
     * Get agent capabilities and configuration information.
     *
     * @return agent capabilities
     */
    @GetMapping("/capabilities")
    public ResponseEntity<Map<String, Object>> getAgentCapabilities() {
        try {
            Object metrics = agentChatService.getAgentMetrics();
            Map<String, Object> agentInfo = (Map<String, Object>) metrics;

            return ResponseEntity.ok(Map.of(
                "agent", Map.of(
                    "id", agentInfo.get("agentId"),
                    "name", agentInfo.get("agentName"),
                    "version", agentInfo.get("version"),
                    "createdAt", agentInfo.get("createdAt"),
                    "capabilities", agentInfo.get("capabilities")
                ),
                "features", Map.of(
                    "taskProcessing", true,
                    "memoryManagement", true,
                    "metricsCollection", true,
                    "lifecycleManagement", true,
                    "errorHandling", true,
                    "threadIntegration", true
                ),
                "abstractions", Map.of(
                    "baseInterface", "Agent",
                    "taskInterface", "TaskAgent<TASK, RESULT>",
                    "aiInterface", "AiAgent<TASK, PROMPT, CHAT_RESPONSE, RESULT>",
                    "chatInterface", "ChatAgent<REQUEST, RESPONSE>"
                )
            ));
        } catch (Exception e) {
            logger.error("Failed to get agent capabilities", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}