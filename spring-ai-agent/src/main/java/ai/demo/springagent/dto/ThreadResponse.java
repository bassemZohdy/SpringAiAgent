package ai.demo.springagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class ThreadResponse {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("object")
    private String object = "thread";
    
    @JsonProperty("created_at")
    private long createdAt;
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("metadata")
    private Object metadata;
    
    @JsonProperty("message_count")
    private int messageCount;
    
    @JsonProperty("last_activity")
    private long lastActivity;
    
    public ThreadResponse() {}
    
    public ThreadResponse(String id, String title, Object metadata) {
        this.id = id;
        this.title = title;
        this.metadata = metadata;
        this.createdAt = Instant.now().getEpochSecond();
        this.lastActivity = this.createdAt;
        this.messageCount = 0;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getObject() {
        return object;
    }
    
    public void setObject(String object) {
        this.object = object;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
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
    
    public int getMessageCount() {
        return messageCount;
    }
    
    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }
    
    public long getLastActivity() {
        return lastActivity;
    }
    
    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }
}