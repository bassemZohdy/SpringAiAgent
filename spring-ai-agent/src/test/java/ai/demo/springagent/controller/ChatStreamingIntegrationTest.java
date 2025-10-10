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
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
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
                        post("/v1/chat/completions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Accept", "text/event-stream")
                                .content(body)
                )
                .andExpect(request().asyncStarted())
                .andReturn();

        // SseEmitter responses are async in Spring MVC
        var result = mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/event-stream;charset=UTF-8"))
                .andReturn();

        String contentStr = result.getResponse().getContentAsString();
        assertThat(contentStr).contains("data:");
        // Should include our streamed parts across chunks
        assertThat(contentStr).contains("Hello");
        assertThat(contentStr).contains("world");
        // Verify SSE format
        assertThat(contentStr).contains("data:");
        assertThat(contentStr).contains("data: {");
    }

    @Test
    void nonStreamingRequestReturnsJsonResponse() throws Exception {
        String body = "{\n" +
                "  \"model\": \"test-model\",\n" +
                "  \"messages\": [{\"role\":\"user\",\"content\":\"Hi\"}],\n" +
                "  \"stream\": false\n" +
                "}";

        mockMvc.perform(post("/v1/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.choices[0].message.content").value("Hello world!"))
                .andExpect(jsonPath("$.model").value("test-model"));
    }
}
