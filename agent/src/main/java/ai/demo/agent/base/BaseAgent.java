package ai.demo.agent.base;

import ai.demo.agent.base.task.Task;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract base implementation of the Agent interface providing common functionality
 * including lifecycle management, metrics collection, and single-threaded task processing.
 * 
 * @param <TASK> The type of task/input the agent processes (must extend Task)
 * @param <RESULT> The type of result/output the agent produces
 */
public abstract class BaseAgent<TASK extends Task, RESULT> implements Agent<TASK, RESULT> {
    
    // Core identity and configuration
    private final String agentId;
    private final String agentName;
    private final String version;
    private final AgentConfiguration configuration;
    private final List<String> capabilities;
    
    // State management
    private final AtomicReference<AgentState> state = new AtomicReference<>(AgentState.CREATED);
    private final Object stateLock = new Object();
    
    // Execution infrastructure
    private volatile ExecutorService executor;
    private final AgentMetrics metrics;
    
    // Memory system
    private final AgentMemory memory;
    
    /**
     * Construct a new base agent with the specified configuration.
     * 
     * @param agentName the human-readable name for this agent
     * @param version the version of this agent
     * @param configuration the agent configuration
     * @param capabilities the list of capabilities this agent supports
     */
    protected BaseAgent(String agentName, String version, AgentConfiguration configuration, 
                       List<String> capabilities) {
        this.agentId = UUID.randomUUID().toString();
        this.agentName = Objects.requireNonNull(agentName, "Agent name cannot be null");
        this.version = Objects.requireNonNull(version, "Version cannot be null");
        this.configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");
        this.capabilities = new ArrayList<>(Objects.requireNonNull(capabilities, "Capabilities cannot be null"));
        this.metrics = new AgentMetrics();
        this.memory = new AgentMemory();
    }
    
    /**
     * Construct a new base agent with default configuration.
     * 
     * @param agentName the human-readable name for this agent
     * @param version the version of this agent
     * @param capabilities the list of capabilities this agent supports
     */
    protected BaseAgent(String agentName, String version, List<String> capabilities) {
        this(agentName, version, AgentConfiguration.defaultConfiguration(), capabilities);
    }
    
    // === Core Processing ===
    
    @Override
    public CompletableFuture<RESULT> process(TASK task) {
        if (!isRunning()) {
            return CompletableFuture.failedFuture(
                new AgentException("Agent is not running", agentId, state.get()));
        }
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.nanoTime();
            metrics.recordTaskStarted();
            onTaskStarted(task);
            
            try {
                RESULT result = doProcess(task);
                long processingTime = System.nanoTime() - startTime;
                metrics.recordTaskSucceeded(processingTime);
                
                // Record successful execution in memory
                memory.recordExecution(task, result, true, processingTime, null);
                
                onTaskCompleted(task, result);
                return result;
                
            } catch (Exception e) {
                long processingTime = System.nanoTime() - startTime;
                metrics.recordTaskFailed(processingTime);
                
                // Record failed execution in memory with error details
                String learnings = "Error: " + e.getClass().getSimpleName() + 
                                 (e.getMessage() != null ? " - " + e.getMessage() : "");
                memory.recordExecution(task, null, false, processingTime, learnings);
                
                onTaskFailed(task, e);
                throw new RuntimeException("Task processing failed", e);
            }
        }, executor);
    }
    
    /**
     * Abstract method that subclasses must implement to define their task processing logic.
     * This method is called from within the single-threaded executor.
     * 
     * @param task the task to process
     * @return the result of processing the task
     * @throws Exception if processing fails
     */
    protected abstract RESULT doProcess(TASK task) throws Exception;
    
    // === Agent Identity ===
    
    @Override
    public String getAgentId() {
        return agentId;
    }
    
    @Override
    public String getAgentName() {
        return agentName;
    }
    
    @Override
    public String getVersion() {
        return version;
    }
    
    // === Configuration ===
    
    @Override
    public String getInstructions() {
        return configuration.getInstructions();
    }
    
    @Override
    public void setInstructions(String instructions) {
        throw new UnsupportedOperationException("Instructions are immutable and set via configuration. Create a new agent with updated configuration.");
    }
    
    @Override
    public List<String> getCapabilities() {
        return new ArrayList<>(capabilities);
    }
    
    @Override
    public AgentConfiguration getConfiguration() {
        return configuration;
    }
    
    // === State Management ===
    
    @Override
    public AgentState getState() {
        return state.get();
    }
    
    @Override
    public boolean isRunning() {
        return state.get() == AgentState.STARTED;
    }
    
    @Override
    public boolean isPaused() {
        return state.get() == AgentState.PAUSED;
    }
    
    // === Lifecycle Management ===
    
    @Override
    public void start() throws AgentException {
        synchronized (stateLock) {
            AgentState currentState = state.get();
            AgentState targetState = AgentState.STARTED;
            
            if (!currentState.canTransitionTo(AgentState.STARTING)) {
                throw new AgentException(
                    "Cannot start agent from current state", 
                    agentId, currentState, targetState);
            }
            
            try {
                setState(AgentState.STARTING);
                doStart();
                setState(targetState);
                
            } catch (Exception e) {
                setState(AgentState.ERROR);
                throw new AgentException("Failed to start agent", e, agentId, currentState);
            }
        }
    }
    
    @Override
    public void pause() throws AgentException {
        synchronized (stateLock) {
            AgentState currentState = state.get();
            AgentState targetState = AgentState.PAUSED;
            
            if (!currentState.canTransitionTo(AgentState.PAUSING)) {
                throw new AgentException(
                    "Cannot pause agent from current state", 
                    agentId, currentState, targetState);
            }
            
            try {
                setState(AgentState.PAUSING);
                doPause();
                setState(targetState);
                
            } catch (Exception e) {
                setState(AgentState.ERROR);
                throw new AgentException("Failed to pause agent", e, agentId, currentState);
            }
        }
    }
    
    @Override
    public void stop() throws AgentException {
        synchronized (stateLock) {
            AgentState currentState = state.get();
            AgentState targetState = AgentState.STOPPED;
            
            if (!currentState.canTransitionTo(AgentState.STOPPING)) {
                throw new AgentException(
                    "Cannot stop agent from current state", 
                    agentId, currentState, targetState);
            }
            
            try {
                setState(AgentState.STOPPING);
                doStop();
                setState(targetState);
                
            } catch (Exception e) {
                setState(AgentState.ERROR);
                throw new AgentException("Failed to stop agent", e, agentId, currentState);
            }
        }
    }
    
    @Override
    public void reset() throws AgentException {
        synchronized (stateLock) {
            AgentState currentState = state.get();
            AgentState targetState = AgentState.CREATED;
            
            if (!currentState.canTransitionTo(AgentState.RESETTING)) {
                throw new AgentException(
                    "Cannot reset agent from current state", 
                    agentId, currentState, targetState);
            }
            
            try {
                setState(AgentState.RESETTING);
                doReset();
                setState(targetState);
                
            } catch (Exception e) {
                setState(AgentState.ERROR);
                throw new AgentException("Failed to reset agent", e, agentId, currentState);
            }
        }
    }
    
    private void setState(AgentState newState) {
        AgentState oldState = state.getAndSet(newState);
        onStateChanged(oldState, newState);
    }
    
    // === Lifecycle Implementation Methods ===
    
    /**
     * Perform agent startup logic. Default implementation creates the executor.
     * Subclasses can override for custom startup behavior.
     * 
     * @throws Exception if startup fails
     */
    protected void doStart() throws Exception {
        if (executor == null || executor.isShutdown()) {
            executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "Agent-" + agentName + "-" + agentId.substring(0, 8));
                t.setDaemon(true);
                return t;
            });
        }
    }
    
    /**
     * Perform agent pause logic. Default implementation does nothing.
     * Subclasses can override for custom pause behavior.
     * 
     * @throws Exception if pause fails
     */
    protected void doPause() throws Exception {
        // Default implementation does nothing - executor remains available
    }
    
    /**
     * Perform agent stop logic. Default implementation shuts down the executor.
     * Subclasses can override for custom stop behavior.
     * 
     * @throws Exception if stop fails
     */
    protected void doStop() throws Exception {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            if (!executor.awaitTermination(
                    configuration.getShutdownTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        }
    }
    
    /**
     * Perform agent reset logic. Default implementation resets metrics.
     * Subclasses can override for custom reset behavior.
     * 
     * @throws Exception if reset fails
     */
    protected void doReset() throws Exception {
        metrics.reset();
        memory.clear();
        // Executor is left in current state - will be recreated on next start if needed
    }
    
    // === Metrics ===
    
    @Override
    public AgentMetrics getMetrics() {
        return metrics;
    }
    
    // === Memory Management ===
    
    @Override
    public AgentMemory getMemory() {
        return memory;
    }
    
    @Override
    public void clearMemory() {
        memory.clear();
    }
    
    @Override
    public void compactMemory() {
        memory.compact();
        
        // Generate summary if we have enough entries
        if (memory.size() > 10) {
            String summary = generateMemorySummary();
            if (summary != null && !summary.trim().isEmpty()) {
                memory.setSummary(summary);
            }
        }
    }
    
    /**
     * Generate a summary of the agent's memory for compacting.
     * Subclasses can override this to provide custom summarization logic.
     * 
     * @return a summary of the agent's experiences and learnings, or null if no summary can be generated
     */
    protected String generateMemorySummary() {
        AgentMemory.MemoryStats stats = memory.getStats();
        List<AgentMemory.MemoryEntry> recentEntries = memory.getRecentEntries(5);
        
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Agent Memory Summary (%s):\n", agentName));
        summary.append(String.format("- Total tasks processed: %d\n", stats.getTotalEntries()));
        summary.append(String.format("- Success rate: %.1f%%\n", stats.getSuccessRate() * 100));
        summary.append(String.format("- Most recent %d tasks:\n", recentEntries.size()));
        
        for (AgentMemory.MemoryEntry entry : recentEntries) {
            summary.append(String.format("  * %s: %s\n", 
                entry.isSuccess() ? "SUCCESS" : "FAILED", 
                entry.getTaskDescription()));
            if (entry.getLearnings() != null) {
                summary.append(String.format("    Learning: %s\n", entry.getLearnings()));
            }
        }
        
        return summary.toString();
    }
    
    /**
     * Record a custom learning or insight in the agent's memory.
     * This can be used by subclasses to record domain-specific learnings.
     * 
     * @param task the task that generated the learning
     * @param result the result of the task (can be null)
     * @param learning the learning or insight to record
     */
    protected void recordLearning(TASK task, RESULT result, String learning) {
        memory.recordExecution(task, result, true, 0, learning);
    }
    
    // === Event Hooks ===
    
    /**
     * Called when the agent's state changes.
     * Subclasses can override for custom behavior.
     * 
     * @param oldState the previous state
     * @param newState the new state
     */
    protected void onStateChanged(AgentState oldState, AgentState newState) {
        // Default implementation does nothing
    }
    
    /**
     * Called when a task starts processing.
     * Subclasses can override for custom behavior.
     * 
     * @param task the task that started
     */
    protected void onTaskStarted(TASK task) {
        // Default implementation does nothing
    }
    
    /**
     * Called when a task completes successfully.
     * Subclasses can override for custom behavior.
     * 
     * @param task the task that completed
     * @param result the result produced
     */
    protected void onTaskCompleted(TASK task, RESULT result) {
        // Default implementation does nothing
    }
    
    /**
     * Called when a task fails.
     * Subclasses can override for custom behavior.
     * 
     * @param task the task that failed
     * @param exception the exception that caused the failure
     */
    protected void onTaskFailed(TASK task, Exception exception) {
        // Default implementation does nothing
    }
    
    @Override
    public String toString() {
        return String.format("%s{id='%s', name='%s', version='%s', state=%s}", 
                getClass().getSimpleName(), agentId, agentName, version, state.get());
    }
}