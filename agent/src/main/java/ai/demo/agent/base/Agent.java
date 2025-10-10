package ai.demo.agent.base;

import java.util.List;

/**
 * Base interface for all agents regardless of interaction style.
 * Provides identity, lifecycle, configuration, metrics, and memory access.
 */
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

    void start() throws AgentException;
    void pause() throws AgentException;
    void stop() throws AgentException;
    void reset() throws AgentException;

    AgentMetrics getMetrics();
    AgentMemory getMemory();
    void clearMemory();
    void compactMemory();
}

