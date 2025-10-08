package ai.demo.springagent.controller;

import ai.demo.springagent.config.AiModelConfiguration;
import ai.demo.springagent.dto.ChatCompletionChunk;
import ai.demo.springagent.dto.ChatRequest;
import ai.demo.springagent.dto.ChatResponse;
import ai.demo.springagent.provider.OpenAIProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Disabled;

@SpringBootTest
@AutoConfigureMockMvc
@Disabled("SseEmitter + MockMvc async handling needs additional setup; pending dedicated test harness")
class ChatStreamingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class TestProviderConfig {
        @Bean
        @Primary
        OpenAIProvider fakeProvider(AiModelConfiguration cfg) {
            cfg.setModel("test-model");
            return new OpenAIProvider(null, cfg) {
                @Override
                public Mono<ChatResponse> complete(ChatRequest request) {
                    ChatResponse resp = new ChatResponse();
                    resp.setId("chatcmpl-" + UUID.randomUUID().toString().replace("-", ""));
                    resp.setModel(cfg.getModel());
                    ChatResponse.Message msg = new ChatResponse.Message("assistant", "Hello world!");
                    ChatResponse.Choice choice = new ChatResponse.Choice(0, msg, "stop");
                    resp.setChoices(List.of(choice));
                    ChatResponse.Usage usage = new ChatResponse.Usage(0,0,0);
                    resp.setUsage(usage);
                    return Mono.just(resp);
                }

                @Override
                public Flux<ChatCompletionChunk> stream(ChatRequest request) {
                    String id = "chatcmpl-test";
                    return Flux.just("Hello", " ", "world", "!")
                            .map(part -> {
                                ChatCompletionChunk chunk = new ChatCompletionChunk();
                                chunk.setId(id);
                                chunk.setModel(cfg.getModel());
                                ChatCompletionChunk.Delta delta = new ChatCompletionChunk.Delta(part);
                                ChatCompletionChunk.ChunkChoice choice = new ChatCompletionChunk.ChunkChoice(0, delta, null);
                                chunk.setChoices(List.of(choice));
                                return chunk;
                            })
                            .concatWith(Flux.defer(() -> {
                                ChatCompletionChunk finalChunk = new ChatCompletionChunk();
                                finalChunk.setId(id);
                                finalChunk.setModel(cfg.getModel());
                                ChatCompletionChunk.Delta delta = new ChatCompletionChunk.Delta();
                                ChatCompletionChunk.ChunkChoice choice = new ChatCompletionChunk.ChunkChoice(0, delta, "stop");
                                finalChunk.setChoices(List.of(choice));
                                return Flux.just(finalChunk);
                            }));
                }
            };
        }
    }

    @Test
    void streamsChunksOverSse() throws Exception {
        String body = "{\n" +
                "  \"model\": \"test-model\",\n" +
                "  \"messages\": [{\"role\":\"user\",\"content\":\"Hi\"}],\n" +
                "  \"stream\": true\n" +
                "}";

        var mvcResult = mockMvc.perform(
                        MockMvcRequestBuilders.post("/v1/chat/completions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Accept", "text/event-stream")
                                .content(body)
                )
                .andReturn();

        // SseEmitter responses are async in Spring MVC
        var result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andReturn();

        String contentStr = result.getResponse().getContentAsString();
        assertThat(contentStr).contains("data:");
        // Should include our streamed parts concatenated across chunks
        assertThat(contentStr).contains("Hello");
        assertThat(contentStr).contains("world");
    }
}
