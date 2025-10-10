package ai.demo.agent.base;

import ai.demo.agent.base.task.Task;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Base interface for agents with identity, state management, configuration, and lifecycle support.
 */
public interface Agent<TASK extends Task, RESULT> {
    
    CompletableFuture<RESULT> process(TASK task);
    
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
    
    default void onTaskStarted(TASK task) {
    }
    
    default void onTaskCompleted(TASK task, RESULT result) {
    }
    
    default void onTaskFailed(TASK task, Throwable error) {
    }
}

