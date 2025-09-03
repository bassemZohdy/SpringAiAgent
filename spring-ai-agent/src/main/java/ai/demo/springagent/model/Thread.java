package ai.demo.springagent.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Thread {
    private String id;
    private String title;
    private Object metadata;
    private List<ThreadMessage> messages;
    private long createdAt;
    private long lastActivity;
    
    public Thread() {
        this.messages = new ArrayList<>();
        this.createdAt = Instant.now().getEpochSecond();
        this.lastActivity = this.createdAt;
    }
    
    public Thread(String id, String title, Object metadata) {
        this();
        this.id = id;
        this.title = title;
        this.metadata = metadata;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public Object getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }
    
    public List<ThreadMessage> getMessages() {
        return messages;
    }
    
    public void setMessages(List<ThreadMessage> messages) {
        this.messages = messages;
    }
    
    public void addMessage(ThreadMessage message) {
        this.messages.add(message);
        this.lastActivity = Instant.now().getEpochSecond();
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getLastActivity() {
        return lastActivity;
    }
    
    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }
    
    public int getMessageCount() {
        return messages.size();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Thread thread = (Thread) o;
        return Objects.equals(id, thread.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}