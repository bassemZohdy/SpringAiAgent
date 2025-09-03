package ai.demo.springagent.service;

import ai.demo.springagent.dto.ThreadRequest;
import ai.demo.springagent.dto.ThreadResponse;
import ai.demo.springagent.model.Thread;
import ai.demo.springagent.model.ThreadMessage;
import ai.demo.springagent.repository.ThreadRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ThreadService {
    
    private final ThreadRepository threadRepository;
    
    public ThreadService(ThreadRepository threadRepository) {
        this.threadRepository = threadRepository;
    }
    
    public ThreadResponse createThread(ThreadRequest request) {
        String threadId = "thread_" + UUID.randomUUID().toString().replace("-", "");
        String title = request.getTitle() != null ? request.getTitle() : "New Chat";
        
        Thread thread = new Thread(threadId, title, request.getMetadata());
        threadRepository.save(thread);
        
        return convertToResponse(thread);
    }
    
    public Optional<ThreadResponse> getThread(String threadId) {
        return threadRepository.findById(threadId)
                .map(this::convertToResponse);
    }
    
    public List<ThreadResponse> getAllThreads() {
        return threadRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    public Optional<ThreadResponse> updateThread(String threadId, ThreadRequest request) {
        return threadRepository.findById(threadId)
                .map(thread -> {
                    if (request.getTitle() != null) {
                        thread.setTitle(request.getTitle());
                    }
                    if (request.getMetadata() != null) {
                        thread.setMetadata(request.getMetadata());
                    }
                    threadRepository.save(thread);
                    return convertToResponse(thread);
                });
    }
    
    public boolean deleteThread(String threadId) {
        if (threadRepository.existsById(threadId)) {
            threadRepository.deleteById(threadId);
            return true;
        }
        return false;
    }
    
    public void addMessageToThread(String threadId, String role, String content) {
        String messageId = "msg_" + UUID.randomUUID().toString().replace("-", "");
        ThreadMessage message = new ThreadMessage(messageId, threadId, role, content);
        threadRepository.saveMessage(message);
    }
    
    public List<ThreadMessage> getThreadMessages(String threadId) {
        return threadRepository.findMessagesByThreadId(threadId);
    }
    
    private ThreadResponse convertToResponse(Thread thread) {
        ThreadResponse response = new ThreadResponse(thread.getId(), thread.getTitle(), thread.getMetadata());
        response.setCreatedAt(thread.getCreatedAt());
        response.setLastActivity(thread.getLastActivity());
        response.setMessageCount(thread.getMessageCount());
        return response;
    }
}