package ai.demo.springagent.provider;

import ai.demo.springagent.config.AiModelConfiguration;
import ai.demo.springagent.dto.ChatCompletionChunk;
import ai.demo.springagent.dto.ChatRequest;
import ai.demo.springagent.dto.ChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Component
public class OpenAIProvider implements LLMProvider {

    private final ChatClient chatClient;
    private final AiModelConfiguration aiModelConfig;

    public OpenAIProvider(ChatClient chatClient, AiModelConfiguration aiModelConfig) {
        this.chatClient = chatClient;
        this.aiModelConfig = aiModelConfig;
    }

    @Override
    public Mono<ChatResponse> complete(ChatRequest request) {
        String conversationText = request.getMessages().stream()
                .map(msg -> msg.getRole() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));

        return Mono.fromCallable(() -> {
            var response = chatClient.prompt()
                    .user(conversationText)
                    .call()
                    .content();

            ChatResponse chatResponse = new ChatResponse();
            chatResponse.setId("chatcmpl-" + UUID.randomUUID().toString().replace("-", ""));
            chatResponse.setModel(aiModelConfig.getModel());
            
            ChatResponse.Message responseMessage = new ChatResponse.Message("assistant", response);
            ChatResponse.Choice choice = new ChatResponse.Choice(0, responseMessage, "stop");
            chatResponse.setChoices(List.of(choice));
            
            ChatResponse.Usage usage = new ChatResponse.Usage(50, 100, 150);
            chatResponse.setUsage(usage);
            
            return chatResponse;
        });
    }

    @Override
    public Flux<ChatCompletionChunk> stream(ChatRequest request) {
        String conversationText = request.getMessages().stream()
                .map(msg -> msg.getRole() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));

        String chatId = "chatcmpl-" + UUID.randomUUID().toString().replace("-", "");
        AtomicBoolean isFirst = new AtomicBoolean(true);

        return chatClient.prompt()
                .user(conversationText)
                .stream()
                .content()
                .map(content -> {
                    ChatCompletionChunk chunk = new ChatCompletionChunk();
                    chunk.setId(chatId);
                    chunk.setModel(aiModelConfig.getModel());
                    
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
                .concatWith(Flux.just(createFinalChunk(chatId)));
    }

    private ChatCompletionChunk createFinalChunk(String chatId) {
        ChatCompletionChunk finalChunk = new ChatCompletionChunk();
        finalChunk.setId(chatId);
        finalChunk.setModel(aiModelConfig.getModel());
        
        ChatCompletionChunk.Delta delta = new ChatCompletionChunk.Delta();
        ChatCompletionChunk.ChunkChoice choice = new ChatCompletionChunk.ChunkChoice(0, delta, "stop");
        finalChunk.setChoices(List.of(choice));
        
        return finalChunk;
    }
}