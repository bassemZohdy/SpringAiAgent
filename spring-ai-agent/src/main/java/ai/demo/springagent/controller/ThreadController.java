package ai.demo.springagent.controller;

import ai.demo.springagent.dto.ThreadRequest;
import ai.demo.springagent.dto.ThreadResponse;
import ai.demo.springagent.model.ThreadMessage;
import ai.demo.springagent.service.ThreadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1")
@CrossOrigin(origins = "*")
public class ThreadController {
    
    private final ThreadService threadService;
    
    public ThreadController(ThreadService threadService) {
        this.threadService = threadService;
    }
    
    @PostMapping("/threads")
    public ResponseEntity<ThreadResponse> createThread(@Valid @RequestBody ThreadRequest request) {
        ThreadResponse response = threadService.createThread(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/threads")
    public ResponseEntity<Map<String, Object>> getAllThreads() {
        List<ThreadResponse> threads = threadService.getAllThreads();
        Map<String, Object> response = new HashMap<>();
        response.put("object", "list");
        response.put("data", threads);
        response.put("has_more", false);
        response.put("first_id", threads.isEmpty() ? null : threads.get(0).getId());
        response.put("last_id", threads.isEmpty() ? null : threads.get(threads.size() - 1).getId());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/threads/{threadId}")
    public ResponseEntity<ThreadResponse> getThread(@PathVariable String threadId) {
        return threadService.getThread(threadId)
                .map(thread -> ResponseEntity.ok(thread))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/threads/{threadId}")
    public ResponseEntity<ThreadResponse> updateThread(
            @PathVariable String threadId, 
            @Valid @RequestBody ThreadRequest request) {
        return threadService.updateThread(threadId, request)
                .map(thread -> ResponseEntity.ok(thread))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/threads/{threadId}")
    public ResponseEntity<Map<String, Object>> deleteThread(@PathVariable String threadId) {
        boolean deleted = threadService.deleteThread(threadId);
        if (deleted) {
            Map<String, Object> response = new HashMap<>();
            response.put("id", threadId);
            response.put("object", "thread.deleted");
            response.put("deleted", true);
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/threads/{threadId}/messages")
    public ResponseEntity<ThreadMessage> addMessage(@PathVariable String threadId, @Valid @RequestBody Map<String, String> request) {
        if (!threadService.getThread(threadId).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        String role = request.get("role");
        String content = request.get("content");
        
        if (role == null || content == null) {
            return ResponseEntity.badRequest().build();
        }
        
        threadService.addMessageToThread(threadId, role, content);
        List<ThreadMessage> messages = threadService.getThreadMessages(threadId);
        ThreadMessage lastMessage = messages.get(messages.size() - 1);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(lastMessage);
    }
    
    @GetMapping("/threads/{threadId}/messages")
    public ResponseEntity<Map<String, Object>> getThreadMessages(@PathVariable String threadId) {
        if (!threadService.getThread(threadId).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        List<ThreadMessage> messages = threadService.getThreadMessages(threadId);
        Map<String, Object> response = new HashMap<>();
        response.put("object", "list");
        response.put("data", messages);
        response.put("has_more", false);
        response.put("first_id", messages.isEmpty() ? null : messages.get(0).getId());
        response.put("last_id", messages.isEmpty() ? null : messages.get(messages.size() - 1).getId());
        return ResponseEntity.ok(response);
    }
}