package ai.demo.springagent.service;

import ai.demo.springagent.dto.ChatRequest;
import ai.demo.springagent.dto.ChatResponse;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public ChatResponse processChat(ChatRequest request) {
        String conversationText = request.getMessages().stream()
                .map(msg -> msg.getRole() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));

        UserMessage userMessage = new UserMessage(conversationText);
        Prompt prompt = new Prompt(List.of(userMessage));
        
        var response = chatClient.call(prompt);
        String content = response.getResult().getOutput().getContent();

        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setId("chatcmpl-" + UUID.randomUUID().toString().replace("-", ""));
        chatResponse.setModel(request.getModel());
        
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
                    "id", "gpt-3.5-turbo",
                    "object", "model",
                    "owned_by", "spring-ai-agent"
                )
            )
        );
    }
}