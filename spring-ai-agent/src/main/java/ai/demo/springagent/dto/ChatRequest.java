package ai.demo.springagent.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public class ChatRequest {
    
    @NotBlank
    private String model = "gpt-3.5-turbo";
    
    @NotEmpty
    @NotEmpty
    private List<@Valid Message> messages;
    
    private String threadId;
    private String sessionId; // Model-level session ID for conversation continuity
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "2.0")
    private Double temperature = 0.7;

    @Min(1)
    private Integer maxTokens;
    private boolean stream = false;

    public static class Message {
        @NotBlank
        @Pattern(regexp = "^(user|assistant|system)$", message = "role must be user, assistant, or system")
        private String role;

        @NotBlank
        private String content;

        public Message() {}

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public boolean isStream() { return stream; }
    public void setStream(boolean stream) { this.stream = stream; }
}
