package ai.demo.agent.base;

import java.time.Instant;
import java.util.List;

/**
 * Base interface for all agents regardless of interaction style.
 * Provides identity, lifecycle, configuration, metrics, and memory access.
 *
 * <p>This interface defines the common contract that all agents must follow,
 * including essential metadata, state management, and operational capabilities.
 *
 * <p>Agents should implement more specific interfaces like {@link TaskAgent}
 * or {@link ChatAgent} for their intended use case.
 */
public interface Agent {

    /**
     * @return unique identifier for this agent instance
     */
    String getAgentId();

    /**
     * @return human-readable name for this agent
     */
    String getAgentName();

    /**
     * @return version of this agent (semantic versioning recommended)
     */
    String getVersion();

    /**
     * @return operational instructions or system prompt for this agent
     */
    String getInstructions();

    /**
     * @return immutable list of capabilities this agent supports
     */
    List<String> getCapabilities();

    /**
     * @return configuration for this agent
     */
    AgentConfiguration getConfiguration();

    /**
     * @return current lifecycle state of this agent
     */
    AgentState getState();

    /**
     * @return true if agent is currently running and accepting work
     */
    boolean isRunning();

    /**
     * @return true if agent is currently paused
     */
    boolean isPaused();

    /**
     * @return timestamp when agent was created
     */
    Instant getCreatedAt();

    /**
     * Start the agent, making it ready to accept work.
     *
     * @throws AgentException if agent cannot be started from current state
     */
    void start() throws AgentException;

    /**
     * Pause the agent, temporarily stopping work processing.
     *
     * @throws AgentException if agent cannot be paused from current state
     */
    void pause() throws AgentException;

    /**
     * Stop the agent, terminating all work processing.
     *
     * @throws AgentException if agent cannot be stopped from current state
     */
    void stop() throws AgentException;

    /**
     * Reset the agent to initial state, clearing metrics and memory.
     *
     * @throws AgentException if agent cannot be reset from current state
     */
    void reset() throws AgentException;

    /**
     * @return metrics about agent performance and operations
     */
    AgentMetrics getMetrics();

    /**
     * @return memory system containing execution history and learnings
     */
    AgentMemory getMemory();

    /**
     * Clear all memory entries, removing execution history.
     */
    void clearMemory();

    /**
     * Compact memory by summarizing older entries and retaining recent ones.
     */
    void compactMemory();
}

