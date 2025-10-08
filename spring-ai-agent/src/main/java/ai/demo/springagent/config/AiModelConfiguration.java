package ai.demo.springagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiModelConfiguration {
    
    private String model = "gpt-5-nano"; // Default model
    private int maxHistoryTokens = 4096; // Approximate token cap for history
    private int charsPerToken = 4;       // Heuristic conversion factor
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }

    public int getMaxHistoryTokens() {
        return maxHistoryTokens;
    }

    public void setMaxHistoryTokens(int maxHistoryTokens) {
        this.maxHistoryTokens = maxHistoryTokens;
    }

    public int getCharsPerToken() {
        return charsPerToken;
    }

    public void setCharsPerToken(int charsPerToken) {
        this.charsPerToken = charsPerToken;
    }
}
