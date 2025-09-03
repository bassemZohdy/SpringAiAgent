package ai.demo.springagent.dto;

import java.time.Instant;
import java.util.List;

public class ChatCompletionChunk {
    
    private String id;
    private String object = "chat.completion.chunk";
    private long created;
    private String model;
    private List<ChunkChoice> choices;

    public static class ChunkChoice {
        private int index;
        private Delta delta;
        private String finishReason;

        public ChunkChoice() {}

        public ChunkChoice(int index, Delta delta, String finishReason) {
            this.index = index;
            this.delta = delta;
            this.finishReason = finishReason;
        }

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }
        public Delta getDelta() { return delta; }
        public void setDelta(Delta delta) { this.delta = delta; }
        public String getFinishReason() { return finishReason; }
        public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    }

    public static class Delta {
        private String role;
        private String content;

        public Delta() {}

        public Delta(String content) {
            this.content = content;
        }

        public Delta(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public ChatCompletionChunk() {
        this.created = Instant.now().getEpochSecond();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getObject() { return object; }
    public void setObject(String object) { this.object = object; }
    public long getCreated() { return created; }
    public void setCreated(long created) { this.created = created; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<ChunkChoice> getChoices() { return choices; }
    public void setChoices(List<ChunkChoice> choices) { this.choices = choices; }
}