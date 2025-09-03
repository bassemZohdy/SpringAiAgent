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

    public ChatService(ChatClient chatClient, AiModelConfiguration aiModelConfig, ThreadService threadService,
                      OpenAIProvider openAIProvider) {
        this.chatClient = chatClient;
        this.aiModelConfig = aiModelConfig;
        this.threadService = threadService;
        this.openAIProvider = openAIProvider;
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
}