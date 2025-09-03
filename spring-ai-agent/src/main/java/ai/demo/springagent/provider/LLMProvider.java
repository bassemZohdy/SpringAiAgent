package ai.demo.springagent.provider;

import ai.demo.springagent.dto.ChatRequest;
import ai.demo.springagent.dto.ChatResponse;
import ai.demo.springagent.dto.ChatCompletionChunk;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LLMProvider {
    Mono<ChatResponse> complete(ChatRequest request);
    Flux<ChatCompletionChunk> stream(ChatRequest request);
}