package ai.demo.springagent.service;

import ai.demo.springagent.dto.ChatCompletionChunk;
import ai.demo.springagent.dto.ChatRequest;
import ai.demo.springagent.dto.ChatResponse;
import ai.demo.springagent.config.AiModelConfiguration;
import ai.demo.springagent.model.ThreadMessage;
import ai.demo.springagent.provider.LLMProvider;
import ai.demo.springagent.provider.OpenAIProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    
    private final ChatClient chatClient;
    private final AiModelConfiguration aiModelConfig;
    private final ThreadService threadService;
    private final OpenAIProvider openAIProvider;
    private final SessionMappingService sessionMappingService;

    public ChatService(ChatClient chatClient, AiModelConfiguration aiModelConfig, ThreadService threadService,
                      OpenAIProvider openAIProvider, SessionMappingService sessionMappingService) {
        this.chatClient = chatClient;
        this.aiModelConfig = aiModelConfig;
        this.threadService = threadService;
        this.openAIProvider = openAIProvider;
        this.sessionMappingService = sessionMappingService;
    }

    public ChatResponse processChat(ChatRequest request, String provider) {
        logger.debug("Processing chat request - provider: {}, model: {}, threadId: {}", 
                    provider, request.getModel(), request.getThreadId());
        
        long startTime = System.currentTimeMillis();
        ChatRequest processedRequest = processThreadHistory(request);
        LLMProvider llmProvider = getProvider(provider);
        
        ChatResponse response = llmProvider.complete(processedRequest).block();
        saveAssistantResponse(request.getThreadId(), response);
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Chat completion successful - provider: {}, model: {}, duration: {}ms", 
                   provider, request.getModel(), duration);
        
        return response;
    }

    public ChatResponse processChatWithMemoryAdvisor(ChatRequest request, String provider) {
        logger.debug("Processing chat request with memory advisor - provider: {}, model: {}, threadId: {}", 
                    provider, request.getModel(), request.getThreadId());
        
        long startTime = System.currentTimeMillis();
        
        // Map thread ID to session ID for model-level session management
        String sessionId = sessionMappingService.getOrCreateSessionId(request.getThreadId());
        String conversationId = request.getThreadId() != null ? request.getThreadId() : sessionId;
        
        // Set the session ID in the request for downstream processing
        request.setSessionId(sessionId);
        
        // Get the user message from the last message in the request
        String userMessage = "";
        if (!request.getMessages().isEmpty()) {
            ChatRequest.Message lastMessage = request.getMessages().get(request.getMessages().size() - 1);
            if ("user".equals(lastMessage.getRole())) {
                userMessage = lastMessage.getContent();
            }
        }
        
        // Use ChatClient with memory advisor and session mapping
        String response = chatClient.prompt()
                .user(userMessage)
                // Use conversation scoping for memory advisor; compatible with Spring AI M4
                .advisors(a -> a.param("conversationId", conversationId))
                .call()
                .content();
        
        // Create ChatResponse in OpenAI format
        ChatResponse chatResponse = createChatResponse(response, request.getModel());
        
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Chat completion with memory advisor successful - provider: {}, model: {}, threadId: {}, sessionId: {}, conversationId: {}, duration: {}ms", 
                   provider, request.getModel(), request.getThreadId(), sessionId, conversationId, duration);
        
        return chatResponse;
    }
    
    @Async
    public void streamChatAsync(ChatRequest request, String provider, SseEmitter emitter) {
        logger.debug("Starting streaming chat - provider: {}, model: {}, threadId: {}", 
                    provider, request.getModel(), request.getThreadId());
        
        long startTime = System.currentTimeMillis();
        ChatRequest processedRequest = processThreadHistory(request);
        LLMProvider llmProvider = getProvider(provider);
        
        StringBuilder fullResponse = new StringBuilder();
        ObjectMapper objectMapper = new ObjectMapper();
        
        llmProvider.stream(processedRequest)
                .doOnNext(chunk -> {
                    try {
                        String data = "data: " + objectMapper.writeValueAsString(chunk) + "\n\n";
                        emitter.send(data);
                        
                        if (chunk.getChoices() != null && !chunk.getChoices().isEmpty() &&
                            chunk.getChoices().get(0).getDelta() != null &&
                            chunk.getChoices().get(0).getDelta().getContent() != null) {
                            fullResponse.append(chunk.getChoices().get(0).getDelta().getContent());
                        }
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                })
                .doOnComplete(() -> {
                    try {
                        emitter.send("data: [DONE]\n\n");
                        emitter.complete();
                        
                        if (request.getThreadId() != null && fullResponse.length() > 0) {
                            saveAssistantMessage(request.getThreadId(), fullResponse.toString());
                        }
                        
                        long duration = System.currentTimeMillis() - startTime;
                        logger.info("Streaming chat completed - provider: {}, model: {}, duration: {}ms, chars: {}", 
                                   provider, request.getModel(), duration, fullResponse.length());
                    } catch (IOException e) {
                        logger.error("Error completing streaming chat", e);
                        emitter.completeWithError(e);
                    }
                })
                .doOnError(emitter::completeWithError)
                .subscribe();
    }
    
    private ChatRequest processThreadHistory(ChatRequest request) {
        String threadId = request.getThreadId();
        List<ChatRequest.Message> messages = request.getMessages();
        
        if (threadId != null && threadService.getThread(threadId).isPresent()) {
            // Save user message to thread
            if (!messages.isEmpty()) {
                ChatRequest.Message lastMessage = messages.get(messages.size() - 1);
                if ("user".equals(lastMessage.getRole())) {
                    threadService.addMessageToThread(threadId, lastMessage.getRole(), lastMessage.getContent());
                }
            }
            
            // Get complete thread history for better context
            List<ThreadMessage> threadHistory = threadService.getThreadMessages(threadId);
            if (!threadHistory.isEmpty()) {
                messages = threadHistory.stream()
                        .map(tm -> new ChatRequest.Message(tm.getRole(), tm.getContent()))
                        .collect(Collectors.toList());

                // Apply approximate token-based truncation to fit within budget
                messages = truncateByTokenBudget(messages, aiModelConfig.getMaxHistoryTokens(), aiModelConfig.getCharsPerToken());
            }
        }
        
        // Create new request with processed messages
        ChatRequest processedRequest = new ChatRequest();
        processedRequest.setModel(request.getModel());
        processedRequest.setMessages(messages);
        processedRequest.setTemperature(request.getTemperature());
        processedRequest.setMaxTokens(request.getMaxTokens());
        processedRequest.setStream(request.isStream());
        processedRequest.setThreadId(request.getThreadId());
        
        return processedRequest;
    }

    private List<ChatRequest.Message> truncateByTokenBudget(List<ChatRequest.Message> messages, int maxTokens, int charsPerToken) {
        if (messages == null || messages.isEmpty()) return messages;
        int budgetChars = Math.max(1, maxTokens) * Math.max(1, charsPerToken);

        int totalChars = 0;
        for (ChatRequest.Message m : messages) {
            totalChars += (m.getContent() != null ? m.getContent().length() : 0);
        }
        if (totalChars <= budgetChars) return messages;

        // Keep latest messages within budget; always keep last user message if possible
        int running = 0;
        java.util.LinkedList<ChatRequest.Message> deque = new java.util.LinkedList<>();
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatRequest.Message m = messages.get(i);
            int len = (m.getContent() != null ? m.getContent().length() : 0);
            if (running + len > budgetChars && !deque.isEmpty()) {
                break;
            }
            deque.addFirst(m);
            running += len;
        }
        return List.copyOf(deque);
    }
    
    private LLMProvider getProvider(String provider) {
        return openAIProvider;
    }
    
    private void saveAssistantResponse(String threadId, ChatResponse response) {
        if (threadId != null && threadService.getThread(threadId).isPresent() &&
            response.getChoices() != null && !response.getChoices().isEmpty()) {
            String content = response.getChoices().get(0).getMessage().getContent();
            threadService.addMessageToThread(threadId, "assistant", content);
        }
    }
    
    private void saveAssistantMessage(String threadId, String content) {
        if (threadId != null && threadService.getThread(threadId).isPresent()) {
            threadService.addMessageToThread(threadId, "assistant", content);
        }
    }

    private ChatResponse createChatResponse(String content, String model) {
        ChatResponse response = new ChatResponse();
        response.setId("chatcmpl-" + UUID.randomUUID().toString().replace("-", ""));
        response.setObject("chat.completion");
        response.setCreated(System.currentTimeMillis() / 1000);
        response.setModel(model);
        
        ChatResponse.Choice choice = new ChatResponse.Choice();
        choice.setIndex(0);
        choice.setFinishReason("stop");
        
        ChatResponse.Message message = new ChatResponse.Message();
        message.setRole("assistant");
        message.setContent(content);
        choice.setMessage(message);
        
        response.setChoices(List.of(choice));
        
        ChatResponse.Usage usage = new ChatResponse.Usage();
        usage.setPromptTokens(0); // Would need token counting implementation
        usage.setCompletionTokens(0);
        usage.setTotalTokens(0);
        response.setUsage(usage);
        
        return response;
    }

    public Map<String, Object> getAvailableModels() {
        return Map.of(
            "object", "list",
            "data", List.of(
                Map.of(
                    "id", aiModelConfig.getModel(),
                    "object", "model",
                    "owned_by", "spring-ai-agent"
                )
            )
        );
    }

    public Map<String, Object> getSessionStatistics() {
        return Map.of(
            "sessionMappings", sessionMappingService.getAllMappings(),
            "totalMappings", sessionMappingService.getAllMappings().size(),
            "statistics", sessionMappingService.getStatistics()
        );
    }
}
