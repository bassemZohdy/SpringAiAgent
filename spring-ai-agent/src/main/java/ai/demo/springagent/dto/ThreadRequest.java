package ai.demo.springagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class ThreadRequest {
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("metadata")
    private Object metadata;
    
    public ThreadRequest() {}
    
    public ThreadRequest(String title, Object metadata) {
        this.title = title;
        this.metadata = metadata;
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
}