package ai.demo.springagent.model;

import java.time.Instant;
import java.util.Objects;

public class ThreadMessage {
    private String id;
    private String threadId;
    private String role;
    private String content;
    private long createdAt;
    private Object metadata;
    
    public ThreadMessage() {
        this.createdAt = Instant.now().getEpochSecond();
    }
    
    public ThreadMessage(String id, String threadId, String role, String content) {
        this();
        this.id = id;
        this.threadId = threadId;
        this.role = role;
        this.content = content;
    }
    
    public ThreadMessage(String id, String threadId, String role, String content, Object metadata) {
        this(id, threadId, role, content);
        this.metadata = metadata;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getThreadId() {
        return threadId;
    }
    
    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public Object getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThreadMessage that = (ThreadMessage) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}