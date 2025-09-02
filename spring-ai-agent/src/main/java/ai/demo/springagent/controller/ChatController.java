package ai.demo.springagent.controller;

import ai.demo.springagent.dto.ChatRequest;
import ai.demo.springagent.dto.ChatResponse;
import ai.demo.springagent.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

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
    public ResponseEntity<ChatResponse> chatCompletions(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = chatService.processChat(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/models")
    public ResponseEntity<?> getModels() {
        return ResponseEntity.ok(chatService.getAvailableModels());
    }
}