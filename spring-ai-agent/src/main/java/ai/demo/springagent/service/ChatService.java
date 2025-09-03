package ai.demo.springagent.service;

import ai.demo.springagent.dto.ChatRequest;
import ai.demo.springagent.dto.ChatResponse;
import ai.demo.springagent.config.AiModelConfiguration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final AiModelConfiguration aiModelConfig;

    public ChatService(ChatClient chatClient, AiModelConfiguration aiModelConfig) {
        this.chatClient = chatClient;
        this.aiModelConfig = aiModelConfig;
    }

    public ChatResponse processChat(ChatRequest request) {
        String conversationText = request.getMessages().stream()
                .map(msg -> msg.getRole() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));

        var response = chatClient.prompt()
                .user(conversationText)
                .call()
                .content();
        String content = response;

        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setId("chatcmpl-" + UUID.randomUUID().toString().replace("-", ""));
        chatResponse.setModel(aiModelConfig.getModel());
        
        ChatResponse.Message responseMessage = new ChatResponse.Message("assistant", content);
        ChatResponse.Choice choice = new ChatResponse.Choice(0, responseMessage, "stop");
        chatResponse.setChoices(List.of(choice));
        
        ChatResponse.Usage usage = new ChatResponse.Usage(50, 100, 150);
        chatResponse.setUsage(usage);
        
        return chatResponse;
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