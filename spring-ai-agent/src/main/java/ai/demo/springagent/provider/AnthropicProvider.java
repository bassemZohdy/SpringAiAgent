package ai.demo.springagent.provider;

import ai.demo.springagent.dto.ChatCompletionChunk;
import ai.demo.springagent.dto.ChatRequest;
import ai.demo.springagent.dto.ChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class AnthropicProvider implements LLMProvider {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String anthropicApiKey;

    public AnthropicProvider(@Value("${anthropic.api.key:}") String anthropicApiKey, ObjectMapper objectMapper) {
        this.anthropicApiKey = anthropicApiKey;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.anthropic.com/v1")
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("x-api-key", anthropicApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public Mono<ChatResponse> complete(ChatRequest request) {
        Map<String, Object> anthropicRequest = buildAnthropicRequest(request, false);
        
        return webClient.post()
                .uri("/messages")
                .body(BodyInserters.fromValue(anthropicRequest))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(30))
                .map(response -> convertToOpenAIResponse(response, request.getModel()))
                .onErrorMap(ex -> new RuntimeException("Anthropic API error: " + ex.getMessage(), ex));
    }

    @Override
    public Flux<ChatCompletionChunk> stream(ChatRequest request) {
        Map<String, Object> anthropicRequest = buildAnthropicRequest(request, true);
        String chatId = "chatcmpl-" + UUID.randomUUID().toString().replace("-", "");
        AtomicBoolean isFirst = new AtomicBoolean(true);

        return webClient.post()
                .uri("/messages")
                .body(BodyInserters.fromValue(anthropicRequest))
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(60))
                .flatMap(line -> parseSSELine(line))
                .filter(event -> "content_block_delta".equals(event.get("type")))
                .map(event -> {
                    String content = extractDeltaText(event);
                    
                    ChatCompletionChunk chunk = new ChatCompletionChunk();
                    chunk.setId(chatId);
                    chunk.setModel(request.getModel());
                    
                    ChatCompletionChunk.Delta delta;
                    if (isFirst.getAndSet(false)) {
                        delta = new ChatCompletionChunk.Delta("assistant", content);
                    } else {
                        delta = new ChatCompletionChunk.Delta(content);
                    }
                    
                    ChatCompletionChunk.ChunkChoice choice = new ChatCompletionChunk.ChunkChoice(0, delta, null);
                    chunk.setChoices(List.of(choice));
                    
                    return chunk;
                })
                .concatWith(Flux.just(createFinalChunk(chatId, request.getModel())));
    }

    private Map<String, Object> buildAnthropicRequest(ChatRequest request, boolean stream) {
        Map<String, Object> anthropicRequest = new HashMap<>();
        anthropicRequest.put("model", "claude-3-haiku-20240307");
        anthropicRequest.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 1000);
        
        if (request.getTemperature() != null) {
            anthropicRequest.put("temperature", request.getTemperature());
        }
        
        if (stream) {
            anthropicRequest.put("stream", true);
        }
        
        // Convert OpenAI messages to Anthropic format
        List<Map<String, String>> anthropicMessages = request.getMessages().stream()
                .filter(msg -> !"system".equals(msg.getRole()))
                .map(msg -> {
                    Map<String, String> message = new HashMap<>();
                    message.put("role", "user".equals(msg.getRole()) ? "user" : "assistant");
                    message.put("content", msg.getContent());
                    return message;
                })
                .toList();
        
        anthropicRequest.put("messages", anthropicMessages);
        
        return anthropicRequest;
    }

    private ChatResponse convertToOpenAIResponse(JsonNode anthropicResponse, String model) {
        ChatResponse response = new ChatResponse();
        response.setId("chatcmpl-" + UUID.randomUUID().toString().replace("-", ""));
        response.setModel(model);
        
        String content = "";
        if (anthropicResponse.has("content") && anthropicResponse.get("content").isArray() 
            && anthropicResponse.get("content").size() > 0) {
            content = anthropicResponse.get("content").get(0).get("text").asText();
        }
        
        ChatResponse.Message responseMessage = new ChatResponse.Message("assistant", content);
        ChatResponse.Choice choice = new ChatResponse.Choice(0, responseMessage, "stop");
        response.setChoices(List.of(choice));
        
        // Mock usage for simplicity
        ChatResponse.Usage usage = new ChatResponse.Usage(50, 100, 150);
        response.setUsage(usage);
        
        return response;
    }

    private Flux<Map<String, Object>> parseSSELine(String line) {
        if (line.startsWith("data: ") && !line.equals("data: [DONE]")) {
            try {
                String json = line.substring(6);
                JsonNode event = objectMapper.readTree(json);
                Map<String, Object> eventMap = objectMapper.convertValue(event, Map.class);
                return Flux.just(eventMap);
            } catch (Exception e) {
                return Flux.empty();
            }
        }
        return Flux.empty();
    }

    private String extractDeltaText(Map<String, Object> event) {
        try {
            Map<String, Object> delta = (Map<String, Object>) event.get("delta");
            if (delta != null && delta.containsKey("text")) {
                return delta.get("text").toString();
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return "";
    }

    private ChatCompletionChunk createFinalChunk(String chatId, String model) {
        ChatCompletionChunk finalChunk = new ChatCompletionChunk();
        finalChunk.setId(chatId);
        finalChunk.setModel(model);
        
        ChatCompletionChunk.Delta delta = new ChatCompletionChunk.Delta();
        ChatCompletionChunk.ChunkChoice choice = new ChatCompletionChunk.ChunkChoice(0, delta, "stop");
        finalChunk.setChoices(List.of(choice));
        
        return finalChunk;
    }
}