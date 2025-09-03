package ai.demo.springagent.controller;

import ai.demo.springagent.dto.ChatCompletionChunk;
import ai.demo.springagent.dto.ChatRequest;
import ai.demo.springagent.dto.ChatResponse;
import ai.demo.springagent.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import java.io.IOException;

@RestController
@RequestMapping("/v1")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat/completions")
    public ResponseEntity<?> chatCompletions(
            @Valid @RequestBody ChatRequest request,
            @RequestHeader(value = "X-LLM-Provider", defaultValue = "openai") String provider) {
        
        if (request.isStream()) {
            SseEmitter emitter = new SseEmitter();
            chatService.streamChatAsync(request, provider, emitter);
            return ResponseEntity.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(emitter);
        } else {
            ChatResponse response = chatService.processChat(request, provider);
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/models")
    public ResponseEntity<?> getModels() {
        return ResponseEntity.ok(chatService.getAvailableModels());
    }
}