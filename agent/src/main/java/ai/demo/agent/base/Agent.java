package ai.demo.agent.base;

import ai.demo.agent.metrics.AgentMetrics;
import java.time.Instant;
import java.util.List;

public interface Agent {

    String getAgentId();
    String getAgentName();
    String getVersion();
    String getInstructions();
    List<String> getCapabilities();
    AgentConfiguration getConfiguration();
    AgentState getState();
    boolean isRunning();
    boolean isPaused();
    Instant getCreatedAt();

    void start() throws AgentException;
    void pause() throws AgentException;
    void stop() throws AgentException;
    void reset() throws AgentException;

    AgentMetrics getMetrics();
    AgentMemory getMemory();
    void clearMemory();
    void compactMemory();
}

