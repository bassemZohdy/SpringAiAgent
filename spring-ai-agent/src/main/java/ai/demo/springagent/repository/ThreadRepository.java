package ai.demo.springagent.repository;

import ai.demo.springagent.model.Thread;
import ai.demo.springagent.model.ThreadMessage;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class ThreadRepository {
    
    private final Map<String, Thread> threads = new ConcurrentHashMap<>();
    private final Map<String, List<ThreadMessage>> threadMessages = new ConcurrentHashMap<>();
    
    public Thread save(Thread thread) {
        threads.put(thread.getId(), thread);
        threadMessages.putIfAbsent(thread.getId(), new ArrayList<>());
        return thread;
    }
    
    public Optional<Thread> findById(String id) {
        Thread thread = threads.get(id);
        if (thread != null) {
            List<ThreadMessage> messages = threadMessages.get(id);
            if (messages != null) {
                thread.setMessages(new ArrayList<>(messages));
            }
        }
        return Optional.ofNullable(thread);
    }
    
    public List<Thread> findAll() {
        return threads.values().stream()
                .map(thread -> {
                    Thread threadCopy = new Thread(thread.getId(), thread.getTitle(), thread.getMetadata());
                    threadCopy.setCreatedAt(thread.getCreatedAt());
                    threadCopy.setLastActivity(thread.getLastActivity());
                    List<ThreadMessage> messages = threadMessages.get(thread.getId());
                    if (messages != null) {
                        threadCopy.setMessages(new ArrayList<>(messages));
                    }
                    return threadCopy;
                })
                .sorted((t1, t2) -> Long.compare(t2.getLastActivity(), t1.getLastActivity()))
                .collect(Collectors.toList());
    }
    
    public void deleteById(String id) {
        threads.remove(id);
        threadMessages.remove(id);
    }
    
    public void saveMessage(ThreadMessage message) {
        threadMessages.computeIfAbsent(message.getThreadId(), k -> new ArrayList<>()).add(message);
        
        Thread thread = threads.get(message.getThreadId());
        if (thread != null) {
            thread.setLastActivity(message.getCreatedAt());
        }
    }
    
    public List<ThreadMessage> findMessagesByThreadId(String threadId) {
        return threadMessages.getOrDefault(threadId, new ArrayList<>());
    }
    
    public void deleteMessagesByThreadId(String threadId) {
        threadMessages.remove(threadId);
    }
    
    public boolean existsById(String id) {
        return threads.containsKey(id);
    }
    
    public long count() {
        return threads.size();
    }
}